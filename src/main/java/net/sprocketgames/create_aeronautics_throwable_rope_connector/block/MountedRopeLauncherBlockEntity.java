package net.sprocketgames.create_aeronautics_throwable_rope_connector.block;

import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchBlockEntity;
import dev.simulated_team.simulated.content.items.rope.RopeItem.RopeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.CreateAeronauticsThrowableRopeConnector;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.config.ModCommonConfig;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.entity.MountedRopeLauncherSeatEntity;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.entity.ThrowableRopeConnectorProjectile;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.item.ThrowableRopeConnectorPlacement;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModBlockEntityTypes;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModItems;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public final class MountedRopeLauncherBlockEntity extends RopeWinchBlockEntity {
    private static final float MIN_MOUNTED_PITCH = -30.0F;
    private static final float MAX_MOUNTED_PITCH = 30.0F;
    private static final float MAX_MOUNTED_YAW_OFFSET = 80.0F;
    private static final Component NO_AMMO = Component.translatable("message.create_aeronautics_throwable_rope_connector.mounted_no_ammo");
    private static final Component ALREADY_CONNECTED = Component.translatable("message.create_aeronautics_throwable_rope_connector.mounted_already_connected");
    private static final Component BLOCKED = Component.translatable("message.create_aeronautics_throwable_rope_connector.mounted_blocked");
    private static final Component ANCHOR_CONNECTED = Component.translatable("message.create_aeronautics_throwable_rope_connector.mounted_anchor_connected");
    private static final Component ANCHOR_FAILED = Component.translatable("message.create_aeronautics_throwable_rope_connector.mounted_anchor_failed");
    private static final Component RELEASE_DISABLED = Component.translatable("message.create_aeronautics_throwable_rope_connector.mounted_release_disabled");

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ModItems.THROWABLE_ROPE_CONNECTOR.get());
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final IItemHandler automationInventory = new IItemHandler() {
        @Override
        public int getSlots() {
            return inventory.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return inventory.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return inventory.isItemValid(slot, stack);
        }
    };

    private BlockPos remoteConnectorPos;
    private float aimYaw;
    private float aimPitch;
    private long nextFireGameTime;
    private boolean pendingAmmoConsumed;
    private boolean fireRedstonePowered;
    private boolean releaseRedstonePowered;

    public MountedRopeLauncherBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.aimYaw = yawFromFacing(state.getValue(MountedRopeLauncherBlock.FACING));
    }

    public MountedRopeLauncherBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntityTypes.MOUNTED_ROPE_LAUNCHER.get(), pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntityTypes.MOUNTED_ROPE_LAUNCHER.get(),
                (blockEntity, side) -> blockEntity.getAutomationInventory()
        );
    }

    public void loadAmmo(ServerPlayer player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!this.inventory.isItemValid(0, held)) {
            return;
        }

        ItemStack insert = held.copy();
        ItemStack remainder = this.inventory.insertItem(0, insert, false);
        int inserted = insert.getCount() - remainder.getCount();
        if (inserted <= 0) {
            return;
        }

        if (!player.isCreative()) {
            held.shrink(inserted);
        }

        this.playClick();
        this.setChanged();
    }

    public void fire(@Nullable ServerPlayer player) {
        this.fireInDirection(player, Vec3.atLowerCornerOf(this.getFacing().getNormal()), this.getBarrelPosition(), true);
    }

    public void fireFromRider(ServerPlayer player) {
        float clampedPitch = clampMountedPitch(player.getXRot());
        float clampedYaw = clampYawForFacing(this.getFacing(), player.getYRot());
        Vec3 direction = Vec3.directionFromRotation(clampedPitch, clampedYaw).normalize();
        Vec3 launchPos = player.getEyePosition().add(direction.scale(0.45D));
        this.fireInDirection(player, direction, launchPos, false);
    }

    public void fireFromSavedAim() {
        float clampedPitch = clampMountedPitch(this.aimPitch);
        float clampedYaw = clampYawForFacing(this.getFacing(), this.aimYaw);
        Vec3 direction = Vec3.directionFromRotation(clampedPitch, clampedYaw).normalize();
        this.fireInDirection(null, direction, this.getRedstoneBarrelPosition(), false);
    }

    private void fireInDirection(@Nullable ServerPlayer player, Vec3 direction, Vec3 launchPos, boolean checkFixedFront) {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (serverLevel.getGameTime() < this.nextFireGameTime) {
            return;
        }

        if (this.isConnected()) {
            this.message(player, ALREADY_CONNECTED);
            return;
        }

        if (this.inventory.getStackInSlot(0).isEmpty()) {
            this.message(player, NO_AMMO);
            return;
        }

        boolean consumeOnSuccessOnly = ModCommonConfig.CONSUME_ON_SUCCESS_ONLY.get();
        if (!consumeOnSuccessOnly) {
            if (this.inventory.extractItem(0, 1, false).isEmpty()) {
                this.message(player, NO_AMMO);
                return;
            }
            this.pendingAmmoConsumed = true;
        }

        BlockPos front = this.worldPosition.above().relative(this.getFacing());
        if (checkFixedFront && !serverLevel.getBlockState(front).getCollisionShape(serverLevel, front).isEmpty()) {
            if (!consumeOnSuccessOnly) {
                this.refundPendingAmmo();
            }
            this.message(player, BLOCKED);
            serverLevel.playSound(null, this.worldPosition, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.5F, 1.0F);
            return;
        }

        ThrowableRopeConnectorProjectile projectile = new ThrowableRopeConnectorProjectile(
                serverLevel,
                launchPos.x,
                launchPos.y,
                launchPos.z,
                ModCommonConfig.getClampedMountedLauncherRange()
        );
        projectile.setMountedSource(this.worldPosition);
        if (player != null) {
            projectile.setOwner(player);
        }
        projectile.setTrailFromMountedLauncher(true);
        projectile.setItem(new ItemStack(ModItems.THROWABLE_ROPE_CONNECTOR.get()));
        projectile.setDeltaMovement(direction.normalize().scale(3.0D));
        serverLevel.addFreshEntity(projectile);
        serverLevel.playSound(null, this.worldPosition, SoundEvents.CROSSBOW_SHOOT, SoundSource.BLOCKS, 0.8F, 0.8F);
        this.nextFireGameTime = serverLevel.getGameTime() + Math.max(2, ModCommonConfig.DEFAULT_COOLDOWN_TICKS);
        this.setChanged();
    }

    public void mount(ServerPlayer player) {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (player.getVehicle() instanceof MountedRopeLauncherSeatEntity currentSeat && this.worldPosition.equals(currentSeat.getLauncherPos())) {
            return;
        }

        MountedRopeLauncherSeatEntity seat = this.findSeat(serverLevel);
        if (seat == null) {
            seat = new MountedRopeLauncherSeatEntity(serverLevel, this.worldPosition);
            serverLevel.addFreshEntity(seat);
        }

        player.startRiding(seat, true);
        this.setAim(player.getYRot(), player.getXRot());
    }

    @Nullable
    private MountedRopeLauncherSeatEntity findSeat(ServerLevel level) {
        AABB searchBounds = new AABB(this.worldPosition).inflate(2.0D);
        for (MountedRopeLauncherSeatEntity seat : level.getEntitiesOfClass(MountedRopeLauncherSeatEntity.class, searchBounds)) {
            if (seat.isRemoved()) {
                continue;
            }

            if (this.worldPosition.equals(seat.getLauncherPos())) {
                if (!seat.isVehicle()) {
                    seat.discard();
                    continue;
                }
                return seat;
            }
        }
        return null;
    }

    @Nullable
    public Entity getControllingPassenger() {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return null;
        }

        MountedRopeLauncherSeatEntity seat = this.findSeat(serverLevel);
        return seat == null ? null : seat.getFirstPassenger();
    }

    public void setAim(float yaw, float pitch) {
        float clampedYaw = clampYawForFacing(this.getFacing(), yaw);
        float clampedPitch = clampMountedPitch(pitch);
        if (Math.abs(this.aimYaw - clampedYaw) < 0.5F && Math.abs(this.aimPitch - clampedPitch) < 0.5F) {
            return;
        }

        this.aimYaw = clampedYaw;
        this.aimPitch = clampedPitch;
        this.notifyUpdate();
    }

    public float getAimYaw() {
        return this.aimYaw;
    }

    public float getAimPitch() {
        return this.aimPitch;
    }

    public boolean connectToPlacedConnector(ServerLevel level, BlockPos connectorPos, @Nullable ServerPlayer player) {
        RopeStrandHolderBehavior localHolder = this.getBehavior();
        RopeStrandHolderBehavior remoteHolder = RopeItem.getRopeHolder(level, connectorPos);
        if (remoteHolder == null || remoteHolder.isAttached() || localHolder == null || localHolder.isAttached()) {
            this.message(player, ANCHOR_FAILED);
            return false;
        }

        boolean consumeOnSuccessOnly = ModCommonConfig.CONSUME_ON_SUCCESS_ONLY.get();
        if (consumeOnSuccessOnly && this.inventory.extractItem(0, 1, true).isEmpty()) {
            this.message(player, NO_AMMO);
            return false;
        }

        if (!this.tryCreateRopeCompat(localHolder, remoteHolder)) {
            this.message(player, ANCHOR_FAILED);
            return false;
        }

        if (consumeOnSuccessOnly) {
            this.inventory.extractItem(0, 1, false);
        } else {
            this.pendingAmmoConsumed = false;
        }
        this.remoteConnectorPos = connectorPos.immutable();
        this.message(player, ANCHOR_CONNECTED);
        this.setChanged();
        return true;
    }

    public void onProjectileFailed() {
        this.refundPendingAmmo();
        this.setChanged();
    }

    public void release(@Nullable ServerPlayer player) {
        if (!(this.level instanceof ServerLevel level)) {
            return;
        }

        RopeStrandHolderBehavior localHolder = this.getBehavior();
        boolean ropeReleased = true;
        if (localHolder != null && localHolder.isAttached()) {
            ServerPlayer releasePlayer = player != null ? player : this.findAutomationReleasePlayer(level);
            ropeReleased = this.tryDestroyRopeCompat(localHolder, releasePlayer, this.getAttachmentPoint());
            if (!ropeReleased) {
                return;
            }
        }

        if (this.remoteConnectorPos != null && ModCommonConfig.canMountedLauncherRemoteRelease()) {
            level.removeBlock(this.remoteConnectorPos, false);
        }

        this.remoteConnectorPos = null;
        this.setChanged();
    }

    public void onBroken() {
        this.release(null);
        if (this.level != null) {
            ItemStack ammo = this.inventory.getStackInSlot(0);
            if (!ammo.isEmpty()) {
                Containers.dropItemStack(this.level, this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D, this.worldPosition.getZ() + 0.5D, ammo.copy());
            }
        }
    }

    public boolean isConnected() {
        RopeStrandHolderBehavior localHolder = this.getBehavior();
        return localHolder != null && localHolder.isAttached();
    }

    public int getAmmoCount() {
        return this.inventory.getStackInSlot(0).getCount();
    }

    public IItemHandler getAutomationInventory() {
        return this.automationInventory;
    }

    public boolean unloadAmmo(Player player) {
        ItemStack unloaded = this.inventory.getStackInSlot(0).copy();
        if (unloaded.isEmpty()) {
            return false;
        }

        this.inventory.setStackInSlot(0, ItemStack.EMPTY);
        this.setChanged();
        this.playClick();

        if (!player.getInventory().add(unloaded)) {
            ItemEntity itemEntity = player.drop(unloaded, false);
            if (itemEntity != null) {
                Vec3 spawnPos = player.position().add(0.0D, player.getBbHeight() * 0.45D, 0.0D);
                itemEntity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                itemEntity.setDeltaMovement(Vec3.ZERO);
            }
        }

        return true;
    }

    public void handleRedstoneSignal(boolean firePowered, boolean releasePowered) {
        if (!ModCommonConfig.canMountedLauncherFireFromRedstone()) {
            this.fireRedstonePowered = firePowered;
            this.releaseRedstonePowered = releasePowered;
            return;
        }

        boolean fireRisingEdge = firePowered && !this.fireRedstonePowered;
        boolean releaseRisingEdge = releasePowered && !this.releaseRedstonePowered;
        this.fireRedstonePowered = firePowered;
        this.releaseRedstonePowered = releasePowered;

        if (fireRisingEdge) {
            this.fireFromSavedAim();
        }

        if (releaseRisingEdge) {
            this.tryRemoteRelease(null, false);
        }
    }

    public boolean tryRemoteRelease(@Nullable ServerPlayer player) {
        return this.tryRemoteRelease(player, true);
    }

    public boolean tryRemoteRelease(@Nullable ServerPlayer player, boolean showDisabledMessage) {
        if (!this.isConnected()) {
            return false;
        }

        if (this.remoteConnectorPos != null && !ModCommonConfig.canMountedLauncherRemoteRelease()) {
            if (showDisabledMessage) {
                this.message(player, RELEASE_DISABLED);
            }
            return false;
        }

        this.release(player);
        return true;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Inventory", this.inventory.serializeNBT(registries));
        tag.putFloat("AimYaw", this.aimYaw);
        tag.putFloat("AimPitch", this.aimPitch);
        tag.putBoolean("PendingAmmoConsumed", this.pendingAmmoConsumed);
        tag.putBoolean("FireRedstonePowered", this.fireRedstonePowered);
        tag.putBoolean("ReleaseRedstonePowered", this.releaseRedstonePowered);
        if (this.remoteConnectorPos != null) {
            tag.putLong("RemoteConnectorPos", this.remoteConnectorPos.asLong());
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        this.inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        this.aimYaw = tag.getFloat("AimYaw");
        this.aimPitch = tag.getFloat("AimPitch");
        this.pendingAmmoConsumed = tag.getBoolean("PendingAmmoConsumed");
        this.fireRedstonePowered = tag.getBoolean("FireRedstonePowered");
        this.releaseRedstonePowered = tag.getBoolean("ReleaseRedstonePowered");
        this.remoteConnectorPos = tag.contains("RemoteConnectorPos") ? BlockPos.of(tag.getLong("RemoteConnectorPos")) : null;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return super.getRenderBoundingBox().minmax(new AABB(this.worldPosition.above())).inflate(1.0D);
    }

    private Direction getFacing() {
        return this.getBlockState().getValue(MountedRopeLauncherBlock.FACING);
    }

    private static float yawFromFacing(Direction facing) {
        return switch (facing) {
            case NORTH -> 180.0F;
            case EAST -> -90.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }

    public static float clampYawForFacing(Direction facing, float yaw) {
        float baseYaw = yawFromFacing(facing);
        float delta = Mth.wrapDegrees(yaw - baseYaw);
        return baseYaw + Mth.clamp(delta, -MAX_MOUNTED_YAW_OFFSET, MAX_MOUNTED_YAW_OFFSET);
    }

    public static float clampMountedPitch(float pitch) {
        return Mth.clamp(pitch, MIN_MOUNTED_PITCH, MAX_MOUNTED_PITCH);
    }

    private Vec3 getBarrelPosition() {
        Direction facing = this.getFacing();
        return this.worldPosition.getCenter().add(
                facing.getStepX() * 0.8D,
                1.35D,
                facing.getStepZ() * 0.8D
        );
    }

    private Vec3 getRedstoneBarrelPosition() {
        Direction facing = this.getFacing();
        return this.worldPosition.getCenter().add(
                facing.getStepX() * 0.68D,
                0.82D,
                facing.getStepZ() * 0.68D
        );
    }

    public Vec3 getAutomatedTrailStartPosition() {
        Direction facing = this.getFacing();
        return this.worldPosition.getCenter().add(
                facing.getStepX() * 0.3D,
                0.42D,
                facing.getStepZ() * 0.3D
        );
    }

    private Vec3 getAttachmentPoint() {
        return this.worldPosition.getCenter();
    }

    private void message(@Nullable ServerPlayer player, Component message) {
        if (player != null && ModCommonConfig.SHOW_FAILURE_MESSAGES.get()) {
            player.displayClientMessage(message, true);
        }
    }

    private void playClick() {
        Level level = this.level;
        if (level != null) {
            level.playSound(null, this.worldPosition, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.4F, 1.0F);
        }
    }

    private void refundPendingAmmo() {
        if (!this.pendingAmmoConsumed || this.level == null) {
            return;
        }

        ItemStack remainder = this.inventory.insertItem(0, new ItemStack(ModItems.THROWABLE_ROPE_CONNECTOR.get()), false);
        if (!remainder.isEmpty()) {
            Containers.dropItemStack(this.level, this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D, this.worldPosition.getZ() + 0.5D, remainder);
        }
        this.pendingAmmoConsumed = false;
    }

    @Nullable
    private ServerPlayer findAutomationReleasePlayer(ServerLevel level) {
        return level.players().isEmpty() ? null : level.players().getFirst();
    }

    private boolean tryCreateRopeCompat(RopeStrandHolderBehavior localHolder, RopeStrandHolderBehavior remoteHolder) {
        try {
            Method directMethod = localHolder.getClass().getMethod("createRope", remoteHolder.getClass());
            Object result = directMethod.invoke(localHolder, remoteHolder);
            if (result instanceof Boolean booleanResult) {
                return booleanResult;
            }
            return localHolder.isAttached() || remoteHolder.isAttached();
        } catch (ReflectiveOperationException ignored) {
            try {
                Method twoArgMethod = localHolder.getClass().getMethod("createRope", remoteHolder.getClass(), boolean.class);
                Object result = twoArgMethod.invoke(localHolder, remoteHolder, false);
                if (result instanceof Boolean booleanResult) {
                    return booleanResult;
                }
                return localHolder.isAttached() || remoteHolder.isAttached();
            } catch (ReflectiveOperationException ignoredAgain) {
                for (Method method : localHolder.getClass().getMethods()) {
                    if (!method.getName().equals("createRope")) {
                        continue;
                    }

                    Class<?>[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes.length == 1 && parameterTypes[0].isInstance(remoteHolder)) {
                        try {
                            Object result = method.invoke(localHolder, remoteHolder);
                            if (result instanceof Boolean booleanResult) {
                                return booleanResult;
                            }
                            return localHolder.isAttached() || remoteHolder.isAttached();
                        } catch (ReflectiveOperationException reflectiveOperationException) {
                            CreateAeronauticsThrowableRopeConnector.LOGGER.error("Failed to invoke one-arg Simulated createRope compat path.", reflectiveOperationException);
                            return false;
                        }
                    }

                    if (parameterTypes.length == 2
                            && parameterTypes[0].isInstance(remoteHolder)
                            && parameterTypes[1] == boolean.class) {
                        try {
                            Object result = method.invoke(localHolder, remoteHolder, false);
                            if (result instanceof Boolean booleanResult) {
                                return booleanResult;
                            }
                            return localHolder.isAttached() || remoteHolder.isAttached();
                        } catch (ReflectiveOperationException reflectiveOperationException) {
                            CreateAeronauticsThrowableRopeConnector.LOGGER.error("Failed to invoke two-arg Simulated createRope compat path.", reflectiveOperationException);
                            return false;
                        }
                    }
                }

                CreateAeronauticsThrowableRopeConnector.LOGGER.error("Could not find a compatible Simulated createRope method for Mounted Rope Launcher.");
                return false;
            }
        } catch (Throwable throwable) {
            CreateAeronauticsThrowableRopeConnector.LOGGER.error("Mounted Rope Launcher failed to create rope link.", throwable);
            return false;
        }
    }

    private boolean tryDestroyRopeCompat(RopeStrandHolderBehavior localHolder, @Nullable ServerPlayer player, Vec3 attachmentPoint) {
        try {
            Method threeArgMethod = localHolder.getClass().getMethod("destroyRope", ServerPlayer.class, Vec3.class, boolean.class);
            threeArgMethod.invoke(localHolder, player, attachmentPoint, false);
            return !localHolder.isAttached();
        } catch (ReflectiveOperationException ignored) {
        } catch (Throwable throwable) {
            CreateAeronauticsThrowableRopeConnector.LOGGER.error("Mounted Rope Launcher failed to destroy rope link via three-arg Simulated API.", throwable);
            return false;
        }

        try {
            Method twoArgMethod = localHolder.getClass().getMethod("destroyRope", ServerPlayer.class, Vec3.class);
            twoArgMethod.invoke(localHolder, player, attachmentPoint);
            return !localHolder.isAttached();
        } catch (ReflectiveOperationException reflectiveOperationException) {
            CreateAeronauticsThrowableRopeConnector.LOGGER.error("Could not find a compatible Simulated destroyRope method for Mounted Rope Launcher.", reflectiveOperationException);
        } catch (Throwable throwable) {
            CreateAeronauticsThrowableRopeConnector.LOGGER.error("Mounted Rope Launcher failed to destroy rope link.", throwable);
        }
        return false;
    }
}
