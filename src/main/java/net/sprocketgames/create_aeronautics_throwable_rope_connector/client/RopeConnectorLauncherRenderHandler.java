package net.sprocketgames.create_aeronautics_throwable_rope_connector.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.equipment.potatoCannon.PotatoProjectileEntity;
import com.simibubi.create.content.equipment.zapper.ShootableGadgetRenderHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.item.RopeConnectorLauncherItem;

public final class RopeConnectorLauncherRenderHandler extends ShootableGadgetRenderHandler {
    private float nextPitch = 0.8F;

    @Override
    protected void playSound(InteractionHand hand, Vec3 position) {
        PotatoProjectileEntity.playLaunchSound(Minecraft.getInstance().level, position, this.nextPitch);
    }

    @Override
    protected boolean appliesTo(ItemStack stack) {
        return stack.getItem() instanceof RopeConnectorLauncherItem;
    }

    @Override
    protected void transformTool(PoseStack poseStack, float flip, float equipProgress, float recoil, float partialTicks) {
        // Match Create's PotatoCannonRenderHandler transform for native first-person recoil.
        poseStack.translate(flip * -0.1F, 0.0F, 0.14F);
        poseStack.scale(0.75F, 0.75F, 0.75F);
        poseStack.mulPose(Axis.XP.rotationDegrees(recoil * 80.0F));
    }

    @Override
    protected void transformHand(PoseStack poseStack, float flip, float equipProgress, float recoil, float partialTicks) {
        // Match Create's PotatoCannonRenderHandler hand placement.
        poseStack.translate(flip * -0.09F, -0.275F, -0.25F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(flip * -10.0F));
    }

    public void beforeShoot(float pitch) {
        this.nextPitch = pitch;
    }
}
