package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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

/** Native instant casts, harmful-effect attribution, resident surface selection and real Academy/stop fixtures. */
public final class EnderWikiChecks {
    private EnderWikiChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Ender/Wiki: "+why);}
    private static Fixture running;
    private static GameChecks.WandThirdPlayerState stopState;
    private static String epoch;
    private static final class Fixture implements AutoCloseable {
        boolean checked;int warmTicks;
        boolean warm(){require(++warmTicks<=200,"resident fixture becomes entity ticking");for(int x=-18;x<=-12;x++)for(int z=-18;z<=-12;z++)if(!owner.level().areEntitiesLoaded(net.minecraft.world.level.ChunkPos.pack(x,z)))return false;return true;}
        final GameController game; final ServerPlayer owner,other;
        final GameChecks.WandThirdPlayerState saved,otherSaved;
        final Vec3 at=new Vec3(-240.5,240,-240.5);
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final Map<net.minecraft.stats.Stat<Identifier>,Integer> stats=new LinkedHashMap<>();
        final Set<Long> forced=new HashSet<>();
        final boolean offhand;
        final boolean noBorders;
        final Map<String,Integer> scores=new HashMap<>();
        final int skill,trinket,challengeProgress;
        final Set<String> unlocks;
        final Set<Identifier> advancements=new HashSet<>();
        final Map<Identifier,Set<String>> completed=new HashMap<>();
        final net.minecraft.world.scores.PlayerTeam team;
        Fixture(GameController game){
            this.game=game;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing()&&owner!=null&&other!=null,"two active participants");
            saved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());otherSaved=new GameChecks.WandThirdPlayerState(other,game.items().mana());offhand=game.config().offhandWands;noBorders=game.config().noBorders;
            for(var key:List.of("falltimer","effect_rtp_1","effect_rtp_2","effect_rtp_3"))scores.put(key,game.items().mana().score(owner,key).get());
            var profile=game.players().get(owner.getUUID());skill=profile.lbcSkill;trinket=profile.lbcTrinket;challengeProgress=profile.lbcChallengeProgress;unlocks=new HashSet<>(profile.lbcUnlocks);
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var stat=net.minecraft.stats.Stats.CUSTOM.get(id);stats.put(stat,owner.getStats().getValue(stat));}
            for(String id:List.of("lbc:true_advancements/staffs/ender_random","lbc:true_advancements/staffs/badwiki")){
                var key=Identifier.parse(id);var a=game.server().getAdvancements().get(key);if(a!=null){advancements.add(key);var prior=new HashSet<String>();owner.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(prior::add);completed.put(key,prior);}
            }
            owner.level().getAllEntities().forEach(before::add);
            team=game.server().getScoreboard().addPlayerTeam("ender_wiki_fixture");
        }
        void init(){
            for(int x=-18;x<=-12;x++)for(int z=-18;z<=-12;z++){long key=net.minecraft.world.level.ChunkPos.pack(x,z);if(!owner.level().getForceLoadedChunks().contains(key)){forced.add(key);owner.level().setChunkForced(x,z,true);}owner.level().getChunk(x,z);}
            owner.closeContainer();owner.getInventory().clearContent();owner.removeAllEffects();owner.setGameMode(GameType.SURVIVAL);owner.setNoGravity(true);
            owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),0,0,true);other.teleportTo(owner.level(),at.x+90,at.y,at.z,Set.of(),0,0,true);other.setNoGravity(true);
            var p=game.players().get(owner.getUUID());p.lbcSkill=0;p.lbcTrinket=0;
            game.items().mana().noMagic(owner,0);game.items().mana().set(owner,100000);game.config().offhandWands=true;game.config().noBorders=true;
            owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);owner.setHealth(1000);owner.setAbsorptionAmount(0);
        }
        ItemStack loot(String leaf){var list=game.lbc().rollTable(owner.level(),at,owner,"lbc:ported/"+leaf);require(list.size()==1,"one loot item "+leaf);return list.getFirst();}
        void mode(ItemStack stack,int mode){var d=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();d.putInt("mode",mode);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(d));}
        boolean use(ItemStack stack,InteractionHand hand){owner.setItemInHand(hand,stack);return owner.gameMode.useItem(owner,owner.level(),stack,hand).consumesAction();}
        Cow cow(Vec3 p){var c=new Cow(EntityTypes.COW,owner.level());c.setPos(p);c.setNoAi(true);c.setNoGravity(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);c.setHealth(1000);owner.level().addFreshEntity(c);return c;}
        int flag(ItemStack s,String key){return s.get(DataComponents.CUSTOM_DATA).copyTag().getIntOr(key,0);}
        void trial(ItemStack s){var d=s.get(DataComponents.CUSTOM_DATA).copyTag();d.putInt("upgradable",1);s.set(DataComponents.CUSTOM_DATA,CustomData.of(d));}
        void resetHealth(){owner.removeAllEffects();owner.setHealth(1000);owner.setInvulnerableTime(0);owner.damageCooldownTime=0;owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);}
        void effect(LivingEntity target,net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> type,int ticks,int amplifier){require(target.hasEffect(type)&&target.getEffect(type).getDuration()==ticks&&target.getEffect(type).getAmplifier()==amplifier,"exact native "+type.getRegisteredName()+" expected="+ticks+"/"+amplifier+" actual="+target.getEffect(type)+" all="+target.getActiveEffects());}
        void block(BlockPos pos,net.minecraft.world.level.block.state.BlockState value){blocks.putIfAbsent(pos,game.maps().snapshot(owner.level(),pos));owner.level().setBlock(pos,value,net.minecraft.world.level.block.Block.UPDATE_CLIENTS);}
        void clean(){resetHealth();game.items().magic().colors().clearEffects(owner);owner.setShiftKeyDown(false);game.items().mana().set(owner,100000);}
        void presentation(){for(var leaf:List.of("ender_wand","academy_ender_wand","bad_wiki","academy_bad_wiki")){
            var s=loot(leaf);boolean upgraded=leaf.startsWith("academy_"),wiki=leaf.endsWith("bad_wiki");
            require(s.is(wiki?(upgraded?LbcWands.ACADEMY_BAD_WIKI:LbcWands.BAD_WIKI):(upgraded?LbcWands.ACADEMY_ENDER:LbcWands.ENDER))&&flag(s,wiki?"badwiki":"enderwand")==1&&flag(s,"magicwand")==1,"source public item/flags");
            require(Identifier.parse("lbc:"+(wiki?"badwiki":"enderwand")).equals(s.get(DataComponents.ITEM_MODEL))&&Identifier.parse("lbc:tier_"+(upgraded?"a":wiki?"u":"b")).equals(s.get(DataComponents.TOOLTIP_STYLE))&&s.get(DataComponents.LORE).lines().size()==(wiki?6:4)+(upgraded?1:0),"source model/tier/full lore");
        }}
        void outcomes(){
            int[] seeds=new int[84];Arrays.fill(seeds,-1);for(int seed=0;java.util.Arrays.stream(seeds).anyMatch(v->v<0);seed++)seeds[net.minecraft.util.RandomSource.create(seed).nextInt(84)]=seed;
            for(int index=0;index<84;index++){
                clean();var randomBook=loot("bad_wiki");owner.getRandom().setSeed(seeds[index]);require(use(randomBook,InteractionHand.OFF_HAND)&&game.items().mana().get(owner)==99500,"actual random cost outcome "+index);
                var e=LbcEnderWiki.RANDOM[index];var type=net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(Identifier.withDefaultNamespace(e.id())).orElseThrow();
                if(index<81){effect(owner,type,e.ticks(),e.amplifier());require(owner.getEffect(type).isVisible()==!e.hidden(),"source visibility");}
                else require(!owner.hasEffect(MobEffects.RAID_OMEN)&&game.items().mana().score(owner,"effect_rtp_"+(index-80)).get()==400,"sentinel becomes cumulative RTP, no vanilla raid");
            }
            clean();var damageBook=loot("academy_bad_wiki");mode(damageBook,9);var damageData=damageBook.get(DataComponents.CUSTOM_DATA).copyTag();damageData.putInt("tier",3);damageBook.set(DataComponents.CUSTOM_DATA,CustomData.of(damageData));require(use(damageBook,InteractionHand.OFF_HAND),"native damage book cast");MobEffects.INSTANT_DAMAGE.value().applyEffectTick(owner.level(),owner,2);require(owner.getHealth()==976,"native Instant Damage III applies24 HP");
            require(LbcEnderWiki.RANDOM[55].ticks()==2000&&LbcEnderWiki.RANDOM[19].amplifier()==3&&LbcEnderWiki.RANDOM[46].amplifier()==1,"source anomalies retained, not inferred from tier");
            for(int m=1;m<=28;m++)for(int tier=1;tier<=3;tier++){
                clean();var s=loot("academy_bad_wiki");mode(s,m);var d=s.get(DataComponents.CUSTOM_DATA).copyTag();d.putInt("tier",tier);s.set(DataComponents.CUSTOM_DATA,CustomData.of(d));require(use(s,InteractionHand.OFF_HAND),"selectable mode/tier cast");
                var e=LbcEnderWiki.ACADEMY[m-1];if(m==28)require(game.items().mana().score(owner,"effect_rtp_"+tier).get()==400,"all Academy RTP tiers");
                else effect(owner,net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(Identifier.withDefaultNamespace(e.id())).orElseThrow(),e.ticks(),tier-1);
            }
            clean();var trial=loot("bad_wiki");trial(trial);for(var e:LbcEnderWiki.ACADEMY)if(!Set.of("instant_health","instant_damage","saturation","raid_omen").contains(e.id()))owner.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(Identifier.withDefaultNamespace(e.id())).orElseThrow(),600,0),owner);
            require(use(trial,InteractionHand.OFF_HAND)&&flag(trial,"upgrade_available")==1,"all24 effects unlock actually used offhand book");clean();
        }
        void teleports(){
            var base=BlockPos.containing(at).below();for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)block(base.offset(x,0,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            for(int i=0;i<20;i++){var dest=LbcSurfaceTeleport.destination(owner.level(),at,2,owner.level().getMaxY()+1,owner.getRandom());require(dest!=null&&Math.abs(dest.x-at.x)<=100&&Math.abs(dest.z-at.z)<=100&&owner.level().getBlockState(BlockPos.containing(dest).below()).is(net.minecraft.world.level.block.Blocks.STONE),"finite safe resident surface");}
            var far=new Vec3(1000000,240,1000000);require(!owner.level().hasChunkAt(BlockPos.containing(far))&&LbcSurfaceTeleport.destination(owner.level(),far,100,320,owner.getRandom())==null&&!owner.level().hasChunkAt(BlockPos.containing(far)),"empty/unloaded square remains unloaded");
            require(LbcSurfaceTeleport.destination(owner.level(),at,10,120,owner.getRandom())==null,"under-ceiling rejects high platform");
            clean();var s=loot("ender_wand");trial(s);game.items().mana().score(owner,"falltimer").set(99);owner.setOnGround(false);game.items().enderWiki().tick();require(game.items().mana().score(owner,"falltimer").get()==100,"raw airborne tick");
            require(use(s,InteractionHand.OFF_HAND)&&game.items().mana().get(owner)==97500&&Math.abs(owner.getY()-Math.floor(owner.getY())-.5)<1e-6&&flag(s,"upgrade_available")==1&&!owner.hasEffect(MobEffects.SLOW_FALLING),"Ender source landing half-block/trial/cost, offhand ordinary");
            owner.setOnGround(true);game.items().enderWiki().tick();require(game.items().mana().score(owner,"falltimer").get()==0,"ground resets airborne score");owner.setOnGround(false);
            require(use(loot("academy_ender_wand"),InteractionHand.OFF_HAND),"Academy Ender");effect(owner,MobEffects.SLOW_FALLING,60,0);
            clean();var colors=game.items().magic().colors();
            for(int tier=0;tier<3;tier++){require(owner.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.RAID_OMEN,20,40+tier),other),"native transport accepted");require(game.items().mana().score(owner,"effect_rtp_"+(tier+1)).get()==20,"native transport duration");}
            require(!owner.hasEffect(MobEffects.RAID_OMEN),"no raid transport remains");colors.tickTeleports();for(int tier=1;tier<=3;tier++)require(game.items().mana().score(owner,"effect_rtp_"+tier).get()==15,"five-tick source countdown");
            String hud=game.items().hud(owner).getString();require(hud.chars().filter(c->c==0xE142).count()==3,"all3 source RTP actionbar icons");
            colors.addPlayerCounters(owner);for(int tier=1;tier<=3;tier++)require(game.items().mana().score(owner,"effect_rtp_"+tier).get()==16,"KFC raw +1 for all RTP tiers");
            owner.entityTags().add("sin_greed_active");colors.tickTeleports();require(game.items().mana().score(owner,"effect_rtp_3").get()==16,"Greed pauses RTP");owner.entityTags().remove("sin_greed_active");
            owner.entityTags().add("time_stopped");colors.tickTeleports();require(game.items().mana().score(owner,"effect_rtp_3").get()==16,"native Time Stop pauses RTP");require(!use(loot("ender_wand"),InteractionHand.OFF_HAND),"Time Stop rejects Ender cast");owner.entityTags().remove("time_stopped");
            var victim=cow(at.add(0,5,8));require(victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.RAID_OMEN,20,42),other),"mob transport accepts author");victim.setHealth(1);victim.setOnGround(false);victim.hurtServer(owner.level(),victim.damageSources().fall(),100);require(!victim.isAlive()&&victim.getKillCredit()==other,"native fall death inherits teleport author");game.items().magic().colors().unloaded(victim);victim.discard();
            colors.unloaded(owner);colors.loaded(owner);colors.tickTeleports();require(game.items().mana().score(owner,"effect_rtp_3").get()==11,"load rebuilds all3 timer indexes");
            game.items().clear(owner);for(int tier=1;tier<=3;tier++)require(game.items().mana().score(owner,"effect_rtp_"+tier).get()==0,"purify clears all tiers");clean();
        }
        void admission(){
            var s=loot("academy_bad_wiki");owner.setShiftKeyDown(true);game.items().mana().set(owner,-100);
            owner.setXRot(0);require(use(s,InteractionHand.OFF_HAND)&&flag(s,"mode")==2&&flag(s,"tier")==1,"horizon cycles exactly once");owner.setXRot(-90);require(use(s,InteractionHand.OFF_HAND)&&flag(s,"tier")==2&&flag(s,"mode")==2,"up cycles tier");owner.setXRot(90);require(use(s,InteractionHand.OFF_HAND)&&flag(s,"tier")==1&&flag(s,"mode")==2&&game.items().mana().get(owner)==-100,"down cycles tier with no mana admission/debit");
            owner.setShiftKeyDown(false);owner.setXRot(0);game.items().mana().set(owner,499);require(!use(s,InteractionHand.OFF_HAND),"cost admission");game.items().mana().set(owner,10000);
            var marker=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,owner.level());marker.setPos(owner.position());marker.entityTags().add("stopper_magic");owner.level().addFreshEntity(marker);game.items().magic().loaded(marker);
            try{require(!use(s,InteractionHand.OFF_HAND)&&game.items().mana().get(owner)==10000,"antimagic rejects cast");}finally{game.items().magic().unloaded(marker);marker.discard();}
            var right=loot("academy_bad_wiki");mode(right,23);var left=loot("academy_bad_wiki");mode(left,24);owner.setItemInHand(InteractionHand.OFF_HAND,left);require(use(right,InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==9000&&owner.hasEffect(MobEffects.SPEED)&&owner.hasEffect(MobEffects.STRENGTH)&&owner.getOffhandItem()==left,"both hands cast their actual stacks/costs");clean();
        }
        int phase;long lastTime=Long.MIN_VALUE;net.minecraft.world.entity.Interaction panel;
        boolean academyStep(){long now=owner.level().getGameTime();if(now==lastTime)return false;lastTime=now;
            if(panel==null){panel=new net.minecraft.world.entity.Interaction(EntityTypes.INTERACTION,owner.level());panel.entityTags().add("magic_academy");panel.setPos(at);owner.level().addFreshEntity(panel);}
            String name=phase<2?"ender_wand":"bad_wiki";
            if(phase%2==0){var s=loot(name);owner.setItemInHand(InteractionHand.MAIN_HAND,s);require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&LbcAcademy.trial(s),"Academy admits "+name);game.items().academy().ready(owner,InteractionHand.MAIN_HAND,s);}
            else {require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&owner.getMainHandItem().is(name.equals("ender_wand")?LbcWands.ACADEMY_ENDER:LbcWands.ACADEMY_BAD_WIKI)&&Identifier.parse("lbc:tier_a").equals(owner.getMainHandItem().get(DataComponents.TOOLTIP_STYLE)),"real Academy promotion "+name);}
            return ++phase==4;
        }
        public void close(){
            game.items().timeStop().end(owner.level());game.items().magic().colors().clearEffects(owner);scores.forEach((k,v)->game.items().mana().score(owner,k).set(v));game.config().noBorders=noBorders;var fresh=new ArrayList<Entity>();owner.level().getAllEntities().forEach(e->{if(!before.contains(e)&&e.position().distanceToSqr(at)<40000)fresh.add(e);});fresh.forEach(Entity::discard);
            for(var e:blocks.entrySet()){if(owner.level().getBlockEntity(e.getKey()) instanceof net.minecraft.world.Container c)c.clearContent();game.maps().restoreSnapshot(owner.level(),e.getKey(),e.getValue());}
            game.server().getScoreboard().removePlayerTeam(team);saved.restore(game);otherSaved.restore(game);stats.forEach((s,v)->owner.getStats().setValue(owner,s,v));
            var p=game.players().get(owner.getUUID());p.lbcSkill=skill;p.lbcTrinket=trinket;p.lbcChallengeProgress=challengeProgress;p.lbcUnlocks.clear();p.lbcUnlocks.addAll(unlocks);game.config().offhandWands=offhand;
            for(var key:advancements){var a=game.server().getAdvancements().get(key);for(String c:a.value().criteria().keySet())if(!completed.get(key).contains(c))owner.getAdvancements().revoke(a,c);}
            game.items().indirectKills().clear(owner);game.items().indirectKills().clear(other);
            for(long key:forced)owner.level().setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
        }
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");var f=new Fixture(game);running=f;try{f.init();}catch(RuntimeException|Error error){running=null;f.close();throw error;}}
    public static void tick(GameController game){if(running==null)return;var f=running;try{if(!f.checked){if(!f.warm())return;f.presentation();f.outcomes();f.teleports();f.admission();f.checked=true;}if(f.academyStep()){running=null;f.close();SkyWars2.LOGGER.info("[check] PASS ender-wiki");}}catch(Throwable e){running=null;try{f.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL ender-wiki",e);}}
    public static void stopProbe(GameController game){var p=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(stopState==null,"exclusive stop");stopState=new GameChecks.WandThirdPlayerState(p,game.items().mana());p.removeAllEffects();for(int i=40;i<=42;i++)p.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.RAID_OMEN,400,i),p);epoch=game.items().epoch();SkyWars2.LOGGER.info("[check] PASS ender-wiki-stop-probe");}
    public static void cleanup(GameController game){var p=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));try{require(stopState!=null&&!game.items().currentEpoch(epoch),"actual epoch advance");for(int i=1;i<=3;i++)require(game.items().mana().score(p,"effect_rtp_"+i).get()==0,"actual stop clears all3 timers");require(game.items().mana().score(p,"falltimer").get()==0,"actual stop clears airborne counter");}finally{if(stopState!=null)stopState.restore(game);stopState=null;epoch=null;}SkyWars2.LOGGER.info("[check] PASS ender-wiki-cleanup");}
}
