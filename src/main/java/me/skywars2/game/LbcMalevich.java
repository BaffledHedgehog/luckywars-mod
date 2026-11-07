package me.skywars2.game;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Paper Malevich: deferred live-slot filling and the whole ground conversion/overflow consequence. */
public final class LbcMalevich {
    private static final List<net.minecraft.nbt.CompoundTag> PATTERNS=List.of("luckywars","lws","lucky_wars").stream().map(namespace->{
        var root=new net.minecraft.nbt.CompoundTag();var values=new net.minecraft.nbt.CompoundTag();var item=new net.minecraft.nbt.CompoundTag();
        item.putString(namespace+":type","malevich");values.put(namespace+":item",item);root.put("PublicBukkitValues",values);return root;
    }).toList();
    private static final EquipmentSlot[] ARMOR={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private static final ResourceKey<LootTable> LOOT=ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("lbc:malevich_rnd"));
    private final GameController game;
    // Cache loaded item references, not their identity/stack: native /item replacements remain visible at the next poll.
    private final Set<ItemEntity> items=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<ServerPlayer,Pending> pending=new IdentityHashMap<>();
    private final Set<ServerPlayer> dirty=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<ServerPlayer> candidates=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<ServerPlayer> holders=Collections.newSetFromMap(new IdentityHashMap<>());
    private record Pending(ServerLevel level,Vec3 origin,LootTable table,String epoch,UUID producer){}
    private int phase;
    public LbcMalevich(GameController game){this.game=game;}
    public static boolean matches(ItemStack stack){
        if(stack.isEmpty())return false;
        for(var pattern:PATTERNS)if(LbcTmtInventory.matches(stack,pattern))return true;
        return false;
    }
    private ItemStack carried(ServerPlayer player){
        for(int i=0;i<=40;i++){var stack=player.getInventory().getItem(i);if(matches(stack))return stack;}
        return ItemStack.EMPTY;
    }
    private static boolean room(ServerPlayer player){
        for(int i=0;i<=40;i++)if(player.getInventory().getItem(i).isEmpty())return true;
        return false;
    }
    public void inventoryChanged(ServerPlayer player){dirty.add(player);}
    public void inventoryChanged(ServerPlayer player,ItemStack changed){
        if(holders.contains(player)||matches(changed))dirty.add(player);
    }
    private boolean admitted(ServerPlayer player){
        return player.isAlive()&&!player.isRemoved()&&player.level()==game.maps().level()
                &&game.canUseGameplayItems(player)&&!game.isObserver(player)&&game.stage()!=GameController.Stage.ENDING
                &&game.items().wands().validRoleTarget(player,false)
                &&!game.items().wands().inputTimeStopped(player)&&!game.items().timeStop().stopped(player.level())
                &&!excluded(player.level())
                &&game.players().get(player.getUUID()).lbcSkill!=2
                &&!game.items().magic().beaconSuppressedAabb(player.level(),player.position(),20);
    }
    private static boolean excluded(ServerLevel level){return level.dimension().identifier().equals(Identifier.parse("minecraft:nexus"))
            ||level.dimension().identifier().equals(Identifier.parse("minecraft:imprinted"));}
    private LootTable table(){return game.server().reloadableRegistries().getLootTable(LOOT);}
    private static List<ItemStack> roll(LootTable table,ServerLevel level,Vec3 at){
        // Paper's context has position, no looted entity/killer/luck; failed/empty rolls remain empty.
        try{return table.getRandomItems(new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,at).create(LootContextParamSets.COMMAND));}
        catch(RuntimeException ignored){return List.of();}
    }
    private UUID producer(ItemStack stack,LivingEntity fallback){
        var known=game.items().inventoryHazards().producerIdOf(stack);
        return known!=null?known:fallback==null?null:fallback.getUUID();
    }
    private LivingEntity resolve(UUID id){
        if(id==null)return null;var player=game.server().getPlayerList().getPlayer(id);
        if(player!=null)return player;var entity=game.maps().level().getEntity(id);
        return entity instanceof LivingEntity living?living:null;
    }
    private void lineage(ItemStack stack,UUID producer){
        if(producer!=null)LbcInventoryHazards.recordProducer(stack,producer,game.items().epoch());
    }
    private ItemStack first(Pending job){
        for(var stack:roll(job.table,job.level,job.origin))if(!stack.isEmpty()){lineage(stack,job.producer);return stack;}
        return ItemStack.EMPTY;
    }
    private void fill(ServerPlayer player,Pending job){
        var inventory=player.getInventory();
        // Read the live slot just before each write; no captured inventory array can restore a moved item.
        for(int i=0;i<36;i++)if(inventory.getItem(i).isEmpty()){var stack=first(job);if(!stack.isEmpty())inventory.setItem(i,stack);}
        for(var slot:ARMOR)if(player.getItemBySlot(slot).isEmpty()){var stack=first(job);if(!stack.isEmpty())player.setItemSlot(slot,stack);}
        if(player.getOffhandItem().isEmpty()){var stack=first(job);if(!stack.isEmpty())player.setItemSlot(EquipmentSlot.OFFHAND,stack);}
        inventory.setChanged();
        // Coalesce native slot notifications; full inventories then leave the polling set entirely.
        inventoryChanged(player);candidates.remove(player);
    }
    public void loaded(Entity entity){
        if(entity instanceof ServerPlayer player){inventoryChanged(player);return;}
        if(!(entity instanceof ItemEntity item)||entity.level()!=game.maps().level())return;
        items.add(item);
        if(game.playing()&&matches(item.getItem())){
            game.items().markCurrentItemEpoch(item);
            if(game.items().inventoryHazards().producerIdOf(item.getItem())==null&&item.getOwner() instanceof LivingEntity author)lineage(item.getItem(),author.getUUID());
        }
    }
    public void unloaded(Entity entity){items.remove(entity);}
    private boolean ground(ItemEntity item){
        return !item.isRemoved()&&item.getAge()<5999&&item.level()==game.maps().level()&&game.playing()
                &&game.stage()!=GameController.Stage.ENDING&&!excluded((ServerLevel)item.level())
                &&!item.entityTags().contains("time_stopped")&&!game.items().hasStaleItemEpoch(item)&&matches(item.getItem());
    }
    public void tick(){
        if(game.maps().level()==null||game.stage()==GameController.Stage.ENDING||game.items().wands().timeStopped()
                ||game.items().timeStop().stopped(game.maps().level()))return;
        phase=(phase+1)%20;
        // The existing batch commits before this phase's new admissions, exactly one active tick later.
        var batch=new IdentityHashMap<>(pending);pending.clear();
        for(var entry:batch.entrySet()){
            var player=entry.getKey();var job=entry.getValue();
            if(!game.items().currentEpoch(job.epoch)||player.level()!=job.level||!admitted(player)||carried(player).isEmpty())continue;
            fill(player,job);
        }
        if(phase%5==1){
            for(var player:List.copyOf(dirty)){
                dirty.remove(player);
                boolean holding=player.isAlive()&&!player.isRemoved()&&player.level()==game.maps().level()&&!carried(player).isEmpty();
                if(holding)holders.add(player);else holders.remove(player);
                if(holding&&room(player))candidates.add(player);else candidates.remove(player);
            }
            // Only holders with empty slots remain here, so suppression ending needs no synthetic inventory event.
            for(var player:List.copyOf(candidates))if(admitted(player)){
                var source=carried(player);
                if(source.isEmpty()||!room(player)){candidates.remove(player);if(source.isEmpty())holders.remove(player);continue;}
                pending.put(player,new Pending(player.level(),player.position(),table(),game.items().epoch(),producer(source,player)));
            }
        }
        if(phase==0)groundPulse();
    }
    private void groundPulse(){
        items.removeIf(Entity::isRemoved);
        var males=items.stream().filter(this::ground).toList();if(males.isEmpty())return;
        var table=table();
        for(var male:males){
            if(!ground(male)||game.items().magic().beaconSuppressedAabb((ServerLevel)male.level(),male.position(),20))continue;
            game.items().markCurrentItemEpoch(male);
            boolean converted=false;var at=male.position();var level=(ServerLevel)male.level();
            var author=producer(male.getItem(),game.items().specialDrops().owner(male));
            for(var other:level.getEntitiesOfClass(ItemEntity.class,male.getBoundingBox().inflate(3),item->item!=male&&!item.isRemoved()&&!matches(item.getItem()))){
                converted=true;var position=other.position();other.discard();
                for(var stack:roll(table,level,position))if(!stack.isEmpty()){
                    lineage(stack,author);var replacement=new ItemEntity(level,position.x,position.y,position.z,stack);
                    var living=resolve(author);if(living!=null)replacement.setThrower(living);
                    replacement.setDefaultPickUpDelay();game.items().markCurrentItemEpoch(replacement);level.addFreshEntity(replacement);
                }
            }
            if(!converted&&males.size()>100){explode(males);return;}
        }
    }
    private void explode(List<ItemEntity> males){
        var random=game.maps().level().getRandom();
        for(var male:males){
            if(!ground(male)||game.items().magic().beaconSuppressedAabb((ServerLevel)male.level(),male.position(),20))continue;
            var level=(ServerLevel)male.level();var at=male.position();
            var author=producer(male.getItem(),game.items().specialDrops().owner(male));male.discard();
            int count=random.nextInt(2)+1;
            for(int i=0;i<count;i++){
                var motion=new Vec3(random.nextDouble()-.5,random.nextDouble(),random.nextDouble()-.5).scale(.2);
                game.items().tntDrops().spawnRecordedActor(level,at,author,motion,random.nextInt(11));
            }
        }
    }
    public void forget(ServerPlayer player){pending.remove(player);dirty.remove(player);candidates.remove(player);holders.remove(player);}
    public void stop(){
        phase=0;pending.clear();candidates.clear();holders.clear();dirty.clear();dirty.addAll(game.server().getPlayerList().getPlayers());
        for(var item:List.copyOf(items))if(matches(item.getItem())&&game.items().currentEpoch(item))item.discard();
        items.removeIf(Entity::isRemoved);
    }
    int pendingCount(){return pending.size();}
    int groundCount(){return (int)items.stream().filter(this::ground).count();}
}
