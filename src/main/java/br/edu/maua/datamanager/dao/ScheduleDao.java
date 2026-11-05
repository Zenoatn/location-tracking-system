package br.edu.maua.datamanager.dao;

import br.edu.maua.datamanager.DatabaseConnection;
import br.edu.maua.model.ScheduleEntry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ScheduleDao {

    public List<ScheduleEntry> findActive(String direction, String dayType) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT vehicle_id, departure_time, direction, day_type
                FROM schedules
                WHERE active = TRUE
                """);
        List<String> parameters = new ArrayList<>();
        if (direction != null) {
            sql.append(" AND direction = ?");
            parameters.add(direction);
        }
        if (dayType != null) {
            sql.append(" AND day_type = ?");
            parameters.add(dayType);
        }
        sql.append(" ORDER BY departure_time, direction");

        List<ScheduleEntry> schedules = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int index = 0; index < parameters.size(); index++) {
                statement.setString(index + 1, parameters.get(index));
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    schedules.add(new ScheduleEntry(
                            result.getLong("vehicle_id"),
                            result.getTime("departure_time").toString(),
                            result.getString("direction"),
                            result.getString("day_type")));
                }
            }
        }
        return schedules;
    }
}
