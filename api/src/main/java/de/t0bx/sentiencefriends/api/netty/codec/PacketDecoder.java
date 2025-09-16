package de.t0bx.sentiencefriends.api.netty.codec;

import de.t0bx.sentiencefriends.api.PacketController;
import de.t0bx.sentiencefriends.api.netty.PacketType;
import de.t0bx.sentiencefriends.api.netty.utils.ByteBufHelper;
import de.t0bx.sentiencefriends.api.network.FriendsPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;
import java.util.logging.Level;

public class PacketDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> out) throws Exception {
        try {
            if (byteBuf.readableBytes() < 4) return;

            byteBuf.markReaderIndex();
            int packetId = ByteBufHelper.readVarInt(byteBuf);
            PacketType packetType = PacketType.getById(packetId);
            if (packetType == null) {
                byteBuf.resetReaderIndex();
                return;
            }

            FriendsPacket packet = packetType.createPacket();
            packet.read(byteBuf);
            out.add(packet);
        } catch (Exception exception) {
            PacketController.getInstance().getLogger().log(Level.SEVERE, "Failed to read packet", exception);

            channelHandlerContext.close();
            throw exception;
        }
    }
}
