import java.io.*;
import java.net.*;
import java.util.*;

public class UsersClient {
    private final String serverHost;
    private final int serverPort;

    public UsersClient(String host, int port) {
        this.serverHost = host;
        this.serverPort = port;
    }

    public List<String> getUsers(int serverPortFilter, String currentUser) {
        List<String> users = new ArrayList<>();
        try (Socket socket = new Socket(serverHost, serverPort);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println("USER " + serverPortFilter + " " + currentUser);
            String response = in.readLine();
            if (response != null && response.startsWith("USERS ")) {
                String listStr = response.substring(6); 
                if (!listStr.isEmpty()) {
                    users = Arrays.asList(listStr.split(","));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return users;
    }


}
