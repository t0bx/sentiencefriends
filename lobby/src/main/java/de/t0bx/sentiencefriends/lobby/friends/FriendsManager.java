package de.t0bx.sentiencefriends.lobby.friends;

import de.t0bx.sentiencefriends.api.data.FriendsData;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class FriendsManager {

    private final Map<UUID, FriendsData> friendsData;

    public FriendsManager() {
        this.friendsData = new ConcurrentHashMap<>();
    }
}
