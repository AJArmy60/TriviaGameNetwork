import java.io.*;
import java.net.*;
import java.util.Properties;

public class Server {

    private static int TCP_PORT; 
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

    static class ClientHandler implements Runnable {
        private Socket clientSocket;
    
        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }
    
        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
            ) {
                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                    System.out.println("Received from client: " + clientMessage);
                    out.println("Server received: " + clientMessage); // Send a response back to client
                }
            } catch (IOException e) {
                System.err.println("Error handling client: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close(); // Close the client socket when done
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
                System.out.println("Connection with client closed.");
            }
        }
    }
}