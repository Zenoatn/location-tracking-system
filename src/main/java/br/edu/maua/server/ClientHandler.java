
package br.edu.maua.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final RequestHandler requestHandler;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.requestHandler = new RequestHandler();
    }

    @Override
    public void run() {

        try (
            BufferedReader input = new BufferedReader(
                new InputStreamReader(
                    clientSocket.getInputStream(),
                    StandardCharsets.UTF_8
                )
            );

            PrintWriter output = new PrintWriter(
                clientSocket.getOutputStream(),
                true,
                StandardCharsets.UTF_8
            )
        ) {

            String request;

            while ((request = input.readLine()) != null) {

                String response = requestHandler.process(request);

                output.println(response);
            }

        } catch (IOException e) {

            System.out.println("Cliente desconectado: "
                    + clientSocket.getInetAddress());

        } finally {

            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Erro ao fechar conexao.");
            }
        }
    }
}
