package de.t0bx.sentiencefriends.lobby.listener;

import de.t0bx.sentiencefriends.lobby.LobbyPlugin;
import de.t0bx.sentiencefriends.lobby.friends.FriendsManager;
import de.t0bx.sentiencefriends.lobby.inventory.InventoryProvider;
import de.t0bx.sentiencefriends.lobby.inventory.inventories.FriendsMenuInventory;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerInteractListener implements Listener {

    private final FriendsMenuInventory friendsMenuInventory;
    private final FriendsManager friendsManager;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public PlayerInteractListener(InventoryProvider inventoryProvider, FriendsManager friendsManager) {
        this.friendsMenuInventory = inventoryProvider.getFriendsMenuInventory();
        this.friendsManager = friendsManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();

        if (event.getItem() == null) return;
        if (event.getItem().getItemMeta() == null) return;

        if (event.getItem().getType() == Material.PLAYER_HEAD) {
            event.setCancelled(true);
            if (!this.friendsManager.getFriendsData().containsKey(player.getUniqueId())) {
                player.sendMessage(
                        this.miniMessage.deserialize(LobbyPlugin.getInstance().getPrefix() + "<red>Your friends are not loaded yet!"));
                return;
            }
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_PLACE, 0.8f, 1.0f);
            this.friendsMenuInventory.open(player, 1);
        }
    }
}
