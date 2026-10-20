package me.skywars2.game;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/** Персональная read-only панель атрибутов после короткой неподвижной паузы. */
public final class AttributeHud {
    private static final int IDLE_TICKS_TO_SHOW = 20;
    private static final int VALUE_POLL_TICKS = 5;
    private static final double CHANGE_EPSILON = 1.0e-9;
    private static final int DISPLAY_SCALE = 10_000;
    private static final double MAX_SCALED_VALUE = Long.MAX_VALUE / (double) DISPLAY_SCALE;
    // Цифры — 6 px; иконка вместе с font-space дополнением занимает отдельный слот 10 px.
    private static final int GLYPH_ADVANCE = 6;
    private static final int ICON_ADVANCE = 10;
    private static final int ICON_PADDING_OFFSET = 0xF0000;
    private static final int ROWS_PER_COLUMN = 18;
    private static final int COLUMN_GAP = 6;
    private static final Style SPACING_STYLE = Style.EMPTY.withFont(new FontDescription.Resource(
            Identifier.parse("lbc:attribute_hud/spacing")));
    private static final Map<Integer, Style> ROW_STYLES = new HashMap<>();
    private static final String ATTR_OBJECTIVE_NAME_PREFIX = "a";

    private final GameController game;
    private final AttributeSupplier playerDefaults = DefaultAttributes.getSupplier(EntityTypes.PLAYER);
    private final List<AttributeInfo> attributes;
    private final Map<UUID, State> states = new HashMap<>();

    public AttributeHud(GameController game) {
        this.game = game;
        this.attributes = cacheAttributes();
    }

    /** Вызывать раз за серверный тик: позиция каждого игрока сверяется с предыдущим сэмплом. */
    public void tick() {
        for (ServerPlayer player : game.server().getPlayerList().getPlayers()) {
            if (!player.isAlive() || game.lbc().hidesKnowledge(player)) {
                clear(player);
                continue;
            }

            UUID id = player.getUUID();
            ResourceKey<Level> dimension = player.level().dimension();
            DisplaySlot sidebarSlot = sidebarSlot(player);
            State state = states.get(id);
            if (state == null) {
                // Первый сэмпл задаёт baseline; 20 следующих стабильных тиков дают одну секунду.
                states.put(id, new State(player, dimension, player.position(), sidebarSlot));
                continue;
            }
            state.player = player;

            boolean slotChanged = state.sidebarSlot != sidebarSlot;
            if (slotChanged) {
                if (state.shown) hide(state);
                state.sidebarSlot = sidebarSlot;
                state.pollTicks = 0;
            }

            if (!state.dimension.equals(dimension) || !state.position.equals(player.position())) {
                state.dimension = dimension;
                state.position = player.position();
                state.idleTicks = 0;
                state.pollTicks = 0;
                if (state.shown) hide(state);
                state.snapshot = null;
                state.cachedLines = List.of();
                continue;
            }

            int previousIdleTicks = state.idleTicks;
            state.idleTicks = Math.min(IDLE_TICKS_TO_SHOW, state.idleTicks + 1);
            boolean firstEligibleTick = previousIdleTicks < IDLE_TICKS_TO_SHOW
                    && state.idleTicks >= IDLE_TICKS_TO_SHOW;
            if (state.idleTicks >= IDLE_TICKS_TO_SHOW
                    && (slotChanged || firstEligibleTick || ++state.pollTicks >= VALUE_POLL_TICKS)) {
                state.pollTicks = 0;
                updateSnapshot(state);
            }
        }
    }

    /** Убирает временную панель игрока, в том числе при отключении. */
    public void clear(ServerPlayer player) {
        State state = states.remove(player.getUUID());
        if (state != null && state.shown) {
            state.player = player;
            hide(state);
        }
    }

    /** Восстанавливает sidebar всем подключённым игрокам и прекращает их отслеживать. */
    public void stop() {
        for (State state : states.values()) {
            if (state.shown && state.player.connection != null) hide(state);
        }
        states.clear();
    }

    // Package-private методы дают GameChecks доступ к реальному состоянию этой панели.
    boolean shown(ServerPlayer player) {
        State state = states.get(player.getUUID());
        return state != null && state.shown;
    }

    int idleTicks(ServerPlayer player) {
        State state = states.get(player.getUUID());
        return state == null ? 0 : state.idleTicks;
    }

    List<Component> displayedLines(ServerPlayer player) {
        State state = states.get(player.getUUID());
        return state == null || !state.shown ? List.of() : List.copyOf(state.lines);
    }

