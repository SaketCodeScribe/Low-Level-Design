package Problems.Game.ChessGame;

public class Knight extends Entity{
    private int[][] moves = new int[][]{{2,1},{2,-1},{-2,1},{-2,-1}};
    public Knight(Color color){
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
