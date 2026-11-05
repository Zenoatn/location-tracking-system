package br.edu.maua.client.interfaces;

import br.edu.maua.client.communication.ServerClient;
import br.edu.maua.common.protocol.Protocol;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

public class MainWindow extends JFrame {

    private static final String BUNDLE = "br.edu.maua.client.i18n.UiText";
    private static final Locale[] SUPPORTED_LOCALES = {
        Locale.forLanguageTag("pt-BR"), Locale.ENGLISH,
        Locale.forLanguageTag("es"), Locale.FRENCH, Locale.GERMAN
    };

    private Locale locale = SUPPORTED_LOCALES[0];
    private ResourceBundle text = ResourceBundle.getBundle(BUNDLE, locale);
    private final ServerClient serverClient = new ServerClient();
    private final DefaultTableModel vehiclesModel = new DefaultTableModel(
            new Object[0][0], new String[0]) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel schedulesModel = new DefaultTableModel(
            new Object[0][0], new String[0]) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private JTable vehiclesTable;
    private JTable schedulesTable;
    private JComboBox<String> directionFilter;
    private JComboBox<String> dayTypeFilter;
    private JSpinner dateFilter;
    private JLabel statusLabel;
    private JMenuItem loginMenuItem;
    private JMenuItem startTripMenuItem;
    private JMenuItem updateLocationMenuItem;
    private JMenuItem endTripMenuItem;
    private String sessionToken;
    private String driverName;
    private boolean driverAccess;
    private boolean vehicleRequestInFlight;
    private Long activeTripId;

    public MainWindow() {
        setTitle(text.getString("app.title"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 620);
        setLocationRelativeTo(null);
        buildWindow();
        new Timer(5000, event -> refreshVehicles(true)).start();
        refreshVehicles(true);
    }

    private void buildWindow() {
        getContentPane().removeAll();
        setJMenuBar(buildMenu());
        setLayout(new BorderLayout(12, 12));
        vehiclesModel.setColumnIdentifiers(new Object[] {
            text.getString("column.vehicle.id"), text.getString("column.vehicle"),
            text.getString("column.direction"),
            text.getString("column.latitude"), text.getString("column.longitude"),
            text.getString("column.updated")
        });
        schedulesModel.setColumnIdentifiers(new Object[] {
            text.getString("column.vehicle"), text.getString("column.departure"),
            text.getString("column.direction"), text.getString("column.day")
        });

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 8));
        filters.setBorder(BorderFactory.createTitledBorder(text.getString("filters.title")));
        directionFilter = new JComboBox<>(new String[] {
            text.getString("direction.all"), "CAMPUS", "CENTRO"
        });
        dayTypeFilter = new JComboBox<>(new String[] {
            text.getString("day.auto"), text.getString("day.weekday"),
            text.getString("day.saturday"), text.getString("day.sunday"),
            text.getString("day.holiday")
        });
        dateFilter = new JSpinner(new SpinnerDateModel());
        DateFormat localizedDateFormat = DateFormat.getDateInstance(DateFormat.SHORT, locale);
        dateFilter.setEditor(new JSpinner.DateEditor(dateFilter,
                ((SimpleDateFormat) localizedDateFormat).toPattern()));
        statusLabel = new JLabel(text.getString("status.ready"));
        filters.add(new JLabel(text.getString("filter.direction")));
        filters.add(directionFilter);
        filters.add(new JLabel(text.getString("filter.day")));
        filters.add(dayTypeFilter);
        filters.add(new JLabel(text.getString("filter.date")));
        filters.add(dateFilter);

