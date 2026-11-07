package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.scores.ScoreHolder;

/** Passive Ice Wand donor and the arcanum left by its native recipe. */
public final class LbcIceMaterials {
    private static final CompoundTag SHARD_FLAG=flag("runic_ice_shard"),BALL_FLAG=flag("neutral_ball");
    public static final Item RUNIC_ICE_SHARD=register("runic_ice_shard",Items.REPEATING_COMMAND_BLOCK,"q",false);
    public static final Item NEUTRAL_BALL=register("neutral_ball",Items.CARROT_ON_A_STICK,"b",true);
    private final GameController game;

    LbcIceMaterials(GameController game,LbcMana mana){
        this.game=game;
        mana.score(ScoreHolder.forNameOnly("neutral_ball_mana_activate"),"lbc.math").set(5000);
    }
    public static void register(){ }

    private static CompoundTag flag(String name){var tag=new CompoundTag();tag.putInt(name,1);return tag;}
    private static Item register(String name,Item proxy,String tier,boolean ball){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:"+name));
        var data=flag(name);data.putInt("transformable",1);
        if(ball){data.putInt("arcanum",1);data.putInt("arcanumt",1);}
        else{data.putInt("accessory",1);data.putInt("craftusable",1);}
        var model=Identifier.parse("lbc:"+name);
        var properties=new Item.Properties(){@Override public Identifier effectiveModel(){return model;}}
                .setId(key).overrideDescription(name).component(DataComponents.ITEM_MODEL,model)
                .component(DataComponents.TOOLTIP_STYLE,Identifier.parse("lbc:tier_"+tier))
                .component(DataComponents.CUSTOM_DATA,CustomData.of(data));
        // These are ordinary passive Items: their vanilla client proxies never place blocks or ride pigs.
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(properties,proxy,true));
    }
    private static boolean matches(ItemStack stack,Item item,CompoundTag flag){
        if(stack.isEmpty())return false;
        if(stack.is(item))return true;
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return data!=null&&data.matchedBy(flag);
    }
    public static boolean isRunicIceShard(ItemStack stack){return matches(stack,RUNIC_ICE_SHARD,SHARD_FLAG);}
    public static boolean isNeutralBall(ItemStack stack){return matches(stack,NEUTRAL_BALL,BALL_FLAG);}

    private boolean active(ServerPlayer player){
        return game.items().mana().visible(player)&&!game.items().timeStop().frozen(player)
                &&!game.items().timeStop().stopped(player.level())&&!player.entityTags().contains("time_stopped")
                &&!game.items().magic().suppressed(player);
    }
    /** Source ten-tick boundary, also callable by the bounded native fixture. */
    void pulseShard(ServerPlayer player){
        if(!active(player))return;
        boolean mainShard=isRunicIceShard(player.getMainHandItem()),offShard=isRunicIceShard(player.getOffhandItem());
        boolean handsOnly=game.lbc().skillsEnabled()&&game.players().get(player.getUUID()).lbcSkill==2;
        boolean containerShard=false;
        if(!handsOnly||mainShard||offShard){
            for(var stack:player.getInventory().getNonEquipmentItems()){
                if(isRunicIceShard(stack)){containerShard=true;break;}
            }
        }
        // Source container.* includes the selected mainhand slot, but excludes offhand and armor.
        // Keep both independent calls: selected mainhand alone therefore pulses twice.
        if(containerShard)game.items().frost().runicShard(player);
        if(mainShard||offShard)game.items().frost().runicShard(player);
    }
    /** Called once per ordinary server player tick; no extra inventory cache or serialized NBT scan. */
    void tickPlayer(ServerPlayer player){
        if(!active(player))return;
        if(game.server().getTickCount()%10==0)pulseShard(player);
        // Frozen holders still receive the shard's Resistance reward above, as in its source.
        if(game.items().frost().isFrozen(player))return;
        boolean mainBall=isNeutralBall(player.getMainHandItem()),offBall=isNeutralBall(player.getOffhandItem());
        if(!mainBall&&!offBall)return;
        var mana=game.items().mana();
        int admission=mana.score(ScoreHolder.forNameOnly("neutral_ball_mana_activate"),"lbc.math").get();
        // Main then off is intentional: the offhand can undo the mainhand exchange in this frame.
        if(mainBall&&mana.get(player)>=admission){
            mana.baseRegen(player,mana.baseRegen(player)+1);mana.add(player,-5000);
            mana.award(player,"lbc:true_advancements/arcanums/neutral");
        }
        if(offBall&&mana.baseRegen(player)>=1){
            mana.baseRegen(player,mana.baseRegen(player)-1);mana.add(player,5000);
        }
    }
}
