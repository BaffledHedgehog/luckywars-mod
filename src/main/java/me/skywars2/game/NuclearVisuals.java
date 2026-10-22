package me.skywars2.game;

import com.mojang.math.Transformation;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ManualAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Один shader-носитель на видимый взрыв; 16 слотов выделяются отдельно каждому клиенту. */
public final class NuclearVisuals {
    private static final int SLOT_COUNT=16, GLOW_COLOR=15180800;
    // The shader fades for four seconds after removal: six active seconds + tail = ten.
    private static final int SLOT_TAIL=80, LIFETIME=200-SLOT_TAIL, UPDATE_TICKS=2;
    private final GameController game;
    private final List<Effect> effects=new ArrayList<>();
    private final Map<ServerPlayer,Slot[]> viewers=new IdentityHashMap<>();
    private int clock;

    private static final class Effect {
        final ServerLevel level;
        final Vec3 origin, displayOrigin;
        final float radius;
        final String epoch;
        final boolean cancelInAntimagic;
        int age;
        Effect(ServerLevel level,Vec3 origin,float radius,String epoch,boolean magical) {
            this.level=level; this.origin=origin; this.displayOrigin=origin.add(0,-3,0);
            this.radius=radius; this.epoch=epoch;
            cancelInAntimagic=magical;
        }
    }
    private static final class Slot {
        Effect effect;
        ElementHolder holder;
        ItemDisplayElement display;
        int availableAt;
    }

    NuclearVisuals(GameController game) { this.game=game; }
    public int count() { return effects.size(); }

    /** Общий вход для будущих legacy nuclear-маркеров; разрушения и урон остаются у вызывающего. */
    public void show(ServerLevel level,Vec3 origin,float radius,boolean cancelInAntimagic) {
        if(level==null || !(radius>0) || !Float.isFinite(radius)
                || !Double.isFinite(origin.x) || !Double.isFinite(origin.y) || !Double.isFinite(origin.z))
            throw new IllegalArgumentException("Nuclear origin and radius must be finite");
        effects.add(new Effect(level,origin,radius,game.items().epoch(),cancelInAntimagic));
        refreshViewers();
    }

    public void tick() {
        if(!game.server().tickRateManager().runsNormally()) return;
        clock++;
        if(effects.isEmpty() && viewers.isEmpty()) return;
        boolean removed=effects.removeIf(effect->!game.items().currentEpoch(effect.epoch)
                || effect.cancelInAntimagic && game.items().magic().suppressedAt(effect.level,effect.origin)
                || !game.items().timeStop().stopped(effect.level) && ++effect.age>=LIFETIME);
        // Перевыбор ближайших — раз в полсекунды, без сортировки всего списка взрывов.
        if(removed || clock%10==0 || effects.isEmpty()) refreshViewers();
        if(clock%UPDATE_TICKS!=0) return;
        for(var slots:viewers.values()) for(var slot:slots) {
            if(slot.effect==null) continue;
            if(game.items().timeStop().stopped(slot.effect.level)) continue;
            slot.display.setScale(scale(slot.effect));
            // Polymer startInterpolation принудительно помечает START_INTERPOLATION dirty,
            // даже когда его значение снова 0: это обязательная часть shader-контракта.
            slot.display.setStartInterpolation(0);
            slot.holder.tick();
        }
    }

    private static Vector3f scale(Effect effect) {
        // Y — возраст, а не геометрическая высота процедурного гриба.
        return new Vector3f(effect.radius,1+effect.age/20f,effect.radius);
    }

    private void refreshViewers() {
        var players=game.server().getPlayerList().getPlayers();
        var connected=new HashSet<>(players);
        for(var iterator=viewers.entrySet().iterator();iterator.hasNext();) {
            var entry=iterator.next();
            if(!connected.contains(entry.getKey())) {
                for(var slot:entry.getValue()) release(slot);
                iterator.remove();
            }
        }
        for(var player:players) {
            var wanted=nearest(player);
            var slots=viewers.get(player);
            if(slots==null) {
                if(wanted.isEmpty()) continue;
                slots=new Slot[SLOT_COUNT];
                java.util.Arrays.setAll(slots,index->new Slot());
                viewers.put(player,slots);
            }
            for(var slot:slots) if(slot.effect!=null && !wanted.contains(slot.effect)) release(slot);
            for(var effect:wanted) {
                boolean present=false;
                for(var slot:slots) if(slot.effect==effect) { present=true; break; }
                if(present) continue;
                for(int index=0;index<slots.length;index++) {
                    var slot=slots[index];
                    if(slot.effect==null && clock>=slot.availableAt) { attach(player,slot,effect,index); break; }
                }
            }
        }
        viewers.entrySet().removeIf(entry->{
            for(var slot:entry.getValue()) if(slot.effect!=null || clock<slot.availableAt) return false;
            return true;
        });
    }

    private List<Effect> nearest(ServerPlayer player) {
        var best=new ArrayList<Effect>(SLOT_COUNT);
        var position=player.position();
        for(var effect:effects) {
            if(player.level()!=effect.level) continue;
            double distance=position.distanceToSqr(effect.origin);
            int at=0;
            while(at<best.size() && position.distanceToSqr(best.get(at).origin)<=distance) at++;
            if(at<SLOT_COUNT) { best.add(at,effect); if(best.size()>SLOT_COUNT) best.removeLast(); }
        }
        return best;
    }

    private void attach(ServerPlayer player,Slot slot,Effect effect,int index) {
        var stack=new ItemStack(Items.PAPER);
        stack.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:nuclear_mushroom"));
        var display=new ItemDisplayElement(stack);
        display.setItemDisplayContext(ItemDisplayContext.NONE);
        display.setBillboardMode(Display.BillboardConstraints.FIXED);
        display.setGlowing(true); display.setGlowColorOverride(GLOW_COLOR+index);
        display.setDisplaySize(0,0); display.setViewRange(64);
        display.setTransformation(new Transformation(new Vector3f(),new Quaternionf(),scale(effect),new Quaternionf()));
        display.setInterpolationDuration(UPDATE_TICKS);
        var holder=new ElementHolder(); holder.addElement(display);
        // Ручная привязка исключает зависимость от tracking range маленькой native-сущности
        // и не загружает чанк эпицентра. У каждого клиента свои 16 shader-слотов.
        new ManualAttachment(holder,effect.level,()->effect.displayOrigin);
        slot.effect=effect; slot.display=display; slot.holder=holder;
        holder.startWatching(player); holder.tick();
    }

    private void release(Slot slot) {
        if(slot.holder==null) return;
        slot.holder.destroy(); slot.holder=null; slot.display=null; slot.effect=null;
        // Клиент хранит затухающий хвост 4 секунды; повторный slot до этого его перезапишет.
        slot.availableAt=clock+SLOT_TAIL;
    }

    public void stop() {
        effects.clear();
        for(var slots:viewers.values()) for(var slot:slots) release(slot);
        // Карантин сохраняется между матчами, пока тот же клиент подключён.
    }

    List<ItemDisplayElement> displays(ServerPlayer player) {
        var result=new ArrayList<ItemDisplayElement>(); var slots=viewers.get(player);
        if(slots!=null) for(var slot:slots) if(slot.display!=null) result.add(slot.display);
        return List.copyOf(result);
    }
    List<Vec3> origins(ServerPlayer player) {
        var result=new ArrayList<Vec3>(); var slots=viewers.get(player);
        if(slots!=null) for(var slot:slots) if(slot.effect!=null) result.add(slot.effect.origin);
        return List.copyOf(result);
    }
}
