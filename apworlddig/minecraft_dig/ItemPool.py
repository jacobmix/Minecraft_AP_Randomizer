from typing import List

from BaseClasses import Item
from worlds.AutoWorld import World

from . import Constants


def get_junk_item_names(rand, k: int) -> str:
	junk_weights = Constants.item_info["junk_weights"]
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

	# Fill remaining itempool with randomly generated junk
	junk = get_junk_item_names(mcworld.random, total_location_count - len(itempool))
	itempool += [mc_world.create_item(name) for name in junk]

	return itempool
