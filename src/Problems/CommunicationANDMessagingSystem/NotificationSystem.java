package Problems.CommunicationANDMessagingSystem;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/*
FRs:
1. notifications supported by system - EMAIL, SMS, PUSH
2. Sends notifications asynchronously
3. Send one notification at a time.
4. The system should send notifications asynchronously.
5. Delivery should be non-blocking, using a thread pool to manage parallel sending.

NFRs:
1. Modular: classes should be well separated.
2. Extensible: class should be extensible for future features.
3. OOD: system should follow OOD principles


 */
public class NotificationSystem {
    enum MessageType {
        PUSH,
        EMAIL,
        SMS;
    }

    static interface Service {
        public MessageType getType();

        public Response publish(Recipient recipient, String content) throws DeliveryException;
    }

    static class Recipient {
        private final String recipientId;
        private final String email;
        private final String mobNo;

        public Recipient(String recipientId, String email, String mobNo) {
            this.recipientId = recipientId;
            this.email = email;
            this.mobNo = mobNo;
        }

        public String getRecipientId() {
            return recipientId;
        }

        public String getEmail() {
            return email;
        }

        public String getMobNo() {
            return mobNo;
        }
    }

    static class Response {
        private final MessageType channel;
        private final int code;
        private final boolean success;
        private final String detail;

        public Response(MessageType channel, int code, boolean success, String detail) {
            this.channel = channel;
            this.code = code;
            this.success = success;
            this.detail = detail;
        }

        public MessageType getChannel() {
            return channel;
        }

        public int getCode() {
            return code;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getDetail() {
            return detail;
        }
    }

    static class EmailService implements Service {
        @Override
        public MessageType getType() {
            return MessageType.EMAIL;
        }

        @Override
        public Response publish(Recipient recipient, String content) {
            return new Response(getType(), 200, true, "message sent");
        }
    }

    static class PushService implements Service {
        @Override
        public MessageType getType() {
            return MessageType.PUSH;
        }

        @Override
        public Response publish(Recipient recipient, String content) {
            return new Response(getType(), 200, true, "message sent");
        }
    }

    static class SMSService implements Service {
        @Override
        public MessageType getType() {
            return MessageType.SMS;
        }

        @Override
        public Response publish(Recipient recipient, String content) {
            return new Response(getType(), 200, true, "message sent");
        }
    }

    static class RetryService implements Service {
        private final Service service;
        private final int max_retry = 3;
        private final int retryDelayMillis = 1000;

        public RetryService(Service service) {
            this.service = service;
        }

        @Override
        public MessageType getType() {
            return null;
        }

        @Override
        public Response publish(Recipient recipient, String content) throws DeliveryException {
            int attempt = 0;

            while (attempt <= this.max_retry) {
                try {
                    return this.service.publish(recipient, content);
                } catch (Exception e) {
                    attempt++;
                    System.out.println("Error: Attempt " + attempt + " failed for notification " + recipient.getRecipientId() + ". Retrying...");
                    if (attempt >= max_retry) {
                        System.out.println(e.getMessage());
                        throw new DeliveryException("Failed to send notification after " + max_retry + " attempts.", e);
                    }
                    try {
                        Thread.sleep(retryDelayMillis);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new DeliveryException("Interrupted during retry backoff.", ie);
                    }
                }
            }
        }

    }

    static class DeliveryException extends Exception {
        public DeliveryException(String msg, Exception e) {
            super(msg, e);
        }
    }

    static class RecipientService {
        private final Map<String, Recipient> recipients;

        public RecipientService() {
            this.recipients = new ConcurrentHashMap<>();
        }

        public Recipient createUser(String recipientId, String email, String mobNo) {
            return recipients.computeIfAbsent(recipientId, id -> new Recipient(id, email, mobNo));
        }

        public Recipient getRecipient(String recipientId) {
            return recipients.get(recipientId);
        }
    }

    static class ServiceFactory {
        private static final Map<MessageType, Service> serviceMap = new ConcurrentHashMap<>();

        public static Service createService(MessageType type) {
            return serviceMap.computeIfAbsent(type, ServiceFactory::buildService);
        }

        private static Service buildService(MessageType type) {
            switch (type) {
                case EMAIL:
                    return new EmailService();
                case SMS:
                    return new SMSService();
                case PUSH:
                    return new PushService();
                default:
                    throw new IllegalArgumentException("Unsupported notification type: " + type);
            }
        }
    }

    static class NotificationService {
        private ExecutorService executor;
        private RecipientService recipientService;

        public NotificationService(RecipientService recipientService) {
            this.recipientService = recipientService;
            this.executor = Executors.newFixedThreadPool(10, (runnable) -> {
                Thread th = new Thread(runnable, "Worker Thread");
                th.setDaemon(true);
                return th;
            });
        }


        public CompletableFuture<List<Response>> publish(List<MessageType> messageTypes, String content, String recipientId) {
            List<CompletableFuture<Response>> futures = messageTypes.stream()
                    .map(messageType -> CompletableFuture.supplyAsync(() -> {
                        Service svc = new RetryService(ServiceFactory.createService(messageType));

                        try {
                            return svc.publish(recipientService.getRecipient(recipientId), content);
                        } catch (DeliveryException e) {
                            throw new RuntimeException(e);
                        }
                    }, executor).exceptionally(ex -> new Response(messageType, 500, false, ex.getMessage())))
                    .toList();
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).
                    thenApply(v -> futures.stream().map(CompletableFuture::join).toList());
        }

        public void shutdown() {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                    executor.awaitTermination(2, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    static class NotificationSystemFacade {
        private static final Object lock = new Object();
        private static volatile NotificationSystemFacade instance = null;
        private NotificationService notificationService;
        private RecipientService recipientService;

        public NotificationSystemFacade() {
            this.recipientService = new RecipientService();
            this.notificationService = new NotificationService(this.recipientService);
        }

        public static NotificationSystemFacade getInstance() {
            if (instance == null) {
                synchronized (lock) {
                    if (instance == null) {
                        instance = new NotificationSystemFacade();
                    }
                }
            }
            return instance;
        }

        public Recipient registerRecipient(String recipientId, String mobNo, String email) {
            return recipientService.createUser(recipientId, email, mobNo);
        }

        public CompletableFuture<List<Response>> publish(String recipientId, String content, List<MessageType> messageTypes) {
            if (recipientService.getRecipient(recipientId) == null)
                return CompletableFuture.failedFuture(new IllegalArgumentException("Unknown recipient"));
            return this.notificationService.publish(messageTypes, content, recipientId);
        }
    }
}
