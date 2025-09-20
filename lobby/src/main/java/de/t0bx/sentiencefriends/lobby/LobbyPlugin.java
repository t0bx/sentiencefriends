package de.t0bx.sentiencefriends.lobby;

import de.t0bx.sentiencefriends.lobby.friends.FriendsManager;
import de.t0bx.sentiencefriends.lobby.listener.PlayerJoinListener;
import de.t0bx.sentiencefriends.lobby.listener.PlayerQuitListener;
import de.t0bx.sentiencefriends.lobby.netty.NettyManager;
import lombok.Getter;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

@Getter
public class LobbyPlugin extends JavaPlugin {

    @Getter
    private static LobbyPlugin instance;

    private FriendsManager friendsManager;
    private NettyManager nettyManager;

    private final String channelName = this.getRandomChannelIdentifier();

    @Override
    public void onEnable() {
        instance = this;

        this.friendsManager = new FriendsManager();
        this.nettyManager = new NettyManager("localhost", 1337);

        registerListener();

        this.getLogger().info("LobbyPlugin enabled!");
    }

    private void registerListener() {
        PluginManager pluginManager = this.getServer().getPluginManager();

        pluginManager.registerEvents(new PlayerJoinListener(this.nettyManager), this);
        pluginManager.registerEvents(new PlayerQuitListener(this.friendsManager), this);
    }

    @Override
    public void onDisable() {
        this.getLogger().info("LobbyPlugin disabled!");
    }

    public String getRandomChannelIdentifier() {
        return UUID.randomUUID().toString();
    }
}
