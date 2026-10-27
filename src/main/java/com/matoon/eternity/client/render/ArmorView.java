package com.matoon.eternity.client.render;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.client.ArmorClient;
import com.matoon.eternity.entity.ArmorPowers;
import com.matoon.eternity.entity.ArmorUpgrade;
import com.matoon.eternity.entity.EntityDestroyerArmor;
import com.matoon.eternity.entity.EntityDestroyerArmor.State;
import com.matoon.eternity.entity.Overcharge;
import com.matoon.eternity.item.ItemAstralRegulator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import static com.matoon.eternity.client.render.VaultParts.clamp;
import static com.matoon.eternity.client.render.VaultParts.smooth;
import static com.matoon.eternity.client.render.VaultParts.strike;

/**
 * The Destroyer Armor from inside: its forearms and fists out in front of you where your own hands
 * would be, and the inside of its face around the edges of your sight, slats of dark metal with
 * the furnace's light warming them. Along the bottom, how much more it can take and how hot the
 * furnace runs. Whoever is inside sees nothing else of it.
 */
@SideOnly(Side.CLIENT)
public final class ArmorView {

    private static final Arms ARMS = new Arms();

    private ArmorView() {
    }

    // ------------------------------------------------------------------ its arms, where yours would be

    /** Its forearms and fists, as the pilot sees them: out in front, moving as the armour does. */
    public static void arms(EntityDestroyerArmor armor, float pt) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(RenderDestroyer.skinTexture(armor.skin()));
        float t = armor.clientStateTicks + pt;
        State state = armor.getState();
        float swing = armor.prevLimbSwingAmount + (armor.limbSwingAmount - armor.prevLimbSwingAmount) * pt;
        float phase = (armor.limbSwing - armor.limbSwingAmount * (1F - pt)) * 0.42F;
        // Into view as its back shuts on you; out of it as it opens.
        float present = state == State.ENTER ? smooth((t - EntityDestroyerArmor.ENTER_SHUT) / 12F)
                : state == State.EXIT ? 1F - smooth(t / 10F) : 1F;
        // Flying flat out its arms sweep back past the pilot's sight.
        present *= 1F - 0.9F * armor.flight(pt) * VaultParts.clamp((armor.lean(pt) - 0.1F) / 0.8F);
        if (present <= 0.01F) {
            return;
        }
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableCull();
        for (int side = -1; side <= 1; side += 2) {
            boolean right = side > 0;
            float x = 0.58F * side, y = -0.62F, z = -0.95F, pitch = -78F, yaw = 13F * side, roll = 0F;
            // Its stride, rolling through its shoulders.
            float step = MathHelper.cos(phase) * swing;
            y += Math.abs(MathHelper.sin(phase)) * 0.05F * swing;
            z += step * 0.08F * side;
            switch (state) {
                case PUNCH_R:
                case PUNCH_L: {
                    boolean mine = (state == State.PUNCH_R) == right;
                    int hit = EntityDestroyerArmor.PUNCH_HIT;
                    float wind = smooth(t / (hit - 3F)) * (1F - smooth((t - hit + 3F) / 2F));
                    float blow = strike(t, hit - 3F, hit, EntityDestroyerArmor.PUNCH_TICKS - 3F);
                    if (mine) {
                        z += 0.22F * wind - 0.75F * blow;
                        y += -0.08F * wind + 0.16F * blow;
                        x -= 0.34F * blow * side;
                        yaw -= 10F * blow * side;
                        pitch += 4F * blow;
                    } else {
                        float guard = Math.max(wind, blow);
                        z += 0.18F * guard;
                        y -= 0.06F * guard;
                    }
                    break;
                }
                case SLAM: {
                    int hit = EntityDestroyerArmor.SLAM_HIT;
                    float raise = smooth(t / (hit - 6F)) * (1F - smooth((t - hit + 4F) / 3F));
                    float down = VaultParts.outCubic(clamp((t - hit + 3F) / 3F)) * (1F - smooth((t - hit - 8F) / 10F));
                    y += 0.95F * raise - 0.22F * down;
                    z -= 0.1F * raise + 0.35F * down;
                    x -= 0.3F * side * (raise + down);
                    pitch += 55F * raise - 30F * down;
                    yaw -= 10F * side * raise;
                    break;
                }
                case RAY: {
                    float brace = smooth(t / 8F);
                    x += 0.22F * side * brace;
                    y -= 0.2F * brace;
                    z += 0.12F * brace;
                    yaw += 12F * side * brace;
                    roll = -8F * side * brace + MathHelper.sin(t * 2.9F) * 0.6F * brace;
                    break;
                }
                case VENT:
                    y -= 0.3F * smooth(t / 8F) * (1F - smooth((t - EntityDestroyerArmor.VENT_TICKS + 10F) / 10F));
                    break;
                default:
                    break;
            }
            y -= (1F - present) * 1.2F;
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, z);
            GlStateManager.rotate(roll, 0F, 0F, 1F);
            GlStateManager.rotate(yaw, 0F, 1F, 0F);
            GlStateManager.rotate(pitch, 1F, 0F, 0F);
            // A quarter turn back about x lays the forearm (down +y from the elbow in model space) out ahead, its back
            // and its spikes (+z) up where the pilot sees them. Unflipped, the model's left arm has its outer spikes
            // toward +x, so it is the one on the right of the view, and its right arm on the left.
            ModelRenderer fore = right ? ARMS.foreL : ARMS.foreR;
            fore.render(0.0625F * 0.8F);
            // Its wounds on its fists too, and the fire in the worst of them.
            int stage = RenderDestroyer.scarred(armor);
            if (stage > 0) {
                GlSnapshot before = GlSnapshot.take();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                GlStateManager.depthFunc(GL11.GL_LEQUAL);
                GlStateManager.depthMask(false);
                Minecraft.getMinecraft().getTextureManager().bindTexture(RenderDestroyer.scarTexture(stage, false));
                fore.render(0.0625F * 0.8F);
                if (stage > 1) {
                    GlStateManager.disableLighting();
                    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
                    GlStateManager.color(1F, 1F, 1F, 0.65F + 0.35F * MathHelper.sin(armor.ticksExisted * 0.3F));
                    Minecraft.getMinecraft().getTextureManager().bindTexture(RenderDestroyer.scarTexture(stage, true));
                    fore.render(0.0625F * 0.8F);
                    GlStateManager.color(1F, 1F, 1F, 1F);
                }
                GlStateManager.depthMask(true);
                before.restore();
                Minecraft.getMinecraft().getTextureManager().bindTexture(RenderDestroyer.skinTexture(armor.skin()));
            }
            GlStateManager.popMatrix();
        }
        GlStateManager.disableRescaleNormal();
    }

    /** Just its forearms and fists, off the Destroyer's own sheet. */
    private static final class Arms extends ModelBase {
        final ModelRenderer foreR, foreL;

        Arms() {
            VaultBoxes.Sheet sheet = VaultBoxes.DESTROYER;
            textureWidth = sheet.width;
            textureHeight = sheet.height;
            foreR = VaultParts.part(this, sheet, "foreR", null, 0F, 0F, 0F);
            VaultParts.part(this, sheet, "fistR", foreR, 0F, 8F, 0F);
            foreL = VaultParts.part(this, sheet, "foreL", null, 0F, 0F, 0F);
            VaultParts.part(this, sheet, "fistL", foreL, 0F, 8F, 0F);
        }
    }

    // ------------------------------------------------------------------ the inside of its face

    /**
     * The edges of the pilot's sight: the slats of its face plates to either side, its brow above,
     * the furnace's light warming everything, brighter as it runs hotter and blinding when it fires.
     * As the armour wakes around you, the dark lifts in flickers.
     */
    public static void visor(EntityDestroyerArmor armor, double w, double h, float pt, boolean firstPerson) {
        float t = armor.clientStateTicks + pt;
        float heat = armor.heat(pt);
        float open = armor.faceOpen(pt);
        State state = armor.getState();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        if (firstPerson) {
            // The slats of its face plates, either side, fading in toward the middle of your sight.
            double band = w * 0.13D, slat = h / 17D;
            for (int side = 0; side < 2; side++) {
                double outer = side == 0 ? 0D : w, inner = side == 0 ? band : w - band;
                for (int i = 0; i < 17; i++) {
                    double y0 = i * slat, y1 = y0 + slat;
                    boolean gap = i % 2 == 1;
                    float edge = gap ? 0.94F : 0.88F;
                    float c = gap ? 0.005F : 0.045F + 0.02F * heat;
                    float warm = gap ? 0.16F * heat : 0.02F * heat;
                    gradientX(buffer, outer, inner, y0, y1, c + warm, c + warm * 0.4F, c, edge, 0F);
                }
            }
            // Its brow above, and the lower edge of its face below.
            gradientY(buffer, 0D, w, 0D, h * 0.11D, 0.03F, 0.03F, 0.035F, 0.92F, 0F);
            gradientY(buffer, 0D, w, h, h * 0.9D, 0.05F + 0.1F * heat, 0.03F + 0.03F * heat, 0.02F, 0.85F, 0F);
            // The furnace, warming the inside of its face; blinding while the ray pours out.
            float glow = 0.05F * heat + 0.22F * open;
            rect(buffer, 0D, 0D, w, h, 1F, 0.45F, 0.1F, Math.min(0.35F, glow));
            // Waking: the dark lifting from your eyes in flickers.
            float dark = 0F;
            if (state == State.ENTER) {
                float shut = t - EntityDestroyerArmor.ENTER_SHUT;
                dark = shut < 0F ? 0F : shut < 6F ? 1F : 1F - smooth((shut - 6F) / 8F);
                if (shut > 6F && shut < 12F && ((int) t) % 3 == 0) {
                    dark = 0.85F;
                }
            }
            if (dark > 0F) {
                rect(buffer, 0D, 0D, w, h, 0F, 0F, 0F, dark);
            }
        }
        tessellator.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        if (armor.isManned()) {
            gauges(armor, w, h, pt);
        }
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
    }

    /** How much more it can take, and how hot the furnace runs; and the furnace venting when it must. */
    private static void gauges(EntityDestroyerArmor armor, double w, double h, float pt) {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer font = mc.fontRenderer;
        float health = armor.getHealth() / armor.getMaxHealth();
        float furnace = armor.getFurnace() / EntityDestroyerArmor.HEAT_MAX;
        boolean venting = armor.getState() == State.VENT;
        float t = armor.ticksExisted + pt;
        int segments = 20;
        double width = Math.min(130D, w * 0.22D), seg = width / segments, y = h - 34D;
        double left = 12D, right = w - 12D - width;
        GlStateManager.disableTexture2D();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < segments; i++) {
            float at = (i + 0.5F) / segments;
            boolean lit = at <= health;
            float pulse = health < 0.25F && lit ? 0.6F + 0.4F * MathHelper.sin(t * 0.5F) : 1F;
            rect(buffer, left + i * seg, y, left + (i + 1) * seg - 1D, y + 6D, lit ? 1F * pulse : 0.15F, lit ? 0.7F * pulse : 0.1F,
                    lit ? 0.25F * pulse : 0.06F, lit ? 0.9F : 0.6F);
            boolean hot = at <= furnace;
            float r = hot ? 1F : 0.15F, g = hot ? 0.75F - 0.6F * at : 0.1F, b = hot ? 0.15F * (1F - at) : 0.06F;
            float flash = venting ? 0.7F : 1F;
            rect(buffer, right + i * seg, y, right + (i + 1) * seg - 1D, y + 6D, r * flash, g * flash, b * flash, hot ? 0.9F : 0.6F);
        }
        tessellator.draw();
        // What feeds it, under its integrity: molten lava, or the red of Forge Energy in its dynamo; dark where it has burned away.
        boolean electric = armor.electric();
        float lava = armor.getLava() / armor.lavaCapacity();
        double ly = y + 10D;
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        rect(buffer, left, ly, left + width - 1D, ly + 3D, 0.12F, 0.08F, 0.05F, 0.6F);
        float ripple = 0.85F + 0.15F * MathHelper.sin(t * 0.3F);
        float fr = electric ? 0.9F : 1F, fg = electric ? 0.12F : 0.42F, fb = electric ? 0.16F : 0.05F;
        rect(buffer, left, ly, left + (width - 1D) * lava, ly + 3D, fr * ripple, fg * ripple, fb * ripple, lava < 0.15F ? 0.6F + 0.4F * MathHelper.sin(t * 0.6F) : 0.9F);
        // The stones in its chest, under the furnace, each burning in its own colour.
        int stones = armor.stones();
        if (stones != 0) {
            double pip = (width - 5D) / ArmorPowers.COUNT;
            for (int i = 0; i < ArmorPowers.COUNT; i++) {
                int rgb = ArmorPowers.RGB[i];
                boolean held = (stones & (1 << i)) != 0;
                float r = (rgb >> 16 & 255) / 255F, g = (rgb >> 8 & 255) / 255F, b = (rgb & 255) / 255F, k = held ? 0.8F + 0.2F * MathHelper.sin(t * 0.15F + i) : 0.18F;
                rect(buffer, right + i * (pip + 1D), ly, right + i * (pip + 1D) + pip, ly + 3D, r * k, g * k, b * k, held ? 0.95F : 0.5F);
            }
        }
        tessellator.draw();
        GlStateManager.enableTexture2D();
        font.drawStringWithShadow("INTEGRITY", (float) left, (float) y - 10F, 0xD8B070);
        String fuel = electric ? "ENERGY " + ItemAstralRegulator.formatEnergy(armor.energy()) + " FE" : String.format("LAVA %.1f", armor.getLava() / EntityDestroyerArmor.BUCKET);
        font.drawStringWithShadow(fuel, (float) left, (float) ly + 6F, armor.cold() ? 0xFF3B2B : electric ? 0xE05060 : 0xC08048);
        String heat = venting ? "VENTING" : "FURNACE";
        font.drawStringWithShadow(heat, (float) (right + width - font.getStringWidth(heat)), (float) y - 10F, venting ? 0xFF5030 : 0xD8B070);
        if (stones != 0) {
            String count = Integer.bitCount(stones) == ArmorPowers.COUNT ? "INFINITY" : Integer.bitCount(stones) + " STONE" + (Integer.bitCount(stones) == 1 ? "" : "S");
            font.drawStringWithShadow(count, (float) (right + width - font.getStringWidth(count)), (float) ly + 6F, 0xC890FF);
        }
        if (armor.canOvercharge()) {
            overcharge(armor, font, w, y, t);
        }
        if (armor.isFlying()) {
            String fly = armor.soaring() ? "FLYING" : "ASCENDED";
            font.drawStringWithShadow(fly, (float) (w - font.getStringWidth(fly)) / 2F, (float) y + 4F, armor.soaring() ? 0xFFE03A : 0xC890FF);
        }
        // What has failed it, in the middle of the pilot's sight, flashing.
        String warning = armor.breach() > 0 ? null
                : armor.scorching() ? "CORE OVERHEATING  -  IT IS TEARING ITSELF APART"
                : armor.core() >= 90F ? "CORE CRITICAL  -  TURN THE OVERCHARGE DOWN"
                : armor.surging() && armor.surgeDraw() > armor.supply() && armor.energy() < armor.surgeDraw() ? "DYNAMO STARVED  -  OVERCHARGE DRAWS MORE THAN IT IS FED"
                : armor.cold() ? (electric ? "DYNAMO DRAINED  -  NEEDS FORGE ENERGY" : "FURNACE COLD  -  NEEDS LAVA")
                : armor.condition() == EntityDestroyerArmor.Condition.CRITICAL ? "INTEGRITY CRITICAL  -  NEEDS URU PLATE"
                : lava < 0.1F ? (electric ? "ENERGY LOW" : "LAVA LOW") : null;
        if (armor.breach() > 0) {
            breach(armor, font, w, h, pt);
        } else if (warning != null) {
            // Held steady, never blinking.
            font.drawStringWithShadow(warning, (float) (w - font.getStringWidth(warning)) / 2F, (float) (h * 0.28D), 0xFF3B2B);
        }
    }

    /**
     * Its overcharge, over the middle of the gauges: each part and its level, the one its keys turn picked out; and
     * under them what the overcharge draws against what it is fed, and its core's heat. Off, how to turn it on.
     */
    private static void overcharge(EntityDestroyerArmor armor, FontRenderer font, double w, double y, float t) {
        Overcharge[] parts = Overcharge.values();
        int picked = Math.floorMod(ArmorClient.surgePart, parts.length);
        String[] labels = new String[parts.length];
        int width = 0, gap = 8;
        for (int i = 0; i < parts.length; i++) {
            labels[i] = (i == picked ? "> " : "") + parts[i].title.toUpperCase() + " " + armor.surge(parts[i]);
            width += font.getStringWidth(labels[i]) + (i > 0 ? gap : 0);
        }
        float x = (float) (w - width) / 2F, row = (float) y - 26F;
        for (int i = 0; i < parts.length; i++) {
            int level = armor.surge(parts[i]);
            int color = i == picked ? (level > 0 ? parts[i].color : 0xD8B070) : level > 0 ? dim(parts[i].color) : 0x6A645A;
            font.drawStringWithShadow(labels[i], x, row, color);
            if (i == picked) {
                Gui.drawRect((int) x, (int) row + 9, (int) x + font.getStringWidth(labels[i]), (int) row + 10, 0xC0000000 | color);
            }
            x += font.getStringWidth(labels[i]) + gap;
        }
        int total = armor.surgeTotal();
        String line;
        int color;
        if (total == 0 && armor.core() < 1F) {
            line = String.format("OVERCHARGE OFF  -  %s/%s pick a part, %s turns it up", ArmorClient.OVERCHARGE_PREV.getDisplayName(),
                    ArmorClient.OVERCHARGE_NEXT.getDisplayName(), ArmorClient.OVERCHARGE_UP.getDisplayName());
            color = 0x80786C;
        } else {
            int draw = armor.surgeDraw(), fed = armor.supply();
            line = String.format("DRAW %s / FED %s FE/t  -  CORE %.0f%%", ItemAstralRegulator.formatEnergy(draw), ItemAstralRegulator.formatEnergy(fed), armor.core());
            color = armor.breach() > 0 ? 0xFF3B2B : armor.core() >= 65F || draw > fed ? 0xFF7040 : 0xC8BCA8;
        }
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.75F, 0.75F, 1F);
        font.drawStringWithShadow(line, (float) (w / 0.75D - font.getStringWidth(line)) / 2F, (float) ((y - 14D) / 0.75D), color);
        GlStateManager.popMatrix();
        if (total > 0 || armor.core() >= 1F) {
            // Its core's heat, a thin bar under them.
            double bw = 90D, bx = (w - bw) / 2D, by = y - 6D;
            float core = armor.core() / Overcharge.CORE_MAX;
            GlStateManager.disableTexture2D();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            rect(buffer, bx, by, bx + bw, by + 2D, 0.12F, 0.06F, 0.04F, 0.7F);
            float r = 0.6F + 0.4F * core, g = 0.1F + 0.85F * core * core, b = 0.05F + 0.8F * Math.max(0F, core - 0.6F);
            rect(buffer, bx, by, bx + bw * core, by + 2D, r, g, b, 0.95F);
            tessellator.draw();
            GlStateManager.enableTexture2D();
        }
    }

    /**
     * Its core breaching, told plainly and without a single flash: a panel high in the pilot's sight with the
     * seconds left before it implodes, large, and a bar draining to nothing; the key that stops it; and the edges of
     * their sight closing in with a still, dark red that deepens as the end nears. The alarm is heard, not flashed.
     */
    private static void breach(EntityDestroyerArmor armor, FontRenderer font, double w, double h, float pt) {
        float near = MathHelper.clamp((armor.breach() + pt) / (float) Overcharge.BREACH_TICKS, 0F, 1F);
        float left = Math.max(0F, (Overcharge.BREACH_TICKS - armor.breach() - pt) / 20F);
        Tessellator tessellator = Tessellator.getInstance();
        // Gui.drawRect, above, leaves blending off.
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.disableTexture2D();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        // The edges of their sight, darkening red, closing in steadily as it nears the end.
        double band = Math.min(w, h) * (0.12D + 0.18D * near);
        float edge = 0.25F + 0.4F * near;
        gradientX(buffer, 0D, band, 0D, h, 0.35F, 0.02F, 0.01F, edge, 0F);
        gradientX(buffer, w, w - band, 0D, h, 0.35F, 0.02F, 0.01F, edge, 0F);
        gradientY(buffer, 0D, w, 0D, band, 0.35F, 0.02F, 0.01F, edge, 0F);
        gradientY(buffer, 0D, w, h, h - band, 0.35F, 0.02F, 0.01F, edge, 0F);
        // The panel.
        double pw = 230D, ph = 62D, px = (w - pw) / 2D, py = h * 0.16D;
        rect(buffer, px, py, px + pw, py + ph, 0.04F, 0.02F, 0.02F, 0.82F);
        rect(buffer, px, py, px + pw, py + 1D, 0.9F, 0.2F, 0.1F, 1F);
        rect(buffer, px, py + ph - 1D, px + pw, py + ph, 0.9F, 0.2F, 0.1F, 1F);
        rect(buffer, px, py, px + 1D, py + ph, 0.9F, 0.2F, 0.1F, 1F);
        rect(buffer, px + pw - 1D, py, px + pw, py + ph, 0.9F, 0.2F, 0.1F, 1F);
        // How long it has, draining: amber to red as it goes.
        double bx = px + 10D, by = py + ph - 12D, bw = pw - 20D;
        rect(buffer, bx, by, bx + bw, by + 5D, 0.15F, 0.05F, 0.04F, 1F);
        rect(buffer, bx, by, bx + bw * (1F - near), by + 5D, 1F, 0.75F - 0.6F * near, 0.2F - 0.15F * near, 1F);
        tessellator.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableTexture2D();
        String head = "CORE BREACH  -  IMPLODING";
        font.drawStringWithShadow(head, (float) (px + 10D), (float) (py + 8D), 0xFF5A40);
        String key = "Press " + ArmorClient.OVERCHARGE_CUT.getDisplayName().toUpperCase() + " to cut the overcharge";
        font.drawStringWithShadow(key, (float) (px + 10D), (float) (py + 22D), 0xF0E0D0);
        // The seconds left, large, against the right of the panel.
        String secs = String.format("%.1f", left);
        int color = left > 5F ? 0xFFC060 : left > 2.5F ? 0xFF8040 : 0xFF3B2B;
        GlStateManager.pushMatrix();
        GlStateManager.translate(px + pw - 10D, py + 7D, 0D);
        GlStateManager.scale(2.5F, 2.5F, 1F);
        font.drawStringWithShadow(secs, -font.getStringWidth(secs), 0F, color);
        GlStateManager.popMatrix();
        String unit = "SECONDS";
        GlStateManager.pushMatrix();
        GlStateManager.translate(px + pw - 10D, py + 32D, 0D);
        GlStateManager.scale(0.75F, 0.75F, 1F);
        font.drawStringWithShadow(unit, -font.getStringWidth(unit), 0F, 0xC8BCA8);
        GlStateManager.popMatrix();
    }

    private static int dim(int rgb) {
        return ((rgb >> 16 & 255) * 3 / 5) << 16 | ((rgb >> 8 & 255) * 3 / 5) << 8 | (rgb & 255) * 3 / 5;
    }

    /** How near they are to stepping out, held: a bar under the crosshair. */
    public static void leaving(double w, double h, float progress) {
        Minecraft mc = Minecraft.getMinecraft();
        double width = 60D, x0 = (w - width) / 2D, y0 = h / 2D + 14D;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        rect(buffer, x0, y0, x0 + width, y0 + 3D, 0.1F, 0.08F, 0.06F, 0.7F);
        rect(buffer, x0, y0, x0 + width * progress, y0 + 3D, 1F, 0.7F, 0.25F, 0.95F);
        tessellator.draw();
        GlStateManager.enableTexture2D();
        String text = "Stepping out";
        mc.fontRenderer.drawStringWithShadow(text, (float) (w - mc.fontRenderer.getStringWidth(text)) / 2F, (float) y0 + 6F, 0xD8B070);
    }

    // ------------------------------------------------------------------ helpers

    private static void rect(BufferBuilder buffer, double x0, double y0, double x1, double y1, float r, float g, float b, float a) {
        buffer.pos(x0, y1, 0D).color(r, g, b, a).endVertex();
        buffer.pos(x1, y1, 0D).color(r, g, b, a).endVertex();
        buffer.pos(x1, y0, 0D).color(r, g, b, a).endVertex();
        buffer.pos(x0, y0, 0D).color(r, g, b, a).endVertex();
    }

    /** A band across, fading from alpha a0 at x0 to a1 at x1. */
    private static void gradientX(BufferBuilder buffer, double x0, double x1, double y0, double y1, float r, float g, float b, float a0, float a1) {
        double lo = Math.min(x0, x1), hi = Math.max(x0, x1);
        float alo = x0 < x1 ? a0 : a1, ahi = x0 < x1 ? a1 : a0;
        buffer.pos(lo, y1, 0D).color(r, g, b, alo).endVertex();
        buffer.pos(hi, y1, 0D).color(r, g, b, ahi).endVertex();
        buffer.pos(hi, y0, 0D).color(r, g, b, ahi).endVertex();
        buffer.pos(lo, y0, 0D).color(r, g, b, alo).endVertex();
    }

    /** A band down, fading from alpha a0 at y0 to a1 at y1. */
    private static void gradientY(BufferBuilder buffer, double x0, double x1, double y0, double y1, float r, float g, float b, float a0, float a1) {
        double lo = Math.min(y0, y1), hi = Math.max(y0, y1);
        float alo = y0 < y1 ? a0 : a1, ahi = y0 < y1 ? a1 : a0;
        buffer.pos(x0, hi, 0D).color(r, g, b, ahi).endVertex();
        buffer.pos(x1, hi, 0D).color(r, g, b, ahi).endVertex();
        buffer.pos(x1, lo, 0D).color(r, g, b, alo).endVertex();
        buffer.pos(x0, lo, 0D).color(r, g, b, alo).endVertex();
    }
}
