import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;

public class RegisterGUI extends JFrame {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JTextField txtServerHost;
    private JTextField txtServerPort;
    private JTextField txtListenPort;
    private JButton btnRegister;
    private JButton btnQuit;
    private JTextArea txtAreaLog;

    private ServerSocket srvSocket;

    public RegisterGUI() {
        super("Peer Register");

        setLayout(new BorderLayout());

        JPanel pnlForm = new JPanel(new GridLayout(5,2,5,5));
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

        pnlForm.add(new JLabel("Listen Port:"));
        txtListenPort = new JTextField("6000");
        pnlForm.add(txtListenPort);

        add(pnlForm, BorderLayout.NORTH);

        txtAreaLog = new JTextArea(10,30);
        txtAreaLog.setEditable(false);
        add(new JScrollPane(txtAreaLog), BorderLayout.CENTER);

        JPanel pnlButtons = new JPanel();
        btnRegister = new JButton("Register");
        btnQuit = new JButton("Quit");
        pnlButtons.add(btnRegister);
        pnlButtons.add(btnQuit);
        add(pnlButtons, BorderLayout.SOUTH);

        btnRegister.addActionListener(e -> doRegister());
        btnQuit.addActionListener(e -> {
            closeSocket();
            System.exit(0);
        });

        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void doRegister() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();
        String host = txtServerHost.getText().trim();
        int serverPort = Integer.parseInt(txtServerPort.getText().trim());
        int listenPort = Integer.parseInt(txtListenPort.getText().trim());

        if(username.isEmpty() || password.isEmpty()) {
            txtAreaLog.append("Username và password không được để trống.\n");
            return;
        }

        try {
            srvSocket = new ServerSocket(listenPort);
            txtAreaLog.append("Listening on port " + listenPort + "\n");
        } catch (IOException e) {
            txtAreaLog.append("Cannot open port " + listenPort + ": " + e.getMessage() + "\n");
            return;
        }


        RegisterClient client = new RegisterClient(host, serverPort);
        String response = client.register(username, password, listenPort, serverPort);
        txtAreaLog.append("Register response: " + response + "\n");
        
        if ("OK".equalsIgnoreCase(response)) {
            txtAreaLog.append("Đăng ký thành công! Chuyển sang đăng nhập...\n");
            SwingUtilities.invokeLater(() -> {
                new LogGUI();  
                this.dispose(); 
            });
        }
    }

    private void closeSocket() {
        if (srvSocket != null && !srvSocket.isClosed()) {
            try {
                srvSocket.close();
                txtAreaLog.append("Socket closed.\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(RegisterGUI::new);
    }
}
