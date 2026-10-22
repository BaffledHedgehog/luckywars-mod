#!/usr/bin/env python3
"""Build Woodificator's offline texture-colour lookup (Python 3 + Pillow).

Example (resource packs are ordered from lowest to highest priority)::

  python tools/generate_woodificator_palette.py --client-jar CLIENT.jar \
    --resource-pack USER_PACK --registry build/verification/woodificator-block-registry.json

This reads assets only; it never edits the client or resource pack. Ordinary models
resolve parents, texture variables, blockstate variants and multipart conditions.
Every rendered face contributes its geometric area times mean texture alpha;
transparent texels contribute neither colour nor weight. UV rectangles and all six
faces are sampled, including log end caps. Animation frames use mcmeta durations.
Pixels are averaged in linear sRGB, then nearest wood is selected by CIE76 distance
in D65 CIELAB (an already wooden source retains itself on a zero-distance tie;
otherwise lexicographic ID resolves ties). No Minecraft map colours are used.

Biome-dependent faces use a documented representative plains biome (temperature
0.8, downfall 0.4); grass/foliage multipliers come from the actual colormap assets.
Other native fixed tints and dye multipliers are listed below. This deliberately
generates one stable lookup per block ID, not biome/state-dependent runtime maths.
Special renderer atlases use explicit texture/UV adapters; particle-only models
are reported as fallbacks, never silently replaced by an invented colour.

The explicit preserved_wood list comes from vanilla semantic block tags plus an
audited wood-only furniture/root list, not name substrings or texture colour.
Runtime must preserve these source shapes ONLY when the actual state has no
block entity. Wooden containers/signs/shelves with block entities still convert.
"""
from __future__ import annotations

import argparse
from collections import Counter
from functools import lru_cache
import hashlib
import io
import itertools
import json
import math
from pathlib import Path
import zipfile

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
EXCLUDED = {"minecraft:" + name for name in
            ("air", "cave_air", "void_air", "light", "structure_void", "barrier")}
DYES = dict(zip(("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
                 "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"),
                (0xF9FFFE, 0xF9801D, 0xC74EBD, 0x3AB3DA, 0xFED83D, 0x80C71F, 0xF38BAA, 0x474F52,
                 0x9D9D97, 0x169C9C, 0x8932B8, 0x3C44AA, 0x835432, 0x5E7C16, 0xB02E26, 0x1D1D21)))


def resource(identifier, folder, suffix=".json"):
    namespace, name = identifier.split(":", 1) if ":" in identifier else ("minecraft", identifier)
    return f"assets/{namespace}/{folder}/{name}{suffix}"


def rgb_int(value):
    return ((value >> 16) & 255, (value >> 8) & 255, value & 255)


def linear(value):
    value /= 255
    return value / 12.92 if value <= .04045 else ((value + .055) / 1.055) ** 2.4


def srgb(value):
    return 255 * (12.92 * value if value <= .0031308 else 1.055 * value ** (1 / 2.4) - .055)


def lab(rgb):
    r, g, b = rgb
    x = (r * .4124564 + g * .3575761 + b * .1804375) / .95047
    y = r * .2126729 + g * .7151522 + b * .0721750
    z = (r * .0193339 + g * .1191920 + b * .9503041) / 1.08883
    def f(v):
        return v ** (1 / 3) if v > 216 / 24389 else v * (24389 / 27) / 116 + 16 / 116
    x, y, z = f(x), f(y), f(z)
    return (116 * y - 16, 500 * (x - y), 200 * (y - z))


