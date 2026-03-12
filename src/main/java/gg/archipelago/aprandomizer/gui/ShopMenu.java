package gg.archipelago.aprandomizer.gui;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.managers.ShopManager;
import io.github.archipelagomw.flags.NetworkItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class ShopMenu extends ChestMenu {
    private static final Logger LOGGER = LogManager.getLogger();

    private final ServerPlayer player;
    private final Container shopContainer;

    // Row 0: Upgrade items
    private static final int SLOT_TOOLS = 2;
    private static final int SLOT_HASTE = 3;
    private static final int SLOT_EXCAVATION = 4;
    private static final int SLOT_REACH = 5;
    private static final int SLOT_EFFICIENCY = 6;

    // Row 1: Info + separator
    private static final int SLOT_INFO = 45;

    // Rows 2-4: Item Shop (27 items, slots 18-44)
    private static final int ITEM_SHOP_START_SLOT = 18;
    public static final int ITEM_SHOP_COUNT = 27;
    public static final int ITEM_SHOP_COST = 20;

    // Item Shop AP location ID offset
    private static final long ITEM_SHOP_AP_START_ID = 54800;

    // Row 5: Close button
    private static final int SLOT_CLOSE = 53;

    public ShopMenu(int containerId, Inventory playerInventory, Container container, ServerPlayer player) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, container, 6);
        this.player = player;
        this.shopContainer = container;
        refreshShopItems();
    }

    /**
     * Open the shop for a player
     */
    public static void openShop(ServerPlayer player) {
        SimpleContainer container = new SimpleContainer(54);

        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ShopMenu(id, inv, container, player),
            getShopTitle()
        ));
    }

    /**
     * Get the shop title with current balance
     */
    private static Component getShopTitle() {
        int balance = APRandomizer.worldData != null ? APRandomizer.worldData.getFossilBalance() : 0;
        return Component.literal("Shop - Balance: " + balance + " Fossils");
    }

    /**
     * Refresh all shop items to show current state
     */
    public void refreshShopItems() {
        // Clear all slots first
        for (int i = 0; i < shopContainer.getContainerSize(); i++) {
            shopContainer.setItem(i, ItemStack.EMPTY);
        }

        // Row 0: Upgrade items
        shopContainer.setItem(SLOT_TOOLS, createToolsUpgradeItem());
        shopContainer.setItem(SLOT_HASTE, createUpgradeItem(ShopManager.CATEGORY_HASTE, Items.SUGAR));
        shopContainer.setItem(SLOT_EXCAVATION, createUpgradeItem(ShopManager.CATEGORY_EXCAVATION, Items.STRUCTURE_VOID));
        shopContainer.setItem(SLOT_REACH, createUpgradeItem(ShopManager.CATEGORY_REACH, Items.ENDER_PEARL));
        shopContainer.setItem(SLOT_EFFICIENCY, createUpgradeItem(ShopManager.CATEGORY_EFFICIENCY, Items.REDSTONE));

        // Row 1: Info
        shopContainer.setItem(SLOT_INFO, createInfoItem());

        // Rows 2-4: Item Shop bundles (locked by Progressive Shop tier)
        int shopTier = APRandomizer.worldData != null ? APRandomizer.worldData.getShopTierUnlocked() : 0;
        for (int i = 0; i < ITEM_SHOP_COUNT; i++) {
            int requiredTier = (i / 9) + 1; // items 0-8 = tier 1, 9-17 = tier 2, 18-26 = tier 3
            if (shopTier >= requiredTier) {
                shopContainer.setItem(ITEM_SHOP_START_SLOT + i, createItemShopBundle(i));
            } else {
                shopContainer.setItem(ITEM_SHOP_START_SLOT + i, createLockedShopItem(requiredTier));
            }
        }

        // Row 5: Close button
        shopContainer.setItem(SLOT_CLOSE, createCloseItem());
    }

    /**
     * Create a bundle item for an Item Shop location
     */
    private ItemStack createItemShopBundle(int shopIndex) {
        boolean purchased = APRandomizer.worldData != null &&
            APRandomizer.worldData.isShopLocationPurchased(shopIndex);

        ItemStack stack = purchased ?
            new ItemStack(Items.GRAY_STAINED_GLASS_PANE) :
            new ItemStack(Items.END_CRYSTAL);

        int flags = APRandomizer.worldData != null ?
            APRandomizer.worldData.getShopItemFlag(shopIndex) : 0;
        String itemName = APRandomizer.worldData != null ?
            APRandomizer.worldData.getShopItemName(shopIndex) : "Item Shop " + (shopIndex + 1);
        String playerName = APRandomizer.worldData != null ?
            APRandomizer.worldData.getShopItemPlayer(shopIndex) : "";

        // Color name by item classification
        String colorCode = getColorCodeForFlags(flags);
        String displayName;
        List<String> lore = new ArrayList<>();

        if (purchased) {
            displayName = "§8" + itemName + " §7(Purchased)";
            lore.add("§7Already purchased");
            if (!playerName.isEmpty()) {
                lore.add("§7For: §f" + playerName);
            }
        } else {
            displayName = colorCode + itemName;
            if (!playerName.isEmpty()) {
                lore.add("§7For: §f" + playerName);
            }
            String classificationName = getClassificationName(flags);
            lore.add("§7Type: " + colorCode + classificationName);
            lore.add("");
            lore.add("§7Cost: §e" + ITEM_SHOP_COST + " fossils");

            int balance = APRandomizer.worldData != null ? APRandomizer.worldData.getFossilBalance() : 0;
            if (balance >= ITEM_SHOP_COST) {
                lore.add("");
                lore.add("§aClick to purchase!");
            } else {
                lore.add("");
                lore.add("§cNot enough fossils!");
            }
        }

        setItemNameAndLore(stack, displayName, lore);
        stack.getOrCreateTag().putString("shopAction", "itemShop");
        stack.getOrCreateTag().putInt("shopIndex", shopIndex);

        // Add enchant glint to purchasable bundles
        if (!purchased) {
            int balance = APRandomizer.worldData != null ? APRandomizer.worldData.getFossilBalance() : 0;
            if (balance >= ITEM_SHOP_COST) {
                stack.enchant(Enchantments.UNBREAKING, 1);
                stack.getOrCreateTag().putInt("HideFlags", 1);
            }
        }

        return stack;
    }

    /**
     * Create a locked item for shop slots that require a higher Progressive Shop tier
     */
    private ItemStack createLockedShopItem(int requiredTier) {
        ItemStack stack = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
        List<String> lore = new ArrayList<>();
        lore.add("§cLocked");
        lore.add("§7Requires §eProgressive Shop §7Tier §e" + requiredTier);
        setItemNameAndLore(stack, "§8???", lore);
        return stack;
    }

    /**
     * Get the color code for item flags
     */
    private String getColorCodeForFlags(int flags) {
        if ((flags & NetworkItem.ADVANCEMENT) != 0) {
            return "§d"; // Light purple for progression
        } else if ((flags & NetworkItem.USEFUL) != 0) {
            return "§a"; // Green for useful
        } else if ((flags & NetworkItem.TRAP) != 0) {
            return "§c"; // Red for trap
        }
        return "§f"; // White for filler
    }

    /**
     * Get human-readable classification name
     */
    private String getClassificationName(int flags) {
        if ((flags & NetworkItem.ADVANCEMENT) != 0) {
            return "Progression";
        } else if ((flags & NetworkItem.USEFUL) != 0) {
            return "Useful";
        } else if ((flags & NetworkItem.TRAP) != 0) {
            return "Trap";
        }
        return "Filler";
    }

    /**
     * Create the tools upgrade item with pickaxe representing next tier
     */
    private ItemStack createToolsUpgradeItem() {
        int currentTier = APRandomizer.worldData != null ? APRandomizer.worldData.getPurchasedTier(ShopManager.CATEGORY_TOOLS) : 0;
        int nextTier = ShopManager.getNextPurchasableTier(ShopManager.CATEGORY_TOOLS);

        net.minecraft.world.item.Item pickaxeItem = getPickaxeForTier(nextTier >= 0 ? nextTier : currentTier);
        return createUpgradeItem(ShopManager.CATEGORY_TOOLS, pickaxeItem);
    }

    /**
     * Get the pickaxe item for a given tier
     */
    private net.minecraft.world.item.Item getPickaxeForTier(int tier) {
        return switch (tier) {
            case 0 -> Items.WOODEN_PICKAXE;
            case 1 -> Items.STONE_PICKAXE;
            case 2 -> Items.IRON_PICKAXE;
            case 3 -> Items.DIAMOND_PICKAXE;
            default -> Items.NETHERITE_PICKAXE;
        };
    }

    /**
     * Create an upgrade item for a category
     */
    private ItemStack createUpgradeItem(String category, net.minecraft.world.item.Item displayItem) {
        ItemStack stack = new ItemStack(displayItem);
        int currentTier = APRandomizer.worldData != null ? APRandomizer.worldData.getPurchasedTier(category) : 0;
        int nextTier = ShopManager.getNextPurchasableTier(category);

        String currentName = ShopManager.getTierName(category, currentTier);
        String displayName;
        List<String> lore = new ArrayList<>();
        boolean shouldGlow = false;

        if (nextTier < 0) {
            displayName = "§a" + currentName + " §7(MAX)";
            lore.add("§7Current tier: §f" + currentTier);
            lore.add("");
            lore.add("§aFully upgraded!");
        } else {
            String nextName = ShopManager.getTierName(category, nextTier);
            int price = ShopManager.getTierPrice(category, nextTier);
            boolean unlocked = ShopManager.isNextTierUnlocked(category);
            boolean canAfford = APRandomizer.worldData != null &&
                APRandomizer.worldData.getFossilBalance() >= price;

            displayName = "§e" + capitalizeFirst(category) + " Upgrade";
            lore.add("§7Current tier: §f" + currentName);
            lore.add("§7Next tier: §e" + nextName);
            lore.add("");

            if (unlocked) {
                shouldGlow = true;
                lore.add("§7Cost: §e" + price + " fossils");
                if (canAfford) {
                    lore.add("");
                    lore.add("§aClick to purchase!");
                } else {
                    lore.add("");
                    lore.add("§cNot enough fossils!");
                }
            } else {
                lore.add("§cLOCKED");
                lore.add("§7Needs unlock from Archipelago");
            }
        }

        setItemNameAndLore(stack, displayName, lore);
        stack.getOrCreateTag().putString("shopCategory", category);

        if (shouldGlow) {
            stack.enchant(Enchantments.UNBREAKING, 1);
            stack.getOrCreateTag().putInt("HideFlags", 1);
        }

        return stack;
    }

    /**
     * Create the info item showing fossil balance and stats
     */
    private ItemStack createInfoItem() {
        ItemStack stack = new ItemStack(Items.BOOK);
        int balance = APRandomizer.worldData != null ? APRandomizer.worldData.getFossilBalance() : 0;
        int collected = APRandomizer.worldData != null ? APRandomizer.worldData.getCollectedFossils().size() : 0;

        List<String> lore = new ArrayList<>();
        lore.add("§7Current balance: §e" + balance + " fossils");
        lore.add("§7Total collected: §e" + collected + " fossils");
        lore.add("");
        lore.add("§7Break blocks to find fossils!");
        lore.add("§7Use fossils to buy upgrades.");

        setItemNameAndLore(stack, "§bFossil Info", lore);
        stack.getOrCreateTag().putString("shopAction", "info");

        return stack;
    }

    /**
     * Create the close button
     */
    private ItemStack createCloseItem() {
        ItemStack stack = new ItemStack(Items.BARRIER);

        List<String> lore = new ArrayList<>();
        lore.add("§7Click to close the shop");

        setItemNameAndLore(stack, "§cClose", lore);
        stack.getOrCreateTag().putString("shopAction", "close");

        return stack;
    }

    /**
     * Helper to set item name and lore
     */
    private void setItemNameAndLore(ItemStack stack, String name, List<String> lore) {
        stack.setHoverName(Component.literal(name));

        CompoundTag display = stack.getOrCreateTagElement("display");
        ListTag loreTag = new ListTag();
        for (String line : lore) {
            loreTag.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(line))));
        }
        display.put("Lore", loreTag);
    }

    /**
     * Helper to capitalize first letter
     */
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player p) {
        // Only handle clicks on shop slots, not player inventory
        if (slotId < 0 || slotId >= 54) {
            return;
        }

        ItemStack clickedStack = shopContainer.getItem(slotId);
        if (clickedStack.isEmpty()) {
            return;
        }

        CompoundTag tag = clickedStack.getTag();
        if (tag == null) {
            return;
        }

        // Check for category upgrade click
        if (tag.contains("shopCategory")) {
            String category = tag.getString("shopCategory");
            int nextTier = ShopManager.getNextPurchasableTier(category);
            if (nextTier >= 0) {
                ShopManager.purchase(player, category, nextTier);
                refreshShopItems();
                player.containerMenu.broadcastFullState();
            }
            return;
        }

        // Check for action click
        if (tag.contains("shopAction")) {
            String action = tag.getString("shopAction");
            switch (action) {
                case "close":
                    player.closeContainer();
                    break;
                case "info":
                    break;
                case "itemShop":
                    handleItemShopPurchase(tag.getInt("shopIndex"));
                    break;
            }
        }
    }

    /**
     * Handle purchasing an Item Shop location
     */
    private void handleItemShopPurchase(int shopIndex) {
        if (APRandomizer.worldData == null) return;

        // Tier unlocked?
        int requiredTier = (shopIndex / 9) + 1;
        if (APRandomizer.worldData.getShopTierUnlocked() < requiredTier) {
            Utils.sendMessageToPlayer(player, "§cThis tier is locked! Need Progressive Shop Tier " + requiredTier + ".");
            player.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
            return;
        }

        // Already purchased?
        if (APRandomizer.worldData.isShopLocationPurchased(shopIndex)) {
            Utils.sendMessageToPlayer(player, "§cAlready purchased!");
            player.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
            return;
        }

        // Enough fossils?
        int balance = APRandomizer.worldData.getFossilBalance();
        if (balance < ITEM_SHOP_COST) {
            Utils.sendMessageToPlayer(player, "§cNot enough fossils! Need " + (ITEM_SHOP_COST - balance) + " more.");
            player.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
            return;
        }

        // Spend fossils
        if (!APRandomizer.worldData.spendFossils(ITEM_SHOP_COST)) {
            return;
        }

        // Mark as purchased
        APRandomizer.worldData.markShopLocationPurchased(shopIndex);

        // Check the AP location
        long locationID = ITEM_SHOP_AP_START_ID + shopIndex;
        APRandomizer.getAP().checkLocation(locationID);

        // Feedback
        Utils.sendMessageToAll("§a" + player.getName().getString() + " purchased §eItem Shop " + (shopIndex + 1) + "§a!");
        player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);

        LOGGER.info("{} purchased Item Shop {} (location {}). Remaining balance: {}",
            player.getName().getString(), shopIndex + 1, locationID, APRandomizer.worldData.getFossilBalance());

        // Save
        APRandomizer.getServer().execute(() -> {
            APRandomizer.getServer().saveEverything(true, true, true);
        });

        // Refresh the GUI
        refreshShopItems();
        player.containerMenu.broadcastFullState();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
