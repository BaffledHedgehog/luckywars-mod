package me.skywars2.game;

import java.util.*;
import net.minecraft.commands.CommandSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.*;

/** Native source Supply/Cyborg encounters and their actual reward consumers. */
public final class LbcShishEncounter {
    public static final String ACTOR="skywars2.shish.actor",POINT="skywars2.shish.point";
    private static final Identifier KATANA_SPEED=Identifier.parse("minecraft:a286fd96-0981-4d73-a4b4-cd298373175a");
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final CompoundTag CYBORG=tag("{attributes:[{base:2.0d,id:\"minecraft:attack_damage\"},{base:75.0d,id:\"minecraft:max_health\"}],PersistenceRequired:1b,Tags:[\"cyborg_boss\",\"lucky_mob\",\"mk2\",\"new\"],Health:75.0f,Silent:1b,Rotation:[90.0f,0.0f],\"drop_chances\": {\"mainhand\": 0.0,\"offhand\": 0.0,\"head\": 0.0,\"chest\": 0.0,\"legs\": 0.0,\"feet\": 0.0},CustomName:{\"translate\":\"cyborg_boss\"},DeathLootTable:\"lbc:cyborgboss\"}");
    private static final CompoundTag MAGNUM_STAND=tag("{DisabledSlots:1048623,Tags:[\"supply_drop\",\"rc_1sec\"],Rotation:[45.0f,0.0f],Invisible:1b,NoGravity:1b,equipment:{head:{id:\"minecraft:carrot_on_a_stick\",components:{\"minecraft:custom_name\":{\"italic\":false,\"translate\":\"magnum_mode_standard\"},\"minecraft:lore\":[[{\"translate\":\"type\",\"italic\":false,\"color\":\"aqua\"},{\"translate\":\"shooter\",\"italic\":false,\"color\":\"aqua\"}],{\"translate\":\"magnum_lore_1\",\"italic\":false,\"color\":\"white\"},{\"translate\":\"magnum_lore_2\",\"italic\":false,\"color\":\"white\"},{\"translate\":\"magnum_lore_3\",\"italic\":false,\"color\":\"aqua\"}],\"minecraft:custom_data\":{transformable:1,shooter:1,magnum:1,mode:0,raycastspell:1},\"minecraft:item_model\":\"lbc:magnum\"},count:1}}}"),KATANA_STAND=tag("{DisabledSlots:1048623,Tags:[\"supply_drop\",\"rc_1sec\"],Rotation:[45.0f,0.0f],Invisible:1b,NoGravity:1b,equipment:{head:{id:\"minecraft:carrot_on_a_stick\",components:{\"minecraft:custom_name\":{\"italic\":false,\"translate\":\"techrunner_katana\"},\"minecraft:lore\":[{\"translate\":\"type\",\"italic\":false,\"color\":\"aqua\",\"extra\":[{\"translate\":\"sword\"}]},{\"translate\":\"techrunner_katana_lore_1\",\"italic\":false,\"color\":\"white\"},{\"translate\":\"techrunner_katana_lore_2\",\"italic\":false,\"color\":\"white\"},{\"translate\":\"techrunner_katana_lore_3\",\"italic\":false,\"color\":\"white\"}],\"minecraft:attribute_modifiers\":[{type:\"minecraft:attack_damage\",slot:\"mainhand\",amount:7,operation:\"add_value\",id:\"tecghas1hishdoiaj2w\"},{type:\"minecraft:movement_speed\",slot:\"mainhand\",amount:0.05,operation:\"add_value\",id:\"tecghas1hishdoiaj2w\"},{type:\"minecraft:attack_speed\",slot:\"mainhand\",id:\"minecraft:base_attack_speed\",amount:-2.4,operation:\"add_value\"}],\"minecraft:custom_data\":{techrunner_katana:1,magicwand:1,sword:1,transformable:1,swordwithability:1},\"minecraft:item_model\":\"lbc:techrunner_katana\"},count:1}}}");
    private static final CompoundTag REGULAR_CYBORG=tag("{PersistenceRequired:1b,CustomName:{\"translate\":\"cyborg_boss\"},Silent:1b,Tags:[\"cyborg_boss\",\"lucky_mob\",\"new\"],Health:75.0f,attributes:[{base:75.0d,id:\"minecraft:max_health\"}],active_effects:[{id:\"minecraft:fire_resistance\",duration:-1,show_particles:0b}],drop_chances:{head:0.0f,chest:0.0f,legs:0.0f,feet:0.0f},DeathLootTable:\"lbc:cyborgboss\"}");
    private enum Kind {POINT,SUPPLY,INTERACTION,PEDESTAL,CYBORG,YELLOW,CHILD}
    private static final class State {
        final LbcApocalypseEvents.Authority authority;final Kind kind;int fall;boolean opening;int lastHurt=Integer.MIN_VALUE;
        State(LbcApocalypseEvents.Authority authority,Kind kind){this.authority=authority;this.kind=kind;}
    }
    private static final class NativeCyborg extends Skeleton {
        final LbcApocalypseEvents.Authority accepted;
        NativeCyborg(ServerLevel level,LbcApocalypseEvents.Authority accepted){super(EntityTypes.SKELETON,level);this.accepted=accepted;}
    }
    private static final class YellowBall extends Snowball {
        final LbcApocalypseEvents.Authority accepted;
        YellowBall(ServerLevel level,LbcApocalypseEvents.Authority accepted){super(EntityTypes.SNOWBALL,level);this.accepted=accepted;}
        @Override public boolean shouldBeSaved(){return false;}
    }
    private final GameController game;
    private IdentityHashMap<Entity,State> actors=new IdentityHashMap<>();
    private final Map<Entity,State> causes=new WeakHashMap<>();
    private final Map<UUID,State> unloaded=new HashMap<>();
    private int supplyDue=-1,rainDue=-1,supplyIssued=Integer.MIN_VALUE,rainIssued=Integer.MIN_VALUE;
    private final Set<ServerPlayer> katanaOwners=Collections.newSetFromMap(new IdentityHashMap<>());
    private State executing;private ServerLevel executingLevel;
    private int lastTick=Integer.MIN_VALUE;
    private int checkpointDepth,cadenceOffset;private boolean checkpointFrozen;
    private Runnable stagedPublication;
    public LbcShishEncounter(GameController game){this.game=game;}
    private static CompoundTag tag(String value){try{return TagParser.parseCompoundFully(value);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new ExceptionInInitializerError(e);}}
    private static void load(Entity actor,CompoundTag tag){actor.load(TagValueInput.create(ProblemReporter.DISCARDING,actor.registryAccess(),tag.copy()));}
    private boolean live(LbcApocalypseEvents.Authority authority){return authority!=null&&game.playing()&&game.stage()!=GameController.Stage.ENDING&&game.items().currentEpoch(authority.epoch());}
    private boolean paused(ServerLevel level){return game.items().timeStop().stopped(level)||game.items().wands().timeStopped();}
    private boolean player(ServerPlayer p){return p!=null&&p.isAlive()&&!p.isRemoved()&&game.playing()&&game.stage()!=GameController.Stage.ENDING&&p.level()==game.maps().level()&&LbcDimensionItems.ordinary(p)&&game.canUseGameplayItems(p)&&!game.isObserver(p)&&game.items().wands().validRoleTarget(p,false)&&!paused(p.level())&&!game.items().wands().inputTimeStopped(p);}
    private ServerPlayer author(LbcApocalypseEvents.Authority authority){return game.server().getPlayerList().getPlayer(authority.author());}
    private int read(Entity entity,String name){return game.items().mana().score(entity,name).get();}
    private void write(Entity entity,String name,int value){game.items().mana().score(entity,name).set(value);}
    public static boolean managed(Entity entity){return entity!=null&&entity.entityTags().contains(ACTOR);}
    private State state(Entity entity){var state=actors.get(entity);if(state==null)state=causes.get(entity);if(state==null&&entity instanceof NativeCyborg cyborg)state=new State(cyborg.accepted,Kind.CYBORG);if(state==null&&entity instanceof YellowBall ball)state=new State(ball.accepted,Kind.YELLOW);return state;}
    public LbcIndirectKills.Owner cause(Entity entity){var state=state(entity);return state==null?null:state.authority.cause();}
    public ServerPlayer owner(Entity entity){var state=state(entity);return state==null?null:author(state.authority);}
    public boolean canAffect(Entity entity,Entity target){var state=state(entity);return state!=null&&live(state.authority)&&game.items().wands().validRoleTarget(target,false)&&!LbcGreed.active(target);}
    private <T extends Entity> T admit(T entity,Vec3 at,LbcApocalypseEvents.Authority authority,Kind kind){
        if(checkpointFrozen)return null;checkpointDepth++;try{
        if(entity==null||!live(authority))return null;var level=(ServerLevel)entity.level();entity.snapTo(at.x,at.y,at.z,entity.getYRot(),entity.getXRot());entity.entityTags().add(ACTOR);entity.entityTags().add(authority.epoch());var state=new State(authority,kind);actors.put(entity,state);causes.put(entity,state);
        var source=new LbcSpellSource.Source(entity.getUUID(),authority.author(),authority.epoch(),authority.cause(),Set.of(),true);game.items().spellSources().bind(entity,source);
        if(!level.addFreshEntity(entity)){actors.remove(entity);entity.discard();return null;}game.items().timeStop().prepareFrozen(entity);return entity;

        }finally{checkpointDepth--;}
    }
    private Entity raw(ServerLevel level,EntityType<?> type,CompoundTag payload,Vec3 at,LbcApocalypseEvents.Authority authority,Kind kind){var entity=type.create(level,EntitySpawnReason.COMMAND);if(entity==null)return null;load(entity,payload);return admit(entity,at,authority,kind);}
    private void sound(ServerLevel level,Vec3 at,String id,float volume,float pitch){LbcOverload.sound(level,at,id,volume,pitch,0,0);}
    private void particles(ServerLevel level,ParticleOptions particle,Vec3 at,int count,double x,double y,double z,double speed,boolean force){level.sendParticles(particle,force,false,at.x,at.y,at.z,count,x,y,z,speed);}
    /** Actual40 point source batch, min0/max192 native spreadplayers, shared REPLACE-style selector schedule. */
    public void riskOfRain(LbcApocalypseEvents.Authority authority,ServerLevel level){
        if(checkpointFrozen)return;checkpointDepth++;try{
        if(!live(authority)||paused(level))return;
        for(int n=0;n<40;n++){var point=EntityTypes.MARKER.create(level,EntitySpawnReason.COMMAND);if(point!=null){point.entityTags().addAll(List.of("supply_point",POINT));admit(point,new Vec3(0,128,0),authority,Kind.POINT);}}
        var source=game.server().createCommandSourceStack().withSource(CommandSource.NULL).withLevel(level).withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput();
        try{game.server().getCommands().getDispatcher().execute("spreadplayers 0 0 0 192 false @e[type=minecraft:marker,tag="+POINT+"]",source);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException nativeFailure){/* Source failures keep the actual point actors. */}
        nextRain();

        }finally{checkpointDepth--;}
    }
    private void nextRain(){
        var point=actors.entrySet().stream().filter(e->e.getValue().kind==Kind.POINT&&!e.getKey().isRemoved()).map(Map.Entry::getKey).findFirst().orElse(null);
        if(point==null){for(var entry:List.copyOf(actors.entrySet()))if(entry.getKey().entityTags().contains("supply_drop")&&!entry.getKey().isRemoved())
                drop((ServerLevel)entry.getKey().level(),entry.getKey().position(),new ItemStack(Items.ARROW,16),entry.getValue().authority);rainDue=-1;return;}
        if(paused((ServerLevel)point.level())){rainDue=1;rainIssued=game.server().getTickCount();return;}
        var state=actors.get(point);spawnSupply(state.authority,(ServerLevel)point.level(),point.position());actors.remove(point);point.discard();rainDue=20;rainIssued=game.server().getTickCount();
    }
    public Display.ItemDisplay spawnSupply(LbcApocalypseEvents.Authority authority,ServerLevel level,Vec3 at){return supply(authority,level,at,99,true);}
    private Display.ItemDisplay supply(LbcApocalypseEvents.Authority authority,ServerLevel level,Vec3 at,int initial,boolean immediate){
        if(checkpointFrozen)return null;checkpointDepth++;try{
        var display=EntityTypes.ITEM_DISPLAY.create(level,EntitySpawnReason.COMMAND);if(display==null)return null;
        load(display,tag("{Tags:['supply_drop','new','rc_1sec'],Rotation:[0.0f,0.0f],transformation:{translation:[0.0f,0.5f,0.0f]},item:{id:'minecraft:stick',count:1,components:{'minecraft:item_model':'lbc:supply_beacon'}}}"));
        var center=immediate?Vec3.atCenterOf(BlockPos.containing(at)):at;display=admit(display,center,authority,Kind.SUPPLY);if(display!=null){actors.get(display).fall=initial;write(display,"supplyFall",initial);if(immediate)runSupply(display,actors.get(display));}return display;

        }finally{checkpointDepth--;}
    }
    private void runSupply(Display.ItemDisplay display,State state){
        int count=++state.fall;write(display,"supplyFall",count);var level=(ServerLevel)display.level();var at=display.position();
        if(count<=150)particles(level,new DustParticleOptions(0x4cb2ff,.8f),at.add(0,60,0),30,0,16,0,0,true);
        if(count==100)sound(level,at,"minecraft:lbcsounds.supply_fall",5,1);
        if(count>=146&&count<=149)particles(level,ParticleTypes.CLOUD,at.add(0,(150-count)*30,0),200,.5,20,.5,0,true);
        if(count==150){display.setItemStack(model(Items.REPEATING_COMMAND_BLOCK,"lbc:supply_crate"));particles(level,ParticleTypes.CLOUD,at.add(0,1,0),1000,2,1,2,0,true);game.items().indirectKills().trapChanges(level,state.authority.cause(),()->LbcDropBlocks.fill(level,display.blockPosition(),display.blockPosition().above(),Blocks.BARRIER,true));hurtAround(display,state,3,10);hurtAround(display,state,1,900);}
        if(count==200)open(display,state);
        if(count<=200){supplyDue=1;supplyIssued=game.server().getTickCount();}
    }
    private void hurtAround(Entity emitter,State state,double radius,float amount){var level=(ServerLevel)emitter.level();var damage=game.items().indirectKills().captureDamage(new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.GENERIC),emitter,emitter),state.authority.cause());for(var target:level.getEntities(emitter,new AABB(emitter.position(),emitter.position()).inflate(radius),e->e.getType().builtInRegistryHolder().is(MOBS)&&e.position().distanceToSqr(emitter.position())<=radius*radius&&canAffect(emitter,e)))target.hurtServer(level,damage,amount);}
    private static ItemStack model(Item item,String model){var stack=new ItemStack(item);stack.set(DataComponents.ITEM_MODEL,Identifier.parse(model));return stack;}
    private void open(Display.ItemDisplay display,State state){
        var level=(ServerLevel)display.level();var at=display.position();sound(level,at,"minecraft:block.piston.extend",2,.7f);sound(level,at,"minecraft:entity.generic.extinguish_fire",.8f,.8f);particles(level,ParticleTypes.CLOUD,at.add(0,1,-1),30,.4,.6,.4,0,false);display.setItemStack(model(Items.REPEATING_COMMAND_BLOCK,"lbc:supply_crate_opened"));
        raw(level,EntityTypes.INTERACTION,tag("{width:1.2f,height:2.4f,Tags:['supply_drop','rc_1sec']}"),at.add(0,-.2,0),state.authority,Kind.INTERACTION);
        raw(level,EntityTypes.ARMOR_STAND,MAGNUM_STAND,at.add(.6,0,-.55),state.authority,Kind.PEDESTAL);raw(level,EntityTypes.ARMOR_STAND,KATANA_STAND,at.add(-.6,-.4,-.55),state.authority,Kind.PEDESTAL);display.entityTags().remove("new");state.opening=true;
    }
    private void dispatchSupply(){
        boolean waiting=false;
        for(var entry:List.copyOf(actors.entrySet()))if(entry.getKey() instanceof Display.ItemDisplay display&&entry.getValue().kind==Kind.SUPPLY&&display.entityTags().contains("new")&&!display.isRemoved()){
            if(paused((ServerLevel)display.level()))waiting=true;else runSupply(display,entry.getValue());
        }
        if(waiting&&supplyDue<0){supplyDue=1;supplyIssued=game.server().getTickCount();}
    }
    /** Source CyberShish selector is per participant and may spawn multiple bosses at one nearby crate. */
    public void spawnCyborgs(LbcApocalypseEvents.Authority authority,ServerLevel level,Vec3 playerPosition){for(var entity:List.copyOf(actors.keySet()))if(entity.level()==level&&entity instanceof Display.ItemDisplay&&entity.entityTags().contains("supply_drop")&&!entity.isRemoved()&&entity.position().distanceToSqr(playerPosition)<=400)spawnCyborg(authority,level,entity.position().add(0,1,0));}
    public void cyborgPhase(LbcApocalypseEvents.Authority authority,net.minecraft.commands.CommandSourceStack source){if(!live(authority))return;for(var player:List.copyOf(game.server().getPlayerList().getPlayers()))if(player(player))spawnCyborgs(authority,player.level(),player.position());}
    public Skeleton spawnCyborg(LbcApocalypseEvents.Authority authority,ServerLevel level,Vec3 at){
        if(checkpointFrozen)return null;checkpointDepth++;try{
        if(!live(authority))return null;var mob=new NativeCyborg(level,authority);load(mob,CYBORG);for(var entry:Map.of(EquipmentSlot.HEAD,"helmet",EquipmentSlot.CHEST,"chestplate",EquipmentSlot.LEGS,"leggings",EquipmentSlot.FEET,"boots").entrySet()){var outputs=game.lbc().rollTable(level,at,mob,"lbc:neko_set_technomk2/"+entry.getValue());if(!outputs.isEmpty())mob.setItemSlot(entry.getKey(),outputs.getFirst());}mob.entityTags().remove("new");write(mob,"cyborgBossFly",50);return admit(mob,at,authority,Kind.CYBORG);

        }finally{checkpointDepth--;}
    }
    /** Actual source HurtTime10 observer; callers can invoke after the native accepted hurt boundary too. */
    public void incoming(LivingEntity entity){
        if(checkpointFrozen)return;checkpointDepth++;try{var state=actors.get(entity);if(state==null||state.kind!=Kind.CYBORG||entity.hurtTime!=10||!live(state.authority)||paused((ServerLevel)entity.level()))return;int now=game.server().getTickCount();if(state.lastHurt==now)return;state.lastHurt=now;var source=new LbcSpellSource.Source(entity.getUUID(),state.authority.author(),state.authority.epoch(),state.authority.cause(),Set.of("rcdenyalways","sauvojen_tuntija","transformator","cyborg_boss","summoned_cultist"),true);game.items().wands().cyborgElectro(entity,source);sound((ServerLevel)entity.level(),entity.position(),"minecraft:entity.iron_golem.hurt",2,1.5f);
        }finally{checkpointDepth--;}
    }
    private void cyborg(Skeleton entity,State state,int now){
        var level=(ServerLevel)entity.level();if(game.items().magic().mechanismSuppressedAt(level,entity.position())){entity.discard();return;}incoming(entity);
        var fly=game.server().getScoreboard().getObjective("cyborgBossFly");var info=fly==null?null:game.server().getScoreboard().getPlayerScoreInfo(entity,fly);
        if(info!=null&&info.value()==0&&level.players().stream().anyMatch(p->player(p)&&p.position().distanceToSqr(entity.position())<=9))wyvern(entity,state);
        if(info!=null&&info.value()>0)write(entity,"cyborgBossFly",info.value()-1);
        if(entity.entityTags().contains("mk2")){particles(level,new DustParticleOptions(0xffa32d,1),entity.position().add(0,.9,0),1,.3,.4,.3,0,false);entity.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,20,4,false,false),entity);}
        if(Math.floorMod(now+cadenceOffset,5)==0&&level.getRandom().nextFloat()<.05f){entity.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,40,4,false,false),entity);particles(level,ParticleTypes.ELECTRIC_SPARK,entity.position().add(0,1,0),50,.5,1,.5,.1,false);sound(level,entity.position(),"minecraft:block.beacon.activate",1,1);}
    }
    private void wyvern(Skeleton entity,State state){
        var level=(ServerLevel)entity.level();entity.addEffect(ItemEffectLevels.create(MobEffects.LEVITATION,20,30,false,false),entity);entity.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,120,4,false,false),entity);
        var damage=game.items().indirectKills().captureDamage(new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.CACTUS),entity,entity),state.authority.cause());
        for(var target:level.getEntities(entity,entity.getBoundingBox().inflate(10),e->e.position().distanceToSqr(entity.position())<=100&&canAffect(entity,e)&&!e.entityTags().contains("jump")&&!sameTeam(entity,e))){if(target.getType().builtInRegistryHolder().is(MOBS))target.hurtServer(level,damage,8);if(target instanceof LivingEntity living&&target.position().distanceToSqr(entity.position())>=.0001)living.addEffect(ItemEffectLevels.create(MobEffects.BLINDNESS,100,0,false,false),entity);}
        sound(level,entity.position(),"minecraft:entity.ender_dragon.growl",4,1);particles(level,ParticleTypes.CLOUD,entity.position().add(0,5,0),1000,1,5,1,.2,true);write(entity,"cyborgBossFly",200);
    }
    private boolean sameTeam(Entity a,Entity b){var objective=game.server().getScoreboard().getObjective("team_number");if(objective==null)return false;var x=game.server().getScoreboard().getPlayerScoreInfo(a,objective);var y=game.server().getScoreboard().getPlayerScoreInfo(b,objective);return x!=null&&y!=null&&x.value()==y.value();}
    private boolean sameId(Entity a,Entity b){var objective=game.server().getScoreboard().getObjective("lbcID2");if(objective==null)return false;var x=game.server().getScoreboard().getPlayerScoreInfo(a,objective);var y=game.server().getScoreboard().getPlayerScoreInfo(b,objective);return x!=null&&y!=null&&x.value()==y.value();}
    public boolean useBeacon(ServerPlayer player,InteractionHand hand,Vec3 at){var stack=player.getItemInHand(hand);if(LbcFartus.data(stack).getIntOr("supply_beacon",0)!=1||!player(player))return false;var accepted=new LbcApocalypseEvents.Authority(player.getUUID(),game.items().indirectKills().wildCause(player),game.items().epoch());if(supply(accepted,player.level(),at,0,false)==null)return false;stack.consume(1,player);return true;}
    /** Native Interaction front door; source clears inventory arrows in pairs, capped32 iterations. */
    public boolean interact(ServerPlayer player,Entity entity,InteractionHand hand){
        if(checkpointFrozen)return false;checkpointDepth++;try{if(!player(player)||actors.get(entity)==null||actors.get(entity).kind!=Kind.INTERACTION||!player.getItemInHand(hand).is(Items.ARROW)||player.getItemInHand(hand).getCount()<2)return false;boolean infinite=false;for(int n=0;n<player.getInventory().getContainerSize();n++)if(LbcFartus.data(player.getInventory().getItem(n)).getIntOr("infinite_arrow",0)==1)infinite=true;
        for(int n=0;n<32;n++){if(!infinite)player.getInventory().clearOrCountMatchingItems(s->s.is(Items.ARROW),false,2,player.inventoryMenu.getCraftSlots());ServerPlayer closest=null;double distance=Double.POSITIVE_INFINITY;for(var p:player.level().players())if(!p.isSpectator()){double next=p.distanceToSqr(player);if(next<distance){distance=next;closest=p;}}if(closest!=null)for(var stack:game.lbc().rollTable(player.level(),closest.position(),player,"lbc:magnum_ammo"))drop(player.level(),closest.position(),stack,state(entity).authority);sound(player.level(),player.position(),"minecraft:block.piston.extend",1,1.5f);int arrows=0;for(int slot=0;slot<36;slot++){var stack=player.getInventory().getItem(slot);if(stack.is(Items.ARROW)&&stack.getCount()>=2){arrows=2;break;}}if(arrows<2)break;}return true;
        }finally{checkpointDepth--;}
    }
    /** Original armor-stand criterion is independent of supply ownership and scans all full ordinary Techno stands. */
    public boolean technoInteraction(ServerPlayer player,Entity clicked,ItemStack used){
        if(checkpointFrozen)return false;checkpointDepth++;try{
        if(!player(player)||clicked.getType()!=EntityTypes.ARMOR_STAND||LbcFartus.data(used).getIntOr("techno",0)!=1)return false;
        var authority=new LbcApocalypseEvents.Authority(player.getUUID(),game.items().indirectKills().wildCause(player),game.items().epoch());
        var stands=new ArrayList<net.minecraft.world.entity.decoration.ArmorStand>();player.level().getAllEntities().forEach(entity->{if(entity instanceof net.minecraft.world.entity.decoration.ArmorStand stand)stands.add(stand);});for(var entity:stands){
            boolean full=true;for(var entry:Map.of(EquipmentSlot.HEAD,Items.LEATHER_HELMET,EquipmentSlot.CHEST,Items.LEATHER_CHESTPLATE,EquipmentSlot.LEGS,Items.LEATHER_LEGGINGS,EquipmentSlot.FEET,Items.LEATHER_BOOTS).entrySet()){var item=entity.getItemBySlot(entry.getKey());if(!item.is(entry.getValue())||item.getCount()!=1||LbcFartus.data(item).getIntOr("techno",0)!=1){full=false;break;}}if(!full)continue;
            var boss=new NativeCyborg(player.level(),authority);load(boss,REGULAR_CYBORG);for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND))boss.setItemSlot(slot,entity.getItemBySlot(slot).copy());boss.setYRot(entity.getYRot());boss.setXRot(entity.getXRot());
            var counter=game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("*id.cloud"),"lbcID2");counter.add(1);write(boss,"lbcID2",counter.get());write(boss,"cyborgBossFly",50);boss.entityTags().remove("new");if(admit(boss,entity.position(),authority,Kind.CYBORG)!=null){game.items().entityUnloaded(entity);entity.discard();}
        }return true;

        }finally{checkpointDepth--;}
    }
    private void drop(ServerLevel level,Vec3 at,ItemStack stack,LbcApocalypseEvents.Authority authority){var drop=new ItemEntity(level,at.x,at.y,at.z,stack.copy());var owner=author(authority);if(owner!=null)drop.setThrower(owner);admit(drop,at,authority,Kind.CHILD);}
    public static boolean matches(ItemStack stack){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("techrunner_katana",0)==1;}
    private static void movementAmount(ItemStack stack,double amount){if(stack.isEmpty())return;var old=stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY);var changed=new ArrayList<ItemAttributeModifiers.Entry>();for(var entry:old.modifiers()){var mod=entry.modifier();changed.add(entry.attribute().equals(Attributes.MOVEMENT_SPEED)?new ItemAttributeModifiers.Entry(entry.attribute(),new AttributeModifier(mod.id(),amount,mod.operation()),entry.slot(),entry.display()):entry);}if(!changed.equals(old.modifiers()))stack.set(DataComponents.ATTRIBUTE_MODIFIERS,new ItemAttributeModifiers(changed));}
    public void damaged(LivingEntity victim,DamageSource source){
        if(checkpointFrozen)return;checkpointDepth++;try{if(!(source.getEntity() instanceof ServerPlayer player)||!LbcDamage.isMeleeAttack(source,player)||!player(player)||!matches(player.getMainHandItem())||game.items().magic().mechanismSuppressed(player))return;write(player,"swordcharging6",read(player,"swordcharging6")+1);movementAmount(player.getMainHandItem(),.05);if(read(player,"swordcharging6")==3){write(player,"swordcharging6",0);write(player,"swordcharged6",read(player,"swordcharged6")+5);}
        }finally{checkpointDepth--;}
    }
    public boolean use(ServerPlayer player,InteractionHand hand){
        if(checkpointFrozen)return false;checkpointDepth++;try{if(!player(player)||!matches(player.getItemInHand(hand))||read(player,"swordcharged6")<1||game.items().magic().mechanismSuppressed(player)||hand==InteractionHand.OFF_HAND&&!game.config().offhandWands)return false;var level=player.level();var at=player.position();particles(level,ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS,at.add(0,.8,0),30,.4,.5,.4,0,false);var target=at.add(player.getLookAngle().scale(5));teleport(player,target);game.items().indirectKills().teleported(player,player,true);particles(level,ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS,player.position().add(0,.8,0),30,.4,.5,.4,0,false);sound(level,player.position(),"minecraft:item.trident.riptide_1",2,0);
        for(int n=0;n<3;n++)if(!level.getBlockState(player.blockPosition()).isAir()){teleport(player,player.position().add(0,1,0));game.items().indirectKills().teleported(player,player,true);}
        var main=player.getMainHandItem();var attrs=main.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY);int amount=0;for(var entry:attrs.modifiers())if(entry.attribute().equals(Attributes.MOVEMENT_SPEED)){amount=Mth.floor(entry.modifier().amount()*100);break;}movementAmount(main,(amount+2)*.01);write(player,"swordcharged6",read(player,"swordcharged6")-1);return true;

        }finally{checkpointDepth--;}
    }
    private static void teleport(ServerPlayer player,Vec3 target){var motion=player.getDeltaMovement();try{player.teleportTo(player.level(),target.x,target.y,target.z,TeleportMomentum.positionFlags(Set.of()),player.getYRot(),player.getXRot(),true);}finally{player.setDeltaMovement(motion);}}
    /** Canonical original movement scoreboard, shared with Witch statistics; no second private position ledger. */
    public void sampleMovement(ServerPlayer player){
        int x=Mth.floor(player.getX()*100),y=Mth.floor(player.getY()*100),z=Mth.floor(player.getZ()*100);int dx=x-read(player,"x"),dy=y-read(player,"y"),dz=z-read(player,"z");write(player,"speedy",dy);int squared=dx*dx+dy*dy+dz*dz,speed=Math.floorDiv(squared,2);
        for(int n=0;n<17;n++){int value=squared;if(speed!=0)value=Math.floorDiv(value,speed);value+=speed;speed=Math.floorDiv(value,2);}write(player,"speed",speed);if(read(player,"sleep_time")>0)write(player,"sleep_time",read(player,"sleep_time")-15);if(speed==0)write(player,"sleep_time",read(player,"sleep_time")+16);
        if(!player.entityTags().contains("time_stopped_lol")){write(player,"x",x);write(player,"y",y);write(player,"z",z);}
    }
    private void katana(ServerPlayer player){var attr=player.getAttribute(Attributes.ATTACK_DAMAGE);if(matches(player.getMainHandItem())){int speed=Math.floorDiv(read(player,"speed"),18);int amount=Math.floorDiv(speed*speed+1,2);var old=attr.getModifier(KATANA_SPEED);if(old==null||old.amount()!=amount){attr.removeModifier(KATANA_SPEED);attr.addPermanentModifier(new AttributeModifier(KATANA_SPEED,amount,AttributeModifier.Operation.ADD_VALUE));}katanaOwners.add(player);}else{attr.removeModifier(KATANA_SPEED);katanaOwners.remove(player);}}
    public net.minecraft.network.chat.Component hud(ServerPlayer player){if(!matches(player.getMainHandItem())&&!matches(player.getOffhandItem()))return net.minecraft.network.chat.Component.empty();return net.minecraft.network.chat.Component.literal("    \uE103 \u00a7fx").append(net.minecraft.network.chat.Component.literal(Integer.toString(read(player,"swordcharged6"))).withColor(0xffffff)).append(net.minecraft.network.chat.Component.literal(" [")).append(net.minecraft.network.chat.Component.literal(Integer.toString(read(player,"swordcharging6"))).withColor(0x41bbe0)).append(net.minecraft.network.chat.Component.literal("/3\u00a7f]").withColor(0x41bbe0));}
    private static boolean yellow(ItemStack stack){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("yellow_lightning",0)==1;}
    public void inventoryChanged(ServerPlayer player){boolean hotbar=false,inventory=false;for(int slot=0;slot<36;slot++){boolean found=yellow(player.getInventory().getItem(slot));inventory|=found;if(slot<9)hotbar|=found;}boolean held=yellow(player.getMainHandItem())||yellow(player.getOffhandItem());if(hotbar||held)player.entityTags().addAll(List.of("yellow_lightning","yellow_lightning_active"));if(!hotbar&&!held)player.entityTags().remove("yellow_lightning");if(!inventory&&!held)player.entityTags().remove("yellow_lightning_active");}
    private void yellow(ServerPlayer player,int now){if(player.entityTags().contains("yellow_lightning")&&(yellow(player.getMainHandItem())||yellow(player.getOffhandItem()))){var source=game.server().createCommandSourceStack().withSource(CommandSource.NULL).withLevel(player.level()).withEntity(player).withPosition(player.position()).withRotation(player.getRotationVector()).withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput();for(var command:List.of("particle minecraft:item{item:{id:'minecraft:yellow_stained_glass'}} ~ ~.9 ~ ^100 ^ ^70000 0.00001 0 force","particle minecraft:item{item:{id:'minecraft:yellow_stained_glass'}} ~ ~.85 ~ ^-100 ^ ^60000 0.00001 0 force"))try{game.server().getCommands().getDispatcher().execute(command,source);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException nativeParticleFailure){/* Native source failure does not suppress the independent snowball branch. */}}
        if(Math.floorMod(now+cadenceOffset,5)==0&&player.entityTags().contains("yellow_lightning_active")){var authority=new LbcApocalypseEvents.Authority(player.getUUID(),game.items().indirectKills().wildCause(player),game.items().epoch());var ball=new YellowBall(player.level(),authority);ball.setItem(model(Items.STICK,"lbc:empty"));ball.entityTags().add("piss");ball.setDeltaMovement(player.getLookAngle().scale(.5));write(ball,"lbcID2",read(player,"lbcID2"));write(ball,"team_number",read(player,"team_number"));admit(ball,player.position().add(0,1.75,0),authority,Kind.YELLOW);}
    }
    private void yellowBall(Entity entity,State state){var level=(ServerLevel)entity.level();var center=entity.position().add(0,-1,0);for(var target:level.getEntities(entity,new AABB(center,center).inflate(1.5),e->e instanceof LivingEntity&&e.getType().builtInRegistryHolder().is(MOBS)&&e.position().distanceToSqr(center)<=2.25&&canAffect(entity,e)&&!sameId(entity,e)&&!sameTeam(entity,e)))game.items().psycho().yellowPersistent((LivingEntity)target,author(state.authority),60);}
    /** Must wrap the actual native actor tick so native arrows/rewards receive accepted immutable provenance. */
    public void withTick(Entity actor,Runnable nativeTick){
        if(checkpointFrozen)return;checkpointDepth++;try{var state=state(actor);if(state==null){nativeTick.run();return;}if(!live(state.authority)){actor.discard();return;}if(paused((ServerLevel)actor.level())||actor.entityTags().contains("time_stopped"))return;var previous=executing;var previousLevel=executingLevel;executing=state;executingLevel=(ServerLevel)actor.level();try(var scope=game.items().spellSources().scope(actor)){game.items().indirectKills().trapChanges(executingLevel,state.authority.cause(),nativeTick);}finally{executing=previous;executingLevel=previousLevel;}
        }finally{checkpointDepth--;}
    }
    public void inheritChild(Entity child){if(checkpointFrozen)return;if(executing==null||child.level()!=executingLevel||!live(executing.authority)||!(child instanceof Projectile||child instanceof ItemEntity))return;var state=new State(executing.authority,Kind.CHILD);child.entityTags().add(ACTOR);child.entityTags().add(state.authority.epoch());actors.put(child,state);causes.put(child,state);var source=game.items().spellSources().current();if(source==null)source=new LbcSpellSource.Source(child.getUUID(),state.authority.author(),state.authority.epoch(),state.authority.cause(),Set.of(),true);game.items().spellSources().bind(child,source);if(child instanceof ItemEntity item){var author=author(state.authority);if(author!=null)item.setThrower(author);}game.items().timeStop().prepareFrozen(child);}
    public void inheritConversion(Entity parent,Entity child){if(checkpointFrozen)return;var state=state(parent);if(state==null||!live(state.authority)||child==null)return;child.entityTags().add(ACTOR);child.entityTags().add(state.authority.epoch());var inherited=new State(state.authority,Kind.CHILD);actors.put(child,inherited);causes.put(child,inherited);var source=game.items().spellSources().source(parent);if(source==null)source=new LbcSpellSource.Source(parent.getUUID(),state.authority.author(),state.authority.epoch(),state.authority.cause(),Set.of(),true);game.items().spellSources().bind(child,source);game.items().timeStop().prepareFrozen(child);}
    public void loaded(Entity entity){
        checkpointDepth++;try{if(checkpointFrozen||MatchActorCheckpoint.restoring(entity))return;if(!managed(entity)||actors.containsKey(entity))return;var state=unloaded.remove(entity.getUUID());if(state==null||!live(state.authority)||!game.items().currentEpoch(entity)){entity.discard();return;}actors.put(entity,state);causes.put(entity,state);if(game.items().spellSources().source(entity)==null)game.items().spellSources().bind(entity,new LbcSpellSource.Source(entity.getUUID(),state.authority.author(),state.authority.epoch(),state.authority.cause(),Set.of(),true));game.items().timeStop().prepareFrozen(entity);
        }finally{checkpointDepth--;}
    }
    public void unloaded(Entity entity){
        checkpointDepth++;try{if(checkpointFrozen||MatchActorCheckpoint.restoring(entity))return;var state=actors.remove(entity);if(state!=null&&entity.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK)unloaded.put(entity.getUUID(),state);
        }finally{checkpointDepth--;}
    }
    public void tick(){
        if(checkpointFrozen)return;checkpointDepth++;try{int now=game.server().getTickCount();if(now==lastTick||!game.server().tickRateManager().runsNormally())return;lastTick=now;if(!game.playing()||game.stage()==GameController.Stage.ENDING){stop();return;}
        for(var entry:List.copyOf(actors.entrySet())){var actor=entry.getKey();var state=entry.getValue();if(actor.isRemoved()||!live(state.authority)){actors.remove(actor);if(!actor.isRemoved())actor.discard();continue;}if(paused((ServerLevel)actor.level())||actor.entityTags().contains("time_stopped"))continue;if(actor instanceof Skeleton skeleton&&state.kind==Kind.CYBORG)cyborg(skeleton,state,now);if(state.kind==Kind.YELLOW)yellowBall(actor,state);}
        if(!game.items().wands().timeStopped()){
            if(rainDue>=0&&rainIssued!=now&&--rainDue<=0){rainDue=-1;nextRain();}
            if(supplyDue>=0&&supplyIssued!=now&&--supplyDue<=0){supplyDue=-1;dispatchSupply();}
            if(Math.floorMod(now+cadenceOffset,20)==0)for(var entry:List.copyOf(actors.entrySet()))if(entry.getValue().kind==Kind.SUPPLY&&entry.getKey().entityTags().contains("new")&&!entry.getKey().isRemoved()&&!paused((ServerLevel)entry.getKey().level()))dispatchSupply();
        }
        for(var player:List.copyOf(game.server().getPlayerList().getPlayers()))if(player(player)){sampleMovement(player);katana(player);yellow(player,now);}else if(!paused(player.level())&&!game.items().wands().inputTimeStopped(player)&&katanaOwners.remove(player))player.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(KATANA_SPEED);

        }finally{checkpointDepth--;}
    }
    /** Already accepted encounters and children continue after author death/disconnect. */
    public void forget(ServerPlayer player){player.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(KATANA_SPEED);katanaOwners.remove(player);}
    public void stop(){
        checkpointDepth++;try{checkpointFrozen=false;stagedPublication=null;cadenceOffset=0;for(var player:List.copyOf(katanaOwners))forget(player);for(var actor:List.copyOf(actors.keySet()))actor.discard();actors.clear();causes.clear();unloaded.clear();supplyDue=rainDue=-1;supplyIssued=rainIssued=Integer.MIN_VALUE;executing=null;executingLevel=null;
        }finally{checkpointDepth--;}
    }
    public int actorCount(){return actors.size();}public int supplyCount(){return(int)actors.values().stream().filter(s->s.kind==Kind.SUPPLY).count();}public int cyborgCount(){return(int)actors.values().stream().filter(s->s.kind==Kind.CYBORG).count();}public List<Entity> snapshot(){return List.copyOf(actors.keySet());}

    boolean captureReady(){return checkpointDepth==0&&executing==null&&!checkpointFrozen;}
    void checkpointBoundary(){if(checkpointDepth!=0||executing!=null)throw new IllegalStateException("Finish Shish native callbacks before checkpointing");}
    void prepareFrozen(){
        checkpointBoundary();checkpointFrozen=true;stagedPublication=null;
        actors.clear();causes.clear();unloaded.clear();katanaOwners.clear();supplyDue=rainDue=-1;
    }
    void publishFrozen(){checkpointBoundary();if(stagedPublication!=null)stagedPublication.run();stagedPublication=null;checkpointFrozen=false;}
    void cancelFrozen(){checkpointBoundary();stagedPublication=null;checkpointFrozen=false;}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){checkpointBoundary();if(!captureReady())throw new IllegalStateException("Capture Shish outside native construction");return new Checkpoint(links);}
    final class Checkpoint {
        private record Value(State original,int fall,boolean opening,int lastHurt){}
        private record Cause(MatchDamageSources.Ref actor,State state){}
        private final MatchSpellCheckpoint.IdentityImage<Entity,State> registry;
        private final Map<UUID,State> dormant=new HashMap<>(unloaded);
        private final List<Value> values=new ArrayList<>();private final List<Cause> provenance=new ArrayList<>();
        private final List<MatchDamageSources.Ref> wielders=new ArrayList<>();
        private final int supply=supplyDue,rain=rainDue,supplyFrame=supplyIssued,rainFrame=rainIssued;
        private final MatchAuthorityCheckpoint.Clock clock=new MatchAuthorityCheckpoint.Clock(game,lastTick,0);
        private final int phase=Math.floorMod(game.server().getTickCount()+cadenceOffset,20);
        Checkpoint(MatchSpellCheckpoint.Links links){
            for(var actor:actors.keySet())if(links.key(actor).historical()!=null)throw new IllegalStateException("Capture current Shish actors");
            registry=new MatchSpellCheckpoint.IdentityImage<>(actors,links);
            var seen=Collections.newSetFromMap(new IdentityHashMap<State,Boolean>());
            for(var value:actors.values())if(seen.add(value))values.add(new Value(value,value.fall,value.opening,value.lastHurt));
            for(var value:unloaded.values())if(seen.add(value))values.add(new Value(value,value.fall,value.opening,value.lastHurt));
            causes.forEach((actor,value)->{provenance.add(new Cause(links.key(actor),value));if(seen.add(value))values.add(new Value(value,value.fall,value.opening,value.lastHurt));});
            for(var owner:katanaOwners)wielders.add(links.key(owner));
        }
        Runnable prepare(MatchSpellCheckpoint.Links links){
            checkpointBoundary();var nextActors=registry.restore(links);var nextCauses=new ArrayList<Map.Entry<Entity,State>>();
            for(var saved:provenance)nextCauses.add(Map.entry(links.get(saved.actor),saved.state));
            var nextWielders=new ArrayList<ServerPlayer>();for(var owner:wielders)nextWielders.add((ServerPlayer)links.get(owner));
            return ()->{
                for(var saved:values){saved.original.fall=saved.fall;saved.original.opening=saved.opening;}
                actors=nextActors;unloaded.clear();unloaded.putAll(dormant);causes.clear();for(var saved:nextCauses)causes.put(saved.getKey(),saved.getValue());
                katanaOwners.clear();katanaOwners.addAll(nextWielders);supplyDue=supply;rainDue=rain;
                stagedPublication=()->{lastTick=clock.last(game);supplyIssued=supplyFrame==Integer.MIN_VALUE?supplyFrame:clock.time(supplyFrame,game);rainIssued=rainFrame==Integer.MIN_VALUE?rainFrame:clock.time(rainFrame,game);
                    for(var saved:values)saved.original.lastHurt=saved.lastHurt==Integer.MIN_VALUE?saved.lastHurt:clock.time(saved.lastHurt,game);cadenceOffset=phase-game.server().getTickCount();};
            };
        }
    }
}
