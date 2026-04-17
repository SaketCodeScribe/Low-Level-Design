package Problems.Game.MineSweeper;


public class Game {
    Player player;
    MinePlacementStrategy strategy;
    Board board;
    GameStatus status;

    public Game(Player player, MinePlacementStrategy strategy, Board board) {
        this.player = player;
        this.strategy = strategy;
        this.board = board;
        this.status = GameStatus.NOT_STARTED;
    }
    public synchronized GameStatus makeMove(Position position, Action action){
        if (status == GameStatus.NOT_STARTED){
            status = GameStatus.IN_PROGRESS;
            board.placeMines(strategy, position);
        }
        if (!board.move(position, action)) status = GameStatus.LOST;
        else status = GameStatus.WON;
        return status;
    }
    public void undo(){
        board.undo();
    }

    public GameStatus getStatus() {
        return status;
    }

    public Player getPlayer() {
        return player;
    }
}
