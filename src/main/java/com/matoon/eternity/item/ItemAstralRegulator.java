package com.matoon.eternity.item;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.ability.EternityAbilities;
import com.matoon.eternity.ability.MastersAbilities;
import com.matoon.eternity.ability.TribunalAbilities;
import com.matoon.eternity.entity.EntityEternity;
import com.matoon.eternity.entity.EntityInBetweener;
import com.matoon.eternity.entity.EntityLivingTribunal;
import com.matoon.eternity.entity.EntityOrderChaos;
import com.matoon.eternity.world.Director;
import com.matoon.eternity.world.InBetweenerDirector;
import com.matoon.eternity.world.OrderAndChaos;
import com.matoon.eternity.world.TribunalDirector;
import lucraft.mods.lucraftcore.infinity.items.ItemIndestructible;
import lucraft.mods.lucraftcore.superpowers.abilities.Ability;
import lucraft.mods.lucraftcore.superpowers.abilities.supplier.IAbilityProvider;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import com.matoon.eternity.entity.EntityDestroyerArmor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * A faceted crystal built to hold what should never be released. Used on Eternity as
 * he dies, it drinks his escaping energy back in and saves the universe; used on any of
 * the other cosmic beings once they are beaten, it drinks them in too, in whatever order.
 * Once it holds anything it is charged: its name burns purple, it remembers what it
 * absorbed, and whoever holds it can wield those powers as Lucraft abilities. Like the
 * stones, it cannot be destroyed.
 */
public class ItemAstralRegulator extends ItemIndestructible implements IAbilityProvider {

    public static final String ETERNITY = Eternity.MODID + ":eternity";
    public static final String TRIBUNAL = Eternity.MODID + ":living_tribunal";
    public static final String ORDER = Eternity.MODID + ":master_order";
    public static final String CHAOS = Eternity.MODID + ":lord_chaos";
    public static final String INBETWEENER = Eternity.MODID + ":inbetweener";

    private static final String CHARGED = "Charged";
    private static final String ABSORBED = "Absorbed";

