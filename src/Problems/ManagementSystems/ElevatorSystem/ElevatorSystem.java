package Problems.ManagementSystems.ElevatorSystem;

import Problems.Game.ChessGame.IllegalMoveException;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public class ElevatorSystem{
    enum DoorState{
        Open,
        Close;
    }

    enum Direction{
        Up,
        Down,
        None;
    }

    enum ElevatorState{
        Idle,
        Moving,
        DoorOpen,
        OutOfService;
    }

    static class Door {
        volatile DoorState doorState;

        {
            doorState = DoorState.Close;
        }

        public DoorState getDoorState() {
            return doorState;
        }

        public void setDoorState(DoorState doorState) {
            this.doorState = doorState;
        }
    }

    static class Floor{
        int floorNo;
        List<Display> displays;
        boolean pressUp, pressDown;

        public Floor(int floorNo, List<Display> displays) {
            this.floorNo = floorNo;
            this.displays = displays;
        }

        public void setPressUp(boolean pressUp) {
            this.pressUp = pressUp;
        }

        public void setPressDown(boolean pressDown) {
            this.pressDown = pressDown;
        }
    }
    enum RequestType{
        Internal,
        External;
    }
    static class Request{
        int floorNo;
        Direction direction;
        RequestType requestType;

        public Request(int floorNo, Direction direction, RequestType requestType) {
            this.floorNo = floorNo;
            this.direction = direction;
            this.requestType = requestType;
        }

        public int getFloorNo() {
            return floorNo;
        }

        public Direction getDirection() {
            return direction;
        }

        public RequestType getRequestType() {
            return requestType;
        }
    }

    interface ElevatorObserver{
        void updateStateChange(int elevatorId, int floor, Direction direction);
    }

    static class Display implements ElevatorObserver{
        int elevatorId;
        int floor;
        Direction direction = Direction.None;

        public Display(int elevatorId) {
            this.elevatorId = elevatorId;
        }

        @Override
        public void updateStateChange(int elevatorId, int floor, Direction direction) {
            if (this.elevatorId == elevatorId){
                this.floor = floor;
                this.direction = direction;
                System.out.println(this);
            }
        }

        @Override
        public String toString() {
            return "Display{" +
                    "floor=" + floor +
                    ", direction=" + direction +
                    '}';
        }
    }
    static class Elevator{
        AtomicInteger currentFloor;
        int elevatorId;
        Door door;
        volatile Direction direction;
        volatile ElevatorState state;
        List<ElevatorObserver> observers;
        NavigableSet<Integer> upRequests;
        NavigableSet<Integer> downRequests;
        int maxFloor;

        public Elevator(int elevatorId, int maxFloor){
            this.elevatorId = elevatorId;
            currentFloor = new AtomicInteger(0);
            door = new Door();
            direction = Direction.None;
            state = ElevatorState.Idle;
            observers = new ArrayList<>();
            upRequests = Collections.synchronizedNavigableSet(new TreeSet<>());
            downRequests = Collections.synchronizedNavigableSet(new TreeSet<>());
            this.maxFloor = maxFloor;

        }
        public int getFloor(){
            return currentFloor.get();
        }

        private void openDoor(){
            door.setDoorState(DoorState.Open);
        }
        private void closeDoor(){
            door.setDoorState(DoorState.Close);
        }
        public void move(int floorNo){
            state = ElevatorState.Moving;
            if (currentFloor.get() < floorNo){
                direction = Direction.Up;
                while(currentFloor.get() < floorNo){
                    currentFloor.incrementAndGet();
                    notifyStateChange(
                            this.elevatorId,
                            currentFloor.get(),
                            direction
                    );
                    LockSupport.parkNanos(100000);
                }
            }
            else if(currentFloor.get() > floorNo){
                direction = Direction.Down;
                while(currentFloor.get() > floorNo){
                    currentFloor.decrementAndGet();
                    notifyStateChange(
                            this.elevatorId,
                            currentFloor.get(),
                            direction
                    );
                    LockSupport.parkNanos(100000);
                }
            }
            openDoor();
            LockSupport.parkNanos(100000);
            closeDoor();

        }


        public void add(int floorNo){
            if (floorNo < 0) throw new IllegalMoveException("floor can't be negative");
            if (floorNo > maxFloor) throw new IllegalMoveException("floor can't be more than"+ maxFloor);

            if (currentFloor.get() < floorNo){
                upRequests.add(floorNo);
            }
            else if (currentFloor.get() > floorNo){
                downRequests.add(floorNo);
            }
            else{
                upRequests.add(floorNo);
            }
       }

        public void run(){
            if (upRequests.isEmpty() && downRequests.isEmpty()){
                state = ElevatorState.Idle;
                direction = Direction.None;
                return;
            }

            if (direction == Direction.None){
                direction = !upRequests.isEmpty()
                        ? Direction.Up
                        : Direction.Down;
            }

            if (direction == Direction.Up){
                Integer next = upRequests.ceiling(currentFloor.get());
                if (next == null){
                    direction = Direction.Down;
                    next = downRequests.floor(currentFloor.get());
                    if (next != null){
                        move(next);
                        completeRequest(next);
                    }
                    return;
                }
                move(next);
                completeRequest(next);
            }
            else{
                Integer next = downRequests.floor(currentFloor.get());
                if (next == null){
                    direction = Direction.Up;
                    next = upRequests.ceiling(currentFloor.get());
                    if (next != null){
                        move(next);
                        completeRequest(next);
                    }
                    return;
                }
                move(next);
                completeRequest(next);
            }
        }

        private void completeRequest(Integer floor) {
            upRequests.remove(floor);
            downRequests.remove(floor);
        }

        private void notifyStateChange(int elevatorId, int currentFloor, Direction direction) {
            for(ElevatorObserver observer:observers){
                observer.updateStateChange(elevatorId, currentFloor, direction);
            }
        }

        public void addObserver(ElevatorObserver observer){
            observers.add(observer);
        }
    }
    static class ElevatorController{
        Elevator elevator;
        ExecutorService worker;

        public ElevatorController(Elevator elevator) {
            this.elevator = elevator;
            worker = Executors.newFixedThreadPool(1, r -> {
                Thread t = new Thread(r, "worker thread - "+ elevator.elevatorId);
                t.setDaemon(true);
                return t;
            });
            worker.execute(this::execute);
        }

        public void offer(int floorNo){
            elevator.add(floorNo);
        }

        public void execute(){
            while (!Thread.currentThread().isInterrupted()){
                elevator.run();
                LockSupport.parkNanos(1000);
            }
        }

        public void shutdown(){
            worker.shutdown();
            try{
                worker.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }
    interface ElevatorDispatchStrategy {
        ElevatorController getElevator(List<ElevatorController> controllers, Request request);
    }

    static class NearestElevatorStrategy implements ElevatorDispatchStrategy {
        static class Result{
            ElevatorController controller;
            int distance = Integer.MAX_VALUE;

            public int getScore(){
                return distance;
            }

        }
        private final int bonus = 10;

        @Override
        public ElevatorController getElevator(List<ElevatorController> controllers, Request request) {
            Direction direction = request.getDirection();
            int floor = request.getFloorNo();

            Result idle = new Result();
            Result sameDirection = new Result();
            Result oppositeDirection = new Result();

            getIdleElevatorController(controllers, floor, idle);
            getSameDirectionElevatorController(controllers, direction, floor, sameDirection);
            getOppositeDirectionElevatorController(controllers, direction, floor, oppositeDirection);

            if (idle.controller != null &&
                    idle.getScore() < Math.min(
                            sameDirection.getScore() - bonus,
                            oppositeDirection.getScore())){
                return idle.controller;
            }

            if (sameDirection.controller != null &&
                    sameDirection.getScore() - bonus < Math.min(
                            idle.getScore(),
                            oppositeDirection.getScore())){
                return sameDirection.controller;
            }

            if (oppositeDirection.controller != null){
                return oppositeDirection.controller;
            }

            throw new IllegalStateException(
                    "No elevator available"
            );
        }

        private void getOppositeDirectionElevatorController(List<ElevatorController> controllers, Direction direction, int floor, Result result) {
            for(ElevatorController controller: controllers){
                if (controller.elevator.state != ElevatorState.OutOfService && !isSameDirection(direction, controller, floor)){
                    if (result.distance > Math.abs(controller.elevator.getFloor() - floor)){
                        result.controller = controller;
                        result.distance = Math.abs(controller.elevator.getFloor() - floor);
                    }
                }
            }
        }

        private void getIdleElevatorController(List<ElevatorController> controllers, int floor, Result result) {
            for(ElevatorController controller: controllers){
                if (controller.elevator.state == ElevatorState.Idle){
                    if (result.distance > Math.abs(controller.elevator.getFloor() - floor)){
                        result.controller = controller;
                        result.distance = Math.abs(controller.elevator.getFloor() - floor);
                    }
                }
            }
        }
        private void getSameDirectionElevatorController(List<ElevatorController> controllers, Direction direction, int floor, Result result) {
            for(ElevatorController controller: controllers){
                if (controller.elevator.state == ElevatorState.Moving && isSameDirection(direction, controller, floor)){
                    if (result.distance > Math.abs(controller.elevator.getFloor() - floor)){
                        result.controller = controller;
                        result.distance = Math.abs(controller.elevator.getFloor() - floor);
                    }
                }
            }
        }

        private static boolean isSameDirection(Direction direction, ElevatorController controller, int floor) {
            return direction == controller.elevator.direction && ((direction == Direction.Up && controller.elevator.getFloor() < floor) || (direction == Direction.Down && controller.elevator.getFloor() > floor));
        }
    }
    private static volatile ElevatorSystem instance;

    List<ElevatorController> controllers;
    List<Display> displays;
    ElevatorDispatchStrategy elevatorDispatchStrategy;
    int buildingHeight;
    private static Object lock = new Object();
    public static ElevatorSystem getInstance(List<ElevatorController> controllers, List<Display> displays, ElevatorDispatchStrategy elevatorDispatchStrategy, int buildingHeight){
        if (instance == null){
            synchronized (lock){
                if (instance == null){
                    instance = new ElevatorSystem(controllers, displays, elevatorDispatchStrategy, buildingHeight);
                }
            }
        }
        return instance;
    }

    private ElevatorSystem(List<ElevatorController> controllers, List<Display> displays, ElevatorDispatchStrategy elevatorDispatchStrategy, int buildingHeight) {
        this.controllers = controllers;
        this.displays = displays;
        this.elevatorDispatchStrategy = elevatorDispatchStrategy;
        this.buildingHeight = buildingHeight;
    }

    public Request createRequest(int floorNo, Direction direction, RequestType requestType){
        return new Request(floorNo, direction, requestType);
    }

    public void add(Request request){
        ElevatorController controller = elevatorDispatchStrategy.getElevator(controllers, request);
        controller.offer(request.floorNo);
    }

    public void outOfService(ElevatorController controller){
        controller.shutdown();
        controller.elevator.direction = Direction.None;
        controller.elevator.state = ElevatorState.OutOfService;
    }
}