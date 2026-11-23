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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.CreateAeronauticsThrowableRopeConnector;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.block.MountedRopeLauncherBlockEntity;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.entity.MountedRopeLauncherSeatEntity;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

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
    static final ResourceLocation BASE_MODEL = ResourceLocation.fromNamespaceAndPath(
            CreateAeronauticsThrowableRopeConnector.MOD_ID, "block/mounted_rope_launcher_base_horizontal");
    private static final PartialModel BASE = PartialModel.of(BASE_MODEL);

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
            Vec3 worldLookDirection = localPlayer.getViewVector(partialTicks);
            Vec3 localLookDirection = blockEntity.worldDirectionToLocal(worldLookDirection);
            aimYaw = MountedRopeLauncherBlockEntity.clampMountedYaw(
                    (float) Math.toDegrees(Math.atan2(-localLookDirection.x, localLookDirection.z))
            );
            aimPitch = MountedRopeLauncherBlockEntity.clampMountedPitch(
                    (float) Math.toDegrees(Math.atan2(
                            -localLookDirection.y,
                            Math.sqrt(localLookDirection.x * localLookDirection.x + localLookDirection.z * localLookDirection.z)
                    ))
            );
        }

        float yaw = Mth.wrapDegrees(180.0F - aimYaw);
        float pitch = -MountedRopeLauncherBlockEntity.clampMountedPitch(aimPitch * PITCH_VISUAL_MULTIPLIER);

        var orientation = blockEntity.getOrientation();
        var x = orientation.toBlockSpace(new Vec3(1, 0, 0)).toVector3f();
        var y = orientation.toBlockSpace(new Vec3(0, 1, 0)).toVector3f();
        var z = orientation.toBlockSpace(new Vec3(0, 0, 1)).toVector3f();
        var rotation = new Quaternionf().setFromNormalized(new Matrix3f().setColumn(0, x).setColumn(1, y).setColumn(2, z));
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.solid());
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(rotation);
        poseStack.translate(-0.5D, -0.5D, -0.5D);

        if (orientation.up() != Direction.UP) {
            poseStack.pushPose();
            poseStack.translate(0.5D, 0.5D, 0.5D);
            poseStack.mulPose(Axis.XN.rotationDegrees(90));
            poseStack.translate(-0.5D, -0.5D, -0.5D);
            CachedBuffers.partial(BASE, state).light(light).renderInto(poseStack, vertexConsumer);
            poseStack.popPose();
        }

        poseStack.translate(0.5D, HEAD_PIVOT_Y, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.translate(-0.5D, HEAD_REST_Y_OFFSET, -0.5D);

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
