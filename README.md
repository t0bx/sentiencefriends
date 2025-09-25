# SentienceFriends

> This Plugin is still under development

A modular friends and party system for Minecraft networks. I built this as a Velocity proxy backend with a Paper lobby frontend, connected via Netty and backed by MySQL. Clean, fast, and made for modern Minecraft versions.

## Features
- Friends management: add, accept, remove, favorites, status, last online
- Parties: create, invite, chat, and server switch handling
- Lobby GUI: clean inventories for friends, settings, and pagination
- Realtime transport: custom Netty pipeline between lobby and proxy
- Persistence: MySQL with HikariCP connection pooling
- Modern Java: Java 21 toolchain, Gradle builds, Lombok for ergonomics
- Modular architecture: `api`, `proxy` (Velocity), `lobby` (Paper)

## Architecture
```
           [ Paper Lobby Plugin ]
                 (Java 21)
                     |
           Netty client (host: localhost, port: 1339)
                     |
           -----------------------------------------
                     |
           Netty server (port: 1339)
                     |
            [ Velocity Proxy Plugin ]  ——  [ MySQL 8.x ]
                       (Java 21)            (HikariCP)

         ^ Shared models & packets come from the `api` module
```

- `api`: Shared packet types, codecs, and helpers for Netty transport.
- `proxy`: Velocity plugin; owns the database and the authoritative state (friends, settings, party). Exposes Netty server on port 1339.
- `lobby`: Paper plugin; provides player-facing GUIs and talks to the proxy via Netty.

## Requirements
- Java 21 (JDK 21)
- Velocity 3.4.x (for the proxy)
- Paper 1.21.7 (for the lobby)
- MySQL 8.x

## Quick start
1) Build the project
```bash
# Windows
./gradlew.bat clean build
# macOS/Linux
./gradlew clean build
```
The build produces:
- `proxy/build/libs/SentienceFriends-Velocity-1.0.0.jar`
- `lobby/build/libs/SentienceFriends-Lobby-1.0.0.jar`
- `api/build/libs/api-1.0.0.jar` (library, bundled into the others)

2) Install plugins
- Put the Velocity JAR into your Velocity server `plugins/` folder.
- Put the Lobby JAR into your Paper server `plugins/` folder.

3) Configure database (proxy)
- Start Velocity once. The proxy creates `plugins/SentienceFriends/database.json`.
- Fill in your credentials, for example:
```json
{
  "host": "localhost",
  "port": 3306,
  "database": "sentiencefriends",
  "username": "root",
  "password": ""
}
```
- Restart Velocity. The plugin will create tables if needed.

4) Start order
- Start Velocity first (brings up the Netty server on port 1339).
- Start Paper lobby next (it connects to `localhost:1339`).

5) Verify
- Velocity logs should show initialization and DB connection.
- Paper logs should show the lobby plugin enabling and Netty connecting.

## Commands (proxy)
- `friend` (aliases: `friends`, `f`): manage friends
- `party` (alias: `p`): manage parties
- `partychat` (alias: `pc`): party chat

Permissions are standard Velocity command registrations; customize in your proxy permission system as needed.

## Configuration notes
- Netty port defaults to `1339`.
- Lobby connects to `localhost:1339`.
- If you want different host/port, change it in source:
  - Proxy: `MasterNettyManager(1339)` in `proxy`
  - Lobby: `new NettyManager("localhost", 1339)` in `lobby`

## Development
- Toolchain: Java 21 + Gradle Wrapper
- IDE: IntelliJ IDEA recommended
- Modules:
  - `api` (pure Java lib)
  - `proxy` (Velocity plugin, depends on `api`)
  - `lobby` (Paper plugin, depends on `api`)

Useful Gradle tasks:
```bash
./gradlew :proxy:shadowJar
./gradlew :lobby:shadowJar
```
These produce shaded plugin JARs with the expected artifact names.

## Roadmap
- Friend request UX improvements in the lobby GUI
- Cross-server notifications polishing
- Configurable Netty endpoint via external config

## Credits
- Velocity and Paper teams for the excellent platforms
- Netty for the robust networking toolkit
