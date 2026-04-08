package Problems.Game.TicTactoe;

public class ColWinningStrategy implements WinningStrategy{
    @Override
    public boolean checkWin(Board board, Symbol symbol) {
        int row = board.row, col = board.col, i, j;
        for(i=0; i<col; i++){
            int cnt = 0;
            for(j=0; j<row; j++){
                if (board.grid[i][j].symbol == symbol){
                    cnt++;
                }
            }
            if (row == cnt) return true;
        }
        return false;
    }
}
