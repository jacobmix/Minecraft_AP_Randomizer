import math
from worlds.AutoWorld import World


def get_rules_lookup(player: int):
    # Progressive Tools: 1=Stone, 2=Iron, 3=Diamond, 4=Netherite
    # Progressive Efficiency: 1=Eff I, 2=Eff II, 3=Eff III
    # Progressive Haste: 1=Haste I
    rules_lookup = {
        "entrances": {
            "Top[exit]": lambda state: state.has('Progressive Tools', player, 1),
            "Shovel1[exit]": lambda state: state.has('Progressive Tools', player, 2),
            "Shovel2[exit]": lambda state: state.has('Progressive Tools', player, 2),
            "Pick1[exit]": lambda state: state.has('Progressive Tools', player, 3),
            "Pick2[exit]": lambda state: state.has('Progressive Haste', player, 1),
            "Haste1[exit]": lambda state: state.has('Progressive Tools', player, 4),
            "Pick3[exit]": lambda state: state.has('Progressive Efficiency', player, 1),
            "Pick4[exit]": lambda state: state.has('Progressive Efficiency', player, 2),
            "Haste2[exit]": lambda state: state.has('Progressive Efficiency', player, 3)
        }
    }
    return rules_lookup


def get_expansions_needed_for_chunk(chunk_index: int, chunk_count: int) -> int:
    """Calculate how many World Barrier Expansion items are needed to access a chunk.

    The grid is side x side where side = ceil(sqrt(chunk_count)).
    Chunk at grid position (cx, cz) needs side = max(cx, cz) + 1.
    Expansions needed = needed_side - 1.

    Example 3x3:
      chunk 0 (0,0) -> 0 exp | chunk 1 (1,0) -> 1 exp | chunk 2 (2,0) -> 2 exp
      chunk 3 (0,1) -> 1 exp | chunk 4 (1,1) -> 1 exp | chunk 5 (2,1) -> 2 exp
      chunk 6 (0,2) -> 2 exp | chunk 7 (1,2) -> 2 exp | chunk 8 (2,2) -> 2 exp
    """
    if chunk_index == 0:
        return 0
    side = math.ceil(math.sqrt(chunk_count))
    cx = chunk_index % side
    cz = chunk_index // side
    needed_side = max(cx, cz) + 1
    return needed_side - 1


def set_rules(mc_world: World) -> None:
    multiworld = mc_world.multiworld
    player = mc_world.player

    rules_lookup = get_rules_lookup(player)

    # Set entrance rules
    for entrance_name, rule in rules_lookup["entrances"].items():
        multiworld.get_entrance(entrance_name, player).access_rule = rule

    # Item Shop tier access rules (Progressive Shop unlocks rows)
    multiworld.get_entrance("Menu -> Item Shop Tier 1", player).access_rule = \
        lambda state: state.has("Progressive Shop", player, 1)
    multiworld.get_entrance("Menu -> Item Shop Tier 2", player).access_rule = \
        lambda state: state.has("Progressive Shop", player, 2)
    multiworld.get_entrance("Menu -> Item Shop Tier 3", player).access_rule = \
        lambda state: state.has("Progressive Shop", player, 3)

    # If progressive chunks is enabled, add access rules for chunk locations
    if mc_world.options.progressive_chunks.value:
        chunk_count = mc_world.options.chunk_count.value
        for c in range(chunk_count):
            expansions_needed = get_expansions_needed_for_chunk(c, chunk_count)
            if expansions_needed > 0:
                # Add rule to all locations in this chunk
                for loc in multiworld.get_locations(player):
                    if loc.name.startswith(f"Chunk {c} Layer "):
                        original_rule = loc.access_rule
                        needed = expansions_needed  # Capture in closure
                        loc.access_rule = lambda state, orig=original_rule, n=needed: \
                            orig(state) and state.has("World Barrier Expansion", player, n)

    # Completion requires max tools, haste and efficiency
    multiworld.completion_condition[player] = lambda state: state.has("Progressive Tools", player, 4) \
                                                            and state.has("Progressive Haste", player, 1) \
                                                            and state.has("Progressive Efficiency", player, 3)
