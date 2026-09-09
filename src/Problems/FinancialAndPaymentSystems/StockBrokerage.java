package Problems.FinancialAndPaymentSystems;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StockBrokerage {
    enum OrderType {
        MarketOrder,
        LimitOrder
    }

    enum OrderStatus {
        Initiated,
        Completed,
        Failed,
        Partial_Complete
    }

    interface Order<T> extends Comparable<T> {
        String getOrderId();

        double getNoOfShares();

        void reduceShares(double shares);

        OrderStatus getStatus();

        void setStatus(OrderStatus status);

        double getAmount();
    }

    interface Observable {
        void notifyClient(Order order, User user);
    }

    interface Observer {
        void updateStateChange(Order order, User user);
    }

    record User(String userId, String userName) {
    }

    record Company(String companyName) {
    }

    static class Buy implements Order<Buy> {
        final String orderId;
        final double amount;
        final OrderType orderType;
        double noOfShares;
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
        public void reduceShares(double shares) {
            this.noOfShares -= shares;
        }

        @Override
        public String getOrderId() {
            return orderId;
        }

        @Override
        public OrderStatus getStatus() {
            return status;
        }

        @Override
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
        final double amount;
        final OrderType orderType;
        double noOfShares;
        OrderStatus status;

        public Sell(String orderId, double noOfShares, double amount, OrderType orderType) {
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
        public void reduceShares(double shares) {
            this.noOfShares -= shares;
        }

        @Override
        public String getOrderId() {
            return orderId;
        }

        @Override
        public OrderStatus getStatus() {
            return status;
        }

        @Override
        public void setStatus(OrderStatus status) {
            this.status = status;
        }

        @Override
        public double getAmount() {
            return amount;
        }

        @Override
        public int compareTo(Sell o) {
            return Double.compare(this.amount, o.amount);
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
            this.shares = new HashMap<>(portfolio.shares);
            this.history = new LinkedHashMap<>(portfolio.history);
        }

        public void updateBalance(double money) {
            this.balance += money;
        }

        public void updateValue(double value) {
            this.value = value;
        }

        public void addShare(Company company, double share) {
            shares.merge(company, share, Double::sum);
            if (shares.get(company) == 0d) shares.remove(company);
        }

        public List<Order> getOrders() {
            return new ArrayList<>(this.history.values());
        }

        public void appendOrder(Order order) {
            this.history.putIfAbsent(order.getOrderId(), order);
        }
    }

    static class MarketService {
        ConcurrentHashMap<String, Double> sharePrices;
        Map<Company, Queue<Double>> companyPerformances;

        public MarketService() {
            this.sharePrices = new ConcurrentHashMap<>();
            this.companyPerformances = new ConcurrentHashMap<>();
        }

        public void registerCompany(Company company, double initialPrice) {
            sharePrices.putIfAbsent(company.companyName(), initialPrice);
        }

        public void updateSharePrice(Company company, double sharePrice) {
            sharePrices.compute(company.companyName(), (key, value) -> {
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
        Map<User, Portfolio> portfolios;
        MarketService ms;

        public PortfolioService(MarketService ms) {
            this.ms = ms;
            portfolios = new ConcurrentHashMap<>();
        }

        public void registerUser(User user, double initialBalance) {
            portfolios.putIfAbsent(user, new Portfolio(initialBalance, 0, 0));
        }

        public void updateBalance(User user, double money) {
            portfolios.computeIfPresent(user, (key, value) -> {
                value.updateBalance(money);
                return value;
            });
        }

        public void addOrder(User user, Order order) {
            portfolios.computeIfPresent(user, (key, value) -> {
                value.appendOrder(order);
                return value;
            });
        }

        public Portfolio getPortfolio(User user) {
            return portfolios.get(user);
        }

        public void addShare(User user, Company company, double shares) {
            portfolios.computeIfPresent(user, (key, value) -> {
                value.addShare(company, shares);
                return value;
            });
        }

        public double getUserInvestment(User user) {
            double[] amount = new double[1];
            portfolios.computeIfPresent(user, (key, value) -> {
                for (Map.Entry<Company, Double> entry : value.shares.entrySet()) {
                    amount[0] += ms.getCompanySharePrice(entry.getKey()) * entry.getValue();
                }
                return value;
            });
            return amount[0];
        }
    }

    static class OrderService implements Observable {
        MarketService ms;
        PortfolioService ps;
        Map<Company, PriorityQueue<Order<Buy>>> buys;
        Map<Company, PriorityQueue<Order<Sell>>> sells;
        Map<String, Order<Buy>> buyOrders;
        Map<String, Order<Sell>> sellOrders;
        Map<Company, ConcurrentLinkedQueue<Order<Buy>>> buyStaging = new ConcurrentHashMap<>();
        Map<Company, ConcurrentLinkedQueue<Order<Sell>>> sellStaging = new ConcurrentHashMap<>();
        Map<String, User> orderToUser;
        Map<Company, Lock> companyLocks;
        ExecutorService executors;
        Set<Observer> observers;

        public OrderService(MarketService ms, PortfolioService ps) {
            this.ms = ms;
            this.ps = ps;
            this.buys = new ConcurrentHashMap<>();
            this.sells = new ConcurrentHashMap<>();
            this.buyOrders = new ConcurrentHashMap<>();
            this.sellOrders = new ConcurrentHashMap<>();
            this.orderToUser = new ConcurrentHashMap<>();
            this.observers = ConcurrentHashMap.newKeySet();
            this.companyLocks = new ConcurrentHashMap<>();
            AtomicInteger thCnt = new AtomicInteger(0);
            this.executors = Executors.newFixedThreadPool(10, (runnable) -> {
                Thread th = new Thread(runnable, "Order daemon Thread - " + thCnt.getAndIncrement());
                th.setDaemon(true);
                return th;
            });
        }

        private Lock getCompanyLock(Company company) {
            return companyLocks.computeIfAbsent(company, k -> new ReentrantLock());
        }

        public Order<Buy> createBuyOrder(User user, Company company, String orderId,
                                         OrderType orderType, double noOfShares, double amt) {
            Order<Buy> buyOrder;
            switch (orderType) {
                case MarketOrder ->
                        buyOrder = new Buy(orderId, noOfShares, ms.getCompanySharePrice(company), OrderType.MarketOrder);
                case LimitOrder -> buyOrder = new Buy(orderId, noOfShares, amt, OrderType.LimitOrder);
                default -> throw new IllegalArgumentException(orderType + " not supported");
            }

            buyStaging.computeIfAbsent(company, k -> new ConcurrentLinkedQueue<>()).add(buyOrder);

            buyOrders.putIfAbsent(orderId, buyOrder);
            orderToUser.putIfAbsent(orderId, user);
            ps.addOrder(user, buyOrder);
            triggerMatching(company);
            return buyOrder;
        }

        public Order<Sell> createSellOrder(User user, Company company, String orderId,
                                           OrderType orderType, double noOfShares, double amt) {
            Order<Sell> sellOrder;
            switch (orderType) {
                case MarketOrder ->
                        sellOrder = new Sell(orderId, noOfShares, ms.getCompanySharePrice(company), OrderType.MarketOrder);
                case LimitOrder -> sellOrder = new Sell(orderId, noOfShares, amt, OrderType.LimitOrder);
                default -> throw new IllegalArgumentException(orderType + " not supported");
            }
            sellStaging.computeIfAbsent(company, k -> new ConcurrentLinkedQueue<>()).add(sellOrder);

            sellOrders.putIfAbsent(orderId, sellOrder);
            orderToUser.putIfAbsent(orderId, user);
            ps.addOrder(user, sellOrder);
            triggerMatching(company);
            return sellOrder;
        }

        @Override
        public void notifyClient(Order order, User user) {
            for (Observer ob : observers) {
                ob.updateStateChange(order, user);
            }
        }

        private void triggerMatching(Company company) {
            this.executors.submit(() -> {
                drainStaging(company);
                Lock lock = getCompanyLock(company);
                if (lock.tryLock()) {
                    try {
                        matchOrder(company);
                    } finally {
                        lock.unlock();
                        if (hasMatchableOrders(company)) {
                            triggerMatching(company);
                        }
                    }
                }
            });
        }

        private void drainStaging(Company company) {
            ConcurrentLinkedQueue<Order<Buy>> buyQ = buyStaging.get(company);
            if (buyQ != null) {
                Order<Buy> o;
                while ((o = buyQ.poll()) != null) {
                    buys.computeIfAbsent(company, k -> new PriorityQueue<>()).add(o);
                }
            }

            ConcurrentLinkedQueue<Order<Sell>> sellQ = sellStaging.get(company);
            if (sellQ != null) {
                Order<Sell> o;
                while ((o = sellQ.poll()) != null) {
                    sells.computeIfAbsent(company, k -> new PriorityQueue<>()).add(o);
                }
            }
        }

        private boolean hasMatchableOrders(Company company) {
            PriorityQueue<Order<Buy>> buyQ = buys.get(company);
            PriorityQueue<Order<Sell>> sellQ = sells.get(company);
            if (buyQ == null || buyQ.isEmpty() || sellQ == null || sellQ.isEmpty()) return false;
            return buyQ.peek().getAmount() >= sellQ.peek().getAmount();
        }

        private void matchOrder(Company company) {
            PriorityQueue<Order<Buy>> buyQ = buys.get(company);
            PriorityQueue<Order<Sell>> sellQ = sells.get(company);
            if (buyQ == null || sellQ == null) return;

            while (!buyQ.isEmpty() && !sellQ.isEmpty()) {
                if (buyQ.peek().getAmount() < sellQ.peek().getAmount()) break;

                Order<Buy> buyOrder = buyQ.poll();
                Order<Sell> sellOrder = sellQ.poll();
                double matched = Math.min(buyOrder.getNoOfShares(), sellOrder.getNoOfShares());
                double price = sellOrder.getAmount();

                User buyer = orderToUser.get(buyOrder.getOrderId());
                User seller = orderToUser.get(sellOrder.getOrderId());

                try {
                    ps.updateBalance(buyer, -(matched * price));
                    ps.addShare(buyer, company, matched);

                    ps.updateBalance(seller, matched * price);
                    ps.addShare(seller, company, -matched);

                    ms.updateSharePrice(company, price);

                    if (Math.abs(matched - buyOrder.getNoOfShares()) < 1e-9) {
                        buyOrder.setStatus(OrderStatus.Completed);
                        notifyClient(buyOrder, buyer);
                    } else {
                        buyOrder.reduceShares(matched);
                        buyOrder.setStatus(OrderStatus.Partial_Complete);
                        buyQ.offer(buyOrder);
                    }

                    if (Math.abs(matched - sellOrder.getNoOfShares()) < 1e-9) {
                        sellOrder.setStatus(OrderStatus.Completed);
                        notifyClient(sellOrder, seller);
                    } else {
                        sellOrder.reduceShares(matched);
                        sellOrder.setStatus(OrderStatus.Partial_Complete);
                        sellQ.offer(sellOrder);
                    }

                } catch (Exception e) {
                    ps.updateBalance(buyer, matched * price);
                    ps.addShare(buyer, company, -matched);
                    ps.updateBalance(seller, -(matched * price));
                    ps.addShare(seller, company, matched);

                    buyOrder.setStatus(OrderStatus.Failed);
                    sellOrder.setStatus(OrderStatus.Failed);
                    buyQ.offer(buyOrder);
                    sellQ.offer(sellOrder);
                    notifyClient(buyOrder, buyer);
                    notifyClient(sellOrder, seller);
                    break;
                }
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

        public void registerCompany(Company company, double initialPrice) {
            marketService.registerCompany(company, initialPrice);
        }

        public void registerUser(User user, double initialBalance) {
            portfolioService.registerUser(user, initialBalance);
        }

        public Portfolio getPortfolio(User user) {
            return portfolioService.getPortfolio(user);
        }

        public List<Order> getOrderHistory(User user) {
            return portfolioService.getPortfolio(user).getOrders();
        }

        public Double getSharePrice(Company company) {
            return marketService.getCompanySharePrice(company);
        }

        public Map.Entry<Company, List<Double>> getCompanyPerformance(Company company) {
            return marketService.getCompanyPerformance(company);
        }

        public Order<Buy> placeBuyOrder(User user, Company company, String orderId,
                                        OrderType orderType, double shares, double limitPrice) {
            return orderService.createBuyOrder(user, company, orderId, orderType, shares, limitPrice);
        }

        public Order<Sell> placeSellOrder(User user, Company company, String orderId,
                                          OrderType orderType, double shares, double limitPrice) {
            return orderService.createSellOrder(user, company, orderId, orderType, shares, limitPrice);
        }

        public void registerObserver(Observer observer) {
            orderService.observers.add(observer);
        }

        public void removeObserver(Observer observer) {
            orderService.observers.remove(observer);
        }
    }
}