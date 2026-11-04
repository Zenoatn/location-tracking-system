
package br.edu.maua.common.protocol;

public class Protocol {

    public static final String LOGIN = "LOGIN";
    public static final String START_TRIP = "START_TRIP";
    public static final String UPDATE_LOCATION = "UPDATE_LOCATION";
    public static final String GET_ACTIVE_VEHICLES = "GET_ACTIVE_VEHICLES";
    public static final String GET_SCHEDULE = "GET_SCHEDULE";
    public static final String END_TRIP = "END_TRIP";

    public static final String OK = "OK";
    public static final String ERROR = "ERROR";

    private Protocol() {
        // Impede a criação de objetos desta classe.
    }
}
