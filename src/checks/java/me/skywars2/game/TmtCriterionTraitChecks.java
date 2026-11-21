package me.skywars2.game;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.GameType;

/** Native advancement predicates and native using ticks; kill-trigger proof is not a real death. */
public final class TmtCriterionTraitChecks {
    private TmtCriterionTraitChecks(){}
    private static GameController requested;
    private static Scene running;
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT criterion traits: "+why);}
    public static void run(GameController game){require(requested==null&&running==null,"exclusive END-bound native request");requested=game;}
    public static void tick(GameController game){
        if(requested!=null){require(requested==game,"same queued controller");requested=null;var scene=new Scene(game);running=scene;
            try{scene.begin();}catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL tmt-criterion-traits",error);}return;}
        var scene=running;if(scene==null)return;
        try{require(scene.game==game,"same native controller");if(!scene.step())return;running=null;scene.close();
            SkyWars2.LOGGER.info("[check] PASS tmt-criterion-traits (Harpoon actual native PLAYER_KILLED_ENTITY criterion boundary/all5 carriers/mainhand, offhand/nonplayer/plain negatives; no actual death claimed; Sea Legend actual native bow+arrow/apple using ticks both hands/plain/spectator negatives; exact private payload/no scope, no label-created trident/charge/spin/enchants; native bucket HEAD armor3/durability200 and Oguzok wooden-sword components/effective attributes; no false catalog pending or component-only ability tickets)");
        }catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL tmt-criterion-traits",error);}}
    public static void cancel(GameController game){requested=null;if(running==null)return;var scene=running;running=null;scene.close();}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final NativeMeleeChecks.NativeStats stats;
        final Set<UUID> original=new HashSet<>();final CompoundTag base=new CompoundTag();
        final Map<AdvancementHolder,Set<String>> priorProgress=new LinkedHashMap<>();
        AdvancementHolder harpoonAdv,seaAdv;LbcTmtAbilities.Ability harpoon,sea,sibling;
        ItemStack current,before,bucket,wood,oguzok;CustomData dataBefore;
        int started,when,phase,useCase;double woodDamage,woodSpeed;float alphaHealth,betaHealth;boolean closed;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);
            var items=new ArrayList<Item>(List.of(LbcWtf.SWORD,LbcUtilityItems.Kind.BUCKET_HEAD.item,Items.WOODEN_SWORD));
            for(int n=0;n<5;n++)items.add(LbcTmtItems.prototype(n).getItem());stats=new NativeMeleeChecks.NativeStats(f.owner,items);
            f.level.getAllEntities().forEach(actor->original.add(actor.getUUID()));}
        LbcTmtCatalog.Family family(String id){return game.items().tmt().catalog().families().stream().filter(row->row.id().equals(id)).findFirst().orElseThrow();}
        LbcTmtAbilities.Ability property(String flag){return game.items().tmt().catalog().families().stream().flatMap(row->row.variants().stream())
                .filter(row->((ItemData.IntReader)(Object)row.data()).skywars2$readInt(flag,0)==1).findFirst().orElseThrow();}
        AdvancementHolder advancement(String id){var holder=Objects.requireNonNull(game.server().getAdvancements().get(Identifier.parse(id)),"Retained original advancement "+id);
            var done=new HashSet<String>();f.owner.getAdvancements().getOrStartProgress(holder).getCompletedCriteria().forEach(done::add);priorProgress.put(holder,done);return holder;}
        void revoke(AdvancementHolder holder){for(var key:holder.value().criteria().keySet())f.owner.getAdvancements().revoke(holder,key);}
        boolean done(AdvancementHolder holder){return f.owner.getAdvancements().getOrStartProgress(holder).isDone();}
        void clear(){f.owner.stopUsingItem();f.owner.getInventory().clearContent();f.owner.inventoryMenu.getCraftSlots().clearContent();f.owner.containerMenu.setCarried(ItemStack.EMPTY);
            for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND))f.owner.setItemSlot(slot,ItemStack.EMPTY);}
        ItemStack combined(int carrier,LbcTmtAbilities.Ability... properties){var stack=LbcTmtItems.prototype(carrier);var data=base.copy();
            data.merge(stack.get(DataComponents.CUSTOM_DATA).copyTag());LbcTmtAbilities.write(data,List.of(properties));stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            stack.set(DataComponents.CUSTOM_NAME,Component.literal("criterion-only physical carrier"));stack.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("independent private sibling"))));
            stack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND,3))));return stack;}
        void unchanged(ItemStack stack,ItemStack expected){require(ItemStack.matches(stack,expected)&&!ItemData.contains(stack,LbcTmtAbilities.ACTIVE)&&!LbcTmtAbilities.active(stack)&&!LbcTmtAbilities.nativePredicatesActive(),"native criterion reads preserve exact independent physical/private payload without a source scope");}
        void catalog(){var h=family("native/harpoon");var s=family("native/sea_legend");harpoon=h.variants().getFirst();sea=s.variants().getFirst();sibling=property("zaika");
            require(h.events().equals(Set.of(LbcTmtCatalog.Event.KILL))&&h.locations().equals(Set.of(LbcTmtCatalog.Location.MAIN_HAND)),"Harpoon owns genuine mainhand kill criterion only");
            require(s.events().equals(Set.of(LbcTmtCatalog.Event.USING))&&s.locations().equals(Set.of(LbcTmtCatalog.Location.USED_HAND)),"Sea Legend owns genuine native used-item criterion only");
            var onlyComponents=Set.of("skywars2:bucket_head","skywars2:oguzok_sword");
            for(var finding:game.items().tmt().catalog().domains().findings())if(onlyComponents.contains(finding.item())||Set.of("skywars2:harpoon","skywars2:sea_legend").contains(finding.item()))
                require(finding.disposition()==LbcTmtCatalog.Disposition.EXCLUDED_SERVICE||finding.disposition()==LbcTmtCatalog.Disposition.EXCLUDED_INERT_KEY,"native criteria/component items have no false pending: "+finding);
            for(var row:game.items().tmt().catalog().families())for(var property:row.variants()){
                var source=LbcTmtComponents.nativeItem(property.data());require(!onlyComponents.contains(BuiltInRegistries.ITEM.getKey(source).toString()),"native bucket/Oguzok components do not manufacture independent ability tickets");
                var data=(ItemData.IntReader)(Object)property.data();require(data.skywars2$readInt("slow_falling",0)!=1,"writer-only legacy slow_falling flag is not sampled as an independent active ability");}
        }
        void begin(){harpoonAdv=advancement("lbc:true_advancements/tridents/harpoon");seaAdv=advancement("lbc:true_advancements/tridents/sea_legend");
            f.prepareParticipants();f.owner.setGameMode(GameType.SURVIVAL);started=game.server().getTickCount();alphaHealth=f.owner.getHealth();betaHealth=f.other.getHealth();
            try(var stream=Objects.requireNonNull(getClass().getResourceAsStream("/skywars2/tmt_properties.json"))){var flags=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("baseFlags");
                flags.entrySet().forEach(entry->base.putInt(entry.getKey(),entry.getValue().getAsInt()));}
            catch(Exception error){throw new IllegalStateException("Actual TMT source base flags",error);}
            require(base.getIntOr("transformable",0)==1&&base.getIntOr("trident",0)==1,"actual generator header supplies native criterion qualifiers");
            catalog();killCriteria();additionalCriteria();labelDoesNotCast();useBegin();}
        void additionalCriteria(){
            var crowbar=property("crowbar");var slime=property("slimeball");
            var crowbarAdv=advancement("lbc:true_advancements/swords/crowbar");var slimeAdv=advancement("lbc:true_advancements/swords/slimeball");
            var cow=new Cow(EntityTypes.COW,f.level);cow.setPos(f.home.add(4,4,0));cow.setNoAi(true);cow.setNoGravity(true);
            require(f.level.addFreshEntity(cow),"native crowbar nonplayer witness");f.own(cow);
            for(int carrier=0;carrier<5;carrier++){
                clear();var stack=combined(carrier,crowbar,slime,sibling);var saved=stack.copy();f.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
                revoke(crowbarAdv);CriteriaTriggers.PLAYER_KILLED_ENTITY.trigger(f.owner,f.other,f.owner.damageSources().playerAttack(f.owner));
                require(done(crowbarAdv),"Crowbar real player kill criterion on carrier="+carrier);unchanged(stack,saved);
                revoke(crowbarAdv);CriteriaTriggers.PLAYER_KILLED_ENTITY.trigger(f.owner,cow,f.owner.damageSources().playerAttack(f.owner));
                require(!done(crowbarAdv),"Crowbar rejects nonplayer criterion");
                f.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);f.owner.getInventory().setItem(9,stack);
                revoke(slimeAdv);CriteriaTriggers.INVENTORY_CHANGED.trigger(f.owner,f.owner.getInventory(),stack);
                require(done(slimeAdv),"Slimeball actual inventory criterion on carrier="+carrier);unchanged(stack,saved);
            }
            clear();var offhand=combined(0,crowbar,sibling);f.owner.setItemInHand(InteractionHand.OFF_HAND,offhand);
            revoke(crowbarAdv);CriteriaTriggers.PLAYER_KILLED_ENTITY.trigger(f.owner,f.other,f.owner.damageSources().playerAttack(f.owner));require(!done(crowbarAdv),"Crowbar offhand does not satisfy mainhand");
            var plain=combined(0,sibling);f.owner.getInventory().setItem(9,plain);revoke(slimeAdv);
            CriteriaTriggers.INVENTORY_CHANGED.trigger(f.owner,f.owner.getInventory(),plain);require(!done(slimeAdv),"Slimeball rejects plain header");
            clear();var spectator=combined(0,slime,sibling);f.owner.getInventory().setItem(9,spectator);f.owner.setGameMode(GameType.SPECTATOR);revoke(slimeAdv);
            CriteriaTriggers.INVENTORY_CHANGED.trigger(f.owner,f.owner.getInventory(),spectator);require(!done(slimeAdv),"Slimeball rejects spectator");f.owner.setGameMode(GameType.SURVIVAL);
            f.discard(cow);require(f.owner.getHealth()==alphaHealth&&f.other.getHealth()==betaHealth,"additional native criteria do not simulate player deaths");
        }
        void killCriterion(Entity victim,boolean expected,String why){revoke(harpoonAdv);
            CriteriaTriggers.PLAYER_KILLED_ENTITY.trigger(f.owner,victim,f.owner.damageSources().playerAttack(f.owner));
            require(done(harpoonAdv)==expected,"actual native PLAYER_KILLED_ENTITY predicate, not a simulated death: "+why);}
        void killCriteria(){for(int carrier=0;carrier<5;carrier++){
                clear();var stack=combined(carrier,harpoon,sibling);var saved=stack.copy();f.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);killCriterion(f.other,true,"all5 physical carriers mainhand="+carrier);unchanged(stack,saved);}
            clear();f.owner.setItemInHand(InteractionHand.MAIN_HAND,combined(0,sibling));f.owner.setItemInHand(InteractionHand.OFF_HAND,combined(1,harpoon,sibling));killCriterion(f.other,false,"offhand label cannot meet mainhand requirement");
            clear();f.owner.setItemInHand(InteractionHand.MAIN_HAND,combined(0,sibling));killCriterion(f.other,false,"plain qualified TMT header lacks Harpoon label");
            var cow=new Cow(EntityTypes.COW,f.level);cow.setPos(f.home.add(4,4,0));cow.setNoAi(true);cow.setNoGravity(true);require(f.level.addFreshEntity(cow),"native nonplayer criterion witness");f.own(cow);
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,combined(0,harpoon,sibling));killCriterion(cow,false,"victim must genuinely be a player");f.discard(cow);
            require(f.owner.getHealth()==alphaHealth&&f.other.getHealth()==betaHealth&&f.owner.isAlive()&&f.other.isAlive(),"criterion boundary causes no player death, damage or resurrection");}
        long tridents(){long count=0;for(var actor:f.level.getAllEntities())if(actor instanceof ThrownTrident&&!original.contains(actor.getUUID()))count++;return count;}
        void labelDoesNotCast(){clear();var stack=combined(0,harpoon,sea,sibling);var saved=stack.copy();f.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);revoke(seaAdv);long actors=tridents();
            require(stack.getEnchantments().isEmpty(),"private labels do not copy native trident enchantments onto physical carrot");
            f.owner.gameMode.useItem(f.owner,f.level,stack,InteractionHand.MAIN_HAND);
            require(!f.owner.isUsingItem()&&!f.owner.isAutoSpinAttack()&&tridents()==actors&&!done(seaAdv),"criterion labels alone create no trident body, charge, Riptide/spin or fictional carrot using event");unchanged(stack,saved);}
        void useBegin(){clear();revoke(seaAdv);boolean plain=useCase==0;int carrier=useCase<3?1:4;var hand=useCase==2||useCase==4?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
            current=plain?combined(carrier,sibling):combined(carrier,sea,sibling);before=current.copy();dataBefore=current.get(DataComponents.CUSTOM_DATA);f.owner.setItemInHand(hand,current);
            if(carrier==1)f.owner.getInventory().setItem(9,new ItemStack(Items.ARROW));
            require(f.owner.gameMode.useItem(f.owner,f.level,current,hand).consumesAction()&&f.owner.isUsingItem()&&f.owner.getUseItem()==current&&f.owner.getUsedItemHand()==hand,"actual native bow+arrow or apple starts real physical use case="+useCase);
            when=game.server().getTickCount();phase=0;}
        void spectator(){clear();revoke(seaAdv);current=combined(1,sea,sibling);before=current.copy();f.owner.setItemInHand(InteractionHand.MAIN_HAND,current);f.owner.setGameMode(GameType.SPECTATOR);
            // This direct boundary is a controlled negative only. Positives above use genuine native ticks.
            CriteriaTriggers.USING_ITEM.trigger(f.owner,current);require(!done(seaAdv),"retained native Sea Legend criterion rejects actual spectator GameType");unchanged(current,before);f.owner.setGameMode(GameType.SURVIVAL);}
        void bucketBegin(){spectator();clear();bucket=f.leaf("bucket_head");var equipment=bucket.get(DataComponents.EQUIPPABLE);
            require(bucket.is(LbcUtilityItems.Kind.BUCKET_HEAD.item)&&equipment!=null&&equipment.slot()==EquipmentSlot.HEAD&&bucket.getMaxDamage()==200,"real native bucket has HEAD equipment and authored durability200");
            double armor=bucket.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream().filter(entry->entry.attribute().equals(Attributes.ARMOR)&&entry.slot().test(EquipmentSlot.HEAD)).mapToDouble(entry->entry.modifier().amount()).sum();require(armor==3,"authored source bucket armor3 component survives ability exclusion");
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,bucket);require(f.owner.gameMode.useItem(f.owner,f.level,bucket,InteractionHand.MAIN_HAND).consumesAction(),"actual native EQUIPPABLE use equips source bucket");when=game.server().getTickCount();phase=1;}
        boolean step(){f.client.acknowledge();f.audience.acknowledge();require(game.server().getTickCount()-started<40,"bounded native criterion scene phase="+phase+" case="+useCase);
            if(game.server().getTickCount()==when)return false;
            if(phase==0){require(!done(seaAdv),"using a labeled bow/apple cannot award successful Sea Legend propulsion case="+useCase);
                require(f.owner.isUsingItem()&&current.get(DataComponents.CUSTOM_DATA)==dataBefore&&!f.owner.isAutoSpinAttack()&&tridents()==0,"actual native using retains carrier identity and creates no trident mechanics");unchanged(current,before);
                if(useCase<3)require(f.owner.getInventory().getItem(9).getCount()==1,"native bow use starts with legitimate unconsumed arrow; no fabricated shot");
                f.owner.stopUsingItem();if(++useCase<5)useBegin();else bucketBegin();return false;}
            if(phase==1){require(f.owner.getItemBySlot(EquipmentSlot.HEAD).is(LbcUtilityItems.Kind.BUCKET_HEAD.item)&&f.owner.getArmorValue()==3,"real next native equipment tick applies exactly bucket armor3");
                clear();wood=new ItemStack(Items.WOODEN_SWORD);oguzok=LbcWtf.sword();require(oguzok.getMaxDamage()==wood.getMaxDamage()&&oguzok.get(DataComponents.ATTRIBUTE_MODIFIERS).equals(wood.get(DataComponents.ATTRIBUTE_MODIFIERS)),"component-only Oguzok retains exact genuine wooden-sword durability and attribute entries");
                f.owner.setItemInHand(InteractionHand.MAIN_HAND,wood);when=game.server().getTickCount();phase=2;return false;}
            if(phase==2){woodDamage=f.owner.getAttributeValue(Attributes.ATTACK_DAMAGE);woodSpeed=f.owner.getAttributeValue(Attributes.ATTACK_SPEED);f.owner.setItemInHand(InteractionHand.MAIN_HAND,oguzok);when=game.server().getTickCount();phase=3;return false;}
            if(phase==3){require(f.owner.getAttributeValue(Attributes.ATTACK_DAMAGE)==woodDamage&&f.owner.getAttributeValue(Attributes.ATTACK_SPEED)==woodSpeed,"actual native Oguzok equipment tick preserves wooden-sword effective attributes without a custom ability ticket");return true;}
            throw new IllegalStateException("Unexpected native criterion phase "+phase);}
        public void close(){if(closed)return;closed=true;try{f.owner.stopUsingItem();for(var actor:f.level.getAllEntities())if(!original.contains(actor.getUUID())&&actor instanceof Projectile)f.own(actor);}
            finally{try{f.close();}finally{stats.close();priorProgress.forEach((holder,done)->{for(var key:holder.value().criteria().keySet())if(done.contains(key))f.owner.getAdvancements().award(holder,key);else f.owner.getAdvancements().revoke(holder,key);});}}}
    }
}
