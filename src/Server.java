import java.io.*;
import java.net.*;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Server {

    private static int TCP_PORT;
    private static int UDP_PORT = 2000; // port for UDP
    private static ConcurrentLinkedQueue<String> udpMessageQueue = new ConcurrentLinkedQueue<>();
    private static ConcurrentHashMap<String, ClientHandler> connectedClients = new ConcurrentHashMap<>(); // keeps track
                                                                                                          // of clients
    private static boolean gameState;

    public static void main(String[] args) {

        // establish connection, port #, max connections, IP addresses
        try (ServerSocket serverSocket = new ServerSocket(1000, 15, InetAddress.getByName("0.0.0.0"))) {
            loadServerConfig("config/serverConfig.txt");
            System.out.println("Server started. Waiting for a client...");

            gameStart();
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // Create a new thread to handle the client
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start(); // Start the thread
            }

        } catch (SocketException e) {
            // Handle errors during socket creation
            System.err.println("Error creating or configuring socket: " + e.getMessage());
        } catch (Exception e) {
            // Handle unexpected errors
            System.err.println("Unexpected error: " + e.getMessage());
        }
    }

    public static void acceptUDPMessage() {
        // UDP message acceptance
        try (DatagramSocket socket = new DatagramSocket(UDP_PORT)) {
            System.out.println("Listening for UDP messages on port " + UDP_PORT);
            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // Receive the UDP packet

                // Extract the message from the packet
                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received UDP message: " + message);

                // Parse the message (expected format: "buzz:<ClientID>:<QuestionNumber>")
                String[] parts = message.split(":");
                if (parts.length == 3 && parts[0].equals("buzz")) {
                    String clientID = parts[1];
                    int questionNumber = Integer.parseInt(parts[2]);

                    // Add the message to the queue
                    udpMessageQueue.add(clientID + ":" + questionNumber);
                    System.out
                            .println("Added to queue: ClientID=" + clientID + ", for QuestionNumber=" + questionNumber);
                } else {
                    System.err.println("Invalid UDP message format: " + message);
                }
            }
        } catch (IOException e) {
            // Handle errors related to UDP communication
            System.err.println("Error receiving UDP message: " + e.getMessage());
        }
    }

    // handles UDP message queue in separate thread
    // must be called once per each time the queue is full
    public static class UDPThread implements Runnable {
        @Override
        public void run() {
            // Peek at the first message in the queue without removing it
            String message = udpMessageQueue.poll();
            String clientID = getClientID(message);
            ClientHandler clientHandler = connectedClients.get(clientID);
            if (clientHandler != null) {
                clientHandler.sendMessage("ack");
                System.out.println("Sent 'ack' to ClientID=" + clientID);
            }

            // sends negative ack to other clients in queue
            while (!udpMessageQueue.isEmpty()) {
                message = udpMessageQueue.poll();
                clientID = getClientID(message);
                clientHandler = connectedClients.get(clientID);
                if (clientHandler != null) {
                    clientHandler.sendMessage("negative-ack");
                    System.out.println("Sent 'negative-ack' to ClientID=" + clientID);
                }
            }

            // Sleep briefly to avoid busy-waiting
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                System.err.println("UDPThread interrupted: " + e.getMessage());
            }
        }
    }

    public static String getClientID(String message) {
        if (message != null) {
            String[] parts = message.split(":");
            String clientID = parts[0]; // Extract the clientID
            //int questionNumber = Integer.parseInt(parts[1]); // Extract the question number
            return clientID;
        }
        return "Failed to parse message";
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
        private Socket clientSocket; // The socket used for communicating with the client
        private PrintWriter out; // Used to send messages to the client
        private String clientID; // Client ID

        // Constructor to initialize the client socket
        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        // Entry point for the client thread
        @Override
        public void run() {
            try (
                    // Input stream to receive messages from the client
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));) {
                // Output stream to send messages to the client
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                // Read the first message from the client, which is expected to be their name or
                // ID
                clientID = in.readLine();
                if (clientID == null || clientID.isEmpty()) {
                    // Fallback to the client's IP address if no name was provided
                    clientID = clientSocket.getInetAddress().toString();
                }

                // Store this client in the shared map so the server can communicate with them
                // later
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

    public Boolean getGameState() {
        return gameState;
    }

    public static void gameStart() {
        Scanner scanner = new Scanner(System.in);

        // player must press enter on server to begin game
        System.out.println("When all players are connected, press enter to begin game.");
        scanner.nextLine();

        // game begins
        gameState = true;
        System.out.println("Game started!");
        while(gameState){
            acceptUDPMessage();
        }
        scanner.close();
    }
}