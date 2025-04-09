import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
    private Timer activeTimer; // Track the currently active Timer
    
    public ClientWindow()
	{
		JOptionPane.showMessageDialog(window, "This is a trivia game");
		
		window = new JFrame("Trivia");
		question = new JLabel("Q1. This is a sample question"); // represents the question
		window.add(question);
		question.setBounds(10, 5, 350, 100);;
		
		options = new JRadioButton[4];
		optionGroup = new ButtonGroup();
		for(int index=0; index<options.length; index++)
		{
			options[index] = new JRadioButton("Option " + (index+1));  // represents an option
			// if a radio button is clicked, the event would be thrown to this class to handle
			options[index].addActionListener(this);
			options[index].setBounds(10, 110+(index*20), 350, 20);
			window.add(options[index]);
			optionGroup.add(options[index]);
		}

		timer = new JLabel("TIMER");  // represents the countdown shown on the window
		timer.setBounds(250, 250, 100, 20);
		clock = new TimerCode(30, this, true);  // represents clocked task that should run after X seconds
		Timer t = new Timer();  // event generator
		t.schedule(clock, 0, 1000); // clock is called every second
		window.add(timer);
		
		
		score = new JLabel("SCORE"); // represents the score
		score.setBounds(50, 250, 100, 20);
		window.add(score);

		poll = new JButton("Poll");  // button that use clicks/ like a buzzer
		poll.setBounds(10, 300, 100, 20);
		poll.addActionListener(this);  // calls actionPerformed of this class
		window.add(poll);
		
		submit = new JButton("Submit");  // button to submit their answer
		submit.setBounds(200, 300, 100, 20);
		submit.addActionListener(this);  // calls actionPerformed of this class
		window.add(submit);
		
		
		window.setSize(400,400);
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

    // Show the given Question object
    public void showQuestion(Question currentQuestion) {
        SwingUtilities.invokeLater(() -> {
            if (currentQuestion == null) {
                JOptionPane.showMessageDialog(window, "No question received.");
                return;
            }

            // Cancel any existing timer
            if (activeTimer != null) {
                activeTimer.cancel();
            }

            // Set the question text
            question.setText(currentQuestion.getQuestion());
    
            // Set the options for the question
            String[] optionsArray = currentQuestion.getOptions();
            if (optionsArray == null || optionsArray.length != options.length) {
                JOptionPane.showMessageDialog(window, "Invalid options received.");
                return;
            }

            for (int i = 0; i < options.length; i++) {
                options[i].setText(optionsArray[i]);
                options[i].setEnabled(false); // Disable options at the start of the question
            }
    
            // Reset the Poll and Submit buttons
            poll.setEnabled(true); // Enable Poll button
            submit.setEnabled(false); // Disable Submit button initially
    
            // Start the question timer (e.g., 20 seconds total: 10 for polling, 10 for submission)
            startQuestionTimer(20);
        });
    }

    private void startQuestionTimer(int totalDuration) {
        // Cancel any existing timer
        if (activeTimer != null) {
            activeTimer.cancel();
        }

        // Reset the timer display
        timer.setText(String.valueOf(totalDuration));
        pollPhase = true; // Start with the polling phase
        answered = false; // Reset the answered flag

        // Create and schedule the question timer
        activeTimer = new Timer();
        activeTimer.schedule(new TimerTask() {
            private int remainingTime = totalDuration;

            @Override
            public void run() {
                if (remainingTime <= 0) {
                    // End of question timer
                    if (pollPhase) {
                        // Transition from polling to submission phase
                        pollPhase = false;
                        poll.setEnabled(false); // Disable Poll button
                        enableOptions(false); // Disable options until ack is received
                        remainingTime = totalDuration / 2; // Set remaining time for submission phase
                        timer.setText(String.valueOf(remainingTime));
                    } else {
                        // End of submission phase
                        if (!answered) {
                            scoreCount -= 20; // Subtract 20 points if no answer was submitted
                            SwingUtilities.invokeLater(() -> score.setText("SCORE: " + scoreCount));
                        }
                        activeTimer.cancel(); // Stop the timer
                    }
                    return;
                }

                // Update the timer display
                SwingUtilities.invokeLater(() -> {
                    timer.setText(String.valueOf(remainingTime));
                    timer.setForeground(remainingTime <= 5 ? Color.red : Color.black);
                });

                remainingTime--;
            }
        }, 0, 1000); // Schedule the timer to run every second
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
                    if (activeTimer != null) {
                        activeTimer.cancel(); // Cancel the previous timer
                    }
                    activeTimer = new Timer();
                    clock = new TimerCode(10, clientWindow, false); // Pass `false` for submission phase
                    activeTimer.schedule(clock, 0, 1000); // Schedule the submission timer
                } else {
                    // End of submission phase
                    if (!answered) {
                        scoreCount -= 20; // Subtract 20 points if no answer was submitted
                        score.setText("SCORE: " + scoreCount);
                    }
                    //clientWindow.moveToNextQuestion(); // Move to the next question
                    this.cancel(); // Stop the submission timer
                    if (activeTimer != null) {
                        activeTimer.cancel(); // Cancel the active timer
                    }
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

        // During submission phase
        SwingUtilities.invokeLater(() -> {
            if (ack) {
                submit.setEnabled(true);
                enableOptions(true);
            } else {
                submit.setEnabled(false);
                enableOptions(false);
            }
        });
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public void enablePollButton() {
        SwingUtilities.invokeLater(() -> {
            poll.setEnabled(true); // Enable the Poll button
        });
    }
}
