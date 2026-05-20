package net.sprocketgames.create_aeronautics_throwable_rope_connector.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.CreateAeronauticsThrowableRopeConnector;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.block.MountedRopeLauncherBlockEntity;

public final class ModBlockEntityTypes {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(
            Registries.BLOCK_ENTITY_TYPE,
            CreateAeronauticsThrowableRopeConnector.MOD_ID
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MountedRopeLauncherBlockEntity>> MOUNTED_ROPE_LAUNCHER =
            BLOCK_ENTITY_TYPES.register(
                    "mounted_rope_launcher",
                    () -> BlockEntityType.Builder.of(
                            MountedRopeLauncherBlockEntity::new,
                            ModBlocks.MOUNTED_ROPE_LAUNCHER.get()
                    ).build(null)
            );

    private ModBlockEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
