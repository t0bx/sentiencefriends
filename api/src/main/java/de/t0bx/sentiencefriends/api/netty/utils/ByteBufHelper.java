package de.t0bx.sentiencefriends.api.netty.utils;

import de.t0bx.sentiencefriends.api.data.FriendsData;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ByteBufHelper {
    public static void writeVarInt(ByteBuf buf, int value) {
        while ((value & -128) != 0) {
            buf.writeByte((value & 127) | 128);
            value >>>= 7;
        }
        buf.writeByte(value);
    }

    public static int readVarInt(ByteBuf buf) {
        int numRead = 0;
        int result = 0;
        byte read;

        do {
            if (numRead >= 5) {
                throw new RuntimeException("VarInt is too big");
            }

            read = buf.readByte();
            int value = (read & 0b01111111);
            result |= (value << (7 * numRead));

            numRead++;
        } while ((read & 0b10000000) != 0);

        return result;
    }

    public static void writeUUID(ByteBuf buf, UUID uuid) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
    }

    public static void writeString(ByteBuf buf, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    public static UUID readUUID(ByteBuf buf) {
        long most = buf.readLong();
        long least = buf.readLong();
        return new UUID(most, least);
    }

    public static String readString(ByteBuf buf) {
        int length = readVarInt(buf);
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static <T> void writeList(ByteBuf buf, List<T> list, BiConsumer<ByteBuf, T> writer) {
        writeVarInt(buf, list.size());
        for (T element : list) {
            writer.accept(buf, element);
        }
    }

    public static <T> List<T> readList(ByteBuf buf, Function<ByteBuf, T> reader) {
        int size = readVarInt(buf);
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(reader.apply(buf));
        }
        return list;
    }

    public static void writeFriendsData(ByteBuf buf, FriendsData data) {
        ByteBufHelper.writeUUID(buf, data.uuid);

        buf.writeBoolean(data.getSettings().isRequestsEnabled());
        buf.writeBoolean(data.getSettings().isNotificationsEnabled());
        buf.writeBoolean(data.getSettings().isJumpEnabled());

        List<FriendsData.Friend> snapshot = new ArrayList<>(data.getFriends().values());
        ByteBufHelper.writeVarInt(buf, snapshot.size());
        for (FriendsData.Friend friend : snapshot) {
            ByteBufHelper.writeUUID(buf, friend.getUuid());
            ByteBufHelper.writeString(buf, friend.getCachedName() == null ? "" : friend.getCachedName());
            buf.writeLong(friend.getSince());
            buf.writeLong(friend.getLastOnline());
            buf.writeBoolean(friend.isFavorite());
            buf.writeBoolean(friend.isOnline());
        }

        List<UUID> inSnap = new ArrayList<>(data.getIncomingRequests());
        writeList(buf, inSnap, ByteBufHelper::writeUUID);

        List<UUID> outSnap = new ArrayList<>(data.getOutgoingRequests());
        writeList(buf, outSnap, ByteBufHelper::writeUUID);
    }

    public static FriendsData readFriendsData(ByteBuf buf) {
        UUID uuid = ByteBufHelper.readUUID(buf);
        FriendsData data = new FriendsData(uuid);

        FriendsData.Settings settings = data.getSettings();
        settings.setRequestsEnabled(buf.readBoolean());
        settings.setNotificationsEnabled(buf.readBoolean());
        settings.setJumpEnabled(buf.readBoolean());

        int friendsSize = ByteBufHelper.readVarInt(buf);
        for (int i = 0; i < friendsSize; i++) {
            UUID friendUuid = ByteBufHelper.readUUID(buf);
            String cachedName = ByteBufHelper.readString(buf);
            if (cachedName.isEmpty()) cachedName = null;
            long since = buf.readLong();
            long lastOnline = buf.readLong();
            boolean favorite = buf.readBoolean();
            boolean online = buf.readBoolean();

            FriendsData.Friend friend = new FriendsData.Friend(friendUuid, cachedName, since);
            friend.setLastOnline(lastOnline);
            friend.setFavorite(favorite);
            friend.setOnline(online);

            data.getFriends().put(friendUuid, friend);
        }

        List<UUID> incoming = ByteBufHelper.readList(buf, ByteBufHelper::readUUID);
        data.getIncomingRequests().addAll(incoming);

        List<UUID> outgoing = ByteBufHelper.readList(buf, ByteBufHelper::readUUID);
        data.getOutgoingRequests().addAll(outgoing);

        return data;
    }
}
