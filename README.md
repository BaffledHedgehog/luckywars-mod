# LuckyWars

Серверный мод LuckyWars для Minecraft 26.3 на Fabric, Java 25. Обычному игроку
достаточно ванильного клиента и ресурс-пака. Дополнительный мод телефона находится
в `media-client/`; его сетевой протокол — в `media-protocol/`.

## Сборка

Из каталога проекта с настроенным `JAVA_HOME` на JDK 25:

```powershell
.\gradlew.bat --console=plain productionJar
```

Готовый серверный JAR: `build/libs/skywars2-0.1.0.jar`. Fabric API и Polymer
устанавливаются отдельно; SGui вложен в JAR. Carpet нужен только проверкам,
для обычной игры он необязателен. Перед заменой JAR остановите целевой сервер.

Для отдельного тестового артефакта:

```powershell
.\gradlew.bat --console=plain productionJar testServerJar
python -B tools/check_map_editing_artifacts.py build/libs/skywars2-0.1.0.jar build/libs/skywars2-0.1.0-test-server.jar
```

Не используйте `gradlew clean` в рабочем каталоге разработчика: в `build/` также
хранятся локальные проверочные миры и подтверждённые сборки.

## Где что находится

| Каталог | Назначение |
| --- | --- |
| `src/main/java` | Серверная реализация и Mixin |
| `src/main/resources` | Таблицы лута, структуры, теги, конфигурация и прочие ресурсы мода |
| `src/checks/java` | Нативные проверки и инструменты редактирования карт |
| `src/checks/resources` | Метаданные проверочных Mixin |
| `media-client` | Необязательный клиентский плеер телефона |
| `media-protocol` | Общие типы сетевого протокола телефона |
| `resourcepack_template` | Дополнения и инструменты для авторского ресурс-пака |
| `tools` | Сборка ресурсов, проверки и инструменты разработки |
| `docs/history` | Прежние подробные отчёты и игровые контракты |

Реорганизация Java-пакетов продолжается. Актуальные границы исходников описаны
в [ARCHITECTURE.md](ARCHITECTURE.md), проверка сборок — в
[SERVER_TESTS.md](SERVER_TESTS.md), безопасный перенос классов — в
[tools/JAVA_REFACTOR.md](tools/JAVA_REFACTOR.md).

Миры, учётные данные и локальные настройки сервера не входят в репозиторий.
Исторические инструкции по запуску и прошлым переносам сохранены в
[справочных материалах](docs/history/README.md); их отметки о сборках не означают
состояние установленного сейчас сервера.
