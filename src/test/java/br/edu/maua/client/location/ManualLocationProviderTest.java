package br.edu.maua.client.location;

import org.junit.Test;
import static org.junit.Assert.*;

public class ManualLocationProviderTest {
    @Test public void startsWithoutInventedCoordinatesAndStoresExplicitInput() {
        ManualLocationProvider provider = new ManualLocationProvider();
        assertTrue(provider.currentLocation().isEmpty());
        provider.update(-23.6, -46.6);
        assertEquals(-23.6, provider.currentLocation().orElseThrow().latitude(), 0.000001);
        assertEquals("Coordenada informada manualmente", provider.sourceName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsCoordinatesOutsideEarth() { new ManualLocationProvider().update(100, 0); }
}
