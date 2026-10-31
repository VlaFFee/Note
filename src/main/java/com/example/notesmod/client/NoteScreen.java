package com.example.notesmod.client;

import com.example.notesmod.ModSounds;
import com.example.notesmod.NoteItem;
import com.example.notesmod.network.NetworkHandler;
import com.example.notesmod.network.SaveItemPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class NoteScreen extends Screen {
    private static final int BG = 0x09070B;
    private static final int PURPLE = 0xB69CFF;
    private static final float BODY_SCALE = 1.2f;
    private static final float TITLE_SCALE = 2.6f;

    private final InteractionHand hand;
    private final boolean creative;
    private final ScreenAnim anim = new ScreenAnim();
    private final List<AbstractWidget> mine = new ArrayList<>();

    private String label, title, body, photo;           // текущие значения
    private String sLabel, sTitle, sBody, sPhoto;       // последние сохранённые
    private boolean editing;

    private float scroll, scrollTarget, maxScroll;
    private long lastFrame = Util.getMillis();
    private int bodyTop, bodyH;
    private EditBox labelBox, titleBox;
    private MultiLineEditBox bodyBox;
    private int rx, rw;

    public NoteScreen(InteractionHand hand, ItemStack stack) {
        super(Component.translatable("gui.notesmod.note"));
        this.hand = hand;
        CompoundTag t = stack.getTag();
        sLabel = t != null && t.contains(NoteItem.TAG_LABEL) ? t.getString(NoteItem.TAG_LABEL) : "NOTE #1";
        sTitle = t != null ? t.getString(NoteItem.TAG_TITLE) : "";
        sBody = t != null ? t.getString(NoteItem.TAG_BODY) : "";
        sPhoto = t != null ? t.getString(NoteItem.TAG_PHOTO) : "";
        var p = Minecraft.getInstance().player;
        creative = p != null && p.isCreative();
        revert();
        editing = creative && sTitle.isEmpty() && sBody.isEmpty();
        PhotoCache.forgetFailures();
        ScreenAudio.play(ModSounds.NOTE_OPEN, 1f, 0.9f);
        ScreenAudio.ensureAmbient();
    }

    private void revert() {
        label = sLabel; title = sTitle; body = sBody; photo = sPhoto;
    }

    private void sync() {
        if (editing && labelBox != null) {
            label = labelBox.getValue();
            title = titleBox.getValue();
            body = bodyBox.getValue();
        }
    }

    private <T extends AbstractWidget> T add(T w) {
        mine.add(w);
        return addRenderableWidget(w);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        mine.clear();
        rx = (int) (width * 0.54f);
        rw = (int) (width * 0.40f);

        if (editing) {
            labelBox = new EditBox(font, rx, 26, Math.min(170, rw), 18, Component.empty());
            labelBox.setMaxLength(32);
            labelBox.setValue(label);
            add(labelBox);

            titleBox = new EditBox(font, rx, 64, rw, 20, Component.empty());
            titleBox.setMaxLength(100);
            titleBox.setValue(title);
            add(titleBox);

            int top = 104;
            bodyBox = new MultiLineEditBox(font, rx, top, rw, Math.max(40, height - top - 48),
                    Component.translatable("gui.notesmod.body_hint"), Component.empty());
            bodyBox.setValue(body);
            add(bodyBox);

            int by = height - 34, gap = 4, bw = (rw - gap * 3) / 4;
            add(Button.builder(Component.translatable("gui.notesmod.pick"), b -> {
                sync();
                minecraft.setScreen(new PhotoPickerScreen(this, name -> photo = name));
            }).bounds(rx, by, bw, 20).build());
            add(Button.builder(Component.translatable("gui.notesmod.remove"), b -> photo = "")
                    .bounds(rx + (bw + gap), by, bw, 20).build());
            add(Button.builder(Component.translatable("gui.notesmod.save"), b -> save())
                    .bounds(rx + (bw + gap) * 2, by, bw, 20).build());
            add(Button.builder(Component.translatable("gui.notesmod.cancel"), b -> {
                revert();
                editing = false;
                scroll = scrollTarget = 0;
                rebuildWidgets();
            }).bounds(rx + (bw + gap) * 3, by, bw, 20).build());
        } else if (creative) {
            add(Button.builder(Component.translatable("gui.notesmod.edit"), b -> {
                editing = true;
                rebuildWidgets();
            }).bounds(rx, height - 34, 130, 20).build());
        }
    }

    private void save() {
        sync();
        label = cut(label, 64);
        title = cut(title, 128);
        body = cut(body, 6000);
        NetworkHandler.CHANNEL.sendToServer(new SaveItemPacket(
                (byte) (hand == InteractionHand.MAIN_HAND ? 0 : 1), label, title, body, photo == null ? "" : photo));
        sLabel = label; sTitle = title; sBody = body; sPhoto = photo;
        ScreenAudio.play(ModSounds.SCRIBBLE, 1f, 0.8f);
        editing = false;
        scroll = scrollTarget = 0;
        rebuildWidgets();
    }

    private static String cut(String s, int n) {
        return s.length() > n ? s.substring(0, n) : s;
    }

    @Override
    public void onClose() {
        if (anim.closing()) return;
        anim.startClose();
        ScreenAudio.play(ModSounds.NOTE_CLOSE, 1f, 0.8f);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        if (anim.done()) {
            minecraft.setScreen(null);
            return;
        }
        float a = anim.value();
        float ta = Mth.clamp(a, 0f, 1f);

        long now = Util.getMillis();
        float dt = Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        scroll += (scrollTarget - scroll) * Math.min(1f, dt * 14f); // плавная прокрутка

        for (AbstractWidget w : mine) w.visible = ta > 0.7f;

        g.fill(0, 0, width, height, GuiUtil.rgba(BG, ta));
        drawPaper(g, ta);

        if (ta > 0.04f) {
            if (editing) {
                g.drawString(font, Component.translatable("gui.notesmod.label"), rx, 14, GuiUtil.rgba(0x8E8798, ta), false);
                g.drawString(font, Component.translatable("gui.notesmod.title"), rx, 52, GuiUtil.rgba(0x8E8798, ta), false);
                g.drawString(font, Component.translatable("gui.notesmod.body"), rx, 92, GuiUtil.rgba(0x8E8798, ta), false);
            } else {
                drawText(g, ta);
            }
        }
        super.render(g, mx, my, pt);

        if (ta > 0.04f) {
            Component hint = Component.translatable(editing ? "gui.notesmod.hint_edit" : "gui.notesmod.hint_view");
            g.drawString(font, hint, width - font.width(hint) - 10, height - 12, GuiUtil.rgba(0x6F6A72, ta), false);
        }
    }

    // ---------- левая часть: бумага + фото + текст ----------
    private void drawPaper(GuiGraphics g, float ta) {
        int pw = (int) (width * 0.42f), ph = (int) (height * 0.80f);
        int px = (int) (width * 0.06f), py = (int) (height * 0.10f);
        float inv = 1f - ta;

        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(px + pw / 2f - inv * pw * 0.9f, py + ph / 2f + inv * 24f, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(-1.2f - inv * 6f));
        ps.translate(-pw / 2f, -ph / 2f, 0);

        g.fill(6, 8, pw + 6, ph + 8, 0x66000000);                 // тень
        g.fill(0, 0, pw, ph, 0xFFD9CDB2);                         // лист
        g.fillGradient(0, 0, pw, ph, 0x22FFFFFF, 0x33000000);     // потёртость
        for (int y = 26; y < ph - 10; y += 11) g.fill(6, y, pw - 12, y + 1, 0x553A3226); // линейки
        g.fill(22, 0, 23, ph, 0x44A04040);                        // поле

        Random r = new Random(7);                                 // рваный низ
        for (int x = 0; x < pw; x += 3) {
            int cut = 1 + r.nextInt(7);
            g.fill(x, ph - cut, x + 3, ph, 0xFF000000 | BG);
        }
        for (int y = 10; y < ph - 8; y += 14) g.fill(pw - 8, y, pw - 2, y + 6, 0xFF000000 | BG); // перфорация

        int startLine = drawPhotoOnPaper(g, pw, ph);
        drawPaperText(g, pw, ph, startLine);
        ps.popPose();
    }

    /** Рисует фото сверху листа и возвращает Y первой линейки под ним. */
    private int drawPhotoOnPaper(GuiGraphics g, int pw, int ph) {
        if (photo == null || photo.isEmpty()) return 37;
        PhotoCache.Entry e = PhotoCache.get(photo, 768);
        if (e == null) {
            g.drawCenteredString(font, Component.translatable("gui.notesmod.missing", photo), pw / 2, 40, 0xFF7A2A2A);
            return snap(70);
        }
        float s = Math.min((pw - 70f) / e.w(), (ph * 0.46f) / e.h());
        float w = e.w() * s, h = e.h() * s;
        float cx = pw / 2f - 4, cy = 30 + h / 2f;
        float ang = GuiUtil.hashUnit(photo, 1) * 3.5f;

        GuiUtil.quadColor(g, GuiUtil.rect(cx + 3, cy + 4, w + 16, h + 16, ang, 0, 0), 0x66000000);
        GuiUtil.quadColor(g, GuiUtil.rect(cx, cy, w + 14, h + 14, ang, 0, 0), 0xFFE9E2D0);
        GuiUtil.quadTex(g, e.tex(), GuiUtil.rect(cx, cy, w, h, ang, 0, 0), 0xFFFFFFFF);
        GuiUtil.quadColor(g, GuiUtil.rect(cx, cy - h / 2 - 8, 50, 14, ang + 4, 0, 0), 0xAAE8D88A); // скотч
        return snap(cy + h / 2f + 22);
    }

    /** Ближайшая линейка листа на уровне y или ниже. */
    private static int snap(float y) {
        return 26 + (int) Math.ceil((y - 26) / 11f) * 11;
    }

    /** "Рукописный" текст: мелкий, бледный, с лёгкой дрожью. Не влезает — обрезается на "...". */
    private void drawPaperText(GuiGraphics g, int pw, int ph, int startLine) {
        if (body == null || body.isBlank()) return;
        final float S = 0.8f;
        int x0 = 28;
        int wrapW = (int) ((pw - x0 - 18) / S);
        int maxLines = (ph - 14 - startLine) / 11 + 1;
        if (maxLines <= 0) return;

        List<FormattedText> ls = font.getSplitter().splitLines(body, wrapW, Style.EMPTY);
        if (ls.isEmpty()) return;
        boolean over = ls.size() > maxLines;
        int n = Math.min(ls.size(), maxLines);

        PoseStack ps = g.pose();
        for (int i = 0; i < n; i++) {
            String s = ls.get(i).getString();
            if (over && i == n - 1) {
                s = font.plainSubstrByWidth(s, wrapW - font.width("...")).stripTrailing() + "...";
            }
            if (s.isEmpty()) continue;
            float jx = GuiUtil.hashUnit(body, i) * 0.8f;
            float jy = GuiUtil.hashUnit(body, i + 100) * 0.5f;
            ps.pushPose();
            ps.translate(x0 + jx, startLine + i * 11 - 8 + jy, 0);
            ps.scale(S, S, 1f);
            g.drawString(font, s, 1, 0, 0x402A2830, false);   // размытая "тень чернил"
            g.drawString(font, s, 0, 0, 0xB82A2830, false);   // сам текст
            ps.popPose();
        }
    }

    // ---------- правая часть: чёткий текст ----------
    private void drawText(GuiGraphics g, float ta) {
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate((1f - ta) * 40f, 0, 0);

        int ly = (int) (height * 0.16f);
        int lx = rx;
        for (char c : label.toUpperCase().toCharArray()) {
            String ch = String.valueOf(c);
            g.drawString(font, ch, lx, ly, GuiUtil.rgba(PURPLE, ta), false);
            lx += font.width(ch) + 2;
        }

        int titleY = ly + 16;
        List<FormattedCharSequence> tl = font.split(Component.literal(title), (int) (rw / TITLE_SCALE));
        ps.pushPose();
        ps.translate(rx, titleY, 0);
        ps.scale(TITLE_SCALE, TITLE_SCALE, 1f);
        for (int i = 0; i < tl.size(); i++) g.drawString(font, tl.get(i), 0, i * 11, GuiUtil.rgba(0xF4EEE0, ta), false);
        ps.popPose();

        int lineY = titleY + (int) (Math.max(1, tl.size()) * 11 * TITLE_SCALE) + 4;
        g.fill(rx, lineY, rx + 34, lineY + 1, GuiUtil.rgba(0x6F6A72, ta));

        bodyTop = lineY + 14;
        bodyH = Math.max(30, height - bodyTop - 46);
        List<FormattedCharSequence> lines = font.split(Component.literal(body), (int) (rw / BODY_SCALE));
        float lineH = 12f * BODY_SCALE;
        maxScroll = Math.max(0, lines.size() * lineH - bodyH);
        scrollTarget = Mth.clamp(scrollTarget, 0, maxScroll);
        scroll = Mth.clamp(scroll, 0, maxScroll);

        g.enableScissor(rx - 4, bodyTop, rx + rw + 4 + 60, bodyTop + bodyH);
        ps.pushPose();
        ps.translate(rx, bodyTop - scroll, 0);
        ps.scale(BODY_SCALE, BODY_SCALE, 1f);
        for (int i = 0; i < lines.size(); i++) g.drawString(font, lines.get(i), 0, i * 12, GuiUtil.rgba(0xE8E2D6, ta), false);
        ps.popPose();
        g.disableScissor();

        int bgA = GuiUtil.rgba(BG, ta), bgT = BG; // прозрачный конец градиента
        if (scroll > 1) g.fillGradient(rx - 4, bodyTop, rx + rw + 4, bodyTop + 16, bgA, bgT);
        if (scroll < maxScroll - 1) g.fillGradient(rx - 4, bodyTop + bodyH - 16, rx + rw + 4, bodyTop + bodyH, bgT, bgA);
        ps.popPose();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!editing) {
            scrollTarget = Mth.clamp(scrollTarget - (float) delta * 28f, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (!editing && minecraft.options.keyInventory.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }
}
