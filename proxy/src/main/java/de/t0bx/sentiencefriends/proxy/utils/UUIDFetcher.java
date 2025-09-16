package de.t0bx.sentiencefriends.proxy.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A utility class for fetching UUIDs associated with Minecraft player names.
 * This class supports fetching UUIDs for both Java Edition and Bedrock Edition players.
 * Java Edition names are fetched directly through the Mojang API while Bedrock Edition
 * names are handled using an external API.
 *
 * The class provides both synchronous and asynchronous methods for fetching UUIDs.
 * It also maintains a cache to optimize and reduce repeated API calls for the same player names.
 *
 * Thread-safety:
 * - The class is thread-safe as it uses a {@link ConcurrentHashMap} for caching and an
 *   {@link ExecutorService} for asynchronous requests.
 *
 * Methods Summary:
 * - `getUUID(String name)` - Retrieves the UUID for a player name, supports both Java and Bedrock Edition.
 * - `getUUIDAsync(String name)` - Fetches the UUID for a name asynchronously with a {@link CompletableFuture}.
 */
public class UUIDFetcher {
    private static final String MINECRAFT_API_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String XBOX_API_URL = "https://mcprofile.io/api/v1/bedrock/gamertag/";
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final ConcurrentHashMap<String, UUID> NAME_UUID_CACHE = new ConcurrentHashMap<>();

    /**
     * Retrieves a UUID for the given player name. The name can represent
     * either a Java or a Bedrock player. If the name begins with a period ("."),
     * it is considered as a Bedrock player name. The method makes use of
     * caching to minimize API calls.
     *
     * @param name the player's name whose UUID is to be fetched; must not be null or empty.
     *             If it starts with ".", the name is treated as a Bedrock player's name.
     * @return the UUID associated with the given name.
     * @throws IllegalArgumentException if the name is null or empty.
     * @throws Exception if the UUID cannot be retrieved from the respective API.
     */
    public static UUID getUUID(String name) throws Exception {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name darf nicht null oder leer sein");
        }

        if (NAME_UUID_CACHE.containsKey(name)) {
            return NAME_UUID_CACHE.get(name);
        }

        boolean isBedrock = name.startsWith(".");
        String actualName = isBedrock ? name.substring(1) : name;

        if (isBedrock) {
            return fetchBedrockUUID(actualName);
        } else {
            return fetchJavaUUID(actualName);
        }
    }

    /**
     * Asynchronously retrieves a UUID for the given player name. The name can
     * represent either a Java or a Bedrock player. If the name begins with a
     * period ("."), it is considered a Bedrock player name. This method uses a
     * completable future to perform the operation in a non-blocking manner.
     *
     * @param name the player's name whose UUID is to be fetched; must not be null or empty.
     *             If it starts with ".", the name is treated as a Bedrock player's name.
     * @return a {@code CompletableFuture} containing the UUID associated with the given name.
     */
    public static CompletableFuture<UUID> getUUIDAsync(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getUUID(name);
            } catch (Exception e) {
                throw new RuntimeException("Fehler beim Abrufen der UUID für " + name, e);
            }
        }, EXECUTOR);
    }

    /**
     * Fetches the UUID of a Java Minecraft player using the Mojang API.
     * The method retrieves the UUID by making a GET request to the defined API URL
     * and formats the UUID from its raw representation.
     * The retrieved UUID is cached for future reference.
     *
     * @param name The name of the Java Minecraft player for which the UUID is to be fetched.
     *             Must not be null or empty.
     * @return The UUID associated with the given Java player name.
     * @throws Exception If the UUID could not be retrieved from the Mojang API
     *                   or in case of HTTP errors.
     */
    private static UUID fetchJavaUUID(String name) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(MINECRAFT_API_URL + name).openConnection();
        connection.setRequestMethod("GET");

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                JsonObject response = JsonParser.parseReader(reader).getAsJsonObject();
                String id = response.get("id").getAsString();

                UUID uuid = UUID.fromString(id.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
                NAME_UUID_CACHE.put(name, uuid);
                NAME_UUID_CACHE.put("." + name, uuid);

                return uuid;
            }
        } else {
            throw new Exception("Konnte UUID für Java-Spieler nicht abrufen: HTTP " + connection.getResponseCode());
        }
    }

    /**
     * Fetches the UUID of a Bedrock Minecraft player using the defined API URL.
     * The method sends a GET request to retrieve the UUID corresponding to the provided player name.
     * The retrieved UUID is cached for future reference.
     *
     * @param name The name of the Bedrock Minecraft player for which the UUID is to be fetched.
     *             Must not be null or empty.
     * @return The UUID associated with the given Bedrock player name.
     * @throws Exception If the UUID cannot be retrieved from the API or in case of HTTP errors.
     */
    private static UUID fetchBedrockUUID(String name) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(XBOX_API_URL + name).openConnection();
        connection.setRequestMethod("GET");

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                JsonObject response = JsonParser.parseReader(reader).getAsJsonObject();
                String id = response.get("floodgateuid").getAsString();

                UUID uuid = UUID.fromString(id);
                NAME_UUID_CACHE.put("." + name, uuid);

                return uuid;
            }
        } else {
            throw new Exception("Konnte UUID für Bedrock-Spieler nicht abrufen: HTTP " + connection.getResponseCode());
        }
    }
}
