package de.t0bx.sentiencefriends.api.network.packets;

import de.t0bx.sentiencefriends.api.netty.PacketType;
import de.t0bx.sentiencefriends.api.netty.utils.ByteBufHelper;
import de.t0bx.sentiencefriends.api.network.FriendsPacket;
import io.netty.buffer.ByteBuf;
import lombok.Getter;

@Getter
public class ChannelIdentifyPacket implements FriendsPacket {

    private String channelName;

    public ChannelIdentifyPacket() {}

    public ChannelIdentifyPacket(String channelName) {
        this.channelName = channelName;
    }

    @Override
    public void read(ByteBuf buf) {
        this.channelName = ByteBufHelper.readString(buf);
    }

    @Override
    public void write(ByteBuf buf) {
        ByteBufHelper.writeString(buf, channelName);
    }

    @Override
    public int getId() {
        return PacketType.CHANNEL_IDENTIFY_PACKET.getId();
    }
}
