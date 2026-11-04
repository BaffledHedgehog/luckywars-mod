package me.skywars2.game;

import com.google.gson.Gson;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.SguiUtils;
import eu.pb4.sgui.api.gui.SimpleGui;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Native GUI/economy, causal cosmetic bodies and whole-coordinator checks. No client/audio claim. */
public final class CaseCosmeticChecks {
    private CaseCosmeticChecks() { }
    private static final Gson JSON=new Gson();
    private static Fixture running;
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("Cases/cosmetics: "+why);}
    private static Field field(Object object,String name){for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new IllegalStateException("Missing fixture field "+name);}
    private static Object get(Object object,String name){try{return field(object,name).get(object);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static void set(Object object,String name,Object value){try{field(object,name).set(object,value);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static Object invoke(Object target,String name,Class<?>[] types,Object... args){try{var method=target.getClass().getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(target,args);}catch(ReflectiveOperationException e){throw new IllegalStateException("Native fixture call "+name,e instanceof InvocationTargetException i?i.getCause():e);}}
    public static void runCases(GameController game){checkDropRequests();start(game,0);}
    public static void checkDropRequests(){
        var profile=new PlayerStore.Profile();
        require(LbcDropRequests.pending(profile).isEmpty(),"unearned manual requests excluded");
        profile.legacyUnlocks.add(LbcDropRequests.fulfilledKey(2));
        require(LbcDropRequests.pending(profile).isEmpty(),"completion alone never fabricates earned requests");
        for(int id=1;id<=3;id++){
            require(LbcSourceUnlocks.valid(LbcDropRequests.earnedKey(id))&&LbcSourceUnlocks.valid(LbcDropRequests.fulfilledKey(id)),"finite original request flags "+id);
            profile.legacyUnlocks.add(LbcDropRequests.earnedKey(id));
        }
        require(LbcDropRequests.pending(profile).equals(List.of(1,3)),"per-ID earned and unfulfilled manual requests");
        profile.legacyUnlocks.remove(LbcDropRequests.fulfilledKey(2));
        require(LbcDropRequests.pending(profile).equals(List.of(1,2,3)),"unmade preserves earned ownership and restores only its request");
        profile.dropRequestOverrides.add("2");var image=new MatchValueSnapshot(profile);
        profile.dropRequestOverrides.clear();profile.legacyUnlocks.add(LbcDropRequests.fulfilledKey(2));image.restore();
        require(profile.dropRequestOverrides.equals(Set.of("2"))&&LbcDropRequests.pending(profile).equals(List.of(1,2,3)),"canonical whole-profile replay includes explicit unmade authority and original flags");
        require(!LbcSourceUnlocks.valid("swrg:rewards/drop_4")&&!LbcSourceUnlocks.valid("swrg:rewards/drop_4_requested"),"unknown request flags rejected");
        UUID id=UUID.randomUUID();profile.username="KnownPlayer";var profiles=new LinkedHashMap<UUID,PlayerStore.Profile>();profiles.put(id,profile);
        require(LbcDropRequests.resolve(profiles,"knownplayer").equals(id),"offline canonical name resolution");
        for(String name:List.of("UnknownPlayer","@a","../players","")){
            boolean rejected=false;try{LbcDropRequests.resolve(profiles,name);}catch(IllegalArgumentException expected){rejected=true;}
            require(rejected&&profiles.size()==1,"unknown/unchecked names never create profiles");
        }
        var collision=new PlayerStore.Profile();collision.username="KNOWNPLAYER";profiles.put(UUID.randomUUID(),collision);
        boolean rejected=false;try{LbcDropRequests.resolve(profiles,"KnownPlayer");}catch(IllegalArgumentException expected){rejected=true;}
        require(rejected,"ambiguous stored names cannot administer another player's prize");
    }
    public static void runCosmetics(GameController game){start(game,1);}
    public static void runRewind(GameController game){start(game,2);}
    private static void start(GameController game,int kind){require(running==null,"exclusive native fixture");var f=new Fixture(game,kind);running=f;try{f.begin();}catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    public static void tick(GameController game){if(running==null)return;var f=running;try{require(f.game==game,"same controller");if(!f.step())return;running=null;f.close();SkyWars2.LOGGER.info("[check] PASS {} ({})",f.name(),f.kind==0?"actual keys/source27 cards/Slot2/one debit/payout/paid resume and native source ownership":f.kind==1?"actual cosmetic GUI gates, tracked cages/individual TAB and chat suffixes with shared teams preserved, causal death bodies and source postgame work":"nonempty native profiles/strip/private cosmetic work/RNG whole coordinator replay");}catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL "+f.name(),error);}}

    private static final class Fixture implements AutoCloseable {
        final GameController game;final int kind;final ServerPlayer owner;ServerPlayer other;
        final ServerLevel level;final RegenerationBeltChecks.Snapshot saved,otherSaved;
        final MatchCoreCheckpoint core;final MatchWorldCheckpoint world;final MatchWorldRandomCheckpoint random;
        final LbcCases cases;final LbcCosmetics cosmetics;final LbcCosmetics.Policy policy;
        final Set<UUID> original=new HashSet<>();final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();
        final Vec3 origin;final int started;final boolean frozen;final String epoch;
        final MatchRandomState ownerRandom,otherRandom;final int playerKills,playerKilledBy;
        RegenerationBeltChecks.ScheduledClient client,audience;boolean closed,stopped,restoring,offline,endingStarted;int stage,when,caseKind;
        LbcCases.Pending accepted,normalized;String pendingImage;int coinsBefore,memBefore,keyBefore;Object cacheImage;
        int projectedAwards;long expectedRandom;UUID dormant=GameChecks.offlineUuid("CaseCosmeticDormant");String dormantImage;
        Map<String,Object> jobImage;Object replayJob;List<LbcCases.Token> startingStrip;boolean checkedOffline,checkedPaid;
        final Set<String> unlockedDuring=new LinkedHashSet<>();
        Fixture(GameController game,int kind){
            this.game=game;this.kind=kind;owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));other=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));level=owner.level();
            require(game.playing()&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other)&&!game.vanity().busy()&&!game.vanity().hasSnapshot(),"two active actual participants and exclusive coordinator");
            require(!game.items().timeStop().stopped(level),"unpaused source fixture");cases=game.cases();cosmetics=game.items().cosmetics();
            require(!cases.hasPending(owner)&&!cases.hasPending(other)&&jobs().isEmpty(),"no unrelated accepted cases/cosmetics");
            require(kind!=1||game.items().stonekivi().queuedSnapshot().isEmpty(),"postgame fixture owns an initially empty shared terrain queue");epoch=game.items().epoch();
            frozen=game.server().tickRateManager().isFrozen();core=new MatchCoreCheckpoint(game);world=new MatchWorldCheckpoint(game);random=new MatchWorldRandomCheckpoint(game);
            ownerRandom=MatchRandomState.capture(owner.getRandom());otherRandom=MatchRandomState.capture(other.getRandom());playerKills=owner.getStats().getValue(net.minecraft.stats.Stats.ENTITY_KILLED.get(EntityTypes.PLAYER));playerKilledBy=other.getStats().getValue(net.minecraft.stats.Stats.ENTITY_KILLED_BY.get(EntityTypes.PLAYER));
            saved=new RegenerationBeltChecks.Snapshot(game,owner);otherSaved=new RegenerationBeltChecks.Snapshot(game,other);
            policy=(LbcCosmetics.Policy)get(cosmetics,"policy");level.getAllEntities().forEach(e->original.add(e.getUUID()));origin=new Vec3(owner.getX(),Math.min(240,level.getMaxY()-40),owner.getZ());started=game.server().getTickCount();
            client=new RegenerationBeltChecks.ScheduledClient(game,owner);audience=new RegenerationBeltChecks.ScheduledClient(game,other);
        }
        String name(){return kind==0?"cases-native":kind==1?"cosmetics-native":"cases-cosmetics-rewind";}
        PlayerStore.Profile profile(){return game.players().get(owner.getUUID());}
        PlayerStore.Profile otherProfile(){return game.players().get(other.getUUID());}
        LbcCases.Pending pending(){return JSON.fromJson(profile().casePending,LbcCases.Pending.class);}
        @SuppressWarnings("unchecked") List<Object> jobs(){return new ArrayList<>((Set<Object>)get(cosmetics,"jobs"));}
        @SuppressWarnings("unchecked") List<Packet<?>> packets(RegenerationBeltChecks.ScheduledClient source){return (List<Packet<?>>)get(get(source,"connection"),"packets");}
        void move(ServerPlayer p,Vec3 at){require(p.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true),"actual fixture teleport");p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);if(p==owner)client.acknowledge();else audience.acknowledge();}
        void next(int n){stage=n;when=game.server().getTickCount();}
        void pause(){game.items().timeStop().begin(level,10000);stopped=true;}
        void thaw(){game.items().timeStop().end(level);stopped=false;}
        void begin(){
            game.server().tickRateManager().setFrozen(false);game.config().lbcSkills=false;game.config().ranked=false;
            for(var p:List.of(owner,other)){game.lbc().clear(p);p.closeContainer();p.stopUsingItem();p.getInventory().clearContent();p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);p.getAbilities().invulnerable=false;p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(1000);p.setAbsorptionAmount(0);p.setInvulnerableTime(0);p.damageCooldownTime=0;clearOwnership(p);}
            move(owner,origin);move(other,origin.add(12,0,0));
            // Transparent native-write observer; admission, journaling and damage attribution remain canonical.
            set(cosmetics,"policy",Proxy.newProxyInstance(LbcCosmetics.Policy.class.getClassLoader(),new Class<?>[]{LbcCosmetics.Policy.class},(proxy,method,args)->{
                if(method.getName().equals("write")){var l=(ServerLevel)args[1];var at=(BlockPos)args[2];require(l==level,"owned scene world");remember(at);}
                else if(method.getName().equals("cageWrite"))remember((BlockPos)args[1]);
                try{return method.invoke(policy,args);}catch(InvocationTargetException e){throw e.getCause();}
            }));
            if(kind==0){keyShop();ordinaryMapping();ioFailure();beginCase(0);}else if(kind==1){cosmeticMenus();cages();suffixes();sounds();startKills();}else beginReplay();
        }
        void clearOwnership(ServerPlayer p){var row=game.players().get(p.getUUID());row.legacyUnlocks.clear();row.lbcUnlocks.clear();row.casePending="";row.killSound=row.cage=row.suffix=row.winEffect=row.killEffect=0;p.entityTags().removeIf(s->s.startsWith("swrg.kit")||s.startsWith("swrg.skill")||s.startsWith("swrg.sound")||s.startsWith("swrg.bars"));for(var a:game.server().getAdvancements().getAllAdvancements())if(a.id().toString().startsWith("swrg:rewards/")||Set.of("lbc:true_advancements/event/pumpkin_sword","lbc:true_advancements/event/soulcorrupt_wand_upgraded","lbc:true_advancements/event/soulhunter_mace").contains(a.id().toString()))for(var criterion:a.value().criteria().keySet())p.getAdvancements().revoke(a,criterion);}
        void grant(ServerPlayer p,String key){var row=game.players().get(p.getUUID());if(LbcSourceUnlocks.valid(key)){row.legacyUnlocks.add(key);LbcSourceUnlocks.publish(p,row);}else{row.lbcUnlocks.add(key);game.items().mana().award(p,key);}require(LbcSourceUnlocks.has(p,row,key),"canonical source ownership "+key);}
        SimpleGui menu(GameMenus.Page page){require(GameController.operator(owner),"actual operator menu context");game.menus().openDebug(owner);owner.entityTags().remove("skywars2.debug_perks");require(game.canUseMenus(owner)&&!game.debugPerks(owner),"actual open GUI access without perk bypass");game.menus().open(owner,page);owner.entityTags().remove("skywars2.debug_perks");require(SguiUtils.getCurrentGui(owner) instanceof SimpleGui,"real source SGui "+page);return (SimpleGui)SguiUtils.getCurrentGui(owner);}
        SimpleGui gui(){return (SimpleGui)Objects.requireNonNull(SguiUtils.getCurrentGui(owner));}
        void click(int slot){require(gui().getGuiElement(slot)!=null,"source GUI slot "+slot);gui().click(slot,ClickType.MOUSE_LEFT,ContainerInput.PICKUP);}
        Object page(){return get(gui(),"page");}
        void keyShop(){
            profile().coins=10000;profile().memcoins=10000;Arrays.fill(profile().keys,0);
            int[][] prices={{100,400,700},{150,600,1050},{150,600,1050},{200,800,1400},{100,400,750}};
            for(int k=0;k<5;k++)for(int tier=0;tier<3;tier++){int coins=profile().coins,mem=profile().memcoins,count=profile().keys[k];menu(GameMenus.Page.KEY_SHOP);click(k*3+tier);require(profile().keys[k]==count+(tier==0?1:tier==1?5:10),"actual key purchase count "+k+"/"+tier);require(profile().coins==coins-(k==4?0:prices[k][tier])&&profile().memcoins==mem-(k==4?prices[k][tier]:0),"actual key purchase currency "+k+"/"+tier);}
            menu(GameMenus.Page.KEY_SHOP);profile().coins=0;int keys=profile().keys[0];click(0);require(profile().keys[0]==keys&&profile().coins==0,"insufficient shop cannot charge/grant");profile().coins=1000;profile().memcoins=1000;owner.closeContainer();
        }
        void ordinaryMapping(){
            boolean kits=game.config().vanillaKits;
            try{
                game.config().vanillaKits=true;
                grant(owner,"swrg.kit19");menu(GameMenus.Page.VANILLA_KITS);click(19);
                require(profile().kit==20&&!game.debugPerks(owner),"source reward tag19 selects ordinary kit20 without perk bypass");owner.closeContainer();
                // The ordinary spawn owner supplies equipment; selecting a personal menu choice does not.
                game.events().giveKit(owner);
                require(owner.getInventory().contains(s->s.is(Items.GOLDEN_SWORD)),"actual ordinary kit20 giveKit boundary supplies golden equipment");owner.getInventory().clearContent();
                grant(owner,"swrg.kit20");menu(GameMenus.Page.VANILLA_KITS);click(20);
                require(profile().kit==21&&!game.debugPerks(owner),"source reward tag20 selects ordinary kit21 without perk bypass");owner.closeContainer();
                game.events().giveKit(owner);
                require(owner.getInventory().contains(s->s.is(Items.OAK_SLAB)),"actual ordinary kit21 giveKit boundary supplies source slab");
            }finally{game.config().vanillaKits=kits;owner.getInventory().clearContent();owner.closeContainer();}
            clearOwnership(owner);
        }
        void ioFailure(){
            menu(GameMenus.Page.CASES);var file=Path.of("config","luckywars","players.json").toAbsolutePath().normalize();var temporary=file.resolveSibling("players.json.tmp");
            require(!Files.exists(temporary),"checked IO fixture requires absent known atomic temp path");int keys=profile().keys[0];String state=profile().casePending;
            try{byte[] before=Files.readAllBytes(file);Files.createDirectory(temporary);require(!cases.open(owner,0),"actual checked write failure rejects acceptance");require(profile().keys[0]==keys&&profile().casePending.equals(state)&&Arrays.equals(before,Files.readAllBytes(file)),"checked failure preserves wallet/pending and exact canonical file");}
            catch(java.io.IOException e){throw new IllegalStateException("Scoped checked-write failure",e);}finally{try{Files.deleteIfExists(temporary);}catch(java.io.IOException e){throw new IllegalStateException("Empty fixture temp directory cleanup",e);}}
            owner.closeContainer();
        }
        void ownAll(int k){if(k==1)for(int i=1;i<=20;i++)grant(owner,"swrg.kit"+i);if(k==2)for(int i=1;i<=11;i++)grant(owner,"swrg.skill"+i);if(k==3){for(int i=1;i<=20;i++)grant(owner,"swrg.bars"+i);for(int i=1;i<=13;i++)grant(owner,"swrg.sound"+i);}if(k==4){for(int i=1;i<=26;i++)grant(owner,"swrg:rewards/suffix_"+i);for(int i=1;i<=3;i++)grant(owner,"swrg:rewards/drop_"+i);}}
        void beginCase(int k){
            caseKind=k;clearOwnership(owner);if(k>0)ownAll(k);profile().keys[k]=2;coinsBefore=profile().coins;memBefore=profile().memcoins;keyBefore=2;menu(GameMenus.Page.CASES);var stale=gui();click(k);
            require(profile().keys[k]==1&&cases.hasPending(owner),"actual case GUI accepts one matching debit "+k);accepted=pending();require(accepted.kind()==k&&accepted.phase()==3&&accepted.timer()==0&&accepted.strip().size()==27&&!accepted.paid(),"source accepted phase3 finite27 strip");
            require(accepted.version()==2&&accepted.payout()!=null,"accepted result and key debit share the durable transaction");
            if(k==0)for(var token:accepted.strip()){String unlock=LbcCases.unlock(token);if(!unlock.isEmpty())grant(owner,unlock);}
            if(k>0)require(accepted.strip().stream().allMatch(t->t.family().equals("rewardcoin")),"all-owned native same-rarity compensation, no unowned redraw "+k);
            startingStrip=accepted.strip();stale.click(k,ClickType.MOUSE_LEFT,ContainerInput.PICKUP);gui().click(k,ClickType.MOUSE_DOUBLE_CLICK,ContainerInput.PICKUP_ALL);require(profile().keys[k]==1&&pending().equals(accepted),"stale/double click cannot debit or reroll accepted session");
            var state=MatchRandomState.capture(level.getRandom());long expected=level.getRandom().nextLong();state.restore(level.getRandom());for(int i=0;i<8;i++){var view=cases.renderView(owner);require(view.strip().size()==9&&view.winnerSlot()==2&&view.page()==5100+100*k,"native source nine-card frame/Slot2 mapping");}require(level.getRandom().nextLong()==expected,"render consumes no gameplay RNG");state.restore(level.getRandom());
            for(int i=0;i<9;i++)require(ItemStack.matches(gui().getGuiElement(9+i).getItemStack(),cases.renderView(owner).strip().get(i)),"actual GUI copies native visible source slot "+i);
            checkedOffline=checkedPaid=false;normalized=null;owner.closeContainer();next(0);
        }
        boolean caseStep(){
            if(stage==1){require(game.server().getPlayerList().getPlayer(owner.getUUID())==null&&profile().casePending.equals(pendingImage),"offline native resolver leaves paid input/strip/phase untouched");if(game.server().getTickCount()-when<2)return false;game.server().getPlayerList().getPlayersByUUID().put(owner.getUUID(),owner);offline=false;cases.onJoin(owner);require(pending().equals(accepted),"rejoin resumes exact accepted27 cards without reroll/debit");next(0);return false;}
            if(!cases.hasPending(owner)){
                require(checkedPaid,"award phase was observed before cleanup");require(profile().keys[caseKind]==keyBefore-1,"exact one key through complete animation");
                if(caseKind<4){beginCase(caseKind+1);return false;}manualClaim();next(2);return false;
            }
            var current=pending();
            require(current.payout().equals(accepted.payout()),"future ownership changes cannot replace the accepted outcome");
            if(!checkedOffline&&caseKind==0&&current.phase()>=7&&current.phase()<15){
                accepted=current;pendingImage=profile().casePending;checkedOffline=true;cases.onLeave(owner);require(game.server().getPlayerList().getPlayersByUUID().remove(owner.getUUID(),owner),"scoped actual native online resolver removal");offline=true;next(1);return false;
            }
            if(current.payout()!=null&&current.phase()>=21&&normalized==null){
                normalized=current;require(current.strip().get(2).equals(startingStrip.get(20)),"nineteen source shifts select initial Slot21 at backing Slot2");
                var token=current.strip().get(2);if(caseKind>0){int expected=new int[]{0,20,50,100,1000}[token.value()];require(current.payout().coins()==(caseKind==4?0:expected)&&current.payout().memcoins()==(caseKind==4?expected:0),"same-rarity compensation exact canonical currency");}
            }
            if(current.paid()&&!checkedPaid){
                require(current.phase()==25&&normalized!=null,"fixed normalized payout precedes source award25");checkedPaid=true;var payout=current.payout();
                require(profile().coins==coinsBefore+payout.coins()&&profile().memcoins==memBefore+payout.memcoins(),"single exact native wallet payout");
                if(!payout.unlock().isEmpty())require(profile().legacyUnlocks.contains(payout.unlock())&&LbcSourceUnlocks.has(owner,profile(),payout.unlock()),"durable award projects actual source ownership");
                String committed=profile().casePending;cases.onLeave(owner);cases.onJoin(owner);require(profile().casePending.equals(committed)&&profile().coins==coinsBefore+payout.coins()&&profile().memcoins==memBefore+payout.memcoins(),"paid rejoin retains receipt and never pays twice");
            }
            if(checkedPaid)require(profile().coins==coinsBefore+current.payout().coins()&&profile().memcoins==memBefore+current.payout().memcoins(),"paid visual phases never repeat payout");
            return false;
        }
        @SuppressWarnings("unchecked") List<LbcCases.Token> nativeTokens(String table,long seed){return (List<LbcCases.Token>)invoke(cases,"materialize",new Class<?>[]{ServerPlayer.class,String.class,long.class},owner,table,seed);}
        void manualClaim(){
            clearOwnership(owner);LbcCases.Token token=null;for(long seed=1;seed<=64&&token==null;seed++){var candidate=nativeTokens("swrg:gui/roll/case4/legendary",seed).getFirst();if(candidate.family().equals("drop"))token=candidate;}
            require(token!=null,"actual native legendary source drop token");var payout=new LbcCases.Payout("swrg:rewards/drop_"+token.value(),0,0,"new_drop_request",true);
            // Supporting recovered-transaction fixture: actual native seeded card, with normalized durable pre-award phase.
            profile().casePending=JSON.toJson(new LbcCases.Pending(1,4,Collections.nCopies(27,token),24,0,payout,false));projectedAwards=packets(client).size();coinsBefore=profile().coins;memBefore=profile().memcoins;cases.onJoin(owner);
        }
        boolean manualStep(){
            if(!cases.hasPending(owner))return true;var state=pending();if(state.paid()){
                require(profile().legacyUnlocks.contains(state.payout().unlock())&&LbcSourceUnlocks.has(owner,profile(),state.payout().unlock()),"manual entitlement actual native advancement projection");
                require(profile().coins==coinsBefore&&profile().memcoins==memBefore&&owner.getInventory().isEmpty(),"manual claim grants entitlement without automatic prize or wallet invention");
                var messages=packets(client).subList(projectedAwards,packets(client).size()).stream().filter(ClientboundSystemChatPacket.class::isInstance).map(ClientboundSystemChatPacket.class::cast).map(p->p.content().getString()).toList();
                require(messages.stream().anyMatch(t->t.contains("new_drop_request"))&&messages.stream().anyMatch(t->t.contains("discord.gg/eTr7Wybutc"))&&messages.stream().anyMatch(t->t.contains("BaffledHedgehog")),"source manual claim hints emitted once, with no external tool action");
            }return false;
        }
        void cosmeticMenus(){
            long coins=profile().coins,mem=profile().memcoins;int[] keys=profile().keys.clone();
            var rows=List.of(new Object[]{GameMenus.Page.COSMETIC_SOUNDS,0,"swrg.sound1",LbcCosmetics.Kind.SOUND,1},new Object[]{GameMenus.Page.COSMETIC_CAGES,21,"swrg.bars20",LbcCosmetics.Kind.CAGE,20},new Object[]{GameMenus.Page.COSMETIC_SUFFIXES,0,"swrg:rewards/suffix_1",LbcCosmetics.Kind.SUFFIX,1},new Object[]{GameMenus.Page.COSMETIC_WINS,0,"lbc:true_advancements/event/pumpkin_sword",LbcCosmetics.Kind.WIN,1},new Object[]{GameMenus.Page.COSMETIC_KILLS,0,"lbc:true_advancements/event/soulcorrupt_wand_upgraded",LbcCosmetics.Kind.KILL,1});
            for(var row:rows){var page=(GameMenus.Page)row[0];int slot=(int)row[1];var type=(LbcCosmetics.Kind)row[3];int id=(int)row[4];game.config().ranked=true;var locked=menu(page).getGuiElement(slot).getItemStack().copy();click(slot);require(!cosmetics.unlocked(owner,type,id)&&selection(type)==0,"ranked and actual debug GUI never bypass cosmetic permanent gate "+type);grant(owner,(String)row[2]);game.config().ranked=false;menu(page);require(!ItemStack.matches(locked,gui().getGuiElement(slot).getItemStack()),"actual authored locked source icon changes after ownership "+type);click(slot);require(selection(type)==id,"actual source cosmetic callback selects "+type);var selected=gui().getGuiElement(slot).getItemStack().copy();menu(page);require(ItemStack.matches(selected,gui().getGuiElement(slot).getItemStack()),"selected source overlay stable "+type);if(type!=LbcCosmetics.Kind.CAGE){click(type==LbcCosmetics.Kind.SOUND?26:25);require(selection(type)==0,"source reset callback "+type);}click(18);require(page()==GameMenus.Page.COSMETICS,"actual cosmetic back navigation");}
            grant(owner,"swrg:rewards/suffix_26");grant(owner,"swrg:rewards/turnir/trophy3");menu(GameMenus.Page.COSMETIC_SUFFIXES);click(26);require(page()==GameMenus.Page.COSMETIC_SUFFIXES_NEXT,"source suffix next page");click(1);require(profile().suffix==26,"source second-page suffix26");click(4);require(profile().suffix==29,"actual tournament trophy3 suffix29");click(18);require(page()==GameMenus.Page.COSMETIC_SUFFIXES,"source previous suffix page");
            require(profile().coins==coins&&profile().memcoins==mem&&Arrays.equals(profile().keys,keys),"all equip/reset/navigation selections are free");owner.closeContainer();
        }
        int selection(LbcCosmetics.Kind kind){return switch(kind){case SOUND->profile().killSound;case CAGE->profile().cage;case SUFFIX->profile().suffix;case WIN->profile().winEffect;case KILL->profile().killEffect;};}
        void remember(BlockPos at){terrain.putIfAbsent(at.immutable(),game.maps().snapshot(level,at));}
        void block(BlockPos at,BlockState state){remember(at);if(level.getBlockEntity(at) instanceof net.minecraft.world.Container c)c.clearContent();level.setBlock(at,state,818);}
        void cages(){
            require(((List<?>)get(game.maps(),"cageBlocks")).isEmpty(),"exclusive canonical cage restoration list");var feet=BlockPos.containing(origin.add(5,0,0));
            for(int id=1;id<=20;id++){
                grant(owner,"swrg.bars"+id);profile().cage=id;for(var p:BlockPos.betweenClosed(feet.offset(-1,-2,-1),feet.offset(1,4,1)))block(p,Blocks.GLASS.defaultBlockState());
                var protectedCell=feet.offset(0,0,1);block(protectedCell,Blocks.STONE.defaultBlockState());var corner=feet.offset(1,-2,1);block(corner,Blocks.STONE.defaultBlockState());
                cosmetics.cage(owner,level,feet,1);
                require(level.getBlockState(protectedCell).is(Blocks.STONE),"cages replace existing glass only "+id);
                if(id<=16)require(level.getBlockState(feet).is(Blocks.STAINED_GLASS.pick(DyeColor.values()[id-1])),"all sixteen authored stained cage colors "+id);
                else if(id==17)require(level.getBlockState(feet).is(Blocks.BARRIER),"barrier cage");else if(id==18)require(level.getBlockState(feet).is(Blocks.ICE),"ice cage");
                else if(id==19){int chain=0;for(var p:BlockPos.betweenClosed(feet.offset(-1,-2,-1),feet.offset(1,3,1)))if(level.getBlockState(p).is(Blocks.IRON_CHAIN))chain++;require(chain==24&&level.getBlockState(corner).is(Blocks.IRON_CHAIN)&&level.getBlockState(feet).is(Blocks.BARRIER),"canonical cage tracks all24 unconditional chain corners and barrier shell");}
                else{var colors=new DyeColor[]{DyeColor.RED,DyeColor.YELLOW,DyeColor.GREEN,DyeColor.BLUE,DyeColor.PURPLE};for(int y=-2;y<=2;y++)require(level.getBlockState(feet.offset(-1,y,0)).is(Blocks.STAINED_GLASS.pick(colors[y+2])),"five rainbow source layers "+y);require(level.getBlockState(feet.above(3)).is(Blocks.GLASS),"source upper rainbow glass retained");}
                var tracked=new ArrayList<>((List<?>)get(game.maps(),"cageBlocks"));require(!tracked.isEmpty(),"every cage variant enrolls canonical removal owner");game.maps().cages(false);for(var p:tracked)require(level.getBlockState((BlockPos)p).isAir(),"canonical opening removes converted cage material "+id);
            }
            profile().cage=20;block(feet,Blocks.GLASS.defaultBlockState());cosmetics.cage(owner,level,feet,3);require(level.getBlockState(feet).is(Blocks.GLASS)&&((List<?>)get(game.maps(),"cageBlocks")).isEmpty(),"BedWars skips ordinary cosmetic cages");
        }
        void suffixes(){
            var team=owner.getTeam();require(team!=null,"actual assigned scoreboard team");var previousOther=other.getTeam();var previousSuffix=team.getPlayerSuffix().copy();String name=team.getName();var color=team.getColor();
            game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),team);grant(owner,"swrg:rewards/suffix_17");grant(other,"swrg:rewards/turnir/trophy3");profile().suffix=17;otherProfile().suffix=29;team.setPlayerSuffix(LbcCosmetics.suffix(29));
            cosmetics.startSuffixes(List.of(owner,other),0);require(owner.getTeam()==team&&other.getTeam()==team&&team.getName().equals(name)&&team.getColor().equals(color),"suffix retains actual shared team identity, membership and color");
            require(team.getPlayerSuffix().getString().isEmpty(),"old shared source suffix cleared without replacing gameplay teams");
            require(owner.getDisplayName().getString().endsWith(" \uE186")&&other.getDisplayName().getString().endsWith(" \uE209"),"individual server chat names retain different suffixes in the same team");
            require(owner.getTabListDisplayName().getString().endsWith(" \uE186")&&other.getTabListDisplayName().getString().endsWith(" \uE209"),"individual native TAB names retain canonical selections");
            var published=packets(client).stream().filter(ClientboundPlayerInfoUpdatePacket.class::isInstance).map(ClientboundPlayerInfoUpdatePacket.class::cast).filter(p->p.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME)).anyMatch(p->p.entries().stream().anyMatch(e->e.profileId().equals(owner.getUUID())&&e.displayName()!=null&&e.displayName().getString().endsWith(" \uE186"))&&p.entries().stream().anyMatch(e->e.profileId().equals(other.getUUID())&&e.displayName()!=null&&e.displayName().getString().endsWith(" \uE209")));
            require(published,"native per-player TAB update packet publishes both individual names");
            String[] expected={"★","۞","♛","♔","♠","♣","♥","♦","♪","☉","20㎝","☠","卍","✟","❤","🤩","\uE186","\uE187","\uE188","\uE189","\uE190","\uE191","\uE192","\uE193","\uE194","\uE195","\uE207","\uE208","\uE209"};
            for(int id=1;id<=29;id++)require(LbcCosmetics.suffix(id).getString().equals(" "+expected[id-1]),"all29 exact authored suffix glyphs "+id);
            otherProfile().suffix=0;cosmetics.startSuffixes(List.of(other),1);require(team.getPlayerSuffix().getString().isEmpty()&&!other.getDisplayName().getString().endsWith(" \uE209")&&owner.getDisplayName().getString().endsWith(" \uE186"),"zero resets only that player's personal suffix");
            team.setPlayerSuffix(Component.literal(" [team]"));cosmetics.startSuffixes(List.of(owner,other),2);require(team.getPlayerSuffix().getString().equals(" [team]")&&owner.getDisplayName().getString().endsWith(" [team] \uE186"),"unrelated native team formatting survives individual cosmetics");
            // Restore teams before testing hostile native death admission.
            if(previousOther==null)game.server().getScoreboard().removePlayerFromTeam(other.getScoreboardName(),team);else game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),previousOther);team.setPlayerSuffix(previousSuffix);
        }
        void sounds(){
            String[] sounds={"piglin.hurt","zombie.hurt","witch.celebrate","bee.death","horse.death","chicken.death","pig.death","wolf.growl","villager.death","vindicator.death","lightning_bolt.thunder","ender_dragon.ambient","ghast.hurt"};
            for(int id=1;id<=13;id++){grant(other,"swrg.sound"+id);otherProfile().killSound=id;int a=packets(client).size(),b=packets(audience).size();cosmetics.deathSound(other);checkSound(client,a,"minecraft:entity."+sounds[id-1],id==1?.8f:id==3?.9f:1);checkSound(audience,b,"minecraft:entity."+sounds[id-1],id==1?.8f:id==3?.9f:1);}
        }
        void checkSound(RegenerationBeltChecks.ScheduledClient listener,int from,String sound,float pitch){
            var actual=packets(listener).subList(from,packets(listener).size()).stream().filter(ClientboundSoundPacket.class::isInstance).map(ClientboundSoundPacket.class::cast).filter(p->p.getSound().value().location().toString().equals(sound)).toList();
            require(actual.size()==1,"one victim-selected native sound publication "+sound);var packet=actual.getFirst();var at=listener.player.position();require(packet.getSource()==SoundSource.MASTER&&packet.getVolume()==1&&packet.getPitch()==pitch&&Math.abs(packet.getX()-at.x)<.126&&Math.abs(packet.getY()-at.y)<.126&&Math.abs(packet.getZ()-at.z)<.126,"source death sound at listener's own position/category/volume/pitch");
        }
        String effect(Object job){return get(job,"effect").toString();}
        List<Object> effectJobs(String name){return jobs().stream().filter(j->effect(j).equals(name)).toList();}
        Cow cow(){var cow=new Cow(EntityTypes.COW,level);cow.setPos(origin.add(5,0,0));cow.setNoGravity(true);cow.setNoAi(true);require(level.addFreshEntity(cow),"actual native death victim");return cow;}
        void kill(Cow cow){require(cow.hurtServer(level,cow.damageSources().playerAttack(owner),10000)&&cow.isDeadOrDying()&&cow.getKillCredit()==owner,"actual credited native cow death");}
        Set<UUID> bodies(){var ids=new LinkedHashSet<UUID>();for(var e:level.getAllEntities())if(e instanceof ItemEntity||e instanceof ExperienceOrb)ids.add(e.getUUID());return ids;}
        void startKills(){
            profile().killEffect=1;grant(owner,"lbc:true_advancements/event/soulcorrupt_wand_upgraded");var unrelated=new ItemEntity(level,origin.x+1,origin.y,origin.z,new ItemStack(Items.EMERALD,7));unrelated.setNoGravity(true);unrelated.setPickUpDelay(32767);require(level.addFreshEntity(unrelated),"unrelated fresh native loot positive control");
            var before=bodies();kill(cow());var originals=bodies();originals.removeAll(before);require(!originals.isEmpty(),"actual native victim loot published");
            var scoped=effectJobs("DROPS");require(scoped.size()==1,"one cosmetic consumer from actual native death scope with no reward skill");
            @SuppressWarnings("unchecked") var accepted=(List<LbcCosmetics.DropBody>)get(scoped.getFirst(),"drops");require(!accepted.isEmpty()&&accepted.stream().allMatch(d->originals.contains(d.identity())&&d.sourceAgeBucket()==0)&&accepted.stream().noneMatch(d->d.identity().equals(unrelated.getUUID())),"immutable causal original item/XP identities; unrelated fresh body excluded");next(30);
        }
        ServerPlayer retired;int soundStart;Vec3 hammerStart;Object hammer;
        void playerDeath(int effect){
            require(game.lives(other.getUUID())>1,"actual life loss retains a playable participant");profile().killEffect=effect;grant(owner,effect==2?"lbc:true_advancements/event/soulhunter_mace":"swrg:rewards/turnir/killer");
            grant(other,"swrg.sound3");otherProfile().killSound=3;profile().killSound=1;grant(owner,"swrg.sound1");other.getInventory().clearContent();other.setExperienceLevels(0);other.setExperiencePoints(0);other.totalExperience=0;
            if(effect==3)other.getInventory().setItem(9,new ItemStack(Items.DIAMOND,3));move(other,origin.add(0,0,8));other.setHealth(10);other.setInvulnerableTime(0);other.damageCooldownTime=0;soundStart=packets(client).size();retired=other;
            require(other.hurtServer(level,other.damageSources().playerAttack(owner),10000)&&other.isDeadOrDying()&&game.awaitingRespawn(other.getUUID()),"actual native player life loss with selected cosmetic");checkSound(client,soundStart,"minecraft:entity.witch.celebrate",.9f);
            if(effect==2){var selected=effectJobs("HAMMER");require(selected.size()==1,"empty-loot actual player death still enrolls one hammer");hammer=selected.getFirst();hammerStart=(Vec3)get(hammer,"at");require(hammerStart.equals(retired.position().add(0,11,0)),"hammer source11 height over actual victim");}
            else require(!effectJobs("DROPS").isEmpty()&&packets(client).subList(soundStart,packets(client).size()).stream().filter(ClientboundSystemChatPacket.class::isInstance).map(ClientboundSystemChatPacket.class::cast).anyMatch(p->p.content().getString().contains("bloodshedkilled")),"blood actual player message and causal body producer");
        }
        void respawn(){
            audience.close();other=game.server().getPlayerList().respawn(retired,false,Entity.RemovalReason.KILLED);require(other!=retired&&other.isAlive(),"genuine native replacement player");other.connection.resetPosition();var ack=(MatchPlayerCheckpoint.SimulatedAcknowledgement)other.connection;ack.skywars2$beginRespawn();ack.skywars2$acknowledgeRestore();audience=new RegenerationBeltChecks.ScheduledClient(game,other);set(otherSaved,"player",other);set(otherSaved.state,"player",other);move(other,origin.add(12,0,0));
        }
        Map<String,Object> image(Object job){var result=new LinkedHashMap<String,Object>();for(var key:List.of("at","direction","age","selected","cursor","stage","rng","drops","closed"))result.put(key,get(job,key));return result;}
        void beginReplay(){
            require(game.server().getPlayerList().getPlayer(dormant)==null,"offline profile has no fabricated native player");profile().coins=1234;profile().memcoins=4321;profile().keys[0]=2;grant(owner,"swrg.kit19");grant(owner,"swrg:rewards/suffix_26");
            menu(GameMenus.Page.CASES);click(0);owner.closeContainer();require(profile().keys[0]==1&&pending().strip().size()==27,"actual accepted transaction before full capture");
            pendingImage=profile().casePending;coinsBefore=profile().coins;memBefore=profile().memcoins;cacheImage=((Map<?,?>)get(cases,"decoded")).get(owner.getUUID());
            var offlineProfile=game.players().get(dormant);offlineProfile.coins=917;offlineProfile.memcoins=719;offlineProfile.keys[4]=3;offlineProfile.casePending=pendingImage;dormantImage=offlineProfile.casePending;
            profile().killEffect=3;grant(owner,"swrg:rewards/turnir/killer");kill(cow());require(effectJobs("DROPS").size()==1,"real original causal death job before full capture");replayJob=effectJobs("DROPS").getFirst();jobImage=image(replayJob);
            var rng=MatchRandomState.capture(level.getRandom());expectedRandom=level.getRandom().nextLong();rng.restore(level.getRandom());pause();require(game.vanity().requestCapture(owner,false),"actual whole coordinator captures accepted transaction/private causal work/offline profile");next(50);
        }
        boolean rewindStep(int elapsed){
            if(stage==50){require(profile().casePending.equals(pendingImage)&&image(replayJob).equals(jobImage),"TimeStop holds accepted economy and private causal graph during capture");if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;
                // Probe the actual accepted snapshot boundary; native world RNG may advance while capture waits.
                var rngImage=get(get(game.vanity(),"image"),"random");var current=MatchRandomState.capture(level.getRandom());boolean found=false;
                for(var entry:(List<?>)get(rngImage,"worlds"))if(get(entry,"level")==level){((MatchRandomState)get(entry,"state")).restore(level.getRandom());expectedRandom=level.getRandom().nextLong();found=true;break;}current.restore(level.getRandom());require(found,"actual world RNG saved in whole snapshot");
                thaw();next(51);return false;}
            if(stage==51){
                if(!cases.hasPending(owner)||!pending().paid()){require(elapsed<100,"actual future transaction reaches paid source25");return false;}
                require(!jobs().contains(replayJob),"future causal source job actually consumed and retired");pause();profile().coins=7777;profile().memcoins=8888;game.players().get(dormant).coins=999;game.players().get(dormant).casePending="";
                profile().legacyUnlocks.remove("swrg.kit19");profile().legacyUnlocks.remove("swrg:rewards/suffix_26");owner.entityTags().remove("swrg.kit19");var a=game.server().getAdvancements().get(Identifier.parse("swrg:rewards/suffix_26"));for(var c:a.value().criteria().keySet())owner.getAdvancements().revoke(a,c);grant(owner,"swrg.skill11");
                require(game.vanity().requestRestoreForCheck(),"actual whole coordinator rewinds paid future to accepted unpaid state");restoring=true;next(52);return false;
            }
            if(stage==52){
                if(game.vanity().busy())return false;
                require(profile().casePending.equals(pendingImage)&&profile().coins==coinsBefore&&profile().memcoins==memBefore&&profile().keys[0]==1,"whole profile owner restores accepted strip/phase/one debit/wallet");
                require(game.players().get(dormant).coins==917&&game.players().get(dormant).memcoins==719&&game.players().get(dormant).keys[4]==3&&game.players().get(dormant).casePending.equals(dormantImage),"whole profile owner restores actual offline scalar session/economy");
                require(owner.entityTags().contains("swrg.kit19")&&LbcSourceUnlocks.has(owner,profile(),"swrg:rewards/suffix_26")&&!owner.entityTags().contains("swrg.skill11"),"native tags/advancement projection restore with profile ownership");
                require(jobs().contains(replayJob)&&image(replayJob).equals(jobImage),"typed private causal cursor/RNG/position/drop bodies restore through whole coordinator");
                require(((Map<?,?>)get(cases,"decoded")).get(owner.getUUID())!=cacheImage&&cases.renderView(owner).phase()==3,"restored scalar invalidates derived case cache and rehydrates same native cards");
                require(level.getRandom().nextLong()==expectedRandom,"actual world RNG next draw restored");require(game.items().timeStop().stopped(level),"source pause boundary restored");game.vanity().stop();thaw();next(53);return false;
            }
            if(stage==53){if(!cases.hasPending(owner)||!pending().paid()){require(elapsed<100,"replayed accepted transaction reaches one source payout");return false;}normalized=pending();require(profile().coins==coinsBefore+normalized.payout().coins()&&profile().memcoins==memBefore+normalized.payout().memcoins(),"replayed accepted economy pays once");next(54);return false;}
            require(profile().coins==coinsBefore+normalized.payout().coins()&&profile().memcoins==memBefore+normalized.payout().memcoins(),"paid replay cannot duplicate wallet award");return elapsed>=8;
        }
        Object endingSword;BlockPos blastCenter;int endingAge;boolean sawEndingIngot;Object midasWave;Set<BlockPos> blastBlocks=new HashSet<>();
        void beginEnding(){
            cosmetics.stop();profile().killEffect=0;profile().winEffect=2;grant(owner,"swrg:rewards/turnir/trophy1");game.config().endingSeconds=60;level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES,true,game.server());
            @SuppressWarnings("unchecked") var members=(Map<UUID,Object>)get(game,"participants");for(var e:members.entrySet())if(!e.getKey().equals(owner.getUUID()))set(e.getValue(),"lives",0);
            endingStarted=true;invoke(game,"finish",new Class<?>[0]);require(game.stage()==GameController.Stage.ENDING&&!game.playing()&&!game.items().currentEpoch(epoch),"actual canonical finish enters ENDING and retires its gameplay epoch (no direct phase assignment)");
            require(effectJobs("WINNER").size()==1,"actual canonical winner transition enrolls one golden source producer");move(owner,origin);owner.setYRot(0);owner.setXRot(0);endingAge=(int)get(effectJobs("WINNER").getFirst(),"age");next(40);
        }
        boolean cosmeticStep(int elapsed){
            if(stage==30){if(effectJobs("SOUL").isEmpty()){require(elapsed<8,"native causal souls published on next source tick");return false;}require(effectJobs("SOUL").size()<=4,"four original item/XP age buckets bound native souls");cosmetics.stop();playerDeath(2);next(31);return false;}
            if(stage==31){respawn();next(32);return false;}
            if(stage==32){
                if(!(boolean)get(hammer,"impact")){require(elapsed<10,"actual scheduled hammer reaches source Lifetime6 impact");return false;}
                require(((Vec3)get(hammer,"at")).equals(hammerStart.add(0,-10,0)),"five source2-block hammer falls then one impact");var display=(Entity)get(hammer,"display");require(display!=null&&LbcCosmetics.managed(display)&&cosmetics.owner(display)==owner,"real native hammer retains causal killer");
                cosmetics.stop();playerDeath(3);next(33);return false;
            }
            if(stage==33){respawn();next(34);return false;}
            if(stage==34){if(effectJobs("BLOOD").isEmpty()){require(elapsed<8,"actual source blood body job follows native player publications");return false;}require(effectJobs("BLOOD").size()<=4,"causal blood bucket bound");cosmetics.stop();beginEnding();return false;}
            return endingStep(elapsed);
        }
        boolean endingStep(int elapsed){
            require(game.stage()==GameController.Stage.ENDING,"actual canonical ENDING remains live during source celebration work");
            if(stage==40){
                for(var e:level.getAllEntities())if(!original.contains(e.getUUID())&&e instanceof ItemEntity item&&item.getItem().is(Items.GOLD_INGOT)&&LbcCosmetics.managed(e)){sawEndingIngot=true;require(cosmetics.owner(e)==owner,"actual postgame ingot captured winner attribution");}
                var swords=effectJobs("SWORD");if(!sawEndingIngot||swords.stream().noneMatch(j->get(j,"display")!=null)){require(elapsed<12,"real ENDING branch pumps shared golden producer and native sword display");return false;}
                require((int)get(effectJobs("WINNER").getFirst(),"age")>endingAge,"actual source postgame logical ticker advances");endingSword=swords.stream().filter(j->get(j,"display")!=null).findFirst().orElseThrow();
                blastCenter=BlockPos.containing(origin.add(0,0,24));for(var p:BlockPos.betweenClosed(blastCenter.offset(-5,-5,-5),blastCenter.offset(5,5,5)))block(p,Blocks.AIR.defaultBlockState());
                for(var p:BlockPos.betweenClosed(blastCenter.offset(-1,-1,-1),blastCenter.offset(1,1,1))){block(p,Blocks.PUMPKIN.defaultBlockState());blastBlocks.add(p.immutable());}
                // Select one existing source terminal deterministically; publication/scheduler/TNT remain native.
                set(endingSword,"at",Vec3.atCenterOf(blastCenter));set(endingSword,"stage",1);set(endingSword,"selected",2);next(41);return false;
            }
            if(stage==41){
                if(blastBlocks.stream().noneMatch(p->level.getBlockState(p).isAir())){require(elapsed<15,"source terminal actual native TNT destroys fixture blocks in ENDING");return false;}
                long falling=0;for(var e:level.getAllEntities())if(!original.contains(e.getUUID())&&e instanceof net.minecraft.world.entity.item.FallingBlockEntity body&&LbcCosmetics.managed(e)){falling++;require(body.getBlockState().is(Blocks.GOLD_BLOCK)&&cosmetics.owner(e)==owner&&cosmetics.cause(e).friendlyFire()&&game.items().spellSources().source(e)!=null,"actual falling gold material and inherited native winner WILD source");}
                require(falling>0,"source terminal publishes native falling gold bodies beside real explosion");
                cosmetics.stop();var midas=(LbcMidas)get(cosmetics,"midas");blastCenter=BlockPos.containing(origin.add(12,0,24));for(int x=0;x<=21;x++)block(blastCenter.offset(x,0,0),x==1?Blocks.RAW_GOLD_BLOCK.defaultBlockState():Blocks.STONE.defaultBlockState());
                for(int x=0;x<=21;x++)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++)if(dy!=0||dz!=0)block(blastCenter.offset(x,dy,dz),Blocks.AIR.defaultBlockState());
                midas.launch(owner,level,blastCenter,game.items().indirectKills().wildCause(owner));next(42);return false;
            }
            if(stage==42){
                if(!level.getBlockState(blastCenter.offset(20,0,0)).is(Blocks.RAW_GOLD_BLOCK)){require(elapsed<45,"real shared ENDING Midas propagates source20 logical layers");return false;}
                require(level.getBlockState(blastCenter.offset(21,0,0)).is(Blocks.STONE),"Euclidean radius20 excludes21 while raw-gold neighbour still propagates");
                cosmetics.stop();profile().winEffect=1;grant(owner,"lbc:true_advancements/event/pumpkin_sword");require(cosmetics.winner(owner,0)&&effectJobs("PUMPKIN").size()==5,"source pumpkin celebration produces five phase-owned jobs");
                for(var job:effectJobs("PUMPKIN")){set(job,"spreading",false);set(job,"at",Vec3.atCenterOf(blastCenter));set(job,"stage",1);set(job,"selected",9);}
                set(effectJobs("PUMPKIN").getFirst(),"selected",0);
                next(43);return false;
            }
            if(stage==43){
                if(effectJobs("FILL").isEmpty()){require(elapsed<10,"source pumpkin producers enroll real destructive terrain work in ENDING");return false;}
                boolean pumpkin=false;for(var e:level.getAllEntities())if(!original.contains(e.getUUID())&&e instanceof net.minecraft.world.entity.item.FallingBlockEntity body&&LbcCosmetics.managed(e)&&body.getBlockState().is(Blocks.PUMPKIN)){pumpkin=true;require(cosmetics.owner(e)==owner&&cosmetics.cause(e).friendlyFire()&&game.items().spellSources().source(e)!=null,"actual falling pumpkin retains native material and causal winner source");}require(pumpkin,"source30percent branch publishes a real PUMPKIN falling body");
                require(game.items().stonekivi().queuedSnapshot().stream().anyMatch(LbcStonekivi.Work::postgame),"same canonical queue retains source postgame capability");cosmetics.stop();game.items().stonekivi().tick();require(jobs().isEmpty(),"source phase cleanup removes accepted producers");for(var e:level.getAllEntities())require(original.contains(e.getUUID())||!LbcCosmetics.managed(e),"cleanup removes every owned native display/ingot/falling body");return true;
            }
            throw new IllegalStateException("cosmetic ending stage "+stage);
        }
        boolean step(){
            client.acknowledge();if(other.isAlive())audience.acknowledge();require(game.server().getTickCount()-started<1200,"bounded scheduled fixture "+name()+" stage="+stage);int elapsed=game.server().getTickCount()-when;
            if(restoring&&game.vanity().busy())for(var p:game.server().getPlayerList().getPlayers())if(p.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            if(kind==0)return stage==2?manualStep():caseStep();if(kind==1)return cosmeticStep(elapsed);return rewindStep(elapsed);
        }
        @Override public void close(){
            if(closed)return;closed=true;Throwable failure=null;
            for(Runnable action:List.<Runnable>of(
                    ()->{if(offline){game.server().getPlayerList().getPlayersByUUID().put(owner.getUUID(),owner);offline=false;}},
                    ()->{if(kind==2)game.vanity().stop();},()->{cosmetics.stop();if(stopped){game.items().timeStop().end(level);stopped=false;}},
                    ()->{if(retired!=null&&!other.isAlive())respawn();},
                    ()->{
                        owner.closeContainer();other.closeContainer();
                        // getAllEntities() is a live EntityLookup view; discard mutates that view.
                        var cleanup=new ArrayList<Entity>();
                        for(var e:level.getAllEntities())if(e!=null&&!original.contains(e.getUUID())&&!(e instanceof ServerPlayer))cleanup.add(e);
                        for(var e:cleanup){if(e instanceof LivingEntity living)game.items().indirectKills().clear(living);e.discard();}
                    },
                    ()->{game.maps().cages(false);for(var entry:terrain.entrySet())game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue(),818);},
                    ()->{
                        game.server().tickRateManager().setFrozen(true);
                        if(endingStarted){
                            // finish() genuinely retired the original gameplay epoch. After its owned
                            // postgame work is gone, restore this fixture's original identity handle
                            // before using the strict same-epoch core/world checkpoint adapters.
                            game.items().stonekivi().stop();require(jobs().isEmpty(),"all postgame work removed before fixture epoch restoration");set(game.items(),"entityEpochTag",epoch);
                        }
                        core.restoreFrozen();world.restoreFrozen();random.restoreFrozen();
                    },
                    ()->{saved.restoreAttributes();otherSaved.restoreAttributes();saved.restore(game);otherSaved.restore(game);saved.restoreUse();otherSaved.restoreUse();ownerRandom.restore(owner.getRandom());otherRandom.restore(other.getRandom());owner.getStats().setValue(owner,net.minecraft.stats.Stats.ENTITY_KILLED.get(EntityTypes.PLAYER),playerKills);other.getStats().setValue(other,net.minecraft.stats.Stats.ENTITY_KILLED_BY.get(EntityTypes.PLAYER),playerKilledBy);},
                    ()->{set(cosmetics,"policy",policy);cases.restored();game.players().save();if(kind==1)game.config().save();},
                    ()->{game.items().magic().inventoryChanged(owner);game.items().magic().inventoryChanged(other);game.items().magic().flushCleansing();},
                    client::close,()->audience.close(),()->game.server().tickRateManager().setFrozen(frozen)))
                try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("Cases/cosmetics scoped native restoration",failure);
        }
    }
}
