package de.t0bx.sentiencefriends.proxy.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.t0bx.sentiencefriends.proxy.ProxyPlugin;
import de.t0bx.sentiencefriends.proxy.friends.FriendsData;
import de.t0bx.sentiencefriends.proxy.friends.FriendsManager;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
        UUID uuid = event.getUniqueId();
        this.friendsManager.exists(uuid).thenAccept(exists -> {
            if (!exists) {
                this.friendsManager.create(uuid);
                return;
            }

            this.friendsManager.loadFriends(uuid).thenAccept(loaded -> {
                FriendsData friendsData = this.friendsManager.get(uuid);
                StringBuilder nameRefresh = new StringBuilder();
                for (UUID friends : friendsData.getFriends().keySet()) {
                    this.proxyServer.getPlayer(friends).ifPresent(friend -> {
                        FriendsData friendData = this.friendsManager.get(friends);

                        nameRefresh.append(friend.getUsername()).append(",").append(friend.getUniqueId()).append(";");
                        if (!friendData.getSettings().isNotificationsEnabled()) return;

                        friend.sendMessage(this.miniMessage.deserialize(this.prefix + "<green>" + event.getUsername() + " is now online."));
                    });
                }

                if (nameRefresh.isEmpty()) return;

                CompletableFuture.runAsync(() -> {
                    String[] names = nameRefresh.substring(0, nameRefresh.length() - 1).split(";");
                    for (String name : names) {
                        String[] nameParts = name.split(",");
                        if (nameParts.length != 2) continue;

                        friendsData.getFriends().get(UUID.fromString(nameParts[1])).setCachedName(nameParts[0]);
                    }

                    //TODO batch Update for faster name lookup
                });
            });
        });
    }
}
