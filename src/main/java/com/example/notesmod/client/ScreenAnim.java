package com.example.notesmod.client;

import net.minecraft.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Плавное появление/исчезновение экрана. value(): 0 = скрыт, 1 = показан. */
@OnlyIn(Dist.CLIENT)
public final class ScreenAnim {
    private static final float OPEN_MS = 460f, CLOSE_MS = 300f;

    private final long start = Util.getMillis();
    private long closeStart = -1;

    public boolean closing() {
        return closeStart >= 0;
    }

    public void startClose() {
        if (closeStart < 0) closeStart = Util.getMillis();
    }

    public boolean done() {
        return closing() && Util.getMillis() - closeStart >= CLOSE_MS;
    }

    public float value() {
        return value(false);
    }

    /** back = true: открытие с небольшим "перелётом" (для падающей фотографии). */
    public float value(boolean back) {
        long now = Util.getMillis();
        if (closing()) {
            float t = clamp((now - closeStart) / CLOSE_MS);
            return 1f - t * t;
        }
        float t = clamp((now - start) / OPEN_MS);
        if (back) {
            float c1 = 1.70158f, c3 = c1 + 1f, u = t - 1f;
            return 1f + c3 * u * u * u + c1 * u * u;
        }
        float u = 1f - t;
        return 1f - u * u * u;
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
