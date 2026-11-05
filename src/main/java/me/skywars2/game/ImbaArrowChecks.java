package me.skywars2.game;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import me.skywars2.SkyWars2;
import me.skywars2.mixin.ShulkerBowMixin;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.entity.projectile.arrow.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.*;

/** Actual native weapon input and payloads, using the existing scoped player/terrain fixture. */
public final class ImbaArrowChecks {
    private static final List<String> NAMES=List.of("strong","weak","poison","reactive","explosive","evoker","paralyzing","electric");
    private static final List<String> DEFS=List.of("6e933ca5dfdd","08d47b0cbea1","aeb24809f14b","542a1f35e483","92b1f1fe7523","4ffdef18a9cb","0cc65f50a29e","56437910eed7");
    private static Scene running;
    private ImbaArrowChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("IMBA arrows: "+why);}
    public static void run(GameController game){require(running==null,"exclusive native fixture");var s=new Scene(game);running=s;try{s.begin();}catch(Throwable error){running=null;try{s.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    public static void tick(GameController game){var s=running;if(s==null)return;try{if(!s.step())return;running=null;s.close();SkyWars2.LOGGER.info("[check] PASS native-imba-arrows (8 independent source prototypes/Polymer, real native both-hand bow and charged crossbow input, actual high critical/poison/paralysis contacts, reactive native TNT/TimeStop/TECH, WILD vacuum and virtual visuals, shared electrical/nuclear terminals, beam/shulker carriers)");}catch(Throwable error){running=null;try{s.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL native-imba-arrows",error);}}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final Set<UUID> initial=new HashSet<>();final MatchRandomState worldRandom;
        final List<ItemStack> ammo=new ArrayList<>();final Vec3 high;final Collection<?> traces;final int began;int index,when;ItemStack cross,selected;boolean closed,nuclear;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);high=f.home.add(0,500,0);began=game.server().getTickCount();worldRandom=MatchRandomState.capture(f.level.getRandom());
            for(var e:f.level.getAllEntities())initial.add(e.getUUID());traces=(Collection<?>)WoolBucketChecks.read(game.items().wands(),"electroTraces");
            require(traces.isEmpty()&&game.items().nukeEvent().jobCount()==0&&game.items().nukeEvent().markerCount()==0&&game.items().danmaku().cards().isEmpty()&&game.items().danmaku().bombs().isEmpty(),"exclusive relevant ability owners");}
        boolean dispenser(Item item){try{var field=DispenserBlock.class.getDeclaredField("DISPENSER_REGISTRY");field.setAccessible(true);return ((Map<?,?>)field.get(null)).containsKey(item);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
        void begin(){f.prepareParticipants();f.move(f.owner,high);f.move(f.other,high.add(12,0,0));f.owner.getAbilities().invulnerable=true;f.other.getAbilities().invulnerable=true;
            presentation();for(int i=0;i<ammo.size();i++)for(var hand:InteractionHand.values())bow(i,hand);startCross();}
        void presentation(){var params=new LootParams.Builder(f.level).withParameter(LootContextParams.ORIGIN,high).withOptionalParameter(LootContextParams.THIS_ENTITY,f.owner).create(LootContextParamSets.CHEST);var ops=game.server().reloadableRegistries().lookup().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
            var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)(Object)f.owner.connection;
            for(int i=0;i<NAMES.size();i++){var stack=f.leaf("imba_"+NAMES.get(i)+"_arrow");require(stack.is(LbcImbaNativeItems.specialArrows().get(i))&&stack.getItem() instanceof TippedArrowItem&&stack.is(ItemTags.ARROWS),"native selected ammunition identity/tag "+NAMES.get(i));require(stack.getCount()>=1&&stack.getCount()<=10,"source1..10 quantity "+i);
                try(var stream=ImbaArrowChecks.class.getResourceAsStream("/data/lbc/loot_table/item_defs/minecraft/tipped_arrow__"+DEFS.get(i)+".json")){
                    var raw=JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(stream),StandardCharsets.UTF_8)).getAsJsonObject();raw.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().addProperty("name","minecraft:tipped_arrow");
                    var expected=LootTable.DIRECT_CODEC.parse(ops,raw).getOrThrow().getRandomItems(params,17);require(expected.size()==1,"one independent original-carrier source output");var comparable=new ItemStack(Items.TIPPED_ARROW);comparable.applyComponents(stack.getComponents());
                    require(ItemStack.matches(comparable.copyWithCount(1),expected.getFirst().copyWithCount(1)),"all source/default components preserved "+NAMES.get(i));var defaults=stack.getItem().getDefaultInstance();require(stack.getComponents().keySet().equals(defaults.getComponents().keySet()),"give/loot component keys agree "+i);for(var c:stack.getComponents())require(Objects.equals(c.value(),defaults.get(c.type())),"give/loot property "+i+" "+c.type()+" expected="+defaults.get(c.type())+" actual="+c.value());
                }catch(java.io.IOException error){throw new IllegalStateException(error);}
                var proxy=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(stack,TooltipFlag.NORMAL,provider.getPacketContext(),f.owner.registryAccess());require(ItemStack.matches(stack,eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(proxy,provider.getPacketContext(),f.owner.registryAccess())),"native Polymer roundtrip "+i);
                require(dispenser(stack.getItem()),"native dispenser factory registered "+i);ammo.add(stack.copyWithCount(1));
            }
            require(new ItemStack(LbcStand.ARROW).is(ItemTags.ARROWS),"existing native Stand Arrow can be selected as ammunition");
        }
        Set<UUID> ids(){var result=new HashSet<UUID>();for(var e:f.level.getAllEntities())result.add(e.getUUID());return result;}
        Arrow published(Set<UUID> before,ItemStack expected){var found=new ArrayList<Arrow>();for(var e:f.level.getAllEntities())if(!before.contains(e.getUUID())&&e instanceof Arrow a&&!a.isRemoved()){f.own(a);found.add(a);}require(found.size()==1,"exactly one native Arrow");var a=found.getFirst();require(a.getOwner()==f.owner&&ItemStack.matches(a.getPickupItemStackOrigin().copyWithCount(1),expected.copyWithCount(1)),"actual owner and all selected ammo properties retained");return a;}
        void bow(int i,InteractionHand hand){f.owner.stopUsingItem();f.owner.getInventory().clearContent();var weapon=new ItemStack(Items.BOW);var donor=ammo.get(i).copyWithCount(3);f.owner.setItemInHand(hand,weapon);f.owner.setItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND,donor);var before=ids();
            require(f.owner.gameMode.useItem(f.owner,f.level,weapon,hand).consumesAction()&&Items.BOW.releaseUsing(weapon,f.level,f.owner,71980),"native bow use/release "+i+" "+hand);f.owner.stopUsingItem();var shot=published(before,donor);require(donor.getCount()==2,"one selected arrow spent");require(shot.getWeaponItem()!=null&&shot.getWeaponItem().is(Items.BOW),"real weapon retained");f.discard(shot);}
        void startCross(){f.owner.stopUsingItem();f.owner.getInventory().clearContent();cross=new ItemStack(Items.CROSSBOW);selected=ammo.get(index).copyWithCount(3);f.owner.setItemInHand(InteractionHand.MAIN_HAND,cross);f.owner.setItemInHand(InteractionHand.OFF_HAND,selected);require(f.owner.gameMode.useItem(f.owner,f.level,cross,InteractionHand.MAIN_HAND).consumesAction()&&f.owner.isUsingItem(),"native crossbow charging starts");when=game.server().getTickCount();}
        boolean step(){f.client.acknowledge();f.audience.acknowledge();require(game.server().getTickCount()-began<600,"bounded native charge fixture");if(game.server().getTickCount()-when<30)return false;
            require(CrossbowItem.isCharged(cross)&&selected.getCount()==2,"actual elapsed native charge selects and consumes one typed arrow "+index);f.owner.stopUsingItem();var before=ids();require(f.owner.gameMode.useItem(f.owner,f.level,cross,InteractionHand.MAIN_HAND).consumesAction(),"actual charged native use fires");f.owner.stopUsingItem();var shot=published(before,ammo.get(index));require(!CrossbowItem.isCharged(cross),"native charged contents cleared after shot");f.discard(shot);
            if(++index<ammo.size()){startCross();return false;}payloads();return true;}
        Arrow arrow(int i,Vec3 at){var a=new Arrow(f.level,f.owner,ammo.get(i).copy(),new ItemStack(Items.BOW));a.setPos(at);a.setNoGravity(true);a.setDeltaMovement(Vec3.ZERO);require(f.level.addFreshEntity(a),"native payload carrier inserted");f.own(a);return a;}
        Cow cow(Vec3 at,float hp){var c=EntityTypes.COW.create(f.level,EntitySpawnReason.COMMAND);require(c!=null,"native cow");c.setPos(at);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);c.setHealth(hp);require(f.level.addFreshEntity(c),"resident native target");f.own(c);return c;}
        void strike(Arrow a,Cow c){a.setPos(c.position().add(0,.5,-2));a.setDeltaMovement(0,0,4);a.tick();}
        void payloads(){
            var strong=cow(high.add(0,50,0),1000);var arrow=arrow(0,strong.position().add(0,.5,-2));arrow.setCritArrow(true);strike(arrow,strong);require(!strong.isAlive(),"real native1e20 critical arrow is lethal rather than overflowing");f.discard(arrow);f.discard(strong);
            var weak=arrow(1,high.add(0,60,0));game.items().quiver().beforeArrow(weak);require(((ShulkerBowMixin.ArrowAccess)weak).skywars2$baseDamage()==1,"source weak base damage1");f.discard(weak);
            var poison=cow(high.add(0,70,0),1000);var poisoned=arrow(2,poison.position().add(0,.5,-2));strike(poisoned,poison);require(poison.hasEffect(MobEffects.POISON)&&poison.getEffect(MobEffects.POISON).isInfiniteDuration()&&poison.hasEffect(MobEffects.WITHER)&&!poison.hasEffect(MobEffects.RAID_OMEN),"native infinite poison/wither; source transport marker is consumed");require(game.items().acidCorruption().deathRemaining(poison)==450,"native3600/8 marker duration becomes source450-second delayed-death counter");game.items().acidCorruption().clear(poison);f.discard(poisoned);f.discard(poison);
            var para=cow(high.add(0,80,0),1000);var paralysis=arrow(6,para.position().add(0,.5,-2));strike(paralysis,para);require(para.isNoAi()&&para.isInvulnerable()&&para.isNoGravity()&&para.isSilent()&&para.getActiveEffects().isEmpty(),"source non-player stasis saved flags and effect clearing");f.discard(paralysis);f.discard(para);
            reactive();vacuum();electrical();carriers();directVacuum();nuclear();
        }
        void reactive(){var point=high.add(0,0,24);var target=cow(point.add(0,0,8),1000);var arrow=arrow(3,point);int before=game.items().quiver().children().size();game.items().quiver().beforeArrow(arrow);
            var children=game.items().quiver().children();require(children.size()==before+1&&children.getLast() instanceof PrimedTnt,"reactive source creates real native TNT");var tnt=(PrimedTnt)children.getLast();var expected=point.subtract(target.getEyePosition().subtract(point).normalize().scale(3));require(tnt.getFuse()==0&&tnt.getOwner()==f.owner&&tnt.getDeltaMovement().equals(Vec3.ZERO)&&tnt.position().distanceToSqr(expected)<1e-12,"source zero fuse, actual owner, exact back3 origin, no priming motion");f.discard(tnt);
            f.pause();try{int count=game.items().quiver().children().size();game.items().quiver().beforeArrow(arrow);require(game.items().quiver().children().size()==count,"TimeStop holds reactive emission");}finally{f.thaw();}
            f.field(true,point);try{game.items().quiver().beforeArrow(arrow);require(game.items().quiver().children().stream().noneMatch(e->e instanceof PrimedTnt&&!e.isRemoved()),"TECH prevents reactive explosion origin");}finally{f.removeFields();}f.discard(arrow);f.discard(target);
        }
        void vacuum(){var point=high.add(0,0,3);var mob=cow(point.add(4,0,0),1000);var far=cow(point.add(0,102,0),1000);var item=new ItemEntity(f.level,point.x+5,point.y,point.z,new ItemStack(Items.DIAMOND,7));require(f.level.addFreshEntity(item),"native loose item");f.own(item);var physical=arrow(1,point.add(6,0,0));var strict=new Marker(EntityTypes.MARKER,f.level);strict.setPos(point.add(7,0,0));strict.entityTags().add("strict_map_object");require(f.level.addFreshEntity(strict),"protected control");f.own(strict);
            var card=game.items().danmaku().card(f.owner,point.add(8,0,0),new Vec3(0,0,1),false);require(card!=null,"real virtual-owner visual");f.own(card.display);var player=f.other.position();var oldFar=far.position();var oldStrict=strict.position();var impact=arrow(5,point);game.items().quiver().ground(impact);
            require(impact.isRemoved()&&mob.position().equals(point)&&item.position().equals(point)&&physical.position().equals(point),"WILD pull moves allied/unowned native bodies, loose items and projectiles");require(f.other.position().equals(player)&&far.position().equals(oldFar)&&strict.position().equals(oldStrict),"players, source radius and service roles protected");require(card.display.position().equals(point)&&card.direction.equals(new Vec3(0,0,1)),"canonical virtual card and its visual move together without changing flight direction");game.items().quiver().ground(impact);
            for(var e:List.of(mob,far,item,physical,strict,card.display))f.discard(e);game.items().danmaku().stop();
        }
        void electrical(){var point=high.add(0,130,0);var first=cow(point.add(0,0,5),1000);var second=cow(point.add(5,0,8),1000);int before=traces.size();var a=arrow(7,point);game.items().quiver().ground(a);require(a.isRemoved()&&first.getHealth()==984&&second.getHealth()==984,"one16-damage native chain contact per hostile target");require(first.getLastDamageSource().getEntity()==f.owner&&traces.size()==before+6,"true shooter and three native ten-tick arcs per target");game.items().quiver().ground(a);require(traces.size()==before+6,"terminal cannot replay after carrier removal");traces.clear();f.discard(first);f.discard(second);}
        void carriers(){var point=high.add(0,170,0);var victim=cow(point.add(3,0,0),1000);var raw=arrow(5,point);var weapon=new ItemStack(Items.BOW);var data=new net.minecraft.nbt.CompoundTag();data.putInt("shulker_new",1);weapon.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));var family=game.items().shulkerBow().augment(f.owner,weapon,List.of(raw));require(family.size()==1&&family.getFirst() instanceof ShulkerBullet,"actual native shulker conversion");var bullet=(ShulkerBullet)family.getFirst();require(f.level.getEntity(bullet.getUUID())==bullet,"shared conversion publishes native shulker exactly once");f.own(bullet);var hit=new BlockHitResult(point,Direction.UP,BlockPos.containing(point),false);game.items().shulkerBow().blockHit(bullet,hit);require(victim.position().equals(point),"native shulker carrier retains IMBA ground payload");var s=((LbcShulkerBow.Carrier)bullet).skywars2$shulkerState();require(s.terminal,"native shulker terminal claim recorded");f.discard(bullet);f.discard(raw);f.discard(victim);
            var beamArrow=arrow(5,point);var payload=LbcBeamAmmo.capture(game,f.owner,new ItemStack(Items.BOW),List.of(beamArrow));require(payload!=null,"real accepted beam ammo");f.discard(beamArrow);var target=cow(point.add(4,0,0),1000);payload.blockContact(f.level,point,game.items().indirectKills().coldCause(f.owner,true));require(target.position().equals(point),"shared detached beam context retains IMBA wall payload");f.discard(target);
        }
        void directVacuum(){var point=high.add(0,220,0);var victim=cow(point,1000);var control=cow(point.add(4,0,0),1000);var a=arrow(5,point.add(0,.5,-2));strike(a,victim);require(control.position().equals(victim.position())&&!victim.hasEffect(MobEffects.RAID_OMEN),"actual native Evo hit delivers vacuum in the hit callback without a marker tick");f.discard(a);f.discard(victim);f.discard(control);}
        void nuclear(){require(game.items().nukeEvent().jobCount()==0,"exclusive nuclear control");var point=high.add(0,300,0);var a=arrow(4,point);nuclear=true;game.items().quiver().ground(a);int count=game.items().nukeEvent().jobCount();require(a.isRemoved()&&count>0,"real shared nuclear work is queued by the ground terminal");game.items().quiver().ground(a);require(game.items().nukeEvent().jobCount()==count,"removed nuclear carrier never detonates twice");game.items().nukeEvent().stop();var victim=cow(point,1000);var hit=arrow(4,point.add(0,.5,-2));strike(hit,victim);require(game.items().nukeEvent().jobCount()>0&&victim.getHealth()<990&&!victim.hasEffect(MobEffects.RAID_OMEN),"native nuclear hit publishes shared work and first-wave damage inside its actual contact");f.discard(hit);f.discard(victim);game.items().nukeEvent().stop();}
        public void close(){if(closed)return;closed=true;try{if(nuclear)game.items().nukeEvent().stop();traces.clear();game.items().danmaku().stop();game.items().legacyEffects().personalStasis().clear(f.owner);game.items().legacyEffects().personalStasis().clear(f.other);
                var fresh=new ArrayList<Entity>();for(var e:f.level.getAllEntities())fresh.add(e);for(var e:fresh)if(!initial.contains(e.getUUID())){if(e instanceof LivingEntity living)game.items().acidCorruption().clear(living);f.own(e);}
            }finally{try{f.close();}finally{worldRandom.restore(f.level.getRandom());}}}
    }
}
