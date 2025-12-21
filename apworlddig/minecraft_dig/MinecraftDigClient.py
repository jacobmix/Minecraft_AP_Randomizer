import os
import sys
import glob
import json
import zipfile
import logging
import shutil
import subprocess
import time
import re
import atexit
from base64 import b64encode, b64decode
from subprocess import Popen
from time import strftime
from typing import Any

import requests
import shlex

import Utils
from Utils import is_windows
from worlds.LauncherComponents import Component, SuffixIdentifier, Type, components, launch_subprocess

atexit.register(input, "Press enter to exit.")

DEFAULT_DIG_MOD_URL = ("https://github.com/AshIndigo/Minecraft_AP_Randomizer/releases/download/dig-v0.0.2-hotfix/aprandomizer-MC1.19.4-hotfix-0.0.2.jar")

# regex for max heap sizes
max_heap_re = re.compile(r"^\d+[mMgG][bB]?$")


def launch_dig_subprocess(_url=None):
    launch_subprocess(run_client, name="MinecraftDigClient")

def add_to_launcher_components():
    component = Component(
        "Minecraft Dig Client",
        func=launch_dig_subprocess,
        component_type=Type.CLIENT,
        file_identifier=SuffixIdentifier(".apmcdig"),
        cli=True
    )
    components.append(component)


# ------------------------ Auto-launch ------------------------
def try_auto_launch_minecraft_dig():
    """
    Launch Minecraft using the 'mc_launch' host.yaml setting for Minecraft Dig.
    """
    from worlds.minecraft_dig import MinecraftDigWorld
    mc_launch = MinecraftDigWorld.settings.mc_launch.strip()

    if not mc_launch:
        return

    try:
        print(f"[Minecraft Dig] Executing: {mc_launch}")
        # Pass the string directly, let Windows CMD handle quotes
        subprocess.Popen(mc_launch, shell=True)
        print(f"[Minecraft Dig] Auto-launched Minecraft: {mc_launch}")
    except Exception as e:
        print(f"[Minecraft Dig] Failed to auto-launch Minecraft: {e}")


# ------------------------ CLI Launcher ------------------------
def run_client(*args):
    """
    Run Minecraft Dig client from command line.
    Supports:
      - .apmcdig file input
      - optional installation of Java / Forge
    """
    import argparse
    from worlds.minecraft_dig import MinecraftDigWorld
    Utils.init_logging("MinecraftDig")
    parser = argparse.ArgumentParser()
    parser.add_argument("apmcdig_file", default=None, nargs='?',
                        help="Path to a Minecraft Dig data file (.apmcdig)")
    parser.add_argument('--install', '-i', action='store_true', help="Install Java/Forge, do not launch.")
    parser.add_argument('--java', '-j', type=str, help="Java version to use.")
    parser.add_argument('--forge', '-f', type=str, help="Forge version to use.")
    args = parser.parse_args(args)

    apmcdig_file = os.path.abspath(args.apmcdig_file) if args.apmcdig_file else None

    # Change to executable's working directory
    os.chdir(os.path.abspath(os.path.dirname(sys.argv[0])))

    if apmcdig_file is None and not args.install:
        apmcdig_file = Utils.open_filename("Select Minecraft Dig file", (("APMCDIG File", (".apmcdig",)),))

    settings = MinecraftDigWorld.settings

    java_version = args.java or settings.java_version
    max_heap = settings.max_heap_size
    forge_dir = os.path.expanduser(str(settings.forge_directory))
    forge_url = settings.forge_url
    mod_url = settings.dig_mod_url or DEFAULT_DIG_MOD_URL

    # Install Java if needed
    if is_windows and not find_jdk_dir(java_version):
        if prompt_yes_no("Java not found. Download now?"):
            download_java(java_version)

    # Install Forge if needed
    if not os.path.isdir(forge_dir):
        if prompt_yes_no("Forge not found. Install now?"):
            install_forge_from_url(forge_dir, forge_url, java_version)
        else:
            return

    # Auto-install mod/update
    update_mod(forge_dir, mod_url)

    if args.install:
        print("[Minecraft Dig] Installation complete. Exiting.")
        return

    # Convert and replace .apmcdig
    replace_apmcdig_files(forge_dir, apmcdig_file)

    # EULA check
    check_eula(forge_dir)

    # Run server
    server = run_forge_server(forge_dir, java_version, max_heap, None)
    wait_for_server_ready(forge_dir)

    # Auto-launch Minecraft Dig client
    try_auto_launch_minecraft_dig()

    # Wait for server to finish
    server.wait()


