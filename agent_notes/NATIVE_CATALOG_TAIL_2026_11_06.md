# Final ready-catalog carrier identities — qualified, installed (6 November 2026)

Four existing ready outcomes now use their own registered Polymer identities:

| Existing ready loot | Native identity | Existing ability authority |
| --- | --- | --- |
| `lbc:ported/bucket_head` | `skywars2:bucket_head` | ThrownUtilities helmet replacement |
| `lbc:ported/moisei_boots` | `skywars2:moisei_boots` | MoiseiBoots sprint/terrain/TNT |
| `lbc:ported/op_hoe` | `skywars2:op_hoe` | Native Netherite hoe; ordinary tool pools |
| `lbc:ported/malevich` | `skywars2:malevich` | Malevich live-slot/ground conversion |

The ordinary OP hoe is separate from the already-native IMBA hoe. It joins the
existing native tool family. The other three use the common source prototype
reader; no second equipment, terrain, inventory conversion, timer or NBT engine
was introduced. Existing raw component-marked stacks retain their handlers.

Canonical definitions change only identity. Bucket Head's referenced base was
independently verified to be an empty leather-helmet entry and inlined into the
same canonical table; all its original modifiers remain. The shared empty base
itself stays unchanged. Ready aliases, weights/qualities, conditions, counts,
names, lore, tooltip styles, equipment assets, attributes, enchants and custom
data stay authored. The Malevich wrapper's luck/Zaika conditions stay intact.

## Shared native defaults and compatibility

The prototype reader now uses Minecraft26.3's own leather armor builders and
Netherite hoe builder, followed by the original component overrides. This keeps
native enchantability, repairability, durability, armor/equippable machinery and
hoe interactions. Seven additive tag overlays inherit every observed direct
vanilla/Fabric/project carrier membership and the resulting complete tag graph;
existing tag entries remain. OP hoe retains Efficiency255, Unbreaking255,
Fortune25 and Mending255. No resource-pack asset or translation changes were
needed: existing models/equipment assets and RU/EN keys were checked read-only.

Malevich keeps the canonical per-output uniform1..100 attribute function. It is
never sampled or frozen during item registration. The native default contains
all fixed authored components and functional PDC, while real canonical loot
adds its fresh generated attribute. Gameplay already uses the PDC, not this
attribute. Legacy toggle_tooltips is reflected in the native default so its
attribute remains hidden. The shared prototype reader explicitly rejects any
other unsupported generated-attribute use rather than silently discarding it.

Thrown Bucket accepts the canonical native helmet; its shared equipment helper
still publishes the exact previous helmet on the ground with pickup delay0
before replacement. Ordinary Pumpkin's path is unchanged. The original vanilla
helmet fixture, provenance and pickup-statistic witnesses stay vanilla.

A pre-existing randomized clone fixture placed its selected clone only four
blocks away, within the possible nested x2*x4 scatter. A sibling could therefore
paint its supposedly-outside control cell. The fixture now isolates that clone
eight blocks away, retaining the eight-body, lineage and AIR-only boundary
assertions. Production spread/geometry is unchanged. The rejected candidate
logs and both test corrections are retained separately from final evidence.

## Qualification and installation

Exact test artifact `db8dc925`, production `e1e72d33`. Twelve isolated matches
passed on the same test SHA: eight actual SkyWars1 matches and four BedWars3
matches. These cover full independent original-vanilla-source component/count
oracles, native defaults and inherited tags, Polymer authoritative roundtrips,
server ItemStack codecs, fresh Malevich RNG, actual armor use, actual OP hoe
tilling, the full native-tools and shared projectile-factory regressions,
Malevich/Moisei/Bucket gameplay, ordinary weighted tool pools and Bucket's native
coordinator rewind. Each cohort shut down normally and restored its fixture;
temporary BedWars maps and auxiliary snapshots were restored/removed.

Only12 fresh owned Java families and11 resources were overlaid on each qualified
parent archive. 10278 other source inputs and282 author
working paths remained byte-exact. Binary linkage and every archive entry were
checked. Protected Academy/source/template work is not shipped. Production
retains exclusion of map-authoring/deletion classes. Gradle used512MiB/one worker,
the isolated fixture2GiB, sequentially with free-RAM checks and no persistent
daemon. Main/both build outputs were installed with the exact qualified hashes;
the main server was not started or restarted. No push.

The bounded audit now has776/776 native first outcomes and no vanilla first
outcome. This is ONLY the direct ready-root audit, not a completion percentage
for logical items or the mod. Full TMT-last, custom/mixed/type pools, modes2/4,
dependent kits/skill2, protected Academy, full-world rewind/release, physical
client, peakTPS and disk-restart qualification remain separate. No gate was
opened merely to change this count.

Evidence: `build/verification/native-catalog-tail-2026-11-06/qualification.json`, `main-install.json`,
`canonical-definition-proof.json`, `tag-source-audit.json`,
`presentation-proof.json`, `remaining-catalog-proof.json`, `linkage-proof.json`,
`package-proof.json`, the sealed native logs/results and `local-commit.json`.
