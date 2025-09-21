package de.t0bx.sentiencefriends.lobby.netty;

import de.t0bx.sentiencefriends.api.network.FriendsPacket;
import de.t0bx.sentiencefriends.api.network.packets.ReceiveFriendsPacket;
import de.t0bx.sentiencefriends.lobby.LobbyPlugin;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class PacketHandler extends SimpleChannelInboundHandler<FriendsPacket> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FriendsPacket friendsPacket) throws Exception {
        if (friendsPacket instanceof ReceiveFriendsPacket receiveFriendsPacket) {
            System.out.println("Received friends data from " + receiveFriendsPacket.getUuid());
            LobbyPlugin.getInstance().getFriendsManager().getFriendsData().put(
                    receiveFriendsPacket.getUuid(),
                    receiveFriendsPacket.getFriendsData()
            );
        }
    }
}
