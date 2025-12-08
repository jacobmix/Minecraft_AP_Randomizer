from worlds.Files import APProcedurePatch, APPatchExtension
import hashlib
import json
import os

class MinecraftProcedurePatch(APProcedurePatch):
    """
    A patch container for Minecraft world data to upload to Archipelago rooms.
    Compatible with AP 0.6.4.

    This patch stores world data in 'data.json' and defines a procedure
    for applying it. Currently, server info is not included, since the
    patch is generated locally and uploaded to a room.
    """
    game = "Minecraft"
    patch_file_ending = ".apmc"

    # Procedure steps: currently only apply our Minecraft data JSON
    procedure = [("apply_minecraft_data", ["data.json"])]

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        patch_data = self._get_mc_data()
        self.hash = hashlib.sha1(json.dumps(patch_data).encode()).hexdigest()

    def write_contents(self, opened_zipfile):
        """
        Writes patch contents into the container (zip).
        'data.json' is the main payload used by the client mod.
        """
        # Store data.json in the patch files
        data_bytes = json.dumps(self._get_mc_data(), ensure_ascii=False).encode("utf-8")
        self.write_file("data.json", data_bytes)
        # Call superclass to include manifest and procedure
        super().write_contents(opened_zipfile)

    def _get_mc_data(self):
        if hasattr(self, "data") and self.data is not None:
            return self.data
        return {}


class MinecraftPatchExtension(APPatchExtension):
    """
    Defines procedure steps for Minecraft patch.
    """
    game = "Minecraft"

    @staticmethod
    def apply_minecraft_data(caller, rom: bytes, file_name: str):
        """
        Procedure step to extract Minecraft world data.
        'rom' is ignored here; we just return the stored file bytes.
        """
        return caller.get_file(file_name)