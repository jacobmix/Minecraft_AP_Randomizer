# Instructions:
0. Recommend installing latest stable [Archipelago](<https://github.com/ArchipelagoMW/Archipelago/releases/latest>) with **"clean libs" ticked on**. (Also try if client won't open or see ``no attribute 'mc_launch'``)
1. Install the [minecraft.apworld](https://github.com/jacobmix/Minecraft_AP_Randomizer/releases?q=main) or [minecraft_dig.apworld](https://github.com/jacobmix/Minecraft_AP_Randomizer/releases?q=dig). (double click it after downloading), and wait for the installed popup.<br/> Then **FULLY RESTART ARCHIPELAGO**, and 'Generate Template Options' in the launcher.  
2. Use 'Option Creator' in the launcher to create your Minecraft player yaml (or edit it manually with a text editor like [Notepad++](<https://notepad-plus-plus.org/downloads/>))
   - Give yaml to host or put yaml(s) in the ``Players`` folder, and press "Generate" to get a multiworld ``.zip`` in the ``output`` folder<br/>
   (Terminal window will close if gen successful)
   - Host by uploading to the multiworld zip to the [site](<https://archipelago.gg/uploads>).
     - Only need one Minecraft yaml. Multiple players can join the same mc server/slot.<br/>
     Can have more Minecraft yamls/slots but they won't run on the same server.<br/>
     Note: Each patch is file is it's own server. If you wanna run multiple. Run client once with patch. Then copy APData folder for each.  
3. Get ``.apmc`` patch file from either the room on the site/host themselves/extracted multiworld zip.
4. Open Archipelago Launcher. Then Minecraft Client in there. (Can hint in-game or with Text Client. Export Datapackage for item names, ect)
5. Client will ask for ``.apmc`` file. Point it to the one received from room/host/zip. (If no prompts try running ``ArchipelagoLauncherDebug.exe``  
   Found in the Archipelago directory. Usually at ``C:\ProgramData\Archipelago`` or simply open "Brows Files" in the Archipelago Launcher)  
6. There will be prompts to install Java, Forge, and the randomizer mod (if none are installed). Say yes to all.  
   **Do not close the terminal! That is the Minecraft Forge server you'll be connecting to, and needs to stay open!**
7. Manually open [Minecraft Java Edition](<https://www.minecraft.net/en-us/store/minecraft-java-edition>) (Needs to be v1.20.4 or v1.19.4 for dig), go to ``Multiplayer > Direct Connection``,<br/>
type (literally): ``localhost`` in the server address, and join. (Or IP of player running the Forge Server with ``.apmc``)
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

``IncompatibleVersion`` check you're on Minecraft version 1.20.4.  

**Linux**: You'll most likely have to run Archipelago with your terminal.  
``IllegalArgumentException``/``Missing required library``: Install Java 17 <https://github.com/corretto/corretto-17/releases/latest>  
Add the actual full path for Java to your ``host.yaml`` (if it fails try a path with no spaces):  
```yaml
minecraft_options:
  java: "/home/<user>/.local/share/Archipelago/jdk17.0.17_10/bin/java"
```

``missing/not found win_args``, ``wrong/no .apmc``, ``UnicodeDecodeError``, ``getsockopt``, ``Module 'world.minecraft' has no attribute`` or crashing:  
Close any running Java processes, and delete ``Minecraft Forge server`` and redo setup. Backup ``APData`` & ``Archipelago-#-X``.  

If Forge server gives ``OutOfMemoryError`` and you see ``_JAVA_OPTIONS``. Then delete ``_JAVA_OPTIONS`` from Environment Variables.
Either delete in Windows [sysdm.cpl](<https://superuser.com/questions/949560/how-do-i-set-system-environment-variables-in-windows-10>) or download & run [Rapid Environment Editor](<https://www.rapidee.com/en/download>) as admin.

If you see in your forge server logs: ``java.io.IOException: The process cannot access the file``
Something is blocking files. Don't cloud sync the Archipelago  directory with OneDrive, ect.
Maybe running forge server already. Close any java process with task manager. Else could be your anti-virus:
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
> - PortForward [mod](<https://modrinth.com/mod/dedicatedmcupnp/version/uru0Bn9t>) + [Architectury API](<https://modrinth.com/mod/architectury-api/version/11.1.17+minecraftforge>) (or [e4mc](<https://modrinth.com/mod/e4mc/version/5.3.1-forge>)). Place in ``mods`` folder of Forge server. Or try [this](<https://gist.github.com/jacobmix/ed6a0dcf3188f4843e414099fdd63cc4>). 

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

If you prefere to host a multiworld without auto collect enabled, open ``host.yaml`` with a text editor like Notepad++  
Change ``collect_mode`` setting from ``auto`` to ``disabled``. Or ``goal`` to allow manual collect. Then generate a multiworld.  
Maybe change other settings too. Like release mode, hint options, set a server password for remote admin commands (recommended), ect.  
Plando players might also want host to edit ``plando_options`` to ``bosses, items, texts, connections``  

## Manual install Software links
- [Minecraft Forge Download Page](<https://files.minecraftforge.net/net/minecraftforge/forge/>)
- [Minecraft Archipelago Randomizer Mod Releases Page](<https://github.com/cjmang/Minecraft_AP_Randomizer/releases/>)
- [Minecraft Dig Archipelago Randomizer Mod Releases Page](<https://github.com/AshIndigo/Minecraft_AP_Randomizer/releases/>)
   - **DO NOT INSTALL THESE MODS ON YOUR CLIENT**
- [Java 17 Download Page](<https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/downloads-list.html>)

