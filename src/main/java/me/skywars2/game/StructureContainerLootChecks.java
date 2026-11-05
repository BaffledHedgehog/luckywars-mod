package me.skywars2.game;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;

/** Source wrappers must be registered as well as present in the source tree; native containers unpack once. */
public final class StructureContainerLootChecks {
    private static final List<String> RESTORED=List.of("27_acessories","27_bows","27_grimoires","27_instruments","27_potions","27_random_items","27_rofls","27_shooters","27_slabs","27_staffs","27_summons","27_swords","6_sets","neko_set_overload_full","neko_set_traveller_full","random_grimoire");
    private StructureContainerLootChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Structure loot: "+why);}
    private static ResourceKey<LootTable> key(String id){return ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse(id));}
    private static void templates(GameController game){
        int containers=0;
        for(var id:List.of("grimoire_dungeon/room_1","grimoire_dungeon/room_2","item_gallery","sob")){
            var template=game.server().getStructureTemplateManager().get(Identifier.parse("lbc:"+id)).orElseThrow();var data=template.save(new CompoundTag());
            for(var raw:data.getListOrEmpty("blocks")){
                var block=(CompoundTag)raw;var nbt=block.getCompoundOrEmpty("nbt");var table=nbt.getStringOr("LootTable",nbt.getStringOr("loot_table",""));
                if(table.isEmpty())continue;containers++;
                require(game.server().reloadableRegistries().getLootTable(key(table))!=LootTable.EMPTY,"native template container resolves "+id+" -> "+table);
            }
        }
        require(containers==795,"all four source container structures retain795 exact table links, got="+containers);
        for(var kind:LbcLuckyBlocks.Kind.values())require(LbcLuckyBlocks.kind(LbcLuckyBlocks.legacyState(kind))==null,"ordinary source mushroom block remains vanilla; no map conversion");
    }
    private static void wrappers(GameController game,net.minecraft.server.level.ServerPlayer player){
        var params=new LootParams.Builder(player.level()).withParameter(LootContextParams.ORIGIN,player.position()).withOptionalParameter(LootContextParams.THIS_ENTITY,player).create(LootContextParamSets.CHEST);
        var ops=game.server().reloadableRegistries().lookup().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        for(var id:RESTORED){
            var table=game.server().reloadableRegistries().getLootTable(key("lbc:"+id));require(table!=LootTable.EMPTY,"registered complete internal source wrapper "+id);
            try(var input=StructureContainerLootChecks.class.getResourceAsStream("/data/lbc/loot_table/"+id+".json")){
                var raw=JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(input),StandardCharsets.UTF_8));var expected=LootTable.DIRECT_CODEC.parse(ops,raw).getOrThrow();
                var actual=table.getRandomItems(params,98761);var original=expected.getRandomItems(params,98761);
                require(actual.size()==original.size(),"source roll count/stack splitting retained "+id);
                for(int i=0;i<actual.size();i++)require(ItemStack.matches(actual.get(i),original.get(i)),"all source functions/counts/weights/conditions match native codec "+id+" output"+i);
                require(!actual.isEmpty(),"real source wrapper produces rewards "+id);
                if(List.of("27_acessories","27_potions","27_rofls").contains(id))
                    require(actual.stream().allMatch(stack->stack.getCount()==64&&stack.getMaxStackSize()==64&&!stack.has(DataComponents.MAX_DAMAGE)),"native64 count/max-stack/durability removal modifiers actually run "+id);
                if(id.equals("27_slabs"))require(actual.size()>=27&&actual.stream().allMatch(stack->stack.getCount()==64),"every nested slab reward applies64 count while retaining its own variable rolls");
                if(id.equals("27_grimoires"))require(actual.stream().mapToInt(ItemStack::getCount).sum()==27*99,"native27 grimoire rolls really apply99 count before splitting");
                if(id.equals("random_grimoire")){
                    boolean multiple=false;
                    for(long seed=1;seed<=16;seed++){int total=table.getRandomItems(params,seed).stream().mapToInt(ItemStack::getCount).sum();require(total>=1&&total<=6,"source grimoire uniform1..6 count retained");multiple|=total>1;}
                    require(multiple,"actual source count modifier is not silently ignored");
                }
            }catch(java.io.IOException error){throw new IllegalStateException(id,error);}
        }
    }
    private static void nativeContainer(GameController game,net.minecraft.server.level.ServerPlayer player){
        var level=player.level();var pos=player.blockPosition().above(4);require(level.hasChunkAt(pos),"resident native container control");var saved=game.maps().snapshot(level,pos);
        try{
            level.setBlock(pos,Blocks.BARREL.defaultBlockState(),2|16|32);var barrel=(BarrelBlockEntity)level.getBlockEntity(pos);require(barrel!=null,"real native barrel");
            barrel.setLootTable(key("lbc:random_grimoire"),98761);require(barrel.getLootTable()!=null,"source lazy table installed before interaction");
            barrel.unpackLootTable(player);require(barrel.getLootTable()==null,"native loot is generated only once");
            var before=new ArrayList<ItemStack>();for(int i=0;i<barrel.getContainerSize();i++)before.add(barrel.getItem(i).copy());
            require(before.stream().anyMatch(stack->!stack.isEmpty()),"source structure grimoire chest is no longer empty");
            barrel.unpackLootTable(player);for(int i=0;i<before.size();i++)require(ItemStack.matches(before.get(i),barrel.getItem(i)),"second access never rerolls native container contents");
            barrel.clearContent();
        }finally{if(level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel){barrel.setLootTable(null);barrel.clearContent();}game.maps().restoreSnapshot(level,pos,saved);}
    }
    public static void run(GameController game){
        require(game.playing(),"actual active match");var player=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
        var saved=new RegenerationBeltChecks.Snapshot(game,player);var attribution=new SauvojenMeditationChecks.Attribution(game,List.of(player));
        try{templates(game);wrappers(game,player);nativeContainer(game,player);}
        finally{saved.restoreAttributes();saved.restore(game);saved.restoreUse();attribution.close();}
        SkyWars2.LOGGER.info("[check] PASS structure-container-loot (795 native template links,16 restored internal wrappers/source codec/counts/properties, actual one-shot lazy barrel, no structure/map edits)");
    }
}
