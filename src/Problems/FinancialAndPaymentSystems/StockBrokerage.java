package Problems.FinancialAndPaymentSystems;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Functional Requirements:
 * Users can buy/sell orders at market or limit order
 * Users can track his history
 * Notify users when order transaction is complete(buy/sell achieved)
 * User portfolio - total amt invested, current value, remaining amount
 * <p>
 * Non-functional requirements:
 * classes should be modular and follow OOD principle for easier testing of components
 * classes should be extensible to accommodate new features
 * system should be scalable to handle growing no of users
 * system should handle concurrent requests
 */
public class StockBrokerage {
    enum OrderType {
        MarketOrder,
        LimitOrder;
    }

    enum OrderStatus {
        Initiated,
        Completed,
        Failed;
    }

    interface Order<T> extends Comparable<T> {
        String getOrderId();

        double getNoOfShares();

        void setStatus(OrderStatus status);

        double getAmount();
    }

    interface Observer {
        void notifyClient(String orderId, User user, String mssg);
    }

    record User(String userId, String userName) {
    }

    static class Buy implements Order<Buy> {
        final String orderId;
        final double noOfShares;
        final double amount;
        final OrderType orderType;
        OrderStatus status;

        public Buy(String orderId, double noOfShares, double amount, OrderType orderType) {
            this.orderId = orderId;
            this.noOfShares = noOfShares;
            this.amount = amount;
            this.orderType = orderType;
            this.status = OrderStatus.Initiated;
        }

        @Override
        public double getNoOfShares() {
            return noOfShares;
        }

        @Override
        public String getOrderId() {
            return this.orderId;
        }

        public void setStatus(OrderStatus status) {
            this.status = status;
        }

        @Override
        public double getAmount() {
            return amount;
        }

        @Override
        public int compareTo(Buy o) {
            return Double.compare(o.amount, this.amount);
        }
    }

    static class Sell implements Order<Sell> {
        final String orderId;
        final double noOfShares;
        final double amount;
        final OrderType orderType;
        OrderStatus status;

        public Sell(String orderId, double noOfShares, double amount, OrderType orderType) {
            this.orderId = orderId;
            this.noOfShares = noOfShares;
            this.amount = amount;
            this.orderType = orderType;
            this.status = OrderStatus.Initiated;
        }

        public void setStatus(OrderStatus status) {
            this.status = status;
        }

        @Override
        public double getNoOfShares() {
            return noOfShares;
        }

        @Override
        public String getOrderId() {
            return this.orderId;
        }

        @Override
        public int compareTo(Sell o) {
            return Double.compare(this.amount, o.amount);
        }

        @Override
        public double getAmount() {
            return amount;
        }

    }

    static class Portfolio {
        double balance;
        double invested;
        double value;
        Map<Company, Double> shares;
        Map<String, Order> history;

        public Portfolio(double balance, double invested, double value) {
            this.balance = balance;
            this.invested = invested;
            this.value = value;
            this.shares = new HashMap<>();
            this.history = new LinkedHashMap<>();
        }

        public Portfolio(Portfolio portfolio) {
            this.balance = portfolio.balance;
            this.invested = portfolio.invested;
            this.value = portfolio.value;
            this.shares = portfolio.shares;
            this.history = portfolio.history;
        }

        public void updateBalance(double money) {
            this.balance += balance;
        }

        public void updateInvested(double money) {
            this.invested += invested;
        }

        public void updateValue(double value) {
            this.value = value;
        }

        public void addShare(Company company, double share) {
            shares.put(company, shares.getOrDefault(company, 0d) + share);
        }

        public List<Order> getOrders() {
            return new ArrayList<>(this.history.values());
        }

        public void appendOrder(Order order) {
            this.history.putIfAbsent(order, );
        }

    }

    record Company(String companyName) {
    }

    static class MarketService {
        ConcurrentHashMap<String, Double> sharePrices;
        Map<Company, Queue<Double>> companyPerformances;

        public MarketService() {
            this.sharePrices = new ConcurrentHashMap<>();
            this.companyPerformances = new HashMap<>();
        }

        public void updateSharePrice(Company company, double sharePrice) {
            this.sharePrices.computeIfPresent(company.companyName(), (key, value) -> {
                updateCompanyPerformance(company, sharePrice);
                return sharePrice;
            });
        }

        private void updateCompanyPerformance(Company company, double sharePrice) {
            companyPerformances.computeIfAbsent(company, x -> new LinkedList<>()).offer(sharePrice);
        }

