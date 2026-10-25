# Weapon presentation and Vex targeting corrections

25 October 2026. Points 4?7 are implemented for root qualification. The author resource pack and existing zoom and zoom_out resources are unchanged.

## Minigun muzzle

Mainhand uses the legacy eye-relative command offset `^-.3 ^-.2 ^.5`; offhand keeps its previously correct mirrored `^.3 ^-.2 ^.5`. `LbcLaserMinigun.muzzle(player, hand)` uses native `LocalCoordinates.apply`, including the command basis and Mth angle quantization at steep pitch. Each paid shot passes its actual hand to tracing and display creation. The old handless helper represents mainhand.

Legacy source: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/shoot/cast.mcfunction`. Native checks cover cardinal and oblique yaw, three pitches, mirrored origins and actual mainhand/offhand outgoing beam spawns. Mirror checks use the native quantized local-left basis: at pitch55 its nominal0.6 separation is approximately0.599973, so exact Euclidean0.6 would contradict the command geometry.

## LowVex equipment and sword ultimate

The normal ten-Vex trap payload and LowVex egg explicitly contain empty mainhand equipment. Egg adoption clears both hands after native finalization and before insertion, removing the vanilla Vex iron sword. Existing health, AI, attribution and death rewards remain operative. Native trap and block/fluid egg fixtures assert empty hands.

Each ultimate update draws five independent random targets with replacement from the eligible anchored area. Eligibility is rechecked before every draw. A living enemy can be selected again immediately; selection retains no locked victim. Native checks require five shots with only one eligible enemy, then recover the actual aimed centers from arrow origins and velocities and verify switching between two enemies while both remain alive. Technical targets and inventory sword immunity remain excluded.

## Vanilla scope attack animation

SLR and Magnum default item properties set `DataComponents.ATTACK_ANIMATION` to `new SwingAnimation(SwingAnimationType.NONE, 6)`. Exact 26.3 `Minecraft.startAttack` reads the mainhand stack, calls `getAttackAnimation`, performs native entity/block attack logic, predicts that animation locally, and then sends `ServerboundPunchPacket`. The synchronized component therefore suppresses the initiating player's mainhand rifle swing before its first frame. Genuine native attacks and the existing punch-driven scope toggle remain available; zoom_out still lasts six ticks.

`INTERACT_ANIMATION` controls a different branch in `startUseItem`. Consumable/use animation components do not replace the mainhand attack-animation lookup and do not provide an equivalent LMB fix.

An offhand-only rifle with an ordinary or empty mainhand still predicts the ordinary mainhand swing. Cancelling incoming Punch or filtering a later server swing packet cannot prevent this earlier local prediction. Hiding that swing requires changing or projecting the mainhand stack itself, or client support; an empty stack cannot carry the required item component. This implementation leaves ordinary mainhand items intact.

Exact source is the project Loom cache `minecraft-merged-d6f9bb6878-26.3-sources.jar`: `net/minecraft/client/Minecraft.java` (`startAttack`, `continueAttack`, `startUseItem`), `world/item/ItemStack.java` (`getAttackAnimation`), `world/item/component/SwingAnimation.java`, and `server/network/ServerGamePacketListenerImpl.java` (`handlePunch`). Checks cover actual Polymer projection of the NONE component, native Punch swing state, connection/packet-codec preservation, native handleAttack damage and the existing scope lifecycle. Real client pixels remain a separate observation.
