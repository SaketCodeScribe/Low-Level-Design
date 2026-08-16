# PubSub System — Low-Level Design

## Functional Requirements

1. Create and delete topics
2. Producers can write messages to a topic
3. Consumers can subscribe/unsubscribe to a topic
4. Messages are broadcast to every subscriber on the topic
5. Message ordering is preserved per subscriber
6. Producers write and consumers read asynchronously

## Non-Functional Requirements

1. **Modularity** — clear separation of concerns, independently testable components
2. **Extensibility** — open to future features (partitions, dead-letter queues, replay)
3. **Scalability** — concurrent-safe without global locks, non-blocking write path

---

## High-Level Architecture

```
┌──────────────────────────────────────────────────────────┐
│                      PubSubFacade                        │
│                  (Singleton — DCL + volatile)             │
├──────────────────┬──────────────────┬────────────────────┤
│  PublisherService│   TopicService   │ SubscriberService  │
│                  │                  │                    │
│  publishers      │   topics         │  subscribers       │
│  pubTopics       │                  │  subTopics         │
└────────┬─────────┴────────┬─────────┴──────────┬─────────┘
         │                  │                    │
         └──────────────────┼────────────────────┘
                            ▼
                    ┌──────────────┐
                    │    Topic     │
                    │              │
                    │  buffers     │
                    │  executor    │
                    │              │
                    │ ┌──────────┐ │
                    │ │ Buffer   │ │  × N subscribers
                    │ │ (LBQ +CF)│ │
                    │ └──────────┘ │
                    └──────────────┘
```

---

## Class Diagram

```mermaid
classDiagram
    class PubSubFacade {
        -TopicService topicService
        -PublisherService publisherService
        -SubscriberService subscriberService
        +getInstance() PubSubFacade
        +publish(pubId, topic, key, value) CompletableFuture~Boolean~[]
        +consume(subId, topic) Record
        +createTopic(topicName)
        +deleteTopic(topicName)
        +registerPublisher(pubId)
        +registerSubscriber(subId)
        +subscribe(subId, topicName)
    }

    class TopicService {
        -Map~String, Topic~ topics
        +addTopic(topicName)
        +delete(topicName)
        +writeRecord(record, topicName) CompletableFuture~Boolean~[]
        +readRecord(subId, topicName) Record
    }

    class PublisherService {
        -Map~String, Publisher~ publishers
        -Map~String, Set~String~~ pubTopics
        +write(record, pubId, topicName) CompletableFuture~Boolean~[]
        +subscribeToTopic(pubId, topicName)
    }

    class SubscriberService {
        -Map~String, Subscriber~ subscribers
        -Map~String, Set~String~~ subTopics
        +subscribe(subId, topicName)
        +readRecord(subId, topicName) Record
    }

    class Topic {
        -String topicName
        -Map~String, Buffer~ buffers
        -ExecutorService executorService
        +write(record) CompletableFuture~Boolean~[]
        +read(subId) Record
        +addBuffer(subId)
        +removeBuffer(subId)
        +shutdown()
    }

    class Buffer {
        -String subId
        -LinkedBlockingQueue~Record~ records
        -CompletableFuture~Boolean~ completableFuture
        +flush(record, executor) CompletableFuture~Boolean~
        +readRecord() Record
    }

    class Publisher {
        <<record>>
        +String id
    }

    class Subscriber {
        <<record>>
        +String id
    }

    class Record {
        <<record>>
        +K notificationKey
        +V notificationValue
        +String pubId
    }

    PubSubFacade --> TopicService
    PubSubFacade --> PublisherService
    PubSubFacade --> SubscriberService
    PublisherService --> TopicService
    SubscriberService --> TopicService
    TopicService --> Topic : 1..*
    Topic --> Buffer : 1 per subscriber
    PublisherService --> Publisher : 1..*
    SubscriberService --> Subscriber : 1..*
```