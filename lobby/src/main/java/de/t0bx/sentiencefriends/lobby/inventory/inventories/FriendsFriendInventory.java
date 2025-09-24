package de.t0bx.sentiencefriends.lobby.inventory.inventories;

import de.t0bx.sentiencefriends.api.data.FriendsData;
import de.t0bx.sentiencefriends.lobby.friends.FriendsManager;
import de.t0bx.sentiencefriends.lobby.inventory.InventoryProvider;
import de.t0bx.sentiencefriends.lobby.utils.ItemProvider;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class FriendsFriendInventory {

    private final MiniMessage miniMessage;
    private final InventoryProvider inventoryProvider;
    private final FriendsManager friendsManager;
    private final FriendsMenuInventory friendsMenuInventory;

    public FriendsFriendInventory(InventoryProvider inventoryProvider, FriendsManager friendsManager) {
        this.miniMessage = MiniMessage.miniMessage();
        this.inventoryProvider = inventoryProvider;
        this.friendsManager = friendsManager;
        this.friendsMenuInventory = inventoryProvider.getFriendsMenuInventory();
    }

    public void open(Player player, FriendsData.Friend friend) {
        final Inventory inventory = this.inventoryProvider.getInventory(
                player,
                "friend_friends_menu",
                9 * 3,
                "<gray>» <green>Friends <dark_gray>→ <red>Friend"
        );

        inventory.clear();

        setBasics(inventory, friend);

        String name = friend.isFavorite() ? "<red>Unfavorite" : "<green>Favorite";
        inventory.setItem(11, new ItemProvider(Material.NETHER_STAR)
                .setName(name)
                .setPersistentData("friends", "friend", friend.getUuid().toString())
                .build());

        inventory.setItem(13, new ItemProvider(Material.ENDER_PEARL)
                .setName("<green>Jump to Friend")
                .setPersistentData("friends", "friend", friend.getUuid().toString())
                .build());

        inventory.setItem(15, new ItemProvider(Material.REDSTONE)
                .setName("<red>Remove Friend")
                .setPersistentData("friends", "friend", friend.getUuid().toString())
                .build());

        player.openInventory(inventory);
    }

    public void openConfirmDelete(Player player, FriendsData.Friend friend) {
        final Inventory inventory = this.inventoryProvider.getInventory(
                player,
                "friend_friends_menu_delete",
                9 * 3,
                "<gray>» <green>Friends <dark_gray>→ <red>Remove Friend"
        );

        inventory.clear();

        setBasics(inventory, friend);

        inventory.setItem(12, new ItemProvider(Material.EMERALD)
                .setName("<green>Remove Friend")
                .setPersistentData("friends", "friend", friend.getUuid().toString())
                .build());

        inventory.setItem(14, new ItemProvider(Material.REDSTONE)
                .setName("<red>Cancel")
                .build());

        player.openInventory(inventory);
    }

    private void setBasics(Inventory inventory, FriendsData.Friend friend) {
        this.inventoryProvider.setPlaceHolder(inventory, Material.BLACK_STAINED_GLASS_PANE, this.inventoryProvider.getBorderSlots(3, 9));

        ItemProvider playerHead;
        if (friend.isOnline()) {
            playerHead = ItemProvider.createPlayerSkull(friend.getCachedName());
            playerHead.setLore("§aOnline", "§eFriends since " + friend.getSinceFormatted());
        } else {
            playerHead = new ItemProvider(Material.SKELETON_SKULL);
            playerHead.setLore("§eFriends since: " + friend.getSinceFormatted(), "§eLast seen: " + friend.getLastOnlineTime());
        }

        playerHead.setName("<green>" + friend.getCachedName());

        inventory.setItem(4, playerHead.build());

        inventory.setItem(18, new ItemProvider(Material.BARRIER)
                .setName("<red>« Go back")
                .build());
    }
}
