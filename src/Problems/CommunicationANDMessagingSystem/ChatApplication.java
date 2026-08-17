package Problems.CommunicationANDMessagingSystem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FRs
 * 1. Application should support one to one and group conversation
 * 2. System preserve the order of messages
 * 3. System should store and show the chat history
 * 4. Mssgs are immutable - it can't be edited or deleted
 * 5. User deletes the chat, then he would loose the history.
 * 6. Any new user added will only see the chat after his addition
 * NFRs:
 * 1. Modular - Classes are modular and has a clear separation of concern
 * 2. Extensible - System should support future features
 * 3. Scalable - System should support many concurrent users and deliver mssgs
 * 4. Maintainability - Class should be clean, testable and easy to support update or extend
 */
public class ChatApplication {
    enum MessageType {
        SYSTEM,
        USER;
    }

    enum ConversationType {
        ONE_TO_ONE,
        GROUP;
    }

    static interface Conversation {
        Set<User> getUsers();
    }

    static interface MutableConversation extends Conversation {
        void addUsers(List<User> users);

        void removeUsers(User user);
    }

    interface Observer {
        void updateStateChange(String conversationId, Message message, Set<User> users);
    }

    interface Observable {
        void notifyAll(String conversationId, Message message, Set<User> users);
    }

    record User(String userId, String userName) {
    }

    static class NormalConversation implements Conversation {
        Set<User> users;

        public NormalConversation(List<User> users) {
            this.users = new HashSet<>();
            this.users.addAll(users);
        }

        @Override
        public Set<User> getUsers() {
            return this.users;
        }
    }

    static class GroupConversation implements MutableConversation {
        Set<User> users;

        private GroupConversation() {
            this.users = new HashSet<>();
        }

        public GroupConversation(List<User> users) {
            this();
            this.users.addAll(users);
        }

        @Override
        public Set<User> getUsers() {
            return this.users;
        }

        @Override
        public void addUsers(List<User> user) {
            users.addAll(user);
        }

        @Override
        public void removeUsers(User user) {
            this.users.remove(user);
        }
    }

    record Message(long messageId, User owner, String content, long timeStampInMilli, MessageType type) {
    }

    static class Record {
        long recordId;
        Set<Message> records;

        public Record(long recordId) {
            this.recordId = recordId;
            this.records = new LinkedHashSet<>();
        }

        public synchronized Set<Message> getRecords() {
            return this.records;
        }

        public synchronized void append(Message message) {
            this.records.add(message);
        }
    }

    static class ChatService implements Observable {
        final ConversationManagementService cms;
        final RecordService recordService;
        Set<Observer> observers;

        public ChatService(ConversationManagementService cms, RecordService recordService) {
            this.cms = cms;
            this.recordService = recordService;
            this.observers = ConcurrentHashMap.newKeySet();
        }

        public void addObserver(Observer ob) {
            this.observers.add(ob);
        }

        public void removeObserver(Observer ob) {
            this.observers.remove(ob);
        }

        public synchronized void sendMessage(String conversationId, Message message) {
            if (cms.getConversation(conversationId) == null || !cms.getConversation(conversationId).getUsers().contains(message.owner()))
                return;
            recordService.appendRecord(conversationId, message);
            notifyAll(conversationId, message, cms.getUsers(conversationId));
        }

        @Override
        public void notifyAll(String conversationId, Message message, Set<User> users) {
            for (Observer ob : this.observers) ob.updateStateChange(conversationId, message, users);
        }
    }

    static class RecordService {
        final ConversationManagementService cms;
        Map<String, Map<String, Record>> userRecords;

        public RecordService(ConversationManagementService cms) {
            this.userRecords = new HashMap<>();
            this.cms = cms;
        }

        public synchronized void appendRecord(String convoId, Message message) {
            for (User user : cms.getUsers(convoId)) {
                userRecords.compute(user.userId(), (key, value) -> {
                    if (value == null) value = new HashMap<>();
                    value.computeIfAbsent(convoId, k -> new Record(UUID.randomUUID().node())).append(message);
                    return value;
                });
            }
        }

        public synchronized void removeRecord(String userId, String convoId) {
            userRecords.computeIfPresent(userId, (key, value) -> {
                value.remove(convoId);
                if (value.isEmpty()) return null;
                return value;
            });
        }

        public synchronized Set<Message> getUserRecordForConversation(String userId, String convoId) {
            if (!userRecords.containsKey(userId)) return null;
            Record record = userRecords.get(userId).get(convoId);
            return record != null ? record.getRecords() : Set.of();
        }
    }

    static class ConversationManagementService {
        Map<String, Conversation> conversations;
        Map<String, Set<String>> userGroups;
        ChatService chatService;
        RecordService recordService;

        public ConversationManagementService() {
            this.conversations = new HashMap<>();
            this.userGroups = new HashMap<>();
        }

        public void init(ChatService chatService, RecordService recordService) {
            this.chatService = chatService;
            this.recordService = recordService;
        }

        public Conversation getConversation(String conversationId) {
            return this.conversations.get(conversationId);
        }

        public Set<User> getUsers(String conversationId) {
            Conversation conversation = this.conversations.get(conversationId);
            return conversation == null ? Set.of() : conversation.getUsers();
        }

