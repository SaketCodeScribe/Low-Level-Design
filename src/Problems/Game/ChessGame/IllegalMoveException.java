package Problems.Game.ChessGame;

public class IllegalMoveException extends RuntimeException{
    public IllegalMoveException(String mssg){
        super(mssg);
    }
}
