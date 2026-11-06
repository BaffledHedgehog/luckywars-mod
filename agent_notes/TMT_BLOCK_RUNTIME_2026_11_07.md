# TMT native payload components and use-on — 7 November 2026

Component verified, **not installed**. Full TMT is still incomplete.

All five transports forward a genuine game-mode use-on context to the selected
registered native BlockItem, marker egg, special egg or Pumpkinator factory. Native
owners retain placement geometry, build permissions, suppression and accepted-hand
debit. Failed placement releases the TMT event guard so a subsequent admitted
input is not locked out. Legacy Dirter/follow-up shortcuts defer TMT to this one
coordinator; ordinary legacy inputs remain unchanged. Fluid activation uses the
source native factory and its real player raycast, without a fabricated block hit.

Private properties retain native entity, bucket, block entity/state, potion,
duration, firework and container payloads. Only authored differences from finalized
native defaults are serialized. These components are projected onto the actual
stack inside the existing private property scope, then restored. Native mutations
belong to that property's private payload. Whole-item refunds restore the original
components; captured producer copies retain their resolved native context. Generated
stats, equipment, food, appearance and actual ammunition are not replaced by the
payload projection. No arbitrary function executor was introduced.

The sampler and direct-use domain capture these payload differences. Native ammo
descriptors use the same resolved source snapshots. Component codecs, current
registry lookup and existing native factories are shared, without separate
implementations of their gameplay mechanics.

Sealed internal candidates on full `4e71e440` / `970e74af`:

- Test `fb24be1e62f7c2a36b4779b42ef15a5820a4cd27e013ab609244f2fe69334eb5`.
- Production `e99d79ede6d7accb6994bb84cb6b0c79500561265bbf0554ab1a43defd524c15`.
- `build/verification/tmt-block-2026-11-07/component-verification.json`.
- Thirteen explicit Java families, zero resources; 10,325 other source inputs and
  282 author paths preserved. Fresh single-worker 512 MiB compile; protocol hotfix
  retained byte-exact. Main/libs, author resourcepack and concurrent HUD unchanged.

Nine actual mode-1 matches pass: `tmt-block-use`, `tmt-catalog`, `tmt-native-use`,
`tmt-held`, `tmt-ammo`, `totems`, `imba-followup`, `imba-placeables`, `imba-utilities`.
Native evidence includes two distinct factory outputs/debits from one real click,
an authored Vex payload and original HP, exact outer item/potion restoration,
codec/refund/exception paths, actual BlockItem placement with one shared terrain
job, positive MAG denial and subsequent accepted input. The ordinary source owners
and prior TMT held/ammo/totem checks continue to pass.

Earlier diagnostic failures were fixture issues: Syringe unequip observation must
allow its existing five-tick cadence, and cleanup must snapshot the native entity
collection before discarding its members. Both final checks are retained; the
failed reports are not counted as passes. Final report verifies normal shutdown,
exact restoration, no failures/cleanup errors/live child/retained locks.

Catalog 747 families / 1,361 variants is not proof of full coverage. Dropped/intake,
remaining native domains, final acquisition and release are pending. No physical
client rendering or peak-TPS claim.
