package me.skywars2.game;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.skywars2.SkyWars2;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Authorized native replacement for the source minecart menu. No global inventory clearing. */
public final class DeveloperTools {
    public static Item ITEM;
    private final GameController game;
    private final ContentGallery gallery;
    private final MapAuthoring authoring;
    private final Set<UUID> visualizing=new HashSet<>();
    private final Map<UUID,Integer> teams=new HashMap<>();
    private int ticks;

    public enum MarkerKind {
        SPAWN(Items.BEE_SPAWN_EGG,"Player Spawn",0xffff00,"swrg.spawn"),
        LOOK(Items.GHAST_SPAWN_EGG,"Look Marker",0xffffff,"swrg.look"),
        DELETE(Items.ENDERMAN_SPAWN_EGG,"Delete Marker",0x000000,"swrg.delete"),
        DEFAULT(Items.CREEPER_SPAWN_EGG,"Default Chest",0x80ff00,"swrg.default"),
        MIDLE(Items.CHICKEN_SPAWN_EGG,"Midle Chest",0xff0000,"swrg.midle"),
        CENTER(Items.DROWNED_SPAWN_EGG,"Center Chest",0x80cfff,"swrg.center"),
        COPPER(Items.FOX_SPAWN_EGG,"Copper generator",0x804020,"copper_generator","strict_map_object"),
        IRON(Items.WOLF_SPAWN_EGG,"Iron generator",0xcccccc,"iron_generator","strict_map_object"),
        GOLD(Items.BLAZE_SPAWN_EGG,"Gold generator",0xff8000,"gold_generator","strict_map_object"),
        DIAMOND(Items.ALLAY_SPAWN_EGG,"Diamond generator",0x0000ff,"diamond_generator","strict_map_object"),
        STAR(Items.SNIFFER_SPAWN_EGG,"Star generator",0x0000ff,"star_generator","strict_map_object"),
        SHOP(Items.VILLAGER_SPAWN_EGG,"Shop",0xffffff,"bedwars_shop","strict_map_object"),
        BED(Items.ENDERMITE_SPAWN_EGG,"Bed Place",0x00ff00,"bedwars_bed","strict_map_object"),
        STRICT_SPAWN(Items.BEE_SPAWN_EGG,"STRICT Player Spawn",0xffff00,"swrg.spawn","strict_map_object"),
        STRICT_LOOK(Items.GHAST_SPAWN_EGG,"STRICT Look Marker",0xffffff,"swrg.look","strict_map_object");
        public final Item egg; public final String label; public final int color; public final List<String> tags;
        MarkerKind(Item egg,String label,int color,String...tags){this.egg=egg;this.label=label;this.color=color;this.tags=List.of(tags);}
        public ItemStack stack(){
            var stack=new ItemStack(egg); var data=new CompoundTag(); data.putString("developer_marker",name());
            stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            stack.set(DataComponents.CUSTOM_NAME,Component.literal(label));
            var entity=new CompoundTag();var list=new ListTag();for(var tag:tags)list.add(StringTag.valueOf(tag));entity.put("Tags",list);
            if(this==SHOP){entity.putBoolean("PersistenceRequired",true);entity.putBoolean("NoAI",true);entity.putBoolean("Invulnerable",true);entity.putFloat("Health",100000000);var attributes=new ListTag();var health=new CompoundTag();health.putString("id","minecraft:max_health");health.putDouble("base",100000000);attributes.add(health);entity.put("attributes",attributes);}
            stack.set(DataComponents.ENTITY_DATA,TypedEntityData.of(this==SHOP?EntityTypes.VILLAGER:EntityTypes.MARKER,entity));
            return stack;
        }
    }
    public DeveloperTools(GameController game){this.game=game;gallery=new ContentGallery(game);authoring=new MapAuthoring(game);}
    public ContentGallery gallery(){return gallery;}
    public MapAuthoring authoring(){return authoring;}
    public static void registerItem(){
        if(ITEM!=null)return;
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:dev_tools"));
        var flags=new CompoundTag();flags.putBoolean("developer_menu",true);
        ITEM=Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(new Item.Properties().setId(key).stacksTo(1)
                .component(DataComponents.CUSTOM_NAME,Component.literal("Developer Tools").withStyle(net.minecraft.ChatFormatting.GREEN))
                .component(DataComponents.CUSTOM_DATA,CustomData.of(flags)),Items.PAPER,true));
    }
    public static boolean authorized(ServerPlayer player){return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(player.createCommandSourceStack());}
    private static void message(ServerPlayer player,String text){player.sendSystemMessage(Component.literal(text));}
    private static DeveloperTools service(){return Objects.requireNonNull(SkyWars2.game(),"Game not running").developerTools();}
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher){
        java.util.function.Predicate<CommandSourceStack> op=source -> Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source);
        dispatcher.register(Commands.literal("gallery").requires(op).executes(ctx->{service().gallery.spawn(ctx.getSource().getPlayerOrException());return 1;}));
        dispatcher.register(Commands.literal("developer").requires(op).then(Commands.argument("enabled",IntegerArgumentType.integer(0,1)).executes(ctx->{
            var p=ctx.getSource().getPlayerOrException();boolean enabled=IntegerArgumentType.getInteger(ctx,"enabled")==1;
            SkyWars2.game().developerMode(p,enabled);if(enabled)service().giveTools(p);return 1;})));
        var save=Commands.literal("mapsave").requires(op).executes(ctx->{service().open(ctx.getSource().getPlayerOrException());return 1;})
                .then(Commands.literal("tools").executes(ctx->{service().giveTools(ctx.getSource().getPlayerOrException());return 1;}))
                .then(Commands.literal("pos").then(Commands.argument("corner",IntegerArgumentType.integer(0,1)).executes(ctx->{service().authoring.select(ctx.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(ctx,"corner"));return 1;})));
        for(String mode:List.of("skywars","bedwars"))save.then(Commands.literal("save"+mode).then(Commands.argument("name",StringArgumentType.greedyString()).executes(ctx->{service().authoring.save(ctx.getSource().getPlayerOrException(),mode,StringArgumentType.getString(ctx,"name"));return 1;})));
        dispatcher.register(save);
    }
    public void giveTools(ServerPlayer player){if(!authorized(player))return;for(int i=0;i<player.getInventory().getContainerSize();i++)if(isTools(player.getInventory().getItem(i)))return;give(player,new ItemStack(ITEM));}
    private static void give(ServerPlayer player,ItemStack stack){if(!player.getInventory().add(stack))player.drop(stack,false,net.minecraft.util.Prediction.SERVER_ONLY);}
    static boolean isTools(ItemStack stack){var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();return stack.is(ITEM)||data.getBooleanOr("developer_menu",false)||data.getIntOr("developer_menu",0)==1;}
    static MarkerKind kind(ItemStack stack){
        String nativeKind=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getStringOr("developer_marker","");
        for(var kind:MarkerKind.values())if(kind.name().equals(nativeKind))return kind;
        var entity=stack.get(DataComponents.ENTITY_DATA);if(entity==null||entity.type()!=EntityTypes.MARKER&&entity.type()!=EntityTypes.VILLAGER)return null;
        var tags=entity.copyTagWithoutId().getListOrEmpty("Tags");var found=new HashSet<String>();for(var tag:tags)tag.asString().ifPresent(found::add);
        for(var kind:MarkerKind.values())if(kind.egg==stack.getItem()&&found.containsAll(kind.tags)&&found.contains("strict_map_object")==kind.tags.contains("strict_map_object"))return kind;
        return null;
    }
    public boolean useItem(ServerPlayer player,InteractionHand hand){var stack=player.getItemInHand(hand);if(isTools(stack)){if(authorized(player))open(player);return true;}return kind(stack)!=null;}
    public boolean useBlock(ServerPlayer player,InteractionHand hand,BlockHitResult hit){
        var stack=player.getItemInHand(hand);var kind=kind(stack);
        if(kind!=null){if(authorized(player))place(player,kind,Vec3.atBottomCenterOf(hit.getBlockPos().relative(hit.getDirection())),stack);return true;}
        if(useItem(player,hand))return true;
        return gallery.useBlock(player,hit.getBlockPos());
    }
    public boolean useEntity(ServerPlayer player,Entity entity){if(isTools(player.getMainHandItem())||isTools(player.getOffhandItem())){if(authorized(player))open(player);return true;}if(kind(player.getMainHandItem())!=null||kind(player.getOffhandItem())!=null)return true;return gallery.useEntity(player,entity);}
    static String ownerTag(ServerPlayer player){return "developer_owner:"+player.getUUID();}
    public List<Entity> ownedMarkers(ServerPlayer player){var found=new ArrayList<Entity>();for(var entity:player.level().getAllEntities())if(entity.entityTags().contains(ownerTag(player))&&entity.entityTags().contains("developer_map_marker"))found.add(entity);return found;}
    /** Only the authored map schemas qualify; general spell markers are never map tools targets. */
    public List<Entity> mapMarkers(ServerPlayer player){
        var result=new ArrayList<Entity>();var bounds=authoring.selectedBounds(player);
        for(var entity:player.level().getAllEntities()){
            if(entity.isRemoved()||!(entity instanceof Marker||entity instanceof Villager&&entity.entityTags().contains("bedwars_shop")))continue;
            if(bounds!=null?!bounds.contains(entity.position()):entity.distanceToSqr(player)>256*256)continue;
            for(var kind:MarkerKind.values())if(kind!=MarkerKind.DELETE&&entity.entityTags().contains(kind.tags.getFirst())){result.add(entity);break;}
        }
        return result;
    }
    Entity place(ServerPlayer player,MarkerKind kind,Vec3 at,ItemStack stack){
        if(!authorized(player))return null;
        if(kind==MarkerKind.DELETE){for(var entity:mapMarkers(player))if(entity.position().distanceToSqr(at)<=9)entity.discard();if(!player.isCreative())stack.shrink(1);return null;}
        Entity entity;
        if(kind==MarkerKind.SHOP){var shop=new Villager(EntityTypes.VILLAGER,player.level());shop.setNoAi(true);shop.setPersistenceRequired();shop.setPermanentlyInvulnerable(true);shop.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100000000);shop.setHealth(shop.getMaxHealth());entity=shop;}
        else entity=new Marker(EntityTypes.MARKER,player.level());
        int team=teams.getOrDefault(player.getUUID(),1);
        if(entity instanceof Marker){
            var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,player.registryAccess());entity.saveWithoutId(output);
            var nbt=output.buildResult();var persistent=nbt.getCompoundOrEmpty("data");persistent.putInt("skywars2_team",team);nbt.put("data",persistent);
            entity.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,player.registryAccess(),nbt));
        }
        entity.setPos(at);entity.setYRot(player.getYRot());entity.entityTags().addAll(kind.tags);entity.entityTags().add(ownerTag(player));entity.entityTags().add("developer_map_marker");
        if(!player.level().addFreshEntity(entity))return null;
        var board=game.server().getScoreboard();var nativeTeam=board.getPlayerTeam(Integer.toString(team));if(nativeTeam==null)nativeTeam=board.addPlayerTeam(Integer.toString(team));board.addPlayerToTeam(entity.getScoreboardName(),nativeTeam);
        if(!player.isCreative())stack.shrink(1);return entity;
    }
    public void open(ServerPlayer player){
        if(!authorized(player))return;
        var gui=new SimpleGui(MenuType.GENERIC_9x3,player,false);gui.setTitle(Component.literal("Developer Tools"));gui.setLockPlayerInventory(true);
        button(gui,player,0,Items.BARRIER,"Отображение маркеров",()->{});
        button(gui,player,9,BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("lime_concrete")),"ВКЛ",()->visualizing.add(player.getUUID()));
        button(gui,player,18,BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("red_concrete")),"ВЫКЛ",()->visualizing.remove(player.getUUID()));
        button(gui,player,1,Items.STRUCTURE_BLOCK,"Сохранить карту",()->saveMenu(player));
        button(gui,player,10,Items.STRUCTURE_BLOCK,"Сбросить выделение",()->{authoring.clearSelection(player);message(player,"Выделение сброшено.");});
        button(gui,player,2,Items.BEE_SPAWN_EGG,"Выдать маркеры",()->{for(var kind:MarkerKind.values())give(player,kind.stack());});
        button(gui,player,11,Items.SPYGLASS,"Посчитать маркеры",()->count(player));
        button(gui,player,20,Items.GLASS,"Поставить клетки",()->cages(player));
        button(gui,player,17,Items.CLOCK,"Завершить игру",()->{gui.close();game.stopMatch();});
        button(gui,player,26,Items.COMMAND_BLOCK,"/reload",()->{gui.close();game.server().getCommands().performPrefixedCommand(player.createCommandSourceStack(),"reload");});
        button(gui,player,19,BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("white_bed")),"BedWars team: "+teams.getOrDefault(player.getUUID(),1),()->{teams.compute(player.getUUID(),(id,n)->n==null?2:n%16+1);open(player);});
        gui.open();
    }
    private void saveMenu(ServerPlayer player) {
        if (!authorized(player)) return;
        var gui = new SimpleGui(MenuType.GENERIC_9x3,player,false); gui.setTitle(Component.literal("Сохранение карты")); gui.setLockPlayerInventory(true);
        button(gui,player,10,BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("lime_dye")),"Первая точка здесь",()->authoring.select(player,0));
        button(gui,player,12,BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("red_dye")),"Вторая точка здесь",()->authoring.select(player,1));
        button(gui,player,14,Items.GRASS_BLOCK,"Сохранить SkyWars",()->saveName(player,gui,"saveskywars"));
        button(gui,player,16,Items.ENDER_PEARL,"Сохранить BedWars",()->saveName(player,gui,"savebedwars"));
        button(gui,player,18,Items.ARROW,"Назад",()->open(player)); gui.open();
    }
    private void saveName(ServerPlayer player,SimpleGui gui,String mode) {
        gui.close(); player.sendSystemMessage(Component.literal("[Нажми и допиши название карты]")
                .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.YELLOW).withClickEvent(
                        new net.minecraft.network.chat.ClickEvent.SuggestCommand("/mapsave "+mode+" "))));
    }
    private void button(SimpleGui gui,ServerPlayer player,int slot,Item item,String name,Runnable action){var stack=new ItemStack(item);var data=new CompoundTag();data.putBoolean("is_gui",true);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));gui.setSlot(slot,new GuiElementBuilder(stack).setName(Component.literal(name)).setCallback((index,type,input,source)->{if(!authorized(player)){gui.close();return;}if(input==net.minecraft.world.inventory.ContainerInput.PICKUP_ALL||type==eu.pb4.sgui.api.ClickType.MOUSE_DOUBLE_CLICK)return;action.run();}).build());}
    private void count(ServerPlayer player){var entities=mapMarkers(player);for(var kind:MarkerKind.values())if(kind!=MarkerKind.DELETE&&kind!=MarkerKind.STRICT_SPAWN&&kind!=MarkerKind.STRICT_LOOK)message(player,kind.label+": "+entities.stream().filter(e->e.entityTags().contains(kind.tags.getFirst())).count());}
    private void cages(ServerPlayer player){
        for(var marker:mapMarkers(player))if(marker.entityTags().contains("swrg.spawn")){
            var origin=marker.blockPosition();for(var pos:BlockPos.betweenClosed(origin.offset(-1,-2,-1),origin.offset(1,5,1)))if(player.level().getBlockState(pos).is(Blocks.GLASS))player.level().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            for(int x=-1;x<=1;x++)for(int y=-1;y<=3;y++)for(int z=-1;z<=1;z++){boolean hole=(y>=0&&x==0&&z==0)||(Math.abs(x)==1&&Math.abs(z)==1);player.level().setBlockAndUpdate(origin.offset(x,y,z),(hole?Blocks.AIR:Blocks.GLASS).defaultBlockState());}
        }
    }
    public void tick(){gallery.tick();authoring.tick();if(++ticks%5!=0)return;for(var id:List.copyOf(visualizing)){var player=game.server().getPlayerList().getPlayer(id);if(player==null||!authorized(player)){visualizing.remove(id);continue;}for(var entity:mapMarkers(player)){if(entity.distanceToSqr(player)>128*128)continue;for(var kind:MarkerKind.values())if(kind!=MarkerKind.SHOP&&entity.entityTags().contains(kind.tags.getFirst())){player.level().sendParticles(player,new DustParticleOptions(kind.color,1),false,false,entity.getX(),entity.getY()+.5,entity.getZ(),6,.25,.5,.25,0);break;}}}}
    public void close(){gallery.close();authoring.close();visualizing.clear();teams.clear();}
}
