package com.matoon.eternity.story;

import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.client.render.NorseBoxes;
import com.matoon.eternity.entity.EntityNorse;
import com.matoon.eternity.entity.NorsePose;
import com.matoon.eternity.item.ItemAstralRegulator;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgTribunalFx;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.UUID;

import static com.matoon.eternity.story.Speakers.DEATH;
import static com.matoon.eternity.story.Talk.act;
import static com.matoon.eternity.story.Talk.say;

/**
 * Death, out of her skin, in front of someone she thinks is hers: and they are holding the Astral
 * Regulator. Right-clicked with it, she is drawn in, the way the cosmic beings are, and she does
 * not go quietly. She thought they loved her; she took her face off for them. She claws at the
 * ground, she screams it at them, and then there is only the crystal, heavy, and something in it
 * beating on the walls. Put it down before she is all the way in, or walk off, and she is out,
 * and coming, and furious.
 * <p>
 * Once she is in, she is gone from the world (see {@link #contained}): no more visits, no more
 * whispers, nobody standing over what dies at their hand to show them where the next stone is.
 */
final class Betrayal {

    /** She is in the Regulator (a world beat: there is only one of her). */
    static final String CONTAINED = "death_contained";

    private final StoryDirector story;
    /** Her, while she is being drawn in; whoever raised it, and with which hand. */
    @Nullable
    private EntityNorse her;
    @Nullable
    private Talk scene;
    @Nullable
    private UUID holder;
    private EnumHand hand = EnumHand.MAIN_HAND;
    private int drawTicks;
    /** The one being drawn in is the Death who came back to see what they had become (see {@link Aftermath}). */
    private boolean ex;

    Betrayal(StoryDirector story) {
        this.story = story;
    }

    void reset() {
        her = null;
        scene = null;
        holder = null;
    }

    static boolean contained() {
        return StoryDirector.INSTANCE.data().seen(CONTAINED);
    }

    /** She is gone from the world: in the Regulator, or gone for good after the snap (see {@link Reckoning}). */
    static boolean absent() {
        return contained() || StoryDirector.INSTANCE.data().seen(Reckoning.DEATH_GONE);
    }

    /** Someone right-clicks her: true if she is out of her skin and they have turned the Regulator on her. */
    boolean raised(EntityPlayerMP player, EntityNorse d) {
        if (!StoryDirector.enabled() || d.look() != NorseBoxes.UNVEILED && !story.aftermath.has(d) || d.pose() == NorsePose.TEAR || Oath.alive(her) != null
                || contained() || story.reckoning.has(d) || !ItemAstralRegulator.holdsRegulator(player)) {
            return false;
        }
        hand = player.getHeldItemMainhand().getItem() instanceof ItemAstralRegulator ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
        holder = player.getUniqueID();
        drawTicks = 0;
        // Whatever she was saying, she stops saying it. The one who raised it takes her over from whoever brought her.
        ex = story.aftermath.has(d);
        if (ex) {
            story.aftermath.taken();
        }
        story.cut(player);
        WorldServer world = player.getServerWorld();
        EntityNorse e = her = EntityNorse.of(world, ex ? d.look() : NorseBoxes.UNVEILED, "betrayed");
        e.setRot(ex ? d.rot() : e.rot());
        e.setLocationAndAngles(d.posX, d.posY, d.posZ, d.rotationYaw, 0F);
        e.face(player.getPositionVector());
        d.setDead();
        world.spawnEntity(e);
        e.watch(player);
        world.playSound(null, player.posX, player.posY, player.posZ, EternitySounds.RIFT_OPEN, SoundCategory.HOSTILE, 4F, 0.5F);
        world.playSound(null, player.posX, player.posY, player.posZ, EternitySounds.FINALE_DRUMS, SoundCategory.HOSTILE, 2F, 0.8F);

        Talk t = scene = new Talk(player).at(e, 48D).reach(32D);
        if (ex) {
            exScene(t, e, player);
        } else {
            t.run(() -> {
                story.mark(player, Devotion.UNVEILED);
                e.act(NorsePose.RECOIL, 0);
            })
                    .direction("You lift the Regulator. Its facets catch the light coming off her, and hold on to it.")
                    .line(DEATH, e, "What's that?")
                    .direction("Her cape is lifting off her a thread at a time, streaming toward your hand like smoke up a chimney. She looks down at it. Then she looks at you.")
                    .line(DEATH, e, "No. Put it down. Sweetheart, put it down. You don't know what that is.")
                    .line(DEATH, e, "You do. You do know. You've had it the whole time.")
                    .run(() -> e.act(NorsePose.FURY))
                    .direction("Her jaw drops open past where a jaw stops, and she comes at you, and gets nowhere. Whatever is in the crystal has her by the edges, and it's pulling.")
                    .line(DEATH, e, "I HELD YOU. EVERY TIME YOU DIED, I HELD YOU.")
                    .line(DEATH, e, "I TOOK MY FACE OFF FOR YOU. WITH MY OWN HANDS. AND YOU HAD THAT IN YOUR POCKET.")
                    .ask(DEATH, e, 20 * 12, 2,
                            say("I'm sorry.", c -> c.line(DEATH, e, "NO YOU'RE NOT. You're not sorry. You're careful. That's so much worse.")),
                            say("You wanted half of everything dead.", c -> c.line(DEATH, e, "I wanted to give you EVERYTHING. Half was the wrapping paper.")),
                            act("(Hold it steady.)", c -> c.line(DEATH, e, "Look at your hands. Not even shaking. Who taught you that? I did. I DID.")))
                    .run(() -> e.act(NorsePose.CLAW))
                    .direction("She digs her fingers into the ground. It doesn't matter. She slides toward you anyway, a little at a time, and the earth comes up in furrows behind her nails.")
                    .line(DEATH, e, "You think a box can hold me? Boxes end. Crystals end. Stars end. YOU end.")
                    .line(DEATH, e, "And when you do, I'll be the one who holds you. Think about that. Think about it every night.")
                    .run(() -> e.act(NorsePose.REACH))
                    .direction("The last of her goes in arms first, reaching for you, the fingers still opening and closing on nothing.")
                    .pause(30)
                    .run(() -> contain(t, player))
                    .pause(30)
                    .direction("Then there's only the Regulator in your hand: warm, much heavier than it was, and something inside it beating on the walls.")
                    .pause(40)
                    .run(() -> {
                        for (EntityPlayerMP p : t.present()) {
                            Obsession.whisper(p, "You'll let me out. Everyone does, in the end. And I'll remember who it was that put me in.");
                        }
                    });
        }
        t.abandoned(this::escape);
        story.talk(player, t);
        return true;
    }

