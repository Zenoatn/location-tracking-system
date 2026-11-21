package br.edu.maua.model;

public class ActiveVehicle {

    private final long vehicleId;
    private final String identification;
    private final String type;
    private final String direction;
    private final Double latitude;
    private final Double longitude;
    private final String recordedAt;

    public ActiveVehicle(long vehicleId, String identification, String type, String direction, Double latitude, Double longitude, String recordedAt) {
        this.vehicleId = vehicleId;
        this.identification = identification;
        this.type = type;
        this.direction = direction;
        this.latitude = latitude;
        this.longitude = longitude;
        this.recordedAt = recordedAt;
    }

    public long getVehicleId(){ 
        return vehicleId; 
    }

    public String getIdentification(){ 
        return identification; 
    }

    public String getType(){ 
        return type; 
    }

    public String getDirection(){ 
        return direction; 
    }

    public Double getLatitude(){ 
        return latitude; 
    }
    
    public Double getLongitude(){ 
        return longitude; 
    }

    public String getRecordedAt(){ 
        return recordedAt; 
    }
}
