package com.example.notesmod;

import com.example.notesmod.client.ClientHooks;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PhotoItem extends Item {
    public static final String TAG_PHOTO = "Photo";

    public PhotoItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openPhoto(hand, stack));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        CompoundTag t = stack.getTag();
        if (t != null && !t.getString(TAG_PHOTO).isEmpty()) {
            tip.add(Component.literal(t.getString(TAG_PHOTO)).withStyle(ChatFormatting.DARK_GRAY));
        }
        tip.add(Component.translatable("tooltip.notesmod.open").withStyle(ChatFormatting.GRAY));
    }
}
