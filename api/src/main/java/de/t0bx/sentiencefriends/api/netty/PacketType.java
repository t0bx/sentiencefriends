package de.t0bx.sentiencefriends.api.netty;

import de.t0bx.sentiencefriends.api.network.FriendsPacket;
import de.t0bx.sentiencefriends.api.network.packets.*;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public enum PacketType {
    CHANNEL_IDENTIFY_PACKET(1, ChannelIdentifyPacket::new),
    REQUEST_FRIENDS_PACKET(2, RequestFriendsPacket::new),
    RECEIVE_FRIENDS_PACKET(3, ReceiveFriendsPacket::new);


    @Getter
    private final int id;
    private final Supplier<FriendsPacket> packetSupplier;

    private static final Map<Integer, PacketType> BY_ID = new HashMap<>();
    private static final Map<Class<? extends FriendsPacket>, PacketType> BY_CLASS = new HashMap<>();

    static {
        for (PacketType type : values()) {
            BY_ID.put(type.id, type);

            FriendsPacket instance = type.packetSupplier.get();
            BY_CLASS.put(instance.getClass(), type);
        }
    }

    PacketType(int id, Supplier<FriendsPacket> packetSupplier) {
        this.id = id;
        this.packetSupplier = packetSupplier;
    }

    public FriendsPacket createPacket() {
        return packetSupplier.get();
    }

    public static PacketType getById(int id) { return BY_ID.get(id); }

    public static PacketType getByClass(Class<? extends FriendsPacket> clazz) { return BY_CLASS.get(clazz); }
}
