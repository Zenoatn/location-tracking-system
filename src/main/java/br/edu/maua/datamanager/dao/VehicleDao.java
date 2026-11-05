package br.edu.maua.datamanager.dao;

import br.edu.maua.datamanager.DatabaseConnection;
import br.edu.maua.model.ActiveVehicle;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class VehicleDao {

    private static final String FIND_ACTIVE = """
            SELECT v.id AS vehicle_id, v.identification, t.direction,
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
                        result.getString("identification"),
                        result.getString("direction"),
                        nullableDouble(result, "latitude"),
                        nullableDouble(result, "longitude"),
                        recordedAt == null ? null : recordedAt.toInstant().toString()));
            }
        }
        return vehicles;
    }

    private Double nullableDouble(ResultSet result, String column) throws SQLException {
        double value = result.getDouble(column);
        return result.wasNull() ? null : value;
    }
}
