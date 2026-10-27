package com.matoon.eternity.compat;

import anvil.infinity.abilities.AbilitySnap;
import anvil.infinity.snap.SnapHelper;
import com.matoon.eternity.Eternity;
import com.matoon.eternity.entity.ArmorPowers;
import com.matoon.eternity.entity.EntityDestroyerArmor;
import lucraft.mods.heroesexpansion.abilities.AbilityBlackHole;
import lucraft.mods.heroesexpansion.abilities.AbilityForceField;
import lucraft.mods.heroesexpansion.abilities.AbilityGrabEntity;
import lucraft.mods.lucraftcore.infinity.EnumInfinityStone;
import lucraft.mods.lucraftcore.infinity.ModuleInfinity;
import lucraft.mods.lucraftcore.infinity.items.InventoryInfinityGauntlet;
import lucraft.mods.lucraftcore.infinity.items.ItemInfinityStone;
import lucraft.mods.lucraftcore.superpowers.abilities.Ability;
import lucraft.mods.lucraftcore.superpowers.abilities.AbilityAttributeModifier;
import lucraft.mods.lucraftcore.superpowers.abilities.AbilityDamageResistance;
import lucraft.mods.lucraftcore.superpowers.abilities.AbilityEntry;
import lucraft.mods.lucraftcore.superpowers.abilities.AbilityFlight;
import lucraft.mods.lucraftcore.superpowers.abilities.AbilityHealing;
import lucraft.mods.lucraftcore.superpowers.abilities.AbilityHealth;
import lucraft.mods.lucraftcore.superpowers.abilities.AbilityInvisibility;
import lucraft.mods.lucraftcore.superpowers.abilities.AbilityPunch;
import lucraft.mods.lucraftcore.superpowers.abilities.AbilityStrength;
import lucraft.mods.lucraftcore.superpowers.abilities.data.AbilityData;
import lucraft.mods.lucraftcore.superpowers.abilities.supplier.AbilityContainer;
import lucraft.mods.lucraftcore.superpowers.abilities.supplier.IAbilityProvider;
import lucraft.mods.lucraftcore.util.abilitybar.EnumAbilityBarColor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Infinity Stones set in the Destroyer Armor's chest, lending whoever is inside it their own powers: the same
 * Lucraft abilities each stone lends set in an Infinity Gauntlet, on the ability bar like any other. They come
 * through a context of their own, so they are there exactly while someone rides an armour with stones in it, and
 * change the moment a stone is set or taken out.
 * <p>
 * Most work as they are. The few that reach out from their user's eyes would find the armour first, since those eyes
 * are inside it; they are swapped for versions that reach out past it. The armour reads the rest (flight, the force
 * field, invisibility, strength, resistance, healing) off its pilot and does them itself; see
 * {@link EntityDestroyerArmor}.
 */
@Mod.EventBusSubscriber(modid = Eternity.MODID)
public final class StoneAbilities {

    public static Ability.EnumAbilityContext CONTEXT;

    /** One provider for each set of stones, so the same set is always the same provider and abilities only rebuild when it changes. */
    private static final Map<String, Gifts> SETS = new HashMap<>();

    private StoneAbilities() {
    }

    /**
     * The stand-ins, entered in Lucraft's registry of abilities as every ability must be; they take the names and
     * settings of the abilities they stand in for.
     */
    @SubscribeEvent
    public static void onRegisterAbilities(RegistryEvent.Register<AbilityEntry> event) {
        event.getRegistry().register(new AbilityEntry(Grip.class, new ResourceLocation(Eternity.MODID, "destroyer_grip")));
        event.getRegistry().register(new AbilityEntry(Snap.class, new ResourceLocation(Eternity.MODID, "destroyer_snap")));
    }

    /** In pre-init, before any entity is made: every entity's abilities are laid out by context as it is created. */
    public static void register() {
        Ability.addAbilityContext("ETERNITY_DESTROYER");
        CONTEXT = Ability.EnumAbilityContext.valueOf("ETERNITY_DESTROYER");
        Ability.registerSupplier(CONTEXT, StoneAbilities::provider, AbilityContainer::new);
    }

    @Nullable
    private static IAbilityProvider provider(EntityLivingBase entity) {
        EntityDestroyerArmor armor = EntityDestroyerArmor.carrier(entity);
        if (armor == null || !(entity instanceof EntityPlayer)) {
            return null;
        }
        String kinds = armor.stoneKinds();
        return kinds.isEmpty() ? null : SETS.computeIfAbsent(kinds, Gifts::new);
    }

