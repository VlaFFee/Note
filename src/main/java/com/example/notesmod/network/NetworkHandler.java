package com.example.notesmod.network;

import com.example.notesmod.NotesMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class NetworkHandler {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(NotesMod.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        CHANNEL.registerMessage(0, SaveItemPacket.class,
                SaveItemPacket::encode, SaveItemPacket::decode, SaveItemPacket::handle);
    }

    private NetworkHandler() {}
}
