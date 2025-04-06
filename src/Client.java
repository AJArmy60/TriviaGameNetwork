import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;

public class Client 
{
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int NODE_ID = secureRandom.nextInt(1000);
    private static List<String> SERVER_IPS;
    private static int SERVER_PORT;
    private static String DIRECTORY_PATH;
    private static int versionCounter = 1;

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
}
