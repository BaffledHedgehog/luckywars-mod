# Gaycaster acid source review, 25 October 2026

The user asked whether the green acid effect uses the correct variant. The
existing production dispatch matches both source variants and is preserved.

Read-only source root:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.

- Ordinary `lbc/function/workingitems/gaycaster/ray/impact/entity/green.mcfunction`
  and `impact/block/green.mcfunction` call `lbcr:raycast_preloop11` while facing
  upward. The latter creates six ordinary `raycastacid` charges, lifetime300,
  with player, slowraycast, shield_blockable2 and magic tags. The source ordinary
  acid trajectory keeps its physical gravity/reactivity. `LbcGaycaster` uses
  the shared six-charge ordinary acid volley, not the big ordinary or awakened
  seed. Block impacts retain their source two-block launch offset.
- Academy `ray/cast2_green_entity.mcfunction` and `cast2_green_block.mcfunction`
  call `workingitems/awaken_tuntija_wands/acidacid/click2.mcfunction`: one
  upward `great_acid` seed, lifetime500. `LbcGaycasterAcademy` already calls the
  corresponding shared awakened seed. Its first collision releases the source
  forty500-tick children, with existing native gravity/reactivity on the children.

`GaycasterChecks` now asserts ordinary charge count, variant tags, shield tag,
300-tick lifetime and active gravity. The existing Academy fixture asserts
one500-tick straight seed and forty children at actual native block collision.
Root owns combined compilation and native qualification. No production acid
variant, author resource pack or legacy source is changed by this review.

Final root qualification: build `b4cdfa8d`,148 recorded results across two actual
mode1 matches and actual stop cleanup. See [combined review](GAMEPLAY_REVIEW_2026_10_25.md)
for the tested artifact, deployment state and verification boundaries.