        public Map.Entry<Company, List<Double>> getCompanyPerformance(Company company) {
            return Map.entry(company, new ArrayList<>(companyPerformances.getOrDefault(company, new LinkedList<>())));
        }

        public Double getCompanySharePrice(Company company) {
            return sharePrices.get(company.companyName());
        }
    }

    static class PortfolioService {
        Map<User, AtomicReference<Portfolio>> portfolios;
        MarketService ms;

        public PortfolioService(MarketService ms) {
            this.ms = ms;
            portfolios = new ConcurrentHashMap<>();
        }

        public void updateBalance(User user, double money) {
            portfolios.computeIfPresent(user, (key, value) -> {
                Portfolio old = value.get();
                Portfolio newP = new Portfolio(old);
                newP.updateBalance(money);
                while (!value.compareAndSet(old, newP)) {
                    old = value.get();
                    newP.balance = old.balance;
                    newP.updateBalance(money);
                }
                return value;
            });
        }

        public void updateInvestment(User user, double investment) {
            portfolios.computeIfPresent(user, (key, value) -> {
                Portfolio old = value.get();
                Portfolio newP = new Portfolio(old);
                newP.updateInvested(investment);
                while (!value.compareAndSet(old, newP)) {
                    old = value.get();
                    newP.invested = old.invested;
                    newP.updateInvested(investment);
                }
                return value;
            });
        }

        public void addOrder(User user, Order order) {
            portfolios.computeIfPresent(user, (key, value) -> {
                Portfolio old = value.get();
                Portfolio newP = new Portfolio(old);
                newP.appendOrder(order);
                while (!value.compareAndSet(old, newP)) {
                    old = value.get();
                    newP.history = old.history;
                    newP.appendOrder(order);
                }
                return value;
            });
        }

        public Portfolio getPortfolio(User user) {
            return portfolios.get(user).get();
        }
    }

    static class OrderService {
        MarketService ms;
        PortfolioService ps;
        Map<Company, PriorityQueue<Order<Buy>>> buys;
        Map<Company, PriorityQueue<Order<Sell>>> sells;
        Map<String, Order<Buy>> buyOrders;
        Map<String, Order<Sell>> sellOrders;
        Map<String, User> orderToUser;
        Map<Company, Map.Entry<Lock, Condition>> companyTokens;
        ReentrantReadWriteLock lock;
        ExecutorService executors;
        Set<Observer> observers;

        public OrderService(MarketService ms, PortfolioService ps) {
            this.ms = ms;
            this.ps = ps;
            this.buys = new ConcurrentHashMap<>();
            this.sells = new ConcurrentHashMap<>();
            this.buyOrders = new ConcurrentHashMap<>();
            this.sellOrders = new ConcurrentHashMap<>();
            this.lock = new ReentrantReadWriteLock();
            this.orderToUser = new ConcurrentHashMap<>();
            this.observers = ConcurrentHashMap.newKeySet();
            this.companyTokens = new HashMap<>();
            AtomicReference<Integer> thCnt = new AtomicReference<>(0);
            this.executors = Executors.newFixedThreadPool(10, (runnable) -> {
                Thread th = new Thread(runnable, "Order daemon Thread - " + thCnt.getAndSet(thCnt.get() + 1));
                th.setDaemon(true);
                return th;
            });
        }


        public void cancelOrder(String orderId) {
            this.lock.writeLock().lock();
            orders.get(orderId).setStatus(OrderStatus.Cancelled);
            this.lock.writeLock().unlock();
        }

        public Order<Buy> createBuyOrder(User user, Company company, String orderId, OrderType orderType, double noOfShares, double amt) {
            Order<Buy> buyOrder;

            companyTokens.computeIfAbsent(company, key -> {
                Lock lock = new ReentrantLock();
                Condition condition = lock.newCondition();
                return Map.entry(lock, condition);
            });
            buys.putIfAbsent(company, new PriorityQueue<>());
            switch (orderType) {
                case MarketOrder ->
                        buyOrder = new Buy(orderId, noOfShares, ms.getCompanySharePrice(company), OrderType.MarketOrder);
                case LimitOrder -> buyOrder = new Buy(orderId, noOfShares, amt, OrderType.LimitOrder);
                default -> throw new IllegalArgumentException(orderType + " not supported");
            }
            buys.compute(company, (k, v) -> {
                if (v == null) v = new PriorityQueue<>();
                v.add(buyOrder);
                return v;
            });
            buyOrders.putIfAbsent(orderId, buyOrder);
            orderToUser.putIfAbsent(orderId, user);
            companyTokens.computeIfPresent(company, (k, v) -> {
                v.getValue().signalAll();
                return v;
            });

            return buyOrder;
        }

