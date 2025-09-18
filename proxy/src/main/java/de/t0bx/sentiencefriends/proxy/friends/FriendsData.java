package de.t0bx.sentiencefriends.proxy.friends;

import com.velocitypowered.api.proxy.Player;
import de.t0bx.sentiencefriends.proxy.ProxyPlugin;
import de.t0bx.sentiencefriends.proxy.database.IMySQLManager;
import lombok.Data;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Data
public class FriendsData {
    private static final long SAVE_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(5);

    private final UUID uuid;
    private final Settings settings;
    private final Map<UUID, Friend> friends;
    private final List<UUID> incomingRequests;
    private final List<UUID> outgoingRequests;
    private final IMySQLManager mySQLManager;

    public FriendsData(UUID uuid) {
        this.uuid = uuid;
        this.settings = new Settings();
        this.friends = new ConcurrentHashMap<>();
        this.incomingRequests = new ArrayList<>();
        this.outgoingRequests = new ArrayList<>();
        this.mySQLManager = ProxyPlugin.getInstance().getMySQLManager();
    }

    @Data
    public static class Friend {
        private final UUID uuid;
        private String cachedName;
        private final long since;
        private long lastOnline;
        private boolean favorite;

        public Friend(UUID uuid, String cachedName, long since) {
            this.uuid = uuid;
            this.cachedName = cachedName;
            this.since = since;
            this.lastOnline = since;
        }
    }

    @Data
    public static class Settings {
        private boolean requestsEnabled = true;
        private boolean notificationsEnabled = true;
        private boolean jumpEnabled = true;
    }

    @Getter
    public enum SettingType {
        REQUESTS("friend_requests_enabled"),
        NOTIFICATIONS("notifications_enabled"),
        JUMP("jump_enabled");

        private final String key;
        SettingType(String key) { this.key = key; }
    }

    public void sendRequest(UUID receiver) {
        this.outgoingRequests.add(receiver);
        String update = "INSERT INTO friends_requests (uuid_sender, uuid_receiver) VALUES (?, ?)";
        this.mySQLManager.updateAsync(update, uuid.toString(), receiver.toString());

        Optional<Player> player = ProxyPlugin.getInstance().getProxyServer().getPlayer(receiver);
        if (player.isEmpty()) return;

        FriendsData friendsData = ProxyPlugin.getInstance().getFriendsManager().get(receiver);
        friendsData.getIncomingRequests().add(uuid);
    }

    public void acceptRequest(String playerName, UUID sender, String friendName) {
        this.incomingRequests.remove(sender);

        String del = "DELETE FROM friends_requests WHERE uuid_sender = ? AND uuid_receiver = ?";
        this.mySQLManager.updateAsync(del, sender.toString(), uuid.toString());

        String insert = "INSERT INTO friends_data (uuid_player, uuid_friend, cached_name, since, last_online) VALUES (?, ?, ?, ?, ?)";
        long now = System.currentTimeMillis();
        this.mySQLManager.updateAsync(insert, uuid.toString(), sender.toString(), friendName, now, now);
        this.mySQLManager.updateAsync(insert, sender.toString(), uuid.toString(), playerName, now, now);

        this.friends.put(sender, new Friend(sender, friendName, now));

        Optional<Player> player = ProxyPlugin.getInstance().getProxyServer().getPlayer(sender);
        if (player.isEmpty()) return;

        FriendsData friendsData = ProxyPlugin.getInstance().getFriendsManager().get(sender);
        friendsData.getFriends().put(uuid, new Friend(uuid, playerName, now));
        friendsData.getOutgoingRequests().remove(uuid);
    }

    public void declineRequest(UUID sender) {
        this.incomingRequests.remove(sender);
        String update = "DELETE FROM friends_requests WHERE uuid_sender = ? AND uuid_receiver = ?";
        this.mySQLManager.updateAsync(update, sender.toString(), uuid.toString());

        Optional<Player> player = ProxyPlugin.getInstance().getProxyServer().getPlayer(sender);
        if (player.isEmpty()) return;

        FriendsData friendsData = ProxyPlugin.getInstance().getFriendsManager().get(sender);
        friendsData.getOutgoingRequests().remove(uuid);
    }

    public void removeFriend(UUID friend) {
        Friend removed = friends.remove(friend);
        if (removed == null) return;

        String update = "DELETE FROM friends_data WHERE uuid_friend = ? AND uuid_player = ?";
        this.mySQLManager.updateAsync(update, friend.toString(), uuid.toString());

        String update2 = "DELETE FROM friends_data WHERE uuid_friend = ? AND uuid_player = ?";
        this.mySQLManager.updateAsync(update2, uuid.toString(), friend.toString());

        Optional<Player> player = ProxyPlugin.getInstance().getProxyServer().getPlayer(friend);
        if (player.isEmpty()) return;

        FriendsData friendsData = ProxyPlugin.getInstance().getFriendsManager().get(friend);
        friendsData.getFriends().remove(uuid);
    }

    public void setFavorite(UUID friend, boolean favorite) {
        Friend friendData = friends.get(friend);
        if (friendData == null) return;

        friendData.setFavorite(favorite);
        String update = "UPDATE friends_data SET favorite = ? WHERE uuid_friend = ? AND uuid_player = ?";
        this.mySQLManager.updateAsync(update, favorite, friend.toString(), uuid.toString());
    }

    public void updateLastOnline(UUID friend) {
        Friend friendData = friends.get(friend);
        if (friendData == null) return;

        long now = System.currentTimeMillis();
        friendData.setLastOnline(now);

        String update = "UPDATE friends_data SET last_online = ? WHERE uuid_friend = ? AND uuid_player = ?";
        this.mySQLManager.updateAsync(update, now, friend.toString(), uuid.toString());
    }

    public void changeSetting(SettingType setting, boolean value) {
        switch (setting) {
            case JUMP -> settings.setJumpEnabled(value);
            case NOTIFICATIONS -> settings.setNotificationsEnabled(value);
            case REQUESTS -> settings.setRequestsEnabled(value);
        }

        String key = setting.getKey();
        String update = "UPDATE friends_settings SET " + key + " = ? WHERE uuid = ?";
        this.mySQLManager.updateAsync(update, value, uuid.toString());
    }
}
