package me.skywars2.game;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import me.skywars2.SkyWars2;
import me.skywars2.game.items.gadgets.LbcBadAppleItem;
import me.skywars2.game.items.gadgets.LbcBridgeBuilderItem;
import me.skywars2.game.items.gadgets.LbcImbaPlaceableItems;
import me.skywars2.game.items.tools.LbcRewardToolItems;
import me.skywars2.game.items.wands.LbcWands;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.*;
import me.skywars2.game.match.events.LbcNukeEvent;

/** Independent canonical/vanilla oracle, actual native marker-egg placement and shared owner checks. */
public final class NativeUtilityInputChecks {
    private record Source(String id,String resource,Item item,Item carrier){}
    private static List<Source> sources(){var out=new ArrayList<Source>();
        for(var k:LbcRewardToolItems.Kind.values())out.add(new Source(k.id,k.definition,k.item,Items.CARROT_ON_A_STICK));
        out.add(new Source("imba_nuclear_bomb","data/lbc/loot_table/item_defs/lbc/nuke__c507f2f53fda.json",LbcImbaPlaceableItems.NUCLEAR_BOMB,Items.PANDA_SPAWN_EGG));
        out.add(new Source("bridge_builder_imba","data/lbc/loot_table/item_defs/lbc/bridgebuilder__d20c5b8711ad.json",LbcBridgeBuilderItem.TOKEN,Items.SNOWBALL));
        out.add(new Source("bad_apple_imba","data/lbc/loot_table/item_defs/lbc/badapple1__53d62d9b2e05.json",LbcBadAppleItem.TOKEN,Items.REPEATING_COMMAND_BLOCK));return out;}
    private NativeUtilityInputChecks(){}
    private static void require(boolean v,String why){if(!v)throw new IllegalStateException("Native utility input: "+why);}
    public static void run(GameController game){try(var s=new Scene(game)){s.begin();}}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final Set<UUID> before=new HashSet<>();final int used;boolean touchedNuke;
        final Map<BlockPos,net.minecraft.world.level.block.state.BlockState> nukeFrame=new LinkedHashMap<>();
        void rememberNuke(Vec3 point){
            var origin=BlockPos.containing(point);nukeFrame.clear();
            for(var cell:BlockPos.betweenClosed(origin.offset(-1,0,-1),origin.offset(1,2,1))){
                var state=f.level.getBlockState(cell);nukeFrame.put(cell.immutable(),state);f.set(cell.immutable(),state);
            }
        }
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);used=f.owner.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(LbcImbaPlaceableItems.NUCLEAR_BOMB));for(var e:f.level.getAllEntities())before.add(e.getUUID());require(game.items().nukeEvent().markerCount()==0&&game.items().nukeEvent().jobCount()==0,"exclusive nuclear fixture");}
        void begin(){f.prepareParticipants();presentation();nuclear();ordinaryNuclear();groundCrossbows();swordWear();}
        void groundCrossbows(){
            var at=f.owner.blockPosition().offset(3,0,0);
            var minigun=me.skywars2.game.items.weapons.ranged.LbcProjectileWeaponItems.Kind.MINIGUN_CROSSBOW.item;
            var blaze=me.skywars2.game.items.weapons.ranged.LbcProjectileWeaponItems.Kind.BLAZE_CROSSBOW.item;
            var ghast=me.skywars2.game.items.weapons.ranged.LbcProjectileWeaponItems.Kind.GHAST_CROSSBOW.item;
            f.set(at.below(),Blocks.NETHERRACK.defaultBlockState());f.set(at,Blocks.FIRE.defaultBlockState());
            require(f.level.getBlockState(at).is(Blocks.FIRE),"supported native fire fixture");
            var fire=new net.minecraft.world.entity.item.ItemEntity(f.level,at.getX()+.5,at.getY(),at.getZ()+.5,minigun.getDefaultInstance());
            require(f.level.addFreshEntity(fire),"native minigun ground fire actor");f.own(fire);f.client().packets().clear();fire.tick();
            require(fire.getItem().is(blaze),"native item tick crafts Blaze before typed fire immunity");
            craftParticles(net.minecraft.core.particles.ParticleTypes.FLAME,1,at);
            var bolts=new ArrayList<net.minecraft.world.entity.LightningBolt>();
            for(var entity:f.level.getAllEntities())if(!before.contains(entity.getUUID())&&entity instanceof net.minecraft.world.entity.LightningBolt bolt){bolts.add(bolt);f.own(bolt);}
            require(bolts.size()==1&&bolts.getFirst().position().equals(new Vec3(at.getX()+.5,at.getY()-5,at.getZ()+.5)),"one legacy lightning bolt five blocks below fire craft");
            var crafted=fire.getItem().copy();var bolt=bolts.getFirst();
            // Run both real lifecycles: the crafted item rises and lands while the bolt flashes.
            // Checking only its spawn position would miss a later natural-lightning reforge.
            for(var cell:BlockPos.betweenClosed(at.offset(-1,-6,-1),at.offset(1,-4,1)))f.set(cell.immutable(),f.level.getBlockState(cell));
            int lifetime=0;
            while(!bolt.isRemoved()&&lifetime++<120){
                f.level.tickNonPassenger(bolt);f.level.tickNonPassenger(fire);
                game.items().lightningAcquisition().tick();
                require(!fire.isRemoved()&&ItemStack.matches(crafted,fire.getItem())
                        &&game.items().lightningAcquisition().pendingCount()==0,"craft lightning never reforges or destroys the finished Blaze crossbow");
            }
            require(bolt.isRemoved()&&lifetime>=3,"complete native lightning lifetime, not a discarded spawn probe");
            fire.discard();
            var blast=new net.minecraft.world.entity.item.ItemEntity(f.level,at.getX()+.5,at.getY(),at.getZ()+.5,minigun.getDefaultInstance());
            require(f.level.addFreshEntity(blast),"native minigun explosion actor");f.own(blast);
            f.client().packets().clear();blast.hurtServer(f.level,f.level.damageSources().explosion(null,f.owner),4);
            require(!blast.isRemoved()&&blast.getItem().is(ghast),"actual native explosion crafts Ghast before item protection");blast.discard();
            craftParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,16,at);
            var nearby=new net.minecraft.world.entity.item.ItemEntity(f.level,at.getX()+.5,at.getY(),at.getZ()+.5,minigun.getDefaultInstance());
            require(f.level.addFreshEntity(nearby),"native minigun fuse actor");f.own(nearby);
            var tnt=new net.minecraft.world.entity.item.PrimedTnt(f.level,at.getX()+3,at.getY(),at.getZ(),f.owner);tnt.setFuse(2);
            require(f.level.addFreshEntity(tnt),"native TNT fuse2 witness");f.own(tnt);f.client().packets().clear();nearby.tick();
            require(nearby.getItem().is(ghast),"legacy seven-block fuse2 ground craft");nearby.discard();tnt.discard();
            craftParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,16,at);
        }
        void craftParticles(net.minecraft.core.particles.ParticleOptions type,int spread,BlockPos at){
            var particles=SmokeVolumeChecks.flatten(f.client().packets()).stream()
                    .filter(net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.class::isInstance)
                    .map(net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.class::cast).filter(packet->packet.particle()==type).toList();
            require(particles.size()==1,"whole legacy craft effect uses one native particle packet");var effect=particles.getFirst();
            require(effect.count()==200&&effect.overrideLimiter()&&effect.xDist()==spread&&effect.yDist()==spread&&effect.zDist()==spread
                    &&effect.xMaxSpeed()==1&&effect.yMaxSpeed()==1&&effect.zMaxSpeed()==1,"exact legacy force/count/spread/speed");
            require(effect.x()==at.getX()+.5&&effect.y()==at.getY()+1&&effect.z()==at.getZ()+.5,"effect origin stays one block above item");
        }
        void swordWear(){
            var models=java.util.Set.of("anal_whip","banhammer","blazefire_sword","cyberkatana","death_skythe","fartus","lightsaber_empty","piston_sword","shit_katana","techrunner_katana","terrasword","vex_sword");
            int seen=0;
            for(var item:net.minecraft.core.registries.BuiltInRegistries.ITEM){
                var stack=item.getDefaultInstance();var model=stack.get(net.minecraft.core.component.DataComponents.ITEM_MODEL);
                if(model!=null&&model.getNamespace().equals("lbc")&&models.contains(model.getPath())){
                    seen++;require(stack.getMaxDamage()!=25,"no leaked carrot durability on "+model);
                }
            }
            require(seen>=8,"audit registered legacy sword carriers");
            require(f.leaf("terra_sword").getMaxDamage()==0&&f.leaf("true_terra_sword").getMaxDamage()==9000,"canonical Terra swords preserve their distinct wear policies");
        }
        void ordinaryNuclear(){
            f.owner.setGameMode(GameType.SURVIVAL);
            var hit=pad();var expected=hit.getLocation().relative(hit.getDirection(),.01);
            rememberNuke(expected);touchedNuke=true;
            var stack=f.loot("lbc:nuke").copyWithCount(2);f.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
            require(stack.is(me.skywars2.game.entities.projectiles.LbcCrocodildoItems.NUKE),"public ordinary nuclear loot identity");
            require(f.owner.gameMode.useItemOn(f.owner,f.level,stack,InteractionHand.MAIN_HAND,hit).consumesAction()&&stack.getCount()==1,"public ordinary nuke native placement/debit");
            var origin=BlockPos.containing(expected);
            require(f.level.getBlockState(origin.above()).is(Blocks.BEACON)&&f.level.getBlockState(origin).is(Blocks.NETHERITE_BLOCK),"ordinary public nuke creates immediate visible netherite beacon");
            game.items().nukeEvent().stop();nukeFrame.forEach(f::set);nukeFrame.clear();
        }
        void presentation(){var params=new LootParams.Builder(f.level).withParameter(LootContextParams.ORIGIN,f.home).withOptionalParameter(LootContextParams.THIS_ENTITY,f.owner).create(LootContextParamSets.CHEST);var ops=game.server().reloadableRegistries().lookup().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
            var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)(Object)f.owner.connection;
            for(var source:sources()){var stack=f.leaf(source.id);require(stack.is(source.item)&&stack.getCount()==1,"canonical identity/quantity "+source.id);var defaults=new ItemStack(source.item);
                for(var c:stack.getComponents())require(Objects.equals(c.value(),defaults.get(c.type())),"canonical/default component "+source.id+" "+c.type());
                try(var stream=NativeUtilityInputChecks.class.getResourceAsStream("/"+source.resource)){
                    var raw=JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(stream),StandardCharsets.UTF_8)).getAsJsonObject();raw.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().addProperty("name",BuiltInRegistries.ITEM.getKey(source.carrier).toString());
                    var result=LootTable.DIRECT_CODEC.parse(ops,raw).getOrThrow().getRandomItems(params,17);require(result.size()==1,"one independent source output");var expected=result.getFirst();var actual=new ItemStack(source.carrier);actual.applyComponents(stack.getComponents());
                    // Native item's intrinsic fallback title is stable; the authored visible custom name is compared normally.
                    actual.set(DataComponents.ITEM_NAME, expected.get(DataComponents.ITEM_NAME));
                    // The server's enchantable-wand rule intentionally extends the source carrot carrier.
                    if (source.item instanceof LbcWands.NativeTool) {
                        expected.set(DataComponents.ENCHANTABLE, new net.minecraft.world.item.enchantment.Enchantable(1));
                    }
                    if (!ItemStack.matches(actual.copyWithCount(1), expected.copyWithCount(1))) {
                        var mismatches = new ArrayList<String>();
                        var keys = new LinkedHashSet<>(expected.getComponents().keySet());
                        keys.addAll(actual.getComponents().keySet());
                        for (var key : keys) {
                            Object want = expected.get(key), got = actual.get(key);
                            if (!Objects.equals(want, got)) mismatches.add(key + " expected=" + want + " actual=" + got);
                        }
                        require(false, "source component mismatch " + source.id + ": " + mismatches);
                    }
                }catch(java.io.IOException error){throw new IllegalStateException(error);}
                var proxy=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(stack,TooltipFlag.NORMAL,provider.getPacketContext(),f.owner.registryAccess());require(ItemStack.matches(stack,eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(proxy,provider.getPacketContext(),f.owner.registryAccess())),"recipient Polymer all-component roundtrip "+source.id);
            }
            require(LbcWands.isBoltMaterial(new ItemStack(LbcRewardToolItems.Kind.LIGHTNING.item)),"native lightning remains a craft material");
        }
        ItemStack held(InteractionHand hand){var stack=f.leaf("imba_nuclear_bomb").copyWithCount(3);f.owner.setItemInHand(hand,stack);f.owner.setItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND,new ItemStack(Items.STICK,7));return stack;}
        BlockHitResult pad(){var p=BlockPos.containing(f.home.add(0,0,2)).below();f.set(p,Blocks.STONE.defaultBlockState());f.set(p.above(),Blocks.AIR.defaultBlockState());f.set(p.above(2),Blocks.AIR.defaultBlockState());return new BlockHitResult(new Vec3(p.getX()+.5,p.getY()+1,p.getZ()+.5),Direction.UP,p,false);}
        void placed(Vec3 point){var service=game.items().nukeEvent();require(service.markerCount()==1&&service.jobCount()==0,"one existing nuclear warning actor, no premature detonation");var marker=service.markers().getFirst();f.own(marker);require(service.owner(marker)==f.owner&&marker.position().equals(point)&&marker.entityTags().containsAll(List.of("nuke_bruh","rc_1sec","marker_2t","mecha")),"actual owner, source placement and full nuke-bruh root");
            var origin=BlockPos.containing(point);
            for(var pos:BlockPos.betweenClosed(origin.offset(-1,0,-1),origin.offset(1,0,1)))require(f.level.getBlockState(pos).is(Blocks.NETHERITE_BLOCK),"placed nuclear bomb immediately publishes its netherite base");
            require(f.level.getBlockState(origin.above()).is(Blocks.BEACON),"placed nuclear bomb immediately publishes its real beacon");
            service.stop();require(marker.isRemoved()&&service.markerCount()==0,"shared owner retires source actor");
            nukeFrame.forEach(f::set);nukeFrame.clear();}
        void nuclear(){touchedNuke=true;var hit=pad();var expected=hit.getLocation();int count=used;var frame=BlockPos.containing(expected);for(var cell:BlockPos.betweenClosed(frame.offset(-1,0,-1),frame.offset(1,2,1)))f.set(cell.immutable(),f.level.getBlockState(cell));
            for(var hand:InteractionHand.values()){rememberNuke(expected);var stack=held(hand);require(f.owner.gameMode.useItemOn(f.owner,f.level,stack,hand,hit).consumesAction()&&stack.getCount()==2,"real native used-hand nuke debit "+hand);require(f.owner.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND).getCount()==7,"other hand is preserved");require(f.owner.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(LbcImbaPlaceableItems.NUCLEAR_BOMB))==++count,"one native use statistic");placed(expected);}
            f.pause();try{for(var hand:InteractionHand.values()){var stack=held(hand);f.owner.gameMode.useItemOn(f.owner,f.level,stack,hand,hit);require(stack.getCount()==3&&game.items().nukeEvent().markerCount()==0,"TimeStop rejects without debit");}}finally{f.thaw();}
            f.field(false,expected);try{rememberNuke(expected);var stack=held(InteractionHand.OFF_HAND);require(f.owner.gameMode.useItemOn(f.owner,f.level,stack,InteractionHand.OFF_HAND,hit).consumesAction()&&stack.getCount()==2,"MAG does not prevent physical nuclear input");count++;placed(expected);}finally{f.removeFields();}
            var water=BlockPos.containing(f.owner.getEyePosition().add(0,0,3));f.set(water,Blocks.WATER.defaultBlockState());rememberNuke(new Vec3(water.getX()+.5,water.getY(),water.getZ()+.5));var stack=held(InteractionHand.MAIN_HAND);require(f.owner.gameMode.useItem(f.owner,f.level,stack,InteractionHand.MAIN_HAND).consumesAction()&&stack.getCount()==2,"source-fluid native use");count++;placed(new Vec3(water.getX()+.5,water.getY(),water.getZ()+.5));f.set(water,Blocks.AIR.defaultBlockState());
            f.owner.setGameMode(GameType.CREATIVE);rememberNuke(expected);stack=held(InteractionHand.MAIN_HAND);require(f.owner.gameMode.useItemOn(f.owner,f.level,stack,InteractionHand.MAIN_HAND,hit).consumesAction()&&stack.getCount()==3,"creative native placement is free");count++;placed(expected);
            require(f.owner.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(LbcImbaPlaceableItems.NUCLEAR_BOMB))==count,"successful input statistics only");
        }
        public void close(){try{if(touchedNuke)game.items().nukeEvent().stop();for(var e:f.level.getAllEntities())if(!before.contains(e.getUUID()))f.own(e);f.owner.getStats().setValue(f.owner,net.minecraft.stats.Stats.ITEM_USED.get(LbcImbaPlaceableItems.NUCLEAR_BOMB),used);}finally{f.close();}}
    }
}
