package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** ENTITY_LOAD and actual scheduled item work; no direct loose-owner tick/copy/tablet calls. */
public final class TmtLooseRewardChecks {
    private TmtLooseRewardChecks(){}
    private static Scene running;
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT loose rewards: "+why);}
    public static void run(GameController game){require(running==null,"exclusive native scene");var scene=new Scene(game);running=scene;
        try{scene.begin();}catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    public static void tick(GameController game){var scene=running;if(scene==null)return;
        try{require(scene.game==game,"same controller");if(!scene.step())return;running=null;scene.close();
            SkyWars2.LOGGER.info("[check] PASS tmt-loose-rewards (actual native drop/load/scheduled work; all5 private copier carriers/full sibling payload; native attribute/default/negative, full quantity/metadata split, direct enchantment saturation; independent debit/source order/exhaustion/no-op; nearest and private donor exclusions, MAG/TECH/TimeStop, real pickup/native split/copy/codec/unload-reload, full Vanity restore reindex and continuation)");
        }catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL tmt-loose-rewards",error);}}
    public static void cancel(GameController game){if(running==null)return;var scene=running;running=null;scene.close();}
    private record Case(String name,Runnable setup,Runnable verify){}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final Vec3 at;
        final Set<UUID> original=new HashSet<>();final List<Case> cases=new ArrayList<>();
        final Map<Integer,LbcTmtAbilities.Ability> multiply=new HashMap<>();
        final Map<net.minecraft.stats.Stat<Item>,Integer> pickupStats=new LinkedHashMap<>();
        LbcTmtAbilities.Ability copier,sibling;ItemEntity host,donor,other;ItemStack physical,payload,published;
        ItemAttributeModifiers attributes;ItemEnchantments stored;UUID restoredHost,restoredDonor;
        int index,phase,when,started,hostCount;boolean closed,restoring;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);at=f.home.add(0,5,8);
            f.level.getAllEntities().forEach(entity->original.add(entity.getUUID()));
            for(int n=0;n<5;n++){var stat=net.minecraft.stats.Stats.ITEM_PICKED_UP.get(LbcTmtItems.prototype(n).getItem());pickupStats.put(stat,f.owner.getStats().getValue(stat));}}
        List<ItemEntity> drops(){var result=new ArrayList<ItemEntity>();for(var entity:f.level.getAllEntities())
            if(entity instanceof ItemEntity item&&!item.isRemoved()&&!original.contains(item.getUUID())&&item.position().distanceToSqr(at)<32*32)result.add(item);return result;}
        void clear(){for(var item:drops())item.discard();f.removeFields();}
        ItemEntity drop(ItemStack stack,Vec3 position){var item=new ItemEntity(f.level,position.x,position.y,position.z,stack);
            item.setNoGravity(true);item.setNeverPickUp();item.setDeltaMovement(Vec3.ZERO);item.setThrower(f.owner);
            require(f.level.addFreshEntity(item),"actual native item publication");f.own(item);return item;}
        static CompoundTag saved(Entity actor){var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,actor.registryAccess());actor.saveWithoutId(output);return output.buildResult();}
        LbcTmtAbilities.Ability property(String flag){return game.items().tmt().catalog().families().stream().flatMap(row->row.variants().stream())
                .filter(p->((ItemData.IntReader)(Object)p.data()).skywars2$readInt(flag,0)==1).findFirst().orElseThrow();}
        ItemStack combined(int carrier,int count,LbcTmtAbilities.Ability... selected){var stack=LbcTmtItems.prototype(carrier);
            var data=stack.get(DataComponents.CUSTOM_DATA).copyTag();LbcTmtAbilities.write(data,List.of(selected));data.putString("loose_fixture","outer-retained");
            stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));stack.setCount(count);
            stack.set(DataComponents.CUSTOM_NAME,Component.literal("physical loose host"));stack.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("outer sibling lore"))));
            stack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND,3))));
            stack.set(DataComponents.POTION_CONTENTS,new PotionContents(Optional.empty(),Optional.of(0x234567),List.of(),Optional.of("physical payload")));
            var entityData=new CompoundTag();entityData.putString("loose_fixture","entity payload");stack.set(DataComponents.ENTITY_DATA,TypedEntityData.of(EntityTypes.PIG,entityData));
            return stack;}
        ItemStack decorated(Item item,int count){var stack=new ItemStack(item,count);stack.set(DataComponents.CUSTOM_NAME,Component.literal("complete donor"));
            stack.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("retained donor lore"))));var data=new CompoundTag();data.putString("loose_donor","full payload");stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return stack;}
        void source(int count,LbcTmtAbilities.Ability... selected){physical=combined(2,count,selected);host=drop(physical.copy(),at);published=host.getItem();}
        void donor(ItemStack stack){payload=stack.copy();donor=drop(stack,at.add(.7,0,0));}
        void retained(int count){require(!host.isRemoved()&&ItemStack.matches(host.getItem(),physical.copyWithCount(count)),"complete actual replacement host/sibling components retained count="+count+" case="+(index<cases.size()?cases.get(index).name():"persistence"));
            if(count<physical.getCount())require(host.getItem()!=published,"accepted native debit publishes a replacement stack through ItemEntity.setItem");
            require(!ItemData.contains(host.getItem(),LbcTmtAbilities.ACTIVE)&&LbcTmtAbilities.read(host.getItem()).equals(LbcTmtAbilities.read(physical)),"no projected ACTIVE or private-slot loss in published native remainder");
            var ops=f.owner.registryAccess().createSerializationContext(NbtOps.INSTANCE);var round=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,host.getItem()).getOrThrow()).getOrThrow();
            require(ItemStack.matches(round,host.getItem()),"actual host remainder persists through native codec");}
        void unchanged(){retained(physical.getCount());require(donor==null||ItemStack.matches(donor.getItem(),payload),"no-op retains complete donor");}
        List<ItemEntity> copies(){return drops().stream().filter(item->item!=host&&item!=donor&&item!=other).toList();}
        void add(String name,Runnable setup,Runnable verify){cases.add(new Case(name,setup,verify));}
        void begin(){f.prepareParticipants();started=game.server().getTickCount();copier=property("copypaster");sibling=property("zaika");
            var families=game.items().tmt().catalog().families();
            for(var row:families)for(var property:row.variants()){int kind=((ItemData.IntReader)(Object)property.data()).skywars2$readInt("multiply",0);if(kind>=2&&kind<=4)multiply.put(kind,property);}
            require(multiply.size()==3&&multiply.values().stream().map(LbcTmtAbilities.Ability::id).distinct().count()==1,"one genuine multiply family retains all3 private typed modes");
            for(int carrier=0;carrier<5;carrier++){final int choice=carrier;
                add("copier physical carrier "+carrier,()->{physical=combined(choice,1,copier,sibling);host=drop(physical.copy(),at);host.setDeltaMovement(.02,.03,.04);donor(decorated(Items.DIAMOND,7));},()->{
                    require(host.isRemoved()&&ItemStack.matches(donor.getItem(),payload),"one accepted native copier debit; original donor full stack untouched");
                    var outputs=copies();require(outputs.size()==1&&ItemStack.matches(outputs.getFirst().getItem(),payload),"real native copy preserves every donor component/count carrier="+choice);
                    var child=outputs.getFirst();require(child.getOwner()==f.owner,"copier thrower: "+child.getOwner());require(game.items().currentEpoch(child),"copier published epoch: "+child.entityTags());require(child.getDeltaMovement().distanceToSqr(host.getDeltaMovement())<1e-12,"copier native motion child="+child.getDeltaMovement()+" host="+host.getDeltaMovement());});}
            add("copier forged aggregate zero and retained outer",()->{source(2,copier,sibling);var data=host.getItem().get(DataComponents.CUSTOM_DATA).copyTag();data.putInt("copypaster",0);host.getItem().set(DataComponents.CUSTOM_DATA,CustomData.of(data));physical=host.getItem().copy();donor(decorated(Items.EMERALD,9));},()->{retained(1);require(copies().size()==1&&ItemStack.matches(copies().getFirst().getItem(),payload),"private copier survives forged aggregate flag0");});
            add("copier no target",()->source(2,copier,sibling),this::unchanged);
            add("private copier donors excluded read-only",()->{source(2,copier,sibling);other=drop(combined(2,2,copier,sibling),at.add(.2,0,0));var data=other.getItem().get(DataComponents.CUSTOM_DATA).copyTag();data.putInt("copypaster",0);other.getItem().set(DataComponents.CUSTOM_DATA,CustomData.of(data));payload=other.getItem().copy();},()->{retained(2);require(ItemStack.matches(other.getItem(),payload)&&drops().size()==2,"two real private copiers cannot copy one another despite forged outer0");});
            add("forged outer flags have no private source",()->{source(2,sibling);var data=host.getItem().get(DataComponents.CUSTOM_DATA).copyTag();data.putInt("copypaster",1);data.putInt("multiply",3);host.getItem().set(DataComponents.CUSTOM_DATA,CustomData.of(data));physical=host.getItem().copy();donor(decorated(Items.DIAMOND,7));},()->{unchanged();require(copies().isEmpty(),"aggregate flags cannot enroll a false private loose ability");});
            for(var item:List.of(Items.DIAMOND_SWORD,Items.DIAMOND_CHESTPLATE,Items.TRIDENT))add("multiply2 implicit native defaults "+item,()->{source(2,multiply.get(2),sibling);donor(decorated(item,1));attributes=payload.get(DataComponents.ATTRIBUTE_MODIFIERS);require(attributes!=null&&!attributes.modifiers().isEmpty(),"real native defaults exist");},this::doubled);
            add("multiply2 negative operations and displays",()->{source(2,multiply.get(2),sibling);var stack=decorated(Items.STICK,1);var entries=new ArrayList<ItemAttributeModifiers.Entry>();
                for(int n=0;n<3;n++)entries.add(new ItemAttributeModifiers.Entry(Attributes.ATTACK_SPEED,new AttributeModifier(Identifier.parse("skywars2:loose_check_"+n),n==0?-2.5:n+.25,AttributeModifier.Operation.values()[n]),EquipmentSlotGroup.MAINHAND,ItemAttributeModifiers.Display.override(Component.literal("display "+n))));
                attributes=new ItemAttributeModifiers(entries);stack.set(DataComponents.ATTRIBUTE_MODIFIERS,attributes);donor(stack);},this::doubled);
            add("multiply2 nearest no-op no farther fallback",()->{source(2,multiply.get(2),sibling);donor(decorated(Items.DIRT,1));donor.setPos(at.add(.2,0,0));other=drop(decorated(Items.DIAMOND_SWORD,1),at.add(.9,0,0));},()->{unchanged();require(ItemStack.isSameItemSameComponents(other.getItem(),decorated(Items.DIAMOND_SWORD,1)),"native nearest no-op does not skip to farther attributes");});
            add("private multiply donor presence excludes",()->{source(2,multiply.get(2),sibling);other=drop(combined(2,2,multiply.get(4),sibling),at.add(.2,0,0));payload=other.getItem().copy();},()->{retained(2);require(ItemStack.matches(other.getItem(),payload)&&drops().size()==2,"other private multiply token is not a donor; no projection mutation");});
            add("forged aggregate multiply does not exclude real donor",()->{source(2,multiply.get(2),sibling);var stack=combined(2,1,sibling);var data=stack.get(DataComponents.CUSTOM_DATA).copyTag();data.putInt("multiply",0);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
                attributes=new ItemStack(Items.DIAMOND_SWORD).get(DataComponents.ATTRIBUTE_MODIFIERS);stack.set(DataComponents.ATTRIBUTE_MODIFIERS,attributes);donor(stack);},this::doubled);
            add("prior epoch token cannot reenroll",()->{physical=combined(2,2,copier,sibling);host=new ItemEntity(f.level,at.x,at.y,at.z,physical.copy());host.setNoGravity(true);host.setNeverPickUp();host.setThrower(f.owner);host.entityTags().add("skywars2.items.stale_loose_fixture");
                f.level.addFreshEntity(host);f.own(host);donor(decorated(Items.DIAMOND,7));},()->{require((host.isRemoved()||ItemStack.matches(host.getItem(),physical))&&ItemStack.matches(donor.getItem(),payload)&&copies().isEmpty()&&!game.items().currentEpoch(host),"native stale-epoch quarantine never reenrolls or spends a prior match's token");});
            add("multiply3 full legal split metadata",()->{source(2,multiply.get(3),sibling);donor(decorated(Items.DIAMOND,40));donor.setDeltaMovement(.03,.02,.01);donor.entityTags().add("loose_metadata");donor.setTarget(f.owner.getUUID());},()->{
                retained(1);var outputs=drops().stream().filter(item->item!=host).toList();require(outputs.size()==2&&outputs.stream().mapToInt(item->item.getItem().getCount()).sum()==120,"40 source units become complete120, split into64+56");
                var donorNBT=saved(donor);for(var item:outputs){require(item.getItem().getCount()<=item.getItem().getMaxStackSize()&&ItemStack.isSameItemSameComponents(item.getItem(),payload),"native legal quantity and complete item metadata");
                    var tag=saved(item);for(String key:List.of("Age","PickupDelay","Thrower","Owner","NoGravity"))require(Objects.equals(tag.get(key),donorNBT.get(key)),"native split retains entity field "+key);
                    require(item.entityTags().containsAll(donor.entityTags())&&item.getDeltaMovement().equals(donor.getDeltaMovement())&&item.getOwner()==f.owner&&(item==donor||game.items().currentEpoch(item)),"native split tags/physics/thrower and published-child epoch");}
                require(outputs.stream().map(Entity::getUUID).distinct().count()==2,"native split entities have independent identities");});
            add("multiply3 unstackable native singles",()->{source(2,multiply.get(3),sibling);donor(decorated(Items.DIAMOND_SWORD,1));},()->{retained(1);var outputs=drops().stream().filter(item->item!=host).toList();require(outputs.size()==3&&outputs.stream().allMatch(item->item.getItem().getCount()==1&&ItemStack.isSameItemSameComponents(item.getItem(),payload)),"one unstackable becomes three complete legal singles");});
            add("multiply4 direct saturation and stored untouched",()->{source(2,multiply.get(4),sibling);var registry=f.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);var levels=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);levels.set(registry.getOrThrow(Enchantments.SHARPNESS),600000000);levels.set(registry.getOrThrow(Enchantments.VANISHING_CURSE),3);
                var stack=decorated(Items.DIAMOND_SWORD,1);stack.set(DataComponents.ENCHANTMENTS,levels.toImmutable());stack.setDamageValue(19);var book=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);book.set(registry.getOrThrow(Enchantments.SHARPNESS),7);stored=book.toImmutable();stack.set(DataComponents.STORED_ENCHANTMENTS,stored);donor(stack);},()->{
                retained(1);var registry=f.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);require(donor.getItem().getEnchantments().getLevel(registry.getOrThrow(Enchantments.SHARPNESS))==Integer.MAX_VALUE&&donor.getItem().getEnchantments().getLevel(registry.getOrThrow(Enchantments.VANISHING_CURSE))==12,"native wide direct levels saturate and curse quadruples");
                require(donor.getItem().get(DataComponents.STORED_ENCHANTMENTS).equals(stored)&&donor.getItem().getDamageValue()==19,"stored levels and physical wear unchanged");});
            add("multiply4 stored-only no-op",()->{source(2,multiply.get(4),sibling);var stack=decorated(Items.ENCHANTED_BOOK,1);var levels=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);levels.set(f.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS),7);stack.set(DataComponents.STORED_ENCHANTMENTS,levels.toImmutable());donor(stack);},this::unchanged);
            add("multiply4 already saturated no-op",()->{source(2,multiply.get(4),sibling);var stack=decorated(Items.STICK,1);var levels=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);levels.set(f.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS),Integer.MAX_VALUE);stack.set(DataComponents.ENCHANTMENTS,levels.toImmutable());donor(stack);},this::unchanged);
            for(int count=1;count<=3;count++){final int physicalCount=count;add("tablet before copier independent debit count"+count,()->{source(physicalCount,copier,multiply.get(3),sibling);donor(decorated(Items.DIAMOND,7));},()->{
                require(donor.getItem().getCount()==21,"tablet source order runs before copier regardless private slot order");var copies=copies();require(copies.size()==(physicalCount==1?0:1)&&copies.stream().allMatch(item->item.getItem().getCount()==21),"each accepted property consumes one actual host unit; exhausted host stops");
                if(physicalCount<=2)require(host.isRemoved(),"physical count exhausted exactly");else retained(1);});}
            add("no-op tablet does not debit before copier",()->{source(2,copier,multiply.get(2),sibling);donor(decorated(Items.DIRT,7));},()->{retained(1);require(copies().size()==1&&ItemStack.matches(copies().getFirst().getItem(),payload),"no-op keeps its quantum and successful copier alone debits1");});
            add("MAG blocks tablet but permits copier",()->{source(2,copier,multiply.get(3),sibling);donor(decorated(Items.DIAMOND,7));f.field(false,at);},()->{retained(1);require(ItemStack.matches(donor.getItem(),payload)&&copies().size()==1&&ItemStack.matches(copies().getFirst().getItem(),payload),"MAG blocked tablet retains its unit; unjammed copier still executes");});
            add("tablet source-only MAG",()->{source(2,multiply.get(3),sibling);donor(decorated(Items.DIAMOND,7));f.field(false,at.add(-19.5,0,0));require(game.items().magic().suppressedAt(f.level,at)&&!game.items().magic().suppressedAt(f.level,donor.position()),"real source-only MAG geometry");},this::unchanged);
            add("tablet target-only MAG",()->{source(2,multiply.get(3),sibling);donor(decorated(Items.DIAMOND,7));f.field(false,at.add(20.5,0,0));require(!game.items().magic().suppressedAt(f.level,at)&&game.items().magic().suppressedAt(f.level,donor.position()),"real target-only MAG geometry");},this::unchanged);
            add("TECH no tablet gate",()->{source(2,multiply.get(3),sibling);donor(decorated(Items.DIAMOND,7));f.field(true,at);},()->{retained(1);require(donor.getItem().getCount()==21,"TECH does not suppress native tablet");});
            // Commands can arrive before this frame's native owner tick. Start after that
            // tick, so every case observes exactly one work quantum and no extra physics.
            phase=-1;
        }
        void doubled(){retained(1);var actual=donor.getItem().get(DataComponents.ATTRIBUTE_MODIFIERS);require(actual.modifiers().size()==attributes.modifiers().size(),"native component keeps full entry list");
            for(int n=0;n<attributes.modifiers().size();n++){var before=attributes.modifiers().get(n);var after=actual.modifiers().get(n);require(after.modifier().amount()==before.modifier().amount()*2&&after.attribute().equals(before.attribute())&&after.modifier().id().equals(before.modifier().id())&&after.modifier().operation()==before.modifier().operation()&&after.slot()==before.slot()&&after.display().equals(before.display()),"native effective positive/negative/default attribute amount doubled with identity/slot/display preserved");}
            var expected=payload.copy();expected.set(DataComponents.ATTRIBUTE_MODIFIERS,actual);require(ItemStack.matches(expected,donor.getItem()),"all unrelated donor components and count remain exact");}
        void nextCase(){clear();host=null;donor=null;other=null;physical=null;payload=null;
            if(index==cases.size()){persistence();return;}cases.get(index).setup().run();when=game.server().getTickCount();phase=0;}
        void persistence(){source(2,copier,sibling);donor(decorated(Items.DIAMOND,7));f.pause();phase=1;when=game.server().getTickCount();}
        boolean step(){f.client.acknowledge();f.audience.acknowledge();require(game.server().getTickCount()-started<700,"bounded real scheduler fixture phase="+phase+" case="+index);
            if(phase==-1){nextCase();return false;}
            if(restoring&&game.vanity().busy())for(var player:game.server().getPlayerList().getPlayers())if(player.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            if(phase==0){if(game.server().getTickCount()==when)return false;try{cases.get(index).verify().run();}catch(Throwable error){throw new IllegalStateException("Actual native loose case: "+cases.get(index).name(),error);}index++;nextCase();return false;}
            if(phase==1){if(game.server().getTickCount()-when<3)return false;retained(2);require(copies().isEmpty(),"canonical TimeStop holds actual scheduled copier work");
                var tag=saved(host);var identity=host.getUUID();host.setRemoved(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                var loaded=new ItemEntity(EntityTypes.ITEM,f.level);loaded.load(TagValueInput.create(ProblemReporter.DISCARDING,f.level.registryAccess(),tag));require(loaded.getUUID().equals(identity)&&ItemStack.matches(loaded.getItem(),physical)&&saved(loaded).equals(tag),"full native entity NBT/codec roundtrip retains physical private payload and entity state");
                require(f.level.addFreshEntity(loaded),"real unload/reload publishes retained UUID through native callback");host=loaded;f.own(host);
                var clone=new ItemEntity(EntityTypes.ITEM,f.level);var fresh=clone.getUUID();clone.restoreFrom(host);clone.setUUID(fresh);clone.setPos(at.add(6,0,0));require(f.level.addFreshEntity(clone),"native restoreFrom copy publishes independent physical entity");other=clone;f.own(clone);
                require(ItemStack.matches(clone.getItem(),physical)&&!clone.getUUID().equals(host.getUUID()),"native entity clone retains full private payload without sharing identity");f.thaw();phase=2;when=game.server().getTickCount();return false;}
            if(phase==2){if(game.server().getTickCount()==when)return false;retained(1);require(copies().size()==1&&ItemStack.matches(other.getItem(),physical),"actual reload index resumes exactly one copy; out-of-range clone retains full stack");
                var encoded=host.getItem().copy();var split=encoded.split(1);require(ItemStack.matches(split,physical.copyWithCount(1))&&encoded.isEmpty(),"native split preserves full private payload");
                f.owner.getInventory().clearContent();host.setPos(f.owner.position());host.setNoPickUpDelay();host.playerTouch(f.owner);require(host.isRemoved(),"actual native item pickup removes physical host");
                var picked=f.owner.getInventory().getItem(0);require(ItemStack.matches(picked,physical.copyWithCount(1)),"actual pickup retains all private properties/components");
                clear();f.owner.getInventory().clearContent();source(3,copier,sibling);donor(decorated(Items.EMERALD,7));f.pause();
                require(game.vanity().requestCapture(f.owner,false),"real whole coordinator point accepts paused pending loose item");restoredHost=host.getUUID();restoredDonor=donor.getUUID();phase=3;when=game.server().getTickCount();return false;}
            if(phase==3){if(!game.vanity().hasSnapshot()||game.vanity().capturePreparing()){require(game.server().getTickCount()-when<160,"real snapshot readiness");return false;}
                host.discard();donor.discard();require(game.vanity().requestRestoreForCheck(),"actual full native rewind requested");restoring=true;phase=4;when=game.server().getTickCount();return false;}
            if(phase==4){if(game.vanity().busy()){require(game.server().getTickCount()-when<400,"bounded full native rewind");return false;}
                host=(ItemEntity)f.level.getEntity(restoredHost);donor=(ItemEntity)f.level.getEntity(restoredDonor);require(host!=null&&donor!=null&&game.items().timeStop().stopped(f.level),"actual rewind restores paused physical item bodies");
                require(ItemStack.matches(host.getItem(),physical)&&ItemStack.matches(donor.getItem(),payload)&&game.items().currentEpoch(host),"restored full outer remainder, donor and epoch");
                f.thaw();phase=5;when=game.server().getTickCount();return false;}
            if(phase==5){if(game.server().getTickCount()==when)return false;require(host.getItem().getCount()==2&&copies().size()==1,"real restored native index resumes one copier quantum without manual loaded/tick calls");
                require(ItemStack.matches(host.getItem(),physical.copyWithCount(2))&&!ItemData.contains(host.getItem(),LbcTmtAbilities.ACTIVE),"whole rewind's continued actual host stays complete and unprojected");return true;}
            throw new IllegalStateException("Unexpected loose phase "+phase);
        }
        public void close(){if(closed)return;closed=true;try{game.vanity().stop();clear();}finally{try{f.close();}finally{pickupStats.forEach((stat,value)->f.owner.getStats().setValue(f.owner,stat,value));}}}
    }
}
