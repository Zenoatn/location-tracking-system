package br.edu.maua.client.driver;

import br.edu.maua.client.communication.ServerClient;
import br.edu.maua.client.location.LocationProvider;
import br.edu.maua.client.location.ManualLocationProvider;
import br.edu.maua.common.protocol.Protocol;
import br.edu.maua.model.VehicleOption;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

public final class DriverWindow extends JFrame {
    private final ServerClient client = new ServerClient();
    private final String token;
    private final ManualLocationProvider manualLocation = new ManualLocationProvider();
    private final LocationProvider locationProvider;
    private final JComboBox<VehicleOption> vehicles = new JComboBox<>();
    private final JComboBox<String> direction = new JComboBox<>(new String[]{
            "Estação Sacomã → IMT", "IMT → Estação Sacomã"});
    private final JComboBox<String> waitingLocation = new JComboBox<>(new String[]{"IMT", "Estação Sacomã"});
    private final JLabel status = new JLabel("Carregando estado…");
    private final JButton wait = new JButton("Declarar espera");
    private final JButton start = new JButton("Iniciar trajeto e compartilhamento");
    private final JButton end = new JButton("Encerrar trajeto");
    private final JButton cancelWait = new JButton("Cancelar espera");
    private final JButton setLocation = new JButton("Informar coordenada manual");
    private final Timer locationTimer;
    private String currentStatus;
    private Long currentVehicleId;
    private Long activeTripId;
    private VehicleOption reservedVehicle;
    private boolean locationRequestInFlight;

