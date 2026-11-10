package me.skywars2.game;

import java.lang.reflect.Method;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.phys.*;

/** Optional Carpet players: shared Chef routing, actual survival inputs/inventory and native attacks. */
public final class LbcCarpetBots {
    private static final class RangedWands {
        static final Set<Item> ITEMS=java.util.stream.Stream.concat(LbcWands.tmtAbilities().stream()
                .map(ability->net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(ability.nativeItem())),LbcAcademy.wandItems().stream())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    private final GameController game;
    private final Map<UUID,State> states=new LinkedHashMap<>();
    private final Map<UUID,ServerPlayer> pendingDeaths=new LinkedHashMap<>();
    public LbcCarpetBots(GameController game){this.game=game;}
    void damaged(ServerPlayer player,net.minecraft.world.damagesource.DamageSource source){
        var state=states.get(player.getUUID());
        if(state==null||state.player!=player)return;
        if(source.is(net.minecraft.world.damagesource.DamageTypes.THORNS)
                &&source.getEntity() instanceof net.minecraft.world.entity.LivingEntity defender)state.defense.rememberThorns(defender);
        if((source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK)||source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK))
                &&source.getDirectEntity() instanceof net.minecraft.world.entity.LivingEntity attacker&&attacker!=player&&!player.isAlliedTo(attacker))state.defense.meleeDamage(attacker);
        if(source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile shot
                &&shot.getOwner() instanceof net.minecraft.world.entity.LivingEntity attacker&&attacker!=player&&!player.isAlliedTo(attacker))
            state.defense.rangedDamage(attacker);
    }
    static boolean bot(ServerPlayer player){return player.getGameProfile().name().regionMatches(true,0,"bot",0,3)
            &&player.getClass().getName().equals("carpet.patches.EntityPlayerMPFake");}
    /** Let native death/drop/life accounting finish before replacing the real Carpet body. */
    public boolean died(ServerPlayer player){
        if(!bot(player)||!game.playing()||game.lives(player.getUUID())<=0||game.isObserver(player))return false;
        forget(player);pendingDeaths.put(player.getUUID(),player);return true;
    }
    public boolean recover(ServerPlayer player){
        if(pendingDeaths.remove(player.getUUID())!=player)return false;
        if(game.server().getPlayerList().getPlayer(player.getUUID())!=player)return true;
        player.setHealth(0);
        player.connection.handleClientCommand(new net.minecraft.network.protocol.game.ServerboundClientCommandPacket(
                net.minecraft.network.protocol.game.ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
        var replacement=game.server().getPlayerList().getPlayer(player.getUUID());
        if(replacement!=null&&replacement!=player){replacement.hasChangedDimension();replacement.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());}
        return replacement!=player;
    }
    public void tick(ServerPlayer player){
        if(!bot(player))return;
        var state=states.get(player.getUUID());
        if(state!=null&&(state.player!=player||state.level!=player.level()||!state.epoch.equals(game.items().epoch()))){forget(player);state=null;}
        if(!game.playing()||game.stage()==GameController.Stage.ENDING||!player.isAlive()||player.isRemoved()
                ||player.level()!=game.maps().level()||game.isObserver(player)||player.isSpectator()||!game.canUseGameplayItems(player)){
            forget(player);return;
        }
        if(state==null){state=new State(player);states.put(player.getUUID(),state);}
        if(state.lastTick==game.server().getTickCount())return;
        state.lastTick=game.server().getTickCount();
        if(!game.server().tickRateManager().runsNormally()||game.vanity().blocked(player)
                ||game.items().timeStop().frozen(player)||game.items().shooters().stunned(player)||game.items().fpvDrone().controlling(player)){
            state.abortMining();state.halt();state.commit();return;
        }
        state.tick();state.commit();
    }
    public void forget(ServerPlayer player){pendingDeaths.remove(player.getUUID());var old=states.remove(player.getUUID());if(old!=null)old.close();}
    public void reset(){for(var state:List.copyOf(states.values()))state.close();states.clear();
        // Death transport belongs to the native body, even if a match ends before its next tick.
        pendingDeaths.entrySet().removeIf(e->game.server().getPlayerList().getPlayer(e.getKey())!=e.getValue());}
    void prepareCheckpoint(){for(var state:List.copyOf(states.values()))state.crafting.cancel();}
    public boolean managed(ServerPlayer player){return bot(player)&&states.containsKey(player.getUUID());}
    int count(){return states.size();}
    record Observation(int attempts,int hits,int looted,int placements,int foods){}
    Observation observation(ServerPlayer player){var s=states.get(player.getUUID());return s==null?new Observation(0,0,0,0,0):new Observation(s.attempts,s.hits,s.looted,s.placements,s.foods);}
    String diagnostics(ServerPlayer player){var state=states.get(player.getUUID());return state==null?"inactive":state.diagnostics();}

    /** Resolve the optional API once, rather than reflection/NBT discovery on every input. */
    private static final class CarpetInput {
        static final Method GET,FORWARD,STRAFE,SNEAK,SPRINT,START,ONCE,CONTINUOUS,STOP;
        static final Object JUMP;
        static {try{
            GET=Class.forName("carpet.fakes.ServerPlayerInterface").getMethod("getActionPack");
            var type=Class.forName("carpet.helpers.EntityPlayerActionPack");
            var action=Class.forName("carpet.helpers.EntityPlayerActionPack$Action");var actionType=Class.forName("carpet.helpers.EntityPlayerActionPack$ActionType");
            START=type.getMethod("start",actionType,action);ONCE=action.getMethod("once");CONTINUOUS=action.getMethod("continuous");JUMP=actionType.getField("JUMP").get(null);
            FORWARD=type.getMethod("setForward",float.class);STRAFE=type.getMethod("setStrafing",float.class);SNEAK=type.getMethod("setSneaking",boolean.class);SPRINT=type.getMethod("setSprinting",boolean.class);STOP=type.getMethod("stopAll");
        }catch(ReflectiveOperationException error){throw new ExceptionInInitializerError(error);}}
        final Object pack;boolean jumpDown,swimming;
        CarpetInput(ServerPlayer player){try{pack=GET.invoke(player);STOP.invoke(pack);}catch(ReflectiveOperationException error){throw new IllegalStateException("Carpet bot input",error);}}
        void jump(boolean down,boolean swim){try{if(down!=jumpDown||down&&swim!=swimming){START.invoke(pack,JUMP,down?(swim?CONTINUOUS:ONCE).invoke(null):null);jumpDown=down;swimming=swim;}}catch(ReflectiveOperationException error){throw new IllegalStateException("Carpet bot jump",error);}}
        void move(float forward,float side,boolean sneak,boolean sprint){try{SNEAK.invoke(pack,sneak);SPRINT.invoke(pack,sprint);FORWARD.invoke(pack,forward);STRAFE.invoke(pack,side);}catch(ReflectiveOperationException error){throw new IllegalStateException("Carpet bot movement",error);}}
    }
    private final class State implements LbcChefBrain.Driver {
        final ServerPlayer player;
        final net.minecraft.server.level.ServerLevel level;
        final String epoch;
        final CarpetInput input;
        final LbcChefBrain brain;final LbcBotSurvival survival;final LbcBotDefense defense;final LbcBotCrafting crafting;final LbcBotAcademy academy;
        final Map<BlockPos,Integer> visited=new LinkedHashMap<>();
        final List<BlockPos> chests=new ArrayList<>();
        List<ItemStack> tools=List.of(ItemStack.EMPTY);
        Vec3 goal;net.minecraft.world.entity.LivingEntity enemy;
        final Vec3 island;boolean startingLoot=true;UUID aggressor;int hurtStamp=Integer.MIN_VALUE,aggressorUntil,nextInventory;
        UUID droppedLoot,ignoredLoot;int droppedUntil,ignoredUntil;Vec3 afterPickup;
        BlockPos lucky,chest,mining;Direction miningFace=Direction.UP;int miningAdmission;
        int lastTick=Integer.MIN_VALUE,nextScan,nextGear,nextRoam,chestSince,lootUntil,turnedTick=Integer.MIN_VALUE,miningSince,miningTick;
        int nextSwing,actions,attempts,hits,looted,placements,foods,crystalsCleared;boolean build,closing;
        float forward,side;boolean jump,sprint,crouch;
        State(ServerPlayer player){this.player=player;level=player.level();epoch=game.items().epoch();input=new CarpetInput(player);island=player.position();goal=player.position();brain=new LbcChefBrain(player,this);survival=new LbcBotSurvival(player,this,brain,at->goal=at);defense=new LbcBotDefense(player,this,survival);crafting=new LbcBotCrafting(player,this,survival);academy=new LbcBotAcademy(player,this,brain,at->goal=at);}
        String diagnostics(){return "goal="+goal+" chest="+chest+" enemy="+(enemy==null?"none":enemy.getUUID())+" yaw="+player.getYRot()+" ranged="+rangedLoadout()+" los="+(enemy!=null&&player.hasLineOfSight(enemy))+" thorn="+defense.thorns(enemy)+" mana="+game.items().mana().get(player)+" nomagic="+game.items().mana().noMagic(player)+" nextAbility="+nextAbility+" now="+game.server().getTickCount()+" using="+player.isUsingItem()+" hand="+player.getMainHandItem()+" playerMining="+mining+" miningAdmission="+miningAdmission+" blockRange="+player.blockInteractionRange()+" menu="+player.containerMenu.getClass().getSimpleName()+" crafts="+crafting.crafted+" craftStage="+crafting.stage+" craftPair="+crafting.first+","+crafting.second+" searched="+crafting.searched+" attempts="+attempts+" hits="+hits+" looted="+looted+" blocks="+placements+" foods="+foods+" "+crafting.tools.diagnostics()+" "+survival.diagnostics()+" "+defense.diagnostics()+" "+brain.diagnostics();}
        void tick(){
            halt();build=blockSlot()>=0;
            refreshTools();
            int now=game.server().getTickCount();
            if(now>=nextGear&&!crafting.active()&&!survival.urgent()&&!brain.preciseAction()&&!player.isUsingItem()&&player.containerMenu==player.inventoryMenu){gear();nextGear=now+5;}
            var attacker=player.getLastHurtByMob();
            if(attacker!=null&&attacker!=player&&player.getLastHurtByMobTimestamp()!=hurtStamp){hurtStamp=player.getLastHurtByMobTimestamp();aggressor=attacker.getUUID();aggressorUntil=now+1200;}
            if(defense.retaliation!=null){aggressor=defense.retaliation.getUUID();aggressorUntil=now+1200;defense.retaliation=null;}
            var previousEnemy=enemy;enemy=nearestEnemy();
            if(enemy!=previousEnemy){brain.reset();abortMining();}
            if(player.isUsingItem()&&(survival.urgent()||
                    player.getUseItem().has(DataComponents.FOOD)&&defense.shelterUrgent()||
                    (player.getUseItem().getItem() instanceof BowItem||player.getUseItem().getItem() instanceof CrossbowItem
                    ||player.getUseItem().getItem() instanceof TridentItem)&&defense.interruptUse(enemy)))player.stopUsingItem();
            if(survival.tick(enemy!=null)){crafting.cancel();academy.cancel();return;}
            if(brain.freeBody()){crafting.cancel();academy.cancel();return;}
            if(player.isUsingItem()){
                if(defense.holdingShield())return;
                var used=player.getUseItem();
                if(used.getItem() instanceof BowItem||used.getItem() instanceof CrossbowItem||used.getItem() instanceof TridentItem){
                    if(!pursueUsing(enemy==null?player.getEyePosition():shotAim(),false))return;
                    // Native onUseTick loads the crossbow, including Quick Charge and special ammunition.
                    if(used.getItem() instanceof CrossbowItem?CrossbowItem.isCharged(used):player.getTicksUsingItem()>=(used.getItem() instanceof TridentItem?10:20))
                        player.releaseUsingItem();
                }
                else if(enemy!=null&&used.has(DataComponents.FOOD)&&!defense.holdsHighGround(enemy)&&!defense.sheltered(enemy))defense.retreat(enemy.position(),9);
                else if(!used.has(DataComponents.FOOD)){
                    boolean melee=attribute(used,Attributes.ATTACK_DAMAGE,1,EquipmentSlot.MAINHAND)>1;
                    if(!pursueUsing(enemy==null?player.getEyePosition():enemy.getEyePosition(),melee))return;
                    // Repeating native mace input must yield to an actual melee swing.
                    // Guns/channels keep their native onUseTick and track a moving target.
                    double reach=player.entityInteractionRange()+enemy.getBbWidth()*.5;
                    if(player.distanceToSqr(enemy)<=reach*reach&&player.getTicksUsingItem()>=2
                            &&melee)player.releaseUsingItem();
                }
                return;
            }
            if(defense.tick(enemy)){crafting.cancel();academy.cancel();return;}
            if(crafting.tick(enemy!=null||startingLoot))return;
            if(academy.tick(enemy!=null||startingLoot))return;
            if(enemy!=null&&player.getHealth()<player.getMaxHealth()*.65&&eat()){
                if(!defense.holdsHighGround(enemy)&&!defense.sheltered(enemy))defense.retreat(enemy.position(),9);return;
            }
            if(enemy!=null){
                stopCollecting();
                if(player.containerMenu!=player.inventoryMenu)player.closeContainer();
                goal=enemy.position();chest=null;
                if(clearCrystal())return;
                double distance=player.distanceToSqr(enemy);boolean shooting=rangedLoadout();
                if(defense.sheltered(enemy)){turn(enemy.getEyePosition());return;}
                if(defense.holdsHighGround(enemy)){
                    if(!defense.thorns(enemy)&&player.hasLineOfSight(enemy)){
                        if(shooting&&(ranged()||ability()))return;
                        weapon();turn(enemy.getEyePosition());attack();
                    }else turn(enemy.getEyePosition());
                    return;
                }
                if(shooting&&distance<16&&!defense.thorns(enemy)&&defense.retreat(enemy.position(),6))return;
                if(shooting&&!defense.thorns(enemy)&&distance>=16&&distance<=32*32&&player.hasLineOfSight(enemy)){
                    if(distance>12*12){
                        if(!brain.preciseAction())turn(rangedSlot()>=0?shotAim():enemy.getEyePosition());
                        if(!brain.closeGround(enemy.position()))brain.tick();
                    }
                    if(brain.preciseAction())return;
                    if(!ranged()&&!ability())turn(enemy.getEyePosition());return;
                }
                weapon();
                if(defense.thorns(enemy)){
                    // Once safely separated, continue toward the centre instead of staring
                    // forever at an opponent which cannot be attacked without retaliation.
                    if(distance>=100){goal=new Vec3(0,player.getY(),0);if(!brain.closeGround(goal))brain.tick();}
                    else turn(enemy.getEyePosition());return;
                }
                double reach=player.entityInteractionRange();
                if(distance>Math.pow(reach+enemy.getBbWidth()*.5,2)||!player.hasLineOfSight(enemy)){if(!brain.closeGround(enemy.position()))brain.tick();}
                if(brain.preciseAction())return;
                if(!defense.thorns(enemy)&&(ranged()||ability()))return;
                if(defense.thorns(enemy))return;
                turn(enemy.getEyePosition());attack();return;
            }
            if(player.containerMenu!=player.inventoryMenu){loot(now);return;}
            if(eat())return;
            if(collectDrop(now))return;
            if(now>=nextScan){scanChests(now);nextScan=now+40;}
            if(chest==null){
                chest=chests.stream().filter(p->visited.getOrDefault(p,Integer.MIN_VALUE)<=now&&(!startingLoot?Vec3.atCenterOf(p).distanceToSqr(player.position())<=12*12:Vec3.atCenterOf(p).distanceToSqr(island)<=18*18))
                        .min(Comparator.comparingDouble(p->Vec3.atCenterOf(p).distanceToSqr(player.position()))).orElse(null);
                if(chest!=null){chestSince=now;goal=Vec3.atBottomCenterOf(chest);brain.reset();}
            }
            if(chest!=null){
                goal=Vec3.atBottomCenterOf(chest);
                if(now-chestSince>300){visited.put(chest,now+200);chest=null;brain.reset();return;}
                if(!level.hasChunkAt(chest)||!(level.getBlockEntity(chest) instanceof Container)){
                    visited.put(chest,now+200);chest=null;return;
                }
                Vec3 center=Vec3.atCenterOf(chest);
                if(center.distanceToSqr(player.getEyePosition())<16&&aimAction(center)){
                    var ray=level.clip(new ClipContext(player.getEyePosition(),center,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
                    if(ray.getType()==HitResult.Type.BLOCK&&ray.getBlockPos().equals(chest)){
                        if(!emptyHand()){visited.put(chest,now+200);chest=null;return;}
                        var result=player.gameMode.useItemOn(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND,ray);
                        if(result.consumesAction()&&player.containerMenu!=player.inventoryMenu){lootUntil=now+40;return;}
                        visited.put(chest,now+100);chest=null;return;
                    }
                }
                brain.tick();return;
            }
            if(startingLoot){startingLoot=false;nextRoam=0;}
            if(!crafting.prepared()){
                crafting.tick(false);
                if(!crafting.prepared())return;
            }
            if(openLucky())return;
            if(now>=nextRoam||goal.distanceToSqr(player.position())<2){
                chooseRoam();nextRoam=now+120;brain.reset();
            }
            brain.tick();
        }
        private net.minecraft.world.entity.LivingEntity nearestEnemy(){
            int now=game.server().getTickCount();
            if(aggressor!=null&&now<aggressorUntil&&level.getEntity(aggressor) instanceof net.minecraft.world.entity.LivingEntity remembered&&hostile(remembered))return remembered;
            if(enemy!=null&&hostile(enemy)&&player.distanceToSqr(enemy)<48*48&&player.hasLineOfSight(enemy))return enemy;
            net.minecraft.world.entity.LivingEntity best=null;double closest=Double.POSITIVE_INFINITY;
            for(var candidate:level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,player.getBoundingBox().inflate(16),this::hostile)){
                double distance=player.distanceToSqr(candidate);boolean otherBot=candidate instanceof ServerPlayer p&&bot(p);
                if(distance>(otherBot?64:196))continue;double rank=distance*(otherBot?1.6:1);
                if(rank<closest){closest=rank;best=candidate;}
            }
            return best;
        }
        private boolean hostile(net.minecraft.world.entity.LivingEntity target){
            if(target==player||!target.isAlive()||target.isRemoved()||target.isSpectator()||player.isAlliedTo(target))return false;
            if(target instanceof ServerPlayer p)return !game.isObserver(p)&&game.allowDamage(p);
            return target.getUUID().equals(aggressor)||target instanceof net.minecraft.world.entity.Mob mob&&mob.getTarget()==player;
        }
        int nextLuckyScan;
        private boolean openLucky(){
            int now=game.server().getTickCount();
            if(lucky!=null){if(LbcLuckyBlocks.kind(level.getBlockState(lucky))==null){lucky=null;brain.reset();return true;}goal=Vec3.atBottomCenterOf(lucky);if(player.getEyePosition().distanceToSqr(Vec3.atCenterOf(lucky))<16){mine(lucky);}else brain.tick();return true;}
            if(now<nextLuckyScan)return false;nextLuckyScan=now+20;
            for(var at:BlockPos.betweenClosed(player.blockPosition().offset(-6,-2,-6),player.blockPosition().offset(6,3,6)))if(level.hasChunkAt(at)&&LbcLuckyBlocks.kind(level.getBlockState(at))!=null&&(lucky==null||player.distanceToSqr(Vec3.atCenterOf(at))<player.distanceToSqr(Vec3.atCenterOf(lucky))))lucky=at.immutable();
            if(lucky!=null)return true;
            for(int i=0;i<36;i++){var stack=player.getInventory().getItem(i);if(!(stack.getItem() instanceof BlockItem block)||LbcLuckyBlocks.kind(block.getBlock().defaultBlockState())==null)continue;
                if(!select(i))return true;var floor=player.blockPosition().offset(1,-1,0);var at=Vec3.atCenterOf(floor).add(0,.5,0);turn(at);
                var hit=LbcBotActions.ray(player,player.blockInteractionRange());if(hit.getType()!=HitResult.Type.BLOCK||!hit.getBlockPos().equals(floor)||hit.getDirection()!=Direction.UP)return true;
                var target=hit.getBlockPos().above();if(!level.getBlockState(target).canBeReplaced())continue;
                player.gameMode.useItemOn(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
                if(LbcLuckyBlocks.kind(level.getBlockState(target))!=null)lucky=target;return true;
            }
            return false;
        }
        private Vec3 shotAim(){double ticks=Math.min(20,player.distanceTo(enemy)/3);return enemy.getEyePosition().add(enemy.getDeltaMovement().scale(ticks)).add(0,.025*ticks*ticks,0);}
        private int rangedSlot(){
            for(int slot=0;slot<36;slot++){
                var stack=player.getInventory().getItem(slot);
                if((stack.getItem() instanceof BowItem||stack.getItem() instanceof CrossbowItem)
                        &&(stack.getItem() instanceof CrossbowItem&&CrossbowItem.isCharged(stack)||!player.getProjectile(stack).isEmpty()))return slot;
            }
            return -1;
        }
        private boolean ranged(){
            if(player.distanceToSqr(enemy)<16&&!defense.holdsHighGround(enemy)||player.distanceToSqr(enemy)>32*32||!player.hasLineOfSight(enemy))return false;
            int slot=rangedSlot();if(slot<0)return false;
            turn(shotAim());if(!select(slot))return true;
            if(player.getLookAngle().dot(shotAim().subtract(player.getEyePosition()).normalize())<.995)return true;
            return player.gameMode.useItem(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction();
        }
        private boolean clearCrystal(){
            var sight=level.clip(new ClipContext(player.getEyePosition(),enemy.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
            if(sight.getType()!=HitResult.Type.BLOCK)return false;
            var crystal=game.items().iceWand().crystalAt(level,sight.getBlockPos());if(crystal==null)return false;
            var center=crystal.getBoundingBox().getCenter();double reach=player.entityInteractionRange();
            if(crystal.getBoundingBox().distanceToSqr(player.getEyePosition())>reach*reach){
                var away=player.position().subtract(center).multiply(1,0,1).normalize();
                goal=new Vec3(center.x+away.x*1.6,player.getY(),center.z+away.z*1.6);
                if(!brain.closeGround(goal))brain.tick();return true;
            }
            halt();abortMining();
            if(!aimAction(center)||!LbcBotActions.melee(player,crystal,reach))return true;
            int now=game.server().getTickCount();if(now<nextSwing)return true;
            // Use the same native attack packet and Fabric interaction callback as a real player.
            player.connection.handleAttack(new net.minecraft.network.protocol.game.ServerboundAttackPacket(crystal.getId()));
            player.swing(InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT,true);
            if(crystal.isRemoved())crystalsCleared++;
            nextSwing=now+5;return true;
        }
        private boolean rangedLoadout(){
            if(rangedSlot()>=0)return true;
            for(int slot=0;slot<36;slot++){
                var stack=player.getInventory().getItem(slot);if(stack.isEmpty())continue;
                if(RangedWands.ITEMS.contains(stack.getItem()))return true;
                if(LbcTmtItems.matches(stack)&&LbcTmtAbilities.read(stack).stream().anyMatch(ability->ability.id().startsWith("wand/")))return true;
            }
            return false;
        }
        int nextAbility,abilitySlot;
        private boolean pursueUsing(Vec3 aim,boolean melee){
            if(enemy==null||player.distanceToSqr(enemy)>32*32||!player.hasLineOfSight(enemy)){player.stopUsingItem();return false;}
            double wanted=melee?player.entityInteractionRange()+enemy.getBbWidth()*.5-.25:12;
            if(player.distanceToSqr(enemy)>wanted*wanted&&!defense.holdsHighGround(enemy)){
                if(!brain.preciseAction())turn(aim);
                if(!brain.closeGround(enemy.position()))brain.tick();
                if(brain.preciseAction()){player.stopUsingItem();return false;}
            }
            turn(aim);return true;
        }
        private boolean ability(){
            int now=game.server().getTickCount();if(now<nextAbility||!player.hasLineOfSight(enemy))return false;
            for(int offset=0;offset<36;offset++){int i=(abilitySlot+offset)%36;var stack=player.getInventory().getItem(i);if(stack.isEmpty()||stack.has(DataComponents.FOOD))continue;
                if(LbcTmtExtraUses.candidate(stack)&&!LbcTmtExtraUses.combatCandidate(stack))continue;
                boolean nativeCandidate=game.items().wands().tmtNativeUseCandidate(stack)||LbcTmtExtraUses.candidate(stack)||LbcTmtBlockUses.airCandidate(stack);
                var model=stack.get(DataComponents.ITEM_MODEL);
                boolean custom=model!=null&&model.getNamespace().equals("lbc");
                if(!nativeCandidate&&(!custom||stack.getItem() instanceof BlockItem))continue;
                turn(enemy.getEyePosition());if(!select(i))return true;
                if(player.getLookAngle().dot(enemy.getEyePosition().subtract(player.getEyePosition()).normalize())<.99)return true;
                abilitySlot=(i+1)%36;
                boolean used=player.gameMode.useItem(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction();
                nextAbility=now+(used?10:2);return used;
            }
            return false;
        }
        private void attack(){
            if(enemy==null||player.distanceToSqr(enemy)>Math.pow(player.entityInteractionRange()+enemy.getBbWidth()*.5,2))return;
            int now=game.server().getTickCount();if(now<nextSwing||player.getAttackStrengthScale(.5f)<.9)return;
            nextSwing=now+5;attempts++;double reach=player.entityInteractionRange();var start=player.getEyePosition();var end=start.add(player.getLookAngle().scale(reach));
            var wall=level.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
            if(wall.getType()!=HitResult.Type.MISS)end=wall.getLocation();
            net.minecraft.world.entity.LivingEntity hit=null;double distance=start.distanceToSqr(end);
            for(var target:level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1),this::hostile)){
                var box=target.getBoundingBox().inflate(target.getPickRadius());var contact=box.clip(start,end);
                if(box.contains(start)){hit=target;distance=0;}
                else if(contact.isPresent()&&start.distanceToSqr(contact.get())<distance){hit=target;distance=start.distanceToSqr(contact.get());}
            }
            if(hit!=null){float before=player.getHealth()+player.getAbsorptionAmount();player.attack(hit);defense.observedHit(hit,before);hits++;}else player.resetAttackStrengthTicker();
            player.swing(InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT,true);
        }
        private void scanChests(int now){
            chests.clear();visited.entrySet().removeIf(e->e.getValue()<=now-200);
            int x=player.blockPosition().getX()>>4,z=player.blockPosition().getZ()>>4;
            // Resident chunks only: no chunk loads, loot-table unpacking or block/NBT volume scan.
            for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++){
                var chunk=level.getChunkSource().getChunkNow(x+dx,z+dz);if(chunk==null)continue;
                for(var entity:chunk.getBlockEntities().values())if(entity instanceof Container){
                    var p=entity.getBlockPos();if(Vec3.atCenterOf(p).distanceToSqr(player.position())<=48*48)chests.add(p.immutable());
                }
            }
            if(visited.size()>128){var iterator=visited.keySet().iterator();while(visited.size()>96){iterator.next();iterator.remove();}}
        }
        private boolean collectDrop(int now){
            // Let actual movement and native pickup delay collect mined/Lucky loot. No inventory grant.
            if(droppedLoot!=null&&now>=droppedUntil){ignoredLoot=droppedLoot;ignoredUntil=now+200;droppedLoot=null;}
            var drop=droppedLoot==null?null:level.getEntity(droppedLoot);
            if(!(drop instanceof net.minecraft.world.entity.item.ItemEntity item)||!item.isAlive()||item.isRemoved()){
                droppedLoot=null;
                drop=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,player.getBoundingBox().inflate(4),e->e.isAlive()&&!e.isRemoved()&&!e.getItem().isEmpty()
                        &&(now>=ignoredUntil||!e.getUUID().equals(ignoredLoot))
                        &&(player.getInventory().getFreeSlot()>=0||player.getInventory().countItem(e.getItem().getItem())<e.getItem().getMaxStackSize()))
                        .stream().min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
                if(drop==null){stopCollecting();return false;}droppedLoot=drop.getUUID();droppedUntil=now+80;
            }
            if(afterPickup==null){afterPickup=goal;brain.reset();}
            var delta=drop.position().subtract(player.position());
            goal=new Vec3(drop.getX(),Math.abs(delta.y)<2?player.getY():Math.floor(drop.getY()),drop.getZ());
            if(delta.horizontalDistanceSqr()<.36&&Math.abs(delta.y)<2){halt();return true;}
            if(!brain.closeGround(goal))brain.tick();return true;
        }
        private void stopCollecting(){
            droppedLoot=null;
            if(afterPickup!=null){goal=afterPickup;afterPickup=null;brain.reset();}
        }
        private void loot(int now){
            var menu=player.containerMenu;if(chest==null||now>=lootUntil||!menu.stillValid(player)){finishLoot(now);return;}
            if(now<nextInventory)return;
            int chosen=-1;double best=0;
            for(int i=0;i<menu.slots.size();i++){var slot=menu.slots.get(i);if(slot.container==player.getInventory()||!slot.hasItem()||!slot.mayPickup(player))continue;double score=lootValue(slot.getItem());if(score>best){best=score;chosen=i;}}
            if(chosen<0){finishLoot(now);return;}
            var slot=menu.slots.get(chosen);int before=slot.getItem().getCount();menu.clicked(chosen,0,ContainerInput.QUICK_MOVE,player);nextInventory=now+5;
            if(slot.getItem().getCount()<before){looted+=before-slot.getItem().getCount();lootUntil=now+40;}
            else {int worst=-1;double value=best;for(int i=0;i<menu.slots.size();i++){var own=menu.slots.get(i);if(own.container==player.getInventory()&&own.hasItem()&&lootValue(own.getItem())<value){value=lootValue(own.getItem());worst=i;}}if(worst>=0)menu.clicked(worst,1,ContainerInput.THROW,player);else finishLoot(now);}
            menu.broadcastChanges();
        }
        private double lootValue(ItemStack stack){
            var equipment=stack.get(DataComponents.EQUIPPABLE);
            if(equipment!=null){double value=armor(stack,equipment.slot());return 40+value*4+(value>armor(player.getItemBySlot(equipment.slot()),equipment.slot())?150:0);}
            if(stack.is(Items.WATER_BUCKET))return player.getInventory().countItem(Items.WATER_BUCKET)==0?400:80;
            if(stack.getItem() instanceof BlockItem)return player.getInventory().countItem(stack.getItem())<64?180:30;
            if(stack.has(DataComponents.FOOD))return 100+stack.get(DataComponents.FOOD).nutrition()*3;
            if(stack.is(Items.BOW)||stack.is(Items.CROSSBOW)||stack.is(Items.ARROW)||stack.is(Items.GOLDEN_APPLE))return 160;
            return 50+Math.max(0,attribute(stack,Attributes.ATTACK_DAMAGE,1,EquipmentSlot.MAINHAND))*8+Math.max(stack.getDestroySpeed(Blocks.STONE.defaultBlockState()),stack.getDestroySpeed(Blocks.OAK_LOG.defaultBlockState()))*3;
        }
        private void finishLoot(int now){player.closeContainer();if(chest!=null)visited.put(chest,now+1200);chest=null;nextGear=now;nextScan=now+20;}
        private void chooseRoam(){
            if(player.position().horizontalDistanceSqr()>18*18){goal=new Vec3(0,player.getY(),0);return;}
            double angle=level.getRandom().nextDouble()*Math.PI*2;goal=new Vec3(Math.cos(angle)*12,player.getY(),Math.sin(angle)*12);
        }
        private int inventorySlot(int index){
            var slots=player.inventoryMenu.slots;
            for(int i=0;i<slots.size();i++)if(slots.get(i).container==player.getInventory()&&slots.get(i).getContainerSlot()==index)return i;
            return -1;
        }
        private boolean select(int index){
            if(index<0||index>=36||player.containerMenu!=player.inventoryMenu)return false;
            if(index>=9){if(game.server().getTickCount()<nextInventory)return false;int slot=inventorySlot(index);if(slot<0)return false;player.inventoryMenu.clicked(slot,0,ContainerInput.SWAP,player);nextInventory=game.server().getTickCount()+5;index=0;}
            player.getInventory().setSelectedSlot(index);player.inventoryMenu.broadcastChanges();return true;
        }
        private boolean emptyHand(){
            for(int i=0;i<36;i++)if(player.getInventory().getItem(i).isEmpty())return select(i);
            return false;
        }
        private static double attribute(ItemStack item,net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> type,double base,EquipmentSlot slot){
            return item.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY).compute(type,base,slot);
        }
        private boolean enchanted(ItemStack stack,net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> kind){var enchantments=stack.getOrDefault(DataComponents.ENCHANTMENTS,net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);for(var key:enchantments.keySet())if(key.is(kind)&&enchantments.getLevel(key)>0)return true;return false;}
        private double armor(ItemStack item,EquipmentSlot slot){return attribute(item,Attributes.ARMOR,0,slot)
                +attribute(item,Attributes.ARMOR_TOUGHNESS,0,slot)*.4+Math.max(0,attribute(item,Attributes.MAX_HEALTH,0,slot))*.08;}
        private void gear(){
            for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){
                double best=armor(player.getItemBySlot(slot),slot);int chosen=-1;
                for(int i=0;i<36;i++){
                    var item=player.getInventory().getItem(i);var equipment=item.get(DataComponents.EQUIPPABLE);
                    if(item.isEmpty()||equipment==null||equipment.slot()!=slot)continue;
                    double value=armor(item,slot);if(value>best+.001){best=value;chosen=i;}
                }
                if(chosen<0||!select(chosen))continue;
                if(game.server().getTickCount()<nextInventory)return;
                int selected=player.getInventory().getSelectedSlot();int menuSlot=switch(slot){case HEAD->5;case CHEST->6;case LEGS->7;case FEET->8;default->throw new IllegalStateException();};
                // Native armor-slot pickup rules enforce binding/equipment restrictions and stack limits.
                player.inventoryMenu.clicked(menuSlot,selected,ContainerInput.SWAP,player);nextInventory=game.server().getTickCount()+5;break;
            }
            refreshTools();player.inventoryMenu.broadcastChanges();
        }
        private void refreshTools(){
            var available=new ArrayList<ItemStack>();available.add(ItemStack.EMPTY);
            for(int i=0;i<36;i++){var item=player.getInventory().getItem(i);if(!item.isEmpty())available.add(item);}
            tools=available;
        }
        private void weapon(){
            if(player.isUsingItem())return;int chosen=-1;double best=0;
            for(int i=0;i<36;i++){
                var item=player.getInventory().getItem(i);if(item.isEmpty())continue;
                double damage=attribute(item,Attributes.ATTACK_DAMAGE,1,EquipmentSlot.MAINHAND);
                double speed=Math.max(.1,attribute(item,Attributes.ATTACK_SPEED,4,EquipmentSlot.MAINHAND));
                double score=Math.max(0,damage)*Math.sqrt(speed);
                if(score>best){best=score;chosen=i;}
            }
            if(chosen>=0)select(chosen);
        }
        private boolean eat(){
            if(player.getHealth()<player.getMaxHealth()*.65)for(int i=0;i<36;i++)if(player.getInventory().getItem(i).is(Items.GOLDEN_APPLE)&&select(i))return player.gameMode.useItem(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction();
            if(player.getFoodData().getFoodLevel()>=(player.getHealth()<player.getMaxHealth()*.65?20:18)||!player.canEat(false))return false;
            int chosen=-1;int best=0;
            for(int i=0;i<36;i++){var item=player.getInventory().getItem(i);var food=item.get(DataComponents.FOOD);
                if(food!=null&&food.nutrition()>best&&item.has(DataComponents.CONSUMABLE)){chosen=i;best=food.nutrition();}}
            if(chosen<0||!select(chosen))return false;
            if(player.gameMode.useItem(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction()&&player.isUsingItem()){foods++;return true;}
            return false;
        }
        private int blockSlot(){
            int fallback=-1;
            for(int i=0;i<36;i++){
                var item=player.getInventory().getItem(i);if(item.isEmpty()||!(item.getItem() instanceof BlockItem block))continue;
                var state=block.getBlock().defaultBlockState();
                if(LbcLuckyBlocks.kind(state)!=null||block.getBlock() instanceof FallingBlock||state.hasBlockEntity()||state.is(Blocks.TNT)||state.is(Blocks.MAGMA_BLOCK)
                        ||!state.isCollisionShapeFullBlock(level,player.blockPosition()))continue;
                if(state.is(Blocks.COBBLESTONE))return i;if(fallback<0)fallback=i;
            }
            return fallback;
        }
        private void turn(Vec3 at){
            int now=game.server().getTickCount();if(turnedTick==now)return;turnedTick=now;
            Vec3 delta=at.subtract(player.getEyePosition());
            float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));float pitch=(float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance()));
            player.setYRot(Mth.approachDegrees(player.getYRot(),yaw,12));player.setYHeadRot(player.getYRot());
            player.setXRot(Mth.approach(player.getXRot(),pitch,8));player.setYBodyRot(player.getYRot());
        }
        void commit(){
            // Native sneak protects a stopped builder from walking off its last support.
            boolean unsafe=false;
            var motion=player.getDeltaMovement();
            if(player.onGround()&&!jump){
                var box=player.getBoundingBox();
                // A normal one-block step down needs no edge sneak. Keep void/bridge protection.
                // Even sub-.01 residual motion can cross the last supported
                // fraction of a block while a stopped actor turns its camera.
                var feet=new AABB(box.minX+motion.x*2,box.minY-1.125,box.minZ+motion.z*2,
                        box.maxX+motion.x*2,box.minY-.02,box.maxZ+motion.z*2);
                unsafe=level.noCollision(player,feet);
            }
            boolean sneak=!closing&&(crouch||unsafe);
            input.move(forward,side,sneak,sprint);input.jump(jump,player.isInWater());if(!jump)player.setJumping(false);
            var value=new Input(forward>.01,forward<-.01,side>.01,side<-.01,jump,sneak,sprint);
            if(!value.equals(player.getLastClientInput()))player.connection.handlePlayerInput(new net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(value));
        }
        void close(){closing=true;crafting.cancel();academy.cancel();brain.reset();halt();commit();if(player.containerMenu!=player.inventoryMenu)player.closeContainer();}
        public ServerPlayer owner(){return player;}
        public LbcItems items(){return game.items();}
        public Vec3 destination(){return goal;}
        public Vec3 lookPoint(){return enemy==null?goal.add(0,1,0):enemy.getEyePosition();}
        public void halt(){forward=side=0;jump=sprint=false;crouch=false;}
        public void crouch(boolean value){crouch=value;}
        public void steer(Vec3 point,boolean jump,Vec3 facing){
            Vec3 delta=point.subtract(player.position());double distance=delta.horizontalDistance();
            turn(facing==null?lookPoint():facing);double yaw=Math.toRadians(player.getYRot());
            double dx=distance<.035?0:delta.x/distance,dz=distance<.035?0:delta.z/distance;
            float throttle=(float)Math.min(1,distance*4);
            side=(float)(dx*Math.cos(yaw)+dz*Math.sin(yaw))*throttle;forward=(float)(-dx*Math.sin(yaw)+dz*Math.cos(yaw))*throttle;
            this.jump=jump;sprint=player.isSprinting()&&canSprint()&&distance>.8;
        }
        public boolean equip(ItemStack item){
            if(item.is(Items.COBBLESTONE))return select(blockSlot());
            for(int i=0;i<36;i++)if(player.getInventory().getItem(i)==item||ItemStack.isSameItemSameComponents(player.getInventory().getItem(i),item))return select(i);
            return item.isEmpty();
        }
        public boolean aimAction(Vec3 point){turn(point);return player.getLookAngle().dot(point.subtract(player.getEyePosition()).normalize())>=Math.cos(Math.toRadians(3));}
        public void acted(Vec3 point){actions++;}
        public int actionSerial(){return actions;}
        public void pillarJump(){halt();jump=true;}
        public boolean canSprint(){return player.getFoodData().getFoodLevel()>6&&!player.isUsingItem();}
        public boolean mayRescue(){return false;}
        public boolean hasBlocks(){return build;}
        // Two midair placements require the Chef's unrestricted aim; use ordinary finite bridges.
        public boolean mayJumpBuild(){return false;}
        public int maxGapJump(){return 4;}
        public boolean prepareJump(Vec3 at){turn(new Vec3(at.x,player.getEyeY(),at.z));var desired=at.subtract(player.position()).multiply(1,0,1).normalize();return player.getLookAngle().multiply(1,0,1).normalize().dot(desired)>.99;}
        public boolean nativeActions(){return true;}
        public List<ItemStack> nativeTools(){return tools;}
        public boolean mine(BlockPos pos){
            miningAdmission=1;
            int now=game.server().getTickCount();
            if(!level.hasChunkAt(pos)||Vec3.atCenterOf(pos).distanceToSqr(player.getEyePosition())>20.25)return false;
            // A close eye sees the upper block first. Mine that real, adjacent obstruction
            // rather than repeatedly aiming through it at the lower block.
            var obstruction=level.clip(new ClipContext(player.getEyePosition(),Vec3.atCenterOf(pos),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
            if(obstruction.getType()!=HitResult.Type.BLOCK)return false;
            if(!obstruction.getBlockPos().equals(pos)){
                var visible=obstruction.getBlockPos();
                if(visible.getX()!=pos.getX()||visible.getZ()!=pos.getZ()||Math.abs(visible.getY()-pos.getY())>1
                        ||level.getBlockState(visible).getDestroySpeed(level,visible)<0)return false;
                pos=visible.immutable();
            }
            boolean opening=LbcLuckyBlocks.kind(level.getBlockState(pos))!=null;
            var selectedState=level.getBlockState(pos);
            ItemStack best=tools.stream().filter(t->!opening||!enchanted(t,net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH)).max(Comparator.comparingDouble(t->t.getDestroySpeed(selectedState))).orElse(ItemStack.EMPTY);
            if(!equip(best)){miningAdmission=2;return true;}
            if(!aimAction(Vec3.atCenterOf(pos))){miningAdmission=3;return true;}
            var actual=LbcBotActions.ray(player,player.blockInteractionRange());
            if(actual.getType()!=HitResult.Type.BLOCK||!actual.getBlockPos().equals(pos)){miningAdmission=4;return true;}
            if(mining!=null&&now-miningTick>1)abortMining();
            miningTick=now;
            if(!pos.equals(mining)){
                abortMining();mining=pos.immutable();miningSince=now;
                var hit=level.clip(new ClipContext(player.getEyePosition(),Vec3.atCenterOf(pos),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
                if(hit.getType()!=HitResult.Type.BLOCK||!hit.getBlockPos().equals(pos)){mining=null;return false;}
                miningFace=hit.getDirection();
                player.gameMode.handleBlockBreakAction(pos,net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,miningFace,level.getMaxY(),0);
            }
            var state=level.getBlockState(pos);
            if(state.isAir()){mining=null;return true;}
            float progress=state.getDestroyProgress(player,level,pos);
            if(!(progress>0)||!Float.isFinite(progress)){miningAdmission=5;abortMining();return false;}
            miningAdmission=6;
            if(progress*(now-miningSince+1)>=1){
                player.gameMode.handleBlockBreakAction(pos,net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,miningFace,level.getMaxY(),0);
                mining=null;return true;
            }
            // A real hand can need 500 ticks for iron bars. Do not restart a valid break at tick240.
            int expected=(int)Math.min(1200,Math.ceil(1.0/progress)+40);
            if(now-miningSince>expected){abortMining();return false;}
            if(now%5==0)player.swing(InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT,true);
            return true;
        }
        public void abortMining(){
            if(mining==null)return;
            player.gameMode.handleBlockBreakAction(mining,net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,miningFace,level.getMaxY(),0);mining=null;
        }
        public boolean place(BlockPos pos,BlockHitResult hit){
            if(!select(blockSlot())){build=false;return false;}
            boolean placed=LbcBotActions.placeHeld(player,pos);
            if(placed){placements++;player.swing(InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT,true);}
            build=blockSlot()>=0;return placed;
        }
    }
}
