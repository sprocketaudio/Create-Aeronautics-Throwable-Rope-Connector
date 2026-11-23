package net.sprocketgames.create_aeronautics_throwable_rope_connector.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public record MountedLauncherOrientation(Direction forward, Direction up) {
    public static MountedLauncherOrientation of(BlockState state) {
        Direction up = state.getValue(MountedRopeLauncherBlock.HEAD_DIRECTION);
        Direction forward = state.getValue(MountedRopeLauncherBlock.FACING);
        // Older saves can contain vertical facings with no recorded head direction.
        if (forward.getAxis() == up.getAxis()) {
            forward = up.getAxis() == Direction.Axis.Z ? Direction.EAST : Direction.SOUTH;
        }
        return new MountedLauncherOrientation(forward, up);
    }

    public Direction fireSide() {
        Vec3 normal = normal(forward).cross(normal(up));
        return Direction.getNearest(normal.x, normal.y, normal.z);
    }

    // Model and aim space use south as forward and world up as the head direction.
    public Vec3 toBlockSpace(Vec3 offset) {
        return normal(fireSide().getOpposite()).scale(offset.x)
                .add(normal(up).scale(offset.y))
                .add(normal(forward).scale(offset.z));
    }

    public Vec3 toLauncherSpace(Vec3 offset) {
        return new Vec3(offset.dot(normal(fireSide().getOpposite())), offset.dot(normal(up)), offset.dot(normal(forward)));
    }

    public VoxelShape rotateShape(VoxelShape shape) {
        Vec3 center = new Vec3(0.5, 0.5, 0.5);
        VoxelShape rotated = Shapes.empty();
        for (var box : shape.toAabbs()) {
            Vec3 a = toBlockSpace(new Vec3(box.minX, box.minY, box.minZ).subtract(center)).add(center);
            Vec3 b = toBlockSpace(new Vec3(box.maxX, box.maxY, box.maxZ).subtract(center)).add(center);
            rotated = Shapes.or(rotated, Shapes.create(new net.minecraft.world.phys.AABB(a, b)));
        }
        return rotated;
    }

    private static Vec3 normal(Direction direction) {
        return Vec3.atLowerCornerOf(direction.getNormal());
    }
}
