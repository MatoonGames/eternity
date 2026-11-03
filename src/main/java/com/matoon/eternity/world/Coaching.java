package com.matoon.eternity.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

/**
 * Warlock's eye on the board, at their side on the Magus's road: a good move for the king's side
 * and, in a sentence, why it is good. Pure rules, no world; the director lights what he means and
 * puts the words in his mouth. Squares are named as the player sees the board from the pedestal:
 * x runs left to right, y from the far edge to the near one.
 */
public final class Coaching {

    /** How far ahead he looks. Further than the house's raiders would take a fair while. */
    private static final int DEPTH = 3;
    /** How much worse than the best a move may be and still be worth showing for a reason he can say. */
    private static final int SLACK = 160;

    /** What he shows them: the piece, where it goes, what it takes, and what he says about it. */
    public static final class Advice {
        public final int from, to;
        public final List<Integer> takes;
        public final String line;

        Advice(int from, int to, List<Integer> takes, String line) {
            this.from = from;
            this.to = to;
            this.takes = takes;
            this.line = line;
        }
    }

    private Coaching() {
    }

    /** The move he would show them now, with the defenders to move, or null if there is nothing to say. */
    @Nullable
    public static Advice advise(Kingstable game, Random rand, boolean first) {
        if (game.raidersToMove || game.outcome != Kingstable.Outcome.PLAYING || game.king() < 0) {
            return null;
        }
        int[] moves = game.moves();
        if (moves.length == 0) {
            return null;
        }
        int[] score = new int[moves.length];
        int best = 0;
        for (int i = 0; i < moves.length; i++) {
            score[i] = game.moveScore(moves[i], DEPTH);
            if (score[i] > score[best] || score[i] == score[best] && rand.nextBoolean()) {
                best = i;
            }
        }
        int king = game.king();

        // The road is open: nothing else matters.
        int escape = game.escapeSquare();
        if (escape >= 0) {
            return new Advice(king, escape, taken(game, king * Kingstable.CELLS + escape),
                    "The king. Straight to the " + corner(escape) + " corner - nothing in his way. Go, before he sees it.");
        }

        // A raider one move from closing on the king: shut it first.
        Kingstable threat = game.copy();
        threat.raidersToMove = true;
        int kill = threat.killingMove();
        if (kill >= 0) {
            int killTo = kill % Kingstable.CELLS;
            int save = -1;
            for (int pass = 0; pass < 2 && save < 0; pass++) {
                for (int i = 0; i < moves.length; i++) {
                    if (pass == 0 && i != best || !safe(game, moves[i])) {
                        continue;
                    }
                    if (save < 0 || score[i] > score[save]) {
                        save = i;
                    }
                }
            }
            if (save >= 0) {
                int m = moves[save];
                int from = m / Kingstable.CELLS, to = m % Kingstable.CELLS;
                List<Integer> takes = taken(game, m);
                String where = from(killTo, king);
                String why;
                if (from == king) {
                    why = "He's one move from closing on the king, " + where + ". Step him out of it, to the lit square.";
                } else if (to == killTo) {
                    why = "He wants the lit square - a raider there and the king is caught. Put a guard on it first.";
                } else if (takes.contains(kill / Kingstable.CELLS)) {
                    why = "That raider's one move from the king. Take it first: to the lit square, and it's caught between two of yours.";
                } else {
                    why = "He's one move from the king, " + where + ". This shuts the gap - nothing he moves can close on him after it.";
                }
                return new Advice(from, to, takes, why);
            }
        }

        // Two roads at once: he can only shut one.
        for (int i = 0; i < moves.length; i++) {
            int m = moves[i];
            if (m / Kingstable.CELLS != king) {
                continue;
            }
            Kingstable after = after(game, m);
            List<Integer> roads = roads(after);
            if (roads.size() >= 2 && after.killingMove() < 0) {
                return new Advice(king, m % Kingstable.CELLS, taken(game, m), "King to the lit square. Two roads open from there - the "
                        + corner(roads.get(0)) + " corner and the " + corner(roads.get(1)) + ". He can only close one.");
            }
        }

        // A raider caught between two of theirs.
        int capture = -1;
        for (int i = 0; i < moves.length; i++) {
            if (!taken(game, moves[i]).isEmpty() && score[i] >= score[best] - SLACK && safe(game, moves[i])
                    && (capture < 0 || score[i] > score[capture])) {
                capture = i;
            }
        }
        if (capture >= 0) {
            int m = moves[capture];
            List<Integer> takes = taken(game, m);
            String what = takes.size() > 1 ? "two of his raiders fall" : "the raider beside it falls";
            return new Advice(m / Kingstable.CELLS, m % Kingstable.CELLS, takes, "That one, to the lit square, and " + what
                    + " - caught between two of yours. One of yours either side: that's how his come off.");
        }

        // One road: he has to spend his move shutting it.
        for (int i = 0; i < moves.length; i++) {
            int m = moves[i];
            if (m / Kingstable.CELLS != king || score[i] < score[best] - SLACK || !safe(game, m)) {
                continue;
            }
            List<Integer> roads = roads(after(game, m));
            if (roads.size() == 1) {
                return new Advice(king, m % Kingstable.CELLS, taken(game, m), "King to the lit square opens a road to the " + corner(roads.get(0))
                        + " corner. He'll spend his move shutting it, not hunting you.");
            }
        }

        // Otherwise the best of it, and what it does.
        int m = moves[best];
        int from = m / Kingstable.CELLS, to = m % Kingstable.CELLS;
        String why = quiet(game, m, king);
        if (first) {
            why = "The king wins at a corner; the guards clear his road. " + why;
        }
        return new Advice(from, to, taken(game, m), why);
    }

