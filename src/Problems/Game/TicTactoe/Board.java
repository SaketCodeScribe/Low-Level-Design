package Problems.Game.TicTactoe;

import java.util.Arrays;

public class Board {
    Cell[][] grid;
    int row;
    int col;
    int size; // = row*col
    public Board(int row, int col){
        this.row = row;
        this.col = col;
        size = row*col;
        grid = new Cell[row][col];
        for(int i=0; i<row; i++){
            Arrays.fill(grid[i], Symbol.EMPTY);
        }
    }
    public void placeSymbol(int x, int y, Symbol symbol){
        if (!isCellEmpty(x, y)){
            throw new InvalidMoveException("cell is not empty");
        }
        grid[x][y].symbol = symbol;
    }
    public boolean isCellEmpty(int x, int y){
        return grid[x][y].isEmpty;
    }
    public boolean isBoardFull(){
        return size == 0;
    }

}
