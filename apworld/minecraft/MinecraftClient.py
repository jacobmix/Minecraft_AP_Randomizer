import argparse
from base64 import b64encode, b64decode
import zipfile
import json
import os
import sys
import re
import atexit
import shutil
from subprocess import Popen
from shutil import copyfile
from time import strftime
import logging
from typing import Any

import requests
import shlex
import socket
import time
import tempfile
import subprocess

import Utils
from Utils import is_windows
from worlds.LauncherComponents import Component, SuffixIdentifier, Type, components, launch_subprocess
from settings import get_settings

atexit.register(input, "Press enter to exit.")

# 1 or more digits followed by m or g, then optional b
max_heap_re = re.compile(r"^\d+[mMgG][bB]?$")


def try_auto_launch_minecraft():
    """
    Launch Minecraft using the 'mc_launch' host.yaml setting if provided.
    """
    settings = get_settings()
    mc_settings = settings.minecraft_options

    mc_launch = mc_settings.mc_launch
    forge_dir = os.path.expanduser(str(mc_settings.forge_directory))
    max_heap  = mc_settings.max_heap_size

    if not mc_launch:
        return

    # Pass the entire command as a string to Popen with shell=True
    try:
        print(f"Executing: {mc_launch}")
        subprocess.Popen(mc_launch, shell=True)
        print(f"[Minecraft Client] Auto-launched Minecraft: {mc_launch}")
    except Exception as e:
        print(f"[Minecraft Client] Failed to auto-launch Minecraft: {e}")


def wait_for_server_ready(forge_dir: str, timeout: int = 120):
    """
    Wait until the Minecraft server prints the "Done (...)" line indicating it's fully started.
    Only reacts to new log entries after this function is called.
    """
    log_file = os.path.join(forge_dir, "logs", "latest.log")
    start_time = time.time()

    # Wait until the log file exists
    while not os.path.isfile(log_file):
        if time.time() - start_time > timeout:
            raise TimeoutError("Timeout waiting for latest.log to appear")
        time.sleep(0.5)

    print(f"[Minecraft Client] Waiting for server to be ready (reading {log_file})...")

    # Track position in file
    last_size = os.path.getsize(log_file)  # <-- start at the end, ignore old content

    while True:
        try:
            current_size = os.path.getsize(log_file)
            if current_size < last_size:
                # Log was rotated/recreated
                last_size = 0

            with open(log_file, "r", encoding="utf-8") as f:
                f.seek(last_size)
                lines = f.readlines()
                last_size = f.tell()

            for line in lines:
                if "Done (" in line and ")! For help, type \"help\"" in line:
                    print("[Minecraft Client] Server is ready!")
                    return

        except (OSError, IOError):
            pass  # File temporarily locked

        if time.time() - start_time > timeout:
            raise TimeoutError("Timeout waiting for server to be ready")

        time.sleep(0.5)


#def get_minecraft_server_port(forge_dir: str) -> int:
#    """Read server.properties to get server port, defaulting to 25565."""
#    port = 25565  # default
#    properties_file = os.path.join(forge_dir, "server.properties")
#    if os.path.isfile(properties_file):
#        with open(properties_file, "r") as f:
#            for line in f:
#                line = line.strip()
#                if line.startswith("server-port"):
#                    try:
#                        port = int(line.split("=", 1)[1])
#                    except ValueError:
#                        pass
#                    break
#    return port


def prompt_yes_no(prompt):
    yes_inputs = {'yes', 'ye', 'y'}
    no_inputs = {'no', 'n'}
    while True:
        choice = input(prompt + " [y/n] ").lower()
        if choice in yes_inputs:
            return True
        elif choice in no_inputs:
            return False
        else:
            print('Please respond with "y" or "n".')


def find_ap_randomizer_jar(forge_dir):
    """Create mods folder if needed; find AP randomizer jar; return None if not found."""
    mods_dir = os.path.join(forge_dir, 'mods')
    if os.path.isdir(mods_dir):
        for entry in os.scandir(mods_dir):
            if entry.name.startswith("aprandomizer") and entry.name.endswith(".jar"):
                logging.info(f"Found AP randomizer mod: {entry.name}")
                return entry.name
        return None
    else:
        os.mkdir(mods_dir)
        logging.info(f"Created mods folder in {forge_dir}")
        return None


