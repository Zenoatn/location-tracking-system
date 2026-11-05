package br.edu.maua.common.security;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

public final class TlsSupport {

    private static final String[] ALLOWED_PROTOCOLS = {"TLSv1.3", "TLSv1.2"};

    private TlsSupport() {
    }

    public static boolean isEnabled() {
        return Boolean.parseBoolean(System.getenv().getOrDefault("SERVER_TLS_ENABLED", "false"));
    }

    public static ServerSocket createServerSocket(int port) throws IOException {
        char[] password = System.getenv().getOrDefault("SERVER_TLS_KEYSTORE_PASSWORD", "").toCharArray();
        String path = System.getenv("SERVER_TLS_KEYSTORE_PATH");
        String type = System.getenv().getOrDefault("SERVER_TLS_KEYSTORE_TYPE", "PKCS12");
        if (password.length == 0 || path == null || path.isBlank()) {
            throw new IOException("TLS exige SERVER_TLS_KEYSTORE_PATH e SERVER_TLS_KEYSTORE_PASSWORD.");
        }
        try {
            KeyStore keyStore = KeyStore.getInstance(type);
            try (InputStream input = Files.newInputStream(Path.of(path))) {
                keyStore.load(input, password);
            }
            KeyManagerFactory keyManagers = KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm());
            keyManagers.init(keyStore, password);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(keyManagers.getKeyManagers(), null, new SecureRandom());
            SSLServerSocket serverSocket = (SSLServerSocket) context.getServerSocketFactory()
                    .createServerSocket(port);
            serverSocket.setEnabledProtocols(enabledProtocols(serverSocket.getSupportedProtocols()));
            return serverSocket;
        } catch (Exception exception) {
            throw new IOException("Não foi possível inicializar o socket TLS do servidor.", exception);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    public static Socket connect(String host, int port, int connectTimeoutMillis, int readTimeoutMillis)
            throws IOException {
        if (!isEnabled()) {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), connectTimeoutMillis);
            socket.setSoTimeout(readTimeoutMillis);
            return socket;
        }

        Socket transport = new Socket();
        try {
            transport.connect(new InetSocketAddress(host, port), connectTimeoutMillis);
            SSLSocket socket = (SSLSocket) SSLContext.getDefault().getSocketFactory()
                    .createSocket(transport, host, port, true);
            socket.setSoTimeout(readTimeoutMillis);
            socket.setEnabledProtocols(enabledProtocols(socket.getSupportedProtocols()));
            SSLParameters parameters = socket.getSSLParameters();
            parameters.setEndpointIdentificationAlgorithm("HTTPS");
            socket.setSSLParameters(parameters);
            socket.startHandshake();
            return socket;
        } catch (IOException exception) {
            transport.close();
            throw exception;
        } catch (Exception exception) {
            transport.close();
            throw new IOException("Não foi possível estabelecer a conexão TLS com o servidor.", exception);
        }
    }

    private static String[] enabledProtocols(String[] supported) throws IOException {
        String[] enabled = Arrays.stream(ALLOWED_PROTOCOLS)
                .filter(protocol -> Arrays.asList(supported).contains(protocol))
                .toArray(String[]::new);
        if (enabled.length == 0) {
            throw new IOException("A JVM não oferece TLS 1.2 ou TLS 1.3.");
        }
        return enabled;
    }
}
