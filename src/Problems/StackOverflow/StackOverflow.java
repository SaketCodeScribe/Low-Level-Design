package Problems.StackOverflow;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

/**
 Functional requirements
     Users can register and log in.
     Users can post questions.
     Users can post answers to questions.
     Users can comment on questions and answers.
     Users can upvote or downvote questions and answers, and a user can vote only once per post.
     Questions can have tags.
     Users can search questions by keywords and filter by tags.
     The system may maintain user reputation based on votes.

 Non-functional requirements
     The system must handle concurrent voting and updates safely.
     Vote counts, view counts, and accepted answer updates must be consistent.
     The design should be modular, testable, and extensible.
     The system should scale to support many users and posts.
     The system should be maintainable using clean OOD principles.
 */
public class StackOverflow {
    enum VoteType{
        UP,
        DOWN, NONE;
    }
    enum ReputationEvent {
        Question_Creation(5),
        Answer_Addition(10),
        Question_UpVote(20),
        Question_DownVote(-15),
        Answer_UpVote(25),
        Answer_DownVote(-30),
        Question_Deletion(-5),
        Answer_Deletion(-10),
        Comment_Addition(2),
        Comment_Deletion(-2);
        private final int delta;
        ReputationEvent(int delta) {
            this.delta = delta;
        }
        public int getDelta(){
            return this.delta;
        }
    }
    static class User{
        private final long userId;
        private final String userName;
        private AtomicLong reputation;
        public User(long userId, String userName) {
            this.userId = userId;
            this.userName = userName;
            reputation = new AtomicLong(1);
        }

        public long getUserId() {
            return userId;
        }

        public String getUserName() {
            return userName;
        }

        public long getReputation() {
            return reputation.get();
        }

