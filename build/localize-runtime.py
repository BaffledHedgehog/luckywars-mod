from pathlib import Path
import json

langs = {'ru_ru': {}, 'en_us': {}}
def tr(key, ru, en):
    for locale, value in [('ru_ru', ru), ('en_us', en)]: langs[locale]['skywars2.' + key] = value
def edit(name, replacements):
    path = Path('src/main/java/me/skywars2/' + name)
    source = path.read_text(encoding='utf-8')
    for old, new in replacements:
        assert old in source, (name, old)
        source = source.replace(old, new)
    path.write_text(source, encoding='utf-8', newline='\n')

base = [
('message.starting','Игровой цикл ещё загружается.','The game is still loading.'),
('settings.lobby_only','Настройки можно менять только в лобби.','Settings can only be changed in the lobby.'),
('settings.arguments','Укажите параметр и значение.','Specify a setting and its value.'),
('settings.unknown','Неизвестный параметр «%s». Выполните /settings для списка.','Unknown setting "%s". Use /settings to list settings.'),
('settings.integer','Параметр %s должен быть целым числом от %s до %s.','Setting %s must be an integer between %s and %s.'),
('settings.range','Для %s допустим диапазон %s..%s.','Setting %s must be within %s..%s.'),
('settings.boolean','Параметр %s принимает только true или false.','Setting %s only accepts true or false.'),
('settings.saved','Настройка сохранена: %s=%s','Setting saved: %s=%s'),
('settings.map_failed','Не удалось выбрать карту или её вариант.','Could not select this map or variant.'),
('settings.map_first','Текущая карта недоступна; сначала выберите карту: /settings map <id>.','Select an available map first: /settings map <id>.'),
('settings.broken','Разрушенный вариант: %s.','Broken map variant: %s.'),
('settings.header','Настройки лобби (изменение только до матча):\n','Lobby settings (change before the match):\n'),
('settings.line','%s: %s\n','%s: %s\n'),
('settings.help','Использование: /settings <параметр> <значение>. Карта: map <id>, затем broken true|false.','Usage: /settings <setting> <value>. Map: map <id>, then broken true|false.'),
('common.enabled','Включено','Enabled'),('common.disabled','Выключено','Disabled'),
('common.broken','разрушенная','broken'),('common.normal','обычная','normal'),
('game.title','SkyWars','SkyWars'),
('game.status','Состояние: %s | карта: %s | загрузка: %s | живых: %s | участников: %s | тиков: %s | лобби: %s | ошибка: %s','State: %s | map: %s | loading: %s | alive: %s | participants: %s | ticks: %s | lobby: %s | error: %s'),
('game.join_hint','SkyWars: /sw или ПКМ компасом — меню.','SkyWars: /sw or right-click a compass to open the menu.'),
('game.map_unknown','Неизвестная карта: %s.','Unknown map: %s.'),
('game.map_unavailable','Карта %s недоступна: %s','Map %s is unavailable: %s'),
('game.no_broken','У карты %s нет полного разрушенного варианта.','Map %s has no complete broken variant.'),
('game.map_selected','Выбрана карта %s (%s). Загрузка при старте.','Selected map %s (%s). It will load when the match starts.'),
('game.start_unavailable','Сейчас нельзя начать матч.','Cannot start a match right now.'),
('game.host_only','Матч запускает ведущий.','Only the host can start the match.'),
('game.quick_missing','В пуле быстрого старта нет полной карты на %s игроков. Выберите карту вручную.','No complete quick-start map supports %s players. Select a map manually.'),
('game.start_requirements','Нужны минимум два игрока и полная карта с достаточным числом спавнов.','At least two players and a complete map with enough spawns are required.'),
('game.loading','Загрузка карты %s...','Loading map %s...'),
('game.load_failed','Карта не загружена: %s','Map loading failed: %s'),
('game.cancelled_players','Старт отменён: недостаточно игроков или спавнов.','Start cancelled: not enough players or spawns.'),
('game.cancelled_leave','Старт отменён: игроки вышли.','Start cancelled: players left.'),
('game.started','Матч начался!','The match has started!'),
('game.kill','Игрок устранён: %s','Player eliminated: %s'),
('game.eliminated','%s выбыл.','%s has been eliminated.'),
('game.lives','%s: осталось жизней %s','%s: %s lives remaining'),
('game.wait_loading','Дождитесь окончания текущей загрузки.','Wait for the current loading operation to finish.'),
('game.cancelled','Матч отменён без награды.','Match cancelled without rewards.'),
('game.winner','Победитель: %s (+%s монет и ключ).','Winner: %s (+%s coins and a key).'),
('game.draw','Ничья.','Draw.'),
('game.lobby_ready','Лобби готово. /sw — меню.','Lobby ready. Use /sw to open the menu.'),
('item.menu_compass','SkyWars — меню (ПКМ)','SkyWars — menu (right-click)'),
('bossbar.countdown','Начало через %s с','Starting in %s s'),
('bossbar.phase','Стадия %s: %s с','Stage %s: %s s'),
('event.stage_one','Первая стадия: %s','First stage: %s'),
('event.stage_two','Вторая стадия: %s','Second stage: %s'),
('skill.unavailable','Навык %s временно отключён: его триггер или безопасная цель пока не подтверждены.','Skill %s is temporarily disabled: its trigger or safe target is not yet confirmed.'),
('game.give_failed','Не удалось выдать предмет/набор игроку %s: %s','Could not give an item/kit to %s: %s'),
('test.disconnect','Проверка позднего входа завершена.','Late-join check complete.'),
('map.error.dimension','Не загружено измерение skywars2:arena. Проверьте данные мода.','Dimension skywars2:arena is not loaded. Check the mod data.')]
for row in base: tr(*row)
for stage, ru, en in [('lobby','Лобби','Lobby'),('loading','Загрузка','Loading'),('countdown','Отсчёт','Countdown'),('playing','Игра','Playing'),('phase_one','Первая стадия','First stage'),('phase_two','Вторая стадия','Second stage'),('ending','Завершение','Ending')]: tr('stage.'+stage,ru,en)
fields = [
('lives',['lives'],1,5,'Жизни (lives, 1..5)','Lives (lives, 1..5)'),
('health',['health'],1,20,'Здоровье (health, 1..20)','Health (health, 1..20)'),
('countdownSeconds',['countdownseconds','countdown'],0,60,'Отсчёт (countdownSeconds, 0..60)','Countdown (countdownSeconds, 0..60)'),
('stageOneSeconds',['stageoneseconds','stage1seconds'],0,3600,'Первая стадия (stageOneSeconds, 0..3600)','First stage (stageOneSeconds, 0..3600)'),
('stageTwoSeconds',['stagetwoseconds','stage2seconds'],0,7200,'Вторая стадия (stageTwoSeconds, 0..7200)','Second stage (stageTwoSeconds, 0..7200)'),
('endingSeconds',['endingseconds','ending'],0,120,'Финал (endingSeconds, 0..120)','Ending (endingSeconds, 0..120)'),
('stageOneEvent',['stageoneevent','stage1event'],0,9,'Событие 1 (stageOneEvent, 0..9)','Event 1 (stageOneEvent, 0..9)'),
('stageTwoEvent',['stagetwoevent','stage2event'],0,8,'Событие 2 (stageTwoEvent, 0..8)','Event 2 (stageTwoEvent, 0..8)'),
('pvpMode',['pvpmode','pvp'],1,2,'Режим боя (pvpMode, 1..2)','Combat mode (pvpMode, 1..2)'),
('day',['day'],1,3,'Время (day: 1 день, 2 ночь, 3 полночь)','Time (day: 1 day, 2 night, 3 midnight)'),
('vanillaKits',['vanillakits'],None,None,'Наборы (vanillaKits)','Kits (vanillaKits)'),
('vanillaSkills',['vanillaskills'],None,None,'Навыки (vanillaSkills)','Skills (vanillaSkills)'),
('noBorders',['noborders'],None,None,'Без границ (noBorders)','No boundaries (noBorders)')]
for field, _, _, _, ru, en in fields: tr('settings.label.'+field,ru,en)
tr('settings.label.map','Карта (map)','Map (map)'); tr('settings.label.broken','Разрушенный вариант (broken)','Broken variant (broken)')

