package Problems.Game.MineSweeper;

public class InvalidMoveException extends RuntimeException{
    public InvalidMoveException(String mssg){
        super(mssg);
    }
}
