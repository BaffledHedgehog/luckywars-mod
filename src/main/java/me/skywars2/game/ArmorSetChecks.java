package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Source loot, real equipped stacks, sourced native status and the live 5/20 tick scheduler. */
public final class ArmorSetChecks {
    private ArmorSetChecks(){}
    private static final EquipmentSlot[] SLOTS={EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD};
    private static final String[] PARTS={"boots","leggings","chestplate","helmet"};
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Armor sets: "+why);}
    private static java.lang.reflect.Field field(Object value,String name){for(Class<?> type=value.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new IllegalStateException("Missing fixture field "+name);}
    private static Object member(Object value,String name){try{return field(value,name).get(value);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static void set(Object value,String name,Object replacement){try{field(value,name).set(value,replacement);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static void effect(LivingEntity target,Holder<MobEffect> type,int ticks,int amplifier){var e=target.getEffect(type);require(e!=null&&e.getDuration()==ticks&&e.getAmplifier()==amplifier,"native effect "+type+" duration="+ticks+" amp="+amplifier);}

    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;final ServerPlayer owner,beta;
        final GameChecks.WandThirdPlayerState betaSaved;final LbcArmorSets service;
        final List<Cow> actors=new ArrayList<>();final Set<Long> forced=new HashSet<>(),wanted=new LinkedHashSet<>();final boolean skills;final int skill,oldPhase;final Object oldEpoch;
        final net.minecraft.world.entity.ai.attributes.AttributeMap attributes=new net.minecraft.world.entity.ai.attributes.AttributeMap(net.minecraft.world.entity.ai.attributes.DefaultAttributes.getSupplier(EntityTypes.PLAYER));
        final Vec3 at;final Map<String,ItemStack[]> sources=new LinkedHashMap<>();
        ScheduledClient client;int started,stage,updates,warmTicks;boolean closed,preparing=true;
        Fixture(GameController game){this.game=game;service=game.items().armorSets();base=new ElementalGrimoireChecks.Fixture(game);owner=base.owner;
            beta=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));betaSaved=new GameChecks.WandThirdPlayerState(beta,game.items().mana());
            for(var holder:net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.listElements().toList()){var attr=owner.getAttribute(holder);if(attr!=null)attributes.getInstance(holder).replaceFrom(attr);}
            skills=game.config().lbcSkills;skill=game.players().get(owner.getUUID()).lbcSkill;oldPhase=(Integer)member(service,"phase");oldEpoch=member(service,"epoch");
            base.initialize();at=owner.position();
            // The farthest legitimate aura edge is eighty blocks; own only its thin fixture corridor.
            for(int x=((int)Math.floor(at.x)-1)>>4;x<=((int)Math.floor(at.x)+101)>>4;x++)for(int z=((int)Math.floor(at.z)-1)>>4;z<=((int)Math.floor(at.z)+2)>>4;z++){long key=net.minecraft.world.level.ChunkPos.pack(x,z);wanted.add(key);if(!owner.level().getForceLoadedChunks().contains(key)){forced.add(key);owner.level().setChunkForced(x,z,true);}owner.level().getChunk(x,z);}
            beta.teleportTo(owner.level(),at.x+100,at.y,at.z,Set.of(),0,0,true);beta.setNoGravity(true);
            owner.removeAllEffects();owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1_000_000);owner.setHealth(1_000_000);game.config().lbcSkills=false;game.players().get(owner.getUUID()).lbcSkill=0;
        }
        boolean prepare(){
            var missing=wanted.stream().filter(key->!owner.level().areEntitiesLoaded(key)).toList();
            require(++warmTicks<=200,"native aura corridor ready within200 ticks; missing="+missing);
            if(!missing.isEmpty())return false;
            preparing=false;acquisition();reforges();paladin();auras();erosion();guards();schedule();return true;
        }
        void auraEffect(LivingEntity target,Holder<MobEffect> type,int amount,String label){
            var active=target.getEffect(type);boolean visible=owner.level().getEntitiesOfClass(LivingEntity.class,owner.getBoundingBox().inflate(80),value->value==target).contains(target);
            require(active!=null&&active.getDuration()==60&&active.getAmplifier()==0,"native aura target="+label+" amount="+amount+" type="+type+" actual="+active+" targetPos="+target.position()+" wearerPos="+owner.position()+" distanceSquared="+target.position().distanceToSqr(owner.position())+" queryVisible="+visible+" entityTicking="+owner.level().isPositionEntityTicking(target.blockPosition())+" eligible="+game.items().wands().projectileTarget(owner,target));
        }
        List<ItemStack> roll(String id){return game.lbc().rollTable(owner.level(),at,owner,"lbc:"+id);}
        void empty(){for(var slot:SLOTS)owner.setItemSlot(slot,ItemStack.EMPTY);owner.removeAllEffects();for(var target:actors)target.removeAllEffects();beta.removeAllEffects();base.recording.packets.clear();}
        void wear(String family,int count){empty();for(int n=0;n<count;n++)owner.setItemSlot(SLOTS[n],sources.get(family)[n].copy());}
        Cow cow(Vec3 position){var cow=new Cow(EntityTypes.COW,owner.level());cow.setNoAi(true);cow.setNoGravity(true);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);cow.setPos(position);require(owner.level().addFreshEntity(cow),"real target publication");actors.add(cow);return cow;}
        int protection(ItemStack stack){return EnchantmentHelper.getItemEnchantmentLevel(owner.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION),stack);}
        void protection(ItemStack stack,int value){var e=owner.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);EnchantmentHelper.updateEnchantments(stack,m->m.set(e,value));}
        void acquisition(){
            for(var family:List.of("paladin","shitty","erosion")){
                var leaves=new ItemStack[4];sources.put(family,leaves);
                for(int n=0;n<4;n++){
                    var out=roll("neko_set_"+family+"/"+PARTS[n]);require(out.size()==1&&out.getFirst().getCount()==1,"exact authored leaf "+family+"/"+PARTS[n]);var stack=leaves[n]=out.getFirst();
                    var vanilla=List.of(Items.LEATHER_BOOTS,Items.LEATHER_LEGGINGS,Items.LEATHER_CHESTPLATE,Items.LEATHER_HELMET).get(n);
                    require(stack.is(vanilla)&&stack.has(DataComponents.CUSTOM_NAME)&&stack.has(DataComponents.LORE)&&stack.has(DataComponents.ITEM_MODEL)&&stack.has(DataComponents.TOOLTIP_STYLE),"source item and presentation "+family+"/"+PARTS[n]);
                    require(stack.get(DataComponents.EQUIPPABLE).slot()==SLOTS[n],"native authored equipment slot");
                    require(stack.getMaxDamage()==(family.equals("paladin")?4000:family.equals("erosion")?9000:300),"authored durability");
                    require(LbcFartus.data(stack).getIntOr(family.equals("shitty")?"gownoleggings":family,0)==1&&LbcFartus.data(stack).getIntOr("transformable",0)==1,"original shared flags");
                    require(protection(stack)==(family.equals("paladin")?3:family.equals("erosion")?new int[]{100,180,255,120}[n]:0),"source Protection level");
                    if(family.equals("shitty"))require(LbcFartus.data(stack).getIntOr("govnoq",0)==1&&LbcFartus.data(stack).getIntOr("craftusable",0)==1,"Gowno crafting flags preserved");
                    if(family.equals("paladin")){
                        var entry=List.of(Enchantments.FEATHER_FALLING,Enchantments.FIRE_PROTECTION,Enchantments.BLAST_PROTECTION,Enchantments.PROJECTILE_PROTECTION).get(n);
                        require(EnchantmentHelper.getItemEnchantmentLevel(owner.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(entry),stack)==255,"authored extreme secondary enchantment");
                        var mods=stack.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers();require(mods.size()==3,"three source Paladin attributes");double armor=new double[]{7,10,15,8}[n],health=new double[]{-.1,-.3,-.3,-.2}[n];
                        require(mods.stream().anyMatch(e->e.attribute().equals(Attributes.ARMOR)&&e.modifier().amount()==armor)&&mods.stream().anyMatch(e->e.attribute().equals(Attributes.ARMOR_TOUGHNESS)&&e.modifier().amount()==armor)&&mods.stream().anyMatch(e->e.attribute().equals(Attributes.MAX_HEALTH)&&Math.abs(e.modifier().amount()-health)<.00001),"exact source armor/toughness/health amounts");
                    }else if(family.equals("erosion"))require(stack.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().size()==1,"source erosion armor attribute");
                }
                var full=roll("neko_set_"+family+"_full");require(full.size()==4,"four-piece source full acquisition");for(var leaf:leaves)require(full.stream().anyMatch(s->ItemStack.matches(s,leaf)),"full acquisition retains every original component");
                var seen=new HashSet<Integer>();for(int attempt=0;attempt<256&&seen.size()<4;attempt++){var root=roll("neko_set_"+family);require(root.size()==1,"native root one weighted piece");int selected=-1;for(int n=0;n<4;n++)if(ItemStack.matches(root.getFirst(),leaves[n]))selected=n;require(selected>=0,"root preserves source leaf identity");seen.add(selected);}require(seen.size()==4,"all four native root outcomes");
            }
        }
        @SuppressWarnings("unchecked") void reforges(){
            for(int slot:new int[]{0,1,3})for(var family:List.of("shitty","paladin","erosion")){
                int selected=family.equals("shitty")?5:family.equals("paladin")?11:13;String id="transformables_"+PARTS[slot];
                var table=game.server().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,net.minecraft.resources.Identifier.parse("lbc:"+id)));
                var pools=(List<net.minecraft.world.level.storage.loot.LootPool>)member(table,"pools");require(pools.size()==1,"one native original armor reforge pool");
                var sequence=(Optional<net.minecraft.resources.Identifier>)member(table,"randomSequence");require(sequence.isEmpty(),"normal armor reforge uses native level RNG");
                var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(owner.level()).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,at).withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,owner).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.COMMAND);
                var context=new net.minecraft.world.level.storage.loot.LootContext.Builder(params).create(sequence);var pool=pools.getFirst();int total=0,offset=-1,selectedWeight=0;
                for(int n=0;n<pool.entries.size();n++){var expanded=new ArrayList<net.minecraft.world.level.storage.loot.entries.LootPoolEntry>();pool.entries.get(n).expand(context,expanded::add);for(var entry:expanded){int weight=entry.getWeight(context.getLuck());if(weight<=0)continue;if(n==selected){if(offset<0)offset=total;selectedWeight+=weight;}total+=weight;}}
                require(selectedWeight==1&&offset>=0,"exact original weight1 set reforge branch");int bound=total,pick=offset;var random=field(owner.level(),"random");Object original=member(owner.level(),"random");int[] calls={0};
                var input=new net.minecraft.world.item.ItemStack(Items.STICK);var flags=new net.minecraft.nbt.CompoundTag();flags.putInt("transformable",1);flags.putInt(PARTS[slot],1);input.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(flags));var donor=new net.minecraft.world.entity.item.ItemEntity(owner.level(),at.x,at.y,at.z,input);donor.setNoGravity(true);require(owner.level().addFreshEntity(donor),"real native armor reforge donor");var outputs=new ArrayList<ItemStack>();
                try{random.set(owner.level(),java.lang.reflect.Proxy.newProxyInstance(net.minecraft.util.RandomSource.class.getClassLoader(),new Class<?>[]{net.minecraft.util.RandomSource.class},(proxy,method,args)->{if(method.getName().equals("nextInt")&&args!=null&&args.length==1&&args[0].equals(bound)){calls[0]++;return pick;}try{return method.invoke(original,args);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}}));
                    var factory=LbcBolts.class.getDeclaredMethod("reforgeInput",net.minecraft.world.entity.item.ItemEntity.class,LbcBolts.ReforgeFamily.class,int.class,ServerPlayer.class,java.util.function.Consumer.class);factory.setAccessible(true);var work=(LbcStonekivi.Work)factory.invoke(game.items().bolts(),donor,LbcBolts.ReforgeFamily.NORMAL,1,owner,(java.util.function.Consumer<ItemStack>)outputs::add);require(work!=null&&work.step(),"existing native normal armor reforge Work");
                    require(calls[0]==1&&donor.isRemoved()&&input.isEmpty()&&outputs.size()==1&&ItemStack.matches(outputs.getFirst(),sources.get(family)[slot]),"native reforge consumes one input and retains exact authored leaf "+family+"/"+PARTS[slot]);
                }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}finally{try{random.set(owner.level(),original);}catch(IllegalAccessException e){throw new IllegalStateException(e);}finally{donor.discard();}}
            }
        }
        void paladin(){
            for(int count=0;count<=4;count++){wear("paladin",count);service.pulse20(owner);require(owner.hasEffect(MobEffects.RESISTANCE)==(count==4),"Paladin requires actual four worn pieces, count="+count);if(count==4)effect(owner,MobEffects.RESISTANCE,60,3);}
            empty();for(int n=0;n<4;n++)owner.getInventory().setItem(9+n,sources.get("paladin")[n].copy());service.pulse20(owner);require(!owner.hasEffect(MobEffects.RESISTANCE),"carried full set is not equipped");owner.getInventory().clearContent();
        }
        void auras(){
            var inner=cow(at.add(1,0,0));var edge=cow(at.add(10,0,0));var outside=cow(at.add(10.01,0,0));var same=cow(at);var greed=cow(at.add(2,0,0));greed.entityTags().add("sin_greed_active");var technical=cow(at.add(2,0,1));technical.entityTags().add("strict_map_object");
            for(int amount=1;amount<=8;amount++){
                // Odd amounts above four cannot be reached by an authored loadout; the source has eight
                // selectors, of which 1..4 and skill9's 2/4/6/8 are gameplay-reachable.
                if(amount==5||amount==7)continue;
                boolean doubled=amount>4;game.config().lbcSkills=doubled;game.players().get(owner.getUUID()).lbcSkill=doubled?9:0;
                wear("shitty",doubled?amount/2:amount);double radius=10*amount;edge.setPos(at.add(radius,0,0));outside.setPos(at.add(radius+.01,0,0));service.pulse20(owner);
                auraEffect(inner,MobEffects.POISON,amount,"inner");auraEffect(inner,MobEffects.HUNGER,amount,"inner");auraEffect(edge,MobEffects.POISON,amount,"outer-edge");
                require(!outside.hasEffect(MobEffects.POISON)&&!same.hasEffect(MobEffects.POISON)&&!owner.hasEffect(MobEffects.POISON),"exact source outer radius and minimum distance amount="+amount);
                require(!greed.hasEffect(MobEffects.POISON)&&!technical.hasEffect(MobEffects.POISON),"native Greed and technical roles rejected");
                require(inner.hasEffect(MobEffects.WITHER)==(amount>=4),"source Wither starts at four effective pieces");
                if(amount>=4){effect(inner,MobEffects.WITHER,60,3);double wr=4*(amount-3);edge.setPos(at.add(wr,0,0));outside.setPos(at.add(wr+.01,0,0));edge.removeAllEffects();outside.removeAllEffects();service.pulse20(owner);effect(edge,MobEffects.WITHER,60,3);require(!outside.hasEffect(MobEffects.WITHER)&&outside.hasEffect(MobEffects.POISON),"independent inner Wither band amount="+amount);}
                var packets=base.recording.packets.stream().filter(net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.class::isInstance).map(net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.class::cast).toList();
                int expectedAmount=amount;double spread=amount==1?16:8*amount;require(packets.stream().anyMatch(p->p.particle().getType()==net.minecraft.core.particles.ParticleTypes.SNEEZE&&p.count()==expectedAmount&&Math.abs(p.y()-(at.y+1))<1e-5&&p.xDist()==spread&&p.yDist()==spread&&p.zDist()==spread&&!p.overrideLimiter()&&!p.alwaysShow()),"native particle amount and owner+1 origin");
            }
            // Skill9 admission is tested at every physical count, including the config-off guard.
            game.players().get(owner.getUUID()).lbcSkill=9;for(int count=1;count<=4;count++){game.config().lbcSkills=true;wear("shitty",count);edge.setPos(at.add(20*count,0,0));service.pulse20(owner);auraEffect(edge,MobEffects.POISON,2*count,"skill9-edge");game.config().lbcSkills=false;edge.removeAllEffects();service.pulse20(owner);require(!edge.hasEffect(MobEffects.POISON),"skill9 only doubles with native config enabled");}
            // Actual scoreboard alliance, rather than a synthetic team tag.
            var board=game.server().getScoreboard();var oldOwner=owner.getTeam();var oldBeta=beta.getTeam();require(board.getPlayerTeam("armor_sets_fixture")==null,"exclusive alliance fixture");var team=board.addPlayerTeam("armor_sets_fixture");team.setAllowFriendlyFire(false);
            try{board.addPlayerToTeam(owner.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);beta.teleportTo(owner.level(),at.x+2,at.y,at.z,Set.of(),0,0,true);wear("shitty",4);service.pulse20(owner);require(!beta.hasEffect(MobEffects.POISON)&&!beta.hasEffect(MobEffects.HUNGER)&&!beta.hasEffect(MobEffects.WITHER),"native allied participant protected");}
            finally{board.removePlayerFromTeam(owner.getScoreboardName(),team);board.removePlayerFromTeam(beta.getScoreboardName(),team);board.removePlayerTeam(team);if(oldOwner!=null)board.addPlayerToTeam(owner.getScoreboardName(),oldOwner);if(oldBeta!=null)board.addPlayerToTeam(beta.getScoreboardName(),oldBeta);}
            beta.teleportTo(owner.level(),at.x+2,at.y,at.z,Set.of(),0,0,true);beta.removeAllEffects();wear("shitty",4);service.pulse20(owner);effect(beta,MobEffects.HUNGER,60,0);
            var statuses=(Map<?,?>)member(game.items().indirectKills(),"statuses");var byEffect=(Map<?,?>)statuses.get(inner);require(byEffect!=null,"native poison attribution registered");var status=byEffect.get(MobEffects.POISON);var cause=member(status,"owner");require(owner.getUUID().equals(member(cause,"player"))&&member(status,"effect")==inner.getEffect(MobEffects.POISON),"active native effect retains registered wearer's UUID and actual effect identity");
            var debts=(Map<?,?>)member(game.items().indirectKills(),"hungerDebts");require(debts.get(beta)!=null&&owner.getUUID().equals(member(debts.get(beta),"player")),"native hunger causality uses actual registered owner");
            beta.removeAllEffects();beta.teleportTo(owner.level(),at.x+100,at.y,at.z,Set.of(),0,0,true);game.config().lbcSkills=false;game.players().get(owner.getUUID()).lbcSkill=0;
        }
        void erosion(){
            for(int count=0;count<=3;count++){wear("erosion",count);service.erode(owner);for(int n=0;n<count;n++)require(protection(owner.getItemBySlot(SLOTS[n]))==protection(sources.get("erosion")[n])-1,"every partial equipped piece erodes count="+count);}
            for(int selected=0;selected<4;selected++){wear("erosion",4);var saved=new ItemStack[4];for(int n=0;n<4;n++)saved[n]=owner.getItemBySlot(SLOTS[n]).copy();try(var random=new RandomControl(owner,selected)){service.erode(owner);require(random.calls==1,"one full-set four-way random choice");}for(int n=0;n<4;n++){var actual=owner.getItemBySlot(SLOTS[n]);require(protection(actual)==protection(saved[n])-(n==selected?1:0),"full set modifies only chosen live native slot branch="+selected);var expected=saved[n].copy();if(n==selected)protection(expected,protection(expected)-1);require(ItemStack.matches(actual,expected),"native enchantment mutation preserves all unrelated stack metadata");}}
            wear("erosion",1);var actual=owner.getItemBySlot(SLOTS[0]);protection(actual,1);var unbreaking=owner.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING);EnchantmentHelper.updateEnchantments(actual,m->m.set(unbreaking,7));actual.setDamageValue(17);var detached=actual.copy();service.erode(owner);require(protection(actual)==0&&EnchantmentHelper.getItemEnchantmentLevel(unbreaking,actual)==7&&actual.getDamageValue()==17,"Protection reaches zero without touching other enchantments/durability");var exhausted=actual.copy();service.erode(owner);require(ItemStack.matches(actual,exhausted)&&protection(detached)==1,"zero is stable and detached saved copies are not mutated");owner.setItemSlot(SLOTS[0],new ItemStack(Items.LEATHER_BOOTS));service.erode(owner);require(protection(detached)==1&&owner.getItemBySlot(SLOTS[0]).getEnchantments().isEmpty(),"replaced slot never reuses stale erosion reference");
            wear("erosion",4);protection(owner.getItemBySlot(SLOTS[2]),0);var before=Arrays.stream(SLOTS).map(owner::getItemBySlot).map(ItemStack::copy).toList();try(var random=new RandomControl(owner,2)){service.erode(owner);require(random.calls==1,"exhausted full-set slot still consumes native random branch");}for(int n=0;n<4;n++)require(ItemStack.matches(before.get(n),owner.getItemBySlot(SLOTS[n])),"exhausted selected branch is wasted, no retry");
        }
        void guards(){
            wear("paladin",4);for(var tag:List.of("time_stopped","spectator","strict_map_object")){owner.entityTags().add(tag);try{service.pulse20(owner);require(!owner.hasEffect(MobEffects.RESISTANCE),"native owner role guard "+tag);}finally{owner.entityTags().remove(tag);}}
            game.items().timeStop().begin(owner.level(),40);try{service.pulse20(owner);require(!owner.hasEffect(MobEffects.RESISTANCE),"actual dimension Time Stop suppresses passive");}finally{game.items().timeStop().end(owner.level());}
            require(game.items().shooters().stun(beta,owner,10),"native stun control");try{require(game.items().actionsBlocked(owner),"stun blocks active actions");service.pulse20(owner);effect(owner,MobEffects.RESISTANCE,60,3);}finally{game.items().shooters().clearStun(owner);}owner.removeAllEffects();
            owner.entityTags().add("nomagic_active");try{game.items().mana().noMagic(owner,100);service.pulse20(owner);effect(owner,MobEffects.RESISTANCE,60,3);}finally{owner.entityTags().remove("nomagic_active");game.items().mana().noMagic(owner,0);}owner.removeAllEffects();
            var original=game.stage();try{set(game,"stage",GameController.Stage.ENDING);service.pulse20(owner);require(!owner.hasEffect(MobEffects.RESISTANCE),"ENDING rejects callbacks");}finally{set(game,"stage",original);}
            var level=owner.level();var elsewhere=java.util.stream.StreamSupport.stream(game.server().getAllLevels().spliterator(),false).filter(l->l!=level).findFirst().orElseThrow();try{owner.teleportTo(elsewhere,at.x,at.y,at.z,Set.of(),0,0,true);service.pulse20(owner);require(!owner.hasEffect(MobEffects.RESISTANCE),"wrong dimension rejects native passive");}finally{owner.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true);}
            service.stop();require((Integer)member(service,"phase")==0&&member(service,"epoch")==null,"service stop removes cadence state");
        }
        void schedule(){
            actors.forEach(Cow::discard);actors.clear();owner.getInventory().clearContent();wear("erosion",1);client=new ScheduledClient(game,owner);set(service,"epoch",game.items().epoch());set(service,"phase",0);started=game.server().getTickCount();updates=client.updates();stage=-1;
        }
        boolean step(){
            int elapsed=game.server().getTickCount()-started;require(elapsed<90,"bounded actual native scheduler fixture");require(client.ready(),"actual scheduled owner stays client-loaded and teleport-complete");
            // Establish the clock after this command's first END tick, not before the service runs in that same tick.
            if(stage<0){set(service,"phase",0);started=game.server().getTickCount();updates=client.updates();stage=0;return false;}
            if(stage==0){require(protection(owner.getItemBySlot(SLOTS[0]))==100-elapsed/5,"actual five-tick erosion cadence elapsed="+elapsed);if(elapsed<20)return false;require(elapsed==20&&client.updates()-updates>=20,"twenty actual registered connection updates");wear("paladin",4);set(service,"phase",0);started=game.server().getTickCount();stage=1;return false;}
            require(owner.hasEffect(MobEffects.RESISTANCE)==(elapsed>=20),"actual twenty-tick Paladin cadence elapsed="+elapsed);if(elapsed<20)return false;effect(owner,MobEffects.RESISTANCE,60,3);return true;
        }
        @Override public void close(){if(closed)return;closed=true;try{if(client!=null)client.close();actors.forEach(Cow::discard);for(long key:forced)game.maps().level().setChunkForced((int)key,(int)(key>>32),false);game.items().shooters().clearStun(owner);game.items().timeStop().end(game.maps().level());game.config().lbcSkills=skills;game.players().get(owner.getUUID()).lbcSkill=skill;set(service,"phase",oldPhase);set(service,"epoch",oldEpoch);betaSaved.restore(game);}finally{base.retain=true;base.close();for(var holder:net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.listElements().toList()){var attr=owner.getAttribute(holder);var saved=attributes.getInstance(holder);if(attr!=null&&saved!=null)attr.replaceFrom(saved);}owner.setHealth(base.saved.health);owner.setAbsorptionAmount(base.saved.absorption);}}
    }
    private static final class ScheduledConnection extends net.minecraft.network.Connection {
        final List<net.minecraft.network.protocol.Packet<?>> packets=new ArrayList<>();
        net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket latestPosition;
        ScheduledConnection(){super(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);}
        @Override public boolean isConnecting(){return false;}
        @Override public boolean isConnected(){return true;}
        @Override public boolean isMemoryConnection(){return true;}
        @Override public void send(net.minecraft.network.protocol.Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){packets.add(packet);if(packet instanceof net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket position)latestPosition=position;}
        @Override public void flushChannel(){}
    }
    private static final class ScheduledClient implements AutoCloseable {
        final GameController game;final ServerPlayer player;final net.minecraft.server.network.ServerGamePacketListenerImpl previous,listener;
        final ScheduledConnection connection=new ScheduledConnection();final java.lang.reflect.Field updates;
        ScheduledClient(GameController game,ServerPlayer player){this.game=game;this.player=player;previous=player.connection;
            try{
                listener=new net.minecraft.server.network.ServerGamePacketListenerImpl(game.server(),connection,player,new net.minecraft.server.network.CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));
                // A fresh memory connection has no protocol channel pipeline; bind only its own native dispatch field.
                field(connection,"packetListener").set(connection,listener);updates=field(listener,"tickCount");
                listener.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
                listener.teleport(player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot());
                var teleport=connection.packets.stream().filter(net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.class::isInstance).map(net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.class::cast).reduce((a,b)->b).orElseThrow();
                listener.handleAcceptTeleportPacket(new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket(teleport.id(),player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot()));
                require(listener.hasClientLoaded()&&field(listener,"awaitingPositionFromClient").get(listener)==null,"fresh native listener loaded and real teleport acknowledged");
                game.server().getConnection().getConnections().add(connection);
            }catch(ReflectiveOperationException|RuntimeException|Error error){game.server().getConnection().getConnections().remove(connection);player.connection=previous;throw new IllegalStateException("scoped native connection scheduler",error);}
        }
        void acknowledge(){listener.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());try{if(field(listener,"awaitingPositionFromClient").get(listener)!=null){var teleport=Objects.requireNonNull(connection.latestPosition,"actual emitted teleport packet survives measurement windows");listener.handleAcceptTeleportPacket(new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket(teleport.id(),player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot()));}}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}require(ready(),"native player loaded and latest actual teleport acknowledged");}
        boolean ready(){try{return listener.hasClientLoaded()&&!player.isChangingDimension()&&field(listener,"awaitingPositionFromClient").get(listener)==null;}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
        int updates(){try{return updates.getInt(listener);}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
        @Override public void close(){try{game.server().getConnection().getConnections().remove(connection);}finally{player.connection=previous;}}
    }
    private static final class RandomControl implements AutoCloseable {
        final Object player,original;final java.lang.reflect.Field random;int calls;
        RandomControl(ServerPlayer player,int selected){this.player=player;random=field(player,"random");try{original=random.get(player);random.set(player,java.lang.reflect.Proxy.newProxyInstance(net.minecraft.util.RandomSource.class.getClassLoader(),new Class<?>[]{net.minecraft.util.RandomSource.class},(proxy,method,args)->{if(method.getName().equals("nextInt")&&args!=null&&args.length==1&&args[0].equals(4)){calls++;return selected;}try{return method.invoke(original,args);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}}));}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
        @Override public void close(){try{random.set(player,original);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    }
    private static Fixture running;
    public static void run(GameController game){require(running==null,"exclusive native armor fixture");running=new Fixture(game);}
    public static void tick(GameController game){if(running==null)return;try{if(running.preparing){running.prepare();return;}if(running.step()){running.close();running=null;SkyWars2.LOGGER.info("[check] PASS armor-sets (12 original leaves, roots/full acquisition; equipped Paladin; reachable Shitty radii, skill9, native teams/roles/causality; all four native Erosion choices and metadata; actual5/20 scheduler)");}}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL armor-sets",error);}}
}
