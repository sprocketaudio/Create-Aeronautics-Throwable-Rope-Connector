package net.sprocketgames.create_aeronautics_throwable_rope_connector.integration;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CreateSimulatedIntegration {
    private CreateSimulatedIntegration() {
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
