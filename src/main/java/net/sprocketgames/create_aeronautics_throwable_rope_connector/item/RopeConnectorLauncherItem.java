package net.sprocketgames.create_aeronautics_throwable_rope_connector.item;

import java.util.function.Consumer;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.client.CreateAeronauticsThrowableRopeConnectorClient;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.client.RopeConnectorLauncherItemRenderer;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.config.ModCommonConfig;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.entity.ThrowableRopeConnectorProjectile;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.network.RopeConnectorLauncherShootPacket;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModItems;
import org.jetbrains.annotations.Nullable;

public final class RopeConnectorLauncherItem extends Item implements CustomArmPoseItem {
    private static final Component OFFHAND_AMMO_REQUIRED = Component.translatable("message.create_aeronautics_throwable_rope_connector.offhand_ammo_required");
    private static final float LAUNCHER_VELOCITY = 3.0F;

    public RopeConnectorLauncherItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack launcherStack = player.getItemInHand(usedHand);
        if (ShootableGadgetItemMethods.shouldSwap(player, launcherStack, usedHand, stack -> stack.getItem() instanceof RopeConnectorLauncherItem)) {
            return InteractionResultHolder.fail(launcherStack);
        }

        InteractionHand ammoHand = usedHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack ammoStack = player.getItemInHand(ammoHand);
        boolean mainHandLauncher = usedHand == InteractionHand.MAIN_HAND;
        if (!ammoStack.is(ModItems.THROWABLE_ROPE_CONNECTOR.get())) {
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

        ThrowableRopeConnectorProjectile projectile = new ThrowableRopeConnectorProjectile(level, barrelPos.x, barrelPos.y, barrelPos.z, ModCommonConfig.getClampedLauncherMaxDistance());
        projectile.setOwner(player);
        projectile.setSourceSlot(getSourceSlot(player, ammoHand));
        projectile.setTrailFromOffhand(usedHand == InteractionHand.OFF_HAND);
        projectile.setTrailFromLauncher(true);
        projectile.setConsumeOnSuccessOnly(ModCommonConfig.CONSUME_ON_SUCCESS_ONLY.get());
        projectile.setItem(new ItemStack(ModItems.THROWABLE_ROPE_CONNECTOR.get()));
        projectile.setDeltaMovement(motion);
        level.addFreshEntity(projectile);

        if (!ModCommonConfig.CONSUME_ON_SUCCESS_ONLY.get() && !player.hasInfiniteMaterials()) {
            ammoStack.consume(1, player);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        player.getCooldowns().addCooldown(this, ModCommonConfig.DEFAULT_COOLDOWN_TICKS);
        sendShootPackets(player, usedHand, barrelPos, 0.8F);
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, launcherStack);
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
