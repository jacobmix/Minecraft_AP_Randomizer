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
import subprocess
import time

import Utils
from Utils import is_windows
from worlds.LauncherComponents import Component, SuffixIdentifier, Type, components, launch_subprocess
from settings import get_settings

atexit.register(input, "Press enter to exit.")

# Default Dig settings
DEFAULT_DIG_MOD_URL = "https://github.com/AshIndigo/Minecraft_AP_Randomizer/releases/download/dig-v0.0.2-hotfix/aprandomizer-MC1.19.4-hotfix-0.0.2.jar"
DEFAULT_DIG_FORGE_URL = "https://maven.minecraftforge.net/net/minecraftforge/forge/1.19.4-45.3.15/forge-1.19.4-45.3.15-installer.jar"
DEFAULT_DIG_JAVA_VERSION = "17"

max_heap_re = re.compile(r"^\d+[mMgG][bB]?$")


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


def try_auto_launch_minecraft():
    settings = get_settings()
    mc_settings = settings.minecraft_dig_options
    mc_launch = mc_settings.mc_launch
    if mc_launch:
        try:
            print(f"Executing: {mc_launch}")
            subprocess.Popen(mc_launch, shell=True)
            print(f"[Minecraft Dig] Auto-launched Minecraft: {mc_launch}")
        except Exception as e:
            print(f"[Minecraft Dig] Failed to auto-launch Minecraft: {e}")


def wait_for_server_ready(forge_dir: str, timeout: int = 120):
    log_file = os.path.join(forge_dir, "logs", "latest.log")
    start_time = time.time()
    while not os.path.isfile(log_file):
        if time.time() - start_time > timeout:
            raise TimeoutError("Timeout waiting for latest.log")
        time.sleep(0.5)
    last_size = os.path.getsize(log_file)
    while True:
        current_size = os.path.getsize(log_file)
        if current_size < last_size:
            last_size = 0
        with open(log_file, "r", encoding="utf-8") as f:
            f.seek(last_size)
            lines = f.readlines()
            last_size = f.tell()
        for line in lines:
            if "Done (" in line and ")! For help, type \"help\"" in line:
                print("[Minecraft Dig] Server is ready!")
                return
        if time.time() - start_time > timeout:
            raise TimeoutError("Timeout waiting for server ready")
        time.sleep(0.5)


def find_ap_randomizer_jar(forge_dir):
    mods_dir = os.path.join(forge_dir, 'mods')
    if not os.path.isdir(mods_dir):
        os.mkdir(mods_dir)
    for entry in os.scandir(mods_dir):
        if entry.name.startswith("aprandomizer") and entry.name.endswith(".jar"):
            logging.info(f"Found Dig mod: {entry.name}")
            return entry.name
    return None


def convert_apmcdig_to_base64(input_path: str, output_path: str) -> None:
    if zipfile.is_zipfile(input_path):
        with zipfile.ZipFile(input_path, 'r') as zf:
            if "data.json" not in zf.namelist():
                raise ValueError("ZIP .apmcdig missing data.json!")
            raw_json = zf.read("data.json").decode("utf-8")
            encoded = b64encode(raw_json.encode("utf-8")).decode("utf-8")
        with open(output_path, 'w', encoding='utf-8') as f:
            f.write(encoded)
        print(f"[Dig] Converted ZIP {input_path} → base64 JSON {output_path}")
        return
    else:
        with open(input_path, "r", encoding="utf-8") as f:
            content = f.read().strip()
        try:
            decoded = b64decode(content).decode("utf-8")
            json.loads(decoded)
        except Exception as e:
            raise ValueError(f"Invalid old-format .apmcdig file: {input_path}") from e
        with open(output_path, "w", encoding="utf-8") as f:
            f.write(content)
        print(f"[Dig] Passed through old-format base64 file: {input_path} → {output_path}")


