package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

/** Native-input components for the asynchronous map fixture; the caller owns exact restoration. */
public final class BobaChecks {
    private BobaChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Boba: "+why);}
    public static ItemStack token(int count){
        var stack=new ItemStack(Items.CARROT_ON_A_STICK,count);var data=new CompoundTag();
        data.putInt("magicwand",1);data.putInt("boba_eblan",1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
        return stack;
    }
    public static void geometry(){
        regionFrames();
        require(LbcBoba.matches(token(1))&&!LbcBoba.matches(new ItemStack(Items.CARROT_ON_A_STICK)),"raw flags required");
        var missing=token(1);var data=missing.get(DataComponents.CUSTOM_DATA).copyTag();data.remove("magicwand");
        missing.set(DataComponents.CUSTOM_DATA,CustomData.of(data));require(!LbcBoba.matches(missing),"real magicwand dispatcher flag required");
        for(double coordinate:new double[]{-16000,-15999.99,0,16000,16000.99}){
            var at=new Vec3(coordinate,77,coordinate);
            require(LbcBoba.origin(at).equals(new BlockPos(0,160,0)),"source integer threshold inclusive "+coordinate);
        }
        for(var at:new Vec3[]{new Vec3(-16000.001,77,0),new Vec3(16001,77,0),new Vec3(0,77,-16000.001),new Vec3(0,77,16001)})
            require(LbcBoba.origin(at).equals(BlockPos.containing(at)),"outside source threshold follows caster "+at);
    }
    private static void regionFrames(){
        var v=new Vec3(.3,-.2,.5);var turned=v;
        for(int i=0;i<4;i++)turned=MapRotation.rotateVector(turned);
        require(turned.equals(v),"four turns restore momentum exactly");
        var look=MapRotation.rotateLook(37,-24);
        require(Vec3.directionFromRotation(look[1],look[0]).distanceToSqr(MapRotation.rotateVector(Vec3.directionFromRotation(-24,37)))<1e-7,"rotated yaw/pitch follow look vector");
        java.nio.file.Path folder=null;
        try{
            folder=java.nio.file.Files.createTempDirectory("boba-region-check-");var file=folder.resolve("regions.csv");
            var field=new BobaRegionPhysics(file);var center=new BlockPos(0,160,0);
            for(int i=1;i<=4;i++){
                field.rotate(center);
                require(field.turns(new BlockPos(0,160,0))==(i&3),"persistent repeated regional turns "+i);
                require(field.turns(new BlockPos(160,160,0))==0&&field.turns(new BlockPos(0,320,0))==0,"outside half-open region remains native");
                require(new BobaRegionPhysics(file).capture().equals(field.capture()),"world restart retains region frame "+i);
            }
            field.rotate(center);var once=field.capture();field.rotate(new BlockPos(160,160,0));
            require(field.turns(new BlockPos(-159,100,0))==1&&field.turns(new BlockPos(200,100,0))==1,"overlapping cubes preserve both exterior frames");
            require(field.turns(new BlockPos(100,100,0))==2,"overlap composes map gravity");
            field.restore(once);require(field.turns(new BlockPos(200,100,0))==0&&field.turns(new BlockPos(100,100,0))==1,"rewind restores exact region ownership");
            field.clear();require(field.capture().isEmpty(),"map cleanup clears region physics");
        }catch(java.io.IOException e){throw new IllegalStateException("Boba region fixture",e);}
        finally{if(folder!=null)try{java.nio.file.Files.deleteIfExists(folder.resolve("regions.csv"));java.nio.file.Files.deleteIfExists(folder.resolve("regions.csv.tmp"));java.nio.file.Files.deleteIfExists(folder);}catch(java.io.IOException e){throw new IllegalStateException("Boba fixture cleanup",e);}}
    }
    /** Caller owns terrain rollback; run in the completed first-turn region. */
    static void nativeRegionPhysics(GameController game){
        var level=game.maps().level();var at=new BlockPos(32,240,32);
        require(game.maps().rotationPhysics().turns(at)==1,"first-turn native physics fixture");
        var original=new java.util.LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();
        for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){
            var p=at.offset(x,y,z);original.put(p,game.maps().snapshot(level,p));
            level.setBlock(p,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2|16);
        }
        try{
            var item=new net.minecraft.world.entity.item.ItemEntity(level,at.getX()+.5,at.getY()+.5,at.getZ()+.5,new ItemStack(Items.DIAMOND));
            item.setDeltaMovement(Vec3.ZERO);item.tick();
            require(item.getDeltaMovement().z<-.02&&Math.abs(item.getDeltaMovement().y)<1e-8,"native item gravity follows local DOWN");
            var cow=new net.minecraft.world.entity.animal.cow.Cow(net.minecraft.world.entity.EntityTypes.COW,level);
            cow.setNoAi(true);cow.setPos(Vec3.atCenterOf(at));cow.setDeltaMovement(Vec3.ZERO);cow.travel(Vec3.ZERO);
            require(cow.getDeltaMovement().z<-.02&&Math.abs(cow.getDeltaMovement().y)<1e-8,"native living air gravity follows local DOWN");
            level.setBlock(at.north(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2|16);
            item.setPos(at.getX()+.5,at.getY()+.5,at.getZ()+.4);item.setDeltaMovement(0,0,-.6);
            item.move(net.minecraft.world.entity.MoverType.SELF,item.getDeltaMovement());
            // Native boxes are anchored at local feet. After +X rotation, local +Y
            // extends from item.z toward +Z; width/2 is not a world-Z center offset.
            var contact=item.getBoundingBox();double wallFace=at.north().getZ()+1;
            String contactState="pos="+item.position()+" bbox="+contact+" wallFace="+wallFace
                    +" velocity="+item.getDeltaMovement()+" onGround="+item.onGround()
                    +" vertical="+item.verticalCollision+" below="+item.verticalCollisionBelow
                    +" horizontal="+item.horizontalCollision+" support="+item.getOnPos()
                    +" frame="+game.maps().rotationPhysics().turns(item.blockPosition());
            require(item.onGround()&&item.verticalCollision&&item.verticalCollisionBelow&&!item.horizontalCollision
                    &&Math.abs(contact.minZ-wallFace)<1e-7&&contact.minZ>=wallFace-1e-7
                    &&Math.abs(item.getZ()-contact.minZ)<1e-7&&item.getOnPos().equals(at.north()),
                    "regional ground collision prevents item penetrating support wall; "+contactState);
            for(var player:game.server().getPlayerList().getPlayers())require(BobaRegionPhysics.turns(player)==0,"player gravity remains native");
            level.setBlock(at.north(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2|16);
            level.setBlock(at,net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(),2|16);
            var fluid=level.getFluidState(at);((net.minecraft.world.level.material.FlowingFluid)fluid.getType()).tick(level,at,level.getBlockState(at),fluid);
            require(!level.getFluidState(at.north()).isEmpty()&&level.getFluidState(at.below()).isEmpty(),"native water falls toward regional DOWN, not overworld DOWN");
            item.discard();cow.discard();
        }finally{for(var e:original.entrySet())game.maps().restoreSnapshot(level,e.getKey(),e.getValue());}
    }
    /** Invoke while the asynchronous fixture already owns an accepted rotation, in one server callback. */
    public static void busyUses(GameController game,ServerPlayer owner){
        require(game.maps().rotationBusy(),"fixture must own an accepted job");
        require(game.config().gameMode<=2,"rotation rather than Bedwars branch");
        boolean offhand=game.config().offhandWands;game.config().offhandWands=true;
        try{
            for(var hand:InteractionHand.values()){
                owner.getInventory().clearContent();
                var clicked=token(3);owner.setItemInHand(hand,clicked);
                int before=count(owner);var velocity=new Vec3(.3125,-.1875,.4375);owner.setDeltaMovement(velocity);
                int mana=game.items().mana().score(owner,"mana").get();
                long visits=game.maps().rotationVisits(),writes=game.maps().rotationWrites();
                require(owner.gameMode.useItem(owner,owner.level(),clicked,hand).consumesAction(),"actual native busy use "+hand);
                require(count(owner)==before-1,"busy refusal still debits exactly one raw token "+hand);
                require(owner.getDeltaMovement().equals(velocity),"busy raw use preserves caster momentum "+hand);
                require(game.items().mana().score(owner,"mana").get()==mana,"source has no mana price "+hand);
                require(game.maps().rotationBusy()&&game.maps().rotationVisits()==visits&&game.maps().rotationWrites()==writes,
                        "busy use cannot replace or synchronously advance accepted job "+hand);
            }
            // Source /clear uses inventory order, even when a different hand dispatched the input.
            owner.getInventory().clearContent();var donor=token(2);owner.getInventory().setItem(0,donor);
            var clicked=token(3);owner.setItemInHand(InteractionHand.OFF_HAND,clicked);
            require(owner.gameMode.useItem(owner,owner.level(),clicked,InteractionHand.OFF_HAND).consumesAction(),"native offhand inventory-order use");
            require(donor.getCount()==1&&clicked.getCount()==3,"source inventory debit precedes clicked-hand stack");
        }finally{game.config().offhandWands=offhand;}
    }
    /** Admission failures must leave physical tokens intact even if a terrain rotation is busy. */
    public static void rejectedUses(GameController game,ServerPlayer owner){
        boolean offhand=game.config().offhandWands;boolean tagged=owner.entityTags().contains("nomagic_active");
        try{
            owner.getInventory().clearContent();var clicked=token(3);owner.setItemInHand(InteractionHand.MAIN_HAND,clicked);
            owner.entityTags().add("nomagic_active");owner.gameMode.useItem(owner,owner.level(),clicked,InteractionHand.MAIN_HAND);
            require(clicked.getCount()==3,"source nomagic_active rejects before debit");
            owner.entityTags().remove("nomagic_active");game.config().offhandWands=true;
            var field=new net.minecraft.world.entity.Marker(net.minecraft.world.entity.EntityTypes.MARKER,owner.level());field.setPos(owner.position());field.entityTags().add("stopper_magic");
            require(owner.level().addFreshEntity(field),"native Boba beacon");game.items().magic().loaded(field);
            try{for(var hand:InteractionHand.values()){
                owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setItemInHand(hand,clicked);
                owner.gameMode.useItem(owner,owner.level(),clicked,hand);require(clicked.getCount()==3,"user beacon rejects Boba before debit in "+hand);
            }}finally{game.items().magic().unloaded(field);field.discard();game.items().magic().flushCleansing();}
            var previous=owner.getInventory().getItem(8);owner.getInventory().setItem(8,new ItemStack(LbcMagic.MYSTICAL_ARTIFACT));game.items().magic().inventoryChanged(owner);game.items().magic().flushCleansing();
            try{for(var hand:InteractionHand.values()){
                owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setItemInHand(hand,clicked);
                owner.gameMode.useItem(owner,owner.level(),clicked,hand);require(clicked.getCount()==3,"actual artifact rejects Boba before debit in "+hand);
            }}finally{owner.getInventory().setItem(8,previous);game.items().magic().inventoryChanged(owner);game.items().magic().flushCleansing();}
            owner.entityTags().remove("nomagic_active");game.config().offhandWands=false;
            owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.OFF_HAND,clicked);
            owner.gameMode.useItem(owner,owner.level(),clicked,InteractionHand.OFF_HAND);
            require(clicked.getCount()==3,"disabled source offhand rejects before debit");
        }finally{game.config().offhandWands=offhand;if(tagged)owner.entityTags().add("nomagic_active");else owner.entityTags().remove("nomagic_active");}
    }
    /** Full caller Vanity image owns all spawned loot and restores the complete match after this component. */
    public static void bedwars(GameController game,ServerPlayer owner){
        var beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
        require(beta!=null&&beta!=owner&&beta.level()==owner.level(),"actual distinct TestBeta in source dimension");
        require(!game.maps().rotationBusy(),"Bedwars fixture starts without terrain work");
        var savedBeta=new GameChecks.WandThirdPlayerState(beta,game.items().mana());
        var board=game.server().getScoreboard();var oldTeam=beta.getTeam();
        int mode=game.config().gameMode;boolean offhand=game.config().offhandWands;
        try{
            if(oldTeam!=null)board.removePlayerFromTeam(beta.getScoreboardName(),oldTeam);
            beta.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            require(!owner.isAlliedTo(beta)&&game.items().wands().target(owner,beta),"native enemy/shared-role admission for TestBeta");
            game.config().offhandWands=true;
            for(int sourceMode:new int[]{3,4}){
                game.config().gameMode=sourceMode;
                for(var hand:InteractionHand.values()){
                    owner.getInventory().clearContent();owner.getInventory().setSelectedSlot(5);
                    var donor=token(2);owner.getInventory().setItem(0,donor);var clicked=token(3);owner.setItemInHand(hand,clicked);
                    var at=owner.position().add(0,0,4);beta.setPos(at);beta.setYRot(37);beta.setXRot(19);
                    var velocity=new Vec3(.28125,-.15625,.40625);beta.setDeltaMovement(velocity);
                    var casterVelocity=new Vec3(-.21875,.125,.34375);owner.setDeltaMovement(casterVelocity);
                    double angle=Math.toRadians(37);var expected=at.add(Math.cos(angle),0,Math.sin(angle));
                    var before=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<net.minecraft.world.entity.Entity,Boolean>());
                    beta.level().getAllEntities().forEach(before::add);
                    long visits=game.maps().rotationVisits(),writes=game.maps().rotationWrites();
                    require(owner.gameMode.useItem(owner,owner.level(),clicked,hand).consumesAction(),"actual native Bedwars mode"+sourceMode+" "+hand);
                    require(donor.getCount()==1&&clicked.getCount()==3&&count(owner)==4,"Bedwars exact one inventory-order debit mode"+sourceMode+" "+hand);
                    require(!game.maps().rotationBusy()&&game.maps().rotationVisits()==visits&&game.maps().rotationWrites()==writes,"Bedwars never creates/advances map job");
                    require(beta.position().distanceToSqr(expected)<1e-12&&Math.abs(beta.getYRot()-127)<1e-5&&beta.getXRot()==-90,"source local-left/yaw+90/upward pitch");
                    require(beta.getDeltaMovement().equals(velocity)&&owner.getDeltaMovement().equals(casterVelocity),"source retains target and caster momentum");
                    var drops=new java.util.ArrayList<net.minecraft.world.entity.item.ItemEntity>();
                    beta.level().getAllEntities().forEach(e->{if(e instanceof net.minecraft.world.entity.item.ItemEntity item&&!before.contains(e)
                            &&item.position().distanceToSqr(expected)<1e-10)drops.add(item);});
                    require(!drops.isEmpty(),"real native source loot actors at moved enemy");
                    int total=0;for(var drop:drops){
                        require(LbcFartus.data(drop.getItem()).getIntOr("govnoq",0)==1,"source govno tables rather than invented loot");
                        total+=drop.getItem().getCount();
                    }
                    require(total==94,"source govno_lot ten*3 plus govno_lot_2 sixty-four, actual="+total);
                }
            }
        }finally{
            game.config().gameMode=mode;game.config().offhandWands=offhand;
            if(beta.getTeam()!=null)board.removePlayerFromTeam(beta.getScoreboardName(),beta.getTeam());
            if(oldTeam!=null)board.addPlayerToTeam(beta.getScoreboardName(),oldTeam);
            savedBeta.restore(game);
        }
    }
    private static int count(ServerPlayer owner){
        return owner.getInventory().clearOrCountMatchingItems(s->!s.isEmpty()&&LbcFartus.data(s).getIntOr("boba_eblan",0)==1,
                true,0,owner.inventoryMenu.getCraftSlots());
    }
}
