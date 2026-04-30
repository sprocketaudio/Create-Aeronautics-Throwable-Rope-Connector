package net.sprocketgames.create_aeronautics_throwable_rope_connector.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.CreateAeronauticsThrowableRopeConnector;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.entity.ThrowableRopeConnectorProjectile;

public final class ModEntityTypes {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(
            Registries.ENTITY_TYPE,
            CreateAeronauticsThrowableRopeConnector.MOD_ID
    );

    public static final DeferredHolder<EntityType<?>, EntityType<ThrowableRopeConnectorProjectile>> THROWABLE_ROPE_CONNECTOR = ENTITY_TYPES.register(
            "throwable_rope_connector",
            () -> EntityType.Builder.<ThrowableRopeConnectorProjectile>of(ThrowableRopeConnectorProjectile::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("throwable_rope_connector")
    );

    private ModEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
