package com.matoon.eternity.item;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.ability.EternityAbilities;
import com.matoon.eternity.ability.TribunalAbilities;
import com.matoon.eternity.entity.EntityEternity;
import com.matoon.eternity.entity.EntityLivingTribunal;
import com.matoon.eternity.world.Director;
import com.matoon.eternity.world.TribunalDirector;
import lucraft.mods.lucraftcore.infinity.items.ItemIndestructible;
import lucraft.mods.lucraftcore.superpowers.abilities.Ability;
import lucraft.mods.lucraftcore.superpowers.abilities.supplier.IAbilityProvider;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
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
 * he dies, it drinks his escaping energy back in and saves the universe. Afterwards
 * it is charged: its name burns purple, it remembers what it absorbed, and whoever
 * holds it can wield those powers as Lucraft abilities. Like the stones, it cannot
 * be destroyed.
 */
public class ItemAstralRegulator extends ItemIndestructible implements IAbilityProvider {

    public static final String ETERNITY = Eternity.MODID + ":eternity";
    public static final String TRIBUNAL = Eternity.MODID + ":living_tribunal";

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

    /** Whether this entity holds a charged Regulator in either hand. */
    public static boolean holdsCharged(@Nullable Entity entity) {
        if (!(entity instanceof EntityLivingBase)) {
            return false;
        }
        EntityLivingBase living = (EntityLivingBase) entity;
        return isChargedRegulator(living.getHeldItemMainhand()) || isChargedRegulator(living.getHeldItemOffhand());
    }

    private static boolean isChargedRegulator(ItemStack stack) {
        return stack.getItem() instanceof ItemAstralRegulator && isCharged(stack);
    }

    /** A Regulator that has drunk in Eternity. */
    public static ItemStack charged() {
        ItemStack stack = new ItemStack(EternityItems.ASTRAL_REGULATOR);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean(CHARGED, true);
        NBTTagList list = new NBTTagList();
        list.appendTag(new NBTTagString(ETERNITY));
        tag.setTag(ABSORBED, list);
        stack.setTagCompound(tag);
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
        return (target instanceof EntityEternity || target instanceof EntityLivingTribunal)
                && release(player.world, player, stack, hand) == EnumActionResult.SUCCESS;
    }

    private static EnumActionResult release(World world, EntityPlayer player, ItemStack stack, EnumHand hand) {
        if (isCharged(stack)) {
            // Charged, it can drink in one thing more: the Living Tribunal, while he lies beaten.
            if (absorbed(stack).contains(TRIBUNAL)) {
                return EnumActionResult.PASS;
            }
            if (world.isRemote) {
                return EntityLivingTribunal.downedNear(player) ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
            }
            return player instanceof EntityPlayerMP && TribunalDirector.INSTANCE.tryAbsorb((EntityPlayerMP) player, hand)
                    ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
        }
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }
        if (player instanceof EntityPlayerMP && Director.INSTANCE.tryContain((EntityPlayerMP) player)) {
            if (!player.capabilities.isCreativeMode) {
                stack.shrink(1);
            }
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.FAIL;
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
