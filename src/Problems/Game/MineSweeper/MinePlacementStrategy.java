package Problems.Game.MineSweeper;

import java.util.List;

public interface MinePlacementStrategy {
    public List<Position> placeMines(Board board, Position safePosition, int mines);

}
