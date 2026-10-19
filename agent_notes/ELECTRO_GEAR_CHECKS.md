# Electro crafting and gear checks

Updated on 16 October (user): the combat fixture places a real Polymer antimagic
beacon around the attacker. Ten native melee hits still charge Cyberkatana and
apply its sourced aura; RMB still creates shock pulses. Actual Wither and native
katana lightning damage do not increment either charging counter. The beacon is
restored before shield/other-wand checks and on fixture failure. Current results:
[SERVER_TESTS.md](../SERVER_TESTS.md).

The focused `tools/smoke_terra.py --electro-crafting` scenario checks ordinary Tiny Electro combining in both native crafting grids. It covers preview safety, normal and Shift takes, exact occupied cells, stack conservation, rejection of two donors in one cell or an extra ingredient, legacy `stelectro_tiny` custom-data recognition, no output from an untaken preview, and observer take restrictions.

Run `tools/smoke_terra.py --electro-gear` for the two additional gear recipes and combat paths. The crafting fixture assembles Cyberkatana and Electro Shield through actual 2x2 and 3x3 menus, verifies exact iron sword or shield consumption, rejects extra ingredients, checks retained Tiny Electro components, and compares 64 donor returns per recipe against a separately seeded 60% reference sequence. The menu check verifies source loot and Polymer restoration for both gear items. The match check drives ten native non-arrow katana attacks, spends the charge through the normal use packet, checks enemy/ally/observer shock eligibility and owner-attributed native lightning damage, then holds the native shield while real self-owned and ally-owned arrows reverse on two consecutive Electro ticks and transfer to the blocker. It checks native shield blocking components and that reflected arrows cannot hit an ally.

Both modes require an isolated server directory containing the Fabric launcher. The smoke script installs the project-built jar into that copy before launch.

Финальные проверки расширены корневым агентом: native UseItem действительно
начинает использование щита; два последовательных тика отражают также TNT,
падающий блок и виртуальные Electro-разряды без сброса их возраста.
Проверены передача TNT owner, DamageSource падающего блока и защита активного
союзника. Молния отдельно проверяет отсутствие урона и поджигания союзника.
Состав финальных прогонов и ограничения — [SERVER_TESTS.md](../SERVER_TESTS.md).
