package src.main.java.server;

import java.io.*;
import java.net.*;
import src.main.java.common.Protocol;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Server.ServerListener listener;
    private final String clientId;
    private final String clientIp;

    public ClientHandler(Socket socket, Server.ServerListener listener) {
        this.socket   = socket;
        this.listener = listener;
        this.clientIp = socket.getInetAddress().getHostAddress();
        this.clientId = clientIp + ":" + socket.getPort();
    }

    @Override
    public void run() {
        try (
            InputStream  input  = socket.getInputStream();
            OutputStream output = socket.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input,  Protocol.ENCODING));
            PrintWriter   writer = new PrintWriter(new OutputStreamWriter(output, Protocol.ENCODING), true);
        ) {
            notify_connected();

            String message;
            while ((message = reader.readLine()) != null) {
                if ("EXIT".equalsIgnoreCase(message)) break;
                notify_command(message);
                executeCommand(message, writer, reader);
            }

        } catch (SocketTimeoutException e) {
            notify_log("Client " + clientId + " inactif — déconnexion forcée.");
        } catch (IOException e) {
            notify_log("Erreur client " + clientId + " : " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException e) { /* ignore */ }
            notify_disconnected();
        }
    }

    // -------------------------------------------------------------------------
    //  Exécution de commande
    // -------------------------------------------------------------------------
    private void executeCommand(String command, PrintWriter clientWriter, BufferedReader clientReader) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb = os.contains("win")
                    ? new ProcessBuilder("cmd.exe", "/c", command)
                    : new ProcessBuilder("/bin/sh", "-c", command);

            pb.redirectErrorStream(true);
            Process process = pb.start();

            OutputStreamWriter processIn = new OutputStreamWriter(process.getOutputStream(), Protocol.ENCODING);

            // Lecture de la sortie du processus → envoi au client
            Thread outputThread = new Thread(() -> {
                try (InputStreamReader processOut = new InputStreamReader(process.getInputStream(), Protocol.ENCODING)) {
                    int c;
                    while ((c = processOut.read()) != -1) {
                        clientWriter.print((char) c);
                        clientWriter.flush();
                    }
                } catch (IOException e) { /* ignore */ }
            });
            outputThread.start();

            // Entrée interactive client → processus
            while (process.isAlive()) {
                if (clientReader.ready()) {
                    String input = clientReader.readLine();
                    if (input != null) {
                        processIn.write(input + "\n");
                        processIn.flush();
                    }
                }
                Thread.sleep(50);
            }

            process.waitFor();
            outputThread.join(1000);
            clientWriter.println("\n--- FIN DE COMMANDE ---");

        } catch (Exception e) {
            clientWriter.println("Erreur : " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    //  Notifications listener
    // -------------------------------------------------------------------------
    private void notify_connected() {
        if (listener != null) {
            listener.onClientConnected(clientId, clientIp);
            listener.onLog("Client connecté : " + clientId);
        }
    }

    private void notify_disconnected() {
        if (listener != null) {
            listener.onClientDisconnected(clientId);
            listener.onLog("Client déconnecté : " + clientId);
        }
    }

    private void notify_command(String command) {
        if (listener != null) {
            listener.onCommandReceived(clientId, command);
            listener.onLog("[CMD] " + clientId + " → " + command);
        }
    }

    private void notify_log(String message) {
        if (listener != null) listener.onLog(message);
    }
}
