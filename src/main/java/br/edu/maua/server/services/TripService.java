package br.edu.maua.server.services;

import br.edu.maua.datamanager.dao.TripDao;
import br.edu.maua.model.UserSession;
import java.sql.SQLException;
import java.time.Instant;
import java.util.OptionalLong;

public class TripService {

    private final TripDao tripDao;
    private final SessionRegistry sessionRegistry;

    public TripService() {
        this(new TripDao(), SessionRegistry.getInstance());
    }

    TripService(TripDao tripDao, SessionRegistry sessionRegistry) {
        this.tripDao = tripDao;
        this.sessionRegistry = sessionRegistry;
    }

    public StartedTrip start(String token, long vehicleId, String direction)
            throws SQLException, OperationException {
        UserSession session = requireDriver(token);
        if (vehicleId <= 0 || direction == null || direction.isBlank() || direction.length() > 50) {
            throw new OperationException("VALIDATION_ERROR", "vehicleId ou direction inválido.");
        }
        TripDao.StartResult result = tripDao.start(session.getUserId(), vehicleId, direction.trim());
        return switch (result.status()) {
            case STARTED -> new StartedTrip(result.tripId(), result.startedAt());
            case DRIVER_UNAVAILABLE -> throw new OperationException("UNAUTHORIZED", "A conta não está ativa.");
            case VEHICLE_UNAVAILABLE -> throw new OperationException("NOT_FOUND", "Veículo não encontrado.");
            case ACTIVE_TRIP_EXISTS -> throw new OperationException("CONFLICT",
                    "O motorista ou veículo já possui uma viagem ativa.");
        };
    }

    public void updateLocation(String token, long tripId, double latitude, double longitude,
            Instant recordedAt) throws SQLException, OperationException {
        UserSession session = requireDriver(token);
        if (tripId <= 0 || !Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new OperationException("VALIDATION_ERROR", "tripId ou coordenadas inválidos.");
        }
        if (recordedAt == null) {
            recordedAt = Instant.now();
        }
        if (!tripDao.recordLocation(session.getUserId(), tripId, latitude, longitude, recordedAt)) {
            throw new OperationException("CONFLICT", "A viagem não está ativa para este motorista.");
        }
    }

    public Instant end(String token, long tripId) throws SQLException, OperationException {
        UserSession session = requireDriver(token);
        if (tripId <= 0) {
            throw new OperationException("VALIDATION_ERROR", "tripId deve ser positivo.");
        }
        TripDao.EndResult result = tripDao.end(session.getUserId(), tripId);
        if (!result.ended()) {
            throw new OperationException("CONFLICT", "A viagem não está ativa para este motorista.");
        }
        return result.endedAt();
    }

    public Long findActiveTrip(String token) throws SQLException, OperationException {
        UserSession session = requireDriver(token);
        OptionalLong activeTrip = tripDao.findActiveTrip(session.getUserId());
        return activeTrip.isPresent() ? activeTrip.getAsLong() : null;
    }

    private UserSession requireDriver(String token) throws OperationException {
        UserSession session = sessionRegistry.find(token);
        if (session == null) {
            throw new OperationException("UNAUTHORIZED", "Autenticação necessária ou sessão expirada.");
        }
        if (!"DRIVER".equals(session.getRole())) {
            throw new OperationException("FORBIDDEN", "Esta operação é restrita a motoristas.");
        }
        return session;
    }

    public record StartedTrip(long tripId, Instant startedAt) { }
}
