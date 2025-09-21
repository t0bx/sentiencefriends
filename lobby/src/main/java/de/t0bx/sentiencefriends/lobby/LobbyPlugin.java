package de.t0bx.sentiencefriends.lobby;

import de.t0bx.sentiencefriends.lobby.friends.FriendsManager;
import de.t0bx.sentiencefriends.lobby.inventory.InventoryProvider;
import de.t0bx.sentiencefriends.lobby.listener.PlayerInteractListener;
import de.t0bx.sentiencefriends.lobby.listener.PlayerJoinListener;
import de.t0bx.sentiencefriends.lobby.listener.PlayerQuitListener;
import de.t0bx.sentiencefriends.lobby.netty.NettyManager;
import lombok.Getter;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public class LobbyPlugin extends JavaPlugin {

    @Getter
    private static LobbyPlugin instance;

    private FriendsManager friendsManager;
    private NettyManager nettyManager;

    private InventoryProvider inventoryProvider;

    private final String channelName = this.getRandomChannelIdentifier();

    @Override
    public void onEnable() {
        instance = this;

        this.friendsManager = new FriendsManager();
        this.nettyManager = new NettyManager("localhost", 1339);

        this.inventoryProvider = new InventoryProvider();

        registerListener();

        this.getLogger().info("LobbyPlugin enabled!");
    }

    private void registerListener() {
        PluginManager pluginManager = this.getServer().getPluginManager();

        pluginManager.registerEvents(new PlayerJoinListener(this.nettyManager), this);
        pluginManager.registerEvents(new PlayerQuitListener(this.friendsManager), this);
        pluginManager.registerEvents(new PlayerInteractListener(this.inventoryProvider, this.friendsManager), this);
    }

    @Override
    public void onDisable() {
        this.nettyManager.close();

        this.getLogger().info("LobbyPlugin disabled!");
    }

    public String getRandomChannelIdentifier() {
        return "ChannelLobby";
    }
}
