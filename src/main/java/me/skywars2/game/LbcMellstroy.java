package me.skywars2.game;

import java.io.IOException;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.*;

/** Native mask ownership; the shared ray queue owns all traversal and replay cursors. */
final class LbcMellstroy {
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final EquipmentSlot[] ARMOR={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private static final String[] MUSIC={"minecraft:lbcsounds.business_suka","minecraft:lbcsounds.amamamamam"};
    private final GameController game;
    private final RandomSource random;
    private final LinkedHashSet<UUID> dirty=new LinkedHashSet<>();
    private final LinkedHashMap<UUID,Wearer> wearers=new LinkedHashMap<>();
    private final LinkedHashMap<UUID,LbcExasperato.RayTicket> pending=new LinkedHashMap<>();
    private final LinkedHashSet<UUID> listeners=new LinkedHashSet<>();
    private boolean restoring;
    private int callbacks;
    private long casinos,wins,losses,rays,samples,attempts,walls;

    record Wearer(ServerLevel level,String epoch,int music) {}
    record Counters(long casinos,long wins,long losses,long rays,long samples,long attempts,long walls) {}
    /** Immutable identities only; active ray positions are captured by Exasperato's SearchCheckpoint. */
    record State(Map<UUID,Wearer> wearers,List<UUID> dirty,Map<UUID,LbcExasperato.RayTicket> pending,
                 Set<UUID> listeners,MatchRandomState random,Counters counters) {
        State {wearers=Collections.unmodifiableMap(new LinkedHashMap<>(wearers));dirty=List.copyOf(dirty);
            pending=Collections.unmodifiableMap(new LinkedHashMap<>(pending));listeners=Set.copyOf(listeners);}
    }

    LbcMellstroy(GameController game){this(game,RandomSource.create());}
    LbcMellstroy(GameController game,RandomSource random){this.game=game;this.random=Objects.requireNonNull(random);}
    static boolean mask(ItemStack stack){return !stack.isEmpty()&&ItemData.intValue(stack,"mellstroy_mask",0)==1;}
    void inventoryChanged(ServerPlayer player){if(!restoring)dirty.add(player.getUUID());}
    /** Same-tick loot producers can resolve their already-dirtied owner before consulting the cache. */
    boolean luckyEquipped(ServerPlayer player){
        if(!restoring&&!paused(player)&&dirty.remove(player.getUUID()))refresh(player);
        return equipped(player);
    }
    boolean equipped(ServerPlayer player){var wearer=wearers.get(player.getUUID());return wearer!=null&&wearer.level==player.level()
            &&game.items().currentEpoch(wearer.epoch)&&admitted(player);}
    private boolean admitted(ServerPlayer p){return !restoring&&game.playing()&&p.isAlive()&&!p.isRemoved()
            &&LbcDimensionItems.ordinary(p)&&game.canUseGameplayItems(p)&&!game.isObserver(p);}
    private boolean paused(ServerPlayer p){return game.items().timeStop().stopped(p.level())
            ||game.items().timeStop().frozen(p)||game.items().wands().inputTimeStopped(p);}
    private boolean target(ServerPlayer owner,Entity entity){return entity instanceof LivingEntity
            &&entity.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().target(owner,entity)&&!sameId(owner,entity);}
    private boolean sameId(Entity a,Entity b){var board=game.server().getScoreboard();var objective=board.getObjective("lbcID2");
        if(objective==null)return false;var own=board.getPlayerScoreInfo(a,objective);var foreign=board.getPlayerScoreInfo(b,objective);
        return own!=null&&foreign!=null&&own.value()==foreign.value();}

    private void refresh(ServerPlayer player){
        boolean has=false;if(admitted(player))for(var slot:ARMOR)if(mask(player.getItemBySlot(slot))){has=true;break;}
        var old=wearers.get(player.getUUID());
        if(!has){remove(player);return;}
        if(old!=null&&old.level==player.level()&&game.items().currentEpoch(old.epoch))return;
        remove(player);int selected=random.nextInt(2);wearers.put(player.getUUID(),new Wearer(player.level(),game.items().epoch(),selected));
        stopMusic(player);playMusic(player,selected);
    }
    void tick(){
        if(restoring||!game.server().tickRateManager().runsNormally()||game.server().tickRateManager().isFrozen())return;
        int budget=Math.min(32,dirty.size());
        while(budget-->0){var id=dirty.removeFirst();var p=game.server().getPlayerList().getPlayer(id);
            if(p==null){wearers.remove(id);cancelRay(id);continue;}if(paused(p)){dirty.add(id);continue;}refresh(p);}
        for(var id:List.copyOf(wearers.keySet())){
            var p=game.server().getPlayerList().getPlayer(id);var wearer=wearers.get(id);
            if(p==null){wearers.remove(id);cancelRay(id);continue;}
            if(!admitted(p)||wearer.level!=p.level()||!game.items().currentEpoch(wearer.epoch)){remove(p);continue;}
            if(paused(p))continue;
            if(!game.items().magic().suppressed(p)){
                p.addEffect(ItemEffectLevels.create(MobEffects.REGENERATION,20,0),p);
                p.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,20,0),p);
                p.addEffect(ItemEffectLevels.create(MobEffects.SPEED,20,0),p);
                p.addEffect(ItemEffectLevels.create(MobEffects.HASTE,20,0),p);
                p.addEffect(ItemEffectLevels.create(MobEffects.JUMP_BOOST,20,1),p);
                p.addEffect(ItemEffectLevels.create(MobEffects.SATURATION,20,0),p);
                for(var enemy:p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(6),
                        e->e.position().distanceToSqr(p.position())<=36&&target(p,e)
                                &&!game.items().magic().suppressedAt(p.level(),e.position()))){
                    // Native effect callbacks retain the actual mask wearer's poison/blindness author.
                    enemy.addEffect(ItemEffectLevels.create(MobEffects.BLINDNESS,40,0),p);
                    enemy.addEffect(ItemEffectLevels.create(MobEffects.NAUSEA,100,0),p);
                    enemy.addEffect(ItemEffectLevels.create(MobEffects.HUNGER,20,0),p);
                    enemy.addEffect(ItemEffectLevels.create(MobEffects.POISON,20,0),p);
                }
            }
            var ticket=pending.get(id);if(ticket!=null&&ticket.done())pending.remove(id,ticket);
            if(!pending.containsKey(id)&&!game.items().magic().mechanismSuppressed(p)){
                rays++;game.items().exasperato().replayableSamples(p,p.getEyePosition(),p.getLookAngle(),684,new EyeRay(p));
            }
        }
    }

    private final class EyeRay implements LbcExasperato.ReplayableRay {
        final UUID owner;final ServerLevel level;final String epoch;final Vec3 origin,direction;
        final LbcIndirectKills.Owner cause;
        LbcExasperato.RayTicket ticket;
        EyeRay(ServerPlayer p){owner=p.getUUID();level=p.level();epoch=game.items().epoch();origin=p.getEyePosition();
            direction=p.getLookAngle().normalize();cause=game.items().indirectKills().coldCause(p,false);}
        public void submitted(LbcExasperato.RayTicket ticket){this.ticket=ticket;pending.put(owner,ticket);}
        public double sampleStep(){return .5;}
        public boolean block(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){return !state.is(AIRS);}
        public boolean target(Entity entity){var p=game.server().getPlayerList().getPlayer(owner);return p!=null&&LbcMellstroy.this.target(p,entity);}
        public void trail(Vec3 from,Vec3 to){}
        public HitResult sampleAt(Vec3 point){
            callbacks++;try{
                var p=game.server().getPlayerList().getPlayer(owner);
                if(p==null||p.level()!=level||!game.items().currentEpoch(epoch)||!equipped(p)
                        ||game.items().magic().mechanismSuppressed(p)||game.items().magic().mechanismSuppressedAt(level,point))
                    return BlockHitResult.miss(point,net.minecraft.core.Direction.UP,BlockPos.containing(point));
                samples++;
                double travelled=point.subtract(origin).dot(direction);var from=travelled<.25?origin:point.subtract(direction.scale(.5));
                // Precise native hitboxes; stop at the first non-air voxel, even inside a sample step.
                var fromBlock=BlockPos.containing(from);
                var wall=!level.getBlockState(fromBlock).is(AIRS)
                        ?new BlockHitResult(from,net.minecraft.core.Direction.UP,fromBlock,true)
                        :LbcTerra.wallHit(level,from,point,(pos,state)->!state.is(AIRS));
                Vec3 end=wall==null?point:wall.getLocation();
                var hit=wall!=null&&from.distanceToSqr(end)<1e-14?null:
                        ProjectileUtil.getEntityHitResult(level,p,from,end,new AABB(from,end),this::target,0);
                if(hit!=null){attempts++;hit.getEntity().hurtServer(level,
                        game.items().indirectKills().captureDamage(level.damageSources().source(DamageTypes.GENERIC,p),cause),.05f);}
                for(var listener:level.players())if(listener!=p||travelled>=14.5)
                    level.sendParticles(listener,new DustParticleOptions(0xff0000,1),true,false,end.x,end.y,end.z,1,.05,.05,.05,0);
                if(random.nextInt(10)==0)level.playSound(null,end.x,end.y,end.z,
                        net.minecraft.sounds.SoundEvents.CREEPER_PRIMED,SoundSource.MASTER,.2f,1.5f);
                if(wall!=null){walls++;return wall;}return null;
            }finally{callbacks--;}
        }
        public void complete(HitResult hit){pending.remove(owner,ticket);}
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links){return current->()->{};}
    }

    /** Source scoreboard multiplication and division are signed 32-bit operations, in that order. */
    static int casinoBalance(int balance,boolean win){return win?Math.floorDiv(balance*5,2):0;}
    boolean use(ServerPlayer player,InteractionHand hand){
        if(hand!=InteractionHand.MAIN_HAND||!mask(player.getItemInHand(hand))||!admitted(player)||paused(player))return false;
        checkpointBoundary();var profile=game.players().get(player.getUUID());var before=new MatchValueSnapshot(profile);
        var rng=MatchRandomState.capture(random);int oldCoins=profile.coins,oldMemcoins=profile.memcoins;
        boolean win=random.nextInt(100)<70;
        try{profile.coins=casinoBalance(oldCoins,win);profile.memcoins=casinoBalance(oldMemcoins,win);game.players().saveAtomic();}
        catch(IOException|RuntimeException failure){before.restore();rng.restore(random);
            player.sendSystemMessage(Component.translatable("commands.generic.failed"));return false;}
        casinos++;if(win){wins++;
            player.sendSystemMessage(Component.translatable("you_got").append(Component.literal(Integer.toString(profile.coins-oldCoins))).append(Component.translatable("coins")));
            player.sendSystemMessage(Component.translatable("you_got").append(Component.literal(Integer.toString(profile.memcoins-oldMemcoins))).append(Component.translatable("memcoins")));
            sound(player,player.position(),"minecraft:lbcsounds.skolko_nahui",1,1);
        }else{losses++;
            player.sendSystemMessage(Component.translatable("lost_all_coins").withStyle(ChatFormatting.DARK_RED));
            player.sendSystemMessage(Component.translatable("lost_all_memcoins").withStyle(ChatFormatting.DARK_RED));
            sound(player,player.position(),"minecraft:lbcsounds.uzhe_krasni",1,1);
        }
        return true;
    }

    private static void sound(ServerPlayer listener,Vec3 at,String id,float volume,float pitch){
        listener.connection.send(new ClientboundSoundPacket(Holder.direct(SoundEvent.createVariableRangeEvent(Identifier.parse(id))),
                SoundSource.MASTER,at.x,at.y,at.z,volume,pitch,0));
    }
    private void playMusic(ServerPlayer owner,int selected){for(var listener:owner.level().players())if(listener.distanceToSqr(owner)<=225){
        sound(listener,owner.position(),MUSIC[selected],1,1);listeners.add(listener.getUUID());}}
    private static void stopMusic(ServerPlayer player){for(var id:MUSIC)player.connection.send(new ClientboundStopSoundPacket(Identifier.parse(id),null));}
    private void stopAllMusic(){for(var id:List.copyOf(listeners)){var p=game.server().getPlayerList().getPlayer(id);if(p!=null)stopMusic(p);}listeners.clear();}
    private void cancelRay(UUID owner){var ray=pending.remove(owner);if(ray!=null)ray.cancel();}
    private void remove(ServerPlayer player){if(wearers.remove(player.getUUID())!=null)stopMusic(player);cancelRay(player.getUUID());}
    void forget(ServerPlayer player){if(restoring||MatchPlayerCheckpoint.restoring(player))return;dirty.remove(player.getUUID());remove(player);}
    void stop(){checkpointBoundary();stopAllMusic();for(var ray:List.copyOf(pending.values()))ray.cancel();pending.clear();wearers.clear();dirty.clear();}
    Counters counters(){return new Counters(casinos,wins,losses,rays,samples,attempts,walls);}
    void checkpointBoundary(){if(callbacks!=0)throw new IllegalStateException("Finish Mellstroy sample before checkpoint/replay");}
    State state(){checkpointBoundary();return new State(wearers,List.copyOf(dirty),pending,listeners,MatchRandomState.capture(random),counters());}
    void prepareFrozen(){checkpointBoundary();restoring=true;stopAllMusic();wearers.clear();dirty.clear();pending.clear();}
    void cancelFrozen(){checkpointBoundary();restoring=false;}
    void publishFrozen(){checkpointBoundary();boolean publish=restoring;restoring=false;if(publish)for(var entry:wearers.entrySet()){
        var p=game.server().getPlayerList().getPlayer(entry.getKey());if(p!=null&&admitted(p)&&p.level()==entry.getValue().level)playMusic(p,entry.getValue().music);}}
    void restore(State state){checkpointBoundary();wearers.clear();wearers.putAll(state.wearers);dirty.clear();dirty.addAll(state.dirty);
        pending.clear();pending.putAll(state.pending);listeners.clear();listeners.addAll(state.listeners);state.random.restore(random);
        var c=state.counters;casinos=c.casinos;wins=c.wins;losses=c.losses;rays=c.rays;samples=c.samples;attempts=c.attempts;walls=c.walls;}
    MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links){var saved=state();return current->()->restore(saved);}
}
