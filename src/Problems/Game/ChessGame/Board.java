package Problems.Game.ChessGame;

import java.util.List;

public class Board {
    Cell[][] grid;
    Entity kingPos1, kingPos2;
    public ChessCondition move(Entity piece, Position curr, Position newPos, List<Condition> conditions, ChessCondition currCondition){
        piece.move(grid, curr, newPos, currCondition);

        for(Condition condition:conditions){
            ChessCondition chessCondition = condition.check(grid, piece.getColor() == kingPos1.getColor() ? kingPos2 : kingPos1);
            if (chessCondition != ChessCondition.NOOP) return chessCondition;
        }
        return ChessCondition.NOOP;
    }
}
