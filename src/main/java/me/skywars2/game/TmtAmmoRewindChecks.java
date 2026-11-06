package me.skywars2.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

/** A whole coordinator capture restores accepted native carriers and their private payload cursors. */
public final class TmtAmmoRewindChecks {
    private TmtAmmoRewindChecks(){}
    private static Scene running;
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT ammo rewind: "+why);}
    public static void run(GameController game){
        require(running==null,"exclusive scheduled scene");var scene=new Scene(game);running=scene;
        try{scene.begin();}catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}
    }
    public static void tick(GameController game){
        var scene=running;if(scene==null)return;
        try{require(scene.game==game,"same native controller");if(!scene.step())return;running=null;scene.close();
            SkyWars2.LOGGER.info("[check] PASS tmt-ammo-rewind (actual accepted arrow/shulker, partial payload ages, whole coordinator native body/owner/ammo/state restoration, TimeStop and resumed native cursors)");
        }catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL tmt-ammo-rewind",error);}
    }
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;
        final HashSet<UUID> initial=new HashSet<>();final int started;
        final List<Projectile> carriers=new ArrayList<>();final List<Vec3> positions=new ArrayList<>();
        final List<Integer> ages=new ArrayList<>();final List<ItemStack> ammunition=new ArrayList<>();
        int phase,when;boolean restoring,closed;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,true);started=game.server().getTickCount();for(var e:f.level.getAllEntities())initial.add(e.getUUID());}
        ItemStack weapon(boolean shulker){
            var stack=LbcTmtItems.prototype(3);var root=stack.get(DataComponents.CUSTOM_DATA).copyTag();var slots=new ArrayList<LbcTmtAbilities.Ability>();
            if(shulker){var data=new net.minecraft.nbt.CompoundTag();data.putInt("shulker_new",1);slots.add(new LbcTmtAbilities.Ability("check/shulker",CustomData.of(data)));}
            for(var item:List.of(LbcImbaNativeItems.QUIVER_EVOKER_ARROW,LbcImbaNativeItems.QUIVER_PARALYZING_ARROW)){
                var data=item.getDefaultInstance().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();var id=BuiltInRegistries.ITEM.getKey(item);
                data.putString(LbcTmtAbilities.NATIVE,id.toString());slots.add(new LbcTmtAbilities.Ability("native/ammo/"+id.getPath(),CustomData.of(data)));
            }
            LbcTmtAbilities.write(root,slots);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(root));return stack;
        }
        void fire(boolean shulker){
            var before=new HashSet<UUID>();for(var e:f.level.getAllEntities())before.add(e.getUUID());
            var stack=weapon(shulker);var ammo=new ItemStack(Items.ARROW,5);f.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);f.owner.setItemInHand(InteractionHand.OFF_HAND,ammo);
            stack.set(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.ofNonEmpty(List.of(ammo.copyWithCount(1))));
            require(f.owner.gameMode.useItem(f.owner,f.level,stack,InteractionHand.MAIN_HAND).consumesAction(),"real native loaded crossbow release");
            Projectile body=null;for(var e:f.level.getAllEntities())if(!before.contains(e.getUUID())&&e instanceof Projectile candidate&&!candidate.isRemoved()&&(shulker?candidate instanceof ShulkerBullet:candidate instanceof AbstractArrow)){
                require(body==null,"one actual accepted native output");body=candidate;
            }
            require(body!=null,"native output published");f.own(body);carriers.add(body);
            body.setPos(f.home.add(carriers.size()*3,500,0));body.setNoGravity(true);body.setDeltaMovement(Vec3.ZERO);
            ammunition.add(shulker?game.items().shulkerBow().ammo((ShulkerBullet)body):((AbstractArrow)body).getPickupItemStackOrigin().copy());
            require(LbcTmtAmmoPayloads.state(body)!=null&&LbcTmtAmmoPayloads.state(body).entries.size()==2,"two typed properties installed at native acceptance");
        }
        void begin(){f.prepareParticipants();fire(false);fire(true);next(0);}
        void next(int next){phase=next;when=game.server().getTickCount();}
        void saved(){positions.clear();ages.clear();for(var body:carriers){positions.add(body.position());var state=LbcTmtAmmoPayloads.state(body);require(state!=null&&state.entries.size()==2,"two live native properties");require(state.entries.getFirst().age>0,"actual scheduler advanced the payload");ages.add(state.entries.getFirst().age);}}
        void verify(){for(int i=0;i<carriers.size();i++){
            var body=carriers.get(i);var state=LbcTmtAmmoPayloads.state(body);
            require(!body.isRemoved()&&f.level.getEntity(body.getUUID())==body&&body.getOwner()==f.owner,"same registered native body/UUID and owner");
            require(state!=null&&state.entries.size()==2&&state.entries.getFirst().age==ages.get(i)&&state.entries.getLast().age==ages.get(i),"exact independent partial payload ages");
            require(body.position().equals(positions.get(i)),"native pose held and restored");
            var ammo=body instanceof ShulkerBullet bullet?game.items().shulkerBow().ammo(bullet):((AbstractArrow)body).getPickupItemStackOrigin();
            require(ItemStack.matches(ammo,ammunition.get(i)),"real original ammo restored independently of properties");
        }}
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();require(game.server().getTickCount()-started<900,"bounded scene phase="+phase);
            if(restoring&&game.vanity().busy())for(var player:game.server().getPlayerList().getPlayers())if(player.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            int elapsed=game.server().getTickCount()-when;
            if(phase==0){if(elapsed<3)return false;saved();f.pause();require(game.vanity().requestCapture(f.owner,false),"whole coordinator accepts partial native property state");next(1);return false;}
            if(phase==1){if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;verify();f.thaw();next(2);return false;}
            if(phase==2){if(elapsed<3)return false;for(int i=0;i<carriers.size();i++)require(LbcTmtAmmoPayloads.state(carriers.get(i)).entries.getFirst().age>ages.get(i),"live timeline advanced after capture");f.pause();for(var body:carriers)f.discard(body);require(game.vanity().requestRestoreForCheck(),"restore discarded native property carriers");restoring=true;next(3);return false;}
            if(phase==3){if(game.vanity().busy())return false;verify();require(game.items().timeStop().stopped(f.level),"captured TimeStop restored");game.vanity().stop();f.thaw();next(4);return false;}
            if(phase==4){if(elapsed<2)return false;for(int i=0;i<carriers.size();i++)require(!carriers.get(i).isRemoved()&&LbcTmtAmmoPayloads.state(carriers.get(i)).entries.getFirst().age>ages.get(i),"restored native payload resumes its own active cursor");return true;}
            return false;
        }
        public void close(){if(closed)return;closed=true;try{game.vanity().stop();if(f.stopped)f.thaw();for(var e:f.level.getAllEntities())if(!initial.contains(e.getUUID())){f.own(e);game.items().entityUnloaded(e);e.discard();}}finally{f.close();}}
    }
}
