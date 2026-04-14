package Problems.Game.SnakeAndLadder;

public class InvalidMoveException extends RuntimeException{
    public InvalidMoveException(String mssg){
        super(mssg);
    }
}
