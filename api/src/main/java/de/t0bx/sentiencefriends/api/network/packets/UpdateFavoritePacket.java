package de.t0bx.sentiencefriends.api.network.packets;

import de.t0bx.sentiencefriends.api.netty.PacketType;
import de.t0bx.sentiencefriends.api.netty.utils.ByteBufHelper;
import de.t0bx.sentiencefriends.api.network.FriendsPacket;
import io.netty.buffer.ByteBuf;
import lombok.Getter;

import java.util.UUID;

@Getter
public class UpdateFavoritePacket implements FriendsPacket {

    private UUID player;
    private UUID target;
    private boolean favorite;

    public UpdateFavoritePacket() {}

    public UpdateFavoritePacket(UUID player, UUID target, boolean favorite) {
        this.player = player;
        this.target = target;
        this.favorite = favorite;
    }

    @Override
    public void read(ByteBuf buf) {
        this.player = ByteBufHelper.readUUID(buf);
        this.target = ByteBufHelper.readUUID(buf);
        this.favorite = buf.readBoolean();
    }

    @Override
    public void write(ByteBuf buf) {
        ByteBufHelper.writeUUID(buf, player);
        ByteBufHelper.writeUUID(buf, target);
        buf.writeBoolean(favorite);
    }

    @Override
    public int getId() {
        return PacketType.UPDATE_FAVORITE_PACKET.getId();
    }
}
