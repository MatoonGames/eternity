package com.matoon.eternity.item;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgTribunalFx;
import com.matoon.eternity.world.Nexus;
import com.matoon.eternity.world.OdinsVault;
import com.matoon.eternity.world.StarChamber;
import lucraft.mods.lucraftcore.infinity.items.ItemIndestructible;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentFrostWalker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * The Casket of Ancient Winters, the source of the Frost Giants' power, taken from Jotunheim by
 * Odin and kept in his vault on the Pedestal of Fimbulvetr. It is real, and it works.
 * <ul>
 * <li><b>Use</b>: the winter inside pours out ahead of you in a gale. Everything caught in it is
 * frozen where it stands; water ices over, lava crusts to stone, snow settles, fires go out,
 * and spikes of ice erupt from the ground.</li>
 * <li><b>Sneak and use</b>: it bursts out all round you at once, a ring of ice and a deeper freeze.</li>
 * <li>Held, water freezes under your feet.</li>
 * </ul>
 * Nothing it does changes the world inside Odin's Vault, the Star Chamber or the Nexus, nor
 * anywhere at all with {@code casketChangesTerrain} off. Like the stones, it cannot be destroyed.
 */
public class ItemCasket extends ItemIndestructible {

    private static final int GALE_COOLDOWN = 30, BURST_COOLDOWN = 140;
    private static final double GALE_LENGTH = 18D, GALE_SPREAD = 0.42D, BURST_RADIUS = 9D;
    /** How long anything it catches stays frozen, in ticks. */
    public static final int FREEZE_TICKS = 100;

    private final Random random = new Random();

