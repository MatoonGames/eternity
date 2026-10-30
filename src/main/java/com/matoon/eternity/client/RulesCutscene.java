package com.matoon.eternity.client;

import com.matoon.eternity.client.Cutscene.Cue;
import com.matoon.eternity.client.Cutscene.Key;
import com.matoon.eternity.client.gui.SpeakerPanel;
import com.matoon.eternity.client.render.GrandmasterBoard;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgGrandmaster;
import com.matoon.eternity.network.MsgGrandmasterAct;
import com.matoon.eternity.world.GrandmasterDirector;
import com.matoon.eternity.world.GrandmasterScenes;
import com.matoon.eternity.world.Kingstable;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

/**
 * The Grandmaster teaches the king's table, and his board shows it as he speaks. The camera
 * cranes down over the board, swings past him as he laughs at the Norse, drops low among the
 * king's guards, follows a guard down its row, watches a raider closed in and taken, sweeps the
 * corners, rides with the king as he runs for one, looks at what is staked, watches the stone
 * leap, and comes back round him as he bows, into the player's own eyes.
 * <p>
 * Lines and moves are keyed to {@link GrandmasterScenes#RULES}, which the server uses to time
 * his gestures, so what he does and what the board does land together.
 */
@SideOnly(Side.CLIENT)
public final class RulesCutscene {

    private RulesCutscene() {
    }

