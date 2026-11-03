package com.matoon.eternity.client.render;

import com.matoon.eternity.entity.EntityChampion;
import com.matoon.eternity.entity.EntityChampion.State;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The Gardener's hedge, taking hold of the Champion: black-green stems as thick as an arm, thorned, a leaf here
 * and there, coming off the hedge in front of him and up out of the ground, and wound round his legs, his waist,
 * his chest and both arms. They whip in when he is caught, creak tighter as he heaves, stretch as he gathers
 * himself, and when he throws his arms apart they snap, and the pieces fly.
 * <p>
 * Worked out from his synced state and his pose as last drawn, in his own frame (right, up, forward), so they
 * stay on him whichever way he faces and follow his arms as he strains against them.
 */
final class ChampionVines {

    private static final float[] DARK = {0.12F, 0.17F, 0.09F}, STEM = {0.22F, 0.31F, 0.14F}, RIDGE = {0.33F, 0.45F, 0.2F};
    private static final float[] THORN = {0.82F, 0.79F, 0.64F}, LEAF = {0.2F, 0.36F, 0.13F};
    /** Model units to blocks, at his scale. */
    private static final float UNIT = RenderChampion.SCALE / 16F;
    private static final int RING = 6;

    private ChampionVines() {
    }

    static void render(EntityChampion c, double x, double y, double z, float partial) {
        State state = c.getState();
        if (state != State.SNARED && state != State.STRAIN && state != State.TEAR) {
            return;
        }
        float t = c.clientStateTicks + partial;
        float grow = 1F, tight = 0F, snapped = -1F;
        if (state == State.SNARED) {
            grow = ModelChampion.smooth(t / 22F);
        } else if (state == State.STRAIN) {
            tight = ModelChampion.heave(t);
        } else if (t < EntityChampion.TEAR_RIP) {
            tight = 0.6F + 0.6F * ModelChampion.smooth(t / EntityChampion.TEAR_RIP);
        } else {
            snapped = t - EntityChampion.TEAR_RIP;
            if (snapped > 34F) {
                return;
            }
        }
        float yaw = (float) Math.toRadians(c.prevRotationYaw + MathHelper.wrapDegrees(c.rotationYaw - c.prevRotationYaw) * partial);
        Vec3d fwd = new Vec3d(-MathHelper.sin(yaw), 0D, MathHelper.cos(yaw));
        Vec3d right = new Vec3d(-fwd.z, 0D, fwd.x);
        Frame f = new Frame(new Vec3d(x, y, z), right, fwd);
        float[] pose = c.poseLast;
        float drop = pose[ModelChampion.DROP] * UNIT;
        float light = MathHelper.clamp(c.getBrightness(), 0.25F, 1F);

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Random rand = new Random(c.getEntityId() * 31L);
        for (Vine v : vines(pose, drop, tight)) {
            List<Vec3d> pts = v.points;
            if (snapped >= 0F) {
                shards(f, pts, v.thick, snapped, light, rand);
                continue;
            }
            int shown = Math.max(2, Math.round(pts.size() * grow));
            tube(f, pts.subList(0, shown), v.thick, light, 1F, rand);
        }
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    // ------------------------------------------------------------------ where they go

    private static final class Vine {
        final List<Vec3d> points;
        final double thick;

        Vine(List<Vec3d> points, double thick) {
            this.points = points;
            this.thick = thick;
        }
    }

    /** Every vine on him, in his frame: x right, y up, z forward, from where it comes off the hedge or the ground. */
    private static List<Vine> vines(float[] pose, float drop, float tight) {
        List<Vine> out = new ArrayList<>();
        double squeeze = 1D - 0.12D * tight;
        // Legs, from the foot of the hedge in front of him, round and round, feet to hips.
        out.add(wound(new Vec3d(-0.75D, 0.05D, 1.15D), new Vec3d(-0.27D, 0.12D, 0D), new Vec3d(-0.27D, 1.15D - drop, 0D), 0.27D * squeeze, 2.3D, 0.4D, 0.085D));
        out.add(wound(new Vec3d(0.8D, 0.05D, 1.1D), new Vec3d(0.27D, 0.1D, 0D), new Vec3d(0.27D, 1.1D - drop, 0D), 0.27D * squeeze, 2.3D, 2.2D, 0.085D));
        // Waist, from the hedge at his belt; and a second up out of the ground beside him.
        out.add(wound(new Vec3d(0.1D, 1.3D, 1.2D), new Vec3d(0D, 1.2D - drop, 0D), new Vec3d(0D, 1.75D - drop, 0D), 0.6D * squeeze, 1.6D, 1.1D, 0.1D));
        out.add(wound(new Vec3d(0.9D, 0D, -0.5D), new Vec3d(0D, 1.05D - drop, 0D), new Vec3d(0D, 1.55D - drop, 0D), 0.62D * squeeze, 1.2D, 3.6D, 0.08D));
        // Chest, from the ground behind him, across and round under his arms.
        out.add(wound(new Vec3d(-0.9D, 0D, -0.7D), new Vec3d(0D, 1.95D - drop, 0D), new Vec3d(0D, 2.35D - drop, 0D), 0.66D * squeeze, 1.25D, 5.1D, 0.09D));
        // Arms, from high on the hedge, wound from the shoulder to the wrist, wherever he has forced them.
        for (int side = -1; side <= 1; side += 2) {
            float a = side > 0 ? -pose[ModelChampion.LA_Z] : pose[ModelChampion.RA_Z];
            double reach = 1.15D;
            Vec3d shoulder = new Vec3d(side * 8.2D * UNIT, 2.36D - drop, 0D);
            Vec3d along = new Vec3d(side * MathHelper.sin(a), -MathHelper.cos(a), 0D);
            Vec3d from = shoulder.add(along.scale(0.12D * reach)), to = shoulder.add(along.scale(0.95D * reach));
            out.add(wound(new Vec3d(side * 1.35D, 2.1D, 1.05D), from, to, 0.24D * squeeze, 2.7D, side > 0 ? 0.6D : 2.9D, 0.075D));
        }
        return out;
    }

    /** From a root, bowed out to the near end of an axis, then wound round it to the far end, so many turns. */
    private static Vine wound(Vec3d root, Vec3d a0, Vec3d a1, double radius, double turns, double phase, double thick) {
        List<Vec3d> pts = new ArrayList<>();
        Vec3d axis = a1.subtract(a0);
        Vec3d dir = axis.normalize();
        Vec3d helper = Math.abs(dir.y) > 0.9D ? new Vec3d(0D, 0D, 1D) : new Vec3d(0D, 1D, 0D);
        Vec3d e1 = dir.crossProduct(helper).normalize(), e2 = dir.crossProduct(e1).normalize();
        Vec3d start = a0.add(e1.scale(Math.cos(phase) * radius)).add(e2.scale(Math.sin(phase) * radius));
        // The reach across: a bow that sags toward the ground before it climbs on to him.
        Vec3d mid = root.add(start).scale(0.5D).add(0D, -0.25D, 0D);
        for (int i = 0; i < 10; i++) {
            double u = i / 10D;
            pts.add(root.scale((1 - u) * (1 - u)).add(mid.scale(2 * u * (1 - u))).add(start.scale(u * u)));
        }
        int n = (int) Math.ceil(turns * 14D);
        for (int i = 0; i <= n; i++) {
            double u = (double) i / n;
            double ang = phase + u * turns * Math.PI * 2D;
            pts.add(a0.add(axis.scale(u)).add(e1.scale(Math.cos(ang) * radius)).add(e2.scale(Math.sin(ang) * radius)));
        }
        return new Vine(pts, thick);
    }

    // ------------------------------------------------------------------ drawing

    /** His frame, laid into the world relative to the camera. */
    private static final class Frame {
        final Vec3d base, right, fwd;

        Frame(Vec3d base, Vec3d right, Vec3d fwd) {
            this.base = base;
            this.right = right;
            this.fwd = fwd;
        }

        Vec3d world(Vec3d p) {
            return base.add(right.scale(p.x)).add(0D, p.y, 0D).add(fwd.scale(p.z));
        }
    }

    /** A stem along points: a six-sided tube, thicker at the root and tapering to a tip, with thorns and the odd leaf. */
    private static void tube(Frame f, List<Vec3d> local, double thick, float light, float alpha, Random rand) {
        int n = local.size();
        if (n < 2) {
            return;
        }
        Vec3d[] p = new Vec3d[n];
        for (int i = 0; i < n; i++) {
            p[i] = f.world(local.get(i));
        }
        Vec3d[][] ring = new Vec3d[n][];
        Vec3d[][] normal = new Vec3d[n][];
        Vec3d last = null;
        for (int i = 0; i < n; i++) {
            Vec3d tan = p[Math.min(n - 1, i + 1)].subtract(p[Math.max(0, i - 1)]).normalize();
            Vec3d side = last != null ? last : tan.crossProduct(Math.abs(tan.y) > 0.9D ? new Vec3d(1D, 0D, 0D) : new Vec3d(0D, 1D, 0D));
            // Carried along the stem so the ring never twists.
            Vec3d nrm = side.subtract(tan.scale(side.dotProduct(tan))).normalize();
            Vec3d bin = tan.crossProduct(nrm).normalize();
            last = nrm;
            double taper = Math.min(1D, (n - 1 - i) / 5D) * (1D + 0.3D * Math.max(0D, 1D - i / 6D));
            double r = thick * Math.max(0.15D, taper) * (0.92D + 0.08D * Math.sin(i * 1.7D));
            ring[i] = new Vec3d[RING];
            normal[i] = new Vec3d[RING];
            for (int k = 0; k < RING; k++) {
                double a = Math.PI * 2D * k / RING;
                Vec3d d = nrm.scale(Math.cos(a)).add(bin.scale(Math.sin(a)));
                normal[i][k] = d;
                ring[i][k] = p[i].add(d.scale(r));
            }
        }
        Tessellator tes = Tessellator.getInstance();
        BufferBuilder buf = tes.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < n - 1; i++) {
            for (int k = 0; k < RING; k++) {
                int k2 = (k + 1) % RING;
                vertex(buf, ring[i][k], normal[i][k], k, light, alpha);
                vertex(buf, ring[i + 1][k], normal[i + 1][k], k, light, alpha);
                vertex(buf, ring[i + 1][k2], normal[i + 1][k2], k2, light, alpha);
                vertex(buf, ring[i][k2], normal[i][k2], k2, light, alpha);
            }
        }
        tes.draw();
        // Thorns, pale and hooked, every few joints; and now and then a leaf.
        buf.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 2; i < n - 2; i += 3) {
            int k = (i * 5 + 1) % RING;
            Vec3d out = normal[i][k];
            Vec3d tip = ring[i][k].add(out.scale(0.11D)).add(p[i + 1].subtract(p[i]).normalize().scale(-0.04D));
            Vec3d a = ring[i][k], b = ring[i][(k + 1) % RING], cc = ring[i + 1][k];
            float s = shade(out, light);
            for (Vec3d[] tri : new Vec3d[][]{{a, b, tip}, {b, cc, tip}, {cc, a, tip}}) {
                for (Vec3d v : tri) {
                    buf.pos(v.x, v.y, v.z).color(THORN[0] * s, THORN[1] * s, THORN[2] * s, alpha).endVertex();
                }
            }
            if (i % 9 == 5) {
                Vec3d along = p[i + 1].subtract(p[i]).normalize();
                Vec3d stalk = ring[i][(k + 3) % RING];
                Vec3d spread = normal[i][(k + 3) % RING];
                Vec3d leafTip = stalk.add(spread.scale(0.26D)).add(along.scale(0.1D));
                Vec3d w = along.crossProduct(spread).normalize().scale(0.08D);
                Vec3d midL = stalk.add(leafTip).scale(0.5D).add(w), midR = stalk.add(leafTip).scale(0.5D).subtract(w);
                float ls = shade(spread, light);
                for (Vec3d[] tri : new Vec3d[][]{{stalk, midL, leafTip}, {stalk, leafTip, midR}}) {
                    for (Vec3d v : tri) {
                        buf.pos(v.x, v.y, v.z).color(LEAF[0] * ls, LEAF[1] * ls, LEAF[2] * ls, alpha).endVertex();
                    }
                }
            }
        }
        tes.draw();
    }

    private static void vertex(BufferBuilder buf, Vec3d at, Vec3d normal, int k, float light, float alpha) {
        // One pale ridge running down each stem, the rest dark, darker underneath.
        float[] col = k == 0 ? RIDGE : k == 3 ? DARK : STEM;
        float s = shade(normal, light);
        buf.pos(at.x, at.y, at.z).color(col[0] * s, col[1] * s, col[2] * s, alpha).endVertex();
    }

    private static float shade(Vec3d normal, float light) {
        return light * (0.55F + 0.45F * (float) Math.max(0D, normal.y * 0.7D + 0.3D));
    }

    /**
     * Snapped: what was wound round him breaks into lengths that fly off him and fall, shrinking; what reached
     * across from the hedge and the ground drops slack where it was.
     */
    private static void shards(Frame f, List<Vec3d> pts, double thick, float s, float light, Random rand) {
        float shrink = 1F - ModelChampion.smooth((s - 12F) / 22F);
        if (shrink <= 0.02F) {
            return;
        }
        // The reach across, slack and falling to the ground.
        List<Vec3d> slack = new ArrayList<>();
        for (int i = 0; i < Math.min(10, pts.size()); i++) {
            Vec3d q = pts.get(i);
            double fall = Math.min(q.y, s * 0.12D * (i / 10D));
            slack.add(q.add(0D, -fall, 0D));
        }
        tube(f, slack, thick * shrink, light, 1F, rand);
        // The coils, in lengths of five joints, flung off him.
        for (int start = 10; start < pts.size() - 1; start += 5) {
            List<Vec3d> piece = pts.subList(start, Math.min(pts.size(), start + 6));
            Vec3d centre = piece.get(piece.size() / 2);
            Vec3d out = new Vec3d(centre.x, 0D, centre.z);
            out = out.lengthSquared() < 1.0E-4 ? new Vec3d(0D, 0D, 1D) : out.normalize();
            double speed = 0.16D + rand.nextDouble() * 0.14D;
            double lift = 0.05D + rand.nextDouble() * 0.12D;
            Vec3d move = out.scale(speed * s).add(0D, lift * s - 0.012D * s * s, 0D);
            List<Vec3d> flying = new ArrayList<>();
            for (Vec3d q : piece) {
                Vec3d moved = q.add(move);
                flying.add(new Vec3d(moved.x, Math.max(0.05D, moved.y), moved.z));
            }
            tube(f, flying, thick * shrink, light, 1F, rand);
        }
    }
}
