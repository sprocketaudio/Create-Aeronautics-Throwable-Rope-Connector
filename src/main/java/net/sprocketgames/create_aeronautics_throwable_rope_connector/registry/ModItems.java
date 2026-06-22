package net.sprocketgames.create_aeronautics_throwable_rope_connector.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.CreateAeronauticsThrowableRopeConnector;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.item.MountedRopeLauncherBlockItem;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.item.RopeConnectorLauncherItem;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.item.ThrowableRopeConnectorItem;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateAeronauticsThrowableRopeConnector.MOD_ID);

    public static final DeferredItem<Item> THROWABLE_ROPE_CONNECTOR = ITEMS.register(
            "throwable_rope_connector",
            () -> new ThrowableRopeConnectorItem(new Item.Properties())
    );

    public static final DeferredItem<Item> ROPE_CONNECTOR_LAUNCHER = ITEMS.register(
            "rope_connector_launcher",
            () -> new RopeConnectorLauncherItem(new Item.Properties().stacksTo(1))
    );

    public static final DeferredItem<BlockItem> MOUNTED_ROPE_LAUNCHER = ITEMS.register(
            "mounted_rope_launcher",
            () -> new MountedRopeLauncherBlockItem(ModBlocks.MOUNTED_ROPE_LAUNCHER.get(), new Item.Properties())
    );

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
