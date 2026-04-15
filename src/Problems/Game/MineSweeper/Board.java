package Problems.Game.MineSweeper;

public class Board {
    Cell[][] grid;
    int row, col;
    int mines;

    public Board(int row, int col, int mines) {
        this.row = row;
        this.col = col;
        this.mines = mines;
    }
}
