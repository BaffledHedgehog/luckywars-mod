package me.skywars2.game;

import java.lang.reflect.Method;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

/** Actual native use/draw/volley callbacks; no nearby-arrow substitution or second ammunition debit. */
public final class ImbaCrossbowChecks {
    private ImbaCrossbowChecks(){}
    private static Fixture running;
    private enum BlastPhase { PAUSED, PREFIX, CAPTURE, ORIGINAL_TAIL, RESTORE, REPLAY }
    private record CollectorRun(List<?> positions,long nextLong,int nextInt,long nanos){}
    private record BeamPackets(long riptide,int soulPackets,int soulParticles,int dustPackets,int dustParticles,boolean dustAppearance){}
    static void require(boolean value,String why){if(!value)throw new IllegalStateException("IMBA crossbows: "+why);}
    private static Object blastField(LbcStonekivi.Work job,String name){
        try {
            var field=job.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(job);
        }catch(ReflectiveOperationException failure){
            throw new IllegalStateException("Missing crossbow blast fixture field "+name,failure);
        }
    }

    private static CollectorRun collect(Method collector,ServerLevel level,Vec3 at,DamageSource damage,float power){
        var result=new CollectorRun[1];
        CrossbowBlastBlocks.scoped(level,at,damage,()->{
            var explosion=new ServerExplosion(level,null,damage,null,at,power,false,Explosion.BlockInteraction.DESTROY);
            require(explosion.getDirectSourceEntity()==null&&explosion.getDamageSource()==damage,
                    "source-null collector retains the accepted crossbow damage object");
            try{
                long start=System.nanoTime();
                var positions=(List<?>)collector.invoke(explosion);
                long elapsed=System.nanoTime()-start;
                result[0]=new CollectorRun(positions,level.getRandom().nextLong(),level.getRandom().nextInt(),elapsed);
            }catch(ReflectiveOperationException failure){
                throw new IllegalStateException("Native crossbow explosion collector",failure);
            }
        });
        return result[0];
    }

