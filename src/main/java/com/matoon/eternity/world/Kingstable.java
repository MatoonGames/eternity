package com.matoon.eternity.world;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * The Grandmaster's game: the king's table, as he says it should be played. The Norse played
 * it on bone boards as hnefatafl, and he will tell you they learned it from him and got it
 * wrong. Pure rules, no Minecraft in it, so it can be reasoned about on its own.
 * <ul>
 * <li>A board of seven by seven squares. The <b>throne</b> is the centre, the four
 * <b>refuges</b> are the corners.</li>
 * <li>You hold the <b>king</b> and his four <b>guards</b>, round the throne. Your opponent
 * holds eight <b>raiders</b>, two on each edge. Raiders move first.</li>
 * <li>Every piece moves like a rook: any distance along a row or column, never through
 * another piece. Only the king may stop on, or pass over, the throne, and only he may stop on
 * a refuge.</li>
 * <li>A piece is taken when a move closes it between two enemies along a line. Moving between
 * two enemies is safe. A refuge, the empty throne and the Mind Stone's square count as an
 * enemy to everyone. The king is armed: he helps take raiders.</li>
 * <li>The king is taken like anyone else, except on the throne or beside it, where it takes
 * raiders on all four sides (the throne, or the stone, standing in for one).</li>
 * <li><b>The stone stays in play.</b> After every move the Mind Stone leaps to an empty
 * square, and everyone can see where it will go next. Nothing may enter or cross its square.</li>
 * <li>The king wins by reaching a refuge. The raiders win by taking the king, or when the
 * guards have no move to make, or when the game runs past {@link #PLY_LIMIT} moves. The
 * house does not play for draws.</li>
 * </ul>
 */
public final class Kingstable {

    public static final int SIZE = 7;
    public static final int CELLS = SIZE * SIZE;
    public static final int THRONE = 3 * SIZE + 3;
    public static final int PLY_LIMIT = 160;

    public static final byte EMPTY = 0, RAIDER = 1, GUARD = 2, KING = 3;

    /** How a game stands. */
    public enum Outcome { PLAYING, KING_ESCAPED, KING_TAKEN, GUARDS_STUCK, RAIDERS_STUCK, TOO_LONG }

    private static final int[] DX = {1, -1, 0, 0};
    private static final int[] DY = {0, 0, 1, -1};
    private static final int WIN = 1_000_000;

    public final byte[] cells = new byte[CELLS];
    /** The square the Mind Stone is on, and the one it will leap to next. */
    public int stone = -1, nextStone = -1;
    /** Squares that have been broken out of the board: nothing stands on them or crosses them, and they count against everyone. One bit each. */
    public long holes;
    public boolean raidersToMove = true;
    public int ply;
    public Outcome outcome = Outcome.PLAYING;

    public Kingstable() {
    }

    /** The opening position. */
    public static Kingstable opening(Random rand) {
        Kingstable k = new Kingstable();
        k.cells[THRONE] = KING;
        for (int i = 0; i < 4; i++) {
            k.cells[at(3 + DX[i], 3 + DY[i])] = GUARD;
            k.cells[at(3 + DX[i] * 3, 3 + DY[i] * 3)] = RAIDER;
            k.cells[at(3 + DX[i] * 2, 3 + DY[i] * 2)] = RAIDER;
        }
        k.stone = k.randomFree(rand, -1);
        k.nextStone = k.randomFree(rand, k.stone);
        return k;
    }

    public Kingstable copy() {
        Kingstable k = new Kingstable();
        System.arraycopy(cells, 0, k.cells, 0, CELLS);
        k.stone = stone;
        k.nextStone = nextStone;
        k.raidersToMove = raidersToMove;
        k.ply = ply;
        k.holes = holes;
        k.outcome = outcome;
        return k;
    }

    // ------------------------------------------------------------------ geometry

    public static int at(int x, int y) {
        return y * SIZE + x;
    }

    public static int x(int square) {
        return square % SIZE;
    }

    public static int y(int square) {
        return square / SIZE;
    }

    public static boolean inside(int x, int y) {
        return x >= 0 && y >= 0 && x < SIZE && y < SIZE;
    }

    public static boolean isRefuge(int square) {
        int x = x(square), y = y(square);
        return (x == 0 || x == SIZE - 1) && (y == 0 || y == SIZE - 1);
    }

    public static boolean isThrone(int square) {
        return square == THRONE;
    }

    /** Whether a square is one of the king's, where only he may stand. */
    public boolean isHole(int square) {
        return square >= 0 && (holes >>> square & 1L) != 0L;
    }

    public void breakOut(int square) {
        holes |= 1L << square;
        cells[square] = EMPTY;
        if (stone == square) {
            stone = -1;
        }
    }

    public static boolean isRestricted(int square) {
        return isThrone(square) || isRefuge(square);
    }

    public static boolean isRaider(byte piece) {
        return piece == RAIDER;
    }

    public static boolean isDefender(byte piece) {
        return piece == GUARD || piece == KING;
    }

    public int king() {
        for (int i = 0; i < CELLS; i++) {
            if (cells[i] == KING) {
                return i;
            }
        }
        return -1;
    }

    public int count(byte piece) {
        int n = 0;
        for (byte b : cells) {
            if (b == piece) {
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------ moves

    /** Every square the piece on {@code from} could move to, in order along each line. */
    public List<Integer> destinations(int from) {
        List<Integer> out = new ArrayList<>();
        if (from < 0 || from >= CELLS) {
            return out;
        }
        byte piece = cells[from];
        if (piece == EMPTY) {
            return out;
        }
        boolean king = piece == KING;
        for (int d = 0; d < 4; d++) {
            int x = x(from) + DX[d], y = y(from) + DY[d];
            while (inside(x, y)) {
                int sq = at(x, y);
                if (cells[sq] != EMPTY || sq == stone || isHole(sq)) {
                    break;
                }
                if (isThrone(sq) && !king) {
                    // Over the empty throne, but never onto it.
                    x += DX[d];
                    y += DY[d];
                    continue;
                }
                if (!isRefuge(sq) || king) {
                    out.add(sq);
                }
                x += DX[d];
                y += DY[d];
            }
        }
        return out;
    }

    public boolean isLegal(int from, int to) {
        if (outcome != Outcome.PLAYING || from < 0 || from >= CELLS || cells[from] == EMPTY) {
            return false;
        }
        if (isRaider(cells[from]) != raidersToMove) {
            return false;
        }
        return destinations(from).contains(to);
    }

    /** Every legal move for the side to move, as from * CELLS + to; captures first, which helps the search. */
    public int[] moves() {
        int[] buffer = new int[256];
        int n = 0;
        int captures = 0;
        for (int from = 0; from < CELLS; from++) {
            byte piece = cells[from];
            if (piece == EMPTY || isRaider(piece) != raidersToMove) {
                continue;
            }
            for (int to : destinations(from)) {
                int move = from * CELLS + to;
                if (wouldCapture(from, to)) {
                    // Captures go to the front.
                    buffer[n++] = buffer[captures];
                    buffer[captures++] = move;
                } else {
                    buffer[n++] = move;
                }
            }
        }
        return Arrays.copyOf(buffer, n);
    }

    private boolean wouldCapture(int from, int to) {
        byte piece = cells[from];
        cells[from] = EMPTY;
        cells[to] = piece;
        boolean any = false;
        for (int d = 0; d < 4 && !any; d++) {
            any = capturable(to, d, piece);
        }
        cells[to] = EMPTY;
        cells[from] = piece;
        return any;
    }

    /** What a move did: the squares whose pieces it took, and how the game stands after. */
    public static final class Result {
        public final int from, to;
        public final List<Integer> taken = new ArrayList<>();
        public Outcome outcome = Outcome.PLAYING;

        Result(int from, int to) {
            this.from = from;
            this.to = to;
        }
    }

    /**
     * Plays a move, which must be legal. The stone then leaps to where everyone saw it would,
     * and the next leap is chosen from {@code rand}; pass null to leave the stone where it
     * lands next (for looking ahead, where nobody knows where it goes after that).
     */
    public Result play(int from, int to, Random rand) {
        Result r = new Result(from, to);
        byte piece = cells[from];
        cells[from] = EMPTY;
        cells[to] = piece;
        for (int d = 0; d < 4; d++) {
            if (capturable(to, d, piece)) {
                int sq = at(x(to) + DX[d], y(to) + DY[d]);
                cells[sq] = EMPTY;
                r.taken.add(sq);
            }
        }
        if (piece == KING && isRefuge(to)) {
            outcome = Outcome.KING_ESCAPED;
        } else if (isRaider(piece) && kingSurrounded()) {
            int k = king();
            cells[k] = EMPTY;
            r.taken.add(k);
            outcome = Outcome.KING_TAKEN;
        }
        ply++;
        raidersToMove = !raidersToMove;
        if (rand != null) {
            leap(rand);
        } else if (nextStone >= 0 && cells[nextStone] == EMPTY) {
            stone = nextStone;
        }
        if (outcome == Outcome.PLAYING && moves().length == 0) {
            outcome = raidersToMove ? Outcome.RAIDERS_STUCK : Outcome.GUARDS_STUCK;
        }
        if (outcome == Outcome.PLAYING && ply >= PLY_LIMIT) {
            outcome = Outcome.TOO_LONG;
        }
        r.outcome = outcome;
        return r;
    }

    /** The stone goes where it was going, and picks where it goes after. Never onto a piece. */
    public void leap(Random rand) {
        stone = nextStone >= 0 && cells[nextStone] == EMPTY ? nextStone : randomFree(rand, stone);
        nextStone = randomFree(rand, stone);
    }

    /** A random empty square that is not the king's, nor {@code not}. */
    public int randomFree(Random rand, int not) {
        int[] free = new int[CELLS];
        int n = 0;
        for (int i = 0; i < CELLS; i++) {
            if (cells[i] == EMPTY && !isRestricted(i) && i != not && !isHole(i)) {
                free[n++] = i;
            }
        }
        return n == 0 ? -1 : free[rand.nextInt(n)];
    }

    /** Whether the piece next to {@code to} in direction d is closed in by the piece that just arrived. */
    private boolean capturable(int to, int d, byte mover) {
        int nx = x(to) + DX[d], ny = y(to) + DY[d];
        int fx = nx + DX[d], fy = ny + DY[d];
        if (!inside(nx, ny)) {
            return false;
        }
        int victim = at(nx, ny);
        byte v = cells[victim];
        if (v == EMPTY || isRaider(v) == isRaider(mover)) {
            return false;
        }
        if (v == KING) {
            // The king is only taken by the raiders' own check, after their move.
            return false;
        }
        return inside(fx, fy) && hostile(at(fx, fy), v);
    }

    /** Whether a square counts as an enemy of the given piece. */
    private boolean hostile(int square, byte to) {
        byte there = cells[square];
        if (there != EMPTY) {
            return isRaider(there) != isRaider(to);
        }
        return isRefuge(square) || isThrone(square) || square == stone || isHole(square);
    }

    /** Whether the king is closed in, by the rule for wherever he stands. */
    public boolean kingSurrounded() {
        int k = king();
        if (k < 0) {
            return false;
        }
        int kx = x(k), ky = y(k);
        boolean nearThrone = Math.abs(kx - 3) + Math.abs(ky - 3) <= 1;
        if (nearThrone) {
            for (int d = 0; d < 4; d++) {
                int sq = at(kx + DX[d], ky + DY[d]);
                if (!(cells[sq] == RAIDER || isThrone(sq) || sq == stone || isHole(sq))) {
                    return false;
                }
            }
            return true;
        }
        for (int axis = 0; axis < 2; axis++) {
            int d = axis * 2;
            int ax = kx + DX[d], ay = ky + DY[d], bx = kx - DX[d], by = ky - DY[d];
            if (inside(ax, ay) && inside(bx, by) && kingHostile(at(ax, ay)) && kingHostile(at(bx, by))) {
                return true;
            }
        }
        return false;
    }

    private boolean kingHostile(int square) {
        return cells[square] == RAIDER || isRefuge(square) || square == stone || isHole(square);
    }

    // ------------------------------------------------------------------ what the house watches for

    /** Refuges the king could step onto with his next move, if it were his. */
    public int escapes() {
        int k = king();
        if (k < 0) {
            return 0;
        }
        int n = 0;
        for (int to : destinations(k)) {
            if (isRefuge(to)) {
                n++;
            }
        }
        return n;
    }

    /** The first refuge the king could reach right now, or -1. */
    public int escapeSquare() {
        int k = king();
        if (k < 0) {
            return -1;
        }
        for (int to : destinations(k)) {
            if (isRefuge(to)) {
                return to;
            }
        }
        return -1;
    }

    /** A raiders' move that would take the king right now, or -1. */
    public int killingMove() {
        if (!raidersToMove) {
            return -1;
        }
        for (int move : moves()) {
            Kingstable k = copy();
            k.play(move / CELLS, move % CELLS, null);
            if (k.outcome == Outcome.KING_TAKEN) {
                return move;
            }
        }
        return -1;
    }

    /** Whether the defenders, to move, can win this move whatever happens. */
    public boolean kingCanEscapeNow() {
        return !raidersToMove && escapes() > 0;
    }

    // ------------------------------------------------------------------ playing the raiders

    /**
     * The best move for whoever is to move, looking {@code depth} moves ahead, or -1 if there is none. The
     * stone is taken to go where it is shown going, and then to stay.
     */
    public int bestMove(int depth, Random rand) {
        int[] moves = moves();
        if (moves.length == 0) {
            return -1;
        }
        // Shuffle among equals so he never plays the same game twice.
        for (int i = moves.length - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            if (i != j && !(wouldCapture(moves[i] / CELLS, moves[i] % CELLS) || wouldCapture(moves[j] / CELLS, moves[j] % CELLS))) {
                int t = moves[i];
                moves[i] = moves[j];
                moves[j] = t;
            }
        }
        int best = moves[0];
        int bestScore = Integer.MIN_VALUE;
        int alpha = -WIN * 2, beta = WIN * 2;
        for (int move : moves) {
            Kingstable k = copy();
            k.play(move / CELLS, move % CELLS, null);
            int score = -k.search(depth - 1, -beta, -alpha);
            if (score > bestScore) {
                bestScore = score;
                best = move;
            }
            alpha = Math.max(alpha, score);
        }
        return best;
    }

    /** Negamax with alpha-beta, scored for the side to move. */
    private int search(int depth, int alpha, int beta) {
        if (outcome != Outcome.PLAYING || depth <= 0) {
            int s = evaluate();
            return raidersToMove ? s : -s;
        }
        int[] moves = moves();
        int best = -WIN * 2;
        for (int move : moves) {
            Kingstable k = copy();
            k.play(move / CELLS, move % CELLS, null);
            int score = -k.search(depth - 1, -beta, -alpha);
            if (score > best) {
                best = score;
            }
            if (best > alpha) {
                alpha = best;
            }
            if (alpha >= beta) {
                break;
            }
        }
        return best;
    }

    /** How good the position is for the raiders. */
    public int evaluate() {
        switch (outcome) {
            case KING_ESCAPED:
            case RAIDERS_STUCK:
                return -WIN + ply;
            case KING_TAKEN:
            case GUARDS_STUCK:
            case TOO_LONG:
                return WIN - ply;
            default:
                break;
        }
        int k = king();
        int kx = x(k), ky = y(k);
        int score = count(RAIDER) * 140 - count(GUARD) * 110;
        // How many moves the king needs to reach each refuge, through the squares free now.
        int[] dist = kingDistances(k);
        boolean kingsMove = !raidersToMove;
        int one = 0, two = 0, three = 0;
        for (int r : REFUGES) {
            if (dist[r] == 1) {
                one++;
            } else if (dist[r] == 2) {
                two++;
            } else if (dist[r] == 3) {
                three++;
            }
        }
        if (one > 0) {
            // An open road is as good as lost if it is his move, or if there are two of them.
            score -= kingsMove || one > 1 ? WIN / 2 : 900;
        }
        score -= two * (kingsMove ? 260 : 170) + three * 45;
        // Every square the king could reach in one move is room to slip away through.
        score -= destinations(k).size() * 14;
        // Raiders closing in round the king; guards keeping them off.
        for (int d = 0; d < 4; d++) {
            int nx = kx + DX[d], ny = ky + DY[d];
            if (inside(nx, ny)) {
                byte b = cells[at(nx, ny)];
                if (b == RAIDER) {
                    score += 70;
                } else if (b == GUARD) {
                    score -= 20;
                }
            }
        }
        // Raiders shutting the refuges: the squares beside a corner close it outright.
        for (int[] b : CORNER_GUARDS) {
            if (cells[at(b[0], b[1])] == RAIDER) {
                score += b[2] == 1 ? 60 : 30;
            }
        }
        // The further the king is from the edge, the safer the raiders.
        int edge = Math.min(Math.min(kx, SIZE - 1 - kx), Math.min(ky, SIZE - 1 - ky));
        score += edge * 35;
        return score;
    }

    private static final int[] REFUGES = {at(0, 0), at(SIZE - 1, 0), at(0, SIZE - 1), at(SIZE - 1, SIZE - 1)};
    /** x, y, and 1 for the squares beside a corner, 2 for the one diagonal to it. */
    private static final int[][] CORNER_GUARDS = {{1, 0, 1}, {0, 1, 1}, {5, 0, 1}, {6, 1, 1}, {0, 5, 1}, {1, 6, 1}, {6, 5, 1}, {5, 6, 1},
            {1, 1, 2}, {5, 1, 2}, {1, 5, 2}, {5, 5, 2}};

    /** Rook moves the king needs to reach every square, if nothing else moved; 99 for out of reach in three. */
    private int[] kingDistances(int k) {
        int[] dist = new int[CELLS];
        Arrays.fill(dist, 99);
        if (k < 0) {
            return dist;
        }
        int[] queue = new int[CELLS];
        int head = 0, tail = 0;
        dist[k] = 0;
        queue[tail++] = k;
        while (head < tail) {
            int from = queue[head++];
            if (dist[from] >= 3) {
                continue;
            }
            for (int d = 0; d < 4; d++) {
                int x = x(from) + DX[d], y = y(from) + DY[d];
                while (inside(x, y)) {
                    int sq = at(x, y);
                    if (cells[sq] != EMPTY && sq != k || sq == stone || isHole(sq)) {
                        break;
                    }
                    if (dist[sq] == 99) {
                        dist[sq] = dist[from] + 1;
                        if (!isRefuge(sq)) {
                            queue[tail++] = sq;
                        }
                    }
                    x += DX[d];
                    y += DY[d];
                }
            }
        }
        return dist;
    }

    // ------------------------------------------------------------------ saving

    public int[] save() {
        int[] out = new int[CELLS + 7];
        for (int i = 0; i < CELLS; i++) {
            out[i] = cells[i];
        }
        out[CELLS] = stone;
        out[CELLS + 1] = nextStone;
        out[CELLS + 2] = raidersToMove ? 1 : 0;
        out[CELLS + 3] = ply;
        out[CELLS + 4] = outcome.ordinal();
        out[CELLS + 5] = (int) holes;
        out[CELLS + 6] = (int) (holes >>> 32);
        return out;
    }

    public static Kingstable load(int[] data) {
        Kingstable k = new Kingstable();
        if (data.length < CELLS + 5) {
            return opening(new Random());
        }
        for (int i = 0; i < CELLS; i++) {
            k.cells[i] = (byte) data[i];
        }
        k.stone = data[CELLS];
        k.nextStone = data[CELLS + 1];
        k.raidersToMove = data[CELLS + 2] != 0;
        k.ply = data[CELLS + 3];
        k.outcome = Outcome.values()[Math.max(0, Math.min(Outcome.values().length - 1, data[CELLS + 4]))];
        if (data.length >= CELLS + 7) {
            k.holes = (data[CELLS + 5] & 0xFFFFFFFFL) | (long) data[CELLS + 6] << 32;
        }
        return k;
    }

    /** For logs and tests: R raiders, G guards, K king, * the stone, o where it goes next, + the king's squares. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int sq = at(x, y);
                char c = cells[sq] == RAIDER ? 'R' : cells[sq] == GUARD ? 'G' : cells[sq] == KING ? 'K'
                        : sq == stone ? '*' : sq == nextStone ? 'o' : isRestricted(sq) ? '+' : '.';
                sb.append(c).append(' ');
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
