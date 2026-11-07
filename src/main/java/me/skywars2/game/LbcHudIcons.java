package me.skywars2.game;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.util.StringDecomposer;

/** Assign the resource pack's no-shadow marker to custom actionbar glyphs only. */
final class LbcHudIcons {
    static final int COLOR = 0x4E5C24;

    private LbcHudIcons() { }

    static MutableComponent mark(Component component) {
        MutableComponent result;
        if (component.getContents() instanceof PlainTextContents literal &&
                literal.text().codePoints().anyMatch(LbcHudIcons::isIcon)) {
            var runs = new IconRuns(component.getStyle());
            // Read inline section-sign formatting so text following an icon
            // keeps its original color, even when the icon overrides that color.
            StringDecomposer.iterateFormatted(literal.text(), Style.EMPTY, runs::accept);
            runs.flush();
            result = runs.result;
        } else {
            // Keep translation keys and other component contents client-resolved.
            result = component.plainCopy().setStyle(component.getStyle());
        }
        for (var child : component.getSiblings()) result.append(mark(child));
        return result;
    }

    private static boolean isIcon(int codePoint) {
        // The pack's HUD/attribute glyph range; negative-space glyphs use F800+.
        return codePoint >= 0xE100 && codePoint <= 0xE2FF;
    }

    private static final class IconRuns {
        final MutableComponent result;
        final StringBuilder text = new StringBuilder();
        Style runStyle;

        IconRuns(Style style) {
            result = Component.empty().setStyle(style);
        }

        boolean accept(int index, Style style, int codePoint) {
            Style desired = isIcon(codePoint) ? style.withColor(COLOR) : style;
            if (!desired.equals(runStyle)) {
                flush();
                runStyle = desired;
            }
            text.appendCodePoint(codePoint);
            return true;
        }

        void flush() {
            if (!text.isEmpty()) {
                result.append(Component.literal(text.toString()).setStyle(runStyle));
                text.setLength(0);
            }
        }
    }
}
