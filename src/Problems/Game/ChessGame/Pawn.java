package Problems.Game.ChessGame;

public class Pawn extends Entity{
    private int[][] moves = new int[][]{{1,0}};
    private int[][] initialMove = new int[][]{{1,0},{2,0}};
    public Pawn(Color color) {
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
