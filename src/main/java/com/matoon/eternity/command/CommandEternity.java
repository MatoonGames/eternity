package com.matoon.eternity.command;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.compat.Galacticraft;
import com.matoon.eternity.compat.InfinityStones;
import com.matoon.eternity.compat.Relics;
import com.matoon.eternity.entity.EntityEternity;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgOmen;
import com.matoon.eternity.world.CometData;
import com.matoon.eternity.world.CometDirector;
import com.matoon.eternity.world.Director;
import com.matoon.eternity.world.InBetweenerData;
import com.matoon.eternity.world.InBetweenerDirector;
import com.matoon.eternity.world.Nexus;
import com.matoon.eternity.world.RelicLedger;
import com.matoon.eternity.world.EternityData;
import com.matoon.eternity.world.NexusDirector;
import com.matoon.eternity.world.TribunalData;
import com.matoon.eternity.world.TribunalDirector;
import lucraft.mods.lucraftcore.infinity.EnumInfinityStone;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Operator tools for testing and running servers. Eternity himself never speaks in chat. */
public class CommandEternity extends CommandBase {

    private static final String[] SUBCOMMANDS = {"status", "summon", "travel", "hunt", "release", "omen", "stones", "relics", "wound", "slay", "doom", "restore", "reset", "comet", "tribunal", "nexus", "inbetweener"};

