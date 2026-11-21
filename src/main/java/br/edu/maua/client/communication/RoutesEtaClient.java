package br.edu.maua.client.communication;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;

/** Google Routes computeRoutes wrapper with a one-minute origin/destination cache. */
public final class RoutesEtaClient {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final long CACHE_MILLIS = 60_000;
    private static final Map<String, Cached> CACHE = new ConcurrentHashMap<>();

    public synchronized OptionalLong estimateMinutes(double latitude, double longitude, String destination) {


        String key = String.format(java.util.Locale.ROOT, "%.5f,%.5f>%s", latitude, longitude, destination);

        Cached cached = CACHE.get(key);


        if (cached != null && System.currentTimeMillis() - cached.at < CACHE_MILLIS){
            return OptionalLong.of(cached.minutes);
        }

        String apiKey = System.getenv("GOOGLE_MAPS_ROUTES_API_KEY");

        if (apiKey == null || apiKey.isBlank()){
            return OptionalLong.empty();
        }

        try {
            JsonObject body = new JsonObject();


            body.add("origin", point(latitude, longitude));
            body.add("destination", point(Double.parseDouble(destination.split(",")[0]), Double.parseDouble(destination.split(",")[1])));
            body.addProperty("travelMode", "DRIVE");
            body.addProperty("routingPreference", "TRAFFIC_AWARE");


            HttpRequest request = HttpRequest.newBuilder(URI.create("https://routes.googleapis.com/directions/v2:computeRoutes"))
                    .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
                    .header("X-Goog-Api-Key", apiKey).header("X-Goog-FieldMask", "routes.duration")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200){
                return OptionalLong.empty();
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

            if (!json.has("routes") || json.getAsJsonArray("routes").isEmpty()){
                return OptionalLong.empty();
            }

            String duration = json.getAsJsonArray("routes").get(0).getAsJsonObject().get("duration").getAsString();
            long seconds = Long.parseLong(duration.replace("s", ""));
            long minutes = Math.max(1, Math.round(seconds / 60.0));

            CACHE.put(key, new Cached(minutes, System.currentTimeMillis()));

            if (CACHE.size() > 1000){
                CACHE.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue().at > CACHE_MILLIS);
            }

            return OptionalLong.of(minutes);

        } catch (Exception ignored){
            return OptionalLong.empty();
        }
    }

    private JsonObject point(double latitude, double longitude) {
        JsonObject ll = new JsonObject(); 
        
        ll.addProperty("latitude", latitude);
        ll.addProperty("longitude", longitude);

        JsonObject location = new JsonObject();

        location.add("latLng", ll);

        JsonObject point = new JsonObject(); 
        
        point.add("location", location); 
        
        return point;
    }

    
    private record Cached(long minutes, long at) { }
}
