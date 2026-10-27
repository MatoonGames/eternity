package com.matoon.eternity.entity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.List;

/**
 * What the Destroyer Armor can be made into. Six lines of work, each forged in order: its frame,
 * its furnace, the dynamo that can take the furnace's place, the pistons in its legs, its fists, and last
 * the cradle in its chest that holds the Astral Regulator and the Infinity Stones. Every step is paid for in materials out of the pilot's own pack; the last steps
 * of the cradle also want stones set in it.
 */
public enum ArmorUpgrade {

    // ------------------------------------------------------------------ its frame
    URU_PLATING(Branch.FRAME, 0, null, "Uru Plating", "Its frame relaid in Uru. Holds 30% more integrity.",
            ore("ingotUru", 6), item(Items.IRON_INGOT, 16)),
    BANDED_HULL(Branch.FRAME, 1, URU_PLATING, "Banded Hull", "Another 30% more integrity, harder armour, and explosions do half as much again.",
            ore("blockUru", 2), item(Blocks.OBSIDIAN, 8)),
    ODINS_WARDS(Branch.FRAME, 2, BANDED_HULL, "Odin's Wards", "Runes along its bands. It slowly mends itself from its lava, inside or out.",
            ore("blockUru", 3), ore("gemDiamond", 4), item(Items.ENDER_PEARL, 4)),

    // ------------------------------------------------------------------ its furnace
    DEEP_FURNACE(Branch.FURNACE, 0, null, "Deep Furnace", "A furnace twice as deep: holds 16 buckets of lava rather than 8.",
            item(Blocks.MAGMA, 8), item(Items.CAULDRON, 1), item(Items.IRON_INGOT, 16)),
    BELLOWS(Branch.FURNACE, 1, DEEP_FURNACE, "Bellows", "Burns a third less lava, and the furnace cools twice as fast.",
            item(Items.BLAZE_ROD, 6), ore("blockGold", 2)),
    DISINTEGRATOR(Branch.FURNACE, 2, BELLOWS, "Disintegrator", "The ray strikes 60% harder, reaches half again as far, and heats slower.",
            ore("ingotUru", 4), item(Items.BLAZE_POWDER, 16), item(Items.GHAST_TEAR, 4)),
    MOLTEN_HEART(Branch.FURNACE, 3, DISINTEGRATOR, "Molten Heart", "Wherever the ray lands it bursts. Venting takes half as long.",
            item(Items.NETHER_STAR, 1), item(Items.MAGMA_CREAM, 8)),

    // ------------------------------------------------------------------ its legs
    PISTONS(Branch.STRIDE, 0, null, "Hydraulic Pistons", "Strides 25% faster, and turns quicker.",
            item(Blocks.PISTON, 12), ore("blockRedstone", 4)),
    LEAP_JETS(Branch.STRIDE, 1, PISTONS, "Leap Jets", "Leaps higher. Hold jump in the air to glide down on jets of fire.",
            item(Blocks.SLIME_BLOCK, 4), item(Items.FIRE_CHARGE, 16)),
    THUNDERSTEP(Branch.STRIDE, 2, LEAP_JETS, "Thunderstep", "Charges 30% faster and rams harder; every landing breaks the ground half again as wide.",
            ore("ingotUru", 4), item(Blocks.TNT, 8)),

    // ------------------------------------------------------------------ its fists
    URU_KNUCKLES(Branch.FISTS, 0, null, "Uru Knuckles", "Punches 50% harder, and breaks any stone short of obsidian.",
            ore("ingotUru", 6), ore("gemDiamond", 4)),
    SHOCKWAVE(Branch.FISTS, 1, URU_KNUCKLES, "Shockwave Slam", "The slam reaches half again as far, flings harder, and heats half as much.",
            ore("blockIron", 4), ore("gunpowder", 12)),
    HAMMERFALL(Branch.FISTS, 2, SHOCKWAVE, "Hammerfall", "Every punch lands with a burst of force that sets its target ablaze.",
            ore("blockUru", 2), item(Items.BLAZE_ROD, 8)),

    // ------------------------------------------------------------------ the Infinity Stones
    CRADLE(Branch.INFINITY, 0, null, "Infinity Cradle", "Hollows out its chest: a cavity for the Astral Regulator, six sockets for the Infinity Stones arched over it. Each stone set there lends it a gift, and lends whoever is inside it the stone's own powers.",
            ore("blockUru", 2), item(Items.NETHER_STAR, 1), ore("gemDiamond", 8)) {
        @Override
        public int forgedNeeded() {
            return 6;
        }
    },
    CONDUITS(Branch.INFINITY, 1, CRADLE, "Stone Conduits", "The stones' power runs through all of it: every gift a stone lends it is stronger.",
            ore("blockUru", 4), item(Items.ENDER_EYE, 8)) {
        @Override
        public int stonesNeeded() {
            return 2;
        }
    },
    ASCENSION(Branch.INFINITY, 2, CONDUITS, "Ascension", "It flies. Double-tap jump to rise into the air; jump climbs, sneak descends.",
            ore("blockUru", 4), item(Items.ELYTRA, 1), item(Items.NETHER_STAR, 1)) {
        @Override
        public int stonesNeeded() {
            return 4;
        }
    },
    INFINITE(Branch.INFINITY, 3, ASCENSION, "The Infinite Destroyer", "With all six stones: it never burns fuel, mends itself, and every blow it strikes carries all six.",
            ore("blockUru", 8), item(Items.NETHER_STAR, 2)) {
        @Override
        public int stonesNeeded() {
            return 6;
        }
    },

