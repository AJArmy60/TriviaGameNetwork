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

    //variables for polling timer
    private TimerTask pollClock;
    private boolean pollPhase = true; // Track whether it's the polling phase
    
    private List<Question> questions;  // To hold questions and their options
    private int currentQuestionIndex = 0;  // Track the current question
    private int scoreCount = 0;  // Track the score
    private boolean answered = false;  // Flag to track if an answer has been submitted
    
    public ClientWindow() {
        // Setup the GUI...
        
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

                //submit.setEnabled(true);  // Enable Submit button //should only be enabled when positive ack is recieved
                //enableOptions(true);  // Enable options after Poll is clicked
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
            options[i].setEnabled(false); // Disable options at the start of the question
        }
    
        // Reset the Poll and Submit buttons
        //can poll
        poll.setEnabled(true);
        //cannot submit yet
        submit.setEnabled(false);
    
        // Start the polling timer (5 seconds for polling)
        timer.setText("5");
        pollPhase = true; // Set to polling phase
        pollClock = new TimerCode(5, this, true); // Pass `true` for polling phase
        Timer t = new Timer();
        t.schedule(pollClock, 0, 1000); // Schedule the polling timer to run every second
    
        // Reset the answered flag
        answered = false;
    }

    // Handle the submission of an answer
    private void handleAnswerSubmission() {
        for (int i = 0; i < options.length; i++) {
            if (options[i].isSelected()) {
                answered = true;  // Mark the question as answered
                
                if (questions.get(currentQuestionIndex).getOptions()[i].equals(questions.get(currentQuestionIndex).getCorrectAnswer())) {
                    scoreCount += 10; // Increment score by 10 for correct answer
                    score.setText("SCORE: " + scoreCount);
                } else {
                    scoreCount -= 10; // Decrement score by 10 for incorrect answer
                    score.setText("SCORE: " + scoreCount);
                }
                break;
            }
        }
    }

    // Enable or disable the options based on the argument
    private void enableOptions(boolean enable) {
        for (int i = 0; i < options.length; i++) {
            options[i].setEnabled(enable);
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
        private ClientWindow clientWindow;
        private boolean isPollingPhase; // Track whether this is the polling phase
    
        public TimerCode(int duration, ClientWindow clientWindow, boolean isPollingPhase) {
            this.duration = duration;
            this.clientWindow = clientWindow;
            this.isPollingPhase = isPollingPhase;
        }
    
        @Override
        public void run() {
            if (duration < 0) {
                if (isPollingPhase) {
                    // End of polling phase
                    pollPhase = false; // Switch to submission phase
                    poll.setEnabled(false); // Disable Poll button
                    clientWindow.enableOptions(false); // Disable options until ack is received
                    this.cancel(); // Stop the polling timer
    
                    // Start the submission timer (10 seconds for submission)
                    timer.setText("10");
                    Timer t = new Timer();
                    clock = new TimerCode(10, clientWindow, false); // Pass `false` for submission phase
                    t.schedule(clock, 0, 1000); // Schedule the submission timer
                } else {
                    // End of submission phase
                    if (!answered) {
                        scoreCount -= 20; // Subtract 20 points if no answer was submitted
                        score.setText("SCORE: " + scoreCount);
                    }
                    clientWindow.moveToNextQuestion(); // Move to the next question
                    this.cancel(); // Stop the submission timer
                }
                return;
            }
    
            // Update the timer display
            if (duration < 6)
                clientWindow.timer.setForeground(Color.red);
            else
                clientWindow.timer.setForeground(Color.black);
    
            clientWindow.timer.setText(duration + "");
            duration--;
            clientWindow.window.repaint();
        }
    }

	public int getCurrentQuestionIndex(){
		return currentQuestionIndex;
	}

    public void setCurrentQuestionIndex(int i){
        currentQuestionIndex = i;
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

	//determines which client can answer based off poll
    public void onAckReceived(Boolean ack) {
		//client is the first in queue, can answer question
        if (ack) {
            submit.setEnabled(true);
			enableOptions(true);
		//client was late, cannot answer question
        } else {
            submit.setEnabled(false);
			enableOptions(false);
        }
    }

    public void setClient(Client client) {
        this.client = client;
    }
}
