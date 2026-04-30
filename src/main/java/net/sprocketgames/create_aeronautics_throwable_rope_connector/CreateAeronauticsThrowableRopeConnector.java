package net.sprocketgames.create_aeronautics_throwable_rope_connector;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.config.ModCommonConfig;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModEntityTypes;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModItems;
import org.slf4j.Logger;

@Mod(CreateAeronauticsThrowableRopeConnector.MOD_ID)
public final class CreateAeronauticsThrowableRopeConnector {
    public static final String MOD_ID = "create_aeronautics_throwable_rope_connector";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateAeronauticsThrowableRopeConnector(IEventBus modEventBus, ModContainer modContainer) {
        ModEntityTypes.register(modEventBus);
        ModItems.register(modEventBus);
        modEventBus.addListener(ModItems::addToCreativeTabs);
        modContainer.registerConfig(ModConfig.Type.COMMON, ModCommonConfig.SPEC);
    }
}
