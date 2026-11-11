package me.skywars2.game;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Input routing only. All costs, suppression, payloads and output factories remain native. */
public final class LbcTmtTriggers {
    private LbcTmtTriggers(){}
    static final String KEY="tmt_inputs",SCHEMA="tmt_input_schema";
    public enum Input { JUMP, RIGHT_CLICK, LEFT_CLICK, SNEAK, TICK, SHOT, EAT, THROW, SPRINT, SNEAK_PRESS, HIT, BLOCK_USE, HURT, MINING }
    enum Action { USE, SELF, MELEE, INCOMING, JUMP, SHOT, CONSUME, BLOCK, MINING }
    private record Selection(ItemStack source,Action action,Set<String> ids){}
    private static final ThreadLocal<Selection> selection=new ThreadLocal<>();
    private static final ThreadLocal<Integer> dispatchDepth=ThreadLocal.withInitial(()->0);
    private static final ThreadLocal<ServerPlayer> inputOwner=new ThreadLocal<>();
    private static final ThreadLocal<Input> origin=new ThreadLocal<>();
    private record HeldView(ServerPlayer owner,InteractionHand hand,ItemStack source){}
    private static final ThreadLocal<HeldView> heldView=new ThreadLocal<>();
    public static ItemStack readHeld(LivingEntity owner,net.minecraft.world.entity.EquipmentSlot slot){
        var view=heldView.get();if(view==null||view.owner!=owner)return null;
        var expected=view.hand==InteractionHand.MAIN_HAND?net.minecraft.world.entity.EquipmentSlot.MAINHAND:net.minecraft.world.entity.EquipmentSlot.OFFHAND;
        return slot==expected?view.source:null;
    }
    /** A final accepted shot/break can retire its physical carrier before continuation.
     * Expose its captured source to native readers only; never swap inventory slots.
     */
    static void captured(ServerPlayer owner,Input input,ItemStack source,InteractionHand hand,Entity target,
                         net.minecraft.core.BlockPos block,net.minecraft.world.level.block.state.BlockState before){
        if(!modern(source)||!owner.getItemInHand(hand).isEmpty()){input(owner,input,target,block,before);return;}
        var previous=heldView.get();heldView.set(new HeldView(owner,hand,source));
        try{input(owner,input,target,block,before);}finally{if(previous==null)heldView.remove();else heldView.set(previous);}
    }
    public static boolean suppressUseModifier(Entity entity){var selected=selection.get();return selected!=null&&selected.action==Action.USE&&inputOwner.get()==entity&&origin.get()!=Input.RIGHT_CLICK&&origin.get()!=Input.BLOCK_USE;}
    private record Stamp(UUID player,ItemStack stack,String property,Action action){}
    private static final Set<Stamp> dispatched=new HashSet<>();
    private static int tick=Integer.MIN_VALUE;
    static Map<Action,Input> parse(CustomData data){
        var result=new EnumMap<Action,Input>(Action.class);var root=data.copyTag().getCompoundOrEmpty(KEY);
        for(var action:Action.values())try{var value=root.getStringOr(action.name(),"");if(!value.isEmpty())result.put(action,Input.valueOf(value));}catch(IllegalArgumentException ignored){}
        return Map.copyOf(result);
    }

