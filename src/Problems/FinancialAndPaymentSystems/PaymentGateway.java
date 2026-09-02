package Problems.FinancialAndPaymentSystems;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 Functional Requirements
 * payment type supported - credt/debit card, upi
 * retrial mechanism if payment fails
 * when payment succeeds, system should notify the customer and merchant about the status update

 Non-functional Requirements
 * classes should be modular, following OODs principle
 * class should be extensible to handle future features easily
 * system should be scalable to handle increasing no of users

 */
public class PaymentGateway {

    record CardDetails(String cardNo, String expiryDate, String cvv){}
    static class Response{
        int code;
        String body;

        public Response(int code, String body) {
            this.code = code;
            this.body = body;
        }
    }
    interface Stripe {
        Response pay(Double mat, String upiId);
        Response pay(Double mat, CardDetails cardDetails);
    }
    interface Payment {
        Response makePayment(Double amt, Stripe stripe);
    }
    static class CardPayment implements Payment{
        CardDetails cardDetails;

        public CardPayment(CardDetails cardDetails) {
            this.cardDetails = cardDetails;
        }

        public Response makePayment(Double amt, Stripe stripe) {
            return stripe.pay(amt, this.cardDetails);
        }
    }
    static class UpiPayment implements Payment{
        String upiId;

        public UpiPayment(String upiId) {
            this.upiId = upiId;
        }

        public Response makePayment(Double amt, Stripe stripe) {
            return stripe.pay(amt, this.upiId);
        }
    }

    static class RetryPayment implements Payment {
        private final Payment delegate;
        private final int maxAttempts;

        public RetryPayment(Payment delegate, int maxAttempts) {
            this.delegate = delegate;
            this.maxAttempts = maxAttempts;
        }

        public Response makePayment(Double amt, Stripe stripe) {
            Response response = new Response(500, "internal server error");
            for (int i = 0; i < maxAttempts; i++) {
                response = delegate.makePayment(amt, stripe);
                if (response.code == 201) return response;
            }
            return response;
        }
    }
    interface Observer{
        void paymentStatus(String mssg);
    }
    enum MessageType {
        CUSTOMER,
        MERCHANT;
    }
    static class UserDisplay implements Observer{

        @Override
        public void paymentStatus(String mssg) {
            System.out.println(mssg);
        }
    }
    static class MerchantDisplay implements Observer{

        @Override
        public void paymentStatus(String mssg) {
            System.out.println(mssg);
        }
    }
    interface Observable {
        void notifyAllObservers(String mssg, MessageType messageType);
    }
    record User(String userName, String userEmailId){};
    record Merchant(String merchantName, String merchantContact){};
    static class PaymentGatewayService implements Observable {
        Map<String, CompletableFuture<Response>> payments;
        Map<String, Map.Entry<User, Merchant>> orders;
        Stripe stripe;
        ExecutorService executor;
        Map<MessageType, Set<Observer>> observersByType;

        static final int MAX_ATTEMPT = 3;

        public PaymentGatewayService(Stripe stripe) {
            payments = new ConcurrentHashMap<>();
            orders = new ConcurrentHashMap<>();
            this.stripe = stripe;
            this.observersByType = new ConcurrentHashMap<>();
            this.executor = Executors.newFixedThreadPool(10, runnable -> {
                Thread th = new Thread(runnable, "payment daemon thread");
                th.setDaemon(true);
                return th;
            });
        }

        public CompletableFuture<Response> makePayment(CardDetails cardDetails, String orderId,
                                                       User user, Merchant merchant, Double amt) {
            boolean[] isNew = {false};
            CompletableFuture<Response> future = payments.computeIfAbsent(orderId, key -> {
                isNew[0] = true;
                return new CompletableFuture<>();
            });

            if (isNew[0]) {
                orders.put(orderId, Map.entry(user, merchant));
                CompletableFuture<Response> actual = submitAndNotify(
                        new RetryPayment(new CardPayment(cardDetails), 3), amt, orderId);
                actual.whenComplete((resp, ex) -> {
                    if (ex != null) future.completeExceptionally(ex);
                    else future.complete(resp);
                });
            }

            return future;
        }

        public CompletableFuture<Response> makePayment(String upiId, String orderId,
                                                       User user, Merchant merchant, Double amt) {
            boolean[] isNew = {false};
            CompletableFuture<Response> future = payments.computeIfAbsent(orderId, key -> {
                isNew[0] = true;
                return new CompletableFuture<>();
            });

            if (isNew[0]) {
                orders.put(orderId, Map.entry(user, merchant));
                CompletableFuture<Response> actual = submitAndNotify(
                        new RetryPayment(new UpiPayment(upiId), 3), amt, orderId);
                actual.whenComplete((resp, ex) -> {
                    if (ex != null) future.completeExceptionally(ex);
                    else future.complete(resp);
                });
            }

            return future;
        }

        private CompletableFuture<Response> submitAndNotify(Payment payment, Double amt, String orderId) {
            return CompletableFuture
                    .supplyAsync(() -> payment.makePayment(amt, this.stripe), this.executor)
                    .thenApply(response -> {
                        Map.Entry<User, Merchant> pair = orders.get(orderId);
                        String status = response.code == 201 ? "SUCCESS" : "FAILED";
                        notifyAllObservers(
                                "Order " + orderId + " " + status + " for " + pair.getKey().userName(),
                                MessageType.CUSTOMER);
                        notifyAllObservers(
                                "Order " + orderId + " " + status + " from " + pair.getKey().userName(),
                                MessageType.MERCHANT);
                        return response;
                    });
        }

        @Override
        public void notifyAllObservers(String mssg, MessageType messageType) {
            observersByType.getOrDefault(messageType, Set.of()).forEach(ob -> ob.paymentStatus(mssg));
        }
    }

    static class PaymentFacade {
        private static volatile PaymentFacade INSTANCE;
        private final PaymentGatewayService service;

        private PaymentFacade(Stripe stripe) {
            this.service = new PaymentGatewayService(stripe);
        }

        public static PaymentFacade getInstance(Stripe stripe) {
            if (INSTANCE == null) {
                synchronized (PaymentFacade.class) {
                    if (INSTANCE == null) {
                        INSTANCE = new PaymentFacade(stripe);
                    }
                }
            }
            return INSTANCE;
        }

        public CompletableFuture<Response> payViaCard(String orderId, User user, Merchant merchant,
                                                      Double amt, CardDetails cardDetails) {
            return service.makePayment(cardDetails, orderId, user, merchant, amt);
        }

        public CompletableFuture<Response> payViaUpi(String orderId, User user, Merchant merchant,
                                                     Double amt, String upiId) {
            return service.makePayment(upiId, orderId, user, merchant, amt);
        }

        public void registerObserver(Observer observer, MessageType messageType) {
            this.service.observersByType.computeIfAbsent(messageType, x -> ConcurrentHashMap.newKeySet()).add(observer);
        }
    }

}
