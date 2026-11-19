#!/usr/bin/env python3
"""Build the Oona client jar.

base jar  +  build/libs/carrotmods-*.jar  +  base/mods/*.jar (Iris, Sodium)  +  branding
  ->  build/oona-client-26.2.jar

Adds a second client entrypoint, a second (non-required) mixin config and the bundled mods
as jar-in-jar to fabric.mod.json, swaps the logo texture, and renames the visible
"Carrot" strings in the original classes. Internal class and asset names stay as they are.
"""
import glob, json, os, struct, sys, zipfile

BASE = "base/carrot-client-26.2.jar"
OUT = "build/oona-client-26.2.jar"
LOGO_ENTRY = "assets/carrotclient/textures/gui/logo.png"

# visible text only; identifiers (com/carrot/..., carrotclient) are never touched
RENAME = {
    "CARROT CLIENT": "OONA",
    "Carrot Client 26.2": "Oona 26.2",
    "Carrot Client": "Oona",
    "Carrot Cap": "Oona Cap",
    "Carrot Cloak": "Oona Cloak",
    "Carrot Hoodie": "Oona Hoodie",
}
# constant pool entry sizes by tag (everything except Utf8, handled separately)
SIZES = {3: 5, 4: 5, 5: 9, 6: 9, 7: 3, 8: 3, 9: 5, 10: 5, 11: 5, 12: 5, 15: 4, 16: 3, 17: 5, 18: 5, 19: 3, 20: 3}


def rename_strings(data: bytes) -> bytes:
    """Rewrites matching Utf8 constants. Other structures refer to constants by index, so
    changing a string's length is safe."""
    count = struct.unpack(">H", data[8:10])[0]
    out = bytearray(data[:10])
    p, i = 10, 1
    while i < count:
        tag = data[p]
        if tag == 1:
            n = struct.unpack(">H", data[p + 1:p + 3])[0]
            raw = data[p + 3:p + 3 + n]
            text = raw.decode("utf-8", "replace")
            if text in RENAME:
                new = RENAME[text].encode("utf-8")
                out += b"\x01" + struct.pack(">H", len(new)) + new
            else:
                out += data[p:p + 3 + n]
            p += 3 + n
        else:
            size = SIZES[tag]
            out += data[p:p + size]
            p += size
            if tag in (5, 6):
                i += 1
        i += 1
    out += data[p:]
    return bytes(out)


mods = [p for p in glob.glob("build/libs/carrotmods-*.jar") if not p.endswith("-sources.jar")]
if not mods:
    sys.exit("no carrotmods jar in build/libs")
mods = mods[0]
nested = sorted(glob.glob("base/mods/*.jar"))
logo = open("branding/logo.png", "rb").read()

seen = set()
renamed = 0
with zipfile.ZipFile(BASE) as base, zipfile.ZipFile(mods) as add, \
        zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as out:
    fm = json.loads(base.read("fabric.mod.json"))
    fm["entrypoints"]["client"].append("com.carrot.mods.CarrotMods")
    fm["mixins"].append("carrotmods.mixins.json")
    fm["name"] = "Oona"
    fm["description"] = "Oona client: PvP modules, shader library, Iris and Sodium."
    fm.setdefault("jars", [])

    for info in base.infolist():
        n = info.filename
        if info.is_dir() or n == "fabric.mod.json":
            continue
        data = base.read(n)
        if n == LOGO_ENTRY:
            data = logo
        elif n.endswith(".class"):
            patched = rename_strings(data)
            renamed += patched != data
            data = patched
        out.writestr(n, data)
        seen.add(n)

    for info in add.infolist():
        n = info.filename
        if info.is_dir() or n.startswith("META-INF/") or n == "fabric.mod.json":
            continue
        if n in seen:
            sys.exit("collision on " + n)
        out.writestr(n, add.read(n))
        seen.add(n)

    for path in nested:
        entry = "META-INF/jars/" + os.path.basename(path)
        if entry in seen:
            sys.exit("collision on " + entry)
        out.write(path, entry)
        fm["jars"].append({"file": entry})
        seen.add(entry)

    out.writestr("fabric.mod.json", json.dumps(fm, indent=2))

print("wrote", OUT, "|", len(seen) + 1, "entries |", len(nested), "bundled mods |",
      renamed, "classes renamed | logo swapped")
