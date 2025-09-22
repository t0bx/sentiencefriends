package de.t0bx.sentiencefriends.api.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;

import java.util.List;

public class VarIntFrameDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        in.markReaderIndex();

        int numRead = 0, result = 0;
        while (true) {
            if (!in.isReadable()) {
                in.resetReaderIndex();
                return;
            }

            byte read = in.readByte();
            int value = (read & 0x7F);
            result |= (value << (7 * numRead));
            numRead++;

            if (numRead > 5) throw new CorruptedFrameException("VarInt too big");
            if ((read & 0x80) == 0) break;
        }

        int frameLength = result;
        if (in.readableBytes() < frameLength) {
            in.resetReaderIndex();
            return;
        }

        out.add(in.readRetainedSlice(frameLength));
    }
}
