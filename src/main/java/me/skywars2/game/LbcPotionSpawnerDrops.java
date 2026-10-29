package me.skywars2.game;

import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.AABB;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Source-configured native spawners; vanilla owns activation, selection, delay and potion flight. */
public final class LbcPotionSpawnerDrops {
    public interface StateAccess { CompoundTag skywars2$potionMetadata(); }
    public static final String DATA="skywars2_potion_spawner";
    private static final String ACTOR="skywars2.potion_spawner_drop";
    private static final String OWNER="skywars2.potion_spawner_owner.";
    private static final String EVENT_CAUSE="skywars2.potion_spawner_event.";
    private record Potion(String effect,int amplifier,int duration,int color) { }
    private static final List<Potion> POTIONS=List.of(
            new Potion("instant_damage",0,20,8388608),new Potion("slowness",4,600,2035284),
            new Potion("nausea",1,600,1935126),new Potion("blindness",1,600,0),
            new Potion("hunger",127,400,5476112),new Potion("weakness",0,400,4728399),
            new Potion("mining_fatigue",2,600,13433087),new Potion("poison",2,300,6618974),
            new Potion("wither",2,300,10921638),new Potion("glowing",1,1200,16774925),
            new Potion("levitation",10,40,16192767),new Potion("speed",20,600,14417906),
            new Potion("haste",127,600,16756293),new Potion("strength",0,800,16384000),
            new Potion("jump_boost",20,600,13762327),new Potion("slow_falling",1,600,3932152),
            new Potion("instant_damage",1,20,4521984),new Potion("levitation",0,400,12910585));
    private final GameController game;
    private final Map<BaseSpawner,SpawnerBlockEntity> spawners=new IdentityHashMap<>();
    private final Set<ThrownSplashPotion> potions=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<UUID,ServerPlayer> authors=new HashMap<>();

    public LbcPotionSpawnerDrops(GameController game) { this.game=game; }

    /** Fresh native NBT, with source values only; lifecycle metadata is attached at admission. */
    public static CompoundTag payload(LbcPotionSpawnerDropItem.Kind kind) {
        var tag=new CompoundTag();boolean old=kind==LbcPotionSpawnerDropItem.Kind.OLD;
        tag.putInt("SpawnCount",1);tag.putInt("Delay",1);tag.putInt("MinSpawnDelay",4);tag.putInt("MaxSpawnDelay",5);
        tag.putInt("SpawnRange",old?15:6);tag.putInt("MaxNearbyEntities",old?1000:200);
        tag.putInt("RequiredPlayerRange",old?100:35);
        var first=new CompoundTag();first.put("entity",potion(POTIONS.getFirst()));tag.put("SpawnData",first);
        var potentials=new ListTag();for(var potion:POTIONS) {
            var data=new CompoundTag();data.put("entity",potion(potion));
            var entry=new CompoundTag();entry.putInt("weight",1);entry.put("data",data);potentials.add(entry);
        }
        tag.put("SpawnPotentials",potentials);return tag;
    }

    private static CompoundTag potion(Potion potion) {
        var effect=new CompoundTag();effect.putString("id","minecraft:"+potion.effect());
        effect.putByte("amplifier",(byte)potion.amplifier());effect.putInt("duration",potion.duration());
        var effects=new ListTag();effects.add(effect);var contents=new CompoundTag();
        contents.putInt("custom_color",potion.color());contents.put("custom_effects",effects);
        var components=new CompoundTag();components.put("minecraft:potion_contents",contents);
        var item=new CompoundTag();item.putString("id","minecraft:splash_potion");item.putInt("count",1);item.put("components",components);
        var entity=new CompoundTag();entity.putString("id","minecraft:splash_potion");entity.put("Item",item);return entity;
    }

