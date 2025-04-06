import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;

public class ClientWindow implements ActionListener {
    private JButton poll;
    private JButton submit;
    private JRadioButton options[];
    private ButtonGroup optionGroup;
    private JLabel question;
    private JLabel timer;
    private JLabel score;
    private TimerTask clock;
    
    private JFrame window;
    private static SecureRandom random = new SecureRandom();
    
    private List<Question> questions;  // To hold questions and their options
    private int currentQuestionIndex = 0;  // Track the current question
    private int scoreCount = 0;  // Track the score
    
    public ClientWindow() {
        JOptionPane.showMessageDialog(window, "This is a trivia game");
        
        window = new JFrame("Trivia");
        question = new JLabel("Q1. This is a sample question");
        window.add(question);
        question.setBounds(10, 5, 350, 100);

        options = new JRadioButton[4];
        optionGroup = new ButtonGroup();
        for (int index = 0; index < options.length; index++) {
            options[index] = new JRadioButton("Option " + (index + 1));  
            options[index].addActionListener(this);
            options[index].setBounds(10, 110 + (index * 20), 350, 20);
            window.add(options[index]);
            optionGroup.add(options[index]);
        }

        timer = new JLabel("TIMER");
        timer.setBounds(250, 250, 100, 20);
        clock = new TimerCode(30);
        Timer t = new Timer();
        t.schedule(clock, 0, 1000);
        window.add(timer);

        score = new JLabel("SCORE: 0");
        score.setBounds(50, 250, 100, 20);
        window.add(score);

        poll = new JButton("Poll");
        poll.setBounds(10, 300, 100, 20);
        poll.addActionListener(this);
        window.add(poll);

        submit = new JButton("Submit");
        submit.setBounds(200, 300, 100, 20);
        submit.addActionListener(this);
        window.add(submit);

        window.setSize(400, 400);
        window.setBounds(50, 50, 400, 400);
        window.setLayout(null);
        window.setVisible(true);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);

        // Load questions and options from the file
        loadQuestions("questions.txt");
        showQuestion(currentQuestionIndex);  // Display the first question
    }

    // This method is called when you check/uncheck any radio button or press submit/poll
    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("You clicked " + e.getActionCommand());

        String input = e.getActionCommand();
        switch (input) {
            case "Option 1":
            case "Option 2":
            case "Option 3":
            case "Option 4":
                // Here you can add any specific logic for when an option is clicked
                break;
            case "Poll":
                // Handle polling logic (e.g., show a prompt or handle events)
                break;
            case "Submit":
                // Check if the selected answer is correct and update score
                handleAnswerSubmission();
                break;
            default:
                System.out.println("Incorrect Option");
        }

        // Test code below to demo enable/disable components
        if (poll.isEnabled()) {
            poll.setEnabled(false);
            submit.setEnabled(true);
        } else {
            poll.setEnabled(true);
            submit.setEnabled(false);
        }

        // Move to the next question after each submission
        if (currentQuestionIndex < questions.size() - 1) {
            currentQuestionIndex++;
            showQuestion(currentQuestionIndex);
        } else {
            JOptionPane.showMessageDialog(window, "You have finished the quiz! Final Score: " + scoreCount);
        }
    }

    // Show the question at the given index
    private void showQuestion(int index) {
        Question currentQuestion = questions.get(index);
        question.setText("Q" + (index + 1) + ". " + currentQuestion.getQuestion());
        for (int i = 0; i < options.length; i++) {
            options[i].setText(currentQuestion.getOptions()[i]);
            options[i].setEnabled(true);  // Re-enable all options for the new question
        }
        timer.setText("30");
        clock = new TimerCode(30);
        Timer t = new Timer();
        t.schedule(clock, 0, 1000);
    }

    // Handle the submission of an answer
    private void handleAnswerSubmission() {
        for (int i = 0; i < options.length; i++) {
            if (options[i].isSelected()) {
                if (questions.get(currentQuestionIndex).getOptions()[i].equals(questions.get(currentQuestionIndex).getCorrectAnswer())) {
                    scoreCount++;
                    score.setText("SCORE: " + scoreCount);
                    break;
                }
            }
        }
    }

    // Load questions from a file
    private void loadQuestions(String filePath) {
        questions = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("questions.txt"))) {
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

    // Timer class to handle the countdown
    public class TimerCode extends TimerTask {
        private int duration;

        public TimerCode(int duration) {
            this.duration = duration;
        }

        @Override
        public void run() {
            if (duration < 0) {
                timer.setText("Time's up!");
                window.repaint();
                this.cancel();
                return;
            }

            if (duration < 6)
                timer.setForeground(Color.red);
            else
                timer.setForeground(Color.black);

            timer.setText(duration + "");
            duration--;
            window.repaint();
        }
    }

    // Question class to hold question, options, and the correct answer
    public class Question {
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
}
