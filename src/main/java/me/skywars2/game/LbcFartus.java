package me.skywars2.game;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.scores.ScoreHolder;
import java.util.*;

/** Fartus's two ordinary volleys and its Academy charge-powered palm sword. */
public final class LbcFartus {
    public enum Kind { BANANA, LABEL, HOMING }
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final String[] LOW_SOUNDS={"minecraft:lbcsounds.oluh_1","minecraft:lbcsounds.oluh_2","minecraft:lbcsounds.oluh_3","minecraft:lbcsounds.oluh_4","minecraft:lbcsounds.oluh_5","minecraft:lbcsounds.oluh_6"};
    public static final EntityType<Shot> TYPE=registerType();
    public static final Item ITEM=registerItem("skywars2:fartus_wand",false,false);
    public static final Item ACADEMY=registerItem("skywars2:academy_fartus_wand",true,false);
    /** Craft donor only; its legacy summon-on-melee ability is a separate port. */
    public static final Item MATERIAL=registerItem("skywars2:wtf_item",false,true);
    private final GameController game;
    private final Set<Shot> shots=new LinkedHashSet<>();
    LbcFartus(GameController game){this.game=game;}
    public static void register(){}
    private static EntityType<Shot> registerType(){
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Identifier.parse("skywars2:fartus_projectile"));
        var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<Shot>of(Shot::new,MobCategory.MISC)
                .sized(1,1).clientTrackingRange(10).updateInterval(1).build(key));
        PolymerEntityUtils.registerType(type);return type;
    }
    private static Item registerItem(String id,boolean academy,boolean material){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse(id));
        var model=Identifier.parse(material?"lbc:banan":academy?"lbc:palma":"lbc:fartus");
        var tag=new CompoundTag();tag.putInt(material?"wtf_item":"fartus_nadristailo",1);
        if(!material){tag.putInt("transformable",1);tag.putInt("staff",1);tag.putInt("magicwand",1);tag.putInt("swordwithability",1);}
        if(academy)tag.putInt("upgraded",1);
        var properties=new Item.Properties(){@Override public Identifier effectiveModel(){return model;}}
                .setId(key).stacksTo(material?64:1).overrideDescription(material?"wtf_item":"fartus_nadristailo")
                .component(DataComponents.ITEM_MODEL,model).component(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        if(!material)properties.component(DataComponents.TOOLTIP_STYLE,Identifier.parse(academy?"lbc:tier_x":"lbc:tier_s"));
        if(academy)properties.component(DataComponents.ATTRIBUTE_MODIFIERS,attributes(19));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(properties,material?Items.REPEATING_COMMAND_BLOCK:Items.CARROT_ON_A_STICK,true){
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
                var game=me.skywars2.SkyWars2.game();
                return !material&&game!=null&&player instanceof ServerPlayer owner&&game.items().wands().use(owner,hand)
                        ?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    static CompoundTag data(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    public static boolean isFartus(ItemStack stack){return !stack.isEmpty()&&(stack.is(ITEM)||stack.is(ACADEMY)||data(stack).getIntOr("fartus_nadristailo",0)==1);}
    public static boolean isMaterial(ItemStack stack){return !stack.isEmpty()&&(stack.is(MATERIAL)||data(stack).getIntOr("wtf_item",0)==1);}
    public static boolean academy(ItemStack stack){return isFartus(stack)&&(stack.is(ACADEMY)||data(stack).getIntOr("upgraded",0)==1||!LbcTmtAbilities.active(stack)&&LbcTmtInventory.upgraded(stack,"fartus_nadristailo"));}
    public static boolean magic(Entity entity){return entity instanceof Shot shot&&shot.kind!=Kind.BANANA;}
    public static final class Shot extends Projectile implements PolymerEntity {
        Kind kind=Kind.BANANA;int age;float launchYaw,launchPitch;Display.ItemDisplay display;LbcFartus service;
        Shot(EntityType<? extends Shot> type,Level level){super(type,level);}
        @Override protected void defineSynchedData(SynchedEntityData.Builder builder){}
        @Override public EntityType<?> getPolymerEntityType(PacketContext context){return EntityTypes.MARKER;}
        @Override public boolean shouldBeSaved(){return false;}
        @Override public boolean isPushedByFluid(){return false;}
        @Override public boolean isPushable(){return false;}
    }
    public int count(){return shots.size();}
    List<Shot> snapshot(){return List.copyOf(shots);}
    public boolean use(ServerPlayer owner,InteractionHand hand){
        var stack=owner.getItemInHand(hand);
        if(!isFartus(stack)||!validOwner(owner)||game.items().mana().noMagic(owner)>0||game.items().actionsBlocked(owner)||game.items().wands().inputTimeStopped(owner)
                ||game.items().magic().suppressed(owner)||game.items().magic().suppressedAt(owner.level(),owner.getEyePosition()))return false;
        if(academy(stack)){
            int charges=charges(owner);if(charges<3)return false;
            game.items().mana().score(owner,"fartus_sword").set(charges-3);
            for(int i=0;i<5;i++){
                float yaw=owner.getYRot()+(owner.getRandom().nextInt(10001)-5000)*.01f;
                float pitch=owner.getXRot()+(owner.getRandom().nextInt(10001)-5000)*.01f;
                spawn(owner,Kind.HOMING,owner.getEyePosition(),Vec3.directionFromRotation(pitch,yaw).scale(.4));
            }
            return true;
        }
        int cost=game.items().mana().score(ScoreHolder.forNameOnly("fartus_nadristailo_mana_use"),"lbc.math").get();
        if(!game.items().mana().trySpendWand(owner,cost))return false;
        int branch=owner.getRandom().nextInt(2)+1;
        cast(owner,branch);trial(owner,hand,stack,branch);
        LbcOverload.sound(owner,owner.position(),"minecraft:entity.snowball.throw",1,1,0);
        game.items().wands().wandCasted(owner);return true;
    }
    /** A deterministic choice still follows the exact production five-shot geometry. */
    void cast(ServerPlayer owner,int branch){
        if(branch!=1&&branch!=2)throw new IllegalArgumentException("Fartus branch must be1 or2");
        for(int i=0;i<5;i++){
            Vec3 motion;
            if(branch==1){var r=owner.getRandom();var v=owner.getLookAngle();motion=new Vec3(
                    (Math.floor(v.x*15000)+r.nextInt(10001)-5000)*.0001,
                    (Math.floor(v.y*15000)+r.nextInt(10001)-5000)*.0001,
                    (Math.floor(v.z*15000)+r.nextInt(10001)-5000)*.0001);}
            else motion=Vec3.directionFromRotation(owner.getXRot(),owner.getYRot()+30-i*15).scale(.4);
            spawn(owner,branch==1?Kind.BANANA:Kind.LABEL,owner.getEyePosition(),motion);
            if(branch==2)LbcOverload.sound(owner,owner.position(),"minecraft:ui.cartography_table.take_result",.5f,2,0);
        }
    }
    void trial(ServerPlayer owner,InteractionHand hand,ItemStack stack,int branch){
        if(!LbcAcademy.trial(stack)||owner.getItemInHand(hand)!=stack)return;
        var tag=data(stack);int score=tag.getIntOr("score",0),prev=tag.getIntOr("prev",0);
        score=switch(score){case 1->branch==prev?2:0;case 2->branch!=prev?3:0;case 3->branch==prev?4:0;default->score;};
        if(score==0)score=1;tag.putInt("prev",branch);tag.putInt("score",score);
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));owner.getInventory().setChanged();
        if(score>=4)game.items().academy().ready(owner,hand,stack);
    }
    Shot spawn(ServerPlayer owner,Kind kind,Vec3 origin,Vec3 motion){
        if(!owner.level().hasChunkAt(BlockPos.containing(origin)))return null;
        var shot=new Shot(TYPE,owner.level());shot.service=this;shot.kind=kind;shot.launchYaw=owner.getYRot();shot.launchPitch=owner.getXRot();shot.setOwner(owner);shot.setPos(origin);shot.setNoGravity(true);shot.setDeltaMovement(motion);
        shot.entityTags().add(kind==Kind.BANANA?"physical_object":"magic");shot.entityTags().add("skywars2.fartus_shot");game.items().markCurrentItemEpoch(shot);
        var display=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,owner.level());shot.display=display;
        try{
            var preset=TagParser.parseCompoundFully("{teleport_duration:1,interpolation_duration:1,shadow_radius:0f,shadow_strength:0f,item_display:'fixed',view_range:10f,transformation:{translation:[0f,0f,0f],left_rotation:"+(kind==Kind.LABEL?"[1f,0f,0f,1f]":"[0f,0f,0f,1f]")+",right_rotation:[0f,0f,0f,1f],scale:"+(kind==Kind.LABEL?"[.5f,.5f,.5f]":"[1f,1f,1f]")+"},item:{id:'"+(kind==Kind.LABEL?"minecraft:repeating_command_block":"minecraft:carrot_on_a_stick")+"',count:1,components:{'minecraft:item_model':'"+(kind==Kind.LABEL?"lbc:banan":"lbc:banana")+"'}}}");
            display.load(TagValueInput.create(ProblemReporter.DISCARDING,owner.registryAccess(),preset));
        }catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}
        // Visual actors never simulate gravity; ordinary banana gravity belongs only
        // to physical(shot), while homing flight has no gravitational acceleration.
        display.setNoGravity(true);
        display.entityTags().add("skywars2.fartus_display");if(kind!=Kind.BANANA)display.entityTags().add("magic");game.items().markCurrentItemEpoch(display);
        orient(shot);shots.add(shot);game.items().timeStop().prepareFrozen(shot);game.items().timeStop().prepareFrozen(display);
        if(!owner.level().addFreshEntity(shot)||shot.isRemoved()||!owner.level().addFreshEntity(display))remove(shot);
        return shot;
    }
    private void orient(Shot shot){
        var v=shot.getDeltaMovement();if(v.lengthSqr()>1e-12){shot.setYRot((float)-Math.toDegrees(Math.atan2(v.x,v.z)));shot.setXRot((float)-Math.toDegrees(Math.atan2(v.y,v.horizontalDistance())));}
        if(shot.display!=null){
            if(shot.kind==Kind.BANANA){
                // Source ItemDisplay retains its launch entity Rotation. Paper
                // PhysicalObjectPhysics applies current motion in left_rotation.
                shot.display.snapTo(shot.getX(),shot.getY(),shot.getZ(),shot.launchYaw,shot.launchPitch);
                if(shot.age>0&&v.lengthSqr()>1e-6)shot.display.setTransformation(new com.mojang.math.Transformation(
                        new org.joml.Vector3f(),new org.joml.Quaternionf().rotateY((float)Math.toRadians(-shot.getYRot())).rotateX((float)Math.toRadians(shot.getXRot())),
                        new org.joml.Vector3f(1),new org.joml.Quaternionf()));
            }else shot.display.snapTo(shot.getX(),shot.getY(),shot.getZ(),shot.getYRot(),shot.getXRot());
        }
    }
    private boolean validOwner(ServerPlayer owner){return game.playing()&&game.stage()!=GameController.Stage.ENDING&&owner.isAlive()&&!owner.isRemoved()
            &&game.canUseGameplayItems(owner)&&owner.level()==game.maps().level();}
    private boolean valid(Shot shot){return !shot.isRemoved()&&shot.getOwner() instanceof ServerPlayer owner&&validOwner(owner)&&owner.level()==shot.level()
            &&game.items().currentEpoch(shot)&&shot.display!=null&&!shot.display.isRemoved()&&((ServerLevel)shot.level()).hasChunkAt(shot.blockPosition())
            &&(shot.kind==Kind.BANANA||!game.items().magic().suppressedAt((ServerLevel)shot.level(),shot.position()));}
    public void tick(){
        for(var shot:List.copyOf(shots))flight(shot);
        if(game.server().getTickCount()%20==0)for(var owner:game.server().getPlayerList().getPlayers())updateSword(owner);
    }
    void flight(Shot shot){
        if(!shots.contains(shot))return;
        if(!valid(shot)){remove(shot);return;}
        var level=(ServerLevel)shot.level();
        if(game.items().timeStop().stopped(level)||game.items().timeStop().frozen(shot))return;
        LivingEntity homingTarget=shot.kind==Kind.HOMING?homingTarget(shot):null;
        if(shot.kind==Kind.BANANA)physical(shot);
        else for(int i=0;i<3&&!shot.isRemoved();i++){
            if(homingTarget!=null)home(shot,homingTarget);
            Vec3 from=shot.position(),to=from.add(shot.getDeltaMovement().normalize().scale(.4));
            var wall=LbcTerra.wallHit(level,from,to);
            Vec3 end=wall==null?to:wall.getLocation();
            if(contact(shot,from,end,0))break;
            shot.setPos(end);
            // User clarified: keep source homing wall reversal, disable only gravity.
            if(wall!=null){if(!level.hasChunkAt(wall.getBlockPos())||shot.kind==Kind.LABEL){remove(shot);break;}
                // Keep the swept hit just outside the solid so the following
                // outward substep does not collide again at the same boundary.
                shot.setPos(end.subtract(shot.getDeltaMovement().normalize().scale(1e-7)));
                shot.setDeltaMovement(shot.getDeltaMovement().reverse());}
            if(!valid(shot)){remove(shot);break;}
        }
        if(!shot.isRemoved()){if(++shot.age>=(shot.kind==Kind.LABEL?60:100))remove(shot);else orient(shot);}
    }
    private LivingEntity homingTarget(Shot shot){
        var owner=(ServerPlayer)shot.getOwner();LivingEntity nearest=null;double distance=Double.POSITIVE_INFINITY;
        for(var candidate:game.items().lifetimes().loadedTargets())if(candidate.level()==shot.level()&&candidate.getType().builtInRegistryHolder().is(MOBS)
                &&game.items().wands().target(owner,candidate)&&!game.items().magic().suppressedAt((ServerLevel)shot.level(),candidate.position())){
            double d=shot.position().distanceToSqr(candidate.getEyePosition());if(d<distance){distance=d;nearest=candidate;}
        }
        return nearest;
    }
    private void home(Shot shot,LivingEntity nearest){
        var toward=nearest.getEyePosition().subtract(shot.position()).normalize();
        shot.setDeltaMovement(shot.getDeltaMovement().normalize().add(toward.scale(.05)).normalize().scale(.4));
    }
    /** Source physical banana: centered cube, axis X/Y/Z clipping, .08 gravity and .9 rebound. */
    private void physical(Shot shot){
        var level=(ServerLevel)shot.level();
        LbcPhysicalObjects.step(shot,.5,.9,.8,1,(from,to)->{
            if(LbcTerra.wallHit(level,from,to,state->false)!=null){remove(shot);return true;}return false;
        },(from,to)->contact(shot,from,to,.5));
    }
    private boolean contact(Shot shot,Vec3 from,Vec3 to,double padding){
        var level=(ServerLevel)shot.level();var owner=(ServerPlayer)shot.getOwner();
        var greed=LbcGreed.firstHit(level,from,to,padding);
        var hit=ProjectileUtil.getEntityHitResult(level,shot,from,to,new AABB(from,to).inflate(1+padding),
                e->e instanceof LivingEntity&&e.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().projectileTarget(owner,e),(float)padding);
        if(greed!=null&&(shot.getOwner()!=greed.getEntity()||!greed.getEntity().getBoundingBox().inflate(.2).contains(from))
                &&(hit==null||from.distanceToSqr(greed.getLocation())<=from.distanceToSqr(hit.getLocation()))){
            shot.setPos(greed.getLocation());LbcGreed.reflect(shot,greed.getEntity());orient(shot);return true;
        }
        if(hit==null)return false;
        shot.setPos(hit.getLocation());impact(shot,(LivingEntity)hit.getEntity());return true;
    }
    void impact(Shot shot,LivingEntity target){
        if(!valid(shot)||!(shot.getOwner() instanceof ServerPlayer owner)||!game.items().wands().projectileTarget(owner,target))return;
        if(shot.kind!=Kind.BANANA&&game.items().magic().suppressedAt((ServerLevel)target.level(),target.position())){remove(shot);return;}
        var level=(ServerLevel)shot.level();var cause=game.items().indirectKills().coldCause(owner,true);
        var victims=new ArrayList<LivingEntity>();victims.add(target);
        if(shot.kind==Kind.BANANA)for(var other:level.getEntitiesOfClass(LivingEntity.class,new AABB(shot.position(),shot.position()).inflate(.500001),
                e->e.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().projectileTarget(owner,e)))if(other!=target)victims.add(other);
        for(var victim:victims)applyImpact(shot,victim,owner,cause);
        remove(shot);
    }
    private void applyImpact(Shot shot,LivingEntity target,ServerPlayer owner,LbcIndirectKills.Owner cause){
        var level=(ServerLevel)shot.level();
        if(shot.kind==Kind.LABEL){
            game.items().indirectKills().inheritedEffect(target,ItemEffectLevels.create(MobEffects.WITHER,300,1),cause);
            game.items().indirectKills().inheritedEffect(target,ItemEffectLevels.create(MobEffects.POISON,300,2),cause);
            game.items().indirectKills().inheritedEffect(target,ItemEffectLevels.create(MobEffects.SLOWNESS,300,0),cause);
            if(target instanceof ServerPlayer player)player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                    net.minecraft.core.Holder.direct(net.minecraft.sounds.SoundEvent.createVariableRangeEvent(Identifier.parse(LOW_SOUNDS[owner.getRandom().nextInt(6)]))),
                    net.minecraft.sounds.SoundSource.MASTER,shot.getX(),shot.getY(),shot.getZ(),1,1,owner.getRandom().nextLong()));
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM,net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(model("lbc:banan",true))),shot.getX(),shot.getY(),shot.getZ(),1,0,0,0,0);
        }else{
            // The source's Slowness C becomes negative speed with uncapped attributes.
            // The shared stun enforces the same40 active ticks without that exploit.
            game.items().shooters().stun(owner,target,40);
            for(var effect:List.of(MobEffects.BLINDNESS,MobEffects.DARKNESS,MobEffects.WEAKNESS))
                game.items().indirectKills().inheritedEffect(target,ItemEffectLevels.create(effect,40,effect.is(MobEffects.BLINDNESS)||effect.is(MobEffects.DARKNESS)?1:99,false,false),cause);
            game.items().indirectKills().withCause(target,cause,()->MobEffects.INSTANT_DAMAGE.value().applyInstantaneousEffect(level,shot,owner,target,3,1));
            LbcOverload.sound(owner,target.position(),"minecraft:lbcsounds.bananoponos",1,1,0);
            level.sendParticles(ParticleTypes.SNEEZE,target.getX(),target.getY()+1,target.getZ(),40,.1,.1,.1,1);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.CONCRETE_POWDER.pick(net.minecraft.world.item.DyeColor.YELLOW).defaultBlockState()),target.getX(),target.getY()+1,target.getZ(),40,.3,1,.3,1);
        }
    }
    private static ItemStack model(String name,boolean label){var stack=new ItemStack(label?Items.REPEATING_COMMAND_BLOCK:Items.CARROT_ON_A_STICK);stack.set(DataComponents.ITEM_MODEL,Identifier.parse(name));return stack;}
    public int charges(ServerPlayer owner){return game.items().mana().score(owner,"fartus_sword").get();}
    /** Called only by the common accepted-melee hook; source and victim remain native. */
    public void melee(ServerPlayer owner,LivingEntity victim,DamageSource source){
        if(LbcDamage.isMeleeAttack(source,owner)&&validOwner(owner)&&academy(owner.getMainHandItem())&&game.items().wands().projectileTarget(owner,victim,true)){
            game.items().mana().score(owner,"fartus_sword").set(charges(owner)+1);
            // User: the Academy staff's accepted close-combat strike also stuns.
            game.items().shooters().stun(owner,victim,40);
        }
    }
    static ItemAttributeModifiers attributes(double damage){
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,new AttributeModifier(Identifier.parse("minecraft:base_attack_damage"),damage,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,new AttributeModifier(Identifier.parse("minecraft:base_attack_speed"),-3.5,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE,new AttributeModifier(Identifier.parse("minecraft:a8uvy89ryvmg89dmybv8gdfrgg"),10,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND).build();
    }
    void updateSword(ServerPlayer owner){
        if(!validOwner(owner)||!academy(owner.getMainHandItem())||game.items().wands().inputTimeStopped(owner))return;
        long count=0;for(var stack:owner.getInventory().getNonEquipmentItems())if(ItemData.intValue(stack,"govnoq",0)==1)count+=stack.getCount();
        // clear @s includes offhand/equipment in the source, not just hotbar/storage.
        for(var slot:net.minecraft.world.entity.EquipmentSlot.values())if(slot!=net.minecraft.world.entity.EquipmentSlot.MAINHAND){var stack=owner.getItemBySlot(slot);if(ItemData.intValue(stack,"govnoq",0)==1)count+=stack.getCount();}
        owner.getMainHandItem().set(DataComponents.ATTRIBUTE_MODIFIERS,attributes(19+10d*count));owner.getInventory().setChanged();
    }
    public MutableComponent hud(ServerPlayer owner){return academy(owner.getMainHandItem())||academy(owner.getOffhandItem())
            ?Component.literal("    \uE103 §fx").append(Component.literal(Integer.toString(charges(owner))).withStyle(ChatFormatting.YELLOW)):Component.empty();}
    private void remove(Shot shot){shots.remove(shot);if(shot.display!=null&&!shot.display.isRemoved())shot.display.discard();if(!shot.isRemoved())shot.discard();}
    public void loaded(Entity entity){
        if(entity instanceof Shot shot){if(!shot.isRemoved()&&shot.service==this&&game.items().currentEpoch(shot))shots.add(shot);else shot.discard();}
        else if(entity.entityTags().contains("skywars2.fartus_display")&&shots.stream().noneMatch(s->s.display==entity))entity.discard();
    }
    public void unloaded(Entity entity){if(entity instanceof Shot shot)remove(shot);else for(var shot:List.copyOf(shots))if(shot.display==entity)remove(shot);}
    public void forget(ServerPlayer owner){for(var shot:List.copyOf(shots))if(shot.getOwner()==owner)remove(shot);game.items().mana().score(owner,"fartus_sword").set(0);}
    public void purify(ServerPlayer owner){forget(owner);}
    public void stop(){for(var shot:List.copyOf(shots))remove(shot);for(var owner:game.server().getPlayerList().getPlayers())game.items().mana().score(owner,"fartus_sword").set(0);}

    void cancelTransient(){for(var shot:List.copyOf(shots))remove(shot);}

    LbcCloneSkills.ActorImage cloneImage(Shot source) {
        var kind=source.kind;int age=source.age;float yaw=source.launchYaw,pitch=source.launchPitch;
        return new LbcCloneSkills.ActorImage() {
            public void install(Entity original,Entity clone,Map<Entity,Entity> family) {
                var target=(Shot)clone;target.service=LbcFartus.this;target.kind=kind;target.age=age;
                target.launchYaw=yaw;target.launchPitch=pitch;
                var display=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,target.level());target.display=display;
                try{
                    var preset=TagParser.parseCompoundFully("{teleport_duration:1,interpolation_duration:1,shadow_radius:0f,shadow_strength:0f,item_display:'fixed',view_range:10f,transformation:{translation:[0f,0f,0f],left_rotation:"+(target.kind==Kind.LABEL?"[1f,0f,0f,1f]":"[0f,0f,0f,1f]")+",right_rotation:[0f,0f,0f,1f],scale:"+(target.kind==Kind.LABEL?"[.5f,.5f,.5f]":"[1f,1f,1f]")+"},item:{id:'"+(target.kind==Kind.LABEL?"minecraft:repeating_command_block":"minecraft:carrot_on_a_stick")+"',count:1,components:{'minecraft:item_model':'"+(target.kind==Kind.LABEL?"lbc:banan":"lbc:banana")+"'}}}");
                    display.load(TagValueInput.create(ProblemReporter.DISCARDING,target.registryAccess(),preset));
                }catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}
                // Visual actors never simulate gravity; ordinary banana gravity belongs only
                // to physical(shot), while homing flight has no gravitational acceleration.
                display.setNoGravity(true);
                display.entityTags().add("skywars2.fartus_display");if(target.kind!=Kind.BANANA)display.entityTags().add("magic");game.items().markCurrentItemEpoch(display);
                orient(target);shots.add(target);game.items().spellSources().inherit(target,display);
                game.items().timeStop().prepareFrozen(display);
                if(!target.level().addFreshEntity(display)||display.isRemoved())throw new IllegalStateException("Fartus clone display publication failed");
            }
            public void rollback(Entity clone){remove((Shot)clone);}
        };
    }


    void directionChanged(Entity actor) { if(actor instanceof Shot shot)orient(shot); }

}
