package me.skywars2.game;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Finite registered Paper mode3 shop; physical native inventory transactions, no loot/function runner. */
public final class LbcBedwarsShop {
    public enum Beacon { SPEED2,JUMP2,FIRE_RES,STRENGTH2,HASTE2,REGEN2,RESISTANCE1 }
    public interface Policy {
        String epoch();
        boolean canTrade(ServerPlayer player,Entity shop);
        int team(ServerPlayer player);
        ItemStack teamChest(ServerPlayer player);
        ItemStack teamZombie(ServerPlayer player);
        default boolean lucky() { return false; }
        default ItemStack luckyBlock(ServerPlayer player,LbcLuckyBlocks.Kind kind) { return ItemStack.EMPTY; }
        void beacon(ServerPlayer player,Beacon buff,int seconds);
    }
    enum Category { ARMOR,WEAPONS,TOOLS,FOOD,BLOCKS,SPECIAL,ENCHANTS,BEACONS,LUCKY }
    private enum Kind { ITEM,TEAM_CHEST,ARMOR,WEAPONS,TOOLS,BOWS,KNOCKBACK,BEACON,LUCKY_BLOCK }
    record Price(int copper,int iron,int gold,int diamond,int star) {
        int at(int i){return switch(i){case 0->copper;case 1->iron;case 2->gold;case 3->diamond;default->star;};}
    }
    record Offer(int slot,ItemStack item,Price price,int gives,Kind kind,Beacon buff,int seconds) { }
    private static final Item[] CURRENCY={Items.COPPER_INGOT,Items.IRON_INGOT,Items.GOLD_INGOT,Items.DIAMOND,Items.NETHER_STAR};
    private static final String[] CURRENCY_KEYS={"item.minecraft.copper_ingot","item.minecraft.iron_ingot","item.minecraft.gold_ingot","item.minecraft.diamond","item.minecraft.nether_star"};
    private static final String[] BARE_CURRENCY_KEYS={"copper_ingot","iron_ingot","gold_ingot","diamond","nether_star"};
    private static final ChatFormatting[] COLORS={ChatFormatting.GOLD,ChatFormatting.GRAY,ChatFormatting.GOLD,ChatFormatting.AQUA,ChatFormatting.RED};
    private static final DyeColor[] TEAM_COLORS={DyeColor.LIGHT_BLUE,DyeColor.BLACK,DyeColor.BLUE,DyeColor.CYAN,DyeColor.PURPLE,DyeColor.GRAY,DyeColor.GREEN,DyeColor.MAGENTA,DyeColor.BROWN,DyeColor.ORANGE,DyeColor.LIGHT_GRAY,DyeColor.LIME,DyeColor.PINK,DyeColor.RED,DyeColor.WHITE,DyeColor.YELLOW};
    private final Policy policy;
    private final Set<Menu> menus=Collections.newSetFromMap(new IdentityHashMap<>());
    public LbcBedwarsShop(Policy policy){this.policy=Objects.requireNonNull(policy);}
    public boolean open(ServerPlayer player,Entity shop){
        if(!eligible(player,shop)||policy.team(player)<1||policy.team(player)>16)return false;
        var menu=new Menu(player,shop);menus.add(menu);boolean opened=menu.open();if(!opened)menus.remove(menu);return opened;
    }
    public void stop(){for(var menu:List.copyOf(menus))menu.close();menus.clear();}
    private boolean eligible(ServerPlayer player,Entity shop){return !player.hasDisconnected()&&player.isAlive()&&!player.isSpectator()&&shop instanceof net.minecraft.world.entity.npc.villager.Villager&&shop.isAlive()&&!shop.isRemoved()
            &&shop.level()==player.level()&&LbcBedwars.shopRole(shop)&&policy.canTrade(player,shop);}
    private static Price p(int c,int i,int g,int d,int s){return new Price(c,i,g,d,s);}
    private static ItemStack named(ItemStack item,String key){if(key!=null)item.set(DataComponents.ITEM_NAME,Component.translatable(key));return item;}
    private static ItemStack item(Item item){return new ItemStack(item);}
    private static ItemStack enchant(ServerPlayer player,ItemStack item,ResourceKey<Enchantment> key,int level){
        Holder<Enchantment> holder=player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        var current=item.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY);var changed=new ItemEnchantments.Mutable(current);changed.set(holder,level);item.set(DataComponents.ENCHANTMENTS,changed.toImmutable());return item;
    }
    private static void stock(List<Offer> out,int slot,ItemStack stack,String key,Price price,int gives){out.add(new Offer(slot,named(stack,key),price,gives,Kind.ITEM,null,0));}
    private static ItemStack prot(ServerPlayer p,Item item,int level){return enchant(p,item(item),Enchantments.PROTECTION,level);}
    List<Offer> offers(ServerPlayer player,Category category){
        var out=new ArrayList<Offer>();var dye=TEAM_COLORS[policy.team(player)-1];
        switch(category){
            case LUCKY -> {
                if(policy.lucky()){
                    var kinds=LbcLuckyBlocks.Kind.values();
                    Price[] prices={p(16,4,0,0,0),p(0,4,8,0,0),p(0,0,4,8,0)};
                    int[] slots={29,31,33};
                    for(int n=0;n<kinds.length;n++){
                        var block=policy.luckyBlock(player,kinds[n]);
                        if(!block.isEmpty())out.add(new Offer(slots[n],block,prices[n],block.getCount(),Kind.LUCKY_BLOCK,null,0));
                    }
                }
            }
            case ARMOR -> {
                Item[] leather={Items.LEATHER_HELMET,Items.LEATHER_CHESTPLATE,Items.LEATHER_LEGGINGS,Items.LEATHER_BOOTS};
                String[] keys={"leather_helm_p1","leather_chest_p1","leather_legs_p1","leather_boots_p1"};
                for(int n=0;n<4;n++){var stack=prot(player,leather[n],1);stack.set(DataComponents.DYED_COLOR,new DyedItemColor(dye.getTextureDiffuseColor()));stock(out,18+n,stack,keys[n],p(n==1?2:1,0,0,0,0),n==1?2:1);}
                String[] chainKeys={"armor_chain_p1","armor_chain_p2","armor_chain_p3"};
                for(int n=1;n<=3;n++)stock(out,22+n,prot(player,Items.CHAINMAIL_CHESTPLATE,n),chainKeys[n-1],p(0,n==1?1:n==2?3:5,0,0,0),1);
                var heavy=prot(player,Items.CHAINMAIL_CHESTPLATE,3);enchant(player,heavy,Enchantments.FIRE_PROTECTION,3);enchant(player,heavy,Enchantments.BLAST_PROTECTION,3);stock(out,26,heavy,"armor_chain_heavy",p(0,5,2,0,0),1);
                Item[] iron={Items.IRON_HELMET,Items.IRON_CHESTPLATE,Items.IRON_LEGGINGS,Items.IRON_BOOTS},diamond={Items.DIAMOND_HELMET,Items.DIAMOND_CHESTPLATE,Items.DIAMOND_LEGGINGS,Items.DIAMOND_BOOTS};
                String[] ironKeys={"iron_helm_p1","iron_chest_p1","iron_legs_p1","iron_boots_p1"},diamondKeys={"dia_helm_p1","dia_chest_p1","dia_legs_p1","dia_boots_p1"};int[] cost={5,8,7,4};
                for(int n=0;n<4;n++){stock(out,36+n,prot(player,iron[n],1),ironKeys[n],p(0,cost[n],cost[n],0,0),1);stock(out,41+n,prot(player,diamond[n],1),diamondKeys[n],p(0,0,cost[n],3,0),1);}
            }
            case WEAPONS -> {
                stock(out,18,item(Items.WOODEN_AXE),"wep_wood_axe",p(5,0,0,0,0),1);
                stock(out,19,enchant(player,item(Items.STICK),Enchantments.KNOCKBACK,1),"wep_kbstick1",p(64,0,0,0,0),1);
                String[] swordKeys={"wep_gsword_s1","wep_gsword_s2","wep_gsword_s3"};
                for(int n=1;n<=3;n++)stock(out,19+n,enchant(player,item(Items.GOLDEN_SWORD),Enchantments.SHARPNESS,n),swordKeys[n-1],p(0,n==1?1:n==2?3:5,0,0,0),1);
                var iron=enchant(player,item(Items.IRON_SWORD),Enchantments.SHARPNESS,1);enchant(player,iron,Enchantments.KNOCKBACK,1);stock(out,23,iron,"wep_iron_s1k1",p(0,5,3,0,0),1);
                var diamond=enchant(player,item(Items.DIAMOND_SWORD),Enchantments.SHARPNESS,1);enchant(player,diamond,Enchantments.KNOCKBACK,1);stock(out,24,diamond,"wep_dia_s1k1",p(0,0,10,5,0),1);
                var mace=enchant(player,item(Items.MACE),Enchantments.DENSITY,5);enchant(player,mace,Enchantments.WIND_BURST,1);stock(out,25,mace,"wep_mace_d5w1",p(0,0,15,7,0),1);
                stock(out,26,item(Items.WIND_CHARGE),null,p(0,0,8,0,0),8);
                stock(out,38,item(Items.BOW),"bow_p1",p(0,0,3,0,0),1);
                stock(out,39,enchant(player,item(Items.BOW),Enchantments.POWER,1),"bow_p2",p(0,0,5,0,0),1);
                stock(out,40,enchant(player,item(Items.BOW),Enchantments.POWER,2),"bow_p3",p(0,0,10,0,0),1);
                var bow=enchant(player,item(Items.BOW),Enchantments.POWER,2);enchant(player,bow,Enchantments.PUNCH,1);stock(out,41,bow,"bow_p3_k1",p(0,0,10,2,0),1);
                var max=enchant(player,item(Items.BOW),Enchantments.POWER,3);enchant(player,max,Enchantments.PUNCH,1);enchant(player,max,Enchantments.FLAME,1);stock(out,42,max,"bow_p4_k1_f1",p(0,0,15,10,0),1);
                stock(out,49,item(Items.ARROW),null,p(0,0,1,0,0),8);
            }
            case TOOLS -> {
                stock(out,19,item(Items.SHEARS),"tool_shears",p(4,0,0,0,0),1);stock(out,20,item(Items.WOODEN_PICKAXE),"tool_wood_pick",p(7,0,0,0,0),1);stock(out,21,item(Items.IRON_PICKAXE),"tool_iron_pick",p(0,2,0,0,0),1);
                stock(out,22,enchant(player,item(Items.IRON_PICKAXE),Enchantments.EFFICIENCY,2),"tool_iron_pick_e2",p(0,5,2,0,0),1);
                stock(out,23,enchant(player,item(Items.IRON_PICKAXE),Enchantments.EFFICIENCY,4),"tool_iron_pick_e4",p(0,10,5,0,0),1);
                stock(out,24,enchant(player,item(Items.DIAMOND_PICKAXE),Enchantments.EFFICIENCY,3),"tool_dia_pick_e3",p(0,0,7,5,0),1);
                stock(out,25,enchant(player,item(Items.GOLDEN_AXE),Enchantments.EFFICIENCY,5),"tool_gold_axe_e5",p(0,0,1,0,0),1);
            }
            case FOOD -> {stock(out,20,item(Items.BREAD),null,p(2,0,0,0,0),1);stock(out,21,item(Items.COOKED_BEEF),null,p(7,0,0,0,0),1);stock(out,22,item(Items.GOLDEN_CARROT),null,p(0,1,0,0,0),2);stock(out,23,item(Items.GOLDEN_APPLE),null,p(0,0,3,0,0),1);stock(out,24,item(Items.ENCHANTED_GOLDEN_APPLE),null,p(0,0,10,2,0),1);}
            case BLOCKS -> {
                var wool=BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(dye.getName()+"_wool"));var concrete=BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(dye.getName()+"_concrete"));
                stock(out,19,item(wool),null,p(1,0,0,0,0),3);stock(out,20,item(Items.SANDSTONE),null,p(3,0,0,0,0),2);stock(out,21,item(concrete),null,p(5,0,0,0,0),1);stock(out,22,item(Items.END_STONE),null,p(7,0,0,0,0),1);stock(out,23,item(Items.GLASS),null,p(0,1,0,0,0),10);stock(out,24,item(Items.SLIME_BLOCK),null,p(0,4,0,0,0),2);stock(out,25,item(Items.DARK_OAK_FENCE),null,p(0,6,0,0,0),4);stock(out,31,item(Items.OBSIDIAN),null,p(0,0,30,15,0),24);
            }
            case SPECIAL -> {
                stock(out,18,item(Items.CHEST),null,p(0,2,0,0,0),1);stock(out,19,item(Items.ENDER_CHEST),null,p(0,0,6,0,0),1);
                out.add(new Offer(20,named(policy.teamChest(player).copy(),"team_chest"),p(0,0,6,0,0),1,Kind.TEAM_CHEST,null,0));
                stock(out,21,item(Items.TNT),null,p(0,2,1,0,0),1);stock(out,22,item(Items.COMPASS),null,p(0,0,3,1,0),1);stock(out,23,item(Items.FLINT_AND_STEEL),null,p(0,0,2,0,0),1);stock(out,24,item(Items.COBWEB),null,p(0,4,0,0,0),1);stock(out,25,item(Items.LADDER),null,p(3,0,0,0,0),6);stock(out,26,policy.teamZombie(player).copy(),"team_zombie",p(0,12,0,0,0),1);
                stock(out,29,PotionContents.createItemStack(Items.POTION,Potions.REGENERATION),null,p(0,0,5,0,0),1);
                stock(out,30,PotionContents.createItemStack(Items.POTION,Potions.STRENGTH),null,p(0,0,10,2,0),1);
                stock(out,31,PotionContents.createItemStack(Items.POTION,Potions.STRONG_STRENGTH),null,p(0,0,0,6,6),1);
                stock(out,32,PotionContents.createItemStack(Items.POTION,Potions.TURTLE_MASTER),null,p(0,16,0,0,0),1);
                stock(out,33,PotionContents.createItemStack(Items.POTION,Potions.LONG_FIRE_RESISTANCE),null,p(0,0,3,0,0),1);
            }
            case ENCHANTS -> {
                var kinds=new Kind[]{Kind.ARMOR,Kind.WEAPONS,Kind.TOOLS,Kind.BOWS,Kind.KNOCKBACK};var icons=new Item[]{Items.IRON_CHESTPLATE,Items.IRON_SWORD,Items.IRON_PICKAXE,Items.BOW,Items.STICK};var keys=new String[]{"enchant_armor","enchant_weapon","enchant_tool","enchant_bow","upgrade_knockback"};int[] costs={10,5,5,15,5};
                for(int n=0;n<5;n++)out.add(new Offer(29+n,named(item(icons[n]),keys[n]),p(0,0,0,0,costs[n]),1,kinds[n],null,0));
            }
            case BEACONS -> {
                Item[] icons={Items.SUGAR,Items.RABBIT_FOOT,Items.MAGMA_CREAM,Items.BLAZE_POWDER,Items.GOLDEN_PICKAXE,Items.GHAST_TEAR,Items.SHIELD};String[] keys={"beacon_speed2_5m","beacon_jump2_5m","beacon_fire_15m","beacon_strength2_2m","beacon_haste2_10m","beacon_regen2_3m","beacon_resistance1_5m"};int[] seconds={300,300,900,120,600,180,300};
                for(int n=0;n<7;n++)out.add(new Offer(19+n,named(item(icons[n]),keys[n]),p(0,0,0,0,5),1,Kind.BEACON,Beacon.values()[n],seconds[n]));
            }
        }
        return List.copyOf(out);
    }
    private static List<Component> priceLore(Offer offer){
        var lines=new ArrayList<Component>();boolean bare=offer.kind!=Kind.ITEM&&offer.kind!=Kind.TEAM_CHEST;
        for(int n=0;n<5;n++)if(offer.price.at(n)>0){String key=bare?BARE_CURRENCY_KEYS[n]:CURRENCY_KEYS[n];lines.add(Component.literal(offer.price.at(n)+"\u00d7 ").withStyle(ChatFormatting.WHITE,ChatFormatting.BOLD).append(Component.translatable(key).withStyle(COLORS[n])));}
        return lines;
    }
    private boolean buy(Menu menu,Offer offer,boolean shift){
        if(!menu.valid()||offer.item.isEmpty()||offer.kind==Kind.LUCKY_BLOCK&&!policy.lucky())return false;
        var player=menu.getPlayer();var inventory=player.getInventory();var before=new ArrayList<ItemStack>();
        for(int n=0;n<inventory.getContainerSize();n++)before.add(inventory.getItem(n).copy());var work=new ArrayList<ItemStack>();for(var stack:before)work.add(stack.copy());
        boolean ordinary=offer.kind==Kind.ITEM;int repeats=ordinary&&shift?(offer.item.getMaxStackSize()+offer.gives-1)/offer.gives:1;
        for(int c=0;c<5;c++)if(offer.price.at(c)>0){long total=0;for(var stack:work)if(stack.is(CURRENCY[c]))total+=stack.getCount();repeats=(int)Math.min(repeats,total/offer.price.at(c));}
        if(repeats<=0)return false;
        for(int c=0;c<5;c++){int remaining=offer.price.at(c)*repeats;for(int n=0;n<work.size()&&remaining>0;n++){var stack=work.get(n);if(!stack.is(CURRENCY[c]))continue;int take=Math.min(remaining,stack.getCount());stack.shrink(take);remaining-=take;}}
        ItemStack overflow=ItemStack.EMPTY;
        if(ordinary||offer.kind==Kind.TEAM_CHEST||offer.kind==Kind.LUCKY_BLOCK){
            var output=offer.item.copyWithCount(Math.min(offer.item.getMaxStackSize(),repeats*offer.gives));int armorSlot=armorSlot(output);
            if(armorSlot>=0&&armorSlot<work.size()&&work.get(armorSlot).isEmpty()){work.set(armorSlot,output.copyWithCount(1));output.shrink(1);}
            for(int n=0;n<36&&!output.isEmpty();n++){var at=work.get(n);if(!at.isEmpty()&&ItemStack.isSameItemSameComponents(at,output)){int amount=Math.min(output.getCount(),at.getMaxStackSize()-at.getCount());if(amount>0){at.grow(amount);output.shrink(amount);}}}
            for(int n=0;n<36&&!output.isEmpty();n++)if(work.get(n).isEmpty()){int amount=Math.min(output.getCount(),output.getMaxStackSize());work.set(n,output.copyWithCount(amount));output.shrink(amount);}
            overflow=output;
        }else if(offer.kind!=Kind.BEACON)upgrade(player,work,offer.kind);
        if(!menu.valid()||!sameInventory(player,before))return false;
        ItemEntity published=null;
        if(!overflow.isEmpty()){
            published=new ItemEntity(player.level(),player.getX(),player.getY(),player.getZ(),overflow.copy());published.setDefaultPickUpDelay();
            if(!player.level().addFreshEntity(published))return false;
            if(!menu.valid()||!sameInventory(player,before)){published.discard();return false;}
        }
        for(int n=0;n<work.size();n++)if(!ItemStack.matches(before.get(n),work.get(n)))inventory.setItem(n,work.get(n));
        inventory.setChanged();
        if(offer.kind==Kind.BEACON)policy.beacon(player,offer.buff,offer.seconds);
        player.inventoryMenu.broadcastChanges();return true;
    }
    private static boolean sameInventory(ServerPlayer player,List<ItemStack> before){for(int n=0;n<before.size();n++)if(!ItemStack.matches(before.get(n),player.getInventory().getItem(n)))return false;return true;}
    private static int armorSlot(ItemStack stack){
        var equip=stack.get(DataComponents.EQUIPPABLE);if(equip==null)return -1;
        // Current native armor tags include newly added vanilla materials; elytra/heads/pumpkins stay excluded.
        if(!(stack.is(ItemTags.HEAD_ARMOR)||stack.is(ItemTags.CHEST_ARMOR)||stack.is(ItemTags.LEG_ARMOR)||stack.is(ItemTags.FOOT_ARMOR)))return -1;
        return switch(equip.slot()){case FEET->36;case LEGS->37;case CHEST->38;case HEAD->39;default->-1;};
    }
    private static boolean vanillaFamily(ItemStack stack){return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("minecraft");}
    private static void upgrade(ServerPlayer player,List<ItemStack> work,Kind kind){
        if(kind==Kind.ARMOR){for(int n=36;n<=39&&n<work.size();n++)for(var key:List.of(Enchantments.FIRE_PROTECTION,Enchantments.PROJECTILE_PROTECTION,Enchantments.BLAST_PROTECTION,Enchantments.FEATHER_FALLING))increase(player,work.get(n),key,1,false);return;}
        // Confirmed in actual old Paper bytecode: getContents includes equipment, then source visits armor/offhand AGAIN.
        var indices=new ArrayList<Integer>();for(int n=0;n<work.size();n++)indices.add(n);for(int n=36;n<=40&&n<work.size();n++)indices.add(n);
        for(int n:indices){var stack=work.get(n);if(stack.isEmpty())continue;
            switch(kind){
                case WEAPONS -> {if(vanillaFamily(stack)&&(stack.is(ItemTags.SWORDS)||stack.is(ItemTags.AXES)||stack.is(ItemTags.SPEARS)||stack.is(Items.TRIDENT)||stack.is(Items.MACE)))increase(player,stack,Enchantments.SHARPNESS,1,false);}
                case TOOLS -> {if(vanillaFamily(stack)&&(stack.is(ItemTags.PICKAXES)||stack.is(ItemTags.AXES)||stack.is(ItemTags.SHOVELS)||stack.is(ItemTags.HOES)||stack.is(Items.SHEARS)))increase(player,stack,Enchantments.EFFICIENCY,1,false);}
                case BOWS -> {if(stack.is(Items.BOW)){increase(player,stack,Enchantments.POWER,2,false);increase(player,stack,Enchantments.INFINITY,1,true);increase(player,stack,Enchantments.FLAME,1,true);increase(player,stack,Enchantments.PUNCH,1,true);}}
                case KNOCKBACK -> {var key=player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.KNOCKBACK);if(stack.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY).getLevel(key)>0)increase(player,stack,Enchantments.KNOCKBACK,1,false);}
                default -> { }
            }
        }
    }
    private static void increase(ServerPlayer player,ItemStack stack,ResourceKey<Enchantment> key,int delta,boolean minimum){
        if(stack.isEmpty())return;var holder=player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);int old=stack.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY).getLevel(holder);
        int value=minimum?Math.max(old,delta):(int)Math.min(Integer.MAX_VALUE,(long)old+delta);enchant(player,stack,key,value);
    }
    final class Menu extends SimpleGui {
        private final Entity shop;private final String epoch;private final int team;private Category category=Category.ARMOR;private final Map<Integer,Offer> actions=new HashMap<>();
        Menu(ServerPlayer player,Entity shop){super(MenuType.GENERIC_9x6,player,false);this.shop=shop;this.epoch=policy.epoch();this.team=policy.team(player);setTitle(Component.translatable("bedwars.shop.title"));setLockPlayerInventory(false);render();}
        boolean valid(){return epoch.equals(policy.epoch())&&team==policy.team(getPlayer())&&eligible(getPlayer(),shop);}
        private void render(){
            actions.clear();for(int n=0;n<54;n++)clearSlot(n);
            Item[] icons={Items.LEATHER_CHESTPLATE,Items.GOLDEN_SWORD,Items.IRON_PICKAXE,Items.COOKED_BEEF,Items.SANDSTONE,Items.TNT,Items.ENCHANTING_TABLE,Items.BEACON};String[] keys={"category_armor","category_weapons","category_tools","category_food","category_blocks","special_items","category_enchantments","category_beacons"};
            for(int n=0;n<8;n++)setSlot(n<6?n:n+1,new GuiElementBuilder(named(item(icons[n]),keys[n])).build());
            if(policy.lucky())setSlot(6,new GuiElementBuilder(named(item(LbcLuckyBlocks.item(LbcLuckyBlocks.Kind.NORMAL)),"category_lucky_blocks")).build());
            var separator=item(Items.PALE_HANGING_MOSS);separator.set(DataComponents.ITEM_NAME,Component.empty());separator.set(DataComponents.TOOLTIP_DISPLAY,new TooltipDisplay(true,new LinkedHashSet<>()));
            for(int n=9;n<18;n++)setSlot(n,new GuiElementBuilder(separator).build());
            for(var offer:offers(getPlayer(),category)){actions.put(offer.slot,offer);var shown=offer.item.copy();shown.setCount(offer.gives==2&&offer.item.is(Items.LEATHER_CHESTPLATE)?1:offer.gives);shown.set(DataComponents.LORE,new ItemLore(priceLore(offer)));setSlot(offer.slot,new GuiElementBuilder(shown).build());}
        }
        @Override public boolean onAnyClick(int slot,ClickType type,ContainerInput action){
            if(!isOpen()||!valid()){close();return false;}
            // Let vanilla handle ordinary lower-inventory moves; intercept every transfer that could enter top/cursor collection.
            return slot>=54&&!type.shift&&!type.isDragging&&!type.numKey&&action!=ContainerInput.PICKUP_ALL&&action!=ContainerInput.QUICK_MOVE&&action!=ContainerInput.QUICK_CRAFT;
        }
        @Override public boolean click(int slot,ClickType type,ContainerInput action){
            if(!isOpen()||!valid()){close();return true;}
            boolean purchase=(action==ContainerInput.PICKUP||action==ContainerInput.QUICK_MOVE)&&!type.numKey&&!type.isDragging&&type!=ClickType.MOUSE_DOUBLE_CLICK;
            if(slot>=0&&slot<9&&(slot!=6||policy.lucky())&&purchase){category=slot==6?Category.LUCKY:Category.values()[slot<6?slot:slot-1];getPlayer().playSound(SoundEvents.UI_BUTTON_CLICK.value(),1f,1.2f);render();}
            else if(slot>=18&&slot<54&&purchase){var offer=actions.get(slot);if(offer!=null){boolean ok=buy(this,offer,type.shift);getPlayer().playSound(ok?SoundEvents.PLAYER_LEVELUP:SoundEvents.VILLAGER_NO,1f,1f);getPlayer().playSound(ok?SoundEvents.PLAYER_LEVELUP:SoundEvents.VILLAGER_NO,1f,1f);if(valid())render();}}
            getPlayer().containerMenu.sendAllDataToRemote();return true;
        }
        @Override public boolean onClick(int slot,ClickType type,ContainerInput action,GuiElement element){getPlayer().containerMenu.sendAllDataToRemote();return true;}
        @Override public boolean canSpectatorsClick(){return false;}
        @Override public void onRemoved(){menus.remove(this);}
        @Override public void handleException(Throwable error){
            getPlayer().containerMenu.resumeRemoteUpdates();getPlayer().containerMenu.sendAllDataToRemote();
            me.skywars2.SkyWars2.LOGGER.error("BedWars shop input failed",error);close();
        }
    }
}
