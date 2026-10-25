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
import java.util.concurrent.CompletableFuture;

/** Player-owned reactive cooldown and the terrain-only consumer of the shared long-ray queue. */
public final class LbcReactive {
    private static final net.minecraft.world.scores.ScoreHolder WALL_COST=net.minecraft.world.scores.ScoreHolder.forNameOnly("shish_wall_wand_mana_use");
    private final GameController game;
    private final LbcMana mana;
    private final Map<ServerPlayer,Integer> cooldowns=new LinkedHashMap<>();
    private final Map<CompletableFuture<HitResult>,ServerPlayer> rays=new LinkedHashMap<>();
    public LbcReactive(GameController game,LbcMana mana) {
        this.game=game; this.mana=mana;
        mana.score(WALL_COST,"lbc.math").set(500);
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
            player.addEffect(new MobEffectInstance(MobEffects.LEVITATION,20,15),player);
            game.items().wands().wandCasted(player);
            return true;
        }
        if(!LbcReactiveItems.isWall(stack) || mana.get(player)<500 && mana.overloadArmor(player)!=15) return false;
        return castWall(player,true);
    }
    boolean castWall(ServerPlayer player,boolean paid){
        var origin=player.getEyePosition(); var direction=player.getLookAngle().normalize();
        int mode=player.isShiftKeyDown()?3:!player.onGround()?2:player.isSprinting()?4:1;
        float yaw=player.getYRot(); String epoch=game.items().epoch(); var level=player.level();
        var pending=game.items().exasperato().searchLoadedBlocks(player,origin.add(direction.scale(.5)),direction,
                (from,to)->trail(player,from,to));
        rays.put(pending,player);
        pending.whenComplete((result,error)-> {
            rays.remove(pending);
            if(error!=null || !(result instanceof BlockHitResult hit) || !game.items().currentEpoch(epoch)
                    || !admitted(player) || player.level()!=level || !level.hasChunkAt(hit.getBlockPos())) return;
            int cost=Math.max(0,mana.score(WALL_COST,"lbc.math").get());
            // A miss is free. Recheck admission on deferred completion before charging exactly one hit.
            if(paid&&!mana.trySpendWand(player,500,cost)) return;
            LbcWall.shishTerminal(game,player,hit,direction,mode,yaw);
            if(paid)game.items().wands().wandCasted(player);
        });
        return true;
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
            remaining--;
            if(remaining==95 && !game.items().magic().suppressed(player)) platform(player);
            if(remaining<=0) cooldowns.remove(player); else cooldowns.put(player,remaining);
        }
        for(var entry:List.copyOf(rays.entrySet())) if(!valid(entry.getValue())) entry.getKey().cancel(false);
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
        cooldowns.remove(player);
        for(var entry:List.copyOf(rays.entrySet())) if(entry.getValue()==player) entry.getKey().cancel(false);
    }
    public void stop() {
        cooldowns.clear();
        for(var pending:List.copyOf(rays.keySet())) pending.cancel(false);
        rays.clear();
    }
}
