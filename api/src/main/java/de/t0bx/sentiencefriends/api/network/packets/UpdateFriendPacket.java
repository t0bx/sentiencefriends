package de.t0bx.sentiencefriends.api.network.packets;

import de.t0bx.sentiencefriends.api.data.FriendsData;
import de.t0bx.sentiencefriends.api.data.UpdateType;
import de.t0bx.sentiencefriends.api.netty.PacketType;
import de.t0bx.sentiencefriends.api.netty.utils.ByteBufHelper;
import de.t0bx.sentiencefriends.api.network.FriendsPacket;
import io.netty.buffer.ByteBuf;
import lombok.Getter;

import java.util.UUID;

@Getter
public class UpdateFriendPacket implements FriendsPacket {

    private UUID uuid;
    private UpdateType updateType;
    private FriendsData.Friend friend;

    public UpdateFriendPacket() {}

    public UpdateFriendPacket(UUID uuid, UpdateType updateType, FriendsData.Friend friend) {
        this.uuid = uuid;
        this.updateType = updateType;
        this.friend = friend;
    }

    @Override
    public void read(ByteBuf buf) {
        this.uuid = ByteBufHelper.readUUID(buf);
        this.updateType = UpdateType.valueOf(ByteBufHelper.readString(buf));
        this.friend = ByteBufHelper.readFriend(buf);
    }

    @Override
    public void write(ByteBuf buf) {
        ByteBufHelper.writeUUID(buf, uuid);
        ByteBufHelper.writeString(buf, updateType.name());
        ByteBufHelper.writeFriend(buf, friend);
    }

    @Override
    public int getId() {
        return PacketType.UPDATE_FRIEND_PACKET.getId();
    }
}
