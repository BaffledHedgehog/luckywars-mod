package me.skywars2.game;

import java.util.*;
import me.skywars2.game.genie.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import static me.skywars2.game.genie.GenieBlueprint.*;

/** Real bow release, native AI/physics, direct attacker vs kill credit and loaded-body lifetime. */
final class GenieLaunchChecks {
    private final GameController game;
    private final WoolBucketChecks.Fixture base;
    private final Set<UUID> before;
    private int phase,when;
    private Creeper body;
    private Vec3 origin;
    GenieLaunchChecks(GameController game,WoolBucketChecks.Fixture base){
        this.game=game;this.base=base;
        for(String type:List.of("player","fishing_bobber","item","not_a_native_entity"))reject(type,40,200);
        reject("warden",40,200);
        var repeated=plan("stick",Trigger.USE,"creeper",40,600);
        try{GenieRuntime.construct(new GenieBlueprint(repeated.name(),repeated.description(),repeated.icon(),1,List.of(repeated.rules().getFirst(),repeated.rules().getFirst()),List.of(),repeated.twist()));throw new IllegalStateException("excessive simultaneous native population accepted");}catch(IllegalArgumentException expected){}
        var feet=base.owner.blockPosition();
        for(var cell:BlockPos.betweenClosed(feet.offset(-3,0,-2),feet.offset(3,5,8)))base.set(cell.immutable(),Blocks.AIR.defaultBlockState());
        base.owner.getInventory().clearContent();base.owner.getInventory().setSelectedSlot(0);
        base.owner.setYRot(0);base.owner.setXRot(0);base.owner.setDeltaMovement(Vec3.ZERO);
        base.owner.setItemInHand(InteractionHand.MAIN_HAND,GenieRuntime.construct(plan("bow",Trigger.SHOT,"creeper",40,60)));
        base.owner.getInventory().setItem(1,new ItemStack(Items.ARROW,16));
        before=base.level.getEntitiesOfClass(Creeper.class,base.owner.getBoundingBox().inflate(8)).stream().map(Entity::getUUID).collect(java.util.stream.Collectors.toSet());
        base.client.acknowledge();base.owner.connection.handleClientTickEnd(new net.minecraft.network.protocol.game.ServerboundClientTickEndPacket());
        base.owner.connection.handleUseItem(new net.minecraft.network.protocol.game.ServerboundUseItemPacket(InteractionHand.MAIN_HAND,19000,0,0));
        require(base.owner.isUsingItem(),"real native bow starts drawing");
        when=game.server().getTickCount();
    }
    private static GenieBlueprint plan(String icon,Trigger trigger,String type,int cooldown,int life){
        return new GenieBlueprint("A native launcher","Launches a native body.",icon,1,
                List.of(new Rule(trigger,Target.SELF,Condition.ALWAYS,Effect.ENTITY_LAUNCH,type,2,0,life,cooldown,1)),
                List.of(),new Twist(TwistKind.HUNGER_PRICE,1));
    }
    private static void reject(String type,int cooldown,int life){
        try{GenieRuntime.construct(plan("stick",Trigger.USE,type,cooldown,life));throw new IllegalStateException("unsupported native program accepted: "+type);}catch(IllegalArgumentException expected){}
    }
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException(why);}
    boolean tick(){
        int age=game.server().getTickCount()-when;
        if(phase==0){
            if(age<25)return false;
            int drawn=base.owner.getTicksUsingItem(),food=base.owner.getFoodData().getFoodLevel();
            base.owner.releaseUsingItem();base.owner.stopUsingItem();
            var added=base.level.getEntitiesOfClass(Creeper.class,base.owner.getBoundingBox().inflate(8),e->!before.contains(e.getUUID()));
            require(added.size()==1,"one native creeper from actual bow SHOT event, found="+added.size()+" drawn="+drawn+" ammo="+base.owner.getInventory().getItem(1).getCount()+" food="+food+"->"+base.owner.getFoodData().getFoodLevel()+" pos="+base.owner.position());
            body=added.getFirst();base.own(body);origin=body.position();
            require(!body.isNoAi()&&!body.isNoGravity()&&body.getDeltaMovement().z>1.9,"native AI/gravity and requested initial motion retained");
            var source=game.items().spellSources().source(body);
            require(source!=null&&source.emitter().equals(body.getUUID())&&source.author().equals(base.owner.getUUID())&&source.cause().player().equals(base.owner.getUUID()),"native mob body remains emitter while player retains statistical credit");
            require(game.items().spellSources().nativeMobSource(body)==body,"physical attacking mob is never replaced by credit owner");
            require(base.owner.getInventory().getItem(1).getCount()==15,"native bow actually spent one real arrow");
            var arrows=base.level.getEntitiesOfClass(Projectile.class,base.owner.getBoundingBox().inflate(8),e->e.getOwner()==base.owner);
            require(!arrows.isEmpty(),"ordinary bow projectile remains native");arrows.forEach(base::own);
            thorns();
            when=game.server().getTickCount();phase=1;return false;
        }
        if(phase==1){
            if(age<5)return false;
            require(body.isAlive()&&body.position().distanceTo(origin)>2,"launched mob really moves under native physics over multiple frames");
            phase=2;return false;
        }
        if(phase==2){
            if(age<65)return false;
            require(body.isRemoved(),"native mob expires after its real loaded lifetime");
            base.owner.stopUsingItem();base.owner.setXRot(-60);
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,GenieRuntime.construct(plan("stick",Trigger.USE,"ender_pearl",40,60)));
            var previous=base.level.getEntitiesOfClass(ThrownEnderpearl.class,base.owner.getBoundingBox().inflate(8)).stream().map(Entity::getUUID).collect(java.util.stream.Collectors.toSet());
            LbcTmtTriggers.input(base.owner,LbcTmtTriggers.Input.RIGHT_CLICK);
            var pearls=base.level.getEntitiesOfClass(ThrownEnderpearl.class,base.owner.getBoundingBox().inflate(8),e->!previous.contains(e.getUUID()));
            require(pearls.size()==1&&pearls.getFirst().getOwner()==base.owner,"generic native projectile factory preserves real ender-pearl owner");
            var pearl=pearls.getFirst();base.own(pearl);
            pearl.setPos(pearl.getX(),base.level.getMinY()-1,pearl.getZ());game.items().genie().runtime().tick();
            require(pearl.isRemoved(),"generic projectile cannot persist below native min height");
            return true;
        }
        throw new IllegalStateException("unknown native launch phase");
    }
    private void thorns(){
        // Creeper.doHurtTarget deliberately returns true without dealing melee damage.
        // Launch a second real native factory whose ordinary melee actually reaches thorns.
        var bow=base.owner.getMainHandItem();
        var previous=base.level.getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Zombie.class,base.owner.getBoundingBox().inflate(8)).stream().map(Entity::getUUID).collect(java.util.stream.Collectors.toSet());
        base.owner.setItemInHand(InteractionHand.MAIN_HAND,GenieRuntime.construct(plan("stick",Trigger.USE,"zombie",40,60)));
        LbcTmtTriggers.input(base.owner,LbcTmtTriggers.Input.RIGHT_CLICK);
        var zombies=base.level.getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Zombie.class,base.owner.getBoundingBox().inflate(8),e->!previous.contains(e.getUUID()));
        require(zombies.size()==1,"second generic native mob factory");var attacker=zombies.getFirst();base.own(attacker);
        var cow=Objects.requireNonNull(EntityTypes.COW.create(base.level,EntitySpawnReason.TRIGGERED));
        cow.setPos(attacker.position().add(1,0,0));cow.setNoAi(true);
        var plate=new ItemStack(Items.DIAMOND_CHESTPLATE);plate.enchant(base.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.THORNS),10);
        cow.setItemSlot(EquipmentSlot.CHEST,plate);require(base.level.addFreshEntity(cow),"native thorns control");base.own(cow);
        float player=base.owner.getHealth(),mob=attacker.getHealth(),victim=cow.getHealth();
        require(attacker.doHurtTarget((ServerLevel)attacker.level(),cow)&&cow.getHealth()<victim,"native launched mob successfully deals real melee damage");
        require(attacker.getHealth()<mob&&attacker.isAlive()&&base.owner.getHealth()==player,"real thorns reflect to immediate mob, not statistical player author");
        base.discard(cow);base.discard(attacker);base.owner.setItemInHand(InteractionHand.MAIN_HAND,bow);
    }
}
