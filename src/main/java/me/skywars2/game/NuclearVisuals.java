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

/** Per-client shader slots; each retiring main has an independent fade carrier. */
public final class NuclearVisuals {
    private static final int SLOT_COUNT=16, GLOW_COLOR=15180800, FADE_COLOR=15181056;
    // The shader fades for four seconds after removal: six active seconds + tail = ten.
    private static final int SLOT_TAIL=90, LIFETIME=120, UPDATE_TICKS=2;
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
        ElementHolder fadeHolder;
        ItemDisplayElement fade;
        boolean retiring, natural;
        int fixedAge, stagedAt, mainRemovedAt=-1;
        int availableAt;
    }

    NuclearVisuals(GameController game) { this.game=game; }
    public int count() { return effects.size(); }
    int clock() { return clock; }

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
        for(var effect:effects) if(!game.items().timeStop().stopped(effect.level)) effect.age++;
        // Stage the natural handoff one active tick before the six-second deadline.
        for(var entry:viewers.entrySet()) for(int index=0;index<SLOT_COUNT;index++) {
            var slot=entry.getValue()[index];
            if(slot.effect!=null && entry.getKey().level()==slot.effect.level
                    && !slot.retiring && slot.fade==null && slot.effect.age>=LIFETIME-1)
                stageFade(entry.getKey(),slot,index,LIFETIME,true);
        }
        boolean removed=effects.removeIf(effect->!game.items().currentEpoch(effect.epoch)
                || effect.cancelInAntimagic && game.items().magic().suppressedAt(effect.level,effect.origin)
                || effect.age>=LIFETIME);
        maintainSlots();
        // Перевыбор ближайших — раз в полсекунды, без сортировки всего списка взрывов.
        if(removed || clock%10==0 || effects.isEmpty()) refreshViewers();
        maintainSlots();
        if(clock%UPDATE_TICKS!=0) return;
        for(var slots:viewers.values()) for(var slot:slots) {
            if(slot.effect==null || slot.retiring || slot.display==null) continue;
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

    private static Vector3f scale(Effect effect,int age) {
        return new Vector3f(effect.radius,1+age/20f,effect.radius);
    }

    private void refreshViewers() {
        var players=game.server().getPlayerList().getPlayers();
        var connected=new HashSet<>(players);
        for(var iterator=viewers.entrySet().iterator();iterator.hasNext();) {
            var entry=iterator.next();
            if(!connected.contains(entry.getKey())) {
                for(var slot:entry.getValue()) destroyImmediately(slot);
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
            for(int index=0;index<slots.length;index++) {
                var slot=slots[index];
                if(slot.effect!=null && player.level()!=slot.effect.level) destroyImmediately(slot);
                else if(slot.effect!=null && !slot.retiring && !wanted.contains(slot.effect))
                    retire(player,slot,index);
            }
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
        var display=createDisplay(effect,index,false,effect.age);
        var holder=createHolder(player,effect,display);
        slot.effect=effect; slot.display=display; slot.holder=holder;
        slot.retiring=false; slot.natural=false; slot.mainRemovedAt=-1;
        slot.stagedAt=0; slot.fixedAge=0; slot.availableAt=0;
        if(effect.age>=LIFETIME-1) stageFade(player,slot,index,LIFETIME,true);
    }

    private ItemDisplayElement createDisplay(Effect effect,int index,boolean fading,int age) {
        var stack=new ItemStack(Items.PAPER);
        stack.set(DataComponents.ITEM_MODEL,Identifier.parse(fading?"lbc:nuclear_mushroom_fade":"lbc:nuclear_mushroom"));
        var display=new ItemDisplayElement(stack);
        display.setItemDisplayContext(ItemDisplayContext.NONE);
        display.setBillboardMode(Display.BillboardConstraints.FIXED);
        display.setGlowing(true); display.setGlowColorOverride((fading?FADE_COLOR:GLOW_COLOR)+index);
        display.setDisplaySize(0,0); display.setViewRange(64);
        display.setTransformation(new Transformation(new Vector3f(),new Quaternionf(),scale(effect,age),new Quaternionf()));
        display.setInterpolationDuration(fading?0:UPDATE_TICKS);
        return display;
    }

    private ElementHolder createHolder(ServerPlayer player,Effect effect,ItemDisplayElement display) {
        var holder=new ElementHolder(); holder.addElement(display);
        // Ручная привязка исключает зависимость от tracking range маленькой native-сущности
        // и не загружает чанк эпицентра. У каждого клиента свои 16 shader-слотов.
        new ManualAttachment(holder,effect.level,()->effect.displayOrigin);
        holder.startWatching(player); holder.tick();
        return holder;
    }

    private void stageFade(ServerPlayer player,Slot slot,int index,int age,boolean natural) {
        if(slot.fade!=null) return;
        slot.fixedAge=age; slot.natural=natural; slot.stagedAt=clock;
        slot.fade=createDisplay(slot.effect,index,true,age);
        slot.fadeHolder=createHolder(player,slot.effect,slot.fade);
    }

    private void retire(ServerPlayer player,Slot slot,int index) {
        if(slot.retiring) return;
        boolean natural=slot.effect.age>=LIFETIME;
        stageFade(player,slot,index,natural?LIFETIME:slot.effect.age,natural);
        // A stop during the staged natural handoff freezes at the actual early age.
        if(!natural && slot.natural) {
            slot.natural=false; slot.fixedAge=slot.effect.age;
            slot.fade.setScale(scale(slot.effect,slot.fixedAge)); slot.fadeHolder.tick();
        }
        slot.retiring=true;
        slot.availableAt=0; // The retention deadline starts only after main removal.
    }

    private void maintainSlots() {
        var connected=new HashSet<>(game.server().getPlayerList().getPlayers());
        for(var iterator=viewers.entrySet().iterator();iterator.hasNext();) {
            var entry=iterator.next();
            if(!connected.contains(entry.getKey())) {
                for(var slot:entry.getValue()) destroyImmediately(slot);
                iterator.remove(); continue;
            }
            for(var slot:entry.getValue()) {
                if(slot.effect==null) continue;
                if(entry.getKey().level()!=slot.effect.level) { destroyImmediately(slot); continue; }
                if(slot.retiring && slot.holder!=null && clock>slot.stagedAt
                        && (!slot.natural || slot.effect.age>=LIFETIME)) {
                    // Send the terminal age before removal; fade remains independently tracked.
                    slot.display.setScale(scale(slot.effect,slot.fixedAge));
                    slot.display.setStartInterpolation(0); slot.holder.tick();
                    slot.holder.destroy(); slot.holder=null; slot.display=null;
                    slot.mainRemovedAt=clock; slot.availableAt=clock+SLOT_TAIL;
                }
                if(slot.mainRemovedAt>=0 && clock>=slot.availableAt) {
                    slot.fadeHolder.destroy(); slot.fadeHolder=null; slot.fade=null; slot.effect=null;
                    slot.retiring=false;
                }
            }
        }
    }

    private void destroyImmediately(Slot slot) {
        if(slot.holder!=null) slot.holder.destroy();
        if(slot.fadeHolder!=null) slot.fadeHolder.destroy();
        if(slot.effect!=null) slot.availableAt=Math.max(slot.availableAt,clock+SLOT_TAIL);
        slot.holder=null; slot.display=null; slot.fadeHolder=null; slot.fade=null; slot.effect=null;
        slot.retiring=false;
    }

    public void stop() {
        effects.clear();
        refreshViewers();
        // Карантин сохраняется между матчами, пока тот же клиент подключён.
    }

    List<ItemDisplayElement> displays(ServerPlayer player) {
        var result=new ArrayList<ItemDisplayElement>(); var slots=viewers.get(player);
        if(slots!=null) for(var slot:slots) if(slot.display!=null && !slot.retiring) result.add(slot.display);
        return List.copyOf(result);
    }
    List<Vec3> origins(ServerPlayer player) {
        var result=new ArrayList<Vec3>(); var slots=viewers.get(player);
        if(slots!=null) for(var slot:slots) if(slot.effect!=null && !slot.retiring) result.add(slot.effect.origin);
        return List.copyOf(result);
    }

    List<ItemDisplayElement> fadeDisplays(ServerPlayer player) {
        var result=new ArrayList<ItemDisplayElement>(); var slots=viewers.get(player);
        if(slots!=null) for(var slot:slots) if(slot.fade!=null) result.add(slot.fade);
        return List.copyOf(result);
    }

    List<Vec3> fadingOrigins(ServerPlayer player) {
        var result=new ArrayList<Vec3>(); var slots=viewers.get(player);
        if(slots!=null) for(var slot:slots) if(slot.fade!=null) result.add(slot.effect.origin);
        return List.copyOf(result);
    }

    record SlotFrame(int slot,Vec3 origin,int fixedAge,int stagedAt,int mainRemovedAt,int availableAt,
                     boolean retiring,boolean natural,boolean mainPresent,boolean fadePresent) {}

    List<SlotFrame> frames(ServerPlayer player) {
        var result=new ArrayList<SlotFrame>(); var slots=viewers.get(player);
        if(slots!=null) for(int index=0;index<slots.length;index++) {
            var slot=slots[index];
            if(slot.effect!=null) result.add(new SlotFrame(index,slot.effect.origin,slot.fixedAge,
                    slot.stagedAt,slot.mainRemovedAt,slot.availableAt,slot.retiring,slot.natural,
                    slot.display!=null,slot.fade!=null));
        }
        return List.copyOf(result);
    }
}
