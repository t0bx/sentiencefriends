package de.t0bx.sentiencefriends.proxy.friends;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.t0bx.sentiencefriends.proxy.ProxyPlugin;
import de.t0bx.sentiencefriends.proxy.database.IMySQLManager;
import lombok.Data;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;

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
    private final ProxyServer proxyServer;

    public FriendsData(UUID uuid) {
        this.uuid = uuid;
        this.settings = new Settings();
        this.friends = new ConcurrentHashMap<>();
        this.incomingRequests = new ArrayList<>();
        this.outgoingRequests = new ArrayList<>();
        this.mySQLManager = ProxyPlugin.getInstance().getMySQLManager();
        this.proxyServer = ProxyPlugin.getInstance().getProxyServer();
    }

    @Data
    public static class Friend {
        private final UUID uuid;
        private String cachedName;
        private final long since;
        private long lastOnline;
        private boolean favorite;
        private boolean online;

        public Friend(UUID uuid, String cachedName, long since) {
            this.uuid = uuid;
            this.cachedName = cachedName;
            this.since = since;
            this.lastOnline = since;
        }

        public String getLastOnlineTime() {
            long diff = System.currentTimeMillis() - this.lastOnline;
            if (diff < 0) diff = 0;

            long seconds = diff / 1000;
            long days = seconds / 86_400;
            seconds %= 86_400;
            long hours = seconds / 3_600;
            seconds %= 3_600;
            long minutes = seconds / 60;
            seconds %= 60;

            StringBuilder sb = new StringBuilder();
            if (days > 0) sb.append(days).append("d ");
            if (hours > 0) sb.append(hours).append("h ");
            if (minutes > 0) sb.append(minutes).append("m ");
            if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");

            return sb.toString().trim();
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

        SettingType(String key) {
            this.key = key;
        }
    }

    public void sendRequest(UUID receiver) {
        this.outgoingRequests.add(receiver);
        String update = "INSERT INTO friends_requests (uuid_sender, uuid_receiver) VALUES (?, ?)";
        this.mySQLManager.updateAsync(update, uuid.toString(), receiver.toString());

        this.proxyServer.getPlayer(receiver).ifPresent(player -> {
            FriendsData friendsData = ProxyPlugin.getInstance().getFriendsManager().get(receiver);
            if (friendsData == null) return;

            friendsData.getIncomingRequests().add(uuid);
        });
    }

    public void acceptRequest(String playerName, UUID sender, String friendName) {
        this.incomingRequests.remove(sender);

        String del = "DELETE FROM friends_requests WHERE uuid_sender = ? AND uuid_receiver = ?";
        this.mySQLManager.updateAsync(del, sender.toString(), uuid.toString());

        String insert = "INSERT INTO friends_data (uuid_player, uuid_friend, cached_name, since, last_online) VALUES (?, ?, ?, ?, ?)";
        long now = System.currentTimeMillis();
        this.mySQLManager.updateAsync(insert, uuid.toString(), sender.toString(), friendName, now, now);
        this.mySQLManager.updateAsync(insert, sender.toString(), uuid.toString(), playerName, now, now);

        Friend selfFriend = new Friend(uuid, friendName, now);

        this.proxyServer.getPlayer(sender).ifPresent(player -> {
            FriendsData friendsData = ProxyPlugin.getInstance().getFriendsManager().get(sender);
            if (friendsData == null) return;

            Friend friend = new Friend(uuid, playerName, now);
            friend.setOnline(true);
            selfFriend.setOnline(true);

            friendsData.getFriends().put(uuid, friend);
            friendsData.getOutgoingRequests().remove(uuid);
            player.sendMessage(MiniMessage.miniMessage().deserialize(ProxyPlugin.getInstance().getPrefix() + "<green>You are now friends with " + playerName + "."));
        });

        this.friends.put(sender, selfFriend);
    }

    public void declineRequest(UUID sender) {
        this.incomingRequests.remove(sender);
        String update = "DELETE FROM friends_requests WHERE uuid_sender = ? AND uuid_receiver = ?";
        this.mySQLManager.updateAsync(update, sender.toString(), uuid.toString());

        this.proxyServer.getPlayer(sender).ifPresent(player -> {
            FriendsData friendsData = ProxyPlugin.getInstance().getFriendsManager().get(sender);
            if (friendsData == null) return;

            friendsData.getOutgoingRequests().remove(uuid);
        });
    }

    public void removeFriend(UUID friend) {
        Friend removed = friends.remove(friend);
        if (removed == null) return;

        String update = "DELETE FROM friends_data WHERE uuid_friend = ? AND uuid_player = ?";
        this.mySQLManager.updateAsync(update, friend.toString(), uuid.toString());

        String update2 = "DELETE FROM friends_data WHERE uuid_friend = ? AND uuid_player = ?";
        this.mySQLManager.updateAsync(update2, uuid.toString(), friend.toString());

        this.proxyServer.getPlayer(friend).ifPresent(player -> {
            FriendsData friendsData = ProxyPlugin.getInstance().getFriendsManager().get(friend);
            if (friendsData == null) return;

            friendsData.getFriends().remove(uuid);
        });
    }

    public void setFavorite(UUID friend, boolean favorite) {
        Friend friendData = friends.get(friend);
        if (friendData == null) return;

        friendData.setFavorite(favorite);
        String update = "UPDATE friends_data SET favorite = ? WHERE uuid_friend = ? AND uuid_player = ?";
        this.mySQLManager.updateAsync(update, favorite, friend.toString(), uuid.toString());
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
