package com.matoon.eternity.item;

import com.google.common.collect.Multimap;
import com.matoon.eternity.Eternity;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgTribunalFx;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * An Einherjar's spear: a long dark shaft bound in gold, a leaf blade, a spike at its butt. It
 * reaches further than a sword ({@link #REACH} blocks), and held back with use it is driven forward
 * with your whole weight behind it, the way they fight: you lunge, and everything along the
 * spear's length in front of you is run through and thrown back.
 */
public class ItemEinherjarSpear extends Item {

    /** How far its point reaches when you strike with it. */
    public static final double REACH = 5D;
    /** How long it must be drawn back before a thrust, and how long until it is at full strength. */
    private static final int READY = 6, FULL = 18;
    private static final float DAMAGE = 9F;

    public ItemEinherjarSpear() {
        setRegistryName(Eternity.MODID, "einherjar_spear");
        setTranslationKey(Eternity.MODID + ".einherjar_spear");
        setCreativeTab(CreativeTabs.COMBAT);
        setMaxStackSize(1);
        setMaxDamage(1200);
        setFull3D();
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.RARE;
    }

    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {
        Multimap<String, AttributeModifier> modifiers = super.getAttributeModifiers(slot, stack);
        if (slot == EntityEquipmentSlot.MAINHAND) {
            modifiers.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", DAMAGE - 1F, 0));
            modifiers.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", -2.9D, 0));
        }
        return modifiers;
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        stack.damageItem(1, attacker);
        return true;
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World world, IBlockState state, BlockPos pos, EntityLivingBase entity) {
        if (state.getBlockHardness(world, pos) != 0F) {
            stack.damageItem(2, entity);
        }
        return true;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment.type == EnumEnchantmentType.WEAPON || super.canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public int getItemEnchantability() {
        return 14;
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return repair.getItem() == Items.GOLD_INGOT || super.getIsRepairable(toRepair, repair);
    }

    // ------------------------------------------------------------------ the thrust

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.BOW;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (hand != EnumHand.MAIN_HAND) {
            return new ActionResult<>(EnumActionResult.PASS, player.getHeldItem(hand));
        }
        player.setActiveHand(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    /** Let go, and it is driven forward: a lunge, and everything along its length is run through. */
    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase user, int timeLeft) {
        int drawn = getMaxItemUseDuration(stack) - timeLeft;
        if (drawn < READY || !(user instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) user;
        float power = Math.min(1F, (drawn - READY) / (float) (FULL - READY));
        Vec3d look = player.getLookVec();
        Vec3d flat = new Vec3d(look.x, 0D, look.z).normalize();
        if (player.onGround) {
            player.motionX += flat.x * (0.5D + 0.6D * power);
            player.motionZ += flat.z * (0.5D + 0.6D * power);
        }
        world.playSound(null, player.posX, player.posY + 1D, player.posZ, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1F, 1.3F - 0.3F * power);
        player.getCooldownTracker().setCooldown(this, 16);
        if (world.isRemote) {
            return;
        }
        Vec3d from = player.getPositionEyes(1F);
        double reach = REACH + 1.5D * power;
        float damage = (DAMAGE + EnchantmentHelper.getModifierForCreature(stack, EnumCreatureAttribute.UNDEFINED)) * (1F + 0.8F * power);
        boolean struck = false;
        for (EntityLivingBase victim : world.getEntitiesWithinAABB(EntityLivingBase.class, player.getEntityBoundingBox().grow(reach))) {
            if (victim == player || victim.isRidingOrBeingRiddenBy(player) || !victim.isEntityAlive()) {
                continue;
            }
            Vec3d body = new Vec3d(victim.posX, victim.posY + victim.height * 0.5D, victim.posZ);
            Vec3d to = body.subtract(from);
            double along = to.dotProduct(look);
            if (along < 0D || along > reach + victim.width * 0.5D) {
                continue;
            }
            double off = to.subtract(look.scale(along)).length();
            if (off > 0.6D + victim.width * 0.5D || !player.canEntityBeSeen(victim)) {
                continue;
            }
            victim.hurtResistantTime = 0;
            if (victim.attackEntityFrom(DamageSource.causePlayerDamage(player), damage)) {
                victim.knockBack(player, 0.6F + 0.8F * power, -flat.x, -flat.z);
                EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.EINHERJAR_HIT, body.x, body.y, body.z, flat.x, 0D, flat.z,
                        victim.getEntityId(), 0), world.provider.getDimension());
                struck = true;
            }
        }
        if (struck) {
            world.playSound(null, player.posX, player.posY + 1D, player.posZ, SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.2F, 0.9F);
            stack.damageItem(2, player);
        }
        // The line of the thrust, in the air.
        WorldServer server = (WorldServer) world;
        for (int i = 2; i <= MathHelper.ceil(reach * 2D); i++) {
            Vec3d at = from.add(look.scale(i * 0.5D)).add(0D, -0.25D, 0D);
            server.spawnParticle(EnumParticleTypes.CRIT, at.x, at.y, at.z, 1, 0D, 0D, 0D, 0.02D);
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.GOLD + "Carried by the Einherjar, Asgard's palace guard.");
        tooltip.add(TextFormatting.GRAY + "Reaches further than a sword.");
        tooltip.add(TextFormatting.GRAY + "Hold use to draw it back, let go to lunge.");
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    /** Whether someone holds one, for its reach. */
    public static boolean wields(@Nullable Entity entity) {
        return entity instanceof EntityLivingBase && ((EntityLivingBase) entity).getHeldItemMainhand().getItem() instanceof ItemEinherjarSpear;
    }
}
