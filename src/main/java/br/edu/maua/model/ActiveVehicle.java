package br.edu.maua.model;

public class ActiveVehicle {

    private final long vehicleId;
    private final String identification;
    private final String direction;
    private final Double latitude;
    private final Double longitude;
    private final String recordedAt;

    public ActiveVehicle(long vehicleId, String identification, String direction,
            Double latitude, Double longitude, String recordedAt) {
        this.vehicleId = vehicleId;
        this.identification = identification;
        this.direction = direction;
        this.latitude = latitude;
        this.longitude = longitude;
        this.recordedAt = recordedAt;
    }
}
