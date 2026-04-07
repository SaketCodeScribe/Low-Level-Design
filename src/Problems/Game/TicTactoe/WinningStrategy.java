package Problems.Game.TicTactoe;

public interface WinningStrategy {
    public boolean checkWin(Board board, Player p1, Player p2);
}
