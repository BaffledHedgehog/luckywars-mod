# Suppression audit: spells, guns and effects (27 October 2026)

Read-only source inspection. No Java/resources/build/server/runtime changes, no
external browsing. This new English note is the only file written by this audit.
Read the applicable root and agent_notes rules and RESOURCE_PACK.md; the actual
resource pack and original server remained read-only. Findings describe the
current source tree, not a runtime proof or identification of an installed JAR.

All Java references below are relative to
`src/main/java/me/skywars2/game/`. A `USER DECISION` is a concrete policy question,
not permission to implement it or a claim that legacy omission proves a bug.

## Most useful decisions

| ID | Family / classification | Stage and current behavior | Exact evidence / bounded decision |
| --- | --- | --- | --- |
| S1 | Tiny Blind/Air/Stone/Teleport/Acid, MAGIC | Launch is admitted by wand mana/magic policy, but the instantaneous 35-block ray can enter an anti-magic sphere and execute its terminal there. No actor exists for later beacon deletion. | `LbcWands.java:1392`, `:1403`, `:1434`, `:1474`; `firstHit`/common target helpers do not provide a hidden suppression guard (`:849`, `:1483`). USER DECISION: should these rays stop at a magic boundary, or merely reject terminal effects inside it? Include Tiny Ruby/Terra below separately because their geometry differs. |
| S2 | AK-47, TECH | Firing inside a mechanism field fails, but a bullet ray fired outside can cross a field, hit an entity inside, or destroy a block inside/beyond it. The bullet is a synchronous ray, so central entity deletion cannot intercept it. | `LbcAk47.java:73`, `:112`, `:118`, `:125`, `:133`. USER DECISION: should mechanism fields clip the ray as Magnum does, or only disable the gun at its origin? Finite/infinite variants share `nativeRay`; do not fix one alias alone. |
| S3 | Ruby destructive rays, Stone ray/spikes, concentrated Fire, MAGIC | Owner activation is gated; several instantaneous paths never consult suppression along the ray or at the terminal. They can alter terrain inside a field while the owner stands outside it. Ruby hole also pulls/damages targets without a suppression-specific target guard. | Tiny Ruby `LbcWands.java:907`; tiny Terra `:948`; Fire `:886`; Ruby Academy hole `LbcRuby.java:184`, `:201`, cutting `:226`; Stone `LbcStone.java:141`, `:198`. USER DECISION: boundary clipping and/or per-cell/terminal protection. Preserve the already approved Ruby ability to cut ordinarily indestructible blocks; that says nothing about suppression. |
| S4 | Ordinary/Academy/awakened acid, MAGIC actor | Acid actors carry `magic`, so Magic Stopper does centrally delete them. However the anti-magic artifact is only queried by explicit callers, and shared acid has no artifact flight guard. An acid actor outside a beacon sphere can also poison/burn a target just inside it through its radius; target selection has no suppression check. | Tags `LbcClouds.java:429`, `:460`; `wandAcidTarget :375`; ticking `:530`; aura `:612`; awakened aura `:632`. Central beacon deletion `LbcMagic.java:522`, `:973` is real coverage, so this is NOT a claim that beacon deletion is absent. USER DECISION: artifact should reject flight/aura, and target-specific magic protection should apply across sphere edges? Do not alter the already approved queued rifle persistence. |
| S5 | Blood / Confuse and their grimoires, MAGIC | Owner/body and eye admission are checked, but nearby victims use only role/team/Greed eligibility. A caster outside a ward can apply Blood damage/Wither/Hunger or Confuse Slowness/Blindness to a victim within the ward. | `LbcBloodConfuse.java:43`, `:65`, `:69`, `:77`, `:110`. Common `LbcWands.target :1493` has no suppression guard; potion rejection only represents cleanse artifacts/Greed, not anti-magic (`LbcMagic.java:325`, `:331`). USER DECISION: reject hostile area payloads at protected target positions? |
| S6 | Released Electro/Tesla arcs, NONMAGICAL electricity; source carrier TECH tag | Ignoring MAGIC suppression during released flight is explicitly approved. Separately, ordinary player ElectroTrace constructors default `mechanism=false`; their flight is not mechanism-jammed, unlike Magnum/NPC traces. Source Tiny Electro carries `mecha`, and source mechanism stopper kills such carriers. | `LbcWands.java:331`, `:343`, `:1025`, `:1093`, `:1101`, `:1270`; actual old `workingitems/sauvojen_staffs_tiny/electro_end.mcfunction:2` and `other/stopper.mcfunction:4`. USER DECISION only about mechanism jammer: should the source mecha carrier classification extend to player Tiny/ordinary/Tesla/library arcs? Do NOT propose reinstating magic-flight deletion. |

