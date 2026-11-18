package me.skywars2.game;

import java.util.List;
import me.skywars2.SkyWars2;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Actual field inventories, native music arrows, and both grenade cloud variants. */
final class SmokeStaffSuppressionChecks {
    private SmokeStaffSuppressionChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Smoke/staff suppression: "+why);}
    static void run(GameController game){
        var smoke=game.items().reactiveArtifacts();var music=game.items().musicStaff();var magic=game.items().magic();
        require(smoke.cloudCount()==0&&music.arrows().isEmpty(),"exclusive scoped emitters");
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();Vec3 center=f.home.add(0,25,0);f.move(f.owner,center.add(-30,0,0));f.move(f.other,center.add(30,0,0));
            for(boolean old:new boolean[]{false,true}){
                smoke.grenadeSmoke(f.owner,center,old);require(smoke.cloudCount()==1,"real grenade cloud before suppression");
                var cloud=((List<?>)WoolBucketChecks.read(smoke,"clouds")).getFirst();
                var carrier=(ItemEntity)WoolBucketChecks.read(cloud,"carrier");
                if(!old){
                    f.own(carrier);require(carrier.entityTags().contains("mecha"),"native dropped carrier retains mechanism identity");
                    carrier.setPos(center.add(1,0,0));smoke.tick();
                    require(carrier.position().equals(WoolBucketChecks.read(cloud,"center")),"smoke center follows displaced native carrier before cleanup");
                }
                f.field(true,center);smoke.tick();
                require(smoke.cloudCount()==0&&(carrier==null||carrier.isRemoved()),"TECH removes grenade emitter and physical carrier");
                smoke.grenadeSmoke(f.owner,center,old);require(smoke.cloudCount()==0,"a newly published grenade inside TECH cannot persist");
                f.removeFields();
            }
            f.move(f.owner,center);f.move(f.other,center.add(0,0,40));f.owner.setYRot(0);f.owner.setXRot(0);
            var staff=f.leaf("music_staff");f.owner.setItemInHand(InteractionHand.MAIN_HAND,staff);game.items().mana().set(f.owner,5000);
            f.field(false,center);int mana=game.items().mana().get(f.owner);
            require(!music.use(f.owner,InteractionHand.MAIN_HAND)&&music.arrows().isEmpty()&&game.items().mana().get(f.owner)==mana,
                    "MAGIC beacon rejects the cast before ammo or mana changes");f.removeFields();
            for(boolean artifact:new boolean[]{false,true}){
                Vec3 field=center.add(0,0,25);
                if(artifact){
                    f.move(f.other,field);f.other.getInventory().setItem(9,new ItemStack(LbcMagic.MYSTICAL_ARTIFACT));
                    magic.inventoryChanged(f.other);magic.tick();require(magic.artifactActive(f.other),"actual carried anti-magic artifact indexed");
                }else f.field(false,field);
                game.items().mana().set(f.owner,5000);require(music.use(f.owner,InteractionHand.MAIN_HAND),"cast outside MAGIC field succeeds");
                var shot=music.arrows().stream().filter(s->!s.isRemoved()).findFirst().orElseThrow();f.own(shot);require(magic.magical(shot),"music arrows share the ordinary magical actor policy");
                shot.setPos(field.add(0,1,-21));shot.setDeltaMovement(0,0,3);shot.tick();
                require(shot.isRemoved(),"in-flight native segment is stopped at "+(artifact?"artifact":"beacon")+" boundary");
                if(artifact){
                    f.move(f.owner,field);int before=game.items().mana().get(f.owner);
                    require(!music.use(f.owner,InteractionHand.MAIN_HAND)&&game.items().mana().get(f.owner)==before,"artifact rejects a local cast too");
                    f.other.getInventory().setItem(9,ItemStack.EMPTY);magic.inventoryChanged(f.other);magic.tick();
                }else f.removeFields();
            }
        }finally{smoke.stop();for(var shot:music.arrows())shot.discard();}
        SkyWars2.LOGGER.info("[check] PASS smoke-staff-suppression (TECH removes both smoke emitters; beacon/artifact cast admission and real arrow boundary crossing)");
    }
}
