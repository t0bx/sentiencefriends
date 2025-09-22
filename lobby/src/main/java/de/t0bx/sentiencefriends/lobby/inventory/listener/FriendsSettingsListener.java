package de.t0bx.sentiencefriends.lobby.inventory.listener;

import de.t0bx.sentiencefriends.api.data.FriendsData;
import de.t0bx.sentiencefriends.api.network.packets.UpdateSettingsPacket;
import de.t0bx.sentiencefriends.lobby.LobbyPlugin;
import de.t0bx.sentiencefriends.lobby.friends.FriendsManager;
import de.t0bx.sentiencefriends.lobby.inventory.InventoryProvider;
import de.t0bx.sentiencefriends.lobby.inventory.inventories.FriendsMenuInventory;
import de.t0bx.sentiencefriends.lobby.netty.NettyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
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

public class FriendsSettingsListener implements Listener {

    private final NettyManager nettyManager;
    private final FriendsManager friendsManager;
    private final FriendsMenuInventory friendsMenuInventory;
    private final MiniMessage miniMessage;
    private final NamespacedKey settingKey;
    private final Component friendsMenuTitle;

    private final Map<UUID, Long> cooldownMap;

    public FriendsSettingsListener(NettyManager nettyManager, FriendsManager friendsManager, InventoryProvider inventoryProvider) {
        this.nettyManager = nettyManager;
        this.friendsManager = friendsManager;
        this.miniMessage = MiniMessage.miniMessage();
        this.friendsMenuInventory = inventoryProvider.getFriendsMenuInventory();
        this.settingKey = new NamespacedKey("friends", "setting");
        this.friendsMenuTitle = miniMessage.deserialize("<gray>» <green>Friends <dark_gray>→ <red>Settings");
        this.cooldownMap = new ConcurrentHashMap<>();
    }

    @EventHandler
    public void onFriendSettingClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        if (!event.getView().title().equals(this.friendsMenuTitle)) return;
        if (event.getCurrentItem() == null) return;
        if (event.getCurrentItem().getItemMeta() == null) return;

        event.setCancelled(true);

        if (event.getSlot() == 18) {
            this.friendsMenuInventory.open(player, 1);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return;
        }

        String key = event.getCurrentItem().getItemMeta().getPersistentDataContainer().getOrDefault(this.settingKey, PersistentDataType.STRING, "empty");
        if (key.equals("empty")) return;

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 1.0f);
        event.getView().close();

        long cooldown = this.cooldownMap.getOrDefault(player.getUniqueId(), 0L);
        if (cooldown > System.currentTimeMillis()) {
            player.sendMessage(this.miniMessage.deserialize("<red>Please wait before changing your settings again!"));
            return;
        }

        FriendsData.SettingType settingType = FriendsData.SettingType.fromKey(key);
        FriendsData.Settings settings = this.friendsManager.getFriendsData().get(player.getUniqueId()).getSettings();

        boolean newValue = switch (settingType) {
            case JUMP -> !settings.isJumpEnabled();
            case NOTIFICATIONS -> !settings.isNotificationsEnabled();
            case REQUESTS -> !settings.isRequestsEnabled();
        };

        switch (settingType) {
            case JUMP -> settings.setJumpEnabled(newValue);
            case NOTIFICATIONS -> settings.setNotificationsEnabled(newValue);
            case REQUESTS -> settings.setRequestsEnabled(newValue);
        }

        String updatedValue = newValue ? "<green>enabled" : "<red>disabled";
        String updatedMessage = switch (settingType) {
            case JUMP -> "Jumping is now " + updatedValue;
            case NOTIFICATIONS -> "Notifications are now " + updatedValue;
            case REQUESTS -> "Requests are now " + updatedValue;
        };

        player.sendMessage(this.miniMessage.deserialize(LobbyPlugin.getInstance().getPrefix() + updatedMessage));

        var updatePacket = new UpdateSettingsPacket(player.getUniqueId(), key, newValue);
        this.nettyManager.getChannel().writeAndFlush(updatePacket);
        this.cooldownMap.put(player.getUniqueId(), System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5));
    }
}
