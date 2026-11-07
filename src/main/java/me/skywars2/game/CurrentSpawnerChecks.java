package me.skywars2.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.storage.TagValueOutput;

/** Both source spawner variants pass through the genuine loose-item dispatcher and shared BaseSpawner factory. */
public final class CurrentSpawnerChecks {
    private CurrentSpawnerChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Current lucky spawner: "+why);}
    private static ItemStack token(boolean old){var stack=new ItemStack(Items.CARROT_ON_A_STICK);var data=new CompoundTag();data.putInt("drop",1);data.putInt(old?"luckyspawner_old":"luckyspawner",1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return stack;}
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();
            for(boolean old:new boolean[]{false,true}){
                var at=BlockPos.containing(f.home.add(old?2:-2,1,4));f.set(at,Blocks.AIR.defaultBlockState());
                ItemStack source=token(old);
                if(!old){
                    var property=game.items().tmt().catalog().families().stream().flatMap(row->row.variants().stream())
                            .filter(a->((ItemData.IntReader)(Object)a.data()).skywars2$readInt("luckyspawner",0)==1).findFirst().orElseThrow();
                    source=LbcTmtItems.prototype(2);var root=source.get(DataComponents.CUSTOM_DATA).copyTag();
                    LbcTmtAbilities.write(root,List.of(property,new LbcTmtAbilities.Ability("fixture/retained",CustomData.of(new CompoundTag()))));source.set(DataComponents.CUSTOM_DATA,CustomData.of(root));
                }
                var item=new ItemEntity(f.level,at.getX()+.5,at.getY(),at.getZ()+.5,source);item.setThrower(f.owner);f.own(item);game.items().markCurrentItemEpoch(item);
                require(f.level.addFreshEntity(item),"actual token publication");
                require(game.items().specialDrops().spawn(item),"actual loose-item dispatcher "+old);
                require(item.isRemoved()&&f.level.getBlockEntity(at) instanceof SpawnerBlockEntity,"one real token debit/native spawner");
                var spawner=(SpawnerBlockEntity)f.level.getBlockEntity(at);var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,f.level.registryAccess());spawner.getSpawner().save(output);var data=output.buildResult();
                require(data.getIntOr("SpawnCount",-1)==1&&data.getIntOr("SpawnRange",-1)==(old?3:5)&&data.getIntOr("Delay",-1)==0,"native population/range/initial delay");
                require(data.getIntOr("MinSpawnDelay",-1)==(old?100:80)&&data.getIntOr("MaxSpawnDelay",-1)==(old?101:100)&&data.getIntOr("RequiredPlayerRange",-1)==(old?100:50),"source-specific native clocks/player range");
                var entity=data.getCompoundOrEmpty("SpawnData").getCompoundOrEmpty("entity");require(entity.getStringOr("id","").equals("minecraft:marker")&&entity.getListOrEmpty("Tags").toString().contains("futureluckyblock"),"shared native future-lucky producer");
                f.set(at,Blocks.AIR.defaultBlockState());
            }
        }
        me.skywars2.SkyWars2.LOGGER.info("[check] PASS current-lucky-spawner (actual mixed TMT/current and ordinary old token dispatch, native BaseSpawner variants/source producer and one debit)");
    }
}
