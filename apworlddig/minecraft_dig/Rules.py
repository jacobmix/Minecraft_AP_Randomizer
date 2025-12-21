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


def set_rules(mc_world: World) -> None:
    multiworld = mc_world.multiworld
    player = mc_world.player

    rules_lookup = get_rules_lookup(player)

    # Set entrance rules
    for entrance_name, rule in rules_lookup["entrances"].items():
        multiworld.get_entrance(entrance_name, player).access_rule = rule

    multiworld.completion_condition[player] = lambda state: state.has("Progressive Pickaxe", player, 5) \
                                                            and state.has("Progressive Shovel", player, 2) \
                                                            and state.has("Progressive Haste", player, 2)