def convert_apmc_to_base64(input_path: str, output_path: str) -> None:
    """
    Converts an APMC file into a base64-encoded JSON text file.
    Supports BOTH:
    - New-format ZIP-based .apmc (with data.json)
    - Old-format base64 JSON .apmc (already encoded)
    """

    # Case 1: NEW FORMAT (ZIP PROCEDURE PATCH)
    if zipfile.is_zipfile(input_path):
        with zipfile.ZipFile(input_path, 'r') as zf:
            if "data.json" not in zf.namelist():
                raise ValueError("ZIP .apmc missing data.json!")

            raw_json = zf.read("data.json").decode("utf-8")
            encoded = b64encode(raw_json.encode("utf-8")).decode("utf-8")

        with open(output_path, 'w', encoding='utf-8') as f:
            f.write(encoded)

        print(f"[APMC] Converted ZIP {input_path} → base64 JSON {output_path}")
        return

    # Case 2: OLD FORMAT (BASE64 JSON)
    else:
        with open(input_path, "r", encoding="utf-8") as f:
            content = f.read().strip()

        # Validate it's actually base64
        try:
            decoded = b64decode(content).decode('utf-8')
            json.loads(decoded)  # ensure it is valid JSON
        except Exception as e:
            raise ValueError(f"Invalid old-format .apmc file: {input_path}") from e

        # Just copy it — it’s already base64
        with open(output_path, "w", encoding="utf-8") as f:
            f.write(content)

        print(f"[APMC] Passed through old-format base64 file: {input_path} → {output_path}")
        return


def replace_apmc_files(forge_dir: str, zip_apmc_path: str) -> None:
    """
    Takes the AP-generated ZIP-style .apmc file and converts it into
    a Forge-compatible base64 .apmc file inside the server directory.
    """

    # Where Forge expects the final base64 file
    target_apdata = os.path.join(forge_dir, "APData")
    os.makedirs(target_apdata, exist_ok=True)

    # Remove any existing .apmc files (keep folder clean)
    for entry in os.scandir(target_apdata):
        if entry.name.endswith(".apmc"):
            os.remove(entry.path)
            print(f"Removed old patch: {entry.name}")

    # Forge expects the same name but base64 contents
    file_name = os.path.basename(zip_apmc_path)
    base64_apmc_path = os.path.join(target_apdata, file_name)

    # Convert ZIP → base64 JSON text
    convert_apmc_to_base64(zip_apmc_path, base64_apmc_path)

    print(f"Converted {zip_apmc_path} → Forge base64 {base64_apmc_path}")


def read_apmc_file(apmc_path: str):
    """
    Reads either:
    - NEW FORMAT: ZIP containing data.json
    - OLD FORMAT: base64 JSON text
    """
    if not os.path.isfile(apmc_path):
        raise FileNotFoundError(f"APMC file not found: {apmc_path}")

    # NEW format: ZIP procedure patch
    if zipfile.is_zipfile(apmc_path):
        with zipfile.ZipFile(apmc_path, 'r') as zf:
            if "data.json" not in zf.namelist():
                raise ValueError("APMC ZIP missing data.json")
            raw = zf.read("data.json").decode("utf-8")
            return json.loads(raw)

    # OLD format: base64 JSON text
    with open(apmc_path, "r", encoding="utf-8") as f:
        content = f.read().strip()

    try:
        decoded = b64decode(content).decode("utf-8")
        return json.loads(decoded)
    except Exception:
        raise ValueError("APMC file is neither ZIP nor valid base64 JSON")


