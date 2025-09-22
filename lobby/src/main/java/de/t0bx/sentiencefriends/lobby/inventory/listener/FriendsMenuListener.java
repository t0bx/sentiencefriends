package de.t0bx.sentiencefriends.lobby.inventory.listener;

import de.t0bx.sentiencefriends.lobby.friends.FriendsManager;
import de.t0bx.sentiencefriends.lobby.inventory.InventoryProvider;
import de.t0bx.sentiencefriends.lobby.inventory.inventories.FriendsMenuInventory;
import de.t0bx.sentiencefriends.lobby.inventory.inventories.FriendsSettingsInventory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.persistence.PersistentDataType;

public class FriendsMenuListener implements Listener {

    private final FriendsManager friendsManager;
    private final MiniMessage miniMessage;
    private final FriendsMenuInventory friendsMenuInventory;
    private final FriendsSettingsInventory friendsSettingsInventory;
    private final NamespacedKey pageKey;
    private final Component friendsMenuTitle;

    public FriendsMenuListener(FriendsManager friendsManager, InventoryProvider inventoryProvider) {
        this.friendsManager = friendsManager;
        this.miniMessage = MiniMessage.miniMessage();
        this.friendsMenuInventory = inventoryProvider.getFriendsMenuInventory();
        this.friendsSettingsInventory = inventoryProvider.getFriendsSettingsInventory();
        this.pageKey = new NamespacedKey("friends", "page");
        this.friendsMenuTitle = this.miniMessage.deserialize("<gray>» <green>Friends");
    }

    @EventHandler
    public void onFriendMenuClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        if (!event.getView().title().equals(this.friendsMenuTitle)) return;
        if (event.getCurrentItem() == null) return;
        if (event.getCurrentItem().getItemMeta() == null) return;

        event.setCancelled(true);

        if (event.getSlot() == 45) {
            int nextPage = event.getCurrentItem().getItemMeta().getPersistentDataContainer().getOrDefault(this.pageKey, PersistentDataType.INTEGER, 1);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            this.friendsMenuInventory.open(player, nextPage);
            return;
        }

        if (event.getSlot() == 47) {
            int nextPage = event.getCurrentItem().getItemMeta().getPersistentDataContainer().getOrDefault(this.pageKey, PersistentDataType.INTEGER, 1);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            this.friendsMenuInventory.open(player, nextPage);
            return;
        }

        if (event.getSlot() == 53) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            this.friendsSettingsInventory.open(player);
        }
    }
}
