package Problems.Game.MineSweeper;

import java.util.*;

public class RandomMinePlacementStrategy implements MinePlacementStrategy{
    private Random random;
    public RandomMinePlacementStrategy(){
        random = new Random();
    }
    @Override
    public List<Position> placeMines(Board board, Position safePosition, int mines) {
        Set<Position> set = new HashSet<>();
        set.add(safePosition);
        List<Position> mine = new ArrayList<>();
        int row = board.row, col = board.col;
        while(set.size()<mines){
            int rrow = random.nextInt(0, row+1);
            int rcol = random.nextInt(0, col+1);
            Position pos = new Position(rrow, rcol);
            if (!set.contains(pos)) {
                mine.add(pos);
                board.setMine(pos);
                set.add(pos);
            }
        }
        return mine;
    }
}
