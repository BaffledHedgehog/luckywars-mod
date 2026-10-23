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
public final class AdvancedSnowChecks {
    private AdvancedSnowChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Advanced/Snow: "+why);}
    private static Fixture running;
    private static GameChecks.WandThirdPlayerState stopState;
    private static String epoch;
    private static List<net.minecraft.world.entity.AreaEffectCloud> stopped=List.of();
    private static final class Fixture implements AutoCloseable {
        boolean checked;int warmTicks;
        boolean warm(){require(++warmTicks<=200,"resident fixture becomes entity ticking");for(int x=-16;x<=-14;x++)for(int z=-18;z<=8;z++)if(!owner.level().areEntitiesLoaded(net.minecraft.world.level.ChunkPos.pack(x,z)))return false;return true;}
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
            for(String id:List.of("lbc:true_advancements/staffs/teleport","lbc:true_advancements/staffs/snow_wand")){
                var key=Identifier.parse(id);var a=game.server().getAdvancements().get(key);if(a!=null){advancements.add(key);var prior=new HashSet<String>();owner.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(prior::add);completed.put(key,prior);}
            }
            owner.level().getAllEntities().forEach(before::add);
            team=game.server().getScoreboard().addPlayerTeam("advanced_snow_fixture");
        }
        void init(){
            for(int x=-16;x<=-14;x++)for(int z=-18;z<=8;z++){long key=net.minecraft.world.level.ChunkPos.pack(x,z);if(!owner.level().getForceLoadedChunks().contains(key)){forced.add(key);owner.level().setChunkForced(x,z,true);}owner.level().getChunk(x,z);}
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
        void reset(){resetHealth();owner.setShiftKeyDown(false);owner.setXRot(0);owner.setYRot(0);owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),0,0,true);game.items().mana().set(owner,100000);game.items().advancedSnow().forget(owner);game.items().frost().clear(owner);}
        void drain(){for(int i=0;i<300&&game.items().exasperato().pendingSearches()>0;i++)game.items().exasperato().tick();require(game.items().exasperato().pendingSearches()==0,"resident ray finishes, no recursive unbounded call");}
        ItemStack academy(int mode){var s=loot("academy_advanced_ender_wand");mode(s,mode);return s;}
        void presentation(){for(var leaf:List.of("advanced_ender_wand","academy_advanced_ender_wand","snow_wand","academy_snow_wand")){
            var s=loot(leaf);boolean upgraded=leaf.startsWith("academy_"),snow=leaf.endsWith("snow_wand");
            require(s.is(snow?(upgraded?LbcWands.ACADEMY_SNOW:LbcWands.SNOW):(upgraded?LbcWands.ACADEMY_ADVANCED_ENDER:LbcWands.ADVANCED_ENDER))&&flag(s,snow?"snow_wand":"advancedteleport")==1,"public item/flags");
            require(Identifier.parse("lbc:"+(snow?"snow_wand":"advancedenderwand")).equals(s.get(DataComponents.ITEM_MODEL))&&Identifier.parse("lbc:tier_"+(snow?(upgraded?"b":"d"):(upgraded?"s":"a"))).equals(s.get(DataComponents.TOOLTIP_STYLE))&&s.get(DataComponents.LORE).lines().size()==(snow?4:9)+(upgraded?1:0),"complete source model/tier/lore");
        }}
        void wall(){for(int y=240;y<=244;y++)for(int z=-231;z<=-230;z++)block(new BlockPos(-241,y,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());block(new BlockPos(-241,242,-232),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());}
        void rays(){
            reset();wall();var s=loot("advanced_ender_wand");trial(s);require(use(s,InteractionHand.OFF_HAND)&&game.items().mana().get(owner)==97000,"native offhand ordinary admission/cost");require(owner.position().equals(at),"ray result applies through queue");drain();
            require(Math.abs(owner.getZ()+231.5)<1e-6&&Math.abs(owner.getY()-241.5)<1e-6&&flag(s,"upgrade_available")==1,"source half-block landing and head-in-wall trial on actual used stack");effect(owner,MobEffects.INSTANT_DAMAGE,1,0);MobEffects.INSTANT_DAMAGE.value().applyEffectTick(owner.level(),owner,0);require(owner.getHealth()==994,"ordinary teleport native6 self damage");
            reset();require(use(academy(1),InteractionHand.OFF_HAND),"Academy default");drain();require(!owner.hasEffect(MobEffects.INSTANT_DAMAGE),"Academy removes self damage");effect(owner,MobEffects.SLOW_FALLING,20,0);
            reset();require(use(academy(2),InteractionHand.OFF_HAND),"topmost mode");drain();require(Math.abs(owner.getY()-245.5)<1e-6,"motion-blocking highest roof, not first wall step");
            reset();require(use(academy(3),InteractionHand.OFF_HAND),"clip mode");drain();require(owner.getZ()>-230&&owner.level().getBlockState(owner.blockPosition()).isAir()&&owner.level().getBlockState(owner.blockPosition().above()).isAir(),"first two-air opening after traversing solid wall");
            reset();for(int y=240;y<=244;y++)for(int z=-251;z<=-250;z++)block(new BlockPos(-241,y,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());owner.setYRot(180);require(use(academy(2),InteractionHand.OFF_HAND),"negative-direction topmost");drain();require(Math.abs(owner.getY()-245.5)<1e-6&&owner.blockPosition().getZ()==-250,"topmost uses actual negative-facing hit block, not boundary floor");
            reset();owner.setYRot(180);require(use(academy(3),InteractionHand.OFF_HAND),"negative-direction wall clip");drain();require(owner.getZ()<-251&&owner.level().getBlockState(owner.blockPosition()).isAir()&&owner.level().getBlockState(owner.blockPosition().above()).isAir(),"negative-direction exit selects the air cell beyond the wall");
            // Remove the nearby wall and overhang: the next target is beyond both old teleport ray limits.
            for(var pos:new ArrayList<>(blocks.keySet()))if(pos.getX()==-241&&pos.getZ()>=-232&&pos.getZ()<=-230)block(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            reset();block(new BlockPos(-241,241,100),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());require(use(academy(1),InteractionHand.OFF_HAND),"long ray cast");drain();require(owner.getZ()>99&&owner.position().distanceTo(at)>330,"loaded ray reaches341 blocks with no artificial fixed cap");
            block(new BlockPos(-241,241,100),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            reset();var victim=cow(at.add(0,1,8));require(use(academy(1),InteractionHand.OFF_HAND),"entity aim");drain();require(owner.distanceToSqr(victim)<4&&victim.getHealth()==1000,"actual entity hitbox aims teleport without damaging target");victim.discard();
            reset();owner.setXRot(90);block(new BlockPos(-241,-10,-241),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());var cost=game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("advanced_ender_wand_mana_use"),"lbc.math");int old=cost.get();cost.set(4000);
            try{require(use(academy(2),InteractionHand.OFF_HAND),"low topmost cast");drain();require(owner.position().equals(at)&&game.items().mana().get(owner)==100000,"invalid low surface keeps original position and refunds configured4000, no hardcoded3000");}finally{cost.set(old);}reset();
            owner.setXRot(-90);var high=cow(new Vec3(at.x,400,at.z));require(use(academy(1),InteractionHand.OFF_HAND),"above-build-height entity aim");drain();require(owner.getY()>399,"living target beyond build height extends natural ray, not a ceiling cap");high.discard();reset();
            owner.setXRot(-90);require(use(academy(1),InteractionHand.OFF_HAND),"empty vertical ray");drain();require(owner.position().equals(at),"no target above dimension means no arbitrary teleport");reset();
        }
        void clouds(){
            var service=game.items().advancedSnow();reset();var s=loot("snow_wand");trial(s);require(use(s,InteractionHand.OFF_HAND)&&game.items().mana().get(owner)==98500,"native ordinary snow/cost");require(service.snapshot().size()==1,"one native cloud");var state=service.snapshot().getFirst();var cloud=state.entity();
            require(cloud.getOwner()==owner&&cloud.position().equals(at.add(0,2,0))&&cloud.getRadius()==4&&cloud.getDuration()==250&&cloud.getWaitTime()==20&&cloud.getParticle() instanceof net.minecraft.core.particles.BlockParticleOption dust&&dust.getState().is(net.minecraft.world.level.block.Blocks.SNOW),"native source owner/radius/duration/wait/position/falling dust");
            Cow a=cow(cloud.position().add(1,0,0)),b=cow(cloud.position().add(-1,0,0)),c=cow(cloud.position().add(0,0,1)),outside=cow(cloud.position().add(7,0,0)),npc=cow(cloud.position().add(0,0,-1));npc.entityTags().add("strict_map_object");
            game.server().getScoreboard().addPlayerToTeam(owner.getScoreboardName(),team);game.server().getScoreboard().addPlayerToTeam(a.getScoreboardName(),team);game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),team);other.teleportTo(owner.level(),at.x+2,at.y+2,at.z,Set.of(),0,0,true);
            service.pulse(state);require(game.items().frost().cold(a)==2&&game.items().frost().cooldown(a)==2&&game.items().frost().cold(other)==0&&game.items().frost().cold(owner)==0&&game.items().frost().cold(outside)==0&&game.items().frost().cold(npc)==0,"source+2/cooldown2, friendly mob accepted, caster/allied player/NPC/radius protected");
            for(int i=1;i<50;i++)service.pulse(state);require(game.items().frost().cold(a)==100&&!game.items().frost().isFrozen(a),"cloud only accumulates cold until shared second boundary");game.items().frost().second();service.pulse(state);require(game.items().frost().isFrozen(a)&&game.items().frost().isFrozen(b)&&game.items().frost().isFrozen(c)&&flag(s,"upgrade_available")==1,"source second boundary freezes three mobs and unlocks actual offhand snow trial");
            game.items().frost().second();int frozen=game.items().frost().frozenSeconds(a);service.pulse(state);require(game.items().frost().frozenSeconds(a)==frozen,"snow does not refresh existing freeze");
            for(var e:List.of(a,b,c,outside,npc)){game.items().frost().clear(e);e.discard();}game.items().frost().clear(other);game.server().getScoreboard().removePlayerFromTeam(other.getScoreboardName(),team);game.server().getScoreboard().removePlayerFromTeam(owner.getScoreboardName(),team);other.teleportTo(owner.level(),at.x+90,at.y,at.z,Set.of(),0,0,true);
            cloud.tickCount=269;cloud.tick();require(!cloud.isRemoved(),"native cloud alive at269 ticks including wait20");cloud.tickCount=270;cloud.tick();require(cloud.isRemoved(),"native expiry at wait20+duration250");service.tick();require(service.snapshot().isEmpty(),"expired cloud index pruned");
            var slowed=cow(at.add(3,2,0));slowed.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.SLOWNESS,100,2));game.items().frost().snow(slowed,owner);require(game.items().frost().cold(slowed)==6,"initial Slowness III multiplier");game.items().frost().snow(slowed,owner);require(game.items().frost().cold(slowed)==8,"later source pulse adds only2");game.items().frost().clear(slowed);slowed.discard();
            var blinded=cow(at.add(10,2,0));for(int i=0;i<30;i++)game.items().frost().snow(blinded,owner);game.items().frost().unloaded(blinded);game.items().frost().loaded(blinded);game.items().frost().second();require(blinded.hasEffect(MobEffects.BLINDNESS),"deferred cold state/source survives index reload");blinded.setHealth(1);blinded.hurtServer(owner.level(),blinded.damageSources().fall(),100);require(!blinded.isAlive()&&blinded.getKillCredit()==owner,"native fall death after cold blindness credits applying player");game.items().frost().clear(blinded);blinded.discard();
            blinded=cow(at.add(10,2,0));for(int i=0;i<30;i++)game.items().frost().snow(blinded,owner);game.items().frost().second();blinded.hurtServer(owner.level(),blinded.damageSources().playerAttack(other),1);blinded.tick();game.items().frost().second();blinded.setHealth(1);blinded.setInvulnerableTime(0);blinded.damageCooldownTime=0;blinded.hurtServer(owner.level(),blinded.damageSources().fall(),100);require(!blinded.isAlive()&&blinded.getKillCredit()==other,"later player hit survives subsequent cold-effect refresh");game.items().frost().clear(blinded);blinded.discard();
            blinded=cow(at.add(10,2,0));for(int i=0;i<30;i++)game.items().frost().snow(blinded,owner);game.items().frost().second();blinded.hurtServer(owner.level(),blinded.damageSources().playerAttack(other),1);game.items().frost().snow(blinded,owner);blinded.tick();game.items().frost().second();blinded.setHealth(1);blinded.setInvulnerableTime(0);blinded.damageCooldownTime=0;blinded.hurtServer(owner.level(),blinded.damageSources().fall(),100);require(!blinded.isAlive()&&blinded.getKillCredit()==owner,"a genuinely later cold pulse retakes attribution");game.items().frost().clear(blinded);blinded.discard();
            reset();block(new BlockPos(-241,241,-231),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());require(use(loot("academy_snow_wand"),InteractionHand.OFF_HAND),"Academy aimed snow");drain();require(service.snapshot().size()==1&&service.snapshot().getFirst().entity().getZ()>-232&&Math.abs(service.snapshot().getFirst().entity().getY()-244.62)<1e-4,"aimed source impact+3 cloud placement");service.forget(owner);
            reset();require(use(loot("snow_wand"),InteractionHand.OFF_HAND),"unload setup");cloud=service.snapshot().getFirst().entity();service.unloaded(cloud);require(cloud.isRemoved()&&service.snapshot().isEmpty(),"detached native cloud quietly removed");
            require(use(loot("snow_wand"),InteractionHand.OFF_HAND),"beacon setup");cloud=service.snapshot().getFirst().entity();var marker=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,owner.level());marker.setPos(cloud.position());marker.entityTags().add("stopper_magic");owner.level().addFreshEntity(marker);game.items().magic().loaded(marker);
            try{service.tick();require(cloud.isRemoved()&&service.snapshot().isEmpty(),"antimagic removes existing snow cloud");}finally{game.items().magic().unloaded(marker);marker.discard();}reset();
        }
        void admission(){
            var s=academy(1);game.items().mana().set(owner,-100);owner.setShiftKeyDown(true);for(int m:new int[]{2,3,1})require(use(s,InteractionHand.OFF_HAND)&&flag(s,"mode")==m&&game.items().mana().get(owner)==-100,"free negative-mana mode switch");owner.setShiftKeyDown(false);game.items().mana().set(owner,1499);require(!use(loot("snow_wand"),InteractionHand.OFF_HAND),"snow cost admission");game.items().mana().set(owner,2999);require(!use(s,InteractionHand.OFF_HAND),"teleport cost admission");
            reset();owner.setItemInHand(InteractionHand.OFF_HAND,loot("snow_wand"));require(use(loot("snow_wand"),InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==97000&&game.items().advancedSnow().snapshot().size()==2,"parallel actual hand costs/clouds");reset();
            owner.entityTags().add("time_stopped");require(!use(s,InteractionHand.OFF_HAND)&&!use(loot("snow_wand"),InteractionHand.OFF_HAND),"Time Stop rejects both spells");owner.entityTags().remove("time_stopped");reset();
        }
        int phase,wait,cloudAge;long lastTime=Long.MIN_VALUE;net.minecraft.world.entity.Interaction panel;net.minecraft.world.entity.AreaEffectCloud frozenCloud;
        boolean step(){long now=owner.level().getGameTime();if(now==lastTime)return false;lastTime=now;
            if(phase<4){if(panel==null){panel=new net.minecraft.world.entity.Interaction(EntityTypes.INTERACTION,owner.level());panel.entityTags().add("magic_academy");panel.setPos(at);owner.level().addFreshEntity(panel);}String name=phase<2?"advanced_ender_wand":"snow_wand";
                if(phase%2==0){var s=loot(name);owner.setItemInHand(InteractionHand.MAIN_HAND,s);require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&LbcAcademy.trial(s),"real Academy trial admission");game.items().academy().ready(owner,InteractionHand.MAIN_HAND,s);}
                else require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&owner.getMainHandItem().is(phase==1?LbcWands.ACADEMY_ADVANCED_ENDER:LbcWands.ACADEMY_SNOW)&&Identifier.parse(phase==1?"lbc:tier_s":"lbc:tier_b").equals(owner.getMainHandItem().get(DataComponents.TOOLTIP_STYLE)),"real successive-tick Academy promotion");phase++;return false;}
            if(phase==4){reset();require(use(loot("snow_wand"),InteractionHand.OFF_HAND),"native cloud freeze setup");frozenCloud=game.items().advancedSnow().snapshot().getFirst().entity();cloudAge=frozenCloud.tickCount;game.items().timeStop().begin(owner.level(),200);phase++;return false;}
            require(!frozenCloud.isRemoved()&&frozenCloud.tickCount==cloudAge,"actual dimension Time Stop preserves native cloud age");if(++wait<3)return false;game.items().timeStop().end(owner.level());return true;
        }
        public void close(){
            game.items().timeStop().end(owner.level());game.items().advancedSnow().forget(owner);game.items().magic().colors().clearEffects(owner);scores.forEach((k,v)->game.items().mana().score(owner,k).set(v));game.config().noBorders=noBorders;var fresh=new ArrayList<Entity>();owner.level().getAllEntities().forEach(e->{if(!before.contains(e)&&e.position().distanceToSqr(at)<40000)fresh.add(e);});fresh.forEach(e->{if(e instanceof LivingEntity living)game.items().frost().clear(living);e.discard();});
            for(var e:blocks.entrySet()){if(owner.level().getBlockEntity(e.getKey()) instanceof net.minecraft.world.Container c)c.clearContent();game.maps().restoreSnapshot(owner.level(),e.getKey(),e.getValue());}
            game.server().getScoreboard().removePlayerTeam(team);saved.restore(game);otherSaved.restore(game);stats.forEach((s,v)->owner.getStats().setValue(owner,s,v));
            var p=game.players().get(owner.getUUID());p.lbcSkill=skill;p.lbcTrinket=trinket;p.lbcChallengeProgress=challengeProgress;p.lbcUnlocks.clear();p.lbcUnlocks.addAll(unlocks);game.config().offhandWands=offhand;
            for(var key:advancements){var a=game.server().getAdvancements().get(key);for(String c:a.value().criteria().keySet())if(!completed.get(key).contains(c))owner.getAdvancements().revoke(a,c);}
            game.items().indirectKills().clear(owner);game.items().indirectKills().clear(other);
            for(long key:forced)owner.level().setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
        }
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");var f=new Fixture(game);running=f;try{f.init();}catch(RuntimeException|Error error){running=null;f.close();throw error;}}
    public static void tick(GameController game){if(running==null)return;var f=running;try{if(!f.checked){if(!f.warm())return;f.presentation();f.rays();f.clouds();f.admission();f.checked=true;}if(f.step()){running=null;f.close();SkyWars2.LOGGER.info("[check] PASS advanced-snow");}}catch(Throwable e){running=null;try{f.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL advanced-snow",e);}}
    public static void stopProbe(GameController game){var p=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(stopState==null,"exclusive actual stop");stopState=new GameChecks.WandThirdPlayerState(p,game.items().mana());p.removeAllEffects();p.setShiftKeyDown(false);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);game.items().mana().noMagic(p,0);game.items().mana().set(p,10000);p.setItemInHand(InteractionHand.MAIN_HAND,game.lbc().rollTable(p.level(),p.position(),p,"lbc:ported/snow_wand").getFirst());require(game.items().wands().use(p,InteractionHand.MAIN_HAND),"actual native cloud before stop");stopped=game.items().advancedSnow().snapshot().stream().map(LbcAdvancedSnow.Cloud::entity).toList();require(stopped.size()==1,"one stop cloud");epoch=game.items().epoch();SkyWars2.LOGGER.info("[check] PASS advanced-snow-stop-probe");}
    public static void cleanup(GameController game){try{require(stopState!=null&&!game.items().currentEpoch(epoch)&&stopped.size()==1&&stopped.stream().allMatch(Entity::isRemoved)&&game.items().advancedSnow().snapshot().isEmpty(),"actual quiet stop clears native cloud and index");}finally{if(stopState!=null)stopState.restore(game);stopState=null;epoch=null;stopped=List.of();}SkyWars2.LOGGER.info("[check] PASS advanced-snow-cleanup");}
}
