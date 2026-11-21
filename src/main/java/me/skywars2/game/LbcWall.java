package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashSet;

/** Мгновенные кирпичные формы Wall: без сущностей и постоянного tick-обработчика. */
final class LbcWall {
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final TagKey<Block> BRICKS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:bricks"));
    private static final BlockParticleOption PARTICLES=new BlockParticleOption(ParticleTypes.BLOCK,Blocks.BRICKS.defaultBlockState());
    static final Identifier STAIR=Identifier.parse("lbc:stair5x5x5");
    private LbcWall() { }

    /** Режим меняется у использованной руки; переключение не расходует ману и не запускает бонусы каста. */
    static boolean cycle(GameController game,ServerPlayer owner,ItemStack stack) {
        var mana=game.items().mana();
        if(!mana.visible(owner) || owner.entityTags().contains("time_stopped")
                || game.items().magic().suppressed(owner)) return false;
        // (user) Горизонт относится к переключению вперёд, без двух отменяющих друг друга вызовов.
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        int mode=Math.floorMod(data.getIntOr("mode",1)-1+LbcModeScroll.steps(owner.getXRot()>0?-1:1),4)+1;
        data.putInt("mode",mode); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
        owner.sendSystemMessage(Component.translatable(switch(mode) {
            case 2 -> "mode_horizontal_wall";
            case 3 -> "mode_break_wall";
            case 4 -> "mode_stair";
            default -> "mode_vertical_wall";
        }));
        return true;
    }

