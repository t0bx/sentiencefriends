package de.t0bx.sentiencefriends.lobby.listener;

import de.t0bx.sentiencefriends.lobby.friends.FriendsManager;
import de.t0bx.sentiencefriends.lobby.inventory.InventoryProvider;
import de.t0bx.sentiencefriends.lobby.inventory.inventories.FriendsMenuInventory;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerInteractListener implements Listener {

    private final FriendsMenuInventory friendsMenuInventory;

    public PlayerInteractListener(InventoryProvider inventoryProvider, FriendsManager friendsManager) {
        this.friendsMenuInventory = new FriendsMenuInventory(inventoryProvider, friendsManager);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();

        if (event.getItem() == null) return;

        if (event.getItem().getItemMeta() == null) return;

        if (event.getItem().getItemMeta().customName().equals(MiniMessage.miniMessage().deserialize("<gray>» <green>Friends <gray>«"))) {
            event.setCancelled(true);
            this.friendsMenuInventory.open(player, 1);
        }
    }
}
