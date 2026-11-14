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

/** Bounded native equipment/lifecycle and in-memory persistence checks; no reconnect/death claim. */
public final class FleshArmorChecks {
    private FleshArmorChecks(){}
    private static final EquipmentSlot[] SLOTS={EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD};
    private static final String[] PARTS={"boots","leggings","chestplate","helmet"};
    private static final Identifier BONUS=Identifier.parse("minecraft:44a8aa48-d53d-42c0-b229-4557d3c581b1");
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Flesh armor: "+why);}
    private static java.lang.reflect.Field field(Object value,String name){for(Class<?> c=value.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new IllegalStateException("Missing fixture field "+name);}
    private static Object read(Object value,String name){try{return field(value,name).get(value);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static void set(Object value,String name,Object next){try{field(value,name).set(value,next);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static void equipmentUpdate(LivingEntity player){try{var m=LivingEntity.class.getDeclaredMethod("detectEquipmentUpdates");m.setAccessible(true);m.invoke(player);var health=LivingEntity.class.getDeclaredMethod("onAttributeUpdated",net.minecraft.core.Holder.class);health.setAccessible(true);health.invoke(player,Attributes.MAX_HEALTH);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    private static net.minecraft.nbt.CompoundTag save(LivingEntity player){var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,player.registryAccess());net.fabricmc.fabric.api.networking.v1.context.PacketContext.supplyWithoutContext(()->{player.saveWithoutId(output);return null;});return output.buildResult();}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer owner,beta;final LbcFleshArmor service;
        final GameChecks.WandThirdPlayerState saved;
        final AttributeMap attributes=new AttributeMap(DefaultAttributes.getSupplier(EntityTypes.PLAYER));
        final Map<EquipmentSlot,ItemStack> equipment=new EnumMap<>(EquipmentSlot.class);
        final Map<net.minecraft.advancements.AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();
        final Map<net.minecraft.world.scores.Objective,Integer> scores=new LinkedHashMap<>();
        final Map<java.lang.reflect.Field,Object> profile=new LinkedHashMap<>();
        final Map<Integer,net.minecraft.world.effect.MobEffectInstance> shadowEntries=new LinkedHashMap<>();
        final Map<java.lang.reflect.Field,Object> shadowFields=new LinkedHashMap<>();
        final List<Entity> created=new ArrayList<>();final ItemStack[] pieces=new ItemStack[4];
        Fixture(GameController game){this.game=game;service=game.items().armorSets().flesh();owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));beta=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));
            require(game.playing()&&game.canUseGameplayItems(owner)&&owner.containerMenu==owner.inventoryMenu,"active closed-inventory participant");
            saved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());for(var h:net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.listElements().toList()){var a=owner.getAttribute(h);if(a!=null)attributes.getInstance(h).replaceFrom(a);}
            for(var slot:EquipmentSlot.values())equipment.put(slot,((Map<EquipmentSlot,ItemStack>)read(owner,"lastEquipmentItems")).getOrDefault(slot,ItemStack.EMPTY).copy());
            for(var a:game.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();owner.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(done::add);advancements.put(a,done);}
            for(var o:game.server().getScoreboard().getObjectives()){var score=game.server().getScoreboard().getPlayerScoreInfo(owner,o);if(score!=null)scores.put(o,score.value());}
            try{var pr=game.players().get(owner.getUUID());for(var f:pr.getClass().getFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){var value=f.get(pr);profile.put(f,value instanceof int[] a?a.clone():value instanceof Set<?> v?new HashSet<>(v):value);}var shadow=ShadowAbsorption.state(owner);shadow.entries.forEach((k,v)->shadowEntries.put(k,new net.minecraft.world.effect.MobEffectInstance(v)));for(var f:ShadowAbsorption.State.class.getFields())if(!f.getName().equals("entries"))shadowFields.put(f,f.get(shadow));}catch(IllegalAccessException e){throw new IllegalStateException(e);}
            for(var tag:List.of("time_stopped","spectator","strict_map_object","magic_academy","rcdenyalways","sin_greed_active","nomagic_active"))owner.entityTags().remove(tag);
            owner.removeAllEffects();owner.getInventory().clearContent();wear(0);equipmentUpdate(owner);var max=owner.getAttribute(Attributes.MAX_HEALTH);max.removeModifiers();max.setBaseValue(20);owner.setHealth(10);owner.setAbsorptionAmount(0);
        }
        List<ItemStack> roll(String id){require(LootTestCatalog.isSuggested(Identifier.parse("lbc:"+id)),"public source readiness "+id);return game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:"+id);}
        void wear(int count){for(int n=0;n<4;n++)owner.setItemSlot(SLOTS[n],n<count?pieces[n].copy():ItemStack.EMPTY);}
        void apply(){equipmentUpdate(owner);service.inventoryChanged(owner);service.refresh(owner);}
        void nativeArmorDrops(){
            float hp=owner.getHealth();
            try(var client=new RegenerationBeltChecks.ScheduledClient(game,owner)){
                owner.closeContainer();client.acknowledge();
                for(byte button:new byte[]{0,1}){
                    wear(4);apply();require(owner.getMaxHealth()==124&&owner.getAttribute(Attributes.MAX_HEALTH).hasModifier(BONUS),"real full Flesh set before native Q/Control-Q");
                    var menu=owner.containerMenu;var before=new HashSet<UUID>();owner.level().getAllEntities().forEach(e->before.add(e.getUUID()));
                    owner.connection.handleContainerClick(new net.minecraft.network.protocol.game.ServerboundContainerClickPacket(menu.containerId+1,menu.getStateId(),(short)5,button,net.minecraft.world.inventory.ContainerInput.THROW,it.unimi.dsi.fastutil.ints.Int2ObjectMaps.emptyMap(),net.minecraft.network.HashedStack.EMPTY));
                    require(!owner.getItemBySlot(EquipmentSlot.HEAD).isEmpty()&&owner.getAttribute(Attributes.MAX_HEALTH).hasModifier(BONUS),"rejected foreign-window packet leaves actual armor and bonus intact");
                    owner.connection.handleContainerClick(new net.minecraft.network.protocol.game.ServerboundContainerClickPacket(menu.containerId,menu.getStateId(),(short)5,button,net.minecraft.world.inventory.ContainerInput.THROW,it.unimi.dsi.fastutil.ints.Int2ObjectMaps.emptyMap(),net.minecraft.network.HashedStack.EMPTY));
                    require(owner.getItemBySlot(EquipmentSlot.HEAD).isEmpty()&&!owner.getAttribute(Attributes.MAX_HEALTH).hasModifier(BONUS)&&owner.getMaxHealth()==54,"Q/Control-Q removes the full-set and native piece bonuses before another server tick");
                    var drops=new ArrayList<net.minecraft.world.entity.item.ItemEntity>();for(var e:owner.level().getAllEntities())if(!before.contains(e.getUUID())&&e instanceof net.minecraft.world.entity.item.ItemEntity item){drops.add(item);created.add(item);}
                    require(drops.size()==1&&ItemStack.matches(drops.getFirst().getItem(),pieces[3]),"one actual unchanged helmet drops, no item duplication or loss");
                }
            }finally{wear(4);apply();owner.setHealth(hp);}
        }
        void acquisition(){String[] ids={"gevrf7nik7mutcrftkjujukhjth","genehverthbvdfcgjx_galth","getbub7kcrdtkjib6itgalth","hdtfbvjd6vgfd"};double[] amounts={6,12,16,8};
            for(int n=0;n<4;n++){var out=roll("neko_set_flesh/"+PARTS[n]);require(out.size()==1&&out.getFirst().getCount()==1,"one leaf output");var s=pieces[n]=out.getFirst();
                require(s.is(List.of(Items.LEATHER_BOOTS,Items.LEATHER_LEGGINGS,Items.LEATHER_CHESTPLATE,Items.LEATHER_HELMET).get(n))&&s.getMaxDamage()==6000&&s.getDamageValue()==0&&s.getEnchantments().isEmpty(),"original leather durability6000");
                var d=LbcFartus.data(s);require(d.size()==3&&d.getIntOr("flesh",0)==1&&d.getIntOr("transformable",0)==1&&d.getIntOr(PARTS[n],0)==1,"exact original flags");
                require(s.get(DataComponents.EQUIPPABLE).slot()==SLOTS[n]&&s.get(DataComponents.EQUIPPABLE).assetId().orElseThrow().identifier().equals(Identifier.parse("lbc:flesh"))&&s.get(DataComponents.ITEM_MODEL).equals(Identifier.parse("lbc:armors/flesh_"+PARTS[n]))&&s.get(DataComponents.TOOLTIP_STYLE).equals(Identifier.parse("lbc:tier_c")),"original model/equipment/tooltip");
                var name=s.get(DataComponents.CUSTOM_NAME);require(name.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t&&t.getKey().equals("flesh_"+PARTS[n])&&!name.getStyle().isItalic()&&name.getStyle().getColor().getValue()==0xff5555,"red nonitalic translated name");
                var lore=s.get(DataComponents.LORE).lines();var keys=new ArrayList<String>();lore.forEach(c->translations(c,keys));require(lore.size()==2&&keys.equals(List.of("type",PARTS[n],"flesh.lore")),"two original lore rows");
                var modifiers=s.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers();require(modifiers.size()==1,"only original health attribute");var e=modifiers.getFirst();require(e.attribute().equals(Attributes.MAX_HEALTH)&&e.modifier().id().equals(Identifier.withDefaultNamespace(ids[n]))&&e.modifier().amount()==amounts[n]&&e.modifier().operation()==AttributeModifier.Operation.ADD_VALUE&&e.slot().test(SLOTS[n]),"exact source equipment health ID/amount/op/slot");
            }
            var full=roll("neko_set_flesh_full");require(full.size()==4,"four full outputs");for(var p:pieces)require(full.stream().anyMatch(s->ItemStack.matches(s,p)),"full exact leaf");var pre=roll("neko_set_flesh_pre");require(pre.size()==5,"five pre draws");for(var s:pre)require(Arrays.stream(pieces).anyMatch(p->ItemStack.matches(s,p)),"pre exact leaf");
            var seen=new HashSet<Integer>();for(int a=0;a<256&&seen.size()<4;a++){var out=roll("neko_set_flesh");require(out.size()==1,"single root draw");int found=-1;for(int n=0;n<4;n++)if(ItemStack.matches(out.getFirst(),pieces[n]))found=n;require(found>=0,"root exact leaf");seen.add(found);}require(seen.size()==4,"all four root branches");
        }
        @SuppressWarnings("unchecked") void reforges(){var at=owner.position();
            for(int n=0;n<4;n++){
                require(LbcBolts.reforgeTables(pieces[n],false).equals(List.of("transformables_"+PARTS[n]))&&LbcBolts.reforgeTables(pieces[n],true).equals(List.of("neko_set_flesh")),"original normal slot and Ultra family routes");
                var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(owner.level()).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,at).withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,owner).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.COMMAND);
                var context=new net.minecraft.world.level.storage.loot.LootContext.Builder(params).create(Optional.empty());
                var pools=game.items().allItemPools();var types=List.of(PARTS[n]);var slots=pools.slots(LbcAllItemPools.Domain.CUSTOM,types);int selected=-1;
                for(int i=0;i<slots.size();i++){var preview=new ArrayList<ItemStack>();pools.drawSlot(LbcAllItemPools.Domain.CUSTOM,types,i,context,preview::add);if(preview.size()==1&&ItemStack.matches(preview.getFirst(),pieces[n])){selected=i;break;}}
                require(selected>=0,"canonical Flesh leaf is present in the actual uniform slot pool");int bound=slots.size(),pick=selected;
                Object random=read(owner.level(),"random");int[] calls={0};var donor=new net.minecraft.world.entity.item.ItemEntity(owner.level(),at.x,at.y,at.z,pieces[n].copy());donor.setNoGravity(true);require(owner.level().addFreshEntity(donor),"actual normal reforge donor");created.add(donor);var out=new ArrayList<ItemStack>();
                try{set(owner.level(),"random",java.lang.reflect.Proxy.newProxyInstance(net.minecraft.util.RandomSource.class.getClassLoader(),new Class<?>[]{net.minecraft.util.RandomSource.class},(proxy,m,args)->{if(m.getName().equals("nextInt")&&args!=null&&args.length==1&&args[0].equals(bound)){calls[0]++;return pick;}try{return m.invoke(random,args);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}}));reforge(donor,LbcBolts.ReforgeFamily.NORMAL,out);require(calls[0]==1&&donor.isRemoved()&&out.size()==1&&ItemStack.matches(out.getFirst(),pieces[n]),"actual native normal reforge consumes source donor and retains exact slot leaf");}finally{set(owner.level(),"random",random);donor.discard();}
                donor=new net.minecraft.world.entity.item.ItemEntity(owner.level(),at.x,at.y,at.z,pieces[n].copy());donor.setNoGravity(true);require(owner.level().addFreshEntity(donor),"actual Ultra source donor");created.add(donor);out.clear();reforge(donor,LbcBolts.ReforgeFamily.ULTRA,out);require(donor.isRemoved()&&out.size()==1&&Arrays.stream(pieces).anyMatch(p->ItemStack.matches(p,out.getFirst())),"actual native Ultra reforge follows flesh family root");
            }
        }
        void reforge(net.minecraft.world.entity.item.ItemEntity donor,LbcBolts.ReforgeFamily family,List<ItemStack> outputs){try{var method=LbcBolts.class.getDeclaredMethod("reforgeInput",net.minecraft.world.entity.item.ItemEntity.class,LbcBolts.ReforgeFamily.class,int.class,ServerPlayer.class,java.util.function.Consumer.class);method.setAccessible(true);var work=(LbcStonekivi.Work)method.invoke(game.items().bolts(),donor,family,1,owner,(java.util.function.Consumer<ItemStack>)outputs::add);require(work!=null&&work.step(),"existing native reforge Work completes");}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}


        void equipment(){double[] partial={20,26,38,54};for(int n=0;n<4;n++){wear(n);apply();require(owner.getMaxHealth()==partial[n]&&!LbcFleshArmor.full(owner)&&owner.getAttribute(Attributes.MAX_HEALTH).getModifier(BONUS)==null,"native partial health "+n);}
            wear(0);for(int n=0;n<4;n++)owner.getInventory().setItem(9+n,pieces[n].copy());owner.setItemInHand(InteractionHand.OFF_HAND,pieces[3].copy());apply();require(!LbcFleshArmor.full(owner)&&owner.getMaxHealth()==20,"carried/offhand excluded");owner.getInventory().clearContent();
            wear(4);apply();require(LbcFleshArmor.full(owner)&&owner.getMaxHealth()==124,"full native source health62 times2");var a=owner.getAttribute(Attributes.MAX_HEALTH);var modifier=a.getModifier(BONUS);require(modifier!=null&&modifier.amount()==1&&modifier.operation()==AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,"fixed original bonus");
            owner.setHealth(110);for(int n=0;n<20;n++){service.refresh(owner);service.inventoryChanged(owner);service.tick();}require(owner.getHealth()==110&&a.getModifier(BONUS)==modifier,"idempotent reconciliation never removes/readds or heals");
            owner.setItemSlot(EquipmentSlot.HEAD,pieces[3].copy());apply();require(owner.getHealth()==110,"same set equipment replacement cannot clamp HP through temporary modifier removal");
            for(int raw:new int[]{0,2}){wear(4);var s=pieces[3].copy();var tag=LbcFartus.data(s).copy();tag.putInt("flesh",raw);s.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));owner.setItemSlot(EquipmentSlot.HEAD,s);apply();require(!LbcFleshArmor.full(owner)&&a.getModifier(BONUS)==null,"raw flag equality "+raw);}
            wear(4);var wrong=new ItemStack(Items.STICK);wrong.set(DataComponents.CUSTOM_DATA,pieces[3].get(DataComponents.CUSTOM_DATA));owner.setItemSlot(EquipmentSlot.HEAD,wrong);apply();require(LbcFleshArmor.full(owner),"original raw set flags independent of base item");
            wear(4);apply();owner.setHealth(110);owner.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);equipmentUpdate(owner);require(a.getModifier(BONUS)==null&&owner.getHealth()<=owner.getMaxHealth(),"actual native equipment-change entrypoint removes stale full bonus");
        }
        void coexistence(){wear(4);apply();var a=owner.getAttribute(Attributes.MAX_HEALTH);var extra=Identifier.parse("skywars2:flesh_check_external");a.addTransientModifier(new AttributeModifier(extra,10,AttributeModifier.Operation.ADD_VALUE));owner.addEffect(ItemEffectLevels.create(MobEffects.HEALTH_BOOST,180,1));require(owner.getMaxHealth()==160,"external plus10 and native HealthBoostII plus8 both multiplied");owner.setHealth(100);service.refresh(owner);require(owner.getHealth()==100,"refresh does not heal coexistence");
            require(owner.addEffect(ItemEffectLevels.create(MobEffects.ABSORPTION,200,-2)),"native shadow absorption accepted");var shadow=ShadowAbsorption.state(owner);require(shadow.capacity>0,"actual shadow ledger active");double stored=shadow.stored;int duration=shadow.entries.get(-2).getDuration();service.refresh(owner);require(shadow.stored==stored&&shadow.entries.get(-2).getDuration()==duration,"idempotent Flesh does not mutate shadow balance/time");
            owner.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);apply();require(a.getModifier(extra)!=null&&owner.hasEffect(MobEffects.HEALTH_BOOST)&&shadow.entries.containsKey(-2)&&shadow.entries.get(-2).getDuration()==duration,"remove full set preserves independent health/effect/shadow owners");wear(4);apply();require(a.getModifier(extra)!=null&&shadow.entries.get(-2).getDuration()==duration,"replace full set preserves independent owners and time");owner.removeAllEffects();a.removeModifier(extra);
        }
        void policy(){require(!LbcDimensionItems.ordinary("minecraft:nexus")&&!LbcDimensionItems.ordinary("minecraft:imprinted")&&LbcDimensionItems.ordinary(owner.level()),"shared dimension classification (no actual transition claim)");wear(4);apply();var a=owner.getAttribute(Attributes.MAX_HEALTH);owner.setHealth(100);
            for(String tag:List.of("spectator","strict_map_object","magic_academy","rcdenyalways")){owner.entityTags().add(tag);try{service.refresh(owner);require(a.getModifier(BONUS)==null,"shared protected role "+tag);}finally{owner.entityTags().remove(tag);service.refresh(owner);}}
            var stage=game.stage();try{set(game,"stage",GameController.Stage.ENDING);service.refresh(owner);require(a.getModifier(BONUS)==null,"ENDING drops bonus");}finally{set(game,"stage",stage);service.refresh(owner);}
            for(String tag:List.of("stopper_magic","stopper")){var marker=new Marker(EntityTypes.MARKER,owner.level());marker.setPos(owner.position());marker.entityTags().add(tag);require(owner.level().addFreshEntity(marker),"native jammer marker");created.add(marker);game.items().magic().loaded(marker);try{require(tag.equals("stopper_magic")?game.items().magic().beaconSuppressed(owner.position()):game.items().magic().mechanismSuppressed(owner),"actual jammer positive control "+tag);owner.setHealth(100);service.refresh(owner);require(a.getModifier(BONUS)!=null&&owner.getMaxHealth()==124&&owner.getHealth()==100,"physical set unaffected by "+tag);}finally{game.items().magic().unloaded(marker);marker.discard();}}
            var modifier=a.getModifier(BONUS);owner.entityTags().add("time_stopped");try{service.refresh(owner);require(a.getModifier(BONUS)==modifier,"entity freeze preserves passive");}finally{owner.entityTags().remove("time_stopped");}
            game.items().timeStop().begin(owner.level(),40);try{service.tick();require(a.getModifier(BONUS)==modifier&&owner.getHealth()==100,"dimension freeze preserves native physical maximum without HP/time reset");}finally{game.items().timeStop().end(owner.level());}
            a.removeModifier(BONUS);a.addPermanentModifier(new AttributeModifier(BONUS,2,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));owner.setHealth(110);service.refresh(owner);require(a.getModifier(BONUS).amount()==1&&owner.getMaxHealth()==124&&owner.getHealth()==110,"old x3 source ID migrates atomically to x2 without intermediate clamp");
        }
        void criterion(){var advancement=Objects.requireNonNull(game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/armor/flesh")));for(int n:new int[]{3,4}){advancement.value().criteria().keySet().forEach(key->owner.getAdvancements().revoke(advancement,key));wear(n);net.minecraft.advancements.triggers.CriteriaTriggers.PLAYER_KILLED_ENTITY.trigger(owner,beta,owner.damageSources().playerAttack(owner));require(owner.getAdvancements().getOrStartProgress(advancement).isDone()==(n==4),"original native Flesh PvP advancement");}}
        void lifecycle(){wear(4);apply();owner.setHealth(110);service.forget(owner);require(owner.getHealth()==110&&owner.getAttribute(Attributes.MAX_HEALTH).getModifier(BONUS)!=null,"disconnect forget drops references without clamping HP");service.loaded(owner);require(owner.getHealth()==110,"loaded full equipment preserves current HP");
            var packed=save(owner);require(packed.getFloatOr("Health",-1)==110,"native serialized current HP");var profile=owner.getGameProfile();var restored=new ServerPlayer(game.server(),owner.level(),profile,owner.clientInformation());
            try{restored.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,owner.registryAccess(),packed));require(restored.getHealth()==110,"native full save/read retains HP above permanent-only max BEFORE loaded/equipment repair");equipmentUpdate(restored);service.loaded(restored);require(restored.getHealth()==110&&restored.getMaxHealth()==124,"native loaded full save retains exact current and maximum HP");require(restored.getAttribute(Attributes.MAX_HEALTH).getModifiers().stream().filter(m->m.id().equals(BONUS)).count()==1,"one source modifier after serialization and repeated repair");}
            finally{service.forget(restored);owner.getAdvancements().setPlayer(owner);}
            wear(0);service.loaded(owner);require(owner.getAttribute(Attributes.MAX_HEALTH).getModifier(BONUS)==null,"loaded stale source modifier reconciles current empty gear");wear(4);apply();var foreign=Identifier.parse("skywars2:flesh_check_stop_neighbor");owner.getAttribute(Attributes.MAX_HEALTH).addTransientModifier(new AttributeModifier(foreign,7,AttributeModifier.Operation.ADD_VALUE));service.stop();require(owner.getAttribute(Attributes.MAX_HEALTH).getModifier(BONUS)==null&&owner.getAttribute(Attributes.MAX_HEALTH).getModifier(foreign)!=null,"stop removes only own online modifier");owner.getAttribute(Attributes.MAX_HEALTH).removeModifier(foreign);service.refresh(owner);
        }
        void roundTrip(float hp,float maximum,String label){
            var packed=save(owner);var restored=new ServerPlayer(game.server(),owner.level(),owner.getGameProfile(),owner.clientInformation());
            try{
                restored.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,owner.registryAccess(),packed));
                require(restored.getHealth()==hp&&restored.getMaxHealth()==maximum,label+" native load validates saved HP against complete equipment before any repair");
                equipmentUpdate(restored);service.loaded(restored);
                require(restored.getHealth()==hp&&restored.getMaxHealth()==maximum,label+" first native equipment detection and role reconciliation retain HP");
                if(owner.hasEffect(MobEffects.ABSORPTION))require(ShadowAbsorption.state(restored).capacity==ShadowAbsorption.state(owner).capacity
                        &&ShadowAbsorption.state(restored).stored==ShadowAbsorption.state(owner).stored
                        &&restored.getEffect(MobEffects.HEALTH_BOOST).getDuration()==owner.getEffect(MobEffects.HEALTH_BOOST).getDuration(),label+" independent shadow/boost data not healed or advanced");
            }finally{service.forget(restored);owner.getAdvancements().setPlayer(owner);}
        }
        void commonLoadControls(){
            wear(4);apply();owner.addEffect(ItemEffectLevels.create(MobEffects.HEALTH_BOOST,121,1));
            owner.addEffect(ItemEffectLevels.create(MobEffects.ABSORPTION,123,-2));owner.setHealth(120);
            require(owner.getMaxHealth()==136,"full set plus native boost and shadow maximum");roundTrip(120,136,"Flesh with HealthBoost/shadow");owner.removeAllEffects();
            wear(0);apply();var held=new ItemStack(Items.WOODEN_SWORD);
            held.set(DataComponents.ATTRIBUTE_MODIFIERS,net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                    .add(Attributes.MAX_HEALTH,new AttributeModifier(Identifier.parse("skywars2:held_health_load_probe"),50,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND).build());
            owner.getInventory().setSelectedSlot(2);owner.setItemInHand(InteractionHand.MAIN_HAND,held);equipmentUpdate(owner);owner.setHealth(65);
            require(owner.getMaxHealth()==70,"ordinary selected-slot mainhand adds health");roundTrip(65,70,"ordinary mainhand with no Flesh");owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);equipmentUpdate(owner);
            var mob=new net.minecraft.world.entity.animal.cow.Cow(EntityTypes.COW,owner.level());
            var copy=new net.minecraft.world.entity.animal.cow.Cow(EntityTypes.COW,owner.level());
            try{
                mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);var armor=new ItemStack(Items.LEATHER_HELMET);
                armor.set(DataComponents.ATTRIBUTE_MODIFIERS,net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                        .add(Attributes.MAX_HEALTH,new AttributeModifier(Identifier.parse("skywars2:mob_health_load_probe"),60,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.HEAD).build());
                mob.setItemSlot(EquipmentSlot.HEAD,armor);equipmentUpdate(mob);mob.setHealth(75);
                copy.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,owner.registryAccess(),save(mob)));
                require(copy.getHealth()==75&&copy.getMaxHealth()==80,"ordinary mob armor native load preserves HP without any Flesh modifier");
                equipmentUpdate(copy);require(copy.getHealth()==75,"ordinary mob first equipment tick preserves loaded HP");
            }finally{mob.discard();copy.discard();}
        }
        @SuppressWarnings("unchecked") public void close(){created.forEach(Entity::discard);service.forget(owner);saved.restore(game);owner.getAttributes().assignAllValues(attributes);owner.setHealth(saved.health);owner.setAbsorptionAmount(saved.absorption);var cache=(Map<EquipmentSlot,ItemStack>)read(owner,"lastEquipmentItems");cache.clear();equipment.forEach((s,i)->cache.put(s,i.copy()));advancements.forEach((a,done)->{for(var k:a.value().criteria().keySet())if(done.contains(k))owner.getAdvancements().award(a,k);else owner.getAdvancements().revoke(a,k);});for(var o:game.server().getScoreboard().getObjectives())if(scores.containsKey(o))game.server().getScoreboard().getOrCreatePlayerScore(owner,o,true).set(scores.get(o));else game.server().getScoreboard().resetSinglePlayerScore(owner,o);
            try{for(var e:profile.entrySet())e.getKey().set(game.players().get(owner.getUUID()),e.getValue());var shadow=ShadowAbsorption.state(owner);shadow.entries.clear();shadow.entries.putAll(shadowEntries);for(var e:shadowFields.entrySet())e.getKey().set(shadow,e.getValue());}catch(IllegalAccessException e){throw new IllegalStateException(e);}owner.setHealth(saved.health);owner.setAbsorptionAmount(saved.absorption);owner.connection.resetPosition();}
    }
    private static void translations(net.minecraft.network.chat.Component c,List<String> keys){if(c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t){keys.add(t.getKey());require(!c.getStyle().isItalic()&&c.getStyle().getColor()!=null&&c.getStyle().getColor().getValue()==0x55ffff,"original aqua nonitalic lore");}c.getSiblings().forEach(s->translations(s,keys));}
    public static void run(GameController game){try(var f=new Fixture(game)){f.acquisition();f.reforges();f.equipment();f.nativeArmorDrops();f.coexistence();f.policy();f.criterion();f.lifecycle();f.commonLoadControls();}SkyWars2.LOGGER.info("[check] PASS flesh-armor (seven source tables, exact payload/presentation/health modifiers, consumed normal/Ultra reforges, native equipment changes, no-heal idempotence, HealthBoost/shadow coexistence, original advancement, forget/loaded/stop and native in-memory full save/read; no full reconnect/death proof)");}
}
