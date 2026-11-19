package br.edu.maua.client.location;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** Holds an explicitly entered coordinate; it never generates or simulates a position. */
public final class ManualLocationProvider implements LocationProvider {
    private final AtomicReference<Coordinates> current = new AtomicReference<>();

    public void update(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180)
            throw new IllegalArgumentException("Coordenadas fora dos limites geográficos.");
        current.set(new Coordinates(latitude, longitude));
    }

    @Override public Optional<Coordinates> currentLocation() { return Optional.ofNullable(current.get()); }
    @Override public String sourceName() { return "Coordenada informada manualmente"; }
}
