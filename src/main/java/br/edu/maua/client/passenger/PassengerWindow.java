package br.edu.maua.client.passenger;

import br.edu.maua.client.communication.RoutesEtaClient;
import br.edu.maua.client.communication.ServerClient;
import br.edu.maua.client.components.MapPanel;
import br.edu.maua.common.protocol.Protocol;
import br.edu.maua.model.ActiveVehicle;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

public final class PassengerWindow extends JFrame {


    private final ServerClient client = new ServerClient();
    private final RoutesEtaClient routes = new RoutesEtaClient();
    private final JComboBox<String> location = new JComboBox<>(new String[]{"IMT", "ESTAÇÃO", "TODAS"});
    private final JLabel arrival = new JLabel("Carregando veículos…");
    private final JLabel waiting = new JLabel(" ");
    private final JLabel connection = new JLabel(" ");
    private final MapPanel map = new MapPanel();
    private final JButton schedules = new JButton("Consultar Horários");
    private final Timer refreshTimer = new Timer(10_000, event -> refresh());
    private boolean requestInFlight;
    private long arrivalGeneration;
    private List<ActiveVehicle> lastVehicles = List.of();

    public PassengerWindow() {

        super("Location Tracking — Passageiro");

        setDefaultCloseOperation(EXIT_ON_CLOSE); 
        setMinimumSize(new Dimension(900, 620)); 
        setSize(1100, 720);
        setLocationRelativeTo(null);


        JPanel top = new JPanel(new BorderLayout(12, 8));
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEADING));

        filters.add(new JLabel("Localização")); 
        filters.add(location); 
        filters.add(schedules);

        top.add(filters, BorderLayout.NORTH); 
        top.add(arrival, BorderLayout.CENTER);

        arrival.setFont(arrival.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        JPanel side = new JPanel(new BorderLayout()); 
        side.setPreferredSize(new Dimension(260, 100));
        side.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 12, 10, 12));


        waiting.setForeground(new java.awt.Color(20, 115, 55)); 
        waiting.setFont(waiting.getFont().deriveFont(java.awt.Font.BOLD, 14f));

        side.add(waiting, BorderLayout.NORTH); side.add(connection, BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout(8, 8)); 

        body.add(map, BorderLayout.CENTER); 
        body.add(side, BorderLayout.EAST);


        JPanel content = new JPanel(new BorderLayout(8, 8));

        content.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 14, 12, 14));
        content.add(top, BorderLayout.NORTH); 
        content.add(body, BorderLayout.CENTER); 
        
        setContentPane(content);

        location.addActionListener(event -> { updateWaitingDisplay(); refresh(); });
        schedules.addActionListener(event -> loadSchedules());

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override 
            public void windowClosing(java.awt.event.WindowEvent event){ 
                refreshTimer.stop(); 
            }
        });

        refresh(); 

        refreshTimer.start();
    }

    private void refresh() {

        if (requestInFlight) {
            return;
        }

        requestInFlight = true;

        SwingWorker<Snapshot, Void> worker = new SwingWorker<>() {

            @Override 
            protected Snapshot doInBackground() throws Exception {

                JsonObject vehiclesResponse = client.request(request(Protocol.GET_ACTIVE_VEHICLES, new JsonObject()));
                JsonArray vehicleRows = requireData(vehiclesResponse).getAsJsonArray("vehicles");


                List<ActiveVehicle> vehicles = new ArrayList<>();

                for (var item : vehicleRows) {
                    JsonObject row = item.getAsJsonObject();

                    vehicles.add(new ActiveVehicle(row.get("vehicleId").getAsLong(), str(row, "identification"),
                            str(row, "type"), str(row, "direction"), number(row, "latitude"),
                            number(row, "longitude"), str(row, "recordedAt")));
                }

                JsonObject waitingData = new JsonObject();

                String selected = selectedLocation();

                if (!"TODAS".equals(selected)) {
                    waitingData.addProperty("location", "ESTAÇÃO".equals(selected) ? "ESTACAO" : "IMT");
                }

                JsonObject waitingResponse = client.request(request(Protocol.GET_WAITING_DRIVERS, waitingData));
                JsonArray waitingRows = requireData(waitingResponse).getAsJsonArray("drivers");


                return new Snapshot(vehicles, waitingRows);
            }
            @Override 
            protected void done() {
                requestInFlight = false;

                try {
                    Snapshot snapshot = get(); 
                    
                    lastVehicles = snapshot.vehicles();
                    List<ActiveVehicle> visible = lastVehicles.stream().filter(v -> "TODAS".equals(selectedLocation())
                            || headsTo(v.getDirection(), selectedLocation())).toList();

                    updateArrival(visible); 
                    updateWaiting(snapshot.waiting()); 
                    
                    map.refresh(visible);

                    connection.setText("Atualizado agora");

                } catch (Exception exception) {
                    arrival.setText("Não foi possível consultar os veículos.");
                    connection.setText("Servidor indisponível; tentando novamente automaticamente.");
                    map.showMessage("Mapa e dados temporariamente indisponíveis.");
                }
            }
        };
        
        worker.execute();
    }

    private void updateArrival(List<ActiveVehicle> vehicles) {

        String selected = selectedLocation();

        List<ActiveVehicle> relevant = vehicles.stream().filter(v -> "TODAS".equals(selected)
            || headsTo(v.getDirection(), selected)).toList();


        if (relevant.isEmpty()) {

            arrival.setText("TODAS".equals(selected) ? "Nenhum veículo atualmente em deslocamento"
                : "Nenhum veículo atualmente em deslocamento à localização atual");

            return;
        }


        boolean routesConfigured = System.getenv("GOOGLE_MAPS_ROUTES_API_KEY") != null
            && !System.getenv("GOOGLE_MAPS_ROUTES_API_KEY").isBlank();


        if (!routesConfigured || (!"TODAS".equals(selected) && coordsFor(selected) == null)) {
            arrival.setText(relevant.size() == 1
                ? typePt(relevant.get(0).getType()) + " " + relevant.get(0).getIdentification() + " em deslocamento; estimativa indisponível."
                : relevant.size() + " veículos em deslocamento; estimativa de chegada indisponível.");



            return;
        }


        arrival.setText("Calculando estimativa de chegada…");


        long generation = ++arrivalGeneration;


        new SwingWorker<String, Void>() {
            @Override 
            protected String doInBackground() {

                List<ArrivalCandidate> candidates = new ArrayList<>();

                List<String> unavailable = new ArrayList<>();

                for (ActiveVehicle vehicle : relevant) {

                    String destination = "TODAS".equals(selected)
                        ? (headsTo(vehicle.getDirection(), "IMT") ? "IMT" : "ESTAÇÃO") : selected;

                    String routeTarget = coordsFor(destination);


                    if (routeTarget == null || vehicle.getLatitude() == null || vehicle.getLongitude() == null) {
                        unavailable.add(vehicle.getIdentification() + ": estimativa indisponível");
                        continue;
                    }


                    OptionalLong minutes = routes.estimateMinutes(vehicle.getLatitude(), vehicle.getLongitude(), routeTarget);

                    if (minutes.isPresent()) {
                        candidates.add(new ArrivalCandidate(vehicle, minutes.getAsLong(), destination));
                    }
                    else{
                        unavailable.add(vehicle.getIdentification() + ": estimativa indisponível");
                    }
                }
                if ("TODAS".equals(selected)) {
                    List<String> messages = new ArrayList<>();

                    for (ArrivalCandidate candidate : candidates) {
                        messages.add(typePt(candidate.vehicle().getType())
                            + " " + candidate.vehicle().getIdentification() + " → " + candidate.destination()
                            + ": aprox. " + candidate.minutes() + " min");
                    }

                    messages.addAll(unavailable);

                    return messages.isEmpty() ? "Estimativa de chegada indisponível no momento."
                        : String.join(" | ", messages);
                }

                if (candidates.isEmpty()){
                    return "Estimativa de chegada indisponível no momento.";
                }

                ArrivalCandidate closest = candidates.stream().min(java.util.Comparator.comparingLong(ArrivalCandidate::minutes)).orElseThrow();

                String destination = "IMT".equals(selected) ? "ao IMT" : "à Estação Sacomã";

                return typePt(closest.vehicle().getType()) + " " + closest.vehicle().getIdentification()
                    + " chegando " + destination + " em aproximadamente " + closest.minutes() + " minutos.";
            }
            @Override 
            protected void done() {
                try { 
                    if (generation == arrivalGeneration && selected.equals(selectedLocation())){
                        arrival.setText(get());
                    } 
                }
                catch (Exception e){ 
                    arrival.setText("Estimativa de chegada indisponível no momento."); 
                }
            }
        }.execute();
    }

    private void updateWaiting(JsonArray drivers) {

        String selected = selectedLocation();

        List<String> places = new ArrayList<>();

        for (var item : drivers) {
            String place = str(item.getAsJsonObject(), "waitingLocation");
            if (!places.contains(place)){
                places.add(place);
            }
        }

        if (places.isEmpty()){
            waiting.setText(" ");
        }
        else if ("TODAS".equals(selected)){
            waiting.setText("UM MOTORISTA ESTÁ ESPERANDO: " + String.join(", ", places));
        }
        else{
            waiting.setText("UM MOTORISTA ESTÁ ESPERANDO NO PONTO DE ESPERA NESTE MOMENTO");
        }
    }

    private void loadSchedules() {

        String selected = selectedLocation();
        JsonObject data = new JsonObject();

        // Deliberate inverse filter: at IMT show services heading back to the station.
        if ("IMT".equals(selected)){
            data.addProperty("direction", "CENTRO");
        }
        else if ("ESTAÇÃO".equals(selected)){
            data.addProperty("direction", "CAMPUS");
        }

        new SwingWorker<JsonArray, Void>() {

            @Override 
            protected JsonArray doInBackground() throws Exception {
                return requireData(client.request(request(Protocol.GET_SCHEDULE, data))).getAsJsonArray("schedules");
            }


            @Override 
            protected void done() {
                try {
                    JsonArray rows = get();
                    DefaultTableModel model = new DefaultTableModel(new Object[]{"Veículo", "Tipo", "Partida", "Origem", "Destino", "Tipo de dia"}, 0){
                        @Override 
                        public boolean isCellEditable(int row, int column){ 
                            return false; 
                        }
                    };
                    for(var item : rows){
                        JsonObject row = item.getAsJsonObject();

                        String direction = str(row, "direction");

                        String from, to;


                        if("CAMPUS".equals(direction) || "ESTACAO_TO_IMT".equals(direction)){ 
                            from = "Estação Sacomã"; to = "IMT"; 
                        }
                        else{ 
                            from = "IMT"; to = "Estação Sacomã"; 
                        }


                        model.addRow(new Object[]{str(row, "identification"), typePt(str(row, "type")),
                                str(row, "departureTime"), from, to, str(row, "dayType")});
                    }

                    JTable table = new JTable(model); table.setFillsViewportHeight(true);

                    JOptionPane.showMessageDialog(PassengerWindow.this, new JScrollPane(table),
                            "Horários — " + selected, JOptionPane.PLAIN_MESSAGE);

                } catch (Exception e) { 
                    JOptionPane.showMessageDialog(PassengerWindow.this,
                        "Não foi possível consultar os horários.", "Erro de conexão", JOptionPane.ERROR_MESSAGE); 
                }
            }
        }.execute();
    }

    private void updateWaitingDisplay() { 
        waiting.setText(" "); 
    }



    private String selectedLocation() { 
        return location.getSelectedItem().toString(); 
    }

    private static boolean headsTo(String direction, String place) {
        if ("IMT".equals(place)){
            return "CAMPUS".equals(direction) || "ESTACAO_TO_IMT".equals(direction) || "TO_IMT".equals(direction);
        }


        return "CENTRO".equals(direction) || "IMT_TO_ESTACAO".equals(direction) || "TO_ESTACAO".equals(direction);
    }


    private static String coordsFor(String place) {

        String prefix = "IMT".equals(place) ? "MAP_IMT" : "MAP_ESTACAO";


        try {

            double lat = Double.parseDouble(System.getenv(prefix + "_LAT"));
            double lng = Double.parseDouble(System.getenv(prefix + "_LNG"));

            if (!Double.isFinite(lat) || !Double.isFinite(lng) || lat < -90 || lat > 90 || lng < -180 || lng > 180){
                return null;
            }

            return lat + "," + lng;

        } catch (Exception e) {
            return null; 
        }
    }


    private JsonObject requireData(JsonObject response) throws java.io.IOException {

        if (Protocol.OK.equals(str(response, "status"))){
            return response.getAsJsonObject("data");
        }

        throw new java.io.IOException("Server request failed");
    }


    private static JsonObject request(String operation, JsonObject data) {
        JsonObject request = new JsonObject(); 
        
        request.addProperty("requestId", UUID.randomUUID().toString());
        request.addProperty("operation", operation);
        request.add("data", data); 
         
        return request;
    }


    private static String str(JsonObject obj, String key){ 
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null; 
    }


    private static Double number(JsonObject obj, String key){ 
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsDouble() : null; 
    }


    private static String typePt(String type) {
        if (type == null){
            return "Veículo";
        }

        return switch (type){ 
            case "MICRO_ONIBUS" -> "Micro-ônibus"; 
            case "ONIBUS", "BUS" -> "Ônibus";
            case "VAN" -> "Van"; 
            default -> type; 
        };
    }

    private record Snapshot(List<ActiveVehicle> vehicles, JsonArray waiting) { }

    private record ArrivalCandidate(ActiveVehicle vehicle, long minutes, String destination) { }
}
