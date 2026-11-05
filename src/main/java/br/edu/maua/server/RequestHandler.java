
package br.edu.maua.server;

import br.edu.maua.common.protocol.Protocol;
import br.edu.maua.server.services.AuthService;
import br.edu.maua.server.services.OperationException;
import br.edu.maua.server.services.TripService;
import br.edu.maua.server.services.TransportService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

public class RequestHandler {

    private static final Gson GSON = new Gson();
    private final TransportService transportService;
    private final AuthService authService;
    private final TripService tripService;

    public RequestHandler() {
        this(new TransportService(), new AuthService(), new TripService());
    }

    RequestHandler(TransportService transportService, AuthService authService, TripService tripService) {
        this.transportService = transportService;
        this.authService = authService;
        this.tripService = tripService;
    }

    public String process(String request) {
        JsonObject requestObject;
        try {
            requestObject = JsonParser.parseString(request).getAsJsonObject();
        } catch (JsonSyntaxException | IllegalStateException exception) {
            return error(null, Protocol.INVALID_REQUEST, "A requisição deve ser um objeto JSON válido.");
        }

        String requestId = stringField(requestObject, Protocol.REQUEST_ID);
        String operation = stringField(requestObject, Protocol.OPERATION);

        if (requestId == null || requestId.isBlank() || requestId.length() > 128
                || operation == null || operation.isBlank()
                || !requestObject.has(Protocol.DATA)
                || !requestObject.get(Protocol.DATA).isJsonObject()) {
            return error(requestId, Protocol.INVALID_REQUEST,
                    "Campos obrigatórios: requestId (string), operation (string) e data (objeto).");
        }

        JsonObject data = requestObject.getAsJsonObject(Protocol.DATA);
        try {
            return switch (operation) {
                case Protocol.GET_ACTIVE_VEHICLES -> activeVehicles(requestId);
                case Protocol.GET_SCHEDULE -> schedules(requestId, data);
                case Protocol.LOGIN -> login(requestId, data);
                case Protocol.START_TRIP -> startTrip(requestId, data);
                case Protocol.UPDATE_LOCATION -> updateLocation(requestId, data);
                case Protocol.END_TRIP -> endTrip(requestId, data);
                default -> error(requestId, Protocol.UNKNOWN_OPERATION, "Operação desconhecida.");
            };
        } catch (OperationException exception) {
            return error(requestId, exception.getCode(), exception.getMessage());
        } catch (java.sql.SQLException exception) {
            System.err.println("Falha ao consultar o banco de dados:");
            exception.printStackTrace(System.err);
            return error(requestId, "INTERNAL_ERROR", "Não foi possível concluir a consulta.");
        } catch (GeneralSecurityException exception) {
            System.err.println("Falha no serviço criptográfico:");
            exception.printStackTrace(System.err);
            return error(requestId, "INTERNAL_ERROR", "Não foi possível concluir a autenticação.");
        } catch (IllegalArgumentException exception) {
            return error(requestId, "VALIDATION_ERROR", "Um ou mais filtros da consulta são inválidos.");
        }
    }

    private String activeVehicles(String requestId) throws java.sql.SQLException {
        JsonObject data = new JsonObject();
        data.add("vehicles", GSON.toJsonTree(transportService.getActiveVehicles()).getAsJsonArray());
        return success(requestId, data);
    }

    private String schedules(String requestId, JsonObject requestData) throws java.sql.SQLException {
        String direction = optionalString(requestData, "direction");
        String dayType = optionalString(requestData, "dayType");
        if (direction != null && (direction.isBlank() || direction.length() > 50)) {
            return error(requestId, "VALIDATION_ERROR", "direction deve ter entre 1 e 50 caracteres.");
        }
        if (dayType != null && !java.util.Set.of("WEEKDAY", "SATURDAY", "SUNDAY", "HOLIDAY")
                .contains(dayType)) {
            return error(requestId, "VALIDATION_ERROR", "dayType deve ser um tipo de dia reconhecido.");
        }
        JsonArray schedules = GSON.toJsonTree(
                transportService.getSchedules(direction, dayType)).getAsJsonArray();
        JsonObject data = new JsonObject();
        data.add("schedules", schedules);
        return success(requestId, data);
    }

