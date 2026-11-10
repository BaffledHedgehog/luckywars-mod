package me.skywars2.game;

import java.util.*;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.minecraft.world.phys.*;

/** Р’РµС‡РЅРѕРµ Р±РѕРіР°С‚СЃС‚РІРѕ: СЂРµР°Р»СЊРЅС‹Рµ Р±Р»РѕРєРё, РЅР°С‚РёРІРЅС‹Рµ С‚РµР»Р° РѕС‡Р°РіРѕРІ Рё РѕРґРёРЅ РѕР±С‰РёР№ РІР»Р°РґРµР»РµС† СѓСЂРѕРЅР°. */
public final class LbcEternalWealth {
    static final String TAG="skywars2.eternal_wealth";
    private static final TagKey<Block> SOLID=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:solid"));
    private static final TagKey<Block> FLUIDS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:fluids"));
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final DustColorTransitionOptions CURSE_DUST=new DustColorTransitionOptions(0xffff00,0xffbf00,.2f);
    private static final DustColorTransitionOptions FILL_DUST=new DustColorTransitionOptions(0xffff00,0xffbf00,3f);
    private static final Vec3[] CONTACTS={new Vec3(.3,-.5,0),new Vec3(-.3,-.5,0),new Vec3(0,-.5,.3),new Vec3(0,-.5,-.3),
            new Vec3(.3,1,0),new Vec3(-.3,1,0),new Vec3(0,1,.3),new Vec3(0,1,-.3)};
    private enum Kind { PENDING,SEED,FIELD,DIAMOND }
    private static final class Actor {
        final Marker body;final LbcIndirectKills.Owner cause;
        Kind kind;int age,born=Integer.MIN_VALUE;
        Actor(Marker body,LbcIndirectKills.Owner cause,Kind kind,int age){this.body=body;this.cause=cause;this.kind=kind;this.age=age;}
    }
    private record Section(ServerLevel level,int x,int y,int z) {}
    private record Image(UUID id,LbcIndirectKills.Owner cause,Kind kind,int age) {}
    private final GameController game;
    private final Map<Marker,Actor> actors=new LinkedHashMap<>();
    private final Set<UUID> dirty=new HashSet<>(),carriers=new HashSet<>();
    private boolean restoring;
    private int lastTick=Integer.MIN_VALUE,playerPhase;
    LbcEternalWealth(GameController game){this.game=game;}
    public void inventoryChanged(ServerPlayer player){dirty.add(player.getUUID());}
    public static boolean managed(Entity entity){return entity.entityTags().contains(TAG);}
    public int count(){return actors.size();}
    public int fields(){return (int)actors.values().stream().filter(a->a.kind==Kind.FIELD).count();}
    public int diamonds(){return (int)actors.values().stream().filter(a->a.kind==Kind.DIAMOND).count();}
    List<Marker> snapshot(){return List.copyOf(actors.keySet());}
    boolean captureReady(){return actors.values().stream().noneMatch(a->a.kind==Kind.PENDING);}
    private boolean suppressed(ServerLevel level,Vec3 at){return game.items().magic().suppressedAt(level,at);}
    private boolean admitted(ServerPlayer p){return p!=null&&p.isAlive()&&!p.isRemoved()&&!game.isObserver(p)
            &&game.canUseGameplayItems(p)&&LbcDimensionItems.ordinary(p)&&game.items().wands().validRoleTarget(p,false)
            &&!game.items().wands().inputTimeStopped(p)&&!game.items().timeStop().stopped(p.level());}
    private boolean resident(ServerLevel level,BlockPos at,int radius,boolean prepare){
        boolean ready=true;
        for(int x=(at.getX()-radius)>>4;x<=(at.getX()+radius)>>4;x++)for(int z=(at.getZ()-radius)>>4;z<=(at.getZ()+radius)>>4;z++){
            if(level.getChunkSource().getChunkNow(x,z)!=null)continue;
            if(prepare)game.maps().prepareChunk(level,new ChunkPos(x,z));ready=false;
        }
        return ready;
    }
    /** РћР±С‰РёР№ РґРёСЃРїРµС‚С‡РµСЂ РґСЂРѕРїРѕРІ СЂР°СЃС…РѕРґСѓРµС‚ СЂРѕРІРЅРѕ РѕРґРёРЅ С‚РѕРєРµРЅ; РЅР°РіСЂР°РґР° РїРѕСЏРІР»СЏРµС‚СЃСЏ СЃСЂР°Р·Сѓ. */
    boolean ready(ItemEntity token){return token.level() instanceof ServerLevel level&&resident(level,token.blockPosition(),1,false);}
    boolean spawn(ItemEntity token){
        if(!(token.level() instanceof ServerLevel level)||!resident(level,token.blockPosition(),1,false))return false;
        var at=token.blockPosition();LbcDropBlocks.setBlock(level,at,Blocks.GOLD_BLOCK.defaultBlockState(),false);
        var frame=new ItemFrame(EntityTypes.GLOW_ITEM_FRAME,level,at.above(),net.minecraft.core.Direction.UP);
        frame.setInvisible(true);frame.setGlowingTag(true);frame.setItem(new ItemStack(LbcEternalWealthItems.STONE));
        game.items().markCurrentItemEpoch(frame);level.addFreshEntity(frame);
        level.sendParticles(new DustParticleOptions(0xffff00,2),at.getX(),at.getY()+1,at.getZ(),20,1,1,1,0);
        return true;
    }
    private Actor add(ServerLevel level,Vec3 at,Kind kind,LbcIndirectKills.Owner cause){
        var marker=new Marker(EntityTypes.MARKER,level);marker.setPos(at);
        marker.entityTags().add(TAG);
        if(kind==Kind.SEED||kind==Kind.FIELD)marker.entityTags().add("magic");
        if(kind==Kind.DIAMOND)marker.entityTags().add("avarice_diamond");
        game.items().markCurrentItemEpoch(marker);
        var actor=new Actor(marker,cause,kind,0);actor.born=game.server().getTickCount();actors.put(marker,actor);save(actor);
        game.items().timeStop().prepareFrozen(marker);
        if(!level.addFreshEntity(marker)){actors.remove(marker);return null;}
        return actor;
    }
    private void save(Actor a){
        setValue(a.body,"kind",a.kind.name());setValue(a.body,"age",Integer.toString(a.age));
        if(a.cause!=null){setValue(a.body,"owner",a.cause.player().toString());setValue(a.body,"order",Long.toString(a.cause.order()));}
    }
    private static void setValue(Marker marker,String key,String text){String prefix=TAG+"."+key+".";marker.entityTags().removeIf(t->t.startsWith(prefix));marker.entityTags().add(prefix+text);}
    private static String value(Marker marker,String key,String fallback){String prefix=TAG+"."+key+".";for(var tag:marker.entityTags())if(tag.startsWith(prefix))return tag.substring(prefix.length());return fallback;}
    void loaded(Entity entity){
        if(restoring||!(entity instanceof Marker marker)||!managed(marker)||actors.containsKey(marker))return;
        try{
            var kind=Kind.valueOf(value(marker,"kind",""));
            var owner=value(marker,"owner","");
            var cause=owner.isEmpty()?null:new LbcIndirectKills.Owner(UUID.fromString(owner),Long.parseLong(value(marker,"order","0")),true,false);
            var actor=new Actor(marker,cause,kind,Integer.parseInt(value(marker,"age","0")));
            actors.put(marker,actor);
        }catch(IllegalArgumentException invalid){marker.discard();}
    }
    void unloaded(Entity entity){if(entity instanceof Marker marker)actors.remove(marker);}
    private void remove(Actor actor){actors.remove(actor.body);actor.body.discard();}
    private void intake(ServerPlayer player){
        if(!admitted(player)||suppressed(player.level(),player.position())||game.items().magic().rejectCustomEffect(player))return;
        if(game.items().inventoryHazards().protectedInventory(player)&&!LbcEternalWealthItems.isStone(player.getMainHandItem()))return;
        int removed=player.getInventory().clearOrCountMatchingItems(LbcEternalWealthItems::isStone,false,1,player.inventoryMenu.getCraftSlots());
        if(removed==0){carriers.remove(player.getUUID());return;}
        var mana=game.items().mana();var score=mana.score(player,"greed_prestige");score.set(score.get()+1);
        if(!game.items().magic().rejectCustomDebuff(player))player.entityTags().add("greed_cursed");
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(" ")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("greed_curse_activate").withColor(0xffaa00)));
        player.level().playSound(null,player.blockPosition(),SoundEvents.ENCHANTMENT_TABLE_USE,SoundSource.MASTER,1,0);
        var random=player.getRandom();
        add(player.level(),new Vec3(random.nextInt(261)-130,random.nextInt(285)+16,random.nextInt(261)-130),Kind.PENDING,game.items().indirectKills().wildCause(player));
        mana.inventoryChanged(player);player.getInventory().setChanged();
    }
    public void tick(){
        if(restoring)return;int frame=game.server().getTickCount();if(frame==lastTick)return;lastTick=frame;
        if(!game.playing()||game.stage()==GameController.Stage.ENDING)return;
        for(var id:Set.copyOf(dirty)){
            dirty.remove(id);var p=game.server().getPlayerList().getPlayer(id);boolean found=false;
            if(p!=null)for(int i=0;i<p.getInventory().getContainerSize();i++)if(LbcEternalWealthItems.isStone(p.getInventory().getItem(i))){found=true;break;}
            if(found)carriers.add(id);else carriers.remove(id);
        }
        for(var id:Set.copyOf(carriers))intake(game.server().getPlayerList().getPlayer(id));
        var damage=new HashMap<Section,List<Actor>>();
        for(var actor:List.copyOf(actors.values())){
            if(actor.body.isRemoved()){unloaded(actor.body);continue;}
            var level=(ServerLevel)actor.body.level();
            if(!game.items().currentEpoch(actor.body)){remove(actor);continue;}
            if(game.items().timeStop().stopped(level)||actor.body.entityTags().contains("time_stopped"))continue;
            if((actor.kind==Kind.SEED||actor.kind==Kind.FIELD)&&suppressed(level,actor.body.position())){remove(actor);continue;}
            if(actor.born==frame)continue;
            actor.age++;
            if(actor.kind==Kind.PENDING){
                setValue(actor.body,"age",Integer.toString(actor.age));
                if(actor.age>=20&&resident(level,actor.body.blockPosition(),8,true))deploy(actor);
            }else if(actor.kind==Kind.SEED){
                if(actor.age%20==0)setValue(actor.body,"age",Integer.toString(actor.age));
                if(actor.age>=180&&resident(level,actor.body.blockPosition(),4,false))fill(actor);
            }else if(actor.kind==Kind.DIAMOND){touch(actor);}
            else if(actor.age%5==0){var at=actor.body.blockPosition();var section=new Section(level,at.getX()>>4,at.getY()>>4,at.getZ()>>4);
                damage.computeIfAbsent(section,unused->new ArrayList<>()).add(actor);}
        }
        damage(damage);
        if(!game.items().wands().timeStopped()&&++playerPhase%20==0)for(var p:game.server().getPlayerList().getPlayers()){
            if(!admitted(p)||!p.entityTags().contains("greed_cursed"))continue;
            if(LbcParasite.active(p)){p.entityTags().remove("greed_cursed");continue;}
            if(suppressed(p.level(),p.position()))continue;
            boolean protectedByDiamond=actors.values().stream().anyMatch(a->a.kind==Kind.DIAMOND&&a.body.level()==p.level()&&a.body.position().distanceToSqr(p.position())<=400);
            if(!protectedByDiamond)add(p.level(),Vec3.atCenterOf(p.blockPosition()),Kind.SEED,game.items().indirectKills().wildCause(p));
            p.level().sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,Blocks.GOLD_BLOCK.defaultBlockState()),p.getX(),p.getY(),p.getZ(),5,8,8,8,0);
        }
    }
    private void deploy(Actor pending){
        var level=(ServerLevel)pending.body.level();var origin=pending.body.blockPosition().offset(-7,-7,-7);
        var template=game.server().getStructureTemplateManager().get(Identifier.parse("lbc:avarice_diamond")).orElseThrow();
        game.items().indirectKills().capturedWorldChanges(level,pending.cause,()->template.placeInWorld(level,origin,origin,
                new StructurePlaceSettings().setIgnoreEntities(true).setKnownShape(true),level.getRandom(),818));
        var diamond=add(level,Vec3.atLowerCornerOf(origin).add(6.493980711860754,8.5,6.476747271611746),Kind.DIAMOND,pending.cause);
        if(diamond!=null)touch(diamond);
        remove(pending);
    }
    /** РћР±СЉРµРґРёРЅРµРЅРёРµ С‡РµС‚С‹СЂС‘С… РёСЃС…РѕРґРЅС‹С… fill: РєСѓР± 7Ві Рё РїРѕ РґРѕРїРѕР»РЅРёС‚РµР»СЊРЅРѕРјСѓ СЃР»РѕСЋ РІРґРѕР»СЊ РєР°Р¶РґРѕР№ РѕСЃРё. */
    static boolean inMask(int x,int y,int z){return Math.abs(x)<=4&&Math.abs(y)<=4&&Math.abs(z)<=4
            &&(Math.abs(x)<=3?0:1)+(Math.abs(y)<=3?0:1)+(Math.abs(z)<=3?0:1)<=1;}
    private void fill(Actor actor){
        var level=(ServerLevel)actor.body.level();var center=actor.body.blockPosition();
        game.items().indirectKills().capturedWorldChanges(level,actor.cause,()->{
            for(int x=-4;x<=4;x++)for(int y=-4;y<=4;y++)for(int z=-4;z<=4;z++)if(inMask(x,y,z)){
                var pos=center.offset(x,y,z);if(level.isOutsideBuildHeight(pos))continue;var old=level.getBlockState(pos);
                if(old.is(SOLID)){
                    LbcDropBlocks.setBlock(level,pos,LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.NORMAL).defaultBlockState(),true);
                }else if(old.is(FLUIDS))LbcDropBlocks.setBlock(level,pos,Blocks.GOLD_BLOCK.defaultBlockState(),true);
            }
        });
        actor.kind=Kind.FIELD;actor.age=0;save(actor);
        level.sendParticles(FILL_DUST,actor.body.getX(),actor.body.getY(),actor.body.getZ(),5,3,3,3,0);
    }
    private void damage(Map<Section,List<Actor>> groups){
        for(var entry:groups.entrySet()){
            var key=entry.getKey();var level=key.level();var bounds=new AABB(key.x()*16,key.y()*16,key.z()*16,key.x()*16+16,key.y()*16+16,key.z()*16+16).inflate(5.5);
            for(var field:entry.getValue())level.sendParticles(CURSE_DUST,field.body.getX(),field.body.getY(),field.body.getZ(),10,2,2,2,0);
            for(var target:level.getEntitiesOfClass(LivingEntity.class,bounds,e->e.isAlive()&&e.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().validRoleTarget(e,false))){
                if(suppressed(level,target.position())||game.items().mana().hudValue(target,"void_resistance")>0||target.entityTags().contains("sin_greed_active"))continue;
                int contacts=0;
                for(var delta:CONTACTS)
                    if(level.getBlockState(BlockPos.containing(target.position().add(delta))).is(LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.NORMAL)))contacts++;
                if(contacts==0)continue;
                for(var field:entry.getValue())if(!field.body.isRemoved()&&field.body.position().distanceToSqr(target.position())<=30.25){
                    var source=game.items().indirectKills().captureDamage(level.damageSources().fellOutOfWorld(),field.cause);
                    for(int n=0;n<contacts&&target.isAlive();n++)target.hurtServer(level,source,1.5f);
                }
            }
        }
    }
    private void touch(Actor diamond){
        var level=(ServerLevel)diamond.body.level();var at=diamond.body.position();
        var near=level.getEntitiesOfClass(ServerPlayer.class,new AABB(at,at).inflate(2),p->!game.isObserver(p)&&p.isAlive()&&p.position().distanceToSqr(at)<=4);
        if(near.isEmpty()||!resident(level,diamond.body.blockPosition(),10,true))return;
        var cursed=near.stream().filter(p->p.entityTags().contains("greed_cursed")).toList();
        var poor=near.stream().filter(p->!p.entityTags().contains("greed_cursed")).toList();
        if(!poor.isEmpty()){
            sound(level,at,SoundEvents.ENDER_DRAGON_GROWL,.15f,2,.15f);
            recolor(level,diamond.body.blockPosition(),Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.BLACK));
            var origin=diamond.body.blockPosition().offset(-6,-6,-6);
            game.server().getStructureTemplateManager().get(Identifier.parse("lbc:air11x11")).orElseThrow().placeInWorld(level,origin,origin,
                    new StructurePlaceSettings().setIgnoreEntities(true).setKnownShape(true).addProcessor(new BlockRotProcessor(.2f)),level.getRandom(),818);
            for(var p:poor)reward(level,at,p,"lbc:ported/feather_greed");
        }
        if(!cursed.isEmpty()){
            for(float pitch:new float[]{.5f,1,0})sound(level,at,SoundEvents.ENCHANTMENT_TABLE_USE,.5f,pitch,.3f);
            recolor(level,diamond.body.blockPosition(),Blocks.DIAMOND_BLOCK);
            for(var p:level.getEntitiesOfClass(ServerPlayer.class,new AABB(at,at).inflate(10),p->p.position().distanceToSqr(at)<=100&&p.entityTags().contains("greed_cursed"))){
                p.entityTags().remove("greed_cursed");reward(level,at,p,"lbc:ported/divider");
            }
            var owners=cursed.stream().map(ServerPlayer::getUUID).collect(java.util.stream.Collectors.toSet());
            cleanse(level,owners);
        }
        for(var p:level.getEntitiesOfClass(ServerPlayer.class,new AABB(at,at).inflate(16),p->!game.isObserver(p)&&p.position().distanceToSqr(at)<=256))
            game.items().mana().award(p,"lbc:true_advancements/legends/avarice_diamond");
        level.playSound(null,diamond.body.blockPosition(),SoundEvents.EVOKER_CAST_SPELL,SoundSource.MASTER,3,1);
        level.sendParticles(new DustColorTransitionOptions(0x03_1aff,0xff_1a03,4),at.x,at.y,at.z,500,5,5,5,0);
        remove(diamond);
    }
    private void sound(ServerLevel level,Vec3 at,net.minecraft.sounds.SoundEvent event,float volume,float pitch,float minimum){
        for(var p:level.getPlayers(p->p.position().distanceToSqr(at)<=900)){
            var origin=p.position().distanceToSqr(at)>Math.pow(Math.max(1,volume)*16,2)?p.position().add(at.subtract(p.position()).normalize().scale(2)):at;
            float gain=origin==at?volume:minimum;
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(net.minecraft.core.Holder.direct(event),SoundSource.MASTER,origin.x,origin.y,origin.z,gain,pitch,level.getRandom().nextLong()));
        }
    }
    private void reward(ServerLevel level,Vec3 at,ServerPlayer player,String table){
        for(var item:game.lbc().rollTable(level,at,player,table)){
            var drop=new ItemEntity(level,at.x,at.y,at.z,item);drop.setDefaultPickUpDelay();game.items().markCurrentItemEpoch(drop);level.addFreshEntity(drop);
        }
    }
    private void recolor(ServerLevel level,BlockPos at,Block replacement){
        LbcDropBlocks.fill(level,at.offset(-10,-10,-10),at.offset(10,10,10),replacement,true,state->state.is(Blocks.GLAZED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.LIGHT_BLUE)));
    }
    private void cleanse(ServerLevel level,Set<UUID> owners){
        var removed=new HashSet<Actor>();var positions=new HashSet<Long>();var protectedCells=new HashSet<Long>();
        for(var field:actors.values())if(field.kind==Kind.FIELD&&field.body.level()==level&&field.cause!=null&&owners.contains(field.cause.player())){
            removed.add(field);maskCells(field.body.blockPosition(),positions);
        }
        for(var field:actors.values())if(field.kind==Kind.FIELD&&field.body.level()==level&&!removed.contains(field))maskCells(field.body.blockPosition(),protectedCells);
        positions.removeAll(protectedCells);
        for(long value:positions){var pos=BlockPos.of(value);
            if(level.hasChunkAt(pos)&&level.getBlockState(pos).is(LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.NORMAL)))
                LbcDropBlocks.setBlock(level,pos,Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.YELLOW).defaultBlockState(),true);
        }
        removed.forEach(this::remove);
    }
    private static void maskCells(BlockPos center,Set<Long> output){for(int x=-4;x<=4;x++)for(int y=-4;y<=4;y++)for(int z=-4;z<=4;z++)if(inMask(x,y,z))output.add(center.offset(x,y,z).asLong());}
    void stop(){for(var actor:List.copyOf(actors.values()))actor.body.discard();actors.clear();dirty.clear();carriers.clear();restoring=false;playerPhase=0;}
    Checkpoint checkpoint(){return new Checkpoint();}
    void prepareFrozen(){restoring=true;actors.clear();dirty.clear();carriers.clear();}
    void publishFrozen(){restoring=false;lastTick=game.server().getTickCount();for(var p:game.server().getPlayerList().getPlayers())inventoryChanged(p);}
    final class Checkpoint {
        private final List<Image> images=actors.values().stream().map(a->new Image(a.body.getUUID(),a.cause,a.kind,a.age)).toList();
        private final int phase=playerPhase;
        MatchFieldCheckpoint.Plan prepare(Function<UUID,Entity> lookup){
            var pending=new LinkedHashSet<UUID>();var rebuilt=new LinkedHashMap<Marker,Actor>();
            for(var image:images){var entity=lookup.apply(image.id());if(!(entity instanceof Marker marker)){pending.add(image.id());continue;}
                var actor=new Actor(marker,image.cause(),image.kind(),image.age());rebuilt.put(marker,actor);}
            return new MatchFieldCheckpoint.Plan(Set.copyOf(pending),()->{actors.clear();actors.putAll(rebuilt);playerPhase=phase;});
        }
    }
}
