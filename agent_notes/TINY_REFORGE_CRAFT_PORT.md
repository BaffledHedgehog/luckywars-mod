# Tiny Teleport forced random crafting

## Author requirement (user)

> а получится сделать чтоб перековка была случайная и чтоб нельзя было от неё отказаться? просто в сетке крафта игрок так то может тупо предмет забрать если его не устроит результат, а перековка должна быть типа forced если игрок её делает

The native target is a stable generic preview. Taking the result commits the
random roll and ingredient consumption together; the player cannot inspect an
actual reward and then retrieve the ingredients instead. Closing before taking
the preview remains an ordinary cancelled craft, with no random roll or cost.

## Qualified transaction

Inventory 2x2 and workbench 3x3 use the same custom recipe: exactly two occupied
slots, one Tiny Teleport wand and one supported transformable donor. The existing
two-identical-Tiny upgrade recipe keeps precedence. No mana cost. Each accepted
action consumes exactly one donor and has a 30% chance to consume one Tiny;
remaining stack components are retained. Empty/unsupported output preserves the
ingredients. This replaces the source's ground interaction and whole-stack
recursion, following the native crafting requirement.

The Ultra Reforge category mapper and normalized loot roller are shared with
LbcBolts. A one-unit donor snapshot excludes the unrelated exact64-arrow rule.
Keep every output stack, including secondary rewards such as Razor seeds and
independent category rewards. The detached ItemEntity supplies native donor loot
context without being added to the world. Award attribution belongs to the
crafting player.

Intercept AbstractContainerMenu.clicked before native dispatch: vanilla onTake
is too late because several actions already transferred the result. Accepted
actions are empty-cursor pickup, quick move, empty hotbar/offhand swap and throw.
One action commits one craft. Extra rewards go into inventory, with guaranteed
ground overflow; throw drops the entire reward bundle. Capacity never becomes a
post-roll refusal. Clone, drag, pickup-all, occupied destinations and invalid
buttons are rejected without a roll. Direct quickMoveStack is also intercepted.
Creative uses the same transaction. No placeholder may leave the result slot.

Native role, match, dimension, time-stop, menu validity and input checks precede
the roll. Stack mutation and delivery happen synchronously on the server thread;
no tick polling or queued ground scan is added. This is a narrow random-crafting
transaction; existing fixed-result recipes retain their native handling.

## Translation handoff

Merge the two new skywars2.crafting.tiny_reforge keys from the template languages
into the user's resource pack. An English fallback is available before merging.
No icon texture or original resource pack is changed.

## Evidence

TinyReforgeChecks runs actual menu.clicked paths in both grids, with seeded native
chance RNG, temporary native multi-output/empty loot tables and a live shooter
roll. It checks rejected actions, preview stability, input conservation, both
chance branches, overflow and shared crafting regressions. Final build and two native crafting matches passed in 41.56 seconds; two more
ambient lightning/Infbolt regression matches passed in 47.29 seconds on the same
artifact. All four matches used SHA256
310f2fc979087a167a8b13083b240da0c68849e0cb68dc00a73d490f786ba837.
The deterministic multi-output fixture uses the native 26.3 modifier schema;
chance seeds are spread deterministically because adjacent native legacy seeds
can produce the same initial branch. No production RNG injection was added.

Ready archive: build/verification/skywars2-tiny-reforge-310f2fc9.jar. Evidence:
build/verification/tiny-reforge-final/ contains build/native/regression logs,
results and qualification. CRC, 2639 processed resources, recipe, Fabric metadata
and bundled SGUI verified. The isolated server stopped and released its world
lock. Main server JAR remains 332415d6 and was not modified. Original resource
pack untouched; only two template translation keys per locale were added.
Client appearance remains for manual review.

Legacy and native click-path sources: [acquisition review](NEXT_ACQUISITION_REVIEW.md).
Related qualified implementation: [ambient lightning](LIGHTNING_ACQUISITION_PORT.md).
