package de.t0bx.sentiencefriends.proxy.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import de.t0bx.sentiencefriends.proxy.friends.FriendsManager;

import java.util.UUID;

public class PlayerDisconnectListener {

    private final FriendsManager friendsManager;

    public PlayerDisconnectListener(FriendsManager friendsManager) {
        this.friendsManager = friendsManager;
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();

        this.friendsManager.get(uuid).flushNow();
    }
}
