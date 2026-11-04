package com.matoon.eternity.stones.mind;

import com.matoon.eternity.Eternity;
import lucraft.mods.lucraftcore.superpowers.abilities.AbilityAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** The Mind Stone reaching 32 blocks along its holder's gaze, and taking the first creature or player it finds. */
public class AbilityMindControl extends AbilityAction {

    private static final double RANGE = 32D;
    private static final ResourceLocation ICON = new ResourceLocation(Eternity.MODID, "textures/abilities/mind_control.png");

    public AbilityMindControl(EntityLivingBase entity) {
        super(entity);
        setMaxCooldown(60);
    }

    @Override
    public boolean action() {
        if (!(entity instanceof EntityPlayerMP) || isCoolingdown()) {
            return false;
        }
        EntityPlayerMP owner = (EntityPlayerMP) entity;
        EntityLivingBase target = findTarget(owner);
        if (target == null) {
            return false;
        }
        MindControlManager.control(owner, target);
        // A few points of light along the reach, seen by everyone near.
        Vec3d eyes = owner.getPositionEyes(1F), look = owner.getLook(1F);
        for (int i = 1; i <= 8; i++) {
            Vec3d point = eyes.add(look.scale(RANGE * i / 8D));
            ((WorldServer) owner.world).spawnParticle(EnumParticleTypes.SPELL_MOB, point.x, point.y, point.z, 1, 0.03D, 0.01D, 0.08D, 1D, 165, 60, 255);
        }
        setCooldown(getMaxCooldown());
        owner.world.playSound(null, owner.posX, owner.posY, owner.posZ, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.9F, 1.35F);
        return true;
    }

    private static EntityLivingBase findTarget(EntityPlayerMP owner) {
        Vec3d start = owner.getPositionEyes(1F), end = start.add(owner.getLook(1F).scale(RANGE));
        RayTraceResult block = owner.world.rayTraceBlocks(start, end, false, true, false);
        if (block != null) {
            end = block.hitVec;
        }
        AxisAlignedBB box = owner.getEntityBoundingBox().expand(end.x - start.x, end.y - start.y, end.z - start.z).grow(1D);
        EntityLivingBase best = null;
        double nearest = start.squareDistanceTo(end);
        for (Entity hit : owner.world.getEntitiesWithinAABBExcludingEntity(owner, box)) {
            if (!(hit instanceof EntityCreature || hit instanceof EntityPlayerMP) || hit instanceof EntityArmorStand
                    || hit == owner.getRidingEntity() || !hit.canBeCollidedWith()) {
                continue;
            }
            RayTraceResult result = hit.getEntityBoundingBox().grow(0.25D).calculateIntercept(start, end);
            if (result != null && start.squareDistanceTo(result.hitVec) < nearest) {
                best = (EntityLivingBase) hit;
                nearest = start.squareDistanceTo(result.hitVec);
            }
        }
        return best;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void drawIcon(Minecraft mc, Gui gui, int x, int y) {
        mc.getTextureManager().bindTexture(ICON);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, 16, 16, 16, 16);
    }
}