        public void updateReputation(int delta){
            while(true){
                long oldRep = reputation.get();
                long newRep = Math.max(1, oldRep + delta);
                if (reputation.compareAndSet(oldRep, newRep)){
                    break;
                }
                else LockSupport.parkNanos(1000);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof User user)) return false;
            return userId == user.userId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId);
        }
    }

    static abstract class Post implements Comparable<Post>{
        private final long postId;
        private final long ownerId;
        private final long creationTime;
        private final AtomicReference<String> content;
        protected final ConcurrentLinkedQueue<Post> children;
        private final Set<Long> upVote, downVote;

        public Post(long postId, long ownerId, String content) {
            this.postId = postId;
            this.ownerId = ownerId;
            this.content = new AtomicReference<>(content);
            this.children = new ConcurrentLinkedQueue<>();
            this.upVote = new HashSet<>();
            this.downVote = new HashSet<>();
            this.creationTime = System.currentTimeMillis();
        }

        public long getOwnerId() {
            return ownerId;
        }

        public String getContent() {
            return content.get();
        }

        public boolean setContent(String newContent){
            String oldContent = content.get();
            return content.compareAndSet(oldContent, newContent);
        }

        public void reply(Post post){
            children.offer(post);
        }

        private ConcurrentLinkedQueue<Post> getChildren(){
            return this.children;
        }

        private void dfs(Post root, List<Post> result) {
            for(Post post:root.getChildren()){
                result.add(post);
                dfs(post, result);
            }
        }

        public List<Post> getAllReplies(){
            List<Post> result = new ArrayList<>();
            dfs(this, result);
            return result;
        }

        public long getPostId() {
            return postId;
        }

        public synchronized VoteType vote(long userId, VoteType voteType) {
            if (Long.compare(userId, this.getOwnerId()) == 0) return null; // null = rejected
            VoteType previous = upVote.contains(userId) ? VoteType.UP
                    : downVote.contains(userId) ? VoteType.DOWN
                    : VoteType.NONE;
            switch (voteType) {
                case UP   -> { downVote.remove(userId); upVote.add(userId); }
                case DOWN -> { upVote.remove(userId);  downVote.add(userId); }
            }
            return previous; // NONE = first vote, UP/DOWN = switching from prior vote
        }

        @Override
        public int compareTo(Post other) {
            return Long.compare(this.creationTime, other.creationTime);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Post post)) return false;
            return postId == post.postId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(postId);
        }
    }
    static class Question extends Post{
        private final List<String> tags;
        public Question(long postId, long ownerId, String content, List<String> tags) {
            super(postId, ownerId, content);
            this.tags = tags;
        }

        public List<String> getTags() {
            return tags;
        }

    }
    static class Answer extends Post{
        public Answer(long postId, long ownerId, String content) {
            super(postId, ownerId, content);
        }
    }
    static class Comment extends Post{
        public Comment(long postId, long ownerId, String content) {
            super(postId, ownerId, content);
        }
    }

    static class UserService{
        private final ConcurrentHashMap<Long, User> users;

        public UserService() {
            users = new ConcurrentHashMap<>();
        }

        public void register(long userId, String userName){
            users.putIfAbsent(userId, new User(userId, userName));
        }

        public User getUser(long id){
            return users.get(id);
        }

        public void removeUser(long userId){
            users.remove(userId);
        }

        public long getReputation(long userId){
            User user = users.getOrDefault(userId, null);
            return user != null ? user.getReputation() : -1;
        }
    }
    static class PostService{
        private final ConcurrentHashMap<Long, Set<Long>> usersPost;
        private final ConcurrentHashMap<Long, Post> posts;

        public PostService() {
            usersPost = new ConcurrentHashMap<>();
            posts = new ConcurrentHashMap<>();
        }

        public List<Long> getUserPosts(long id){
            return new ArrayList<>(usersPost.getOrDefault(id, ConcurrentHashMap.newKeySet()));
        }

        public void delete(long postId){
            posts.computeIfPresent(postId , (pk, pv) -> {
                usersPost.computeIfPresent(pv.getOwnerId(), (uk, uv) -> {
                    uv.remove(pk);
                    return uv;
                });
                return null;
            });
        }

        public void edit(long postId, String content){
            posts.computeIfPresent(postId, (k,v) -> {
                v.setContent(content);
                return v;
            });
        }

        public void createPost(Post post, long userId){
            posts.computeIfAbsent(post.getPostId(), k -> {
                usersPost.computeIfAbsent(userId, x -> ConcurrentHashMap.newKeySet()).add(k);
                return post;
            });
        }

        public void reply(Post reply, long parentId){
            posts.computeIfPresent(parentId, (k,v) -> {
                v.reply(reply);
                return v;
            });
        }

        public List<String> getAllReplies(long postId){
            List<Post> replies = new ArrayList<>();
            posts.computeIfPresent(postId, (k,v) -> {
                replies.addAll(v.getAllReplies());
                return v;
            });
            return replies.stream().map(Post::getContent).collect(Collectors.toList());
        }

        public List<String> getAllUserPost(long userId){
            List<Long> postIds = new ArrayList<>();
            List<Post> posts = new ArrayList<>();
            usersPost.computeIfPresent(userId, (k,v) -> {
                postIds.addAll(v);
                return v;
            });

            if (postIds.isEmpty()) return new ArrayList<>();

            posts = postIds.stream().map(this.posts::get).filter(Objects::nonNull).collect(Collectors.toList());
            Collections.sort(posts);
            return posts.stream().map(Post::getContent).collect(Collectors.toList());
        }
        public Post getPost(long id){
            return posts.get(id);
        }

        public VoteType vote(long postId, long userId, VoteType voteType) {
            VoteType[] result = { null };
            posts.computeIfPresent(postId, (k, v) -> {
                result[0] = v.vote(userId, voteType);
                return v;
            });
            return result[0];
        }
    }
    static class VoterService{
        public void updateReputation(User user, ReputationEvent reputationEvent){
            user.updateReputation(reputationEvent.getDelta());
        }
    }

    static class SearchService{
        private final ConcurrentHashMap<String, Set<Long>> tagToPost;
        private final ConcurrentHashMap<String, Set<Long>> wordToPost;

        {
            tagToPost = new ConcurrentHashMap<>();
            wordToPost = new ConcurrentHashMap<>();
        }

        private void index(Post post, boolean isAdd){
            if (post instanceof Question question){
                for(String tag:question.getTags()){
                    if (isAdd) {
                        tagToPost.computeIfAbsent(tag, x -> ConcurrentHashMap.newKeySet()).add(question.getPostId());
                    } else{
                        tagToPost.computeIfPresent(tag, (k,v) -> {
                            v.remove(question.getPostId());
                            return v;
                        });
                    }
                }
            }
            String[] words = post.getContent().split(" ");
            for(String word:words){
                if (isAdd) {
                    wordToPost.computeIfAbsent(word, x -> ConcurrentHashMap.newKeySet()).add(post.getPostId());
                } else{
                    wordToPost.computeIfPresent(word, (k, v) -> {
                        v.remove(post.getPostId());
                        return v;
                    });
                }
            }
        }

        public List<Long> getPosts(String keyword){
            Set<Long> posts = new HashSet<>();
            for(String word:keyword.split(" ")){
                posts.addAll(wordToPost.getOrDefault(word, Collections.<Long>emptySet()));
            }
            return posts.stream().filter(Objects::nonNull).collect(Collectors.toList());
        }

        public List<Long> getPostsFromTags(List<String> tags){
            Set<Long> posts = new HashSet<>();
            for(String tag:tags){
                posts.addAll(tagToPost.getOrDefault(tag, Collections.<Long>emptySet()));
            }
            return posts.stream().filter(Objects::nonNull).collect(Collectors.toList());
        }

        public void addPost(Post post){
            index(post, true);
        }

        public void removePost(Post post){
            index(post, false);
        }
    }

    static class StackOverFlowSystemFacade{
        private static volatile StackOverFlowSystemFacade instance = null;
        private final UserService userService;
        private final PostService postService;
        private final VoterService voterService;
        private final SearchService searchService;
        private static final Object lock = new Object();
        private StackOverFlowSystemFacade(UserService userService, PostService postService, VoterService voterService, SearchService searchService){
            this.userService = userService;
            this.postService = postService;
            this.voterService = voterService;
            this.searchService = searchService;
        }

        public static StackOverFlowSystemFacade getInstance(){
            if (instance == null){
                synchronized (lock){
                    if (instance == null){
                        instance = new StackOverFlowSystemFacade(new UserService(), new PostService(), new VoterService(), new SearchService());
                    }
                }
            }
            return instance;
        }


        public void registerUser(long userId, String userName){
            userService.register(userId, userName);
        }

        public void removeUser(long userId){
            userService.removeUser(userId);
        }

        public long getReputation(long userId){
            return userService.getReputation(userId);
        }

        public void createQuestion(long postId, long ownerId, String content, List<String> tags){
            User user = userService.getUser(ownerId);
            if (user == null) return;
            Question question = new Question(postId, ownerId, content, tags);
            postService.createPost(question, ownerId);
            searchService.addPost(question);
            voterService.updateReputation(user, ReputationEvent.Question_Creation);
        }

        public void createAnswer(long postId, long ownerId, String content, long questionId){
            User user = userService.getUser(ownerId);
            if (user == null) return;
            Answer answer = new Answer(postId, ownerId, content);
            postService.createPost(answer, ownerId);
            postService.reply(answer, questionId);
            searchService.addPost(answer);
            voterService.updateReputation(user, ReputationEvent.Answer_Addition);
        }

        public void createComment(long postId, long ownerId, String content, long parentId){
            User user = userService.getUser(ownerId);
            if (user == null) return;
            Comment comment = new Comment(postId, ownerId, content);
            postService.createPost(comment, ownerId);
            postService.reply(comment, parentId);
            voterService.updateReputation(user, ReputationEvent.Comment_Addition);
        }

        public void editPost(long postId, String newContent){
            postService.edit(postId, newContent);
        }

        public void deleteQuestion(long postId, long ownerId){
            User user = userService.getUser(ownerId);
            if (user == null) return;
            Post post = postService.getPost(postId);
            postService.delete(postId);
            voterService.updateReputation(user, ReputationEvent.Question_Deletion);
            searchService.removePost(post);
        }

        public void deleteAnswer(long postId, long ownerId){
            User user = userService.getUser(ownerId);
            if (user == null) return;
            Post post = postService.getPost(postId);
            postService.delete(postId);
            voterService.updateReputation(user, ReputationEvent.Answer_Deletion);
            searchService.removePost(post);
        }

        public void deleteComment(long postId, long ownerId){
            User user = userService.getUser(ownerId);
            if (user == null) return;
            Post post = postService.getPost(postId);
            postService.delete(postId);
            voterService.updateReputation(user, ReputationEvent.Comment_Deletion);
            searchService.removePost(post);
        }

        public void upvoteQuestion(long ownerId){
            User user = userService.getUser(ownerId);
            if (user == null) return;
            voterService.updateReputation(user, ReputationEvent.Question_UpVote);
        }

        public void downvoteQuestion(long ownerId){
            User user = userService.getUser(ownerId);
            if (user == null) return;
            voterService.updateReputation(user, ReputationEvent.Question_DownVote);
        }

        public void upvoteAnswer(long ownerId){
            User user = userService.getUser(ownerId);
            if (user == null) return;
            voterService.updateReputation(user, ReputationEvent.Answer_UpVote);
        }

        public void downvoteAnswer(long ownerId){
            User user = userService.getUser(ownerId);
            if (user == null) return;
            voterService.updateReputation(user, ReputationEvent.Answer_DownVote);
        }

        public List<String> getReplies(long postId){
            return postService.getAllReplies(postId);
        }

        public List<String> getUserPosts(long userId){
            return postService.getAllUserPost(userId);
        }

        public List<String> search(String keyword){
            List<Post> posts = searchService.getPosts(keyword).stream().map(postService::getPost).filter(Objects::nonNull).sorted().toList();
            return posts.stream().map(Post::getContent).collect(Collectors.toList());
        }


        public List<String> filter(List<String> tags){
            List<Post> posts = searchService.getPostsFromTags(tags).stream().map(postService::getPost).filter(Objects::nonNull).sorted().toList();
            return posts.stream().map(Post::getContent).collect(Collectors.toList());
        }

        public void vote(long postId, long userId, VoteType voteType) {
            User user = userService.getUser(userId);
            if (user == null) return;
            Post post = postService.getPost(postId);
            if (post == null) return;
            User owner = userService.getUser(post.getOwnerId());
            if (owner == null) return;

            VoteType previousVote = postService.vote(postId, userId, voteType);
            if (previousVote == null) return; // self-vote rejected, don't touch reputation
            if (post instanceof Comment) return;

            boolean isQuestion = post instanceof Question;

            // Reverse old vote's reputation effect
            if (previousVote != VoteType.NONE) {
                ReputationEvent oldEvent = previousVote == VoteType.UP
                        ? (isQuestion ? ReputationEvent.Question_UpVote   : ReputationEvent.Answer_UpVote)
                        : (isQuestion ? ReputationEvent.Question_DownVote : ReputationEvent.Answer_DownVote);
                owner.updateReputation(-oldEvent.getDelta());
            }

            // Apply new vote's reputation effect
            ReputationEvent newEvent = voteType == VoteType.UP
                    ? (isQuestion ? ReputationEvent.Question_UpVote   : ReputationEvent.Answer_UpVote)
                    : (isQuestion ? ReputationEvent.Question_DownVote : ReputationEvent.Answer_DownVote);
            voterService.updateReputation(owner, newEvent);
        }
    }

}
