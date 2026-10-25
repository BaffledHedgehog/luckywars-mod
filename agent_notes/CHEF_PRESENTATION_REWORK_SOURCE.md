# Chef presentation audit — 25 October 2026

Read-only source/resource audit; only this note was written. No Java, resources, build, server, author pack or git changes. This records proposed work, not a qualified fix.

## Missing texture evidence

`src/main/java/me/skywars2/game/LbcWtf.java:35-43` builds a static resolved profile, UUID `2853d7b5-a618-4c73-98b6-ff37a92157f8`, name `Shef`, empty properties, with native patch `texture=skywars2:entity/chef`, `model=wide`. This key is already correct. Local 26.3 `ClientAsset.ResourceTexture(Identifier)` expands it to `skywars2:textures/entity/chef.png`; passing the full textures/...png string to that single-argument constructor/codec would double the prefix/suffix and break it.

`src/main/resources/assets/skywars2/textures/entity/chef.png` is valid 64×64 RGBA, 4455 bytes, SHA256 `bc1ca79f70a6e01688495db3abb3fccccb60298c40f7b6d049d6dbf46848eb36`. Exact identical bytes exist in both qualified JARs and `E:/servers_mew/skywars2_last/polymer/resource_pack.zip` at `assets/skywars2/textures/entity/chef.png`. `SkyWars2.java:60` calls `PolymerResourcePackUtils.addModAssets(MOD_ID)`. Generated RP SHA256 observed `9f3515a7380601e8a747212de4b0632b1d0021ba40c8d8eab9a474f906af2a62`.

Disk observation during this audit: `mods/skywars2.jar` already equals Danmaku ready `04396ca4cfd84f4c70f3a559f0dcd3811d46be79a12dfc9128e5fda2da48445e`, whereas retained `build/verification/skywars2-chef-skin-2558af6b.jar` is `2558af6b570287dd32d692c5305644b3804fe052695e0aa453c4718e421b5206`. Root controls installation/qualification; this audit did not change either file.

There is no demonstrated missing server file or wrong profile key. A client without the generated server pack has a direct missing-resource reference and can show the reported black/purple texture. Config `config/polymer/auto-host.json` enables hosting but has `required:false`; `resource-pack.json` likewise does not require it. Delivery, successful client loading and stale pack state remain unproven; do not present a guessed cause as confirmed. Inspect actual client resource-load logs/status and render the NPC after the current server pack loads. Network skin upload is unnecessary: native `ClientMannequin` -> `PlayerSkinRenderCache` -> `PlayerSkin.with(patch)` uses this local resource directly. Existing `ChefChecks.java:117-126` proves profile path/model and stream round-trip only, not rendered pixels.

## Subtitle removal

The subtitle comes from vanilla `Mannequin.DEFAULT_DESCRIPTION`, translate key `entity.minecraft.mannequin.label`; it is independent of custom name `shef`. `DATA_DESCRIPTION` initially contains that component. Native private `setHideDescription(true)` calls `updateDescription`, sending `Optional.empty()` in entity metadata. Use a narrow accessor/invoker mixin to invoke that method when constructing Chef. Preserve the existing custom name. Overriding server `getDescription()` alone cannot change client metadata. Alternative native load with `hide_description:true` must preserve other saved fields because `readAdditionalSaveData` resets pose/hand/layers/immovable/default description from defaults.

## Native held sword

Read-only author pack chain is complete:

- `assets/lbc/items/oguzok_sword.json`: model `lbc:item/oguzok_sword`.
- `assets/lbc/models/item/oguzok_sword.json`: parent `minecraft:item/handheld`, layer0 `minecraft:item/oguzok_sword`.
- `assets/minecraft/textures/item/oguzok_sword.png` exists.

No author RP edit/template is needed. Proposed native item registry key `skywars2:oguzok_sword`, client item-model key exactly `lbc:oguzok_sword`. Register before registry freeze through existing `LbcWtf.register()`/`LbcItems.register()` path, using `SimplePolymerItem(properties, Items.WOODEN_SWORD, true)` and `new Item.Properties().setId(key).sword(ToolMaterial.WOOD,3,-2.4f)`. Ensure final default `DataComponents.ITEM_MODEL` remains the author key after property finalization, following existing component-default modification patterns if needed. Native WOOD material adds0 attack damage, producing exactly MAINHAND `minecraft:base_attack_damage` +3 ADD_VALUE and `minecraft:base_attack_speed` -2.4 ADD_VALUE. IRON/NETHERITE material would add their bonuses and be wrong.

Replace all combat equips: `LbcWtf.java:74,152`, `LbcChefBrain.java:122`; review internal tools list at brain line26 (currently netherite sword) so mining selection cannot re-equip the old combat sword. Preserve actual mining tools/building blocks. Existing attack at `LbcWtf.java:158` explicitly deals8 and delay20; item attributes do not automatically alter that manually supplied damage. Decide/apply requested gameplay interpretation explicitly and check it rather than assuming +3 item modifiers change the current attack callback.

## Exact local API source

`.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`: `net/minecraft/core/ClientAsset.java`, `world/entity/player/PlayerSkin.java`, `world/item/component/ResolvableProfile.java`, `world/entity/decoration/Mannequin.java`, `client/entity/ClientMannequin.java`, `client/renderer/PlayerSkinRenderCache.java`, `world/item/ToolMaterial.java`. No web or upload required.

## Manual author-pack delivery observation

Exact author-pack `assets/skywars2/textures/entity/chef.png` is absent (read-only check). Public `server.properties`: `resource-pack` empty, `require-resource-pack=false`. Polymer auto-host is enabled but optional (`required:false`). Thus a user loading only their author pack receives the item models but lacks this chef texture; this is consistent with the reported missing texture and established manual-template workflow, though actual client loaded-pack state was not inspected. After the current qualification checkpoint, prepare only `resourcepack_template/assets/skywars2/textures/entity/chef.png` with the original bundled bytes and concise application instructions if root chooses manual delivery. Do not edit the author pack, duplicate models/translations, upload a skin, or change the already-correct asset identifier. This audit did not create that template.
