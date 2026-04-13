package gg.archipelago.aprandomizer.managers.itemmanager;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.managers.FossilManager;
import gg.archipelago.aprandomizer.managers.ShopManager;
import gg.archipelago.aprandomizer.managers.advancementmanager.CustomAdvancementHandler;
import gg.archipelago.aprandomizer.managers.itemmanager.powers.ExcavationPower;
import gg.archipelago.aprandomizer.managers.itemmanager.powers.Power;
import gg.archipelago.aprandomizer.managers.itemmanager.traps.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.Callable;

@Mod.EventBusSubscriber
public class ItemManager {
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    // ============================================================================
    // ITEM IDS - MUST MATCH items.json ORDER EXACTLY
    // ============================================================================
    // 50000: Progressive Tools      (handled by shop system)
    // 50001: Progressive Haste      (handled by shop system)
    // 50002: Progressive Excavation (handled by shop system)
    // 50003: TNT
    // 50004: Golden Pick
    // 50005: True Golden Pick
    // 50006: Defensive Fish
    // 50007-50020: Traps (14 items)
    // 50021: Fossil Xray
    // 50022: World Barrier Expansion
    // 50023: Haste Boost
    // 50024: Excavation Boost
    // 50025: Progressive Reach
    // 50026: Progressive Efficiency
    // 50027: Meteor Shower (trap)
    // 50028: Earthquake (trap)
    // 50029: Explosive Bow
    // 50030: Increased Gravity (trap)
    // 50031: Progressive Shop
    // ============================================================================

    // Progressive item IDs for shop unlock system (must match items.json order)
    private static final long PROGRESSIVE_TOOLS_ID = 50000;      // Unified tools (4 tiers)
    private static final long PROGRESSIVE_HASTE_ID = 50001;      // Haste (1 tier)
    private static final long PROGRESSIVE_EXCAVATION_ID = 50002; // Excavation (3 tiers)
    private static final long PROGRESSIVE_REACH_ID = 50025;      // Reach (1 tier)
    private static final long PROGRESSIVE_EFFICIENCY_ID = 50026; // Efficiency (3 tiers)
    private static final long PROGRESSIVE_SHOP_ID = 50031;      // Shop tiers (3 tiers)


    // Item stacks - explicit IDs matching items.json
    private final HashMap<Long, ItemStack> itemStacks = new HashMap<>();

    // Trap data - explicit IDs matching items.json
    private final HashMap<Long, Callable<Trap>> trapData = new HashMap<>();

