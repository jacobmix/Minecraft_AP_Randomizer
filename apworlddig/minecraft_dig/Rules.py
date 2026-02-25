import math
from worlds.AutoWorld import World


def get_rules_lookup(player: int):
    rules_lookup = {
        "entrances": {
            "Top[exit]": lambda state: state.has('Progressive Shovel', player, 1),
            "Shovel1[exit]": lambda state: state.has('Progressive Shovel', player, 2),
            "Shovel2[exit]": lambda state: state.has('Progressive Pickaxe', player, 1),
            "Pick1[exit]": lambda state: state.has('Progressive Pickaxe', player, 2),
            "Pick2[exit]": lambda state: state.has('Progressive Haste', player, 1),
            "Haste1[exit]": lambda state: state.has('Progressive Pickaxe', player, 3),
            "Pick3[exit]": lambda state: state.has('Progressive Pickaxe', player, 4),
            "Pick4[exit]": lambda state: state.has('Progressive Haste', player, 2),
            "Haste2[exit]": lambda state: state.has('Progressive Pickaxe', player, 5)
        }
    }
    return rules_lookup


def get_expansions_needed_for_chunk(chunk_index: int) -> int:
    """Calculate how many World Barrier Expansion items are needed to access a chunk.

    Tier 1 (side=1): chunk 0 -> 0 expansions
    Tier 2 (side=2): chunks 1-3 -> 1 expansion
    Tier 3 (side=3): chunks 4-8 -> 2 expansions
    etc.
    """
    if chunk_index == 0:
        return 0
    tier = math.ceil(math.sqrt(chunk_index + 1))
    return tier - 1


def set_rules(mc_world: World) -> None:
    multiworld = mc_world.multiworld
    player = mc_world.player

    rules_lookup = get_rules_lookup(player)

    # Set entrance rules
    for entrance_name, rule in rules_lookup["entrances"].items():
        multiworld.get_entrance(entrance_name, player).access_rule = rule

    # If progressive chunks is enabled, add access rules for chunk locations
    if mc_world.options.progressive_chunks.value:
        chunk_count = mc_world.options.chunk_count.value
        for c in range(chunk_count):
            expansions_needed = get_expansions_needed_for_chunk(c)
            if expansions_needed > 0:
                # Add rule to all locations in this chunk
                for loc in multiworld.get_locations(player):
                    if loc.name.startswith(f"Chunk {c} Layer "):
                        original_rule = loc.access_rule
                        needed = expansions_needed  # Capture in closure
                        loc.access_rule = lambda state, orig=original_rule, n=needed: \
                            orig(state) and state.has("World Barrier Expansion", player, n)

    multiworld.completion_condition[player] = lambda state: state.has("Progressive Pickaxe", player, 5) \
                                                            and state.has("Progressive Shovel", player, 2) \
                                                            and state.has("Progressive Haste", player, 2)
