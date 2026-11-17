package me.skywars2.game.genie;

import java.util.*;
import net.minecraft.util.Prediction;
import me.skywars2.game.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import static me.skywars2.game.genie.GenieBlueprint.*;

/** Bounded native primitives. A blueprint cannot execute Java, commands or custom NBT. */
public final class GenieRuntime {
    public static final String PROGRAM="genie_program",SERIAL="genie_serial";
    private record Key(UUID owner,String serial,int rule){}
    private final GameController game;
    private final Map<Key,Integer> cooldowns=new HashMap<>();
    private final Map<String,GenieBlueprint> programs=new LinkedHashMap<>();
    private boolean dispatching;
    private final Set<Projectile> projectiles=Collections.newSetFromMap(new IdentityHashMap<>());
    private static final String SHOT_POWER="skywars2.genie_power/",SHOT_LIFE="skywars2.genie_life/";
    GenieRuntime(GameController game){this.game=game;}
    public static boolean matches(ItemStack stack){return ItemData.contains(stack,PROGRAM);}
    private static Identifier id(String text){return Identifier.withDefaultNamespace(text);}
    private static void require(boolean condition,String reason){if(!condition)throw new IllegalArgumentException(reason);}
    private static boolean forbiddenItem(Item item){
        var path=BuiltInRegistries.ITEM.getKey(item).getPath();
        return item==Items.AIR||path.contains("command_block")||path.contains("structure")||path.endsWith("spawn_egg")
                ||Set.of("bedrock","barrier","spawner","trial_spawner","jigsaw","debug_stick","light","end_portal_frame","knowledge_book","test_block","test_instance_block").contains(path);
    }
    /** Resolve all ids first: a failed program cannot consume a lamp or half-apply attributes. */
    public static void validateNative(GenieBlueprint plan){
        plan.validate();var icon=BuiltInRegistries.ITEM.get(id(plan.icon())).orElseThrow(()->new IllegalArgumentException("unknown item icon")).value();
        require(!forbiddenItem(icon),"unsupported native item");
        require(!icon.components().has(DataComponents.CONSUMABLE)||!Set.of(TwistKind.BRITTLE,TwistKind.DROP_ITEM).contains(plan.twist().kind()),"consumables need a drawback beyond consuming or dropping their already-spent source");
        for(var stat:plan.stats())require(BuiltInRegistries.ATTRIBUTE.get(id(stat.id())).isPresent(),"unknown attribute");
        require(plan.stats().isEmpty()||!Set.of(TwistKind.WET_ONLY,TwistKind.DRY_ONLY,TwistKind.NIGHT_ONLY,TwistKind.AIR_ONLY).contains(plan.twist().kind()),"conditional drawback cannot gate native attribute bonuses");
        for(var rule:plan.rules()){
            if(Set.of(Effect.DAMAGE,Effect.HEAL,Effect.STATUS,Effect.FIRE,Effect.PUSH,Effect.PULL,Effect.MANA,Effect.FOOD,Effect.CLEANSE).contains(rule.effect()))require(rule.target()!=Target.LOOK_POINT,"effect needs living targets");
            if(rule.effect()==Effect.EXPLOSION)require(!Set.of(Target.NEARBY_ALL,Target.NEARBY_ENEMIES).contains(rule.target()),"area explosion uses one centre, not one explosion per nearby body");
            if(rule.effect()==Effect.PROJECTILE)require(rule.target()==Target.SELF||rule.target()==Target.LOOK_POINT,"projectiles originate at the owner's camera");
            if(rule.effect()==Effect.TELEPORT||rule.effect()==Effect.REPAIR)require(rule.target()==Target.SELF,"self primitive requires SELF");
            if(rule.effect()==Effect.TELEPORT)require(rule.radius()>0,"teleport needs a nonzero range");
            if(rule.effect()==Effect.REPAIR)require(icon.components().has(DataComponents.MAX_DAMAGE),"repair needs a native durable item");

            if(rule.trigger()==Trigger.SHOT)require(icon instanceof BowItem||icon instanceof CrossbowItem,"SHOT needs a real ranged weapon");
            if(rule.trigger()==Trigger.EAT)require(icon.components().has(DataComponents.CONSUMABLE),"EAT needs a real consumable");
            if(rule.target()==Target.HIT_TARGET)require(rule.trigger()==Trigger.HIT||rule.trigger()==Trigger.HURT,"hit target needs a contact event");
            switch(rule.effect()){
                case STATUS -> require(BuiltInRegistries.MOB_EFFECT.get(id(rule.id())).isPresent(),"unknown status");
                case SOUND -> require(BuiltInRegistries.SOUND_EVENT.get(id(rule.id())).isPresent(),"unknown sound");
                case PARTICLES -> require(BuiltInRegistries.PARTICLE_TYPE.get(id(rule.id())).map(h->h.value() instanceof SimpleParticleType).orElse(false),"particle needs a simple native type");
                case BLOCK -> {
                    var block=BuiltInRegistries.BLOCK.get(id(rule.id())).orElseThrow(()->new IllegalArgumentException("unknown block")).value();
                    require(block!=Blocks.AIR&&!block.defaultBlockState().hasBlockEntity()&&!forbiddenItem(block.asItem())&&block.defaultBlockState().getDestroySpeed(null,BlockPos.ZERO)>=0,"unsupported block material");
                }
                default -> {}
            }
        }
    }
    public static ItemStack construct(GenieBlueprint plan){
        validateNative(plan);var item=new ItemStack(BuiltInRegistries.ITEM.get(id(plan.icon())).orElseThrow().value(),plan.count());
        item.set(DataComponents.CUSTOM_NAME,Component.literal(plan.name()).withStyle(s->s.withItalic(false)));
        item.set(DataComponents.LORE,new ItemLore(List.of(Component.literal(plan.description()).withStyle(s->s.withItalic(false).withColor(0xffffff)))));
        item.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true);
        var data=item.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();data.putString(PROGRAM,plan.encode());data.putString(SERIAL,UUID.randomUUID().toString());item.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
        var modifiers=item.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY);int index=0;
        for(var stat:plan.stats()){
            var slot=switch(stat.slot()){
                case AUTO -> {var equipment=item.get(DataComponents.EQUIPPABLE);yield equipment==null?EquipmentSlotGroup.MAINHAND:EquipmentSlotGroup.bySlot(equipment.slot());}
                case MAIN_HAND -> EquipmentSlotGroup.MAINHAND;case OFF_HAND -> EquipmentSlotGroup.OFFHAND;case HAND -> EquipmentSlotGroup.HAND;
                case HEAD -> EquipmentSlotGroup.HEAD;case CHEST -> EquipmentSlotGroup.CHEST;case LEGS -> EquipmentSlotGroup.LEGS;case FEET -> EquipmentSlotGroup.FEET;
            };
            modifiers=modifiers.withModifierAdded(BuiltInRegistries.ATTRIBUTE.get(id(stat.id())).orElseThrow(),
                    new AttributeModifier(Identifier.parse("skywars2:genie/"+ItemData.stringValue(item,SERIAL,"")+"/stat_"+index++),stat.amount(),AttributeModifier.Operation.ADD_VALUE),slot);
        }
        item.set(DataComponents.ATTRIBUTE_MODIFIERS,modifiers);return item;
    }
    private GenieBlueprint plan(ItemStack stack){
        String text=ItemData.stringValue(stack,PROGRAM,"");if(text.isEmpty())return null;
        var found=programs.get(text);if(found!=null)return found;
        try{found=GenieBlueprint.decode(text);validateNative(found);if(programs.size()>=128)programs.remove(programs.keySet().iterator().next());programs.put(text,found);return found;}
        catch(RuntimeException invalid){return null;}
    }
    public boolean admitted(ServerPlayer owner){return game.playing()&&!game.balanceLegacy()&&!game.isObserver(owner)&&owner.isAlive()
            &&game.canUseGameplayItems(owner)&&!game.items().actionsBlocked(owner)&&!GenieNative.stopped(game,owner);}
    public void input(ServerPlayer owner,LbcTmtTriggers.Input input,Entity target){
        Trigger trigger=switch(input){case RIGHT_CLICK,BLOCK_USE -> Trigger.USE;case HIT -> Trigger.HIT;case HURT -> Trigger.HURT;case JUMP -> Trigger.JUMP;
            case SPRINT -> Trigger.SPRINT;case TICK -> Trigger.TICK;
            // These events have an exact captured native source, including its final consumed/broken item.
            case SHOT,EAT,MINING -> null;default -> null;};
        if(trigger!=null)event(owner,trigger,target,null);
    }
    /** Captured source survives the last consumable/shot/break without becoming a held replacement. */
    public void event(ServerPlayer owner,Trigger trigger,Entity target,ItemStack captured){event(owner,trigger,target,captured,null);}
    public void event(ServerPlayer owner,Trigger trigger,Entity target,ItemStack captured,InteractionHand usedHand){
        if(dispatching||!admitted(owner))return;dispatching=true;
        try{
            var selected=Collections.newSetFromMap(new IdentityHashMap<ItemStack,Boolean>());
            if(captured!=null)selected.add(captured);else {
                selected.add(owner.getMainHandItem());selected.add(owner.getOffhandItem());
                if(trigger==Trigger.TICK||trigger==Trigger.HURT||trigger==Trigger.JUMP||trigger==Trigger.SPRINT)
                    for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))selected.add(owner.getItemBySlot(slot));
            }
            for(var stack:selected){
                var plan=plan(stack);if(plan==null)continue;String serial=ItemData.stringValue(stack,SERIAL,"");if(serial.isEmpty())continue;
                // Snapshot components own effects/enchantments; wear/repair stays on the real used hand.
                var physical=captured==null?stack:usedHand==null?ItemStack.EMPTY:owner.getItemInHand(usedHand);
                if(!serial.equals(ItemData.stringValue(physical,SERIAL,"")))physical=ItemStack.EMPTY;
                if(plan.rules().isEmpty()&&trigger==Trigger.TICK){
                    var passive=new Key(owner.getUUID(),serial,-1);if(!cooldowns.containsKey(passive)){cooldowns.put(passive,20);drawback(owner,physical,target,plan.twist());}
                }
                for(int i=0;i<plan.rules().size();i++){
                    var rule=plan.rules().get(i);var key=new Key(owner.getUUID(),serial,i);
                    if(rule.trigger()!=trigger||cooldowns.containsKey(key)||!condition(owner,rule.condition())||!twistCondition(owner,plan.twist())||owner.getRandom().nextDouble()>=rule.chance())continue;
                    var point=lookPoint(owner);var targets=targets(owner,target,point,rule);
                    if(targets.isEmpty()&&rule.target()!=Target.LOOK_POINT)continue;
                    cooldowns.put(key,rule.cooldown());
                    try(var weapon=WeaponEnchantments.using(owner,stack)){
                        apply(owner,stack,physical,point,targets,rule);
                        if(admitted(owner))drawback(owner,physical,target,plan.twist());
                    }
                    if(!owner.isAlive())return;
                }
            }
        }finally{dispatching=false;}
    }
    private static boolean condition(ServerPlayer p,Condition condition){return switch(condition){
        case ALWAYS -> true;case WET -> p.isInWaterOrRain();case DRY -> !p.isInWaterOrRain();case ON_GROUND -> p.onGround();case IN_AIR -> !p.onGround();
        case DAY -> p.level().isBrightOutside();case NIGHT -> !p.level().isBrightOutside();case SNEAKING -> p.isShiftKeyDown();case SPRINTING -> p.isSprinting();
        case LOW_HEALTH -> p.getHealth()<p.getMaxHealth()*.3;case FULL_HEALTH -> p.getHealth()>=p.getMaxHealth();};}
    private static boolean twistCondition(ServerPlayer p,Twist twist){return switch(twist.kind()){
        case WET_ONLY -> p.isInWaterOrRain();case DRY_ONLY -> !p.isInWaterOrRain();case NIGHT_ONLY -> !p.level().isBrightOutside();case AIR_ONLY -> !p.onGround();default -> true;};}
    private static Vec3 lookPoint(ServerPlayer p){var from=p.getEyePosition();return p.level().clip(new ClipContext(from,from.add(p.getLookAngle().scale(32)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getLocation();}
    private List<LivingEntity> targets(ServerPlayer p,Entity hit,Vec3 point,Rule rule){
        return switch(rule.target()){
            case SELF -> List.of(p);case HIT_TARGET -> hit instanceof LivingEntity living?List.of(living):List.of();
            case LOOK_POINT -> List.of();
            case NEARBY_ALL,NEARBY_ENEMIES -> p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(rule.radius()),
                    e->e.isAlive()&&e.distanceToSqr(p)<=rule.radius()*rule.radius()&&(rule.target()==Target.NEARBY_ALL||GenieNative.enemy(game,p,e))).stream().limit(32).toList();
            case AIMED_TARGET -> {
                LivingEntity nearest=null;double distance=Double.POSITIVE_INFINITY;var from=p.getEyePosition();
                for(var body:p.level().getEntitiesOfClass(LivingEntity.class,new AABB(from,point).inflate(.5),e->e!=p&&e.isAlive())){
                    var contact=body.getBoundingBox().inflate(.3).clip(from,point);if(contact.isPresent()&&contact.get().distanceToSqr(from)<distance){distance=contact.get().distanceToSqr(from);nearest=body;}
                }yield nearest==null?List.of():List.of(nearest);
            }
        };
    }
    private void damage(ServerPlayer owner,LivingEntity victim,double amount){if(amount>0)victim.hurtServer(owner.level(),owner.damageSources().indirectMagic(owner,owner),(float)amount);}
    private void apply(ServerPlayer owner,ItemStack stack,ItemStack physical,Vec3 point,List<LivingEntity> targets,Rule rule){
        var level=owner.level();var positions=rule.target()==Target.LOOK_POINT?List.of(point):targets.stream().map(Entity::position).toList();
        switch(rule.effect()){
            case DAMAGE -> targets.forEach(t->damage(owner,t,rule.amount()));
            case HEAL -> targets.forEach(t->t.heal((float)rule.amount()));
            case STATUS -> targets.forEach(t->t.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.get(id(rule.id())).orElseThrow(),Math.max(1,rule.ticks()),(int)rule.amount()),owner));
            case FIRE -> targets.forEach(t->{t.igniteForSeconds((float)Math.max(.05,rule.ticks()/20.));game.items().indirectKills().ignited(t,owner);});
            case PUSH,PULL -> targets.forEach(t->{var direction=t==owner?owner.getLookAngle():t.position().subtract(owner.position()).normalize();var vector=direction.scale(Math.min(3,rule.amount())*(rule.effect()==Effect.PULL?-1:1));t.push(vector.x,vector.y+.1,vector.z);t.syncVelocity=true;game.items().indirectKills().displaced(t,owner);});
            case TELEPORT -> teleport(owner,rule.radius());
            case EXPLOSION -> positions.forEach(pos->level.explode(owner,pos.x,pos.y,pos.z,(float)rule.amount(),Level.ExplosionInteraction.MOB));
            case LIGHTNING -> {
                Entity first=null;for(var pos:positions){var bolt=EntityTypes.LIGHTNING_BOLT.create(level,EntitySpawnReason.TRIGGERED);if(bolt!=null){bolt.setPos(pos);bolt.setVisualOnly(true);level.addFreshEntity(bolt);if(first==null)first=bolt;}}
                var source=new net.minecraft.world.damagesource.DamageSource(owner.damageSources().lightningBolt().typeHolder(),first,owner);
                var victims=rule.target()==Target.LOOK_POINT?level.getEntitiesOfClass(LivingEntity.class,new AABB(point,point).inflate(2)):targets;
                victims.forEach(t->t.hurtServer(level,source,(float)rule.amount()));
            }
            case PROJECTILE -> projectile(owner,stack,rule);
            case REPAIR -> {if(owned(owner,physical)&&physical.isDamageableItem())physical.setDamageValue(Math.max(0,physical.getDamageValue()-(int)rule.amount()));}
            case MANA -> targets.stream().filter(ServerPlayer.class::isInstance).map(ServerPlayer.class::cast).forEach(p->game.items().mana().add(p,(int)rule.amount()));
            case FOOD -> targets.stream().filter(ServerPlayer.class::isInstance).map(ServerPlayer.class::cast).forEach(p->p.getFoodData().eat((int)rule.amount(),.2f));
            case CLEANSE -> targets.forEach(t->{for(var effect:List.copyOf(t.getActiveEffects()))if(!effect.getEffect().value().isBeneficial())t.removeEffect(effect.getEffect());});
            case SOUND -> positions.forEach(pos->level.playSound(null,pos.x,pos.y,pos.z,BuiltInRegistries.SOUND_EVENT.get(id(rule.id())).orElseThrow(),SoundSource.PLAYERS,1,1));
            case PARTICLES -> positions.forEach(pos->level.sendParticles((SimpleParticleType)BuiltInRegistries.PARTICLE_TYPE.get(id(rule.id())).orElseThrow().value(),pos.x,pos.y+.5,pos.z,Math.max(1,(int)rule.amount()),.4,.4,.4,.03));
            case BLOCK -> positions.forEach(pos->{int radius=(int)rule.radius();for(var cell:BlockPos.betweenClosed(BlockPos.containing(pos).offset(-radius,-radius,-radius),BlockPos.containing(pos).offset(radius,radius,radius))){
                if(!level.hasChunkAt(cell)||level.isOutsideBuildHeight(cell)||cell.distToCenterSqr(pos)>Math.pow(rule.radius()+.5,2))continue;
                var state=level.getBlockState(cell);if(!state.isAir()&&!state.hasBlockEntity()&&state.getDestroySpeed(level,cell)>=0)
                    GenieNative.write(game,owner,level,cell.immutable(),BuiltInRegistries.BLOCK.get(id(rule.id())).orElseThrow().value().defaultBlockState());
            }});
        }
    }
    /** Walk the short swept body corridor: no far32block endpoint and no clipping through a thin wall. */
    private static void teleport(ServerPlayer owner,double range){
        var start=owner.position();var look=owner.getLookAngle();Vec3 destination=start;
        for(double distance=Math.min(.125,range);distance<=range+.00001;distance=Math.min(range,distance+.125)){
            var next=start.add(look.scale(distance));var box=owner.getBoundingBox().move(destination.subtract(start)).expandTowards(next.subtract(destination));
            if(!owner.level().hasChunkAt(BlockPos.containing(next))||!owner.level().getWorldBorder().isWithinBounds(box)||!owner.level().noCollision(owner,box))break;
            destination=next;if(distance>=range)break;
        }
        if(!destination.equals(start))owner.teleportTo(destination.x,destination.y,destination.z);
    }
    private void projectile(ServerPlayer p,ItemStack source,Rule rule){
        Projectile shot;String kind=id(rule.id()).getPath();
        if(kind.equals("arrow")){var arrow=new Arrow(p.level(),p,new ItemStack(Items.ARROW),source.copyWithCount(1));arrow.setBaseDamage(Math.max(0,rule.amount())/2);arrow.pickup=Arrow.Pickup.DISALLOWED;shot=arrow;}
        else if(kind.equals("snowball"))shot=new Snowball(EntityTypes.SNOWBALL,p.level());
        else shot=new SmallFireball(p.level(),p,p.getLookAngle());
        shot.setOwner(p);shot.setPos(p.getEyePosition());var look=p.getLookAngle();shot.shoot(look.x,look.y,look.z,2,1);
        shot.entityTags().add(SHOT_POWER+rule.amount());shot.entityTags().add(SHOT_LIFE+Math.max(1,rule.ticks()==0?200:rule.ticks()));
        GenieNative.mark(game,shot);p.level().addFreshEntity(shot);loaded(shot);
    }
    private static boolean owned(ServerPlayer p,ItemStack item){
        if(item.isEmpty())return false;if(p.containerMenu.getCarried()==item)return true;
        for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i)==item)return true;
        return false;
    }
    private void drawback(ServerPlayer p,ItemStack item,Entity target,Twist twist){
        float power=(float)twist.power();switch(twist.kind()){
            case HEALTH_PRICE -> damage(p,p,2*power);case MANA_PRICE -> game.items().mana().add(p,-(int)(50*power));
            case HUNGER_PRICE -> p.getFoodData().setFoodLevel(Math.max(0,p.getFoodData().getFoodLevel()-(int)(2*power)));
            case RECOIL -> {var force=p.getLookAngle().scale(-.7*power);p.push(force.x,.3,force.z);p.syncVelocity=true;}
            case SELF_POISON -> p.addEffect(new MobEffectInstance(MobEffects.POISON,100,(int)power-1));
            case SELF_FIRE -> p.igniteForSeconds(3*power);case SELF_GLOW -> p.addEffect(new MobEffectInstance(MobEffects.GLOWING,200,(int)power-1));
            case SELF_LAUNCH -> {p.push(0,power,0);p.syncVelocity=true;}
            case SELF_TELEPORT -> {double x=p.getX()+(p.getRandom().nextDouble()*2-1)*4*power,z=p.getZ()+(p.getRandom().nextDouble()*2-1)*4*power;var move=new Vec3(x,p.getY(),z);if(p.level().hasChunkAt(BlockPos.containing(move))&&p.level().noCollision(p,p.getBoundingBox().move(move.subtract(p.position()))))p.teleportTo(x,p.getY(),z);}
            case SHUFFLE_POCKETS -> {int a=p.getRandom().nextInt(36),b=p.getRandom().nextInt(36);var first=p.getInventory().getItem(a);p.getInventory().setItem(a,p.getInventory().getItem(b));p.getInventory().setItem(b,first);p.inventoryMenu.broadcastChanges();}
            case DROP_ITEM -> {if(owned(p,item))p.drop(item.split(1),false,Prediction.SERVER_ONLY);}
            case BRITTLE -> {if(owned(p,item)){
                var slot=EquipmentSlot.MAINHAND;for(var candidate:EquipmentSlot.values())if(p.getItemBySlot(candidate)==item){slot=candidate;break;}
                if(item.isDamageableItem())item.hurtAndBreak(Math.max(1,item.getMaxDamage()/10),p,slot);else item.shrink(1);
            }}
            case HEAL_ENEMY -> {for(var living:p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(6),e->e!=p&&e.isAlive()))living.heal(3*power);}
            case CRUMBLE_FLOOR -> {var cell=p.blockPosition().below();var state=p.level().getBlockState(cell);if(!state.isAir()&&!state.hasBlockEntity()&&state.getDestroySpeed(p.level(),cell)>=0)GenieNative.write(game,p,p.level(),cell,Blocks.AIR.defaultBlockState());}
            case SLOW_OWNER -> p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,100,(int)power+1));

            case WET_ONLY,DRY_ONLY,NIGHT_ONLY,AIR_ONLY -> {}
        }
    }
    public void loaded(Entity entity){if(entity instanceof Projectile shot&&tagNumber(shot,SHOT_LIFE,-1)>0)projectiles.add(shot);}
    public void unloaded(Entity entity){projectiles.remove(entity);}
    private static double tagNumber(Entity entity,String prefix,double fallback){
        for(var tag:entity.entityTags())if(tag.startsWith(prefix))try{return Double.parseDouble(tag.substring(prefix.length()));}catch(NumberFormatException ignored){}
        return fallback;
    }
    public static float projectilePower(Projectile shot,float ordinary){double power=tagNumber(shot,SHOT_POWER,-1);return Double.isFinite(power)&&power>=0&&power<=16?(float)power:ordinary;}
    public void tick(){
        for(var shot:List.copyOf(projectiles)){
            if(shot.isRemoved()){projectiles.remove(shot);continue;}
            if(shot.tickCount>=tagNumber(shot,SHOT_LIFE,200)||shot.level().isOutsideBuildHeight(shot.blockPosition())){shot.discard();projectiles.remove(shot);}
        }
        cooldowns.replaceAll((key,remaining)->{var owner=game.server().getPlayerList().getPlayer(key.owner);return owner!=null&&GenieNative.stopped(game,owner)?remaining:remaining-1;});
        cooldowns.values().removeIf(value->value<=0);
    }
    public static final class Snapshot {
        private final Map<Key,Integer> cooldowns;
        private Snapshot(Map<Key,Integer> values){cooldowns=Map.copyOf(values);}
        public void restore(GenieRuntime runtime){runtime.cooldowns.clear();runtime.cooldowns.putAll(cooldowns);}
    }
    public Snapshot snapshot(){return new Snapshot(cooldowns);}
    public void forget(ServerPlayer p){cooldowns.keySet().removeIf(key->key.owner.equals(p.getUUID()));}
    public void clear(){cooldowns.clear();programs.clear();projectiles.clear();}
}
