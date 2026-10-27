package com.matoon.eternity.inventory;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.entity.EntityDestroyerArmor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

/** The mod's menus. x carries the entity a menu is about. */
public final class EternityGuis implements IGuiHandler {

    /** The Destroyer Armor's systems: its integrity and lava, its upgrades, and its cradle of stones. */
    public static final int ARMOR = 0;

    @Nullable
    private static EntityDestroyerArmor armor(World world, int id) {
        Entity entity = world.getEntityByID(id);
        return entity instanceof EntityDestroyerArmor ? (EntityDestroyerArmor) entity : null;
    }

    @Nullable
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        EntityDestroyerArmor armor = id == ARMOR ? armor(world, x) : null;
        return armor != null && armor.reachableBy(player) ? new ContainerDestroyerArmor(player.inventory, armor) : null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        EntityDestroyerArmor armor = id == ARMOR ? armor(world, x) : null;
        return armor != null ? Eternity.proxy.armorMenu(player, armor) : null;
    }
}
