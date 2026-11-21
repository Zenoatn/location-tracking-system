package br.edu.maua.client.components;

import br.edu.maua.model.ActiveVehicle;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** Static map image panel. It remains usable when Maps is not configured. */
public final class MapPanel extends JPanel {
    private static final HttpClient HTTP = HttpClient.newBuilder().build();
    private final JLabel message = new JLabel("", JLabel.CENTER);
    private Image image;
    private String lastFingerprint;
    private long lastLoadedAt;

    public MapPanel() {

        super(new BorderLayout());
        setBackground(Color.WHITE);
        message.setFont(message.getFont().deriveFont(Font.PLAIN, 15f));
        add(message, BorderLayout.CENTER);

        showMessage("Mapa indisponível até configurar a Google Maps Static API e as coordenadas.");
    }

    public void refresh(List<ActiveVehicle> vehicles) {



        
        String key = System.getenv("MAPS_STATIC_API_KEY");

        //teste
        System.out.println("API KEY carregada: "
            + (key != null && !key.isBlank()));

        System.out.println("Latitude IMT: "
            + System.getenv("MAP_IMT_LAT"));

        System.out.println("Longitude IMT: "
            + System.getenv("MAP_IMT_LNG"));


        
        String imt = configured("MAP_IMT_LAT", "MAP_IMT_LNG");
        String station = configured("MAP_ESTACAO_LAT", "MAP_ESTACAO_LNG");

        if (key == null || key.isBlank() || imt == null || station == null) {
            showMessage("Mapa indisponível: configure a chave e as coordenadas de IMT e ESTAÇÃO.");
            return;
        }


        String fingerprint = vehicles.stream().map(v -> v.getVehicleId() + ":" + v.getLatitude() + ":" + v.getLongitude())
            .sorted().collect(java.util.stream.Collectors.joining("|"));

        long sinceLast = System.currentTimeMillis() - lastLoadedAt;


        if (image != null && sinceLast < 30_000) {
            return;
        }

        if (fingerprint.equals(lastFingerprint) && sinceLast < 60_000) {
            return;
        }

        showMessage("Atualizando mapa…");


        Thread.ofVirtual().start(() -> {
            try {
                
                StringBuilder url = new StringBuilder("https://maps.googleapis.com/maps/api/staticmap?size=800x500&scale=2&maptype=roadmap");
                url.append("&markers=color:green%7Clabel:I%7C").append(enc(imt));
                url.append("&markers=color:blue%7Clabel:E%7C").append(enc(station));


                for (ActiveVehicle vehicle : vehicles) {
                    if (vehicle.getLatitude() != null && vehicle.getLongitude() != null) {
                        String label = vehicle.getDirection() != null && vehicle.getDirection().contains("IMT") ? "I" : "E";
                        url.append("&markers=color:red%7Clabel:").append(label).append("%7C")
                                .append(vehicle.getLatitude()).append(",").append(vehicle.getLongitude());
                    }
                }
                url.append("&key=").append(enc(key));


                HttpResponse<byte[]> response = HTTP.send(HttpRequest.newBuilder(URI.create(url.toString()))
                        .GET().timeout(java.time.Duration.ofSeconds(12)).build(), HttpResponse.BodyHandlers.ofByteArray());



                if (response.statusCode() != 200){

                    //teste
                    System.out.println("\n========== GOOGLE MAPS ERROR ==========");
                    System.out.println("HTTP Status: " + response.statusCode());

                    System.out.println("Content-Type: "
                        + response.headers().firstValue("Content-Type").orElse("desconhecido"));

                    System.out.println("Resposta do Google:");

                    System.out.println(
                        new String(response.body(), StandardCharsets.UTF_8)
                    );

                    System.out.println("=======================================\n");




                    throw new java.io.IOException("Maps API HTTP " + response.statusCode());
                }


                BufferedImage loaded = ImageIO.read(new java.io.ByteArrayInputStream(response.body()));


                if (loaded == null) throw new java.io.IOException("A resposta não contém imagem.");


                SwingUtilities.invokeLater(() -> { image = loaded; message.setText("");
                    lastFingerprint = fingerprint; lastLoadedAt = System.currentTimeMillis(); repaint(); });


            } catch (Exception exception) {
                //showMessage("Não foi possível carregar o mapa. Confira a chave e a conexão.");
                //teste
                exception.printStackTrace();
                showMessage("Não foi possível carregar o mapa. Confira o console.");
            }
        });
    }

    public void showMessage(String value) {
        SwingUtilities.invokeLater(() -> { image = null; message.setText(value); repaint(); });
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (image != null) graphics.drawImage(image, 0, 0, getWidth(), getHeight(), this);
    }

    private static String configured(String latKey, String lngKey) {


        String lat = System.getenv(latKey), lng = System.getenv(lngKey);

        if (lat == null || lng == null) {
            return null;
        }

        try {
            double latitude = Double.parseDouble(lat), longitude = Double.parseDouble(lng);


            if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                    || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {

                return null;
            }


            return latitude + "," + longitude;


        } catch (NumberFormatException exception) { 
            return null; 
        }
    }
    private static String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
