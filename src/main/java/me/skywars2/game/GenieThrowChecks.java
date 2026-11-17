package me.skywars2.game;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import com.google.gson.JsonPrimitive;
import me.skywars2.game.genie.GenieScriptItem;
import me.skywars2.game.genie.GenieScriptRuntime;
import me.skywars2.game.genie.script.ScriptRepository;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Accepted native throws carry source identity past debit and leave identical sibling stacks alone. */
final class GenieThrowChecks {
    private record Carrier(String item,String projectile) {}
    private static final List<Carrier> THROWN=List.of(
        new Carrier("snowball","snowball"),new Carrier("egg","egg"),new Carrier("ender_pearl","ender_pearl"),
        new Carrier("wind_charge","wind_charge"),new Carrier("experience_bottle","experience_bottle"),
        new Carrier("splash_potion","splash_potion"),new Carrier("lingering_potion","lingering_potion"));

    private final GameController game;
    private final WoolBucketChecks.Fixture base;
    private final GenieScriptRuntime scripts;
    private final CompletableFuture<ScriptRepository.Loaded> program;
    private final BlockPos marker;
    private final int started;
    private int sequence=29000,afterDraw;
    private ItemStack trident,twin;

    GenieThrowChecks(GameController game,WoolBucketChecks.Fixture base){
        this.game=game;this.base=base;scripts=game.items().genie().runtime().scripts();
        var anchor=base.resident(false);
        base.move(base.owner,anchor);
        base.owner.setDeltaMovement(Vec3.ZERO);
        marker=BlockPos.containing(anchor).offset(2,-3,2);
        base.set(marker,Blocks.AIR.defaultBlockState());
        String first=setMarker("gold_block"),second=setMarker("emerald_block");
        program=scripts.prepare("""
            {"version":1,"state":{"throws":0},"handlers":{"THROW":"onThrow"},"functions":{"onThrow":{
            "parameters":["event"],"code":[
            ["load","state"],["const","hand"],["load","event"],["const","hand"],["get"],["put"],["drop"],
            ["load","state"],["const","projectile"],["load","event"],["const","projectiles"],["get"],
            ["const",0],["get"],["put"],["drop"],
            ["load","state"],["const","throws"],["load","state"],["const","throws"],["get"],["const",1],
            ["add"],["put"],["const",2],["eq"],["jump_if_false","first"],
            ["const",%s],["call","world.command",1],["return"],
            ["label","first"],["const",%s],["call","world.command",1],["return"]]}}}
            """.formatted(new JsonPrimitive(second),new JsonPrimitive(first)));
        started=game.server().getTickCount();
    }

    boolean tick(){
        int tick=game.server().getTickCount();
        if(afterDraw!=0){
            if(tick-afterDraw<12)return false;
            var before=projectiles();
            releaseUse();
            require(trident.isEmpty()&&base.owner.getOffhandItem().isEmpty(),"native throw consumes the used trident");
            accept(before,"trident",twin,Blocks.GOLD_BLOCK);
            return true;
        }
        require(tick-started<100,"script prepares off-thread");
        if(!program.isDone())return false;
        for(var carrier:THROWN)testCarrier(carrier);
        rejectDrop();
        trident=item("trident",1);
        twin=trident.copy();
        equip(twin,trident);
        useOffhand();
        require(base.owner.isUsingItem(),"native trident draw begins");
        releaseUse();
        reject("early trident release");
        require(!trident.isEmpty(),"early release preserves used trident");
        useOffhand();
        require(base.owner.isUsingItem(),"second real trident draw begins in the offhand");
        afterDraw=tick;
        return false;
    }