    private void initializeItemMaps() {
        // TNT - ID 50003
        itemStacks.put(50003L, new ItemStack(Items.TNT, 16));

        // Golden Pick - ID 50004
        ItemStack goldenPick = new ItemStack(Items.GOLDEN_PICKAXE);
        goldenPick.enchant(Enchantments.BLOCK_EFFICIENCY, 10);
        goldenPick.enchant(Enchantments.UNBREAKING, 5);
        itemStacks.put(50004L, goldenPick);

        // True Golden Pick - ID 50005
        ItemStack trueGoldenPick = new ItemStack(Items.GOLDEN_PICKAXE);
        addLore(trueGoldenPick,"True Golden Pickaxe", new String[] {
                "A golden pickaxe,",
                "A tool of great power,",
                "But also of great danger.",
                "",
                "It can mine the most precious ores,",
                "But it can also destroy the most hardened structures.",
                "",
                "It is a tool to be used with caution,",
                "For it can be a tool of both good and evil."});
        trueGoldenPick.enchant(Enchantments.BLOCK_EFFICIENCY, 2);
        trueGoldenPick.setDamageValue(trueGoldenPick.getMaxDamage() - 1);
        trueGoldenPick.getOrCreateTag().putBoolean("truepick", true);
        itemStacks.put(50005L, trueGoldenPick);

        // Defensive Fish - ID 50006
        ItemStack defensiveFish = new ItemStack(Items.SALMON);
        defensiveFish.enchant(Enchantments.KNOCKBACK, 10);
        itemStacks.put(50006L, defensiveFish);

        // Explosive Bow - ID 50029
        ItemStack explosiveBow = new ItemStack(Items.BOW);
        explosiveBow.enchant(Enchantments.POWER_ARROWS, 3);
        explosiveBow.enchant(Enchantments.INFINITY_ARROWS, 1);
        addLore(explosiveBow, "§c§lExplosive Bow", new String[] {
                "§6Use it to dig...",
                "§4or to kill the others,",
                "§8I don't care.",
                "",
                "§a✦ §7Doesn't need arrows",
                "§a✦ §7Won't hurt you"
        });
        explosiveBow.getOrCreateTag().putBoolean("explosiveBow", true);
        // 16 uses via durability: set damage so only 16 durability remains
        explosiveBow.setDamageValue(explosiveBow.getMaxDamage() - 16);
        itemStacks.put(50029L, explosiveBow);

        // Traps - IDs 50007-50020 (matching items.json order)
        trapData.put(50007L, BeeTrap::new);
        trapData.put(50008L, CreeperTrap::new);
        trapData.put(50009L, SandRain::new);
        trapData.put(50010L, FakeWither::new);
        trapData.put(50011L, GoonTrap::new);
        trapData.put(50012L, FishFountainTrap::new);
        trapData.put(50013L, MiningFatigueTrap::new);
        trapData.put(50014L, BlindnessTrap::new);
        trapData.put(50015L, PhantomTrap::new);
        trapData.put(50016L, WaterTrap::new);
        trapData.put(50017L, GhastTrap::new);
        trapData.put(50018L, LevitateTrap::new);
        trapData.put(50019L, AboutFaceTrap::new);
        trapData.put(50020L, AnvilTrap::new);
        trapData.put(50027L, MeteorShowerTrap::new);
        // 50028: Earthquake removed
        trapData.put(50030L, IncreasedGravityTrap::new);
    }

    private List<Long> receivedItems = new ArrayList<>();

    private static final HashMap<String, ItemStack> permanentItems = new HashMap<>();
    private static final HashMap<String, MobEffectInstance> permanentEffects = new HashMap<>();

    public ItemManager() {
        initializeItemMaps();
        permanentEffects.put("saturation", new MobEffectInstance(MobEffects.SATURATION, MobEffectInstance.INFINITE_DURATION, 0, true, false));
    }

    private void addLore(ItemStack iStack, String name, String[] Lore) {
        iStack.setHoverName(Component.literal(name));
        CompoundTag compoundNBT = iStack.getOrCreateTagElement("display");
        ListTag itemLoreLines = new ListTag();
        for (String s : Lore) {
            StringTag itemLore = StringTag.valueOf(Component.Serializer.toJson(Component.literal(s)));
            itemLoreLines.add(itemLore);
        }
        compoundNBT.put("Lore", itemLoreLines);
    }

    public void setReceivedItems(List<Long> items) {
        this.receivedItems = items;
        APRandomizer.getGoalManager().updateGoal(false);
    }

    public void giveItem(Long itemID, ServerPlayer player) {
        if (APRandomizer.isJailPlayers()) {
            //dont send items to players if game has not started.
            return;
        }

        if (itemStacks.containsKey(itemID)) {
            ItemStack itemstack = itemStacks.get(itemID).copy();
            Utils.giveItemToPlayer(player, itemstack);
        } else if (trapData.containsKey(itemID)) {
            try {
                trapData.get(itemID).call().trigger(player);
                for (ServerPlayer serverPlayer : APRandomizer.getServer().getPlayerList().getPlayers()) {
                    CustomAdvancementHandler.grantAdvancement(serverPlayer,new ResourceLocation(APRandomizer.MODID,"archipelago/get_trap"));
                }
            } catch (Exception ignored) {
            }
        }
    }


    // Item IDs based on position in all_items array + 50000 offset (must match items.json)
    private static final long FOSSIL_XRAY_ID = 50021;
    private static final long WORLD_BARRIER_EXPANSION_ID = 50022;
    private static final long HASTE_BOOST_ID = 50023;
    private static final long EXCAVATION_BOOST_ID = 50024;