p=Path('src/main/java/me/skywars2/game/GameConfig.java')
s=p.read_text(encoding='utf-8').replace('import com.google.gson.Gson;', 'import net.minecraft.network.chat.Component;\nimport static me.skywars2.SkyWars2.text;\n\nimport com.google.gson.Gson;')
start=s.index('    /** Validates before assignment')
end=s.index('    private static int clamp(', start)
cases=[]
for field, aliases, lo, hi, *_ in fields:
    parser=f'integer(key, value, {lo}, {hi})' if lo is not None else 'bool(key, value)'
    cases.append('            case '+', '.join('"'+a+'"' for a in aliases)+' -> { configured = '+field+' = '+parser+'; canonical = "'+field+'"; }')
block='''    /** Keeps translated validation errors intact until they reach the client. */
    public static final class SettingError extends IllegalArgumentException {
        private final Component component;
        public SettingError(Component component) { super(component.getString()); this.component = component; }
        public Component component() { return component; }
    }

    public Component setSetting(String parameter, String rawValue) {
        if (parameter == null || rawValue == null) throw new SettingError(text("settings.arguments"));
        String key = parameter.trim().toLowerCase(java.util.Locale.ROOT);
        String value = rawValue.trim();
        String canonical;
        Object configured;
        switch (key) {
'''+ '\n'.join(cases)+'''
            default -> throw new SettingError(text("settings.unknown", parameter));
        }
        save();
        return text("settings.saved", canonical, configured instanceof Boolean enabled
                ? text(enabled ? "common.enabled" : "common.disabled") : configured);
    }

    private static int integer(String key, String value, int min, int max) {
        final int parsed;
        try { parsed = Integer.parseInt(value); }
        catch (NumberFormatException e) { throw new SettingError(text("settings.integer", key, min, max)); }
        if (parsed < min || parsed > max) throw new SettingError(text("settings.range", key, min, max));
        return parsed;
    }

    private static boolean bool(String key, String value) {
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        throw new SettingError(text("settings.boolean", key));
    }

'''
p.write_text(s[:start]+block+s[end:],encoding='utf-8',newline='\n')

