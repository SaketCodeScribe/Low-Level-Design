package Problems.Game.TicTactoe;

import java.util.List;

public class Game {
    Board board;
    Player p1, p2;
    List<WinningStrategy> winningStrategoes;
    List<GameObserver> observers;
    GameStatus status;
    Symbol playerTurn;
    public void makeMove(Player p, int x,  int y){
        if (p.symbol != playerTurn || !board.grid[x][y].isEmpty)
            throw new InvalidMoveExceptioin();
        board.placeSymbol(x, y, p.symbol);
        for(WinningStrategy strategy:winningStrategoes){
            if (strategy.checkWin(board, p1, p2)){
                status = p.symbol == Symbol.X ? GameStatus.WINNER_X : GameStatus.WINNER_O;
                notifyObservers();
            }
        }
        if (board.isBoardFull()){
            status = GameStatus.DRAW;
            notifyObservers();
        }
    }
    public void addObserver(GameObserver observer){
        observers.add(observer);
    }
    public void notifyObservers(){
        for(GameObserver observer:observers){
            observer.update(this);
        }
    }
}