    public ItemAstralRegulator() {
        setRegistryName(Eternity.MODID, "astral_regulator");
        setTranslationKey(Eternity.MODID + ".astral_regulator");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.TOOLS);
        setEntitySize(0.5F, 0.5F);
    }

    public static boolean isCharged(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().getBoolean(CHARGED);
    }

    /** Entity ids of the cosmic beings held inside, in the order they were absorbed. */
    public static List<String> absorbed(ItemStack stack) {
        List<String> ids = new ArrayList<>();
        if (stack.hasTagCompound()) {
            NBTTagList list = stack.getTagCompound().getTagList(ABSORBED, Constants.NBT.TAG_STRING);
            for (int i = 0; i < list.tagCount(); i++) {
                ids.add(list.getStringTagAt(i));
            }
        }
        // Regulators charged before the list existed could only have held Eternity.
        if (ids.isEmpty() && isCharged(stack)) {
            ids.add(ETERNITY);
        }
        return ids;
    }

    /** Adds a being to what the Regulator holds. */
    public static void absorb(ItemStack stack, String id) {
        List<String> ids = absorbed(stack);
        if (ids.contains(id)) {
            return;
        }
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (String held : ids) {
            list.appendTag(new NBTTagString(held));
        }
        list.appendTag(new NBTTagString(id));
        tag.setBoolean(CHARGED, true);
        tag.setTag(ABSORBED, list);
        stack.setTagCompound(tag);
    }

    /**
     * The Regulator set in the chest of the Destroyer Armor this entity pilots, or nothing: it counts as held, so
     * its powers, its endurance and its reach are theirs while they are inside.
     */
    public static ItemStack slotted(@Nullable Entity entity) {
        EntityDestroyerArmor armor = EntityDestroyerArmor.carrier(entity);
        return armor == null ? ItemStack.EMPTY : armor.lentRegulator(entity);
    }

    /** The Regulator to raise for someone: the one in this hand, else the other, else the one in their armour's chest. */
    public static ItemStack raised(EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!(stack.getItem() instanceof ItemAstralRegulator)) {
            stack = player.getHeldItem(hand == EnumHand.MAIN_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND);
        }
        return stack.getItem() instanceof ItemAstralRegulator ? stack : slotted(player);
    }

    /** Whether this entity holds a charged Regulator in either hand, or in the chest of the armour they pilot. */
    public static boolean holdsCharged(@Nullable Entity entity) {
        if (!(entity instanceof EntityLivingBase)) {
            return false;
        }
        EntityLivingBase living = (EntityLivingBase) entity;
        return isChargedRegulator(living.getHeldItemMainhand()) || isChargedRegulator(living.getHeldItemOffhand()) || isChargedRegulator(slotted(entity));
    }

    private static boolean isChargedRegulator(ItemStack stack) {
        return stack.getItem() instanceof ItemAstralRegulator && isCharged(stack);
    }

    /** Whether this entity holds a Regulator, empty or not, in either hand. */
    public static boolean holdsRegulator(@Nullable Entity entity) {
        if (!(entity instanceof EntityLivingBase)) {
            return false;
        }
        EntityLivingBase living = (EntityLivingBase) entity;
        return living.getHeldItemMainhand().getItem() instanceof ItemAstralRegulator
                || living.getHeldItemOffhand().getItem() instanceof ItemAstralRegulator || !slotted(entity).isEmpty();
    }

    /** Whether this entity holds, in either hand, a Regulator with this being inside. */
    public static boolean holds(@Nullable Entity entity, String id) {
        if (!(entity instanceof EntityLivingBase)) {
            return false;
        }
        EntityLivingBase living = (EntityLivingBase) entity;
        return holdsIn(living.getHeldItemMainhand(), id) || holdsIn(living.getHeldItemOffhand(), id) || holdsIn(slotted(entity), id);
    }

    public static boolean holdsIn(ItemStack stack, String id) {
        return stack.getItem() instanceof ItemAstralRegulator && absorbed(stack).contains(id);
    }

    /** A Regulator that has drunk in Eternity. */
    public static ItemStack charged() {
        return charged(new ArrayList<>());
    }

    /** A Regulator that has drunk in Eternity, on top of whatever it already held. */
    public static ItemStack charged(List<String> already) {
        ItemStack stack = new ItemStack(EternityItems.ASTRAL_REGULATOR);
        for (String id : already) {
            absorb(stack, id);
        }
        absorb(stack, ETERNITY);
        return stack;
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (isInCreativeTab(tab)) {
            items.add(new ItemStack(this));
            items.add(charged());
        }
    }

    /** Pale and cold when empty; its name burns purple once it holds something. */
    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return isCharged(stack) ? EnumRarity.EPIC : EnumRarity.RARE;
    }

    // ------------------------------------------------------------------ powers

    @Override
    public Ability.AbilityMap addDefaultAbilities(EntityLivingBase entity, Ability.AbilityMap abilities, Ability.EnumAbilityContext context) {
        ItemStack stack = context == Ability.EnumAbilityContext.OFF_HAND ? entity.getHeldItemOffhand() : entity.getHeldItemMainhand();
        if (stack.getItem() == this && isCharged(stack) && absorbed(stack).contains(ETERNITY)) {
            EternityAbilities.grantEternity(entity, abilities);
        }
        if (stack.getItem() == this && isCharged(stack) && absorbed(stack).contains(TRIBUNAL)) {
            TribunalAbilities.grant(entity, abilities);
        }
        if (stack.getItem() == this && isCharged(stack)) {
            List<String> held = absorbed(stack);
            if (held.contains(ORDER)) {
                MastersAbilities.grantOrder(entity, abilities);
            }
            if (held.contains(CHAOS)) {
                MastersAbilities.grantChaos(entity, abilities);
            }
            if (held.contains(INBETWEENER)) {
                MastersAbilities.grantInBetweener(entity, abilities);
            }
        }
        return abilities;
    }

    // ------------------------------------------------------------------ containment

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        return new ActionResult<>(release(world, player, stack, hand), stack);
    }

    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase target, EnumHand hand) {
        return (target instanceof EntityEternity || target instanceof EntityLivingTribunal || target instanceof EntityOrderChaos
                || target instanceof EntityInBetweener)
                && release(player.world, player, stack, hand) == EnumActionResult.SUCCESS;
    }

    /**
     * Empty or not, it drinks in whichever of them it is raised at, in any order: Eternity as he
     * dies, the Living Tribunal, Master Order or Lord Chaos while they lie beaten, the
     * In-Betweener while he lies fallen outside his glass.
     */
    private static EnumActionResult release(World world, EntityPlayer player, ItemStack stack, EnumHand hand) {
        if (world.isRemote) {
            List<String> held = absorbed(stack);
            boolean any = !held.contains(ETERNITY)
                    || !held.contains(TRIBUNAL) && EntityLivingTribunal.downedNear(player) || EntityOrderChaos.downedNear(player)
                    || !held.contains(INBETWEENER) && EntityInBetweener.fallenNear(player) || EntityOrderChaos.standingBefore(player);
            return any ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
        }
        return player instanceof EntityPlayerMP ? raise((EntityPlayerMP) player, stack, hand) : EnumActionResult.PASS;
    }

    /**
     * The Regulator raised, on the server: from a hand, or from the chest of the Destroyer Armor its pilot is inside
     * (see {@link #slotted}); it drinks in whichever being it can.
     */
    public static EnumActionResult raise(EntityPlayerMP mp, ItemStack stack, EnumHand hand) {
        EntityPlayer player = mp;
        List<String> held = absorbed(stack);
        // Eternity it takes by flying into his heart; it comes back holding him and everything it held before.
        if (!held.contains(ETERNITY) && Director.INSTANCE.tryContain(mp, held)) {
            if (!player.capabilities.isCreativeMode) {
                stack.shrink(1);
            }
            return EnumActionResult.SUCCESS;
        }
        boolean took = !held.contains(TRIBUNAL) && TribunalDirector.INSTANCE.tryAbsorb(mp, hand)
                || OrderAndChaos.INSTANCE.tryAbsorb(mp, hand)
                || !held.contains(INBETWEENER) && InBetweenerDirector.INSTANCE.tryAbsorb(mp, hand)
                // Raised at one of them still standing, it takes nothing, but they notice.
                || OrderAndChaos.INSTANCE.regulatorRaised(mp);
        return took ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
    }

    // ------------------------------------------------------------------ energy

    /**
     * Forge Energy it gives each tick. Even empty it hums with power; every being it
     * holds pours far more out through it.
     */
    public static int energyPerTick(ItemStack stack) {
        long rate = 10_000L;
        List<String> ids = absorbed(stack);
        if (ids.contains(ETERNITY)) {
            rate += 1_000_000L;
        }
        if (ids.contains(TRIBUNAL)) {
            rate += 100_000_000L;
        }
        if (ids.contains(ORDER)) {
            rate += 250_000_000L;
        }
        if (ids.contains(CHAOS)) {
            rate += 250_000_000L;
        }
        if (ids.contains(INBETWEENER)) {
            rate += 500_000_000L;
        }
        return (int) Math.min(Integer.MAX_VALUE, rate);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new RegulatorEnergy(stack);
    }

    /** Carried anywhere in an inventory, it keeps everything else there that runs on Forge Energy full. */
    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isRemote || !(entity instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) entity;
        int budget = energyPerTick(stack);
        for (int i = 0; i < player.inventory.getSizeInventory() && budget > 0; i++) {
            ItemStack other = player.inventory.getStackInSlot(i);
            if (other.isEmpty() || other == stack || other.getItem() == this) {
                continue;
            }
            IEnergyStorage storage = other.getCapability(CapabilityEnergy.ENERGY, null);
            if (storage != null && storage.canReceive()) {
                budget -= storage.receiveEnergy(budget, false);
            }
        }
    }

    /** Only shown for an empty Regulator; a charged one draws its own tooltip (see client.RegulatorTooltip). */
    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        if (!isCharged(stack)) {
            tooltip.add(TextFormatting.DARK_AQUA + "" + TextFormatting.ITALIC + "Built to hold what should never be released.");
            tooltip.add(TextFormatting.GOLD + formatEnergy(energyPerTick(stack)) + " FE/t");
        }
    }

    /** Energy in the shortest readable form: 10k, 1.01M, 101M. */
    public static String formatEnergy(long fe) {
        if (fe >= 1_000_000_000L) {
            return String.format("%.2fB", fe / 1.0E9D);
        }
        if (fe >= 1_000_000L) {
            return String.format(fe >= 100_000_000L ? "%.0fM" : "%.2fM", fe / 1.0E6D);
        }
        if (fe >= 1_000L) {
            return String.format("%.0fk", fe / 1.0E3D);
        }
        return String.valueOf(fe);
    }
}
