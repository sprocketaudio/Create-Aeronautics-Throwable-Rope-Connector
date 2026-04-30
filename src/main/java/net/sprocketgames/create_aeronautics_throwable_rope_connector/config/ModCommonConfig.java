package net.sprocketgames.create_aeronautics_throwable_rope_connector.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ModCommonConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.push("throwable_rope_connector");

        MAX_THROW_DISTANCE = BUILDER
                .comment("Maximum distance in blocks that a thrown connector can travel before it fizzles and returns.")
                .defineInRange("maxThrowDistance", 20.0D, 1.0D, 128.0D);

        THROW_VELOCITY = BUILDER
                .comment("Projectile launch velocity. Snowballs use 1.5.")
                .defineInRange("throwVelocity", 1.5D, 0.1D, 5.0D);

        CONSUME_ON_SUCCESS_ONLY = BUILDER
                .comment("If true, the item is only consumed after the connector is placed successfully. If false, it is consumed when thrown and refunded on failure.")
                .define("consumeOnSuccessOnly", true);

        COOLDOWN_TICKS = BUILDER
                .comment("Cooldown applied after a successful throw.")
                .defineInRange("cooldownTicks", 10, 0, 200);

        PLAY_PARTICLES = BUILDER
                .comment("Spawn particles at the placed connector on success.")
                .define("playParticles", true);

        SHOW_FAILURE_MESSAGES = BUILDER
                .comment("Show action bar feedback when the item cannot be used.")
                .define("showFailureMessages", true);

        BUILDER.pop();
    }

    public static final ModConfigSpec.DoubleValue MAX_THROW_DISTANCE;
    public static final ModConfigSpec.DoubleValue THROW_VELOCITY;
    public static final ModConfigSpec.BooleanValue CONSUME_ON_SUCCESS_ONLY;
    public static final ModConfigSpec.IntValue COOLDOWN_TICKS;
    public static final ModConfigSpec.BooleanValue PLAY_PARTICLES;
    public static final ModConfigSpec.BooleanValue SHOW_FAILURE_MESSAGES;
    public static final ModConfigSpec SPEC = BUILDER.build();

    private ModCommonConfig() {
    }
}
