# Перенос пакетов Java

`java_refactor.py` использует семантическое переименование файлов из Eclipse JDT Language Server. JDT обновляет декларации пакетов, импорты и ссылки на Java-классы. Инструмент сохраняет WorkspaceEdit для просмотра, сверяет хэши при применении и проводит отдельную диагностику изменённых документов.

## Подготовка

Работайте в отдельном чистом Git worktree. Заранее создайте каталоги целевых пакетов внутри соответствующих Java source roots. Для ресурсов, Fabric metadata и строковых ссылок в скриптах нужны отдельные изменения.

Укажите пути к установленному JDT LS, JDK 25 и XML `.classpath` с абсолютными путями к разрешённым библиотекам Loom. Подходит classpath из существующего Java workspace редактора. Пути можно передать параметрами `--jdt-home`, `--java-home`, `--classpath` или переменными `JDTLS_HOME`, `JDTLS_JAVA_HOME`, `JDTLS_CLASSPATH`.

Сервер использует свой внешний workspace. Скрипт отключает Gradle и Maven import и autobuild. Он игнорирует чужие Eclipse project references, каталоги скомпилированных классов и любые JAR с классами `me.skywars2`. Классы LuckyWars доступны из связанных исходников.

## Один перенос

```powershell
python tools/java_refactor.py --worktree $worktree plan `
  --move src/main/java/me/skywars2/game/LbcPortalDropItem.java `
  --to src/main/java/me/skywars2/game/items/drops/portal `
  --out $plan
python tools/java_refactor.py --worktree $worktree apply --plan $plan
python tools/java_refactor.py --worktree $worktree apply --plan $plan --write
python tools/java_refactor.py --worktree $worktree diagnose --plan $plan --out $diagnostics
```

`$plan` и `$diagnostics` находятся вне выбранного worktree, их родительские каталоги уже существуют. С указанным `--state-dir` JDT также сохраняет локальный журнал в выбранном внешнем каталоге. В план одного вызова можно передать несколько `--move` из одного исходного каталога.

## Группы из разных source roots

Передайте `plan --batch $groups --out $plan` вместо `--move` и `--to`. Пример файла групп:

```json
[
  {
    "sources": ["src/main/java/me/skywars2/game/MyItem.java"],
    "to": "src/main/java/me/skywars2/game/items/example"
  },
  {
    "sources": ["src/checks/java/me/skywars2/game/MyItemChecks.java"],
    "to": "src/checks/java/me/skywars2/game/items/example"
  }
]
```

Для каждой группы JDT выполняет свой `java/move` с точным целевым package fragment. Группы идут по порядку на внешней копии отслеживаемых Java-исходников. Оригинальные файлы меняются только после `apply --write`. По умолчанию подключаются существующие `src/main/java`, `src/checks/java` и `media-protocol/src/main/java`. Нестандартный набор задаётся через повторяемый `--source-root`.

Production и проверки с package-private доступом должны оказаться в одном Java-пакете даже при размещении в разных source roots. Проверяйте итоговую группу целиком через `diagnose`. Эта команда запрашивает диагностику JDT для новых файлов и всех документов, затронутых WorkspaceEdit. Затем запустите сборку production и test-server для проверки полного зависимого кода и ресурсов.
