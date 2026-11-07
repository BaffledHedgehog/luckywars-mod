package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringDecomposer;
import net.minecraft.world.phys.Vec3;

/** Actual HUD packets and independent native Imprinted arrivals, without a Troll Box event. */
public final class ImprintedHudChecks {
    private ImprintedHudChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Imprinted/HUD: "+why);}
    private record Row(String counter,char glyph,ChatFormatting color){}
    private static final List<Row> ROWS=List.of(
        new Row("dimensional_shifting",'\uE125',ChatFormatting.WHITE),
        new Row("all_seeing_eye",'\uE127',ChatFormatting.GREEN),new Row("living_legend",'\uE128',ChatFormatting.GREEN),
        new Row("sprint_cooldown",'\uE136',ChatFormatting.WHITE),new Row("effect_explosion",'\uE140',ChatFormatting.RED),
        new Row("effect_big_explosion",'\uE141',ChatFormatting.RED),new Row("effect_traps",'\uE143',ChatFormatting.RED),
        new Row("effect_dimshifting",'\uE149',ChatFormatting.WHITE),new Row("effect_witch_gens_reset",'\uE150',ChatFormatting.RED),
        new Row("effect_hedgehog_boom",'\uE151',ChatFormatting.RED),new Row("effect_stand_arrow_hit",'\uE152',ChatFormatting.RED),
        new Row("effect_nuking",'\uE153',ChatFormatting.RED),new Row("effect_shock_potion",'\uE156',ChatFormatting.WHITE),
        new Row("effect_snus",'\uE161',ChatFormatting.WHITE),new Row("effect_rtp_4",'\uE142',ChatFormatting.WHITE));
    private record Glyph(int code,Style style){}
    private static List<Glyph> glyphs(Component text){var out=new ArrayList<Glyph>();StringDecomposer.iterateFormatted(text,Style.EMPTY,(i,s,c)->{out.add(new Glyph(c,s));return true;});return out;}
    public static void hud(GameController game){
        LbcHudIconChecks.main(new String[0]);
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();var mana=game.items().mana();
            for(var row:ROWS)mana.score(f.owner,row.counter).set(0);
            // Test each row in isolation: shared icon IDs must not mask a missing counter.
            for(var row:ROWS){
                mana.score(f.owner,row.counter).set(1739);game.items().invalidateHud(f.owner);
                var packets=(List<?>)WoolBucketChecks.read(f.client.connection,"packets");int before=packets.size();
                game.items().refreshHud(f.owner);
                var packet=packets.subList(before,packets.size()).stream().filter(ClientboundSetActionBarTextPacket.class::isInstance)
                        .map(ClientboundSetActionBarTextPacket.class::cast).findFirst().orElseThrow();
                var glyphs=glyphs(packet.text());var visible=new StringBuilder();for(var g:glyphs)visible.appendCodePoint(g.code);
                String needle="  "+row.glyph+" 1739";int at=visible.indexOf(needle);
                require(at>=0,"original prefix/value "+row.counter);
                var icon=glyphs.get(at+2);require(icon.code==row.glyph&&icon.style.getColor().getValue()==LbcHudIcons.COLOR,"shader marker "+row.counter);
                for(int i=at+4;i<at+8;i++)require(glyphs.get(i).style.getColor().getValue()==net.minecraft.network.chat.TextColor.fromLegacyFormat(row.color).getValue(),"legacy number color "+row.counter);
                mana.score(f.owner,row.counter).set(0);
                require(!game.items().hud(f.owner).getString().contains(needle),"zero counter hidden "+row.counter);
            }
            mana.score(f.owner,"cold_cd").set(1739);
            require(!game.items().hud(f.owner).getString().contains("1739"),"internal cold cooldown remains hidden");
        }
        SkyWars2.LOGGER.info("[check] PASS legacy-hud (15 original counter rows in actual actionbar packets, icon marker and independent number colors; hidden cold_cd; author 512-glyph checks)");
    }
    private static Fixture running;
    public static void start(GameController game){require(running==null,"exclusive return scene");var f=new Fixture(game);running=f;try{f.begin();}catch(Throwable e){running=null;try{f.close();}catch(Throwable x){e.addSuppressed(x);}throw e;}}
    public static void tick(GameController game){if(running==null)return;var f=running;try{if(!f.step())return;running=null;f.close();SkyWars2.LOGGER.info("[check] PASS imprinted-return (real 900-active-tick visit, staggered native arrivals without Troll Box; TimeStop pause; same-world teleport, exit/reentry and missing persisted counter)");}catch(Throwable e){running=null;try{f.close();}catch(Throwable x){e.addSuppressed(x);}SkyWars2.LOGGER.error("[check] FAIL imprinted-return",e);}}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final ServerLevel imprinted;
        int phase,when,a,b;boolean stopped;
        Fixture(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);imprinted=Objects.requireNonNull(game.server().getLevel(LbcHedgehogDimensions.IMPRINTED));}
        int left(ServerPlayer p){return game.items().mana().hudValue(p,LbcHedgehogDimensions.RETURN_COUNTER);}
        void counter(ServerPlayer p,int value){game.items().mana().score(p,LbcHedgehogDimensions.RETURN_COUNTER).set(value);}
        boolean inside(ServerPlayer p){return p.level()==imprinted;}
        void ack(){f.client.acknowledge();f.audience.acknowledge();}
        void move(ServerPlayer p,ServerLevel world){var at=new Vec3(f.home.x,100,f.home.z);require(p.teleportTo(world,at.x,at.y,at.z,Set.of(),0,0,true),"actual native world teleport");p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.setKnownMovement(Vec3.ZERO);ack();}
        void next(int n){phase=n;when=game.server().getTickCount();a=left(f.owner);b=left(f.other);}
        void begin(){
            f.prepareParticipants();var event=game.items().trollBox().view();require(!event.darkness()&&!event.hunter()&&!event.powerfall()&&!event.abyss(),"no borrowed Troll Box event");
            require(game.items().hedgehogDimensions().teleport(f.owner,f.owner,4),"real shared shifting destination4");
            ack();require(inside(f.owner)&&left(f.owner)==900,"world-change callback installs full 45 seconds");next(0);
        }
        boolean step(){
            ack();int age=game.server().getTickCount()-when;
            switch(phase){
                case 0->{require(left(f.owner)==899||left(f.owner)==900,"initial native tick boundary");next(1);}
                case 1->{
                    require(inside(f.owner)&&left(f.owner)==a-age,"owner alone has one active decrement per native tick");
                    require(!inside(f.other)&&left(f.other)==0,"unrelated outside player unaffected");
                    if(age==20){move(f.other,imprinted);require(left(f.other)==900,"independent direct entrant full timer");next(2);}
                }
                case 2->{
                    require(left(f.owner)==a-age&&left(f.other)==b-age,"staggered personal clocks");
                    if(age==20){game.items().timeStop().begin(imprinted,10000);stopped=true;next(3);}
                }
                case 3->{
                    require(left(f.owner)==a&&left(f.other)==b&&inside(f.owner)&&inside(f.other),"TimeStop pauses both visits");
                    if(age==20){game.items().timeStop().end(imprinted);stopped=false;next(4);}
                }
                case 4->{
                    require(age<a?inside(f.owner)&&left(f.owner)==a-age:!inside(f.owner)&&left(f.owner)==0,"owner returns exactly after 900 active residence ticks");
                    require(age<b?inside(f.other)&&left(f.other)==b-age:!inside(f.other)&&left(f.other)==0,"late entrant keeps separate full residence");
                    if(age==b){move(f.owner,imprinted);require(left(f.owner)==900,"new entry resets");counter(f.owner,123);move(f.owner,imprinted);require(left(f.owner)==123,"same-world teleport keeps visit");move(f.owner,f.level);require(left(f.owner)==0,"exit clears visit immediately");move(f.owner,imprinted);require(left(f.owner)==900,"same-tick exit/reentry starts new visit");counter(f.owner,0);next(5);}
                }
                case 5->{require(inside(f.owner)&&left(f.owner)==900,"already-resident player with missing legacy score gets personal timer");counter(f.owner,1);next(6);}
                case 6->{require(!inside(f.owner)&&left(f.owner)==0,"fallback visit also uses general return owner");return true;}
                default->throw new IllegalStateException("Unknown return phase");
            }
            return false;
        }
        @Override public void close(){try{if(stopped)game.items().timeStop().end(imprinted);}finally{f.close();}}
    }
}
