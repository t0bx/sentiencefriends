package de.t0bx.sentiencefriends.proxy.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.t0bx.sentiencefriends.proxy.ProxyPlugin;
import de.t0bx.sentiencefriends.proxy.party.PartyData;
import de.t0bx.sentiencefriends.proxy.party.PartyManager;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PartyCommand implements SimpleCommand {

    private final ProxyPlugin plugin;
    private final String prefix;
    private final MiniMessage miniMessage;
    private final PartyManager partyManager;
    private final ProxyServer proxyServer;

    public PartyCommand(ProxyPlugin plugin, PartyManager partyManager) {
        this.plugin = plugin;
        this.prefix = plugin.getPartyPrefix();
        this.miniMessage = MiniMessage.miniMessage();
        this.partyManager = partyManager;
        this.proxyServer = plugin.getProxyServer();
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(miniMessage.deserialize(prefix + "<red>You must be a player to use this command."));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length == 0) {
            this.handleHelp(player);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (args.length != 1) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "Usage: /party create <dark_gray>| <gray>Create a party."));
                    return;
                }

                if (this.partyManager.isPartyLeader(player.getUniqueId())) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You are already have a party."));
                    return;
                }

                if (this.partyManager.isMemberOfParty(player.getUniqueId())) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You are already in a party."));
                    return;
                }

                this.partyManager.createParty(player.getUniqueId());
                player.sendMessage(this.miniMessage.deserialize(this.prefix + "<green>You have created a party."));
            }

            case "join" -> {
                if (args.length != 2) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "Usage: /party join <Playername> <dark_gray>| <gray>Join a party."));
                    return;
                }

                String playerName = args[1];
                Player target = this.plugin.getProxyServer().getPlayer(playerName).orElse(null);
                if (target == null) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>The Player " + playerName + " is not online."));
                    return;
                }

                PartyData partyData = this.partyManager.getParty(target.getUniqueId());
                if (partyData == null) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>The Player " + playerName + " does not have a party."));
                    return;
                }

                if (this.partyManager.isMemberOfParty(player.getUniqueId()) || this.partyManager.isPartyLeader(player.getUniqueId())) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You are already in a party."));
                    return;
                }

                if (!partyData.isPublic()) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>The party of " + playerName + " is private."));
                    return;
                }

                this.partyManager.addPartyMember(target.getUniqueId(), player.getUniqueId());
                player.sendMessage(this.miniMessage.deserialize(this.prefix + "<green>You have joined the party of " + playerName + "."));

                for (UUID uuid : partyData.getMembers()) {
                    if (uuid.equals(player.getUniqueId())) continue;

                    this.proxyServer.getPlayer(uuid).ifPresent(member -> {
                        member.sendMessage(this.miniMessage.deserialize(this.prefix + "<green>" + player.getUsername() + " has joined the party."));
                    });
                }
            }

            case "invite" -> {
                if (args.length != 2) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "Usage: /party invite <Playername> <dark_gray>| <gray>Invite a player to your party."));
                    return;
                }

                String playerName = args[1];
                Player target = this.plugin.getProxyServer().getPlayer(playerName).orElse(null);
                if (target == null) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>The Player " + playerName + " is not online."));
                    return;
                }

                if (!this.partyManager.isPartyLeader(player.getUniqueId())) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You are not the party leader."));
                    return;
                }

                if (this.partyManager.hasPartyInvite(target.getUniqueId(), player.getUniqueId())) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You have already sent an invite to " + playerName + "."));
                    return;
                }

                if (this.partyManager.isMemberOfParty(target.getUniqueId()) || this.partyManager.isPartyLeader(target.getUniqueId())) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>The Player " + playerName + " is already in a party."));
                    return;
                }

                this.partyManager.invitePartyMember(player.getUniqueId(), target.getUniqueId());
                player.sendMessage(this.miniMessage.deserialize(this.prefix + "<green>You have sent an invite to " + playerName + "."));
                target.sendMessage(this.miniMessage.deserialize(this.prefix + "<green>You have received a Party invite from " + player.getUsername() + "."));
                target.sendMessage(this.miniMessage.deserialize(this.prefix + "<green><b>[ACCEPT]")
                        .clickEvent(ClickEvent.runCommand("/party accept " + player.getUsername()))
                        .append(this.miniMessage.deserialize(" <red><b>[DENY]")
                                .clickEvent(ClickEvent.runCommand("/party deny " + player.getUsername()))));
            }

            case "kick" -> {
                if (args.length != 2) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "Usage: /party kick <Playername> <dark_gray>| <gray>Kick a player from your party."));
                    return;
                }

                String playerName = args[1];
                Player target = this.plugin.getProxyServer().getPlayer(playerName).orElse(null);
                if (target == null) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>The Player " + playerName + " is not online."));
                    return;
                }

                if (!this.partyManager.isPartyLeader(player.getUniqueId())) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You are not the party leader."));
                    return;
                }

                PartyData partyData = this.partyManager.getParty(player.getUniqueId());
                if (!partyData.getMembers().contains(target.getUniqueId())) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>The Player " + playerName + " is not in your party."));
                    return;
                }

                if (partyData.getLeader().equals(target.getUniqueId())) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You can not kick yourself from the party."));
                    return;
                }

                this.partyManager.removePartyMember(player.getUniqueId(), target.getUniqueId());
                player.sendMessage(this.miniMessage.deserialize(this.prefix + "<green>You have kicked " + playerName + " from your party."));

                for (UUID uuid : partyData.getMembers()) {
                    if (uuid.equals(player.getUniqueId())) continue;

                    this.proxyServer.getPlayer(uuid).ifPresent(member -> {
                        member.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>" + playerName + " has left the party."));
                    });
                }
            }

            case "accept" -> {
                if (args.length != 2) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "Usage: /party accept <Playername> <dark_gray>| <gray>Accept a party invite from a player."));
                    return;
                }

                String playerName = args[1];
                Player target = this.plugin.getProxyServer().getPlayer(playerName).orElse(null);
                if (target == null) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>The Player " + playerName + " is not online."));
                    return;
                }

                if (!this.partyManager.hasPartyInvite(player.getUniqueId(), target.getUniqueId())) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You have not received an invite from " + playerName + "."));
                    return;
                }

                if (!this.partyManager.isPartyLeader(target.getUniqueId())) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>The party of " + playerName + " does no longer exists."));
                    return;
                }

                if (this.partyManager.isMemberOfParty(player.getUniqueId()) || this.partyManager.isPartyLeader(player.getUniqueId())) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You are already in a party."));
                    return;
                }

                this.partyManager.acceptPartyInvite(player.getUniqueId());
                player.sendMessage(this.miniMessage.deserialize(this.prefix + "<green>You have accepted the invite from " + playerName + "."));

                for (UUID uuid : this.partyManager.getPartyMembersWithLeader(target.getUniqueId())) {
                    if (uuid.equals(player.getUniqueId())) continue;

                    this.proxyServer.getPlayer(uuid).ifPresent(member -> {
                        member.sendMessage(this.miniMessage.deserialize(this.prefix + "<green>" + player.getUsername() + " has joined the party."));
                    });
                }
            }

            case "deny" -> {
                if (args.length != 2) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "Usage: /party deny <Playername> <dark_gray>| <gray>Deny a party invite from a player."));
                    return;
                }

                String playerName = args[1];
                Player target = this.plugin.getProxyServer().getPlayer(playerName).orElse(null);
                if (target == null) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>The Player " + playerName + " is not online."));
                    return;
                }

                if (!this.partyManager.hasPartyInvite(player.getUniqueId(), target.getUniqueId())) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You have not received an invite from " + playerName + "."));
                    return;
                }

                this.partyManager.declinePartyInvite(player.getUniqueId());
                player.sendMessage(this.miniMessage.deserialize(this.prefix + "<green>You have denied the invite from " + playerName + "."));
            }

            case "leave" -> {
                if (args.length != 1) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "Usage: /party leave <dark_gray>| <gray>Leave a party."));
                    return;
                }

                if (this.partyManager.isPartyLeader(player.getUniqueId())) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You are the party leader. You can not leave your party."));
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>To delete your party use /party delete."));
                    return;
                }

                if (!this.partyManager.isMemberOfParty(player.getUniqueId())) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You are not in a party."));
                    return;
                }

                PartyData partyData = this.partyManager.getPartyByMember(player.getUniqueId());

                partyData.getMembers().remove(player.getUniqueId());
                player.sendMessage(this.miniMessage.deserialize(this.prefix + "<green>You have left the party."));

                for (UUID uuid : this.partyManager.getPartyMembersWithLeader(partyData.getLeader())) {
                    if (uuid.equals(player.getUniqueId())) continue;

                    this.proxyServer.getPlayer(uuid).ifPresent(member -> {
                        member.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>" + player.getUsername() + " has left the party."));
                    });
                }
            }

            case "delete" -> {
                if (args.length != 1) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "Usage: /party delete <dark_gray>| <gray>Delete a party."));
                    return;
                }

                if (!this.partyManager.isPartyLeader(player.getUniqueId())) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You are not the party leader."));
                    return;
                }

                PartyData partyData = this.partyManager.getParty(player.getUniqueId());
                if (partyData == null) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You are not in a party."));
                    return;
                }

                Set<UUID> members = new HashSet<>(partyData.getMembers());
                this.partyManager.deleteParty(player.getUniqueId());
                player.sendMessage(this.miniMessage.deserialize(this.prefix + "<green>You have deleted your party."));

                for (UUID uuid : members) {
                    this.proxyServer.getPlayer(uuid).ifPresent(member -> {
                        member.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>" + player.getUsername() + " has deleted their party."));
                    });
                }
            }

            case "list" -> {
                if (args.length != 1) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "Usage: /party list <dark_gray>| <gray>List all players in your party."));
                    return;
                }

                if (!this.partyManager.isMemberOfParty(player.getUniqueId()) && !this.partyManager.isPartyLeader(player.getUniqueId())) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You are not in a party."));
                    return;
                }

                boolean isLeader = this.partyManager.isPartyLeader(player.getUniqueId());
                PartyData partyData = isLeader
                        ? this.partyManager.getParty(player.getUniqueId())
                        : this.partyManager.getPartyByMember(player.getUniqueId());

                player.sendMessage(this.miniMessage.deserialize(this.prefix + "<gray>Party members (<green>" + partyData.getMembers().size() + "<gray>):"));
                player.sendMessage(this.miniMessage.deserialize(this.prefix + "<gray>--------------------------------"));

                if (isLeader) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<green>Party Leader: " + player.getUsername()));
                } else {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<green>Party Leader: " + this.proxyServer.getPlayer(partyData.getLeader()).get().getUsername()));
                }

                for (UUID uuid : partyData.getMembers()) {
                    this.proxyServer.getPlayer(uuid).ifPresent(member -> {
                       player.sendMessage(this.miniMessage.deserialize(this.prefix + "<green>" + member.getUsername()));
                    });
                }
            }

            case "privacy" -> {
                if (args.length != 2) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "Usage: /party privacy <Public/Private> <dark_gray>| <gray>Change the privacy of your party."));
                    return;
                }

                if (!args[1].equalsIgnoreCase("public") && !args[1].equalsIgnoreCase("private")) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>Invalid privacy setting. Valid Types are <b>PUBLIC</b> <red>and <b>PRIVATE</b>"));
                    return;
                }

                if (!this.partyManager.isPartyLeader(player.getUniqueId())) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You are not the party leader."));
                    return;
                }

                PartyData partyData = this.partyManager.getParty(player.getUniqueId());
                if (partyData == null) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You are not in a party."));
                    return;
                }

                partyData.setPublic(args[1].equalsIgnoreCase("public"));
                player.sendMessage(this.miniMessage.deserialize(this.prefix + "<green>You have changed the privacy of your party to " + args[1].toUpperCase() + "."));
            }

            case "chat" -> {
                if (args.length != 2) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "Usage: /party chat <On/Off> <dark_gray>| <gray>Change the chat of your party."));
                    return;
                }

                if (!args[1].equalsIgnoreCase("on") && !args[1].equalsIgnoreCase("off")) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>Invalid chat setting. Valid Types are <b>ON</b> <red>and <b>OFF</b>"));
                    return;
                }

                if (!this.partyManager.isPartyLeader(player.getUniqueId())) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You are not the party leader."));
                    return;
                }

                PartyData partyData = this.partyManager.getParty(player.getUniqueId());
                if (partyData == null) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You are not in a party."));
                    return;
                }

                partyData.setChatDisabled(args[1].equalsIgnoreCase("off"));
                player.sendMessage(this.miniMessage.deserialize(this.prefix + "<green>You have changed the chat of your party to " + args[1].toUpperCase() + "."));
            }

            default -> this.handleHelp(player);
        }
    }

    private void handleHelp(Player player) {
        player.sendMessage(this.miniMessage.deserialize(this.prefix + " "));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + "-/party create <dark_gray>| <gray>Create a party."));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + "-/party join <Playername> <dark_gray>| <gray>Join a party."));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + "-/party invite <Playername> <dark_gray>| <gray>Invite a player to your party."));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + "-/party kick <Playername> <dark_gray>| <gray>Kick a player from your party."));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + "-/party accept <Playername> <dark_gray>| <gray>Accept a party invite from a player."));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + "-/party deny <Playername> <dark_gray>| <gray>Deny a party invite from a player."));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + "-/party leave <dark_gray>| <gray>Leave a party."));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + "-/party delete <dark_gray>| <gray>Delete a party."));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + "-/party list <dark_gray>| <gray>List all players in your party."));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + "-/party privacy <Public/Private> <dark_gray>| <gray>Change the privacy of your party."));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + "-/party chat <On/Off> <dark_gray>| <gray>Change the chat of your party."));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + " "));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return SimpleCommand.super.suggest(invocation);
    }
}
