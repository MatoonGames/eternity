package com.matoon.eternity.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * The Living Tribunal's attacks, Master Order's and Lord Chaos's, Adam Warlock's, and the Champion of the
 * Universe's, for everyone who can see them.
 */
public class MsgTribunalFx implements IMessage {

    /** A golden sigil of judgement burns onto the floor at (x,y,z). a = radius, time = ticks until it strikes. */
    public static final byte SIGIL = 0;
    /** His fist comes down at (x,y,z). a = reach of the blow. */
    public static final byte GAVEL = 1;
    /** The light of his eyes, from (x,y,z) to (a,b,c), for this instant. id = tenths of a block between the two beams. */
    public static final byte GAZE = 2;
    /** Lightning from the void in his chest, from (x,y,z) to (a,b,c). */
    public static final byte BOLT = 3;
    /** He is being drawn into the Regulator: light streams from (x,y,z) to the hand of entity {@link #id}. */
    public static final byte ABSORB = 4;
    /** He falls to his knees at (x,y,z). */
    public static final byte FALL = 5;
    /** He rises again, healed, at (x,y,z). id = the ascension he rises to. */
    public static final byte RISE = 6;
    /** A great scale-pan of light comes down on (x,y,z). a = radius, time = ticks until it lands. */
    public static final byte PAN = 7;
    /** A cage of light closes around (x,y,z). a = radius, time = ticks until it bursts. */
    public static final byte CAGE = 8;
    /** The floor at (x,y,z) opens onto the void. a = radius, time = how long it stays open. */
    public static final byte VOID = 9;
    /** Golden blades circle (x,y,z) and close in. a = radius, time = ticks until they meet. */
    public static final byte BLADES = 10;
    /** A pillar of light erupts at (x,y,z). a = height. */
    public static final byte PILLAR = 11;
    /** A ring of light races out across the floor from (x,y,z). a = how far it goes, time = ticks it takes. */
    public static final byte NOVA = 12;
    /** A wall of light sweeps from z to c, along the floor at y, leaving a gap at x. time = ticks it takes. */
    public static final byte SWEEP = 13;
    /** A ghost of him appears at (x,y,z) facing a degrees, and brings his fist down. time = how long it lasts. */
    public static final byte ECHO = 14;
    /** Crushing weight bears down on everything around (x,y,z). a = radius, time = how long. */
    public static final byte GRAVITY = 15;
    /** Lord Chaos: a patch of the world at (x,y,z) flickers onto deep space, then bursts. a = radius, time = ticks until it bursts. */
    public static final byte CHAOS_BURST = 16;
    /** Lord Chaos inhales: space streams from around (x,y,z) into his maw at (a,b,c). time = how long. */
    public static final byte CHAOS_MAW = 17;
    /** Lord Chaos flings something from (x,y,z) to (a,b,c). */
    public static final byte CHAOS_SCATTER = 18;
    /** Master Order lays down an edict for everyone at the junction. id = which (0 stand still, 1 do not strike, 2 look upon me), time = how long it holds. */
    public static final byte ORDER_EDICT = 19;
    /** A straight golden bolt of punishment falls on (x,y,z), for breaking his edict. */
    public static final byte ORDER_BOLT = 20;
    /** A five-by-five grid of golden tiles around (x,y,z), a blocks across each; id = which tiles are lit, one bit each. time = ticks until they erupt. */
    public static final byte ORDER_TILES = 21;
    /** The lit tiles of a grid erupt. Same fields as ORDER_TILES. */
    public static final byte ORDER_ERUPT = 22;
    /** A golden line is drawn across the floor from (x,y,z) to (a,b,c); everything is pulled toward it. time = how long. */
    public static final byte ORDER_LINE = 23;
    /** A beam of judgement runs the length of the line from (x,y,z) to (a,b,c). */
    public static final byte ORDER_BEAM = 24;
    /** Lord Chaos rolls two vast dice, landing at (x,y,z). id = the two faces, as tens and units. time = how long they tumble. */
    public static final byte CHAOS_DICE = 25;
    /** A star falls from (x,y,z) onto (a,b,c). time = how long it falls. */
    public static final byte CHAOS_STAR = 26;
    /** A little rift of deep space wanders at (x,y,z), a across. Sent every couple of ticks while it lives. */
    public static final byte CHAOS_WANDER = 27;
    /** Chaos's beam of deep space, from his maw at (x,y,z) to (a,b,c), for this instant. */
    public static final byte CHAOS_BEAM = 28;
    /** One of the In-Betweener's paradoxes takes hold at (x,y,z): two rings, one white and one black, turning through each other. a = radius. */
    public static final byte PARADOX = 29;

