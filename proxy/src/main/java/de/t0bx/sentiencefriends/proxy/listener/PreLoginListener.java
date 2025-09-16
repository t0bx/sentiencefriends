package de.t0bx.sentiencefriends.proxy.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import de.t0bx.sentiencefriends.proxy.friends.FriendsManager;

import java.util.UUID;

public class PreLoginListener {

    private final FriendsManager friendsManager;

    public PreLoginListener(FriendsManager friendsManager) {
        this.friendsManager = friendsManager;
    }

    @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        this.friendsManager.exists(uuid).thenAccept(exists -> {
            if (!exists) {
                this.friendsManager.create(uuid);
            }

            this.friendsManager.loadFriends(uuid);
        });
    }
}
