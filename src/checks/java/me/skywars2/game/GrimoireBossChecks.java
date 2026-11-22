package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import me.skywars2.game.effects.magic.LbcMagic;
import me.skywars2.game.entities.mobs.LbcGrimoireBoss;
import me.skywars2.game.match.checkpoints.MatchRandomState;
import me.skywars2.game.presentation.projectiles.LbcBeamGeometry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import me.skywars2.game.academy.TrialWarningVisual;

/** Native, independently callable encounter fixtures. Registration/run ownership stays in GameChecks. */
public final class GrimoireBossChecks {
    private GrimoireBossChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Grimoire boss: "+why);}
    @SuppressWarnings("unchecked") private static AttachmentType<CompoundTag> attachment(){try{var field=LbcGrimoireBoss.class.getDeclaredField("DATA");field.setAccessible(true);return (AttachmentType<CompoundTag>)field.get(null);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
    private static CompoundTag saved(Entity e){var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,e.registryAccess());e.saveWithoutId(output);return output.buildResult();}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;final LbcGrimoireBoss service;
        final List<Entity> victims=new ArrayList<>();Warden boss;Cow laserTarget;float firstHealth;
        final MatchRandomState worldRandom;
        Fixture(GameController game){this.game=game;base=new ElementalGrimoireChecks.Fixture(game,true);base.retain=true;base.initialize();worldRandom=MatchRandomState.capture(base.owner.level().getRandom());service=new LbcGrimoireBoss(game);}
        void spawn(){boss=service.spawn(base.owner.level(),base.owner.position().add(0,0,2),0,base.owner);require(boss!=null,"actual native stationary warden admission");boss.setNoGravity(true);
            require(boss.getHealth()==1000&&boss.getMaxHealth()==1000&&boss.getAttribute(Attributes.ARMOR).getBaseValue()==1&&boss.getAttribute(Attributes.ARMOR_TOUGHNESS).getBaseValue()==1,"source1000HP/armor1/toughness1");
            require(saved(boss).getStringOr("DeathLootTable","").equals("lbc:grimoire_boss"),"real native source death table");
            require(!game.lbc().rollTable(base.owner.level(),boss.position(),base.owner,"lbc:grimoire_boss").isEmpty(),"packaged boss death rewards are executable");
            var visual=service.snapshot().stream().filter(e->e.entityTags().contains("grimoire_boss_visual")).findFirst().orElseThrow();
            require(visual instanceof Display.ItemDisplay item&&item.getItemStack().get(DataComponents.ITEM_MODEL).toString().equals("lbc:grimoire_boss")&&visual.position().distanceToSqr(boss.position().add(0,6,0))<1e-10,"existing model at exact hitbox+6");
            require(service.snapshot().stream().filter(e->e.entityTags().contains("spinning_red_penis")).allMatch(e->e instanceof Display.BlockDisplay block&&block.getBlockState().is(Blocks.REDSTONE_BLOCK)),"red core has an actual native block state");
            var source=game.items().spellSources().source(boss);require(source!=null&&source.emitter().equals(boss.getUUID())&&source.author().equals(base.owner.getUUID())&&source.persistent(),"real emitter and persistent causal owner");
            require(game.items().spellSources().target(source,base.owner)&&!game.items().spellSources().target(source,boss),"hostile emitter admits author and excludes own body");
        }
        Cow cow(Vec3 at){var e=new Cow(EntityTypes.COW,base.owner.level());e.setPos(at);e.setNoAi(true);e.setNoGravity(true);
            // Isolate hurt-time and ray geometry from recoil and neighboring fixture-body pushes.
            e.noPhysics=true;e.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);e.setHealth(1000);require(base.owner.level().addFreshEntity(e),"native living target registration");victims.add(e);return e;}
        void warningAndFrozen(){service.tick();require(service.warningSnapshot().size()==1,"one shared native display warning");var warning=service.warningSnapshot().getFirst();
            require(warning.model().toString().equals("lbc:meditation_warning_beam_red")&&warning.age()==0,"existing bloom/fade warning model with source countdown");
            var before=boss.getAttached(attachment()).copy();boss.entityTags().add("time_stopped");service.tick();require(boss.getAttached(attachment()).equals(before)&&warning.age()==0,"actor TimeStop freezes canonical clocks and derived warning");boss.entityTags().remove("time_stopped");
            service.tick();require(warning.age()==1,"second active encounter update advances original warning");
            var out=saved(boss);require(out.toString().contains("grimoire_boss")&&boss.getAttached(attachment()).getIntOr("age",0)==2,"native serialized attachment owns phase clock");
        }
        void exactLaser(){
            var origin=boss.position().add(0,6,0);var hit=cow(origin.add(0,-.5,4));var miss=cow(origin.add(.8,-.5,4));var protectedMob=cow(origin.add(0,-.5,6));protectedMob.entityTags().add("dungeon_mob");
            var d=boss.getAttached(attachment()).copy();d.putInt("phase",1);d.putInt("age",70);d.putDouble("aimX",0);d.putDouble("aimY",0);d.putDouble("aimZ",1);boss.setAttached(attachment(),d);
            // Remove auto-facing ambiguity: actual nearest eye lies precisely on the existing beam.
            hit.setPos(origin.add(0,-hit.getEyeHeight(),4));miss.setPos(origin.add(.8,-hit.getEyeHeight(),4));
            base.owner.setPos(origin.add(8, -6, 0));laserTarget=hit;
            var stop = BlockPos.containing(origin.add(0, 0, 9));
            var level = base.owner.level();
            var previous = level.getBlockState(stop);
            require(previous.isAir(), "open native laser sample area");
            level.setBlock(stop, Blocks.STONE.defaultBlockState(), 3);
            try {
                base.recording.packets.clear();
                service.tick();
                int samples = 0;
                for (double n = 0; n <= stop.getZ() - origin.z; n += .3) samples++;
                assertTrail(ParticleTypes.DUST_COLOR_TRANSITION, samples, .15f, "blocked boss laser");
            } finally {
                level.setBlock(stop, previous, 3);
            }
            firstHealth=hit.getHealth();require(firstHealth==990,"exact body contact deals ten native HP, actual="+firstHealth);
            require(hit.getLastDamageSource()!=null&&hit.getLastDamageSource().getDirectEntity()==boss
                    &&hit.getLastDamageSource().is(net.minecraft.world.damagesource.DamageTypes.MAGIC),"native physical attacker remains the real boss");
            require(miss.getHealth()==1000&&protectedMob.getHealth()==1000,"nearby body outside precise ray and dungeon role stay protected");require(service.warningSnapshot().isEmpty(),"damage starts after warning retirement");
            require(!LbcBeamGeometry.intersects(new AABB(origin.add(.5,-1,2),origin.add(1.1,1,3)),origin,origin.add(0,0,10)),"independent former selector-cell near miss");
        }
        void suppression(){
            var visual=service.snapshot().stream().filter(e->e.entityTags().contains("grimoire_boss_visual")).findFirst().orElseThrow();
            require(!game.items().magic().magical(visual),"boss presentation is preserved by generic antimagic cleanup");
            var marker=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,base.owner.level());marker.setPos(boss.position());marker.entityTags().add("stopper_magic");
            require(base.owner.level().addFreshEntity(marker),"native attack suppression beacon");game.items().magic().loaded(marker);
            int age=boss.getAttached(attachment()).getIntOr("age",0);float health=laserTarget.getHealth();
            try{service.tick();require(boss.isAlive()&&!visual.isRemoved()&&boss.getAttached(attachment()).getIntOr("age",0)==age&&laserTarget.getHealth()==health,"beacon blocks attacks while retaining boss and visual");}
            finally{game.items().magic().unloaded(marker);marker.discard();}
            var old=base.owner.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).copy();var at=base.owner.position();
            base.owner.setPos(boss.position().add(2,0,0));base.owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(LbcMagic.MYSTICAL_ARTIFACT));
            game.items().magic().inventoryChanged(base.owner);game.items().magic().flushCleansing();
            try{service.tick();require(game.items().magic().artifactActive(base.owner)&&boss.isAlive()&&!visual.isRemoved()
                    &&boss.getAttached(attachment()).getIntOr("age",0)==age&&laserTarget.getHealth()==health,"artifact also blocks attacks and preserves native boss/presentation");}
            finally{base.owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,old);base.owner.setPos(at);game.items().magic().inventoryChanged(base.owner);game.items().magic().flushCleansing();}
        }
        void identityAndReload(){
            var doomed=cow(boss.position().add(3,6,3));doomed.setHealth(1);
            require(doomed.hurtServer(base.owner.level(),laserTarget.getLastDamageSource(),10)&&!doomed.isAlive()
                    &&doomed.getKillCredit()==base.owner,"actual fatal beam source gives native kill credit to captured causal author");
            var inherited=game.items().spellSources().source(boss);
            var second=service.spawn(base.owner.level(),boss.position().add(4,0,0),90,inherited);require(second!=null,"independent nearby boss from accepted source without player argument");second.setNoGravity(true);
            var continued=game.items().spellSources().source(second);require(continued.author().equals(inherited.author())&&continued.epoch().equals(inherited.epoch())&&continued.cause().order()==inherited.cause().order()&&continued.emitter().equals(second.getUUID()),"accepted native continuation preserves original author/epoch/order and binds new actual emitter");
            var old=boss.getAttached(attachment()).copy();service.unloaded(boss);require(!boss.isRemoved(),"unload retains native body and saved state");service.loaded(boss);require(boss.getAttached(attachment()).equals(old),"reload does not reset phases or owner");
            boss.hurtServer(base.owner.level(),boss.damageSources().genericKill(),Float.MAX_VALUE);service.tick();
            require(service.snapshot().contains(second)&&second.isAlive(),"one boss death cannot retire foreign nearby hitbox");
            for(var e:service.snapshot())require(!e.getAttached(attachment()).getStringOr("boss","").equals(boss.getStringUUID()),"only exact boss-linked pieces retired");
        }
        void phasePatterns(){
            barrierFacing();
            // Exercise the real attack branches: partial render presets must not move children to 0,0,0.
            for(int phase:new int[]{2,3}){
                var d=boss.getAttached(attachment()).copy();d.putInt("phase",phase);d.putInt("age",phase==2?29:19);boss.setAttached(attachment(),d);
                List<Entity> created=List.of();
                for(int n=0;n<32&&created.isEmpty();n++){
                    service.tick();final int kind=phase==2?3:2;
                    created=service.snapshot().stream().filter(e->e.getAttached(attachment()).getIntOr("kind",0)==kind).toList();
                }
                require(!created.isEmpty(),"native chance-gated attack branch "+phase);
                for (var child : created) {
                    require(child.getY() > boss.getY() && child.position().distanceToSqr(boss.position()) < 1600,
                            "meteor/prism native position survives render preset " + phase);
                    if(phase==2){
                        require(child instanceof Display.BlockDisplay stone&&stone.getBlockState().is(Blocks.CHISELED_TUFF_BRICKS),"meteor carries actual visible chiseled tuff rather than air");
                        DisplayTransformChecks.pose((Display)child,0,0,0,1,1,1);
                        require(child.shouldRenderAtSqrDistance(128*128),"native meteor view range admits a distant dungeon camera");
                    }else{
                        var transform=com.mojang.math.Transformation.EXTENDED_CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,saved(child).get("transformation")).getOrThrow().getMatrix();
                        float[] expected={0,0,1,0,1,0,0,0,0,1,0,0,0,0,0,1};
                        for(int n=0;n<16;n++)require(Math.abs(transform.getRowColumn(n>>2,n&3)-expected[n])<1e-5,"prism uses exact ROW-major legacy axis permutation; cell="+n);
                        var tip=transform.transformDirection(new org.joml.Vector3f(0,1,0));
                        require(tip.distance(new org.joml.Vector3f(0,0,1))<1e-5,"actual prism texture tip points along the local shot direction");
                    }
                }
                if (phase == 3) prismBurst(created.getFirst());
                for (var child : created) {
                    service.unloaded(child);
                    child.discard();
                }
            }
        }

        void barrierFacing(){
            var level=base.owner.level();var at=boss.position().add(0,0,8);var cell=BlockPos.containing(at.add(0,.5,0));
            var positions=List.of(cell.east(),cell.west(),cell.south(),cell.north());var before=new ArrayList<net.minecraft.world.level.block.state.BlockState>();positions.forEach(p->before.add(level.getBlockState(p)));
            float[] angles={-90,90,0,180};
            try{
                for(int i=0;i<positions.size();i++){
                    positions.forEach(p->level.setBlock(p,Blocks.AIR.defaultBlockState(),818));level.setBlock(positions.get(i),Blocks.BARRIER.defaultBlockState(),818);
                    var spawned=service.spawn(level,at,37,game.items().spellSources().source(boss));require(spawned!=null,"native boss spawn with adjacent barrier");
                    var visual=service.snapshot().stream().filter(e->e.entityTags().contains("grimoire_boss_visual")&&e.getAttached(attachment()).getStringOr("boss","").equals(spawned.getStringUUID())).findFirst().orElseThrow();
                    require(spawned.getYRot()==angles[i]&&visual.getYRot()==angles[i],"barrier selects exact legacy facing, independent of marker yaw");
                    spawned.setYRot(73);spawned.setYHeadRot(120);visual.setYRot(19);visual.setXRot(42);service.tick();
                    require(spawned.getYRot()==angles[i]&&visual.getYRot()==angles[i]&&visual.getXRot()==0,"boss facing remains fixed while attacks track independently");
                    var own=new ArrayList<Entity>();for(var e:service.snapshot())if(e==spawned||e.getAttached(attachment()).getStringOr("boss","").equals(spawned.getStringUUID()))own.add(e);
                    for(var e:own){service.unloaded(e);e.discard();}
                }
            }finally{for(int i=0;i<positions.size();i++)level.setBlock(positions.get(i),before.get(i),818);}
        }

        void prismBurst(Entity prism) {
            var from = base.owner.position().add(0, 3, 0);
            prism.setPos(from);
            prism.setYRot(0);
            prism.setXRot(0);
            var d = prism.getAttached(attachment()).copy();
            d.putInt("age", 30);
            prism.setAttached(attachment(), d);
            base.recording.packets.clear();
            service.tick();
            require(prism.isRemoved(), "native prism completes its shot");
            assertTrail(ParticleTypes.FIREWORK, 91, .01f, "prism burst");
            var flash = packets(ParticleTypes.FLASH);
            require(flash.size() == 1 && flash.getFirst().count() == 1 && flash.getFirst().overrideLimiter(),
                    "one forced native flash at prism firing");
            var packet = flash.getFirst();
            require(new Vec3(packet.x(), packet.y(), packet.z()).distanceToSqr(from) < 1e-10
                    && packet.particle() instanceof ColorParticleOption color
                    && color.getRed() == 1 && Math.abs(color.getGreen() - .1f) <= 1f / 255
                    && color.getBlue() == 0 && color.getAlpha() == 1,
                    "legacy red flash color and muzzle position");
        }

        List<ClientboundLevelParticlesPacket> packets(ParticleType<?> type) {
            return base.recording.packets.stream().filter(ClientboundLevelParticlesPacket.class::isInstance)
                    .map(ClientboundLevelParticlesPacket.class::cast).filter(p -> p.particle().getType() == type).toList();
        }

        void assertTrail(ParticleType<?> type, int expected, float speed, String attack) {
            var trail = packets(type);
            int total = trail.stream().mapToInt(ClientboundLevelParticlesPacket::count).sum();
            require(total == expected && trail.size() == (expected + 2) / 3
                    && trail.stream().allMatch(p -> !p.overrideLimiter() && p.xMaxSpeed() == speed
                            && p.yMaxSpeed() == speed && p.zMaxSpeed() == speed),
                    attack + " keeps samples, settings and speed in fewer packets: " + total + "/" + expected
                            + " particles, " + trail.size() + " packets");
            SkyWars2.LOGGER.info("[check] {} particle packets {} -> {}, density {}", attack, expected, trail.size(), total);
        }
        @Override public void close(){try{service.stop();for(var e:victims){game.items().indirectKills().clear((LivingEntity)e);e.discard();}
            var fresh=new ArrayList<Entity>();base.owner.level().getAllEntities().forEach(e->{if(!base.before.contains(e)&&!(e instanceof net.minecraft.server.level.ServerPlayer))fresh.add(e);});fresh.forEach(Entity::discard);worldRandom.restore(base.owner.level().getRandom());}finally{base.close();}}
    }
    private static Fixture running;private static int started;
    public static void run(GameController game){
        require(running==null,"exclusive encounter fixture");var fixture=new Fixture(game);
        try{fixture.spawn();fixture.warningAndFrozen();fixture.exactLaser();fixture.suppression();running=fixture;started=game.server().getTickCount();}
        catch(Throwable error){fixture.close();throw error;}
    }
    public static void tick(GameController game){if(running==null)return;var fixture=running;try{
        int elapsed=game.server().getTickCount()-started;require(elapsed<15,"bounded native hurt cooldown");
        if(elapsed<10){require(fixture.laserTarget.getHealth()==fixture.firstHealth,"repeated beam attempts obey native hurt cooldown");return;}
        require(fixture.laserTarget.getHealth()==fixture.firstHealth-10,"one further ten-HP hit after ten real native ticks");
        fixture.phasePatterns();fixture.identityAndReload();fixture.close();running=null;
        SkyWars2.LOGGER.info("[check] PASS grimoire-boss (native spawn/model/loot, shared display warning, exact ray/body and protected roles, native ten-HP/ten-tick cadence, antimagic retains boss/visual, source continuation/reload and independent death cleanup)");
        }catch(Throwable error){try{fixture.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL grimoire-boss",error);}
    }
}
