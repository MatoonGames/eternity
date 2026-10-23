package com.matoon.eternity.world;

import com.matoon.eternity.compat.Relics;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Where each relic was last seen, and which relics have been claimed. A stone locked in
 * a chest in an unloaded chunk cannot be scanned, so the Relic Tracker points at its last
 * known resting place. A claimed relic is the one true copy the mod recognises; see
 * {@link Relics}.
 */
public class RelicLedger extends WorldSavedData {

    /** Unchanged from when this was only a ledger of stones, so older worlds keep their records. */
    public static final String NAME = "eternity_stones";

    /** Keyed by the relic item's registry name. */
    public final Map<String, Sighting> lastSeen = new HashMap<>();
    public final Map<Relics.Kind, Claim> claims = new EnumMap<>(Relics.Kind.class);

    public static final class Sighting {
        public final int dimension;
        public final double x, y, z;
        /** The player carrying it when it was seen, or empty. */
        public final String holder;

        public Sighting(int dimension, double x, double y, double z, String holder) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.holder = holder;
        }
    }

    /** Who first took a relic, and the mark stamped on it that makes it the true one. */
    public static final class Claim {
        public final UUID mark;
        public final UUID claimant;
        public final String claimantName;
        /** World time of the claim. */
        public final long at;
        /** For the Tesseract: it has been broken open, and the relic is now the Space Stone that was inside. */
        public boolean opened;

        public Claim(UUID mark, UUID claimant, String claimantName, long at) {
            this.mark = mark;
            this.claimant = claimant;
            this.claimantName = claimantName;
            this.at = at;
        }
    }

    public RelicLedger() {
        super(NAME);
    }

    public RelicLedger(String name) {
        super(name);
    }

    public static RelicLedger get() {
        World overworld = DimensionManager.getWorld(0);
        MapStorage storage = overworld.getMapStorage();
        RelicLedger ledger = (RelicLedger) storage.getOrLoadData(RelicLedger.class, NAME);
        if (ledger == null) {
            ledger = new RelicLedger();
            storage.setData(NAME, ledger);
        }
        return ledger;
    }

    @Nullable
    public Claim claim(Relics.Kind kind) {
        return claims.get(kind);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        lastSeen.clear();
        NBTTagList list = nbt.getTagList("stones", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            lastSeen.put(tag.getString("item"), new Sighting(tag.getInteger("dim"),
                    tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"), tag.getString("holder")));
        }
        claims.clear();
        NBTTagList claimList = nbt.getTagList("claims", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < claimList.tagCount(); i++) {
            NBTTagCompound tag = claimList.getCompoundTagAt(i);
            Relics.Kind kind = Relics.Kind.byName(tag.getString("kind"));
            if (kind != null && tag.hasUniqueId("mark")) {
                Claim claim = new Claim(tag.getUniqueId("mark"), tag.getUniqueId("claimant"),
                        tag.getString("claimantName"), tag.getLong("at"));
                claim.opened = tag.getBoolean("opened");
                claims.put(kind, claim);
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<String, Sighting> entry : lastSeen.entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("item", entry.getKey());
            tag.setInteger("dim", entry.getValue().dimension);
            tag.setDouble("x", entry.getValue().x);
            tag.setDouble("y", entry.getValue().y);
            tag.setDouble("z", entry.getValue().z);
            tag.setString("holder", entry.getValue().holder);
            list.appendTag(tag);
        }
        nbt.setTag("stones", list);
        NBTTagList claimList = new NBTTagList();
        for (Map.Entry<Relics.Kind, Claim> entry : claims.entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("kind", entry.getKey().name());
            tag.setUniqueId("mark", entry.getValue().mark);
            tag.setUniqueId("claimant", entry.getValue().claimant);
            tag.setString("claimantName", entry.getValue().claimantName);
            tag.setLong("at", entry.getValue().at);
            tag.setBoolean("opened", entry.getValue().opened);
            claimList.appendTag(tag);
        }
        nbt.setTag("claims", claimList);
        return nbt;
    }
}
