# Next NPC special-drop acquisitions: bounded source audit (2026-10-23)

Read-only audit; no code/build/server/RP changes. Three user sources untouched.
Roots: OLD=E:/servers_mew/cringe1_21_4_skywars; NEW=E:/servers_mew/skywars2_last/luckywars-mod.
Legacy datapack base D=OLD/world/datapacksdisabled/LBC/data/lbc; Paper P=OLD/plugins/luckywars/src/main/java/me/luckywars/swrg/SwrgNativeService.java (actual class name, not PaperSwrgNativeService).

## Recommendation

Do not promise 2-3 closed NPC leaves from this list. Academy acquisition is a small closure/hardening of an EXISTING implementation; Transformator is the best one substantive new closure, after choosing its divergent attack semantics. Stalin is another substantive encounter requiring gun-reward and communism-target closure. Mom + Otchim are inseparable from Absurd Child and its lucky-block cycling, not two cheap standalone summons. No broad pools should be enabled.

## Exact acquisition presentation

All five base tokens are repeating_command_block, count1, yellow nonitalic translated name, white nonitalic trap_desc lore, custom_data {FLAG:1,drop:1}. D/loot_table/barriersnottraps.json Academy: FLAG magic_academy, name drop_academy, tier_q, model lbc:drop_not_trap (weight30 quality10). D/loot_table/loliluckyblock.json others: weight10, tier_ff; stalin/drop_stalin/drop_boss; mom/drop_mom/drop_not_trap; transformator/drop_transformator/drop_trap; otchim/drop_otchim/drop_boss. Four loli entries exclude final_luck_modifier=7 and this-player zaika inventory. Keep variant sources separate: imba/barriersnottraps.json Academy adds imba:1; imba/spawnegg.json contains a Mom acquisition variant. This audit does not authorize replacing those pools or flattening their conditions.

D/function/drops/specialdrops.mcfunction dispatches these flags. Transformator uniquely aligns XYZ then offsets +.5,+.5,+.5 and -.6Y. Stalin adds +.5Y inside its summon. Do not infer a universal centered spawn.

## Academy: already supported, qualify the acquisition instead of duplicate implementation

D/function/drops/magic_academy.mcfunction places lbc:magic_academy at (-6,0,-6), then teleports nearest nonspectator within12 to source+(0,1,-2). Token variant drops/magic_academy_token.mcfunction places at (-6,-1,-6), teleport sound and350 witch particles, kills the whole item entity. Paper P:4284 runNativeMagicAcademyDrop matches base position, nearest target, consumes one unit.
NEW/src/main/java/me/skywars2/game/LbcAcademy.java:340-419 ALREADY recognizes academy_token and magic_academy, queues template rows, consumes one base unit, retains token whole-stack removal, owns template entities and cleanup. It also implements tagged magic_academy NPC interaction and24 wand trial paths (interact around138). Thus claiming this NPC subsystem wholly unported is false. Native kit academy token and ported/academy_token loot already exist.
Needed bounded leaf: exact base token registration + ported loot, native resource/interaction/full acquisition regression using existing service. Audit placement BEFORE debit, resident chunk checks (current hasChunk versus getChunkNow), Time Stop pause, epoch/unload/quiet cleanup and teleport before asynchronously completed building. The current row queue and teleport timing differ from atomic legacy placement. Preserve existing academy upgrades; source category must not silently offer unsupported rewards. Academy is not a reforge NPC.

## Transformator: one substantive new leaf

D/function/drops/transformator.mcfunction: adult zombie, transformator+lucky_mob, visible gold translated name plus gray ' Nightmare23 Birthday', NoAI, silent, persistent,500HP, knockback resistance100, invisible amplifier1/2000000 ticks, zero helmet drop; repeating_command_block helmet model lbc:transformator. Separate centered billboard text tr_text at +3Y, view_range2, see_through, matching text. No reward table or right-click/reforge menu is defined. Existing NEW reforge classes/bolt categories do not implement this encounter; name similarity is not functional reuse.
D/function/mobs_5t_lbc.mcfunction runs other/transformator each5ticks unless stopper within20. Each pass:10% seven electro rays; independent5% instant-health I and II; <=490 warning50%, <=480 smoke, <=470 cure sound50% volume1, <=46050% volume2, <=450 fire in3cube replacing airs, <=440 other/transformator_explode. Explosion: native fireball at +2Y, Motion Y-10, power12, dirt at -1Y, kill tr_text within10 and remove actor.
P:5543 spawn and5937 tick reproduce actor/thresholds;6241 fireTransformatorBurst instead applies4 damage plus cosmetic lightning to up to7 DIFFERENT nearby living targets in a30-axis box. Datapack other/transformator_shoot creates7 physical slowraycast electro markers lifetime30, mecha/shield_blockable, all aimed at nearest qualifying target within30. Paper heal adds4/8HP rather than zombie instant-health effects (which may damage undead). P:6302 native fireball explosion and adjacent display cleanup. These are material behavior divergences, not formatting.
Required closure: chosen attack path end-to-end shield/obstruction/terrain/credit; fireball owner lineage and shared WILD mechanism rules; health-triggered fire; exact actor/display visuals; role protection without making killable500HP actor immortal; pause mechanism and actor/visual cleanup under Time Stop/unload/stop. Reuse native fireball/blast/indirect kill services where applicable; no arbitrary TTL or target caps beyond source seven rays. Do not substitute a menu or reforge machine.

