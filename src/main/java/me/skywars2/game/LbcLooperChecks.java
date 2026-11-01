package me.skywars2.game;

import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import java.util.*;

/** Native recipient conversion and first-item-tick packet checks; no physical-client claims. */
public final class LbcLooperChecks {
    private LbcLooperChecks(){}
    private static void require(boolean condition,String why){if(!condition)throw new IllegalStateException("Looper: "+why);}
    private static Component translated(Component component,String key){
        if(component.getContents() instanceof TranslatableContents contents&&contents.getKey().equals(key))return component;
        for(var child:component.getSiblings()){var found=translated(child,key);if(found!=null)return found;}return null;
    }
    private static ItemStack project(ServerPlayer viewer,ItemStack source){
        var connection=new Connection(PacketFlow.SERVERBOUND){@Override public net.minecraft.network.PacketListener getPacketListener(){return viewer.connection;}};
        var provider=(PacketContextProvider)connection;ItemStack[] result={ItemStack.EMPTY};
        PacketContext.runWithContext(provider,()->result[0]=PolymerItemUtils.getPolymerItemStack(source,TooltipFlag.NORMAL,provider.getPacketContext(),viewer.registryAccess()));
        require(ItemStack.matches(source,PolymerItemUtils.getRealItemStack(result[0],provider.getPacketContext(),viewer.registryAccess())),"real Polymer recipient round-trip");return result[0];
    }
    private static void grimoire(ServerPlayer owner,ServerPlayer other,ItemStack source){
        var before=source.copy();var yes=project(owner,source);var no=project(other,source);
        var lines=yes.get(DataComponents.LORE).lines();
        var descriptions=lines.stream().map(line->translated(line,"mana_usage_2")).filter(Objects::nonNull).toList();
        require(descriptions.size()==1,"one exact mana description for "+source.getItem());
        var description=descriptions.getFirst();
        require(description.getStyle().getColor()!=null&&description.getStyle().getColor().getValue()==0xff55ff&&!description.getStyle().isItalic(),"source light purple/nonitalic label");
        require(description.getSiblings().size()==1&&description.getSiblings().getFirst().getString().equals(Integer.toString(ItemData.intValue(source,"mana",0))),"source decimal mana sibling");
        require(no.get(DataComponents.LORE).lines().stream().noneMatch(line->translated(line,"mana_usage_2")!=null),"other recipient retains unresolved lore");
        var unresolved=no.get(DataComponents.LORE).lines();
        var authored=source.get(DataComponents.LORE).styledLines().stream()
                .map(line->Component.empty().append(line).setStyle(PolymerItemUtils.CLEAN_STYLE)).toList();
        int start=Collections.indexOfSubList(unresolved,authored);
        require(start>=0&&lines.size()==unresolved.size(),"complete authored lore sequence survives native rendering");
        for(int i=0;i<lines.size();i++){
            if(i==start+1)require(translated(lines.get(i),"mana_usage_2")!=null,"cost occupies exactly source lore[1]");
            else require(lines.get(i).equals(unresolved.get(i)),"header, authored lines and native tooltip order retained at "+i);
        }
        require(ItemStack.matches(source,before),"authoritative components/count unchanged");
    }
    private static final class Capture implements AutoCloseable {
        final ServerPlayer player;final ServerGamePacketListenerImpl previous;final List<Packet<?>> packets=new ArrayList<>();
        Capture(GameController game,ServerPlayer player){this.player=player;previous=player.connection;
            var connection=new Connection(PacketFlow.SERVERBOUND){
                @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){packets.add(packet);}
                @Override public boolean isConnected(){return true;}
                @Override public boolean isMemoryConnection(){return true;}
            };
            player.connection=new ServerGamePacketListenerImpl(game.server(),connection,player,new CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));
        }
        long count(Class<?> type){return packets.stream().filter(type::isInstance).count();}
        void clear(){packets.clear();}
        @Override public void close(){player.connection=previous;}
    }
    private static ItemEntity drop(ServerPlayer owner,int imba){
        var stack=new ItemStack(Items.SNOWBALL);var tag=new CompoundTag();tag.putInt("imba",imba);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        var item=new ItemEntity(owner.level(),owner.getX(),owner.getY(),owner.getZ(),stack);item.setNoGravity(true);item.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);item.setNeverPickUp();return item;
    }
    private static void firstTick(GameController game,ServerPlayer owner,ServerPlayer other){
        try(var yes=new Capture(game,owner);var no=new Capture(game,other)){
            var item=drop(owner,1);
            try{
                yes.clear();no.clear();item.tick();
                require(yes.count(ClientboundLevelParticlesPacket.class)==1&&yes.count(ClientboundSoundPacket.class)==1,"real first native item tick sends one particle and one sound packet");
                var particles=yes.packets.stream().filter(ClientboundLevelParticlesPacket.class::isInstance).map(ClientboundLevelParticlesPacket.class::cast).findFirst().orElseThrow();
                require(particles.count()==200&&particles.overrideLimiter()&&particles.x()==item.getX()&&particles.y()==item.getY()+20&&particles.z()==item.getZ()
                        &&particles.xDist()==0&&particles.yDist()==40&&particles.zDist()==0&&particles.xMaxSpeed()==0&&particles.yMaxSpeed()==0&&particles.zMaxSpeed()==0,"exact source force particle position/spread/count/speed");
                require(particles.particle() instanceof net.minecraft.core.particles.DustParticleOptions dust&&dust.getScale()==4
                        &&dust.getColor().distance(new org.joml.Vector3f(246/255f,212/255f,1))<.00001,"source pink dust scale4");
                var sound=yes.packets.stream().filter(ClientboundSoundPacket.class::isInstance).map(ClientboundSoundPacket.class::cast).findFirst().orElseThrow();
                require(sound.getSound().value().location().toString().equals("minecraft:entity.bee.loop_aggressive")&&sound.getSource()==net.minecraft.sounds.SoundSource.MASTER&&sound.getVolume()==1&&sound.getPitch()==1,"exact source master sound1/1");
                require(no.count(ClientboundLevelParticlesPacket.class)==0&&no.count(ClientboundSoundPacket.class)==0,"ordinary viewer receives neither announcement");
                yes.clear();item.tick();require(yes.count(ClientboundLevelParticlesPacket.class)==0&&yes.count(ClientboundSoundPacket.class)==0,"native second item tick never repeats");
                var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,owner.registryAccess());item.saveWithoutId(output);
                var reloaded=drop(owner,1);try{reloaded.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,owner.registryAccess(),output.buildResult()));reloaded.tick();require(yes.count(ClientboundSoundPacket.class)==0,"native save/load retains once marker");}finally{reloaded.discard();}
            }finally{item.discard();}
            var legacy=drop(owner,1);try{legacy.entityTags().add("tagged");yes.clear();legacy.tick();require(yes.count(ClientboundSoundPacket.class)==0,"legacy tagged entity never reannounces");}finally{legacy.discard();}
            var far=drop(owner,1);far.setPos(LbcFpvDrone.viewpoint(owner).add(64,0,0));
            try{
                yes.clear();no.clear();far.tick();var sound=yes.packets.stream().filter(ClientboundSoundPacket.class::isInstance).map(ClientboundSoundPacket.class::cast).findFirst().orElseThrow();
                var expected=LbcFpvDrone.viewpoint(owner).add(far.position().subtract(LbcFpvDrone.viewpoint(owner)).normalize().scale(2));
                require(sound.getX()==(int)(expected.x*8)/8d&&sound.getY()==(int)(expected.y*8)/8d&&sound.getZ()==(int)(expected.z*8)/8d&&sound.getVolume()==1,"far minVolume1 packet uses recipient-relative two-block sound position");
                require(no.count(ClientboundSoundPacket.class)==0,"far sound also remains reader-only");
            }finally{far.discard();}
            var ordinary=drop(owner,0);var stackBefore=ordinary.getItem().copy();
            try{yes.clear();ordinary.tick();require(!ordinary.isRemoved()&&ItemStack.matches(stackBefore,ordinary.getItem())&&yes.count(ClientboundSoundPacket.class)==0&&yes.count(ClientboundLevelParticlesPacket.class)==0,"ordinary native item tick retains its stack with no Looper packets");}finally{ordinary.discard();}
            var paused=drop(owner,1);game.items().timeStop().begin(owner.level(),40);
            try{yes.clear();paused.tick();require(!paused.entityTags().contains("skywars2:lbc_looper_tagged")&&yes.count(ClientboundSoundPacket.class)==0,"paused native item defers first announcement");}
            finally{game.items().timeStop().end(owner.level());}
            try{yes.clear();paused.tick();require(yes.count(ClientboundSoundPacket.class)==1,"first resumed native tick announces once");}finally{paused.discard();}
            var disabled=drop(owner,1);game.config().lbcSkills=false;
            try{yes.clear();disabled.tick();game.config().lbcSkills=true;disabled.tick();require(yes.count(ClientboundSoundPacket.class)==0,"disabled first tick cannot replay after reenable");}finally{disabled.discard();game.config().lbcSkills=true;}
        }
    }
    public static void run(GameController game){
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        var other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
        require(owner!=null&&other!=null&&game.playing()&&GameController.operator(owner),"actual active two-player operator fixture");
        require(!game.items().timeStop().stopped(owner.level())&&game.server().tickRateManager().runsNormally(),"fixture starts on an ordinary running clock");
        var profile=game.players().get(owner.getUUID());var otherProfile=game.players().get(other.getUUID());
        int skill=profile.lbcSkill,otherSkill=otherProfile.lbcSkill,challenge=profile.lbcChallenge,otherChallenge=otherProfile.lbcChallenge;
        boolean enabled=game.config().lbcSkills,ranked=game.config().ranked,debug=owner.entityTags().contains("skywars2.debug_perks");
        var unlocks=new HashSet<>(profile.lbcUnlocks);String gate="lbc:true_advancements/accesories/mystical_artifact";
        var advancement=game.server().getAdvancements().get(net.minecraft.resources.Identifier.parse(gate));require(advancement!=null,"original source advancement exists");
        var awarded=new HashSet<String>();owner.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(awarded::add);
        try{
            game.menus().openDebug(owner);require(game.canUseMenus(owner),"actual native debug menu authorizes match selection");
            game.config().lbcSkills=true;game.config().ranked=false;profile.lbcChallenge=0;otherProfile.lbcChallenge=0;otherProfile.lbcSkill=0;
            game.lbc().refreshKnowledgeView(owner);game.lbc().refreshKnowledgeView(other);
            owner.entityTags().remove("skywars2.debug_perks");profile.lbcUnlocks.remove(gate);
            for(var key:advancement.value().criteria().keySet())owner.getAdvancements().revoke(advancement,key);
            require(!game.lbc().select(owner,LbcLoadouts.Kind.SKILLS,10000,2),"normal slot2 retains mystical-artifact unlock gate");
            for(var key:advancement.value().criteria().keySet())owner.getAdvancements().award(advancement,key);
            require(game.lbc().select(owner,LbcLoadouts.Kind.SKILLS,10000,2)&&profile.lbcSkill==3&&game.lbc().selected(profile,LbcLoadouts.Kind.SKILLS,10000,2),"source catalog slot selects skill3 with original achievement");
            game.menus().open(owner,GameMenus.Page.LBC_SKILLS);
            owner.entityTags().remove("skywars2.debug_perks");
            try{
                require(eu.pb4.sgui.api.SguiUtils.getCurrentGui(owner) instanceof eu.pb4.sgui.api.gui.SimpleGui,"actual LBC skills menu opens with ordinary unlock policy");
                var menu=(eu.pb4.sgui.api.gui.SimpleGui)eu.pb4.sgui.api.SguiUtils.getCurrentGui(owner);var button=menu.getGuiElement(2);require(button!=null,"source slot2 button exists");
                profile.lbcSkill=0;button.getGuiCallback().click(2,eu.pb4.sgui.api.ClickType.MOUSE_LEFT,net.minecraft.world.inventory.ContainerInput.PICKUP,menu);
                require(profile.lbcSkill==3,"actual ordinary menu callback selects Looper");
            }finally{owner.closeContainer();}
            game.lbc().refreshArtifactView(other);int books=0;
            for(var definition:BuiltInRegistries.ITEM){var stack=new ItemStack(definition);if(ItemData.intValue(stack,"unresolved_grimoire",0)==1&&stack.getOrDefault(DataComponents.LORE,ItemLore.EMPTY).lines().size()>=2){grimoire(owner,other,stack);books++;}}
            require(books>=LbcElementalGrimoires.Kind.values().length+1,"all registered ordinary/variant/advanced/extra and Danmaku grimoires checked");
            for(int mana:new int[]{0,1200,-27,Integer.MIN_VALUE,Integer.MAX_VALUE}){
                var vanilla=new ItemStack(Items.SNOWBALL,7);var tag=new CompoundTag();tag.putInt("unresolved_grimoire",1);tag.putInt("mana",mana);tag.putString("neighbor","retained");vanilla.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));vanilla.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("header"),Component.empty(),Component.literal("authored description"))));grimoire(owner,other,vanilla);
            }
            for(var duplicateLore:List.of(
                    List.<Component>of(Component.literal("same"),Component.literal("same"),Component.literal("distinct tail")),
                    List.<Component>of(Component.literal("same"),Component.literal("same"),Component.literal("same")))){
                var duplicate=new ItemStack(Items.SNOWBALL,5);var tag=new CompoundTag();tag.putInt("unresolved_grimoire",1);tag.putInt("mana",437);duplicate.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));duplicate.set(DataComponents.LORE,new ItemLore(duplicateLore));grimoire(owner,other,duplicate);
            }
            var hiddenBook=new ItemStack(LbcElementalGrimoires.Kind.values()[0].item);var hiddenBefore=hiddenBook.copy();
            profile.lbcChallenge=1;game.lbc().refreshKnowledgeView(owner);
            try{
                var hidden=project(owner,hiddenBook);var hiddenLore=hidden.get(DataComponents.LORE).lines();
                require(hidden.getHoverName().getString().equals("????????????????????????????")&&hiddenLore.size()==1&&hiddenLore.getFirst().getString().equals("????????????????????????????")
                        &&hiddenLore.stream().noneMatch(line->translated(line,"mana_usage_2")!=null),"actual challenge1 recipient mask takes precedence over grimoire mana projection");
                require(ItemStack.matches(hiddenBook,hiddenBefore),"knowledge hiding preserves actual grimoire components");
            }finally{profile.lbcChallenge=0;game.lbc().refreshKnowledgeView(owner);}
            firstTick(game,owner,other);
            game.config().lbcSkills=false;game.lbc().refreshArtifactView(owner);require(!LbcSkills.canReadArtifacts(owner),"global flag revokes recipient knowledge");
        }finally{
            owner.closeContainer();
            profile.lbcSkill=skill;otherProfile.lbcSkill=otherSkill;profile.lbcChallenge=challenge;otherProfile.lbcChallenge=otherChallenge;profile.lbcUnlocks.clear();profile.lbcUnlocks.addAll(unlocks);
            for(var key:advancement.value().criteria().keySet())if(awarded.contains(key))owner.getAdvancements().award(advancement,key);else owner.getAdvancements().revoke(advancement,key);
            game.config().lbcSkills=enabled;game.config().ranked=ranked;if(debug)owner.entityTags().add("skywars2.debug_perks");else owner.entityTags().remove("skywars2.debug_perks");
            game.lbc().refreshKnowledgeView(owner);game.lbc().refreshKnowledgeView(other);game.lbc().refreshArtifactView(owner);game.lbc().refreshArtifactView(other);game.players().save();
        }
        me.skywars2.SkyWars2.LOGGER.info("[check] PASS looper (native recipient roundtrips, original gate/menu, exact first-tick packets, save/load and Time Stop)");
    }
}
