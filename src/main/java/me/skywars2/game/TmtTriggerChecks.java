package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraft.world.scores.ScoreHolder;

/** Real native costs, projectiles, input boundaries and persistent private trigger definitions. */
public final class TmtTriggerChecks {
    private TmtTriggerChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("TMT triggers: "+why);}
    private static int seed;
    private static ItemStack fire(int carrier,LbcTmtTriggers.Input input){
        var profile=LbcWands.tmtAbilities().stream().filter(p->p.nativeItem().toString().equals("skywars2:fire_wand")).findFirst().orElseThrow();
        var payload=profile.inMode(0).data().copyTag();var bindings=new CompoundTag();bindings.putString("USE",input.name());payload.put(LbcTmtTriggers.KEY,bindings);
        var stack=LbcTmtItems.prototype(carrier);var data=stack.get(DataComponents.CUSTOM_DATA).copyTag();
        data.putInt("tmt_texture_seed",++seed);data.putInt(LbcTmtTriggers.SCHEMA,1);
        LbcTmtAbilities.write(data,List.of(new LbcTmtAbilities.Ability("wand/stfire",CustomData.of(payload))));
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return stack;
    }
    private static Set<UUID> bodies(WoolBucketChecks.Fixture f){var ids=new HashSet<UUID>();for(var body:f.level.getAllEntities())ids.add(body.getUUID());return ids;}
    private static List<Entity> fresh(WoolBucketChecks.Fixture f,Set<UUID> before){
        var result=new ArrayList<Entity>();for(var body:f.level.getAllEntities())if(!before.contains(body.getUUID())){result.add(body);f.own(body);}return result;
    }
    private static List<Entity> cast(WoolBucketChecks.Fixture f,ItemStack source,Runnable event){
        var mana=f.game.items().mana();f.owner.stopUsingItem();f.owner.setItemInHand(InteractionHand.MAIN_HAND,source);f.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
        mana.set(f.owner,1000);var before=bodies(f);event.run();var created=fresh(f,before);
        require(created.stream().filter(SmallFireball.class::isInstance).count()==3,"existing native Fire emits three true projectiles");
        require(mana.get(f.owner)==900,"one ordinary native100-mana debit");
        require(!LbcTmtAbilities.active(source)&&!ItemData.contains(source,LbcTmtAbilities.ACTIVE),"temporary native view closes");
        for(var body:created)f.discard(body);
        return List.copyOf(created);
    }
    public static void run(GameController game){synchronous(game);TmtReboundChecks.run(game);beginTimed(game);}
    private static void synchronous(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();f.move(f.owner,f.home.add(0,20,0));f.owner.setYRot(0);f.owner.setXRot(0);
            var mana=game.items().mana();mana.maximum(f.owner,10000);
            var price=mana.score(ScoreHolder.forNameOnly("stfire_wand_mana_use"),"lbc.math");int oldPrice=price.get();price.set(100);
            try{
                var random=RandomSource.create(83501);var carriers=new HashSet<net.minecraft.world.item.Item>();
                for(int i=0;i<128;i++){
                    var generated=game.items().tmt().generate(random);carriers.add(generated.getItem());
                    require(LbcTmtTriggers.modern(generated),"generation installs the native trigger schema");
                    var permitted=LbcTmtTriggers.inputs(generated);
                    for(var property:LbcTmtAbilities.read(generated))for(var pair:property.inputs().entrySet())
                        require(permitted.contains(pair.getValue()),"every generated input is reachable on its actual carrier");
                    var ops=f.level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
                    var restored=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,generated).getOrThrow()).getOrThrow();
                    require(ItemStack.matches(generated,restored),"binding/native parameter codec roundtrip");
                }
                require(carriers.size()==5,"every native transport occurs in the generator");
                for(var family:game.items().tmt().catalog().families())if(family.events().contains(LbcTmtCatalog.Event.USING))
                    require(!LbcTmtTriggers.actions(family).contains(LbcTmtTriggers.Action.INCOMING),"sustained native shield condition is not an invented instant incoming action");
                var metadata=fire(0,LbcTmtTriggers.Input.JUMP);var skin=metadata.get(DataComponents.ITEM_MODEL);
                LbcTmtAbilities.invoke(metadata,"wand/stfire",()->{
                    var data=metadata.get(DataComponents.CUSTOM_DATA).copyTag();data.remove(LbcTmtTriggers.KEY);data.putInt("mode",3);
                    metadata.set(DataComponents.CUSTOM_DATA,CustomData.of(data));metadata.set(DataComponents.ITEM_MODEL,net.minecraft.resources.Identifier.parse("minecraft:stone"));return true;
                });
                require(LbcTmtAbilities.read(metadata).getFirst().inputs().get(LbcTmtTriggers.Action.USE)==LbcTmtTriggers.Input.JUMP
                        &&Objects.equals(skin,metadata.get(DataComponents.ITEM_MODEL)),"native mode/presentation writer cannot erase bindings or generated skin");
                for(var input:LbcTmtTriggers.Input.values()){
                    int carrier=input==LbcTmtTriggers.Input.THROW?2:input==LbcTmtTriggers.Input.EAT?4:input==LbcTmtTriggers.Input.SHOT?1:0;
                    var item=fire(carrier,input);
                    boolean sneak=input==LbcTmtTriggers.Input.SNEAK||input==LbcTmtTriggers.Input.SNEAK_PRESS;
                    f.owner.setShiftKeyDown(sneak);
                    cast(f,item,()->LbcTmtTriggers.input(f.owner,input));
                    require(f.owner.isShiftKeyDown()==sneak,"automatic native action never changes real input flags");
                    f.owner.setShiftKeyDown(false);
                }
                var jumping=fire(0,LbcTmtTriggers.Input.JUMP);f.owner.setItemInHand(InteractionHand.MAIN_HAND,jumping);mana.set(f.owner,1000);
                var before=bodies(f);f.owner.gameMode.useItem(f.owner,f.level,jumping,InteractionHand.MAIN_HAND);
                require(mana.get(f.owner)==1000&&fresh(f,before).isEmpty(),"rebound jump property does not also cast on old RMB");
                cast(f,jumping,()->GroundJumpEvents.jumped(f.owner));
                LbcTmtTriggers.input(f.owner,LbcTmtTriggers.Input.JUMP);require(mana.get(f.owner)==900,"same input/frame cannot double-run a private property");
                var thrown=fire(2,LbcTmtTriggers.Input.THROW);
                var thrownBodies=cast(f,thrown,()->require(f.owner.gameMode.useItem(f.owner,f.level,thrown,InteractionHand.MAIN_HAND).consumesAction(),"actual native snowball launch"));
                require(thrownBodies.stream().filter(Snowball.class::isInstance).count()==1,"THROW follows one real native snowball publication");
                require(thrown.isEmpty(),"last native throwable pays its one actual debit");
                var eating=fire(4,LbcTmtTriggers.Input.EAT);
                cast(f,eating,()->eating.finishUsingItem(f.level,f.owner));require(eating.isEmpty(),"last native food input still fires before its debit");
                var lastBow=fire(1,LbcTmtTriggers.Input.SHOT);lastBow.setDamageValue(lastBow.getMaxDamage()-1);
                var arrows=new ItemStack(net.minecraft.world.item.Items.ARROW,5);f.owner.getInventory().setItem(9,arrows);
                cast(f,lastBow,()->((net.minecraft.world.item.BowItem)lastBow.getItem()).releaseUsing(lastBow,f.level,f.owner,lastBow.getUseDuration(f.owner)-20));
                require(lastBow.isEmpty()&&f.owner.getMainHandItem().isEmpty()&&arrows.getCount()==4,"captured final broken bow fires its assigned SHOT property without restoring a retired inventory item");
                f.owner.getInventory().setItem(9,ItemStack.EMPTY);
                f.owner.removeAllEffects();f.owner.setAbsorptionAmount(0);f.owner.setInvulnerableTime(0);f.owner.damageCooldownTime=0;
                cast(f,fire(0,LbcTmtTriggers.Input.HURT),()->require(f.owner.hurtServer(f.level,f.owner.damageSources().generic(),1),"actual accepted incoming damage"));
                var target=new Cow(EntityTypes.COW,f.level);target.setPos(f.owner.position().add(0,0,2));target.setNoAi(true);target.setNoGravity(true);target.setHealth(.1f);f.level.addFreshEntity(target);f.own(target);
                cast(f,fire(0,LbcTmtTriggers.Input.HIT),()->{f.owner.attack(target);require(target.isDeadOrDying(),"real native lethal melee input");});
                var spellTarget=new Cow(EntityTypes.COW,f.level);spellTarget.setPos(f.owner.position().add(0,0,2));spellTarget.setNoAi(true);spellTarget.setNoGravity(true);f.level.addFreshEntity(spellTarget);f.own(spellTarget);
                var hitOnly=fire(0,LbcTmtTriggers.Input.HIT);f.owner.setItemInHand(InteractionHand.MAIN_HAND,hitOnly);mana.set(f.owner,1000);before=bodies(f);
                var bolt=new net.minecraft.world.damagesource.DamageSource(f.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getOrThrow(LbcDamage.ELECTRO_BOLT),f.owner);
                require(spellTarget.hurtServer(f.level,bolt,100)&&spellTarget.isDeadOrDying(),"real credited lethal Tesla source");
                require(mana.get(f.owner)==1000&&fresh(f,before).stream().noneMatch(SmallFireball.class::isInstance),"credited spell kill never becomes melee input");
                var rawTarget=new Cow(EntityTypes.COW,f.level);rawTarget.setPos(f.owner.position().add(0,0,2));rawTarget.setNoAi(true);rawTarget.setNoGravity(true);f.level.addFreshEntity(rawTarget);f.own(rawTarget);
                before=bodies(f);var rawSource=f.owner.damageSources().playerAttack(f.owner);
                rawTarget.getCombatTracker().recordDamage(rawSource,rawTarget.getHealth());rawTarget.setHealth(0);rawTarget.die(rawSource);
                require(mana.get(f.owner)==1000&&fresh(f,before).stream().noneMatch(SmallFireball.class::isInstance),"raw HP death does not fabricate a HIT missing from the same nonlethal source");
                var pos=BlockPos.containing(f.owner.position().add(1,-1,0));f.set(pos,Blocks.STONE.defaultBlockState());
                cast(f,fire(0,LbcTmtTriggers.Input.MINING),()->require(f.owner.gameMode.destroyBlock(pos),"actual successful native block destruction"));
                f.set(pos,Blocks.STONE.defaultBlockState());var blockItem=fire(0,LbcTmtTriggers.Input.BLOCK_USE);
                cast(f,blockItem,()->f.owner.gameMode.useItemOn(f.owner,f.level,blockItem,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false)));
                cast(f,fire(0,LbcTmtTriggers.Input.LEFT_CLICK),()->f.owner.connection.handlePunch(net.minecraft.network.protocol.game.ServerboundPunchPacket.INSTANCE));
                f.owner.setShiftKeyDown(false);f.owner.setLastClientInput(net.minecraft.world.entity.player.Input.EMPTY);
                var pressed=fire(0,LbcTmtTriggers.Input.SNEAK_PRESS);
                var shifted=new net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(new net.minecraft.world.entity.player.Input(false,false,false,false,false,true,false));
                cast(f,pressed,()->f.owner.connection.handlePlayerInput(shifted));
                f.owner.connection.handlePlayerInput(shifted);require(mana.get(f.owner)==900,"held/duplicate native Shift input is not a second press");
                f.owner.setShiftKeyDown(false);f.owner.setLastClientInput(net.minecraft.world.entity.player.Input.EMPTY);
            }finally{price.set(oldPrice);f.owner.setShiftKeyDown(false);}
        }
    }
    private static Timed running;
    private static void beginTimed(GameController game){require(running==null,"one scheduled native input fixture");var next=new Timed(game);running=next;try{next.begin();}catch(RuntimeException|Error failure){running=null;next.close();throw failure;}}
    public static void tick(GameController game){
        if(running==null)return;var current=running;
        try{if(current.step()){running=null;current.close();SkyWars2.LOGGER.info("[check] PASS tmt-triggers (128 generated/codecs; all14 inputs/native factories; true tick/sneak/sprint/press/punch, last throw/eat, hurt/lethal melee/mining/block, no spell-kill melee, mode/skin integrity and TimeStop)");}}
        catch(RuntimeException|Error error){running=null;try{current.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL tmt-triggers",error);}
    }
    private static final class Timed implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final Set<UUID> initial;final int oldPrice;
        int stage,when,started;boolean closed;
        Timed(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);initial=bodies(f);oldPrice=game.items().mana().score(ScoreHolder.forNameOnly("stfire_wand_mana_use"),"lbc.math").get();}
        void begin(){f.prepareParticipants();f.move(f.owner,f.home.add(0,25,0));f.owner.setYRot(0);f.owner.setXRot(0);game.items().mana().maximum(f.owner,10000);game.items().mana().baseRegen(f.owner,0);game.items().mana().score(ScoreHolder.forNameOnly("stfire_wand_mana_use"),"lbc.math").set(100);started=game.server().getTickCount();prepare(0);}
        void prepare(int next){
            stage=next;when=game.server().getTickCount();f.owner.setShiftKeyDown(next==1);f.owner.setSprinting(next==2);
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,fire(0,next==1?LbcTmtTriggers.Input.SNEAK:next==2?LbcTmtTriggers.Input.SPRINT:LbcTmtTriggers.Input.TICK));
            game.items().mana().set(f.owner,1000);if(next==3)f.owner.entityTags().add("time_stopped");
        }
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();require(game.server().getTickCount()-started<40,"bounded real scheduled ticks");
            int elapsed=game.server().getTickCount()-when;if(elapsed<2)return false;
            int spent=1000-game.items().mana().get(f.owner);
            require(stage==3?spent==0:spent>=100&&spent<=300&&spent%100==0,"real registered input ticker applies only its active condition, one native cost per frame stage="+stage+" spent="+spent);
            if(stage==3)return true;prepare(stage+1);return false;
        }
        public void close(){if(closed)return;closed=true;f.owner.entityTags().remove("time_stopped");f.owner.setShiftKeyDown(false);f.owner.setSprinting(false);game.items().mana().score(ScoreHolder.forNameOnly("stfire_wand_mana_use"),"lbc.math").set(oldPrice);for(var body:fresh(f,initial))f.discard(body);f.close();}
    }
}
