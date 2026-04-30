package net.sprocketgames.create_aeronautics_throwable_rope_connector.integration;

import net.minecraft.resources.ResourceLocation;

public final class CreateSimulatedIds {
    public static final String SIMULATED_MOD_ID = "simulated";

    public static final ResourceLocation ROPE_CONNECTOR_ITEM = id("rope_connector");
    public static final ResourceLocation ROPE_ITEM = id("rope_coupling");
    public static final ResourceLocation ROPE_FIRST_CONNECTION_COMPONENT = id("rope_first_connection");

    private CreateSimulatedIds() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(SIMULATED_MOD_ID, path);
    }
}
