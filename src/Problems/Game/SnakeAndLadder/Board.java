package Problems.Game.SnakeAndLadder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Board {
    private final int size;
    private final Map<Integer, Integer> positionMap;
    public Board(int size, List<BoardEntity> entities){
        this.size = size;
        positionMap = new HashMap<>();
        for(BoardEntity entity:entities){
            assert entity.getStart() > 0;
            assert entity.getEnd() <= size;
            positionMap.put(entity.getStart(), entity.getEnd());
        }
    }
    public int getPosition(int number){
        return positionMap.getOrDefault(number, number);
    }
}