The examples above are independent of passive armor/accessories, food, melee,
hostile encounter engines and random drop engines, which are outside this audit.

## Additional policy decisions, with lower certainty about intended classification

- **S7 — moving magic actors can enter a sphere and resolve a hit in the same
  step.** Air, Stone, Teleport and Ruby inspect suppression at the current actor
  position, then trace to the next position/hit without a swept suppression
  fraction. References: `LbcAir.java:148`, `LbcStone.java:166`,
  `LbcTeleport.java:164`, `LbcRuby.java:149`; common geometry
  `LbcWands.java:856` only clips blocks/entities. Central `magic` deletion can
  remove a surviving actor later, but cannot undo a terminal already executed
  during the entry step. USER DECISION: make suppression a swept boundary for
  these MAGIC actors, or retain point admission? This is a boundary/ordering
  question, not a request to erase every projectile.
- **S8 — rideable cloud arrow.** `LbcTeleport.java:132` gives the arrow its cloud
  identity without `magic`; `tickClouds :279` validates owner, epoch, chunks and
  pause but checks neither magic field nor mechanism field. It keeps its rider
  support behavior inside both fields. Arrow type is absent from
  `data/minecraft/tags/entity_type/magic_entities.json`. USER DECISION: is this a
  persistent magical transport after launch, an ordinary physical arrow, or a
  protected exception? Boltbolt Wind Racer reuses this actor with extra lightning
  (`:292`), so do not classify it from the display name alone.
- **S9 — SLR/rifle classification and ray protection.** Current SLR activation
  uses `canFire` and MAGIC suppression (`LbcShooters.java:592`, `:780`). Its
  segmented ray (`:627`) has no field boundary check, and impacts (`:671`) can
  damage, debuff, stun or explode inside either field. It is not currently a
  TECH launcher. USER DECISION: keep MAGIC classification and add target/path
  protection, or classify the rifle as TECH and use mechanism policy? Recoil,
  modes, ammunition and cooldown do not need redesign to answer this.
- **S10 — ongoing custom statuses.** Fear/lookback/piss keep acting after their
  victim enters anti-magic; only player twitching explicitly pauses there.
  `LbcPsycho.java:366`, `:381`, `:395`, `:398`, `:420`. Frozen/cold state also
  continues native holding/ice restoration under anti-magic
  (`LbcFrost.java:71`, `:192`, `:210`, `:253`), while clear-all artifacts remain
  respected. SLR stun similarly keeps forcing input under either field
  (`LbcShooters.java:701`, `:752`). These states can have several physical and
  magical producers; USER DECISION: which effects are magical after application,
  and should entry pause, cleanse, or leave them running? A blanket guard in a
  shared state store would silently affect unrelated producers.