    // ------------------------------------------------------------------ its power (last, so the bits of those before keep their places)
    DYNAMO(Branch.ENERGY, 0, null, "Dynamo", "Its furnace torn out for a dynamo: it runs on Forge Energy rather than lava, drawn from batteries and cells, from an Astral Regulator in its chest, and from every Infinity Stone set there.",
            ore("blockRedstone", 8), ore("plateUru", 4), ore("ingotGold", 12), item(Items.COMPARATOR, 4)),
    OVERCHARGE(Branch.ENERGY, 1, DYNAMO, "Overcharge", "Its dynamo driven past what it was built for, into its beam, its jets, its fists and its plating, each turned up on its own as far as you dare, on its OVERCHARGE page or from inside (the arrow keys). Every level draws Forge Energy every tick and heats its core: run it hotter than it sheds, or draw more than it is fed, and it implodes, for good.",
            ore("blockRedstone", 16), ore("plateUru", 8), item(Items.NETHER_STAR, 1), ore("dustGlowstone", 16));

    public enum Branch {
        FRAME("Frame"), FURNACE("Furnace"), ENERGY("Energy"), STRIDE("Legs"), FISTS("Fists"), INFINITY("Infinity");

        public final String title;

        Branch(String title) {
            this.title = title;
        }
    }

    public final Branch branch;
    /** How far along its line it is, from 0. */
    public final int tier;
    /** The step before it on its line, which must be forged first. */
    public final ArmorUpgrade parent;
    public final String title, description;
    public final Cost[] costs;

    ArmorUpgrade(Branch branch, int tier, ArmorUpgrade parent, String title, String description, Cost... costs) {
        this.branch = branch;
        this.tier = tier;
        this.parent = parent;
        this.title = title;
        this.description = description;
        this.costs = costs;
    }

    public int bit() {
        return 1 << ordinal();
    }

    public boolean in(int forged) {
        return (forged & bit()) != 0;
    }

    /** How many other upgrades must be forged before this one. */
    public int forgedNeeded() {
        return 0;
    }

    /** How many Infinity Stones must be set in its chest before this one. */
    public int stonesNeeded() {
        return 0;
    }

    /** What still stands in the way of forging it, or null if nothing but its cost does. */
    public String blocker(int forged, int stones) {
        if (in(forged)) {
            return "Forged";
        }
        if (parent != null && !parent.in(forged)) {
            return "Needs " + parent.title;
        }
        if (Integer.bitCount(forged) < forgedNeeded()) {
            return "Needs " + forgedNeeded() + " other upgrades forged";
        }
        if (Integer.bitCount(stones) < stonesNeeded()) {
            return "Needs " + stonesNeeded() + " Infinity Stones set in its chest";
        }
        return null;
    }

    /** Whether the player carries everything it costs. */
    public boolean affordable(EntityPlayer player) {
        return affordable(player, costs);
    }

    /** Takes what it costs out of the player's pack. */
    public void pay(EntityPlayer player) {
        pay(player, costs);
    }

    /** Whether the player carries all of a list of costs; in creative, they always do. */
    public static boolean affordable(EntityPlayer player, Cost[] costs) {
        if (player.capabilities.isCreativeMode) {
            return true;
        }
        for (Cost cost : costs) {
            if (cost.carried(player) < cost.count) {
                return false;
            }
        }
        return true;
    }

    /** Takes a list of costs out of the player's pack. */
    public static void pay(EntityPlayer player, Cost[] costs) {
        if (player.capabilities.isCreativeMode) {
            return;
        }
        for (Cost cost : costs) {
            int owed = cost.count;
            for (ItemStack stack : player.inventory.mainInventory) {
                if (owed <= 0) {
                    break;
                }
                if (cost.matches(stack)) {
                    int take = Math.min(owed, stack.getCount());
                    stack.shrink(take);
                    owed -= take;
                }
            }
        }
        player.inventory.markDirty();
    }

    // ------------------------------------------------------------------ what it costs

    static Cost ore(String name, int count) {
        return new Cost(name, null, count);
    }

    static Cost item(Item item, int count) {
        return new Cost(null, item, count);
    }

    static Cost item(net.minecraft.block.Block block, int count) {
        return new Cost(null, Item.getItemFromBlock(block), count);
    }

    /** So many of one material: an item, or anything the ore dictionary files under a name. */
    public static final class Cost {
        public final String ore;
        public final Item item;
        public final int count;

        Cost(String ore, Item item, int count) {
            this.ore = ore;
            this.item = item;
            this.count = count;
        }

        public boolean matches(ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            if (item != null) {
                return stack.getItem() == item;
            }
            int id = OreDictionary.getOreID(ore);
            for (int have : OreDictionary.getOreIDs(stack)) {
                if (have == id) {
                    return true;
                }
            }
            return false;
        }

        public int carried(EntityPlayer player) {
            int n = 0;
            for (ItemStack stack : player.inventory.mainInventory) {
                if (matches(stack)) {
                    n += stack.getCount();
                }
            }
            return n;
        }

        /** Something to show for it. */
        public ItemStack icon() {
            if (item != null) {
                return new ItemStack(item);
            }
            List<ItemStack> ores = OreDictionary.getOres(ore);
            if (ores.isEmpty()) {
                return new ItemStack(Blocks.BARRIER);
            }
            ItemStack first = ores.get(0).copy();
            if (first.getMetadata() == OreDictionary.WILDCARD_VALUE) {
                first.setItemDamage(0);
            }
            return first;
        }

        public String name() {
            return icon().getDisplayName();
        }
    }
}
