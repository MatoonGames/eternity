package com.matoon.eternity.story;

import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgFellowship;
import com.matoon.eternity.network.MsgFellowshipAct;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Who quests with whom, as the players see it: what they are told when a fellowship forms, and the
 * /fellowship command for asking someone along, taking them up on it, or going your own way.
 * The record itself is kept in StoryData.
 */
public final class Fellowship {

    /** Who has asked whom along, and when, by whom they asked. */
    private static final Map<UUID, Map<UUID, Long>> invites = new HashMap<>();
    /** How long an invitation stands. */
    private static final long INVITE_TICKS = 20L * 120L;

    private Fellowship() {
    }

    static void reset() {
        invites.clear();
        looking.clear();
    }

    /** What each of a fellowship is told when someone joins it. */
    static ITextComponent joined(EntityPlayerMP who, EntityPlayerMP with, EntityPlayerMP told) {
        String text = told == who ? "You are questing with " + with.getName() + " now."
                : told == with ? who.getName() + " is questing with you now."
                : who.getName() + " is questing with your fellowship now.";
        return say(text + " What happens to any of you in the story has happened to you all.");
    }

    private static ITextComponent say(String text) {
        TextComponentString t = new TextComponentString(text);
        t.getStyle().setColor(TextFormatting.GREEN).setItalic(true);
        return t;
    }

    /** A name for someone, online or not. */
    static String name(MinecraftServer server, UUID id) {
        EntityPlayerMP p = server.getPlayerList().getPlayerByUUID(id);
        if (p != null) {
            return p.getName();
        }
        GameProfile profile = server.getPlayerProfileCache().getProfileByUUID(id);
        return profile != null ? profile.getName() : id.toString().substring(0, 8);
    }

    /** Everyone else in someone's fellowship, by name; empty if they quest alone. */
    static String others(MinecraftServer server, EntityPlayerMP player) {
        StringBuilder s = new StringBuilder();
        for (UUID id : StoryDirector.INSTANCE.data().fellows(player.getUniqueID())) {
            if (!id.equals(player.getUniqueID())) {
                s.append(s.length() > 0 ? ", " : "").append(name(server, id));
            }
        }
        return s.toString();
    }

    // ------------------------------------------------------------------ the command

    public static String usage() {
        return "/fellowship [menu|invite <player>|accept [player]|decline [player]|leave]";
    }

    // ------------------------------------------------------------------ the menu

    /** Who has had the menu open lately, and when they last said so, to keep it up to date for them. */
    private static final Map<UUID, Long> looking = new HashMap<>();

