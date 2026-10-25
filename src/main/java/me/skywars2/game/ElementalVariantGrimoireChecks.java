package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Native consumed variants: actual item input, distinct payloads, terrain, queues and stop cleanup. */
public final class ElementalVariantGrimoireChecks {
    private ElementalVariantGrimoireChecks() { }
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Elemental variant: "+why);}
    private static final class Fixture implements AutoCloseable {
        final ElementalGrimoireChecks.Fixture base;
        final Map<BlockPos,BlockState> terrain=new LinkedHashMap<>();
        final Vec3 origin;final BlockPos contact;
        boolean retain;
        Fixture(GameController game) {
            base=new ElementalGrimoireChecks.Fixture(game);base.initialize();origin=base.owner.position();
            contact=BlockPos.containing(base.owner.getEyePosition().add(0,0,5));
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(origin.add(-8,-7,-8)),BlockPos.containing(origin.add(8,12,68)))) {
                require(base.owner.level().hasChunkAt(pos),"resident terrain fixture");
                terrain.put(pos.immutable(),base.owner.level().getBlockState(pos));
            }
        }
        void reset() {
            base.owner.stopRiding();base.clear();
            for(var e:List.copyOf(entities()))if(!base.before.contains(e)&&(e instanceof Projectile p&&p.getOwner()==base.owner||e instanceof net.minecraft.world.entity.item.ItemEntity))e.discard();
            for(var entry:terrain.entrySet())if(!base.owner.level().getBlockState(entry.getKey()).equals(entry.getValue()))base.owner.level().setBlock(entry.getKey(),entry.getValue(),2|16|32);
            base.owner.removeEffect(MobEffects.RESISTANCE);
            base.owner.teleportTo(base.owner.level(),origin.x,origin.y,origin.z,Set.of(),0,0,true);
            base.owner.setDeltaMovement(Vec3.ZERO);base.recording.packets.clear();
        }
        List<Entity> entities(){var out=new ArrayList<Entity>();base.owner.level().getAllEntities().forEach(out::add);return out;}
        List<Projectile> projectiles(){return entities().stream().filter(e->!base.before.contains(e)&&!e.isRemoved()&&e instanceof Projectile p&&p.getOwner()==base.owner).map(Projectile.class::cast).toList();}
        boolean sound(String id) {return base.recording.packets.stream().filter(ClientboundSoundPacket.class::isInstance).map(ClientboundSoundPacket.class::cast).anyMatch(p->p.getSound().value().location().toString().equals(id));}
        long changed(){return terrain.entrySet().stream().filter(e->!base.owner.level().getBlockState(e.getKey()).equals(e.getValue())).count();}
        void prepare(LbcElementalGrimoires.Kind kind) {
            if(switch(kind){case RUBY_RAY,RUBY_HOLE,ELECTRO_RAY,STONE_RAY,STONE_SPIKES,FIRE_RAY,BLIND_RAIN -> true;default -> false;})
                base.owner.level().setBlock(contact,Blocks.STONE.defaultBlockState(),2|16|32);
        }
        void payload(LbcElementalGrimoires.Kind kind) {
            var game=base.game;var owner=base.owner;var shots=projectiles();
            int expected=switch(kind){case AIR_BURST,BLIND_PHANTOM -> 4;case TERRA_FAST,AIR_FAST,AIR_HOMING,ACID_BURNING,TELEPORT_INVERTED,TELEPORT_CLOUD,FIRE_BIG -> 1;default -> 0;};
            require(shots.size()==expected,"source projectile count "+kind+" / "+shots.size());
            for(var p:shots)require(game.items().currentEpoch(p)&&p.getOwner()==owner&&p.getDeltaMovement().lengthSqr()>0,"author/epoch/motion "+kind);
            switch(kind) {
                case RUBY_RAY -> require(owner.level().getBlockState(contact).isAir(),"cutting ray removes the actual obstacle");
                case RUBY_HOLE -> require(owner.level().getBlockState(contact).is(Blocks.BEDROCK),"hole creates its bedrock center");
                case TERRA_FAST -> require(shots.getFirst() instanceof LbcTerra.TerraProjectile&&Math.abs(shots.getFirst().getDeltaMovement().length()-7.5)<1e-7,"fast Terra native speed");
                case ELECTRO_RAY -> require(game.items().wands().electroCount()==0&&sound("minecraft:entity.firework_rocket.twinkle"),"instant electrical ray, no moving traces");
                case AIR_BURST,AIR_FAST,AIR_HOMING -> {
                    int mode=kind==LbcElementalGrimoires.Kind.AIR_BURST?1:kind==LbcElementalGrimoires.Kind.AIR_FAST?2:3;
                    require(game.items().air().snapshot().stream().allMatch(p->p.mode==mode&&!p.trial),"correct Air mode without wand trial "+kind);
                }
                case ACID_BURNING -> require(shots.getFirst().entityTags().contains("on_fire")
                        &&game.items().clouds().acidAge(game.items().clouds().acidSnapshot().getFirst())==1
                        &&LbcClouds.acidLifetime(game.items().clouds().acidSnapshot().getFirst())==300
                        &&shots.getFirst().getDeltaMovement().normalize().distanceTo(owner.getLookAngle())<1e-6,"aligned burning big acid");
                case TELEPORT_INVERTED -> require(game.items().teleport().snapshot().getFirst().mode==2,"inverse teleport projectile");
                case TELEPORT_CLOUD -> require(owner.getVehicle()==shots.getFirst()&&shots.getFirst() instanceof Arrow
                        &&Math.abs(shots.getFirst().getY()-origin.y-.01)<1e-7
                        &&Math.abs(shots.getFirst().getDeltaMovement().length()-.5)<1e-7,"native owned cloud carrier and mount");
                case STONE_RAY,STONE_SPIKES -> require(changed()>8&&sound("minecraft:block.stone.place"),"ray places its actual rock/spike structure "+kind);
                case STONE_BRIDGE -> {
                    var start=BlockPos.containing(origin.add(0,-1,0));
                    require(owner.level().getBlockState(start).is(Blocks.CRYING_OBSIDIAN)
                            &&owner.level().getBlockState(start.offset(0,0,30)).is(Blocks.DIORITE),"complete 15-material bridge");
                }
                case STONE_SHIELD -> {var effect=owner.getEffect(MobEffects.RESISTANCE);require(effect!=null&&effect.getAmplifier()==4&&effect.getDuration()==100,"five-second Resistance V");}
                case FIRE_BIG -> require(shots.getFirst().getType()==EntityTypes.FIREBALL&&Math.abs(shots.getFirst().getDeltaMovement().length()-2.5)<1e-7,"large fireball native velocity");
                case FIRE_RAY -> require(terrain.keySet().stream().anyMatch(p->owner.level().getBlockState(p).is(Blocks.LAVA)),"concentrated ray creates terminal lava");
                case BLIND_GATLING -> require(game.items().blind().queued(owner)==20,"20 captured-owner queued shots");
                case BLIND_PHANTOM -> require(game.items().blind().snapshot().stream().allMatch(p->p.phantom&&p.lifetime==30&&!p.trial),"four phantom tar charges");
                case BLIND_RAIN -> require(game.items().blind().cloudCount(owner)==1,"one rain cloud at actual ray contact");
                default -> throw new IllegalArgumentException("variant "+kind);
            }
            if(switch(kind){case RUBY_RAY,RUBY_HOLE,ELECTRO_RAY,STONE_RAY,STONE_SPIKES,FIRE_RAY,BLIND_RAIN -> true;default -> false;})
                require(base.recording.packets.stream().anyMatch(ClientboundLevelParticlesPacket.class::isInstance),"native ray/terminal particles "+kind);
            for(var e:entities())require(base.before.contains(e)||e.getType()!=EntityTypes.SNOWBALL,"no throwaway carrier");
        }
        void casts() {
            for(var kind:LbcElementalGrimoires.variants()) {
                base.presentation(kind);
                for(var hand:InteractionHand.values())for(boolean shift:new boolean[]{false,true}) {
                    reset();prepare(kind);var stack=base.held(kind,hand,1);base.owner.setShiftKeyDown(shift);
                    base.mana.set(base.owner,shift?-400:0);int balance=base.mana.get(base.owner),used=base.owner.getStats().getValue(Stats.ITEM_USED.get(kind.item));
                    require(base.input(hand)&&stack.isEmpty()&&base.mana.get(base.owner)==balance,"last book consumed, no mana admission/debit "+kind+" "+hand+" shift="+shift);
                    require(base.owner.getStats().getValue(Stats.ITEM_USED.get(kind.item))==used+1,"one native use stat "+kind);
                    payload(kind);
                }
            }
            reset();base.owner.setShiftKeyDown(false);
            var main=base.held(LbcElementalGrimoires.Kind.BLIND_GATLING,InteractionHand.MAIN_HAND,1);
            var off=base.loot(LbcElementalGrimoires.Kind.AIR_HOMING);base.owner.setItemInHand(InteractionHand.OFF_HAND,off);
            require(base.input(InteractionHand.MAIN_HAND)&&main.isEmpty()&&off.isEmpty()
                    &&base.game.items().blind().queued(base.owner)==20&&base.game.items().air().count()==1,"parallel mixed consumed variants");
            reset();
            var stack=base.held(LbcElementalGrimoires.Kind.BLIND_GATLING,InteractionHand.MAIN_HAND,2);
            require(base.input(InteractionHand.MAIN_HAND)&&base.input(InteractionHand.MAIN_HAND)&&stack.isEmpty()
                    &&base.game.items().blind().queued(base.owner)==40,"repeated paid-in-books gatling casts accumulate");
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            base.game.items().blind().tick();base.game.items().blind().tick();
            require(base.game.items().blind().queued(base.owner)==39&&base.game.items().blind().count()==1,"captured queue fires after final book leaves hand");
            reset();
        }
        void guards() {
            for(var kind:LbcElementalGrimoires.variants()) {
                var stack=base.held(kind,InteractionHand.MAIN_HAND,2);
                base.game.items().timeStop().begin(base.owner.level(),40);
                try{require(!base.input(InteractionHand.MAIN_HAND)&&stack.getCount()==2&&base.count()==0,"time stop preserves "+kind);}
                finally{base.game.items().timeStop().end(base.owner.level());}
                base.field=new Marker(EntityTypes.MARKER,base.owner.level());base.field.setPos(base.owner.position());base.field.entityTags().add("stopper_magic");
                require(base.owner.level().addFreshEntity(base.field),"native antimagic fixture");base.game.items().magic().loaded(base.field);
                try{require(!base.input(InteractionHand.MAIN_HAND)&&stack.getCount()==2&&base.count()==0,"antimagic preserves "+kind);}
                finally{base.removeField();}
                stack=base.held(kind,InteractionHand.OFF_HAND,2);base.game.config().offhandWands=false;
                try{require(!base.input(InteractionHand.OFF_HAND)&&stack.getCount()==2&&base.count()==0,"disabled offhand preserves "+kind);}
                finally{base.game.config().offhandWands=true;}
            }
        }
        @Override public void close(){if(!retain)reset();base.retain=retain;base.close();}
    }
    private static List<Entity> retained;
    private static String retainedEpoch;
    private static ServerPlayerRef retainedOwner;
    private record ServerPlayerRef(UUID uuid) { }
    public static void run(GameController game) {
        try(var fixture=new Fixture(game)){fixture.casts();fixture.guards();}
        SkyWars2.LOGGER.info("[check] PASS elemental-variant-grimoires");
    }
    public static void stopProbe(GameController game) {
        require(retained==null,"exclusive retained fixture");
        try(var fixture=new Fixture(game)) {
            for(var kind:List.of(LbcElementalGrimoires.Kind.TERRA_FAST,LbcElementalGrimoires.Kind.AIR_HOMING,
                    LbcElementalGrimoires.Kind.ACID_BURNING,LbcElementalGrimoires.Kind.TELEPORT_INVERTED,
                    LbcElementalGrimoires.Kind.FIRE_BIG,LbcElementalGrimoires.Kind.BLIND_PHANTOM,
                    LbcElementalGrimoires.Kind.BLIND_GATLING,LbcElementalGrimoires.Kind.TELEPORT_CLOUD)) {
                fixture.base.held(kind,InteractionHand.MAIN_HAND,1);
                require(fixture.base.input(InteractionHand.MAIN_HAND),"retained actual input "+kind);
            }
            var actors=new ArrayList<Entity>(fixture.projectiles());
            actors.forEach(e->e.entityTags().add("time_stopped"));
            require(actors.size()==10&&game.items().blind().queued(fixture.base.owner)==20,"retained native actors and queue");
            retained=List.copyOf(actors);retainedEpoch=game.items().epoch();retainedOwner=new ServerPlayerRef(fixture.base.owner.getUUID());
            fixture.base.owner.stopRiding();fixture.retain=true;
        }
        SkyWars2.LOGGER.info("[check] PASS elemental-variant-grimoires-stop-probe");
    }
    public static void cleanup(GameController game) {
        var actors=retained;require(actors!=null&&!actors.isEmpty(),"retained consumed variant actors");
        try{var owner=game.server().getPlayerList().getPlayer(retainedOwner.uuid());
            require(!game.items().currentEpoch(retainedEpoch)&&actors.stream().allMatch(Entity::isRemoved)
                    &&owner!=null&&game.items().blind().queued(owner)==0&&game.items().teleport().cloudCount()==0,"actual stop retires variants, cloud and queue");
        }finally{actors.forEach(Entity::discard);retained=null;retainedEpoch=null;retainedOwner=null;}
        SkyWars2.LOGGER.info("[check] PASS elemental-variant-grimoires-cleanup");
    }
}
