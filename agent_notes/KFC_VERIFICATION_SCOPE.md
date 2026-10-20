# KFC final verification scope

`build/verification/verify_kfc.py` passed for artifact `4902c01a` after both
`kfc-results.json` and `kfc-shared-results.json` reported `passed: true` for that
exact build. The native report contains five completed entries, including two
entries for each of two matches. The shared report contains 19 completed groups.

The script compares all nine complete source loot payloads after the same schema
migration and native item replacements as `tools/import_kfc.py`, verifies their
loot reference closure (including `govno_eaten`), and compares packaged bytes. It
checks 18 current Java sources against the sources JAR, four damage tags, the
Cultus arrow damage type and registered lightning mixins. Cultus retains its own
arrow-style damage type; in that artifact vanilla arrow cooldown was not changed globally.
Later user clarification on 20 October restores the full legacy bypass list,
including arrow/generic; see [DAMAGE_COOLDOWN_PORT.md](DAMAGE_COOLDOWN_PORT.md).

The actual user resource pack is read through `tools/import_lbc_consuming.RP`.
All payload translation keys, item models, model texture files and tooltip sprites
are checked, along with all six Cultus variant models and the `penis` name key.
Neither that pack nor the template is edited.

The UTF-16 build log must contain `BUILD SUCCESSFUL`. Both successful runtime
reports must identify the exact final JAR SHA-256. Only then is the artifact
archived as `skywars2-kfc-<sha8>.jar` and evidence written to
`build/verification/kfc-verification.json`. The main installed mod must retain
SHA-256 `a0c4cb03c4682c99e71e0c197b376f98dc6781e1f153ec7ee61cba67408606a5`.

This evidence does not establish real-client visual correctness or mass-load/MSPT
performance. Runtime evidence is preserved verbatim in the proof file.