        public synchronized Conversation createConversation(ConversationType type, String convoId, List<User> users, User owner) {
            switch (type) {
                case ONE_TO_ONE -> {
                    if (users.size() != 2)
                        throw new IllegalArgumentException(type + " need to have only two users but found " + users.size());
                    if (conversations.putIfAbsent(convoId, new NormalConversation(users)) == null) {
                        userGroups.computeIfAbsent(users.get(0).userId(), x -> new HashSet<>()).add(convoId);
                        userGroups.computeIfAbsent(users.get(1).userId(), x -> new HashSet<>()).add(convoId);
                    }
                }
                case GROUP -> {
                    if (conversations.putIfAbsent(convoId, new GroupConversation(users)) == null) {
                        for (User user : users) {
                            userGroups.computeIfAbsent(user.userId(), x -> new HashSet<>()).add(convoId);
                        }
                    }
                    chatService.sendMessage(convoId, new Message(UUID.randomUUID().node(), null, String.format("%s is create group %s", owner, convoId), System.currentTimeMillis(), MessageType.SYSTEM));
                }
                default -> throw new IllegalArgumentException(type + " not supported!");
            }
            return conversations.get(convoId);
        }

        public synchronized void addUser(String convoId, List<User> users, User addedBy) {
            Conversation convo = conversations.get(convoId);
            if (convo instanceof MutableConversation mutable) {
                mutable.addUsers(users);
                for (User user : users) {
                    userGroups.computeIfAbsent(user.userId(), x -> new HashSet<>()).add(convoId);
                }
                chatService.sendMessage(convoId, new Message(UUID.randomUUID().node(), null, String.format("%s has added %s to group %s", addedBy.userId(), users, convoId), System.currentTimeMillis(), MessageType.SYSTEM));
            }
        }

        public synchronized void removeUser(String convoId, List<User> users, User removedBy) {
            Conversation convo = conversations.get(convoId);
            if (convo instanceof MutableConversation mutable) {
                for (User user : users) {
                    mutable.removeUsers(user);
                    userGroups.get(user.userId()).remove(convoId);
                    recordService.removeRecord(user.userId(), convoId);
                }
                chatService.sendMessage(convoId, new Message(UUID.randomUUID().node(), null, String.format("%s has removed %s from group %s", removedBy.userId(), users, convoId), System.currentTimeMillis(), MessageType.SYSTEM));
            }
        }

        public synchronized void leaveGroup(String convoId, User user) {
            if (userGroups.containsKey(user.userId()) && userGroups.get(user.userId()).contains(convoId)) {
                Conversation convo = conversations.get(convoId);
                if (convo instanceof MutableConversation mutable) {
                    mutable.removeUsers(user);
                    userGroups.get(user.userId()).remove(convoId);
                    recordService.removeRecord(user.userId(), convoId);
                    chatService.sendMessage(convoId, new Message(UUID.randomUUID().node(), null, String.format("%s has left the group %s", user.userId(), convoId), System.currentTimeMillis(), MessageType.SYSTEM));
                }
            }
            if (conversations.get(convoId).getUsers().isEmpty()) conversations.remove(convoId);
        }
    }

    static class NotificationService implements Observer {

        @Override
        public void updateStateChange(String conversationId, Message message, Set<User> users) {
            for (User user : users) {
                if (!user.equals(message.owner()))
                    System.out.printf("You received a notification %s in %s%n", message.content(), conversationId);
            }
        }
    }

    static class ChatApplicationFacade {
        private static volatile ChatApplicationFacade INSTANCE;

        private final ConversationManagementService cms;
        private final ChatService chatService;
        private final RecordService recordService;

        private ChatApplicationFacade() {
            this.cms = new ConversationManagementService();
            this.recordService = new RecordService(cms);
            this.chatService = new ChatService(cms, recordService);
            this.cms.init(chatService, recordService);
        }

        public static ChatApplicationFacade getInstance() {
            if (INSTANCE == null) {
                synchronized (ChatApplicationFacade.class) {
                    if (INSTANCE == null) {
                        INSTANCE = new ChatApplicationFacade();
                    }
                }
            }
            return INSTANCE;
        }

        // ---- Conversation lifecycle ----
        public Conversation createOneToOne(String convoId, List<User> users) {
            return cms.createConversation(ConversationType.ONE_TO_ONE, convoId, users, null);
        }

        public Conversation createGroup(String convoId, List<User> users, User owner) {
            return cms.createConversation(ConversationType.GROUP, convoId, users, owner);
        }

        public void addUsersToGroup(String convoId, List<User> users, User addedBy) {
            cms.addUser(convoId, users, addedBy);
        }

        public void removeUsersFromGroup(String convoId, List<User> users, User removedBy) {
            cms.removeUser(convoId, users, removedBy);
        }

        public void leaveGroup(String convoId, User user) {
            cms.leaveGroup(convoId, user);
        }

        // ---- Messaging ----
        public void sendMessage(String convoId, Message message) {
            chatService.sendMessage(convoId, message);
        }

        public Set<Message> getChatHistory(String userId, String convoId) {
            return recordService.getUserRecordForConversation(userId, convoId);
        }

        // ---- Observers ----
        public void addObserver(Observer observer) {
            chatService.addObserver(observer);
        }

        public void removeObserver(Observer observer) {
            chatService.removeObserver(observer);
        }
    }
}
