package server;
import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import common.*;

/* 1 identifiant du server
   2 un ecouteur 
   3 un accept
   4 un canal de comunication 
*/


public class Server {
    public static void main(String[] args) {

        ExecutorService pool = Executors.newFixedThreadPool(Protocol.MAX_CLIENTS);

    try (ServerSocket serverSocket = new ServerSocket(Protocol.DEFAULT_PORT)){
        System.out.println("Serveur a demarer sur le port " + Protocol.DEFAULT_PORT + "...");
        while (true) {
                Socket socket = serverSocket.accept();
                socket.setSoTimeout(Protocol.CONNECTION_TIMEOUT_MS);
                pool.execute(new ClientHandler(socket));
              
        }
    } catch (Exception e) {
        System.err.println("Erreur serveur : " + e.getMessage());
        e.printStackTrace();
    }



}

    
}

