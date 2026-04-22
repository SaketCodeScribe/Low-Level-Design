package Problems.Game.ChessGame;

public interface Condition {
    public ChessCondition check(Cell[][] grid, Entity king);
}
