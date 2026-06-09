package Problems.ManagementSystems.TrafficControl;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

public class TrafficControlSystem {
    enum Direction{
        North,
        South,
        East,
        West;
    }
    enum TrafficLight{
        Green,
        Red,
        Yellow;
    }
    static class TrafficSignal{
        AtomicReference<TrafficLight> light;
        long redWaitTime;
        long greenWaitTime;
        long yellowWaitTime;

        public TrafficSignal(long redWaitTime, long greenWaitTime, long yellowWaitTime) {
            this.redWaitTime = redWaitTime;
            this.greenWaitTime = greenWaitTime;
            this.yellowWaitTime = yellowWaitTime;
        }
        public void setLight(TrafficLight l){
            light.set(l);
        }
        public TrafficLight getLight(){
            return light.get();
        }
    }
    static class Directions{
        private Direction direction;
        private TrafficSignal trafficSignal;

        public Directions(Direction direction, TrafficSignal signal) {
            this.direction = direction;
            this.trafficSignal = signal;
        }

        public void setDirection(Direction direction) {
            this.direction = direction;
        }

        public void setLight(TrafficLight light) {
            this.trafficSignal.setLight(light);
        }
    }

    enum JunctionState{
        Moving,
        Stopped;
    }
    interface TrafficObserver{
        void updateChangeState(Junction junction);
    }
    static abstract class Junction{
        String id;
        Set<TrafficObserver> observers;

        public Junction(String id) {
            this.id = id;
            observers = new HashSet<>();
        }

        public void add(TrafficObserver ob){
            observers.add(ob);
        }
        public void remove(TrafficObserver ob){
            observers.remove(ob);
        }
        public void updateStateChange(){
            for(TrafficObserver ob:observers){
                ob.updateChangeState(this);
            }
        }
        public String getJunctionId(){
            return id;
        }

        abstract void nextState();
        abstract JunctionState currentState();

    }
    static class NorthSouthJunction extends Junction{
        private AtomicReference<JunctionState> state;

        public NorthSouthJunction(JunctionState state, String id) {
            super(id);
            this.state = new AtomicReference<>(state);
        }

        public void nextState(){
            state.set(state.get() == JunctionState.Moving ? JunctionState.Stopped : JunctionState.Moving);
        }
        public JunctionState currentState(){
            return state.get();
        }
    }

    static class EastWestJunction extends Junction{
        private AtomicReference<JunctionState> state;

        public EastWestJunction(JunctionState state, String id) {
            super(id);
            this.state = new AtomicReference<>(state);
        }

        public void nextState(){
            state.set(state.get() == JunctionState.Moving ? JunctionState.Stopped : JunctionState.Moving);
        }
        public JunctionState currentState(){
            return state.get();
        }
    }
    static class SouthNorthJunction extends Junction{
        private AtomicReference<JunctionState> state;

        public SouthNorthJunction(JunctionState state, String id) {
            super(id);
            this.state = new AtomicReference<>(state);
        }

        public void nextState(){
            state.set(state.get() == JunctionState.Moving ? JunctionState.Stopped : JunctionState.Moving);
        }
        public JunctionState currentState(){
            return state.get();
        }
    }
    static class WestEastJunction extends Junction{
        private AtomicReference<JunctionState> state;

        public WestEastJunction(JunctionState state, String id) {
            super(id);
            this.state = new AtomicReference<>(state);
        }

        public void nextState(){
            state.set(state.get() == JunctionState.Moving ? JunctionState.Stopped : JunctionState.Moving);
        }
        public JunctionState currentState(){
            return state.get();
        }
    }
    static class TrafficMonitoring implements TrafficObserver{
        @Override
        public void updateChangeState(Junction junction) {
            System.out.println(junction.getJunctionId()+" - "+junction.currentState());
        }
    }
    static class TrafficController {
        Map<Direction, Junction> junctionMap;
        List<Directions> directions;

        public TrafficController(Map<Direction, Junction> junctionMap) {
            this.junctionMap = junctionMap;
        }

        public void updateState(Directions directions, TrafficLight light) {
            directions.trafficSignal.setLight(light);
            Junction junction = junctionMap.get(directions.direction);
            if (directions.trafficSignal.getLight() == TrafficLight.Green || directions.trafficSignal.getLight() == TrafficLight.Red) {
                junction.nextState();
            }
        }
    }
    interface SignalDispatcher{
        void run();
        TrafficController getController();
        void close();
    }
    static class TimeBasedSignalDispatcher implements SignalDispatcher{
        TrafficController controller;
        AtomicBoolean isRunning;
        public TimeBasedSignalDispatcher(TrafficController controller) {
            this.controller = controller;
        }

        public void run(){
            List<Directions> green, red, yellow;
            green = new ArrayList<>();
            red = new ArrayList<>();
            yellow = new ArrayList<>();
            isRunning = new AtomicBoolean(true);
            while(true){
                if (!isRunning.get()) {
                    for (Directions d : green) {
                        controller.updateState(d, TrafficLight.Red);
                    }
                    break;
                }
                for(Directions d: controller.directions){
                    switch (d.trafficSignal.getLight()){
                        case Red -> red.add(d);
                        case Green -> green.add(d);
                        case Yellow -> yellow.add(d);
                        default -> throw new IllegalStateException("undefined traffic light: "+d.trafficSignal.getLight());
                    }
                }
                try {
                    if (!green.isEmpty()){
                        Thread.sleep(green.get(0).trafficSignal.greenWaitTime);
                        for(Directions d:green){controller.updateState(d, TrafficLight.Yellow);}
                    }
                    else if (!yellow.isEmpty()){
                        Thread.sleep(yellow.get(0).trafficSignal.yellowWaitTime);
                        for(Directions d:yellow){controller.updateState(d, TrafficLight.Red);}
                    }
                    else {
                        Thread.sleep(red.get(0).trafficSignal.redWaitTime);
                        for(Directions d: red){controller.updateState(d, TrafficLight.Green);}
                    }

                }catch (InterruptedException e) {
                    for (Directions d:controller.directions) controller.updateState(d, TrafficLight.Red);
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public TrafficController getController() {
            return controller;
        }

        public void close(){
            isRunning.set(false);
        }
    }

    List<TimeBasedSignalDispatcher> dispatchers;
    List<Thread> threads;

    public TrafficControlSystem(List<TrafficController> controllers) {
        dispatchers = new ArrayList<>();
        controllers.forEach(controller -> dispatchers.add(new TimeBasedSignalDispatcher(controller)));
        threads = new ArrayList<>();
    }

    public void start(){
        for(SignalDispatcher dispatcher:dispatchers){
            TrafficController controller = dispatcher.getController();
            for(Directions d:controller.directions){
                if (d.direction == Direction.North || d.direction == Direction.South){
                    controller.updateState(d, TrafficLight.Green);
                }
            }
            Thread th = new Thread(dispatcher::run);
            th.setDaemon(true);
            th.start();
            threads.add(th);
        }
    }
    public void stop(){
        for(SignalDispatcher dispatcher:dispatchers){
            dispatcher.close();
        }
    }

}
