import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class QuestionHandler {
    private List<Question> questions;  // To hold questions and their options
    private int currentQuestionIndex = 0;  // Track the current question


    public QuestionHandler() {
        questions = new ArrayList<>();
        currentQuestionIndex = 0;
        loadQuestions("questions.txt");
    }

    // Load questions from a file
    private void loadQuestions(String filePath) {
        questions = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 6) {
                    String questionText = parts[0];
                    String[] options = new String[] {parts[1], parts[2], parts[3], parts[4]};
                    String correctAnswer = parts[5];
                    questions.add(new Question(questionText, options, correctAnswer));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean outOfQuestions(){
        //no questions left
        if(questions.size() == 0){
            return true;
        }
        //questions left
        return false;
    }

    public List<Question> getQuestionArray(){
        return questions;
    }

    public void nextQuestion(){
        if(!outOfQuestions()){
            questions.remove(0);
        }
        else{
            System.out.println("No more questions.");
        }
    }

    public int getCurrentQuestionIndex(){
		return currentQuestionIndex;
	}

    public void setCurrentQuestionIndex(int i){
        currentQuestionIndex = i;
    }

    public void questionToString() {
        if (!outOfQuestions()) {
            Question currentQuestion = questions.get(0); // Get the question at index 0
            StringBuilder sb = new StringBuilder();
            sb.append("Question: ").append(currentQuestion.getQuestion()).append("\n");
            sb.append("Options:\n");
            String[] options = currentQuestion.getOptions();
            for (int i = 0; i < options.length; i++) {
                sb.append((char) ('A' + i)).append(". ").append(options[i]).append("\n");
            }
            System.out.println(sb.toString()); // Print the formatted question
        } else {
            System.out.println("No more questions available.");
        }
    }
}
