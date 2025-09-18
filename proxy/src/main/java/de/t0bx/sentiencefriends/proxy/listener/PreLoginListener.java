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
                for (Player players : ProxyPlugin.getInstance().getProxyServer().getAllPlayers()) {
                    if (!friendsData.getFriends().containsKey(players.getUniqueId())) continue;

                    FriendsData playerData = this.friendsManager.get(players.getUniqueId());
                    if (!playerData.getSettings().isNotificationsEnabled()) continue;

                    players.sendMessage(this.miniMessage.deserialize(this.prefix + "<green>" + event.getUsername() + " is now online."));
                }
            });
        });
    }
}
