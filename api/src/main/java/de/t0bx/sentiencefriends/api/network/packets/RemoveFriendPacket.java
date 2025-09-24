package de.t0bx.sentiencefriends.api.network.packets;

import de.t0bx.sentiencefriends.api.netty.PacketType;
import de.t0bx.sentiencefriends.api.netty.utils.ByteBufHelper;
import de.t0bx.sentiencefriends.api.network.FriendsPacket;
import io.netty.buffer.ByteBuf;
import lombok.Getter;

import java.util.UUID;

@Getter
public class RemoveFriendPacket implements FriendsPacket {

    private UUID uuid;
    private UUID friend;

    public RemoveFriendPacket() {}

    public RemoveFriendPacket(UUID uuid, UUID friend) {
        this.uuid = uuid;
        this.friend = friend;
    }

    @Override
    public void read(ByteBuf buf) {
        this.uuid = ByteBufHelper.readUUID(buf);
        this.friend = ByteBufHelper.readUUID(buf);
    }

    @Override
    public void write(ByteBuf buf) {
        ByteBufHelper.writeUUID(buf, uuid);
        ByteBufHelper.writeUUID(buf, friend);
    }

    @Override
    public int getId() {
        return PacketType.REMOVE_FRIEND_PACKET.getId();
    }
}
