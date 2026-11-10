package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/** Native Carpet controls, ordinary HP and real starter loot, followed by isolated interaction regressions. */
final class BotRevisionChecks {
    private static Fixture running;
    private static void require(boolean value,String reason){if(!value)throw new IllegalStateException("Bot revision: "+reason);}
    private static final class Fixture extends CarpetBotChecks.Fixture {
        final List<BlockPos> starters=new ArrayList<>();
        final Set<UUID> preexisting=new HashSet<>();
        int phase,at,lastInventory,lastSignature,startedUse=-1,minimumCharge;
        float targetHealth,botHealth;boolean sawCharge,sawShot,checkpointCraft;int hits;net.minecraft.world.entity.Interaction teacher;
        net.minecraft.world.phys.Vec3 drawStart;net.minecraft.world.entity.projectile.arrow.Arrow defended;
        net.minecraft.world.entity.monster.zombie.Husk meleeMob;
        net.minecraft.world.phys.Vec3 strikePosition,strikeAway;float meleeHealth;int meleeContacts;boolean backedOff;
        int barsMined;BlockPos fallbackCenter;
        net.minecraft.world.entity.projectile.ShulkerBullet counterBullet;
        final boolean defenseOnly,counterOnly,academyOnly;
        Fixture(GameController game,boolean defenseOnly,boolean counterOnly,boolean academyOnly){super(game);this.defenseOnly=defenseOnly;this.counterOnly=counterOnly;this.academyOnly=academyOnly;}
        Object state(){return ((Map<?,?>)WoolBucketChecks.read(game.carpetBots(),"states")).get(bot.getUUID());}
        @Override void setup(){
            for(var actor:base.level.getAllEntities())preexisting.add(actor.getUUID());
            super.setup();bot.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);bot.setHealth(20);bot.getFoodData().setFoodLevel(20);
            if(defenseOnly){
                if(base.level.getBlockEntity(firstChest) instanceof net.minecraft.world.Container old)old.clearContent();
                base.set(firstChest,Blocks.AIR.defaultBlockState());if(academyOnly)studyIce();else if(counterOnly)counterShot();else cover();return;
            }
            starters.addAll(starterChests());at=game.server().getTickCount();
        }
        void next(int value){phase=value;at=game.server().getTickCount();}
        void isolated(){game.carpetBots().forget(bot);bot.stopUsingItem();bot.setLastHurtByMob(null);bot.getInventory().clearContent();bot.removeAllEffects();bot.setHealth(20);bot.getFoodData().setFoodLevel(20);move(bot,origin.add(2,0,0),-90);}
        void cover(){
            base.move(base.other,origin.add(15,0,0));isolated();bot.getInventory().setItem(0,new ItemStack(Items.COBBLESTONE,16));
            defended=new net.minecraft.world.entity.projectile.arrow.Arrow(base.level,base.other,new ItemStack(Items.ARROW),null);
            defended.setPos(bot.getEyePosition().add(12,0,0));defended.setDeltaMovement(-.45,0,0);defended.setNoGravity(true);defended.setBaseDamage(8);
            require(base.level.addFreshEntity(defended),"real incoming damaging arrow");base.owned.add(defended.getUUID());botHealth=bot.getHealth();next(11);
        }
        void ranged(boolean quick){
            isolated();var bow=new ItemStack(Items.CROSSBOW);
            if(quick)bow.enchant(game.server().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.QUICK_CHARGE),3);
            bot.getInventory().setItem(0,bow);bot.getInventory().setItem(1,new ItemStack(Items.ARROW));
            base.move(base.other,origin.add(13,0,0));base.other.setHealth(base.other.getMaxHealth());base.other.setInvulnerableTime(0);base.other.damageCooldownTime=0;
            targetHealth=base.other.getHealth();startedUse=-1;sawCharge=sawShot=false;minimumCharge=CrossbowItem.getChargeDuration(bow,bot);
        }
        void cageFallback(boolean supplies){
            isolated();bot.getInventory().setItem(0,new ItemStack(Items.IRON_PICKAXE));
            if(supplies)bot.getInventory().setItem(1,new ItemStack(Items.COBBLESTONE,16));
            fallbackCenter=BlockPos.containing(bot.position());
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=0;y<=4;y++)base.set(fallbackCenter.offset(x,y,z),Blocks.AIR.defaultBlockState());
            for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL)for(int y=0;y<(supplies?2:4);y++)
                base.set(fallbackCenter.relative(direction).above(y),Blocks.IRON_BARS.defaultBlockState());
            barsMined=bot.getStats().getValue(net.minecraft.stats.Stats.BLOCK_MINED.get(Blocks.IRON_BARS));next(supplies?13:14);
        }
        void counterShot(){
            isolated();var cell=BlockPos.containing(bot.position());
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=0;y<=4;y++)base.set(cell.offset(x,y,z),Blocks.AIR.defaultBlockState());
            bot.getInventory().setItem(0,new ItemStack(Items.IRON_SWORD));
            base.move(base.other,bot.position().add(0,0,6));
            counterBullet=new net.minecraft.world.entity.projectile.ShulkerBullet(base.level,base.other,bot,net.minecraft.core.Direction.Axis.Y);
            counterBullet.setPos(bot.getEyePosition().add(0,0,6));counterBullet.setDeltaMovement(0,0,-.15);
            require(base.level.addFreshEntity(counterBullet),"native homing bullet counter witness");base.owned.add(counterBullet.getUUID());
            base.move(base.other,origin.add(84,8,0));next(18);
        }
        void studyIce(){
            isolated();bot.getInventory().setItem(0,new ItemStack(LbcWands.ICE));
            teacher=new net.minecraft.world.entity.Interaction(net.minecraft.world.entity.EntityTypes.INTERACTION,base.level);
            teacher.setPos(origin.add(5,0,1));teacher.entityTags().add("magic_academy");
            require(base.level.addFreshEntity(teacher),"native Academy interaction actor");base.owned.add(teacher.getUUID());next(8);
        }
        int inventorySignature(){int value=1;for(int i=0;i<36;i++){var item=bot.getInventory().getItem(i);value=31*value+ItemStack.hashItemAndComponents(item);value=31*value+item.getCount();}for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))value=31*value+ItemStack.hashItemAndComponents(bot.getItemBySlot(slot));return value;}
        @Override boolean step(){
            base.client.acknowledge();base.audience.acknowledge();int now=game.server().getTickCount();
            require(now-at<(phase==0?1200:650),"phase deadline "+phase+" "+game.carpetBots().diagnostics(bot));
            require(bot.isAlive()&&game.server().getPlayerList().getPlayer(bot.getUUID())==bot,"ordinary20HP native body survives phase "+phase);
            switch(phase){
                case 0->{
                    var state=state();if(state==null||Boolean.TRUE.equals(WoolBucketChecks.read(state,"startingLoot"))
                            ||!((LbcBotCrafting)WoolBucketChecks.read(state,"crafting")).prepared())return false;
                    require(bot.getMaxHealth()==20,"no inflated survival HP");
                    require(game.carpetBots().observation(bot).looted()>20,"actual starter tables transferred through player chest menus");
                    require(bot.getInventory().countItem(Items.WATER_BUCKET)>0,"three actual starter tables supply a native water bucket");
                    for(var pos:starters)base.set(pos,Blocks.AIR.defaultBlockState());
                    isolated();farChest=BlockPos.containing(origin).offset(13,0,0);chest(farChest,new ItemStack(Items.EMERALD));
                    var drop=new ItemEntity(base.level,origin.x+5,origin.y,origin.z,new ItemStack(Items.DIAMOND,3));drop.setDefaultPickUpDelay();require(base.level.addFreshEntity(drop),"native diverted drop");base.owned.add(drop.getUUID());next(1);
                }
                case 1->{
                    require(now-at<240,"pickup diversion restores chest destination promptly");
                    if(bot.getInventory().countItem(Items.EMERALD)==0)return false;
                    require(bot.getInventory().countItem(Items.DIAMOND)==3,"native delayed pickup, not a remote grant");base.set(farChest,Blocks.AIR.defaultBlockState());ranged(false);next(2);
                }
                case 2,3->{
                    require(now-at<140,"native crossbow fires instead of cycling incomplete charges phase "+phase);
                    if(bot.isUsingItem()&&startedUse<0)startedUse=now;
                    if(CrossbowItem.isCharged(bot.getMainHandItem())){sawCharge=true;require(bot.getInventory().countItem(Items.ARROW)==0,"last ammunition really loads into crossbow");}
                    for(var arrow:base.level.getEntitiesOfClass(AbstractArrow.class,bot.getBoundingBox().inflate(20),e->e.getOwner()==bot)){sawShot=true;base.owned.add(arrow.getUUID());}
                    if(base.other.getHealth()>=targetHealth)return false;
                    require(sawCharge&&sawShot&&startedUse>=0&&now-startedUse>=minimumCharge,"native charge, last-arrow shot and real collision damage");
                    if(phase==2){ranged(true);next(3);}
                    else{
                        base.move(base.other,origin.add(84,8,0));isolated();bot.getInventory().setItem(18,new ItemStack(Items.IRON_CHESTPLATE));bot.getInventory().setItem(25,new ItemStack(Items.IRON_BOOTS));
                        bot.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.LEATHER_CHESTPLATE));lastSignature=inventorySignature();lastInventory=Integer.MIN_VALUE;next(4);
                    }
                }
                case 4->{
                    int signature=inventorySignature();if(signature!=lastSignature){require(lastInventory==Integer.MIN_VALUE||now-lastInventory>=5,"at most one native inventory manipulation per five ticks");lastInventory=now;lastSignature=signature;}
                    if(!bot.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE)||!bot.getItemBySlot(EquipmentSlot.FEET).is(Items.IRON_BOOTS))return false;
                    isolated();bot.getInventory().setItem(0,new ItemStack(Items.IRON_SWORD));
                    base.move(base.other,origin.add(2,0,2.5));base.other.setInvulnerableTime(0);base.other.damageCooldownTime=0;
                    var plate=new ItemStack(Items.DIAMOND_CHESTPLATE);plate.enchant(game.server().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.THORNS),10);base.other.setItemSlot(EquipmentSlot.CHEST,plate);
                    bot.setYRot(0);bot.setYHeadRot(0);botHealth=bot.getHealth()+bot.getAbsorptionAmount();next(5);
                }
                case 5->{
                    if(bot.getHealth()+bot.getAbsorptionAmount()>=botHealth)return false;
                    var state=state();var defense=(LbcBotDefense)WoolBucketChecks.read(state,"defense");require(defense.thorns(base.other),"real native reflected hit remembers this thorny opponent");
                    hits=game.carpetBots().observation(bot).hits();next(6);
                }
                case 6->{
                    require(game.carpetBots().observation(bot).hits()==hits,"bot does not keep damaging itself against known thorns");
                    var defense=(LbcBotDefense)WoolBucketChecks.read(state(),"defense");
                    require(defense.thorns(base.other),"unchanged thorny gear remains known beyond the old ten-second timeout");
                    if(now-at<240)return false;
                    base.other.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);
                    require(!defense.thorns(base.other),"removing actual reflected-damage gear permits another attack");next(15);
                }
                case 15->{
                    require(now-at<180,"native melee resumes after actual thorns gear is removed");
                    if(game.carpetBots().observation(bot).hits()==hits)return false;
                    isolated();bot.getInventory().setItem(0,new ItemStack(Items.BOW));bot.getInventory().setItem(1,new ItemStack(Items.ARROW,4));
                    base.move(base.other,origin.add(12,0,0));
                    var plate=new ItemStack(Items.DIAMOND_CHESTPLATE);plate.enchant(game.server().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.THORNS),10);
                    base.other.setItemSlot(EquipmentSlot.CHEST,plate);next(16);
                }
                case 16->{
                    require(now-at<180,"actual arrow reflection teaches thorns without a synchronous melee attack");
                    var state=state();if(state==null||!((LbcBotDefense)WoolBucketChecks.read(state,"defense")).thorns(base.other))return false;
                    require(game.carpetBots().observation(bot).hits()==0&&bot.getInventory().countItem(Items.ARROW)==3,
                            "one actual arrow shot, no melee observation, learns reflected damage");next(17);
                }
                case 17->{
                    require(bot.getInventory().countItem(Items.ARROW)==3,"remembered ranged reflection prevents repeated self-damaging shots");
                    if(now-at<60)return false;
                    base.other.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);
                    base.move(base.other,origin.add(84,8,0));isolated();
                    for(int slot=0;slot<35;slot++)if(slot!=34)bot.getInventory().setItem(slot,new ItemStack(Items.DIRT));
                    bot.getInventory().setItem(30,new ItemStack(LbcWands.AIR_WAND));bot.getInventory().setItem(35,new ItemStack(LbcWands.EXOPERSO));next(7);
                }
                case 7->{
                    require(now-at<300,"bounded late-slot recipe discovery and native clicks, including checkpoint cancellation");
                    var state=state();if(state==null)return false;var craft=(LbcBotCrafting)WoolBucketChecks.read(state,"crafting");
                    if(!checkpointCraft&&craft.stage==2){
                        game.checkpointCore();checkpointCraft=true;
                        require(!craft.active()&&bot.inventoryMenu.getCarried().isEmpty(),"checkpoint cancels cursor before native player image");
                        for(int slot=1;slot<=4;slot++)require(!bot.inventoryMenu.getSlot(slot).hasItem(),"checkpoint returns all crafting-grid stacks");
                        require(bot.getInventory().countItem(LbcWands.AIR_WAND)==1&&bot.getInventory().countItem(LbcWands.EXOPERSO)==1,"checkpoint neither loses nor duplicates ingredients");
                    }
                    if(bot.getInventory().countItem(LbcWands.EXASPERATO)==0)return false;
                    require(checkpointCraft&&bot.getInventory().countItem(LbcWands.AIR_WAND)==0&&bot.getInventory().countItem(LbcWands.EXOPERSO)==0&&bot.getInventory().countItem(LbcWands.EXASPERATO)==1,"native player-grid recipe debits source wands and yields exactly one enhanced wand");
                    studyIce();
                }
                case 8->{
                    ItemStack wand=ItemStack.EMPTY;for(int i=0;i<36;i++)if(bot.getInventory().getItem(i).is(LbcWands.ICE))wand=bot.getInventory().getItem(i);
                    if(wand.isEmpty()||ItemData.intValue(wand,"upgradable",0)!=1)return false;
                    require(ItemData.intValue(wand,"upgrade_available",0)==0,"actual trial begins without injected progress");
                    game.items().frost().clear(base.other);base.other.removeAllEffects();base.other.setHealth(base.other.getMaxHealth());
                    base.other.setInvulnerableTime(0);base.other.damageCooldownTime=0;base.move(base.other,origin.add(8,0,0));next(19);
                }
                case 19->{
                    ItemStack wand=ItemStack.EMPTY;for(int slot=0;slot<36;slot++)if(bot.getInventory().getItem(slot).is(LbcWands.ICE))wand=bot.getInventory().getItem(slot);
                    require(!wand.isEmpty(),"ordinary study wand remains during its real trial");
                    if((now-at)%40==0){
                        var sight=base.level.clip(new net.minecraft.world.level.ClipContext(bot.getEyePosition(),base.other.getEyePosition(),
                                net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,bot));
                        SkyWars2.LOGGER.info("[check] Native bot Academy trial ticks={} cold={} frozen={} cost={} sight={} block={} enemyEye={} {}",now-at,
                                game.items().frost().cold(base.other),game.items().frost().isFrozen(base.other),
                                game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("ice_wand_mana_use"),"lbc.math").get(),
                                sight.getType(),sight.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK?sight.getBlockPos():null,base.other.getEyePosition(),game.carpetBots().diagnostics(bot));
                    }
                    if(ItemData.intValue(wand,"upgrade_available",0)!=1)return false;
                    require(game.items().frost().isFrozen(base.other),"real native Ice casts freeze a player and complete the Academy criterion");
                    require((int)WoolBucketChecks.read(state(),"crystalsCleared")>0,
                            "trial clears an obstructing real Ice crystal through a native aimed attack");
                    base.move(base.other,origin.add(84,8,0));game.items().frost().clear(base.other);base.other.removeAllEffects();next(9);
                }
                case 9->{
                    if(bot.getInventory().countItem(LbcWands.ACADEMY_ICE)==0)return false;
                    require(bot.getInventory().countItem(LbcWands.ICE)==0,"actual native teacher replaces one eligible wand without a duplicate");
                    if(academyOnly)return true;
                    isolated();bot.getInventory().setItem(0,new ItemStack(Items.BOW));bot.getInventory().setItem(1,new ItemStack(Items.ARROW,12));
                    base.move(base.other,origin.add(15,0,0));base.other.setHealth(base.other.getMaxHealth());base.other.setInvulnerableTime(0);base.other.damageCooldownTime=0;
                    targetHealth=base.other.getHealth();drawStart=null;sawCharge=false;next(10);
                }
                case 10->{
                    require(now-at<160,"charging bow keeps pursuing the retreating target");
                    if(bot.isUsingItem()&&drawStart==null){drawStart=bot.position();base.move(base.other,origin.add(23,0,0));}
                    if(drawStart!=null&&!bot.isUsingItem()&&bot.getInventory().countItem(Items.ARROW)<12){
                        require(bot.position().subtract(drawStart).horizontalDistanceSqr()>.36,"actual movement continues during the native bow charge");sawCharge=true;
                    }
                    if(base.other.getHealth()>=targetHealth)return false;
                    require(sawCharge,"native moving bow shot really hits the retreating player");
                    cover();
                }
                case 11->{
                    require(now-at<100,"finite native cover response");
                    if(now-at<40){var ai=state();var defense=ai==null?null:(LbcBotDefense)WoolBucketChecks.read(ai,"defense");SkyWars2.LOGGER.info("[check] Cover frame={} arrowOwner={} alive={} time={} arrow={} {} {}",now-at,defended.getOwner()==base.other,defended.isAlive(),LbcBotActions.impactTime(bot,defended),defended.position(),defense==null?null:defense.diagnostics(),game.carpetBots().diagnostics(bot));}
                    require(bot.getHealth()==botHealth,"legal wall intercepts the real incoming arrow: arrow="+defended.position()+" motion="+defended.getDeltaMovement()+" "+game.carpetBots().diagnostics(bot));
                    if(game.carpetBots().observation(bot).placements()<2)return false;
                    boolean grounded;
                    try{var m=AbstractArrow.class.getDeclaredMethod("isInGround");m.setAccessible(true);grounded=(Boolean)m.invoke(defended);}
                    catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
                    if(!grounded)return false;
                    require(bot.getInventory().countItem(Items.COBBLESTONE)==14,"two real cover blocks are debited from the finite owned stack");
                    if(defenseOnly)return true;
                    defended.discard();base.move(base.other,origin.add(84,8,0));isolated();
                    bot.getInventory().setItem(0,new ItemStack(Items.IRON_SWORD));
                    meleeMob=new net.minecraft.world.entity.monster.zombie.Husk(net.minecraft.world.entity.EntityTypes.HUSK,base.level);
                    meleeMob.setPos(origin.add(6,0,0));meleeMob.setPersistenceRequired();meleeMob.setTarget(bot);
                    require(base.level.addFreshEntity(meleeMob),"native attacking mob with ordinary HP and movement");base.owned.add(meleeMob.getUUID());
                    meleeHealth=meleeMob.getHealth();next(12);
                }
                case 12->{
                    require(now-at<240,"native mob is defeated without a distant melee stalemate "+game.carpetBots().diagnostics(bot));
                    if(meleeMob.getHealth()<meleeHealth){
                        meleeContacts++;meleeHealth=meleeMob.getHealth();strikePosition=bot.position();
                        strikeAway=bot.position().subtract(meleeMob.position()).multiply(1,0,1).normalize();
                    }
                    if(strikePosition!=null&&bot.getAttackStrengthScale(.5f)<.88
                            &&bot.position().subtract(strikePosition).dot(strikeAway)>.1)backedOff=true;
                    if(meleeMob.isAlive())return false;
                    require(meleeContacts>=2&&backedOff,"real melee contacts alternate with native movement away during cooldown");
                    require(meleeMob.getLastDamageSource()!=null&&meleeMob.getLastDamageSource().getEntity()==bot,"native bot strike defeats the actual attacker");
                    cageFallback(true);
                }
                case 13,14->{
                    require(now-at<160,"unavailable roof cannot stall native escape "+game.carpetBots().diagnostics(bot));
                    require(base.level.getBlockState(fallbackCenter.above(2)).isAir(),"no floating roof is placed without an actual visible attachment or owned blocks");
                    if(bot.getStats().getValue(net.minecraft.stats.Stats.BLOCK_MINED.get(Blocks.IRON_BARS))<=barsMined)return false;
                    require(java.util.stream.StreamSupport.stream(net.minecraft.core.Direction.Plane.HORIZONTAL.spliterator(),false)
                            .anyMatch(direction->base.level.getBlockState(fallbackCenter.relative(direction).above()).isAir()),"native accepted mining opens a real upper exit");
                    if(phase==13){
                        require(bot.getInventory().countItem(Items.COBBLESTONE)==16,"impossible roof never consumes the finite block stack");
                        cageFallback(false);
                    }else counterShot();
                }
                case 18->{
                    var state=state();var defense=state==null?null:(LbcBotDefense)WoolBucketChecks.read(state,"defense");
                    if((now-at)%20==0)SkyWars2.LOGGER.info("[check] Native counter tick={} bullet={} motion={} alive={} ticking={} time={} ray={} attack={} {} {}",now-at,
                            counterBullet.position(),counterBullet.getDeltaMovement(),counterBullet.isAlive(),counterBullet.tickCount,LbcBotActions.impactTime(bot,counterBullet),
                            LbcBotActions.melee(bot,counterBullet,bot.entityInteractionRange()),bot.getAttributeValue(Attributes.ATTACK_DAMAGE),defense==null?null:defense.diagnostics(),game.carpetBots().diagnostics(bot));
                    require(now-at<160,"native counter swings before the approaching homing bullet hits: "+counterBullet.position()+" "+game.carpetBots().diagnostics(bot));
                    require(bot.getHealth()==20&&!bot.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION),"counter uses native attack without accepting a bullet hit");
                    if(!counterBullet.isRemoved())return false;
                    require(state!=null&&defense.countered==1,
                            "actual precisely aimed player attack destroys the real shulker bullet");return true;
                }
                default->throw new IllegalStateException();
            }
            return false;
        }
        @Override public void close(){
            // Native starter drops, projectiles and reflected-hit effects belong to this scene too.
            for(var actor:base.level.getAllEntities())if(!(actor instanceof net.minecraft.server.level.ServerPlayer)
                    &&!preexisting.contains(actor.getUUID())&&actor.position().distanceToSqr(origin)<120*120)base.owned.add(actor.getUUID());
            super.close();
        }
    }
    static void run(GameController game){start(game,false,false,false);}
    static void defense(GameController game){start(game,true,false,false);}
    static void counter(GameController game){start(game,true,true,false);}
    static void academy(GameController game){start(game,true,false,true);}
    private static void start(GameController game,boolean defenseOnly,boolean counterOnly,boolean academyOnly){require(running==null,"exclusive fixture");running=new Fixture(game,defenseOnly,counterOnly,academyOnly);try{running.setup();}catch(Throwable e){try{running.close();}catch(Throwable c){e.addSuppressed(c);}running=null;throw e;}}
    static void tick(GameController game){
        if(running==null)return;
        String suite=running.academyOnly?"bot-academy-trial":running.counterOnly?"bot-counter":running.defenseOnly?"bot-defense":"bot-revision";
        try{
            if(!running.step())return;
            running.close();running=null;
            SkyWars2.LOGGER.info("[check] PASS {} (native Carpet controls, real resources/menu clicks, physical projectile collision and scoped cleanup)",suite);
        }catch(Throwable error){
            try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;
            SkyWars2.LOGGER.error("[check] FAIL "+suite,error);
        }
    }
}
