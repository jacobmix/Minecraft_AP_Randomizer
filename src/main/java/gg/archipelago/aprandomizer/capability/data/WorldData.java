package gg.archipelago.aprandomizer.capability.data;

import com.google.common.collect.Lists;

import java.util.*;

public class WorldData {

    private String seedName = "";

    private int dragonState = DRAGON_ASLEEP;

    private boolean jailPlayers = true;

    private Set<Long> locations = new HashSet<>();

    private long index = 0;

    private String serverAddress = "";

    // For progressive chunks: current unlocked chunk level (1 = 1x1, 2 = 2x2, etc.)
    private int unlockedChunkLevel = 1;

    // Known players (have logged in at least once)
    private Set<UUID> knownPlayers = new HashSet<>();

    // Pending bonuses for offline players: UUID -> (BonusType -> seconds remaining)
    private HashMap<UUID, HashMap<String, Integer>> pendingBonuses = new HashMap<>();

    public static final int DRAGON_KILLED = 30;
    public static final int DRAGON_SPAWNED = 20;
    public static final int DRAGON_WAITING = 15;
    public static final int DRAGON_ASLEEP = 10;

    public void setSeedName(String seedName) {
        this.seedName = seedName;
    }

    public String getSeedName() {
        return seedName;
    }

    public void setDragonState(int dragonState) {
        this.dragonState = dragonState;
    }

    public int getDragonState() {
        return dragonState;
    }

    public boolean getJailPlayers() {
        return jailPlayers;
    }

    public void setJailPlayers(boolean jailPlayers) {
        this.jailPlayers = jailPlayers;
    }

    public void addLocation(long location) {
        this.locations.add(location);
    }

    public void setLocations(long[] locations) {
        this.locations.addAll(Lists.newArrayList(Arrays.stream(locations).iterator()));
    }

    public Set<Long> getLocations() {
        return locations;
    }

    public long getIndex() {
        return index;
    }

    public void setIndex(Long index) {
        this.index = index;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public int getUnlockedChunkLevel() {
        return unlockedChunkLevel;
    }

    public void setUnlockedChunkLevel(int level) {
        this.unlockedChunkLevel = level;
    }

    public void incrementUnlockedChunkLevel() {
        this.unlockedChunkLevel++;
    }

    // Known players methods
    public Set<UUID> getKnownPlayers() {
        return knownPlayers;
    }

    public void setKnownPlayers(Set<UUID> players) {
        this.knownPlayers = players;
    }

    public void addKnownPlayer(UUID uuid) {
        this.knownPlayers.add(uuid);
    }

    // Pending bonuses methods
    public HashMap<UUID, HashMap<String, Integer>> getAllPendingBonuses() {
        return pendingBonuses;
    }

    public void setAllPendingBonuses(HashMap<UUID, HashMap<String, Integer>> bonuses) {
        this.pendingBonuses = bonuses;
    }

    public HashMap<String, Integer> getPendingBonuses(UUID uuid) {
        return pendingBonuses.get(uuid);
    }

    public void addPendingBonus(UUID uuid, String bonusType, int seconds) {
        pendingBonuses.computeIfAbsent(uuid, k -> new HashMap<>());
        HashMap<String, Integer> playerBonuses = pendingBonuses.get(uuid);
        int currentSeconds = playerBonuses.getOrDefault(bonusType, 0);
        playerBonuses.put(bonusType, currentSeconds + seconds);
    }

    public void clearPendingBonuses(UUID uuid) {
        pendingBonuses.remove(uuid);
    }
}
