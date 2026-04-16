package Problems.Game.MineSweeper;


import java.util.List;

public class MineSweeperGameFacade {
    private static volatile MineSweeperGameFacade instance = null;
    private static Object lock = new Object();
    private Game currentGame;
    private List<GameObserver> observers;

    private MineSweeperGameFacade(){};

    public MineSweeperGameFacade getInstance(){
        if (instance == null){
            synchronized (lock){
                if (instance == null){
                    instance = new MineSweeperGameFacade();
                }
            }
        }
        return instance;
    }
    public Game createGame(GameDifficulty difficulty, Player player, MinePlacementStrategy strategy){
        Board board = difficulty.getBoard();
        currentGame = new Game(player, strategy, board);
        return currentGame;
    }
    public void makeMove(Position position, Action action){
        GameStatus status = currentGame.makeMove(position, action);
        if (status == GameStatus.WON) {
            System.out.println(currentGame.getPlayer().getName() + " won!!");
        }
        else if (status == GameStatus.LOST) {
            System.out.println(currentGame.getPlayer().getName() + " lost!!");
        }
    }
    public void addObserver(GameObserver observer){
        observers.add(observer);
    }
    public void notifyAllObservers(){
        for(GameObserver obs:observers)
            obs.update(currentGame.player, currentGame.getStatus());
    }
}