    private String login(String requestId, JsonObject requestData)
            throws java.sql.SQLException, GeneralSecurityException, OperationException {
        String email = requiredString(requestData, "email");
        String password = requiredString(requestData, "password");
        requestData.remove("password");
        if (email.length() > 150 || !email.contains("@")) {
            return error(requestId, "VALIDATION_ERROR", "Informe um e-mail válido.");
        }
        char[] secret = password.toCharArray();
        AuthService.LoginSession session = authService.login(email, secret);
        if (session == null) {
            return error(requestId, "INVALID_CREDENTIALS", "E-mail ou senha inválidos.");
        }
        JsonObject data = new JsonObject();
        data.addProperty("userId", session.userId());
        data.addProperty("name", session.name());
        data.addProperty("role", session.role());
        data.addProperty("token", session.token());
        Long activeTripId = "DRIVER".equals(session.role())
                ? tripService.findActiveTrip(session.token()) : null;
        if (activeTripId == null) {
            data.add("activeTripId", com.google.gson.JsonNull.INSTANCE);
        } else {
            data.addProperty("activeTripId", activeTripId);
        }
        return success(requestId, data);
    }

    private String startTrip(String requestId, JsonObject requestData)
            throws java.sql.SQLException, OperationException {
        TripService.StartedTrip trip = tripService.start(
                requiredString(requestData, "token"),
                requiredLong(requestData, "vehicleId"),
                requiredString(requestData, "direction"));
        JsonObject data = new JsonObject();
        data.addProperty("tripId", trip.tripId());
        data.addProperty("startedAt", trip.startedAt().toString());
        return success(requestId, data);
    }

    private String updateLocation(String requestId, JsonObject requestData)
            throws java.sql.SQLException, OperationException {
        Instant recordedAt = optionalInstant(requestData, "recordedAt");
        if (recordedAt == null) {
            recordedAt = Instant.now();
        }
        tripService.updateLocation(
                requiredString(requestData, "token"),
                requiredLong(requestData, "tripId"),
                requiredDouble(requestData, "latitude"),
                requiredDouble(requestData, "longitude"), recordedAt);
        JsonObject data = new JsonObject();
        data.addProperty("recorded", true);
        data.addProperty("recordedAt", recordedAt.toString());
        return success(requestId, data);
    }

    private String endTrip(String requestId, JsonObject requestData)
            throws java.sql.SQLException, OperationException {
        Instant endedAt = tripService.end(
                requiredString(requestData, "token"), requiredLong(requestData, "tripId"));
        JsonObject data = new JsonObject();
        data.addProperty("tripId", requiredLong(requestData, "tripId"));
        data.addProperty("endedAt", endedAt.toString());
        data.addProperty("status", "FINISHED");
        return success(requestId, data);
    }

    private String requiredString(JsonObject object, String name) {
        String value = optionalString(object, name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required.");
        }
        return value;
    }

    private long requiredLong(JsonObject object, String name) {
        if (!object.has(name) || !object.get(name).isJsonPrimitive()
                || !object.getAsJsonPrimitive(name).isNumber()) {
            throw new IllegalArgumentException(name + " must be an integer.");
        }
        try {
            return object.get(name).getAsBigDecimal().longValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be an integer.", exception);
        }
    }

    private double requiredDouble(JsonObject object, String name) {
        if (!object.has(name) || !object.get(name).isJsonPrimitive()
                || !object.getAsJsonPrimitive(name).isNumber()) {
            throw new IllegalArgumentException(name + " must be numeric.");
        }
        try {
            double value = new BigDecimal(object.get(name).getAsString()).doubleValue();
            if (!Double.isFinite(value)) {
                throw new NumberFormatException("not finite");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be numeric.", exception);
        }
    }

    private Instant optionalInstant(JsonObject object, String name) {
        String value = optionalString(object, name);
        if (value == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(name + " must be an ISO-8601 timestamp with offset.", exception);
        }
    }

    private String optionalString(JsonObject object, String name) {
        if (!object.has(name) || object.get(name).isJsonNull()) {
            return null;
        }
        if (!object.get(name).isJsonPrimitive() || !object.getAsJsonPrimitive(name).isString()) {
            throw new IllegalArgumentException(name + " must be a string");
        }
        return object.get(name).getAsString();
    }

    private String success(String requestId, JsonObject data) {
        JsonObject response = new JsonObject();
        response.addProperty(Protocol.REQUEST_ID, requestId);
        response.addProperty(Protocol.STATUS, Protocol.OK);
        response.add(Protocol.DATA, data);
        return response.toString();
    }

    private String stringField(JsonObject object, String name) {
        if (!object.has(name) || !object.get(name).isJsonPrimitive()
                || !object.getAsJsonPrimitive(name).isString()) {
            return null;
        }
        return object.get(name).getAsString();
    }

    private String error(String requestId, String code, String message) {
        JsonObject response = new JsonObject();
        if (requestId != null) {
            response.addProperty(Protocol.REQUEST_ID, requestId);
        }
        response.addProperty(Protocol.STATUS, Protocol.ERROR_STATUS);
        JsonObject error = new JsonObject();
        error.addProperty(Protocol.CODE, code);
        error.addProperty(Protocol.MESSAGE, message);
        response.add(Protocol.ERROR, error);
        return response.toString();
    }
}
