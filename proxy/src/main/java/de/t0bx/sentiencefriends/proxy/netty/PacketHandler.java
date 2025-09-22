package de.t0bx.sentiencefriends.proxy.netty;

import de.t0bx.sentiencefriends.api.data.FriendsData;
import de.t0bx.sentiencefriends.api.network.FriendsPacket;
import de.t0bx.sentiencefriends.api.network.packets.ChannelIdentifyPacket;
import de.t0bx.sentiencefriends.api.network.packets.ReceiveFriendsPacket;
import de.t0bx.sentiencefriends.api.network.packets.RequestFriendsPacket;
import de.t0bx.sentiencefriends.api.network.packets.UpdateSettingsPacket;
import de.t0bx.sentiencefriends.proxy.ProxyPlugin;
import de.t0bx.sentiencefriends.proxy.friends.FriendsDataImpl;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.UUID;

public class PacketHandler extends SimpleChannelInboundHandler<FriendsPacket> {

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
        }
    }
}
