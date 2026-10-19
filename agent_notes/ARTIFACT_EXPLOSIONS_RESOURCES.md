# Mystical artifact explosion resources

The native item payload for the explosive mystical artifact is imported from
`LBC/data/lbc/loot_table/artifact.json`, selecting the entry marked
`unresolved_artifact:1`, `mystical:1`, and `type:2`. The importer changes only
the server item id to `skywars2:mystical_artifact`; the source name, lore,
formatting, model, tooltip style, and custom data are retained through the
existing loot schema migration. The result is
`data/lbc/loot_table/ported/mystical_artifact_explosions.json`.

`data/minecraft/tags/entity_type/unstable.json` carries the legacy LBC tag with
`replace:false` and all 11 original values. The design source checker compares
both resources against the read-only legacy datapack. Gradle includes them in
the packaged resource allowlist.

Verification run: `python tools/import_lbc_weapons.py` and
`python tools/check_design_sources.py` completed successfully. No build or
server was run.
