package Problems.ECommerceAndBooking;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Functional Requirements:
 * Lockers can be of different sizes
 * Locker will be assigned based on product size, if not available then next available locker size will be assigned
 * System manages multiple locker locations
 * Pin code for locker would be shared to customer once it's assigned
 * If customer doesn't pick the order in 3 days then order would be cancelled and returned
 * Locker assignment should be thread safe
 * Non-functional requirements:
 * Classes should be modular and follow OOD principle for clear separation of concern.
 * Classes should be extensible to handle future features
 * Classes should be testable in isolation
 */
public class AmazonLocker {
    enum LockerState {
        EMPTY,
        OCCUPIED
    }

    enum Size {
        SMALL(0),
        MEDIUM(1),
        LARGE(2);
        private final int size;

        Size(int size) {
            this.size = size;
        }

        public int getSize() {
            return size;
        }
    }

    record User(String userId, String userName) {
    }

    record Order(String orderId, Size size, User user) {
    }

    static class Locker {
        String id;
        LockerState lockerState;
        Timestamp orderTimestamp;
        Size size;
        Optional<Order> order;

        public Locker(String id, Size size) {
            this.id = id;
            this.size = size;
            this.lockerState = LockerState.EMPTY;
            order = Optional.empty();
        }

        public Size lockerSize() {
            return size;
        }

        public void addOrder(Order order) {
            if (lockerState == LockerState.OCCUPIED)
                throw new IllegalArgumentException("You cant place an order in Locker whose state is: " + this.lockerState);
            this.order = Optional.of(order);
            this.orderTimestamp = Timestamp.from(Instant.now());
            this.lockerState = LockerState.OCCUPIED;
        }

        public Order emptyLocker() {
            if (lockerState == LockerState.EMPTY) return null;
            if (lockerState == LockerState.OCCUPIED && this.order.isEmpty())
                throw new IllegalArgumentException("Locker can't be empty if state is: " + this.lockerState);
            Order order = this.order.get();
            this.order = Optional.empty();
            this.lockerState = LockerState.EMPTY;
            return order;
        }

        @Override
        public String toString() {
            return "Locker{" +
                    "lockerState=" + lockerState +
                    ", size=" + size +
                    ", order=" + order +
                    '}';
        }
    }
    record Location(double lat, double longi) {}
    static class Warehouse {
        String id;
        Location location;
        Set<Locker>[] lockers;
        int size;

        public Warehouse(String id, Location location, List<Locker> lockers, int size) {
            this.id = id;
            this.size = size;
            this.location = location;
            this.lockers = new Set[size];
            for(int i=0; i<size; i++) {
                this.lockers[i] = new HashSet<>();
            }
            for(Locker locker:lockers) {
                this.lockers[locker.lockerSize().getSize()].add(locker);
            }
        }

        public Set<Locker> lockers(int size) {
            return this.lockers[size];
        }

        public int warehouseSize() {
            return size;
        }
    }
    interface Observer {
        void updateStateChange(String lockerId, Warehouse warehouse, LockerState lockerState, Order order, String pin);
    }
    interface Observable {
        void notifyAllObservers(String lockerId, Warehouse warehouse, LockerState lockerState, Order order, String pin);
    }
    static class PinService {
        Map<String, String> pins = new ConcurrentHashMap<>();

        public String create(String key) {
            return pins.computeIfAbsent(key, x -> UUID.randomUUID().toString().substring(0,6));
        }

        public String expire(String key) {
            return pins.remove(key);
        }
    }

    static class WareHouseService {
        Map<String, Map<Integer, ReentrantLock>> permits;
        Map<String, Warehouse> warehouses;


        public WareHouseService(List<Warehouse> warehouses) {
            this.permits = new HashMap<>();
            this.warehouses = new ConcurrentHashMap<>();
            for(Warehouse warehouse:warehouses) {
                this.warehouses.putIfAbsent(warehouse.id, warehouse);
                permits.computeIfAbsent(warehouse.id, x -> {
                    return Map.of(0, new ReentrantLock(), 1, new ReentrantLock(), 2, new ReentrantLock());
                });
            }
        }

        public void addWarehouse(Warehouse wh) {
            this.warehouses.putIfAbsent(wh.id, wh);
        }

        public Warehouse getWarehouse(String id) {
            return this.warehouses.get(id);
        }

        public Locker assignLock(Warehouse warehouse, Order order) {
            Locker[] res = new Locker[1];
            for(int size= order.size().getSize(); size<=Size.LARGE.getSize(); size++) {
                final int _size = size;
                permits.computeIfPresent(warehouse.id, (key, value) -> {
                    ReentrantLock lock = value.get(_size);
                    lock.lock();
                    Locker locker = null;
                    try {
                        for(Locker l:warehouse.lockers(_size)) {
                        }
                    } catch (Exception ex) {
                        if (locker != null) {
                            locker.emptyLocker();
                            warehouse.lockers(_size).add(locker);
                            lock.unlock();
                        }
                    }
                    return value;
                });
            }
            return res[0];
        }
    }

    static class NotificationService implements Observer {

        @Override
        public void updateStateChange(String lockerId, Warehouse warehouse, LockerState lockerState, Order order, String pin) {
            System.out.println((lockerState == LockerState.OCCUPIED ? lockerId + " is assigned to " + order.user + " with pin " + pin + " at wareHouseId: "+ warehouse.id+ " location: "+ warehouse.location :
                    order.orderId +" of "+ order.user + " has been cancelled and returned as user didn't pick up the order within 3 business day, Locker: "+lockerId+ " wareHouseId: "+ warehouse.id+ " location: "+ warehouse.location));
        }
    }

    static class LockManagerService {
        PinService pinService;
        WareHouseService wareHouseService;
        NotificationService notificationService;

        public LockManagerService(PinService pinService, WareHouseService wareHouseService, NotificationService notificationService, Map<String, Warehouse> warehouses) {
            this.pinService = pinService;
            this.wareHouseService = wareHouseService;
            this.notificationService = notificationService;
        }

        public void placeOrder(Order order, String warehouseId) {

        }

        public void returnOrder() {

        }
    }
}
