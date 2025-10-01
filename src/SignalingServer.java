import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.sql.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignalingServer {
    private static final String DB_URL = "jdbc:mariadb://localhost:3306/chatvoice";
    private static final String DB_USER = "chatuser";
    private static final String DB_PASS = "chat123";

    public static void main(String[] args) throws Exception {
        Class.forName("org.mariadb.jdbc.Driver"); 
        int port = 5000;
        if (args.length > 0) port = Integer.parseInt(args[0]);

        ServerSocket server = new ServerSocket();
        server.setReuseAddress(true);
        server.bind(new InetSocketAddress(port));

        System.out.println("Server Signaling đã khởi động trên cổng " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { server.close(); } catch (IOException e) { e.printStackTrace(); }
            System.out.println("Socket server đã đóng khi tắt server");
        }));

        while (true) {
            try {
                Socket client = server.accept();
                new Thread(() -> handleClient(client)).start();
            } catch (SocketException se) {
                System.out.println("Socket server đã đóng, thoát vòng lặp accept.");
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void handleClient(Socket s) {
        try (s;
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
             PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {

            String line = in.readLine();
            if (line == null) return;

            String[] parts = line.split(" ");
            String cmd = parts[0].toUpperCase();

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                switch (cmd) {
                    case "REGISTER" -> handleRegister(parts, s, conn, out);
                    case "LOOKUP" -> handleLookup(parts, conn, out);
                    case "UNREGISTER" -> handleUnregister(parts, conn, out);
                    case "LOGIN" -> handleLogin(parts, conn, out);
                    case "USER" -> handleUser(parts, conn, out);
                    case "SEND_MESSENGER" -> handleSend(parts, conn, out);
                    case "SEND_FILE" -> handleSendFile(parts, conn, out);
                    case "DOWNLOAD_FILE" -> handleDownloadFile(parts, s);
                    case "GET_CHAT" -> handleGetChat(parts, conn, out);
                    case "VOICE_MESSENGER" -> handleVoiceMessenger(parts, conn, out);
                    case "DOWNLOAD_VOICE" -> handleDownloadVoice(parts, s);
                    default -> out.println("LỖI: Lệnh không hợp lệ");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                out.println("LỖI: Lỗi cơ sở dữ liệu");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void handleRegister(String[] parts, Socket s, Connection conn, PrintWriter out) {
        if (parts.length < 5) {
            out.println("LỖI: Thiếu tham số");
            return;
        }

        try {
            String username = parts[1];
            String password = parts[2];
            int port = Integer.parseInt(parts[3]);
            int serverPort = Integer.parseInt(parts[4]);
            String host = s.getInetAddress().getHostAddress();

            String hashedPassword = hashPassword(password);

            String sql = "INSERT INTO users (username, password, host, port, server_port) " +
                         "VALUES (?, ?, ?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE password=?, host=?, port=?, server_port=?, last_seen=CURRENT_TIMESTAMP";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, hashedPassword);
                ps.setString(3, host);
                ps.setInt(4, port);
                ps.setInt(5, serverPort);
                ps.setString(6, hashedPassword);
                ps.setString(7, host);
                ps.setInt(8, port);
                ps.setInt(9, serverPort);
                ps.executeUpdate();
            }

            out.println("OK");
            System.out.println("Registered " + username + " -> " + host + ":" + port);

        } catch (Exception e) {
            e.printStackTrace();
            out.println("LỖI: Đăng ký thất bại");
        }
    }

    public static void handleLookup(String[] parts, Connection conn, PrintWriter out) throws SQLException {
        if (parts.length < 2) {
            out.println("LỖI: Thiếu tham số");
            return;
        }
        String username = parts[1];
        String sql = "SELECT host, port FROM users WHERE username=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    out.println("TÌM THẤY " + rs.getString("host") + " " + rs.getInt("port"));
                } else {
                    out.println("KHÔNG TÌM THẤY");
                }
            }
        }
    }

    public static void handleUnregister(String[] parts, Connection conn, PrintWriter out) throws SQLException {
        if (parts.length < 2) {
            out.println("LỖI: Thiếu tham số");
            return;
        }
        String username = parts[1];
        String sql = "DELETE FROM users WHERE username=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        }
        out.println("OK");
    }

    public static void handleLogin(String[] parts, Connection conn, PrintWriter out) {
        if (parts.length < 3) {
            out.println("ĐĂNG NHẬP THẤT BẠI"); 
            return;
        }

        try {
            String username = parts[1];
            String password = parts[2];
            String hashedPassword = hashPassword(password);

            String sql = "SELECT * FROM users WHERE username=? AND password=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, hashedPassword);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String updateSql = "UPDATE users SET last_seen=CURRENT_TIMESTAMP WHERE username=?";
                        try (PreparedStatement ps2 = conn.prepareStatement(updateSql)) {
                            ps2.setString(1, username);
                            ps2.executeUpdate();
                        }
                        out.println("ĐĂNG NHẬP THÀNH CÔNG");
                        System.out.println("Người dùng đăng nhập: " + username);
                    } else {
                        out.println("ĐĂNG NHẬP THẤT BẠI");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.println("LỖI: Đăng nhập thất bại");
        }
    }

    public static String hashPassword(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(password.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
    
    public static void handleUser(String[] parts, Connection conn, PrintWriter out) {
        if (parts.length < 3) { 
            out.println("LỖI: Thiếu tham số");
            return;
        }

        try {
            int serverPort = Integer.parseInt(parts[1]);
            String currentUser = parts[2]; 
            String sql = "SELECT username FROM users WHERE server_port=? AND username<>?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, serverPort);
                ps.setString(2, currentUser);
                try (ResultSet rs = ps.executeQuery()) {
                    StringBuilder userList = new StringBuilder();
                    while (rs.next()) {
                        if (userList.length() > 0) userList.append(",");
                        userList.append(rs.getString("username"));
                    }
                    out.println("DANH SÁCH NGƯỜI DÙNG " + userList.toString());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            out.println("LỖI: Lấy danh sách người dùng thất bại");
        }
    }
    
    public static void handleSend(String[] parts, Connection conn, PrintWriter out) {
        if (parts.length < 4) { 
            out.println("LỖI: Thiếu tham số");
            return;
        }
        try {
            String nguoiGui = parts[1];
            String nguoiNhan = parts[2];
            String noiDung = String.join(" ", Arrays.copyOfRange(parts, 3, parts.length));

            int idGui = getUserId(conn, nguoiGui);
            int idNhan = getUserId(conn, nguoiNhan);
            if (idGui == -1 || idNhan == -1) {
                out.println("LỖI: Người dùng không tồn tại");
                return;
            }

            String sql = "INSERT INTO tin_nhan (nguoi_gui_id, nguoi_nhan_id, noi_dung) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idGui);
                ps.setInt(2, idNhan);
                ps.setString(3, noiDung);
                ps.executeUpdate();
            }
            out.println("OK");
            System.out.println("Tin nhắn: " + nguoiGui + " -> " + nguoiNhan + ": " + noiDung);

        } catch (Exception e) {
            e.printStackTrace();
            out.println("LỖI: Gửi tin nhắn thất bại");
        }
    }

    private static int getUserId(Connection conn, String username) throws SQLException {
        String sql = "SELECT id FROM users WHERE username=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        return -1;
    }
    
    public static void handleSendFile(String[] parts, Connection conn, PrintWriter out) {
        try {
        	
            String line = String.join(" ", parts);
            Pattern p = Pattern.compile("SEND_FILE\\s+(\\S+)\\s+(\\S+)\\s+\"([^\"]+)\"\\s+\"([^\"]+)\"\\s+\"([^\"]+)\"");
            Matcher m = p.matcher(line);
            if (!m.find()) {
                out.println("LỖI: Phân tích lệnh SEND_FILE thất bại");
                return;
            }

            String sender = m.group(1);
            String receiver = m.group(2);
            String fileName = m.group(3);
            String format = m.group(4);      
            String base64Data = m.group(5);

            int idSender = getUserId(conn, sender);
            int idReceiver = getUserId(conn, receiver);
            if (idSender == -1 || idReceiver == -1) {
                out.println("LỖI: Người gửi hoặc người nhận không tồn tại");
                return;
            }


            byte[] fileBytes = Base64.getDecoder().decode(base64Data);
            File filesDir = new File("Files");
            if (!filesDir.exists()) filesDir.mkdirs();
            String serverFileName = sender + "_" + fileName;
            String serverFilePath = new File(filesDir, serverFileName).getAbsolutePath();

            try (FileOutputStream fos = new FileOutputStream(serverFilePath)) {
                fos.write(fileBytes);
            }

            String sql = "INSERT INTO tep_tin (nguoi_gui_id, nguoi_nhan_id, ten_file, duong_dan) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idSender);
                ps.setInt(2, idReceiver);
                ps.setString(3, serverFileName);
                ps.setString(4, serverFilePath);
                ps.executeUpdate();
            }

            out.println("OK");
            System.out.println("File sent: " + sender + " -> " + receiver + ": " + fileName);

        } catch (Exception e) {
            e.printStackTrace();
            out.println("LỖI: Gửi file thất bại");
        }
    }

    
    public static void handleDownloadFile(String[] parts, Socket s) {
        try (OutputStream outStream = s.getOutputStream();
             PrintWriter out = new PrintWriter(outStream, true)) {

            if (parts.length < 2) {
                out.println("LỖI: Thiếu tham số");
                return;
            }

            String fileName = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));

            File filesDir = new File("Files");
            File targetFile = Arrays.stream(filesDir.listFiles())
                                    .filter(f -> f.getName().equals(fileName) || f.getName().endsWith("_" + fileName))
                                    .findFirst()
                                    .orElse(null);

            if (targetFile == null || !targetFile.exists()) {
                out.println("LỖI: File không tồn tại");
                return;
            }

            out.println("START " + targetFile.length());

            try (FileInputStream fis = new FileInputStream(targetFile)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = fis.read(buffer)) > 0) {
                    outStream.write(buffer, 0, read);
                }
            }

            outStream.flush();
            System.out.println("Đã gửi file: " + targetFile.getName());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    
    public static void handleVoiceMessenger(String[] parts, Connection conn, PrintWriter out) {
        try {

            String line = String.join(" ", parts);
            Pattern p = Pattern.compile("VOICE_MESSENGER\\s+(\\S+)\\s+(\\S+)\\s+\"([^\"]+)\"\\s+\"([^\"]+)\"\\s+\"([^\"]+)\"");
            Matcher m = p.matcher(line);
            if (!m.find()) { out.println("ERROR Parsing VOICE_MESSENGER"); return; }

            String sender = m.group(1);
            String receiver = m.group(2);
            String fileName = m.group(3);
            String format = m.group(4);
            String base64Data = m.group(5);

            int idSender = getUserId(conn, sender);
            int idReceiver = getUserId(conn, receiver);
            if (idSender == -1 || idReceiver == -1) { out.println("ERROR User not found"); return; }

            byte[] voiceBytes = Base64.getDecoder().decode(base64Data);
            File ghiAmDir = new File("GhiAm");
            if (!ghiAmDir.exists()) ghiAmDir.mkdirs();
            String serverFilePath = new File(ghiAmDir, sender + "_" + System.currentTimeMillis() + "_" + fileName).getAbsolutePath();
            try (FileOutputStream fos = new FileOutputStream(serverFilePath)) {
                fos.write(voiceBytes);
            }

            String sql = "INSERT INTO voice_messenger (nguoi_gui_id, nguoi_nhan_id, ten_file, duong_dan, format) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idSender);
                ps.setInt(2, idReceiver);
                ps.setString(3, fileName);
                ps.setString(4, serverFilePath); 
                ps.setString(5, format);
                ps.executeUpdate();
            }

            out.println("OK");
            System.out.println("Voice message: " + sender + " -> " + receiver + ": " + fileName);

        } catch (Exception e) {
            e.printStackTrace();
            out.println("LỖI: Gửi tin nhắn giọng nói thất bại");
        }
    }
    
    public static void handleDownloadVoice(String[] parts, Socket s) {
        try (OutputStream outStream = s.getOutputStream();
             PrintWriter out = new PrintWriter(outStream, true)) {

            if (parts.length < 2) {
                out.println("LỖI: Thiếu tham số");
                return;
            }

            String fileName = parts[1];
            File voiceDir = new File("GhiAm");
            File voiceFile = Arrays.stream(voiceDir.listFiles())
                                   .filter(f -> f.getName().endsWith(fileName))
                                   .findFirst()
                                   .orElse(null);

            if (voiceFile == null || !voiceFile.exists()) {
                out.println("LỖI: File giọng nói không tồn tại");
                return;
            }

            out.println("START " + voiceFile.length());

            try (FileInputStream fis = new FileInputStream(voiceFile)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = fis.read(buffer)) > 0) {
                    outStream.write(buffer, 0, read);
                }
            }
            outStream.flush();
            System.out.println("Đã gửi file giọng nói: " + voiceFile.getName());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void handleGetChat(String[] parts, Connection conn, PrintWriter out) {
        if (parts.length < 3) {
            out.println("LỖI: Thiếu tham số");
            return;
        }

        try {
            String user1 = parts[1];
            String user2 = parts[2];

            int id1 = getUserId(conn, user1);
            int id2 = getUserId(conn, user2);
            if (id1 == -1 || id2 == -1) {
                out.println("LỖI: Người dùng không tồn tại");
                return;
            }

            String sql = """
                SELECT 'MSG' AS type, u1.username AS sender, t.noi_dung AS content, t.thoi_gian AS time
                FROM tin_nhan t
                JOIN users u1 ON t.nguoi_gui_id = u1.id
                WHERE (t.nguoi_gui_id=? AND t.nguoi_nhan_id=?) OR (t.nguoi_gui_id=? AND t.nguoi_nhan_id=?)
                
                UNION ALL
                SELECT 'FILE' AS type, u1.username AS sender, t.ten_file AS content, t.thoi_gian AS time
                FROM tep_tin t
                JOIN users u1 ON t.nguoi_gui_id = u1.id
                WHERE (t.nguoi_gui_id=? AND t.nguoi_nhan_id=?) OR (t.nguoi_gui_id=? AND t.nguoi_nhan_id=?)
                
                UNION ALL
                SELECT 'VOICE' AS type, u1.username AS sender, v.ten_file AS content, v.thoi_gian AS time
                FROM voice_messenger v
                JOIN users u1 ON v.nguoi_gui_id = u1.id
                WHERE (v.nguoi_gui_id=? AND v.nguoi_nhan_id=?) OR (v.nguoi_gui_id=? AND v.nguoi_nhan_id=?)
                
                ORDER BY time ASC
            """;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id1); ps.setInt(2, id2);
                ps.setInt(3, id2); ps.setInt(4, id1);

                ps.setInt(5, id1); ps.setInt(6, id2);
                ps.setInt(7, id2); ps.setInt(8, id1);

                ps.setInt(9, id1); ps.setInt(10, id2);
                ps.setInt(11, id2); ps.setInt(12, id1);

                try (ResultSet rs = ps.executeQuery()) {
                    StringBuilder sb = new StringBuilder("CHAT ");
                    while (rs.next()) {
                        sb.append("[")
                          .append(rs.getTimestamp("time")).append(" ")
                          .append(rs.getString("sender")).append("] ")
                          .append(rs.getString("type")).append(":")
                          .append(rs.getString("content")).append("|");
                    }
                    out.println(sb.toString());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.println("LỖI: Lấy lịch sử chat thất bại");
        }
    }


}