    public ItemCasket() {
        setRegistryName(Eternity.MODID, "casket_of_ancient_winters");
        setTranslationKey(Eternity.MODID + ".casket_of_ancient_winters");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.COMBAT);
        setEntitySize(0.6F, 0.5F);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.AQUA + "The source of the Frost Giants' power.");
        tooltip.add(TextFormatting.GRAY + "Use: a gale of ancient winter");
        tooltip.add(TextFormatting.GRAY + "Sneak and use: winter bursts out all round you");
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        boolean burst = player.isSneaking();
        Vec3d at = new Vec3d(player.posX, player.posY + player.getEyeHeight() - 0.35D, player.posZ);
        Vec3d look = player.getLookVec();
        if (burst) {
            burst(world, player, at);
        } else {
            gale(world, player, at.add(look.scale(0.6D)), look);
        }
        player.getCooldownTracker().setCooldown(this, burst ? BURST_COOLDOWN : GALE_COOLDOWN);
        player.swingArm(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    /** The winter inside pours out ahead in a widening cone. */
    private void gale(World world, EntityPlayer player, Vec3d from, Vec3d dir) {
        EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.CASKET_WINTER, from.x, from.y, from.z, dir.x, dir.y, dir.z, 0, 24),
                world.provider.getDimension());
        world.playSound(null, from.x, from.y, from.z, SoundEvents.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.PLAYERS, 0.6F, 0.3F);
        world.playSound(null, from.x, from.y, from.z, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.2F, 0.5F);
        world.playSound(null, from.x, from.y, from.z, SoundEvents.ENTITY_ENDERDRAGON_FLAP, SoundCategory.PLAYERS, 1.5F, 0.6F);
        AxisAlignedBB box = new AxisAlignedBB(from.x, from.y, from.z, from.x, from.y, from.z).grow(GALE_LENGTH);
        for (EntityLivingBase victim : world.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            if (victim == player) {
                continue;
            }
            Vec3d to = new Vec3d(victim.posX, victim.posY + victim.height * 0.5D, victim.posZ).subtract(from);
            double along = to.dotProduct(dir);
            if (along > 0D && along < GALE_LENGTH && to.subtract(dir.scale(along)).length() < 1D + along * GALE_SPREAD) {
                freeze(victim, player, 6F, dir.scale(0.5D));
            }
        }
        if (changesTerrain(world)) {
            for (int i = 0; i < 140; i++) {
                double along = 1.5D + random.nextDouble() * (GALE_LENGTH - 1.5D);
                double r = random.nextDouble() * (1D + along * GALE_SPREAD);
                double a = random.nextDouble() * Math.PI * 2D;
                Vec3d[] basis = basis(dir);
                Vec3d p = from.add(dir.scale(along)).add(basis[0].scale(Math.cos(a) * r)).add(basis[1].scale(Math.sin(a) * r));
                winter(world, new BlockPos(p), random.nextInt(9) == 0);
            }
        }
    }

    /** It bursts out all round at once. */
    private void burst(World world, EntityPlayer player, Vec3d at) {
        EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.CASKET_WINTER, at.x, player.posY, at.z, BURST_RADIUS, 0D, 0D, 1, 30),
                world.provider.getDimension());
        world.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.5F, 0.5F);
        world.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 2F, 0.4F);
        for (EntityLivingBase victim : world.getEntitiesWithinAABB(EntityLivingBase.class, player.getEntityBoundingBox().grow(BURST_RADIUS))) {
            double d = victim.getDistance(player);
            if (victim != player && d < BURST_RADIUS) {
                Vec3d away = new Vec3d(victim.posX - player.posX, 0D, victim.posZ - player.posZ);
                freeze(victim, player, 10F, away.lengthSquared() < 1.0E-4 ? Vec3d.ZERO : away.normalize().scale(0.8D));
            }
        }
        if (changesTerrain(world)) {
            for (int i = 0; i < 220; i++) {
                double r = 1.5D + Math.sqrt(random.nextDouble()) * (BURST_RADIUS - 1.5D);
                double a = random.nextDouble() * Math.PI * 2D;
                BlockPos pos = new BlockPos(at.x + Math.cos(a) * r, player.posY + 2D, at.z + Math.sin(a) * r);
                winter(world, pos, r > BURST_RADIUS * 0.55D && random.nextInt(4) == 0);
            }
        }
    }

    /** Frozen where it stands: bitter cold, and it can barely move for five seconds. */
    private static void freeze(EntityLivingBase victim, EntityPlayer by, float damage, Vec3d push) {
        victim.attackEntityFrom(new EntityDamageSource("eternity.casket", by).setDamageBypassesArmor(), damage);
        victim.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, FREEZE_TICKS, 6, false, false));
        victim.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, FREEZE_TICKS, 3, false, false));
        victim.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, FREEZE_TICKS, 1, false, false));
        victim.extinguish();
        victim.motionX = push.x;
        victim.motionZ = push.z;
        victim.velocityChanged = true;
        EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.CASKET_FREEZE, victim.posX, victim.posY, victim.posZ, victim.width,
                victim.height, 0D, victim.getEntityId(), FREEZE_TICKS), victim.dimension);
    }

    /**
     * Winter comes to one spot: to the ground under it, whatever that is. Water ices over, lava
     * crusts, fire goes out, snow settles; and sometimes a spike of ice erupts from the ground.
     */
    private void winter(World world, BlockPos start, boolean spike) {
        BlockPos pos = start;
        for (int i = 0; i < 6 && pos.getY() > 1; i++) {
            IBlockState state = world.getBlockState(pos);
            if (!world.isAirBlock(pos) && state.getMaterial() != Material.FIRE && !state.getMaterial().isReplaceable()) {
                break;
            }
            pos = pos.down();
        }
        IBlockState ground = world.getBlockState(pos);
        BlockPos above = pos.up();
        if (ground.getMaterial() == Material.WATER) {
            world.setBlockState(pos, Blocks.ICE.getDefaultState());
            return;
        }
        if (ground.getMaterial() == Material.LAVA) {
            world.setBlockState(pos, ground.getBlock() == Blocks.LAVA && ground.getValue(net.minecraft.block.BlockLiquid.LEVEL) == 0
                    ? Blocks.OBSIDIAN.getDefaultState() : Blocks.COBBLESTONE.getDefaultState());
            return;
        }
        if (!ground.isSideSolid(world, pos, net.minecraft.util.EnumFacing.UP)) {
            return;
        }
        IBlockState over = world.getBlockState(above);
        if (over.getMaterial() == Material.FIRE) {
            world.setBlockToAir(above);
        }
        if (spike) {
            int height = 1 + random.nextInt(3);
            for (int k = 0; k < height; k++) {
                BlockPos p = above.up(k);
                if (!world.isAirBlock(p) && !world.getBlockState(p).getMaterial().isReplaceable()) {
                    break;
                }
                world.setBlockState(p, (k == height - 1 ? Blocks.ICE : Blocks.PACKED_ICE).getDefaultState());
            }
        } else if (world.isAirBlock(above) || over.getBlock() == Blocks.SNOW_LAYER || over.getMaterial().isReplaceable() && over.getMaterial() != Material.WATER) {
            int layers = over.getBlock() == Blocks.SNOW_LAYER ? Math.min(8, over.getValue(net.minecraft.block.BlockSnow.LAYERS) + 1) : 1 + random.nextInt(2);
            world.setBlockState(above, Blocks.SNOW_LAYER.getDefaultState().withProperty(net.minecraft.block.BlockSnow.LAYERS, layers));
        }
    }

    private static boolean changesTerrain(World world) {
        int dim = world.provider.getDimension();
        return EternityConfig.vault.casketChangesTerrain && dim != OdinsVault.dimension() && dim != StarChamber.dimension()
                && dim != Nexus.dimension();
    }

    /** Held, it freezes the water under your feet. */
    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isRemote || !(entity instanceof EntityLivingBase) || !changesTerrain(world)) {
            return;
        }
        EntityLivingBase holder = (EntityLivingBase) entity;
        if (holder.getHeldItemMainhand() == stack || holder.getHeldItemOffhand() == stack) {
            EnchantmentFrostWalker.freezeNearby(holder, world, new BlockPos(holder), 1);
        }
    }

    private static Vec3d[] basis(Vec3d d) {
        Vec3d helper = Math.abs(d.y) > 0.9D ? new Vec3d(1D, 0D, 0D) : new Vec3d(0D, 1D, 0D);
        Vec3d u = d.crossProduct(helper).normalize();
        return new Vec3d[]{u, d.crossProduct(u).normalize()};
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }
}
