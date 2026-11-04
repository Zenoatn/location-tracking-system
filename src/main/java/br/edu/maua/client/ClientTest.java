
package br.edu.maua.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientTest {

    public static void main(String[] args) {

        try (
            Socket socket = new Socket("localhost", 5000);

            BufferedReader input = new BufferedReader(
                new InputStreamReader(
                    socket.getInputStream(),
                    StandardCharsets.UTF_8
                )
            );

            PrintWriter output = new PrintWriter(
                socket.getOutputStream(),
                true,
                StandardCharsets.UTF_8
            )
        ) {

            System.out.println("Servidor: " + input.readLine());

            String[] requests = {
                "GET_ACTIVE_VEHICLES",
                "GET_SCHEDULE",
                "LOGIN|enzo@maua.br|123456",
                "INVALID_COMMAND"
            };

            for (String request : requests) {

                System.out.println("\nEnviando: " + request);

                output.println(request);

                System.out.println("Resposta: " + input.readLine());
            }

        } catch (Exception e) {
            System.out.println("Erro: " + e.getMessage());
        }
    }
}
