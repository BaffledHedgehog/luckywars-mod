package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;

/** Real incoming criteria plus shared terrain/native falling-sand scheduler; no synthetic player ticks. */
public final class GaaraPotChecks {
    private GaaraPotChecks(){}
    private static State running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Gaara pot: "+why);}
    private static final class State implements AutoCloseable {
        final GameController game;final AllGrimoireChecks.Fixture base;final LbcGaaraPot service;
        final net.minecraft.server.level.ServerPlayer owner;
        final Map<BlockPos,MapManager.BlockSnapshot> extra=new LinkedHashMap<>();
        ItemStack source;BlockPos stone,web,water,edge,outside,air;int started,stage;boolean closed;
        State(GameController game){this.game=game;base=new AllGrimoireChecks.Fixture(game);base.reset();owner=base.base.owner;service=game.items().gaaraPot();
            require(service.count()==0,"exclusive pending queue");
            for(var pos:BlockPos.betweenClosed(owner.blockPosition().offset(-11,-11,-11),owner.blockPosition().offset(11,11,11)))
                if(!base.cells.containsKey(pos))extra.put(pos.immutable(),game.maps().snapshot(base.level,pos));
        }
        ItemStack pot(int slot,int amount){var stack=source.copyWithCount(amount);owner.getInventory().setItem(slot,stack);return stack;}
        void clean(){game.items().stonekivi().stop();owner.getInventory().clearContent();owner.removeAllEffects();owner.setInvulnerableTime(0);owner.damageCooldownTime=0;owner.setAbsorptionAmount(0);game.items().magic().inventoryChanged(owner);}
        void hit(){owner.setInvulnerableTime(0);owner.damageCooldownTime=0;long before=game.items().incomingArtifacts().criterionCount();
            require(owner.hurtServer(base.level,owner.damageSources().cactus(),1),"actual native environmental hurt accepted");
            require(game.items().incomingArtifacts().criterionCount()==before+1,"actual incoming criterion dispatched once");}
        void blocked(String why){var stack=pot(14,2);hit();require(stack.getCount()==2&&service.count()==0&&!owner.hasEffect(MobEffects.LEVITATION),why);owner.getInventory().setItem(14,ItemStack.EMPTY);}
        Marker marker(String tag){var marker=new Marker(EntityTypes.MARKER,base.level);marker.setPos(owner.position());marker.entityTags().add(tag);require(base.level.addFreshEntity(marker),"native "+tag+" marker inserted");game.items().magic().loaded(marker);return marker;}
        void remove(Marker marker){game.items().magic().unloaded(marker);marker.discard();}
        void policies(){
            var mag=marker("stopper_magic");try{require(game.items().magic().suppressed(owner),"native MAG beacon active");blocked("MAG beacon preserves pot");}finally{remove(mag);}
            var artifact=new ItemStack(Items.REPEATING_COMMAND_BLOCK);var data=new net.minecraft.nbt.CompoundTag();data.putInt("mystical",1);data.putInt("type",1);artifact.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            owner.getInventory().setItem(0,artifact);game.items().magic().inventoryChanged(owner);game.items().magic().flushCleansing();
            try{require(game.items().magic().artifactActive(owner),"real inventory antimagic artifact active");blocked("MAG artifact preserves pot");}finally{owner.getInventory().setItem(0,ItemStack.EMPTY);game.items().magic().inventoryChanged(owner);game.items().magic().flushCleansing();}
            owner.entityTags().add("time_stopped");try{var stack=pot(14,2);service.incoming(owner);require(stack.getCount()==2&&service.count()==0,"input TimeStop preserves pot at admitted boundary");owner.getInventory().setItem(14,ItemStack.EMPTY);}finally{owner.entityTags().remove("time_stopped");}
            game.items().timeStop().begin(base.level,40);try{var stack=pot(14,2);service.incoming(owner);require(stack.getCount()==2&&service.count()==0,"world TimeStop blocks direct admitted boundary");owner.getInventory().setItem(14,ItemStack.EMPTY);}finally{game.items().timeStop().end(base.level);}
            var tech=marker("stopper");try{
                require(game.items().magic().mechanismSuppressedAt(base.level,owner.position()),"native TECH zone active");
                var stack=pot(40,2);hit();require(stack.getCount()==1&&service.count()==1,"TECH does not suppress real offhand incoming reaction");
                require(!game.items().stonekivi().captureSettled(),"accepted construction blocks capture");
            }finally{remove(tech);clean();}
            owner.inventoryMenu.getCraftSlots().setItem(0,source.copy());hit();require(service.count()==0,"craft-only pot does not open whole-inventory incoming predicate");owner.inventoryMenu.getCraftSlots().clearContent();
        }
        void begin(){
            var loot=game.lbc().rollTable(base.level,owner.position(),owner,"lbc:ported/gaara_pot");
            require(loot.size()==1,"one isolated source leaf");source=loot.getFirst();
            require(source.is(LbcClownItems.POT)&&source.getCount()==1&&LbcGaaraPot.matches(source)
                    &&Identifier.parse("lbc:sand_pot").equals(source.get(DataComponents.ITEM_MODEL)),"native source pot identity/flag/model");
            policies();clean();
            var center=owner.blockPosition();stone=center.offset(3,4,0);web=center.offset(4,4,0);water=center.offset(5,4,0);
            edge=center.offset(10,10,10);outside=center.offset(11,0,0);air=center.offset(6,4,0);
            base.level.setBlock(stone,Blocks.STONE.defaultBlockState(),2);base.level.setBlock(web,Blocks.COBWEB.defaultBlockState(),2);
            base.level.setBlock(water,Blocks.WATER.defaultBlockState(),2);base.level.setBlock(edge,Blocks.STONE.defaultBlockState(),2);
            base.level.setBlock(outside,Blocks.STONE.defaultBlockState(),2);base.level.setBlock(air,Blocks.AIR.defaultBlockState(),2);
            var first=pot(9,2);var second=pot(10,2);var offhand=pot(40,2);base.base.recording.packets.clear();hit();
            require(first.getCount()==1&&second.getCount()==2&&offhand.getCount()==2&&service.count()==1,"whole inventory triggers once and debits exactly one across copies");
            var levitation=owner.getEffect(MobEffects.LEVITATION);var slow=owner.getEffect(MobEffects.SLOW_FALLING);
            require(levitation!=null&&levitation.getAmplifier()==15&&levitation.getDuration()==40&&slow!=null&&slow.getAmplifier()==0&&slow.getDuration()==400,"exact source native effect clocks");
            var particles=base.base.recording.packets.stream().filter(ClientboundLevelParticlesPacket.class::isInstance).map(ClientboundLevelParticlesPacket.class::cast)
                    .filter(p->p.particle() instanceof ItemParticleOption).toList();
            require(particles.size()==1,"one source item particle batch");var packet=particles.getFirst();var particle=((ItemParticleOption)packet.particle()).getItem().create();
            require(packet.count()==30&&packet.xDist()==.3f&&packet.yDist()==.3f&&packet.zDist()==.3f&&packet.y()==owner.getY()+1.3
                    &&particle.is(Items.REPEATING_COMMAND_BLOCK)&&Identifier.parse("lbc:sand_pot").equals(particle.get(DataComponents.ITEM_MODEL)),"source30 particles retain model/spread/height");
            // The next lower native hurt is rejected by the real cooldown and cannot consume another pot.
            long criteria=game.items().incomingArtifacts().criterionCount();boolean repeated=owner.hurtServer(base.level,owner.damageSources().cactus(),.5f);
            require(!repeated&&criteria==game.items().incomingArtifacts().criterionCount()&&first.getCount()==1&&service.count()==1,"rejected native hurt does not repeat reaction");
            owner.getInventory().clearContent();started=game.server().getTickCount();
        }
        boolean step(){
            require(game.server().getTickCount()-started<40,"bounded shared native construction/falling progress");
            if(stage==0){if(service.count()!=0)return false;
                for(var pos:List.of(stone,web,water,edge))require(base.level.getBlockState(pos).is(Blocks.SAND),"tag target replaced through inclusive21cube "+pos);
                require(base.level.getBlockState(outside).is(Blocks.STONE)&&base.level.getBlockState(air).isAir(),"outside cube and nontarget air preserved");
                require(game.items().stonekivi().captureSettled(),"completed construction permits capture");stage=1;
            }
            boolean falling=false;for(var entity:base.level.getAllEntities())if(entity instanceof FallingBlockEntity block&&!base.base.before.contains(entity)&&block.getBlockState().is(Blocks.SAND))falling=true;
            if(!falling)return false;
            require(!base.level.getBlockState(stone).is(Blocks.SAND),"normal native fill updates schedule unsupported sand fall");return true;
        }
        public void close(){if(closed)return;closed=true;try{game.items().stonekivi().stop();for(var cell:extra.entrySet())game.maps().restoreSnapshot(base.level,cell.getKey(),cell.getValue());}finally{base.close();}}
    }
    public static void start(GameController game){require(running==null,"exclusive fixture");running=new State(game);try{running.begin();}catch(Throwable failure){running.close();running=null;throw failure;}SkyWars2.LOGGER.info("[check] START gaara-pot");}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS gaara-pot (source native leaf; real incoming environmental criterion and rejected cooldown; inventory/offhand single debit across copies; MAG beacon/artifact TECH input/world TimeStop; exact effects/model particles; shared accepted-work capture gate; inclusive tag fill/nontarget boundary; actual scheduled native falling sand)");}catch(Throwable failure){try{running.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL gaara-pot",failure);}}
}
