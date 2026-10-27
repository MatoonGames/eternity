package com.matoon.eternity.client.render;

import com.matoon.eternity.entity.EntityEinherjar;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * An Einherjar's armour as worn by anyone else: the same boxes, painted the same, laid over a
 * body of ordinary proportions, one model for each piece. The helm with its ring of horns; the
 * cuirass with the mantle, the pauldrons, the mail sleeves and vambraces, and the cloak, which
 * hangs from the shoulders and lifts behind the wearer as they move; the belt and the tassets
 * that swing with the legs, over armoured thighs; the boots and greaves.
 *
 * The Einherjar is taller in the body than a player, so his chest and waist are set one above
 * the other inside a player's torso, and his upper arm and forearm along a player's arm.
 */
@SideOnly(Side.CLIENT)
public class ModelEinherjarArmor extends ModelBiped {

    private static final ModelEinherjarArmor[] BY_SLOT = new ModelEinherjarArmor[4];

    private final VaultBoxes.Sheet sheet = VaultBoxes.EINHERJAR;
    private final ModelRenderer[] cape = new ModelRenderer[EntityEinherjar.CAPE];
    private ModelRenderer tassetF, tassetB;
    private float partial;

    /** The model for one piece; each draws only its own. */
    public static ModelEinherjarArmor of(EntityEquipmentSlot slot) {
        int i = slot.getIndex();
        if (BY_SLOT[i] == null) {
            BY_SLOT[i] = new ModelEinherjarArmor(slot);
        }
        return BY_SLOT[i];
    }

    private ModelEinherjarArmor(EntityEquipmentSlot slot) {
        super(0F, 0F, VaultBoxes.EINHERJAR.width, VaultBoxes.EINHERJAR.height);
        for (ModelRenderer part : new ModelRenderer[]{bipedHead, bipedHeadwear, bipedBody, bipedRightArm, bipedLeftArm, bipedRightLeg, bipedLeftLeg}) {
            part.cubeList.clear();
        }
        switch (slot) {
            case HEAD:
                helm();
                break;
            case CHEST:
                cuirass();
                break;
            case LEGS:
                greaves();
                break;
            default:
                boots();
                break;
        }
    }

    private ModelRenderer part(String name, ModelRenderer parent, float x, float y, float z, float grow) {
        return VaultParts.part(this, sheet, name, parent, x, y, z, grow);
    }

    /** The helm and its crest: a ring of gold horns open at the top, a blade standing up through it. */
    private void helm() {
        ModelRenderer head = part("head", bipedHead, 0F, 0F, 0F, 0.5F);
        part("crestStem", head, 0F, -10.1F, 0F, 0F);
        part("crestBlade", head, 0F, -10.5F, 0F, 0F);
        int n = VaultBoxes.CREST_RING;
        float radius = 3.2F, ringY = -15.8F;
        for (int i = 0; i < n; i++) {
            float a = (float) Math.toRadians(45D + i * 270D / (n - 1));
            ModelRenderer seg = part("ring" + i, head, radius * MathHelper.sin(a), ringY - radius * MathHelper.cos(a), 0F, 0F);
            seg.rotateAngleZ = a - (float) Math.PI / 2F;
        }
        for (int side = -1; side <= 1; side += 2) {
            float a = (float) Math.toRadians(side < 0 ? 315D : 45D);
            ModelRenderer horn = part(side < 0 ? "hornR" : "hornL", head, (radius + 0.4F) * MathHelper.sin(a),
                    ringY - (radius + 0.4F) * MathHelper.cos(a) + 0.3F, 0F, 0F);
            horn.rotateAngleZ = 0.75F * side;
        }
    }

    /** Cuirass over mail, the mantle and its clasps, pauldrons, sleeves and vambraces, and the cloak. */
    private void cuirass() {
        ModelRenderer chest = part("chest", bipedBody, 0F, 7F, 0F, 0.35F);
        part("claspR", chest, 0F, 0F, 0F, 0.35F);
        part("claspL", chest, 0F, 0F, 0F, 0.35F);
        part("abdomen", bipedBody, 0F, 12F, 0F, 0.4F);
        ModelRenderer parent = chest;
        for (int i = 0; i < cape.length; i++) {
            cape[i] = part("cape" + i, parent, 0F, i == 0 ? -7.4F : 6F, i == 0 ? 3.1F : 0F, 0F);
            parent = cape[i];
        }
        for (int side = -1; side <= 1; side += 2) {
            ModelRenderer arm = side < 0 ? bipedRightArm : bipedLeftArm;
            String s = side < 0 ? "R" : "L";
            ModelRenderer upper = part("upper" + s, arm, 0.5F * side, -0.5F, 0F, 0.4F);
            part("pauldron" + s, upper, 0F, 0F, 0F, 0.4F);
            part("fore" + s, upper, 0.5F * side, 4.5F, 0F, 0.4F);
        }
    }

    /** The belt, the tassets hung from it, and armoured thighs. */
    private void greaves() {
        part("pelvis", bipedBody, 0F, 12F, 0F, 0.3F);
        part("tassetR", bipedBody, 0F, 12F, 0F, 0.3F);
        part("tassetL", bipedBody, 0F, 12F, 0F, 0.3F);
        tassetF = part("tassetF", bipedBody, 0F, 12.6F, -2.9F, 0.2F);
        tassetB = part("tassetB", bipedBody, 0F, 12.6F, 2.9F, 0.2F);
        part("thighR", bipedRightLeg, 0F, 0F, 0F, 0.35F);
        part("thighL", bipedLeftLeg, 0F, 0F, 0F, 0.35F);
    }

