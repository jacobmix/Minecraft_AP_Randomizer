# Minecraft Dig APWorld

Minecraft Dig is an Archipelago randomizer where you dig out chunks layer by layer. Each layer is a check that rewards you with items, bonuses or traps.

## Requirements

- [Archipelago](https://github.com/ArchipelagoMW/Archipelago/releases/latest)
- [Minecraft Java Edition](https://www.minecraft.net/en-us/store/minecraft-java-edition) **version 1.19.4**
- Java 17 (will be installed automatically by the client)

## Installation

1. Download `minecraft_dig.apworld` and **double-click it** to install. Wait for the "installed" popup.
2. **Fully restart Archipelago**, then use "Option Creator" in the launcher to create your Minecraft Dig player YAML.

## Generating a Game

1. Place your YAML file(s) in the `Players` folder.
2. Click "Generate" to create a multiworld `.zip` in the `output` folder.
3. Host by uploading the multiworld zip to [archipelago.gg/uploads](https://archipelago.gg/uploads).

> **Note:** Only one Minecraft Dig YAML is needed per slot. Multiple players can join the same MC server/slot.

## Playing

1. Get the `.apmcdig` patch file from the room page, host, or extracted multiworld zip.
2. Open **Archipelago Launcher** > **Minecraft Dig Client**.
3. The client will ask for the `.apmcdig` file. Point it to the file you received.
4. Say **yes** to all prompts to install Java, Forge, and the randomizer mod.

   > **Do not close the terminal!** That is the Minecraft Forge server and needs to stay open.

5. Open **Minecraft Java Edition 1.19.4**, go to `Multiplayer > Direct Connection`, type `localhost` (or your server hosting ip) and join.
6. In-game, connect to Archipelago: `/connect <IP> <PORT> <PASS>`
   - Example: `/connect archipelago.gg 38281 MyPassword`
7. Start the game with `/start`

## Options

| Option | Description | Default |
|--------|-------------|---------|
| **Chunk Count** | Number of chunks to dig (1-25) | 1 |
| **Progressive Chunks** | Start with 1 chunk, unlock more via items | Enabled |
| **Trap Weights** | Adjust frequency of various traps | Various |

### Available Traps
Bee Trap, Creeper Trap, Sand Rain, Spawn Wither, Goon Squad, Fish Fountain, Bad Air, Pocket Sand, Pesky Bird, Flash Flood, Pet the Kitty, Wingardium Leviosa, About Face, Acme Delivery

## How It Works

- You start on a small platform with basic tools (pickaxe, shovel, axe).
- Dig down through the chunk(s) layer by layer.
- Each layer cleared is a location check that sends you items.
- With **Progressive Chunks** enabled, you start with 1 chunk accessible. "World Barrier Expansion" items unlock additional chunks.
- Complete all layers to finish!

## Troubleshooting

### Common Issues

**Client won't open / "no attribute 'mc_launch'"**
- Reinstall Archipelago with "clean libs" ticked.

**IncompatibleVersion**
- Make sure you're on Minecraft **1.19.4** (not 1.20.4).

**Missing required library / IllegalArgumentException**
- Install [Java 17](https://github.com/corretto/corretto-17/releases/latest).

**OutOfMemoryError with _JAVA_OPTIONS**
- Delete `_JAVA_OPTIONS` from Windows Environment Variables.

**java.io.IOException: The process cannot access the file**
- Don't sync the Archipelago directory with OneDrive/cloud services.
- Close any running Java processes.
- Add Archipelago folder to antivirus exclusions.

### Linux Users

Add Java path to `host.yaml`:
```yaml
minecraft_dig_options:
  java: "/home/<user>/.local/share/Archipelago/jdk17.0.17_10/bin/java"
```

## Multiplayer (Local)

To let other players join your local Forge server:
1. Port forward port `25565` (default Minecraft server port).
2. Allow the Forge server through your firewall.
3. Share your IP address with other players.

## Hosting on an External Server (VPS/Dedicated)

If you want to host the Minecraft Dig server on a VPS or dedicated server instead of your local machine:

### Prerequisites on the Server

1. **Java 17** installed on the server:
   ```bash
   # Ubuntu/Debian
   sudo apt update && sudo apt install openjdk-17-jdk

   # CentOS/RHEL
   sudo yum install java-17-openjdk

   # Verify installation
   java -version
   ```

2. **Open port 25565** (Minecraft) in your server's firewall:
   ```bash
   # UFW (Ubuntu)
   sudo ufw allow 25565/tcp

   # firewalld (CentOS)
   sudo firewall-cmd --permanent --add-port=25565/tcp
   sudo firewall-cmd --reload
   ```

### Setup Steps

1. **Generate the game locally** using Archipelago and get the `.apmcdig` file.

2. **Run the Minecraft Dig Client once locally** to let it download Forge and the mod. The server files will be created in your Archipelago installation folder

3. **Copy the entire server folder** to your external server

4. **Start the server** using the included `run.sh`:
   ```bash
   cd "Minecraft Dig Forge server"
   chmod +x run.sh
   ./run.sh
   ```


### Connecting

1. Players connect to Minecraft using your server's IP: `your-server-ip:25565`
2. In-game, connect to Archipelago: `/connect archipelago.gg PORT PASSWORD`



