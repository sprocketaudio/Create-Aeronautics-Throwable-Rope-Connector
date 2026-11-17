package net.sprocketgames.create_aeronautics_throwable_rope_connector.item;

import java.util.function.Consumer;
import java.util.List;
import com.simibubi.create.content.equipment.zapper.ShootableGadgetItemMethods;
import com.simibubi.create.foundation.item.CustomArmPoseItem;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.client.CreateAeronauticsThrowableRopeConnectorClient;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.client.RopeConnectorLauncherItemRenderer;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.config.ModCommonConfig;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.entity.ThrowableRopeConnectorProjectile;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.network.RopeConnectorLauncherShootPacket;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.integration.CreateSimulatedIntegration;
import org.jetbrains.annotations.Nullable;

public final class RopeConnectorLauncherItem extends Item implements CustomArmPoseItem {
    private static final Component OFFHAND_AMMO_REQUIRED = Component.translatable("message.create_aeronautics_throwable_rope_connector.offhand_ammo_required");
    private static final float LAUNCHER_VELOCITY = 3.0F;

    public RopeConnectorLauncherItem(Properties properties) {
        super(properties);
    }

    public static void skipCouplingUseWhenAimingLauncher(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (event.getHand() == InteractionHand.MAIN_HAND && off.getItem() instanceof RopeConnectorLauncherItem
                && CreateSimulatedIntegration.isRopeCoupling(main) && CreateSimulatedIntegration.isLauncherAmmo(main)
                && CreateSimulatedIntegration.isRopeConnector(event.getLevel(), event.getPos())) {
            event.setUseItem(net.neoforged.neoforge.common.util.TriState.FALSE);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack launcherStack = player.getItemInHand(usedHand);
        InteractionHand ammoHand = usedHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack ammoStack = player.getItemInHand(ammoHand);
        if (CreateSimulatedIntegration.isPreparedRopeCoupling(ammoStack)) {
            return InteractionResultHolder.pass(launcherStack);
        }
        if (ShootableGadgetItemMethods.shouldSwap(player, launcherStack, usedHand, stack -> stack.getItem() instanceof RopeConnectorLauncherItem)) {
            return InteractionResultHolder.fail(launcherStack);
        }

        boolean mainHandLauncher = usedHand == InteractionHand.MAIN_HAND;
        if (!CreateSimulatedIntegration.isLauncherAmmo(ammoStack)) {
            if (!level.isClientSide()) {
                ThrowableRopeConnectorPlacement.fail(player, OFFHAND_AMMO_REQUIRED);
            }
            return InteractionResultHolder.fail(launcherStack);
        }

        Vec3 barrelPos = ShootableGadgetItemMethods.getGunBarrelVec(player, mainHandLauncher, new Vec3(0.75F, -0.15F, 1.5F));

        if (level.isClientSide()) {
            CreateAeronauticsThrowableRopeConnectorClient.LAUNCHER_RENDER_HANDLER.dontAnimateItem(usedHand);
            return InteractionResultHolder.success(launcherStack);
        }

        Vec3 correction = ShootableGadgetItemMethods.getGunBarrelVec(player, mainHandLauncher, new Vec3(-0.05F, 0.0F, 0.0F))
                .subtract(player.position().add(0.0D, player.getEyeHeight(), 0.0D));
        Vec3 motion = player.getLookAngle().add(correction).normalize().scale(LAUNCHER_VELOCITY);

        boolean coupling = CreateSimulatedIntegration.isRopeCoupling(ammoStack);
        Vec3 origin = coupling ? player.getEyePosition() : barrelPos;
        ThrowableRopeConnectorProjectile projectile = new ThrowableRopeConnectorProjectile(
                level, origin.x, origin.y, origin.z, ModCommonConfig.getClampedLauncherMaxDistance());
        projectile.setOwner(player);
        projectile.setSourceSlot(getSourceSlot(player, ammoHand));
        projectile.setTrailFromOffhand(usedHand == InteractionHand.OFF_HAND);
        projectile.setTrailFromLauncher(true);
        boolean consumeOnSuccessOnly = ModCommonConfig.CONSUME_ON_SUCCESS_ONLY.get();
        projectile.setConsumeOnSuccessOnly(consumeOnSuccessOnly);
        projectile.setItem(ammoStack.copyWithCount(1));
        projectile.setAmmoPaid(!consumeOnSuccessOnly && !player.hasInfiniteMaterials());
        projectile.setNoGravity(coupling);
        projectile.setDeltaMovement(coupling ? player.getLookAngle().scale(LAUNCHER_VELOCITY) : motion);
        if (!level.addFreshEntity(projectile)) {
            return InteractionResultHolder.fail(launcherStack);
        }

        if (!consumeOnSuccessOnly && !player.hasInfiniteMaterials()) {
            ammoStack.consume(1, player);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        player.getCooldowns().addCooldown(this, ModCommonConfig.DEFAULT_COOLDOWN_TICKS);
        sendShootPackets(player, usedHand, barrelPos, 0.8F);
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, launcherStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("message.create_aeronautics_throwable_rope_connector.offhand_ammo_required")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.create_aeronautics_throwable_rope_connector.launcher_redocking")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    private static int getSourceSlot(Player player, InteractionHand ammoHand) {
        return ammoHand == InteractionHand.OFF_HAND ? Inventory.SLOT_OFFHAND : player.getInventory().selected;
    }

    private static void sendShootPackets(Player player, InteractionHand hand, Vec3 barrelPos, float pitch) {
        RopeConnectorLauncherShootPacket trackingPacket = new RopeConnectorLauncherShootPacket(barrelPos, hand, pitch, false);
        PacketDistributor.sendToPlayersTrackingEntity(player, trackingPacket);
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new RopeConnectorLauncherShootPacket(barrelPos, hand, pitch, true));
        }
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
        return true;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    @Nullable
    public ArmPose getArmPose(ItemStack stack, AbstractClientPlayer player, InteractionHand hand) {
        return player.swinging ? null : ArmPose.CROSSBOW_HOLD;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(SimpleCustomRenderer.create(this, new RopeConnectorLauncherItemRenderer()));
    }
}
