package me.skywars2.game;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import me.skywars2.game.genie.GenieScriptItem;
import me.skywars2.game.genie.GenieScriptRuntime;
import me.skywars2.game.genie.script.ScriptRepository;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.phys.Vec3;

/** Real hits are delivered once to the used item, including after a weapon switch. */
final class GenieHitChecks {
    private final GameController game;
    private final WoolBucketChecks.Fixture base;
    private final GenieScriptRuntime scripts;
    private final CompletableFuture<ScriptRepository.Loaded> program;
    private int phase, when;
    private ItemStack bow, twin;
    private Cow target;
    private UUID arrowId;

    GenieHitChecks(GameController game,WoolBucketChecks.Fixture base){
        this.game=game;this.base=base;scripts=game.items().genie().runtime().scripts();
        program=scripts.prepare("""
            {"version":1,"state":{"count":0},"handlers":{"HIT":"hit"},"functions":{"hit":{
            "parameters":["event"],"code":[
            ["load","state"],["const","count"],["load","state"],["const","count"],["get"],["const",1],
            ["add"],["put"],["drop"],
            ["load","state"],["const","hand"],["load","event"],["const","hand"],["get"],["put"],["drop"],
            ["load","state"],["const","target"],["load","event"],["const","target"],["get"],["put"],["drop"],
            ["load","state"],["const","projectile"],["load","event"],["const","projectiles"],["get"],
            ["const",0],["get"],["put"],["return"]]}}}
            """);
        when=game.server().getTickCount();
    }

