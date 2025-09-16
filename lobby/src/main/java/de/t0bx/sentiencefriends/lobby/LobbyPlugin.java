package de.t0bx.sentiencefriends.lobby;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class LobbyPlugin extends JavaPlugin {

    @Getter
    private static LobbyPlugin instance;

    @Override
    public void onEnable() {
        instance = this;

        this.getLogger().info("LobbyPlugin enabled!");
    }

    @Override
    public void onDisable() {
        this.getLogger().info("LobbyPlugin disabled!");
    }
}