edit('game/GameController.java', [
('import java.util.ArrayList;', 'import static me.skywars2.SkyWars2.text;\n\nimport java.util.ArrayList;'),
('Component.literal("SkyWars")','text("game.title")'),
('private String mapFailure = "";', 'private Component mapFailure = Component.empty();'),
('mapFailure = "";', 'mapFailure = Component.empty();'),
('mapFailure = "Не найдено измерение skywars2:arena. Проверьте загрузку данных мода.";', 'mapFailure = text("map.error.dimension");'),
('SkyWars2.LOGGER.error(mapFailure);','SkyWars2.LOGGER.error("{}", mapFailure.getString());'),
('Component.literal("SkyWars: /sw или ПКМ компасом — меню.")', 'text("game.join_hint")'),
('map == null ? "Неизвестная карта: " + id : map.reason()', 'map == null ? text("game.map_unknown", id) : map.unavailableReason()'),
('"У карты " + map.name() + " нет полного разрушенного варианта."', 'text("game.no_broken", map.displayName())'),
('"Выбрана карта " + map.name() + (broken ? " (разрушенная)" : "") + ". Загрузка при старте."', 'text("game.map_selected", map.displayName(), text(broken ? "common.broken" : "common.normal"))'),
('public String configureLobbySetting(', 'public Component configureLobbySetting('),
('new IllegalArgumentException("Настройки можно менять только в лобби.")','new GameConfig.SettingError(text("settings.lobby_only"))'),
('new IllegalArgumentException("Неизвестная карта «" + value + "». Проверьте /settings.")','new GameConfig.SettingError(text("game.map_unknown", value))'),
('new IllegalArgumentException("Карта «" + map.name() + "» недоступна: " + map.reason())','new GameConfig.SettingError(text("game.map_unavailable", map.displayName(), map.unavailableReason()))'),
('new IllegalArgumentException("Не удалось выбрать карту.")','new GameConfig.SettingError(text("settings.map_failed"))'),
('"Выбрана карта " + map.name() + " (" + map.id() + "). Она загрузится при старте матча."','text("game.map_selected", map.displayName(), text(config.broken ? "common.broken" : "common.normal"))'),
('new IllegalArgumentException("Параметр broken принимает только true или false.")','new GameConfig.SettingError(text("settings.boolean", "broken"))'),
('new IllegalArgumentException("Текущая карта недоступна; сначала выберите доступную карту через /settings map <id>.")','new GameConfig.SettingError(text("settings.map_first"))'),
('new IllegalArgumentException("Не удалось изменить вариант карты.")','new GameConfig.SettingError(text("settings.map_failed"))'),
('"Вариант разрушенной карты: " + (brokenVariant ? "включён" : "выключен") + "."','text("settings.broken", text(brokenVariant ? "common.enabled" : "common.disabled"))'),
('"Сейчас нельзя начать матч."','text("game.start_unavailable")'),
('"Матч запускает ведущий."','text("game.host_only")'),
('"В пуле быстрого старта нет полной карты на " + count + " игроков. Выберите карту вручную."','text("game.quick_missing", count)'),
('"Нужны минимум два игрока и полная карта с достаточным числом спавнов."','text("game.start_requirements")'),
('"Загрузка карты " + config.mapId + "..."','text("game.loading", maps.info(config.mapId).displayName())'),
('"Карта не загружена: " + result.message()','text("game.load_failed", result.message())'),
('"Старт отменён: недостаточно игроков или спавнов."','text("game.cancelled_players")'),
('Component.literal("Начало через " + seconds + " с")','text("bossbar.countdown", seconds)'),
('"Старт отменён: игроки вышли."','text("game.cancelled_leave")'),
('"Матч начался!"','text("game.started")'),
('Component.literal("Игрок устранён: " + player.getScoreboardName())','text("game.kill", player.getDisplayName())'),
('player.getScoreboardName() + (p.lives == 0 ? " выбыл." : ": осталось жизней " + p.lives)','p.lives == 0 ? text("game.eliminated", player.getDisplayName()) : text("game.lives", player.getDisplayName(), p.lives)'),
('"Дождитесь окончания текущей загрузки."','text("game.wait_loading")'),
('"Матч отменён без награды."','text("game.cancelled")'),
('"Победитель: " + winner.getScoreboardName() + " (+100 монет и ключ)."','text("game.winner", winner.getDisplayName(), 100)'),
('"Ничья."','text("game.draw")'),
('SkyWars2.LOGGER.error("Lobby: {}", mapFailure)', 'SkyWars2.LOGGER.error("Lobby: {}", mapFailure.getString())'),
('"Лобби готово. /sw — меню."','text("game.lobby_ready")'),
('Component.literal("SkyWars — меню (ПКМ)")','text("item.menu_compass")'),
('private void tell(ServerPlayer player, String message) { if (player != null) player.sendSystemMessage(Component.literal(message)); else SkyWars2.LOGGER.info(message); }','private void tell(ServerPlayer player, Component message) { if (player != null) player.sendSystemMessage(message); else SkyWars2.LOGGER.info("{}", message.getString()); }'),
('public void announce(String message) { server.getPlayerList().broadcastSystemMessage(Component.literal(message), false); SkyWars2.LOGGER.info("[game] {}", message); }','public void announce(Component message) { server.getPlayerList().broadcastSystemMessage(message, false); SkyWars2.LOGGER.info("[game] {}", message.getString()); }'),
('Component.literal("Стадия " + (stage == Stage.PLAYING ? 1 : 2) + ": " + remaining + " с")','text("bossbar.phase", stage == Stage.PLAYING ? 1 : 2, remaining)')])
p=Path('src/main/java/me/skywars2/game/GameController.java'); s=p.read_text(encoding='utf-8')
start=s.index('    public String status()'); end=s.index('    public void startServer()',start)
s=s[:start]+'''    public Component status() {
        return text("game.status", text("stage." + stage.name().toLowerCase(java.util.Locale.ROOT)),
                maps.info(config.mapId) == null ? text("game.map_unknown", config.mapId) : maps.info(config.mapId).displayName(),
                text(maps.busy() ? "common.enabled" : "common.disabled"), alivePlayers().size(),
                participants.size(), gameTicks, text(lobbyReady ? "common.enabled" : "common.disabled"), mapFailure);
    }

'''+s[end:]
start=s.index('    public String settingsSummary()'); end=s.index('    public void requestStart(',start)
rows=',\n'.join('                {"'+f[0]+'", config.'+f[0]+'}' for f in fields)+',\n                {"map", config.mapId}, {"broken", config.broken}'
s=s[:start]+'''    public Component settingsSummary() {
        var summary = text("settings.header");
        for (Object[] row : new Object[][] {
'''+rows+'''
        }) {
            Object value = row[1] instanceof Boolean enabled ? text(enabled ? "common.enabled" : "common.disabled") : row[1];
            summary.append(text("settings.line", text("settings.label." + row[0]), value));
        }
        return summary.append(text("settings.help"));
    }

'''+s[end:]
p.write_text(s,encoding='utf-8',newline='\n')

