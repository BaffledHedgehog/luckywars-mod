package me.skywars2.game.items.wands;

import java.util.*;
import java.util.function.Predicate;
import me.skywars2.game.GameController;
import me.skywars2.game.combat.LbcDamage;
import me.skywars2.game.effects.ItemEffectLevels;
import me.skywars2.game.effects.magic.LbcOverload;
import me.skywars2.game.entities.LbcDimensionItems;
import me.skywars2.game.items.ItemData;
import me.skywars2.game.items.gadgets.LbcImbaPlaceableItems;
import me.skywars2.game.items.tools.LbcCraftingRecipe;
import me.skywars2.mixin.loot.TinyWandItemAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.phys.Vec3;

/** Tiny catalysts: local once-per-second ground reactions and native passive crafting-grid actions. */
public final class LbcTinyWandInteractions {
    public static final String AIR_COPY="skywars2.tiny_air.copy";
    private enum Tiny {
        RUBY(LbcWands.TINY_RUBY,"struby_tiny",LbcCraftingRecipe.Kind.RUBY),
        TERRA(LbcWands.TINY_TERRA,"stterra_tiny",LbcCraftingRecipe.Kind.TERRA),
        ELECTRO(LbcWands.TINY_ELECTRO,"stelectro_tiny",LbcCraftingRecipe.Kind.ELECTRO),
        AIR(LbcWands.TINY_AIR,"stair_tiny",LbcCraftingRecipe.Kind.AIR),
        ACID(LbcWands.TINY_ACID,"stacid_tiny",LbcCraftingRecipe.Kind.ACID),
        TELEPORT(LbcWands.TINY_TELEPORT,"stteleport_tiny",LbcCraftingRecipe.Kind.TELEPORT),
        STONE(LbcWands.TINY_STONE,"ststone_tiny",LbcCraftingRecipe.Kind.STONE),
        FIRE(LbcWands.TINY_FIRE,"stfire_tiny",LbcCraftingRecipe.Kind.FIRE),
        BLIND(LbcWands.TINY_BLIND,"stblind_tiny",LbcCraftingRecipe.Kind.BLIND);
        final Item item;final String flag;final LbcCraftingRecipe.Kind recipe;
        Tiny(Item item,String flag,LbcCraftingRecipe.Kind recipe){this.item=item;this.flag=flag;this.recipe=recipe;}
        boolean matches(ItemStack stack){return !stack.isEmpty()&&(stack.is(item)||ItemData.intValue(stack,flag,0)==1);}
    }
    private final GameController game;
    private final Map<UUID,ItemEntity> catalysts=new LinkedHashMap<>();
    private int phase;
    public LbcTinyWandInteractions(GameController game){this.game=game;}
    private static Tiny tiny(ItemStack stack){for(var type:Tiny.values())if(type.matches(stack))return type;return null;}
    public void loaded(Entity entity){if(entity instanceof ItemEntity item)itemChanged(item);}
    public void unloaded(Entity entity){catalysts.remove(entity.getUUID());}
    public void itemChanged(ItemEntity item){if(tiny(item.getItem())!=null)catalysts.put(item.getUUID(),item);else catalysts.remove(item.getUUID());}
    public void stop(){catalysts.clear();phase=0;}
    /** Frozen body replay suppresses ENTITY_LOAD; rebuild only this derived index. */
    public void reindexRestored(Collection<? extends Entity> bodies){
        if(!game.server().isSameThread()||!game.server().tickRateManager().isFrozen())
            throw new IllegalStateException("Reindex Tiny catalysts on the frozen native thread");
        stop();var level=game.maps().level();
        if(level==null||!game.playing())return;
        for(var body:bodies)if(body instanceof ItemEntity item&&item.isAlive()&&!item.isRemoved()
                &&LbcDimensionItems.crafting(game,item.level())&&((ServerLevel)item.level()).getEntity(item.getUUID())==item&&!game.items().hasStaleItemEpoch(item))itemChanged(item);
    }
    private boolean eligible(ItemEntity item){return item.isAlive()&&!item.isRemoved()&&!item.getItem().isEmpty()
            &&LbcDimensionItems.crafting(game,item.level())&&!game.items().hasStaleItemEpoch(item)
            &&!game.items().timeStop().stopped((ServerLevel)item.level())&&!game.items().timeStop().frozen(item);}
    private static boolean thrown(ItemEntity item){return ((TinyWandItemAccess)item).skywars2$thrower()!=null;}
    public void tick(){
        var level=game.maps().level();
        if(level==null||!game.playing()||game.stage()==GameController.Stage.ENDING||game.supernovaMapTransition()
                ||game.items().wands().timeStopped())return;
        for(var player:game.server().getPlayerList().getPlayers())if(player.isAlive()&&!game.isObserver(player)&&game.canUseGameplayItems(player)
                &&LbcDimensionItems.crafting(game,player.level())&&!game.items().timeStop().stopped(player.level())
                &&!game.items().timeStop().frozen(player))crafting(player);
        if(++phase<20)return;phase=0;
        for(var item:List.copyOf(catalysts.values())){
            if(!eligible(item)){if(item.isRemoved()||item.getItem().isEmpty())catalysts.remove(item.getUUID());continue;}
            react(item);
        }
    }
    private List<ItemEntity> neighbours(ItemEntity item){
        return ((ServerLevel)item.level()).getEntitiesOfClass(ItemEntity.class,item.getBoundingBox().inflate(1),other->
                other!=item&&eligible(other)&&other.position().distanceToSqr(item.position())>1e-8
                        &&other.position().distanceToSqr(item.position())<=1);
    }
    private static ItemEntity first(List<ItemEntity> items,Predicate<ItemStack> condition){
        for(var item:items)if(item.isAlive()&&condition.test(item.getItem()))return item;return null;
    }
    private void react(ItemEntity catalyst){
        var type=tiny(catalyst.getItem());if(type==null)return;
        var level=(ServerLevel)catalyst.level();var at=catalyst.position();var nearby=neighbours(catalyst);
        if(type==Tiny.RUBY){var spell=first(nearby,s->ItemData.intValue(s,"libspell",0)==1);if(spell!=null){
            var output=loot(level,at,"lbc:transformables_spells");if(!output.isEmpty()){debit(spell,1);publish(catalyst,output,false);effects(level,at,false,100);}return;}}
        // Native core merging can combine two catalysts before a second entity is published.
        var partner=catalyst.getItem().getCount()>=2?catalyst:first(nearby,type::matches);
        if(partner!=null){
            var input=CraftingInput.of(2,1,List.of(catalyst.getItem().copyWithCount(1),partner.getItem().copyWithCount(1)));
            var result=type.recipe.recipe.assemble(input);
            if(!result.isEmpty()){
                if(partner==catalyst)debit(catalyst,2);else{debit(catalyst,1);debit(partner,1);}
                publish(level,at,result,catalyst.getOwner(),false);effects(level,at,false,100);
            }
            return;
        }
        switch(type){
            case TERRA->{
                if(nearby.stream().noneMatch(LbcTinyWandInteractions::thrown))return;
                for(var target:nearby)if(target.isAlive())target.setItem(new ItemStack(Items.SPRUCE_LEAVES,target.getItem().getCount()));
            }
            case ELECTRO->{
                var donor=first(nearby,s->s.is(Items.IRON_SWORD)||s.is(Items.SHIELD)&&ItemData.intValue(s,"energy_shield",0)!=1);if(donor==null)return;
                var table=donor.getItem().is(Items.SHIELD)?"electro_shield":"cyberkatana";
                var output=loot(level,at,"lbc:ported/"+table);if(output.isEmpty())return;
                debit(donor,1);if(level.getRandom().nextFloat()<.4f)debit(catalyst,1);
                publish(level,at,output,catalyst.getOwner(),false);effects(level,at,false,100);
                // Bow and four-piece iron recipes retain their existing dedicated native services.
            }
            case AIR->{
                if(nearby.stream().noneMatch(LbcTinyWandInteractions::thrown))return;
                var donor=nearby.stream().filter(ItemEntity::isAlive).min(Comparator.comparingDouble(e->e.distanceToSqr(at))).orElse(null);
                if(donor==null)return;var stack=donor.getItem().copy();var owner=catalyst.getOwner();
                debit(catalyst,1);
                for(int i=0;i<40;i++)publish(level,at,stack.copy(),owner,true);
                var tnt=new PrimedTnt(level,at.x,at.y,at.z,owner instanceof net.minecraft.world.entity.LivingEntity living?living:null);
                tnt.setFuse(0);tnt.entityTags().add(LbcDamage.UNPROTECTED_TNT);game.items().markCurrentItemEpoch(tnt);level.addFreshEntity(tnt);
            }
            case ACID->{
                var donor=first(nearby,s->!type.matches(s)&&!LbcImbaPlaceableItems.tagged(s,"corrupted_dust"));
                if(donor==null||nearby.stream().noneMatch(LbcTinyWandInteractions::thrown))return;
                debit(donor,1);if(level.getRandom().nextFloat()<.2f)publish(catalyst,loot(level,at,"lbc:corrupted_dust"),false);
                effects(level,at,false,10);
            }
            case TELEPORT->{
                var donor=first(nearby,s->!type.matches(s)&&ItemData.intValue(s,"transformable",0)==1);
                if(donor==null||!thrown(donor))return;var tables=LbcBolts.reforgeTables(donor.getItem().copyWithCount(1),true);
                if(tables.isEmpty())return;var output=game.items().bolts().rollReforge(donor,tables);if(output.isEmpty())return;
                debit(donor,1);if(level.getRandom().nextFloat()<.3f)debit(catalyst,1);
                publish(level,at,output,catalyst.getOwner(),false);effects(level,at,false,100);
            }
            case STONE->{
                var donor=first(nearby,s->!s.has(DataComponents.UNBREAKABLE));if(donor==null)return;
                var stack=donor.getItem().copy();stack.set(DataComponents.UNBREAKABLE,Unit.INSTANCE);donor.setItem(stack);
            }
            case FIRE->{
                var donor=first(nearby,s->!type.matches(s));if(donor==null||nearby.stream().noneMatch(LbcTinyWandInteractions::thrown))return;
                var player=level.players().stream().filter(p->p.isAlive()&&!p.isSpectator()&&!game.isObserver(p))
                        .min(Comparator.comparingDouble(p->p.distanceToSqr(at))).orElse(null);
                debit(donor,1);if(player!=null)fireReward(player,level.getRandom().nextInt(64)+1);effects(level,at,true,10);
            }
            case BLIND->{
                var donor=first(nearby,s->s.is(Items.GLASS_BOTTLE));if(donor==null)return;
                var output=loot(level,at,"lbc:bad_potion");if(output.isEmpty())return;
                debit(donor,1);publish(catalyst,output,false);effects(level,at,false,100);
            }
            default->{}
        }
    }
    private static void debit(ItemEntity item,int count){
        var remainder=item.getItem().copy();remainder.shrink(count);
        if(remainder.isEmpty())item.discard();else item.setItem(remainder);
    }
    private List<ItemStack> loot(ServerLevel level,Vec3 at,String table){return game.lbc().rollTable(level,at,null,table);}
    private void publish(ItemEntity context,List<ItemStack> output,boolean protectedCopy){publish((ServerLevel)context.level(),context.position(),output,context.getOwner(),protectedCopy);}
    private void publish(ServerLevel level,Vec3 at,List<ItemStack> output,Entity owner,boolean protectedCopy){for(var stack:output)publish(level,at,stack,owner,protectedCopy);}
    private void publish(ServerLevel level,Vec3 at,ItemStack stack,Entity owner,boolean protectedCopy){
        if(stack.isEmpty())return;var item=new ItemEntity(level,at.x,at.y,at.z,stack);item.setDefaultPickUpDelay();
        if(owner!=null)item.setThrower(owner);
        if(protectedCopy){item.setPermanentlyInvulnerable(true);item.entityTags().add(AIR_COPY);}
        game.items().markCurrentItemEpoch(item);level.addFreshEntity(item);
    }
    private static void effects(ServerLevel level,Vec3 at,boolean fire,int count){
        level.sendParticles(fire?ParticleTypes.FLAME:ParticleTypes.ENCHANT,at.x,at.y+.2,at.z,count,1,1,1,1);
        LbcOverload.sound(level,at,fire?"minecraft:item.firecharge.use":"minecraft:block.enchantment_table.use",1,1,0,0);
    }
    private void crafting(ServerPlayer player){
        if(!(player.containerMenu instanceof AbstractCraftingMenu menu)||menu.getInputGridSlots().isEmpty()
                ||!(menu.getInputGridSlots().getFirst().container instanceof CraftingContainer grid))return;
        boolean changed=false;
        for(var type:Tiny.values()){
            boolean present=false;for(int i=0;i<grid.getContainerSize();i++)if(type.matches(grid.getItem(i))){present=true;break;}
            if(!present)continue;
            if(type==Tiny.STONE){
                for(int i=0;i<grid.getContainerSize();i++)if(!grid.getItem(i).isEmpty()&&!grid.getItem(i).has(DataComponents.UNBREAKABLE)){
                    var stack=grid.getItem(i).copy();stack.set(DataComponents.UNBREAKABLE,Unit.INSTANCE);grid.setItem(i,stack);changed=true;
                }
                continue;
            }
            for(int i=0;i<grid.getContainerSize();i++){
                var donor=grid.getItem(i);
                if(donor.isEmpty()||type.matches(donor))continue;
                String table=null;boolean fire=false;
                if(type==Tiny.RUBY&&ItemData.intValue(donor,"libspell",0)==1)table="lbc:transformables_spells";
                else if(type==Tiny.BLIND&&donor.is(Items.GLASS_BOTTLE))table="lbc:bad_potion";
                else if(type==Tiny.ACID&&!LbcImbaPlaceableItems.tagged(donor,"corrupted_dust")){
                    if(player.getRandom().nextFloat()<.2f)table="lbc:corrupted_dust";
                }else if(type==Tiny.FIRE)fire=true;
                else continue;
                var rewards=table==null?List.<ItemStack>of():loot(player.level(),player.position(),table);
                var remainder=donor.copy();remainder.shrink(1);grid.setItem(i,remainder);changed=true;
                if(fire)fireReward(player,player.getRandom().nextInt(64)+1);
                publish(player.level(),player.position(),rewards,player,false);
                effects(player.level(),player.position(),fire,type==Tiny.ACID||fire?10:100);
                break;
            }
        }
        if(changed){grid.setChanged();menu.slotsChanged(grid);menu.broadcastChanges();}
    }
    // Filled from the authored64-outcome Tiny Fire table; constants avoid resource IO during a reaction.
    private record Buff(String effect,int ticks,int amplifier){}
    private static final Buff[] FIRE_EFFECTS={
        new Buff("minecraft:absorption",1200,0),
        new Buff("minecraft:absorption",1200,1),
        new Buff("minecraft:absorption",1200,2),
        new Buff("minecraft:fire_resistance",400,0),
        new Buff("minecraft:fire_resistance",800,0),
        new Buff("minecraft:fire_resistance",1200,0),
        new Buff("minecraft:haste",1200,0),
        new Buff("minecraft:haste",1200,1),
        new Buff("minecraft:haste",1200,2),
        new Buff("minecraft:health_boost",1200,0),
        new Buff("minecraft:health_boost",1200,1),
        new Buff("minecraft:health_boost",1200,2),
        new Buff("minecraft:instant_health",20,0),
        new Buff("minecraft:instant_health",20,1),
        new Buff("minecraft:instant_health",20,2),
        new Buff("minecraft:invisibility",1200,0),
        new Buff("minecraft:invisibility",600,0),
        new Buff("minecraft:invisibility",400,0),
        new Buff("minecraft:jump_boost",1200,0),
        new Buff("minecraft:jump_boost",1200,1),
        new Buff("minecraft:jump_boost",1200,2),
        new Buff("minecraft:night_vision",600,0),
        new Buff("minecraft:night_vision",1200,0),
        new Buff("minecraft:night_vision",2400,0),
        new Buff("minecraft:regeneration",1200,0),
        new Buff("minecraft:regeneration",800,1),
        new Buff("minecraft:regeneration",400,2),
        new Buff("minecraft:resistance",1200,0),
        new Buff("minecraft:resistance",800,1),
        new Buff("minecraft:resistance",400,2),
        new Buff("minecraft:saturation",20,3),
        new Buff("minecraft:saturation",20,6),
        new Buff("minecraft:saturation",20,12),
        new Buff("minecraft:slow_falling",1200,0),
        new Buff("minecraft:slow_falling",800,0),
        new Buff("minecraft:slow_falling",400,0),
        new Buff("minecraft:speed",1200,0),
        new Buff("minecraft:speed",1000,1),
        new Buff("minecraft:speed",800,2),
        new Buff("minecraft:strength",1200,0),
        new Buff("minecraft:strength",800,1),
        new Buff("minecraft:strength",400,2),
        new Buff("minecraft:luck",600,0),
        new Buff("minecraft:luck",400,1),
        new Buff("minecraft:luck",200,2)
    };
    public void fireReward(ServerPlayer player,int roll){
        if(roll<1||roll>64)throw new IllegalArgumentException("Tiny Fire reward index");
        player.entityTags().addAll(List.of("have_lucky_effects","have_lucky_effects_1s","have_lucky_effects_5t"));
        var mana=game.items().mana();
        if(roll<=5)mana.add(player,50+50*roll);
        else if(roll==6)mana.baseRegen(player,mana.baseRegen(player)+1);
        else if(roll<=16)mana.maximum(player,mana.maximum(player)+10*(roll-6));
        else if(roll==17)mana.score(player,"all_seeing_eye").add(300);
        else if(roll==18)mana.boost(player,mana.boost(player)+600);
        else if(roll==19)mana.score(player,"void_resistance").add(20);
        else{
            var buff=FIRE_EFFECTS[roll-20];var effect=BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(buff.effect)).orElseThrow();
            var instance=ItemEffectLevels.create(effect,buff.ticks,buff.amplifier);
            if(buff.effect.equals("minecraft:instant_health")||buff.effect.equals("minecraft:regeneration"))
                me.skywars2.game.combat.WeaponEnchantments.markHealingEffect(instance,me.skywars2.game.combat.WeaponEnchantments.effectCause(player));
            player.addEffect(instance,player);
        }
    }
}
