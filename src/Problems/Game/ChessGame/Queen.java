package Problems.Game.ChessGame;

public class Queen extends Entity{
    public Queen(Color color){
        super(color);
    }
    @Override
    public void move(Cell[][] grid, Position curr, Position newPos, ChessCondition currCondition) {
        validateMove(grid, curr, newPos);
        grid[curr.row][curr.col].setEntity(null);
        grid[newPos.row][newPos.col].setEntity(this);
    }

    private void validateMove(Cell[][] grid, Position curr, Position newPos) {

    }
}
