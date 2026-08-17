# Notification System — Low-Level Design

## Functional Requirements

1. Supports EMAIL, SMS, and PUSH notification channels
2. Sends notifications asynchronously
3. Can send one or multiple channel types per request
4. Delivery is non-blocking, using a thread pool for parallel sending
5. Failed deliveries are retried with configurable attempts and backoff

## Non-Functional Requirements

1. **Modularity** — well-separated classes with single responsibilities
2. **Extensibility** — new channels added without modifying existing code
3. **OOD** — follows SOLID principles (Strategy, Factory, Decorator patterns)

---

## High-Level Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                  NotificationSystemFacade                     │
│                 (Singleton — DCL + volatile)                  │
├─────────────────────────┬────────────────────────────────────┤
│    RecipientService     │       NotificationService          │
│                         │       (10-thread daemon pool)      │
│    recipients (CHM)     │                                    │
└─────────────────────────┴──────────────┬─────────────────────┘
                                         │
                                         ▼
                                  ┌──────────────┐
                                  │ RetryService  │  (Decorator)
                                  │  max 3 + 1s  │
                                  └──────┬───────┘
                                         │
                              ┌──────────┼──────────┐
                              ▼          ▼          ▼
                        ┌─────────┐ ┌─────────┐ ┌─────────┐
                        │ Email   │ │  SMS    │ │  Push   │
                        │ Service │ │ Service │ │ Service │
                        └─────────┘ └─────────┘ └─────────┘
                              ▲          ▲          ▲
                              └──────────┼──────────┘
                                         │
                                  ServiceFactory
                                  (cached singletons)
```

---

## Class Diagram

```mermaid
classDiagram
    class NotificationSystemFacade {
        -RecipientService recipientService
        -NotificationService notificationService
        +getInstance() NotificationSystemFacade
        +registerRecipient(id, mobNo, email) Recipient
        +publish(recipientId, content, types) CompletableFuture~List~Response~~
    }

    class RecipientService {
        -Map~String, Recipient~ recipients
        +createUser(id, email, mobNo) Recipient
        +getRecipient(id) Recipient
    }

    class NotificationService {
        -ExecutorService executor
        -RecipientService recipientService
        +publish(types, content, recipientId) CompletableFuture~List~Response~~
        +shutdown()
    }

    class Service {
        <<interface>>
        +getType() MessageType
        +publish(recipient, content) Response
    }

    class EmailService {
        +getType() MessageType
        +publish(recipient, content) Response
    }

    class SMSService {
        +getType() MessageType
        +publish(recipient, content) Response
    }

    class PushService {
        +getType() MessageType
        +publish(recipient, content) Response
    }

    class RetryService {
        -Service service
        -int max_retry
        -int retryDelayMillis
        +getType() MessageType
        +publish(recipient, content) Response
    }

    class ServiceFactory {
        -Map~MessageType, Service~ serviceMap
        +createService(type) Service
    }

    class Recipient {
        -String recipientId
        -String email
        -String mobNo
    }

    class Response {
        -MessageType channel
        -int code
        -boolean success
        -String detail
    }

    class DeliveryException {
        +DeliveryException(msg, cause)
    }

    class MessageType {
        <<enum>>
        EMAIL
        SMS
        PUSH
    }

    NotificationSystemFacade --> RecipientService
    NotificationSystemFacade --> NotificationService
    NotificationService --> RecipientService
    NotificationService --> RetryService : creates per call
    NotificationService --> ServiceFactory : gets channel impl
    RetryService ..|> Service : implements
    RetryService --> Service : decorates
    EmailService ..|> Service
    SMSService ..|> Service
    PushService ..|> Service
    ServiceFactory --> Service : caches + creates
    Service --> Response : returns
    Service --> Recipient : receives
    Service --> DeliveryException : throws
    Response --> MessageType
```