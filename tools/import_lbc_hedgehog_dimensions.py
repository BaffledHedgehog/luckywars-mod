#!/usr/bin/env python3
"""Migrate the two authored confuse-spike destinations to the local 26.3 codecs."""
import copy
import json
from import_lbc_loadouts import ROOT, SOURCE

DEST = ROOT / "src/main/resources/data/minecraft"
LEGACY = SOURCE.parent / "minecraft"

def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2)+"\n", encoding="utf-8", newline="\n")

def migrate():
    for name in ("nexus", "imprinted"):
        dimension=json.loads((LEGACY/f"dimension/{name}.json").read_text(encoding="utf-8-sig"))
        # A zero-height air layer and no layers describe the same void generator.
        dimension["generator"]["settings"]["layers"]=[layer for layer in dimension["generator"]["settings"]["layers"] if layer["height"]]
        write(DEST/f"dimension/{name}.json",dimension)
        old=json.loads((LEGACY/f"dimension_type/{name}.json").read_text(encoding="utf-8-sig"))
        keep=("has_skylight","has_ceiling","coordinate_scale","ambient_light","logical_height","infiniburn","min_y","height","monster_spawn_light_level","monster_spawn_block_light_limit")
        data={key:copy.deepcopy(old[key]) for key in keep}
        data.update(has_fixed_time=False,has_ender_dragon_fight=False,
                    skybox="none" if name=="nexus" else "overworld",
                    cardinal_light="nether" if name=="nexus" else "default",
                    default_clock="minecraft:overworld",timelines="#minecraft:in_overworld")
        data["attributes"]={"minecraft:gameplay/can_start_raid":old["has_raids"],
            "minecraft:gameplay/respawn_anchor_works":old["respawn_anchor_works"],
            "minecraft:gameplay/piglins_zombify":not old["piglin_safe"],
            "minecraft:gameplay/water_evaporates":old["ultrawarm"]}
        write(DEST/f"dimension_type/{name}.json",data)
        old=json.loads((LEGACY/f"worldgen/biome/{name}.json").read_text(encoding="utf-8-sig"))
        data={key:copy.deepcopy(old[key]) for key in ("temperature","downfall","has_precipitation","temperature_modifier","carvers","features")}
        effect=old["effects"]
        data["effects"]={key:(f"#{value:06X}" if key.endswith("_color") else value)
                         for key,value in effect.items() if key in ("water_color","foliage_color","grass_color","grass_color_modifier")}
        attrs={"minecraft:visual/sky_color":f"#{effect['sky_color']:06X}",
               "minecraft:visual/fog_color":f"#{effect['fog_color']:06X}",
               "minecraft:visual/water_fog_color":f"#{effect['water_fog_color']:06X}",
               "minecraft:audio/music_volume":effect["music_volume"],
               "minecraft:gameplay/creature_world_gen_spawn_probability":old["creature_spawn_probability"],
               "minecraft:gameplay/natural_mob_spawns":{"spawns_by_category":old["spawners"],"spawn_costs":old["spawn_costs"]}}
        if "mood_sound" in effect:attrs["minecraft:audio/ambient_sounds"]={"mood":effect["mood_sound"]}
        data["attributes"]=attrs
        write(DEST/f"worldgen/biome/{name}.json",data)

if __name__ == "__main__":
    migrate()