    @Override
    public String getName() {
        return "eternity";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/eternity <status|summon|travel <dim>|hunt [player]|release|omen <1-6> [player]|stones [player]|relics [release <kind>]|wound <percent>|slay|doom [0-1]|restore|reset|comet ...|tribunal ...|nexus [player]|inbetweener ...>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new WrongUsageException(getUsage(sender));
        }
        Director director = Director.INSTANCE;
        EternityData data = director.data();
        switch (args[0]) {
            case "status": {
                EntityEternity body = director.body();
                reply(sender, "Phase " + data.phase + " in dimension " + data.dimension
                        + String.format(" at %.0f %.0f %.0f", data.x, data.y, data.z));
                reply(sender, String.format("Health %.1f%%, body %s, travelling %s, next rift in %ds",
                        data.health * 100F, body == null ? "dissolved" : "present (" + body.getState() + ")",
                        director.isTravelling(), Math.max(0, (data.nextRiftAt - data.clock) / 20)));
                if (data.target != null) {
                    reply(sender, "Hunting " + name(server, data.target) + " for " + Math.max(0, (data.huntUntil - data.clock) / 20) + "s more");
                }
                for (UUID id : data.marks.keySet()) {
                    EternityData.Mark mark = data.marks.get(id);
                    reply(sender, " - " + name(server, id) + ": " + InfinityStones.count(mark.everMask) + " stone types"
                            + (mark.encounterAt >= 0 ? ", arriving in " + (mark.encounterAt - data.clock) / 20 + "s" : ""));
                }
                break;
            }
            case "summon": {
                EntityPlayerMP player = getCommandSenderAsPlayer(sender);
                ensureAlive(data);
                director.startHunt(player, 5 * 1200);
                director.travel(destinationNear(player));
                reply(sender, "The sky opens.");
                break;
            }
            case "travel": {
                if (args.length < 2) {
                    throw new WrongUsageException("/eternity travel <dimension>");
                }
                ensureAlive(data);
                int dim = parseInt(args[1]);
                director.travel(new Director.Destination(dim, data.x, 140, data.z));
                reply(sender, "Eternity tears toward dimension " + dim + ".");
                break;
            }
            case "hunt": {
                EntityPlayerMP player = args.length > 1 ? getPlayer(server, sender, args[1]) : getCommandSenderAsPlayer(sender);
                ensureAlive(data);
                director.startHunt(player, 5 * 1200);
                reply(sender, "Eternity turns toward " + player.getName() + ".");
                break;
            }
            case "release": {
                if (data.phase == EternityData.Phase.HUNTING) {
                    data.phase = EternityData.Phase.ROAMING;
                    data.target = null;
                    data.markDirty();
                }
                reply(sender, "Eternity loses interest.");
                break;
            }
            case "omen": {
                if (args.length < 2) {
                    throw new WrongUsageException("/eternity omen <1-6> [player]");
                }
                int level = parseInt(args[1], 1, 6);
                EntityPlayerMP player = args.length > 2 ? getPlayer(server, sender, args[2]) : getCommandSenderAsPlayer(sender);
                EternityNetwork.send(new MsgOmen(level, level == 6 ? 400 : 0), player);
                break;
            }
            case "stones": {
                EntityPlayerMP player = args.length > 1 ? getPlayer(server, sender, args[1]) : getCommandSenderAsPlayer(sender);
                int held = InfinityStones.scan(player);
                StringBuilder line = new StringBuilder(player.getName()).append(" holds:");
                for (EnumInfinityStone stone : EnumInfinityStone.values()) {
                    if ((held & InfinityStones.bit(stone)) != 0) {
                        line.append(' ').append(stone.name().toLowerCase());
                    }
                }
                reply(sender, line.toString());
                break;
            }
            case "relics":
                relics(server, sender, args);
                break;
            case "wound": {
                if (args.length < 2) {
                    throw new WrongUsageException("/eternity wound <percent>");
                }
                double percent = parseDouble(args[1], 0, 100);
                EntityEternity body = director.body();
                if (body != null) {
                    body.setHealth((float) Math.max(1, body.getHealth() - body.getMaxHealth() * percent / 100D));
                } else {
                    data.health = (float) Math.max(0.01, data.health - percent / 100D);
                    data.markDirty();
                }
                reply(sender, "Eternity bleeds starlight.");
                break;
            }
            case "slay": {
                EntityEternity body = director.body();
                if (body == null) {
                    throw new CommandException("Eternity has no body nearby; use /eternity summon first.");
                }
                body.onDeath(DamageSource.causePlayerDamage(getCommandSenderAsPlayer(sender)));
                break;
            }
            case "doom": {
                float progress = args.length > 1 ? (float) parseDouble(args[1], 0, 1) : 0F;
                director.forceUnmaking(progress);
                reply(sender, "Reality begins to unmake itself.");
                break;
            }
            case "restore": {
                director.restoreReality(server);
                reply(sender, "Reality holds.");
                break;
            }
            case "comet":
                comet(server, sender, args);
                break;
            case "tribunal":
                tribunal(server, sender, args);
                break;
            case "inbetweener":
                inBetweener(server, sender, args);
                break;
            case "nexus": {
                EntityPlayerMP player = args.length > 1 ? getPlayer(server, sender, args[1]) : getCommandSenderAsPlayer(sender);
                NexusDirector.INSTANCE.enter(player);
                reply(sender, player.getName() + " falls into the Nexus of All Realities.");
                break;
            }
            case "reset": {
                if (data.phase == EternityData.Phase.UNMAKING || data.phase == EternityData.Phase.VOID) {
                    director.restoreReality(server);
                }
                director.vanish();
                data.phase = EternityData.Phase.ROAMING;
                data.target = null;
                data.health = 1F;
                data.nextRiftAt = -1;
                data.marks.clear();
                data.markDirty();
                reply(sender, "Eternity forgets everything.");
                break;
            }
            default:
                throw new WrongUsageException(getUsage(sender));
        }
    }

    private static void comet(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        CometDirector comet = CometDirector.INSTANCE;
        CometData data = comet.data();
        String action = args.length > 1 ? args[1] : "status";
        switch (action) {
            case "status": {
                reply(sender, "Comet " + data.phase + (data.isVisible() ? " in dimension " + data.dimension
                        + String.format(" at %.0f %.0f %.0f", data.x, data.y, data.z) : ""));
                reply(sender, String.format("Integrity %.1f%%, unseen for %ds of %ds%s", comet.integrity(data) * 100F,
                        data.lostTicks / 20, EternityConfig.comet.escapeSeconds,
                        data.breaker.isEmpty() ? "" : ", last struck by " + data.breaker));
                break;
            }
            case "summon": {
                EntityPlayerMP player = getCommandSenderAsPlayer(sender);
                if (data.phase == CometData.Phase.SHATTERED) {
                    throw new CommandException("The comet was broken open; it will never return. Use /eternity comet restore first.");
                }
                if (data.phase == CometData.Phase.CRACKING) {
                    throw new CommandException("The comet is already breaking open.");
                }
                boolean space = player.dimension == EternityConfig.comet.spaceDimension || Galacticraft.isSpace(player.world);
                comet.appear(player, space);
                reply(sender, "A light tears across the sky.");
                break;
            }
            case "wound": {
                if (args.length < 3) {
                    throw new WrongUsageException("/eternity comet wound <percent>");
                }
                comet.wound(parseDouble(args[2], 0, 100) / 100D, sender.getName());
                reply(sender, String.format("The comet's integrity is %.1f%%.", comet.integrity(data) * 100F));
                break;
            }
            case "depart": {
                comet.depart();
                reply(sender, "The comet slips away.");
                break;
            }
            case "restore": {
                comet.restore();
                reply(sender, "The comet is whole again, somewhere out in the dark.");
                break;
            }
            default:
                throw new WrongUsageException("/eternity comet <status|summon|wound <percent>|depart|restore>");
        }
    }

    private static void tribunal(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        TribunalDirector tribunal = TribunalDirector.INSTANCE;
        TribunalData data = tribunal.data();
        String action = args.length > 1 ? args[1] : "status";
        switch (action) {
            case "status": {
                String target = data.target == null ? "nobody" : name(server, data.target);
                reply(sender, "The Living Tribunal is " + (tribunal.isJudging() ? "judging " + target
                        : data.summonAt >= 0 ? "coming for " + target + " in " + Math.max(0, (data.summonAt - data.clock) / 20) + "s"
                        : "watching"));
                int limit = EternityConfig.tribunal.perWorld;
                reply(sender, "Absorbed " + data.absorbed + " of " + (limit < 0 ? "unlimited" : String.valueOf(limit)) + " this world holds");
                reply(sender, "Cast out: " + (data.banished.isEmpty() ? "nobody" : String.join(", ", data.banished.values())));
                break;
            }
            case "summon": {
                EntityPlayerMP player = args.length > 2 ? getPlayer(server, sender, args[2]) : getCommandSenderAsPlayer(sender);
                if (!tribunal.summon(player)) {
                    throw new CommandException("The Living Tribunal is already sitting in judgement.");
                }
                reply(sender, "He comes for " + player.getName() + ".");
                break;
            }
            case "pardon": {
                if (args.length < 3) {
                    throw new WrongUsageException("/eternity tribunal pardon <player>");
                }
                if (!tribunal.pardon(args[2])) {
                    throw new CommandException(args[2] + " was never cast out.");
                }
                reply(sender, args[2] + " may return.");
                break;
            }
            default:
                throw new WrongUsageException("/eternity tribunal <status|summon [player]|pardon <player>>");
        }
    }

    private static void inBetweener(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        InBetweenerDirector director = InBetweenerDirector.INSTANCE;
        InBetweenerData data = director.data();
        String action = args.length > 1 ? args[1] : "status";
        switch (action) {
            case "status": {
                String visitor = director.visitorName(server);
                Vec3d drift = director.driftPoint();
                reply(sender, "The In-Betweener is " + (data.inBetweenerAbsorbed ? "inside an Astral Regulator"
                        : drift != null ? String.format("drifting empty through the Nexus near %d %d %d", (int) drift.x, (int) drift.y, (int) drift.z)
                        : data.broken ? "lying outside his broken glass" : "in his glass")
                        + (visitor != null ? ", with " + visitor : "")
                        + String.format(", at %d %d %d in dimension %d", (int) Math.floor(Nexus.JUNCTION_X), (int) Math.floor(Nexus.JUNCTION_Y),
                        (int) Math.floor(Nexus.JUNCTION_Z), Nexus.dimension()));
                reply(sender, "Soul Gem: " + (director.wearsGem() ? "on his brow"
                        : data.gemTaken ? "taken by " + data.gemHolder : "not his (one exists elsewhere, or holdsSoulStone is off)"));
                if (data.wrathAt >= 0) {
                    reply(sender, "Order and Chaos arrive in " + Math.max(0, (data.wrathAt - data.clock) / 20) + "s");
                }
                if (data.inBetweenerAbsorbed || data.orderAbsorbed || data.chaosAbsorbed) {
                    reply(sender, "Inside Astral Regulators:" + (data.inBetweenerAbsorbed ? " the In-Betweener" : "")
                            + (data.orderAbsorbed ? " Master Order" : "") + (data.chaosAbsorbed ? " Lord Chaos" : ""));
                }
                reply(sender, "He remembers " + data.memories.size() + " visitors and refuses " + data.castOut.size());
                break;
            }
            case "visit": {
                EntityPlayerMP player = args.length > 2 ? getPlayer(server, sender, args[2]) : getCommandSenderAsPlayer(sender);
                InBetweenerDirector.visit(player);
                reply(sender, player.getName() + " stands at the edge of the grey.");
                break;
            }
            case "wrath": {
                if (!data.broken) {
                    throw new CommandException("His glass is whole; Order and Chaos have no reason to come.");
                }
                director.hastenWrath();
                reply(sender, "Order and Chaos arrive.");
                break;
            }
            case "masters": {
                EntityPlayerMP player = args.length > 2 ? getPlayer(server, sender, args[2]) : getCommandSenderAsPlayer(sender);
                if (player.dimension != Nexus.dimension()) {
                    throw new CommandException("They only come to the Nexus. Use /eternity inbetweener visit first.");
                }
                director.summonMasters(player);
                reply(sender, "Master Order and Lord Chaos turn their attention to " + player.getName() + ".");
                break;
            }
            case "revive": {
                director.revive();
                reply(sender, "He is back in his glass, colour and all. He still remembers everyone.");
                break;
            }
            case "reset": {
                director.restore();
                reply(sender, "His glass is whole, he and his masters are back if they were absorbed, the gem is on his brow unless a Soul Stone exists elsewhere, and he remembers nobody.");
                break;
            }
            default:
                throw new WrongUsageException("/eternity inbetweener <status|visit [player]|wrath|masters [player]|revive|reset>");
        }
    }

    /** Who claimed each relic and when, where it is now, and a way to forget a claim. */
    private void relics(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        RelicLedger ledger = RelicLedger.get();
        if (args.length > 1 && "release".equals(args[1])) {
            Relics.Kind kind = args.length > 2 ? Relics.Kind.byName(args[2]) : null;
            if (kind == null) {
                throw new WrongUsageException("/eternity relics release <tesseract|soul_stone>");
            }
            Relics.release(kind);
            reply(sender, "Nobody holds " + kind.title + " any more. The next copy someone takes becomes the true one.");
            return;
        }
        long now = server.getWorld(0).getTotalWorldTime();
        for (Relics.Kind kind : Relics.Kind.values()) {
            RelicLedger.Claim claim = ledger.claim(kind);
            reply(sender, claim == null ? kind.title + ": unclaimed"
                    : kind.title + ": claimed by " + claim.claimantName + ", " + (now - claim.at) / 24000L + " days ago"
                    + (claim.opened ? " (broken open: it is the Space Stone now)" : ""));
        }
        for (Map.Entry<String, RelicLedger.Sighting> entry : ledger.lastSeen.entrySet()) {
            RelicLedger.Sighting s = entry.getValue();
            reply(sender, String.format(" - %s: %s in dimension %d at %.0f %.0f %.0f", entry.getKey(),
                    s.holder.isEmpty() ? "last seen" : "carried by " + s.holder, s.dimension, s.x, s.y, s.z));
        }
    }

    private static void ensureAlive(EternityData data) throws CommandException {
        if (data.phase != EternityData.Phase.ROAMING && data.phase != EternityData.Phase.HUNTING) {
            throw new CommandException("Eternity is gone (" + data.phase + "). Use /eternity restore, then /eternity reset, to bring him back.");
        }
    }

    private static Director.Destination destinationNear(EntityPlayerMP player) {
        double yaw = Math.toRadians(player.rotationYaw);
        return new Director.Destination(player.dimension, player.posX - Math.sin(yaw) * 40D, player.posY - 2D, player.posZ + Math.cos(yaw) * 40D);
    }

    private static String name(MinecraftServer server, UUID id) {
        EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(id);
        return player != null ? player.getName() : id.toString().substring(0, 8);
    }

    private static void reply(ICommandSender sender, String text) {
        TextComponentString message = new TextComponentString(text);
        message.getStyle().setColor(TextFormatting.DARK_PURPLE);
        sender.sendMessage(message);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, SUBCOMMANDS);
        }
        if (args.length == 2 && "tribunal".equals(args[0])) {
            return getListOfStringsMatchingLastWord(args, "status", "summon", "pardon");
        }
        if (args.length == 3 && "tribunal".equals(args[0]) && "summon".equals(args[1])) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        if (args.length == 2 && "inbetweener".equals(args[0])) {
            return getListOfStringsMatchingLastWord(args, "status", "visit", "wrath", "masters", "revive", "reset");
        }
        if (args.length == 3 && "inbetweener".equals(args[0]) && "visit".equals(args[1])) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        if (args.length == 2 && "relics".equals(args[0])) {
            return getListOfStringsMatchingLastWord(args, "release");
        }
        if (args.length == 3 && "relics".equals(args[0])) {
            return getListOfStringsMatchingLastWord(args, "tesseract", "soul_stone");
        }
        if (args.length == 2 && "comet".equals(args[0])) {
            return getListOfStringsMatchingLastWord(args, "status", "summon", "wound", "depart", "restore");
        }
        if (args.length == 2 && ("hunt".equals(args[0]) || "stones".equals(args[0]) || "nexus".equals(args[0]))) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        return Collections.emptyList();
    }
}
