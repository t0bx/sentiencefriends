package de.t0bx.sentiencefriends.lobby.inventory.inventories;

import de.t0bx.sentiencefriends.api.data.FriendsData;
import de.t0bx.sentiencefriends.lobby.friends.FriendsManager;
import de.t0bx.sentiencefriends.lobby.inventory.InventoryProvider;
import de.t0bx.sentiencefriends.lobby.utils.ItemProvider;
import de.t0bx.sentiencefriends.lobby.utils.PaginationUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class FriendsMenuInventory {

    private static final int[] CONTENT_SLOTS = {
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35
    };

    private static final int[] BORDER_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            36, 37, 38, 39, 40, 41, 42, 43, 44
    };

    private static final int SIZE = 9 * 6;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_INFO = 46;
    private static final int SLOT_NEXT = 47;

    private final InventoryProvider inventoryProvider;
    private final FriendsManager friendsManager;

    public FriendsMenuInventory(InventoryProvider inventoryProvider, FriendsManager friendsManager) {
        this.inventoryProvider = inventoryProvider;
        this.friendsManager = friendsManager;
    }

    public void open(Player player, int page) {
        final Inventory inventory = this.inventoryProvider.getInventory(
                player,
                "default_friends_menu",
                SIZE,
                "<gray>» <green>Friends"
        );
        inventory.clear();

        this.inventoryProvider.setPlaceHolder(inventory, Material.BLACK_STAINED_GLASS_PANE, BORDER_SLOTS);

        List<ItemStack> onlineFriends = getOnlineFriends(player.getUniqueId());
        List<ItemStack> offlineFriends = getOfflineFriends(player.getUniqueId());

        int pageSize = CONTENT_SLOTS.length;
        int totalOnline = onlineFriends.size();
        int totalOffline = offlineFriends.size();
        int total = totalOnline + totalOffline;

        int maxPage = Math.max(1, PaginationUtil.pageCount(total, pageSize));
        int currentPage = PaginationUtil.clamp(page, 1, maxPage);
        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, total);

        if (total == 0) {
            inventory.setItem(22, new ItemProvider(Material.BARRIER)
                    .setName("<red>No friends found!")
                    .build());
        } else {
            int slotIndex = 0;
            for (int i = start; i < end; i++) {
                int slot = CONTENT_SLOTS[slotIndex++];

                ItemStack item;
                if (i < totalOnline) {
                    item = onlineFriends.get(i);
                } else {
                    int offlineIndex = i - totalOnline;
                    item = offlineFriends.get(offlineIndex);
                }
                inventory.setItem(slot, item);
            }
        }

        if (currentPage > 1) {
            ItemStack previous = ItemProvider.createCustomSkull("http://textures.minecraft.net/texture/bd69e06e5dadfd84e5f3d1c21063f2553b2fa945ee1d4d7152fdc5425bc12a9")
                    .setName("<yellow>« Previous Page")
                    .setPersistentDataInt("friends", "page", currentPage - 1)
                    .build();
            inventory.setItem(SLOT_PREV, previous);
        }

        ItemStack info = new ItemProvider(Material.PAPER)
                .setName("<green>Page " + currentPage + " of " + maxPage)
                .build();
        inventory.setItem(SLOT_INFO, info);

        if (currentPage < maxPage) {
            ItemStack next = ItemProvider.createCustomSkull("http://textures.minecraft.net/texture/19bf3292e126a105b54eba713aa1b152d541a1d8938829c56364d178ed22bf")
                    .setName("<yellow>» Next Page")
                    .setPersistentDataInt("friends", "page", currentPage + 1)
                    .build();
            inventory.setItem(SLOT_NEXT, next);
        }

        inventory.setItem(53, new ItemProvider(Material.REPEATER).setName("<red>Settings").build());

        player.openInventory(inventory);
    }


    private List<ItemStack> getOnlineFriends(UUID uuid) {
        List<ItemStack> items = new ArrayList<>();
        List<FriendsData.Friend> friends = this.friendsManager.getFriendsData()
                .get(uuid)
                .getFriends()
                .values()
                .stream()
                .filter(FriendsData.Friend::isOnline)
                .toList();

        for (FriendsData.Friend friend : friends) {
            final String name = friend.getCachedName();
            final boolean favorite = friend.isFavorite();

            ItemStack friendItem = ItemProvider.createPlayerSkull(name)
                    .setName(favorite ? "<yellow>★ <green>" + name : "<green>" + name)
                    .setLore("§eFriends since " + friend.getSinceFormatted())
                    .setPersistentData("friends", "friend", friend.getUuid().toString())
                    .build();

            items.add(friendItem);
        }

        return items;
    }

    private List<ItemStack> getOfflineFriends(UUID uuid) {
        List<ItemStack> items = new ArrayList<>();
        List<FriendsData.Friend> friends = this.friendsManager.getFriendsData()
                .get(uuid)
                .getFriends()
                .values()
                .stream()
                .filter(friend -> !friend.isOnline())
                .sorted(Comparator.comparingLong(FriendsData.Friend::getLastOnline).reversed())
                .toList();

        for (FriendsData.Friend friend : friends) {
            final String name = friend.getCachedName();
            final boolean favorite = friend.isFavorite();

            ItemStack friendItem = new ItemProvider(Material.SKELETON_SKULL)
                    .setName(favorite ? "<yellow>★ <green>" + name : "<green>" + name)
                    .setLore("§eFriends since: " + friend.getSinceFormatted(), "§eLast seen: " + friend.getLastOnlineTime())
                    .setPersistentData("friends", "friend", friend.getUuid().toString())
                    .build();

            items.add(friendItem);
        }

        return items;
    }
}
