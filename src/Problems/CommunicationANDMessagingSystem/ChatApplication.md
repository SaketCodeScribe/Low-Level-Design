# Chat Application — Low-Level Design

## Functional Requirements

1. Support one-to-one and group conversations
2. Preserve message ordering
3. Store and show chat history per user
4. Messages are immutable — cannot be edited or deleted
5. User deletes chat → loses their history only
6. Newly added users only see messages after their addition

## Non-Functional Requirements

1. **Modularity** — clear separation of concerns across services
2. **Extensibility** — open to future features (read receipts, media, reactions)
3. **Scalability** — support many concurrent users and message delivery
4. **Maintainability** — clean, testable, easy to extend

---

## High-Level Architecture

```
┌───────────────────────────────────────────────────────────┐
│                   ChatApplicationFacade                    │
│                  (Singleton — DCL + volatile)              │
├──────────────────┬──────────────────┬─────────────────────┤
│    ChatService   │  ConversationMgmt│   RecordService     │
│   (Observable)   │     Service      │                     │
│                  │                  │  userRecords         │
│   observers ─────┼──────────────────┼──► per-user,        │
│                  │  conversations   │     per-convo        │
│                  │  userGroups      │     (LinkedHashSet)  │
└────────┬─────────┴──────┬───────────┴─────────────────────┘
         │                │
         │         ┌──────┴──────┐
         │         │ Conversation│
         │         ├─────────────┤
         │         │ Normal      │──── Conversation (immutable members)
         │         │ Group       │──── MutableConversation (add/remove)
         │         └─────────────┘
         │
    ┌────┴─────────────┐
    │ NotificationService│  (Observer)
    │  (pluggable)       │
    └────────────────────┘
```

**Circular dependency resolution:** `CMS` uses a two-phase init — no-arg constructor + `init(chatService, recordService)` called by the facade after all three services are created.

---

## Class Diagram

```mermaid
classDiagram
    class ChatApplicationFacade {
        -ConversationManagementService cms
        -ChatService chatService
        -RecordService recordService
        +getInstance() ChatApplicationFacade
        +createOneToOne(convoId, users) Conversation
        +createGroup(convoId, users, owner) Conversation
        +addUsersToGroup(convoId, users, addedBy)
        +removeUsersFromGroup(convoId, users, removedBy)
        +leaveGroup(convoId, user)
        +sendMessage(convoId, message)
        +getChatHistory(userId, convoId) Set~Message~
        +addObserver(observer)
        +removeObserver(observer)
    }

    class ConversationManagementService {
        -Map~String, Conversation~ conversations
        -Map~String, Set~String~~ userGroups
        -ChatService chatService
        -RecordService recordService
        +init(chatService, recordService)
        +createConversation(type, convoId, users, owner) Conversation
        +addUser(convoId, users, addedBy)
        +removeUser(convoId, users, removedBy)
        +leaveGroup(convoId, user)
        +getConversation(convoId) Conversation
        +getUsers(convoId) Set~User~
    }

    class ChatService {
        -ConversationManagementService cms
        -RecordService recordService
        -Set~Observer~ observers
        +sendMessage(convoId, message)
        +addObserver(ob)
        +removeObserver(ob)
        +notifyAll(convoId, message, users)
    }

    class RecordService {
        -ConversationManagementService cms
        -Map~String, Map~String, Record~~ userRecords
        +appendRecord(convoId, message)
        +removeRecord(userId, convoId)
        +getUserRecordForConversation(userId, convoId) Set~Message~
    }

    class Conversation {
        <<interface>>
        +getUsers() Set~User~
    }

    class MutableConversation {
        <<interface>>
        +addUsers(users)
        +removeUsers(user)
    }

    class NormalConversation {
        -Set~User~ users
        +getUsers() Set~User~
    }

    class GroupConversation {
        -Set~User~ users
        +getUsers() Set~User~
        +addUsers(users)
        +removeUsers(user)
    }

    class Record {
        -long recordId
        -Set~Message~ records
        +getRecords() Set~Message~
        +append(message)
    }

    class Observer {
        <<interface>>
        +updateStateChange(convoId, message, users)
    }

    class Observable {
        <<interface>>
        +notifyAll(convoId, message, users)
    }

    class NotificationService {
        +updateStateChange(convoId, message, users)
    }

    class User {
        <<record>>
        +String userId
        +String userName
    }

    class Message {
        <<record>>
        +long messageId
        +User owner
        +String content
        +long timeStampInMilli
        +MessageType type
    }

    class MessageType {
        <<enum>>
        SYSTEM
        USER
    }

    class ConversationType {
        <<enum>>
        ONE_TO_ONE
        GROUP
    }

    MutableConversation --|> Conversation
    NormalConversation ..|> Conversation
    GroupConversation ..|> MutableConversation
    ChatService ..|> Observable
    NotificationService ..|> Observer
    ChatApplicationFacade --> ConversationManagementService
    ChatApplicationFacade --> ChatService
    ChatApplicationFacade --> RecordService
    ConversationManagementService --> ChatService : sends system messages
    ConversationManagementService --> RecordService : cleanup on remove
    ChatService --> ConversationManagementService : reads users
    ChatService --> RecordService : appends records
    ChatService --> Observer : notifies 1..*
    RecordService --> ConversationManagementService : reads users
    ConversationManagementService --> Conversation : 1..*
    RecordService --> Record : per user per convo
    Message --> MessageType