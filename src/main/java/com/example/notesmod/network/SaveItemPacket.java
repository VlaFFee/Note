package com.example.notesmod.network;

import com.example.notesmod.NoteItem;
import com.example.notesmod.PhotoItem;
import com.example.notesmod.PhotoNames;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Клиент -> сервер: сохранить данные в предмет. Принимается ТОЛЬКО от игрока в креативе. */
public record SaveItemPacket(byte hand, String label, String title, String body, String photo) {

    public void encode(net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeByte(hand);
        buf.writeUtf(label, 64);
        buf.writeUtf(title, 128);
        buf.writeUtf(body, 8000);
        buf.writeUtf(photo, 128);
    }

    public static SaveItemPacket decode(FriendlyByteBuf buf) {
        return new SaveItemPacket(buf.readByte(), buf.readUtf(64), buf.readUtf(128),
                buf.readUtf(8000), buf.readUtf(128));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = c.getSender();
            if (player == null || !player.isCreative()) return; // правка только в креативе

            InteractionHand h = hand == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(h);
            String photoName = PhotoNames.valid(photo) ? photo : "";

            if (stack.getItem() instanceof NoteItem) {
                CompoundTag t = stack.getOrCreateTag();
                t.putString(NoteItem.TAG_LABEL, label);
                t.putString(NoteItem.TAG_TITLE, title);
                t.putString(NoteItem.TAG_BODY, body);
                if (photoName.isEmpty()) t.remove(NoteItem.TAG_PHOTO);
                else t.putString(NoteItem.TAG_PHOTO, photoName);
            } else if (stack.getItem() instanceof PhotoItem) {
                CompoundTag t = stack.getOrCreateTag();
                if (photoName.isEmpty()) t.remove(PhotoItem.TAG_PHOTO);
                else t.putString(PhotoItem.TAG_PHOTO, photoName);
            }
        });
        c.setPacketHandled(true);
    }
}
