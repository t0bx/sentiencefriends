package de.t0bx.sentiencefriends.lobby.listener;

import de.t0bx.sentiencefriends.api.network.packets.RequestFriendsPacket;
import de.t0bx.sentiencefriends.lobby.LobbyPlugin;
import de.t0bx.sentiencefriends.lobby.netty.NettyManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class PlayerJoinListener implements Listener {

    private final NettyManager nettyManager;

    public PlayerJoinListener(NettyManager nettyManager) {
        this.nettyManager = nettyManager;
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        final Player player = event.getPlayer();

        this.nettyManager.getChannel().writeAndFlush(
                new RequestFriendsPacket(LobbyPlugin.getInstance().getChannelName(), player.getUniqueId())
        );
    }
}
