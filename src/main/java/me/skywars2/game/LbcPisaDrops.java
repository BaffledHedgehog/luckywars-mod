package me.skywars2.game;

import net.minecraft.commands.arguments.coordinates.LocalCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** The authored leaning tower, using ordinary native boats and a real chest reward. */
public final class LbcPisaDrops {
    public static final String ACTOR="skywars2.pisa_boat";
    private final GameController game;
    private final Set<AbstractBoat> tracked=new LinkedHashSet<>();

    public LbcPisaDrops(GameController game) { this.game=game; }

    private Vec2 rotation(ItemEntity token) {
        var source=token.createCommandSourceStackForNameResolution((ServerLevel)token.level()).facing(Vec3.ZERO);
        return new Vec2(0,source.getRotation().y);
    }
    private static Vec3 position(Vec3 origin,Vec2 rotation,int index) {
        return new LocalCoordinates(0,index,-.4*index).apply(origin,rotation);
    }
    public boolean ready(ItemEntity token) {
        if(token.isRemoved() || token.getAge()>=5999 || !LbcPisaDropItem.matches(token.getItem())
                || !game.playing() || game.stage()==GameController.Stage.ENDING
                || token.level()!=game.maps().level() || !(token.level() instanceof ServerLevel level)
                || game.items().timeStop().stopped(level)) return false;
        for(var tag:token.entityTags())
            if(tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag)) return false;
        Vec3 origin=token.position().add(0,20,0); Vec2 rotation=rotation(token);
        for(int i=0;i<=61;i++) {
            var pos=BlockPos.containing(position(origin,rotation,i));
            if(!Level.isInSpawnableBounds(pos)) continue;
            // Include each boat's footprint, without loading chunks or keeping tickets.
            for(int x=(pos.getX()-1)>>4;x<=(pos.getX()+1)>>4;x++)
                for(int z=(pos.getZ()-1)>>4;z<=(pos.getZ()+1)>>4;z++)
                    if(level.getChunkSource().getChunkNow(x,z)==null) return false;
        }
        return true;
    }
    public boolean spawn(ItemEntity token) {
        if(!ready(token)) return false;
        var level=(ServerLevel)token.level();
        Vec3 origin=token.position().add(0,20,0); Vec2 rotation=rotation(token);
        ServerPlayer nearest=null; double distance=Double.POSITIVE_INFINITY;
        for(var player:level.players()) if(game.items().wands().validTarget(player)) {
            double candidate=player.position().distanceToSqr(origin);
            if(candidate<distance) { nearest=player; distance=candidate; }
        }
        boolean upgraded=nearest!=null && game.lbc().skillsEnabled() && game.players().get(nearest.getUUID()).lbcSkill==9;
        var reward=game.lbc().rollTable(level,origin,nearest,"lbc:"+(upgraded?"pisa_relic_upgraded":"pisa_relic"));
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.OAK_PLANKS.defaultBlockState()),
                origin.x,origin.y-10,origin.z,100,.5,8,.5,0);
        var sound=Holder.direct(SoundEvents.WOOD_BREAK);
        double range=sound.value().getRange(1);
        long seed=level.getRandom().nextLong();
        for(var viewer:level.players()) if(viewer.position().distanceToSqr(origin)<=range*range)
            viewer.connection.send(new ClientboundSoundPacket(sound,SoundSource.MASTER,origin.x,origin.y,origin.z,1,1,seed));
        for(int i=0;i<=61;i++) {
            var pos=position(origin,rotation,i);
            if(!Level.isInSpawnableBounds(BlockPos.containing(pos))) continue;
            AbstractBoat boat=i==61?EntityTypes.OAK_CHEST_BOAT.create(level,EntitySpawnReason.COMMAND)
                    :EntityTypes.OAK_BOAT.create(level,EntitySpawnReason.COMMAND);
            if(boat==null) continue;
            boat.snapTo(pos.x,pos.y,pos.z,0,0);
            if(i==0 || i==61) {
                boat.setCustomName(Component.translatable("pizansk"));
                boat.setCustomNameVisible(true);
            }
            if(boat instanceof ChestBoat chest && !reward.isEmpty()) chest.setItem(14,reward.getFirst());
            boat.entityTags().add(ACTOR);
            game.items().markCurrentItemEpoch(boat);
            tracked.add(boat);
            game.items().timeStop().prepareFrozen(boat);
            if(!level.addFreshEntity(boat)) { tracked.remove(boat); discardQuietly(boat); }
        }
        return true;
    }
    private static boolean managed(Entity entity) {
        return entity instanceof AbstractBoat && entity.entityTags().contains(ACTOR);
    }
    public void loaded(Entity entity) {
        if(!managed(entity)) return;
        var boat=(AbstractBoat)entity;
        if(!game.playing() || game.stage()==GameController.Stage.ENDING || boat.level()!=game.maps().level()
                || !game.items().currentEpoch(boat)) { discardQuietly(boat); return; }
        tracked.add(boat);
        game.items().timeStop().prepareFrozen(boat);
    }
    public void unloaded(Entity entity) { if(managed(entity)) tracked.remove(entity); }
    private static void discardQuietly(AbstractBoat boat) {
        // Native chest boats drop their container even on discard; lifecycle cleanup must be quiet.
        if(boat instanceof ChestBoat chest) chest.clearContent();
        boat.discard();
    }
    public void stop() {
        var boats=List.copyOf(tracked); tracked.clear();
        for(var boat:boats) if(!boat.isRemoved()) discardQuietly(boat);
    }
    public List<AbstractBoat> snapshot() { return List.copyOf(tracked); }
    public int trackedCount() { return tracked.size(); }
}
