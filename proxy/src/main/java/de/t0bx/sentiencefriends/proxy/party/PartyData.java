package de.t0bx.sentiencefriends.proxy.party;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
public class PartyData {
    private final UUID leader;
    private Set<UUID> members = new HashSet<>();
    private boolean isPublic = false;
    private boolean isChatDisabled = false;
}
