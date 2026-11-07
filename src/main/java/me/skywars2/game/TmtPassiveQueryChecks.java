package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntry;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.scores.ScoreHolder;

/** Real native loot predicates and scheduled Balance equipment intake; no substitute passive engine. */
public final class TmtPassiveQueryChecks {
    private TmtPassiveQueryChecks(){}
    private static Reactive running;
    private static Active active;
    private static final EquipmentSlot[] EQUIPMENT={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND};
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT passive queries: "+why);}
    private static LbcTmtAbilities.Ability property(GameController game,String flag){return game.items().tmt().catalog().families().stream().flatMap(row->row.variants().stream())
            .filter(row->((ItemData.IntReader)(Object)row.data()).skywars2$readInt(flag,0)==1).findFirst().orElseThrow();}
    private static ItemStack combined(LbcTmtAbilities.Ability... properties){var stack=LbcTmtItems.prototype(0);var data=stack.get(DataComponents.CUSTOM_DATA).copyTag();
        LbcTmtAbilities.write(data,List.of(properties));stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return stack;}
    private static void clear(ServerPlayer player){player.stopUsingItem();player.getInventory().clearContent();for(var slot:EQUIPMENT)player.setItemSlot(slot,ItemStack.EMPTY);player.containerMenu.setCarried(ItemStack.EMPTY);}
    private static final class Score implements AutoCloseable {
        final net.minecraft.world.scores.Scoreboard board;final ScoreHolder holder;final net.minecraft.world.scores.Objective objective;final Integer saved;
        Score(GameController game,String name,String objective){board=game.server().getScoreboard();holder=ScoreHolder.forNameOnly(name);this.objective=Objects.requireNonNull(board.getObjective(objective));
            var value=board.getPlayerScoreInfo(holder,this.objective);saved=value==null?null:value.value();}
        void set(int value){board.getOrCreatePlayerScore(holder,objective,true).set(value);}
        public void close(){if(saved==null)board.resetSinglePlayerScore(holder,objective);else set(saved);}
    }
    private static final class Zaika implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final MatchRandomState random;
        final Score luck,ranked,mode;final List<List<ItemStack>> sets=new ArrayList<>();
        Zaika(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);random=MatchRandomState.capture(f.level.getRandom());
            luck=new Score(game,"final_luck_modifier","lbc.math");ranked=new Score(game,"ranked","lbc.event");mode=new Score(game,"#gamemode","swrg.math");
            f.prepareParticipants();luck.set(1);ranked.set(0);mode.set(1);f.owner.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.LUCK).setBaseValue(0);}
        LootTable table(String name){return game.server().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("lbc:"+name)));}
        @SuppressWarnings("unchecked") List<LootPool> pools(String name){return (List<LootPool>)WoolBucketChecks.read(table(name),"pools");}
        LootParams params(LbcLuckyBlocks.Kind kind){return new LootParams.Builder(f.level).withParameter(LootContextParams.ORIGIN,f.owner.position())
                .withParameter(LootContextParams.THIS_ENTITY,f.owner).withParameter(LootContextParams.BLOCK_STATE,LbcLuckyBlocks.legacyState(kind))
                .withParameter(LootContextParams.TOOL,ItemStack.EMPTY).create(LootContextParamSets.BLOCK);}
        LootContext context(LbcLuckyBlocks.Kind kind){return new LootContext.Builder(params(kind)).withOptionalRandomSeed(17).create(Optional.empty());}
        List<LootPoolEntry> expand(String name,int entry,LootContext context){var result=new ArrayList<LootPoolEntry>();pools(name).getFirst().entries.get(entry).expand(context,result::add);return result;}
        List<ItemStack> nativeNeko(){var state=LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.NEKO).defaultBlockState();return state.getDrops(new LootParams.Builder(f.level)
                .withParameter(LootContextParams.ORIGIN,f.owner.position()).withParameter(LootContextParams.THIS_ENTITY,f.owner).withParameter(LootContextParams.BLOCK_STATE,state)
                .withParameter(LootContextParams.TOOL,ItemStack.EMPTY));}
        boolean same(List<ItemStack> left,List<ItemStack> right){if(left.size()!=right.size())return false;var rest=new ArrayList<>(right);
            for(var stack:left){int hit=-1;for(int i=0;i<rest.size();i++)if(ItemStack.matches(stack,rest.get(i))){hit=i;break;}if(hit<0)return false;rest.remove(hit);}return rest.isEmpty();}
        void expect(boolean zaika,String why){var ctx=context(LbcLuckyBlocks.Kind.NEKO);
            boolean normalBranch=!expand("nekoluckyblock",0,ctx).isEmpty(),zaikaBranch=!expand("nekoluckyblock",1,ctx).isEmpty();
            require(normalBranch!=zaika&&zaikaBranch==zaika,"actual native Neko table branch: "+why+" normal="+normalBranch+" zaika="+zaikaBranch
                    +" mainhand="+f.owner.getMainHandItem().getItem()+" rawFlag="+ItemData.intValue(f.owner.getMainHandItem(),"zaika",0));
            var selected=expand("nekoluckyblock",zaika?1:0,ctx);require(selected.size()==1&&selected.getFirst().getWeight(0)==10,"real native branch retains source chance weight: "+why);
            var loli=context(LbcLuckyBlocks.Kind.LOLI);for(int i:new int[]{2,5,6,9,10,12,19,22,27,29,32})require(expand("loliluckyblock",i,loli).isEmpty()==zaika,"actual Loli negative gate entry="+i+" "+why);
            ranked.set(1);try{require(expand("loliluckyblock",20,context(LbcLuckyBlocks.Kind.LOLI)).isEmpty()==zaika,"actual ranked-only twelfth Loli exclusion: "+why);}finally{ranked.set(0);}
            if(zaika){var output=nativeNeko();require(sets.stream().anyMatch(set->same(set,output)),"actual native BLOCK loot emits one complete source armor set: "+why);}}
        void place(ItemStack stack,int location){clear(f.owner);switch(location){case 0->f.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);case 1->f.owner.getInventory().setItem(9,stack);
            case 2->f.owner.setItemSlot(EquipmentSlot.HEAD,stack);case 3->f.owner.setItemSlot(EquipmentSlot.CHEST,stack);case 4->f.owner.setItemSlot(EquipmentSlot.LEGS,stack);
            case 5->f.owner.setItemSlot(EquipmentSlot.FEET,stack);case 6->f.owner.setItemInHand(InteractionHand.OFF_HAND,stack);default->throw new IllegalArgumentException();}}
        void run(){
            for(int i=0;i<5;i++){var ctx=context(LbcLuckyBlocks.Kind.NEKO);var entries=expand("nekolucky_zaika_items",i,ctx);require(entries.size()==1,"one real full-set family choice");
                var items=new ArrayList<ItemStack>();entries.getFirst().createItemStack(items::add,ctx);require(items.size()==4||items.size()==5,"complete native full-set leaf");sets.add(items);}
            var zaika=property(game,"zaika");var sibling=property(game,"reroll_machine");var admin=f.leaf("admin_zaika");
            // Native transmuteCopy transfers only overrides. Rebuild the full original raw
            // carrier, including Zaika/presentation components inherited from native defaults.
            var ordinary=admin.transmuteCopy(Items.CARROT_ON_A_STICK);ordinary.applyComponents(admin.getComponents());
            require(ordinary.is(Items.CARROT_ON_A_STICK)&&ordinary.getCount()==admin.getCount()
                    &&admin.getComponents().keySet().stream().allMatch(type->Objects.equals(ordinary.get(type),admin.get(type)))
                    &&ItemData.intValue(ordinary,"zaika",0)==1,"raw original-carrot source retains the full authored admin component map and actual passive flag");
            clear(f.owner);expect(false,"withheld");
            for(var source:List.of(ordinary,admin,combined(zaika,sibling)))for(int location=0;location<7;location++){
                var held=source.copy();var original=held.get(DataComponents.CUSTOM_DATA);place(held,location);expect(true,"source="+source.getItem()+" location="+location);
                require(held.get(DataComponents.CUSTOM_DATA)==original,"real loot queries retain private and ordinary source components");
                if(LbcTmtItems.matches(held))require(LbcTmtAbilities.read(held).size()==2&&!LbcTmtAbilities.active(held),"passive predicate reads aggregate traits without entering/replaying a source");}
            for(int location=0;location<7;location++){var negative=combined(sibling);place(negative,location);expect(false,"no private Zaika at location="+location);}
            clear(f.owner);f.other.getInventory().setItem(9,combined(zaika));expect(false,"another player's inventory is not THIS_ENTITY");
            f.owner.containerMenu.setCarried(combined(zaika));expect(false,"cursor is not a native entity inventory slot");f.owner.containerMenu.setCarried(ItemStack.EMPTY);
            place(combined(zaika,sibling),1);var key=new ItemStack(Items.TRIPWIRE_HOOK);var data=new net.minecraft.nbt.CompoundTag();data.putInt("luck_key",1);key.set(DataComponents.CUSTOM_DATA,CustomData.of(data));f.owner.setItemInHand(InteractionHand.MAIN_HAND,key);
            var ctx=context(LbcLuckyBlocks.Kind.NEKO);require(expand("nekoluckyblock",0,ctx).isEmpty()&&expand("nekoluckyblock",1,ctx).isEmpty()&&nativeNeko().isEmpty(),"actual native mainhand luck_key priority remains above Zaika");
            clear(f.owner);var trophy=f.leaf("god");require(trophy.is(LbcHedgehogItems.GOD)&&ItemData.intValue(trophy,"god",0)==1,"real source trophy acquisition");
            for(var hand:InteractionHand.values()){f.owner.setItemInHand(hand,trophy);int mana=game.items().mana().get(f.owner);var original=trophy.copy();
                require(!f.owner.gameMode.useItem(f.owner,f.level,trophy,hand).consumesAction()&&ItemStack.matches(trophy,original)&&game.items().mana().get(f.owner)==mana,"actual usable=false trophy use has no item operation: "+hand);f.owner.setItemInHand(hand,ItemStack.EMPTY);}
        }
        public void close(){try{luck.close();ranked.close();mode.close();random.restore(f.level.getRandom());}finally{f.close();}}
    }
    public static void runZaika(GameController game){require(running==null&&active==null,"exclusive loot scene");try(var f=new Zaika(game)){f.run();}SkyWars2.LOGGER.info("[check] PASS tmt-zaika-queries (ordinary/admin/private native Neko/Loli slot predicates, actual complete BLOCK outputs, all seven held/inventory/equipment locations and cursor/other-player/no-trait negatives; real luck_key priority and usable=false trophy)");}

    private static final class Reactive implements AutoCloseable {
        final GameController game;final ServerPlayer owner;final RegenerationBeltChecks.Snapshot saved;final RegenerationBeltChecks.ScheduledClient client;
        final LbcBalance balance;final LbcBalancePassive passive;final Map<UUID,LbcBalancePassive.Gear> oldGear;
        final Map<UUID,Integer> oldClocks=new LinkedHashMap<>();final Map<UUID,int[]> oldCooldowns=new LinkedHashMap<>();
        final LbcTmtAbilities.Ability trait,sibling;final ItemStack raw;int phase,when,start;long clouds;boolean closed;
        @SuppressWarnings("unchecked") Reactive(GameController game){this.game=game;require(game.playing()&&game.balanceLegacy(),"an actual active legacy Balance overlay, never a reflected mode flag");
            owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));require(game.canUseGameplayItems(owner)&&!game.isObserver(owner)&&!game.items().timeStop().stopped(owner.level()),"ordinary live native participant");
            saved=new RegenerationBeltChecks.Snapshot(game,owner);client=new RegenerationBeltChecks.ScheduledClient(game,owner);balance=game.items().balance();passive=(LbcBalancePassive)WoolBucketChecks.read(balance,"passive");oldGear=passive.image();
            oldClocks.putAll((Map<UUID,Integer>)WoolBucketChecks.read(balance,"clocks"));((Map<UUID,int[]>)WoolBucketChecks.read(balance,"cooldowns")).forEach((id,values)->oldCooldowns.put(id,values.clone()));
            trait=property(game,"reactive_old");sibling=property(game,"reroll_machine");require(LbcTmtComponents.nativeItem(trait.data())==Items.IRON_BOOTS,"actual authored legacy reactive source is iron boots");
            raw=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:item_defs/lbc/reactive__a2337e384e16").getFirst();start=game.server().getTickCount();}
        @SuppressWarnings("unchecked") long clouds(){return ((List<Packet<?>>)WoolBucketChecks.read(client.connection,"packets")).stream().filter(ClientboundLevelParticlesPacket.class::isInstance)
                .map(ClientboundLevelParticlesPacket.class::cast).filter(packet->packet.particle().getType()==ParticleTypes.CLOUD&&packet.count()==1
                        &&Math.abs(packet.x()-owner.getX())<.001&&Math.abs(packet.y()-(owner.getY()-.2))<.001&&Math.abs(packet.z()-owner.getZ())<.001).count();}
        ItemStack wrong(boolean item){var data=trait.data().copyTag();if(item)data.putString(LbcTmtAbilities.NATIVE,"minecraft:leather_boots");else data.putInt("reactive_old",0);
            return combined(new LbcTmtAbilities.Ability("check/reactive-negative",CustomData.of(data)),sibling);}
        void equip(){clear(owner);owner.removeEffect(MobEffects.SLOW_FALLING);owner.setNoGravity(true);owner.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            var mixed=combined(trait,sibling);mixed.set(DataComponents.EQUIPPABLE,Equippable.builder(EquipmentSlot.FEET).build());
            switch(phase){case 0->owner.setItemSlot(EquipmentSlot.FEET,raw.copy());case 1->owner.setItemSlot(EquipmentSlot.FEET,mixed);
                case 2->owner.setItemInHand(InteractionHand.MAIN_HAND,mixed);case 3->owner.getInventory().setItem(9,mixed);case 4->owner.setItemSlot(EquipmentSlot.CHEST,mixed);
                case 5->owner.setItemInHand(InteractionHand.OFF_HAND,mixed);case 6->owner.setItemSlot(EquipmentSlot.FEET,wrong(true));case 7->owner.setItemSlot(EquipmentSlot.FEET,wrong(false));
                case 8->owner.setItemSlot(EquipmentSlot.FEET,new ItemStack(Items.IRON_BOOTS));default->throw new IllegalArgumentException();}
            // Native inventory-menu listeners own InventoryChangeTrigger and cache refresh.
            // No passive.changed(), inventoryChanged() or owner tick is invoked by the fixture.
            owner.inventoryMenu.broadcastChanges();when=game.server().getTickCount();clouds=clouds();}
        boolean step(){client.acknowledge();require(game.balanceLegacy()&&game.server().getTickCount()-start<100,"stable actual legacy scheduled scene");if(game.server().getTickCount()-when<4)return false;
            boolean expected=phase<=1;var gear=passive.image().get(owner.getUUID());require(gear!=null&&gear.reactive()==expected,"actual inventory/equipment intake caches exact FEET/source identity phase="+phase);
            require(owner.hasEffect(MobEffects.SLOW_FALLING)==expected&&(clouds()>clouds)==expected,"actual native Balance scheduled effect and matching source cloud phase="+phase);
            if(expected)require(owner.getEffect(MobEffects.SLOW_FALLING).getAmplifier()==0&&owner.getEffect(MobEffects.SLOW_FALLING).getDuration()<=20,"source Slow Falling I20tick refresh");
            if(phase==1)require(LbcTmtAbilities.read(owner.getItemBySlot(EquipmentSlot.FEET)).size()==2&&!LbcTmtAbilities.active(owner.getItemBySlot(EquipmentSlot.FEET)),"existing passive consumes private identity without replaying sibling properties");
            if(++phase>8)return true;equip();return false;}
        @SuppressWarnings("unchecked") public void close(){if(closed)return;closed=true;try{saved.restoreAttributes();saved.restore(game);saved.restoreUse();
            var clocks=(Map<UUID,Integer>)WoolBucketChecks.read(balance,"clocks");clocks.clear();clocks.putAll(oldClocks);var cooldowns=(Map<UUID,int[]>)WoolBucketChecks.read(balance,"cooldowns");cooldowns.clear();oldCooldowns.forEach((id,values)->cooldowns.put(id,values.clone()));passive.restore(oldGear);
        }finally{client.close();}}
    }
    public static void runReactive(GameController game){require(running==null&&active==null,"exclusive reactive scene");var next=new Reactive(game);running=next;try{next.equip();}catch(Throwable error){running=null;try{next.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    private static final class Active implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final int jumps;
        LbcTmtAbilities.Ability trait,sibling;ItemStack raw,held,feet;CustomData heldData,feetData;
        int when,beforeUsingJumps;long flames,sounds;boolean closed;
        Active(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);jumps=f.owner.getStats().getValue(Stats.CUSTOM.get(Stats.JUMP));}
        @SuppressWarnings("unchecked") List<Packet<?>> packets(boolean audience){return (List<Packet<?>>)WoolBucketChecks.read((audience?f.audience:f.client).connection,"packets");}
        long flames(){return packets(false).stream().filter(ClientboundLevelParticlesPacket.class::isInstance).map(ClientboundLevelParticlesPacket.class::cast)
                .filter(packet->packet.particle().getType()==ParticleTypes.FLAME&&packet.count()==100&&packet.overrideLimiter()&&!packet.alwaysShow()
                        &&Math.abs(packet.xDist()-.2)<.00001&&Math.abs(packet.yDist()-.1)<.00001&&Math.abs(packet.zDist()-.2)<.00001
                        &&Math.abs(packet.xMaxSpeed()-.1)<.00001&&Math.abs(packet.yMaxSpeed()-.1)<.00001&&Math.abs(packet.zMaxSpeed()-.1)<.00001).count();}
        long sounds(boolean audience){return packets(audience).stream().filter(ClientboundSoundPacket.class::isInstance).map(ClientboundSoundPacket.class::cast)
                .filter(packet->packet.getSound().value().location().toString().equals("minecraft:entity.firework_rocket.blast")&&packet.getSource()==SoundSource.MASTER
                        &&packet.getVolume()==10&&packet.getPitch()==0).count();}
        void clean(){clear(f.owner);f.owner.removeEffect(MobEffects.LEVITATION);f.owner.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);f.owner.setNoGravity(true);f.owner.setOnGround(true);}
        ItemStack privateLeather(){var data=trait.data().copyTag();data.putString(LbcTmtAbilities.NATIVE,"minecraft:leather_boots");return combined(new LbcTmtAbilities.Ability("check/active-leather",CustomData.of(data)),sibling);}
        void jump(ItemStack item,int location,boolean expected,String why){clean();switch(location){case 0->f.owner.setItemSlot(EquipmentSlot.FEET,item);case 1->f.owner.setItemInHand(InteractionHand.MAIN_HAND,item);
            case 2->f.owner.getInventory().setItem(9,item);case 3->f.owner.setItemSlot(EquipmentSlot.HEAD,item);case 4->f.owner.setItemSlot(EquipmentSlot.CHEST,item);
            case 5->f.owner.setItemSlot(EquipmentSlot.LEGS,item);case 6->f.owner.setItemInHand(InteractionHand.OFF_HAND,item);default->throw new IllegalArgumentException();}
            var original=item.get(DataComponents.CUSTOM_DATA);long beforeFlames=flames(),beforeSounds=sounds(false);int before=f.owner.getStats().getValue(Stats.CUSTOM.get(Stats.JUMP));
            f.owner.jumpFromGround();require(f.owner.getStats().getValue(Stats.CUSTOM.get(Stats.JUMP))==before+1,"actual native Player jump statistic: "+why);
            require(f.owner.hasEffect(MobEffects.LEVITATION)==expected&&flames()-beforeFlames==(expected?1:0)&&sounds(false)-beforeSounds==(expected?1:0),"real jump source effect/forced particle/Master sound: "+why);
            if(expected){var effect=f.owner.getEffect(MobEffects.LEVITATION);require(effect.getDuration()==20&&effect.getAmplifier()==0&&!effect.isAmbient()&&!effect.isVisible()&&!effect.showIcon(),"exact source Levitation I20 hidden layer: "+why);}
            require(item.get(DataComponents.CUSTOM_DATA)==original&&!LbcTmtAbilities.active(item),"actual jump leaves ordinary/private source data intact: "+why);
        }
        void begin(){f.prepareParticipants();trait=property(game,"reactive_old");sibling=property(game,"reroll_machine");raw=f.loot("lbc:item_defs/lbc/reactive__a2337e384e16");
            jump(raw.copy(),0,true,"ordinary exact iron feet");var leather=raw.transmuteCopy(Items.LEATHER_BOOTS);
            require(!LbcTmtPassiveQueries.reactiveOldFeet(leather),"ordinary noniron active gear is not legacy passive gear");jump(leather,0,true,"ordinary ANY-material active feet");
            jump(combined(trait,sibling),0,true,"actual private iron source");leather=privateLeather();require(!LbcTmtPassiveQueries.reactiveOldFeet(leather),"private noniron active gear is not legacy passive gear");
            jump(leather,0,true,"private ANY-material active feet");for(int location=1;location<7;location++)jump(combined(trait,sibling),location,false,"wrong actual slot="+location);
            var forged=combined(sibling);var data=forged.get(DataComponents.CUSTOM_DATA).copyTag();data.putInt("reactive_old",1);forged.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            jump(forged,0,false,"forged outer flag without a private reactive source");jump(new ItemStack(Items.IRON_BOOTS),0,false,"plain iron without source flag");
            f.move(f.other,f.owner.position().add(21,0,0));long distant=sounds(true);jump(combined(trait,sibling),0,true,"source audible audience cutoff");require(sounds(true)==distant,"source Master audience excludes actual recipient beyond20");
            clean();f.owner.setItemSlot(EquipmentSlot.FEET,combined(trait,sibling));f.pause();try{long before=flames();f.owner.jumpFromGround();require(!f.owner.hasEffect(MobEffects.LEVITATION)&&flames()==before,"actual native world TimeStop closes shared jump admission");}finally{f.thaw();}
            clean();feet=combined(trait,sibling);feet.set(DataComponents.EQUIPPABLE,Equippable.builder(EquipmentSlot.FEET).build());feetData=feet.get(DataComponents.CUSTOM_DATA);f.owner.setItemSlot(EquipmentSlot.FEET,feet);
            held=LbcTmtItems.prototype(4);data=held.get(DataComponents.CUSTOM_DATA).copyTag();LbcTmtAbilities.write(data,List.of(sibling));held.set(DataComponents.CUSTOM_DATA,CustomData.of(data));held.setCount(2);heldData=held.get(DataComponents.CUSTOM_DATA);
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,held);flames=flames();sounds=sounds(false);beforeUsingJumps=f.owner.getStats().getValue(Stats.CUSTOM.get(Stats.JUMP));
            require(f.owner.gameMode.useItem(f.owner,f.level,held,InteractionHand.MAIN_HAND).consumesAction()&&f.owner.isUsingItem(),"real native TMT golden-apple use starts its ordinary using callbacks");when=game.server().getTickCount();
        }
        boolean step(){f.client.acknowledge();f.audience.acknowledge();int elapsed=game.server().getTickCount()-when;require(elapsed<20,"bounded actual native USING armor group");if(elapsed<5)return false;
            require(f.owner.getUseItem()==held&&f.owner.getTicksUsingItem()>=4&&held.getCount()==2,"ordinary sustained use progresses without synthetic consume or jump");
            require(f.owner.hasEffect(MobEffects.LEVITATION)&&f.owner.getEffect(MobEffects.LEVITATION).getAmplifier()==0&&f.owner.getEffect(MobEffects.LEVITATION).getDuration()>=18
                    &&flames()>flames&&sounds(false)>sounds,"real native TMT USING armor SELF group reaches the shared reactive source leaf");
            require(f.owner.getStats().getValue(Stats.CUSTOM.get(Stats.JUMP))==beforeUsingJumps,"armor SELF does not fabricate a physical jump");
            require(held.get(DataComponents.CUSTOM_DATA)==heldData&&feet.get(DataComponents.CUSTOM_DATA)==feetData&&LbcTmtAbilities.read(feet).size()==2,"both independent actual held and feet source properties survive armor SELF");return true;
        }
        public void close(){if(closed)return;closed=true;try{f.owner.stopUsingItem();}finally{try{f.close();}finally{f.owner.getStats().setValue(f.owner,Stats.CUSTOM.get(Stats.JUMP),jumps);}}}
    }
    public static void runActive(GameController game){require(active==null&&running==null,"exclusive active source scene");var next=new Active(game);active=next;
        try{next.begin();}catch(Throwable error){active=null;try{next.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    public static void tick(GameController game){if(active!=null){var scene=active;try{require(scene.game==game,"same active controller");if(scene.step()){active=null;scene.close();SkyWars2.LOGGER.info("[check] PASS tmt-reactive-active (actual native jumps and ordinary TMT USING armor SELF, ANY-material/private feet, exact hidden20tick Levitation/forced100flame/Master10pitch0 audience20, wrong slots and forged outer negatives, actual TimeStop admission, no fake jump or passive iron broadening)");}}
        catch(Throwable error){active=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL tmt-reactive-active",error);}return;}
        var scene=running;if(scene==null)return;try{require(scene.game==game,"same controller");if(!scene.step())return;running=null;scene.close();SkyWars2.LOGGER.info("[check] PASS tmt-reactive-old (actual legacy scheduled native Slow Falling/cloud, ordinary and private exact iron FEET; mainhand/offhand/inventory/chest/wrong-native-source/wrong-flag negatives; real equipment intake, no mode spoof or private owner tick)");}
        catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL tmt-reactive-old",error);}}
    public static void cancel(GameController game){if(active!=null){var scene=active;require(scene.game==game,"same active cancel owner");active=null;scene.close();}var scene=running;if(scene==null)return;require(scene.game==game,"same cancel owner");running=null;scene.close();}
}