class Assets:
    def __init__(self, client, packs):
        self.layers = [(str(client.resolve()), zipfile.ZipFile(client))]
        for pack in packs:
            self.layers.append((str(pack.resolve()), pack if pack.is_dir() else zipfile.ZipFile(pack)))
        self.used = {}

    @lru_cache(maxsize=None)
    def read(self, name):
        for label, layer in reversed(self.layers):
            if isinstance(layer, Path):
                path = layer / name
                if not path.is_file():
                    continue
                data = path.read_bytes()
            else:
                try:
                    data = layer.read(name)
                except KeyError:
                    continue
            self.used[name] = (label, hashlib.sha256(data).hexdigest())
            return data
        return None

    def json(self, name):
        data = self.read(name)
        return json.loads(data) if data is not None else None

    @lru_cache(maxsize=None)
    def vanilla_tag(self, identifier):
        # Client resource packs cannot redefine server data tags. Read the
        # installed vanilla semantic tags from the client jar alone.
        name = resource(identifier,"tags/block").replace("assets/","data/",1)
        label, client = self.layers[0]
        try:
            raw = client.read(name)
        except KeyError:
            raise ValueError(f"Missing vanilla wood classification tag {identifier}")
        self.used[name] = (label,hashlib.sha256(raw).hexdigest())
        result = set()
        for entry in json.loads(raw).get("values",[]):
            value = entry if isinstance(entry,str) else entry["id"]
            if value.startswith("#"):
                result.update(self.vanilla_tag(value[1:]))
            else:
                result.add(value if ":" in value else "minecraft:"+value)
        return frozenset(result)

    @lru_cache(maxsize=None)
    def model(self, identifier):
        data = self.json(resource(identifier, "models"))
        if data is None:
            raise ValueError(f"Missing model {identifier}")
        parent = data.get("parent")
        inherited = self.model(parent) if parent and "builtin/" not in parent else {}
        result = inherited | data
        result["textures"] = inherited.get("textures", {}) | data.get("textures", {})
        return result

    @lru_cache(maxsize=None)
    def image(self, texture):
        raw = self.read(resource(texture, "textures", ".png"))
        if raw is None:
            raise ValueError(f"Missing texture {texture}")
        return Image.open(io.BytesIO(raw)).convert("RGBA")

    @lru_cache(maxsize=None)
    def frames(self, texture):
        image = self.image(texture)
        meta = self.json(resource(texture, "textures", ".png.mcmeta")) or {}
        animation = meta.get("animation")
        if animation is None:
            return ((image, 1),)
        # Minecraft's unspecified animation size is a square of the shorter side.
        width = animation.get("width", image.width if "height" in animation else min(image.size))
        height = animation.get("height", image.height if "width" in animation else min(image.size))
        across, down = image.width // width, image.height // height
        default = animation.get("frametime", 1)
        frames = animation.get("frames", list(range(across * down)))
        result = []
        for frame in frames:
            index, duration = (frame, default) if isinstance(frame, int) else (frame["index"], frame.get("time", default))
            if index < 0 or index >= across * down or duration <= 0:
                raise ValueError(f"Invalid frame in {texture}")
            x, y = index % across * width, index // across * height
            result.append((image.crop((x, y, x + width, y + height)), duration))
        return tuple(result)

    @lru_cache(maxsize=None)
    def sample(self, texture, uv=(0, 0, 16, 16), tint=(255, 255, 255)):
        accum, mass = [0., 0., 0.], 0.
        frames = self.frames(texture)
        duration_sum = sum(duration for _, duration in frames)
        for image, duration in frames:
            x1, x2 = sorted((uv[0] * image.width / 16, uv[2] * image.width / 16))
            y1, y2 = sorted((uv[1] * image.height / 16, uv[3] * image.height / 16))
            area = (x2 - x1) * (y2 - y1)
            if area <= 0:
                continue
            for y in range(math.floor(y1), math.ceil(y2)):
                wy = max(0, min(y + 1, y2) - max(y, y1))
                for x in range(math.floor(x1), math.ceil(x2)):
                    wx = max(0, min(x + 1, x2) - max(x, x1))
                    pixel = image.getpixel((x % image.width, y % image.height))
                    weight = wx * wy * pixel[3] / 255 * duration / duration_sum / area
                    mass += weight
                    for channel in range(3):
                        accum[channel] += linear(pixel[channel] * tint[channel] / 255) * weight
        return tuple(accum), mass


def texture_reference(value, textures):
    visited = set()
    while True:
        # 26.3 permits {sprite,force_translucent} in place of a texture string.
        if isinstance(value,dict):
            value = value["sprite"]
        # The installed heavy_core model uses bare "all" rather than "#all".
        if not value.startswith("#") and value in textures:
            value = "#"+value
        if not value.startswith("#"):
            return value
        if value in visited:
            raise ValueError(f"Texture reference cycle: {value}")
        visited.add(value)
        value = textures[value[1:]]


