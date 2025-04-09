import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
    private JFrame window;

    private int scoreCount = 0;  // Track the score
    private boolean answered = false;  // Flag to track if an answer has been submitted
    
    public ClientWindow() {
        JOptionPane.showMessageDialog(window, "This is a trivia game");

        window = new JFrame("Trivia");
        question = new JLabel("Q1. This is a sample question"); // represents the question
        window.add(question);
        question.setBounds(10, 5, 350, 100);
        
        options = new JRadioButton[4];
        optionGroup = new ButtonGroup();
        for(int index = 0; index < options.length; index++) {
            options[index] = new JRadioButton("Option " + (index + 1));  // represents an option
            options[index].addActionListener(this);
            options[index].setBounds(10, 110 + (index * 20), 350, 20);
            window.add(options[index]);
            optionGroup.add(options[index]);
        }

        timer = new JLabel("TIMER");
        timer.setBounds(250, 250, 100, 20);
        window.add(timer);

        score = new JLabel("SCORE");
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

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("You clicked " + e.getActionCommand());

        String input = e.getActionCommand();
        switch (input) {
            case "Poll":
                poll.setEnabled(false);  // Disable Poll button
                
                submit.setEnabled(true);  // Enable Submit button //should only be enabled when positive ack is recieved
                submit.setEnabled(true);  // Enable Submit button
                enableOptions(true);  // Enable options after Poll is clicked
                client.sendUDP(); // Send UDP packet to server
                break;
            case "Submit":
                handleAnswerSubmission();  // Handle the answer submission
                poll.setEnabled(false);  // Disable Poll button after Submit
                submit.setEnabled(false);  // Disable Submit button after submission
                enableOptions(false);  // Disable options after Submit
                break;
            default:
                break;
        }
    }

    public void showQuestion(Question currentQuestion) {
        SwingUtilities.invokeLater(() -> {
            if (currentQuestion == null) {
                JOptionPane.showMessageDialog(window, "No question received.");
                return;
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

            // Reset the answered flag
            answered = false;
        });
    }

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

    private void enableOptions(boolean enable) {
        for (int i = 0; i < options.length; i++) {
            options[i].setEnabled(enable);
        }
    }

    public void onAckReceived(Boolean ack) {
        // Client is the first in queue, can answer the question
        if (ack) {
            submit.setEnabled(true);
            enableOptions(true);
        } else {
            submit.setEnabled(false);
            enableOptions(false);
        }
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public void enablePollButton() {
        SwingUtilities.invokeLater(() -> {
            poll.setEnabled(true); // Enable the Poll button
        });
    }

    public void updateTimer(int remainingTime) {
        SwingUtilities.invokeLater(() -> {
            if (remainingTime < 6) {
                timer.setForeground(Color.red); // Change color to red for the last 5 seconds
            } else {
                timer.setForeground(Color.black);
            }
            timer.setText(String.valueOf(remainingTime)); // Update the timer display
        });
    }
}
