package me.skywars2.game;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.SguiUtils;
import eu.pb4.sgui.api.gui.SimpleGui;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;

/** Actual mode3 participant/NPC and native packet input. Root owns match/team/arena setup. */
public final class BedwarsShopChecks {
    private BedwarsShopChecks(){}
    private static Run running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Bedwars shop: "+why);}
    public static void start(GameController game,LbcBedwarsShop shop,Entity actualShop){
        require(running==null,"exclusive fixture");var run=new Run(game,shop,actualShop);running=run;
        try{run.begin();}catch(Throwable error){running=null;run.close();throw error;}
    }
    public static void tick(GameController game){
        if(running==null)return;var run=running;require(run.game==game,"same running controller");
        try{if(!run.finish())return;running=null;run.close();SkyWars2.LOGGER.info("[check] PASS bedwars-shop (actual mode{} source SGui catalog; all77 ordinary actions plus3 Lucky offers in mode4, native click/Shift exact source debit/full loot components and next-mode isolation; atomic currency/equipment/overflow, double enchant visits/int saturation/no-op debit, protected transfers/roles/stale menu, positive MAG/TECH and TimeStop)",game.gameMode());}
        catch(Throwable error){running=null;try{run.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL bedwars-shop",error);}
    }
    private static final class Run implements AutoCloseable {
        final GameController game;final LbcBedwarsShop shop;final Entity npc;final ServerPlayer player;
        final GameChecks.WandThirdPlayerState saved;final RegenerationBeltChecks.ScheduledClient client;
        final Set<UUID> originalDrops=new HashSet<>();final List<Marker> jammers=new ArrayList<>();boolean stopped,closed;int at;SimpleGui stoppedMenu;
        Run(GameController game,LbcBedwarsShop shop,Entity npc){
            this.game=game;this.shop=shop;this.npc=npc;player=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            require(game.playing()&&game.bedwars().mode()&&player!=null&&game.canUseGameplayItems(player),"actual active BedWars3/4 TestAlpha");
            require(npc!=null&&npc.entityTags().contains("bedwars_shop")&&!npc.isRemoved(),"actual indexed shop NPC");
            require(!game.items().timeStop().stopped(player.level()),"exclusive TimeStop fixture");
            saved=new GameChecks.WandThirdPlayerState(player,game.items().mana());client=new RegenerationBeltChecks.ScheduledClient(game,player);
            drops().forEach(e->originalDrops.add(e.getUUID()));
        }
        List<ItemEntity> drops(){return player.level().getEntitiesOfClass(ItemEntity.class,player.getBoundingBox().inflate(5),e->!e.isRemoved());}
        long count(Item item){long total=0;for(int n=0;n<player.getInventory().getContainerSize();n++){var stack=player.getInventory().getItem(n);if(stack.is(item))total+=stack.getCount();}return total;}
        void clear(){player.getInventory().clearContent();player.containerMenu.setCarried(ItemStack.EMPTY);}
        void coins(Item item,int count){player.getInventory().add(new ItemStack(item,count));}
        SimpleGui open(){player.closeContainer();client.acknowledge();require(shop.open(player,npc),"registered actual shop opens");require(SguiUtils.getCurrentGui(player) instanceof SimpleGui,"real SGui");return (SimpleGui)SguiUtils.getCurrentGui(player);}
        void packet(int slot,byte button,ContainerInput action){client.acknowledge();var menu=player.containerMenu;player.connection.handleContainerClick(new ServerboundContainerClickPacket(menu.containerId,menu.getStateId(),(short)slot,button,action,it.unimi.dsi.fastutil.ints.Int2ObjectMaps.emptyMap(),HashedStack.EMPTY));}
        void click(int slot){packet(slot,(byte)0,ContainerInput.PICKUP);}
        void category(int top){click(top);}
        int power(ItemStack stack){return stack.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY).getLevel(player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.POWER));}
        void luckyOffers(){
            boolean lucky=game.gameMode()==4;int next=game.config().gameMode;
            try{
                game.config().gameMode=lucky?3:4;
                require((open().getGuiElement(6)!=null)==lucky,"future configuration cannot change the active shop family");
                var offers=shop.offers(player,LbcBedwarsShop.Category.LUCKY);
                require(offers.size()==(lucky?3:0),"three source Lucky offers only in active mode4");
                if(!lucky)return;
                var currencies=new Item[]{Items.COPPER_INGOT,Items.IRON_INGOT,Items.GOLD_INGOT,Items.DIAMOND,Items.NETHER_STAR};
                var kinds=LbcLuckyBlocks.Kind.values();
                int[][] prices={{16,4,0,0,0},{0,4,8,0,0},{0,0,4,8,0}};
                for(int n=0;n<offers.size();n++){
                    clear();category(6);var offer=offers.get(n);var block=LbcLuckyBlocks.item(kinds[n]);
                    require(offer.item().is(block),"source loot table resolves native block kind "+n);
                    click(offer.slot());require(count(block)==0,"unaffordable source block produces nothing");
                    for(var currency:currencies)coins(currency,64);
                    click(offer.slot());require(count(block)==1,"actual native packet purchases one source block");
                    packet(offer.slot(),(byte)0,ContainerInput.QUICK_MOVE);
                    require(count(block)==2,"source Shift purchase remains one block/one payment");
                    for(int c=0;c<currencies.length;c++)require(offer.price().at(c)==prices[n][c]
                            &&count(currencies[c])==64-2*prices[n][c],"exact source mixed price "+n+"/"+c);
                    for(int slot=0;slot<player.getInventory().getContainerSize();slot++){
                        var stack=player.getInventory().getItem(slot);
                        if(stack.is(block))require(ItemStack.isSameItemSameComponents(stack,offer.item()),
                                "full authored reward components retained without preview price lore");
                    }
                }
            }finally{game.config().gameMode=next;clear();open();}
        }
        void previewTransfers(){
            clear();open();player.getInventory().setItem(0,new ItemStack(Items.STONE,3));player.containerMenu.setCarried(new ItemStack(Items.STICK,2));
            var expected=new ArrayList<ItemStack>();for(int n=0;n<player.getInventory().getContainerSize();n++)expected.add(player.getInventory().getItem(n).copy());var cursor=player.containerMenu.getCarried().copy();var before=new HashSet<UUID>();drops().forEach(e->before.add(e.getUUID()));
            for(int slot:new int[]{0,8,9,18,38,54})for(var input:List.of(ContainerInput.SWAP,ContainerInput.THROW,ContainerInput.CLONE,ContainerInput.PICKUP_ALL,ContainerInput.QUICK_CRAFT,ContainerInput.QUICK_MOVE)){
                if(slot>=54&&(input==ContainerInput.SWAP||input==ContainerInput.THROW||input==ContainerInput.CLONE))continue;
                for(byte button:new byte[]{0,1,2,8,40}){
                    packet(slot,button,input);
                    for(int n=0;n<expected.size();n++)require(ItemStack.matches(expected.get(n),player.getInventory().getItem(n)),"native forged "+input+"/"+slot+"/"+button+" cannot move GUI preview into inventory slot "+n);
                    require(ItemStack.matches(cursor,player.containerMenu.getCarried()),"native forged transfer cannot replace the cursor");
                    require(drops().stream().allMatch(e->before.contains(e.getUUID())),"native forged transfer cannot throw a usable GUI icon");
                }
            }
            clear();open();
        }
        void begin(){
            require(player.position().distanceToSqr(npc.position())<=64,"root places enrolled player within shop reach");
            boolean lucky=game.gameMode()==4;
            int[] expected={16,15,7,5,8,14,5,7,lucky?3:0};int total=0;
            for(int n=0;n<expected.length;n++){var offers=shop.offers(player,LbcBedwarsShop.Category.values()[n]);require(offers.size()==expected[n],"source finite category count "+n);total+=offers.size();var slots=new HashSet<Integer>();for(var offer:offers)require(offer.slot()>=18&&offer.slot()<54&&slots.add(offer.slot())&&!offer.item().isEmpty(),"unique stocked source slot");}
            require(total==(lucky?80:77),"complete source finite graph");clear();var menu=open();require((menu.getGuiElement(6)!=null)==lucky,"Lucky tab appears only in the active Lucky BedWars match");
            require(menu.getGuiElement(9).getItemStack().is(Items.PALE_HANGING_MOSS)&&menu.getGuiElement(9).getItemStack().get(DataComponents.TOOLTIP_DISPLAY).hideTooltip(),"actual source separator differs from stale VINE comment");
            luckyOffers();previewTransfers();
            // Mixed currency rejection must leave BOTH currencies, output and cursor untouched.
            coins(Items.IRON_INGOT,5);category(1);click(23);require(count(Items.IRON_INGOT)==5&&count(Items.IRON_SWORD)==0&&player.containerMenu.getCarried().isEmpty(),"missing gold cannot partially debit iron");
            coins(Items.GOLD_INGOT,3);click(23);require(count(Items.IRON_INGOT)==0&&count(Items.GOLD_INGOT)==0&&count(Items.IRON_SWORD)==1,"exact real multicurrency purchase");
            clear();player.getInventory().setItem(9,new ItemStack(Items.COPPER_INGOT,2));player.getInventory().setItem(10,new ItemStack(Items.COPPER_INGOT,2));var custom=new ItemStack(Items.COPPER_INGOT);custom.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("physical copper"));player.getInventory().setItem(40,custom);click(18);require(count(Items.COPPER_INGOT)==0&&count(Items.WOODEN_AXE)==1,"physical material currency across storage/offhand/custom components");
            clear();coins(Items.COPPER_INGOT,64);category(4);packet(19,(byte)0,ContainerInput.QUICK_MOVE);require(count(Items.COPPER_INGOT)==42&&countWool()==64,"source ceil22repeat costs22 for capped64 wool");
            clear();coins(Items.IRON_INGOT,64);packet(23,(byte)0,ContainerInput.QUICK_MOVE);require(count(Items.IRON_INGOT)==57&&count(Items.GLASS)==64,"source ceil7repeat costs7 for capped64 glass");
            clear();coins(Items.COPPER_INGOT,2);category(0);click(19);require(player.getInventory().getItem(38).is(Items.LEATHER_CHESTPLATE)&&count(Items.LEATHER_CHESTPLATE)==1&&count(Items.COPPER_INGOT)==0,"source gives2 leather chest clamps to1 and auto-equips empty slot");
            // Native accepted overflow at full inventory; currency in offhand avoids creating a free storage slot.
            clear();for(int n=0;n<36;n++)player.getInventory().setItem(n,new ItemStack(Items.STONE,64));player.getInventory().setItem(40,new ItemStack(Items.IRON_INGOT,2));category(5);var before=new HashSet<UUID>();drops().forEach(e->before.add(e.getUUID()));click(18);var output=drops().stream().filter(e->!before.contains(e.getUUID())&&e.getItem().is(Items.CHEST)).toList();require(output.size()==1&&output.getFirst().getItem().getCount()==1&&count(Items.IRON_INGOT)==0,"accepted physical overflow body and exactly one debit");output.forEach(Entity::discard);
            clear();coins(Items.NETHER_STAR,5);category(7);click(30);require(count(Items.NETHER_STAR)==0,"source pay-on-no-op weapon upgrade");
            clear();coins(Items.NETHER_STAR,15);player.getInventory().setItem(9,new ItemStack(Items.BOW));player.getInventory().setItem(40,new ItemStack(Items.BOW));click(32);require(power(player.getInventory().getItem(9))==2&&power(player.getInventory().getItem(40))==4,"confirmed Bukkit contents plus explicit offhand second visit");
            var holder=player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.POWER);var wide=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);wide.set(holder,Integer.MAX_VALUE);player.getInventory().getItem(9).set(DataComponents.ENCHANTMENTS,wide.toImmutable());coins(Items.NETHER_STAR,15);click(32);require(power(player.getInventory().getItem(9))==Integer.MAX_VALUE,"existing positive-int enchantment policy saturates, never wraps");
            clear();coins(Items.GOLD_INGOT,3);category(1);packet(38,(byte)0,ContainerInput.SWAP);packet(38,(byte)0,ContainerInput.PICKUP_ALL);packet(38,(byte)0,ContainerInput.QUICK_CRAFT);require(count(Items.GOLD_INGOT)==3&&count(Items.BOW)==0&&player.containerMenu.getCarried().isEmpty(),"forged swap/collect/drag cannot take GUI stock or debit");
            require(!menu.onAnyClick(54,ClickType.MOUSE_LEFT_SHIFT,ContainerInput.QUICK_MOVE),"lower shift intercepted");require(!menu.onAnyClick(54,ClickType.MOUSE_DOUBLE_CLICK,ContainerInput.PICKUP_ALL),"lower double collection intercepted");
            // Canonical shop identity survives public tag mutation; actual reach controls admission.
            var returnAt=player.position();float returnYaw=player.getYRot(),returnPitch=player.getXRot();
            var held=(SimpleGui)SguiUtils.getCurrentGui(player);
            try{
                require(player.teleportTo(player.level(),npc.getX()+16,npc.getY(),npc.getZ(),Set.of(),returnYaw,returnPitch,true),"native out-of-reach teleport accepted");client.acknowledge();
                require(player.distanceToSqr(npc)>64&&LbcBedwars.shopRole(npc),"real lost reach with canonical shop role retained");
                held.click(38,ClickType.MOUSE_LEFT,ContainerInput.PICKUP);
                require(count(Items.GOLD_INGOT)==3&&count(Items.BOW)==0&&!shop.open(player,npc),"out-of-reach held menu cannot debit and shop cannot reopen");
            }finally{
                require(player.teleportTo(player.level(),returnAt.x,returnAt.y,returnAt.z,Set.of(),returnYaw,returnPitch,true),"native within-reach restoration accepted");client.acknowledge();
            }
            player.setGameMode(GameType.SPECTATOR);try{require(!shop.open(player,npc),"observer cannot open physical shop");}finally{player.setGameMode(GameType.SURVIVAL);}
            for(String tag:List.of("stopper_magic","stopper")){
                var marker=new Marker(EntityTypes.MARKER,player.level());marker.setPos(player.position());marker.entityTags().add(tag);require(player.level().addFreshEntity(marker),"actual jammer marker");jammers.add(marker);game.items().magic().loaded(marker);
                try{require(tag.equals("stopper_magic")?game.items().magic().suppressed(player):game.items().magic().mechanismSuppressed(player),"jammer positive control");clear();coins(Items.COPPER_INGOT,5);open();category(1);click(18);require(count(Items.WOODEN_AXE)==1&&count(Items.COPPER_INGOT)==0,"physical trade ignores active jammer "+tag);}finally{game.items().magic().unloaded(marker);marker.discard();jammers.remove(marker);}
            }
            clear();coins(Items.GOLD_INGOT,3);stoppedMenu=open();category(1);game.items().timeStop().begin(player.level(),100);stopped=true;at=game.server().getTickCount();
        }
        long countWool(){long result=0;for(var stack:player.getInventory().getNonEquipmentItems())if(stack.is(net.minecraft.tags.ItemTags.WOOL))result+=stack.getCount();return result;}
        boolean finish(){
            if(game.server().getTickCount()-at<2)return false;
            require(game.items().timeStop().stopped(player.level())&&game.items().timeStop().frozen(player),"actual shared TimeStop retained across server ticks");
            // Deliver a native menu input against the still-held GUI; policy must reject before a debit.
            stoppedMenu.click(38,ClickType.MOUSE_LEFT,ContainerInput.PICKUP);require(count(Items.GOLD_INGOT)==3&&count(Items.BOW)==0,"frozen trade policy no debit");
            game.items().timeStop().end(player.level());stopped=false;var stale=open();shop.stop();stale.click(38,ClickType.MOUSE_LEFT,ContainerInput.PICKUP);require(count(Items.GOLD_INGOT)==3&&count(Items.BOW)==0,"closed/stale native menu cannot transact");return true;
        }
        @Override public void close(){if(closed)return;closed=true;if(stopped)game.items().timeStop().end(player.level());for(var marker:jammers){game.items().magic().unloaded(marker);marker.discard();}player.closeContainer();for(var entity:drops())if(!originalDrops.contains(entity.getUUID()))entity.discard();client.close();saved.restore(game);}
    }
}
