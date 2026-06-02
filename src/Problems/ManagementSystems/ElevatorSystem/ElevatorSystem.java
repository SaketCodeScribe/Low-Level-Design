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

    static class Door{
        DoorState doorState;
        boolean pressUp;
        boolean pressDown;

        {doorState = DoorState.Close;}

        public DoorState getDoorState() {
            return doorState;
        }

        public void setDoorState(DoorState doorState) {
            this.doorState = doorState;
        }

        public void setPressUp() {
            this.pressUp = true;
        }

        public void setPressDown() {
            this.pressDown = true;
        }
    }

    static class Floor{
        int floorNo;
        List<Display> displays;
        List<Door> doors;

    }

    static class Request{
        int floorNo;
        Direction direction;

        public Request(int floorNo, Direction direction) {
            this.floorNo = floorNo;
            this.direction = direction;
        }

        public int getFloorNo() {
            return floorNo;
        }

        public Direction getDirection() {
            return direction;
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
        Direction direction;
        ElevatorState state;
        List<ElevatorObserver> observers;
        TreeSet<Integer> upRequests;
        TreeSet<Integer> downRequests;
        int maxFloor;

        public Elevator(int elevatorId, int maxFloor){
            this.elevatorId = elevatorId;
            currentFloor = new AtomicInteger(0);
            door = new Door();
            direction = Direction.None;
            state = ElevatorState.Idle;
            observers = new ArrayList<>();
            upRequests = new TreeSet<>();
            downRequests = new TreeSet<>();
            this.maxFloor = maxFloor;

        }
        public int getFloor(){
            return currentFloor.get();
        }

        private void openDoor(){
            door.setDoorState(DoorState.Open);
            state = ElevatorState.DoorOpen;
        }
        private void closeDoor(){
            door.setDoorState(DoorState.Close);
            state = ElevatorState.Idle;
        }
        public void move(int floorNo){
            state = ElevatorState.Moving;
            while(currentFloor.get() < floorNo){
                notifyStateChange(this.elevatorId, this.currentFloor.getAndIncrement(), direction);
                LockSupport.parkNanos(100000);
            }
            notifyStateChange(this.elevatorId, this.currentFloor.getAndIncrement(), direction);
            LockSupport.parkNanos(100000);
            openDoor();
            LockSupport.parkNanos(100000);
            closeDoor();
            state = ElevatorState.Idle;
        }

        public void add(int floorNo){
            if (floorNo < 0) throw new IllegalMoveException("floor can't be negative");
            if (floorNo > maxFloor) throw new IllegalMoveException("floor can't be more than"+ maxFloor);

            if (currentFloor.get() < floorNo) upRequests.add(floorNo);
            else if (currentFloor.get() > floorNo) downRequests.add(floorNo);
       }

       public void run(){
            if (upRequests.isEmpty() && downRequests.isEmpty()){
                state = ElevatorState.Idle;
                direction = Direction.None;
            }
            if (direction == Direction.Up){
                Integer next = upRequests.ceiling(currentFloor.get());
                if (next == null){
                    direction = Direction.Down;
                }
                else move(next);
            }
            else if (direction == Direction.Down){
                Integer next = downRequests.floor(currentFloor.get());
                if (next == null){
                    direction = Direction.Up;
                }
                else move(next);
            }
            else{
                direction = Direction.Up;
            }
       }

        private void notifyStateChange(int elevatorId, int currentFloor, Direction direction) {
            for(ElevatorObserver observer:observers){
                observer.updateStateChange(elevatorId, currentFloor, direction);
            }
        }

        public void addObserver(ElevatorObserver observer){
            observers = new ArrayList<>();
        }
    }
    static class ElevatorController{
        Elevator elevator;
        ExecutorService worker;

        public ElevatorController(Elevator elevator) {
            this.elevator = elevator;
            worker = Executors.newFixedThreadPool(1, r -> {
                Thread t = new Thread("worker thread - "+ elevator.elevatorId);
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
                return distance < Integer.MAX_VALUE ? distance : 0;
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

            if (idle.getScore() > Math.max(sameDirection.getScore(), oppositeDirection.getScore())){
                return idle.controller;
            }
            if (sameDirection.getScore() > Math.max(idle.getScore(), oppositeDirection.getScore())){
                return sameDirection.controller;
            }
            return oppositeDirection.controller;
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
    public static ElevatorSystem getInstance(){
        if (instance == null){
            synchronized (lock){
                if (instance == null){
                    instance = new ElevatorSystem();
                }
            }
        }
        return instance;
    }

    public static void setInstance(ElevatorSystem instance) {
        ElevatorSystem.instance = instance;
    }

    public void setControllers(List<ElevatorController> controllers) {
        this.controllers = controllers;
    }

    public void setDisplays(List<Display> displays) {
        this.displays = displays;
    }

    public void setElevatorDispatchStrategy(ElevatorDispatchStrategy elevatorDispatchStrategy) {
        this.elevatorDispatchStrategy = elevatorDispatchStrategy;
    }

    public void setBuildingHeight(int buildingHeight) {
        this.buildingHeight = buildingHeight;
    }
    public Request createRequest(int floorNo, Direction direction){
        return new Request(floorNo, direction);
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