    // Adam Warlock. Everything from WARLOCK_BOOM on is his.
    /** He breaks the sound barrier at (x,y,z), leaving along (a,b,c). time = how long the ring lasts. */
    public static final byte WARLOCK_BOOM = 30;
    /** A blow lands at (x,y,z), driving along (a,b,c). id = who it struck (-1 for nobody), time = 0 a jab, 1 a big hit, 2 a hammer. */
    public static final byte WARLOCK_IMPACT = 31;
    /** He stops dead in the air at (x,y,z), and the air keeps going. a = how far the ring goes. */
    public static final byte WARLOCK_LAND = 32;
    /** Sparks of power stream into (x,y,z) from a around, on entity {@link #id}, for time ticks. */
    public static final byte WARLOCK_CHARGE = 33;
    /** A bolt of quantum energy leaves (x,y,z) at (a,b,c) blocks a tick. id = the bolt, time = how long it can fly. */
    public static final byte WARLOCK_BOLT = 34;
    /** Bolt {@link #id} bursts at (x,y,z), a across. */
    public static final byte WARLOCK_BURST = 35;
    /** His beam, from his palms at (x,y,z) to (a,b,c), for this instant. id = the tick it is in, for the spiral. */
    public static final byte WARLOCK_BEAM = 36;
    /** A ring of gold on the ground at (x,y,z), a across: where he will land, in time ticks. */
    public static final byte WARLOCK_MARK = 37;
    /** He lands at (x,y,z), fists first. a = how far the ground shakes. */
    public static final byte WARLOCK_SLAM = 38;
    /** He is gone from (x,y,z) and at (a,b,c) in the same instant. */
    public static final byte WARLOCK_BLINK = 39;
    /** His cocoon hatches at (x,y,z). a = how far it throws its light; id = him. */
    public static final byte WARLOCK_EMERGE = 40;
    /** His cocoon is broken open at (x,y,z) before it was ready. */
    public static final byte WARLOCK_SHATTER = 41;

    // The Champion of the Universe. Everything from CHAMPION_IMPACT on is his.
    /** A blow lands at (x,y,z), driving along (a,b,c). id = who it struck (-1 for nobody), time = 0 a jab, 1 a big hit, 2 a haymaker. */
    public static final byte CHAMPION_IMPACT = 42;
    /** A blow of yours meets his guard at (x,y,z), facing along (a,b,c). time = 0 blocked, 1 slipped, 2 parried. */
    public static final byte CHAMPION_GUARD = 43;
    /** Shockwave rings race out across the ground from (x,y,z). a = how far, time = ticks; id = how many rings. */
    public static final byte CHAMPION_QUAKE = 44;
    /** The ground cracks open at (x,y,z) toward (a,b,c). time = how long the cracks glow; id = a seed. */
    public static final byte CHAMPION_CRACK = 45;
    /** He roars at (x,y,z): a ring of distorted air, a = how far. id = him. */
    public static final byte CHAMPION_ROAR = 46;
    /** He leaves the ground at (x,y,z) for (a,b,c): dust kicked up, and a ring where he jumped. time = how long he flies. */
    public static final byte CHAMPION_LEAP = 47;
    /** Purple arcs crawl from the stone in his brow, entity {@link #id}, for time ticks. a = how wild, 0 to 1. */
    public static final byte CHAMPION_ARCS = 48;
    /** Planet Breaker: his fists come down at (x,y,z). a = how far the world breaks. */
    public static final byte CHAMPION_BREAK = 49;
    /** A ring of purple marks the ground at (x,y,z), a across: where he will land, in time ticks. */
    public static final byte CHAMPION_MARK = 50;
    /** The stone's light flies from his brow at (x,y,z) to the hand of entity {@link #id}. */
    public static final byte CHAMPION_STONE = 51;
    /** Rocket entity {@link #id} fires its engine, for time ticks. a = 0 idling, 1 lifting off. */
    public static final byte CHAMPION_EXHAUST = 52;
    /** He shoulder-charges from (x,y,z) along (a,b,c): dust and a bow wave of air. time = how long. */
    public static final byte CHAMPION_RUSH = 53;
    /** Tamarata's state, for whoever is there: id = 1 if it is broken, 0 if it is whole. */
    public static final byte CHAMPION_WORLD = 54;

