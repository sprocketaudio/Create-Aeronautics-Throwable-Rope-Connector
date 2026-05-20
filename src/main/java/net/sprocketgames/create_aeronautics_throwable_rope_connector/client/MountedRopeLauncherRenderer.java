package net.sprocketgames.create_aeronautics_throwable_rope_connector.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.CreateAeronauticsThrowableRopeConnector;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.block.MountedRopeLauncherBlockEntity;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.entity.MountedRopeLauncherSeatEntity;

public final class MountedRopeLauncherRenderer extends RopeWinchRenderer {
    private static final float PITCH_VISUAL_MULTIPLIER = 1.0F;
    private static final float MIN_MOUNTED_PITCH = -30.0F;
    private static final float MAX_MOUNTED_PITCH = 30.0F;
    private static final double HEAD_PIVOT_Y = 1.0D;
    private static final double HEAD_REST_Y_OFFSET = 1.0D - HEAD_PIVOT_Y;
    private static final PartialModel HEAD = PartialModel.of(ResourceLocation.fromNamespaceAndPath(
            CreateAeronauticsThrowableRopeConnector.MOD_ID,
            "block/mounted_rope_launcher_head"
    ));
    private static final PartialModel COG = PartialModel.of(ResourceLocation.fromNamespaceAndPath(
            CreateAeronauticsThrowableRopeConnector.MOD_ID,
            "block/cog"
    ));

    public MountedRopeLauncherRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(
            RopeWinchBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int light,
            int overlay
    ) {
        super.renderSafe(blockEntity, partialTicks, poseStack, buffer, light, overlay);
        if (blockEntity instanceof MountedRopeLauncherBlockEntity mounted) {
            this.renderLauncherHead(mounted, partialTicks, poseStack, buffer, light);
        }
    }

    private void renderLauncherHead(
            MountedRopeLauncherBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int light
    ) {
        BlockState state = blockEntity.getBlockState();
        float aimYaw = blockEntity.getAimYaw();
        float aimPitch = blockEntity.getAimPitch();

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer localPlayer = minecraft.player;
        if (localPlayer != null
                && localPlayer.getVehicle() instanceof MountedRopeLauncherSeatEntity seat
                && blockEntity.getBlockPos().equals(seat.getLauncherPos())) {
            aimYaw = MountedRopeLauncherBlockEntity.clampYawForFacing(
                    state.getValue(net.sprocketgames.create_aeronautics_throwable_rope_connector.block.MountedRopeLauncherBlock.FACING),
                    localPlayer.getViewYRot(partialTicks)
            );
            aimPitch = MountedRopeLauncherBlockEntity.clampMountedPitch(localPlayer.getViewXRot(partialTicks));
        }

        float yaw = Mth.wrapDegrees(180.0F - aimYaw);
        float pitch = -MountedRopeLauncherBlockEntity.clampMountedPitch(aimPitch * PITCH_VISUAL_MULTIPLIER);

        poseStack.pushPose();
        poseStack.translate(0.5D, HEAD_PIVOT_Y, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.translate(-0.5D, HEAD_REST_Y_OFFSET, -0.5D);

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.solid());
        SuperByteBuffer head = CachedBuffers.partial(HEAD, state);
        head.light(light).renderInto(poseStack, vertexConsumer);
        this.renderCog(state, poseStack, vertexConsumer, light);
        poseStack.popPose();
    }

    private void renderCog(BlockState state, PoseStack poseStack, VertexConsumer vertexConsumer, int light) {
        float angle = AnimationTickHolder.getRenderTime() * -2.5F % 360.0F;
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, -6.0F / 16.0F);
        poseStack.translate(8.0F / 16.0F, 8.5F / 16.0F, 8.0F / 16.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
        poseStack.translate(-8.0F / 16.0F, -8.5F / 16.0F, -8.0F / 16.0F);
        SuperByteBuffer cog = CachedBuffers.partial(COG, state);
        cog.light(light).renderInto(poseStack, vertexConsumer);
        poseStack.popPose();
    }
}
