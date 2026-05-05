package net.sprocketgames.create_aeronautics_throwable_rope_connector.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.CreateAeronauticsThrowableRopeConnector;

public final class RopeConnectorLauncherItemRenderer extends CustomRenderedItemModelRenderer {
    private static final PartialModel COG = PartialModel.of(ResourceLocation.fromNamespaceAndPath(
            CreateAeronauticsThrowableRopeConnector.MOD_ID,
            "item/rope_connector_launcher/cog"
    ));

    @Override
    protected void render(
            ItemStack stack,
            CustomRenderedItemModel model,
            PartialItemModelRenderer renderer,
            ItemDisplayContext transformType,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int light,
            int overlay
    ) {
        renderer.render(model.getOriginalModel(), light);

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        float angle = AnimationTickHolder.getRenderTime() * -2.5F;
        if (player != null) {
            boolean inMainHand = player.getMainHandItem() == stack;
            boolean inOffHand = player.getOffhandItem() == stack;
            if (inMainHand || inOffHand) {
                boolean leftHanded = player.getMainArm() == HumanoidArm.LEFT;
                float recoil = CreateAeronauticsThrowableRopeConnectorClient.LAUNCHER_RENDER_HANDLER.getAnimation(
                        inMainHand ^ leftHanded,
                        AnimationTickHolder.getPartialTicks()
                );
                angle += 360.0F * Mth.clamp(recoil * 5.0F, 0.0F, 1.0F);
            }
        }
        angle %= 360.0F;

        float offset = 0.5F / 16.0F;
        poseStack.pushPose();
        poseStack.translate(0.0F, offset, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
        poseStack.translate(0.0F, -offset, 0.0F);
        renderer.render(COG.get(), light);
        poseStack.popPose();
    }
}
