package Problems.Game.ChessGame;

public abstract class Entity {
    private final Color color;
    public Entity(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    public abstract void move(Cell[][] grid, Position curr, Position newPos, ChessCondition currCondition);

}
