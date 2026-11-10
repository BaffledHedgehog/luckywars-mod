package me.skywars2.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** One startup bridge to Polymer's private mining cooldown; no reflective tick access. */
public final class SkyWarsMixinPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String mixinPackage){}
    @Override public String getRefMapperConfig(){return null;}
    @Override public boolean shouldApplyMixin(String targetClassName,String mixinClassName){return true;}
    @Override public void acceptTargets(Set<String> mine,Set<String> others){}
    @Override public List<String> getMixins(){return null;}
    @Override public void preApply(String target,ClassNode node,String mixin,IMixinInfo info){}
    @Override public void postApply(String target,ClassNode node,String mixin,IMixinInfo info){
        if(!mixin.equals("me.skywars2.mixin.PolymerMiningCadenceMixin"))return;
        var field=node.fields.stream().filter(f->f.desc.equals("I")&&f.name.endsWith("polymer$blockBreakingCooldown")).toList();
        var bridge=node.methods.stream().filter(m->m.name.endsWith("skywars2$elapsedCooldown")&&m.desc.equals("(I)I")).toList();
        if(field.size()!=1||bridge.size()!=1)throw new IllegalStateException("Unsupported Polymer mining layout");
        int reads=0;
        for(var method:node.methods)for(var instruction:method.instructions.toArray()){
            if(!(instruction instanceof FieldInsnNode read)||read.getOpcode()!=Opcodes.GETFIELD
                    ||!read.owner.equals(node.name)||!read.name.equals(field.getFirst().name))continue;
            var code=new InsnList();code.add(new VarInsnNode(Opcodes.ALOAD,0));code.add(new InsnNode(Opcodes.SWAP));
            code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,node.name,bridge.getFirst().name,"(I)I",false));
            method.instructions.insert(instruction,code);reads++;
        }
        // Both the progress handler (test + decrement) and START's native delay use it.
        if(reads!=3)throw new IllegalStateException("Unsupported Polymer mining reads: "+reads);
    }
}