    /** Why a move with nothing loud about it is still the right one. */
    private static String quiet(Kingstable game, int move, int king) {
        int from = move / Kingstable.CELLS, to = move % Kingstable.CELLS;
        Kingstable after = after(game, move);
        int newKing = after.king();
        int roomBefore = game.destinations(king).size(), roomAfter = newKing >= 0 ? after.destinations(newKing).size() : 0;
        if (from == king) {
            if (edge(to) < edge(from)) {
                return "King to the lit square. Nearer the edge, every corner's closer, and he has to spread his raiders thin.";
            }
            return "King to the lit square. He can run to " + squares(roomAfter) + " from there, not " + roomBefore + ".";
        }
        if (adjacent(to, king)) {
            return "That guard, beside the king. While it stands there, no raider can close on him from that side.";
        }
        if (roomAfter > roomBefore) {
            return "Get that guard out of the king's way. It opens his lines - " + squares(roomAfter) + " he can reach, not " + roomBefore + ".";
        }
        if (cornerGuard(to)) {
            return "That guard, by the corner. He wanted a raider there to shut that corner. Now he can't.";
        }
        if (raiderNeighbours(after, to) > 0) {
            return "That guard, up against his raider. One more of yours on its far side next move, and it comes off.";
        }
        return "That guard, to the lit square. It keeps his raiders apart - make him come at the king one at a time.";
    }

    private static String squares(int n) {
        return n == 1 ? "one square" : n + " squares";
    }

    /** The position after a move, the stone going where it was shown going. */
    private static Kingstable after(Kingstable game, int move) {
        Kingstable k = game.copy();
        k.play(move / Kingstable.CELLS, move % Kingstable.CELLS, null);
        return k;
    }

    /** Whether, after this move, no raider can take the king with his next. */
    private static boolean safe(Kingstable game, int move) {
        Kingstable k = after(game, move);
        return k.outcome == Kingstable.Outcome.KING_ESCAPED || k.outcome == Kingstable.Outcome.PLAYING && k.killingMove() < 0;
    }

    /** What a move would take. */
    private static List<Integer> taken(Kingstable game, int move) {
        Kingstable k = game.copy();
        return new ArrayList<>(k.play(move / Kingstable.CELLS, move % Kingstable.CELLS, null).taken);
    }

    /** The refuges the king could reach in one move, as things stand. */
    private static List<Integer> roads(Kingstable game) {
        List<Integer> out = new ArrayList<>();
        int k = game.king();
        if (k >= 0) {
            for (int to : game.destinations(k)) {
                if (Kingstable.isRefuge(to)) {
                    out.add(to);
                }
            }
        }
        return out;
    }

    private static int edge(int sq) {
        int x = Kingstable.x(sq), y = Kingstable.y(sq), n = Kingstable.SIZE - 1;
        return Math.min(Math.min(x, n - x), Math.min(y, n - y));
    }

    private static boolean adjacent(int a, int b) {
        return Math.abs(Kingstable.x(a) - Kingstable.x(b)) + Math.abs(Kingstable.y(a) - Kingstable.y(b)) == 1;
    }

    /** The squares right beside a corner, where one raider shuts it. */
    private static boolean cornerGuard(int sq) {
        int x = Kingstable.x(sq), y = Kingstable.y(sq), n = Kingstable.SIZE - 1;
        boolean xEdge = x == 0 || x == n, yEdge = y == 0 || y == n;
        return xEdge && (y == 1 || y == n - 1) || yEdge && (x == 1 || x == n - 1);
    }

    private static int raiderNeighbours(Kingstable game, int sq) {
        int n = 0;
        int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] s : d) {
            int x = Kingstable.x(sq) + s[0], y = Kingstable.y(sq) + s[1];
            if (Kingstable.inside(x, y) && Kingstable.isRaider(game.cells[Kingstable.at(x, y)])) {
                n++;
            }
        }
        return n;
    }

    /** A corner as they see it from the pedestal. */
    static String corner(int sq) {
        boolean left = Kingstable.x(sq) == 0, far = Kingstable.y(sq) == 0;
        return (far ? "far " : "near ") + (left ? "left" : "right");
    }

    /** Which way a raider would come at the king, as they see the board from their own side of it. */
    static String from(int square, int king) {
        int dx = Kingstable.x(square) - Kingstable.x(king), dy = Kingstable.y(square) - Kingstable.y(king);
        if (Math.abs(dx) >= Math.abs(dy)) {
            return dx < 0 ? "from the left" : "from the right";
        }
        return dy < 0 ? "from the far side" : "from your side";
    }
}
