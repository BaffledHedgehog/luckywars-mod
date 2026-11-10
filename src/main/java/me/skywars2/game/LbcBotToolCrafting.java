package me.skywars2.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Native recipe ingredients and menu clicks; tools are made from owned materials, never granted. */
final class LbcBotToolCrafting {
    private record Click(int slot,int button){}
    private final ServerPlayer player;
    private final LbcChefBrain.Driver driver;
    private final LbcBotSurvival survival;
    private List<Click> clicks=List.of();
    private int cursor;
    private Item expected;
    private BlockPos bench;
    private Vec3 approach,resume;
    private boolean preparing,ready;
    LbcBotToolCrafting(ServerPlayer player,LbcChefBrain.Driver driver,LbcBotSurvival survival){this.player=player;this.driver=driver;this.survival=survival;}
    boolean active(){return preparing||!clicks.isEmpty();}
    boolean prepared(){return ready;}
    String diagnostics(){return "toolPreparation="+preparing+" toolReady="+ready+" toolClick="+cursor+"/"+clicks.size()+" workbench="+bench;}
    private void finishApproach(){approach=null;if(resume!=null){survival.destination.accept(resume);resume=null;survival.brain.reset();}}
    void cancel(){clicks=List.of();cursor=0;preparing=false;ready=false;finishApproach();}
    /** Movement/aiming runs every frame; only actual inventory clicks use the five-tick gate. */
    boolean navigate(){return preparing&&clicks.isEmpty()&&bench!=null&&player.containerMenu==player.inventoryMenu&&openBench();}
    private int count(Item item){return player.getInventory().countItem(item);}
    private ItemStack find(Item item){for(int i=0;i<36;i++)if(player.getInventory().getItem(i).is(item))return player.getInventory().getItem(i);return ItemStack.EMPTY;}
    private ShapedRecipe recipe(String id){
        var key=ResourceKey.create(Registries.RECIPE,Identifier.withDefaultNamespace(id));
        var holder=player.level().recipeAccess().byKey(key).orElse(null);
        return holder!=null&&holder.value() instanceof ShapedRecipe shaped?shaped:null;
    }
    /** One inventory unit per pattern cell, preserving each real stack's components. */
    private int[] ingredients(ShapedRecipe recipe){
        if(recipe==null)return null;
        var wanted=recipe.getIngredients();var result=new int[wanted.size()];Arrays.fill(result,-1);
        int[] remaining=new int[36];for(int i=0;i<36;i++)remaining[i]=player.getInventory().getItem(i).getCount();
        for(int cell=0;cell<wanted.size();cell++)if(wanted.get(cell).isPresent()){
            for(int slot=0;slot<36;slot++)if(remaining[slot]>0){
                var stack=player.getInventory().getItem(slot);
                if(!LbcBotCrafting.special(stack)&&wanted.get(cell).get().test(stack)){result[cell]=slot;remaining[slot]--;break;}
            }
            if(result[cell]<0)return null;
        }
        return result;
    }
    private int inventorySlot(int index){
        for(int i=0;i<player.containerMenu.slots.size();i++){
            var slot=player.containerMenu.slots.get(i);
            if(slot.container==player.getInventory()&&slot.getContainerSlot()==index)return i;
        }
        return -1;
    }
    private boolean plan(ShapedRecipe recipe,int width){
        var inputs=ingredients(recipe);int free=player.getInventory().getFreeSlot();if(inputs==null||free<0)return false;
        var material=new LinkedHashMap<Integer,List<Integer>>();
        var stacks=new ArrayList<ItemStack>(Collections.nCopies(width*width,ItemStack.EMPTY));
        for(int cell=0;cell<inputs.length;cell++)if(inputs[cell]>=0){
            int grid=(cell/recipe.getWidth())*width+cell%recipe.getWidth();
            material.computeIfAbsent(inputs[cell],ignored->new ArrayList<>()).add(1+grid);
            stacks.set(grid,player.getInventory().getItem(inputs[cell]).copyWithCount(1));
        }
        var output=recipe.assemble(CraftingInput.of(width,width,stacks));if(output.isEmpty())return false;
        var steps=new ArrayList<Click>();
        for(var entry:material.entrySet()){
            int slot=inventorySlot(entry.getKey());if(slot<0)return false;
            steps.add(new Click(slot,0));for(int grid:entry.getValue())steps.add(new Click(grid,1));
            steps.add(new Click(slot,0));
        }
        steps.add(new Click(0,0));steps.add(new Click(inventorySlot(free),0));
        expected=output.getItem();clicks=List.copyOf(steps);cursor=0;return true;
    }
    private BlockPos nearbyBench(){
        var level=player.level();BlockPos best=null;double nearest=8*8;
        // Discovery may exceed click range; openBench still requires a physical route and native ray.
        for(var p:BlockPos.betweenClosed(player.blockPosition().offset(-8,-2,-8),player.blockPosition().offset(8,2,8)))
            if(level.hasChunkAt(p)&&level.getBlockState(p).is(Blocks.CRAFTING_TABLE)){
                double distance=player.getEyePosition().distanceToSqr(Vec3.atCenterOf(p));
                if(distance<nearest){nearest=distance;best=p.immutable();}
            }
        return best;
    }
    private boolean openBench(){
        driver.crouch(false);
        if(bench==null||!player.level().getBlockState(bench).is(Blocks.CRAFTING_TABLE))bench=nearbyBench();
        if(bench==null){
            driver.halt();
            var item=find(Items.CRAFTING_TABLE);if(item.isEmpty())return false;
            for(var side:Direction.Plane.HORIZONTAL){
                var at=player.blockPosition().relative(side,2);
                if(!player.level().hasChunkAt(at)||!player.level().getBlockState(at).canBeReplaced()
                        ||player.level().getBlockState(at.below()).getCollisionShape(player.level(),at.below()).isEmpty())continue;
                if(survival.place(at,item)){bench=at.immutable();return true;}
            }
            return true;
        }
        var aim=LbcBotActions.visibleBlockAim(player,bench,player.position(),player.blockInteractionRange());
        if(aim==null){
            if(approach==null||!survival.brain.supported(approach)
                    ||LbcBotActions.visibleBlockAim(player,bench,approach,player.blockInteractionRange())==null){
                approach=null;double nearest=Double.POSITIVE_INFINITY;
                for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){
                    var at=survival.brain.surfaceAt(Vec3.atBottomCenterOf(bench.offset(x,0,z)));if(at==null)continue;
                    double distance=player.distanceToSqr(at);if(distance>=nearest)continue;
                    if(LbcBotActions.visibleBlockAim(player,bench,at,player.blockInteractionRange())!=null){nearest=distance;approach=at;}
                }
                survival.brain.reset();
            }
            if(approach!=null){
                if(resume==null)resume=driver.destination();survival.destination.accept(approach);
                if(!survival.brain.closeGround(approach))survival.brain.tick();
            }else driver.halt();
            return true;
        }
        driver.halt();if(!driver.aimAction(aim))return true;
        var hit=LbcBotActions.ray(player,player.blockInteractionRange());
        if(hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK&&hit.getBlockPos().equals(bench))
            player.gameMode.useItemOn(player,player.level(),player.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
        if(player.containerMenu instanceof CraftingMenu)finishApproach();
        return true;
    }
    /** Caller owns the five-tick input budget shared with all other crafting. */
    boolean tick(){
        if(!clicks.isEmpty()){
            driver.halt();
            if(cursor>=clicks.size()){
                if(!player.containerMenu.getCarried().isEmpty()){player.closeContainer();cancel();return true;}
                clicks=List.of();cursor=0;preparing=false;
                if(player.containerMenu instanceof CraftingMenu)player.closeContainer();
                return true;
            }
            var click=clicks.get(cursor);
            if(click.slot()==0&&(!player.containerMenu.getSlot(0).hasItem()||!player.containerMenu.getSlot(0).getItem().is(expected))){
                player.closeContainer();cancel();return true;
            }
            player.containerMenu.clicked(click.slot(),click.button(),ContainerInput.PICKUP,player);cursor++;
            player.containerMenu.broadcastChanges();return true;
        }
        // A found or previously crafted pickaxe needs no station or new inventory work.
        boolean stone=count(Items.COBBLESTONE)>=3;
        for(int i=0;i<36;i++)if(player.getInventory().getItem(i).getDestroySpeed(Blocks.IRON_BARS.defaultBlockState())>=(stone?4:2)){
            ready=true;preparing=false;return false;
        }
        if(player.containerMenu!=player.inventoryMenu&&!(preparing&&player.containerMenu instanceof CraftingMenu))return false;
        if(player.isUsingItem()||!player.containerMenu.getCarried().isEmpty()||player.getInventory().getFreeSlot()<0)return false;
        String desired=stone?"stone_pickaxe":"wooden_pickaxe";
        var target=recipe(desired);
        if(count(Items.STICK)<2){
            var sticks=recipe("stick");
            if(ingredients(sticks)!=null){preparing=true;return plan(sticks,player.containerMenu instanceof CraftingMenu?3:2);}
        }
        if(ingredients(target)==null){ready=true;preparing=false;return false;}
        preparing=true;ready=false;
        if(player.containerMenu instanceof CraftingMenu)return plan(target,3);
        if(bench==null)bench=nearbyBench();
        if(bench==null&&count(Items.CRAFTING_TABLE)==0){
            var table=recipe("crafting_table");
            if(ingredients(table)==null){ready=true;preparing=false;return false;}
            return plan(table,2);
        }
        return openBench();
    }
}
