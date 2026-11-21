package me.skywars2.game;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.skywars2.SkyWars2;
import me.skywars2.game.genie.GenieScriptItem;
import me.skywars2.game.genie.script.ScriptRepository;
import me.skywars2.game.match.checkpoints.MatchPlayerCheckpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/** The remaining lifetime of a real gift body survives both native NBT load and a whole-match rewind. */
final class GenieVanityChecks {
    private static Fixture active;

    static void run(GameController game) {
        require(active==null,"exclusive fixture");
        var fixture=new Fixture(game);
        try { fixture.begin(); active=fixture; }
        catch(Throwable error) {
            try { fixture.base.close(); } catch(Throwable cleanup) { error.addSuppressed(cleanup); }
            throw error;
        }
    }

    static void tick(GameController game) {
        var fixture=active;
        if(fixture==null||fixture.game!=game)return;
        try {
            if(!fixture.step())return;
            active=null;fixture.base.close();
            SkyWars2.LOGGER.info("[check] PASS genie-vanity-lifetime (native gift launch, real NBT reload, full paused rewind, same actor and exact sixty active ticks)");
        } catch(Throwable error) {
            active=null;
            try { fixture.base.close(); } catch(Throwable cleanup) { error.addSuppressed(cleanup); }
            SkyWars2.LOGGER.error("[check] FAIL genie-vanity-lifetime",error);
        }
    }

    private static void require(boolean ok,String why) {
        if(!ok)throw new IllegalStateException("Genie Vanity lifetime: "+why);
    }

    private static final class Fixture {
        private static final int LIFE=60;
        final GameController game;
        final WoolBucketChecks.Fixture base;
        final CompletableFuture<ScriptRepository.Loaded> program;
        ItemStack gift;
        Cow body;
        UUID id;
        Vec3 launchPoint,capturedPosition;
        int phase,beforeReload,capturedNativeTicks;

        Fixture(GameController game) {
            this.game=game;
            program=game.items().genie().runtime().scripts().prepare("""
                    {"version":1,"state":{},"handlers":{"USE_REPLACE":"launch"},"functions":{"launch":{
                    "parameters":["event"],"code":[
                    ["load","state"],["const","id"],["const","minecraft:cow"],
                    ["const",null],
                    ["const",[0,0,0.6]],["const",60],["call","entity.launch",4],["put"],["return"]]}}}
                    """);
            base=new WoolBucketChecks.Fixture(game,true);
        }

        void begin() {
            base.prepareParticipants();
            var start=base.resident(false);
            base.move(base.owner,start);base.move(base.other,start.add(0,0,-4));
            var feet=BlockPos.containing(start);
            for(var pos:BlockPos.betweenClosed(feet.offset(-2,-1,-2),feet.offset(2,4,5)))
                if(!base.level.getBlockState(pos).isAir())base.set(pos.immutable(),Blocks.AIR.defaultBlockState());
            base.owner.setYRot(0);base.owner.setXRot(0);
        }

        void checkSource() {
            var source=game.items().spellSources().source(body);
            require(source!=null&&source.emitter().equals(id)&&source.author().equals(base.owner.getUUID()),
                    "physical native emitter and gift author rebind to the saved actor");
        }

        String lifetimeDetails() {
            return " reason="+body.getRemovalReason()+" position="+body.position()
                    +" ticks before reload="+beforeReload+" captured native="+capturedNativeTicks
                    +" current native="+body.tickCount;
        }

