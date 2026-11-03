package com.matoon.eternity.item;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.story.StoryDirector;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A stub of a ticket from the Colosseum on Tamarata, the Champion's mark stamped through it. He gave it
 * to whoever brought him home after his stone was torn out of him: hold it up, and he comes down at
 * their side; hold it up again, and he goes.
 */
public class ItemChampionTicket extends Item {

    public ItemChampionTicket() {
        setRegistryName(Eternity.MODID, "champion_ticket");
        setTranslationKey(Eternity.MODID + ".champion_ticket");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.RARE;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.LIGHT_PURPLE + "\"Hold it up. I'll come.\"");
        tooltip.add(TextFormatting.GRAY + "Use: call the Champion to your side");
        tooltip.add(TextFormatting.GRAY + "Use again: send him off");
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote && player instanceof EntityPlayerMP) {
            StoryDirector.INSTANCE.ticket((EntityPlayerMP) player);
        }
        player.getCooldownTracker().setCooldown(this, 40);
        player.swingArm(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
}