    public boolean ready(ItemEntity token,LbcPotionSpawnerDropItem.Kind kind) {
        if(token.isRemoved() || token.getAge()>=5999 || !LbcPotionSpawnerDropItem.matches(token.getItem(),kind)
                || !game.playing() || game.stage()==GameController.Stage.ENDING
                || token.level()!=game.maps().level() || !(token.level() instanceof ServerLevel level)
                || game.items().timeStop().stopped(level))return false;
        for(var tag:token.entityTags())if(tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag))return false;
        var pos=BlockPos.containing(token.position().add(0,10,0));
        // Placement only. Native producer candidates are checked individually at their collision test.
        return resident(level,new AABB(pos).inflate(1));
    }

    public boolean spawn(ItemEntity token,LbcPotionSpawnerDropItem.Kind kind) {
        if(!ready(token,kind))return false;
        var level=(ServerLevel)token.level();var cause=game.items().specialDrops().owner(token);
        var author=LbcIndirectKills.playerSource(cause);if(author!=null)authors.put(author.getUUID(),author);
        if(kind==LbcPotionSpawnerDropItem.Kind.NORMAL)level.sendParticles(
                ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT,0xFF00FF00),
                token.getX(),token.getY()+5,token.getZ(),300,.3,4,.3,0);
        var pos=BlockPos.containing(token.position().add(0,10,0));
        if(level.isDebug() || level.isOutsideBuildHeight(pos))return true;
        var tag=payload(kind);var metadata=new CompoundTag();metadata.putString("epoch",game.items().epoch());
        metadata.putBoolean("closed",false);if(author!=null)metadata.putString("owner",author.getUUID().toString());
        tag.put(DATA,metadata);enrich(tag.getCompoundOrEmpty("SpawnData").getCompoundOrEmpty("entity"),metadata);
        for(var raw:tag.getListOrEmpty("SpawnPotentials"))if(raw instanceof CompoundTag entry)
            enrich(entry.getCompoundOrEmpty("data").getCompoundOrEmpty("entity"),metadata);
        game.items().indirectKills().worldChanges(level,cause,true,()-> {
            var old=level.getBlockState(pos);
            if(new BlockInput(Blocks.SPAWNER.defaultBlockState(),Set.of(),tag).place(level,pos,258))
                level.updateNeighboursOnBlockSet(pos,old);
        });
        return true;
    }

    /** Adopt a source-configured native spawner without replacing its potentials or cadence. */
    public void admitEventSpawner(ServerLevel level,BlockPos pos,LbcApocalypseEvents.Authority authority){
        if(!(level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner)||authority==null)return;
        var tag=spawner.saveWithFullMetadata(level.registryAccess());
        // Native block serialization retains all authored SpawnData/SpawnPotentials values.

        var metadata=new CompoundTag();metadata.putString("epoch",authority.epoch());metadata.putBoolean("closed",false);
        metadata.putString("owner",authority.author().toString());metadata.putBoolean("event",true);
        metadata.putString("emitter",UUID.randomUUID().toString());metadata.putLong("order",authority.cause().order());
        metadata.putBoolean("friendlyFire",authority.cause().friendlyFire());metadata.putBoolean("projectile",authority.cause().projectile());
        tag.put(DATA,metadata);enrich(tag.getCompoundOrEmpty("SpawnData").getCompoundOrEmpty("entity"),metadata);
        for(var raw:tag.getListOrEmpty("SpawnPotentials"))if(raw instanceof CompoundTag entry)
            enrich(entry.getCompoundOrEmpty("data").getCompoundOrEmpty("entity"),metadata);
        spawner.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),tag));
        spawner.setChanged();loadedSpawner(spawner);
    }
    private static void enrich(CompoundTag entity,CompoundTag metadata) {
        var tags=entity.getListOrEmpty("Tags");tags.add(StringTag.valueOf(ACTOR));
        tags.add(StringTag.valueOf(metadata.getStringOr("epoch","")));
        var author=metadata.getStringOr("owner","");if(!author.isEmpty())tags.add(StringTag.valueOf(OWNER+author));
        if(metadata.getBooleanOr("event",false))tags.add(StringTag.valueOf(EVENT_CAUSE+metadata.getStringOr("emitter","")+"."
                +metadata.getLongOr("order",0)+"."+metadata.getBooleanOr("friendlyFire",true)+"."+metadata.getBooleanOr("projectile",false)));
        entity.put("Tags",tags);
    }
    private static CompoundTag metadata(SpawnerBlockEntity spawner) {
        return ((StateAccess)spawner).skywars2$potionMetadata();
    }
    public static boolean managed(SpawnerBlockEntity spawner) { return metadata(spawner).contains("epoch"); }
    public static boolean managed(Entity actor) { return actor instanceof ThrownSplashPotion && actor.entityTags().contains(ACTOR); }
    public boolean isClosed(SpawnerBlockEntity spawner) { return managed(spawner) && metadata(spawner).getBooleanOr("closed",false); }

    public void loadedSpawner(SpawnerBlockEntity spawner) {
        spawners.remove(spawner.getSpawner());
        if(!managed(spawner) || !current(spawner))return;
        if(!producerActive(spawner)) { close(spawner);return; }
        if(!isClosed(spawner))spawners.put(spawner.getSpawner(),spawner);
    }
    public void unloadedSpawner(SpawnerBlockEntity spawner) { spawners.remove(spawner.getSpawner()); }
    private static boolean current(SpawnerBlockEntity spawner) {
        if(spawner.isRemoved() || !(spawner.getLevel() instanceof ServerLevel level))return false;
        var pos=spawner.getBlockPos();var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
        return chunk!=null && chunk.getBlockEntities().get(pos)==spawner;
    }
    private boolean producerActive(SpawnerBlockEntity spawner) {
        return game.playing() && game.stage()!=GameController.Stage.ENDING && (spawner.getLevel()==game.maps().level()||metadata(spawner).getBooleanOr("event",false))
                && game.items().currentEpoch(metadata(spawner).getStringOr("epoch",""));
    }
    public boolean canTick(SpawnerBlockEntity spawner) {
        if(!managed(spawner))return true;
        if(!current(spawner))return false;
        if(!producerActive(spawner)) { close(spawner);return false; }
        if(isClosed(spawner))return false;
        spawners.put(spawner.getSpawner(),spawner);
        return !(spawner.getLevel() instanceof ServerLevel level) || !game.items().timeStop().stopped(level);
    }
    public boolean canSpawnAt(BaseSpawner producer,ServerLevel level,AABB bounds) {
        return !spawners.containsKey(producer) || resident(level,bounds.inflate(1));
    }
    private static boolean resident(ServerLevel level,AABB bounds) {
        int minX=net.minecraft.util.Mth.floor(bounds.minX)>>4,maxX=net.minecraft.util.Mth.floor(bounds.maxX)>>4;
        int minZ=net.minecraft.util.Mth.floor(bounds.minZ)>>4,maxZ=net.minecraft.util.Mth.floor(bounds.maxZ)>>4;
        for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++)if(level.getChunkSource().getChunkNow(x,z)==null)return false;
        return true;
    }
    private void close(SpawnerBlockEntity spawner) {
        metadata(spawner).putBoolean("closed",true);spawners.remove(spawner.getSpawner());
        if(spawner.getLevel() instanceof ServerLevel level) {
            var pos=spawner.getBlockPos();var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
            if(chunk!=null)chunk.markUnsaved();
        }
    }

    private ServerPlayer author(Entity actor) {
        for(var tag:actor.entityTags())if(tag.startsWith(OWNER))try {
            var id=UUID.fromString(tag.substring(OWNER.length()));var online=game.server().getPlayerList().getPlayer(id);
            return online==null?authors.get(id):online;
        } catch(IllegalArgumentException ignored) { }
        return null;
    }
    public LivingEntity owner(Entity actor) {
        if(actor instanceof ItemEntity token && LbcPotionSpawnerDropItem.matches(token.getItem()))return game.items().specialDrops().owner(token);
        if(!managed(actor))return null;
        var potion=(ThrownSplashPotion)actor;
        return potion.getOwner() instanceof LivingEntity nativeOwner?nativeOwner:author(actor);
    }
    public boolean wild(Entity source) {
        return managed(source) || source instanceof ItemEntity token && LbcPotionSpawnerDropItem.matches(token.getItem());
    }
    private boolean active(Entity actor) {
        return game.playing() && game.stage()!=GameController.Stage.ENDING && (actor.level()==game.maps().level()||game.items().spellSources().persistent(actor))
                && game.items().currentEpoch(actor);
    }
    public boolean canAffect(Entity source,Entity target) {
        return !managed(source) || target!=null && active(source) && (game.items().spellSources().wild(source)
                ?game.items().spellSources().canAffect(source,target):game.items().wands().validTarget(target));
    }
    public boolean canTarget(Entity source,LivingEntity target) { return target==null || canAffect(source,target); }
    public void loaded(Entity actor) {
        if(!managed(actor))return;
        for(var tag:actor.entityTags())if(tag.startsWith(EVENT_CAUSE))try{
            var parts=tag.substring(EVENT_CAUSE.length()).split("\\.");
            UUID author=null;for(var candidate:actor.entityTags())if(candidate.startsWith(OWNER))author=UUID.fromString(candidate.substring(OWNER.length()));
            String epoch=null;for(var candidate:actor.entityTags())if(candidate.startsWith("skywars2.items."))epoch=candidate;
            if(author!=null&&epoch!=null)game.items().spellSources().bind(actor,new LbcSpellSource.Source(UUID.fromString(parts[0]),author,epoch,
                    new LbcIndirectKills.Owner(author,Long.parseLong(parts[1]),Boolean.parseBoolean(parts[2]),Boolean.parseBoolean(parts[3])),Set.of(),true));
        }catch(IllegalArgumentException ignored){}
        if(!active(actor)) { actor.discard();return; }
        var potion=(ThrownSplashPotion)actor;
        // Source spawners omit Owner: keep native ownerless collision/flight, credit the separate author.
        potions.add(potion);game.items().timeStop().prepareFrozen(potion);
    }
    public void unloaded(Entity actor) { if(managed(actor))potions.remove(actor); }
    public void tick() {
        for(var potion:List.copyOf(potions))if(potion.isRemoved())potions.remove(potion);
            else if(!active(potion)) { potions.remove(potion);potion.discard(); }
        for(var spawner:List.copyOf(spawners.values())) {
            if(!current(spawner))unloadedSpawner(spawner);
            else if(!producerActive(spawner))close(spawner);
        }
    }
    public void stop() {
        var previous=List.copyOf(potions);potions.clear();
        for(var potion:previous)if(!potion.isRemoved())potion.discard();
        for(var spawner:List.copyOf(spawners.values()))close(spawner);
        spawners.clear();authors.clear();
    }
    public List<ThrownSplashPotion> snapshot() { return List.copyOf(potions); }
    public List<SpawnerBlockEntity> snapshotSpawners() { return List.copyOf(spawners.values()); }
    public int trackedCount() { return potions.size(); }
    public int spawnerCount() { return spawners.size(); }
}
