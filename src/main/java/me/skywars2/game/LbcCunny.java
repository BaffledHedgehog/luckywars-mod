package me.skywars2.game;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

/** Native owner of the22 finite authored outcomes; terrain and match reset retain their canonical owners. */
final class LbcCunny {
    enum Suppression { SOURCE, MAGIC }
    enum Part { ACTIVATION, PAIR, NAHIDA }
    record Pair(UUID seeker,UUID target,LbcIndirectKills.Owner cause){}
    record Emission(int remaining,boolean visible,UUID owner){}
    record State(List<Pair> pairs,Map<UUID,Integer> nahida,Map<UUID,Emission> emissions,int phase,
                 MatchRandomState random,long accepted,long kills,long summons){
        State{pairs=List.copyOf(pairs);nahida=Map.copyOf(nahida);emissions=Map.copyOf(emissions);}}
    private static final Identifier BUFF=Identifier.parse("minecraft:cunny2buffs"),PLAP=Identifier.parse("minecraft:plapper_debuff");
    private static final String[] OUTCOME_KEYS={"cunny_slab_0","cunny_slab_1","cunny_slab_2","cunny_slab_3","cunny_slab_4","cunny_slab_5","cunny_slab_6","cunny_slab_7","cunny_slab_8","cunny_slab_9","cunny_slab_10","cunny_slab_11","cunny_slab_12","cunny_slab_13","cunny_slab_14","cunny_slab_15","cunny_slab_16","cunny_slab_17","cunny_slab_18","cunny_slab_19","cunny_slab_20","cunny_slab_21"};
    private static final String[] SOUNDS={"minecraft:lbcsounds.icum","minecraft:lbcsounds.yamai_desu","minecraft:lbcsounds.yamai_desu_2","minecraft:lbcsounds.yamai_desu_3","minecraft:lbcsounds.ooh","minecraft:lbcsounds.nyah"};
    private final GameController game;private final RandomSource random=RandomSource.create();
    private final List<Pair> pairs=new ArrayList<>();private final Map<UUID,Integer> nahida=new HashMap<>();
    private final Map<UUID,Emission> emissions=new HashMap<>();private final EnumMap<Part,Suppression> policies=new EnumMap<>(Part.class);
    private int phase;private long accepted,kills,summons;private boolean restoring,advancing;
    LbcCunny(GameController game){this.game=game;for(var part:Part.values())policies.put(part,Suppression.SOURCE);}
    void policy(Part part,Suppression policy){policies.put(part,Objects.requireNonNull(policy));}
    private boolean suppressed(ServerPlayer p,Part part){return policies.get(part)==Suppression.MAGIC&&
            (game.items().magic().suppressed(p)||game.items().magic().suppressedAt(p.level(),p.getEyePosition()));}
    private boolean active(ServerPlayer p){return !restoring&&game.playing()&&game.stage()!=GameController.Stage.ENDING
            &&p.isAlive()&&!p.isRemoved()&&LbcDimensionItems.ordinary(p)&&game.canUseGameplayItems(p)
            &&!game.isObserver(p)&&game.items().wands().validRoleTarget(p,false);}
    private boolean paused(ServerPlayer p){return !game.server().tickRateManager().runsNormally()||game.server().tickRateManager().isFrozen()
            ||game.items().timeStop().stopped(p.level())||game.items().timeStop().frozen(p)||game.items().wands().inputTimeStopped(p);}
    private boolean hostile(ServerPlayer owner,Entity target){return target!=owner&&game.items().wands().target(owner,target);}
    boolean use(ServerPlayer p,InteractionHand hand){var used=p.getItemInHand(hand);int outcome=LbcCunnyItems.variant(used);
        if(outcome<0||outcome>21||!active(p)||paused(p)||game.items().actionsBlocked(p)
                ||hand==InteractionHand.OFF_HAND&&!game.config().offhandWands||suppressed(p,Part.ACTIVATION))return false;
        if((outcome==4||outcome==21)&&!game.items().summons().ready())return false;
        // Debit the accepted actual stack. Outcome0 clears inventories itself, so never debit the new match's hand.
        if(outcome==0){if(!game.restartForCunny(p))return false;used.shrink(1);p.getInventory().setChanged();p.containerMenu.broadcastChanges();accepted++;announce(p,0);return true;}
        boolean result=switch(outcome){
            case 1->{for(var target:p.level().players())if(hostile(p,target)&&p.distanceToSqr(target)>=.0001
                        &&!game.items().inventoryHazards().protectedInventory(target)){
                    for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})target.setItemSlot(slot,ItemStack.EMPTY);
                    target.getInventory().setChanged();target.containerMenu.broadcastChanges();
                    p.level().sendParticles(ParticleTypes.WITCH,target.getX(),target.getY()+1,target.getZ(),100,.3,.7,.3,0);}yield true;}
            case 2->{buff(p,Attributes.ARMOR,6,AttributeModifier.Operation.ADD_VALUE);buff(p,Attributes.ARMOR_TOUGHNESS,3,AttributeModifier.Operation.ADD_VALUE);
                buff(p,Attributes.ATTACK_DAMAGE,6,AttributeModifier.Operation.ADD_VALUE);buff(p,Attributes.SCALE,-.5,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
                buff(p,Attributes.MAX_HEALTH,.5,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);buff(p,Attributes.MOVEMENT_SPEED,.2,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
                for(var id:List.of("lbc:inf_shulker","lbc:inf_steak","lbc:evolution_sword"))give(p,id);yield true;}
            case 3->{boolean ok=game.items().cunnyTerrain().grass(p);if(ok)p.addEffect(ItemEffectLevels.create(MobEffects.REGENERATION,-1,2),p);yield ok;}
            case 4->{for(int[] offset:new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1},{2,0},{0,-2},{-2,0},{0,2},{2,0},{0,-2},{-2,0},{0,2},{2,-2},{-2,2},{2,2},{-2,-2}})
                    summonAt(p,p.position().add(offset[0],0,offset[1]),random.nextInt(6)+1);yield true;}
            case 5->{give(p,"lbc:imba/nahida_dakimakura");yield true;}
            case 6->{var candidates=p.level().players().stream().filter(e->hostile(p,e)&&!e.isSpectator()).toList();
                if(!candidates.isEmpty()){var target=candidates.get(random.nextInt(candidates.size()));pairs.add(new Pair(p.getUUID(),target.getUUID(),game.items().indirectKills().coldCause(p,false)));
                    p.entityTags().add("girl_seeker");target.entityTags().add("love_girl");target.sendSystemMessage(Component.translatable("you_loved_with").withColor(0xe680f3).append(p.getDisplayName()));}yield true;}
            case 7->{for(var target:p.level().players())if(target!=p&&active(target)&&!target.isSpectator())for(var reward:game.lbc().rollTable(p.level(),p.position().add(0,1,0),target,"lbc:anal_whip_named")){
                    var data=LbcFartus.data(reward);data.putString("skywars2.whip_target",target.getUUID().toString());data.putInt("teleportid",(int)(target.getUUID().getMostSignificantBits()>>>32));reward.set(DataComponents.CUSTOM_DATA,CustomData.of(data));drop(p,p.position().add(0,1,0),reward);}yield true;}
            case 8->{spawn(p,p.position(),"lbc:imba/cunny_slabs");for(var target:p.level().players())if(active(target)){
                    game.items().psycho().afflict(target,p,0,0,60,0);var old=emissions.get(target.getUUID());int ticks=(old==null?0:old.remaining)+160;
                    emissions.put(target.getUUID(),new Emission(ticks,old!=null&&old.visible,p.getUUID()));game.items().mana().score(target,"cumming").set(ticks);
                    LbcOverload.sound(target,target.position(),SOUNDS[random.nextInt(6)],2.5f,1,0);}yield true;}
            case 9->game.items().cunnyTerrain().teleport(p);
            case 10->{for(var id:List.of("lbc:luckyblock_all_stacks","lbc:op_hoe","lbc:god_dice"))give(p,id);
                Entity selected=null;int seen=0;for(var entity:p.level().getAllEntities())if(killCandidate(p,entity)&&random.nextInt(++seen)==0)selected=entity;
                if(selected!=null)randomKill(p,selected);yield true;}
            case 21->{var candidates=new ArrayList<Entity>();int seen=0;for(var entity:p.level().getAllEntities())if(!entity.isRemoved()&&game.items().wands().validRoleTarget(entity,false)){
                    seen++;if(candidates.size()<64)candidates.add(entity);else{int n=random.nextInt(seen);if(n<64)candidates.set(n,entity);}}
                for(var entity:candidates)summonAt(p,entity.position(),random.nextInt(6)+1);yield true;}
            default->true; // Source11..20 are deliberately inert consumables, with unchanged probability.
        };
        if(!result)return false;used.shrink(1);accepted++;p.getInventory().setChanged();p.containerMenu.broadcastChanges();
        announce(p,outcome);return true;
    }
    private void announce(ServerPlayer owner,int outcome){var text=Component.translatable(OUTCOME_KEYS[outcome]).withColor(0xe680f3);
        if(outcome==0||outcome==1||outcome==8||outcome==10||outcome==21)game.server().getPlayerList().broadcastSystemMessage(text,false);else owner.sendSystemMessage(text);}
    private void buff(ServerPlayer p,Holder<Attribute> type,double value,AttributeModifier.Operation operation){var a=p.getAttribute(type);if(a!=null&&!a.hasModifier(BUFF))a.addPermanentModifier(new AttributeModifier(BUFF,value,operation));}
    void summonAt(ServerPlayer p,Vec3 at,int choice){if(game.items().summons().cunnyCultist(p,at,choice)!=null){summons++;
        p.level().sendParticles(ParticleTypes.SOUL_FIRE_FLAME,at.x,at.y+1,at.z,100,5,5,5,0);
        for(float pitch:new float[]{2,1})LbcOverload.sound(p,at,"minecraft:entity.warden.agitated",1,pitch,0);
        for(int n=0;n<2;n++)LbcOverload.sound(p,at,"minecraft:entity.vex.ambient",1,1,0);}}
    private boolean killCandidate(ServerPlayer owner,Entity e){return !e.isRemoved()&&e.isAlive()&&game.items().wands().validRoleTarget(e,false)
            ;}
    boolean randomKill(ServerPlayer owner,Entity e){if(!killCandidate(owner,e))return false;
        if(game.items().wtf().erase(e)){kills++;return true;}
        if(e instanceof LivingEntity living){var cause=game.items().indirectKills().wildCause(owner);
            living.hurtServer(owner.level(),game.items().indirectKills().captureDamage(owner.level().damageSources().source(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL,owner),cause),Float.MAX_VALUE);
        }else e.kill(owner.level());kills++;return true;}
    private void give(ServerPlayer p,String id){for(var stack:game.lbc().rollTable(p.level(),p.position(),p,id))if(!p.getInventory().add(stack))drop(p,p.position(),stack);}
    private void spawn(ServerPlayer p,Vec3 at,String id){for(var stack:game.lbc().rollTable(p.level(),at,p,id))drop(p,at,stack);}
    private void drop(ServerPlayer p,Vec3 at,ItemStack stack){var e=new ItemEntity(p.level(),at.x,at.y,at.z,stack);e.setThrower(p);game.items().markCurrentItemEpoch(e);p.level().addFreshEntity(e);}
    boolean nahidaAllowed(ServerPlayer p,InteractionHand hand){return active(p)&&!paused(p)&&!game.items().actionsBlocked(p)
            &&(hand!=InteractionHand.OFF_HAND||game.config().offhandWands)&&LbcCunnyItems.nahida(p.getItemInHand(hand))&&!suppressed(p,Part.NAHIDA);}
    void nahidaUseTick(ServerPlayer p){if(paused(p))return;if(!nahidaAllowed(p,p.getUsedItemHand())||!p.isUsingItem())return;
        if(!nahida.containsKey(p.getUUID()))startNahidaSound(p);
        nahida.put(p.getUUID(),phase);p.entityTags().add("plapper");
        for(var type:List.of(Attributes.MAX_HEALTH,Attributes.MAX_ABSORPTION,Attributes.ATTACK_DAMAGE,Attributes.CAMERA_DISTANCE)){
            var a=p.getAttribute(type);if(a!=null&&!a.hasModifier(PLAP))a.addTransientModifier(new AttributeModifier(PLAP,-1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));}
        LbcAttributeGains.apply(p,random.nextInt(26)+1);
    }
    private void startNahidaSound(ServerPlayer owner){var at=owner.position();for(var listener:game.server().getPlayerList().getPlayers())
        listener.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(Holder.direct(
                net.minecraft.sounds.SoundEvent.createVariableRangeEvent(Identifier.parse("minecraft:lbcsounds.bafled"))),
                net.minecraft.sounds.SoundSource.MASTER,at.x,at.y,at.z,2,1,0));}
    private void stopNahidaSound(){for(var listener:game.server().getPlayerList().getPlayers())listener.connection.send(
            new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(Identifier.parse("minecraft:lbcsounds.bafled"),net.minecraft.sounds.SoundSource.MASTER));}
    private void stopNahida(ServerPlayer p){boolean wasActive=nahida.remove(p.getUUID())!=null||p.entityTags().contains("plapper");if(wasActive)stopNahidaSound();p.entityTags().remove("plapper");for(var type:List.of(Attributes.MAX_HEALTH,Attributes.MAX_ABSORPTION,Attributes.ATTACK_DAMAGE,Attributes.CAMERA_DISTANCE)){
        var a=p.getAttribute(type);if(a!=null)a.removeModifier(PLAP);}}
    void tick(){if(restoring||!game.server().tickRateManager().runsNormally()||game.server().tickRateManager().isFrozen())return;
        if(game.items().timeStop().stopped(game.maps().level()))return;
        advancing=true;try{phase++;
        for(var id:List.copyOf(nahida.keySet())){var p=game.server().getPlayerList().getPlayer(id);if(p==null){nahida.remove(id);stopNahidaSound();continue;}if(paused(p))continue;
            if(!active(p)||!p.isUsingItem()||!LbcCunnyItems.nahida(p.getUseItem())||suppressed(p,Part.NAHIDA))stopNahida(p);}
        var moved=new HashSet<UUID>();for(var pair:List.copyOf(pairs)){var owner=game.server().getPlayerList().getPlayer(pair.seeker);var target=game.server().getPlayerList().getPlayer(pair.target);
            if(owner==null||target==null)continue;if(!active(owner)||!active(target)){removePair(pair);continue;}
            if(owner.level()!=target.level()||paused(owner)||paused(target)||!hostile(owner,target)||suppressed(owner,Part.PAIR)||suppressed(target,Part.PAIR))continue;
            if(!target.onGround()){var nearest=pairs.stream().filter(x->x.target.equals(pair.target)).map(x->game.server().getPlayerList().getPlayer(x.seeker))
                    .filter(Objects::nonNull).filter(x->x.level()==target.level()&&active(x)).min(Comparator.comparingDouble(target::distanceToSqr)).orElse(owner);
                // Once per target each active tick, retaining that actual seeker's causal owner.
                if(nearest==owner&&moved.add(target.getUUID())){var direction=owner.position().subtract(target.position()).normalize().scale(.1);
                target.setDeltaMovement(target.getDeltaMovement().add(direction));target.needsSync=true;target.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(target));game.items().indirectKills().displaced(target,pair.cause);
                target.level().sendParticles(ParticleTypes.HEART,target.getX(),target.getY()+3,target.getZ(),1,0,0,0,0);}}
            if((phase&1)==0){
                if(!game.items().magic().suppressedAt(target.level(),target.position()))target.level().sendParticles(owner,ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT,0xff000300),true,false,target.getX(),target.getY(),target.getZ(),1,0,0,0,0);
                var at=owner.position().add(0,3,0);if(!game.items().magic().suppressedAt(owner.level(),at)){
                    var listener=owner.level().players().stream().filter(x->!x.isSpectator()&&active(x)).min(Comparator.comparingDouble(x->x.position().distanceToSqr(at))).orElse(null);
                    if(listener!=null){var facing=target.getEyePosition().subtract(at).normalize();for(double step:new double[]{.1,.5,.9,1.3,1.7,2.1,2.6,3.2}){
                        var point=at.add(facing.scale(step));owner.level().sendParticles(listener,ParticleTypes.FLAME,false,false,point.x,point.y,point.z,1,0,0,0,0);}}
                }}
        }
        for(var id:List.copyOf(emissions.keySet())){var p=game.server().getPlayerList().getPlayer(id);var e=emissions.get(id);if(p==null)continue;if(paused(p))continue;
            if(!active(p)||e.remaining<=1){emissions.remove(id);p.entityTags().remove("cumming");game.items().mana().score(p,"cumming").set(0);continue;}
            boolean visible=e.visible;if(random.nextInt(10)==0)visible=false;if(random.nextInt(10)==0)visible=true;
            emissions.put(id,new Emission(e.remaining-1,visible,e.owner));game.items().mana().score(p,"cumming").set(e.remaining-1);
            if(visible){p.entityTags().add("cumming");var item=new ItemParticleOption(ParticleTypes.ITEM,ItemStackTemplate.fromNonEmptyStack(new ItemStack(net.minecraft.world.level.block.Blocks.STAINED_GLASS.pick(DyeColor.WHITE).asItem())));
                var forward=p.getLookAngle();var left=new Vec3(forward.z,0,-forward.x).normalize();var one=left.scale(100).add(forward.scale(70000));var two=left.scale(-100).add(forward.scale(60000));
                p.level().sendParticles(item,p.getX(),p.getY()+.9,p.getZ(),0,one.x,one.y,one.z,.00001);p.level().sendParticles(item,p.getX(),p.getY()+.85,p.getZ(),0,two.x,two.y,two.z,.00001);
            }else p.entityTags().remove("cumming");}
        }finally{advancing=false;}}
    private void removePair(Pair pair){pairs.remove(pair);var seeker=game.server().getPlayerList().getPlayer(pair.seeker);var target=game.server().getPlayerList().getPlayer(pair.target);
        if(seeker!=null&&pairs.stream().noneMatch(x->x.seeker.equals(pair.seeker)))seeker.entityTags().remove("girl_seeker");
        if(target!=null&&pairs.stream().noneMatch(x->x.target.equals(pair.target)))target.entityTags().remove("love_girl");}
    void forget(ServerPlayer p){if(restoring||MatchPlayerCheckpoint.restoring(p))return;stopNahida(p);for(var pair:List.copyOf(pairs))if(pair.seeker.equals(p.getUUID())||pair.target.equals(p.getUUID()))removePair(pair);
        emissions.remove(p.getUUID());p.entityTags().remove("cumming");game.items().mana().score(p,"cumming").set(0);}
    void stop(){checkpointBoundary();for(var p:game.server().getPlayerList().getPlayers())forget(p);pairs.clear();nahida.clear();emissions.clear();}
    void checkpointBoundary(){if(advancing)throw new IllegalStateException("Finish Cunny active tick before checkpoint");}
    State state(){checkpointBoundary();return new State(pairs,nahida,emissions,phase,MatchRandomState.capture(random),accepted,kills,summons);}
    void restore(State state){checkpointBoundary();pairs.clear();pairs.addAll(state.pairs);nahida.clear();nahida.putAll(state.nahida);emissions.clear();emissions.putAll(state.emissions);
        phase=state.phase;state.random.restore(random);accepted=state.accepted;kills=state.kills;summons=state.summons;}
    void prepareFrozen(){checkpointBoundary();restoring=true;if(!nahida.isEmpty())stopNahidaSound();}
    void cancelFrozen(){checkpointBoundary();restoring=false;}
    void publishFrozen(){checkpointBoundary();boolean publish=restoring;restoring=false;if(publish)for(var id:nahida.keySet()){
        var p=game.server().getPlayerList().getPlayer(id);if(p!=null&&active(p))startNahidaSound(p);}}
    MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links){var saved=state();return current->()->restore(saved);}
}