    public boolean giveItemToAll(long itemID, long index) {
        // Skip if this item was already processed (prevents duplicate processing on reconnect)
        if (APRandomizer.worldData != null && APRandomizer.worldData.getIndex() >= index) {
            LOGGER.debug("Skipping already processed item {} at index {}", itemID, index);
            return false;
        }

        receivedItems.add(itemID);

        // Handle progressive items - unlock tiers in the shop instead of direct application
        // Uses WorldData to derive next tier (crash-proof, no in-memory counters)
        if (itemID == PROGRESSIVE_TOOLS_ID) {
            int nextTier = ShopManager.findNextTierToUnlock(ShopManager.CATEGORY_TOOLS);
            if (nextTier > 0) {
                ShopManager.unlockTier(ShopManager.CATEGORY_TOOLS, nextTier);
                LOGGER.info("Progressive Tools unlock received, unlocking tools tier {}", nextTier);
            }
        } else if (itemID == PROGRESSIVE_HASTE_ID) {
            int nextTier = ShopManager.findNextTierToUnlock(ShopManager.CATEGORY_HASTE);
            if (nextTier > 0) {
                ShopManager.unlockTier(ShopManager.CATEGORY_HASTE, nextTier);
                LOGGER.info("Progressive Haste unlock received, unlocking haste tier {}", nextTier);
            }
        } else if (itemID == PROGRESSIVE_EXCAVATION_ID) {
            int nextTier = ShopManager.findNextTierToUnlock(ShopManager.CATEGORY_EXCAVATION);
            if (nextTier > 0) {
                ShopManager.unlockTier(ShopManager.CATEGORY_EXCAVATION, nextTier);
                LOGGER.info("Progressive Excavation unlock received, unlocking excavation tier {}", nextTier);
            }
        } else if (itemID == PROGRESSIVE_REACH_ID) {
            int nextTier = ShopManager.findNextTierToUnlock(ShopManager.CATEGORY_REACH);
            if (nextTier > 0) {
                ShopManager.unlockTier(ShopManager.CATEGORY_REACH, nextTier);
                LOGGER.info("Progressive Reach unlock received, unlocking reach tier {}", nextTier);
            }
        } else if (itemID == PROGRESSIVE_EFFICIENCY_ID) {
            int nextTier = ShopManager.findNextTierToUnlock(ShopManager.CATEGORY_EFFICIENCY);
            if (nextTier > 0) {
                ShopManager.unlockTier(ShopManager.CATEGORY_EFFICIENCY, nextTier);
                LOGGER.info("Progressive Efficiency unlock received, unlocking efficiency tier {}", nextTier);
            }
        } else if (itemID == PROGRESSIVE_SHOP_ID) {
            if (APRandomizer.worldData != null) {
                int currentShopTier = APRandomizer.worldData.getShopTierUnlocked();
                int nextShopTier = currentShopTier + 1;
                APRandomizer.worldData.setShopTierUnlocked(nextShopTier);
                Utils.sendMessageToAll("§d[Archipelago] §eItem Shop Tier " + nextShopTier + " unlocked!");
                LOGGER.info("Progressive Shop unlock received, unlocking shop tier {}", nextShopTier);
            }
        }

        // Handle World Barrier Expansion
        if (itemID == WORLD_BARRIER_EXPANSION_ID) {
            APRandomizer.expandWorldBarrier();
        }

        // Handle Haste Boost
        if (itemID == HASTE_BOOST_ID) {
            TemporaryBonusManager.grantBonusToAll(TemporaryBonusManager.BONUS_HASTE);
        }

        // Handle Excavation Boost
        if (itemID == EXCAVATION_BOOST_ID) {
            TemporaryBonusManager.grantBonusToAll(TemporaryBonusManager.BONUS_EXCAVATION);
        }

        // Handle Fossil Xray - activate for ALL players
        if (itemID == FOSSIL_XRAY_ID) {
            FossilManager.activateFossilXrayForAll();
        }

        // Update the index and give items to players
        APRandomizer.getServer().execute(() -> {
            APRandomizer.worldData.setIndex(index);

            boolean isPhysicalItem = itemStacks.containsKey(itemID);
            boolean isTrap = trapData.containsKey(itemID);

            // Trigger traps only ONCE (not per-player), since traps already affect all players internally
            if (isTrap) {
                try {
                    List<ServerPlayer> players = APRandomizer.getServer().getPlayerList().getPlayers();
                    if (!players.isEmpty()) {
                        trapData.get(itemID).call().trigger(players.get(0));
                        for (ServerPlayer sp : players) {
                            CustomAdvancementHandler.grantAdvancement(sp, new ResourceLocation(APRandomizer.MODID, "archipelago/get_trap"));
                        }
                    }
                } catch (Exception ignored) {}
            }

            // Get online player UUIDs
            Set<UUID> onlinePlayerUUIDs = new HashSet<>();
            for (ServerPlayer player : APRandomizer.getServer().getPlayerList().getPlayers()) {
                onlinePlayerUUIDs.add(player.getUUID());
                if (!isTrap) {
                    giveItem(itemID, player);
                }
            }

            // Queue item for offline known players (only physical items, NOT traps)
            if (isPhysicalItem && !isTrap && APRandomizer.worldData != null) {
                for (UUID knownUUID : APRandomizer.worldData.getKnownPlayers()) {
                    if (!onlinePlayerUUIDs.contains(knownUUID)) {
                        APRandomizer.worldData.addPendingItem(knownUUID, itemID);
                        LOGGER.info("Queued item {} for offline player {}", itemID, knownUUID);
                    }
                }
            }

            APRandomizer.getServer().saveEverything(true, true, true);
        });
        return true;
    }

