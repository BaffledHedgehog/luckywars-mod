package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import java.util.*;

/** Actual recipient Polymer projection and original17-entry native inventory criteria. */
public final class FragmentKnowledgeChecks {
    private FragmentKnowledgeChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Fragment knowledge: "+why);}
    private static java.lang.reflect.Field field(Object actor,String name)throws NoSuchFieldException{for(Class<?> type=actor.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    private static Component translated(Component c,String key){if(c.getContents() instanceof TranslatableContents t&&t.getKey().equals(key))return c;for(var child:c.getSiblings()){var found=translated(child,key);if(found!=null)return found;}if(c.getContents() instanceof TranslatableContents t)for(var argument:t.getArgs())if(argument instanceof Component child){var found=translated(child,key);if(found!=null)return found;}return null;}
    private static boolean has(ItemStack stack,String key){var lore=stack.get(DataComponents.LORE);return lore!=null&&lore.lines().stream().anyMatch(c->translated(c,key)!=null);}
    private static ItemStack projection(ServerPlayer viewer,ItemStack source){
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND){@Override public net.minecraft.network.PacketListener getPacketListener(){return viewer.connection;}};
        var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)connection;
        require(eu.pb4.polymer.common.api.PolymerCommonUtils.getPlayer(provider.getPacketContext())==viewer,"native recipient context");
        ItemStack[] result={ItemStack.EMPTY};
        net.fabricmc.fabric.api.networking.v1.context.PacketContext.runWithContext(provider,()->result[0]=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(source,net.minecraft.world.item.TooltipFlag.NORMAL,provider.getPacketContext(),viewer.registryAccess()));
        require(ItemStack.matches(source,eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(result[0],provider.getPacketContext(),viewer.registryAccess())),"real Polymer round-trip preserves authoritative components");
        return result[0];
    }
    @SuppressWarnings("unchecked") private static ItemStack selected(GameController game,ServerPlayer owner,int type,boolean old){
        String id=old?"ported/legacy_fragment_sword":"fragmentsword";
        var table=game.server().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("lbc:"+id)));
        try{
            var pools=(List<net.minecraft.world.level.storage.loot.LootPool>)field(table,"pools").get(table);
            require(pools.size()==1&&pools.getFirst().entries.size()==17,"actual source17-entry table "+id);
            var sequence=(Optional<Identifier>)field(table,"randomSequence").get(table);require(sequence.isEmpty(),"source table native level RNG");
            var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(owner.level())
                    .withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,owner)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,owner.position())
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.COMMAND);
            var context=new net.minecraft.world.level.storage.loot.LootContext.Builder(params).create(sequence);
            require(context.getLuck()==0,"native command roll luck0");int total=0;
            for(var container:pools.getFirst().entries){var entries=new ArrayList<net.minecraft.world.level.storage.loot.entries.LootPoolEntry>();container.expand(context,entries::add);require(entries.size()==1&&entries.getFirst().getWeight(context.getLuck())==1,"each original Fragment entry has effective weight1");total+=entries.getFirst().getWeight(context.getLuck());}
            require(total==17,"source total effective weight17");
            var randomField=field(owner.level(),"random");var previous=randomField.get(owner.level());int[] draws={0};
            randomField.set(owner.level(),java.lang.reflect.Proxy.newProxyInstance(net.minecraft.util.RandomSource.class.getClassLoader(),new Class<?>[]{net.minecraft.util.RandomSource.class},(proxy,method,args)->{
                if(method.getName().equals("nextInt")&&args!=null&&args.length==1&&args[0].equals(17)){draws[0]++;return type;}
                try{return method.invoke(previous,args);}catch(java.lang.reflect.InvocationTargetException error){throw error.getCause();}
            }));
            try{var result=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:"+id);require(draws[0]==1&&result.size()==1,"one actual weighted native source selection "+id+"/"+type);var stack=result.getFirst();require(stack.is(old?Items.GOLDEN_SWORD:Items.IRON_SWORD)&&LbcFartus.data(stack).getIntOr("cooltype",-1)==type,"unchanged original selected Fragment carrier/type");return stack;}finally{randomField.set(owner.level(),previous);}
        }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }
    private static final class Saved {
        final ServerPlayer player;final GameChecks.WandThirdPlayerState state;
        final Map<java.lang.reflect.Field,Object> profile=new LinkedHashMap<>();
        final Map<net.minecraft.advancements.AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();
        final Map<net.minecraft.world.scores.Objective,Integer> scores=new LinkedHashMap<>();
        Saved(GameController game,ServerPlayer player){this.player=player;state=new GameChecks.WandThirdPlayerState(player,game.items().mana());
            try{for(var f:game.players().get(player.getUUID()).getClass().getFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){var value=f.get(game.players().get(player.getUUID()));profile.put(f,value instanceof int[] array?array.clone():value instanceof Set<?> set?new HashSet<>(set):value);}}catch(IllegalAccessException error){throw new IllegalStateException(error);}
            for(var a:game.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();player.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(done::add);advancements.put(a,done);}
            for(var objective:game.server().getScoreboard().getObjectives()){var value=game.server().getScoreboard().getPlayerScoreInfo(player,objective);if(value!=null)scores.put(objective,value.value());}
        }
        void restore(GameController game){
            advancements.forEach((a,done)->{for(var key:a.value().criteria().keySet())if(done.contains(key))player.getAdvancements().award(a,key);else player.getAdvancements().revoke(a,key);});
            state.restore(game);
            try{for(var e:profile.entrySet())e.getKey().set(game.players().get(player.getUUID()),e.getValue());}catch(IllegalAccessException error){throw new IllegalStateException(error);}
            for(var objective:game.server().getScoreboard().getObjectives())if(scores.containsKey(objective))game.server().getScoreboard().getOrCreatePlayerScore(player,objective,true).set(scores.get(objective));else game.server().getScoreboard().resetSinglePlayerScore(player,objective);
            game.lbc().refreshArtifactView(player);game.lbc().refreshKnowledgeView(player);
        }
    }
    public static void run(GameController game){
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));var unknown=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
        require(owner!=null&&unknown!=null&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(unknown),"two actual active recipients");
        var saved=new Saved(game,owner);var other=new Saved(game,unknown);boolean skills=game.config().lbcSkills;
        try{
            require(owner.containerMenu==owner.inventoryMenu&&unknown.containerMenu==unknown.inventoryMenu,"closed source personal inventories");
            owner.getInventory().clearContent();unknown.getInventory().clearContent();owner.inventoryMenu.getCraftSlots().clearContent();unknown.inventoryMenu.getCraftSlots().clearContent();owner.containerMenu.setCarried(ItemStack.EMPTY);unknown.containerMenu.setCarried(ItemStack.EMPTY);
            game.config().lbcSkills=true;game.players().get(owner.getUUID()).lbcSkill=3;game.players().get(owner.getUUID()).lbcChallenge=0;game.players().get(unknown.getUUID()).lbcSkill=0;game.players().get(unknown.getUUID()).lbcChallenge=0;
            for(var p:List.of(owner,unknown)){game.lbc().refreshArtifactView(p);game.lbc().refreshKnowledgeView(p);}
            for(int type=0;type<=16;type++){
                var stack=selected(game,owner,type,false);var original=stack.copy();var yes=projection(owner,stack);var no=projection(unknown,stack);
                require(has(yes,"fragment_"+type)&&!has(yes,"fragment_sword.lore")&&has(no,"fragment_sword.lore")&&!has(no,"fragment_"+type),"source recipient-only fragment description "+type);
                String descriptionKey="fragment_"+type;var lines=yes.get(DataComponents.LORE).lines().stream().map(c->translated(c,descriptionKey)).filter(Objects::nonNull).toList();
                require(lines.size()==1&&!lines.getFirst().getStyle().isItalic()&&lines.getFirst().getStyle().getColor()!=null&&lines.getFirst().getStyle().getColor().getValue()==0xffff55,"source single yellow nonitalic fragment_N line");
                require(ItemStack.matches(original,stack)&&LbcFartus.data(stack).getIntOr("unresolved_fragment",0)==1,"no authoritative NBT flag/lore mutation");
                var old=selected(game,owner,type,true);var oldCopy=old.copy();for(var viewer:List.of(owner,unknown))require(has(projection(viewer,old),"fragment_sword.lore")&&!has(projection(viewer,old),"fragment_"+type),"old fragment unchanged for both recipients");require(ItemStack.matches(old,oldCopy),"old authoritative payload unchanged");
            }
            var stack=selected(game,owner,7,false);game.players().get(owner.getUUID()).lbcChallenge=1;game.lbc().refreshKnowledgeView(owner);
            var hidden=projection(owner,stack);require(hidden.getHoverName().getString().equals("????????????????????????????")&&hidden.get(DataComponents.LORE).lines().size()==1&&hidden.get(DataComponents.LORE).lines().getFirst().getString().equals("????????????????????????????"),"knowledge challenge retains final projection priority");
            game.players().get(owner.getUUID()).lbcChallenge=0;game.lbc().refreshKnowledgeView(owner);
            var task=game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/swords/fragment"));require(task!=null&&task.value().criteria().size()==17,"actual packaged17criterion collection task");
            for(var key:task.value().criteria().keySet())owner.getAdvancements().revoke(task,key);
            for(int type=0;type<=16;type++){
                owner.getInventory().clearContent();var old=selected(game,owner,type,true);owner.getInventory().setItem(0,old);CriteriaTriggers.INVENTORY_CHANGED.trigger(owner,owner.getInventory(),old);
                require(!owner.getAdvancements().getOrStartProgress(task).hasProgress(),"legacy source payload does not advance current Fragment collection");
            }
            for(int type=0;type<=16;type++){
                owner.getInventory().clearContent();var current=selected(game,owner,type,false);owner.getInventory().setItem(0,current);CriteriaTriggers.INVENTORY_CHANGED.trigger(owner,owner.getInventory(),current);
                var progress=owner.getAdvancements().getOrStartProgress(task);require(progress.getCriterion("cool_"+(type+1)).isDone(),"actual source native inventory criterion "+type);
                require(progress.isDone()==(type==16),"collection accumulates over time and requires all17 actual variants");
            }
        }finally{game.config().lbcSkills=skills;try{saved.restore(game);}finally{other.restore(game);}}
        SkyWars2.LOGGER.info("[check] PASS fragment-knowledge (actual original17-entry native weighted family acquisition; recipient Polymer yellow descriptions/unknown/legacy/knowledge priority; authoritative NBT and client round-trip; real native17criterion collection accumulated over time; scoped restoration)");
    }
    /** Synchronous native criterion/projection checks have no pending player/effect ticks. */
    public static void tick(GameController game){}
}
