package br.edu.maua.model;

public class ScheduleEntry {

    private final long vehicleId;
    private final String departureTime;
    private final String direction;
    private final String dayType;

    public ScheduleEntry(long vehicleId, String departureTime, String direction, String dayType) {
        this.vehicleId = vehicleId;
        this.departureTime = departureTime;
        this.direction = direction;
        this.dayType = dayType;
    }
}
