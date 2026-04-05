package server;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

import common.Protocol;

public class ClientHandler implements Runnable {
    private Socket socket;
    public ClientHandler(Socket socket){
        this.socket = socket;
    }
     @Override
    public void run() {
        try(
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input)); // canal client->serveur
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);// canal server->client
        ) {
            String message ;
                System.out.println("client [" + Thread.currentThread().getId() + "] connecter " );

            while ((message= reader.readLine()) != null) {
                if ("EXIT".equalsIgnoreCase(message)) break;

                executeCommand(message , writer, reader);
            }
            
        }catch (SocketTimeoutException e) {
        System.err.println("Client trop lent, déconnexion forcée."); }

        catch (IOException e) {
            System.out.println("Erreur avec le client : " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException e) { e.printStackTrace(); }
            System.out.println("Connexion client [" + Thread.currentThread().getId() + "] fermée.");
        }    
    }

    private void executeCommand(String command, PrintWriter clientWriter, BufferedReader clientReader) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb = os.contains("win") 
                ? new ProcessBuilder("cmd.exe", "/c", command)
                : new ProcessBuilder("/bin/sh", "-c", command);

            pb.redirectErrorStream(true);
            Process process = pb.start();

            OutputStreamWriter processIn = new OutputStreamWriter(process.getOutputStream(), Protocol.ENCODING);

            // --- THREAD DE LECTURE (SORTIE DU PROCESSUS -> CLIENT) ---
            Thread outputThread = new Thread(() -> {
                try (InputStreamReader processOut = new InputStreamReader(process.getInputStream(), Protocol.ENCODING)) {
                    int c;
                    // On lit caractère par caractère pour ne pas rater les "prompts" sans \n
                    while ((c = processOut.read()) != -1) {
                        clientWriter.print((char) c);
                        clientWriter.flush(); // Crucial pour l'interactivité !
                    }
                } catch (IOException e) {
                }
            });
            outputThread.start();

            // --- BOUCLE PRINCIPALE (CLIENT -> ENTRÉE DU PROCESSUS) ---
            while (process.isAlive()) {
                if (clientReader.ready()) { // On vérifie si le client a tapé quelque chose
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
}
