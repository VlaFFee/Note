package com.example.notesmod.client;

import com.example.notesmod.NotesMod;
import com.example.notesmod.PhotoNames;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/** Загрузка фото из папки notes_photos + единая "плёночная" стилизация. */
@OnlyIn(Dist.CLIENT)
public final class PhotoCache {
    public record Entry(ResourceLocation tex, int w, int h) {}

    private static final Map<String, Entry> CACHE = new HashMap<>();
    private static final Set<String> FAILED = new HashSet<>();
    private static int counter;

    public static Path dir() {
        Path p = FMLPaths.GAMEDIR.get().resolve("notes_photos");
        try {
            Files.createDirectories(p);
        } catch (IOException ignored) {}
        return p;
    }

    public static boolean isImage(String n) {
        String l = n.toLowerCase(Locale.ROOT);
        return l.endsWith(".png") || l.endsWith(".jpg") || l.endsWith(".jpeg")
                || l.endsWith(".bmp") || l.endsWith(".gif");
    }

    public static List<String> list() {
        try (Stream<Path> s = Files.list(dir())) {
            return s.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(PhotoCache::isImage)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    public static Entry peek(String name, int maxSide) {
        return CACHE.get(name + "@" + maxSide);
    }

    public static boolean failed(String name, int maxSide) {
        return FAILED.contains(name + "@" + maxSide);
    }

    public static void forgetFailures() {
        FAILED.clear();
    }

    public static void clear() {
        for (Entry e : CACHE.values()) {
            Minecraft.getInstance().getTextureManager().release(e.tex());
        }
        CACHE.clear();
        FAILED.clear();
    }

    /** Возвращает стилизованную текстуру или null, если файла нет / он не читается. */
    public static Entry get(String name, int maxSide) {
        if (!PhotoNames.valid(name) || !isImage(name)) return null;
        String key = name + "@" + maxSide;
        Entry e = CACHE.get(key);
        if (e != null) return e;
        if (FAILED.contains(key)) return null;
        try {
            Path root = dir().normalize();
            Path file = root.resolve(name).normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                FAILED.add(key);
                return null;
            }
            NativeImage img = process(file, maxSide, name.hashCode());
            int w = img.getWidth(), h = img.getHeight();
            DynamicTexture tex = new DynamicTexture(img);
            ResourceLocation loc = new ResourceLocation(NotesMod.MODID, "photo/p" + (counter++));
            Minecraft.getInstance().getTextureManager().register(loc, tex);
            tex.setFilter(true, false); // билинейная фильтрация, без мипмапов
            e = new Entry(loc, w, h);
            CACHE.put(key, e);
            return e;
        } catch (Exception ex) {
            NotesMod.LOGGER.warn("Cannot load photo {}: {}", name, ex.toString());
            FAILED.add(key);
            return null;
        }
    }

    /**
     * Стиль: контраст, почти ч/б с лёгким тёплым оттенком, виньетка, плёночное зерно.
     * Параметры можно крутить под свой вкус.
     */
    private static NativeImage process(Path file, int maxSide, long seed) throws IOException {
        BufferedImage src = ImageIO.read(file.toFile());
        if (src == null) throw new IOException("unsupported image");
        int sw = src.getWidth(), sh = src.getHeight();
        double sc = Math.min(1.0, (double) maxSide / Math.max(sw, sh));
        int w = Math.max(1, (int) Math.round(sw * sc));
        int h = Math.max(1, (int) Math.round(sh * sc));

        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = dst.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(src, 0, 0, w, h, null);
        g2.dispose();

        NativeImage out = new NativeImage(NativeImage.Format.RGBA, w, h, false);
        Random rnd = new Random(seed);
        float cx = w / 2f, cy = h / 2f;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = dst.getRGB(x, y);
                float r = argb >> 16 & 255, gr = argb >> 8 & 255, b = argb & 255;
                float lum = 0.299f * r + 0.587f * gr + 0.114f * b;
                lum = (lum - 128f) * 1.18f + 120f;                 // контраст, чуть темнее
                float dx = (x - cx) / cx, dy = (y - cy) / cy;
                float d = Math.min(1f, (dx * dx + dy * dy) / 2f);
                lum *= 1f - 0.70f * d;                             // виньетка
                lum += (rnd.nextFloat() - 0.5f) * 30f;             // зерно
                int rr = clamp(lum * 1.00f + 6f);
                int gg = clamp(lum * 0.96f + 2f);
                int bb = clamp(lum * 0.88f);
                out.setPixelRGBA(x, y, 0xFF000000 | bb << 16 | gg << 8 | rr); // ABGR
            }
        }
        return out;
    }

    private static int clamp(float v) {
        return v < 0 ? 0 : Math.min(255, (int) v);
    }

    private PhotoCache() {}
}
