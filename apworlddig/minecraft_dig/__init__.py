import os
import json
import settings
import typing
from base64 import b64encode, b64decode
from typing import Dict, Any

from BaseClasses import Region, Entrance, Item, ItemClassification, Location
from worlds.AutoWorld import World

from . import Constants
from .ItemPool import build_item_pool, get_junk_item_names
from .Options import MinecraftDigOptions
from .Rules import set_rules
from .MinecraftDigPatch import MinecraftDigProcedurePatch
from .MinecraftDigClient import add_to_launcher_components

add_to_launcher_components()
client_version = 11
GAME_NAME = "Minecraft Dig"

class MinecraftDigSettings(settings.Group):
    """
    Host.yaml settings for Minecraft Dig client.
    """
    class ForgeDirectory(settings.OptionalUserFolderPath):
        """
        Forge Server Folder.
        """
        pass
    class ReleaseChannel(str):
        """
        release channel, currently "release", or "beta"
        """
    class MCLaunch(str):
        """
        Path + arguments to auto-launch Minecraft.
        Example: '"C:/Users/<USER>/AppData/Local/Programs/MultiMC/MultiMC.exe" -d "C:/Users/<USER/AppData/Local/Programs/MultiMC" -l "1.19.4" -s "localhost" -a "<USER>"'
        """
        pass
    class JavaPath(str):
        """
        Java path.
        For Linux/Mac or if you wanna simply use an exisiting install.
        Example: "/usr/lib/jvm/default/bin/java"
        """
        pass

    forge_directory: ForgeDirectory = ForgeDirectory("Minecraft Dig Forge server")
    max_heap_size: str = "2G"
    release_channel: ReleaseChannel = ReleaseChannel("release")
    mc_launch: MCLaunch = MCLaunch("")
    java: JavaPath = JavaPath("")

class MinecraftDigWorld(World):
    """
    Minecraft Dig - dig a hole.
    """
    game = GAME_NAME
    options_dataclass = MinecraftDigOptions
    options: MinecraftDigOptions
    settings: typing.ClassVar[MinecraftDigSettings] = MinecraftDigSettings()
    topology_present = False

    item_name_to_id = Constants.item_name_to_id
    location_name_to_id = Constants.location_name_to_id

    data_version = 0

    def _get_mc_data(self) -> Dict[str, Any]:
        return {
            'world_seed': self.random.getrandbits(32),
            'seed_name': self.multiworld.seed_name,
            'player_name': self.multiworld.get_player_name(self.player),
            'player_id': self.player,
            'client_version': client_version,
            'race': self.multiworld.is_race,
            'chunk_count': self.options.chunk_count.value,
            'progressive_chunks': bool(self.options.progressive_chunks.value),
        }

    def create_item(self, name: str) -> Item:
        item_class = ItemClassification.filler
        if name in Constants.item_info["progression_items"]:
            item_class = ItemClassification.progression
        elif name in Constants.item_info["useful_items"]:
            item_class = ItemClassification.useful
        elif name in Constants.item_info["trap_items"]:
            item_class = ItemClassification.trap

        return MinecraftDigItem(name, item_class, self.item_name_to_id.get(name, None), self.player)

    def create_event(self, region_name: str, event_name: str) -> None:
        region = self.multiworld.get_region(region_name, self.player)
        loc = MinecraftDigLocation(self.player, event_name, None, region)
        loc.place_locked_item(self.create_event_item(event_name))
        region.locations.append(loc)

    def create_event_item(self, name: str) -> Item:
        item = self.create_item(name)
        item.classification = ItemClassification.progression
        return item

    def create_regions(self) -> None:
        chunk_count = self.options.chunk_count.value
        # Create regions and generate location names
        for region_name, exits, layer_range in Constants.region_info["regions"]:
            r = Region(region_name, self.player, self.multiworld)

            # create exits for region
            for exit_name in exits:
                r.exits.append(Entrance(self.player, exit_name, r))

            # generate Location's from range, one per chunk per layer
            if layer_range is not None:
                for c in range(chunk_count):
                    for layerID in range(layer_range["top"], layer_range["bottom"]-1, -1):
                        loc_name = f"Chunk {c} Layer {layerID}"
                        loc = MinecraftDigLocation(self.player, loc_name,
                                                self.location_name_to_id.get(loc_name, None), r)
                        r.locations.append(loc)

            self.multiworld.regions.append(r)

        # Bind mandatory connections
        for entr_name, region_name in Constants.region_info["mandatory_connections"]:
            e = self.multiworld.get_entrance(entr_name, self.player)
            r = self.multiworld.get_region(region_name, self.player)
            e.connect(r)

    def create_items(self) -> None:
        # Give starting tools so the player can dig from the start
        for name in ["Progressive Pickaxe", "Progressive Shovel", "Progressive Axe"]:
            self.multiworld.push_precollected(self.create_item(name))
        self.multiworld.itempool += build_item_pool(self)

    set_rules = set_rules

    def generate_output(self, output_directory: str) -> None:
        patch = MinecraftDigProcedurePatch(
            player=self.player,
            player_name=self.multiworld.get_player_name(self.player)
        )

        patch.data = self._get_mc_data()

        patch.write(os.path.join(output_directory,
            f"AP_{self.multiworld.seed_name}_P{self.player}_{self.multiworld.get_player_name(self.player)}.apmcdig"))

    def fill_slot_data(self) -> dict:
        slot_data = self._get_mc_data()
        return slot_data

    def get_filler_item_name(self) -> str:
        return get_junk_item_names(self.multiworld.random, 1)[0]


class MinecraftDigLocation(Location):
    game = GAME_NAME


class MinecraftDigItem(Item):
    game = GAME_NAME


def mc_update_output(raw_data, server, port):
    data = json.loads(b64decode(raw_data))
    data['server'] = server
    data['port'] = port
    return b64encode(bytes(json.dumps(data), 'utf-8'))



