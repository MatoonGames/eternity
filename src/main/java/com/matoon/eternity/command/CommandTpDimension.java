package com.matoon.eternity.command;

import com.matoon.eternity.world.Teleport;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * /tpdimension &lt;player&gt; &lt;dimension&gt; [x y z]
 *
 * Without coordinates the player lands safely at the dimension's spawn. With them,
 * they go exactly there; "~" is relative to where the player currently stands.
 */
public class CommandTpDimension extends CommandBase {

    @Override
    public String getName() {
        return "tpdimension";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/tpdimension <player> <dimension> [x y z]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 2 && args.length != 5) {
            throw new WrongUsageException(getUsage(sender));
        }
        EntityPlayerMP player = getPlayer(server, sender, args[0]);
        int dimension = parseInt(args[1]);
        if (!DimensionManager.isDimensionRegistered(dimension)) {
            throw new CommandException("Dimension " + dimension + " does not exist.");
        }
        WorldServer world = server.getWorld(dimension);
        if (world == null) {
            throw new CommandException("Dimension " + dimension + " could not be loaded.");
        }

        double x, y, z;
        boolean safe;
        if (args.length == 5) {
            x = parseCoordinate(player.posX, args[2], true).getResult();
            y = parseCoordinate(player.posY, args[3], -4096, 4096, false).getResult();
            z = parseCoordinate(player.posZ, args[4], true).getResult();
            safe = false;
        } else {
            BlockPos spawn = dimension == 1 && world.getSpawnCoordinate() != null ? world.getSpawnCoordinate() : world.getSpawnPoint();
            x = spawn.getX() + 0.5D;
            y = spawn.getY();
            z = spawn.getZ() + 0.5D;
            safe = true;
        }

        Teleport.player(player, dimension, x, y, z, safe);
        notifyCommandListener(sender, this, "Teleported %s to dimension %s at %s, %s, %s", player.getName(), dimension,
                String.format("%.1f", player.posX), String.format("%.1f", player.posY), String.format("%.1f", player.posZ));
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        if (args.length == 2) {
            List<String> dims = new ArrayList<>();
            for (Map.Entry<DimensionType, it.unimi.dsi.fastutil.ints.IntSortedSet> entry : DimensionManager.getRegisteredDimensions().entrySet()) {
                for (int id : entry.getValue()) {
                    dims.add(String.valueOf(id));
                }
            }
            return getListOfStringsMatchingLastWord(args, dims);
        }
        if (args.length >= 3 && args.length <= 5) {
            return getTabCompletionCoordinate(args, 2, targetPos);
        }
        return Collections.emptyList();
    }
}