    // Odin's Vault: its guards, the Destroyer, the Casket of Ancient Winters. Everything from VAULT_STATE on is the vault's.
    /** What stands where in the vault, for whoever is there: id = 1 if the Casket is on its pedestal. */
    public static final byte VAULT_STATE = 55;
    /** The Destroyer wakes in its niche: entity {@link #id}. */
    public static final byte DESTROYER_WAKE = 56;
    /** The Destroyer's foot comes down at (x,y,z). a = how hard, 0 to 1. */
    public static final byte DESTROYER_STEP = 57;
    /** A blow lands at (x,y,z), driving along (a,b,c). id = who it struck (-1 for nobody), time = 0 a punch, 1 a backhand, 2 both fists. */
    public static final byte DESTROYER_IMPACT = 58;
    /** Its fists come down at (x,y,z): rings of force race out across the floor. a = how far, time = ticks. */
    public static final byte DESTROYER_QUAKE = 59;
    /** Its face opens and heat gathers in it: entity {@link #id}, for time ticks. */
    public static final byte DESTROYER_CHARGE = 60;
    /** Its ray, from its face at (x,y,z) to (a,b,c), for this instant. id = the tick it is in. */
    public static final byte DESTROYER_RAY = 61;
    /** It falls at (x,y,z): smoke, sparks, the fire in its face going out. id = it. */
    public static final byte DESTROYER_FALL = 62;
    /** A blow meets an Einherjar's shield at (x,y,z), facing along (a,b,c). */
    public static final byte EINHERJAR_BLOCK = 63;
    /** An Einherjar's spear strikes at (x,y,z), driving along (a,b,c). id = who it struck. */
    public static final byte EINHERJAR_HIT = 64;
    /** The Casket's winter pours out from (x,y,z) along (a,b,c), for time ticks. id = 0 a gale ahead, 1 a burst all round a blocks. */
    public static final byte CASKET_WINTER = 65;
    /** Entity {@link #id} is frozen in ice for time ticks. */
    public static final byte CASKET_FREEZE = 66;
    /**
     * The Infinity Stones in the Destroyer Armor. id = the stones, one bit each; time = {@link com.matoon.eternity.entity.ArmorPowers} FX kind.
     * FX_RAY: their light along its ray from (x,y,z) to (a,b,c).
     */
    public static final byte DESTROYER_STONE = 67;
    /**
     * The Destroyer Armor (entity {@link #id}) hurt: a blow landing at (x,y,z), (a,b,c) the way its shards fly, time = how
     * hard, in hundredths of a twentieth of its integrity.
     */
    public static final byte DESTROYER_WOUND = 68;
    /** The Destroyer Armor imploding, its core breached: a = its size; id = the armour. */
    public static final byte DESTROYER_IMPLODE = 69;

    public byte type;
    public double x, y, z;
    public double a, b, c;
    public int id;
    public int time;

    public MsgTribunalFx() {
    }

    public MsgTribunalFx(byte type, double x, double y, double z, double a, double b, double c, int id, int time) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.a = a;
        this.b = b;
        this.c = c;
        this.id = id;
        this.time = time;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        type = buf.readByte();
        x = buf.readDouble();
        y = buf.readDouble();
        z = buf.readDouble();
        a = buf.readDouble();
        b = buf.readDouble();
        c = buf.readDouble();
        id = buf.readInt();
        time = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(type);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeDouble(a);
        buf.writeDouble(b);
        buf.writeDouble(c);
        buf.writeInt(id);
        buf.writeInt(time);
    }
}
