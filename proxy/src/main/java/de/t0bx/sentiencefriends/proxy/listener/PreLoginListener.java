package de.t0bx.sentiencefriends.proxy.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import de.t0bx.sentiencefriends.proxy.ProxyPlugin;
import de.t0bx.sentiencefriends.proxy.friends.FriendsData;
import de.t0bx.sentiencefriends.proxy.friends.FriendsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PreLoginListener {

    private final ProxyPlugin plugin;
    private final ProxyServer proxyServer;
    private final FriendsManager friendsManager;
    private final MiniMessage miniMessage;
    private final String prefix;

    public PreLoginListener(ProxyPlugin plugin, FriendsManager friendsManager) {
        this.plugin = plugin;
        this.friendsManager = friendsManager;
        this.proxyServer = plugin.getProxyServer();
        this.miniMessage = MiniMessage.miniMessage();
        this.prefix = plugin.getPrefix();
    }

    @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        final UUID uuid = event.getUniqueId();
        if (uuid == null) return;

        this.friendsManager.exists(uuid).thenAccept(exists -> {
            if (!exists) {
                this.friendsManager.create(uuid);
                return;
            }

            this.friendsManager.loadFriends(uuid).thenAccept(loaded -> {
                final FriendsData self = this.friendsManager.get(uuid);
                if (self == null) return;

                final Set<UUID> friendIds = new HashSet<>(self.getFriends().keySet());
                if (friendIds.isEmpty()) return;

                final String name = event.getUsername();
                final Component msg = this.miniMessage.deserialize(this.prefix + "<green>" + name + " is now online.");

                this.proxyServer.getScheduler().buildTask(this.plugin, () -> {
                    for (UUID friendId : friendIds) {
                        this.proxyServer.getPlayer(friendId).ifPresent(player -> {
                            final FriendsData.Friend selfRelation = self.getFriends().getOrDefault(friendId, null);
                            if (selfRelation != null) {
                                selfRelation.setOnline(true);
                            }

                            final FriendsData friendData = this.friendsManager.get(friendId);
                            if (friendData == null) return;

                            final FriendsData.Friend relation = friendData.getFriends().get(uuid);
                            if (relation != null) {
                                relation.setCachedName(name);
                                relation.setOnline(true);
                            }

                            if (!friendData.getSettings().isNotificationsEnabled()) return;

                            player.sendMessage(msg);
                        });
                    }
                }).schedule();

                final String sql = "UPDATE friends_data SET cached_name = ? WHERE uuid_friend = ?;";
                this.plugin.getMySQLManager().updateAsync(sql, name, uuid.toString());
            }).exceptionally(ex -> {
                this.plugin.getLogger().warn("Failed to load friends for {}", uuid, ex);
                return null;
            });
        });
    }
}