def condition_matches(condition, state):
    if "OR" in condition:
        return any(condition_matches(entry, state) for entry in condition["OR"])
    if "AND" in condition:
        return all(condition_matches(entry, state) for entry in condition["AND"])
    return all(state.get(key) in str(value).split("|") for key, value in condition.items())


def condition_domains(condition, domains):
    for key, value in condition.items():
        if key in ("OR", "AND"):
            for entry in value:
                condition_domains(entry, domains)
        else:
            domain = domains.setdefault(key, set())
            domain.update(str(value).split("|"))
            if domain & {"true", "false"}:
                domain.update(("true", "false"))


def model_weights(blockstate):
    """Marginal model frequencies; rotations preserve total face area and colour."""
    weights = Counter()
    def add(apply, probability):
        choices = apply if isinstance(apply, list) else [apply]
        total = sum(choice.get("weight", 1) for choice in choices)
        for choice in choices:
            weights[choice["model"]] += probability * choice.get("weight", 1) / total
    variants = blockstate.get("variants", {})
    domains = {}
    for key in variants:
        condition_domains(dict(part.split("=", 1) for part in key.split(",") if part), domains)
    for key, apply in variants.items():
        specified = dict(part.split("=", 1) for part in key.split(",") if part)
        probability = math.prod(1 / len(domains[prop]) for prop in specified)
        add(apply, probability)
    multipart = blockstate.get("multipart", [])
    domains = {}
    for part in multipart:
        condition_domains(part.get("when", {}), domains)
    properties = sorted(domains)
    states = [dict(zip(properties, values)) for values in itertools.product(*(sorted(domains[p]) for p in properties))]
    for part in multipart:
        probability = sum(condition_matches(part.get("when", {}), state) for state in states) / len(states)
        add(part["apply"], probability)
    return weights


def default_uv(face, start, end):
    x1, y1, z1 = start
    x2, y2, z2 = end
    return {"down": (x1, 16 - z2, x2, 16 - z1), "up": (x1, z1, x2, z2),
            "north": (16 - x2, 16 - y2, 16 - x1, 16 - y1),
            "south": (x1, 16 - y2, x2, 16 - y1),
            "west": (z1, 16 - y2, z2, 16 - y1),
            "east": (16 - z2, 16 - y2, 16 - z1, 16 - y1)}[face]


