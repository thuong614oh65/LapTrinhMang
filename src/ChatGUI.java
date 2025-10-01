import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatGUI extends JFrame {
    private JList<String> friendsList;
    private JTextField inputField;
    private JButton btnSend, btnFile, btnIcon, btnVoice;
    private String username;

    private JPanel chatPanel;       
    private JScrollPane chatScroll; 
    private Map<String, JPanel> chatMap = new HashMap<>(); 
    private String currentFriend;   

    private String serverHost;
    private int serverPort;
    
    private TargetDataLine microphone;
    private boolean isRecording = false;
    private File tempVoiceFile;


    public ChatGUI(String serverHost, int serverPort, String username, List<String> userList) {
        this.username = username;
        this.serverHost = serverHost;
        this.serverPort = serverPort;

        setTitle("Chat - " + username);
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String user : userList) {
            listModel.addElement(user);

            JPanel chatBox = new JPanel();
            chatBox.setLayout(new BoxLayout(chatBox, BoxLayout.Y_AXIS));
            chatMap.put(user, chatBox);
        }
        friendsList = new JList<>(listModel);
        friendsList.setFixedCellHeight(60);
        JScrollPane friendsScroll = new JScrollPane(friendsList);
        friendsScroll.setPreferredSize(new Dimension(200, 0));
        add(friendsScroll, BorderLayout.WEST);

        chatPanel = new JPanel(new BorderLayout());
        if (!userList.isEmpty()) {
            currentFriend = userList.get(0); 
            chatScroll = new JScrollPane(chatMap.get(currentFriend));
            chatPanel.add(chatScroll, BorderLayout.CENTER);
        }

        JPanel inputPanel = new JPanel(new FlowLayout());
        inputField = new JTextField(30);
        btnSend = new JButton("Gửi");
        btnFile = new JButton("File");
        btnIcon = new JButton("Icon");
        btnVoice = new JButton("Ghi âm");
        inputPanel.add(inputField);
        inputPanel.add(btnSend);
        inputPanel.add(btnFile);
        inputPanel.add(btnIcon);
        inputPanel.add(btnVoice);

        chatPanel.add(inputPanel, BorderLayout.SOUTH);
        add(chatPanel, BorderLayout.CENTER);

        friendsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                currentFriend = friendsList.getSelectedValue();
                JPanel chatBox = chatMap.get(currentFriend);

                chatBox.removeAll();

                TinNhanClient client = new TinNhanClient(serverHost, serverPort);
                String chat = client.getChat(username, currentFriend);

                if (chat != null && chat.startsWith("CHAT")) {
                    String[] items = chat.substring(5).split("\\|");
                    for (String item : items) {
                        if (item.isEmpty()) continue;

                        int endMeta = item.indexOf("]");
                        if (endMeta != -1) {
                            String meta = item.substring(1, endMeta);
                            String[] metaParts = meta.split(" ");
                            String time = metaParts[0] + " " + metaParts[1];
                            String sender = metaParts[metaParts.length - 1];

                            String rest = item.substring(endMeta + 1).trim();
                            if (rest.startsWith("MSG:")) {
                                addMessage(chatBox, sender, rest.substring(4), time);
                            } else if (rest.startsWith("FILE:")) {
                                String fileName = rest.substring(5);
                                String type = fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")
                                              ? "image" : "file";
                                themTinNhanTapTin(chatBox, sender, fileName, type, time);
                            }
                        }
                    }
                }

                chatPanel.remove(chatScroll);
                chatScroll = new JScrollPane(chatBox);
                chatPanel.add(chatScroll, BorderLayout.CENTER);
                chatPanel.revalidate();
                chatPanel.repaint();
            }
        });

        btnSend.addActionListener(e -> {
            String msg = inputField.getText().trim();
            if (!msg.isEmpty() && currentFriend != null) {
                TinNhanClient client = new TinNhanClient(serverHost, serverPort);
                client.sendMessage(username, currentFriend, msg);

                JPanel chatBox = chatMap.get(currentFriend);

                String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                addMessage(chatBox, username, msg, now);

                inputField.setText("");
                chatBox.revalidate();
                chatBox.repaint();
            }
        });
        
        btnFile.addActionListener(e -> {
            if (currentFriend == null) return;

            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                String fileName = file.getName();
                String type = fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")
                              ? "image" : "file";

                File uploadsDir = new File("Files");
                if (!uploadsDir.exists()) uploadsDir.mkdirs();

                Path source = file.toPath();
                Path target = Paths.get(uploadsDir.getAbsolutePath(), username + "_" + fileName);
                try {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Lưu file thất bại!");
                    return;
                }

                TinNhanClient client = new TinNhanClient(serverHost, serverPort);
                client.sendFileMessage(username, currentFriend, target);

                JPanel chatBox = chatMap.get(currentFriend);
                String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                themTinNhanTapTin(chatBox, username, fileName, type, now);

                chatBox.revalidate();
                chatBox.repaint();
            }
        });
        
        btnVoice.addActionListener(e -> {
            if (!isRecording) {
                ghiAm();
            } else {
                dungGhiAm();
            }
        });
        
        btnIcon.addActionListener(e -> {
            JDialog dialog = new JDialog(ChatGUI.this, "Chọn Icon", true);
           
            JPanel iconPanel = new JPanel();
            iconPanel.setLayout(new GridLayout(0, 3, 3, 3)); 
            
            String[] icons = {
                "😀","😃","😄","😁","😆","😅","😂","🤣","😊","😇",
                "🙂","🙃","😉","😌","😍","🥰","😘","😗","😙","😚",
                "😋","😛","😝","😜","🤪","🤨","🧐","🤓","😎","🥳",
                "😏","😒","😞","😔","😟","😕","🙁","☹️","😣","😖",
                "😫","😩","🥺","😢","😭","😤","😠","😡","🤬","🤯",
                "😳","🥵","🥶","😱","😨","😰","😥","😓","🤗","🤔",
                "🤭","🤫","🤥","😶","😐","😑","😬","🙄","😯","😦",
                "😧","😮","😲","🥱","😴","🤤","😪","😵","🤐","🥴",
                "🤢","🤮","🤧","😷","🤒","🤕","🤑","🤠","😈","👿",
                "👹","👺","💀","👻","👽","👾","🤖","💩","🎃","😺",
                "😸","😹","😻","😼","😽","🙀","😿","😾","❤️","🧡",
                "💛","💚","💙","💜","🖤","🤍","🤎","💔","❣️","💕",
                "💞","💓","💗","💖","💘","💝","💟","👍","👎","👊",
                "✊","🤛","🤜","👏","🙌","👐","🤲","🙏","🤝","💪",
                "🖐️","✋","🤚","🖖","👋","🤙","💅","🤳","💃","🕺",
                "👯","🧖","🧘","🛀","🛌","🧗","🏇","🏂","🏄","🚣"
            };

            for (String ic : icons) {
                JButton btn = new JButton(ic);
                btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
                btn.addActionListener(ev -> {
                    if (currentFriend != null) {
                        TinNhanClient client = new TinNhanClient(serverHost, serverPort);
                        client.sendMessage(username, currentFriend, ic);

                        JPanel chatBox = chatMap.get(currentFriend);
                        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        addMessage(chatBox, username, ic, now);

                        chatBox.revalidate();
                        chatBox.repaint();
                    }
                    dialog.dispose();
                });
                iconPanel.add(btn);
            }

            JScrollPane scrollPane = new JScrollPane(iconPanel);
            scrollPane.setPreferredSize(new Dimension(400, 300));
            dialog.add(scrollPane);

            dialog.pack();
            dialog.setLocationRelativeTo(ChatGUI.this);
            dialog.setVisible(true);
        });


        new Thread(() -> {
            TinNhanClient client = new TinNhanClient(serverHost, serverPort);
            String lastChat = ""; 
            while (true) {
                try {
                    if (currentFriend != null) {
                        String chat = client.getChat(username, currentFriend);
                        if (chat != null && chat.startsWith("CHAT") && !chat.equals(lastChat)) {
                            String finalChat = chat;
                            SwingUtilities.invokeLater(() -> {
                                JPanel chatBox = chatMap.get(currentFriend);
                                chatBox.removeAll(); 
                                String[] items = finalChat.substring(5).split("\\|");
                                for (String item : items) {
                                    if (item.isEmpty()) continue;

                                    int endMeta = item.indexOf("]");
                                    if (endMeta != -1) {
                                        String meta = item.substring(1, endMeta);
                                        String[] metaParts = meta.split(" ");
                                        String time = metaParts[0] + " " + metaParts[1];
                                        String sender = metaParts[metaParts.length - 1];

                                        String rest = item.substring(endMeta + 1).trim();
                                        if (rest.startsWith("MSG:")) {
                                            addMessage(chatBox, sender, rest.substring(4), time);
                                        } else if (rest.startsWith("FILE:")) {
                                            String fileName = rest.substring(5);
                                            String type = fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")
                                                          ? "image" : "file";
                                            themTinNhanTapTin(chatBox, sender, fileName, type, time);
                                        } else if (rest.startsWith("VOICE:")) {
                                            String fileName = rest.substring(6); 
                                            themTinNhanTapTin(chatBox, sender, fileName, "voice", time);
                                        }

                                    }
                                }
                                chatBox.revalidate();
                                chatBox.repaint();
                            });
                            lastChat = chat;
                        }
                    }
                    Thread.sleep(1500);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();


        setVisible(true);
    }
    
    private void addMessage(JPanel chatBox, String sender, String text, String time) {
        JPanel bubblePanel = new JPanel(new BorderLayout());
        bubblePanel.setOpaque(false);

        JComponent msgComp;

        if (text.codePointCount(0, text.length()) == 1) {
            JLabel emojiLabel = new JLabel(text);
            emojiLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48)); 
            emojiLabel.setHorizontalAlignment(SwingConstants.CENTER);
            emojiLabel.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));
            msgComp = emojiLabel;
        } else {
            RoundLabel textLabel = new RoundLabel(
                "<html><p style='width:250px;'>" + text + "</p></html>",
                sender.equals(username) ? new Color(173, 216, 230) : new Color(220, 220, 220)
            );
            msgComp = textLabel;
        }

        JLabel timeLabel = new JLabel(time, SwingConstants.CENTER);
        timeLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        timeLabel.setForeground(Color.GRAY);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        contentPanel.add(msgComp);
        contentPanel.add(timeLabel);

        if (sender.equals(username)) {
            bubblePanel.add(contentPanel, BorderLayout.EAST);
        } else {
            bubblePanel.add(contentPanel, BorderLayout.WEST);
        }

        chatBox.add(bubblePanel);
        chatBox.add(Box.createVerticalStrut(5));

        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScroll.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }


    private void themTinNhanTapTin(JPanel chatBox, String sender, String fileName, String type, String time) {
        JPanel bubblePanel = new JPanel(new BorderLayout());
        bubblePanel.setOpaque(false);

        String displayText;
        if (type.equals("voice")) {
            displayText = "[Voice] " + fileName;
        } else if (type.equals("file")){
            displayText = "[File] " + fileName;
        } else {
        	displayText = "[Image] " + fileName;
        }

        RoundLabel msgLabel = new RoundLabel("<html><p style='width:250px;'>" + displayText + "</p></html>",
                sender.equals(username) ? new Color(173, 216, 230) : new Color(220, 220, 220));
        
        if (type.equals("voice")) {
            msgLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            msgLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    File voiceFile = new File("GhiAm/" + fileName);

                    if (!voiceFile.exists()) {
                        TinNhanClient client = new TinNhanClient(serverHost, serverPort);
                        client.downloadVoice(fileName);
                    }

                    if (voiceFile.exists()) {
                        try {
                            AudioInputStream audioIn = AudioSystem.getAudioInputStream(voiceFile);
                            Clip clip = AudioSystem.getClip();
                            clip.open(audioIn);
                            clip.start();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(ChatGUI.this, "Không thể phát file âm thanh!");
                        }
                    }
                }
            });
        } else if (type.equals("file")) {
            msgLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            msgLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    File normalFile = new File("Files/" + fileName);

                    if (!normalFile.exists()) {
            
                        TinNhanClient client = new TinNhanClient(serverHost, serverPort);
                        client.downloadFile(fileName);
                    }

                    if (normalFile.exists()) {
                       
                        try {
                            Desktop.getDesktop().open(normalFile);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(ChatGUI.this, "Không thể mở file này!");
                        }
                    }
                }
            });
        }else if (type.equals("image")) {
            msgLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            msgLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    File imageFile = new File("Files/" + fileName);
                    if (!imageFile.exists()) {
                        TinNhanClient client = new TinNhanClient(serverHost, serverPort);
                        client.downloadFile(fileName);
                    }
                    if (imageFile.exists()) {
                        try {
                            ImageIcon originalIcon = new ImageIcon(imageFile.getAbsolutePath());

                            int originalWidth = originalIcon.getIconWidth();
                            int originalHeight = originalIcon.getIconHeight();

                            int newWidth = 1500;
                            int newHeight = 730;

                            Image scaledImage = originalIcon.getImage().getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                            ImageIcon scaledIcon = new ImageIcon(scaledImage);

                            JLabel imgLabel = new JLabel(scaledIcon);
                            JOptionPane.showMessageDialog(ChatGUI.this, imgLabel, "Xem ảnh", JOptionPane.PLAIN_MESSAGE);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(ChatGUI.this, "Không thể mở ảnh này!");
                        }
                    }
                }
            });
        }


        JLabel timeLabel = new JLabel(time, SwingConstants.CENTER);
        timeLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        timeLabel.setForeground(Color.GRAY);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        contentPanel.add(msgLabel);
        contentPanel.add(timeLabel);

        if (sender.equals(username)) {
            bubblePanel.add(contentPanel, BorderLayout.EAST);
        } else {
            bubblePanel.add(contentPanel, BorderLayout.WEST);
        }

        chatBox.add(bubblePanel);
        chatBox.add(Box.createVerticalStrut(5));

        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScroll.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    private void ghiAm() {
        try {
            AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();

            File tempDir = new File("GhiAm");
            if (!tempDir.exists()) tempDir.mkdirs();

            String fileName = "voice_" + System.currentTimeMillis() + ".wav";
            tempVoiceFile = new File(tempDir, fileName);

            new Thread(() -> {
                try (AudioInputStream ais = new AudioInputStream(microphone)) {
                    AudioSystem.write(ais, AudioFileFormat.Type.WAVE, tempVoiceFile);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }).start();

            isRecording = true;
            btnVoice.setText("Dừng");

        } catch (LineUnavailableException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Không thể ghi âm!");
        }
    }

    private void dungGhiAm() {
        if (microphone != null && isRecording) {
            microphone.stop();
            microphone.close();
            isRecording = false;
            btnVoice.setText("Ghi âm");

            if (currentFriend != null && tempVoiceFile.exists()) {
                TinNhanClient client = new TinNhanClient(serverHost, serverPort);
                String response = client.sendVoiceMessage(username, currentFriend, tempVoiceFile.toPath());
                System.out.println("Server trả về: " + response);

                JPanel chatBox = chatMap.get(currentFriend);
                String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                themTinNhanTapTin(chatBox, username, tempVoiceFile.getName(), "voice", now);

                chatBox.revalidate();
                chatBox.repaint();
            }
        }
    }



}
