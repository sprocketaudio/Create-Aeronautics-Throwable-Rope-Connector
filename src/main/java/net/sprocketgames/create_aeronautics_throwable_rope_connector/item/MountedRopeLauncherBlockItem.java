package net.sprocketgames.create_aeronautics_throwable_rope_connector.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.config.ModCommonConfig;

import java.util.List;

public final class MountedRopeLauncherBlockItem extends BlockItem {
    public MountedRopeLauncherBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.create_aeronautics_throwable_rope_connector.mounted_rope_launcher.load").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.create_aeronautics_throwable_rope_connector.mounted_rope_launcher.mount").withStyle(ChatFormatting.GRAY));
        if (ModCommonConfig.canMountedLauncherRemoteRelease()) {
            tooltipComponents.add(Component.translatable("tooltip.create_aeronautics_throwable_rope_connector.mounted_rope_launcher.release").withStyle(ChatFormatting.GRAY));
        }
        tooltipComponents.add(Component.translatable("tooltip.create_aeronautics_throwable_rope_connector.mounted_rope_launcher.unload").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.create_aeronautics_throwable_rope_connector.mounted_rope_launcher.mounted_fire").withStyle(ChatFormatting.DARK_GRAY));
        if (ModCommonConfig.canMountedLauncherRemoteRelease()) {
            tooltipComponents.add(Component.translatable("tooltip.create_aeronautics_throwable_rope_connector.mounted_rope_launcher.mounted_release").withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltipComponents.add(Component.translatable("tooltip.create_aeronautics_throwable_rope_connector.mounted_rope_launcher.mounted_dismount").withStyle(ChatFormatting.DARK_GRAY));
        if (ModCommonConfig.canMountedLauncherFireFromRedstone()) {
            tooltipComponents.add(Component.translatable("tooltip.create_aeronautics_throwable_rope_connector.mounted_rope_launcher.redstone_fire_side").withStyle(ChatFormatting.DARK_GRAY));
            if (ModCommonConfig.canMountedLauncherRemoteRelease()) {
                tooltipComponents.add(Component.translatable("tooltip.create_aeronautics_throwable_rope_connector.mounted_rope_launcher.redstone_release_side").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
