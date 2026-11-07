package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import me.skywars2.mixin.BedwarsProtectionMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Qualification helpers run by the real mode3/local-map fixture; never installs a replacement policy. */
public final class BedwarsProtectionChecks {
    private BedwarsProtectionChecks(){}
    /** All original cells must come from the actual placed/captured local map; added/source/piston are original AIR. */
    public record Cells(BlockPos stone,BlockPos chest,BlockPos sand,BlockPos tnt,BlockPos added,BlockPos copySource,BlockPos piston){}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("BedWars protection: "+why);}
    private static boolean same(MapManager.BlockSnapshot a,MapManager.BlockSnapshot b){return a.state()==b.state()&&Objects.equals(a.entity(),b.entity());}
    private static Set<UUID> entities(ServerLevel level){var out=new HashSet<UUID>();level.getAllEntities().forEach(e->out.add(e.getUUID()));return out;}
    private static List<Entity> newEffects(ServerLevel level,Set<UUID> before){var out=new ArrayList<Entity>();level.getAllEntities().forEach(e->{if(!e.isRemoved()&&!before.contains(e.getUUID())&&(e instanceof ItemEntity||e instanceof ExperienceOrb||e instanceof PrimedTnt||e instanceof FallingBlockEntity))out.add(e);});return out;}
    private static String xyz(BlockPos pos){return pos.getX()+" "+pos.getY()+" "+pos.getZ();}
    private static int clone(GameController game,ServerPlayer player,BlockPos from,BlockPos to,String mode){
        var source=game.server().createCommandSourceStack().withLevel(player.level()).withEntity(player).withPosition(player.position())
                .withPermission(net.minecraft.server.permissions.PermissionSet.ALL_PERMISSIONS).withSuppressedOutput();
        try{return game.server().getCommands().getDispatcher().execute("clone "+xyz(from)+" "+xyz(from)+" "+xyz(to)+" replace "+mode,source);}
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){return 0;}
    }
    public static void runNative(GameController game,ServerPlayer player,Cells cells){
        var own=game.bedwars().arena().topology().beds().stream().filter(b->b.team()==game.bedwars().team(player)).findFirst().orElseThrow();
        var ownState=player.level().getBlockState(own.head());boolean directOnly=game.config().bedwarsDirectBedBreaking;
        try{
            require(player.level().setBlock(own.head(),ownState.setValue(net.minecraft.world.level.block.BedBlock.OCCUPIED,!ownState.getValue(net.minecraft.world.level.block.BedBlock.OCCUPIED)),2)
                    &&game.bedwars().hasBed(player),"native bed occupancy change retains its team's respawn right");
            game.config().bedwarsDirectBedBreaking=true;require(!game.bedwars().arena().allowsRemoval(player.level(),own.head()),"direct-only setting rejects indirect bed removal");
            game.config().bedwarsDirectBedBreaking=false;require(game.bedwars().arena().allowsRemoval(player.level(),own.head()),"permissive setting admits indirect removal through the common terrain boundary");
        }finally{game.config().bedwarsDirectBedBreaking=directOnly;player.level().setBlock(own.head(),ownState,2);}
        var arena=game.bedwars().arena();var level=player.level();require(game.bedwars().mode()&&game.playing()&&arena.active(),"actual initialized BedWars3/4 owner");
        require(arena.topology().level()==level,"same actual arena realm");
        for(var pos:List.of(cells.stone,cells.chest,cells.sand,cells.tnt))require(arena.originalTypeAt(level,pos)==level.getBlockState(pos).getBlock(),"fixture original type was captured at actual map placement "+pos);
        require(level.getBlockState(cells.stone).is(Blocks.STONE)&&level.getBlockState(cells.chest).is(Blocks.CHEST)&&level.getBlockState(cells.sand).is(Blocks.SAND)&&level.getBlockState(cells.tnt).is(Blocks.TNT),"exact native original fixture block types");
        for(var pos:List.of(cells.added,cells.copySource,cells.piston))require(arena.originalTypeAt(level,pos)==null&&level.getBlockState(pos).isAir(),"fixture additions start at originally-air coordinates");
        require(cells.piston.east().equals(cells.stone),"native piston footprint pushes the protected stone east");
        var chest=game.maps().snapshot(level,cells.chest);var before=entities(level);var held=player.getMainHandItem().copy();
        require(level.getBlockEntity(cells.chest) instanceof net.minecraft.world.Container container&&!container.isEmpty(),"protected original chest has real inventory to detect rejected-clear loss");
        try{
            require(!level.setBlockAndUpdate(cells.chest,Blocks.AIR.defaultBlockState()),"direct native setBlockAndUpdate rejects original type change");
            require(!level.destroyBlock(cells.chest,true,player,512),"native destroyBlock rejects BEFORE inventory/drop/XP callbacks");
            require(!player.gameMode.destroyBlock(cells.chest),"native player break rejects before playerWillDestroy/tool/drop effects");
            game.maps().restoreSnapshot(level,cells.chest,MapManager.BlockSnapshot.air());
            require(same(chest,game.maps().snapshot(level,cells.chest)),"denied typed snapshot never clears the original inventory or NBT");
            var falling=FallingBlockEntity.fall(level,cells.sand,level.getBlockState(cells.sand));
            require(falling.isRemoved()&&level.getBlockState(cells.sand).is(Blocks.SAND),"denied falling source publishes no duplicate body");
            require(!net.minecraft.world.level.block.TntBlock.prime(level,cells.tnt,player,ItemStack.EMPTY),"protected TNT cannot publish fuse entity/sound branch");
            ((BedwarsProtectionMixin.FireAccess)Blocks.FIRE).skywars2$burn(level,cells.tnt,1,net.minecraft.util.RandomSource.create(91),0);
            require(level.getBlockState(cells.tnt).is(Blocks.TNT),"native fire transaction cannot prime a retained protected TNT");
            ((BedwarsProtectionMixin.FluidAccess)Fluids.WATER).skywars2$spread(level,cells.sand,level.getBlockState(cells.sand),Direction.DOWN,Fluids.WATER.defaultFluidState());
            require(same(chest,game.maps().snapshot(level,cells.chest))&&level.getBlockState(cells.sand).is(Blocks.SAND),"native fluid displacement has no retained-source/drop side effects");
            var explosion=new ServerExplosion(level,null,null,null,Vec3.atCenterOf(cells.chest),2,false,Explosion.BlockInteraction.DESTROY);
            ((NuclearTerrain.BlockEffects)explosion).skywars2$applyNuclearBlocks(new ArrayList<>(List.of(cells.chest,cells.tnt,cells.stone)));
            require(same(chest,game.maps().snapshot(level,cells.chest))&&level.getBlockState(cells.tnt).is(Blocks.TNT)&&level.getBlockState(cells.stone).is(Blocks.STONE),"actual native explosion per-block transaction excludes protected loot and TNT reactions");
            require(newEffects(level,before).isEmpty(),"all denied native operations publish zero items, XP, falling or TNT bodies");
            require(level.setBlockAndUpdate(cells.piston,Blocks.PISTON.defaultBlockState().setValue(net.minecraft.world.level.block.piston.PistonBaseBlock.FACING,Direction.EAST)),"ordinary unprotected piston addition");
            require(!((BedwarsProtectionMixin.PistonAccess)Blocks.PISTON).skywars2$move(level,cells.piston,Direction.EAST,true),"native resolved piston move rejects protected source as one transaction");
            require(level.getBlockState(cells.stone).is(Blocks.STONE)&&level.getBlockEntity(cells.stone)==null&&level.getBlockEntity(cells.stone.east())==null&&newEffects(level,before).isEmpty(),"denied piston creates no moving BE, changed original or loot");
            require(level.setBlockAndUpdate(cells.copySource,Blocks.CHEST.defaultBlockState()),"unprotected real copy-source chest");
            var source=(net.minecraft.world.Container)level.getBlockEntity(cells.copySource);source.setItem(0,new ItemStack(Items.EMERALD,17));
            var copy=game.maps().snapshot(level,cells.copySource);var stone=game.maps().snapshot(level,cells.stone);
            require(clone(game,player,cells.copySource,cells.stone,"force")==0&&same(stone,game.maps().snapshot(level,cells.stone)),"real clone command rejects protected unlike-type target before barriers/NBT");
            require(clone(game,player,cells.chest,cells.added,"move")==0&&same(chest,game.maps().snapshot(level,cells.chest))&&level.getBlockState(cells.added).isAir(),"real MOVE command rejects before clearing protected source or publishing destination");
            var template=new StructureTemplate();template.fillFromWorld(level,cells.copySource,new Vec3i(1,1,1),false,List.of());
            var settings=new StructurePlaceSettings().setIgnoreEntities(true);
            require(!template.placeInWorld(level,cells.stone,cells.stone,settings,net.minecraft.util.RandomSource.create(3),2)&&same(stone,game.maps().snapshot(level,cells.stone)),"actual native template rejects unlike protected type before any cell publication");
            require(template.placeInWorld(level,cells.chest,cells.chest,settings,net.minecraft.util.RandomSource.create(3),2),"source type-only policy permits SAME-type native template/NBT replacement");
            require(((net.minecraft.world.Container)level.getBlockEntity(cells.chest)).getItem(0).getCount()==17,"same-type template really transfers source inventory NBT");
            game.maps().restoreSnapshot(level,cells.chest,chest);
            require(same(chest,game.maps().snapshot(level,cells.chest)),"same-state typed native restore really reinstalls original BE image");
            require(clone(game,player,cells.copySource,cells.chest,"force")>0&&((net.minecraft.world.Container)level.getBlockEntity(cells.chest)).getItem(0).is(Items.EMERALD),"same-type native clone command retains source NBT semantics");
            game.maps().restoreSnapshot(level,cells.chest,chest);
            require(same(copy,game.maps().snapshot(level,cells.copySource)),"copies leave actual source chest unchanged");
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STONE,2));
            var hit=new BlockHitResult(Vec3.atCenterOf(cells.added.below()),Direction.UP,cells.added.below(),false);
            require(player.gameMode.useItemOn(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND,hit).consumesAction()&&level.getBlockState(cells.added).is(Blocks.STONE),"actual native player placement works at originally-air cell");
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));
            require(player.gameMode.destroyBlock(cells.added)&&level.getBlockState(cells.added).isAir(),"actual native player breaks its unprotected addition");
            require(newEffects(level,before).stream().anyMatch(ItemEntity.class::isInstance),"unprotected native player break really publishes loot positive control");
            SkyWars2.LOGGER.info("[check] PASS bedwars-protection-native (real active mode3 placement baseline; typed snapshot inventory integrity; native player/Level destruction, falling, TNT/fire/fluid, explosion, piston, template+clone/MOVE rejection; same-type NBT; ordinary placement and loot positive control)");
        }finally{
            player.setItemInHand(InteractionHand.MAIN_HAND,held);
            game.maps().restoreSnapshot(level,cells.chest,chest);
            for(var pos:List.of(cells.added,cells.copySource,cells.piston)){if(level.getBlockEntity(pos) instanceof net.minecraft.world.Clearable clear)clear.clearContent();level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);}
            newEffects(level,before).forEach(Entity::discard);
        }
    }
    public static void ownBed(GameController game,ServerPlayer owner,LbcBedwarsArena.Bed bed){
        var level=owner.level();var foot=game.maps().snapshot(level,bed.foot());var head=game.maps().snapshot(level,bed.head());
        require(!owner.gameMode.destroyBlock(bed.foot())&&!owner.gameMode.destroyBlock(bed.head()),"actual owner cannot break either native bed half");
        require(same(foot,game.maps().snapshot(level,bed.foot()))&&same(head,game.maps().snapshot(level,bed.head())),"own bed denied without partial native half removal");
    }
    /** Root captures/rewinds its real mode3 fixture around this irreversible gameplay transaction. */
    public static void enemyBed(GameController game,ServerPlayer enemy,LbcBedwarsArena.Bed bed){
        var level=enemy.level();var before=entities(level);var beacon=bed.spawn().below(2);require(level.getBlockState(beacon).is(Blocks.BEACON),"actual owned spawn beacon before bed break");
        require(enemy.gameMode.destroyBlock(bed.head())&&level.getBlockState(bed.foot()).isAir()&&level.getBlockState(bed.head()).isAir(),"actual enemy break atomically removes both registered native halves");
        require(level.getBlockState(beacon).isAir()&&game.bedwars().arena().originalTypeAt(level,beacon)==null,"owned spawn beacon removal retires its exact protection cell");
        require(game.bedwars().arena().originalTypeAt(level,bed.foot())==null&&game.bedwars().arena().originalTypeAt(level,bed.head())==null,"both removed bed coordinates retire protection exactly once");
        require(newEffects(level,before).isEmpty(),"manual source bed transaction emits no bed/beacon inventory or loot bodies");
    }
}
