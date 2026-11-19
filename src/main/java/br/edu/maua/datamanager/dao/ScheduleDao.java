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
                SELECT s.vehicle_id, v.identification, v.type,
                       s.departure_time, s.direction, s.day_type
                FROM schedules s JOIN vehicles v ON v.id = s.vehicle_id
                WHERE s.active = TRUE
                """);
        List<String> parameters = new ArrayList<>();
        if (direction != null) {
            if ("CAMPUS".equals(direction) || "ESTACAO_TO_IMT".equals(direction) || "TO_IMT".equals(direction)) {
                sql.append(" AND s.direction IN ('CAMPUS', 'ESTACAO_TO_IMT', 'TO_IMT')");
            } else if ("CENTRO".equals(direction) || "IMT_TO_ESTACAO".equals(direction) || "TO_ESTACAO".equals(direction)) {
                sql.append(" AND s.direction IN ('CENTRO', 'IMT_TO_ESTACAO', 'TO_ESTACAO')");
            } else {
                sql.append(" AND s.direction = ?");
                parameters.add(direction);
            }
        }
        if (dayType != null) {
            sql.append(" AND s.day_type = ?");
            parameters.add(dayType);
        }
        sql.append(" ORDER BY s.departure_time, s.direction");

        List<ScheduleEntry> schedules = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int index = 0; index < parameters.size(); index++) {
                statement.setString(index + 1, parameters.get(index));
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    schedules.add(new ScheduleEntry(
                            result.getLong("vehicle_id"), result.getString("identification"),
                            result.getString("type"),
                            result.getTime("departure_time").toString(),
                            result.getString("direction"),
                            result.getString("day_type")));
                }
            }
        }
        return schedules;
    }
}