class Palette:
    def __init__(self, assets):
        self.assets = assets
        self.evidence = {}

    def tint(self, block):
        name = block.split(":")[-1]
        if name in ("water", "bubble_column", "water_cauldron"):
            return rgb_int(0x3F76E4)
        if "redstone" in name:
            return rgb_int(0xB70000)  # representative power seven
        if name == "spruce_leaves":
            return rgb_int(0x619961)
        if name == "birch_leaves":
            return rgb_int(0x80A755)
        if name == "mangrove_leaves":
            return rgb_int(0x92C648)
        if name == "lily_pad":
            return rgb_int(0x208030)
        if name in ("melon_stem", "pumpkin_stem", "attached_melon_stem", "attached_pumpkin_stem"):
            return rgb_int(0xE0C71C)
        colormap = "foliage" if "leaves" in name or name in ("vine", "chorus_plant") else "grass"
        image = self.assets.image("minecraft:colormap/" + colormap)
        return image.getpixel((int((1 - .8) * 255), int((1 - .8 * .4) * 255)))[:3]

    def special(self, block):
        """Return explicit atlas samples (texture, UV/16, geometric area, tint).

        Whole-atlas adapters exclude alpha-zero padding, but are labelled as such
        in evidence: they approximate special-renderer UV frequency, not model faces.
        Chest UVs use the actual native lid/base/lock cuboids instead.
        """
        name = block.split(":")[-1]
        plain = name.removeprefix("waxed_")
        if plain.endswith("chest"):
            kind = {"chest": "normal", "trapped_chest": "trapped", "ender_chest": "ender",
                    "copper_chest": "copper", "exposed_copper_chest": "copper_exposed",
                    "weathered_copper_chest": "copper_weathered", "oxidized_copper_chest": "copper_oxidized"}.get(plain)
            if kind:
                # ModelPart Cube atlas layout: top/bottom followed by west,north,east,south.
                faces = []
                for u, v, w, h, d in ((0, 0, 14, 5, 14), (0, 19, 14, 10, 14), (0, 0, 2, 4, 1)):
                    rectangles = [(u+d,v,u+d+w,v+d,w*d), (u+d+w,v,u+d+2*w,v+d,w*d),
                                  (u,v+d,u+d,v+d+h,d*h), (u+d,v+d,u+d+w,v+d+h,w*h),
                                  (u+d+w,v+d,u+2*d+w,v+d+h,d*h), (u+2*d+w,v+d,u+2*d+2*w,v+d+h,w*h)]
                    for x1,y1,x2,y2,area in rectangles:
                        faces.append(("minecraft:entity/chest/"+kind, (x1/4,y1/4,x2/4,y2/4),area,(255,255,255)))
                return faces, "special_chest_cuboid_uv"
        texture, tint = None, (255,255,255)
        if name in ("water", "bubble_column", "lava"):
            fluid = "water" if name != "lava" else "lava"
            # Four sides and two caps; moving/still frames both contribute.
            tint = self.tint(block) if fluid == "water" else tint
            return [(f"minecraft:block/{fluid}_still",(0,0,16,16),2,tint),
                    (f"minecraft:block/{fluid}_flow",(0,0,16,16),4,tint)], "special_fluid_six_faces"
        if name in ("end_portal", "end_gateway"):
            texture = "minecraft:entity/end_portal/end_portal"
        elif "shulker_box" in name:
            color = name.removesuffix("_shulker_box") if name != "shulker_box" else ""
            texture = "minecraft:entity/shulker/shulker" + ("_"+color if color else "")
        elif name.endswith("banner"):
            color = name.replace("_wall_banner", "").replace("_banner", "")
            texture, tint = "minecraft:entity/banner/base", rgb_int(DYES[color])
        elif name == "conduit":
            texture = "minecraft:entity/conduit/base"
        elif name == "decorated_pot":
            return [("minecraft:entity/decorated_pot/decorated_pot_side",(0,0,16,16),4,tint),
                    ("minecraft:entity/decorated_pot/decorated_pot_base",(0,0,16,16),2,tint)], "special_pot_atlas"
        elif "copper_golem_statue" in plain:
            oxidation = plain.removesuffix("copper_golem_statue").rstrip("_")
            texture = "minecraft:entity/copper_golem/copper_golem" + ("_"+oxidation if oxidation else "")
        elif name.endswith(("_head", "_wall_head", "_skull", "_wall_skull")):
            kind = name.replace("_wall", "").removesuffix("_head").removesuffix("_skull")
            texture = {"player":"minecraft:entity/player/wide/steve", "zombie":"minecraft:entity/zombie/zombie",
                       "skeleton":"minecraft:entity/skeleton/skeleton", "wither_skeleton":"minecraft:entity/skeleton/wither_skeleton",
                       "creeper":"minecraft:entity/creeper/creeper", "piglin":"minecraft:entity/piglin/piglin",
                       "dragon":"minecraft:entity/enderdragon/dragon"}.get(kind)
            if texture and kind != "dragon":
                # Native skull cuboid head UVs; no torso/leg colours from the entity skin.
                # Skeleton/creeper skins use 64x32 rather than the 64x64 player layout.
                vscale = 2 if kind in ("skeleton","wither_skeleton","creeper") else 1
                rectangles = ((2,0,4,2),(4,0,6,2),(0,2,2,4),(2,2,4,4),(4,2,6,4),(6,2,8,4))
                return [(texture,(x1,y1*vscale,x2,y2*vscale),64,tint)
                        for x1,y1,x2,y2 in rectangles], "special_head_uv"
        if texture:
            return [(texture,(0,0,16,16),1,tint)], "special_renderer_alpha_atlas"
        return None

    @lru_cache(maxsize=None)
    def color(self, block):
        state = self.assets.json(resource(block,"blockstates"))
        if state is None:
            raise ValueError(f"No blockstate assets for {block}; provide visualBlock in registry export")
        models = model_weights(state)
        accum, mass, textures = [0.,0.,0.], 0., set()
        model_faces = []
        for identifier, probability in sorted(models.items()):
            model = self.assets.model(identifier)
            for element in model.get("elements", []):
                start, end = element["from"], element["to"]
                lengths = [abs(b-a) for a,b in zip(start,end)]
                rotation = element.get("rotation", {})
                scales = [1.,1.,1.]
                if rotation.get("rescale"):
                    scale = 1 / math.cos(math.radians(rotation.get("angle",0)))
                    for axis in range(3):
                        if "xyz"[axis] != rotation.get("axis"):
                            scales[axis] = scale
                for face, definition in element.get("faces", {}).items():
                    axes = {"up":(0,2),"down":(0,2),"north":(0,1),"south":(0,1),"east":(1,2),"west":(1,2)}[face]
                    area = probability * math.prod(lengths[a]*scales[a] for a in axes)
                    texture = texture_reference(definition["texture"],model["textures"])
                    uv = tuple(definition.get("uv", default_uv(face,start,end)))
                    tint = self.tint(block) if definition.get("tintindex",-1) >= 0 else (255,255,255)
                    model_faces.append((texture,uv,area,tint))
        method = "model_faces"
        faces = model_faces
        if not faces:
            special = self.special(block)
            if special:
                faces, method = special
            else:
                faces, method = [], "particle_fallback"
                for identifier, probability in sorted(models.items()):
                    model = self.assets.model(identifier)
                    particle = model.get("textures", {}).get("particle")
                    if particle:
                        faces.append((texture_reference(particle,model["textures"]),(0,0,16,16),probability,(255,255,255)))
        for texture,uv,area,tint in faces:
            sample, alpha = self.assets.sample(texture,uv,tint)
            textures.add(texture)
            mass += alpha*area
            for channel in range(3):
                accum[channel] += sample[channel]*area
        if mass <= 0:
            raise ValueError(f"No visible texture pixels for {block}; classify explicitly instead of guessing")
        result = tuple(channel/mass for channel in accum)
        self.evidence[block] = {"rgb": [round(srgb(value),3) for value in result],
                                "method":method,"textures":sorted(textures),"models":sorted(models)}
        return result


