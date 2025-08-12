package common;


import java.io.Serializable;
import java.util.Map;

public class Question implements Serializable {
    private String category;
    private String question;
    private Map<String, String> options;
    private String correctAnswer;

    // Constructor
    public Question(String category, String question, Map<String, String> options, String correctAnswer) {
        this.category = category;
        this.question = question;
        this.options = options;
        this.correctAnswer = correctAnswer;
    }

    // Getters
    public String getCategory() {
        return category;
    }

    public String getQuestion() {
        return question;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    @Override
    public String toString() {
        return "Category: " + category + "\n" +
               "Question: " + question + "\n" +
               "Options: " + options.toString() + "\n" +
               "Correct Answer: " + correctAnswer;
    }
}
