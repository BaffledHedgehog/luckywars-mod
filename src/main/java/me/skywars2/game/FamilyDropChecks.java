package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Native drop/egg entry points and durable entity clocks. Suppression defaults remain pending author policy. */
public final class FamilyDropChecks {
    private FamilyDropChecks(){}
    private static Fixture retained;
    private static String stopEpoch;
    private static List<Entity> stopActors=List.of();
    private static ItemEntity stopToken;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Family drops: "+why);}
    private static CompoundTag saved(Entity e){var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,e.registryAccess());e.saveWithoutId(out);return out.buildResult();}
    private static boolean translated(Component text,String key){return text!=null&&(text.getContents() instanceof TranslatableContents t&&t.getKey().equals(key)||text.getSiblings().stream().anyMatch(c->translated(c,key)));}
    private static int clock(Entity e){return e.entityTags().stream().filter(t->t.startsWith("skywars2.family_clock.")).mapToInt(t->Integer.parseInt(t.substring("skywars2.family_clock.".length()))).findFirst().orElseThrow();}
    private static boolean visual(LightningBolt bolt){try{var field=LightningBolt.class.getDeclaredField("visualOnly");field.setAccessible(true);return field.getBoolean(bolt);}catch(ReflectiveOperationException e){throw new IllegalStateException("native lightning visual-only flag",e);}}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final AllGrimoireChecks.Fixture base;final LbcFamilyDrops family;
        final MatchRandomState random,playerRandom,betaRandom;
        final Map<net.minecraft.advancements.AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();
        final Map<net.minecraft.stats.Stat<Item>,Integer> eggStats=new LinkedHashMap<>();
        final LbcIndirectKills.TerrainProvenance provenance;
        final SauvojenMeditationChecks.Attribution attribution;
        final Set<Entity> initial=Collections.newSetFromMap(new IdentityHashMap<>());
        final List<Entity> owned=new ArrayList<>();final Vec3 at;final boolean frozen;
        BlockPos field;boolean keep,closed;
        Fixture(GameController game){
            this.game=game;family=game.items().familyDrops();require(family.snapshot().isEmpty(),"exclusive empty family service");
            require(game.server().tickRateManager().runsNormally()&&!game.items().timeStop().stopped(game.maps().level()),"normal unborrowed clock");
            base=new AllGrimoireChecks.Fixture(game);base.reset();at=base.at.add(0,2,0);frozen=game.server().tickRateManager().isFrozen();
            random=MatchRandomState.capture(base.level.getRandom());playerRandom=MatchRandomState.capture(base.base.owner.getRandom());betaRandom=MatchRandomState.capture(base.beta.getRandom());
            for(var kind:List.of(LbcSpecialEggs.Kind.MOM,LbcSpecialEggs.Kind.OTCHIM)){var stat=net.minecraft.stats.Stats.ITEM_USED.get(kind.item());eggStats.put(stat,base.base.owner.getStats().getValue(stat));}
            for(var advancement:game.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();base.base.owner.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(done::add);advancements.put(advancement,done);}
            base.level.getAllEntities().forEach(initial::add);provenance=game.items().indirectKills().captureTerrain(base.level,base.cells.keySet());
            attribution=new SauvojenMeditationChecks.Attribution(game,List.of(base.base.owner,base.beta));
            for(var p:base.cells.keySet())base.level.setBlock(p,Blocks.AIR.defaultBlockState(),818);
            movePlayers();
        }
        void movePlayers(){base.base.owner.teleportTo(base.level,at.x+30,at.y,at.z,Set.of(),0,0,true);base.beta.teleportTo(base.level,at.x+32,at.y,at.z,Set.of(),0,0,true);}
        void ticks(int n){for(int i=0;i<n;i++)family.tick();}
        void forgetAuthor(){
            // This fixture may exercise the real shared departure hook only when it cannot
            // retire another subsystem's borrowed transient spell or projectile.
            for(var level:game.server().getAllLevels())for(var e:level.getAllEntities()){
                var source=game.items().spellSources().source(e);
                require(source==null||!Objects.equals(source.author(),base.base.owner.getUUID())||source.persistent(),"exclusive persistent author sources before real forget hook");
            }
            game.items().spellSources().forget(base.base.owner);
        }
        ItemStack leaf(String id){var list=game.lbc().rollTable(base.level,at,base.base.owner,id);require(list.size()==1&&list.getFirst().getCount()==1,"one source leaf "+id);return list.getFirst();}
        ItemEntity token(ItemStack stack,Vec3 pos){var token=new ItemEntity(base.level,pos.x,pos.y,pos.z,stack);token.setThrower(base.base.owner);token.setNoGravity(true);token.setPickUpDelay(1000);require(base.level.addFreshEntity(token),"native dropped token inserted");owned.add(token);game.items().specialDrops().loaded(token);return token;}
        Entity drop(LbcFamilyDropItems.Kind kind,Vec3 pos){
            var before=family.snapshot();var token=token(new ItemStack(kind.item(),2),pos);
            require(game.items().currentEpoch(token)&&family.ready(token),"shared admission stamps epoch and resident readiness");
            require(game.items().specialDrops().spawn(token)&&token.getItem().getCount()==1,"shared dispatcher debits exactly one");
            require(!game.items().specialDrops().spawn(token)&&token.getItem().getCount()==1,"same-tick duplicate does not debit");
            var fresh=family.snapshot().stream().filter(e->!before.contains(e)).toList();require(fresh.size()==1,"one native family actor");
            game.items().specialDrops().unloaded(token);token.discard();var actor=fresh.getFirst();owned.add(actor);return actor;
        }
        void identity(Entity actor,boolean mom){
            require(mom?actor instanceof ArmorStand:actor instanceof Zombie,"exact native parent type");
            var living=(LivingEntity)actor;var head=living.getItemBySlot(mom?EquipmentSlot.MAINHAND:EquipmentSlot.HEAD);
            require(!head.isEmpty()&&Identifier.parse(mom?"lbc:mum":"lbc:otchim").equals(head.get(DataComponents.ITEM_MODEL)),"actual source visual equipment");
            require(translated(actor.getCustomName(),mom?"mom":"otchim")&&Objects.equals(actor.getCustomName(),head.get(DataComponents.CUSTOM_NAME)),"actual source equipment name copied to actor");
            require(actor.entityTags().containsAll(mom?Set.of("mom","rc_1sec","active"):Set.of("otchim","lucky_mob","active"))&&saved(actor).getBooleanOr("Invulnerable",false),"source tags and invulnerability");
            if(!mom)require(((Zombie)actor).isPersistenceRequired()&&living.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY),"persistent invisible Otchim");
            var source=game.items().spellSources().source(actor);
            require(source!=null&&game.items().currentEpoch(actor),"current native actor policy and epoch");
            if(mom)require(Objects.equals(source.author(),base.base.owner.getUUID())&&source.cause()!=null&&source.cause().player().equals(base.base.owner.getUUID())&&source.cause().friendlyFire(),"nonhostile mother retains original trigger metadata");
            else require(source.author()==null&&source.cause()==null&&source.nativeEmitter()==actor&&actor.entityTags().contains("skywars2.players_only_trap"),"Otchim is independently hostile to every player, with native mob attacker and no human kill author");
            require(clock(actor)==0,"new native clock starts at zero");
        }
        void guards(){
            var token=token(new ItemStack(LbcFamilyDropItems.Kind.MOM.item(),2),at);
            game.items().timeStop().begin(base.level,40);try{require(!game.items().specialDrops().spawn(token)&&token.getItem().getCount()==2,"world stop preserves token");}finally{game.items().timeStop().end(base.level);}
            game.server().tickRateManager().setFrozen(true);try{require(!game.items().specialDrops().spawn(token)&&token.getItem().getCount()==2,"native freeze preserves token");}finally{game.server().tickRateManager().setFrozen(false);}
            token.entityTags().add("skywars2.items.family-stale-fixture");require(!game.items().specialDrops().spawn(token)&&token.getItem().getCount()==2,"stale token rejected before debit");game.items().specialDrops().unloaded(token);token.discard();
            var remote=token(new ItemStack(LbcFamilyDropItems.Kind.MOM.item(),2),at);int x=(BlockPos.containing(at).getX()>>4)+128,z=(BlockPos.containing(at).getZ()>>4)+128;while(base.level.getChunkSource().getChunkNow(x,z)!=null)x+=128;
            remote.setPos(x*16+8,at.y,z*16+8);require(!game.items().specialDrops().spawn(remote)&&remote.getItem().getCount()==2&&base.level.getChunkSource().getChunkNow(x,z)==null,"unresident input cannot load/debit");game.items().specialDrops().unloaded(remote);remote.discard();
        }
        Allay birth(){
            var mom=drop(LbcFamilyDropItems.Kind.MOM,at.add(2.49,0,0));var dad=drop(LbcFamilyDropItems.Kind.OTCHIM,at);identity(mom,true);identity(dad,false);
            var otherMom=drop(LbcFamilyDropItems.Kind.MOM,at.add(8,0,0));var otherDad=drop(LbcFamilyDropItems.Kind.OTCHIM,at.add(-8,0,0));
            require(base.base.owner.entityTags().contains("have_mom"),"Mom grants existing dependency tag");
            var momPolicy=game.items().spellSources().source(mom);var dadPolicy=game.items().spellSources().source(dad);
            require(momPolicy.persistent()&&dadPolicy.persistent(),"independent parents carry persistent shared policies");forgetAuthor();
            require(momPolicy.equals(game.items().spellSources().source(mom))&&dadPolicy.equals(game.items().spellSources().source(dad)),"real author forget retains exact accepted parent provenance");
            var before=new HashSet<Entity>();base.level.getAllEntities().forEach(before::add);int expectedBolts=0;for(var e:base.level.getAllEntities())if(!e.isRemoved())expectedBolts++;ticks(4);require(!mom.isRemoved()&&!dad.isRemoved(),"birth waits for fifth family frame");
            var bodies=MatchActorCheckpoint.captureRegistered(game);var spells=new MatchSpellCheckpoint(game,bodies.damageSources());ticks(1);
            var children=family.snapshot().stream().filter(e->e instanceof Allay&&!e.isRemoved()).map(e->(Allay)e).toList();require(children.size()==1,"one actual allay birth inside2.5");var child=children.getFirst();owned.add(child);
            require(mom.isRemoved()&&dad.isRemoved()&&!otherMom.isRemoved()&&!otherDad.isRemoved(),"only chosen original parent pair removed");
            require(child.getMaxHealth()==100000&&child.getHealth()==100000&&saved(child).getBooleanOr("Glowing",false)&&child.isPersistenceRequired()&&translated(child.getCustomName(),"absurd_child"),"native child100000HP/glow/name/persistence");
            require(child.getMainHandItem().is(LbcLuckyBlocks.item(LbcLuckyBlocks.Kind.NORMAL))&&saved(child).getCompoundOrEmpty("drop_chances").getFloatOr("mainhand",-1)==0,"native Lucky block hand and zero hand drop chance");
            var bolts=new ArrayList<LightningBolt>();for(var e:base.level.getAllEntities())if(!before.contains(e)&&e instanceof LightningBolt bolt)bolts.add(bolt);
            require(bolts.size()==expectedBolts&&bolts.stream().noneMatch(FamilyDropChecks::visual),"datapack birth creates one real bolt per existing local entity, not per newly created bolt");owned.addAll(bolts);
            ticks(1);require(!family.snapshot().contains(mom)&&!family.snapshot().contains(dad),"consumed parents are removed from live service index before rewind");
            game.server().tickRateManager().setFrozen(true);
            try{bodies.restoreBodiesFrozen();require(bodies.restoreLinksFrozen().isEmpty(),"shared native rewind restores complete actor links");require(spells.restoreFrozen(bodies::resolve).isEmpty(),"shared spell rewind resolves policies and family service index");}
            finally{game.server().tickRateManager().setFrozen(false);}
            require(!mom.isRemoved()&&!dad.isRemoved()&&family.snapshot().contains(mom)&&family.snapshot().contains(dad)&&clock(mom)==4&&clock(dad)==4,"shared rewind revives and reindexes exact consumed parents with native clocks");
            require(child.isRemoved(),"shared native rewind removes future child");ticks(1);
            var replay=family.snapshot().stream().filter(e->e instanceof Allay&&!e.isRemoved()).map(e->(Allay)e).toList();
            require(replay.size()==1&&replay.getFirst()!=child&&mom.isRemoved()&&dad.isRemoved(),"restored service performs one actual rebirth without manual loaded callback");
            child=replay.getFirst();owned.add(child);for(var e:base.level.getAllEntities())if(!before.contains(e)&&e instanceof LightningBolt)owned.add(e);
            otherMom.discard();otherDad.discard();ticks(1);return child;
        }
        void cycle(Allay child){
            int phase=clock(child);var first=child.getMainHandItem().copy();ticks(19-phase);require(ItemStack.isSameItemSameComponents(first,child.getMainHandItem()),"hand does not cycle before twenty-frame boundary");ticks(1);
            require(child.getMainHandItem().is(LbcLuckyBlocks.item(LbcLuckyBlocks.Kind.NEKO)),"normal to native Neko");ticks(20);require(child.getMainHandItem().is(LbcLuckyBlocks.item(LbcLuckyBlocks.Kind.LOLI)),"Neko to native Loli");ticks(20);require(child.getMainHandItem().is(LbcLuckyBlocks.item(LbcLuckyBlocks.Kind.NORMAL)),"Loli to native normal");
        }
        Cow cow(Vec3 pos,String tag){var cow=new Cow(EntityTypes.COW,base.level);cow.setPos(pos);cow.setNoAi(true);cow.setNoGravity(true);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);if(tag!=null)cow.entityTags().add(tag);require(base.level.addFreshEntity(cow)&&cow.getMaxHealth()==100&&cow.getHealth()==100,"native ray contact fixture has100HP for independent nonlethal six-damage pulses");owned.add(cow);return cow;}
        void pulse(Allay child){ticks(20-clock(child));}
        void movement(Allay child){
            child.setPos(at);var player=base.base.owner;player.teleportTo(base.level,at.x,at.y,at.z+8,Set.of(),0,0,true);
            var target=cow(at.add(0,0,4),null);float hp=target.getHealth();pulse(child);
            var delta=player.getEyePosition().subtract(child.position()).normalize();
            var expected=new Vec3((int)(delta.x*10000)*.00005,(int)(delta.y*10000)*.00005,(int)(delta.z*10000)*.00005);
            require(child.getDeltaMovement().distanceToSqr(expected)<1e-16&&target.getHealth()==hp,"source quantized0.5 motion toward nearest player eyes; no fabricated attack ray");
            var accepted=game.items().spellSources().source(child);forgetAuthor();require(accepted.equals(game.items().spellSources().source(child)),"departure retains independent child policy");
            child.setDeltaMovement(Vec3.ZERO);pulse(child);require(child.getDeltaMovement().distanceToSqr(expected)<1e-16,"source motion continues after author departure hook");
            player.teleportTo(base.level,at.x,at.y,at.z+4,Set.of(),0,0,true);var sentinel=new Vec3(.03,.04,.05);child.setDeltaMovement(sentinel);pulse(child);
            require(child.getDeltaMovement().equals(sentinel),"a nonspectator within5 prevents only the movement refresh");
            target.discard();movePlayers();
            for(String key:List.of("minecraft:nexus","minecraft:imprinted")){
                var level=game.server().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,Identifier.parse(key)));
                require(level!=null,"actual excluded dimension exists "+key);int before=0;for(var e:level.getAllEntities())before++;
                require(family.birthLightning(level)==0,"no birth lightning in "+key);int after=0;for(var e:level.getAllEntities())after++;require(before==after,"excluded dimension unchanged "+key);
            }
        }
        void field(boolean active){if(active){field=BlockPos.containing(at).offset(3,0,0);base.level.setBlock(field,LbcMagic.MAGIC_BEACON_BLOCK.defaultBlockState(),3);game.items().magic().blockChanged(field,base.level.getBlockState(field));require(game.items().magic().suppressedAt(base.level,at),"real MAG beacon suppression active");}else if(field!=null){base.level.setBlock(field,Blocks.AIR.defaultBlockState(),3);game.items().magic().blockChanged(field,base.level.getBlockState(field));field=null;}}
        void clocks(Allay child){
            int clock=clock(child);var held=child.getMainHandItem().copy();game.items().timeStop().begin(base.level,40);try{ticks(21);require(clock(child)==clock&&ItemStack.isSameItemSameComponents(held,child.getMainHandItem()),"TimeStop pauses durable family clock");}finally{game.items().timeStop().end(base.level);}
            game.server().tickRateManager().setFrozen(true);try{ticks(21);require(clock(child)==clock,"native freeze pauses family clock");}finally{game.server().tickRateManager().setFrozen(false);}
            field(true);try{var still=new Vec3(.13,.14,.15);child.setDeltaMovement(still);pulse(child);require(!ItemStack.isSameItemSameComponents(held,child.getMainHandItem())&&child.getDeltaMovement().equals(still),"MAG blocks only source movement; native hand cycle remains active");var momAt=at.add(-2,0,0);base.level.setBlock(BlockPos.containing(momAt).below(),Blocks.NETHERRACK.defaultBlockState(),818);var mom=drop(LbcFamilyDropItems.Kind.MOM,momAt);ticks(20);require(base.level.getBlockState(mom.blockPosition()).is(Blocks.FIRE),"physical Mom acts under MAG");mom.discard();}finally{field(false);}
            var artifact=new ItemStack(Items.REPEATING_COMMAND_BLOCK);var flags=new CompoundTag();flags.putInt("mystical",1);flags.putInt("type",1);artifact.set(DataComponents.CUSTOM_DATA,CustomData.of(flags));base.base.owner.getInventory().setItem(0,artifact);base.base.owner.teleportTo(base.level,at.x+2,at.y,at.z,Set.of(),0,0,true);game.items().magic().inventoryChanged(base.base.owner);game.items().magic().flushCleansing();
            try{require(game.items().magic().artifactActive(base.base.owner)&&game.items().magic().suppressedAt(base.level,child.position()),"real inventory artifact covers child");var beforeHand=child.getMainHandItem().copy();var still=new Vec3(.23,.24,.25);child.setDeltaMovement(still);pulse(child);require(!ItemStack.isSameItemSameComponents(beforeHand,child.getMainHandItem())&&child.getDeltaMovement().equals(still),"artifact blocks only source movement; native hand cycle remains active");}finally{base.base.owner.getInventory().clearContent();game.items().magic().inventoryChanged(base.base.owner);game.items().magic().flushCleansing();movePlayers();}
            ticks(7);int n=clock(child);held=child.getMainHandItem().copy();var data=saved(child);family.unloaded(child);require(!family.snapshot().contains(child),"native unload detaches service reference");ticks(9);require(clock(child)==n,"unloaded actor clock stays in native body");
            family.loaded(child);require(family.snapshot().contains(child)&&clock(child)==n,"same body import reattaches native clock");
            var restored=new Allay(EntityTypes.ALLAY,base.level);restored.load(TagValueInput.create(ProblemReporter.DISCARDING,base.level.registryAccess(),data));restored.setUUID(UUID.randomUUID());family.unloaded(child);child.discard();require(base.level.addFreshEntity(restored),"saved native child body reinserted");owned.add(restored);family.loaded(restored);require(clock(restored)==n&&restored.getHealth()==100000&&ItemStack.isSameItemSameComponents(restored.getMainHandItem(),held),"native body load imports health/hand/persistent clock");ticks(1);require(clock(restored)==(n+1)%20,"import continues existing family clock");
        }
        void sources(){
            base.base.owner.teleportTo(base.level,at.x,at.y+1,at.z-3,Set.of(),0,0,true);
            for(var kind:LbcFamilyDropItems.Kind.values()){
                String id=kind==LbcFamilyDropItems.Kind.MOM?"lbc:item_defs/lbc/drop_not_trap__51f951f5f194":"lbc:item_defs/lbc/drop_boss__a5f2a6f79256";
                var stack=leaf(id);require(stack.is(kind.item())&&LbcFamilyDropItems.matches(stack,kind),"actual root-linked drop definition canonicalizes "+kind);
            }
            for(var kind:List.of(LbcSpecialEggs.Kind.MOM,LbcSpecialEggs.Kind.OTCHIM)){
                boolean mom=kind==LbcSpecialEggs.Kind.MOM;var stack=leaf(mom?"lbc:item_defs/minecraft/pig_spawn_egg__05968de6b978":"lbc:item_defs/minecraft/snow_golem_spawn_egg__b6d3d42c6315");
                require(stack.is(kind.item())&&stack.getItem() instanceof SpawnEggItem&&Identifier.parse(mom?"minecraft:pig_spawn_egg":"minecraft:snow_golem_spawn_egg").equals(stack.get(DataComponents.ITEM_MODEL)),"source native egg and original proxy "+kind);
                var before=new HashSet<Entity>();base.level.getAllEntities().forEach(before::add);var pad=BlockPos.containing(at).below();base.level.setBlock(pad,Blocks.STONE.defaultBlockState(),818);stack.setCount(2);base.base.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
                var hit=new BlockHitResult(Vec3.atCenterOf(pad).add(0,.5,0),Direction.UP,pad,false);
                require(base.base.owner.gameMode.useItemOn(base.base.owner,base.level,stack,InteractionHand.MAIN_HAND,hit).consumesAction()&&stack.getCount()==1,"real egg interaction debits one "+kind);
                var fresh=new ArrayList<Entity>();for(var e:base.level.getAllEntities())if(!before.contains(e))fresh.add(e);owned.addAll(fresh);
                var tokens=fresh.stream().filter(e->e instanceof ItemEntity).map(e->(ItemEntity)e).toList();require(tokens.size()==1&&fresh.stream().noneMatch(e->e instanceof LivingEntity),"egg creates actual intermediate item before family dispatcher");var token=tokens.getFirst();
                require(LbcFamilyDropItems.matches(token.getItem(),mom?LbcFamilyDropItems.Kind.MOM:LbcFamilyDropItems.Kind.OTCHIM)&&game.items().specialDrops().owner(token)==base.base.owner&&game.items().currentEpoch(token),"egg intermediate preserves causal owner and admission");
                var actors=family.snapshot();require(game.items().specialDrops().spawn(token)&&token.isRemoved(),"intermediate reaches shared dispatcher and consumes final unit");var created=family.snapshot().stream().filter(e->!actors.contains(e)).toList();require(created.size()==1,"egg dispatcher creates one parent");identity(created.getFirst(),mom);owned.addAll(created);created.forEach(Entity::discard);
            }
        }
        public void close(){if(closed)return;closed=true;try{field(false);game.items().timeStop().end(base.level);game.server().tickRateManager().setFrozen(frozen);if(!keep){family.stop();for(var e:owned){game.items().specialDrops().unloaded(e);game.server().getScoreboard().resetAllPlayerScores(e);e.discard();}}}finally{try{base.close();}finally{eggStats.forEach((stat,n)->base.base.owner.getStats().setValue(base.base.owner,stat,n));advancements.forEach((a,done)->{for(var key:a.value().criteria().keySet())if(done.contains(key))base.base.owner.getAdvancements().award(a,key);else base.base.owner.getAdvancements().revoke(a,key);});game.items().indirectKills().restoreTerrain(provenance);attribution.close();random.restore(base.level.getRandom());playerRandom.restore(base.base.owner.getRandom());betaRandom.restore(base.beta.getRandom());}}}
    }
    public static void run(GameController game){try(var f=new Fixture(game)){f.guards();var child=f.birth();f.cycle(child);f.movement(child);f.clocks(child);var actors=f.family.snapshot();f.family.stop();require(f.family.snapshot().isEmpty()&&actors.stream().allMatch(Entity::isRemoved),"service stop removes native actors and references");}SkyWars2.LOGGER.info("[check] PASS family-drops (shared token admission/debit, native parents/cause, datapack local real birth lightning, native three-block cycle, quantized player-follow motion/no invented attack, TimeStop/freeze, ungated parents/chat/hand cycle; source movement MAG, native saved clock import, quiet cleanup)");}
    public static void source(GameController game){try(var f=new Fixture(game)){f.sources();}SkyWars2.LOGGER.info("[check] PASS family-drop-source (root-linked canonical tokens; actual Mom pig/Otchim snow-golem egg use; native item intermediate and shared dispatcher)");}
    public static void stopProbe(GameController game){require(retained==null,"exclusive retained stop probe");var f=new Fixture(game);try{f.drop(LbcFamilyDropItems.Kind.MOM,f.at);f.drop(LbcFamilyDropItems.Kind.OTCHIM,f.at.add(8,0,0));stopActors=f.family.snapshot();stopEpoch=game.items().epoch();stopToken=f.token(new ItemStack(LbcFamilyDropItems.Kind.MOM.item(),3),f.at.add(0,4,0));game.items().specialDrops().unloaded(stopToken);f.keep=true;f.base.retain=true;f.base.base.retain=true;retained=f;}finally{f.close();}SkyWars2.LOGGER.info("[check] PASS family-drops-stop-probe");}
    public static void cleanup(GameController game){require(retained!=null,"retained actual-stop fixture");try{require(!game.items().currentEpoch(stopEpoch)&&game.items().familyDrops().snapshot().isEmpty()&&stopActors.stream().allMatch(Entity::isRemoved),"actual match stop clears epoch/native parents/service");require(stopToken.getItem().getCount()==3&&!game.items().specialDrops().spawn(stopToken),"old pending token cannot replay or debit");}finally{for(var e:retained.owned){game.items().specialDrops().unloaded(e);game.server().getScoreboard().resetAllPlayerScores(e);e.discard();}retained.base.restore();retained=null;stopToken=null;stopActors=List.of();stopEpoch=null;}SkyWars2.LOGGER.info("[check] PASS family-drops-cleanup");}
}
