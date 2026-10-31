package com.example.notesmod.client;

import com.example.notesmod.ModSounds;
import com.example.notesmod.PhotoItem;
import com.example.notesmod.network.NetworkHandler;
import com.example.notesmod.network.SaveItemPacket;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/** Просмотр фотографии: "падает" на стол, слегка повёрнута, перекошена, с бликом и заломами. */
@OnlyIn(Dist.CLIENT)
public class PhotoScreen extends Screen {
    private final InteractionHand hand;
    private final boolean creative;
    private final ScreenAnim anim = new ScreenAnim();
    private final List<AbstractWidget> mine = new ArrayList<>();
    private String photo;

    public PhotoScreen(InteractionHand hand, ItemStack stack) {
        super(Component.translatable("gui.notesmod.photo"));
        this.hand = hand;
        CompoundTag t = stack.getTag();
        photo = t != null ? t.getString(PhotoItem.TAG_PHOTO) : "";
        var p = Minecraft.getInstance().player;
        creative = p != null && p.isCreative();
        PhotoCache.forgetFailures();
        ScreenAudio.play(ModSounds.PHOTO_OPEN, 1f, 0.9f);
        ScreenAudio.ensureAmbient();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        mine.clear();
        if (creative) {
            Button b = Button.builder(Component.translatable("gui.notesmod.pick"), x ->
                    minecraft.setScreen(new PhotoPickerScreen(this, name -> {
                        photo = name;
                        NetworkHandler.CHANNEL.sendToServer(new SaveItemPacket(
                                (byte) (hand == InteractionHand.MAIN_HAND ? 0 : 1), "", "", "", name));
                    }))).bounds(width / 2 - 60, height - 30, 120, 20).build();
            mine.add(b);
            addRenderableWidget(b);
        }
    }

    @Override
    public void onClose() {
        if (anim.closing()) return;
        anim.startClose();
        ScreenAudio.play(ModSounds.NOTE_CLOSE, 1.1f, 0.7f);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        if (anim.done()) {
            minecraft.setScreen(null);
            return;
        }
        float v = anim.value(true);                       // с "перелётом"
        float fade = Mth.clamp(anim.value(), 0f, 1f);
        for (AbstractWidget w : mine) w.visible = fade > 0.7f;

        g.fillGradient(0, 0, width, height, GuiUtil.mulAlpha(0xF0050507, fade), GuiUtil.mulAlpha(0xF2000000, fade));

        PhotoCache.Entry e = photo.isEmpty() ? null : PhotoCache.get(photo, 1024);
        if (e == null) {
            if (fade > 0.04f) {
                g.drawCenteredString(font, Component.translatable(photo.isEmpty()
                        ? "gui.notesmod.empty_photo" : "gui.notesmod.missing", photo),
                        width / 2, height / 2, GuiUtil.rgba(0x8E8798, fade));
            }
        } else {
            float sc = 0.78f + 0.22f * v;
            float s = Math.min(width * 0.60f / e.w(), height * 0.74f / e.h()) * sc;
            float w = e.w() * s, h = e.h() * s;
            float cx = width / 2f, cy = height / 2f - 6 + (1f - v) * 55f;

            float t = (Util.getMillis() % 100000L) / 1000f;
            float ang = GuiUtil.hashUnit(photo, 1) * 3f + 4f + (float) Math.sin(t * 0.7) * 0.35f + (1f - v) * 12f;
            float persp = 0.035f + GuiUtil.hashUnit(photo, 2) * 0.015f;
            float shear = GuiUtil.hashUnit(photo, 3) * 0.03f;

            GuiUtil.quadColor(g, GuiUtil.rect(cx + 10, cy + 14, w + 30 * sc, h + 30 * sc, ang, persp, shear),
                    GuiUtil.mulAlpha(0x88000000, fade));
            GuiUtil.quadColor(g, GuiUtil.rect(cx, cy, w + 26 * sc, h + 26 * sc, ang, persp, shear),
                    GuiUtil.mulAlpha(0xFFE6DFCB, fade));
            GuiUtil.quadTex(g, e.tex(), GuiUtil.rect(cx, cy, w, h, ang, persp, shear),
                    GuiUtil.rgba(0xFFFFFF, fade));

            float fw = w + 26 * sc;
            GuiUtil.quadColor(g, GuiUtil.rect(cx, cy + h * 0.12f, fw, 1.6f, ang, persp, shear), GuiUtil.mulAlpha(0x33FFFFFF, fade));
            GuiUtil.quadColor(g, GuiUtil.rect(cx, cy + h * 0.12f + 2f, fw, 1.6f, ang, persp, shear), GuiUtil.mulAlpha(0x44000000, fade));
            GuiUtil.quadColor(g, GuiUtil.rect(cx - w * 0.2f, cy, 1.4f, h + 26 * sc, ang, persp, shear), GuiUtil.mulAlpha(0x22FFFFFF, fade));
            // блик медленно ползёт по фото
            float glare = (float) Math.sin(t * 0.5) * w * 0.08f;
            GuiUtil.quadColor(g, GuiUtil.rect(cx - w * 0.15f + glare, cy - h * 0.1f, w * 0.18f, h * 1.3f, ang + 18, persp, shear),
                    GuiUtil.mulAlpha(0x12FFFFFF, fade));
        }
        super.render(g, mx, my, pt);
        if (fade > 0.04f) {
            g.drawString(font, Component.translatable("gui.notesmod.hint_close"), 10, height - 12, GuiUtil.rgba(0x6F6A72, fade), false);
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (minecraft.options.keyInventory.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }
}
