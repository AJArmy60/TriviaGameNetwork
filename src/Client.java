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

    public Client(ClientWindow clientWindow) {
        this.clientWindow = clientWindow;
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
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Connected to server.");
            out.println("Client connected: " + socket.getLocalAddress().getHostAddress() + ":" + socket.getLocalPort());

            // Start a thread to listen for server responses
            new Thread(() -> {
                try {
                    String response;
                    while ((response = in.readLine()) != null) {
                        handleServerResponse(response); // Handle the server's response
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
            long threadId = Thread.currentThread().getId(); // Get the current thread's ID
            String message = "Thread ID: " + threadId; // Create a message with the thread ID
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
        } else if (response.equals("false")) {
            // Received "negative-ack" from the server
            clientWindow.onAckReceived(false);
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
        Client client = new Client(window);
        window.setClient(client); // Pass the client to the ClientWindow

        client.connectToServer();
    }
}