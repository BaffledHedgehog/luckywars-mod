package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.util.StringDecomposer;

/** Component-level reproduction of the unmarked boots icon and mixed HUD text. */
public final class LbcHudIconChecks {
    private record Glyph(int codePoint, Style style) { }

    private static List<Glyph> glyphs(Component text) {
        var result = new ArrayList<Glyph>();
        StringDecomposer.iterateFormatted(text, Style.EMPTY, (index, style, codePoint) -> {
            result.add(new Glyph(codePoint, style));
            return true;
        });
        return result;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void check(Component source) {
        var original = source.copy();
        var marked = LbcHudIcons.mark(source);
        require(source.equals(original), "Source component mutated");
        var before = glyphs(source);
        // Check the actual actionbar packet content, after the normalization
        // used by refreshHud; this does not claim to launch a game client.
        var after = glyphs(new ClientboundSetActionBarTextPacket(marked).text());
        require(before.size() == after.size(), "Glyph count changed");
        for (int i = 0; i < before.size(); i++) {
            var a = before.get(i); var b = after.get(i);
            require(a.codePoint == b.codePoint, "Glyph order changed");
            if (a.codePoint >= 0xE100 && a.codePoint <= 0xE2FF) {
                require(b.style.getColor() != null && b.style.getColor().getValue() == LbcHudIcons.COLOR,
                        "Icon lacks no-shadow marker: " + Integer.toHexString(a.codePoint));
                require(a.style.withColor(LbcHudIcons.COLOR).equals(b.style), "Other icon styling changed");
            } else require(a.style.equals(b.style), "Ordinary character style changed: " + a.codePoint);
        }
        require(glyphs(LbcHudIcons.mark(marked)).equals(after), "Repeated normalization changed rendering");
    }

    public static void main(String[] args) {
        var boots = Component.literal("  \uE102 ").append(Component.literal("123").withStyle(ChatFormatting.YELLOW));
        require(glyphs(boots).get(2).style.getColor() == null, "Old boots reproduction is already marked");
        check(boots);
        check(Component.literal("\uE100").withColor(LbcHudIcons.COLOR)
                .append(Component.literal(" 20575").withStyle(ChatFormatting.GREEN)).append(boots));
        check(Component.literal("\u00a7a\uE102 123 \u00a7r456 \uE103\u00a7fx7").withStyle(ChatFormatting.BLUE));
        check(Component.literal("prefix \uF800 \uE102 12 \uD83D\uDE00").withStyle(ChatFormatting.BOLD)
                .append(Component.literal(" \uE101 9").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC)));
        for (var color : ChatFormatting.values())
            check(Component.literal("\uE102 0123456789 abc \u0410\u0411\u0412").withStyle(color));
        for (int cp = 0xE100; cp <= 0xE2FF; cp++) check(Component.literal(Character.toString(cp)));
        var translated = Component.literal("\uE104 ").append(Component.translatable("ready").withStyle(ChatFormatting.GREEN));
        check(translated);
        require(LbcHudIcons.mark(translated).getSiblings().stream().anyMatch(c -> c.getContents() instanceof TranslatableContents),
                "Client-resolved translation was flattened");
        System.out.println("PASS: old boots witness, all 512 custom glyphs, mana, all vanilla colors, inline formats, inheritance, translations and source immutability");
    }
}
