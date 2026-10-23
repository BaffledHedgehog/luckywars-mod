package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Native hand/menu fixtures with bounded service stepping and complete owned-terrain restoration. */
public final class ReactiveFamilyChecks {
    private ReactiveFamilyChecks() { }
    private static String stopEpoch;
    private static ServerPlayer stopOwner;
    private static Fixture stopping;
    private static void require(boolean ok,String message) {
        if(!ok)throw new IllegalStateException("Reactive family: "+message);
    }
    private static void translated(Component text,String key,int color) {
        require(text!=null && text.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tr
                && tr.getKey().equals(key) && !text.getStyle().isItalic() && text.getStyle().getColor()!=null
                && text.getStyle().getColor().getValue()==color,"source translation/style "+key);
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerLevel level;final ServerPlayer owner;
        final GameChecks.WandThirdPlayerState state;
        final BlockPos base;final Vec3 origin;
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final Set<Entity> owned=Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<Long> forced=new HashSet<>();
        final Map<net.minecraft.stats.Stat<Identifier>,Integer> customStats=new LinkedHashMap<>();
        final Map<net.minecraft.stats.Stat<Item>,Integer> itemStats=new LinkedHashMap<>();
        final boolean offhand,shift,sprint,onGround;final int skill,trinket,trinketSlot;
        final int[] trinkets;final Set<String> unlocks;
        boolean retain;
        Fixture(GameController game) {
            this.game=game;level=game.maps().level();owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            require(game.playing() && owner!=null && game.canUseGameplayItems(owner),"actual active match participant");
            require(game.items().reactive().activeCount()==0,"exclusive fixture service");
            state=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
            offhand=game.config().offhandWands;shift=owner.isShiftKeyDown();sprint=owner.isSprinting();onGround=owner.onGround();
            var profile=game.players().get(owner.getUUID());skill=profile.lbcSkill;trinket=profile.lbcTrinket;trinketSlot=profile.lbcTrinketSlot;
            trinkets=profile.trinkets.clone();unlocks=new HashSet<>(profile.lbcUnlocks);
            origin=new Vec3(Math.floor(owner.getX()/16)*16+8.5,250.75,Math.floor(owner.getZ()/16)*16+8.5);base=BlockPos.containing(origin);
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var stat=net.minecraft.stats.Stats.CUSTOM.get(id);customStats.put(stat,owner.getStats().getValue(stat));}
            for(var item:List.of(LbcReactiveItems.REACTIVE,LbcReactiveItems.WALL))for(var type:List.of(net.minecraft.stats.Stats.ITEM_USED,net.minecraft.stats.Stats.ITEM_CRAFTED)) {
                var stat=type.get(item);itemStats.put(stat,owner.getStats().getValue(stat));
            }
        }
        void snapshot(BlockPos pos) {
            var p=pos.immutable();if(blocks.containsKey(p))return;
            int x=p.getX()>>4,z=p.getZ()>>4;long key=net.minecraft.world.level.ChunkPos.pack(x,z);
            if(!level.getForceLoadedChunks().contains(key)){forced.add(key);level.setChunkForced(x,z,true);}
            level.getChunk(x,z);blocks.put(p,game.maps().snapshot(level,p));
        }
        void set(BlockPos pos,BlockState block) {
            snapshot(pos);if(level.getBlockEntity(pos) instanceof Container c)c.clearContent();level.setBlock(pos,block,818);
        }
        void initialize() {
            game.config().offhandWands=false;owner.closeContainer();owner.setGameMode(GameType.SURVIVAL);owner.setNoGravity(true);
            owner.getInventory().clearContent();owner.removeAllEffects();owner.setShiftKeyDown(false);owner.setSprinting(false);
            for(var slot:List.of(net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET))owner.setItemSlot(slot,ItemStack.EMPTY);
            var profile=game.players().get(owner.getUUID());profile.lbcSkill=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);
            for(var p:BlockPos.betweenClosed(base.offset(-9,-4,-4),base.offset(9,10,19)))set(p,Blocks.AIR.defaultBlockState());
            place(origin);owner.setOnGround(true);game.items().mana().maximum(owner,2000);game.items().mana().set(owner,2000);
            game.items().magic().inventoryChanged(owner);game.items().magic().tick();
        }
        void place(Vec3 pos) {owner.teleportTo(level,pos.x,pos.y,pos.z,Set.of(),0,0,true);owner.setDeltaMovement(Vec3.ZERO);}
        void ticks(int count) {for(int i=0;i<count;i++)game.items().reactive().tick();}
        void hands(Item item,InteractionHand hand) {
            owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            owner.setItemInHand(hand,new ItemStack(item));game.items().mana().inventoryChanged(owner);
        }
        boolean nativeUse(InteractionHand hand) {
            return owner.gameMode.useItem(owner,level,owner.getItemInHand(hand),hand).consumesAction();
        }
        void reset() {
            game.items().timeStop().end(level);game.items().reactive().clear(owner);game.items().frost().clear(owner);
            owner.removeAllEffects();owner.setHealth(state.health);owner.setShiftKeyDown(false);owner.setSprinting(false);owner.setOnGround(true);
            owner.getInventory().clearContent();owner.inventoryMenu.getCraftSlots().clearContent();owner.inventoryMenu.setCarried(ItemStack.EMPTY);
            for(var e:owned)e.discard();
            for(var p:blocks.keySet())if(p.getY()<300)set(p,Blocks.AIR.defaultBlockState());
            place(origin);game.items().mana().set(owner,2000);game.items().magic().inventoryChanged(owner);game.items().magic().tick();
        }
        Set<Entity> entities() {var out=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(out::add);return out;}
        List<ItemEntity> newItems(Set<Entity> before) {
            var out=new ArrayList<ItemEntity>();for(var e:level.getAllEntities())if(!before.contains(e) && e instanceof ItemEntity item && item.position().distanceTo(origin)<50){owned.add(item);out.add(item);}return out;
        }
        void presentation() {
            for(boolean wall:List.of(false,true)) {
                var item=wall?LbcReactiveItems.WALL:LbcReactiveItems.REACTIVE;var stack=new ItemStack(item);
                var flags=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
                var keys=wall?Set.of("craftusable","transformable","staff","magicwand","shish_wall_wand"):Set.of("craftusable","magicwand","reactive_shish","spellwithcooldown");
                require(flags.keySet().equals(keys),"exact source flags "+wall);for(var key:keys)require(flags.getIntOr(key,0)==1,"source flag value "+key);
                require((wall?LbcReactiveItems.isWall(stack):LbcReactiveItems.isReactive(stack)) && Identifier.parse(wall?"lbc:tier_b":"lbc:tier_c").equals(stack.get(DataComponents.TOOLTIP_STYLE))
                        && Identifier.parse(wall?"lbc:shish_wall_wand":"minecraft:spruce_sapling").equals(stack.get(DataComponents.ITEM_MODEL)),"native item recognition/model/tier");
                require(stack.getMaxStackSize()==1 && stack.getMaxDamage()==25 && stack.getDamageValue()==0,"native source carrot durability and stack limit");
                translated(stack.getHoverName(),wall?"shish_wall_wand":"reactive_shish",0x00aa00);
                var lore=stack.get(DataComponents.LORE).lines();require(lore.size()==(wall?8:4),"all source lore rows");
                if(!wall) {
                    translated(lore.get(0),"reactive_shish.lore",0x55ff55);translated(lore.get(1),"reactive_shish.lore.1",0x55ff55);
                    translated(lore.get(2),"craft_with_upgraded_wall_wand.1",0xffaa00);translated(lore.get(3),"craft_with_upgraded_wall_wand.2",0xffaa00);
                } else {
                    translated(lore.getFirst(),"type",0x55ffff);require(lore.getFirst().getSiblings().stream().anyMatch(c->c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t && t.getKey().equals("staff")),"joined source staff type");
                    require(lore.get(1).getString().startsWith("500") && !lore.get(1).getStyle().isItalic(),"500 mana-on-hit source lore");
                    for(int i=1;i<=6;i++)translated(lore.get(i+1),"shish_wall_wand.lore."+i,i==6?0xaaaaaa:0xffffff);
                }
                var loot=game.lbc().rollTable(level,origin,owner,wall?"lbc:ported/shish_wall_wand":"lbc:ported/reactive_shish");
                require(loot.size()==1 && loot.getFirst().is(item) && loot.getFirst().getCount()==1
                        && loot.getFirst().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().equals(flags),"real public leaf yields complete native flagged weapon");
                require(Objects.equals(loot.getFirst().get(DataComponents.ITEM_MODEL),stack.get(DataComponents.ITEM_MODEL))
                        && Objects.equals(loot.getFirst().get(DataComponents.TOOLTIP_STYLE),stack.get(DataComponents.TOOLTIP_STYLE)),"public presentation retains source model and tier");
            }
        }
        void reactive() {
            reset();hands(LbcReactiveItems.REACTIVE,InteractionHand.MAIN_HAND);game.items().mana().set(owner,0);
            require(game.items().hud(owner).getString().contains("\uE104"),"shared HUD shows source ready icon while held");
            require(nativeUse(InteractionHand.MAIN_HAND),"native main-hand Reactive use at zero mana");
            var effect=owner.getEffect(MobEffects.LEVITATION);require(effect!=null && effect.getDuration()==20 && effect.getAmplifier()==15,"real20-tick native Levitation amplifier15");
            require(game.items().mana().get(owner)==0 && game.items().reactive().cooldownTicks(owner)==125,"zero mana cost and125 active-tick cooldown");
            require(game.items().reactive().hudValue(owner)==25 && game.items().hud(owner).getString().contains("\uE105 25"),
                    "shared HUD retains source score units and cooldown icon");
            game.config().offhandWands=true;owner.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(LbcReactiveItems.REACTIVE));
            require(!nativeUse(InteractionHand.OFF_HAND) && game.items().reactive().cooldownTicks(owner)==125,"cooldown shared by player across hands");
            owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            var platform=BlockPos.containing(origin).below(2);ticks(29);
            require(level.getBlockState(platform).isAir() && game.items().reactive().cooldownTicks(owner)==96,"platform waits through29 active ticks");
            // The delayed platform follows the player's current floor position, not the cast origin.
            var moved=origin.add(3,2,1);place(moved);var center=BlockPos.containing(moved).below(2);
            for(var p:BlockPos.betweenClosed(center.offset(-1,0,-1),center.offset(1,0,1)))set(p,Blocks.STONE.defaultBlockState());
            ticks(1);platform(center);require(level.getBlockState(platform).isAir(),"cast-origin platform is not created after movement, pending cast survives putting rod away");
            owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcReactiveItems.REACTIVE));owner.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(LbcReactiveItems.REACTIVE));
            ticks(94);require(game.items().reactive().cooldownTicks(owner)==1 && !nativeUse(InteractionHand.MAIN_HAND),"cooldown survives active tick124");
            ticks(1);require(game.items().reactive().cooldownTicks(owner)==0 && nativeUse(InteractionHand.OFF_HAND),"cooldown ready exactly at125 and admits native offhand");
            reset();hands(LbcReactiveItems.REACTIVE,InteractionHand.OFF_HAND);game.config().offhandWands=false;
            require(!nativeUse(InteractionHand.OFF_HAND) && game.items().reactive().cooldownTicks(owner)==0,"offhand configuration gate");
            game.config().offhandWands=true;owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            require(game.items().wands().use(owner,InteractionHand.MAIN_HAND) && game.items().reactive().cooldownTicks(owner)==125,"shared main-hand dispatch routes allowed offhand Reactive once");
            reset();hands(LbcReactiveItems.REACTIVE,InteractionHand.MAIN_HAND);place(new Vec3(origin.x,326.75,origin.z));
            var high=new BlockPos(base.getX(),319,base.getZ());for(var p:BlockPos.betweenClosed(high.offset(-1,0,-1),high.offset(1,0,1)))set(p,Blocks.AIR.defaultBlockState());
            require(nativeUse(InteractionHand.MAIN_HAND),"high-altitude native use");ticks(30);platform(high);
        }
        void platform(BlockPos center) {
            for(var p:BlockPos.betweenClosed(center.offset(-1,0,-1),center.offset(1,0,1))) {
                var block=level.getBlockState(p);require(block.is(Blocks.SPRUCE_LEAVES) && !block.getValue(LeavesBlock.PERSISTENT),"exact3x3 nonpersistent spruce platform cell "+p);
            }
        }
        void cleansingAndLifecycle() {
            reset();hands(LbcReactiveItems.REACTIVE,InteractionHand.MAIN_HAND);
            var debuff=game.lbc().rollTable(level,origin,owner,"lbc:ported/mystical_artifact_debuff").getFirst();owner.getInventory().setItem(34,debuff);
            game.items().magic().inventoryChanged(owner);game.items().magic().tick();
            require(nativeUse(InteractionHand.MAIN_HAND),"native cast beside genuine debuff-cleanser artifact");game.items().magic().tickPassives();
            require(owner.hasEffect(MobEffects.LEVITATION),"real Reactive rod preserves its native buff through debuff cleansing exclusion");
            owner.addEffect(new MobEffectInstance(MobEffects.RAID_OMEN,40,43));
            require(game.items().reactive().cooldownTicks(owner)==0 && !owner.hasEffect(MobEffects.LEVITATION),"native purifier callback clears Reactive state and effect");
            ticks(30);require(level.getBlockState(base.below(2)).isAir(),"purified cast cannot place delayed platform");
            reset();hands(LbcReactiveItems.REACTIVE,InteractionHand.MAIN_HAND);require(nativeUse(InteractionHand.MAIN_HAND),"death-invalidated cast fixture");
            owner.setHealth(0);ticks(1);require(game.items().reactive().cooldownTicks(owner)==0,"dead participant cannot retain delayed Reactive state");owner.setHealth(state.health);
            ticks(30);require(level.getBlockState(base.below(2)).isAir(),"dead cast has no delayed terrain write");
        }
        void guardsAndPause() {
            reset();game.config().offhandWands=true;owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.CARROT_ON_A_STICK));
            owner.getInventory().setItem(5,new ItemStack(LbcReactiveItems.REACTIVE));
            require(!game.items().reactive().use(owner,InteractionHand.MAIN_HAND) && !game.items().wands().use(owner,InteractionHand.MAIN_HAND),"plain held lookalike plus inventory rod cannot cast");
            var fake=new ItemEntity(level,origin.x,origin.y,origin.z,new ItemStack(LbcReactiveItems.REACTIVE));fake.makeFakeItem();fake.setNoGravity(true);
            require(level.addFreshEntity(fake),"native give visual fixture");owned.add(fake);ticks(30);require(game.items().reactive().activeCount()==0,"dropped fake display never casts");
            hands(LbcReactiveItems.REACTIVE,InteractionHand.MAIN_HAND);
            for(var tag:List.of("spectator","rcdenyalways","strict_map_object","sauvojen_tuntija","magic_academy","transformator")) {
                boolean added=owner.entityTags().add(tag);try{require(!game.items().reactive().use(owner,InteractionHand.MAIN_HAND),"protected role blocks cast "+tag);}finally{if(added)owner.entityTags().remove(tag);}
            }
            owner.setGameMode(GameType.SPECTATOR);try{require(!game.items().reactive().use(owner,InteractionHand.MAIN_HAND),"native spectator cannot cast");}finally{owner.setGameMode(GameType.SURVIVAL);owner.setNoGravity(true);}
            for(int i=0;i<10;i++)game.items().frost().add(owner,null);
            try{require(game.items().frost().isFrozen(owner) && !nativeUse(InteractionHand.MAIN_HAND),"native frozen attachment blocks hand dispatch");}
            finally{game.items().frost().clear(owner);owner.removeAllEffects();place(origin);}
            var suppressor=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,level);suppressor.setPos(origin);suppressor.entityTags().add("stopper_magic");
            require(level.addFreshEntity(suppressor),"native antimagic marker");owned.add(suppressor);
            try{require(game.items().magic().suppressed(owner) && !nativeUse(InteractionHand.MAIN_HAND),"normal magic suppression blocks use");}finally{suppressor.discard();}
            require(nativeUse(InteractionHand.MAIN_HAND),"cast before dimension pause");ticks(7);int cooldown=game.items().reactive().cooldownTicks(owner);
            game.items().timeStop().begin(level,200);try{ticks(150);require(game.items().reactive().cooldownTicks(owner)==cooldown && level.getBlockState(base.below(2)).isAir()
                        && !nativeUse(InteractionHand.MAIN_HAND),"Time Stop freezes cooldown and delayed platform with no recast");}finally{game.items().timeStop().end(level);}
            ticks(22);require(level.getBlockState(base.below(2)).isAir(),"pause preserves exact remaining platform delay");ticks(1);platform(base.below(2));
            game.items().reactive().clear(owner);
            reset();hands(LbcReactiveItems.REACTIVE,InteractionHand.MAIN_HAND);require(nativeUse(InteractionHand.MAIN_HAND),"cast before delayed suppression");ticks(29);
            var lateSuppressor=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,level);lateSuppressor.setPos(origin);lateSuppressor.entityTags().add("stopper_magic");
            require(level.addFreshEntity(lateSuppressor),"native delayed antimagic fixture");owned.add(lateSuppressor);
            ticks(1);lateSuppressor.discard();ticks(100);
            require(level.getBlockState(base.below(2)).isAir() && game.items().reactive().cooldownTicks(owner)==0,"suppressed platform attempt is skipped permanently rather than replayed");
        }
        BlockPos target() {return BlockPos.containing(owner.getEyePosition().add(0,0,8));}
        boolean wallUse(int mana) {
            hands(LbcReactiveItems.WALL,InteractionHand.MAIN_HAND);game.items().mana().set(owner,mana);
            boolean result=nativeUse(InteractionHand.MAIN_HAND);
            // Long rays belong to the existing shared engine, not the new cooldown updater.
            for(int i=0;i<80 && game.items().reactive().pendingRayCount()>0;i++){game.items().exasperato().tick();ticks(1);}
            require(game.items().reactive().pendingRayCount()==0,"bounded fixture drains shared loaded-block ray queue");return result;
        }
        Set<BlockPos> bricks() {
            var out=new HashSet<BlockPos>();for(var p:blocks.keySet())if(level.getBlockState(p).is(Blocks.BRICKS))out.add(p);return out;
        }
        void walls() {
            reset();game.config().offhandWands=false;var hit=target();set(hit,Blocks.STONE.defaultBlockState());
            require(!wallUse(499) && game.items().mana().get(owner)==499 && bricks().isEmpty(),"Shish Wall rejects499 mana before ray");
            var cow=new Cow(EntityTypes.COW,level);cow.setPos(owner.position().add(0,0,3));cow.setNoAi(true);cow.setNoGravity(true);
            require(level.addFreshEntity(cow),"native entity before wall target");owned.add(cow);float cowHealth=cow.getHealth();
            require(wallUse(1000) && game.items().mana().get(owner)==500,"first non-air block past entity charges500 once");
            var expected=new HashSet<BlockPos>();for(int x=-3;x<=3;x++)for(int y=-2;y<=3;y++)if(x!=0 || y!=0)expected.add(hit.offset(x,y,0));
            require(bricks().equals(expected) && level.getBlockState(hit).is(Blocks.STONE) && cow.getHealth()==cowHealth,"exact7x6 vertical air-only wall, preserved solid target and nonterminal entity");
            reset();int missingX=base.getX()>>4,missingZ=(base.getZ()+20)>>4;
            while(level.getChunkSource().getChunkNow(missingX,missingZ)!=null)missingZ++;
            int forceCount=level.getForceLoadedChunks().size();
            require(wallUse(1000) && game.items().mana().get(owner)==1000 && bricks().isEmpty(),"open loaded-chunk miss spends no mana");
            require(level.getChunkSource().getChunkNow(missingX,missingZ)==null && level.getForceLoadedChunks().size()==forceCount,"ray terminates at unloaded boundary without loading chunks or adding tickets");
            reset();owner.setOnGround(false);owner.setSprinting(true);hit=target();set(hit,Blocks.STONE.defaultBlockState());
            require(wallUse(500) && game.items().mana().get(owner)==0,"airborne wall charges500");
            expected.clear();for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)if(x!=0 || z!=0)expected.add(hit.offset(x,0,z));
            require(bricks().equals(expected),"airborne horizontal7x7 takes priority over sprinting stairs");
            reset();owner.setOnGround(true);owner.setSprinting(true);hit=target();set(hit,Blocks.STONE.defaultBlockState());
            require(wallUse(500) && game.items().mana().get(owner)==0,"sprinting stair charges500");
            var stair=new HashSet<BlockPos>();for(var p:blocks.keySet())if(level.getBlockState(p).is(Blocks.BRICK_STAIRS))stair.add(p);
            int stairTargetY=hit.getY();require(stair.size()==45 && stair.stream().allMatch(p->p.getY()>stairTargetY)
                    && stair.stream().mapToInt(BlockPos::getY).max().orElseThrow()>stairTargetY+1,"exact native45-block source stair rises beyond impact");
            reset();owner.setOnGround(false);owner.setSprinting(true);owner.setShiftKeyDown(true);hit=target();
            for(var p:BlockPos.betweenClosed(hit.offset(-2,-2,-2),hit.offset(2,2,2)))set(p,Blocks.BRICKS.defaultBlockState());
            var before=entities();require(wallUse(1000),"sneaking dismantle admission");var rewards=newItems(before);
            require(game.items().mana().get(owner)==1000 && rewards.stream().filter(i->i.getItem().is(Items.BRICKS)).mapToInt(i->i.getItem().getCount()).sum()>=15,
                    "sneak dismantle wins over airborne/sprint, returns full bricks and at most500 mana");
            reset();hit=target().south(292);
            // Explicitly prepare the full ray corridor; the ability must never make these chunks resident itself.
            for(int z=base.getZ();z<=hit.getZ();z++)set(new BlockPos(base.getX(),hit.getY(),z),Blocks.AIR.defaultBlockState());
            for(var p:BlockPos.betweenClosed(hit.offset(-4,-3,-1),hit.offset(4,4,1)))set(p,Blocks.AIR.defaultBlockState());
            set(hit,Blocks.STONE.defaultBlockState());forceCount=level.getForceLoadedChunks().size();
            require(wallUse(1000) && game.items().mana().get(owner)==500,"smart shared ray reaches actual block300 blocks away beyond obsolete250 range");
            expected.clear();for(int x=-3;x<=3;x++)for(int y=-2;y<=3;y++)if(x!=0 || y!=0)expected.add(hit.offset(x,y,0));
            require(bricks().equals(expected) && level.getForceLoadedChunks().size()==forceCount,"one terminal at long-range target, no extra admission tickets");
        }
        void crafting() {
            reset();var grid=owner.inventoryMenu.getCraftSlots();var menu=owner.inventoryMenu;
            grid.setItem(0,new ItemStack(LbcReactiveItems.REACTIVE,2));grid.setItem(3,new ItemStack(LbcWands.ACADEMY_WALL,2));
            require(menu.getResultSlot().getItem().is(LbcReactiveItems.WALL),"native2x2 complete Shish Wall preview");
            grid.setItem(1,new ItemStack(Items.STONE));require(menu.getResultSlot().getItem().isEmpty(),"extra ingredient rejects recipe");grid.setItem(1,ItemStack.EMPTY);
            GameChecks.inventoryClick(owner,0,0,ContainerInput.PICKUP);
            require(menu.getCarried().is(LbcReactiveItems.WALL) && grid.getItem(0).getCount()==1 && grid.getItem(3).getCount()==1,"native result take consumes one selected donor pair");
            grid.clearContent();menu.setCarried(ItemStack.EMPTY);grid.setItem(0,new ItemStack(LbcReactiveItems.REACTIVE));grid.setItem(3,new ItemStack(LbcWands.WALL));
            require(menu.getResultSlot().getItem().isEmpty(),"ordinary non-upgraded Wall is not a donor");grid.clearContent();
            var input=CraftingInput.of(2,2,List.of(new ItemStack(LbcReactiveItems.REACTIVE),new ItemStack(LbcWands.ACADEMY_WALL),ItemStack.EMPTY,ItemStack.EMPTY));
            require(LbcCraftingRecipe.craftingKind(input)==LbcCraftingRecipe.Kind.SHISH_WALL,"genuine donor recipe classification");
            var tablePos=base.west(3);set(tablePos,Blocks.CRAFTING_TABLE.defaultBlockState());
            owner.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inventory,player)->new CraftingMenu(id,inventory,
                    net.minecraft.world.inventory.ContainerLevelAccess.create(level,tablePos)),Component.translatable("container.crafting")));
            try {
                var table=(CraftingMenu)owner.containerMenu;var slots=table.getInputGridSlots();
                slots.get(0).setByPlayer(new ItemStack(LbcReactiveItems.REACTIVE,2));slots.get(8).setByPlayer(new ItemStack(LbcWands.ACADEMY_WALL,2));
                require(table.getResultSlot().getItem().is(LbcReactiveItems.WALL),"native3x3 diagonal donor recipe");
                for(int i=0;i<36;i++)owner.getInventory().setItem(i,new ItemStack(Items.STONE,64));
                GameChecks.inventoryClick(owner,0,0,ContainerInput.QUICK_MOVE);require(slots.get(0).getItem().getCount()==2 && slots.get(8).getItem().getCount()==2,"full inventory shift-take preserves both donors");
                owner.getInventory().setItem(0,ItemStack.EMPTY);GameChecks.inventoryClick(owner,0,0,ContainerInput.QUICK_MOVE);
                require(owner.getInventory().getItem(0).is(LbcReactiveItems.WALL) && slots.get(0).getItem().getCount()==1 && slots.get(8).getItem().getCount()==1,"native3x3 shift crafts one pair into one available slot");
                slots.forEach(s->s.setByPlayer(ItemStack.EMPTY));table.setCarried(ItemStack.EMPTY);
            } finally{
                if(owner.containerMenu instanceof CraftingMenu table){table.getInputGridSlots().forEach(s->s.setByPlayer(ItemStack.EMPTY));table.setCarried(ItemStack.EMPTY);}
                owner.closeContainer();owner.getInventory().clearContent();
            }
        }
        public void close() {
            if(!retain){game.items().timeStop().end(level);game.items().reactive().clear(owner);}
            owner.closeContainer();game.items().frost().clear(owner);owned.forEach(Entity::discard);
            for(var entry:blocks.entrySet()){if(level.getBlockEntity(entry.getKey()) instanceof Container c)c.clearContent();game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue());}
            game.items().indirectKills().clear(owner);state.restore(game);owner.setShiftKeyDown(shift);owner.setSprinting(sprint);owner.setOnGround(onGround);
            var profile=game.players().get(owner.getUUID());profile.lbcSkill=skill;profile.lbcTrinket=trinket;profile.lbcTrinketSlot=trinketSlot;
            System.arraycopy(trinkets,0,profile.trinkets,0,trinkets.length);profile.lbcUnlocks.clear();profile.lbcUnlocks.addAll(unlocks);game.config().offhandWands=offhand;
            customStats.forEach((stat,value)->owner.getStats().setValue(owner,stat,value));itemStats.forEach((stat,value)->owner.getStats().setValue(owner,stat,value));
            game.items().magic().inventoryChanged(owner);
            for(long key:forced)level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
        }
    }
    public static void run(GameController game) {
        try(var f=new Fixture(game)){f.initialize();f.presentation();f.reactive();f.cleansingAndLifecycle();f.guardsAndPause();f.walls();f.crafting();}
        SkyWars2.LOGGER.info("[check] PASS reactive-family");
    }
    public static void stopProbe(GameController game) {
        require(stopping==null,"one actual stop probe");var f=new Fixture(game);stopping=f;
        try {
            f.initialize();f.hands(LbcReactiveItems.REACTIVE,InteractionHand.MAIN_HAND);require(f.nativeUse(InteractionHand.MAIN_HAND),"native pending cast before actual stop");
            f.ticks(7);f.hands(LbcReactiveItems.WALL,InteractionHand.MAIN_HAND);require(f.nativeUse(InteractionHand.MAIN_HAND),"queued Wall ray before actual stop");
            require(game.items().reactive().pendingRayCount()==1,"native Wall ray remains pending before shared engine advances");
            stopEpoch=game.items().epoch();stopOwner=f.owner;game.items().timeStop().begin(f.level,200);
            require(game.items().reactive().cooldownTicks(f.owner)==118 && game.items().reactive().activeCount()==2,"live frozen cooldown and pending ray before actual stop");
            f.retain=true;f.close();stopping=null;SkyWars2.LOGGER.info("[check] PASS reactive-family-stop-probe");
        } catch(RuntimeException|Error error){f.retain=false;try{f.close();}finally{stopping=null;}throw error;}
    }
    public static void cleanup(GameController game) {
        require(stopEpoch!=null && !game.items().currentEpoch(stopEpoch) && stopOwner!=null
                && game.items().reactive().activeCount()==0 && game.items().reactive().cooldownTicks(stopOwner)==0,"actual stop invalidates Reactive pending state and cooldown");
        var held=stopOwner.getMainHandItem();stopOwner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcReactiveItems.REACTIVE));
        try{require(!game.items().reactive().use(stopOwner,InteractionHand.MAIN_HAND),"off-stage real Reactive replay rejected");}
        finally{stopOwner.setItemInHand(InteractionHand.MAIN_HAND,held);}stopEpoch=null;stopOwner=null;
        SkyWars2.LOGGER.info("[check] PASS reactive-family-cleanup");
    }
}
