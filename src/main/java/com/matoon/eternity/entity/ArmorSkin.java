package com.matoon.eternity.entity;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;

import static com.matoon.eternity.entity.ArmorUpgrade.item;
import static com.matoon.eternity.entity.ArmorUpgrade.ore;

/**
 * The finishes the Destroyer Armor can be painted in, on its Paint page. Each but the first is made
 * once, out of dyes and materials from the pilot's own pack, and is then the armour's to switch to
 * whenever its pilot likes. The textures themselves are painted by {@code tools/VaultSkin.java}, as
 * {@code destroyer_<key>.png} (and {@code destroyer_<key>_glow.png} for those with light of their own).
 */
public enum ArmorSkin {

    GUNMETAL("gunmetal", "Uru Gunmetal", "The Destroyer as Odin's smiths made it: dark banded Uru.",
            new int[]{0x121317, 0x2b2e35, 0x666c76, 0xa8aeb7}, false),
    GOLD("gold", "Asgardian Gold", "Gilded like the palace itself, every band catching the light.",
            new int[]{0x4a3010, 0x9a6c24, 0xe8bc58, 0xfff0b0}, false,
            ore("blockGold", 2), ore("dyeYellow", 8)),
    BURNISHED("burnished", "Burnished Steel", "Polished bright and pale, the dark ground out of every band.",
            new int[]{0x3d4149, 0x767c86, 0xc4c9d0, 0xf0f3f6}, false,
            ore("ingotIron", 24), ore("dyeWhite", 8), ore("dyeLightGray", 8)),
    ROYAL("royal", "Royal Guard", "Deep Asgardian blue, its bands, crown and spikes picked out in gold.",
            new int[]{0x0a1230, 0x26397e, 0xc49434, 0xfff0b0}, false,
            ore("ingotGold", 12), ore("dyeBlue", 12), ore("dyeCyan", 4)),
    FROST("frost", "Jotunheim Frost", "Rimed in the ice of the Frost Giants, pale blue to white.",
            new int[]{0x2c4058, 0x5f8298, 0xb4d0de, 0xeaf6fc}, false,
            item(Blocks.PACKED_ICE, 8), ore("dyeLightBlue", 8), ore("dyeWhite", 8)),
    MUSPEL("muspel", "Muspelheim", "Burnt black, split with molten cracks that glow with its furnace.",
            new int[]{0x0b0807, 0x241a15, 0xa8380c, 0xffa040}, true,
            item(Blocks.MAGMA, 8), ore("dyeBlack", 8), ore("dyeRed", 4), item(Items.BLAZE_POWDER, 8)),
    BIFROST("bifrost", "Bifrost", "Every band painted in the colours of the Rainbow Bridge.",
            new int[]{0xe04040, 0xf0c040, 0x40c070, 0x6060f0}, false,
            ore("dyeRed", 4), ore("dyeOrange", 4), ore("dyeYellow", 4), ore("dyeLime", 4), ore("dyeLightBlue", 4),
            ore("dyePurple", 4), ore("dustGlowstone", 8)),
    CELESTIAL("celestial", "Celestial", "The violet of the space between the realms, scattered with golden stars.",
            new int[]{0x140b22, 0x3c2160, 0x8058b0, 0xffe39a}, true,
            ore("dyePurple", 12), ore("dyeBlack", 8), ore("nuggetGold", 16), item(Items.ENDER_PEARL, 4)),
    WARPAINT("warpaint", "Warpaint", "Its dark Uru daubed with crimson war-stripes, chipped where it has fought.",
            new int[]{0x121317, 0x41454e, 0x961c18, 0xe05040}, false,
            ore("dyeRed", 12), ore("dyeBlack", 6), item(Items.BONE, 4));

    public final String key, title, description;
    /** Its colours, darkest to brightest, for its swatch on the Paint page. */
    public final int[] swatch;
    /** Whether it has light of its own, glowing as its furnace does. */
    public final boolean glows;
    public final ArmorUpgrade.Cost[] costs;

    ArmorSkin(String key, String title, String description, int[] swatch, boolean glows, ArmorUpgrade.Cost... costs) {
        this.key = key;
        this.title = title;
        this.description = description;
        this.swatch = swatch;
        this.glows = glows;
        this.costs = costs;
    }

    public int bit() {
        return 1 << ordinal();
    }

    public static ArmorSkin of(int index) {
        ArmorSkin[] all = values();
        return index >= 0 && index < all.length ? all[index] : GUNMETAL;
    }
}
