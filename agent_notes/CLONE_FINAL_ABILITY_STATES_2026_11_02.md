# Final logical/native ability clone owners ? 2 November 2026

Implementation only, no build/native process/qualification. Scoped git diff --check
passes. Owner edits now frozen for root integration/compilation.

`CloneFinalAbilityStates` exposes recognizes/recognizesGameplay/protectedActor/
captureNative plus duplicateNear(game,holder,skill,budget,deadlineNanos).
Root must invoke duplicateNear alongside its existing virtual dispatcher. Existing
CloneVirtualAbilityStates was not edited by this task.

Source construction mapping and resulting admission:

- Source sauvojen_wand/cast_2 and cast_2_upgraded create nine source ItemDisplay
  producers tagged slowraycast (savoujen_player/upgraded and st kind tags). Modern
  LbcSauvojen.Drone owns their Marker carrier skywars2.sauvojen.flying plus packet
  model. CloneNearby reconstructs exact kind/accepted owner/hand/independent stack,
  academy flag, lifetime/nextMode, position/direction and current displayed model.
  Private index precedes native publication; one visual is attached/synced once.
  Carrier native doubled/quadried tags are once bits and survive native rewind.
  Trial stacks excluded. Old emitted payloads are not copied; future emissions use
  original caster normally. Producer movement is deterministic owner-relative
  teleport geometry, not a retained velocity field. No cast, mana, advancement,
  original admission sound or player authority is duplicated. Protect native carrier
  independently; the virtual owner handles its main logical state.
- Source items/swords/banhammer/rc_end creates slowraycast banhammer3d ItemDisplay.
  Modern LbcSpecialTools.Hammer is packet-only gameplay producer. Copy actual point,
  intended impact, age and original external author/epoch, plus private once mask.
  Falling movement is native collision-swept fixed two-block descent; there is no
  retained velocity field to randomize. Rebuild one display at current copied point,
  register it, then publish. Already impacted remaining models are decoration and
  are excluded from clone discovery. No activation price or earlier impact is replayed.
- LbcBouncingPoop actual ItemDisplay tag skywars2.bouncing_poop is a gameplay main,
  even though modern tags omit slowraycast. Its persistent Fabric DATA attachment is
  the sole runtime: author/order/epoch/team, mega flag and age. Native NBT/metadata
  supplies rotation, components/model and identity-independent body state. Clone image
  explicitly deep-copies DATA, installs actors membership and binds Source with clone
  emitter while retaining original author/cause. Existing loaded validates/adopts this
  state. No other private queues exist in this owner; rollback removes clone index.
- EXACT EXCEPTION: erecto/click_upgraded_2_mode_sphere_place creates Tags:
  [erecto_sphere,rc_5t,mecha], lifetime300, with NO slowraycast. Root explicitly
  confirmed no scope expansion. Sphere clone implementation is retained dormant and
  is NOT invoked by duplicateNear. Its active state/once-mask checkpoint remains
  available without making this source-ineligible producer eligible.
- Requested classes LbcScareDrops and SauvojenScareFaces do not exist here. Actual
  LbcScareDropItem is item registration. LbcSpecialDrops directly calls LbcItems.screamer:
  immediate audio/particles + native Blindness, with no independently resident slow
  actor. Do not invent an additional gameplay body/history. Source meditation scare
  faces are sauvojen_wand_meditator/scare_face/scare_picture; modern owner is
  SauvojenMeditation.Kind.SCARE under skywars2.sauvojen.meditation. These are trial
  authority and its children, so protectedActor excludes the actual meditation tag.
  No scare/drop/meditation source files were edited after this read-only audit.

Long-state rewind API requiring root coordinator wiring:

`new CloneFinalAbilityStates.Checkpoint(game,links)` captures only Sphere/Drone/Hammer
logical state; `prepare(links)` resolves canonical owner/carrier references and prepares
all three owner plans before mutations. Execute its returned Runnable during frozen
ability install. Native carrier tags preserve Drone once bits; Hammer/Sphere images
preserve their private masks. Rebuild private visuals detached, then call
`CloneFinalAbilityStates.publishCheckpoint(game)` once at presentation publication;
owner pending flags make repeated publication idempotent. Existing unrelated player
windups/erase jobs/trials are not copied or overwritten by these narrow owner plans.
No full checkpoint/coordinator or native process was run by this task.
