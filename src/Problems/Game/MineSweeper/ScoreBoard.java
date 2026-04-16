package Problems.Game.MineSweeper;

import java.util.HashMap;
import java.util.Map;

public class ScoreBoard implements GameObserver{
    Map<String, Integer> wins;
    Map<String, Integer> losses;
    public ScoreBoard(){
        wins = new HashMap<>();
        losses = new HashMap<>();
    }
    @Override
    public void update(Player player, GameStatus status) {
        if (status == GameStatus.WON)
            wins.put(player.getName(), wins.getOrDefault(player.getName(), 0) + 1);
        else losses.put(player.getName(), losses.getOrDefault(player.getName(), 0) + 1);
    }
    public void analysis(String playerName){
        if (!wins.containsKey(playerName) && !losses.containsKey(playerName)) System.out.println(playerName +" never played a game!!");
        else System.out.println(playerName + "won: "+wins.get(playerName)+", lost: "+losses.get(playerName));
    }
}
