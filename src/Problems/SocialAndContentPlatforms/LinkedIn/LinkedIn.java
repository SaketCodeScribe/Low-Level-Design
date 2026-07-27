package Problems.SocialAndContentPlatforms.LinkedIn;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * FRs:
 * 1. Profile management: users can manage their profile by updating details, work exp and certifications
 * 2. Connection management: users can send connection requests and accept/reject it
 * 3. Post management: Users personal feed for posts from his followers. Create post and interact with other post
 * 4. Notification management: Users receive notification of acceptance of connection, and interaction with his post.
 * 5. Search: users can search other members with their name.
 * <p>
 * NFRs:
 * 1. class should follow ood for better maintainability and testing
 * 2. class should be extensible for future features.
 * 3. class should be modular - organized into well separated components
 */
public class LinkedIn {
    interface Observer {
        public void updateStateChange(UserAction action);
    }

    static class User {
        String userId;
        String userName;
        String phoneNumber;
        String emailId;

        public User(String userId, String userName, String phoneNumber, String emailId) {
            this.userId = userId;
            this.userName = userName;
            this.phoneNumber = phoneNumber;
            this.emailId = emailId;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String getUserId() {
            return userId;
        }

        public String getUserName() {
            return userName;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public String getEmailId() {
            return emailId;
        }

        static class WorkExperience {
            String companyName;
            String location;
            String jobDescription;
            String jobDesignation;
            Date startDate;
            Date endDate;

            public WorkExperience(String companyName, String location, String jobDescription, String jobDesignation, Date startDate, Date endDate) {
                this.companyName = companyName;
                this.location = location;
                this.jobDescription = jobDescription;
                this.jobDesignation = jobDesignation;
                this.startDate = startDate;
                this.endDate = endDate;
            }

            @Override
            public String toString() {
                return "WorkExperience{" +
                        "companyName='" + companyName + '\'' +
                        ", location='" + location + '\'' +
                        ", jobDescription='" + jobDescription + '\'' +
                        ", jobDesignation='" + jobDesignation + '\'' +
                        ", startDate=" + startDate +
                        ", endDate=" + endDate +
                        '}';
            }
        }

        static class Builder {
            String userId;
            String userName;
            String phoneNumber;
            String emailId;

            public Builder() {
            }

            public Builder withUserId(String userId) {
                Objects.requireNonNull(userId);
                this.userId = userId;
                return this;
            }

            public Builder withUserName(String userName) {
                Objects.requireNonNull(userName);
                this.userName = userName;
                return this;
            }

            public Builder withPhoneNumber(String phoneNumber) {
                Objects.requireNonNull(phoneNumber);
                this.phoneNumber = phoneNumber;
                return this;
            }

            public Builder withEmailId(String emailId) {
                Objects.requireNonNull(emailId);
                this.emailId = emailId;
                return this;
            }

            public User build() {
                return new User(userId, userName, phoneNumber, emailId);
            }
        }
    }

    static abstract class UserAction {
        User owner;
        User actor;
        Post post;

        public UserAction(User owner, User actor, Post post) {
            this.owner = owner;
            this.actor = actor;
            this.post = post;
        }

        public User getOwner() {
            return owner;
        }

        public User getActor() {
            return actor;
        }

        public Post getPost() {
            return post;
        }

        public abstract String showNotification();
    }

    static class ReactionAction extends UserAction {
        public ReactionAction(User owner, User actor, Post post) {
            super(owner, actor, post);
        }

        @Override
        public String showNotification() {
            return actor + " like your post" + post;
        }
    }

    static class CommentAction extends UserAction {
        public CommentAction(User owner, User actor, Post post) {
            super(owner, actor, post);
        }

        @Override
        public String showNotification() {
            return actor + " commented on your post" + post;
        }
    }

    static class ReplyOnCommentAction extends UserAction {
        Comment comment;

        public ReplyOnCommentAction(User owner, User actor, Post post, Comment comment) {
            super(owner, actor, post);
            this.comment = comment;
        }

        @Override
        public String showNotification() {
            return actor + " replied on your comment" + comment.getComment() + " on post" + post;
        }
    }

    static class ConnectionAcceptanceAction extends UserAction {
        public ConnectionAcceptanceAction(User owner, User actor, Post post) {
            super(owner, actor, null);
        }

        @Override
        public String showNotification() {
            return actor + " accepted your connection request";
        }
    }

    static class ConnectionRequestAction extends UserAction {
        public ConnectionRequestAction(User owner, User actor, Post post) {
            super(owner, actor, null);
        }

        @Override
        public String showNotification() {
            return actor + " sent you a connection request";
        }
    }

    static class PostCreationAction extends UserAction {
        public PostCreationAction(User owner, User connection, Post post) {
            super(owner, connection, post);
        }

        @Override
        public String showNotification() {
            return owner + " create a post" + post;
        }
    }


    static class RemoveConnectionAction extends UserAction {
        public RemoveConnectionAction(User owner, User connection, Post post) {
            super(owner, connection, post);
        }

        @Override
        public String showNotification() {
            return null;
        }
    }

    static class PostDeletionAction extends UserAction {
        public PostDeletionAction(User user, Post post) {
            super(user, user, post);
        }

        @Override
        public String showNotification() {
            return null;
        }
    }

    static abstract class Post {
        private final String content;
        String postId;
        User user;

        public Post(String content, String postId, User user) {
            this.content = content;
            this.postId = postId;
            this.user = user;
        }

        public String getContent() {
            return content;
        }

        public String getPostId() {
            return postId;
        }

        public User getUser() {
            return user;
        }

    }

    static class UserPost extends Post {
        private String content;

        public UserPost(String postId, User user, String content) {
            super(content, postId, user);
        }
    }

    static class Comment {
        private final Map<User, String> replies;
        User actor;
        String commentId;
        String comment;

        public Comment(User actor, String commentId, String comment) {
            this.actor = actor;
            this.commentId = commentId;
            this.comment = comment;
            replies = new ConcurrentHashMap<>();
        }

        public User getActor() {
            return actor;
        }

        public String getCommentId() {
            return commentId;
        }

        public String getComment() {
            return comment;
        }

        public Map<User, String> getReplies() {
            return replies;
        }
    }

    static class UserManagementService {
        private static volatile UserManagementService instance = null;
        private static Object lock = new Object();
        Map<String, User> user;
        Map<String, Map<Integer, User.WorkExperience>> userWorkExp;

        private UserManagementService() {
            user = new ConcurrentHashMap<>();
            userWorkExp = new ConcurrentHashMap<>();
        }

        public static UserManagementService getInstance() {
            if (instance == null) {
                synchronized (lock) {
                    if (instance == null) {
                        instance = new UserManagementService();
                    }
                }
            }
            return instance;
        }

        public User registerUser(String userId, String userName, String phoneNumber, String emailId) {
            return user.computeIfAbsent(userId, key -> {
                        User user = User.builder()
                                .withUserId(userId)
                                .withUserName(userName)
                                .withPhoneNumber(phoneNumber)
                                .withEmailId(emailId)
                                .build();
                        userWorkExp.putIfAbsent(key, new ConcurrentHashMap<>());
                        return user;
                    }
            );
        }

        public void deleteUser(String userId) {
            user.remove(userId);
        }

        public void addWorkExperience(String userId, Map<Integer, User.WorkExperience> workExp) {
            userWorkExp.computeIfPresent(userId, (key, value) -> {
                value.putAll(workExp);
                return value;
            });
        }

        public void editWorkExperience(String userId, int workId, User.WorkExperience workExperience) {
            userWorkExp.computeIfPresent(userId, (key, value) -> {
                value.put(workId, workExperience);
                return value;
            });
        }

        public User getUser(String userId) {
            return user.get(userId);
        }

        public boolean userExists(String userId) {
            return user.containsKey(userId);
        }
    }

    static class ConnectionManagementService {
        private static volatile ConnectionManagementService instance = null;
        private static Object lock = new Object();
        Map<String, Set<String>> connections;
        Map<String, Set<String>> pendingRequests;

        Set<Observer> observers;

        private ConnectionManagementService() {
            connections = new HashMap<>();
            pendingRequests = new HashMap<>();
            observers = ConcurrentHashMap.newKeySet();
        }

        public static ConnectionManagementService getInstance() {
            if (instance == null) {
                synchronized (lock) {
                    if (instance == null) {
                        instance = new ConnectionManagementService();
                    }
                }
            }
            return instance;
        }

        public void addObserver(Observer ob) {
            observers.add(ob);
        }

        public void removeObserver(Observer ob) {
            observers.remove(ob);
        }

        public synchronized void sendConnectionRequest(User sender, User receiver) {
            pendingRequests.computeIfAbsent(receiver.getUserId(), x -> new HashSet<>()).add(sender.getUserId());
            notifyAllObservers(new ConnectionRequestAction(receiver, sender, null));
        }

        public synchronized void acceptConnectionRequest(User sender, User receiver) {
            connections.computeIfAbsent(receiver.getUserId(), x -> new HashSet<>()).add(sender.getUserId());
            connections.computeIfAbsent(sender.getUserId(), x -> new HashSet<>()).add(receiver.getUserId());
            deleteConnectionRequest(sender, receiver);
            notifyAllObservers(new ConnectionAcceptanceAction(sender, receiver, null));
        }

        public synchronized void deleteConnectionRequest(User sender, User receiver) {
            pendingRequests.computeIfPresent(receiver.getUserId(), (key, value) -> {
                value.remove(sender.getUserId());
                return value;
            });
        }

        public synchronized void removeConnection(User userA, User userB) {
            connections.computeIfPresent(userB.getUserId(), (key, value) -> {
                value.remove(userA.getUserId());
                return value;
            });
            connections.computeIfPresent(userA.getUserId(), (key, value) -> {
                value.remove(userB.getUserId());
                return value;
            });
            notifyAllObservers(new RemoveConnectionAction(userA, userB, null));
        }

        public Set<String> getUserConnections(String userId) {
            return connections.getOrDefault(userId, Collections.emptySet());
        }

        private void notifyAllObservers(UserAction action) {
            for (Observer ob : observers) {
                ob.updateStateChange(action);
            }
        }
    }

    static class PostManagementService {
        private static volatile PostManagementService instance = null;
        private static Object lock = new Object();
        private final UserManagementService userManagementService;
        private final ConnectionManagementService connectionManagementService;
        Map<String, Set<String>> userPosts;
        Map<String, Post> posts;
        Map<String, Set<User>> likes;
        Set<Observer> observers;

        private PostManagementService() {
            userPosts = new HashMap<>();
            posts = new ConcurrentHashMap<>();
            observers = ConcurrentHashMap.newKeySet();
            userManagementService = UserManagementService.getInstance();
            connectionManagementService = ConnectionManagementService.getInstance();
            likes = new HashMap<>();
        }

        public static PostManagementService getInstance() {
            if (instance == null) {
                synchronized (lock) {
                    if (instance == null) {
                        instance = new PostManagementService();
                    }
                }
            }
            return instance;
        }

        public void addObserver(Observer ob) {
            observers.add(ob);
        }

        public void removeObserver(Observer ob) {
            observers.remove(ob);
        }

        public synchronized void createPost(String userId, String postId, String content) {
            User user = userManagementService.getUser(userId);
            Post post = posts.putIfAbsent(postId, new UserPost(postId, user, content));
            userPosts.computeIfAbsent(user.getUserId(), k -> new HashSet<>()).add(postId);

            notifyAllObservers(new PostCreationAction(user, user, post));
            for (String connection : connectionManagementService.getUserConnections(user.getUserId())) {
                notifyAllObservers(new PostCreationAction(userManagementService.getUser(connection), user, post));
            }
        }

        public synchronized void deletePost(User user, String postId) {
            Post post = posts.remove(postId);
            userPosts.get(user.getUserId()).remove(postId);
            notifyAllObservers(new PostDeletionAction(user, post));
        }

        public synchronized void likePost(String postId, User actor) {
            likes.compute(postId, (key, value) -> {
                if (value == null) value = new HashSet<>();
                value.add(actor);
                return value;
            });
            Post post = getPost(postId);
            if (post == null) return;
            User user = getCreator(postId);
            notifyAllObservers(new ReactionAction(user, actor, post));
        }

        public void removeLike(String postId, User actor) {
            likes.computeIfPresent(postId, (key, value) -> {
                value.remove(actor);
                return value;
            });
        }

        public int getLikes(String postId) {
            return likes.get(postId).size();
        }

        public Post getPost(String postId) {
            return posts.get(postId);
        }

        public User getCreator(String postId) {
            Post post = getPost(postId);
            if (post == null) return null;
            return post.getUser();
        }

        private void notifyAllObservers(UserAction action) {
            for (Observer ob : observers) {
                ob.updateStateChange(action);
            }
        }
    }

    static class CommentManagementService {
        private static volatile CommentManagementService instance = null;
        private static Object lock = new Object();
        private final PostManagementService pms;
        private final UserManagementService ums;
        Map<String, Set<String>> postComments;
        Map<String, Comment> comments;
        Map<String, Set<String>> replies;
        Set<Observer> observers;

        private CommentManagementService() {
            this.postComments = new HashMap<>();
            this.comments = new HashMap<>();
            this.replies = new HashMap<>();
            observers = ConcurrentHashMap.newKeySet();
            pms = PostManagementService.getInstance();
            ums = UserManagementService.getInstance();
        }

        public static CommentManagementService getInstance() {
            if (instance == null) {
                synchronized (lock) {
                    if (instance == null) {
                        instance = new CommentManagementService();
                    }
                }
            }
            return instance;
        }

        public void addObserver(Observer ob) {
            observers.add(ob);
        }

        public void removeObserver(Observer ob) {
            observers.remove(ob);
        }

        public synchronized void addComment(String postId, String actor, String commentId, String content) {
            Comment comment = this.comments.putIfAbsent(commentId, new Comment(ums.getUser(actor), commentId, content));
            postComments.computeIfAbsent(postId, x -> new LinkedHashSet<>()).add(commentId);
            notifyAllObservers(new CommentAction(pms.getCreator(postId), ums.getUser(actor), pms.getPost(postId)));
        }

        public synchronized void removeComment(String commentId, String postId) {
            postComments.get(postId).remove(commentId);
            comments.remove(commentId);
        }

        public synchronized void replyOnComment(String postId, String commentId, String reply, String actor) {
            replies.computeIfAbsent(commentId, x -> new LinkedHashSet<>()).add(reply);
            notifyAllObservers(new ReplyOnCommentAction(pms.getCreator(postId), ums.getUser(actor), pms.getPost(postId), comments.get(commentId)));
        }

        private void notifyAllObservers(UserAction action) {
            for (Observer ob : observers) {
                ob.updateStateChange(action);
            }
        }
    }

    static class NotificationManagementService implements Observer {
        private static volatile NotificationManagementService instance = null;
        private static Object lock = new Object();
        Map<String, List<UserAction>> userNotifications;

        private NotificationManagementService() {
            this.userNotifications = new ConcurrentHashMap<>();
        }

        public static NotificationManagementService getInstance() {
            if (instance == null) {
                synchronized (lock) {
                    if (instance == null) {
                        instance = new NotificationManagementService();
                    }
                }
            }
            return instance;
        }

        @Override
        public void updateStateChange(UserAction action) {
            if (action.getOwner() == null || action instanceof RemoveConnectionAction) return;
            userNotifications
                    .computeIfAbsent(action.getOwner().getUserId(), k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(action);
        }

        public List<UserAction> getNotifications(String userId) {
            List<UserAction> list = userNotifications.get(userId);
            return list == null ? Collections.emptyList() : List.copyOf(list);
        }
    }

    static class FeedManagementService implements Observer {
        private static volatile FeedManagementService instance = null;
        private static Object lock = new Object();
        private final PostManagementService pms;
        private final UserManagementService ums;
        private final ConnectionManagementService cms;
        private final Map<String, Set<String>> userFeed;
        private Integer capacity;

        private FeedManagementService() {
            userFeed = new HashMap<>();
            this.pms = PostManagementService.getInstance();
            this.ums = UserManagementService.getInstance();
            this.cms = ConnectionManagementService.getInstance();
        }

        public static FeedManagementService getInstance() {
            if (instance == null) {
                synchronized (lock) {
                    if (instance == null) {
                        instance = new FeedManagementService();
                    }
                }
            }
            return instance;
        }

        public void setCapacity(int capacity) {
            if (this.capacity == null) {
                this.capacity = capacity;
            }
        }

        @Override
        public synchronized void updateStateChange(UserAction action) {
            if (action instanceof PostCreationAction) {
                updateUserFeed(action);
            } else if (action instanceof RemoveConnectionAction) {
                removeFeedFromUser(action);
            } else if (action instanceof PostDeletionAction) {
                removeDeletedPosts(action);
            }
        }

        private void updateUserFeed(UserAction action) {
            User recipient = action.getOwner();
            Set<String> feed = userFeed.computeIfAbsent(recipient.getUserId(), k -> new LinkedHashSet<>());
            if (feed.size() == this.capacity) {
                Iterator<String> it = feed.iterator();
                it.next();
                it.remove();
            }
            feed.add(action.getPost().getPostId());
        }

        private void removeDeletedPosts(UserAction action) {
            Set<String> feed = userFeed.get(action.getOwner().getUserId());
            if (feed == null) return;
            feed.removeIf(postId -> pms.getPost(postId) == null);
        }

        public synchronized void removeFeedFromUser(UserAction action) {
            User userA = action.getOwner();
            User userB = action.getActor();
            Set<String> userAFeed = userFeed.get(userA.getUserId());
            Set<String> userBFeed = userFeed.get(userB.getUserId());

            if (userAFeed != null) remove(userAFeed, userB.getUserId());
            if (userBFeed != null) remove(userBFeed, userA.getUserId());
        }

        private void remove(Set<String> feed, String targetUserId) {
            feed.removeIf(postId -> {
                User creator = pms.getCreator(postId);
                return creator != null && creator.getUserId().equals(targetUserId);
            });
        }

        public List<Post> getUserFeed(String userId) {
            Set<String> postIds = userFeed.get(userId);
            if (postIds == null) return null;
            List<Post> posts = postIds.stream().map(pms::getPost).collect(Collectors.toList());
            Collections.reverse(posts);
            return posts;
        }
    }

    static class SearchService {
        private static volatile SearchService instance = null;
        private static Object lock = new Object();
        private final TrieNode root;
        private final Map<String, String> userIdToName;
        private final UserManagementService ums;

        private SearchService() {
            this.root = new TrieNode();
            this.userIdToName = new ConcurrentHashMap<>();
            this.ums = UserManagementService.getInstance();
        }

        public static SearchService getInstance() {
            if (instance == null) {
                synchronized (lock) {
                    if (instance == null) {
                        instance = new SearchService();
                    }
                }
            }
            return instance;
        }

        public synchronized void indexUser(String userId, String name) {
            userIdToName.put(userId, name);
            for (String token : tokenize(name)) {
                insertToken(token, userId);
            }
        }

        public synchronized void removeUser(String userId) {
            String name = userIdToName.remove(userId);
            if (name == null) return;
            for (String token : tokenize(name)) {
                removeToken(token, userId);
            }
        }

        public synchronized List<User> search(String prefix, int limit) {
            if (prefix == null || prefix.isEmpty()) return Collections.emptyList();
            TrieNode node = root;
            for (char c : prefix.toLowerCase().toCharArray()) {
                node = node.children.get(c);
                if (node == null) return Collections.emptyList();
            }
            return node.userIds.stream()
                    .limit(limit)
                    .map(ums::getUser)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        private void insertToken(String token, String userId) {
            TrieNode node = root;
            for (char c : token.toCharArray()) {
                node = node.children.computeIfAbsent(c, k -> new TrieNode());
                node.userIds.add(userId);
            }
        }

        private void removeToken(String token, String userId) {
            TrieNode node = root;
            for (char c : token.toCharArray()) {
                node = node.children.get(c);
                if (node == null) return;
                node.userIds.remove(userId);
            }
        }

        private List<String> tokenize(String name) {
            List<String> tokens = new ArrayList<>();
            for (String part : name.toLowerCase().trim().split("\\s+")) {
                if (!part.isEmpty()) tokens.add(part);
            }
            return tokens;
        }

        private static class TrieNode {
            Map<Character, TrieNode> children = new HashMap<>();
            Set<String> userIds = new HashSet<>();
        }
    }

    static class LinkedInFacade {
        private final UserManagementService userService;
        private final ConnectionManagementService connectionService;
        private final PostManagementService postService;
        private final CommentManagementService commentService;
        private final NotificationManagementService notificationService;
        private final FeedManagementService feedService;
        private final SearchService searchService;


        public LinkedInFacade() {
            this.userService = UserManagementService.getInstance();
            this.connectionService = ConnectionManagementService.getInstance();
            this.postService = PostManagementService.getInstance();
            this.commentService = CommentManagementService.getInstance();
            this.notificationService = NotificationManagementService.getInstance();
            this.feedService = FeedManagementService.getInstance();
            this.searchService = SearchService.getInstance();

            feedService.setCapacity(1000);

            connectionService.addObserver(notificationService);
            connectionService.addObserver(feedService);
            postService.addObserver(notificationService);
            postService.addObserver(feedService);
            commentService.addObserver(notificationService);
        }

        // ---------- Validation helpers ----------
        private User requireUser(String userId) {
            User u = userService.getUser(userId);
            if (u == null) throw new NoSuchElementException("User not found: " + userId);
            return u;
        }

        private Post requirePost(String postId) {
            Post p = postService.getPost(postId);
            if (p == null) throw new NoSuchElementException("Post not found: " + postId);
            return p;
        }

        private Comment requireComment(String commentId) {
            Comment c = commentService.comments.get(commentId);
            if (c == null) throw new NoSuchElementException("Comment not found: " + commentId);
            return c;
        }

        // ---------- User ----------
        public User registerUser(String userId, String userName, String phoneNumber, String emailId) {
            User u = userService.registerUser(userId, userName, phoneNumber, emailId);
            searchService.indexUser(userId, userName);
            return u;
        }

        public void deleteUser(String userId) {
            requireUser(userId);
            userService.deleteUser(userId);
            searchService.removeUser(userId);
        }

        public void addWorkExperience(String userId, Map<Integer, User.WorkExperience> workExp) {
            requireUser(userId);
            userService.addWorkExperience(userId, workExp);
        }

        public void editWorkExperience(String userId, int workId, User.WorkExperience we) {
            requireUser(userId);
            userService.editWorkExperience(userId, workId, we);
        }

        public User getUser(String userId) {
            return requireUser(userId);
        }

        // ---------- Connections ----------
        public void sendConnectionRequest(String senderId, String receiverId) {
            if (senderId.equals(receiverId))
                throw new IllegalArgumentException("Cannot connect to self");
            User sender = requireUser(senderId);
            User receiver = requireUser(receiverId);
            connectionService.sendConnectionRequest(sender, receiver);
        }

        public void acceptConnectionRequest(String senderId, String receiverId) {
            User sender = requireUser(senderId);
            User receiver = requireUser(receiverId);
            connectionService.acceptConnectionRequest(sender, receiver);
        }

        public void rejectConnectionRequest(String senderId, String receiverId) {
            User sender = requireUser(senderId);
            User receiver = requireUser(receiverId);
            connectionService.deleteConnectionRequest(sender, receiver);
        }

        public void removeConnection(String userAId, String userBId) {
            User a = requireUser(userAId);
            User b = requireUser(userBId);
            connectionService.removeConnection(a, b);
        }

        public Set<String> getUserConnections(String userId) {
            requireUser(userId);
            Set<String> conns = connectionService.getUserConnections(userId);
            return conns == null ? Collections.emptySet() : conns;
        }

        // ---------- Posts ----------
        public void createPost(String userId, String postId, String content) {
            requireUser(userId);
            postService.createPost(userId, postId, content);
        }

        public void removePost(String userId, String postId) {
            User user = requireUser(userId);
            Post post = requirePost(postId);
            if (!post.getUser().getUserId().equals(userId))
                throw new IllegalStateException("User " + userId + " does not own post " + postId);
            postService.deletePost(user, postId);
        }

        public void likePost(String postId, String actorId) {
            User actor = requireUser(actorId);
            requirePost(postId);
            postService.likePost(postId, actor);
        }

        public void unlikePost(String postId, String actorId) {
            User actor = requireUser(actorId);
            requirePost(postId);
            postService.removeLike(postId, actor);
        }

        public int getLikes(String postId) {
            requirePost(postId);
            return postService.getLikes(postId);
        }

        // ---------- Comments ----------
        public void addComment(String postId, String actorId, String commentId, String content) {
            requireUser(actorId);
            requirePost(postId);
            commentService.addComment(postId, actorId, commentId, content);
        }

        public void removeComment(String postId, String commentId) {
            requirePost(postId);
            requireComment(commentId);
            commentService.removeComment(commentId, postId);
        }

        public void replyOnComment(String postId, String commentId, String reply, String actorId) {
            requireUser(actorId);
            requirePost(postId);
            requireComment(commentId);
            commentService.replyOnComment(postId, commentId, reply, actorId);
        }

        // ---------- Notifications ----------
        public List<UserAction> getNotifications(String userId) {
            requireUser(userId);
            return notificationService.getNotifications(userId);
        }

        // ---------- Feed ----------
        public List<Post> getUserFeed(String userId) {
            requireUser(userId);
            List<Post> feed = feedService.getUserFeed(userId);
            return feed == null ? Collections.emptyList() : feed;
        }

        // New search method:
        public List<User> searchUsers(String prefix, int limit) {
            return searchService.search(prefix, limit);
        }
    }

}
