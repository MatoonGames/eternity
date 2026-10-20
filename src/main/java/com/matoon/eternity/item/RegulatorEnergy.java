package com.matoon.eternity.item;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

/**
 * The Astral Regulator as Forge Energy: a source that never runs dry. Whatever it holds
 * pours out through it, so it gives more the more it has absorbed (see
 * {@link ItemAstralRegulator#energyPerTick}). It can be drawn from but never filled.
 */
public class RegulatorEnergy implements IEnergyStorage, ICapabilityProvider {

    private final ItemStack stack;

    public RegulatorEnergy(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return Math.max(0, Math.min(maxExtract, ItemAstralRegulator.energyPerTick(stack)));
    }

    /** Always full: it is not a battery but a wound in the universe, leaking power. */
    @Override
    public int getEnergyStored() {
        return getMaxEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, ItemAstralRegulator.energyPerTick(stack) * 1000L);
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return false;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY;
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY ? CapabilityEnergy.ENERGY.cast(this) : null;
    }
}
