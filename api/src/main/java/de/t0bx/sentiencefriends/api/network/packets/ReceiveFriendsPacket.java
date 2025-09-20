package de.t0bx.sentiencefriends.api.network.packets;

import de.t0bx.sentiencefriends.api.data.FriendsData;
import de.t0bx.sentiencefriends.api.netty.PacketType;
import de.t0bx.sentiencefriends.api.netty.utils.ByteBufHelper;
import de.t0bx.sentiencefriends.api.network.FriendsPacket;
import io.netty.buffer.ByteBuf;
import lombok.Getter;

import java.util.UUID;

@Getter
public class ReceiveFriendsPacket implements FriendsPacket {

    private UUID uuid;
    private FriendsData friendsData;

    public ReceiveFriendsPacket() {}

    public ReceiveFriendsPacket(UUID uuid, FriendsData friendsData) {
        this.uuid = uuid;
        this.friendsData = friendsData;
    }

    @Override
    public void read(ByteBuf buf) {
        this.uuid = ByteBufHelper.readUUID(buf);

        this.friendsData = ByteBufHelper.readFriendsData(buf);
    }

    @Override
    public void write(ByteBuf buf) {
        ByteBufHelper.writeVarInt(buf, getId());

        ByteBufHelper.writeUUID(buf, uuid);
        ByteBufHelper.writeFriendsData(buf, friendsData);
    }

    @Override
    public int getId() {
        return PacketType.RECEIVE_FRIENDS_PACKET.getId();
    }
}
