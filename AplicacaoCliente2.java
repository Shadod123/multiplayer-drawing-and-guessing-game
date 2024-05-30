import java.awt.Color;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class AplicacaoCliente2 extends JFrame {
    private static final long serialVersionUID = 1L;
    private JTextField textField;
    private JTextArea textArea;
    private JPanel drawingPanel;
    private Graphics graphics;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private DatagramSocket datagramSocket;

    public AplicacaoCliente2() {
        setTitle("Chat e Jogo Gartic - Cliente");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        textField = new JTextField();
        textField.addActionListener(e -> {
            sendMessage(textField.getText());
            textField.setText("");
        });
        add(textField, "South");

        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(250, 800));
        add(scrollPane, "East");

        drawingPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                graphics = g;
            }
        };
        drawingPanel.setBackground(Color.WHITE);
        drawingPanel.addMouseMotionListener(new MouseAdapter() {
            private int lastX;
            private int lastY;

            @Override
            public void mousePressed(MouseEvent e) {
                lastX = e.getX();
                lastY = e.getY();
                mouseDragged(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (graphics != null) {
                    int currentX = e.getX();
                    int currentY = e.getY();
                    graphics.setColor(Color.BLACK);
                    graphics.drawLine(lastX, lastY, currentX, currentY);
                    lastX = currentX;
                    lastY = currentY;
                    String message = "[DRAW]" + currentX + "," + currentY;
                    processDrawing(message);
                    sendDrawing(message);
                    showMessage(message);
                }
            }
        });
        JScrollPane drawingScrollPane = new JScrollPane(drawingPanel);
        add(drawingScrollPane, "Center");

        setVisible(true);

        try {
            socket = new Socket("192.168.0.2", 8888);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            datagramSocket = new DatagramSocket(9872);
            new Thread(new DrawingReceiver()).start();
            new Thread(new MessageReceiver()).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showMessage(String message) {
        textArea.append(message + "\n");
    }

    private void sendMessage(String message) {
        out.println(message);
    }

    private void sendDrawing(String message) {
        try {
            InetAddress address = InetAddress.getByName("192.168.0.2");
            byte[] buffer = message.getBytes();

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 8887);
            datagramSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processDrawing(String message) {
        String[] parts = message.substring(6).split(",");
        int x = Integer.parseInt(parts[0]);
        int y = Integer.parseInt(parts[1]);

        if (graphics != null) {
            graphics.setColor(Color.BLACK);
            graphics.fillOval(x, y, 5, 5);
            drawingPanel.repaint();
        }
    }

    private class DrawingReceiver implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    datagramSocket.receive(packet);
                    String message = new String(packet.getData());

                    if (message.startsWith("[DRAW]")) {
                       // processDrawing(message);
                        showMessage(message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                datagramSocket.close();
            }
        }
    }

    private class MessageReceiver implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    String message = in.readLine();
                    showMessage(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new AplicacaoCliente2();
    }
}
