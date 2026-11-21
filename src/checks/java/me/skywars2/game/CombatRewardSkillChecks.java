package me.skywars2.game;

import java.util.*;
import java.lang.reflect.Field;
import me.skywars2.SkyWars2;
import me.skywars2.game.effects.magic.LbcMagic;
import me.skywars2.game.gui.GameMenus;
import me.skywars2.game.gui.loadouts.LbcLoadouts;
import me.skywars2.game.items.gadgets.LbcImbaNativeItems;
import me.skywars2.game.items.tmt.LbcTmtAbilities;
import me.skywars2.game.items.tmt.LbcTmtItems;
import me.skywars2.game.items.trinkets.LbcTrinkets;
import me.skywars2.game.items.weapons.ranged.LbcGunAccuracy;
import me.skywars2.game.items.weapons.ranged.LbcMadokaBow;
import me.skywars2.game.items.weapons.ranged.LbcShulkerBow;
import me.skywars2.game.match.checkpoints.MatchPlayerCheckpoint;
import me.skywars2.game.players.attributes.LbcAttributeGains;
import me.skywars2.game.presentation.projectiles.WeaponSpreadVisuals;
import me.skywars2.game.skills.LbcCombatRewards;
import me.skywars2.game.skills.LbcMachinegunSkill;
import me.skywars2.game.world.maps.MapManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Scoped actual native kill/drop and bow-use checks. Explicit helper calls never advance time. */
public final class CombatRewardSkillChecks {
    private CombatRewardSkillChecks() {}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Combat reward skills: "+why);}
    private static void near(double got,double expected,String why){require(Math.abs(got-expected)<1e-5,why+" got="+got+" expected="+expected);}
    private static Field field(Object target,String name){for(Class<?> type=target.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new IllegalStateException("Missing fixture field "+name);}
    private static Map<Field,Object> attackers(ServerPlayer p){var result=new LinkedHashMap<Field,Object>();try{for(String name:List.of("lastHurtByMob","lastHurtByMobTimestamp","lastHurtByPlayer","lastHurtByPlayerMemoryTime","lastHurtMob","lastHurtMobTimestamp")){var f=field(p,name);result.put(f,f.get(p));}}catch(IllegalAccessException e){throw new IllegalStateException(e);}return result;}
    private static void restoreAttackers(ServerPlayer p,Map<Field,Object> image){try{for(var entry:image.entrySet())entry.getKey().set(p,entry.getValue());}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    public static void run(GameController game){try(var f=new Fixture(game)){f.initialize();f.menu();f.gains();f.absorption();f.nativeMobKills();f.nestedLoot();f.suppression();}SkyWars2.LOGGER.info("[check] PASS combat-reward-skills (source menus, all26 base gains, positive native donor surplus, real credited mob death and exact published stacks)");}

    public static void runMergedLoot(GameController game){
        try(var fixture=new Fixture(game)){
            fixture.initialize();
            var ordinary=fixture.seededLootDeath(false,false,false);
            require(ordinary.equals(new CowLoot(2,1)),"fixed-seed vanilla cow table really rolls beef2/leather1: "+ordinary);
            require(fixture.seededLootDeath(false,true,false).equals(ordinary),"unskilled prepublication merge credits the original quantities once");
            var skilled=fixture.seededLootDeath(true,true,false);
            require(skilled.equals(ordinary.doubled()),"skill18 live prepublication merge delta: actual="+skilled+" expected="+ordinary.doubled());
            require(fixture.seededLootDeath(true,false,false).equals(ordinary.doubled()),"skill18 duplicates ordinary published loot exactly once");
            var sameDeath=fixture.seededLootDeath(false,false,true);
            require(sameDeath.equals(new CowLoot(5,1)),"normal native beef2 and extra native beef3 produce one total of five: "+sameDeath);
            var sameDeathSkilled=fixture.seededLootDeath(true,false,true);
            require(sameDeathSkilled.equals(sameDeath.doubled()),"skill18 copies both original contributions once even after one mutates the published body: actual="
                    +sameDeathSkilled+" expected="+sameDeath.doubled());
        }
        SkyWars2.LOGGER.info("[check] PASS combat-reward-merged-loot (native fixed-seed Cow deaths, old compatible bodies, exact skill/no-skill quantities, ordinary publication, two contributions in one death and recursion guard)");
    }

    private record CowLoot(int beef,int leather){
        CowLoot doubled(){return new CowLoot(beef*2,leather*2);}
    }

    private interface Staged extends AutoCloseable { boolean step(); String group(); void close(); }
    private static Staged running;
    public static void runMachinegun(GameController game){require(running==null,"exclusive staged native check");var f=new BowFixture(game);running=f;try{f.initialize();f.begin();}catch(RuntimeException|Error error){running=null;f.close();throw error;}}
    public static void runPlayerDeath(GameController game){require(running==null,"exclusive staged native check");var f=new PlayerDeathFixture(game);running=f;try{f.initialize();f.begin();}catch(RuntimeException|Error error){running=null;f.close();throw error;}}
    public static void tick(GameController game){if(running==null)return;var f=running;try{if(f.step()){running=null;f.close();SkyWars2.LOGGER.info("[check] PASS {} (registered native updates and exact scoped restoration)",f.group());}}catch(RuntimeException|Error error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}

    private static class Fixture implements AutoCloseable {
        final GameController game;ServerPlayer owner,other;final net.minecraft.server.level.ServerLevel level;
        final RegenerationBeltChecks.Snapshot saved,otherSaved;final Map<Field,Object> ownerAttackers,otherAttackers;
        final boolean skills,ranked,frozen;final int cooldown,otherCooldown,pvpMode;
        final Set<Entity> actors=Collections.newSetFromMap(new IdentityHashMap<>()),originalEntities=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();
        RegenerationBeltChecks.ScheduledClient client,audience;Vec3 at;Marker jammer;boolean closed;
        Fixture(GameController game){this.game=game;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));require(owner!=null&&other!=null&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other)&&owner.level()==other.level(),"two actual same-world participants");saved=new RegenerationBeltChecks.Snapshot(game,owner);otherSaved=new RegenerationBeltChecks.Snapshot(game,other);ownerAttackers=attackers(owner);otherAttackers=attackers(other);level=owner.level();level.getAllEntities().forEach(originalEntities::add);skills=game.config().lbcSkills;ranked=game.config().ranked;frozen=game.server().tickRateManager().isFrozen();pvpMode=game.config().pvpMode;cooldown=owner.damageCooldownTime;otherCooldown=other.damageCooldownTime;require(game.items().combatRewards().pendingCount()==0,"exclusive empty native drop queue");}
        void select(int id){game.players().get(owner.getUUID()).lbcSkill=id;require(game.lbc().hasSkill(owner,id),"canonical live skill"+id);}
        void initialize(){game.config().pvpMode=0;game.config().lbcSkills=true;game.config().ranked=true;game.server().tickRateManager().setFrozen(false);for(var p:List.of(owner,other)){p.closeContainer();p.stopUsingItem();p.getInventory().clearContent();p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);p.getAbilities().invulnerable=false;p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.setKnownMovement(Vec3.ZERO);p.setLastHurtByPlayer((net.minecraft.world.entity.player.Player)null,0);p.setLastHurtByMob(null);p.entityTags().removeAll(Set.of("time_stopped","spectator","strict_map_object","magic_academy","rcdenyalways","sin_greed_active","nomagic_active"));var profile=game.players().get(p.getUUID());profile.lbcKit=0;profile.lbcChallengeKit=0;profile.lbcSkill=0;profile.lbcChallenge=0;profile.lbcTrinket=0;Arrays.fill(profile.levelups,0);Arrays.fill(profile.trinkets,0);resetBases(p);p.setHealth(p.getMaxHealth());p.setAbsorptionAmount(0);game.items().magic().inventoryChanged(p);game.items().armorSets().inventoryChanged(p);}game.items().magic().flushCleansing();int x=(owner.blockPosition().getX()>>4)*16+8,z=(owner.blockPosition().getZ()>>4)*16+8,y=Math.min(230,level.getMaxY()-8);for(int dx=-6;dx<=6;dx++)for(int dz=-6;dz<=6;dz++)for(int dy=0;dy<=3;dy++){var pos=new BlockPos(x+dx,y+dy,z+dz);require(level.hasChunkAt(pos),"resident fixture terrain");terrain.put(pos,game.maps().snapshot(level,pos));level.setBlock(pos,(dy==0?Blocks.STONE:Blocks.AIR).defaultBlockState(),2);}at=new Vec3(x+.5,y+1,z+.5);move(owner,at);move(other,at.add(0,0,5));client=new RegenerationBeltChecks.ScheduledClient(game,owner);audience=new RegenerationBeltChecks.ScheduledClient(game,other);}
        void move(ServerPlayer p,Vec3 pos){p.teleportTo(level,pos.x,pos.y,pos.z,Set.of(),0,-45,true);if(p==owner&&client!=null)client.acknowledge();if(p==other&&audience!=null)audience.acknowledge();}
        void resetBases(ServerPlayer p){var defaults=DefaultAttributes.getSupplier(EntityTypes.PLAYER);for(var holder:BuiltInRegistries.ATTRIBUTE.listElements().toList()){var a=p.getAttribute(holder);if(a!=null&&defaults.hasAttribute(holder)){a.removeModifiers();a.setBaseValue(holder.equals(Attributes.MAX_HEALTH)?game.config().health:defaults.getBaseValue(holder));}}}
        Map<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>,Double> bases(ServerPlayer p){var out=new LinkedHashMap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>,Double>();for(var holder:BuiltInRegistries.ATTRIBUTE.listElements().toList()){var a=p.getAttribute(holder);if(a!=null)out.put(holder,a.getBaseValue());}return out;}
        void menu(){require(GameController.operator(owner),"real operator debug menu entry");game.config().ranked=false;var profile=game.players().get(owner.getUUID());for(var row:List.of(new Object[]{14,13,"lbc:true_advancements/bows/minigun"},new Object[]{16,15,"lbc:true_advancements/hoes/death_skytche"},new Object[]{18,17,"lbc:true_advancements/hoes/socializm"})){int id=(Integer)row[0],slot=(Integer)row[1];String gate=(String)row[2];game.menus().openDebug(owner);owner.entityTags().remove("skywars2.debug_perks");require(game.canUseMenus(owner)&&!game.debugPerks(owner),"ordinary real source gate context");var adv=game.server().getAdvancements().get(Identifier.parse(gate));require(adv!=null,"source advancement "+gate);profile.lbcSkill=0;profile.lbcUnlocks.remove(gate);for(var criterion:adv.value().criteria().keySet())owner.getAdvancements().revoke(adv,criterion);require(!game.lbc().select(owner,LbcLoadouts.Kind.SKILLS,10000,slot),"locked source slot"+slot);for(var criterion:adv.value().criteria().keySet())owner.getAdvancements().award(adv,criterion);require(game.lbc().select(owner,LbcLoadouts.Kind.SKILLS,10000,slot)&&profile.lbcSkill==id,"completed original gate"+id);game.menus().open(owner,GameMenus.Page.LBC_SKILLS);owner.entityTags().remove("skywars2.debug_perks");require(eu.pb4.sgui.api.SguiUtils.getCurrentGui(owner) instanceof eu.pb4.sgui.api.gui.SimpleGui,"actual skill GUI");var gui=(eu.pb4.sgui.api.gui.SimpleGui)eu.pb4.sgui.api.SguiUtils.getCurrentGui(owner);var button=gui.getGuiElement(slot);require(button!=null,"source slot button"+slot);profile.lbcSkill=0;button.getGuiCallback().click(slot,eu.pb4.sgui.api.ClickType.MOUSE_LEFT,net.minecraft.world.inventory.ContainerInput.PICKUP,gui);require(profile.lbcSkill==id,"actual GUI selects"+id);owner.closeContainer();}game.config().ranked=true;}
        void gains(){var attrs=List.of(Attributes.ARMOR,Attributes.ARMOR_TOUGHNESS,Attributes.ATTACK_DAMAGE,Attributes.ATTACK_KNOCKBACK,Attributes.ATTACK_SPEED,Attributes.BLOCK_BREAK_SPEED,Attributes.BLOCK_INTERACTION_RANGE,Attributes.BURNING_TIME,Attributes.ENTITY_INTERACTION_RANGE,Attributes.EXPLOSION_KNOCKBACK_RESISTANCE,Attributes.FALL_DAMAGE_MULTIPLIER,Attributes.JUMP_STRENGTH,Attributes.KNOCKBACK_RESISTANCE,Attributes.LUCK,Attributes.MAX_HEALTH,Attributes.MINING_EFFICIENCY,Attributes.MOVEMENT_EFFICIENCY,Attributes.OXYGEN_BONUS,Attributes.SAFE_FALL_DISTANCE,Attributes.SCALE,Attributes.SNEAKING_SPEED,Attributes.STEP_HEIGHT,Attributes.SUBMERGED_MINING_SPEED,Attributes.SWEEPING_DAMAGE_RATIO,Attributes.WATER_MOVEMENT_EFFICIENCY,Attributes.MOVEMENT_SPEED);double[] scales={1,1,10,10,5,10,5,10,5,10,10,1000,10,1,1,3,10,10,5,100,10,10,20,20,10,100000},steps={1,1,1,1,1,1,1,-1,1,1,-1,1,10,1,2,1,1,1,1,1,1,1,1,1,1,100},outputs={1,1,.1,.1,.2,.1,.2,.1,.2,.1,.1,.001,.1,1,1,.333333333,.1,.1,.2,.01,.1,.1,.05,.05,.1,.00001};for(int i=0;i<26;i++){resetBases(owner);var before=bases(owner);var attr=attrs.get(i);LbcAttributeGains.apply(owner,i+1);near(owner.getAttributeBaseValue(attr),((int)(before.get(attr)*scales[i])+steps[i])*outputs[i],"source positive26 draw"+(i+1));for(var e:before.entrySet())if(!e.getKey().equals(attr))near(owner.getAttributeBaseValue(e.getKey()),e.getValue(),"one base only draw"+(i+1));}resetBases(owner);}
        void absorption(){resetBases(owner);resetBases(other);var defaults=bases(owner);for(var e:defaults.entrySet()){var a=other.getAttribute(e.getKey());if(DefaultAttributes.getSupplier(EntityTypes.PLAYER).hasAttribute(e.getKey()))a.setBaseValue(e.getValue()+.125);}other.getAttribute(Attributes.MAX_HEALTH).setBaseValue(game.config().health+12);other.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.5);other.getAttribute(Attributes.LUCK).setBaseValue(-4);other.getAttribute(Attributes.ARMOR).addTransientModifier(new AttributeModifier(Identifier.parse("skywars2:combat_reward_fixture"),500,AttributeModifier.Operation.ADD_VALUE));game.items().combatRewards().absorb(owner,other,false);near(owner.getAttributeBaseValue(Attributes.MAX_HEALTH),game.config().health+12,"HP surplus over configured match baseline");near(owner.getAttributeBaseValue(Attributes.MOVEMENT_SPEED),.5,"full positive movement surplus");near(owner.getAttributeBaseValue(Attributes.ARMOR),.125,"native modifiers excluded from donation");near(owner.getAttributeBaseValue(Attributes.LUCK),defaults.get(Attributes.LUCK),"negative donor base does not transfer");near(owner.getAttributeBaseValue(Attributes.ATTACK_SPEED),defaults.get(Attributes.ATTACK_SPEED),"explicit helper attack speed exclusion");for(var e:defaults.entrySet())if(!Set.of(Attributes.MAX_HEALTH,Attributes.MOVEMENT_SPEED,Attributes.LUCK,Attributes.ATTACK_SPEED).contains(e.getKey())&&DefaultAttributes.getSupplier(EntityTypes.PLAYER).hasAttribute(e.getKey()))near(owner.getAttributeBaseValue(e.getKey()),e.getValue()+.125,"all positive default-backed base surplus "+e.getKey());resetBases(owner);game.items().combatRewards().absorb(owner,other,true);near(owner.getAttributeBaseValue(Attributes.ATTACK_SPEED),defaults.get(Attributes.ATTACK_SPEED)+.125,"author includes attack speed surplus");resetBases(owner);resetBases(other);game.config().pvpMode=1;other.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(100000);game.items().combatRewards().absorb(owner,other,true);near(owner.getAttributeBaseValue(Attributes.ATTACK_SPEED),defaults.get(Attributes.ATTACK_SPEED),"PvP1 default has no donor surplus");other.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(100003);game.items().combatRewards().absorb(owner,other,true);near(owner.getAttributeBaseValue(Attributes.ATTACK_SPEED),defaults.get(Attributes.ATTACK_SPEED)+3,"PvP1 transfers only positive surplus over100000");game.config().pvpMode=0;resetBases(owner);resetBases(other);}
        Cow cow(){var c=new Cow(EntityTypes.COW,level);c.setPos(at.add(2,0,0));c.setNoAi(true);c.setNoGravity(true);require(level.addFreshEntity(c),"native victim cow");actors.add(c);return c;}
        List<ItemEntity> items(){var out=new ArrayList<ItemEntity>();for(var e:level.getAllEntities())if(e instanceof ItemEntity item&&!item.isRemoved())out.add(item);return out;}

        private ItemEntity oldLoot(Vec3 position,ItemStack stack){
            var item=new ItemEntity(level,position.x,position.y,position.z,stack);
            item.setDefaultPickUpDelay();
            try { field(item,"age").setInt(item,120); }
            catch(IllegalAccessException error){throw new IllegalStateException("Set real old item age",error);}
            actors.add(item);
            require(level.addFreshEntity(item)&&level.getEntity(item.getUUID())==item&&item.getAge()==120,
                    "already-published native resident stack with original age120: "+stack);
            return item;
        }

        private List<ItemEntity> nearbyCowLoot(Vec3 position){
            var beef=new ItemStack(Items.BEEF);var leather=new ItemStack(Items.LEATHER);
            return items().stream().filter(item->item.position().distanceToSqr(position)<4*4
                    &&(ItemStack.isSameItemSameComponents(item.getItem(),beef)
                    ||ItemStack.isSameItemSameComponents(item.getItem(),leather))).toList();
        }

        private CowLoot cowLoot(Vec3 position){
            int beef=0,leather=0;
            for(var item:nearbyCowLoot(position)){
                if(item.getItem().is(Items.BEEF))beef+=item.getItem().getCount();
                else leather+=item.getItem().getCount();
            }
            return new CowLoot(beef,leather);
        }

        CowLoot seededLootDeath(boolean skilled,boolean merge,boolean secondBeefDrop){
            game.players().get(owner.getUUID()).lbcSkill=skilled?18:0;
            require(game.lbc().hasSkill(owner,18)==skilled,"live skill18 mode matches the requested control");
            require(!game.items().timeStop().stopped(level)&&game.items().combatRewards().pendingCount()==0,
                    "native merging stays active and there is no pending unrelated reward");
            var position=at.add(2,0,0);
            require(nearbyCowLoot(position).isEmpty(),"exclusive native old-stack and publication scene");
            ItemEntity beef=null,leather=null,incompatible=null;
            var victim=new Cow(EntityTypes.COW,level){
                @Override public long getLootTableSeed(){return 61983L;}

                @Override protected void dropCustomDeathLoot(ServerLevel world,DamageSource source,boolean killedByPlayer){
                    super.dropCustomDeathLoot(world,source,killedByPlayer);
                    if(!secondBeefDrop)return;
                    var first=nearbyCowLoot(position).stream().filter(item->item.getItem().is(Items.BEEF)).findFirst().orElseThrow();
                    require(world.getEntity(first.getUUID())==first&&first.getItem().getCount()==2,
                            "the first beef2 body really published earlier in this native death");
                    var incoming=spawnAtLocation(world,new ItemStack(Items.BEEF,3));
                    require(incoming!=null&&incoming.isRemoved()&&world.getEntity(incoming.getUUID())==null
                            &&world.getEntity(first.getUUID())==first&&first.getItem().getCount()==5,
                            "the real second beef3 drop merges before publication into the first body's existing beef2");
                }
            };
            victim.setPos(position);victim.setNoAi(true);victim.setNoGravity(true);actors.add(victim);
            try {
                if(merge){
                    beef=oldLoot(position,new ItemStack(Items.BEEF,11));
                    leather=oldLoot(position,new ItemStack(Items.LEATHER,17));
                    var named=new ItemStack(Items.BEEF,9);
                    named.set(DataComponents.CUSTOM_NAME,Component.literal("Independent cow-loot fixture control"));
                    incompatible=oldLoot(position,named);
                }
                require(level.addFreshEntity(victim),"native fixed-seed cow before genuine death");
                if(skilled&&!merge)owner.entityTags().add("time_stopped");
                require(victim.hurtServer(level,victim.damageSources().playerAttack(owner),1000)
                        &&victim.isDeadOrDying()&&victim.getKillCredit()==owner,"actual credited lethal cow hit runs native loot table");
                if(skilled&&!merge){
                    var originals=nearbyCowLoot(position);
                    require(originals.size()==2&&originals.stream().allMatch(item->level.getEntity(item.getUUID())==item)
                            &&cowLoot(position).equals(new CowLoot(secondBeefDrop?5:2,1))&&game.items().combatRewards().pendingCount()==1,
                            "actual clean publication registers the two original bodies before the skill copies");
                    owner.entityTags().remove("time_stopped");
                    game.items().combatRewards().tick();
                }

                var result=cowLoot(position);
                if(merge){
                    var bodies=nearbyCowLoot(position);
                    require(bodies.size()==2&&bodies.contains(beef)&&bodies.contains(leather)
                            &&level.getEntity(beef.getUUID())==beef&&level.getEntity(leather.getUUID())==leather,
                            "original native bodies absorb the compatible drops before entity publication");
                    require(incompatible.getItem().getCount()==9&&incompatible.getAge()==120,
                            "older incompatible beef body is left untouched by both merge and reward");
                }
                for(int tick=0;tick<2;tick++)game.items().combatRewards().tick();
                require(cowLoot(position).equals(result)&&game.items().combatRewards().pendingCount()==0,
                        "published or absorbed skill copies do not enqueue or recursively copy themselves");
                return merge?new CowLoot(result.beef()-11,result.leather()-17):result;
            } finally {
                owner.entityTags().remove("time_stopped");
                for(var item:nearbyCowLoot(position)){actors.add(item);item.discard();}
                if(incompatible!=null&&!incompatible.isRemoved())incompatible.discard();
                if(!victim.isRemoved())victim.discard();
            }
        }
        private Cow seededRewardCow(long seed){
            var body=new Cow(EntityTypes.COW,level){@Override public long getLootTableSeed(){return seed;}};
            body.setPos(at.add(2,0,0));body.setNoAi(true);body.setNoGravity(true);
            require(level.addFreshEntity(body),"real fixed-seed cow for this native reward case");actors.add(body);
            return body;
        }

        private void killRewardCow(Cow victim,String phase){
            require(victim.hurtServer(level,victim.damageSources().playerAttack(owner),1000)
                    &&victim.isDeadOrDying()&&victim.getKillCredit()==owner,"actual credited native death: "+phase);
        }

        private CowLoot rewardLoot(){
            var position=at.add(2,0,0);
            for(var item:items())if(item.position().distanceToSqr(position)<4*4){
                var stack=item.getItem();
                if(stack.is(Items.BEEF)||stack.is(Items.LEATHER))
                    require(ItemStack.isSameItemSameComponents(stack,new ItemStack(stack.getItem())),
                            "vanilla native cow-loot components remain exact: "+stack);
            }
            return cowLoot(position);
        }

        private void requireRewardIncrease(CowLoot before,CowLoot expected,String phase){
            var after=rewardLoot();var actual=new CowLoot(after.beef()-before.beef(),after.leather()-before.leather());
            require(actual.equals(expected),phase+": actual="+actual+" expected="+expected);
        }

        void nativeMobKills(){
            select(16);resetBases(owner);var before=bases(owner);
            killRewardCow(seededRewardCow(61983L),"ordinary Soul Hunter");
            require(bases(owner).entrySet().stream().filter(e->!Objects.equals(e.getValue(),before.get(e.getKey()))).count()==1,
                    "one positive26 native reward per actual mob kill");

            resetBases(owner);select(18);
            var emeralds=new ItemStack(Items.EMERALD,11);
            emeralds.set(DataComponents.CUSTOM_NAME,Component.literal("Unrelated native newborn control"));
            var fresh=new ItemEntity(level,at.x+4,at.y,at.z,emeralds.copy());
            fresh.setNoPickUpDelay();require(level.addFreshEntity(fresh),"unrelated newborn native item");actors.add(fresh);
            var beforeLoot=rewardLoot();owner.entityTags().add("time_stopped");
            try {
                killRewardCow(seededRewardCow(61983L),"one paused skill18 batch");
                requireRewardIncrease(beforeLoot,new CowLoot(2,1),"original native quantities are admitted while the owner is paused");
                require(game.items().combatRewards().pendingCount()==1,"one paused victim batch captures all successful original drops");
            } finally { owner.entityTags().remove("time_stopped"); }
            game.items().combatRewards().tick();
            requireRewardIncrease(beforeLoot,new CowLoot(4,2),"one exact component-matching copy per native death drop");
            require(level.getEntity(fresh.getUUID())==fresh&&ItemStack.matches(fresh.getItem(),emeralds)&&fresh.getAge()==0
                    &&game.items().combatRewards().pendingCount()==0,"unrelated newborn excluded and duplicate children never recaptured");
            game.items().combatRewards().tick();
            requireRewardIncrease(beforeLoot,new CowLoot(4,2),"second worker tick cannot copy children again");
        }
        void suppression(){for(String tag:List.of("stopper_magic","stopper")){jammer=new Marker(EntityTypes.MARKER,level);jammer.setPos(owner.position());jammer.entityTags().add(tag);require(level.addFreshEntity(jammer),"native suppression field");actors.add(jammer);game.items().magic().loaded(jammer);try{require(tag.equals("stopper_magic")?game.items().magic().suppressed(owner):game.items().magic().mechanismSuppressed(owner),"field positive control");select(16);var before=bases(owner);var victim=cow();require(victim.hurtServer(level,victim.damageSources().playerAttack(owner),1000),"native kill under jammer");require(!before.equals(bases(owner)),"skill16 entirely unjammed");unjammedLoot();}finally{unjam();}}other.getInventory().setItem(9,new ItemStack(LbcMagic.MYSTICAL_ARTIFACT));game.items().magic().inventoryChanged(other);game.items().magic().flushCleansing();try{require(game.items().magic().suppressed(owner),"real strong artifact positive control");select(16);var before=bases(owner);var victim=cow();require(victim.hurtServer(level,victim.damageSources().playerAttack(owner),1000)&&!before.equals(bases(owner)),"skill16 strong artifact independent");unjammedLoot();}finally{other.getInventory().clearContent();game.items().magic().inventoryChanged(other);game.items().magic().flushCleansing();}}
        void unjam(){if(jammer!=null){game.items().magic().unloaded(jammer);jammer.discard();jammer=null;}}
        void nestedLoot(){
            select(18);var outer=seededRewardCow(61983L);var inner=seededRewardCow(3L);var before=rewardLoot();
            owner.entityTags().add("time_stopped");
            try {
                game.items().combatRewards().withDeathLoot(outer,level,outer.damageSources().playerAttack(owner),()->{
                    killRewardCow(outer,"native outer scoped death");
                    requireRewardIncrease(before,new CowLoot(2,1),"outer victim original amounts before the inner death");
                    killRewardCow(inner,"native nested scoped death");
                });
                require(game.items().combatRewards().pendingCount()==2,"nested native deaths retain separate owner batches");
                requireRewardIncrease(before,new CowLoot(5,3),"both original native rolls remain distinct contributions");
            } finally { owner.entityTags().remove("time_stopped"); }
            game.items().combatRewards().tick();
            requireRewardIncrease(before,new CowLoot(10,6),"nested victims duplicate their exact amounts once without cross capture");
            require(game.items().combatRewards().pendingCount()==0,"both nested deferred batches settle");
        }

        void unjammedLoot(){
            select(18);var before=rewardLoot();
            killRewardCow(seededRewardCow(61983L),"native loot during suppression");
            requireRewardIncrease(before,new CowLoot(4,2),"immediate native loot has one exact skill copy during suppression");
            require(game.items().combatRewards().pendingCount()==0,"unpaused native death copies are immediate and entirely unjammed");
        }
        @Override public void close(){if(closed)return;closed=true;Throwable failure=null;for(Runnable action:List.<Runnable>of(this::unjam,()->{owner.entityTags().remove("time_stopped");owner.stopUsingItem();other.stopUsingItem();game.items().combatRewards().clear();},()->{for(var e:level.getAllEntities())if(!originalEntities.contains(e)&&(e instanceof ItemEntity||e instanceof net.minecraft.world.entity.ExperienceOrb)&&at!=null&&e.position().distanceToSqr(at)<16*16)actors.add(e);for(var e:actors){if(e instanceof net.minecraft.world.entity.LivingEntity living)game.items().indirectKills().clear(living);if(e instanceof Marker)game.items().magic().unloaded(e);e.discard();}},()->{if(client!=null)client.close();if(audience!=null)audience.close();},()->terrain.forEach((p,s)->game.maps().restoreSnapshot(level,p,s)),()->{game.config().pvpMode=pvpMode;game.config().lbcSkills=skills;game.config().ranked=ranked;saved.restore(game);otherSaved.restore(game);},()->{saved.restoreAttributes();otherSaved.restoreAttributes();saved.restoreUse();otherSaved.restoreUse();owner.damageCooldownTime=cooldown;other.damageCooldownTime=otherCooldown;restoreAttackers(owner,ownerAttackers);restoreAttackers(other,otherAttackers);},()->{game.items().magic().inventoryChanged(owner);game.items().magic().inventoryChanged(other);game.items().magic().flushCleansing();game.items().armorSets().inventoryChanged(owner);game.items().armorSets().inventoryChanged(other);game.server().tickRateManager().setFrozen(frozen);game.players().save();}))try{action.run();}catch(Throwable e){if(failure==null)failure=e;else failure.addSuppressed(e);}if(failure!=null)throw new IllegalStateException("Combat reward fixture cleanup",failure);}
    }
    private static final class BowFixture extends Fixture implements Staged {
        int stage,updates,started,charge;Set<Entity> previous;ItemStack bow;
        BowFixture(GameController game){super(game);}
        public String group(){return "machinegun-skill";}
        List<AbstractArrow> arrows(){var out=new ArrayList<AbstractArrow>();for(var e:level.getAllEntities())if(e instanceof AbstractArrow arrow&&!arrow.isRemoved()&&!previous.contains(e)){out.add(arrow);actors.add(arrow);}return out;}
        void factory(){
            select(14);owner.getInventory().setSelectedSlot(0);owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BOW));owner.getInventory().setItem(9,new ItemStack(Items.ARROW,10));
            var source=owner.getInventory().getItem(9);source.set(DataComponents.CUSTOM_NAME,Component.literal("factory supplied components"));
            game.items().mana().score(owner,"bow_charge").set(2300);game.items().mana().score(owner,"machinegun_shots").set(100);
            try{var randomField=field(owner,"random");var original=randomField.get(owner);var shoot=LbcMachinegunSkill.class.getDeclaredMethod("shoot",ServerPlayer.class);shoot.setAccessible(true);
                for(boolean consume:new boolean[]{false,true}){int[] rolls={0};var before=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(before::add);int count=source.getCount();randomField.set(owner,java.lang.reflect.Proxy.newProxyInstance(net.minecraft.util.RandomSource.class.getClassLoader(),new Class<?>[]{net.minecraft.util.RandomSource.class},(proxy,method,args)->{if(method.getName().equals("nextBoolean")){rolls[0]++;return consume;}try{return method.invoke(original,args);}catch(java.lang.reflect.InvocationTargetException error){throw error.getCause();}}));try{require((Boolean)shoot.invoke(game.items().machinegunSkill(),owner),"actual automatic arrow factory publishes");}finally{randomField.set(owner,original);}require(rolls[0]==1&&source.getCount()==count-(consume?1:0),"one source50percent boolean ammo decision per successful pulse");Arrow result=null;for(var e:level.getAllEntities())if(!before.contains(e)&&e instanceof Arrow arrow){require(result==null,"one automatic factory arrow");result=arrow;actors.add(arrow);}require(result!=null&&result.getOwner()==owner&&!result.isCritArrow(),"fresh noncritical native owned arrow");require(ItemStack.matches(result.getPickupItemStackOrigin(),source.copyWithCount(1)),"factory preserves exact supplied normal arrow components");require(ItemStack.matches(result.getWeaponItem(),owner.getMainHandItem()),"factory preserves actual current native weapon snapshot");var direction=owner.getLookAngle();near(result.getDeltaMovement().x,direction.x*LbcMachinegunSkill.speed(2300),"zero-spread exact X motion");near(result.getDeltaMovement().y,direction.y*LbcMachinegunSkill.speed(2300),"zero-spread exact Y motion");near(result.getDeltaMovement().z,direction.z*LbcMachinegunSkill.speed(2300),"zero-spread exact Z motion");result.discard();}
            }catch(ReflectiveOperationException error){throw new IllegalStateException("Native automatic arrow factory",error);}finally{game.items().mana().score(owner,"bow_charge").set(0);game.items().mana().score(owner,"bow_charge_sub").set(0);game.items().mana().score(owner,"machinegun_shots").set(0);}
        }
        void specialAmmo(){
            int limit=game.config().lbcTrinketLimit;game.config().lbcTrinketLimit=99;
            try{
                select(14);owner.getInventory().setSelectedSlot(0);
                var shoot=LbcMachinegunSkill.class.getDeclaredMethod("shoot",ServerPlayer.class);shoot.setAccessible(true);
                for(var flag:List.of("madoka_bow","shulker_new"))for(var item:List.of(Items.TIPPED_ARROW,Items.SPECTRAL_ARROW)){
                    owner.getInventory().clearContent();
                    var weapon=new ItemStack(Items.BOW);var data=new net.minecraft.nbt.CompoundTag();data.putInt(flag,1);
                    data.putString("independent_payload","exact automatic weapon");
                    weapon.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));
                    var source=new ItemStack(item,3);source.set(DataComponents.CUSTOM_NAME,Component.literal("exact automatic special ammo"));
                    if(item==Items.TIPPED_ARROW){source.set(DataComponents.POTION_CONTENTS,new net.minecraft.world.item.alchemy.PotionContents(Optional.empty(),Optional.of(0x123456),List.of(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS,160,2)),Optional.empty()));source.set(DataComponents.POTION_DURATION_SCALE,.25f);}
                    var expected=source.copyWithCount(1);
                    var hand=item==Items.TIPPED_ARROW?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;
                    owner.setItemInHand(hand,weapon);owner.setItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND,source);
                    var reserve=new ItemStack(Items.ARROW,10);owner.getInventory().setItem(9,reserve);
                    owner.getInventory().setItem(10,new ItemStack(LbcTrinkets.BEDROCK_BALLS,99));owner.getInventory().setItem(11,new ItemStack(LbcTrinkets.COCKTAIL,99));
                    require(owner.gameMode.useItem(owner,level,weapon,hand).consumesAction()&&owner.isUsingItem(),"native either-hand special automatic draw");
                    var before=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(before::add);
                    require((Boolean)shoot.invoke(game.items().machinegunSkill(),owner),"actual special automatic factory publishes");
                    var family=new ArrayList<Projectile>();for(var e:level.getAllEntities())if(!before.contains(e)){actors.add(e);if(e instanceof Projectile projectile&&!projectile.isRemoved())family.add(projectile);}
                    require(family.size()==1,"one actual automatic special projectile family");var shot=family.getFirst();
                    require(shot.getOwner()==owner&&shot.entityTags().containsAll(List.of("bedrock_arrow","cocktail_arrow")),"both independent trinkets augment actual special automatic carrier");
                    if(flag.equals("shulker_new")){
                        require(LbcShulkerBow.managed(shot),"automatic Shulker replaces exact primary arrow");
                        var state=((LbcShulkerBow.Carrier)shot).skywars2$shulkerState();
                        require(ItemStack.matches(state.ammo,expected)&&ItemStack.matches(state.weapon,weapon),"automatic Shulker preserves exact special ammo and current weapon");
                        require((state.spectral>0)==(item==Items.SPECTRAL_ARROW),"automatic Shulker preserves native spectral payload");
                    }else{
                        require(shot instanceof AbstractArrow,"automatic Madoka keeps native arrow");var arrow=(AbstractArrow)shot;
                        require((arrow instanceof SpectralArrow)==(item==Items.SPECTRAL_ARROW)&&ItemStack.matches(arrow.getPickupItemStackOrigin(),expected)&&ItemStack.matches(arrow.getWeaponItem(),weapon)&&LbcMadokaBow.recognizes(arrow),"automatic Madoka preserves native special ammo, components and weapon");
                    }
                    require(reserve.getCount()==10&&(source.getCount()==2||source.getCount()==3)&&weapon.getDamageValue()==0,"automatic special draw chooses held ammo and preserves50percent economy/final-release durability");
                    shot.discard();owner.stopUsingItem();game.items().machinegunSkill().clear(owner);
                }
            }catch(ReflectiveOperationException error){throw new IllegalStateException("Native automatic special arrow factory",error);}
            finally{owner.stopUsingItem();owner.getInventory().clearContent();game.config().lbcTrinketLimit=limit;game.items().machinegunSkill().clear(owner);}
        }
        void sharedContinuations(){
            select(14);owner.stopUsingItem();owner.getInventory().clearContent();owner.getInventory().setSelectedSlot(0);
            int limit=game.config().lbcTrinketLimit;game.config().lbcTrinketLimit=99;
            try{
                var shoot=LbcMachinegunSkill.class.getDeclaredMethod("shoot",ServerPlayer.class);shoot.setAccessible(true);
                for(boolean tmt:List.of(false,true)){
                    var weapon=tmt?LbcTmtItems.prototype(1):new ItemStack(Items.BOW);var data=new net.minecraft.nbt.CompoundTag();
                    if(tmt){data.putInt("tmt_item",1);var abilities=new ArrayList<LbcTmtAbilities.Ability>();
                        for(var flag:List.of("double_old","madoka_bow")){var property=new net.minecraft.nbt.CompoundTag();property.putInt(flag,1);
                            abilities.add(new LbcTmtAbilities.Ability("native/check_"+flag,net.minecraft.world.item.component.CustomData.of(property)));}
                        LbcTmtAbilities.write(data,abilities);
                    }else data.putInt("fastfired",1);
                    weapon.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));
                    owner.setItemInHand(InteractionHand.MAIN_HAND,weapon);var ammo=new ItemStack(Items.ARROW,64);
                    ammo.set(DataComponents.CUSTOM_NAME,Component.literal("shared automatic native ammo"));owner.getInventory().setItem(9,ammo);
                    owner.getInventory().setItem(10,new ItemStack(LbcTrinkets.BEDROCK_BALLS,99));
                    require(owner.gameMode.useItem(owner,level,weapon,InteractionHand.MAIN_HAND).consumesAction(),"actual shared-continuation bow draw");
                    var before=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(before::add);
                    require((Boolean)shoot.invoke(game.items().machinegunSkill(),owner),"actual automatic accepted continuation");
                    var family=new ArrayList<Projectile>();for(var e:level.getAllEntities())if(!before.contains(e)){
                        actors.add(e);if(e instanceof Projectile shot&&!e.isRemoved())family.add(shot);
                    }
                    if(tmt){require(family.size()==2,"TMT automatic accepted volley expands through native double owner");
                        for(var shot:family)require(shot instanceof AbstractArrow arrow&&LbcMadokaBow.recognizes(arrow)
                                &&shot.getOwner()==owner&&shot.entityTags().contains("bedrock_arrow")
                                &&ItemStack.matches(arrow.getPickupItemStackOrigin(),ammo.copyWithCount(1)),
                                "both TMT outputs retain independent Madoka, once-rolled trinket and exact ammo");
                        require(LbcTmtAbilities.read(weapon).size()==2,"automatic continuation retains both private TMT properties");
                    }else require(family.size()==1&&family.getFirst() instanceof net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball
                            &&family.getFirst().getOwner()==owner,"automatic legacy bow converts its accepted arrow to the real fireball");
                    family.forEach(Entity::discard);owner.stopUsingItem();game.items().machinegunSkill().clear(owner);owner.getInventory().clearContent();
                }
            }catch(ReflectiveOperationException error){throw new IllegalStateException("Shared automatic bow continuations",error);}
            finally{owner.stopUsingItem();game.items().machinegunSkill().clear(owner);owner.getInventory().clearContent();game.config().lbcTrinketLimit=limit;}
        }
        void acceptedShots(){
            select(14);owner.stopUsingItem();owner.getInventory().clearContent();owner.getInventory().setSelectedSlot(0);
            var service=game.items().machinegunSkill();service.clear(owner);
            var weapon=new ItemStack(Items.BOW);owner.setItemInHand(InteractionHand.MAIN_HAND,weapon);
            owner.getInventory().setItem(9,new ItemStack(Items.ARROW,64));owner.getInventory().setItem(10,new ItemStack(Items.ARROW,64));
            require(owner.gameMode.useItem(owner,level,weapon,InteractionHand.MAIN_HAND).consumesAction(),"actual draw for accepted-arrow telemetry");
            owner.setYRot(37);owner.setXRot(-19);game.items().mana().score(owner,"bow_charge").set(200);
            var first=WeaponSpreadVisuals.sample(game,owner);require(first!=null&&Math.abs(first.degrees()-LbcGunAccuracy.vanillaDegrees(1))<1e-5,"ordinary bow indicator before first automatic shot");
            var known=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(known::add);
            try{var method=LbcMachinegunSkill.class.getDeclaredMethod("shoot",ServerPlayer.class);method.setAccessible(true);
                for(int n=1;n<=100;n++){
                    require((Boolean)method.invoke(service,owner)&&service.shots(owner)==n,"actual accepted arrow counter "+n);
                    int count=0;for(var e:level.getAllEntities())if(known.add(e)&&e instanceof AbstractArrow arrow){actors.add(arrow);count++;}
                    require(count==1,"one real published pulse for the hundred-arrow progression");
                    var aim=WeaponSpreadVisuals.sample(game,owner);require(aim!=null&&Math.abs(aim.degrees()-service.aimDegrees(owner))<1e-5,"machinegun overrides ordinary bow from first accepted arrow "+n);
                }
                require(service.aimDegrees(owner)==0,"exact zero reticle after100 actual arrows");
                game.items().mana().score(owner,"bow_charge").set(10000);
                weapon.enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FLAME),1);
                require((Boolean)method.invoke(service,owner),"actual late linear-speed arrow");
                AbstractArrow last=null;for(var e:level.getAllEntities())if(known.add(e)&&e instanceof AbstractArrow arrow){actors.add(arrow);require(last==null,"single late native arrow");last=arrow;}
                require(last!=null&&last.getDeltaMovement().distanceTo(owner.getLookAngle().scale(51))<1e-10,"late speed51 and truly straight zero-spread flight");
                require(last.isOnFire(),"automatic arrows run native projectile-spawn enchantment callbacks");
                owner.stopUsingItem();owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BOW));owner.setItemInHand(InteractionHand.OFF_HAND,weapon);
                require(owner.gameMode.useItem(owner,level,weapon,InteractionHand.OFF_HAND).consumesAction(),"actual offhand automatic draw beside a mainhand bow");
                require((Boolean)method.invoke(service,owner),"first accepted arrow of the new offhand draw");
                var offhandAim=WeaponSpreadVisuals.sample(game,owner);
                require(offhandAim!=null&&offhandAim.hand()==InteractionHand.OFF_HAND&&Math.abs(offhandAim.degrees()-service.aimDegrees(owner))<1e-5,
                        "active offhand automatic indicator overrides idle mainhand bow from its first shot");
                owner.stopUsingItem();owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.MAIN_HAND,weapon);
                require(owner.gameMode.useItem(owner,level,weapon,InteractionHand.MAIN_HAND).consumesAction(),"resume native mainhand draw");
                owner.getInventory().clearContent();owner.setItemInHand(InteractionHand.MAIN_HAND,weapon);owner.getInventory().setItem(9,new ItemStack(LbcImbaNativeItems.INFINITE_ARROW));
                for(int n=0;n<12;n++)require((Boolean)method.invoke(service,owner)&&owner.getInventory().getItem(9).getCount()==1,"only selected infinite arrow stays free");
                for(var e:level.getAllEntities())if(known.add(e)&&e instanceof AbstractArrow arrow)actors.add(arrow);
                owner.getInventory().setItem(9,ItemStack.EMPTY);int count=service.shots(owner);
                require(!(Boolean)method.invoke(service,owner)&&service.shots(owner)==count,"failed no-ammo pulse does not count as a shot");
            }catch(ReflectiveOperationException error){throw new IllegalStateException("Actual hundred-arrow progression",error);}
            finally{owner.stopUsingItem();service.clear(owner);owner.getInventory().clearContent();}
        }
        void begin(){
            specialAmmo();sharedContinuations();acceptedShots();
            for(int p=0;p<=100;p++){require(LbcMachinegunSkill.interval(p)==Math.max(1,20-p),"safe declining burst interval"+p);require(LbcMachinegunSkill.arrowsPerBurst(p)==1+Math.max(0,p-19)/20,"terminal burst count grows linearly per20 completed saturated bursts"+p);}
            near(LbcMachinegunSkill.speed(200),2,"source velocity formula");near(LbcMachinegunSkill.spread(50),1000,"halfway accepted-arrow XYZ spread");near(LbcMachinegunSkill.spread(100),0,"spread stops after100 accepted arrows");
            near(LbcMachinegunSkill.speed(9799),49.995,"source high-charge velocity retained");near(LbcMachinegunSkill.speed(9800),50,"linear speed continues above old discontinuity");for(String tag:List.of("stopper_magic","stopper")){var marker=new Marker(EntityTypes.MARKER,level);marker.setPos(owner.position());marker.entityTags().add(tag);require(level.addFreshEntity(marker),"native machinegun suppression marker");actors.add(marker);game.items().magic().loaded(marker);}other.getInventory().setItem(9,new ItemStack(LbcMagic.MYSTICAL_ARTIFACT));game.items().magic().inventoryChanged(other);game.items().magic().flushCleansing();require(game.items().magic().suppressed(owner)&&game.items().magic().mechanismSuppressed(owner),"MAG/TECH/strong artifact active throughout native automatic draw");factory();
            select(14);game.items().machinegunSkill().clear(owner);owner.getInventory().setSelectedSlot(0);bow=new ItemStack(Items.BOW);owner.setItemInHand(InteractionHand.MAIN_HAND,bow);var ammo=new ItemStack(Items.ARROW,64);ammo.set(DataComponents.CUSTOM_NAME,Component.literal("ordinary id custom components"));owner.getInventory().setItem(9,ammo);previous=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(previous::add);updates=client.updates();started=game.server().getTickCount();stage=0;
        }
        void draw(){require(owner.gameMode.useItem(owner,level,bow,InteractionHand.MAIN_HAND).consumesAction()&&owner.isUsingItem(),"actual native survival bow use");updates=client.updates();}
        String drawDiagnostics(){var service=game.items().machinegunSkill();return "elapsed="+(client.updates()-updates)+" server="+game.server().getTickCount()+" playerTick="+owner.tickCount+" using="+owner.isUsingItem()+" remaining="+owner.getUseItemRemainingTicks()+" usedRef="+(owner.getUseItem()==bow)+" handRef="+(owner.getMainHandItem()==bow)+" charge="+service.charge(owner)+" progress="+service.progress(owner)+" pending="+service.pending(owner)+" hasSkill="+game.lbc().hasSkill(owner,14)+" playerFrozen="+game.items().timeStop().frozen(owner)+" worldFrozen="+game.items().timeStop().stopped(level)+" runsNormally="+game.server().tickRateManager().runsNormally()+" tickFrozen="+game.server().tickRateManager().isFrozen()+" tags="+owner.entityTags()+" native="+client.diagnostics();}
        public boolean step(){require(game.server().getTickCount()-started<150,"bounded native bow clock");client.acknowledge();audience.acknowledge();int elapsed=client.updates()-updates;switch(stage){
            case 0->{if(elapsed<2)return false;draw();stage=1;}
            case 1->{if(game.items().machinegunSkill().charge(owner)<39)require(game.items().machinegunSkill().shots(owner)==0,"machinegun cannot fire before its extra20-tick warmup");if(elapsed<41)return false;require(game.items().machinegunSkill().progress(owner)>0,"actual automatic burst completes under MAG/TECH/strong artifact: "+drawDiagnostics());for(var actor:actors)if(actor instanceof Marker marker&&marker.entityTags().contains("stopper")){game.items().magic().unloaded(marker);marker.discard();var position=marker.blockPosition();require(terrain.containsKey(position),"converted native jammer stays inside scoped terrain");game.maps().restoreSnapshot(level,position,terrain.get(position));}require(!game.items().magic().mechanismSuppressed(owner),"separate native flight from shared mechanical arrow deletion");updates=client.updates();stage=6;}
            case 6->{if(elapsed<21)return false;var shots=arrows();require(!shots.isEmpty(),"real using-item native updates publish automatic arrows: "+drawDiagnostics());for(var arrow:shots){require(arrow.getOwner()==owner&&arrow.entityTags().contains("killedarrow"),"automatic arrow has genuine owner/source tag");require(arrow.getPickupItemStackOrigin().is(Items.ARROW)&&Component.literal("ordinary id custom components").equals(arrow.getPickupItemStackOrigin().get(DataComponents.CUSTOM_NAME)),"automatic arrow preserves supplied normal ammo custom components");}require(bow.getDamageValue()==0,"automatic pulses preserve final-release durability path");charge=game.items().machinegunSkill().charge(owner);require(charge>=19,"native draw counter advances");owner.entityTags().add("time_stopped");updates=client.updates();stage=2;}
            case 2->{if(elapsed<3)return false;require(game.items().machinegunSkill().charge(owner)==charge,"manual Time Stop freezes charge and cadence");owner.entityTags().remove("time_stopped");updates=client.updates();stage=3;}
            case 3->{if(elapsed<3)return false;require(game.items().machinegunSkill().charge(owner)>charge,"real draw resumes after Time Stop");int before=arrows().size();owner.releaseUsingItem();require(!owner.isUsingItem()&&bow.getDamageValue()==1,"actual final native release occurs exactly once");require(arrows().size()==before+1,"final native release publishes one additional arrow");updates=client.updates();stage=4;}
            case 4->{if(elapsed<2)return false;require(game.items().machinegunSkill().charge(owner)==0&&game.items().machinegunSkill().progress(owner)==0,"release clears both source counters");owner.getInventory().setItem(9,new ItemStack(Items.TIPPED_ARROW,64));draw();updates=client.updates();stage=5;charge=arrows().size();}
            case 5->{if(elapsed<43)return false;require(arrows().size()>charge,"tipped-only inventory supplies actual automatic native arrows");for(var arrow:arrows())if(arrow.entityTags().contains(LbcMachinegunSkill.TAG)&&arrow.getPickupItemStackOrigin().is(Items.TIPPED_ARROW))require(arrow.getWeaponItem()!=null&&arrow.getWeaponItem().is(Items.BOW),"native automatic tipped shot retains real bow");owner.stopUsingItem();game.items().machinegunSkill().clear(owner);return true;}
            default->throw new IllegalStateException("Bow stage");}return false;}
        @Override public void close(){try{owner.entityTags().remove("time_stopped");game.items().machinegunSkill().clear(owner);if(previous!=null)arrows();}finally{super.close();}}
    }

    private static final class PlayerDeathFixture extends Fixture implements Staged {
        final GameController.Checkpoint core;final int killed,killedBy;ServerPlayer retired;int stage,started,lives;Set<Entity> previous;
        PlayerDeathFixture(GameController game){super(game);require(!game.developerMode(),"ordinary finite player death fixture");core=game.checkpointCore();killed=owner.getStats().getValue(net.minecraft.stats.Stats.ENTITY_KILLED.get(EntityTypes.PLAYER));killedBy=other.getStats().getValue(net.minecraft.stats.Stats.ENTITY_KILLED_BY.get(EntityTypes.PLAYER));}
        public String group(){return "combat-reward-player-death";}
        void begin(){
            select(18);started=game.server().getTickCount();lives=game.lives(other.getUUID());require(lives>1,"two native deaths leave match alive");other.setExperienceLevels(0);other.setExperiencePoints(0);other.totalExperience=0;
            var expected=new ArrayList<ItemStack>();
            var diamonds=new ItemStack(Items.DIAMOND,7);diamonds.set(DataComponents.CUSTOM_NAME,Component.literal("exact native player death stack"));other.getInventory().setItem(9,diamonds);expected.add(diamonds.copy());
            var slots=List.of(net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET,net.minecraft.world.entity.EquipmentSlot.MAINHAND,net.minecraft.world.entity.EquipmentSlot.OFFHAND);
            var gear=List.of(Items.IRON_HELMET,Items.IRON_CHESTPLATE,Items.IRON_LEGGINGS,Items.IRON_BOOTS,Items.DIAMOND_SWORD,Items.SHIELD);
            for(int i=0;i<gear.size();i++){
                var stack=new ItemStack(gear.get(i));stack.set(DataComponents.UNBREAKABLE,net.minecraft.util.Unit.INSTANCE);stack.setDamageValue(i+1);stack.set(DataComponents.CUSTOM_NAME,Component.literal("native loot "+slots.get(i).getName()));
                var tag=new net.minecraft.nbt.CompoundTag();tag.putInt("loot_fixture_slot",i);stack.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));other.setItemSlot(slots.get(i),stack);expected.add(stack.copy());
            }
            previous=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(previous::add);retired=other;owner.entityTags().add("time_stopped");other.setInvulnerableTime(0);other.damageCooldownTime=0;
            require(other.hurtServer(level,other.damageSources().playerAttack(owner),1000)&&other.isDeadOrDying()&&game.awaitingRespawn(other.getUUID()),"actual foreign-player native death");
            var originals=deathLoot();
            exactStacks(expected,originals,"one complete native inventory/armor/main-hand/offhand set");
            require(game.items().combatRewards().pendingCount()==1,"one paused native player death reward batch");
            owner.entityTags().remove("time_stopped");game.items().combatRewards().tick();
            exactAdditionalSet(expected,originals,"one complete additional set with identical counts, damage and components");
            game.items().combatRewards().tick();
            exactAdditionalSet(expected,originals,"two complete sets remain after the next reward tick");
            require(game.items().combatRewards().pendingCount()==0,"no additional native player death reward was enqueued");stage=0;
        }
        List<ItemStack> deathLoot(){
            var actual=new ArrayList<ItemStack>();
            for(var drop:items())if(!previous.contains(drop)){actual.add(drop.getItem().copy());actors.add(drop);}
            return actual;
        }
        void exactAdditionalSet(List<ItemStack> expected,List<ItemStack> originals,String why){
            var actual=deathLoot();
            for(var stack:actual)require(originals.stream().anyMatch(original->ItemStack.isSameItemSameComponents(original,stack)),
                    why+" unexpected native drop components: "+stack);
            var delta=new ArrayList<ItemStack>();
            for(var original:originals){
                int total=actual.stream().filter(stack->ItemStack.isSameItemSameComponents(original,stack)).mapToInt(ItemStack::getCount).sum();
                require(total>original.getCount(),why+" no extra items matching "+original);
                delta.add(original.copyWithCount(total-original.getCount()));
            }
            exactStacks(expected,delta,why);
        }
        void exactStacks(List<ItemStack> expected,List<ItemStack> actual,String why){
            var unmatched=new ArrayList<>(actual);require(unmatched.size()==expected.size(),why+" stack count: "+actual);
            for(var stack:expected){int found=-1;for(int i=0;i<unmatched.size();i++)if(ItemStack.matches(stack,unmatched.get(i))){found=i;break;}require(found>=0,why+" missing "+stack+" among "+unmatched);unmatched.remove(found);}
        }
        void respawn(){if(audience!=null){audience.close();audience=null;}other=game.server().getPlayerList().respawn(retired,false,Entity.RemovalReason.KILLED);require(other!=retired&&other.isAlive(),"genuine native replacement player");other.connection.resetPosition();var ack=(MatchPlayerCheckpoint.SimulatedAcknowledgement)other.connection;ack.skywars2$beginRespawn();ack.skywars2$acknowledgeRestore();audience=new RegenerationBeltChecks.ScheduledClient(game,other);audience.acknowledge();require(other.connection.hasClientLoaded(),"replacement client acknowledged native respawn before next attack");move(other,at.add(0,0,5));}
        public boolean step(){require(game.server().getTickCount()-started<100&&game.playing(),"bounded native player reward death");if(stage==0){respawn();require(game.lives(other.getUUID())==lives-1,"ordinary life loss retained for loot skill");require(other.hasEffect(net.minecraft.world.effect.MobEffects.RESISTANCE),"actual respawn protection is present");stage=2;return false;}if(stage==2){if(other.hasEffect(net.minecraft.world.effect.MobEffects.RESISTANCE))return false;select(16);resetBases(owner);resetBases(other);other.getAttribute(Attributes.ARMOR).setBaseValue(3.125);other.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(LbcCombatRewards.baseline(Attributes.ATTACK_SPEED,game.config())+2.25);other.getAttribute(Attributes.MAX_HEALTH).setBaseValue(game.config().health+12);other.setHealth(other.getMaxHealth());other.setInvulnerableTime(0);other.damageCooldownTime=0;retired=other;var before=bases(owner);require(other.hurtServer(level,other.damageSources().playerAttack(owner),1000)&&other.isDeadOrDying(),"real credited player death for Soul Hunter");var after=bases(owner);require(after.get(Attributes.ARMOR)>=3.125&&after.get(Attributes.MAX_HEALTH)>=game.config().health+12,"native exact victim surplus captured before player cleanup");require(after.get(Attributes.ATTACK_SPEED)>=before.get(Attributes.ATTACK_SPEED)+2.25,"actual native player kill transfers attack speed surplus");require(!before.equals(after),"actual player kill receives reward");stage=1;return false;}respawn();return true;}
        @Override public void close(){try{if(retired!=null&&other!=otherSaved.player){field(otherSaved,"player").set(otherSaved,other);field(otherSaved.state,"player").set(otherSaved.state,other);}game.server().tickRateManager().setFrozen(true);game.restoreCore(core);owner.getStats().setValue(owner,net.minecraft.stats.Stats.ENTITY_KILLED.get(EntityTypes.PLAYER),killed);other.getStats().setValue(other,net.minecraft.stats.Stats.ENTITY_KILLED_BY.get(EntityTypes.PLAYER),killedBy);}catch(IllegalAccessException e){throw new IllegalStateException("Replacement player snapshot",e);}finally{super.close();}}
    }
}