def wooden_candidates(assets, entries, wood_sources):
    # Every exclusively wooden family participates, including future installed
    # species and audited furniture/roots. Only the DEFAULT native state may be
    # a full cube: double slab variants never become implicit output states.
    # The current export lacks BE metadata, so these exact semantic sets/native
    # IDs exclude wooden block entities. Native qualification separately checks
    # every candidate's actual default state has no block entity.
    block_entities = {"minecraft:chest","minecraft:trapped_chest","minecraft:barrel"}
    block_entities.update(assets.vanilla_tag("minecraft:all_signs"))
    block_entities.update(assets.vanilla_tag("minecraft:wooden_shelves"))
    return sorted(identifier for identifier in wood_sources
                  if entries[identifier].get("fullCube") is True
                  and not entries[identifier].get("hasBlockEntity",False)
                  and identifier not in block_entities)


def preserved_wood(assets, ids):
    """Semantic source shapes; the native runtime block-entity guard overrides.

    Mangrove roots are exposed wood, muddy roots include mud and are excluded.
    The living bamboo plant/sapling, leaves and potted plants are not wooden
    construction material. Bookshelves contain books, beds wool, scaffolding
    string, mixed workstations nonwood ingredients: none are blanket-preserved.
    Hanging signs/shelves are tagged wooden furniture but have block entities;
    runtime must still convert them, exactly like wooden chests/barrels.
    """
    tags = ("minecraft:logs","minecraft:planks","minecraft:bamboo_blocks",
            "minecraft:wooden_buttons","minecraft:wooden_doors","minecraft:wooden_fences",
            "minecraft:wooden_pressure_plates","minecraft:wooden_slabs","minecraft:wooden_stairs",
            "minecraft:wooden_trapdoors","minecraft:wooden_shelves","minecraft:fence_gates",
            "minecraft:all_signs")
    sources = {}
    for tag in tags:
        for identifier in sorted(assets.vanilla_tag(tag)&set(ids)):
            sources.setdefault(identifier,[]).append("#"+tag)
    # These exact native IDs fill semantic-tag gaps. No substring recognition:
    # the mosaic family is bamboo wood; ladder is sticks; composter is wooden
    # slabs; crafting table is planks. Containers are wooden but BE overrides.
    extras = ("minecraft:bamboo_mosaic","minecraft:bamboo_mosaic_slab","minecraft:bamboo_mosaic_stairs",
              "minecraft:ladder","minecraft:composter","minecraft:crafting_table","minecraft:mangrove_roots",
              "minecraft:chest","minecraft:trapped_chest","minecraft:barrel")
    for identifier in extras:
        if identifier in ids:
            sources.setdefault(identifier,[]).append("audited exclusively wooden native block")
    return dict(sorted(sources.items()))


