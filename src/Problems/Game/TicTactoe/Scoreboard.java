package Problems.Game.TicTactoe;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Scoreboard implements GameObserver{
    ConcurrentMap<String, Integer> scores;
    public Scoreboard(){
        scores = new ConcurrentHashMap<>();
    }

    @Override
    public void update(Game game) {
        Player winner = game.getWinner();
        if (winner != null){
            recordWin(winner);
        }
    }

    private void recordWin(Player winner) {
        scores.merge(winner.name, 1, Integer::sum);
    }
    public int getScore(String playerName){
        return scores.getOrDefault(playerName, 0);
    }
}
