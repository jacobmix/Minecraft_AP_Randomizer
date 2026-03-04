package gg.archipelago.aprandomizer.managers.itemmanager;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.common.Utils.Utils;
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

    private long index = 50000;

    private final HashMap<Long, ProgressiveList<PermanentInterface>> progressiveItems = new HashMap<>() {{
        put(index++, new ProgressiveList<>() {{ //progressive pick
            add(new PermanentItem(new ItemStack(Items.WOODEN_PICKAXE), "pick"));
            add(new PermanentItem(new ItemStack(Items.STONE_PICKAXE), "pick"));
            add(new PermanentItem(new ItemStack(Items.IRON_PICKAXE), "pick"));
            add(new PermanentItem(new ItemStack(Items.DIAMOND_PICKAXE), "pick"));

            ItemStack eff3 = new ItemStack(Items.NETHERITE_PICKAXE);
            eff3.enchant(Enchantments.BLOCK_EFFICIENCY, 3);
            add(new PermanentItem(eff3, "pick"));

            ItemStack eff5 = new ItemStack(Items.NETHERITE_PICKAXE);
            eff5.enchant(Enchantments.BLOCK_EFFICIENCY, 5);
            add(new PermanentItem(eff5, "pick"));

            ItemStack eff7 = new ItemStack(Items.NETHERITE_PICKAXE);
            eff7.enchant(Enchantments.BLOCK_EFFICIENCY, 7);
            add(new PermanentItem(eff7, "pick"));

            ItemStack eff9 = new ItemStack(Items.NETHERITE_PICKAXE);
            eff9.enchant(Enchantments.BLOCK_EFFICIENCY, 9);
            add(new PermanentItem(eff9, "pick"));
        }});
        put(index++, new ProgressiveList<>() {{ //progressive shovel
            add(new PermanentItem(new ItemStack(Items.WOODEN_SHOVEL), "shovel"));
            add(new PermanentItem(new ItemStack(Items.STONE_SHOVEL), "shovel"));
            add(new PermanentItem(new ItemStack(Items.IRON_SHOVEL), "shovel"));
            add(new PermanentItem(new ItemStack(Items.DIAMOND_SHOVEL), "shovel"));

            ItemStack eff3 = new ItemStack(Items.NETHERITE_SHOVEL);
            eff3.enchant(Enchantments.BLOCK_EFFICIENCY, 3);
            add(new PermanentItem(eff3, "shovel"));

            ItemStack eff5 = new ItemStack(Items.NETHERITE_SHOVEL);
            eff5.enchant(Enchantments.BLOCK_EFFICIENCY, 5);
            add(new PermanentItem(eff5, "shovel"));

            ItemStack eff7 = new ItemStack(Items.NETHERITE_SHOVEL);
            eff7.enchant(Enchantments.BLOCK_EFFICIENCY, 7);
            add(new PermanentItem(eff7, "shovel"));
        }});
        put(index++, new ProgressiveList<>() {{ //progressive axe
            add(new PermanentItem(new ItemStack(Items.WOODEN_AXE), "axe"));
            add(new PermanentItem(new ItemStack(Items.STONE_AXE), "axe"));
            add(new PermanentItem(new ItemStack(Items.IRON_AXE), "axe"));
            add(new PermanentItem(new ItemStack(Items.DIAMOND_AXE), "axe"));
            add(new PermanentItem(new ItemStack(Items.NETHERITE_AXE), "axe"));
        }});
        put(index++, new ProgressiveList<>() {{ //progressive hoe
            add(new PermanentItem(new ItemStack(Items.WOODEN_HOE), "hoe"));
            add(new PermanentItem(new ItemStack(Items.STONE_HOE), "hoe"));
            add(new PermanentItem(new ItemStack(Items.IRON_HOE), "hoe"));
            add(new PermanentItem(new ItemStack(Items.DIAMOND_HOE), "hoe"));
            add(new PermanentItem(new ItemStack(Items.NETHERITE_HOE), "hoe"));
        }});
        put(index++, new ProgressiveList<>() {{ //progressive haste (nerfed to 1 tier)
            add(new PermanentEffect(new MobEffectInstance(MobEffects.DIG_SPEED, MobEffectInstance.INFINITE_DURATION, 0, true, false), "haste"));
        }});

    }};

    private final HashMap<Long, ItemStack> itemStacks = new HashMap<>() {{
        put(index++, new ItemStack(Items.TNT, 16));

        ItemStack goldenPick = new ItemStack(Items.GOLDEN_PICKAXE);
        goldenPick.enchant(Enchantments.BLOCK_EFFICIENCY, 10);
        goldenPick.enchant(Enchantments.UNBREAKING, 5);
        put(index++, goldenPick);

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
        put(index++, trueGoldenPick);

        ItemStack DefensiveFish = new ItemStack(Items.SALMON);
        DefensiveFish.enchant(Enchantments.KNOCKBACK, 10);
        put(index++, DefensiveFish);
    }};

    private final HashMap<Long, Callable<Trap>> trapData = new HashMap<>() {{
        put(index++, BeeTrap::new);
        put(index++, CreeperTrap::new);
        put(index++, SandRain::new);
        put(index++, FakeWither::new);
        put(index++, GoonTrap::new);
        put(index++, FishFountainTrap::new);
        put(index++, MiningFatigueTrap::new);
        put(index++, BlindnessTrap::new);
        put(index++, PhantomTrap::new);
        put(index++, WaterTrap::new);
        put(index++, GhastTrap::new);
        put(index++, LevitateTrap::new);
        put(index++, AboutFaceTrap::new);
        put(index++, AnvilTrap::new);
    }};

    private final HashMap<Long, Power> powers = new HashMap<>() {{
        put(index++, new ExcavationPower());
    }};

    private List<Long> receivedItems = new ArrayList<>();

    private static final HashMap<String, ItemStack> permanentItems = new HashMap<>();
    private static final HashMap<String, MobEffectInstance> permanentEffects = new HashMap<>();

    public ItemManager() {
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


    // Item IDs based on position in all_items array + 50000 offset
    private static final long WORLD_BARRIER_EXPANSION_ID = 50024;
    private static final long HASTE_BOOST_ID = 50025;
    private static final long EXCAVATION_BOOST_ID = 50026;

    public boolean giveItemToAll(long itemID, long index) {
        receivedItems.add(itemID);

        if (progressiveItems.containsKey(itemID)) {
            progressiveItems.get(itemID).getNext().applyEffect();
        }

        if (powers.containsKey(itemID)) {
            powers.get(itemID).grantPower();
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

        if(APRandomizer.worldData.getIndex() < index) {
            APRandomizer.getServer().execute(() -> {
                APRandomizer.worldData.setIndex(index);

                boolean isPhysicalItem = itemStacks.containsKey(itemID);
                boolean isTrap = trapData.containsKey(itemID);

                // Get online player UUIDs
                Set<UUID> onlinePlayerUUIDs = new HashSet<>();
                for (ServerPlayer player : APRandomizer.getServer().getPlayerList().getPlayers()) {
                    onlinePlayerUUIDs.add(player.getUUID());
                    giveItem(itemID, player);
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
        return false;
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
                player.sendSystemMessage(Component.literal("§aWhile you were offline, you received:"));
                for (String name : itemNames) {
                    player.sendSystemMessage(Component.literal("  §7- §f" + name));
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
        } else if (progressiveItems.containsKey(itemID)) {
            return "Progressive Item";
        } else if (powers.containsKey(itemID)) {
            return "Power Upgrade";
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
