package de.t0bx.sentiencefriends.api.network;

import io.netty.buffer.ByteBuf;

public interface FriendsPacket {
    void read(ByteBuf buf);

    void write(ByteBuf buf);

    int getId();
}
