package me.skywars2.game;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import me.skywars2.SkyWars2;
import me.skywars2.mixin.ShulkerBowMixin;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.*;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;

/** Native inputs and the remaining shared Quiver terminals, with the existing scoped fixture. */
public final class NativeQuiverArrowChecks {
    private static final List<String> NAMES=List.of("strong_arrow","weak_arrow","poison_arrow","reactive_arrow","explosive_arrow","evoker_arrow","paralyzing_arrow","electric_arrow","lightshoot_arrow","kiuaskivi_arrow","trapping_arrow");
    private static final List<String> SOURCES=List.of(
        "tipped_arrow__f6855c681848/variant_2db9ec05ff9f","tipped_arrow__a83b315ade86/variant_16be374fe64a",
        "tipped_arrow__129290638d78/variant_1df5252a74b4","tipped_arrow__2e91b33c6339/variant_4fe32bdc2571",
        "tipped_arrow__797b3af909ce/variant_d60afff3fc92","tipped_arrow__ba59a43bb29d/variant_5d58e57bac30",
        "tipped_arrow__c420ac48aee2/variant_8a94d060a1ae","tipped_arrow__93d954dbcaf5/variant_dd8e535af39f",
        "tipped_arrow__62efeb3693af","tipped_arrow__9355d6686bac","tipped_arrow__4d9d3a4c2b22");
    private static final boolean SINGLE_TICK_LIGHTSHOOT = Boolean.getBoolean("skywars2.check.lightshootSingleTick");
    private static final List<Integer> LIGHT_TNT_Y = List.of(0, 3, -1, -2, -3, -4, -5, -7, -9,
            -11, -13, -15, -17, -19, -21, -23, -25, -27, -29, -31, -33, -35, -37, -39, -41, -43, -45);
    private static final List<Integer> LIGHT_BOLT_Y = List.of(0, 3, -1, -2, -3, -4, -5, -6, -7, -9, -13, -17, -21, -25, -33, -45);
    private static Scene running;

    private static List<Integer> lightTntOffsets(int slice) {
        if (slice < 0 || slice >= 3) return List.of();
        if (SINGLE_TICK_LIGHTSHOOT) return slice == 0 ? LIGHT_TNT_Y : List.of();
        return LIGHT_TNT_Y.subList(slice * 9, (slice + 1) * 9);
    }