    /**
     * Register a player as known (called on first join, persisted in WorldData)
     */
    public void registerPlayer(ServerPlayer player) {
        if (APRandomizer.worldData == null) return;

        UUID uuid = player.getUUID();
        Set<UUID> known = APRandomizer.worldData.getKnownPlayers();
        if (!known.contains(uuid)) {
            APRandomizer.worldData.addKnownPlayer(uuid);
            LOGGER.info("Registered new player: {} ({})", player.getName().getString(), uuid);
        }
    }

    /**
     * Give all pending items to a player (called when they rejoin, reads from WorldData)
     */
    public void givePendingItems(ServerPlayer player) {
        if (APRandomizer.worldData == null) return;

        UUID uuid = player.getUUID();
        List<Long> pending = APRandomizer.worldData.removePendingItems(uuid);
        if (pending != null && !pending.isEmpty()) {
            LOGGER.info("Giving {} pending items to {}", pending.size(), player.getName().getString());

            // Build list of item names for the message
            List<String> itemNames = new ArrayList<>();
            for (Long itemID : pending) {
                String name = getItemNameById(itemID);
                itemNames.add(name);
                giveItem(itemID, player);
            }

            // Send message to player
            if (!itemNames.isEmpty()) {
                Utils.sendMessageToPlayer(player, "§aWhile you were offline, you received:");
                for (String name : itemNames) {
                    Utils.sendMessageToPlayer(player, "  §7- §f" + name);
                }
                player.playNotifySound(
                    net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    1.0f, 1.0f
                );
            }
            APRandomizer.getServer().saveEverything(true, true, true);
        }
    }

    /**
     * Get the display name for an item ID (public for state file)
     */
    public String getItemNameById(Long itemID) {
        if (itemStacks.containsKey(itemID)) {
            return itemStacks.get(itemID).getHoverName().getString();
        } else if (trapData.containsKey(itemID)) {
            return "Trap";
        } else if (itemID == PROGRESSIVE_TOOLS_ID) {
            return "Progressive Tools";
        } else if (itemID == PROGRESSIVE_HASTE_ID) {
            return "Progressive Haste";
        } else if (itemID == PROGRESSIVE_EXCAVATION_ID) {
            return "Progressive Excavation";
        } else if (itemID == FOSSIL_XRAY_ID) {
            return "Fossil Xray";
        } else if (itemID == WORLD_BARRIER_EXPANSION_ID) {
            return "World Barrier Expansion";
        } else if (itemID == HASTE_BOOST_ID) {
            return "Haste Boost";
        } else if (itemID == EXCAVATION_BOOST_ID) {
            return "Excavation Boost";
        }
        return "Item #" + itemID;
    }