def update_mod(forge_dir, url: str):
    """Check mod version, download new mod from GitHub releases page if needed. """
    ap_randomizer = find_ap_randomizer_jar(forge_dir)
    os.path.basename(url)
    if ap_randomizer is not None:
        logging.info(f"Your current mod is {ap_randomizer}.")
    else:
        logging.info(f"You do not have the AP randomizer mod installed.")

    if ap_randomizer != os.path.basename(url):
        logging.info(f"A new release of the Minecraft AP randomizer mod was found: "
                     f"{os.path.basename(url)}")
        if prompt_yes_no("Would you like to update?"):
            old_ap_mod = os.path.join(forge_dir, 'mods', ap_randomizer) if ap_randomizer is not None else None
            new_ap_mod = os.path.join(forge_dir, 'mods', os.path.basename(url))
            logging.info("Downloading AP randomizer mod. This may take a moment...")
            apmod_resp = requests.get(url)
            if apmod_resp.status_code == 200:
                with open(new_ap_mod, 'wb') as f:
                    f.write(apmod_resp.content)
                    logging.info(f"Wrote new mod file to {new_ap_mod}")
                if old_ap_mod is not None:
                    os.remove(old_ap_mod)
                    logging.info(f"Removed old mod file from {old_ap_mod}")
            else:
                logging.error(f"Error retrieving the randomizer mod (status code {apmod_resp.status_code}).")
                logging.error(f"Please report this issue on the Archipelago Discord server.")
                sys.exit(1)


def check_eula(forge_dir):
    """Check if the EULA is agreed to, and prompt the user to read and agree if necessary."""
    eula_path = os.path.join(forge_dir, "eula.txt")
    if not os.path.isfile(eula_path):
        # Create eula.txt
        with open(eula_path, 'w') as f:
            f.write("#By changing the setting below to TRUE you are indicating your agreement to our EULA (https://account.mojang.com/documents/minecraft_eula).\n")
            f.write(f"#{strftime('%a %b %d %X %Z %Y')}\n")
            f.write("eula=false\n")
    with open(eula_path, 'r+') as f:
        text = f.read()
        if 'false' in text:
            # Prompt user to agree to the EULA
            logging.info("You need to agree to the Minecraft EULA in order to run the server.")
            logging.info("The EULA can be found at https://account.mojang.com/documents/minecraft_eula")
            if prompt_yes_no("Do you agree to the EULA?"):
                f.seek(0)
                f.write(text.replace('false', 'true'))
                f.truncate()
                logging.info(f"Set {eula_path} to true")
            else:
                sys.exit(0)


def find_jdk_dir(version: str) -> str | None:
    """get the specified versions jdk directory"""
    for entry in os.listdir():
        if os.path.isdir(entry) and entry.startswith(f"jdk{version}"):
            return os.path.abspath(entry)


def find_jdk(version: str) -> str:
    """get the java exe location"""
    if is_windows:
        jdk = find_jdk_dir(version)
        if jdk:
            jdk_exe = os.path.join(jdk, "bin", "java.exe")
            if os.path.isfile(jdk_exe):
                return jdk_exe
        return "java"  # fallback
    else:
        settings = get_settings()
        java_cmd = settings.minecraft_options.java or "java"
        jdk_exe = shutil.which(java_cmd)
        if not jdk_exe:
            raise Exception("Could not find Java. Is Java installed on the system?")
        return jdk_exe


def download_java(java: str):
    """Download Corretto (Amazon JDK)"""

    jdk = find_jdk_dir(java)
    if jdk is not None:
        print(f"Removing old JDK...")
        from shutil import rmtree
        rmtree(jdk)

    print(f"Downloading Java...")
    jdk_url = f"https://corretto.aws/downloads/latest/amazon-corretto-{java}-x64-windows-jdk.zip"
    resp = requests.get(jdk_url)
    if resp.status_code == 200:  # OK
        print(f"Extracting...")
        import zipfile
        from io import BytesIO
        with zipfile.ZipFile(BytesIO(resp.content)) as zf:
            zf.extractall()
    else:
        print(f"Error downloading Java (status code {resp.status_code}).")
        print(f"If this was not expected, please report this issue on the Archipelago Discord server.")
        if not prompt_yes_no("Continue anyways?"):
            sys.exit(0)


