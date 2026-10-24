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
public final class PrismChecks {
    private PrismChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Prism: "+why);}
    private static Fixture running;
    private static GameChecks.WandThirdPlayerState stopState;
    private static String epoch;
    private static final class Fixture implements AutoCloseable {
        boolean checked;int warmTicks;
        boolean warm(){require(++warmTicks<=200,"resident fixture becomes entity ticking");for(int x=-16;x<=-14;x++)for(int z=-23;z<=-5;z++)if(!owner.level().areEntitiesLoaded(net.minecraft.world.level.ChunkPos.pack(x,z)))return false;return true;}
        final GameController game; final ServerPlayer owner,other;
        final GameChecks.WandThirdPlayerState saved,otherSaved;
        final Vec3 at=new Vec3(-240.5,240,-240.5);
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final Map<net.minecraft.stats.Stat<Identifier>,Integer> stats=new LinkedHashMap<>();
        final Set<Long> forced=new HashSet<>();
        final Map<String,Integer> colors=new HashMap<>();
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
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var stat=net.minecraft.stats.Stats.CUSTOM.get(id);stats.put(stat,owner.getStats().getValue(stat));}
            for(String id:List.of("lbc:true_advancements/staffs/prism")){
                var key=Identifier.parse(id);var a=game.server().getAdvancements().get(key);if(a!=null){advancements.add(key);var prior=new HashSet<String>();owner.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(prior::add);completed.put(key,prior);}
            }
            for(String name:List.of("h","s","v"))colors.put(name,game.items().mana().score(owner,name).get());
            owner.level().getAllEntities().forEach(before::add);
            team=game.server().getScoreboard().addPlayerTeam("prism_fixture");
        }
        void init(){
            for(int x=-16;x<=-14;x++)for(int z=-23;z<=-5;z++){long key=net.minecraft.world.level.ChunkPos.pack(x,z);if(!owner.level().getForceLoadedChunks().contains(key)){forced.add(key);owner.level().setChunkForced(x,z,true);}owner.level().getChunk(x,z);}
            owner.closeContainer();owner.getInventory().clearContent();owner.removeAllEffects();owner.setGameMode(GameType.SURVIVAL);owner.setNoGravity(true);
            owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),0,0,true);other.teleportTo(owner.level(),at.x+90,at.y,at.z,Set.of(),0,0,true);other.setNoGravity(true);
            var p=game.players().get(owner.getUUID());p.lbcSkill=0;p.lbcTrinket=0;
            game.items().mana().noMagic(owner,0);game.items().mana().set(owner,100000);game.config().offhandWands=true;
            owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);owner.setHealth(1000);owner.setAbsorptionAmount(0);
        }
        ItemStack loot(String leaf){var list=game.lbc().rollTable(owner.level(),at,owner,"lbc:ported/"+leaf);require(list.size()==1,"one loot item "+leaf);return list.getFirst();}
        void mode(ItemStack stack,int mode){var d=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();d.putInt("mode",mode);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(d));}
        boolean use(ItemStack stack,InteractionHand hand){owner.setItemInHand(hand,stack);return owner.gameMode.useItem(owner,owner.level(),stack,hand).consumesAction();}
        Cow cow(Vec3 p){var c=new Cow(EntityTypes.COW,owner.level());c.setPos(p);c.setNoAi(true);c.setNoGravity(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);c.setHealth(1000);owner.level().addFreshEntity(c);return c;}
        int flag(ItemStack s,String key){return s.get(DataComponents.CUSTOM_DATA).copyTag().getIntOr(key,0);}
        void trial(ItemStack s){var d=s.get(DataComponents.CUSTOM_DATA).copyTag();d.putInt("upgradable",1);s.set(DataComponents.CUSTOM_DATA,CustomData.of(d));}
        void resetHealth(){owner.removeAllEffects();owner.setHealth(1000);owner.setInvulnerableTime(0);owner.damageCooldownTime=0;owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);}
        void effect(LivingEntity target,net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> type,int ticks,int amplifier){require(target.hasEffect(type)&&target.getEffect(type).getDuration()==ticks&&target.getEffect(type).getAmplifier()==amplifier,"exact native "+type.getRegisteredName());}
        int phase,waits;long lastTime=Long.MIN_VALUE;
        LbcPrism.Charge beam;Cow victim,friend,technical;
        ItemStack prism;net.minecraft.world.entity.Interaction panel;
        void block(BlockPos p,net.minecraft.world.level.block.Block b){blocks.putIfAbsent(p,game.maps().snapshot(owner.level(),p));owner.level().setBlock(p,b.defaultBlockState(),2);}
        void clear(){for(var p:game.items().prism().snapshot())if(!before.contains(p))p.discard();for(var e:game.items().mobgun().snapshot(owner))if(!before.contains(e))e.discard();owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);}
        LbcPrism.Charge cast(String leaf,InteractionHand hand){
            var prior=new HashSet<>(game.items().prism().snapshot());prism=loot(leaf);game.items().mana().set(owner,100000);owner.setShiftKeyDown(false);
            require(use(prism,hand)&&game.items().mana().get(owner)==98800,"actual native PKM price1200 "+leaf);
            return game.items().prism().snapshot().stream().filter(p->!prior.contains(p)).findFirst().orElseThrow();
        }
        void presentation(){for(boolean academy:new boolean[]{false,true}){
            var name=(academy?"academy_":"")+"unstable_prism";var s=loot(name);
            require(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals("skywars2:"+name),"native ready identity");
            require(flag(s,"prisma")==1&&flag(s,"staff")==1&&flag(s,"transformable")==1&&flag(s,"raycastspell")==1&&flag(s,"upgraded")==(academy?1:0),"exact source flags");
            require(Identifier.parse("lbc:prism").equals(s.get(DataComponents.ITEM_MODEL))&&Identifier.parse(academy?"lbc:tier_ss":"lbc:tier_b").equals(s.get(DataComponents.TOOLTIP_STYLE)),"source model/tier");
            require(s.get(DataComponents.LORE).lines().size()==(academy?10:9)&&s.get(DataComponents.CUSTOM_NAME).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t&&t.getKey().equals("unstable_prism")&&!s.getHoverName().getStyle().isItalic(),"source full lore/name/style");
        }}
        void ordinary(){
            clear();beam=cast("unstable_prism",InteractionHand.OFF_HAND);trial(prism);
            require(beam.getOwner()==owner&&beam.origin.distanceTo(owner.getEyePosition().add(0,0,.5))<1e-5&&beam.entityTags().contains("shield_blockable")&&!beam.shouldBeSaved(),"native owner, source offhand origin and shield policy");
            require(ProjectileLifetimes.protectedWandProjectile(beam),"shared physical-impulse exclusion");
            Vec3 start=beam.position();beam.tick();require(beam.position().distanceTo(start)>1.99&&beam.position().distanceTo(start)<2.01&&beam.age==1,"native tick advances the source nine samples/two blocks");
            victim=cow(beam.position());friend=cow(beam.position().add(.15,0,.15));technical=cow(beam.position().add(-.15,0,0));technical.entityTags().add("strict_map_object");
            game.server().getScoreboard().addPlayerToTeam(owner.getScoreboardName(),team);game.server().getScoreboard().addPlayerToTeam(friend.getScoreboardName(),team);
            game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),team);other.teleportTo(other.level(),beam.getX(),beam.getY(),beam.getZ(),Set.of(),0,0,true);other.removeAllEffects();
            float otherHealth=other.getHealth();beam.tick();require(victim.getHealth()<1000&&friend.getHealth()<1000&&technical.getHealth()==1000&&other.getHealth()==otherHealth,"hits friendly mobs, protects allied players and technical roles");
            require(victim.getKillCredit()==owner&&flag(prism,"upgrade_available")==0,"near accepted native hit has owner but does not pass distance trial");
            victim.discard();friend.discard();technical.discard();other.teleportTo(other.level(),at.x+90,at.y,at.z,Set.of(),0,0,true);
            beam.age=149;beam.tick();require(beam.isRemoved(),"source150 lifetime boundary");
            clear();beam=cast("unstable_prism",InteractionHand.MAIN_HAND);beam.setPos(1000000,240,1000000);require(!owner.level().hasChunkAt(beam.blockPosition()),"empty unloaded boundary");beam.tick();require(beam.isRemoved()&&!owner.level().hasChunkAt(beam.blockPosition()),"unloaded flight stops without loading chunks");
            clear();
        }
        void beginStable(){
            owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),0,0,true);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            block(BlockPos.containing(owner.getEyePosition()),net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:red_stained_glass")));
            victim=cow(owner.getEyePosition().add(0,0,90));friend=cow(owner.getEyePosition().add(.1,0,90));game.server().getScoreboard().addPlayerToTeam(friend.getScoreboardName(),team);
            beam=cast("unstable_prism",InteractionHand.MAIN_HAND);trial(prism);beam.tick();require(beam.phase==LbcPrism.Charge.Phase.STABLE,"red glass stabilizes native charge");
        }
        void stableDone(){
            require(beam.steps>250&&beam.steps<=451&&victim.getHealth()<1000&&friend.getHealth()<1000,"stabilized beam reaches actual90 block native targets: steps="+beam.steps+" at="+beam.position()+" phase="+beam.phase+" victim="+victim.getHealth()+" friend="+friend.getHealth());
            require(victim.getKillCredit()==owner&&flag(prism,"upgrade_available")==1,"accepted far hit satisfies actual used-stack75 trial");
            var award=game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/staffs/prism"));require(award!=null&&owner.getAdvancements().getOrStartProgress(award).isDone(),"stable hit grants source advancement");
            require(owner.level().getBlockState(BlockPos.containing(owner.getEyePosition())).is(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:red_stained_glass"))),"glass survives beam erosion");
            victim.discard();friend.discard();block(BlockPos.containing(owner.getEyePosition()),net.minecraft.world.level.block.Blocks.AIR);clear();
        }
        void beginAcademy(){
            Vec3 eye=owner.getEyePosition();var wall=BlockPos.containing(eye.add(0,0,3));for(int x=-2;x<=2;x++)for(int y=-2;y<=2;y++)block(wall.offset(x,y,0),net.minecraft.world.level.block.Blocks.STONE);
            victim=cow(eye.add(0,0,1.5));friend=cow(eye.add(0,0,-2));game.server().getScoreboard().addPlayerToTeam(friend.getScoreboardName(),team);
            game.items().mana().score(owner,"h").set(340);beam=cast("academy_unstable_prism",InteractionHand.OFF_HAND);
            require(!beam.entityTags().contains("shield_blockable")&&game.items().mana().score(owner,"h").get()==0&&game.items().mana().score(owner,"s").get()==80&&game.items().mana().score(owner,"v").get()==100,"Academy source hue cycle and no invented shield tag");
        }
        void academyDone(){
            require(beam.bounces>=1&&beam.steps==301&&victim.getHealth()<1000&&friend.getHealth()<1000,"native face bounce, full301 samples and friendly-mob hits behind caster: bounces="+beam.bounces+" steps="+beam.steps+" position="+beam.position()+" victim="+victim.getHealth()+" friend="+friend.getHealth());
            require(victim.getKillCredit()==owner&&friend.getKillCredit()==owner,"Academy direct7 hit ownership");
            victim.discard();friend.discard();clear();
        }
        void crystals(){
            var c=cast("unstable_prism",InteractionHand.MAIN_HAND);c.setPos(at.add(0,35,30));Vec3 motion=c.getDeltaMovement();
            var prior=new HashSet<>(game.items().mobgun().snapshot(owner));game.items().prism().crystal(c);
            var crystal=(net.minecraft.world.entity.boss.enderdragon.EndCrystal)game.items().mobgun().snapshot(owner).stream().filter(e->!prior.contains(e)&&e instanceof net.minecraft.world.entity.boss.enderdragon.EndCrystal).findFirst().orElseThrow();
            require(!crystal.showsBottom()&&game.items().mobgun().owner(crystal)==owner&&crystal.position().distanceTo(c.position().add(0,-.5,0))<1e-6,"source native crystal with shared author");
            victim=cow(c.position().add(1,0,0));require(crystal.hurtServer(owner.level(),owner.level().damageSources().generic(),1)&&crystal.isRemoved(),"actual native crystal detonation");
            require(victim.getHealth()<1000&&victim.getKillCredit()==owner,"removed native crystal source retains caster credit");
            require(c.getDeltaMovement().equals(motion),"actual explosion does not push magical charge");victim.discard();clear();
            game.items().mana().set(owner,-1000);owner.setItemInHand(InteractionHand.MAIN_HAND,loot("unstable_prism"));require(!game.items().wands().use(owner,InteractionHand.MAIN_HAND),"insufficient mana rejects cast");
            game.items().mana().set(owner,10000);var charge=cast("unstable_prism",InteractionHand.MAIN_HAND);
            var marker=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,owner.level());marker.setPos(at);marker.entityTags().add("stopper_magic");owner.level().addFreshEntity(marker);game.items().magic().loaded(marker);
            try{int mana=game.items().mana().get(owner);require(!game.items().wands().use(owner,InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==mana,"actual antimagic field blocks cast");charge.tick();require(charge.isRemoved(),"existing charge removed inside actual antimagic field");}
            finally{game.items().magic().unloaded(marker);marker.discard();}clear();
        }
        void beginFreeze(){beam=cast("unstable_prism",InteractionHand.MAIN_HAND);beam.setPos(at.add(0,15,0));game.items().timeStop().begin(owner.level(),200);frozenAt=beam.position();frozenAge=beam.age;waits=0;}
        Vec3 frozenAt;int frozenAge;
        boolean step(){long now=owner.level().getGameTime();if(now==lastTime)return false;lastTime=now;
            switch(phase){
                case 0->{presentation();ordinary();beginStable();phase++;}
                case 1->{if(!beam.isRemoved()){require(++waits<200,"stable queued beam completes");return false;}stableDone();beginAcademy();phase++;waits=0;}
                case 2->{if(!beam.isRemoved()){require(++waits<200,"Academy queued beam completes");return false;}academyDone();crystals();beginFreeze();phase++;}
                case 3->{require(beam.position().equals(frozenAt)&&beam.age==frozenAge,"actual native Time Stop freezes position/age");if(++waits<3)return false;game.items().timeStop().end(owner.level());phase++;}
                case 4->{require(beam.position().distanceTo(frozenAt)>1&&beam.age>frozenAge,"actual native resume restores flight");clear();phase++;}
                case 5->{if(panel==null){panel=new net.minecraft.world.entity.Interaction(EntityTypes.INTERACTION,owner.level());panel.entityTags().add("magic_academy");panel.setPos(at);owner.level().addFreshEntity(panel);}var s=loot("unstable_prism");owner.setItemInHand(InteractionHand.MAIN_HAND,s);require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&LbcAcademy.trial(s),"real Academy trial admission");game.items().academy().ready(owner,InteractionHand.MAIN_HAND,s);phase++;}
                case 6->{require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&owner.getMainHandItem().is(LbcWands.ACADEMY_PRISM)&&Identifier.parse("lbc:tier_ss").equals(owner.getMainHandItem().get(DataComponents.TOOLTIP_STYLE)),"real Academy promotion/tier");return true;}
                default->throw new IllegalStateException("phase "+phase);
            }return false;
        }
        public void close(){
            game.items().timeStop().end(owner.level());var fresh=new ArrayList<Entity>();owner.level().getAllEntities().forEach(e->{if(!before.contains(e)&&e.position().distanceToSqr(at)<40000)fresh.add(e);});fresh.forEach(Entity::discard);
            for(var e:blocks.entrySet()){if(owner.level().getBlockEntity(e.getKey()) instanceof net.minecraft.world.Container c)c.clearContent();game.maps().restoreSnapshot(owner.level(),e.getKey(),e.getValue());}
            colors.forEach((key,value)->game.items().mana().score(owner,key).set(value));
            game.server().getScoreboard().removePlayerTeam(team);saved.restore(game);otherSaved.restore(game);stats.forEach((s,v)->owner.getStats().setValue(owner,s,v));
            var p=game.players().get(owner.getUUID());p.lbcSkill=skill;p.lbcTrinket=trinket;p.lbcChallengeProgress=challengeProgress;p.lbcUnlocks.clear();p.lbcUnlocks.addAll(unlocks);game.config().offhandWands=offhand;
            for(var key:advancements){var a=game.server().getAdvancements().get(key);for(String c:a.value().criteria().keySet())if(!completed.get(key).contains(c))owner.getAdvancements().revoke(a,c);}
            game.items().indirectKills().clear(owner);game.items().indirectKills().clear(other);
            for(long key:forced)owner.level().setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
        }
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");var f=new Fixture(game);running=f;try{f.init();}catch(RuntimeException|Error error){running=null;f.close();throw error;}}
    public static void tick(GameController game){if(running==null)return;var f=running;try{if(!f.warm())return;if(f.step()){running=null;f.close();SkyWars2.LOGGER.info("[check] PASS prism");}}catch(Throwable e){running=null;try{f.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL prism",e);}}
    private static java.util.List<LbcPrism.Charge> stopped=List.of();
    public static void stopProbe(GameController game){var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(stopState==null,"exclusive actual stop");stopState=new GameChecks.WandThirdPlayerState(owner,game.items().mana());owner.setShiftKeyDown(false);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);game.items().mana().noMagic(owner,0);game.items().mana().set(owner,10000);owner.setItemInHand(InteractionHand.MAIN_HAND,game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/unstable_prism").getFirst());require(game.items().wands().use(owner,InteractionHand.MAIN_HAND),"actual live charge before stop");game.items().mana().set(owner,100000);for(int i=0;i<24;i++){owner.setItemInHand(InteractionHand.MAIN_HAND,game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/academy_unstable_prism").getFirst());require(game.items().wands().use(owner,InteractionHand.MAIN_HAND),"Academy stop probe launch");}stopped=game.items().prism().snapshot().stream().filter(p->!p.isRemoved()&&p.getOwner()==owner).toList();require(!stopped.isEmpty(),"live stop probe");SkyWars2.LOGGER.info("[check] Prism stop probes live={} queued={}",stopped.size(),game.items().prism().pendingRays());epoch=game.items().epoch();SkyWars2.LOGGER.info("[check] PASS prism-stop-probe");}
    public static void cleanup(GameController game){try{require(stopState!=null&&epoch!=null&&!game.items().currentEpoch(epoch)&&stopped.stream().allMatch(Entity::isRemoved),"actual stop removes native rays and queue");}finally{if(stopState!=null)stopState.restore(game);stopState=null;epoch=null;stopped=List.of();}SkyWars2.LOGGER.info("[check] PASS prism-cleanup");}
}
