package com.matoon.eternity.item;

import com.google.common.collect.Multimap;
import com.matoon.eternity.Eternity;
import com.matoon.eternity.client.render.ModelEinherjarArmor;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * The armour of the Einherjar, taken from one of Asgard's palace guard: a helm crowned with a ring
 * of gold horns, a cuirass of blackened steel over mail with the mustard cloak pinned at its
 * shoulders, armoured greaves and tassets, and heavy boots. Harder than diamond, and every piece
 * steadies you against being knocked about. Worn, it looks just as it did on him, cloak and all.
 */
public class ItemEinherjarArmor extends ItemArmor {

    public static final ArmorMaterial MATERIAL = EnumHelper.addArmorMaterial("EINHERJAR", Eternity.MODID + ":einherjar", 42,
            new int[]{4, 7, 9, 4}, 18, SoundEvents.ITEM_ARMOR_EQUIP_GOLD, 3.5F);

    private static final String TEXTURE = Eternity.MODID + ":textures/entity/einherjar.png";
    /** Each piece's share of steadiness, one id per slot so they add up. */
    private static final UUID[] STEADY = {UUID.fromString("5e3c1d6a-8f0b-4c1e-9a54-2b7d6e0f1a01"), UUID.fromString("5e3c1d6a-8f0b-4c1e-9a54-2b7d6e0f1a02"),
            UUID.fromString("5e3c1d6a-8f0b-4c1e-9a54-2b7d6e0f1a03"), UUID.fromString("5e3c1d6a-8f0b-4c1e-9a54-2b7d6e0f1a04")};

    static {
        MATERIAL.setRepairItem(new ItemStack(Items.GOLD_INGOT));
    }

    public ItemEinherjarArmor(String name, EntityEquipmentSlot slot) {
        super(MATERIAL, 0, slot);
        setRegistryName(Eternity.MODID, name);
        setTranslationKey(Eternity.MODID + "." + name);
        setCreativeTab(CreativeTabs.COMBAT);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.RARE;
    }

    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {
        Multimap<String, AttributeModifier> modifiers = super.getAttributeModifiers(slot, stack);
        if (slot == armorType) {
            modifiers.put(SharedMonsterAttributes.KNOCKBACK_RESISTANCE.getName(),
                    new AttributeModifier(STEADY[slot.getIndex()], "Einherjar steadiness", 0.15D, 0));
        }
        return modifiers;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
        return TEXTURE;
    }

    @SideOnly(Side.CLIENT)
    @Nullable
    @Override
    public ModelBiped getArmorModel(EntityLivingBase entity, ItemStack stack, EntityEquipmentSlot slot, ModelBiped original) {
        return ModelEinherjarArmor.of(slot);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.GOLD + "Worn by the Einherjar, Asgard's palace guard.");
    }
}
