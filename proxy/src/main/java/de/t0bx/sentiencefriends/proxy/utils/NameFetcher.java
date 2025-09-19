package de.t0bx.sentiencefriends.proxy.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * A utility class that retrieves player names based on UUIDs from either
 * the Minecraft Java Edition API or the Bedrock Edition API.
 *
 * It performs caching for efficient repeated lookups and supports
 * asynchronous retrieval for better performance in multi-threaded environments.
 */
public class NameFetcher {
    private static final String MINECRAFT_API_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private static final String XBOX_API_URL = "https://mcprofile.io/api/v1/bedrock/fuid/";
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final ConcurrentHashMap<UUID, String> UUID_NAME_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Boolean> BEDROCK_PLAYER_CACHE = new ConcurrentHashMap<>();

    /**
     * Retrieves the name associated with the given UUID. The method checks if the name is
     * cached, and if not, attempts to fetch the name either for a Java or Bedrock player.
     *
     * @param uuid The unique identifier of the player whose name is to be retrieved. Cannot be null.
     * @return The name of the player as a String. A dot (".") prefix will be added to names for Bedrock players.
     * @throws IllegalArgumentException If the provided UUID is null.
     * @throws Exception If an error occurs during the retrieval of*/
    public static String getName(UUID uuid) throws Exception {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID darf nicht null sein");
        }

        if (UUID_NAME_CACHE.containsKey(uuid)) {
            String name = UUID_NAME_CACHE.get(uuid);
            boolean isBedrock = BEDROCK_PLAYER_CACHE.getOrDefault(uuid, false);
            return isBedrock ? "." + name : name;
        }

        try {
            return fetchJavaName(uuid);
        } catch (Exception e) {
            return fetchBedrockName(uuid);
        }
    }

    /**
     * Retrieves the name associated with the given UUID asynchronously.
     * This method executes the name retrieval process in a separate thread using the configured executor.
     *
     * @param uuid the UUID for which the name should be retrieved; must not be null
     * @return a CompletableFuture that will complete with the name associated with the given UUID,
     *         or complete exceptionally if an error occurs during name retrieval
     */
    public static CompletableFuture<String> getNameAsync(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getName(uuid);
            } catch (Exception e) {
                throw new RuntimeException("Fehler beim Abrufen des Namens für UUID " + uuid, e);
            }
        }, EXECUTOR);
    }

    public static CompletableFuture<List<String>> getNamesAsync(List<UUID> uuids) {
        if (uuids == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("UUID-Liste darf nicht null sein"));
        }

        List<CompletableFuture<String>> perUuid = uuids.stream()
                .map(uuid -> {
                    if (uuid == null) {
                        return CompletableFuture.<String>completedFuture(null);
                    }
                    return getNameAsync(uuid).exceptionally(ex -> null);
                })
                .toList();

        CompletableFuture<Void> all = CompletableFuture.allOf(perUuid.toArray(new CompletableFuture[0]));

        return all.thenApply(v ->
                perUuid.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Fetches the name of a Java edition Minecraft player based on their UUID.
     *
     * The method sends a HTTP GET request to the Minecraft API to retrieve the
     * name associated with the specified UUID. If the request is successful,
     * the player's name is cached for future use. In case of an error, an
     * exception will be thrown.
     *
     * @param uuid the UUID of the Java edition Minecraft player
     * @return the name of the Java edition Minecraft player
     * @throws Exception if there is an error during the HTTP request or response parsing
     */
    private static String fetchJavaName(UUID uuid) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(MINECRAFT_API_URL + uuid.toString().replace("-", "")).openConnection();
        connection.setRequestMethod("GET");

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                JsonObject response = JsonParser.parseReader(reader).getAsJsonObject();
                String name = response.get("name").getAsString();

                UUID_NAME_CACHE.put(uuid, name);
                BEDROCK_PLAYER_CACHE.put(uuid, false);

                return name;
            }
        } else {
            throw new Exception("Konnte Namen für Java-Spieler nicht abrufen: HTTP " + connection.getResponseCode());
        }
    }

    /**
     * Fetches the Bedrock gamertag of a player based on their UUID by making an HTTP request to the Xbox API.
     * Caches the result for future use and marks the player as a Bedrock player in the cache.
     *
     * @param uuid the unique identifier (UUID) of the player whose Bedrock gamertag is to be fetched
     * @return the Bedrock gamertag of the player prefixed with a dot (e.g., ".gamertag")
     * @throws Exception if the request fails or the response code is not HTTP 200 (OK)
     */
    private static String fetchBedrockName(UUID uuid) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(XBOX_API_URL + uuid.toString()).openConnection();
        connection.setRequestMethod("GET");

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                JsonObject response = JsonParser.parseReader(reader).getAsJsonObject();
                String name = response.get("gamertag").getAsString();

                UUID_NAME_CACHE.put(uuid, name);
                BEDROCK_PLAYER_CACHE.put(uuid, true);

                return "." + name;
            }
        } else {
            throw new Exception("Konnte Namen für Bedrock-Spieler nicht abrufen: HTTP " + connection.getResponseCode());
        }
    }
}
