package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import common.*;

public class Client {
    public static void main(String[] args) {


            System.out.println("--- Connecté au serveur ---");
            System.out.print("--- Donne l'adresse ip du server :");
            Scanner scanner = new Scanner(System.in);
        String hostname = scanner.nextLine();

        try (Socket socket = new Socket(hostname, Protocol.DEFAULT_PORT)) {
            // Flux pour envoyer (Clavier -> Serveur)
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), Protocol.ENCODING), true);
            
            // Flux pour recevoir (Serveur -> Ecran)
            // On utilise InputStreamReader directement pour lire CARACTÈRE par CARACTÈRE
            InputStreamReader reader = new InputStreamReader(socket.getInputStream(), Protocol.ENCODING);

            System.out.println("--- Connecté au serveur ---");

            // THREAD DE LECTURE : Il tourne en arrière-plan
            Thread receiverThread = new Thread(() -> {
                try {
                    int c;
                    // On lit chaque caractère dès qu'il arrive
                    while ((c = reader.read()) != -1) {
                        System.out.print((char) c);
                        System.out.flush(); // On force l'affichage immédiat
                    }
                } catch (IOException e) {
                    System.out.println("\n[Info] Flux de lecture fermé.");
                }
            });
            receiverThread.setDaemon(true); 
            receiverThread.start();

            // BOUCLE PRINCIPALE : Lecture clavier
             scanner = new Scanner(System.in);
            while (true) {

                if (scanner.hasNextLine()) {
                    String userMessage = scanner.nextLine();
                    writer.println(userMessage);

                    if ("EXIT".equalsIgnoreCase(userMessage)) {
                        break;
                    }
                }
            }
            
            System.out.println("Déconnexion...");

        } catch (IOException e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }
}