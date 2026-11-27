package me.skywars2.game;

import me.skywars2.SkyWars2;
import me.skywars2.game.combat.WeaponEnchantments;
import me.skywars2.game.items.wands.LbcExasperato;
import me.skywars2.game.items.wands.LbcFevgaleaStar;
import me.skywars2.game.items.wands.LbcWands;
import me.skywars2.game.items.wands.LbcSauvojen;
import me.skywars2.game.items.wands.SauvojenStars;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Native damage endpoints after a hand swap; controller steps are explicitly advanced by the fixture. */
public final class DelegatedWandPowerChecks {
    private DelegatedWandPowerChecks(){}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException("Delegated wand Power: "+message);}
    private static void near(float actual,float expected,String message){require(Math.abs(actual-expected)<.002f,message+" expected="+expected+" actual="+actual);}
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();
            require(game.items().blaze().count()==0&&game.items().iceWand().count()==0
                    &&game.items().exasperato().pendingSearches()==0,"empty owned controller scene");
            Vec3 origin=f.home.add(0,18,0);
            for(var cell:BlockPos.betweenClosed(BlockPos.containing(origin).offset(-3,-2,-3),BlockPos.containing(origin).offset(8,4,3)))f.set(cell,Blocks.AIR.defaultBlockState());
            f.move(f.owner,origin.add(-12,0,0));f.move(f.other,origin.add(-12,0,5));
            var victim=new Witness(f.level);victim.setPos(origin.add(4,0,0));victim.setNoAi(true);victim.setNoGravity(true);
            victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);victim.getAttribute(Attributes.ARMOR).setBaseValue(0);victim.setHealth(1000);
            require(f.level.addFreshEntity(victim),"native hostile witness");f.own(victim);
            try{
                ice(f,victim,origin);
                queuedBlast(f,victim,origin);
                blaze(f,victim,origin);
                stars(f,victim,origin);
                fevgalea(f,victim,origin);
                erecto(f,victim,origin);
                keeperThorns(f,victim,origin);
            }finally{
                game.items().iceWand().stop();game.items().blaze().stop();game.items().exasperato().stop();game.items().sauvojen().stars().stop();
            }
        }
        SkyWars2.LOGGER.info("[check] PASS delegated-wand-power (actual Ice impact/crystal, queued native explosion, Blaze-native melee/fireball/controller change; explicit manual controller steps)");
    }
    private static ItemStack wand(WoolBucketChecks.Fixture f,String id,boolean powered){
        var stack=id.equals("blaze_wand")?new ItemStack(LbcWands.BLAZE_WAND):f.leaf(id);
        if(powered){var registry=f.owner.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            stack.enchant(registry.getOrThrow(Enchantments.POWER),1);stack.enchant(registry.getOrThrow(Enchantments.SHARPNESS),5);}
        return stack;
    }
    private static void reset(GameController game,LivingEntity victim){
        game.items().frost().clear(victim);victim.removeAllEffects();victim.clearFire();victim.setHealth(1000);
        victim.setAbsorptionAmount(0);victim.setInvulnerableTime(0);victim.damageCooldownTime=0;victim.setDeltaMovement(Vec3.ZERO);
    }
    private static void ice(WoolBucketChecks.Fixture f,Zombie victim,Vec3 origin){
        var game=f.game;var service=game.items().iceWand();var point=origin.add(0,.5,0);
        var cell=BlockPos.containing(point);f.set(cell.below(),Blocks.STONE.defaultBlockState());
        victim.setPos(point.add(1,0,0));reset(game,victim);
        var stack=wand(f,"ice_wand",true);
        me.skywars2.game.items.wands.LbcIceWand.IceShot shot;
        try(var use=WeaponEnchantments.using(f.owner,stack)){shot=service.spawn(f.owner,InteractionHand.MAIN_HAND,stack);}
        f.own(shot);f.own(shot.display);f.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIRT));
        service.impact(shot,point);near(victim.getHealth(),992.5f,"Ice impact5 gets Power I once and no Sharpness after hand swap");
        require(service.snapshot().size()==1,"the actual impact publishes one delayed crystal");
        var crystal=service.snapshot().getFirst();f.own(crystal.display);f.own(crystal.hitbox);
        reset(game,victim);require(service.attack(f.owner,crystal.hitbox),"actual crystal attack accepted");
        near(victim.getHealth(),992.5f,"crystal5 retains original Power I after the shot is removed");
        service.stop();reset(game,victim);
    }
    private static void queuedBlast(WoolBucketChecks.Fixture f,Zombie victim,Vec3 origin){
        var game=f.game;victim.setPos(origin.add(4,0,0));
        float plain=queuedBlastDamage(f,victim,origin,wand(f,"air_wand",false));
        float powered=queuedBlastDamage(f,victim,origin,wand(f,"air_wand",true));
        require(plain>0,"native queued explosion baseline deals damage");
        near(powered,plain*1.5f,"Search completion's native explosion retains Power only across a hand swap");
        reset(game,victim);
    }
    private static float queuedBlastDamage(WoolBucketChecks.Fixture f,Zombie victim,Vec3 origin,ItemStack stack){
        reset(f.game,victim);var completed=new boolean[1];
        try(var use=WeaponEnchantments.using(f.owner,stack)){
            f.game.items().exasperato().replayableSpell(f.owner,origin.add(0,.8,0),new Vec3(1,0,0),8,new LbcExasperato.ReplayableRay(){
                public boolean block(BlockPos pos,BlockState state){return !state.isAir();}
                public boolean target(Entity entity){return entity==victim;}
                public void trail(Vec3 from,Vec3 to){}
                public void complete(HitResult hit){require(hit!=null,"queued ray actually hit native witness");completed[0]=true;
                    f.game.items().exasperato().protectedBlast(f.owner,f.level,hit.getLocation(),1,ignored->{});}
            });
        }
        f.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIRT));
        for(int i=0;i<8&&!completed[0];i++)f.game.items().exasperato().tickSearchesWhileMapStopped();
        require(completed[0]&&f.game.items().exasperato().pendingSearches()==0,"bounded real queued terminal");
        return 1000-victim.getHealth();
    }
    private static void blaze(WoolBucketChecks.Fixture f,Zombie victim,Vec3 origin){
        victim.setPos(origin.add(4,0,0));
        float plain=blazeDamage(f,victim,origin,false,false,false);
        float powered=blazeDamage(f,victim,origin,true,false,false);
        near(powered,plain*1.5f,"native Blaze-owned SmallFireball retains its summoning wand after hand swap");
        require(plain>0,"native Blaze fireball baseline");
        float reflected=blazeDamage(f,victim,origin,true,true,false);
        near(reflected,plain,"changing the physical fireball controller cannot borrow its former owner's Power");
        plain=blazeDamage(f,victim,origin,false,false,true);
        powered=blazeDamage(f,victim,origin,true,false,true);
        require(plain>0,"native Blaze melee baseline");near(powered,plain*1.5f,"native summoned Blaze melee retains only original wand Power");
        reset(f.game,victim);f.owner.setItemInHand(InteractionHand.MAIN_HAND,wand(f,"blaze_wand",true));
        var ordinary=new SmallFireball(EntityTypes.SMALL_FIREBALL,f.level);ordinary.setOwner(f.owner);ordinary.setPos(origin.add(0,.8,0));ordinary.setDeltaMovement(1.6,0,0);
        require(f.level.addFreshEntity(ordinary),"ordinary unmarked native fireball");f.own(ordinary);
        for(int i=0;i<5&&!ordinary.isRemoved()&&victim.getHealth()==1000;i++)ordinary.tick();
        near(1000-victim.getHealth(),5,"unmarked ordinary fireball does not borrow held wand Power");
        nativeBurn(victim,1,"ordinary fireball native burn remains unchanged");ordinary.discard();
    }
    private static float blazeDamage(WoolBucketChecks.Fixture f,Zombie victim,Vec3 origin,boolean powered,boolean reflected,boolean melee){
        reset(f.game,victim);var before=f.level.getEntitiesOfClass(Blaze.class,new AABB(origin,origin).inflate(32)).stream().map(Entity::getUUID).collect(java.util.stream.Collectors.toSet());
        try(var use=WeaponEnchantments.using(f.owner,wand(f,"blaze_wand",powered))){f.game.items().blaze().summonFriend(f.owner);}
        var bodies=f.level.getEntitiesOfClass(Blaze.class,new AABB(origin,origin).inflate(32),body->!before.contains(body.getUUID()));
        require(bodies.size()==1,"actual owned native Blaze publication");var body=bodies.getFirst();f.own(body);body.setNoAi(true);body.setNoGravity(true);body.setPos(origin.add(0,.8,0));
        f.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIRT));
        if(melee)require(body.doHurtTarget(f.level,victim),"actual native Blaze melee accepted");
        else{
            var shot=new SmallFireball(EntityTypes.SMALL_FIREBALL,f.level);shot.setOwner(body);shot.setPos(origin.add(0,.8,0));shot.setDeltaMovement(1.6,0,0);
            require(f.level.addFreshEntity(shot),"native Blaze fireball publication");f.own(shot);
            require(shot.getOwner()==body,"physical Blaze controller is preserved");
            if(reflected)shot.setOwner(f.other);
            for(int i=0;i<5&&!shot.isRemoved()&&victim.getHealth()==1000;i++)shot.tick();
            shot.discard();
        }
        float amount=1000-victim.getHealth();
        if(!melee)nativeBurn(victim,powered&&!reflected?1.5f:1,"actual native ON_FIRE tick retains only accepted original Power");
        body.discard();return amount;
    }
    private static void nativeBurn(LivingEntity victim,float expected,String label){
        require(victim.isOnFire(),"accepted native fireball actually ignited its target");
        victim.setInvulnerableTime(0);victim.damageCooldownTime=0;victim.setRemainingFireTicks(20);
        float before=victim.getHealth();victim.baseTick();near(before-victim.getHealth(),expected,label);
    }
    private static void fevgalea(WoolBucketChecks.Fixture f,Zombie victim,Vec3 origin){
        reset(f.game,victim);victim.setPos(origin.add(4,0,0));
        var registry=f.owner.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var plain=new ItemStack(LbcFevgaleaStar.SWORD);plain.enchant(registry.getOrThrow(Enchantments.SHARPNESS),5);
        var powered=plain.copy();powered.enchant(registry.getOrThrow(Enchantments.POWER),1);
        require(!me.skywars2.game.combat.EnchantmentRules.spellSource(powered),"physical Fevgalea sword remains outside spell classification");
        var source=f.level.damageSources().playerAttack(f.owner);
        float normal=net.minecraft.world.item.enchantment.EnchantmentHelper.modifyDamage(f.level,plain,victim,source,4);
        float withPower=net.minecraft.world.item.enchantment.EnchantmentHelper.modifyDamage(f.level,powered,victim,source,4);
        require(normal>4,"real sword retains native Sharpness");near(withPower,normal,"Power does not modify physical sword melee preprocessing");
        f.owner.setItemInHand(InteractionHand.MAIN_HAND,powered);f.owner.setShiftKeyDown(true);
        var counter=f.game.items().mana().score(f.owner,"fevgalea_charge");int before=counter.get();counter.set(7);
        try{
            require(f.game.items().fevgaleaStar().use(f.owner,InteractionHand.MAIN_HAND),"actual explicit Fevgalea half-health ability admitted");
            near(victim.getHealth(),250,"half-health ability500 gets Power I only, without native Sharpness");
            require(f.owner.getMainHandItem()==powered&&!me.skywars2.game.combat.EnchantmentRules.spellSource(powered),"ability projection never mutates the physical sword");
        }finally{counter.set(before);f.owner.setShiftKeyDown(false);f.game.items().sauvojen().stars().stop();}
    }
    private static final class Witness extends Zombie {
        int starHits,arcHits;
        Witness(ServerLevel level){super(EntityTypes.ZOMBIE,level);}
        @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){
            boolean accepted=super.hurtServer(level,source,amount);
            if(accepted&&source.getDirectEntity()!=null&&source.getDirectEntity().entityTags().contains("star_raindrop"))starHits++;
            if(accepted&&source.is(me.skywars2.game.combat.LbcDamage.ELECTRO_BOLT))arcHits++;
            return accepted;
        }
    }
    private static void stars(WoolBucketChecks.Fixture f,Witness victim,Vec3 origin){
        var stars=f.game.items().sauvojen().stars();require(stars.count()==0,"empty stars scene");reset(f.game,victim);victim.starHits=0;
        var stack=new ItemStack(LbcWands.ACADEMY_SAUVOJEN);var registry=f.owner.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        stack.enchant(registry.getOrThrow(Enchantments.POWER),1);stack.enchant(registry.getOrThrow(Enchantments.SHARPNESS),5);
        try(var use=WeaponEnchantments.using(f.owner,stack)){
            f.game.items().sauvojen().emitAcademy(f.owner,InteractionHand.MAIN_HAND,stack,LbcSauvojen.Kind.TERRA,6,origin,new Vec3(0,1,0));
        }
        f.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIRT));
        for(int i=0;i<64&&stars.snapshot().stream().noneMatch(view->view.kind()==SauvojenStars.Kind.DROP);i++)stars.tick();
        var child=stars.snapshot().stream().filter(view->view.kind()==SauvojenStars.Kind.DROP).findFirst().orElseThrow(()->new IllegalStateException("native rain did not publish a child within64 controller steps"));
        for(var actor:stars.snapshot()){f.own(actor.carrier());if(actor.kind()==SauvojenStars.Kind.RAIN)actor.carrier().discard();}
        // Select a real child contact without depending on random rain direction or world travel distance.
        child.carrier().setPos(victim.position().add(-.3,0,-.3));stars.tick();
        require(victim.starHits>0,"actual child native damage endpoint accepted");
        near(1000-victim.getHealth(),victim.starHits*4.5f,"rain child inherits parent Power I after hand swap; no Sharpness");stars.stop();
    }
    private static void erecto(WoolBucketChecks.Fixture f,Witness victim,Vec3 origin){
        require(f.game.items().wands().electroSnapshot().isEmpty(),"empty virtual Erecto scene");reset(f.game,victim);victim.setPos(origin.add(1,0,0));
        var stack=new ItemStack(LbcWands.ERECTO);var registry=f.owner.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        stack.enchant(registry.getOrThrow(Enchantments.POWER),1);stack.enchant(registry.getOrThrow(Enchantments.SHARPNESS),5);
        var from=origin.add(0,.8,0);
        try(var use=WeaponEnchantments.using(f.owner,stack)){
            f.game.items().erecto().emitAt(f.owner,InteractionHand.MAIN_HAND,stack,1,from,new Vec3(1,0,0));
        }
        var center=f.game.items().wands().electroSnapshot().stream().filter(t->t.erecto!=null&&t.erecto.kind==me.skywars2.game.items.weapons.ranged.LbcErecto.CENTRAL).findFirst().orElseThrow();
        f.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIRT));
        // Native selector unit cells at x=0,.5,1 overlap this witness;1.5 and2 do not.
        victim.arcHits=0;f.game.items().erecto().step(center);require(victim.arcHits==3,"exact authored Erecto geometry has three accepted unit-cell contacts");
        near(1000-victim.getHealth(),4.5f,"three delayed Erecto contacts retain Power I and exclude Sharpness");
        reset(f.game,victim);victim.arcHits=0;var reflected=new LbcWands.ElectroTrace(center);reflected.owner=f.other;reflected.position=from;
        f.game.items().erecto().step(reflected);require(victim.arcHits==3,"controller change preserves the same three source contacts");
        near(1000-victim.getHealth(),3,"changed physical Erecto controller cannot borrow captured Power");
        for(var trace:f.game.items().wands().electroSnapshot())trace.age=trace.lifetime;
        f.game.items().wands().tick();f.game.items().erecto().stop();
    }
    private static void thornsArmor(WoolBucketChecks.Fixture f,net.minecraft.server.level.ServerPlayer defender){
        var plate=new ItemStack(Items.IRON_CHESTPLATE);
        // Keep genuine native armor-slot Thorns while isolating spell damage from armor mitigation.
        plate.set(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY);
        plate.enchant(defender.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.THORNS),20);
        defender.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,plate);
    }
    private static void keeperThorns(WoolBucketChecks.Fixture f,Zombie witness,Vec3 origin){
        var game=f.game;require(game.items().sauvojen().count()==0,"exclusive actual Keeper emitter");
        f.move(f.owner,origin.add(-4,0,0));f.move(f.other,origin.add(4,0,0));witness.setPos(origin.add(8,0,0));
        reset(game,f.owner);reset(game,f.other);thornsArmor(f,f.other);
        var wand=new ItemStack(LbcWands.ACADEMY_SAUVOJEN);
        wand.enchant(f.owner.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.POWER),1);
        try{
            var keeper=keeperFireball(f,wand);f.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIRT));
            keeper.setPos(f.other.position().add(-2,.8,0));keeper.setDeltaMovement(1.6,0,0);hitFireball(keeper,f.other);
            near(f.other.getHealth(),992.5f,"actual upgraded Keeper fireball retains Power I after hand swap");
            near(f.owner.getHealth(),1000,"genuine guaranteed armor Thorns cannot retaliate against floating Keeper's credited player");
            require(f.other.getKillCredit()==f.owner,"Keeper emitter retains actual player kill credit");

            reset(game,f.owner);reset(game,f.other);var direct=new SmallFireball(EntityTypes.SMALL_FIREBALL,f.level);
            try(var use=WeaponEnchantments.using(f.owner,wand)){
                game.items().wands().launchFire(f.owner,direct,f.other.position().add(-2,.8,0),new Vec3(1.6,0,0));
            }
            f.own(direct);hitFireball(direct,f.other);
            near(f.other.getHealth(),992.5f,"ordinary direct wand fireball retains the same captured Power");
            require(f.owner.getHealth()<=999&&f.owner.getLastDamageSource()!=null&&f.owner.getLastDamageSource().is(net.minecraft.world.damagesource.DamageTypes.THORNS),
                    "ordinary direct wand projectile still triggers genuine native Thorns");

            reset(game,f.owner);reset(game,f.other);thornsArmor(f,f.owner);
            var reflected=keeperFireball(f,wand);reflected.setOwner(f.other);
            reflected.setPos(f.owner.position().add(-2,.8,0));reflected.setDeltaMovement(1.6,0,0);hitFireball(reflected,f.owner);
            near(f.owner.getHealth(),995,"reflected Keeper projectile does not borrow original author's Power or receive stray retaliation");
            require(f.other.getHealth()<=999&&f.other.getLastDamageSource()!=null&&f.other.getLastDamageSource().is(net.minecraft.world.damagesource.DamageTypes.THORNS)
                            &&f.other.getLastDamageSource().getEntity()==f.owner,
                    "changed physical controller receives genuine Thorns instead of the former Keeper author");
        }finally{game.items().sauvojen().forget(f.owner);f.owner.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,ItemStack.EMPTY);f.other.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,ItemStack.EMPTY);}
    }
    private static SmallFireball keeperFireball(WoolBucketChecks.Fixture f,ItemStack wand){
        var before=new java.util.HashSet<java.util.UUID>();for(var body:f.level.getAllEntities())before.add(body.getUUID());
        f.owner.setYRot(-90);f.owner.setXRot(0);
        try(var use=WeaponEnchantments.using(f.owner,wand)){
            f.game.items().sauvojen().cast(f.owner,InteractionHand.MAIN_HAND,wand,LbcSauvojen.Kind.FIRE.ordinal());
        }
        f.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIRT));
        var emitted=new java.util.ArrayList<SmallFireball>();
        for(int step=0;step<8&&emitted.isEmpty();step++){
            f.game.items().sauvojen().tick();
            for(var body:f.level.getAllEntities())if(body instanceof SmallFireball ball&&!before.contains(body.getUUID()))emitted.add(ball);
        }
        require(emitted.size()==3,"actual Academy Keeper FIRE mode publishes its three native small fireballs");
        var selected=emitted.getFirst();for(var ball:emitted){f.own(ball);if(ball!=selected)ball.discard();}
        f.game.items().sauvojen().forget(f.owner);require(f.game.items().sauvojen().count()==0,"emitter retires before its child impact");
        return selected;
    }
    private static void hitFireball(SmallFireball ball,LivingEntity defender){
        float before=defender.getHealth();for(int step=0;step<5&&!ball.isRemoved()&&defender.getHealth()==before;step++)ball.tick();
        require(defender.getHealth()<before,"actual native SmallFireball contact");ball.discard();
    }
}
