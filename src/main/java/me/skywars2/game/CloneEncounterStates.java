package me.skywars2.game;

import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/** Body-local encounter reconstruction; emitted independent attacks are never replayed with a boss. */
public final class CloneEncounterStates {
    private CloneEncounterStates() { }
    public static boolean recognizes(Entity e) {
        return LbcCrocodildo.managed(e)||LbcSauvojenBoss.managed(e)||LbcGrimoireBoss.managed(e)||LbcGodHedgehog.cloneRecognizes(e)
                ||LbcHelicopterEncounter.cloneRecognizes(e)||LbcShishEncounter.managed(e)||LbcApocalypseActors.managed(e);
    }
    public static boolean protectedActor(Entity e) {
        return LbcCrocodildo.visual(e)||LbcGrimoireBoss.cloneProtected(e)||LbcGodHedgehog.cloneProtected(e)
                ||LbcHelicopterEncounter.cloneProtected(e)||LbcShishEncounter.cloneProtected(e);
    }
    public static LbcCloneSkills.ActorImage capture(GameController game,Entity e) {
        if(protectedActor(e))return null;
        if(LbcCrocodildo.managed(e))return game.items().crocodildo().cloneImage(e);
        if(LbcSauvojenBoss.managed(e))return game.items().sauvojenBoss().cloneImage(e);
        if(LbcGrimoireBoss.managed(e))return game.items().grimoireBoss().cloneImage(e);
        if(LbcGodHedgehog.cloneRecognizes(e))return game.items().godHedgehog().cloneImage(e);
        if(LbcHelicopterEncounter.cloneRecognizes(e))return game.items().helicopterEncounter().cloneImage(e);
        if(LbcShishEncounter.managed(e))return game.items().shishEncounter().cloneImage(e);
        if(LbcApocalypseActors.managed(e))return game.items().apocalypseActors().cloneImage(e);
        return null;
    }
    static LbcSpellSource.Source rebase(LbcSpellSource.Source source,UUID original,Entity clone) {
        if(source==null)return null;
        return new LbcSpellSource.Source(original.equals(source.emitter())?clone.getUUID():source.emitter(),
                source.author(),source.epoch(),source.cause(),source.excluded(),source.persistent(),source.unownedMobs(),original.equals(source.emitter())&&(source.author()==null||source.helmetCredit())&&source.unownedMobs()&&clone instanceof net.minecraft.world.entity.LivingEntity living?living:source.nativeEmitter(),source.helmetCredit());
    }
    /** A reviewed presentation child's native serialized value, with a fresh construction identity. */
    static final class Presentation {
        private final CompoundTag nbt;
        private final net.minecraft.world.entity.EntityType<?> type;
        private final Class<?> bodyClass;
        final net.minecraft.world.phys.Vec3 position;
        Presentation(Entity child) {
            var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,child.registryAccess());
            child.saveWithoutId(out);nbt=out.buildResult().copy();nbt.remove("UUID");nbt.remove("uuid");nbt.remove("Passengers");
            type=child.getType();bodyClass=child.getClass();position=child.position();
        }
        Entity create(ServerLevel level) {
            var e=type.create(level,EntitySpawnReason.COMMAND);
            if(e==null||e.getClass()!=bodyClass)throw new IllegalStateException("Unreviewed encounter presentation class");
            UUID uuid=e.getUUID();int nativeId=e.getId();
            e.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),nbt.copy()));
            e.setUUID(uuid);if(e.getId()!=nativeId)throw new IllegalStateException("Presentation load changed native ID");
            return e;
        }
    }
}
