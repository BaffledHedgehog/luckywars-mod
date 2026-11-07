# TMT corrupted glyph use-on adapter — 7 November 2026

Contribution on HEAD `a1a59a04` / qualified parent `62aad5ba`.
Owned changes: `LbcTmtBlockUses.java`, new `TmtGlyphChecks.java`, this note.
No protected Academy/Reroll, glyph owner, source resources, global registrations,
catalog, shared checkpoints, resource pack, or templates were edited.
No compiler/JVM, Gradle, JDT/index, server, subagent or commit.

## Adapter

The genuine `LbcCorruptedGlyphs.DUST` item now participates in the existing
native use-on loop. Every accepted call remains `DUST.useOn(actualContext)` ->
`LbcCorruptedGlyphs.useOn` -> existing `spawn`; no new terrain, placement, damage,
event, scheduler, clone or rewind engine was introduced. Native owner guards,
clicked-face geometry, owner/epoch and one used-hand debit remain authoritative.

Three admission distinctions are explicit:

- `canonical(stack)` checks native Dust **and valid `kind(stack)` before generic
  supported-item dispatch**. The bare registered Dust default is not a source
  glyph; all four authored STRING `glyphkind` variants are valid.
- Both `airCandidate` and `air` explicitly exclude Dust. Adding a use-on factory
  must not advertise a native air-use property that the owner never implements.
- The use-on loop validates the actual **private source prototype** for a Dust
  property through existing `LbcTmtComponents.source` plus the native `kind`
  reader. A forged bare property cannot borrow a valid unrelated outer
  `glyphkind`. This adds no second selector parser or synthetic actor.

Root catalog integration must keep one BLOCK/USED_HAND family with four genuine
kind variants. Handle the bare default as missing selector only after confirming
the valid four source profiles. Do not globally call Dust inert or add an air
event to this family.

Canonical source leaves:

- `lbc:item_defs/lbc/corrupted_dust__bcc0d073f9b6` — BAFOMET;
- `lbc:item_defs/lbc/corrupted_dust_lamashtu__87282b19beaa` — LAMASHTU;
- `lbc:item_defs/lbc/corrupted_dust_belial__608e76e04a9c` — BELIAL;
- `lbc:item_defs/lbc/corrupted_dust_ahriman__15d3f7afa1d5` — AHRIMAN.

## Native scheduled check

Root wiring:

- command `tmt-glyph` -> `TmtGlyphChecks.run(game)`;
- scheduled check hook -> `TmtGlyphChecks.tick(game)`.

The check rolls those actual source leaves and uses genuine
`owner.gameMode.useItemOn` for **four kinds × five physical TMT transports**,
alternating main/offhand. It includes solid top-face, solid east-face and empty
clicked-cell geometry. The resulting actual native display must have the right
kind, owner, epoch, exact position and source model. It waits for a real native
scheduler tick, observes the owner's age advance, then uses existing entity
unload cleanup. It never invokes glyph tick/spawn methods manually.

Each physical stack contains a harmless stick-native sibling with its own private
potion component, a conflicting outer glyphkind, independent outer pig EntityData,
potion, nested container, name and sibling custom data. Checks observe the actual
selected native identity/private kind and native-only component view, exact full
outer-copy restoration, one actual debit, untouched opposite-hand identity and
all components/codec roundtrip after the native factory returns.

An actual MAG field must reject placement without debit or display, after which
the **same physical stack** succeeds when the field clears. A separate real
use-on attempt with a bare Dust property and a valid outer glyphkind must reject
without borrowing that outer selector. Native air-candidate/execution exclusion
is checked inside the selected valid source scope and for bare/default sources.

The fixture reuses existing scoped participant/core/terrain/advancement restoration
and explicitly restores the five physical carriers' used-item statistics. Existing
glyph lifecycle, clone and native rewind behavior remains with the original owner.

Validation in this contribution: Java brace balance, cached 26.3 source confirmation
of Direction/Vec3/ItemDisplay APIs, and `git diff --check`. Compilation and actual
native runtime qualification are **not claimed**; root will run the shared build
and scheduled check before including this component.