def replace_apmcdig_files(forge_dir: str, zip_apmcdig_path: str) -> None:
    target_apdata = os.path.join(forge_dir, "APData")
    os.makedirs(target_apdata, exist_ok=True)
    for entry in os.scandir(target_apdata):
        if entry.name.endswith(".apmc"):
            os.remove(entry.path)
            print(f"Removed old patch: {entry.name}")
    base_name = os.path.splitext(os.path.basename(zip_apmcdig_path))[0] + ".apmc"
    base64_apmc_path = os.path.join(target_apdata, base_name)
    convert_apmcdig_to_base64(zip_apmcdig_path, base64_apmc_path)
    print(f"Converted {zip_apmcdig_path} → Forge base64 {base64_apmc_path}")


def update_mod(forge_dir, url: str):
    ap_randomizer = find_ap_randomizer_jar(forge_dir)
    if ap_randomizer != os.path.basename(url):
        logging.info(f"A new release of Dig mod was found: {os.path.basename(url)}")
        if prompt_yes_no("Would you like to update?"):
            old_ap_mod = os.path.join(forge_dir, 'mods', ap_randomizer) if ap_randomizer else None
            new_ap_mod = os.path.join(forge_dir, 'mods', os.path.basename(url))
            resp = requests.get(url)
            if resp.status_code == 200:
                with open(new_ap_mod, 'wb') as f:
                    f.write(resp.content)
                if old_ap_mod:
                    os.remove(old_ap_mod)
            else:
                logging.error("Failed to download Dig mod.")
                sys.exit(1)


def check_eula(forge_dir):
    eula_path = os.path.join(forge_dir, "eula.txt")
    if not os.path.isfile(eula_path):
        with open(eula_path, 'w') as f:
            f.write(f"#By changing the setting below to TRUE you are indicating your agreement to our EULA\n")
            f.write(f"eula=false\n")
    with open(eula_path, 'r+') as f:
        text = f.read()
        if 'false' in text:
            logging.info("You must agree to the Minecraft EULA to run the server.")
            if prompt_yes_no("Do you agree to the EULA?"):
                f.seek(0)
                f.write(text.replace('false', 'true'))
                f.truncate()
            else:
                sys.exit(0)


def find_jdk_dir(version: str) -> str | None:
    for entry in os.listdir():
        if os.path.isdir(entry) and entry.startswith(f"jdk{version}"):
            return os.path.abspath(entry)


def find_jdk(version: str) -> str:
    if is_windows:
        jdk = find_jdk_dir(version)
        if jdk:
            jdk_exe = os.path.join(jdk, "bin", "java.exe")
            if os.path.isfile(jdk_exe):
                return jdk_exe
        return "java"
    else:
        settings = get_settings()
        java_cmd = getattr(settings.minecraft_dig_options, "java", "java")
        jdk_exe = shutil.which(java_cmd)
        if not jdk_exe:
            raise Exception("Could not find Java.")
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


def is_correct_forge(forge_dir, forge_url) -> bool:
    forge_version = forge_url.split("/")[-1].replace("forge-", "").replace("-installer.jar", "")
    return os.path.isdir(os.path.join(forge_dir, "libraries", "net", "minecraftforge", "forge", forge_version))


def install_forge(directory, forge_url, java_version):
    java_exe = find_jdk(java_version)
    resp = requests.get(forge_url)
    if resp.status_code != 200:
        raise ValueError(f"Forge installer could not be downloaded: {forge_url}")
    forge_install_jar = os.path.join(directory, "forge_install.jar")
    os.makedirs(directory, exist_ok=True)
    with open(forge_install_jar, 'wb') as f:
        f.write(resp.content)
    print(f"Installing Forge...")
    Popen([java_exe, "-jar", forge_install_jar, "--installServer", directory]).wait()
    os.remove(forge_install_jar)


