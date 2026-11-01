package me.skywars2.game;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.TagParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Registry closure and actual native launches run in the isolated real-match fixture. */
public final class MobgunCatalogChecks {
    private MobgunCatalogChecks() { }
    private static void require(boolean ok,String why) {
        if(!ok) throw new IllegalStateException("Mobgun catalog: "+why);
    }
    /** Source Mob bodies may be passengers of an authored technical carrier (Shulker outcome48). */
    private static void collectMobs(CompoundTag data,JsonObject entry,Set<Identifier> mobs,Map<Identifier,JsonObject> entries) {
        var id=Identifier.parse(data.getStringOr("id",""));
        if(mobs.contains(id)) entries.putIfAbsent(id,entry);
        for(var passenger:data.getListOrEmpty("Passengers"))
            if(passenger instanceof CompoundTag child) collectMobs(child,entry,mobs,entries);
    }
    public static void run(GameController game) {
        require(game.playing(),"requires a real match");
        var owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
        var service=game.items().mobgun();
        require(service.ready(),"native catalog preparation completed");
        require(service.snapshot(owner).isEmpty(),"fixture starts without owned Mobgun actors");
        var saved=new RegenerationBeltChecks.Snapshot(game,owner);
        var mobs=new TreeSet<Identifier>(Comparator.comparing(Identifier::toString));
        // Constructor-only prototypes are never inserted into the fixture world or ticked.
        // Native instanceof includes MISC-category golems and excludes other living entities.
        for(var type:BuiltInRegistries.ENTITY_TYPE) {
            var id=BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if(!id.getNamespace().equals("minecraft")) continue;
            var prototype=type.create(game.maps().level(),EntitySpawnReason.COMMAND);
            if(prototype==null) continue; // Player/fishing-hook factories cannot create unowned bodies.
            try { if(prototype instanceof Mob) mobs.add(id); }
            finally { service.dispose(prototype); }
        }
        require(!mobs.isEmpty(),"native Mob prototype enumeration is nonempty");
        var entries=new HashMap<Identifier,JsonObject>();
        int launched=0; int previousCooldown=service.cooldown(owner);
        try(var input=MobgunCatalogChecks.class.getResourceAsStream("/skywars2/mobgun_presets.json")) {
            require(input!=null,"packaged source catalog exists");
            var catalog=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            for(var raw:catalog.getAsJsonArray("outcomes")) {
                var entry=raw.getAsJsonObject();
                var data=TagParser.parseCompoundFully(entry.get("nbt").getAsString());
                collectMobs(data,entry,mobs,entries);
            }
            var missing=new TreeSet<String>();
            for(var id:mobs) if(!entries.containsKey(id)) missing.add(id.toString());
            require(missing.isEmpty(),"every native Mob type is represented, missing="+missing);
            // Only Mob subclasses qualify: mannequin, armor stand and player remain outside closure.
            require(!mobs.contains(Identifier.withDefaultNamespace("mannequin"))
                    && !mobs.contains(Identifier.withDefaultNamespace("armor_stand"))
                    && !mobs.contains(Identifier.withDefaultNamespace("player")),"native Mob versus other living/entity boundary");
            owner.setGameMode(GameType.SURVIVAL); owner.setShiftKeyDown(false);
            owner.teleportTo(game.maps().level(),.5,261,50.5,Set.of(),0,0,true); owner.hasChangedDimension();
            owner.setNoGravity(true); owner.setDeltaMovement(Vec3.ZERO); owner.setYRot(0); owner.setXRot(0);
            for(var id:mobs) {
                var entry=entries.get(id); int outcome=entry.get("outcome").getAsInt();
                require(service.launch(owner,outcome,0),"actual native Mob launch "+id+" outcome="+outcome);
                var actors=service.snapshot(owner);
                var roots=actors.stream().filter(e->!e.isPassenger()).toList();
                require(roots.size()==1,"one native root for "+id);
                var root=roots.getFirst();
                var bodies=root.getSelfAndPassengers().toList();
                var matches=bodies.stream().filter(body->BuiltInRegistries.ENTITY_TYPE.getKey(body.getType()).equals(id)).toList();
                require(matches.size()==1 && matches.getFirst() instanceof Mob,"exact native Mob body in source carrier/passenger tree "+id);
                var mob=(Mob)matches.getFirst();
                require(!mob.isNoAi(),"native AI retained "+id);
                if(mob!=root) require(mob.isPassenger() && mob.getDeltaMovement().distanceTo(Vec3.ZERO)<.001,
                        "native passenger retains zero own velocity while its carrier launches "+id);
                require(root.position().distanceTo(owner.getEyePosition().add(0,0,entry.get("offset").getAsDouble()))<.001
                        && root.getDeltaMovement().distanceTo(new Vec3(0,0,3))<.001,"source offset and launch velocity "+id);
                for(var actor:bodies) {
                    require(actors.contains(actor) && service.owner(actor)==owner && service.isManaged(actor)
                            && actor.entityTags().contains(LbcMobgun.ACTOR) && !actor.entityTags().contains("prestartf")
                            && game.items().currentEpoch(actor) && !actor.shouldBeSaved(),"owner/epoch/actor/no-save on native body "+id);
                }
                service.forget(owner);
                require(actors.stream().allMatch(Entity::isRemoved) && service.snapshot(owner).isEmpty(),"complete immediate native cleanup "+id);
                launched++;
            }
        } catch(java.io.IOException | com.mojang.brigadier.exceptions.CommandSyntaxException error) {
            throw new IllegalStateException("Cannot read Mobgun native catalog fixture",error);
        } finally {
            service.forget(owner); service.sourceCooldown(owner,previousCooldown); saved.restore(game);
        }
        SkyWars2.LOGGER.info("[check] PASS mobgun-catalog nativeMobTypes={} actualNativeLaunches={}",mobs.size(),launched);
    }
}
