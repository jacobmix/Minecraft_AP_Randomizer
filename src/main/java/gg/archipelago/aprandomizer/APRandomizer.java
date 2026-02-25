package gg.archipelago.aprandomizer;

import com.google.gson.Gson;
import gg.archipelago.aprandomizer.APStorage.APMCData;
import gg.archipelago.aprandomizer.capability.APCapabilities;
import gg.archipelago.aprandomizer.capability.data.WorldData;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.managers.GoalManager;
import gg.archipelago.aprandomizer.managers.advancementmanager.LayerManager;
import gg.archipelago.aprandomizer.managers.itemmanager.ItemManager;
import gg.archipelago.aprandomizer.managers.itemmanager.TemporaryBonusManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(APRandomizer.MODID)
public class APRandomizer {
    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "aprandomizer";

    //store our APClient
    static private APClient apClient;

    static public MinecraftServer server;

    static private LayerManager layerManager;
    static private ItemManager itemManager;
    static private GoalManager goalManager;
    static private APMCData apmcData;
    static private final Set<Integer> validVersions = new HashSet<>() {{
        this.add(12);
    }};
    static private boolean jailPlayers = true;
    static private BlockPos jailCenter = BlockPos.ZERO;
    static public WorldData worldData;

    // Auto-reconnect settings
    private static int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static String pendingReconnectAddress = null;

