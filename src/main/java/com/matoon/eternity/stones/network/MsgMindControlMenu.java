package com.matoon.eternity.stones.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** What the server sees of someone under the Mind Stone's control, to its holder, to draw their window into them. */
public class MsgMindControlMenu implements StoneMessage {

    public static class AbilityInfo {
        public final String key, name, description;
        public final boolean ready, triggerable;

        public AbilityInfo(String key, String name, String description, boolean ready, boolean triggerable) {
            this.key = key;
            this.name = name;
            this.description = description;
            this.ready = ready;
            this.triggerable = triggerable;
        }
    }

    public UUID target;
    public int entityId;
    public String name, superpowerId, superpowerName;
    public boolean playerInventory, abilityAutocast;
    public List<ItemStack> items = new ArrayList<>();
    public List<AbilityInfo> abilities = new ArrayList<>();
    public float health, maxHealth;
    public int armor, remainingTicks;

    public MsgMindControlMenu() {
    }

    public MsgMindControlMenu(UUID target, int entityId, String name, boolean playerInventory, List<ItemStack> items,
                                     float health, float maxHealth, int armor, int remainingTicks, List<AbilityInfo> abilities,
                                     boolean abilityAutocast, String superpowerId, String superpowerName) {
        this.target = target;
        this.entityId = entityId;
        this.name = name;
        this.playerInventory = playerInventory;
        this.items = items;
        this.health = health;
        this.maxHealth = maxHealth;
        this.armor = armor;
        this.remainingTicks = remainingTicks;
        this.abilities = abilities;
        this.abilityAutocast = abilityAutocast;
        this.superpowerId = superpowerId;
        this.superpowerName = superpowerName;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        StoneMessage.writeUuid(buf, target);
        buf.writeInt(entityId);
        ByteBufUtils.writeUTF8String(buf, name);
        buf.writeBoolean(playerInventory);
        buf.writeFloat(health);
        buf.writeFloat(maxHealth);
        buf.writeInt(armor);
        buf.writeInt(remainingTicks);
        buf.writeInt(items.size());
        for (ItemStack stack : items) {
            ByteBufUtils.writeItemStack(buf, stack);
        }
        buf.writeInt(abilities.size());
        for (AbilityInfo ability : abilities) {
            ByteBufUtils.writeUTF8String(buf, ability.key);
            ByteBufUtils.writeUTF8String(buf, ability.name);
            ByteBufUtils.writeUTF8String(buf, ability.description);
            buf.writeBoolean(ability.ready);
            buf.writeBoolean(ability.triggerable);
        }
        buf.writeBoolean(abilityAutocast);
        ByteBufUtils.writeUTF8String(buf, superpowerId == null ? "" : superpowerId);
        ByteBufUtils.writeUTF8String(buf, superpowerName == null ? "" : superpowerName);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        target = StoneMessage.readUuid(buf);
        entityId = buf.readInt();
        name = ByteBufUtils.readUTF8String(buf);
        playerInventory = buf.readBoolean();
        health = buf.readFloat();
        maxHealth = buf.readFloat();
        armor = buf.readInt();
        remainingTicks = buf.readInt();
        items = new ArrayList<>();
        for (int i = buf.readInt(); i > 0; i--) {
            items.add(ByteBufUtils.readItemStack(buf));
        }
        abilities = new ArrayList<>();
        for (int i = buf.readInt(); i > 0; i--) {
            abilities.add(new AbilityInfo(ByteBufUtils.readUTF8String(buf), ByteBufUtils.readUTF8String(buf),
                    ByteBufUtils.readUTF8String(buf), buf.readBoolean(), buf.readBoolean()));
        }
        abilityAutocast = buf.readBoolean();
        superpowerId = ByteBufUtils.readUTF8String(buf);
        superpowerName = ByteBufUtils.readUTF8String(buf);
    }
}
