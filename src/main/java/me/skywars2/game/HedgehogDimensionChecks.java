package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Real native dimension transfers and source destination/registration checks. */
public final class HedgehogDimensionChecks {
    private HedgehogDimensionChecks(){}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException("Hedgehog dimensions: "+message);}
    private static void close(double actual,double expected,String message){require(Math.abs(actual-expected)<.00001,message+": "+actual+" expected "+expected);}
    public static void run(GameController game){
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        var victim=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
        require(owner!=null&&victim!=null&&game.allowDamage(owner)&&game.allowDamage(victim),"requires active Alpha/Beta participants");
        var dimensions=game.items().hedgehogDimensions();var arena=game.maps().level();
        require(arena==game.server().overworld()&&arena.dimension()==Level.OVERWORLD,"actual legacy Overworld map, no dimension alias");
        var nether=game.server().getLevel(Level.NETHER);var end=game.server().getLevel(Level.END);
        var nexus=game.server().getLevel(LbcHedgehogDimensions.NEXUS);var imprinted=game.server().getLevel(LbcHedgehogDimensions.IMPRINTED);
        require(nether!=null&&end!=null&&nexus!=null&&imprinted!=null,"all five authored destination roles have real loaded worlds");
        require(nexus.dimensionType().minY()==0&&nexus.dimensionType().height()==2032
                &&imprinted.dimensionType().minY()==-16&&imprinted.dimensionType().height()==336,"source world vertical ranges preserved");
        require(Level.isInSpawnableBounds(new BlockPos(0,2332,0)),"source Nexus RTP2332 is legal above build height");
        require(dimensions.destination(arena,1)==nether&&dimensions.destination(arena,2)==end
                &&dimensions.destination(arena,3)==nexus&&dimensions.destination(arena,4)==imprinted,"overworld role exact four destinations");
        require(dimensions.destination(nether,1)==arena&&dimensions.destination(nether,2)==end
                &&dimensions.destination(nether,3)==nexus&&dimensions.destination(nether,4)==imprinted,"Nether exact four destinations");
        require(dimensions.destination(end,1)==arena&&dimensions.destination(end,2)==nether
                &&dimensions.destination(end,3)==nexus&&dimensions.destination(end,4)==imprinted,"End exact four destinations");
        require(dimensions.destination(imprinted,1)==nether&&dimensions.destination(imprinted,2)==end
                &&dimensions.destination(imprinted,3)==nexus&&dimensions.destination(imprinted,4)==arena,"Imprinted exact four destinations");
        for(int roll=1;roll<=4;roll++)require(dimensions.destination(nexus,roll)==null,"Nexus source no-op retained");
        var saved=new GameChecks.WandThirdPlayerState(victim,game.items().mana());
        var originalLevel=victim.level();
        var initial=owner.position().add(24,3,24);float yaw=37,pitch=-19;
        Cow cow=null;net.minecraft.world.entity.LivingEntity cowArrival=null;java.util.UUID cowId=null;
        try{
            victim.teleportTo(arena,initial.x,initial.y,initial.z,java.util.Set.of(),yaw,pitch,true);
            victim.setDeltaMovement(new Vec3(.2,-.7,.3));
            require(dimensions.teleport(victim,owner,1)&&victim.level()==nether,"actual player Nether transfer");
            close(victim.getX(),initial.x/8,"execute-in Nether xscale");close(victim.getZ(),initial.z/8,"execute-in Nether zscale");
            close(victim.getY(),initial.y,"ordinary branch retains y");close(victim.getYRot(),yaw,"retains yaw");close(victim.getXRot(),pitch,"retains pitch");
            close(victim.getDeltaMovement().y,-.7,"dimension changes preserve authored incoming velocity");
            require(game.allowDamage(victim)&&game.canUseGameplayItems(victim)&&game.alivePlayers().contains(victim),"crossworld participant retains life accounting and gameplay eligibility");
            require(dimensions.teleport(victim,owner,1)&&victim.level()==arena,"actual Nether to logicaloverworld arena return");
            close(victim.getX(),initial.x,"roundtrip coordinate scaling");
            require(dimensions.teleport(victim,owner,2)&&victim.level()==end,"actual End transfer");
            require(dimensions.teleport(victim,owner,4)&&victim.level()==imprinted,"actual Imprinted transfer");
            var chunk=imprinted.getChunkAt(victim.blockPosition());
            require(chunk.getBlockState(new BlockPos(victim.getBlockX(),-16,victim.getBlockZ())).is(Blocks.BEDROCK)
                    &&chunk.getBlockState(new BlockPos(victim.getBlockX(),-12,victim.getBlockZ())).isAir(),"actual Imprinted source flat layers generated");
            require(dimensions.teleport(victim,owner,4)&&victim.level()==arena,"actual Imprinted to arena return");
            victim.entityTags().add("strict_map_object");
            try{require(!dimensions.teleport(victim,owner,1)&&victim.level()==arena,"protected role cannot be moved");}
            finally{victim.entityTags().remove("strict_map_object");}
            var gravity=victim.getAttribute(Attributes.GRAVITY);var anti=Identifier.parse("minecraft:antigravity_effect");
            gravity.addTransientModifier(new AttributeModifier(anti,-.5,AttributeModifier.Operation.ADD_VALUE));
            try{
                require(dimensions.teleport(victim,owner,3)&&victim.level()==nexus,"actual Nexus RTP transfer");
                require(victim.getY()==2332&&victim.getX()>=-4800&&victim.getX()<=4800
                        &&victim.getZ()>=-4800&&victim.getZ()<=4800&&victim.getX()==Math.rint(victim.getX())
                        &&victim.getZ()==Math.rint(victim.getZ()),"Nexus exact integer RTP bounds and height");
                require(!gravity.hasModifier(anti),"Nexus RTP removes only authored antigravity modifier");
                var point=victim.position();require(!dimensions.teleport(victim,owner,1)&&victim.level()==nexus&&victim.position().equals(point),"actual Nexus no-op");
            }finally{gravity.removeModifier(anti);}
            cow=new Cow(EntityTypes.COW,arena);cowId=cow.getUUID();cow.setNoAi(true);cow.setNoGravity(true);
            cow.setPos(initial);cow.setCustomName(net.minecraft.network.chat.Component.literal("Hedgehog dimension fixture"));
            require(arena.addFreshEntity(cow),"native mob fixture admitted");
            var arrival=dimensions.transfer(cow,owner,4);cowArrival=arrival;
            require(arrival!=null&&arrival.level()==imprinted,"actual nonplayer native transfer");
            require(cow.isRemoved()&&arrival instanceof Cow moved&&moved.isAlive()&&moved.hasCustomName()
                    &&moved.getUUID().equals(cowId)&&moved.isNoAi()&&moved.isNoGravity(),"native replacement preserves UUID/name/AI/gravity on actual destination entity: originalRemoved="+cow.isRemoved()+", arrival="+arrival+", noAI="+(arrival instanceof Cow c&&c.isNoAi())+", noGravity="+arrival.isNoGravity());
            SkyWars2.LOGGER.info("[check] PASS hedgehog-dimensions");
        }finally{
            if(cow!=null&&!cow.isRemoved())cow.discard();
            if(cowArrival!=null&&!cowArrival.isRemoved())cowArrival.discard();
            if(cowId!=null)for(var level:game.server().getAllLevels()){var entity=level.getEntity(cowId);if(entity!=null)entity.discard();}
            // WandThirdPlayerState restores within the current level; crossworld fixtures restore it explicitly.
            victim.teleportTo(originalLevel,saved.position.x,saved.position.y,saved.position.z,java.util.Set.of(),saved.yaw,saved.pitch,true);
            victim.hasChangedDimension();
            saved.restore(game);
            victim.hasChangedDimension();
        }
    }
}
