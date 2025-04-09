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
    private static ConcurrentHashMap<String, ClientHandler> connectedClients = new ConcurrentHashMap<>(); // keeps track of clients
    private static ConcurrentHashMap<String, Integer> clientScores = new ConcurrentHashMap<>();
    private static QuestionHandler questionHandler = new QuestionHandler();
    private static boolean gameState;

    //main handles connection of new clients
    public static void main(String[] args) {

        // establish connection, port #, max connections, IP addresses
        try (ServerSocket serverSocket = new ServerSocket(1000, 15, InetAddress.getByName("0.0.0.0"))) {
            loadServerConfig("config/serverConfig.txt");
            System.out.println("Server started. Waiting for a client...");

            //gamestart handles game logic
            new Thread(() -> gameStart()).start();
            // Start a thread to listen for server terminal commands
            new Thread(() -> listenForCommands()).start();
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

    // Method to listen for server terminal commands
    private static void listenForCommands() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String command = scanner.nextLine().trim();
            if (command.startsWith("KILL:")) {
                String clientID = command.substring(5).trim(); // Extract the ClientID
                terminateClient(clientID); // Call the terminateClient method
            } else {
                System.out.println("Unknown command: " + command);
            }
        }
    }

    // Method to send a KILL signal to a specific client
    public static void terminateClient(String clientID) {
        ClientHandler clientHandler = connectedClients.get(clientID);
        if (clientHandler != null) {
            clientHandler.sendMessage("KILL");
            System.out.println("Sent KILL signal to ClientID=" + clientID);
            connectedClients.remove(clientID); // Remove the client from the connected clients map
        } else {
            System.err.println("ClientID=" + clientID + " not found.");
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
                String clientID = new String(packet.getData(), 0, packet.getLength()).trim();
                System.out.println("Received UDP message: ClientID=" + clientID);

                // Add the ClientID directly to the queue
                udpMessageQueue.add(clientID);
                System.out.println("Added to queue: ClientID=" + clientID);
            }
        } catch (IOException e) {
            // Handle errors related to UDP communication
            System.err.println("Error receiving UDP message: " + e.getMessage());
        }
    }

    // handles UDP message queue in separate thread
    public static class UDPThread implements Runnable {
        @Override
        public void run() {
            String clientID = udpMessageQueue.poll();
            if (clientID == null) {
                System.err.println("No messages in the UDP queue.");
                return;
            }

            clientID = clientID.trim(); // Ensure no extra spaces
            System.out.println("Looking up ClientID=" + clientID + " in connectedClients.");
            ClientHandler clientHandler = connectedClients.get(clientID);
            if (clientHandler != null) {
                clientHandler.sendMessage("ack");
                clientHandler.handleAck(true); // Mark that the client received a positive ack
                System.out.println("Sent 'ack' to ClientID=" + clientID);
            } else {
                System.err.println("No ClientHandler found for ClientID=" + clientID);
            }

            while (!udpMessageQueue.isEmpty()) {
                clientID = udpMessageQueue.poll().trim();
                System.out.println("Looking up ClientID=" + clientID + " in connectedClients.");
                clientHandler = connectedClients.get(clientID);
                if (clientHandler != null) {
                    clientHandler.sendMessage("negative-ack");
                    clientHandler.handleAck(false); // Mark that the client received a negative ack
                    System.out.println("Sent 'negative-ack' to ClientID=" + clientID);
                } else {
                    System.err.println("No ClientHandler found for ClientID=" + clientID);
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
            if (parts.length >= 2) {
                return parts[1].trim(); // Extract the clientID (second part of the message)
            }
        }
        return null; // Return null if the message format is invalid
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
        private ObjectOutputStream objectOut; // Used to send objects to the client
        private PrintWriter out; // Used to send messages to the client
        private String clientID; // Client ID
        private boolean hasSubmittedAnswer = false; // Flag to track if the client has submitted an answer
        private boolean receivedAck = false; // Flag to track if the client received a positive ack

        // Constructor to initialize the client socket
        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
            try {
                // Initialize the ObjectOutputStream
                objectOut = new ObjectOutputStream(clientSocket.getOutputStream());
            } catch (IOException e) {
                System.err.println("Error initializing ObjectOutputStream: " + e.getMessage());
            }
        }

        // Entry point for the client thread
        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                clientID = clientSocket.getInetAddress().getHostAddress().trim(); // Use IP address as ClientID
                connectedClients.put(clientID, this);
                System.out.println("Client registered as: " + clientID);

                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                    System.out.println("[" + clientID + "]: " + clientMessage);

                    // Handle submitted answers
                    if (clientMessage.startsWith("ANSWER:")) {
                        String submittedAnswer = clientMessage.substring(7).trim();
                        hasSubmittedAnswer = true; // Mark that the client has submitted an answer
                        handleAnswer(submittedAnswer);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error handling client: " + e.getMessage());
            } finally {
                connectedClients.remove(clientID);
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing socket: " + e.getMessage());
                }
                System.out.println("Connection with " + clientID + " closed.");
            }
        }

        // Handle the submitted answer
        public void handleAnswer(String submittedAnswer) {
            Question currentQuestion = questionHandler.getQuestionArray().get(0); // Get the current question

            if (submittedAnswer.equals(currentQuestion.getCorrectAnswer())) {
                // Trigger CORRECT if the submitted answer matches the correct answer
                sendMessage("CORRECT");
                System.out.println("Client " + clientID + " answered correctly.");
                clientScores.merge(clientID, 10, Integer::sum); // Add 10 points for a correct answer
            } else {
                // Trigger INCORRECT if the submitted answer does not match the correct answer
                sendMessage("INCORRECT");
                System.out.println("Client " + clientID + " answered incorrectly.");
                clientScores.merge(clientID, -10, Integer::sum); // Deduct 10 points for an incorrect answer
            }
        }

        // Handle timeout if no answer was submitted
        public void handleTimeout() {
            if (!hasSubmittedAnswer && receivedAck) {
                sendMessage("TIMEOUT");
                System.out.println("Client " + clientID + " did not submit an answer (TIMEOUT).");
                clientScores.merge(clientID, -10, Integer::sum); // Deduct 10 points for timeout
            }
        }

        // Reset the submission and ack flags for a new question
        public void resetFlags() {
            hasSubmittedAnswer = false;
            receivedAck = false;
        }

        // Method to handle positive acknowledgment
        public void handleAck(boolean isPositive) {
            receivedAck = isPositive;
        }

        // Method to send a Question object to the client
        public void sendQuestion(Question question) {
            try {
                if (objectOut != null) {
                    objectOut.writeObject(question); // Serialize and send the Question object
                    objectOut.flush();
                    System.out.println("Sent question to ClientID=" + clientID);
                }
            } catch (IOException e) {
                System.err.println("Error sending question: " + e.getMessage());
            }
        }

        // Sends a message to this client
        public void sendMessage(String message) {
            try {
                if (objectOut != null) {
                    objectOut.writeObject(message); // Send the string as a serialized object
                    objectOut.flush();
                    System.out.println("Sent message: " + message);
                }
            } catch (IOException e) {
                System.err.println("Error sending message: " + e.getMessage());
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

    //handles game logic 
    public static void gameStart() {
        Scanner scanner = new Scanner(System.in);

        // player must press enter on server to begin game
        System.out.println("When all players are connected, press enter to begin game.");
        scanner.nextLine();

        // game begins
        gameState = true;
        System.out.println("Game started!");

        for (ClientHandler clientHandler : connectedClients.values()) {
            //send message to all clients that game has started
            clientHandler.sendMessage("game-started!");
        }

        //game loop is active while the gameState is true and the array still has questions
        while(gameState && !questionHandler.outOfQuestions()){
            Question currentQuestion = questionHandler.getQuestionArray().get(0);
            // Reset submission and ack flags for all clients at the start of a new question
            for (ClientHandler clientHandler : connectedClients.values()) {
                clientHandler.resetFlags();
            }
            //for all clients
            for (ClientHandler clientHandler : connectedClients.values()) {
                //using clienthandler send a Question to each client at array index 
                //sends the first index at the questions array, after each question, first index gets removed
                questionHandler.questionToString();
                clientHandler.sendQuestion(currentQuestion);
            }

            // Start the polling timer (10 seconds for polling)
            System.out.println("Polling phase started...");
            ServerTimer pollingTimer = new ServerTimer(10, true);
            pollingTimer.start();

            // Process UDP messages
            System.out.println("Processing UDP messages...");
            new Thread(() -> acceptUDPMessage()).start();

            // Wait for polling to finish
            try {
                pollingTimer.join(); // Wait for the polling timer to finish
            } catch (InterruptedException e) {
                System.err.println("Polling timer interrupted: " + e.getMessage());
            }

            //after polling ends process the UDP messages
            UDPThread udpThread = new UDPThread();
            udpThread.run();

            // Start the answering timer (10 seconds for answering)
            System.out.println("Answering phase started...");
            ServerTimer answeringTimer = new ServerTimer(10, false);
            answeringTimer.start();

            // Wait for answering to finish
            try {
                answeringTimer.join(); // Wait for the answering timer to finish
            } catch (InterruptedException e) {
                System.err.println("Answering timer interrupted: " + e.getMessage());
            }
            // Print the scores of all clients
            System.out.println("Current Scores:");
            clientScores.forEach((clientID, score) -> {
                System.out.println("---------------------------------------------------------------------");
                System.out.println("ClientID: " + clientID + ", Score: " + score);
                System.out.println("---------------------------------------------------------------------");
            });

            // Remove the used question so the next can be displayed
            questionHandler.nextQuestion();
            System.out.println("Moving to the next question...");
        }
        System.out.println("Game Over!");
        scanner.close();
    }

    // Timer class to handle the countdown
    public static class ServerTimer extends Thread {
        private int duration;
        private boolean isPollingPhase;

        public ServerTimer(int duration, boolean isPollingPhase) {
            this.duration = duration;
            this.isPollingPhase = isPollingPhase;
        }

        @Override
        public void run() {
            while (duration > 0) {
                System.out.println((isPollingPhase ? "Polling" : "Answering") + " phase timer: " + duration + " seconds remaining...");

                // Broadcast the remaining time to all clients
                for (ClientHandler clientHandler : connectedClients.values()) {
                    clientHandler.sendMessage("TIMER:" + duration);
                }

                try {
                    Thread.sleep(1000); // Wait for 1 second
                } catch (InterruptedException e) {
                    System.err.println("Timer interrupted: " + e.getMessage());
                }
                duration--;
            }

            if (isPollingPhase) {
                System.out.println("Polling phase ended.");
            } else {
                System.out.println("Answering phase ended.");
                // Trigger timeout handling for all connected clients
                for (ClientHandler clientHandler : connectedClients.values()) {
                    clientHandler.handleTimeout(); // Handle timeout if no answer was submitted
                }
            }
        }
    }
}