- **S11 — signed environmental effects, classification unresolved.**
  `SignedEnvironmentalEffects.protectedTarget :44` handles Greed, roles and
  pause, not either suppression field. Fire Resistance II+ terrain/aura (`:86`),
  extended Conduit water (`:108`), Infested admission/queued release (`:125`,
  `:131`), Wind Charged death push (`:150`, `:157`), Ooze/Weave consequences
  (`:171`, `:178`) therefore continue inside fields. `EffectTerrain.java:114`,
  `:126` repeats that same protection policy for delayed fire/water work;
  `LbcColors.java:278` covers legacy enhanced-water cases through the same
  protection API. USER DECISION: should already applied vanilla/signed potion
  consequences count as MAGIC or remain physical/vanilla effects? The custom
  system also works without an active match; adding a match-only blanket rule
  would be a separate behavior change. WILD authorship does not answer immunity.

## Central protection actually present

- `LbcMagic.suppressedAt :410` means active anti-magic artifact sphere R20 OR
  Magic Stopper sphere R20 in the arena. `suppressionFraction :431` can locate
  entry along a segment. `mechanismSuppressedAt :456` and
  `mechanismSuppressionFraction :459` are separate mechanism-only queries.
- `LbcMagic.magical :522` recognizes the `minecraft:magic_entities` type tag,
  entity `magic` tag, and applicable `summactivated` mobs. Beacon pulse
  `:973` actually kills these actors inside its sphere. This is not artifact
  aura deletion, a generic DamageSource immunity, or a native potion cleanse.
- Mechanism pulse uses `mechanical :999`; source `mecha` entities are covered,
  with protected role exclusions. Virtual traces/rays have no Entity and cannot
  be covered by this deletion predicate.
- `LbcMagic.rejectEffect :325` / `rejectCustomEffect :331` are clear-debuff /
  clear-all artifact and Greed policy. Their presence does not imply anti-magic
  immunity; `cleansingTypes :315` actually disables those artifact passives
  within the Magic Stopper field.
- `LbcWands.projectileTarget :1483`, `target :1493`, `validRoleTarget :1501`,
  and `firstHit :849` supply role/team/Greed/native-hitbox policy. They do not
  silently add MAGIC/TECH suppression, so each producer needs classification.

## Approved exceptions and false positives excluded

1. Released electricity ignoring anti-magic is an explicit user ruling in
   `AGENTS.md:4687` (quoted user text starts `:4689`) and
   `WANDS_ELECTRO_PORT.md:85`, `TESLA_PORT.md:17`.
   Wand activation still pays through `LbcMana.trySpendWand`; do not gate released
   Electro, Erecto or Tesla by MAGIC just because they are wand-produced.
2. Hohlopitek is explicitly mechanism-only (root `AGENTS.md:2847` onward).
   Its shared TECH search in `LbcExasperato.java:275`, `:288`, `:317`, `:320`
   already distinguishes magic and technology. No proposed MAGIC gate.
3. Queued acid rifle preserves its queued count while suppression pauses
   discharge: `LbcAwakenedAcid.java:78` checks cleanse/ownership and `:83`
   pauses without decrementing; `:89` decrements only an emitted shot. This is
   not an ungated cast or a queue that should be deleted on beacon entry.
4. Personal `nomagic` counter is not the same mechanism as anti-magic artifact
   or stopper: `NOMAGIC_PORT.md` and `LbcWands.java:432` state its resource-cycle
   meaning. Do not insert it as a universal spell-disable rule.
5. WILD deliberately broadens caster/allied victim policy and preserves credit;
   it does not mean immunity bypass. Conversely, an allowed WILD payload does
   not itself prove that anti-magic should block that physical consequence.
6. Source gun/arrow tags matter. Govnomet explicitly has origin MAGIC + TECH
   admission but neither flight tag (`LbcGovnomet.java:102`, `:113`); treating
   its untagged physical flight as a missing deletion gate would invent policy.
   Pine/phantom arrows (`LbcShooters.java:877`, `:892`) are likewise native
   arrows without a magic tag; no blanket projectile disappearance is proposed.
7. Existing native fireballs/potions/clouds/fangs are in the central magic type
   tag, and acid/Air/Stone actors carry explicit `magic` tags. Lexical absence
   of a local `suppressed` call is not enough to call their beacon deletion
   missing. Distinguish target-edge effects and artifact policy as S4/S7 do.

