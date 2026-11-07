package me.skywars2.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.phys.Vec3;

/** Native source shield and all five physical TMT carriers; no damage interception or owner tick loop. */
public final class TmtShieldChecks {
    private TmtShieldChecks(){}
    private static Scene running;
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT Hylian shield: "+why);}
    public static void run(GameController game){require(running==null,"exclusive scene");var next=new Scene(game);running=next;
        try{next.begin();}catch(Throwable error){running=null;try{next.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    public static void tick(GameController game){var scene=running;if(scene==null)return;
        try{require(scene.game==game,"same controller");if(!scene.step())return;running=null;scene.close();
            SkyWars2.LOGGER.info("[check] PASS tmt-hylian-shield (stock unbreakable source plus all5 physical carriers, actual gameMode use/native delay, front/rear/bypass damage, native wear/stat/audio, private slot restoration, real apple32tick consumption, native axe disable/cooldown recovery and actual max-damage break, real loaded-crossbow shot and bow ammo/release preserved; no Hylian sword alias)");
        }catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL tmt-hylian-shield",error);}}
    public static void cancel(GameController game){var scene=running;if(scene==null)return;require(scene.game==game,"same cancel owner");running=null;scene.close();}

    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final NativeMeleeChecks.NativeStats itemStats;
        final java.util.Map<Stat<?>,Integer> stats=new java.util.LinkedHashMap<>();
        LbcTmtAbilities.Ability shield,sibling;Zombie attacker;ItemStack current;CustomData original;
        InteractionHand hand=InteractionHand.MAIN_HAND;int carrier=-1,phase,when,start,disableTicks;boolean closed;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);
            var items=new ArrayList<net.minecraft.world.item.Item>();items.add(LbcMeleeItems.Kind.HYLIAN_SHIELD.item);for(int i=0;i<5;i++)items.add(LbcTmtItems.prototype(i).getItem());
            itemStats=new NativeMeleeChecks.NativeStats(f.owner,items);
            for(var key:List.of(Stats.DAMAGE_TAKEN,Stats.DAMAGE_BLOCKED_BY_SHIELD,Stats.DAMAGE_RESISTED)){var stat=Stats.CUSTOM.get(key);stats.put(stat,f.owner.getStats().getValue(stat));}}
        LbcTmtAbilities.Ability property(String flag){return game.items().tmt().catalog().families().stream().flatMap(row->row.variants().stream())
                .filter(row->((ItemData.IntReader)(Object)row.data()).skywars2$readInt(flag,0)==1).findFirst().orElseThrow();}
        ItemStack mixed(int index){var stack=LbcTmtItems.prototype(index);var data=stack.get(DataComponents.CUSTOM_DATA).copyTag();
            LbcTmtAbilities.write(data,List.of(shield,sibling));stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));if(index==4)stack.setCount(2);
            var food=stack.get(DataComponents.CONSUMABLE);var damage=stack.get(DataComponents.MAX_DAMAGE);
            LbcTmtShield.apply(game,stack);
            require(stack.get(DataComponents.CONSUMABLE)==food&&java.util.Objects.equals(stack.get(DataComponents.MAX_DAMAGE),damage)
                    &&!stack.has(DataComponents.UNBREAKABLE),"shield attaches behavior while preserving generated food/durability and independent unbreakability");return stack;}
        @SuppressWarnings("unchecked") List<Packet<?>> packets(){return (List<Packet<?>>)WoolBucketChecks.read(f.client.connection,"packets");}
        long sounds(String name){return packets().stream().filter(ClientboundSoundPacket.class::isInstance).map(ClientboundSoundPacket.class::cast)
                .filter(packet->packet.getSound().value().location().toString().equals(name)).count();}
        int blocked(){return f.owner.getStats().getValue(Stats.CUSTOM.get(Stats.DAMAGE_BLOCKED_BY_SHIELD));}
        void front(){attacker.setPos(f.owner.position().add(0,0,3));f.owner.setYRot(0);f.owner.setYHeadRot(0);f.owner.setYBodyRot(0);f.owner.setXRot(0);}
        void resetHit(){f.owner.setInvulnerableTime(0);f.owner.damageCooldownTime=0;f.owner.setDeltaMovement(Vec3.ZERO);}
        void next(int value){phase=value;when=game.server().getTickCount();}
        void begin(){f.prepareParticipants();start=game.server().getTickCount();shield=property("hylianshield");sibling=property("reroll_machine");
            require(((ItemData.IntReader)(Object)shield.data()).skywars2$readInt("hylian",0)==0,"actual shield source is not the sword aura");
            var draws=List.of(shield,sibling,shield,sibling,shield,sibling,shield);
            for(int seed=0;seed<5;seed++){var generated=game.items().tmt().generate(net.minecraft.util.RandomSource.create(seed),draws);
                require(generated.has(DataComponents.BLOCKS_ATTACKS)&&!generated.get(DataComponents.EQUIPPABLE).swappable()
                        &&LbcTmtAbilities.read(generated).size()==2,"actual generation attaches native blocking and prevents equipment swapping from intercepting sustained use");}
            attacker=new Zombie(EntityTypes.ZOMBIE,f.level);attacker.setNoAi(true);attacker.setNoGravity(true);attacker.setSilent(true);
            attacker.setPos(f.home.add(0,0,3));require(f.level.addFreshEntity(attacker),"actual native attacker registered");f.own(attacker);equip();}
        void equip(){
            f.owner.stopUsingItem();f.owner.removeAllEffects();f.owner.setAbsorptionAmount(0);f.owner.setHealth(900);resetHit();attacker.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);f.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            current=carrier<0?f.leaf("hylian_shield"):mixed(carrier);original=current.get(DataComponents.CUSTOM_DATA);
            f.owner.setItemInHand(hand,current);front();
            require(f.owner.getProjectile(current).isEmpty(),"bow/crossbow fallback is exercised without fabricated ammunition");
            require(f.owner.gameMode.useItem(f.owner,f.level,current,hand).consumesAction()&&f.owner.isUsingItem()&&f.owner.getUseItem()==current,"real physical carrier input starts native shield-compatible use: "+carrier+"/"+hand);
            require(f.owner.getItemBlockingWith()==null,"native shield delay still rejects an immediate block");
            float hp=f.owner.getHealth();int wear=current.getDamageValue();f.owner.hurtServer(f.level,f.owner.damageSources().mobAttack(attacker),1);
            require(f.owner.getHealth()<hp&&current.getDamageValue()==wear,"real damage before native blocking delay reaches HP without shield wear");next(0);
        }
        void blocking(){
            front();resetHit();require(f.owner.getItemBlockingWith()==current,"actual native using clock reaches source blocking delay");
            var block=current.get(DataComponents.BLOCKS_ATTACKS);var damage=f.owner.damageSources().mobAttack(attacker);
            require(block.resolveBlockedDamage(damage,4,0)==4,"native source component recognizes actual frontal damage");
            float hp=f.owner.getHealth();int wear=current.getDamageValue(),before=blocked();boolean damageable=current.isDamageableItem();String sound=block.blockSound().orElseThrow().value().location().toString();long heard=sounds(sound);
            boolean damageAccepted=f.owner.hurtServer(f.level,damage,4);
            require(!damageAccepted&&f.owner.getHealth()==hp&&blocked()>before&&sounds(sound)>heard,"actual zero-HP native block produces authoritative shield statistic and audio feedback");
            // Player native mob damage may be difficulty-scaled before blocking. Its authoritative
            // blocked-damage statistic measures that actual blocked amount in tenths of one HP.
            float actuallyBlocked=(blocked()-before)/10f;int expected=damageable?block.itemDamage().apply(actuallyBlocked):0;
            require(current.getDamageValue()==wear+expected,"actual native block wears only the physical carrier's own durability: carrier="+carrier+" hand="+hand
                    +" difficulty="+f.level.getDifficulty()+" blocked="+actuallyBlocked+" before="+wear+" after="+current.getDamageValue()+" expectedDelta="+expected
                    +" damageable="+damageable+" unbreakable="+current.has(DataComponents.UNBREAKABLE)+" enchantments="+current.get(DataComponents.ENCHANTMENTS));
            if(carrier<0)require(current.has(DataComponents.UNBREAKABLE)&&current.getDamageValue()==0,"stock Hylian source retains its authored unbreakability");
            else require(current.get(DataComponents.CUSTOM_DATA)==original&&LbcTmtAbilities.read(current).size()==2&&!LbcTmtAbilities.active(current)
                    &&ItemData.intValue(current,"hylian",0)==0,"actual damage preserves independent private shield/craft traits and never creates a sword aura");
            attacker.setPos(f.owner.position().add(0,0,-3));resetHit();hp=f.owner.getHealth();f.owner.hurtServer(f.level,damage,1);
            require(f.owner.getHealth()<hp,"real rear damage remains outside native shield arc");front();resetHit();hp=f.owner.getHealth();
            var bypass=f.owner.damageSources().fellOutOfWorld();require(block.bypassedBy().map(types->types.contains(bypass.typeHolder())).orElse(false),"actual bypass source belongs to the native component's bypass set");f.owner.hurtServer(f.level,bypass,1);
            require(f.owner.getHealth()<hp,"native bypass damage reaches HP during actual blocking");
        }
        boolean step(){f.client.acknowledge();f.audience.acknowledge();require(game.server().getTickCount()-start<600,"bounded native shield scene");int elapsed=game.server().getTickCount()-when;
            if(phase==0){if(elapsed<current.get(DataComponents.BLOCKS_ATTACKS).blockDelayTicks()+2)return false;blocking();
                if(carrier==4){next(1);return false;}
                if(hand==InteractionHand.OFF_HAND){
                    attacker.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_AXE));front();resetHit();
                    float seconds=attacker.getSecondsToDisableBlocking();require(seconds>0,"actual native axe weapon component provides disabling duration");
                    // Cached26.3 native private helper: positive(seconds * scale) -> round(value *20), otherwise0.
                    disableTicks=Math.max(0,Math.round(seconds*current.get(DataComponents.BLOCKS_ATTACKS).disableCooldownScale()*20));float hp=f.owner.getHealth();
                    f.owner.hurtServer(f.level,f.owner.damageSources().mobAttack(attacker),4);
                    require(f.owner.getHealth()==hp&&!f.owner.isUsingItem()&&f.owner.getCooldowns().isOnCooldown(current),"actual frontal axe hit invokes native shield disable and physical-item cooldown");
                    require(!f.owner.gameMode.useItem(f.owner,f.level,current,hand).consumesAction()&&!f.owner.isUsingItem(),"real native cooldown rejects immediate physical re-use");next(3);return false;
                }
                release();return false;
            }
            if(phase==1){if(f.owner.isUsingItem()){require(elapsed<40,"generated edible duration must finish rather than resetting on every USING replay");return false;}
                require(current.getCount()==1&&f.owner.getItemBlockingWith()==null&&current.get(DataComponents.CUSTOM_DATA)==original&&LbcTmtAbilities.read(current).size()==2,"actual apple consumption ends its32tick shield window, debits one apple and retains both private traits");
                release();return false;
            }
            if(phase==2){if(elapsed<1)return false;equip();return false;}
            if(phase==3){if(f.owner.getCooldowns().isOnCooldown(current)){require(elapsed<=disableTicks+5,"native cooldown expires on its real scheduler");return false;}
                attacker.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);front();resetHit();
                require(f.owner.gameMode.useItem(f.owner,f.level,current,hand).consumesAction()&&f.owner.isUsingItem(),"same actual offhand carrier can block again after natural native cooldown");next(4);return false;
            }
            if(phase==4){if(elapsed<current.get(DataComponents.BLOCKS_ATTACKS).blockDelayTicks()+2)return false;
                require(f.owner.getItemBlockingWith()==current&&current.isDamageableItem(),"actual recovered damageable carrier is blocking");current.setDamageValue(current.getMaxDamage()-1);
                front();resetHit();float hp=f.owner.getHealth();f.owner.hurtServer(f.level,f.owner.damageSources().mobAttack(attacker),4);
                require(f.owner.getHealth()==hp&&current.isEmpty()&&f.owner.getItemInHand(hand).isEmpty(),"native max-damage block breaks the real physical item while preventing that accepted hit's HP damage");rangedPrecedence();return false;
            }
            if(phase==5){if(elapsed<12)return false;require(f.owner.getItemBlockingWith()==current,"ordinary native bow draw also reaches the real shield delay without resetting it");
                var before=new HashSet<Projectile>(f.level.getEntitiesOfClass(Projectile.class,f.owner.getBoundingBox().inflate(20)));f.owner.releaseUsingItem();
                var shots=f.level.getEntitiesOfClass(Projectile.class,f.owner.getBoundingBox().inflate(20),actor->!before.contains(actor));
                for(var shot:shots)f.own(shot);
                require(shots.size()==1&&shots.getFirst().getOwner()==f.owner&&f.owner.getOffhandItem().getCount()==1
                        &&current.get(DataComponents.CUSTOM_DATA)==original&&LbcTmtAbilities.read(current).size()==2,"real native bow release debits one actual ammo and fires one owned projectile without losing either private property");
                for(var shot:shots)f.discard(shot);return true;
            }
            return false;
        }
        void release(){
            var before=new HashSet<Projectile>(f.level.getEntitiesOfClass(Projectile.class,f.owner.getBoundingBox().inflate(20)));
            f.owner.releaseUsingItem();require(!f.owner.isUsingItem()&&f.owner.getItemBlockingWith()==null,"real native release ends blocking");
            require(f.level.getEntitiesOfClass(Projectile.class,f.owner.getBoundingBox().inflate(20),actor->!before.contains(actor)).isEmpty(),"release without native ammo never fabricates a bow/crossbow shot");
            if(++carrier>4){carrier=0;hand=InteractionHand.OFF_HAND;}next(2);
        }
        void rangedPrecedence(){
            f.owner.stopUsingItem();f.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            var loaded=mixed(3);var data=loaded.get(DataComponents.CUSTOM_DATA);f.owner.setItemInHand(InteractionHand.MAIN_HAND,loaded);
            loaded.set(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.ofNonEmpty(List.of(new ItemStack(Items.ARROW))));
            var before=new HashSet<Projectile>(f.level.getEntitiesOfClass(Projectile.class,f.owner.getBoundingBox().inflate(20)));
            require(f.owner.gameMode.useItem(f.owner,f.level,loaded,InteractionHand.MAIN_HAND).consumesAction(),"real loaded crossbow preserves native shot precedence");
            var shots=f.level.getEntitiesOfClass(Projectile.class,f.owner.getBoundingBox().inflate(20),actor->!before.contains(actor));
            for(var shot:shots)f.own(shot);
            require(shots.size()==1&&shots.getFirst().getOwner()==f.owner&&loaded.get(DataComponents.CHARGED_PROJECTILES).isEmpty()
                    &&loaded.get(DataComponents.CUSTOM_DATA)==data&&LbcTmtAbilities.read(loaded).size()==2,"loaded native carrier fires one real owned projectile and retains both private shield/craft properties");
            for(var shot:shots)f.discard(shot);f.owner.stopUsingItem();
            current=mixed(1);original=current.get(DataComponents.CUSTOM_DATA);f.owner.setItemInHand(InteractionHand.MAIN_HAND,current);
            f.owner.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.ARROW,2));front();
            require(f.owner.gameMode.useItem(f.owner,f.level,current,InteractionHand.MAIN_HAND).consumesAction()&&f.owner.isUsingItem(),"real bow ammo draw still starts with the shield component");next(5);
        }
        public void close(){if(closed)return;closed=true;try{f.owner.stopUsingItem();if(attacker!=null)f.discard(attacker);}
            finally{try{f.close();}finally{itemStats.close();stats.forEach((stat,value)->f.owner.getStats().setValue(f.owner,stat,value));}}}
    }
}
