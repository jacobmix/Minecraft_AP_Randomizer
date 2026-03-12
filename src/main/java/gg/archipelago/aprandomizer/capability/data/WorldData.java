package gg.archipelago.aprandomizer.capability.data;

import com.google.common.collect.Lists;

import java.util.*;
import java.util.Map;

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

    // Pending items for offline players: UUID -> list of item IDs
    private HashMap<UUID, List<Long>> pendingItems = new HashMap<>();

    // Fossil system fields
    private int fossilBalance = 0;
    private Set<String> unlockedTiers = new HashSet<>();      // "tools_2", "haste_1"
    private Map<String, Integer> purchasedTiers = new HashMap<>(); // {"tools": 2}
    private Set<Long> generatedFossils = new HashSet<>();     // All fossil positions (BlockPos.asLong())
    private Set<Long> collectedFossils = new HashSet<>();     // Collected fossil positions (BlockPos.asLong())

    // Item Shop: purchased shop location indices (0-19)
    private Set<Integer> purchasedShopLocations = new HashSet<>();
    // Item Shop: cached item flags from scouting (index -> flags)
    private Map<Integer, Integer> shopItemFlags = new HashMap<>();
    // Item Shop: cached item names from scouting (index -> name)
    private Map<Integer, String> shopItemNames = new HashMap<>();
    // Item Shop: cached player names from scouting (index -> player name)
    private Map<Integer, String> shopItemPlayers = new HashMap<>();

    // Shop tier unlocked (0 = none, 1 = row 1, 2 = row 1+2, 3 = all rows)
    private int shopTierUnlocked = 0;

    // Goal completion flag - stops receiving AP items after goal + release
    private boolean goalCompleted = false;

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

    // Pending items methods
    public HashMap<UUID, List<Long>> getAllPendingItems() {
        return pendingItems;
    }

    public void setAllPendingItems(HashMap<UUID, List<Long>> items) {
        this.pendingItems = items;
    }

    public List<Long> getPendingItems(UUID uuid) {
        return pendingItems.get(uuid);
    }

    public void addPendingItem(UUID uuid, Long itemId) {
        pendingItems.computeIfAbsent(uuid, k -> new ArrayList<>()).add(itemId);
    }

    public List<Long> removePendingItems(UUID uuid) {
        return pendingItems.remove(uuid);
    }

    // Fossil system methods
    public int getFossilBalance() {
        return fossilBalance;
    }

    public void setFossilBalance(int balance) {
        this.fossilBalance = balance;
    }

    public void addFossils(int amount) {
        this.fossilBalance += amount;
    }

    public boolean spendFossils(int amount) {
        if (this.fossilBalance >= amount) {
            this.fossilBalance -= amount;
            return true;
        }
        return false;
    }

    // Unlocked tiers methods
    public Set<String> getUnlockedTiers() {
        return unlockedTiers;
    }

    public void setUnlockedTiers(Set<String> tiers) {
        this.unlockedTiers = tiers;
    }

    public void unlockTier(String tierKey) {
        this.unlockedTiers.add(tierKey);
    }

    public boolean isTierUnlocked(String tierKey) {
        return this.unlockedTiers.contains(tierKey);
    }

    // Purchased tiers methods
    public Map<String, Integer> getPurchasedTiers() {
        return purchasedTiers;
    }

    public void setPurchasedTiers(Map<String, Integer> tiers) {
        this.purchasedTiers = tiers;
    }

    public void setPurchasedTier(String category, int tier) {
        this.purchasedTiers.put(category, tier);
    }

    public int getPurchasedTier(String category) {
        return this.purchasedTiers.getOrDefault(category, 0);
    }

    // Collected fossils methods (to prevent double collection)
    public Set<Long> getCollectedFossils() {
        return collectedFossils;
    }

    public void setCollectedFossils(Set<Long> fossils) {
        this.collectedFossils = fossils;
    }

    public boolean isFossilCollected(long posLong) {
        return this.collectedFossils.contains(posLong);
    }

    public void markFossilCollected(long posLong) {
        this.collectedFossils.add(posLong);
    }

    // Generated fossils methods (all fossil positions in the world)
    public Set<Long> getGeneratedFossils() {
        return generatedFossils;
    }

    public void setGeneratedFossils(Set<Long> fossils) {
        this.generatedFossils = fossils;
    }

    public void addGeneratedFossil(long posLong) {
        this.generatedFossils.add(posLong);
    }

    public boolean isFossilPosition(long posLong) {
        return this.generatedFossils.contains(posLong);
    }

    public boolean areFossilsGenerated() {
        return !this.generatedFossils.isEmpty();
    }

    // Item Shop methods
    public Set<Integer> getPurchasedShopLocations() {
        return purchasedShopLocations;
    }

    public void setPurchasedShopLocations(Set<Integer> locations) {
        this.purchasedShopLocations = locations;
    }

    public boolean isShopLocationPurchased(int index) {
        return purchasedShopLocations.contains(index);
    }

    public void markShopLocationPurchased(int index) {
        purchasedShopLocations.add(index);
    }

    public Map<Integer, Integer> getShopItemFlags() {
        return shopItemFlags;
    }

    public void setShopItemFlags(Map<Integer, Integer> flags) {
        this.shopItemFlags = flags;
    }

    public void setShopItemFlag(int index, int flags) {
        this.shopItemFlags.put(index, flags);
    }

    public int getShopItemFlag(int index) {
        return this.shopItemFlags.getOrDefault(index, 0);
    }

    public Map<Integer, String> getShopItemNames() {
        return shopItemNames;
    }

    public void setShopItemNames(Map<Integer, String> names) {
        this.shopItemNames = names;
    }

    public void setShopItemName(int index, String name) {
        this.shopItemNames.put(index, name);
    }

    public String getShopItemName(int index) {
        return this.shopItemNames.getOrDefault(index, "Item Shop " + (index + 1));
    }

    public Map<Integer, String> getShopItemPlayers() {
        return shopItemPlayers;
    }

    public void setShopItemPlayers(Map<Integer, String> players) {
        this.shopItemPlayers = players;
    }

    public void setShopItemPlayer(int index, String playerName) {
        this.shopItemPlayers.put(index, playerName);
    }

    public String getShopItemPlayer(int index) {
        return this.shopItemPlayers.getOrDefault(index, "");
    }

    // Shop tier methods
    public int getShopTierUnlocked() {
        return shopTierUnlocked;
    }

    public void setShopTierUnlocked(int tier) {
        this.shopTierUnlocked = Math.max(this.shopTierUnlocked, tier);
    }

    // Goal completion methods
    public boolean isGoalCompleted() {
        return goalCompleted;
    }

    public void setGoalCompleted(boolean completed) {
        this.goalCompleted = completed;
    }
}
