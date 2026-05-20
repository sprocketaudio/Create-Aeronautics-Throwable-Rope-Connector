package net.sprocketgames.create_aeronautics_throwable_rope_connector.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.server.level.ServerPlayer;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.client.RopeConnectorLauncherClientPacketHandler;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.entity.MountedRopeLauncherSeatEntity;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(
                        RopeConnectorLauncherShootPacket.TYPE,
                        RopeConnectorLauncherShootPacket.STREAM_CODEC,
                        ModNetworking::handleLauncherShoot
                )
                .playToServer(
                        MountedRopeLauncherFirePacket.TYPE,
                        MountedRopeLauncherFirePacket.STREAM_CODEC,
                        ModNetworking::handleMountedLauncherFire
                )
                .playToServer(
                        MountedRopeLauncherDismountPacket.TYPE,
                        MountedRopeLauncherDismountPacket.STREAM_CODEC,
                        ModNetworking::handleMountedLauncherDismount
                )
                .playToServer(
                        MountedRopeLauncherReleasePacket.TYPE,
                        MountedRopeLauncherReleasePacket.STREAM_CODEC,
                        ModNetworking::handleMountedLauncherRelease
                );
    }

    private static void handleLauncherShoot(RopeConnectorLauncherShootPacket packet, IPayloadContext context) {
        RopeConnectorLauncherClientPacketHandler.handle(packet);
    }

    private static void handleMountedLauncherFire(MountedRopeLauncherFirePacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.getVehicle() instanceof MountedRopeLauncherSeatEntity seat
                && seat.getId() == packet.seatEntityId()) {
            seat.fire(player);
        }
    }

    private static void handleMountedLauncherDismount(MountedRopeLauncherDismountPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.getVehicle() instanceof MountedRopeLauncherSeatEntity seat
                && seat.getId() == packet.seatEntityId()) {
            player.stopRiding();
            seat.discard();
        }
    }

    private static void handleMountedLauncherRelease(MountedRopeLauncherReleasePacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.getVehicle() instanceof MountedRopeLauncherSeatEntity seat
                && seat.getId() == packet.seatEntityId()) {
            seat.release(player);
        }
    }
}
