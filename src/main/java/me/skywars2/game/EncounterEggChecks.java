package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Real item interactions, native configured actors/death loot and actual match-stop fixtures. */
public final class EncounterEggChecks {
    private EncounterEggChecks() { }
    private static final List<LbcSpecialEggs.Kind> KINDS=List.of(LbcSpecialEggs.Kind.FAT_SKELETON,
            LbcSpecialEggs.Kind.LUCKY_PIG,LbcSpecialEggs.Kind.HONEY_BEE,LbcSpecialEggs.Kind.STALIN,
            LbcSpecialEggs.Kind.TRANSFORMATOR,LbcSpecialEggs.Kind.HELICOPTER,LbcSpecialEggs.Kind.ANT);
    private static Fixture running, stopping;
    private static String stopEpoch;
    private static List<Entity> stopActors=List.of();
    private static void require(boolean value,String message) {
        if(!value)throw new IllegalStateException("Encounter eggs: "+message);
    }
    private static net.minecraft.nbt.CompoundTag saved(Entity entity) {
        var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,entity.registryAccess());
        entity.saveWithoutId(output);return output.buildResult();
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final ServerLevel level;
        final ServerPlayer owner;
        final GameChecks.WandThirdPlayerState state;
        final Vec3 origin;
        final BlockPos pad;
        final Set<Entity> initial=Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<Entity> owned=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final Set<Long> forced=new HashSet<>();
        final Map<net.minecraft.stats.Stat<Identifier>,Integer> stats=new LinkedHashMap<>();
        final boolean mobDrops;
        int phase,lastTick,wait;
        List<Entity> frozen=List.of();
        final Map<Entity,Vec3> positions=new IdentityHashMap<>();
        Fixture(GameController game) {
            this.game=game;level=game.maps().level();
            owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            require(game.playing()&&owner!=null&&game.allowDamage(owner),"actual active match participant");
            state=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
            mobDrops=level.getGameRules().get(GameRules.MOB_DROPS);
            for(var id:BuiltInRegistries.CUSTOM_STAT){var stat=net.minecraft.stats.Stats.CUSTOM.get(id);stats.put(stat,owner.getStats().getValue(stat));}
            origin=new Vec3(-280.5,230,-280.5);
            pad=BlockPos.containing(origin).below();level.getAllEntities().forEach(initial::add);
        }
        void initialize() {
            for(int x=(pad.getX()-64)>>4;x<=(pad.getX()+64)>>4;x++)for(int z=(pad.getZ()-64)>>4;z<=(pad.getZ()+64)>>4;z++){
                long key=net.minecraft.world.level.ChunkPos.pack(x,z);
                if(!level.getForceLoadedChunks().contains(key)){forced.add(key);level.setChunkForced(x,z,true);}level.getChunk(x,z);
            }
            for(var p:BlockPos.betweenClosed(pad.offset(-3,0,-3),pad.offset(3,5,3)))set(p,Blocks.AIR.defaultBlockState());
            set(pad,Blocks.STONE.defaultBlockState());
            owner.closeContainer();owner.getInventory().clearContent();owner.removeAllEffects();owner.setGameMode(GameType.SURVIVAL);
            owner.setNoGravity(true);owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);owner.setHealth(1000);owner.setAbsorptionAmount(0);
            owner.teleportTo(level,origin.x,origin.y+2,origin.z-3,Set.of(),0,0,true);
            level.getGameRules().set(GameRules.MOB_DROPS,true,game.server());lastTick=game.server().getTickCount();
        }
        void set(BlockPos p,net.minecraft.world.level.block.state.BlockState value) {
            p=p.immutable();blocks.computeIfAbsent(p,where->game.maps().snapshot(level,where));
            if(level.getBlockEntity(p) instanceof net.minecraft.world.Container c)c.clearContent();level.setBlock(p,value,818);
        }
        Set<Entity> current() {var out=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(out::add);return out;}
        List<Entity> fresh(Set<Entity> before) {var out=new ArrayList<Entity>();for(var e:level.getAllEntities())if(!before.contains(e)){owned.add(e);out.add(e);}return out;}
        void capture(){for(var e:level.getAllEntities())if(!initial.contains(e)&&e.position().distanceToSqr(origin)<18000)owned.add(e);}
        ItemStack egg(LbcSpecialEggs.Kind kind,int count){var s=game.lbc().rollTable(level,origin,owner,"lbc:ported/"+leaf(kind)).getFirst();s.setCount(count);return s;}
        String leaf(LbcSpecialEggs.Kind kind){return BuiltInRegistries.ITEM.getKey(kind.item()).getPath();}
        BlockHitResult hit(){return new BlockHitResult(Vec3.atCenterOf(pad).add(0,.5,0),Direction.UP,pad,false);}
        boolean use(ItemStack stack,InteractionHand hand){owner.setItemInHand(hand,stack);return owner.gameMode.useItemOn(owner,level,stack,hand,hit()).consumesAction();}
        Entity spawn(LbcSpecialEggs.Kind kind,InteractionHand hand){
            var before=current();var stack=egg(kind,2);require(use(stack,hand)&&stack.getCount()==1,"actual native clicked-face use spends exactly one "+kind);
            var actors=fresh(before).stream().filter(e->e instanceof Mob||e instanceof ItemEntity).toList();
            require(actors.size()==1,"one native actor/token "+kind);var actor=actors.getFirst();
            require(game.items().currentEpoch(actor)&&Math.abs(actor.getX()-origin.x)<1e-6&&Math.abs(actor.getZ()-origin.z)<1e-6,"native position and pre-insertion epoch "+kind);
            return actor;
        }
        void presentation(){
            for(var kind:KINDS){
                var stack=egg(kind,2);require(stack.is(kind.item())&&stack.getItem() instanceof SpawnEggItem,"native public leaf "+kind);
                var flags=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
                require(flags.size()==1&&flags.getIntOr("imba",0)==1&&stack.get(DataComponents.ENTITY_DATA)!=null,"only original imba flag and native payload "+kind);
                var name=stack.get(DataComponents.CUSTOM_NAME);var lore=stack.get(DataComponents.LORE);
                require(name.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tr&&tr.getKey().equals(leaf(kind))&&!name.getStyle().isItalic()&&name.getStyle().getColor().getValue()==0xff5555&&lore.lines().size()==1,"source red name and one-row lore "+kind);
                var model=switch(kind){case FAT_SKELETON->"skeleton_spawn_egg";case LUCKY_PIG->"pig_spawn_egg";case HONEY_BEE->"bee_spawn_egg";case STALIN->"zombified_piglin_spawn_egg";case TRANSFORMATOR,ANT->"zombie_spawn_egg";case HELICOPTER->"snow_golem_spawn_egg";default->throw new IllegalStateException();};
                require(Identifier.withDefaultNamespace(model).equals(stack.get(DataComponents.ITEM_MODEL)),"source vanilla model "+kind);
            }
        }
        boolean warm(){
            require(++wait<=200,"test-only resident area becomes entity ticking");
            for(int x=(pad.getX()-64)>>4;x<=(pad.getX()+64)>>4;x++)for(int z=(pad.getZ()-64)>>4;z<=(pad.getZ()+64)>>4;z++)
                if(!level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.pack(x,z)))return false;
            wait=0;return true;
        }
        void admission(){
            for(var kind:KINDS){
                var stack=egg(kind,2);var before=current();game.items().timeStop().begin(level,200);
                try{require(!use(stack,InteractionHand.MAIN_HAND)&&stack.getCount()==2&&fresh(before).isEmpty(),"Time Stop rejects without debit "+kind);}finally{game.items().timeStop().end(level);}
                var prior=new GameChecks.WandThirdPlayerState(owner,game.items().mana());game.spectators().enter(owner);
                try{require(!use(stack,InteractionHand.MAIN_HAND)&&stack.getCount()==2&&fresh(before).isEmpty(),"observer rejects without debit "+kind);}finally{game.spectators().remove(owner);prior.restore(game);}
            }
        }
        void payload(LbcSpecialEggs.Kind kind,Entity actor){
            var tag=saved(actor);
            switch(kind){
                case FAT_SKELETON->{var s=(net.minecraft.world.entity.monster.skeleton.Skeleton)actor;
                    require(s.getAttributeValue(Attributes.MOVEMENT_SPEED)==.6&&s.getAttributeValue(Attributes.ARMOR)==20&&s.getAttributeValue(Attributes.ATTACK_KNOCKBACK)==100,"native source skeleton attributes");
                    require(s.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.BONE)&&s.getItemBySlot(EquipmentSlot.MAINHAND).getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY).modifiers().isEmpty()&&s.getItemBySlot(EquipmentSlot.LEGS).isEmpty(),"egg skeleton remains distinct from normal bone modifiers/leg drops");
                    require(tag.getStringOr("DeathLootTable","").equals("lbc:fat_skeleton_rewards"),"complete native skeleton rewards alias");}
                case LUCKY_PIG->require(actor instanceof net.minecraft.world.entity.animal.pig.Pig&&actor.isCustomNameVisible()&&tag.getStringOr("DeathLootTable","").equals("lbc:applepig"),"native named lucky-pig loot");
                case HONEY_BEE->require(actor instanceof net.minecraft.world.entity.animal.bee.Bee&&tag.getBooleanOr("HasNectar",false)&&tag.getStringOr("DeathLootTable","").equals("lbc:honey"),"native nectar bee and honey loot");
                case STALIN->{var s=(net.minecraft.world.entity.monster.zombie.ZombifiedPiglin)actor;
                    require(s.getHealth()==500&&s.getMaxHealth()==500&&s.isSilent()&&s.isPersistenceRequired()&&tag.getStringOr("DeathLootTable","").equals("lbc:stalin"),"native500HP Stalin reward/lifecycle");
                    require(s.getItemBySlot(EquipmentSlot.CHEST).get(DataComponents.EQUIPPABLE).assetId().orElseThrow().identifier().equals(Identifier.parse("lbc:stalin")),"source Stalin equipment asset");}
                case TRANSFORMATOR->{var z=(Zombie)actor;require(z.isBaby()&&z.isNoAi()&&z.getHealth()==500&&z.getMaxHealth()==500&&z.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY),"egg baby500HP Transformer preserves original NoAI/effect");
                    require(z.getCustomName().getString().contains("05.06.2024")&&game.items().transformator().snapshot().stream().anyMatch(v->v.actor()==z&&v.text()==null),"source birthday label, without invented text display");}
                case ANT->{var z=(Zombie)actor;require(z.isBaby()&&z.isSilent()&&z.getHealth()==10&&z.getMaxHealth()==10&&z.getAttributeValue(Attributes.SPAWN_REINFORCEMENTS_CHANCE)==100,"native10HP ant source traits");
                    require(z.getItemBySlot(EquipmentSlot.HEAD).get(DataComponents.UNBREAKABLE)==null&&tag.getStringOr("DeathLootTable","").equals("lbc:ant_rewards"),"egg ant keeps vanishing helmet instead of drop's unbreakable preset");}
                case HELICOPTER->{var token=(ItemEntity)actor;require(saved(token).getIntOr("PickupDelay",0)==32767&&token.getItem().is(Items.REPEATING_COMMAND_BLOCK)&&LbcHelicopterEncounter.isTrigger(token.getItem())&&token.getOwner()==owner,"source unpickable helicopter token and actual summoner");return;}
                default->throw new IllegalStateException();
            }
            require(LbcIndirectKills.playerSource(actor)==owner,"existing causal/lifecycle adoption "+kind);
        }
        Cow cow(Vec3 at){var c=new Cow(EntityTypes.COW,level);c.setPos(at);c.setNoAi(true);c.setNoGravity(true);require(level.addFreshEntity(c),"native victim");owned.add(c);return c;}
        void reward(Mob actor,String table){
            var loot=game.server().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse(table)));
            var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,actor.position())
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,actor).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE,actor.damageSources().generic())
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
            long seed=93175491;var expected=new ArrayList<ItemStack>(loot.getRandomItems(params,seed));require(!expected.isEmpty(),"complete source reward table is live "+table);
            var tag=saved(actor);tag.putLong("DeathLootTableSeed",seed);actor.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),tag));
            var before=current();require(actor.hurtServer(level,actor.damageSources().generic(),1000000)&&!actor.isAlive(),"actual native reward death "+table);
            var actual=fresh(before).stream().filter(e->e instanceof ItemEntity).map(e->((ItemEntity)e).getItem().copy()).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            for(var wanted:expected){int n=wanted.getCount();for(var got:actual)if(ItemStack.isSameItemSameComponents(got,wanted)){int taken=Math.min(n,got.getCount());got.shrink(taken);n-=taken;}require(n==0,"actual native death retains complete reward stack "+wanted+" from "+table);}
            fresh(before).forEach(Entity::discard);
        }
        void families(){
            int i=0;for(var kind:KINDS){var actor=spawn(kind,(i++%2==0)?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND);payload(kind,actor);
                switch(kind){
                    case FAT_SKELETON->reward((Mob)actor,"lbc:fat_skeleton_rewards");
                    case LUCKY_PIG->reward((Mob)actor,"lbc:applepig");
                    case HONEY_BEE->reward((Mob)actor,"lbc:honey");
                    case STALIN->{var s=(net.minecraft.world.entity.monster.zombie.ZombifiedPiglin)actor;var victim=cow(s.position().add(0,0,2));victim.setHealth(1);
                        require(s.doHurtTarget(level,victim)&&!victim.isAlive()&&victim.getKillCredit()==owner,"real egg Stalin melee keeps summoner kill credit");victim.discard();reward(s,"lbc:stalin");}
                    case ANT->{var victim=cow(actor.position().add(0,0,3));var acid=game.items().antDrops().shoot((Zombie)actor,victim);require(acid!=null&&LbcIndirectKills.playerSource(acid)==owner,"real egg ant uses shared acid with summoner");owned.add(acid);acid.discard();victim.discard();reward((Mob)actor,"lbc:ant_rewards");}
                    case TRANSFORMATOR->{var z=(Zombie)actor;var victim=cow(z.position().add(0,.5,1.6));victim.setHealth(.1f);
                        var before=new HashSet<>(game.items().wands().electroSnapshot());game.items().transformator().burst(z);
                        var shots=game.items().wands().electroSnapshot().stream().filter(e->!before.contains(e)).toList();
                        require(shots.size()==7&&shots.stream().allMatch(e->e.source()==z&&e.mechanism&&LbcIndirectKills.playerSource(e.source())==owner),"egg Transformer emits seven real shared attributed electric arcs");
                        for(int tick=0;tick<31;tick++)game.items().wands().tick();
                        require(!victim.isAlive()&&victim.getKillCredit()==owner,"actual egg Transformer shared arc hit/death keeps summoner credit");victim.discard();}
                    case HELICOPTER->helicopter((ItemEntity)actor);
                    default->throw new IllegalStateException();
                }
                actor.discard();capture();owned.stream().filter(e->!e.isRemoved()).forEach(Entity::discard);
            }
        }
        void helicopter(ItemEntity token){
            var before=current();require(game.items().helicopterEncounter().spawn(token)&&token.isRemoved(),"native egg token dispatches one boss and one debit");fresh(before);
            var boss=game.items().helicopterEncounter().snapshot().stream().filter(v->owned.contains(v.hitbox())).findFirst().orElseThrow();
            require(boss.hitbox().getMaxHealth()==150&&LbcIndirectKills.playerSource(boss.hitbox())==owner,"egg helicopter adopts existing150HP encounter and causal author");
            game.items().helicopterEncounter().tick();capture();var arrows=game.items().helicopterEncounter().attackSnapshot().stream().filter(e->!e.isRemoved()&&e instanceof net.minecraft.world.entity.projectile.arrow.Arrow).map(e->(net.minecraft.world.entity.projectile.arrow.Arrow)e).toList();
            require(!arrows.isEmpty(),"real boss dispatch fires native arrows");var arrow=arrows.getFirst();owned.add(arrow);
            require(arrow.getOwner()==boss.hitbox()&&LbcIndirectKills.playerSource(arrow)==owner,"native NPC projectile owner stays distinct from player causal author");
            var victim=cow(origin.add(0,3,9));victim.setHealth(1);arrow.setPos(origin.add(0,3,8));arrow.setNoGravity(true);arrow.setDeltaMovement(0,0,2);arrow.tick();
            require(!victim.isAlive()&&victim.getKillCredit()==owner,"actual native helicopter arrow collision/death inherits summoner kill");victim.discard();
            boss.display().discard();boss.hitbox().discard();
        }
        void offspring(){
            var parent=new Zombie(EntityTypes.ZOMBIE,level);parent.setPos(origin.add(2,0,0));parent.setNoAi(true);require(level.addFreshEntity(parent),"ordinary native parent");owned.add(parent);
            for(var kind:List.of(LbcSpecialEggs.Kind.ANT,LbcSpecialEggs.Kind.TRANSFORMATOR)){
                var stack=egg(kind,2);owner.setItemInHand(InteractionHand.OFF_HAND,stack);var before=current();
                require(parent.interact(owner,InteractionHand.OFF_HAND,Vec3.ZERO).consumesAction()&&stack.getCount()==1,"native matching-mob use consumes one "+kind);
                var children=fresh(before).stream().filter(e->e instanceof Zombie).toList();require(children.size()==1,"one configured native child "+kind);payload(kind,children.getFirst());children.forEach(Entity::discard);
                require(!parent.isBaby()&&!parent.isSilent()&&parent.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),"existing parent remains ordinary");
            }parent.discard();
        }
        void freeze(){
            for(var kind:KINDS){var actor=spawn(kind,InteractionHand.MAIN_HAND);if(actor instanceof ItemEntity token){var before=current();require(game.items().helicopterEncounter().spawn(token),"freeze probe creates actual boss");fresh(before);}}
            capture();frozen=owned.stream().filter(e->!e.isRemoved()&&(e instanceof Mob||e instanceof net.minecraft.world.entity.Display)).toList();
            require(frozen.size()==8,"six native actors and helicopter display/hitbox");for(var e:frozen)positions.put(e,e.position());
            game.items().timeStop().begin(level,200);
        }
        boolean step(){int now=game.server().getTickCount();if(now==lastTick)return false;lastTick=now;require(game.playing(),"actual match retained");
            if(phase==0){if(!warm())return false;presentation();admission();families();offspring();capture();owned.forEach(Entity::discard);freeze();phase++;return false;}
            require(frozen.stream().allMatch(e->!e.isRemoved()&&e.position().distanceTo(positions.get(e))<1e-8),"actual dimension Time Stop freezes all seven egg families");
            if(++wait<3)return false;game.items().timeStop().end(level);return true;
        }
        void close(boolean retain){capture();game.items().timeStop().end(level);if(!retain)owned.forEach(Entity::discard);
            for(var entry:blocks.entrySet()){if(level.getBlockEntity(entry.getKey()) instanceof net.minecraft.world.Container c)c.clearContent();game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue());}
            state.restore(game);game.items().indirectKills().clear(owner);stats.forEach((stat,value)->owner.getStats().setValue(owner,stat,value));level.getGameRules().set(GameRules.MOB_DROPS,mobDrops,game.server());
            for(long key:forced)level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
        }
        public void close(){close(false);}
    }
    public static void run(GameController game){require(running==null&&stopping==null,"exclusive fixture");var f=new Fixture(game);running=f;try{f.initialize();}catch(RuntimeException|Error e){running=null;f.close();throw e;}}
    public static void tick(GameController game){
        if(stopping!=null&&stopEpoch==null){var f=stopping;try{if(!f.warm())return;f.freeze();stopActors=f.frozen;stopEpoch=game.items().epoch();f.close(true);game.items().timeStop().begin(f.level,200);SkyWars2.LOGGER.info("[check] PASS encounter-eggs-stop-probe");}catch(Throwable e){f.close();stopping=null;SkyWars2.LOGGER.error("[check] FAIL encounter-eggs-stop-probe",e);}return;}
        if(running==null)return;var f=running;try{if(f.step()){f.close();running=null;SkyWars2.LOGGER.info("[check] PASS encounter-eggs");}}catch(Throwable e){try{f.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL encounter-eggs",e);}
    }
    public static void stopProbe(GameController game){require(running==null&&stopping==null,"exclusive stop fixture");var f=new Fixture(game);stopping=f;try{f.initialize();}catch(RuntimeException|Error e){stopping=null;f.close();throw e;}}
    public static void cleanup(GameController game){require(stopping!=null&&!game.items().currentEpoch(stopEpoch)&&stopActors.size()==8&&stopActors.stream().allMatch(Entity::isRemoved),"actual stop quietly clears all seven native egg families");var f=stopping;
        require(f.level.getEntities((Entity)null,new net.minecraft.world.phys.AABB(f.origin,f.origin).inflate(120),e->e instanceof ItemEntity&&!f.initial.contains(e)&&!e.isRemoved()).isEmpty(),"quiet stop creates no encounter rewards");stopping=null;stopActors=List.of();stopEpoch=null;SkyWars2.LOGGER.info("[check] PASS encounter-eggs-cleanup");}
}
