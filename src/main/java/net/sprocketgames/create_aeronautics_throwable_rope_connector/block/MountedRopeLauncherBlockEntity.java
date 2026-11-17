package net.sprocketgames.create_aeronautics_throwable_rope_connector.block;

import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.ryanhcode.sable.Sable;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
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
import net.sprocketgames.create_aeronautics_throwable_rope_connector.integration.AnchorProtection;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.integration.CreateSimulatedIntegration;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModBlockEntityTypes;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModItems;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

public final class MountedRopeLauncherBlockEntity extends RopeWinchBlockEntity {
    private static final float MIN_MOUNTED_PITCH = -30.0F;
    private static final float MAX_MOUNTED_PITCH = 30.0F;
    private static final float MAX_MOUNTED_YAW_OFFSET = 80.0F;
    private static final float MOUNTED_FIRE_PITCH_OFFSET = 2.0F;
    private static final float SABLE_MOUNTED_FIRE_PITCH_OFFSET = 1.5F;
    private static final float SABLE_REDSTONE_FIRE_PITCH_OFFSET = 0.0F;
    private static final double HEAD_PIVOT_HEIGHT = 1.0D;
    private static final double MOUNTED_MUZZLE_DISTANCE = 13.5D / 16.0D;
    private static final double MOUNTED_TRAIL_START_DISTANCE = 3.5D / 16.0D;
    private static final Component NO_AMMO = Component.translatable("message.create_aeronautics_throwable_rope_connector.mounted_no_ammo");
    private static final Component ALREADY_CONNECTED = Component.translatable("message.create_aeronautics_throwable_rope_connector.mounted_already_connected");
    private static final Component BLOCKED = Component.translatable("message.create_aeronautics_throwable_rope_connector.mounted_blocked");
    private static final Component ANCHOR_CONNECTED = Component.translatable("message.create_aeronautics_throwable_rope_connector.mounted_anchor_connected");
    private static final Component ANCHOR_FAILED = Component.translatable("message.create_aeronautics_throwable_rope_connector.mounted_anchor_failed");
    private static final Component REMOVE_DENIED = Component.translatable("message.create_aeronautics_throwable_rope_connector.mounted_remove_denied");

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return CreateSimulatedIntegration.isLauncherAmmo(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            notifyUpdate();
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

    private float aimYaw;
    private float aimPitch;
    private long nextFireGameTime;
    private UUID pendingShot;
    private ItemStack paidAmmo = ItemStack.EMPTY;
    private long pendingShotExpires;
    private boolean fireRedstonePowered;
    private boolean releaseRedstonePowered;
    private boolean brokenCleanupDone;

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
        if (CreateSimulatedIntegration.isRopeCoupling(this.inventory.getStackInSlot(0))) {
            Vec3 localLook = this.worldDirectionToLocal(player.getLookAngle());
            float yaw = clampYawForFacing(this.getFacing(), yawFromDirection(localLook));
            float pitch = clampMountedPitch(pitchFromDirection(localLook));
            Vec3 direction = this.localDirectionToWorld(Vec3.directionFromRotation(pitch, yaw));
            Vec3 eye = Sable.HELPER.projectOutOfSubLevel(player.level(), player.getEyePosition());
            // Use the rider's sight line for precise coupling shots. The rope trail still starts at the barrel.
            this.fireInDirection(player, direction, eye, false);
            return;
        }

        float clampedPitch = clampMountedPitch(this.aimPitch);
        float clampedYaw = clampYawForFacing(this.getFacing(), this.aimYaw);
        Vec3 localDirection = this.getMountedLaunchDirection(clampedYaw, clampedPitch, true);
        Vec3 localLaunchPos = this.getMountedMuzzlePosition(localDirection);
        Vec3 launchPos = this.projectPositionOutOfSubLevel(localLaunchPos);
        Vec3 direction;
        if (this.getContainingSubLevelPose() != null) {
            direction = this.projectDirectionOutOfSubLevel(localDirection, localLaunchPos).normalize();
        } else {
            Vec3 target = this.getMountedReticleTarget(player);
            direction = target.subtract(launchPos).normalize();
            if (direction.lengthSqr() <= 1.0E-6D) {
                direction = this.projectDirectionOutOfSubLevel(localDirection, localLaunchPos).normalize();
            }
        }
        this.fireInDirection(player, direction, launchPos, false);
    }

