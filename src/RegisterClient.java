import java.io.*;
import java.net.*;

public class RegisterClient {

    private final String serverHost;
    private final int serverPort;

    public RegisterClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public String register(String username, String password, int listenPort, int serverPort) {
        try (Socket s = new Socket(serverHost, serverPort);
             var out = new PrintWriter(s.getOutputStream(), true);
             var in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            
            out.println("REGISTER " + username + " " + password + " " + listenPort + " " + serverPort);
            return in.readLine();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    public String unregister(String username) {
        try (Socket s = new Socket(serverHost, serverPort);
             var out = new PrintWriter(s.getOutputStream(), true);
             var in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            
            out.println("UNREGISTER " + username);
            return in.readLine();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
}
