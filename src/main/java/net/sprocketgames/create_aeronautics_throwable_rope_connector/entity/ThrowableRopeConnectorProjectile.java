package net.sprocketgames.create_aeronautics_throwable_rope_connector.entity;

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
import net.sprocketgames.create_aeronautics_throwable_rope_connector.item.ThrowableRopeConnectorPlacement;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModEntityTypes;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModItems;

public final class ThrowableRopeConnectorProjectile extends ThrowableItemProjectile {
    private static final Component TARGET_TOO_FAR = Component.translatable("message.create_aeronautics_throwable_rope_connector.target_too_far");
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

    private Vec3 origin = Vec3.ZERO;
    private double maxThrowDistance = 10.0D;
    private int sourceSlot = -1;
    private boolean consumeOnSuccessOnly = true;
    private BlockPos mountedSourcePos;

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

    public void setMountedSource(BlockPos mountedSourcePos) {
        this.mountedSourcePos = mountedSourcePos.immutable();
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && !this.origin.equals(Vec3.ZERO) && this.position().distanceTo(this.origin) > this.maxThrowDistance) {
            this.failAndReturn(TARGET_TOO_FAR);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (this.mountedSourcePos != null) {
            this.handleMountedBlockHit(serverLevel, result);
            return;
        }

        Entity owner = this.getOwner();
        if (!(owner instanceof Player player)) {
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
            this.failAndReturn(ThrowableRopeConnectorPlacement.CANNOT_ATTACH_THERE);
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
        compound.putBoolean("TrailFromOffhand", this.isTrailFromOffhand());
        compound.putBoolean("TrailFromLauncher", this.isTrailFromLauncher());
        compound.putBoolean("TrailFromMountedLauncher", this.isTrailFromMountedLauncher());
        if (this.mountedSourcePos != null) {
            compound.putLong("MountedSourcePos", this.mountedSourcePos.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.origin = new Vec3(compound.getDouble("OriginX"), compound.getDouble("OriginY"), compound.getDouble("OriginZ"));
        this.maxThrowDistance = compound.contains("MaxThrowDistance") ? compound.getDouble("MaxThrowDistance") : ModCommonConfig.getClampedThrowDistance();
        this.sourceSlot = compound.contains("SourceSlot") ? compound.getInt("SourceSlot") : -1;
        this.consumeOnSuccessOnly = !compound.contains("ConsumeOnSuccessOnly") || compound.getBoolean("ConsumeOnSuccessOnly");
        this.setTrailFromOffhand(compound.getBoolean("TrailFromOffhand"));
        this.setTrailFromLauncher(compound.getBoolean("TrailFromLauncher"));
        this.setTrailFromMountedLauncher(compound.getBoolean("TrailFromMountedLauncher"));
        this.mountedSourcePos = compound.contains("MountedSourcePos") ? BlockPos.of(compound.getLong("MountedSourcePos")) : null;
    }

    private void failAndReturn(Component message) {
        if (this.level().isClientSide) {
            return;
        }

        if (this.mountedSourcePos != null) {
            this.notifyMountedFailure();
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
            return;
        }

        Entity owner = this.getOwner();
        if (owner instanceof Player player) {
            if (!this.consumeOnSuccessOnly) {
                ThrowableRopeConnectorPlacement.returnThrowableItem(player, this.sourceSlot);
            }
            ThrowableRopeConnectorPlacement.fail(player, message);
        } else {
            this.spawnAtLocation(new ItemStack(ModItems.THROWABLE_ROPE_CONNECTOR.get()));
            this.level().playSound(null, BlockPos.containing(this.position()), SoundEvents.DISPENSER_FAIL, SoundSource.NEUTRAL, 0.5F, 1.0F);
        }

        this.level().broadcastEntityEvent(this, (byte) 3);
        this.discard();
    }

    private void handleMountedBlockHit(ServerLevel serverLevel, BlockHitResult result) {
        if (!(serverLevel.getBlockEntity(this.mountedSourcePos) instanceof MountedRopeLauncherBlockEntity mountedLauncher)) {
            this.failAndReturn(ThrowableRopeConnectorPlacement.CANNOT_ATTACH_THERE);
            return;
        }

        java.util.Optional<BlockPos> placedConnector = ThrowableRopeConnectorPlacement.placeConnectorFromMountedLauncher(serverLevel, result);
        if (placedConnector.isEmpty() || !mountedLauncher.connectToPlacedConnector(serverLevel, placedConnector.get(), null)) {
            placedConnector.ifPresent(pos -> serverLevel.removeBlock(pos, false));
            this.failAndReturn(ThrowableRopeConnectorPlacement.CANNOT_ATTACH_THERE);
            return;
        }

        this.level().broadcastEntityEvent(this, (byte) 3);
        this.discard();
    }

    private void notifyMountedFailure() {
        if (this.level().getBlockEntity(this.mountedSourcePos) instanceof MountedRopeLauncherBlockEntity mountedLauncher) {
            mountedLauncher.onProjectileFailed();
        }
    }
}
