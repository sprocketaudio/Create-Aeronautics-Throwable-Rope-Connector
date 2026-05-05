package net.sprocketgames.create_aeronautics_throwable_rope_connector.item;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.config.ModCommonConfig;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.entity.ThrowableRopeConnectorProjectile;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModItems;

public final class ThrowableRopeConnectorItem extends Item implements ProjectileItem {
    public ThrowableRopeConnectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);
        if (isLauncherInOtherHand(player, usedHand)) {
            return InteractionResultHolder.pass(itemStack);
        }

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.SNOWBALL_THROW,
                SoundSource.NEUTRAL,
                0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F)
        );

        if (!level.isClientSide()) {
            ThrowableRopeConnectorProjectile projectile = new ThrowableRopeConnectorProjectile(level, player, ModCommonConfig.MAX_THROW_DISTANCE.get());
            projectile.setSourceSlot(getSourceSlot(player, usedHand));
            projectile.setTrailFromOffhand(usedHand == InteractionHand.OFF_HAND);
            projectile.setTrailFromLauncher(false);
            projectile.setConsumeOnSuccessOnly(ModCommonConfig.CONSUME_ON_SUCCESS_ONLY.get());
            projectile.setItem(itemStack);
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, ModCommonConfig.THROW_VELOCITY.get().floatValue(), 1.0F);
            level.addFreshEntity(projectile);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        player.getCooldowns().addCooldown(this, ModCommonConfig.COOLDOWN_TICKS.get());
        if (!ModCommonConfig.CONSUME_ON_SUCCESS_ONLY.get()) {
            itemStack.consume(1, player);
        }
        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    @Override
    public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        ThrowableRopeConnectorProjectile projectile = new ThrowableRopeConnectorProjectile(level, pos.x(), pos.y(), pos.z(), ModCommonConfig.MAX_THROW_DISTANCE.get());
        projectile.setConsumeOnSuccessOnly(ModCommonConfig.CONSUME_ON_SUCCESS_ONLY.get());
        projectile.setItem(stack);
        return projectile;
    }

    private static int getSourceSlot(Player player, InteractionHand usedHand) {
        return usedHand == InteractionHand.OFF_HAND ? Inventory.SLOT_OFFHAND : player.getInventory().selected;
    }

    private static boolean isLauncherInOtherHand(Player player, InteractionHand usedHand) {
        InteractionHand otherHand = usedHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        return player.getItemInHand(otherHand).is(ModItems.ROPE_CONNECTOR_LAUNCHER.get());
    }
}
