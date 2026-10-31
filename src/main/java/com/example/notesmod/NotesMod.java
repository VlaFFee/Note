package com.example.notesmod;

import com.example.notesmod.client.ClientInit;
import com.example.notesmod.network.NetworkHandler;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(NotesMod.MODID)
public class NotesMod {
    public static final String MODID = "notesmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NotesMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(bus);
        ModItems.TABS.register(bus);
        ModSounds.SOUNDS.register(bus);
        bus.addListener(this::commonSetup);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientInit::init);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::register);
    }
}
