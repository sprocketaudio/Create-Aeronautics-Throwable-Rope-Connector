package net.sprocketgames.create_aeronautics_throwable_rope_connector;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.block.MountedRopeLauncherBlockEntity;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.client.CreateAeronauticsThrowableRopeConnectorClient;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.config.ModCommonConfig;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.network.ModNetworking;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModBlockEntityTypes;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModBlocks;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModCreativeTabs;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModEntityTypes;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModItems;
import org.slf4j.Logger;

@Mod(CreateAeronauticsThrowableRopeConnector.MOD_ID)
public final class CreateAeronauticsThrowableRopeConnector {
    public static final String MOD_ID = "create_aeronautics_throwable_rope_connector";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateAeronauticsThrowableRopeConnector(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.register(modEventBus);
        ModBlockEntityTypes.register(modEventBus);
        ModEntityTypes.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        modEventBus.addListener(ModNetworking::register);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::onConfigLoaded);
        modEventBus.addListener(this::onConfigReloaded);
        modContainer.registerConfig(ModConfig.Type.COMMON, ModCommonConfig.SPEC);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            CreateAeronauticsThrowableRopeConnectorClient.init(modEventBus, modContainer);
        }
    }

    private void onConfigLoaded(ModConfigEvent.Loading event) {
        this.correctOwnCommonConfig(event.getConfig());
    }

    private void onConfigReloaded(ModConfigEvent.Reloading event) {
        this.correctOwnCommonConfig(event.getConfig());
    }

    private void correctOwnCommonConfig(ModConfig config) {
        if (config.getSpec() != ModCommonConfig.SPEC) {
            return;
        }

        ModCommonConfig.correctRangeValuesToSimulatedMax();
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        MountedRopeLauncherBlockEntity.registerCapabilities(event);
    }
}
