package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Native projectile collisions, real entity-interact packets, and actual tracking ticks. */
public final class IceCrystalInputChecks {
    private IceCrystalInputChecks(){}
    private static Fixture running;
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException("Ice crystal input: "+detail);}
    private static void near(Vec3 actual,Vec3 expected,String detail){require(actual.distanceToSqr(expected)<1e-8,detail+" actual="+actual+" expected="+expected);}
    private static void flat(Packet<?> packet,List<Packet<?>> out){if(packet instanceof ClientboundBundlePacket bundle)for(var child:bundle.subPackets())flat(child,out);else out.add(packet);}
    private enum Use { BLOCK,SLAB_TOP,SLAB_BOTTOM,OFF_BLOCK,OFF_PACKET,BLOCK_PRIORITY,FOOD_PRIORITY,FIRE,FAIL_PRIORITY,WATER,PICKUP,SNOWBALL,FOOD_OFF,SHIELD,SHIELD_OFF,ARMOR_STAND,WAND,WAND_SHIELD,SHIFT_WAND }
    private record Input(Use use,boolean shift){}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final IceWandChecks.Fixture f;
        final List<Input> inputs=new ArrayList<>();
        RegenerationBeltChecks.ScheduledClient client;
        NativeMeleeChecks.NativeStats stats;
        LbcIceWand.Crystal crystal;
        LbcIceWand.IceShot flight;
        int ticks,lastTick,phase,face,when,start,caseIndex,flightFrames;
        int durationId,leftRotationId;
        Fixture(GameController game){this.game=game;f=new IceWandChecks.Fixture(game);}
        void init(){
            f.init();client=new RegenerationBeltChecks.ScheduledClient(game,f.owner);
            stats=new NativeMeleeChecks.NativeStats(f.owner,List.of(Items.STONE,Items.STONE_SLAB,Items.STICK,Items.APPLE,Items.FLINT_AND_STEEL,
                    Items.WATER_BUCKET,Items.BUCKET,Items.SNOWBALL,Items.SHIELD,Items.ARMOR_STAND));
            try{var field=Display.class.getDeclaredField("DATA_POS_ROT_INTERPOLATION_DURATION_ID");field.setAccessible(true);
                durationId=((net.minecraft.network.syncher.EntityDataAccessor<?>)field.get(null)).id();
                field=Display.class.getDeclaredField("DATA_LEFT_ROTATION_ID");field.setAccessible(true);leftRotationId=((net.minecraft.network.syncher.EntityDataAccessor<?>)field.get(null)).id();
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            for(var use:Use.values()){
                if(use==Use.WAND||use==Use.WAND_SHIELD)inputs.add(new Input(use,false));
                else if(use==Use.SHIFT_WAND)inputs.add(new Input(use,true));
                else for(boolean shift:new boolean[]{false,true})inputs.add(new Input(use,shift));
            }
            lastTick=game.server().getTickCount();
        }
        List<Packet<?>> packets(){var out=new ArrayList<Packet<?>>();for(var packet:client.packets().subList(start,client.packets().size()))flat(packet,out);return out;}
        void eye(ServerPlayer player,Vec3 eye,Vec3 direction){
            f.move(player,eye.add(0,-player.getEyeHeight(),0));player.setYRot((float)-Math.toDegrees(Math.atan2(direction.x,direction.z)));
            player.setXRot((float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance())));client.acknowledge();
        }
        void rear(Direction normal,float yaw,float pitch,org.joml.Quaternionfc local){
            // Vanilla DisplayRenderer FIXED uses rotationYXZ(-yaw,pitch,0). The authored mesh grows along local +Y.
            var front=new Vector3f(0,1,0).rotate(local).rotate(new Quaternionf().rotationYXZ((float)Math.toRadians(-yaw),(float)Math.toRadians(pitch),0));
            var expected=normal.getUnitVec3();require(front.x*expected.x+front.y*expected.y+front.z*expected.z>.99999,"mesh front points out of actual impacted face "+normal+" yaw="+yaw+" pitch="+pitch+" local="+local+" front="+front);
        }
        void fireFace(){
            f.clear();var support=f.origin.above(3);var normal=Direction.values()[face];
            for(var dir:Direction.values())for(int distance=1;distance<=4;distance++)f.block(support.relative(dir,distance),Blocks.AIR);
            f.block(support,Blocks.STONE);var outward=normal.getUnitVec3();
            eye(f.owner,Vec3.atCenterOf(support).add(outward.scale(2.5)),outward.reverse());
            var wand=f.loot("ice_wand");f.owner.setItemInHand(InteractionHand.MAIN_HAND,wand);start=client.packets().size();
            var shot=f.service.spawn(f.owner,InteractionHand.MAIN_HAND,wand);shot.setNoGravity(true);
            for(int frame=0;frame<4&&!shot.isRemoved();frame++)f.owner.level().tickNonPassenger(shot);
            require(shot.isRemoved()&&f.service.snapshot().size()==1,"real native block collision creates one crystal "+normal);
            crystal=f.service.snapshot().getFirst();near(crystal.center(),Vec3.atCenterOf(support).add(outward),"actual face-aligned crystal center "+normal);
            rear(normal,crystal.display.getYRot(),crystal.display.getXRot(),crystal.display.getEntityData().getNonDefaultValues().stream().filter(value->value.id()==leftRotationId).map(value->(org.joml.Quaternionfc)value.value()).findFirst().orElseGet(Quaternionf::new));
            require(f.owner.level().getBlockState(support.relative(normal)).is(Blocks.BARRIER),"native outside face barrier cell "+normal);
            when=game.server().getTickCount();phase=1;
        }
        void faceWire(){
            var normal=Direction.values()[face];var wire=packets();
            var spawn=wire.stream().filter(ClientboundAddEntityPacket.class::isInstance).map(ClientboundAddEntityPacket.class::cast)
                    .filter(packet->packet.getId()==crystal.display.getId()).findFirst();
            if(spawn.isEmpty()&&game.server().getTickCount()-when<5)return;
            require(spawn.isPresent(),"actual client receives display pairing "+normal);
            near(new Vec3(spawn.get().getX(),spawn.get().getY(),spawn.get().getZ()),crystal.center(),"wire crystal position "+normal);
            var local=wire.stream().filter(ClientboundSetEntityDataPacket.class::isInstance).map(ClientboundSetEntityDataPacket.class::cast)
                    .filter(packet->packet.id()==crystal.display.getId()).flatMap(packet->packet.packedItems().stream()).filter(value->value.id()==leftRotationId)
                    .map(value->(org.joml.Quaternionfc)value.value()).reduce((a,b)->b).orElseGet(Quaternionf::new);
            rear(normal,spawn.get().getYRot(),spawn.get().getXRot(),local);
            require(wire.stream().filter(ClientboundSetEntityDataPacket.class::isInstance).map(ClientboundSetEntityDataPacket.class::cast)
                    .anyMatch(packet->packet.id()==crystal.display.getId()&&packet.packedItems().stream().anyMatch(value->value.id()==durationId&&Integer.valueOf(1).equals(value.value()))),"one-tick interpolation is paired on actual wire "+normal);
            f.clear();if(++face<Direction.values().length)phase=0;else phase=2;
        }
        void fresh(Set<Entity> before){
            var after=new ArrayList<Entity>();f.owner.level().getAllEntities().forEach(entity->{if(!before.contains(entity)&&!(entity instanceof ServerPlayer))after.add(entity);});
            after.forEach(Entity::discard);
        }
        ItemStack hand(InteractionHand hand,Item item,int count){var stack=new ItemStack(item,count);f.owner.setItemInHand(hand,stack);return stack;}
        void input(Input input){
            f.owner.stopUsingItem();f.clear();f.owner.getInventory().clearContent();f.owner.setShiftKeyDown(input.shift);f.owner.getFoodData().setFoodLevel(17);
            var cell=f.origin.above();var dest=cell.north();f.block(dest,Blocks.AIR);f.block(dest.below(),Blocks.AIR);
            crystal=f.crystal(f.owner,cell);eye(f.owner,Vec3.atCenterOf(cell).add(0,0,-2.5),new Vec3(0,0,1));
            InteractionHand packetHand=InteractionHand.MAIN_HAND;ItemStack primary=ItemStack.EMPTY;Vec3 click=new Vec3(0,.6,0);
            switch(input.use){
                case BLOCK->{primary=hand(InteractionHand.MAIN_HAND,Items.STONE,2);}
                case SLAB_TOP,SLAB_BOTTOM->{primary=hand(InteractionHand.MAIN_HAND,Items.STONE_SLAB,2);click=new Vec3(0,input.use==Use.SLAB_TOP?.95:.25,0);
                    var target=crystal.hitbox.position().add(click);eye(f.owner,f.owner.getEyePosition(),target.subtract(f.owner.getEyePosition()));}
                case OFF_BLOCK->{primary=hand(InteractionHand.OFF_HAND,Items.STONE,2);}
                case OFF_PACKET->{hand(InteractionHand.MAIN_HAND,Items.SNOWBALL,2);primary=hand(InteractionHand.OFF_HAND,Items.STONE,2);packetHand=InteractionHand.OFF_HAND;}
                case BLOCK_PRIORITY->{primary=hand(InteractionHand.MAIN_HAND,Items.STONE,2);hand(InteractionHand.OFF_HAND,Items.APPLE,2);}
                case FOOD_PRIORITY->{primary=hand(InteractionHand.MAIN_HAND,Items.APPLE,2);hand(InteractionHand.OFF_HAND,Items.STONE,2);}
                case FIRE->{f.block(dest.below(),Blocks.STONE);primary=hand(InteractionHand.MAIN_HAND,Items.FLINT_AND_STEEL,1);}
                case FAIL_PRIORITY->{primary=hand(InteractionHand.MAIN_HAND,Items.FLINT_AND_STEEL,1);hand(InteractionHand.OFF_HAND,Items.SNOWBALL,2);
                    var hit=new BlockHitResult(Vec3.atCenterOf(cell).add(0,0,-.5),Direction.NORTH,cell,false);
                    require(f.owner.gameMode.useItemOn(f.owner,f.owner.level(),primary,InteractionHand.MAIN_HAND,hit)==InteractionResult.FAIL,"independent native flint no-support FAIL witness");}
                case WATER->primary=hand(InteractionHand.MAIN_HAND,Items.WATER_BUCKET,1);
                case PICKUP->{f.block(dest,Blocks.WATER);primary=hand(InteractionHand.MAIN_HAND,Items.BUCKET,1);}
                case SNOWBALL->primary=hand(InteractionHand.MAIN_HAND,Items.SNOWBALL,2);
                case FOOD_OFF->primary=hand(InteractionHand.OFF_HAND,Items.APPLE,2);
                case SHIELD->primary=hand(InteractionHand.MAIN_HAND,Items.SHIELD,1);
                case SHIELD_OFF->{hand(InteractionHand.MAIN_HAND,Items.STICK,1);primary=hand(InteractionHand.OFF_HAND,Items.SHIELD,1);}
                case ARMOR_STAND->primary=hand(InteractionHand.MAIN_HAND,Items.ARMOR_STAND,1);
                case WAND,SHIFT_WAND,WAND_SHIELD->{primary=f.loot("ice_wand");f.owner.setItemInHand(InteractionHand.MAIN_HAND,primary);game.items().mana().set(f.owner,100000);
                    if(input.use==Use.WAND_SHIELD)hand(InteractionHand.OFF_HAND,Items.SHIELD,1);}
            }
            if(input.use==Use.BLOCK){
                require(f.owner.gameMode.useItem(f.owner,f.owner.level(),primary,InteractionHand.MAIN_HAND)==InteractionResult.PASS
                        &&f.owner.level().getBlockState(dest).isAir(),"independent native air-only block use cannot place");
            }
            var before=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());f.owner.level().getAllEntities().forEach(before::add);
            start=client.packets().size();
            f.owner.connection.handleInteract(new ServerboundInteractPacket(crystal.hitbox.getId(),packetHand,click,input.shift));
            f.owner.connection.handleClientTickEnd(new ServerboundClientTickEndPacket());
            try{
                switch(input.use){
                    case BLOCK,OFF_BLOCK,OFF_PACKET,BLOCK_PRIORITY->{require(f.owner.level().getBlockState(dest).is(Blocks.STONE)&&primary.getCount()==1,"actual packet places exactly one block "+input);
                        require(!f.owner.isUsingItem(),"successful placement precedes offhand food "+input);
                        if(input.use==Use.OFF_PACKET)require(f.owner.getMainHandItem().getCount()==2,"explicit offhand packet never uses main snowball");}
                    case SLAB_TOP,SLAB_BOTTOM->require(f.owner.level().getBlockState(dest).is(Blocks.STONE_SLAB)&&primary.getCount()==1
                            &&f.owner.level().getBlockState(dest).getValue(net.minecraft.world.level.block.SlabBlock.TYPE)==(input.use==Use.SLAB_TOP?net.minecraft.world.level.block.state.properties.SlabType.TOP:net.minecraft.world.level.block.state.properties.SlabType.BOTTOM),"original native hit location chooses slab half "+input);
                    case FIRE->require(f.owner.level().getBlockState(dest).is(Blocks.FIRE)&&primary.getDamageValue()==1,"native item useOn ignition and one durability debit "+input);
                    case FAIL_PRIORITY->require(f.owner.getOffhandItem().getCount()==2&&primary.getDamageValue()==0,"block FAIL stops air and offhand fallthrough "+input);
                    case WATER->require(f.owner.level().getBlockState(dest).is(Blocks.WATER)&&f.owner.getMainHandItem().is(Items.BUCKET),"native bucket POV use and replacement "+input);
                    case PICKUP->require(f.owner.level().getBlockState(dest).isAir()&&f.owner.getMainHandItem().is(Items.WATER_BUCKET),"native bucket fluid pickup "+input);
                    case SNOWBALL->{require(primary.getCount()==1,"native thrown item single debit after client tick end "+input);
                        require(f.owner.level().getEntitiesOfClass(Snowball.class,crystal.hitbox.getBoundingBox().inflate(4),shot->!before.contains(shot)&&shot.getOwner()==f.owner).size()==1,"exactly one actual native thrown entity "+input);}
                    case FOOD_PRIORITY,FOOD_OFF,SHIELD,SHIELD_OFF->{var used=input.use==Use.FOOD_OFF||input.use==Use.SHIELD_OFF?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
                        require(f.owner.isUsingItem()&&f.owner.getUsedItemHand()==used,"real continuous use and actual hand "+input);
                        require(primary.getCount()==(input.use==Use.FOOD_OFF||input.use==Use.FOOD_PRIORITY?2:1),"continuous use has no early debit "+input);
                        require(f.owner.level().getBlockState(dest).isAir(),"main air use precedes offhand block "+input);}
                    case ARMOR_STAND->require(primary.isEmpty()&&f.owner.level().getEntities((Entity)null,crystal.hitbox.getBoundingBox().inflate(4),entity->!before.contains(entity)&&entity.getType()==EntityTypes.ARMOR_STAND).size()==1,"native entity-placement item useOn "+input);
                    case WAND,WAND_SHIELD->{require(f.service.shotSnapshot().size()==1&&game.items().mana().get(f.owner)==99600,"custom wand executes once through original dispatcher "+input);
                        if(input.use==Use.WAND_SHIELD)require(f.owner.isUsingItem()&&f.owner.getUsedItemHand()==InteractionHand.OFF_HAND,"one wand cast then native offhand shield");}
                    case SHIFT_WAND->require(crystal.removed&&f.service.shotSnapshot().isEmpty()&&game.items().mana().get(f.owner)==99800,"native crouching wand action shatters managed crystal once");
                }
            }finally{f.owner.stopUsingItem();f.clear();fresh(before);f.block(dest,Blocks.AIR);f.block(dest.below(),Blocks.AIR);}
        }
        void beginFlight(){
            f.clear();f.owner.getInventory().clearContent();f.owner.setShiftKeyDown(false);
            var begin=f.at.add(-5,6,0);for(int x=-7;x<=13;x++)for(int y=5;y<=7;y++)f.block(f.origin.offset(x,y,0),Blocks.AIR);
            eye(f.owner,begin,new Vec3(1,0,0));var wand=f.loot("ice_wand");f.owner.setItemInHand(InteractionHand.MAIN_HAND,wand);
            start=client.packets().size();flight=f.service.spawn(f.owner,InteractionHand.MAIN_HAND,wand);flight.setNoGravity(true);phase=3;
        }
        void flight(){
            require(!flight.isRemoved()&&!flight.display.isRemoved(),"native scheduled free flight remains resident");near(flight.display.position(),flight.position(),"native visual follows exact current flight body");
            if(++flightFrames>2){
                var wire=packets();boolean sent=wire.stream().anyMatch(packet->packet instanceof ClientboundEntityPositionSyncPacket moved&&moved.id()==flight.display.getId()&&moved.hasPosition()
                        ||packet instanceof ClientboundMoveEntityPacket relative&&relative.hasPosition()&&relative.getEntity(f.owner.level())==flight.display);
                require(sent,"native tracker transmits display position every active flight tick, frame="+flightFrames);
            }
            start=client.packets().size();
        }
        boolean step(){
            int now=game.server().getTickCount();if(now==lastTick)return false;lastTick=now;
            require(++ticks<180&&game.playing(),"bounded native fixture, phase="+phase);client.acknowledge();
            if(phase==0){fireFace();return false;}
            if(phase==1){faceWire();return false;}
            if(phase==2){if(caseIndex<inputs.size()){input(inputs.get(caseIndex++));return false;}beginFlight();return false;}
            flight();return flightFrames>=9;
        }
        @Override public void close(){try{if(client!=null)client.close();}finally{try{if(stats!=null)stats.close();}finally{f.close();}}}
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.init();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;int cases=running.inputs.size();running.close();running=null;
        SkyWars2.LOGGER.info("[check] PASS ice-crystal-inputs (six actual native impact faces and wire poses/interpolation, {} real RMB packets/both hands/crouch/block-air precedence/FAIL/buckets/food/shields/custom wand, scheduled per-tick display tracking; no physical-client FPS claim)",cases);
    }catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL ice-crystal-inputs",error);}}
}
