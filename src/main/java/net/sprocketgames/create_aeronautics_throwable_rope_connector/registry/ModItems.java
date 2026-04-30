package net.sprocketgames.create_aeronautics_throwable_rope_connector.registry;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.CreateAeronauticsThrowableRopeConnector;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.item.ThrowableRopeConnectorItem;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateAeronauticsThrowableRopeConnector.MOD_ID);

    public static final DeferredItem<Item> THROWABLE_ROPE_CONNECTOR = ITEMS.register(
            "throwable_rope_connector",
            () -> new ThrowableRopeConnectorItem(new Item.Properties())
    );

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    public static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(THROWABLE_ROPE_CONNECTOR);
        }
    }
}
