package me.skywars2.game.items.wands;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.triggers.ImpossibleTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Achievement eligibility of a cast, independent of damage ownership and held items. */
public final class WandCastOrigin {
    private WandCastOrigin() {}
    private static final String BORROWED_TAG="skywars2.borrowed_wand_cast";
    private static final ThreadLocal<Set<UUID>> BORROWED=new ThreadLocal<>();

    public static boolean borrowed(ServerPlayer owner) {
        var current=BORROWED.get();return current!=null&&current.contains(owner.getUUID());
    }
    public static boolean borrowed(Entity actor) {return actor!=null&&actor.entityTags().contains(BORROWED_TAG);}
    public static void stamp(Entity actor,ServerPlayer owner) {
        stamp(actor,borrowed(owner));
    }
    public static void stamp(Entity actor,boolean borrowed) {
        if(borrowed)actor.entityTags().add(BORROWED_TAG);
        else actor.entityTags().remove(BORROWED_TAG);
    }
    public static Scope scope(ServerPlayer owner,boolean borrowed) {
        return borrowed(owner)==borrowed?Scope.UNCHANGED:new Scope(owner.getUUID(),borrowed);
    }
    public static Scope scope(ServerPlayer owner,Entity actor) {return scope(owner,borrowed(actor));}
    public static boolean allows(ServerPlayer owner,AdvancementHolder advancement,String criterion) {
        var id=advancement.id();
        if(!borrowed(owner)||!id.getNamespace().equals("lbc")
                ||!id.getPath().startsWith("true_advancements/staffs/")
                ||id.getPath().equals("true_advancements/staffs/unstable_book"))return true;
        // Inventory and kill predicates already require the actual authored item.
        // Preserve those legacy conditions; only scripted cast rewards are borrowed.
        var rule=advancement.value().criteria().get(criterion);
        return rule!=null&&!(rule.triggerInstance() instanceof ImpossibleTrigger.TriggerInstance);
    }
    public static final class Scope implements AutoCloseable {
        private static final Scope UNCHANGED=new Scope();
        private final Set<UUID> previous;
        private boolean closed;
        private Scope() {previous=null;closed=true;}
        private Scope(UUID owner,boolean borrowed) {
            previous=BORROWED.get();var next=previous==null?new HashSet<UUID>():new HashSet<>(previous);
            if(borrowed)next.add(owner);else next.remove(owner);
            if(next.isEmpty())BORROWED.remove();else BORROWED.set(Set.copyOf(next));
        }
        @Override public void close() {
            if(closed)return;closed=true;
            if(previous==null)BORROWED.remove();else BORROWED.set(previous);
        }
    }
}
