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
            objectIn = new ObjectInputStream(socket.getInputStream()); // For receiving objects and strings

            System.out.println("Connected to server.");
            out.println("Client connected: " + socket.getLocalAddress().getHostAddress() + ":" + socket.getLocalPort());

            // Start a thread to listen for server responses
            new Thread(() -> {
                try {
                    while (true) {
                        // Read the incoming object
                        Object receivedObject = objectIn.readObject();
                        if (receivedObject instanceof String) {
                            System.out.println("Received string from server: " + receivedObject); // Debug log
                            handleServerResponse((String) receivedObject);
                        } else if (receivedObject instanceof Question) {
                            handleReceivedQuestion((Question) receivedObject);
                        } else {
                            System.err.println("Unknown object type received: " + receivedObject.getClass().getName());
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error reading server response (IO): " + e.getMessage());
                } catch (ClassNotFoundException e) {
                    System.err.println("Error reading server response (ClassNotFound): " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("Unexpected error in response thread: " + e.getMessage());
                }
            }).start();
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
        }
    }

    //takes accepted question from server and passes it to ClientWindow logic
    public void handleReceivedQuestion(Question q) {
        SwingUtilities.invokeLater(() -> {
            System.out.println("Question received");
            clientWindow.showQuestion(q); // Delegate question display and timer handling to ClientWindow
        });
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

            // Use only the IP address as the ClientID
            String clientID = socket.getLocalAddress().getHostAddress();
            String message = "buzz:" + clientID; // Standardized message format
            byte[] buffer = message.getBytes();

            InetAddress serverAddress = InetAddress.getByName(SERVER_IPS.get(0));
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, UDP_PORT);
            udpSocket.send(packet);

            System.out.println("UDP packet sent: " + message);
            udpSocket.close();
        } catch (IOException e) {
            System.err.println("Error sending UDP packet: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error in sendUDP: " + e.getMessage());
        }
    }

    private void handleServerResponse(String response) {
        if (response.startsWith("TIMER:")) {
            // Extract the remaining time from the message
            int remainingTime = Integer.parseInt(response.substring(6).trim());
            clientWindow.updateTimer(remainingTime); // Update the timer in the ClientWindow
        } else if (response.equals("ack")) {
            clientWindow.onAckReceived(true);
            System.out.println("Received ack from server.");
        } else if (response.equals("negative-ack")) {
            clientWindow.onAckReceived(false);
            System.out.println("Received negative-ack from server.");
        } else if (response.equals("game-started!")) {
            clientWindow.enablePollButton();
            System.out.println("Game started! Poll button enabled.");

        //handle score
        } else if (response.equals("CORRECT")) {
            clientWindow.updateScore(10);
        } else if (response.equals("INCORRECT")) {
            clientWindow.updateScore(-10);
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

        // Create the ClientWindow and pass it to the Client
        ClientWindow window = new ClientWindow();
        QuestionHandler questionHandler = new QuestionHandler();
        Client client = new Client(window, questionHandler);
        window.setClient(client); // Pass the client to the ClientWindow

        client.connectToServer();
    }
}