    public DriverWindow(JsonObject session) {
        super("Location Tracking — Colaborador");
        token = DriverLoginWindow.value(session, "token");
        locationProvider = java.util.ServiceLoader.load(LocationProvider.class).findFirst().orElse(manualLocation);
        currentStatus = DriverLoginWindow.value(session, "currentStatus");
        currentVehicleId = session.has("currentVehicleId") && !session.get("currentVehicleId").isJsonNull()
                ? session.get("currentVehicleId").getAsLong() : null;
        if (currentVehicleId != null) reservedVehicle = new VehicleOption(currentVehicleId,
                DriverLoginWindow.value(session, "currentVehicleIdentification"),
                DriverLoginWindow.value(session, "currentVehicleType"));
        activeTripId = session.has("activeTripId") && !session.get("activeTripId").isJsonNull()
                ? session.get("activeTripId").getAsLong() : null;
        setDefaultCloseOperation(EXIT_ON_CLOSE); setSize(700, 360); setLocationRelativeTo(null);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 10));
        controls.add(new JLabel("Veículo")); controls.add(vehicles);
        controls.add(new JLabel("Sentido")); controls.add(direction);
        controls.add(new JLabel("Local de espera")); controls.add(waitingLocation);
        controls.add(wait); controls.add(start); controls.add(end); controls.add(cancelWait); controls.add(setLocation);
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 18, 16, 18));
        content.add(new JLabel("Compartilhamento de localização: " + locationProvider.sourceName()), BorderLayout.NORTH);
        content.add(controls, BorderLayout.CENTER); content.add(status, BorderLayout.SOUTH); setContentPane(content);
        vehicles.setRenderer((ListCellRenderer<? super VehicleOption>) (list, value, index, selected, focus) -> {
            JLabel label = new JLabel(value == null ? "Selecione um veículo" : value.identification() + " — " + typePt(value.type()));
            if (selected) { label.setOpaque(true); label.setBackground(list.getSelectionBackground()); label.setForeground(list.getSelectionForeground()); }
            return label;
        });
        wait.addActionListener(event -> declareWait()); start.addActionListener(event -> startTrip());
        end.addActionListener(event -> endTrip()); cancelWait.addActionListener(event -> cancelWait());
        setLocation.addActionListener(event -> promptLocation());
        setLocation.setEnabled(locationProvider == manualLocation);
        locationTimer = new Timer(parseInterval(), event -> sendLocation());
        if ("ON_TRIP".equals(currentStatus) && activeTripId != null) locationTimer.start();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent event) { locationTimer.stop(); }
        });
        refreshVehicles(); refreshControls();
    }

    private void refreshVehicles() {
        JsonObject data = new JsonObject(); data.addProperty("token", token);
        runRequest(Protocol.GET_AVAILABLE_VEHICLES, data, response -> {
            if (!ok(response)) return;
            JsonArray list = response.getAsJsonObject("data").getAsJsonArray("vehicles");
            DefaultComboBoxModel<VehicleOption> model = new DefaultComboBoxModel<>();
            for (var item : list) {
                JsonObject v = item.getAsJsonObject();
                model.addElement(new VehicleOption(v.get("vehicleId").getAsLong(),
                        v.get("identification").getAsString(), DriverLoginWindow.value(v, "type")));
            }
            if (reservedVehicle != null && ("WAITING".equals(currentStatus) || "ON_TRIP".equals(currentStatus))
                    && java.util.stream.IntStream.range(0, model.getSize()).noneMatch(i ->
                            model.getElementAt(i).vehicleId() == reservedVehicle.vehicleId()))
                model.addElement(reservedVehicle);
            vehicles.setModel(model);
            if (currentVehicleId != null) for (int i = 0; i < model.getSize(); i++)
                if (model.getElementAt(i).vehicleId() == currentVehicleId) vehicles.setSelectedIndex(i);
            refreshControls();
        });
    }

    private void declareWait() {
        VehicleOption vehicle = (VehicleOption) vehicles.getSelectedItem(); if (vehicle == null) return;
        JsonObject data = new JsonObject(); data.addProperty("token", token);
        data.addProperty("vehicleId", vehicle.vehicleId());
        data.addProperty("waitingLocation", waitingLocation.getSelectedIndex() == 0 ? "IMT" : "ESTACAO");
        runRequest(Protocol.DECLARE_WAIT, data, response -> {
            if (ok(response)) { currentStatus = "WAITING"; currentVehicleId = vehicle.vehicleId();
                reservedVehicle = vehicle;
                status.setText("Em espera em " + waitingLocation.getSelectedItem() + " com " + vehicle.identification());
                refreshVehicles(); refreshControls(); }
        });
    }

    private void cancelWait() {
        JsonObject data = new JsonObject(); data.addProperty("token", token);
        runRequest(Protocol.CANCEL_WAIT, data, response -> {
            if (ok(response)) { currentStatus = "AVAILABLE"; currentVehicleId = null;
                reservedVehicle = null;
                status.setText("Disponível"); refreshVehicles(); refreshControls(); }
        });
    }

    private void startTrip() {
        VehicleOption vehicle = (VehicleOption) vehicles.getSelectedItem(); if (vehicle == null) return;
        JsonObject data = new JsonObject(); data.addProperty("token", token); data.addProperty("vehicleId", vehicle.vehicleId());
        data.addProperty("direction", direction.getSelectedIndex() == 0 ? "ESTACAO_TO_IMT" : "IMT_TO_ESTACAO");
        runRequest(Protocol.START_TRIP, data, response -> {
            if (ok(response)) {
                JsonObject result = response.getAsJsonObject("data"); activeTripId = result.get("tripId").getAsLong();
                currentStatus = "ON_TRIP"; currentVehicleId = vehicle.vehicleId(); locationTimer.start();
                reservedVehicle = vehicle;
                status.setText(locationProvider.sourceName() + ": informe uma coordenada para iniciar o envio periódico.");
                promptLocation(); refreshVehicles(); refreshControls();
            }
        });
    }

    private void endTrip() {
        int choice = JOptionPane.showConfirmDialog(this, "Encerrar a viagem e liberar o veículo?",
                "Encerrar trajeto", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;
        locationTimer.stop();
        JsonObject data = new JsonObject(); data.addProperty("token", token); data.addProperty("tripId", activeTripId);
        runRequest(Protocol.END_TRIP, data, response -> {
            if (ok(response)) { activeTripId = null; currentStatus = "AVAILABLE"; currentVehicleId = null;
                reservedVehicle = null;
                status.setText("Viagem encerrada. Você está disponível."); refreshVehicles(); refreshControls(); }
            else if ("ON_TRIP".equals(currentStatus)) locationTimer.start();
        });
    }

    private void promptLocation() {
        javax.swing.JTextField lat = new javax.swing.JTextField(12), lng = new javax.swing.JTextField(12);
        JPanel fields = new JPanel(new java.awt.GridLayout(0, 2, 5, 5));
        fields.add(new JLabel("Latitude")); fields.add(lat); fields.add(new JLabel("Longitude")); fields.add(lng);
        int result = JOptionPane.showConfirmDialog(this, fields, "Coordenada manual (não é GPS)",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        try {
            manualLocation.update(Double.parseDouble(lat.getText().trim()), Double.parseDouble(lng.getText().trim()));
            status.setText("Coordenada manual definida. Envio a cada " + parseInterval() / 1000 + " segundos durante a viagem.");
            if ("ON_TRIP".equals(currentStatus)) sendLocation();
        } catch (RuntimeException exception) { JOptionPane.showMessageDialog(this, "Coordenadas inválidas."); }
    }

    private void sendLocation() {
        if (!"ON_TRIP".equals(currentStatus) || activeTripId == null || locationRequestInFlight) return;
        var coordinate = locationProvider.currentLocation();
        if (coordinate.isEmpty()) { status.setText("Localização indisponível: informe uma coordenada manual; nenhuma coordenada falsa será enviada."); return; }
        locationRequestInFlight = true;
        JsonObject data = new JsonObject(); data.addProperty("token", token); data.addProperty("tripId", activeTripId);
        data.addProperty("latitude", coordinate.get().latitude()); data.addProperty("longitude", coordinate.get().longitude());
        runRequest(Protocol.UPDATE_LOCATION, data, response -> {
            locationRequestInFlight = false;
            if (ok(response)) status.setText("Localização manual enviada às " + java.time.LocalTime.now().withNano(0));
        }, () -> locationRequestInFlight = false);
    }

    private void refreshControls() {
        boolean hasVehicle = vehicles.getSelectedItem() != null;
        boolean available = "AVAILABLE".equals(currentStatus), waiting = "WAITING".equals(currentStatus), travelling = "ON_TRIP".equals(currentStatus);
        wait.setEnabled(available && hasVehicle); start.setEnabled((available || waiting) && hasVehicle);
        end.setEnabled(travelling); cancelWait.setEnabled(waiting);
        waitingLocation.setEnabled(available); vehicles.setEnabled(available);
        direction.setEnabled(available || waiting);
        if (currentStatus != null && !"ON_TRIP".equals(currentStatus) && status.getText().startsWith("Carregando"))
            status.setText(switch (currentStatus) { case "WAITING" -> "Estado recuperado: EM ESPERA"; case "AVAILABLE" -> "Estado recuperado: DISPONÍVEL"; default -> "Estado: " + currentStatus; });
        if ("ON_TRIP".equals(currentStatus) && status.getText().startsWith("Carregando"))
            status.setText("Viagem ativa recuperada. Localização: informe ou atualize a coordenada manual.");
    }

    private void runRequest(String operation, JsonObject data, java.util.function.Consumer<JsonObject> success) {
        runRequest(operation, data, success, () -> { });
    }
    private void runRequest(String operation, JsonObject data, java.util.function.Consumer<JsonObject> success, Runnable finished) {
        new SwingWorker<JsonObject, Void>() {
            @Override protected JsonObject doInBackground() throws Exception { return client.request(DriverLoginWindow.request(operation, data)); }
            @Override protected void done() {
                try { JsonObject response = get(); if (ok(response)) success.accept(response); }
                catch (Exception e) { status.setText("Servidor indisponível. A operação pode ser repetida quando a conexão voltar."); }
                finally { finished.run(); }
            }
        }.execute();
    }
    private boolean ok(JsonObject response) {
        if (Protocol.OK.equals(DriverLoginWindow.value(response, "status"))) return true;
        JsonObject error = response.has("error") ? response.getAsJsonObject("error") : new JsonObject();
        status.setText("Operação recusada: " + DriverLoginWindow.value(error, "message")); return false;
    }
    private static int parseInterval() {
        try { return Math.max(1000, Integer.parseInt(System.getenv().getOrDefault("LOCATION_UPDATE_INTERVAL_MS", "5000"))); }
        catch (NumberFormatException e) { return 5000; }
    }
    private static String typePt(String type) {
        if (type == null) return "tipo não informado";
        return switch (type) { case "MICRO_ONIBUS" -> "Micro-ônibus"; case "ONIBUS", "BUS" -> "Ônibus"; case "VAN" -> "Van"; default -> type; };
    }
}