        private void updateBuyOrderStatus(String orderId, OrderStatus orderStatus) {
            buyOrders.computeIfPresent(orderId, (k, v) -> {
                v.setStatus(orderStatus);
                return v;
            });
        }

        public Order<Sell> createSellOrder(User user, Company company, String orderId, OrderType orderType, double noOfShares, double amt) {
            Order<Sell> sellOrder;

            sells.putIfAbsent(company, new PriorityQueue<>());
            companyTokens.computeIfAbsent(company, key -> {
                Lock lock = new ReentrantLock();
                Condition condition = lock.newCondition();
                return Map.entry(lock, condition);
            });
            switch (orderType) {
                case MarketOrder ->
                        sellOrder = new Sell(orderId, noOfShares, ms.getCompanySharePrice(company), OrderType.MarketOrder);
                case LimitOrder -> sellOrder = new Sell(orderId, noOfShares, amt, OrderType.LimitOrder);
                default -> throw new IllegalArgumentException(orderType + " not supported");
            }
            sells.compute(company, (k, v) -> {
                if (v == null) v = new PriorityQueue<>();
                v.add(sellOrder);
                return v;
            });
            sellOrders.putIfAbsent(orderId, sellOrder);
            orderToUser.putIfAbsent(orderId, user);
            companyTokens.computeIfPresent(company, (k, v) -> {
                v.getValue().signalAll();
                return v;
            });
            return sellOrder;
        }

        private void notifyClient(String orderId, User user, String mssg) {
            for (Observer ob : observers) {
                ob.updateState(orderId, user, mssg);
            }
        }

        private void updateSellOrderStatus(String orderId, OrderStatus orderStatus) {
            sellOrders.computeIfPresent(orderId, (k, v) -> {
                v.setStatus(orderStatus);
                return v;
            });
        }

        public void execute() {

        }

        static class Response {
            final OrderStatus status;
            final int code;
            final String mssg;

            public Response(OrderStatus status, String mssg, int code) {
                this.status = status;
                this.mssg = mssg;
                this.code = code;
            }
        }

    }
    static class StockBrokerageFacade {
        private static volatile StockBrokerageFacade INSTANCE;

        private final MarketService marketService;
        private final PortfolioService portfolioService;
        private final OrderService orderService;

        private StockBrokerageFacade() {
            this.marketService = new MarketService();
            this.portfolioService = new PortfolioService(marketService);
            this.orderService = new OrderService(marketService, portfolioService);
        }

        public static StockBrokerageFacade getInstance() {
            if (INSTANCE == null) {
                synchronized (StockBrokerageFacade.class) {
                    if (INSTANCE == null) {
                        INSTANCE = new StockBrokerageFacade();
                    }
                }
            }
            return INSTANCE;
        }

        // ---- User / Portfolio ----

        public Portfolio getPortfolio(User user) {
            return portfolioService.getPortfolio(user);
        }

        public List<Order> getOrderHistory(User user) {
            return portfolioService.getPortfolio(user).getOrders();
        }

        // ---- Market Data ----

        public Double getSharePrice(Company company) {
            return marketService.getCompanySharePrice(company);
        }

        public Map.Entry<Company, List<Double>> getCompanyPerformance(Company company) {
            return marketService.getCompanyPerformance(company);
        }

        // ---- Order Placement ----

        public Order<Buy> placeBuyOrder(User user, Company company, String orderId,
                                        OrderType orderType, double shares, double limitPrice) {
            return orderService.createBuyOrder(user, company, orderId, orderType, shares, limitPrice);
        }

        public Order<Sell> placeSellOrder(User user, Company company, String orderId,
                                          OrderType orderType, double shares, double limitPrice) {
            return orderService.createSellOrder(user, company, orderId, orderType, shares, limitPrice);
        }

        public void cancelOrder(String orderId) {
            orderService.cancelOrder(orderId);
        }

        // ---- Observer ----

        public void registerObserver(Observer observer) {
            orderService.observers.add(observer);
        }

        public void removeObserver(Observer observer) {
            orderService.observers.remove(observer);
        }
    }

}
