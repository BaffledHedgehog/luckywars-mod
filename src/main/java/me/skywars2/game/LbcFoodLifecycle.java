package me.skywars2.game;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

/** Missing native consume criteria and the original one-tick infinite-stew helper. */
public final class LbcFoodLifecycle {
    private static final String PUMPKIN="skywars2.food_lifecycle.pumpkin",CHILD="skywars2.food_lifecycle.child";
    private static final Identifier HALLOWEEN=Identifier.parse("lbc:true_advancements/event/eat_halloween_food");
    private static final Set<Item> ALL_FOOD=Set.of(Items.APPLE,Items.GOLDEN_APPLE,Items.ENCHANTED_GOLDEN_APPLE,
            Items.MELON_SLICE,Items.SWEET_BERRIES,Items.GLOW_BERRIES,Items.CHORUS_FRUIT,Items.CARROT,Items.GOLDEN_CARROT,
            Items.POTATO,Items.BAKED_POTATO,Items.POISONOUS_POTATO,Items.BEETROOT,Items.DRIED_KELP,
            Items.BEEF,Items.COOKED_BEEF,Items.PORKCHOP,Items.COOKED_PORKCHOP,Items.MUTTON,Items.COOKED_MUTTON,
            Items.CHICKEN,Items.COOKED_CHICKEN,Items.RABBIT,Items.COOKED_RABBIT,Items.COD,Items.COOKED_COD,
            Items.SALMON,Items.COOKED_SALMON,Items.TROPICAL_FISH,Items.PUFFERFISH,Items.BREAD,Items.COOKIE,
            Items.PUMPKIN_PIE,Items.ROTTEN_FLESH,Items.SPIDER_EYE,Items.MUSHROOM_STEW,Items.BEETROOT_SOUP,
            Items.RABBIT_STEW,Items.SUSPICIOUS_STEW,Items.MILK_BUCKET,Items.HONEY_BOTTLE);
    private static final List<Holder<MobEffect>> BAD_APPLE=List.of(MobEffects.ABSORPTION,MobEffects.CONDUIT_POWER,
            MobEffects.FIRE_RESISTANCE,MobEffects.HASTE,MobEffects.HEALTH_BOOST,MobEffects.INSTANT_HEALTH,
            MobEffects.INVISIBILITY,MobEffects.NIGHT_VISION,MobEffects.REGENERATION,MobEffects.RESISTANCE,
            MobEffects.SATURATION,MobEffects.SLOW_FALLING,MobEffects.SPEED,MobEffects.STRENGTH,MobEffects.WATER_BREATHING);
    private record Accepted(LbcIndirectKills.Owner cause,String epoch){}
    /** Keep attribution on a removed native direct source while a TimeStop DamageSource still refers to it. */
    private static final class Pumpkin extends FallingBlockEntity {
        final Accepted accepted;
        Pumpkin(ServerLevel level,Accepted accepted){super(EntityTypes.FALLING_BLOCK,level);this.accepted=accepted;}
    }
    private final GameController game;
    private final Map<Entity,Accepted> actors=new IdentityHashMap<>();
    private final Map<Entity,Accepted> causes=new WeakHashMap<>();
    private final Map<UUID,Accepted> unloaded=new HashMap<>();
    private final Set<Entity> publishing=Collections.newSetFromMap(new IdentityHashMap<>());
    private record Eating(String epoch,InteractionHand hand){}
    private final Map<UUID,Eating> scheduled=new LinkedHashMap<>();
    private final Map<UUID,Integer> halloweenTicks=new HashMap<>();
    private int lastTick=Integer.MIN_VALUE,scheduledTick=Integer.MIN_VALUE;
    private Accepted executing;private ServerLevel executingLevel;
    public LbcFoodLifecycle(GameController game){this.game=game;}

