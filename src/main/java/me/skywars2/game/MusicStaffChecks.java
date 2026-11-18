package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

/** Real native arrows, global sound packets and a full uninterrupted 1040-server-tick study. */
public final class MusicStaffChecks {
    private MusicStaffChecks(){}
    private static Fixture running;
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException("Music staff: "+message);}
    public static void run(GameController game){require(running==null,"exclusive fixture");var fixture=new Fixture(game);running=fixture;try{fixture.begin();}catch(Throwable error){running=null;fixture.close();throw error;}}
    public static void tick(GameController game){
        if(running==null)return;var f=running;
        try{require(f.game==game,"same native game");if(!f.step())return;running=null;f.close();SkyWars2.LOGGER.info("[check] PASS music-staff (bundled prices, aqua score lore and a local arrow sound, no cooldown including stale item tags, shared double-bow offsets/independent spread, fixed8/16 WILD allied hits, global music lifecycle, actual1040tick Academy trial)");}
        catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL music-staff",error);}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture base;final LbcMusicStaff service;
        final net.minecraft.world.scores.PlayerTeam team;final Entity teacher;
        ItemStack staff;int phase,when;final int began;float health;boolean closed;
        Fixture(GameController game){
            this.game=game;base=new WoolBucketChecks.Fixture(game,false);base.prepareParticipants();service=game.items().musicStaff();
            require(service.arrows().isEmpty()&&!service.playing(),"exclusive musical scene");
            var board=game.server().getScoreboard();require(board.getPlayerTeam("sw2_music_check")==null,"exclusive team");team=board.addPlayerTeam("sw2_music_check");team.setAllowFriendlyFire(false);
            board.addPlayerToTeam(base.owner.getScoreboardName(),team);board.addPlayerToTeam(base.other.getScoreboardName(),team);
            var interaction=EntityTypes.INTERACTION.create(base.level,EntitySpawnReason.TRIGGERED);teacher=Objects.requireNonNull(interaction);teacher.entityTags().add("magic_academy");teacher.setPos(base.home.add(3,0,0));require(base.level.addFreshEntity(teacher),"native Academy teacher");base.own(teacher);
            staff=base.leaf("music_staff");began=when=game.server().getTickCount();
        }
        void next(int phase){this.phase=phase;when=game.server().getTickCount();}
        List<ClientboundSoundPacket> songs(RegenerationBeltChecks.ScheduledClient client){return SmokeVolumeChecks.flatten(client.packets()).stream().filter(ClientboundSoundPacket.class::isInstance).map(ClientboundSoundPacket.class::cast).filter(p->p.getSound().value().location().equals(LbcMusicStaff.MUSIC)).toList();}
        long stops(RegenerationBeltChecks.ScheduledClient client){return SmokeVolumeChecks.flatten(client.packets()).stream().filter(ClientboundStopSoundPacket.class::isInstance).map(ClientboundStopSoundPacket.class::cast).filter(p->LbcMusicStaff.MUSIC.equals(p.getName())).count();}
        long hitSounds(RegenerationBeltChecks.ScheduledClient client) {
            return SmokeVolumeChecks.flatten(client.packets()).stream().filter(packet -> packet instanceof ClientboundGameEventPacket event
                && event.getEvent() == ClientboundGameEventPacket.PLAY_ARROW_HIT_SOUND).count();
        }
        long shotSounds(RegenerationBeltChecks.ScheduledClient client, int start) {
            var packets = client.packets();
            return SmokeVolumeChecks.flatten(packets.subList(start, packets.size())).stream()
                    .filter(packet -> packet instanceof ClientboundSoundPacket sound
                            && sound.getSound().value() == net.minecraft.sounds.SoundEvents.ARROW_SHOOT)
                    .count();
        }
        List<LbcMusicStaff.Shot> shoot(ItemStack item){
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,item);game.items().mana().set(base.owner,5000);
            var prior=Collections.newSetFromMap(new IdentityHashMap<LbcMusicStaff.Shot,Boolean>());prior.addAll(service.arrows());
            int casterBefore = base.client.packets().size(), audienceBefore = base.audience.packets().size();
            require(base.owner.gameMode.useItem(base.owner,base.level,item,InteractionHand.MAIN_HAND).consumesAction(),"real native staff use");
            require(shotSounds(base.client, casterBefore) == 1, "one vanilla firing sound per cast, even for two arrows");
            if (base.owner.distanceToSqr(base.other) < 100) {
                require(shotSounds(base.audience, audienceBefore) == 1, "nearby players hear the local arrow shot");
            }
            var fresh=service.arrows().stream().filter(s->!prior.contains(s)).toList();require(!fresh.isEmpty()&&fresh.size()<=2,"one or two arrows");
            var eye=base.owner.getEyePosition();var forward=base.owner.getLookAngle().scale(.3);var side=Vec3.directionFromRotation(0,base.owner.getYRot()-90).scale(.4);
            for(var shot:fresh){base.own(shot);require(shot.getOwner()==base.owner,"real physical controller");
                require(fresh.size()==1?shot.position().distanceToSqr(eye)<1e-10:Math.min(shot.position().distanceToSqr(eye.add(forward).add(side)),shot.position().distanceToSqr(eye.add(forward).subtract(side)))<1e-10,"single camera origin or original double-bow .4 side/.3 forward geometry");
                require(shot.getDeltaMovement().length()>2.8&&shot.getDeltaMovement().length()<3.2,"native bow launch speed with vanilla triangular spread");require(shot.model!=null&&shot.model.display()!=null,"tracked authored arrow model");
                require(game.items().spellSources().source(shot).cause().friendlyFire(),"captured WILD cause");}
            if(fresh.size()==2)require(fresh.get(0).position().distanceToSqr(fresh.get(1).position())>.63,"two distinct opposite muzzle positions");return fresh;
        }
        void manaConfiguration(){
            try (var input = LbcSourceConfig.class.getResourceAsStream(LbcSourceConfig.RESOURCE)) {
                require(input != null, "packaged authored source values exist");
                var values = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(
                        input, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("values");
                for (var name : List.of("music_staff", "academy_music_staff")) {
                    var item = base.leaf(name);
                    int price = values.get(name.startsWith("academy") ? LbcMusicStaff.ACADEMY_MANA : LbcMusicStaff.BASE_MANA).getAsInt();
                    var line = LbcTooltipScores.resolve(game, base.owner, item.get(DataComponents.LORE).lines().get(1));
                    require(line.getString().startsWith(Integer.toString(price)) && line.getStyle().getColor() != null
                                    && line.getStyle().getColor().getValue() == 0x55FFFF,
                            "actual score-based aqua mana lore uses the bundled price for " + name);
                    require(ItemData.intValue(item,"spellwithcooldown",0)==0&&item.get(DataComponents.USE_COOLDOWN)==null,"fresh staff has no cooldown declaration");
                    var stale=item.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();stale.putInt("mana_cost",1);stale.putInt("cast_cooldown_ticks",999);stale.putInt("spellwithcooldown",1);item.set(DataComponents.CUSTOM_DATA,CustomData.of(stale));
                    for (int cast = 0; cast < 2; cast++) {
                        var arrows = shoot(item);
                        require(game.items().mana().get(base.owner) == 5000 - price
                                        && !base.owner.getCooldowns().isOnCooldown(item),
                                "same-tick native casts use the bundled price and ignore obsolete stack tuning");
                        arrows.forEach(base::discard);
                    }
                }
            } catch (java.io.IOException error) {
                throw new IllegalStateException(error);
            }
        }
        void aim(){base.move(base.owner,base.home.add(0,12,0));base.move(base.other,base.owner.position().add(0,0,6));base.owner.setYRot(0);base.owner.setXRot(0);base.other.setInvulnerableTime(0);base.other.damageCooldownTime=0;base.other.setHealth(1000);health=base.other.getHealth();}
        void begin(){
            require(staff.is(LbcMusicStaff.ITEM)&&!LbcMusicStaff.upgraded(staff),"base canonical leaf");aim();manaConfiguration();var shots=shoot(staff);require(shots.size()==1,"base always emits one");next(0);
        }
        boolean step(){
            require(game.server().getTickCount()-began<1800,"bounded native52second fixture");int elapsed=game.server().getTickCount()-when;
            if(phase==0){
                if(elapsed<8)return false;require(base.other.getHealth()==health-8,"actual allied native fixed8 hit, HP="+base.other.getHealth());
                require(hitSounds(base.client) == 1 && hitSounds(base.audience) == 0,
                    "physically owned WILD staff arrow sends exactly one private hit sound");
                require(service.playing()&&songs(base.client).size()==1&&songs(base.audience).size()==1,"one common track for holder and other player");
                var sound=songs(base.client).getFirst();require(sound.getY()==1_000_000&&sound.getVolume()==Float.MAX_VALUE,"exact requested height and finite maximum volume");
                aim();var improved=base.leaf("academy_music_staff");var shots=shoot(improved);for(int i=1;i<shots.size();i++)base.discard(shots.get(i));
                // The double-bow muzzles are wider than a player's central hitbox: test damage on the chosen arrow's lane.
                var offset=shots.getFirst().position().subtract(base.owner.getEyePosition());base.move(base.other,base.other.position().add(offset.x,0,offset.z));next(1);return false;
            }
            if(phase==1){
                if(elapsed<8)return false;require(base.other.getHealth()==health-16,"actual upgraded native fixed16 hit, HP="+base.other.getHealth());
                require(hitSounds(base.client) == 2 && hitSounds(base.audience) == 0,
                    "upgraded staff preserves one private hit sound for the actual projectile");
                base.move(base.other,base.home.add(20,12,0));var improved=base.leaf("academy_music_staff");int doubles=0;
                for(int cast=0;cast<100;cast++){
                    base.owner.setYRot(cast*17);base.owner.setXRot((cast%13-6)*7);
                    var shots=shoot(improved);if(shots.size()==2){doubles++;require(shots.get(0).getDeltaMovement().distanceToSqr(shots.get(1).getDeltaMovement())>1e-12,"double arrows have independently sampled spread");}
                    shots.forEach(base::discard);
                }
                require(doubles>=25&&doubles<=80,"both random upgraded branches occur in broad native sample; doubles="+doubles);
                staff=base.leaf("music_staff");base.owner.setItemInHand(InteractionHand.MAIN_HAND,staff);
                require(game.items().academy().interact(base.owner,InteractionHand.MAIN_HAND,teacher)&&LbcAcademy.trial(staff),"actual Academy assigns standard trial");
                next(2);return false;
            }
            if(phase==2){
                if(service.studyTicks(base.owner)<60)return false;int progress=service.studyTicks(base.owner);base.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);base.owner.setItemInHand(InteractionHand.OFF_HAND,staff);next(3);return false;
            }
            if(phase==3){
                if(elapsed<3)return false;require(service.studyTicks(base.owner)>=63,"moving same staff to offhand keeps uninterrupted listening");
                base.other.setItemInHand(InteractionHand.MAIN_HAND,base.leaf("music_staff"));base.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);next(4);return false;
            }
            if(phase==4){
                if(elapsed<3)return false;require(service.studyTicks(base.owner)==0&&service.playing()&&stops(base.client)==0,"removing owner staff resets study, another holder preserves common music");
                base.other.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);next(5);return false;
            }
            if(phase==5){
                if(elapsed<3)return false;require(!service.playing()&&stops(base.client)==1&&stops(base.audience)==1,"last holder disappearance stops exact track for everyone");
                base.owner.setItemInHand(InteractionHand.MAIN_HAND,staff);next(6);return false;
            }
            if(phase==6){
                if(elapsed<1039){require(ItemData.intValue(staff,"upgrade_available",0)==0,"study cannot finish before52seconds");return false;}
                if(service.studyTicks(base.owner)>0){require(service.studyTicks(base.owner)==1039&&LbcAcademy.trial(staff),"alive trial after1039continuous ticks");next(7);return false;}
                throw new IllegalStateException("Music staff: study completed too early");
            }
            if(phase==7){
                require(elapsed==1&&ItemData.intValue(staff,"upgrade_available",0)==1&&!LbcAcademy.trial(staff),"ready on exact1040th native tick");
                require(game.items().academy().interact(base.owner,InteractionHand.MAIN_HAND,teacher),"actual Academy promotion input");
                var promoted=base.owner.getMainHandItem();require(promoted.is(LbcMusicStaff.ACADEMY)&&LbcMusicStaff.upgraded(promoted),"real promoted native item");
                require(promoted.get(DataComponents.LORE).lines().stream().anyMatch(c->c.getString().contains("16")),"promoted damage lore updated");
                float hp=base.owner.getHealth();base.owner.setHealth(0);service.tick();require(!service.playing(),"dead holder stops music even before inventory disappears");base.owner.setHealth(hp);
                service.stop();require(service.studyTicks(base.owner)==0&&service.arrows().isEmpty(),"round reset clears transient study/arrows");return true;
            }
            return false;
        }
        @Override public void close(){if(closed)return;closed=true;service.stop();var board=game.server().getScoreboard();for(var p:List.of(base.owner,base.other))if(p.getTeam()==team)board.removePlayerFromTeam(p.getScoreboardName(),team);board.removePlayerTeam(team);base.close();}
    }
}
