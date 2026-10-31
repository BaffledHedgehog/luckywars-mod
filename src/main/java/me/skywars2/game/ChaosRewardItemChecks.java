package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

/** Native raw hand admission, the existing queued factories and exact accepted melee identity. */
public final class ChaosRewardItemChecks {
    private ChaosRewardItemChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Chaos reward items: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final AllGrimoireChecks.Fixture base;final ServerPlayer p;
        final MatchRandomState random,playerRandom;
        final Map<AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();
        final Map<Item,Integer> stats=new IdentityHashMap<>();
        final int cost;boolean closed;
        Fixture(GameController game){
            this.game=game;base=new AllGrimoireChecks.Fixture(game);p=base.base.owner;
            random=MatchRandomState.capture(base.level.getRandom());playerRandom=MatchRandomState.capture(p.getRandom());
            cost=game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("unstable_spellbook_mana_use"),"lbc.math").get();
            require(game.items().sauvojen().count()==0&&game.items().danmaku().count()==0
                    &&game.items().danmaku().externalRemaining(p)==0,"exclusive source producers and owner external queue");
            for(var a:game.server().getAdvancements().getAllAdvancements()){
                var done=new HashSet<String>();p.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(done::add);advancements.put(a,done);
            }
            for(var item:List.of(Items.CARROT_ON_A_STICK,Items.DIAMOND_SWORD,Items.NETHERITE_SWORD))stats.put(item,p.getStats().getValue(Stats.ITEM_USED.get(item)));
            LbcChaosRewardItems.prepare(game.items().mana());
        }
        void ack(){
            p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            var packets=base.base.recording.packets.stream().filter(ClientboundPlayerPositionPacket.class::isInstance).map(ClientboundPlayerPositionPacket.class::cast).toList();
            if(!packets.isEmpty())p.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(packets.getLast().id(),p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot()));
        }
        void hand(ItemStack stack,InteractionHand hand){
            p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            p.setItemInHand(hand,stack);game.config().offhandWands=hand==InteractionHand.OFF_HAND;ack();
        }
        ItemStack book(){
            for(int n=0;n<1000;n++)for(var item:game.lbc().rollTable(base.level,p.position(),p,"lbc:transformables_staff"))if(LbcChaosRewardItems.book(item))return item;
            throw new IllegalStateException("Original raw random book leaf missing");
        }
        void choose(int bound,int selected){
            for(int n=0;n<10000;n++){var state=MatchRandomState.capture(p.getRandom());if(p.getRandom().nextInt(bound)==selected){state.restore(p.getRandom());return;}}
            throw new IllegalStateException("bounded native random selection");
        }
        void input(InteractionHand hand){ack();require(p.gameMode.useItem(p,base.level,p.getItemInHand(hand),hand).consumesAction(),"actual native hand route "+hand);}
        void books(){
            for(var h:InteractionHand.values()){
                var stack=book();hand(stack,h);var original=stack.copy();game.items().mana().set(p,10000);
                choose(55,54);input(h);
                require(game.items().sauvojen().count()==1&&game.items().mana().get(p)==7500,"source55 real retained Sauvojen producer and one2500 payment");
                require(ItemStack.matches(stack,original),"random ordinary raw stack retains all components/count");game.items().sauvojen().stop();
            }
            var stack=book();hand(stack,InteractionHand.MAIN_HAND);
            var data=LbcFartus.data(stack);data.putInt("upgradable",1);for(int n=1;n<=52;n++)data.putInt("have"+n,1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            game.items().mana().set(p,10000);choose(55,52);input(InteractionHand.MAIN_HAND);
            require(game.items().danmaku().externalRemaining(p)==20,"source53 enqueues existing independent20tick native owner");
            require(LbcFartus.data(stack).getIntOr("have53",0)==1&&LbcFartus.data(stack).getIntOr("upgrade_available",0)==1,"all53 stack-local trial branches use existing Academy ready transition");
            game.items().danmaku().forget(p);
            require(LbcChaosRewardItems.academy(game,p,InteractionHand.MAIN_HAND)&&LbcFartus.data(stack).getIntOr("upgraded",0)==1,"same raw book completes Academy upgrade");
            data=LbcFartus.data(stack);data.putInt("mode",54);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            game.items().mana().set(p,10000);choose(242,241);input(InteractionHand.MAIN_HAND);
            require(game.items().sauvojen().count()==1&&game.items().mana().get(p)==7500,"source54 random-all reaches242 through same Sauvojen owner");game.items().sauvojen().stop();
            p.setShiftKeyDown(true);p.setXRot(-30);int balance=game.items().mana().get(p);input(InteractionHand.MAIN_HAND);
            require(LbcFartus.data(stack).getIntOr("mode",0)==55&&game.items().mana().get(p)==balance,"upgraded sneak cycles actual stack without debit");
            p.setShiftKeyDown(false);game.items().mana().set(p,2499);
            require(!game.items().wands().use(p,InteractionHand.MAIN_HAND)&&game.items().mana().get(p)==2499&&game.items().sauvojen().count()==0,"insufficient balance leaves input and factory untouched");
        }
        Cow cow(Vec3 at,UUID uuid){
            var cow=new Cow(EntityTypes.COW,base.level);if(uuid!=null)cow.setUUID(uuid);cow.setPos(at);cow.setNoAi(true);cow.setNoGravity(true);
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);require(base.level.addFreshEntity(cow),"native cow insertion");return cow;
        }
        void whips(){
            var at=p.position();var victim=cow(at.add(0,0,3),null);var id=victim.getUUID();
            var collision=new UUID((id.getMostSignificantBits()&0xffffffff00000000L)|((id.getMostSignificantBits()+1)&0xffffffffL),id.getLeastSignificantBits()+1);
            var nearer=cow(at.add(0,0,1),collision);
            var stack=game.lbc().rollTable(base.level,p.position(),p,"lbc:anal_whip").getFirst();hand(stack,InteractionHand.MAIN_HAND);
            p.attack(victim);
            require(victim.getHealth()<100&&LbcFartus.data(stack).getStringOr("skywars2.whip_target","").equals(id.toString()),"accepted actual native melee binds actual victim despite nearer same-word UUID decoy");
            require(nearer.getHealth()==100,"nearer body receives no invented hit");
            for(var h:InteractionHand.values()){
                hand(stack,h);victim.setPos(at.add(4,0,4));var velocity=new Vec3(.31,.42,-.27);p.setDeltaMovement(velocity);
                int balance=game.items().mana().get(p);var before=stack.copy();input(h);
                require(p.position().distanceToSqr(victim.position())<1e-12&&p.getDeltaMovement().distanceToSqr(velocity)<1e-12,"exact same current UUID body and retained native momentum "+h);
                require(ItemStack.matches(stack,before)&&game.items().mana().get(p)==balance,"whip use has no debit/component rewrite "+h);
            }
            hand(stack,InteractionHand.MAIN_HAND);
            var field=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,base.level);field.setPos(p.position());field.entityTags().add("stopper_magic");
            require(base.level.addFreshEntity(field),"real caster MAG marker insertion");game.items().magic().loaded(field);
            var blockedAt=p.position();victim.setPos(blockedAt.add(3,0,3));
            try{require(!game.items().wands().use(p,InteractionHand.MAIN_HAND)&&p.position().equals(blockedAt),"user-qualified caster MAG rejects whip teleport");}
            finally{game.items().magic().unloaded(field);field.discard();game.items().magic().flushCleansing();}
            var artifactSlot=p.getInventory().getItem(8);
            p.getInventory().setItem(8,new ItemStack(LbcMagic.MYSTICAL_ARTIFACT));game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();
            try{for(var h:InteractionHand.values()){
                hand(stack,h);var position=p.position();var before=stack.copy();
                require(game.items().magic().suppressed(p)&&!game.items().wands().use(p,h)
                        &&p.position().equals(position)&&ItemStack.matches(stack,before),"actual stronger artifact blocks whip in either hand without debit or teleport "+h);
            }}finally{p.getInventory().setItem(8,artifactSlot);game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();}
            hand(stack,InteractionHand.MAIN_HAND);
            victim.setPos(blockedAt.add(40,0,0));
            var destinationField=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,base.level);destinationField.setPos(victim.position());destinationField.entityTags().add("stopper_magic");
            require(base.level.addFreshEntity(destinationField),"real destination MAG marker insertion");game.items().magic().loaded(destinationField);
            try{input(InteractionHand.MAIN_HAND);require(p.position().distanceToSqr(victim.position())<1e-12,"unsuppressed caster may move himself into target MAG without moving/damaging target");}
            finally{game.items().magic().unloaded(destinationField);destinationField.discard();game.items().magic().flushCleansing();}
            victim.discard();var origin=p.position();hand(stack,InteractionHand.MAIN_HAND);input(InteractionHand.MAIN_HAND);
            require(p.position().equals(origin),"absent full UUID never falls back to same-word decoy");
            game.config().offhandWands=false;hand(stack,InteractionHand.OFF_HAND);game.config().offhandWands=false;
            require(!game.items().wands().use(p,InteractionHand.OFF_HAND),"disabled offhand cannot teleport");
        }
        @Override public void close(){if(closed)return;closed=true;
            try{game.items().sauvojen().stop();game.items().danmaku().forget(p);
                for(var e:advancements.entrySet())for(var criterion:e.getKey().value().criteria().keySet())if(!e.getValue().contains(criterion))p.getAdvancements().revoke(e.getKey(),criterion);
                stats.forEach((item,value)->p.getStats().setValue(p,Stats.ITEM_USED.get(item),value));
                game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("unstable_spellbook_mana_use"),"lbc.math").set(cost);
            }finally{try{base.close();}finally{random.restore(base.level.getRandom());playerRandom.restore(p.getRandom());}}
        }
    }
    public static void run(GameController game){try(var f=new Fixture(game)){f.books();f.whips();}
        SkyWars2.LOGGER.info("[check] PASS chaos-reward-items (actual raw two-hand book55/54, shared53queue/trial/Academy/mode/balance; native accepted whip victim/full UUID collision/retained momentum/absent target/offhand; scoped restore)");}
}
