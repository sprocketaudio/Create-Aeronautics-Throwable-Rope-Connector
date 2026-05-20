package net.sprocketgames.create_aeronautics_throwable_rope_connector.client;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.entity.MountedRopeLauncherSeatEntity;

public final class MountedRopeLauncherSeatRenderer extends EntityRenderer<MountedRopeLauncherSeatEntity> {
    public MountedRopeLauncherSeatRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(MountedRopeLauncherSeatEntity entity, Frustum frustum, double x, double y, double z) {
        return false;
    }

    @Override
    public ResourceLocation getTextureLocation(MountedRopeLauncherSeatEntity entity) {
        return null;
    }
}
