package Problems.Game.TicTactoe;

import java.util.concurrent.locks.Lock;

public class TicTacToeSystem {
    private static volatile TicTacToeSystem instance;
    private final Scoreboard scoreboard;
    private Game currentGame;
    private static final Object lock = new Object();

    private TicTacToeSystem(){
        scoreboard = new Scoreboard();
    }

    public static TicTacToeSystem getInstance(){
        if (instance == null){
            synchronized (lock){
                if (instance == null){
                    instance = new TicTacToeSystem();
                }
            }
        }
        return instance;
    }
    public Game createGame(Player player1, Player player2) {
        currentGame = new Game(player1, player2, 3, 3);
        currentGame.addObserver(scoreboard);
        System.out.println("New game started: " + player1.name +
                " vs " + player2.name);
        return currentGame;
    }

    public void makeMove(Player player, int row, int col) {
        if (currentGame == null) {
            throw new IllegalStateException("No active game. Call createGame first.");
        }
        System.out.println(player.name + " plays at (" + row + ", " + col + ")");
        currentGame.makeMove(row, col);
    }
    public GameStatus getGameStatus() {
        if (currentGame == null) {
            throw new IllegalStateException("No active game.");
        }
        return currentGame.status;
    }
    public static void resetInstance(){
        synchronized (lock){
            instance = null;
        }
    }
}
