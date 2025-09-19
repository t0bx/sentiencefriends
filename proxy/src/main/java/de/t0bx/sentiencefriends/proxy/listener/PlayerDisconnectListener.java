package de.t0bx.sentiencefriends.proxy.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.t0bx.sentiencefriends.proxy.ProxyPlugin;
import de.t0bx.sentiencefriends.proxy.friends.FriendsData;
import de.t0bx.sentiencefriends.proxy.friends.FriendsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerDisconnectListener {

    private final ProxyPlugin plugin;
    private final ProxyServer proxyServer;
    private final MiniMessage miniMessage;
    private final FriendsManager friendsManager;
    private final String prefix;

    public PlayerDisconnectListener(ProxyPlugin plugin, FriendsManager friendsManager) {
        this.plugin = plugin;
        this.friendsManager = friendsManager;
        this.proxyServer = plugin.getProxyServer();
        this.miniMessage = MiniMessage.miniMessage();
        this.prefix = plugin.getPrefix();
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        final String name = player.getUsername();
        final long lastOnline = System.currentTimeMillis();

        final FriendsData self = this.friendsManager.get(uuid);
        if (self == null) return;

        final Set<UUID> friendIds = new HashSet<>(self.getFriends().keySet());
        if (friendIds.isEmpty()) return;

        final Component msg = this.miniMessage.deserialize(this.prefix + "<red>" + name + " is now offline.");

        for (UUID friendId : friendIds) {
            this.proxyServer.getPlayer(friendId).ifPresent(friend -> {
                final FriendsData.Friend selfRelation = self.getFriends().getOrDefault(friendId, null);
                if (selfRelation != null) {
                    selfRelation.setOnline(false);
                }

                final FriendsData friendData = this.friendsManager.get(friendId);
                if (friendData == null) return;

                FriendsData.Friend relation = friendData.getFriends().get(uuid);
                if (relation != null) {
                    relation.setOnline(false);
                    relation.setLastOnline(lastOnline);
                }

                if (!friendData.getSettings().isNotificationsEnabled()) return;
                friend.sendMessage(msg);
            });
        }

        final String sql = "UPDATE friends_data SET last_online = ? WHERE uuid_friend = ?;";
        try {
            if (plugin.getMySQLManager().getConnection().isClosed()) return;

            if (this.plugin.isShutdown()) {
                try {
                    this.plugin.getMySQLManager().update(sql, lastOnline, uuid.toString());
                } catch (SQLException exception) {
                    this.plugin.getLogger().warn("Failed to update last online for {}!", uuid, exception);
                }
            } else {
                this.plugin.getMySQLManager().updateAsync(sql, lastOnline, uuid.toString());
            }
        } catch (SQLException exception) {
            this.plugin.getLogger().warn("Failed to update last online for {}", uuid, exception);
        }
    }
}
