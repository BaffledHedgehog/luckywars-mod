package me.skywars2.game;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/** A fixed-format LIST component aligns heart sprites and ping without changing player names. */
final class TabPresentation {
    static final Identifier HEART_FONT=Identifier.parse("skywars2:tab_hearts");
    static final Style HEART_STYLE=Style.EMPTY.withFont(new FontDescription.Resource(HEART_FONT)).withColor(ChatFormatting.WHITE).withBold(false).withItalic(false).withoutShadow();
    private static final int[] ADVANCES=advances();
    private static final String DIGITS="0123456789.-";
    private static final int[] NUMBER_ADVANCES=numberAdvances();
    private static final int NUMBER_CELL=Arrays.stream(NUMBER_ADVANCES).max().orElseThrow();
    private static final Map<ServerGamePacketListenerImpl,Component> HEADERS=new IdentityHashMap<>();
    private static MinecraftServer owner;private static long sampled;private static Component header;
    private TabPresentation(){}
    private static int[] advances(){
        try(var in=TabPresentation.class.getResourceAsStream("/skywars2/tab_heart_metrics.json")){
            var rows=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(Objects.requireNonNull(in),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("glyphs");
            int[] result=new int[rows.size()];for(int i=0;i<result.length;i++)result[i]=rows.get(i).getAsJsonObject().get("advance").getAsInt();return result;
        }catch(java.io.IOException error){throw new IllegalStateException("Cannot load TAB heart metrics",error);}
    }
    private static int[] numberAdvances(){
        try(var in=TabPresentation.class.getResourceAsStream("/skywars2/tab_heart_metrics.json")){
            var rows=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(Objects.requireNonNull(in),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("numbers");
            int[] result=new int[rows.size()];for(int i=0;i<result.length;i++)result[i]=rows.get(i).getAsJsonObject().get("advance").getAsInt();return result;
        }catch(java.io.IOException error){throw new IllegalStateException("Cannot load TAB number metrics",error);}
    }
    private static void glyph(StringBuilder text,int index){text.append((char)(0xE000+index));}
    private static void space(StringBuilder text,int pixels){while(pixels!=0){int step=Math.max(-16,Math.min(16,pixels));text.append((char)(0xE200+step+16));pixels-=step;}}
    private static Component spacing(int width){var text=new StringBuilder();space(text,width);return Component.literal(text.toString()).setStyle(HEART_STYLE);}
    private static Component number(String value,int color){var text=new StringBuilder();for(int i=0;i<value.length();i++){int digit=DIGITS.indexOf(value.charAt(i));if(digit<0)throw new IllegalArgumentException(value);text.append((char)(0xE100+digit));space(text,NUMBER_CELL-NUMBER_ADVANCES[digit]);}return Component.literal(text.toString()).setStyle(HEART_STYLE.withColor(color));}
    private static String healthNumber(int score){return Integer.toString(score/2+score%2);}
    private static int heartWidth(int score){if(score<=0)return 0;int slots=Math.max(score,20)/2,step=Math.min(86/slots,9);return step<=3?healthNumber(score).length()*NUMBER_CELL:(Math.max((score+1)/2,slots)-1)*step+9;}
    static Component score(int health,int ping){
        var result=hearts(health).copy().append(spacing(90-heartWidth(health)+4));String value=ping<0?"-":Integer.toString(ping);
        int color=ping<0?0xAAAAAA:ping<150?0x55FF55:ping<300?0xFFFF55:0xFF5555;
        return result.append(spacing((5-value.length())*NUMBER_CELL)).append(number(value,color));
    }
    static Component hearts(int score){
        if(score<=0)return Component.empty();int full=(score+1)/2,slots=Math.max(score,20)/2,step=Math.min(86/slots,9);
        if(step<=3)return number(healthNumber(score),0x00FF00);
        var text=new StringBuilder();int count=Math.max(full,slots);
        for(int i=0;i<count;i++){
            glyph(text,0);int end=ADVANCES[0];
            if(i<full){space(text,-end);int fill=i*2+1<score?(i>=10?3:1):(i>=10?4:2);glyph(text,fill);end=ADVANCES[fill];}
            space(text,(i==count-1?9:step)-end);
        }
        return Component.literal(text.toString()).setStyle(HEART_STYLE);
    }
    static void tick(MinecraftServer server){
        if(owner!=server){stop();owner=server;}
        long now=System.nanoTime();if(header==null||now-sampled>=1_000_000_000L){
            sampled=now;double target=server.tickRateManager().isFrozen()?0:server.tickRateManager().tickrate();double value=ServerTickMetrics.tps(60_000_000_000L,now,target);
            var color=target<=0?ChatFormatting.GRAY:value>=target*.9?ChatFormatting.GREEN:value>=target*.75?ChatFormatting.YELLOW:ChatFormatting.RED;
            header=Component.literal("TPS: ").withStyle(ChatFormatting.AQUA).append(Component.literal(String.format(Locale.ROOT,"%.1f",value)).withStyle(color));
        }
        HEADERS.keySet().removeIf(c->c.player.connection!=c||server.getPlayerList().getPlayer(c.player.getUUID())!=c.player);
        for(var player:server.getPlayerList().getPlayers()){var c=player.connection;if(c!=null&&c.hasClientLoaded()&&!header.equals(HEADERS.get(c))){c.send(new ClientboundTabListPacket(header,Component.empty()));HEADERS.put(c,header);}}
    }
    static void forget(ServerPlayer player){HEADERS.keySet().removeIf(c->c.player==player);}
    static void stop(){HEADERS.clear();owner=null;sampled=0;header=null;}
}
