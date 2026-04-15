package Problems.Game.MineSweeper;

public class Cell {
    private CellState state;
    private boolean hasMine;

    public Cell() {
        this.state = CellState.EMPTY;
    }

    public void flagCell(){
        if (state != CellState.EMPTY){
            throw new InvalidMoveException("Cell state is already "+ CellState.FLAG);
        }
        state = CellState.FLAG;
    }

    public void unFlagCell(){
        if (state != CellState.FLAG){
            throw new InvalidMoveException("Cell state is already "+ CellState.EMPTY);
        }
        state = CellState.EMPTY;
    }

    public void placeMine(boolean hasMine){
        this.hasMine = hasMine;
    }

    public CellState getState() {
        return state;
    }

    public boolean isHasMine() {
        return hasMine;
    }
}
