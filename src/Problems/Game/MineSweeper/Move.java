package Problems.Game.MineSweeper;

import java.util.ArrayList;
import java.util.List;

public class Move {
    private List<Position> positions;
    private Action action;

    public Move(Action action) {
        this.positions = new ArrayList<>();
        this.action = action;
    }

    public void add(Position position){
        positions.add(position);
    }

    public Action getAction() {
        return action;
    }

    public List<Position> getPositions() {
        return positions;
    }
}
