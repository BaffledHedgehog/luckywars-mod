package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.scores.Objective;

/** Native component rendering and immutable scalar reads; no item grants or world writes. */
public final class CombatFeedbackChecks {
    private CombatFeedbackChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Combat feedback: "+why);}
    private record Glyph(int codepoint,Integer color){}
    private record SavedScore(Objective objective,Integer value){}
    private static List<Glyph> glyphs(Component component){
        var result=new ArrayList<Glyph>();
        component.getVisualOrderText().accept((index,style,codepoint)->{
            result.add(new Glyph(codepoint,style.getColor()==null?null:style.getColor().getValue()));return true;
        });
        return result;
    }
    private static String text(List<Glyph> glyphs){
        var result=new StringBuilder();for(var glyph:glyphs)result.appendCodePoint(glyph.codepoint);return result.toString();
    }
    private static void color(List<Glyph> glyphs,int start,int end,int expected,String why){
        for(int index=start;index<end;index++)require(Objects.equals(glyphs.get(index).color,expected),why+" at "+index);
    }
    private static void hud(GameController game,ServerPlayer player,int maximum,int regeneration){
        var mana=game.items().mana();
        mana.set(player,25);mana.maximum(player,maximum);mana.score(player,"manaregen_visual").set(regeneration);
        mana.score(player,"reverse_mana").set(0);
        int objectives=game.server().getScoreboard().getObjectives().size();
        var glyphs=glyphs(mana.hud(player));var actual=text(glyphs);
        String max=Integer.toString(maximum),digits=Long.toString(Math.abs((long)regeneration));
        String expected="\uE100 25/"+max+" ["+(regeneration<0?"-":"+")+digits+"]";
        require(actual.equals(expected),"exact source glyph/separators/signed magnitude, including MIN_VALUE: "+maximum+"/"+regeneration+" got="+actual);
        int slash=actual.indexOf('/'),open=actual.indexOf('['),close=actual.indexOf(']');
        color(glyphs,2,slash,0x55ffff,"positive current mana retains aqua");
        color(glyphs,slash,slash+1,0x55ff55,"slash retains source green");
        color(glyphs,slash+1,slash+1+max.length(),maximum<0?0xff5555:0x55ffff,"only maximum number changes color");
        color(glyphs,open,open+1,0x55ff55,"opening bracket stays green");
        color(glyphs,open+1,open+2,regeneration<0?0xff5555:0x55ff55,"regeneration minus red/positive plus green");
        color(glyphs,open+2,close,regeneration<0?0xff5555:0x55ffff,"regeneration digits red only when negative");
        color(glyphs,close,close+1,0x55ff55,"closing bracket stays green");
        require(game.server().getScoreboard().getObjectives().size()==objectives&&mana.get(player)==25
                &&mana.maximum(player)==maximum&&mana.regeneration(player)==regeneration,"HUD rendering leaves native resource state unchanged");
    }
    private static void itemData(){
        require(ItemData.intValue(ItemStack.EMPTY,"flag",37)==37,"empty stack fallback");
        var bare=new ItemStack(Items.PAPER);
        require(ItemData.intValue(bare,"flag",37)==37,"absent custom component fallback");
        var tag=new CompoundTag();tag.putByte("byte",(byte)1);tag.putShort("short",(short)-23);tag.putInt("int",Integer.MIN_VALUE);
        tag.putLong("long",4294967297L);tag.putFloat("float",-7.75f);tag.putDouble("double",19.5);tag.putString("text","1");
        bare.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));var before=bare.copy();
        for(String key:List.of("byte","short","int","long","float","double","text","missing"))
            require(ItemData.intValue(bare,key,37)==tag.getIntOr(key,37),"native NumericTag conversion/fallback parity: "+key);
        require(ItemData.intValue(bare,"byte",37)==1&&ItemData.intValue(bare,"int",37)==Integer.MIN_VALUE,
                "Byte and Int source flags share native compatible reads");
        require(ItemData.intValue(bare,"text",37)==37&&ItemData.intValue(bare,"missing",37)==37,"wrong scalar type/missing key use caller fallback");
        require(ItemStack.matches(before,bare)&&bare.get(DataComponents.CUSTOM_DATA).copyTag().equals(tag),"reads do not mutate item components or NBT");
        for(int value=-4;value<=4;value++){
            var fresh=tag.copy();if((value&1)==0)fresh.putByte("flag",(byte)value);else fresh.putInt("flag",value);
            bare.set(DataComponents.CUSTOM_DATA,CustomData.of(fresh));
            require(ItemData.intValue(bare,"flag",37)==value,"fresh component replacements never return cached values");
        }
        bare.remove(DataComponents.CUSTOM_DATA);
        require(ItemData.intValue(bare,"flag",37)==37,"removed component immediately restores fallback");
        require(before.get(DataComponents.CUSTOM_DATA).copyTag().equals(tag),"updates to tested stack never mutate retained component snapshot");
    }
    public static void run(GameController game){
        itemData();
        var player=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(player!=null&&game.playing(),"actual active fixture player");
        var board=game.server().getScoreboard();var saved=new LinkedHashMap<String,SavedScore>();
        for(String key:List.of("mana","manamax","manaregen_visual","reverse_mana")){
            var objective=board.getObjective(key);var score=objective==null?null:board.getPlayerScoreInfo(player,objective);
            saved.put(key,new SavedScore(objective,score==null?null:score.value()));
        }
        try{
            for(int maximum:new int[]{5000,0,-1,Integer.MIN_VALUE})
                for(int regen:new int[]{10,0,-1,Integer.MIN_VALUE})hud(game,player,maximum,regen);
        }finally{
            saved.forEach((key,state)->{
                var objective=board.getObjective(key);if(objective==null)return;
                if(state.objective==null)board.removeObjective(objective);
                else if(state.value==null)board.resetSinglePlayerScore(player,objective);
                else board.getOrCreatePlayerScore(player,objective).set(state.value);
            });
            game.items().invalidateHud(player);
        }
        SkyWars2.LOGGER.info("[check] PASS combat-feedback (16 native signed mana component/style cases, immutable typed item-data reads/fresh replacements)");
    }
}