def install_forge(directory, forge_version, java_version):
    """download and install forge"""

    java_exe = find_jdk(java_version)
    if java_exe is not None:
        print(f"Downloading Forge {forge_version}...")
        forge_url = f"https://maven.minecraftforge.net/net/minecraftforge/forge/{forge_version}/forge-{forge_version}-installer.jar"
        resp = requests.get(forge_url)
        if resp.status_code == 200:  # OK
            forge_install_jar = os.path.join(directory, "forge_install.jar")
            if not os.path.exists(directory):
                os.mkdir(directory)
            with open(forge_install_jar, 'wb') as f:
                f.write(resp.content)
            print(f"Installing Forge...")
            install_process = Popen([java_exe, "-jar", forge_install_jar, "--installServer", directory])
            install_process.wait()
            os.remove(forge_install_jar)


def run_forge_server(forge_dir: str, java_version: str, heap_arg: str, forge_version) -> Popen:
    """Run the Forge server."""

    java_exe = find_jdk(java_version)
    if not os.path.isfile(java_exe):
        java_exe = "java"  # try to fall back on java in the PATH

    heap_arg = max_heap_re.match(heap_arg).group()
    if heap_arg[-1] in ['b', 'B']:
        heap_arg = heap_arg[:-1]
    heap_arg = "-Xmx" + heap_arg

    os_args = "win_args.txt" if is_windows else "unix_args.txt"
    args_file = os.path.join(forge_dir, "libraries", "net", "minecraftforge", "forge", forge_version, os_args)
    forge_args = []
    with open(args_file) as argfile:
        for line in argfile:
            forge_args.extend(line.strip().split(" "))

    args = [java_exe, heap_arg, *forge_args, "-nogui"]
    logging.info(f"Running Forge server: {args}")
    os.chdir(forge_dir)
    return Popen(args)


def get_minecraft_versions(version, release_channel="release"):
    version_file_endpoint = "https://raw.githubusercontent.com/cjmang/Minecraft_AP_Randomizer/refs/heads/master/versions/minecraft_versions.json"
    resp = requests.get(version_file_endpoint)
    local = False
    if resp.status_code == 200:  # OK
        try:
            data = resp.json()
        except requests.exceptions.JSONDecodeError:
            logging.warning(f"Unable to fetch version update file, using local version. (status code {resp.status_code}).")
            local = True
    else:
        logging.warning(f"Unable to fetch version update file, using local version. (status code {resp.status_code}).")
        local = True

    if local:
        with open(Utils.user_path("minecraft_versions.json"), 'r') as f:
            data = json.load(f)
    else:
        with open(Utils.user_path("minecraft_versions.json"), 'w') as f:
            json.dump(data, f)

    try:
        if version:
            return next(filter(lambda entry: entry["version"] == version, data[release_channel]))
        else:
            return resp.json()[release_channel][0]
    except (StopIteration, KeyError):
        logging.error(f"No compatible mod version found for client version {version} on \"{release_channel}\" channel.")
        if release_channel != "release":
            logging.error("Consider switching \"release_channel\" to \"release\" in your Host.yaml file")
        else:
            logging.error("No suitable mod found on the \"release\" channel. Please Contact us on discord to report this error.")
        sys.exit(0)


def is_correct_forge(forge_dir, forge_version) -> bool:
    if os.path.isdir(os.path.join(forge_dir, "libraries", "net", "minecraftforge", "forge", forge_version)):
        return True
    return False

def add_to_launcher_components():
    component = Component(
        "Minecraft Client",
        func=run_client,
        component_type=Type.CLIENT,
        file_identifier=SuffixIdentifier(".apmc"),
        cli=True
    )
    components.append(component)


