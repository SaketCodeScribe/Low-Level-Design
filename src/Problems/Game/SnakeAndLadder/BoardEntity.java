package Problems.Game.SnakeAndLadder;

public abstract class BoardEntity {
    private int start, end;

    public BoardEntity(int s, int e) {
        start = s;
        end = e;
    }

    public int getStart(){
        return start;
    }
    public int getEnd(){
        return end;
    }
    public abstract void validate();
}
