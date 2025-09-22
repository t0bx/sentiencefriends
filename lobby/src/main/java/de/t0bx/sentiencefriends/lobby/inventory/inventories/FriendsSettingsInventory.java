package de.t0bx.sentiencefriends.lobby.inventory.inventories;

import de.t0bx.sentiencefriends.api.data.FriendsData;
import de.t0bx.sentiencefriends.lobby.friends.FriendsManager;
import de.t0bx.sentiencefriends.lobby.inventory.InventoryProvider;
import de.t0bx.sentiencefriends.lobby.utils.ItemProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class FriendsSettingsInventory {

    private final InventoryProvider inventoryProvider;
    private final FriendsManager friendsManager;

    public FriendsSettingsInventory(InventoryProvider inventoryProvider, FriendsManager friendsManager) {
        this.inventoryProvider = inventoryProvider;
        this.friendsManager = friendsManager;
    }

    public void open(Player player) {
        final Inventory inventory = this.inventoryProvider.getInventory(
                player,
                "settings_friends_menu",
                9 * 3,
                "<gray>» <green>Friends <dark_gray>→ <red>Settings"
        );

        inventory.clear();

        this.inventoryProvider.setPlaceHolder(inventory, Material.BLACK_STAINED_GLASS_PANE, this.inventoryProvider.getBorderSlots(3, 9));

        FriendsData.Settings settings = this.friendsManager.getFriendsData().get(player.getUniqueId()).getSettings();

        inventory.setItem(18, new ItemProvider(Material.BARRIER)
                .setName("<red>« Go back")
                .build());

        inventory.setItem(11, new ItemProvider(Material.PAPER)
                .setName("<green>Notifications <dark_gray>» " + getSettingState(settings.isNotificationsEnabled()))
                .setPersistentData("friends", "setting", FriendsData.SettingType.NOTIFICATIONS.getKey())
                .build());

        inventory.setItem(13, new ItemProvider(Material.FIREWORK_ROCKET)
                .setName("<green>Jump <dark_gray>» " + getSettingState(settings.isJumpEnabled()))
                .setPersistentData("friends", "setting", FriendsData.SettingType.JUMP.getKey())
                .build());

        inventory.setItem(15, new ItemProvider(Material.ENDER_EYE)
                .setName("<green>Requests <dark_gray>» " + getSettingState(settings.isRequestsEnabled()))
                .setPersistentData("friends", "setting", FriendsData.SettingType.REQUESTS.getKey())
                .build());

        player.openInventory(inventory);
    }

    private String getSettingState(boolean value) {
        return value ? "<green>Enabled" : "<red>Disabled";
    }
}
