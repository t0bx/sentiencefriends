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
    protected void encode(ChannelHandlerContext ctx, FriendsPacket packet, ByteBuf out) throws Exception {
        try {

            ByteBuf payload = ctx.alloc().buffer();
            ByteBufHelper.writeVarInt(payload, packet.getId());
            packet.write(payload);

            int len = payload.readableBytes();
            ByteBufHelper.writeVarInt(out, len);
            out.writeBytes(payload);
            payload.release();
        } catch (Exception exception) {
            PacketController.getInstance().getLogger().log(Level.SEVERE, "Failed to write packet '" + packet.getClass().getSimpleName() + "':" + exception);

            ctx.close();
            throw exception;
        }
    }
}