    /** Exact source tag membership, including only the two existing registered source-carrier aliases. */
    public static boolean countsAsFood(ItemStack stack){return !stack.isEmpty()&&(ALL_FOOD.contains(stack.getItem())
            ||stack.is(LbcKfcFood.KFC_CHICK)||stack.is(LbcKfcFood.GOVNO_EATEN));}
    public static int sourceFoodCount(){return ALL_FOOD.size();}
    public static boolean matches(ItemStack stack){if(countsAsFood(stack))return true;var d=LbcFartus.data(stack);return !stack.isEmpty()
            &&(d.getIntOr("inf",0)==1||d.getIntOr("inf_old",0)==1||d.getIntOr("halloween_food",0)==1
                ||d.getIntOr("halloween_chorus",0)==1||d.getIntOr("bad_apple",0)==1);}
    private boolean admitted(ServerPlayer p){return p!=null&&game.playing()&&game.stage()!=GameController.Stage.ENDING
            &&p.isAlive()&&!p.isRemoved()&&p.level()==game.maps().level()&&LbcDimensionItems.ordinary(p)
            &&game.canUseGameplayItems(p)&&!game.isObserver(p)&&game.items().wands().validRoleTarget(p,false)
            &&!game.items().wands().timeStopped()&&!game.items().wands().inputTimeStopped(p)
            &&!game.items().timeStop().stopped(p.level());}
    /** Native callback boundary before native debit: this service never consumes or shrinks any stack. */
    public void consumed(ServerPlayer p,ItemStack stack){
        if(stack.isEmpty()||!admitted(p)||!matches(stack))return;
        var data=LbcFartus.data(stack);
        if(countsAsFood(stack))game.items().mana().score(p,"food_count").add(1);
        var hand=p.getUsedItemHand();
        if(data.getIntOr("inf",0)==1)p.setItemInHand(hand,infiniteSteak());
        if(data.getIntOr("inf_old",0)==1){p.entityTags().add("scheduled_eating");scheduled.put(p.getUUID(),new Eating(game.items().epoch(),hand));scheduledTick=game.server().getTickCount();}
        if(data.getIntOr("halloween_food",0)==1)halloween(p);
        if(data.getIntOr("halloween_chorus",0)==1)chorus(p);
        if(data.getIntOr("bad_apple",0)==1)badApple(p);
    }
    public static ItemStack infiniteSteak(){
        var stack=new ItemStack(Items.COOKED_BEEF);presentation(stack,Component.translatable("infinite_steak"),Component.translatable("infinite_steak.lore"));
        var data=new CompoundTag();data.putInt("inf",1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
        stack.set(DataComponents.TOOLTIP_STYLE,Identifier.parse("lbc:tier_c"));return stack;
    }
    public static ItemStack oldInfiniteStew(){
        var stack=new ItemStack(Items.RABBIT_STEW);presentation(stack,Component.translatable("horn"),Component.translatable("horn.lore"));
        var data=new CompoundTag();data.putInt("inf_old",1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return stack;
    }
    private static void presentation(ItemStack stack,Component name,Component lore){
        stack.set(DataComponents.CUSTOM_NAME,name.copy().withStyle(ChatFormatting.WHITE).withStyle(s->s.withItalic(false)));
        stack.set(DataComponents.LORE,new ItemLore(List.of(lore.copy().withStyle(ChatFormatting.WHITE).withStyle(s->s.withItalic(false)))));
    }
    private void halloween(ServerPlayer p){
        int now=game.server().getTickCount();if(Objects.equals(halloweenTicks.put(p.getUUID(),now),now))return;
        var score=game.items().mana().score(p,"pumpkin_counter");score.add(1);int count=score.get();
        if(count<1||count>100)return;
        var advancement=game.server().getAdvancements().get(HALLOWEEN);
        if(advancement!=null){
            boolean before=p.getAdvancements().getOrStartProgress(advancement).isDone();
            p.getAdvancements().award(advancement,Integer.toString(count));
            if(!before&&p.getAdvancements().getOrStartProgress(advancement).isDone()){
                // Original default_kit reward is only this title pair; it never grants an inventory kit.
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal(" ")));
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.translatable("new_swrg_kit")));
            }
        }
    }
    private void badApple(ServerPlayer p){
        for(var effect:BAD_APPLE){int duration=effect.is(MobEffects.INSTANT_HEALTH)?30:300;
            int ticks=effect.value().isInstantaneous()?duration:duration*20;
            p.addEffect(ItemEffectLevels.create(effect,ticks,0),p);
        }
    }
    private void chorus(ServerPlayer p){
        var level=p.level();var start=p.position();var accepted=new Accepted(game.items().indirectKills().wildCause(p),game.items().epoch());
        LbcOverload.sound(p,start,"minecraft:entity.zombie.break_wooden_door",1,1,0);
        var pumpkins=new ArrayList<Pumpkin>(20);
        for(int n=0;n<20;n++){
            var actor=new Pumpkin(level,accepted);
            try{actor.load(TagValueInput.create(ProblemReporter.DISCARDING,actor.registryAccess(),TagParser.parseCompoundFully(
                    "{BlockState:'minecraft:pumpkin',HurtEntities:true,FallHurtAmount:5.0f,FallHurtMax:10,Tags:['pumpkinfall']}")));}
            catch(CommandSyntaxException error){throw new IllegalStateException("Frozen Halloween pumpkin source",error);}
            actor.setPos(start.add(0,.5,0));actor.entityTags().add(PUMPKIN);actors.put(actor,accepted);causes.put(actor,accepted);
            game.items().markCurrentItemEpoch(actor);publishing.add(actor);
            try{if(level.addFreshEntity(actor)&&!actor.isRemoved()){pumpkins.add(actor);game.items().timeStop().prepareFrozen(actor);}else actors.remove(actor);}
            finally{publishing.remove(actor);}
        }
        // Source summons all twenty before drawing each actor's X,Y,Z motion, using the native world RNG.
        for(var actor:pumpkins)actor.setDeltaMovement(new Vec3((level.getRandom().nextInt(101)-50)*.01,
                (level.getRandom().nextInt(101)+50)*.01,(level.getRandom().nextInt(101)-50)*.01));
        particles(p,start);spread(p,start);particles(p,p.position());
    }
    private void particles(ServerPlayer p,Vec3 at){p.level().sendParticles(new ItemParticleOption(ParticleTypes.ITEM,ItemStackTemplate.fromNonEmptyStack(new ItemStack(Items.PUMPKIN))),at.x,at.y+1,at.z,150,.3,.6,.3,0);}
    private void spread(ServerPlayer p,Vec3 at){
        var source=game.server().createCommandSourceStack().withSource(CommandSource.NULL).withLevel(p.level()).withEntity(p)
                .withPosition(at).withRotation(p.getRotationVector()).withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput();
        try{game.server().getCommands().getDispatcher().execute("spreadplayers "+at.x+" "+at.z+" 0 10 false @s",source);}
        catch(CommandSyntaxException nativeSourceFailure){/* A native failed spread does not undo the twenty accepted pumpkins. */}
    }
    public static boolean managed(Entity actor){return actor!=null&&(actor.entityTags().contains(PUMPKIN)||actor.entityTags().contains(CHILD));}
    public static boolean wild(Entity actor){return managed(actor);}
    private Accepted accepted(Entity actor){return actor instanceof Pumpkin pumpkin?pumpkin.accepted:causes.get(actor);}
    public LbcIndirectKills.Owner cause(Entity actor){var accepted=accepted(actor);return accepted==null?null:accepted.cause;}
    public ServerPlayer owner(Entity actor){var accepted=accepted(actor);return accepted==null?null:game.server().getPlayerList().getPlayer(accepted.cause.player());}
    private boolean live(Accepted accepted){return accepted!=null&&game.playing()&&game.stage()!=GameController.Stage.ENDING&&game.items().currentEpoch(accepted.epoch);}
    public boolean canAffect(Entity actor,Entity target){return live(accepted(actor))&&game.items().wands().validRoleTarget(target,false)&&!LbcGreed.active(target);}
    /** Wrap the real falling-block tick, not a predicted landing or manufactured damage event. */
    public void withTick(FallingBlockEntity actor,Runnable nativeTick){
        var accepted=accepted(actor);if(!live(accepted)){actor.discard();return;}
        if(game.items().timeStop().stopped((ServerLevel)actor.level())||actor.entityTags().contains("time_stopped"))return;
        var previous=executing;var previousLevel=executingLevel;executing=accepted;executingLevel=(ServerLevel)actor.level();
        try{game.items().indirectKills().trapChanges(executingLevel,accepted.cause,nativeTick);}
        finally{executing=previous;executingLevel=previousLevel;}
    }
    /** Native dropped pumpkin items inherit only while their real parent tick is running. */
    public void inheritChild(Entity child){
        if(!(child instanceof ItemEntity item)||executing==null||child.level()!=executingLevel||!live(executing))return;
        item.entityTags().add(CHILD);actors.put(item,executing);causes.put(item,executing);game.items().markCurrentItemEpoch(item);
        var author=game.server().getPlayerList().getPlayer(executing.cause.player());if(author!=null)item.setThrower(author);
        game.items().timeStop().prepareFrozen(item);
    }
    public void inheritConversion(Entity parent,Entity child){
        var accepted=accepted(parent);if(!live(accepted)||child==null)return;
        child.entityTags().add(child instanceof FallingBlockEntity?PUMPKIN:CHILD);actors.put(child,accepted);causes.put(child,accepted);
        game.items().markCurrentItemEpoch(child);game.items().timeStop().prepareFrozen(child);
    }
    /** One call per native game tick; original schedule replacement is global to the one helper. */
    public void tick(){
        int now=game.server().getTickCount();if(now==lastTick||!game.server().tickRateManager().runsNormally())return;lastTick=now;
        if(!game.playing()||game.stage()==GameController.Stage.ENDING){stop();return;}
        for(var actor:List.copyOf(actors.keySet()))if(actor.isRemoved()||!live(accepted(actor))){actors.remove(actor);if(!actor.isRemoved())actor.discard();}
        if(scheduled.isEmpty()||now<=scheduledTick||game.items().wands().timeStopped())return;
        var due=new LinkedHashMap<>(scheduled);scheduled.clear();scheduledTick=Integer.MIN_VALUE;
        for(var entry:due.entrySet()){
            var player=game.server().getPlayerList().getPlayer(entry.getKey());if(player==null)continue;
            if(game.items().polymorph().masked(player)){scheduled.put(entry.getKey(),entry.getValue());scheduledTick=now;continue;}
            if(game.items().currentEpoch(entry.getValue().epoch())
                    &&(game.items().timeStop().stopped(player.level())||game.items().wands().timeStopped()||game.items().wands().inputTimeStopped(player))){
                scheduled.put(entry.getKey(),entry.getValue());scheduledTick=now;continue;
            }
            if(player.entityTags().contains("scheduled_eating")&&game.items().currentEpoch(entry.getValue().epoch())&&admitted(player))
                player.setItemInHand(entry.getValue().hand(),oldInfiniteStew());
            player.entityTags().remove("scheduled_eating");
        }
    }
    public void loaded(Entity actor){
        if(!managed(actor)||publishing.contains(actor)||actors.containsKey(actor))return;
        var accepted=unloaded.remove(actor.getUUID());if(!live(accepted)||!game.items().currentEpoch(actor)){actor.discard();return;}
        actors.put(actor,accepted);causes.put(actor,accepted);game.items().timeStop().prepareFrozen(actor);
    }
    public void unloaded(Entity actor){
        var accepted=actors.remove(actor);
        if(accepted!=null&&actor.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK)unloaded.put(actor.getUUID(),accepted);
    }
    /** Accepted falling-block consequences retain their author across death/disconnect. */
    public void forget(ServerPlayer player){scheduled.remove(player.getUUID());halloweenTicks.remove(player.getUUID());player.entityTags().remove("scheduled_eating");}
    public void stop(){
        for(var id:scheduled.keySet()){var p=game.server().getPlayerList().getPlayer(id);if(p!=null)p.entityTags().remove("scheduled_eating");}
        scheduled.clear();halloweenTicks.clear();scheduledTick=Integer.MIN_VALUE;for(var actor:List.copyOf(actors.keySet()))actor.discard();actors.clear();causes.clear();unloaded.clear();
    }
    public int pendingCount(){return scheduled.size();}
    public int actorCount(){return actors.size();}
    public List<Entity> snapshot(){return List.copyOf(actors.keySet());}
}
