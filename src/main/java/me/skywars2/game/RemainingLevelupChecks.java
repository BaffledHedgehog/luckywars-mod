package me.skywars2.game;

import java.lang.reflect.Field;
import java.util.*;
import me.skywars2.SkyWars2;
import me.skywars2.mixin.ShulkerBowMixin;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.*;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.phys.Vec3;

/** Native menu/shot/scheduler checks; scripted source choices supplement actual installed gameplay owners. */
public final class RemainingLevelupChecks {
    private RemainingLevelupChecks(){}
    private static Fixture running;
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("Remaining levelups: "+why);}
    private static void near(double actual,double expected,String why){require(Math.abs(actual-expected)<.00001,why+" actual="+actual+" expected="+expected);}
    private static Field field(Object value,String name){for(Class<?> c=value.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new IllegalStateException("Missing fixture field "+name);}
    public static void runRandom(GameController game){start(game,0);}
    public static void runProjectile(GameController game){start(game,1);}
    public static void runRewind(GameController game){start(game,2);}
    private static void start(GameController game,int kind){require(running==null,"exclusive fixture");var f=new Fixture(game,kind);running=f;try{f.begin();}catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    public static void tick(GameController game){
        if(running==null)return;var f=running;
        try{require(game==f.game,"same controller");if(!f.step())return;running=null;f.close();SkyWars2.LOGGER.info("[check] PASS {} ({})",f.name(),f.kind==0?"real gates, all40 real distinct Tatam outcomes, duplicate exclusion, retained random pairs and ordinary/BedWars reapplication":f.kind==1?"actual bow/multishot/firework/snowball/egg/pearl/machinegun inputs, source scalars, owned/foreign Homing30 and owner-only Gravitas":"actual nonempty coordinator random/RNG/projectile clone/freeze replay");}
        catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL "+f.name(),error);}
    }
    /** Only used while resolving supporting finite source sequences; never installed during coordinator capture. */
    private static final class Sequence extends LegacyRandomSource {
        final ArrayDeque<Integer> values=new ArrayDeque<>();int draws;
        Sequence(int... supplied){super(1);for(int n:supplied)values.add(n);}
        @Override public int nextInt(int bound){require(!values.isEmpty(),"unexpected source random draw bound="+bound);int n=values.removeFirst();require(n>=0&&n<bound,"scripted native choice inside bound "+bound);draws++;return n;}
        void complete(int count){require(values.isEmpty()&&draws==count,"exact finite source draw count "+count+" actual="+draws);}
    }
    private record Ballistic(double coefficient,Vec3 motion,boolean noGravity,UUID owner){}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final int kind;final ServerPlayer owner,other;final ServerLevel level;
        final RegenerationBeltChecks.Snapshot saved,otherSaved;final RegenerationBeltChecks.ScheduledClient client,audience;
        final SauvojenMeditationChecks.Attribution attribution;final LbcResolvedLevelups resolved;
        final Map<UUID,LbcResolvedLevelups.Image> images,originalImages;
        final Field randomField;final RandomSource originalRandom;final MatchRandomState originalRandomState;
        final boolean levelups,skills,ranked;final int limit;final Vec3 origin;final int started;
        final Set<UUID> originalBodies=new HashSet<>();final Set<UUID> ownedIds=new LinkedHashSet<>();
        final Map<UUID,Ballistic> replayBodies=new LinkedHashMap<>();final List<Entity> auraBodies=new ArrayList<>(),foreignBodies=new ArrayList<>();
        int stage,when;boolean closed,stopped,restoring;ItemStack automaticBow;LbcResolvedLevelups.Image chosen,replayChoice;long expectedNextRandom;
        Entity outside,protectedBody,frozenBody;Vec3 outsideMotion;int automaticStart;
        final List<Projectile> homingOwned=new ArrayList<>(),homingForeign=new ArrayList<>();
        net.minecraft.world.entity.animal.cow.Cow homingTarget;
        @SuppressWarnings("unchecked") Fixture(GameController game,int kind){
            this.game=game;this.kind=kind;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing()&&owner!=null&&other!=null&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other),"two actual active participants");level=owner.level();
            require(!game.vanity().busy()&&!game.vanity().hasSnapshot()&&!game.items().timeStop().stopped(level),"exclusive actual coordinator/source clock");
            require(game.items().projectileLevelups().checkpoint().owners().isEmpty(),"no unrelated Gravitas owner work");
            saved=new RegenerationBeltChecks.Snapshot(game,owner);otherSaved=new RegenerationBeltChecks.Snapshot(game,other);attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner,other));
            resolved=game.lbc().resolvedLevelups();randomField=field(resolved,"random");
            try{images=(Map<UUID,LbcResolvedLevelups.Image>)field(resolved,"images").get(resolved);originalImages=Map.copyOf(images);originalRandom=(RandomSource)randomField.get(resolved);originalRandomState=MatchRandomState.capture(originalRandom);}catch(IllegalAccessException e){throw new IllegalStateException(e);}
            levelups=game.config().lbcLevelups;skills=game.config().lbcSkills;ranked=game.config().ranked;limit=game.config().lbcLevelupLimit;
            level.getAllEntities().forEach(e->originalBodies.add(e.getUUID()));origin=new Vec3(owner.getX(),level.getMaxY()-24,owner.getZ());
            require(level.hasChunkAt(net.minecraft.core.BlockPos.containing(origin))&&level.getBlockState(net.minecraft.core.BlockPos.containing(origin)).isAir(),"resident native air scene");
            client=new RegenerationBeltChecks.ScheduledClient(game,owner);audience=new RegenerationBeltChecks.ScheduledClient(game,other);started=game.server().getTickCount();
        }
        String name(){return kind==0?"random-levelups":kind==1?"projectile-levelups":"remaining-levelups-rewind";}
        PlayerStore.Profile profile(){return game.players().get(owner.getUUID());}
        void random(RandomSource source){try{randomField.set(resolved,source);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
        void nativeRandom(){random(originalRandom);}
        void begin(){
            game.config().lbcLevelups=true;game.config().lbcSkills=false;game.config().ranked=false;game.config().lbcLevelupLimit=12;
            for(var p:List.of(owner,other)){
                game.lbc().clear(p);resolved.forget(p.getUUID());p.stopUsingItem();p.getInventory().clearContent();p.inventoryMenu.getCraftSlots().clearContent();p.containerMenu.setCarried(ItemStack.EMPTY);p.getInventory().setSelectedSlot(0);p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);p.getAbilities().invulnerable=false;p.getAbilities().flying=false;p.noPhysics=false;p.setNoGravity(true);p.setOnGround(false);p.setDeltaMovement(Vec3.ZERO);p.setKnownMovement(Vec3.ZERO);
                p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(500);p.setAbsorptionAmount(0);p.getFoodData().setFoodLevel(8);p.getFoodData().setSaturation(0);
                var row=game.players().get(p.getUUID());row.lbcSkill=row.lbcKit=row.lbcTrinket=row.lbcChallengeKit=0;Arrays.fill(row.levelups,0);Arrays.fill(row.trinkets,0);row.lbcLevelupSlot=0;
                game.items().mana().resetResources(p);game.items().mana().noMagic(p,0);game.items().magic().inventoryChanged(p);
            }
            move(owner,origin);move(other,origin.add(30,0,0));game.items().magic().flushCleansing();
            if(kind==0){randomCases();next(0);}
            else {projectilePicks(kind==2);if(kind==1){nativeLaunches();beginHoming();}else beginReplay();}
        }
        void move(ServerPlayer p,Vec3 at){require(p.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true),"native participant teleport");p.setDeltaMovement(Vec3.ZERO);p.setYHeadRot(0);p.setYBodyRot(0);if(p==owner)client.acknowledge();else audience.acknowledge();}
        String gate(int id){return "lbc:true_advancements/"+switch(id){case 1->"accesories/infinite_quiver";case 3->"accesories/regeneration_belt";case 14->"armor/chaos";case 25->"bows/railgun";case 28->"bows/railshish";case 29->"bows/randchanted";case 30->"bows/shulker";default->throw new IllegalArgumentException("fixture gate "+id);};}
        void revoke(String path){profile().lbcUnlocks.remove(path);var a=game.server().getAdvancements().get(Identifier.parse(path));require(a!=null,"real source advancement "+path);for(var key:a.value().criteria().keySet())owner.getAdvancements().revoke(a,key);}
        void award(String path){game.items().mana().award(owner,path);var a=game.server().getAdvancements().get(Identifier.parse(path));require(a!=null&&owner.getAdvancements().getOrStartProgress(a).isDone(),"actual completed source criteria "+path);}
        boolean menuPick(LbcLoadouts.Kind kind,int page,int slot){
            require(GameController.operator(owner),"actual operator menu context");boolean before=owner.entityTags().contains("skywars2.debug_perks");
            try{game.menus().openDebug(owner);require(game.menus().debugAccess(owner)&&game.canUseMenus(owner),"actual open debug GUI grants menu access");owner.entityTags().remove("skywars2.debug_perks");require(!game.debugPerks(owner),"ordinary source gate without debug bypass");return game.lbc().select(owner,kind,page,slot);}
            finally{try{owner.closeContainer();}finally{if(before)owner.entityTags().add("skywars2.debug_perks");else owner.entityTags().remove("skywars2.debug_perks");}}
        }
        void pick(int id){award(gate(id));require(menuPick(LbcLoadouts.Kind.LEVELUPS,id<=25?8000:8100,id<=25?id-1:id-28),"real source levelup selector "+id);}
        void raw(int... ids){Arrays.fill(profile().levelups,0);System.arraycopy(ids,0,profile().levelups,0,ids.length);}
        void apply(){game.lbc().apply(owner,false);}
        void fresh(){game.lbc().clear(owner);resolved.forget(owner.getUUID());game.items().mana().resetResources(owner);}
        void values(int mana,int max,int regen,String why){var m=game.items().mana();require(m.get(owner)==mana&&m.maximum(owner)==max&&m.baseRegen(owner)==regen,why+" actual="+m.get(owner)+"/"+m.maximum(owner)+"/"+m.baseRegen(owner));}
        int choiceIndex(int wanted,int... excluded){
            int index=0;
            for(int id=1;id<=44;id++){
                if(id==14||id==19||id==26||id==27)continue;
                boolean omit=false;for(int blocked:excluded)if(id==blocked)omit=true;
                if(omit)continue;if(id==wanted)return index;index++;
            }
            throw new IllegalArgumentException("Not a real available Tatam result "+wanted);
        }
        void randomCases(){
            revoke(gate(14));require(!menuPick(LbcLoadouts.Kind.LEVELUPS,8000,13),"locked Tatam source selector rejects actual input");pick(14);
            revoke(gate(25));require(!menuPick(LbcLoadouts.Kind.LEVELUPS,8000,24),"direct locked result25 is rejected independently");
            try{
                for(int result=1;result<=44;result++){
                    if(result==14||result==19||result==26||result==27)continue;
                    fresh();raw(14,14);Sequence seq=result==1?new Sequence(0,0,20):new Sequence(choiceIndex(result));random(seq);apply();seq.complete(result==1?3:1);
                    var image=resolved.image(owner.getUUID());long expected=1L<<result;
                    require(image.randomOutcome()==result&&image.effectiveMask()==expected&&game.lbc().effectiveLevelupMask(owner)==expected,"actual installed random source outcome "+result);
                    require(profile().levelups[0]==14&&profile().levelups[1]==14&&game.lbc().selected(profile(),LbcLoadouts.Kind.LEVELUPS,8000,13),"raw Tatam/menu highlight retained after outcome "+result);
                    if(result==25)require(game.lbc().hasLevelup(owner,25)&&!owner.getAdvancements().getOrStartProgress(game.server().getAdvancements().get(Identifier.parse(gate(25)))).isDone(),"chosen25 bypasses its locked result gate only");
                    if(result==2)values(5000,5000,15,"random2 reaches existing native mana owner");
                    if(result==11)values(2500,2500,30,"random11 reaches existing native mana owner");
                    if(result==17)values(5000,6500,14,"random17 reaches existing native mana owner");
                    if(result==31)values(5000,10000,30,"random31 reaches existing native mana owner");
                    if(result==32)values(0,0,0,"random32 reaches existing native Nulla owner");
                    apply();seq.complete(result==1?3:1);require(resolved.image(owner.getUUID()).equals(image),"ordinary reapplication never rerolls outcome "+result);
                }
                fresh();raw(14,14);var pair=new Sequence(0,0,20);random(pair);apply();pair.complete(3);chosen=resolved.image(owner.getUUID());require(chosen.firstStat()==0&&chosen.secondStat()==20,"independent stored random-stat pair");
                var armor=owner.getAttribute(Attributes.ARMOR);require(armor.getModifier(Identifier.parse("minecraft:levelup_random_statup_1"))!=null,"first actual source random-stat modifier installed");
                raw();apply();require(resolved.image(owner.getUUID()).randomOutcome()==1&&resolved.image(owner.getUUID()).firstStat()==0,"removing Tatam retains dormant match choice");raw(14,14);apply();require(resolved.image(owner.getUUID()).equals(chosen),"debug remove/reselect cannot fish a new outcome");pair.complete(3);
                award(gate(1));raw(1,14,14);apply();require(resolved.image(owner.getUUID()).effectiveMask()==(1L<<1),"later direct duplicate of persisted Tatam result remains one effect without reroll");require(resolved.image(owner.getUUID()).firstStat()==0&&resolved.image(owner.getUUID()).secondStat()==20,"direct1 and chosen1 share one pair");pair.complete(3);
                var stable=resolved.image(owner.getUUID());game.lbc().clear(owner);apply();require(resolved.image(owner.getUUID()).equals(stable),"ordinary clear/body-reapply choice stability");game.lbc().applyBedwarsRespawn(owner);require(resolved.image(owner.getUUID()).equals(stable)&&armor.getModifier(Identifier.parse("minecraft:levelup_random_statup_1"))!=null,"actual BedWars reapplication restores retained pair and effects");pair.complete(3);
                fresh();raw(14);var equal=new Sequence(0,11,11);random(equal);apply();equal.complete(3);near(owner.getMaxHealth(),1800,"two equal independent max-health draws keep distinct source modifier IDs");
                fresh();award(gate(3));raw(14,3,3);var health=new Sequence(2);random(health);owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);apply();health.complete(1);near(owner.getMaxHealth(),46,"duplicate direct3 applies once while Tatam excludes already selected3");owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(80);apply();near(owner.getMaxHealth(),92,"owned multiplier follows current native base without reroll");health.complete(1);
                fresh();raw(14,31);var nulla=new Sequence(choiceIndex(32,31));random(nulla);apply();nulla.complete(1);values(0,0,0,"resolved Nulla clears resources after explicit Sanguis");
                fresh();raw(14,14);var finalPair=new Sequence(0,0,20);random(finalPair);apply();finalPair.complete(3);chosen=resolved.image(owner.getUUID());
            }finally{nativeRandom();}
        }
        void projectilePicks(boolean randomSpeed){
            raw();profile().lbcLevelupSlot=0;
            if(randomSpeed){pick(14);var seq=new Sequence(choiceIndex(25));random(seq);try{apply();seq.complete(1);}finally{nativeRandom();}}
            else pick(25);
            pick(28);pick(29);pick(30);apply();
            for(int id:new int[]{25,28,29,30})require(game.lbc().hasLevelup(owner,id),"canonical effective physical perk "+id);
            require(game.items().projectileLevelups().checkpoint().owners().contains(owner.getUUID()),"actual loadout application enrolls Gravitas owner");
        }
        Set<UUID> bodies(){var ids=new HashSet<UUID>();level.getAllEntities().forEach(e->ids.add(e.getUUID()));return ids;}
        List<Projectile> added(Set<UUID> before){var result=new ArrayList<Projectile>();for(var e:level.getAllEntities())if(!before.contains(e.getUUID())){ownedIds.add(e.getUUID());if(e instanceof Projectile p&&p.getOwner()==owner&&!p.isRemoved())result.add(p);}return result;}
        List<Projectile> bow(ItemStack ammo,ItemStack weapon,int remaining){var before=bodies();owner.stopUsingItem();owner.setItemInHand(InteractionHand.MAIN_HAND,weapon);owner.setItemInHand(InteractionHand.OFF_HAND,ammo);owner.gameMode.useItem(owner,level,weapon,InteractionHand.MAIN_HAND);boolean accepted=Items.BOW.releaseUsing(weapon,level,owner,remaining);owner.stopUsingItem();var result=added(before);require(accepted==!result.isEmpty(),"native Bow release result agrees with actual publication");return result;}
        List<Projectile> cross(ItemStack ammo,boolean multi){var before=bodies();owner.stopUsingItem();var weapon=new ItemStack(Items.CROSSBOW);if(multi)weapon.enchant(owner.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MULTISHOT),1);weapon.set(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.ofNonEmpty(multi?List.of(ammo.copy(),ammo.copy(),ammo.copy()):List.of(ammo.copy())));owner.setItemInHand(InteractionHand.MAIN_HAND,weapon);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);require(owner.gameMode.useItem(owner,level,weapon,InteractionHand.MAIN_HAND).consumesAction(),"actual native charged Crossbow use");return added(before);}
        Projectile thrown(Item item){var before=bodies();owner.stopUsingItem();var stack=new ItemStack(item,2);owner.setItemInHand(InteractionHand.MAIN_HAND,stack);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);require(owner.gameMode.useItem(owner,level,stack,InteractionHand.MAIN_HAND).consumesAction(),"actual native throwable item use "+item);var result=added(before);require(result.size()==1&&stack.getCount()==1,"actual throwable creates one body/debits one item "+item);owner.getCooldowns().removeCooldown(owner.getCooldowns().getCooldownGroup(stack));return result.getFirst();}
        double coefficient(AbstractArrow a){return ((ShulkerBowMixin.ArrowAccess)a).skywars2$baseDamage();}
        void finalized(Projectile p,String why){require(p.entityTags().contains(LbcProjectileLevelups.FINALIZED)&&p.isNoGravity(),why+" has exactly-once marker and Gravitas");if(p instanceof AbstractArrow a)near(coefficient(a),3,why+" native coefficient2 ->3");}
        void discardOwned(){var remove=new ArrayList<Entity>();for(var e:level.getAllEntities())if(ownedIds.contains(e.getUUID())&&!originalBodies.contains(e.getUUID()))remove.add(e);remove.forEach(Entity::discard);}
        void nativeLaunches(){
            var foreign=new Arrow(EntityTypes.ARROW,level);foreign.setOwner(other);foreign.setPos(owner.getEyePosition());foreign.setDeltaMovement(Vec3.ZERO);publish(foreign);double before=coefficient(foreign);
            require(bow(ItemStack.EMPTY,new ItemStack(Items.BOW),71980).isEmpty(),"actual no-ammo Bow cannot publish");require(bow(new ItemStack(Items.ARROW),new ItemStack(Items.BOW),71999).isEmpty(),"actual weak Bow release cannot publish");
            var normal=bow(new ItemStack(Items.ARROW),new ItemStack(Items.BOW),71980);require(normal.size()==1&&normal.getFirst() instanceof AbstractArrow,"actual ordinary Bow family");var arrow=(AbstractArrow)normal.getFirst();finalized(arrow,"actual Bow");require(arrow.isCritArrow()&&arrow.getDeltaMovement().length()>3.7,"native critical flag and accelerated flight retained");require(coefficient(foreign)==before&&!foreign.entityTags().contains(LbcProjectileLevelups.FINALIZED),"real shot never adopts neighboring foreign arrow");
            var powered=new ItemStack(Items.BOW);powered.enchant(owner.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.POWER),3);var enchanted=bow(new ItemStack(Items.SPECTRAL_ARROW),powered,71980);require(enchanted.size()==1&&enchanted.getFirst() instanceof SpectralArrow,"native enchanted spectral Bow carrier retained");finalized(enchanted.getFirst(),"actual enchanted spectral Bow");
            var family=cross(new ItemStack(Items.SPECTRAL_ARROW),true);require(family.size()==3,"actual multishot has three native siblings");for(var p:family){require(p instanceof SpectralArrow,"actual multishot spectral carrier");finalized(p,"actual multishot sibling");}
            var fireworks=cross(new ItemStack(Items.FIREWORK_ROCKET),true);require(fireworks.size()==3,"actual fireworks multishot publishes three native rockets");for(var p:fireworks){require(!(p instanceof AbstractArrow),"firework retains native damage owner");finalized(p,"actual firework sibling");}
            var snow=thrown(Items.SNOWBALL);require(snow instanceof Snowball,"actual snowball native carrier");finalized(snow,"actual SnowballItem route");require(snow.getDeltaMovement().length()>1.8&&snow.getDeltaMovement().length()<2.1,"real snowball receives speed trait");
            for(var item:List.of(Items.EGG,Items.ENDER_PEARL)){var p=thrown(item);require(!p.entityTags().contains(LbcProjectileLevelups.FINALIZED)&&p.isNoGravity()&&p.getDeltaMovement().length()<1.7,"ordinary egg/pearl gets owned gravity without unsupported speed/damage "+item);p.discard();}
            // Supporting scalar contract on a native arrow; native admission is proved by the real inputs above.
            var custom=new Arrow(EntityTypes.ARROW,level);custom.setOwner(owner);custom.setBaseDamage(2.137);custom.setDeltaMovement(12,-.0000007,3);game.items().projectileLevelups().beforePublish(owner,custom,LbcProjectileLevelups.Launch.BOW);near(coefficient(custom),3.17,"floor multiply precedes additive .5 over actual custom coefficient2.137");near(custom.getDeltaMovement().x,15.6,"speed trait has no former10 component cap");near(custom.getDeltaMovement().y,-.00000091,"cap-free1.3 retains tiny signed motion");game.items().projectileLevelups().beforePublish(owner,custom,LbcProjectileLevelups.Launch.BOW);near(coefficient(custom),3.17,"finalized body cannot receive a second coefficient mutation");near(custom.getDeltaMovement().x,15.6,"finalized body cannot receive a second speed mutation");custom.discard();discardOwned();
        }
        void beginHoming(){
            game.config().lbcSkills=true;award("lbc:true_advancements/legends/academy/exahoming");
            require(menuPick(LbcLoadouts.Kind.SKILLS,10100,2)&&game.lbc().hasSkill(owner,30),"actual Homing30 source selector");
            move(other,origin.add(-30,0,0));var from=origin.add(0,1.6,35);
            homingTarget=new net.minecraft.world.entity.animal.cow.Cow(EntityTypes.COW,level);
            homingTarget.setPos(from.add(0,0,25));homingTarget.setNoAi(true);homingTarget.setNoGravity(true);publish(homingTarget);
            for(var author:List.of(owner,other))for(int family=0;family<3;family++){
                Projectile shot;
                if(family==2){
                    shot=game.items().clouds().trinketAcid(author,from.add(1,0,0),-90,0);ownedIds.add(shot.getUUID());
                    require(!shot.isRemoved()&&LbcClouds.sourceAcidManaged(shot)&&shot.getOwner()==null
                            &&game.items().projectileLevelups().projectileOwner(shot)==author,"real trinket acid retains empty native owner and canonical source author");
                    // Collision mode is independent of directional control. Exercise that flag
                    // on a real producer-owned body, without replacing its native source policy.
                    shot.noPhysics=true;
                }else{
                    shot=family==0?new Arrow(EntityTypes.ARROW,level):new Snowball(EntityTypes.SNOWBALL,level);
                    shot.setOwner(author);shot.setPos(from.add(family*.5,0,0));shot.setDeltaMovement(.15,0,0);shot.setNoGravity(true);game.items().markCurrentItemEpoch(shot);publish(shot);
                }
                (author==owner?homingOwned:homingForeign).add(shot);
            }
            next(5);
        }
        void beginAutomatic(){game.config().lbcSkills=true;award("lbc:true_advancements/bows/minigun");require(menuPick(LbcLoadouts.Kind.SKILLS,10000,13)&&game.lbc().hasSkill(owner,14),"actual Machinegun skill selector");owner.getInventory().clearContent();automaticBow=new ItemStack(Items.BOW);owner.setItemInHand(InteractionHand.MAIN_HAND,automaticBow);owner.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.ARROW,64));require(owner.gameMode.useItem(owner,level,automaticBow,InteractionHand.MAIN_HAND).consumesAction()&&owner.isUsingItem(),"actual native held Bow starts Machinegun input");automaticStart=client.updates();next(10);}
        void homingTrace(int elapsed){
            SkyWars2.LOGGER.info("[check] Homing30 frame={} skill={} suppressed={} inputStopped={} worldStopped={} target={} targetPos={} targetAlive={} targetHealth={} targetLoaded={} targetEligible={}",
                    elapsed,game.lbc().hasSkill(owner,30),game.items().magic().suppressed(owner),game.items().wands().inputTimeStopped(owner),game.items().timeStop().stopped(level),
                    homingTarget.getUUID(),homingTarget.position(),homingTarget.isAlive(),homingTarget.getHealth(),game.items().lifetimes().loadedTargets().contains(homingTarget),game.items().wands().target(owner,homingTarget));
            for(var expected:List.of(owner,other))for(var body:expected==owner?homingOwned:homingForeign){
                boolean indexed;
                try{indexed=((Set<?>)field(game.items().homing(),"projectiles").get(game.items().homing())).contains(body);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
                var actual=body.getOwner();var shared=game.items().projectileLevelups().projectileOwner(body);
                SkyWars2.LOGGER.info("[check] Homing30 body={} uuid={} type={} class={} expectedOwner={} nativeOwner={} sharedOwner={} pos={} motion={} removed={} noPhysics={} noGravity={} frozen={} suspended={} indexed={} cantHome={} nearbyPlayers={}",
                        body.getId(),body.getUUID(),body.getType(),body.getClass().getSimpleName(),expected.getUUID(),actual==null?null:actual.getUUID(),shared==null?null:shared.getUUID(),
                        body.position(),body.getDeltaMovement(),body.isRemoved(),body.noPhysics,body.isNoGravity(),game.items().timeStop().frozen(body),game.items().polymorph().suspended(body),indexed,body.entityTags().contains("cant_be_homing"),
                        level.players().stream().filter(p->!p.isSpectator()).map(p->p.getScoreboardName()+":"+p.position().distanceTo(body.position())).toList());
            }
        }
        <T extends Entity>T publish(T e){ownedIds.add(e.getUUID());require(level.addFreshEntity(e),"actual native fixture actor publication "+e.getType());return e;}
        Projectile gravityProjectile(EntityType<? extends Projectile> type,ServerPlayer author,Vec3 at){var p=type.create(level,EntitySpawnReason.COMMAND);require(p!=null,"native aura projectile factory");p.setOwner(author);p.setPos(at);p.setDeltaMovement(Vec3.ZERO);publish(p);require(p.isNoGravity()==(author==owner),"common native admission applies Gravitas only to actual owner at any distance");p.setNoGravity(false);return p;}
        void beginAura(){
            owner.stopUsingItem();game.items().machinegunSkill().clear(owner);discardOwned();var eye=owner.getEyePosition();
            auraBodies.add(gravityProjectile(EntityTypes.EGG,owner,eye.add(2,0,0)));foreignBodies.add(gravityProjectile(EntityTypes.EGG,other,eye.add(-2,0,0)));foreignBodies.add(gravityProjectile(EntityTypes.ENDER_PEARL,other,eye.add(0,0,2)));
            var tnt=new PrimedTnt(level,eye.x,eye.y,eye.z-2,owner);tnt.setFuse(400);tnt.setDeltaMovement(Vec3.ZERO);publish(tnt);require(tnt.isNoGravity(),"common loaded admission gives owned TNT gravity independent of shot flags");tnt.setNoGravity(false);auraBodies.add(tnt);
            outside=gravityProjectile(EntityTypes.EGG,owner,eye.add(0,0,4));outsideMotion=outside.getDeltaMovement();
            protectedBody=gravityProjectile(EntityTypes.EGG,owner,eye.add(1.8,0,1.8));LbcBedwarsRoleGuards.markShop(protectedBody);
            frozenBody=gravityProjectile(EntityTypes.EGG,owner,eye.add(-1.8,0,1.8));frozenBody.entityTags().add("time_stopped");
            var magic=new LbcClouds.AcidProjectile(LbcClouds.ACID_PROJECTILE,level);magic.setOwner(owner);magic.setPos(eye.add(0,1,-1));magic.setDeltaMovement(Vec3.ZERO);game.items().markCurrentItemEpoch(magic);publish(magic);require(magic.isNoGravity(),"real acid native type receives owned gravity at common admission");magic.setNoGravity(false);auraBodies.add(magic);
            var falling=EntityTypes.FALLING_BLOCK.create(level,EntitySpawnReason.COMMAND);require(falling!=null,"native neutral falling block factory");falling.setPos(eye);game.items().projectileLevelups().beforeNativeSpawn(falling);require(!falling.isNoGravity(),"common admission never treats a neutral falling block as an owned projectile");falling.discard();
            var foreignTnt=new PrimedTnt(level,eye.x-1,eye.y,eye.z,other);foreignTnt.setFuse(400);foreignTnt.setDeltaMovement(Vec3.ZERO);publish(foreignTnt);foreignBodies.add(foreignTnt);
            var neutralTnt=new PrimedTnt(level,eye.x+1,eye.y,eye.z,null);neutralTnt.setFuse(400);neutralTnt.setDeltaMovement(Vec3.ZERO);publish(neutralTnt);foreignBodies.add(neutralTnt);
            var receipt=new net.minecraft.world.entity.item.ItemEntity(level,eye.x,eye.y,eye.z,new ItemStack(Items.PAPER));receipt.setThrower(owner);
            var sources=game.items().spellSources();
            for(var kind:LbcLuckyBlocks.Kind.values())try(var reward=LbcLuckyBlocks.rewardScope(LbcLuckyBlocks.block(kind).defaultBlockState(),owner)){
                var body=EntityTypes.FALLING_BLOCK.create(level,EntitySpawnReason.COMMAND);require(body!=null,"native Lucky trap body");body.setPos(eye.add(1,1,1));
                game.items().projectileLevelups().beforeNativeSpawn(body);publish(body);foreignBodies.add(body);
                require(!body.isNoGravity()&&sources.projectileController(body)==null&&sources.source(body)!=null&&sources.source(body).emitter().equals(owner.getUUID())
                        &&sources.canAffect(body,owner),"each real Lucky reward scope keeps creator-emitted traps WILD and outside Gravitas: "+kind);
            }
            try(var loot=sources.lootScope(receipt)){
                var anvil=EntityTypes.FALLING_BLOCK.create(level,EntitySpawnReason.COMMAND);require(anvil!=null,"native trap anvil factory");
                var data=new net.minecraft.nbt.CompoundTag();data.putString("BlockState","minecraft:anvil");
                anvil.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),data));anvil.setPos(eye.add(1,1,0));
                game.items().projectileLevelups().beforeNativeSpawn(anvil);publish(anvil);foreignBodies.add(anvil);
                require(anvil.getBlockState().is(Blocks.ANVIL)&&!anvil.isNoGravity()&&sources.projectileController(anvil)==null,"creator's Gravitas never adopts an actual falling trap anvil: state="+anvil.getBlockState()+",noGravity="+anvil.isNoGravity()+",controller="+sources.projectileController(anvil));
                var bomb=new PrimedTnt(level,eye.x,eye.y+1,eye.z+1,owner);bomb.setFuse(400);bomb.setDeltaMovement(Vec3.ZERO);publish(bomb);foreignBodies.add(bomb);
                require(bomb.getOwner()==null&&!bomb.isNoGravity()&&sources.projectileController(bomb)==null,"trap TNT loses native creator control without gaining Gravitas: owner="+bomb.getOwner()+",native="+((LbcSpellSource.TntOwnerAccess)bomb).skywars2$nativeLootOwner()+",gravity="+bomb.isNoGravity()+",frozen="+game.items().timeStop().frozen(bomb)+",policy="+sources.source(bomb));
                var arrow=new net.minecraft.world.entity.projectile.arrow.Arrow(level,owner,new ItemStack(Items.ARROW),null);arrow.setPos(eye.add(2,1.5,0));arrow.setDeltaMovement(Vec3.ZERO);
                game.items().projectileLevelups().beforePublish(owner,arrow,LbcProjectileLevelups.Launch.BOW);publish(arrow);foreignBodies.add(arrow);
                require(arrow.getOwner()==null&&!arrow.isNoGravity()&&!arrow.entityTags().contains(LbcProjectileLevelups.FINALIZED),"trap arrows never acquire creator's launch scalars or gravity");
                for(var body:List.of(anvil,bomb,arrow)){
                    var cause=sources.source(body);require(cause!=null&&cause.cause()!=null&&cause.cause().player().equals(owner.getUUID())&&LbcIndirectKills.playerSource(body)==owner,"statistical trap receipt survives independently of native control");
                    require(sources.canAffect(body,owner),"trap can affect its own creator");
                }
            }finally{receipt.discard();}
            game.items().timeStop().begin(level,1000);stopped=true;next(12);
        }
        void beginReplay(){
            replayChoice=resolved.image(owner.getUUID());require(replayChoice.randomOutcome()==25,"actual replay has nonempty resolved Tatam choice");
            var nativeShot=bow(new ItemStack(Items.SPECTRAL_ARROW),new ItemStack(Items.BOW),71980);require(nativeShot.size()==1,"actual nonempty native shot before replay");var arrow=(AbstractArrow)nativeShot.getFirst();finalized(arrow,"replay native arrow");
            require(game.items().cloneSkills().copyNow(arrow,15)&&game.items().cloneSkills().copyNow(arrow,17),"installed x2/x4 accept finalized actual arrow");
            for(var e:level.getAllEntities())if(!originalBodies.contains(e.getUUID())&&e instanceof AbstractArrow a&&a.getOwner()==owner){ownedIds.add(a.getUUID());replayBodies.put(a.getUUID(),new Ballistic(coefficient(a),a.getDeltaMovement(),a.isNoGravity(),owner.getUUID()));}
            require(replayBodies.size()==5,"actual native source plus one x2 and three x4 bodies");for(var b:replayBodies.values()){near(b.coefficient(),3,"clone keeps finalized coefficient");require(b.noGravity(),"clone keeps finalized noGravity");}
            var rng=MatchRandomState.capture(originalRandom);expectedNextRandom=originalRandom.nextLong();rng.restore(originalRandom);
            game.items().timeStop().begin(level,10000);stopped=true;require(game.vanity().requestCapture(owner,false),"actual coordinator accepts nonempty random/projectile graph");next(40);
        }
        void next(int n){stage=n;when=game.server().getTickCount();}
        boolean step(){
            client.acknowledge();audience.acknowledge();require(game.server().getTickCount()-started<500,"bounded fixture stage="+stage);int elapsed=game.server().getTickCount()-when;
            if(restoring&&game.vanity().busy())for(var p:game.server().getPlayerList().getPlayers())if(p.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            switch(stage){
                case 0->{if(elapsed<2)return false;require(resolved.image(owner.getUUID()).equals(chosen),"real server ticks retain final random choice");return true;}
                case 5->{
                    if(elapsed==1||elapsed==5||elapsed==10||elapsed>=19)homingTrace(elapsed);
                    for(var expected:List.of(owner,other))for(var body:expected==owner?homingOwned:homingForeign){
                        require(game.items().projectileLevelups().projectileOwner(body)==expected,"Homing30 retains each actual canonical projectile author");
                        if(LbcClouds.sourceAcidManaged(body))require(body.getOwner()==null&&body.noPhysics,"real source acid keeps native owner empty and collision mode while native steering runs");
                    }
                    for(var foreign:homingForeign)require(!foreign.isRemoved()&&game.items().projectileLevelups().projectileOwner(foreign)==other&&Math.abs(foreign.getDeltaMovement().z)<.00001,"Homing30 never steers nearby foreign arrow/throwable/magic or steals authorship");
                    if(homingOwned.stream().anyMatch(p->p.getDeltaMovement().z<=.001)){require(elapsed<20,"bounded actual native Homing30 steers all three own projectile families");return false;}
                    for(var own:homingOwned)require(!own.isRemoved()&&game.items().projectileLevelups().projectileOwner(own)==owner,"Homing30 preserves own canonical projectile authorship and native representation");
                    discardOwned();move(other,origin.add(30,0,0));beginAutomatic();
                }
                case 10->{if(client.updates()-automaticStart<2)return false;require(owner.isUsingItem()&&game.items().machinegunSkill().charge(owner)>0,"actual native using-item ticks advance automatic charge");game.items().mana().score(owner,"bow_charge").set(2398);next(11);}
                case 11->{
                    var shots=new ArrayList<AbstractArrow>();for(var e:level.getAllEntities())if(e instanceof AbstractArrow a&&a.getOwner()==owner&&a.entityTags().contains(LbcMachinegunSkill.TAG)&&!originalBodies.contains(a.getUUID())){ownedIds.add(a.getUUID());shots.add(a);}
                    if(shots.isEmpty()){require(elapsed<8,"real high-charge native Machinegun publication");return false;}for(var a:shots){finalized(a,"actual Machinegun");require(a.getDeltaMovement().length()>10,"actual Machinegun source velocity remains above10 after1.3 trait");}require(automaticBow.getDamageValue()==0,"automatic pulse preserves final-release durability economy");beginAura();
                }
                case 12->{for(var e:auraBodies)require(!e.isNoGravity()||game.items().timeStop().frozen(e),"source TimeStop holds owned aura mutation");if(elapsed<4)return false;game.items().timeStop().end(level);stopped=false;for(var e:auraBodies)require(!e.isNoGravity(),"thaw reveals owned bodies had no aura mutation during pause");frozenBody.entityTags().add("time_stopped");next(13);}
                case 13->{
                    if(auraBodies.stream().anyMatch(e->!e.isNoGravity())){require(elapsed<20,"bounded fair native aura serves each nearby mutable body");return false;}
                    for(var foreign:foreignBodies)require(!foreign.isNoGravity(),"Gravitas never adopts foreign/neutral nearby ammo or TNT");
                    require(!outside.isNoGravity()&&!protectedBody.isNoGravity()&&!frozenBody.isNoGravity(),"aura excludes outside3, canonical protected shop role and individually frozen body");for(var e:auraBodies)require(!(e instanceof Projectile p)||!p.entityTags().contains(LbcProjectileLevelups.FINALIZED),"owned aura grants gravity only, never direct speed/damage finalization");
                    game.lbc().clear(owner);move(owner,origin.add(8,0,0));next(14);
                }
                case 14->{if(elapsed<2)return false;for(var e:auraBodies)require(e.isNoGravity(),"source Gravitas persists after owner departure and selection clear");return true;}
                case 40->{
                    if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;require(resolved.image(owner.getUUID()).equals(replayChoice),"captured actual resolved choice unchanged");
                    discardOwned();game.lbc().clear(owner);require(resolved.image(owner.getUUID()).effectiveMask()==0,"future active choice layer really removed");originalRandom.nextLong();originalRandom.nextLong();game.items().mana().set(owner,777);require(game.vanity().requestRestoreForCheck(),"actual whole coordinator restore request");restoring=true;next(41);
                }
                case 41->{
                    if(game.vanity().busy())return false;require(resolved.image(owner.getUUID()).equals(replayChoice),"actual whole coordinator restores exact resolved choice/scalars");require(originalRandom.nextLong()==expectedNextRandom,"actual native private random next draw restored");
                    require(game.items().projectileLevelups().checkpoint().owners().contains(owner.getUUID())&&game.items().timeStop().stopped(level),"actual enrolled aura and source pause boundary restored");
                    for(var e:replayBodies.entrySet())require(level.getEntity(e.getKey()) instanceof AbstractArrow a&&a.getOwner()==owner&&coefficient(a)==e.getValue().coefficient()&&a.entityTags().contains(LbcProjectileLevelups.FINALIZED),"actual native arrow identity/owner/coefficient/finalization rebound");
                    game.vanity().stop();game.items().timeStop().end(level);stopped=false;
                    for(var e:replayBodies.entrySet()){var a=(AbstractArrow)level.getEntity(e.getKey());require(a.isNoGravity()==e.getValue().noGravity()&&a.getDeltaMovement().equals(e.getValue().motion()),"actual thaw restores finalized native motion/gravity once");}return true;
                }
                default->throw new IllegalStateException("remaining-levelup fixture stage "+stage);
            }
            return false;
        }
        @Override public void close(){
            if(closed)return;closed=true;Throwable failure=null;
            for(Runnable action:List.<Runnable>of(
                ()->{if(kind==2)game.vanity().stop();},()->{if(stopped)game.items().timeStop().end(level);},
                ()->{owner.stopUsingItem();game.items().machinegunSkill().clear(owner);game.lbc().clear(owner);game.lbc().clear(other);},this::discardOwned,
                ()->{nativeRandom();originalRandomState.restore(originalRandom);images.clear();images.putAll(originalImages);},
                ()->{game.config().lbcLevelups=levelups;game.config().lbcSkills=skills;game.config().ranked=ranked;game.config().lbcLevelupLimit=limit;saved.restoreAttributes();otherSaved.restoreAttributes();saved.restore(game);otherSaved.restore(game);saved.restoreUse();otherSaved.restoreUse();game.items().projectileLevelups().reconcile(owner);game.items().projectileLevelups().reconcile(other);game.items().magic().inventoryChanged(owner);game.items().magic().inventoryChanged(other);},
                client::close,audience::close,attribution::close))try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("Remaining levelup scoped restoration",failure);
        }
    }
}