    List<String> displayedAttributeKeys(ServerPlayer player) {
        State state = states.get(player.getUUID());
        if (state == null || !state.shown || state.snapshot == null) return List.of();
        List<String> result = new ArrayList<>();
        for (int i = 0; i < attributes.size(); i++) {
            if (state.snapshot.displayed[i]) result.add(attributes.get(i).name);
        }
        return List.copyOf(result);
    }

    boolean displays(ServerPlayer player, Holder<Attribute> holder) {
        State state = states.get(player.getUUID());
        if (state == null || !state.shown || state.snapshot == null) return false;
        for (int i = 0; i < attributes.size(); i++) {
            if (attributes.get(i).holder.equals(holder)) return state.snapshot.displayed[i];
        }
        return false;
    }

    private void updateSnapshot(State state) {
        DisplaySnapshot next = capture(state.player);
        boolean changed = state.snapshot == null || !state.snapshot.sameDisplayedState(next);
        state.snapshot = next;
        if (changed) state.cachedLines = buildLines(next);

        if (state.shown) {
            if (state.cachedLines.isEmpty()) hide(state);
            else sendChangedLines(state, state.cachedLines);
        } else if (state.idleTicks >= IDLE_TICKS_TO_SHOW && !state.cachedLines.isEmpty()) {
            show(state);
        }
    }

    private DisplaySnapshot capture(ServerPlayer player) {
        boolean[] supported = new boolean[attributes.size()];
        boolean[] displayed = new boolean[attributes.size()];
        double[] values = new double[attributes.size()];
        long[] displayKeys = new long[attributes.size()];
        for (int i = 0; i < attributes.size(); i++) {
            AttributeInfo info = attributes.get(i);
            if (!player.getAttributes().hasAttribute(info.holder)) continue;
            supported[i] = true;
            double value = player.getAttributeValue(info.holder);
            values[i] = value;
            displayed[i] = differs(value, info.baseline);
            if (displayed[i]) displayKeys[i] = displayKey(value);
        }
        return new DisplaySnapshot(supported, displayed, values, displayKeys);
    }

    private List<Component> buildLines(DisplaySnapshot snapshot) {
        List<Value> changed = new ArrayList<>();
        for (int i = 0; i < attributes.size(); i++) {
            if (!snapshot.displayed[i]) continue;
            AttributeInfo info = attributes.get(i);
            changed.add(new Value(info.icon, format(snapshot.values[i])));
        }
        if (changed.isEmpty()) return List.of();

        // Одна scoreboard entry обходит лимит 15 строк. После 18 атрибутов начинаем новый столбик.
        // Верхний отступ ограничен: исходные PNG с прозрачным полем должны помещаться в glyph atlas клиента.
        MutableComponent column = Component.empty();
        int firstRow = -Math.min(6, (Math.min(ROWS_PER_COLUMN, changed.size()) - 1) / 2);
        int width = 0;
        for (int i = 0; i < changed.size(); i++) {
            if (i > 0 && i % ROWS_PER_COLUMN == 0) {
                column.append(Component.literal("\uE000".repeat(width + COLUMN_GAP)).withStyle(SPACING_STYLE));
                width = 0;
            }
            Value value = changed.get(i);
            String text = value.icon + iconPadding(value.icon) + value.text;
            int advance = 0;
            for (int offset = 0; offset < text.length();) {
                int glyph = text.codePointAt(offset);
                if (attributeIcon(glyph)) advance += ICON_ADVANCE;
                else if (!attributeIcon(glyph - ICON_PADDING_OFFSET))
                    advance += glyph == '.' ? 2 : GLYPH_ADVANCE;
                offset += Character.charCount(glyph);
            }
            column.append(Component.literal(text).withStyle(rowStyle(firstRow + i % ROWS_PER_COLUMN)));
            column.append(Component.literal(rewind(advance)).withStyle(style -> style.withFont(FontDescription.DEFAULT)));
            width = Math.max(width, advance);
        }
        // Предыдущие столбики уже сдвинули X; последняя прокладка завершает общую ширину sidebar.
        column.append(Component.literal("\uE000".repeat(width)).withStyle(SPACING_STYLE));
        return List.of(column);
    }

    private static Style rowStyle(int row) {
        return ROW_STYLES.computeIfAbsent(row, offset -> Style.EMPTY.withFont(new FontDescription.Resource(
                Identifier.parse("lbc:attribute_hud/row_" +
                        (offset < 0 ? "m" + -offset : offset > 0 ? "p" + offset : "0")))));
    }

