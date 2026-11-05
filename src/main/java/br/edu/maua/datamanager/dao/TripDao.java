package br.edu.maua.datamanager.dao;

import br.edu.maua.datamanager.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.OptionalLong;

public class TripDao {

    public OptionalLong findActiveTrip(long driverId) throws SQLException {
        String sql = "SELECT id FROM trips WHERE driver_id = ? AND status = 'ACTIVE' "
                + "ORDER BY id DESC LIMIT 1";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, driverId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? OptionalLong.of(result.getLong("id")) : OptionalLong.empty();
            }
        }
    }

    public StartResult start(long driverId, long vehicleId, String direction) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!existsForUpdate(connection,
                        "SELECT id FROM users WHERE id = ? AND active = TRUE FOR UPDATE", driverId)) {
                    connection.rollback();
                    return new StartResult(StartStatus.DRIVER_UNAVAILABLE, 0, null);
                }
                if (!existsForUpdate(connection,
                        "SELECT id FROM vehicles WHERE id = ? AND active = TRUE FOR UPDATE", vehicleId)) {
                    connection.rollback();
                    return new StartResult(StartStatus.VEHICLE_UNAVAILABLE, 0, null);
                }
                String activeSql = "SELECT id FROM trips WHERE status = 'ACTIVE' "
                        + "AND (driver_id = ? OR vehicle_id = ?) LIMIT 1 FOR UPDATE";
                try (PreparedStatement active = connection.prepareStatement(activeSql)) {
                    active.setLong(1, driverId);
                    active.setLong(2, vehicleId);
                    try (ResultSet result = active.executeQuery()) {
                        if (result.next()) {
                            connection.rollback();
                            return new StartResult(StartStatus.ACTIVE_TRIP_EXISTS, 0, null);
                        }
                    }
                }

                Instant startedAt = Instant.now();
                String insertSql = "INSERT INTO trips "
                        + "(driver_id, vehicle_id, start_time, status, direction) "
                        + "VALUES (?, ?, ?, 'ACTIVE', ?)";
                long tripId;
                try (PreparedStatement insert = connection.prepareStatement(
                        insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    insert.setLong(1, driverId);
                    insert.setLong(2, vehicleId);
                    insert.setTimestamp(3, Timestamp.from(startedAt));
                    insert.setString(4, direction);
                    insert.executeUpdate();
                    try (ResultSet keys = insert.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("O MySQL não retornou o identificador da viagem.");
                        }
                        tripId = keys.getLong(1);
                    }
                }
                connection.commit();
                return new StartResult(StartStatus.STARTED, tripId, startedAt);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public boolean recordLocation(long driverId, long tripId, double latitude,
            double longitude, Instant recordedAt) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String activeTripSql = "SELECT t.id FROM trips t JOIN users u ON u.id = t.driver_id "
                        + "WHERE t.id = ? AND t.driver_id = ? AND t.status = 'ACTIVE' "
                        + "AND u.active = TRUE FOR UPDATE";
                if (!existsForUpdate(connection, activeTripSql, tripId, driverId)) {
                    connection.rollback();
                    return false;
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO locations (trip_id, latitude, longitude, recorded_at) VALUES (?, ?, ?, ?)")) {
                    insert.setLong(1, tripId);
                    insert.setDouble(2, latitude);
                    insert.setDouble(3, longitude);
                    insert.setTimestamp(4, Timestamp.from(recordedAt));
                    insert.executeUpdate();
                }
                connection.commit();
                return true;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public EndResult end(long driverId, long tripId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Instant endedAt = Instant.now();
                String updateSql = "UPDATE trips SET status = 'FINISHED', end_time = ? "
                        + "WHERE id = ? AND driver_id = ? AND status = 'ACTIVE' "
                        + "AND EXISTS (SELECT 1 FROM users WHERE id = ? AND active = TRUE)";
                try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                    update.setTimestamp(1, Timestamp.from(endedAt));
                    update.setLong(2, tripId);
                    update.setLong(3, driverId);
                    update.setLong(4, driverId);
                    if (update.executeUpdate() == 0) {
                        connection.rollback();
                        return new EndResult(false, null);
                    }
                }
                connection.commit();
                return new EndResult(true, endedAt);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private boolean existsForUpdate(Connection connection, String sql, long id) throws SQLException {
        return existsForUpdate(connection, sql, id, null);
    }

    private boolean existsForUpdate(Connection connection, String sql, long first, Long second)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, first);
            if (second != null) {
                statement.setLong(2, second);
            }
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    public enum StartStatus { STARTED, DRIVER_UNAVAILABLE, VEHICLE_UNAVAILABLE, ACTIVE_TRIP_EXISTS }

    public record StartResult(StartStatus status, long tripId, Instant startedAt) { }

    public record EndResult(boolean ended, Instant endedAt) { }
}
