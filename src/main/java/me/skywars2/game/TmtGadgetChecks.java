package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

/** Grouped real input/continuation checks for the three formerly missing legacy owners. */
public final class TmtGadgetChecks {
    private TmtGadgetChecks(){}
    private static Scene running;
    private static GameController requested;
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT gadgets: "+why);}
    public static void run(GameController game){require(running==null&&requested==null,"exclusive native scene");requested=game;}
    public static void tick(GameController game){if(requested!=null){require(requested==game,"queued controller");requested=null;var start=new Scene(game);running=start;try{start.begin();}catch(Throwable e){running=null;try{start.close();}catch(Throwable c){e.addSuppressed(c);}SkyWars2.LOGGER.error("[check] FAIL tmt-gadgets",e);}return;}var scene=running;if(scene==null)return;try{require(scene.game==game,"same controller");scene.f.client.acknowledge();scene.f.audience.acknowledge();if(!scene.step())return;running=null;scene.close();SkyWars2.LOGGER.info("[check] PASS tmt-gadgets (three current native factories and private TMT catalog entries; native cumulative metric and additive curse; TECH no-spend/MAG distinction; real riding Arrow ticks, TimeStop, frozen source-policy/age continuation and quiet timeout; all six authored mask payloads and native Troll Box source activation/private debit; source reward and outer sibling preservation)");}catch(Throwable e){running=null;try{scene.close();}catch(Throwable c){e.addSuppressed(c);}SkyWars2.LOGGER.error("[check] FAIL tmt-gadgets",e);}}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final Vec3 origin;
        final NativeMeleeChecks.NativeStats used;
        final Map<UUID,Integer> metric=new HashMap<>();final int nativeDamage;
        ItemStack stack,before;Arrow rocket;int phase,when,start,rocketCase,radarCase,trollCase;boolean closed;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);origin=f.home.add(0,f.level.getMaxY()+24-f.home.y,0);
            var objective=game.server().getScoreboard().getObjective(LbcGayradar.DAMAGE_OBJECTIVE);require(objective!=null,"native damage metric registered at startup");
            for(var p:game.server().getPlayerList().getPlayers())metric.put(p.getUUID(),LbcGayradar.damage(game,p));
            nativeDamage=f.owner.getStats().getValue(Stats.CUSTOM.get(Stats.DAMAGE_DEALT));
            used=new NativeMeleeChecks.NativeStats(f.owner,List.of(LbcGayradar.TOKEN,LbcSteampunkRocket.TOKEN,LbcTrollBox.TOKEN,LbcTmtItems.prototype(0).getItem(),LbcTmtItems.prototype(1).getItem()));}
        LbcTmtAbilities.Ability ability(Item item){return game.items().tmt().catalog().families().stream().flatMap(row->row.variants().stream()).filter(p->LbcTmtComponents.nativeItem(p.data())==item).findFirst().orElseThrow();}
        ItemStack combined(Item item,int carrier){var s=LbcTmtItems.prototype(carrier);var root=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            var sibling=game.items().tmt().catalog().families().stream().flatMap(row->row.variants().stream()).filter(p->((ItemData.IntReader)(Object)p.data()).skywars2$readInt("zaika",0)==1).findFirst().orElseThrow();
            LbcTmtAbilities.write(root,List.of(ability(item),sibling));root.putInt("gayradar",0);root.putInt("steampunk_rocket",0);root.putInt("trollge",0);s.set(DataComponents.CUSTOM_DATA,CustomData.of(root));s.setCount(2);return s;}
        void begin(){f.prepareParticipants();f.move(f.owner,origin);f.move(f.other,origin.add(12,0,0));start=game.server().getTickCount();
            for(var item:List.of(LbcGayradar.TOKEN,LbcSteampunkRocket.TOKEN,LbcTrollBox.TOKEN)){var a=ability(item);require(LbcTmtNativeThrows.canonical(LbcTmtComponents.source(game,a))==item,"actual current factory is a sampled private property");}
            var radar=f.loot("lbc:item_defs/lbc/gayradar__02394082461d");require(radar.is(LbcGayradar.TOKEN),"source leaf produces native Gayradar");
            require(f.loot("lbc:troll_box").is(LbcTrollBox.TOKEN),"source leaf produces native Troll Box");
            for(int n=1;n<=6;n++){var mask=game.items().trollBox().maskPrototype(n);require(LbcTrollBox.mask(mask)==n&&mask.has(DataComponents.EQUIPPABLE),"all six complete authored native mask components "+n);}
            LbcGayradar.resetRound(game);f.owner.awardStat(Stats.CUSTOM.get(Stats.DAMAGE_DEALT),73);require(LbcGayradar.damage(game,f.owner)==73,"native statistic updates dedicated criterion without melee dummy polling");
            require(LbcGayradar.choose(game,f.other)==f.owner,"actual maximum includes native metric");
            LbcGayradar.resetRound(game);require(LbcGayradar.choose(game,f.other)==f.other,"zero metric selects caster");
            radar();}
        void clear(){f.owner.stopUsingItem();f.owner.stopRiding();game.items().steampunkRocket().stop();game.items().trollBox().stop();f.removeFields();game.items().clearTurnCurse(f.owner);game.items().clearTurnCurse(f.other);game.items().mana().score(f.owner,"skill_issue").set(0);game.items().mana().score(f.other,"skill_issue").set(0);f.owner.removeAllEffects();f.other.removeAllEffects();f.owner.getInventory().clearContent();f.move(f.owner,origin);f.move(f.other,origin.add(12,0,0));}
        void held(ItemStack value,InteractionHand hand){stack=value;before=value.copy();f.owner.setItemInHand(hand,value);}
        void preserved(int count){require(ItemStack.matches(stack,before.copyWithCount(count))&&!LbcTmtAbilities.active(stack)&&!ItemData.contains(stack,LbcTmtAbilities.ACTIVE),"full outer source and private sibling preserved; sole native debit");}
        void radar(){clear();LbcGayradar.resetRound(game);var objective=game.server().getScoreboard().getObjective(LbcGayradar.DAMAGE_OBJECTIVE);game.server().getScoreboard().getOrCreatePlayerScore(f.other,objective,true).set(100);
            var hand=radarCase%2==0?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;held(radarCase==0?new ItemStack(LbcGayradar.TOKEN,2):combined(LbcGayradar.TOKEN,0),hand);
            f.field(true,origin);require(!f.owner.gameMode.useItem(f.owner,f.level,stack,hand).consumesAction(),"TECH rejects Gayradar before debit");preserved(2);when=game.server().getTickCount();phase=5;}
        void radarAccepted(){var hand=radarCase%2==0?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;f.removeFields();
            f.field(false,origin);game.items().addTurnCurse(f.other,10);
            require(f.owner.gameMode.useItem(f.owner,f.level,stack,hand).consumesAction(),"real native radar input with MAG and selected private payload case="+radarCase);preserved(1);
            require(game.items().mana().score(f.other,"skill_issue").get()==1810&&f.other.getEffect(MobEffects.SLOWNESS)!=null,"same existing curse owner adds 1800 and native Slow II");
            require(game.items().mana().score(f.owner,"skill_issue").get()==0,"maximum is target, not caster");when=game.server().getTickCount();phase=0;}
        void rocket(){clear();var hand=rocketCase%2==0?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;held(rocketCase==0?new ItemStack(LbcSteampunkRocket.TOKEN,2):combined(LbcSteampunkRocket.TOKEN,0),hand);
            f.field(true,origin);require(!f.owner.gameMode.useItem(f.owner,f.level,stack,hand).consumesAction(),"TECH rejects native rocket before debit");preserved(2);when=game.server().getTickCount();phase=6;}
        void rocketAccepted(){var hand=rocketCase%2==0?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;f.removeFields();f.owner.setXRot(-90);
            require(f.owner.gameMode.useItem(f.owner,f.level,stack,hand).consumesAction(),"real native rocket input");preserved(1);require(game.items().steampunkRocket().count()==1,"one actual owned Arrow");rocket=game.items().steampunkRocket().snapshot().getFirst();f.own(rocket);
            require(f.owner.getVehicle()==rocket&&rocket.isNoGravity(),"native Arrow riding and source flags");when=game.server().getTickCount();phase=1;}
        long seed(int outcome){for(long s=0;s<10000;s++){var r=net.minecraft.util.RandomSource.create(s);if(r.nextInt(6)+1==outcome)return s;}throw new IllegalStateException("source lottery seed");}
        void troll(){clear();var hand=trollCase%2==0?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;held(trollCase==0?new ItemStack(LbcTrollBox.TOKEN,2):combined(LbcTrollBox.TOKEN,0),hand);
            f.owner.getRandom().setSeed(seed(1));f.field(true,origin);
            require(f.owner.gameMode.useItem(f.owner,f.level,stack,hand).consumesAction(),"real Troll Box input is unjammed, including private TMT");preserved(1);
            var v=game.items().trollBox().view();require(v.hunter()&&!v.darkness()&&!v.powerfall()&&!v.abyss(),"seeded native six-way source chooses actual hunter owner");
            require(f.owner.getInventory().contains(s->LbcTrollBox.mask(s)==1),"authored mask reward reaches actual inventory");when=game.server().getTickCount();phase=4;}
        boolean step(){require(game.server().getTickCount()-start<240,"bounded grouped scene phase="+phase);int elapsed=game.server().getTickCount()-when;
            if(phase==5){if(elapsed<1)return false;radarAccepted();return false;}
            if(phase==6){if(elapsed<1)return false;rocketAccepted();return false;}
            if(phase==0){if(elapsed<1)return false;require(game.items().mana().score(f.other,"skill_issue").get()==1809,"existing native curse tick retained under jammer: "+game.items().mana().score(f.other,"skill_issue").get());if(++radarCase<3)radar();else rocket();return false;}
            if(phase==1){if(elapsed<2)return false;require(game.items().steampunkRocket().age(rocket)==2&&rocket.getDeltaMovement().y>.1,"actual native arrow hook steers and ages");
                if(rocketCase==0){f.pause();when=game.server().getTickCount();phase=2;return false;}
                when=game.server().getTickCount();phase=3;return false;}
            if(phase==2){require(game.items().steampunkRocket().age(rocket)==2,"TimeStop holds native flight age");if(elapsed<2)return false;f.thaw();
                var bodies=MatchActorCheckpoint.currentBodies(game);var sources=new MatchDamageSources(game,bodies.values());var checkpoint=new MatchSpellCheckpoint(game,sources);game.server().tickRateManager().setFrozen(true);
                try{checkpoint.prepareFrozen();require(checkpoint.restoreFrozen(bodies::get,false).isEmpty(),"shared native policy and rocket source resolve atomically");checkpoint.publishFrozen();require(game.items().steampunkRocket().age(rocket)==2&&f.owner.getVehicle()==rocket,"frozen private replay retains actual Arrow/passenger and age");}finally{game.server().tickRateManager().setFrozen(false);}
                when=game.server().getTickCount();phase=3;return false;}
            if(phase==3){if(!rocket.isRemoved())return false;require(game.items().steampunkRocket().count()==0&&f.owner.getVehicle()!=rocket,"forty active tick timeout removes rocket quietly");preserved(1);if(++rocketCase<3)rocket();else troll();return false;}
            if(phase==4){if(elapsed<1)return false;require(game.items().trollBox().captureReady(),"idle hunter outcome leaves canonical capture ready");if(++trollCase<3)troll();else return true;return false;}
            throw new IllegalStateException("phase "+phase);}
        public void close(){if(closed)return;closed=true;try{clear();if(f.stopped)f.thaw();}finally{try{f.close();}finally{used.close();f.owner.getStats().setValue(f.owner,Stats.CUSTOM.get(Stats.DAMAGE_DEALT),nativeDamage);var objective=game.server().getScoreboard().getObjective(LbcGayradar.DAMAGE_OBJECTIVE);for(var p:game.server().getPlayerList().getPlayers())game.server().getScoreboard().getOrCreatePlayerScore(p,objective,true).set(metric.getOrDefault(p.getUUID(),0));}}}
    }
}
