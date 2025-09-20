package de.t0bx.sentiencefriends.proxy.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.t0bx.sentiencefriends.proxy.ProxyPlugin;
import de.t0bx.sentiencefriends.proxy.friends.FriendsDataImpl;
import de.t0bx.sentiencefriends.proxy.friends.FriendsManager;
import de.t0bx.sentiencefriends.proxy.utils.NameFetcher;
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
                if (args.length != 2) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "Usage: /friend remove <Name> <dark_gray>| <gray>Remove a friend"));
                    return;
                }

                this.handleRemove(player, args[1]);
            }

            case "accept" -> {
                if (args.length != 2) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "Usage: /friend accept <Name> <dark_gray>| <gray>Accept a friend request"));
                    return;
                }

                this.handleAccept(player, args[1]);
            }

            case "deny" -> {
                if (args.length != 2) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "Usage: /friend deny <Name> <dark_gray>| <gray>Deny a friend request"));
                    return;
                }

                this.handleDeny(player, args[1]);
            }

            case "jump" -> {
                if (args.length != 2) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "Usage: /friend jump <Name> <dark_gray>| <gray>Jump to a friend"));
                    return;
                }

                this.handleJump(player, args[1]);
            }

            case "favorite" -> {
                if (args.length != 2) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "Usage: /friend favorite <Name> <dark_gray>| <gray>Mark a friend as a favorite or no longer as favorite"));
                    return;
                }

                this.handleFavorite(player, args[1]);
            }

            case "list" -> {
                if (args.length != 1) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "Usage: /friend list <dark_gray>| <gray>Get a list of all friends"));
                    return;
                }

                this.handleList(player);
            }

            case "requests" -> {
                if (args.length != 1) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "Usage: /friend requests <dark_gray>| <gray>Get a list of all requests"));
                    return;
                }

                this.handleRequests(player);
            }

            case "settings" -> {
                this.handleSettings(player, args);
            }

            default -> this.handleHelp(player);
        }
    }

    private void handleHelp(Player player) {
        player.sendMessage(this.miniMessage.deserialize(this.prefix + " "));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + "-/friend add <Name> <dark_gray>| <gray>Add a friend."));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + "-/friend remove <Name> <dark_gray>| <gray>Remove a friend."));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + "-/friend accept <Name> <dark_gray>| <gray>Accept a friend request."));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + "-/friend deny <Name> <dark_gray>| <gray>Deny a friend request."));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + "-/friend jump <Name> <dark_gray>| <gray>Jump to a friend."));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + "-/friend favorite <Name> <dark_gray>| <gray>Mark a friend as a favorite or no longer as favorite."));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + "-/friend list <dark_gray>| <gray>Get a list of all friends."));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + "-/friend requests <dark_gray>| <gray>Get a list of all requests."));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + "-/friend settings <dark_gray>| <gray>Get a list of all settings."));
        player.sendMessage(this.miniMessage.deserialize(this.prefix + " "));
    }

    private void handleAdd(Player player, String name) {
        UUIDFetcher.getUUIDAsync(name)
                .exceptionally(ex -> null)
                .thenAccept(uuid -> {
                    if (uuid == null) {
                        this.proxyServer.getScheduler().buildTask(this.plugin, () ->
                                player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>Player not found"))).schedule();
                        return;
                    }

                    this.friendsManager.doesFriendAcceptRequests(uuid)
                            .exceptionally(ex -> false)
                            .thenAccept(acceptsRequests ->
                                    this.proxyServer.getScheduler().buildTask(this.plugin, () -> {
                                        if (uuid.equals(player.getUniqueId())) {
                                            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You can't add yourself as a friend."));
                                            return;
                                        }

                                        FriendsDataImpl friendsDataImpl = this.friendsManager.get(player.getUniqueId());

                                        if (friendsDataImpl.getFriends().containsKey(uuid)) {
                                            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You already have this friend."));
                                            return;
                                        }

                                        if (friendsDataImpl.getOutgoingRequests().contains(uuid)) {
                                            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You already have a request for this player."));
                                            return;
                                        }

                                        if (friendsDataImpl.getIncomingRequests().contains(uuid)) {
                                            friendsDataImpl.acceptRequest(player.getUsername(), uuid, name);
                                            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<green>You are now friends with " + name + "."));
                                            return;
                                        }

                                        if (!acceptsRequests) {
                                            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>The player " + name + " has disabled friend requests."));
                                            return;
                                        }

                                        friendsDataImpl.sendRequest(uuid);
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
                                    }).schedule());
                });
    }

    private void handleRemove(Player player, String name) {
        UUIDFetcher.getUUIDAsync(name)
                .exceptionally(ex -> null)
                .thenAccept(uuid -> {
                    this.proxyServer.getScheduler().buildTask(this.plugin, () -> {
                        if (uuid == null) {
                            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>Player not found"));
                            return;
                        }

                        FriendsDataImpl playerData = this.friendsManager.get(player.getUniqueId());
                        if (!playerData.getFriends().containsKey(uuid)) {
                            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You are not friends with this player."));
                            return;
                        }

                        playerData.removeFriend(uuid);
                        player.sendMessage(this.miniMessage.deserialize(this.prefix + "<green>You are no longer friends with " + name + "."));
                    }).schedule();
                });
    }

    private void handleAccept(Player player, String name) {
        UUIDFetcher.getUUIDAsync(name)
                .exceptionally(ex -> null)
                .thenAccept(uuid -> {
                    this.proxyServer.getScheduler().buildTask(this.plugin, () -> {
                        if (uuid == null) {
                            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>Player not found"));
                            return;
                        }

                        FriendsDataImpl playerData = this.friendsManager.get(player.getUniqueId());
                        if (!playerData.getIncomingRequests().contains(uuid)) {
                            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You have no incoming requests from this player."));
                            return;
                        }

                        playerData.acceptRequest(player.getUsername(), uuid, name);
                        player.sendMessage(this.miniMessage.deserialize(this.prefix + "<green>You are now friends with " + name + "."));
                    }).schedule();
                });
    }

    private void handleDeny(Player player, String name) {
        UUIDFetcher.getUUIDAsync(name)
                .exceptionally(ex -> null)
                .thenAccept(uuid -> {
                    this.proxyServer.getScheduler().buildTask(this.plugin, () -> {
                        if (uuid == null) {
                            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>Player not found"));
                            return;
                        }

                        FriendsDataImpl playerData = this.friendsManager.get(player.getUniqueId());
                        if (!playerData.getIncomingRequests().contains(uuid)) {
                            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You have no incoming requests from this player."));
                            return;
                        }

                        playerData.declineRequest(uuid);
                        player.sendMessage(this.miniMessage.deserialize(this.prefix + "<green>You have declined the request from " + name + "."));
                    }).schedule();
                });
    }

    private void handleJump(Player player, String name) {
        UUIDFetcher.getUUIDAsync(name)
                .exceptionally(ex -> null)
                .thenAccept(uuid -> {
                    this.proxyServer.getScheduler().buildTask(this.plugin, () -> {
                        if (uuid == null) {
                            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>Player not found"));
                            return;
                        }

                        FriendsDataImpl playerData = this.friendsManager.get(player.getUniqueId());
                        if (!playerData.getFriends().containsKey(uuid)) {
                            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You are not friends with this player."));
                            return;
                        }

                        Player target = this.proxyServer.getPlayer(uuid).orElse(null);
                        if (target == null) {
                            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>The player " + name + " is not online."));
                            return;
                        }

                        FriendsDataImpl targetData = this.friendsManager.get(uuid);
                        if (!targetData.getSettings().isJumpEnabled()) {
                            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>The player " + name + " has disabled the jump feature."));
                            return;
                        }

                        ServerConnection serverConnection = target.getCurrentServer().orElse(null);
                        if (serverConnection == null) {
                            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>The player " + name + " is not connected to a server."));
                            return;
                        }

                        RegisteredServer registeredServer = serverConnection.getServer();
                        if (registeredServer == null) {
                            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>The player " + name + " is not connected to a server."));
                            return;
                        }

                        if (player.getCurrentServer().get().getServer().getServerInfo().getName().equals(registeredServer.getServerInfo().getName())) {
                            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You are already on the same server as " + name + "."));
                            return;
                        }

                        player.createConnectionRequest(registeredServer).fireAndForget();
                        player.sendMessage(this.miniMessage.deserialize(this.prefix + "<green>You are now on the server of " + name + "."));
                    }).schedule();
                });
    }

    private void handleList(Player player) {
        FriendsDataImpl friendsDataImpl = this.friendsManager.get(player.getUniqueId());
        if (friendsDataImpl.getFriends().isEmpty()) {
            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You have no friends."));
            return;
        }

        List<FriendsDataImpl.Friend> favoriteFriends = friendsDataImpl.getFriends().values().stream()
                .filter(FriendsDataImpl.Friend::isFavorite)
                .toList();

        List<FriendsDataImpl.Friend> onlineFriends = friendsDataImpl.getFriends().values().stream()
                .filter(FriendsDataImpl.Friend::isOnline)
                .filter(f -> !f.isFavorite())
                .toList();

        List<FriendsDataImpl.Friend> offlineFriends = friendsDataImpl.getFriends().values().stream()
                .filter(f -> !f.isOnline())
                .filter(f -> !f.isFavorite())
                .toList();

        for (FriendsDataImpl.Friend favoriteFriend : favoriteFriends) {
            final String cachedName = favoriteFriend.getCachedName();
            String message = favoriteFriend.isOnline() ? "<green>Online" : "<red>Last online <gray>" + favoriteFriend.getLastOnlineTime() + "<gray>.";

            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<yellow>★ <gray>" + cachedName + " " + message));
        }

        for (FriendsDataImpl.Friend onlineFriend : onlineFriends) {
            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<gray>" + onlineFriend.getCachedName() + " <dark_gray>» <green>Online."));
        }

        for (FriendsDataImpl.Friend offlineFriend : offlineFriends) {
            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<gray>" + offlineFriend.getCachedName() + " <dark_gray>» <gray>Last online <red>" + offlineFriend.getLastOnlineTime() + "<gray>."));
        }
    }

    private void handleFavorite(Player player, String name) {
        UUIDFetcher.getUUIDAsync(name)
                .exceptionally(ex -> null)
                .thenAccept(uuid -> {
                    this.proxyServer.getScheduler().buildTask(this.plugin, () -> {
                        if (uuid == null) {
                            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>Player not found"));
                            return;
                        }

                        FriendsDataImpl friendsDataImpl = this.friendsManager.get(player.getUniqueId());
                        if (!friendsDataImpl.getFriends().containsKey(uuid)) {
                            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You are not friends with this player."));
                            return;
                        }

                        FriendsDataImpl.Friend friend = friendsDataImpl.getFriends().getOrDefault(uuid, null);
                        if (friend == null) {
                            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>An error occurred."));
                            return;
                        }

                        boolean isFavorite = friendsDataImpl.getFriends().get(uuid).isFavorite();
                        String message = isFavorite ?
                                "<red>" + name + " is now no longer marked as favorite." :
                                "<green>" + name + " is now marked as favorite.";

                        friendsDataImpl.setFavorite(uuid, !isFavorite);
                        player.sendMessage(this.miniMessage.deserialize(this.prefix + message));
                    }).schedule();
                });
    }

    private void handleRequests(Player player) {
        FriendsDataImpl friendsDataImpl = this.friendsManager.get(player.getUniqueId());
        if (friendsDataImpl.getIncomingRequests().isEmpty()) {
            player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>You have no incoming requests."));
            return;
        }

        NameFetcher.getNamesAsync(friendsDataImpl.getIncomingRequests())
                .exceptionally(ex -> null)
                .thenAccept(names -> {
                    if (names == null) {
                        player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>An error occurred."));
                        return;
                    }

                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<green>Incoming requests (" + names.size() + "):"));
                    for (String name : names) {
                        player.sendMessage(this.miniMessage.deserialize(this.prefix + "<gray>" + name));
                    }
                });
    }

    private void handleSettings(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(this.miniMessage.deserialize(this.prefix + "Usage: /friend settings <jump|notifications|requests> <dark_gray>| <gray>Set a setting"));
            return;
        }

        FriendsDataImpl friendsDataImpl = this.friendsManager.get(player.getUniqueId());

        switch (args[1].toLowerCase()) {
            case "jump" -> {
                boolean enabled = friendsDataImpl.getSettings().isJumpEnabled();
                friendsDataImpl.changeSetting(FriendsDataImpl.SettingType.JUMP, !enabled);
                String message = enabled
                        ? "<red>Friends will no longer be able to jump to your server"
                        : "<green>Friends will now be able to jump to your server";
                player.sendMessage(this.miniMessage.deserialize(this.prefix + message));
            }

            case "notifications" -> {
                boolean enabled = friendsDataImpl.getSettings().isNotificationsEnabled();
                friendsDataImpl.changeSetting(FriendsDataImpl.SettingType.NOTIFICATIONS, !enabled);
                String message = enabled
                        ? "<red>You will no longer receive friend notifications"
                        : "<green>You will now receive friend notifications";
                player.sendMessage(this.miniMessage.deserialize(this.prefix + message));
            }

            case "requests" -> {
                boolean enabled = friendsDataImpl.getSettings().isRequestsEnabled();
                friendsDataImpl.changeSetting(FriendsDataImpl.SettingType.REQUESTS, !enabled);
                String message = enabled
                        ? "<red>You will no longer receive friend requests"
                        : "<green>You will now receive friend requests";
                player.sendMessage(this.miniMessage.deserialize(this.prefix + message));
            }
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return SimpleCommand.super.suggest(invocation);
    }
}