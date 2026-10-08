# Куда встроить datapack в Fabric-мод

В новом проекте данные мода находятся здесь:

`E:/servers_mew/skywars2_last/luckywars-mod/src/main/resources/data/`

Например:

```text
luckywars-mod/src/main/resources/
├── fabric.mod.json
└── data/
    ├── lbc/
    │   ├── function/
    │   ├── loot_table/
    │   ├── advancement/
    │   └── structure/
    ├── lbcr/function/
    ├── swrg/
    │   ├── function/
    │   ├── loot_table/
    │   └── structure/         ← NBT из пакета maps
    ├── minecraft/tags/function/
    │   ├── load.json
    │   └── tick.json
    └── tag_replacements/tags/
```

Копируется содержимое `data/` исходного пакета, а не внешняя папка с названием
пакета. Namespace сохраняются: например, `lbc:load` должен остаться в
`data/lbc/function/load.mcfunction`. Gradle упакует эти ресурсы в JAR мода.
`assets/` предназначен для клиентских моделей/текстур, а `data/` — для серверных данных.

## Что учесть именно для этих трёх пакетов

- В снимке найден ровно один совпадающий путь между пакетами:
  `data/minecraft/tags/function/load.json`. У LBC он вызывает `lbc:load`,
  у swarogskywars — `swrg:core/load`. При копировании поверх друг друга один
  вызов потеряется. Нужен общий тег с обоими вызовами и проверенный порядок загрузки.
  Список совпадений: `inventory/resource_collisions.json`.
- Встроенные обычные данные мода не сохраняют раздельные `pack.mcmeta` трёх пакетов.
  Фильтр `minecraft:advancement/.*` из `swarogskywars/pack.mcmeta` поэтому не
  переносится простым копированием `data/`: его роль нужно разобрать отдельно.
- Исходный `spyglass.json` LBC указывает Minecraft 1.21.11. Новый сервер работает
  на 26.3; само расположение файлов не гарантирует совместимость команд,
  компонентов, loot tables, advancements, worldgen или NBT.
- Полный datapack сохраняет свои функции и tick-логику. Если ту же механику
  перенести в Java, её старую функцию/триггер надо согласованно выключить,
  чтобы два обработчика не управляли одним состоянием.

В ходе текущего исследования в мод ничего не копируется: сначала фиксируется
фактическое поведение исходного набора. Эта заметка описывает место будущего переноса.