    private static boolean lightBoltSlice(int slice) {
        return slice == (SINGLE_TICK_LIGHTSHOOT ? 0 : 2);
    }
    private NativeQuiverArrowChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Native Quiver arrows: "+why);}
    public static void run(GameController game){require(running==null,"exclusive scene");var s=new Scene(game);running=s;try{s.begin();}catch(Throwable e){running=null;try{s.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}throw e;}}
    public static void tick(GameController game){var s=running;if(s==null)return;try{if(!s.step())return;running=null;s.close();SkyWars2.LOGGER.info("[check] PASS native-quiver-arrows (11 exact source prototypes/Polymer, both-hand native bows, elapsed crossbow charging, real dispensers, ownerless101/102/103/111/112/113, native fangs, nullable status reload/clone/columns, native Lightshoot blast frames, TimeStop/MAG/TECH and Stand ground return)");}catch(Throwable e){running=null;try{s.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL native-quiver-arrows",e);}}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final Set<UUID> initial=new HashSet<>();final MatchRandomState worldRandom;
        final Vec3 high;final BlockPos dispenser;final List<ItemStack> ammo=new ArrayList<>();final int began;
        ItemStack cross,selected;int index,when,phase;boolean closed;Cow fangVictim;
        final List<Long> lightTimings = new ArrayList<>();
        final List<Integer> lightBlastFrames = new ArrayList<>();
        final Set<UUID> lightChildren = new HashSet<>();
        Vec3 lightOrigin;
        Marker lightMarker;
        BlockPos lightControl;
        List<BlockPos> lightWitnesses;
        List<PrimedTnt> previousLightTnt = List.of();
        boolean savedTntExplodes, changedTntRule;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);high=f.home.add(0,500,0);dispenser=BlockPos.containing(f.home.add(0,20,0));began=game.server().getTickCount();worldRandom=MatchRandomState.capture(f.level.getRandom());for(var e:f.level.getAllEntities())initial.add(e.getUUID());
            require(game.items().quiver().trails().isEmpty()&&game.items().quiver().children().isEmpty(),"exclusive existing Quiver owners");}
        void begin(){f.prepareParticipants();f.move(f.owner,high);f.move(f.other,high.add(12,0,0));f.owner.getAbilities().invulnerable=true;f.other.getAbilities().invulnerable=true;
            prototypes();for(int i=0;i<ammo.size();i++)for(var hand:InteractionHand.values())f.discard(bow(i,hand));startCross();}
        void prototypes(){
            var params=new LootParams.Builder(f.level).withParameter(LootContextParams.ORIGIN,high).withOptionalParameter(LootContextParams.THIS_ENTITY,f.owner).create(LootContextParamSets.CHEST);
            var ops=game.server().reloadableRegistries().lookup().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
            var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)(Object)f.owner.connection;
            for(int i=0;i<NAMES.size();i++){var stack=f.leaf(NAMES.get(i));require(stack.is(LbcImbaNativeItems.quiverArrows().get(i))&&stack.getItem() instanceof TippedArrowItem&&stack.is(ItemTags.ARROWS),"native selectable identity "+i);
                try(var input=NativeQuiverArrowChecks.class.getResourceAsStream("/data/lbc/loot_table/item_defs/minecraft/"+SOURCES.get(i)+".json")){
                    var raw=JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(input),StandardCharsets.UTF_8)).getAsJsonObject();raw.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().addProperty("name","minecraft:tipped_arrow");
                    var expected=LootTable.DIRECT_CODEC.parse(ops,raw).getOrThrow().getRandomItems(params,17);require(expected.size()==1,"independent original-carrier oracle");
                    var comparable=new ItemStack(Items.TIPPED_ARROW);comparable.applyComponents(stack.getComponents());require(ItemStack.matches(comparable.copyWithCount(1),expected.getFirst().copyWithCount(1)),"all source/default components "+i);
                    var defaults=stack.getItem().getDefaultInstance();require(stack.getComponents().keySet().equals(defaults.getComponents().keySet()),"default/loot keys "+i);for(var c:stack.getComponents())require(Objects.equals(c.value(),defaults.get(c.type())),"default/loot component "+i+" "+c.type());
                }catch(java.io.IOException e){throw new IllegalStateException(e);}
                var proxy=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(stack,TooltipFlag.NORMAL,provider.getPacketContext(),f.owner.registryAccess());require(ItemStack.matches(stack,eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(proxy,provider.getPacketContext(),f.owner.registryAccess())),"Polymer roundtrip "+i);ammo.add(stack.copyWithCount(1));
            }
        }
        Set<UUID> ids(){var out=new HashSet<UUID>();for(var e:f.level.getAllEntities())out.add(e.getUUID());return out;}
        Arrow published(Set<UUID> before,ItemStack expected){var list=new ArrayList<Arrow>();for(var e:f.level.getAllEntities())if(e instanceof Arrow a&&!a.isRemoved()&&!before.contains(a.getUUID())){f.own(a);list.add(a);}require(list.size()==1,"one real native arrow");var a=list.getFirst();require(a.getOwner()==f.owner&&ItemStack.isSameItemSameComponents(a.getPickupItemStackOrigin(),expected),"actual shooter/ammo");return a;}
        Arrow bow(int i,InteractionHand hand){f.owner.stopUsingItem();f.owner.getInventory().clearContent();var weapon=new ItemStack(Items.BOW);var donor=ammo.get(i).copyWithCount(3);f.owner.setItemInHand(hand,weapon);f.owner.setItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND,donor);var before=ids();require(f.owner.gameMode.useItem(f.owner,f.level,weapon,hand).consumesAction()&&Items.BOW.releaseUsing(weapon,f.level,f.owner,71980),"both-hand bow input "+i);f.owner.stopUsingItem();var a=published(before,donor);require(donor.getCount()==2,"selected-only one debit");return a;}
        void startCross(){f.owner.stopUsingItem();f.owner.getInventory().clearContent();cross=new ItemStack(Items.CROSSBOW);selected=ammo.get(index).copyWithCount(3);f.owner.setItemInHand(InteractionHand.MAIN_HAND,cross);f.owner.setItemInHand(InteractionHand.OFF_HAND,selected);require(f.owner.gameMode.useItem(f.owner,f.level,cross,InteractionHand.MAIN_HAND).consumesAction()&&f.owner.isUsingItem(),"real crossbow charge starts");when=game.server().getTickCount();}
        boolean step(){f.client.acknowledge();f.audience.acknowledge();require(game.server().getTickCount()-began<650,"bounded native scene");
            if(phase==2)return stepLightshoot();
            if(phase==1){
                if(game.server().getTickCount()-when<12)return false;
                require(fangVictim.getHealth()<990&&fangVictim.getLastDamageSource().getDirectEntity() instanceof EvokerFangs&&fangVictim.getLastDamageSource().getEntity()==null,
                        "real scheduled native ownerless fang attack after arrow impact");
                startLightshoot();phase=2;when=game.server().getTickCount();return false;
            }
            if(game.server().getTickCount()-when<30)return false;require(CrossbowItem.isCharged(cross)&&selected.getCount()==2,"elapsed native charge "+index);f.owner.stopUsingItem();var before=ids();require(f.owner.gameMode.useItem(f.owner,f.level,cross,InteractionHand.MAIN_HAND).consumesAction(),"native charged crossbow fires");f.owner.stopUsingItem();var a=published(before,ammo.get(index));require(!CrossbowItem.isCharged(cross),"native chamber emptied");f.discard(a);
            if(++index<ammo.size()){startCross();return false;}payloads();phase=1;when=game.server().getTickCount();return false;
        }
        Arrow dispense(int i,Vec3 at){var a=OwnerlessImbaArrowChecks.dispense(f,dispenser,ammo.get(i),at);require(game.items().quiver().arrows().contains(a),"common native ownerless admission "+i);return a;}
        Cow cow(Vec3 at){var c=EntityTypes.COW.create(f.level,EntitySpawnReason.COMMAND);require(c!=null,"native cow");c.setPos(at);c.setNoAi(true);c.setNoGravity(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);c.setHealth(1000);require(f.level.addFreshEntity(c),"resident cow");f.own(c);return c;}
        void strike(Arrow a,Cow c){a.setPos(c.position().add(0,.5,-2));a.setDeltaMovement(0,0,4);a.tick();}
        void payloads(){
            f.set(dispenser,Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.SOUTH));
            for(int i=0;i<ammo.size();i++){var a=dispense(i,high.add(0,600+i*20,0));if(i==0||i==1)require(((ShulkerBowMixin.ArrowAccess)a).skywars2$baseDamage()==(i==0?5:1),"original5/1 damage coefficient");f.discard(a);}
            var poisoned=cow(high.add(0,140,0));var poison=dispense(2,poisoned.position());strike(poison,poisoned);require(poisoned.getEffect(MobEffects.POISON).getDuration()==20&&poisoned.getEffect(MobEffects.WITHER).getDuration()==5&&game.items().acidCorruption().deathRemaining(poisoned)==0,"ordinary poison160/40 scales natively, no IMBA delayed death");f.discard(poison);f.discard(poisoned);
            var para=cow(high.add(0,170,0));para.setNoAi(false);var paralyze=dispense(6,para.position());strike(paralyze,para);require(para.getEffect(MobEffects.SLOWNESS).getAmplifier()==127&&para.getEffect(MobEffects.SLOWNESS).getDuration()==5&&!para.isInvulnerable()&&!para.isNoAi(),"ordinary slowness/fatigue, no IMBA permanent stasis");f.discard(paralyze);f.discard(para);
            var explosion=cow(high.add(0,200,0));var explosive=dispense(4,explosion.position());strike(explosive,explosion);require(game.items().babylonEffects().queued(explosion,101)==1&&!explosion.hasEffect(MobEffects.RAID_OMEN),"real101 intake consumes native transport");game.items().babylonEffects().unloaded(explosion);game.items().babylonEffects().loaded(explosion);require(game.items().babylonEffects().queued(explosion,101)==1,"null101 contribution survives native attachment reload");explosion.setInvulnerableTime(0);explosion.damageCooldownTime=0;game.items().babylonEffects().tickTarget(explosion);require(game.items().babylonEffects().queued(explosion,101)==0&&explosion.getLastDamageSource().getEntity()==null&&game.items().babylonEffects().explicitOwner(explosion.getLastDamageSource())==null,"shared native101 blast without invented player");f.discard(explosive);f.discard(explosion);
            var shock=cow(high.add(0,230,0));var electric=dispense(7,shock.position());strike(electric,shock);require(game.items().quiver().queued(shock,103)==1,"real103 intake");game.items().quiver().unloaded(shock);game.items().quiver().loaded(shock);require(game.items().quiver().queued(shock,103)==1,"null103 contribution reload");game.items().quiver().tick();require(shock.hasEffect(MobEffects.SPEED)&&shock.hasEffect(MobEffects.WITHER)&&game.items().quiver().queued(shock,103)==0,"shared shock pulse runs without player");f.discard(electric);f.discard(shock);
            reloadedPolicy();terrain();trails();stand();groundGuards();
            fangVictim=cow(high.add(0,300,0));var evoker=dispense(5,fangVictim.position());strike(evoker,fangVictim);game.items().quiver().tick();require(game.items().quiver().children().stream().filter(e->e instanceof EvokerFangs&&!e.isRemoved()).count()>=9,"ownerless living102 emits complete native fang formation");fangVictim.setInvulnerableTime(0);fangVictim.damageCooldownTime=0;f.discard(evoker);
        }
        LbcIndirectKills.Owner queuedCause(LivingEntity target,int index){
            var pending=(Map<?,?>)WoolBucketChecks.read(game.items().quiver(),"pending");var queues=(List<?>)pending.get(target);var queue=(Deque<?>)queues.get(index);
            return (LbcIndirectKills.Owner)WoolBucketChecks.read(WoolBucketChecks.read(queue.peekFirst(),"authority"),"cause");
        }
        void reloadedPolicy(){
            var at=high.add(0,260,0);var victim=cow(at);var a=bow(7,InteractionHand.MAIN_HAND);strike(a,victim);
            require(game.items().quiver().queued(victim,103)==1,"real player-owned shock marker intake");game.items().quiver().unloaded(victim);game.items().quiver().loaded(victim);
            var cause=queuedCause(victim,1);require(cause.player().equals(f.owner.getUUID())&&!cause.friendlyFire()&&cause.projectile(),"reload retains separate friendship and projectile flags");
            var board=game.server().getScoreboard();var team=board.addPlayerTeam("quiver_reload_fixture");var before=f.other.position();
            try{board.addPlayerToTeam(f.owner.getScoreboardName(),team);board.addPlayerToTeam(f.other.getScoreboardName(),team);f.move(f.other,at.add(.4,0,0));game.items().quiver().tick();
                require(!f.other.hasEffect(MobEffects.WITHER)&&!f.other.hasEffect(MobEffects.BLINDNESS)&&victim.hasEffect(MobEffects.WITHER),"restored ordinary shock protects an actual allied player while affecting its victim");
            }finally{board.removePlayerFromTeam(f.owner.getScoreboardName());board.removePlayerFromTeam(f.other.getScoreboardName());board.removePlayerTeam(team);f.other.removeAllEffects();f.move(f.other,before);f.discard(a);f.discard(victim);}
            game.items().mana().score(f.owner,"effect_shock").set(2);game.items().quiver().adoptRawFoodStatuses(f.owner);game.items().quiver().unloaded(f.owner);game.items().quiver().loaded(f.owner);
            cause=queuedCause(f.owner,1);require(cause.friendlyFire()&&!cause.projectile(),"raw self-authored WILD counter retains its non-projectile flag on reload");game.items().quiver().clear(f.owner);
        }
        void terrain(){
            var center=dispenser.above(6);for(var p:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,1,1)))f.remember(p);
            f.set(center.below(),Blocks.STONE.defaultBlockState());f.set(center,Blocks.OAK_LOG.defaultBlockState());var a=dispense(9,Vec3.atCenterOf(center));game.items().quiver().beforeArrow(a);require(f.level.getBlockState(center).is(Blocks.FIRE),"source111 actual bounded terrain writes with null player and real level");f.discard(a);
            var highGround=dispense(9,high.add(0,800,0));game.items().quiver().ground(highGround);require(highGround.isRemoved(),"ground111 closes above build height without fake owner or invalid section access");
        }
        @SuppressWarnings("unchecked") Map<Marker,Object> trailStates(){return (Map<Marker,Object>)WoolBucketChecks.read(game.items().quiver(),"trails");}
        void age(Object state,int value){try{var field=state.getClass().getDeclaredField("age");field.setAccessible(true);field.setInt(state,value);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
        int age(Object state){return (Integer)WoolBucketChecks.read(state,"age");}
        void trails(){
            var a=dispense(8,high.add(0,60,0));game.items().quiver().beforeArrow(a);f.discard(a);var light=game.items().quiver().trails().stream().filter(m->m.entityTags().contains("lightarrowtrail")).findFirst().orElseThrow();f.own(light);age(trailStates().get(light),198);
            var image=game.items().quiver().cloneImage(light);var clone=new Marker(EntityTypes.MARKER,f.level);clone.setPos(light.position());clone.entityTags().addAll(light.entityTags());image.install(light,clone,Map.of(light,clone));require(f.level.addFreshEntity(clone),"actual cloned nullable trail publication");f.own(clone);
            f.pause();try{game.items().quiver().tick();require(age(trailStates().get(light))==198&&age(trailStates().get(clone))==198,"TimeStop holds both trail clocks");}finally{f.thaw();}
            int frames = SINGLE_TICK_LIGHTSHOOT ? 2 : 3;
            for (int slice = 0; slice < frames; slice++) {
                game.items().quiver().tick();
                var children = game.items().quiver().children().stream().filter(e -> e instanceof PrimedTnt || e instanceof LightningBolt).toList();
                checkLightChildren(children, light.position(), slice, true, 2);
                children.forEach(f::discard);
                if (slice == 0) {
                    f.pause();
                    try {
                        game.items().quiver().tick();
                        require(age(trailStates().get(light)) == 199 && age(trailStates().get(clone)) == 199
                                        && game.items().quiver().children().stream().noneMatch(e -> e instanceof PrimedTnt || e instanceof LightningBolt),
                                "TimeStop between column frames neither advances nor republishes children");
                    } finally { f.thaw(); }
                }
            }
            require(light.isRemoved() && clone.isRemoved(), "both ownerless trails retire after all exact 27 TNT and 16 lightning each");
            var trap=dispense(10,high.add(0,25,0));game.items().quiver().beforeArrow(trap);f.discard(trap);var marker=game.items().quiver().trails().stream().filter(m->m.entityTags().contains("trappertrail")).findFirst().orElseThrow();f.own(marker);var target=cow(marker.position().add(1,0,0));age(trailStates().get(marker),119);
            java.lang.reflect.Field field=null;for(Class<?> c=f.level.getClass();c!=null&&field==null;c=c.getSuperclass())try{field=c.getDeclaredField("random");}catch(NoSuchFieldException ignored){}require(field!=null,"native random seam");
            try{field.setAccessible(true);var original=(net.minecraft.util.RandomSource)field.get(f.level);var controlled=java.lang.reflect.Proxy.newProxyInstance(net.minecraft.util.RandomSource.class.getClassLoader(),new Class<?>[]{net.minecraft.util.RandomSource.class},(proxy,method,args)->{if(method.getName().equals("nextFloat"))return 0f;if(method.getName().equals("nextInt")&&args!=null&&args.length==1&&args[0].equals(3))return 1;try{return method.invoke(original,args);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}});field.set(f.level,controlled);try{int before=game.items().quiver().children().size();game.items().quiver().tick();require(game.items().quiver().children().size()>before,"source120 trapper creates native fang without player");}finally{field.set(f.level,original);}}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
            for(var child:game.items().quiver().children())f.discard(child);f.discard(marker);f.discard(target);
        }
        List<PrimedTnt> checkLightChildren(List<Entity> children, Vec3 origin, int slice, boolean ownerless, int copies) {
            var tntOffsets = new ArrayList<Integer>();
            var boltOffsets = new ArrayList<Integer>();
            var tntBodies = new ArrayList<PrimedTnt>();
            var quiver = game.items().quiver();
            var expectedOwner = ownerless ? null : f.owner;
            for (var child : children) {
                double yOffset = child.getY() - origin.y;
                int offset = (int)Math.round(yOffset);
                require(child.getX() == origin.x && child.getZ() == origin.z && Math.abs(yOffset - offset) < 1e-9
                                && f.level.getEntity(child.getUUID()) == child && game.items().currentEpoch(child),
                        "accepted native Lightshoot child retains its exact published position and epoch");
                var cause = quiver.cause(child);
                require(quiver.owner(child) == expectedOwner && (ownerless ? cause == null
                                : cause != null && cause.player().equals(expectedOwner.getUUID())
                                        && cause.projectile() && cause.friendlyFire() == (child instanceof LightningBolt)),
                        "Lightshoot child keeps native player or nullable authority and lightning's WILD policy");
                if (child instanceof PrimedTnt tnt) {
                    require(tnt.getClass() == PrimedTnt.class && tnt.getFuse() == 0 && tnt.getDeltaMovement().equals(Vec3.ZERO),
                            "each slice admits exact native zero-fuse stationary TNT");
                    tntOffsets.add(offset);
                    tntBodies.add(tnt);
                } else if (child instanceof LightningBolt bolt) {
                    require(bolt.getCause() == expectedOwner, "native lightning retains authorized player or null");
                    boltOffsets.add(offset);
                }
            }
            var expectedTnt = new ArrayList<Integer>();
            var expectedBolt = new ArrayList<Integer>();
            for (int i = 0; i < copies; i++) {
                expectedTnt.addAll(lightTntOffsets(slice));
                if (lightBoltSlice(slice)) expectedBolt.addAll(LIGHT_BOLT_Y);
            }
            Collections.sort(tntOffsets);
            Collections.sort(boltOffsets);
            Collections.sort(expectedTnt);
            Collections.sort(expectedBolt);
            require(tntOffsets.equals(expectedTnt) && boltOffsets.equals(expectedBolt),
                    "exact source offsets in slice " + slice + ": TNT=" + tntOffsets + " lightning=" + boltOffsets);
            tntBodies.sort(Comparator.comparingInt(Entity::getId));
            if (copies == 1) require(tntBodies.stream().map(tnt -> (int)Math.round(tnt.getY() - origin.y)).toList().equals(lightTntOffsets(slice)),
                    "retains authored TNT publication order");
            return tntBodies;
        }

        void startLightshoot() {
            var quiver = game.items().quiver();
            require(quiver.trails().isEmpty(), "physical Lightshoot scene starts with no other trails");
            var base = f.owner.blockPosition();
            var center = new BlockPos((base.getX() >> 4) * 16 + 8, f.level.getMaxY() - 32, (base.getZ() >> 4) * 16 + 8);
            lightOrigin = Vec3.atCenterOf(center);
            f.move(f.owner, lightOrigin.add(0, 0, -12));
            f.move(f.other, lightOrigin.add(12, 0, 0));
            require(f.level.hasChunkAt(center) && f.level.isPositionEntityTicking(center), "the actual blast column is warm and entity-ticking");
            savedTntExplodes = f.level.getGameRules().get(GameRules.TNT_EXPLODES);
            f.level.getGameRules().set(GameRules.TNT_EXPLODES, true, game.server());
            changedTntRule = true;

            var witness = new ArrayList<BlockPos>();
            for (int dy : List.of(0, -17, -35)) {
                var layer = center.below(-dy);
                for (BlockPos p : BlockPos.betweenClosed(layer.offset(-3, -2, -3), layer.offset(3, 2, 3))) {
                    require(f.level.getBlockState(p).isAir(), "seeded terrain uses empty sky " + p);
                    if (p.getX() == center.getX() && p.getZ() == center.getZ()) continue;
                    int hash = Math.floorMod(p.getX() * 19 + p.getY() * 11 + p.getZ() * 5, 7);
                    f.set(p, (hash < 3 ? Blocks.OAK_PLANKS : Blocks.STONE).defaultBlockState());
                }
                var breakable = layer.east();
                f.set(breakable, Blocks.STONE.defaultBlockState());
                witness.add(breakable);
            }
            for (int dy : LIGHT_BOLT_Y) {
                var at = center.offset(0, dy, 0);
                for (BlockPos p : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 1, 1))) f.remember(p);
            }
            lightWitnesses = List.copyOf(witness);
            lightControl = center.east(2);
            f.set(lightControl, Blocks.OBSIDIAN.defaultBlockState());

            var arrow = bow(8, InteractionHand.MAIN_HAND);
            arrow.setPos(lightOrigin);
            arrow.setNoGravity(true);
            arrow.setDeltaMovement(Vec3.ZERO);
            arrow.tick();
            f.discard(arrow);
            var markers = quiver.trails().stream().filter(marker -> marker.entityTags().contains("lightarrowtrail")).toList();
            require(markers.size() == 1 && markers.getFirst().position().equals(lightOrigin),
                    "true player bow and actual native arrow tick publish one canonical Lightshoot trail at the witness");
            lightMarker = markers.getFirst();
            f.own(lightMarker);
            age(trailStates().get(lightMarker), 198);
            f.level.getRandom().setSeed(0x51c08bL);
            f.client.packets().clear();
            f.audience.packets().clear();
        }

        List<ClientboundExplodePacket> explosionPackets(RegenerationBeltChecks.ScheduledClient receiver) {
            return receiver.packets().stream().filter(ClientboundExplodePacket.class::isInstance)
                    .map(ClientboundExplodePacket.class::cast).toList();
        }

        boolean stepLightshoot() {
            int tick = game.server().getTickCount();
            int frame = tick - when;
            require(frame == lightBlastFrames.size() + 1 && frame <= 5, "samples consecutive actual world ticks");
            if (frame > 1) {
                long elapsed = ServerTickMetrics.completedTickNanos(game.server(), tick - 1);
                require(elapsed > 0, "available completed whole native tick including the Fabric item loop");
                lightTimings.add(elapsed);
            }

            var originPackets = explosionPackets(f.client);
            var peerPackets = explosionPackets(f.audience);
            var centers = originPackets.stream().map(ClientboundExplodePacket::center).toList();
            var actualCenters = previousLightTnt.stream().map(body -> {
                require(body.isRemoved(), "published zero-fuse body detonates in exactly its next native tick");
                return new Vec3(body.getX(), body.getY(.0625), body.getZ());
            }).toList();
            require(centers.equals(actualCenters) && peerPackets.stream().map(ClientboundExplodePacket::center).toList().equals(actualCenters)
                            && originPackets.stream().allMatch(packet -> packet.radius() == 4)
                            && peerPackets.stream().allMatch(packet -> packet.radius() == 4),
                    "one real power-four explosion packet per body in both audiences at native centers, frame=" + frame
                            + " expected=" + actualCenters + " actual=" + centers);
            lightBlastFrames.add(centers.size());

            var newChildren = game.items().quiver().children().stream()
                    .filter(child -> !child.isRemoved() && (child instanceof PrimedTnt || child instanceof LightningBolt)
                            && lightChildren.add(child.getUUID())).toList();
            newChildren.forEach(f::own);
            previousLightTnt = checkLightChildren(newChildren, lightOrigin, frame - 1, false, 1);
            f.client.packets().clear();
            f.audience.packets().clear();
            if (frame < 5) return false;

            var expected = SINGLE_TICK_LIGHTSHOOT ? List.of(0, 27, 0, 0, 0) : List.of(0, 9, 9, 9, 0);
            require(lightBlastFrames.equals(expected) && lightChildren.size() == 43 && lightMarker.isRemoved(),
                    "complete physical 27 TNT and 16 lightning on exact active frames: " + lightBlastFrames);
            require(lightWitnesses.stream().allMatch(pos -> f.level.getBlockState(pos).isAir())
                            && f.level.getBlockState(lightControl).is(Blocks.OBSIDIAN),
                    "all three physical blast heights break adjacent seeded stone and preserve obsidian");
            SkyWars2.LOGGER.info("[check] native Lightshoot first publication + three burn frames: blasts={} wholeTickMs={}",
                    lightBlastFrames, lightTimings.stream().map(nanos -> nanos / 1_000_000d).toList());
            return true;
        }

        void stand(){var stack=f.loot("lbc:stand_arrow");var before=ids();var a=OwnerlessImbaArrowChecks.dispense(f,dispenser,stack,high.add(0,380,0));game.items().quiver().ground(a);var items=new ArrayList<ItemEntity>();for(var e:f.level.getAllEntities())if(e instanceof ItemEntity item&&!before.contains(e.getUUID())){items.add(item);f.own(item);}require(a.isRemoved()&&items.size()==1&&LbcStand.isArrow(items.getFirst().getItem())&&items.getFirst().getItem().getCount()==1,"unowned Stand ground returns exactly one real source arrow");for(var item:items)f.discard(item);}
        void groundGuards(){var at=high.add(0,410,0);var a=dispense(5,at);var target=cow(at.add(3,0,0));int count=game.items().quiver().children().size();f.field(false,at);try{game.items().quiver().ground(a);require(a.isRemoved()&&game.items().quiver().children().size()==count,"MAG suppresses nullable evoker ground terminal");}finally{f.removeFields();}f.discard(target);
            var b=dispense(7,at);target=cow(at.add(3,0,0));f.field(true,at);try{game.items().quiver().ground(b);require(b.isRemoved()&&!target.hasEffect(MobEffects.WITHER),"TECH suppresses nullable shock terminal");}finally{f.removeFields();}f.discard(target);}
        public void close(){if(closed)return;closed=true;try{var fresh=new ArrayList<Entity>();for(var e:f.level.getAllEntities())fresh.add(e);for(var e:fresh)if(!initial.contains(e.getUUID())){if(e instanceof LivingEntity living){game.items().quiver().clear(living);game.items().babylonEffects().clear(living);}f.own(e);}}finally{try{
            if (changedTntRule) f.level.getGameRules().set(GameRules.TNT_EXPLODES, savedTntExplodes, game.server());
            f.close();
        }finally{worldRandom.restore(f.level.getRandom());}}}
    }
}
