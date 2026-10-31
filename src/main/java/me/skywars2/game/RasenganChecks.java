package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** Actual native uses/blasts, inside the existing exact terrain/player restoration fixture. */
public final class RasenganChecks {
    private RasenganChecks(){}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException("rasengan: "+message);}
    static ItemStack token(int count){
        var item=new ItemStack(Items.SNOWBALL,count);var data=new CompoundTag();data.putInt("gadget",1);data.putInt("rasengan",1);
        item.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return item;
    }
    private static void geometry(){
        require(!LbcRasengan.matches(new ItemStack(Items.SNOWBALL))&&LbcRasengan.matches(token(1)),"both raw custom flags required");
        var missing=token(1);var data=missing.get(DataComponents.CUSTOM_DATA).copyTag();data.remove("gadget");missing.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
        require(!LbcRasengan.matches(missing),"unrelated rasengan marker cannot bypass gadget dispatcher");
    }
    public static void run(GameController game){
        geometry();var service=game.items().rasengan();require(service.pendingCount()==0,"no unrelated pending throw");
        try(var f=new ChefChecks.Fixture(game);
                var attribution=new SauvojenMeditationChecks.Attribution(game,List.of(f.owner,f.other))){
            f.init();f.move(f.owner,f.at);f.move(f.other,f.at.add(0,0,-3));
            Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());f.owner.level().getAllEntities().forEach(before::add);
            try{
                var blastAt=f.at.add(0,6,0);var impulseWitness=f.cow(blastAt.add(1,0,0));
                impulseWitness.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
                Vec3 baseline=null;float baselineDamage=0;
                for(int cell=0;cell<26;cell++){
                    impulseWitness.setHealth(200);impulseWitness.setDeltaMovement(Vec3.ZERO);impulseWitness.setInvulnerableTime(0);impulseWitness.damageCooldownTime=0;
                    var calculator=LbcRasengan.blastCalculator(cell);int multiplier=cell<8?1:cell<11?2:6;
                    require(calculator.getKnockbackMultiplier(impulseWitness)==multiplier,"exact native knockback multiplier at cell "+cell);
                    f.owner.level().explode(null,impulseWitness.damageSources().explosion(null,null),calculator,blastAt,1,false,net.minecraft.world.level.Level.ExplosionInteraction.MOB);
                    var actual=impulseWitness.getDeltaMovement();float damage=200-impulseWitness.getHealth();
                    if(cell==0){baseline=actual;baselineDamage=damage;require(baseline.lengthSqr()>0&&damage>0,"actual native explosion impulse/damage baseline");}
                    require(actual.distanceToSqr(baseline.scale(multiplier))<1e-10,"native explosion applies exact source multiplicity to impulse "+cell);
                    require(Math.abs(damage-baselineDamage)<1e-5,"native damage remains single power1 hit at cell "+cell);
                }
                f.remove(impulseWitness);
                for(var hand:InteractionHand.values()){
                    f.owner.removeAllEffects();f.other.removeAllEffects();f.owner.setDeltaMovement(Vec3.ZERO);
                    f.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);f.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
                    var stack=token(2);f.owner.setItemInHand(hand,stack);int stat=f.owner.getStats().getValue(Stats.ITEM_USED.get(Items.SNOWBALL));
                    require(f.owner.gameMode.useItem(f.owner,f.owner.level(),stack,hand).consumesAction(),"actual native use "+hand);
                    require(stack.getCount()==1&&f.owner.getStats().getValue(Stats.ITEM_USED.get(Items.SNOWBALL))==stat+1,"native one-item debit/stat "+hand);
                    require(service.pendingCount()==1,"actual entity-load enqueues exactly one cast");
                    var ball=f.owner.level().getEntitiesOfClass(Snowball.class,f.owner.getBoundingBox().inflate(4)).stream()
                            .filter(e->!before.contains(e)&&LbcRasengan.matches(e.getItem())).findFirst().orElseThrow();
                    var cow=f.cow(f.at.add(0,1.62,5));cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);cow.setHealth(200);
                    var protectedCow=f.cow(f.at.add(.3,1.62,5));protectedCow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);protectedCow.setHealth(200);protectedCow.entityTags().add("strict_map_object");
                    // Throw entity can be far away: source geometry still belongs to caster eyes/rotation.
                    ball.setPos(f.at.add(12,0,0));service.tick();
                    require(service.pendingCount()==0&&!ball.isRemoved(),"one spell admission retains native source snowball");
                    require(cow.getHealth()<200&&protectedCow.getHealth()==200,"native far-cell MOB explosions hurt living target and preserve protected role: hp="+cow.getHealth()+" protected="+protectedCow.getHealth()+" pos="+f.owner.position()+" look="+f.owner.getLookAngle());
                    require(f.owner.getEffect(MobEffects.RESISTANCE)!=null&&f.owner.getEffect(MobEffects.RESISTANCE).getAmplifier()==4
                            &&f.owner.getEffect(MobEffects.RESISTANCE).getDuration()==20,"source caster ResistanceV20");
                    require(f.other.getEffect(MobEffects.RESISTANCE)!=null&&f.other.getEffect(MobEffects.RESISTANCE).getAmplifier()==3
                            &&f.other.getEffect(MobEffects.RESISTANCE).getDuration()==20,"source nearby player ResistanceIV20");
                    require(f.owner.getEffect(MobEffects.LEVITATION)!=null&&f.owner.getEffect(MobEffects.LEVITATION).getDuration()==20
                            &&f.owner.getEffect(MobEffects.SLOW_FALLING)!=null&&f.owner.getEffect(MobEffects.SLOW_FALLING).getDuration()==200,"native launch/slow-fall clocks");
                    float after=cow.getHealth();service.tick();require(cow.getHealth()==after,"retained ball cannot retrigger");
                    f.remove(cow);f.remove(protectedCow);ball.discard();
                }
                f.owner.removeAllEffects();
                var frozen=new Snowball(f.owner.level(),f.owner,token(1));frozen.setPos(f.at.add(0,1.62,0));
                frozen.entityTags().add("time_stopped");require(f.owner.level().addFreshEntity(frozen),"native frozen throw load");
                service.tick();require(service.pendingCount()==1&&f.owner.getEffect(MobEffects.LEVITATION)==null,"shared Time Stop holds uncommitted cast");
                service.forget(f.owner);require(service.pendingCount()==0&&!frozen.isRemoved(),"logout cancels uncommitted spell without removing native ball");frozen.discard();
                var cancelled=new Snowball(f.owner.level(),f.owner,token(1));cancelled.setPos(f.at.add(0,1.62,0));
                require(f.owner.level().addFreshEntity(cancelled)&&service.pendingCount()==1,"another actual pending throw");
                service.stop();require(service.pendingCount()==0&&!cancelled.isRemoved(),"match/Vanity cancellation owns pending work only");cancelled.discard();
                var marker=new Marker(EntityTypes.MARKER,f.owner.level());marker.entityTags().add("stopper_magic");
                marker.setPos(f.at.add(0,0,24));require(f.owner.level().addFreshEntity(marker),"native registered magic stopper");
                require(game.items().magic().rasenganSuppressed(f.owner.level(),f.owner.position()),"source spherical magic stopper includes24");
                marker.setPos(f.at.add(0,0,24.001));
                require(!game.items().magic().rasenganSuppressed(f.owner.level(),f.owner.position()),"source magic stopper excludes beyond24");
                marker.setPos(f.at.add(0,0,24));f.owner.removeAllEffects();
                var blocked=new Snowball(f.owner.level(),f.owner,token(1));blocked.setPos(f.at.add(0,1.62,0));
                require(f.owner.level().addFreshEntity(blocked),"native suppressed source load");service.tick();
                require(service.pendingCount()==0&&!blocked.isRemoved()&&f.owner.getEffect(MobEffects.LEVITATION)==null,"suppressed spell ends without deleting native ball");
                game.items().magic().unloaded(marker);marker.discard();blocked.discard();
                var mystical=new ItemStack(Items.REPEATING_COMMAND_BLOCK);var mysticalData=new CompoundTag();mysticalData.putInt("mystical",1);mysticalData.putInt("type",2);
                mystical.set(DataComponents.CUSTOM_DATA,CustomData.of(mysticalData));f.other.getInventory().setItem(0,mystical);f.move(f.other,f.at.add(0,0,12));
                require(game.items().magic().rasenganSuppressed(f.owner.level(),f.owner.position()),"raw inventory mystical2 includes12 before aura activation");
                f.move(f.other,f.at.add(0,0,12.001));
                require(!game.items().magic().rasenganSuppressed(f.owner.level(),f.owner.position()),"raw mystical inventory excludes beyond12");f.other.getInventory().clearContent();
            }finally{
                service.stop();var cleanup=new java.util.ArrayList<Entity>();
                f.owner.level().getAllEntities().forEach(e->{if(e!=null)cleanup.add(e);});
                for(var e:cleanup)if(!before.contains(e)&&!(e instanceof net.minecraft.server.level.ServerPlayer)){
                    if(e instanceof net.minecraft.world.entity.LivingEntity living)game.items().indirectKills().clear(living);e.discard();
                }
            }
        }
        SkyWars2.LOGGER.info("[check] PASS rasengan (both actual native hands/stat/debit,26 native cells with exact x1/x2/x6 native impulses and single-hit power1 damage, caster geometry, native MOB damage/protected role, resistance/launch clocks, retained physical snowball, once-only dispatch, source suppression boundaries, shared stasis and logout/stop cancellation; exact fixture restoration)");
    }
}
