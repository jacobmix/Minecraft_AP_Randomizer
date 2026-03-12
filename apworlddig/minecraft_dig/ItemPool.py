import math
from typing import List

from BaseClasses import Item
from worlds.AutoWorld import World

from . import Constants


def get_junk_item_names(rand, k: int, trap_weights: dict) -> List[str]:
    # Start with base junk weights (non-trap items)
    junk_weights = {}
    trap_items = set(Constants.item_info["trap_items"])

    # Add non-trap items from default weights
    for item, weight in Constants.item_info["junk_weights"].items():
        if item not in trap_items:
            junk_weights[item] = weight

    # Add trap items with custom weights from options
    junk_weights.update(trap_weights)

    # Filter out items with 0 weight
    junk_weights = {k: v for k, v in junk_weights.items() if v > 0}

    if not junk_weights:
        # Fallback if all weights are 0
        junk_weights = {"TNT": 1}

    junk = rand.choices(
        list(junk_weights.keys()),
        weights=list(junk_weights.values()),
        k=k)
    return junk


def build_item_pool(mc_world: World) -> List[Item]:
    mcworld = mc_world.multiworld
    player = mc_world.player

    itempool = []
    total_location_count = len(mcworld.get_unfilled_locations(player))

    required_pool = Constants.item_info["required_pool"]

    # Add required progression items
    for item_name, num in required_pool.items():
        itempool += [mc_world.create_item(item_name) for _ in range(num)]

    # Add World Barrier Expansion items if progressive chunks is enabled
    if mc_world.options.progressive_chunks.value:
        chunk_count = mc_world.options.chunk_count.value
        # Expansions are by tier (1x1 -> 2x2 -> 3x3 -> etc.)
        # tier = ceil(sqrt(chunk_count)), expansions needed = tier - 1
        tier = math.ceil(math.sqrt(chunk_count))
        expansion_count = tier - 1
        if expansion_count > 0:
            itempool += [mc_world.create_item("World Barrier Expansion") for _ in range(expansion_count)]

    # Build trap weights from options
    trap_weights = {
        "Bee Trap": mc_world.options.bee_trap_weight.value,
        "Creeper Trap": mc_world.options.creeper_trap_weight.value,
        "Sand Rain": mc_world.options.sand_rain_weight.value,
        "Spawn Wither": mc_world.options.spawn_wither_weight.value,
        "Goon Squad": mc_world.options.goon_squad_weight.value,
        "Fish Fountain": mc_world.options.fish_fountain_weight.value,
        "Bad Air": mc_world.options.bad_air_weight.value,
        "Pocket Sand": mc_world.options.pocket_sand_weight.value,
        "Pesky Bird": mc_world.options.pesky_bird_weight.value,
        "Flash Flood": mc_world.options.flash_flood_weight.value,
        "Pet the kitty": mc_world.options.pet_the_kitty_weight.value,
        "Wingardium Leviosa": mc_world.options.wingardium_leviosa_weight.value,
        "About Face": mc_world.options.about_face_weight.value,
        "Acme Delivery": mc_world.options.acme_delivery_weight.value,
        "Meteor Shower": mc_world.options.meteor_shower_weight.value,
        "Increased Gravity": mc_world.options.increased_gravity_weight.value,
    }

    # Fill remaining itempool with randomly generated junk
    junk = get_junk_item_names(mcworld.random, total_location_count - len(itempool), trap_weights)
    itempool += [mc_world.create_item(name) for name in junk]

    return itempool
