import java.io.*;
import java.net.*;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private static int TCP_PORT; 
    private static ConcurrentHashMap<String, ClientHandler> connectedClients = new ConcurrentHashMap<>(); //keeps track of clients
    public boolean gameState;
    public static void main(String[] args) {

        //establish connection, port #, max connections, IP addresses
        try (ServerSocket serverSocket = new ServerSocket(1000, 15, InetAddress.getByName("0.0.0.0"))) {
            loadServerConfig("config/serverConfig.txt");
            System.out.println("Server started. Waiting for a client...");
            while(true){
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // Create a new thread to handle the client
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();  // Start the thread
            }

        } catch (SocketException e) {
            // Handle errors during socket creation
            System.err.println("Error creating or configuring socket: " + e.getMessage());
        } catch (Exception e) {
            // Handle unexpected errors
            System.err.println("Unexpected error: " + e.getMessage());
        }
    }

    // Load server configuration from the provided file path
    private static void loadServerConfig(String serverConfigPath) {
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(serverConfigPath));
            TCP_PORT = Integer.parseInt(props.getProperty("port"));
            System.out.println("Loaded server configuration: PORT=" + TCP_PORT);
        } catch (IOException | NumberFormatException e) {
            // Handle errors related to loading or parsing the config file
            System.err.println("Error reading server config file: " + e.getMessage());
            System.exit(1);
        }
    }

    // Handles communication with a single client connected to the server
    static class ClientHandler implements Runnable {
        private Socket clientSocket;         // The socket used for communicating with the client
        private PrintWriter out;             // Used to send messages to the client
        private String clientID;           // Client ID

        // Constructor to initialize the client socket
        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        // Entry point for the client thread
        @Override
        public void run() {
            try (
                // Input stream to receive messages from the client
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            ) {
                // Output stream to send messages to the client
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                // Read the first message from the client, which is expected to be their name or ID
                clientID = in.readLine();
                if (clientID == null || clientID.isEmpty()) {
                    // Fallback to the client's IP address if no name was provided
                    clientID = clientSocket.getInetAddress().toString();
                }

                // Store this client in the shared map so the server can communicate with them later
                connectedClients.put(clientID, this);
                System.out.println("Client registered as: " + clientID);

                // Listen for further messages from the client in a loop
                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                    // Display message on server console
                    System.out.println("[" + clientID + "]: " + clientMessage);
                    // Send acknowledgment back to the client
                    out.println("Server received: " + clientMessage);
                }
            } catch (IOException e) {
                // Handle any communication errors
                System.err.println("Error handling client: " + e.getMessage());
            } finally {
                // Remove the client from the map when they disconnect
                connectedClients.remove(clientID);
                try {
                    clientSocket.close(); // Always close the socket when done
                } catch (IOException e) {
                    System.err.println("Error closing socket: " + e.getMessage());
                }
                System.out.println("Connection with " + clientID + " closed.");
            }
        }

        // Sends a message to this client
        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        // Returns the name or identifier of this client
        public String getclientID() {
            return clientID;
        }
    }

    private void gameStart(){
        Scanner scanner = new Scanner(System.in);

        //player must press enter on server to begin game
        System.out.println("When all players are connected, press enter to begin game.");
        scanner.nextLine();

        //game begins
        gameState = true;

        scanner.close();
    }
}