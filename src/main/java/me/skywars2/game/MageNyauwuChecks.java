package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import java.util.*;

/** Bounded native source-loot, reforge, mana and armor checks using restored live fixtures. */
public final class MageNyauwuChecks {
    private MageNyauwuChecks(){}
    private static final EquipmentSlot[] SLOTS={EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD};
    private static final String[] PARTS={"boots","leggings","chestplate","helmet"};
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Mage/Nyauwu: "+why);}
    private static java.lang.reflect.Field field(Object value,String name){for(Class<?> c=value.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new IllegalStateException("Missing fixture field "+name);}
    private static Object read(Object value,String name){try{return field(value,name).get(value);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static void set(Object value,String name,Object next){try{field(value,name).set(value,next);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static void equipmentUpdate(LivingEntity player){try{var m=LivingEntity.class.getDeclaredMethod("detectEquipmentUpdates");m.setAccessible(true);m.invoke(player);var health=LivingEntity.class.getDeclaredMethod("onAttributeUpdated",net.minecraft.core.Holder.class);health.setAccessible(true);health.invoke(player,Attributes.MAX_HEALTH);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    private static net.minecraft.nbt.CompoundTag save(LivingEntity player){var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,player.registryAccess());net.fabricmc.fabric.api.networking.v1.context.PacketContext.supplyWithoutContext(()->{player.saveWithoutId(output);return null;});return output.buildResult();}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer owner,beta;final LbcMana mana;
        final GameChecks.WandThirdPlayerState saved,betaSaved;
        final AttributeMap attributes=new AttributeMap(DefaultAttributes.getSupplier(EntityTypes.PLAYER));
        final Map<EquipmentSlot,ItemStack> equipment=new EnumMap<>(EquipmentSlot.class);
        final Map<net.minecraft.advancements.AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();
        final Map<net.minecraft.world.scores.Objective,Integer> scores=new LinkedHashMap<>();
        final Map<net.minecraft.stats.Stat<?>,Integer> stats=new LinkedHashMap<>();
        final Map<java.lang.reflect.Field,Object> profile=new LinkedHashMap<>();
        final Map<Integer,net.minecraft.world.effect.MobEffectInstance> shadowEntries=new LinkedHashMap<>();
        final Map<java.lang.reflect.Field,Object> shadowFields=new LinkedHashMap<>();
        final List<Entity> created=new ArrayList<>();final ItemStack[][] pieces=new ItemStack[2][4];
        Fixture(GameController game){this.game=game;mana=game.items().mana();owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));beta=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));
            require(game.playing()&&game.canUseGameplayItems(owner)&&owner.containerMenu==owner.inventoryMenu,"active closed-inventory participant");
            for(var item:List.of(Items.LEATHER_BOOTS,Items.LEATHER_LEGGINGS,Items.LEATHER_CHESTPLATE,Items.LEATHER_HELMET)){var stat=net.minecraft.stats.Stats.ITEM_CRAFTED.get(item);stats.put(stat,owner.getStats().getValue(stat));}
            betaSaved=new GameChecks.WandThirdPlayerState(beta,mana);saved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());for(var h:net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.listElements().toList()){var a=owner.getAttribute(h);if(a!=null)attributes.getInstance(h).replaceFrom(a);}
            for(var slot:EquipmentSlot.values())equipment.put(slot,((Map<EquipmentSlot,ItemStack>)read(owner,"lastEquipmentItems")).getOrDefault(slot,ItemStack.EMPTY).copy());
            for(var a:game.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();owner.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(done::add);advancements.put(a,done);}
            for(var o:game.server().getScoreboard().getObjectives()){var score=game.server().getScoreboard().getPlayerScoreInfo(owner,o);if(score!=null)scores.put(o,score.value());}
            try{var pr=game.players().get(owner.getUUID());for(var f:pr.getClass().getFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){var value=f.get(pr);profile.put(f,value instanceof int[] a?a.clone():value instanceof Set<?> v?new HashSet<>(v):value);}var shadow=ShadowAbsorption.state(owner);shadow.entries.forEach((k,v)->shadowEntries.put(k,new net.minecraft.world.effect.MobEffectInstance(v)));for(var f:ShadowAbsorption.State.class.getFields())if(!f.getName().equals("entries"))shadowFields.put(f,f.get(shadow));}catch(IllegalAccessException e){throw new IllegalStateException(e);}
            for(var tag:List.of("time_stopped","spectator","strict_map_object","magic_academy","rcdenyalways","sin_greed_active","nomagic_active"))owner.entityTags().remove(tag);
            owner.removeAllEffects();owner.getInventory().clearContent();wear(0,0);equipmentUpdate(owner);mana.clearEffects(owner);mana.baseRegen(owner,10);
            owner.teleportTo(owner.level(),owner.getX(),owner.level().getMaxY()-24,owner.getZ(),Set.of(),0,-90,true);beta.teleportTo(owner.level(),owner.getX()+2,owner.getY(),owner.getZ(),Set.of(),0,0,true);
        }
        List<ItemStack> roll(String id){require(LootTestCatalog.isSuggested(Identifier.parse("lbc:"+id)),"public source readiness "+id);return game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:"+id);}
        void wear(int family,int count){for(int n=0;n<4;n++)owner.setItemSlot(SLOTS[n],n<count?pieces[family][n].copy():ItemStack.EMPTY);mana.inventoryChanged(owner);}
        void apply(){equipmentUpdate(owner);mana.inventoryChanged(owner);}

        private static final String[] SOURCE_COMPONENTS={
            "{\"minecraft:item_model\":\"lbc:armors/mage_boots\",\"minecraft:tooltip_style\":\"lbc:tier_a\",\"minecraft:max_damage\":300,\"minecraft:enchantments\":{\"minecraft:fire_protection\":2,\"minecraft:blast_protection\":2,\"minecraft:projectile_protection\":2},\"minecraft:equippable\":{\"slot\":\"feet\",\"asset_id\":\"lbc:mage\"},\"minecraft:custom_name\":[{\"translate\":\"mage_boots\",\"italic\":false}],\"minecraft:lore\":[[{\"translate\":\"type\",\"italic\":false,\"color\":\"aqua\"},{\"translate\":\"boots\",\"italic\":false,\"color\":\"aqua\"}],{\"translate\":\"mage_hat.lore.1\",\"color\":\"aqua\",\"italic\":false},{\"translate\":\"mage_hat.lore.2\",\"color\":\"aqua\",\"italic\":false},[{\"translate\":\"set_bonus\",\"italic\":false,\"color\":\"aqua\"}],[{\"translate\":\"set_bonus_mage\",\"italic\":false,\"color\":\"aqua\"}],{\"translate\":\"craft_with_phantom_boots\",\"italic\":false,\"color\":\"yellow\"}],\"minecraft:custom_data\":\"{craftusable:1,transformable:1,boots:1,mage:1}\"}",
            "{\"minecraft:item_model\":\"lbc:armors/mage_leggings\",\"minecraft:tooltip_style\":\"lbc:tier_a\",\"minecraft:max_damage\":300,\"minecraft:enchantments\":{\"minecraft:fire_protection\":4,\"minecraft:blast_protection\":4,\"minecraft:projectile_protection\":4},\"minecraft:equippable\":{\"slot\":\"legs\",\"asset_id\":\"lbc:mage\"},\"minecraft:custom_name\":[{\"translate\":\"mage_leggings\",\"italic\":false}],\"minecraft:lore\":[[{\"translate\":\"type\",\"italic\":false,\"color\":\"aqua\"},{\"translate\":\"leggings\",\"italic\":false,\"color\":\"aqua\"}],{\"translate\":\"mage_hat.lore.1\",\"color\":\"aqua\",\"italic\":false},{\"translate\":\"mage_hat.lore.2\",\"color\":\"aqua\",\"italic\":false},[{\"translate\":\"set_bonus\",\"italic\":false,\"color\":\"aqua\"}],[{\"translate\":\"set_bonus_mage\",\"italic\":false,\"color\":\"aqua\"}],{\"translate\":\"craft_with_phantom_leggings\",\"italic\":false,\"color\":\"yellow\"}],\"minecraft:custom_data\":\"{craftusable:1,transformable:1,leggings:1,mage:1}\"}",
            "{\"minecraft:item_model\":\"lbc:armors/mage_mantle\",\"minecraft:tooltip_style\":\"lbc:tier_a\",\"minecraft:max_damage\":300,\"minecraft:enchantments\":{\"minecraft:fire_protection\":5,\"minecraft:blast_protection\":5,\"minecraft:projectile_protection\":5},\"minecraft:equippable\":{\"slot\":\"chest\",\"asset_id\":\"lbc:mage\"},\"minecraft:custom_name\":[{\"translate\":\"mage_mantle\",\"italic\":false}],\"minecraft:lore\":[[{\"translate\":\"type\",\"italic\":false,\"color\":\"aqua\"},{\"translate\":\"chestplate\",\"italic\":false,\"color\":\"aqua\"}],{\"translate\":\"mage_hat.lore.1\",\"color\":\"aqua\",\"italic\":false},{\"translate\":\"mage_hat.lore.2\",\"color\":\"aqua\",\"italic\":false},[{\"translate\":\"set_bonus\",\"italic\":false,\"color\":\"aqua\"}],[{\"translate\":\"set_bonus_mage\",\"italic\":false,\"color\":\"aqua\"}],{\"translate\":\"craft_with_phantom_chestplate\",\"italic\":false,\"color\":\"yellow\"}],\"minecraft:custom_data\":\"{craftusable:1,transformable:1,chestplate:1,mage:1}\"}",
            "{\"minecraft:item_model\":\"lbc:armors/mage_hat\",\"minecraft:tooltip_style\":\"lbc:tier_a\",\"minecraft:max_damage\":300,\"minecraft:enchantments\":{\"minecraft:fire_protection\":2,\"minecraft:blast_protection\":2,\"minecraft:projectile_protection\":2},\"minecraft:equippable\":{\"slot\":\"head\",\"asset_id\":\"lbc:mage\"},\"minecraft:custom_name\":[{\"translate\":\"mage_hat\",\"italic\":false}],\"minecraft:lore\":[[{\"translate\":\"type\",\"italic\":false,\"color\":\"aqua\"},{\"translate\":\"helmet\",\"italic\":false,\"color\":\"aqua\"}],{\"translate\":\"mage_hat.lore.1\",\"color\":\"aqua\",\"italic\":false},{\"translate\":\"mage_hat.lore.2\",\"color\":\"aqua\",\"italic\":false},[{\"translate\":\"set_bonus\",\"italic\":false,\"color\":\"aqua\"}],[{\"translate\":\"set_bonus_mage\",\"italic\":false,\"color\":\"aqua\"}],{\"translate\":\"craft_with_phantom_helmet\",\"italic\":false,\"color\":\"yellow\"}],\"minecraft:custom_data\":\"{craftusable:1,transformable:1,helmet:1,mage:1}\"}",
            "{\"minecraft:item_model\":\"lbc:armors/nyauwu_boots\",\"minecraft:tooltip_style\":\"lbc:tier_s\",\"minecraft:max_damage\":1800,\"minecraft:enchantments\":{\"minecraft:fire_protection\":5,\"minecraft:blast_protection\":5,\"minecraft:projectile_protection\":5},\"minecraft:equippable\":{\"slot\":\"feet\",\"asset_id\":\"lbc:nyauwu\"},\"minecraft:attribute_modifiers\":[{\"type\":\"minecraft:movement_speed\",\"slot\":\"feet\",\"id\":\"ghfhstgfhjymnjkhed\",\"amount\":0.2,\"operation\":\"add_multiplied_base\"},{\"type\":\"minecraft:armor\",\"slot\":\"feet\",\"id\":\"ghfhstgfhjymnjjujujkhed\",\"amount\":3,\"operation\":\"add_value\"}],\"minecraft:custom_name\":[{\"translate\":\"nyauwu_boots\",\"italic\":false}],\"minecraft:lore\":[[{\"translate\":\"type\",\"italic\":false,\"color\":\"aqua\"},{\"translate\":\"boots\",\"italic\":false,\"color\":\"aqua\"}],{\"translate\":\"nyauwu_boots.lore.1\",\"color\":\"white\",\"italic\":false},{\"translate\":\"nyauwu_boots.lore.2\",\"color\":\"white\",\"italic\":false},[{\"translate\":\"set_bonus_nyauwu.1\",\"italic\":false,\"color\":\"gold\"}],[{\"translate\":\"set_bonus_nyauwu.2\",\"italic\":false,\"color\":\"gold\"}],[{\"translate\":\"set_bonus_nyauwu.3\",\"italic\":false,\"color\":\"gold\"}],[{\"translate\":\"set_bonus_nyauwu.4\",\"italic\":false,\"color\":\"gold\"}],[{\"translate\":\"set_bonus_nyauwu.5\",\"italic\":false,\"color\":\"gold\"}],[{\"translate\":\"mage_compatible\",\"italic\":false,\"color\":\"aqua\"}]],\"minecraft:custom_data\":\"{transformable:1,boots:1,nyauwu:1,mage:1}\"}",
            "{\"minecraft:item_model\":\"lbc:armors/nyauwu_leggings\",\"minecraft:tooltip_style\":\"lbc:tier_s\",\"minecraft:max_damage\":1800,\"minecraft:enchantments\":{\"minecraft:fire_protection\":5,\"minecraft:blast_protection\":5,\"minecraft:projectile_protection\":5},\"minecraft:equippable\":{\"slot\":\"legs\",\"asset_id\":\"lbc:nyauwu\"},\"minecraft:attribute_modifiers\":[{\"type\":\"minecraft:max_health\",\"slot\":\"legs\",\"id\":\"ghfh43534645646hhtkhed\",\"amount\":4,\"operation\":\"add_value\"},{\"type\":\"minecraft:armor\",\"slot\":\"legs\",\"id\":\"ghfherg6rjtyu476978978yhued\",\"amount\":7,\"operation\":\"add_value\"}],\"minecraft:custom_name\":[{\"translate\":\"nyauwu_leggings\",\"italic\":false}],\"minecraft:lore\":[[{\"translate\":\"type\",\"italic\":false,\"color\":\"aqua\"},{\"translate\":\"leggings\",\"italic\":false,\"color\":\"aqua\"}],{\"translate\":\"nyauwu_leggings.lore.1\",\"color\":\"white\",\"italic\":false},{\"translate\":\"nyauwu_leggings.lore.2\",\"color\":\"white\",\"italic\":false},[{\"translate\":\"set_bonus_nyauwu.1\",\"italic\":false,\"color\":\"gold\"}],[{\"translate\":\"set_bonus_nyauwu.2\",\"italic\":false,\"color\":\"gold\"}],[{\"translate\":\"set_bonus_nyauwu.3\",\"italic\":false,\"color\":\"gold\"}],[{\"translate\":\"set_bonus_nyauwu.4\",\"italic\":false,\"color\":\"gold\"}],[{\"translate\":\"set_bonus_nyauwu.5\",\"italic\":false,\"color\":\"gold\"}],[{\"translate\":\"mage_compatible\",\"italic\":false,\"color\":\"aqua\"}]],\"minecraft:custom_data\":\"{transformable:1,leggings:1,nyauwu:1,mage:1}\"}",
            "{\"minecraft:item_model\":\"lbc:armors/nyauwu_chestplate\",\"minecraft:tooltip_style\":\"lbc:tier_s\",\"minecraft:max_damage\":1800,\"minecraft:enchantments\":{\"minecraft:fire_protection\":5,\"minecraft:blast_protection\":5,\"minecraft:projectile_protection\":5},\"minecraft:equippable\":{\"slot\":\"chest\",\"asset_id\":\"lbc:nyauwu\"},\"minecraft:attribute_modifiers\":[{\"type\":\"minecraft:water_movement_efficiency\",\"slot\":\"chest\",\"id\":\"ghfh438juylhumhgfjmluilulkiohed\",\"amount\":1,\"operation\":\"add_value\"},{\"type\":\"minecraft:armor\",\"slot\":\"chest\",\"id\":\"ghfhstgfhsgdthtyjtukukujkhed\",\"amount\":9,\"operation\":\"add_value\"}],\"minecraft:custom_name\":[{\"translate\":\"nyauwu_chestplate\",\"italic\":false}],\"minecraft:lore\":[[{\"translate\":\"type\",\"italic\":false,\"color\":\"aqua\"},{\"translate\":\"chestplate\",\"italic\":false,\"color\":\"aqua\"}],{\"translate\":\"nyauwu_chestplate.lore.1\",\"color\":\"white\",\"italic\":false},{\"translate\":\"nyauwu_chestplate.lore.2\",\"color\":\"white\",\"italic\":false},[{\"translate\":\"set_bonus_nyauwu.1\",\"italic\":false,\"color\":\"gold\"}],[{\"translate\":\"set_bonus_nyauwu.2\",\"italic\":false,\"color\":\"gold\"}],[{\"translate\":\"set_bonus_nyauwu.3\",\"italic\":false,\"color\":\"gold\"}],[{\"translate\":\"set_bonus_nyauwu.4\",\"italic\":false,\"color\":\"gold\"}],[{\"translate\":\"set_bonus_nyauwu.5\",\"italic\":false,\"color\":\"gold\"}],[{\"translate\":\"mage_compatible\",\"italic\":false,\"color\":\"aqua\"}]],\"minecraft:custom_data\":\"{transformable:1,chestplate:1,nyauwu:1,mage:1}\"}",
            "{\"minecraft:item_model\":\"lbc:armors/nyauwu_helmet\",\"minecraft:tooltip_style\":\"lbc:tier_s\",\"minecraft:max_damage\":1800,\"minecraft:enchantments\":{\"minecraft:fire_protection\":5,\"minecraft:blast_protection\":5,\"minecraft:projectile_protection\":5},\"minecraft:equippable\":{\"slot\":\"head\",\"asset_id\":\"lbc:nyauwu\"},\"minecraft:attribute_modifiers\":[{\"type\":\"minecraft:sneaking_speed\",\"slot\":\"head\",\"id\":\"ghfh438jh5rfdfdfdfd6hhtkhed\",\"amount\":1,\"operation\":\"add_value\"},{\"type\":\"minecraft:armor\",\"slot\":\"head\",\"id\":\"ghfherg6rjh6hjtujkhed\",\"amount\":4,\"operation\":\"add_value\"}],\"minecraft:custom_name\":[{\"translate\":\"nyauwu_helmet\",\"italic\":false}],\"minecraft:lore\":[[{\"translate\":\"type\",\"italic\":false,\"color\":\"aqua\"},{\"translate\":\"helmet\",\"italic\":false,\"color\":\"aqua\"}],{\"translate\":\"nyauwu_helmet.lore.1\",\"color\":\"white\",\"italic\":false},{\"translate\":\"nyauwu_helmet.lore.2\",\"color\":\"white\",\"italic\":false},[{\"translate\":\"set_bonus_nyauwu.1\",\"italic\":false,\"color\":\"gold\"}],[{\"translate\":\"set_bonus_nyauwu.2\",\"italic\":false,\"color\":\"gold\"}],[{\"translate\":\"set_bonus_nyauwu.3\",\"italic\":false,\"color\":\"gold\"}],[{\"translate\":\"set_bonus_nyauwu.4\",\"italic\":false,\"color\":\"gold\"}],[{\"translate\":\"set_bonus_nyauwu.5\",\"italic\":false,\"color\":\"gold\"}],[{\"translate\":\"mage_compatible\",\"italic\":false,\"color\":\"aqua\"}]],\"minecraft:custom_data\":\"{transformable:1,helmet:1,nyauwu:1,mage:1}\"}"
        };
        void acquisition(){
            var ops=owner.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
            for(int family=0;family<2;family++)for(int n=0;n<4;n++){
                String leaf="neko_set_"+(family==0?"mage":"femboy")+"/"+PARTS[n];
                var out=roll(leaf);require(out.size()==1&&out.getFirst().getCount()==1,"one original leaf "+leaf);
                var actual=pieces[family][n]=out.getFirst();
                require(actual.is(List.of(Items.LEATHER_BOOTS,Items.LEATHER_LEGGINGS,Items.LEATHER_CHESTPLATE,Items.LEATHER_HELMET).get(n))&&actual.getDamageValue()==0,"original undamaged leather "+leaf);
                var json=com.google.gson.JsonParser.parseString(SOURCE_COMPONENTS[family*4+n]).getAsJsonObject();
                for(var entry:json.entrySet()){
                    var type=net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.parse(entry.getKey()));
                    require(type!=null,"registered source component "+entry.getKey());
                    Object expected=type.codecOrThrow().parse(ops,entry.getValue()).getOrThrow();
                    require(Objects.equals(actual.get(type),expected),"exact original component "+leaf+" "+entry.getKey());
                }

            }
            for(int family=0;family<2;family++){
                final int f=family;String root=family==0?"neko_set_mage":"neko_set_nyauwu";
                var full=roll(family==0?"neko_set_mage_full":"neko_set_femboy_full");require(full.size()==4,"four exact full outputs");
                for(var piece:pieces[f])require(full.stream().anyMatch(item->ItemStack.matches(item,piece)),"full retains exact leaf");
                var seen=new HashSet<Integer>();for(int attempt=0;attempt<256&&seen.size()<4;attempt++){
                    var out=roll(root);require(out.size()==1,"one original root draw");int index=-1;
                    for(int n=0;n<4;n++)if(ItemStack.matches(out.getFirst(),pieces[f][n]))index=n;
                    require(index>=0,"root retains exact leaf");seen.add(index);
                }require(seen.size()==4,"all original root branches");
            }
            var pre=roll("neko_set_mage_pre");require(pre.size()==5,"Mage five original pre draws");
            for(var item:pre)require(Arrays.stream(pieces[0]).anyMatch(piece->ItemStack.matches(item,piece)),"pre exact Mage leaf");
        }
        @SuppressWarnings("unchecked") void reforges(){var at=owner.position();
            for(int family=0;family<2;family++)for(int n=0;n<4;n++){
                require(LbcBolts.reforgeTables(pieces[family][n],false).equals(List.of("transformables_"+PARTS[n]))&&LbcBolts.reforgeTables(pieces[family][n],true).equals(List.of(family==0?"neko_set_mage":"neko_set_nyauwu")),"original normal slot and Ultra family routes");
                var table=game.server().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,Identifier.parse("lbc:transformables_"+PARTS[n])));
                var pool=((List<net.minecraft.world.level.storage.loot.LootPool>)read(table,"pools")).getFirst();var sequence=(Optional<Identifier>)read(table,"randomSequence");require(sequence.isEmpty(),"ordinary reforge uses native level random");
                var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(owner.level()).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,at).withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,owner).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.COMMAND);var context=new net.minecraft.world.level.storage.loot.LootContext.Builder(params).create(sequence);int total=0,offset=-1,weight=0;
                for(int i=0;i<pool.entries.size();i++){var expanded=new ArrayList<net.minecraft.world.level.storage.loot.entries.LootPoolEntry>();pool.entries.get(i).expand(context,expanded::add);for(var e:expanded){int w=e.getWeight(context.getLuck());if(w<=0)continue;if(i==4){if(offset<0)offset=total;weight+=w;}total+=w;}}require(weight==1&&offset>=0,"original weight1 Mage slot branch");int bound=total,pick=offset;Object random=read(owner.level(),"random");int[] calls={0};var donor=new net.minecraft.world.entity.item.ItemEntity(owner.level(),at.x,at.y,at.z,pieces[family][n].copy());donor.setNoGravity(true);require(owner.level().addFreshEntity(donor),"actual normal reforge donor");created.add(donor);var out=new ArrayList<ItemStack>();
                try{set(owner.level(),"random",java.lang.reflect.Proxy.newProxyInstance(net.minecraft.util.RandomSource.class.getClassLoader(),new Class<?>[]{net.minecraft.util.RandomSource.class},(proxy,m,args)->{if(m.getName().equals("nextInt")&&args!=null&&args.length==1&&args[0].equals(bound)){calls[0]++;return pick;}try{return m.invoke(random,args);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}}));reforge(donor,LbcBolts.ReforgeFamily.NORMAL,out);require(calls[0]==1&&donor.isRemoved()&&out.size()==1&&ItemStack.matches(out.getFirst(),pieces[0][n]),"actual native normal reforge consumes source donor and retains exact slot leaf");}finally{set(owner.level(),"random",random);donor.discard();}
                donor=new net.minecraft.world.entity.item.ItemEntity(owner.level(),at.x,at.y,at.z,pieces[family][n].copy());donor.setNoGravity(true);require(owner.level().addFreshEntity(donor),"actual Ultra source donor");created.add(donor);out.clear();reforge(donor,LbcBolts.ReforgeFamily.ULTRA,out);require(donor.isRemoved()&&out.size()==1&&Arrays.stream(pieces[family]).anyMatch(p->ItemStack.matches(p,out.getFirst())),"actual native Ultra reforge follows own family root");
            }
        }
        void reforge(net.minecraft.world.entity.item.ItemEntity donor,LbcBolts.ReforgeFamily family,List<ItemStack> outputs){try{var method=LbcBolts.class.getDeclaredMethod("reforgeInput",net.minecraft.world.entity.item.ItemEntity.class,LbcBolts.ReforgeFamily.class,int.class,ServerPlayer.class,java.util.function.Consumer.class);method.setAccessible(true);var work=(LbcStonekivi.Work)method.invoke(game.items().bolts(),donor,family,1,owner,(java.util.function.Consumer<ItemStack>)outputs::add);require(work!=null&&work.step(),"existing native reforge Work completes");}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}


        void mage(){
            int[] perSlot={4,7,6,4};
            for(int family=0;family<2;family++){
                for(int n=0;n<4;n++){
                    wear(family,0);owner.setItemSlot(SLOTS[n],pieces[family][n].copy());apply();mana.set(owner,0);mana.maximum(owner,5000);mana.tickPlayer(owner);
                    require(mana.get(owner)==10+perSlot[n]&&mana.regeneration(owner)==10+perSlot[n]&&!mana.mageFullSet(owner),"actual shared mana slot "+family+":"+PARTS[n]);
                }
                for(int n=0;n<=4;n++){
                    wear(family,n);apply();mana.maximum(owner,5000);mana.set(owner,1000);int before=mana.get(owner);
                    int expected=10;for(int j=0;j<n;j++)expected+=perSlot[j];mana.tickPlayer(owner);
                    require(mana.get(owner)==before+expected&&mana.mageFullSet(owner)==(n==4),"native partial/full regen and Mage compatibility "+family+":"+n);
                }
            }
            wear(0,0);for(int n=0;n<4;n++)owner.getInventory().setItem(9+n,pieces[0][n].copy());owner.setItemInHand(InteractionHand.OFF_HAND,pieces[1][3].copy());apply();mana.set(owner,0);mana.tickPlayer(owner);
            require(mana.get(owner)==10&&!mana.mageFullSet(owner),"carried/offhand armor grants neither regen nor full bonus");owner.getInventory().clearContent();
            for(int raw:new int[]{0,2}){wear(0,4);var altered=pieces[0][3].copy();var tag=LbcFartus.data(altered).copy();tag.putInt("mage",raw);altered.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));owner.setItemSlot(EquipmentSlot.HEAD,altered);apply();require(!mana.mageFullSet(owner),"exact Mage flag "+raw);}
            wear(0,4);owner.setItemSlot(EquipmentSlot.HEAD,pieces[1][3].copy());apply();require(mana.mageFullSet(owner),"mixed Nyauwu/Mage compatibility");
        }
        void casts(){
            for(int family=0;family<2;family++)for(int count:new int[]{3,4}){
                wear(family,count);owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcWands.TINY_FIRE));owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);apply();
                mana.maximum(owner,5000);mana.set(owner,1000);int expected=count==4?5150:5000;
                var before=new HashSet<Entity>();owner.level().getAllEntities().forEach(before::add);
                try{require(game.items().wands().use(owner,InteractionHand.MAIN_HAND),"actual paid native wand cast");
                    require(mana.get(owner)==975&&mana.maximum(owner)==expected,"one actual cast grants +150 only with four mage flags "+family+":"+count);
                    long spawned=java.util.stream.StreamSupport.stream(owner.level().getAllEntities().spliterator(),false).filter(entity->!before.contains(entity)&&game.items().wands().isFireProjectile(entity)).count();
                    require(spawned==1,"actual cast produces one native fire projectile");
                }finally{owner.level().getAllEntities().forEach(entity->{if(!before.contains(entity)){created.add(entity);entity.discard();}});}
                mana.set(owner,0);int max=mana.maximum(owner);require(!game.items().wands().use(owner,InteractionHand.MAIN_HAND)&&mana.maximum(owner)==max,"failed cast grants no maximum mana");
            }
        }

        void crafting(){
            var phantom=new ItemStack[4];for(int n=0;n<4;n++){var out=roll("neko_set_phantom/"+PARTS[n]);require(out.size()==1,"native Phantom source");phantom[n]=out.getFirst();}
            wear(0,0);owner.getInventory().clearContent();owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            var menu=owner.inventoryMenu;var grid=menu.getInputGridSlots();
            for(int n=0;n<4;n++){
                grid.forEach(slot->slot.setByPlayer(ItemStack.EMPTY));menu.setCarried(ItemStack.EMPTY);
                var mage=pieces[0][n].copyWithCount(2);var ph=phantom[n].copyWithCount(3);
                mage.setDamageValue(17);ph.setDamageValue(31);
                grid.getFirst().setByPlayer(mage);grid.getLast().setByPlayer(ph);
                require(ItemStack.matches(menu.getResultSlot().getItem(),pieces[1][n]),"native 2x2 exact Nyauwu preview "+PARTS[n]);
                for(int attempt=0;attempt<3;attempt++)menu.slotsChanged(owner.getInventory());
                require(ItemStack.matches(grid.getFirst().getItem(),mage)&&ItemStack.matches(grid.getLast().getItem(),ph),"native preview leaves both input counts/components untouched");
                var three=net.minecraft.world.item.crafting.CraftingInput.of(3,3,List.of(ItemStack.EMPTY,mage.copy(),ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY,ph.copy(),ItemStack.EMPTY,ItemStack.EMPTY));
                var kind=LbcNyauwuCrafting.kind(three);require(kind!=null&&kind.recipe.matches(three,owner.level())&&ItemStack.matches(kind.recipe.assemble(three),pieces[1][n]),"native 3x3 registered recipe exact output");
                GameChecks.inventoryClick(owner,0,0,net.minecraft.world.inventory.ContainerInput.PICKUP);
                require(ItemStack.matches(menu.getCarried(),pieces[1][n]),"native ResultSlot transfers exact Nyauwu output");
                require(ItemStack.matches(grid.getFirst().getItem(),mage.copyWithCount(1))&&ItemStack.matches(grid.getLast().getItem(),ph.copyWithCount(2)),"native ResultSlot consumes one each preserving surplus components");
                grid.forEach(slot->slot.setByPlayer(ItemStack.EMPTY));menu.setCarried(ItemStack.EMPTY);
                var mismatch=net.minecraft.world.item.crafting.CraftingInput.of(2,1,List.of(pieces[0][n].copy(),phantom[(n+1)%4].copy()));
                require(LbcNyauwuCrafting.kind(mismatch)==null,"different parts rejected");
                var bad=pieces[0][n].copy();var tag=LbcFartus.data(bad).copy();tag.putInt("mage",2);bad.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
                require(LbcNyauwuCrafting.kind(net.minecraft.world.item.crafting.CraftingInput.of(2,1,List.of(bad,phantom[n].copy())))==null,"raw flag2 rejected");
                bad=pieces[0][n].copy();bad.set(DataComponents.EQUIPPABLE,phantom[n].get(DataComponents.EQUIPPABLE));
                require(LbcNyauwuCrafting.kind(net.minecraft.world.item.crafting.CraftingInput.of(2,1,List.of(bad,phantom[n].copy())))==null,"wrong asset rejected");
            }
            var service=(LbcNyauwuCrafting)read(game.items(),"nyauwuCrafting");
            var tracked=(Set<net.minecraft.world.entity.item.ItemEntity>)read(service,"items");var prior=new HashSet<>(tracked);int phase=(int)read(service,"phase");
            var jammer=new Marker(EntityTypes.MARKER,owner.level());jammer.setPos(owner.position());jammer.entityTags().add("stopper_magic");created.add(jammer);require(owner.level().addFreshEntity(jammer),"native ground crafting jammer");game.items().magic().loaded(jammer);
            try{tracked.clear();mana.set(owner,0);require(game.items().magic().beaconSuppressed(owner.position()),"actual jammer active for physical craft");
                for(int n=0;n<4;n++){
                    var at=owner.position().add(0,2,0);var mage=new net.minecraft.world.entity.item.ItemEntity(owner.level(),at.x,at.y,at.z,pieces[0][n].copyWithCount(2));
                    var ph=new net.minecraft.world.entity.item.ItemEntity(owner.level(),at.x+.25,at.y,at.z,phantom[n].copyWithCount(3));mage.setNoGravity(true);ph.setNoGravity(true);created.add(mage);created.add(ph);
                    require(owner.level().addFreshEntity(mage)&&owner.level().addFreshEntity(ph),"native ground donors published");service.loaded(mage);service.loaded(ph);
                    var before=new HashSet<Entity>();owner.level().getAllEntities().forEach(before::add);set(service,"phase",19);service.tick();
                    var outputs=new ArrayList<net.minecraft.world.entity.item.ItemEntity>();owner.level().getAllEntities().forEach(entity->{if(!before.contains(entity)){created.add(entity);if(entity instanceof net.minecraft.world.entity.item.ItemEntity item)outputs.add(item);}});
                    require(outputs.size()==1&&ItemStack.matches(outputs.getFirst().getItem(),pieces[1][n]),"tracked loaded/tick emits exact ground Nyauwu "+PARTS[n]);
                    require(mage.getItem().getCount()==1&&ph.getItem().getCount()==2&&mana.get(owner)==0,"ground consumes exactly one donor each with zero mana");
                    service.unloaded(mage);service.unloaded(ph);mage.discard();ph.discard();outputs.forEach(Entity::discard);
                }
            }finally{game.items().magic().unloaded(jammer);jammer.discard();for(var item:List.copyOf(tracked))if(!prior.contains(item))item.discard();tracked.clear();tracked.addAll(prior);set(service,"phase",phase);}
        }

        Marker jammer(String tag,net.minecraft.world.phys.Vec3 at){var marker=new Marker(EntityTypes.MARKER,owner.level());marker.setPos(at);marker.entityTags().add(tag);require(owner.level().addFreshEntity(marker),"native "+tag+" marker");created.add(marker);game.items().magic().loaded(marker);return marker;}
        void discardJammer(Marker marker){game.items().magic().unloaded(marker);marker.discard();}
        void near(double actual,double expected,String label){require(Math.abs(actual-expected)<.00001,label+" expected="+expected+" actual="+actual);}
        void nyauwuPassives(){
            var service=game.items().armorSets().nyauwu();var speed=owner.getAttribute(Attributes.MOVEMENT_SPEED);var id=Identifier.parse("minecraft:916c89e6-ae62-4b11-91d7-10e7e57c726d");
            var board=game.server().getScoreboard();var oldTeam=owner.getTeam();var oldBetaTeam=beta.getTeam();var team=board.addPlayerTeam("codex_nyauwu_passive");
            try{board.addPlayerToTeam(owner.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);
                wear(1,0);owner.setItemSlot(EquipmentSlot.HEAD,pieces[1][3].copy());apply();mana.maximum(owner,5000);mana.set(owner,0);mana.tickPlayer(owner);
                require(mana.maximum(owner)==5020&&mana.get(owner)==14,"allied other player triggers helmet +20 maximum on native shared tick");
                beta.teleportTo(owner.level(),owner.getX(),owner.getY()-40,owner.getZ(),Set.of(),0,0,true);mana.tickPlayer(owner);require(mana.maximum(owner)==5020,"helmet player outside ten blocks excluded");
                wear(1,0);owner.setItemSlot(EquipmentSlot.LEGS,pieces[1][1].copy());apply();mana.baseRegen(owner,13);mana.set(owner,0);mana.boost(owner,1);mana.tickPlayer(owner);
                require(mana.regeneration(owner)==60&&mana.get(owner)==60,"leggings doubles entire base13 plus Mage7 before native boost x1.5");
                var ally=cow(owner.position().add(2,0,0));board.addPlayerToTeam(ally.getScoreboardName(),team);mana.set(owner,0);mana.tickPlayer(owner);
                require(mana.get(owner)==20,"any allied source mob prevents leggings doubling");ally.discard();
                beta.teleportTo(owner.level(),owner.getX()+2,owner.getY(),owner.getZ(),Set.of(),0,0,true);mana.set(owner,0);mana.tickPlayer(owner);require(mana.get(owner)==20,"nearby allied player in source mobs prevents doubling");
                wear(1,0);owner.setItemSlot(EquipmentSlot.FEET,pieces[1][0].copy());apply();mana.maximum(owner,5000);mana.set(owner,4999);service.refreshSpeed(owner);
                var modifier=speed.getModifier(id);require(modifier!=null&&modifier.amount()==1&&modifier.operation()==AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,"exact original boots native modifier");
                var external=Identifier.parse("skywars2:nyauwu_check_neighbor");speed.addTransientModifier(new AttributeModifier(external,.13,AttributeModifier.Operation.ADD_VALUE));
                try{for(int n=0;n<5;n++)service.refreshSpeed(owner);require(speed.getModifier(id)==modifier,"boots reconciliation is idempotent");mana.set(owner,5000);service.refreshSpeed(owner);require(speed.getModifier(id)==null&&speed.getModifier(external)!=null,"full mana removes only boots modifier");
                    mana.set(owner,4999);service.refreshSpeed(owner);var tech=jammer("stopper",owner.position());try{require(game.items().magic().mechanismSuppressed(owner),"TECH positive control");service.refreshSpeed(owner);require(speed.getModifier(id)!=null,"TECH does not suppress magical boots");}finally{discardJammer(tech);}
                    var mag=jammer("stopper_magic",owner.position());try{require(game.items().magic().beaconSuppressed(owner.position()),"MAG positive control");service.refreshSpeed(owner);require(speed.getModifier(id)==null&&speed.getModifier(external)!=null,"MAG removes boots source ID only");
                        wear(1,4);apply();mana.maximum(owner,5000);require(service.beforeRegeneration(owner,37)==37&&mana.maximum(owner)==5000,"MAG blocks helmet and leggings generation");
                    }finally{discardJammer(mag);}
                    wear(1,0);owner.setItemSlot(EquipmentSlot.FEET,pieces[1][0].copy());apply();mana.set(owner,1);service.refreshSpeed(owner);owner.entityTags().add("time_stopped");try{service.refreshSpeed(owner);require(speed.getModifier(id)!=null,"time stop preserves existing boots modifier");owner.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);service.refreshSpeed(owner);require(speed.getModifier(id)==null,"unequip removes boots even during time stop");}finally{owner.entityTags().remove("time_stopped");}
                    for(var role:List.of("spectator","strict_map_object","magic_academy","rcdenyalways")){wear(1,0);owner.setItemSlot(EquipmentSlot.FEET,pieces[1][0].copy());apply();service.refreshSpeed(owner);require(speed.getModifier(id)!=null,"role cleanup positive control");owner.entityTags().add(role);try{service.refreshSpeed(owner);require(speed.getModifier(id)==null,"protected role removes boots "+role);}finally{owner.entityTags().remove(role);}}
                }finally{speed.removeModifier(external);}
                wear(1,0);owner.setItemSlot(EquipmentSlot.CHEST,pieces[1][2].copy());apply();mana.baseRegen(owner,10);mana.maximum(owner,5000);mana.set(owner,5000);
                var water=owner.blockPosition();var previous=game.maps().snapshot(owner.level(),water);
                try{owner.level().setBlock(water,net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(),2);mana.tickPlayer(owner);require(mana.get(owner)==5001,"water chest adds one above full mana without cap");mana.tickPlayer(owner);require(mana.get(owner)==5002,"water chest overcap survives consecutive shared regeneration ticks");}
                finally{game.maps().restoreSnapshot(owner.level(),water,previous);}
                wear(1,0);service.refreshSpeed(owner);require(speed.getModifier(id)==null,"final unequip reconciles boots");
            }finally{board.removePlayerTeam(team);if(oldTeam!=null)board.addPlayerToTeam(owner.getScoreboardName(),oldTeam);if(oldBetaTeam!=null)board.addPlayerToTeam(beta.getScoreboardName(),oldBetaTeam);mana.baseRegen(owner,10);}
        }
        net.minecraft.world.entity.animal.cow.Cow cow(net.minecraft.world.phys.Vec3 at){var mob=new net.minecraft.world.entity.animal.cow.Cow(EntityTypes.COW,owner.level());mob.setPos(at);mob.setNoAi(true);mob.setNoGravity(true);mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);mob.setHealth(200);require(owner.level().addFreshEntity(mob),"published native pulse cow");created.add(mob);return mob;}
        void effectTick(LivingEntity entity){try{var method=LivingEntity.class.getDeclaredMethod("tickEffects");method.setAccessible(true);method.invoke(entity);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
        boolean paidPulse(){owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcWands.TINY_FIRE));owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);var before=new HashSet<Entity>();owner.level().getAllEntities().forEach(before::add);try{return game.items().wands().use(owner,InteractionHand.MAIN_HAND);}finally{owner.level().getAllEntities().forEach(entity->{if(!before.contains(entity)){created.add(entity);if(game.items().wands().isFireProjectile(entity))entity.discard();}});}}
        void nyauwuMelee(){
            var service=game.items().armorSets().nyauwu();wear(1,0);owner.setItemSlot(EquipmentSlot.CHEST,pieces[1][2].copy());owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);apply();mana.baseRegen(owner,10);mana.maximum(owner,100000);mana.set(owner,0);mana.tickPlayer(owner);int visual=mana.regeneration(owner);require(visual==16,"native chest visual baseline");
            for(int hit=0;hit<2;hit++){var target=cow(owner.position().add(2+hit,0,0));int before=mana.get(owner);require(target.hurtServer(owner.level(),owner.damageSources().playerAttack(owner),1),"accepted native direct melee hit");require(mana.get(owner)==before+visual*70&&mana.regeneration(owner)==visual,"actual melee restores visual times70 without changing or compounding visual "+hit);target.discard();}
            var arrow=new net.minecraft.world.entity.projectile.arrow.Arrow(EntityTypes.ARROW,owner.level());arrow.setOwner(owner);created.add(arrow);var target=cow(owner.position().add(3,0,0));int before=mana.get(owner);require(target.hurtServer(owner.level(),owner.damageSources().arrow(arrow,owner),1),"accepted attributed native arrow control");require(mana.get(owner)==before&&mana.regeneration(owner)==visual,"arrow attribution cannot trigger chest melee bonus");target.discard();
            var mag=jammer("stopper_magic",owner.position());try{target=cow(owner.position().add(3,0,0));require(target.hurtServer(owner.level(),owner.damageSources().playerAttack(owner),1),"physical native melee still accepted under MAG");require(mana.get(owner)==before&&mana.regeneration(owner)==visual,"MAG blocks chest mana gain without mutating shared visual");target.discard();}finally{discardJammer(mag);}
            owner.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);apply();target=cow(owner.position().add(3,0,0));require(target.hurtServer(owner.level(),owner.damageSources().playerAttack(owner),1),"native melee without chest accepted");require(mana.get(owner)==before,"unequipped chest grants no hit regeneration");target.discard();
        }
        void nyauwuPulse(){
            var board=game.server().getScoreboard();var oldTeam=owner.getTeam();var oldBetaTeam=beta.getTeam();var team=board.addPlayerTeam("codex_nyauwu_pulse");
            try{board.addPlayerToTeam(owner.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);wear(1,4);apply();mana.maximum(owner,5000);mana.set(owner,1000);
                var enemy=cow(owner.position().add(3,0,0));enemy.entityTags().add("magic");var ordinary=cow(owner.position().add(4,0,0));var ally=cow(owner.position().add(5,0,0));board.addPlayerToTeam(ally.getScoreboardName(),team);
                var undead=new net.minecraft.world.entity.monster.zombie.Zombie(EntityTypes.ZOMBIE,owner.level());undead.setPos(owner.position().add(6,0,0));undead.setNoAi(true);undead.setNoGravity(true);undead.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);undead.setHealth(200);require(owner.level().addFreshEntity(undead),"native allied undead");created.add(undead);board.addPlayerToTeam(undead.getScoreboardName(),team);
                var distant=cow(owner.position().add(0,-31,0));distant.entityTags().add("summactivated");var protectedRole=cow(owner.position().add(7,0,0));protectedRole.entityTags().add("magic");protectedRole.entityTags().add("spectator");
                float playerHp=beta.getHealth();require(paidPulse(),"actual successful cast triggers Nyauwu full pulse");near(enemy.getHealth(),175,"exact native 25 arrow pulse on magical enemy");require(enemy.getLastHurtByPlayer()==owner,"native pulse damage credits wearer");near(ordinary.getHealth(),200,"ordinary enemy mob excluded");near(distant.getHealth(),200,"magical mob outside thirty excluded");near(protectedRole.getHealth(),200,"protected mob role excluded");near(beta.getHealth(),playerHp,"pulse never damages allied player");
                require(ally.hasEffect(MobEffects.INSTANT_HEALTH)&&ally.getEffect(MobEffects.INSTANT_HEALTH).getAmplifier()==1,"allied ordinary mob receives native Instant Health II");require(undead.hasEffect(MobEffects.INSTANT_DAMAGE)&&undead.getEffect(MobEffects.INSTANT_DAMAGE).getAmplifier()==1,"allied unholy mob receives native inverted Instant Damage II");effectTick(ally);effectTick(undead);near(ally.getHealth(),208,"native living ally healing");near(undead.getHealth(),208,"native undead inverted healing");
                mana.set(owner,0);require(!paidPulse(),"insufficient mana prevents actual cast");near(enemy.getHealth(),175,"failed cast emits no damage pulse");require(!ally.hasEffect(MobEffects.INSTANT_HEALTH),"failed cast emits no healing pulse");
                var target=cow(owner.position().add(0,-25,0));target.entityTags().add("magic");var targetAlly=cow(owner.position().add(0,-26,0));board.addPlayerToTeam(targetAlly.getScoreboardName(),team);var mag=jammer("stopper_magic",owner.position().add(0,-30,0));
                try{require(!game.items().magic().beaconSuppressed(owner.position())&&game.items().magic().beaconSuppressed(target.position()),"target MAG covers targets while caster remains outside");mana.set(owner,1000);require(paidPulse(),"cast outside target MAG still succeeds");near(target.getHealth(),200,"target MAG blocks magical enemy pulse");require(!targetAlly.hasEffect(MobEffects.INSTANT_HEALTH),"target MAG blocks ally healing pulse");}
                finally{discardJammer(mag);}
                wear(1,3);apply();mana.set(owner,1000);float before=target.getHealth();require(paidPulse(),"partial set still casts");near(target.getHealth(),before,"partial set emits no pulse");
            }finally{board.removePlayerTeam(team);if(oldTeam!=null)board.addPlayerToTeam(owner.getScoreboardName(),oldTeam);if(oldBetaTeam!=null)board.addPlayerToTeam(beta.getScoreboardName(),oldBetaTeam);}
        }

        void ownedPulse(){
            var board=game.server().getScoreboard();var oldOwner=owner.getTeam();var oldBeta=beta.getTeam();
            var team=board.addPlayerTeam("codex_nyauwu_owned");
            LbcSoulless.Field own=null,foreign=null;
            try{
                board.addPlayerToTeam(owner.getScoreboardName(),team);if(oldBeta!=null)board.removePlayerFromTeam(beta.getScoreboardName(),oldBeta);
                wear(1,4);apply();mana.maximum(owner,5000);mana.set(owner,1000);
                own=game.items().soulless().spawn(owner,owner.position().add(2,0,0),net.minecraft.world.phys.Vec3.ZERO,6);
                foreign=game.items().soulless().spawn(beta,owner.position().add(3,0,0),net.minecraft.world.phys.Vec3.ZERO,6);
                require(own!=null&&foreign!=null,"actual owned and foreign Metasola actors");
                require(own.pig.getTeam()==null&&foreign.pig.getTeam()==null,"logical owners rather than a fixture-assigned team");
                require(game.items().magic().magical(own.pig)&&game.items().magic().magical(foreign.pig),"shared classification recognizes actual managed hitboxes");
                own.pig.setHealth(20);float hp=foreign.pig.getHealth();require(paidPulse(),"actual successful cast reaches managed summons");
                near(foreign.pig.getHealth(),hp-25,"foreign magical hitbox receives25");
                require(foreign.pig.getLastHurtByPlayer()==owner,"foreign Metasola hurt credit belongs to caster");
                effectTick(own.pig);near(own.pig.getHealth(),28,"ownerless-team allied Metasola heals and is not damaged");
            }finally{
                if(own!=null)game.items().soulless().unloaded(own.pig);if(foreign!=null)game.items().soulless().unloaded(foreign.pig);
                board.removePlayerTeam(team);if(oldOwner!=null)board.addPlayerToTeam(owner.getScoreboardName(),oldOwner);if(oldBeta!=null)board.addPlayerToTeam(beta.getScoreboardName(),oldBeta);
            }
        }

        @SuppressWarnings("unchecked") public void close(){created.forEach(Entity::discard);stats.forEach((stat,value)->owner.getStats().setValue(owner,stat,value));mana.inventoryChanged(owner);saved.restore(game);beta.teleportTo(beta.level(),betaSaved.position.x,betaSaved.position.y,betaSaved.position.z,Set.of(),betaSaved.yaw,betaSaved.pitch,true);beta.setDeltaMovement(betaSaved.velocity);beta.connection.resetPosition();owner.getAttributes().assignAllValues(attributes);owner.setHealth(saved.health);owner.setAbsorptionAmount(saved.absorption);var cache=(Map<EquipmentSlot,ItemStack>)read(owner,"lastEquipmentItems");cache.clear();equipment.forEach((s,i)->cache.put(s,i.copy()));advancements.forEach((a,done)->{for(var k:a.value().criteria().keySet())if(done.contains(k))owner.getAdvancements().award(a,k);else owner.getAdvancements().revoke(a,k);});for(var o:game.server().getScoreboard().getObjectives())if(scores.containsKey(o))game.server().getScoreboard().getOrCreatePlayerScore(owner,o,true).set(scores.get(o));else game.server().getScoreboard().resetSinglePlayerScore(owner,o);
            try{for(var e:profile.entrySet())e.getKey().set(game.players().get(owner.getUUID()),e.getValue());var shadow=ShadowAbsorption.state(owner);shadow.entries.clear();shadow.entries.putAll(shadowEntries);for(var e:shadowFields.entrySet())e.getKey().set(shadow,e.getValue());}catch(IllegalAccessException e){throw new IllegalStateException(e);}owner.setHealth(saved.health);owner.setAbsorptionAmount(saved.absorption);mana.inventoryChanged(owner);owner.connection.resetPosition();}
    }
    public static void run(GameController game){try(var fixture=new Fixture(game)){fixture.acquisition();fixture.reforges();fixture.mage();fixture.casts();fixture.crafting();fixture.nyauwuPassives();fixture.nyauwuMelee();fixture.nyauwuPulse();fixture.ownedPulse();}SkyWars2.LOGGER.info("[check] PASS mage-nyauwu (thirteen source tables, exact source payloads/presentation/modifiers, native normal/Ultra reforges, shared native Mage regeneration/compatibility and actual successful/failed wand casts, native ResultSlot recipes and tracked ground fusion, Nyauwu native passives, MAG/TECH/roles, native typed pulse and allied inverted effects)");}
}
