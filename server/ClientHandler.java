package server;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

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
                writer.println("Exécution de : " + message);

                executeCommand(message , writer);
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

    private void executeCommand(String command , PrintWriter writer){
    try {
            ProcessBuilder pb = new ProcessBuilder(command.split(" ")); // preparer la commande 

        pb.redirectErrorStream(true);
        
        Process process = pb.start();

        BufferedReader processReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        
        String line;
        while ((line = processReader.readLine()) != null) {
            writer.println(line); // On envoie chaque ligne au client via le socket
        }
        process.waitFor();
    } catch (Exception e) {
       writer.println("Erreur lors de l'exécution : " + e.getMessage());
    }
    }


}
