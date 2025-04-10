// Question class to hold question, options, and the correct answer
import java.io.Serializable;

public class Question implements Serializable {
    private static final long serialVersionUID = 1L; // Recommended for Serializable classes
    private String question;
    private String[] options;
    private String correctAnswer;

    public Question(String question, String[] options, String correctAnswer) {
        this.question = question;
        this.options = options;
        this.correctAnswer = correctAnswer;
    }

    public String getQuestion() {
        return question;
    }

    public String[] getOptions() {
        return options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }
}