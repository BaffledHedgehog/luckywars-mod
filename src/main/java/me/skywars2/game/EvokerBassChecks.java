package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Actual item entry, source geometry, native damage tick/credit and Academy/rhythm fixtures. */
public final class EvokerBassChecks {
    private EvokerBassChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Evoker/Bass: "+why);}
    private static Fixture running;
    private static List<EvokerFangs> stopped=List.of();
    private static String epoch;
    private static final class Fixture implements AutoCloseable {
        boolean checked;int warmTicks;
        boolean warm(){require(++warmTicks<=200,"resident fixture becomes entity ticking");for(int x=-18;x<=-12;x++)for(int z=-18;z<=-12;z++)if(!owner.level().areEntitiesLoaded(net.minecraft.world.level.ChunkPos.pack(x,z)))return false;return true;}
        final GameController game; final ServerPlayer owner,other;
        final GameChecks.WandThirdPlayerState saved,otherSaved;
        final Vec3 at=new Vec3(-240.5,240,-240.5);
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<String,Integer> scores=new LinkedHashMap<>();
        final Map<net.minecraft.stats.Stat<Identifier>,Integer> stats=new LinkedHashMap<>();
        final Set<Long> forced=new HashSet<>();
        final boolean offhand;
        final int skill,trinket,challengeProgress;
        final Set<String> unlocks;
        final Set<Identifier> advancements=new HashSet<>();
        final Map<Identifier,Set<String>> completed=new HashMap<>();
        final net.minecraft.world.scores.PlayerTeam team;
        Fixture(GameController game){
            this.game=game;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing()&&owner!=null&&other!=null,"two active participants");
            saved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());otherSaved=new GameChecks.WandThirdPlayerState(other,game.items().mana());offhand=game.config().offhandWands;
            var profile=game.players().get(owner.getUUID());skill=profile.lbcSkill;trinket=profile.lbcTrinket;challengeProgress=profile.lbcChallengeProgress;unlocks=new HashSet<>(profile.lbcUnlocks);
            for(String n:List.of("bass_combo","bass_combo_ticker","bass_combo_len"))scores.put(n,game.items().mana().score(owner,n).get());
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var stat=net.minecraft.stats.Stats.CUSTOM.get(id);stats.put(stat,owner.getStats().getValue(stat));}
            for(String id:List.of("lbc:true_advancements/staffs/evoker","lbc:true_advancements/staffs/bass")){
                var key=Identifier.parse(id);var a=game.server().getAdvancements().get(key);if(a!=null){advancements.add(key);var prior=new HashSet<String>();owner.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(prior::add);completed.put(key,prior);}
            }
            owner.level().getAllEntities().forEach(before::add);
            team=game.server().getScoreboard().addPlayerTeam("evoker_bass_fixture");
        }
        void init(){
            for(int x=-18;x<=-12;x++)for(int z=-18;z<=-12;z++){long key=net.minecraft.world.level.ChunkPos.pack(x,z);if(!owner.level().getForceLoadedChunks().contains(key)){forced.add(key);owner.level().setChunkForced(x,z,true);}owner.level().getChunk(x,z);}
            owner.closeContainer();owner.getInventory().clearContent();owner.removeAllEffects();owner.setGameMode(GameType.SURVIVAL);owner.setNoGravity(true);
            owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),0,0,true);other.teleportTo(owner.level(),at.x+90,at.y,at.z,Set.of(),0,0,true);other.setNoGravity(true);
            var p=game.players().get(owner.getUUID());p.lbcSkill=0;p.lbcTrinket=0;
            game.items().mana().noMagic(owner,0);game.items().mana().set(owner,100000);game.config().offhandWands=true;
            game.items().evokerBass().forget(owner);
        }
        ItemStack loot(String leaf){var list=game.lbc().rollTable(owner.level(),at,owner,"lbc:ported/"+leaf);require(list.size()==1,"one loot item "+leaf);return list.getFirst();}
        void mode(ItemStack stack,int mode){var d=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();d.putInt("mode",mode);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(d));}
        boolean use(ItemStack stack,InteractionHand hand){owner.setItemInHand(hand,stack);return owner.gameMode.useItem(owner,owner.level(),stack,hand).consumesAction();}
        Cow cow(Vec3 p){var c=new Cow(EntityTypes.COW,owner.level());c.setPos(p);c.setNoAi(true);c.setNoGravity(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);c.setHealth(1000);owner.level().addFreshEntity(c);return c;}
        void clean(){for(var f:game.items().evokerBass().snapshot())f.discard();game.items().evokerBass().tick();}
        void presentation(){for(String name:List.of("evoker_wand","bass_wand"))for(boolean upgraded:new boolean[]{false,true}){
            var s=loot((upgraded?"academy_":"")+name);var d=s.get(DataComponents.CUSTOM_DATA).copyTag();
            require(s.getCount()==1&&net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals("skywars2:"+(upgraded?"academy_":"")+name),"native item leaf");
            require(d.getIntOr("staff",0)==1&&d.getIntOr("magicwand",0)==1&&d.getIntOr("transformable",0)==1&&d.getIntOr(name.equals("evoker_wand")?"evocator":"bassboost",0)==1,"source flags");
            require(Identifier.parse(name.equals("evoker_wand")?"lbc:evokerwand":"lbc:basswand").equals(s.get(DataComponents.ITEM_MODEL)),"source model");
            require(s.get(DataComponents.CUSTOM_NAME).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t&&t.getKey().equals(name)&&!s.getHoverName().getStyle().isItalic(),"source name/style");
            require(s.get(DataComponents.LORE).lines().size()==(name.equals("evoker_wand")?5:8)+(upgraded?1:0),"complete source lore");
        }}
        void formations(){
            var mob=cow(at.add(0,0,6));
            for(int m=1;m<=3;m++){var s=loot("academy_evoker_wand");mode(s,m);game.items().mana().set(owner,10000);require(use(s,InteractionHand.MAIN_HAND),"real use mode "+m);
                var f=game.items().evokerBass().snapshot();require(f.size()==switch(m){case 1->5;case 2->36;default->51;},"exact source formation count "+m);
                require(game.items().mana().get(owner)==9300&&f.stream().allMatch(e->e.getOwner()==owner&&game.items().currentEpoch(e)),"cost and native owners");
                if(m==1)require(f.stream().anyMatch(e->e.position().distanceTo(mob.position().add(1.2,0,0))<1e-6),"target cross offsets");
                if(m==2)require(f.stream().filter(e->e.getY()==at.y+4).count()==12,"three source vertical ring layers");
                if(m==3)require(f.stream().anyMatch(e->e.position().distanceTo(at.add(0,0,20))<1e-6)&&f.stream().anyMatch(e->e.position().distanceTo(at.add(8,0,17))<1e-6),"source forward fan endpoints");
                clean();
            }
            var s=loot("academy_evoker_wand");mode(s,1);use(s,InteractionHand.MAIN_HAND);var f=game.items().evokerBass().snapshot().stream().filter(e->e.position().distanceTo(mob.position())<1e-6).findFirst().orElseThrow();
            for(var extra:game.items().evokerBass().snapshot())if(extra!=f)extra.discard();
            game.items().timeStop().begin(owner.level(),200);
            try{for(int i=0;i<10;i++)owner.level().tickNonPassenger(f);
                var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,f.registryAccess());f.saveWithoutId(out);require(out.buildResult().getIntOr("Warmup",-1)==0,"native fang attack clock freezes through the real world tick gate");
            }finally{game.items().timeStop().end(owner.level());}
            for(int i=0;i<7;i++)f.tick();require(mob.getHealth()==1000,"native damage is not early");f.tick();require(mob.getHealth()==994&&mob.getKillCredit()==owner,"native eighth tick six magic damage with attribution");clean();
            // A friendly mob remains a valid projectile target, with native timing and kill credit.
            game.server().getScoreboard().addPlayerToTeam(owner.getScoreboardName(),team);game.server().getScoreboard().addPlayerToTeam(mob.getScoreboardName(),team);
            mob.setHealth(6);mob.setInvulnerableTime(0);mob.damageCooldownTime=0;use(s,InteractionHand.MAIN_HAND);f=game.items().evokerBass().snapshot().stream().filter(e->e.position().distanceTo(mob.position())<1e-6).findFirst().orElseThrow();for(int i=0;i<8;i++)f.tick();
            require(!mob.isAlive()&&mob.getKillCredit()==owner,"lethal native fang accepts allied mob and preserves killer");clean();mob.discard();
            game.server().getScoreboard().removePlayerFromTeam(owner.getScoreboardName(),team);
            var trialTarget=cow(at.add(0,0,8));var prior=new ArrayList<EvokerFangs>();
            for(int i=0;i<40;i++){var old=new EvokerFangs(owner.level(),trialTarget.getX(),trialTarget.getY(),trialTarget.getZ(),0,0,owner);owner.level().addFreshEntity(old);prior.add(old);}
            var trial=loot("evoker_wand");var flags=trial.get(DataComponents.CUSTOM_DATA).copyTag();flags.putInt("upgradable",1);trial.set(DataComponents.CUSTOM_DATA,CustomData.of(flags));
            use(trial,InteractionHand.OFF_HAND);require(trial.get(DataComponents.CUSTOM_DATA).copyTag().getIntOr("upgrade_available",0)==1,"Evoker trial counts40 existing nearby fangs on the used offhand item");
            prior.forEach(Entity::discard);trialTarget.discard();clean();owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);

            // Admission follows the existing role/time/mana gates without spending on a rejected cast.
            game.items().mana().set(owner,699);require(!use(loot("evoker_wand"),InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==699,"insufficient mana");
            owner.setShiftKeyDown(true);game.items().mana().set(owner,-100);s=loot("academy_evoker_wand");require(use(s,InteractionHand.OFF_HAND)&&s.get(DataComponents.CUSTOM_DATA).copyTag().getIntOr("mode",0)==2&&game.items().mana().get(owner)==-100,"mode change in offhand with negative mana");owner.setShiftKeyDown(false);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            game.items().mana().set(owner,10000);game.items().timeStop().begin(owner.level(),200);try{require(!use(s,InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==10000,"dimension Time Stop rejects cast");}finally{game.items().timeStop().end(owner.level());}
            var marker=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,owner.level());marker.setPos(at);marker.entityTags().add("stopper_magic");owner.level().addFreshEntity(marker);game.items().magic().loaded(marker);
            try{require(!use(s,InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==10000,"antimagic rejects without debit");}finally{game.items().magic().unloaded(marker);marker.discard();}
            owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
        }
        void bass(){
            var mob=cow(at.add(0,0,5));var s=loot("bass_wand");var d=s.get(DataComponents.CUSTOM_DATA).copyTag();d.putInt("upgradable",1);s.set(DataComponents.CUSTOM_DATA,CustomData.of(d));
            for(int i=0;i<80&&!(mob.hasEffect(MobEffects.WEAKNESS)&&mob.hasEffect(MobEffects.SLOWNESS)&&mob.hasEffect(MobEffects.BLINDNESS)&&mob.hasEffect(MobEffects.MINING_FATIGUE));i++){game.items().mana().set(owner,100000);require(use(s,InteractionHand.MAIN_HAND),"normal random bass cast");}
            require(mob.getEffect(MobEffects.WEAKNESS).getDuration()==160&&mob.getEffect(MobEffects.SLOWNESS).getDuration()==240&&mob.getEffect(MobEffects.SLOWNESS).getAmplifier()==3&&mob.getEffect(MobEffects.BLINDNESS).getDuration()==160&&mob.getEffect(MobEffects.MINING_FATIGUE).getDuration()==180,"four exact source debuffs");
            require(s.get(DataComponents.CUSTOM_DATA).copyTag().getIntOr("upgrade_available",0)==1,"four effects on one nearby target complete the used wand's trial");
            mob.removeAllEffects();s=loot("academy_bass_wand");mode(s,3);use(s,InteractionHand.MAIN_HAND);require(!mob.hasEffect(MobEffects.BLINDNESS),"Academy blind mode is player-only as source");
            owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);game.items().evokerBass().forget(owner);mode(s,1);game.items().mana().set(owner,10000);
            require(use(s,InteractionHand.MAIN_HAND),"first beat");for(int i=0;i<10;i++)game.items().evokerBass().tick();require(use(s,InteractionHand.MAIN_HAND),"second beat");for(int i=0;i<10;i++)game.items().evokerBass().tick();
            float hp=mob.getHealth();require(use(s,InteractionHand.MAIN_HAND)&&game.items().mana().score(owner,"bass_combo").get()==3&&game.items().mana().get(owner)==8100&&mob.getHealth()==hp-3,"third rhythmic cast deals combo3 and refunds500");
            for(int i=0;i<20;i++)game.items().evokerBass().tick();require(use(s,InteractionHand.MAIN_HAND)&&game.items().mana().score(owner,"bass_combo").get()==1,"failed beat starts new source combo");
            // The two hands each cast/pay independently; items are never replaced during use.
            var left=loot("academy_bass_wand");owner.setItemInHand(InteractionHand.OFF_HAND,left);int mana=game.items().mana().get(owner);require(use(s,InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==mana-1600&&owner.getOffhandItem()==left,"dual dispatch and separate costs");
            mob.discard();owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
        }
        int academyPhase; long lastTime=Long.MIN_VALUE;
        net.minecraft.world.entity.Interaction panel;
        boolean academyStep(){
            long now=owner.level().getGameTime();if(now==lastTime)return false;lastTime=now;
            if(panel==null){panel=new net.minecraft.world.entity.Interaction(EntityTypes.INTERACTION,owner.level());panel.entityTags().add("magic_academy");panel.setPos(at);owner.level().addFreshEntity(panel);}
            String name=academyPhase<2?"evoker_wand":"bass_wand";
            if(academyPhase%2==0){var s=loot(name);owner.setItemInHand(InteractionHand.MAIN_HAND,s);require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&LbcAcademy.trial(s),"Academy admits "+name);game.items().academy().ready(owner,InteractionHand.MAIN_HAND,s);}
            else {require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel),"Academy promotes "+name);
                require(owner.getMainHandItem().is(name.equals("evoker_wand")?LbcWands.ACADEMY_EVOKER:LbcWands.ACADEMY_BASS)&&Identifier.parse("lbc:tier_b").equals(owner.getMainHandItem().get(DataComponents.TOOLTIP_STYLE)),"native promotion retains source tier");}
            return ++academyPhase==4;
        }
        public void close(){
            game.items().timeStop().end(owner.level());game.items().evokerBass().forget(owner);
            var fresh=new ArrayList<Entity>();owner.level().getAllEntities().forEach(e->{if(!before.contains(e)&&e.position().distanceToSqr(at)<40000)fresh.add(e);});fresh.forEach(Entity::discard);
            game.server().getScoreboard().removePlayerTeam(team);saved.restore(game);otherSaved.restore(game);scores.forEach((n,v)->game.items().mana().score(owner,n).set(v));stats.forEach((s,v)->owner.getStats().setValue(owner,s,v));
            var p=game.players().get(owner.getUUID());p.lbcSkill=skill;p.lbcTrinket=trinket;p.lbcChallengeProgress=challengeProgress;p.lbcUnlocks.clear();p.lbcUnlocks.addAll(unlocks);game.config().offhandWands=offhand;
            for(var key:advancements){var a=game.server().getAdvancements().get(key);for(String c:a.value().criteria().keySet())if(!completed.get(key).contains(c))owner.getAdvancements().revoke(a,c);}
            game.items().indirectKills().clear(owner);
            for(long key:forced)owner.level().setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
        }
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");var f=new Fixture(game);running=f;try{f.init();}catch(RuntimeException|Error error){running=null;f.close();throw error;}}
    public static void tick(GameController game){if(running==null)return;var f=running;try{if(!f.checked){if(!f.warm())return;f.presentation();f.formations();f.bass();f.checked=true;}if(f.academyStep()){running=null;f.close();SkyWars2.LOGGER.info("[check] PASS evoker-bass");}}catch(Throwable e){running=null;try{f.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL evoker-bass",e);}}
    public static void stopProbe(GameController game){
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));var state=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
        try{owner.setShiftKeyDown(false);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);game.items().mana().noMagic(owner,0);game.items().mana().set(owner,10000);var s=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/academy_evoker_wand").getFirst();var d=s.get(DataComponents.CUSTOM_DATA).copyTag();d.putInt("mode",2);s.set(DataComponents.CUSTOM_DATA,CustomData.of(d));owner.setItemInHand(InteractionHand.MAIN_HAND,s);require(game.items().wands().use(owner,InteractionHand.MAIN_HAND),"stop probe cast");stopped=game.items().evokerBass().snapshot();require(stopped.size()==36,"live fangs before actual stop");epoch=game.items().epoch();}finally{state.restore(game);}SkyWars2.LOGGER.info("[check] PASS evoker-bass-stop-probe");
    }
    public static void cleanup(GameController game){require(epoch!=null&&!game.items().currentEpoch(epoch)&&stopped.size()==36&&stopped.stream().allMatch(Entity::isRemoved)&&game.items().evokerBass().snapshot().isEmpty(),"actual match stop clears native fangs");stopped=List.of();epoch=null;SkyWars2.LOGGER.info("[check] PASS evoker-bass-cleanup");}
}