    public void fireFromSavedAim() {
        float clampedPitch = clampMountedPitch(this.aimPitch);
        float clampedYaw = clampYawForFacing(this.getFacing(), this.aimYaw);
        Vec3 localDirection = this.getMountedLaunchDirection(clampedYaw, clampedPitch, false);
        Vec3 localLaunchPos = this.getMountedMuzzlePosition(localDirection);
        Vec3 launchPos = this.projectPositionOutOfSubLevel(localLaunchPos);
        Vec3 direction = this.projectDirectionOutOfSubLevel(localDirection, localLaunchPos).normalize();
        this.fireInDirection(null, direction, launchPos, false);
    }

    private void fireInDirection(@Nullable ServerPlayer player, Vec3 direction, Vec3 launchPos, boolean checkFixedFront) {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (serverLevel.getGameTime() < this.nextFireGameTime || this.pendingShot != null) {
            return;
        }

        if (this.isConnected()) {
            this.message(player, ALREADY_CONNECTED);
            return;
        }

        ItemStack ammo = this.inventory.getStackInSlot(0).copyWithCount(1);
        if (!CreateSimulatedIntegration.isLauncherAmmo(ammo)) {
            this.message(player, NO_AMMO);
            return;
        }

        BlockPos front = this.worldPosition.above().relative(this.getFacing());
        if (checkFixedFront && !serverLevel.getBlockState(front).getCollisionShape(serverLevel, front).isEmpty()) {
            this.message(player, BLOCKED);
            serverLevel.playSound(null, this.worldPosition, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.5F, 1.0F);
            return;
        }

        double range = ModCommonConfig.getClampedMountedLauncherRange();
        ThrowableRopeConnectorProjectile projectile = new ThrowableRopeConnectorProjectile(
                serverLevel,
                launchPos.x,
                launchPos.y,
                launchPos.z,
                range
        );
        projectile.setMountedSource(this.worldPosition, player != null);
        if (player != null) {
            projectile.setOwner(player);
        }
        projectile.setTrailFromMountedLauncher(true);
        projectile.setItem(ammo);
        projectile.setNoGravity(CreateSimulatedIntegration.isRopeCoupling(ammo));
        projectile.setDeltaMovement(direction.normalize().scale(3.0D));
        if (!serverLevel.addFreshEntity(projectile)) {
            return;
        }
        this.pendingShot = projectile.getUUID();
        this.pendingShotExpires = serverLevel.getGameTime() + Math.max(100L, Math.round(range * 3.0D));
        if (!ModCommonConfig.CONSUME_ON_SUCCESS_ONLY.get()) {
            this.paidAmmo = this.inventory.extractItem(0, 1, false);
        }
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
        this.setAimFromWorldDirection(player.getLookAngle());
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

    public void setAimFromDirection(Vec3 localDirection) {
        float clampedYaw = clampYawForFacing(this.getFacing(), yawFromDirection(localDirection));
        float clampedPitch = clampMountedPitch(pitchFromDirection(localDirection));
        if (Math.abs(this.aimYaw - clampedYaw) < 0.5F && Math.abs(this.aimPitch - clampedPitch) < 0.5F) {
            return;
        }

        this.aimYaw = clampedYaw;
        this.aimPitch = clampedPitch;
        this.notifyUpdate();
    }

    public void setAimFromWorldDirection(Vec3 worldDirection) {
        this.setAimFromDirection(this.worldDirectionToLocal(worldDirection));
    }

    public float getAimYaw() {
        return this.aimYaw;
    }

    public float getAimPitch() {
        return this.aimPitch;
    }

    public boolean canFinishShot(UUID shot, ItemStack ammo) {
        if (!shot.equals(this.pendingShot)) {
            return false;
        }
        ItemStack available = this.paidAmmo.isEmpty() ? this.inventory.getStackInSlot(0) : this.paidAmmo;
        return ItemStack.isSameItemSameComponents(available, ammo);
    }

    public boolean connectToConnector(ServerLevel level, BlockPos connectorPos, UUID shot, ItemStack ammo, @Nullable ServerPlayer player) {
        RopeStrandHolderBehavior localHolder = this.getBehavior();
        RopeStrandHolderBehavior remoteHolder = RopeItem.getRopeHolder(level, connectorPos);
        if (!this.canFinishShot(shot, ammo) || remoteHolder == null || remoteHolder.isAttached()
                || localHolder == null || localHolder.isAttached()) {
            this.message(player, ANCHOR_FAILED);
            return false;
        }

        if (!this.tryCreateRopeCompat(localHolder, remoteHolder)) {
            this.message(player, ANCHOR_FAILED);
            return false;
        }

        if (this.paidAmmo.isEmpty()) {
            this.inventory.extractItem(0, 1, false);
        }
        this.paidAmmo = ItemStack.EMPTY;
        this.pendingShot = null;
        this.message(player, ANCHOR_CONNECTED);
        this.notifyUpdate();
        return true;
    }

    public void onProjectileFailed(UUID shot) {
        if (shot.equals(this.pendingShot)) {
            this.refundPendingAmmo();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level instanceof ServerLevel level
                && (this.pendingShot != null || !this.paidAmmo.isEmpty())
                && level.getGameTime() >= this.pendingShotExpires) {
            this.refundPendingAmmo();
        }
    }

    public boolean disconnectRope() {
        if (!(this.level instanceof ServerLevel level) || !this.isConnected()) {
            return false;
        }
        RopeStrandHolderBehavior holder = this.getBehavior();
        if (!holder.ownsRope()) {
            BlockPos destination = this.getRopeDestination();
            if (destination == null || !level.hasChunkAt(destination)) {
                return false;
            }
            holder = RopeItem.getRopeHolder(level, destination);
            if (holder == null || !holder.ownsRope()) {
                return false;
            }
        }
        holder.destroyRope(null, this.getAttachmentPoint(), false);
        this.notifyUpdate();
        return !this.isConnected();
    }

    public void removeAnchor(ServerPlayer player) {
        if (!(this.level instanceof ServerLevel level)) {
            return;
        }
        BlockPos destination = this.getRopeDestination();
        if (destination == null) {
            return;
        }
        if (!AnchorProtection.canRemove(level, destination, player)) {
            this.message(player, REMOVE_DENIED);
            return;
        }
        if (this.disconnectRope() && CreateSimulatedIntegration.canUseDockConnector(level, destination)) {
            level.removeBlock(destination, false);
        }
    }

    @Nullable
    private BlockPos getRopeDestination() {
        RopeStrandHolderBehavior holder = this.getBehavior();
        var strand = holder == null ? null : holder.getAttachedStrand();
        if (strand != null) {
            for (var attachment : strand.getAttachments()) {
                BlockPos pos = attachment.blockAttachment();
                if (!pos.equals(this.worldPosition)) {
                    return pos;
                }
            }
        }
        return null;
    }

    public void onBroken() {
        if (this.brokenCleanupDone) {
            return;
        }
        this.brokenCleanupDone = true;
        this.disconnectRope();
        this.refundPendingAmmo();
        if (this.level != null) {
            ItemStack ammo = this.inventory.extractItem(0, this.inventory.getSlotLimit(0), false);
            if (!ammo.isEmpty()) {
                Containers.dropItemStack(this.level, this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D, this.worldPosition.getZ() + 0.5D, ammo);
            }
        }
        this.setChanged();
    }

    public boolean isConnected() {
        RopeStrandHolderBehavior localHolder = this.getBehavior();
        return localHolder != null && localHolder.isAttached();
    }

    public int getAmmoCount() {
        return this.inventory.getStackInSlot(0).getCount();
    }

    public ItemStack getLoadedAmmo() {
        return this.inventory.getStackInSlot(0).copy();
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
        boolean fireRisingEdge = firePowered && !this.fireRedstonePowered;
        boolean releaseRisingEdge = releasePowered && !this.releaseRedstonePowered;
        if (this.fireRedstonePowered != firePowered || this.releaseRedstonePowered != releasePowered) {
            this.setChanged();
        }
        this.fireRedstonePowered = firePowered;
        this.releaseRedstonePowered = releasePowered;

        if (fireRisingEdge && ModCommonConfig.canMountedLauncherFireFromRedstone()) {
            this.fireFromSavedAim();
        }

        if (releaseRisingEdge) {
            this.disconnectRope();
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Inventory", this.inventory.serializeNBT(registries));
        tag.putFloat("AimYaw", this.aimYaw);
        tag.putFloat("AimPitch", this.aimPitch);
        tag.putBoolean("FireRedstonePowered", this.fireRedstonePowered);
        tag.putBoolean("ReleaseRedstonePowered", this.releaseRedstonePowered);
        if (this.pendingShot != null) {
            tag.putUUID("PendingShot", this.pendingShot);
            tag.putLong("PendingShotExpires", this.pendingShotExpires);
        }
        if (!this.paidAmmo.isEmpty()) {
            tag.put("PaidAmmo", this.paidAmmo.save(registries));
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        this.inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        this.aimYaw = tag.getFloat("AimYaw");
        this.aimPitch = tag.getFloat("AimPitch");
        this.pendingShot = tag.hasUUID("PendingShot") ? tag.getUUID("PendingShot") : null;
        this.pendingShotExpires = tag.getLong("PendingShotExpires");
        this.paidAmmo = ItemStack.parseOptional(registries, tag.getCompound("PaidAmmo"));
        if (this.paidAmmo.isEmpty() && tag.getBoolean("PendingAmmoConsumed")) {
            this.paidAmmo = new ItemStack(ModItems.THROWABLE_ROPE_CONNECTOR.get());
        }
        this.fireRedstonePowered = tag.getBoolean("FireRedstonePowered");
        this.releaseRedstonePowered = tag.getBoolean("ReleaseRedstonePowered");
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
        return this.getBaseAttachmentPoint().add(
                facing.getStepX() * 0.8D,
                0.85D,
                facing.getStepZ() * 0.8D
        );
    }

    private Vec3 getRedstoneBarrelPosition() {
        Direction facing = this.getFacing();
        return this.getBaseAttachmentPoint().add(
                facing.getStepX() * 0.68D,
                0.58D,
                facing.getStepZ() * 0.68D
        );
    }

    public Vec3 getAutomatedTrailStartPosition() {
        float clampedPitch = clampMountedPitch(this.aimPitch);
        float clampedYaw = clampYawForFacing(this.getFacing(), this.aimYaw);
        Vec3 localDirection = this.getMountedLaunchDirection(clampedYaw, clampedPitch, false);
        Vec3 localTrailStart = this.getMountedTrailStartPosition(localDirection);
        return this.projectPositionOutOfSubLevel(localTrailStart);
    }

    private Vec3 getAttachmentPoint() {
        RopeStrandHolderBehavior localHolder = this.getBehavior();
        if (localHolder != null) {
            return localHolder.getAttachmentPoint();
        }
        return this.worldPosition.getCenter();
    }

    private Vec3 getBaseAttachmentPoint() {
        RopeStrandHolderBehavior localHolder = this.getBehavior();
        if (localHolder != null) {
            return localHolder.getVisualAttachmentPoint();
        }
        return this.worldPosition.getCenter();
    }

    private Vec3 getMountedHeadPivotPosition() {
        return Vec3.atBottomCenterOf(this.worldPosition).add(0.0D, HEAD_PIVOT_HEIGHT, 0.0D);
    }

    private Vec3 getMountedMuzzlePosition(Vec3 localDirection) {
        return this.getMountedHeadPivotPosition().add(localDirection.scale(MOUNTED_MUZZLE_DISTANCE));
    }

    private Vec3 getMountedTrailStartPosition(Vec3 localDirection) {
        return this.getMountedHeadPivotPosition().add(localDirection.scale(MOUNTED_TRAIL_START_DISTANCE));
    }

    private Vec3 getMountedReticleTarget(ServerPlayer player) {
        double range = ModCommonConfig.getClampedMountedLauncherRange();
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookDirection = player.getLookAngle().normalize();
        Vec3 endPos = eyePos.add(lookDirection.scale(range));
        Level raycastLevel = player.level();
        ClipContext clipContext = new ClipContext(eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        BlockHitResult blockHitResult = raycastLevel.clip(clipContext);
        if (blockHitResult.getType() != HitResult.Type.MISS) {
            return Sable.HELPER.projectOutOfSubLevel(raycastLevel, blockHitResult.getLocation());
        }
        return endPos;
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
        this.pendingShot = null;
        if (this.level == null) {
            return;
        }
        ItemStack ammo = this.paidAmmo;
        this.paidAmmo = ItemStack.EMPTY;
        ItemStack remainder = this.inventory.insertItem(0, ammo, false);
        if (!remainder.isEmpty()) {
            Containers.dropItemStack(this.level, this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D, this.worldPosition.getZ() + 0.5D, remainder);
        }
        this.setChanged();
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

    private static float yawFromDirection(Vec3 direction) {
        return (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
    }

    private static float pitchFromDirection(Vec3 direction) {
        double horizontalLength = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        return (float) Math.toDegrees(Math.atan2(-direction.y, horizontalLength));
    }

    private Vec3 getMountedLaunchDirection(float yaw, float pitch, boolean mountedManualFire) {
        float pitchOffset = MOUNTED_FIRE_PITCH_OFFSET;
        if (this.getContainingSubLevelPose() != null) {
            pitchOffset += mountedManualFire ? SABLE_MOUNTED_FIRE_PITCH_OFFSET : SABLE_REDSTONE_FIRE_PITCH_OFFSET;
        }
        return Vec3.directionFromRotation(pitch - pitchOffset, yaw).normalize();
    }

    private Vec3 projectPositionOutOfSubLevel(Vec3 localPosition) {
        Level currentLevel = this.level;
        if (currentLevel == null) {
            return localPosition;
        }

        try {
            Class<?> sableClass = Class.forName("dev.ryanhcode.sable.Sable");
            Field helperField = sableClass.getField("HELPER");
            Object helper = helperField.get(null);
            Method projectOutMethod = helper.getClass().getMethod("projectOutOfSubLevel", Level.class, Vec3.class);
            Object projected = projectOutMethod.invoke(helper, currentLevel, localPosition);
            if (projected instanceof Vec3 vec3) {
                return vec3;
            }
        } catch (ReflectiveOperationException ignored) {
        } catch (Throwable throwable) {
            CreateAeronauticsThrowableRopeConnector.LOGGER.debug("Failed to project Mounted Rope Launcher position out of sublevel.", throwable);
        }

        return localPosition;
    }

    public Vec3 projectLocalPositionToWorld(Vec3 localPosition) {
        return this.projectPositionOutOfSubLevel(localPosition);
    }

    private Vec3 projectDirectionOutOfSubLevel(Vec3 localDirection, Vec3 localOrigin) {
        Object pose = this.getContainingSubLevelPose();
        if (pose != null) {
            try {
                Method transformNormal = pose.getClass().getMethod("transformNormal", Vec3.class);
                Object worldDirection = transformNormal.invoke(pose, localDirection);
                if (worldDirection instanceof Vec3 vec3) {
                    return vec3.lengthSqr() > 1.0E-6D ? vec3.normalize() : localDirection;
                }
            } catch (ReflectiveOperationException ignored) {
            } catch (Throwable throwable) {
                CreateAeronauticsThrowableRopeConnector.LOGGER.debug("Failed to project Mounted Rope Launcher direction out of sublevel.", throwable);
            }
        }

        Vec3 worldOrigin = this.projectPositionOutOfSubLevel(localOrigin);
        Vec3 worldAhead = this.projectPositionOutOfSubLevel(localOrigin.add(localDirection));
        Vec3 projectedDirection = worldAhead.subtract(worldOrigin);
        return projectedDirection.lengthSqr() > 1.0E-6D ? projectedDirection.normalize() : localDirection;
    }

    public Vec3 worldDirectionToLocal(Vec3 worldDirection) {
        Object pose = this.getContainingSubLevelPose();
        if (pose != null) {
            try {
                Method transformNormal = pose.getClass().getMethod("transformNormal", Vec3.class);
                Vec3 localXWorld = (Vec3) transformNormal.invoke(pose, new Vec3(1.0D, 0.0D, 0.0D));
                Vec3 localYWorld = (Vec3) transformNormal.invoke(pose, new Vec3(0.0D, 1.0D, 0.0D));
                Vec3 localZWorld = (Vec3) transformNormal.invoke(pose, new Vec3(0.0D, 0.0D, 1.0D));
                Vec3 normalizedWorldDirection = worldDirection.normalize();
                Vec3 localDirection = new Vec3(
                        normalizedWorldDirection.dot(localXWorld),
                        normalizedWorldDirection.dot(localYWorld),
                        normalizedWorldDirection.dot(localZWorld)
                );
                if (localDirection.lengthSqr() > 1.0E-6D) {
                    return localDirection.normalize();
                }
            } catch (ReflectiveOperationException ignored) {
            } catch (Throwable throwable) {
                CreateAeronauticsThrowableRopeConnector.LOGGER.debug("Failed to convert world direction into Mounted Rope Launcher local direction.", throwable);
            }
        }

        return worldDirection.normalize();
    }

    public Vec3 localDirectionToWorld(Vec3 localDirection) {
        return this.projectDirectionOutOfSubLevel(localDirection, this.getMountedHeadPivotPosition()).normalize();
    }


    @Nullable
    private Object getContainingSubLevelPose() {
        Level currentLevel = this.level;
        if (currentLevel == null) {
            return null;
        }

        try {
            Class<?> sableClass = Class.forName("dev.ryanhcode.sable.Sable");
            Field helperField = sableClass.getField("HELPER");
            Object helper = helperField.get(null);
            Method getContaining = helper.getClass().getMethod("getContaining", net.minecraft.world.level.block.entity.BlockEntity.class);
            Object subLevel = getContaining.invoke(helper, this);
            if (subLevel == null) {
                return null;
            }

            Method logicalPose = subLevel.getClass().getMethod("logicalPose");
            return logicalPose.invoke(subLevel);
        } catch (ReflectiveOperationException ignored) {
        } catch (Throwable throwable) {
            CreateAeronauticsThrowableRopeConnector.LOGGER.debug("Failed to resolve Mounted Rope Launcher sublevel pose.", throwable);
        }

        return null;
    }
}
