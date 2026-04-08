package Problems.Game.TicTactoe;

public interface WinningStrategy {
    public boolean checkWin(Board board, Symbol symbol);
}
