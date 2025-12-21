from worlds.Files import APProcedurePatch, APPatchExtension
import hashlib
import json

GAME_NAME = "Minecraft Dig"


class MinecraftDigProcedurePatch(APProcedurePatch):
    """
    Patch container for Minecraft Dig world data.
    Compatible with Archipelago 0.6.4+
    """
    game = GAME_NAME
    patch_file_ending = ".apmcdig"

    # Single procedure: expose data.json
    procedure = [("apply_minecraft_data", ["data.json"])]

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.data = getattr(self, "data", {})  # ensure self.data exists
        self.hash = ""  # initialize hash

    def write_contents(self, opened_zipfile):
        """
        Write the world patch to the zip file and calculate the hash.
        """
        data_bytes = json.dumps(self.data, ensure_ascii=False).encode("utf-8")
        self.write_file("data.json", data_bytes)

        # Call parent to handle any extra processing
        super().write_contents(opened_zipfile)

        # Compute hash after writing contents
        self.hash = hashlib.sha1(data_bytes).hexdigest()


class MinecraftDigPatchExtension(APPatchExtension):
    game = GAME_NAME

    @staticmethod
    def apply_minecraft_data(caller, rom: bytes, file_name: str):
        return caller.get_file(file_name)
