package de.t0bx.sentiencefriends.proxy.friends;

import com.velocitypowered.api.proxy.Player;
import de.t0bx.sentiencefriends.proxy.ProxyPlugin;
import de.t0bx.sentiencefriends.proxy.deferred.DeferredSaver;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Data
public class FriendsData implements AutoCloseable {
    private static final long SAVE_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(5);

    private final UUID uuid;
    private final Settings settings;
    private final Map<UUID, Friend> friends;
    private final List<UUID> incomingRequests;
    private final List<UUID> outgoingRequests;

    private final DeferredSaver saver;

    public FriendsData(UUID uuid) {
        this.uuid = uuid;
        this.settings = new Settings();
        this.friends = new ConcurrentHashMap<>();
        this.incomingRequests = new ArrayList<>();
        this.outgoingRequests = new ArrayList<>();
        this.saver = new DeferredSaver(
                ProxyPlugin.getInstance(),
                SAVE_INTERVAL_MILLIS,
                "Friends-DeferredSaver-" + uuid
        );
    }

    @Data
    public static class Friend {
        private final UUID uuid;
        private final long since;
        private long lastOnline;
        private boolean favorite;

        public Friend(UUID uuid, long since) {
            this.uuid = uuid;
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
        String update = "INSERT INTO friends_requests (sender, receiver) VALUES (?, ?)";
        saver.enqueue(update, uuid.toString(), receiver.toString());
    }

    public void acceptRequest(UUID sender) {
        this.incomingRequests.remove(sender);

        String del = "DELETE FROM friends_requests WHERE uuid_sender = ? AND uuid_receiver = ?";
        saver.enqueue(del, sender.toString(), uuid.toString());

        String insert1 = "INSERT INTO friends_data (uuid_player, uuid_friend, since, last_online) VALUES (?, ?, ?, ?)";
        long now = System.currentTimeMillis();
        saver.enqueue(insert1, uuid.toString(), sender.toString(), now, now);

        String insert2 = "INSERT INTO friends_data (uuid_player, uuid_friend, since, last_online) VALUES (?, ?, ?, ?)";
        saver.enqueue(insert2, sender.toString(), uuid.toString(), now, now);
        this.friends.put(sender, new Friend(sender, now));

        Optional<Player> player = ProxyPlugin.getInstance().getProxyServer().getPlayer(sender);
        if (player.isEmpty()) return;

        FriendsData friendsData = ProxyPlugin.getInstance().getFriendsManager().get(sender);
        friendsData.getFriends().put(uuid, new Friend(uuid, now));
        friendsData.getOutgoingRequests().remove(sender);
    }

    public void declineRequest(UUID sender) {
        this.incomingRequests.remove(sender);
        String update = "DELETE FROM friends_requests WHERE uuid_sender = ? AND uuid_receiver = ?";
        saver.enqueue(update, sender.toString(), uuid.toString());
    }

    public void addFriend(UUID friend) {
        Friend friendData = new Friend(friend, System.currentTimeMillis());
        friends.put(friend, friendData);

        String insert = "INSERT INTO friends_data (uuid_player, uuid_friend, since, last_online) VALUES (?, ?, ?, ?)";
        saver.enqueue(insert, uuid.toString(), friend.toString(), friendData.getSince(), friendData.getLastOnline());
    }

    public void removeFriend(UUID friend) {
        Friend removed = friends.remove(friend);
        if (removed == null) return;

        String update = "DELETE FROM friends_data WHERE uuid_friend = ? AND uuid_player = ?";
        saver.enqueue(update, friend.toString(), uuid.toString());
    }

    public void setFavorite(UUID friend, boolean favorite) {
        Friend friendData = friends.get(friend);
        if (friendData == null) return;

        friendData.setFavorite(favorite);
        String update = "UPDATE friends_data SET favorite = ? WHERE uuid_friend = ? AND uuid_player = ?";
        saver.enqueue(update, friendData.isFavorite(), friend.toString(), uuid.toString());
    }

    public void updateLastOnline(UUID friend) {
        Friend friendData = friends.get(friend);
        if (friendData == null) return;

        long now = System.currentTimeMillis();
        friendData.setLastOnline(now);

        String update = "UPDATE friends_data SET last_online = ? WHERE uuid_friend = ? AND uuid_player = ?";
        saver.enqueue(update, now, friend.toString(), uuid.toString());
    }

    public void changeSetting(SettingType setting, boolean value) {
        switch (setting) {
            case JUMP -> settings.setJumpEnabled(value);
            case NOTIFICATIONS -> settings.setNotificationsEnabled(value);
            case REQUESTS -> settings.setRequestsEnabled(value);
        }

        String key = setting.getKey();
        String update = "UPDATE friends_settings SET " + key + " = ? WHERE uuid = ?";
        saver.enqueue(update, value, uuid.toString());
    }

    public void flushNow() {
        saver.flushNow();
    }

    @Override
    public void close() {
        saver.close();
    }
}
