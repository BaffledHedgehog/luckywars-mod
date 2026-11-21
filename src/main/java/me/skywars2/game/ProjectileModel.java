package me.skywars2.game;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.EntityAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

/** A native projectile owns physics; its recipient-tracked display owns only the authored model. */
public final class ProjectileModel {
    private final Projectile body;
    private final ElementHolder holder=new ElementHolder();
    private final ItemDisplayElement display;
    public ProjectileModel(Projectile body,Identifier model){
        this.body=body;var stack=new ItemStack(Items.STICK);stack.set(DataComponents.ITEM_MODEL,model);
        display=new ItemDisplayElement(stack);display.setItemDisplayContext(ItemDisplayContext.FIXED);
        display.setInterpolationDuration(1);display.setTeleportDuration(1);display.setShadowRadius(0);display.setShadowStrength(0);
        holder.addElement(display);sync(body.position());new EntityAttachment(holder,body,false);
    }
    public void sync(Vec3 visible){
        var motion=body.getDeltaMovement();
        if(motion.lengthSqr()>1e-12){
            display.setYaw((float)-Math.toDegrees(Math.atan2(motion.x,motion.z)));
            display.setPitch((float)-Math.toDegrees(Math.atan2(motion.y,motion.horizontalDistance())));
        }
        boolean stepped=((ServerLevel)body.level()).getServer().tickRateManager().isFrozen();
        display.setTeleportDuration(stepped?0:1);display.setAlwaysSyncAbsolutePosition(stepped);
        display.setOffset(visible.subtract(body.position()));holder.tick();
    }
    public ItemDisplayElement display(){return display;}
    public void close(){holder.destroy();}
}
