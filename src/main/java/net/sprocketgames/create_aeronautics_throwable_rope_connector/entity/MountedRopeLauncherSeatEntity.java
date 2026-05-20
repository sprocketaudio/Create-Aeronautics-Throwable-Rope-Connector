package net.sprocketgames.create_aeronautics_throwable_rope_connector.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.block.MountedRopeLauncherBlock;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.block.MountedRopeLauncherBlockEntity;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModEntityTypes;

public final class MountedRopeLauncherSeatEntity extends Entity implements IEntityWithComplexSpawn {
    private BlockPos launcherPos = BlockPos.ZERO;

    public MountedRopeLauncherSeatEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public MountedRopeLauncherSeatEntity(Level level, BlockPos launcherPos) {
        this(ModEntityTypes.MOUNTED_ROPE_LAUNCHER_SEAT.get(), level);
        this.setLauncherPos(launcherPos);
    }

    public BlockPos getLauncherPos() {
        return this.launcherPos;
    }

    public void setLauncherPos(BlockPos launcherPos) {
        this.launcherPos = launcherPos.immutable();
        Vec3 seatPos = this.getSeatPosition();
        this.setPos(seatPos.x, seatPos.y, seatPos.z);
    }

    public void fire(ServerPlayer player) {
        if (this.level().getBlockEntity(this.launcherPos) instanceof MountedRopeLauncherBlockEntity launcher) {
            launcher.fireFromRider(player);
        }
    }

    public void release(ServerPlayer player) {
        if (this.level().getBlockEntity(this.launcherPos) instanceof MountedRopeLauncherBlockEntity launcher) {
            launcher.release(player);
        }
    }

    @Override
    public void setPos(double x, double y, double z) {
        super.setPos(x, y, z);
        AABB bounds = this.getBoundingBox();
        Vec3 diff = new Vec3(x, y, z).subtract(bounds.getCenter());
        this.setBoundingBox(bounds.move(diff));
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
        if (!this.hasPassenger(passenger)) {
            return;
        }

        double heightOffset = this.getPassengerRidingPosition(passenger).y - passenger.getVehicleAttachmentPoint(this).y;
        callback.accept(passenger, this.getX(), 1.0D / 16.0D + heightOffset, this.getZ());
    }

    @Override
    public void onPassengerTurned(Entity entity) {
        entity.setYHeadRot(entity.getYRot());
        if (!this.level().isClientSide() && entity instanceof ServerPlayer player
                && this.level().getBlockEntity(this.launcherPos) instanceof MountedRopeLauncherBlockEntity launcher) {
            launcher.setAim(player.getYRot(), player.getXRot());
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoGravity(true);

        Vec3 seatPos = this.getSeatPosition();
        this.setPos(seatPos.x, seatPos.y, seatPos.z);

        if (this.level().isClientSide()) {
            return;
        }

        if (!this.isValidLauncher() || !this.isVehicle()) {
            this.discard();
            return;
        }

        Entity passenger = this.getFirstPassenger();
        if (passenger != null) {
            this.clampPassengerRotation(passenger);
        }

        if (passenger instanceof ServerPlayer player) {
            this.fireAimUpdate(player);
        }
    }

    private void fireAimUpdate(ServerPlayer player) {
        if (this.level().getBlockEntity(this.launcherPos) instanceof MountedRopeLauncherBlockEntity launcher) {
            launcher.setAim(player.getYRot(), player.getXRot());
        }
    }

    private void clampPassengerRotation(Entity passenger) {
        if (!(passenger instanceof LivingEntity livingPassenger)) {
            return;
        }

        BlockState state = this.level().getBlockState(this.launcherPos);
        if (!(state.getBlock() instanceof MountedRopeLauncherBlock)) {
            return;
        }

        float clampedYaw = MountedRopeLauncherBlockEntity.clampYawForFacing(
                state.getValue(MountedRopeLauncherBlock.FACING),
                passenger.getYRot()
        );
        float clampedPitch = MountedRopeLauncherBlockEntity.clampMountedPitch(passenger.getXRot());
        boolean yawChanged = Math.abs(net.minecraft.util.Mth.wrapDegrees(passenger.getYRot() - clampedYaw)) >= 0.1F;
        boolean pitchChanged = Math.abs(passenger.getXRot() - clampedPitch) >= 0.1F;

        if (!yawChanged && !pitchChanged) {
            return;
        }

        passenger.setYRot(clampedYaw);
        passenger.setYHeadRot(clampedYaw);
        passenger.setXRot(clampedPitch);
        passenger.yRotO = clampedYaw;
        passenger.xRotO = clampedPitch;
        livingPassenger.setYBodyRot(clampedYaw);
        livingPassenger.yHeadRotO = clampedYaw;
        livingPassenger.yBodyRotO = clampedYaw;
    }

    private boolean isValidLauncher() {
        BlockState state = this.level().getBlockState(this.launcherPos);
        return state.getBlock() instanceof MountedRopeLauncherBlock
                && state.getValue(MountedRopeLauncherBlock.HALF) == DoubleBlockHalf.LOWER;
    }

    private Vec3 getSeatPosition() {
        return Vec3.atBottomCenterOf(this.launcherPos).add(0.0D, 0.4D, 0.0D);
    }

    @Override
    protected boolean canRide(Entity entity) {
        return !(entity instanceof FakePlayer);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Direction facing = Direction.NORTH;
        BlockState state = this.level().getBlockState(this.launcherPos);
        if (state.getBlock() instanceof MountedRopeLauncherBlock) {
            facing = state.getValue(MountedRopeLauncherBlock.FACING);
        }

        Vec3 fallback = super.getDismountLocationForPassenger(passenger);
        return fallback.add(
                -facing.getStepX() * 0.85D,
                0.15D,
                -facing.getStepZ() * 0.85D
        );
    }

    @Override
    public void setDeltaMovement(Vec3 movement) {
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.launcherPos = tag.contains("LauncherPos") ? BlockPos.of(tag.getLong("LauncherPos")) : BlockPos.ZERO;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putLong("LauncherPos", this.launcherPos.asLong());
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.launcherPos);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        this.setLauncherPos(buffer.readBlockPos());
    }
}
