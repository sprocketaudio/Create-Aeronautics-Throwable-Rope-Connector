package net.sprocketgames.create_aeronautics_throwable_rope_connector.item;

import com.mojang.authlib.GameProfile;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.CreateAeronauticsThrowableRopeConnector;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.config.ModCommonConfig;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.integration.AnchorProtection;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.integration.CreateSimulatedIntegration;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModItems;

public final class ThrowableRopeConnectorPlacement {
    public static final Component CANNOT_ATTACH_THERE = Component.translatable("message.create_aeronautics_throwable_rope_connector.cannot_attach_there");
    private static final Component MISSING_SIMULATED = Component.translatable("message.create_aeronautics_throwable_rope_connector.missing_simulated");
    private static final GameProfile AUTOMATED_LAUNCHER = new GameProfile(UUID.fromString("945effc4-7c28-44cc-b6b0-a688a7fedebb"), "[Rope Launcher]");

    private ThrowableRopeConnectorPlacement() {
    }

    public static boolean placeConnectorAndGiveRope(ServerLevel level, ServerPlayer player, BlockHitResult hitResult, int sourceSlot, boolean consumeOnSuccessOnly) {
        Optional<PlacementTarget> target = resolvePlacementTarget(level, player, hitResult);
        if (target.isEmpty()) {
            return false;
        }

        Optional<BlockItem> maybeConnectorItem = CreateSimulatedIntegration.getRopeConnectorItem();
        Optional<ItemStack> maybeRopeStack = CreateSimulatedIntegration.createPreselectedRope(target.get().placePos());
        if (maybeConnectorItem.isEmpty() || maybeRopeStack.isEmpty()) {
            CreateAeronauticsThrowableRopeConnector.LOGGER.error("Simulated integration registry lookup failed.");
            fail(player, MISSING_SIMULATED);
            return false;
        }

        if (consumeOnSuccessOnly && !canConsumeThrowable(player, sourceSlot)) {
            return false;
        }

        ItemStack connectorPlacementStack = new ItemStack(maybeConnectorItem.get());
        BlockHitResult placeHit = new BlockHitResult(target.get().hitLocation(), target.get().face(), target.get().targetPos(), false);
        BlockPlaceContext placeContext = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, connectorPlacementStack, placeHit);
        if (!placeContext.getClickedPos().equals(target.get().placePos()) || !connectorPlacementStack.useOn(placeContext).consumesAction()) {
            return false;
        }

        giveToSourceSlot(player, maybeRopeStack.get(), sourceSlot, ModItems.THROWABLE_ROPE_CONNECTOR.get(), consumeOnSuccessOnly);
        player.getCooldowns().addCooldown(ModItems.THROWABLE_ROPE_CONNECTOR.get(), ModCommonConfig.DEFAULT_COOLDOWN_TICKS);

        if (ModCommonConfig.PLAY_PARTICLES.get()) {
            level.sendParticles(
                    ParticleTypes.POOF,
                    target.get().placePos().getX() + 0.5D,
                    target.get().placePos().getY() + 0.5D,
                    target.get().placePos().getZ() + 0.5D,
                    6,
                    0.15D,
                    0.15D,
                    0.15D,
                    0.01D
            );
        }