    /** What a set of stones lends, by their items' registry names in socket order. */
    private static final class Gifts implements IAbilityProvider {
        private final List<ItemInfinityStone> stones = new ArrayList<>();

        Gifts(String kinds) {
            for (String name : kinds.split(",")) {
                Item item = name.isEmpty() ? null : Item.REGISTRY.getObject(new ResourceLocation(name));
                if (item instanceof ItemInfinityStone) {
                    stones.add((ItemInfinityStone) item);
                }
            }
        }

        @Override
        public Ability.AbilityMap addDefaultAbilities(EntityLivingBase entity, Ability.AbilityMap abilities, Ability.EnumAbilityContext context) {
            boolean space = false, power = false;
            for (ItemInfinityStone stone : stones) {
                stone.addStoneAbilities(entity, abilities, context);
                space |= stone.getType() == EnumInfinityStone.SPACE;
                power |= stone.getType() == EnumInfinityStone.POWER;
            }
            // In a gauntlet the Space Stone opens a black hole beside the Power Stone, which it looks for in its user's
            // hands; here both are in the chest.
            if (space && power && Ability.getAbilitiesFromClass(new ArrayList<>(abilities.values()), AbilityBlackHole.class).isEmpty()) {
                abilities.put("black_hole", new AbilityBlackHole(entity).setDataValue(Ability.BAR_COLOR, EnumAbilityBarColor.LIGHT_BLUE).setMaxCooldown(6000));
            }
            for (String key : new ArrayList<>(abilities.keySet())) {
                Ability ability = abilities.get(key);
                if (ability.getClass() == AbilityGrabEntity.class) {
                    abilities.put(key, copy(ability, new Grip(entity)));
                } else if (ability.getClass() == AbilitySnap.class) {
                    abilities.put(key, copy(ability, new Snap(entity)));
                }
            }
            return abilities;
        }
    }

    /** One ability's settings onto another, standing in for it. */
    private static Ability copy(Ability from, Ability to) {
        for (AbilityData<?> data : from.getDataManager().getData()) {
            put(to, data, from.getDataManager().get(data));
        }
        return to;
    }

    @SuppressWarnings("unchecked")
    private static <T> void put(Ability to, AbilityData<T> data, Object value) {
        if (value != null && to.getDataManager().has(data)) {
            to.getDataManager().set(data, (T) value);
        }
    }

    // ------------------------------------------------------------------ what reaches out past the armour

    /**
     * Grabbing (the Space Stone's grip, the Mind Stone's telekinesis): looking out from inside the armour, the first
     * thing in sight is always the armour itself. This one looks out from past its face and never takes hold of it.
     */
    public static class Grip extends AbilityGrabEntity {
        public Grip(EntityLivingBase entity) {
            super(entity);
        }

        @Override
        public RayTraceResult getPosLookingAt() {
            EntityDestroyerArmor armor = EntityDestroyerArmor.carrier(entity);
            if (armor == null) {
                return super.getPosLookingAt();
            }
            Vec3d eye = entity.getPositionEyes(1F), look = entity.getLookVec();
            double distance = entity.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue() + armor.width;
            for (double along = armor.width * 0.75D; along < distance; along += 0.5D) {
                Vec3d pos = eye.add(look.scale(along));
                BlockPos block = new BlockPos(pos);
                if (entity.world.isBlockFullCube(block) && !entity.world.isAirBlock(block)) {
                    return new RayTraceResult(RayTraceResult.Type.BLOCK, pos, null, block);
                }
                AxisAlignedBB box = new AxisAlignedBB(pos.x - 0.25D, pos.y - 0.25D, pos.z - 0.25D, pos.x + 0.25D, pos.y + 0.25D, pos.z + 0.25D);
                for (Entity found : entity.world.getEntitiesInAABBexcluding(entity, box, e -> e != armor)) {
                    return new RayTraceResult(found);
                }
            }
            return new RayTraceResult(eye.add(look.scale(distance)), null);
        }
    }

    /**
     * The snap wants all six stones in a gauntlet in its user's hand. With all six in the armour's chest, its pilot's
     * hand holds one for the instant of it, set with the armour's own; if the snap is turned on its maker, it is the
     * armour's stones that are spent.
     */
    public static class Snap extends AbilitySnap {
        public Snap(EntityLivingBase entity) {
            super(entity);
        }

