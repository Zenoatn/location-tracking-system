package br.edu.maua.model;

public class UserCredential {

    private final long id;
    private final String name;
    private final String email;
    private final String passwordHash;
    private final String role;
    private final String currentStatus;
    private final Long currentVehicleId;
    private final String waitingLocation;
    private final String currentVehicleIdentification;
    private final String currentVehicleType;

    public UserCredential(long id, String name, String email, String passwordHash, String role,
            String currentStatus, Long currentVehicleId, String waitingLocation,
            String currentVehicleIdentification, String currentVehicleType) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.currentStatus = currentStatus;
        this.currentVehicleId = currentVehicleId;
        this.waitingLocation = waitingLocation;
        this.currentVehicleIdentification = currentVehicleIdentification;
        this.currentVehicleType = currentVehicleType;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public String getCurrentStatus() { return currentStatus; }
    public Long getCurrentVehicleId() { return currentVehicleId; }
    public String getWaitingLocation() { return waitingLocation; }
    public String getCurrentVehicleIdentification() { return currentVehicleIdentification; }
    public String getCurrentVehicleType() { return currentVehicleType; }
}
