package me.skywars2.game;

import java.util.*;
import me.skywars2.game.items.ItemData;
import me.skywars2.game.items.wands.LbcTinyWandInteractions;
import me.skywars2.game.items.wands.LbcWands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Real world publication/merging, native TNT and both passive input-grid sizes. */
public final class TinyWandInteractionChecks {
    private TinyWandInteractionChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Tiny interactions: "+why);}
    public static void run(GameController game){
        try(var f=new Fixture(game)){
            f.pairs();f.air();f.terra();f.specials();f.grids();
        }
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture scope;
        final LbcTinyWandInteractions service;final Vec3 at;final AABB area;
        final Set<UUID> baseline=new HashSet<>();
        Fixture(GameController game){
            this.game=game;scope=new WoolBucketChecks.Fixture(game,false);service=game.items().tinyWands();
            scope.prepareParticipants();at=scope.home.add(0,24,0);area=new AABB(at,at).inflate(10);
            scope.move(scope.owner,at.add(0,-8,0));scope.move(scope.other,at.add(12,-8,0));
            scope.level.getAllEntities().forEach(e->baseline.add(e.getUUID()));
        }
        List<ItemEntity> items(){return scope.level.getEntitiesOfClass(ItemEntity.class,area,e->e.isAlive()&&!baseline.contains(e.getUUID()));}
        void reset(){
            clearGrid();for(var e:scope.level.getEntitiesOfClass(Entity.class,area,e->!baseline.contains(e.getUUID())))scope.discard(e);
        }
        ItemEntity drop(ItemStack stack,double x,boolean thrown){
            var item=new ItemEntity(scope.level,at.x+x,at.y,at.z,stack);
            item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);item.setPickUpDelay(32767);
            if(thrown)item.setThrower(scope.owner);
            game.items().markCurrentItemEpoch(item);scope.own(item);require(scope.level.addFreshEntity(item),"native item published");return item;
        }
        void ground(){for(int i=0;i<20;i++)service.tick();for(var item:items())scope.own(item);}
        int count(Item item){return items().stream().filter(e->e.getItem().is(item)).mapToInt(e->e.getItem().getCount()).sum();}
        ItemStack flagged(String flag,int count){var s=new ItemStack(Items.PAPER,count);var data=new CompoundTag();data.putInt(flag,1);s.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return s;}
        void pairs(){
            Item[][] pairs={{LbcWands.TINY_RUBY,LbcWands.RUBY_WAND},{LbcWands.TINY_TERRA,LbcWands.TERRA},
                    {LbcWands.TINY_ELECTRO,LbcWands.ELECTRO},{LbcWands.TINY_AIR,LbcWands.AIR_WAND},
                    {LbcWands.TINY_ACID,LbcWands.ACID_WAND},{LbcWands.TINY_TELEPORT,LbcWands.TELEPORT_WAND},
                    {LbcWands.TINY_STONE,LbcWands.STONE_WAND},{LbcWands.TINY_FIRE,LbcWands.FIRE_WAND},
                    {LbcWands.TINY_BLIND,LbcWands.BLIND_WAND}};
            for(var pair:pairs){reset();drop(new ItemStack(pair[0],3),0,true);ground();require(count(pair[1])==1&&count(pair[0])==1,"merged catalyst stack consumes exactly two: "+pair[0]);}
        }
        void air(){
            reset();var original=new ItemStack(Items.DIAMOND_SWORD,3);original.setDamageValue(11);
            original.set(DataComponents.CUSTOM_NAME,Component.literal("Keep all components"));
            drop(new ItemStack(LbcWands.TINY_AIR),0,true);drop(original.copy(),.4,true);ground();
            require(count(LbcWands.TINY_AIR)==0,"air catalyst consumed");
            var copies=items().stream().filter(e->e.entityTags().contains(LbcTinyWandInteractions.AIR_COPY)).toList();
            require(copies.stream().mapToInt(e->e.getItem().getCount()).sum()==120,"forty full three-item stacks, not forty individual swords");
            require(copies.stream().allMatch(e->ItemStack.isSameItemSameComponents(e.getItem(),original)),"damage/name/components retained");
            var blasts=scope.level.getEntitiesOfClass(PrimedTnt.class,area,e->!baseline.contains(e.getUUID()));
            require(blasts.size()==1&&blasts.getFirst().getFuse()==0,"one immediate native TNT");scope.own(blasts.getFirst());
            scope.level.tickNonPassenger(blasts.getFirst());
            require(items().stream().filter(e->e.entityTags().contains(LbcTinyWandInteractions.AIR_COPY)).mapToInt(e->e.getItem().getCount()).sum()==120,"all copied stacks survive actual accompanying explosion");
            require(copies.size()<40,"copies use native merged ground entities");
        }
        void terra(){
            reset();drop(new ItemStack(LbcWands.TINY_TERRA),0,false);
            var one=new ItemStack(Items.DIAMOND_SWORD,7);one.set(DataComponents.CUSTOM_NAME,Component.literal("Remove me"));one.setDamageValue(8);
            drop(one,.3,true);drop(new ItemStack(Items.GOLD_INGOT,11),-.3,false);ground();
            require(count(Items.SPRUCE_LEAVES)==18&&count(LbcWands.TINY_TERRA)==1,"terra converts every nearby donor, retains catalyst and counts");
            require(items().stream().filter(e->e.getItem().is(Items.SPRUCE_LEAVES)).allMatch(e->ItemStack.isSameItemSameComponents(e.getItem(),new ItemStack(Items.SPRUCE_LEAVES))),"terra clears all donor components");
        }
        void specials(){
            reset();drop(new ItemStack(LbcWands.TINY_RUBY),0,true);drop(flagged("libspell",2),.3,true);ground();
            require(count(LbcWands.TINY_RUBY)==1&&items().stream().filter(e->ItemData.intValue(e.getItem(),"libspell",0)==1).mapToInt(e->e.getItem().getCount()).sum()==1,"ruby consumes one spell donor and retains catalyst");require(items().size()>=3,"ruby actually publishes random spell loot");
            reset();drop(new ItemStack(LbcWands.TINY_BLIND),0,true);drop(new ItemStack(Items.GLASS_BOTTLE,2),.3,true);ground();
            require(count(Items.GLASS_BOTTLE)==1&&count(LbcWands.TINY_BLIND)==1&&items().size()>=3,"blind publishes potion, consumes one bottle; drops="+items().stream().map(e->e.getItem().toString()+" @"+e.position()).toList()+" stage="+game.stage());
            for(var donor:List.of(Items.IRON_SWORD,Items.SHIELD)){
                reset();drop(new ItemStack(LbcWands.TINY_ELECTRO),0,true);drop(new ItemStack(donor,2),.3,true);ground();
                require(count(donor)==1&&items().stream().anyMatch(e->!e.getItem().is(donor)&&!e.getItem().is(LbcWands.TINY_ELECTRO)),"electro publishes actual sword/shield output");
            }
            reset();drop(new ItemStack(LbcWands.TINY_STONE),0,true);var tool=drop(new ItemStack(Items.DIAMOND_PICKAXE,64),.3,true);ground();
            require(tool.getItem().getCount()==64&&tool.getItem().has(DataComponents.UNBREAKABLE)&&count(LbcWands.TINY_STONE)==1,"stone keeps whole overstack and catalyst");
            tool.getItem().remove(DataComponents.UNBREAKABLE);service.stop();
            game.server().tickRateManager().setFrozen(true);
            try{var bodies=new ArrayList<Entity>();scope.level.getAllEntities().forEach(bodies::add);service.reindexRestored(bodies);}
            finally{game.server().tickRateManager().setFrozen(false);}
            ground();require(tool.getItem().has(DataComponents.UNBREAKABLE),"native ground catalysts recover after frozen replay without ENTITY_LOAD");
            reset();drop(new ItemStack(LbcWands.TINY_ACID),0,true);drop(new ItemStack(Items.DIRT,3),.3,true);ground();
            require(count(Items.DIRT)==2&&count(LbcWands.TINY_ACID)==1,"acid consumes exactly one donor, never catalyst");
            reset();drop(new ItemStack(LbcWands.TINY_TELEPORT),0,true);var donor=flagged("sword",3);var data=donor.get(DataComponents.CUSTOM_DATA).copyTag();data.putInt("transformable",1);donor.set(DataComponents.CUSTOM_DATA,CustomData.of(data));drop(donor,.3,true);ground();
            require(count(Items.PAPER)==2&&items().stream().anyMatch(e->!e.getItem().is(Items.PAPER)&&!e.getItem().is(LbcWands.TINY_TELEPORT)),"teleport consumes one and publishes reforge output");
            reset();drop(new ItemStack(LbcWands.TINY_FIRE),0,true);drop(new ItemStack(Items.DIRT,3),.3,true);ground();
            require(count(Items.DIRT)==2&&count(LbcWands.TINY_FIRE)==1&&scope.owner.entityTags().contains("have_lucky_effects"),"fire sacrifices one donor and rewards nearest player");
            for(int i=1;i<=64;i++)service.fireReward(scope.owner,i);
        }
        void clearGrid(){
            if(scope.owner.containerMenu instanceof AbstractCraftingMenu menu)for(var slot:menu.getInputGridSlots())slot.setByPlayer(ItemStack.EMPTY);
            scope.owner.containerMenu.setCarried(ItemStack.EMPTY);
        }
        void grids(){
            for(int size:new int[]{2,3}){
                reset();scope.owner.containerMenu=size==2?scope.owner.inventoryMenu:new CraftingMenu(89,scope.owner.getInventory(),ContainerLevelAccess.NULL);
                var menu=(AbstractCraftingMenu)scope.owner.containerMenu;var slots=menu.getInputGridSlots();
                for(var tiny:List.of(LbcWands.TINY_STONE,LbcWands.TINY_RUBY,LbcWands.TINY_BLIND,LbcWands.TINY_ACID,LbcWands.TINY_FIRE)){
                    reset();slots.getFirst().setByPlayer(new ItemStack(tiny));
                    var donor=tiny==LbcWands.TINY_RUBY?flagged("libspell",3):new ItemStack(tiny==LbcWands.TINY_BLIND?Items.GLASS_BOTTLE:Items.DIAMOND_SWORD,3);
                    donor.set(DataComponents.CUSTOM_NAME,Component.literal("Preserved grid remainder"));
                    slots.getLast().setByPlayer(donor.copy());service.tick();
                    require(slots.getFirst().getItem().getCount()==1,"passive grid catalyst retained "+size+"/"+tiny);
                    var left=slots.getLast().getItem();require(left.getCount()==(tiny==LbcWands.TINY_STONE?3:2)&&left.get(DataComponents.CUSTOM_NAME).equals(donor.get(DataComponents.CUSTOM_NAME)),"passive grid donor count/components "+size+"/"+tiny);
                    if(tiny==LbcWands.TINY_STONE)require(left.has(DataComponents.UNBREAKABLE)&&slots.getFirst().getItem().has(DataComponents.UNBREAKABLE),"stone changes all occupied input slots");
                    if(tiny==LbcWands.TINY_RUBY||tiny==LbcWands.TINY_BLIND)require(!items().isEmpty(),"grid rewards are actually spawned without clicking result");
                }
                clearGrid();scope.owner.containerMenu=scope.owner.inventoryMenu;
            }
        }
        @Override public void close(){reset();scope.owner.containerMenu=scope.owner.inventoryMenu;scope.close();}
    }
}
