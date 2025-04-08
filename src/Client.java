import java.io.*;
import java.net.*;
import java.util.*;

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
    
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
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
        if (response.equals("true")) {
            // Received "ack" from the server
            clientWindow.onAckReceived(true);
            System.out.println("Received ack from server.");
        } else if (response.equals("false")) {
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