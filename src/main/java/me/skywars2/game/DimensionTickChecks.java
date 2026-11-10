package me.skywars2.game;

import java.util.Set;
import me.skywars2.SkyWars2;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Actual ordinary-dimension player ticks, native fire immunity, End escalation and emitted HUD packets. */
public final class DimensionTickChecks {
    private DimensionTickChecks(){}
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Dimension ticks: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final int oldEnd,oldVoid,oldTotems;
        int phase,when,began,endCount;float hp;boolean closed;
        Fixture(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);
            oldTotems=f.owner.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
            oldEnd=game.items().mana().score(f.owner,"end_damage").get();oldVoid=game.items().mana().score(f.owner,"void_resistance").get();}
        void move(ServerLevel level){
            // Explicit fixture bootstrap, not a gameplay synchronous chunk-loading path.
            var packets=(java.util.List<?>)WoolBucketChecks.read(f.client.connection,"packets");int before=packets.size();
            var at=new Vec3(f.home.x,200,f.home.z);level.getChunkAt(net.minecraft.core.BlockPos.containing(at));
            require(f.owner.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true),"actual native dimension transfer");
            f.owner.setNoGravity(true);f.owner.setDeltaMovement(Vec3.ZERO);f.client.acknowledge();
            game.items().invalidateHud(f.owner);game.items().refreshHud(f.owner);
            require(game.items().mana().visible(f.owner)&&packets.subList(before,packets.size()).stream().anyMatch(ClientboundSetActionBarTextPacket.class::isInstance),"native actionbar remains admitted and emits a packet in "+level.dimension());
        }
        void setup(){
            f.prepareParticipants();f.owner.removeAllEffects();f.owner.getInventory().clearContent();
            game.items().mana().score(f.owner,"void_resistance").set(0);game.items().mana().score(f.owner,"end_damage").set(3);
            f.owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);f.owner.setHealth(f.owner.getMaxHealth());
            f.owner.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
            require(f.owner.isAlive()&&f.owner.getHealth()==f.owner.getMaxHealth()&&f.owner.getHealth()>10,"native totem-protected full-health fire witness: health="+f.owner.getHealth()+", maximum="+f.owner.getMaxHealth());
            f.owner.setInvulnerableTime(0);f.owner.damageCooldownTime=0;
            f.owner.getFoodData().setFoodLevel(10);began=when=game.server().getTickCount();hp=f.owner.getHealth();
            move(game.server().getLevel(Level.NETHER));
        }
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();int now=game.server().getTickCount();
            require(now-began<440,"bounded actual fire/End tick scene");
            if(phase==0){
                if(now==when||now%10!=0)return false;
                require(f.owner.isAlive()&&f.owner.getOffhandItem().isEmpty()
                        &&((Number)WoolBucketChecks.read(f.owner,"lastHurt")).floatValue()==1_000_000
                        &&f.owner.getLastDamageSource()!=null&&f.owner.getLastDamageSource().is(net.minecraft.world.damagesource.DamageTypes.IN_FIRE),"actual Nether ten-tick native million in_fire consumes exactly the real totem");
                f.owner.removeAllEffects();f.owner.setAbsorptionAmount(0);f.owner.setHealth(f.owner.getMaxHealth());f.owner.setInvulnerableTime(0);f.owner.damageCooldownTime=0;
                f.owner.addEffect(ItemEffectLevels.create(MobEffects.FIRE_RESISTANCE,100,0));hp=f.owner.getHealth();when=now;phase=1;return false;
            }
            if(phase==1){
                if(now==when||now%10!=0)return false;
                require(f.owner.getHealth()==hp,"native Fire Resistance protects from the same million fire damage");
                move(game.server().getLevel(Level.END));when=now;phase=2;return false;
            }
            if(phase==2){
                if(now==when||now%200!=0)return false;
                endCount=game.items().mana().score(f.owner,"end_damage").get();
                require(endCount==4&&f.owner.getHealth()==hp-4,"actual End ten-second native outside_border and source (3+1)*6/5 escalation");
                game.items().mana().score(f.owner,"void_resistance").set(30);hp=f.owner.getHealth();when=now;phase=3;return false;
            }
            if(now==when||now%200!=0)return false;
            require(game.items().mana().score(f.owner,"end_damage").get()==6&&f.owner.getHealth()==hp,"End counter continues growing while positive void resistance blocks its native damage");
            var panel=game.attributeHud();panel.clear(f.owner);for(int i=0;i<25;i++)panel.tick();
            require(panel.shown(f.owner)&&panel.displayedLines(f.owner).stream().anyMatch(c->c.getString().contains("\uE171")),"ordinary End dimension retains sidebar and actual life glyph");
            for(var key:java.util.List.of(LbcHedgehogDimensions.NEXUS,LbcHedgehogDimensions.IMPRINTED)){
                var level=game.server().getLevel(key);var at=new Vec3(f.home.x,200,f.home.z);level.getChunkAt(net.minecraft.core.BlockPos.containing(at));
                var packets=(java.util.List<?>)WoolBucketChecks.read(f.client.connection,"packets");int before=packets.size();
                require(f.owner.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true),"actual special-dimension transfer");f.client.acknowledge();
                game.items().refreshHud(f.owner);panel.tick();
                require(!panel.shown(f.owner)&&!game.items().mana().visible(f.owner)
                        &&packets.subList(before,packets.size()).stream().filter(ClientboundSetActionBarTextPacket.class::isInstance)
                            .map(ClientboundSetActionBarTextPacket.class::cast).allMatch(p->p.text().getString().isEmpty()),"both GUI owners stay hidden in "+key);
            }
            return true;
        }
        public void close(){if(closed)return;closed=true;
            try{if(f.owner.level()!=f.level){f.owner.teleportTo(f.level,f.home.x,f.home.y,f.home.z,Set.of(),0,0,true);f.client.acknowledge();}}
            finally{try{f.close();}finally{game.items().mana().score(f.owner,"end_damage").set(oldEnd);game.items().mana().score(f.owner,"void_resistance").set(oldVoid);f.owner.getStats().setValue(f.owner,net.minecraft.stats.Stats.ITEM_USED.get(net.minecraft.world.item.Items.TOTEM_OF_UNDYING),oldTotems);}}
        }
    }
    public static void run(GameController game){require(running==null,"exclusive dimension fixture");running=new Fixture(game);
        try{running.setup();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;
        SkyWars2.LOGGER.info("[check] PASS dimension-ticks (actual Nether ten-tick million in_fire; native Fire Resistance; actual End200tick escalation/outside_border/void-resistance; emitted actionbar in both dimensions)");
    }catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL dimension-ticks",error);}}
}