    static boolean bound(LbcTmtAbilities.Ability property){return ((ItemData.IntReader)(Object)property.data()).skywars2$contains(KEY);}
    public static boolean modern(ItemStack stack){return LbcTmtItems.matches(stack)&&ItemData.intValue(stack,SCHEMA,0)==1;}
    static List<Input> inputs(ItemStack carrier){
        var result=new ArrayList<>(List.of(Input.JUMP,Input.RIGHT_CLICK,Input.LEFT_CLICK,Input.SNEAK,Input.TICK,
                Input.SPRINT,Input.SNEAK_PRESS,Input.HIT,Input.BLOCK_USE,Input.HURT,Input.MINING));
        var type=LbcTmtItems.vanillaCarrier(carrier);
        if(type==Items.BOW||type==Items.CROSSBOW)result.add(Input.SHOT);
        if(type==Items.GOLDEN_APPLE)result.add(Input.EAT);
        if(type==Items.SNOWBALL)result.add(Input.THROW);
        return List.copyOf(result);
    }
    static EnumSet<Action> actions(LbcTmtCatalog.Family family){
        var result=EnumSet.noneOf(Action.class);
        if(family.id().startsWith("native/component/"))return result;
        for(var action:Action.values())if(family.events().contains(LbcTmtCatalog.Event.valueOf(action.name())))result.add(action);
        // Sustained use and equipped/inventory conditions stay with their native owners.
        // They are conditions, not replacement inputs or fake equip/consume/death events.
        if(family.events().contains(LbcTmtCatalog.Event.USING)){result.remove(Action.USE);result.remove(Action.SELF);}
        if(family.locations().contains(LbcTmtCatalog.Location.EQUIPPED)){result.remove(Action.JUMP);result.remove(Action.SELF);result.remove(Action.INCOMING);}
        for(var property:family.variants())if(flag(property,"feeding")||flag(property,"vampire")||flag(property,"seaguardian")||flag(property,"evolution")||flag(property,"triple_golden_sword")||flag(property,"blazefire_sword"))result.add(Action.MELEE);
        return result;
    }
    private static boolean flag(LbcTmtAbilities.Ability property,String key){return ((ItemData.IntReader)(Object)property.data()).skywars2$readInt(key,0)==1;}
    static boolean compatible(LbcTmtCatalog.Family family,ItemStack carrier){
        return !actions(family).contains(Action.SHOT)||inputs(carrier).contains(Input.SHOT);
    }
    static List<LbcTmtAbilities.Ability> assign(net.minecraft.util.RandomSource random,ItemStack carrier,LbcTmtCatalog catalog,List<LbcTmtAbilities.Ability> draws){
        var candidates=catalog.triggerCandidates(carrier);
        var permitted=inputs(carrier);var result=new ArrayList<LbcTmtAbilities.Ability>();
        for(var draw:draws){
            var family=catalog.family(draw.id());if(family==null)throw new IllegalStateException("Missing TMT family "+draw.id());
            if(!compatible(family,carrier)){family=candidates.get(random.nextInt(candidates.size()));draw=family.variants().get(random.nextInt(family.variants().size()));}
            var bindings=new CompoundTag();var remaining=new ArrayList<>(permitted);
            for(var action:actions(family))bindings.putString(action.name(),remaining.remove(random.nextInt(remaining.size())).name());
            if(!bindings.isEmpty()){var data=draw.data().copyTag();data.put(KEY,bindings);draw=new LbcTmtAbilities.Ability(draw.id(),CustomData.of(data));}
            result.add(draw);
        }
        return List.copyOf(result);
    }
    /** Structural serialization uses read(); native callbacks only see their selected private slots. */
    static List<LbcTmtAbilities.Ability> effective(ItemStack stack){
        var all=LbcTmtAbilities.read(stack);var selected=selection.get();
        String captured=ItemData.stringValue(stack,LbcTmtAbilities.ACTIVE,"");
        if(LbcTmtAbilities.active(stack)&&!captured.isEmpty())return all.stream().filter(a->a.id().equals(captured)).toList();
        if(selected!=null&&sameSource(stack,selected.source))return all.stream().filter(a->selected.ids.contains(a.id())).toList();
        if(!LbcTmtItems.matches(stack))return List.of();
        return ((LbcTmtAbilities.CachedSlots)(Object)stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY)).skywars2$tmtNaturalSlots();
    }
    static List<LbcTmtAbilities.Ability> effective(CompoundTag root){
        var all=LbcTmtAbilities.read(root);var selected=selection.get();
        if(selected!=null&&root.getIntOr("tmt_texture_seed",-1)==ItemData.intValue(selected.source,"tmt_texture_seed",-2))return all.stream().filter(a->selected.ids.contains(a.id())).toList();
        return all.stream().filter(a->!bound(a)).toList();
    }
    private static boolean sameSource(ItemStack first,ItemStack second){
        return first==second||LbcTmtItems.matches(first)&&LbcTmtItems.matches(second)
                &&ItemData.intValue(first,"tmt_texture_seed",-1)==ItemData.intValue(second,"tmt_texture_seed",-2);
    }
    private static <T>T selected(ItemStack source,Action action,Collection<LbcTmtAbilities.Ability> properties,Supplier<T> run){
        var before=selection.get();selection.set(new Selection(source,action,properties.stream().map(LbcTmtAbilities.Ability::id).collect(java.util.stream.Collectors.toUnmodifiableSet())));
        try{return run.get();}finally{if(before==null)selection.remove();else selection.set(before);}
    }
    /** The natural shot pipeline owns its one real volley, ammo debit and trinket roll. */
    static void shot(ItemStack fired,Runnable callback){
        var current=selection.get();
        if(current!=null&&current.action==Action.SHOT&&sameSource(fired,current.source)){callback.run();return;}
        var allowed=LbcTmtAbilities.read(fired).stream().filter(a->!bound(a)||matches(a,Action.SHOT,Input.SHOT)).toList();
        selected(fired,Action.SHOT,allowed,()->{callback.run();return null;});
    }
    private static boolean matches(LbcTmtAbilities.Ability ability,Action action,Input input){
        return ability.inputs().get(action)==input;
    }
    public static void input(ServerPlayer owner,Input input){input(owner,input,null,null,null);}
    public static void input(ServerPlayer owner,Input input,Entity target,net.minecraft.core.BlockPos block,
                             net.minecraft.world.level.block.state.BlockState before){
        var game=me.skywars2.SkyWars2.game();
        if(game==null||game.balanceLegacy()||dispatchDepth.get()>0||LbcModeScroll.scrolling()||!game.items().tmt().admitted(owner))return;
        int now=game.server().getTickCount();if(tick!=now){dispatched.clear();tick=now;}
        dispatchDepth.set(dispatchDepth.get()+1);
        inputOwner.set(owner);origin.set(input);
        try{
            for(var hand:InteractionHand.values()){
                var stack=owner.getItemInHand(hand);if(!modern(stack)||LbcTmtAbilities.active(stack)||!LbcDimensionItems.allowed(owner,stack))continue;
                var all=LbcTmtAbilities.read(stack);
                for(var action:Action.values()){
                    if(!game.items().tmt().admitted(owner))break;
                    if(hand==InteractionHand.OFF_HAND&&(action==Action.MELEE||action==Action.SELF))continue;
                    // The accepted-shot owner already handled this same native volley.
                    if(input==Input.SHOT&&action==Action.SHOT)continue;
                    var ready=new ArrayList<LbcTmtAbilities.Ability>();
                    for(var property:all)if(matches(property,action,input)&&dispatched.add(new Stamp(owner.getUUID(),stack,property.id(),action)))ready.add(property);
                    if(ready.isEmpty())continue;
                    if(stack.isEmpty()||owner.getItemInHand(hand)!=stack)break;
                    var chosenHand=hand;
                    selected(stack,action,ready,()->{
                        if(action==Action.SHOT){shoot(owner,chosenHand,stack);return null;}
                        for(var property:ready){
                            if(!game.items().tmt().admitted(owner))break;
                            if(stack.isEmpty()||owner.getItemInHand(chosenHand)!=stack)break;
                            selected(stack,action,List.of(property),()->{
                                if(action==Action.USE){game.items().wands().castTmtSpells(owner,chosenHand);game.items().tmt().useProfiles().use(owner,chosenHand);}
                                else if(action==Action.BLOCK){var hit=blockHit(owner);if(hit.getType()==HitResult.Type.BLOCK)LbcTmtBlockUses.use(game,new UseOnContext(owner,chosenHand,hit));}
                                else LbcTmtAbilities.invoke(stack,property.id(),()->{
                                    switch(action){
                                        case MELEE->{var victim=target instanceof LivingEntity living?living:aimed(owner);if(victim!=null){
                                            var damage=owner.damageSources().playerAttack(owner);
                                            game.items().tmtMelee(owner,victim,damage);
                                            game.items().reactiveArtifacts().propertyMelee(owner,victim,damage,property.data());
                                            game.items().incomingArtifacts().propertyMelee(owner,victim,property.data());
                                            game.items().reactiveTrinkets().propertyMelee(owner,victim,damage,property.data());
                                        }}
                                        case INCOMING->{
                                            game.items().incomingArtifacts().propertyIncoming(owner,target,property.data());
                                            game.items().reactiveTrinkets().propertyIncoming(owner,property.data());
                                            if(flag(property,"gaara_tikva"))game.items().gaaraPot().propertyIncoming(owner,stack);
                                        }
                                        case SELF->{game.items().classicMelee().sourceSelf(owner);game.items().chargedSwords().sourceSelf(owner);if(flag(property,"halloween_mace"))game.items().halloweenWind().jump(owner);}
                                        case JUMP->{game.items().jumpArtifacts().propertyJump(owner,property.data());if(chosenHand==InteractionHand.MAIN_HAND&&flag(property,"halloween_mace"))game.items().halloweenWind().jump(owner);}
                                        case CONSUME->{game.consumedProperties(owner,stack);LbcTmtManaPotions.consumed(game,owner,stack);}
                                        case MINING->{
                                            var hit=blockHit(owner);var pos=block!=null?block:hit.getType()==HitResult.Type.BLOCK?hit.getBlockPos():null;
                                            if(pos!=null){
                                                if(input==Input.MINING)LbcTmtTools.activate(game,owner,stack,pos,before!=null?before:owner.level().getBlockState(pos));
                                                else owner.gameMode.destroyBlock(pos); // Real native root break owns protection, wear and the existing tool continuation.
                                            }
                                        }
                                        default->{}
                                    }return true;
                                });return null;
                            });
                        }return null;
                    });
                }
            }
        }finally{inputOwner.remove();origin.remove();int next=dispatchDepth.get()-1;if(next==0)dispatchDepth.remove();else dispatchDepth.set(next);}
    }
    private static BlockHitResult blockHit(ServerPlayer owner){
        var from=owner.getEyePosition();double reach=Math.max(0,Math.min(64,owner.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE)));
        return owner.level().clip(new ClipContext(from,from.add(owner.getLookAngle().scale(reach)),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,owner));
    }
    private static LivingEntity aimed(ServerPlayer owner){
        var start=owner.getEyePosition();var end=start.add(owner.getLookAngle().scale(Math.min(64,owner.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE))));
        var wall=owner.level().clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,owner));if(wall.getType()==HitResult.Type.BLOCK)end=wall.getLocation();
        LivingEntity closest=null;double distance=Double.POSITIVE_INFINITY;
        for(var body:owner.level().getEntitiesOfClass(LivingEntity.class,new AABB(start,end).inflate(1),e->e!=owner&&e.isAlive())){
            var hit=body.getBoundingBox().clip(start,end);if(hit.isPresent()&&hit.get().distanceToSqr(start)<distance){distance=hit.get().distanceToSqr(start);closest=body;}
        }return closest;
    }
    private static void shoot(ServerPlayer owner,InteractionHand hand,ItemStack weapon){
        var type=LbcTmtItems.vanillaCarrier(weapon);
        if(type==Items.BOW)((BowItem)weapon.getItem()).releaseUsing(weapon,owner.level(),owner,weapon.getUseDuration(owner)-20);
        else if(type==Items.CROSSBOW&&weapon.has(DataComponents.CHARGED_PROJECTILES)&&!weapon.get(DataComponents.CHARGED_PROJECTILES).isEmpty())weapon.getItem().use(owner.level(),owner,hand);
    }
    static int shotCharge(int ordinary){var current=selection.get();return current!=null&&current.action==Action.SHOT&&origin.get()!=Input.SHOT?20:ordinary;}
    static void tick(GameController game){
        for(var owner:game.server().getPlayerList().getPlayers()){
            if(!game.items().tmt().admitted(owner)||game.items().timeStop().stopped(owner.level()))continue;
            boolean sneaking=owner.isShiftKeyDown();
            if(sneaking)input(owner,Input.SNEAK);if(owner.isSprinting())input(owner,Input.SPRINT);input(owner,Input.TICK);
        }
    }
    static void describe(GameController game,ItemStack stack){
        var lines=new ArrayList<>(stack.getOrDefault(DataComponents.LORE,ItemLore.EMPTY).lines());
        for(var property:LbcTmtAbilities.read(stack)){
            var source=LbcTmtComponents.source(game,property);
            var name=source.getOrDefault(DataComponents.CUSTOM_NAME,source.getOrDefault(DataComponents.ITEM_NAME,net.minecraft.network.chat.Component.literal(property.id())));
            for(var action:Action.values())if(property.inputs().containsKey(action))lines.add(net.minecraft.network.chat.Component.literal(label(property.inputs().get(action))+" → "+actionLabel(action)+": ")
                    .withStyle(net.minecraft.ChatFormatting.GRAY).append(name.copy()));
        }
        stack.set(DataComponents.LORE,new ItemLore(List.copyOf(lines)));
    }
    static boolean canCycle(GameController game,ItemStack stack){
        return LbcTmtAbilities.read(stack).stream().anyMatch(a->LbcModeScroll.supports(game,LbcTmtComponents.source(game,a)));
    }
    public static boolean tryCycle(ServerPlayer owner,ItemStack stack,InteractionHand hand){
        var game=me.skywars2.SkyWars2.game();
        if(game==null||!modern(stack)||!owner.isShiftKeyDown()||!game.items().tmt().admitted(owner)||!LbcDimensionItems.allowed(owner,stack)||!canCycle(game,stack))return false;
        cycle(game,owner,hand);return true;
    }
    static boolean cycle(GameController game,ServerPlayer owner,InteractionHand hand){
        var stack=owner.getItemInHand(hand);var modes=LbcTmtAbilities.read(stack).stream().filter(a->LbcModeScroll.supports(game,LbcTmtComponents.source(game,a))).toList();
        return selected(stack,Action.USE,modes,()->game.items().wands().castTmtSpells(owner,hand)+game.items().tmt().useProfiles().use(owner,hand))>0;
    }
    private static String label(Input input){return switch(input){
        case JUMP->"Прыжок";case RIGHT_CLICK->"ПКМ";case LEFT_CLICK->"ЛКМ";case SNEAK->"Удержание Shift";
        case TICK->"Каждый тик";case SHOT->"Выстрел";case EAT->"Съедание";case THROW->"Бросок снаряда";
        case SPRINT->"Спринт";case SNEAK_PRESS->"Нажатие Shift";case HIT->"Удар";case BLOCK_USE->"ПКМ по блоку";case HURT->"Получение урона";case MINING->"Добыча блока";
    };}
    private static String actionLabel(Action action){return switch(action){
        case USE->"применение";case SELF->"свойство меча";case MELEE->"эффект удара";case INCOMING->"ответ на урон";case JUMP->"эффект прыжка";
        case SHOT->"эффект выстрела";case CONSUME->"эффект еды";case BLOCK->"действие по блоку";case MINING->"эффект добычи";
    };}
    static void forget(ServerPlayer owner){dispatched.removeIf(s->s.player.equals(owner.getUUID()));}
    static void stop(){dispatched.clear();tick=Integer.MIN_VALUE;}
}
