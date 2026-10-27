# Gaycaster warning orientation, lane density and red countdown

The author requested three corrections to meditation warnings: turn the electric
preview wall 90degrees along laser travel; add midpoint electric columns; and
replace the old first-phase particle-refresh pulses with a continuous countdown.

`TrialWarningVisual.wall` now orients its long local X axis along travel by
adding90degrees to the previous yaw. Existing world center,30x2scale, seven-block
lead and movement interpolation remain. Electric damage/particle/paint columns
now occupy -14,-10.5,-7,-3.5,0,3.5,7,10.5,14: nine instead of five, retaining the
same outer columns,8damage and0.5block/tick travel. The11random50%wall spawn
opportunities are unchanged.

The first phase creates one30tick beam per prism at hazard age1, with one beacon
activation sound, retaining27block length and the existing attack at age31.
There are11first-phase warning displays per complete trial, replacing33pulses.

All shared red BEAM warnings now carry separate opacity/countdown bytes in
`CUSTOM_MODEL_DATA.colors[0]`: R=opacity,G=countdown,B=0. Other warnings keep the
grayscale fade contract. Countdown moves0..255 before the final fade window;
opacity stays255 throughout color progression, then falls while countdown stays
255. The shader uses exact raw texture RGBA to interpolate `#600018` to `#FF0E0E`.
This is continuous GLSL mixing with tick-updated data, not additional texture/model
variants. Shader data does not collide with reserved zone/black-hole/nuclear
constant tints. Existing lifetime, Time Stop and epoch/viewer cleanup are shared.

The actual author RP was read only. Three ready replacements were prepared from
its current files: item.vsh,item.fsh and lbc:meditation_warning.glsl. Vertex output
and fragment input location12 carry raw Color.g independently of location11fade.
The helper's former scene-color bloom function is an inert compatibility stub:
even an unremoved old post call cannot reintroduce approximate terrain matching.
No post chain, model, texture, language or signed-effect shader changes. The
author-emptied old bloom sampler template was preserved exactly, not restored.
Copy only the three new shader deltas and F3+T. Material-only warning bloom still
has the limitations recorded in MEDITATION_WARNINGS_README.md.

## Evidence

Final build **23c9e577**:
`build/verification/skywars2-gaycaster-warnings-23c9e577.jar`, SHA256
`23c9e57725152ed19e4c299ae1766d8614415b7cf4d2930bc3972df4ff55d75c`.
Gradle offline build passed. Exact523sources,1482classes,3224resources; all nine
core-merging classes remain byte-identical to the previous qualified b54c8454.
195other preexisting dirty author paths and31live overrides retain their initial
bytes/existence. Reintroducing only the warning include is the intentional
already-applied-template delta; original RP files retain baseline hashes.

The final native runner passed28results in two actual mode1matches,126.94seconds,
with normal child shutdown and exact JAR/config/player/scoreboard restoration.
Natural trials verify the same beam identity/no gap through the full countdown,
all11warnings, separate tint channels/bright final fade, actual long-axis
orientation via the display matrix, complete phases, Time Stop and cleanup.
A scoped native wall step damages all nine real cows at old and midpoint columns
for exactly8HP, while a cow in the intervening gap stays undamaged. Sauvojen,
minigun, Time Stop/contacts, TPS compensation, layered effects and FOV suites pass.

An initial geometry assertion compared unnormalised lookup-table direction
vectors and failed; it was replaced by a matrix-based normalized direction check.
The initial failed logs/results remain in this scope, and production wall
rotation did not change between those runs. The final artifact is from the
successful complete run; no failed build was qualified or queued.

Standalone RTX4080SUPER validation compiles/links eight item-shader variants,
checks768gradient/opacity samples and21unchanged other-marker samples. All1300
vanilla block textures at five brightnesses (96430samples) produce zero false
terrain glow with the new include. This is GPU/source/native-packet validation,
not an in-game physical-client image claim. Evidence is in
`build/verification/gaycaster-warning-timing/`, including the scoped final native
results/log, shader validator/results and qualification.

Installer PID15932 adopted23c9e577 in place of queuedb54c8454 and waits for normal
main23a25d43shutdown without stopping/restarting the server. Receipt:
`build/verification/gaycaster-warning-timing/main-install.json`.
After verified adoption, the superseded queued b54c8454JAR was removed with exact
path/hash checks (40,692,687bytes);23c9e577ready,23a25d43installed/rollback and the
existing8d2f7513/6890ae59fallbacks remain. The cleanup receipt is `cleanup.json`
in this verification scope; the old installer receipt now records supersession.
