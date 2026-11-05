
package br.edu.maua.server;

import br.edu.maua.common.security.TlsSupport;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private static final int DEFAULT_PORT = 5000;

    public static void main(String[] args) {

        int port;
        try {
            port = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "" + DEFAULT_PORT));
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("SERVER_PORT deve estar entre 1 e 65535.");
            }
        } catch (RuntimeException exception) {
            System.err.println("Porta inválida: " + exception.getMessage());
            return;
        }

        try (ServerSocket serverSocket = TlsSupport.isEnabled()
                ? TlsSupport.createServerSocket(port) : new ServerSocket(port)) {

            System.out.println("Servidor iniciado!");
            System.out.println("TLS: " + (TlsSupport.isEnabled() ? "ativado" : "desativado"));
            System.out.println("Aguardando conexoes na porta " + port);

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