    private static boolean attributeIcon(int codepoint) {
        return codepoint >= 0xE210 && codepoint <= 0xE245;
    }

    private static String iconPadding(String icon) {
        if (icon.isEmpty() || icon.codePointCount(0, icon.length()) != 1) return "";
        int codepoint = icon.codePointAt(0);
        // Ширина невидимого дополнения задаётся в RP: серверу не нужно читать текстуры и считать alpha.
        return attributeIcon(codepoint) ? new String(Character.toChars(codepoint + ICON_PADDING_OFFSET)) : "";
    }

    private static String rewind(int pixels) {
        StringBuilder result = new StringBuilder(pixels / 32 + 1);
        while (pixels >= 32) { result.append('\uF82F'); pixels -= 32; }
        if (pixels > 0) result.append((char) (pixels <= 16 ? 0xF800 + pixels - 1 : 0xF820 + pixels - 17));
        return result.toString();
    }

    private void show(State state) {
        if (state.cachedLines.isEmpty()) return;
        state.objective = makeObjective(state.player);
        state.player.connection.send(new ClientboundSetObjectivePacket(state.objective,
                ClientboundSetObjectivePacket.METHOD_ADD));
        state.shown = true;
        state.lines = state.cachedLines;
        for (int i = 0; i < state.lines.size(); i++) sendLine(state, i, state.lines.get(i), state.lines.size());
        state.player.connection.send(new ClientboundSetDisplayObjectivePacket(state.sidebarSlot, state.objective));
    }

    private void hide(State state) {
        ServerPlayer player = state.player;
        DisplaySlot slot = state.sidebarSlot;
        player.connection.send(new ClientboundSetDisplayObjectivePacket(slot, objectiveForSlot(player, slot)));
        for (int i = 0; i < state.lines.size(); i++) {
            player.connection.send(new ClientboundResetScorePacket(scoreOwner(i), state.objective.getName()));
        }
        player.connection.send(new ClientboundSetObjectivePacket(state.objective,
                ClientboundSetObjectivePacket.METHOD_REMOVE));
        state.shown = false;
        state.objective = null;
        state.lines = List.of();
    }

    private void sendChangedLines(State state, List<Component> next) {
        int oldCount = state.lines.size();
        int newCount = next.size();
        boolean countChanged = oldCount != newCount;
        for (int i = 0; i < Math.max(oldCount, newCount); i++) {
            if (i >= newCount) {
                state.player.connection.send(new ClientboundResetScorePacket(scoreOwner(i), state.objective.getName()));
            } else if (countChanged || i >= oldCount || !next.get(i).equals(state.lines.get(i))) {
                sendLine(state, i, next.get(i), newCount);
            }
        }
        state.lines = next;
    }

    private static void sendLine(State state, int index, Component line, int lineCount) {
        state.player.connection.send(new ClientboundSetScorePacket(scoreOwner(index), state.objective.getName(),
                lineCount - index, Optional.of(line), Optional.of(BlankFormat.INSTANCE)));
    }

