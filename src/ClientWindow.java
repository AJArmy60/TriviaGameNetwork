import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;

public class ClientWindow implements ActionListener {
	private Client client;
    private JButton poll;
    private JButton submit;
    private JRadioButton options[];
    private ButtonGroup optionGroup;
    private JLabel question;
    private JLabel timer;
    private JLabel score;
    private TimerTask clock;
    private JFrame window;
    
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
            options[index].setEnabled(false);  // Disable options initially
        }

        timer = new JLabel("TIMER");
        timer.setBounds(250, 250, 100, 20);
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
            case "Poll":
                poll.setEnabled(false);  // Disable Poll button
                submit.setEnabled(true);  // Enable Submit button
                enableOptions(true);  // Enable options after Poll is clicked
				client.sendUDP(); //send UDP packet to server
                break;
            case "Submit":
                handleAnswerSubmission();  // Check if the selected answer is correct and update score
                poll.setEnabled(false);  // Disable Poll button after Submit
                submit.setEnabled(false);  // Disable Submit button after submission
                enableOptions(false);  // Disable options after Submit
                break;
            default:
                break;
        }
    }

    // Show the question at the given index
    private void showQuestion(int index) {
        Question currentQuestion = questions.get(index);
        question.setText("Q" + (index + 1) + ". " + currentQuestion.getQuestion());
        for (int i = 0; i < options.length; i++) {
            options[i].setText(currentQuestion.getOptions()[i]);
            options[i].setEnabled(false);  // Disable options at the start of the question
        }

        // Reset the Poll and Submit buttons
        poll.setEnabled(true);
        submit.setEnabled(false);

        // Set the timer to 10 seconds for this question
        timer.setText("10");
        clock = new TimerCode(10, this);  // Pass `this` (ClientWindow) as the reference
        Timer t = new Timer();
        t.schedule(clock, 0, 1000);  // Schedule the timer to run every second
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

    // Enable or disable the options based on the argument
    private void enableOptions(boolean enable) {
        for (int i = 0; i < options.length; i++) {
            options[i].setEnabled(enable);
        }
    }

	public void onAckReceived(Boolean ack){
		if(ack){
			submit.setEnabled(true);
		}
		else{
			submit.setEnabled(false);
		}
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

	public void setClient(Client client) {
		this.client = client;
	}

    // Move to the next question after the current one
	private void moveToNextQuestion() {
		// Disable all options and reset the timer
		for (JRadioButton option : options) {
			option.setEnabled(false);
		}

		// Move to the next question, or finish the game if no more questions
		if (currentQuestionIndex < questions.size() - 1) {
			currentQuestionIndex++;  // Increment question index properly
			showQuestion(currentQuestionIndex);  // Show the next question
		} else {
			JOptionPane.showMessageDialog(window, "Game Over! Final Score: " + scoreCount);
			window.dispose();  // Close the game window after finishing the quiz
			System.exit(0);  // Exit the program after game over
		}
	}


    // Timer class to handle the countdown
    public class TimerCode extends TimerTask {
        private int duration;
        private ClientWindow clientWindow;  // Reference to ClientWindow
    
        // Constructor to initialize duration and the ClientWindow reference
        public TimerCode(int duration, ClientWindow clientWindow) {
            this.duration = duration;
            this.clientWindow = clientWindow;
        }
    
        @Override
        public void run() {
            if (duration < 0) {
                clientWindow.moveToNextQuestion();  // Move to next question when time is up
                this.cancel();  // Stop the timer
                return;
            }
    
            // Change timer color as time runs out
            if (duration < 6)
                clientWindow.timer.setForeground(Color.red);
            else
                clientWindow.timer.setForeground(Color.black);
    
            clientWindow.timer.setText(duration + "");
            duration--;  // Decrease the timer
            clientWindow.window.repaint();
        }
    
        // Getter for the duration so it can be accessed in the ClientWindow
        public int getDuration() {
            return duration;
        }
    
        // Setter for the duration (if needed)
        public void setDuration(int duration) {
            this.duration = duration;
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
