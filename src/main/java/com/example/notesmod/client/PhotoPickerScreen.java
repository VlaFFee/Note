package com.example.notesmod.client;

import com.example.notesmod.ModSounds;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.Consumer;

/** Сетка миниатюр из папки notes_photos (уже в том же стиле, что и в заметке). */
@OnlyIn(Dist.CLIENT)
public class PhotoPickerScreen extends Screen {
    private static final int CELL_W = 112, CELL_H = 118, GAP = 8, THUMB = 192;
    private static final int BG = 0xFF09070B;

    private final Screen parent;
    private final Consumer<String> onPick;
    private List<String> files = List.of();
    private float scroll, maxScroll;
    private int areaX, areaY, areaW, areaH, cols, gridX;

    public PhotoPickerScreen(Screen parent, Consumer<String> onPick) {
        super(Component.translatable("gui.notesmod.picker_title"));
        this.parent = parent;
        this.onPick = onPick;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        files = PhotoCache.list();
        areaX = 24;
        areaY = 36;
        areaW = width - 48;
        areaH = height - areaY - 44;
        cols = Math.max(1, (areaW + GAP) / (CELL_W + GAP));
        gridX = areaX + (areaW - (cols * (CELL_W + GAP) - GAP)) / 2;

        int by = height - 30, bw = 120;
        addRenderableWidget(Button.builder(Component.translatable("gui.notesmod.open_folder"),
                b -> Util.getPlatform().openFile(PhotoCache.dir().toFile()))
                .bounds(width / 2 - bw / 2 - 8 - bw, by, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.notesmod.refresh"), b -> {
            PhotoCache.clear();
            files = PhotoCache.list();
            scroll = 0;
        }).bounds(width / 2 - bw / 2, by, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.notesmod.back"), b -> onClose())
                .bounds(width / 2 + bw / 2 + 8, by, bw, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, BG);
        g.drawCenteredString(font, title, width / 2, 14, 0xFFB69CFF);

        int rows = (files.size() + cols - 1) / cols;
        maxScroll = Math.max(0, rows * (CELL_H + GAP) - GAP - areaH);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        if (files.isEmpty()) {
            List<FormattedCharSequence> l = font.split(Component.translatable("gui.notesmod.picker_empty"), areaW - 40);
            for (int i = 0; i < l.size(); i++)
                g.drawCenteredString(font, l.get(i), width / 2, height / 2 - 10 + i * 12, 0xFF8E8798);
        }

        g.enableScissor(areaX, areaY, areaX + areaW, areaY + areaH);
        int budget = 1; // грузим по одной миниатюре за кадр, чтобы не подвисало
        for (int i = 0; i < files.size(); i++) {
            String name = files.get(i);
            int x = gridX + (i % cols) * (CELL_W + GAP);
            int y = areaY + (i / cols) * (CELL_H + GAP) - (int) scroll;
            if (y + CELL_H < areaY || y > areaY + areaH) continue;

            boolean hover = mx >= x && mx < x + CELL_W && my >= Math.max(y, areaY)
                    && my < Math.min(y + CELL_H, areaY + areaH);
            g.fill(x, y, x + CELL_W, y + CELL_H, hover ? 0xFF2A2433 : 0xFF171320);
            if (hover) g.renderOutline(x, y, CELL_W, CELL_H, 0xFFB69CFF);

            PhotoCache.Entry e = PhotoCache.peek(name, THUMB);
            if (e == null && budget > 0 && !PhotoCache.failed(name, THUMB)) {
                e = PhotoCache.get(name, THUMB);
                budget--;
            }
            if (e != null) {
                float s = Math.min((CELL_W - 12f) / e.w(), (CELL_H - 30f) / e.h());
                GuiUtil.quadTex(g, e.tex(),
                        GuiUtil.rect(x + CELL_W / 2f, y + 6 + (CELL_H - 30) / 2f, e.w() * s, e.h() * s, 0, 0, 0), 0xFFFFFFFF);
            } else if (PhotoCache.failed(name, THUMB)) {
                g.drawCenteredString(font, "?", x + CELL_W / 2, y + 40, 0xFF7A2A2A);
            }
            g.drawString(font, font.plainSubstrByWidth(name, CELL_W - 8), x + 4, y + CELL_H - 16, 0xFFCFC8D8, false);
        }
        g.disableScissor();
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0 || mx < areaX || mx >= areaX + areaW || my < areaY || my >= areaY + areaH) return false;
        double rx = mx - gridX, ry = my - areaY + scroll;
        if (rx < 0 || ry < 0) return false;
        int col = (int) (rx / (CELL_W + GAP)), row = (int) (ry / (CELL_H + GAP));
        if (rx % (CELL_W + GAP) >= CELL_W || ry % (CELL_H + GAP) >= CELL_H || col >= cols) return false;
        int idx = row * cols + col;
        if (idx >= 0 && idx < files.size()) {
            ScreenAudio.play(ModSounds.PICK, 1f, 0.8f);
            onPick.accept(files.get(idx));
            minecraft.setScreen(parent);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll = (float) Math.max(0, Math.min(maxScroll, scroll - delta * 30));
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
