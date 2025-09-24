package de.t0bx.sentiencefriends.proxy.netty;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.t0bx.sentiencefriends.api.data.FriendsData;
import de.t0bx.sentiencefriends.api.network.FriendsPacket;
import de.t0bx.sentiencefriends.api.network.packets.*;
import de.t0bx.sentiencefriends.proxy.ProxyPlugin;
import de.t0bx.sentiencefriends.proxy.friends.FriendsDataImpl;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.UUID;

public class PacketHandler extends SimpleChannelInboundHandler<FriendsPacket> {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final String prefix = ProxyPlugin.getInstance().getPrefix();

    @Override
    protected void channelRead0(ChannelHandlerContext handlerContext, FriendsPacket packet) throws Exception {
        if (packet instanceof ChannelIdentifyPacket channelIdentifyPacket) {
            ProxyPlugin.getInstance().getNettyManager().addChannel(channelIdentifyPacket.getChannelName(), handlerContext.channel());
        } else if (packet instanceof RequestFriendsPacket requestFriendsPacket) {
            String channel = requestFriendsPacket.getChannelName();
            UUID uuid = requestFriendsPacket.getUuid();

            FriendsData friendsData = ProxyPlugin.getInstance().getFriendsManager().get(uuid);
            if (friendsData == null) return; //Maybe impl message that failed to get friends

            var receiveFriendsPacket = new ReceiveFriendsPacket(uuid, friendsData);
            ProxyPlugin.getInstance().getNettyManager().sendPacket(channel, receiveFriendsPacket);
        } else if (packet instanceof UpdateSettingsPacket updateSettingsPacket) {
            final UUID uuid = updateSettingsPacket.getUuid();
            final FriendsData.SettingType settingType = FriendsData.SettingType.fromKey(updateSettingsPacket.getSettingType());
            final boolean value = updateSettingsPacket.isValue();

            FriendsDataImpl friendsData = ProxyPlugin.getInstance().getFriendsManager().get(uuid);
            if (friendsData == null) return; //Maybe impl message that failed to get friends

            friendsData.changeSetting(settingType, value);
        } else if (packet instanceof RequestJumpPacket requestJumpPacket) {
            final UUID playerUuid = requestJumpPacket.getPlayer();
            final UUID targetUuid = requestJumpPacket.getTarget();

            ProxyPlugin.getInstance().getProxyServer().getScheduler().buildTask(ProxyPlugin.getInstance(), () -> {
                Player player = ProxyPlugin.getInstance().getProxyServer().getPlayer(playerUuid).orElse(null);
                if (player == null) return;

                Player target = ProxyPlugin.getInstance().getProxyServer().getPlayer(targetUuid).orElse(null);
                if (target == null) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>The player is not online."));
                    return;
                }

                String name = target.getUsername();

                FriendsDataImpl targetData = ProxyPlugin.getInstance().getFriendsManager().get(targetUuid);
                if (!targetData.getSettings().isJumpEnabled()) {
                    player.sendMessage(this.miniMessage.deserialize(this.prefix + "<red>The player has disabled the jump feature."));
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
        } else if (packet instanceof UpdateFavoritePacket updateFavoritePacket) {
            final UUID player = updateFavoritePacket.getPlayer();
            final UUID target = updateFavoritePacket.getTarget();
            final boolean favorite = updateFavoritePacket.isFavorite();

            FriendsDataImpl friendsData = ProxyPlugin.getInstance().getFriendsManager().get(player);
            if (friendsData == null) return;

            friendsData.setFavorite(target, favorite);
        } else if (packet instanceof RemoveFriendPacket removeFriendPacket) {
            final UUID player = removeFriendPacket.getUuid();
            final UUID target = removeFriendPacket.getFriend();

            FriendsDataImpl friendsData = ProxyPlugin.getInstance().getFriendsManager().get(player);
            if (friendsData == null) return;

            friendsData.removeFriend(target);
        }
    }
}
