package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Actual installed clone service, native bodies and staged ordinary server movement. */
public final class CloneSkillChecks {
    private CloneSkillChecks(){}
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Clone skills: "+why);}
    private static void near(double actual,double expected,String why){require(Math.abs(actual-expected)<1e-5,why+" actual="+actual+" expected="+expected);}
    private static Object read(Object value,String name){
        for(Class<?> type=value.getClass();type!=null;type=type.getSuperclass())try{var field=type.getDeclaredField(name);field.setAccessible(true);return field.get(value);}catch(NoSuchFieldException ignored){}catch(IllegalAccessException error){throw new IllegalStateException(error);}
        throw new IllegalStateException("Missing reviewed fixture field "+name);
    }
    @SuppressWarnings("unchecked") private static Map<Object,Object> map(Object value,String name){return (Map<Object,Object>)read(value,name);}
    public static boolean active(){return running!=null;}
    public static void run(GameController game){
        require(running==null,"exclusive native fixture");var fixture=new Fixture(game);running=fixture;
        try{fixture.initialize();fixture.menu();fixture.spread();fixture.composition();fixture.wild();fixture.mobs();fixture.relationships();fixture.effects();fixture.protection();fixture.customOwners();fixture.lateOwners();fixture.prepareRestoration();}
        catch(Throwable failure){try{fixture.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}running=null;throw failure;}
    }
    public static void tick(GameController game){
        if(running==null)return;try{if(running.advance()){running.close();running=null;SkyWars2.LOGGER.info("[check] PASS clone-skills (installed service; source menu gates; x2*x4=8 once composition; WILD owner cases; native gear/AI/passenger/leash/fishing; independent causal effect queues; real Air flight continuation; protected authority)");}}
        catch(Throwable failure){try{running.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL clone-skills",failure);}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer owner,other;
        final RegenerationBeltChecks.Snapshot saved,otherSaved;
        final boolean skills,ranked,mobDrops;final net.minecraft.world.scores.PlayerTeam ownerTeam,otherTeam;
        final FishingHook originalFishing;final int cooldown,otherCooldown;
        final Map<java.lang.reflect.Field,Object> serviceState=new LinkedHashMap<>();
        final Set<Entity> actors=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();
        final String prefix="clone_fixture."+UUID.randomUUID();int serial,stage,began;
        net.minecraft.world.scores.PlayerTeam team;RegenerationBeltChecks.ScheduledClient client,audience;Vec3 at;
        LbcAir.Charge air,airCopy;Vec3 flightStart;int flightAge;String radiusInside,radiusOutside;boolean closed;
        private record PolymorphCopy(Entity sheep,LivingEntity hidden,Object form){}
        final List<PolymorphCopy> restoringCopies=new ArrayList<>();
        Entity originalPolymorphBody;Object originalPolymorphForm;LbcIndirectKills.Owner polymorphPoisonCause;
        Fixture(GameController game){
            this.game=game;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(owner!=null&&other!=null&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other),"two real active participants");
            require(game.playing()&&game.items().cloneSkills().ready(),"installed public adapter ready in actual match");
            saved=new RegenerationBeltChecks.Snapshot(game,owner);otherSaved=new RegenerationBeltChecks.Snapshot(game,other);
            skills=game.config().lbcSkills;ranked=game.config().ranked;mobDrops=owner.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_DROPS);ownerTeam=owner.getTeam();otherTeam=other.getTeam();originalFishing=owner.fishing;cooldown=owner.damageCooldownTime;otherCooldown=other.damageCooldownTime;
            require(originalFishing==null&&owner.getVehicle()==null&&other.getVehicle()==null,"fixture starts without external fishing/riding authority");
            for(var name:List.of("ownerCursor","epoch","scanned","operations","bodies","unsupported","failed"))try{var f=service().getClass().getDeclaredField(name);f.setAccessible(true);serviceState.put(f,f.get(service()));}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        }
        LbcCloneSkills service(){return game.items().cloneSkills();}
        void initialize(){
            game.config().lbcSkills=true;game.config().ranked=true;owner.level().getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MOB_DROPS,true,game.server());
            for(var p:List.of(owner,other)){
                p.closeContainer();p.stopUsingItem();p.getInventory().clearContent();p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);
                p.entityTags().removeAll(Set.of("time_stopped","spectator","strict_map_object","magic_academy","rcdenyalways","sin_greed_active","nomagic_active"));
                var profile=game.players().get(p.getUUID());profile.lbcSkill=0;profile.lbcKit=0;profile.lbcChallengeKit=0;profile.lbcChallenge=0;profile.lbcTrinket=0;
                game.items().magic().inventoryChanged(p);game.items().armorSets().inventoryChanged(p);
            }
            game.items().magic().flushCleansing();
            int x=(owner.blockPosition().getX()>>4)*16+8,z=(owner.blockPosition().getZ()>>4)*16+8,y=Math.min(200,owner.level().getMaxY()-48);
            for(int dx=-6;dx<=6;dx++)for(int dz=-6;dz<=6;dz++)for(int dy=0;dy<=3;dy++){
                var pos=new BlockPos(x+dx,y+dy,z+dz);require(owner.level().hasChunkAt(pos),"bounded resident fixture terrain");terrain.put(pos,game.maps().snapshot(owner.level(),pos));owner.level().setBlock(pos,(dy==0?Blocks.STONE:Blocks.AIR).defaultBlockState(),2);
            }
            at=new Vec3(x+.5,y+1,z+.5);owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),0,-90,true);other.teleportTo(owner.level(),at.x+5,at.y,at.z,Set.of(),0,-90,true);
            client=new RegenerationBeltChecks.ScheduledClient(game,owner);audience=new RegenerationBeltChecks.ScheduledClient(game,other);
            client.acknowledge();audience.acknowledge();require(!game.items().magic().suppressed(owner),"actual unsuppressed native positive control");
        }
        void menu(){
            game.config().ranked=false;var profile=game.players().get(owner.getUUID());
            for(var row:List.of(new Object[]{15,14,"lbc:true_advancements/bows/double"},new Object[]{17,16,"lbc:true_advancements/hoes/sickle"})){
                int id=(Integer)row[0],slot=(Integer)row[1];String gate=(String)row[2];
                game.menus().openDebug(owner);owner.entityTags().remove("skywars2.debug_perks");
                var advancement=game.server().getAdvancements().get(Identifier.parse(gate));require(advancement!=null,"authored advancement "+gate);
                profile.lbcSkill=0;profile.lbcUnlocks.remove(gate);for(var criterion:advancement.value().criteria().keySet())owner.getAdvancements().revoke(advancement,criterion);
                require(!game.lbc().select(owner,LbcLoadouts.Kind.SKILLS,10000,slot),"missing achievement rejects ordinary skill"+id);
                for(var criterion:advancement.value().criteria().keySet())owner.getAdvancements().award(advancement,criterion);
                require(game.lbc().select(owner,LbcLoadouts.Kind.SKILLS,10000,slot)&&profile.lbcSkill==id,"authored unlock selects skill"+id);
                game.menus().open(owner,GameMenus.Page.LBC_SKILLS);owner.entityTags().remove("skywars2.debug_perks");
                var gui=eu.pb4.sgui.api.SguiUtils.getCurrentGui(owner);require(gui instanceof eu.pb4.sgui.api.gui.SimpleGui,"real ordinary skills GUI");
                var menu=(eu.pb4.sgui.api.gui.SimpleGui)gui;var button=menu.getGuiElement(slot);require(button!=null,"authored skill button");profile.lbcSkill=0;
                button.getGuiCallback().click(slot,eu.pb4.sgui.api.ClickType.MOUSE_LEFT,net.minecraft.world.inventory.ContainerInput.PICKUP,menu);
                require(profile.lbcSkill==id,"native GUI callback selects skill"+id);owner.closeContainer();profile.lbcSkill=0;
            }
            game.config().ranked=true;
        }
        <T extends Entity>T add(T body,Vec3 point){body.setPos(point);require(owner.level().hasChunkAt(body.blockPosition()),"native actor stays resident");require(owner.level().addFreshEntity(body)&&!body.isRemoved(),"native publication "+body.getClass().getSimpleName());actors.add(body);return body;}
        String label(Entity source){String tag=prefix+"."+(serial++);source.entityTags().add(tag);return tag;}
        List<Entity> family(String tag){var found=owner.level().getEntities((Entity)null,new AABB(at,at).inflate(48),e->e.entityTags().contains(tag)&&!e.isRemoved());actors.addAll(found);return List.copyOf(found);}
        List<Entity> copy(Entity source,int skill,String tag){var before=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());before.addAll(family(tag));long operations=service().operationCount();require(service().copyNow(source,skill),"installed clone operation skill"+skill+" "+source.getClass().getSimpleName());require(service().operationCount()==operations+1,"one successful operation receipt");var result=family(tag).stream().filter(e->!before.contains(e)).toList();require(result.size()==(skill==15?1:3),"exact native main-body count");for(var body:result){require(body!=source&&body.getId()!=source.getId()&&!body.getUUID().equals(source.getUUID()),"fresh numeric and UUID identity");require(body.getClass()==source.getClass(),"exact native owner class");}return result;}
        Arrow arrow(Entity author,Vec3 point){var arrow=new Arrow(EntityTypes.ARROW,owner.level());arrow.setOwner(author);arrow.setNoGravity(true);arrow.setDeltaMovement(.031,.017,-.023);return add(arrow,point);}
        void spread(){
            var random=arrow(owner,at.add(0,4,0));var position=new Vec3(-.001,-3.777,-19.999);var motion=new Vec3(-.0021,-.9999,.10001);
            for(int n=0;n<64;n++){
                var spread=LbcCloneSkills.spreadPosition(position,random);
                for(int axis=0;axis<3;axis++){double value=axis==0?spread.x:axis==1?spread.y:spread.z,origin=axis==0?position.x:axis==1?position.y:position.z;double raw=value*100-Math.floor(origin*100);near(raw,Math.rint(raw),"centiblock integer floor lattice");require(raw>=(axis==1?0:-200)-1e-7&&raw<=200+1e-7,"source exact positional bounds including negative coordinates");}
                for(int scale:new int[]{10,1000}){int width=scale==10?10:1000;var value=LbcCloneSkills.spreadMotion(motion,random,scale,width);for(int axis=0;axis<3;axis++){double v=axis==0?value.x:axis==1?value.y:value.z,old=axis==0?motion.x:axis==1?motion.y:motion.z;double raw=v*scale-Math.floor(old*scale);near(raw,Math.rint(raw),"source integer motion floor lattice");require(Math.abs(raw)<=width+1e-7,"source motion spread bounds");}}
            }
        }
        void composition(){
            long beforeBodies=service().bodyCount();var original=arrow(owner,at.add(0,5,0));var tag=label(original);var motion=original.getDeltaMovement();var doubled=copy(original,15,tag).getFirst();near(doubled.getDeltaMovement().distanceTo(motion),0,"Two preserves exact native motion");
            require(original.entityTags().contains("doubled")&&doubled.entityTags().contains("doubled"),"source and published clone carry Two marker");
            require(!service().copyNow(original,15)&&!service().copyNow(doubled,15),"same-skill recursion rejected");
            for(var body:List.of(original,doubled))copy(body,17,tag);
            var all=family(tag);require(service().bodyCount()==beforeBodies+7,"seven new native body receipts for composed eight-body family");require(all.size()==8,"intentional x2*x4 eight native bodies");
            require(all.stream().map(Entity::getUUID).distinct().count()==8&&all.stream().map(Entity::getId).distinct().count()==8,"whole composed family identities unique");
            for(var body:all){require(body.entityTags().containsAll(Set.of("doubled","quadried")),"cross-variant once flags compose");require(!service().copyNow(body,15)&&!service().copyNow(body,17),"both repeat paths closed");}
        }
        void wild(){
            for(var type:List.of(EntityTypes.OAK_CHEST_BOAT,EntityTypes.CHEST_MINECART,EntityTypes.EXPERIENCE_ORB,EntityTypes.AREA_EFFECT_CLOUD)){
                var actor=type.create(owner.level(),EntitySpawnReason.COMMAND);require(actor!=null,"native transport/XP/cloud factory");add(actor,at.add(0,5,0));copy(actor,15,label(actor));
            }
            var board=game.server().getScoreboard();require(board.getPlayerTeam("clone_skill_checks")==null,"fixture team free");team=board.addPlayerTeam("clone_skill_checks");board.addPlayerToTeam(owner.getScoreboardName(),team);board.addPlayerToTeam(other.getScoreboardName(),team);
            var npc=add(new Skeleton(EntityTypes.SKELETON,owner.level()),at.add(-3,3,0));npc.setNoAi(false);
            for(var author:Arrays.asList(owner,other,npc,null)){var shot=arrow(author,at.add(0,6,0));var tag=label(shot);for(var clone:copy(shot,15,tag))require(((Projectile)clone).getOwner()==author,"WILD clone retains actual self/ally/NPC/unowned author");}
            board.removePlayerFromTeam(other.getScoreboardName(),team);var enemy=arrow(other,at.add(0,7,0));copy(enemy,17,label(enemy));
        }
        void mobs(){
            var cow=add(new Cow(EntityTypes.COW,owner.level()),at.add(0,3,0));cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(47);cow.setHealth(19);cow.setNoAi(false);cow.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND,7));cow.setDropChance(EquipmentSlot.MAINHAND,2);
            cow.addEffect(new MobEffectInstance(MobEffects.GLOWING,111,0));require(!service().copyNow(cow,15),"x2 never clones an ordinary mob");var original=nbt(cow);
            var cowCopies=copy(cow,17,label(cow));for(var body:cowCopies){var clone=(Cow)body;near(clone.getHealth(),19,"damaged mob HP preserved");near(clone.getAttributeValue(Attributes.MAX_HEALTH),47,"actual native attribute preserved");require(!clone.isNoAi()&&clone.getNavigation()!=cow.getNavigation(),"fresh native AI/navigation instance");require(ItemStack.matches(clone.getMainHandItem(),cow.getMainHandItem()),"full equipment components/count");require(original.get("drop_chances")!=null&&Objects.equals(nbt(clone).get("drop_chances"),original.get("drop_chances")),"native guaranteed gear loot policy preserved");}
            var victim=(Cow)cowCopies.getFirst();var before=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());before.addAll(owner.level().getEntities((Entity)null,new AABB(at,at).inflate(16),e->true));
            victim.kill(owner.level());var drops=owner.level().getEntities((Entity)null,new AABB(at,at).inflate(16),e->!before.contains(e));actors.addAll(drops);
            int diamonds=drops.stream().filter(net.minecraft.world.entity.item.ItemEntity.class::isInstance).map(net.minecraft.world.entity.item.ItemEntity.class::cast).filter(e->e.getItem().is(Items.DIAMOND)).mapToInt(e->e.getItem().getCount()).sum();
            require(diamonds==7,"actual copied native equipment loot drops exactly once with full count");require(cow.isAlive()&&cow.getHealth()==19,"clone native death does not damage original");
            var skeleton=add(new Skeleton(EntityTypes.SKELETON,owner.level()),at.add(3,3,0));skeleton.setTarget(other);for(var body:copy(skeleton,17,label(skeleton)))require(((Skeleton)body).getTargetUnchecked()==other,"actual vanilla hostile target retained with fresh AI");
            var flying=add(new Ghast(EntityTypes.GHAST,owner.level()),at.add(0,9,0));flying.setNoAi(false);for(var body:copy(flying,17,label(flying)))require(!((Mob)body).isNoAi(),"flying native mob retains actual AI");
        }
        CompoundTag nbt(Entity body){var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,body.registryAccess());body.saveWithoutId(out);return out.buildResult();}
        void relationships(){
            var horse=add(new Horse(EntityTypes.HORSE,owner.level()),at.add(1,2,1));var rider=add(new Cow(EntityTypes.COW,owner.level()),horse.position());require(rider.startRiding(horse,true,false),"real passive horse NPC passenger");horse.setLeashedTo(rider,true);require(owner.startRiding(horse,true,false),"real player passenger fixture");
            for(var body:copy(horse,17,label(horse))){var passengers=body.getPassengers();require(passengers.size()==1&&passengers.getFirst() instanceof Cow,"NPC passenger cloned once while player passenger excluded");var child=(Cow)passengers.getFirst();actors.add(child);require(child!=rider&&child.getVehicle()==body&&((Horse)body).getLeashHolder()==child,"passenger and leash map to same copied horse");}
            require(owner.getVehicle()==horse&&rider.getVehicle()==horse&&horse.getLeashHolder()==rider,"original riding/leash/control links unchanged");owner.stopRiding();
            owner.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.FISHING_ROD));var hook=add(new FishingHook(owner,owner.level(),2,3),at.add(0,5,-2));hook.setDeltaMovement(Vec3.ZERO);require(owner.fishing==hook,"actual native fishing ledger source");
            var clones=copy(hook,15,label(hook));require(owner.fishing==hook,"clone factory preserves original player's fishing ledger");for(var clone:clones){require(((FishingHook)clone).getPlayerOwner()==owner,"fishing causal owner retained");clone.discard();require(owner.fishing==hook,"clone removal does not end original fishing");}
            hook.discard();require(owner.fishing==null,"original hook end clears native ledger normally");owner.fishing=originalFishing;owner.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
        }
        void effects(){
            var cow=add(new Cow(EntityTypes.COW,owner.level()),at.add(-1,3,0));game.items().communism().subjugate(owner,cow,true,true);
            cow.addEffect(new MobEffectInstance(MobEffects.SPEED,800,-2),owner);cow.addEffect(new MobEffectInstance(MobEffects.SPEED,200,1),owner);
            require(game.items().ownedEffects().grant(cow,prefix,new MobEffectInstance(MobEffects.HASTE,300,2),owner),"actual owner lease installed");
            var cause=game.items().indirectKills().wildCause(owner);game.items().indirectKills().wildEffect(cow,new MobEffectInstance(MobEffects.POISON,100,0),cause);
            var shot=game.items().babylon().emit(owner,1,1);require(shot!=null,"real Babylon source arrow");actors.add(shot);
            require(game.items().babylonEffects().accept(cow,shot,ItemEffectLevels.create(MobEffects.RAID_OMEN,17,101)),"actual causal FIFO admission");
            var sourceQueue=map(game.items().babylonEffects(),"pending").get(cow);var sourceLease=map(game.items().ownedEffects(),"targets").get(cow);var sourceEffect=cow.getEffect(MobEffects.POISON);
            var nativeBefore=nbt(cow).get("active_effects");
            for(var body:copy(cow,17,label(cow))){var clone=(Cow)body;require(game.items().communism().owner(clone)==owner,"NPC controller overlay retained");require(Objects.equals(nativeBefore,nbt(clone).get("active_effects")),"signed hidden native effect chain preserved");require(clone.getEffect(MobEffects.POISON)!=sourceEffect,"fresh native effect instance");require(game.items().indirectKills().effectCauses(clone).get(MobEffects.POISON).equals(cause),"fresh effect keeps original causal author");var cloneQueue=map(game.items().babylonEffects(),"pending").get(clone);require(cloneQueue!=sourceQueue&&cloneQueue!=null,"independent target FIFO collection");var sourceHead=((List<?>)sourceQueue).getFirst();var cloneHead=((List<?>)cloneQueue).getFirst();require(sourceHead!=cloneHead,"independent mutable queue per channel");require(((Deque<?>)sourceHead).getFirst()!=((Deque<?>)cloneHead).getFirst(),"independent contribution timer object");require(game.items().babylonEffects().queued(clone,101)==17,"current queued lifetime copied without recast");require(map(game.items().ownedEffects(),"targets").get(clone)!=sourceLease,"independent copied lease map");game.items().ownedEffects().forget(clone);require(map(game.items().ownedEffects(),"targets").get(cow)==sourceLease,"clone lease removal never withdraws original");}
            require(map(game.items().babylonEffects(),"pending").get(cow)==sourceQueue&&game.items().babylonEffects().queued(cow,101)==17,"original FIFO untouched by copies");
        }
        void protection(){
            var protectedMob=add(new Cow(EntityTypes.COW,owner.level()),at.add(-2,3,0));protectedMob.entityTags().add("strict_map_object");require(!service().copyNow(protectedMob,17),"map/shop authority protected");
            var field=game.items().soulless().spawn(owner,at.add(2,2,0),Vec3.ZERO,2);require(field!=null,"real field visual source");actors.add(field.pig);actors.add(field.display);require(!service().copyNow(field.display,17)&&!field.display.isRemoved(),"independent child visual excluded without killing source");
            var trial=new CompoundTag();trial.putInt("upgradable",1);var stack=new ItemStack(Items.CARROT_ON_A_STICK);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(trial));var before=Set.copyOf(game.items().air().snapshot());game.items().air().cast(owner,InteractionHand.MAIN_HAND,0,stack);
            var charge=game.items().air().snapshot().stream().filter(c->!before.contains(c)).findFirst().orElseThrow();actors.add(charge);require(charge.trial&&!service().copyNow(charge,15),"actual academy trial projectile authority excluded");
        }
        Object invoke(Object object,String name,Class<?>[] types,Object... arguments){try{var method=object.getClass().getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(object,arguments);}catch(ReflectiveOperationException error){throw new IllegalStateException("Actual producer "+name,error);}}
        void lateOwners(){
            var before=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());for(var e:owner.level().getAllEntities())before.add(e);
            var previous=other.position();float yaw=other.getYRot(),pitch=other.getXRot();
            try{
                for(int variant=1;variant<=10;variant++){
                    var source=game.items().hedgehogSpikes().emitLegacy(other,variant,at.add(0,5,0),new Vec3(.01,.02,.03));require(source!=null,"actual legacy transport variant "+variant);actors.add(source);
                    for(var clone:copy(source,15,label(source))){
                        require(read(source,"kind")==read(clone,"kind")&&read(source,"legacy").equals(read(clone,"legacy"))&&read(source,"originalAuthor").equals(read(clone,"originalAuthor")),"private spike family, variant and historical author preserved");
                        require(game.items().hedgehogSpikes().projectiles().contains(clone),"clone is owned by actual spike lifecycle");
                        if(variant==10)require(read(source,"visual")!=null&&read(clone,"visual")!=null&&read(source,"visual")!=read(clone,"visual"),"independent grenade presentation per physical body");
                    }
                }
                invoke(game.items().pendingSwords(),"branch",new Class<?>[]{ServerPlayer.class,int.class,boolean.class,boolean.class},other,1,false,false);
                invoke(game.items().shooters(),"helicopterVolley",new Class<?>[]{ServerPlayer.class,double.class,boolean.class},other,.2,false);
                invoke(game.items().vexSword(),"spawn",new Class<?>[]{ServerPlayer.class,Vec3.class,Vec3.class,double.class,int.class,boolean.class,boolean.class},other,at.add(1,5,0),new Vec3(.03,.02,.01),3d,2,true,true);
                invoke(game.items().foodLifecycle(),"chorus",new Class<?>[]{ServerPlayer.class},other);
                invoke(game.items().witchIndolence(),"spawn",new Class<?>[]{ServerPlayer.class,Vec3.class,LbcWitchIndolence.Pattern.class,boolean.class},other,at.add(0,5,1),new LbcWitchIndolence.Pattern(1,0,200,20,true),true);
                var originals=new ArrayList<Entity>();for(var e:owner.level().getAllEntities())if(!before.contains(e)&&CloneLateProjectileStates.recognizes(e)&&!(e instanceof Arrow a&&LbcHedgehogSpikes.managed(a))){originals.add(e);actors.add(e);}
                var covered=new HashSet<String>();
                for(var source:originals){
                    var clones=copy(source,15,label(source));covered.add(source.getClass().getSimpleName());
                    for(var clone:clones){
                        if(LbcFoodLifecycle.cloneRecognizes(source))require(game.items().foodLifecycle().snapshot().contains(clone)&&Objects.equals(game.items().foodLifecycle().cause(source),game.items().foodLifecycle().cause(clone)),"falling pumpkin retains accepted hazard lifecycle and source credit");
                        if(LbcWitchIndolence.cloneRecognizes(source)){
                            var sourceState=map(game.items().witchIndolence(),"flights").get(source);var state=map(game.items().witchIndolence(),"flights").get(clone);
                            require(sourceState!=null&&state!=null&&sourceState!=state&&read(state,"age").equals(read(sourceState,"age"))&&read(state,"lifetime").equals(read(sourceState,"lifetime"))&&read(state,"controlled").equals(read(sourceState,"controlled"))&&read(state,"jitter").equals(read(sourceState,"jitter")),"invisible hand owns independent age/control/lifetime state");
                            require(((LbcSpellSource.Source)read(state,"source")).emitter().equals(clone.getUUID()),"hand authority emitter rebound to its new body");
                        }
                        if(LbcVexSword.cloneRecognizes(source))require(read(source,"groundbreaker").equals(read(clone,"groundbreaker"))&&read(source,"wild").equals(read(clone,"wild")),"Vex sword impact and targeting variants preserved");
                    }
                }
                require(covered.containsAll(Set.of("EpicArrow","HelicopterArrow","VexArrow","Pumpkin","Hand")),"all five later private producers actually emitted source bodies: "+covered);
            }finally{
                for(var e:owner.level().getAllEntities())if(!before.contains(e))actors.add(e);
                require(other.teleportTo(owner.level(),previous.x,previous.y,previous.z,Set.of(),yaw,pitch,true),"restore producer's native spread teleport");audience.acknowledge();
            }
        }
        void customOwners(){
            // Every source below comes from its actual service spawn/transform path.
            var target=add(new Cow(EntityTypes.COW,owner.level()),at.add(0,2,0));target.setXRot(-90);
            var chef=game.items().wtf().spawn(owner,target);require(chef!=null,"real Chef spawn");actors.add(chef);chef.receive(3);require(!service().copyNow(chef,15),"x2 never clones Chef");
            var chefSpeed=Identifier.parse("skywars2:clone_fixture_speed");
            chef.getAttribute(Attributes.ATTACK_SPEED).addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(chefSpeed,1.25,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
            float damage=game.items().wtf().damageCount(chef);
            for(var clone:copy(chef,17,label(chef))){var c=(LbcWtf.Chef)clone;require(c.owner==owner&&c.target==target,"Chef original controller/target");near(c.getAttributeValue(Attributes.ATTACK_SPEED),chef.getAttributeValue(Attributes.ATTACK_SPEED),"Chef custom transient attack attribute retained");near(game.items().wtf().damageCount(c),damage,"Chef accumulated damage state");require(c.brain!=chef.brain&&c.survival!=chef.survival,"fresh Chef controller state");}
            Entity vex=game.items().soulless().spawnVex(owner,at.add(1,4,0));require(vex!=null,"real private GhostVex spawn");actors.add(vex);require(!service().copyNow(vex,15),"x2 never clones a summoned mob");
            for(var clone:copy(vex,17,label(vex)))require(game.items().soulless().owner(clone)==owner,"private Vex factory retains actual caster");
            var priorBosses=Set.copyOf(game.items().grimoireBoss().snapshot());
            var boss=game.items().grimoireBoss().spawn(owner.level(),at.add(-2,2,0),owner);require(boss!=null,"real boss and paired model spawn");
            game.items().grimoireBoss().snapshot().stream().filter(e->!priorBosses.contains(e)).forEach(actors::add);
            int children=(int)game.items().grimoireBoss().snapshot().stream().filter(e->e!=boss&&game.items().spellSources().source(e)!=null&&boss.getUUID().equals(game.items().spellSources().source(e).emitter())).count();
            require(children>0,"real boss owns native presentation children");
            for(var clone:copy(boss,17,label(boss))){int copied=(int)game.items().grimoireBoss().snapshot().stream().filter(e->e!=clone&&game.items().spellSources().source(e)!=null&&clone.getUUID().equals(game.items().spellSources().source(e).emitter())).count();require(copied==children,"one paired model family regenerated per copied boss");}
            game.items().grimoireBoss().snapshot().stream().filter(e->!priorBosses.contains(e)).forEach(actors::add);
            var hole=game.items().sheep().spawnHole(owner,at.add(2,3,0));require(hole!=null,"real gameplay black-hole marker and visual handle");actors.add(hole.entity);
            for(var clone:copy(hole.entity,15,label(hole.entity))){var state=game.items().sheep().holes().stream().filter(h->h.entity==clone).findFirst().orElseThrow();require(state!=hole&&state.visual!=null&&state.visual!=hole.visual,"independent gameplay hole and regenerated handle");require(state.owner.equals(hole.owner)&&state.cause.equals(hole.cause),"hole causal owner retained");near(state.velocity.distanceTo(hole.velocity),0,"current hole velocity preserved");}
            var original=add(new Cow(EntityTypes.COW,owner.level()),at.add(0,3,2));original.getAttribute(Attributes.MAX_HEALTH).setBaseValue(41);original.setHealth(17);original.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.EMERALD,4));original.addEffect(new MobEffectInstance(MobEffects.HASTE,300,1));
            var poisonCause=game.items().indirectKills().wildCause(other);game.items().indirectKills().wildEffect(original,new MobEffectInstance(MobEffects.POISON,301,0),poisonCause);
            var body=game.items().polymorph().transform(original,game.items().indirectKills().coldCause(owner,false));require(body!=null,"real temporary nonplayer polymorph");actors.add(body);require(!service().copyNow(body,15),"x2 never clones a polymorphed mob");
            var originalForm=map(game.items().polymorph(),"bodies").get(body);
            originalPolymorphBody=body;originalPolymorphForm=originalForm;polymorphPoisonCause=poisonCause;
            for(var clone:copy(body,17,label(body))){
                var form=map(game.items().polymorph(),"bodies").get(clone);
                require(form!=null&&form!=originalForm,"independent copied temporary form");
                var hidden=(LivingEntity)read(form,"original");actors.add(hidden);
                require(hidden!=original&&!(hidden instanceof ServerPlayer),"fresh suspended nonplayer body");
                require(read(form,"effects")!=read(originalForm,"effects"),"independent saved restoration effects");
                restoringCopies.add(new PolymorphCopy(clone,hidden,form));
            }
            for(var copy:restoringCopies)copy.sheep.discard();
        }
        void prepareRestoration(){
            // Hazard fixtures were inspected synchronously. Retain only reversible forms for
            // the next real tick; active copied holes/bosses must not attack the two clients.
            var retained=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());
            retained.add(originalPolymorphBody);retained.add((Entity)read(originalPolymorphForm,"original"));
            for(var copy:restoringCopies){retained.add(copy.sheep);retained.add(copy.hidden);}
            for(var actor:List.copyOf(actors))if(!retained.contains(actor)){
                if(!actor.isRemoved())actor.discard();game.server().getScoreboard().resetAllPlayerScores(actor);actors.remove(actor);
            }
            stage=-1;began=game.server().getTickCount();
        }
        void verifyRestoration(){
            for(var copy:restoringCopies){
                var hidden=copy.hidden;
                require(!map(game.items().polymorph(),"bodies").containsKey(copy.sheep),"native form removal finishes copied form on next actual tick");
                require(!map(game.items().polymorph(),"forms").containsKey(hidden),"copied hidden original leaves form owner map");
                require(!hidden.isRemoved()&&!hidden.isInvisible(),"copied original restored on actual removal");
                near(hidden.getHealth(),17,"restored copied native HP");
                require(hidden.getMainHandItem().is(Items.EMERALD)&&hidden.getMainHandItem().getCount()==4&&hidden.hasEffect(MobEffects.HASTE),"saved gear and effects restored on copied original");
                require(polymorphPoisonCause.equals(game.items().indirectKills().effectCauses(hidden).get(MobEffects.POISON)),"restored copied poison keeps original causal author");
            }
            require(map(game.items().polymorph(),"bodies").get(originalPolymorphBody)==originalPolymorphForm,"copy restoration leaves original form active");
            restoringCopies.clear();
        }
        void cleanActors(){for(var body:List.copyOf(actors)){if(!body.isRemoved())body.discard();game.server().getScoreboard().resetAllPlayerScores(body);}actors.clear();}
        void beginFlight(){
            cleanActors();owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),0,-90,true);client.acknowledge();
            var before=Set.copyOf(game.items().air().snapshot());game.items().air().cast(owner,InteractionHand.MAIN_HAND,2,ItemStack.EMPTY);
            air=game.items().air().snapshot().stream().filter(c->!before.contains(c)).findFirst().orElseThrow();actors.add(air);flightStart=air.position();began=game.server().getTickCount();stage=0;
        }
        boolean advance(){
            int elapsed=game.server().getTickCount()-began;if(elapsed<1)return false;require(elapsed<20,"actual tick continuation timeout");client.acknowledge();audience.acknowledge();
            if(stage==-1){verifyRestoration();beginFlight();return false;}
            if(stage==2){
                if(family(radiusInside).size()<2&&elapsed<4)return false;
                require(family(radiusInside).size()==2,"actual registered aura discovers source at exact sphere10");
                require(family(radiusOutside).size()==1,"actual registered aura excludes source beyond sphere10");
                game.players().get(owner.getUUID()).lbcSkill=0;return true;
            }
            if(stage==0){require(!air.isRemoved()&&air.age>0&&air.position().distanceToSqr(flightStart)>1,"real native cast advanced through ordinary tick dispatch");flightAge=air.age;airCopy=(LbcAir.Charge)copy(air,15,label(air)).getFirst();require(airCopy.age==air.age&&airCopy.mode==air.mode&&airCopy.sourceLifetime==air.sourceLifetime&&airCopy.getOwner()==owner,"actual custom flight mode/age/lifetime/cause retained");flightStart=airCopy.position();began=game.server().getTickCount();stage=1;return false;}
            require(!air.isRemoved()&&!airCopy.isRemoved()&&air.age>flightAge&&airCopy.age>flightAge,"source and copied custom actors both continue on actual server tick");require(airCopy.position().distanceToSqr(flightStart)>1,"copied native flight actually moved");require(airCopy.age==air.age,"copied owner flight timers advance together");
            if(stage==1){
                cleanActors();for(var player:game.server().getPlayerList().getPlayers())if(player!=owner)require(!game.lbc().hasSkill(player,15)&&!game.lbc().hasSkill(player,17),"no unrelated clone aura holder in scoped fixture");
                owner.setDeltaMovement(Vec3.ZERO);var inside=arrow(owner,owner.position().add(0,10,0));inside.setDeltaMovement(Vec3.ZERO);radiusInside=label(inside);
                var outside=arrow(owner,owner.position().add(0,10.01,0));outside.setDeltaMovement(Vec3.ZERO);radiusOutside=label(outside);
                game.players().get(owner.getUUID()).lbcSkill=15;require(game.lbc().hasSkill(owner,15),"actual selected native aura admission");began=game.server().getTickCount();stage=2;return false;
            }
            return true;
        }
        @Override public void close(){
            if(closed)return;closed=true;Throwable failure=null;
            for(Runnable action:List.<Runnable>of(()->{owner.stopRiding();cleanActors();owner.fishing=originalFishing;},()->{for(var entry:terrain.entrySet())game.maps().restoreSnapshot(owner.level(),entry.getKey(),entry.getValue());},()->{if(client!=null)client.close();if(audience!=null)audience.close();},()->{var board=game.server().getScoreboard();if(team!=null)board.removePlayerTeam(team);if(ownerTeam!=null)board.addPlayerToTeam(owner.getScoreboardName(),ownerTeam);if(otherTeam!=null)board.addPlayerToTeam(other.getScoreboardName(),otherTeam);},()->{owner.level().getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MOB_DROPS,mobDrops,game.server());game.config().lbcSkills=skills;game.config().ranked=ranked;saved.restore(game);otherSaved.restore(game);saved.restoreAttributes();otherSaved.restoreAttributes();saved.restoreUse();otherSaved.restoreUse();owner.damageCooldownTime=cooldown;other.damageCooldownTime=otherCooldown;for(var entry:serviceState.entrySet())try{entry.getKey().set(service(),entry.getValue());}catch(IllegalAccessException error){throw new IllegalStateException(error);}game.players().save();}))try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("Clone fixture restoration",failure);
        }
    }
}
