package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** A physically occluded table, finite ingredients and the actual native crafting menu. */
final class BotWorkbenchChecks {
    private BotWorkbenchChecks(){}
    private static Fixture running;
    private static final class Fixture extends BotStarterChecks.Fixture {
        int started,lastChange=-1,signature;boolean sawMenu;Vec3 from;BlockPos bench;
        Fixture(GameController game){super(game);}
        @Override void setup(){super.setup();
            for(var pos:starters){if(base.level.getBlockEntity(pos) instanceof net.minecraft.world.Container chest)chest.clearContent();base.set(pos,Blocks.AIR.defaultBlockState());}
            base.set(firstChest,Blocks.AIR.defaultBlockState());game.carpetBots().forget(bot);bot.getInventory().clearContent();
            bench=BlockPos.containing(origin.add(5,0,0));base.set(bench,Blocks.CRAFTING_TABLE.defaultBlockState());
            for(int z=-1;z<=1;z++)for(int y=0;y<2;y++)base.set(BlockPos.containing(origin.add(3,y,z)),Blocks.BEDROCK.defaultBlockState());
            bot.getInventory().setItem(0,new ItemStack(Items.OAK_PLANKS,8));from=bot.position();
            var hit=base.level.clip(new ClipContext(bot.getEyePosition(),Vec3.atCenterOf(bench),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,bot));
            require(hit.getType()==HitResult.Type.BLOCK&&!hit.getBlockPos().equals(bench),"table centre genuinely occluded from starting eye");
            started=game.server().getTickCount();signature=signature();
        }
        int signature(){int value=1;for(int i=0;i<36;i++){var stack=bot.getInventory().getItem(i);value=31*value+ItemStack.hashItemAndComponents(stack);value=31*value+stack.getCount();}
            for(var slot:bot.containerMenu.slots)if(slot.container!=bot.getInventory()){value=31*value+ItemStack.hashItemAndComponents(slot.getItem());value=31*value+slot.getItem().getCount();}
            return 31*value+ItemStack.hashItemAndComponents(bot.containerMenu.getCarried());}
        @Override boolean step(){base.client.acknowledge();base.audience.acknowledge();int now=game.server().getTickCount();
            require(now-started<500,"native reachable table cannot stall: "+game.carpetBots().diagnostics(bot));require(bot.isAlive()&&bot.getHealth()==20,"ordinary native bot health");
            if(bot.containerMenu instanceof net.minecraft.world.inventory.CraftingMenu)sawMenu=true;
            int current=signature();if(current!=signature){
                // Opening/closing changes menu topology; native inventory changes still
                // have their own recorded owner budget in the shared regression suite.
                signature=current;lastChange=now;
            }
            if(bot.getInventory().countItem(Items.WOODEN_PICKAXE)==0)return false;
            var state=state();var crafting=state==null?null:(LbcBotCrafting)WoolBucketChecks.read(state,"crafting");
            if(crafting==null||crafting.active()||bot.containerMenu!=bot.inventoryMenu)return false;
            require(sawMenu&&bot.position().distanceToSqr(from)>.25,"native movement reaches an actual usable table face");
            int placed=game.carpetBots().observation(bot).placements();
            require(bot.getInventory().countItem(Items.WOODEN_PICKAXE)==1&&bot.getInventory().countItem(Items.STICK)==2
                    &&bot.getInventory().countItem(Items.OAK_PLANKS)+placed==3,"real recipe debits exactly5 planks, with any native building debited separately: pick="
                    +bot.getInventory().countItem(Items.WOODEN_PICKAXE)+" sticks="+bot.getInventory().countItem(Items.STICK)+" planks="+bot.getInventory().countItem(Items.OAK_PLANKS)+" placed="+placed);
            require(base.level.getBlockState(bench).is(Blocks.CRAFTING_TABLE),"actual world table retained");return true;
        }
    }
    static void run(GameController game){if(running!=null)throw new IllegalStateException("Exclusive table fixture");var next=new Fixture(game);
        try{next.setup();running=next;}catch(Throwable error){next.close();throw error;}}
    static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;
        SkyWars2.LOGGER.info("[check] PASS bot-workbench (occluded centre, physical native approach/use, real shaped menu crafting and finite materials)");
    }catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL bot-workbench",error);}}
}
