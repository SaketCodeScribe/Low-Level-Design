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
        FriendRequest,
        Like,
        Dislike,
        Comment;
    }
    interface Observer{
        void updateStateChange(Map<UserAction, Object> notification);
    }

    static class Notification implements Observer{
        User user;
        List<Map<UserAction, Object>> notifications;
        int lastPointer;
        private Notification(User user) {
            this.user = user;
            notifications = new ArrayList<>();
        }

        public synchronized int getDelta() {
            return !notifications.isEmpty() ? notifications.size()-1-lastPointer : 0;
        }

        public synchronized void updateLastPointer(int delta){
            lastPointer += delta;
        }

        public int getSize(){
            return notifications.size();
        }

        public User getUser() {
            return user;
        }

        public List<Map<UserAction, Object>> getNotifications() {
            return notifications;
        }

        @Override
        public synchronized void updateStateChange(Map<UserAction, Object> notification) {
            notifications.add(notification);
        }
    }
    static class User{
        String name;
        String email;
        Set<User> friends;
        int lastPointer;
        private Notification notification;

        public User(String name, String email) {
            this.name = name;
            this.email = email;
            this.friends = ConcurrentHashMap.newKeySet();
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

        public void addUserAsFriend(User user){
            friends.add(user);
        }

        public void removeUserFromFriend(User user){
            friends.remove(user);
        }

        public synchronized Notification getNotification(){
            return this.notification = this.notification == null ? new Notification(this) : this.notification;
        }

    }

    static class UserService{
        private static volatile UserService instance = null;
        private static final Object lock = new Object();
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

        private UserService() {
        }

        public void addUserAsFriend(User user1, User user2){
            user1.addUserAsFriend(user2);
            user2.addUserAsFriend(user1);
        }

        public void removeUserAsFriend(User user1, User user2){
            user1.removeUserFromFriend(user2);
            user2.removeUserFromFriend(user1);
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
            return user.getNotification().getDelta();
        }

        public int getTotalNotificationCount(User user){
            return user.getNotification().getSize();
        }

        public List<Map<UserAction, Object>> getAllNotifications(User user){
            return user.getNotification().getNotifications();
        }
    }

    static abstract class Post{
        static class Comment{
            User user;
            String Content;

            public Comment(User user, String content) {
                this.user = user;
                Content = content;
            }

            public User getUser() {
                return user;
            }

            public String getContent() {
                return Content;
            }
        }
        User user;
        List<Comment> comments;
        Set<User> likes;
        Set<User> dislikes;
        String postId;


        public Post(String postId, User user) {
            this.postId = postId;
            this.user = user;
            this.comments = new ArrayList<>();
            this.likes = ConcurrentHashMap.newKeySet();
            this.dislikes = ConcurrentHashMap.newKeySet();
        }

        public User getUser() {
            return user;
        }

        public String getPostId() {
            return postId;
        }
    }

    static class TextPost extends Post{
        String content;

        public TextPost(String postId, User user) {
            super(postId, user);
        }

        public String getContent() {
            return content;
        }
    }

    static class PostService{

    }

    static class FeedService{
        Map<String, Track> tracks;

        public FeedService() {
            tracks = new ConcurrentHashMap<>();
        }

        static class Track{
            ArrayDeque<Post> buffers;

            public Track() {
                buffers = new ArrayDeque<>();
            }
        }
    }
}
