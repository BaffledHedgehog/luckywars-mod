# Ant head appearance and egg mob names - 25 October2026

## Exact ant appearance source

Read-only old server: `world/datapacksdisabled/LBC/data/lbc/function/drops/muravei.mcfunction` summons five invisible baby zombies at the item position, wearing the repeating-command-block item model `lbc:muravei`. Paper `plugins/luckywars/src/main/java/me/luckywars/swrg/SwrgNativeService.java:5630-5677` likewise uses baby zombies and the same helmet; it spreads them within.8 horizontally and gives an explicit white/nonitalic name. Neither source shifts the physical actor vertically. Current native drop and egg preserve the same baby zombie and helmet model. Their different unbreakable/vanishing helmet components remain authored differences.

The current author RP was read, never changed: `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/assets/lbc/items/muravei.json` resolves `lbc:item/muravei`; `assets/lbc/models/item/muravei.json` supplies head translation[0,-14.75,2], scale[2,2,2]. The lowest leg vertices haveY0.

Exact1.21.8 official artifacts were downloaded to the OS temporary directory only:

- [Version metadata](https://piston-meta.mojang.com/v1/packages/6b61868e9cc18b59f97241ac46cc84ab605c8152/1.21.8.json).
- [Official client](https://piston-data.mojang.com/v1/objects/a19d9badbea944a4369fd0059e53bf7286597576/client.jar), SHA1a19d9badbea944a4369fd0059e53bf7286597576.
- [Official mappings](https://piston-data.mojang.com/v1/objects/bdeb624c3aefba11d9d40f34bc96176350b549b6/client.txt), SHA1bdeb624c3aefba11d9d40f34bc96176350b549b6.

Mapped bytecode: HumanoidModel(gnx) BABY_TRANSFORMER uses head offset16, head scale1.5/2=.75. BabyModelTransform(gmc) translates then scales PartPose: neutral baby head pivotY12/16block, scale.75. CustomHeadLayer(hhy) applies native head item translationY-.25 then scale(.625,-.625,-.625). ZombieRenderer(hhm) selects the transformed baby humanoid mesh.

Exact26.3 cached sources: `client/model/monster/zombie/BabyZombieModel.java` now supplies a distinct baby head atY15.25/16 with scale1. `client/renderer/entity/layers/CustomHeadLayer.java` retains the same item translation/scale, and HumanoidMobRenderer uses default custom-head transforms. Therefore unchanged old RP equipment is both enlarged4/3 and displaced downward on the new baby model.

For the original unrotatedY0 foot vertex, old neutral height is.03762109375block above actor feet; unchanged RP on26.3 yields-.403296875block, a.44091796875block sink. These are transform calculations, not a client screenshot measurement. The exact neutral-pose compensation is head translation[0,-7.4625,1.5], scale[1.5,1.5,1.5]. It reproduces all old model vertices at neutral head pitch and every yaw. Head pitch animates around a changed native pivot; a static resource-pack transform cannot reproduce every historical pitch exactly. No arbitrary server-bodyY offset or physics/dimensions change is introduced.

## Template application

Follow-up read on25October confirms the author pack already contains the new
head translation `[0,-7.4625,1.5]` and scale `[1.5,1.5,1.5]`. The user removed
the applied template/README during this turn, consistent with the repository's
resource-pack workflow. They are deliberately not restored; the author's pack
is now the authoritative copy. No agent wrote to that pack.

`resourcepack_template/assets/lbc/models/item/muravei.json` is a **JSON merge fragment**, not a replacement complete model. In the author's original model, replace only `display.head` with that fragment's head object. Preserve credit, textures, elements and all other display contexts. The user applies this manually; copying the fragment over the complete model would remove its geometry. Reload the resource pack afterward. It affects both ant drops and ant eggs because they share the model. It targets26.3 and should not be applied to an old1.21.8 client pack. Server changes cannot activate an unapplied author-RP fragment.

## Egg name bug and correction

Native26.3 `EntityType.appendDefaultStackConfig` first applies egg components, including its red CUSTOM_NAME, then loads ENTITY_DATA. `TypedEntityData.loadInto` saves that already-named entity and calls recursive `CompoundTag.merge` with the payload. A source CustomName that omits color (fat skeleton/ant) inherits red from the egg compound; omitted root flags can leak in the same way. This is recursive merge inheritance, not a later vanilla name-copy operation.

`LbcSpecialEggs.adopt` now decodes an explicit payload CustomName independently using the native ComponentSerialization codec and replaces the entity name after native loading and family adoption. Source colors and absent formatting fields remain exact. Names without an explicit authored CustomName retain vanilla egg-name/renamed-egg behavior. Authored mob names take precedence over a renamed egg's item title. Existing SpecialEggMixin appends adoption at the correct native hook and configures offspring before insertion; no new hook is required.

Validation and final build status are recorded by the integrating root agent. No client screenshot/pixel qualification is claimed.

Final root qualification: build `b4cdfa8d`,148 recorded results across two actual
mode1 matches and actual stop cleanup. See [combined review](GAMEPLAY_REVIEW_2026_10_25.md)
for the tested artifact, deployment state and verification boundaries.
