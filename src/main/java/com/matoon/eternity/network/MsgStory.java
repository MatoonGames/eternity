package com.matoon.eternity.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * The story, as one player sees and hears it: who says what, the answers they may give, the
 * scenes the camera plays, and the marks the story leaves on the world and on them. Everything
 * but {@link #LINE}, the questions and their answers, and {@link #STATE} is an effect at {@link #a}, sometimes toward
 * {@link #b}, with one number of its own.
 */
public class MsgStory implements IMessage {

    /** A line, shown in the speaker panel. value = the speaker's entity id (-1 for none), tag s = who (Speakers), t = the line. */
    public static final byte LINE = 0;
    /** Answers to choose from. value = the conversation's number, tag s = who is asked, e = their entity id, a = the answers. */
    public static final byte ASK = 1;
    /** Whatever was being asked is no longer being asked. */
    public static final byte UNASK = 2;
    /** The story's marks on this player. tag = see StoryDirector.stateTag. */
    public static final byte STATE = 3;
    /** A piece of the north lands at a, thrown from the direction of b. value = how big. */
    public static final byte FX_LEAK = 4;
    /** A cutscene begins. value = which (StoryScenes), a and b = what it frames. */
    public static final byte CUTSCENE = 5;
    /** A stone door opens (value 1) or seals (value 0), its middle at a, facing along b. */
    public static final byte FX_DOOR = 6;
    /** A rune of the oath door lights at a. value = how many are lit (0 = they flare and go out). */
    public static final byte FX_RUNE = 7;
    /** The ward round the cube fails at a, all at once. */
    public static final byte FX_WARD_OUT = 8;
    /** The Aether's column shatters at a and the Aether pours into the player whose entity id is value. */
    public static final byte FX_AETHER = 9;
    /** The Eternal Flame draws the Aether out of the player whose entity id is value, at a, and goes out. */
    public static final byte FX_FLAME = 10;
    /** The cube shows the receiving player where it was kept, for value ticks. */
    public static final byte VISION = 11;
    /** One of the dead rises out of its grave at a. */
    public static final byte FX_RISE = 12;
    /** The fight below the mound, heard through the stone at a. value = how hard (0 = over). */
    public static final byte FX_BELOW = 13;
    /** Snow and turf where the world was rewritten at a, from the base's side. value = how many blocks. */
    public static final byte FX_TAKEN = 14;
    /** How a party is voting on what is being asked. value = the conversation, tag v = the votes (id, n = name, i = answer), n = how many may vote, left = ticks until the vote closes (-1 = not yet counting). */
    public static final byte VOTES = 15;
    /** What was answered. value = the conversation, tag as {@link #VOTES} and c = the answer chosen. */
    public static final byte CHOSEN = 16;

    public byte type;
    public Vec3d a = Vec3d.ZERO, b = Vec3d.ZERO;
    public int value;
    public NBTTagCompound tag;

    public MsgStory() {
    }

    public static MsgStory line(byte speaker, int entity, String text) {
        MsgStory m = new MsgStory();
        m.type = LINE;
        m.value = entity;
        m.tag = new NBTTagCompound();
        m.tag.setByte("s", speaker);
        m.tag.setString("t", text);
        return m;
    }

    public static MsgStory ask(int talk, byte speaker, int entity, String[] answers, int voters) {
        MsgStory m = new MsgStory();
        m.type = ASK;
        m.value = talk;
        m.tag = new NBTTagCompound();
        m.tag.setByte("s", speaker);
        m.tag.setInteger("e", entity);
        NBTTagList list = new NBTTagList();
        for (String answer : answers) {
            list.appendTag(new NBTTagString(answer));
        }
        m.tag.setTag("a", list);
        m.tag.setInteger("n", voters);
        return m;
    }

    public static MsgStory votes(int talk, NBTTagCompound tally) {
        MsgStory m = new MsgStory();
        m.type = VOTES;
        m.value = talk;
        m.tag = tally;
        return m;
    }

    public static MsgStory chosen(int talk, NBTTagCompound tally) {
        MsgStory m = new MsgStory();
        m.type = CHOSEN;
        m.value = talk;
        m.tag = tally;
        return m;
    }

    public static MsgStory unask() {
        MsgStory m = new MsgStory();
        m.type = UNASK;
        return m;
    }

    public static MsgStory state(NBTTagCompound tag) {
        MsgStory m = new MsgStory();
        m.type = STATE;
        m.tag = tag;
        return m;
    }

    public static MsgStory fx(byte type, Vec3d a, Vec3d b, int value) {
        MsgStory m = new MsgStory();
        m.type = type;
        m.a = a;
        m.b = b;
        m.value = value;
        return m;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        type = buf.readByte();
        a = new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
        b = new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
        value = buf.readInt();
        if (buf.readBoolean()) {
            tag = ByteBufUtils.readTag(buf);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(type);
        buf.writeDouble(a.x);
        buf.writeDouble(a.y);
        buf.writeDouble(a.z);
        buf.writeDouble(b.x);
        buf.writeDouble(b.y);
        buf.writeDouble(b.z);
        buf.writeInt(value);
        buf.writeBoolean(tag != null);
        if (tag != null) {
            ByteBufUtils.writeTag(buf, tag);
        }
    }
}
