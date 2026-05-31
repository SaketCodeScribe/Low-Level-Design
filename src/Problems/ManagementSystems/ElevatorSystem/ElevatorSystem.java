package Problems.ManagementSystems.ElevatorSystem;

import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ElevatorSystem {
}

enum Direction{
    UP,
    Down,
    Idle;
}

enum DoorState{
    Open,
    Closed;
}
class Door{
    DoorState doorState;
}
enum RequestType{
    Internal,
    External;
}

class Request{
    RequestType requestType;
    Optional<Direction> direction;
    int floorNo;

}

enum ElevatorState{
    MovingUp,
    MovingDown,
    Idle,
    DoorOpen,
    OutOfService;
}

abstract class ElevatorObservable{
    Direction direction;
    int floorNo;
    ElevatorObservable(Direction direction, int floorNo){
        this.direction = direction;
        this.floorNo = floorNo;
    }

    Direction getDirection(){
        return direction;
    }

    int getFloor(){
        return floorNo;
    }

    void notifyAll(List<ElevatorObserver> observers){
        for(ElevatorObserver observer:observers){
            observer.update(this);
        }
    }

}

interface ElevatorObserver{
    void update(ElevatorObservable observable);
}

class Display implements ElevatorObserver{
    Direction direction;
    int floorNo;

    @Override
    public void update(ElevatorObservable observable) {
        direction = observable.getDirection();
        floorNo = observable.getFloor();
    }

    @Override
    public String toString(){
        return String.format("floor no: %d, direction: %s", floorNo, direction.name());
    }
}

class Floor{
    int floorNo;
    Display display;
}

class Elevator extends ElevatorObservable{

    ElevatorState elevatorState;
    BlockingQueue<Request> buffer;

    Thread workerThread;
    public Elevator() {
        super(Direction.Idle, 0);
        buffer = new LinkedBlockingQueue<>();
        workerThread = new Thread(this::move);
        workerThread.setDaemon(true);

    }

    private void setDirection(Direction d){
        this.direction = d;
    }

    private void setFloorNo(int f){
        this.floorNo = f;
    }

    public void enqueueRequest(Request request){
        buffer.offer(request);
    }

    private void move(){
        while(!Thread.currentThread().isInterrupted()){
            try {
                Request request = buffer.poll(1000, TimeUnit.MILLISECONDS);
                int floorNo = request.floorNo;

            } catch (InterruptedException e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

}

