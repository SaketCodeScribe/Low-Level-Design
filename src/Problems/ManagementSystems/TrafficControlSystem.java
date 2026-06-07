package Problems.ManagementSystems;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

public class TrafficControlSystem {
    enum VehicleType {
        TWO_WHEELER,
        FOUR_WHEELER;
    }

    enum TrafficState {
        RED(4000),
        YELLOW(100),
        GREEN(1000);
        private final long duration;
        TrafficState(long d) {
            duration = d;
        }
        public long getDuration(){return this.duration;}
    }

    enum Direction {
        NS,
        EW,
        WE,
        SN,
        NE,
        NW,
        EN,
        ES,
        SW,
        SE;
        public Direction next() {

            switch (this) {

                case NS: return SN;

                case SN: return NS;

                case EW: return WE;

                case WE: return EW;

                case NE: return SE;

                case SE: return NE;

                case NW: return SW;

                case SW: return NW;

                case EN: return ES;

                case ES: return EN;

                default: throw new IllegalStateException();

            }

        }
    }

    static class Intersection implements Delayed{
        Direction direction;
        TrafficState state;
        long lastStateChangeTime;

        public Intersection(Direction d, TrafficState state) {
            this.direction = d;
            this.state = state;
            lastStateChangeTime = System.currentTimeMillis();
        }
        public void updateLastChangedTime(long delay){
            lastStateChangeTime = System.currentTimeMillis() + delay;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long elapsed = lastStateChangeTime - System.currentTimeMillis();
            return unit.convert(elapsed, unit);
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.compare(this.getDelay(TimeUnit.MILLISECONDS),o.getDelay(TimeUnit.MILLISECONDS));
        }
    }

    static class Vehicle {
        private final VehicleType type;

        public Vehicle(VehicleType type) {
            this.type = type;
        }
    }
    interface Metrics{}
    static class CongestionMetrics implements Metrics{
        private final int noOfVehicles;
        private final String laneId;

        public CongestionMetrics(Lane lane) {
            this.noOfVehicles = lane.queue.stream().map(LinkedBlockingQueue::size).reduce(Integer::sum).orElse(0);
            this.laneId = lane.laneId;
        }

        @Override
        public String toString() {
            return "CongestionMetrics{" +
                    "noOfVehicles=" + noOfVehicles +
                    ", lane=" + laneId +
                    '}';
        }
    }

    interface TrafficObservers{
        void updateStateChange(Metrics metrics);
    }


    static class Lane{
        private final int noOfLanes;
        private final String laneId;
        List<LinkedBlockingQueue<Vehicle>> queue;
        AtomicReference<TrafficState> state;
        private final Direction laneType;

        Set<TrafficObservers> observers = new HashSet<>();

        public Lane(String id, int noOfLanes, Direction type) {
            this.laneId = id;
            this.noOfLanes = noOfLanes;
            this.laneType = type;
            this.state.set(TrafficState.RED);
            queue = new ArrayList<>(noOfLanes);
            for (int i = 0; i < noOfLanes; i++) {
                queue.add(new LinkedBlockingQueue<>());
                final int index = i;
                Thread th = new Thread(() -> {
                    while(!Thread.currentThread().isInterrupted()){
                        try {
                            if (state.get() == TrafficState.RED) continue;
                            Vehicle v = queue.get(index).take();
                            System.out.println(v + "is moving");
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            System.out.println("System failure");
                        }
                    }
                });
                th.setDaemon(true);
                th.start();
            }

        }

        public void changeState(TrafficState state) {
            this.state.set(state);
            notifyAllObservers();
        }

        private void notifyAllObservers() {
            for(TrafficObservers ob:observers){
                ob.updateStateChange(new CongestionMetrics(this));
            }
        }

        public void incoming(Vehicle vehicle, int laneNo) {
            if (laneNo < 0 || laneNo >= noOfLanes)
                throw new IndexOutOfBoundsException("lane should be positive, and within bound");
            queue.get(laneNo).offer(vehicle);
        }

        public void observe(TrafficObservers ob){
            observers.add(ob);
        }

        public void remove(TrafficObservers ob){
            observers.remove(ob);
        }

    }

    interface SignalDispatcher{
        List<Lane> nextDispatch();
    }

    static class TimeBasedDispatcher implements SignalDispatcher{
        DelayQueue<Intersection> delayQueue;
        ConcurrentHashMap<Direction, Lane> laneMap;
        Set<Direction> startDirection = Set.of(Direction.NS, Direction.NE, Direction.NW);

        public TimeBasedDispatcher(List<Direction> directions, List<Lane> lanes) {
            delayQueue = new DelayQueue<>();
            for(Direction d:directions){
                if (startDirection.contains(d)) {
                    Intersection e = new Intersection(d, TrafficState.GREEN);
                    delayQueue.offer(e);
                }
            }

            laneMap = new ConcurrentHashMap<>();
            lanes.forEach(l -> laneMap.compute(l.laneType, (k, v) -> v == null ? l : v));
            Thread workerTh = new Thread(this::dispatch);
            workerTh.setDaemon(true);
            workerTh.start();
        }

        private void dispatch() {
            while(true){
                Intersection i = delayQueue.poll();
                if (i == null) LockSupport.parkNanos(1000);
                laneMap.get(i.direction).changeState(i.state);
                Intersection newI;
                if (i.state == TrafficState.GREEN){
                    newI = new Intersection(i.direction.next(), TrafficState.YELLOW);
                    i.updateLastChangedTime(TrafficState.GREEN.getDuration());
                }
                else if (i.state == TrafficState.YELLOW){
                    newI = new Intersection(i.direction.next(), TrafficState.YELLOW);
                    i.updateLastChangedTime(TrafficState.YELLOW.getDuration());
                }
                else{
                    newI = new Intersection(i.direction.next(), TrafficState.YELLOW);
                    i.updateLastChangedTime(TrafficState.RED.getDuration());
                }
                delayQueue.offer(newI);
            }
        }

    }


}
