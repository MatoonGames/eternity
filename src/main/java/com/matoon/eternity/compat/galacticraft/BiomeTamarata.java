package com.matoon.eternity.compat.galacticraft;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.world.Tamarata;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Tamarata's one biome: hot, dry, rainless, and empty of anything living. It is registered
 * so its chunks can name it, but never added to any other world's generation.
 */
@Mod.EventBusSubscriber(modid = Eternity.MODID)
public class BiomeTamarata extends Biome {

    public static BiomeTamarata INSTANCE;

    private BiomeTamarata() {
        super(new BiomeProperties("Tamarata").setTemperature(2F).setRainfall(0F).setRainDisabled().setBaseHeight(0.2F).setHeightVariation(0.1F));
        setRegistryName(new ResourceLocation(Eternity.MODID, "tamarata"));
        spawnableMonsterList.clear();
        spawnableCreatureList.clear();
        spawnableWaterCreatureList.clear();
        spawnableCaveCreatureList.clear();
        IBlockState sand = Tamarata.redSand();
        topBlock = sand;
        fillerBlock = sand;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getSkyColorByTemp(float currentTemperature) {
        return 0xC9774A;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getGrassColorAtPos(net.minecraft.util.math.BlockPos pos) {
        return 0x9C6B3A;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getFoliageColorAtPos(net.minecraft.util.math.BlockPos pos) {
        return 0x8A6038;
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<Biome> event) {
        INSTANCE = new BiomeTamarata();
        event.getRegistry().register(INSTANCE);
        BiomeDictionary.addTypes(INSTANCE, BiomeDictionary.Type.HOT, BiomeDictionary.Type.DRY, BiomeDictionary.Type.SANDY,
                BiomeDictionary.Type.WASTELAND, BiomeDictionary.Type.DEAD);
    }
}