# ------------------------ APMC Conversion ------------------------
def convert_apmcdig_to_apmc(input_path: str, output_path: str):
    """Converts a ZIP-based .apmcdig into a Forge-compatible base64 .apmc."""
    if not zipfile.is_zipfile(input_path):
        raise ValueError("Expected ZIP-based .apmcdig")

    with zipfile.ZipFile(input_path, "r") as zf:
        raw = zf.read("data.json").decode("utf-8")
        encoded = b64encode(raw.encode("utf-8")).decode("utf-8")

    with open(output_path, "w", encoding="utf-8") as f:
        f.write(encoded)


def replace_apmcdig_files(forge_dir: str, apmcdig_path: str):
    """Replace existing .apmc files in server directory with the new Dig patch."""
    target = os.path.join(forge_dir, "APData")
    os.makedirs(target, exist_ok=True)

    for entry in os.scandir(target):
        if entry.name.endswith(".apmc"):
            os.remove(entry.path)

    out_path = os.path.join(
        target,
        os.path.basename(apmcdig_path).replace(".apmcdig", ".apmc")
    )
    convert_apmcdig_to_apmc(apmcdig_path, out_path)


# ------------------------ Find Forge Mods dir ------------------------
def get_forge_mods_dir(forge_dir: str) -> str:
    """Return the mods directory where Forge expects mods (next to run.bat/run.sh)."""
    mods_dir = os.path.join(forge_dir, "mods")
    os.makedirs(mods_dir, exist_ok=True)
    return mods_dir


# ------------------------ Find Forge Logs ------------------------
def find_forge_logs(forge_dir: str) -> str:
    """
    Locate the latest.log file for Forge.
    """
    # Root-style location
    old_log = os.path.join(forge_dir, "logs", "latest.log")
    if os.path.isfile(old_log):
        return old_log

    # libraries-style location.
    matches = glob.glob(os.path.join(forge_dir, "**", "logs", "latest.log"), recursive=True)
    if matches:
        return matches[0]

    return None


# ------------------------ Forge Installation ------------------------
def install_forge_from_url(directory: str, forge_url: str, java_version: str):
    """Download and install Forge server from URL."""
    java_exe = find_jdk(java_version)
    os.makedirs(directory, exist_ok=True)

    installer = os.path.join(directory, "forge_installer.jar")
    resp = requests.get(forge_url)
    resp.raise_for_status()
    with open(installer, "wb") as f:
        f.write(resp.content)

    Popen([java_exe, "-jar", installer, "--installServer", directory]).wait()
    os.remove(installer)


# ------------------------ Minecraft Mod & Server ------------------------
def find_ap_randomizer_jar(mods_dir: str):
    os.makedirs(mods_dir, exist_ok=True)
    for entry in os.scandir(mods_dir):
        if entry.name.startswith("aprandomizer") and entry.name.endswith(".jar"):
            return entry.name
    return None


def update_mod(forge_dir, url: str):
    mods_dir = get_forge_mods_dir(forge_dir)
    ap_randomizer = find_ap_randomizer_jar(mods_dir)
    
    target_name = os.path.basename(url)
    # Only download if it's missing or different
    if ap_randomizer != target_name:
        if ap_randomizer is None:
            print(f"[Minecraft Dig] Installing mod {target_name}...")
        else:
            print(f"[Minecraft Dig] Updating mod {ap_randomizer} -> {target_name}...")
        resp = requests.get(url)
        resp.raise_for_status()
        new_ap_mod = os.path.join(mods_dir, target_name)
        with open(new_ap_mod, "wb") as f:
            f.write(resp.content)
        if ap_randomizer:
            old_ap_mod = os.path.join(mods_dir, ap_randomizer)
            os.remove(old_ap_mod)



def check_eula(forge_dir):
    eula_path = os.path.join(forge_dir, "eula.txt")
    if not os.path.isfile(eula_path):
        with open(eula_path, "w") as f:
            f.write(f"# Minecraft EULA\n# {strftime('%c')}\n")
            f.write("eula=false\n")
    with open(eula_path, "r+") as f:
        content = f.read()
        if "false" in content:
            logging.info("You must accept the Minecraft EULA.")
            if prompt_yes_no("Do you agree?"):
                f.seek(0)
                f.write(content.replace("false", "true"))
                f.truncate()
            else:
                sys.exit(0)


