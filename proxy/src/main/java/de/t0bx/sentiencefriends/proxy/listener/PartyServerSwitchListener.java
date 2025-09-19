package de.t0bx.sentiencefriends.proxy.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import de.t0bx.sentiencefriends.proxy.ProxyPlugin;
import de.t0bx.sentiencefriends.proxy.party.PartyManager;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class PartyServerSwitchListener {

    private final ProxyPlugin plugin;
    private final PartyManager partyManager;
    private final String prefix;
    private final MiniMessage miniMessage;

    public PartyServerSwitchListener(ProxyPlugin plugin, PartyManager partyManager) {
        this.plugin = plugin;
        this.partyManager = partyManager;
        this.prefix = plugin.getPartyPrefix();
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Subscribe
    public void onPlayerSwitchServer(ServerConnectedEvent event) {
        Player player = event.getPlayer();

        if (!this.partyManager.isPartyLeader(player.getUniqueId())) return;

        final String serverName = event.getServer().getServerInfo().getName();
        if (plugin.getBlockedServers().contains(serverName)) return;

        //The party doesn't check if there's enough space for all party members
        this.partyManager.sendParty(player.getUniqueId(), event.getServer());
    }
}