edit('game/MatchEvents.java',[
('import java.util.List;', 'import static me.skywars2.SkyWars2.text;\n\nimport java.util.List;'),
('"§cПервая стадия: событие " + event','text("event.stage_one", text("event.one." + event)).withStyle(net.minecraft.ChatFormatting.RED)'),
('"§cВторая стадия: событие " + event','text("event.stage_two", text("event.two." + event)).withStyle(net.minecraft.ChatFormatting.RED)'),
('Component.literal("Навык " + skill\n                    + " временно отключён: его триггер или безопасная цель пока не подтверждены.")','text("skill.unavailable", skill)'),
('"Не удалось выдать предмет/набор игроку " + player.getScoreboardName() + ": " + command','text("game.give_failed", player.getDisplayName(), command)')])
for category, names in [
('one',[('Скорость II','Speed II'),('Спешка II','Haste II'),('Обновление сундуков','Chest refill'),('Прыгучесть IV','Jump Boost IV'),('Дополнительное здоровье','Health boost'),('Улучшение сундуков','Chest upgrade'),('Невидимость','Invisibility'),('Свечение','Glowing')]),
('two',[('Визеры','Withers'),('Драконы','Dragons'),('Иссушение','Wither effect'),('Дождь TNT','TNT rain'),('Сужение границы','Shrinking border'),('Левитация','Levitation'),('Сундуки из центра','Center chest drops')])]:
    for i,(ru,en) in enumerate(names,1):tr('event.'+category+'.'+str(i),ru,en)
edit('game/GameChecks.java',[('Component.literal("Late-join check complete")','me.skywars2.SkyWars2.text("test.disconnect")')])
Path('build/runtime-translations.json').write_text(json.dumps(langs,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
print('Runtime localization complete:', len(langs['en_us']), 'keys')
