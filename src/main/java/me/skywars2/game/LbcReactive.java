package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;

/** Player-owned reactive cooldown and the terrain-only consumer of the shared long-ray queue. */
public final class LbcReactive {
    private static final net.minecraft.world.scores.ScoreHolder WALL_COST=net.minecraft.world.scores.ScoreHolder.forNameOnly("shish_wall_wand_mana_use");
    private final GameController game;
    private final LbcMana mana;
    private final Map<ServerPlayer,Integer> cooldowns=new LinkedHashMap<>();
    private final Map<LbcExasperato.RayTicket,ServerPlayer> rays=new LinkedHashMap<>();
    public LbcReactive(GameController game,LbcMana mana) {
        this.game=game; this.mana=mana;
        mana.score(WALL_COST,"lbc.math").set(500);
    }
    void setShishCounter(ServerPlayer player,int value){cooldowns.put(player,Math.max(0,value)*5);mana.score(player,"coldownshish").set(value);}
    void reduceSourceCooldown(ServerPlayer player,int value) {
        int ticks=cooldownTicks(player),current=(int)(((long)ticks+4)/5);
        int remaining=(int)Math.max(0,(long)ticks-((long)current-value)*5);
        if(remaining==0)cooldowns.remove(player);else cooldowns.put(player,remaining);
        mana.score(player,"coldownshish").set(value);
    }
    private boolean valid(ServerPlayer player) { return mana.visible(player) && game.items().wands().validTarget(player); }
    private boolean admitted(ServerPlayer player) {
        return valid(player) && !game.items().actionsBlocked(player)
                && !game.items().timeStop().stopped(player.level())
                && !game.items().magic().suppressed(player);
    }
    public boolean use(ServerPlayer player,InteractionHand hand) {
        if(!admitted(player) || hand==InteractionHand.OFF_HAND && !game.config().offhandWands) return false;
        var stack=player.getItemInHand(hand);
        if(LbcReactiveItems.isReactive(stack)) {
            if(cooldownTicks(player)>0) return false;
            cooldowns.put(player,125);
            player.addEffect(ItemEffectLevels.create(MobEffects.LEVITATION,20,15),player);
            game.items().wands().wandCasted(player);
            return true;
        }
        if(!LbcReactiveItems.isWall(stack) || mana.get(player)<500 && mana.overloadArmor(player)!=15) return false;
        return castWall(player,true);
    }
    boolean castWall(ServerPlayer player,boolean paid){
        var origin=player.getEyePosition(); var direction=player.getLookAngle().normalize();
        int mode=player.isShiftKeyDown()?3:!player.onGround()?2:player.isSprinting()?4:1;
        game.items().exasperato().replayableLoaded(player,origin.add(direction.scale(.5)),direction,
                LbcExasperato.RayMode.TERRAIN,Double.POSITIVE_INFINITY,new WallAim(player,direction,mode,paid));
        return true;
    }
    private final class WallAim implements LbcExasperato.ReplayableRay {
        final UUID author;final ServerLevel level;final String epoch;final Vec3 direction;final int mode;final float yaw;final boolean paid;
        LbcExasperato.RayTicket ticket;
        WallAim(ServerPlayer player,Vec3 direction,int mode,boolean paid){author=player.getUUID();level=player.level();epoch=game.items().epoch();this.direction=direction;this.mode=mode;this.paid=paid;yaw=player.getYRot();}
        ServerPlayer owner(){return game.server().getPlayerList().getPlayer(author);}
        public void submitted(LbcExasperato.RayTicket value){ticket=value;rays.put(value,owner());}
        public boolean block(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){return true;}
        public boolean target(net.minecraft.world.entity.Entity entity){return false;}
        public void trail(Vec3 from,Vec3 to){var owner=owner();if(owner!=null)LbcReactive.this.trail(owner,from,to);}
        public void complete(HitResult result){
            rays.remove(ticket);var player=owner();
            if(!(result instanceof BlockHitResult hit)||!game.items().currentEpoch(epoch)||player==null
                    ||!admitted(player)||player.level()!=level||!level.hasChunkAt(hit.getBlockPos()))return;
            completing++;try{
                int cost=Math.max(0,mana.score(WALL_COST,"lbc.math").get());
                // A miss is free. Deferred completion rechecks payment exactly once against the CURRENT player.
                if(paid&&!mana.trySpendWand(player,500,cost))return;
                LbcWall.shishTerminal(game,player,hit,direction,mode,yaw);
                if(paid)game.items().wands().wandCasted(player);
            }finally{completing--;}
        }
    }
    private int completing;private boolean advancing;
    void checkpointBoundary(){if(completing!=0||advancing)throw new IllegalStateException("Finish reactive callbacks before checkpoint");}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){checkpointBoundary();return new Checkpoint(links);}
    final class Checkpoint {
        final Map<MatchDamageSources.Ref,Integer> clocks=new LinkedHashMap<>();
        final Map<LbcExasperato.RayTicket,MatchDamageSources.Ref> requests=new LinkedHashMap<>();
        Checkpoint(MatchSpellCheckpoint.Links links){cooldowns.forEach((p,n)->clocks.put(links.key(p),n));rays.forEach((r,p)->requests.put(r,links.key(p)));}
        Runnable prepare(MatchSpellCheckpoint.Links links){
            var values=new LinkedHashMap<ServerPlayer,Integer>();clocks.forEach((ref,n)->values.put((ServerPlayer)links.get(ref),n));
            var pending=new LinkedHashMap<LbcExasperato.RayTicket,ServerPlayer>();requests.forEach((r,ref)->pending.put(r,(ServerPlayer)links.get(ref)));
            return ()->{cooldowns.clear();cooldowns.putAll(values);rays.clear();rays.putAll(pending);};
        }
    }
    private void trail(ServerPlayer owner,Vec3 from,Vec3 to) {
        var delta=to.subtract(from); double length=delta.length();
        if(length<1e-9) return;
        for(double d=0;d<length;d+=.25) {
            var p=from.add(delta.scale(d/length));
            owner.level().sendParticles(new ItemParticleOption(ParticleTypes.ITEM,Items.BRICKS),p.x,p.y-.2,p.z,1,0,0,0,.1);
            owner.level().sendParticles(new ItemParticleOption(ParticleTypes.ITEM,Items.SPRUCE_SAPLING),p.x,p.y-.2,p.z,1,0,0,0,.1);
        }
    }
    public void tick() {
        if(advancing)throw new IllegalStateException("Reentrant reactive tick");advancing=true;try {
        for(var entry:List.copyOf(cooldowns.entrySet())) {
            var player=entry.getKey(); int remaining=entry.getValue();
            if(!valid(player)) { clear(player); continue; }
            if(game.items().timeStop().stopped(player.level()) || player.entityTags().contains("time_stopped")) continue;
            if(remaining>100) {
                player.level().sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,Blocks.SPRUCE_SAPLING.defaultBlockState()),
                        true,false,player.getX(),player.getY(),player.getZ(),10,.1,.1,.1,.1);
                player.level().sendParticles(ParticleTypes.HAPPY_VILLAGER,player.getX(),player.getY(),player.getZ(),1,.1,.1,.1,0);
                LbcOverload.sound(player,"minecraft:block.grass.step",1,0,0);
            }
            remaining--;mana.score(player,"coldownshish").set((remaining+4)/5);
            if(remaining==95 && !game.items().magic().suppressed(player)) platform(player);
            if(remaining<=0) cooldowns.remove(player); else cooldowns.put(player,remaining);
        }
        for(var entry:List.copyOf(rays.entrySet())) if(!valid(entry.getValue())) entry.getKey().cancel();
        }finally{advancing=false;}
    }
    private void platform(ServerPlayer player) {
        var level=player.level(); var at=player.blockPosition(); int y=at.getY()>=322?319:at.getY()-2;
        var leaves=Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,false);
        game.items().indirectKills().trapChanges(player,()-> {
            for(var cell:BlockPos.betweenClosed(new BlockPos(at.getX()-1,y,at.getZ()-1),new BlockPos(at.getX()+1,y,at.getZ()+1)))
                if(!level.isOutsideBuildHeight(cell) && level.getChunkSource().getChunkNow(cell.getX()>>4,cell.getZ()>>4)!=null) {
                    if(level.getBlockEntity(cell) instanceof Container inventory) {
                        Containers.dropContents(level,cell,inventory); inventory.clearContent();
                    }
                    level.setBlock(cell,leaves,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
                }
        });
    }
    public int cooldownTicks(ServerPlayer player) { return cooldowns.getOrDefault(player,0); }
    public int hudValue(ServerPlayer player) { return (cooldownTicks(player)+4)/5; }
    /** KFC adds one original five-tick score unit, rather than silently changing its meaning to a tick. */
    public void addPlayerCounter(ServerPlayer player) { cooldowns.put(player,cooldownTicks(player)+5); }
    public int activeCount() { return cooldowns.size()+rays.size(); }
    public int pendingRayCount() { return rays.size(); }
    public void clear(ServerPlayer player) {
        cooldowns.remove(player);mana.score(player,"coldownshish").set(0);
        for(var entry:List.copyOf(rays.entrySet())) if(entry.getValue()==player) entry.getKey().cancel();
    }
    public void stop() {
        cooldowns.clear();
        for(var pending:List.copyOf(rays.keySet())) pending.cancel();
        rays.clear();
    }
}
