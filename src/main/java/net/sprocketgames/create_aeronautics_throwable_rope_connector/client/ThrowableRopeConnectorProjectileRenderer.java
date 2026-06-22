package net.sprocketgames.create_aeronautics_throwable_rope_connector.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.equipment.zapper.ShootableGadgetItemMethods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.block.MountedRopeLauncherBlockEntity;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.config.ModCommonConfig;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.entity.ThrowableRopeConnectorProjectile;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.integration.CreateSimulatedIntegration;
import org.joml.Vector3f;

public final class ThrowableRopeConnectorProjectileRenderer extends EntityRenderer<ThrowableRopeConnectorProjectile> {
    private static final float MIN_CAMERA_DISTANCE_SQUARED = 12.25F;
    private static final ResourceLocation ROPE_TEXTURE = ResourceLocation.fromNamespaceAndPath("simulated", "textures/block/rope_particle.png");

    private final ItemRenderer itemRenderer;
    private final ItemStack connectorModelStack;

    public ThrowableRopeConnectorProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.connectorModelStack = CreateSimulatedIntegration.getRopeConnectorItem()
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
    }

    @Override
    protected int getBlockLightLevel(ThrowableRopeConnectorProjectile entity, BlockPos pos) {
        return super.getBlockLightLevel(entity, pos);
    }

    @Override
    public void render(ThrowableRopeConnectorProjectile entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        ItemStack stack = this.connectorModelStack.isEmpty() ? entity.getItem() : this.connectorModelStack;
        if (entity.tickCount >= 2 || !(this.entityRenderDispatcher.camera.getEntity().distanceToSqr(entity) < MIN_CAMERA_DISTANCE_SQUARED)) {
            poseStack.pushPose();
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            this.itemRenderer.renderStatic(
                    stack,
                    ItemDisplayContext.GROUND,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    poseStack,
                    buffer,
                    entity.level(),
                    entity.getId()
            );
            poseStack.popPose();
            this.renderRopeTrail(entity, partialTicks, poseStack, buffer, packedLight);
            super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        }
    }

    private void renderRopeTrail(ThrowableRopeConnectorProjectile entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (!ModCommonConfig.SHOW_PROJECTILE_ROPE_TRAIL.get()) {
            return;
        }

        Entity owner = entity.getOwner();
        Vec3 handPos;
        if (owner instanceof Player player) {
            handPos = this.getTrailStartPos(
                    player,
                    entity.isTrailFromOffhand(),
                    entity.isTrailFromLauncher(),
                    entity.isTrailFromMountedLauncher(),
                    partialTicks
            );
        } else if (entity.isTrailFromMountedLauncher()) {
            handPos = this.getMountedLauncherTrailStartPos(entity);
        } else {
            return;
        }
        Vec3 connectorPos = entity.getPosition(partialTicks).add(0.0D, 0.15D, 0.0D);
        float x = (float) (handPos.x - connectorPos.x);
        float y = (float) (handPos.y - connectorPos.y);
        float z = (float) (handPos.z - connectorPos.z);

        poseStack.pushPose();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(ROPE_TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        Vector3f cameraLeft = this.entityRenderDispatcher.camera.getLeftVector();
        Vector3f cameraUp = this.entityRenderDispatcher.camera.getUpVector();
        float halfWidth = (float) (ModCommonConfig.DEFAULT_PROJECTILE_ROPE_TRAIL_WIDTH * 0.5D);
        float leftX = cameraLeft.x() * halfWidth;
        float leftY = cameraLeft.y() * halfWidth;
        float leftZ = cameraLeft.z() * halfWidth;
        float upX = cameraUp.x() * halfWidth;
        float upY = cameraUp.y() * halfWidth;
        float upZ = cameraUp.z() * halfWidth;
        for (int segment = 0; segment < 24; segment++) {
            float start = fraction(segment, 24);
            float end = fraction(segment + 1, 24);
            boolean launcherTrail = entity.isTrailFromLauncher() || entity.isTrailFromMountedLauncher();
            ropeFace(x, y, z, consumer, pose, start, end, leftX, leftY, leftZ, upX, upY, upZ, packedLight, launcherTrail);
            ropeFace(x, y, z, consumer, pose, start, end, -leftX, -leftY, -leftZ, upX, upY, upZ, packedLight, launcherTrail);
            ropeFace(x, y, z, consumer, pose, start, end, upX, upY, upZ, leftX, leftY, leftZ, packedLight, launcherTrail);
            ropeFace(x, y, z, consumer, pose, start, end, -upX, -upY, -upZ, leftX, leftY, leftZ, packedLight, launcherTrail);
        }
        poseStack.popPose();
    }

    private Vec3 getMountedLauncherTrailStartPos(ThrowableRopeConnectorProjectile entity) {
        BlockPos mountedSourcePos = entity.getMountedSourcePos();
        if (mountedSourcePos != null && entity.level().getBlockEntity(mountedSourcePos) instanceof MountedRopeLauncherBlockEntity mountedLauncher) {
            return mountedLauncher.getAutomatedTrailStartPosition();
        }

        return entity.position();
    }

    private Vec3 getTrailStartPos(Player player, boolean offhand, boolean launcher, boolean mountedLauncher, float partialTicks) {
        if (mountedLauncher) {
            if (this.entityRenderDispatcher.options.getCameraType().isFirstPerson() && player == Minecraft.getInstance().player) {
                double fovScale = 960.0D / (double) this.entityRenderDispatcher.options.fov().get().intValue();
                Vec3 nearPlaneOffset = this.entityRenderDispatcher
                        .camera
                        .getNearPlane()
                        .getPointOnPlane(0.0F, -1.28F)
                        .scale(fovScale);
                return player.getEyePosition(partialTicks).add(nearPlaneOffset);
            }

            return player.getEyePosition(partialTicks).add(player.getLookAngle().normalize().scale(0.45D));
        }

        if (launcher) {
            return ShootableGadgetItemMethods.getGunBarrelVec(player, !offhand, new Vec3(0.75F, -0.15F, 1.5F));
        }

        int side = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        if (offhand) {
            side = -side;
        }

        if (this.entityRenderDispatcher.options.getCameraType().isFirstPerson() && player == Minecraft.getInstance().player) {
            double fovScale = 960.0D / (double) this.entityRenderDispatcher.options.fov().get().intValue();
            float horizontalOffset = (float) side * 0.72F;
            float verticalOffset = offhand ? -0.46F : -0.34F;
            Vec3 nearPlaneOffset = this.entityRenderDispatcher
                    .camera
                    .getNearPlane()
                    .getPointOnPlane(horizontalOffset, verticalOffset)
                    .scale(fovScale);
            return player.getEyePosition(partialTicks).add(nearPlaneOffset);
        }

        float bodyRot = Mth.lerp(partialTicks, player.yBodyRotO, player.yBodyRot) * (float) (Math.PI / 180.0D);
        double sin = Mth.sin(bodyRot);
        double cos = Mth.cos(bodyRot);
        float scale = player.getScale();
        double sideOffset = (double) side * 0.35D * (double) scale;
        double forwardOffset = 0.8D * (double) scale;
        float crouchOffset = player.isCrouching() ? -0.1875F : 0.0F;
        return player.getEyePosition(partialTicks)
                .add(-cos * sideOffset - sin * forwardOffset, (double) crouchOffset - 0.45D * (double) scale, -sin * sideOffset + cos * forwardOffset);
    }

    private static float fraction(int numerator, int denominator) {
        return (float) numerator / (float) denominator;
    }

    private static void ropeFace(
            float x,
            float y,
            float z,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float startFraction,
            float nextRopeFraction,
            float centerOffsetX,
            float centerOffsetY,
            float centerOffsetZ,
            float edgeOffsetX,
            float edgeOffsetY,
            float edgeOffsetZ,
            int packedLight,
            boolean launcherTrail
    ) {
        float startX = x * startFraction;
        float startY = launcherTrail ? launcherTrailY(y, startFraction) : y * (startFraction * startFraction + startFraction) * 0.5F + 0.2F;
        float startZ = z * startFraction;
        float endX = x * nextRopeFraction;
        float endY = launcherTrail ? launcherTrailY(y, nextRopeFraction) : y * (nextRopeFraction * nextRopeFraction + nextRopeFraction) * 0.5F + 0.2F;
        float endZ = z * nextRopeFraction;
        float normalX = endX - startX;
        float normalY = endY - startY;
        float normalZ = endZ - startZ;
        float normalLength = Mth.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
        if (normalLength > 0.0F) {
            normalX /= normalLength;
            normalY /= normalLength;
            normalZ /= normalLength;
        }

        ropeVertex(
                consumer, pose,
                startX + centerOffsetX + edgeOffsetX, startY + centerOffsetY + edgeOffsetY, startZ + centerOffsetZ + edgeOffsetZ,
                0.0F, startFraction * 8.0F, packedLight, normalX, normalY, normalZ
        );
        ropeVertex(
                consumer, pose,
                startX + centerOffsetX - edgeOffsetX, startY + centerOffsetY - edgeOffsetY, startZ + centerOffsetZ - edgeOffsetZ,
                1.0F, startFraction * 8.0F, packedLight, normalX, normalY, normalZ
        );
        ropeVertex(
                consumer, pose,
                endX + centerOffsetX - edgeOffsetX, endY + centerOffsetY - edgeOffsetY, endZ + centerOffsetZ - edgeOffsetZ,
                1.0F, nextRopeFraction * 8.0F, packedLight, normalX, normalY, normalZ
        );
        ropeVertex(
                consumer, pose,
                endX + centerOffsetX + edgeOffsetX, endY + centerOffsetY + edgeOffsetY, endZ + centerOffsetZ + edgeOffsetZ,
                0.0F, nextRopeFraction * 8.0F, packedLight, normalX, normalY, normalZ
        );
    }

    private static float launcherTrailY(float y, float fraction) {
        return y * fraction - Mth.sin(fraction * (float) Math.PI) * 0.25F;
    }

    private static void ropeVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            int packedLight,
            float normalX,
            float normalY,
            float normalZ
    ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, normalX, normalY, normalZ);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrowableRopeConnectorProjectile entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
