package Problems.Game.SnakeAndLadder;

public class Snake extends BoardEntity{
    public Snake(int s, int e){
        super(s, e);
    }
    @Override
    public void validate(){
        if (getStart() <= getEnd()) throw new InvalidInitialization("Snake start position should be > end position");
    }
}
