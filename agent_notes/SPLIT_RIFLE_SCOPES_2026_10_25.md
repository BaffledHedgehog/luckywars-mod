# Separate SLG and Magnum scope presentation

Author request (user):

> если кратко я сделал разделение прицелов им
>
> и ещё партиклы выстрела сильно перекрывают камеру в прицеле - надо их чуть ниже спавнить (сам луч пусть так же идёт, и другие игроки пусть его правильно видят. Т.е. только стреляющему надо их буквально чуть опускать чтоб партиклы не вцентре были. И при удалении от игрока их возвращать на легальный луч рейкаста)

Read-only source: current author pack under
`C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8`,
`blockbench/zoom.txt`, four corresponding post-effect JSONs and both scoped
item definitions. Source hashes are recorded in local verification evidence.
No author pack or applied/deleted template was changed.

SLG uses `lbc:zoom_slg` / `lbc:zoom_slg_out`; Magnum uses
`lbc:zoom_magnum` / `lbc:zoom_magnum_out`. Entry removes all six owned effects,
including the legacy `lbc:zoom` and `lbc:zoom_out` aliases. Normal exit removes
the active overlay and adds its corresponding out overlay for exactly six
ticks. Re-entry cancels that expiry. Weapon replacement clears the old scope
without leaving its exit overlay on the new weapon. Death, disconnect, match
stop and native login cleanup cover all six saved identifiers. Unrelated
post-effects are preserved. Native FOV projection remains at 0.25.

The owning client's Polymer stack receives `minecraft:item_model` equal to
`lbc:super_rifle_scoped` or `lbc:magnum_scoped` while that scope is active.
Exit immediately projects the original model again. Authoritative item
components and slot contents remain unchanged, including arbitrary original
models; Polymer retains the exact server-stack roundtrip. Native menu snapshots
refresh the view on scope transitions. The network callback reads only an
immutable enum snapshot from a concurrent map. No saved hidden-model component,
restoration metadata or slot replacement is introduced.

SLG and every Magnum mode retain their native hit ray and particle payloads.
Only the shooter's scoped tracer points are shifted toward screen-down by
`0.22 * max(0,1-distance/8)^2` blocks, measured from the captured eye position.
Observer points remain exactly on the native ray, and points at eight blocks
or farther are unchanged for everyone. The camera basis is captured before
recoil. The profile is sampled once per ray; only near points need individual
recipient positions. Terminal impact effects retain their real world positions.

Native RiflePresentationChecks covers both weapons and hands, exact overlay
selection and expiry, recipient Polymer models and authoritative roundtrip,
real Shift-click movement with one gun retained, and matching shooter/observer
particle streams with near displacement and exact far convergence.
RifleZoomChecks exercises save/load plus the registered JOIN callback for all
six identifiers. Accuracy checks inspect distant unchanged samples; previous
payload/ammunition/scope/FOV/sound/real-stop regressions remain included.

Build ea7979f0 passed 26 recorded results in two actual mode1 matches (49.18s).
Evidence: `build/verification/split-scopes-final/`. Native packets and state are
verified; client pixels, GPU rendering, physical reconnect, full process restart
and busy-main TPS are not directly observed.
