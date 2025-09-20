package de.t0bx.sentiencefriends.proxy.netty;

import de.t0bx.sentiencefriends.api.data.FriendsData;
import de.t0bx.sentiencefriends.api.network.FriendsPacket;
import de.t0bx.sentiencefriends.api.network.packets.ReceiveFriendsPacket;
import de.t0bx.sentiencefriends.api.network.packets.RequestFriendsPacket;
import de.t0bx.sentiencefriends.proxy.ProxyPlugin;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.UUID;

public class PacketHandler extends SimpleChannelInboundHandler<FriendsPacket> {

    @Override
    protected void channelRead0(ChannelHandlerContext handlerContext, FriendsPacket packet) throws Exception {
        if (packet instanceof RequestFriendsPacket requestFriendsPacket) {
            String channel = requestFriendsPacket.getChannelName();
            UUID uuid = requestFriendsPacket.getUuid();

            FriendsData friendsData = ProxyPlugin.getInstance().getFriendsManager().get(uuid);
            if (friendsData != null) {
                ProxyPlugin.getInstance().getNettyManager().sendPacket(channel, new ReceiveFriendsPacket(uuid, friendsData));
            }
        }
    }
}
