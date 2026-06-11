package Problems.ManagementStates.ParkingLot;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * FRs
 * 1. system needs to show/handle parking spaces to user
 * 2. system should support diff types of vehicle
 * 3. diff parking spot attracts different cost/hr
 * 4. floor in a building & parking space in a floor should be configurable
 * 5. A user can book any parking space if available
 * 6. Parking Monitor to observe any changes in Parking Space
 *
 * NFRs
 * 1. system should be maintainable by proper use of ood
 * 2. system should be flexible & extensible
 * 3. system should handle race conditions
 * 4. Basic validation - larger vehicle booking smaller parking space
 */
public class ParkingLot {
    enum VehicleType{
        TwoWheeler,
        FourWheeler,
        Truck;
    }
    enum ParkingType{
        Compact,
        Regular,
        Large;
    }
    enum ParkingState{
        Available,
        Occupied,
        OutOfOrder;
    }
    static class ParkingSpace{
        String parkingId;
        ParkingType type;
        ParkingState state;
        public ParkingSpace(String parkingId, ParkingType type, ParkingState state) {
            this.parkingId = parkingId;
            this.type = type;
            this.state = state;
        }

        public void setState(ParkingState state) {
            this.state = state;
        }

        public String getParkingId() {
            return parkingId;
        }

        public ParkingType getType() {
            return type;
        }

        public ParkingState getState() {
            return state;
        }
        public ParkingSpace clone(ParkingState state){
            return new ParkingSpace(this.parkingId, this.type, state);
        }
    }
    static class Floor{
        int floorNo;
        Map<String, AtomicReference<ParkingSpace>> parkingSpaces;
        Set<ParkingObserver> parkingSet;


        public Floor(int floorNo, List<ParkingSpace> parkingSpaces) {
            this.floorNo = floorNo;
            this.parkingSpaces = new HashMap<>();
            parkingSpaces.forEach(ps -> this.parkingSpaces.putIfAbsent(ps.getParkingId(), new AtomicReference<>(ps)));
            parkingSet = ConcurrentHashMap.newKeySet();
        }
        public ParkingType getParkingType(String parkingId){
            return parkingSpaces.get(parkingId).get().getType();
        }
        public List<ParkingSpace> getParkingSpace(Set<ParkingType> pt){
            List<ParkingSpace> ps = new ArrayList<>();
            parkingSpaces.values().forEach(av -> {
                var v = av.get();
                if (pt == null || pt.contains(v.type)){
                    ps.add(v);
                }
            });
            return ps;
        }
        public ParkingSpace changeParkingState(String parkingId, ParkingState ps){
            if (!parkingSpaces.containsKey(parkingId)) return null;
            var p = parkingSpaces.get(parkingId);
            if (ps == ParkingState.OutOfOrder){
                synchronized (p){
                    var s = p.get();
                    if (s.state == ParkingState.Available){
                        p.set(s.clone(ps));
                        return s;
                    }
                    else return null;
                }
            }
            var oldValue = p.get();
            if (oldValue.getState() == ParkingState.OutOfOrder) return null;
            if (ps == ParkingState.Occupied && oldValue.getState() != ParkingState.Available) {
                return null;
            };
            if (p.compareAndSet(oldValue, oldValue.clone(ps))){
                return p.get();
            }
            return null;
        }

        private void notifyObserver(ParkingSpace v, String msg) {
            for(ParkingObserver ob:parkingSet){
                ob.update(v, msg);
            }
        }
        public void add(ParkingObserver ob){
            parkingSet.add(ob);
        }
        public void remove(ParkingObserver ob){
            parkingSet.remove(ob);
        }

        public boolean validate(String parkingId, VehicleType vt) {
            ParkingType type = parkingSpaces.get(parkingId).get().getType();
            switch (vt){
                case Truck -> {
                    return type == ParkingType.Large;
                }
                case FourWheeler -> {
                    return type != ParkingType.Compact;
                }
                case TwoWheeler -> {
                    return true;
                }
                default -> throw new RuntimeException(vt+" is unrecognized");
            }
        }
    }
    static interface ParkingObserver{
        void update(ParkingSpace ps, String msg);
    }
    static class ParkingSpaceMonitor implements ParkingObserver{
        @Override
        public void update(ParkingSpace ps, String msg) {
            display(ps.parkingId, msg);
        }
        public void display(String id, String msg){
            System.out.printf("%s: %s%n", msg, id);
        }
    }
    static class Ticket{
        int floorNo;
        String parkingId;
        String bookingId;
        long checkedIn;

        public Ticket(int floorNo, String parkingId, String bookingId, long checkedIn) {
            this.floorNo = floorNo;
            this.parkingId = parkingId;
            this.bookingId = bookingId;
            this.checkedIn = checkedIn;
        }
    }
    static class ParkingController{
        Floor floor;
        ConcurrentHashMap<ParkingType, Double> price;
        ConcurrentHashMap<String, Ticket> bookedSpace;

        public ParkingController(Floor floor, ConcurrentHashMap<ParkingType, Double> price, Map<String, Long> bookedSpace) {
            this.floor = floor;
            this.price = price;
            this.bookedSpace = new ConcurrentHashMap<>();
        }

        public List<ParkingSpace> fetchParkingSpace(Set<ParkingType> pt){
            return floor.getParkingSpace(pt);
        }

        public Ticket book(String bookingId, String parkingId, VehicleType vt){
            Ticket t = null;
            if (floor.validate(parkingId, vt)){
                ParkingSpace ps = floor.changeParkingState(parkingId, ParkingState.Occupied);
                if (ps != null) {
                    t = new Ticket(floor.floorNo, ps.parkingId, bookingId, System.currentTimeMillis());
                    bookedSpace.put(t.bookingId, t);
                }
            }
            return t;
        }

        public double free(Ticket ticket){
            ParkingSpace ps = floor.changeParkingState(ticket.parkingId, ParkingState.Available);
            double cost = computeCost(ticket);
            bookedSpace.remove(ticket.bookingId);
            return cost;
        }

        private double computeCost(Ticket ticket) {
            double elapsed = System.currentTimeMillis() - ticket.checkedIn;
            ParkingType pt = floor.getParkingType(ticket.parkingId);
            double rate = price.get(pt);
            return rate * Math.ceil(elapsed/(60*60*1000d));
        }
    }

    List<ParkingController> controller;

    public ParkingLot(List<ParkingController> controller) {
        this.controller = controller;
    }

    public void fetchParkingSpace(int floorNo, Set<ParkingType> pt){
        controller.get(floorNo).fetchParkingSpace(pt);
    }
    public Ticket book(String bookingId, int floorNo, String parkingId, VehicleType vt){
        Ticket t = controller.get(floorNo).book(bookingId, parkingId, vt);
        if (t == null){
            System.out.println("Parking space is unavailable! try again");
        }
        return t;
    }
    public double exit(Ticket ticket){
        return controller.get(ticket.floorNo).free(ticket);
    }
}
