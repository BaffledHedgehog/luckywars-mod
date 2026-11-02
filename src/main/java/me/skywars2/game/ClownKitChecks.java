package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Finite native input, status, terrain and scheduled-arrow observations for the original Clown kit. */
public final class ClownKitChecks {
    private ClownKitChecks(){}
    private static State running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Clown kit: "+why);}
    private static void text(Component c,String key,Integer color){require(c!=null&&c.getContents() instanceof TranslatableContents t&&t.getKey().equals(key)&&!c.getStyle().isItalic()&&Objects.equals(c.getStyle().getColor()==null?null:c.getStyle().getColor().getValue(),color),"exact source text "+key);}
    private static final class State implements AutoCloseable {
        final GameController game;final ServerPlayer p,beta;final RegenerationBeltChecks.Snapshot saved,betaSaved;
        final AllGrimoireChecks.Fixture f;final RegenerationBeltChecks.ScheduledClient client,audience;
        final List<Marker> fields=new ArrayList<>();final List<Entity> actors=new ArrayList<>();
        final Map<Item,Integer> stats=new IdentityHashMap<>();final Set<String> tags,betaTags;
        final net.minecraft.world.scores.PlayerTeam oldTeam,oldBetaTeam;net.minecraft.world.scores.PlayerTeam team;
        int stage,started;Projectile flight;boolean closed;
        State(GameController game){
            this.game=game;p=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(p!=null&&beta!=null,"two real participants");saved=new RegenerationBeltChecks.Snapshot(game,p);betaSaved=new RegenerationBeltChecks.Snapshot(game,beta);
            tags=new HashSet<>(p.entityTags());betaTags=new HashSet<>(beta.entityTags());oldTeam=p.getTeam();oldBetaTeam=beta.getTeam();
            f=new AllGrimoireChecks.Fixture(game);client=new RegenerationBeltChecks.ScheduledClient(game,p);audience=new RegenerationBeltChecks.ScheduledClient(game,beta);
            for(var item:List.of(LbcClownItems.MASK,LbcClownItems.SMOKE_BOMB,LbcClownItems.FALSE_GRAVE,Items.SNOWBALL))stats.put(item,p.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(item)));
        }
        ItemStack loot(String id){var out=game.lbc().rollTable(f.level,f.at,p,"lbc:ported/"+id);require(out.size()==1&&out.getFirst().getCount()==1,"canonical singleton "+id);return out.getFirst();}
        void position(ServerPlayer player,Vec3 at){player.teleportTo(f.level,at.x,at.y,at.z,Set.of(),0,0,true);player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);player.setKnownMovement(Vec3.ZERO);player.setOnGround(true);if(player==p)client.acknowledge();else audience.acknowledge();}
        List<net.minecraft.world.entity.projectile.arrow.Arrow> arrows(){return game.items().hedgehogSpikes().projectiles().stream().filter(a->!a.isRemoved()).toList();}
        void clean(){
            for(var m:List.copyOf(fields))remove(m);for(var a:List.copyOf(arrows()))a.discard();
            for(var player:List.of(p,beta)){player.removeAllEffects();player.getInventory().clearContent();player.getInventory().setSelectedSlot(0);player.setShiftKeyDown(false);player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                for(var t:List.of("time_stopped","spectator","strict_map_object","magic_academy","sin_greed_active","nomagic_active","rcdenyalways"))player.entityTags().remove(t);
                var profile=game.players().get(player.getUUID());profile.lbcSkill=0;profile.lbcChallenge=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);Arrays.fill(profile.levelups,0);
                game.items().magic().inventoryChanged(player);game.items().magic().flushCleansing();}
            position(p,f.at);position(beta,f.at.add(10,0,0));
        }
        void platform(){var center=BlockPos.containing(f.at);for(var pos:BlockPos.betweenClosed(center.offset(-16,-1,-16),center.offset(16,5,16)))f.level.setBlock(pos,pos.getY()==center.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2|16|32);}
        Marker field(String tag,Vec3 at){var m=new Marker(EntityTypes.MARKER,f.level);m.setPos(at);m.entityTags().add(tag);require(f.level.addFreshEntity(m),"real jammer marker");fields.add(m);game.items().magic().loaded(m);return m;}
        void remove(Marker m){game.items().magic().unloaded(m);m.discard();fields.remove(m);}
        boolean input(InteractionHand hand,boolean raw){client.acknowledge();return raw?net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker().interact(p,f.level,hand).consumesAction():p.gameMode.useItem(p,f.level,p.getItemInHand(hand),hand).consumesAction();}
        ItemStack held(String id,InteractionHand hand,boolean raw){p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);var s=loot(id);if(raw){var original=s;s=s.transmuteCopy(Items.SNOWBALL);
                s.set(DataComponents.CUSTOM_DATA,original.get(DataComponents.CUSTOM_DATA));
                s.set(DataComponents.ITEM_MODEL,original.get(DataComponents.ITEM_MODEL));
                s.set(DataComponents.CUSTOM_NAME,original.get(DataComponents.CUSTOM_NAME));s.set(DataComponents.LORE,original.get(DataComponents.LORE));}s.setCount(2);p.setItemInHand(hand,s);return s;}
        void presentation(){
            var kit=game.lbc().rollTable(f.level,f.at,p,"lbc:kit/clown");require(kit.size()==3,"source three kit pools");
            var items=List.of(LbcClownItems.MASK,LbcClownItems.SMOKE_BOMB,LbcClownItems.FALSE_GRAVE);String[] ids={"clown_mask","clown_smoke_bomb","false_grave"},models={"lbc:armors/clownmask","lbc:smoke_bomb","lbc:false_tumba"},names={"clown_mask","smoke_bomb","false_tumba"};int[] counts={1,4,1};
            for(int i=0;i<3;i++){var s=loot(ids[i]);require(s.is(items.get(i))&&kit.get(i).is(items.get(i))&&kit.get(i).getCount()==counts[i],"source kit native type/count "+ids[i]);
                for(var c:List.of(DataComponents.CUSTOM_NAME,DataComponents.LORE,DataComponents.CUSTOM_DATA,DataComponents.ITEM_MODEL))require(Objects.equals(s.get(c),items.get(i).getDefaultInstance().get(c)),"leaf and registered default exact source components "+ids[i]+" "+c);
                require(Identifier.parse(models[i]).equals(s.get(DataComponents.ITEM_MODEL)),"source model");text(s.get(DataComponents.CUSTOM_NAME),names[i],0xffffff);
                var lore=s.get(DataComponents.LORE).lines();require(lore.size()==(i==1?1:2),"source lore count");for(int n=0;n<lore.size();n++)text(lore.get(n),names[i]+".lore"+(i==1?"":"."+(n+1)),0xaaaaaa);
                require(LbcFartus.data(s).getIntOr(names[i],0)==1,"source trigger identity");
            }
            var mask=loot("clown_mask");require(mask.getMaxDamage()==25&&mask.getMaxStackSize()==1&&mask.get(DataComponents.EQUIPPABLE).slot()==EquipmentSlot.HEAD&&mask.get(DataComponents.EQUIPPABLE).assetId().orElseThrow().identifier().equals(Identifier.parse("lbc:clown_mask")),"native source armor/durability/equipment");
            var mods=mask.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers();require(mods.size()==1&&mods.getFirst().attribute().equals(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)&&mods.getFirst().modifier().amount()==3&&mods.getFirst().modifier().id().equals(Identifier.withDefaultNamespace("fisjgidsjg4givibhjeswh")),"exact source armor3 modifier");
            var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)f.base.recording;
            net.fabricmc.fabric.api.networking.v1.context.PacketContext.runWithContext(provider,()->{var proxy=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(mask,net.minecraft.world.item.TooltipFlag.NORMAL,provider.getPacketContext(),p.registryAccess());require(proxy.is(Items.LEATHER_HELMET)&&Objects.equals(mask.get(DataComponents.EQUIPPABLE),proxy.get(DataComponents.EQUIPPABLE))&&Objects.equals(mask.get(DataComponents.ITEM_MODEL),proxy.get(DataComponents.ITEM_MODEL)),"actual Polymer authored equipment projection");require(ItemStack.matches(mask,eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(proxy,provider.getPacketContext(),p.registryAccess())),"actual Polymer native round trip");});
        }
        void mask(){
            clean();for(var effect:List.of(MobEffects.BLINDNESS,MobEffects.DARKNESS)){p.addEffect(new MobEffectInstance(effect,400,0));p.addEffect(new MobEffectInstance(effect,50,2));require(((EffectLayers.Access)p.getEffect(effect)).skywars2$layerHidden()!=null,"real hidden native layer before mask");}
            p.setItemSlot(EquipmentSlot.HEAD,loot("clown_mask"));game.items().clownKit().effectsChanged(p);
            require(!p.hasEffect(MobEffects.BLINDNESS)&&!p.hasEffect(MobEffects.DARKNESS),"mask clears complete native active/hidden chains");
            var mag=field("stopper_magic",p.position());var tech=field("stopper",p.position());
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,100,0));p.addEffect(new MobEffectInstance(MobEffects.DARKNESS,100,0));
            require(!p.hasEffect(MobEffects.BLINDNESS)&&!p.hasEffect(MobEffects.DARKNESS),"actual native effect-change callback remains unjammed");remove(mag);remove(tech);
            p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);p.getInventory().setItem(9,loot("clown_mask"));p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,100,0));require(p.hasEffect(MobEffects.BLINDNESS),"inventory mask does not protect head");
        }
        long snowballs(){long n=0;for(var e:f.level.getAllEntities())if(!f.base.before.contains(e)&&!e.isRemoved()&&e instanceof Snowball)n++;return n;}
        void grave(){
            clean();platform();
            for(boolean raw:new boolean[]{false,true})for(var hand:InteractionHand.values()){
                position(p,f.at);var original=p.position();var block=BlockPos.containing(original);f.level.setBlock(block,Blocks.AIR.defaultBlockState(),2|16|32);f.level.setBlock(block.above(),Blocks.AIR.defaultBlockState(),2|16|32);
                var s=held("false_grave",hand,raw);var opposite=hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;p.setItemInHand(opposite,new ItemStack(Items.STICK,7));
                var momentum=new Vec3(.21,.17,-.13);p.setDeltaMovement(momentum);p.setYRot(33);p.setXRot(-17);long balls=snowballs();int arrows=arrows().size();
                require(input(hand,raw)&&s.getCount()==1&&p.getItemInHand(opposite).getCount()==7,"native/raw actual used-hand grave debit "+hand+" raw="+raw);
                require(snowballs()==balls&&arrows().size()==arrows,"immediate grave never creates throw transport");
                require(f.level.getBlockState(block).is(Blocks.CHISELED_BOOKSHELF)&&f.level.getBlockState(block.above()).is(Blocks.BARREL),"grave remains at old source command position");
                var dest=p.position();require(Math.abs(dest.x-original.x)<=15.01&&Math.abs(dest.z-original.z)<=15.01&&!BlockPos.containing(dest).equals(block)&&f.level.noCollision(p)&&f.level.getBlockState(p.blockPosition().below()).isSolid(),"real safe random destination within source15");
                require(p.getDeltaMovement().distanceToSqr(momentum)<1e-10&&p.getYRot()==33&&p.getXRot()==-17,"momentum and free camera retained");client.acknowledge();
            }
            position(p,f.at);var s=held("false_grave",InteractionHand.MAIN_HAND,false);var mag=field("stopper_magic",p.position());require(game.items().magic().suppressed(p)&&!input(InteractionHand.MAIN_HAND,false)&&s.getCount()==2,"MAG rejects grave without debit");remove(mag);
            p.getInventory().setItem(9,new ItemStack(LbcMagic.MYSTICAL_ARTIFACT));game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();require(game.items().magic().suppressed(p)&&!input(InteractionHand.MAIN_HAND,false)&&s.getCount()==2,"actual inventory artifact denies grave");p.getInventory().setItem(9,ItemStack.EMPTY);game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();
            var tech=field("stopper",p.position());require(game.items().magic().mechanismSuppressed(p)&&input(InteractionHand.MAIN_HAND,false)&&s.getCount()==1,"TECH permits immediate grave");remove(tech);client.acknowledge();
        }
        void effects(LivingEntity target,boolean expected,String why){for(var type:List.of(MobEffects.BLINDNESS,MobEffects.POISON)){var e=target.getEffect(type);require(expected?e!=null&&e.getDuration()==100&&e.getAmplifier()==0:e==null,why+" "+type);}}
        @SuppressWarnings("unchecked") List<net.minecraft.network.protocol.Packet<?>> packets(){try{Object connection=client.connection;var field=connection.getClass().getDeclaredField("packets");field.setAccessible(true);return (List<net.minecraft.network.protocol.Packet<?>>)field.get(connection);}catch(ReflectiveOperationException error){throw new IllegalStateException("native recording connection packets",error);}}
        void gas(){
            clean();platform();var board=game.server().getScoreboard();require(board.getPlayerTeam("clown_native_fixture")==null,"exclusive team fixture");team=board.addPlayerTeam("clown_native_fixture");team.setAllowFriendlyFire(false);board.addPlayerToTeam(p.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);
            position(beta,f.at.add(3.8,0,0));var cow=new net.minecraft.world.entity.animal.cow.Cow(EntityTypes.COW,f.level);cow.setPos(f.at.add(-3,0,0));cow.setNoAi(true);require(f.level.addFreshEntity(cow),"native gas mob");actors.add(cow);
            var masked=new net.minecraft.world.entity.animal.cow.Cow(EntityTypes.COW,f.level);masked.setPos(f.at.add(0,0,3));masked.setNoAi(true);masked.setItemSlot(EquipmentSlot.HEAD,loot("clown_mask"));require(f.level.addFreshEntity(masked),"native head-mask mob");actors.add(masked);
            var mag=field("stopper_magic",f.at);int packetStart=packets().size();game.items().clownKit().gas(p,f.at);
            var gasPackets=packets().subList(packetStart,packets().size()).stream().filter(net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.class::isInstance).map(net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.class::cast).filter(packet->packet.particle().getType()==net.minecraft.core.particles.ParticleTypes.CAMPFIRE_SIGNAL_SMOKE).toList();
            require(gasPackets.size()==1&&gasPackets.getFirst().count()==6000&&Math.abs(gasPackets.getFirst().xDist()-.1f)<1e-6&&Math.abs(gasPackets.getFirst().yDist()-.1f)<1e-6&&Math.abs(gasPackets.getFirst().zDist()-.1f)<1e-6,"authored private6000 smoke plume with narrow .1 spread");effects(p,true,"WILD caster under MAG");effects(beta,true,"WILD friendly-fire-disabled ally");effects(cow,true,"native mob gas");effects(masked,false,"head mask excludes whole gas");remove(mag);
            p.removeAllEffects();beta.removeAllEffects();cow.removeAllEffects();position(beta,f.at.add(4.2,0,0));game.items().clownKit().gas(p,f.at);effects(beta,false,"outside radius4");
            var old=p.getEffect(MobEffects.POISON);var tech=field("stopper",f.at);require(!game.items().clownKit().smokeAllowed(f.level,f.at),"TECH smoke location positive control");game.items().clownKit().gas(p,f.at);effects(beta,false,"TECH prevents new detonation");require(p.getEffect(MobEffects.POISON)==old&&old.getDuration()==100,"TECH retains already applied five-second effect");remove(tech);
            cow.discard();masked.discard();
        }
        void smokeHands(){
            clean();for(boolean raw:new boolean[]{false,true})for(var hand:InteractionHand.values()){
                var s=held("clown_smoke_bomb",hand,raw);var opposite=hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;p.setItemInHand(opposite,new ItemStack(Items.STICK,7));
                require(LbcHedgehogSpikes.legacyThrow(s)==9,"native/raw smoke uses variant9");int n=arrows().size();require(input(hand,raw)&&s.getCount()==1&&p.getItemInHand(opposite).getCount()==7,"native/raw used-hand smoke debit "+hand+" raw="+raw);
                var arrows=arrows();require(arrows.size()==n+1&&arrows.getLast().getOwner()==p,"same native arrow transport owner");arrows.getLast().discard();
            }
            var s=held("clown_smoke_bomb",InteractionHand.MAIN_HAND,false);var tech=field("stopper",p.position());require(game.items().magic().mechanismSuppressed(p)&&!input(InteractionHand.MAIN_HAND,false)&&s.getCount()==2,"TECH rejects launch without debit");remove(tech);
        }
        void beginFlight(boolean cancel){
            clean();platform();for(int x=-2;x<=2;x++)for(int y=0;y<=4;y++)f.level.setBlock(BlockPos.containing(f.at).offset(x,y,6),Blocks.STONE.defaultBlockState(),2|16|32);
            position(beta,f.at.add(0,0,5));var mag=field("stopper_magic",p.position());require(game.items().magic().suppressed(p)&&game.items().clownKit().smokeAllowed(f.level,p.position()),"MAG permits smoke positive control");
            var s=held("clown_smoke_bomb",InteractionHand.MAIN_HAND,false);require(input(InteractionHand.MAIN_HAND,false)&&s.getCount()==1,"actual MAG smoke launch");var arrows=arrows();require(arrows.size()==1,"one actual live transport");flight=arrows.getFirst();
            if(cancel)field("stopper",flight.position());started=game.server().getTickCount();stage=cancel?1:0;
        }
        void initialize(){clean();presentation();mask();grave();gas();smokeHands();beginFlight(false);}
        boolean tick(){client.acknowledge();audience.acknowledge();int elapsed=game.server().getTickCount()-started;require(elapsed<50,"bounded native flight stage="+stage+" at="+flight.position());
            if(!flight.isRemoved())return false;
            if(stage==0){require(elapsed>0&&beta.hasEffect(MobEffects.BLINDNESS)&&beta.hasEffect(MobEffects.POISON),"actual scheduled arrow contact detonates gas under MAG");var e=beta.getEffect(MobEffects.POISON);require(e.getDuration()>=97&&e.getDuration()<=100&&e.getAmplifier()==0,"native scheduled source100 effect boundary");beginFlight(true);return false;}
            require(!beta.hasEffect(MobEffects.BLINDNESS)&&!beta.hasEffect(MobEffects.POISON),"TECH entering live bomb cancels transport without gas");return true;
        }
        public void close(){if(closed)return;closed=true;Throwable failure=null;
            for(Runnable action:List.<Runnable>of(()->{for(var m:List.copyOf(fields))remove(m);for(var a:List.copyOf(arrows()))a.discard();actors.forEach(Entity::discard);},()->{if(team!=null){var b=game.server().getScoreboard();b.removePlayerFromTeam(p.getScoreboardName(),team);b.removePlayerFromTeam(beta.getScoreboardName(),team);b.removePlayerTeam(team);if(oldTeam!=null)b.addPlayerToTeam(p.getScoreboardName(),oldTeam);if(oldBetaTeam!=null)b.addPlayerToTeam(beta.getScoreboardName(),oldBetaTeam);}},client::close,audience::close,f::close,()->saved.restore(game),saved::restoreAttributes,saved::restoreUse,()->betaSaved.restore(game),betaSaved::restoreAttributes,betaSaved::restoreUse,()->{p.entityTags().clear();p.entityTags().addAll(tags);beta.entityTags().clear();beta.entityTags().addAll(betaTags);stats.forEach((item,n)->p.getStats().setValue(p,net.minecraft.stats.Stats.ITEM_USED.get(item),n));game.items().magic().inventoryChanged(p);game.items().magic().inventoryChanged(beta);game.players().save();}))try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("Clown scoped restore",failure);
        }
    }
    public static void start(GameController game){require(running==null,"exclusive finite trial");var state=new State(game);try{state.initialize();running=state;SkyWars2.LOGGER.info("[check] START clown-kit");}catch(Throwable error){try{state.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.tick())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS clown-kit (source native kit/leaves/Polymer, native/raw main+offhand debit, immediate old-spot safe grave/momentum/free camera, MAG/artifact/TECH, complete native hidden mask clearing, WILD radius4 all bodies/mask, actual scheduled smoke arrow impact and TECH cancellation)");}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL clown-kit",error);}}
}
