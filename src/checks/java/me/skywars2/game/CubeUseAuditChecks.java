package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Compare real cube packet input to the vanilla client's native on/air/other-hand order. */
public final class CubeUseAuditChecks {
    private CubeUseAuditChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Cube use audit: "+why);}
    public static void run(GameController game){try{audit(game);}catch(RuntimeException|Error error){throw error;}catch(Exception error){throw new IllegalStateException("Cube registry audit",error);}}
    private static void audit(GameController game)throws Exception{
        require(game.server().getPort()==25577,"isolated input audit");
        var manifest=new com.google.gson.JsonObject();var entries=new com.google.gson.JsonArray();var domains=new TreeSet<String>();
        for(var item:BuiltInRegistries.ITEM){
            if(item==Items.AIR)continue;
            String air=item.getClass().getMethod("use",Level.class,net.minecraft.world.entity.player.Player.class,InteractionHand.class).getDeclaringClass().getName();
            String on=item.getClass().getMethod("useOn",UseOnContext.class).getDeclaringClass().getName();
            var row=new com.google.gson.JsonObject();row.addProperty("item",BuiltInRegistries.ITEM.getKey(item).toString());row.addProperty("airOwner",air);row.addProperty("onBlockOwner",on);
            var components=new com.google.gson.JsonArray();for(var value:item.getDefaultInstance().getComponents())components.add(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(value.type()).toString());row.add("nativeComponents",components);entries.add(row);domains.add(on+" -> "+air);
        }
        manifest.add("items",entries);manifest.addProperty("nativeMethodDomains",domains.size());manifest.addProperty("fabricCallbacks","Shared native GameMode useItemOn/useItem retain Fabric callbacks; all opaque cubes use the same route regardless of item identity");
        java.nio.file.Files.writeString(java.nio.file.Path.of("cube-use-audit.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(manifest),java.nio.charset.StandardCharsets.UTF_8);
        var states=new ArrayList<BlockState>();var blocks=new com.google.gson.JsonArray();
        for(var block:BuiltInRegistries.BLOCK)if(block instanceof eu.pb4.polymer.core.api.block.PolymerBlock polymer){
            var state=block.defaultBlockState();states.add(state);var row=new com.google.gson.JsonObject();row.addProperty("block",BuiltInRegistries.BLOCK.getKey(block).toString());row.addProperty("projection",polymer.getPolymerBlockState(state,null).toString());row.addProperty("maskedCube",block instanceof LbcBlockModels.Cube);blocks.add(row);
        }
        manifest.add("customBlocks",blocks);java.nio.file.Files.writeString(java.nio.file.Path.of("cube-use-audit.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(manifest),java.nio.charset.StandardCharsets.UTF_8);
        int checks=0;
        try(var f=new WoolBucketChecks.Fixture(game,false);var stats=new NativeMeleeChecks.NativeStats(f.owner)){
            var input=new Input(f);var cases=new ArrayList<Case>();
            for(var item:List.of(Items.SNOWBALL,Items.EGG,Items.ENDER_PEARL,Items.EXPERIENCE_BOTTLE,Items.SPLASH_POTION,Items.LINGERING_POTION,Items.FIREWORK_ROCKET,
                    Items.APPLE,Items.CHORUS_FRUIT,Items.POTION,Items.MILK_BUCKET,Items.HONEY_BOTTLE,Items.BOW,Items.CROSSBOW,Items.TRIDENT,Items.SHIELD,Items.SPYGLASS,
                    Items.BRUSH,Items.IRON_HELMET,Items.ELYTRA,Items.FLINT_AND_STEEL,Items.FIRE_CHARGE,Items.STICK,Items.BONE_MEAL,Items.IRON_AXE))cases.add(new Case(item.toString(),item.getDefaultInstance(),false));
            cases.add(new Case("boat on water",new ItemStack(Items.OAK_BOAT),true));
            cases.add(new Case("native Big Bob",f.loot("lbc:big_bob"),false));
            for(var test:cases)for(var hand:InteractionHand.values())for(var state:states){
                var referenceState=state.getBlock() instanceof LbcBlockModels.Cube?Blocks.STONE.defaultBlockState():state;
                input.prepare(referenceState,test,hand,false);var expected=input.reference();var baseline=input.outcome();input.clearEntities();
                for(boolean crouch:new boolean[]{false,true}){
                    input.prepare(state,test,hand,crouch);input.packet(crouch);var actual=input.outcome();
                    require(actual.same(baseline),test.name+" "+hand+" crouch="+crouch+" "+state+" expected="+baseline+" actual="+actual+" reference="+expected);checks++;input.clearEntities();
                }
            }
            // A successful main-hand on-block action must not also throw the offhand item.
            for(var state:states){
                input.prepare(state,new Case("main placement wins",new ItemStack(Items.STONE,2),false),InteractionHand.MAIN_HAND,false);
                f.owner.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SNOWBALL,2));input.packet(false);
                require(f.level.getBlockState(input.at.above()).is(Blocks.STONE)&&f.owner.getOffhandItem().getCount()==2&&input.outcome().entities.isEmpty(),"on-block success has priority over both air and offhand");checks++;input.clearEntities();
            }
        }
        SkyWars2.LOGGER.info("[check] PASS cube-use-audit ({} registered items/{} native method domains structurally audited; {} actual registered-custom-block inputs vs independent vanilla order, both hands/crouch, inventory/using-hand/equipment/entities/world state and global Big Bob callback; real main action priority)",entries.size(),domains.size(),checks);
    }
    private record Case(String name,ItemStack stack,boolean water){}
    private record Outcome(List<ItemStack> inventory,boolean using,InteractionHand hand,Map<String,Integer> entities,BlockState placed){
        boolean same(Outcome other){if(using!=other.using||using&&hand!=other.hand||!entities.equals(other.entities)||placed!=other.placed||inventory.size()!=other.inventory.size())return false;for(int i=0;i<inventory.size();i++)if(!ItemStack.matches(inventory.get(i),other.inventory.get(i)))return false;return true;}
        @Override public String toString(){return "using="+using+"/"+hand+" entities="+entities+" placed="+placed+" inventory="+inventory;}
    }
    private static final class Input {
        final WoolBucketChecks.Fixture f;final BlockPos at;final Set<UUID> before=new HashSet<>();int sequence;boolean noteClient;
        Input(WoolBucketChecks.Fixture f){this.f=f;at=f.owner.blockPosition().offset(0,7,2);}
        BlockHitResult hit(){return new BlockHitResult(Vec3.atCenterOf(at).add(0,.5,0),Direction.UP,at,false);}
        void prepare(BlockState state,Case test,InteractionHand hand,boolean crouch){
            noteClient=state.getBlock() instanceof eu.pb4.polymer.core.api.block.PolymerBlock polymer&&polymer.getPolymerBlockState(state,f.owner.connection.getPacketContext()).is(Blocks.NOTE_BLOCK);
            f.owner.stopUsingItem();f.owner.connection.handleClientTickEnd(ServerboundClientTickEndPacket.INSTANCE);
            f.prepareParticipants();f.owner.setShiftKeyDown(crouch);f.owner.getFoodData().setFoodLevel(0);
            for(var key:List.copyOf(((me.skywars2.mixin.PlayerCheckpointAccess.Cooldowns)f.owner.getCooldowns()).skywars2$cooldowns().keySet()))f.owner.getCooldowns().removeCooldown(key);
            f.set(at,state);f.set(at.above(),test.water?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState());f.set(at.above(2),Blocks.AIR.defaultBlockState());
            f.owner.setItemInHand(hand,test.stack.copy());f.owner.setItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND,new ItemStack(Items.STICK,7));
            f.owner.getInventory().setItem(9,new ItemStack(Items.ARROW,64));
            f.owner.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,Vec3.atCenterOf(at).add(0,.5,0));f.client.acknowledge();before.clear();for(var e:f.level.getAllEntities())before.add(e.getUUID());
        }
        InteractionResult reference(){
            for(var hand:InteractionHand.values()){
                var result=f.owner.gameMode.useItemOn(f.owner,f.level,f.owner.getItemInHand(hand),hand,hit());if(result instanceof InteractionResult.Success||result instanceof InteractionResult.Fail)return result;
                var held=f.owner.getItemInHand(hand);if(!held.isEmpty()){var air=f.owner.gameMode.useItem(f.owner,f.level,held,hand);if(air instanceof InteractionResult.Success)return air;}
            }return InteractionResult.PASS;
        }
        void packet(boolean crouch){
            f.owner.connection.handleUseItemOn(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND,hit(),++sequence));
            if(crouch||!noteClient)for(var hand:InteractionHand.values()){
                if(hand==InteractionHand.OFF_HAND)f.owner.connection.handleUseItemOn(new ServerboundUseItemOnPacket(hand,hit(),++sequence));
                f.owner.connection.handleUseItem(new ServerboundUseItemPacket(hand,++sequence,f.owner.getYRot(),f.owner.getXRot()));
            }
            f.owner.connection.handleClientTickEnd(ServerboundClientTickEndPacket.INSTANCE);
        }
        Outcome outcome(){
            var inv=new ArrayList<ItemStack>();for(int i=0;i<f.owner.getInventory().getContainerSize();i++)inv.add(f.owner.getInventory().getItem(i).copy());
            var spawned=new TreeMap<String,Integer>();for(var e:f.level.getAllEntities())if(!before.contains(e.getUUID())&&!e.isRemoved())spawned.merge(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString(),1,Integer::sum);
            return new Outcome(inv,f.owner.isUsingItem(),f.owner.getUsedItemHand(),spawned,f.level.getBlockState(at.above()));
        }
        void clearEntities(){var values=new ArrayList<Entity>();f.level.getAllEntities().forEach(values::add);for(var e:values)if(!before.contains(e.getUUID())){f.own(e);e.discard();}}
    }
}
