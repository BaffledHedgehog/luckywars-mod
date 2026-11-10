package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Real item interactions, native configured actors/death loot and actual match-stop fixtures. */
public final class SpecialEggChecks {
    private SpecialEggChecks() { }
    // This regression owns the original four leaves; later egg families have their own fixtures.
    private static final List<LbcSpecialEggs.Kind> KINDS=List.of(LbcSpecialEggs.Kind.SLAB,LbcSpecialEggs.Kind.LOW_VEX,
            LbcSpecialEggs.Kind.SARVATRA_CREEPER,LbcSpecialEggs.Kind.SHAHID);
    private static Fixture running, stopping;
    private static String stopEpoch;
    private static List<Entity> stopActors=List.of();
    private static ItemEntity stopToken;
    private static void require(boolean value,String message) {
        if(!value)throw new IllegalStateException("Special eggs: "+message);
    }
    private static net.minecraft.nbt.CompoundTag saved(Entity entity) {
        var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,entity.registryAccess());
        entity.saveWithoutId(output);return output.buildResult();
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final ServerLevel level;
        final ServerPlayer owner;
        final GameChecks.WandThirdPlayerState state;
        final Vec3 origin;
        final BlockPos pad;
        final Set<Entity> initial=Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<Entity> owned=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final Set<Long> forced=new HashSet<>();
        final Map<net.minecraft.stats.Stat<Identifier>,Integer> stats=new LinkedHashMap<>();
        final boolean mobDrops;
        int phase,lastTick,wait;
        ItemEntity pending;
        List<Entity> frozen=List.of();
        final Map<Entity,Vec3> positions=new IdentityHashMap<>();
        final Map<PrimedTnt,Integer> fuses=new IdentityHashMap<>();
        Fixture(GameController game) {
            this.game=game;level=game.maps().level();
            owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            require(game.playing()&&owner!=null&&game.allowDamage(owner),"actual active match participant");
            state=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
            mobDrops=level.getGameRules().get(GameRules.MOB_DROPS);
            for(var id:BuiltInRegistries.CUSTOM_STAT){var stat=net.minecraft.stats.Stats.CUSTOM.get(id);stats.put(stat,owner.getStats().getValue(stat));}
            origin=new Vec3(Math.floor(owner.getX()/16)*16+8.5,250,Math.floor(owner.getZ()/16)*16+8.5);
            pad=BlockPos.containing(origin).below();level.getAllEntities().forEach(initial::add);
        }
        void initialize() {
            for(int x=(pad.getX()-8)>>4;x<=(pad.getX()+8)>>4;x++)for(int z=(pad.getZ()-8)>>4;z<=(pad.getZ()+8)>>4;z++){
                long key=net.minecraft.world.level.ChunkPos.pack(x,z);
                if(!level.getForceLoadedChunks().contains(key)){forced.add(key);level.setChunkForced(x,z,true);}level.getChunk(x,z);
            }
            for(var p:BlockPos.betweenClosed(pad.offset(-3,0,-3),pad.offset(3,5,3)))set(p,Blocks.AIR.defaultBlockState());
            set(pad,Blocks.STONE.defaultBlockState());
            owner.closeContainer();owner.getInventory().clearContent();owner.removeAllEffects();owner.setGameMode(GameType.SURVIVAL);
            owner.setNoGravity(true);owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);owner.setHealth(1000);owner.setAbsorptionAmount(0);
            owner.teleportTo(level,origin.x,origin.y+2,origin.z-3,Set.of(),0,0,true);
            level.getGameRules().set(GameRules.MOB_DROPS,true,game.server());lastTick=game.server().getTickCount();
        }
        void set(BlockPos p,net.minecraft.world.level.block.state.BlockState value) {
            p=p.immutable();blocks.computeIfAbsent(p,where->game.maps().snapshot(level,where));
            if(level.getBlockEntity(p) instanceof net.minecraft.world.Container c)c.clearContent();level.setBlock(p,value,818);
        }
        Set<Entity> current() {var out=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(out::add);return out;}
        List<Entity> fresh(Set<Entity> before) {var out=new ArrayList<Entity>();for(var e:level.getAllEntities())if(!before.contains(e)){owned.add(e);out.add(e);}return out;}
        void capture(){for(var e:level.getAllEntities())if(!initial.contains(e)&&e.position().distanceToSqr(origin)<400)owned.add(e);}
        ItemStack egg(LbcSpecialEggs.Kind kind,int count){var s=game.lbc().rollTable(level,origin,owner,"lbc:ported/"+leaf(kind)).getFirst();s.setCount(count);return s;}
        String leaf(LbcSpecialEggs.Kind kind){return switch(kind){case SLAB->"slab_spawn_egg";case LOW_VEX->"low_vex_spawn_egg";case SARVATRA_CREEPER->"sarvatra_creeper_spawn_egg";case SHAHID->"shahid_spawn_egg";case HOHOL->"hohol_spawn_egg";default->throw new IllegalArgumentException("Original egg fixture only: "+kind);};}
        BlockHitResult hit(){return new BlockHitResult(Vec3.atCenterOf(pad).add(0,.5,0),Direction.UP,pad,false);}
        boolean use(ItemStack stack,InteractionHand hand,boolean gameMode){owner.setItemInHand(hand,stack);return (gameMode?owner.gameMode.useItemOn(owner,level,stack,hand,hit()):stack.useOn(new UseOnContext(owner,hand,hit()))).consumesAction();}
        Entity spawn(LbcSpecialEggs.Kind kind,InteractionHand hand,boolean gameMode) {
            var before=current();var stack=egg(kind,2);require(use(stack,hand,gameMode)&&stack.getCount()==1,"actual right-click debits one of two "+kind);
            var actors=fresh(before).stream().filter(e->e instanceof Mob||e instanceof ItemEntity).toList();
            require(actors.size()==1,"exactly one native actor/token "+kind);var actor=actors.getFirst();
            require(game.items().currentEpoch(actor),"pre-insertion current epoch "+kind);
            require(Math.abs(actor.getX()-origin.x)<1e-6&&Math.abs(actor.getZ()-origin.z)<1e-6&&actor.getY()>=origin.y&&actor.getY()<origin.y+1,"native clicked-face position "+kind);
            return actor;
        }
        void presentation() {
            for(var kind:KINDS){
                var stack=egg(kind,2);var flags=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
                require(stack.is(kind.item())&&stack.getItem() instanceof SpawnEggItem&&stack.getItem() instanceof eu.pb4.polymer.core.api.item.PolymerItem,"native Polymer spawn egg "+kind);
                require(BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(Identifier.parse("skywars2:"+leaf(kind))),"public native item ID "+kind);
                require(flags.size()==1&&flags.getIntOr("imba",0)==1,"original imba flag only "+kind);
                var model=switch(kind){case SLAB,SHAHID->"minecraft:zombie_spawn_egg";case LOW_VEX->"minecraft:vex_spawn_egg";case SARVATRA_CREEPER->"minecraft:creeper_spawn_egg";case HOHOL->"minecraft:bat_spawn_egg";default->throw new IllegalArgumentException("Original egg fixture only: "+kind);};
                require(Identifier.parse(model).equals(stack.get(DataComponents.ITEM_MODEL)),"source vanilla proxy model "+kind);
                var name=stack.get(DataComponents.CUSTOM_NAME);var lore=stack.get(DataComponents.LORE);
                require(name!=null&&!name.getStyle().isItalic()&&name.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tr&&tr.getKey().equals(leaf(kind))&&name.getStyle().getColor().getValue()==0xff5555&&lore!=null&&lore.lines().size()==1&&!lore.lines().getFirst().getStyle().isItalic(),"source name/lore formatting "+kind);
                require(Identifier.parse(kind==LbcSpecialEggs.Kind.SLAB||kind==LbcSpecialEggs.Kind.SHAHID?"lbc:tier_ff":"lbc:tier_f").equals(stack.get(DataComponents.TOOLTIP_STYLE))&&stack.get(DataComponents.ENTITY_DATA)!=null,"source tier and complete native entity_data "+kind);
            }
        }
        void admission() {
            for(var kind:KINDS){
                var stack=egg(kind,2);var before=current();game.items().timeStop().begin(level,200);
                try{require(!use(stack,InteractionHand.MAIN_HAND,false)&&stack.getCount()==2&&fresh(before).isEmpty(),"frozen native egg declines freely "+kind);}finally{game.items().timeStop().end(level);}
                var observerState=new GameChecks.WandThirdPlayerState(owner,game.items().mana());game.spectators().enter(owner);
                try{require(!use(stack,InteractionHand.MAIN_HAND,false)&&stack.getCount()==2&&fresh(before).isEmpty(),"observer egg declines freely "+kind);}finally{game.spectators().remove(owner);observerState.restore(game);}
            }
            var remote=pad.offset(2048,0,2048);while(level.getChunkSource().getChunkNow(remote.getX()>>4,remote.getZ()>>4)!=null)remote=remote.offset(2048,0,0);
            var stack=egg(LbcSpecialEggs.Kind.LOW_VEX,2);owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var hit=new BlockHitResult(Vec3.atCenterOf(remote),Direction.UP,remote,false);
            require(!stack.useOn(new UseOnContext(owner,InteractionHand.MAIN_HAND,hit)).consumesAction()&&stack.getCount()==2&&level.getChunkSource().getChunkNow(remote.getX()>>4,remote.getZ()>>4)==null,"unresident native click never forces chunk or debit");
        }
        void slab() {
            var zombie=(Zombie)spawn(LbcSpecialEggs.Kind.SLAB,InteractionHand.MAIN_HAND,false);
            slabPayload(zombie);
            zombie.discard();
        }
        void slabPayload(Zombie zombie) {
            require(zombie.isBaby()&&zombie.isSilent()&&zombie.isPersistenceRequired()&&zombie.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY),"native slab baby/silent/persistence/invisibility");
            var effect=zombie.getEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);
            require(effect.getAmplifier()==1&&effect.getDuration()==2000000&&!effect.isVisible(),"exact slab invisibility payload");
            var head=zombie.getItemBySlot(EquipmentSlot.HEAD);var name=head.get(DataComponents.ITEM_NAME);
            require(head.is(Items.OAK_SLAB)&&head.getCount()==1&&name!=null&&name.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tr&&tr.getKey().equals("agressive_slab")&&head.get(DataComponents.CUSTOM_NAME)==null&&zombie.getDropChances().byEquipment(EquipmentSlot.HEAD)==1,"native oak slab item_name and guaranteed helmet drop");
            var modifiers=head.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers();
            require(modifiers.size()==1&&modifiers.getFirst().modifier().amount()==7&&modifiers.getFirst().modifier().id().equals(Identifier.parse("minecraft:base_attack_damage")),"source egg slab +7 distinct from existing +6 drop");
            require(game.items().slabDrops().owner(zombie)==zombie,"slab adopted by existing native creature lifecycle");
        }
        void offspring() {
            var parent=new Zombie(EntityTypes.ZOMBIE,level);parent.setNoAi(true);parent.setPos(origin.add(2,0,0));
            require(level.addFreshEntity(parent),"existing adult zombie for actual mob interaction");owned.add(parent);
            for(var hand:InteractionHand.values()) {
                var stack=egg(LbcSpecialEggs.Kind.SLAB,2);owner.setItemInHand(hand,stack);var before=current();
                require(parent.interact(owner,hand,Vec3.ZERO).consumesAction()&&stack.getCount()==1,"actual matching-mob use consumes one special egg "+hand);
                var children=fresh(before).stream().filter(e->e instanceof Zombie).map(e->(Zombie)e).toList();
                require(children.size()==1,"matching-mob use creates exactly one child "+hand);var child=children.getFirst();
                slabPayload(child);
                require(!child.getUUID().equals(parent.getUUID())&&child.position().distanceTo(parent.position())<1e-8&&game.items().currentEpoch(child),"configured child keeps native identity, position and epoch");
                require(!parent.isBaby()&&!parent.isSilent()&&parent.getItemBySlot(EquipmentSlot.HEAD).isEmpty()&&!parent.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY),"special egg leaves the existing parent unchanged");
                var helmet=child.getItemBySlot(EquipmentSlot.HEAD).copy();before=current();
                // Native equipment chance1 still requires a player kill; environmental deaths are not preserved drops.
                require(child.hurtServer(level,child.damageSources().playerAttack(owner),1000)&&!child.isAlive(),"configured offspring actual native player kill");
                var drops=fresh(before).stream().filter(e->e instanceof ItemEntity item&&ItemStack.isSameItemSameComponents(item.getItem(),helmet)).toList();
                require(drops.size()==1,"offspring drops its complete guaranteed +7 slab once");fresh(before).forEach(Entity::discard);
            }
            var plain=new ItemStack(Items.ZOMBIE_SPAWN_EGG,2);owner.setItemInHand(InteractionHand.MAIN_HAND,plain);var before=current();
            require(parent.interact(owner,InteractionHand.MAIN_HAND,Vec3.ZERO).consumesAction()&&plain.getCount()==1,"ordinary vanilla egg still creates a baby and consumes one");
            var babies=fresh(before).stream().filter(e->e instanceof Zombie).map(e->(Zombie)e).toList();
            require(babies.size()==1&&babies.getFirst().isBaby()&&!babies.getFirst().isSilent()&&babies.getFirst().getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                    &&!babies.getFirst().hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY)&&!LbcSlabDrops.managed(babies.getFirst()),"vanilla offspring stays ordinary and outside special lifecycle");babies.forEach(Entity::discard);
            for(var kind:List.of(LbcSpecialEggs.Kind.LOW_VEX,LbcSpecialEggs.Kind.SARVATRA_CREEPER,LbcSpecialEggs.Kind.SHAHID)) {
                var stack=egg(kind,2);owner.setItemInHand(InteractionHand.MAIN_HAND,stack);before=current();
                require(!parent.interact(owner,InteractionHand.MAIN_HAND,Vec3.ZERO).consumesAction()&&stack.getCount()==2&&fresh(before).isEmpty(),"native mismatched egg remains a free miss "+kind);
            }
            var stack=egg(LbcSpecialEggs.Kind.SLAB,2);owner.setItemInHand(InteractionHand.MAIN_HAND,stack);before=current();game.items().timeStop().begin(level,200);
            try{require(!parent.interact(owner,InteractionHand.MAIN_HAND,Vec3.ZERO).consumesAction()&&stack.getCount()==2&&fresh(before).isEmpty(),"matching-mob path cannot bypass paused-time admission");}finally{game.items().timeStop().end(level);}
            var observerState=new GameChecks.WandThirdPlayerState(owner,game.items().mana());game.spectators().enter(owner);before=current();
            try{require(!parent.interact(owner,InteractionHand.MAIN_HAND,Vec3.ZERO).consumesAction()&&stack.getCount()==2&&fresh(before).isEmpty(),"matching-mob path cannot bypass observer admission");}finally{game.spectators().remove(owner);observerState.restore(game);}
            parent.discard();
        }
        void seededReward(Mob actor,String tableId,String rewardLeaf) {
            try(var input=SpecialEggChecks.class.getResourceAsStream("/data/lbc/loot_table/"+tableId.substring(4)+".json")) {
                require(input!=null,"native chance wrapper resource "+tableId);
                var json=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                var pool=json.getAsJsonArray("pools").get(0).getAsJsonObject();var entry=pool.getAsJsonArray("entries").get(0).getAsJsonObject();
                var condition=(actor instanceof Vex?pool:entry).getAsJsonObject("condition");
                require(pool.get("rolls").getAsInt()==1&&condition!=null&&condition.get("type").getAsString().equals("minecraft:random_chance")&&condition.get("chance").getAsDouble()==(actor instanceof Vex?.1:.05),"native singular codec retains exact10%/5% source chance");
            }catch(java.io.IOException error){throw new IllegalStateException(error);}
            var table=game.server().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse(tableId)));
            var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,actor.position())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,actor)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE,actor.damageSources().generic())
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
            long win=0,miss=0;
            for(int i=1;i<=512&&(win==0||miss==0);i++){long seed=i*0x9E3779B97F4A7C15L;if(table.getRandomItems(params,seed).isEmpty())miss=seed;else win=seed;}
            require(win!=0&&miss!=0,"native source chance has success and failure seeds "+tableId);
            var tag=saved(actor);tag.putLong("DeathLootTableSeed",win);
            actor.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),tag));
            var before=current();require(actor.hurtServer(level,actor.damageSources().generic(),1000)&&!actor.isAlive(),"actual native seeded death "+tableId);
            var expected=game.lbc().rollTable(level,origin,owner,rewardLeaf).getFirst();
            var rewards=fresh(before).stream().filter(e->e instanceof ItemEntity item&&ItemStack.isSameItemSameComponents(item.getItem(),expected)).toList();
            require(rewards.size()==1,"native death emits complete working reward "+tableId);fresh(before).forEach(Entity::discard);
        }
        void rewards() {
            var vex=(Vex)spawn(LbcSpecialEggs.Kind.LOW_VEX,InteractionHand.OFF_HAND,true);
            require(vex.getMainHandItem().isEmpty()&&vex.getOffhandItem().isEmpty(),"LowVex egg clears native default sword before world insertion");
            require(vex.getHealth()==1&&vex.getMaxHealth()==1&&game.items().creatureDrops().owner(vex)==vex,"native offhand one-HP Vex retains adoption");
            seededReward(vex,"lbc:vex_sword","lbc:ported/vex_sword");
            var creeper=(Creeper)spawn(LbcSpecialEggs.Kind.SARVATRA_CREEPER,InteractionHand.MAIN_HAND,true);
            require(saved(creeper).getByteOr("ExplosionRadius",(byte)-1)==1&&game.items().creeperDrops().owner(creeper)==creeper,"native radius1 creeper retains adoption");
            seededReward(creeper,"lbc:creeper_wand","lbc:ported/air_wand");
        }
        void fluid() {
            set(pad.above(),Blocks.WATER.defaultBlockState());
            owner.teleportTo(level,origin.x,origin.y+2,origin.z,Set.of(),0,90,true);
            var stack=egg(LbcSpecialEggs.Kind.LOW_VEX,2);owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var before=current();require(owner.gameMode.useItem(owner,level,stack,InteractionHand.MAIN_HAND).consumesAction()&&stack.getCount()==1,"actual source-water right-click consumes one egg");
            var actors=fresh(before).stream().filter(e->e instanceof Vex).toList();
            require(actors.size()==1&&((Vex)actors.getFirst()).getHealth()==1&&((Vex)actors.getFirst()).getMainHandItem().isEmpty()&&((Vex)actors.getFirst()).getOffhandItem().isEmpty()&&game.items().creatureDrops().owner(actors.getFirst())==actors.getFirst(),"native fluid ray creates adopted one-HP Vex");actors.forEach(Entity::discard);
            set(pad.above(),Blocks.AIR.defaultBlockState());
            owner.teleportTo(level,origin.x,origin.y+2,origin.z-3,Set.of(),0,0,true);
        }
        void shahid() {
            pending=(ItemEntity)spawn(LbcSpecialEggs.Kind.SHAHID,InteractionHand.MAIN_HAND,false);
            require(pending.hasPickUpDelay()&&saved(pending).getShortOr("PickupDelay",(short)0)==32767&&LbcZombieTntDropItem.matches(pending.getItem()),"source unpickable actual Shahid drop token");
            var flags=pending.getItem().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            require(pending.getItem().is(Items.REPEATING_COMMAND_BLOCK)&&flags.size()==2&&flags.getIntOr("shahid",0)==1&&flags.getIntOr("drop",0)==1&&Identifier.parse("lbc:drop_trap").equals(pending.getItem().get(DataComponents.ITEM_MODEL)),"raw source nested token item/model/flags");
            var at=pending.position();var before=current();require(game.items().specialDrops().spawn(pending)&&pending.isRemoved(),"real dispatcher consumes emitted one-unit token");
            var fresh=fresh(before);var zombies=fresh.stream().filter(e->e instanceof Zombie).map(e->(Zombie)e).toList();
            require(zombies.size()==1,"egg Shahid is one carrier, no four-ring fanout");var zombie=zombies.getFirst();
            require(zombie.isBaby()&&zombie.position().distanceTo(at)<1e-6&&zombie.getPassengers().size()==1&&zombie.getPassengers().getFirst() instanceof PrimedTnt,"one colocated baby and native TNT passenger");
            var tnt=(PrimedTnt)zombie.getPassengers().getFirst();owned.add(tnt);
            require(tnt.getFuse()==200&&game.items().zombieTntDrops().owner(tnt)==zombie&&game.items().currentEpoch(tnt),"exact200 native fuse, author and epoch");
            var cow=new Cow(EntityTypes.COW,level);cow.setPos(at.add(2,0,0));cow.setNoAi(true);require(level.addFreshEntity(cow),"melee target");owned.add(cow);
            float hp=cow.getHealth();require(!zombie.doHurtTarget(level,cow)&&cow.getHealth()==hp&&game.items().zombieTntDrops().wild(tnt)&&game.items().zombieTntDrops().canAffect(tnt,owner),"existing no-melee carrier and WILD TNT self target");cow.discard();
            frozen=List.of(zombie,tnt);for(var e:frozen)positions.put(e,e.position());fuses.put(tnt,tnt.getFuse());
            game.items().timeStop().begin(level,200);
        }
        boolean step() {
            int now=game.server().getTickCount();if(now==lastTick)return false;lastTick=now;require(game.playing(),"actual match retained");
            switch(phase){
                case 0->{presentation();admission();slab();offspring();rewards();fluid();shahid();phase++;}
                case 1->{require(frozen.stream().allMatch(e->!e.isRemoved()&&e.position().distanceTo(positions.get(e))<1e-8)&&fuses.entrySet().stream().allMatch(e->e.getKey().getFuse()==e.getValue()),"actual world Time Stop retains native carrier, passenger and fuse");if(++wait>=3){game.items().timeStop().end(level);for(var e:frozen)e.discard();var actor=spawn(LbcSpecialEggs.Kind.LOW_VEX,InteractionHand.MAIN_HAND,false);actor.entityTags().removeIf(t->t.startsWith("skywars2.items."));actor.entityTags().add("skywars2.items.stale-fixture");game.items().creatureDrops().unloaded(actor);game.items().creatureDrops().loaded(actor);require(actor.isRemoved(),"old-epoch native egg actor quietly rejected");return true;}}
                default->throw new IllegalStateException("Unknown special egg phase "+phase);
            }return false;
        }
        void close(boolean retain) {
            capture();game.items().timeStop().end(level);if(!retain)owned.forEach(Entity::discard);
            for(var entry:blocks.entrySet()){if(level.getBlockEntity(entry.getKey()) instanceof net.minecraft.world.Container c)c.clearContent();game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue());}
            state.restore(game);game.items().indirectKills().clear(owner);stats.forEach((stat,value)->owner.getStats().setValue(owner,stat,value));
            level.getGameRules().set(GameRules.MOB_DROPS,mobDrops,game.server());
            for(long key:forced)level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
        }
        public void close(){close(false);}
    }
    public static void run(GameController game) {
        require(running==null&&stopping==null,"one fixture at a time");var f=new Fixture(game);running=f;
        try{f.initialize();}catch(RuntimeException|Error error){running=null;f.close();throw error;}
    }
    public static void tick(GameController game) {
        if(running==null)return;var f=running;
        try{if(f.step()){f.close();running=null;SkyWars2.LOGGER.info("[check] PASS special-eggs");}}
        catch(Throwable error){try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL special-eggs",error);}
    }
    public static void stopProbe(GameController game) {
        require(running==null&&stopping==null,"exclusive actual stop fixture");var f=new Fixture(game);stopping=f;
        try{
            f.initialize();for(var kind:KINDS)f.spawn(kind,InteractionHand.MAIN_HAND,false);
            var token=f.owned.stream().filter(e->e instanceof ItemEntity).map(e->(ItemEntity)e).findFirst().orElseThrow();
            require(game.items().specialDrops().spawn(token),"actual pre-stop center payload");f.capture();
            stopActors=f.owned.stream().filter(e->!e.isRemoved()&&(e instanceof Mob||e instanceof PrimedTnt)).toList();
            require(stopActors.size()==5,"slab, Vex, creeper and single carrier/TNT before actual stop");stopEpoch=game.items().epoch();
            stopToken=(ItemEntity)f.spawn(LbcSpecialEggs.Kind.SHAHID,InteractionHand.MAIN_HAND,false);stopToken.getItem().setCount(3);
            f.close(true);game.items().timeStop().begin(f.level,200);
            require(!game.items().specialDrops().spawn(stopToken)&&stopToken.getItem().getCount()==3,"pending frozen source stack before stop");
            SkyWars2.LOGGER.info("[check] PASS special-eggs-stop-probe");
        }catch(RuntimeException|Error error){stopping=null;f.close();throw error;}
    }
    public static void cleanup(GameController game) {
        require(stopping!=null&&!game.items().currentEpoch(stopEpoch)&&stopActors.size()==5&&stopActors.stream().allMatch(Entity::isRemoved)&&stopToken!=null&&stopToken.getItem().getCount()==3,"actual stop quietly removes native eggs/payload without debit");
        require(!game.items().specialDrops().spawn(stopToken),"stopped source token never replays");stopToken.discard();
        var level=stopping.level;var at=stopping.origin;
        require(level.getEntities((Entity)null,new net.minecraft.world.phys.AABB(at.add(-5,-5,-5),at.add(5,10,5)),e->e instanceof ItemEntity&&!stopping.initial.contains(e)).isEmpty(),"quiet stop produces no source death rewards");
        stopping=null;stopEpoch=null;stopToken=null;stopActors=List.of();SkyWars2.LOGGER.info("[check] PASS special-eggs-cleanup");
    }
}
