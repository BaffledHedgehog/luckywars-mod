package me.skywars2.game;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;

/** Synchronous accepted-shot work waits for native criterion/stat completion and source restoration. */
public final class LbcTmtShotCompletion {
    private LbcTmtShotCompletion(){}
    private static final ThreadLocal<Frame> ACTIVE=new ThreadLocal<>();
    private static final class Frame {
        final ArrayDeque<Runnable> accepted=new ArrayDeque<>();
    }

    /** Ordinary native weapons take their original path without allocating a frame. */
    public static <T> T call(ItemStack weapon,Supplier<T> nativeAction){
        if(!LbcTmtItems.matches(weapon)&&!LbcTrollBox.matches(weapon))return nativeAction.get();
        var parent=ACTIVE.get();var frame=new Frame();ACTIVE.set(frame);boolean completed=false;
        try{T result=nativeAction.get();completed=true;return result;}
        finally{
            if(parent==null)ACTIVE.remove();else ACTIVE.set(parent);
            // A failed native action owns only its own queued continuation. It must
            // neither publish that continuation nor erase its parent's accepted work.
            if(completed&&!frame.accepted.isEmpty()){
                if(parent!=null)parent.accepted.addAll(frame.accepted);
                else LbcTmtAbilities.afterScopes(()->complete(frame.accepted));
            }
        }
    }
    public static void run(ItemStack weapon,Runnable nativeAction){call(weapon,()->{nativeAction.run();return null;});}

    /** Return ownership to the native shoot hook immediately; dispatch after the actual outer action. */
    static void afterNativeAction(Runnable continuation){
        Objects.requireNonNull(continuation);var frame=ACTIVE.get();
        if(frame==null)LbcTmtAbilities.afterScopes(continuation);else frame.accepted.addLast(continuation);
    }
    private static void complete(ArrayDeque<Runnable> accepted){
        Throwable failure=null;
        while(!accepted.isEmpty())try{accepted.removeFirst().run();}
        catch(RuntimeException|Error problem){if(failure==null)failure=problem;else if(failure!=problem)failure.addSuppressed(problem);}
        // Independently accepted nested shots keep their continuation even if a
        // sibling callback fails; the first error still propagates synchronously.
        if(failure instanceof RuntimeException problem)throw problem;
        if(failure instanceof Error problem)throw problem;
    }
}
