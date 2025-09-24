package de.t0bx.sentiencefriends.lobby.inventory.listener;

import de.t0bx.sentiencefriends.lobby.friends.FriendsManager;
import de.t0bx.sentiencefriends.lobby.inventory.InventoryProvider;
import de.t0bx.sentiencefriends.lobby.inventory.inventories.FriendsMenuInventory;
import de.t0bx.sentiencefriends.lobby.inventory.inventories.FriendsSettingsInventory;
import de.t0bx.sentiencefriends.lobby.netty.NettyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FriendsFriendListener implements Listener {

    private final NettyManager nettyManager;
    private final FriendsManager friendsManager;
    private final FriendsMenuInventory friendsMenuInventory;
    private final MiniMessage miniMessage;
    private final Component friendsMenuTitle;

    private final Map<UUID, Long> cooldownMap;

    public FriendsFriendListener(NettyManager nettyManager, FriendsManager friendsManager, InventoryProvider inventoryProvider) {
        this.nettyManager = nettyManager;
        this.friendsManager = friendsManager;
        this.miniMessage = MiniMessage.miniMessage();
        this.friendsMenuInventory = inventoryProvider.getFriendsMenuInventory();
        this.friendsMenuTitle = miniMessage.deserialize("<gray>» <green>Friends <dark_gray>→ <red>Friend");
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

        }

        if (event.getCurrentItem().getType() == Material.ENDER_PEARL) {

        }

        if (event.getCurrentItem().getType() == Material.REDSTONE) {

        }
    }
}
