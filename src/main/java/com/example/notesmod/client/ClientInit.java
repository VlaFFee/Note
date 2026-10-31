package com.example.notesmod.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@OnlyIn(Dist.CLIENT)
public final class ClientInit {
    public static void init() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientInit::clientSetup);
    }

    private static void clientSetup(FMLClientSetupEvent e) {
        // создаём папку .minecraft/notes_photos при первом запуске
        e.enqueueWork(PhotoCache::dir);
    }

    private ClientInit() {}
}
