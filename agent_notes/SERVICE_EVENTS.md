# Service event settings

`GameConfig` stores three server-wide event switches: `eventHalloween`,
`eventNovember`, and `eventTournament`. They default to `false` and persist in
`config/luckywars/game.json` through the existing Gson save/load path.

`GameConfig.setEvent(name, rawValue)` accepts `halloween`, `november`, or
`tournament` (case-insensitive) and `true`/`false`. Successful updates save the
config and return the existing `settings.saved` response with canonical keys
`event.halloween`, `event.november`, or `event.tournament`. Invalid names and
boolean values use the existing `SettingError` translation path. Settings-menu
reset preserves all three flags and ranked: a public GUI must not mutate
OP-only service settings. `/settings event <name> true|false` inherits the
operator-level-2 requirement of `/settings`, allows console access, and is
available in the lobby. `/settings` summary also lists all three event flags.
Event mechanics and score bridging are not implemented by this settings change.

## Shared match controls (user)

The online entity tag `admin` takes priority for map selection, GUI match
settings, start, and quickstart. If at least one online player has the tag,
only tagged players may control these actions. Without online tagged admins,
all non-observer players may control them, subject to the current stage.
Operator status does not bypass this policy; it independently controls service
commands. Personal menus remain available. Callbacks recheck current tags;
pending starts also recheck the original requester's connection and rights
after asynchronous map loading. Console actions remain available.