    private List<AttributeInfo> cacheAttributes() {
        List<AttributeInfo> result = new ArrayList<>();
        for (Attribute attribute : BuiltInRegistries.ATTRIBUTE) {
            Holder<Attribute> holder = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute);
            String name = BuiltInRegistries.ATTRIBUTE.getKey(attribute).getPath();
            double baseline = playerDefaults.hasAttribute(holder)
                    ? playerDefaults.getValue(holder)
                    : attribute.getDefaultValue();
            result.add(new AttributeInfo(holder, name, icon(name), baseline));
        }
        result.sort(Comparator.comparing(AttributeInfo::name));
        return List.copyOf(result);
    }

    private static boolean differs(double value, double baseline) {
        return Double.isFinite(value) && Double.isFinite(baseline)
                ? Math.abs(value - baseline) > CHANGE_EPSILON
                : Double.compare(value, baseline) != 0;
    }

    private static long displayKey(double value) {
        if (!Double.isFinite(value) || Math.abs(value) > MAX_SCALED_VALUE) {
            return Double.doubleToLongBits(value);
        }
        double magnitude = Math.floor(Math.abs(value) * DISPLAY_SCALE + 0.5);
        long rounded = (long) magnitude;
        return value < 0 ? -rounded : rounded;
    }

    private static String format(double value) {
        if (!Double.isFinite(value)) return Double.toString(value);
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
    }

    private static String icon(String id) {
        // Авторские символы всех 36 атрибутов PLAYER; bitmap-иконки задаются в шрифтах строк RP.
        return switch (id) {
            case "air_drag_modifier" -> "\uE223";
            case "armor" -> "\uE213";
            case "armor_toughness" -> "\uE214";
            case "attack_damage" -> "\uE216";
            case "attack_knockback" -> "\uE217";
            case "attack_speed" -> "\uE218";
            case "below_name_distance" -> "\uE224";
            case "block_break_speed" -> "\uE219";
            case "block_interaction_range" -> "\uE225";
            case "bounciness" -> "\uE226";
            case "burning_time" -> "\uE227";
            case "camera_distance" -> "\uE228";
            case "entity_interaction_range" -> "\uE229";
            case "explosion_knockback_resistance" -> "\uE230";
            case "fall_damage_multiplier" -> "\uE231";
            case "friction_modifier" -> "\uE232";
            case "gravity" -> "\uE212";
            case "jump_strength" -> "\uE233";
            case "knockback_resistance" -> "\uE234";
            case "luck" -> "\uE215";
            case "max_absorption" -> "\uE211";
            case "max_health" -> "\uE210";
            case "mining_efficiency" -> "\uE235";
            case "movement_efficiency" -> "\uE221";
            case "movement_speed" -> "\uE222";
            case "name_tag_distance" -> "\uE236";
            case "oxygen_bonus" -> "\uE237";
            case "safe_fall_distance" -> "\uE238";
            case "scale" -> "\uE239";
            case "sneaking_speed" -> "\uE240";
            case "step_height" -> "\uE241";
            case "submerged_mining_speed" -> "\uE220";
            case "sweeping_damage_ratio" -> "\uE242";
            case "water_movement_efficiency" -> "\uE243";
            case "waypoint_receive_range" -> "\uE244";
            case "waypoint_transmit_range" -> "\uE245";
            default -> "?";
        };
    }

    private static String scoreOwner(int index) {
        return "attr_" + index;
    }

    private static Objective makeObjective(ServerPlayer player) {
        Scoreboard actual = player.level().getScoreboard();
        String name;
        do {
            // 1 prefix + 15 hex chars keeps the objective id within vanilla's 16-char limit.
            name = ATTR_OBJECTIVE_NAME_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 15);
        } while (actual.getObjective(name) != null);

        // Objective packets are built from this isolated scoreboard, never the server scoreboard.
        Scoreboard isolated = new Scoreboard();
        return isolated.addObjective(name, ObjectiveCriteria.DUMMY, Component.empty(),
                ObjectiveCriteria.RenderType.INTEGER, false, BlankFormat.INSTANCE);
    }

    private static DisplaySlot sidebarSlot(ServerPlayer player) {
        var scoreboard = player.level().getScoreboard();
        var team = scoreboard.getPlayersTeam(player.getScoreboardName());
        if (team != null) {
            var color = team.getColor();
            if (color.isPresent() && scoreboard.getDisplayObjective(color.get().displaySlot()) != null) {
                return color.get().displaySlot();
            }
        }
        return DisplaySlot.SIDEBAR;
    }

    private static Objective objectiveForSlot(ServerPlayer player, DisplaySlot slot) {
        return player.level().getScoreboard().getDisplayObjective(slot);
    }

    private static final class State {
        ServerPlayer player;
        ResourceKey<Level> dimension;
        Vec3 position;
        DisplaySlot sidebarSlot;
        int idleTicks;
        int pollTicks;
        boolean shown;
        Objective objective;
        DisplaySnapshot snapshot;
        List<Component> cachedLines = List.of();
        List<Component> lines = List.of();

        State(ServerPlayer player, ResourceKey<Level> dimension, Vec3 position, DisplaySlot sidebarSlot) {
            this.player = player;
            this.dimension = dimension;
            this.position = position;
            this.sidebarSlot = sidebarSlot;
        }
    }

    private record AttributeInfo(Holder<Attribute> holder, String name, String icon, double baseline) { }
    private record Value(String icon, String text) { }

    private record DisplaySnapshot(boolean[] supported, boolean[] displayed, double[] values, long[] displayKeys) {
        boolean sameDisplayedState(DisplaySnapshot other) {
            if (!java.util.Arrays.equals(displayed, other.displayed)) return false;
            for (int i = 0; i < displayed.length; i++) {
                if (displayed[i] && displayKeys[i] != other.displayKeys[i]) return false;
            }
            return true;
        }
    }
}
