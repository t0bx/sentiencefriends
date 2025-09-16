package de.t0bx.sentiencefriends.proxy.netty;

import de.t0bx.sentiencefriends.api.network.FriendsPacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class PacketHandler extends SimpleChannelInboundHandler<FriendsPacket> {

    @Override
    protected void channelRead0(ChannelHandlerContext handlerContext, FriendsPacket packet) throws Exception {

    }
}