    /** Один native hit завершает луч; препятствие и сущность больше не исполняют форму независимо. */
    static void cast(GameController game,ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean upgraded) {
        int mode=upgraded?Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY)
                .copyTag().getIntOr("mode",1),1,4):1;
        cast(game,owner,hand,stack,upgraded,mode);
    }
    static void cast(GameController game,ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean upgraded,int mode){
        Vec3 from = owner.getEyePosition();
        Vec3 direction = Vec3.directionFromRotation(owner.getXRot(), owner.getYRot());
        Vec3 end = castAt(game, owner, from, direction, owner.getYRot(), upgraded, mode, true);
        // Meditation inspects the geometry after the accepted cast has placed its blocks.
        if (!upgraded) game.items().academy().wallTrial(owner, hand, stack);
        if (end != null && !owner.level().getEntitiesOfClass(ServerPlayer.class, new AABB(end, end).inflate(3),
                target -> game.items().wands().target(owner, target) && target.position().distanceToSqr(end) <= 9).isEmpty())
            game.items().mana().award(owner, "lbc:true_advancements/staffs/wall");
    }

    static void scriptCast(GameController game, ServerPlayer owner, Vec3 origin, Vec3 direction,
                           boolean upgraded, int mode) {
        float yaw = direction.horizontalDistanceSqr() < 1e-12 ? owner.getYRot()
                : (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        castAt(game, owner, origin, direction, yaw, upgraded, mode, false);
    }

    private static Vec3 castAt(GameController game, ServerPlayer owner, Vec3 from, Vec3 direction, float yaw,
                               boolean upgraded, int mode, boolean refundMana) {
        Vec3 fullEnd=from.add(direction.scale(upgraded?180:100));
        double suppression=game.items().magic().suppressionFraction(owner.level(),from,fullEnd);
        Vec3 limit=Double.isFinite(suppression)?from.lerp(fullEnd,suppression):fullEnd;
        var hit=game.items().wands().firstHit(owner,from,limit);
        Vec3 end=hit==null?limit:hit.getLocation();
        double distance=from.distanceTo(end),spacing=upgraded?.6:1;
        int samplesPerPacket = game.balanceLegacy() ? 1 : upgraded ? 3 : 2;
        try (var trail = new LbcParticleBatch.Group(owner.level(), PARTICLES, .5, .1, false,
                spacing + .01, samplesPerPacket)) {
            for (double d = 0; d <= distance; d += spacing)
                trail.add(from.add(direction.scale(d)), 2);
        }
        boolean impact=hit!=null && (!(hit instanceof BlockHitResult wall) || owner.level().hasChunkAt(wall.getBlockPos()))
                && (!Double.isFinite(suppression) || end.distanceToSqr(from)<limit.distanceToSqr(from)-1e-6);
        if (!impact) return null;
        // Native clip lies on the face. Nudge inward so the wall uses the hit block's plane.
        Vec3 placement = hit instanceof BlockHitResult ? end.add(direction.scale(1e-4)) : end;
        BlockPos center = hit instanceof BlockHitResult wall ? wall.getBlockPos() : BlockPos.containing(end);
        switch (mode) {
            case 1 -> vertical(game, owner, placement, yaw);
            case 2 -> horizontal(game, owner, end.add(direction.scale(.5)));
            case 3 -> dismantle(game, owner, center, refundMana);
            case 4 -> stairs(game, owner, center, yaw);
        }
        return end;
    }

    private static void vertical(GameController game,ServerPlayer owner,Vec3 point,float yaw) {
        Vec3 left=Vec3.directionFromRotation(0,yaw-90);
        var cells=new LinkedHashSet<BlockPos>();
        for(int x=-3;x<=3;x++) {
            BlockPos base=BlockPos.containing(point.add(left.scale(x)));
            for(int y=-2;y<=3;y++) cells.add(base.offset(0,y,0));
        }
        placeBricks(game,owner,cells);
    }

    private static void horizontal(GameController game,ServerPlayer owner,Vec3 point) {
        BlockPos center=BlockPos.containing(point);
        var cells=new LinkedHashSet<BlockPos>();
        for(var pos:BlockPos.betweenClosed(center.offset(-3,0,-3),center.offset(3,0,3))) cells.add(pos.immutable());
        placeBricks(game,owner,cells);
    }

    /** Исходный #airs включает жидкости, растения и варочную стойку. */
    private static void placeBricks(GameController game,ServerPlayer owner,Iterable<BlockPos> cells) {
        var level=owner.level();
        var bricks=Blocks.BRICKS.defaultBlockState();
        int flags=Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE
                | Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;
        game.items().indirectKills().trapChanges(owner,()-> {
            for(var pos:cells) {
                if(!level.hasChunkAt(pos)) continue;
                var old=level.getBlockState(pos);
                if(!old.is(AIRS)) continue;
                if(old.hasBlockEntity()) LbcDropBlocks.setBlockPreservingInventory(level,pos,bricks,flags);
                else level.setBlock(pos,bricks,flags);
            }
        });
        LbcOverload.sound(owner,"minecraft:block.stone.place",1,1,0);
    }

    private static void dismantle(GameController game, ServerPlayer owner, BlockPos center, boolean refundMana) {
        int[] removed={0};
        game.items().indirectKills().worldChanges(owner.level(),owner,()-> {
            for(var pos:BlockPos.betweenClosed(center.offset(-2,-2,-2),center.offset(2,2,2)))
                if(owner.level().hasChunkAt(pos) && owner.level().getBlockState(pos).is(BRICKS)
                        && owner.level().setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS
                            |Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS)) removed[0]++;
        });
        // Все виды #bricks возвращаются полными кирпичными блоками, как в исходнике; стопки валидны для клиента.
        for(int remaining=removed[0];remaining>0;) {
            int count=Math.min(remaining,Blocks.BRICKS.asItem().getDefaultMaxStackSize());
            Block.popResource(owner.level(),center,new ItemStack(Blocks.BRICKS,count));
            remaining-=count;
        }
        if (refundMana && removed[0] >= 15) game.items().mana().add(owner, 500);
        LbcOverload.sound(owner,"minecraft:block.stone.break",1,2,0);
    }

    private static void stairs(GameController game,ServerPlayer owner,BlockPos center,float yaw) {
        Direction facing=Direction.fromYRot(yaw);
        Rotation rotation;
        BlockPos origin;
        switch(facing) {
            case WEST -> { rotation=Rotation.CLOCKWISE_90; origin=center.offset(-1,1,-2); }
            case EAST -> { rotation=Rotation.COUNTERCLOCKWISE_90; origin=center.offset(1,1,2); }
            case NORTH -> { rotation=Rotation.CLOCKWISE_180; origin=center.offset(2,1,-1); }
            default -> { rotation=Rotation.NONE; origin=center.offset(-2,1,1); }
        }
        game.items().indirectKills().trapChanges(owner,
                ()->game.maps().placeLoadedTemplate(owner,STAIR,origin,1,rotation));
        LbcOverload.sound(owner,"minecraft:block.stone.place",1,1,0);
    }
    /** Shish Wall captures pose and facing at cast time, even if its long ray finishes later. */
    static void shishTerminal(GameController game,ServerPlayer owner,BlockHitResult hit,Vec3 direction,int mode,float yaw) {
        Vec3 point=hit.getLocation().add(direction.scale(1e-4));
        switch(mode) {
            case 2 -> horizontal(game,owner,point.add(direction.scale(.5)));
            case 3 -> dismantle(game,owner,hit.getBlockPos(),true);
            case 4 -> stairs(game,owner,hit.getBlockPos(),yaw);
            default -> vertical(game,owner,point,yaw);
        }
    }
}
