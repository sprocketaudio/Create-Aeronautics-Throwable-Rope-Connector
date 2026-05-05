package net.sprocketgames.create_aeronautics_throwable_rope_connector.network;

import net.createmod.catnip.codecs.stream.CatnipStreamCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.CreateAeronauticsThrowableRopeConnector;

public record RopeConnectorLauncherShootPacket(Vec3 location, InteractionHand hand, float pitch, boolean self) implements CustomPacketPayload {
    public static final Type<RopeConnectorLauncherShootPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            CreateAeronauticsThrowableRopeConnector.MOD_ID,
            "rope_connector_launcher_shoot"
    ));

    public static final StreamCodec<RegistryFriendlyByteBuf, RopeConnectorLauncherShootPacket> STREAM_CODEC = StreamCodec.composite(
            CatnipStreamCodecs.VEC3, RopeConnectorLauncherShootPacket::location,
            CatnipStreamCodecs.HAND, RopeConnectorLauncherShootPacket::hand,
            ByteBufCodecs.FLOAT, RopeConnectorLauncherShootPacket::pitch,
            ByteBufCodecs.BOOL, RopeConnectorLauncherShootPacket::self,
            RopeConnectorLauncherShootPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
