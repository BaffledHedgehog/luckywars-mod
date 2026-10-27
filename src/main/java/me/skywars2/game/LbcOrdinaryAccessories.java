package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import java.util.Set;

/** Safety Platform, Magnet and Devil Stomach share source cadence, never an inventory cache or private cooldown. */
public final class LbcOrdinaryAccessories {
    private final GameController game;
    private int phase;
    public LbcOrdinaryAccessories(GameController game){this.game=game;}
    private boolean admitted(ServerPlayer owner){
        return owner!=null&&owner.isAlive()&&!owner.isRemoved()&&owner.level()==game.maps().level()
                &&game.canUseGameplayItems(owner)&&!game.isObserver(owner)&&game.stage()!=GameController.Stage.ENDING
                &&game.items().wands().validRoleTarget(owner,false)
                // Passive inventory effects continue during ordinary stun and ice, but active Time Stop pauses them.
                &&!game.items().wands().inputTimeStopped(owner)&&!game.items().timeStop().stopped(owner.level())
                &&!owner.level().dimension().identifier().equals(Identifier.parse("minecraft:nexus"))
                &&!owner.level().dimension().identifier().equals(Identifier.parse("minecraft:imprinted"));
    }
    private static boolean flag(ItemStack stack,String name){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr(name,0)==1;}
    private record Inventory(int saveSlot,boolean magnet,boolean stomach){}
    private static Inventory inventory(ServerPlayer owner){
        int save=-1;boolean magnet=false,stomach=false;
        // container.* plus weapon.*: legacy source excludes armor, cursor, crafting and new body/saddle slots.
        for(int index=0;index<=36;index++){
            int slot=index==36?40:index;var stack=owner.getInventory().getItem(slot);if(stack.isEmpty())continue;
            var data=LbcFartus.data(stack);
            if(save<0&&data.getIntOr("save",0)==1)save=slot;
            magnet|=data.getIntOr("magnet",0)==1;stomach|=data.getIntOr("demonkishki",0)==1;
        }
        return new Inventory(save,magnet,stomach);
    }
    public void tick(){
        if(game.maps().level()==null||game.stage()==GameController.Stage.ENDING||game.items().wands().timeStopped())return;
        phase=(phase+1)%20;if(phase%5!=0)return;
        for(var owner:game.server().getPlayerList().getPlayers()){
            if(!admitted(owner))continue;
            var inventory=inventory(owner);
            if(phase==0&&(inventory.saveSlot>=0||inventory.magnet)){
                if(inventory.saveSlot>=0&&!game.items().magic().mechanismSuppressedAt(owner.level(),owner.position()))
                    platform(owner,inventory.saveSlot);
                // Source executes Magnet independently at the post-rescue position, which can enter another beacon.
                if(inventory.magnet&&!game.items().magic().mechanismSuppressedAt(owner.level(),owner.position()))magnet(owner);
            }
            if(inventory.stomach&&!game.items().magic().beaconSuppressed(owner.position()))stomach(owner);
        }
    }
    /** Exact selector box, including default dx/dz zero's one-block extent and strict intersection boundaries. */
    static boolean platformHeight(ServerPlayer owner){
        var box=owner.getBoundingBox();double x=owner.getX(),z=owner.getZ();
        return box.maxX>x&&box.minX<x+1&&box.maxY>-5000010&&box.minY<-9&&box.maxZ>z&&box.minZ<z+1;
    }
    private void platform(ServerPlayer owner,int slot){
        if(!platformHeight(owner))return;
        var level=owner.level();var donor=owner.getInventory().getItem(slot);if(!flag(donor,"save"))return;
        var from=BlockPos.containing(owner.getX()-2,1,owner.getZ()-2);
        var to=BlockPos.containing(owner.getX()+2,1,owner.getZ()+2);
        // Source commands continue independently: a failed teleport or fill does not cancel effects, consumption or the award.
        // /tp ~ 2 ~ retains relative X/Z motion and rotation, unlike Magnet's entity destination.
        var relatives=Relative.union(Relative.direction(true,false,true),Relative.position(true,false,true),Relative.ROTATION);
        if(Level.isInSpawnableBounds(BlockPos.containing(owner.getX(),2,owner.getZ())))teleport(owner,0,2,0,0,0,relatives);
        owner.addEffect(ItemEffectLevels.create(MobEffects.LEVITATION,20,99,false,false,false),owner);
        owner.addEffect(ItemEffectLevels.create(MobEffects.SLOW_FALLING,400,0),owner);
        boolean fill=!level.isDebug()&&!level.isOutsideBuildHeight(from)&&!level.isOutsideBuildHeight(to)
                &&level.getGameRules().get(GameRules.MAX_BLOCK_MODIFICATIONS)>=25;
        if(fill)for(var pos:BlockPos.betweenClosed(from,to))
            if(!Level.isInSpawnableBounds(pos)||!level.hasChunkAt(pos)){fill=false;break;}
        // Guard only this STRICT fill against unavailable terrain; never load chunks or write a partial plane.
        if(fill)game.items().indirectKills().trapChanges(owner,()->LbcDropBlocks.fill(level,from,to,Blocks.SLIME_BLOCK,true));
        // Native /clear has broader scope and order than activation: inventory/equipment, crafting, then cursor.
        owner.getInventory().clearOrCountMatchingItems(stack->flag(stack,"save"),false,1,owner.inventoryMenu.getCraftSlots());
        owner.getInventory().setChanged();
        game.items().mana().award(owner,"lbc:true_advancements/accesories/save_platform");
    }
    /** Match native /tp, including its movement reset; never rebuild an item or change its owner/age/custom payload. */
    private static boolean teleport(Entity entity,double x,double y,double z,float yaw,float pitch,Set<Relative> relatives){
        boolean moved=entity.teleportTo((net.minecraft.server.level.ServerLevel)entity.level(),x,y,z,relatives,Mth.wrapDegrees(yaw),Mth.wrapDegrees(pitch),true);
        if(moved&&!(entity instanceof LivingEntity living&&living.isFallFlying())){
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(1,0,1));entity.setOnGround(true);
        }
        return moved;
    }
    private void magnet(ServerPlayer owner){
        var level=owner.level();var at=owner.position();
        if(flag(owner.getMainHandItem(),"magnet")||flag(owner.getOffhandItem(),"magnet"))
            for(var item:level.getEntitiesOfClass(ItemEntity.class,new AABB(at,at).inflate(30),
                    entity->!entity.isRemoved()&&entity.position().distanceToSqr(at)<=900))
                teleport(item,at.x,at.y,at.z,owner.getYRot(),owner.getXRot(),Set.of());
        // Backpack activation still awards from an already-nearby dangerous drop, independently of the hand-only pull.
        if(!level.getEntitiesOfClass(ItemEntity.class,new AABB(at,at).inflate(1),entity->!entity.isRemoved()
                &&entity.position().distanceToSqr(at)<=1&&(flag(entity.getItem(),"penis_bomb")||flag(entity.getItem(),"malevich"))).isEmpty())
            game.items().mana().award(owner,"lbc:true_advancements/accesories/magnet");
    }
    private void stomach(ServerPlayer owner){
        int health=Mth.ceil((double)owner.getHealth()+owner.getAbsorptionAmount());
        if(health>4||owner.getFoodData().getFoodLevel()<16)return;
        if(health==1)game.items().mana().award(owner,"lbc:true_advancements/accesories/devilstomach");
        owner.addEffect(ItemEffectLevels.create(MobEffects.REGENERATION,40,10,false,false,false),owner);
        owner.addEffect(ItemEffectLevels.create(MobEffects.HUNGER,200,255,false,false,false),owner);
        LbcOverload.sound(owner,"minecraft:entity.wither.ambient",1,0,0);
    }
    public void stop(){phase=0;}
}
