# Resource contract: Tiny Electro и Acid

Обновлённый статус Tiny Electro: [WANDS_ELECTRO_PORT.md](WANDS_ELECTRO_PORT.md). Описанное ниже «пока только исследован» — исторический срез при создании этой заметки; текущие исходники содержат Electro item import/ability. Это не утверждение о runtime PASS или установке.

Источник обоих items — OLD LBC `data/lbc/loot_table/random_sauvojen_wand_tiny.json`; каждый source entry имеет proxy `minecraft:carrot_on_a_stick` и weight 1. Tiny Acid импортирован в `data/lbc/loot_table/ported/tiny_acid_wand.json` с сохранением исходных компонентов. Tiny Electro пока только исследован: импорт и портирование механики не заявляются.

| Intended server ID | Source marker | Cost | Model | Name key |
| --- | --- | ---: | --- | --- |
| `skywars2:tiny_electro_wand` | `stelectro_tiny:1` | 100 mana | `lbc:st/electro_tiny` | `wand_electro_tiny` |
| `skywars2:tiny_acid_wand` | `stacid_tiny:1` | 63 mana | `lbc:st/acid_tiny` | `wand_acid_tiny` |

Electro payload uses tier `lbc:tier_a`; Acid uses `lbc:tier_b`. Both preserve `craftusable`, `transformable`, `staff`, `magicwand` and their identity marker; Electro additionally has `cyberset:1`. Lore references dynamic score keys `tiny_electro_wand_mana_use` / `tiny_acid_wand_mana_use` plus shared `type`, `staff`, `mana_on_activate` keys.

The live RP is read-only and already contains item definitions and models for `electro_tiny` and `acid_tiny`, plus all name/lore/shared RU and EN keys. No language or template changes are needed. Source Electro lore says it combines with its copy to upgrade and can combine with an iron sword, full iron armor, shield, stick or bow; it advertises a 60% chance not to consume the wand. The narrow `craft_ground.mcfunction` routes confirm: two Electro rods create the upgraded Electro wand; one can craft a Cyber Katana with an iron sword, Electro Shield with a shield lacking `energy_shield`, Railgun with a bow lacking `railgun_bow`, or Techno full iron armor. Those four recipe handlers consume the Electro wand on `chance40`, consistent with a 60% keep chance; sword/shield/bow/armor input items are consumed. There is no stick recipe in the inspected craft-ground handlers, so that lore item remains unresolved here.

Acid lore says two copies upgrade, and nearby items can be broken into corrupted/demonic dust with a 20% drop chance without consuming the wand. Source checks confirm two Acid rods produce the upgraded `stacid_wand`. Separately, `craft_ground.mcfunction` detects a nearby dropped item with `Thrower` and invokes `crafted_dust`; it excludes the Acid rod itself and existing `corrupted_dust`, rolls `chance20`, and reduces the nearby eligible item stack by one. This is a narrow source-path confirmation, not a claim that all possible item states or runtime craft paths have been tested.

This is an identification/design note only: no payload import or runtime mechanic is claimed complete.
