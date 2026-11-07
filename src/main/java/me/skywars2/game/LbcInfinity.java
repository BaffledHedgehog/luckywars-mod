package me.skywars2.game;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.phys.Vec3;

/** Original Infinity reward, inventory instant selection and native persistent traits. */
public final class LbcInfinity {
    private static final EquipmentSlot[] EQUIPMENT={EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private static final CompoundTag REWARD=preset("""
            {Tags:["itemteamblue"],NoGravity:1b,Glowing:1b,CustomNameVisible:1b,Age:-32768,Health:999999,
            CustomName:{"color":"dark_aqua","italic":false,"translate":"infinity"},
            Item:{id:"minecraft:repeating_command_block",components:{"minecraft:custom_data":{instantitem:1,infinity_rand:1},
            "minecraft:item_model":"lbc:infinity","minecraft:tooltip_style":"lbc:tier_x"},count:1}}
            """);
    private static final Identifier LARGE=Identifier.parse("minecraft:infinity_scale"), SMALL=Identifier.parse("minecraft:infinity_scale1");
    private static final String[] EXPLANATIONS={"infinity_1","infinity_2","infinity_3","infinity_4",
            "infinity_5","infinity_6","infinity_7","infinity_8","infinity_9","infinity_10",
            "infinity_11","infinity_12","infinity_13","infinity_14"};
    private final GameController game;
    private final Map<UUID,Integer> lastAttempt=new HashMap<>();
    private String epoch;
    private int phase,lastTick=Integer.MIN_VALUE;

    public LbcInfinity(GameController game){this.game=game;}
    private static CompoundTag preset(String value){
        try{return TagParser.parseCompoundFully(value);}
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new ExceptionInInitializerError(error);}
    }
    private static boolean flag(ItemStack stack,String key){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr(key,0)==1;}
    public static boolean recognized(ItemStack stack){return flag(stack,"infinity_spawn")&&flag(stack,"drop");}
    public static boolean instant(ItemStack stack){return flag(stack,"infinity_rand");}
    private boolean active(ServerLevel level){return game.playing()&&game.stage()!=GameController.Stage.ENDING
            &&level==game.maps().level()&&!game.items().timeStop().stopped(level);}
    public boolean ready(ItemEntity token){
        if(token.isRemoved()||token.getAge()>=5999||!recognized(token.getItem())
                ||!(token.level() instanceof ServerLevel level)||!active(level))return false;
        for(var tag:token.entityTags())if(tag.startsWith("skywars2.items.")&&!game.items().currentEpoch(tag))return false;
        return resident(level,token.position());
    }
    /** Shared special-drop dispatcher owns the one original token debit. */
    public boolean spawn(ItemEntity token){
        if(!ready(token))return false;
        game.items().sauvojenBoss().infinity((ServerLevel)token.level(),token.position(),
                LbcIndirectKills.playerSource(game.items().specialDrops().owner(token)));
        return true;
    }
    /** The boss and explicit infinity_spawn branch share this exact source reward producer. */
    public ItemEntity produce(ServerLevel level,Vec3 at,LivingEntity author){
        if(!active(level)||!resident(level,at)||!net.minecraft.world.level.Level.isInSpawnableBounds(net.minecraft.core.BlockPos.containing(at)))return null;
        var item=EntityTypes.ITEM.create(level,EntitySpawnReason.COMMAND);
        if(item==null)return null;
        item.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),REWARD.copy()));
        item.setPos(at);LbcBossLoot.protect(item);if(author!=null)item.setThrower(author);
        game.items().markCurrentItemEpoch(item);game.items().timeStop().prepareFrozen(item);
        if(!level.addFreshEntity(item)){item.discard();return null;}
        var board=game.server().getScoreboard();var team=board.getPlayerTeam("bluegays");
        if(team==null)team=board.addPlayerTeam("bluegays");team.setColor(java.util.Optional.of(net.minecraft.world.scores.TeamColor.AQUA));
        board.addPlayerToTeam(item.getScoreboardName(),team);
        return item;
    }
    private static boolean resident(ServerLevel level,Vec3 at){
        var pos=net.minecraft.core.BlockPos.containing(at);
        return level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;
    }
    private boolean admitted(ServerPlayer p){return p!=null&&p.isAlive()&&!p.isRemoved()&&active(p.level())
            &&game.canUseGameplayItems(p)&&!game.isObserver(p)&&game.items().wands().validRoleTarget(p,false)
            &&LbcDimensionItems.ordinary(p)&&!game.items().wands().inputTimeStopped(p);}
    private static boolean inventory(ServerPlayer p,String key){
        for(int slot=0;slot<36;slot++)if(flag(p.getInventory().getItem(slot),key))return true;
        for(var slot:EQUIPMENT)if(flag(p.getItemBySlot(slot),key))return true;
        return false;
    }
    /** One native players_tick instant attempt; returns source branch1..14 or0 without a consume. */
    public int tickPlayer(ServerPlayer p){
        if(!admitted(p)||!inventory(p,"instantitem"))return 0;
        int now=game.server().getTickCount();if(lastAttempt.getOrDefault(p.getUUID(),Integer.MIN_VALUE)==now)return 0;
        var mana=game.items().mana();boolean skills=mana.hudValue(ScoreHolder.forNameOnly("#lbcskill"),"swrg.math")==1;
        int skill=mana.hudValue(p,"lbc.skill");
        if(skills&&skill==2?!instant(p.getMainHandItem()):!inventory(p,"infinity_rand"))return 0;
        lastAttempt.put(p.getUUID(),now);
        int branch=p.getRandom().nextInt(14)+1;
        if(skills&&skill==3)p.sendSystemMessage(Component.translatable(EXPLANATIONS[branch-1]).withStyle(ChatFormatting.LIGHT_PURPLE));
        apply(p,branch);
        // Source clear is global, even when SelectedItem admitted the skill2 attempt.
        p.getInventory().clearOrCountMatchingItems(LbcInfinity::instant,false,1,p.inventoryMenu.getCraftSlots());
        p.getInventory().setChanged();p.inventoryMenu.slotsChanged(p.getInventory());p.containerMenu.broadcastChanges();
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(" ")));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("infinity_feel")));
        mana.award(p,"lbc:true_advancements/legends/infinity");
        return branch;
    }
    private void apply(ServerPlayer p,int branch){
        switch(branch){
            case 1->game.items().mana().maximum(p,100000000);
            case 2->game.items().mana().baseRegen(p,100000000);
            case 3->{base(p,"armor",10000000);base(p,"armor_toughness",10000000);base(p,"burning_time",-1000000);}
            case 4->base(p,"attack_damage",100000000);
            case 5->{base(p,"attack_speed",10000000);base(p,"block_break_speed",214708364);}
            case 6->{base(p,"knockback_resistance",10000000);base(p,"explosion_knockback_resistance",10000000);}
            case 7->base(p,"max_health",214708364);
            case 8->p.entityTags().add("infinite_resistance");
            case 9->p.entityTags().add("infinite_heal");
            case 10->p.entityTags().add("infinite_saturation");
            case 11->{
                var stack=luckyBlockWand(p);p.getInventory().add(stack);
                if(!stack.isEmpty()){
                    var drop=p.drop(stack,false,net.minecraft.util.Prediction.SERVER_ONLY);
                    if(drop!=null){drop.setNoPickUpDelay();drop.setTarget(p.getUUID());}
                }
            }
            case 12->{
                base(p,"fall_damage_multiplier",-1);
                for(var attr:List.of("movement_efficiency","water_movement_efficiency","step_height","sneaking_speed","safe_fall_distance","oxygen_bonus"))base(p,attr,10000000);
            }
            case 13->{
                for(var attr:List.of("scale","attack_damage","explosion_knockback_resistance","jump_strength","knockback_resistance","max_health","movement_efficiency","movement_speed","safe_fall_distance","step_height","water_movement_efficiency","block_break_speed","mining_efficiency","gravity"))
                    modifier(p,attr,LARGE,16,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
                modifier(p,"attack_knockback",LARGE,16,AttributeModifier.Operation.ADD_VALUE);
                modifier(p,"fall_damage_multiplier",LARGE,-.9375,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
                modifier(p,"block_interaction_range",LARGE,32,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
                modifier(p,"entity_interaction_range",LARGE,32,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
                modifier(p,"sweeping_damage_ratio",LARGE,1,AttributeModifier.Operation.ADD_VALUE);
            }
            case 14->{
                modifier(p,"scale",SMALL,-.9375,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
                modifier(p,"safe_fall_distance",SMALL,10000,AttributeModifier.Operation.ADD_VALUE);
                modifier(p,"fall_damage_multiplier",SMALL,-1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
                modifier(p,"gravity",LARGE,-.8125,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            }
            default->throw new IllegalArgumentException("Infinity branch "+branch);
        }
    }
    private net.minecraft.world.entity.ai.attributes.AttributeInstance attribute(ServerPlayer p,String name){
        return p.getAttribute(p.registryAccess().lookupOrThrow(Registries.ATTRIBUTE)
                .getOrThrow(ResourceKey.<Attribute>create(Registries.ATTRIBUTE,Identifier.parse("minecraft:"+name))));
    }
    private void base(ServerPlayer p,String name,double value){var attr=attribute(p,name);if(attr!=null)attr.setBaseValue(value);}
    private void modifier(ServerPlayer p,String name,Identifier id,double value,AttributeModifier.Operation operation){
        var attr=attribute(p,name);
        // Native /attribute modifier add fails on an existing ID; it never replaces/compounds it.
        if(attr!=null&&!attr.hasModifier(id))attr.addPermanentModifier(new AttributeModifier(id,value,operation));
    }
    private static ItemStack luckyBlockWand(ServerPlayer p){
        var stack=new ItemStack(Items.CARROT_ON_A_STICK);var data=new CompoundTag();
        data.putInt("magicwand",1);data.putInt("luckyblockwand",1);data.putInt("craftusable",1);
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
        stack.set(DataComponents.CUSTOM_NAME,Component.translatable("infinite_lucky_block").withStyle(s->s.withColor(ChatFormatting.YELLOW).withItalic(false)));
        stack.set(DataComponents.LORE,new ItemLore(List.of(Component.translatable("infinite_lucky_block.lore").withStyle(s->s.withColor(ChatFormatting.YELLOW).withItalic(false)))));
        stack.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:lucky_block"));stack.set(DataComponents.TOOLTIP_STYLE,Identifier.parse("lbc:tier_u"));
        stack.enchant(p.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.INFINITY),1);
        return stack;
    }
    /** Hook into the existing source300-tick phase. Effect duration is infinite, not300 ticks. */
    public void pulse300(ServerPlayer p){
        if(!admitted(p))return;
        if(p.entityTags().contains("infinite_resistance"))p.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,-1,4),p);
        if(p.entityTags().contains("infinite_heal"))p.addEffect(ItemEffectLevels.create(MobEffects.INSTANT_HEALTH,-1,99),p);
        if(p.entityTags().contains("infinite_saturation"))p.addEffect(ItemEffectLevels.create(MobEffects.SATURATION,-1,50),p);
    }
    /** One owned source phase; never manufactures native effect ticks or advances through Time Stop. */
    public void tick(){
        int now=game.server().getTickCount();if(lastTick==now)return;lastTick=now;
        if(!game.items().currentEpoch(epoch)){phase=0;epoch=game.items().epoch();lastAttempt.clear();}
        var level=game.maps().level();
        if(level==null||!active(level)||game.items().wands().timeStopped())return;
        phase=(phase+1)%300;
        for(var p:game.server().getPlayerList().getPlayers())if(admitted(p)){
            tickPlayer(p);if(phase==0)pulse300(p);
        }
    }
    /** Only attempt guards are session state; source traits, tags and native effects persist normally. */
    public void forget(ServerPlayer p){lastAttempt.remove(p.getUUID());}
    public void stop(){lastAttempt.clear();phase=0;epoch=null;lastTick=Integer.MIN_VALUE;}
}
