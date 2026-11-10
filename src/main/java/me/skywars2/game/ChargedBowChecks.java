package me.skywars2.game;

import java.util.List;
import java.util.Optional;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Scheduled native-match evidence. Production queues advance only through their registered ticks. */
public final class ChargedBowChecks {
    private ChargedBowChecks() { }
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Charged bows: "+why);}
    public static void run(GameController game){
        require(running==null,"exclusive scheduled fixture");running=new Fixture(game);
        try{running.init();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw new IllegalStateException(error);}
    }
    public static void tick(GameController game){
        if(game==null||running==null)return;
        try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS charged-bows (source charge/range, captured tipped+spectral ammo, beam-only family, TECH/MAG, bedrock, TimeStop/typed queued ray replay, same-tick spruce conversion, actual700-sample single-tick beam and native survival use/release/debit)");}
        catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL charged-bows",error);}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ChefChecks.Fixture base;final ServerLevel level;final LbcChargedBows bows;
        final Vec3 eye;final BlockPos leaf,bedrock;
        final java.util.Map<Integer,Integer> sampleTicks=new java.util.HashMap<>();
        final java.lang.reflect.Field payloadField;final LbcChargedBows.Payloads originalPayloads;
        RegenerationBeltChecks.ScheduledClient client;Cow witness;Marker marker;
        ItemStack nativeBow,nativeAmmo;net.minecraft.world.entity.item.ItemEntity tinyDrop,bowDrop,railDrop,shishDrop;final java.util.List<net.minecraft.world.entity.item.ItemEntity> craftDrops=new java.util.ArrayList<>();int stage,when;boolean closed;
        Fixture(GameController game){
            this.game=game;base=new ChefChecks.Fixture(game);level=base.owner.level();bows=game.items().chargedBows();
            eye=Vec3.atCenterOf(base.center).add(0,3.5,-7);
            leaf=BlockPos.containing(eye.add(0,7,3));bedrock=BlockPos.containing(eye.add(0,0,2));
            require(game.items().exasperato().pendingSearches()==0&&bows.pending()==0&&game.items().stonekivi().count()==0,"exclusive shared ray/terrain queues");
            try{
                payloadField=LbcChargedBows.class.getDeclaredField("payloads");payloadField.setAccessible(true);originalPayloads=(LbcChargedBows.Payloads)payloadField.get(bows);
                payloadField.set(bows,(LbcChargedBows.Payloads)(player,weapon,family)->{
                    var delegate=originalPayloads.capture(player,weapon,family);if(delegate==null)return null;
                    return new LbcChargedBows.BeamPayload(){
                        public int variant(){return delegate.variant();}
                        public boolean hasVariant(int variant){return delegate.hasVariant(variant);}
                        public void blockContact(ServerLevel level,Vec3 point,LbcIndirectKills.Owner cause){delegate.blockContact(level,point,cause);}
                        public void sample(ServerLevel level,Vec3 point,LbcIndirectKills.Owner cause,boolean railgun){
                            if(!railgun)sampleTicks.merge(game.server().getTickCount(),1,Integer::sum);
                            delegate.sample(level,point,cause,railgun);
                        }
                        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links){return delegate.checkpoint(links);}
                    };
                });
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        }
        void next(int value){stage=value;when=game.server().getTickCount();}
        void init(){
            base.init();client=new RegenerationBeltChecks.ScheduledClient(game,base.owner);
            base.owner.getAbilities().invulnerable=true;
            base.move(base.owner,eye.add(0,-base.owner.getEyeHeight(),0));base.owner.setYRot(0);base.owner.setXRot(0);client.acknowledge();
            base.move(base.other,base.at.add(13,0,-13));
            require(LbcChargedBows.range(false,0)==0&&LbcChargedBows.range(false,19)==9&&LbcChargedBows.range(false,20)==80
                    &&LbcChargedBows.range(false,75)==300&&LbcChargedBows.range(false,10000)==300,"source Railgun charge discontinuity and cap");
            require(LbcChargedBows.range(true,23)==5&&LbcChargedBows.range(true,24)==6&&LbcChargedBows.range(true,2800)==700,"source Shishgun threshold and cap");
            require(LbcChargedBowItems.RAILGUN instanceof net.minecraft.world.item.BowItem&&LbcChargedBowItems.SHISHGUN instanceof net.minecraft.world.item.BowItem,"native bow classes");
            var weak=arrow(new ItemStack(Items.ARROW));require(bows.release(base.owner,new ItemStack(LbcChargedBowItems.SHISHGUN),23,List.of(weak)),"weak special release handled");
            require(weak.isRemoved()&&game.items().exasperato().pendingSearches()==0&&bows.pending()==0,"weak Shishgun consumes only captured carrier with no beam");
            witness=base.cow(eye.add(0,-.5,2));witness.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(10000);witness.setHealth(10000);
            tipped();next(0);
        }
        ItemStack ammo(){
            var stack=new ItemStack(Items.TIPPED_ARROW);
            stack.set(DataComponents.POTION_CONTENTS,new PotionContents(Optional.empty(),Optional.of(6710882),
                    List.of(new MobEffectInstance(MobEffects.SPEED,160,2),new MobEffectInstance(MobEffects.INVISIBILITY,240)),Optional.empty()));
            stack.set(DataComponents.POTION_DURATION_SCALE,.25f);return stack;
        }
        Arrow arrow(ItemStack ammo){var arrow=new Arrow(level,base.owner,ammo,new ItemStack(LbcChargedBowItems.RAILGUN));arrow.setPos(eye);arrow.setDeltaMovement(0,0,3);require(level.addFreshEntity(arrow),"real accepted native family member");return arrow;}
        void tipped(){
            witness.removeAllEffects();var tipped=arrow(ammo());tipped.setRemainingFireTicks(60);
            var spectral=new SpectralArrow(level,base.owner,new ItemStack(Items.SPECTRAL_ARROW),new ItemStack(LbcChargedBowItems.RAILGUN));
            spectral.setPos(eye);spectral.setDeltaMovement(0,0,3);require(level.addFreshEntity(spectral),"real spectral family member");
            require(bows.release(base.owner,new ItemStack(LbcChargedBowItems.RAILGUN),0,List.of(tipped,spectral)),"zero-range Railgun still submits one sample");
            require(tipped.isRemoved()&&spectral.isRemoved()&&game.items().exasperato().pendingSearches()==1,"exact family retired and one shared beam queued");
        }
        Marker jammer(String tag){var actor=new Marker(EntityTypes.MARKER,level);actor.setPos(base.owner.position());actor.entityTags().add(tag);require(level.addFreshEntity(actor),"native positive jammer");game.items().magic().loaded(actor);return actor;}
        void removeMarker(){if(marker!=null){game.items().magic().unloaded(marker);marker.discard();marker=null;}}
        boolean idle(){return game.items().exasperato().pendingSearches()==0&&bows.pending()==0;}
        int arrows(){return level.getEntitiesOfClass(AbstractArrow.class,new AABB(base.center).inflate(16),a->a.getOwner()==base.owner&&!a.isRemoved()).size();}
        boolean step(){
            int elapsed=game.server().getTickCount()-when;require(elapsed<=500,"bounded native phase "+stage+" rays="+game.items().exasperato().pendingSearches()+" terrain="+bows.pending());
            switch(stage){
                case 0->{
                    if(game.items().exasperato().pendingSearches()!=0)return false;
                    require(witness.hasEffect(MobEffects.SPEED)&&witness.getEffect(MobEffects.SPEED).getAmplifier()==2,"all tipped effects retain amplifier");
                    require(witness.getEffect(MobEffects.SPEED).getDuration()<=40&&witness.hasEffect(MobEffects.INVISIBILITY),"all tipped effects preserve original duration scale");
                    require(witness.hasEffect(MobEffects.GLOWING)&&witness.isOnFire(),"spectral and Flame carried by same beam");require(arrows()==0,"no extra normal arrow");
                    marker=jammer("stopper");require(game.items().magic().mechanismSuppressed(base.owner),"positive TECH jammer indexed");
                    var blocked=arrow(ammo());bows.release(base.owner,new ItemStack(LbcChargedBowItems.RAILGUN),20,List.of(blocked));
                    require(blocked.isRemoved()&&game.items().exasperato().pendingSearches()==0,"TECH suppresses Railgun without ordinary arrow");removeMarker();
                    marker=jammer("stopper_magic");require(game.items().magic().suppressed(base.owner),"positive MAG jammer indexed");
                    var shish=arrow(ammo());bows.release(base.owner,new ItemStack(LbcChargedBowItems.SHISHGUN),24,List.of(shish));
                    require(shish.isRemoved()&&game.items().exasperato().pendingSearches()==0,"MAG suppresses Shishgun");tipped();next(1);
                }
                case 1->{
                    if(game.items().exasperato().pendingSearches()!=0)return false;
                    require(witness.hasEffect(MobEffects.SPEED),"MAG alone does not suppress Railgun");removeMarker();
                    base.remove(witness);witness=null;base.block(bedrock,Blocks.STONE);
                    var carrier=arrow(ammo());game.items().trinketArrows().mark(carrier,LbcTrinketArrows.BEDROCK);
                    bows.release(base.owner,new ItemStack(LbcChargedBowItems.RAILGUN),0,List.of(carrier));next(2);
                }
                case 2->{
                    if(game.items().exasperato().pendingSearches()!=0)return false;
                    require(level.getBlockState(bedrock).is(Blocks.BEDROCK),"source bedrock trait is carried by beam contact");
                    // Remove the fixture's bedrock trait cube before testing ordinary TNT terrain.
                    for(var pos:BlockPos.betweenClosed(bedrock.offset(-1,-1,-1),bedrock.offset(1,1,1)))base.block(pos,Blocks.AIR);
                    base.block(leaf,Blocks.STONE);sampleTicks.clear();
                    var shish=arrow(new ItemStack(Items.ARROW));bows.release(base.owner,new ItemStack(LbcChargedBowItems.SHISHGUN),24,List.of(shish));
                    game.items().timeStop().begin(level,100);next(3);
                }
                case 3->{
                    require(game.items().exasperato().pendingSearches()==1&&bows.pending()==0,"TimeStop holds queued beam before its first native sample");
                    if(elapsed>=5){
                        var graph=new MatchQueuedWorkCheckpoint.Graph(new MatchDamageSources(game,List.of(base.owner,base.other)));
                        var rays=game.items().exasperato().checkpointSearches(graph.links);
                        require(graph.links.resolve(id->game.server().getPlayerList().getPlayer(id)).isEmpty(),"typed instant ray author/payload links resolve");
                        var graphPlan=graph.prepare();var rayPlan=rays.prepare(graph.links);
                        bows.prepareFrozen();game.items().exasperato().prepareSearchCheckpoint();graphPlan.run();rayPlan.run();
                        require(game.items().exasperato().pendingSearches()==1&&bows.pending()==0,"nonempty typed queued instant ray restored before first sample");
                        game.items().timeStop().end(level);next(4);
                    }
                }
                case 4->{
                    if(game.items().exasperato().pendingSearches()!=0)return false;
                    require(bows.pending()==0&&sampleTicks.size()==1&&sampleTicks.values().iterator().next()==6,"six source samples and foliage complete in one native tick, no delayed leaf children");
                    require(level.getBlockState(leaf).is(Blocks.SPRUCE_LEAVES),"real stone outside the blast core becomes spruce leaves before completion");
                    sampleTicks.clear();base.owner.setXRot(-90);var longShot=arrow(new ItemStack(Items.ARROW));
                    bows.release(base.owner,new ItemStack(LbcChargedBowItems.SHISHGUN),2800,List.of(longShot));next(41);
                }
                case 41->{
                    if(game.items().exasperato().pendingSearches()!=0){require(sampleTicks.isEmpty(),"preflight may wait for chunks, but never publishes a partial long shot");return false;}
                    require(sampleTicks.size()==1&&sampleTicks.values().iterator().next()==700&&bows.pending()==0,"maximum700-block beam completes in a single actual native tick");
                    base.owner.setXRot(0);next(5);
                }
                case 5->{
                    if(!idle())return false;
                    nativeBow=new ItemStack(LbcChargedBowItems.SHISHGUN);nativeAmmo=new ItemStack(Items.ARROW,8);
                    base.owner.setItemInHand(InteractionHand.MAIN_HAND,nativeBow);base.owner.setItemInHand(InteractionHand.OFF_HAND,nativeAmmo);
                    require(base.owner.gameMode.useItem(base.owner,level,nativeBow,InteractionHand.MAIN_HAND).consumesAction()&&base.owner.isUsingItem(),"real survival native BowItem input admitted");next(6);
                }
                case 6->{
                    if(elapsed<24)return false;
                    require(base.owner.isUsingItem()&&base.owner.getTicksUsingItem()>=24,"registered native ticks charged bow past source threshold");
                    base.owner.releaseUsingItem();require(!base.owner.isUsingItem()&&nativeBow.getDamageValue()==1,"real native release has one durability debit");
                    require(nativeAmmo.getCount()==8&&arrows()==0,"source Shishgun Infinity conserves ordinary ammo with no ballistic residue");
                    require(game.items().exasperato().pendingSearches()>0,"real native shoot callback dispatched charged beam");next(60);
                }
                case 60->{
                    if(!idle())return false;
                    nativeBow.set(DataComponents.ENCHANTMENTS,net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
                    require(base.owner.gameMode.useItem(base.owner,level,nativeBow,InteractionHand.MAIN_HAND).consumesAction()&&base.owner.isUsingItem(),"modified native bow without Infinity starts a second real charge");next(61);
                }
                case 61->{
                    if(elapsed<24)return false;
                    require(base.owner.isUsingItem()&&base.owner.getTicksUsingItem()>=24,"second registered native charge passes source threshold");
                    base.owner.releaseUsingItem();
                    require(!base.owner.isUsingItem()&&nativeBow.getDamageValue()==2&&nativeAmmo.getCount()==7&&arrows()==0,"removing Infinity restores exactly one paid ammo debit and native durability");
                    require(game.items().exasperato().pendingSearches()>0,"paid native release still dispatches the same charged beam");next(7);
                }
                case 7->{
                    if(!idle())return false;
                    var at=Vec3.atCenterOf(base.center).add(10,3,10);
                    require(LbcChargedBowCrafting.kind(new ItemStack(LbcWands.TINY_ELECTRO),ItemStack.EMPTY)==null,"missing actual bow cannot craft");
                    tinyDrop=craftDrop(at,new ItemStack(LbcWands.TINY_ELECTRO));bowDrop=craftDrop(at.add(.25,0,0),new ItemStack(Items.BOW));next(8);
                }
                case 8->{
                    railDrop=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(tinyDrop.position(),tinyDrop.position()).inflate(2),e->!e.isRemoved()&&e.getItem().is(LbcChargedBowItems.RAILGUN)).stream().findFirst().orElse(null);
                    if(railDrop==null){require(elapsed<60,"native dropped Railgun recipe completes");return false;}
                    craftDrops.add(railDrop);require(bowDrop.isRemoved(),"exact one real ground bow spent");
                    if(!tinyDrop.isRemoved())tinyDrop.discard();railDrop.setNoGravity(true);railDrop.setDeltaMovement(Vec3.ZERO);
                    var leaves=game.lbc().rollTable(level,railDrop.position(),base.owner,"lbc:ported/reactive_shish");require(leaves.size()==1,"canonical Reactive Shish");
                    shishDrop=craftDrop(railDrop.position().add(.25,0,0),leaves.getFirst());next(9);
                }
                case 9->{
                    var output=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(railDrop.position(),railDrop.position()).inflate(2),e->!e.isRemoved()&&e.getItem().is(LbcChargedBowItems.SHISHGUN)).stream().findFirst().orElse(null);
                    if(output==null){require(elapsed<60,"native dropped Shishgun recipe completes");return false;}
                    craftDrops.add(output);require(railDrop.isRemoved()&&shishDrop.isRemoved(),"one Railgun plus one Reactive Shish spent");return true;
                }
                default->throw new IllegalStateException("Charged bow fixture stage");
            }
            return false;
        }
        net.minecraft.world.entity.item.ItemEntity craftDrop(Vec3 at,ItemStack stack){
            var item=new net.minecraft.world.entity.item.ItemEntity(level,at.x,at.y,at.z,stack.copy());item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);item.setDefaultPickUpDelay();
            require(level.addFreshEntity(item),"real dropped craft input admitted");craftDrops.add(item);return item;
        }
        @Override public void close(){
            if(closed)return;closed=true;for(var item:craftDrops)item.discard();Throwable failure=null;
            for(Runnable action:List.<Runnable>of(()->base.owner.stopUsingItem(),()->game.items().timeStop().end(level),bows::stop,
                    this::removeMarker,()->{try{payloadField.set(bows,originalPayloads);}catch(IllegalAccessException error){throw new IllegalStateException(error);}},()->{if(client!=null)client.close();},base::close))try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("Charged bow fixture restoration",failure);
        }
    }
}
