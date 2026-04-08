package Problems.Game.TicTactoe;

public class RowWinningStrategy implements WinningStrategy{
    @Override
    public boolean checkWin(Board board, Symbol symbol) {
        int row = board.row, col = board.col, i, j;
        for(i=0; i<row; i++){
            int cnt = 0;
            for(j=0; j<col; j++){
                if (board.grid[i][j].symbol == symbol){
                    cnt++;
                }
            }
            if (col == cnt) return true;
        }
        return false;
    }
}
