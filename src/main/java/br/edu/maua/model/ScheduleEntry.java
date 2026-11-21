package br.edu.maua.model;

public class ScheduleEntry {

    private final long vehicleId;
    private final String identification;
    private final String type;
    private final String departureTime;
    private final String direction;
    private final String dayType;

    public ScheduleEntry(long vehicleId, String identification, String type, String departureTime, String direction, String dayType) {
        this.vehicleId = vehicleId;
        this.identification = identification;
        this.type = type;
        this.departureTime = departureTime;
        this.direction = direction;
        this.dayType = dayType;
    }
}
