package net.sprocketgames.create_aeronautics_throwable_rope_connector.integration;

import java.util.Optional;
import dev.simulated_team.simulated.content.items.rope.RopeItem.RopeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModItems;

public final class CreateSimulatedIntegration {
    private CreateSimulatedIntegration() {
    }

    public static boolean isRopeCoupling(ItemStack stack) {
        return stack.is(BuiltInRegistries.ITEM.get(CreateSimulatedIds.ROPE_ITEM));
    }

    public static boolean isPreparedRopeCoupling(ItemStack stack) {
        DataComponentType<?> firstConnection = BuiltInRegistries.DATA_COMPONENT_TYPE.get(CreateSimulatedIds.ROPE_FIRST_CONNECTION_COMPONENT);
        return isRopeCoupling(stack) && firstConnection != null && stack.has(firstConnection);
    }

    public static boolean isLauncherAmmo(ItemStack stack) {
        if (stack.is(ModItems.THROWABLE_ROPE_CONNECTOR.get())) {
            return true;
        }
        DataComponentType<?> firstConnection = BuiltInRegistries.DATA_COMPONENT_TYPE.get(CreateSimulatedIds.ROPE_FIRST_CONNECTION_COMPONENT);
        return isRopeCoupling(stack) && firstConnection != null && !stack.has(firstConnection);
    }

    public static boolean isRopeConnector(Level level, BlockPos pos) {
        Block connector = BuiltInRegistries.BLOCK.get(CreateSimulatedIds.ROPE_CONNECTOR_ITEM);
        return level.getBlockState(pos).is(connector);
    }

    public static boolean canUseDockConnector(Level level, BlockPos pos) {
        if (!isRopeConnector(level, pos)) {
            return false;
        }
        var holder = RopeItem.getRopeHolder(level, pos);
        return holder != null && !holder.isAttached();
    }

    public static Optional<BlockItem> getRopeConnectorItem() {
        Item item = BuiltInRegistries.ITEM.get(CreateSimulatedIds.ROPE_CONNECTOR_ITEM);
        if (item instanceof BlockItem blockItem) {
            return Optional.of(blockItem);
        }

        return Optional.empty();
    }

    public static Optional<ItemStack> createPreselectedRope(BlockPos firstConnection) {
        Item ropeItem = BuiltInRegistries.ITEM.get(CreateSimulatedIds.ROPE_ITEM);
        if (ropeItem == null || ropeItem == Items.AIR) {
            return Optional.empty();
        }

        ItemStack stack = new ItemStack(ropeItem);
        if (!setFirstConnection(stack, firstConnection)) {
            return Optional.empty();
        }

        return Optional.of(stack);
    }

    @SuppressWarnings("unchecked")
    public static boolean setFirstConnection(ItemStack stack, BlockPos firstConnection) {
        DataComponentType<?> rawType = BuiltInRegistries.DATA_COMPONENT_TYPE.get(CreateSimulatedIds.ROPE_FIRST_CONNECTION_COMPONENT);
        if (rawType == null) {
            return false;
        }

        DataComponentType<BlockPos> blockPosComponent = (DataComponentType<BlockPos>) rawType;
        stack.set(blockPosComponent, firstConnection.immutable());
        return true;
    }
}
