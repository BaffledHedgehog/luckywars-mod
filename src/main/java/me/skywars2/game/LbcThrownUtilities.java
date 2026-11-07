package me.skywars2.game;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Source utilities using vanilla transport and the shared Psycho, inventory and pipe owners. */
public final class LbcThrownUtilities {
    public interface OwnerGate { boolean skywars2$leftOwner(); }
    public enum Kind { RUCHKA, HONEW, THROWABLE_METAL_PIPE, BUCKET, PUMPKIN }
    /** Author decisions can change suppression without changing native launch/debit ownership. */
    public interface Policy {
        default boolean allowsLaunch(Kind kind,ServerPlayer owner){return true;}
        default boolean allowsFlight(Kind kind,ServerLevel level,Vec3 at){return true;}
        default boolean allowsFlight(Kind kind,ServerLevel level,Vec3 from,Vec3 to){
            return allowsFlight(kind,level,from)&&allowsFlight(kind,level,to);
        }
        default boolean allowsPayload(Kind kind,ServerPlayer owner,ServerPlayer target){return true;}
    }
    public record Flight(Kind kind,UUID author,String epoch,boolean claimed){}
    private static final String TAG="skywars2_thrown_utility";
    private static final AttachmentType<CompoundTag> DATA=AttachmentRegistry.create(
            Identifier.parse("skywars2:thrown_utility"),builder->builder.persistent(CompoundTag.CODEC));
    private final GameController game;
    private final Map<Snowball,Flight> flights=new IdentityHashMap<>();
    private final Map<Snowball,Vec3> previousPositions=new IdentityHashMap<>();
    private Policy policy=new Policy(){};
    public LbcThrownUtilities(GameController game){this.game=game;}
    public static void register(){LbcThrownUtilityItems.register();}
    public void policy(Policy policy){this.policy=java.util.Objects.requireNonNull(policy);}
    public static Kind kind(ItemStack stack){
        if(stack.isEmpty())return null;
        if(ItemData.intValue(stack,"ruchka",0)==1)return Kind.RUCHKA;
        if(ItemData.intValue(stack,"honew",0)==1)return Kind.HONEW;
        if(ItemData.intValue(stack,"throwable_metal_pipe",0)==1)return Kind.THROWABLE_METAL_PIPE;
        if(ItemData.intValue(stack,"bucket",0)==1)return Kind.BUCKET;
        if(ItemData.intValue(stack,"tikva",0)==1)return Kind.PUMPKIN;
        return null;
    }
    public static boolean matches(ItemStack stack){return kind(stack)!=null;}
    public static boolean matches(ItemStack stack,Kind kind){return kind(stack)==kind;}
    public static boolean isManaged(Entity entity){return entity instanceof Snowball&&entity.entityTags().contains(TAG);}
    private boolean active(ServerLevel level){return game.playing()&&game.stage()!=GameController.Stage.ENDING
            &&!game.maps().busy()&&level==game.maps().level()&&LbcDimensionItems.ordinary(level)
            &&LbcBedwarsRoleGuards.publication(level);}
    private boolean admitted(ServerPlayer owner){return active(owner.level())&&owner.isAlive()&&!owner.isRemoved()
            &&!owner.isSpectator()&&!game.isObserver(owner)&&!game.awaitingRespawn(owner.getUUID())
            &&game.canUseGameplayItems(owner)&&game.items().wands().validRoleTarget(owner,false)
            &&!game.items().actionsBlocked(owner)&&!game.items().wands().inputTimeStopped(owner)
            &&!game.items().timeStop().stopped(owner.level())&&!game.server().tickRateManager().isFrozen()
            &&game.server().tickRateManager().runsNormally();}
    private boolean tech(ServerLevel level,Vec3 at){return game.items().magic().mechanismSuppressedAt(level,at,20);}
    public boolean use(ServerPlayer owner,InteractionHand hand){
        var stack=owner.getItemInHand(hand);var kind=kind(stack);
        if(kind==null||!admitted(owner)||kind==Kind.RUCHKA&&tech(owner.level(),owner.position())
                ||!policy.allowsLaunch(kind,owner))return false;
        // Vanilla aims the exact native projectile; attach before publication so entity-load callbacks see authority.
        var shot=new Snowball(owner.level(),owner,stack);shot.shootFromRotation(owner,owner.getXRot(),owner.getYRot(),0,1.5f,1);
        if(kind==Kind.THROWABLE_METAL_PIPE){
            if(!pipe(owner,shot.position(),shot.getDeltaMovement()))return false;
        }else{
            var state=new Flight(kind,owner.getUUID(),game.items().epoch(),false);
            shot.entityTags().add(TAG);write(shot,state);flights.put(shot,state);previousPositions.put(shot,shot.position());game.items().markCurrentItemEpoch(shot);
            if(!owner.level().addFreshEntity(shot)){flights.remove(shot);previousPositions.remove(shot);shot.discard();return false;}
        }
        LbcOverload.sound(owner,owner.position(),"minecraft:entity.snowball.throw",.5f,.4f/(owner.getRandom().nextFloat()*.4f+.8f),0);
        owner.awardStat(Stats.ITEM_USED.get(stack.getItem()));stack.consume(1,owner);return true;
    }
    private boolean pipe(ServerPlayer owner,Vec3 at,Vec3 velocity){
        var output=game.lbc().rollTable(owner.level(),at,owner,"lbc:metal_pipe");
        // This table is the ordinary, recoverable pipe consumed by the existing landing engine.
        if(output.size()!=1||!LbcMetalPipe.matches(output.getFirst()))return false;
        var stack=output.getFirst();LbcInventoryHazards.recordProducer(stack,owner.getUUID(),game.items().epoch());
        var item=new ItemEntity(owner.level(),at.x,at.y,at.z,stack);item.setDeltaMovement(velocity);item.setThrower(owner);
        item.entityTags().add(LbcMetalPipe.THROWN);item.setPickUpDelay(10);game.items().markCurrentItemEpoch(item);
        if(!owner.level().addFreshEntity(item))return false;
        game.items().metalPipe().loaded(item);return true;
    }
    /** Call only after accepted native melee damage; never infer a hit from nearest HurtTime. */
    public void damaged(LivingEntity victim,DamageSource source){
        if(!(victim instanceof ServerPlayer target)||!(source.getEntity() instanceof ServerPlayer owner)
                ||!LbcDamage.isMeleeAttack(source,owner)||!matches(owner.getMainHandItem(),Kind.RUCHKA)
                ||!admitted(owner)||!target(owner,target)||tech(owner.level(),owner.position())
                ||!policy.allowsPayload(Kind.RUCHKA,owner,target))return;
        owner.getMainHandItem().shrink(1);ruchka(owner,target);
    }
    private boolean target(ServerPlayer owner,ServerPlayer target){return target.isAlive()&&!target.isRemoved()
            &&owner.level()==target.level()&&!game.items().timeStop().frozen(target)
            &&game.items().wands().validRoleTarget(target,false);}
    /** Dispatch before native Snowball.tick, after TimeStop's frozen-actor gate. No world scan or tick NBT decode. */
    public void beforeTick(Entity entity){
        if(!(entity instanceof Snowball shot))return;var state=flights.get(shot);if(state==null)return;
        if(shot.isRemoved()||state.claimed()){unloaded(shot);return;}
        if(!active((ServerLevel)shot.level())||!game.items().currentEpoch(state.epoch())){retire(shot);return;}
        if(game.items().timeStop().frozen(shot)||game.items().timeStop().stopped((ServerLevel)shot.level())
                ||game.server().tickRateManager().isFrozen())return;
        var from=previousPositions.put(shot,shot.position());
        if(!policy.allowsFlight(state.kind(),(ServerLevel)shot.level(),from==null?shot.position():from,shot.position())){retire(shot);return;}
        // Source TECH20 suppresses this proximity check; the ordinary snowball still owns its flight/collision.
        if(state.kind()==Kind.RUCHKA&&tech((ServerLevel)shot.level(),shot.position()))return;
        var owner=game.server().getPlayerList().getPlayer(state.author());if(owner==null||owner.level()!=shot.level())return;
        var center=shot.position().add(0,1,0);ServerPlayer nearest=null;double distance=4.5*4.5;
        for(var candidate:((ServerLevel)shot.level()).getEntitiesOfClass(ServerPlayer.class,
                new AABB(center,center).inflate(4.5),p->target(owner,p)&&(p!=owner||((OwnerGate)shot).skywars2$leftOwner()
                        &&(p.getBoundingBox().inflate(.3).contains(shot.position())
                        ||p.getBoundingBox().inflate(.3).clip(shot.position(),shot.position().add(shot.getDeltaMovement())).isPresent())))){
            double next=candidate.position().distanceToSqr(center);
            if(next<=distance){nearest=candidate;distance=next;}
        }
        if(nearest==null||!policy.allowsPayload(state.kind(),owner,nearest))return;
        // Persist the claim before any effect/inventory callback can synchronously re-enter this dispatcher.
        write(shot,new Flight(state.kind(),state.author(),state.epoch(),true));unloaded(shot);
        try{if(state.kind()==Kind.RUCHKA){ruchka(owner,nearest);burst((ServerLevel)shot.level(),nearest.position(),Kind.RUCHKA);}
            else if(state.kind()==Kind.BUCKET){bucket(shot,owner,nearest);}
            else if(state.kind()==Kind.PUMPKIN){pumpkin(shot,owner,nearest);}
            else{honey(owner,nearest);burst((ServerLevel)shot.level(),nearest.position(),Kind.HONEW);}}
        finally{shot.discard();}
    }
    /** Optional native onHit HEAD seam ensures a final proximity attempt at a collision point. */
    public void impact(Entity entity){beforeTick(entity);}
    private void ruchka(ServerPlayer owner,ServerPlayer target){
        game.items().psycho().afflict(target,owner,0,0,600,0);
        game.items().mana().award(target,"lbc:true_advancements/potions/effect_twitching");
        target.sendSystemMessage(Component.translatable("twitching_hands").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
        burst(target.level(),target.position(),Kind.RUCHKA);
    }
    private void honey(ServerPlayer owner,ServerPlayer target){
        if(game.items().inventoryHazards().protectedInventory(target))return;
        var level=target.level();var at=target.position().add(0,1,0);
        for(float pitch:new float[]{1,0,2})LbcOverload.sound(level,at,"minecraft:lbcsounds.yametekudasai",1,pitch,0,0);
        // Publication precedes debit, preserving a slot if native publication is rejected.
        for(int slot=0;slot<41;slot++){
            var stack=target.getInventory().getItem(slot);if(stack.isEmpty())continue;
            var copy=stack.copy();var item=new ItemEntity(level,at.x,at.y,at.z,copy);item.setPickUpDelay(40);
            // rand_motion_small samples three independent inclusive integer values, including negative Y.
            var random=level.getRandom();item.setDeltaMovement(new Vec3((random.nextInt(101)-50)*.01,
                    (random.nextInt(101)-50)*.01,(random.nextInt(101)-50)*.01));
            // Preserve an item's existing causal producer; ejecting it does not manufacture it again.
            game.items().markCurrentItemEpoch(item);
            if(level.addFreshEntity(item))target.getInventory().setItem(slot,ItemStack.EMPTY);
        }
        target.getInventory().setChanged();game.items().magic().inventoryChanged(target);target.containerMenu.broadcastChanges();
    }
    private void bucket(Snowball shot,ServerPlayer owner,ServerPlayer target){
        var level=target.level();
        equipHead(target,"lbc:imba/bucket_head",LbcUtilityItems.Kind.BUCKET_HEAD.item,"bucket_head");
        // A real projectile source retains caster attribution without becoming an accepted melee hit.
        var damage=new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(DamageTypes.GENERIC),shot,owner);
        target.hurtServer(level,game.items().indirectKills().captureDamage(damage,game.items().indirectKills().wildCause(owner)),5);
        for(int i=0;i<10;i++)LbcOverload.sound(level,target.position(),"minecraft:lbcsounds.bell",10,1,5,0);
    }
    /** Publish the exact previous helmet before replacing it; share protection and provenance with the bucket. */
    private boolean equipHead(ServerPlayer target,String table,net.minecraft.world.item.Item expected,String flag){
        if(game.items().inventoryHazards().protectedInventory(target))return false;
        var level=target.level();var output=game.lbc().rollTable(level,target.position(),target,table);
        if(output.size()!=1||!output.getFirst().is(expected)||output.getFirst().getCount()!=1
                ||flag!=null&&ItemData.intValue(output.getFirst(),flag,0)!=1)return false;
        var previous=target.getItemBySlot(EquipmentSlot.HEAD);
        if(!previous.isEmpty()){
            var at=target.position();var item=new ItemEntity(level,at.x,at.y,at.z,previous.copy());
            item.setPickUpDelay(0);game.items().markCurrentItemEpoch(item);
            if(!level.addFreshEntity(item))return false;
        }
        target.setItemSlot(EquipmentSlot.HEAD,output.getFirst());
        target.getInventory().setChanged();game.items().magic().inventoryChanged(target);target.containerMenu.broadcastChanges();return true;
    }
    private void pumpkin(Snowball shot,ServerPlayer owner,ServerPlayer target){
        var level=target.level();var at=target.position();
        for(float pitch:new float[]{1,0,2})LbcOverload.sound(level,at,"minecraft:lbcsounds.yametekudasai",1,pitch,0,0);
        if(equipHead(target,"lbc:imba/pumpkin_head",Items.CARVED_PUMPKIN,"helmet"))
            target.sendSystemMessage(Component.translatable("tikva_equip").withStyle(net.minecraft.ChatFormatting.YELLOW));
        var damage=new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.CACTUS),shot,owner);
        target.hurtServer(level,game.items().indirectKills().captureDamage(damage,game.items().indirectKills().wildCause(owner)),5);
        for(float pitch:new float[]{1,2})LbcOverload.sound(level,at,"minecraft:block.decorated_pot.insert",1,pitch,0,0);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM,ItemStackTemplate.fromNonEmptyStack(new ItemStack(Items.SNOWBALL))),
                at.x,at.y+1,at.z,40,.3,.6,.3,0);
    }
    private static void burst(ServerLevel level,Vec3 at,Kind kind){
        String sound=kind==Kind.RUCHKA?"minecraft:entity.slime.jump_small":"minecraft:entity.slime.jump";
        for(float pitch:kind==Kind.RUCHKA?new float[]{1,2}:new float[]{1,0,2})LbcOverload.sound(level,at,sound,1,pitch,0,0);
        var stack=kind==Kind.RUCHKA?new ItemStack(LbcThrownUtilityItems.RUCHKA):new ItemStack(Items.HONEY_BLOCK);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM,ItemStackTemplate.fromNonEmptyStack(stack)),
                at.x,at.y+1,at.z,40,.3,.6,.3,0);
    }
    private static void write(Snowball shot,Flight state){
        var data=new CompoundTag();data.putString("kind",state.kind().name());data.putString("author",state.author().toString());
        data.putString("epoch",state.epoch());data.putBoolean("claimed",state.claimed());shot.setAttached(DATA,data);
    }
    /** Rebind after entity load, canonical clone or replay; attachment is the authoritative saved state. */
    public void rebind(Entity entity){
        if(!(entity instanceof Snowball shot))return;unloaded(shot);
        var data=shot.getAttached(DATA);if(data==null)return;
        try{
            var state=new Flight(Kind.valueOf(data.getStringOr("kind","")),UUID.fromString(data.getStringOr("author","")),
                    data.getStringOr("epoch",""),data.getBooleanOr("claimed",false));
            shot.entityTags().add(TAG);
            if(state.claimed()||state.kind()==Kind.THROWABLE_METAL_PIPE||!game.items().currentEpoch(state.epoch()))shot.discard();
            else{flights.put(shot,state);previousPositions.put(shot,shot.position());}
        }catch(IllegalArgumentException invalid){shot.discard();}
    }
    public void loaded(Entity entity){rebind(entity);}
    /** Whole replay suppresses ordinary load callbacks; rebuild only the two derived loaded-actor indexes. */
    public void reindexRestored(Iterable<Entity> restored){
        flights.clear();previousPositions.clear();
        for(var entity:restored)rebind(entity);
    }
    public void unloaded(Entity entity){flights.remove(entity);previousPositions.remove(entity);}
    public void tick(){for(var shot:List.copyOf(flights.keySet()))if(shot.isRemoved())unloaded(shot);}
    private void retire(Snowball shot){unloaded(shot);shot.discard();}
    public int count(){return flights.size();}
    public Flight state(Entity entity){return flights.get(entity);}
    public List<Snowball> actors(){return List.copyOf(flights.keySet());}
    public void stop(){for(var shot:List.copyOf(flights.keySet()))shot.discard();flights.clear();previousPositions.clear();}
}
