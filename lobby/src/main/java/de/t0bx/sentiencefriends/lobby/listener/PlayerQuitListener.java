package de.t0bx.sentiencefriends.lobby.listener;

import de.t0bx.sentiencefriends.lobby.friends.FriendsManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final FriendsManager friendsManager;

    public PlayerQuitListener(FriendsManager friendsManager) {
        this.friendsManager = friendsManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        this.friendsManager.getFriendsData().remove(player.getUniqueId());
    }
}