    public APRandomizer() {
        LOGGER.info("Minecraft Archipelago 1.19.4 version (-2) Randomizer initializing.");

        // Register ourselves for server and other game events we are interested in
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        forgeBus.register(this);


        Gson gson = new Gson();
        try {
            Path path = Paths.get("./APData/");
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                LOGGER.info("APData folder missing, creating.");
            }

            File[] files = new File(path.toUri()).listFiles((d, name) -> name.endsWith(".apmc"));
            assert files != null;
            Arrays.sort(files, Comparator.comparingLong(File::lastModified));
            String b64 = Files.readAllLines(files[0].toPath()).get(0);
            String json = new String(Base64.getDecoder().decode(b64));
            apmcData = gson.fromJson(json, APMCData.class);
            if (!validVersions.contains(apmcData.client_version)) {
                apmcData.state = APMCData.State.INVALID_VERSION;
            }
            //LOGGER.info(apmcData.structures.toString());
        } catch (IOException | NullPointerException | ArrayIndexOutOfBoundsException | AssertionError e) {
            LOGGER.error("no .apmc file found. please place .apmc file in './APData/' folder.");
            if (apmcData == null) {
                apmcData = new APMCData();
                apmcData.state = APMCData.State.MISSING;
            }
        }

    }

    public static APClient getAP() {
        return apClient;
    }

    public static boolean isConnected() {
        return (apClient != null && apClient.isConnected());
    }

    public static LayerManager getLayerManager() {
        return layerManager;
    }

    public static APMCData getApmcData() {
        return apmcData;
    }

    public static MinecraftServer getServer() {
        return server;
    }

    public static ItemManager getItemManager() {
        return itemManager;
    }

    public static Set<Integer> getValidVersions() {
        return validVersions;
    }


    public static boolean isJailPlayers() {
        return jailPlayers;
    }

    public static void setJailPlayers(boolean jailPlayers) {
        APRandomizer.jailPlayers = jailPlayers;
        worldData.setJailPlayers(jailPlayers);
    }

    public static BlockPos getJailPosition() {
        return jailCenter;
    }

    public static boolean isRace() {
        //return true;
        return getApmcData().race;
    }

    public static GoalManager getGoalManager() {
        return goalManager;
    }

    public static void resetReconnectAttempts() {
        reconnectAttempts = 0;
        pendingReconnectAddress = null;
    }

    public static void attemptReconnect(String address) {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Utils.sendMessageToAll("Auto-reconnect failed after " + MAX_RECONNECT_ATTEMPTS + " attempts. Use /connect to reconnect manually.");
            LOGGER.warn("Auto-reconnect gave up after {} attempts", MAX_RECONNECT_ATTEMPTS);
            pendingReconnectAddress = null;
            return;
        }

        reconnectAttempts++;
        pendingReconnectAddress = address;

        Utils.sendMessageToAll("Reconnect attempt " + reconnectAttempts + "/" + MAX_RECONNECT_ATTEMPTS + " to " + address);
        LOGGER.info("Reconnect attempt {}/{} to {}", reconnectAttempts, MAX_RECONNECT_ATTEMPTS, address);

        try {
            APClient client = getAP();
            client.setName(apmcData.player_name);
            client.connect(address);
        } catch (Exception e) {
            LOGGER.error("Reconnect attempt failed: {}", e.getMessage());
            // Schedule next retry after 5 seconds
            scheduleReconnectRetry(address);
        }
    }

    public static void scheduleReconnectRetry(String address) {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Utils.sendMessageToAll("Auto-reconnect failed after " + MAX_RECONNECT_ATTEMPTS + " attempts. Use /connect to reconnect manually.");
            pendingReconnectAddress = null;
            return;
        }

        // Schedule retry after 5 seconds (100 ticks)
        server.tell(new net.minecraft.server.TickTask(
            server.getTickCount() + 100,
            () -> attemptReconnect(address)
        ));
    }

    public static String getPendingReconnectAddress() {
        return pendingReconnectAddress;
    }

    public static int getChunkSide() {
        int count = (apmcData != null) ? Math.max(apmcData.chunk_count, 1) : 1;
        return (int) Math.ceil(Math.sqrt(count));
    }

    /**
     * Check if progressive chunks mode is enabled
     */
    public static boolean isProgressiveChunks() {
        return apmcData != null && apmcData.progressive_chunks;
    }

    /**
     * Get the current number of unlocked chunks
     */
    public static int getUnlockedChunks() {
        if (worldData == null) return 1;
        return worldData.getUnlockedChunkLevel(); // Reusing the field, now means chunk count
    }

    /**
     * Get max chunks from apmc data
     */
    public static int getMaxChunks() {
        return (apmcData != null) ? Math.max(apmcData.chunk_count, 1) : 1;
    }

    /**
     * Expand the world barrier by one chunk (called when receiving World Barrier Expansion)
     */
    public static void expandWorldBarrier() {
        if (worldData == null || server == null) return;

        int maxChunks = getMaxChunks();
        int currentChunks = worldData.getUnlockedChunkLevel();

        if (currentChunks < maxChunks) {
            worldData.incrementUnlockedChunkLevel();
            int newChunks = worldData.getUnlockedChunkLevel();

            // Update the world border
            updateWorldBorderForChunks(newChunks);

            Utils.sendMessageToAll("World Barrier Expanded! Now accessible: " + newChunks + "/" + maxChunks + " chunks");
            LOGGER.info("World Barrier expanded to {} chunks", newChunks);
        }
    }

    /**
     * Update the world border to cover n chunks
     * Border grows to fit ceil(sqrt(n)) x ceil(sqrt(n)) area
     */
    public static void updateWorldBorderForChunks(int numChunks) {
        if (server == null) return;

        // Calculate the grid side needed to contain numChunks
        int side = (int) Math.ceil(Math.sqrt(numChunks));

        // Calculate border size: center at middle of grid, size = grid width
        double centerCoord = side * 8.0;
        double borderSize = side * 16.0;

        WorldBorder border = server.overworld().getWorldBorder();
        border.setCenter(centerCoord, centerCoord);
        border.setSize(borderSize);
        border.setWarningBlocks(0);
    }

    @SubscribeEvent
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        if (apmcData.state != APMCData.State.VALID) {
            LOGGER.error("invalid APMC file. Reason: {}", apmcData.state);
        }
        server = event.getServer();
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

        // do something when the server starts
        layerManager = new LayerManager();
        itemManager = new ItemManager();
        goalManager = new GoalManager();

        ServerLevel overworld = server.overworld();

        server.getGameRules().getRule(GameRules.RULE_LIMITED_CRAFTING).set(true, server);
        server.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true, server);
        server.getGameRules().getRule(GameRules.RULE_ANNOUNCE_ADVANCEMENTS).set(true, server);
        server.getGameRules().getRule(GameRules.RULE_FALL_DAMAGE).set(false, server);
        server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        server.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        server.getGameRules().getRule(GameRules.RULE_DO_PATROL_SPAWNING).set(false, server);
        server.getGameRules().getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(false, server);
        server.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        server.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(false, server);
        server.getGameRules().getRule(GameRules.RULE_DOENTITYDROPS).set(false, server);
        server.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS).set(false, server);
        overworld.setDayTime(0);
        server.setDifficulty(Difficulty.NORMAL, true);

        //fetch our custom world save data we attach to the worlds.
        worldData = overworld.getCapability(APCapabilities.WORLD_DATA).orElseThrow(AssertionError::new);
        jailPlayers = worldData.getJailPlayers();
        layerManager.setCheckedLayers(new HashSet<>(worldData.getLocations()));

        // Load known players for temporary bonus system
        TemporaryBonusManager.loadKnownPlayers(worldData.getKnownPlayers());


        //check if APMC data is present and if the seed matches what we expect
        if (apmcData.state == APMCData.State.VALID && !worldData.getSeedName().equals(apmcData.seed_name)) {
            //check to see if our worlddata is empty if it is then save the aproom data.
            if (worldData.getSeedName().isEmpty()) {
                worldData.setSeedName(apmcData.seed_name);
                //this is also our first boot so set this flag so we can do first boot stuff.
            } else {
                apmcData.state = APMCData.State.INVALID_SEED;
            }
        }

        //if no apmc file was found set our world data seed to invalid so it will force a regen of this blank world.
        if (apmcData.state == APMCData.State.MISSING) {
            worldData.setSeedName("Invalid");
        }

        if (apmcData.state == APMCData.State.VALID) {
            apClient = new APClient(server);
        }

        if (jailPlayers) {
            if(!server.getScoreboard().hasObjective("blocks_broken")) {
                Objective blocksBroken = server.getScoreboard().addObjective("blocks_broken", ObjectiveCriteria.DUMMY, Component.literal("Blocks Broken"), ObjectiveCriteria.RenderType.INTEGER);
                server.getScoreboard().setDisplayObjective(Scoreboard.DISPLAY_SLOT_SIDEBAR, blocksBroken);
            }

            int chunkCount = getMaxChunks();
            int side = getChunkSide();

            // Create bedrock ring around the entire chunk area (2 blocks wide)
            int gridSize = side * 16;
            int ringWidth = 2;
            for (int x = -ringWidth; x < gridSize + ringWidth; x++) {
                for (int z = -ringWidth; z < gridSize + ringWidth; z++) {
                    // Only place bedrock outside the chunk area (creating a border ring)
                    if (x < 0 || x >= gridSize || z < 0 || z >= gridSize) {
                        overworld.setBlock(new BlockPos(x, 128, z), Blocks.BEDROCK.defaultBlockState(), 2);
                    }
                }
            }

            // Create spawn platform (5x5) in the corner
            for (int x = -5; x <= -1; x++) {
                for (int z = -5; z <= -1; z++) {
                    overworld.setBlock(new BlockPos(x, 128, z), Blocks.BEDROCK.defaultBlockState(), 2);
                }
            }

            // Dynamically discover layer variant templates
            // Convention: layer_{layerNum}_{a,b,c,...}.nbt in structures folder
            // 4 layers at fixed Y positions: layer_1 (top) to layer_4 (bottom)
            int[] layerYPositions = {81, 33, -15, -63};
            List<List<StructureTemplate>> layerTemplates = new ArrayList<>();

            for (int l = 1; l <= layerYPositions.length; l++) {
                List<StructureTemplate> variants = new ArrayList<>();
                for (char v = 'a'; v <= 'z'; v++) {
                    String name = "layer_" + l + "_" + v;
                    Optional<StructureTemplate> template = overworld.getStructureManager()
                        .get(new ResourceLocation(MODID, name));
                    if (template.isPresent()) {
                        variants.add(template.get());
                        LOGGER.info("Loaded structure variant: {}", name);
                    }
                }
                if (variants.isEmpty()) {
                    LOGGER.warn("No variants found for layer_{}, chunks will be empty at Y={}", l, layerYPositions[l - 1]);
                }
                layerTemplates.add(variants);
            }

            // Use world seed for deterministic variant selection per chunk
            Random variantRng = new Random(apmcData.world_seed);

            // Generate exactly chunk_count chunks, filling row by row
            int chunksPlaced = 0;
            for (int cz = 0; cz < side && chunksPlaced < chunkCount; cz++) {
                for (int cx = 0; cx < side && chunksPlaced < chunkCount; cx++) {
                    int ox = cx * 16;
                    int oz = cz * 16;

                    // Place bedrock floor for this chunk
                    for (int x = ox; x < ox + 16; x++) {
                        for (int z = oz; z < oz + 16; z++) {
                            overworld.setBlock(new BlockPos(x, -64, z), Blocks.BEDROCK.defaultBlockState(), 2);
                        }
                    }

                    // Place layer structures
                    for (int l = 0; l < layerYPositions.length; l++) {
                        List<StructureTemplate> variants = layerTemplates.get(l);
                        if (!variants.isEmpty()) {
                            StructureTemplate template = variants.get(variantRng.nextInt(variants.size()));
                            template.placeInWorld(overworld, new BlockPos(ox, layerYPositions[l], oz),
                                new BlockPos(ox, layerYPositions[l], oz),
                                new StructurePlaceSettings(), RandomSource.create(), 2);
                        }
                    }
                    chunksPlaced++;
                }
            }

            LOGGER.info("Dig mode: placed {} chunks in {}x{} grid", chunksPlaced, side, side);

            overworld.setDefaultSpawnPos(new BlockPos(-3, 129, -3), 0f);
            jailCenter = overworld.getSharedSpawnPos();

            WorldBorder border = overworld.getWorldBorder();
            border.setCenter(-2.5,-2.5);
            border.setSize(5);
            border.setWarningBlocks(0);
            border.setWarningTime(0);
            border.setDamageSafeZone(0);
            border.setDamagePerBlock(Double.MAX_VALUE);

        }

        if (apmcData.state == APMCData.State.VALID) {
            // Check for saved server address (from previous /connect) for auto-reconnect
            String savedAddress = worldData.getServerAddress();

            if (savedAddress != null && !savedAddress.isEmpty()) {
                // Auto-reconnect using saved address with retry system
                LOGGER.info("Found saved address, starting auto-reconnect: " + savedAddress);
                resetReconnectAttempts();
                attemptReconnect(savedAddress);
            }
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        if (apClient != null)
            apClient.close();
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        if (apClient != null)
            apClient.close();
    }
}
