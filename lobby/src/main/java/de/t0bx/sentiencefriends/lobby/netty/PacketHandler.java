package de.t0bx.sentiencefriends.lobby.netty;

import de.t0bx.sentiencefriends.api.data.FriendsData;
import de.t0bx.sentiencefriends.api.network.FriendsPacket;
import de.t0bx.sentiencefriends.api.network.packets.ReceiveFriendsPacket;
import de.t0bx.sentiencefriends.api.network.packets.UpdateFriendPacket;
import de.t0bx.sentiencefriends.lobby.LobbyPlugin;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.UUID;

public class PacketHandler extends SimpleChannelInboundHandler<FriendsPacket> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FriendsPacket friendsPacket) throws Exception {
        if (friendsPacket instanceof ReceiveFriendsPacket receiveFriendsPacket) {
            LobbyPlugin.getInstance().getFriendsManager().getFriendsData().put(
                    receiveFriendsPacket.getUuid(),
                    receiveFriendsPacket.getFriendsData()
            );
        } else if (friendsPacket instanceof UpdateFriendPacket updateFriendPacket) {
            final FriendsData friendsData = LobbyPlugin.getInstance().getFriendsManager()
                    .getFriendsData().getOrDefault(updateFriendPacket.getUuid(), null);
            if (friendsData == null) return;

            friendsData.getFriends().put(updateFriendPacket.getFriend().getUuid(), updateFriendPacket.getFriend());
        }
    }
}
