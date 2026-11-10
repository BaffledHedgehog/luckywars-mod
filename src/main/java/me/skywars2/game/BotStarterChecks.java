package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Shared native starter-loot preparation for isolated bot survival and route scenarios. */
final class BotStarterChecks {
    private BotStarterChecks(){}
    abstract static class Fixture extends CarpetBotChecks.Fixture {
        final List<BlockPos> starters=new ArrayList<>();final Set<UUID> preexisting=new HashSet<>();
        final ItemStack[] pack=new ItemStack[36];final Map<EquipmentSlot,ItemStack> armor=new EnumMap<>(EquipmentSlot.class);
        int originalCobblestone;boolean prepared;
        Fixture(GameController game){super(game);}
        Object state(){return ((Map<?,?>)WoolBucketChecks.read(game.carpetBots(),"states")).get(bot.getUUID());}
        static void require(boolean value,String why){if(!value)throw new IllegalStateException("Bot starter: "+why);}
        @Override void setup(){
            for(var actor:base.level.getAllEntities())preexisting.add(actor.getUUID());super.setup();
            // Basic combat scenarios use a synthetic chest. Never let its destruction
            // leak those items into a scenario which promises real starter loot only.
            if(base.level.getBlockEntity(firstChest) instanceof net.minecraft.world.Container old)old.clearContent();
            bot.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);bot.setHealth(20);bot.getFoodData().setFoodLevel(20);bot.getFoodData().setSaturation(5);
            base.level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.NATURAL_HEALTH_REGENERATION,base.regen,game.server());
            var cell=BlockPos.containing(origin);
            for(int x=(cell.getX()-25)>>4;x<=(cell.getX()+25)>>4;x++)for(int z=(cell.getZ()-25)>>4;z<=(cell.getZ()+25)>>4;z++){
                long key=ChunkPos.pack(x,z);if(!base.level.getForceLoadedChunks().contains(key)){forced.add(key);base.level.setChunkForced(x,z,true);}base.level.getChunk(x,z);
            }
            for(int x=-24;x<=24;x++)for(int z=-24;z<=24;z++)for(int y=-3;y<=5;y++)base.set(cell.offset(x,y,z),y==-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
            starters.addAll(starterChests());
            for(var pos:starters)if(base.level.getBlockEntity(pos) instanceof net.minecraft.world.Container chest)
                for(int slot=0;slot<chest.getContainerSize();slot++){
                    var item=chest.getItem(slot);
                    if(item.is(Items.COBBLESTONE))originalCobblestone+=item.getCount();
                    // The scenario supplies the chosen real trap, not another unrelated random roll.
                    if(lucky(item))chest.setItem(slot,ItemStack.EMPTY);
                }
        }
        private static boolean lucky(ItemStack item){return item.getItem() instanceof BlockItem block&&LbcLuckyBlocks.kind(block.getBlock().defaultBlockState())!=null;}
        boolean prepareKit(){
            if(prepared)return true;
            var state=state();if(state==null||Boolean.TRUE.equals(WoolBucketChecks.read(state,"startingLoot"))
                    ||!((LbcBotCrafting)WoolBucketChecks.read(state,"crafting")).prepared())return false;
            require(bot.getMaxHealth()==20,"ordinary native HP");
            require(bot.getInventory().countItem(Items.WATER_BUCKET)>0,"actual starter bucket available");
            require(java.util.stream.IntStream.range(0,36).mapToObj(i->bot.getInventory().getItem(i))
                    .anyMatch(item->item.isCorrectToolForDrops(Blocks.IRON_BARS.defaultBlockState())&&item.getDestroySpeed(Blocks.IRON_BARS.defaultBlockState())>=2),
                    "actual looted or natively crafted pickaxe is ready before the controlled scenario");
            if(originalCobblestone>=3)require(bot.getInventory().countItem(Items.STONE_PICKAXE)==1&&bot.getInventory().countItem(Items.COBBLESTONE)==originalCobblestone-3,
                    "native workbench recipe consumes exactly three looted cobblestone");
            for(int i=0;i<36;i++){var item=bot.getInventory().getItem(i);pack[i]=lucky(item)?ItemStack.EMPTY:item.copy();}
            for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))armor.put(slot,bot.getItemBySlot(slot).copy());
            for(var pos:starters){if(base.level.getBlockEntity(pos) instanceof net.minecraft.world.Container chest)chest.clearContent();base.set(pos,Blocks.AIR.defaultBlockState());}
            prepared=true;SkyWars2.LOGGER.info("[check] Actual prepared starter kit {} armor={} originalCobble={}",Arrays.toString(pack),armor,originalCobblestone);return true;
        }
        void resetStarterBody(Vec3 at){
            require(bot.isAlive()&&game.server().getPlayerList().getPlayer(bot.getUUID())==bot,"same native body before independent scenario");
            game.carpetBots().forget(bot);bot.stopUsingItem();bot.setLastHurtByMob(null);bot.removeAllEffects();bot.getInventory().clearContent();
            bot.setHealth(20);bot.getFoodData().setFoodLevel(20);bot.getFoodData().setSaturation(5);
            for(int i=0;i<36;i++)bot.getInventory().setItem(i,pack[i].copy());armor.forEach((slot,item)->bot.setItemSlot(slot,item.copy()));
            move(bot,at,-90);
        }
        @Override public void close(){
            for(var actor:base.level.getAllEntities())if(!(actor instanceof ServerPlayer)&&!preexisting.contains(actor.getUUID())&&actor.position().distanceToSqr(origin)<120*120)base.owned.add(actor.getUUID());
            super.close();
        }
    }
}
