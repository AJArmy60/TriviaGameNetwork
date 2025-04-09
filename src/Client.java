import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.SwingUtilities;

public class Client {
    private static List<String> SERVER_IPS;
    private static int SERVER_PORT;
    private static int UDP_PORT = 2000;
    private static String DIRECTORY_PATH;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private ClientWindow clientWindow; // Reference to the ClientWindow
    private QuestionHandler questionHandler;
    private ObjectInputStream objectIn; // Used to receive objects from the server
    

    public Client(ClientWindow clientWindow, QuestionHandler questionHandler) {
        this.clientWindow = clientWindow;
        this.questionHandler = questionHandler;
    }

    private static void loadClientConfig(String filePath) {
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(filePath));
            SERVER_IPS = Arrays.asList(props.getProperty("server_ips").split(","));
            SERVER_PORT = Integer.parseInt(props.getProperty("port"));
            DIRECTORY_PATH = props.getProperty("directory_path");
            System.out.println("Loaded client configuration: SERVER_IPS=" + SERVER_IPS + ", PORT=" + SERVER_PORT + ", DIRECTORY_PATH=" + DIRECTORY_PATH);
        } catch (IOException e) {
            System.err.println("Error reading client config file: " + e.getMessage());
            System.exit(1);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number in config file: " + e.getMessage());
            System.exit(1);
        }
    }

    public void connectToServer() {
        try {
            // Establish a TCP connection to the server
            socket = new Socket(SERVER_IPS.get(0), SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true); // For sending strings
            in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // For receiving strings
            objectIn = new ObjectInputStream(socket.getInputStream()); // For receiving objects
    
            System.out.println("Connected to server.");
            out.println("Client connected: " + socket.getLocalAddress().getHostAddress() + ":" + socket.getLocalPort());
    
            // Start a thread to listen for server responses
            new Thread(() -> {
                try {
                    while (true) {
                        // Check if the server sent a string or an object
                        if (socket.getInputStream().available() > 0) {
                            Object receivedObject = objectIn.readObject();
                            if (receivedObject instanceof Question) {
                                // Handle the received Question object
                                Question question = (Question) receivedObject;
                                handleReceivedQuestion(question);
                            }
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    System.err.println("Error reading server response: " + e.getMessage());
                }
            }).start();
    
            // Start another thread to listen for string responses
            new Thread(() -> {
                try {
                    String response;
                    while ((response = in.readLine()) != null) {
                        handleServerResponse(response); // Handle the server's string response
                    }
                } catch (IOException e) {
                    System.err.println("Error reading server response: " + e.getMessage());
                }
            }).start();

            //another thread to handle answer feedback from server
            new Thread(() -> {
                try {
                    String response;
                    while ((response = in.readLine()) != null) {
                        if (response.equals("CORRECT")) {
                            clientWindow.updateScore(10); // Increment score by 10 for correct answer
                        } else if (response.equals("INCORRECT")) {
                            clientWindow.updateScore(-10); // Decrement score by 10 for incorrect answer
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error reading server response: " + e.getMessage());
                }
            }).start();
    
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
        }
    }

    //takes accepted question from server and passes it to ClientWindow logic
    public void handleReceivedQuestion(Question q){
        // Display the ClientWindow
        SwingUtilities.invokeLater(() -> clientWindow.setVisible(true));
        clientWindow.showQuestion(q);
        q.getCorrectAnswer();
    }

    //sends answer to server
    public void submitAnswer(String answer) {
        try {
            if (out != null) {
                out.println("ANSWER:" + answer); // Send the answer to the server
                System.out.println("Submitted answer: " + answer);
            }
        } catch (Exception e) {
            System.err.println("Error submitting answer: " + e.getMessage());
        }
    }

    public void sendUDP() {
        try {
            DatagramSocket udpSocket = new DatagramSocket();
            String clientID = InetAddress.getLocalHost().getHostName(); // Use hostname as ClientID
            int questionNumber = questionHandler.getCurrentQuestionIndex(); // Get the current question index
            String message = "buzz:" + clientID + ":" + questionNumber; // Format the message
            byte[] buffer = message.getBytes();

            InetAddress serverAddress = InetAddress.getByName(SERVER_IPS.get(0));
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, UDP_PORT);
            udpSocket.send(packet);

            System.out.println("UDP packet sent: " + message);
            udpSocket.close();
        } catch (IOException e) {
            System.err.println("Error sending UDP packet: " + e.getMessage());
        }
    }

    private void handleServerResponse(String response) {
        // Handle the server's TCP response (ack or negative-ack)
        if (response.equals("ack")) {
            // Received "ack" from the server
            clientWindow.onAckReceived(true);
            System.out.println("Received ack from server.");
        } else if (response.equals("negative-ack")) {
            // Received "negative-ack" from the server
            clientWindow.onAckReceived(false);
            System.out.println("Received negative-ack from server.");
        }
    }

    public void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            System.out.println("Connection closed.");
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    public void setClientWindow(ClientWindow clientWindow) {
        this.clientWindow = clientWindow;
    }

    public static void main(String[] args) {
    loadClientConfig("config/clientConfig.txt");

    // Create the QuestionHandler
    QuestionHandler questionHandler = new QuestionHandler();

    // Create the Client instance
    Client client = new Client(null, questionHandler); // Pass null for ClientWindow initially

    // Connect to the server before creating the ClientWindow
    client.connectToServer();

    // Create the ClientWindow after the connection is established
    ClientWindow window = new ClientWindow();
    SwingUtilities.invokeLater(() -> window.setVisible(false));
    client.setClientWindow(window); // Set the ClientWindow in the Client instance
    window.setClient(client); // Pass the Client instance to the ClientWindow
}
}