package Problems.SocialAndContentPlatforms.SocialNetwork;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Functional Requirements:
 * 1. users can register with name and email
 * 2. users can add other as friends. relationship is bidirectional
 * 3. users can share post
 * 4. users can like and comment on posts
 * 5. each user has its own personalized feed.
 * 6. users receives notification upon like and comment on his post
 *
 * NFRs:
 * 1. system should follow ood with clear separation of concern
 * 2. system should be modular and extensible
 * 3. system should be designed for testability and maintainability
 */
public class SocialNetwork {
    enum UserAction {
        POST_CREATION,
        FRIEND_REQUEST,
        LIKE,
        DISLIKE,
        COMMENT, POST_DELETION, FRIEND_REQUEST_ACCEPTED, FRIEND_REQUEST_RECEIVED;
    }
    interface Observer{
        void updateStateChange(User user, UserAction userAction, Object mssg);
    }

    static class User{
        String name;
        String email;
        Set<User> friends;
        Set<User> pendingFriendRequests;
        private final Feed feed;
        private final List<Map.Entry<UserAction, Object>> notifications;
        private final Map<String, Post> posts;
        int lastChecked;

        public User(String name, String email) {
            this.name = name;
            this.email = email;
            this.feed = new Feed(1000);
            this.friends = ConcurrentHashMap.newKeySet();
            posts = new ConcurrentHashMap<>();
            notifications = new ArrayList<>();
            pendingFriendRequests = ConcurrentHashMap.newKeySet();
            lastChecked = -1;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public List<User> getFriends(){
            return new ArrayList<>(this.friends);
        }

        public void addFriendRequest(User user){
            pendingFriendRequests.add(user);
        }

        public void denyFriendRequest(User user){
            pendingFriendRequests.remove(user);
        }

        public synchronized boolean acceptFriendRequest(User user){
            if (pendingFriendRequests.remove(user)) return friends.add(user);
            return false;
        }

        public void addUserAsFriend(User user){
            friends.add(user);
        }

        public void removeUserFromFriend(User user){
            friends.remove(user);
        }

        public synchronized void addNotification(Map.Entry<UserAction, Object> msg){
            notifications.add(msg);
        }

        public synchronized List<Map.Entry<UserAction, Object>> getNotifications(){
            lastChecked = getNotifications().size()-1;
            return notifications;
        }

        public int getLastChecked() {
            return lastChecked;
        }

        public int getNotificationSize() {
            return notifications.size()-1;
        }

        public void updateFeed(Post post) {
            this.feed.offer(post);
        }

        public Feed getFeed() {
            return feed;
        }

        public synchronized void createPost(String postId, String content){
            posts.putIfAbsent(postId, new TextPost(postId, content, this));
        }

        public Post getPost(String postId){
            return posts.get(postId);
        }

        public List<Post> getPosts() {
            return new ArrayList<>(posts.values());
        }

        public Post deletePost(String postId) {
            return posts.remove(postId);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof User user)) return false;
            return Objects.equals(name, user.name) && Objects.equals(email, user.email);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, email);
        }

        public boolean isFriend(User userA) {
            return friends.contains(userA);
        }

        public String getId() {
            return this.name+"-"+this.email;
        }
    }

    static abstract class Post{
        static class Comment{
            long commentId;
            User user;
            String Content;
            long creationTimestamp;
            public Comment(User user, String content, long commentId) {
                this.user = user;
                Content = content;
                this.commentId = commentId;
                this.creationTimestamp = System.currentTimeMillis();
            }

            public User getUser() {
                return user;
            }

            public String getContent() {
                return Content;
            }

            public long getCreationTimestamp() {
                return creationTimestamp;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Comment comment)) return false;
                return commentId == comment.commentId;
            }

            @Override
            public int hashCode() {
                return Objects.hash(commentId);
            }
        }
        String userId;
        Map<Long, Comment> comments;
        Set<String> likes;
        Set<String> dislikes;
        String postId;
        long creationTimestamp;

        public Post(String postId, String userId) {
            this.postId = postId;
            this.userId = userId;
            this.comments = new LinkedHashMap<>();
            this.likes = new HashSet<>();
            this.dislikes = new HashSet<>();
            this.creationTimestamp = System.currentTimeMillis();
        }

        public String getUser() {
            return userId;
        }

        public String getPostId() {
            return postId;
        }

        public long getCreationTimestamp() {
            return creationTimestamp;
        }

        public synchronized void likePost(String user){
            dislikes.remove(user);
            likes.add(user);
        }

        public synchronized void dislikePost(String user){
            dislikes.add(user);
            likes.remove(user);
        }


        public synchronized void removeReactionFromUser(String user){
            dislikes.remove(user);
            likes.remove(user);
        }

        public int getLikeCount(){
            return likes.size();
        }

        public int getDislikeCount(){
            return dislikes.size();
        }

        public synchronized void addComment(User user, String comment, long commentId){
            comments.putIfAbsent(commentId, new Comment(user, comment, commentId));
        }

        public Comment getComment(long commentId){
            return comments.get(commentId);
        }

        public List<Comment> getAllComments(){
            return new ArrayList<>(comments.values());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Post post)) return false;
            return Objects.equals(postId, post.postId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(postId);
        }
    }

    static class TextPost extends Post{
        String content;

        public TextPost(String postId, String content, User user) {
            super(postId, user.name+"-"+user.email);
            this.content = content;
        }

        public String getContent() {
            return content;
        }
    }

    static class Feed{
        TreeSet<Post> buffer;
        private final int capacity;

        public Feed(int capacity) {
            this.capacity = capacity;
            this.buffer = new TreeSet<>((a,b) -> Long.compare(b.getCreationTimestamp(), a.getCreationTimestamp()));
        }
        public synchronized void offer(Post post){
            if (buffer.size() >= capacity) buffer.remove(buffer.last());
            buffer.add(post);
        }

        public synchronized List<Post> getUserFeed(){
            return new ArrayList<>(buffer);
        }

        public synchronized void removeUserPost(Post post){
            buffer.remove(post);
        }
    }

    static class NotificationObserver implements Observer{
        @Override
        public void updateStateChange(User user, UserAction userAction, Object msg) {
            if (userAction != UserAction.POST_CREATION) {
                for(User friend:user.getFriends()){
                    friend.addNotification(Map.entry(userAction, msg));
                }
            }
        }
    }

    static class FeedObserver implements Observer {
        @Override
        public void updateStateChange(User user, UserAction userAction, Object msg) {
            if (userAction == UserAction.POST_CREATION) {
                if (!(msg instanceof Post post)) {
                    throw new RuntimeException(UserAction.POST_CREATION.name() + " can't have object other than " + Post.class);
                }
                for (User friend : user.getFriends()) {
                    friend.updateFeed(post);
                }
            }
        }
    }

    static class UserService{
        private static volatile UserService instance = null;
        private static final Object lock = new Object();
        private final Set<Observer> observers = new HashSet<>();
        private final Map<String, User> users = new ConcurrentHashMap<>();
        public static UserService getInstance(){
            if (instance == null){
                synchronized (lock){
                    if (instance == null){
                        instance = new UserService();
                    }
                }
            }
            return instance;
        }

        private UserService() {}

        public void addObserver(Observer observer){
            observers.add(observer);
        }

        public void removeObserver(Observer observer){
            observers.remove(observer);
        }

        public void addUserAsFriend(User receiver, User user){
            if (receiver.acceptFriendRequest(user)) {
                user.addUserAsFriend(receiver);
                notifyAllObservers(UserAction.FRIEND_REQUEST_ACCEPTED, user, receiver.getName() + " added you as friend");
            }
        }

        private void notifyAllObservers(UserAction action, User user, String msg) {
            observers.forEach(observer -> observer.updateStateChange(user, action, msg));
        }

        public void removeUserAsFriend(User user1, User user2){
            user1.removeUserFromFriend(user2);
            user2.removeUserFromFriend(user1);
        }

        public void sentUserFriendRequest(User sender, User receiver){
            receiver.addFriendRequest(sender);
            notifyAllObservers(UserAction.FRIEND_REQUEST_RECEIVED, receiver, sender.getName()+" sent you a friend request");
        }

        public void registerUser(String name, String email){
            users.putIfAbsent(name+"-"+email, new User(name, email));
        }


        public void deleteUser(User user){
            users.remove(user.getId());
        }
    }

    static class NotificationService{
        private static volatile NotificationService instance = null;
        private static final Object lock = new Object();
        public static NotificationService getInstance(){
            if (instance == null){
                synchronized (lock){
                    if (instance == null){
                        instance = new NotificationService();
                    }
                }
            }
            return instance;
        }
        private NotificationService(){}

        public int getNewNotificationCount(User user){
            return user.getNotificationSize() - user.lastChecked;
        }

        public List<Map.Entry<UserAction, Object>> getAllNotifications(User user){
            return user.getNotifications();
        }
    }


    static class FeedService{
        private static volatile FeedService instance = null;
        private static final Object lock = new Object();
        public static FeedService getInstance(){
            if (instance == null){
                synchronized (lock){
                    if (instance == null){
                        instance = new FeedService();
                    }
                }
            }
            return instance;
        }
        private FeedService(){}

        public List<Post> getUserFeed(User user){
            return user.getFeed().getUserFeed();
        }

        public void removePostsFromUsers(User userA, User userB){
            for(Post post:userA.getPosts()) {
                userA.getFeed().removeUserPost(post);
            }
            for(Post post: userB.getPosts()){
                userB.getFeed().removeUserPost(post);
            }
        }
    }

    static class PostService{
        private static volatile PostService instance = null;
        private static final Object lock = new Object();
        private final Set<Observer> observers = new HashSet<>();
        public static PostService getInstance(){
            if (instance == null){
                synchronized (lock){
                    if (instance == null){
                        instance = new PostService();
                    }
                }
            }
            return instance;
        }
        private PostService(){}

        public void addObserver(Observer observer){
            observers.add(observer);
        }

        public void removeObserver(Observer observer){
            observers.remove(observer);
        }

        public void createPost(User user, String postId, String content){
            user.createPost(postId, content);
            notifyAllObservers(UserAction.POST_CREATION, user, user.getName()+" created post "+ postId);
        }

        private void notifyAllObservers(UserAction action, User user, String msg) {
            observers.forEach(observer -> observer.updateStateChange(user, action, msg));
        }

        public Post deletePost(User user, String postId){
            return user.deletePost(postId);
        }

        public Post getPost(String postId, User user){
            return user.getPost(postId);
        }

        public void likePost(User actor, Post post){
            post.likePost(actor.getId());
            notifyAllObservers(UserAction.LIKE, actor, actor.getName()+" liked your post");
        }

        public void dislikePost(User actor, Post post){
            post.dislikePost(actor.getId());
            notifyAllObservers(UserAction.DISLIKE, actor, actor.getName()+" disliked your post");
        }

        public void removeReaction(User actor, Post post){
            post.removeReactionFromUser(actor.getId());
        }

        public void addComment(Post post, User actor, String comment, long commentId){
            post.addComment(actor, comment, commentId);
            notifyAllObservers(UserAction.COMMENT, actor, actor.getName()+" added comment on your post");
        }
    }

    static class SocialNetworkFacade{
        private static volatile SocialNetworkFacade instance = null;
        private static final Object lock = new Object();

        private final UserService userService;
        private final FeedService feedService;
        private final NotificationService notificationService;
        private final PostService postService;
        public static SocialNetworkFacade getInstance(){
            if (instance == null){
                synchronized (lock){
                    if (instance == null){
                        instance = new SocialNetworkFacade(new UserService(), new FeedService(), new NotificationService(), new PostService());
                    }
                }
            }
            return instance;
        }
        private SocialNetworkFacade(UserService userService, FeedService feedService, NotificationService notificationService, PostService postService){
            this.userService = userService;
            this.feedService = feedService;
            this.notificationService = notificationService;
            this.postService = postService;
        }

        public void registerUser(){}
        public void createPost(){}
        public void deletePost(){}
        public void sendFriendRequest(){}
        public void acceptFriendRequest(){}
        public void cancelFriendRequest(){}
        public void removeFriend(){}
        public void reactOnPost(){}
        public void addCommentOnPost(){}

    }
}
