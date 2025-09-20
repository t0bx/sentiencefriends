package de.t0bx.sentiencefriends.api.network.packets;

import de.t0bx.sentiencefriends.api.netty.PacketType;
import de.t0bx.sentiencefriends.api.netty.utils.ByteBufHelper;
import de.t0bx.sentiencefriends.api.network.FriendsPacket;
import io.netty.buffer.ByteBuf;

import java.util.UUID;

public class RequestFriendsPacket implements FriendsPacket {

    private String channelName;
    private UUID uuid;

    public RequestFriendsPacket() {}

    public RequestFriendsPacket(String channelName, UUID uuid) {
        this.channelName = channelName;
        this.uuid = uuid;
    }

    @Override
    public void read(ByteBuf buf) {
        this.channelName = ByteBufHelper.readString(buf);
        this.uuid = ByteBufHelper.readUUID(buf);
    }

    @Override
    public void write(ByteBuf buf) {
        ByteBufHelper.writeVarInt(buf, getId());

        ByteBufHelper.writeString(buf, channelName);
        ByteBufHelper.writeUUID(buf, uuid);
    }

    @Override
    public int getId() {
        return PacketType.REQUEST_FRIENDS_PACKET.getId();
    }
}
