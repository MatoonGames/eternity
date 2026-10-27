package com.matoon.eternity.client.render;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Builds model parts out of the boxes {@link VaultBoxes} lays out on each texture. */
@SideOnly(Side.CLIENT)
final class VaultParts {

    private VaultParts() {
    }

    /** One part, every box of that name on the sheet, pivoting at (x, y, z) in its parent. */
    static ModelRenderer part(ModelBase model, VaultBoxes.Sheet sheet, String name, ModelRenderer parent, float x, float y, float z) {
        return part(model, sheet, name, parent, x, y, z, 0F);
    }

    /** The same, every box swollen by grow more than the sheet says: worn over a body, it must stand clear of the skin. */
    static ModelRenderer part(ModelBase model, VaultBoxes.Sheet sheet, String name, ModelRenderer parent, float x, float y, float z, float grow) {
        ModelRenderer part = new ModelRenderer(model, name);
        part.setTextureSize(sheet.width, sheet.height);
        for (VaultBoxes.Box b : sheet.of(name)) {
            part.cubeList.add(new ModelBox(part, b.u, b.v, b.x, b.y, b.z, b.w, b.h, b.d, b.delta + grow, false));
        }
        part.setRotationPoint(x, y, z);
        if (parent != null) {
            parent.addChild(part);
        }
        return part;
    }

    static float clamp(float x) {
        return x < 0F ? 0F : x > 1F ? 1F : x;
    }

    static float smooth(float x) {
        x = clamp(x);
        return x * x * (3F - 2F * x);
    }

    static float outCubic(float x) {
        x = 1F - clamp(x);
        return 1F - x * x * x;
    }

    static float outBack(float x) {
        x = clamp(x) - 1F;
        return 1F + 2.70158F * x * x * x + 1.70158F * x * x;
    }

    /** A blow: snapping out between start and contact, held a moment, drawn back by end. */
    static float strike(float t, float start, float contact, float end) {
        if (t < start || t > end + 2F) {
            return 0F;
        }
        if (t < contact) {
            return outCubic((t - start) / (contact - start));
        }
        return t < contact + 1F ? 1F : 1F - smooth((t - contact - 1F) / (end - contact + 1F));
    }

    /** Eases one value of a pose toward a target, by f. */
    static void set(float[] p, int i, float value, float f) {
        p[i] += (value - p[i]) * f;
    }
}
