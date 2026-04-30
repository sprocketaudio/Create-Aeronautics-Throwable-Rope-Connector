package net.sprocketgames.create_aeronautics_throwable_rope_connector.client;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.CreateAeronauticsThrowableRopeConnector;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModEntityTypes;

@Mod(value = CreateAeronauticsThrowableRopeConnector.MOD_ID, dist = Dist.CLIENT)
public final class CreateAeronauticsThrowableRopeConnectorClient {
    public CreateAeronauticsThrowableRopeConnectorClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(this::registerEntityRenderers);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    private void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.THROWABLE_ROPE_CONNECTOR.get(), ThrownItemRenderer::new);
    }
}
