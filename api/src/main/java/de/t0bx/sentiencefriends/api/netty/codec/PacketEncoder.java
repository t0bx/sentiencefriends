package de.t0bx.sentiencefriends.api.netty.codec;

import de.t0bx.sentiencefriends.api.PacketController;
import de.t0bx.sentiencefriends.api.netty.utils.ByteBufHelper;
import de.t0bx.sentiencefriends.api.network.FriendsPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.logging.Level;

public class PacketEncoder extends MessageToByteEncoder<FriendsPacket> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, FriendsPacket friendsPacket, ByteBuf byteBuf) throws Exception {
        try {
            ByteBufHelper.writeVarInt(byteBuf, friendsPacket.getId());
            friendsPacket.write(byteBuf);
        } catch (Exception exception) {
            PacketController.getInstance().getLogger().log(Level.SEVERE, "Failed to write packet '" + friendsPacket.getClass().getSimpleName() + "':" + exception);

            channelHandlerContext.close();
            throw exception;
        }
    }
}
