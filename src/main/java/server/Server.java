package src.main.java.server;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.*;
import src.main.java.common.Protocol;

public class Server {

    // -------------------------------------------------------------------------
    //  Interface de callbacks vers l'UI
    // -------------------------------------------------------------------------
    public interface ServerListener {
        void onClientConnected(String clientId, String ip);
        void onClientDisconnected(String clientId);
        void onCommandReceived(String clientId, String command);
        void onLog(String message);
    }

    // -------------------------------------------------------------------------
    //  État interne
    // -------------------------------------------------------------------------
    private ServerSocket serverSocket;
    private ExecutorService pool;
    private volatile boolean running = false;
    private ServerListener listener;
    private final List<ClientHandler> activeHandlers = new CopyOnWriteArrayList<>();

    public void setListener(ServerListener listener) {
        this.listener = listener;
    }

    // -------------------------------------------------------------------------
    //  Démarrage
    // -------------------------------------------------------------------------
    public void start() throws IOException {
        serverSocket = new ServerSocket(Protocol.DEFAULT_PORT);
        pool = Executors.newFixedThreadPool(Protocol.MAX_CLIENTS);
        running = true;

        log("Serveur démarré sur le port " + Protocol.DEFAULT_PORT);

        Thread acceptThread = new Thread(() -> {
            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    socket.setSoTimeout(Protocol.CONNECTION_TIMEOUT_MS);
                    ClientHandler handler = new ClientHandler(socket, listener);
                    activeHandlers.add(handler);
                    pool.execute(() -> {
                        handler.run();
                        activeHandlers.remove(handler);
                    });
                } catch (IOException e) {
                    if (running) log("Erreur accept : " + e.getMessage());
                }
            }
        });
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    // -------------------------------------------------------------------------
    //  Arrêt
    // -------------------------------------------------------------------------
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException e) { /* ignore */ }
        if (pool != null) pool.shutdownNow();
        activeHandlers.clear();
        log("Serveur arrêté.");
    }

    // -------------------------------------------------------------------------
    //  Utilitaire
    // -------------------------------------------------------------------------
    private void log(String message) {
        if (listener != null) listener.onLog(message);
        else System.out.println("[Server] " + message);
    }

    // -------------------------------------------------------------------------
    //  Point d'entrée → lance l'UI serveur
    // -------------------------------------------------------------------------
    public static void main(String[] args) {
        javafx.application.Application.launch(gui.JavaFxServer.class, args);
    }
}
