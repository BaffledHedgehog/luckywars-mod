package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;

/** A real killing blow must execute the same item hit path exactly once. */
public final class LethalHitChecks {
    private LethalHitChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Lethal hits: "+why);}
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();f.move(f.owner,f.home.add(0,30,0));f.move(f.other,f.owner.position().add(3,0,0));
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcElectro.CYBERKATANA));
            int before=game.items().electro().charges(f.owner);
            for(int i=0;i<10;i++){
                var target=new Cow(EntityTypes.COW,f.level);target.setPos(f.owner.position().add(0,0,2));target.setNoAi(true);target.setNoGravity(true);target.setHealth(1);
                require(f.level.addFreshEntity(target),"native target admitted");f.own(target);
                f.other.removeAllEffects();
                require(target.hurtServer(f.level,target.damageSources().playerAttack(f.owner),100)&&target.isDeadOrDying(),"native accepted killing blow");
                require(f.other.hasEffect(MobEffects.GLOWING)&&f.other.getEffect(MobEffects.GLOWING).getAmplifier()==2,"lethal hit still emits the katana aura");
                target.discard();
            }
            require(game.items().electro().charges(f.owner)==before+1,"ten lethal hits grant exactly one charge, without duplicated dispatch");
            criterionBoundary(game,f);
        }
        SkyWars2.LOGGER.info("[check] PASS lethal-hits (ten actual native lethal hits, live-neighbor aura each time, exactly one katana charge; direct HP death cannot invent an outgoing criterion)");
    }
    private static void criterionBoundary(GameController game,WoolBucketChecks.Fixture f){
        var profile=game.players().get(f.owner.getUUID());int skill=profile.lbcSkill;
        boolean enabled=game.config().lbcSkills,ranked=game.config().ranked;
        try{
            game.config().lbcSkills=true;game.config().ranked=true;profile.lbcSkill=33;
            game.items().championSkill().clear(f.owner);
            require(game.lbc().hasSkill(f.owner,33),"actual Champion admission for criterion boundary");
            var source=f.owner.damageSources().indirectMagic(f.owner,f.owner);
            for(boolean nativeHit:new boolean[]{false,true}){
                var target=new Cow(EntityTypes.COW,f.level);target.setPos(f.owner.position().add(0,0,4));target.setNoAi(true);target.setNoGravity(true);target.setHealth(1);
                require(f.level.addFreshEntity(target),"actual criterion boundary target");f.own(target);
                if(nativeHit)require(target.hurtServer(f.level,source,100),"actual native fatal magic");
                else{
                    // The same recordDamage -> setHealth -> die sequence as direct HP removal.
                    target.getCombatTracker().recordDamage(source,100);target.setHealth(0);target.die(source);
                }
                require(target.isDeadOrDying(),"actual death in both source paths");
                var armor=f.owner.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).getModifier(LbcChampionSkill.ARMOR);
                require(nativeHit?armor!=null&&armor.amount()==20:armor==null,"only native fatal magic has the same outgoing criterion as its nonlethal counterpart");
                target.discard();
            }
        }finally{
            game.items().championSkill().clear(f.owner);profile.lbcSkill=skill;
            game.config().lbcSkills=enabled;game.config().ranked=ranked;
        }
    }
}
