package me.skywars2.game;

import me.skywars2.SkyWars2;
import me.skywars2.game.academy.LbcAcademy;
import me.skywars2.game.items.wands.LbcWands;
import me.skywars2.game.world.maps.MapManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;
import java.util.*;
import me.skywars2.game.effects.magic.LbcMana;

/** Native template/interaction checks; clicks advance on real server ticks, never by changing the clock. */
public final class AcademySingleUseChecks {
    private AcademySingleUseChecks() { }
    private static Fixture running, stopping;
    private static void require(boolean ok,String message) {
        if(!ok)throw new IllegalStateException("Academy single use: "+message);
    }
    private static int flag(ItemStack stack,String key) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr(key,0);
    }
    private record School(BlockPos origin,Interaction actor,Display.TextDisplay label) { }
    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final ServerLevel level;
        final LbcAcademy academy;
        final ServerPlayer alpha,beta;
        final GameChecks.WandThirdPlayerState alphaState,betaState;
        final BlockPos base;
        final BoundingBox bounds;
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();
        final Set<Long> forced=new HashSet<>();
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<Entity> owned=Collections.newSetFromMap(new IdentityHashMap<>());
        final List<ScheduledTick<Block>> blockTicks=new ArrayList<>();
        final List<ScheduledTick<Fluid>> fluidTicks=new ArrayList<>();
        final Map<BlockPos,net.minecraft.world.level.block.state.BlockState> overlapBackground=new LinkedHashMap<>();
        final int alphaChallenge,betaChallenge,alphaProgress,betaProgress;
        final boolean alphaGround,betaGround,testPlacementDrops;
        School first,second,normal,overlappingToken,overlappingBase;
        BlockPos edited,stray;
        ItemStack pendingBeta;
        long lastTime=Long.MIN_VALUE;
        int phase;
        boolean closed;
        final String epoch;
        Fixture(GameController game) {
            this(game,false);
        }
        Fixture(GameController game,boolean stopIsolation) {
            this.game=game;level=game.maps().level();academy=game.items().academy();epoch=game.items().epoch();
            testPlacementDrops=!stopIsolation;
            alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing() && alpha!=null && beta!=null && game.canUseGameplayItems(alpha)
                    && game.canUseGameplayItems(beta),"two native active participants");
            require(academy.ownedCount()==0 && academy.pendingCount()==0,"exclusive academy service");
            alphaState=new GameChecks.WandThirdPlayerState(alpha,game.items().mana());
            betaState=new GameChecks.WandThirdPlayerState(beta,game.items().mana());
            alphaGround=alpha.onGround();betaGround=beta.onGround();
            var a=game.players().get(alpha.getUUID());var b=game.players().get(beta.getUUID());
            alphaChallenge=a.lbcChallenge;alphaProgress=a.lbcChallengeProgress;
            betaChallenge=b.lbcChallenge;betaProgress=b.lbcChallengeProgress;
            level.getAllEntities().forEach(before::add);
            int x=(int)Math.floor(alpha.getX()/16)*16+2,z=(int)Math.floor(alpha.getZ()/16)*16+2;
            int height=stopIsolation?emptyStopHeight(x,z):240;
            base=new BlockPos(x,height,z);
            bounds=new BoundingBox(base.getX()-2,base.getY()-2,base.getZ()-2,base.getX()+45,base.getY()+13,base.getZ()+13);
        }
        int emptyStopHeight(int x,int z) {
            var missing=new ArrayList<String>();
            for(int cx=(x-2)>>4;cx<=(x+45)>>4;cx++)for(int cz=(z-2)>>4;cz<=(z+13)>>4;cz++){
                boolean blocks=level.getChunkSource().getChunkNow(cx,cz)!=null;
                boolean entities=level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.pack(cx,cz));
                if(!blocks||!entities)missing.add(cx+","+cz+" blocks="+blocks+" entities="+entities);
            }
            require(missing.isEmpty(),"stop fixture needs already resident native columns: "+missing);
            for(int y=level.getMaxY()-32;y>=level.getMinY()+16;y-=16){
                var candidate=new BoundingBox(x-2,y-2,z-2,x+45,y+13,z+13);
                if(before.stream().noneMatch(entity->!(entity instanceof ServerPlayer)&&candidate.isInside(entity.blockPosition())))return y;
            }
            var occupied=before.stream().filter(entity->!(entity instanceof ServerPlayer)
                    &&entity.getX()>=x-2&&entity.getX()<x+46&&entity.getZ()>=z-2&&entity.getZ()<z+14)
                    .map(entity->entity.getType()+"@"+entity.position()).limit(64).toList();
            throw new IllegalStateException("Academy single use: no empty resident stop fixture height in native range; occupied="+occupied);
        }
        void initialize() {
            var template=game.server().getStructureTemplateManager().get(Identifier.parse("lbc:magic_academy")).orElseThrow();
            require(template.getSize().equals(new net.minecraft.core.Vec3i(12,11,12)),"actual source template dimensions");
            for(var entity:before)require(entity instanceof ServerPlayer || !bounds.isInside(entity.blockPosition()),"fixture does not overwrite resident entities");
            for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++)for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++) {
                long key=net.minecraft.world.level.ChunkPos.pack(x,z);
                if(!level.getForceLoadedChunks().contains(key)){forced.add(key);level.setChunkForced(x,z,true);}
                var chunk=level.getChunk(x,z);
                ((LevelChunkTicks<Block>)chunk.getBlockTicks()).getAll().filter(t->bounds.isInside(t.pos())).forEach(blockTicks::add);
                ((LevelChunkTicks<Fluid>)chunk.getFluidTicks()).getAll().filter(t->bounds.isInside(t.pos())).forEach(fluidTicks::add);
            }
            for(var p:BlockPos.betweenClosed(new BlockPos(bounds.minX(),bounds.minY(),bounds.minZ()),new BlockPos(bounds.maxX(),bounds.maxY(),bounds.maxZ())))
                terrain.put(p.immutable(),game.maps().snapshot(level,p));
            for(var player:List.of(alpha,beta)) {
                player.closeContainer();player.setGameMode(GameType.SURVIVAL);player.setNoGravity(true);
                player.getInventory().clearContent();player.removeAllEffects();player.setDeltaMovement(Vec3.ZERO);
                player.teleportTo(level,base.getX()+6,base.getY()+2,base.getZ()+6,Set.of(),0,0,true);
                game.players().get(player.getUUID()).lbcChallenge=0;
            }
            first=testPlacementDrops ? buildOverInventories() : build(base,true);
            second=build(base.offset(16,0,0),true);normal=build(base.offset(32,0,0),false);
            require(first.label.getText().getContents() instanceof TranslatableContents tr
                    && tr.getKey().equals(LbcAcademy.SINGLE_USE_WELCOME)
                    && tr.getFallback()!=null && tr.getFallback().equals("Single-use Academy of Magic"),"exact one-use label and human fallback");
            require(normal.label.getText().getContents() instanceof TranslatableContents tr
                    && tr.getKey().equals("welcome_to_academy"),"normal welcome key retained");
            require(first.label.getText().getStyle().equals(normal.label.getText().getStyle())
                    && second.label.getText().getStyle().equals(normal.label.getText().getStyle()),"source welcome font/color/style retained");
            panel(first,Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLUE));panel(second,Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLUE));panel(normal,Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLUE));
            edited=base.offset(4,2,10);stray=base.offset(-1,2,10);
            level.setBlock(edited,Blocks.GOLD_BLOCK.defaultBlockState(),818);
            level.setBlock(stray,Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLUE).defaultBlockState(),818);
        }
        School buildOverInventories() {
            int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ALL_SIDEEFFECTS;
            var left = base.offset(5,1,4);
            var leftState = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,Direction.NORTH)
                    .setValue(ChestBlock.TYPE,ChestType.LEFT);
            var right = left.relative(ChestBlock.getConnectedDirection(leftState));
            level.setBlock(left,leftState,quiet);
            level.setBlock(right,leftState.setValue(ChestBlock.TYPE,ChestType.RIGHT),quiet);
            var joined = ChestBlock.getContainer((ChestBlock)Blocks.CHEST,leftState,level,left,true);
            require(joined!=null && joined.getContainerSize()==54,"actual native double chest before assembly");
            var firstHalf = (ChestBlockEntity)level.getBlockEntity(left);
            var secondHalf = (ChestBlockEntity)level.getBlockEntity(right);
            loadInventory(firstHalf,"{Items:[{Slot:0b,id:'minecraft:diamond_pickaxe',count:64,components:{'minecraft:damage':17}}]}");
            var wide = firstHalf.getItem(0).copy();
            require(wide.getCount()==64 && wide.getMaxStackSize()==1 && wide.getDamageValue()==17,"real old overstored tool slot");
            var gems = new ItemStack(Items.AMETHYST_SHARD,7);
            secondHalf.setItem(0,gems.copy());

            var lazy = base.offset(5,1,6);
            var untouched = base.offset(-1,1,8);
            level.setBlock(lazy,Blocks.CHEST.defaultBlockState(),quiet);
            level.setBlock(untouched,Blocks.CHEST.defaultBlockState(),quiet);
            var loot = ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("minecraft:chests/igloo_chest"));
            var unopened = (ChestBlockEntity)level.getBlockEntity(lazy);
            var untouchedBox = (ChestBlockEntity)level.getBlockEntity(untouched);
            unopened.setLootTable(loot,174329);
            untouchedBox.setLootTable(loot,174329);

            var shulkerPos = base.offset(6,1,6);
            level.setBlock(shulkerPos,Blocks.SHULKER_BOX.defaultBlockState(),quiet);
            var shulker = (ShulkerBoxBlockEntity)level.getBlockEntity(shulkerPos);
            loadInventory(shulker,"{Items:[{Slot:9b,id:'minecraft:golden_pickaxe',count:36,components:{'minecraft:damage':11}}]}");
            var packed = shulker.getItem(9).copy();
            require(packed.getCount()==36 && packed.getMaxStackSize()==1 && packed.getDamageValue()==11,"real shulker stored slot starts intact");

            var potPos = base.offset(7,1,4);
            level.setBlock(potPos,Blocks.DECORATED_POT.defaultBlockState(),quiet);
            var pot = (DecoratedPotBlockEntity)level.getBlockEntity(potPos);
            loadInventory(pot,"{item:{id:'minecraft:brick',count:70}}");
            var potItems = pot.getTheItem().copy();
            require(potItems.is(Items.BRICK) && potItems.getCount()==70,"native pot loads the complete stored stack");

            var shelfPos = base.offset(8,1,4);
            level.setBlock(shelfPos,Blocks.CHISELED_BOOKSHELF.defaultBlockState(),quiet);
            var shelf = (ChiseledBookShelfBlockEntity)level.getBlockEntity(shelfPos);
            loadInventory(shelf,"{Items:[{Slot:0b,id:'minecraft:book',count:5}]}");
            var shelfBooks = shelf.getItem(0).copy();
            require(shelfBooks.is(Items.BOOK) && shelfBooks.getCount()==5 && shelf.getMaxStackSize()==1,
                    "native chiseled shelf loads the full stack above its slot limit");

            var jukeboxPos = base.offset(9,1,4);
            level.setBlock(jukeboxPos,Blocks.JUKEBOX.defaultBlockState(),quiet);
            var jukebox = (JukeboxBlockEntity)level.getBlockEntity(jukeboxPos);
            var disc = new ItemStack(Items.MUSIC_DISC_13);
            jukebox.setTheItem(disc.copy());
            require(jukebox.getSongPlayer().isPlaying(),"native jukebox plays before the overwrite");

            var lecternPos = base.offset(10,1,4);
            level.setBlock(lecternPos,Blocks.LECTERN.defaultBlockState(),quiet);
            var lectern = (LecternBlockEntity)level.getBlockEntity(lecternPos);
            var lecternBook = new ItemStack(Items.WRITABLE_BOOK);
            lecternBook.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("academy fixture book"));
            require(LecternBlock.tryPlaceBook(null,level,lecternPos,level.getBlockState(lecternPos),lecternBook.copy()),
                    "native interaction places a real book on the lectern");

            // The terminal cell's old comparator signal must reach a diode two blocks away
            // through a solid intermediary. Neither comparator nor intermediary is in the template.
            var signalChest = base.offset(0,1,3);
            var conductor = signalChest.west();
            var comparatorPos = conductor.west();
            level.setBlock(signalChest,Blocks.CHEST.defaultBlockState(),quiet);
            ((ChestBlockEntity)level.getBlockEntity(signalChest)).setItem(0,new ItemStack(Items.COPPER_INGOT,64));
            level.setBlock(conductor,Blocks.STONE.defaultBlockState(),quiet);
            level.setBlock(comparatorPos.below(),Blocks.STONE.defaultBlockState(),quiet);
            level.setBlock(comparatorPos,Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING,Direction.EAST)
                    .setValue(ComparatorBlock.POWERED,true),quiet);
            ((ComparatorBlockEntity)level.getBlockEntity(comparatorPos)).setOutputSignal(1);
            require(!level.getBlockTicks().hasScheduledTick(comparatorPos,Blocks.COMPARATOR),"no preexisting comparator task");

            require(unopened.getLootTable()==loot && untouchedBox.getLootTable()==loot,"loot tables stay ungenerated before any write");
            School school;
            long jukeboxStops;
            try (var client = new RegenerationBeltChecks.ScheduledClient(game,alpha)) {
                school = build(base,true);
                jukeboxStops = SmokeVolumeChecks.flatten(client.packets()).stream()
                        .filter(packet->packet instanceof ClientboundLevelEventPacket event
                                && event.getType()==LevelEvent.SOUND_STOP_JUKEBOX_SONG && event.getPos().equals(jukeboxPos))
                        .count();
            }
            require(firstHalf.isRemoved() && secondHalf.isRemoved() && unopened.isRemoved() && shulker.isRemoved(),
                    "real token placement removed the actual physical inventory blocks");
            require(pot.isRemoved() && shelf.isRemoved() && jukebox.isRemoved() && lectern.isRemoved(),
                    "the same real token placement overwrites pot, shelf, jukebox, and lectern");
            var drops = owned.stream().filter(ItemEntity.class::isInstance).map(ItemEntity.class::cast)
                    .filter(item->!item.isRemoved()).toList();
            int potCount = dropCount(drops,potItems), shelfCount = dropCount(drops,shelfBooks);
            require(potCount==70 && shelfCount==5 && jukeboxStops==1,
                    "committed overwrite recovers the complete pot/shelf stacks and emits one native jukebox stop"
                            + " (pot="+potCount+", shelf="+shelfCount+", stop packets="+jukeboxStops+")");
            require(dropCount(drops,disc)==1 && dropCount(drops,lecternBook)==1,
                    "jukebox record and valid named lectern book each eject exactly once");
            require(dropCount(drops,wide)==64 && dropCount(drops,gems)==7,
                    "native token placement ejects both double chest halves exactly once, preserving tool count/components");
            require(drops.stream().filter(item->item.getItem().is(Items.GOLDEN_APPLE)).mapToInt(item->item.getItem().getCount()).sum()==1
                            && unopened.getLootTable()==null && level.getBlockEntity(untouched)==untouchedBox && untouchedBox.getLootTable()==loot,
                    "removed lazy chest generates once and unwritten lazy chest stays untouched");
            var boxes = drops.stream().map(ItemEntity::getItem).filter(item->item.is(Items.SHULKER_BOX)).toList();
            require(boxes.size()==1 && boxes.getFirst().getCount()==1 && dropCount(drops,packed)==0,
                    "replaced shulker emits exactly one packed box without duplicate loose contents");
            var slots = NonNullList.withSize(27,ItemStack.EMPTY);
            boxes.getFirst().getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).copyInto(slots);
            require(ItemStack.matches(slots.get(9),packed),"packed shulker retains exact authored overstack without clamping");
            require(level.getBlockTicks().hasScheduledTick(comparatorPos,Blocks.COMPARATOR),
                    "actual overwrite queues the old chest comparator update through a solid block");
            drops.forEach(Entity::discard);
            return school;
        }

        private void loadInventory(net.minecraft.world.level.block.entity.BlockEntity container,String nbt) {
            try {
                container.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),
                        TagParser.parseCompoundFully(nbt)));
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException error) {
                throw new IllegalStateException(error);
            }
        }

        private int dropCount(List<ItemEntity> drops,ItemStack stack) {
            return drops.stream().filter(item->ItemStack.isSameItemSameComponents(item.getItem(),stack))
                    .mapToInt(item->item.getItem().getCount()).sum();
        }

        School build(BlockPos origin,boolean token) {
            Vec3 source=Vec3.atLowerCornerOf(origin.offset(6,token?1:0,6)).add(.25,.25,.75);
            var loot=game.lbc().rollTable(level,source,alpha,token?"lbc:ported/academy_token":"lbc:ported/magic_academy_drop");
            require(loot.size()==1 && flag(loot.getFirst(),token?"academy_token":"magic_academy")==1,"real public acquisition leaf");
            var old=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(old::add);
            var drop=new ItemEntity(level,source.x,source.y,source.z,loot.getFirst().copy());drop.setThrower(alpha);drop.setOnGround(true);
            owned.add(drop);require(level.addFreshEntity(drop),"native grounded token admission");
            academy.tick();
            require(drop.isRemoved() && academy.pendingCount()==(token?1:0),"native acquisition admitted; only token keeps queued rows: origin="+origin
                    +" removed="+drop.isRemoved()+" pending="+academy.pendingCount()+" onGround="+drop.onGround()
                    +" canAdmitDrop="+(game.playing()&&game.canUseGameplayItems(alpha)&&drop.level()==level)
                    +" mapsBusy="+game.maps().busy()+" timeStop="+game.items().timeStop().stopped(level)
                    +" blockResident="+(level.getChunkSource().getChunkNow(drop.blockPosition().getX()>>4,drop.blockPosition().getZ()>>4)!=null)
                    +" entitiesLoaded="+level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.pack(drop.blockPosition().getX()>>4,drop.blockPosition().getZ()>>4)));
            for(int i=0;i<20 && academy.pendingCount()>0;i++)academy.tick();
            require(academy.pendingCount()==0,"all eleven native rows completed");
            var actors=new ArrayList<Interaction>();var labels=new ArrayList<Display.TextDisplay>();
            for(var e:level.getAllEntities())if(!old.contains(e)) {
                owned.add(e);
                if(e instanceof Interaction actor && actor.entityTags().contains("magic_academy"))actors.add(actor);
                if(e instanceof Display.TextDisplay label)labels.add(label);
            }
            require(actors.size()==1 && labels.size()==1,"one actual native Interaction and one TextDisplay");
            require(actors.getFirst().position().distanceToSqr(Vec3.atLowerCornerOf(origin).add(6,1,10.5))<.0001,"source interaction coordinates");
            require(labels.getFirst().position().distanceToSqr(Vec3.atLowerCornerOf(origin).add(6,token?2.25:2,token?9.97:10))<.0001,"authored token title above floor and clear of panel; ordinary source coordinates");
            if(token){
                var title=labels.getFirst();
                require(level.getBlockState(title.blockPosition()).isAir()&&title.getY()>origin.getY()+2
                        &&title.getZ()<origin.getZ()+10,"actual token label origin is in front of solid background");
            }
            return new School(origin,actors.getFirst(),labels.getFirst());
        }
        void panel(School school,Block block) {
            for(int x=4;x<=7;x++)for(int y=2;y<=3;y++)
                require(level.getBlockState(school.origin.offset(x,y,10)).is(block),"exact eight source background cells: "+block);
        }
        void alive(School school) {require(!school.actor.isRemoved() && !school.label.isRemoved(),"own NPC and label remain usable");}
        void click(ServerPlayer player,School school) {require(academy.interact(player,InteractionHand.MAIN_HAND,school.actor),"actual interaction callback");}
        String interactionState(ServerPlayer player,School school) {
            return " player="+player.getScoreboardName()+" alive="+player.isAlive()+" removed="+player.isRemoved()
                    +" mode="+player.gameMode()+" observer="+game.isObserver(player)+" gameplay="+game.canUseGameplayItems(player)
                    +" playerTags="+player.entityTags()+" frost="+game.items().frost().isFrozen(player)
                    +" dimensionStop="+game.items().timeStop().stopped(level)+" dimension="+player.level().dimension()
                    +" actorAlive="+school.actor.isAlive()+" actorRemoved="+school.actor.isRemoved()+" actorTags="+school.actor.entityTags()
                    +" item="+player.getMainHandItem()+" data="+player.getMainHandItem().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag()
                    +" gameTime="+level.getGameTime()+" fixtureEpochCurrent="+game.items().currentEpoch(epoch);
        }
        void wand(ServerPlayer player) {player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcWands.FIRE_WAND));}
        void ready(ServerPlayer player) {
            var trial=player.getMainHandItem();require(flag(trial,"upgradable")==1,"actual trial assigned");
            academy.ready(player,InteractionHand.MAIN_HAND,trial);
            require(flag(trial,"upgrade_available")==1 && !trial.is(LbcWands.ACADEMY_FIRE),"readiness only, no premature item promotion");
        }
        void promoted(ServerPlayer player) {
            require(player.getMainHandItem().is(LbcWands.ACADEMY_FIRE) && flag(player.getMainHandItem(),"upgraded")==1,"actual base-to-upgraded native item replacement");
        }
        Set<UUID> projectiles(){
            var result=new HashSet<UUID>();
            for(var entity:level.getAllEntities())if(entity instanceof net.minecraft.world.entity.projectile.Projectile)result.add(entity.getUUID());
            return result;
        }
        void afterConsumedClick(ServerPlayer player){
            var mana=game.items().mana();mana.set(player,5000);int beforeMana=mana.get(player);
            var before=projectiles();float pitch=player.getXRot();player.setXRot(-89);
            try{
                require(net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker()
                        .interact(player,level,InteractionHand.MAIN_HAND).consumesAction(),
                        "removed teacher still consumes duplicate registered use callback");
                require(player.gameMode.useItem(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction(),
                        "removed teacher still consumes duplicate native item dispatch");
                require(mana.get(player)==beforeMana&&projectiles().equals(before),
                        "same click after single-use promotion creates no spell or mana debit without a teacher hitbox");
            }finally{player.setXRot(pitch);}
        }
        boolean step() {
            require(game.playing(),"match remains active");
            long now=level.getGameTime();if(now==lastTime)return false;lastTime=now;
            switch(phase++) {
                case 0 -> {
                    alpha.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
                    require(!academy.interact(alpha,InteractionHand.MAIN_HAND,first.actor),"ordinary item clicks pass through the teacher");alive(first);
                    beta.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcWands.ACADEMY_FIRE));click(beta,first);alive(first);
                }
                case 1 -> {
                    wand(alpha);academy.interact(alpha,InteractionHand.OFF_HAND,first.actor);
                    require(flag(alpha.getMainHandItem(),"upgradable")==0,"offhand cannot start trial");
                    game.spectators().enter(beta);wand(beta);click(beta,first);
                    require(flag(beta.getMainHandItem(),"upgradable")==0,"observer cannot start trial or spend academy");
                    game.spectators().remove(beta);beta.setGameMode(GameType.SURVIVAL);
                    click(alpha,first);require(flag(alpha.getMainHandItem(),"upgradable")==1,"supported native trial assignment;"+interactionState(alpha,first));alive(first);
                }
                case 2 -> {
                    click(alpha,first);require(flag(alpha.getMainHandItem(),"upgradable")==1,"unmet conditions leave trial intact");alive(first);
                    ready(alpha);alive(first);panel(second,Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLUE));panel(normal,Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLUE));
                    // Same-tick duplicate packet cannot turn readiness into promotion.
                    click(alpha,first);require(!alpha.getMainHandItem().is(LbcWands.ACADEMY_FIRE),"same-tick duplicate debounce");
                }
                case 3 -> {
                    game.items().timeStop().begin(level,40);click(alpha,first);
                    require(!alpha.getMainHandItem().is(LbcWands.ACADEMY_FIRE),"Time Stop guards actual promotion");alive(first);
                    game.items().timeStop().end(level);click(alpha,first);promoted(alpha);
                    require(first.actor.isRemoved() && first.label.isRemoved(),"successful promotion removes only own foreground and actor");
                    afterConsumedClick(alpha);
                    for(int x=4;x<=7;x++)for(int y=2;y<=3;y++) {
                        var p=base.offset(x,y,10);require(level.getBlockState(p).is(p.equals(edited)?Blocks.GOLD_BLOCK:Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.WHITE)),"exact owned blue-to-white conversion preserves block edit");
                    }
                    require(level.getBlockState(stray).is(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLUE)),"nearby unrelated blue remains blue");
                    alive(second);alive(normal);panel(second,Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLUE));panel(normal,Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLUE));
                    wand(beta);click(beta,second);ready(beta);pendingBeta=beta.getMainHandItem();
                }
                case 4 -> {
                    var before=projectiles();int mana=game.items().mana().get(alpha);float pitch=alpha.getXRot();alpha.setXRot(-89);
                    try{
                        require(alpha.gameMode.useItem(alpha,level,alpha.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction()
                                &&game.items().mana().get(alpha)<mana&&!projectiles().equals(before),
                                "next real tick permits an independent ordinary cast away from the consumed teacher");
                    }finally{alpha.setXRot(pitch);}
                    wand(alpha);click(alpha,second);ready(alpha);alive(second);
                }
                case 5 -> {
                    click(alpha,second);promoted(alpha);click(alpha,second);click(beta,second);
                    require(beta.getMainHandItem()==pendingBeta && flag(pendingBeta,"upgraded")==0,"two-player and duplicate clicks cannot spend twice");
                    require(second.actor.isRemoved() && second.label.isRemoved(),"second instance independently consumed");panel(second,Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.WHITE));
                    alive(normal);panel(normal,Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLUE));
                }
                case 6 -> {wand(alpha);click(alpha,normal);ready(alpha);click(beta,normal);promoted(beta);alive(normal);panel(normal,Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLUE));}
                case 7 -> {click(alpha,normal);promoted(alpha);alive(normal);panel(normal,Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLUE));}
                case 8 -> {
                    overlappingToken=build(base,true);
                    overlappingBase=build(base.offset(1,0,0),false);
                    for(int x=4;x<=7;x++)for(int y=2;y<=3;y++) {
                        var p=base.offset(x,y,10);overlapBackground.put(p,level.getBlockState(p));
                    }
                    wand(alpha);click(alpha,overlappingToken);ready(alpha);alive(overlappingToken);
                }
                case 9 -> {
                    click(alpha,overlappingToken);promoted(alpha);
                    require(overlappingToken.actor.isRemoved() && overlappingToken.label.isRemoved(),"overlapped instance still closes its own actors");
                    alive(overlappingBase);panel(overlappingBase,Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLUE));
                    // The newer base wrote this whole footprint, including the three shared blue columns.
                    overlapBackground.forEach((p,state)->require(level.getBlockState(p).equals(state),"newer overlapping academy owns its current terrain"));
                    alive(normal);panel(normal,Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLUE));
                    academy.stop();require(academy.pendingCount()==0 && academy.ownedCount()==0 && normal.actor.isRemoved()
                            && normal.label.isRemoved(),"quiet stop removes all remaining owned actors");
                    panel(normal,Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLUE));panel(second,Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.WHITE));return true;
                }
                default -> throw new IllegalStateException("unexpected phase");
            }
            return false;
        }
        public void close() {
            if(closed)return;closed=true;
            game.items().timeStop().end(level);academy.stop();owned.forEach(Entity::discard);
            level.getBlockTicks().clearArea(bounds);level.getFluidTicks().clearArea(bounds);
            for(var entry:terrain.entrySet()) {
                if(level.getBlockEntity(entry.getKey()) instanceof net.minecraft.world.Container c)c.clearContent();
                game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue());
            }
            level.getBlockTicks().clearArea(bounds);level.getFluidTicks().clearArea(bounds);
            blockTicks.forEach(level.getBlockTicks()::schedule);fluidTicks.forEach(level.getFluidTicks()::schedule);
            for(long key:forced)level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
            var a=game.players().get(alpha.getUUID());var b=game.players().get(beta.getUUID());
            a.lbcChallenge=alphaChallenge;a.lbcChallengeProgress=alphaProgress;b.lbcChallenge=betaChallenge;b.lbcChallengeProgress=betaProgress;
            alphaState.restore(game);betaState.restore(game);alpha.setOnGround(alphaGround);beta.setOnGround(betaGround);
            if(!game.items().currentEpoch(epoch)){
                // Retained probes borrow manual source pauses from earlier stop fixtures.
                // Those pauses belong to the expired match and must not be resurrected by
                // this later whole-player snapshot after the production stop cleared them.
                alpha.entityTags().remove("time_stopped");beta.entityTags().remove("time_stopped");
                require(!alpha.entityTags().contains("time_stopped")&&!beta.entityTags().contains("time_stopped"),"expired fixture restores no borrowed manual pause");
            }
        }
    }
    public static void run(GameController game) {
        require(running==null && stopping==null,"one fixture at a time");running=new Fixture(game);
        try{running.initialize();}catch(RuntimeException|Error error){try{running.close();}finally{running=null;}throw error;}
    }
    public static void tick(GameController game) {
        if(running==null)return;var state=running;
        try{if(state.step()){state.close();running=null;SkyWars2.LOGGER.info("[check] PASS academy-single-use");}}
        catch(Throwable error){try{state.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}
            SkyWars2.LOGGER.error("[check] FAIL academy-single-use",error);}
    }
    public static void stopProbe(GameController game) {
        require(running==null && stopping==null,"actual stop fixture exclusive");stopping=new Fixture(game,true);
        try {
            // Earlier retained probes can pause this dimension and keep actors near the players.
            // Build these isolated schools normally, then retain their own pause until match stop.
            game.items().timeStop().end(stopping.level);
            stopping.initialize();game.items().timeStop().begin(stopping.level,200);
            SkyWars2.LOGGER.info("[check] PASS academy-single-use-stop-probe");
        }catch(RuntimeException|Error error){try{stopping.close();}finally{stopping=null;}throw error;}
    }
    public static void cleanup(GameController game) {
        var state=stopping;require(state!=null,"retained actual stop probe");
        try {
            require(!game.items().currentEpoch(state.epoch) && state.academy.pendingCount()==0
                    && state.academy.ownedCount()==0,"actual lifecycle invalidates epoch and clears service");
            require(state.first.actor.isRemoved() && state.first.label.isRemoved() && state.second.actor.isRemoved()
                    && state.second.label.isRemoved() && state.normal.actor.isRemoved(),"actual stop removes own native actors");
            state.panel(state.second,Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLUE));state.panel(state.normal,Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLUE));
            SkyWars2.LOGGER.info("[check] PASS academy-single-use-cleanup");
        }finally{state.close();stopping=null;}
    }
}
