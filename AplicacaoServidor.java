import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class AplicacaoServidor extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final int udpPort = 8887;
    private static final int tcpPort = 8888;
    private JTextArea textArea;
    private List<PrintWriter> clientsTCP;
    private List<InetSocketAddress> clientsUDP;
    private DatagramSocket datagramSocket;
    private byte[] buffer;

    public AplicacaoServidor() {
        setTitle("Chat e Jogo Gartic - Servidor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 200);
        setLocationRelativeTo(null);

        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane);

        setVisible(true);

        clientsTCP = new ArrayList<>();
        clientsUDP = new ArrayList<>();

        try {
            ServerSocket serverSocket = new ServerSocket(tcpPort);
            datagramSocket = new DatagramSocket(udpPort);
            buffer = new byte[1024];

            textArea.append("Servidor iniciado...\n");

            new Thread(() -> listenForUDPPackets()).start();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                clientsTCP.add(out);
                new Thread(new ClientHandler(clientSocket)).start();
                textArea.append("Cliente TCP conectado: " + clientSocket.getInetAddress().getHostAddress() + ":"
                        + clientSocket.getPort() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcastChat(String message, String address, int port) {
        for (PrintWriter client : clientsTCP) {
            client.println(address + ":" + port + ": " + message);
        }
    }

    private void sendDrawingToAllClients(String drawingData, String senderAddress, int senderPort) {
        String message = drawingData;

        for (InetSocketAddress client : clientsUDP) {
            if (!(client.getAddress().getHostAddress().equals(senderAddress) && client.getPort() == senderPort)) {
                try {
                    byte[] buffer = message.getBytes();
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, client.getAddress(),
                            client.getPort());
                    datagramSocket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
}
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;

        public ClientHandler(Socket clientSocket) throws IOException {
            this.clientSocket = clientSocket;
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
        }

        @Override
        public void run() {
            try {
                String clientAddress = clientSocket.getInetAddress().getHostAddress();
                int clientPort = clientSocket.getPort();
                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                    broadcastChat(clientMessage, clientAddress, clientPort);
                    textArea.append(clientAddress + ":" + clientPort + ": " + clientMessage + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    in.close();
                    out.close();
                    clientSocket.close();
                    textArea.append("Cliente desconectado: " + clientSocket.getInetAddress().getHostAddress() + ":"
                            + clientSocket.getPort() + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void listenForUDPPackets() {
        try {
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                String senderAddress = packet.getAddress().getHostAddress();
                int senderPort = packet.getPort();
                InetSocketAddress sender = new InetSocketAddress(senderAddress, senderPort);

                if (!clientsUDP.contains(sender)) {
                    clientsUDP.add(sender);
                }

                sendDrawingToAllClients(message, senderAddress, senderPort);

                textArea.append(senderAddress + ":" + senderPort + ": " + message + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        AplicacaoServidor servidor = new AplicacaoServidor();
    }
}
