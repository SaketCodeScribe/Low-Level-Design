package Problems.Game.SnakeAndLadder;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class SnakeAndLadderGame {
    Queue<Player> players;
    Board board;
    GameStatus status;
    Dice dice;

    private SnakeAndLadderGame(Builder builder) {
        this.players = builder.players;
        this.dice = builder.dice;
        this.board = builder.board;
        this.status = GameStatus.NOT_STARTED;
    }
    public synchronized void makeMove(){
        status = status == GameStatus.NOT_STARTED ? GameStatus.IN_PROGRESS : status;
        if (status == GameStatus.FINISHED) throw new InvalidMoveException("Game is finished!");
        while(status == GameStatus.IN_PROGRESS) {
            Player player = players.peek();
            int diceRoll = dice.roll();
            if (diceRoll == 6){
                System.out.println(player.getName()+" gets one more turn!!");
            }
            else {
                players.poll();
                players.offer(player);
            }
            int newPosition = board.getPosition(diceRoll + player.getPosition());
            player.setPosition(newPosition);
            if (board.checkWin(player)) {
                status = GameStatus.FINISHED;
                System.out.println(player.getName() + "won!!");
            }
        }
    }
    public static class Builder {
        private Board board;
        private Queue<Player> players;
        private Dice dice;

        public Builder setBoard(int boardSize, List<BoardEntity> boardEntities) {
            this.board = new Board(boardSize, boardEntities);
            return this;
        }

        public Builder setPlayers(List<String> playerNames) {
            this.players = new LinkedList<>();
            for (String playerName : playerNames) {
                players.add(new Player(playerName));
            }
            return this;
        }

        public Builder setDice(Dice dice) {
            this.dice = dice;
            return this;
        }

        public SnakeAndLadderGame build() {
            if (board == null || players == null || dice == null) {
                throw new IllegalStateException("Board, Players, and Dice must be set.");
            }
            return new SnakeAndLadderGame(this);
        }
    }
}
