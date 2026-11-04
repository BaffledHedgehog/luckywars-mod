package me.skywars2.game;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/** Two distinct troll-mask inputs, with vanilla inventory debit and atomic ground publication. */
public final class LbcMellstroyCrafting {
    private final GameController game;
    private final Set<ItemEntity> items=Collections.newSetFromMap(new IdentityHashMap<>());
    private int phase;
    public LbcMellstroyCrafting(GameController game){this.game=game;}
    public static boolean ingredient(ItemStack stack){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("troll",0)==1;}
    public static boolean matches(CraftingInput input){
        if(input.ingredientCount()!=2)return false;
        for(var stack:input.items())if(!stack.isEmpty()&&!ingredient(stack))return false;
        return true;
    }
    /** A private fixed seed makes repeated previews observationally pure. */
    public static ItemStack result(GameController game){
        var level=game.maps().level();if(level==null)return ItemStack.EMPTY;
        var table=game.server().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("lbc:mellstroy_mask")));
        var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,Vec3.ZERO).create(LootContextParamSets.COMMAND);
        var rolls=table.getRandomItems(params,1L);
        return rolls.size()==1&&rolls.getFirst().getCount()==1?LbcMellstroyMaskItem.normalize(rolls.getFirst().copy()):ItemStack.EMPTY;
    }
    public static boolean blockTake(net.minecraft.world.inventory.AbstractContainerMenu menu,int slot,ServerPlayer player){
        if(slot!=0||!(menu instanceof net.minecraft.world.inventory.AbstractCraftingMenu crafting))return false;
        var slots=crafting.getInputGridSlots();
        if(slots.isEmpty()||!(slots.getFirst().container instanceof net.minecraft.world.inventory.CraftingContainer grid)||!matches(grid.asCraftInput()))return false;
        var game=me.skywars2.SkyWars2.game();
        boolean valid=LbcInfiniteShulker.allowed(game,player)&&game.playing()&&game.stage()!=GameController.Stage.ENDING
                &&player.isAlive()&&!player.isRemoved()&&!game.isObserver(player)&&!game.awaitingRespawn(player.getUUID())
                &&player.level()==game.maps().level()&&game.items().wands().validRoleTarget(player,false)
                &&!game.items().actionsBlocked(player)&&!game.server().tickRateManager().isFrozen()
                &&game.server().tickRateManager().runsNormally()&&!game.items().timeStop().stopped(player.level())
                &&!game.items().wands().timeStopped()&&!game.items().wands().inputTimeStopped(player)
                &&player.containerMenu==menu&&menu.stillValid(player);
        if(!valid)menu.sendAllDataToRemote();return !valid;
    }
    public static void crafted(GameController game,ServerLevel level,Vec3 at,ServerPlayer player){
        var particle=new ItemStack(Items.ACACIA_BOAT);particle.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:mellstroy_mask"));
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM,net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(particle)),true,false,at.x,at.y+.2,at.z,200,.2,.2,.2,0);
        LbcOverload.sound(level,at,"minecraft:lbcsounds.pampampam",2,1,0,0);
        if(player!=null)game.items().mana().award(player,"lbc:true_advancements/legends/mellstroy_mask");
    }
    public void loaded(Entity entity){if(entity instanceof ItemEntity item&&entity.level()==game.maps().level())items.add(item);}
    public void unloaded(Entity entity){items.remove(entity);}
    public void stop(){items.clear();phase=0;}
    public record Snapshot(int phase){}
    public Snapshot snapshot(){return new Snapshot(phase);}
    public void restore(Snapshot state){phase=state==null?0:state.phase();}
    private boolean eligible(ItemEntity item){return !item.isRemoved()&&item.level()==game.maps().level()
            &&ingredient(item.getItem())&&!game.items().hasStaleItemEpoch(item)&&!item.entityTags().contains("time_stopped");}
    private static void consume(ItemEntity item){if(item.getItem().getCount()==1)item.discard();else item.setItem(item.getItem().copyWithCount(item.getItem().getCount()-1));}
    public void tick(){
        items.removeIf(item->item.isRemoved()||item.level()!=game.maps().level());
        var level=game.maps().level();
        if(level==null||!game.playing()||game.stage()==GameController.Stage.ENDING||!LbcDimensionItems.ordinary(level)
                ||game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally()
                ||game.items().timeStop().stopped(level)||game.items().wands().timeStopped())return;
        phase=(phase+1)%20;if(phase!=0)return;
        for(var first:List.copyOf(items)){
            if(!eligible(first)||LbcFartus.data(first.getItem()).getIntOr("craftusable",0)!=1)continue;
            for(var second:level.getEntitiesOfClass(ItemEntity.class,first.getBoundingBox().inflate(1),other->other!=first&&eligible(other)
                    &&other.position().distanceToSqr(first.position())>=.000001&&other.position().distanceToSqr(first.position())<=1)){
                var stack=result(game);if(stack.isEmpty())return;
                var at=first.position();var output=new ItemEntity(level,at.x,at.y,at.z,stack);output.setDefaultPickUpDelay();
                if(first.getOwner() instanceof net.minecraft.world.entity.LivingEntity owner)output.setThrower(owner);
                game.items().markCurrentItemEpoch(output);if(!level.addFreshEntity(output))break;
                consume(first);consume(second);
                ServerPlayer nearest=null;double distance=196;
                for(var player:level.players())if(!player.isSpectator()&&player.position().distanceToSqr(at)<=distance){nearest=player;distance=player.position().distanceToSqr(at);}
                crafted(game,level,at,nearest);break;
            }
        }
    }
}
