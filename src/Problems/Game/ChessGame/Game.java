package Problems.Game.ChessGame;

import java.util.List;

public class Game {
    static class GameBuilder{
        private Board board;
        private Player[] players;
        private List<Condition> conditions;

        public GameBuilder(){};
        public GameBuilder withBoard(Board board){
            this.board = board;
            return this;
        }
        public GameBuilder withP1(Player[] p){
            this.players = p;
            return this;
        }
        public GameBuilder withCondition(List<Condition> conditions){
            this.conditions = conditions;
            return this;
        }
        public Game build(){
            return new Game(this);
        }
    }
    Board board;
    Player[] players;
    int currentTurn;
    List<Condition> conditions;
    GameStatus gameStatus;
    ChessCondition gameCondition;
    public Game(GameBuilder builder){
        board = builder.board;
        players = builder.players;
        conditions = builder.conditions;
        gameCondition = ChessCondition.NOOP;
    }

    public void makeMove(Player p, Entity piece, Position currPos, Position newPos){
        if (gameStatus == GameStatus.NOT_STARTED){
            gameStatus = GameStatus.IN_PROGRESS;
        }
        if (gameStatus == GameStatus.FINISHED) throw new RuntimeException("Game is completed");
        if (p.getColor() != players[currentTurn].getColor()) throw new IllegalMoveException(String.format("Player %s turn", players[currentTurn]));

        gameCondition = board.move(piece, currPos, newPos, conditions, gameCondition);
        if (gameCondition == ChessCondition.STALEMATE || gameCondition == ChessCondition.CHECKMATE){
            System.out.println(gameCondition);
            gameStatus = GameStatus.FINISHED;
        }
        currentTurn = (currentTurn+1)%2;
    }
    public void resign(Player p){
        if (gameStatus == GameStatus.NOT_STARTED) throw new RuntimeException("Game is not started");
        System.out.println(String.format("Player %s yields", p.getPlayerName()));
        gameStatus = GameStatus.FINISHED;
    }
}
