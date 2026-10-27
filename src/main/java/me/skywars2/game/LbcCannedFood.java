package me.skywars2.game;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Source Canned Food consumer. The caller owns the shared forty-tick phase; effects retain native clocks. */
public final class LbcCannedFood {
    private static final CompoundTag FLAG=new CompoundTag();
    private static final Item[] PARTICLES={Items.COOKED_BEEF,Items.COOKED_PORKCHOP,Items.COOKED_RABBIT,
            Items.COOKED_SALMON,Items.COOKED_MUTTON,Items.POTATO,Items.BAKED_POTATO};
    static {FLAG.putInt("konserva",1);}
    private final GameController game;
    public LbcCannedFood(GameController game){this.game=game;}
    private static boolean matches(ItemStack stack){var data=stack.get(DataComponents.CUSTOM_DATA);return !stack.isEmpty()&&data!=null&&data.matchedBy(FLAG);}
    private boolean admitted(ServerPlayer owner){
        if(owner==null||!owner.isAlive()||owner.isRemoved()||owner.level()!=game.maps().level()
                ||!game.canUseGameplayItems(owner)||game.isObserver(owner)||game.stage()==GameController.Stage.ENDING
                ||!game.items().wands().validRoleTarget(owner,false)||game.items().wands().timeStopped()||game.items().wands().inputTimeStopped(owner)
                ||game.items().timeStop().stopped(owner.level()))return false;
        var dimension=owner.level().dimension().identifier();
        return !dimension.equals(Identifier.parse("minecraft:nexus"))&&!dimension.equals(Identifier.parse("minecraft:imprinted"));
    }
    private static boolean container(ServerPlayer owner){for(int slot=0;slot<36;slot++)if(matches(owner.getInventory().getItem(slot)))return true;return false;}
    private static boolean hands(ServerPlayer owner){return matches(owner.getMainHandItem())||matches(owner.getOffhandItem());}
    /** Source container then weapon probes; the first clear can remove the selected-hand trigger for the second. */
    public void pulse(ServerPlayer owner){
        if(!admitted(owner))return;
        // foodlvl is a compatibility DUMMY ledger in this port, not a live native FOOD-criterion supplier.
        if(owner.getFoodData().getFoodLevel()<20&&container(owner))consume(owner);
        if(admitted(owner)&&owner.getFoodData().getFoodLevel()<20&&hands(owner))consume(owner);
    }
    private void consume(ServerPlayer owner){
        // Native addEffect does not feed immediately; equal one-tick grants merge rather than adding feed ticks.
        owner.addEffect(ItemEffectLevels.create(MobEffects.SATURATION,1,100),owner);
        var level=owner.level();var point=owner.getEyePosition().add(owner.getLookAngle().scale(.3));
        for(var item:PARTICLES)
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM,item),point.x,point.y,point.z,2,.1,.1,.1,0);
        LbcOverload.sound(owner,"minecraft:entity.player.burp",1,1,0);
        // Exact eleven outcomes are shared with the original Traveller draw, including native signed caps and visibility.
        LbcTraveller.buff(owner,owner.getRandom().nextInt(11)+1);
        owner.getInventory().clearOrCountMatchingItems(LbcCannedFood::matches,false,1,owner.inventoryMenu.getCraftSlots());
        owner.getInventory().setChanged();owner.containerMenu.broadcastChanges();owner.inventoryMenu.slotsChanged(owner.getInventory());
        // Source increments after clear even if that command had no matching item by this point.
        game.items().mana().score(owner,"food_count").add(1);
    }
}