    /** What the menu sent back: looked at, or something done in it. */
    public static void act(EntityPlayerMP player, byte type, String name) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        String reply = "";
        switch (type) {
            case MsgFellowshipAct.INVITE:
                reply = command(server, player, new String[]{"invite", name});
                break;
            case MsgFellowshipAct.ACCEPT:
                reply = command(server, player, new String[]{"accept", name});
                break;
            case MsgFellowshipAct.DECLINE:
                reply = command(server, player, new String[]{"decline", name});
                break;
            case MsgFellowshipAct.LEAVE:
                reply = command(server, player, new String[]{"leave"});
                break;
            default:
                break;
        }
        looking.put(player.getUniqueID(), StoryDirector.now());
        show(player, reply, false);
        if (type != MsgFellowshipAct.LOOK) {
            refresh(server);
        }
    }

    /** Sends someone their side of it, and opens the menu for them if asked to. */
    static void show(EntityPlayerMP player, String status, boolean open) {
        MinecraftServer server = player.getServer();
        if (server != null) {
            NBTTagCompound tag = roster(server, player);
            tag.setString("status", status);
            EternityNetwork.send(new MsgFellowship(tag, open), player);
        }
    }

    /** Everyone with the menu open sees it as it is now. */
    static void refresh(MinecraftServer server) {
        long now = StoryDirector.now();
        looking.values().removeIf(at -> now - at > 100L);
        for (UUID id : looking.keySet()) {
            EntityPlayerMP p = server.getPlayerList().getPlayerByUUID(id);
            if (p != null) {
                show(p, "", false);
            }
        }
    }

    /**
     * Someone's fellowship as the menu shows it: "members" (id, n = name, on = online, ch = their
     * chapter, you), "asked" (who has asked them along: id, n), "sent" (whom they have asked: id),
     * and "others" (everyone else online: id, n, far = blocks away or -1 elsewhere, band = quests
     * with others already).
     */
    static NBTTagCompound roster(MinecraftServer server, EntityPlayerMP player) {
        StoryDirector story = StoryDirector.INSTANCE;
        StoryData d = story.data();
        long now = StoryDirector.now();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList members = new NBTTagList();
        List<UUID> fellows = d.fellows(player.getUniqueID());
        for (UUID id : fellows) {
            NBTTagCompound m = new NBTTagCompound();
            m.setUniqueId("id", id);
            m.setString("n", name(server, id));
            EntityPlayerMP p = server.getPlayerList().getPlayerByUUID(id);
            m.setBoolean("on", p != null);
            m.setBoolean("you", id.equals(player.getUniqueID()));
            if (p != null) {
                m.setString("ch", Journal.of(story, p).getString("chapter"));
            }
            members.appendTag(m);
        }
        tag.setTag("members", members);
        NBTTagList asked = new NBTTagList();
        Map<UUID, Long> mine = invites.get(player.getUniqueID());
        if (mine != null) {
            mine.values().removeIf(at -> now - at > INVITE_TICKS);
            for (UUID id : mine.keySet()) {
                NBTTagCompound a = new NBTTagCompound();
                a.setUniqueId("id", id);
                a.setString("n", name(server, id));
                asked.appendTag(a);
            }
        }
        tag.setTag("asked", asked);
        NBTTagList sent = new NBTTagList();
        for (Map.Entry<UUID, Map<UUID, Long>> e : invites.entrySet()) {
            Long at = e.getValue().get(player.getUniqueID());
            if (at != null && now - at <= INVITE_TICKS) {
                NBTTagCompound s = new NBTTagCompound();
                s.setUniqueId("id", e.getKey());
                sent.appendTag(s);
            }
        }
        tag.setTag("sent", sent);
        NBTTagList others = new NBTTagList();
        for (EntityPlayerMP p : server.getPlayerList().getPlayers()) {
            if (fellows.contains(p.getUniqueID())) {
                continue;
            }
            NBTTagCompound o = new NBTTagCompound();
            o.setUniqueId("id", p.getUniqueID());
            o.setString("n", p.getName());
            o.setInteger("far", p.dimension == player.dimension ? (int) p.getDistance(player) : -1);
            o.setBoolean("band", d.fellows(p.getUniqueID()).size() > 1);
            others.appendTag(o);
        }
        tag.setTag("others", others);
        return tag;
    }

    /** A personal beat: they have been asked whether they'd like company. */
    static final String OFFERED = "fellowship_offered";

    /**
     * Once the story has found someone, and there are others in the world with them, they are asked
     * (once) whether they'd quest together: a line in chat they can click, or the menu key.
     */
    static void offer(MinecraftServer server, EntityPlayerMP player) {
        StoryData d = StoryDirector.INSTANCE.data();
        StoryData.Player p = d.player(player.getUniqueID());
        int others = server.getPlayerList().getCurrentPlayerCount() - 1;
        if (p.seen(OFFERED) || others < 1 || d.fellows(player.getUniqueID()).size() > 1) {
            return;
        }
        p.beats.add(OFFERED);
        d.markDirty();
        StringBuilder names = new StringBuilder();
        int n = 0;
        for (EntityPlayerMP o : server.getPlayerList().getPlayers()) {
            if (o != player && n < 3) {
                names.append(n++ > 0 ? ", " : "").append(o.getName());
            }
        }
        TextComponentString ask = new TextComponentString("Something has begun, and you aren't alone in this world: " + names
                + (others > 3 ? " and others" : "") + (others == 1 ? " is" : " are")
                + " here too. Quest together, and what happens to one of you in the story happens to you all. ");
        ask.getStyle().setColor(TextFormatting.GREEN).setItalic(true);
        ask.appendSibling(menuButton());
        player.sendMessage(ask);
    }

    /** "[Open Fellowship]" in chat, opening the menu when clicked. */
    static ITextComponent menuButton() {
        TextComponentString b = new TextComponentString("[Open Fellowship]");
        b.getStyle().setColor(TextFormatting.GOLD).setItalic(false).setBold(true)
                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/fellowship menu"))
                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new TextComponentString("Open the fellowship menu (or press its key: K, unless you've changed it)")));
        return b;
    }

    /** /fellowship, from a player: what was done, to tell them. */
    public static String command(MinecraftServer server, EntityPlayerMP player, String[] args) {
        StoryDirector story = StoryDirector.INSTANCE;
        StoryData d = story.data();
        String action = args.length > 0 ? args[0] : "";
        long now = StoryDirector.now();
        switch (action) {
            case "": {
                String others = others(server, player);
                return others.isEmpty() ? "You quest alone. Ask someone along with /fellowship invite <player>."
                        : "You quest with " + others + ".";
            }
            case "invite": {
                if (args.length < 2) {
                    return usage();
                }
                EntityPlayerMP other = server.getPlayerList().getPlayerByUsername(args[1]);
                if (other == null) {
                    return "Nobody called " + args[1] + " is here.";
                }
                if (other == player) {
                    return "You can't ask yourself along.";
                }
                if (d.fellows(player.getUniqueID(), other.getUniqueID())) {
                    return "You already quest with " + other.getName() + ".";
                }
                invites.computeIfAbsent(other.getUniqueID(), k -> new HashMap<>()).put(player.getUniqueID(), now);
                TextComponentString ask = new TextComponentString(player.getName() + " asks you to quest with them. "
                        + "Whatever either of you has got to in the story, you will both have. ");
                ask.getStyle().setColor(TextFormatting.GREEN).setItalic(true);
                TextComponentString accept = new TextComponentString("[/fellowship accept " + player.getName() + "]");
                accept.getStyle().setColor(TextFormatting.AQUA).setItalic(false).setClickEvent(
                        new net.minecraft.util.text.event.ClickEvent(net.minecraft.util.text.event.ClickEvent.Action.RUN_COMMAND,
                                "/fellowship accept " + player.getName()));
                ask.appendSibling(accept);
                ask.appendSibling(new TextComponentString(" "));
                ask.appendSibling(menuButton());
                other.sendMessage(ask);
                refresh(server);
                return "You've asked " + other.getName() + " along.";
            }
            case "accept": {
                Map<UUID, Long> mine = invites.get(player.getUniqueID());
                if (mine != null) {
                    mine.values().removeIf(at -> now - at > INVITE_TICKS);
                }
                if (mine == null || mine.isEmpty()) {
                    return "Nobody has asked you along.";
                }
                UUID from = null;
                for (UUID id : mine.keySet()) {
                    if (args.length < 2 || name(server, id).equalsIgnoreCase(args[1])) {
                        from = id;
                    }
                }
                EntityPlayerMP inviter = StoryDirector.player(from);
                if (inviter == null) {
                    return args.length < 2 ? "Whoever asked you along has gone." : args[1] + " hasn't asked you along, or has gone.";
                }
                mine.remove(from);
                // Off on your own, then, with them: whoever you quested with before goes on without you.
                d.part(player.getUniqueID());
                d.bond(inviter.getUniqueID(), player.getUniqueID());
                for (EntityPlayerMP each : story.fellowship(inviter)) {
                    each.sendMessage(joined(player, inviter, each));
                    story.sendState(each);
                }
                refresh(server);
                return "";
            }
            case "menu":
                looking.put(player.getUniqueID(), now);
                show(player, "", true);
                return "";
            case "decline": {
                Map<UUID, Long> mine = invites.get(player.getUniqueID());
                if (mine == null || mine.isEmpty()) {
                    return "Nobody has asked you along.";
                }
                UUID from = null;
                for (UUID id : mine.keySet()) {
                    if (args.length < 2 || name(server, id).equalsIgnoreCase(args[1])) {
                        from = id;
                    }
                }
                if (from == null) {
                    return args[1] + " hasn't asked you along.";
                }
                mine.remove(from);
                EntityPlayerMP inviter = StoryDirector.player(from);
                if (inviter != null) {
                    inviter.sendMessage(say(player.getName() + " would rather go on alone, for now."));
                }
                return "You turn " + name(server, from) + " down.";
            }
            case "leave": {
                List<UUID> were = d.fellows(player.getUniqueID());
                if (!d.part(player.getUniqueID())) {
                    return "You already quest alone.";
                }
                for (UUID id : were) {
                    EntityPlayerMP p = StoryDirector.player(id);
                    if (p != null && p != player) {
                        p.sendMessage(say(player.getName() + " has gone their own way."));
                    }
                }
                story.sendState(player);
                refresh(server);
                return "You go your own way, remembering everything you've seen.";
            }
            default:
                return usage();
        }
    }
}
