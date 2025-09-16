package de.t0bx.sentiencefriends.proxy.database;

import de.t0bx.sentiencefriends.proxy.ProxyPlugin;
import de.t0bx.sentiencefriends.proxy.utils.JsonDocument;
import lombok.Getter;

import java.io.File;
import java.io.IOException;

@Getter
public class DatabaseFile {

    private final File file;
    private JsonDocument jsonDocument;
    private DatabaseCredentials credentials;
    private boolean createdFreshly = false;

    public DatabaseFile() {
        this.file = new File("plugins/SentienceFriends/database.json");
        this.loadDatabaseFile();
    }

    private void loadDatabaseFile() {
        try {
            this.jsonDocument = JsonDocument.loadDocument(this.file);

            if (this.jsonDocument == null) {
                this.jsonDocument = new JsonDocument();

                this.jsonDocument.setString("host", "localhost");
                this.jsonDocument.setNumber("port", 3306);
                this.jsonDocument.setString("database", "sentiencefriends");
                this.jsonDocument.setString("username", "root");
                this.jsonDocument.setString("password", "");

                this.jsonDocument.save(this.file);
                this.createdFreshly = true;
            }

            this.credentials = new DatabaseCredentials(
                    this.jsonDocument.get("host").getAsString(),
                    this.jsonDocument.get("port").getAsInt(),
                    this.jsonDocument.get("database").getAsString(),
                    this.jsonDocument.get("username").getAsString(),
                    this.jsonDocument.get("password").getAsString()
            );
        } catch (IOException exception) {
            ProxyPlugin.getInstance().getLogger().error("Failed to load database file:");
        }
    }

    public record DatabaseCredentials(String host, int port, String database, String username, String password) {}
}
