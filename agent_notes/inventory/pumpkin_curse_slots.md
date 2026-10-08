# Pumpkin Curse: все переходы слотов

Выбор равновероятен среди перечисленных целей. Сам проход проверяет слоты последовательно; перемещение вперёд может продолжиться в том же проходе.

| Исходный слот | Возможные цели |
|---|---|
| container.0 | container.1, container.27 |
| container.1 | container.0, container.2, container.28 |
| container.10 | container.9, container.11, container.19 |
| container.11 | container.10, container.12, container.20 |
| container.12 | container.11, container.13, container.21 |
| container.13 | container.12, container.14, container.22, weapon.offhand |
| container.14 | container.13, container.15, container.23 |
| container.15 | container.14, container.16, container.24 |
| container.16 | container.15, container.17, container.25 |
| container.17 | container.16, container.26 |
| container.18 | container.9, container.19, container.27 |
| container.19 | container.10, container.18, container.20, container.28 |
| container.2 | container.1, container.3, container.29 |
| container.20 | container.11, container.19, container.21, container.29 |
| container.21 | container.12, container.20, container.22, container.30 |
| container.22 | container.13, container.21, container.23, container.31 |
| container.23 | container.14, container.22, container.24, container.32 |
| container.24 | container.15, container.23, container.25, container.33 |
| container.25 | container.16, container.24, container.26, container.34 |
| container.26 | container.17, container.25, container.35 |
| container.27 | container.18, container.28, container.0 |
| container.28 | container.19, container.27, container.29, container.1 |
| container.29 | container.20, container.28, container.30, container.2 |
| container.3 | container.2, container.4, container.30 |
| container.30 | container.21, container.29, container.31, container.3 |
| container.31 | container.22, container.30, container.32, container.4 |
| container.32 | container.23, container.31, container.33, container.5 |
| container.33 | container.24, container.32, container.34, container.6 |
| container.34 | container.25, container.33, container.35, container.7 |
| container.35 | container.26, container.34, container.8 |
| container.4 | container.3, container.5, container.31 |
| container.5 | container.4, container.6, container.32 |
| container.6 | container.5, container.7, container.33 |
| container.7 | container.6, container.8, container.34 |
| container.8 | container.7, container.35 |
| container.9 | container.10, container.18, armor.feet |
| armor.chest | armor.head, armor.legs |
| armor.feet | armor.legs, weapon.offhand, container.9 |
| armor.head | armor.chest |
| armor.legs | armor.chest, armor.feet |
| weapon.offhand | armor.feet, container.13 |
