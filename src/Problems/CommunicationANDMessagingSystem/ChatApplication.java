package Problems.CommunicationANDMessagingSystem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FRs
 * 1. Application should support one to one and group conversation
 * 2. System preserve the order of messages
 * 3. System should store and show the chat history
 * 4. Mssgs are immutable - it can't be edited or deleted
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

    static interface Conversation {
        Set<User> getUsers();

        default void addUser(List<User> user) {
            throw new RuntimeException("method not supported!");
        }

        default void removeUser(User user) {
            throw new RuntimeException("method not supported!");
        }
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

        public NormalConversation(Set<User> users) {
            this.users = ConcurrentHashMap.newKeySet();
            this.users.addAll(users);
        }

        @Override
        public Set<User> getUsers() {
            return this.users;
        }
    }

    static class GroupConversation implements Conversation {
        Set<User> users;

        public GroupConversation() {
            this.users = ConcurrentHashMap.newKeySet();
        }

        @Override
        public Set<User> getUsers() {
            return this.users;
        }

        @Override
        public void addUser(List<User> user) {
            users.addAll(user);
        }

        @Override
        public void removeUser(User user) {
            this.users.remove(user);
        }
    }

    record Message(String messageId, String owner, String content, long timeStampInMilli) {
    }

    static class Record {
        String recordId;
        Set<Message> records;

        public Record(String recordId) {
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
        Map<String, Record> records;
        Set<Observer> observers;
        ConversationManagementService cms;

        public ChatService(ConversationManagementService cms) {
            this.cms = cms;
            this.records = new HashMap<>();
            this.observers = ConcurrentHashMap.newKeySet();
        }

        public void addObserver(Observer ob) {
            this.observers.add(ob);
        }

        public void removeObserver(Observer ob) {
            this.observers.remove(ob);
        }

        public synchronized void sendMessage(String conversationId, Message message) {
            if (cms.getConversation(conversationId) == null) return;
            records.get(conversationId).append(message);
            notifyAll(conversationId, message, cms.getUsers(conversationId));
        }

        public synchronized Set<Message> getRecordHistory(String conversationId) {
            return records.get(conversationId).getRecords();
        }

        @Override
        public void notifyAll(String conversationId, Message message, Set<User> users) {
            for(Observer ob:this.observers) ob.updateStateChange(conversationId, message, users);
        }
    }

    static class ConversationManagementService {
        Map<String, Conversation> conversations;
        ChatService chatService;

        public ConversationManagementService(ChatService chatService) {
            this.chatService = chatService;
            this.conversations = new ConcurrentHashMap<>();
        }

        public Conversation getConversation(String conversationId) {
            return this.conversations.get(conversationId);
        }

        public Set<User> getUsers(String conversationId) {
            Conversation conversation = this.conversations.get(conversationId);
            return conversation == null ? Set.of() : conversation.getUsers();
        }
    }
}
