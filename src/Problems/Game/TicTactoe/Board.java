package Problems.Game.TicTactoe;

public class Board {
    Cell[][] grid;
    int row;
    int col;
    int size; // = row*col
    public void placeSymbol(int x, int y, Symbol symbol){

    }
    public boolean isCellEmpty(int x, int y){
        return grid[x][y].isEmpty;
    }
    public boolean isBoardFull(){
        return size == 0;
    }

}