    public List<Long> getAllItems() {
        return receivedItems;
    }

    public static void updateEffect(MobEffectInstance effect, String key) {
        permanentEffects.put(key, effect);
    }

    public static void updateItem(ItemStack item, String key) {
        permanentItems.put(key, item);
    }

    /**
     * Apply an upgrade purchased from the shop
     * @param category The upgrade category (tools, haste, excavation)
     * @param tier The tier being purchased
     */
    public void applyShopUpgrade(String category, int tier) {
        LOGGER.info("Applying shop upgrade: {} tier {}", category, tier);

        switch (category) {
            case ShopManager.CATEGORY_TOOLS:
                applyToolsUpgrade(tier);
                // Re-apply efficiency enchant if already purchased
                if (efficiencyLevel > 0) {
                    applyEfficiencyUpgrade(efficiencyLevel);
                }
                break;
            case ShopManager.CATEGORY_HASTE:
                applyHasteUpgrade(tier);
                break;
            case ShopManager.CATEGORY_EXCAVATION:
                applyExcavationUpgrade(tier);
                break;
            case ShopManager.CATEGORY_REACH:
                applyReachUpgrade(tier);
                break;
            case ShopManager.CATEGORY_EFFICIENCY:
                applyEfficiencyUpgrade(tier);
                break;
        }
    }

    /**
     * Apply tools upgrade - upgrades all tools to the specified tier
     * Tier 0: Wooden, 1: Stone, 2: Iron, 3: Diamond, 4: Netherite
     */
    private void applyToolsUpgrade(int tier) {
        ItemStack pick, shovel, axe, hoe;

        switch (tier) {
            case 1: // Stone
                pick = createPermanentItem(new ItemStack(Items.STONE_PICKAXE), "pick");
                shovel = createPermanentItem(new ItemStack(Items.STONE_SHOVEL), "shovel");
                axe = createPermanentItem(new ItemStack(Items.STONE_AXE), "axe");
                hoe = createPermanentItem(new ItemStack(Items.STONE_HOE), "hoe");
                break;
            case 2: // Iron
                pick = createPermanentItem(new ItemStack(Items.IRON_PICKAXE), "pick");
                shovel = createPermanentItem(new ItemStack(Items.IRON_SHOVEL), "shovel");
                axe = createPermanentItem(new ItemStack(Items.IRON_AXE), "axe");
                hoe = createPermanentItem(new ItemStack(Items.IRON_HOE), "hoe");
                break;
            case 3: // Diamond
                pick = createPermanentItem(new ItemStack(Items.DIAMOND_PICKAXE), "pick");
                shovel = createPermanentItem(new ItemStack(Items.DIAMOND_SHOVEL), "shovel");
                axe = createPermanentItem(new ItemStack(Items.DIAMOND_AXE), "axe");
                hoe = createPermanentItem(new ItemStack(Items.DIAMOND_HOE), "hoe");
                break;
            case 4: // Netherite
                pick = createPermanentItem(new ItemStack(Items.NETHERITE_PICKAXE), "pick");
                shovel = createPermanentItem(new ItemStack(Items.NETHERITE_SHOVEL), "shovel");
                axe = createPermanentItem(new ItemStack(Items.NETHERITE_AXE), "axe");
                hoe = createPermanentItem(new ItemStack(Items.NETHERITE_HOE), "hoe");
                break;
            default:
                return;
        }

        // Update permanent items
        if (pick != null) permanentItems.put("pick", pick);
        if (shovel != null) permanentItems.put("shovel", shovel);
        if (axe != null) permanentItems.put("axe", axe);
        if (hoe != null) permanentItems.put("hoe", hoe);
    }

