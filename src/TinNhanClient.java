import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class TinNhanClient {
    private final String serverHost;
    private final int serverPort;

    public TinNhanClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public String sendMessage(String from, String to, String msg) {
        try (Socket s = new Socket(serverHost, serverPort);
             var out = new PrintWriter(s.getOutputStream(), true);
             var in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {

            out.println("SEND_MESSENGER " + from + " " + to + " " + msg);
            return in.readLine();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
    
    public String sendFileMessage(String sender, String receiver, Path filePath) {
        try (Socket s = new Socket(serverHost, serverPort);
             var out = new PrintWriter(s.getOutputStream(), true);
             var in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {

            byte[] fileBytes = Files.readAllBytes(filePath);
            String base64Data = Base64.getEncoder().encodeToString(fileBytes);

            String fileName = filePath.getFileName().toString();
            String format = "";
            int dot = fileName.lastIndexOf('.');
            if (dot != -1) format = fileName.substring(dot + 1);

            String cmd = String.format("SEND_FILE %s %s \"%s\" \"%s\" \"%s\"",
                    sender, receiver, fileName, format, base64Data);

            out.println(cmd);
            return in.readLine(); 

        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
    public void downloadFile(String fileName) {
        try (Socket s = new Socket(serverHost, serverPort);
             InputStream in = s.getInputStream();
             OutputStream out = s.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {

            PrintWriter writer = new PrintWriter(out, true);
            writer.println("DOWNLOAD_FILE " + fileName);

            String response = reader.readLine();
            if (response.startsWith("START ")) {
                long fileSize = Long.parseLong(response.split(" ")[1]);
                File filesDir = new File("Files");
                if (!filesDir.exists()) filesDir.mkdirs();
                File file = new File(filesDir, fileName);

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    byte[] buffer = new byte[4096];
                    long remaining = fileSize;
                    int read;
                    while (remaining > 0 && (read = in.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                        fos.write(buffer, 0, read);
                        remaining -= read;
                    }
                    fos.flush();
                }

                System.out.println("Downloaded file: " + file.getAbsolutePath());
            } else {
                System.out.println("Server error: " + response);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    public String sendVoiceMessage(String sender, String receiver, Path voiceFilePath) {
        try (Socket s = new Socket(serverHost, serverPort);
             var out = new PrintWriter(s.getOutputStream(), true);
             var in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {

            byte[] voiceBytes = Files.readAllBytes(voiceFilePath);
            String base64Data = Base64.getEncoder().encodeToString(voiceBytes);

            String fileName = voiceFilePath.getFileName().toString();
            String format = "";
            int dot = fileName.lastIndexOf('.');
            if (dot != -1) format = fileName.substring(dot + 1);

            String cmd = String.format("VOICE_MESSENGER %s %s \"%s\" \"%s\" \"%s\"",
                    sender, receiver, fileName, format, base64Data);

            out.println(cmd);
            return in.readLine();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
    
    public String getChat(String user1, String user2) {
        try (Socket s = new Socket(serverHost, serverPort);
             var out = new PrintWriter(s.getOutputStream(), true);
             var in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {

            out.println("GET_CHAT " + user1 + " " + user2);
            return in.readLine(); 
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    public void downloadVoice(String fileName) {
        try (Socket s = new Socket(serverHost, serverPort);
             InputStream in = s.getInputStream();
             OutputStream out = s.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {

            PrintWriter writer = new PrintWriter(out, true);
            writer.println("DOWNLOAD_VOICE " + fileName);

            String response = reader.readLine();
            if (response.startsWith("START ")) {
                long fileSize = Long.parseLong(response.split(" ")[1]);
                File voiceDir = new File("GhiAm");
                if (!voiceDir.exists()) voiceDir.mkdirs();
                File file = new File(voiceDir, fileName);

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    byte[] buffer = new byte[4096];
                    long remaining = fileSize;
                    int read;
                    while (remaining > 0 && (read = in.read(buffer, 0, (int)Math.min(buffer.length, remaining))) != -1) {
                        fos.write(buffer, 0, read);
                        remaining -= read;
                    }
                    fos.flush();
                }

                System.out.println("Downloaded voice file: " + file.getAbsolutePath());
            } else {
                System.out.println("Server error: " + response);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    

}