## Mom + Otchim: linked chain, defer as cheap leaves

D/function/drops/mom.mcfunction: invulnerable armor stand mom+rc_1sec, arms, no base, DisabledSlots4144959, legs180deg; hand loot lbc:mom/model lbc:mum; displayName copied from held item. Captures nearest nonspectator lbcID2, gives have_mom tag and broadcast mom + owner + burned. other/mom each second faces nearest player, places fire at its feet and picks six pig/cow/sheep death sounds pitch0/.5. Antimagic progression references a nearby OTHER owner's Mom (players_1s line36): owner link is gameplay data, not optional display text.
Otchim: invulnerable persistent invisible zombie, otchim+lucky_mob+active; helmet loot lbc:otchim/model lbc:otchim, displayName copied, broadcast owner message. P:6619/6668 hardcodes personalized names Daeris_: Mom and DraumLord: Otchim plus white owner, pink translation, tiers f/ff. Source loot tables mom.json and otchim.json are authority for exact complete item presentation.
Every5ticks Otchim within2.5 of Mom invokes other/absurd_child: deletes both, allay at+1Y, glowing/bold gold/100000HP, real lbc:luckyblock_block_only held reward, nearby15 advancement and broadcast. Datapack strikes REAL lightning at EVERY entity outside Nexus; Paper5972 uses ONE cosmetic lightning at birth. Paper uses axis-box Mom lookup rather than source distance sphere. P:6032 child ray attacks deal6 at range20 when target farther5;3% random20 messages within30;6318 cycles red/brown/stem via luckyblock_neko_block/loli_block/block_only. These outputs drag in broad lucky-block reward/acquisition closure and existing source Absurd Child callbacks, not just two static NPCs. Source global lightning needs an explicit user choice before implementation.

## Stalin: independent but not tiny

D/function/drops/stalin.mcfunction: persistent silent zombified piglin, stalin+lucky_mob, visible red translated stalin,500HP/follow100, invisible+fire resistant2000000ticks; AK model in hand and leather equipment lbc:stalin equippable assets on head/chest/feet, unbreakable head; zero equipment drops, DeathLootTable lbc:stalin. Paper5496 omits authored armor asset presentation in its leather construction; restore datapack visuals.
D/function/other/stalin_shoot_pre and related stalin_shoot/pre2 + workingitems/ak47_otdacha_stalin/ak47_rec define combat. P:5919 fires below495HP,50% chance,40range target,16-pass cooldown;6191 uses spread[-4,5),40ray,9damage, white ash/.2step,5% flame, shot and rasstrelat sounds. Legacy second pre2 path/cooldown and communism inventory/communist tag exclusions must be traced through final ray collisions, not assumed from Paper nearestCombatTarget alone.
Real death reward D/loot_table/stalin.json: infinite enchanted AK47, model lbc:ak47, tier_s, name AK-47, type+shooter lore and morgenshtern/Beta Server Harem event credit, flags {transformable:1,shooter:1,magicwand:1,ak47:1}. NEW LbcAk47 already supports native weapon, but verify exact imported reward identity, infinity versus finite variant, actual firing and reforge/crafting flags. Source stalin_ak_finite is separate. Full closed leaf includes native retaliation/melee, aggro/target policy, gun ray shield/obstruction, death reward, lifecycle and indirect attribution.

## Questions before committing scope

1. Transformator: preserve datapack seven shared-target electro rays/undead instant-health behavior, or Paper multi-target lightning4HP and explicit heal4/8? Its source has no reforge menu.
2. Mom/Otchim: retain datapack world-wide destructive lightning or Paper local cosmetic birth? Child broad reward cycling must stay gated until fully closed.
3. Stalin: authoritative datapack burst/pre2 chain versus Paper single-shot cooldown? Confirm combat WILD/communism exclusions through hits, not merely acquisition.
4. Academy: accept existing deferred placement/early teleport ordering, or require deferred teleport after completed safe placement? This is a qualification concern even if no new actor code is required.

No runtime success claimed. Forest/Infinity deliberately excluded; latest Ultraponos qualification is outside this audit.
