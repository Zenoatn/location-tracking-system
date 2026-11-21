package br.edu.maua.client.communication;

import br.edu.maua.common.security.TlsSupport;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Sends one JSON-line request and reads its matching response. */
public class ServerClient {

    private final String host;
    private final int port;

    public ServerClient() {
        host = System.getenv().getOrDefault("SERVER_HOST", "localhost");
        port = parsePort(System.getenv().getOrDefault("SERVER_PORT", "5000"));
    }

    public JsonObject request(JsonObject request) throws IOException {
        try (Socket socket = TlsSupport.connect(host, port, 5000, 7000)) {
            try (PrintWriter output = new PrintWriter(
                         new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                         
                BufferedReader input = new BufferedReader(
                         new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                output.println(request);
                String response = input.readLine();
                if (response == null) {
                    throw new IOException("O servidor encerrou a conexão sem responder.");
                }
                try {
                    JsonObject parsed = JsonParser.parseString(response).getAsJsonObject();
                    String expectedId = request.has("requestId")
                            ? request.get("requestId").getAsString() : null;
                    String responseId = parsed.has("requestId") && !parsed.get("requestId").isJsonNull()
                            ? parsed.get("requestId").getAsString() : null;
                    if (expectedId == null || !expectedId.equals(responseId)) {
                        throw new IOException("O identificador da resposta não corresponde à requisição.");
                    }
                    return parsed;
                } catch (RuntimeException exception) {
                    throw new IOException("O servidor retornou uma resposta JSON inválida.", exception);
                }
            }
        }
    }

    private int parsePort(String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1 || parsed > 65535) {
                throw new NumberFormatException("fora do intervalo TCP");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("SERVER_PORT deve estar entre 1 e 65535.", exception);
        }
    }
}