    public static void play() {
        Minecraft mc = Minecraft.getMinecraft();
        GrandmasterBoard board = GrandmasterBoard.INSTANCE;
        if (!board.active || mc.player == null) {
            EternityNetwork.sendToServer(new MsgGrandmasterAct(-2));
            return;
        }
        Vec3d m = board.middle();
        float wide = Math.max(60F, mc.gameSettings.fovSetting - 4F);

        List<Key> keys = new ArrayList<>();
        // Craning down over the whole board.
        keys.add(new Key(0, m.add(-16, 22, 21), m, wide));
        keys.add(new Key(85, m.add(5, 11, 16), m.add(1, 0.5, 1), wide - 4));
        // Past him as he laughs.
        keys.add(new Key(150, sq(4, 4).add(3.4, 2.5, 4.6), sq(4, 4).add(0, 2.05, 0), 44));
        // Low among the king's guards.
        keys.add(new Key(205, sq(1, 5).add(0, 2.4, 0), sq(3, 3).add(0, 1.7, 0), 48));
        // Up and back over the raiders.
        keys.add(new Key(255, m.add(-10, 9, 9), m, wide - 2));
        // Down the row with the guard.
        keys.add(new Key(305, sq(4, 4).add(1, 3.6, 7.5), sq(5, 4).add(-0.5, 1.2, 0), 56));
        // Up against the raider in its way.
        keys.add(new Key(355, sq(6, 4).add(4.5, 3.2, 3), sq(5, 3).add(0, 1.4, 0), 54));
        // The trap closing on it.
        keys.add(new Key(415, sq(6, 2).add(5.5, 4.5, -2.5), sq(5, 3).add(0, 1, 0), 55));
        // A sweep across the corners.
        keys.add(new Key(470, sq(6, 6).add(4.5, 4, 4.5), sq(6, 6).add(0, 0.8, 0), 56));
        keys.add(new Key(520, m.add(0, 17, 15), m, wide));
        // Riding with the king to his corner.
        keys.add(new Key(565, sq(1, 4).add(-3, 7.5, 6), sq(1, 2).add(0, 1, 0), 58));
        keys.add(new Key(615, sq(0, 0).add(-4.5, 3.4, -4.5), sq(0, 0).add(0.5, 1.5, 0.5), 50));
        // What is staked.
        keys.add(new Key(665, m.add(3.5, 3.6, 17.5), m.add(0, 3, 13), 50));
        // The stone.
        keys.add(new Key(720, sq(5, 5).add(3.5, 2.4, 4.2), sq(5, 5).add(0, 1.25, 0), 50));
        keys.add(new Key(770, sq(3, 5).add(0, 4.5, 8), sq(2, 4).add(0, 1, 0), 60));
        // Round him as he bows, and home.
        keys.add(new Key(815, sq(4, 4).add(-1, 3.4, 7), sq(4, 4).add(0, 1.8, 0), 55));
        keys.add(Cutscene.INSTANCE.home(GrandmasterScenes.RULES_LENGTH));

        List<Cue> cues = new ArrayList<>();
        cues.add(Cutscene.cue(-GrandmasterScenes.PREROLL + 2, () -> {
            board.beginDemo();
            board.demoStone(Kingstable.at(5, 5), Kingstable.at(1, 5));
        }));
        for (GrandmasterScenes.Beat beat : GrandmasterScenes.RULES) {
            cues.add(Cutscene.cue(beat.tick, () -> SpeakerPanel.INSTANCE.say(GrandmasterScenes.GRANDMASTER, board.gmId, beat.line)));
        }
        int[] defenders = {Kingstable.THRONE, Kingstable.at(3, 2), Kingstable.at(2, 3), Kingstable.at(4, 3), Kingstable.at(3, 4)};
        int[] raiders = {Kingstable.at(3, 0), Kingstable.at(3, 1), Kingstable.at(0, 3), Kingstable.at(1, 3), Kingstable.at(5, 3),
                Kingstable.at(6, 3), Kingstable.at(3, 5), Kingstable.at(3, 6)};
        cues.add(Cutscene.cue(182, () -> board.demoGlow(1, 44, defenders)));
        cues.add(Cutscene.cue(227, () -> board.demoGlow(2, 44, raiders)));
        // Straight lines: a guard along its row.
        cues.add(Cutscene.cue(282, () -> board.demoGlow(4, 40, Kingstable.at(4, 4), Kingstable.at(5, 4), Kingstable.at(6, 4))));
        cues.add(Cutscene.cue(298, () -> board.demoMove(Kingstable.at(3, 4), Kingstable.at(5, 4), GrandmasterDirector.ANIM_SLIDE, 22)));
        cues.add(Cutscene.cue(320, () -> board.demoLand(Kingstable.at(3, 4), Kingstable.at(5, 4))));
        // Nothing jumped: the raider in its way.
        cues.add(Cutscene.cue(338, () -> {
            board.demoGlow(3, 30, Kingstable.at(5, 3));
            board.demoFx(MsgGrandmaster.FX_REFUSE, sq(5, 3), Kingstable.at(5, 3));
        }));
        // Closed between two: taken.
        cues.add(Cutscene.cue(378, () -> board.demoGlow(2, 30, Kingstable.at(5, 3))));
        cues.add(Cutscene.cue(384, () -> board.demoMove(Kingstable.at(3, 2), Kingstable.at(5, 2), GrandmasterDirector.ANIM_SLIDE, 22)));
        cues.add(Cutscene.cue(406, () -> board.demoLand(Kingstable.at(3, 2), Kingstable.at(5, 2))));
        cues.add(Cutscene.cue(410, () -> board.demoTake(Kingstable.at(5, 3))));
        // What counts against everyone.
        cues.add(Cutscene.cue(456, () -> {
            board.demoGlow(4, 70, Kingstable.at(0, 0), Kingstable.at(6, 0), Kingstable.at(0, 6), Kingstable.at(6, 6), Kingstable.THRONE);
            board.demoGlow(2, 70, Kingstable.at(5, 5));
        }));
        // The king runs for a corner.
        cues.add(Cutscene.cue(540, () -> board.demoMove(Kingstable.THRONE, Kingstable.at(3, 2), GrandmasterDirector.ANIM_SLIDE, 16)));
        cues.add(Cutscene.cue(556, () -> board.demoLand(Kingstable.THRONE, Kingstable.at(3, 2))));
        cues.add(Cutscene.cue(558, () -> board.demoGlow(1, 50, Kingstable.at(2, 2), Kingstable.at(1, 2), Kingstable.at(0, 2), Kingstable.at(0, 1),
                Kingstable.at(0, 0))));
        cues.add(Cutscene.cue(562, () -> board.demoMove(Kingstable.at(3, 2), Kingstable.at(0, 2), GrandmasterDirector.ANIM_SLIDE, 22)));
        cues.add(Cutscene.cue(584, () -> board.demoLand(Kingstable.at(3, 2), Kingstable.at(0, 2))));
        cues.add(Cutscene.cue(588, () -> board.demoMove(Kingstable.at(0, 2), Kingstable.at(0, 0), GrandmasterDirector.ANIM_SLIDE, 16)));
        cues.add(Cutscene.cue(604, () -> {
            board.demoLand(Kingstable.at(0, 2), Kingstable.at(0, 0));
            board.demoFx(MsgGrandmaster.FX_STAKE, sq(0, 0).add(0, 1.2, 0), 0);
            board.demoFx(MsgGrandmaster.FX_CARDS, sq(0, 0).add(0, 2.5, 0), 16);
            board.demoGlow(1, 50, Kingstable.at(0, 0));
        }));
        // The stone leaps, and shows where it goes next.
        cues.add(Cutscene.cue(712, () -> board.demoStone(Kingstable.at(1, 5), Kingstable.at(2, 1))));
        cues.add(Cutscene.cue(752, () -> board.demoStone(Kingstable.at(2, 1), Kingstable.at(5, 1))));
        // The real board back, while the camera is on him.
        cues.add(Cutscene.cue(800, board::endDemo));

        Cutscene.INSTANCE.play(keys, cues, GrandmasterScenes.PREROLL, GrandmasterScenes.RULES_LENGTH, () -> {
            board.endDemo();
            EternityNetwork.sendToServer(new MsgGrandmasterAct(-2));
        });
    }

    private static Vec3d sq(int x, int y) {
        return GrandmasterBoard.INSTANCE.centre(Kingstable.at(x, y));
    }
}