        vehiclesTable = new JTable(vehiclesModel);
        vehiclesTable.setFillsViewportHeight(true);
        vehiclesTable.setAutoCreateRowSorter(true);
        schedulesTable = new JTable(schedulesModel);
        schedulesTable.setFillsViewportHeight(true);
        schedulesTable.setAutoCreateRowSorter(true);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(text.getString("tab.vehicles"), new JScrollPane(vehiclesTable));
        tabs.addTab(text.getString("tab.schedules"), new JScrollPane(schedulesTable));

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        content.add(filters, BorderLayout.NORTH);
        content.add(tabs, BorderLayout.CENTER);
        content.add(statusLabel, BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private JMenuBar buildMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu(text.getString("menu.file"));
        JMenuItem refresh = new JMenuItem(text.getString("menu.refresh"));
        refresh.addActionListener(event -> {
            refreshVehicles(false);
            refreshSchedules();
        });
        JMenuItem export = new JMenuItem(text.getString("menu.export"));
        export.addActionListener(event -> exportVehicles(false));
        JMenuItem openText = new JMenuItem(text.getString("menu.open.text"));
        openText.addActionListener(event -> openTextFile());
        JMenuItem encryptedExport = new JMenuItem(text.getString("menu.export.encrypted"));
        encryptedExport.addActionListener(event -> exportVehicles(true));
        loginMenuItem = new JMenuItem(sessionToken == null
                ? text.getString("menu.driver.login") : text.getString("menu.driver.logged.in") + driverName);
        loginMenuItem.addActionListener(event -> showLoginDialog());
        JMenuItem exit = new JMenuItem(text.getString("menu.exit"));
        exit.addActionListener(event -> dispose());
        fileMenu.add(refresh);
        fileMenu.add(openText);
        fileMenu.add(export);
        fileMenu.add(encryptedExport);
        fileMenu.addSeparator();
        fileMenu.add(exit);

        JMenu driverMenu = new JMenu(text.getString("menu.driver"));
        startTripMenuItem = new JMenuItem(text.getString("menu.trip.start"));
        startTripMenuItem.setEnabled(driverAccess && activeTripId == null);
        startTripMenuItem.addActionListener(event -> showStartTripDialog());
        updateLocationMenuItem = new JMenuItem(text.getString("menu.location.update"));
        updateLocationMenuItem.setEnabled(driverAccess && activeTripId != null);
        updateLocationMenuItem.addActionListener(event -> showUpdateLocationDialog());
        endTripMenuItem = new JMenuItem(text.getString("menu.trip.end"));
        endTripMenuItem.setEnabled(driverAccess && activeTripId != null);
        endTripMenuItem.addActionListener(event -> confirmEndTrip());
        driverMenu.add(loginMenuItem);
        driverMenu.addSeparator();
        driverMenu.add(startTripMenuItem);
        driverMenu.add(updateLocationMenuItem);
        driverMenu.add(endTripMenuItem);

        JMenu languageMenu = new JMenu(text.getString("menu.language"));
        for (Locale supported : SUPPORTED_LOCALES) {
            JMenuItem item = new JMenuItem(languageName(supported));
            item.addActionListener(event -> changeLanguage(supported));
            languageMenu.add(item);
        }

        JMenu helpMenu = new JMenu(text.getString("menu.help"));
        JMenuItem about = new JMenuItem(text.getString("menu.about"));
        about.addActionListener(event -> JOptionPane.showMessageDialog(
                this, text.getString("about.message"), text.getString("menu.about"),
                JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(about);
        menuBar.add(fileMenu);
        menuBar.add(driverMenu);
        menuBar.add(languageMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private void refreshVehicles() {
        refreshVehicles(false);
    }

    private void refreshVehicles(boolean quiet) {
        if (vehicleRequestInFlight) return;
        vehicleRequestInFlight = true;
        statusLabel.setText(text.getString("status.loading"));
        new SwingWorker<JsonObject, Void>() {
            @Override
            protected JsonObject doInBackground() throws Exception {
                JsonObject data = new JsonObject();
                JsonObject request = request(Protocol.GET_ACTIVE_VEHICLES, data);
                return serverClient.request(request);
            }

            @Override
            protected void done() {
                try {
                    JsonObject response = get();
                    if (!Protocol.OK.equals(value(response, Protocol.STATUS))) {
                        showServerError(response);
                        statusLabel.setText(text.getString("status.server.error"));
                        return;
                    }
                    fillVehicleTable(response.getAsJsonObject(Protocol.DATA).getAsJsonArray("vehicles"));
                    statusLabel.setText(text.getString("status.updated"));
                } catch (Exception exception) {
                    if (!quiet) showError(text.getString("error.connection"));
                    statusLabel.setText(text.getString("status.offline"));
                } finally {
                    vehicleRequestInFlight = false;
                }
            }
        }.execute();
    }

    private void fillVehicleTable(JsonArray vehicles) {
        vehiclesModel.setColumnIdentifiers(new Object[] {
            text.getString("column.vehicle.id"), text.getString("column.vehicle"),
            text.getString("column.direction"),
            text.getString("column.latitude"), text.getString("column.longitude"),
            text.getString("column.updated")
        });
        vehiclesModel.setRowCount(0);
        for (int i = 0; i < vehicles.size(); i++) {
            JsonObject vehicle = vehicles.get(i).getAsJsonObject();
            vehiclesModel.addRow(new Object[] {
                value(vehicle, "vehicleId"), value(vehicle, "identification"), value(vehicle, "direction"),
                value(vehicle, "latitude"), value(vehicle, "longitude"),
                value(vehicle, "recordedAt")
            });
        }
    }

    private void refreshSchedules() {
        statusLabel.setText(text.getString("status.loading"));
        int directionIndex = directionFilter.getSelectedIndex();
        String direction = directionIndex > 0
                ? directionFilter.getSelectedItem().toString() : null;
        LocalDate selectedDate = ((java.util.Date) dateFilter.getValue()).toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        String dayType = selectedDayType(selectedDate);
        new SwingWorker<JsonObject, Void>() {
            @Override
            protected JsonObject doInBackground() throws Exception {
                JsonObject data = new JsonObject();
                if (direction != null) {
                    data.addProperty("direction", direction);
                }
                data.addProperty("dayType", dayType);
                return serverClient.request(request(Protocol.GET_SCHEDULE, data));
            }

            @Override
            protected void done() {
                try {
                    JsonObject response = get();
                    if (!Protocol.OK.equals(value(response, Protocol.STATUS))) {
                        showServerError(response);
                        statusLabel.setText(text.getString("status.server.error"));
                        return;
                    }
                    fillScheduleTable(response.getAsJsonObject(Protocol.DATA).getAsJsonArray("schedules"));
                    statusLabel.setText(text.getString("status.updated"));
                } catch (Exception exception) {
                    showError(text.getString("error.connection"));
                    statusLabel.setText(text.getString("status.offline"));
                }
            }
        }.execute();
    }

    private String selectedDayType(LocalDate date) {
        return switch (dayTypeFilter.getSelectedIndex()) {
            case 1 -> "WEEKDAY";
            case 2 -> "SATURDAY";
            case 3 -> "SUNDAY";
            case 4 -> "HOLIDAY";
            default -> dayTypeFromDate(date);
        };
    }

    private String dayTypeFromDate(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case SATURDAY -> "SATURDAY";
            case SUNDAY -> "SUNDAY";
            default -> "WEEKDAY";
        };
    }

    private void fillScheduleTable(JsonArray schedules) {
        schedulesModel.setColumnIdentifiers(new Object[] {
            text.getString("column.vehicle"), text.getString("column.departure"),
            text.getString("column.direction"), text.getString("column.day")
        });
        schedulesModel.setRowCount(0);
        for (int i = 0; i < schedules.size(); i++) {
            JsonObject schedule = schedules.get(i).getAsJsonObject();
            schedulesModel.addRow(new Object[] {
                value(schedule, "vehicleId"), value(schedule, "departureTime"),
                value(schedule, "direction"), value(schedule, "dayType")
            });
        }
    }

    private String value(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull()
                ? object.get(key).getAsString() : "";
    }

    private void showLoginDialog() {
        JTextField email = new JTextField(24);
        JPasswordField password = new JPasswordField(24);
        JPanel fields = new JPanel(new GridLayout(0, 1, 6, 6));
        fields.add(new JLabel(text.getString("login.email")));
        fields.add(email);
        fields.add(new JLabel(text.getString("login.password")));
        fields.add(password);
        int choice = JOptionPane.showConfirmDialog(this, fields, text.getString("login.title"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            password.setText("");
            return;
        }
        char[] secret = password.getPassword();
        password.setText("");
        if (email.getText().isBlank() || secret.length == 0) {
            java.util.Arrays.fill(secret, '\0');
            showError(text.getString("login.required"));
            return;
        }
        JsonObject data = new JsonObject();
        data.addProperty("email", email.getText().trim());
        data.addProperty("password", new String(secret));
        java.util.Arrays.fill(secret, '\0');
        sendRequest(Protocol.LOGIN, data, text.getString("login.title"), response -> {
            JsonObject result = response.getAsJsonObject(Protocol.DATA);
            sessionToken = value(result, "token");
            driverName = value(result, "name");
            driverAccess = "DRIVER".equals(value(result, "role"));
            activeTripId = result.has("activeTripId") && !result.get("activeTripId").isJsonNull()
                    ? result.get("activeTripId").getAsLong() : null;
            buildWindow();
        });
    }

    private void sendRequest(String operation, JsonObject data, String title) {
        sendRequest(operation, data, title, response -> { });
    }

    private void sendRequest(String operation, JsonObject data, String title,
            Consumer<JsonObject> onSuccess) {
        new SwingWorker<JsonObject, Void>() {
            @Override
            protected JsonObject doInBackground() throws Exception {
                try {
                    return serverClient.request(request(operation, data));
                } finally {
                    if (Protocol.LOGIN.equals(operation)) {
                        data.remove("password");
                    }
                }
            }

            @Override
            protected void done() {
                try {
                    JsonObject response = get();
                    if (Protocol.OK.equals(value(response, Protocol.STATUS))) {
                        onSuccess.accept(response);
                        JOptionPane.showMessageDialog(MainWindow.this,
                                text.getString("message.success"), title, JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        showServerError(response);
                    }
                } catch (Exception exception) {
                    showError(text.getString("error.connection"));
                }
            }
        }.execute();
    }

    private void showStartTripDialog() {
        JSpinner vehicleId = new JSpinner(new SpinnerNumberModel(1L, 1L, Long.MAX_VALUE, 1L));
        JComboBox<String> direction = new JComboBox<>(new String[] {"CAMPUS", "CENTRO"});
        JPanel fields = new JPanel(new GridLayout(0, 1, 6, 6));
        fields.add(new JLabel(text.getString("trip.vehicle")));
        fields.add(vehicleId);
        fields.add(new JLabel(text.getString("trip.direction")));
        fields.add(direction);
        int choice = JOptionPane.showConfirmDialog(this, fields, text.getString("menu.trip.start"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;

        JsonObject data = new JsonObject();
        data.addProperty("token", sessionToken);
        data.addProperty("vehicleId", ((Number) vehicleId.getValue()).longValue());
        data.addProperty("direction", direction.getSelectedItem().toString());
        sendRequest(Protocol.START_TRIP, data, text.getString("menu.trip.start"), response -> {
            activeTripId = response.getAsJsonObject(Protocol.DATA).get("tripId").getAsLong();
            buildWindow();
        });
    }

    private void showUpdateLocationDialog() {
        JTextField latitude = new JTextField(14);
        JTextField longitude = new JTextField(14);
        JPanel fields = new JPanel(new GridLayout(0, 1, 6, 6));
        fields.add(new JLabel(text.getString("location.latitude")));
        fields.add(latitude);
        fields.add(new JLabel(text.getString("location.longitude")));
        fields.add(longitude);
        int choice = JOptionPane.showConfirmDialog(this, fields, text.getString("menu.location.update"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;

        try {
            JsonObject data = new JsonObject();
            data.addProperty("token", sessionToken);
            data.addProperty("tripId", activeTripId);
            data.addProperty("latitude", Double.parseDouble(latitude.getText().trim()));
            data.addProperty("longitude", Double.parseDouble(longitude.getText().trim()));
            sendRequest(Protocol.UPDATE_LOCATION, data, text.getString("menu.location.update"),
                    response -> {
                        statusLabel.setText(text.getString("status.location.saved"));
                        refreshVehicles();
                    });
        } catch (NumberFormatException exception) {
            showError(text.getString("location.invalid"));
        }
    }

    private void confirmEndTrip() {
        int choice = JOptionPane.showConfirmDialog(this, text.getString("trip.end.confirm"),
                text.getString("menu.trip.end"), JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;
        JsonObject data = new JsonObject();
        data.addProperty("token", sessionToken);
        data.addProperty("tripId", activeTripId);
        sendRequest(Protocol.END_TRIP, data, text.getString("menu.trip.end"), response -> {
            activeTripId = null;
            buildWindow();
        });
    }

    private JsonObject request(String operation, JsonObject data) {
        JsonObject request = new JsonObject();
        request.addProperty(Protocol.REQUEST_ID, UUID.randomUUID().toString());
        request.addProperty(Protocol.OPERATION, operation);
        request.add(Protocol.DATA, data);
        return request;
    }

    private void exportVehicles(boolean encrypt) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(text.getString(encrypt ? "export.encrypted.title" : "export.title"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path target = chooser.getSelectedFile().toPath();
        StringBuilder report = new StringBuilder();
        report.append(text.getString("app.title")).append('\n')
                .append(text.getString("export.generated")).append(": ")
                .append(Instant.now()).append('\n');
        for (int column = 0; column < vehiclesModel.getColumnCount(); column++) {
            if (column > 0) report.append('\t');
            report.append(vehiclesModel.getColumnName(column));
        }
        report.append('\n');
        for (int row = 0; row < vehiclesModel.getRowCount(); row++) {
            for (int column = 0; column < vehiclesModel.getColumnCount(); column++) {
                if (column > 0) report.append('\t');
                report.append(vehiclesModel.getValueAt(row, column));
            }
            report.append('\n');
        }
        try {
            if (encrypt) {
                if (!writeEncryptedExport(target, report.toString())) {
                    return;
                }
            } else {
                try (BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
                    writer.write(report.toString());
                }
            }
            JOptionPane.showMessageDialog(this, text.getString("export.saved"),
                    text.getString("menu.export"), JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception exception) {
            showError(text.getString("error.generic"));
        }
    }

    private void openTextFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(text.getString("menu.open.text"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Path source = chooser.getSelectedFile().toPath();
            if (Files.size(source) > 5_000_000) {
                showError(text.getString("file.too.large"));
                return;
            }
            JTextArea preview = new JTextArea(Files.readString(source, StandardCharsets.UTF_8), 24, 72);
            preview.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(preview), source.getFileName().toString(),
                    JOptionPane.PLAIN_MESSAGE);
        } catch (Exception exception) {
            showError(text.getString("error.generic"));
        }
    }

    private boolean writeEncryptedExport(Path target, String report) throws Exception {
        JPasswordField password = new JPasswordField(20);
        int choice = JOptionPane.showConfirmDialog(this, password, text.getString("export.password"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            password.setText("");
            return false;
        }
        char[] secret = password.getPassword();
        password.setText("");
        if (secret.length < 10) {
            java.util.Arrays.fill(secret, '\0');
            showError(text.getString("export.password.short"));
            return false;
        }
        try {
            Files.write(target, br.edu.maua.common.security.CryptoUtil.encrypt(report, secret));
        } finally {
            java.util.Arrays.fill(secret, '\0');
        }
        return true;
    }

    private void showServerError(JsonObject response) {
        String message = text.getString("error.generic");
        if (response.has(Protocol.ERROR) && response.get(Protocol.ERROR).isJsonObject()) {
            JsonObject error = response.getAsJsonObject(Protocol.ERROR);
            String code = value(error, Protocol.CODE);
            String localizedKey = "server.error." + code;
            if (text.containsKey(localizedKey)) {
                message = text.getString(localizedKey);
            } else if (error.has(Protocol.MESSAGE)) {
                message = error.get(Protocol.MESSAGE).getAsString();
            }
        }
        showError(message);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this,
                message == null ? text.getString("error.generic") : message,
                text.getString("error.title"), JOptionPane.ERROR_MESSAGE);
    }

    private void changeLanguage(Locale selected) {
        locale = selected;
        text = ResourceBundle.getBundle(BUNDLE, locale);
        setTitle(text.getString("app.title"));
        buildWindow();
    }

    private String languageName(Locale selected) {
        return switch (selected.getLanguage()) {
            case "pt" -> "Português";
            case "es" -> "Español";
            case "fr" -> "Français";
            case "de" -> "Deutsch";
            default -> "English";
        };
    }
}