def run_forge_server(forge_dir: str, java_version: str, heap_arg: str, forge_version=None) -> Popen:
    java_exe = find_jdk(java_version)

    # Normalize heap
    heap_arg = max_heap_re.match(heap_arg).group()
    if heap_arg[-1] in ("b", "B"):
        heap_arg = heap_arg[:-1]
    heap_arg = "-Xmx" + heap_arg

    # Pick OS-specific args file
    args_filename = "win_args.txt" if is_windows else "unix_args.txt"

    forge_lib_base = os.path.join(
        forge_dir,
        "libraries",
        "net",
        "minecraftforge",
        "forge"
    )

    if not os.path.isdir(forge_lib_base):
        raise FileNotFoundError("Forge libraries directory not found.")

    # Prefer explicitly requested forge version if given
    if forge_version:
        forge_dir_name = forge_version
    else:
        versions = sorted(os.listdir(forge_lib_base))
        if not versions:
            raise FileNotFoundError("No Forge versions found.")
        forge_dir_name = versions[-1]  # newest

    args_path = os.path.join(
        forge_lib_base,
        forge_dir_name,
        args_filename
    )

    if not os.path.isfile(args_path):
        raise FileNotFoundError(f"Missing Forge args file: {args_path}")

    forge_args = []
    with open(args_path, "r", encoding="utf-8") as f:
        for line in f:
            forge_args.extend(line.strip().split())

    cmd = [
        java_exe,
        heap_arg,
        *forge_args,
        "nogui"
    ]

    logging.info(f"[Minecraft Dig] Launching Forge:")
    logging.info(" ".join(cmd))

    return Popen(
        cmd,
        cwd=forge_dir
    )


def wait_for_server_ready(forge_dir: str, timeout: int = 120):
    """
    Waits for the Forge server to create and write "Done" to latest.log.
    """
    log_file = os.path.join(forge_dir, "logs", "latest.log")
    
    start_time = time.time()
    
    # Wait until latest.log exists
    while not os.path.isfile(log_file):
        if time.time() - start_time > timeout:
            raise TimeoutError("Timeout waiting for latest.log to be created.")
        time.sleep(0.5)

    # Wait until server prints Done
    last_size = os.path.getsize(log_file)
    while True:
        current_size = os.path.getsize(log_file)
        if current_size < last_size:
            last_size = 0  # log rotated or cleared
        with open(log_file, "r", encoding="utf-8") as f:
            f.seek(last_size)
            lines = f.readlines()
            last_size = f.tell()
        for line in lines:
            if "Done (" in line and ")! For help, type \"help\"" in line:
                logging.info("Server ready!")
                return
        if time.time() - start_time > timeout:
            raise TimeoutError("Timeout waiting for server to finish starting.")
        time.sleep(0.5)


# ------------------------ Java ------------------------
def find_jdk_dir(version: str):
    for entry in os.listdir():
        if os.path.isdir(entry) and entry.startswith(f"jdk{version}"):
            return os.path.abspath(entry)
    return None


def find_jdk(version: str) -> str:
    """Get the Java executable, supporting host.yaml manual path."""
    from worlds.minecraft_dig import MinecraftDigWorld
    settings = MinecraftDigWorld.settings

    # 1. Check if user set a manual java path in host.yaml
    java_path = getattr(settings, "java", None)
    if java_path and os.path.isfile(java_path):
        return java_path

    # 2. Try to find bundled JDK in the Archipelago directory
    jdk_dir = find_jdk_dir(version)
    if jdk_dir:
        jdk_exe = os.path.join(jdk_dir, "bin", "java.exe" if is_windows else "java")
        if os.path.isfile(jdk_exe):
            return jdk_exe

    # 3. Fallback to PATH
    exe = shutil.which("java")
    if exe:
        return exe

    # 4. No Java found
    raise FileNotFoundError(
        f"Could not find Java (version {version}). "
        "Set 'java' in host.yaml or install a JDK."
    )


def download_java(java: str):
    jdk_url = f"https://corretto.aws/downloads/latest/amazon-corretto-{java}-x64-windows-jdk.zip"
    resp = requests.get(jdk_url)
    resp.raise_for_status()
    from io import BytesIO
    with zipfile.ZipFile(BytesIO(resp.content)) as zf:
        zf.extractall()
    print(f"Downloaded Java {java}")


# ------------------------ Utilities ------------------------
def prompt_yes_no(prompt: str) -> bool:
    while True:
        choice = input(prompt + " [y/n] ").lower()
        if choice in {"y", "yes"}:
            return True
        elif choice in {"n", "no"}:
            return False
