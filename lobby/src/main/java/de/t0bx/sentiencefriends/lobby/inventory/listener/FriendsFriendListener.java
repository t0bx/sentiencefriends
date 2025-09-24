package de.t0bx.sentiencefriends.lobby.inventory.listener;

import de.t0bx.sentiencefriends.api.data.FriendsData;
import de.t0bx.sentiencefriends.api.network.packets.RemoveFriendPacket;
import de.t0bx.sentiencefriends.api.network.packets.RequestJumpPacket;
import de.t0bx.sentiencefriends.api.network.packets.UpdateFavoritePacket;
import de.t0bx.sentiencefriends.api.network.packets.UpdateSettingsPacket;
import de.t0bx.sentiencefriends.lobby.LobbyPlugin;
import de.t0bx.sentiencefriends.lobby.friends.FriendsManager;
import de.t0bx.sentiencefriends.lobby.inventory.InventoryProvider;
import de.t0bx.sentiencefriends.lobby.inventory.inventories.FriendsFriendInventory;
import de.t0bx.sentiencefriends.lobby.inventory.inventories.FriendsMenuInventory;
import de.t0bx.sentiencefriends.lobby.inventory.inventories.FriendsSettingsInventory;
import de.t0bx.sentiencefriends.lobby.netty.NettyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class FriendsFriendListener implements Listener {

    private final NettyManager nettyManager;
    private final FriendsManager friendsManager;
    private final FriendsMenuInventory friendsMenuInventory;
    private final FriendsFriendInventory friendsFriendInventory;
    private final MiniMessage miniMessage;
    private final Component friendsMenuTitle;
    private final Component friendDeleteTitle;
    private final NamespacedKey friendKey;
    private final Map<UUID, Long> cooldownMap;

    public FriendsFriendListener(NettyManager nettyManager, FriendsManager friendsManager, InventoryProvider inventoryProvider) {
        this.nettyManager = nettyManager;
        this.friendsManager = friendsManager;
        this.miniMessage = MiniMessage.miniMessage();
        this.friendsMenuInventory = inventoryProvider.getFriendsMenuInventory();
        this.friendsFriendInventory = inventoryProvider.getFriendsFriendInventory();
        this.friendsMenuTitle = miniMessage.deserialize("<gray>» <green>Friends <dark_gray>→ <red>Friend");
        this.friendDeleteTitle = miniMessage.deserialize("<gray>» <green>Friends <dark_gray>→ <red>Remove Friend");
        this.friendKey = new NamespacedKey("friends", "friend");
        this.cooldownMap = new ConcurrentHashMap<>();
    }

    @EventHandler
    public void onFriendInventoryClick(InventoryClickEvent event) {
        final Player player = (Player) event.getWhoClicked();

        if (!event.getView().title().equals(this.friendsMenuTitle)) return;
        if (event.getCurrentItem() == null) return;
        if (event.getCurrentItem().getItemMeta() == null) return;

        event.setCancelled(true);

        if (event.getSlot() == 18) {
            this.friendsMenuInventory.open(player, 1);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return;
        }

        if (event.getCurrentItem().getType() == Material.NETHER_STAR) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 1.0f);
            event.getView().close();

            long cooldown = this.cooldownMap.getOrDefault(player.getUniqueId(), 0L);
            if (cooldown > System.currentTimeMillis()) {
                player.sendMessage(this.miniMessage.deserialize("<red>Please wait before changing favorite friends again!"));
                return;
            }

            UUID friendUuid = UUID.fromString(event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(this.friendKey, PersistentDataType.STRING));
            FriendsData.Friend friend = this.friendsManager.getFriendsData().get(player.getUniqueId()).getFriends().getOrDefault(friendUuid, null);
            if (friend == null) return;

            boolean newValue = !friend.isFavorite();

            friend.setFavorite(newValue);

            String updatedMessage = newValue
                    ? "<green>" + friend.getCachedName() + " is now your favorite friend!"
                    : "<red>" + friend.getCachedName() + " is no longer your favorite friend!";

            player.sendMessage(this.miniMessage.deserialize(LobbyPlugin.getInstance().getPrefix() + updatedMessage));

            var updatePacket = new UpdateFavoritePacket(player.getUniqueId(), friendUuid, newValue);
            this.nettyManager.getChannel().writeAndFlush(updatePacket);
            this.cooldownMap.put(player.getUniqueId(), System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5));
            return;
        }

        if (event.getCurrentItem().getType() == Material.ENDER_PEARL) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 1.0f);
            event.getView().close();

            UUID friendUuid = UUID.fromString(event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(this.friendKey, PersistentDataType.STRING));

            var updatePacket = new RequestJumpPacket(player.getUniqueId(), friendUuid);
            this.nettyManager.getChannel().writeAndFlush(updatePacket);
            return;
        }

        if (event.getCurrentItem().getType() == Material.REDSTONE) {
            UUID friendUuid = UUID.fromString(event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(this.friendKey, PersistentDataType.STRING));
            FriendsData.Friend friend = this.friendsManager.getFriendsData().get(player.getUniqueId()).getFriends().getOrDefault(friendUuid, null);
            if (friend == null) return;

            this.friendsFriendInventory.openConfirmDelete(player, friend);
        }
    }

    @EventHandler
    public void onFriendConfirmDeleteClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        if (!event.getView().title().equals(this.friendDeleteTitle)) return;
        if (event.getCurrentItem() == null) return;
        if (event.getCurrentItem().getItemMeta() == null) return;

        event.setCancelled(true);

        if (event.getSlot() == 18 || event.getCurrentItem().getType() == Material.REDSTONE) {
            this.friendsMenuInventory.open(player, 1);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return;
        }

        if (event.getCurrentItem().getType() == Material.EMERALD) {
            event.getView().close();
            UUID friendUuid = UUID.fromString(event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(this.friendKey, PersistentDataType.STRING));
            FriendsData friendsData = this.friendsManager.getFriendsData().get(player.getUniqueId());
            FriendsData.Friend friend = friendsData.getFriends().getOrDefault(friendUuid, null);
            if (friend == null) return;

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 1.0f);
            player.sendMessage(this.miniMessage.deserialize(LobbyPlugin.getInstance().getPrefix() + "<green>Friend " + friend.getCachedName() + " has been removed!"));

            friendsData.getFriends().remove(friendUuid);
            var removeFriendPacket = new RemoveFriendPacket(player.getUniqueId(), friendUuid);
            this.nettyManager.getChannel().writeAndFlush(removeFriendPacket);


            FriendsData target = this.friendsManager.getFriendsData().getOrDefault(friendUuid, null);
            if (target == null) return;

            target.getFriends().remove(player.getUniqueId());
        }
    }
}
