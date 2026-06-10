package Problems.ManagementSystems.TrafficControl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TrafficControlSystem {
    enum Light{
        Green,
        Yellow,
        Red;
    }
    enum Direction{
        North,
        South,
        East,
        West;
    }
    static class TrafficLight{
        SignalState curr;
        SignalState next;
        Direction direction;
        Light currentColor;
        Set<TrafficObserver> observers;

        public TrafficLight(SignalState curr, SignalState next, Direction direction) {
            this.curr = curr;
            this.next = next;
            this.direction = direction;
            this.currentColor = Light.Red;
            observers = ConcurrentHashMap.newKeySet();
        }
        public static SignalState getGreen(){
            return GreenState.getInstance();
        }
        public static SignalState getYellow(){
            return YellowState.getInstance();
        }
        public static SignalState getRed(){
            return RedState.getInstance();
        }
        public void add(TrafficObserver ob){
            observers.add(ob);
        }
        public void remove(TrafficObserver ob){
            observers.remove(ob);
        }
        public void setCurrentState(SignalState state){
            this.curr = state;
            this.curr.handle(this);
            notifyStateChange();
        }
        public void setNextState(SignalState state){
            this.next = state;
        }
        public void transition(){
            setCurrentState(this.next);
        }
        public void setColor(Light light){
            this.currentColor = light;
        }
        private void notifyStateChange(){
            for(TrafficObserver ob:observers){
                ob.updateStateChange(this.direction, this.currentColor);
            }
        }

        public void forceRed() {
            setCurrentState(RedState.getInstance());
        }
    }
    static interface SignalState{
        void handle(TrafficLight context);
    }
    static class GreenState implements SignalState{
        private static volatile SignalState instance = null;
        private static Object lock = new Object();
        public static SignalState getInstance(){
            if (instance == null){
                synchronized (lock){
                    if (instance == null){
                        instance = new GreenState();
                    }
                }
            }
            return instance;
        }
        @Override
        public void handle(TrafficLight context) {
            context.setColor(Light.Green);
        }
    }
    static class RedState implements SignalState{
        private static volatile SignalState instance = null;
        private static Object lock = new Object();
        public static SignalState getInstance(){
            if (instance == null){
                synchronized (lock){
                    if (instance == null){
                        instance = new RedState();
                    }
                }
            }
            return instance;
        }
        @Override
        public void handle(TrafficLight context) {
            context.setColor(Light.Red);
        }
    }
    static class YellowState implements SignalState{
        private static volatile SignalState instance = null;
        private static Object lock = new Object();
        public static SignalState getInstance(){
            if (instance == null){
                synchronized (lock){
                    if (instance == null){
                        instance = new YellowState();
                    }
                }
            }
            return instance;
        }
        @Override
        public void handle(TrafficLight context) {
            context.setColor(Light.Yellow);
        }
    }
    interface TrafficObserver{
        void updateStateChange(Direction direction, Light currentColor);

    }
    static class TrafficMonitor implements TrafficObserver{
        @Override
        public void updateStateChange(Direction direction, Light currentColor) {
            System.out.println(direction+" "+currentColor);
        }
    }

    static class IntersectionController{
        private long greenWaitTime;
        private long yellowWaitTime;
        private Map<Direction, TrafficLight> map;
        private IntersectionState state;
        private ExecutorService ex;
        private volatile boolean isRunning;

        public IntersectionController(long greenWaitTime, long yellowWaitTime, Map<Direction, TrafficLight> map, IntersectionState state) {
            this.greenWaitTime = greenWaitTime;
            this.yellowWaitTime = yellowWaitTime;
            this.map = map;
            this.state = state;
            ex = Executors.newFixedThreadPool(1);
            isRunning = true;
        }
        public TrafficLight getTrafficLight(Direction d){return map.get(d);}
        public void start(){
            ex.submit(this::run);
        }
        public void run(){
            while (isRunning){
                this.state = this.state.handle(this);
            }
        }
        public void stop(){
            isRunning = false;
            ex.shutdownNow();
            try{
                ex.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                for(Map.Entry<Direction, TrafficLight> entry: map.entrySet()){
                    entry.getValue().forceRed();
                }
            }
        }

        public void add(TrafficObserver ob){
            for(var entry:map.entrySet()){
                entry.getValue().add(ob);
            }
        }
        public void remove(TrafficObserver ob){
            for(var entry:map.entrySet()){
                entry.getValue().remove(ob);
            }
        }

        public static Builder builder(){
            return new Builder();
        }
        static class Builder{
            private long greenWaitTime = 45;
            private long yellowWaitTime = 15;
            private Map<Direction, TrafficLight> map;
            private IntersectionState state;
            public Builder withGreenWaitTime(long delay){
                this.greenWaitTime = delay;
                return this;
            }
            public Builder withYellowWaitTime(long delay){
                this.yellowWaitTime = delay;
                return this;
            }
            public Builder withTrafficLight(List<TrafficLight> trafficLights){
                this.map = new HashMap<>();
                trafficLights.forEach(t -> this.map.putIfAbsent(t.direction, t));
                return this;
            }
            public Builder withIntersectionState(IntersectionState state){
                this.state = state;
                return this;
            }
            public IntersectionController build(){
                return new IntersectionController(greenWaitTime, yellowWaitTime, map, state);
            }
        }
    }
    static interface IntersectionState{
        IntersectionState handle(IntersectionController context);
    }
    static class NorthSourIntersectionState implements IntersectionState{
        private final IntersectionState next = EastWestIntersectionState.getInstance();
        private static volatile IntersectionState instance  = null;
        private static Object lock = new Object();
        public static IntersectionState getInstance(){
            if (instance == null){
                synchronized (lock){
                    if (instance == null){
                        instance = new NorthSourIntersectionState();
                    }
                }
            }
            return instance;
        }
        @Override
        public IntersectionState handle(IntersectionController context) {
            try {
                context.getTrafficLight(Direction.North).setCurrentState(TrafficLight.getGreen());
                context.getTrafficLight(Direction.South).setCurrentState(TrafficLight.getGreen());
                context.getTrafficLight(Direction.North).setNextState(TrafficLight.getYellow());
                context.getTrafficLight(Direction.South).setNextState(TrafficLight.getYellow());

                Thread.sleep(context.greenWaitTime);
                context.getTrafficLight(Direction.North).transition();
                context.getTrafficLight(Direction.South).transition();
                context.getTrafficLight(Direction.North).setNextState(TrafficLight.getRed());
                context.getTrafficLight(Direction.South).setNextState(TrafficLight.getRed());
                Thread.sleep(context.yellowWaitTime);

                context.getTrafficLight(Direction.North).transition();
                context.getTrafficLight(Direction.South).transition();
                return next;

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
    static class EastWestIntersectionState implements IntersectionState{
        private final IntersectionState next = NorthSourIntersectionState.getInstance();
        private static volatile IntersectionState instance  = null;
        private static Object lock = new Object();
        public static IntersectionState getInstance(){
            if (instance == null){
                synchronized (lock){
                    if (instance == null){
                        instance = new EastWestIntersectionState();
                    }
                }
            }
            return instance;
        }

        @Override
        public IntersectionState handle(IntersectionController context) {
            try {
                context.getTrafficLight(Direction.East).setCurrentState(TrafficLight.getGreen());
                context.getTrafficLight(Direction.West).setCurrentState(TrafficLight.getGreen());
                context.getTrafficLight(Direction.East).setNextState(TrafficLight.getYellow());
                context.getTrafficLight(Direction.West).setNextState(TrafficLight.getYellow());

                Thread.sleep(context.greenWaitTime);
                context.getTrafficLight(Direction.East).transition();
                context.getTrafficLight(Direction.West).transition();
                context.getTrafficLight(Direction.East).setNextState(TrafficLight.getRed());
                context.getTrafficLight(Direction.West).setNextState(TrafficLight.getRed());
                Thread.sleep(context.yellowWaitTime);

                context.getTrafficLight(Direction.East).transition();
                context.getTrafficLight(Direction.West).transition();
                return next;

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
    List<IntersectionController> controllers;

    public TrafficControlSystem(long greenWaitTime, long yellowWaitTime, List<List<TrafficLight>> trafficLights){
        controllers = new ArrayList<>();

        for(List<TrafficLight> light:trafficLights){
            controllers.add(IntersectionController.builder()
                    .withGreenWaitTime(greenWaitTime)
                    .withYellowWaitTime(yellowWaitTime)
                    .withTrafficLight(light)
                    .withIntersectionState(NorthSourIntersectionState.getInstance())
                    .build());
        }

    }

    public void start(){
        for(IntersectionController controller:controllers){
            controller.start();
        }
    }

    public void subscribe(TrafficObserver ob){
        for(IntersectionController controller:controllers){
            controller.add(ob);
        }
    }

    public void remove(IntersectionController controller, TrafficObserver ob){
        controller.remove(ob);
    }
}