    private static void flatten(Packet<?> packet,List<Packet<?>> result){
        if(packet instanceof ClientboundBundlePacket bundle)bundle.subPackets().forEach(child->flatten(child,result));
        else result.add(packet);
    }
    private static long hitSounds(List<Packet<?>> wire,int first){
        var packets=new ArrayList<Packet<?>>();wire.subList(first,wire.size()).forEach(packet->flatten(packet,packets));
        return packets.stream().filter(packet->packet instanceof ClientboundGameEventPacket event
                &&event.getEvent()==ClientboundGameEventPacket.PLAY_ARROW_HIT_SOUND).count();
    }
    private static BeamPackets beamPackets(List<Packet<?>> wire,int first){
        var packets=new ArrayList<Packet<?>>();wire.subList(first,wire.size()).forEach(packet->flatten(packet,packets));
        long sounds=packets.stream().filter(packet->packet instanceof ClientboundSoundPacket sound
                &&sound.getSound().value().location().toString().equals("minecraft:item.trident.riptide_3")).count();
        var souls=packets.stream().filter(packet->packet instanceof ClientboundLevelParticlesPacket particles
                &&particles.particle().getType()==ParticleTypes.SCULK_SOUL).map(ClientboundLevelParticlesPacket.class::cast).toList();
        var dust=packets.stream().filter(packet->packet instanceof ClientboundLevelParticlesPacket particles
                &&particles.particle().getType()==ParticleTypes.DUST_COLOR_TRANSITION).map(ClientboundLevelParticlesPacket.class::cast).toList();
        var from=ARGB.vector3fFromRGB24(0xffccff);var to=ARGB.vector3fFromRGB24(0xccccff);
        boolean dustAppearance=dust.stream().allMatch(packet->packet.particle() instanceof DustColorTransitionOptions color
                &&color.getFromColor().equals(from)&&color.getToColor().equals(to)&&color.getScale()==.9f);
        return new BeamPackets(sounds,souls.size(),souls.stream().mapToInt(ClientboundLevelParticlesPacket::count).sum(),
                dust.size(),dust.stream().mapToInt(ClientboundLevelParticlesPacket::count).sum(),dustAppearance);
    }

    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;
        final List<Entity> owned=new ArrayList<>();final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final boolean skills,levelups;final int trinketLimit;
        final Map<BlockPos,MapManager.BlockSnapshot> blastBlocks=new LinkedHashMap<>();
        final boolean mobGriefing,tntExplodes;
        final int tntProtection;
        List<Vec3> blastCenters;
        Cow blastVictim;
        LbcStonekivi.Work blastJob;
        BlastPhase blastPhase;
        Object capturedDamage;
        int blastPhaseTick,publishedBlasts,capturedCursor,mostBlastsInTick;
        ColdIntake coldIntake;
        boolean blastRulesChanged,blastHit,ownsVanity,closed;
        Fixture(GameController game){this.game=game;base=new ElementalGrimoireChecks.Fixture(game,true);skills=game.config().lbcSkills;levelups=game.config().lbcLevelups;trinketLimit=game.config().lbcTrinketLimit;game.config().lbcSkills=false;game.config().lbcLevelups=false;game.config().lbcTrinketLimit=0;base.initialize();
            var rules=base.owner.level().getGameRules();
            mobGriefing=rules.get(GameRules.MOB_GRIEFING);
            tntExplodes=rules.get(GameRules.TNT_EXPLODES);
            tntProtection=game.config().tntProtection;
        }
        ItemStack leaf(LbcImbaCrossbows.Kind kind){var items=game.lbc().rollTable(base.owner.level(),base.owner.position(),base.owner,"lbc:ported/"+kind.id);require(items.size()==1&&items.getFirst().is(kind.item),"canonical native leaf "+kind);return items.getFirst();}
        void clear(){base.owner.stopUsingItem();base.owner.getInventory().clearContent();for(var e:owned){game.items().entityUnloaded(e);e.discard();}owned.clear();base.clear();game.items().magic().inventoryChanged(base.owner);game.items().magic().flushCleansing();base.owner.setYRot(0);base.owner.setXRot(0);base.owner.setDeltaMovement(Vec3.ZERO);}
        Set<Entity> bodies(){var set=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());base.owner.level().getAllEntities().forEach(set::add);return set;}
        List<Projectile> added(Set<Entity> previous){var out=new ArrayList<Projectile>();for(var e:base.owner.level().getAllEntities())if(!previous.contains(e)&&!e.isRemoved()){owned.add(e);if(e instanceof Projectile p&&p.getOwner()==base.owner)out.add(p);}return out;}
        ItemStack equip(LbcImbaCrossbows.Kind kind,ItemStack ammo){clear();var weapon=leaf(kind);weapon.set(DataComponents.CUSTOM_NAME,Component.literal("preserve exact witness"));base.owner.setItemInHand(InteractionHand.MAIN_HAND,weapon);base.owner.setItemInHand(InteractionHand.OFF_HAND,ammo);return weapon;}
        List<Projectile> shoot(LbcImbaCrossbows.Kind kind,ItemStack ammo){var weapon=equip(kind,ammo);weapon.set(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.ofNonEmpty(List.of(ammo.copyWithCount(1))));var before=bodies();require(base.owner.gameMode.useItem(base.owner,base.owner.level(),weapon,InteractionHand.MAIN_HAND).consumesAction(),"actual native use "+kind);return added(before);}
        ItemStack poison(int count){var ammo=new ItemStack(Items.TIPPED_ARROW,count);ammo.set(DataComponents.CUSTOM_NAME,Component.literal("native ammo witness"));ammo.set(DataComponents.POTION_CONTENTS,new PotionContents(Optional.empty(),Optional.of(0x123456),List.of(new MobEffectInstance(MobEffects.POISON,160,1)),Optional.empty()));return ammo;}
        void presentation(){
            var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)(Object)base.owner.connection;
            for(var kind:LbcImbaCrossbows.Kind.values()){
                var item=leaf(kind);var defaults=kind.item.getDefaultInstance();
                for(var component:List.of(DataComponents.CUSTOM_NAME,DataComponents.LORE,DataComponents.CUSTOM_DATA,DataComponents.TOOLTIP_STYLE,DataComponents.ITEM_MODEL,DataComponents.ENCHANTMENTS))require(Objects.equals(item.get(component),defaults.get(component)),"give/loot exact component "+component+" "+kind);
                var proxy=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(item,TooltipFlag.NORMAL,provider.getPacketContext(),base.owner.registryAccess());
                require(ItemStack.matches(item,eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(proxy,provider.getPacketContext(),base.owner.registryAccess())),"native Polymer roundtrip "+kind);
            }
        }
        void shotgun(){
            var ammo=poison(2);var shots=shoot(LbcImbaCrossbows.Kind.SHOTGUN,ammo);
            require(shots.size()==256,"exact source256 physical arrows");
            for(var p:shots){var arrow=(AbstractArrow)p;require(arrow.getOwner()==base.owner&&arrow.getDeltaMovement().z>=4.5&&arrow.getDeltaMovement().z<=5.5&&Math.abs(arrow.getDeltaMovement().x)<=.5&&Math.abs(arrow.getDeltaMovement().y)<=.5,"source5 speed/cartesian spread and actual owner");require(arrow.getPickupItemStackOrigin().get(DataComponents.POTION_CONTENTS).equals(ammo.get(DataComponents.POTION_CONTENTS))&&arrow.pickup==AbstractArrow.Pickup.DISALLOWED,"all source nondropping pellets preserve actual custom ammo");}
            require(ammo.getCount()==2,"already charged volley never debits inventory twice");clear();
        }
        Cow beamTarget(Vec3 from,double forward,double gap){
            var level=base.owner.level();var target=new Cow(EntityTypes.COW,level);
            double across=gap==0?0:Math.copySign(target.getBbWidth()*.5+Math.abs(gap),gap);
            target.setPos(from.add(across,-.2,forward));target.setNoAi(true);target.setNoGravity(true);
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);target.setHealth(1000);
            owned.add(target);require(level.addFreshEntity(target),"native living sniper target");
            return target;
        }
        void beams(){
            var other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(other!=null&&other.level()==base.owner.level(),"independent listener for native sniper contact");
            try(var audience=new RegenerationBeltChecks.ScheduledClient(game,other)){
                for(var kind:List.of(LbcImbaCrossbows.Kind.SNIPER,LbcImbaCrossbows.Kind.SUPER)){
                    var shots=shoot(kind,poison(1));require(shots.isEmpty(),"beam replaces own accepted native arrows");var traces=game.items().wands().electroSnapshot();int count=kind==LbcImbaCrossbows.Kind.SNIPER?1:32;
                    require(traces.size()==count&&traces.stream().allMatch(t->t.erecto.kind==LbcErecto.CROSSBOW&&t.erecto.ammunition!=null&&t.owner==base.owner&&t.position.equals(base.owner.position())&&t.lifetime==(count==1?200:50)),"exact source count/lifetime/origin and accepted ammo "+kind);
                    if(count==1){
                        var trace=traces.getFirst();var target=beamTarget(trace.position,8,0);
                        int age=trace.age;game.items().timeStop().begin(base.owner.level(),40);game.items().wands().tick();require(trace.age==age,"shared TimeStop pauses beam");game.items().timeStop().end(base.owner.level());
                        int ownFirst=base.recording.packets.size(),otherFirst=audience.packets().size();
                        require(game.items().erecto().step(trace)&&target.getHealth()==860&&target.hasEffect(MobEffects.POISON),"source7 unit-cell hits at20 arrow damage, legacy bypass cooldown and retained actual ammo effect hp="+target.getHealth()+" poison="+target.hasEffect(MobEffects.POISON)+" trace="+trace.position+" target="+target.position());
                        var ownHits=hitSounds(base.recording.packets,ownFirst);var otherHits=hitSounds(audience.packets(),otherFirst);
                        require(ownHits==1&&otherHits==0,"seven direct damage samples emit one private shooter cue (shooter="+ownHits+", listener="+otherHits+")");
                        var clone=new LbcWands.ElectroTrace(trace);require(clone.erecto.ammunition!=trace.erecto.ammunition&&clone.position.equals(trace.position),"typed clone detaches accepted payload state");
                    }
                    clear();
                }
                var ammo=poison(1);ammo.set(DataComponents.POTION_DURATION_SCALE,.25f);
                require(shoot(LbcImbaCrossbows.Kind.SNIPER,ammo).isEmpty(),"real custom-ammunition sniper graze shot");
                var traces=game.items().wands().electroSnapshot();require(traces.size()==1,"one native graze beam");
                var trace=traces.getFirst();var graze=beamTarget(trace.position,8,.20);var outside=beamTarget(trace.position,11,-.30);
                int ownFirst=base.recording.packets.size(),otherFirst=audience.packets().size();
                require(game.items().erecto().step(trace),"native sniper advances through the lateral witnesses");
                var effect=graze.getEffect(MobEffects.POISON);
                require(graze.getHealth()<1000&&effect!=null&&effect.getDuration()==40&&effect.getAmplifier()==1,
                        "arrow-width graze keeps native damage and exact accepted Poison II/40 ticks (hp="+graze.getHealth()+", effect="+effect+")");
                require(outside.getHealth()==1000&&!outside.hasEffect(MobEffects.POISON),"outside the arrow-width sweep has neither damage nor ammo payload");
                var ownHits=hitSounds(base.recording.packets,ownFirst);var otherHits=hitSounds(audience.packets(),otherFirst);
                require(ownHits==1&&otherHits==0,"lateral graze emits exactly one private shooter cue (shooter="+ownHits+", listener="+otherHits+")");
                clear();
            }
        }
        List<LbcStonekivi.Work> queuedBlasts(){
            return game.items().stonekivi().queuedSnapshot().stream()
                    .filter(job->job.getClass().getSimpleName().equals("CrossbowBlasts")).toList();
        }
        List<ClientboundExplodePacket> blastPackets(){
            return base.recording.packets.stream().filter(ClientboundExplodePacket.class::isInstance)
                    .map(ClientboundExplodePacket.class::cast).toList();
        }
        void collectorParity(){
            String option="skywars2.disableExplosionCache",previous=System.getProperty(option);
            var level=base.owner.level();var random=level.getRandom();var state=MatchRandomState.capture(random);
            var cell=BlockPos.containing(base.owner.position().add(0,2,0));
            int flags=Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS;
            require(NativeChunkLoad.resident(level,cell)&&level.getBlockState(cell).isAir(),"resident empty collector witness cell");
            var before=game.maps().snapshot(level,cell);
            var kills=game.items().indirectKills();
            var damage=kills.captureDamage(level.damageSources().explosion(null,base.owner),kills.wildCause(base.owner));
            try{
                var collector=ServerExplosion.class.getDeclaredMethod("calculateExplodedPositions");collector.setAccessible(true);
                var offsets=List.of(new Vec3(.5,.5,.5),new Vec3(.99,.17,.01),new Vec3(.03,.8,.97));
                for(var block:List.of(Blocks.OBSIDIAN,Blocks.STONE)){
                    boolean absorbed=block==Blocks.OBSIDIAN;
                    float power=absorbed?20:4;
                    require(level.setBlock(cell,block.defaultBlockState(),flags),"placed native collector witness "+block);
                    long[] nativeNanos=new long[offsets.size()],scopedNanos=new long[offsets.size()];
                    for(int sample=0;sample<offsets.size();sample++){
                        var at=Vec3.atLowerCornerOf(cell).add(offsets.get(sample));
                        long seed=0x43524F5353424F57L+sample;
                        System.setProperty(option,"true");random.setSeed(seed);
                        var expected=collect(collector,level,at,damage,power);
                        System.clearProperty(option);random.setSeed(seed);
                        var actual=collect(collector,level,at,damage,power);
                        require(absorbed?expected.positions().isEmpty():expected.positions().contains(cell),
                                "native collector witnesses a fully absorbed cast or a destructible center at "+at+" in "+block);
                        require(actual.positions().equals(expected.positions())&&actual.nextLong()==expected.nextLong()
                                        &&actual.nextInt()==expected.nextInt(),
                                "scoped source-null collector preserves vanilla position order and subsequent world RNG at "+at+" in "+block);
                        nativeNanos[sample]=expected.nanos();scopedNanos[sample]=actual.nanos();
                    }
                    Arrays.sort(nativeNanos);Arrays.sort(scopedNanos);
                    SkyWars2.LOGGER.info("[check] imba-crossbows collector {} radius={} calls={} nativeMedianUs={} scopedMedianUs={} nativeTotalUs={} scopedTotalUs={}",
                            block,power,offsets.size(),nativeNanos[1]/1_000d,scopedNanos[1]/1_000d,
                            Arrays.stream(nativeNanos).sum()/1_000d,Arrays.stream(scopedNanos).sum()/1_000d);
                }
            }catch(ReflectiveOperationException failure){
                throw new IllegalStateException("Missing native crossbow explosion collector",failure);
            }finally{
                if(previous==null)System.clearProperty(option);else System.setProperty(option,previous);
                try{game.maps().restoreSnapshot(level,cell,before,flags);}finally{state.restore(random);}
            }
        }
        ItemStack chargedSuper(ServerPlayer shooter){
            return chargedCrossbow(shooter,LbcImbaCrossbows.Kind.SUPER);
        }
        ItemStack chargedCrossbow(ServerPlayer shooter,LbcImbaCrossbows.Kind kind){
            var weapon=leaf(kind);
            shooter.setItemInHand(InteractionHand.MAIN_HAND,weapon);
            weapon.set(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.ofNonEmpty(List.of(new ItemStack(Items.ARROW))));
            return weapon;
        }
        void superBeamPackets(){
            var owner=base.owner;var level=owner.level();var wands=game.items().wands();var stone=game.items().stonekivi();
            var other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(other!=null&&other.level()==level,"second native listener for the same SUPER volley");
            var traces=(Collection<?>)WoolBucketChecks.read(wands,"electroTraces");
            var queue=(Collection<?>)WoolBucketChecks.read(stone,"work");
            var originalJobs=stone.queuedSnapshot();
            require(traces.isEmpty()&&!game.items().timeStop().stopped(level),"exclusive unpaused SUPER packet frame");
            var cell=BlockPos.containing(owner.position().add(0,12,0));var contact=Vec3.atCenterOf(cell);
            require(NativeChunkLoad.resident(level,cell)&&level.getBlockState(cell).isAir(),"resident empty SUPER packet witness");
            int flags=Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS;
            var previousBlock=game.maps().snapshot(level,cell);var previousWeapon=owner.getMainHandItem();
            var observerAt=other.position();var observerVelocity=other.getDeltaMovement();
            var stat=Stats.ITEM_USED.get(LbcImbaCrossbows.Kind.SUPER.item);int used=owner.getStats().getValue(stat);
            var ownerRandom=MatchRandomState.capture(owner.getRandom());var worldRandom=MatchRandomState.capture(level.getRandom());
            try(var audience=new RegenerationBeltChecks.ScheduledClient(game,other)){
                var ahead=Vec3.directionFromRotation(owner.getXRot(),owner.getYRot());
                other.setPos(contact.subtract(ahead.scale(3)));
                require(level.setBlock(cell,Blocks.OBSIDIAN.defaultBlockState(),flags),"native SUPER packet obsidian witness");
                owner.getRandom().setSeed(0x5355504552L);level.getRandom().setSeed(0x534F554E44L);
                var weapon=chargedSuper(owner);
                require(owner.gameMode.useItem(owner,level,weapon,InteractionHand.MAIN_HAND).consumesAction(),"real charged SUPER packet witness");
                var volley=wands.electroSnapshot();
                require(volley.size()==32,"one native SUPER creates all 32 beam paths before its presentation frame");
                var direction=new ArrayList<Vec3>();
                for(var trace:volley){
                    require(trace.owner==owner&&trace.position.equals(owner.position()),"SUPER preserves the native launch source");
                    direction.add(Vec3.directionFromRotation(trace.pitch,trace.yaw));trace.position=contact;
                }
                var stepOwnerRng=MatchRandomState.capture(owner.getRandom());
                var stepWorldRng=MatchRandomState.capture(level.getRandom());
                for(int i=0;i<32*61;i++)if(level.getRandom().nextBoolean())owner.getRandom().nextLong();
                long expectedOwnerNext=owner.getRandom().nextLong(),expectedWorldNext=level.getRandom().nextLong();
                stepOwnerRng.restore(owner.getRandom());stepWorldRng.restore(level.getRandom());
                int ownFirst=base.recording.packets.size(),otherFirst=audience.packets().size(),explosions=blastPackets().size();
                wands.tick();
                boolean ownerRngMatches=owner.getRandom().nextLong()==expectedOwnerNext;
                boolean worldRngMatches=level.getRandom().nextLong()==expectedWorldNext;
                var ownBeam=beamPackets(base.recording.packets,ownFirst);var otherBeam=beamPackets(audience.packets(),otherFirst);
                SkyWars2.LOGGER.info("[check] imba-crossbows SUPER packet sample nativePaths={} shooter={} nearbyListener={} ownerRng={} worldRng={}",
                        volley.size(),ownBeam,otherBeam,ownerRngMatches,worldRngMatches);
                require(ownerRngMatches&&worldRngMatches,"source sample rolls and discarded packet seeds leave both native RNG states exact");
                require(new HashSet<>(direction).size()>1&&wands.electroSnapshot().equals(volley)
                                &&stone.queuedSnapshot().size()==originalJobs.size()+32&&blastPackets().size()==explosions,
                        "one actual SUPER frame advances all launch paths and queues all contacts without inline explosions");
                for(int i=0;i<volley.size();i++){
                    var trace=volley.get(i);var job=trace.erecto.pendingBlasts;
                    require(trace.position.distanceToSqr(contact.add(direction.get(i).scale(18)))<1e-10
                                    &&job!=null&&job.next==0&&job.centers.getFirst().equals(contact),
                            "each beam retains its own full original trajectory and first exact contact");
                }
                for(var sample:List.of(ownBeam,otherBeam)){
                    require(sample.riptide()==1,"one original audible riptide sample per listener for the full native volley: "+sample);
                    require(sample.soulPackets()>0&&sample.soulPackets()<320&&sample.soulParticles()==960,
                            "SCULK_SOUL uses fewer logical packets and preserves all source particles per listener: "+sample);
                    require(sample.dustPackets()>0&&sample.dustPackets()<=672*95/100&&sample.dustParticles()==1952&&sample.dustAppearance(),
                            "SUPER dust uses at least five percent fewer logical packets and preserves source count, colors and size: "+sample);
                }
            }finally{
                for(var trace:wands.electroSnapshot()){
                    if(trace.erecto!=null&&trace.erecto.pendingBlasts!=null){var job=trace.erecto.pendingBlasts;queue.remove(job);job.cancel();}
                    traces.remove(trace);
                }
                other.setPos(observerAt);other.setDeltaMovement(observerVelocity);
                owner.setItemInHand(InteractionHand.MAIN_HAND,previousWeapon);owner.getStats().setValue(owner,stat,used);
                game.maps().restoreSnapshot(level,cell,previousBlock,flags);
                ownerRandom.restore(owner.getRandom());worldRandom.restore(level.getRandom());
            }
            require(stone.queuedSnapshot().equals(originalJobs),"SUPER packet measurement only retires its own native work");
        }
        void superDustVisibility(){
            var owner=base.owner;
            var level=owner.level();
            var boundary=Vec3.atCenterOf(owner.blockPosition()).add(512,0,0);
            var inside=new Vec3[]{boundary.add(-.42,0,0),boundary.add(-.24,0,0),boundary.add(-.04,0,0)};
            var crossing=new Vec3[]{boundary.add(-.28,0,0),boundary.add(-.10,0,0),boundary.add(.10,0,0)};
            var type=new DustColorTransitionOptions(0xffccff,0xccccff,.9f);
            int start=base.recording.packets.size();
            try(var grouped=new LbcParticleBatch.NearbyRuns(level,type,.1,true,.25)){
                grouped.add(inside,inside.length);
                grouped.add(crossing,crossing.length);
            }
            var beam=beamPackets(base.recording.packets,start);
            require(beam.dustPackets()==3&&beam.dustParticles()==5&&beam.dustAppearance(),
                    "native range edge keeps the complete original triplet and both visible samples of the crossing triplet");
        }
        void concurrentSuperVolleys(){
            try(var probe=new SuperIntake()){
                probe.stage();probe.check();
            }
        }
        final class SuperIntake implements AutoCloseable {
            final ServerPlayer owner=base.owner;
            final ServerLevel level=owner.level();
            final LbcWands wands=game.items().wands();
            final LbcStonekivi stone=game.items().stonekivi();
            final ServerPlayer other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            final List<LbcWands.ElectroTrace> oldTraces=wands.electroSnapshot();
            final List<LbcStonekivi.Work> oldJobs=stone.queuedSnapshot();
            final Collection<?> traceStore=(Collection<?>)WoolBucketChecks.read(wands,"electroTraces");
            final Collection<?> queue=(Collection<?>)WoolBucketChecks.read(stone,"work");
            final BlockPos cell=BlockPos.containing(owner.position().add(0,12,0));
            final Vec3 contact=Vec3.atCenterOf(cell);
            final int flags=Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS;
            final MapManager.BlockSnapshot previousBlock;
            final ItemStack previousWeapon=owner.getMainHandItem(),previousAmmo=owner.getOffhandItem(),otherWeapon;
            final Stat<Item> superStat=Stats.ITEM_USED.get(LbcImbaCrossbows.Kind.SUPER.item);
            final Stat<Item> gatlingStat=Stats.ITEM_USED.get(LbcImbaCrossbows.Kind.GATLING.item);
            final int ownerSuperUsed=owner.getStats().getValue(superStat),ownerGatlingUsed=owner.getStats().getValue(gatlingStat),otherSuperUsed;
            final int previouslyPublished=blastPackets().size();
            final List<LbcStonekivi.Work> expectedQueue=new ArrayList<>(oldJobs);
            final List<LbcErecto.CrossbowBlasts> acceptedJobs=new ArrayList<>();
            final ItemStack finiteAmmo=new ItemStack(Items.ARROW,7);
            List<LbcWands.ElectroTrace> traces;
            ItemStack gatling,gatlingBefore;

            SuperIntake(){
                require(other!=null&&other.level()==level&&game.canUseGameplayItems(other)&&!other.isUsingItem(),"independent active crossbow shooter");
                require(!game.items().timeStop().stopped(level),"native SUPER intake begins without Time Stop");
                require(NativeChunkLoad.resident(level,cell)&&level.getBlockState(cell).isAir(),"resident empty SUPER contact witness");
                previousBlock=game.maps().snapshot(level,cell);otherWeapon=other.getMainHandItem();
                otherSuperUsed=other.getStats().getValue(superStat);
            }

            void stage(){
                require(level.setBlock(cell,Blocks.OBSIDIAN.defaultBlockState(),flags),"native SUPER obsidian contact witness");
                for(int volley=1;volley<=2;volley++){
                    var weapon=chargedSuper(owner);
                    require(owner.gameMode.useItem(owner,level,weapon,InteractionHand.MAIN_HAND).consumesAction()
                                    &&wands.electroSnapshot().size()==oldTraces.size()+32*volley,
                            "actual native SUPER volley "+volley+" accepts 32 independent traces");
                }
                gatling=leaf(LbcImbaCrossbows.Kind.GATLING);
                owner.setItemInHand(InteractionHand.MAIN_HAND,gatling);owner.setItemInHand(InteractionHand.OFF_HAND,finiteAmmo);
                require(owner.gameMode.useItem(owner,level,gatling,InteractionHand.MAIN_HAND).consumesAction()&&owner.getUseItem()==gatling,
                        "real sustained Gatling use begins before the same owner builds a backlog");
                gatlingBefore=gatling.copy();

                traces=wands.electroSnapshot();
                int acceptedCenters=0;
                for(var trace:traces.subList(oldTraces.size(),traces.size())){
                    require(trace.owner==owner&&trace.level==level&&trace.erecto.kind==LbcErecto.CROSSBOW
                                    &&trace.erecto.pendingBlasts==null,"accepted SUPER trace belongs to this volley");
                    // Preserve each native spread and stage just the first sample inside the same block.
                    trace.position=contact;
                    game.items().erecto().step(trace);
                    var job=trace.erecto.pendingBlasts;
                    require(job!=null&&job.path==trace.erecto&&job.level==level&&job.next==0
                                    &&job.centers.getFirst().equals(contact),
                            "SUPER trace queues its own job at the exact first contact without publishing it");
                    expectedQueue.add(job);
                    acceptedJobs.add(job);
                    acceptedCenters+=job.centers.size();
                }
                require(expectedQueue.size()==oldJobs.size()+64&&stone.queuedSnapshot().equals(expectedQueue)
                                &&blastPackets().size()==previouslyPublished&&level.getBlockState(cell).is(Blocks.OBSIDIAN),
                        "both SUPER volleys retain all 64 ordered jobs without inline blast packets or terrain edits");
                SkyWars2.LOGGER.info("[check] imba-crossbows SUPER volleys=2 queuedJobs={} acceptedCenters={} inlineBlasts=0",
                        expectedQueue.size()-oldJobs.size(),acceptedCenters);
            }

            void check(){
                gatling.getItem().onUseTick(level,owner,gatling,72000);
                require(owner.getUseItem()==gatling&&owner.getOffhandItem()==finiteAmmo&&finiteAmmo.getCount()==7
                                &&gatling.getOrDefault(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.EMPTY).isEmpty()
                                &&ItemStack.matches(gatling,gatlingBefore)&&owner.getStats().getValue(gatlingStat)==ownerGatlingUsed
                                &&wands.electroSnapshot().equals(traces)&&stone.queuedSnapshot().equals(expectedQueue),
                        "a held uncharged Gatling with finite ammo defers the shot before reload, damage or used stat");
                owner.stopUsingItem();

                var charged=chargedSuper(owner);var chargeBefore=charged.copy();int usedBefore=owner.getStats().getValue(superStat);
                require(!owner.gameMode.useItem(owner,level,charged,InteractionHand.MAIN_HAND).consumesAction()
                                &&owner.getMainHandItem()==charged&&ItemStack.matches(charged,chargeBefore)
                                &&owner.getStats().getValue(superStat)==usedBefore&&wands.electroSnapshot().equals(traces)
                                &&stone.queuedSnapshot().equals(expectedQueue),
                        "one owner's unfinished segments reject the entire native SUPER while preserving exact charge, durability and used stat");

                var independent=chargedSuper(other);
                require(other.gameMode.useItem(other,level,independent,InteractionHand.MAIN_HAND).consumesAction()
                                &&wands.electroSnapshot().stream().filter(trace->trace.owner==other&&!oldTraces.contains(trace)).count()==32
                                &&other.getStats().getValue(superStat)==otherSuperUsed+1&&stone.queuedSnapshot().equals(expectedQueue),
                        "the same backlog still admits a different owner's complete native SUPER volley");

                for(var job:acceptedJobs){queue.remove(job);job.cancel();}
                require(stone.queuedSnapshot().equals(oldJobs),"only the witness jobs retired before retry");
                require(owner.gameMode.useItem(owner,level,charged,InteractionHand.MAIN_HAND).consumesAction()
                                &&charged.getOrDefault(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.EMPTY).isEmpty()
                                &&owner.getStats().getValue(superStat)==usedBefore+1
                                &&wands.electroSnapshot().stream().filter(trace->trace.owner==owner&&!oldTraces.contains(trace)).count()==96,
                        "the preserved charged SUPER retries as a full native volley when its backlog retires");

                owner.setItemInHand(InteractionHand.MAIN_HAND,gatling);
                require(owner.gameMode.useItem(owner,level,gatling,InteractionHand.MAIN_HAND).consumesAction()&&owner.getUseItem()==gatling,
                        "native held Gatling can resume after its owner's backlog retires");
                gatling.getItem().onUseTick(level,owner,gatling,72000);
                require(finiteAmmo.getCount()==4&&owner.getStats().getValue(gatlingStat)==ownerGatlingUsed+1
                                &&wands.electroSnapshot().stream().filter(trace->trace.owner==owner&&!oldTraces.contains(trace)).count()==100
                                &&stone.queuedSnapshot().equals(oldJobs)&&blastPackets().size()==previouslyPublished,
                        "one admitted Gatling tick consumes three finite arrows and creates only its four native traces");
                SkyWars2.LOGGER.info("[check] imba-crossbows native overload preserves charged SUPER and held Gatling, admits independent SUPER and retries both");
            }

            @Override public void close(){
                for(var trace:wands.electroSnapshot()){
                    if(oldTraces.contains(trace))continue;
                    if(trace.erecto!=null&&trace.erecto.pendingBlasts!=null){
                        var job=trace.erecto.pendingBlasts;queue.remove(job);job.cancel();
                    }
                    traceStore.remove(trace);
                }
                owner.stopUsingItem();owner.setItemInHand(InteractionHand.MAIN_HAND,previousWeapon);
                owner.setItemInHand(InteractionHand.OFF_HAND,previousAmmo);other.setItemInHand(InteractionHand.MAIN_HAND,otherWeapon);
                owner.getStats().setValue(owner,superStat,ownerSuperUsed);owner.getStats().setValue(owner,gatlingStat,ownerGatlingUsed);
                other.getStats().setValue(other,superStat,otherSuperUsed);
                game.maps().restoreSnapshot(level,cell,previousBlock,flags);
                require(wands.electroSnapshot().equals(oldTraces)&&stone.queuedSnapshot().equals(oldJobs),
                        "SUPER intake fixture retires only its own traces and queued jobs");
            }
        }
        final class ColdIntake implements AutoCloseable {
            final ServerPlayer owner=base.owner;
            final ServerLevel level=owner.level();
            final LbcWands wands=game.items().wands();
            final LbcStonekivi stone=game.items().stonekivi();
            final Collection<?> traceStore=(Collection<?>)WoolBucketChecks.read(wands,"electroTraces");
            final Collection<?> queue=(Collection<?>)WoolBucketChecks.read(stone,"work");
            final TicketStorage storage=(TicketStorage)WoolBucketChecks.read(level.getChunkSource(),"ticketStorage");
            final TicketType ticket=(TicketType)WoolBucketChecks.read(game.items().exasperato(),"IMPACT_TICKET");
            final Map<BlockPos,MapManager.BlockSnapshot> witnesses=new LinkedHashMap<>();
            final List<LbcWands.ElectroTrace> traces=new ArrayList<>();
            final Stat<Item> sniperStat=Stats.ITEM_USED.get(LbcImbaCrossbows.Kind.SNIPER.item);
            final int shotsUsed=owner.getStats().getValue(sniperStat),started=game.server().getTickCount();
            final ItemStack previousWeapon=owner.getMainHandItem();
            final float pitch=owner.getXRot(),yaw=owner.getYRot();
            final boolean invulnerable=owner.getAbilities().invulnerable;
            final boolean oldMobGriefing=level.getGameRules().get(GameRules.MOB_GRIEFING);
            final int flags=Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS;
            final Set<ChunkPos> footprint=new LinkedHashSet<>(),coldChunks=new LinkedHashSet<>();
            Set<ChunkPos> firstStarts=Set.of(),initialTickets=Set.of();
            LbcErecto.CrossbowBlasts cold,overlap,warm;
            boolean checkedSecondTick;

            Set<ChunkPos> footprint(Vec3 center,int reach){
                var pos=BlockPos.containing(center);var result=new LinkedHashSet<ChunkPos>();
                for(int x=(pos.getX()-reach)>>4;x<=(pos.getX()+reach)>>4;x++)
                    for(int z=(pos.getZ()-reach)>>4;z<=(pos.getZ()+reach)>>4;z++)result.add(new ChunkPos(x,z));
                return result;
            }
            boolean resident(ChunkPos pos){return level.getChunkSource().getChunkNow(pos.x(),pos.z())!=null;}
            Set<ChunkPos> tickets(Collection<ChunkPos> chunks){
                var result=new LinkedHashSet<ChunkPos>();
                for(var chunk:chunks)if(storage.getTickets(chunk.pack()).stream().anyMatch(t->t.getType()==ticket))result.add(chunk);
                return result;
            }
            BlockPos coldEdge(int y){
                var home=ChunkPos.containing(owner.blockPosition());
                for(int radius=1;radius<=24;radius++)for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
                    if(dx==0&&dz==0)continue;
                    var chunk=new ChunkPos(home.x()+dx*radius,home.z()+dz*radius);
                    var cell=new BlockPos((chunk.x()<<4)+8,y,(chunk.z()<<4)+8);
                    if(resident(chunk)&&level.getBlockState(cell).isAir()
                            &&footprint(Vec3.atCenterOf(cell),42).stream().filter(pos->!resident(pos)).count()>16)return cell;
                }
                throw new IllegalStateException("IMBA crossbows: no genuinely cold footprint on the resident arena edge");
            }
            void witness(BlockPos cell){
                require(NativeChunkLoad.resident(level,cell)&&level.getBlockState(cell).isAir(),"resident empty cold-load fixture witness");
                witnesses.put(cell,game.maps().snapshot(level,cell));
                require(level.setBlock(cell,Blocks.OBSIDIAN.defaultBlockState(),flags),"place bounded cold-load fixture witness");
            }
            LbcErecto.CrossbowBlasts stageContact(BlockPos cell){
                int existing=wands.electroSnapshot().size();var weapon=chargedCrossbow(owner,LbcImbaCrossbows.Kind.SNIPER);
                boolean fired=owner.gameMode.useItem(owner,level,weapon,InteractionHand.MAIN_HAND).consumesAction();
                var beams=wands.electroSnapshot();traces.addAll(beams.subList(existing,beams.size()));
                require(fired&&beams.size()==existing+1,"actual native sniper accepts exactly one cold-load fixture trace");
                var trace=beams.getLast();var contact=Vec3.atCenterOf(cell);trace.position=contact;
                game.items().erecto().step(trace);
                var job=trace.erecto.pendingBlasts;
                require(job!=null&&job.next==0&&job.centers.getFirst().equals(contact),"actual contact queues a still unpublished native blast segment");
                footprint.addAll(footprint(job.footprint,job.reach));
                return job;
            }
            void begin(){
                require(queuedBlasts().isEmpty()&&!game.items().timeStop().stopped(level),"exclusive cold-load comparison");
                var nearby=BlockPos.containing(owner.position().add(0,12,0));var edge=coldEdge(nearby.getY());
                witness(edge);witness(nearby);
                owner.getAbilities().invulnerable=true;
                level.getGameRules().set(GameRules.MOB_GRIEFING,false,game.server());
                cold=stageContact(edge);overlap=stageContact(edge);warm=stageContact(nearby);
                require(queuedBlasts().equals(List.of(cold,overlap,warm))&&footprint(warm.footprint,warm.reach).stream().allMatch(this::resident),
                        "two cold jobs precede a fully resident warm peer in the real queue");
                initialTickets=tickets(footprint);
                for(var pos:footprint)if(!resident(pos)&&!initialTickets.contains(pos))coldChunks.add(pos);
                require(coldChunks.size()>16,"fixture has more than two frames of unique cold native chunks");

                stone.tick();
                firstStarts=tickets(coldChunks);
                for(int pass=0;pass<6&&warm.next==0;pass++)stone.tick();
                require(!firstStarts.isEmpty()&&firstStarts.size()<=8&&tickets(coldChunks).size()<=8&&cold.next==0&&overlap.next==0,
                        "at most eight distinct real cold tickets start in one tick, including same-tick revisits (first="+firstStarts.size()
                                +", current="+tickets(coldChunks).size()+", eligible="+coldChunks.size()+")");
                require(warm.next>0&&blastPackets().stream().anyMatch(p->p.center().equals(warm.centers.getFirst())),
                        "an already resident peer publishes a real native explosion while both cold jobs wait");
                firstStarts=tickets(coldChunks);
            }
            boolean nextTick(){
                int elapsed=game.server().getTickCount()-started;
                if(elapsed==0)return false;
                if(checkedSecondTick){
                    require(elapsed<200,"queued overlap receives a native FULL footprint within 200 ticks");
                    if(overlap.path.pendingBlasts!=null)return false;
                    require(overlap.next==overlap.centers.size(),
                            "surviving overlap publishes every accepted center after cold chunks arrive");
                    require(tickets(footprint).equals(initialTickets),"completed footprints release every new native ticket");
                    return true;
                }
                var secondStarts=tickets(coldChunks);secondStarts.removeAll(firstStarts);
                if(secondStarts.isEmpty()){
                    stone.tick();secondStarts=tickets(coldChunks);secondStarts.removeAll(firstStarts);
                }
                require(!secondStarts.isEmpty()&&secondStarts.size()<=8&&cold.next==0&&overlap.next==0,
                        "a later actual tick admits the next bounded set of cold chunks without publishing partial work (new="+secondStarts.size()+")");
                SkyWars2.LOGGER.info("[check] imba-crossbows cold distinctTicketStarts first={} next={} warmPublications={} residentEdge={}",
                        firstStarts.size(),secondStarts.size(),warm.next,cold.footprint);
                queue.remove(cold);cold.cancel();traceStore.remove(cold.trace);
                require(tickets(coldChunks).containsAll(firstStarts)&&overlap.path.pendingBlasts==overlap,
                        "canceling one overlapping footprint preserves the shared native tickets of the survivor");
                checkedSecondTick=true;
                return false;
            }
            @Override public void close(){
                for(var trace:traces){
                    if(trace.erecto!=null&&trace.erecto.pendingBlasts!=null){var job=trace.erecto.pendingBlasts;queue.remove(job);job.cancel();}
                    traceStore.remove(trace);
                }
                witnesses.forEach((cell,image)->game.maps().restoreSnapshot(level,cell,image,flags));
                owner.getAbilities().invulnerable=invulnerable;
                owner.setItemInHand(InteractionHand.MAIN_HAND,previousWeapon);owner.setYRot(yaw);owner.setXRot(pitch);
                owner.getStats().setValue(owner,sniperStat,shotsUsed);
                level.getGameRules().set(GameRules.MOB_GRIEFING,oldMobGriefing,game.server());
                if(!coldChunks.isEmpty())require(tickets(footprint).equals(initialTickets),"only fixture EXASPERATO tickets are released on cancellation");
            }
        }
        void beginBlastQueue(){
            require(queuedBlasts().isEmpty(),"exclusive crossbow blast queue");
            require(!game.vanity().busy()&&!game.vanity().capturePreparing()&&!game.vanity().hasSnapshot(),
                    "exclusive native Vanity point");
            var owner=base.owner;
            var level=owner.level();
            require(!game.items().timeStop().stopped(level),"crossbow fixture starts without time stop");
            require(owner.teleportTo(level,owner.getX(),Math.floor(owner.getY())+.75,owner.getZ(),Set.of(),0,0,true),
                    "ray starts above the slab collision shape within the same voxel");
            require(shoot(LbcImbaCrossbows.Kind.SNIPER,new ItemStack(Items.ARROW)).isEmpty(),
                    "real sniper volley replaces the native arrow");
            var traces=game.items().wands().electroSnapshot();
            require(traces.size()==1,"one actual sniper trace");
            var trace=traces.getFirst();
            var direction=Vec3.directionFromRotation(trace.pitch,trace.yaw);
            var first=BlockPos.containing(trace.position.add(0,0,3));
            var cells=List.of(first,first.south(),first.offset(0,0,2));
            var expected=new ArrayList<Vec3>();
            for(int sample=0;sample<=60;sample++){
                var point=trace.position.add(direction.scale(sample*.3));
                var cell=BlockPos.containing(point);
                require(NativeChunkLoad.resident(level,cell)&&level.getBlockState(cell).isAir(),
                        "resident empty native ray before adding fixture cells");
                if(cells.contains(cell))expected.add(point);
            }
            blastCenters=List.copyOf(expected);
            require(blastCenters.size()>8&&blastCenters.size()<=11
                            &&BlockPos.containing(blastCenters.getFirst()).equals(first)
                            &&BlockPos.containing(blastCenters.getLast()).equals(cells.getLast()),
                    "three adjacent blocks give more centers than one native publication tick allows");
            for(var cell:cells){
                blastBlocks.put(cell,game.maps().snapshot(level,cell));
                level.setBlock(cell,Blocks.SMOOTH_STONE_SLAB.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            }
            // Check MOB semantics with no collateral block updates and with TNT explicitly unavailable.
            var rules=level.getGameRules();
            blastRulesChanged=true;
            rules.set(GameRules.MOB_GRIEFING,false,game.server());
            rules.set(GameRules.TNT_EXPLODES,false,game.server());
            game.config().tntProtection=5;
            owner.getAbilities().invulnerable=true;
            blastVictim=new Cow(EntityTypes.COW,level);
            blastVictim.setNoAi(true);
            blastVictim.setNoGravity(true);
            blastVictim.setPos(Vec3.atCenterOf(first).add(4,.25,0));
            blastVictim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
            blastVictim.setHealth(1000);
            owned.add(blastVictim);
            require(level.addFreshEntity(blastVictim),"real off-axis explosion target");
            base.recording.packets.clear();
            boolean advanced=game.items().erecto().step(trace);
            var jobs=queuedBlasts();
            if(jobs.size()==1)blastJob=jobs.getFirst();
            require(advanced&&blastPackets().isEmpty()&&blastVictim.getHealth()==1000,
                    "trace accepts the whole segment before native explosions can run");
            require(blastJob!=null&&blastField(blastJob,"level")==level
                            &&blastCenters.equals(blastField(blastJob,"centers"))
                            &&Integer.valueOf(0).equals(blastField(blastJob,"next")),
                    "one accepted job holds the exact original ordered centers before publication");
            game.items().wands().stop();
            game.items().timeStop().begin(level,40);
            nextBlastPhase(BlastPhase.PAUSED);
        }
        void nextBlastPhase(BlastPhase phase){
            blastPhase=phase;
            blastPhaseTick=game.server().getTickCount();
        }
        void checkBlastPackets(List<ClientboundExplodePacket> packets,int before,int firstCenter){
            int count=packets.size()-before;
            int publishedNow=count-publishedBlasts;
            require(publishedNow>=0&&publishedNow<=8&&count+firstCenter<=blastCenters.size(),
                    "at most eight native explosions per server tick and no duplicated centers");
            mostBlastsInTick=Math.max(mostBlastsInTick,publishedNow);
            for(int index=publishedBlasts;index<count;index++){
                var packet=packets.get(before+index);
                require(packet.center().equals(blastCenters.get(firstCenter+index))&&packet.radius()==20,
                        "native MOB blast has the exact queued center and power");
            }
            publishedBlasts=count;
        }
        void checkBlastAttribution(){
            if(blastVictim.getHealth()<1000&&!blastHit){
                var owner=base.owner;
                var level=owner.level();
                var damage=blastVictim.getLastDamageSource();
                require(damage!=null&&damage.getDirectEntity()==null&&damage.getEntity()==owner
                                &&damage.typeHolder().equals(level.damageSources().explosion(null,owner).typeHolder()),
                        "real source-null MOB explosion preserves shooter attribution with TNT disabled and maximum protection");
                blastHit=true;
            }
        }
        void checkBlastCompletion(){
            var level=base.owner.level();
            require(blastHit&&!queuedBlasts().contains(blastJob)&&blastBlocks.keySet().stream()
                            .allMatch(cell->level.getBlockState(cell).is(Blocks.SMOOTH_STONE_SLAB)),
                    "all accepted blasts finish, hurt the target, and respect mob griefing");
        }
        boolean advanceBlastCheck(){
            var owner=base.owner;
            var level=owner.level();
            var vanity=game.vanity();
            int elapsed=game.server().getTickCount()-blastPhaseTick;
            var packets=blastPackets();
            require(elapsed<1200,"bounded crossbow blast phase "+blastPhase+" at "+vanity.status()
                    +" with "+packets.size()+" observed native packets");
            switch(blastPhase){
                case PAUSED -> {
                    require(packets.isEmpty()&&blastVictim.getHealth()==1000
                                    &&Integer.valueOf(0).equals(blastField(blastJob,"next")),
                            "native world TimeStop suspends the accepted blast queue");
                    if(elapsed<2)return false;
                    game.items().timeStop().end(level);
                    nextBlastPhase(BlastPhase.PREFIX);
                }
                case PREFIX -> {
                    checkBlastPackets(packets,0,0);
                    checkBlastAttribution();
                    if(publishedBlasts==0)return false;
                    capturedCursor=publishedBlasts;
                    capturedDamage=blastField(blastJob,"damage");
                    require(capturedCursor<blastCenters.size()&&queuedBlasts().contains(blastJob)
                                    &&Integer.valueOf(capturedCursor).equals(blastField(blastJob,"next")),
                            "first active tick leaves an exact nonempty pending suffix beyond the native cap");
                    // Capture also advances shared work. Hold the precise cursor with the real stopping effect first.
                    game.items().timeStop().begin(level,10000);
                    require(vanity.requestCapture(owner,false),"real Vanity coordinator accepts the partial crossbow job");
                    ownsVanity=true;
                    nextBlastPhase(BlastPhase.CAPTURE);
                }
                case CAPTURE -> {
                    require(packets.size()==capturedCursor
                                    &&Integer.valueOf(capturedCursor).equals(blastField(blastJob,"next")),
                            "capture keeps the partial native cursor without publishing its suffix");
                    if(vanity.capturePreparing()||vanity.busy()||!vanity.hasSnapshot())return false;
                    require(game.items().timeStop().stopped(level),"active TimeStop is part of the capture");
                    game.items().timeStop().end(level);
                    nextBlastPhase(BlastPhase.ORIGINAL_TAIL);
                }
                case ORIGINAL_TAIL -> {
                    checkBlastPackets(packets,0,0);
                    checkBlastAttribution();
                    if(publishedBlasts!=blastCenters.size())return false;
                    checkBlastCompletion();
                    require(vanity.requestRestoreForCheck(),"real Vanity restore after the original job retired");
                    nextBlastPhase(BlastPhase.RESTORE);
                }
                case RESTORE -> {
                    for(var player:game.server().getPlayerList().getPlayers())
                        if(player.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)
                            ack.skywars2$acknowledgeRestore();
                    require(packets.size()==blastCenters.size(),"rewind publishes no native explosions while frozen");
                    if(vanity.busy())return false;
                    require(vanity.hasSnapshot()&&game.items().timeStop().stopped(level)
                                    &&queuedBlasts().equals(List.of(blastJob))
                                    &&blastCenters.equals(blastField(blastJob,"centers"))
                                    &&Integer.valueOf(capturedCursor).equals(blastField(blastJob,"next"))
                                    &&blastField(blastJob,"damage")==capturedDamage,
                            "Vanity restores the exact shared job, source and partial cursor before publication");
                    publishedBlasts=0;
                    game.items().timeStop().end(level);
                    nextBlastPhase(BlastPhase.REPLAY);
                }
                case REPLAY -> {
                    checkBlastPackets(packets,blastCenters.size(),capturedCursor);
                    if(publishedBlasts!=blastCenters.size()-capturedCursor)return false;
                    checkBlastCompletion();
                    return true;
                }
            }
            return false;
        }
        void continuous(){
            for(var kind:List.of(LbcImbaCrossbows.Kind.FAST,LbcImbaCrossbows.Kind.GATLING)){
                var ammo=poison(7);var weapon=equip(kind,ammo);var before=bodies();require(base.owner.gameMode.useItem(base.owner,base.owner.level(),weapon,InteractionHand.MAIN_HAND).consumesAction()&&base.owner.isUsingItem(),"native sustained use "+kind);
                for(int i=0;i<2;i++)weapon.getItem().onUseTick(base.owner.level(),base.owner,weapon,72000-i);
                var shots=added(before);require(ammo.getCount()==(kind==LbcImbaCrossbows.Kind.FAST?5:1),"selected debit1/3 per native active use tick "+kind);
                require(kind==LbcImbaCrossbows.Kind.FAST?shots.size()==2:shots.isEmpty()&&game.items().wands().electroSnapshot().size()==8,"exact continuous physical/beam family "+kind);
                require(base.owner.getMainHandItem()==weapon&&weapon.getHoverName().getString().equals("preserve exact witness"),"same native weapon/components retained");
                clear();ammo=new ItemStack(LbcImbaNativeItems.INFINITE_ARROW,1);weapon=equip(kind,ammo);base.owner.gameMode.useItem(base.owner,base.owner.level(),weapon,InteractionHand.MAIN_HAND);weapon.getItem().onUseTick(base.owner.level(),base.owner,weapon,72000);require(ammo.getCount()==1,"only selected infinite ammo is free "+kind);clear();
            }
        }
        void rockets(){
            var shots=shoot(LbcImbaCrossbows.Kind.SHOTGUN,new ItemStack(Items.FIREWORK_ROCKET));
            require(shots.stream().filter(AbstractArrow.class::isInstance).count()==256&&shots.stream().filter(net.minecraft.world.entity.projectile.FireworkRocketEntity.class::isInstance).count()==1,"source shotgun adds pellets while preserving native firework");clear();
            shots=shoot(LbcImbaCrossbows.Kind.SNIPER,new ItemStack(Items.FIREWORK_ROCKET));
            require(shots.size()==1&&shots.getFirst() instanceof net.minecraft.world.entity.projectile.FireworkRocketEntity&&game.items().wands().electroSnapshot().size()==1,"source sniper retains actual native firework alongside beam");clear();
        }
        void guards(){
            for(var kind:LbcImbaCrossbows.Kind.values())if(kind!=LbcImbaCrossbows.Kind.QUICK){
                var ammo=poison(3);var weapon=equip(kind,ammo);var marker=new Marker(EntityTypes.MARKER,base.owner.level());marker.setPos(base.owner.position());marker.entityTags().add("stopper");owned.add(marker);require(base.owner.level().addFreshEntity(marker),"native TECH field");game.items().magic().loaded(marker);
                require(game.items().magic().mechanismSuppressed(base.owner)&&!base.owner.gameMode.useItem(base.owner,base.owner.level(),weapon,InteractionHand.MAIN_HAND).consumesAction()&&ammo.getCount()==3,"TECH rejects native input before debit "+kind);game.items().magic().unloaded(marker);clear();
                ammo=poison(3);weapon=equip(kind,ammo);base.owner.getInventory().setItem(9,new ItemStack(LbcMagic.MYSTICAL_ARTIFACT));game.items().magic().inventoryChanged(base.owner);game.items().magic().flushCleansing();
                require(game.items().magic().artifactSuppressedAt(base.owner.level(),base.owner.position())&&!base.owner.gameMode.useItem(base.owner,base.owner.level(),weapon,InteractionHand.MAIN_HAND).consumesAction()&&ammo.getCount()==3,"actual strong artifact rejects before debit "+kind);clear();
                weapon=equip(kind,poison(3));base.owner.entityTags().add("time_stopped");try{require(!base.owner.gameMode.useItem(base.owner,base.owner.level(),weapon,InteractionHand.MAIN_HAND).consumesAction(),"personal stop rejects "+kind);}finally{base.owner.entityTags().remove("time_stopped");}clear();
            }
        }
        public void close(){
            if(closed)return;
            closed=true;
            var level=base.owner.level();
            Throwable failure=null;
            for(Runnable cleanup:List.<Runnable>of(
                    ()->{if(coldIntake!=null)coldIntake.close();},
                    ()->{if(ownsVanity)game.vanity().stop();},
                    ()->{if(blastJob!=null&&queuedBlasts().contains(blastJob))blastJob.cancel();},
                    ()->{if(blastPhase!=null)game.items().timeStop().end(level);},
                    ()->blastBlocks.forEach((cell,image)->game.maps().restoreSnapshot(level,cell,image,
                            Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS)),
                    this::clear,
                    ()->{
                        if(blastRulesChanged){
                            level.getGameRules().set(GameRules.MOB_GRIEFING,mobGriefing,game.server());
                            level.getGameRules().set(GameRules.TNT_EXPLODES,tntExplodes,game.server());
                            game.config().tntProtection=tntProtection;
                        }
                        game.config().lbcSkills=skills;
                        game.config().lbcLevelups=levelups;
                        game.config().lbcTrinketLimit=trinketLimit;
                    },
                    base::close)){
                try{cleanup.run();}
                catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            }
            if(failure!=null)throw new IllegalStateException("Scoped crossbow fixture cleanup",failure);
        }
    }
    public static void run(GameController game){
        require(running==null,"exclusive scheduled fixture");
        var f=new Fixture(game);
        try {
            f.presentation();f.shotgun();f.beams();f.continuous();f.rockets();f.guards();f.collectorParity();f.superBeamPackets();f.superDustVisibility();f.concurrentSuperVolleys();
            f.coldIntake=f.new ColdIntake();f.coldIntake.begin();
            running=f;
        }catch(RuntimeException|Error failure){
            try{f.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}
            throw failure;
        }
    }
    public static void tick(GameController game){
        var f=running;
        if(f==null)return;
        try {
            require(f.game==game,"same scheduled game");
            if(f.coldIntake!=null){
                if(!f.coldIntake.nextTick())return;
                var completed=f.coldIntake;f.coldIntake=null;completed.close();f.beginBlastQueue();return;
            }
            if(!f.advanceBlastCheck())return;
            running=null;
            f.close();
            SkyWars2.LOGGER.info("[check] imba-crossbows blast centers={} savedCursor={} maxPublicationsPerTick={}",
                    f.blastCenters.size(),f.capturedCursor,f.mostBlastsInTick);
            SkyWars2.LOGGER.info("[check] PASS imba-crossbows (native volley and ammo, capped queued MOB impacts, TimeStop, attribution and exact Vanity suffix)");
        }catch(RuntimeException|Error failure){
            running=null;
            try{f.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}
            SkyWars2.LOGGER.error("[check] FAIL imba-crossbows",failure);
        }
    }
}
