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
        LIKE,
        DISLIKE,
        COMMENT,
        FRIEND_REQUEST_ACCEPTED,
        FRIEND_REQUEST_RECEIVED;
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
            lastChecked = notifications.size()-1;
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

        public synchronized Post createPost(String postId, String content){
            posts.putIfAbsent(postId, new TextPost(postId, content, this));
            return posts.get(postId);
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

        public void removePost(String postId) {
            posts.remove(postId);
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
            this.buffer = new TreeSet<>((a,b) -> {
                int value = Long.compare(b.getCreationTimestamp(), a.getCreationTimestamp());
                return value == 0 ? a.getPostId().compareTo(b.getPostId()) : value;
            });
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
                user.addNotification(Map.entry(userAction, msg));
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

        public User getUser(String userId){
            return users.get(userId);
        }

        public void deleteUser(User user){
            users.remove(user.getId());
        }

        public void cancelFriendRequest(User sender, User receiver) {
            receiver.denyFriendRequest(sender);
        }

        public void deleteUserPost(String userId, String postId) {
            User user = users.get(userId);
            if (user != null) {
                user.removePost(postId);
            }
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

        public void removePostsWhenUnfriended(User userA, User userB){
            for(Post post:userA.getPosts()) {
                userA.getFeed().removeUserPost(post);
            }
            for(Post post: userB.getPosts()){
                userB.getFeed().removeUserPost(post);
            }
        }

        public void removePostOfUserAinUserB(User userA, User userB, String postId){
            Optional.ofNullable(userA.getPost(postId)).ifPresent(post -> userB.getFeed().removeUserPost(post));
        }
    }

    static class PostService{
        private static volatile PostService instance = null;
        private static final Object lock = new Object();
        private final Set<Observer> observers = new HashSet<>();
        private final ConcurrentHashMap<String, Post> posts;
        private final ConcurrentHashMap<String, String> users;
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
        private PostService(){
            this.posts = new ConcurrentHashMap<>();
            this.users = new ConcurrentHashMap<>();
        }

        public void addObserver(Observer observer){
            observers.add(observer);
        }

        public void removeObserver(Observer observer){
            observers.remove(observer);
        }

        public synchronized void createPost(User user, String postId, String content){
            Post post = user.createPost(postId, content);
            posts.putIfAbsent(postId, post);
            users.putIfAbsent(postId, user.getId());
            notifyAllObservers(UserAction.POST_CREATION, user, post);
        }

        private void notifyAllObservers(UserAction action, User user, Object msg) {
            observers.forEach(observer -> observer.updateStateChange(user, action, msg));
        }

        public void deletePost(String postId){
            posts.remove(postId);
            users.remove(postId);
        }

        public Post getPost(String postId){
            return posts.get(postId);
        }

        public void likePost(User actor, String postId, UserService userService){
            Post post = posts.get(postId);
            if (post == null) return;
            User owner = userService.getUser(users.get(postId));
            if (owner == null) return;
            post.likePost(actor.getId());
            notifyAllObservers(UserAction.LIKE, owner, actor.getName()+" liked your post");
        }

        public void dislikePost(User actor, String postId,  UserService userService){
            Post post = posts.get(postId);
            if (post == null) return;
            User owner = userService.getUser(users.get(postId));
            if (owner == null) return;
            post.dislikePost(actor.getId());
            notifyAllObservers(UserAction.DISLIKE, owner, actor.getName()+" disliked your post");
        }

        public void removeReaction(User actor, Post post){
            post.removeReactionFromUser(actor.getId());
        }

        public void addComment(String postId, User actor, UserService userService, String comment, long commentId){
            Post post = posts.get(postId);
            if (post == null) return;
            User owner = userService.getUser(users.get(postId));
            if (owner == null) return;
            post.addComment(actor, comment, commentId);
            notifyAllObservers(UserAction.COMMENT, owner, actor.getName()+" added comment on your post");
        }

        public String getUser(String postId) {
            return users.get(postId);
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
                        instance = new SocialNetworkFacade(UserService.getInstance(), FeedService.getInstance(), NotificationService.getInstance(), PostService.getInstance());
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
            postService.addObserver(new FeedObserver());
            postService.addObserver(new NotificationObserver());
            userService.addObserver(new NotificationObserver());
        }

        public void registerUser(String name, String email){
            userService.registerUser(name, email);
        }
        public synchronized void createPost(String postId, String userId, String content){
            User user = userService.getUser(userId);
            if (user != null) {
                postService.createPost(user, postId, content);
            }
        }
        public synchronized void deletePost(String postId){
            String userId = postService.getUser(postId);
            User user = userService.getUser(userId);
            if (user != null) {
                for (User friend : user.getFriends()) {
                    feedService.removePostOfUserAinUserB(user, friend, postId);
                }
                postService.deletePost(postId);
                userService.deleteUserPost(userId, postId);
            }
        }
        public synchronized void sendFriendRequest(String senderId, String receiverId){
            User sender = userService.getUser(senderId);
            User receiver = userService.getUser(receiverId);
            if (sender == null || receiver == null) return;
            userService.sentUserFriendRequest(sender, receiver);
        }
        public void acceptFriendRequest(String receiverId, String senderId){
            User sender = userService.getUser(senderId);
            User receiver = userService.getUser(receiverId);
            if (sender == null || receiver == null) return;
            userService.addUserAsFriend(receiver, sender);
        }
        public void cancelFriendRequest(String receiverId, String senderId){
            User sender = userService.getUser(senderId);
            User receiver = userService.getUser(receiverId);
            if (sender == null || receiver == null) return;
            userService.cancelFriendRequest(sender, receiver);
        }
        public synchronized void removeFriend(String friendId, String userId){
            User user = userService.getUser(userId);
            User friend = userService.getUser(friendId);
            if (user == null || friend == null) return;
            userService.removeUserAsFriend(user, friend);
            feedService.removePostsWhenUnfriended(user, friend);
        }
        public synchronized void reactOnPost(String postId, String userId, UserAction userAction){
            User user = userService.getUser(userId);
            Post post = postService.getPost(postId);
            if (user == null || post == null) return;
            switch (userAction){
                case LIKE -> postService.likePost(user, postId, userService);
                case DISLIKE -> postService.dislikePost(user, postId, userService);
            }
        }
        public synchronized void addCommentOnPost(String commenterId, String postId, String comment, long commentId){
            User commenter = userService.getUser(commenterId);
            if (commenter != null) {
                postService.addComment(postId, commenter, userService, comment, commentId);
            }
        }

    }
}
