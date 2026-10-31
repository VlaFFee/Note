package com.example.notesmod;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, NotesMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NotesMod.MODID);

    // Нет рецептов, нет лута -> предметы получить можно только из креатива
    public static final RegistryObject<Item> NOTE =
            ITEMS.register("note", () -> new NoteItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> PHOTO =
            ITEMS.register("photo", () -> new PhotoItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.notesmod"))
                    .icon(() -> new ItemStack(NOTE.get()))
                    .displayItems((params, out) -> {
                        out.accept(NOTE.get());
                        out.accept(PHOTO.get());
                    })
                    .build());

    private ModItems() {}
}
