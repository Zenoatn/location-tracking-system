package br.edu.maua.model;

public record WaitingDriver(long userId, String name, String waitingLocation,
        long vehicleId, String identification, String type) { }
