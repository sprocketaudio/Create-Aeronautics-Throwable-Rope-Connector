package net.sprocketgames.create_aeronautics_throwable_rope_connector.entity;

import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.config.ModCommonConfig;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.block.MountedRopeLauncherBlockEntity;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.integration.CreateSimulatedIntegration;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.item.ThrowableRopeConnectorPlacement;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModEntityTypes;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModItems;
import org.jetbrains.annotations.Nullable;

public final class ThrowableRopeConnectorProjectile extends ThrowableItemProjectile {
    private static final Component TARGET_TOO_FAR = Component.translatable("message.create_aeronautics_throwable_rope_connector.target_too_far");
    private static final Component DOCK_UNAVAILABLE = Component.translatable("message.create_aeronautics_throwable_rope_connector.dock_unavailable");
    private static final Component DOCK_CONNECTED = Component.translatable("message.create_aeronautics_throwable_rope_connector.dock_connected");
    private static final Component DOCK_PREPARED = Component.translatable("message.create_aeronautics_throwable_rope_connector.dock_prepared");
    private static final Component DOCK_MISSED = Component.translatable("message.create_aeronautics_throwable_rope_connector.dock_missed");
    private static final EntityDataAccessor<Boolean> DATA_TRAIL_FROM_OFFHAND = SynchedEntityData.defineId(
            ThrowableRopeConnectorProjectile.class,
            EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Boolean> DATA_TRAIL_FROM_LAUNCHER = SynchedEntityData.defineId(
            ThrowableRopeConnectorProjectile.class,
            EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Boolean> DATA_TRAIL_FROM_MOUNTED_LAUNCHER = SynchedEntityData.defineId(
            ThrowableRopeConnectorProjectile.class,
            EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Boolean> DATA_HAS_MOUNTED_SOURCE = SynchedEntityData.defineId(
            ThrowableRopeConnectorProjectile.class,
            EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<BlockPos> DATA_MOUNTED_SOURCE_POS = SynchedEntityData.defineId(
            ThrowableRopeConnectorProjectile.class,
            EntityDataSerializers.BLOCK_POS
    );

    private Vec3 origin = Vec3.ZERO;
    private double maxThrowDistance = 10.0D;
    private int sourceSlot = -1;
    private boolean consumeOnSuccessOnly = true;
    private boolean ammoPaid;
    private BlockPos mountedSourcePos;
    private boolean mountedPlayerShot;

    public ThrowableRopeConnectorProjectile(EntityType<? extends ThrowableRopeConnectorProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public ThrowableRopeConnectorProjectile(Level level, LivingEntity shooter, double maxThrowDistance) {
        super(ModEntityTypes.THROWABLE_ROPE_CONNECTOR.get(), shooter, level);
        this.origin = this.position();
        this.maxThrowDistance = maxThrowDistance;
    }

    public ThrowableRopeConnectorProjectile(Level level, double x, double y, double z, double maxThrowDistance) {
        super(ModEntityTypes.THROWABLE_ROPE_CONNECTOR.get(), x, y, z, level);
        this.origin = this.position();
        this.maxThrowDistance = maxThrowDistance;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TRAIL_FROM_OFFHAND, false);
        builder.define(DATA_TRAIL_FROM_LAUNCHER, false);
        builder.define(DATA_TRAIL_FROM_MOUNTED_LAUNCHER, false);
        builder.define(DATA_HAS_MOUNTED_SOURCE, false);
        builder.define(DATA_MOUNTED_SOURCE_POS, BlockPos.ZERO);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.THROWABLE_ROPE_CONNECTOR.get();
    }

    public void setSourceSlot(int sourceSlot) {
        this.sourceSlot = sourceSlot;
    }

    public void setTrailFromOffhand(boolean trailFromOffhand) {
        this.entityData.set(DATA_TRAIL_FROM_OFFHAND, trailFromOffhand);
    }

    public boolean isTrailFromOffhand() {
        return this.entityData.get(DATA_TRAIL_FROM_OFFHAND);
    }

    public void setTrailFromLauncher(boolean trailFromLauncher) {
        this.entityData.set(DATA_TRAIL_FROM_LAUNCHER, trailFromLauncher);
    }

    public boolean isTrailFromLauncher() {
        return this.entityData.get(DATA_TRAIL_FROM_LAUNCHER);
    }

    public void setTrailFromMountedLauncher(boolean trailFromMountedLauncher) {
        this.entityData.set(DATA_TRAIL_FROM_MOUNTED_LAUNCHER, trailFromMountedLauncher);
    }

    public boolean isTrailFromMountedLauncher() {
        return this.entityData.get(DATA_TRAIL_FROM_MOUNTED_LAUNCHER);
    }

    public void setConsumeOnSuccessOnly(boolean consumeOnSuccessOnly) {
        this.consumeOnSuccessOnly = consumeOnSuccessOnly;
    }

    public void setAmmoPaid(boolean ammoPaid) {
        this.ammoPaid = ammoPaid;
    }

    public void setMountedSource(BlockPos mountedSourcePos, boolean playerShot) {
        this.mountedSourcePos = mountedSourcePos.immutable();
        this.mountedPlayerShot = playerShot;
        this.entityData.set(DATA_HAS_MOUNTED_SOURCE, true);
        this.entityData.set(DATA_MOUNTED_SOURCE_POS, this.mountedSourcePos);
    }

    public BlockPos getMountedSourcePos() {
        if (this.entityData.get(DATA_HAS_MOUNTED_SOURCE)) {
            return this.entityData.get(DATA_MOUNTED_SOURCE_POS);
        }
        return this.mountedSourcePos;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || this.isRemoved()) {
            return;
        }
        if (this.position().distanceToSqr(this.origin) > this.maxThrowDistance * this.maxThrowDistance) {
            this.failAndReturn(TARGET_TOO_FAR);
        } else if (this.tickCount >= 100 && this.getDeltaMovement().lengthSqr() < 0.0025D
                && CreateSimulatedIntegration.isRopeCoupling(this.getItem())) {
            this.failAndReturn(DOCK_MISSED);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (Sable.HELPER.distanceSquaredWithSubLevels(serverLevel, this.origin, result.getLocation())
                > this.maxThrowDistance * this.maxThrowDistance) {
            this.failAndReturn(TARGET_TOO_FAR);
            return;
        }

        if (CreateSimulatedIntegration.isRopeCoupling(this.getItem())) {
            this.handleDockHit(serverLevel, result.getBlockPos());
            return;
        }

        if (this.mountedSourcePos != null) {
            this.handleMountedBlockHit(serverLevel, result);
            return;
        }

        Entity owner = this.getOwner();
        if (!(owner instanceof ServerPlayer player)) {
            this.failAndReturn(ThrowableRopeConnectorPlacement.CANNOT_ATTACH_THERE);
            return;
        }

        boolean placed = ThrowableRopeConnectorPlacement.placeConnectorAndGiveRope(serverLevel, player, result, this.sourceSlot, this.consumeOnSuccessOnly);
        if (placed) {
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        } else {
            this.failAndReturn(ThrowableRopeConnectorPlacement.CANNOT_ATTACH_THERE);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide) {
            this.failAndReturn(CreateSimulatedIntegration.isRopeCoupling(this.getItem()) ? null : ThrowableRopeConnectorPlacement.CANNOT_ATTACH_THERE);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        if (result.getType() == HitResult.Type.MISS) {
            return;
        }

        super.onHit(result);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            ParticleOptions particle = new ItemParticleOption(ParticleTypes.ITEM, this.getItem());
            for (int i = 0; i < 8; i++) {
                this.level().addParticle(particle, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putDouble("OriginX", this.origin.x);
        compound.putDouble("OriginY", this.origin.y);
        compound.putDouble("OriginZ", this.origin.z);
        compound.putDouble("MaxThrowDistance", this.maxThrowDistance);
        compound.putInt("SourceSlot", this.sourceSlot);
        compound.putBoolean("ConsumeOnSuccessOnly", this.consumeOnSuccessOnly);
        compound.putBoolean("AmmoPaid", this.ammoPaid);
        compound.putBoolean("TrailFromOffhand", this.isTrailFromOffhand());
        compound.putBoolean("TrailFromLauncher", this.isTrailFromLauncher());
        compound.putBoolean("TrailFromMountedLauncher", this.isTrailFromMountedLauncher());
        BlockPos mountedSourcePos = this.getMountedSourcePos();
        if (mountedSourcePos != null) {
            compound.putLong("MountedSourcePos", mountedSourcePos.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.origin = new Vec3(compound.getDouble("OriginX"), compound.getDouble("OriginY"), compound.getDouble("OriginZ"));
        this.maxThrowDistance = compound.contains("MaxThrowDistance") ? compound.getDouble("MaxThrowDistance") : ModCommonConfig.getClampedThrowDistance();
        this.sourceSlot = compound.contains("SourceSlot") ? compound.getInt("SourceSlot") : -1;
        this.consumeOnSuccessOnly = !compound.contains("ConsumeOnSuccessOnly") || compound.getBoolean("ConsumeOnSuccessOnly");
        this.ammoPaid = compound.getBoolean("AmmoPaid");
        this.setTrailFromOffhand(compound.getBoolean("TrailFromOffhand"));
        this.setTrailFromLauncher(compound.getBoolean("TrailFromLauncher"));
        this.setTrailFromMountedLauncher(compound.getBoolean("TrailFromMountedLauncher"));
        if (compound.contains("MountedSourcePos")) {
            this.setMountedSource(BlockPos.of(compound.getLong("MountedSourcePos")), compound.hasUUID("Owner"));
        } else {
            this.mountedSourcePos = null;
            this.entityData.set(DATA_HAS_MOUNTED_SOURCE, false);
            this.entityData.set(DATA_MOUNTED_SOURCE_POS, BlockPos.ZERO);
        }
    }

    private void failAndReturn(@Nullable Component message) {
        if (this.level().isClientSide) {
            return;
        }

        if (this.mountedSourcePos != null) {
            if (this.level().getBlockEntity(this.mountedSourcePos) instanceof MountedRopeLauncherBlockEntity mountedLauncher) {
                mountedLauncher.onProjectileFailed(this.getUUID());
            }
            if (this.getOwner() instanceof Player player && CreateSimulatedIntegration.isRopeCoupling(this.getItem())) {
                ThrowableRopeConnectorPlacement.fail(player, message);
            }
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
            return;
        }

        Entity owner = this.getOwner();
        boolean coupling = CreateSimulatedIntegration.isRopeCoupling(this.getItem());
        if (owner instanceof Player player) {
            if (coupling && this.ammoPaid) {
                ThrowableRopeConnectorPlacement.returnLauncherAmmo(player, this.getItem().copy(), this.sourceSlot);
            } else if (!coupling && !this.consumeOnSuccessOnly) {
                ThrowableRopeConnectorPlacement.returnThrowableItem(player, this.sourceSlot);
            }
            ThrowableRopeConnectorPlacement.fail(player, message);
        } else {
            if (coupling ? this.ammoPaid : !this.consumeOnSuccessOnly) {
                this.spawnAtLocation(this.getItem().copy());
            }
            this.level().playSound(null, BlockPos.containing(this.position()), SoundEvents.DISPENSER_FAIL, SoundSource.NEUTRAL, 0.5F, 1.0F);
        }

        this.level().broadcastEntityEvent(this, (byte) 3);
        this.discard();
    }

    private void handleMountedBlockHit(ServerLevel serverLevel, BlockHitResult result) {
        if (!(serverLevel.getBlockEntity(this.mountedSourcePos) instanceof MountedRopeLauncherBlockEntity mountedLauncher)
                || !mountedLauncher.canFinishShot(this.getUUID(), this.getItem())) {
            this.failAndReturn(ThrowableRopeConnectorPlacement.CANNOT_ATTACH_THERE);
            return;
        }

        ServerPlayer player = this.getOwner() instanceof ServerPlayer shooter ? shooter : null;
        if (this.mountedPlayerShot && (player == null || player.level() != serverLevel)) {
            this.failAndReturn(ThrowableRopeConnectorPlacement.CANNOT_ATTACH_THERE);
            return;
        }
        java.util.Optional<BlockPos> placedConnector = ThrowableRopeConnectorPlacement.placeConnectorFromMountedLauncher(serverLevel, result, player);
        if (placedConnector.isEmpty() || !mountedLauncher.connectToConnector(serverLevel, placedConnector.get(), this.getUUID(), this.getItem(), null)) {
            placedConnector.ifPresent(pos -> serverLevel.removeBlock(pos, false));
            this.failAndReturn(ThrowableRopeConnectorPlacement.CANNOT_ATTACH_THERE);
            return;
        }

        this.level().broadcastEntityEvent(this, (byte) 3);
        this.discard();
    }

    private void handleDockHit(ServerLevel level, BlockPos destination) {
        if (!CreateSimulatedIntegration.isRopeConnector(level, destination)) {
            this.failAndReturn(null);
            return;
        }
        if (!CreateSimulatedIntegration.canUseDockConnector(level, destination)) {
            this.failAndReturn(DOCK_UNAVAILABLE);
            return;
        }

        ServerPlayer player = this.getOwner() instanceof ServerPlayer shooter ? shooter : null;
        BlockPos worldPos = BlockPos.containing(Sable.HELPER.projectOutOfSubLevel(level, Vec3.atCenterOf(destination)));
        if (player != null && (player.level() != level || !level.mayInteract(player, worldPos))) {
            this.failAndReturn(DOCK_UNAVAILABLE);
            return;
        }
        boolean mounted = this.mountedSourcePos != null;
        boolean success;
        if (mounted) {
            success = level.getBlockEntity(this.mountedSourcePos) instanceof MountedRopeLauncherBlockEntity launcher
                    && launcher.connectToConnector(level, destination, this.getUUID(), this.getItem(), null);
        } else {
            success = player != null && ThrowableRopeConnectorPlacement.prepareDockCoupling(
                    player, destination, this.getItem(), this.sourceSlot, this.ammoPaid);
        }
        if (!success) {
            this.failAndReturn(DOCK_UNAVAILABLE);
            return;
        }
        if (player != null && ModCommonConfig.SHOW_FAILURE_MESSAGES.get()) {
            player.displayClientMessage(mounted ? DOCK_CONNECTED : DOCK_PREPARED, true);
        }
        level.playSound(null, BlockPos.containing(this.position()), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.7F, 1.2F);
        this.discard();
    }
}
