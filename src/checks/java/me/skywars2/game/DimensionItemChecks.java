package me.skywars2.game;

import me.skywars2.SkyWars2;
import me.skywars2.game.entities.LbcDimensionItems;
import me.skywars2.game.items.wands.LbcWands;
import me.skywars2.game.items.loot.LbcLuckyBlocks;
import me.skywars2.game.world.maps.MapManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/** Source policy boundary checks; real dimension/transition integration remains in native suites. */
public final class DimensionItemChecks {
    private DimensionItemChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Dimension items: "+why);}
    private static void classification(GameController game){
        require(!LbcDimensionItems.ordinary("minecraft:nexus")&&!LbcDimensionItems.ordinary("minecraft:imprinted"),"both source excluded dimensions");
        require(LbcDimensionItems.ordinary("minecraft:overworld")&&LbcDimensionItems.ordinary("minecraft:the_nether")&&LbcDimensionItems.ordinary("minecraft:the_end")
                &&game.maps().level()==game.server().overworld(),"ordinary source worlds and actual Overworld map");
        for(String flag:new String[]{"nexus_roadgun","nexus_corrupter","nexus_orb_gun","nexus_pulsar","nexus_compass","nexus_wire_red","cool","heall"}){
            var stack=new ItemStack(Items.CARROT_ON_A_STICK);var tag=new CompoundTag();tag.putInt(flag,1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
            require(LbcDimensionItems.nexusTool(stack)==java.util.Set.of("nexus_roadgun","nexus_corrupter","nexus_orb_gun","nexus_pulsar","nexus_compass").contains(flag),"exact active Nexus RC exception "+flag);
        }
        require(!LbcDimensionItems.nexusTool(ItemStack.EMPTY),"empty item never creates Nexus exception");
        require(!LbcDimensionItems.custom(new ItemStack(Items.STICK))&&!LbcDimensionItems.custom(new ItemStack(Items.ARROW)),"ordinary vanilla items and projectile ammunition remain ordinary");
        var player=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(player!=null&&game.canUseGameplayItems(player)&&game.items().wands().validRoleTarget(player,false),"policy preserves current participant/vanilla target admission");
    }
    private static final class Fixture implements AutoCloseable {
        record Cell(ServerLevel level,BlockPos pos){}
        final java.util.Map<Cell,MapManager.BlockSnapshot> terrain=new java.util.LinkedHashMap<>();
        final java.util.Map<net.minecraft.stats.Stat<net.minecraft.world.item.Item>,Integer> itemStats=new java.util.LinkedHashMap<>();
        java.util.concurrent.CompletableFuture<?> placementReady;
        final GameController game;final net.minecraft.server.level.ServerPlayer player;
        final RegenerationBeltChecks.Snapshot saved;final RegenerationBeltChecks.ScheduledClient client;
        final SauvojenMeditationChecks.Attribution attribution;
        final net.minecraft.server.level.ServerLevel original;final net.minecraft.world.phys.Vec3 at;
        int stage,start;boolean closed;
        Fixture(GameController game){this.game=game;player=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(player!=null,"active native dimension probe");saved=new RegenerationBeltChecks.Snapshot(game,player);attribution=new SauvojenMeditationChecks.Attribution(game,java.util.List.of(player));original=player.level();at=player.position();
            player.removeAllEffects();player.getInventory().clearContent();player.inventoryMenu.getCraftSlots().clearContent();player.containerMenu.setCarried(ItemStack.EMPTY);player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.getAbilities().invulnerable=false;player.setNoGravity(true);player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);player.setHealth(80);
            for(var tag:new String[]{"time_stopped","spectator","strict_map_object","magic_academy","rcdenyalways","sin_greed_active"})player.entityTags().remove(tag);
            for(var kind:LbcLuckyBlocks.Kind.values()){var stat=net.minecraft.stats.Stats.ITEM_USED.get(LbcLuckyBlocks.item(kind));itemStats.put(stat,player.getStats().getValue(stat));}
            player.setShiftKeyDown(false);client=new RegenerationBeltChecks.ScheduledClient(game,player);start=game.server().getTickCount();try{transfer("minecraft:imprinted");}catch(Throwable error){try{close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}
        }
        void transfer(String id){var level=game.server().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.Identifier.parse(id)));require(level!=null,"real native destination "+id);player.teleportTo(level,at.x,Math.min(240,level.getMaxY()-20),at.z,java.util.Set.of(),0,0,true);client.acknowledge();placementReady=game.maps().prepareChunk(level,net.minecraft.world.level.ChunkPos.containing(player.blockPosition().north(2)));}
        void set(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){
            var level=player.level();terrain.computeIfAbsent(new Cell(level,pos.immutable()),cell->game.maps().snapshot(level,pos));
            level.setBlock(pos,state,2|16|32);
        }
        void placements(){
            var target=player.blockPosition().north(2);set(target.below(),Blocks.STONE.defaultBlockState());
            var hit=new BlockHitResult(Vec3.atCenterOf(target.below()).add(0,.5,0),Direction.UP,target.below(),false);
            for(var kind:LbcLuckyBlocks.Kind.values())for(var hand:InteractionHand.values()){
                set(target,Blocks.AIR.defaultBlockState());player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
                var stack=new ItemStack(LbcLuckyBlocks.item(kind),2);player.setItemInHand(hand,stack);
                require(LbcDimensionItems.allowed(player,stack),"physical lucky block admitted in "+player.level().dimension()+" "+kind);
                require(player.gameMode.useItemOn(player,player.level(),stack,hand,hit).consumesAction(),"real native block input succeeds in "+player.level().dimension()+" "+kind+" "+hand);
                require(LbcLuckyBlocks.kind(player.level().getBlockState(target))==kind&&player.getItemInHand(hand).getCount()==1,
                        "native placed lucky block and exactly one used-hand debit "+kind+" "+hand);
            }
        }
        void controls(){
            require(LbcDimensionItems.blocked(player.level())&&game.canUseGameplayItems(player)&&game.allowDamage(player)&&game.items().wands().validRoleTarget(player,false),"actual transferred player remains participant/native hurt target");
            var wand=new ItemStack(LbcWands.ICE);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,wand);player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,ItemStack.EMPTY);var copy=wand.copy();int mana=game.items().mana().get(player);
            require(net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker().interact(player,player.level(),net.minecraft.world.InteractionHand.MAIN_HAND)==net.minecraft.world.InteractionResult.FAIL,"actual shared UseItem callback rejects new custom input");
            require(!game.items().wands().use(player,net.minecraft.world.InteractionHand.MAIN_HAND),"actual direct native wand dispatcher refuses excluded dimension");
            require(mana==game.items().mana().get(player)&&ItemStack.matches(wand,copy),"no mana/payment/consumption from rejected custom input");
            if(LbcDimensionItems.imprinted(player.level())) {
                var victim=net.minecraft.world.entity.EntityTypes.ZOMBIE.create(player.level(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                require(victim!=null,"native melee target");victim.setPos(player.position().add(0,0,2));victim.setNoAi(true);
                try {
                    require(player.level().addFreshEntity(victim),"publish actual Imprinted melee victim");
                    require(net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.invoker().interact(player,player.level(),InteractionHand.MAIN_HAND,victim,null)==net.minecraft.world.InteractionResult.PASS,
                            "custom held wand cannot cancel vanilla Imprinted attack");
                    player.resetAttackStrengthTicker();victim.setInvulnerableTime(0);victim.damageCooldownTime=0;
                    float hp=victim.getHealth();player.attack(victim);
                    require(victim.getHealth()<hp,"native Player.attack reaches real Imprinted target");
                    require(game.items().mana().get(player)==mana,"native melee does not cast/pay disabled wand");
                    var pos=player.blockPosition().below();var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
                    require(net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.invoker().interact(player,player.level(),InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.PASS,
                            "held custom item leaves native Imprinted block interaction alone");
                }finally{victim.discard();}
            }
            var cow=net.minecraft.world.entity.EntityTypes.COW.create(player.level(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);require(cow!=null,"native vanilla projectile shooter");cow.setPos(player.position().add(0,0,3));
            var arrow=new net.minecraft.world.entity.projectile.arrow.Arrow(player.level(),cow,new ItemStack(Items.ARROW),null);arrow.setPos(cow.position());var source=player.damageSources().arrow(arrow,cow);
            player.setInvulnerableTime(0);player.damageCooldownTime=0;float before=player.getHealth();require(game.items().allowDamage(player,source)&&player.hurtServer(player.level(),source,1)&&player.getHealth()<before,"actual native ordinary arrow damage remains accepted in forbidden item dimension");arrow.discard();cow.discard();
            var special=new ItemStack(Items.CARROT_ON_A_STICK);var data=new CompoundTag();data.putInt("nexus_roadgun",1);special.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            require(LbcDimensionItems.allowed(player,special)==player.level().dimension().identifier().toString().equals("minecraft:nexus"),"source Nexus-only exception classification, no new engine");
        }
        void crafting(){
            if(!player.level().dimension().equals(me.skywars2.game.entities.mobs.hedgehogs.LbcHedgehogDimensions.IMPRINTED))return;
            var input=net.minecraft.world.item.crafting.CraftingInput.of(2,1,java.util.List.of(new ItemStack(LbcWands.TINY_RUBY),new ItemStack(LbcWands.TINY_RUBY)));
            var kinds=me.skywars2.game.items.tools.LbcCraftingRecipe.Kind.values();
            for(var kind:kinds)require(kind.recipe.matches(input,original)==kind.recipe.matches(input,player.level()),"every custom recipe shares arena/Imprinted matching: "+kind);
            require(me.skywars2.game.items.tools.LbcCraftingRecipe.Kind.RUBY.recipe.matches(input,player.level()),"real tiny Ruby combination is admitted");
            var grid=player.inventoryMenu.getCraftSlots();grid.clearContent();grid.setItem(0,input.getItem(0).copy());grid.setItem(1,input.getItem(1).copy());
            player.inventoryMenu.slotsChanged(grid);
            require(!player.inventoryMenu.getSlot(0).getItem().isEmpty(),"actual native2x2 Imprinted result preview");
            player.inventoryMenu.clicked(0,0,net.minecraft.world.inventory.ContainerInput.PICKUP,player);
            require(!player.inventoryMenu.getCarried().isEmpty()&&grid.isEmpty(),"actual Imprinted output take consumes both catalysts once");
            player.inventoryMenu.setCarried(ItemStack.EMPTY);grid.clearContent();player.inventoryMenu.slotsChanged(grid);
        }
        boolean step(){client.acknowledge();require(game.server().getTickCount()-start<40,"bounded actual native dimension scheduler");if(!placementReady.isDone())return false;placementReady.join();crafting();controls();placements();if(stage++==0){transfer("minecraft:nexus");return false;}return true;}
        @Override public void close(){if(closed)return;closed=true;try{for(var entry:terrain.entrySet())game.maps().restoreSnapshot(entry.getKey().level(),entry.getKey().pos(),entry.getValue(),2|16|32);for(var entry:itemStats.entrySet())player.getStats().setValue(player,entry.getKey(),entry.getValue());if(player.level()!=original)player.teleportTo(original,at.x,at.y,at.z,java.util.Set.of(),0,0,true);client.acknowledge();saved.restore(game);saved.restoreAttributes();saved.restoreUse();client.acknowledge();}finally{try{client.close();}finally{attribution.close();}}}
    }
    private static Fixture running;
    public static void run(GameController game){require(running==null,"exclusive dimension fixture");classification(game);running=new Fixture(game);}
    public static void tick(GameController game){if(game==null||running==null)return;try{if(running.step()){running.close();running=null;SkyWars2.LOGGER.info("[check] PASS dimension-items (actual native Imprinted/Nexus transfers/ACK; shared UseItem and direct wand rejection/no payment; native ordinary arrow damage remains targetable; exact Nexus exceptions include active Paper compass; scoped restoration)");}}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL dimension-items",error);}}
}
