package com.example.notesmod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public final class GuiUtil {

    /**
     * Углы прямоугольника (TL, TR, BR, BL) с поворотом, "перспективой" и наклоном.
     * persp > 0: верх уже, низ шире. shear: сдвиг по X пропорционально Y.
     */
    public static float[] rect(float cx, float cy, float w, float h, float angDeg, float persp, float shear) {
        float[][] p = {{-w / 2, -h / 2}, {w / 2, -h / 2}, {w / 2, h / 2}, {-w / 2, h / 2}};
        float rad = (float) Math.toRadians(angDeg);
        float cos = (float) Math.cos(rad), sin = (float) Math.sin(rad);
        float[] out = new float[8];
        for (int i = 0; i < 4; i++) {
            float x = p[i][0], y = p[i][1];
            x *= 1f + persp * (y / (h / 2f));
            x += y * shear;
            out[i * 2] = cx + x * cos - y * sin;
            out[i * 2 + 1] = cy + x * sin + y * cos;
        }
        return out;
    }

    public static void quadTex(GuiGraphics g, ResourceLocation tex, float[] c, int argb) {
        int a = argb >>> 24 & 255, r = argb >> 16 & 255, gr = argb >> 8 & 255, b = argb & 255;
        Matrix4f m = g.pose().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.enableBlend();
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bb.vertex(m, c[0], c[1], 0).uv(0, 0).color(r, gr, b, a).endVertex(); // TL
        bb.vertex(m, c[6], c[7], 0).uv(0, 1).color(r, gr, b, a).endVertex(); // BL
        bb.vertex(m, c[4], c[5], 0).uv(1, 1).color(r, gr, b, a).endVertex(); // BR
        bb.vertex(m, c[2], c[3], 0).uv(1, 0).color(r, gr, b, a).endVertex(); // TR
        BufferUploader.drawWithShader(bb.end());
    }

    public static void quadColor(GuiGraphics g, float[] c, int argb) {
        int a = argb >>> 24 & 255, r = argb >> 16 & 255, gr = argb >> 8 & 255, b = argb & 255;
        Matrix4f m = g.pose().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bb.vertex(m, c[0], c[1], 0).color(r, gr, b, a).endVertex();
        bb.vertex(m, c[6], c[7], 0).color(r, gr, b, a).endVertex();
        bb.vertex(m, c[4], c[5], 0).color(r, gr, b, a).endVertex();
        bb.vertex(m, c[2], c[3], 0).color(r, gr, b, a).endVertex();
        BufferUploader.drawWithShader(bb.end());
    }

    /** Стабильное "случайное" число в [-1; 1] из имени файла. */
    public static float hashUnit(String s, int salt) {
        int h = (s.hashCode() * 31 + salt * 7919) & 0xFFFF;
        return h / 65535f * 2f - 1f;
    }

    /** Умножает альфу ARGB-цвета на f (0..1). */
    public static int mulAlpha(int argb, float f) {
        int a = (int) ((argb >>> 24) * Math.max(0f, Math.min(1f, f)));
        return a << 24 | (argb & 0xFFFFFF);
    }

    /** RGB + прозрачность -> ARGB. */
    public static int rgba(int rgb, float a) {
        return (int) (Math.max(0f, Math.min(1f, a)) * 255) << 24 | (rgb & 0xFFFFFF);
    }

    private GuiUtil() {}
}
