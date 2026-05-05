package net.sprocketgames.create_aeronautics_throwable_rope_connector.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.client.RopeConnectorLauncherClientPacketHandler;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(
                        RopeConnectorLauncherShootPacket.TYPE,
                        RopeConnectorLauncherShootPacket.STREAM_CODEC,
                        ModNetworking::handleLauncherShoot
                );
    }

    private static void handleLauncherShoot(RopeConnectorLauncherShootPacket packet, IPayloadContext context) {
        RopeConnectorLauncherClientPacketHandler.handle(packet);
    }
}
