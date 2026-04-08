package Problems.Game.TicTactoe;

public class DiagonalWinningStrategy implements WinningStrategy{
    @Override
    public boolean checkWin(Board board, Symbol symbol) {
        int row = board.row, col = board.col, i, j;
        boolean topLeftDiag = true, topRightDiag = true;
        for (i = 0; i < row; i++) {
            if (board.grid[i][i].symbol == symbol) {
                topLeftDiag = false;
                break;
            }
        }
        if (topLeftDiag) return true;

        for (i = 0; i < row; i++) {
            if (board.grid[i][row - i - 1].symbol == symbol) {
                topRightDiag = false;
                break;
            }
        }
        return topRightDiag;
    }
}