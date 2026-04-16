package Problems.Game.MineSweeper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class Board {
    Cell[][] grid;
    int row, col;
    Set<Position> minePositions;
    int mines;
    Stack<Move> moves;
    private int[][] directions = new int[][]{{0,1},{0,-1},{1,0},{-1,0},{-1,-1},{-1,1},{1,-1},{1,1}};

    public Board(int row, int col, int mines) {
        this.row = row;
        this.col = col;
        this.mines = mines;
        minePositions = new HashSet<>();
    }
    public void placeMines(MinePlacementStrategy strategy, Position safePosition){
        minePositions.addAll(strategy.placeMines(this, safePosition, mines));
    }
    public CellState getCellState(Position position){
        return grid[position.getRow()][position.getCol()].getState();
    }
    public boolean hasMine(Position position){
        return grid[position.getRow()][position.getCol()].isHasMine();
    }
    public boolean move(Position position, Action action){
        if (position.getRow() >= row || position.getCol() >= col || position.getCol() < 0 || position.getCol() < 0)
            throw new InvalidMoveException("Invalid move {}"+ position);
        if (minePositions.contains(position)) return false;

        switch (action){
            case MOVE:
                if (minePositions.contains(position)) return false;
                if (grid[position.getRow()][position.getCol()].getState() == CellState.FLAG)
                    throw new InvalidActionException(position+" already has flag. Unflag it to make a move");
                return takeAction(position, action, new Move(action));
            case FLAG:
                if (grid[position.getRow()][position.getCol()].getState() == CellState.FLAG)
                    throw new InvalidActionException(position+" already has flag");
                return takeAction(position, action, new Move(action));
            case UNFLAG:
                if (grid[position.getRow()][position.getCol()].getState() == CellState.HIDDEN)
                    throw new InvalidActionException(position+" already has flag");
                return takeAction(position, action, new Move(action));
            default:
                throw new InvalidActionException(action+" not supported");
        }
    }

    private boolean takeAction(Position position, Action action, Move move) {
        switch (action){
            case MOVE:
                move.add(position);
                if (grid[position.getRow()][position.getCol()].isHasMine()) return false;
                grid[position.getRow()][position.getCol()].setState(CellState.REVEALED);
                for (int[] dir:directions){
                    int r = position.getRow()+dir[0], c = position.getCol()+dir[1];
                    if (r >= 0 && c >= 0 && r < row && c < col){
                        if (grid[r][c].isHasMine()){
                            return true;
                        }
                    }
                }
                for (int[] dir:directions) {
                    int r = position.getRow()+dir[0], c = position.getCol()+dir[1];
                    if (r >= 0 && c >= 0 && r < row && c < col){
                        takeAction(new Position(r, c), action, move);
                    }
                }
                return true;
            case FLAG:
                move.add(position);
                grid[position.getRow()][position.getCol()].setState(CellState.FLAG);
                return true;
            case UNFLAG:
                move.add(position);
                grid[position.getRow()][position.getCol()].setState(CellState.HIDDEN);
                return true;
            default:
                throw new InvalidActionException(action+" not supported");
        }
    }
    public void undo(){
        if (moves.isEmpty()) return;
        Move move = moves.pop();
        System.out.println("Undo last move: "+move.getAction());
        for(Position pos: move.getPositions()){
            grid[pos.getRow()][pos.getCol()].setState(CellState.HIDDEN);
        }
    }
    public boolean hasWon(){
        for(int i=0; i<row; i++){
            for(int j=0; j<col; j++){
                if ((grid[i][j].getState() == CellState.HIDDEN || grid[i][j].getState() == CellState.FLAG) && !grid[i][j].isHasMine())
                    return false;
            }
        }
        return true;
    }
    public void setMine(Position position){
        grid[position.getRow()][position.getCol()].placeMine(true);
    }
}
