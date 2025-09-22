package de.t0bx.sentiencefriends.api.network.packets;

import de.t0bx.sentiencefriends.api.netty.PacketType;
import de.t0bx.sentiencefriends.api.netty.utils.ByteBufHelper;
import de.t0bx.sentiencefriends.api.network.FriendsPacket;
import io.netty.buffer.ByteBuf;
import lombok.Getter;

import java.util.UUID;

@Getter
public class UpdateSettingsPacket implements FriendsPacket {

    private UUID uuid;
    private String settingType;
    private boolean value;

    public UpdateSettingsPacket() {}

    public UpdateSettingsPacket(UUID uuid, String settingType, boolean value) {
        this.uuid = uuid;
        this.settingType = settingType;
        this.value = value;
    }

    @Override
    public void read(ByteBuf buf) {
        this.uuid = ByteBufHelper.readUUID(buf);
        this.settingType = ByteBufHelper.readString(buf);
        this.value = buf.readBoolean();
    }

    @Override
    public void write(ByteBuf buf) {
        ByteBufHelper.writeUUID(buf, uuid);
        ByteBufHelper.writeString(buf, settingType);
        buf.writeBoolean(value);
    }

    @Override
    public int getId() {
        return PacketType.UPDATE_SETTINGS_PACKET.getId();
    }
}
