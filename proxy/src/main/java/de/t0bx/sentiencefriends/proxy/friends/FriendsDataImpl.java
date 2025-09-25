package de.t0bx.sentiencefriends.proxy.friends;

import com.velocitypowered.api.proxy.ProxyServer;
import de.t0bx.sentiencefriends.api.data.FriendsData;
import de.t0bx.sentiencefriends.api.data.UpdateType;
import de.t0bx.sentiencefriends.api.network.packets.UpdateFriendPacket;
import de.t0bx.sentiencefriends.proxy.ProxyPlugin;
import de.t0bx.sentiencefriends.proxy.database.IMySQLManager;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.*;

public class FriendsDataImpl extends FriendsData {

    private final IMySQLManager mySQLManager;
    private final ProxyServer proxyServer;

    public FriendsDataImpl(UUID uuid) {
        super(uuid);
        this.mySQLManager = ProxyPlugin.getInstance().getMySQLManager();
        this.proxyServer = ProxyPlugin.getInstance().getProxyServer();
    }

    public void sendRequest(UUID receiver) {
        this.outgoingRequests.add(receiver);
        String update = "INSERT INTO friends_requests (uuid_sender, uuid_receiver) VALUES (?, ?)";
        this.mySQLManager.updateAsync(update, uuid.toString(), receiver.toString());

        this.proxyServer.getPlayer(receiver).ifPresent(player -> {
            FriendsDataImpl friendsDataImpl = ProxyPlugin.getInstance().getFriendsManager().get(receiver);
            if (friendsDataImpl == null) return;

            friendsDataImpl.getIncomingRequests().add(uuid);
        });
    }

    public void acceptRequest(String playerName, UUID sender, String friendName) {
        this.incomingRequests.remove(sender);

        String del = "DELETE FROM friends_requests WHERE uuid_sender = ? AND uuid_receiver = ?";
        this.mySQLManager.updateAsync(del, sender.toString(), uuid.toString());

        String insert = "INSERT INTO friends_data (uuid_player, uuid_friend, cached_name, since, last_online) VALUES (?, ?, ?, ?, ?)";
        long now = System.currentTimeMillis();
        this.mySQLManager.updateAsync(insert, uuid.toString(), sender.toString(), friendName, now, now);
        this.mySQLManager.updateAsync(insert, sender.toString(), uuid.toString(), playerName, now, now);

        Friend selfFriend = new Friend(sender, friendName, now);

        this.proxyServer.getPlayer(sender).ifPresent(player -> {
            FriendsDataImpl friendsDataImpl = ProxyPlugin.getInstance().getFriendsManager().get(sender);
            if (friendsDataImpl == null) return;

            Friend friend = new Friend(uuid, playerName, now);
            friend.setOnline(true);
            selfFriend.setOnline(true);

            friendsDataImpl.getFriends().put(uuid, friend);
            friendsDataImpl.getOutgoingRequests().remove(uuid);
            player.sendMessage(MiniMessage.miniMessage().deserialize(ProxyPlugin.getInstance().getPrefix() + "<green>You are now friends with " + playerName + "."));

            var updateFriendPacket = new UpdateFriendPacket(player.getUniqueId(), UpdateType.ADD, friend);
            ProxyPlugin.getInstance().getNettyManager().sendPacket(updateFriendPacket);
        });

        this.friends.put(sender, selfFriend);

        var updateFriendPacket = new UpdateFriendPacket(uuid, UpdateType.ADD, selfFriend);
        ProxyPlugin.getInstance().getNettyManager().sendPacket(updateFriendPacket);
    }

    public void declineRequest(UUID sender) {
        this.incomingRequests.remove(sender);
        String update = "DELETE FROM friends_requests WHERE uuid_sender = ? AND uuid_receiver = ?";
        this.mySQLManager.updateAsync(update, sender.toString(), uuid.toString());

        this.proxyServer.getPlayer(sender).ifPresent(player -> {
            FriendsDataImpl friendsDataImpl = ProxyPlugin.getInstance().getFriendsManager().get(sender);
            if (friendsDataImpl == null) return;

            friendsDataImpl.getOutgoingRequests().remove(uuid);
        });
    }

    public void removeFriend(UUID friend) {
        Friend removed = friends.remove(friend);
        if (removed == null) return;

        String update = "DELETE FROM friends_data WHERE uuid_friend = ? AND uuid_player = ?";
        this.mySQLManager.updateAsync(update, friend.toString(), uuid.toString());

        String update2 = "DELETE FROM friends_data WHERE uuid_friend = ? AND uuid_player = ?";
        this.mySQLManager.updateAsync(update2, uuid.toString(), friend.toString());

        this.proxyServer.getPlayer(friend).ifPresent(player -> {
            FriendsDataImpl friendsDataImpl = ProxyPlugin.getInstance().getFriendsManager().get(friend);
            if (friendsDataImpl == null) return;

            Friend friendsData = friendsDataImpl.getFriends().remove(uuid);
            var updateFriendPacket = new UpdateFriendPacket(player.getUniqueId(), UpdateType.REMOVE, friendsData);
            ProxyPlugin.getInstance().getNettyManager().sendPacket(updateFriendPacket);
        });

        var updateFriendPacket = new UpdateFriendPacket(uuid, UpdateType.REMOVE, removed);
        ProxyPlugin.getInstance().getNettyManager().sendPacket(updateFriendPacket);
    }

    public void setFavorite(UUID friend, boolean favorite) {
        Friend friendData = friends.get(friend);
        if (friendData == null) return;

        friendData.setFavorite(favorite);
        String update = "UPDATE friends_data SET favorite = ? WHERE uuid_friend = ? AND uuid_player = ?";
        this.mySQLManager.updateAsync(update, favorite, friend.toString(), uuid.toString());

        var updateFriendPacket = new UpdateFriendPacket(uuid, UpdateType.UPDATE, friendData);
        ProxyPlugin.getInstance().getNettyManager().sendPacket(updateFriendPacket);
    }

    public void changeSetting(SettingType setting, boolean value) {
        switch (setting) {
            case JUMP -> settings.setJumpEnabled(value);
            case NOTIFICATIONS -> settings.setNotificationsEnabled(value);
            case REQUESTS -> settings.setRequestsEnabled(value);
        }

        String key = setting.getKey();
        String update = "UPDATE friends_settings SET " + key + " = ? WHERE uuid = ?";
        this.mySQLManager.updateAsync(update, value, uuid.toString());
    }
}
