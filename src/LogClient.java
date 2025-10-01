import java.io.*;
import java.net.*;

public class LogClient {

    private final String serverHost;
    private final int serverPort;

    public LogClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public String login(String username, String password) {
        try (Socket s = new Socket(serverHost, serverPort);
             var out = new PrintWriter(s.getOutputStream(), true);
             var in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            
            out.println("LOGIN " + username + " " + password);
            return in.readLine();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    public String logout(String username) {
        try (Socket s = new Socket(serverHost, serverPort);
             var out = new PrintWriter(s.getOutputStream(), true);
             var in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {

            out.println("LOGOUT " + username);
            return in.readLine();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
}
