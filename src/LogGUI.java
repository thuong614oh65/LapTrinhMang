import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList; 
import java.util.Arrays; 
import java.util.List;

public class LogGUI extends JFrame {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JTextField txtServerHost;
    private JTextField txtServerPort;
    private JButton btnLogin;
    private JButton btnQuit;
    private JTextArea txtAreaLog;

    public LogGUI() {
        super("Peer Login");

        setLayout(new BorderLayout());

        JPanel pnlForm = new JPanel(new GridLayout(4,2,5,5));
        pnlForm.add(new JLabel("Username:"));
        txtUsername = new JTextField("user1");
        pnlForm.add(txtUsername);

        pnlForm.add(new JLabel("Password:"));
        txtPassword = new JPasswordField();
        pnlForm.add(txtPassword);

        pnlForm.add(new JLabel("Server Host:"));
        txtServerHost = new JTextField("localhost");
        pnlForm.add(txtServerHost);

        pnlForm.add(new JLabel("Server Port:"));
        txtServerPort = new JTextField("5000");
        pnlForm.add(txtServerPort);

        add(pnlForm, BorderLayout.NORTH);

        txtAreaLog = new JTextArea(10,30);
        txtAreaLog.setEditable(false);
        add(new JScrollPane(txtAreaLog), BorderLayout.CENTER);

        JPanel pnlButtons = new JPanel();
        btnLogin = new JButton("Login");
        btnQuit = new JButton("Quit");
        pnlButtons.add(btnLogin);
        pnlButtons.add(btnQuit);
        add(pnlButtons, BorderLayout.SOUTH);

        btnLogin.addActionListener(e -> doLogin());
        btnQuit.addActionListener(e -> System.exit(0));

        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void doLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();
        String host = txtServerHost.getText().trim();
        int serverPort = Integer.parseInt(txtServerPort.getText().trim());

        if (username.isEmpty() || password.isEmpty()) {
            txtAreaLog.append("Username và password không được để trống.\n");
            return;
        }

        LogClient client = new LogClient(host, serverPort);
        String response = client.login(username, password);
        txtAreaLog.append("Login response: " + response + "\n");

        if (response.equalsIgnoreCase("SUCCESS")) {
            txtAreaLog.append("Đăng nhập thành công! Chuyển sang chat...\n");
            
            UsersClient usersClient = new UsersClient(host, serverPort);
            List<String> userList = usersClient.getUsers(serverPort, username); 

            SwingUtilities.invokeLater(() -> {
                new ChatGUI(host, serverPort, username, userList); 
                this.dispose();
            });
        }
    }



    public static void main(String[] args) {
        SwingUtilities.invokeLater(LogGUI::new);
    }
}