        boolean step() {
            base.client.acknowledge();base.audience.acknowledge();
            require(game.server().getTickCount()-base.started<1000,"bounded full native coordinator phase="+phase);
            if(phase==0){
                if(!program.isDone()||!base.level.areEntitiesLoaded(ChunkPos.pack(base.owner.getBlockX()>>4,base.owner.getBlockZ()>>4)))return false;
                gift=GenieScriptItem.construct(program.join(),"crossbow","Short-lived herd","Launches an ordinary cow.",1);
                base.owner.setItemInHand(InteractionHand.MAIN_HAND,gift);
                base.owner.connection.handleClientTickEnd(ServerboundClientTickEndPacket.INSTANCE);
                base.owner.connection.handleUseItem(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND,21701,0,0));
                phase=1;return false;
            }
            if(phase==1){
                var state=GenieScriptItem.state(gift);
                if(!state.has("id"))return false;
                require(!state.get("id").isJsonNull(),"real scripted use emits a native gift actor: "+state);
                id=UUID.fromString(state.get("id").getAsString());
                var actor=base.level.getEntity(id);
                require(actor instanceof Cow,"native gift is an actual cow");
                body=(Cow)actor;base.own(body);launchPoint=body.position();checkSource();
                require(!body.isNoGravity()&&!body.isNoAi(),"native launch retains ordinary gravity and AI");
                phase=2;return false;
            }
            if(phase==2){
                require(body.isAlive(),"control cow remains alive while demonstrating natural launch motion");
                if(body.tickCount<4)return false;
                require(body.position().distanceTo(launchPoint)>.5,"real launch actually moved during native ticks");
                // Isolate expiry from damage, ordinary AI and travel to unrelated sections.
                body.setNoAi(true);body.setNoGravity(true);body.setDeltaMovement(Vec3.ZERO);
                beforeReload=body.tickCount;
                var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,base.level.registryAccess());
                body.saveWithoutId(output);
                body.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                var reloaded=new Cow(EntityTypes.COW,base.level);
                reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING,base.level.registryAccess(),output.buildResult()));
                require(reloaded.getUUID().equals(id)&&reloaded.tickCount==0&&base.level.addFreshEntity(reloaded),
                        "real serialized lifetime attachment survives native body reload with transient ticker reset");
                body=reloaded;body.setInvulnerableTime(80);base.own(reloaded);phase=3;return false;
            }
            if(phase==3){
                require(!body.isRemoved(),"reloaded actor survives until full capture"+lifetimeDetails());
                if(body.tickCount<6)return false;
                capturedNativeTicks=body.tickCount;capturedPosition=body.position();
                require(beforeReload>0&&capturedNativeTicks>0&&beforeReload+capturedNativeTicks<LIFE,
                        "positive active age accumulated before and after ordinary reload");
                base.pause();
                require(game.vanity().requestCapture(base.owner,false),"actual whole-match capture during native Time Stop");
                phase=4;return false;
            }
            if(phase==4){
                require(body.tickCount==capturedNativeTicks,"local Time Stop holds the saved native ticker");
                if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;
                base.discard(body);
                require(body.isRemoved()&&base.level.getEntity(id)==null,"actual post-snapshot teardown removes the gift");
                require(game.vanity().requestRestoreForCheck(),"actual whole-match restore accepted");
                phase=5;return false;
            }
            if(phase==5){
                for(var player:game.server().getPlayerList().getPlayers())
                    if(player.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
                if(game.vanity().busy())return false;
                require(base.level.getEntity(id)==body&&!body.isRemoved()&&body.tickCount==capturedNativeTicks
                        &&body.position().equals(capturedPosition),"canonical restore reunites actor, position and exact native ticker"+lifetimeDetails());
                require(game.items().timeStop().stopped(base.level),"the saved local Time Stop remains active");
                game.vanity().stop();base.thaw();phase=6;return false;
            }
            int elapsed=beforeReload+body.tickCount;
            if(!body.isRemoved()){
                require(elapsed<LIFE,"restored gift still present at the exact lifetime deadline"+lifetimeDetails());
                return false;
            }
            require(body.getRemovalReason()==Entity.RemovalReason.DISCARDED&&elapsed==LIFE&&base.level.getEntity(id)==null,
                    "native lifetime removes the restored gift at exactly sixty active ticks"+lifetimeDetails());
            return true;
        }
    }
}
