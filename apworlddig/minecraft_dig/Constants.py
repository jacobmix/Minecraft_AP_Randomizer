import os
import json
import pkgutil


def load_data_file(*args) -> dict:
    fname = os.path.join("data", *args)
    return json.loads(pkgutil.get_data(__name__, fname).decode())


id_offset: int = 50000
MAX_CHUNKS: int = 25
LAYERS_PER_CHUNK: int = 192  # Y from -63 to 128

item_info = load_data_file("items.json")
item_name_to_id = {name: id_offset + index
                   for index, name in enumerate(item_info["all_items"])}

# Generate all possible location names (up to MAX_CHUNKS chunks, 192 layers each).
# Location ID = id_offset + chunk_index * 192 + (y + 63)
all_location_names = []
for c in range(MAX_CHUNKS):
    for y in range(-63, 129):
        all_location_names.append(f"Chunk {c} Layer {y}")

location_name_to_id = {name: id_offset + index
                       for index, name in enumerate(all_location_names)}

region_info = load_data_file("regions.json")
