package br.edu.maua.server;

import br.edu.maua.model.ActiveVehicle;
import br.edu.maua.model.ScheduleEntry;
import br.edu.maua.model.VehicleOption;
import br.edu.maua.model.WaitingDriver;
import br.edu.maua.server.services.AuthService;
import br.edu.maua.server.services.OperationException;
import br.edu.maua.server.services.TransportService;
import br.edu.maua.server.services.TripService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class RequestHandlerTest {
    @Test public void loginReturnsOperationalStateAndActiveTrip() {
        RequestHandler handler = handler();
        JsonObject response = send(handler, "LOGIN", "{\"email\":\"driver@test.org\",\"password\":\"pw\"}");
        JsonObject data = response.getAsJsonObject("data");
        assertEquals("OK", response.get("status").getAsString());
        assertEquals("WAITING", data.get("currentStatus").getAsString());
        assertEquals(17L, data.get("currentVehicleId").getAsLong());
        assertEquals("IMT", data.get("waitingLocation").getAsString());
        assertEquals(42L, data.get("activeTripId").getAsLong());
    }

    @Test public void newReadOperationsExposeAvailableVehiclesAndWaitingDrivers() {
        RequestHandler handler = handler();
        JsonObject available = send(handler, "GET_AVAILABLE_VEHICLES", "{}");
        assertEquals("Circular 1", available.getAsJsonObject("data").getAsJsonArray("vehicles")
                .get(0).getAsJsonObject().get("identification").getAsString());
        JsonObject waiting = send(handler, "GET_WAITING_DRIVERS", "{\"location\":\"IMT\"}");
        assertEquals("IMT", waiting.getAsJsonObject("data").getAsJsonArray("drivers")
                .get(0).getAsJsonObject().get("waitingLocation").getAsString());
    }

    @Test public void waitOperationsRouteThroughServices() {
        FakeTrip trips = new FakeTrip();
        RequestHandler handler = new RequestHandler(new FakeTransport(), new FakeAuth(), trips);
        JsonObject wait = send(handler, "DECLARE_WAIT", "{\"token\":\"t\",\"vehicleId\":17,\"waitingLocation\":\"IMT\"}");
        JsonObject cancel = send(handler, "CANCEL_WAIT", "{\"token\":\"t\"}");
        assertEquals("OK", wait.get("status").getAsString());
        assertEquals("OK", cancel.get("status").getAsString());
        assertTrue(trips.waitDeclared);
        assertTrue(trips.waitCancelled);
    }

    @Test public void invalidWaitingLocationIsRejected() {
        JsonObject response = send(handler(), "GET_WAITING_DRIVERS", "{\"location\":\"MARS\"}");
        assertEquals("ERROR", response.get("status").getAsString());
        assertEquals("VALIDATION_ERROR", response.getAsJsonObject("error").get("code").getAsString());
    }

    private RequestHandler handler() { return new RequestHandler(new FakeTransport(), new FakeAuth(), new FakeTrip()); }
    private static JsonObject send(RequestHandler handler, String operation, String data) {
        return JsonParser.parseString(handler.process("{\"requestId\":\"test-1\",\"operation\":\"" + operation
                + "\",\"data\":" + data + "}")).getAsJsonObject();
    }

    private static final class FakeTransport extends TransportService {
        @Override public List<ActiveVehicle> getActiveVehicles() { return List.of(new ActiveVehicle(1,"Circular 1","ONIBUS","ESTACAO_TO_IMT",-23.5,-46.6,"now")); }
        @Override public List<ScheduleEntry> getSchedules(String direction, String dayType) { return List.of(); }
        @Override public List<VehicleOption> getAvailableVehicles() { return List.of(new VehicleOption(1,"Circular 1","ONIBUS")); }
        @Override public List<WaitingDriver> getWaitingDrivers(String location) { return List.of(new WaitingDriver(2,"Motorista","IMT",1,"Circular 1","ONIBUS")); }
    }
    private static final class FakeAuth extends AuthService {
        @Override public LoginSession login(String email, char[] password) {
            return new LoginSession(2,"Motorista","DRIVER","token","WAITING",17L,"IMT","Circular 2","VAN");
        }
    }
    private static final class FakeTrip extends TripService {
        boolean waitDeclared, waitCancelled;
        @Override public Long findActiveTrip(String token) { return 42L; }
        @Override public void declareWait(String token, long vehicleId, String location) { waitDeclared = true; }
        @Override public void cancelWait(String token) { waitCancelled = true; }
    }
}
