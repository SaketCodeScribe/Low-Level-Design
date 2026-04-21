package Problems.Game.ChessGame;

public class Player {
    private String playerName;
    private Color color;

    public Player(String playerName, Color color) {
        this.playerName = playerName;
        this.color = color;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Color getColor() {
        return color;
    }
}
