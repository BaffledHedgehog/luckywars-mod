package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.IdentityHashMap;
import java.util.List;

/** The slab's terrain fan has one advancing center and two short-lived branches per active tick. */
public final class LbcKaraSlabs {
    public static final Item OXIDIZED=register("kara_slab",Items.CUT_COPPER_SLAB.waxed().oxidized(),"god_slab_imba");
    public static final Item WEATHERED=register("kara_weathered_slab",Items.CUT_COPPER_SLAB.waxed().weathered(),"not_decaying_slab_imba");
    public static final Item EXPOSED=register("kara_exposed_slab",Items.CUT_COPPER_SLAB.waxed().exposed(),"not_decaying_slab_imba");
    static final String TAG="skywars2.kara_slab";
    private static final TagKey<DamageType> ARROW_ONLY=TagKey.create(Registries.DAMAGE_TYPE,Identifier.parse("minecraft:arrow_only"));
    private final GameController game;
    private final IdentityHashMap<Marker,Actor> actors=new IdentityHashMap<>();
    private static final class Actor {
        final Marker carrier;final ServerPlayer owner;final String epoch;final LbcIndirectKills.Owner cause;final float yaw;final boolean central;int age;
        Actor(Marker carrier,ServerPlayer owner,String epoch,LbcIndirectKills.Owner cause,float yaw,boolean central,int age){
            this.carrier=carrier;this.owner=owner;this.epoch=epoch;this.cause=cause;this.yaw=yaw;this.central=central;this.age=age;
        }
    }
    record View(Entity carrier,ServerPlayer owner,Vec3 position,float yaw,int age,boolean central){}
    LbcKaraSlabs(GameController game){this.game=game;}
    static void register(){}
    private static Item register(String name,Item proxy,String translation){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:"+name));
        var model=BuiltInRegistries.ITEM.getKey(proxy);
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(new Item.Properties(){
            @Override public Identifier effectiveModel(){return model;}
        }.setId(key).overrideDescription(translation),proxy,true){
            // These remain placeable copper slabs; placement intentionally loses the old item-only flags.
            @Override public InteractionResult useOn(UseOnContext context){return proxy.useOn(context);}
        });
    }
    public static boolean isSlab(ItemStack stack){return !stack.isEmpty()&&(stack.is(OXIDIZED)||stack.is(WEATHERED)||stack.is(EXPOSED)
            ||stack.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getIntOr("kara",0)==1);}
    private boolean valid(ServerPlayer owner){return owner!=null&&owner.isAlive()&&!owner.isRemoved()
            &&owner.level()==game.maps().level()&&game.canUseGameplayItems(owner)&&game.stage()!=GameController.Stage.ENDING;}
    private boolean paused(ServerPlayer owner){return game.items().timeStop().stopped(owner.level())
            ||owner.entityTags().contains("time_stopped")||game.items().timeStop().frozen(owner);}
    public void damaged(LivingEntity victim,DamageSource source){
        if(!(source.getEntity() instanceof ServerPlayer owner)||!LbcDamage.isMeleeAttack(source,owner)||!isSlab(owner.getMainHandItem())||!valid(owner)
                ||source.is(ARROW_ONLY)||!game.items().wands().validRoleTarget(victim,true)
                ||paused(owner)||game.items().actionsBlocked(owner)||game.items().magic().suppressed(owner))return;
        var dimension=owner.level().dimension().identifier();
        if(dimension.equals(Identifier.parse("minecraft:nexus"))||dimension.equals(Identifier.parse("minecraft:imprinted")))return;
        spawn(owner);
    }
    void spawn(ServerPlayer owner){
        if(!valid(owner)||paused(owner)||game.items().magic().suppressed(owner))return;
        float yaw=owner.getYRot();Vec3 at=owner.getEyePosition().add(Vec3.directionFromRotation(0,yaw).scale(1.5));
        add(owner,at,yaw,0,true,game.items().epoch(),game.items().indirectKills().coldCause(owner,false));
    }
    private void add(ServerPlayer owner,Vec3 at,float yaw,int age,boolean central,String epoch,LbcIndirectKills.Owner cause){
        if(!resident(owner,at)||game.items().magic().suppressedAt(owner.level(),at))return;
        var marker=new Marker(EntityTypes.MARKER,owner.level());marker.setPos(at);marker.setYRot(yaw);marker.setXRot(0);
        marker.setNoGravity(true);marker.entityTags().add(TAG);marker.entityTags().add(epoch);
        marker.entityTags().addAll(List.of("magic","slowraycast","imba_rc","imba_kara"));
        if(central)marker.entityTags().add("central");
        var actor=new Actor(marker,owner,epoch,cause,yaw,central,age);actors.put(marker,actor);
        if(!owner.level().addFreshEntity(marker))remove(actor);
    }
    private static boolean resident(ServerPlayer owner,Vec3 at){
        int x=BlockPos.containing(at).getX(),z=BlockPos.containing(at).getZ();
        // Native destroy/neighbor updates require the immediate halo; no terrain is loaded by the fan.
        for(int cx=(x-1)>>4;cx<=(x+1)>>4;cx++)for(int cz=(z-1)>>4;cz<=(z+1)>>4;cz++)
            if(owner.level().getChunkSource().getChunkNow(cx,cz)==null)return false;
        return true;
    }
    public void tick(){
        for(var actor:List.copyOf(actors.values())){
            var marker=actor.carrier;var owner=actor.owner;
            if(marker.isRemoved()||!valid(owner)||marker.level()!=owner.level()||!game.items().currentEpoch(actor.epoch)
                    ||!resident(owner,marker.position())||game.items().magic().suppressedAt(owner.level(),marker.position())){remove(actor);continue;}
            if(paused(owner)||game.items().timeStop().frozen(marker))continue;
            int age=++actor.age;
            Vec3 at=marker.position();column(actor,BlockPos.containing(at));
            owner.level().sendParticles(ParticleTypes.END_ROD,true,false,at.x,at.y,at.z,100,0,50,0,0);
            LbcOverload.sound(owner,at,"minecraft:entity.generic.explode",.1f,1,50,.1f);
            if(actor.central){
                Vec3 left=Vec3.directionFromRotation(0,actor.yaw-90).scale(.8);
                add(owner,at.add(left),actor.yaw+30,age,false,actor.epoch,actor.cause);
                add(owner,at.subtract(left),actor.yaw-30,age,false,actor.epoch,actor.cause);
            }
            marker.setPos(at.add(Vec3.directionFromRotation(0,actor.yaw).scale(.7)));
            if(age>=100)remove(actor);
        }
    }
    private void column(Actor actor,BlockPos origin){
        var level=actor.owner.level();var chunk=level.getChunkSource().getChunkNow(origin.getX()>>4,origin.getZ()>>4);
        int bottom=Math.max(0,level.getMinY()),top=Math.min(319,level.getMaxY());
        if(chunk==null||bottom>top)return;
        game.items().indirectKills().capturedWorldChanges(level,actor.cause,()->{
            var pos=new BlockPos.MutableBlockPos(origin.getX(),bottom,origin.getZ());
            for(int section=bottom>>4;section<=top>>4;section++){
                if(chunk.getSection(level.getSectionIndexFromSectionY(section)).hasOnlyAir())continue;
                for(int y=Math.max(bottom,section<<4);y<=Math.min(top,(section<<4)+15);y++){
                    pos.setY(y);if(level.getBlockState(pos).isAir())continue;
                    // Native destroy supplies block/container drops. Fill AIR also removes residual water.
                    level.destroyBlock(pos,true,actor.owner);
                    if(!level.getBlockState(pos).isAir())level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
                }
            }
        });
    }
    int count(){return actors.size();}
    List<View> snapshot(){return actors.values().stream().map(a->new View(a.carrier,a.owner,a.carrier.position(),a.yaw,a.age,a.central)).toList();}
    public void loaded(Entity entity){if(entity.entityTags().contains(TAG)&&!actors.containsKey(entity))entity.discard();}
    public void unloaded(Entity entity){var actor=actors.get(entity);if(actor!=null)remove(actor);}
    public void forget(ServerPlayer owner){for(var actor:List.copyOf(actors.values()))if(actor.owner.getUUID().equals(owner.getUUID()))remove(actor);}
    public void stop(){for(var actor:List.copyOf(actors.values()))remove(actor);}
    private void remove(Actor actor){actors.remove(actor.carrier);actor.carrier.discard();}
}
