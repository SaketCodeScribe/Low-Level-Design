package Problems.Game.TicTactoe;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Game {
    Board board;
    Player[] players;
    List<WinningStrategy> winningStrategies;
    List<GameObserver> observers;
    GameStatus status;
    int currentPlayerIndex;
    public Game(Player player1, Player player2, int row, int col) {
        this.board = new Board(row, col);
        this.players = new Player[]{player1, player2};
        this.currentPlayerIndex = 0;
        this.status = GameStatus.IN_PROGRESS;
        this.winningStrategies = initializeStrategies();
        this.observers = new CopyOnWriteArrayList<>();
    }
    private List<WinningStrategy> initializeStrategies() {
        List<WinningStrategy> strategies = new ArrayList<>();
        strategies.add(new RowWinningStrategy());
        strategies.add(new ColWinningStrategy());
        strategies.add(new DiagonalWinningStrategy());
        return strategies;
    }
    public synchronized void makeMove(int x,  int y){
        if (this.status != GameStatus.IN_PROGRESS || !board.grid[x][y].isEmpty)
            throw new InvalidMoveException("Invalid Move");
        Player player = players[currentPlayerIndex];
        board.placeSymbol(x, y, player.symbol);
        for(WinningStrategy strategy: winningStrategies){
            if (strategy.checkWin(board, player.symbol)){
                status = player.symbol == Symbol.X ? GameStatus.WINNER_X : GameStatus.WINNER_O;
                notifyObservers();
            }
        }
        if (board.isBoardFull()){
            status = GameStatus.DRAW;
            notifyObservers();
        }
        currentPlayerIndex = (currentPlayerIndex + 1) % 2;
    }
    public void addObserver(GameObserver observer){
        observers.add(observer);
    }
    public void notifyObservers(){
        for(GameObserver observer:observers){
            observer.update(this);
        }
    }

    public Player getWinner() {
        if (this.status == GameStatus.WINNER_X){
            return players[0].symbol == Symbol.X ? players[0] : players[1];
        }
        else if (this.status == GameStatus.WINNER_O){
            return players[0].symbol == Symbol.O ? players[0] : players[1];
        }
        return null;
    }
}