    /**
     * Apply haste upgrade
     */
    private void applyHasteUpgrade(int tier) {
        if (tier >= 1) {
            MobEffectInstance hasteEffect = new MobEffectInstance(
                MobEffects.DIG_SPEED,
                MobEffectInstance.INFINITE_DURATION,
                0, // Haste I = amplifier 0
                true,
                false
            );
            permanentEffects.put("haste", hasteEffect);
        }
    }

    /**
     * Apply excavation upgrade
     */
    private void applyExcavationUpgrade(int tier) {
        // Grant excavation power at the appropriate level
        ExcavationPower.setLevel(tier);
    }

    // Reach level (0 = no bonus, 1 = +1 reach)
    private static int reachLevel = 0;

    /**
     * Apply reach upgrade
     */
    private void applyReachUpgrade(int tier) {
        reachLevel = tier;
        LOGGER.info("Reach level set to: {}", reachLevel);
    }

    /**
     * Get the current reach bonus
     */
    public static int getReachBonus() {
        return reachLevel;
    }

    // Efficiency level (0 = no bonus, 1-3 = speed multiplier)
    private static int efficiencyLevel = 0;

    /**
     * Apply efficiency upgrade
     */
    private void applyEfficiencyUpgrade(int tier) {
        efficiencyLevel = tier;
        LOGGER.info("Efficiency level set to: {}", efficiencyLevel);

        // Also add real Efficiency enchantment to current permanent tools
        int enchantLevel = tier; // Tier 1 = Eff I, Tier 2 = Eff II, Tier 3 = Eff III

        if (enchantLevel > 0) {
            // Re-create pick, shovel and axe with efficiency enchant added
            for (String key : new String[]{"pick", "shovel", "axe", "hoe"}) {
                if (permanentItems.containsKey(key)) {
                    ItemStack current = permanentItems.get(key);
                    // Only add if the tool doesn't already have a higher efficiency
                    int currentEff = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY, current);
                    if (enchantLevel > currentEff) {
                        // Create new stack with same item/tags but updated enchant
                        ItemStack updated = current.copy();
                        // Remove old efficiency if present
                        updated.getEnchantmentTags().removeIf(tag -> {
                            CompoundTag ct = (CompoundTag) tag;
                            String id = ct.getString("id");
                            return id.equals("minecraft:efficiency");
                        });
                        updated.enchant(Enchantments.BLOCK_EFFICIENCY, enchantLevel);
                        permanentItems.put(key, updated);
                    }
                }
            }
        }
    }

    /**
     * Get the current efficiency level
     */
    public static int getEfficiencyLevel() {
        return efficiencyLevel;
    }

    /**
     * Get the efficiency speed multiplier
     */
    public static float getEfficiencyMultiplier() {
        return switch (efficiencyLevel) {
            case 1 -> 1.2f;  // 20% faster
            case 2 -> 1.4f;  // 40% faster
            case 3 -> 1.7f;  // 70% faster
            default -> 1.0f; // No bonus
        };
    }

    /**
     * Create an enchanted item stack
     */
    private ItemStack createEnchantedStack(net.minecraft.world.item.Item item, net.minecraft.world.item.enchantment.Enchantment enchant, int level) {
        ItemStack stack = new ItemStack(item);
        stack.enchant(enchant, level);
        return stack;
    }

    /**
     * Create a permanent item with unbreakable tag
     */
    private ItemStack createPermanentItem(ItemStack stack, String key) {
        stack.getOrCreateTag().putBoolean("Unbreakable", true);
        stack.getOrCreateTag().putString("key", key);
        return stack;
    }

    /**
     * Initialize starting tools for a new game
     * Called when jailPlayers becomes false (game start)
     * Note: No hoe - it's useless for digging
     */
    public void initializeStartingTools() {
        LOGGER.info("Initializing starting tools for all players");

        // Create wooden tools (tier 0) - pick, shovel, axe, hoe
        ItemStack pick = createPermanentItem(new ItemStack(Items.WOODEN_PICKAXE), "pick");
        ItemStack shovel = createPermanentItem(new ItemStack(Items.WOODEN_SHOVEL), "shovel");
        ItemStack axe = createPermanentItem(new ItemStack(Items.WOODEN_AXE), "axe");
        ItemStack hoe = createPermanentItem(new ItemStack(Items.WOODEN_HOE), "hoe");

        // Create shop item (Nether Star)
        ItemStack shopItem = new ItemStack(Items.NETHER_STAR);
        shopItem.setHoverName(Component.literal("§eOpen Shop"));
        shopItem.getOrCreateTag().putBoolean("isShopItem", true);
        shopItem.getOrCreateTag().putString("key", "shop");

        // Add lore
        CompoundTag display = shopItem.getOrCreateTagElement("display");
        ListTag loreTag = new ListTag();
        loreTag.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7Right-click to open the shop"))));
        loreTag.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7Buy upgrades with fossils!"))));
        display.put("Lore", loreTag);

        // Create scaffolding
        ItemStack scaffolding = new ItemStack(Items.SCAFFOLDING, 64);
        scaffolding.getOrCreateTag().putString("key", "scaffolding");

        // Register as permanent items
        permanentItems.put("pick", pick);
        permanentItems.put("shovel", shovel);
        permanentItems.put("axe", axe);
        permanentItems.put("hoe", hoe);
        permanentItems.put("shop", shopItem);
        permanentItems.put("scaffolding", scaffolding);

        LOGGER.info("Starting tools registered as permanent items");
    }

    @SubscribeEvent
    public static void onGameTick(TickEvent.ServerTickEvent event) {
        // Only run on END phase to avoid double processing
        if (event.phase != TickEvent.Phase.END) return;

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            try {
                Set<String> foundItems = new HashSet<>();
                boolean needsBroadcast = false;

                for (MobEffectInstance effect : permanentEffects.values()) {
                    if (player.hasEffect(effect.getEffect())) {
                        if (player.getEffect(effect.getEffect()).getAmplifier() != effect.getAmplifier())
                            player.forceAddEffect(effect, player);
                    }
                    else {
                        player.forceAddEffect(effect, player);
                    }
                }

                // Check all inventory slots - use a copy of slot list to avoid concurrent modification
                List<Slot> slots = new ArrayList<>(player.containerMenu.slots);
                for (Slot slot : slots) {
                    if (!slot.hasItem())
                        continue;

                    ItemStack slotItem = slot.getItem();
                    if (slotItem.isEmpty()) continue;

                    CompoundTag tag = slotItem.getTag();
                    if (tag == null) continue;

                    String key = tag.getString("key");
                    if (key.isEmpty() || !permanentItems.containsKey(key))
                        continue;

                    ItemStack permanentItem = permanentItems.get(key);
                    if (!ItemStack.isSameItemSameTags(slotItem, permanentItem) ||
                            slotItem.getCount() != permanentItem.getCount()) {
                        slot.set(permanentItem.copy());
                        needsBroadcast = true;
                    }
                    foundItems.add(key);
                }

                // Check item on cursor - but don't modify if player is actively moving items
                ItemStack carriedItem = player.containerMenu.getCarried();
                if (!carriedItem.isEmpty()) {
                    CompoundTag tag = carriedItem.getTag();
                    if (tag != null) {
                        String ckey = tag.getString("key");
                        if (!ckey.isEmpty() && permanentItems.containsKey(ckey)) {
                            // Don't replace carried item - just mark as found to avoid duplication
                            foundItems.add(ckey);
                        }
                    }
                }

                // Give missing permanent items
                for (String key : permanentItems.keySet()) {
                    if (!foundItems.contains(key)) {
                        player.getInventory().add(permanentItems.get(key).copy());
                        needsBroadcast = true;
                    }
                }

                // Broadcast inventory changes only once per tick if needed
                if (needsBroadcast) {
                    player.containerMenu.broadcastFullState();
                }
            } catch (Exception e) {
                LOGGER.warn("Error in permanent item tick for player {}: {}", player.getName().getString(), e.getMessage());
            }
        }
    }
}
