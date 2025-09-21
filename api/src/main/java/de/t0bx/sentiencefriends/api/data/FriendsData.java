package de.t0bx.sentiencefriends.api.data;

import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class FriendsData {

    public final UUID uuid;
    public final Settings settings;
    public final Map<UUID, Friend> friends;
    public final List<UUID> incomingRequests;
    public final List<UUID> outgoingRequests;

    public FriendsData(UUID uuid) {
        this.uuid = uuid;
        this.settings = new Settings();
        this.friends = new ConcurrentHashMap<>();
        this.incomingRequests = new ArrayList<>();
        this.outgoingRequests = new ArrayList<>();
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
            return formatted(diff);
        }

        public String getSinceFormatted() {
            long diff = System.currentTimeMillis() - this.since;
            return formatted(diff);
        }

        private String formatted(long diff) {
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
}
