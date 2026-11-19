package br.edu.maua.client.location;

import java.util.Optional;

/** Location source used by the driver client; implementations must identify simulated/manual data. */
public interface LocationProvider {
    Optional<Coordinates> currentLocation();
    String sourceName();

    record Coordinates(double latitude, double longitude) { }
}
