package Problems.Game.ChessGame;

public class Rook extends Entity{
    public Rook(Color color){
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
