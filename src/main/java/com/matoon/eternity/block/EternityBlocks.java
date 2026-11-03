package com.matoon.eternity.block;

import com.matoon.eternity.Eternity;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod.EventBusSubscriber(modid = Eternity.MODID)
public final class EternityBlocks {

    /** The Grandmaster's board. It has no item: nobody carries it but him. */
    public static final BlockBoard BOARD = new BlockBoard();
    /** A grave-stone of the oath's village. Carried, it remembers whose it is. */
    public static final BlockRuneStone RUNE_STONE = new BlockRuneStone();
    /** The barrow's doors. Nobody carries them. */
    public static final BlockBarrowSeal BARROW_SEAL = new BlockBarrowSeal();
    /** The plants the Gardener is seeding the inner worlds with. */
    public static final BlockStrangeBloom STRANGE_BLOOM = new BlockStrangeBloom();
    /** The hedge round his garden, once he knows. Nobody carries it. */
    public static final BlockGardenThorn GARDEN_THORN = new BlockGardenThorn();
    public static final BlockThornHusk THORN_HUSK = new BlockThornHusk();

    /** Warlock's home: the Sovereign station, and everything built into it. Nobody carries any of it. */
    public static final BlockSovereignHull SOVEREIGN_HULL = new BlockSovereignHull();
    public static final BlockSovereignLight SOVEREIGN_LIGHT = new BlockSovereignLight();
    public static final BlockSovereignGlass SOVEREIGN_GLASS = new BlockSovereignGlass();
    public static final BlockSovereignDoor SOVEREIGN_DOOR = new BlockSovereignDoor();
    public static final BlockSovereignFixture.Open COCOON = new BlockSovereignFixture.Open();
    public static final BlockSovereignFixture.Sealed COCOON_SEALED = new BlockSovereignFixture.Sealed();
    public static final BlockSovereignFixture.Vat VAT = new BlockSovereignFixture.Vat();
    public static final BlockSovereignFixture.Pod POD = new BlockSovereignFixture.Pod();
    public static final BlockSovereignFixture.Console CONSOLE = new BlockSovereignFixture.Console();

    public static final ItemBlock RUNE_STONE_ITEM = new ItemBlock(RUNE_STONE);
    public static final ItemStrangeBloom STRANGE_BLOOM_ITEM = new ItemStrangeBloom(STRANGE_BLOOM);

    private EternityBlocks() {
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(BOARD);
        event.getRegistry().register(RUNE_STONE);
        event.getRegistry().register(BARROW_SEAL);
        event.getRegistry().register(STRANGE_BLOOM);
        event.getRegistry().register(GARDEN_THORN);
        event.getRegistry().register(THORN_HUSK);
        event.getRegistry().registerAll(SOVEREIGN_HULL, SOVEREIGN_LIGHT, SOVEREIGN_GLASS, SOVEREIGN_DOOR, COCOON, COCOON_SEALED, VAT, POD, CONSOLE);
        GameRegistry.registerTileEntity(TileRuneStone.class, RUNE_STONE.getRegistryName());
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        RUNE_STONE_ITEM.setRegistryName(RUNE_STONE.getRegistryName());
        event.getRegistry().register(RUNE_STONE_ITEM);
        STRANGE_BLOOM_ITEM.setRegistryName(STRANGE_BLOOM.getRegistryName());
        event.getRegistry().register(STRANGE_BLOOM_ITEM);
    }
}
