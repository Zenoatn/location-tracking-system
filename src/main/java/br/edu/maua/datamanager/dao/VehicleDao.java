package br.edu.maua.datamanager.dao;

import br.edu.maua.datamanager.DatabaseConnection;
import br.edu.maua.model.ActiveVehicle;
import br.edu.maua.model.VehicleOption;
import br.edu.maua.model.WaitingDriver;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class VehicleDao {

    private static final String FIND_ACTIVE = """
            SELECT v.id AS vehicle_id, v.identification, v.type, t.direction,
                   l.latitude, l.longitude, l.recorded_at
            FROM vehicles v
            JOIN trips t ON t.id = (
                SELECT MAX(t2.id) FROM trips t2
                WHERE t2.vehicle_id = v.id AND t2.status = 'ACTIVE'
            )
            LEFT JOIN locations l ON l.id = (
                SELECT l2.id FROM locations l2
                WHERE l2.trip_id = t.id
                ORDER BY l2.recorded_at DESC, l2.id DESC
                LIMIT 1
            )
            WHERE v.active = TRUE
            ORDER BY v.identification
            """;

    public List<ActiveVehicle> findActiveVehicles() throws SQLException {
        List<ActiveVehicle> vehicles = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ACTIVE);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                Timestamp recordedAt = result.getTimestamp("recorded_at");
                vehicles.add(new ActiveVehicle(
                        result.getLong("vehicle_id"),
                        result.getString("identification"), result.getString("type"),
                        result.getString("direction"),
                        nullableDouble(result, "latitude"),
                        nullableDouble(result, "longitude"),
                        recordedAt == null ? null : recordedAt.toInstant().toString()));
            }
        }
        return vehicles;
    }

    public List<VehicleOption> findAvailable() throws SQLException {
        String sql = "SELECT v.id, v.identification, v.type FROM vehicles v "
                + "LEFT JOIN users u ON u.current_vehicle_id = v.id "
                + "WHERE v.active = TRUE AND u.id IS NULL "
                + "ORDER BY v.identification";
        List<VehicleOption> vehicles = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) vehicles.add(new VehicleOption(result.getLong("id"),
                        result.getString("identification"), result.getString("type")));
            }
        }
        return vehicles;
    }

    public List<WaitingDriver> findWaitingDrivers(String location) throws SQLException {
        String sql = "SELECT u.id, u.name, u.waiting_location, v.id vehicle_id, "
                + "v.identification, v.type FROM users u JOIN vehicles v "
                + "ON v.id = u.current_vehicle_id WHERE u.current_status = 'WAITING' "
                + (location == null ? "" : "AND u.waiting_location = ? ")
                + "ORDER BY u.waiting_location, u.name";
        List<WaitingDriver> drivers = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (location != null) statement.setString(1, location);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) drivers.add(new WaitingDriver(result.getLong("id"),
                        result.getString("name"), result.getString("waiting_location"),
                        result.getLong("vehicle_id"), result.getString("identification"),
                        result.getString("type")));
            }
        }
        return drivers;
    }

    private Double nullableDouble(ResultSet result, String column) throws SQLException {
        double value = result.getDouble(column);
        return result.wasNull() ? null : value;
    }
}