    /** Greaves over the shins, gold at the knee, heavy boots. */
    private void boots() {
        part("shinR", bipedRightLeg, 0F, 6F, 0F, 0.4F);
        part("shinL", bipedLeftLeg, 0F, 6F, 0F, 0.4F);
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks) {
        super.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks);
        partial = partialTicks;
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                                  float scale, Entity entity) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        if (entity instanceof EntityArmorStand) {
            pose((EntityArmorStand) entity);
        }
        float legs = Math.min(bipedRightLeg.rotateAngleX, bipedLeftLeg.rotateAngleX);
        float back = Math.max(bipedRightLeg.rotateAngleX, bipedLeftLeg.rotateAngleX);
        if (tassetF != null) {
            // They hang straight down from the belt however the body leans, pushed out by the thighs.
            tassetF.rotateAngleX = -bipedBody.rotateAngleX + Math.min(0F, legs) * 0.8F;
            tassetB.rotateAngleX = -bipedBody.rotateAngleX + Math.max(0F, back) * 0.7F;
        }
        if (cape[0] != null) {
            drape(entity, back);
        }
    }

    /** On an armour stand it takes the stand's pose, as vanilla's armour does there, not a living body's. */
    private void pose(EntityArmorStand stand) {
        float d = 0.017453292F;
        bipedHead.rotateAngleX = d * stand.getHeadRotation().getX();
        bipedHead.rotateAngleY = d * stand.getHeadRotation().getY();
        bipedHead.rotateAngleZ = d * stand.getHeadRotation().getZ();
        bipedHead.setRotationPoint(0F, 1F, 0F);
        bipedBody.rotateAngleX = d * stand.getBodyRotation().getX();
        bipedBody.rotateAngleY = d * stand.getBodyRotation().getY();
        bipedBody.rotateAngleZ = d * stand.getBodyRotation().getZ();
        bipedLeftArm.rotateAngleX = d * stand.getLeftArmRotation().getX();
        bipedLeftArm.rotateAngleY = d * stand.getLeftArmRotation().getY();
        bipedLeftArm.rotateAngleZ = d * stand.getLeftArmRotation().getZ();
        bipedRightArm.rotateAngleX = d * stand.getRightArmRotation().getX();
        bipedRightArm.rotateAngleY = d * stand.getRightArmRotation().getY();
        bipedRightArm.rotateAngleZ = d * stand.getRightArmRotation().getZ();
        bipedLeftLeg.rotateAngleX = d * stand.getLeftLegRotation().getX();
        bipedLeftLeg.rotateAngleY = d * stand.getLeftLegRotation().getY();
        bipedLeftLeg.rotateAngleZ = d * stand.getLeftLegRotation().getZ();
        bipedLeftLeg.setRotationPoint(1.9F, 11F, 0F);
        bipedRightLeg.rotateAngleX = d * stand.getRightLegRotation().getX();
        bipedRightLeg.rotateAngleY = d * stand.getRightLegRotation().getY();
        bipedRightLeg.rotateAngleZ = d * stand.getRightLegRotation().getZ();
        bipedRightLeg.setRotationPoint(-1.9F, 11F, 0F);
        copyModelAngles(bipedHead, bipedHeadwear);
    }

    /**
     * The cloak: hung from the shoulders, it falls plumb when the wearer stands, lifts behind them
     * as they move, swings out to the side as they turn, and the legs push it out as they stride.
     */
    private void drape(Entity entity, float legsBack) {
        float lift = 0.06F, sway = 0F;
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            float pt = partial;
            double dx = player.prevChasingPosX + (player.chasingPosX - player.prevChasingPosX) * pt - (player.prevPosX + (player.posX - player.prevPosX) * pt);
            double dy = player.prevChasingPosY + (player.chasingPosY - player.prevChasingPosY) * pt - (player.prevPosY + (player.posY - player.prevPosY) * pt);
            double dz = player.prevChasingPosZ + (player.chasingPosZ - player.prevChasingPosZ) * pt - (player.prevPosZ + (player.posZ - player.prevPosZ) * pt);
            float yaw = (float) Math.toRadians(player.prevRenderYawOffset + (player.renderYawOffset - player.prevRenderYawOffset) * pt);
            double sin = MathHelper.sin(yaw), cos = -MathHelper.cos(yaw);
            float rise = MathHelper.clamp((float) dy * 10F, -6F, 32F);
            float behind = Math.max(0F, (float) (dx * sin + dz * cos) * 100F);
            float side = (float) (dx * cos - dz * sin) * 100F;
            float bob = player.prevCameraYaw + (player.cameraYaw - player.prevCameraYaw) * pt;
            rise += MathHelper.sin((player.prevDistanceWalkedModified + (player.distanceWalkedModified - player.prevDistanceWalkedModified) * pt) * 6F)
                    * 32F * bob;
            // Heavy wool: it lifts only a little way, and never as high as a cape of silk would.
            lift = (float) Math.toRadians(MathHelper.clamp(4F + behind * 0.35F + rise * 0.6F, 2F, 70F));
            sway = (float) Math.toRadians(MathHelper.clamp(side * 0.4F, -25F, 25F));
        }
        cape[0].rotateAngleX = lift - bipedBody.rotateAngleX + Math.max(0F, legsBack) * 0.35F;
        cape[0].rotateAngleZ = sway * 0.5F;
        for (int i = 1; i < cape.length; i++) {
            cape[i].rotateAngleX = lift * 0.12F;
            cape[i].rotateAngleZ = 0F;
        }
    }
}
