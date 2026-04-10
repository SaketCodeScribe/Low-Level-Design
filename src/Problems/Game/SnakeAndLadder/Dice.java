package Problems.Game.SnakeAndLadder;

import java.util.Random;

public class Dice {
    private int minValue, maxValue;

    private Random rand;

    public Dice(int min, int max){
        minValue = min;
        maxValue = max;
        rand = new Random();
    }

    public int roll(){
        return rand.nextInt(minValue, maxValue + 1);
    }
}
