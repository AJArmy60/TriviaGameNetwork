import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.*;

public class Client 
{
    private static final SecureRandom secureRandom = new SecureRandom();
    private static List<String> SERVER_IPS;
    private static int SERVER_PORT;
    private static String DIRECTORY_PATH;

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

    public static void main(String[] args)
    {
        loadClientConfig("config/clientConfig.txt");

        ClientWindow window = new ClientWindow();

        try (Socket socket = new Socket(SERVER_IPS.get(0), SERVER_PORT))
        {
            //Create input and output streams for communication with the server
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("Connected to server. Type messages to send.");

            String message;
            while ((message = userInput.readLine()) != null) {
                out.println(message); // Send message to server
                String response = in.readLine(); // Read server's response
                System.out.println("Server response: " + response);
            }

            // Close resources
            in.close();
            out.close();
            userInput.close();
            socket.close();
            System.out.println("Connection closed.");
        }
        catch (IOException e)
        {
            System.err.println("Client error: " + e.getMessage());
        }
    }
    
    //sends a UDP packet ping to the server
    public void sendUDP() {
        try {
            DatagramSocket socket = new DatagramSocket();
            long threadId = Thread.currentThread().getId(); // Get the current thread's ID
            String message = "Thread ID: " + threadId; // Create a message with the thread ID
            byte[] buffer = message.getBytes(); // Convert the message to bytes
    
            InetAddress address = InetAddress.getByName(SERVER_IPS.get(0));
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, SERVER_PORT);
            socket.send(packet); // Send the packet
    
            System.out.println("UDP packet sent to " + SERVER_IPS.get(0) + ":" + SERVER_PORT + " with message: " + message);
            socket.close();
        } catch (IOException e) {
            System.err.println("Error sending UDP packet: " + e.getMessage());
        }
    }
}
