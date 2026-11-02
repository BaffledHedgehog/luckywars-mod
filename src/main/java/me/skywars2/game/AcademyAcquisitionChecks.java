package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Actual imported templates, native actors and interaction; service stepping does not claim natural physics. */
public final class AcademyAcquisitionChecks {
    private AcademyAcquisitionChecks() { }
    private static void require(boolean ok,String message) {
        if (!ok) throw new IllegalStateException("Academy acquisition: "+message);
    }
    public static void run(GameController game) {
        var level=game.maps().level(); var academy=game.items().academy();
        ServerPlayer owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(game.playing() && owner!=null && game.canUseGameplayItems(owner),"active native participant");
        require(academy.pendingCount()==0 && academy.ownedCount()==0,"exclusive academy fixture");
        var state=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
        var template=game.server().getStructureTemplateManager().get(Identifier.parse("lbc:magic_academy")).orElseThrow();
        var size=template.getSize();
        var source=new Vec3(Math.floor(owner.getX()/16)*16+8.25,240.25,Math.floor(owner.getZ()/16)*16+8.75);
        var origin=BlockPos.containing(source).offset(-6,-1,-6);
        var blocks=new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>(); var forced=new HashSet<Long>();
        var before=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());
        level.getAllEntities().forEach(before::add);
        try {
            for (int x=(origin.getX()-1)>>4;x<=(origin.getX()+size.getX())>>4;x++)
                for (int z=(origin.getZ()-1)>>4;z<=(origin.getZ()+size.getZ())>>4;z++) {
                    long key=net.minecraft.world.level.ChunkPos.pack(x,z);
                    if (!level.getForceLoadedChunks().contains(key)) { forced.add(key); level.setChunkForced(x,z,true); }
                    level.getChunk(x,z);
                }
            for (var pos:BlockPos.betweenClosed(origin.offset(-1,-1,-1),origin.offset(size.getX(),size.getY()+1,size.getZ())))
                blocks.put(pos.immutable(),game.maps().snapshot(level,pos));
            for (boolean token:new boolean[]{false,true}) {
                owner.teleportTo(level,source.x+2,source.y+2,source.z,Set.of(),0,0,false);
                var prior=owner.position();
                String table=token?"lbc:ported/academy_token":"lbc:ported/magic_academy_drop";
                var rolled=game.lbc().rollTable(level,source,owner,table);
                require(rolled.size()==1,"one real token from "+table);
                ItemStack stack=rolled.getFirst().copy(); stack.setCount(2);
                require(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag()
                        .getIntOr(token?"academy_token":"magic_academy",0)==1,"real source flag");
                var drop=new ItemEntity(level,source.x,source.y,source.z,stack); drop.setThrower(owner); drop.setOnGround(token);
                require(level.addFreshEntity(drop),"native item admission");
                game.items().timeStop().begin(level,40); academy.tick();
                require(academy.pendingCount()==0 && !drop.isRemoved() && drop.getItem().getCount()==2,"Time Stop prevents debit");
                game.items().timeStop().end(level); academy.tick();
                require(token?academy.pendingCount()==1 && academy.pendingRow()==1:academy.pendingCount()==0,"token is grounded/sliced; active aerial drop completes in its first callback");
                require(token?owner.position().distanceToSqr(prior)<.0001:owner.position().distanceToSqr(source.add(0,1,-2))<.0001,"token waits; active drop is completed before its immediate teleport");
                require(token?drop.isRemoved():!drop.isRemoved() && drop.getItem().getCount()==1,"variant debit semantics");
                drop.discard(); // Remaining base unit is separately eligible on its next grounded tick.
                int row=academy.pendingRow();game.items().timeStop().begin(level,40);academy.tick();
                require(academy.pendingRow()==row,"Time Stop pauses queued rows");game.items().timeStop().end(level);
                for (int i=0;i<size.getY()+2 && academy.pendingCount()>0;i++) academy.tick();
                require(academy.pendingCount()==0,"all native template rows finish");
                Entity actor=null;
                for (var entity:level.getAllEntities()) if (!before.contains(entity) && entity.entityTags().contains("magic_academy")) actor=entity;
                require(actor!=null && !actor.isRemoved(),"actual tagged academy actor");
                require(token?owner.position().distanceToSqr(prior)<.0001:
                        owner.position().distanceToSqr(source.add(0,1,-2))<.0001,"variant completion teleport");
                owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcWands.FIRE_WAND));
                require(academy.interact(owner,InteractionHand.MAIN_HAND,actor),"actual academy interaction callback");
                require(owner.getMainHandItem().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag()
                        .getIntOr("upgradable",0)==1,"native actor starts supported wand trial");
                academy.stop();require(academy.ownedCount()==0 && actor.isRemoved(),"quiet owned actor cleanup");
            }
            // Out-of-height admission remains free, with no partial native write or queued work.
            var invalid=new ItemEntity(level,source.x,level.getMaxY()-1,source.z,new ItemStack(LbcAcademy.TOKEN));
            invalid.setThrower(owner);invalid.setOnGround(true);require(level.addFreshEntity(invalid),"height rejection item");
            academy.tick();require(!invalid.isRemoved() && academy.pendingCount()==0,"full-height validation before debit");invalid.discard();
            SkyWars2.LOGGER.info("[check] PASS academy-acquisition");
        } finally {
            game.items().timeStop().end(level);academy.stop();
            var added=new ArrayList<Entity>();level.getAllEntities().forEach(entity->{if(!before.contains(entity) && !(entity instanceof ServerPlayer)
                    && entity.position().x>=origin.getX()-1 && entity.position().x<=origin.getX()+size.getX()+1
                    && entity.position().z>=origin.getZ()-1 && entity.position().z<=origin.getZ()+size.getZ()+1)added.add(entity);});
            added.forEach(Entity::discard);
            for (var entry:blocks.entrySet()) {
                if (level.getBlockEntity(entry.getKey()) instanceof net.minecraft.world.Container container) container.clearContent();
                game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue());
            }
            for (long key:forced) level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
            state.restore(game);
        }
    }
}
