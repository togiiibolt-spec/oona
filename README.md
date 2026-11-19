# Oona

Oona client for Minecraft 26.2 (Fabric): the Carrot base plus PvP modules, a shader and
resource pack library, a new title screen and an Iris + Sodium setup.

Build: push to GitHub, open Actions, download the `oona-client` artifact (`oona-client-26.2.jar`).
Put only that jar in your mods folder (plus Fabric API).

PvP: Variety, PvP God, Aimbot, Aim Control, High Damage, Reach, Auto Pots, Auto Totem,
Auto Bridge, Auto Clutch, BedWars Assist. Util: Speed Mine, Xray.
Every toggle has a card in the mod menu and a keybind (Controls > Oona).
Default keys: V variety, U god, H aimbot, G totem, K bridge, J xray, O library.

Library (Ctrl+S in a world, or O): shaders and resource packs; drop a .zip on the window to add your own.
Shaders run on the bundled Iris + Sodium.

Branding lives in `branding/` (logo.png is the 256x256 texture used everywhere) and `tools/merge.py`
(renames the visible text in the original classes). Settings: `config/carrotmods.json`.
