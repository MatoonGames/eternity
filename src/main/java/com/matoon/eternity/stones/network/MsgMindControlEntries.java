package com.matoon.eternity.stones.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Everyone the Mind Stone's holder controls, to them, for their ability bar; empty unless they hold the Soul Stone too. */
public class MsgMindControlEntries implements StoneMessage {

    public static class Entry {
        public final UUID id;
        public final String name;
        public final boolean player;
        public final int entityId;

        public Entry(UUID id, String name, boolean player, int entityId) {
            this.id = id;
            this.name = name;
            this.player = player;
            this.entityId = entityId;
        }
    }

    public List<Entry> entries = new ArrayList<>();

    public MsgMindControlEntries() {
    }

    public MsgMindControlEntries(List<Entry> entries) {
        this.entries = entries;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entries.size());
        for (Entry entry : entries) {
            StoneMessage.writeUuid(buf, entry.id);
            ByteBufUtils.writeUTF8String(buf, entry.name);
            buf.writeBoolean(entry.player);
            buf.writeInt(entry.entityId);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entries = new ArrayList<>();
        for (int i = buf.readInt(); i > 0; i--) {
            entries.add(new Entry(StoneMessage.readUuid(buf), ByteBufUtils.readUTF8String(buf), buf.readBoolean(), buf.readInt()));
        }
    }
}
