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
        String sql = "SELECT id, name, email, password_hash, role "
                + "FROM users WHERE email = ? AND active = TRUE";
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
                        result.getString("role")));
            }
        }
    }
}
