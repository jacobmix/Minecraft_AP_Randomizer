from Options import Range, PerGameCommonOptions
from dataclasses import dataclass


class ChunkCount(Range):
    """Number of chunks to generate for digging. Default is 1 (single chunk).
    Higher values spawn additional chunks around the origin, giving more area to dig."""
    display_name = "Chunk Count"
    range_start = 1
    range_end = 25
    default = 1


@dataclass
class MinecraftDigOptions(PerGameCommonOptions):
    chunk_count: ChunkCount
