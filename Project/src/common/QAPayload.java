package common;


import java.util.Map;

public class QAPayload extends Payload {
    private String category;                    // e.g., "Science"
    private String question;                    // e.g., "What is the boiling point of water?"
    private Map<String, String> options;        // e.g., A -> "100°C", B -> "0°C"

    // Constructor: Set payload type
    public QAPayload() {
        super(PayloadType.QUESTION);
    }

    // Getters and Setters
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public void setOptions(Map<String, String> options) {
        this.options = options;
    }

    // Debug-friendly string representation
    @Override
    public String toString() {
        return String.format(
            "[QAPayload] Category: %s | Question: %s | Options: %s",
            category, question, options
        );
    }
}
