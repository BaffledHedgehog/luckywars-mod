package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Real seven-family items, native passenger trees/AI attacks and causal cleanup. */
public final class OrdinarySummonerChecks {
    private OrdinarySummonerChecks() { }
    private static final int[] COUNTS={20,1,2,3,9,3,4};
    private static void require(boolean b,String why){if(!b)throw new IllegalStateException("Ordinary summons: "+why);}
    private static ItemStack loot(AllGrimoireChecks.Fixture f,LbcSummonerItems.Kind kind){
        var list=f.base.game.lbc().rollTable(f.level,f.at,f.base.owner,"lbc:ported/"+kind.id());
        require(list.size()==1&&list.getFirst().is(kind.item()),"exact native narrow loot "+kind.kind());return list.getFirst();
    }
    private static void reset(AllGrimoireChecks.Fixture f){f.restoreWorld=true;f.reset();f.base.owner.getInventory().clearContent();}
    private static void formation(AllGrimoireChecks.Fixture f,LbcSummonerItems.Kind kind,int index){
        var service=f.base.game.items().summons();var actors=service.ordinarySnapshot(kind.kind());
        require(actors.size()==COUNTS[index],"source entity count "+kind.kind()+" actual="+actors.size());
        require(actors.stream().allMatch(e->service.owner(e)==f.base.owner&&f.base.game.items().currentEpoch(e)&&!e.shouldBeSaved()),"direct causal owner/epoch/no saved transient "+kind.kind());
        if(index==0){require(actors.stream().allMatch(e->e.getHealth()==2&&e.getMaxHealth()==2&&e.getMainHandItem().is(Items.GOLDEN_SWORD)&&e.getItemBySlot(EquipmentSlot.HEAD).is(Items.CHAINMAIL_HELMET)),"20 source2HP golden-sword skeletons");
            require(actors.stream().anyMatch(e->e.position().equals(f.at.add(3,0,0)))&&actors.stream().anyMatch(e->e.position().equals(f.at.add(-2,0,2))),"world-coordinate skeleton formation");}
        if(index==1)require(actors.getFirst().getType()==EntityTypes.RAVAGER&&actors.getFirst().getHealth()==40&&actors.getFirst().position().distanceToSqr(f.at.add(-4,0,0))<1e-8,"yaw90 flat forward ravager / source40HP");
        if(index==2)require(actors.stream().allMatch(e->e.getMainHandItem().is(Items.DIAMOND_SWORD)&&e.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE))
                &&actors.stream().anyMatch(e->e.position().distanceToSqr(f.at.add(0,0,1))<1e-8),"flat local elite formation and diamond gear");
        if(index==3){var bat=actors.stream().filter(e->e.getType()==EntityTypes.BAT).findFirst().orElseThrow();var horse=actors.stream().filter(e->e.getType()==EntityTypes.SKELETON_HORSE).findFirst().orElseThrow();var rider=actors.stream().filter(e->e.getType()==EntityTypes.SKELETON).findFirst().orElseThrow();
            require(horse.getVehicle()==bat&&rider.getVehicle()==horse&&rider.getMainHandItem().is(Items.BOW)&&horse.isInvisible(),"native bat/horse/rider tree and authored invisible horse");}
        if(index==4)require(actors.stream().allMatch(e->e.getType()==EntityTypes.GHAST&&e.getY()==f.at.y+20)&&actors.stream().anyMatch(e->e.position().equals(f.at.add(10,20,-10))),"nine high ghasts and exact corner");
        if(index==5)require(actors.stream().allMatch(e->e.getType()==EntityTypes.BLAZE)&&actors.stream().filter(e->e.getVehicle()!=null).count()==2,"three-blaze native passenger tower");
        if(index==6)require(actors.stream().allMatch(e->e.getType()==EntityTypes.GUARDIAN&&e.isNoGravity()),"four source no-gravity guardians");
        require(f.base.recording.packets.stream().filter(ClientboundSoundPacket.class::isInstance).map(ClientboundSoundPacket.class::cast)
                .anyMatch(p->p.getSound().value().location().toString().equals("minecraft:entity.wither.spawn")),"native summon sound "+kind.kind());
    }
    private static Cow cow(AllGrimoireChecks.Fixture f,Vec3 at,float hp){
        var cow=new Cow(EntityTypes.COW,f.level);cow.setPos(at);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);cow.setHealth(hp);cow.setNoAi(true);cow.setNoGravity(true);
        require(f.level.addFreshEntity(cow),"native target");return cow;
    }
    private static void inputs(AllGrimoireChecks.Fixture f){
        var p=f.base.owner;var service=f.base.game.items().summons();int index=0;
        for(var kind:LbcSummonerItems.kinds()){
            for(var hand:InteractionHand.values()){
                reset(f);p.setYRot(90);p.setXRot(65);var stack=loot(f,kind);p.setItemInHand(hand,stack);f.base.mana.set(p,kind.price());
                require(f.base.input(hand)&&p.getItemInHand(hand)==stack&&stack.getCount()==1&&f.base.mana.get(p)==0,"native actual hand/cost/nonconsumption "+kind.kind()+" "+hand);formation(f,kind,index);
                var old=service.ordinarySnapshot(kind.kind());f.base.mana.set(p,kind.price());require(f.base.input(hand)&&old.stream().allMatch(Entity::isRemoved),"recast replaces previous personal formation "+kind.kind());formation(f,kind,index);
                var snapshot=service.snapshot();p.setShiftKeyDown(true);f.base.mana.set(p,-100);require(f.base.input(hand)&&f.base.mana.get(p)==-100&&new HashSet<>(service.snapshot()).equals(new HashSet<>(snapshot)),"Shift commands existing summons without mana or new spawn "+kind.kind());p.setShiftKeyDown(false);
            }
            reset(f);var stack=loot(f,kind);p.setItemInHand(InteractionHand.MAIN_HAND,stack);f.base.mana.set(p,kind.price()-1);
            require(!f.base.input(InteractionHand.MAIN_HAND)&&service.count()==0&&stack.getCount()==1,"insufficient mana preserves item "+kind.kind());
            f.base.mana.set(p,kind.price());p.entityTags().add("time_stopped");try{require(!f.base.input(InteractionHand.MAIN_HAND)&&service.count()==0&&f.base.mana.get(p)==kind.price(),"time stop admission "+kind.kind());}finally{p.entityTags().remove("time_stopped");}
            f.base.field=new Marker(EntityTypes.MARKER,f.level);f.base.field.setPos(f.at);f.base.field.entityTags().add("stopper_magic");require(f.level.addFreshEntity(f.base.field),"native field");f.base.game.items().magic().loaded(f.base.field);
            try{require(!f.base.input(InteractionHand.MAIN_HAND)&&service.count()==0&&f.base.mana.get(p)==kind.price(),"antimagic admission "+kind.kind());}finally{f.base.removeField();}
            index++;
        }
        reset(f);p.setItemInHand(InteractionHand.MAIN_HAND,loot(f,LbcSummonerItems.kinds().get(0)));p.setItemInHand(InteractionHand.OFF_HAND,loot(f,LbcSummonerItems.kinds().get(6)));f.base.mana.set(p,6000);
        require(f.base.input(InteractionHand.MAIN_HAND)&&f.base.mana.get(p)==0&&service.count()==24,"two different summoners cast/pay independently on one input");
        reset(f);for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})p.setItemSlot(slot,
                f.base.game.lbc().rollTable(f.level,f.at,p,switch(slot){case HEAD->"lbc:ported/overload_helmet";case CHEST->"lbc:ported/overload_chestplate";case LEGS->"lbc:ported/overload_leggings";default->"lbc:ported/overload_boots";}).getFirst());
        f.base.mana.inventoryChanged(p);require(f.base.mana.overloadArmor(p)==15,"real complete Overload fixture");
        p.setItemInHand(InteractionHand.MAIN_HAND,loot(f,LbcSummonerItems.kinds().getFirst()));f.base.mana.set(p,0);
        require(f.base.input(InteractionHand.MAIN_HAND)&&f.base.mana.get(p)==-3000&&service.count()==20,"full Overload bypasses only admission, actual mana debt retained");
    }
    private static void combat(AllGrimoireChecks.Fixture f){
        reset(f);var service=f.base.game.items().summons();var p=f.base.owner;
        service.castOrdinary(p,LbcSummonerItems.kinds().getFirst());var skeleton=service.ordinarySnapshot("skeletons").getFirst();
        var target=cow(f,skeleton.position().add(0,0,1),1);require(skeleton.doHurtTarget(f.level,target)&&target.isDeadOrDying()&&target.getKillCredit()==p,"actual skeleton melee kill credits its owner");
        for(String role:List.of("spectator","strict_map_object","magic_academy","rcdenyalways")){
            var guard=cow(f,skeleton.position().add(0,0,1),20);guard.entityTags().add(role);require(!skeleton.canAttack(guard)&&!skeleton.doHurtTarget(f.level,guard)&&guard.getHealth()==20,"native melee rejects protected role "+role);
        }
        require(!skeleton.canAttack(p)&&!skeleton.doHurtTarget(f.level,p),"source owner is protected");
        var converted=skeleton.convertTo(EntityTypes.STRAY,net.minecraft.world.entity.ConversionParams.single(skeleton,true,true),e->{});
        require(converted!=null&&skeleton.isRemoved()&&service.owner(converted)==p&&f.base.game.items().currentEpoch(converted),"native powder-snow conversion retains author/epoch");
        var convertedTarget=cow(f,converted.position().add(0,0,1),1);require(converted.doHurtTarget(f.level,convertedTarget)&&convertedTarget.getKillCredit()==p,"converted native melee retains kill credit");
        reset(f);service.castOrdinary(p,LbcSummonerItems.kinds().get(3));var rider=(Skeleton)service.ordinarySnapshot("ghostrider").stream().filter(e->e instanceof Skeleton).findFirst().orElseThrow();
        var victim=cow(f,rider.position().add(0,0,12),100);rider.performRangedAttack(victim,1);
        var arrow=new ArrayList<AbstractArrow>();for(var e:f.level.getAllEntities())if(!f.base.before.contains(e)&&e instanceof AbstractArrow a)arrow.add(a);
        require(arrow.size()==1&&arrow.getFirst().getOwner()==p&&service.owner(arrow.getFirst())==p&&arrow.getFirst().isOnFire()
                &&arrow.getFirst().pickup==AbstractArrow.Pickup.DISALLOWED,"native enchanted rider arrow retains no-pickup while transferring owner/flame");
        f.beta.teleportTo(f.level,f.at.x,f.at.y+500,f.at.z,Set.of(),0,0,true);f.beta.setNoGravity(true);
        for(int kind:new int[]{4,5}){
            reset(f);var def=LbcSummonerItems.kinds().get(kind);service.castOrdinary(p,def);var shooter=service.ordinarySnapshot(def.kind()).stream().filter(e->e.getPassengers().isEmpty()).findFirst().orElseThrow();shooter.setNoGravity(true);
            var enemy=cow(f,shooter.position().add(0,0,12),1000);shooter.setTarget(enemy);
            // In a native Blaze tower the top rider controls the lower mobs; tick its real root/passenger tree.
            var root=shooter.getRootVehicle();root.setNoGravity(true);
            for(int tick=0;tick<130;tick++)f.level.tickNonPassenger(root);
            var projectiles=new ArrayList<Projectile>();for(var e:f.level.getAllEntities())if(!f.base.before.contains(e)&&!e.isRemoved()&&e instanceof Projectile shot)projectiles.add(shot);
            require(!projectiles.isEmpty()&&projectiles.stream().allMatch(e->e.getOwner()==p&&service.owner(e)==p),"native "+def.kind()+" AI shoots owned projectiles; count="+projectiles.size()+", target="+shooter.getTarget()+", pos="+shooter.position()+", canAttack="+shooter.canAttack(enemy));
        }
        reset(f);service.castOrdinary(p,LbcSummonerItems.kinds().get(6));var guardian=service.ordinarySnapshot("guardians").getFirst();
        f.level.setBlock(BlockPos.containing(f.at.add(0,p.getEyeHeight(),8)),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        var beamTarget=cow(f,guardian.position().add(0,0,12),1000);guardian.setTarget(beamTarget);
        for(int tick=0;tick<130;tick++)f.level.tickNonPassenger(guardian);
        require(beamTarget.getHealth()<1000,"native no-gravity guardian AI beam actually damages a target");
        reset(f);for(int roll=0;roll<128;roll++)for(var item:f.base.game.lbc().rollTable(f.level,f.at,p,"lbc:transformables_summons"))
            require(LbcSummonerItems.kind(item)!=null||item.is(LbcWands.SUMMONER_VEXES),"ordinary weighted pool has only ready native items");
    }
    public static void run(GameController g){try(var f=new AllGrimoireChecks.Fixture(g)){inputs(f);combat(f);}SkyWars2.LOGGER.info("[check] PASS ordinary-summoners");}
    private static AllGrimoireChecks.Fixture retained;private static List<Mob> actors;
    public static void stopProbe(GameController g){require(retained==null,"exclusive stop probe");var f=new AllGrimoireChecks.Fixture(g,true);try{
        for(var kind:LbcSummonerItems.kinds())require(g.items().summons().castOrdinary(f.base.owner,kind),"retained formation "+kind.kind());actors=g.items().summons().snapshot();require(actors.size()>=42,"all seven source formations retained");f.retain=true;retained=f;
    }finally{f.close();}SkyWars2.LOGGER.info("[check] PASS ordinary-summoners-stop-probe");}
    public static void cleanup(GameController g){require(retained!=null,"retained probe");try{require(g.items().summons().count()==0&&actors.stream().allMatch(Entity::isRemoved),"actual stop clears every root and passenger without rewards");}
        finally{retained.restore();retained=null;actors=null;}SkyWars2.LOGGER.info("[check] PASS ordinary-summoners-cleanup");}
}
