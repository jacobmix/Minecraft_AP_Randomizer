package gg.archipelago.aprandomizer.capability.providers;

import gg.archipelago.aprandomizer.capability.APCapabilities;
import gg.archipelago.aprandomizer.capability.data.WorldData;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class WorldDataProvider implements ICapabilitySerializable<Tag> {


    private final WorldData worldData = new WorldData();

    /**
     * Asks the Provider if it has the given capability
     *
     * @param capability<T> capability to be checked for
     * @param facing        the side of the provider being checked (null = no particular side)
     * @param <T>           The interface instance that is used
     * @return a lazy-initialisation supplier of the interface instance that is used to access this capability
     * In this case, we don't actually use lazy initialisation because the instance is very quick to create.
     * See CapabilityProviderFlowerBag for an example of lazy initialisation
     */
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        if (APCapabilities.WORLD_DATA == capability) {
            return (LazyOptional<T>) LazyOptional.of(() -> worldData);
            // why are we using a lambda?  Because LazyOptional.of() expects a NonNullSupplier interface.  The lambda automatically
            //   conforms itself to that interface.  This save me having to define an inner class implementing NonNullSupplier.
            // The explicit cast to LazyOptional<T> is required because our CAPABILITY_ELEMENTAL_FIRE can't be typed.  Our code has
            //   checked that the requested capability matches, so the explicit cast is safe (unless you have made a mistake and mixed them up!)
        }
        return LazyOptional.empty();
        // Note that if you are implementing getCapability in a derived class which implements ICapabilityProvider
        // eg you have added a new MyEntity which has the method MyEntity::getCapability instead of using AttachCapabilitiesEvent to attach a
        // separate class, then you should call
        // return super.getCapability(capability, facing);
        //   instead of
        // return LazyOptional.empty();
    }

    @Override
    public Tag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("dragonState", worldData.getDragonState());
        nbt.putString("seedName", worldData.getSeedName());
        nbt.putBoolean("jailPlayers", worldData.getJailPlayers());
        nbt.putLong("index", worldData.getIndex());
        nbt.putString("serverAddress", worldData.getServerAddress());
        nbt.putInt("unlockedChunkLevel", worldData.getUnlockedChunkLevel());

        // Serialize known players
        ListTag knownPlayersList = new ListTag();
        for (UUID uuid : worldData.getKnownPlayers()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("uuid", uuid);
            knownPlayersList.add(playerTag);
        }
        nbt.put("knownPlayers", knownPlayersList);

        // Serialize pending bonuses
        ListTag pendingBonusesList = new ListTag();
        for (var entry : worldData.getAllPendingBonuses().entrySet()) {
            CompoundTag playerBonusTag = new CompoundTag();
            playerBonusTag.putUUID("uuid", entry.getKey());

            CompoundTag bonusesTag = new CompoundTag();
            for (var bonusEntry : entry.getValue().entrySet()) {
                bonusesTag.putInt(bonusEntry.getKey(), bonusEntry.getValue());
            }
            playerBonusTag.put("bonuses", bonusesTag);
            pendingBonusesList.add(playerBonusTag);
        }
        nbt.put("pendingBonuses", pendingBonusesList);

        // Serialize pending items
        ListTag pendingItemsList = new ListTag();
        for (var entry : worldData.getAllPendingItems().entrySet()) {
            CompoundTag playerItemsTag = new CompoundTag();
            playerItemsTag.putUUID("uuid", entry.getKey());

            long[] itemIds = entry.getValue().stream().mapToLong(Long::longValue).toArray();
            playerItemsTag.putLongArray("items", itemIds);
            pendingItemsList.add(playerItemsTag);
        }
        nbt.put("pendingItems", pendingItemsList);

        // Serialize fossil system fields
        nbt.putInt("fossilBalance", worldData.getFossilBalance());

        // Serialize unlocked tiers
        ListTag unlockedTiersList = new ListTag();
        for (String tier : worldData.getUnlockedTiers()) {
            CompoundTag tierTag = new CompoundTag();
            tierTag.putString("tier", tier);
            unlockedTiersList.add(tierTag);
        }
        nbt.put("unlockedTiers", unlockedTiersList);

        // Serialize purchased tiers
        CompoundTag purchasedTiersTag = new CompoundTag();
        for (var entry : worldData.getPurchasedTiers().entrySet()) {
            purchasedTiersTag.putInt(entry.getKey(), entry.getValue());
        }
        nbt.put("purchasedTiers", purchasedTiersTag);

        // Serialize generated fossils (all fossil positions)
        long[] generatedFossilsArray = worldData.getGeneratedFossils().stream().mapToLong(Long::longValue).toArray();
        nbt.putLongArray("generatedFossils", generatedFossilsArray);

        // Serialize collected fossils
        long[] collectedFossilsArray = worldData.getCollectedFossils().stream().mapToLong(Long::longValue).toArray();
        nbt.putLongArray("collectedFossils", collectedFossilsArray);

        // Serialize purchased shop locations
        int[] purchasedShopArray = worldData.getPurchasedShopLocations().stream().mapToInt(Integer::intValue).toArray();
        nbt.putIntArray("purchasedShopLocations", purchasedShopArray);

        // Serialize shop item flags
        CompoundTag shopFlagsTag = new CompoundTag();
        for (var entry : worldData.getShopItemFlags().entrySet()) {
            shopFlagsTag.putInt(String.valueOf(entry.getKey()), entry.getValue());
        }
        nbt.put("shopItemFlags", shopFlagsTag);

        // Serialize shop item names
        CompoundTag shopNamesTag = new CompoundTag();
        for (var entry : worldData.getShopItemNames().entrySet()) {
            shopNamesTag.putString(String.valueOf(entry.getKey()), entry.getValue());
        }
        nbt.put("shopItemNames", shopNamesTag);

        // Serialize shop item players
        CompoundTag shopPlayersTag = new CompoundTag();
        for (var entry : worldData.getShopItemPlayers().entrySet()) {
            shopPlayersTag.putString(String.valueOf(entry.getKey()), entry.getValue());
        }
        nbt.put("shopItemPlayers", shopPlayersTag);

        nbt.putBoolean("goalCompleted", worldData.isGoalCompleted());

        return nbt;
    }

    @Override
    public void deserializeNBT(Tag nbt) {
        if (nbt.getType() == CompoundTag.TYPE) {
            CompoundTag read = (CompoundTag) nbt;
            worldData.setSeedName(read.getString("seedName"));
            worldData.setDragonState(read.getInt("dragonState"));
            worldData.setJailPlayers(read.getBoolean("jailPlayers"));
            worldData.setIndex(read.getLong("index"));
            worldData.setServerAddress(read.getString("serverAddress"));
            // Default to 1 if not present (for backwards compatibility)
            worldData.setUnlockedChunkLevel(read.contains("unlockedChunkLevel") ? read.getInt("unlockedChunkLevel") : 1);

            // Deserialize known players
            if (read.contains("knownPlayers")) {
                Set<UUID> knownPlayers = new HashSet<>();
                ListTag knownPlayersList = read.getList("knownPlayers", Tag.TAG_COMPOUND);
                for (int i = 0; i < knownPlayersList.size(); i++) {
                    CompoundTag playerTag = knownPlayersList.getCompound(i);
                    knownPlayers.add(playerTag.getUUID("uuid"));
                }
                worldData.setKnownPlayers(knownPlayers);
            }

            // Deserialize pending bonuses
            if (read.contains("pendingBonuses")) {
                HashMap<UUID, HashMap<String, Integer>> pendingBonuses = new HashMap<>();
                ListTag pendingBonusesList = read.getList("pendingBonuses", Tag.TAG_COMPOUND);
                for (int i = 0; i < pendingBonusesList.size(); i++) {
                    CompoundTag playerBonusTag = pendingBonusesList.getCompound(i);
                    UUID uuid = playerBonusTag.getUUID("uuid");

                    HashMap<String, Integer> bonuses = new HashMap<>();
                    CompoundTag bonusesTag = playerBonusTag.getCompound("bonuses");
                    for (String key : bonusesTag.getAllKeys()) {
                        bonuses.put(key, bonusesTag.getInt(key));
                    }
                    pendingBonuses.put(uuid, bonuses);
                }
                worldData.setAllPendingBonuses(pendingBonuses);
            }

            // Deserialize pending items
            if (read.contains("pendingItems")) {
                HashMap<UUID, List<Long>> pendingItems = new HashMap<>();
                ListTag pendingItemsList = read.getList("pendingItems", Tag.TAG_COMPOUND);
                for (int i = 0; i < pendingItemsList.size(); i++) {
                    CompoundTag playerItemsTag = pendingItemsList.getCompound(i);
                    UUID uuid = playerItemsTag.getUUID("uuid");

                    List<Long> items = new ArrayList<>();
                    for (long itemId : playerItemsTag.getLongArray("items")) {
                        items.add(itemId);
                    }
                    pendingItems.put(uuid, items);
                }
                worldData.setAllPendingItems(pendingItems);
            }

            // Deserialize fossil system fields
            if (read.contains("fossilBalance")) {
                worldData.setFossilBalance(read.getInt("fossilBalance"));
            }

            // Deserialize unlocked tiers
            if (read.contains("unlockedTiers")) {
                Set<String> unlockedTiers = new HashSet<>();
                ListTag unlockedTiersList = read.getList("unlockedTiers", Tag.TAG_COMPOUND);
                for (int i = 0; i < unlockedTiersList.size(); i++) {
                    CompoundTag tierTag = unlockedTiersList.getCompound(i);
                    unlockedTiers.add(tierTag.getString("tier"));
                }
                worldData.setUnlockedTiers(unlockedTiers);
            }

            // Deserialize purchased tiers
            if (read.contains("purchasedTiers")) {
                HashMap<String, Integer> purchasedTiers = new HashMap<>();
                CompoundTag purchasedTiersTag = read.getCompound("purchasedTiers");
                for (String key : purchasedTiersTag.getAllKeys()) {
                    purchasedTiers.put(key, purchasedTiersTag.getInt(key));
                }
                worldData.setPurchasedTiers(purchasedTiers);
            }

            // Deserialize generated fossils (all fossil positions)
            if (read.contains("generatedFossils")) {
                Set<Long> generatedFossils = new HashSet<>();
                for (long posLong : read.getLongArray("generatedFossils")) {
                    generatedFossils.add(posLong);
                }
                worldData.setGeneratedFossils(generatedFossils);
            }

            // Deserialize collected fossils
            if (read.contains("collectedFossils")) {
                Set<Long> collectedFossils = new HashSet<>();
                for (long posLong : read.getLongArray("collectedFossils")) {
                    collectedFossils.add(posLong);
                }
                worldData.setCollectedFossils(collectedFossils);
            }

            // Deserialize purchased shop locations
            if (read.contains("purchasedShopLocations")) {
                Set<Integer> purchasedShop = new HashSet<>();
                for (int idx : read.getIntArray("purchasedShopLocations")) {
                    purchasedShop.add(idx);
                }
                worldData.setPurchasedShopLocations(purchasedShop);
            }

            // Deserialize shop item flags
            if (read.contains("shopItemFlags")) {
                Map<Integer, Integer> shopFlags = new HashMap<>();
                CompoundTag shopFlagsTag = read.getCompound("shopItemFlags");
                for (String key : shopFlagsTag.getAllKeys()) {
                    shopFlags.put(Integer.parseInt(key), shopFlagsTag.getInt(key));
                }
                worldData.setShopItemFlags(shopFlags);
            }

            // Deserialize shop item names
            if (read.contains("shopItemNames")) {
                Map<Integer, String> shopNames = new HashMap<>();
                CompoundTag shopNamesTag = read.getCompound("shopItemNames");
                for (String key : shopNamesTag.getAllKeys()) {
                    shopNames.put(Integer.parseInt(key), shopNamesTag.getString(key));
                }
                worldData.setShopItemNames(shopNames);
            }

            // Deserialize goal completed
            if (read.contains("goalCompleted")) {
                worldData.setGoalCompleted(read.getBoolean("goalCompleted"));
            }

            // Deserialize shop item players
            if (read.contains("shopItemPlayers")) {
                Map<Integer, String> shopPlayers = new HashMap<>();
                CompoundTag shopPlayersTag = read.getCompound("shopItemPlayers");
                for (String key : shopPlayersTag.getAllKeys()) {
                    shopPlayers.put(Integer.parseInt(key), shopPlayersTag.getString(key));
                }
                worldData.setShopItemPlayers(shopPlayers);
            }
        }
    }
}