    /** The Death who came back to look at what they are now, and didn't like it: drawn in, and furious about it. */
    private void exScene(Talk t, EntityNorse e, EntityPlayerMP player) {
        t.run(() -> e.act(NorsePose.RECOIL, 0))
                .direction("You lift the Regulator. Its facets catch the light coming off her, and hold on to it.")
                .line(DEATH, e, "What are you doing with that? Why are you pointing that at me?")
                .line(DEATH, e, "Oh, you are NOT serious. The JAR? You're putting me in the jar? With your little collection?")
                .run(() -> e.act(NorsePose.FURY))
                .direction("She plants her feet and leans back against it like someone being walked somewhere they don't want to go. It doesn't help.")
                .line(DEATH, e, "This is SO typical. You get a little bit of power and suddenly I'm a thing on a shelf.")
                .line(DEATH, e, "I liked you better when you were scared of me. Everybody did.")
                .ask(DEATH, e, 20 * 12, 2,
                        say("I'm sorry.", c -> c.line(DEATH, e, "No you're not. And neither am I. And you're going to look SO stupid when I get out of this.")),
                        say("You were never coming back for me.", c -> c.line(DEATH, e, "I came back to LOOK, you absolute... fine. FINE. Yes. Happy?")),
                        act("(Hold it steady.)", c -> c.line(DEATH, e, "Oh, look at you. All steady. Who's a big strong cosmic jailer? Ugh.")))
                .run(() -> e.act(NorsePose.CLAW))
                .direction("She grabs at the ground, at the air, at nothing. Her heels leave two long furrows behind her, all the way to your feet.")
                .line(DEATH, e, story.aftermath.jar() ? "Fine. Put me in your box. Eternity, the Tribunal, and me. A matching set. Is that what I am to you? A SET?"
                        : "Fine. Put me in your box. He let you go, and THIS is what you do with it. Is that what I am to you? Something for the shelf?")
                .line(DEATH, e, "He's going to come for this, you know. Thanos. He's going to take it off you, and I'm going to LAUGH.")
                .run(() -> e.act(NorsePose.REACH))
                .direction("The last of her goes in arms folded, chin up, as if it had been her idea.")
                .pause(30)
                .run(() -> contain(t, player))
                .pause(30)
                .direction("Then there's only the Regulator in your hand: heavier than it was, and something inside it pointedly not speaking to you.")
                .pause(40)
                .run(() -> {
                    for (EntityPlayerMP p : t.present()) {
                        Obsession.whisper(p, "Don't talk to me. I'm not talking to you.");
                    }
                })
                .pause(60)
                .run(() -> {
                    for (EntityPlayerMP p : t.present()) {
                        Obsession.whisper(p, "...I'm still not talking to you.");
                    }
                });
    }

