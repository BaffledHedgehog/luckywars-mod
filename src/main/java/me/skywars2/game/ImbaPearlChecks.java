package me.skywars2.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Native pearl contacts, ledger ownership and the existing shared clone/checkpoint paths. */
final class ImbaPearlChecks {
    private ImbaPearlChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("IMBA pearl: "+why);}
    private static List<Entity> release(SpecialFireballChecks.Fixture f,Vec3 at,Vec3 velocity){
        var item=LbcSpecialFireballItems.entries().stream().filter(entry->entry.id().equals("imba_ender_pearl")).findFirst().orElseThrow().item();
        var carrier=new Snowball(f.owner.level(),f.owner,new ItemStack(item));
        carrier.setPos(at);carrier.setYRot(37);carrier.setXRot(-23);carrier.setDeltaMovement(velocity);
        require(f.owner.level().addFreshEntity(carrier)&&f.service.pendingCount()==1,"legacy snowball alias enters the shared adapter");
        var family=f.service.convert(carrier);
        require(carrier.isRemoved()&&family.size()==17,"one original plus sixteen source siblings");
        f.service.tick();require(f.service.pendingCount()==0,"removed carrier leaves no pending adapter");return family;
    }
    private static void geometry(SpecialFireballChecks.Fixture f){
        var motion=new Vec3(-.123456,.234567,1.345678);
        var expected=new Vec3(Math.floor(motion.x*10000)*.00025,Math.floor(motion.y*10000)*.00025,Math.floor(motion.z*10000)*.00025);
        var family=release(f,f.at.add(0,3,4),motion);
        require(family.getFirst().getDeltaMovement().distanceToSqr(expected)<1e-24,"original source read10000/write0.00025 motion");
        for(int i=0;i<family.size();i++){
            var body=family.get(i);require(body instanceof ThrownEnderpearl&&body.getType()==EntityTypes.ENDER_PEARL,"actual native pearl class");
            var pearl=(ThrownEnderpearl)body;
            require(pearl.getOwner()==f.owner&&f.service.owner(pearl)==f.owner&&f.owner.getEnderPearls().contains(pearl),"native owner/pearl ledger uses the thrower");
            require(pearl.position().equals(f.at.add(0,3,4))&&!pearl.isNoGravity()&&pearl.tickCount==0,"source origin, gravity and no intermediate age");
            if(i>0){var velocity=pearl.getDeltaMovement();double[] a={velocity.x,velocity.y,velocity.z},b={expected.x,expected.y,expected.z};
                for(int axis=0;axis<3;axis++){
                    double quantized=a[axis]*10,spread=quantized-Math.floor(b[axis]*10);
                    require(Math.abs(quantized-Math.rint(quantized))<1e-10&&spread>=-10.0000000001&&spread<=10.0000000001,"three independent inclusive source integer spreads");
                }
            }
        }
        require(f.owner.getEnderPearls().size()==17,"one native ledger entry per final pearl");
        var primary=family.getFirst();var original=primary.getDeltaMovement();
        f.game.items().timeStop().begin(f.owner.level(),100);
        try{require(f.game.items().timeStop().frozen(primary)&&f.game.items().timeStop().persistedVelocity(primary,primary.getDeltaMovement()).equals(original),"shared native TimeStop retains accepted pearl motion");}
        finally{f.game.items().timeStop().end(f.owner.level());}
        require(primary.getDeltaMovement().equals(original),"native pearl resumes its stored velocity");
        f.clear();require(f.owner.getEnderPearls().isEmpty(),"native discard unregisters every pearl");
    }
    private static void cloneAndReplay(SpecialFireballChecks.Fixture f){
        var family=release(f,f.at.add(0,3,4),new Vec3(0,0,1));var primary=family.getFirst();
        require(f.game.items().cloneSkills().copyNow(primary,15),"shared x2 admits native pearl");
        var pair=f.service.snapshot().stream().filter(body->body==primary||!family.contains(body)).toList();
        require(pair.size()==2,"one x2 sibling");
        for(var body:pair)require(f.game.items().cloneSkills().copyNow(body,17),"both x2 roots admit x4");
        require(f.service.trackedCount()==24&&f.owner.getEnderPearls().size()==24,"eight root copies plus sixteen original siblings, each in native ledger");
        for(var body:f.service.snapshot())require(((ThrownEnderpearl)body).getOwner()==f.owner,"clone author remains original thrower");
        var captured=Set.copyOf(f.service.snapshot());
        var graph=new MatchQueuedWorkCheckpoint.Graph(new MatchDamageSources(f.game,List.of(f.owner,f.other)));
        var image=f.service.checkpoint(graph);
        require(graph.links.resolve(id->f.owner.level().getEntity(id)).isEmpty(),"existing private pearl links resolve actual native bodies");
        var plan=image.prepare(graph.links);f.service.prepareFrozen();plan.run();f.service.publishFrozen();
        require(Set.copyOf(f.service.snapshot()).equals(captured)&&f.owner.getEnderPearls().equals(captured),"private service replay rebinds exactly the same native pearl ledger");
        f.clear();require(f.owner.getEnderPearls().isEmpty(),"all cloned pearls unregister on owned cleanup");
    }
    private static void nativeContact(SpecialFireballChecks.Fixture f){
        var family=release(f,f.at.add(0,1.5,4),new Vec3(0,0,1));
        var pearl=(ThrownEnderpearl)family.getFirst();for(var sibling:family)if(sibling!=pearl){f.service.unloaded(sibling);sibling.discard();}
        // Aim through the block's middle: native gravity otherwise carries an edge-aligned pearl below it.
        var start=f.at.add(0,1.5,4);var wall=BlockPos.containing(start.add(0,0,2));
        var saved=f.game.maps().snapshot(f.owner.level(),wall);
        var fallDamage=f.owner.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.FALL_DAMAGE);
        try(var client=new RegenerationBeltChecks.ScheduledClient(f.game,f.owner)){
            client.acknowledge();f.owner.removeAllEffects();f.owner.setHealth(f.owner.getMaxHealth());f.owner.setAbsorptionAmount(0);f.owner.setInvulnerableTime(0);f.owner.damageCooldownTime=0;
            f.owner.getAbilities().invulnerable=false;
            f.owner.level().getGameRules().set(net.minecraft.world.level.gamerules.GameRules.FALL_DAMAGE,true,f.game.server());
            f.owner.getAttribute(Attributes.ARMOR).setBaseValue(0);f.owner.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0);
            var damage=f.owner.damageSources().enderPearl();
            require(!f.owner.isInvulnerableTo(f.owner.level(),damage)&&f.game.items().allowDamage(f.owner,damage),"native damage admission: invulnerable="+f.owner.isInvulnerableTo(f.owner.level(),damage)+" common="+f.game.items().allowDamage(f.owner,damage)+" participant="+f.game.allowDamage(f.owner)+" client="+client.diagnostics());
            float health=f.owner.getHealth();f.owner.level().setBlock(wall,Blocks.STONE.defaultBlockState(),2);
            pearl.setPos(start);pearl.setOldPosAndRot();pearl.setDeltaMovement(new Vec3(0,0,4));pearl.tick();client.acknowledge();
            require(pearl.isRemoved()&&f.owner.position().distanceToSqr(start)<1e-10,"real native block contact teleports the actual owner: removed="+pearl.isRemoved()+" owner="+f.owner.position()+" expected="+start+" pearl="+pearl.position()+" motion="+pearl.getDeltaMovement()+" old="+pearl.oldPosition()+" accepting="+f.owner.connection.isAcceptingMessages());
            require(f.owner.getHealth()<health&&f.owner.getLastDamageSource()!=null&&f.owner.getLastDamageSource().is(DamageTypes.ENDER_PEARL),"native ender-pearl damage and handlers run after teleport: before="+health+" after="+f.owner.getHealth()+" source="+f.owner.getLastDamageSource()+" cooldown="+f.owner.damageCooldownTime+" invulnerable="+f.owner.isInvulnerable()+" tags="+f.owner.entityTags());
            require(f.owner.getEnderPearls().isEmpty(),"native impact removes pearl ledger entry");
        }finally{f.owner.level().getGameRules().set(net.minecraft.world.level.gamerules.GameRules.FALL_DAMAGE,fallDamage,f.game.server());f.game.maps().restoreSnapshot(f.owner.level(),wall,saved);f.clear();}
    }
    static void run(GameController game){
        boolean levelups=game.config().lbcLevelups;
        try(var f=new SpecialFireballChecks.Fixture(game)){
            game.config().lbcLevelups=false;f.initialize();require(f.owner.getEnderPearls().isEmpty(),"isolated native pearl ledger");
            geometry(f);cloneAndReplay(f);nativeContact(f);
        }finally{game.config().lbcLevelups=levelups;}
    }
}
