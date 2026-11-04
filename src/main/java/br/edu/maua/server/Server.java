
package br.edu.maua.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private static final int PORT = 5000;

    public static void main(String[] args) {

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            System.out.println("Servidor iniciado!");
            System.out.println("Aguardando conexoes na porta " + PORT);

            while (true) {

                Socket clientSocket = serverSocket.accept();

                System.out.println("Novo cliente conectado: "
                        + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket);

                Thread thread = new Thread(handler);

                thread.start();
            }

        } catch (IOException e) {
            System.out.println("Erro no servidor: " + e.getMessage());
        }
    }
}
