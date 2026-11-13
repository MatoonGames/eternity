package com.matoon.eternity.command;

import com.matoon.eternity.story.Fellowship;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/** For everyone: whom you quest with, so the story happens to you together. */
public class CommandFellowship extends CommandBase {

    @Override
    public String getName() {
        return "fellowship";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return Fellowship.usage();
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return sender instanceof EntityPlayerMP;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        String reply = Fellowship.command(server, getCommandSenderAsPlayer(sender), args);
        if (!reply.isEmpty()) {
            sender.sendMessage(new TextComponentString(reply));
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "invite", "accept", "leave");
        }
        if (args.length == 2 && ("invite".equals(args[0]) || "accept".equals(args[0]))) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        return Collections.emptyList();
    }
}
