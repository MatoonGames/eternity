package com.matoon.eternity.client;

import com.matoon.eternity.network.MsgRelicTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * The Relic Tracker's needle. Same physics as the vanilla compass, but it points at
 * the relic the server found; with nothing in this dimension to find, it spins.
 */
@SideOnly(Side.CLIENT)
public class RelicTrackerAngle implements IItemPropertyGetter {

    private static boolean hasTarget;
    private static double targetX, targetZ;
    private static int targetDimension;
    private static String targetItem = "";

    private double rotation;
    private double velocity;
    private long lastUpdateTick;

    public static void receive(MsgRelicTarget message) {
        hasTarget = message.found;
        targetX = message.x;
        targetZ = message.z;
        targetItem = message.item;
        targetDimension = Minecraft.getMinecraft().world == null ? 0 : Minecraft.getMinecraft().world.provider.getDimension();
    }

    /** The name of the relic the needle is drawn to in this dimension, or null with nothing to find. */
    @Nullable
    public static String targetName() {
        World world = Minecraft.getMinecraft().world;
        if (!hasTarget || world == null || world.provider.getDimension() != targetDimension) {
            return null;
        }
        Item item = Item.getByNameOrId(targetItem);
        return item == null ? null : new ItemStack(item).getDisplayName();
    }

    @Override
    public float apply(ItemStack stack, @Nullable World world, @Nullable EntityLivingBase holder) {
        if (holder == null && !stack.isOnItemFrame()) {
            return 0F;
        }
        boolean held = holder != null;
        Entity entity = held ? holder : stack.getItemFrame();
        if (entity == null) {
            return 0F;
        }
        if (world == null) {
            world = entity.world;
        }
        double angle;
        if (hasTarget && world.provider.getDimension() == targetDimension) {
            double facing = held ? entity.rotationYaw : frameRotation((EntityItemFrame) entity);
            facing = MathHelper.positiveModulo(facing / 360D, 1D);
            double toTarget = Math.atan2(targetZ - entity.posZ, targetX - entity.posX) / (Math.PI * 2D);
            angle = 0.5D - (facing - 0.25D - toTarget);
        } else {
            angle = Math.random();
        }
        if (held) {
            angle = wobble(world, angle);
        }
        return MathHelper.positiveModulo((float) angle, 1F);
    }

    private double wobble(World world, double target) {
        if (world.getTotalWorldTime() != lastUpdateTick) {
            lastUpdateTick = world.getTotalWorldTime();
            double delta = MathHelper.positiveModulo(target - rotation + 0.5D, 1D) - 0.5D;
            velocity += delta * 0.1D;
            velocity *= 0.8D;
            rotation = MathHelper.positiveModulo(rotation + velocity, 1D);
        }
        return rotation;
    }

    private static double frameRotation(EntityItemFrame frame) {
        return MathHelper.wrapDegrees(180 + frame.facingDirection.getHorizontalIndex() * 90);
    }
}
