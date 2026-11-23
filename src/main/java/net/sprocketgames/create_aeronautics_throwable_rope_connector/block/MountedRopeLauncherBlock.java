package net.sprocketgames.create_aeronautics_throwable_rope_connector.block;

import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.content.contraptions.StructureTransform;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;
import dev.simulated_team.simulated.index.SimBlockShapes;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModBlockEntityTypes;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.integration.CreateSimulatedIntegration;
import org.jetbrains.annotations.Nullable;

public final class MountedRopeLauncherBlock extends RopeWinchBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final DirectionProperty HEAD_DIRECTION = DirectionProperty.create("head_direction");
    private static final DirectionalAxisShaper LOWER_SHAPE = DirectionalAxisShaper.make(
            Shapes.or(
                    SimBlockShapes.ROPE_WINCH,
                    Block.box(6.0D, 0.0D, 4.0D, 10.0D, 2.0D, 10.0D)
            )
    );
    private static final VoxelShape UPPER_SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 8.0D, 12.0D);

    static {
        BlockMovementChecks.registerAttachedCheck((state, level, pos, direction) -> {
            if (state.getBlock() instanceof MountedRopeLauncherBlock launcher
                    && counterpartPos(state, pos).equals(pos.relative(direction))
                    && launcher.isCounterpart(state, level.getBlockState(pos.relative(direction)))) {
                return BlockMovementChecks.CheckResult.SUCCESS;
            }
            return BlockMovementChecks.CheckResult.PASS;
        });
    }

    public MountedRopeLauncherBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(HEAD_DIRECTION, Direction.UP));
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
        Direction head = context.getClickedFace();
        BlockPos headPos = context.getClickedPos().relative(head);
        Level level = context.getLevel();
        if (level.isOutsideBuildHeight(headPos) || !level.getWorldBorder().isWithinBounds(headPos)
                || !level.getBlockState(headPos).canBeReplaced(BlockPlaceContext.at(context, headPos, head))) {
            return null;
        }

        Direction forward = context.getHorizontalDirection();
        if (head.getAxis().isHorizontal()) {
            for (Direction look : context.getNearestLookingDirections()) {
                if (look.getAxis() != head.getAxis()) {
                    forward = look;
                    break;
                }
            }
        }
        return withOrientation(this.defaultBlockState(), forward, head);
    }

    private BlockState withOrientation(BlockState state, Direction forward, Direction head) {
        Direction.Axis shaft = new MountedLauncherOrientation(forward, head).fireSide().getAxis();
        Direction.Axis firstAxis = forward.getAxis() == Direction.Axis.X ? Direction.Axis.Y : Direction.Axis.X;
        return state.setValue(FACING, forward).setValue(HEAD_DIRECTION, head)
                .setValue(AXIS_ALONG_FIRST_COORDINATE, shaft == firstAxis);
    }

    @Override
    public BlockState transform(BlockState state, StructureTransform transform) {
        if (transform.mirror != null) {
            state = this.mirror(state, transform.mirror);
        }
        var orientation = MountedLauncherOrientation.of(state);
        return withOrientation(state, transform.rotateFacing(orientation.forward()), transform.rotateFacing(orientation.up()));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        var orientation = MountedLauncherOrientation.of(state);
        return withOrientation(state, rotation.rotate(orientation.forward()), rotation.rotate(orientation.up()));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        var orientation = MountedLauncherOrientation.of(state);
        return withOrientation(state, mirror.mirror(orientation.forward()), mirror.mirror(orientation.up()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HALF, HEAD_DIRECTION);
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
        level.setBlock(counterpartPos(state, pos), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        this.updateRedstoneInputs(level, pos);
    }

    public static BlockPos getBasePos(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? counterpartPos(state, pos) : pos;
    }

    public static BlockPos getHeadPos(BlockState state, BlockPos basePos) {
        return basePos.relative(state.getValue(HEAD_DIRECTION));
    }

    private static BlockPos counterpartPos(BlockState state, BlockPos pos) {
        Direction head = state.getValue(HEAD_DIRECTION);
        return pos.relative(state.getValue(HALF) == DoubleBlockHalf.UPPER ? head.getOpposite() : head);
    }

    private boolean isCounterpart(BlockState state, BlockState otherState) {
        return otherState.is(this) && otherState.getValue(HALF) != state.getValue(HALF)
                && otherState.getValue(HEAD_DIRECTION) == state.getValue(HEAD_DIRECTION)
                && otherState.getValue(FACING) == state.getValue(FACING);
    }

    private boolean hasBase(BlockGetter level, BlockState state, BlockPos basePos) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER || this.isCounterpart(state, level.getBlockState(basePos));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        BlockPos basePos = getBasePos(state, pos);
        if (!this.hasBase(level, state, basePos)) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        InteractionHand couplingHand = CreateSimulatedIntegration.isPreparedRopeCoupling(stack) ? hand : InteractionHand.OFF_HAND;
        ItemStack coupling = player.getItemInHand(couplingHand);
        if (CreateSimulatedIntegration.isPreparedRopeCoupling(coupling)) {
            InteractionResult result = coupling.useOn(new UseOnContext(player, couplingHand, hitResult.withPosition(basePos)));
            if (result.consumesAction()) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
            return result == InteractionResult.FAIL ? ItemInteractionResult.FAIL : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide()) {
            MountedRopeLauncherBlockEntity blockEntity = this.getMountedBlockEntity(level, basePos);
            ServerPlayer serverPlayer = player instanceof ServerPlayer sp ? sp : null;
            if (blockEntity != null && serverPlayer != null) {
                if (stack.isEmpty() && player.isCrouching()) {
                    if (blockEntity.isConnected()) {
                        blockEntity.disconnectRope();
                    } else {
                        blockEntity.unloadAmmo(player);
                    }
                    return ItemInteractionResult.SUCCESS;
                }

                if (CreateSimulatedIntegration.isLauncherAmmo(stack)) {
                    blockEntity.loadAmmo(serverPlayer, hand);
                    return ItemInteractionResult.SUCCESS;
                }

                blockEntity.mount(serverPlayer);
                return ItemInteractionResult.SUCCESS;
            }
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockPos basePos = getBasePos(state, pos);
        if (!this.hasBase(level, state, basePos)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            MountedRopeLauncherBlockEntity blockEntity = this.getMountedBlockEntity(level, basePos);
            if (blockEntity != null) {
                ServerPlayer serverPlayer = player instanceof ServerPlayer sp ? sp : null;
                if (player.isCrouching()) {
                    if (serverPlayer != null) {
                        if (blockEntity.isConnected()) {
                            blockEntity.disconnectRope();
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
        BlockPos basePos = getBasePos(state, pos);
        if (this.hasBase(level, state, basePos)) {
            this.updateRedstoneInputs(level, basePos);
        }
    }

    private void updateRedstoneInputs(Level level, BlockPos basePos) {
        if (level.isClientSide()) {
            return;
        }

        MountedRopeLauncherBlockEntity blockEntity = this.getMountedBlockEntity(level, basePos);
        BlockState baseState = level.getBlockState(basePos);
        if (blockEntity == null || !baseState.is(this)) {
            return;
        }

        if (baseState.getValue(FACING).getAxis() == baseState.getValue(HEAD_DIRECTION).getAxis()) {
            blockEntity.handleRedstoneSignal(false, false);
            return;
        }

        Direction fireSide = MountedLauncherOrientation.of(baseState).fireSide();
        BlockPos headPos = getHeadPos(baseState, basePos);
        blockEntity.handleRedstoneSignal(
                this.isSidePowered(level, basePos, headPos, fireSide),
                this.isSidePowered(level, basePos, headPos, fireSide.getOpposite())
        );
    }

    private boolean isSidePowered(Level level, BlockPos basePos, BlockPos headPos, Direction side) {
        return this.hasInputFrom(level, basePos, side) || this.hasInputFrom(level, headPos, side);
    }

    private boolean hasInputFrom(Level level, BlockPos targetPos, Direction side) {
        BlockPos neighborPos = targetPos.relative(side);
        BlockState neighborState = level.getBlockState(neighborPos);
        return neighborState.getSignal(level, neighborPos, side.getOpposite()) > 0;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        var orientation = MountedLauncherOrientation.of(state);
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            return orientation.up() == Direction.UP ? UPPER_SHAPE : orientation.rotateShape(UPPER_SHAPE);
        }

        return orientation.up() == Direction.UP ? LOWER_SHAPE.get(orientation.forward(), state.getValue(AXIS_ALONG_FIRST_COORDINATE))
                : orientation.rotateShape(LOWER_SHAPE.get(Direction.SOUTH, true));
    }

    @Override
    public VoxelShape getSubLevelCollisionShape(BlockGetter level, BlockState state) {
        var orientation = MountedLauncherOrientation.of(state);
        if (orientation.up() == Direction.UP) {
            return super.getSubLevelCollisionShape(level, state);
        }
        var upright = withOrientation(state, Direction.SOUTH, Direction.UP);
        return orientation.rotateShape(state.getValue(HALF) == DoubleBlockHalf.UPPER
                ? UPPER_SHAPE : super.getSubLevelCollisionShape(level, upright));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!moving && !state.is(newState.getBlock())) {
            BlockPos otherPos = counterpartPos(state, pos);
            BlockPos basePos = getBasePos(state, pos);
            if (this.hasBase(level, state, basePos)) {
                MountedRopeLauncherBlockEntity blockEntity = this.getMountedBlockEntity(level, basePos);
                if (blockEntity != null) {
                    blockEntity.onBroken();
                }
            }
            BlockState otherState = level.getBlockState(otherPos);
            if (this.isCounterpart(state, otherState)) {
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
