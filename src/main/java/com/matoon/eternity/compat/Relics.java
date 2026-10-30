package com.matoon.eternity.compat;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.entity.EntityAstralRegulator;
import com.matoon.eternity.world.RelicLedger;
import lucraft.mods.heroesexpansion.items.HEItems;
import lucraft.mods.lucraftcore.infinity.EntityItemIndestructible;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.DimensionManager;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * The one true copy of each relic. The first player to take a relic of a kind claims it:
 * the stack is stamped with a mark and the claim is written to the {@link RelicLedger}.
 * From then on only the stack bearing that mark is recognised, by the Relic Tracker and
 * by everything that counts the stones someone holds. Other copies are just items.
 */
public final class Relics {

    /** The mark of the true relic, on the item's tag. */
    public static final String MARK = "EternityRelic";
    /** On a Tesseract a Norse village set out before anyone claimed one. */
    public static final String VILLAGE = "EternityVillage";

    public enum Kind {
        /** The Tesseract, and the Space Stone inside it once it is broken open. */
        TESSERACT("the Tesseract"),
        /** The Soul Stone: the gem on the In-Betweener's brow, or any found before it. */
        SOUL_STONE("the Soul Stone"),
        /** The Power Stone: the gem in the Champion's brow, or any found before it. */
        POWER_STONE("the Power Stone"),
        /** The Mind Stone: kept in play on the Grandmaster's board, or any found before it. */
        MIND_STONE("the Mind Stone");

        public final String title;

        Kind(String title) {
            this.title = title;
        }

        @Nullable
        public static Kind byName(String name) {
            for (Kind kind : values()) {
                if (kind.name().equalsIgnoreCase(name)) {
                    return kind;
                }
            }
            return null;
        }
    }

    private Relics() {
    }

    @Nullable
    public static Kind kind(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Item item = stack.getItem();
        if (item == HEItems.TESSERACT || item == HEItems.SPACE_STONE) {
            return Kind.TESSERACT;
        }
        if (item == anvil.infinity.items.Items.SOUL_STONE) {
            return Kind.SOUL_STONE;
        }
        if (item == anvil.infinity.items.Items.POWER_STONE) {
            return Kind.POWER_STONE;
        }
        if (item == anvil.infinity.items.Items.MIND_STONE) {
            return Kind.MIND_STONE;
        }
        return null;
    }

    @Nullable
    public static UUID mark(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasUniqueId(MARK) ? tag.getUniqueId(MARK) : null;
    }

    public static boolean isVillageCopy(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().getBoolean(VILLAGE);
    }

    /**
     * Whether the mod counts this stack as the relic it looks like. Until a kind is claimed
     * every copy counts; after, only the marked one. The client cannot see the ledger and
     * trusts every stack.
     */
    public static boolean recognized(ItemStack stack, boolean remote) {
        Kind kind = kind(stack);
        if (kind == null || remote || DimensionManager.getWorld(0) == null) {
            return true;
        }
        RelicLedger.Claim claim = RelicLedger.get().claim(kind);
        return claim == null || claim.mark.equals(mark(stack));
    }

    /** Whether this stack is the claimed relic of its kind. */
    public static boolean isTrue(ItemStack stack) {
        Kind kind = kind(stack);
        if (kind == null || DimensionManager.getWorld(0) == null) {
            return false;
        }
        RelicLedger.Claim claim = RelicLedger.get().claim(kind);
        return claim != null && claim.mark.equals(mark(stack));
    }

    public static boolean isClaimed(Kind kind) {
        return DimensionManager.getWorld(0) != null && RelicLedger.get().claim(kind) != null;
    }

    /**
     * Makes this stack the true relic of its kind, claimed by the player. Does nothing and
     * returns false if that kind is already claimed.
     */
    public static boolean claim(ItemStack stack, EntityPlayer player) {
        Kind kind = kind(stack);
        if (kind == null || DimensionManager.getWorld(0) == null) {
            return false;
        }
        RelicLedger ledger = RelicLedger.get();
        if (ledger.claim(kind) != null) {
            return false;
        }
        UUID mark = UUID.randomUUID();
        stamp(stack, mark);
        ledger.claims.put(kind, new RelicLedger.Claim(mark, player.getUniqueID(), player.getName(),
                DimensionManager.getWorld(0).getTotalWorldTime()));
        ledger.markDirty();
        if (stack.getItem() == HEItems.SPACE_STONE) {
            // Taken already out of its Tesseract.
            opened();
        }
        Eternity.LOG.info("{} has claimed {}.", player.getName(), kind.title);
        return true;
    }

    /**
     * The true Tesseract is gone, broken open: the relic is now the Space Stone that was
     * inside it. Its last sighting as a Tesseract is forgotten, so the Relic Tracker does
     * not lead anyone back to an empty pedestal.
     */
    public static void opened() {
        RelicLedger ledger = RelicLedger.get();
        RelicLedger.Claim claim = ledger.claim(Kind.TESSERACT);
        if (claim != null) {
            claim.opened = true;
        }
        if (HEItems.TESSERACT.getRegistryName() != null) {
            ledger.lastSeen.remove(HEItems.TESSERACT.getRegistryName().toString());
        }
        ledger.markDirty();
    }

    /** Forgets a claim, so the next copy someone takes becomes the true one. */
    public static void release(Kind kind) {
        if (DimensionManager.getWorld(0) == null) {
            return;
        }
        RelicLedger ledger = RelicLedger.get();
        if (ledger.claims.remove(kind) != null) {
            ledger.markDirty();
        }
    }

    public static void stamp(ItemStack stack, UUID mark) {
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        tag.setUniqueId(MARK, mark);
        tag.removeTag(VILLAGE);
        stack.setTagCompound(tag);
    }

    public static void markVillageCopy(ItemStack stack) {
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        tag.setBoolean(VILLAGE, true);
        stack.setTagCompound(tag);
    }

    /**
     * Whether something lying in the world holds a relic, and so must never simply be
     * deleted: dropped stones and Regulators (Lucraft's indestructible item entities, or a
     * plain item that slipped through), a relic in an item frame, a Regulator in flight.
     */
    public static boolean protects(Entity entity) {
        if (entity instanceof EntityItemIndestructible || entity instanceof EntityAstralRegulator) {
            return true;
        }
        if (entity instanceof EntityItem) {
            return RelicScanner.containsIndestructible(((EntityItem) entity).getItem());
        }
        if (entity instanceof EntityItemFrame) {
            return RelicScanner.containsIndestructible(((EntityItemFrame) entity).getDisplayedItem());
        }
        return false;
    }
}
