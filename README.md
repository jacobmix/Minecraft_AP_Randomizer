# Instructions:
0. Recommend installing latest stable [Archipelago](<https://github.com/ArchipelagoMW/Archipelago/releases/latest>) version (0.6.5) with "clean libs" ticked on.
1. Install the [minecraft.apworld](https://github.com/jacobmix/Minecraft_AP_Randomizer/releases/latest/download/minecraft.apworld) hotfix (double click it), restart AP, and 'Generate Template Options' in the launcher.
2. Use 'Option Creator' in the launcher to create your Minecraft player yaml (or edit it manually with a text editor like [Notepad++](<https://notepad-plus-plus.org/downloads/>))
   - Give yaml to host or put yaml(s) in the ``Players`` folder, and press "Generate" to get a multiworld ``.zip`` in the ``output`` folder<br/>
   (Terminal window will close if gen successful)
   - Host by uploading to the multiworld zip to the [site](<https://archipelago.gg/uploads>).
     - Only need one Minecraft yaml. Multiple players can join the same mc server/slot.<br/>
     Can have more Minecraft yamls/slots but they won't run on the same server.<br/>
     Note: Each patch is file is it's own server. If you wanna run multiple. Run client once with patch. Then copy APData folder for each.  
3. Get ``.apmc`` patch file from either the room on the site/host themselves/extracted multiworld zip.
4. Open Archipelago Launcher, and open the Minecraft Client.
5. Client should ask for ``.apmc`` file. Point it to the one received from room/host/zip.
6. There will be prompts to install Java, Forge, and the randomizer mod (if none are installed). Say yes to all.
7. Manually open [Minecraft Java Edition](<https://www.minecraft.net/en-us/store/minecraft-java-edition>) (v1.20.4), go to ``Multiplayer > Direct Connection``, type (literally):<br/>
``localhost`` in the server address, and join. (Or IP of player running the Forge Server with ``.apmc``)
   - if you want other players to join your slot. You need to port forward the port, and allow the forge sever through your firewall. Minecraft Forge Server port by default is ``25565``
8. When in-game, you will need to connect manually using ``/connect <IP> <PORT> <PASS>`` Example: ``/connect archipelago.gg 38281 GoodPass``
   - You don't need to set pass if not used.
9. To start. Simply type ``/start``, and press Enter in-game.

How the rando works: <https://github.com/ArchipelagoMW/Archipelago/blob/0.6.1/worlds/minecraft/docs/en_Minecraft.md>  
You can check unlocks in the advancement "received items" menu. [Frequently Asked Questions](<https://docs.google.com/document/d/1AMcototDovob8YJ7w4UFKAiUMCV21uQjJqzw_dr-sJQ>).  
Tracker: <https://github.com/Cyb3RGER/minecraft_rando_tracker>  
> Original setup doc: <https://github.com/ArchipelagoMW/Archipelago/blob/0.6.1/worlds/minecraft/docs/minecraft_en.md>  

### What is Minecraft Dig?  
You dig out a chunk. Each layer is a check. You'll get goodies (like picks or scaffolding) or traps along the way.  
> Note that Dig patch files are ``.apmcdig`` instead of ``.apmc`` It also uses Minecraft 1.19.4. Instead of 1.20.4.

Note:  
> It is highly recommended to use the Archipelago installer to handle the installation of the forge server for you.  
> Support will not be given for those wishing to manually install forge.  
> Will also not help with using other mods besides AP. Tho will say both client & server probably needs the same mods.  
> Server mods usually at: ``C:\ProgramData\Archipelago\Minecraft Forge server\mods``  
## Troubleshooting:  
If an item didn't get sent. Check if it's location is collected on the sever end, by looking at the room tracker page.  
Could have been collected from another game goaling, and collecting it's items from other games.  
Else host can manually send the location with this [command](<https://archipelago.gg/tutorial/Archipelago/commands_en>): ``/send_location <player name> <location name>``  
Or you type: ``/op <Minecraft Username>`` in forge server terminal. Then in-game: ``/advancement (grant|revoke) @a only <advancement>``  
[Example](<https://minecraft.wiki/w/Commands/advancement>): ``/advancement grant @a only aprandomizer:archipelago/get_wood``  
If your slot is too broken you can use the: [Slow Release Client](https://github.com/gjgfuj/AP-SlowRelease/releases/latest) (Requires [Universal Tracker](<https://github.com/FarisTheAncient/Archipelago/releases?q=Tracker>))  

 ``No dataPackage found`` that's fine. It's an expected message, and you won't get a message when one has been made.  

Connect command don't work: You need to be on the server. Direct connect to ``localhost``.  

``IncompatibleVersion`` check you're on Minecraft version 1.20.4.  

Linux: ``IllegalArgumentException``  
Install Java 17 <https://github.com/corretto/corretto-17/releases/latest>  
Add the actual full path for Java to your ``host.yaml``:  
```yaml
minecraft_options:
  java: "/home/<user>/.local/share/Archipelago/jdk17.0.17_10/bin/java"
```

``missing/not found win_args``, ``wrong/no .apmc``, ``UnicodeDecodeError``, ``getsockopt``, ``Module 'world.minecraft' has no attribute`` or crashing:  
Delete ``Minecraft Forge server`` and redo setup. Backup ``APData`` & ``Archipelago-#-X``.  

If you see in your forge server logs: ``java.io.IOException: The process cannot access the file``  
Something might be blocking files. If you're syncing the Archipelago folder with OneDrive disable that.  
May also be you're running a forge server already. Close it. Else it could be your anti-virus:  
> - Windows default anti-virus: Search (Windows Key+S): "Windows Security", and open it.  
>   - Virus & threat protection>Protection history>Click newest top one>ADMIN prompt: Yes>See if "Affected items" is anything Archipelago/Minecraft related. If so follow this next step:  
>   - Virus/protection>Manage settings>Add or remove exclusions>ADMIN prompt: Yes>Add an exclusion>Folder>"Archipelago, Minecraft, and patch location directories"  

Also check files aren't encrypted:  
> You can right click folder>properties to check if it's being encrypted.  
(It might say some files are ready only ``-``, and they will be reset later anyway)  
<img width="740" height="227" alt="Folder_properties" src="https://github.com/user-attachments/assets/1bfc5218-8b19-4453-8d47-d5cfd6312279" />

Firewall/port:  
> - Allow Forge Server through firewall  
>   - Open: ``control.exe`` (Searching in Windows)  
>   - ``Control Panel\All Control Panel Items\Windows Defender Firewall\Allowed apps``>``Change Settings (Admin)``>``Allow another app``>``Browse``>``Forge-x.jar`` >``Add``>``Allow apps``  
> - PortForward [mod](<https://modrinth.com/mod/e4mc/version/5.3.1-forge>) Place in ``mods`` folder of Forge server. Or you can use [this](<https://gist.github.com/jacobmix/ed6a0dcf3188f4843e414099fdd63cc4>) for any ports.  

## Tips:  
Show file extensions: <https://www.howtogeek.com/205086/beginner-how-to-make-windows-show-file-extensions/>  
Bonus apworlds (require ap restart):  
> - Easily update custom apworlds with [APWorld Manager](<https://github.com/silasary/Archipelago/releases?q=Manager>).  
> - Track what is in-logic with [Universal Tracker](https://github.com/FarisTheAncient/Archipelago/releases?q=Tracker).   

You can connect with just one string in the top of any text based client by putting your info in the top bar like this, and pressing enter:  
> ``<SLOT_NAME>:None@<IP/URL>:<PORT>``  
> Example: ``CoolPlayer:CoolerPassword@archipelago.gg:38281``  
``None`` just works with no pass set. But you can also leave it empty: ``CoolPlayer:@``, either works.  
Hosting locally on the same machine just use ``localhost`` for the IP. Localhost also doesn't need a port.  

Can use something like [MultiMC](<https://multimc.org/>) to create a shortcut for Minecraft v1.20.4. Even set it to auto connect to localhost.  
Also it's possible to set ``.apmc`` file to be opened with ``ArchipelagoLauncherDebug.exe``, and if you double click it'll instantly start the forge server.  
Open ``host.yaml`` from the launcher (or look in install directory). Then edit ``mc_launch`` under ``minecraft_options`` to auto start Minecraft after the Forge Server has started.  

To host a multiworld without auto collect enabled, open ``host.yaml`` with a text editor like Notepad++  
Change ``collect_mode`` setting from ``auto`` to ``disabled``. Or ``goal`` to allow manual collect. Then generate a multiworld.  
Other settings too. Like release mode, hint options, server password for remote admin commands, ect.  
Plando players might also want host to edit ``plando_options`` to ``bosses, items, texts, connections``  

## Manual install Software links
- [Minecraft Forge Download Page](<https://files.minecraftforge.net/net/minecraftforge/forge/>)
- [Minecraft Archipelago Randomizer Mod Releases Page](<https://github.com/cjmang/Minecraft_AP_Randomizer/releases/tag/0.1.3_hotfix>)
- [Minecraft Dig Archipelago Randomizer Mod Releases Page](<https://github.com/AshIndigo/Minecraft_AP_Randomizer/releases/tag/dig-v0.0.2-hotfix>)
   - **DO NOT INSTALL THESE MODS ON YOUR CLIENT**
- [Java 17 Download Page](<https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/downloads-list.html>)

