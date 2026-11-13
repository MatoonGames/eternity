package com.matoon.eternity.item;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.EternityTab;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgTribunalFx;
import com.matoon.eternity.world.Fimbulwinter;
import com.matoon.eternity.world.Nexus;
import com.matoon.eternity.world.OdinsVault;
import com.matoon.eternity.world.StarChamber;
import lucraft.mods.lucraftcore.infinity.items.ItemIndestructible;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.EnchantmentFrostWalker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumAction;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
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
 * <li><b>Hold use</b>: the lid comes up and the winter inside pours out ahead of you in a torrent
 * that does not stop while you hold it. Everything caught in it is frozen solid; water ices over,
 * lava crusts to stone, fires go out, snow buries the ground and spikes of ice erupt from it. The
 * longer it is held open the further it reaches and the harder it bites, and the winter spreads
 * round you too, a blizzard of your own.</li>
 * <li><b>Release when full</b> (after {@link #GATHER_TICKS}): the Fimbulwinter. Everything it held is
 * let out at once, a wall of winter racing out across the land for {@code casketWinterRadius}
 * blocks, freezing everything in its path, burying the world in snow, raising glaciers, and
 * leaving it in an ice age under a blizzard sky.</li>
 * <li><b>Sneak and use</b>: it bursts out all round you at once, a ring of ice and a deeper freeze.</li>
 * <li>Held, water freezes under your feet.</li>
 * <li>The Gardener's hedge, which nothing else short of the Power Stone gets through, freezes through
 * wherever it reaches, and frozen, breaks like glass.</li>
 * </ul>
 * Nothing it does changes the world inside Odin's Vault, the Star Chamber or the Nexus, nor
 * anywhere at all with {@code casketChangesTerrain} or {@code destroyBlocks} off. Like the stones,
 * it cannot be destroyed.
 */
public class ItemCasket extends ItemIndestructible {

    /** Ticks it must be held open before the winter in it is full, and can be let out. */
    public static final int GATHER_TICKS = 80;
    /** How far the torrent reaches, just opened and full. */
    public static final double REACH = 13D, REACH_FULL = 34D;
    private static final int BURST_COOLDOWN = 140, WINTER_COOLDOWN = 400;
    private static final double BURST_RADIUS = 12D;
    /** How long anything it catches stays frozen, in ticks. */
    public static final int FREEZE_TICKS = 100;

    private final Random random = new Random();

    public ItemCasket() {
        setRegistryName(Eternity.MODID, "casket_of_ancient_winters");
        setTranslationKey(Eternity.MODID + ".casket_of_ancient_winters");
        setMaxStackSize(1);
        setCreativeTab(EternityTab.INSTANCE);
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
        tooltip.add(TextFormatting.GRAY + "Hold use: a torrent of ancient winter");
        tooltip.add(TextFormatting.GRAY + "Release when full: " + TextFormatting.WHITE + "the Fimbulwinter");
        tooltip.add(TextFormatting.GRAY + "Sneak and use: winter bursts out all round you");
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.NONE;
    }

    /** How full the winter in it is, 0 to 1, while someone holds it open. */
    public static float gathered(EntityLivingBase holder, float partialTicks) {
        if (!holder.isHandActive() || !(holder.getActiveItemStack().getItem() instanceof ItemCasket)) {
            return 0F;
        }
        return MathHelper.clamp((holder.getItemInUseMaxCount() + partialTicks) / GATHER_TICKS, 0F, 1F);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSneaking()) {
            if (!world.isRemote) {
                burst(world, player, new Vec3d(player.posX, player.posY + player.getEyeHeight() - 0.35D, player.posZ));
                player.getCooldownTracker().setCooldown(this, BURST_COOLDOWN);
            }
            player.swingArm(hand);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        player.setActiveHand(hand);
        if (!world.isRemote) {
            world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.PLAYERS, 1F, 0.5F);
            world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.8F, 0.4F);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    /** Held open: the torrent pours out, every tick, harder the longer it is held. */
    @Override
    public void onUsingTick(ItemStack stack, EntityLivingBase holder, int count) {
        World world = holder.world;
        if (world.isRemote || !(holder instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) holder;
        int held = getMaxItemUseDuration(stack) - count;
        float full = MathHelper.clamp(held / (float) GATHER_TICKS, 0F, 1F);
        Vec3d look = player.getLookVec();
        Vec3d from = new Vec3d(player.posX, player.posY + player.getEyeHeight() - 0.35D, player.posZ).add(look.scale(0.6D));
        torrent(world, player, from, look, held, full);
        if (held == GATHER_TICKS) {
            // Full: the casket can hold no more.
            world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.PLAYERS, 1.2F, 0.6F);
            world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 0.6F, 1.6F);
        }
        player.extinguish();
    }

    /** Let go: if the winter in it was full, all of it goes out at once. */
    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase holder, int timeLeft) {
        if (world.isRemote || !(holder instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) holder;
        int held = getMaxItemUseDuration(stack) - timeLeft;
        world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_CHEST_CLOSE, SoundCategory.PLAYERS, 1F, 0.5F);
        if (held >= GATHER_TICKS) {
            Fimbulwinter.INSTANCE.unleash(player);
            player.getCooldownTracker().setCooldown(this, WINTER_COOLDOWN);
        } else {
            player.getCooldownTracker().setCooldown(this, 10);
        }
    }

    /**
     * One tick of the torrent: a cone of winter ahead, reaching further and spreading wider as it
     * fills, and once it is well open, a blizzard round whoever holds it.
     */
    private void torrent(World world, EntityPlayer player, Vec3d from, Vec3d dir, int held, float full) {
        double reach = REACH + (REACH_FULL - REACH) * full;
        double spread = 0.16D + 0.16D * full;
        if (held % 5 == 0) {
            world.playSound(null, from.x, from.y, from.z, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.35F + 0.4F * full, 0.3F + random.nextFloat() * 0.2F);
        }
        if (held % 10 == 0) {
            AxisAlignedBB box = new AxisAlignedBB(from.x, from.y, from.z, from.x, from.y, from.z).grow(reach);
            for (EntityLivingBase victim : world.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
                if (victim == player) {
                    continue;
                }
                Vec3d to = new Vec3d(victim.posX, victim.posY + victim.height * 0.5D, victim.posZ).subtract(from);
                double along = to.dotProduct(dir);
                if (along > 0D && along < reach && to.subtract(dir.scale(along)).length() < 1.2D + along * spread) {
                    freeze(victim, player, 3F + 9F * full, dir.scale(0.25D + 0.35D * full), FREEZE_TICKS);
                }
            }
            com.matoon.eternity.story.StoryDirector.INSTANCE.frostHedge(player, from, dir, reach, spread, 0D);
        }
        // A blizzard round the holder, once it is well open: the cold slows everything near them.
        double storm = full < 0.3F ? 0D : 3D + 13D * (full - 0.3D) / 0.7D;
        if (storm > 0D && held % 20 == 0) {
            for (EntityLivingBase near : world.getEntitiesWithinAABB(EntityLivingBase.class, player.getEntityBoundingBox().grow(storm))) {
                if (near != player && near.getDistance(player) < storm) {
                    near.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 40, 2, false, false));
                    near.extinguish();
                }
            }
        }
        if (!changesTerrain(world)) {
            return;
        }
        Vec3d[] basis = basis(dir);
        int count = 5 + (int) (20 * full);
        for (int i = 0; i < count; i++) {
            double along = 1.5D + random.nextDouble() * (reach - 1.5D);
            double r = random.nextDouble() * (1.2D + along * spread);
            double a = random.nextDouble() * Math.PI * 2D;
            Vec3d p = from.add(dir.scale(along)).add(basis[0].scale(Math.cos(a) * r)).add(basis[1].scale(Math.sin(a) * r));
            // Spikes only well out ahead, never round the holder's own feet.
            winter(world, new BlockPos(p), along > 7D && random.nextInt(40) == 0);
        }
        // Where it lands, the cold goes deep.
        RayTraceResult hit = world.rayTraceBlocks(from, from.add(dir.scale(reach)), true, true, false);
        if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK) {
            double splash = 1.5D + 3D * full;
            boolean far = hit.hitVec.distanceTo(from) > 7D;
            for (int i = 0; i < 4 + (int) (8 * full); i++) {
                BlockPos p = hit.getBlockPos().add(random.nextGaussian() * splash, 2D, random.nextGaussian() * splash);
                winter(world, p, far && random.nextInt(12) == 0);
            }
        }
        for (int i = 0; storm > 0D && i < 2 + (int) (6 * full); i++) {
            double a = random.nextDouble() * Math.PI * 2D, r = 1.5D + random.nextDouble() * storm;
            winter(world, new BlockPos(player.posX + Math.cos(a) * r, player.posY + 3D, player.posZ + Math.sin(a) * r), false);
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
                freeze(victim, player, 12F, away.lengthSquared() < 1.0E-4 ? Vec3d.ZERO : away.normalize().scale(0.9D), FREEZE_TICKS);
            }
        }
        com.matoon.eternity.story.StoryDirector.INSTANCE.frostHedge(player, at, Vec3d.ZERO, 0D, 0D, BURST_RADIUS);
        if (changesTerrain(world)) {
            for (int i = 0; i < 320; i++) {
                double r = 1.5D + Math.sqrt(random.nextDouble()) * (BURST_RADIUS - 1.5D);
                double a = random.nextDouble() * Math.PI * 2D;
                BlockPos pos = new BlockPos(at.x + Math.cos(a) * r, player.posY + 2D, at.z + Math.sin(a) * r);
                winter(world, pos, r > BURST_RADIUS * 0.5D && random.nextInt(4) == 0);
            }
        }
    }

    /** Frozen where it stands: bitter cold, and it can barely move until the ice lets it go. */
    public static void freeze(EntityLivingBase victim, EntityLivingBase by, float damage, Vec3d push, int ticks) {
        PotionEffect slow = victim.getActivePotionEffect(MobEffects.SLOWNESS);
        boolean already = slow != null && slow.getAmplifier() >= 6 && slow.getDuration() > 30;
        victim.attackEntityFrom(new EntityDamageSource("eternity.casket", by).setDamageBypassesArmor(), damage);
        victim.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, ticks, 6, false, false));
        victim.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, ticks, 3, false, false));
        victim.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, ticks, 1, false, false));
        victim.extinguish();
        victim.motionX = push.x;
        victim.motionY = Math.max(victim.motionY, push.y);
        victim.motionZ = push.z;
        victim.velocityChanged = true;
        if (!already) {
            EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.CASKET_FREEZE, victim.posX, victim.posY, victim.posZ, victim.width,
                    victim.height, 0D, victim.getEntityId(), ticks), victim.dimension);
        }
    }

    /**
     * Winter comes to one spot: to the ground under it, whatever that is. Water ices over, lava
     * crusts, fire goes out, snow settles; and sometimes a spike of ice erupts from the ground.
     */
    public void winter(World world, BlockPos start, boolean spike) {
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
            world.setBlockState(pos, (spike ? Blocks.PACKED_ICE : Blocks.ICE).getDefaultState());
            return;
        }
        if (ground.getMaterial() == Material.LAVA) {
            world.setBlockState(pos, ground.getBlock() == Blocks.LAVA && ground.getValue(net.minecraft.block.BlockLiquid.LEVEL) == 0
                    ? Blocks.OBSIDIAN.getDefaultState() : Blocks.COBBLESTONE.getDefaultState());
            return;
        }
        if (!ground.isSideSolid(world, pos, EnumFacing.UP) && ground.getMaterial() != Material.LEAVES) {
            return;
        }
        IBlockState over = world.getBlockState(above);
        if (over.getMaterial() == Material.FIRE) {
            world.setBlockToAir(above);
        }
        if (spike && ground.getMaterial() != Material.LEAVES) {
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

    public static boolean changesTerrain(World world) {
        int dim = world.provider.getDimension();
        return EternityConfig.destroyBlocks && EternityConfig.vault.casketChangesTerrain && dim != OdinsVault.dimension() && dim != StarChamber.dimension()
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
