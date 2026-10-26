Root qualification: build1a8a3825 passed the dedicated native suite and broader
regressions in two actual matches (68 results total); installed by worker19140.
Evidence: build/verification/signed-effects-gameplay/.

# Signed absorption and shadow hearts

Implementation candidate, 26 October 2026. This note does not claim a build,
native check run, gameplay qualification, or installation.

The approved level is `(long) amplifier + 1`. Absorption alone keeps one
independently timed entry per distinct amplifier. Same-level reapplication uses
native same-level duration/flag refresh rules. Different levels add before the
result is split into positive yellow HP or negative concealed maximum HP.
Positive reapplication refills its current net yellow capacity; a completely
spent positive pool expires through the native entity-tick hook. Native positive
single-level behavior remains +4 yellow HP per level, without int overflow.

Negative net capacity conceals the top segment of the native health bar. Empty
hearts in that segment remain empty server-side. For example, 12/20 HP with
negative level 1 becomes 12/16 visible HP and 0/4 shadow HP. Native `heal(8)` fills
visible HP to 16 and then shadow HP to 4. Damage only spends visible HP and can
kill while healed shadow HP remains. Removal returns the concealed segment with
its current healed amount. Healing never revives an entity with zero visible HP.

The maximum-health modifier is transient and multiplicative, chosen against the
native maximum without this modifier so that equipment/health-boost multipliers
still produce an absolute four-HP reduction per net negative level. Its value
changes only when needed. Native minimum max_health is 1; a server getter hook
allows a fully concealed bar to reach zero and die. The native client may briefly
show the attribute minimum of one during this terminal state. Persistent shadow
HP, capacity, and every independent effect's duration/flags use native
ValueInput/ValueOutput and signed effect codecs. Old native hidden absorption
chains are imported into independent entries. State lives on each entity, so
death respawn creates a fresh state and unloading does not retain a global map.

Existing source-based Greed admission remains in force. Greed also defers a
new increase in concealed HP while its absolute health-loss guard is active.
Time Stop stops stack countdown with entity simulation; externally requested
native healing retains its normal admission. All LivingEntity.heal calls handle
shadow overflow. Player.isHurt includes missing shadow HP, admitting native food
regeneration. Custom regeneration code must use
`ShadowAbsorption.needsHealing(entity)` for the same admission rule.

Integration: register ShadowAbsorptionMixin and its nested Attributes, Started,
Clock, and FoodHealing mixins. Exclude ABSORPTION from generic signed attribute
rewrites: this implementation owns its native modifier suppression. Invoke
`ShadowAbsorptionChecks.run(game)` from the isolated native check runner.

Fixtures cover native entity heals/damage/death, independent signed stack
expiration in actual Cow.tick calls, positive refilling, clear/removal, partial
shadow restoration, native entity save/load, maximum-health change, and signed
int endpoints. Food regeneration, health-boost/equipment combinations, Greed,
Time Stop, reconnect/respawn, and physical client rendering still require the
parent integration's broader checks. This agent was instructed not to build,
run the main server, or commit changes.
