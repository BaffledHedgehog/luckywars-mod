package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;

/** Independent native command failure and actual attribute formula checks for the Fevgalea helper closure. */
public final class SauvojenStarChecks {
    private SauvojenStarChecks(){}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException("Sauvojen stars: "+message);}
    public static void run(GameController game){
        // swcheck itself runs inside Commands.CURRENT_EXECUTION_CONTEXT. Nested commands only queue there,
        // so begin on the native server task queue after that outer context has drained and closed.
        game.server().schedule(new net.minecraft.server.TickTask(game.server().getTickCount(),()->{
            try{verify(game);}
            catch(Throwable failure){SkyWars2.LOGGER.error("[check] FAIL sauvojen-stars",failure);}
        }));
    }
    private static void verify(GameController game){
        try(var fixture=new ElementalGrimoireChecks.Fixture(game)){
            var level=fixture.owner.level();
            var storage=game.server().getCommandStorage();
            var id=Identifier.parse("skywars2:sauvojen_star_checks");var saved=storage.get(id).copy();
            var marker=new Marker(EntityTypes.MARKER,level);
            try{
                var data=new CompoundTag();data.putInt("tmp",17);storage.set(id,data);
                var source=fixture.owner.createCommandSourceStack().withEntity(marker).withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput();
                game.server().getCommands().performPrefixedCommand(source,
                        "execute store result storage skywars2:sauvojen_star_checks tmp int 1 run data get entity @s Health 1");
                require(storage.get(id).getIntOr("tmp",-1)==0,"failed native marker Health writes result zero over preset17");
                var zombie=new Zombie(EntityTypes.ZOMBIE,level);
                try{
                    zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(25.5);
                    zombie.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(7.9);
                    zombie.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.317);
                    zombie.setHealth(1);
                    require(Math.abs(SauvojenStars.chargeDamage(zombie,1)-3.75f)<1e-6,"flavour1 floors maxHealth then multiplies .15");
                    require(SauvojenStars.chargeDamage(zombie,2)==7,"flavour2 uses floored target attackDamage despite Health1");
                    require(SauvojenStars.chargeDamage(zombie,3)==15,"flavour3 floors speed times50");
                    zombie.setHealth(20);require(SauvojenStars.chargeDamage(zombie,2)==7,"flavour2 independent of current Health");
                }finally{zombie.discard();}
            }finally{marker.discard();storage.set(id,saved);}
        }
        SkyWars2.LOGGER.info("[check] PASS sauvojen-stars (native marker Health failed-store overwrites preset17 with0; exact maxHealth/attackDamage/speed formulas)");
    }
}
