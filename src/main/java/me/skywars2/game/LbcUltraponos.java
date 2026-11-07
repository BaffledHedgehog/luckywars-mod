package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

/** A finite source emitter with resident-tick native explosions and shared WILD lineage. */
public final class LbcUltraponos {
    private static final Identifier STRUCTURE=Identifier.parse("lbc:ponos");
    private static final double[] HEIGHTS={-1.5,-2.5,.5,1.5};
    private static final CompoundTag GOVNO=govno();
    private static CompoundTag govno() { var tag=new CompoundTag(); tag.putInt("govnoq",1); return tag; }
    private final GameController game;
    private final List<Emitter> emitters=new ArrayList<>();
    private final List<Blast> blasts=new ArrayList<>();
    private int pulses;
    private DamageSource activeExplosion;
    private LivingEntity activeOwner;
    private static final class Blast {
        final ServerLevel level;
        final Vec3 position;
        final int radius;
        final String epoch;
        final LivingEntity owner;
        final LbcIndirectKills.Owner cause;
        int remainingTicks=290;
        boolean charged;
        Blast(ServerLevel level,Vec3 position,int radius,String epoch,LivingEntity owner,LbcIndirectKills.Owner cause) {
            this.level=level; this.position=position; this.radius=radius; this.epoch=epoch; this.owner=owner; this.cause=cause;
        }
    }
    record BlastSnapshot(Vec3 position,int radius,int remainingTicks,LivingEntity owner) { }
    private static final class Emitter {
        final ServerLevel level;
        final Vec3 center;
        final String epoch;
        final ServerPlayer author;
        int age;
        Emitter(ServerLevel level,Vec3 center,String epoch,ServerPlayer author) {
            this.level=level; this.center=center; this.epoch=epoch; this.author=author;
        }
    }
    public LbcUltraponos(GameController game) { this.game=game; }
    public boolean ready(ItemEntity token) {
        if(token.isRemoved() || token.getAge()>=5999 || !LbcUltraponosDropItem.matches(token.getItem())
                || !game.playing() || game.stage()==GameController.Stage.ENDING
                || token.level()!=game.maps().level() || !(token.level() instanceof ServerLevel level)
                || game.items().timeStop().stopped(level)) return false;
        for(var tag:token.entityTags()) if(tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag)) return false;
        var at=token.blockPosition();
        // Template updates touch neighbours; admission never loads chunks for this trap.
        for(int x=(at.getX()-1)>>4;x<=(at.getX()+1)>>4;x++)
            for(int z=(at.getZ()-1)>>4;z<=(at.getZ()+2)>>4;z++)
                if(level.getChunkSource().getChunkNow(x,z)==null) return false;
        return true;
    }
    public boolean spawn(ItemEntity token) {
        if(!ready(token)) return false;
        var level=(ServerLevel)token.level(); var at=token.blockPosition();
        var owner=game.items().specialDrops().owner(token);
        var author=owner instanceof ServerPlayer player?player:null;
        var template=game.server().getStructureTemplateManager().get(STRUCTURE).orElseThrow();
        Runnable placement=()->game.maps().placeMapSlice(level,template,at,new StructurePlaceSettings(),level.getRandom().nextLong());
        if(author==null) placement.run(); else game.items().indirectKills().trapChanges(level,author,true,placement);
        var center=Vec3.atCenterOf(at);
        ServerPlayer nearest=null; double distance=Double.POSITIVE_INFINITY;
        for(var player:level.players()) if(game.items().wands().validTarget(player)) {
            double candidate=player.position().distanceToSqr(token.position());
            if(candidate<distance) { nearest=player; distance=candidate; }
        }
        boolean mega=nearest!=null && game.lbc().skillsEnabled() && game.players().get(nearest.getUUID()).lbcSkill==9;
        scheduleBlasts(level,center,owner,mega);
        emitters.add(new Emitter(level,center,game.items().epoch(),author));
        LbcOverload.sound(level,token.position(),"minecraft:lbcsounds.ponos",1,1,0,0);
        return true;
    }
    private void scheduleBlasts(ServerLevel level,Vec3 center,LivingEntity owner,boolean mega) {
        var epoch=game.items().epoch();
        var cause=owner instanceof ServerPlayer author?game.items().indirectKills().wildCause(author):null;
        // Author retained the datapack's additional four stronger blasts, followed by all four base blasts.
        if(mega) for(int i=0;i<4;i++) blasts.add(new Blast(level,center.add(0,HEIGHTS[i]-.5,0),(i+6)*2,epoch,owner,cause));
        for(int i=0;i<4;i++) blasts.add(new Blast(level,center.add(0,HEIGHTS[i]-.5,0),i+6,epoch,owner,cause));
    }
    public void tick() {
        for(var emitter:List.copyOf(emitters)) {
            if(!game.playing() || game.stage()==GameController.Stage.ENDING || !game.items().currentEpoch(emitter.epoch)) {
                emitters.remove(emitter); continue;
            }
            if(game.items().timeStop().stopped(emitter.level)) continue;
            var cell=BlockPos.containing(emitter.center);
            if(emitter.level.getChunkSource().getChunkNow(cell.getX()>>4,cell.getZ()>>4)==null) continue;
            // Source mecha-marker deletion is permanent; cancelling does not defuse queued blasts.
            if(game.items().magic().mechanismSuppressedAt(emitter.level,emitter.center.add(0,1,0))) {
                emitters.remove(emitter); continue;
            }
            emitter.age++;
            pulse(emitter);
            if(emitter.age>=280) emitters.remove(emitter);
        }
        for(var blast:List.copyOf(blasts)) {
            if(!game.playing() || game.stage()==GameController.Stage.ENDING || !game.items().currentEpoch(blast.epoch)) {
                blasts.remove(blast); continue;
            }
            if(game.items().timeStop().stopped(blast.level)) continue;
            var cell=BlockPos.containing(blast.position);
            // Match an entity's ticking residency without tickets, chunk reads that load, or helper entities.
            if(blast.level.getChunkSource().getChunkNow(cell.getX()>>4,cell.getZ()>>4)==null
                    || !blast.level.isPositionEntityTicking(cell)) continue;
            if(--blast.remainingTicks>0) continue;
            blasts.remove(blast);
            var kills=game.items().indirectKills();
            var damage=kills.captureDamage(blast.level.damageSources().explosion(null,blast.owner),blast.cause);
            var previous=activeExplosion; var previousOwner=activeOwner;
            activeExplosion=damage; activeOwner=blast.owner;
            try {
                kills.trapChanges(blast.level,blast.cause,()->blast.level.explode(null,damage,null,
                        blast.position.x,blast.position.y,blast.position.z,blast.radius*(blast.charged?2:1),false,Level.ExplosionInteraction.MOB));
            } finally { activeExplosion=previous; activeOwner=previousOwner; }
        }
    }
    /** Called only from vanilla's active, non-visual lightning damage phase. Repeated flashes never stack. */
    public void lightning(LightningBolt bolt) {
        if(!(bolt.level() instanceof ServerLevel level) || game.items().timeStop().stopped(level)) return;
        var strike=new AABB(bolt.getX()-3,bolt.getY()-3,bolt.getZ()-3,bolt.getX()+3,bolt.getY()+9,bolt.getZ()+3);
        for(var blast:blasts) if(blast.level==bolt.level() && game.items().currentEpoch(blast.epoch)
                && strike.intersects(new AABB(blast.position.x-.3,blast.position.y,blast.position.z-.3,
                        blast.position.x+.3,blast.position.y+1.7,blast.position.z+.3))) blast.charged=true;
    }
    public boolean isExplosion(DamageSource source) { return source!=null && source==activeExplosion; }
    /** Native TNT is still a real actor; share its existing epoch, WILD and nullable-owner lineage. */
    public void inheritExplosionChild(DamageSource source,net.minecraft.world.entity.Entity child) {
        if(isExplosion(source)) game.items().creeperDrops().adoptUltraponosChild(child,activeOwner);
    }
    private void pulse(Emitter emitter) {
        pulses++;
        var level=emitter.level; var center=emitter.center.add(0,1,0);
        int count=0;
        for(var item:level.getEntitiesOfClass(ItemEntity.class,new AABB(center,center).inflate(15),
                item->item.position().distanceToSqr(center)<=225
                    && item.getItem().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).matchedBy(GOVNO)))
            if(++count>200) return;
        var position=emitter.center.add(0,.55,0);
        for(var stack:game.lbc().rollTable(level,position,emitter.author,"lbc:govno")) {
            var item=new ItemEntity(level,position.x,position.y,position.z,stack);
            var random=level.getRandom();
            item.setDeltaMovement(random.nextDouble()*.4-.2,.8,random.nextDouble()*.4-.2);
            item.setDefaultPickUpDelay(); game.items().markCurrentItemEpoch(item);
            game.items().timeStop().prepareFrozen(item); level.addFreshEntity(item);
        }
        // Same TerrainParticle brown-wool shards; BLOCK normalizes input speed to <=.28Y.
        // DustPillarProvider restores supplied Y plus native Gaussian jitter, with item-like .8 mean.
        level.sendParticles(new BlockParticleOption(ParticleTypes.DUST_PILLAR,Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.BROWN).defaultBlockState()),
                position.x,position.y,position.z,60,0,0,0,0,.8,0,
                net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.ALTERNATIVE);
    }
    public int emitterCount() { return emitters.size(); }
    public int pendingBlastCount() { return blasts.size(); }
    List<BlastSnapshot> pendingBlasts() {
        return blasts.stream().map(blast->new BlastSnapshot(blast.position,blast.radius*(blast.charged?2:1),blast.remainingTicks,blast.owner)).toList();
    }
    public int emittedPulses() { return pulses; }
    public void stop() { emitters.clear(); blasts.clear(); pulses=0; }
}
