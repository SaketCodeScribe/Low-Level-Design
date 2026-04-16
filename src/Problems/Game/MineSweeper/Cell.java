package Problems.Game.MineSweeper;

public class Cell {
    private CellState state;
    private boolean hasMine;

    public Cell() {
        this.state = CellState.HIDDEN;
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

    public void setState(CellState state) {
        this.state = state;
    }
}
