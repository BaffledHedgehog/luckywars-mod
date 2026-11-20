package me.skywars2.game;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import me.skywars2.game.genie.*;
import me.skywars2.game.genie.script.ScriptRepository;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.CustomData;
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
    private final CompletableFuture<ScriptRepository.Loaded> replacement;
    private final CompletableFuture<ScriptRepository.Loaded> shot;
    private final CompletableFuture<ScriptRepository.Loaded> rifle;
    private Set<UUID> beforeReplacement;
    private ItemStack replacementBow;
    private ItemStack ordinaryCrossbow;
    GenieLaunchChecks(GameController game,WoolBucketChecks.Fixture base){
        this.game=game;this.base=base;
        replacement=game.items().genie().runtime().scripts().prepare("""
            {"version":1,"state":{},"handlers":{"SHOT_REPLACE":"replace"},"functions":{"replace":{
            "parameters":["event"],"code":[
            ["load","state"],["const","hand"],["load","event"],["const","hand"],["get"],["put"],["drop"],
            ["load","event"],["const","projectiles"],["get"],["store","volley"],
            ["load","state"],["const","volley"],["load","volley"],["put"],["drop"],
            ["load","state"],["const","volley_length"],["load","volley"],["const","length"],["get"],["put"],["drop"],
            ["load","volley"],["const",0],["get"],["store","shot"],
            ["load","state"],["const","body"],["const","minecraft:creeper"],
            ["load","shot"],["const","position"],["get"],["load","shot"],["const","velocity"],["get"],
            ["const",100],["call","entity.launch",4],["put"],["return"]]}}}
            """);
        shot=game.items().genie().runtime().scripts().prepare("""
            {"version":1,"state":{"count":0},"handlers":{"SHOT":"fire"},"functions":{"fire":{
            "parameters":["event"],"code":[
            ["load","state"],["const","count"],["load","state"],["const","count"],["get"],["const",1],
            ["add"],["put"],["drop"],
            ["load","state"],["const","hand"],["load","event"],["const","hand"],["get"],["put"],["drop"],
            ["load","state"],["const","volley"],["load","event"],["const","projectiles"],["get"],["put"],["return"]]}}}
            """);
        rifle=game.items().genie().runtime().scripts().prepare("""
            {"version":1,"state":{"shots":0},"handlers":{"USE_REPLACE":"fire"},"functions":{"fire":{
            "parameters":["event"],"code":[
            ["load","state"],["const","hit"],["load","event"],["const","eye_position"],["get"],
            ["load","event"],["const","look"],["get"],["const",32],["const",8],["call","combat.hitscan",4],
            ["put"],["drop"],["load","state"],["const","shots"],["load","state"],["const","shots"],["get"],["const",1],
            ["add"],["put"],["return"]]}}}
            """);
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
            when=game.server().getTickCount();phase=3;return false;
        }
        if(phase==3){
            require(age<100,"replacement script prepares off the game thread");
            if(!replacement.isDone())return false;
            var code=replacement.join();
            try{GenieScriptItem.construct(code,"stick","Invalid","Use a bow.",1);throw new IllegalStateException("replacement accepted on wrong item type");}
            catch(IllegalArgumentException expected){}
            replacementBow=GenieScriptItem.construct(code,"bow","Replacement bow","Fires only a native creeper.",1);
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            base.owner.setItemInHand(InteractionHand.OFF_HAND,replacementBow);
            base.owner.setXRot(0);base.owner.setYRot(0);
            base.client.acknowledge();base.owner.connection.handleClientTickEnd(new net.minecraft.network.protocol.game.ServerboundClientTickEndPacket());
            base.owner.connection.handleUseItem(new net.minecraft.network.protocol.game.ServerboundUseItemPacket(InteractionHand.OFF_HAND,19001,0,0));
            require(base.owner.isUsingItem()&&base.owner.getUsedItemHand()==InteractionHand.OFF_HAND,"real replacement bow starts drawing in offhand");
            when=game.server().getTickCount();phase=4;return false;
        }
        if(phase==4){
            if(age<25)return false;
            beforeReplacement=projectiles();
            base.owner.releaseUsingItem();base.owner.stopUsingItem();
            require(projectiles().equals(beforeReplacement),"replacement bow never publishes its vanilla arrow");
            require(base.owner.getInventory().getItem(1).getCount()==14&&replacementBow.getDamageValue()==1,
                "replacement bow retains native ammunition and wear");
            // A deferred handler must use the recorded launch vector rather than the player's later aim.
            base.owner.setYRot(180);
            checkReplacement(replacementBow,1,InteractionHand.OFF_HAND);
            var replacementCrossbow=GenieScriptItem.construct(replacement.join(),"crossbow","Replacement crossbow","Keeps native charging.",1);
            replacementCrossbow.enchant(base.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MULTISHOT),1);
            var arrow=new ItemStack(Items.SPECTRAL_ARROW);
            replacementCrossbow.set(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.ofNonEmpty(List.of(arrow.copy(),arrow.copy(),arrow.copy())));
            base.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,replacementCrossbow);
            base.owner.setYRot(0);
            beforeReplacement=projectiles();
            require(base.owner.gameMode.useItem(base.owner,base.level,replacementCrossbow,InteractionHand.MAIN_HAND).consumesAction(),"real multishot crossbow discharge");
            require(replacementCrossbow.getOrDefault(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.EMPTY).isEmpty()
                &&replacementCrossbow.getDamageValue()>0,"replacement crossbow clears loaded ammo and wears");
            require(projectiles().equals(beforeReplacement),"replacement crossbow never publishes its multishot arrows");
            checkReplacement(replacementCrossbow,3,InteractionHand.MAIN_HAND);
            checkActualVolleys();
            ordinaryCrossbow=shotItem(Items.CROSSBOW);
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,ordinaryCrossbow);
            base.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            base.owner.getInventory().setItem(1,new ItemStack(Items.ARROW,3));
            use(19002,InteractionHand.MAIN_HAND,0);
            require(base.owner.isUsingItem(),"scripted SHOT crossbow uses native charging");
            when=game.server().getTickCount();phase=5;return false;
        }
        if(phase==5){
            if(age<27)return false;
            base.owner.releaseUsingItem();base.owner.stopUsingItem();
            require(CrossbowItem.isCharged(ordinaryCrossbow)&&base.owner.getInventory().getItem(1).getCount()==2
                    &&GenieScriptItem.state(ordinaryCrossbow).get("count").getAsInt()==0,
                    "real crossbow load consumes one arrow and does not fire its SHOT handler");
            var original=projectiles();
            use(19003,InteractionHand.MAIN_HAND,0);
            var arrows=base.level.getEntitiesOfClass(Projectile.class,base.owner.getBoundingBox().inflate(8),
                    p->p.getOwner()==base.owner&&!original.contains(p.getUUID()));
            arrows.forEach(base::own);
            try{
                var state=drainItem(ordinaryCrossbow,1,"count");
                require(arrows.size()==1&&!CrossbowItem.isCharged(ordinaryCrossbow)
                        &&ordinaryCrossbow.getDamageValue()==1&&state.getAsJsonArray("volley").size()==1,
                        "native crossbow discharge retains one real projectile, one wear and one SHOT");
            }finally{arrows.forEach(base::discard);}
            checkRifle();
            return true;
        }
        throw new IllegalStateException("unknown native launch phase");
    }
    private void use(int sequence, InteractionHand hand, float yaw) {
        base.client.acknowledge();
        base.owner.connection.handleClientTickEnd(new net.minecraft.network.protocol.game.ServerboundClientTickEndPacket());
        base.owner.connection.handleUseItem(new net.minecraft.network.protocol.game.ServerboundUseItemPacket(hand,sequence,yaw,0));
    }

    private com.google.gson.JsonObject drainItem(ItemStack item, int expected, String counter) {
        var state=GenieScriptItem.state(item);
        for(int i=0;i<24&&(!state.has(counter)||state.get(counter).getAsInt()!=expected);i++){
            game.items().genie().runtime().scripts().tick();
            state=GenieScriptItem.state(item);
        }
        require(state.get(counter).getAsInt()==expected,"exactly "+expected+" scripted actions in "+counter+": "+state);
        return state;
    }

    private void checkRifle() {
        var feet=base.resident(false);
        base.move(base.owner,feet);
        var foot=BlockPos.containing(feet);
        for(var cell:BlockPos.betweenClosed(foot.offset(0,1,0),foot.offset(0,2,6)))
            base.set(cell.immutable(),Blocks.AIR.defaultBlockState());
        var target=Objects.requireNonNull(EntityTypes.COW.create(base.level,EntitySpawnReason.TRIGGERED));
        target.setNoAi(true);target.setNoGravity(true);target.setPos(feet.add(0,.65,5));
        require(base.level.addFreshEntity(target),"native rifle target added");
        base.own(target);
        try{
            var item=GenieScriptItem.construct(rifle.join(),"crossbow","M24","Instant rifle witness.",1);
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,item);
            var wall=foot.offset(0,1,3);
            base.set(wall,Blocks.STONE.defaultBlockState());
            float health=target.getHealth();
            var original=projectiles();
            use(19004,InteractionHand.MAIN_HAND,0);
            require(!base.owner.isUsingItem(),"instant rifle click does not start vanilla charge even with arrows available");
            require(!drainItem(item,1,"shots").get("hit").getAsBoolean()&&target.getHealth()==health,
                    "solid block prevents rifle damage");
            base.set(wall,Blocks.AIR.defaultBlockState());
            use(19005,InteractionHand.MAIN_HAND,0);
            base.owner.setYRot(180);
            require(drainItem(item,2,"shots").get("hit").getAsBoolean()&&target.getHealth()==health-8,
                    "native use packet delivers exactly eight damage along the click-time aim");
            require(target.getLastHurtByMob()==base.owner&&projectiles().equals(original)
                    &&base.owner.getInventory().getItem(1).getCount()==2&&!CrossbowItem.isCharged(item),
                    "hitscan keeps physical player attribution and never fires or charges vanilla ammunition");
        }finally{base.discard(target);}
    }
    private void checkActualVolleys() {
        base.move(base.owner, base.resident(false));
        base.owner.setYRot(0);
        base.owner.setXRot(0);
        var blaze = shotItem(LbcProjectileWeaponItems.Kind.BLAZE_CROSSBOW.item);
        checkActualVolley(blaze, Collections.nCopies(1, "minecraft:small_fireball"));
        checkActualVolley(shotItem(LbcProjectileWeaponItems.Kind.DILDOMET.item), List.of("minecraft:item"));
        checkActualVolley(shotItem(LbcProjectileWeaponItems.Kind.DOUBLE_BOW.item), Collections.nCopies(2, "minecraft:arrow"));
        checkActualVolley(shotItem(LbcProjectileWeaponItems.Kind.SHULKER_BOW.item), List.of("minecraft:shulker_bullet"));

        var firework = shotItem(Items.BOW);
        var fireworkData = scriptData(firework);
        fireworkData.putInt("explosive",1);
        firework.set(DataComponents.CUSTOM_DATA,CustomData.of(fireworkData));
        // The native Firework ability spends one matching bow from inventory.
        // Give it an earlier ordinary donor, so the fired tool remains observable.
        var donor = new ItemStack(Items.BOW);
        var donorData = new CompoundTag();
        donorData.putInt("explosive",1);
        donor.set(DataComponents.CUSTOM_DATA,CustomData.of(donorData));
        base.owner.getInventory().setItem(0,donor);
        checkActualVolley(firework, Collections.nCopies(29,"minecraft:tnt"));
        require(donor.isEmpty(), "native Firework still debits its donor");
        checkActualVolley(shotItem(LbcImbaCrossbows.Kind.SNIPER.item),List.of());

        var tmt = shotItem(LbcTmtItems.prototype(3).getItem());
        var data = scriptData(tmt);
        data.putInt("tmt_item",1);
        var abilities = new ArrayList<LbcTmtAbilities.Ability>();
        for (var flag : List.of("double","fastfired","penisgun")) {
            var bits = new CompoundTag();
            bits.putInt(flag,1);
            abilities.add(new LbcTmtAbilities.Ability("native/genie_check_" + flag,CustomData.of(bits)));
        }
        LbcTmtAbilities.write(data,abilities);
        tmt.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
        checkActualVolley(tmt,List.of("minecraft:arrow","minecraft:item","minecraft:small_fireball"));
    }

    private static CompoundTag scriptData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
    }

    private ItemStack shotItem(Item carrier) {
        return GenieScriptItem.construct(shot.join(),BuiltInRegistries.ITEM.getKey(carrier).toString(),
            "Native shot report","Records the accepted volley.",1);
    }

    private void checkActualVolley(ItemStack stack,List<String> expectedTypes) {
        base.owner.setItemInHand(InteractionHand.OFF_HAND,stack);
        var before = new HashSet<UUID>();
        base.level.getAllEntities().forEach(body -> before.add(body.getUUID()));
        if (stack.getItem() instanceof CrossbowItem) {
            stack.set(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.ofNonEmpty(List.of(new ItemStack(Items.ARROW))));
            require(base.owner.gameMode.useItem(base.owner,base.level,stack,InteractionHand.OFF_HAND).consumesAction(),
                "actual accepted native crossbow discharge");
        } else {
            base.owner.getInventory().setItem(1,new ItemStack(Items.ARROW,4));
            require(base.owner.gameMode.useItem(base.owner,base.level,stack,InteractionHand.OFF_HAND).consumesAction(),
                "actual accepted native bow draw");
            require(stack.getItem().releaseUsing(stack,base.level,base.owner,71980),"actual charged native bow release");
            base.owner.stopUsingItem();
        }
        var live = new HashMap<String,Entity>();
        base.level.getAllEntities().forEach(body -> {
            if (!body.isRemoved() && !before.contains(body.getUUID())) {
                base.own(body);
                live.put(body.getStringUUID(),body);
            }
        });
        try {
            var types = live.values().stream().map(e -> BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString()).sorted().toList();
            require(types.equals(expectedTypes.stream().sorted().toList()),"actual published body types: "+types+" expected="+expectedTypes);
            var scripts = game.items().genie().runtime().scripts();
            for (int i=0;i<16;i++) scripts.tick();
            var state = GenieScriptItem.state(stack);
            require(state.get("count").getAsInt()==1 && state.get("hand").getAsString().equals("offhand"),
                "one SHOT on the exact firing stack");
            var volley = state.getAsJsonArray("volley");
            require(volley.size()==live.size(),"SHOT reports the actual family: "+volley);
            for (var value : volley) {
                var sample = value.getAsJsonObject();
                String type = sample.get("type").getAsString();
                require(types.contains(type) && sample.has("id"),"SHOT must report real body type and ID: "+sample+" actual="+types);
                var body = live.remove(sample.get("id").getAsString());
                require(body!=null && type.equals(BuiltInRegistries.ENTITY_TYPE.getKey(body.getType()).toString()),
                    "SHOT names each published body once");
                require(vector(sample.getAsJsonArray("position")).distanceTo(body.position())<1e-9
                    && vector(sample.getAsJsonArray("velocity")).distanceTo(body.getDeltaMovement())<1e-9,
                    "SHOT reports actual launch geometry");
                base.discard(body);
            }
            require(live.isEmpty(),"no physical member omitted by the event");
        } finally {
            live.values().forEach(base::discard);
        }
    }

    private Set<UUID> projectiles(){
        var known=new HashSet<UUID>();
        for(var shot:base.level.getEntitiesOfClass(Projectile.class,base.owner.getBoundingBox().inflate(24),e->e.getOwner()==base.owner))known.add(shot.getUUID());
        return known;
    }
    private void checkReplacement(ItemStack stack,int count,InteractionHand hand){
        var runtime=game.items().genie().runtime().scripts();
        var state=GenieScriptItem.state(stack);
        // Advance accepted jobs before vanilla physics changes the exact launch vector.
        for(int i=0;i<12&&!state.has("body");i++){
            runtime.tick();
            state=GenieScriptItem.state(stack);
        }
        require(state.has("body")&&!state.get("body").isJsonNull(),"native replacement has room to appear: "+state);
        var body=base.level.getEntity(UUID.fromString(state.get("body").getAsString()));
        require(body instanceof Creeper,"accepted volley launched a real creeper");
        base.own(body);
        require(state.get("hand").getAsString().equals(hand==InteractionHand.OFF_HAND?"offhand":"mainhand"),"replacement captures exact firing hand");
        var volley=state.getAsJsonArray("volley");
        require(volley.size()==count&&state.get("volley_length").getAsInt()==count,
                "numeric indexing and the length property work for a native replacement volley");
        var first=volley.get(0).getAsJsonObject();
        require(first.get("type").getAsString().equals(count==1?"minecraft:arrow":"minecraft:spectral_arrow"),
            "replacement event identifies original projectile type");
        var position=vector(first.getAsJsonArray("position"));
        var velocity=vector(first.getAsJsonArray("velocity"));
        require(velocity.length()>1&&body.getDeltaMovement().distanceTo(velocity)<1e-6&&body.position().distanceTo(position)<1e-6,
            "native creeper starts at the sampled position and velocity: "+body.position()+" "+body.getDeltaMovement()+" sample="+first);
        if(hand==InteractionHand.OFF_HAND)require(velocity.z>1,"bow retains its earlier aim after the player turns");
        if(count==3)require(!volley.get(0).getAsJsonObject().get("velocity").equals(volley.get(1).getAsJsonObject().get("velocity")),
            "native multishot trajectories remain distinct");
        require(beforeReplacement.containsAll(projectiles()),"no vanilla arrow is published alongside the replacement");
        base.discard(body);
    }
    private static Vec3 vector(com.google.gson.JsonArray array){
        return new Vec3(array.get(0).getAsDouble(),array.get(1).getAsDouble(),array.get(2).getAsDouble());
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
