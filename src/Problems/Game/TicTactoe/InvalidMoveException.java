package Problems.Game.TicTactoe;

public class InvalidMoveException extends RuntimeException{
    public InvalidMoveException(String mssg){
        super(mssg);
    }
}
