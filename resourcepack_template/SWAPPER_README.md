# Swapper charge

Merge updates/swapper.ru_ru.json and updates/swapper.en_us.json into your pack languages;
these are suggested placeholder translations for your wording. Failure key:
`lbc.swapper.charge_failed`; the server applies red formatting.

Add sound event `minecraft:lbcsounds.swap_charge` to your sound definitions
and supply the charging audio (nominal charge is0.5 seconds).
Only the charging player hears it. Add `minecraft:lbcsounds.swap_go` for successful
swaps; it is sent once to each of the two participants after success. Failure, success, item/role/life retirement
and match stop send StopSound for this event/category MASTER. This avoids one
player cancelling another player's charging sound. No audio asset is supplied.

Swapper changes no blocks. Ten active ticks stationary; camera rotation allowed.
Moving, changing the held stack or entering TECH/strong antimagic cancels without
item debit. The target is chosen and rechecked at completion; no target retains
the item. Time Stop pauses the charge. Short pending charges cancel at Vanity
rewind; they are not long-duration actors.
