package me.skywars2.game;

import java.util.List;
import me.skywars2.SkyWars2;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Three stable server blocks; vanilla BLOCK loot owns rolls, tools and explosion context. */
public final class LbcLuckyBlocks {
    private static final ThreadLocal<MiningLuck> MINING_LUCK=new ThreadLocal<>();
    /** Native wear removes equipment modifiers before playerDestroy builds its loot. */
    public record MiningLuck(ServerPlayer player,net.minecraft.world.phys.Vec3 origin,double luck,MiningLuck previous) implements AutoCloseable {
        @Override public void close(){if(previous==null)MINING_LUCK.remove();else MINING_LUCK.set(previous);}
    }
    public static MiningLuck miningScope(ServerPlayer player,BlockPos pos){
        if(kind(player.level().getBlockState(pos))==null)return null;
        var snapshot=new MiningLuck(player,net.minecraft.world.phys.Vec3.atCenterOf(pos),player.getAttributeValue(Attributes.LUCK),MINING_LUCK.get());
        MINING_LUCK.set(snapshot);return snapshot;
    }
    public enum Kind { NORMAL, NEKO, LOLI }
    private static final Block[] BLOCKS=new Block[3];
    private static final Item[] ITEMS=new Item[3];
    private static final BlockState[] LEGACY_STATES=new BlockState[3];
    private static final BlockState[] CLIENT_STATES=new BlockState[3];
    static {
        for(var kind:Kind.values()) {
            var raw=carrierBlock(kind).defaultBlockState();
            for(var property:List.of(HugeMushroomBlock.DOWN,HugeMushroomBlock.UP,HugeMushroomBlock.NORTH,HugeMushroomBlock.SOUTH,HugeMushroomBlock.EAST,HugeMushroomBlock.WEST))raw=raw.setValue(property,false);
            LEGACY_STATES[kind.ordinal()]=raw;
            var id=Identifier.parse("skywars2:"+suffix(kind));
            var model=Identifier.parse(switch(kind){case NORMAL->"minecraft:block/lucky_block";case NEKO->"minecraft:block/neko_lucky_block";case LOLI->"minecraft:block/lucky_block_loli";});
            CLIENT_STATES[kind.ordinal()]=LbcBlockModels.fullCube(id,model);
            var blockKey=ResourceKey.create(Registries.BLOCK,id);
            var block=Registry.register(BuiltInRegistries.BLOCK,blockKey,new LuckyBlock(
                    BlockBehaviour.Properties.ofFullCopy(carrierBlock(kind)).setId(blockKey),kind));
            BLOCKS[kind.ordinal()]=block;
            var itemKey=ResourceKey.create(Registries.ITEM,id);
            var properties=new Item.Properties(){@Override public Identifier effectiveModel(){return Identifier.parse("lbc:"+suffix(kind));}}.setId(itemKey)
                    .component(DataComponents.ITEM_MODEL,Identifier.parse("lbc:"+suffix(kind)))
                    .component(DataComponents.CUSTOM_NAME,name(kind))
                    .component(DataComponents.LORE,new net.minecraft.world.item.component.ItemLore(lore(kind)));
            // Keep the original mushroom carriers' native composting component on all issued profiles.
            properties.compostable(kind==Kind.LOLI
                    ?net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COMPOSTABLE_MEDIUM
                    :net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COMPOSTABLE_MEDIUM_HIGH);
            ITEMS[kind.ordinal()]=Registry.register(BuiltInRegistries.ITEM,itemKey,
                    new LbcBlockModels.CubeItem(block,properties){
                        @Override public InteractionResult useOn(UseOnContext context){
                            var game=SkyWars2.game();
                            if(!(context.getPlayer() instanceof ServerPlayer player)||game==null
                                    ||!game.canUseGameplayItems(player)||game.isObserver(player)
                                    ||game.items().actionsBlocked(player)||!LbcDimensionItems.ordinary(player))return InteractionResult.FAIL;
                            return super.useOn(context);
                        }
                    });
        }
    }
    private LbcLuckyBlocks(){}
    public static void register(){}
    private static String suffix(Kind kind){return switch(kind){case NORMAL->"lucky_block";case NEKO->"lucky_block_neko";case LOLI->"lucky_block_loli";};}
    private static Block carrierBlock(Kind kind){return switch(kind){case NORMAL->Blocks.RED_MUSHROOM_BLOCK;case NEKO->Blocks.BROWN_MUSHROOM_BLOCK;case LOLI->Blocks.MUSHROOM_STEM;};}
    private static Item carrierItem(Kind kind){return switch(kind){case NORMAL->Items.RED_MUSHROOM_BLOCK;case NEKO->Items.BROWN_MUSHROOM_BLOCK;case LOLI->Items.MUSHROOM_STEM;};}
    public static Block block(Kind kind){return BLOCKS[kind.ordinal()];}
    public static Item item(Kind kind){return ITEMS[kind.ordinal()];}
    private static Component name(Kind kind){return switch(kind){
        case NORMAL->Component.translatable("skywars2.item.lucky_block.name").withColor(0xffff55).withStyle(s->s.withItalic(false));
        case NEKO->Component.literal("Lucky Block Neko").withColor(0xff55ff).withStyle(s->s.withItalic(false));
        case LOLI->Component.literal("\u2764 Lucky Block Loli \u2764").withColor(0xffb8e0).withStyle(s->s.withItalic(false));
    };}
    private static List<Component> lore(Kind kind){return switch(kind){
        case NORMAL->List.of(Component.translatable("skywars2.item.lucky_block.credits.1").withColor(0xaaaaaa).withStyle(s->s.withItalic(false)),Component.translatable("skywars2.item.lucky_block.credits.2").withColor(0xaaaaaa).withStyle(s->s.withItalic(false)));
        case NEKO->List.of(Component.literal("By BaffledHedgehog").withColor(0xaaaaaa).withStyle(s->s.withItalic(false)));
        case LOLI->List.of(Component.literal("Community of the Lucky Wars \u2764").withColor(0xaaaaaa).withStyle(s->s.withItalic(false)),Component.literal("Daeris_ | Beta Server harem event").withColor(0x555555).withStyle(s->s.withItalic(false)));
    };}
    public static BlockState legacyState(Kind kind){return LEGACY_STATES[kind.ordinal()];}
    /** A natural mushroom is not a Lucky Block, even when one hidden face is false. */
    public static Kind kind(BlockState state){
        for(var kind:Kind.values()) {
            if(state.is(block(kind)))return kind;
        }
        return null;
    }
    public static void refreshLuck(GameController game,Entity source){
        refreshLuck(game,source,null);
    }
    private static void refreshLuck(GameController game,Entity source,MiningLuck snapshot){
        if(source instanceof ServerPlayer living){
            // Matches execute store result attribute get 1, followed by scoreboard add1.
            int amount=(int)(snapshot==null?living.getAttributeValue(Attributes.LUCK):snapshot.luck());
            game.items().mana().score(source,"luck_amount").set(amount+1);
        }
    }
    public static void prepare(BlockState state,LootParams.Builder params){
        if(kind(state)==null)return;
        var game=SkyWars2.game();
        if(game!=null){
            var source=params.getOptionalParameter(LootContextParams.THIS_ENTITY);var snapshot=MINING_LUCK.get();
            if(snapshot!=null&&(snapshot.player()!=source||!snapshot.origin().equals(params.getOptionalParameter(LootContextParams.ORIGIN))))snapshot=null;
            refreshLuck(game,source,snapshot);
        }
    }
    public static boolean silkToken(ItemStack stack){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("adv_check_lbc",0)==1;}
    public static void silkToken(net.minecraft.world.entity.item.ItemEntity token){
        var game=SkyWars2.game();if(game==null||!(token.level() instanceof net.minecraft.server.level.ServerLevel level))return;
        for(var player:level.players())if(!player.isSpectator()&&player.distanceToSqr(token)<=49
                &&LbcFartus.data(player.getMainHandItem()).getIntOr("silk",0)==1){
            game.items().mana().award(player,"lbc:true_advancements/pickaxes/silk");break;
        }
    }
    /** Retain a real block context, substituting only the source block ID for legacy predicates. */
    private static List<ItemStack> drops(Kind kind,LootParams.Builder params){
        prepare(block(kind).defaultBlockState(),params);
        params.withParameter(LootContextParams.BLOCK_STATE,legacyState(kind));
        var table=ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("lbc:native_lucky/"+kind.name().toLowerCase(java.util.Locale.ROOT)));
        return params.getLevel().getServer().reloadableRegistries().getLootTable(table)
                .getRandomItems(params.create(LootContextParamSets.BLOCK));
    }
    /** The source follows the breaker, rather than whoever happened to place the block. */
    public static LbcSpellSource.Scope rewardScope(BlockState state,Entity breaker){
        var game=SkyWars2.game();
        if(game==null||kind(state)==null)return null;
        var inherited=game.items().spellSources().current();
        if(inherited!=null)return game.items().spellSources().scope(inherited.hostileLoot());
        return breaker instanceof ServerPlayer player
                ?game.items().spellSources().scope(game.items().spellSources().hostile(player,player,java.util.Set.of()).hostileLoot(),player.level()):null;
    }
    private static final class LuckyBlock extends LbcBlockModels.Cube {
        private final Kind kind;
        LuckyBlock(Properties properties,Kind kind){super(properties);this.kind=kind;}
        @Override public BlockState getPolymerBlockState(BlockState state,PacketContext context){return CLIENT_STATES[kind.ordinal()];}
        @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder params){return drops(kind,params);}
        @Override protected ItemStack getCloneItemStack(LevelReader level,BlockPos pos,BlockState state,boolean includeData){
            var game=SkyWars2.game();
            if(game!=null&&level instanceof net.minecraft.server.level.ServerLevel server){
                var result=game.lbc().rollTable(server,net.minecraft.world.phys.Vec3.atCenterOf(pos),null,"lbc:ported/"+suffix(kind));
                if(!result.isEmpty())return result.getFirst();
            }
            return new ItemStack(item(kind));
        }
    }
}
