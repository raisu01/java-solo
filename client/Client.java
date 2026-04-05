package client;
import java.io.*;
import java.net.*;
import java.util.Scanner;
import common.*;
public class Client {
    public static void main(String[] args) {
        String hostname = "localhost"; // Adresse du serveur
        try (Socket socket = new Socket(hostname,Protocol.DEFAULT_PORT)){

            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            
                Scanner scanner = new Scanner(System.in);
                String userMessage;

                while (true) {
                    System.out.print("Tape ton message : ");
                    userMessage = scanner.nextLine();
                    
                    writer.println(userMessage); // Envoi au serveur
                    
                    if ("EXIT".equalsIgnoreCase(userMessage)) break;

                    String response = reader.readLine(); // Attente de la réponse
                    System.out.println("Serveur a répondu : " + response);
                }
                scanner.close();

            
        } catch (UnknownHostException e) {
            System.err.println("Serveur introuvable : " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Erreur I/O : " + e.getMessage());
        }
    }
}