def run_forge_server(forge_dir: str, java_version: str, heap_arg: str, forge_url) -> Popen:
    java_exe = find_jdk(java_version)
    if heap_arg[-1] in ['b', 'B']:
        heap_arg = heap_arg[:-1]
    heap_arg = "-Xmx" + heap_arg
    os_args = "win_args.txt" if is_windows else "unix_args.txt"
    forge_version = forge_url.split("/")[-1].replace("forge-", "").replace("-installer.jar", "")
    args_file = os.path.join(forge_dir, "libraries", "net", "minecraftforge", "forge", forge_version, os_args)
    forge_args = []
    if os.path.isfile(args_file):
        with open(args_file) as argfile:
            for line in argfile:
                forge_args.extend(line.strip().split(" "))
    args = [java_exe, heap_arg, *forge_args, "-nogui"]
    os.chdir(forge_dir)
    return Popen(args)


def add_to_launcher_components():
    component = Component(
        "Minecraft Dig Client",
        func=run_client,
        component_type=Type.CLIENT,
        file_identifier=SuffixIdentifier(".apmcdig"),
        cli=True
    )
    components.append(component)


def run_client(*args):
    Utils.init_logging("MinecraftDigClient")
    parser = argparse.ArgumentParser()
    parser.add_argument("apmcdig_file", default=None, nargs='?', help="Path to a Minecraft Dig .apmcdig file")
    parser.add_argument('--install', '-i', dest='install', default=False, action='store_true',
                        help="Install Java and Forge without launching.")
    parser.add_argument('--java', '-j', dest='java', type=str, help="Java version to use")
    parser.add_argument('--forge', '-f', dest='forge', type=str, help="Forge installer URL")
    parser.add_argument('--mod', '-m', dest='mod', type=str, help="Override Dig mod URL from host.yaml")
    args = parser.parse_args(args)

    settings = get_settings()
    mc_settings = settings.minecraft_dig_options
    forge_dir = os.path.expanduser(str(mc_settings.forge_directory))
    max_heap = getattr(mc_settings, "max_heap_size", "2G")

    java_version = getattr(mc_settings, "java_version", "") or args.java or DEFAULT_DIG_JAVA_VERSION
    forge_url = getattr(mc_settings, "forge_url", "") or args.forge or DEFAULT_DIG_FORGE_URL
    mod_url = getattr(mc_settings, "dig_mod_url", "") or args.mod or DEFAULT_DIG_MOD_URL
    java = getattr(mc_settings, "java", "")
    java_dir = find_jdk_dir(java_version)

    if args.install:
        print("Installing Java and Forge for Minecraft Dig...")
        if is_windows:
            print("Installing Java...")
            download_java(java_version)
        if not is_correct_forge(forge_dir, forge_url):
            if prompt_yes_no("Forge is not installed. Would you like to install it now?"):
                install_forge(forge_dir, forge_url, java_version)
        else:
            print("Correct Forge version already found, skipping install.")
        sys.exit(0)

    apmcdig_file = os.path.abspath(args.apmcdig_file) if args.apmcdig_file else None
    if apmcdig_file is None:
        apmcdig_file = Utils.open_filename('Select APMCDig file', (('APMCDig File', ('.apmcdig',)),))

    if is_windows:
        if java_dir is None or not os.path.isdir(java_dir):
            if prompt_yes_no("Did not find java directory. Download and install java now?"):
                download_java(java_version)
                java_dir = find_jdk_dir(java_version)
            if java_dir is None or not os.path.isdir(java_dir):
                raise NotADirectoryError(f"Path {java_dir} does not exist or could not be accessed.")

    if not is_correct_forge(forge_dir, forge_url):
        if prompt_yes_no("Forge is not installed. Would you like to install it now?"):
            install_forge(forge_dir, forge_url, java_version)
        else:
            sys.exit(0)

    replace_apmcdig_files(forge_dir, apmcdig_file)
    update_mod(forge_dir, mod_url)
    check_eula(forge_dir)

    if not max_heap_re.match(max_heap):
        raise Exception(f"Max heap size {max_heap} incorrect format. Use e.g., 2G or 512M.")

    server_process = run_forge_server(forge_dir, java_version, max_heap, forge_url)
    wait_for_server_ready(forge_dir)
    try_auto_launch_minecraft()
    server_process.wait()
