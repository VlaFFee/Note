package com.example.notesmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientHooks {
    public static void openNote(InteractionHand hand, ItemStack stack) {
        Minecraft.getInstance().setScreen(new NoteScreen(hand, stack));
    }

    public static void openPhoto(InteractionHand hand, ItemStack stack) {
        Minecraft.getInstance().setScreen(new PhotoScreen(hand, stack));
    }

    private ClientHooks() {}
}
