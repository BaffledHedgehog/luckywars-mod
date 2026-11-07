package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ChargedProjectiles;

/** Actual native bow/crossbow input, separate from the TMT coordinator checks. */
public final class LegacyRangedChecks {
    private LegacyRangedChecks(){}
    private static Rewind running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Legacy ranged: "+why);}
    public static void runRewind(GameController game){require(running==null,"exclusive converted producer rewind");var scene=new Rewind(game);running=scene;
        try{scene.begin();}catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    public static void tick(GameController game){var scene=running;if(scene==null)return;
        try{require(scene.game==game,"same converted producer controller");if(!scene.step())return;running=null;scene.close();
            SkyWars2.LOGGER.info("[check] PASS legacy-ranged-rewind (real firegun/bomb native conversion, whole coordinator discard/replay, exact accepted ammo/author/order/owner and producer links, resumed native poison contact)");
        }catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL legacy-ranged-rewind",error);}}
    private static final class Rewind implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final NativeMeleeChecks.NativeStats stats;
        final Set<UUID> initial=new HashSet<>();final List<Entity> bodies=new ArrayList<>();final List<net.minecraft.nbt.CompoundTag> sources=new ArrayList<>();
        final List<net.minecraft.world.phys.Vec3> positions=new ArrayList<>();final int started;int phase,when;boolean restoring,closed;
        Rewind(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,true);stats=new NativeMeleeChecks.NativeStats(f.owner,LbcProjectileWeaponItems.items());started=game.server().getTickCount();for(var e:f.level.getAllEntities())initial.add(e.getUUID());}
        void begin(){f.prepareParticipants();
            for(var kind:List.of(LbcProjectileWeaponItems.Kind.BLAZE_CROSSBOW,LbcProjectileWeaponItems.Kind.DILDOMET)){
                var before=snapshot(f.owner);var weapon=kind.item.getDefaultInstance();weapon.set(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.ofNonEmpty(List.of(tipped(1))));
                f.owner.setItemInHand(InteractionHand.OFF_HAND,weapon);require(f.owner.gameMode.useItem(f.owner,f.level,weapon,InteractionHand.OFF_HAND).consumesAction(),"real converted native rewind shot "+kind.id);
                var made=created(f.owner,before).stream().filter(e->!e.isRemoved()&&LbcLegacyRanged.hasAmmo(e)).toList();require(made.size()==1,"one actual converted producer "+kind.id);
                var body=made.getFirst();f.own(body);bodies.add(body);body.setNoGravity(true);body.setPos(f.home.add(bodies.size()*3,500,0));body.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            }next(0);
        }
        void next(int value){phase=value;when=game.server().getTickCount();}
        net.minecraft.nbt.CompoundTag source(Entity body){var ammo=body.getAttached(LbcLegacyRanged.ammoType());require(ammo!=null,"native body retains private accepted ammo");return ammo.save();}
        void save(){sources.clear();positions.clear();for(var body:bodies){sources.add(source(body));positions.add(body.position());}}
        void verify(){for(int i=0;i<bodies.size();i++){var body=bodies.get(i);var current=source(body);var saved=sources.get(i);
            require(!body.isRemoved()&&f.level.getEntity(body.getUUID())==body&&body.position().equals(positions.get(i)),"same native producer identity and captured position");
            require(current.getStringOr("author","").equals(f.owner.getUUID().toString())&&current.getLongOr("order",-1)==saved.getLongOr("order",-2),"exact accepted private author and causal ordering");
            var row=current.getCompoundOrEmpty("payload").getListOrEmpty("carriers").getCompoundOrEmpty(0);var old=saved.getCompoundOrEmpty("payload").getListOrEmpty("carriers").getCompoundOrEmpty(0);
            require(row.getCompoundOrEmpty("ammo").equals(old.getCompoundOrEmpty("ammo"))&&row.getCompoundOrEmpty("weapon").equals(old.getCompoundOrEmpty("weapon"))&&row.getListOrEmpty("contacts").equals(old.getListOrEmpty("contacts")),"private original ammo/weapon and exact contact ledger");
            require(LbcIndirectKills.playerSource(body)==f.owner,"native producer attribution rebinds original shooter");
        }require(game.items().kfcRewards().bombSnapshot().contains(bodies.getLast()),"restored bomb has its actual native producer link");}
        boolean step(){f.client.acknowledge();f.audience.acknowledge();require(game.server().getTickCount()-started<900,"bounded converted producer rewind phase="+phase);
            if(restoring&&game.vanity().busy())for(var player:game.server().getPlayerList().getPlayers())if(player.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            int elapsed=game.server().getTickCount()-when;
            if(phase==0){if(elapsed<3)return false;save();f.pause();require(game.vanity().requestCapture(f.owner,false),"whole coordinator accepts real converted producers");next(1);return false;}
            if(phase==1){if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;verify();f.thaw();next(2);return false;}
            if(phase==2){if(elapsed<3)return false;f.pause();for(var body:bodies)f.discard(body);require(game.vanity().requestRestoreForCheck(),"whole coordinator restores discarded converted fireball and bomb");restoring=true;next(3);return false;}
            if(phase==3){if(game.vanity().busy())return false;verify();require(game.items().timeStop().stopped(f.level),"converted producer capture restores TimeStop");game.vanity().stop();f.thaw();next(4);return false;}
            if(phase==4){if(elapsed<2)return false;
                require(game.items().kfcRewards().bombSnapshot().contains(bodies.getLast())&&!bodies.getLast().isRemoved(),"restored native bomb producer resumes without replacement");
                var fire=(SmallFireball)bodies.getFirst();var victim=cow(f.owner,fire.position().add(0,-.5,3));f.own(victim);
                fire.setPos(victim.position().add(0,.5,-2));fire.setDeltaMovement(0,0,4);fire.tick();
                require(fire.isRemoved()&&victim.hasEffect(net.minecraft.world.effect.MobEffects.POISON)&&victim.getLastHurtByMob()==f.owner,"actual restored fireball native contact resumes private poison and shooter attribution");
                var contacts=source(fire).getCompoundOrEmpty("payload").getListOrEmpty("carriers").getCompoundOrEmpty(0).getListOrEmpty("contacts");
                require(contacts.size()==1&&contacts.get(0).asString().orElse("").equals(victim.getUUID().toString()),"restored independent contact ledger advances once at real accepted collision");return true;
            }return false;
        }
        public void close(){if(closed)return;closed=true;try{game.vanity().stop();if(f.stopped)f.thaw();for(var e:com.google.common.collect.Lists.newArrayList(f.level.getAllEntities()))if(!initial.contains(e.getUUID())){f.own(e);f.discard(e);}}finally{try{stats.close();}finally{f.close();}}}
    }
    public static void run(GameController game){
        try(var f=new ElementalGrimoireChecks.Fixture(game,true);var stats=new NativeMeleeChecks.NativeStats(f.owner,LbcProjectileWeaponItems.items())){
            f.initialize();f.retain=true;
            var owned=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());
            try{
                var bow=LbcProjectileWeaponItems.Kind.DOUBLE_BOW.item.getDefaultInstance();
                f.owner.setItemInHand(InteractionHand.MAIN_HAND,bow);
                f.owner.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SPECTRAL_ARROW));
                var before=snapshot(f.owner);
                require(f.owner.gameMode.useItem(f.owner,f.owner.level(),bow,InteractionHand.MAIN_HAND).consumesAction(),"real double bow use");
                require(bow.getItem().releaseUsing(bow,f.owner.level(),f.owner,71980),"real double bow release");f.owner.stopUsingItem();
                var copies=created(f.owner,before);owned.addAll(copies);
                var arrows=copies.stream().filter(e->e instanceof AbstractArrow&&!e.isRemoved()).map(e->(AbstractArrow)e).toList();
                require(arrows.size()==2&&arrows.stream().allMatch(e->e instanceof SpectralArrow&&e.getOwner()==f.owner),"exact two native spectral copies");
                var left=net.minecraft.world.phys.Vec3.directionFromRotation(0,f.owner.getYRot()-90);
                for(var arrow:arrows){var offset=arrow.position().subtract(f.owner.getEyePosition());
                    require(Math.abs(Math.abs(offset.dot(left))-.4)<1e-6&&Math.abs(offset.dot(f.owner.getLookAngle())-.3)<1e-6,"source .4 side/.3 forward geometry");}
                require(f.owner.getOffhandItem().isEmpty(),"one native arrow debit for doubled family");
                for(var kind:List.of(LbcProjectileWeaponItems.Kind.BLAZE_CROSSBOW,LbcProjectileWeaponItems.Kind.GHAST_CROSSBOW,LbcProjectileWeaponItems.Kind.DILDOMET)){
                    f.owner.getInventory().clearContent();var weapon=kind.item.getDefaultInstance();
                    weapon.set(DataComponents.CUSTOM_NAME,Component.literal("Retain ranged witness"));weapon.setDamageValue(7);
                    var initial=new ItemStack(kind==LbcProjectileWeaponItems.Kind.BLAZE_CROSSBOW?Items.TIPPED_ARROW:Items.ARROW);
                    if(initial.is(Items.TIPPED_ARROW))initial.set(DataComponents.POTION_CONTENTS,new net.minecraft.world.item.alchemy.PotionContents(Optional.empty(),Optional.of(0x123456),
                            List.of(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,160,1)),Optional.empty()));
                    weapon.set(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.ofNonEmpty(List.of(initial)));
                    f.owner.setItemInHand(InteractionHand.OFF_HAND,weapon);
                    var selected=kind==LbcProjectileWeaponItems.Kind.GHAST_CROSSBOW?tipped(4):new ItemStack(Items.ARROW,4);f.owner.getInventory().setItem(9,selected);
                    if(kind==LbcProjectileWeaponItems.Kind.GHAST_CROSSBOW)f.owner.getInventory().setItem(10,new ItemStack(LbcImbaNativeItems.INFINITE_ARROW,4));
                    var preserved=weapon.copy();before=snapshot(f.owner);
                    require(f.owner.gameMode.useItem(f.owner,f.owner.level(),weapon,InteractionHand.OFF_HAND).consumesAction(),"native offhand "+kind.id);
                    var actors=created(f.owner,before);owned.addAll(actors);
                    require(f.owner.getOffhandItem()==weapon&&weapon.getDamageValue()==8,"same used hand/weapon/native wear "+kind.id);
                    var actual=weapon.copy();for(var s:List.of(actual,preserved)){s.remove(DataComponents.DAMAGE);s.remove(DataComponents.CHARGED_PROJECTILES);}
                    require(ItemStack.matches(actual,preserved),"all unrelated components preserved "+kind.id);
                    var charged=weapon.getOrDefault(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.EMPTY).itemCopies().toList();
                    if(kind==LbcProjectileWeaponItems.Kind.BLAZE_CROSSBOW){
                        require(actors.stream().filter(e->e instanceof SmallFireball&&!e.isRemoved()&&((Projectile)e).getOwner()==f.owner).count()==1,"one native blaze conversion");
                        require(charged.size()==1&&charged.getFirst().is(Items.ARROW)&&f.owner.getInventory().getItem(9).getCount()==4,"free source blaze next chamber");
                        var fire=(SmallFireball)actors.stream().filter(e->e instanceof SmallFireball&&!e.isRemoved()).findFirst().orElseThrow();
                        var ammo=fire.getAttached(LbcLegacyRanged.ammoType());require(ammo!=null,"converted native ammo has owned typed state");
                        // Exercise the persistent native attachment codec path before real collision.
                        fire.setAttached(LbcLegacyRanged.ammoType(),new LbcLegacyRanged.Ammo(ammo.save()));
                        var at=fire.position();var direction=fire.getDeltaMovement().normalize();
                        var victim=cow(f.owner,at.add(direction.scale(1.5)).add(0,-.5,0));owned.add(victim);
                        var neighbor=cow(f.owner,victim.position().add(1.8,0,0));owned.add(neighbor);
                        fire.tick();
                        require(victim.hasEffect(net.minecraft.world.effect.MobEffects.POISON),"native fireball accepted entity hit executes retained tipped-ammo payload after codec reconstruction");
                        require(!neighbor.hasEffect(net.minecraft.world.effect.MobEffects.POISON),"direct native hit has no invented nearby payload radius");
                        victim.discard();neighbor.discard();
                    }else if(kind==LbcProjectileWeaponItems.Kind.GHAST_CROSSBOW){
                        require(charged.size()==1,"one source ghast chamber");var visual=charged.getFirst().copy();visual.remove(DataComponents.CUSTOM_DATA);
                        require(ItemStack.matches(visual,LbcLegacyRanged.ghastRocket()),"exact ghast 3-burst flight3 next chamber visuals");
                        require(f.owner.getInventory().getItem(9).getCount()==3&&f.owner.getInventory().getItem(10).getCount()==4,"selected tipped ammo debited once; unrelated Infinite Arrow retained");
                        before=snapshot(f.owner);require(f.owner.gameMode.useItem(f.owner,f.owner.level(),weapon,InteractionHand.OFF_HAND).consumesAction(),"native reloaded ghast rocket shot");
                        var rockets=created(f.owner,before);owned.addAll(rockets);
                        var rocket=(net.minecraft.world.entity.projectile.FireworkRocketEntity)rockets.stream().filter(e->e instanceof net.minecraft.world.entity.projectile.FireworkRocketEntity&&!e.isRemoved()).findFirst().orElseThrow();
                        require(LbcLegacyRanged.hasAmmo(rocket),"actual next-chamber rocket captures selected ammo context");
                        var victim=cow(f.owner,rocket.position().add(0,-.5,3));owned.add(victim);
                        rocket.setPos(victim.position().add(0,.5,-2));rocket.setDeltaMovement(0,0,4);rocket.tick();
                        require(victim.hasEffect(net.minecraft.world.effect.MobEffects.POISON),"actual native rocket explosion executes selected tipped-ammo effect");victim.discard();
                    }else{
                        require(actors.stream().filter(e->e instanceof ItemEntity item&&ItemData.intValue(item.getItem(),"penis_bomb",0)==1&&!e.isRemoved()).count()==1,"one shared native bomb");
                        require(charged.isEmpty()&&f.owner.getInventory().countItem(Items.ARROW)==5,"one arrow refund and no invented reload");
                    }
                }
            }finally{for(var e:owned){game.items().entityUnloaded(e);e.discard();}}
        }
        SkyWars2.LOGGER.info("[check] PASS legacy-ranged (native double spectral geometry and debit; offhand blaze/ghast/bomb successful shots, exact chambers/refund and component preservation)");
    }
    private static Set<Entity> snapshot(net.minecraft.server.level.ServerPlayer owner){var result=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());owner.level().getAllEntities().forEach(result::add);return result;}
    private static net.minecraft.world.entity.animal.cow.Cow cow(net.minecraft.server.level.ServerPlayer owner,net.minecraft.world.phys.Vec3 at){
        var cow=new net.minecraft.world.entity.animal.cow.Cow(net.minecraft.world.entity.EntityTypes.COW,owner.level());cow.setPos(at);cow.setNoAi(true);cow.setNoGravity(true);
        cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);require(owner.level().addFreshEntity(cow),"native contact target");return cow;
    }
    private static ItemStack tipped(int count){var ammo=new ItemStack(Items.TIPPED_ARROW,count);ammo.set(DataComponents.POTION_CONTENTS,new net.minecraft.world.item.alchemy.PotionContents(Optional.empty(),Optional.of(0x123456),List.of(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,160,1)),Optional.empty()));return ammo;}
    private static List<Entity> created(net.minecraft.server.level.ServerPlayer owner,Set<Entity> before){var result=new ArrayList<Entity>();for(var e:owner.level().getAllEntities())if(!before.contains(e))result.add(e);return result;}
}