    boolean tick(){
        int age=game.server().getTickCount()-when;
        require(age<100,"native scenario phase "+phase+" completes");
        if(phase==0){
            if(!program.isDone())return false;
            base.move(base.owner,base.resident(false));
            base.owner.setNoGravity(true);
            base.owner.setDeltaMovement(Vec3.ZERO);base.owner.setYRot(0);base.owner.setXRot(0);
            melee();
            bow=item("bow");twin=bow.copy();
            equip(ItemStack.EMPTY,bow);
            base.owner.getInventory().setItem(9,new ItemStack(Items.ARROW,2));
            base.client.acknowledge();
            base.owner.connection.handleClientTickEnd(new ServerboundClientTickEndPacket());
            base.owner.connection.handleUseItem(new ServerboundUseItemPacket(InteractionHand.OFF_HAND,31000,0,0));
            require(base.owner.isUsingItem()&&base.owner.getUsedItemHand()==InteractionHand.OFF_HAND,"actual offhand draw");
            next(1);return false;
        }
        if(phase==1){
            if(age<25)return false;
            Set<UUID> before=arrows().stream().map(Entity::getUUID).collect(Collectors.toSet());
            base.owner.releaseUsingItem();base.owner.stopUsingItem();
            var launched=arrows().stream().filter(shot->!before.contains(shot.getUUID())).toList();
            require(launched.size()==1,"real vanilla arrow");
            var arrow=launched.getFirst();
            base.own(arrow);arrowId=arrow.getUUID();
            Vec3 at=arrow.position().add(arrow.getDeltaMovement().scale(2)).add(0,-.45,0);
            target=cow(at,1);
            base.owner.setItemInHand(InteractionHand.OFF_HAND,twin);
            base.owner.getInventory().setItem(10,bow);
            next(2);return false;
        }
        if(target.isAlive()||count(bow)==0)return false;
        drain();
        var state=GenieScriptItem.state(bow);
        require(count(bow)==1&&count(twin)==0,"lethal arrow activates original bow exactly once after switching to same-data twin");
        require(state.get("hand").getAsString().equals("offhand"),"arrow keeps original use hand");
        require(state.get("target").getAsString().equals(target.getUUID().toString()),"arrow event names actual victim");
        require(state.getAsJsonObject("projectile").get("id").getAsString().equals(arrowId.toString()),"arrow event names actual native body");
        var throwable = item("skywars2:throwable_small_fireball");
        throwable.setCount(2);
        fireballHit(throwable, 31001, "hand-thrown fireball");
        var blaze = item(BuiltInRegistries.ITEM.getKey(LbcProjectileWeaponItems.Kind.BLAZE_CROSSBOW.item).toString());
        blaze.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.ofNonEmpty(java.util.List.of(new ItemStack(Items.ARROW))));
        fireballHit(blaze, 31002, "Blaze crossbow conversion");
        base.owner.setNoGravity(false);
        return true;
    }

    private void fireballHit(ItemStack source, int sequence, String description) {
        var sibling = source.copy();
        equip(ItemStack.EMPTY, source);
        var before = fireballs().stream().map(Entity::getUUID).collect(Collectors.toSet());
        base.client.acknowledge();
        base.owner.connection.handleClientTickEnd(new ServerboundClientTickEndPacket());
        base.owner.connection.handleUseItem(new ServerboundUseItemPacket(InteractionHand.OFF_HAND, sequence, 0, 0));
        var launched = fireballs().stream().filter(shot -> !before.contains(shot.getUUID())).toList();
        require(launched.size() == 1, description + " launches one real fireball");
        var shot = launched.getFirst();
        base.own(shot);
        require(source.getCount() == 1, description + " preserves the native post-use stack count");
        base.owner.setItemInHand(InteractionHand.OFF_HAND, sibling);
        base.owner.getInventory().setItem(11, source);
        var at = shot.position().add(shot.getDeltaMovement().normalize().scale(1.5)).add(0, -.5, 0);
        var victim = cow(at, .1f);
        shot.tick();
        drain();
        var state = GenieScriptItem.state(source);
        require(!victim.isAlive() && count(source) == 1 && count(sibling) == 0,
            description + " lethal contact activates the original item exactly once");
        require(state.get("hand").getAsString().equals("offhand")
                && state.get("target").getAsString().equals(victim.getUUID().toString())
                && state.getAsJsonObject("projectile").get("id").getAsString().equals(shot.getUUID().toString()),
            description + " reports the real hand, victim and native fireball");
        base.discard(victim);
        base.discard(shot);
    }

    private java.util.List<SmallFireball> fireballs() {
        return base.level.getEntitiesOfClass(SmallFireball.class, base.owner.getBoundingBox().inflate(16),
            body -> body.getOwner() == base.owner);
    }

    private void melee(){
        var blade=item("iron_sword");
        var clone=blade.copy();
        equip(blade,clone);
        var lethal=cow(base.owner.position().add(0,0,1.5),.1f);
        base.owner.attack(lethal);drain();
        require(!lethal.isAlive()&&count(blade)==1&&count(clone)==0,"lethal melee runs main hand exactly once");
        require(GenieScriptItem.state(blade).get("target").getAsString().equals(lethal.getUUID().toString()),"melee event names victim");
        base.discard(lethal);

        equip(new ItemStack(Items.IRON_SWORD),clone);
        var ordinary=cow(base.owner.position().add(0,0,1.5),.1f);
        base.owner.attack(ordinary);drain();
        require(!ordinary.isAlive()&&count(clone)==0,"ordinary melee leaves offhand script untouched");
        base.discard(ordinary);
        equip(blade,clone);
        var simulated=cow(base.owner.position().add(0,0,1.5),.1f);
        require(simulated.hurtServer(base.level,base.owner.damageSources().playerAttack(base.owner),2),"synthetic melee-shaped damage is accepted");
        drain();
        require(!simulated.isAlive()&&count(blade)==1&&count(clone)==0,"direct damage without native attack scope does not dispatch");
        base.discard(simulated);

        var spear=item("iron_spear");
        equip(clone,spear);
        var live=cow(base.owner.position().add(0,0,1.5),10);
        require(base.owner.stabAttack(EquipmentSlot.OFFHAND,live,4,true,false,false),"native offhand stabbing hit");
        drain();
        require(live.isAlive()&&count(spear)==1&&count(clone)==0,"nonlethal offhand attack runs only spear");
        require(GenieScriptItem.state(spear).get("hand").getAsString().equals("offhand"),"spear event names actual hand");
        base.discard(live);
    }

    private Cow cow(Vec3 position,float health){
        var mob=new Cow(EntityTypes.COW,base.level);mob.setPos(position);mob.setHealth(health);
        mob.setNoAi(true);mob.setNoGravity(true);
        require(base.level.addFreshEntity(mob),"native victim is published");
        base.own(mob);return mob;
    }
    private java.util.List<AbstractArrow> arrows(){
        return base.level.getEntitiesOfClass(AbstractArrow.class,base.owner.getBoundingBox().inflate(16),e->e.getOwner()==base.owner);
    }
    private ItemStack item(String id){return GenieScriptItem.construct(program.join(),id,"Hit test","Counts actual hits.",1);}
    private static int count(ItemStack stack){return GenieScriptItem.state(stack).get("count").getAsInt();}
    private void equip(ItemStack main,ItemStack off){
        base.owner.setItemInHand(InteractionHand.MAIN_HAND,main);base.owner.setItemInHand(InteractionHand.OFF_HAND,off);
    }
    private void drain(){for(int i=0;i<12;i++)scripts.tick();}
    private void next(int phase){this.phase=phase;when=game.server().getTickCount();}
    private static void require(boolean ok,String reason){if(!ok)throw new IllegalStateException("Genie hit: "+reason);}
}
