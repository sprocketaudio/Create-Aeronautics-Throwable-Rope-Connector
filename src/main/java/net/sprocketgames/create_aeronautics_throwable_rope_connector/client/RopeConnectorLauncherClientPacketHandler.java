package net.sprocketgames.create_aeronautics_throwable_rope_connector.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.network.RopeConnectorLauncherShootPacket;

@OnlyIn(Dist.CLIENT)
public final class RopeConnectorLauncherClientPacketHandler {
    private RopeConnectorLauncherClientPacketHandler() {
    }

    public static void handle(RopeConnectorLauncherShootPacket packet) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera == null || camera.position().distanceTo(packet.location()) > 100.0D) {
            return;
        }

        RopeConnectorLauncherRenderHandler handler = CreateAeronauticsThrowableRopeConnectorClient.LAUNCHER_RENDER_HANDLER;
        handler.beforeShoot(packet.pitch());
        if (packet.self()) {
            handler.shoot(packet.hand(), packet.location());
        } else {
            handler.playSound(packet.hand(), packet.location());
        }
    }
}
