package common;


import java.util.Map;

public class PointsPayload extends Payload {
    private Map<String, Integer> playerScores;  // Example: "Alice" -> 10, "Bob" -> 7

    // Constructor: set payload type to POINTS
    public PointsPayload() {
        super(PayloadType.POINTS);
    }

    public Map<String, Integer> getPlayerScores() {
        return playerScores;
    }

    public void setPlayerScores(Map<String, Integer> playerScores) {
        this.playerScores = playerScores;
    }

    // Debug-friendly string output
    @Override
    public String toString() {
        return "[PointsPayload] Player Scores: " + playerScores;
    }
}
