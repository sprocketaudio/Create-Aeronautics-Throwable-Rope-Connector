package net.sprocketgames.create_aeronautics_throwable_rope_connector.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.CreateAeronauticsThrowableRopeConnector;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModBlockEntityTypes;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModEntityTypes;

public final class CreateAeronauticsThrowableRopeConnectorClient {
    public static final RopeConnectorLauncherRenderHandler LAUNCHER_RENDER_HANDLER = new RopeConnectorLauncherRenderHandler();
    private static final ResourceLocation MOUNTED_LAUNCHER_HEAD = ResourceLocation.fromNamespaceAndPath(
            CreateAeronauticsThrowableRopeConnector.MOD_ID,
            "block/mounted_rope_launcher_head"
    );
    private static final ResourceLocation MOUNTED_LAUNCHER_COG = ResourceLocation.fromNamespaceAndPath(
            CreateAeronauticsThrowableRopeConnector.MOD_ID,
            "block/cog"
    );

    public static void init(IEventBus modEventBus, ModContainer container) {
        new CreateAeronauticsThrowableRopeConnectorClient(modEventBus, container);
    }

    private CreateAeronauticsThrowableRopeConnectorClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(this::registerEntityRenderers);
        modEventBus.addListener(this::registerAdditionalModels);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(MountedRopeLauncherClientInput::onInteractionKey);
        LAUNCHER_RENDER_HANDLER.registerListeners(NeoForge.EVENT_BUS);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    private void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.THROWABLE_ROPE_CONNECTOR.get(), ThrowableRopeConnectorProjectileRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.MOUNTED_ROPE_LAUNCHER_SEAT.get(), MountedRopeLauncherSeatRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.MOUNTED_ROPE_LAUNCHER.get(), MountedRopeLauncherRenderer::new);
    }

    private void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(MOUNTED_LAUNCHER_HEAD));
        event.register(ModelResourceLocation.standalone(MOUNTED_LAUNCHER_COG));
    }

    private void onClientTick(ClientTickEvent.Post event) {
        LAUNCHER_RENDER_HANDLER.tick();
        MountedRopeLauncherClientInput.tick();
    }
}
