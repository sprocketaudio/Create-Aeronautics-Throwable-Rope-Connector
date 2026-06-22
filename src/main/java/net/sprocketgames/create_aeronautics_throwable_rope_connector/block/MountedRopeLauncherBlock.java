package net.sprocketgames.create_aeronautics_throwable_rope_connector.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchBlock;
import dev.simulated_team.simulated.util.DirectionalAxisShaper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;
import dev.simulated_team.simulated.index.SimBlockShapes;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModBlockEntityTypes;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModItems;
import org.jetbrains.annotations.Nullable;

public final class MountedRopeLauncherBlock extends RopeWinchBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    private static final DirectionalAxisShaper LOWER_SHAPE = DirectionalAxisShaper.make(
            Shapes.or(
                    SimBlockShapes.ROPE_WINCH,
                    Block.box(6.0D, 0.0D, 4.0D, 10.0D, 2.0D, 10.0D)
            )
    );
    private static final VoxelShape UPPER_SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 8.0D, 12.0D);

    public MountedRopeLauncherBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Class getBlockEntityClass() {
        return MountedRopeLauncherBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends MountedRopeLauncherBlockEntity> getBlockEntityType() {
        return ModBlockEntityTypes.MOUNTED_ROPE_LAUNCHER.get();
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos above = context.getClickedPos().above();
        if (above.getY() >= context.getLevel().getMaxBuildHeight() || !context.getLevel().getBlockState(above).canBeReplaced(context)) {
            return null;
        }

        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }

        Direction horizontalFacing = context.getHorizontalDirection();
        boolean shaftAlongFirstCoordinate = horizontalFacing.getAxis() == Direction.Axis.Z;
        return state
                .setValue(FACING, horizontalFacing)
                .setValue(AXIS_ALONG_FIRST_COORDINATE, shaftAlongFirstCoordinate)
                .setValue(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HALF);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            return null;
        }

        return new MountedRopeLauncherBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
        MountedRopeLauncherBlockEntity blockEntity = this.getMountedBlockEntity(level, pos);
        if (blockEntity != null) {
            blockEntity.handleRedstoneSignal(this.isFirePowered(level, pos, state), this.isReleasePowered(level, pos, state));
        }
    }

    private BlockPos getBasePos(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        BlockPos basePos = this.getBasePos(state, pos);
        if (!level.isClientSide()) {
            MountedRopeLauncherBlockEntity blockEntity = this.getMountedBlockEntity(level, basePos);
            ServerPlayer serverPlayer = player instanceof ServerPlayer sp ? sp : null;
            if (blockEntity != null && serverPlayer != null) {
                if (stack.isEmpty() && player.isCrouching()) {
                    if (blockEntity.isConnected()) {
                        blockEntity.tryRemoteRelease(serverPlayer);
                    } else {
                        blockEntity.unloadAmmo(player);
                    }
                    return ItemInteractionResult.SUCCESS;
                }

                if (stack.is(ModItems.THROWABLE_ROPE_CONNECTOR.get())) {
                    blockEntity.loadAmmo(serverPlayer, hand);
                    return ItemInteractionResult.SUCCESS;
                }

                blockEntity.mount(serverPlayer);
                return ItemInteractionResult.SUCCESS;
            }
        }

        if (stack.is(ModItems.THROWABLE_ROPE_CONNECTOR.get())) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockPos basePos = this.getBasePos(state, pos);
        if (!level.isClientSide()) {
            MountedRopeLauncherBlockEntity blockEntity = this.getMountedBlockEntity(level, basePos);
            if (blockEntity != null) {
                ServerPlayer serverPlayer = player instanceof ServerPlayer sp ? sp : null;
                if (player.isCrouching()) {
                    if (serverPlayer != null) {
                        if (blockEntity.isConnected()) {
                            blockEntity.tryRemoteRelease(serverPlayer);
                        } else {
                            blockEntity.unloadAmmo(player);
                        }
                    }
                    return InteractionResult.SUCCESS;
                }

                if (serverPlayer != null) {
                    blockEntity.mount(serverPlayer);
                }
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (level.isClientSide()) {
            return;
        }

        BlockPos basePos = this.getBasePos(state, pos);
        MountedRopeLauncherBlockEntity blockEntity = this.getMountedBlockEntity(level, basePos);
        if (blockEntity != null) {
            BlockState baseState = level.getBlockState(basePos);
            blockEntity.handleRedstoneSignal(
                    this.isFirePowered(level, basePos, baseState),
                    this.isReleasePowered(level, basePos, baseState)
            );
        }
    }

    private boolean isFirePowered(Level level, BlockPos basePos, BlockState state) {
        return this.isSidePowered(level, basePos, state, state.getValue(FACING).getClockWise());
    }

    private boolean isReleasePowered(Level level, BlockPos basePos, BlockState state) {
        return this.isSidePowered(level, basePos, state, state.getValue(FACING).getCounterClockWise());
    }

    private boolean isSidePowered(Level level, BlockPos basePos, BlockState state, Direction side) {
        if (!(state.getBlock() instanceof MountedRopeLauncherBlock)) {
            return false;
        }

        return this.hasInputFrom(level, basePos, side) || this.hasInputFrom(level, basePos.above(), side);
    }

    private boolean hasInputFrom(Level level, BlockPos targetPos, Direction side) {
        BlockPos neighborPos = targetPos.relative(side);
        BlockState neighborState = level.getBlockState(neighborPos);
        return neighborState.getSignal(level, neighborPos, side.getOpposite()) > 0;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            return UPPER_SHAPE;
        }

        return LOWER_SHAPE.get(state.getValue(FACING), state.getValue(AXIS_ALONG_FIRST_COORDINATE));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            BlockPos otherPos = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos.above();
            MountedRopeLauncherBlockEntity blockEntity = this.getMountedBlockEntity(level, this.getBasePos(state, pos));
            if (blockEntity != null) {
                blockEntity.onBroken();
            }
            BlockState otherState = level.getBlockState(otherPos);
            if (otherState.is(this) && otherState.getValue(HALF) != state.getValue(HALF)) {
                level.removeBlock(otherPos, false);
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Nullable
    private MountedRopeLauncherBlockEntity getMountedBlockEntity(BlockGetter level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof MountedRopeLauncherBlockEntity mounted ? mounted : null;
    }
}
