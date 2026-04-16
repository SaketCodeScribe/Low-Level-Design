package Problems.Game.MineSweeper;

import java.util.List;

public class CustomeMinePlacementStrategy implements MinePlacementStrategy{
    private List<Position> positions;
    public CustomeMinePlacementStrategy(List<Position> positions) {
        this.positions = positions;
    }

    @Override
    public List<Position> placeMines(Board board, Position safePosition, int mines) {
        for(Position position:positions){
            board.setMine(position);
        }
        return positions;
    }
}
