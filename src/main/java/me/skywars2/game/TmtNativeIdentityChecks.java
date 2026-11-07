package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.phys.Vec3;

/** Operator fixture: real damage/glide intake and ordinary scheduled native equipment owners. */
public final class TmtNativeIdentityChecks {
    private TmtNativeIdentityChecks(){}
    private static Scene running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("TMT native identities: "+why);}
    public static void run(GameController game){
        require(running==null,"exclusive scene");var next=new Scene(game);running=next;
        try{next.begin();}catch(Throwable error){running=null;try{next.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}
    }
    public static void tick(GameController game){
        var scene=running;if(scene==null)return;
        try{require(scene.game==game,"same controller");if(!scene.step())return;running=null;scene.close();
            SkyWars2.LOGGER.info("[check] PASS tmt-native-identities (native default normalization, actual ice melee and rocket glide packet, legal mixed four-slot Overload masks/full-set guard, real damage-owned TNT and scheduled head-only night vision; Hylian shield native component stays distinct from sword aura)");
        }catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL tmt-native-identities",error);}
    }
    public static void cancel(GameController game){var scene=running;if(scene==null)return;require(scene.game==game,"same cancel owner");running=null;scene.close();}

    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;
        final EquipmentSlot[] armor={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
        final Item[] originals={LbcOverload.HELMET,LbcOverload.CHESTPLATE,LbcOverload.LEGGINGS,LbcOverload.BOOTS};
        final List<ItemStack> equipped=new ArrayList<>();
        final Map<net.minecraft.server.level.ServerPlayer,Integer> helmetPhases;
        final Object helmetEpoch;
        int phase,when,start;boolean closed,touched;
        @SuppressWarnings("unchecked")
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);
            helmetPhases=new java.util.IdentityHashMap<>((Map<net.minecraft.server.level.ServerPlayer,Integer>)WoolBucketChecks.read(game.items().aimbobRewards(),"phases"));
            helmetEpoch=WoolBucketChecks.read(game.items().aimbobRewards(),"epoch");}
        LbcTmtAbilities.Ability property(Item nativeItem,String flag){
            String id=BuiltInRegistries.ITEM.getKey(nativeItem).toString();
            return game.items().tmt().catalog().families().stream().flatMap(row->row.variants().stream())
                    .filter(row->{var data=(ItemData.IntReader)(Object)row.data();return data.skywars2$readInt(flag,0)==1
                            &&data.skywars2$readString(LbcTmtAbilities.NATIVE,"").equals(id);}).findFirst().orElseThrow(()->new IllegalStateException("Missing normalized native property "+id+"/"+flag));
        }
        ItemStack mixed(EquipmentSlot slot,LbcTmtAbilities.Ability... properties){
            var stack=LbcTmtItems.prototype(0);var data=stack.get(DataComponents.CUSTOM_DATA).copyTag();
            LbcTmtAbilities.write(data,List.of(properties));stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            if(slot!=null)stack.set(DataComponents.EQUIPPABLE,Equippable.builder(slot).build());return stack;
        }
        void next(int n){phase=n;when=game.server().getTickCount();}
        void normalizedDefaults(){
            var items=new ArrayList<Item>(List.of(originals));items.add(LbcItems.ICE_SWORD);items.add(LbcItems.ROCKET_BOOTS);
            for(var item:items){var source=item.getDefaultInstance();var original=source.get(DataComponents.CUSTOM_DATA);
                var data=source.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();data.putString("type","private-type");data.putInt("mode",3);
                LbcTmtNativeIdentities.normalize(source,data);
                require(data.getIntOr(item==LbcItems.ICE_SWORD?"icesword":item==LbcItems.ROCKET_BOOTS?"explosive_feets":"overload",0)==1,"actual native default semantic flag");
                require(data.getIntOr("mage",0)==0&&data.getIntOr("mode",0)==3&&data.getStringOr("type","").equals("private-type"),"no invented Mage set or overwritten private parameters");
                require(source.get(DataComponents.CUSTOM_DATA)==original,"native default remains immutable");
                if(item!=LbcItems.ICE_SWORD&&item!=LbcItems.ROCKET_BOOTS){var authored=f.leaf(BuiltInRegistries.ITEM.getKey(item).getPath());
                    require(authored.is(item)&&ItemData.intValue(authored,"overload",0)==1&&ItemData.intValue(authored,"mage",0)==0,"each normalization agrees with its real authored Overload loot leaf");}}
            var plain=new CompoundTag();LbcTmtNativeIdentities.normalize(new ItemStack(Items.IRON_SWORD),plain);require(plain.isEmpty(),"ordinary carrier gains no identity");
            var shield=LbcMeleeItems.Kind.HYLIAN_SHIELD.item.getDefaultInstance();var tags=new CompoundTag();LbcTmtNativeIdentities.normalize(shield,tags);
            require(LbcTmtNativeIdentities.hylianShield(shield)&&tags.getIntOr("hylian",0)==0&&shield.has(DataComponents.BLOCKS_ATTACKS),"Hylian Shield keeps native blocking contract, never sword aura");
        }
        void begin(){
            require(game.items().frost().cold(f.owner)==0&&game.items().frost().cold(f.other)==0,"exclusive initial frost state");
            require(game.items().rocketJumps(f.owner)==0&&game.items().rocketCooldownTicks(f.owner)==0,"no borrowed native rocket charge state");
            touched=true;normalizedDefaults();f.prepareParticipants();start=game.server().getTickCount();
            game.items().mana().baseRegen(f.owner,0);game.items().mana().set(f.owner,0);
            var ice=mixed(null,property(LbcItems.ICE_SWORD,"icesword"));f.owner.setItemInHand(InteractionHand.MAIN_HAND,ice);
            f.move(f.other,f.owner.position().add(0,0,3));
            require(f.other.hurtServer(f.level,f.other.damageSources().playerAttack(f.owner),1),"actual accepted native player melee damage");
            require(game.items().frost().cold(f.other)>0&&LbcTmtAbilities.read(ice).size()==1,"real ice owner observes native identity/private slot at melee boundary");
            game.items().frost().clear(f.other);f.move(f.other,f.home.add(12,0,0));f.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            for(int i=0;i<armor.length;i++){
                var values=new ArrayList<LbcTmtAbilities.Ability>();values.add(property(originals[i],"overload"));
                if(i==0)values.add(property(Items.IRON_HELMET,"night_vision"));if(i==1)values.add(property(Items.LEATHER_CHESTPLATE,"dynamite"));if(i==3)values.add(property(LbcItems.ROCKET_BOOTS,"explosive_feets"));
                var piece=mixed(armor[i],values.toArray(LbcTmtAbilities.Ability[]::new));equipped.add(piece);f.owner.setItemSlot(armor[i],piece);
                require(piece.get(DataComponents.EQUIPPABLE).slot()==armor[i],"actual legal generated equipment slot");
            }
            game.items().mana().inventoryChanged(f.owner);require(game.items().mana().overloadArmor(f.owner)==15&&!game.items().mana().mageFullSet(f.owner),"four actual slots form only Overload set");
            f.owner.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);game.items().mana().inventoryChanged(f.owner);
            require(game.items().mana().overloadArmor(f.owner)==7&&!game.items().mana().trySpendWand(f.owner,50),"three pieces cannot bypass real native wand payment guard");
            f.owner.setItemSlot(EquipmentSlot.FEET,equipped.get(3));game.items().mana().inventoryChanged(f.owner);
            require(game.items().mana().trySpendWand(f.owner,50)&&game.items().mana().get(f.owner)==-50,"complete set permits exact native overdraft");game.items().mana().set(f.owner,0);
            require(game.items().rocketJumps(f.owner)==3,"real feet equip hook initializes native rocket state");
            f.owner.setOnGround(false);f.owner.setYRot(0);f.owner.setXRot(0);f.owner.setDeltaMovement(Vec3.ZERO);
            f.client.listener.handlePlayerCommand(new ServerboundPlayerCommandPacket(f.owner,ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            require(game.items().rocketJumps(f.owner)==2&&f.owner.getDeltaMovement().lengthSqr()>0,"actual native glide packet consumes one boost and applies motion");f.owner.setDeltaMovement(Vec3.ZERO);f.owner.setOnGround(true);
            var before=new java.util.HashSet<PrimedTnt>(f.level.getEntitiesOfClass(PrimedTnt.class,f.owner.getBoundingBox().inflate(2)));
            f.owner.setInvulnerableTime(0);f.owner.damageCooldownTime=0;
            require(f.owner.hurtServer(f.level,f.owner.damageSources().generic(),1),"actual native incoming damage accepted");
            var bombs=f.level.getEntitiesOfClass(PrimedTnt.class,f.owner.getBoundingBox().inflate(2),entity->!before.contains(entity));
            require(bombs.size()==1&&bombs.getFirst().getOwner()==f.owner&&bombs.getFirst().getFuse()==80,"mixed chest source predicate emits one real owned source-fuse TNT");
            f.own(bombs.getFirst());f.discard(bombs.getFirst());debuffs();f.pause();next(0);
        }
        void debuffs(){f.owner.removeEffect(MobEffects.NIGHT_VISION);for(var effect:List.of(MobEffects.BLINDNESS,MobEffects.DARKNESS,MobEffects.NAUSEA))f.owner.addEffect(ItemEffectLevels.create(effect,200,0));}
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();require(game.server().getTickCount()-start<100,"bounded scheduled scene");
            int elapsed=game.server().getTickCount()-when;
            if(phase==0){if(elapsed<3)return false;
                require(!f.owner.hasEffect(MobEffects.NIGHT_VISION)&&f.owner.hasEffect(MobEffects.BLINDNESS),"native dimension TimeStop holds helmet grants and cleansing");f.thaw();next(1);return false;
            }
            if(phase==1){if(elapsed<21)return false;
                require(f.owner.hasEffect(MobEffects.NIGHT_VISION)&&!f.owner.hasEffect(MobEffects.BLINDNESS)&&!f.owner.hasEffect(MobEffects.DARKNESS)&&!f.owner.hasEffect(MobEffects.NAUSEA),"ordinary native20tick helmet owner grants night vision and clears all three effects");
                for(var piece:equipped)require(LbcTmtAbilities.read(piece).size()>=1&&!LbcTmtAbilities.active(piece),"scheduled owners preserve all private slots and release scopes");
                f.owner.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);f.owner.getInventory().setItem(9,equipped.getFirst());game.items().mana().inventoryChanged(f.owner);debuffs();next(2);return false;
            }
            if(phase==2){if(elapsed<21)return false;
                require(!f.owner.hasEffect(MobEffects.NIGHT_VISION)&&f.owner.hasEffect(MobEffects.BLINDNESS)&&f.owner.hasEffect(MobEffects.DARKNESS)&&f.owner.hasEffect(MobEffects.NAUSEA),"same mixed helmet in inventory never impersonates equipped head");
                require(game.items().mana().overloadArmor(f.owner)==14&&!game.items().mana().mageFullSet(f.owner),"inventory piece cannot complete Overload or Mage set");return true;
            }
            return false;
        }
        @SuppressWarnings("unchecked")
        public void close(){if(closed)return;closed=true;
            try{if(touched){game.items().frost().clear(f.other);game.items().resetRocket(f.owner);}}
            finally{try{f.close();}finally{
                var service=game.items().aimbobRewards();
                var phases=(Map<net.minecraft.server.level.ServerPlayer,Integer>)WoolBucketChecks.read(service,"phases");phases.clear();phases.putAll(helmetPhases);
                try{var epoch=service.getClass().getDeclaredField("epoch");epoch.setAccessible(true);epoch.set(service,helmetEpoch);}
                catch(ReflectiveOperationException error){throw new IllegalStateException("Restore native helmet phase epoch",error);}
            }}
        }
    }
}