        return true;
    }

    public static Optional<BlockPos> placeConnectorFromMountedLauncher(ServerLevel level, BlockHitResult hitResult, @Nullable ServerPlayer player) {
        ServerPlayer actor = player;
        if (actor == null) {
            actor = FakePlayerFactory.get(level, AUTOMATED_LAUNCHER);
            Direction face = hitResult.getDirection();
            actor.setYRot((face.getAxis().isHorizontal() ? face : Direction.NORTH).toYRot());
        }
        Optional<PlacementTarget> target = resolvePlacementTarget(level, actor, hitResult);
        if (target.isEmpty()) {
            return Optional.empty();
        }

        Optional<BlockItem> maybeConnectorItem = CreateSimulatedIntegration.getRopeConnectorItem();
        if (maybeConnectorItem.isEmpty()) {
            CreateAeronauticsThrowableRopeConnector.LOGGER.error("Simulated integration registry lookup failed.");
            return Optional.empty();
        }

        ItemStack connectorPlacementStack = new ItemStack(maybeConnectorItem.get());
        BlockPlaceContext placeContext = new BlockPlaceContext(
                level, actor, InteractionHand.MAIN_HAND, connectorPlacementStack, hitResult
        );
        BlockPos placePos = target.get().placePos();
        if (!placeContext.getClickedPos().equals(placePos)) {
            return Optional.empty();
        }
        if (player != null) {
            return connectorPlacementStack.useOn(placeContext).consumesAction() ? Optional.of(placePos) : Optional.empty();
        }
        // Automated placement needs its own event and rollback for cancellable placement hooks.
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, placePos);
        if (!maybeConnectorItem.get().place(placeContext).consumesAction()) {
            return Optional.empty();
        }
        if (EventHooks.onBlockPlace(actor, snapshot, target.get().face())) {
            snapshot.restore();
            return Optional.empty();
        }
        return Optional.of(placePos);
    }

    public static void returnThrowableItem(Player player, int sourceSlot) {
        if (player.hasInfiniteMaterials()) {
            return;
        }

        giveThrowableToSourceSlot(player, new ItemStack(ModItems.THROWABLE_ROPE_CONNECTOR.get()), sourceSlot);
    }

    public static boolean prepareDockCoupling(Player player, BlockPos connectorPos, ItemStack ammo, int sourceSlot, boolean ammoPaid) {
        ItemStack prepared = ammo.copy();
        if (!CreateSimulatedIntegration.setFirstConnection(prepared, connectorPos)) {
            return false;
        }
        if (!ammoPaid && !player.hasInfiniteMaterials()) {
            int slot = isValidPlayerInventorySlot(player, sourceSlot)
                    && ItemStack.isSameItemSameComponents(player.getInventory().getItem(sourceSlot), ammo) ? sourceSlot : -1;
            for (int index = 0; slot < 0 && index < player.getInventory().getContainerSize(); index++) {
                if (isValidPlayerInventorySlot(player, index)
                        && ItemStack.isSameItemSameComponents(player.getInventory().getItem(index), ammo)) {
                    slot = index;
                }
            }
            if (slot < 0) {
                return false;
            }
            player.getInventory().removeItem(slot, 1);
        }
        giveToSourceSlot(player, prepared, sourceSlot, ammo.getItem(), false);
        return true;
    }

    public static void returnLauncherAmmo(Player player, ItemStack stack, int sourceSlot) {
        giveThrowableToSourceSlot(player, stack, sourceSlot);
    }

    public static void fail(Player player, @Nullable Component message) {
        if (player.level().isClientSide()) {
            return;
        }

        if (message != null && ModCommonConfig.SHOW_FAILURE_MESSAGES.get()) {
            player.displayClientMessage(message, true);
        }

        player.level().playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.5F, 1.0F);
    }

    private static Optional<PlacementTarget> resolvePlacementTarget(ServerLevel level, ServerPlayer player, BlockHitResult hitResult) {
        BlockPos targetPos = hitResult.getBlockPos();
        Direction face = hitResult.getDirection();
        BlockPos placePos = targetPos.relative(face);

        if (!AnchorProtection.canPlace(level, placePos, player) || !level.mayInteract(player, AnchorProtection.projectToWorld(level, targetPos))) {
            return Optional.empty();
        }

        if (!player.mayUseItemAt(placePos, face, new ItemStack(ModItems.THROWABLE_ROPE_CONNECTOR.get()))) {
            return Optional.empty();
        }

        BlockState targetState = level.getBlockState(targetPos);
        if (!targetState.isFaceSturdy(level, targetPos, face)) {
            return Optional.empty();
        }

        BlockState placeState = level.getBlockState(placePos);
        FluidState fluidState = level.getFluidState(placePos);
        if (!placeState.canBeReplaced() || !fluidState.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new PlacementTarget(targetPos, placePos, face, hitResult.getLocation()));
    }

    private static void giveToPlayer(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            dropNearPlayer(player, stack);
        }
    }

    private static void giveToSourceSlot(Player player, ItemStack stack, int sourceSlot, Item ammoItem, boolean consumeThrowableFromInventory) {
        if (consumeThrowableFromInventory && !consumeThrowable(player, sourceSlot)) {
            giveToPlayer(player, stack);
            return;
        }

        if (!isValidPlayerInventorySlot(player, sourceSlot)) {
            giveToPlayer(player, stack);
            return;
        }

        ItemStack current = player.getInventory().getItem(sourceSlot);
        if (current.isEmpty()) {
            player.getInventory().setItem(sourceSlot, stack);
            return;
        }

        if (current.is(ammoItem) && CreateSimulatedIntegration.isLauncherAmmo(current)) {
            if (moveStackOutOfSourceSlot(player, current.copy(), sourceSlot)) {
                player.getInventory().setItem(sourceSlot, stack);
            } else {
                dropNearPlayer(player, stack);
            }
            return;
        }

        giveToPlayer(player, stack);
    }

    private static void giveThrowableToSourceSlot(Player player, ItemStack stack, int sourceSlot) {
        if (!isValidPlayerInventorySlot(player, sourceSlot)) {
            giveToPlayer(player, stack);
            return;
        }

        ItemStack current = player.getInventory().getItem(sourceSlot);
        if (current.isEmpty()) {
            player.getInventory().setItem(sourceSlot, stack);
            return;
        }

        if (ItemStack.isSameItemSameComponents(current, stack) && current.getCount() < current.getMaxStackSize()) {
            current.grow(stack.getCount());
            return;
        }

        giveToPlayer(player, stack);
    }

    private static boolean canConsumeThrowable(Player player, int sourceSlot) {
        if (player.hasInfiniteMaterials()) {
            return true;
        }

        if (isValidPlayerInventorySlot(player, sourceSlot) && player.getInventory().getItem(sourceSlot).is(ModItems.THROWABLE_ROPE_CONNECTOR.get())) {
            return true;
        }

        return findThrowableSlot(player) >= 0;
    }

    private static boolean consumeThrowable(Player player, int sourceSlot) {
        if (player.hasInfiniteMaterials()) {
            return true;
        }

        int slot = isValidPlayerInventorySlot(player, sourceSlot) && player.getInventory().getItem(sourceSlot).is(ModItems.THROWABLE_ROPE_CONNECTOR.get())
                ? sourceSlot
                : findThrowableSlot(player);
        if (slot < 0) {
            return false;
        }

        player.getInventory().removeItem(slot, 1);
        return true;
    }

    private static int findThrowableSlot(Player player) {
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            if (player.getInventory().getItem(slot).is(ModItems.THROWABLE_ROPE_CONNECTOR.get())) {
                return slot;
            }
        }

        return player.getInventory().getItem(Inventory.SLOT_OFFHAND).is(ModItems.THROWABLE_ROPE_CONNECTOR.get()) ? Inventory.SLOT_OFFHAND : -1;
    }

    private static boolean isValidPlayerInventorySlot(Player player, int slot) {
        return slot >= 0 && slot < player.getInventory().getContainerSize()
                && (slot < player.getInventory().items.size() || slot == Inventory.SLOT_OFFHAND);
    }

    private static boolean moveStackOutOfSourceSlot(Player player, ItemStack stack, int sourceSlot) {
        if (stack.isEmpty()) {
            player.getInventory().setItem(sourceSlot, ItemStack.EMPTY);
            return true;
        }

        int mergeSlot = findCompatibleSlot(player, stack, sourceSlot);
        if (mergeSlot >= 0) {
            ItemStack target = player.getInventory().getItem(mergeSlot);
            if (target.isEmpty()) {
                player.getInventory().setItem(mergeSlot, stack);
            } else {
                target.grow(stack.getCount());
            }
            player.getInventory().setItem(sourceSlot, ItemStack.EMPTY);
            return true;
        }

        return false;
    }

    private static int findCompatibleSlot(Player player, ItemStack stack, int sourceSlot) {
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            if (slot == sourceSlot) {
                continue;
            }

            ItemStack target = player.getInventory().getItem(slot);
            if (ItemStack.isSameItemSameComponents(target, stack) && target.getCount() + stack.getCount() <= target.getMaxStackSize()) {
                return slot;
            }
        }

        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            if (slot == sourceSlot) {
                continue;
            }

            if (player.getInventory().getItem(slot).isEmpty()) {
                return slot;
            }
        }

        return -1;
    }

    private static void dropNearPlayer(Player player, ItemStack stack) {
        ItemEntity itemEntity = player.drop(stack, false);
        if (itemEntity == null) {
            return;
        }

        net.minecraft.world.phys.Vec3 forward = player.getLookAngle();
        net.minecraft.world.phys.Vec3 spawnPos = player.position()
                .add(forward.x * 0.18D, player.getBbHeight() * 0.45D, forward.z * 0.18D);
        itemEntity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);

        // Gentle nudge forward so it's visible, but doesn't get flung like vanilla toss.
        itemEntity.setDeltaMovement(forward.x * 0.06D, 0.06D, forward.z * 0.06D);
    }

    private record PlacementTarget(BlockPos targetPos, BlockPos placePos, Direction face, net.minecraft.world.phys.Vec3 hitLocation) {
    }
}
