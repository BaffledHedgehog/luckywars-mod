# Razor Pine resource import

The bounded import is implemented by `tools/import_lbc_weapons.py --razorpine`. It selects the single `lbc:razorpine_with_leaves` reference in the old `transformables_shooter.json`, resolves that source table, and emits only the Razor Pine item entry from its first pool while preserving the provider's second pool of 16 wheat seeds. The item keeps its source model, tier tooltip, translated name and lore components, formatting, and all custom-data flags, including `razorpine:1` used by the crafting recipe.

The item source is `minecraft:carrot_on_a_stick`, with no explicit durability component or custom durability value. Any vanilla proxy durability therefore comes from the Minecraft item default (25 uses); the source does not specify a different cost. The gun's ammunition is foliage consumed during firing, not weapon durability: the old ammo path consumes one best-ammo item (spruce leaves/saplings) or one regular-ammo item before its six-volley firing window; no mana cost is specified.

The importer also brings across the two source ammo tags. `razor_ammo.json` replaces the old `#tag_replacements:flowers` reference with the equivalent native `#minecraft:flowers` tag; Minecraft 26.3 defines that item tag. The repeated pale-moss entries in the legacy broad-ammo tag are retained verbatim. Both ammo tags are included in `build.gradle`.

The resource pack already contains `razorpine` and lore translation keys in both `ru_ru.json` and `en_us.json`, with no duplicate keys detected. Its `assets/lbc/items/razorpine.json` points to `lbc:item/razorpine`, and that model exists. No resource-pack template additions are needed.
