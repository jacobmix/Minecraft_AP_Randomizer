from Options import Range, Toggle, PerGameCommonOptions
from dataclasses import dataclass


class ChunkCount(Range):
    """Number of chunks to generate for digging. Default is 1 (single chunk).
    Higher values spawn additional chunks around the origin, giving more area to dig."""
    display_name = "Chunk Count"
    range_start = 1
    range_end = 25
    default = 1


class ProgressiveChunks(Toggle):
    """If enabled, you start with only 1 chunk accessible.
    World Barrier Expansion items will be added to the pool to unlock additional chunks.
    Requires Chunk Count > 1 to have any effect."""
    display_name = "Progressive Chunks"
    default = 1


# Trap weight options - set to 0 to disable a trap
class BeeTrapWeight(Range):
    """Weight for Bee Trap in the item pool. Set to 0 to disable."""
    display_name = "Bee Trap Weight"
    range_start = 0
    range_end = 50
    default = 5


class CreeperTrapWeight(Range):
    """Weight for Creeper Trap in the item pool. Set to 0 to disable."""
    display_name = "Creeper Trap Weight"
    range_start = 0
    range_end = 50
    default = 5


class SandRainWeight(Range):
    """Weight for Sand Rain trap in the item pool. Set to 0 to disable."""
    display_name = "Sand Rain Weight"
    range_start = 0
    range_end = 50
    default = 5


class SpawnWitherWeight(Range):
    """Weight for Spawn Wither trap in the item pool. Set to 0 to disable."""
    display_name = "Spawn Wither Weight"
    range_start = 0
    range_end = 50
    default = 1


class GoonSquadWeight(Range):
    """Weight for Goon Squad trap in the item pool. Set to 0 to disable."""
    display_name = "Goon Squad Weight"
    range_start = 0
    range_end = 50
    default = 5


class FishFountainWeight(Range):
    """Weight for Fish Fountain trap in the item pool. Set to 0 to disable."""
    display_name = "Fish Fountain Weight"
    range_start = 0
    range_end = 50
    default = 2


class BadAirWeight(Range):
    """Weight for Bad Air trap in the item pool. Set to 0 to disable."""
    display_name = "Bad Air Weight"
    range_start = 0
    range_end = 50
    default = 2


class PocketSandWeight(Range):
    """Weight for Pocket Sand trap in the item pool. Set to 0 to disable."""
    display_name = "Pocket Sand Weight"
    range_start = 0
    range_end = 50
    default = 2


class PeskyBirdWeight(Range):
    """Weight for Pesky Bird trap in the item pool. Set to 0 to disable."""
    display_name = "Pesky Bird Weight"
    range_start = 0
    range_end = 50
    default = 2


class FlashFloodWeight(Range):
    """Weight for Flash Flood trap in the item pool. Set to 0 to disable."""
    display_name = "Flash Flood Weight"
    range_start = 0
    range_end = 50
    default = 3


class PetTheKittyWeight(Range):
    """Weight for Pet the Kitty trap in the item pool. Set to 0 to disable."""
    display_name = "Pet the Kitty Weight"
    range_start = 0
    range_end = 50
    default = 3


class WingardiumLeviosaWeight(Range):
    """Weight for Wingardium Leviosa trap in the item pool. Set to 0 to disable."""
    display_name = "Wingardium Leviosa Weight"
    range_start = 0
    range_end = 50
    default = 3


class AboutFaceWeight(Range):
    """Weight for About Face trap in the item pool. Set to 0 to disable."""
    display_name = "About Face Weight"
    range_start = 0
    range_end = 50
    default = 3


class AcmeDeliveryWeight(Range):
    """Weight for Acme Delivery trap in the item pool. Set to 0 to disable."""
    display_name = "Acme Delivery Weight"
    range_start = 0
    range_end = 50
    default = 3


class MeteorShowerWeight(Range):
    """Weight for Meteor Shower trap in the item pool. Set to 0 to disable."""
    display_name = "Meteor Shower Weight"
    range_start = 0
    range_end = 50
    default = 3



class IncreasedGravityWeight(Range):
    """Weight for Increased Gravity trap in the item pool. Set to 0 to disable."""
    display_name = "Increased Gravity Weight"
    range_start = 0
    range_end = 50
    default = 3


@dataclass
class MinecraftDigOptions(PerGameCommonOptions):
    chunk_count: ChunkCount
    progressive_chunks: ProgressiveChunks
    # Trap weights
    bee_trap_weight: BeeTrapWeight
    creeper_trap_weight: CreeperTrapWeight
    sand_rain_weight: SandRainWeight
    spawn_wither_weight: SpawnWitherWeight
    goon_squad_weight: GoonSquadWeight
    fish_fountain_weight: FishFountainWeight
    bad_air_weight: BadAirWeight
    pocket_sand_weight: PocketSandWeight
    pesky_bird_weight: PeskyBirdWeight
    flash_flood_weight: FlashFloodWeight
    pet_the_kitty_weight: PetTheKittyWeight
    wingardium_leviosa_weight: WingardiumLeviosaWeight
    about_face_weight: AboutFaceWeight
    acme_delivery_weight: AcmeDeliveryWeight
    meteor_shower_weight: MeteorShowerWeight

    increased_gravity_weight: IncreasedGravityWeight
