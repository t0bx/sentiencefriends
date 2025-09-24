package de.t0bx.sentiencefriends.api.network.packets;

import de.t0bx.sentiencefriends.api.netty.PacketType;
import de.t0bx.sentiencefriends.api.network.FriendsPacket;
import io.netty.buffer.ByteBuf;

public class UpdateFriendPacket implements FriendsPacket {
    @Override
    public void read(ByteBuf buf) {

    }

    @Override
    public void write(ByteBuf buf) {

    }

    @Override
    public int getId() {
        return PacketType.UPDATE_FRIEND_PACKET.getId();
    }
}