        @Override
        public boolean action() {
            EntityDestroyerArmor armor = EntityDestroyerArmor.carrier(entity);
            if (armor == null || entity.world.isRemote || Integer.bitCount(armor.stones()) < ArmorPowers.COUNT) {
                return super.action();
            }
            ItemStack gauntlet = new ItemStack(ModuleInfinity.INFINITY_GAUNTLET);
            gauntlet.setTagCompound(new NBTTagCompound());
            InventoryInfinityGauntlet sockets = new InventoryInfinityGauntlet(gauntlet);
            for (int i = 0; i < sockets.getSizeInventory(); i++) {
                sockets.setInventorySlotContents(i, armor.stone(InventoryInfinityGauntlet.SLOTS[i].ordinal()).copy());
            }
            sockets.markDirty();
            ItemStack held = entity.getHeldItemMainhand();
            entity.setHeldItem(EnumHand.MAIN_HAND, gauntlet);
            boolean done;
            try {
                done = SnapHelper.snap(entity);
            } finally {
                ItemStack after = entity.getHeldItemMainhand();
                entity.setHeldItem(EnumHand.MAIN_HAND, held);
                if (!after.hasTagCompound() || new InventoryInfinityGauntlet(after).isEmpty()) {
                    armor.spendStones();
                }
            }
            return done;
        }
    }

    // ------------------------------------------------------------------ what the armour reads off its pilot

    /** Whether they are flying by a power of their own, which the armour then does for them. */
    public static boolean flying(@Nullable EntityLivingBase pilot) {
        if (pilot == null) {
            return false;
        }
        for (AbilityFlight flight : Ability.getAbilitiesFromClass(Ability.getAbilities(pilot), AbilityFlight.class)) {
            if (flight.isUnlocked() && flight.isEnabled()) {
                return true;
            }
        }
        return false;
    }

    /** The force field they hold up, which stands around the whole armour, or null. */
    @Nullable
    public static AbilityForceField shield(@Nullable EntityLivingBase pilot) {
        if (pilot == null) {
            return null;
        }
        for (AbilityForceField field : Ability.getAbilitiesFromClass(Ability.getAbilities(pilot), AbilityForceField.class)) {
            if (field.isUnlocked() && field.isEnabled()) {
                return field;
            }
        }
        return null;
    }

    /** Whether they have made themselves unseen, and the armour with them. */
    public static boolean unseen(@Nullable EntityLivingBase pilot) {
        if (pilot == null) {
            return false;
        }
        for (AbilityInvisibility invisibility : Ability.getAbilitiesFromClass(Ability.getAbilities(pilot), AbilityInvisibility.class)) {
            if (invisibility.isUnlocked() && invisibility.isEnabled()) {
                return true;
            }
        }
        return false;
    }

    /** How much harder their blows land, which the armour's fists carry: their strength and their punch, as their attributes would cap it. */
    public static float strength(@Nullable EntityLivingBase pilot) {
        return Math.min(2048F, amount(pilot, AbilityStrength.class) + amount(pilot, AbilityPunch.class));
    }

    /** How much armour they are lent, which plates the armour. */
    public static float resistance(@Nullable EntityLivingBase pilot) {
        return Math.min(30F, amount(pilot, AbilityDamageResistance.class));
    }

    /** Whether they are lent more life than they have, which the armour takes as more integrity. */
    public static boolean immortal(@Nullable EntityLivingBase pilot) {
        return amount(pilot, AbilityHealth.class) > 0F;
    }

    /** Their healing, which mends the armour as it mends them, or null. */
    @Nullable
    public static AbilityHealing healing(@Nullable EntityLivingBase pilot) {
        if (pilot == null) {
            return null;
        }
        for (AbilityHealing healing : Ability.getAbilitiesFromClass(Ability.getAbilities(pilot), AbilityHealing.class)) {
            if (healing.isUnlocked()) {
                return healing;
            }
        }
        return null;
    }

    private static float amount(@Nullable EntityLivingBase pilot, Class<? extends AbilityAttributeModifier> kind) {
        if (pilot == null) {
            return 0F;
        }
        float total = 0F;
        for (AbilityAttributeModifier modifier : Ability.getAbilitiesFromClass(Ability.getAbilities(pilot), kind)) {
            if (modifier.isUnlocked()) {
                total += Math.max(0F, modifier.getAmount());
            }
        }
        return total;
    }
}
