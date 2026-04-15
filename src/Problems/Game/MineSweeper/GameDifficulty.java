package Problems.Game.MineSweeper;


import java.util.function.Supplier;

public enum GameDifficulty {
    EASY(easyGame()),
    MEDIUM(mediumGame()),
    DIFFICULTY(difficultGame());

    private Supplier<Board> supplier;

    GameDifficulty(Supplier<Board> supplier){
        this.supplier = supplier;
    }

    private static Supplier<Board> easyGame(){
        return () -> new Board(9, 9, 10);
    }


    private static Supplier<Board> mediumGame(){
        return () -> new Board(10,10,16);
    }

    private static Supplier<Board> difficultGame(){
        return () -> new Board(16,16,32);
    }

    public Board getBoard(){
        return this.supplier.get();
    }
}
