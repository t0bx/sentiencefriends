package de.t0bx.sentiencefriends.proxy.friends;

import de.t0bx.sentiencefriends.proxy.ProxyPlugin;
import de.t0bx.sentiencefriends.proxy.database.IMySQLManager;
import org.slf4j.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class FriendsManager {

    private final Map<UUID, FriendsData> cachedFriends = new ConcurrentHashMap<>();
    private final IMySQLManager mySQLManager;
    private final ProxyPlugin plugin;
    private final Logger logger;

    public FriendsManager(ProxyPlugin plugin) {
        this.plugin = plugin;
        this.mySQLManager = plugin.getMySQLManager();
        this.logger = plugin.getLogger();
    }

    public CompletableFuture<Void> loadFriends(UUID uuid) {
        return this.mySQLManager.transactionAsync(connection -> {
            FriendsData friendsData = new FriendsData(uuid);

            final String query = """
                    SELECT 'friend' AS dataset,
                                         uuid_friend AS id,
                                         cached_name AS cached_name,
                                         since,
                                         last_online,
                                         favorite,
                                         NULL AS req_type,
                                         NULL AS friend_requests_enabled,
                                         NULL AS notifications_enabled,
                                         NULL AS jump_enabled
                                  FROM friends_data
                                  WHERE uuid_player = ?
                    
                                  UNION ALL
                    
                                  SELECT 'request' AS dataset,
                                         CASE WHEN uuid_sender = ? THEN uuid_receiver ELSE uuid_sender END AS id,
                                         NULL AS cached_name,
                                         NULL AS since,
                                         NULL AS last_online,
                                         NULL AS favorite,
                                         CASE WHEN uuid_sender = ? THEN 'outgoing' ELSE 'incoming' END AS req_type,
                                         NULL AS friend_requests_enabled,
                                         NULL AS notifications_enabled,
                                         NULL AS jump_enabled
                                  FROM friends_requests
                                  WHERE uuid_sender = ? OR uuid_receiver = ?
                    
                                  UNION ALL
                    
                                  SELECT 'settings' AS dataset,
                                         NULL AS id,
                                         NULL AS cached_name,
                                         NULL AS since,
                                         NULL AS last_online,
                                         NULL AS favorite,
                                         NULL AS req_type,
                                         friend_requests_enabled,
                                         notifications_enabled,
                                         jump_enabled
                                  FROM friends_settings
                                  WHERE uuid = ?;
                    """;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, uuid.toString()); // friends_data
                statement.setString(2, uuid.toString()); // requests (uuid_sender=?)
                statement.setString(3, uuid.toString()); // req_type check
                statement.setString(4, uuid.toString()); // requests WHERE uuid_sender=?
                statement.setString(5, uuid.toString()); // requests WHERE uuid_receiver=?
                statement.setString(6, uuid.toString()); // settings

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        String dataset = resultSet.getString("dataset");

                        switch (dataset) {
                            case "friend" -> {
                                FriendsData.Friend friend = new FriendsData.Friend(
                                        UUID.fromString(resultSet.getString("id")),
                                        resultSet.getString("cached_name"),
                                        resultSet.getLong("since")
                                );

                                friend.setLastOnline(resultSet.getLong("last_online"));
                                friend.setFavorite(resultSet.getBoolean("favorite"));
                                friendsData.getFriends().put(friend.getUuid(), friend);
                            }

                            case "request" -> {
                                UUID requestId = UUID.fromString(resultSet.getString("id"));
                                String type = resultSet.getString("req_type");
                                if (type.equalsIgnoreCase("incoming")) {
                                    friendsData.getIncomingRequests().add(requestId);
                                } else {
                                    friendsData.getOutgoingRequests().add(requestId);
                                }
                            }

                            case "settings" -> {
                                friendsData.getSettings().setRequestsEnabled(resultSet.getBoolean("friend_requests_enabled"));
                                friendsData.getSettings().setNotificationsEnabled(resultSet.getBoolean("notifications_enabled"));
                                friendsData.getSettings().setJumpEnabled(resultSet.getBoolean("jump_enabled"));
                            }
                        }
                    }
                }

                this.cachedFriends.put(uuid, friendsData);
            } catch (SQLException exception) {
                this.logger.error("Error while loading friends data for player {}", uuid, exception);
            }
        });
    }

    public FriendsData get(UUID uuid) {
        return this.cachedFriends.getOrDefault(uuid, null);
    }

    public CompletableFuture<Boolean> exists(UUID uuid) {
        if (this.cachedFriends.containsKey(uuid)) return CompletableFuture.completedFuture(true);

        return this.mySQLManager.existsAsync("friends_settings", "uuid = ?", uuid.toString());
    }

    public void create(UUID uuid) {
        this.cachedFriends.put(uuid, new FriendsData(uuid));

        this.mySQLManager.updateAsync("""
                INSERT INTO friends_settings (uuid)
                VALUES (?)
                """, uuid.toString());
    }

    public CompletableFuture<Boolean> doesFriendAcceptRequests(UUID uuid) {
        if (this.cachedFriends.containsKey(uuid)) {
            return CompletableFuture.completedFuture(this.cachedFriends.get(uuid).getSettings().isRequestsEnabled());
        }

        final String query = "SELECT friend_requests_enabled FROM friends_settings WHERE uuid = ?";
        return this.mySQLManager.queryAsync(query, resultSet -> {
            try {
                return resultSet.getBoolean("friend_requests_enabled");
            } catch (SQLException exception) {
                this.logger.error("Error while executing async query", exception);
                return null;
            }
        }, uuid.toString()).thenApply(List::getFirst);
    }
}
