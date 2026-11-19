package br.edu.maua.datamanager.dao;

import br.edu.maua.datamanager.DatabaseConnection;
import br.edu.maua.model.UserCredential;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UserDao {

    public void createDriver(String name, String email, String passwordHash) throws SQLException {
        String sql = "INSERT INTO users (name, email, password_hash, role, active) "
                + "VALUES (?, ?, ?, 'DRIVER', TRUE)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, email);
            statement.setString(3, passwordHash);
            statement.executeUpdate();
        }
    }

    public Optional<UserCredential> findActiveByEmail(String email) throws SQLException {
        String sql = "SELECT u.id, u.name, u.email, u.password_hash, u.role, u.current_status, "
                + "u.current_vehicle_id, u.waiting_location, v.identification vehicle_identification, v.type vehicle_type "
                + "FROM users u LEFT JOIN vehicles v ON v.id = u.current_vehicle_id "
                + "WHERE u.email = ? AND u.active = TRUE";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(new UserCredential(
                        result.getLong("id"), result.getString("name"),
                        result.getString("email"), result.getString("password_hash"),
                        result.getString("role"), result.getString("current_status"),
                        result.getObject("current_vehicle_id") == null ? null
                                : result.getLong("current_vehicle_id"),
                        result.getString("waiting_location"), result.getString("vehicle_identification"),
                        result.getString("vehicle_type")));
            }
        }
    }
}