    /** While she is being drawn in: the stream of her into the crystal, her dragged toward it, greying; or, if they let go of it, free. */
    void tick(MinecraftServer server) {
        EntityNorse e = Oath.alive(her);
        if (e == null) {
            her = null;
            return;
        }
        drawTicks++;
        EntityPlayerMP p = holder == null ? null : server.getPlayerList().getPlayerByUUID(holder);
        if (p == null || !p.isEntityAlive() || p.dimension != e.dimension || !ItemAstralRegulator.holdsRegulator(p)) {
            escape();
            return;
        }
        if (drawTicks % 4 == 0) {
            EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.ABSORB, e.posX, e.posY + 1D, e.posZ, 0, 0, 0,
                    p.getEntityId(), 8), e.dimension);
        }
        if (drawTicks % 25 == 0) {
            e.world.playSound(null, e.posX, e.posY, e.posZ, EternitySounds.RIFT_IMPACT, SoundCategory.HOSTILE, 3F,
                    Math.min(1.6F, 0.6F + drawTicks / 1200F));
        }
        e.setAsh(Math.min(0.85F, drawTicks / 1100F));
        Vec3d to = p.getPositionVector().subtract(e.getPositionVector());
        Vec3d flat = new Vec3d(to.x, 0D, to.z);
        if (flat.length() > 1.6D) {
            flat = flat.normalize();
            e.getNavigator().clearPath();
            e.move(MoverType.SELF, flat.x * 0.012D, 0D, flat.z * 0.012D);
        }
    }

    /** All of her, in the crystal; and gone from the world. */
    private void contain(Talk t, EntityPlayerMP fallback) {
        EntityNorse e = Oath.alive(her);
        EntityPlayerMP p = holder == null ? null : StoryDirector.player(holder);
        p = p != null ? p : fallback;
        ItemStack stack = ItemAstralRegulator.raised(p, hand);
        if (e == null || stack.isEmpty()) {
            escape();
            return;
        }
        ItemAstralRegulator.absorb(stack, ItemAstralRegulator.DEATH);
        p.inventoryContainer.detectAndSendChanges();
        StoryData data = story.data();
        data.beats.add(CONTAINED);
        for (StoryData.Player sp : data.players().values()) {
            // Nothing she asked for is owed any more, and she is coming to nobody.
            sp.demandLeft = 0;
            sp.demandBy = -1;
            sp.demandRace = false;
            sp.wronged = false;
            sp.nextVisit = -1;
        }
        data.markDirty();
        // What is left of the scene is about the crystal, not her: it no longer hangs on her being there.
        t.at(null, 0D);
        WorldServer world = (WorldServer) e.world;
        world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, e.posX, e.posY + 1D, e.posZ, 60, 0.3D, 0.9D, 0.3D, 0.04D);
        world.playSound(null, e.posX, e.posY, e.posZ, EternitySounds.COMET_BLAST, SoundCategory.HOSTILE, 6F, 0.6F);
        e.setDead();
        her = null;
        scene = null;
        TextComponentString notice = new TextComponentString(p.getName() + " has drawn Death into the Astral Regulator.");
        notice.getStyle().setColor(TextFormatting.GOLD);
        p.server.getPlayerList().sendMessage(notice);
        story.sendStateToAll();
    }

    /** They let go of it, or walked off, before she was all the way in: she is out, and she will be back, and angry. */
    private void escape() {
        EntityNorse e = Oath.alive(her);
        Talk t = scene;
        her = null;
        scene = null;
        if (e == null) {
            return;
        }
        StoryData data = story.data();
        java.util.List<EntityPlayerMP> them = t != null ? t.present() : new java.util.ArrayList<>();
        EntityPlayerMP p = holder == null ? null : StoryDirector.player(holder);
        if (p != null && !them.contains(p)) {
            them.add(p);
        }
        if (t != null) {
            story.cut(t);
        }
        for (EntityPlayerMP each : them) {
            if (ex) {
                // She is gone for good either way; there is nothing left to be wronged about, only the last word.
                Obsession.whisper(each, "Yeah. That's what I thought.");
                continue;
            }
            data.player(each.getUniqueID()).wronged = true;
            data.player(each.getUniqueID()).spurned++;
            Obsession.whisper(each, "You put it down. Good. That was the cleverest thing you've ever done, and it won't save you. Run.");
        }
        data.markDirty();
        Obsession.vanish(e);
    }
}
