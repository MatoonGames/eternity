package com.matoon.eternity.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Who someone quests with, who has asked them along, and who else is about, for the fellowship
 * menu; and, if open is set, a request that the menu be opened. See Fellowship.roster for the tag.
 */
public class MsgFellowship implements IMessage {

    public NBTTagCompound tag = new NBTTagCompound();
    public boolean open;

    public MsgFellowship() {
    }

    public MsgFellowship(NBTTagCompound tag, boolean open) {
        this.tag = tag;
        this.open = open;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        open = buf.readBoolean();
        NBTTagCompound t = ByteBufUtils.readTag(buf);
        tag = t != null ? t : new NBTTagCompound();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(open);
        ByteBufUtils.writeTag(buf, tag);
    }
}
