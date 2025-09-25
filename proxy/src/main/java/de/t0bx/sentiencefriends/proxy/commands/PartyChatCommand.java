package de.t0bx.sentiencefriends.proxy.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.t0bx.sentiencefriends.proxy.ProxyPlugin;
import de.t0bx.sentiencefriends.proxy.party.PartyData;
import de.t0bx.sentiencefriends.proxy.party.PartyManager;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Arrays;
import java.util.UUID;

public class PartyChatCommand implements SimpleCommand {

    private final ProxyPlugin plugin;
    private final ProxyServer proxyServer;
    private final PartyManager partyManager;
    private final String prefix;
    private final MiniMessage miniMessage;

    public PartyChatCommand(ProxyPlugin plugin, PartyManager partyManager) {
        this.plugin = plugin;
        this.proxyServer = plugin.getProxyServer();
        this.partyManager = partyManager;
        this.prefix = plugin.getPartyPrefix();
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You must be a player to use this command!"));
            return;
        }

        if (!this.partyManager.isPartyLeader(player.getUniqueId()) && !this.partyManager.isMemberOfParty(player.getUniqueId())) {
            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You are currently not in a party!"));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length == 0) {
            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>Usage: /partychat <message>"));
            return;
        }

        boolean isLeader = this.partyManager.isPartyLeader(player.getUniqueId());
        PartyData partyData = isLeader
                ? this.partyManager.getParty(player.getUniqueId())
                : this.partyManager.getPartyByMember(player.getUniqueId());

        if (partyData.isChatDisabled()) {
            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>The party chat is currently disabled."));
            return;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + "<gray>You » " + message));
        for (UUID uuid : partyData.getMembers()) {
            this.proxyServer.getPlayer(uuid).ifPresent(member -> {
               member.sendMessage(this.miniMessage.deserialize(this.prefix + "<gray>" + player.getUsername() + " » " + message));
            });
        }
    }
}
