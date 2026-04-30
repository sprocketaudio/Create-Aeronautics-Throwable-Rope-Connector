package net.sprocketgames.create_aeronautics_throwable_rope_connector.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.sprocketgames.create_aeronautics_throwable_rope_connector.item.ThrowableRopeConnectorPlacement;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModEntityTypes;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModItems;

public final class ThrowableRopeConnectorProjectile extends ThrowableItemProjectile {
    private static final Component TARGET_TOO_FAR = Component.translatable("message.create_aeronautics_throwable_rope_connector.target_too_far");

    private Vec3 origin = Vec3.ZERO;
    private double maxThrowDistance = 20.0D;
    private int sourceSlot = -1;
    private boolean consumeOnSuccessOnly = true;

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
    protected Item getDefaultItem() {
        return ModItems.THROWABLE_ROPE_CONNECTOR.get();
    }

    public void setSourceSlot(int sourceSlot) {
        this.sourceSlot = sourceSlot;
    }

    public void setConsumeOnSuccessOnly(boolean consumeOnSuccessOnly) {
        this.consumeOnSuccessOnly = consumeOnSuccessOnly;
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.origin = new Vec3(compound.getDouble("OriginX"), compound.getDouble("OriginY"), compound.getDouble("OriginZ"));
        this.maxThrowDistance = compound.contains("MaxThrowDistance") ? compound.getDouble("MaxThrowDistance") : ModCommonConfig.MAX_THROW_DISTANCE.get();
        this.sourceSlot = compound.contains("SourceSlot") ? compound.getInt("SourceSlot") : -1;
        this.consumeOnSuccessOnly = !compound.contains("ConsumeOnSuccessOnly") || compound.getBoolean("ConsumeOnSuccessOnly");
    }

    private void failAndReturn(Component message) {
        if (this.level().isClientSide) {
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
}
