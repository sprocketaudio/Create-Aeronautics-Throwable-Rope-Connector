package net.sprocketgames.create_aeronautics_throwable_rope_connector.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.CreateAeronauticsThrowableRopeConnector;

public record MountedRopeLauncherReleasePacket(int seatEntityId) implements CustomPacketPayload {
    public static final Type<MountedRopeLauncherReleasePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            CreateAeronauticsThrowableRopeConnector.MOD_ID,
            "mounted_rope_launcher_release"
    ));

    public static final StreamCodec<RegistryFriendlyByteBuf, MountedRopeLauncherReleasePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, MountedRopeLauncherReleasePacket::seatEntityId,
            MountedRopeLauncherReleasePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
