package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

/** A finite source emitter; delayed explosions keep native creeper ticking and shared lineage. */
public final class LbcUltraponos {
    private static final Identifier STRUCTURE=Identifier.parse("lbc:ponos");
    private static final double[] HEIGHTS={-1.5,-2.5,.5,1.5};
    private static final CompoundTag GOVNO=govno();
    private static CompoundTag govno() { var tag=new CompoundTag(); tag.putInt("govnoq",1); return tag; }
    private final GameController game;
    private final List<Emitter> emitters=new ArrayList<>();
    private int pulses;
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
        boolean mega=nearest!=null && game.config().lbcSkills && game.players().get(nearest.getUUID()).lbcSkill==9;
        spawnCreepers(level,center,owner,mega);
        emitters.add(new Emitter(level,center,game.items().epoch(),author));
        LbcOverload.sound(level,token.position(),"minecraft:lbcsounds.ponos",1,1,0,0);
        return true;
    }
    private void spawnCreepers(ServerLevel level,Vec3 center,LivingEntity owner,boolean mega) {
        // Author retained the datapack's additional four stronger blasts, followed by all four base blasts.
        if(mega) for(int i=0;i<4;i++) creeper(level,center.add(0,HEIGHTS[i]-.5,0),owner,(i+6)*2);
        for(int i=0;i<4;i++) creeper(level,center.add(0,HEIGHTS[i]-.5,0),owner,i+6);
    }
    private void creeper(ServerLevel level,Vec3 position,LivingEntity owner,int radius) {
        var creeper=new Creeper(net.minecraft.world.entity.EntityTypes.CREEPER,level);
        var preset=new CompoundTag();
        preset.putShort("Fuse",(short)290); preset.putByte("ExplosionRadius",(byte)radius);
        preset.putBoolean("ignited",true); preset.putBoolean("NoAI",true); preset.putBoolean("NoGravity",true);
        preset.putBoolean("Silent",true); preset.putBoolean("Invulnerable",true); preset.putBoolean("PersistenceRequired",true);
        creeper.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),preset));
        creeper.setPos(position);
        creeper.entityTags().add("govno_creeper");
        creeper.setCustomName(Component.translatable("megadrist").withStyle(net.minecraft.ChatFormatting.YELLOW).withStyle(style->style.withItalic(false)));
        creeper.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY,288,0,true,false,false));
        game.items().creeperDrops().adoptUltraponos(creeper,owner);
        if(!level.addFreshEntity(creeper)) creeper.discard();
    }
    public void tick() {
        for(var emitter:List.copyOf(emitters)) {
            if(!game.playing() || game.stage()==GameController.Stage.ENDING || !game.items().currentEpoch(emitter.epoch)) {
                emitters.remove(emitter); continue;
            }
            if(game.items().timeStop().stopped(emitter.level)) continue;
            var cell=BlockPos.containing(emitter.center);
            if(emitter.level.getChunkSource().getChunkNow(cell.getX()>>4,cell.getZ()>>4)==null) continue;
            // Source mecha-marker deletion is permanent; cancelling does not defuse existing creepers.
            if(game.items().magic().mechanismSuppressedAt(emitter.level,emitter.center.add(0,1,0))) {
                emitters.remove(emitter); continue;
            }
            emitter.age++;
            pulse(emitter);
            if(emitter.age>=280) emitters.remove(emitter);
        }
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
    public int emittedPulses() { return pulses; }
    public void stop() { emitters.clear(); pulses=0; }
}
