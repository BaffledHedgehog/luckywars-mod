package me.skywars2.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;

/** Extra properties of an accepted projectile. Never ammunition, pickup replacements or a shot factory. */
public final class LbcTmtAmmoPayloads {
    private LbcTmtAmmoPayloads(){}
    public static final int MAX_PAYLOADS=7;

    /** Source components are captured once; hot callbacks use the cached native classification. */
    public static final class Payload {
        public final String family;
        private final ItemStack source;
        public final int color,variant,amp;
        final List<MobEffectInstance> effects;
        Payload(String family,ItemStack source){
            if(family==null||family.isBlank()||family.length()>128||!supported(source))throw new IllegalArgumentException("Invalid TMT ammo payload");
            this.family=family;this.source=source.copyWithCount(1);
            var contents=this.source.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY);
            color=contents.customColor().orElse(-1);variant=variant(this.source);amp=LbcQuiver.carrierAmp(this.source);
            var values=new ArrayList<MobEffectInstance>();
            contents.forEachEffect(effect->values.add(new MobEffectInstance(effect)),this.source.getOrDefault(DataComponents.POTION_DURATION_SCALE,1f));
            effects=List.copyOf(values);
        }
        public ItemStack source(){return source.copy();}
    }
    public static Payload fromSource(String family,ItemStack source){return new Payload(family,source);}
    private static final Codec<Payload> PAYLOAD_CODEC=RecordCodecBuilder.create(i->i.group(
            Codec.STRING.fieldOf("family").forGetter((Payload p)->p.family),
            ItemStack.CODEC.fieldOf("source").forGetter((Payload p)->p.source)
    ).apply(i,Payload::new));

    public static final class Entry {
        public final Payload payload;
        public int age,lastReactiveTick=Integer.MIN_VALUE,lastAgeTick=Integer.MIN_VALUE;
        public boolean terminal;
        Entry(Payload payload){this.payload=payload;}
        Entry(Payload payload,int age,boolean terminal,int reactive,int ageTick){this.payload=payload;this.age=age;this.terminal=terminal;lastReactiveTick=reactive;lastAgeTick=ageTick;}
        Entry copy(){return new Entry(payload,age,terminal,lastReactiveTick,lastAgeTick);}
        boolean expired(){return payload.amp>=101&&age>=300;}
        void advance(int now){if(lastAgeTick!=now){if(age<Integer.MAX_VALUE)age++;lastAgeTick=now;}}
    }
    private static final Codec<Entry> ENTRY_CODEC=RecordCodecBuilder.create(i->i.group(
            PAYLOAD_CODEC.fieldOf("payload").forGetter((Entry e)->e.payload),
            Codec.INT.optionalFieldOf("age",0).forGetter((Entry e)->e.age),
            Codec.BOOL.optionalFieldOf("terminal",false).forGetter((Entry e)->e.terminal),
            Codec.INT.optionalFieldOf("reactive_tick",Integer.MIN_VALUE).forGetter((Entry e)->e.lastReactiveTick),
            Codec.INT.optionalFieldOf("age_tick",Integer.MIN_VALUE).forGetter((Entry e)->e.lastAgeTick)
    ).apply(i,Entry::new));
    public static final class State {
        public final List<Entry> entries;
        LbcIndirectKills.Owner cause;
        private int loadedFrame=Integer.MIN_VALUE;
        private State(List<Entry> entries){this.entries=List.copyOf(entries);}
        private State(List<Entry> entries,int frame){this(entries);loadedFrame=frame;}
        private State(List<Entry> entries,int frame,java.util.Optional<LbcIndirectKills.Owner> cause){this(entries,frame);this.cause=cause.orElse(null);}
        public State copy(){var result=new State(entries.stream().map(Entry::copy).toList());result.loadedFrame=loadedFrame;result.cause=cause;return result;}
        /** A saved once-this-tick marker is relative to the capture frame, not the new server tick number. */
        void rebase(int now){if(loadedFrame!=Integer.MIN_VALUE){rebase(loadedFrame,now);loadedFrame=Integer.MIN_VALUE;}}
        void rebase(int frame,int now){for(var e:entries){e.lastReactiveTick=e.lastReactiveTick==frame?now:Integer.MIN_VALUE;e.lastAgeTick=e.lastAgeTick==frame?now:Integer.MIN_VALUE;}}
        public boolean hasVariant(int value){for(var entry:entries)if(entry.payload.variant==value)return true;return false;}
    }
    private static final Codec<List<Entry>> ENTRIES_CODEC=ENTRY_CODEC.listOf().validate(entries->entries.size()<=MAX_PAYLOADS
            ?DataResult.success(entries):DataResult.error(()->"Too many TMT ammo payloads"));
    private static final Codec<LbcIndirectKills.Owner> CAUSE_CODEC=RecordCodecBuilder.create(i->i.group(
            net.minecraft.core.UUIDUtil.CODEC.fieldOf("owner").forGetter(LbcIndirectKills.Owner::player),
            Codec.LONG.fieldOf("order").forGetter(LbcIndirectKills.Owner::order),
            Codec.BOOL.fieldOf("friendly").forGetter(LbcIndirectKills.Owner::friendlyFire),
            Codec.BOOL.fieldOf("projectile").forGetter(LbcIndirectKills.Owner::projectile)
    ).apply(i,LbcIndirectKills.Owner::new));
    public static final Codec<State> CODEC=RecordCodecBuilder.create(i->i.group(
            ENTRIES_CODEC.fieldOf("entries").forGetter((State s)->s.entries),
            Codec.INT.optionalFieldOf("frame_tick",Integer.MIN_VALUE).forGetter((State s)->{var game=me.skywars2.SkyWars2.game();return game==null?Integer.MIN_VALUE:game.server().getTickCount();}),
            CAUSE_CODEC.optionalFieldOf("cause").forGetter((State s)->java.util.Optional.ofNullable(s.cause))
    ).apply(i,State::new));
    private static final AttachmentType<State> DATA=AttachmentRegistry.create(Identifier.parse("skywars2:tmt_ammo_payloads"),b->b.persistent(CODEC));
    /** Force attachment codec registration before loading saved native actors. */
    public static void register(){}
    public static State state(Entity actor){var nativeState=LbcShulkerBow.payloadState(actor);return nativeState!=null?nativeState:actor.getAttached(DATA);}
    public static List<Payload> payloads(Entity actor){var state=state(actor);return state==null?List.of():state.entries.stream().map(e->e.payload).toList();}
    public static boolean hasVariant(Entity actor,int value){var state=state(actor);return state!=null&&state.hasVariant(value);}
    public static Set<Integer> variants(Entity actor){var state=state(actor);if(state==null)return Set.of();return state.entries.stream().map(e->e.payload.variant).collect(java.util.stream.Collectors.toUnmodifiableSet());}
    public static void restore(Entity actor,State state){
        var copy=state==null?null:state.copy();
        if(LbcShulkerBow.restorePayloadState(actor,copy)||copy==null)actor.removeAttached(DATA);else actor.setAttached(DATA,copy);
    }
    public static void copyPayloads(Entity source,Entity target){restore(target,state(source));}
    public static void installDescriptors(Entity actor,List<Payload> descriptors){
        if(descriptors.size()>MAX_PAYLOADS)throw new IllegalArgumentException("Too many TMT ammo properties");
        var unique=new LinkedHashMap<String,Payload>();for(var descriptor:descriptors){unique.remove(descriptor.family);unique.put(descriptor.family,descriptor);}
        restore(actor,unique.isEmpty()?null:new State(unique.values().stream().map(Entry::new).toList()));
    }
    /** Call before physical expansion, only for the real accepted family. Infinite ammo is not a weapon payload. */
    public static void install(GameController game,ServerPlayer owner,ItemStack fired,List<Projectile> family){
        if(!LbcTmtItems.matches(fired))return;
        var payloads=new ArrayList<Payload>();
        for(var ability:LbcTmtAbilities.read(fired)){
            var data=(ItemData.IntReader)(Object)ability.data();var id=Identifier.tryParse(data.skywars2$readString(LbcTmtAbilities.NATIVE,""));
            if(id==null)continue;var item=BuiltInRegistries.ITEM.getValue(id);if(!(item instanceof ArrowItem))continue;
            var source=LbcTmtComponents.source(game,ability);
            if(supported(source))payloads.add(new Payload(ability.id(),source));
        }
        if(payloads.isEmpty())return;
        for(var actor:family)if(actor instanceof AbstractArrow&&actor.getOwner()==owner&&!actor.isRemoved()&&actor.level()==owner.level()){
            installDescriptors(actor,payloads);game.items().markCurrentItemEpoch(actor);
            game.items().quiver().capturePayloads((AbstractArrow)actor);
        }
    }
    private static boolean supported(ItemStack source){
        if(source.isEmpty()||!(source.getItem() instanceof ArrowItem)||source.getItem()==LbcImbaNativeItems.INFINITE_ARROW||LbcInfiniteAmmo.matches(source))return false;
        var item=source.getItem();return LbcImbaNativeItems.specialArrows().contains(item)||LbcImbaNativeItems.quiverArrows().contains(item)
                ||item==LbcImbaNativeItems.HOMING_ARROW||LbcStand.isArrow(source);
    }
    public static boolean supports(ItemStack source){return supported(source);}
    public static int variant(ItemStack ammo){
        if(LbcStand.isArrow(ammo))return 119;
        int color=LbcQuiver.carrierColor(ammo);
        if(color==6710885||color==6710877)return 300;if(color==6710881)return 301;if(color==6710882)return 302;if(color==6710883)return 303;
        return LbcQuiver.carrierAmp(ammo);
    }
}
