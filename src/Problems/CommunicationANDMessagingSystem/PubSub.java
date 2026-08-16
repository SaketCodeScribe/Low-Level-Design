package Problems.CommunicationANDMessagingSystem;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * FRs:
 * 1. Managing of topic (creation & deletion
 * 2. Producer can write mssgs to a topic
 * 3. consumer can subscribe/unsubscribe to a topic
 * 4. Mssgs is broadcaster to every consumer subscribed to the topic
 * 5. Mssg ordering should be respected
 * 6. Producer can write and consumer can read asynchronously
 * <p>
 * NFRs:
 * 1. Modularity: Components should be modular for easier testing and has a clear separation of concern
 * 2. Extensibility: System should be extensible for future features
 * 3. Scalability: System should follow OOD and should be scalable.
 */
public class PubSub {
    record Publisher(String id) {
    }

    record Subscriber(String id) {
    }

    record Record<K, V>(K notificationKey, V notificationValue, String pubId) {
    }

    static class Topic {
        String topicName;
        Map<String, Buffer> buffers;
        ExecutorService executorService;

        public Topic(String topicName) {
            this.topicName = topicName;
            buffers = new ConcurrentHashMap<>();
            executorService = Executors.newFixedThreadPool(4, (runnable) -> {
                Thread th = new Thread(runnable, "Topic daemon thread");
                th.setDaemon(true);
                return th;
            });
        }

        @SuppressWarnings("unchecked")
        public CompletableFuture<Boolean>[] write(Record<String, String> record) {
            CompletableFuture<Boolean>[] futures = buffers.values().
                    stream()
                    .map(buf -> buf.flush(record, executorService))
                    .toArray(CompletableFuture[]::new);
            if (futures.length == 0) return new CompletableFuture[]{CompletableFuture.completedFuture(true)};

            return futures;
        }


        public Record<String, String> read(String subId) {
            Buffer buffer = buffers.get(subId);
            if (buffer != null) return (buffer.readRecord());
            return null;
        }

        public void addBuffer(String subId) {
            buffers.putIfAbsent(subId, new Buffer(subId));
        }

        public void removeBuffer(String subId) {
            buffers.remove(subId);
        }

        public void shutdown() {
            this.executorService.shutdown();
            try {
                this.executorService.awaitTermination(5, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                this.executorService.shutdownNow();
            }
        }

        static class Buffer {
            private final String subId;
            private final Queue<Record<String, String>> records;
            private CompletableFuture<Boolean> completableFuture = CompletableFuture.completedFuture(true);

            public Buffer(String subId) {
                this.subId = subId;
                this.records = new LinkedBlockingQueue<>();
            }

            private CompletableFuture<Boolean> flush(Record<String, String> record, ExecutorService executorService) {
                this.completableFuture = this.completableFuture
                        .thenApplyAsync(
                                res -> this.records.offer(record),
                                executorService);
                return completableFuture;
            }

            private Record<String, String> readRecord() {
                return records.poll();
            }

            @Override
            public String toString() {
                return "Buffer{" +
                        "subId='" + subId + '\'' +
                        '}';
            }
        }
    }

    static class TopicService {
        Map<String, Topic> topics;

        public TopicService() {
            topics = new ConcurrentHashMap<>();
        }

        public void addTopic(String topicName) {
            topics.putIfAbsent(topicName, new Topic(topicName));
        }

        public void delete(String topicName) {
            Topic topic = topics.remove(topicName);
            if (topic != null) topic.shutdown();
        }

        public void deleteConnectivity(String subId, String topicName) {
            topics.computeIfPresent(topicName, (key, value) -> {
                value.removeBuffer(subId);
                return value;
            });
        }

        public void subscribe(String topicName, String subId) {
            topics.computeIfPresent(topicName, (key, value) -> {
                value.addBuffer(subId);
                return value;
            });
        }

        public CompletableFuture<Boolean>[] writeRecord(Record<String, String> record, String topicName) {
            AtomicReference<CompletableFuture<Boolean>[]> futures = new AtomicReference<>(new CompletableFuture[]{CompletableFuture.completedFuture(false)});
            topics.computeIfPresent(topicName, (key, value) -> {
                futures.set(value.write(record));
                return value;
            });
            return futures.get();
        }

        public Record<String, String> readRecord(String subId, String topicName) {
            AtomicReference<Record<String, String>> record = new AtomicReference<>();
            topics.computeIfPresent(topicName, (key, value) -> {
                record.set(value.read(subId));
                return value;
            });
            return record.get();
        }

        public Topic getTopic(String topicName) {
            return topics.get(topicName);
        }

    }

    static class PublisherService {
        private final TopicService topicService;
        Map<String, Publisher> publishers;
        Map<String, Set<String>> pubTopics;

        public PublisherService(TopicService topicService) {
            this.publishers = new ConcurrentHashMap<>();
            this.topicService = topicService;
            this.pubTopics = new ConcurrentHashMap<>();
        }

        public void createPublisher(String pubId) {
            publishers.putIfAbsent(pubId, new Publisher(pubId));
        }

        public void removePublisher(String pubId) {
            publishers.remove(pubId);
            pubTopics.remove(pubId);
        }

        public CompletableFuture<Boolean>[] write(Record<String, String> record, String pubId, String topicName) {
            AtomicReference<CompletableFuture<Boolean>[]> futures = new AtomicReference<>();
            pubTopics.compute(pubId, (key, value) -> {
                if (value == null) throw new IllegalStateException("Publisher not registered: " + pubId);
                if (!value.contains(topicName))
                    throw new IllegalArgumentException(
                            String.format("Publisher %s is not bound to topic %s", pubId, topicName));
                futures.set(topicService.writeRecord(record, topicName));
                return value;
            });
            return futures.get();
        }

        public void subscribeToTopic(String pubId, String topicName) {
            pubTopics.compute(pubId, (key, value) -> {
                if (value == null) value = ConcurrentHashMap.newKeySet();
                Topic topic = topicService.getTopic(topicName);
                if (topic != null) value.add(topicName);
                return value;
            });
        }

        public void unsubscribeToTopic(String pubId, String topicName) {
            pubTopics.computeIfPresent(pubId, (key, value) -> {
                value.remove(topicName);
                return value;
            });
        }
    }

    static class SubscriberService {
        private final TopicService topicService;
        Map<String, Subscriber> subscribers;
        Map<String, Set<String>> subTopics;

        public SubscriberService(TopicService topicService) {
            this.subscribers = new ConcurrentHashMap<>();
            this.topicService = topicService;
            this.subTopics = new ConcurrentHashMap<>();
        }

        public void createConnectivity(String subId) {
            subscribers.putIfAbsent(subId, new Subscriber(subId));
        }

        public void removeConnectivity(String subId, String topicName) {
            subTopics.computeIfPresent(subId, (key, value) -> {
                topicService.deleteConnectivity(key, topicName);
                value.remove(topicName);
                if (value.isEmpty()) {
                    subscribers.remove(key);
                    return null;
                }
                return value;
            });
        }

        public void subscribe(String subId, String topicName) {
            subTopics.compute(subId, (key, value) -> {
                if (value == null) value = ConcurrentHashMap.newKeySet();
                if (topicService.getTopic(topicName) != null) {
                    topicService.subscribe(topicName, subId);
                    value.add(topicName);
                }
                return value;
            });
        }

        public void unsubscribe(String subId, String topicName) {
            subTopics.computeIfPresent(subId, (key, value) -> {
                value.remove(topicName);
                return value;
            });
        }

        public Record<String, String> readRecord(String subId, String topicName) {
            AtomicReference<Record<String, String>> record = new AtomicReference<>();
            subTopics.computeIfPresent(subId, (key, value) -> {
                record.set(topicService.readRecord(subId, topicName));
                return value;
            });
            return record.get();
        }
    }

    static class PubSubFacade {
        private static volatile PubSubFacade INSTANCE;

        private final TopicService topicService;
        private final PublisherService publisherService;
        private final SubscriberService subscriberService;

        private PubSubFacade() {
            this.topicService = new TopicService();
            this.publisherService = new PublisherService(topicService);
            this.subscriberService = new SubscriberService(topicService);
        }

        public static PubSubFacade getInstance() {
            if (INSTANCE == null) {
                synchronized (PubSubFacade.class) {
                    if (INSTANCE == null) {
                        INSTANCE = new PubSubFacade();
                    }
                }
            }
            return INSTANCE;
        }

        // ---- Topic management ----
        public void createTopic(String topicName) {
            topicService.addTopic(topicName);
        }

        public void deleteTopic(String topicName) {
            topicService.delete(topicName);
        }

        // ---- Publisher lifecycle ----
        public void registerPublisher(String pubId) {
            publisherService.createPublisher(pubId);
        }

        public void deregisterPublisher(String pubId) {
            publisherService.removePublisher(pubId);
        }

        public void bindPublisherToTopic(String pubId, String topicName) {
            publisherService.subscribeToTopic(pubId, topicName);
        }

        public void unbindPublisherFromTopic(String pubId, String topicName) {
            publisherService.unsubscribeToTopic(pubId, topicName);
        }

        public CompletableFuture<Boolean>[] publish(String pubId, String topicName, String key, String value) {
            Record<String, String> record = new Record<>(key, value, pubId);
            return publisherService.write(record, pubId, topicName);
        }

        // ---- Subscriber lifecycle ----
        public void registerSubscriber(String subId) {
            subscriberService.createConnectivity(subId);
        }

        public void subscribe(String subId, String topicName) {
            subscriberService.subscribe(subId, topicName);
        }

        public void unsubscribe(String subId, String topicName) {
            subscriberService.unsubscribe(subId, topicName);
        }

        public void deregisterSubscriber(String subId, String topicName) {
            subscriberService.removeConnectivity(subId, topicName);
        }

        public Record<String, String> consume(String subId, String topicName) {
            return subscriberService.readRecord(subId, topicName);
        }
    }

}
