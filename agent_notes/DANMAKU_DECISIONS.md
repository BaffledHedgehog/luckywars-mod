# Danmaku user decisions (25 October 2026)

These supersede source admission and inventory-fallback workarounds.

## Shared accumulating timer (user)

> Сохранить общий таймер и накопление

## Free charged bomb (user)

> Да, бомба без требования маны

## Staff emission and future external sources (user)

> именно из жезла данмаку - стрелять только пока жезл в руке. Дело в том что есть и другие источники данмаку (например, гримуар) который тратится и вот он должен работать даже если жезла данмаку в руке нет. Я там какие то костыли для этого делал (кучу проверок что ли)
> ну сам жезл - только пока в руке должен работать

## Generic damage cooldown tag (user)

> какой нафиг HurtTime?? урон карточки игнорирует HurtTime, проверь типы урона в тегах, я в легаси многое менял, в том числе урон карт

The parent question incorrectly assumed ordinary HurtTime rejection. Preserve
source generic damage and the imported bypasses_cooldown tag, plus source+3
per impact; no new hit-rate gate or invulnerability reset.