def run_client(*args):
    Utils.init_logging("MinecraftClient")
    parser = argparse.ArgumentParser()
    parser.add_argument("apmc_file", default=None, nargs='?', help="Path to an Archipelago Minecraft data file (.apmc)")
    parser.add_argument('--install', '-i', dest='install', default=False, action='store_true',
                        help="Download and install Java and the Forge server. Does not launch the client afterwards.")
    parser.add_argument('--release_channel', '-r', dest="channel", type=str, action='store',
                        help="Specify release channel to use.")
    parser.add_argument('--java', '-j', metavar='17', dest='java', type=str, default=False, action='store',
                        help="specify java version.")
    parser.add_argument('--forge', '-f', metavar='1.18.2-40.1.0', dest='forge', type=str, default=False, action='store',
                        help="specify forge version. (Minecraft Version-Forge Version)")
    parser.add_argument('--version', '-v', metavar='9', dest='data_version', type=int, action='store',
                        help="specify Mod data version to download.")

    args = parser.parse_args(args)
    apmc_file = os.path.abspath(args.apmc_file) if args.apmc_file else None

    # Change to executable's working directory
    os.chdir(os.path.abspath(os.path.dirname(sys.argv[0])))

    settings = get_settings()
    mc_settings = settings.minecraft_options

    mc_launch = mc_settings.mc_launch
    forge_dir = os.path.expanduser(str(mc_settings.forge_directory))
    max_heap  = mc_settings.max_heap_size

    channel = args.channel or mc_settings.release_channel

    apmc_data = None
    data_version = args.data_version or None

    if apmc_file is None and not args.install:
        apmc_file = Utils.open_filename('Select APMC file', (('APMC File', ('.apmc',)),))

    if apmc_file is not None and data_version is None:
        apmc_data = read_apmc_file(apmc_file)
        data_version = apmc_data.get('client_version', '')

    versions = get_minecraft_versions(data_version, channel)

    forge_version = args.forge or versions["forge"]
    java_version  = args.java or versions["java"]
    mod_url       = versions["url"]
    java_dir      = find_jdk_dir(java_version)

    if args.install:
        if is_windows:
            print("Installing Java")
            download_java(java_version)
        if not is_correct_forge(forge_dir, forge_version):
            print("Installing Minecraft Forge")
            install_forge(forge_dir, forge_version, java_version)
        else:
            print("Correct Forge version already found, skipping install.")
        sys.exit(0)

    if apmc_data is None:
        raise FileNotFoundError(f"APMC file does not exist or is inaccessible at the given location ({apmc_file})")

    if is_windows:
        if java_dir is None or not os.path.isdir(java_dir):
            if prompt_yes_no("Did not find java directory. Download and install java now?"):
                download_java(java_version)
                java_dir = find_jdk_dir(java_version)
            if java_dir is None or not os.path.isdir(java_dir):
                raise NotADirectoryError(f"Path {java_dir} does not exist or could not be accessed.")

    if not is_correct_forge(forge_dir, forge_version):
        if prompt_yes_no(f"Did not find forge version {forge_version} download and install it now?"):
            install_forge(forge_dir, forge_version, java_version)
        if not os.path.isdir(forge_dir):
            raise NotADirectoryError(f"Path {forge_dir} does not exist or could not be accessed.")

    if not max_heap_re.match(max_heap):
        raise Exception(f"Max heap size {max_heap} in incorrect format. Use a number followed by M or G, e.g. 512M or 2G.")

    update_mod(forge_dir, mod_url)
    replace_apmc_files(forge_dir, apmc_file)
    check_eula(forge_dir)
    timeout = 90
    server_process = run_forge_server(forge_dir, java_version, max_heap, forge_version)

    # Wait for server to finish starting
    wait_for_server_ready(forge_dir)

#    server_port = get_minecraft_server_port(forge_dir)
#    server_host = "127.0.0.1"
#    timeout = 90
#    start_time = time.time()
#
#    while True:
#        try:
#            with socket.create_connection((server_host, server_port), timeout=1):
#                break
#        except (ConnectionRefusedError, OSError):
#            if time.time() - start_time > timeout:
#                print("[Minecraft Client] Timeout waiting for server to start")
#                break
#            time.sleep(1)
#
#    time.sleep(5)

    # Auto-launch Minecraft
    try_auto_launch_minecraft()

    # Wait for server process to exit
    server_process.wait()
