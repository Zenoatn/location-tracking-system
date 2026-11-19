
package br.edu.maua.common.protocol;

public class Protocol {

    public static final String REQUEST_ID = "requestId";
    public static final String OPERATION = "operation";
    public static final String DATA = "data";
    public static final String STATUS = "status";
    public static final String ERROR = "error";
    public static final String CODE = "code";
    public static final String MESSAGE = "message";

    public static final String INVALID_REQUEST = "INVALID_REQUEST";
    public static final String UNKNOWN_OPERATION = "UNKNOWN_OPERATION";
    public static final String NOT_IMPLEMENTED = "NOT_IMPLEMENTED";

    public static final String LOGIN = "LOGIN";
    public static final String START_TRIP = "START_TRIP";
    public static final String UPDATE_LOCATION = "UPDATE_LOCATION";
    public static final String GET_ACTIVE_VEHICLES = "GET_ACTIVE_VEHICLES";
    public static final String GET_SCHEDULE = "GET_SCHEDULE";
    public static final String END_TRIP = "END_TRIP";
    public static final String GET_AVAILABLE_VEHICLES = "GET_AVAILABLE_VEHICLES";
    public static final String DECLARE_WAIT = "DECLARE_WAIT";
    public static final String CANCEL_WAIT = "CANCEL_WAIT";
    public static final String GET_WAITING_DRIVERS = "GET_WAITING_DRIVERS";

    public static final String OK = "OK";
    public static final String ERROR_STATUS = "ERROR";

    private Protocol() {
        // Impede a criação de objetos desta classe.
    }
}
