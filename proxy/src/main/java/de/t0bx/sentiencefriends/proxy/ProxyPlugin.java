package de.t0bx.sentiencefriends.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import de.t0bx.sentiencefriends.proxy.commands.FriendCommand;
import de.t0bx.sentiencefriends.proxy.database.DatabaseFile;
import de.t0bx.sentiencefriends.proxy.database.IMySQLManager;
import de.t0bx.sentiencefriends.proxy.database.MySQLManager;
import de.t0bx.sentiencefriends.proxy.friends.FriendsManager;
import de.t0bx.sentiencefriends.proxy.listener.PreLoginListener;
import lombok.Getter;
import org.slf4j.Logger;

import java.sql.SQLException;

@Plugin(
        id = "sentiencefriends-velocity",
        name = "SentienceFriends-Velocity",
        version = "1.0.0",
        authors = {"t0bx"}
)
@Getter
public class ProxyPlugin {

    @Getter
    private static ProxyPlugin instance;

    private final Logger logger;
    private final ProxyServer proxyServer;

    private IMySQLManager mySQLManager;
    private final FriendsManager friendsManager;

    private final String prefix = "<gradient:#00aaaa:#55ffff>Friends <dark_gray>» <gray>";

    @Inject
    public ProxyPlugin(Logger logger, ProxyServer proxyServer) {
        instance = this;

        this.logger = logger;
        this.proxyServer = proxyServer;

        DatabaseFile databaseFile = new DatabaseFile();
        if (databaseFile.isCreatedFreshly()) {
            this.logger.info("Database file got created please fill in the credentials in the database.json");
            this.proxyServer.shutdown();
        } else {
            this.mySQLManager = new MySQLManager(
                    databaseFile.getCredentials().host(),
                    databaseFile.getCredentials().port(),
                    databaseFile.getCredentials().username(),
                    databaseFile.getCredentials().password(),
                    databaseFile.getCredentials().database()
            );

            try {
                this.mySQLManager.connect();
            } catch (SQLException exception) {
                this.getLogger().error("Could not connect to database!");
                this.proxyServer.shutdown();
            }

            this.createDatabaseTables();
        }

        this.friendsManager = new FriendsManager(this);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {

        this.registerCommands();
        this.registerListener();

        this.getLogger().info("ProxyPlugin initialized!");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        try {
            if (this.mySQLManager != null && !this.mySQLManager.getConnection().isClosed()) {
                this.mySQLManager.disconnect();
            }
        } catch (SQLException exception) {
            this.getLogger().error("Could not disconnect from database!");
        }
    }

    private void registerCommands() {
        final CommandManager commandManager = proxyServer.getCommandManager();

        final CommandMeta friendCommandMeta = commandManager.metaBuilder("friend").aliases("friends").plugin(this).build();
        commandManager.register(friendCommandMeta, new FriendCommand(this, this.friendsManager));
    }

    private void registerListener() {
        final EventManager eventManager = proxyServer.getEventManager();

        eventManager.register(this, new PreLoginListener(this.friendsManager));
    }

    private void createDatabaseTables() {
        if (this.mySQLManager == null) return;

        String friendSettingsTable = """
                CREATE TABLE IF NOT EXISTS friends_settings(
                uuid VARCHAR(36) PRIMARY KEY,
                friend_requests_enabled BOOLEAN DEFAULT TRUE,
                notifications_enabled BOOLEAN DEFAULT TRUE,
                jump_enabled BOOLEAN DEFAULT TRUE);
                """;

        String friendsDataTable = """
                CREATE TABLE IF NOT EXISTS friends_data(
                uuid_player VARCHAR(36) NOT NULL,
                uuid_friend VARCHAR(36) NOT NULL,
                since BIGINT NOT NULL,
                last_online BIGINT NOT NULL,
                favorite BOOLEAN DEFAULT FALSE,
                PRIMARY KEY (uuid_player, uuid_friend));
                """;

        String friendsRequestsTable = """
                CREATE TABLE IF NOT EXISTS friends_requests(
                uuid_sender VARCHAR(36) NOT NULL,
                uuid_receiver VARCHAR(36) NOT NULL,
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (uuid_sender, uuid_receiver));
                """;

        this.mySQLManager.transactionAsync(connection -> {
            try (var friendSettingsStatement = connection.prepareStatement(friendSettingsTable);
                 var friendsDataStatement = connection.prepareStatement(friendsDataTable);
                 var friendsRequestsStatement = connection.prepareStatement(friendsRequestsTable)) {

                friendSettingsStatement.executeUpdate();
                friendsDataStatement.executeUpdate();
                friendsRequestsStatement.executeUpdate();
            }
        });
    }
}
