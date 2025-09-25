package de.t0bx.sentiencefriends.proxy.party;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.t0bx.sentiencefriends.proxy.ProxyPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PartyManager {

    private final ProxyServer proxyServer;
    private final Map<UUID, PartyData> parties;
    private final Map<UUID, PartyData> partyMembersMap;
    private final Map<UUID, UUID> pendingInvites;

    public PartyManager(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
        this.parties = new ConcurrentHashMap<>();
        this.partyMembersMap = new ConcurrentHashMap<>();
        this.pendingInvites = new ConcurrentHashMap<>();
    }

    public void createParty(UUID leader) {
        this.parties.put(leader, new PartyData(leader));
    }

    public PartyData getParty(UUID leader) {
        return this.parties.getOrDefault(leader, null);
    }

    public void deleteParty(UUID leader) {
        PartyData partyData = this.parties.getOrDefault(leader, null);
        if (partyData == null) return;
        partyData.getMembers().forEach(this.partyMembersMap::remove);
        partyData.getMembers().clear();

        this.parties.remove(leader);
    }

    public boolean isPartyLeader(UUID uuid) {
        return this.parties.containsKey(uuid);
    }

    public Collection<PartyData> getParties() {
        return this.parties.values();
    }

    public Set<UUID> getPartyMembers(UUID leader) {
        PartyData partyData = this.parties.getOrDefault(leader, null);
        if (partyData == null) return null;

        return partyData.getMembers();
    }

    public Set<UUID> getPartyMembersWithLeader(UUID leader) {
        PartyData partyData = this.parties.getOrDefault(leader, null);
        if (partyData == null) return null;

        Set<UUID> members = new HashSet<>(partyData.getMembers());
        members.add(leader);
        return members;
    }

    public void invitePartyMember(UUID leader, UUID member) {
        this.pendingInvites.put(member, leader);
    }

    public void acceptPartyInvite(UUID member) {
        UUID leader = this.pendingInvites.remove(member);
        if (leader == null) return;

        PartyData partyData = this.parties.getOrDefault(leader, null);
        if (partyData == null) return;

        partyData.getMembers().add(member);
        this.partyMembersMap.put(member, partyData);
    }

    public void declinePartyInvite(UUID member) {
        this.pendingInvites.remove(member);
    }

    public void sendParty(UUID leader, RegisteredServer server) {
        this.parties.getOrDefault(leader, null)
                .getMembers()
                .forEach(member -> this.proxyServer.getPlayer(member)
                        .ifPresent(player -> {
                            player.createConnectionRequest(server).fireAndForget();

                            player.sendMessage(MiniMessage.miniMessage().deserialize(
                                    ProxyPlugin.getInstance().getPartyPrefix() + "<green>The party has joined the Server " + server.getServerInfo().getName() + "."
                            ));
                        }));
    }

    public boolean hasPartyInvite(UUID member, UUID leader) {
        return this.pendingInvites.containsKey(member) && this.pendingInvites.get(member).equals(leader);
    }

    public void addPartyMember(UUID leader, UUID member) {
        PartyData partyData = this.parties.getOrDefault(leader, null);
        if (partyData == null) return;
        if (partyData.getMembers().contains(member)) return;

        partyData.getMembers().add(member);
        this.partyMembersMap.put(member, partyData);
    }

    public void removePartyMember(UUID leader, UUID member) {
        PartyData partyData = this.parties.getOrDefault(leader, null);
        if (partyData == null) return;

        partyData.getMembers().remove(member);
        this.partyMembersMap.remove(member);
    }

    public boolean isMemberOfParty(UUID uuid) {
        return this.partyMembersMap.containsKey(uuid);
    }

    public PartyData getPartyByMember(UUID uuid) {
        return this.partyMembersMap.getOrDefault(uuid, null);
    }
}
