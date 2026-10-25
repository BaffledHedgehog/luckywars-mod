package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/** Bounded native fixture; explicit service calls below test boundaries without elapsed-time claims. */
public final class VoidArcanumChecks {
    private VoidArcanumChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Void Arcanum: "+why);}
    private static Object copy(Object value){if(value instanceof int[] array)return array.clone();if(value instanceof Set<?> set)return new HashSet<>(set);return value;}
    private static Map<Field,Object> fields(Object target){
        var values=new LinkedHashMap<Field,Object>();try{for(var f:target.getClass().getFields())if(!Modifier.isStatic(f.getModifiers()))values.put(f,copy(f.get(target)));}
        catch(IllegalAccessException error){throw new IllegalStateException(error);}return values;
    }
    private static void fields(Object target,Map<Field,Object> values){try{for(var entry:values.entrySet())entry.getKey().set(target,copy(entry.getValue()));}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
    private static final class Saved {
        final ServerPlayer player;final GameChecks.WandThirdPlayerState state;final PlayerStore.Profile profile;final Map<Field,Object> profileFields;
        final net.minecraft.world.scores.PlayerTeam team;final boolean shift;final int cowKills;
        final Map<AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();
        final Map<Stat<Identifier>,Integer> custom=new LinkedHashMap<>();final Map<Stat<Item>,Integer> crafted=new LinkedHashMap<>();
        Saved(GameController game,ServerPlayer player){
            this.player=player;state=new GameChecks.WandThirdPlayerState(player,game.items().mana());profile=game.players().get(player.getUUID());profileFields=fields(profile);team=player.getTeam();shift=player.isShiftKeyDown();cowKills=player.getStats().getValue(Stats.ENTITY_KILLED.get(EntityTypes.COW));
            for(var adv:game.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();player.getAdvancements().getOrStartProgress(adv).getCompletedCriteria().forEach(done::add);advancements.put(adv,done);}
            for(var id:BuiltInRegistries.CUSTOM_STAT){var stat=Stats.CUSTOM.get(id);custom.put(stat,player.getStats().getValue(stat));}
            for(var item:BuiltInRegistries.ITEM){var stat=Stats.ITEM_CRAFTED.get(item);crafted.put(stat,player.getStats().getValue(stat));}
        }
        void restore(GameController game){
            advancements.forEach((adv,done)->{for(var key:adv.value().criteria().keySet())if(done.contains(key))player.getAdvancements().award(adv,key);else player.getAdvancements().revoke(adv,key);});
            fields(profile,profileFields);state.restore(game);player.setShiftKeyDown(shift);
            var board=game.server().getScoreboard();if(player.getTeam()!=null)board.removePlayerFromTeam(player.getScoreboardName(),player.getTeam());if(team!=null)board.addPlayerToTeam(player.getScoreboardName(),team);
            custom.forEach((stat,value)->player.getStats().setValue(player,stat,value));crafted.forEach((stat,value)->player.getStats().setValue(player,stat,value));player.getStats().setValue(player,Stats.ENTITY_KILLED.get(EntityTypes.COW),cowKills);game.items().magic().inventoryChanged(player);
        }
    }
    private static final class ProbeCow extends Cow {
        Entity author;int accepted;
        ProbeCow(net.minecraft.server.level.ServerLevel level){super(EntityTypes.COW,level);}
        @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel world,DamageSource source,float amount){boolean hit=super.hurtServer(world,source,amount);if(hit){accepted++;author=source.getEntity();}return hit;}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final LbcVoidArcanum service;final ServerPlayer owner,other;final Saved saved,otherSaved;
        final Map<Field,Object> config;final Map<String,Integer> costs=new LinkedHashMap<>();
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();final List<ProbeCow> mobs=new ArrayList<>();
        final List<ScheduledTick<Block>> blockTicks=new ArrayList<>();final List<ScheduledTick<Fluid>> fluidTicks=new ArrayList<>();final BoundingBox bounds;
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());final Vec3 at;final net.minecraft.world.scores.PlayerTeam team;
        Fixture(GameController game){
            this.game=game;service=game.items().voidArcanum();owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(owner!=null&&other!=null&&game.playing()&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other),"two active participants");
            require(service.count()==0&&game.items().blackHoleVisuals().count()==0&&!game.items().timeStop().stopped(owner.level())&&!game.items().frost().isFrozen(owner)&&!game.items().frost().isFrozen(other)&&game.items().frost().cold(owner)==0&&game.items().frost().cold(other)==0,"clean body/renderer/frost/Time Stop");
            require(owner.containerMenu==owner.inventoryMenu&&other.containerMenu==other.inventoryMenu,"initial inventory menus");
            saved=new Saved(game,owner);otherSaved=new Saved(game,other);config=fields(game.config());owner.level().getAllEntities().forEach(before::add);
            for(String name:List.of("void_arcanum_mana_activate","void_arcanum_mana_tick","void_arcanum_mana_tick_sec"))costs.put(name,game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly(name),"lbc.math").get());
            at=Vec3.atCenterOf(owner.blockPosition().offset(0,48,0));require(at.y+25<owner.level().getMaxY(),"full-radius fixture below native build ceiling");
            var origin=BlockPos.containing(at);bounds=new BoundingBox(origin.getX()-32,origin.getY()-25,origin.getZ()-32,origin.getX()+32,origin.getY()+25,origin.getZ()+32);
            require(game.server().getScoreboard().getPlayerTeam("void_fixture")==null,"exclusive fixture team");team=game.server().getScoreboard().addPlayerTeam("void_fixture");team.setAllowFriendlyFire(false);
        }
        ItemStack loot(){var rolls=game.lbc().rollTable(owner.level(),at,owner,"lbc:ported/void_arcanum");require(rolls.size()==1&&rolls.getFirst().getCount()==1,"live source single loot");return rolls.getFirst();}
        void move(ServerPlayer player,Vec3 p){player.teleportTo(player.level(),p.x,p.y,p.z,Set.of(),0,0,true);player.setDeltaMovement(Vec3.ZERO);}
        void block(BlockPos pos,Block type){require(owner.level().hasChunkAt(pos),"resident fixture cell");blocks.putIfAbsent(pos.immutable(),game.maps().snapshot(owner.level(),pos));owner.level().setBlock(pos,type.defaultBlockState(),2);}
        void init(){
            // Snapshot every resident cell and scheduler before clearing; neighbor updates must not
            // leak from a fixture into restored terrain. Never request an absent column.
            for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++)for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++){
                var chunk=owner.level().getChunkSource().getChunkNow(x,z);if(chunk==null)continue;
                ((LevelChunkTicks<Block>)chunk.getBlockTicks()).getAll().filter(t->bounds.isInside(t.pos())).forEach(blockTicks::add);
                ((LevelChunkTicks<Fluid>)chunk.getFluidTicks()).getAll().filter(t->bounds.isInside(t.pos())).forEach(fluidTicks::add);
            }
            for(var p:BlockPos.betweenClosed(bounds.minX(),bounds.minY(),bounds.minZ(),bounds.maxX(),bounds.maxY(),bounds.maxZ()))
                if(owner.level().getChunkSource().getChunkNow(p.getX()>>4,p.getZ()>>4)!=null){
                    blocks.put(p.immutable(),game.maps().snapshot(owner.level(),p));
                    if(!owner.level().getBlockState(p).isAir())block(p.immutable(),Blocks.AIR);
                }
            game.config().lbcSkills=false;game.config().ranked=false;
            for(var p:List.of(owner,other)){
                p.closeContainer();p.getInventory().clearContent();p.inventoryMenu.getCraftSlots().clearContent();p.containerMenu.setCarried(ItemStack.EMPTY);p.getInventory().setSelectedSlot(0);p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.setShiftKeyDown(false);p.removeAllEffects();
                p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(1000);p.setAbsorptionAmount(0);p.entityTags().remove("time_stopped");game.items().mana().noMagic(p,0);game.items().mana().set(p,100000);game.players().get(p.getUUID()).lbcSkill=0;
                game.server().getScoreboard().addPlayerToTeam(p.getScoreboardName(),team);game.items().magic().inventoryChanged(p);
            }
            move(owner,at.add(-5,0,0));move(other,at.add(5,0,0));game.items().magic().flushCleansing();
            for(var entry:Map.of("void_arcanum_mana_activate",4000,"void_arcanum_mana_tick",11,"void_arcanum_mana_tick_sec",220).entrySet())game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly(entry.getKey()),"lbc.math").set(entry.getValue());
        }
        void bodies(){for(var hole:service.snapshot())service.unloaded(hole.marker);}
        void clear(){service.forget(owner);service.forget(other);owner.getInventory().clearContent();other.getInventory().clearContent();}
        LbcVoidArcanum.Hole spawn(ServerPlayer p,Vec3 point){var hole=service.spawn(p,point);require(hole!=null,"native body spawn");return hole;}
        void prime(){clear();for(var p:List.of(owner,other)){p.setItemInHand(InteractionHand.MAIN_HAND,loot());game.items().mana().set(p,100000);}service.tick();bodies();}
        ProbeCow cow(Vec3 pos){var cow=new ProbeCow(owner.level());cow.setPos(pos);cow.setNoGravity(true);cow.setNoAi(true);owner.level().addFreshEntity(cow);mobs.add(cow);return cow;}
        void presentation(){
            var stack=loot();var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            require(stack.is(LbcVoidArcanum.ITEM)&&Identifier.parse("lbc:arcanumblackhole").equals(stack.get(DataComponents.ITEM_MODEL))&&Identifier.parse("lbc:tier_a").equals(stack.get(DataComponents.TOOLTIP_STYLE)),"native item/source A model and tier");
            require(stack.get(DataComponents.LORE)!=null&&stack.get(DataComponents.LORE).lines().size()==8&&stack.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents name&&name.getKey().equals("void_arcanum"),"source translatable name and authored lore");
            for(String flag:List.of("void_curse","craftusable","transformable","arcanum","arcanumt"))require(data.getIntOr(flag,0)==1,"source flag "+flag);
        }
        void holding(){
            clear();owner.setItemInHand(InteractionHand.MAIN_HAND,loot());game.items().mana().set(owner,3999);service.tick();require(service.count()==0&&game.items().mana().get(owner)==3999,"hold activation below4000 rejected");
            game.items().mana().set(owner,100000);service.tick();require(service.count()==1&&game.items().mana().get(owner)==95989,"one mainhand hold activation4000 plus11 upkeep");var first=service.snapshot().getFirst();
            require(first.marker.position().distanceTo(at.add(-5,0,4))<.2&&!first.marker.shouldBeSaved(),"look-directed four-block native unsaved body");
            service.tick();require(service.count()==1&&game.items().mana().get(owner)==95978,"continuous hold only upkeep");
            owner.setItemInHand(InteractionHand.OFF_HAND,loot());service.tick();require(service.count()==1&&service.snapshot().getFirst().members.size()==2&&game.items().mana().get(owner)==91956,"independent offhand activation merges and charges22 total");
            owner.connection.handlePlayerAction(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,BlockPos.ZERO,Direction.DOWN,0));
            service.tick();require(service.count()==1&&service.snapshot().getFirst().members.size()==4&&game.items().mana().get(owner)==83868,"real F packet rearms both unchanged Arcanum hands and charges88 total");
            clear();service.tick();require(service.count()==0&&first.marker.isRemoved()&&game.items().blackHoleVisuals().count()==0,"unequip removes owned bodies and renderer");
        }
        void levels(){
            prime();
            for(int level=1;level<=5;level++){
                bodies();for(int i=0;i<level;i++)spawn(i==level-1&&level>1?other:owner,at);
                game.items().mana().set(owner,100000);game.items().mana().set(other,100000);service.tick();
                var hole=service.snapshot().getFirst();int total=11<<(level-1);
                require(service.count()==1&&hole.level==level&&hole.members.size()==level,"native merge level "+level);
                int paidOwner=100000-game.items().mana().get(owner),paidOther=100000-game.items().mana().get(other);
                require(paidOwner+paidOther==total,"escalating whole-body upkeep "+total);
                double expectedOwner=(double)total*(level==1?1:level-1)/level;
                require(Math.abs(paidOwner-expectedOwner)<1.001&&Math.abs(paidOther-(total-expectedOwner))<1.001,"contributions split exact whole-body price");
                require(!hole.wild,"allied contributions remain controlled");
                for(int i=0;i<32;i++)if(level==1||level>=3)require(service.source(hole).owner().equals(owner.getUUID()),"majority contributions own damage/terrain independently of order");
                service.updateVisual(hole,owner);float radius=(float)(new double[]{0,2,3,5,9,24}[level]/14);
                require(hole.visual!=null&&Math.abs(hole.visual.radius-radius)<.00001&&hole.visual.disc==(level==5),"renderer source radius/14 and max-level large disc");
                for(int i=0;i<5;i++)game.items().blackHoleVisuals().tick();
                var index=game.items().blackHoleVisuals().origins(owner).indexOf(hole.visual.point);require(index>=0,"visible native shared-renderer handle");
                var display=game.items().blackHoleVisuals().displays(owner).get(index);
                require(Identifier.parse(level==5?"lbc:black_hole_large":"lbc:black_hole_small").equals(display.getItem().get(DataComponents.ITEM_MODEL))&&Math.abs(display.getScale().x()-radius)<.00001,"actual virtual display model and scale");
                if(level==2){var authors=new HashSet<UUID>();for(int i=0;i<128;i++)authors.add(service.source(hole).owner());require(authors.equals(Set.of(owner.getUUID(),other.getUUID())),"equal contribution owners both selected randomly");}
            }
            var hole=service.snapshot().getFirst();game.server().getScoreboard().removePlayerFromTeam(other.getScoreboardName(),team);spawn(other,at);service.tick();
            require(service.count()==1&&hole.level==5&&hole.members.size()==6&&hole.wild,"late hostile sixth contribution keeps one capped5 WILD body");
            service.forget(other);require(service.count()==1&&hole.members.size()==4&&hole.level==4&&hole.wild,"WILD remains after hostile owner leaves");
            service.forget(owner);require(service.count()==0&&hole.marker.isRemoved(),"last owner forget removes merged body");game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),team);
        }
        void physical(){
            prime();var hole=spawn(owner,at);var mob=cow(at.add(.5,0,0));var initial=mob.position();var member=service.source(hole);
            service.physics(hole,owner,member.cause());require(mob.accepted==1&&mob.author==owner&&mob.getHealth()<mob.getMaxHealth(),"actual accepted generic damage carries selected author");
            require(mob.position().equals(initial)&&mob.getDeltaMovement().x<0&&game.items().indirectKills().altitudeAuthor(mob)==owner,"native velocity and owned displacement without teleport");
            var victim=cow(at.add(.4,0,0));victim.setHealth(.1f);service.physics(hole,owner,member.cause());require(victim.isDeadOrDying()&&victim.getKillCredit()==owner,"actual native lethal hole hit credits author");
            var late=cow(at.add(.5,0,0));service.physics(hole,owner,member.cause());late.setInvulnerableTime(0);late.damageCooldownTime=0;
            require(late.hurtServer(owner.level(),late.damageSources().playerAttack(other),1),"later real player hit accepted");late.setInvulnerableTime(0);late.damageCooldownTime=0;
            require(late.hurtServer(owner.level(),late.damageSources().fellOutOfWorld(),1000)&&late.getKillCredit()==other,"later accepted player hit wins over prior pull cause");
            hole.wild=true;move(owner,hole.point.add(.2,0,0));move(other,hole.point.add(-.2,0,0));for(var p:List.of(owner,other)){p.setInvulnerableTime(0);p.damageCooldownTime=0;}float a=owner.getHealth(),b=other.getHealth();
            service.physics(hole,owner,new LbcIndirectKills.Owner(member.owner(),member.cause().order(),true,false));
            require(owner.getHealth()<a&&other.getHealth()<b,"WILD native damage includes caster and allied player");
            move(owner,hole.point.add(2.4,0,0));move(other,hole.point.add(-2.4,0,0));service.physics(hole,owner,member.cause());
            require(owner.getDeltaMovement().equals(Vec3.ZERO)&&other.getDeltaMovement().equals(Vec3.ZERO),"outer influence does not pull players");
            for(var cow:mobs){game.items().indirectKills().clear(cow);cow.discard();}mobs.clear();move(owner,at.add(-5,0,0));move(other,at.add(5,0,0));bodies();
        }
        void terrain(){
            prime();var center=BlockPos.containing(at);
            for(int level=1;level<=5;level++){
                int radius=new int[]{0,2,3,5,9,24}[level];var inside=center.offset(radius,0,0);var outside=center.offset(radius,1,0);var corner=center.offset(radius,radius,0);
                bodies();var hole=spawn(owner,at);hole.level=level;hole.terrainCause=service.source(hole).cause();
                var samples=new ArrayList<BlockPos>();
                if(level<=2)samples.add(inside);
                else for(var pos:BlockPos.betweenClosed(center.offset(-radius,-2,-radius),center.offset(radius,2,radius)))
                    if(Vec3.atCenterOf(pos).distanceToSqr(at)<=radius*radius){require(owner.level().getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null,"resident dither samples");samples.add(pos.immutable());}
                for(var pos:samples)block(pos,Blocks.DIRT);block(outside,Blocks.DIRT);block(corner,Blocks.DIRT);
                var drops=new HashSet<Entity>();owner.level().getAllEntities().forEach(drops::add);
                for(int i=0;i<80;i++){service.terrain(hole,owner,8192,System.nanoTime()+100_000_000);if(!hole.terrain.hasWork())break;}
                require(hole.frontier==radius&&!hole.terrain.hasWork()&&owner.level().getBlockState(outside).is(Blocks.DIRT)&&owner.level().getBlockState(corner).is(Blocks.DIRT),"exact radial mask excludes next off-axis cell and corners tier "+level);
                long erased=samples.stream().filter(pos->owner.level().getBlockState(pos).isAir()).count();
                if(level<=2)require(erased==samples.size(),"small tiers retain complete native destruction "+level);
                else{
                    require(erased>0&&erased<samples.size(),"one high-tier pass erodes and retains intermixed cells "+level);
                    var outer=samples.stream().filter(pos->Vec3.atCenterOf(pos).distanceToSqr(at)>=radius*radius*.64).toList();
                    long outerErased=outer.stream().filter(pos->owner.level().getBlockState(pos).isAir()).count();
                    require(outerErased>0&&outerErased<outer.size(),"outer annulus contains erosion and noisy remnants tier "+level);
                }
                service.updateVisual(hole,owner);require(Math.abs(hole.visual.radius*14-radius)<.00001,"shader lens boundary equals actual destruction radius tier "+level);
                require(hole.terrain.queuedBlockCount()<=49*49*49+7*7*7,"bounded resident moving workset");
                boolean produced=false;for(var e:owner.level().getAllEntities())if(!drops.contains(e)&&e instanceof net.minecraft.world.entity.item.ItemEntity){produced=true;e.discard();}
                require(produced==(level<=2),"small-level native drops and larger-level no-drop erosion");
                for(var pos:samples)block(pos,Blocks.AIR);block(outside,Blocks.AIR);block(corner,Blocks.AIR);
            }bodies();
            BlackHoleErosionChecks.run(game,owner,at,this::block);
        }
        void denseTerrain(){
            prime();var center=BlockPos.containing(at);var dense=new ArrayList<BlockPos>();var core=new ArrayList<BlockPos>();var surface=new ArrayList<BlockPos>();
            for(var pos:BlockPos.betweenClosed(center.offset(-24,-24,-24),center.offset(24,24,24))){
                double distance=Vec3.atCenterOf(pos).distanceToSqr(at);if(distance>24*24)continue;
                require(owner.level().getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null,"resident dense max-tier sphere");
                var cell=pos.immutable();dense.add(cell);if(distance<=2.25)core.add(cell);if(distance>=24*24*.64)surface.add(cell);
            }
            require(dense.size()>57000&&!core.isEmpty()&&surface.size()>10000,"representative filled max-tier sphere");
            for(var pos:dense)block(pos,Blocks.DIRT);
            var hole=spawn(owner,at);hole.level=5;hole.terrainCause=service.source(hole).cause();int clock=game.server().getTickCount();
            long sum=0,max=0,firstWall=0;int calls=0,inspected=0,firstInspected=0;long firstRemoved=0,removed=0,surfaceRemoved=0;
            try{
                do{
                    long started=System.nanoTime();int spent=service.terrain(hole,owner,12288,started+6_000_000);long elapsed=System.nanoTime()-started;
                    require(spent>=0&&spent<=12288,"dense actual callback slice respects inspection allowance");
                    sum+=elapsed;max=Math.max(max,elapsed);calls++;inspected+=spent;
                    require(hole.terrain.queuedBlockCount()<=49*49*49+7*7*7,"dense actual callback queue remains bounded");
                    if(calls==1){
                        firstWall=elapsed;firstInspected=spent;
                        firstRemoved=dense.stream().filter(pos->owner.level().getBlockState(pos).isAir()).count();
                        require(core.stream().allMatch(pos->owner.level().getBlockState(pos).isAir()),"first dense native terrain slice clears actual tiny core immediately");
                        require(firstRemoved>0&&firstRemoved<dense.size()&&surface.stream().anyMatch(pos->owner.level().getBlockState(pos).is(Blocks.DIRT)),"first dense slice changes terrain while retaining outer remains");
                    }
                    require(calls<4096,"dense max-tier pass terminates under bounded slices");
                }while(hole.terrain.hasWork());
                require(game.server().getTickCount()==clock,"dense probe drains one pass without advancing active clock");
                removed=dense.stream().filter(pos->owner.level().getBlockState(pos).isAir()).count();surfaceRemoved=surface.stream().filter(pos->owner.level().getBlockState(pos).isAir()).count();
                require(removed>=firstRemoved&&removed<dense.size()&&surfaceRemoved>0&&surfaceRemoved<surface.size(),"complete dense native pass leaves dithered outer terrain instead of clearing the sphere");
                SkyWars2.LOGGER.info("[check] METRIC void-dense-erosion filled={} removed={} surfaceRemoved={}/{} calls={} inspected={} firstInspected={} firstRemoved={} firstMs={} sumMs={} maxMs={} (synchronous native terrain slices; no natural TPS claim)",
                        dense.size(),removed,surfaceRemoved,surface.size(),calls,inspected,firstInspected,firstRemoved,firstWall/1_000_000.0,sum/1_000_000.0,max/1_000_000.0);
            }finally{bodies();for(var pos:dense)block(pos,Blocks.AIR);}
        }
        void motion(){
            prime();LbcVoidArcanum.Hole hole=null;ProbeCow target=null;
            double[] acceleration={0,.025,.035,.018,.022,.025},caps={0,.13,.13,.13,.13,.5};
            for(int level=1;level<=5;level++){
                bodies();hole=spawn(owner,at);hole.level=level;target=cow(at.add(14,0,0));var cause=service.source(hole).cause();double last=0;
                for(int i=0;i<5;i++){
                    var start=hole.point;var velocity=hole.velocity;service.physics(hole,owner,cause);double speed=hole.point.distanceTo(start);
                    require(speed>=last-.000001&&speed<=caps[level]+.000001&&hole.velocity.x>0,"native level "+level+" accelerates toward nearest suitable entity up to legacy speed");
                    require(Math.abs(speed-Math.min(caps[level],last+acceleration[level]))<.000001&&hole.velocity.subtract(velocity).length()<=acceleration[level]+.000001,"preserved acceleration with legacy tier ceiling "+level);last=speed;
                }
                // Exercise the cap boundary without claiming elapsed native travel time.
                hole.velocity=new Vec3(caps[level]-acceleration[level]/2,0,0);var start=hole.point;service.physics(hole,owner,cause);
                require(Math.abs(hole.point.distanceTo(start)-caps[level])<.000001,"actual native displacement obeys tier speed cap "+level);
                var cappedPoint=hole.point;var cappedVelocity=hole.velocity;var cappedTilt=new org.joml.Quaternionf(hole.tilt);
                var originalTarget=target.position();
                for(Vec3 momentum:List.of(new Vec3(3,0,0),new Vec3(1,2,3),new Vec3(-2,-1,-3))){
                    target.setPos(hole.point.add(8,6,8));hole.velocity=momentum;start=hole.point;
                    service.physics(hole,owner,cause);
                    require(hole.point.distanceTo(start)<=caps[level]+.000001&&hole.velocity.length()<=caps[level]+.000001,
                            "over-cap inherited/downgraded momentum and diagonal steering obey legacy 3D limit "+level);
                }
                for(int tick=0;tick<48;tick++){
                    target.setPos(hole.point.add(tick%2==0?8:-8,4,8));start=hole.point;
                    service.physics(hole,owner,cause);
                    require(hole.point.distanceTo(start)<=caps[level]+.000001,"sustained turning chase never exceeds legacy speed "+level);
                }
                // Keep the pre-existing positive-X visual/steering fixture
                // independent of the deliberately alternating diagonal chase.
                hole.point=cappedPoint;hole.velocity=cappedVelocity;hole.tilt.set(cappedTilt);
                hole.marker.snapTo(cappedPoint.x,cappedPoint.y,cappedPoint.z,0,0);target.setPos(originalTarget);
                if(level<5){game.items().indirectKills().clear(target);target.discard();mobs.remove(target);}
            }
            var member=service.source(hole);
            service.updateVisual(hole,owner);for(int i=0;i<5;i++)game.items().blackHoleVisuals().tick();
            var normal=hole.tilt.transform(new org.joml.Vector3f(0,1,0));
            require(normal.x>0&&normal.y>Math.cos(Math.toRadians(18.01))&&normal.y<1,"smoothed modest disc lean toward target");
            var index=game.items().blackHoleVisuals().origins(owner).indexOf(hole.point);require(index>=0,"tilted live display present");
            var display=game.items().blackHoleVisuals().displays(owner).get(index);
            require(display.getLeftRotation().equals(hole.tilt)&&display.getRightRotation().equals(new org.joml.Quaternionf()),"actual native left-rotation metadata and identity right-rotation");
            var near=cow(hole.point.add(0,0,4));var beforeVelocity=hole.velocity;var beforeTilt=new org.joml.Quaternionf(hole.tilt);
            service.physics(hole,owner,member.cause());
            require(hole.velocity.z>beforeVelocity.z&&hole.velocity.x<beforeVelocity.x&&hole.velocity.subtract(beforeVelocity).length()<=.025001,"nearest replacement steers with reduced bounded acceleration");
            require(!hole.tilt.equals(beforeTilt)&&hole.tilt.transform(new org.joml.Vector3f(0,1,0)).z>0,"tilt smoothly follows replacement target");
            service.updateVisual(hole,owner);require(display.getLeftRotation().equals(hole.tilt),"existing native display receives changed tilt metadata");
            for(var cow:List.of(target,near)){game.items().indirectKills().clear(cow);cow.discard();mobs.remove(cow);}
            var temporarilyStopped=new ArrayList<Entity>();
            try{
                for(var entity:hole.world.getAllEntities())if(entity instanceof net.minecraft.world.entity.LivingEntity&&!entity.entityTags().contains("time_stopped")){entity.entityTags().add("time_stopped");temporarilyStopped.add(entity);}
                service.physics(hole,owner,member.cause());require(hole.velocity.equals(Vec3.ZERO),"no suitable target clears motion");
            }finally{for(var entity:temporarilyStopped)entity.entityTags().remove("time_stopped");}
            bodies();require(hole.velocity.equals(Vec3.ZERO)&&hole.tilt.equals(new org.joml.Quaternionf())&&game.items().blackHoleVisuals().count()==0,"body cleanup releases motion tilt and renderer");
        }
        void boundaries(){
            prime();var hole=spawn(owner,at);service.updateVisual(hole,owner);var point=hole.point;int mana=game.items().mana().get(owner);
            game.items().timeStop().begin(owner.level(),40);try{service.tick();require(service.count()==1&&hole.point.equals(point)&&game.items().mana().get(owner)==mana,"actual dimension Time Stop pauses body and upkeep");}finally{game.items().timeStop().end(owner.level());}
            var stopper=new Marker(EntityTypes.MARKER,owner.level());stopper.setPos(at);stopper.entityTags().add("stopper_magic");owner.level().addFreshEntity(stopper);game.items().magic().loaded(stopper);
            try{service.tick();require(hole.marker.isRemoved()&&service.count()==0&&game.items().blackHoleVisuals().count()==0,"actual antimagic removes native body/renderer");}
            finally{game.items().magic().unloaded(stopper);stopper.discard();}
            var stale=new Marker(EntityTypes.MARKER,owner.level());stale.entityTags().add("skywars2.void_arcanum");owner.level().addFreshEntity(stale);service.loaded(stale);require(stale.isRemoved(),"unowned restored body rejected on native load");clear();
        }
        @Override public void close(){
            clear();for(var cow:mobs){game.items().indirectKills().clear(cow);cow.discard();}game.items().indirectKills().clear(owner);game.items().indirectKills().clear(other);
            owner.level().getBlockTicks().clearArea(bounds);owner.level().getFluidTicks().clearArea(bounds);
            for(var entry:blocks.entrySet()){if(owner.level().getBlockEntity(entry.getKey()) instanceof net.minecraft.world.Container container)container.clearContent();game.maps().restoreSnapshot(owner.level(),entry.getKey(),entry.getValue());}
            owner.level().getBlockTicks().clearArea(bounds);owner.level().getFluidTicks().clearArea(bounds);blockTicks.forEach(owner.level().getBlockTicks()::schedule);fluidTicks.forEach(owner.level().getFluidTicks()::schedule);
            fields(game.config(),config);game.server().getScoreboard().removePlayerTeam(team);
            saved.restore(game);otherSaved.restore(game);costs.forEach((name,value)->game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly(name),"lbc.math").set(value));
            for(var entity:owner.level().getAllEntities())if(!before.contains(entity)&&(entity instanceof net.minecraft.world.entity.item.ItemEntity||entity instanceof net.minecraft.world.entity.ExperienceOrb))entity.discard();game.players().save();game.items().magic().flushCleansing();
        }
    }
    public static void run(GameController game){try(var f=new Fixture(game)){f.init();f.presentation();f.holding();f.levels();f.physical();f.terrain();f.motion();f.boundaries();f.denseTerrain();}SkyWars2.LOGGER.info("[check] PASS void-arcanum (native holding/F swap, merged upkeep/majority/WILD, radial noisy erosion/lens, bounded moving/dense native erosion work, reduced nearest steering, tilt metadata and cleanup; explicit boundary ticks)");}

    private static Saved stopSaved;private static String stopEpoch;private static LbcVoidArcanum.Hole stopHole;
    private static final Set<Entity> stopBefore=Collections.newSetFromMap(new IdentityHashMap<>());
    public static void stopProbe(GameController game){
        require(stopSaved==null,"exclusive stop fixture");var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(owner!=null&&game.canUseGameplayItems(owner)&&game.items().voidArcanum().count()==0,"live clean stop fixture");
        stopSaved=new Saved(game,owner);owner.level().getAllEntities().forEach(stopBefore::add);
        try{
            owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcVoidArcanum.ITEM));owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);stopHole=game.items().voidArcanum().spawn(owner,owner.position().add(0,40,0));require(stopHole!=null,"actual native body before stop");
            // Keep pending erosion inert while separate commands perform the real match-stop boundary.
            owner.entityTags().add("time_stopped");stopHole.marker.entityTags().add("time_stopped");game.items().voidArcanum().updateVisual(stopHole,owner);stopEpoch=game.items().epoch();require(stopHole.visual!=null&&!stopHole.marker.isRemoved(),"live body and shared renderer before actual stop");
        }catch(RuntimeException|Error failure){restoreStop(game);throw failure;}SkyWars2.LOGGER.info("[check] PASS void-arcanum-stop-probe");
    }
    public static void cleanup(GameController game){
        try{require(stopSaved!=null&&!game.items().currentEpoch(stopEpoch)&&game.items().voidArcanum().count()==0&&stopHole.marker.isRemoved()&&game.items().blackHoleVisuals().count()==0,"actual match stop clears native body/shared renderer and item epoch");}
        finally{restoreStop(game);}SkyWars2.LOGGER.info("[check] PASS void-arcanum-cleanup");
    }
    private static void restoreStop(GameController game){
        if(stopSaved==null)return;var owner=stopSaved.player;game.items().voidArcanum().forget(owner);stopSaved.restore(game);
        for(var entity:owner.level().getAllEntities())if(!stopBefore.contains(entity)&&(entity instanceof net.minecraft.world.entity.item.ItemEntity||entity instanceof net.minecraft.world.entity.ExperienceOrb))entity.discard();stopBefore.clear();game.players().save();stopSaved=null;stopHole=null;stopEpoch=null;
    }
}
