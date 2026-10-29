package me.skywars2.game;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.*;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Source acquisition rules over existing live counter owners; no private authority/progress ledger. */
public final class LbcWitchClaims {
    enum Mode {
        GLUTTONY("gluttony","food_count",64),WRATH("wrath","dmg_dealt",5000),
        LUST("lust","lolidefecation",64),GREED("greed","greed_prestige",0),
        VANITY("vanity","dmg_dealt",0),INDOLENCE("indolence","sleep_time",3600),ENVY("envy",null,0);
        final String key,counter;final int minimum;
        Mode(String key,String counter,int minimum){this.key=key;this.counter=counter;this.minimum=minimum;}
        String tag(){return "sin_"+key;}
        static Mode of(Integer value){return value!=null&&value>=1&&value<=values().length?values()[value-1]:null;}
        // Their producer/checkpoint dependencies remain explicit, not aliases for a tag grant.
        boolean implemented(){return this!=LUST&&this!=VANITY;}
    }
    enum Verdict { ALREADY,UNQUALIFIED,QUALIFIED }
    private final GameController game;
    private final LbcSourceCounters counters;
    private final Map<String,EntitySelector> selectors=new HashMap<>();
    LbcWitchClaims(GameController game){this.game=game;counters=new LbcSourceCounters(game);}
    public static boolean matches(ItemStack item){return !item.isEmpty()&&LbcFartus.data(item).getIntOr("witch_gens",0)==1;}
    private List<ServerPlayer> players(ServerPlayer p,String literal){try{
        var selector=selectors.get(literal);if(selector==null){selector=new EntitySelectorParser(new StringReader(literal),true).parse();selectors.put(literal,selector);}
        return selector.findPlayers(p.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput());
    }catch(CommandSyntaxException error){throw new IllegalStateException("Witch claim selector "+literal,error);}}
    private ServerPlayer incumbent(ServerPlayer p,Mode mode){var found=players(p,"@a[gamemode=!spectator,tag="+mode.tag()+",limit=1]");return found.isEmpty()?null:found.getFirst();}
    /** Qualification reads live native owners and never creates missing scoreboard operands. */
    Verdict verdict(ServerPlayer p,Mode mode){
        if(p.entityTags().contains(mode.tag()))return Verdict.ALREADY;
        if(mode==Mode.ENVY){if(p.entityTags().contains("have_mom"))return Verdict.UNQUALIFIED;
            for(var other:players(p,"@a[gamemode=!spectator,tag=!owner]"))if(other!=p&&!other.entityTags().contains("have_mom"))return Verdict.UNQUALIFIED;
            return Verdict.QUALIFIED;}
        Integer score=counters.read(p,mode.counter);
        if(mode==Mode.VANITY)return score!=null&&score==0&&game.lives(p.getUUID())<=1&&game.matchTicks()/20>=600?Verdict.QUALIFIED:Verdict.UNQUALIFIED;
        if(mode==Mode.GREED){boolean token=false;
            // Native Inventory includes equipment/offhand, not the cursor/crafting container.
            for(int i=0;i<p.getInventory().getContainerSize();i++)if(LbcFartus.data(p.getInventory().getItem(i)).getIntOr("greed_greed",0)==1){token=true;break;}
            if(!p.entityTags().contains("greed_cursed")||!token)return Verdict.UNQUALIFIED;
        }else if(score==null||score<mode.minimum)return Verdict.UNQUALIFIED;
        var current=incumbent(p,mode);if(current==null)return Verdict.QUALIFIED;
        Integer theirs=counters.read(current,mode.counter);
        return score!=null&&theirs!=null&&theirs<score?Verdict.QUALIFIED:Verdict.UNQUALIFIED;
    }
    boolean use(ServerPlayer p,InteractionHand hand){
        if(!matches(p.getItemInHand(hand))||!game.playing()||game.stage()==GameController.Stage.ENDING||!p.isAlive()||p.isRemoved()
                ||game.isObserver(p)||!game.canUseGameplayItems(p)||!LbcDimensionItems.ordinary(p)||!game.items().wands().validRoleTarget(p,false)
                ||game.items().actionsBlocked(p)||game.items().wands().inputTimeStopped(p)||game.items().wands().timeStopped()
                ||game.items().timeStop().stopped(p.level())||hand==InteractionHand.OFF_HAND&&!game.config().offhandWands
                ||p.entityTags().contains("nomagic_active")||game.items().magic().artifactSuppressedAt(p.level(),p.position()))return false;
        if(p.isShiftKeyDown()){
            Integer before=counters.read(p,"gens_type");int next=before==null?1:before+1;if(next>=8)next=1;
            game.items().mana().score(p,"gens_type").set(next);var mode=Mode.of(next);
            if(mode!=null)p.sendSystemMessage(Component.translatable(mode.key));return true;
        }
        var mode=Mode.of(counters.read(p,"gens_type"));if(mode==null)return false;
        // Keep raw Lust/Vanity acquisition disconnected until their real dependencies exist.
        // In particular, do not consume the item, transfer a tag or punish a qualified holder.
        if(!mode.implemented())return false;
        var verdict=verdict(p,mode);if(verdict==Verdict.ALREADY)return false;
        if(verdict==Verdict.UNQUALIFIED){failure(p,mode);return true;}
        claim(p,mode);
        // Free authority acquisition has the shared successful magicwand bonuses, but no paid mage-set growth.
        game.items().wands().wandCasted(p,false);return true;
    }
    private void failure(ServerPlayer p,Mode mode){
        p.addEffect(ItemEffectLevels.create(MobEffects.WITHER,200,9),p);
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(net.minecraft.core.Holder.direct(
                net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.Identifier.parse("minecraft:entity.wither.ambient"))),
                net.minecraft.sounds.SoundSource.MASTER,p.getX(),p.getY(),p.getZ(),.5f,1,p.getRandom().nextLong()));
        p.sendSystemMessage(Component.translatable("no_witch_gens"));
        Integer score=mode.counter==null?null:counters.read(p,mode.counter);if(score==null)return;
        String key=switch(mode){case GLUTTONY->"food_count";case WRATH,VANITY->"dmg_dealt";case LUST->"loli_count";case GREED->"greed_count";case INDOLENCE->"indolence_count";default->null;};
        if(key!=null)p.sendSystemMessage(Component.empty().append(Component.translatable(key).withStyle(s->s.withColor(0xff55ff))).append(Component.literal(Integer.toString(mode==Mode.INDOLENCE?Math.floorDiv(score,20):mode==Mode.WRATH||mode==Mode.VANITY?Math.floorDiv(score,10):score))));
    }
    private void yes(ServerPlayer p){
        int removed=p.getInventory().clearOrCountMatchingItems(LbcWitchClaims::matches,false,1,p.inventoryMenu.getCraftSlots());
        if(removed!=1)throw new IllegalStateException("Witch claim lost its source item before synchronous debit");
        game.items().mana().award(p,"lbc:true_advancements/legends/witch_gens");
    }
    private void unget(ServerPlayer p,Mode mode){for(var holder:players(p,"@a[tag="+mode.tag()+"]"))holder.sendSystemMessage(Component.translatable("witch_"+mode.key+"_unget"));}
    private void assign(ServerPlayer p,Mode mode){for(var holder:players(p,"@a")){holder.entityTags().remove(mode.tag());
        if(mode==Mode.GREED)holder.entityTags().remove("sin_greed_active");
        if(mode==Mode.INDOLENCE)holder.entityTags().remove("indolence_active");}
        p.entityTags().add(mode.tag());}
    private void get(ServerPlayer p,Mode mode){p.sendSystemMessage(Component.translatable("witch_"+mode.key+"_get"));}
    private void award(ServerPlayer p,Mode mode){game.items().mana().award(p,"lbc:true_advancements/legends/sins/"+mode.key);}
    private void drop(ServerPlayer p,String table){for(var item:game.lbc().rollTable(p.level(),p.position(),p,table)){
        var e=new ItemEntity(p.level(),p.getX(),p.getY(),p.getZ(),item);e.setDefaultPickUpDelay();game.items().markCurrentItemEpoch(e);p.level().addFreshEntity(e);}}
    private void claim(ServerPlayer p,Mode mode){
        switch(mode){
            case GLUTTONY->{drop(p,"lbc:glutton_summons");unget(p,mode);yes(p);get(p,mode);assign(p,mode);award(p,mode);}
            case WRATH->{unget(p,mode);yes(p);assign(p,mode);get(p,mode);award(p,mode);}
            case GREED->{unget(p,mode);yes(p);assign(p,mode);get(p,mode);award(p,mode);p.entityTags().remove("greed_cursed");}
            case INDOLENCE->{get(p,mode);unget(p,mode);yes(p);assign(p,mode);game.items().indolenceFingers().acquired(p);}
            case ENVY->{get(p,mode);assign(p,mode);award(p,mode);yes(p);}
            default->throw new IllegalStateException("Missing actual Witch acquisition dependency "+mode);
        }
        p.getInventory().setChanged();p.inventoryMenu.sendAllDataToRemote();game.items().mana().inventoryChanged(p);game.items().magic().inventoryChanged(p);
    }
    /** Match-end remove_tags boundary; normal deaths/respawns deliberately retain authority. */
    void stop(){for(var p:game.server().getPlayerList().getPlayers()){
        for(var mode:Mode.values())p.entityTags().remove(mode.tag());
        p.entityTags().remove("sin_greed_active");p.entityTags().remove("indolence_active");p.entityTags().remove("kill_one_skeleton");
    }selectors.clear();}
}
