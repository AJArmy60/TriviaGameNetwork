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
    }

    public void setVisible(boolean visible) {
        window.setVisible(visible); // Delegate visibility to the JFrame
    }

    // This method is called when you check/uncheck any radio button or press submit/poll
    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("You clicked " + e.getActionCommand());
    
        String input = e.getActionCommand();
        switch (input) {
            case "Poll":
                poll.setEnabled(false);  // Disable Poll button

                submit.setEnabled(true);  // Enable Submit button //should only be enabled when positive ack is recieved
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

    // Show the given Question object
    public void showQuestion(Question currentQuestion) {
        SwingUtilities.invokeLater(() -> {
            // Set the question text
            question.setText(currentQuestion.getQuestion());
    
            // Set the options for the question
            String[] optionsArray = currentQuestion.getOptions();
            for (int i = 0; i < options.length; i++) {
                options[i].setText(optionsArray[i]);
                options[i].setEnabled(false); // Disable options at the start of the question
            }
    
            // Reset the Poll and Submit buttons
            poll.setEnabled(true); // Enable Poll button
            submit.setEnabled(false); // Disable Submit button initially
    
            // Start the polling timer (5 seconds for polling)
            startPollingTimer();
    
            // Reset the answered flag
            answered = false;
        });
    }

    private void startPollingTimer() {
        // Cancel any existing timer
        if (pollClock != null) {
            pollClock.cancel();
        }
    
        // Reset the timer display
        timer.setText("5");
        pollPhase = true; // Set to polling phase
    
        // Create and schedule the polling timer
        pollClock = new TimerCode(5, this, true); // Pass `true` for polling phase
        Timer t = new Timer();
        t.schedule(pollClock, 0, 1000); // Schedule the polling timer to run every second
    }

    //Handle the submission of an answer
    private void handleAnswerSubmission() {
        String selectedAnswer = null;
    
        // Find the selected option
        for (int i = 0; i < options.length; i++) {
            if (options[i].isSelected()) {
                selectedAnswer = options[i].getText(); // Get the text of the selected option
                break;
            }
        }
    
        if (selectedAnswer != null) {
            answered = true; // Mark the question as answered
            client.submitAnswer(selectedAnswer); // Send the selected answer to the Client class
            poll.setEnabled(false); // Disable Poll button after Submit
            submit.setEnabled(false); // Disable Submit button after submission
            enableOptions(false); // Disable options after Submit
        } else {
            JOptionPane.showMessageDialog(window, "Please select an answer before submitting.");
        }
    }

    public void updateScore(int delta) {
        scoreCount += delta;
        SwingUtilities.invokeLater(() -> score.setText("SCORE: " + scoreCount));
    }

    // Enable or disable the options based on the argument
    private void enableOptions(boolean enable) {
        for (int i = 0; i < options.length; i++) {
            options[i].setEnabled(enable);
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
                    //clientWindow.moveToNextQuestion(); // Move to the next question
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