    private void testCarrier(Carrier carrier){
        // A two-snowball stack also covers accumulated state as this exact source is consumed.
        var source=item(carrier.item(),carrier.item().equals("snowball")?2:1);
        var twin=source.copy();
        equip(twin,source);
        int count=source.getCount();
        var before=projectiles();
        useOffhand();
        require(source.getCount()==count-1,"one exact offhand debit for "+carrier.item());
        var shot=accept(before,carrier.projectile(),twin,Blocks.GOLD_BLOCK);
        if(source.isEmpty())return;
        var state=GenieScriptItem.state(source);
        require(state.get("throws").getAsInt()==1&&state.get("hand").getAsString().equals("offhand"),
            "one event records the actual used hand");
        var metadata=state.getAsJsonObject("projectile");
        require(metadata.get("id").getAsString().equals(shot.getUUID().toString()),"event records the body published in the world");
        require(metadata.get("type").getAsString().equals("minecraft:"+carrier.projectile()),"event records the native projectile type");
        require(metadata.getAsJsonArray("position").size()==3&&metadata.getAsJsonArray("velocity").size()==3,"event includes launch position and velocity");

        // If native use is rejected by cooldown, it must not publish or trigger a second script.
        var cooldowns=base.owner.getCooldowns();
        var group=cooldowns.getCooldownGroup(source);
        cooldowns.addCooldown(group,20);
        before=projectiles();
        useOffhand();
        require(source.getCount()==1&&projectiles().equals(before),"cooldown rejects the real use");
        reject("rejected snowball");
        cooldowns.removeCooldown(group);

        useOffhand();
        require(source.isEmpty(),"last snowball is consumed");
        accept(before,carrier.projectile(),twin,Blocks.EMERALD_BLOCK);
    }

    private void rejectDrop(){
        var source=item("snowball",1);
        equip(source,ItemStack.EMPTY);
        var before=projectiles();
        var vicinity=base.owner.getBoundingBox().inflate(4);
        var old=base.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,vicinity)
            .stream().map(Entity::getUUID).collect(Collectors.toSet());
        action(ServerboundPlayerActionPacket.Action.DROP_ITEM);
        require(base.owner.getMainHandItem().isEmpty()&&projectiles().equals(before),"Q drops only the item");
        for(var drop:base.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,vicinity,
                e->!old.contains(e.getUUID())))base.discard(drop);
        reject("Q item drop");
    }

    private void reject(String reason){
        for(int i=0;i<4;i++)scripts.tick();
        require(base.level.getBlockState(marker).isAir(),reason+" cannot dispatch THROW");
    }

    private Projectile accept(Set<UUID> before,String expected,ItemStack twin,net.minecraft.world.level.block.Block markerBlock){
        var added=nearbyShots().stream().filter(shot->!before.contains(shot.getUUID())).toList();
        require(added.size()==1,"actual "+expected+" published once");
        var shot=added.getFirst();
        base.own(shot);
        require(BuiltInRegistries.ENTITY_TYPE.getKey(shot.getType()).toString().equals("minecraft:"+expected),
            "real use publishes native "+expected);
        for(int i=0;i<16&&base.level.getBlockState(marker).isAir();i++)scripts.tick();
        require(base.level.getBlockState(marker).is(markerBlock),"only exact used item runs "+expected+" script");
        require(GenieScriptItem.state(twin).get("throws").getAsInt()==0,"same-code same-serial unused twin never runs");
        base.set(marker,Blocks.AIR.defaultBlockState());
        base.discard(shot);
        return shot;
    }

    private Set<UUID> projectiles(){return nearbyShots().stream().map(Entity::getUUID).collect(Collectors.toSet());}
    private List<Projectile> nearbyShots(){
        return base.level.getEntitiesOfClass(Projectile.class,base.owner.getBoundingBox().inflate(10),p->p.getOwner()==base.owner);
    }
    private String setMarker(String block){return "setblock "+marker.getX()+" "+marker.getY()+" "+marker.getZ()+" minecraft:"+block;}
    private ItemStack item(String id,int count){return GenieScriptItem.construct(program.join(),id,"Throw test","Records a native launch.",count);}
    private void equip(ItemStack main,ItemStack off){
        base.owner.setItemInHand(InteractionHand.MAIN_HAND,main);
        base.owner.setItemInHand(InteractionHand.OFF_HAND,off);
    }
    private void useOffhand(){
        ready();
        base.owner.connection.handleUseItem(new ServerboundUseItemPacket(InteractionHand.OFF_HAND,sequence++,0,0));
    }
    private void ready(){
        base.client.acknowledge();
        base.owner.connection.handleClientTickEnd(new ServerboundClientTickEndPacket());
    }
    private void releaseUse(){action(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM);}
    private void action(ServerboundPlayerActionPacket.Action action){
        ready();
        base.owner.connection.handlePlayerAction(new ServerboundPlayerActionPacket(action,BlockPos.ZERO,Direction.DOWN,sequence++));
    }
    private static void require(boolean ok,String reason){if(!ok)throw new IllegalStateException("Genie throw: "+reason);}
}
