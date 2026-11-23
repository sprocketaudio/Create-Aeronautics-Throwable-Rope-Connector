package net.sprocketgames.create_aeronautics_throwable_rope_connector.integration;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.CreateAeronauticsThrowableRopeConnector;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.block.MountedRopeLauncherBlock;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.block.MountedRopeLauncherBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.JadeIds;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElementHelper;

@WailaPlugin
public final class MountedLauncherJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(Provider.INSTANCE, MountedRopeLauncherBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Provider.INSTANCE, MountedRopeLauncherBlock.class);
        registration.addTooltipCollectedCallback((box, accessor) -> {
            if (accessor instanceof BlockAccessor block && block.getBlock() instanceof MountedRopeLauncherBlock
                    && !box.getTooltip().get(Provider.INSTANCE.getUid()).isEmpty()) {
                box.getTooltip().remove(JadeIds.UNIVERSAL_ITEM_STORAGE);
            }
        });
    }

    private enum Provider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public ResourceLocation getUid() {
            return ResourceLocation.fromNamespaceAndPath(CreateAeronauticsThrowableRopeConnector.MOD_ID, "mounted_launcher");
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            BlockPos base = MountedRopeLauncherBlock.getBasePos(accessor.getBlockState(), accessor.getPosition());
            if (accessor.getLevel().getBlockEntity(base) instanceof MountedRopeLauncherBlockEntity launcher) {
                data.putBoolean("LauncherConnected", launcher.isConnected());
                ItemStack ammo = launcher.getLoadedAmmo();
                if (!ammo.isEmpty()) {
                    data.put("LauncherAmmo", ammo.save(accessor.getLevel().registryAccess()));
                }
            }
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains("LauncherConnected")) {
                return;
            }
            ItemStack ammo = ItemStack.parseOptional(accessor.getLevel().registryAccess(), data.getCompound("LauncherAmmo"));
            String prefix = "jade.create_aeronautics_throwable_rope_connector.";
            if (ammo.isEmpty()) {
                tooltip.add(Component.translatable(prefix + "empty"));
            } else {
                IElementHelper elements = IElementHelper.get();
                tooltip.add(List.of(
                        elements.text(Component.translatable(prefix + "ammo_label")),
                        elements.smallItem(ammo.copyWithCount(1)),
                        elements.text(Component.translatable(prefix + "ammo", ammo.getCount(), ammo.getHoverName()))
                ));
            }
            tooltip.add(Component.translatable(prefix + (data.getBoolean("LauncherConnected") ? "connected" : "disconnected")));
        }
    }
}
