package de.t0bx.sentiencefriends.proxy.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.t0bx.sentiencefriends.proxy.ProxyPlugin;
import de.t0bx.sentiencefriends.proxy.friends.FriendsData;
import de.t0bx.sentiencefriends.proxy.friends.FriendsManager;
import de.t0bx.sentiencefriends.proxy.utils.UUIDFetcher;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;
import java.util.Optional;

public class FriendCommand implements SimpleCommand {

    private final ProxyPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final ProxyServer proxyServer;
    private final FriendsManager friendsManager;
    private final String prefix;

    public FriendCommand(ProxyPlugin plugin, FriendsManager friendsManager) {
        this.plugin = plugin;
        this.proxyServer = plugin.getProxyServer();
        this.friendsManager = friendsManager;
        this.prefix = plugin.getPrefix();
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) return;

        String[] args = invocation.arguments();

        if (args.length == 0) {
            handleHelp(player);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> {
                if (args.length != 2) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "Usage: /friend add <Name> <dark_gray>| <gray>Add a friend"));
                    return;
                }

                this.handleAdd(player, args[1]);
            }

            case "remove" -> {

            }

            case "accept" -> {

            }

            case "deny" -> {

            }

            case "list" -> {

            }
        }
    }

    private void handleHelp(Player player) {
        player.sendMessage(this.miniMessage.deserialize(this.prefix + " "));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + "-/friend add <Name> <dark_gray>| <gray>Add a friend."));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + "-/friend remove <Name> <dark_gray>| <gray>Remove a friend."));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + "-/friend accept <Name> <dark_gray>| <gray>Accept a friend request."));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + "-/friend deny <Name> <dark_gray>| <gray>Deny a friend request."));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + "-/friend jump <Name> <dark_gray>| <gray>Jump to a friend."));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + "-/friend list <dark_gray>| <gray>Get a list of all friends."));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + "-/friend requests <dark_gray>| <gray>Get a list of all requests."));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + " "));
    }

    private void handleAdd(Player player, String name) {
        UUIDFetcher.getUUIDAsync(name)
                .exceptionally(ex -> null)
                .thenAccept(uuid -> {
                    this.proxyServer.getScheduler().buildTask(this.plugin, () -> {
                        if (uuid == null) {
                            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>Player not found"));
                            return;
                        }

                        FriendsData friendsData = this.friendsManager.get(player.getUniqueId());

                        if (friendsData.getFriends().containsKey(uuid)) {
                            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You already have this friend."));
                            return;
                        }

                        if (friendsData.getOutgoingRequests().contains(uuid)) {
                            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You already have a request for this player."));
                            return;
                        }

                        friendsData.sendRequest(uuid);
                        player.sendMessage(this.miniMessage.deserialize(this.prefix + "<green>You have sent a request to " + name + "."));

                        Optional<Player> optionalTarget = this.proxyServer.getPlayer(uuid);
                        optionalTarget.ifPresent(target -> {
                            target.sendMessage(this.miniMessage.deserialize(this.prefix + "<green>You have received a request from " + player.getUsername() + "."));
                            target.sendMessage(this.miniMessage.deserialize(this.prefix + "<green><b>[ACCEPT]")
                                    .clickEvent(ClickEvent.runCommand("/friend accept " + player.getUsername()))
                                    .append(this.miniMessage.deserialize(" <red><b>[DENY]")
                                            .clickEvent(ClickEvent.runCommand("/friend deny " + player.getUsername())))
                            );
                        });
                    }).schedule();
                });
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return SimpleCommand.super.suggest(invocation);
    }
}
