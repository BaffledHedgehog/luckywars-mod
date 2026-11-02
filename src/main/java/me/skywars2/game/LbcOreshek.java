package me.skywars2.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

/** Original Oreshek consumer, independent return latch and real shared native rare-event producers. */
public final class LbcOreshek {
    private static final String LATCH="skywars2.oreshek.returned",ACTOR="skywars2.oreshek.actor";
    private static final String AUTHOR="skywars2.oreshek.author.",ORDER="skywars2.oreshek.order.";
    private static final CompoundTag PIGLIN=parse("""
{equipment:{head:{"id":"netherite_helmet","count":1,"components":{"equippable":{"slot":head,"asset_id":"lbc:zelensky"}}},chest:{"id":"netherite_chestplate","count":1,"components":{"equippable":{"slot":chest,"asset_id":"lbc:zelensky"}}},legs:{"id":"netherite_leggings","count":1,"components":{"equippable":{"slot":legs,"asset_id":"lbc:zelensky"}}},feet:{"id":"netherite_boots","count":1,"components":{"equippable":{"slot":feet,"asset_id":"lbc:zelensky"}}}},drop_chances:{head:0,chest:0,legs:0,feet:0},CustomName:[{"text":"Володiмiр","color":"aqua"},{"text":" Zelensky","color":"yellow"}],CustomNameVisible:true}
            """);
    public static final net.minecraft.world.item.Item ITEM=registerItem();
    public static void register(){}
    private final GameController game;
    private final Set<Entity> tracked=Collections.newSetFromMap(new IdentityHashMap<>());
    private ServerLevel executingLevel;private LbcIndirectKills.Owner executingCause;private Entity executingSource;
    private boolean deathNoticeSent;
    private long consumed,explosions,transfers,zelenskys;private Outcome last=Outcome.NONE;
    public enum Outcome { NONE, EXPLOSION, NEXUS, ZELENSKY }
    public LbcOreshek(GameController game){this.game=game;}
    private static CompoundTag parse(String value){try{return TagParser.parseCompoundFully(value);}
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new ExceptionInInitializerError(error);}}
    private static net.minecraft.world.item.Item registerItem(){
        var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.Identifier.parse("skywars2:oreshek"));
        var data=new CompoundTag();data.putInt("eat_oreshek",1);
        var props=new net.minecraft.world.item.Item.Properties().setId(key).stacksTo(1).overrideDescription("oreshek")
                .food(new net.minecraft.world.food.FoodProperties(0,0,true),net.minecraft.world.item.component.Consumable.builder()
                    .onConsume(new net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect(
                        new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INSTANT_HEALTH,1,2,false,false,false))).build())
                .component(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data))
                .component(net.minecraft.core.component.DataComponents.ITEM_MODEL,net.minecraft.resources.Identifier.parse("lbc:bob"))
                .component(net.minecraft.core.component.DataComponents.TOOLTIP_STYLE,net.minecraft.resources.Identifier.parse("lbc:tier_f"));
        // Read the canonical authored name/lore once at registration; every dispenser uses this same identity.
        try(var input=LbcOreshek.class.getResourceAsStream("/data/lbc/loot_table/item_defs/lbc/bob__dbf4db459264.json")){
            if(input==null)throw new IllegalStateException("Missing canonical nut definition");
            var root=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            var modifiers=root.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonArray("modifier");
            for(var modifier:modifiers){var row=modifier.getAsJsonObject();var type=row.get("type").getAsString();
                if(type.equals("minecraft:set_name"))props.component(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                    net.minecraft.network.chat.ComponentSerialization.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,row.get("name")).getOrThrow());
                if(type.equals("minecraft:set_lore")){var lore=new ArrayList<Component>();for(var line:row.getAsJsonArray("lore"))
                    lore.add(net.minecraft.network.chat.ComponentSerialization.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,line).getOrThrow());
                    props.component(net.minecraft.core.component.DataComponents.LORE,new net.minecraft.world.item.component.ItemLore(lore));}
            }
        }catch(java.io.IOException error){throw new IllegalStateException("Cannot read canonical nut definition",error);}
        return net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.ITEM,key,
                new eu.pb4.polymer.core.api.item.SimplePolymerItem(props,net.minecraft.world.item.Items.REPEATING_COMMAND_BLOCK,true));
    }
    public static boolean matches(ItemStack stack){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("eat_oreshek",0)==1;}
    private boolean admitted(ServerPlayer p){return p!=null&&game.playing()&&game.stage()!=GameController.Stage.ENDING
            &&p.isAlive()&&!p.isRemoved()&&p.level()==game.maps().level()&&LbcDimensionItems.ordinary(p)
            &&game.canUseGameplayItems(p)&&!game.isObserver(p)&&game.items().wands().validRoleTarget(p,false)
            &&!game.items().wands().timeStopped()&&!game.items().wands().inputTimeStopped(p)&&!game.items().timeStop().stopped(p.level());}
    /** Native consume owns the only debit. Notification state never blocks subsequent nuts. */
    public void consumed(ServerPlayer p,ItemStack stack){
        if(!matches(stack)||!admitted(p))return;
        // Old persistent return markers must never announce an unrelated later death/reset.
        p.entityTags().remove(LATCH);consumed++;last=Outcome.NONE;
        var level=p.level();var origin=p.position();var accepted=game.items().indirectKills().wildCause(p);
        game.items().legacyEffects().randomDebuff(p);
        if(level.getRandom().nextFloat()<.1f){last=Outcome.EXPLOSION;explode(p,level,origin,accepted);return;}
        LbcOverload.sound(level,origin,"minecraft:lbcsounds.ukraine",8,1,0,0);
        if(level.getRandom().nextFloat()<.01f){last=Outcome.NEXUS;transfers++;game.items().hedgehogDimensions().nexus(p,p,level.getRandom());return;}
        if(level.getRandom().nextFloat()<.0001f){last=Outcome.ZELENSKY;zelenskys++;zelensky(p,level,origin,accepted);return;}
        // Only the ordinary fallthrough revokes source eat_oreshek. Early paths keep its return/death marker.
        p.entityTags().remove(LATCH);
    }
    private void explode(ServerPlayer p,ServerLevel level,Vec3 origin,LbcIndirectKills.Owner accepted){
        var previousLevel=executingLevel;var previousCause=executingCause;var previousSource=executingSource;boolean previousNotice=deathNoticeSent;deathNoticeSent=false;
        final PrimedTnt[] direct={null};executingLevel=level;executingCause=accepted;
        try{game.items().exasperato().encounterBlast(p,level,origin,15,tnt->{direct[0]=tnt;stamp(tnt,accepted);executingSource=tnt;});explosions++;}
        finally{executingLevel=previousLevel;executingCause=previousCause;executingSource=previousSource;deathNoticeSent=previousNotice;if(direct[0]!=null)direct[0].discard();}
        LbcOverload.sound(level,origin,"minecraft:lbcsounds.donbass",4,1,0,0);
    }
    private void zelensky(ServerPlayer p,ServerLevel level,Vec3 origin,LbcIndirectKills.Owner accepted){
        game.items().nukeEvent().start(p,origin,true);
        var payload=PIGLIN.copy();payload.putString("id","minecraft:piglin");
        var piglin=EntityType.loadEntityRecursive(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),payload),level,EntitySpawnReason.COMMAND,
                entity->{entity.snapTo(origin.x,origin.y,origin.z,0,0);stamp(entity,accepted);return entity;});
        if(piglin!=null&&!level.tryAddFreshEntityWithPassengers(piglin))piglin.getSelfAndPassengers().forEach(entity->{tracked.remove(entity);entity.discard();});
        // Shared bomber's native spawn formula adds90 degrees; compensate to retain source relative45/-45/135/-135.
        for(int relative:new int[]{45,-45,135,-135})game.items().droneDrops().spawnBomber(p,origin,p.getYRot()+relative-90);
    }
    private void stamp(Entity entity,LbcIndirectKills.Owner accepted){
        entity.entityTags().removeIf(t->t.startsWith(AUTHOR)||t.startsWith(ORDER));
        entity.entityTags().addAll(List.of(ACTOR,AUTHOR+accepted.player(),ORDER+accepted.order()));game.items().markCurrentItemEpoch(entity);
    }
    public static boolean managed(Entity entity){return entity!=null&&entity.entityTags().contains(ACTOR);}
    public static boolean wild(Entity entity){return managed(entity);}
    /** Immutable lineage on the actual entity also works after its native unpublished TNT source was discarded. */
    public LbcIndirectKills.Owner cause(Entity entity){
        if(!managed(entity))return null;UUID owner=null;long order=0;
        for(var tag:entity.entityTags())try{if(tag.startsWith(AUTHOR))owner=UUID.fromString(tag.substring(AUTHOR.length()));
            else if(tag.startsWith(ORDER))order=Long.parseLong(tag.substring(ORDER.length()));}catch(IllegalArgumentException ignored){}
        return owner==null||order<=0?null:new LbcIndirectKills.Owner(owner,order,true,false);
    }
    public ServerPlayer owner(Entity source){var accepted=cause(source);return accepted==null?null:game.server().getPlayerList().getPlayer(accepted.player());}
    public boolean canAffect(Entity source,Entity target){return target!=null&&cause(source)!=null&&game.playing()&&game.stage()!=GameController.Stage.ENDING
            &&game.items().currentEpoch(source)&&game.items().wands().validRoleTarget(target,false)&&!LbcGreed.active(target);}
    public boolean canTarget(Entity source,Entity target){return canAffect(source,target);}
    public void inherit(Entity parent,Entity child){var accepted=cause(parent);if(accepted!=null&&game.items().currentEpoch(parent))stamp(child,accepted);}
    public void inheritChild(Entity child){if(executingCause!=null&&child.level()==executingLevel)stamp(child,executingCause);}
    public boolean executing(ServerLevel level){return executingCause!=null&&executingLevel==level;}
    public Entity currentEmitter(){return executingSource;}
    public void loaded(Entity entity){
        if(!managed(entity))return;
        if(!game.playing()||game.stage()==GameController.Stage.ENDING||!game.items().currentEpoch(entity)||cause(entity)==null){entity.discard();return;}
        tracked.add(entity);game.items().timeStop().prepareFrozen(entity);
    }
    public void unloaded(Entity entity){tracked.remove(entity);}
    /** Physical accepted children follow their own native lifetime and do not vanish when their author forgets an item. */
    public void forget(ServerPlayer player){}
    public void tick(){for(var entity:List.copyOf(tracked))if(entity.isRemoved())tracked.remove(entity);
        else if(!game.playing()||game.stage()==GameController.Stage.ENDING||!game.items().currentEpoch(entity)){tracked.remove(entity);entity.discard();}}
    public void stop(){for(var entity:List.copyOf(tracked))entity.discard();tracked.clear();}
    public boolean pendingDeathMessage(ServerPlayer p){return p!=null&&!deathNoticeSent&&executingCause!=null
            &&executingSource!=null&&executingLevel==p.level()&&executingCause.player().equals(p.getUUID());}
    public boolean died(ServerPlayer p){return died(p,p.getLastDamageSource());}
    /** Only an actual death in this nut's synchronous native explosion may broadcast the symbol. */
    public boolean died(ServerPlayer p,net.minecraft.world.damagesource.DamageSource damage){
        p.entityTags().remove(LATCH);
        if(!pendingDeathMessage(p)||damage==null||damage.getDirectEntity()!=executingSource)return false;
        deathNoticeSent=true;
        for(var viewer:game.server().getPlayerList().getPlayers())viewer.sendSystemMessage(Component.literal("\uE202"));
        return true;
    }
    public boolean resetBoundary(ServerPlayer p){p.entityTags().remove(LATCH);return false;}
    public List<Entity> snapshot(){return List.copyOf(tracked);}
    public long consumedCount(){return consumed;}public long explosionCount(){return explosions;}public long transferCount(){return transfers;}
    public long zelenskyCount(){return zelenskys;}public Outcome lastOutcome(){return last;}
    /** Exact source payload oracle for native resource/component verification, independent of added epoch/author tags. */
    public static CompoundTag piglinPayload(){return PIGLIN.copy();}
}
