package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;

import java.util.*;

/** Native ordinary combined staff checks. Explicit ticks isolate producer scheduling from payload motion. */
public final class SauvojenChecks {
    private SauvojenChecks() { }
    private static void require(boolean condition,String detail) {
        if(!condition)throw new IllegalStateException("Sauvojen: "+detail);
    }
    private static void near(Vec3 actual,Vec3 expected,String detail) {
        require(actual.distanceToSqr(expected)<1e-9,detail+" actual="+actual+" expected="+expected);
    }
    private static final class Fixture implements AutoCloseable {
        final ElementalGrimoireChecks.Fixture base;
        final GameController game;
        final LbcSauvojen service;
        final List<Cow> cows=new ArrayList<>();
        final Map<AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();
        final int cost,stat;
        boolean retain;
        Fixture(GameController game) {this(game,false);}
        Fixture(GameController game,boolean existingStopProbes) {
            this.game=game;service=game.items().sauvojen();
            require(service.count()==0,"exclusive empty producer service");
            base=new ElementalGrimoireChecks.Fixture(game,existingStopProbes);
            cost=base.mana.score(ScoreHolder.forNameOnly("sauvojen_wand_mana_use"),"lbc.math").get();
            stat=base.owner.getStats().getValue(Stats.ITEM_USED.get(LbcWands.SAUVOJEN));
            for(var a:game.server().getAdvancements().getAllAdvancements()) {
                var complete=new HashSet<String>();base.owner.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(complete::add);
                advancements.put(a,complete);
            }
            base.initialize();base.mana.score(ScoreHolder.forNameOnly("sauvojen_wand_mana_use"),"lbc.math").set(2500);
        }
        ItemStack held(InteractionHand hand) {
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            base.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            var loot=game.lbc().rollTable(base.owner.level(),base.owner.position(),base.owner,"lbc:ported/sauvojen_wand");
            require(loot.size()==1&&loot.getFirst().is(LbcWands.SAUVOJEN),"ordinary source leaf for used stack");
            var stack=loot.getFirst();base.owner.setItemInHand(hand,stack);return stack;
        }
        void clear(){service.stop();base.clear();}
        LbcSauvojen.Drone launch(LbcSauvojen.Kind kind) {
            var stack=held(InteractionHand.MAIN_HAND);service.cast(base.owner,InteractionHand.MAIN_HAND,stack,kind.ordinal());
            require(service.count()==1,"one native producer "+kind);return service.snapshot().getFirst();
        }
        Cow cow(Vec3 position) {
            var cow=new Cow(EntityTypes.COW,base.owner.level());cow.setPos(position);cow.setNoAi(true);cow.setNoGravity(true);
            require(base.owner.level().addFreshEntity(cow),"native cow fixture");cows.add(cow);return cow;
        }
        void removeCows(){for(var cow:cows){game.items().indirectKills().clear(cow);cow.discard();}cows.clear();}
        List<LbcTerra.TerraProjectile> terra() {
            var result=new ArrayList<LbcTerra.TerraProjectile>();
            for(var e:base.owner.level().getAllEntities())if(e instanceof LbcTerra.TerraProjectile p&&!p.isRemoved())result.add(p);
            return result;
        }
        final class Terrain implements AutoCloseable {
            final Map<BlockPos,MapManager.BlockSnapshot> cells=new LinkedHashMap<>();
            Terrain(BlockPos center,int radius) {
                for(var p:BlockPos.betweenClosed(center.offset(-radius,-radius,-radius),center.offset(radius,radius,radius))) {
                    require(base.owner.level().hasChunkAt(p)&&base.owner.level().getBlockState(p).isAir()&&base.owner.level().getBlockEntity(p)==null,"resident untouched air impact volume "+p);
                    cells.put(p.immutable(),game.maps().snapshot(base.owner.level(),p));
                }
            }
            void set(BlockPos p,net.minecraft.world.level.block.state.BlockState state) {
                require(cells.containsKey(p),"bounded fixture terrain mutation");
                base.owner.level().setBlock(p,state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS
                        |Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS|Block.UPDATE_SKIP_ON_PLACE);
            }
            @Override public void close(){cells.forEach((p,s)->game.maps().restoreSnapshot(base.owner.level(),p,s));}
        }
        void impacts() {
            var owner=base.owner;var level=owner.level();var stack=held(InteractionHand.MAIN_HAND);
            var center=BlockPos.containing(owner.getX(),owner.getY()+2,owner.getZ()+4);
            boolean tnt=level.getGameRules().get(GameRules.TNT_EXPLODES);int protection=game.config().tntProtection;
            var beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(beta!=null&&game.canUseGameplayItems(beta),"active allied player for native Air blast protection");
            var betaState=new GameChecks.WandThirdPlayerState(beta,base.mana);var oldOwnerTeam=owner.getTeam();var oldBetaTeam=beta.getTeam();
            var board=game.server().getScoreboard();require(board.getPlayerTeam("sw2_sauvojen_air")==null,"exclusive Air fixture team");
            var team=board.addPlayerTeam("sw2_sauvojen_air");
            try(var terrain=new Terrain(center,9)) {
                level.getGameRules().set(GameRules.TNT_EXPLODES,true,game.server());game.config().tntProtection=0;
                board.addPlayerToTeam(owner.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);
                Vec3 point=new Vec3(center.getX()+.5,center.getY()+.5,center.getZ());
                terrain.set(center,Blocks.OBSIDIAN.defaultBlockState());
                beta.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);beta.removeAllEffects();beta.setNoGravity(true);beta.setHealth(beta.getMaxHealth());
                var betaAt=point.add(2,-.5,-2);beta.teleportTo(level,betaAt.x,betaAt.y,betaAt.z,Set.of(),0,0,true);beta.setDeltaMovement(Vec3.ZERO);
                var enemy=cow(point.add(-2,-.5,-2));enemy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);enemy.setHealth(1000);
                var role=cow(point.add(1.5,-.5,-2));role.entityTags().add("strict_map_object");var greed=cow(point.add(-1.5,-.5,-3));greed.entityTags().add("sin_greed_active");
                float alliedHealth=beta.getHealth(),roleHealth=role.getHealth(),greedHealth=greed.getHealth();
                var entities=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(entities::add);
                base.recording.packets.clear();service.emit(owner,InteractionHand.MAIN_HAND,stack,LbcSauvojen.Kind.AIR,point.add(0,0,-.4),new Vec3(0,0,1));
                game.items().air().tick();
                long packets=base.recording.packets.stream().filter(p->p.getClass().getSimpleName().equals("ClientboundExplodePacket")).count();
                require(packets==3&&game.items().air().count()==0,"one source Air impact executes three actual native blasts/packets, actual="+packets);
                require(enemy.getHealth()<1000&&enemy.getKillCredit()==owner,"actual native Air blast damage retains caster cause");
                require(beta.getHealth()==alliedHealth&&beta.getDeltaMovement().equals(Vec3.ZERO)&&role.getHealth()==roleHealth&&greed.getHealth()==greedHealth,"all three native blasts protect allied player, role and active Greed");
                for(var entity:level.getAllEntities())if(!entities.contains(entity))require(entity.getType()!=EntityTypes.TNT&&entity.getType()!=EntityTypes.CREEPER,"Air uses native immediate blasts without spawned TNT/Creeper helpers");
                require(level.getBlockState(center).is(Blocks.OBSIDIAN),"native Air blast respects actual block resistance");
            } finally {
                level.getGameRules().set(GameRules.TNT_EXPLODES,tnt,game.server());game.config().tntProtection=protection;
                board.removePlayerTeam(team);if(oldOwnerTeam!=null)board.addPlayerToTeam(owner.getScoreboardName(),oldOwnerTeam);if(oldBetaTeam!=null)board.addPlayerToTeam(beta.getScoreboardName(),oldBetaTeam);
                betaState.restore(game);clear();removeCows();
            }
            for(double fraction:new double[]{.2,.8})try(var terrain=new Terrain(center,4)) {
                Vec3 origin=new Vec3(center.getX()+fraction,center.getY()+fraction,center.getZ()-1.2);
                terrain.set(center,Blocks.OBSIDIAN.defaultBlockState());
                var victim=cow(new Vec3(center.getX()+fraction+.9,center.getY()+fraction-.6,center.getZ()+.5));
                service.emit(owner,InteractionHand.MAIN_HAND,stack,LbcSauvojen.Kind.STONE,origin,new Vec3(0,0,1));
                var shot=game.items().stone().snapshot().getFirst();game.items().stone().tick();
                require(shot.isRemoved()&&game.items().stone().count()==0,"native flying Stone impact consumed once at fraction "+fraction);
                Vec3 point=shot.position();var lo=BlockPos.containing(point.add(-1.5,-1.5,-1.5));var hi=BlockPos.containing(point.add(1.5,1.5,1.5));
                for(var cell:terrain.cells.keySet()) {
                    boolean inside=cell.getX()>=lo.getX()&&cell.getX()<=hi.getX()&&cell.getY()>=lo.getY()&&cell.getY()<=hi.getY()&&cell.getZ()>=lo.getZ()&&cell.getZ()<=hi.getZ();
                    require(inside?level.getBlockState(cell).is(Blocks.STONE):level.getBlockState(cell).isAir(),"fractional source floor±1.5 strict entire cube and untouched outer volume "+fraction+" "+cell);
                }
                require(victim.getHealth()==victim.getMaxHealth(),"flying Stone placement has no donor direct damage payload");
                require(level.getBlockState(BlockPos.containing(victim.getEyePosition())).is(Blocks.STONE),"real Stone target eye overlaps newly placed owned cube");
                victim.setInvulnerableTime(0);victim.damageCooldownTime=0;
                require(victim.hurtServer(level,victim.damageSources().inWall(),1)&&victim.getHealth()==victim.getMaxHealth()-1&&victim.getKillCredit()==owner,"native source Stone suffocation inherits exact block author without lethal fixture side effects");
                clear();removeCows();
            } finally{clear();removeCows();}
            var shieldState=new GameChecks.WandThirdPlayerState(beta,base.mana);
            try {
                var feet=owner.position().add(0,3,6);
                beta.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);beta.removeAllEffects();beta.setNoGravity(true);
                beta.teleportTo(level,feet.x,feet.y,feet.z,Set.of(),180,0,true);beta.setDeltaMovement(Vec3.ZERO);
                beta.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHIELD));beta.startUsingItem(InteractionHand.MAIN_HAND);
                for(int tick=0;tick<6;tick++)beta.doTick();
                require(beta.isBlocking(),"actual flying-acid shield fixture finishes native use delay");
                var from=beta.position().add(0,.5,-1.8);var velocity=new Vec3(0,0,1.6);
                require(game.items().wands().shieldHit(owner,from,from.add(velocity))!=null,"actual segment crosses a raised hostile shield");
                service.emit(owner,InteractionHand.MAIN_HAND,stack,LbcSauvojen.Kind.ACID,from,new Vec3(0,0,1));
                var acid=game.items().clouds().acidSnapshot().getFirst();level.tickNonPassenger(acid);game.items().clouds().tick();
                require(!acid.isRemoved()&&beta.hasEffect(MobEffects.POISON),"source flying acid passes both swept shield and nearby shield effect checks");
                near(acid.position(),from.add(velocity),"unshielded source acid moves through shield on actual native tick");
            } finally{beta.stopUsingItem();shieldState.restore(game);clear();}
        }
        void payloads() {
            var owner=base.owner;var stack=held(InteractionHand.OFF_HAND);
            Vec3 origin=owner.getEyePosition().add(1.25,1.5,-1.75),direction=new Vec3(.3,.4,.5).normalize();
            Vec3 position=owner.position(),motion=owner.getDeltaMovement();float yaw=owner.getYRot(),pitch=owner.getXRot();
            base.mana.set(owner,4321);
            for(var kind:LbcSauvojen.Kind.values()) {
                service.emit(owner,InteractionHand.OFF_HAND,stack,kind,origin,direction);
                require(base.mana.get(owner)==4321&&stack.getCount()==1&&service.count()==0,"postpaid emission no second debit or producer "+kind);
                near(owner.position(),position,"emission preserves caster position "+kind);near(owner.getDeltaMovement(),motion,"emission preserves caster velocity "+kind);
                require(owner.getYRot()==yaw&&owner.getXRot()==pitch,"emission preserves camera "+kind);
                if(kind==LbcSauvojen.Kind.ELECTRO) {
                    var arcs=game.items().wands().electroSnapshot();int[] lifetimes={35,20,25,30};
                    require(arcs.size()==4&&base.count()==4,"exact four source electrical arcs");
                    for(int i=0;i<4;i++) {
                        var arc=arcs.get(i);near(arc.position,origin,"electrical origin "+i);
                        require(arc.owner==owner&&arc.lifetime==lifetimes[i]&&arc.age==0&&arc.trialOwner==null&&!arc.statusWild,"source electrical owner, lifetime, no trial or WILD "+i);
                        // Native directionFromRotation uses a float sine lookup, unlike the double aim vector.
                        require(Vec3.directionFromRotation(arc.pitch,arc.yaw).distanceToSqr(direction)<1e-8,"electrical aim "+i);
                    }
                } else {
                    Projectile shot=switch(kind) {
                        case RUBY->game.items().ruby().snapshot().getFirst();case TERRA->terra().getFirst();
                        case AIR->game.items().air().snapshot().getFirst();case ACID->game.items().clouds().acidSnapshot().getFirst();
                        case TELEPORT->game.items().teleport().snapshot().getFirst();case STONE->game.items().stone().snapshot().getFirst();
                        case FIRE->base.fire().getFirst();case BLIND->game.items().blind().snapshot().getFirst();
                        default->throw new IllegalStateException();
                    };
                    double speed=switch(kind){case AIR->.8;case TELEPORT->1.2;case ACID,STONE->1.6;case FIRE->2.5;default->1.8;};
                    require(base.count()==1&&shot.getOwner()==owner&&game.items().currentEpoch(shot),"one owned current-epoch payload "+kind);
                    near(shot.position(),origin,"exact producer emission origin "+kind);
                    near(shot.getDeltaMovement(),direction.scale(speed),"source speed/aim "+kind);
                    switch(kind) {
                        case RUBY->require(((LbcRuby.Charge)shot).sourceLifetime==50,"Ruby50");
                        case TERRA->require(((LbcTerra.TerraProjectile)shot).sourceLifetime==200,"Terra200");
                        case AIR->require(((LbcAir.Charge)shot).sourceLifetime==120&&!((LbcAir.Charge)shot).trial,"Air120 without Academy");
                        case ACID->{
                            require(LbcClouds.acidLifetime((LbcClouds.AcidProjectile)shot)==300&&shot.isNoGravity()&&!shot.entityTags().contains("shield_blockable2"),"Acid300 gravity and shield source flags");
                            var flight=shot.getDeltaMovement();
                            for(int tick=1;tick<=2;tick++) {
                                owner.level().tickNonPassenger(shot);
                                require(!shot.isRemoved(),"native flying acid survives free flight");
                                near(shot.position(),origin.add(flight.scale(tick)),"native acid keeps source flight direction tick "+tick);
                                near(shot.getDeltaMovement(),flight,"source nogravity acid has no native drag decay tick "+tick);
                            }
                        }
                        case TELEPORT->require(((LbcTeleport.Charge)shot).sourceLifetime==60,"Teleport60");
                        case STONE->require(((LbcStone.Charge)shot).lifetime==80,"Stone80");
                        case BLIND->require(((LbcBlind.Charge)shot).lifetime==100&&!((LbcBlind.Charge)shot).trial,"Blind100 without Academy");
                        default->{ }
                    }
                }
                clear();
            }
            // Real native transport tick must apply the source trail, not teleport the caster.
            var victim=cow(origin.add(.4,-.2,.4));
            service.emit(owner,InteractionHand.OFF_HAND,stack,LbcSauvojen.Kind.TELEPORT,origin,new Vec3(0,0,1));
            game.items().teleport().tick();
            var effect=victim.getEffect(MobEffects.LEVITATION);
            require(effect!=null&&effect.getAmplifier()==40&&effect.getDuration()==20,"native flying Teleport trail Levitation41 for20 ticks");
            near(owner.position(),position,"native flying Teleport trail preserves caster position");clear();removeCows();
        }
        void schedulingAndTracking() {
            var advancement=game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/staffs/sauvojen_tuntija_wand"));
            require(advancement!=null&&advancement.value().criteria().size()==9,"existing nine-criterion advancement");
            for(String key:advancement.value().criteria().keySet())base.owner.getAdvancements().revoke(advancement,key);
            for(var kind:LbcSauvojen.Kind.values()) {
                var drone=launch(kind);var origin=base.owner.getEyePosition().add(base.owner.getLookAngle().scale(2));
                near(drone.position,origin,"source eye+look2 launch "+kind);
                require(drone.owner==base.owner&&drone.hand==InteractionHand.MAIN_HAND&&drone.carrier.getType()==EntityTypes.MARKER
                        &&drone.carrier.entityTags().contains(LbcSauvojen.DRONE)&&game.items().currentEpoch(drone.carrier),"native invisible identity/owner/epoch "+kind);
                require(drone.holder.getWatchingPlayers().contains(base.owner.connection),"manual Polymer viewer tracking "+kind);
                require(Identifier.parse("lbc:st/"+kind.name().toLowerCase(Locale.ROOT)).equals(drone.display.getItem().get(DataComponents.ITEM_MODEL)),"source flying staff model "+kind);
                require(base.owner.getAdvancements().getOrStartProgress(advancement).getCriterion("wand"+(kind.ordinal()+1)).isDone(),"exact branch advancement criterion "+kind);
                service.tick();require(drone.lifetime==2&&base.count()==0,"first tick increments directly to2 without firing "+kind);
                // Keep the real position in loaded resident air, without advancing payload engines.
                int first=kind.cadence+1;
                for(int age=3;age<=first;age++){drone.position=origin;drone.carrier.setPos(origin);service.tick();}
                int expected=kind==LbcSauvojen.Kind.ELECTRO?4:1;
                require(drone.lifetime==first&&base.count()==expected,"first source modulo dispatch "+kind+" age="+drone.lifetime+" payloads="+base.count());
                base.clear();
                drone.lifetime=198;service.tick();require(service.count()==1&&drone.lifetime==199,"producer survives age199 "+kind);
                service.tick();require(service.count()==0&&drone.carrier.isRemoved()&&drone.holder.getWatchingPlayers().isEmpty(),"age200 terminal carrier and viewer cleanup "+kind);
                base.clear();
            }
            require(base.owner.getAdvancements().getOrStartProgress(advancement).isDone(),"all nine branches complete existing advancement");
        }
        void targetingAndLifecycle() {
            var drone=launch(LbcSauvojen.Kind.RUBY);var start=drone.position;
            // With no nearby eligible actor, source rotation stays zero even when caster camera differs.
            drone.direction=new Vec3(0,0,1);service.tick();near(drone.direction,new Vec3(0,0,1),"no-target source rotation retained");
            var far=cow(start.add(6,1,0));var near=cow(start.add(-3,0,0));var role=cow(start.add(0,0,1));role.entityTags().add("strict_map_object");
            var greed=cow(start.add(0,0,-1));greed.entityTags().add("sin_greed_active");
            Vec3 from=drone.position;service.tick();near(drone.direction,near.getEyePosition().subtract(from).normalize(),"nearest eligible enemy skips role and active Greed");
            var ally=cow(start.add(0,0,.5));var board=game.server().getScoreboard();var oldTeam=base.owner.getTeam();
            var team=board.addPlayerTeam("sw2_sauvojen_check");
            try {
                board.addPlayerToTeam(base.owner.getScoreboardName(),team);board.addPlayerToTeam(ally.getScoreboardName(),team);
                from=drone.position;service.tick();near(drone.direction,near.getEyePosition().subtract(from).normalize(),"nearest eligible enemy skips allied native actor");
            } finally {
                board.removePlayerTeam(team);if(oldTeam!=null)board.addPlayerToTeam(base.owner.getScoreboardName(),oldTeam);
            }
            Vec3 prior=drone.position;Vec3 away=prior.subtract(base.owner.getEyePosition()).normalize().scale(.1);
            int lifetime=drone.lifetime;var direction=drone.direction;game.items().timeStop().begin(base.owner.level(),40);
            try{service.tick();require(drone.lifetime==lifetime,"Time Stop freezes producer age");near(drone.position,prior,"Time Stop freezes movement");near(drone.direction,direction,"Time Stop freezes aim");}
            finally{game.items().timeStop().end(base.owner.level());}
            service.tick();near(drone.position,prior.add(away),"movement repels from owner eyes by exact0.1 independently of aim");
            prior=drone.position;lifetime=drone.lifetime;base.owner.entityTags().add("time_stopped");
            try{service.tick();require(drone.lifetime==lifetime,"personal Time Stop freezes existing producer");near(drone.position,prior,"personal Time Stop freezes existing movement");}
            finally{base.owner.entityTags().remove("time_stopped");}
            game.items().sauvojen().unloaded(drone.carrier);require(service.count()==0&&drone.carrier.isRemoved(),"carrier unload retires captured producer");
            var orphan=new Marker(EntityTypes.MARKER,base.owner.level());orphan.setPos(start);orphan.entityTags().add(LbcSauvojen.DRONE);
            require(base.owner.level().addFreshEntity(orphan),"native stale carrier fixture");service.loaded(orphan);require(orphan.isRemoved(),"reloaded orphan cannot resume producer");
            removeCows();drone=launch(LbcSauvojen.Kind.RUBY);service.forget(base.owner);
            require(service.count()==0&&drone.carrier.isRemoved(),"disconnect/owner forget retires old identity");
            drone=launch(LbcSauvojen.Kind.RUBY);var cell=BlockPos.containing(drone.position);var block=base.owner.level().getBlockState(cell);
            try{base.owner.level().setBlock(cell,Blocks.STONE.defaultBlockState(),3);service.tick();require(service.count()==0&&drone.carrier.isRemoved(),"ordinary nonair central cell stops producer");}
            finally{base.owner.level().setBlock(cell,block,3);}
            drone=launch(LbcSauvojen.Kind.RUBY);cell=BlockPos.containing(drone.position);block=base.owner.level().getBlockState(cell);
            try {
                base.owner.level().setBlock(cell,Blocks.WATER.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SKIP_ON_PLACE);
                service.tick();require(service.count()==1&&drone.lifetime==2,"source #minecraft:airs water remains passable to flying producer");
            } finally{service.stop();base.owner.level().setBlock(cell,block,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SKIP_ON_PLACE);}
            drone=launch(LbcSauvojen.Kind.RUBY);
            base.field=new Marker(EntityTypes.MARKER,base.owner.level());base.field.setPos(drone.position);base.field.entityTags().add("stopper_magic");
            require(base.owner.level().addFreshEntity(base.field),"native producer antimagic field");game.items().magic().loaded(base.field);
            try{service.tick();require(service.count()==0&&drone.carrier.isRemoved(),"antimagic at producer position retires it");}finally{base.removeField();}
            clear();
        }
        void inputAndPayment() {
            var owner=base.owner;
            var rolled=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/sauvojen_wand");
            require(rolled.size()==1&&rolled.getFirst().is(LbcWands.SAUVOJEN),"exact native ordinary singleton leaf");
            var source=rolled.getFirst();var data=LbcFartus.data(source);var name=source.get(DataComponents.CUSTOM_NAME);var lore=source.get(DataComponents.LORE);
            require(Identifier.parse("lbc:sauvojen_wand").equals(source.get(DataComponents.ITEM_MODEL))&&Identifier.parse("lbc:tier_a").equals(source.get(DataComponents.TOOLTIP_STYLE)),"exact source model and tier tooltip");
            require(name!=null&&name.getString().contains("sauvojen_wand")&&lore!=null&&lore.lines().size()==4
                    &&lore.lines().get(0).getString().contains("type")&&lore.lines().get(0).getString().contains("staff")
                    &&lore.lines().get(1).getString().contains("mana_on_activate")&&lore.lines().get(2).getString().contains("sauvojen_wand.lore.1")&&lore.lines().get(3).getString().contains("sauvojen_wand.lore.2"),"exact ordinary source name and ordered four lore keys");
            require(data.size()==5&&data.getIntOr("craftusable",0)==1&&data.getIntOr("transformable",0)==1&&data.getIntOr("staff",0)==1&&data.getIntOr("magicwand",0)==1&&data.getIntOr("sauvojen_wand",0)==1,"five exact ordinary source custom-data flags");
            var advancement=game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/staffs/sauvojen_tuntija_wand"));
            require(advancement!=null,"native give advancement exists");for(var criterion:advancement.value().criteria().keySet())owner.getAdvancements().revoke(advancement,criterion);
            owner.setItemInHand(InteractionHand.MAIN_HAND,LbcWands.SAUVOJEN.getDefaultInstance());base.mana.set(owner,5000);
            require(base.input(InteractionHand.MAIN_HAND)&&service.count()==1&&base.mana.get(owner)==2500,"native give identity also uses shared paid dispatcher");int lastGiveBranch=service.snapshot().getFirst().kind.ordinal();clear();
            require(owner.getAdvancements().getOrStartProgress(advancement).getCriterion("wand"+(lastGiveBranch+1)).isDone(),"native give identity awards actual selected branch");
            for(var hand:InteractionHand.values()) {
                var stack=held(hand);base.mana.set(owner,2499);
                var usedData=LbcFartus.data(stack);usedData.putInt("sauvojen_fixture_hand",hand.ordinal()+1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(usedData));
                require(!base.input(hand)&&base.mana.get(owner)==2499&&service.count()==0,"insufficient mana preserves input "+hand);
                base.mana.set(owner,2500);require(base.input(hand)&&base.mana.get(owner)==0&&stack.getCount()==1&&service.count()==1,"real paid input "+hand);
                var drone=service.snapshot().getFirst();require(drone.hand==hand&&ItemStack.isSameItemSameComponents(drone.usedStack,stack),"captured actual used hand/stack "+hand);
                stack.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("changed after launch"));owner.setItemInHand(hand,ItemStack.EMPTY);
                require(!ItemStack.isSameItemSameComponents(drone.usedStack,stack),"producer snapshots components "+hand);
                drone.lifetime=drone.kind.cadence;service.tick();require(base.count()==(drone.kind==LbcSauvojen.Kind.ELECTRO?4:1)&&base.mana.get(owner)==0,"paid producer continues after original stack changes "+hand);clear();
            }
            held(InteractionHand.MAIN_HAND);owner.setItemInHand(InteractionHand.OFF_HAND,LbcWands.SAUVOJEN.getDefaultInstance());base.mana.set(owner,5000);
            require(base.input(InteractionHand.MAIN_HAND)&&service.count()==2&&base.mana.get(owner)==0,"both hands pay independently for two producers");
            require(service.snapshot().stream().map(d->d.hand).distinct().count()==2,"both actual hands captured");clear();
            for(var hand:InteractionHand.values()) {
                held(hand);base.mana.set(owner,5000);owner.entityTags().add("time_stopped");
                try{require(!base.input(hand)&&service.count()==0&&base.mana.get(owner)==5000,"personal Time Stop rejects before payment "+hand);}
                finally{owner.entityTags().remove("time_stopped");}
                game.items().timeStop().begin(owner.level(),40);
                try{require(!base.input(hand)&&service.count()==0&&base.mana.get(owner)==5000,"dimension Time Stop rejects before payment "+hand);}
                finally{game.items().timeStop().end(owner.level());}
            }
            held(InteractionHand.OFF_HAND);game.config().offhandWands=false;
            try{require(!base.input(InteractionHand.OFF_HAND)&&service.count()==0,"offhand setting gate");}
            finally{game.config().offhandWands=true;}
            for(String flag:List.of("upgraded","upgradable")) {
                var unsupported=held(InteractionHand.MAIN_HAND);var legacy=LbcFartus.data(unsupported);legacy.putInt(flag,1);unsupported.set(DataComponents.CUSTOM_DATA,CustomData.of(legacy));base.mana.set(owner,5000);
                require(!base.input(InteractionHand.MAIN_HAND)&&service.count()==0&&base.mana.get(owner)==5000,"unported legacy "+flag+" input remains explicitly gated");
            }
            held(InteractionHand.MAIN_HAND);
            base.field=new Marker(EntityTypes.MARKER,owner.level());base.field.setPos(owner.getEyePosition());base.field.entityTags().add("stopper_magic");
            require(owner.level().addFreshEntity(base.field),"native input antimagic field");game.items().magic().loaded(base.field);
            try{require(!base.input(InteractionHand.MAIN_HAND)&&base.mana.get(owner)==5000&&service.count()==0,"antimagic admission rejects before payment");}
            finally{base.removeField();}
            // Actual shared Mage callback must run once for a successful combined cast.
            for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)) {
                var armor=new ItemStack(Items.LEATHER_HELMET);var tag=new net.minecraft.nbt.CompoundTag();tag.putInt("mage",1);armor.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));owner.setItemSlot(slot,armor);
            }
            require(base.mana.mageFullSet(owner),"real Mage full-set fixture");int maximum=base.mana.maximum(owner);var bonus=base.mana.score(ScoreHolder.forNameOnly("mage_set_bonus"),"lbc.math");int previousBonus=bonus.get();
            try{bonus.set(137);held(InteractionHand.MAIN_HAND);base.mana.set(owner,5000);require(base.input(InteractionHand.MAIN_HAND)&&base.mana.maximum(owner)==maximum+137,"single shared Mage cast bonus");}
            finally{bonus.set(previousBonus);clear();}
            game.spectators().enter(owner);held(InteractionHand.MAIN_HAND);int balance=base.mana.get(owner);
            require(!base.input(InteractionHand.MAIN_HAND)&&service.count()==0&&base.mana.get(owner)==balance,"observer input gate preserves mana");
        }
        @Override public void close() {
            removeCows();if(!retain)clear();base.retain=retain;
            base.mana.score(ScoreHolder.forNameOnly("sauvojen_wand_mana_use"),"lbc.math").set(cost);
            advancements.forEach((a,done)->{for(String key:a.value().criteria().keySet())if(done.contains(key))base.owner.getAdvancements().award(a,key);else base.owner.getAdvancements().revoke(a,key);});
            base.owner.getStats().setValue(base.owner,Stats.ITEM_USED.get(LbcWands.SAUVOJEN),stat);base.close();
        }
    }
    private static List<LbcSauvojen.Drone> retained;
    private static String retainedEpoch;
    public static void run(GameController game) {
        try(var fixture=new Fixture(game)){fixture.payloads();fixture.impacts();fixture.schedulingAndTracking();fixture.targetingAndLifecycle();fixture.inputAndPayment();}
        SkyWars2.LOGGER.info("[check] PASS sauvojen (nine source payloads/origins/speeds/lifetimes, real Teleport trail, modulo producer age/tracking/targets, native both-hand mana and Mage, guards and unload)");
    }
    public static void stopProbe(GameController game) {
        require(retained==null,"exclusive retained producer fixture");
        try(var fixture=new Fixture(game,true)) {
            for(var kind:LbcSauvojen.Kind.values()) {var stack=fixture.held(InteractionHand.MAIN_HAND);fixture.service.cast(fixture.base.owner,InteractionHand.MAIN_HAND,stack,kind.ordinal());}
            retained=fixture.service.snapshot();require(retained.size()==9,"nine distinct producers retained for actual stop");
            retainedEpoch=game.items().epoch();fixture.retain=true;
        }
        SkyWars2.LOGGER.info("[check] PASS sauvojen-stop-probe");
    }
    public static void cleanup(GameController game) {
        require(retained!=null,"actual stop probe exists");
        try{require(!game.items().currentEpoch(retainedEpoch)&&game.items().sauvojen().count()==0&&retained.stream().allMatch(d->d.carrier.isRemoved()&&d.holder.getWatchingPlayers().isEmpty()),"actual match stop clears nine carriers, viewers and epoch");}
        finally{retained.forEach(d->d.carrier.discard());retained=null;retainedEpoch=null;}
        SkyWars2.LOGGER.info("[check] PASS sauvojen-cleanup");
    }
}
