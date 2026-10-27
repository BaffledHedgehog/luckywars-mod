# Current Regeneration Belt — approved19..21 native port

Source contract: [complete Paper/data audit](QUIVER_REGEN_BELT_SOURCE_2026_10_27.md),
actual registered `plugins/luckywars/.../command/Regeneration.java`100..384.
Own files: LbcRegenerationBelt, RegenerationBeltChecks, isolated current
ported/regeneration_belt.json and this note. No shared integration, builds,
servers, git, protected author/RP/source files were changed by this child.

`LbcRegenerationBelt(GameController)`, `tick()`, `stop()`, `forget(ServerPlayer)`
and read-only `rate(ServerPlayer)`; root owns `regenerationBelt()` accessor,
tick/lifecycle/whitelist and check runner. Root supplies the exact native beacon
AABB20 API. Healing calls native `player.heal(float)`, without a potion, sound,
particle, cost, consumption or custom HUD. This naturally reaches the existing
negative-absorption shadow healing hook and native self-Greed behavior. It does
not bypass native health/death/max-health rules.

The exact current source leaf is ordinary accessory pool0/entry2, unchanged
weight10/quality9, original carrier/model/tier/name/lore/PDC. It is the source
`.04` HP per logical game tick per physical stack unit. Actual existing ordinary
accessory reforge supplies its real normal route; the isolated leaf alone is
operator staging. No whole unresolved pool/catalog route was enabled here.

Approved physical-unit counting scans41 slots once every pulse, preserving
mainhand/offhand/nonarmor ordering then head/chest/legs/feet. No cursor or craft
healing; armor and offhand are not counted twice. Actual original PDC container
namespaces luckywars/lws/lucky_wars and namespaced/plain minecraft field aliases,
double/byte/string typing, slot predicates, first same-id group type/value,
stackability and ADD/SET/MULT sequencing remain. Nonarmor and armor group passes
remain ordered and distinct. Only actual current heall item payloads participate;
this is not an implementation of unrelated Paper `/regen` commands/mob caches.
Live inventory is recomputed each tick, including programmatic count/item changes.

Each forbidden-dimension owner is skipped independently. Imprinted never returns
from the whole server loop; ordinary Nexus item policy remains authoritative.
Input stun/ice do not block the passive; shared Time Stop does. Healing suppresses
only exact source stopper_magic/native replacement beacons in Paper AABB20,
including diagonal corners outside spherical20. Generic nomagic/antimagic aura
does not suppress it. The separate source task retains its spherical stopper gate.

Every100 unpaused logical ticks, source container/hand trigger plus clear0 full
native inventory/equipment/craft/cursor count>=10 and native objective `y`<=-100000
grant the original impossible regeneration_belt task. The score is read literally;
no invented coordinate threshold replaces it. Clear0 never consumes anything.
Existing native levelup3/page8000/slot2 source gate and+.15 multiplied total max
health remain; no GUI, kit or new attribute scheme was introduced.

Old Belt remains source-only: its heall_old-only legacy-mode scheduled100-tick
grant is native Regeneration20t/amp2/hidden, with no PDC healing and no alias to
current. No legacy leaf/runtime/mode or unresolved whole legacy pool was enabled.

Prepared native checks use real registered memory connections and current server
clock, latest actual teleport packet ACK/loaded handling, exact snapshots and
forceWritable score restoration. They cover physical main/offhand/armor units,
live programmatic mutation, PDC groups/slots, excluded cursor/craft healing,
actual original normal accessory weighted reforge, ordinary healing/no potion,
true native input stun, nomagic independence,
diagonal AABB suppression, per-player Time Stop, self-Greed, negative-absorption
shadowHP, a real Imprinted transfer with independent other-player healing,
actual100-tick source full-clear collection and existing levelup3 attribute.
No manual player/effect/world ticks. Root owns integrated runtime qualification;
no runtime pass, physical client audio/pixels or peakTPS claim is made here.

Frozen initial integration SHA256:

- LbcRegenerationBelt.java: `70eea84d23e4e839170d8ac6e3ae3c3600adaca8920083e534abe19561bd79f6`
- RegenerationBeltChecks.java: `317a699140c2e635463394aa988b4678ecbe335dc8b35a65113d60c44831902b`