def generate(args):
    assets = Assets(args.client_jar,args.resource_pack)
    registry_raw = args.registry.read_bytes()
    registry = json.loads(registry_raw)
    if isinstance(registry,dict):
        registry = registry.get("blocks",registry.get("entries",registry))
        if isinstance(registry,dict):
            registry = [dict(value,id=key) if isinstance(value,dict) else {"id":key} for key,value in registry.items()]
    entries = {entry if isinstance(entry,str) else entry["id"]:
               {"id":entry} if isinstance(entry,str) else entry for entry in registry}
    if len(entries)!=len(registry):
        raise ValueError("Duplicate block registry IDs")
    palette = Palette(assets)
    wood_sources = preserved_wood(assets,entries)
    candidates = wooden_candidates(assets,entries,wood_sources)
    rejected = [identifier for identifier in candidates if entries[identifier].get("fullCube") is False]
    if rejected:
        raise ValueError(f"Wood candidate is not native full cube: {rejected}")
    if not candidates:
        raise ValueError("Empty wooden candidate set")
    colors = {identifier:palette.color(identifier) for identifier in candidates}
    labs = {identifier:lab(color) for identifier,color in colors.items()}
    mapping, evidence, failures = {}, {}, {}
    for identifier,entry in sorted(entries.items()):
        if identifier in EXCLUDED:
            continue
        visual = entry.get("visualBlock") or identifier
        try:
            color = palette.color(visual)
            sample_lab = lab(color)
            target = min(candidates,key=lambda candidate:(sum((a-b)**2 for a,b in zip(sample_lab,labs[candidate])),candidate))
            mapping[identifier] = target
            evidence[identifier] = dict(palette.evidence[visual],visual_block=visual)
        except (ValueError,KeyError,FileNotFoundError) as error:
            failures[identifier] = str(error)
    if failures:
        raise ValueError("Unresolved registry blocks (generation aborted):\n"+json.dumps(failures,indent=2))
    if set(mapping) != set(entries)-EXCLUDED:
        raise ValueError("Registry coverage mismatch")
    for identifier in candidates:
        # Identical assets can tie. Self retention is permitted only when the
        # registry's effective source texture really is the exact zero-distance
        # candidate colour, never by overriding a different-colour nearest result.
        visual = entries[identifier].get("visualBlock") or identifier
        self_distance = sum((a-b)**2 for a,b in zip(lab(palette.color(visual)),labs[identifier]))
        if self_distance != 0:
            raise ValueError(f"Wood source {identifier} has a nonzero self distance: {self_distance}")
        mapping[identifier] = identifier
    source_digest = hashlib.sha256()
    for name,(label,digest) in sorted(assets.used.items()):
        source_digest.update((name+"\0"+label+"\0"+digest+"\n").encode())
    metadata = {
        "minecraft_version":"26.3", "algorithm":"alpha-and-area-weighted linear-sRGB means; D65 CIELAB CIE76 nearest full wooden cube",
        "tints":"representative plains temperature=0.8 downfall=0.4 actual grass/foliage colormaps; native fixed leaf/water/lily/dye tints; redstone power7; mature stems",
        "state_policy":"weighted random model variants; uniform represented block states; multipart condition frequency; all rendered faces; rotations preserve total area",
        "animation_policy":"all mcmeta frames, duration weighted; alpha-zero pixels ignored",
        "special_renderers":"explicit entity atlas adapters, chest/head UVs, fluids six-face still/flow weighting; labelled atlas approximations for dynamic renderers",
        "exclusion_policy":"only air, cave_air, void_air, light, structure_void, barrier; no opacity/collision/hardness exclusions",
        "registry_source":str(args.registry.resolve()), "registry_sha256":hashlib.sha256(registry_raw).hexdigest(),
        "client_jar":str(args.client_jar.resolve()), "client_jar_sha256":hashlib.file_digest(args.client_jar.open("rb"),"sha256").hexdigest(),
        "resource_packs":[str(pack.resolve()) for pack in args.resource_pack],
        "asset_inputs_sha256":source_digest.hexdigest(), "asset_input_count":len(assets.used),
        "asset_input_counts_by_layer":dict(sorted(Counter(label for label,_ in assets.used.values()).items())),
        "registry_count":len(entries), "mapped_count":len(mapping), "excluded_count":len(set(entries)&EXCLUDED),
        "candidate_count":len(candidates), "methods":dict(sorted(Counter(row["method"] for row in evidence.values()).items())),
        "unresolved_count":0, "candidate_validation":"all exclusively wooden preserved_wood families filtered by native DEFAULT state fullCube=true; exact block-entity exclusions all_signs, wooden_shelves, chest, trapped_chest, barrel; native qualification independently checks every target has no block entity",
        "native_full_cube_candidate_count":sum(entries[key].get("fullCube") is True for key in candidates),
        "tie_policy":"already wooden sources keep their exact zero-distance self match; other equal distances use lexicographic block ID",
        "preserved_wood_count":len(wood_sources),
        "preserved_wood_policy":"explicit vanilla wooden semantic tags plus audited native IDs; preserve any shape only when actual state has no block entity; block entities always convert",
        "preserved_wood_decisions":"include crafting_table, composter, ladder, exposed mangrove_roots and bamboo_mosaic family; exclude muddy roots, living bamboo, saplings, leaves, potted plants, bookshelves/books, beds/wool, scaffolding/string, mixed-material workstations and redstone mechanisms",
        "generator_sha256":hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
    }
    output = {"schema_version":1,"metadata":metadata,
              "candidates":{key:{"rgb":[round(srgb(v),3) for v in colors[key]],"lab":[round(v,6) for v in labs[key]]} for key in candidates},
              "excluded":sorted(set(entries)&EXCLUDED),"preserved_wood":list(wood_sources),
              "preserved_wood_sources":wood_sources,"mapping":mapping,"evidence":evidence}
    args.output.parent.mkdir(parents=True,exist_ok=True)
    args.output.write_text(json.dumps(output,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    if args.evidence:
        args.evidence.parent.mkdir(parents=True,exist_ok=True)
        args.evidence.write_text(json.dumps({"metadata":metadata,"asset_inputs":{name:{"layer":label,"sha256":digest} for name,(label,digest) in sorted(assets.used.items())}},indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"output":str(args.output),"registry":len(entries),"mapped":len(mapping),"excluded":len(output["excluded"]),"candidates":len(candidates),"preserved_wood":len(wood_sources),"methods":metadata["methods"],"asset_layers":metadata["asset_input_counts_by_layer"]},indent=2))


def main():
    parser = argparse.ArgumentParser(description=__doc__,formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--client-jar",type=Path,required=True)
    parser.add_argument("--resource-pack",type=Path,action="append",default=[])
    parser.add_argument("--registry",type=Path,required=True,help="Native registry JSON array of IDs or {id,fullCube,visualBlock} objects")
    parser.add_argument("--output",type=Path,default=ROOT/"src/main/resources/skywars2/woodificator_palette.json")
    parser.add_argument("--evidence",type=Path,help="Optional source-asset hashes report")
    args = parser.parse_args()
    try:
        generate(args)
    except (ValueError,KeyError) as error:
        parser.exit(1,str(error)+"\n")


if __name__ == "__main__":
    main()
