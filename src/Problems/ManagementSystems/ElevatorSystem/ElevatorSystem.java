package Problems.ManagementSystems.ElevatorSystem;

import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;

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

class IdleElevator extends ElevatorObservable{
    public IdleElevator(Direction direction, int floorNo) {
        super(direction, floorNo);
    }
}

class MovingUpElevator extends ElevatorObservable{
    public MovingUpElevator(Direction direction, int floorNo) {
        super(direction, floorNo);
    }
}