## Bounded coverage and shared aliases

Inspected actual dispatch/admission, common geometry/target helpers, central
deletion types/tags and the specific ongoing/impact paths cited above. This is
bounded static coverage; it does not claim every installed item behavior was
executed or every original source omission was discovered.

- Wand dispatcher `LbcWands.java:441` routes ordinary/Academy identities into
  their common engines rather than independently implementing renamed items.
  Tiny identities are live enum entries (`:233` onward), not dead private
  helpers. `extraTiny :799` routes the extra tiny grimoires to those same rays.
- Elemental/shaping/corrosive/awakened grimoires use common engines via
  `LbcElementalGrimoires.java:201`; generic library and fixed/random identities
  use `LbcLibrarySpells.java:75`, `:90`; extra IDs route through
  `LbcExtraGrimoires.java:13`. Their own consumption admission includes MAGIC
  owner/eye protection. Any approved payload fix belongs in the shared engine.
- Positive controls: Boltbolt/Lightning Bolts have owner, target and swept magic
  checks (`LbcBolts.java:244`, `:280`, `:341`, `:404`, `:648`); Homing/Curse
  local target predicate already rejects target eye suppression
  (`LbcHoming.java:125`) and flight uses `:302`. Consequently a curse victim
  entering anti-magic is not an unguarded canonical curse case.
- Psycho rays/charges use target and swept magic checks (`LbcPsycho.java:180`,
  `:325`, `:499`), separate from ongoing affliction S10. Ice/Prism have actor
  and impact/target guards (`LbcIceWand.java:169`, `:196`, `:236`;
  `LbcPrism.java:111`, `:157`, `:188`).
- Evoker fangs validate owner, fang and target positions (`LbcEvokerBass.java:127`);
  Lifestealer clips swept suppression (`LbcLifestealer.java:199`). Void Arcanum
  checks owners, hole point, targets, movement and terrain
  (`LbcVoidArcanum.java:138`, `:148`, `:248`, `:300`, `:326`).
- Danmaku checks input, queued release, card actor position and target effect
  (`LbcDanmaku.java:95`, `:149`, `:164`, `:211`). Sauvojen rejects suppressed
  drone position (`LbcSauvojen.java:134`). Gaycaster and Academy hazards have
  explicit activation/point checks (`LbcGaycaster.java:40`, `:45`, `:85`;
  `LbcGaycasterAcademy.java:168`). These are observed controls, not a proof that
  every radius/edge case in these larger families is covered.
- Exasperato shared search/charge paths have explicit policy checks
  (`LbcExasperato.java:317`, `:456`, `:515`, `:527`). Duplicating fields and
  terrain explicitly query suppression (`LbcDuplicating.java:491`, `:497`,
  `:519`, `:574`). Consuming queue/area paths query it
  (`LbcConsuming.java:150`, `:202`, `:503`).
- Magnum already clips mechanism rays (`LbcMagnum.java:178`); Laser Minigun
  deliberately uses MAGIC admission and clips MAGIC rays
  (`LbcLaserMinigun.java:179`, `:202`). Helicopter weapons are user-approved
  TECH and use a shared origin gate (`LbcShooters.java:326`, `:357`); hostile
  helicopter encounters were not audited.
- Shared chained Raid Omen124 status checks BOTH stopper types before activation
  and MAGIC at victims (`LbcChainLightningStatus.java:61`, `:69`); Babylon
  secondary status distinguishes TECH vs MAGIC on application and pulse
  (`LbcBabylonEffects.java:82`, `:97`). The corresponding counters are not raw
  bypasses. Frost/Psycho/stun ongoing decisions are specifically S10.

No build or runtime tests were appropriate for this read-only decision audit.
Before implementation, obtain the user's classification and stage choices;
then verify the chosen common producers, boundary impacts and target effects
with dedicated native scenarios, including the approved exceptions above.
