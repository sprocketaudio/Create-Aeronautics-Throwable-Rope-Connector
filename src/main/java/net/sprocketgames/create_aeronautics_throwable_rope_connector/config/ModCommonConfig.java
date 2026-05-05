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

        LAUNCHER_MAX_DISTANCE = BUILDER
                .comment(
                        "Maximum distance in blocks that the Rope Connector Launcher can fire a connector before it fizzles and returns.",
                        "Default is 40, matching Create Simulated's default max_rope_range.",
                        "For launcher shots to connect successfully, the Simulated server config max_rope_range must match or exceed this value."
                )
                .defineInRange("launcherMaxDistance", 40.0D, 1.0D, 1000.0D);

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

        SHOW_PROJECTILE_ROPE_TRAIL = BUILDER
                .comment("Render a client-side rope-like line from the player to the flying connector projectile.")
                .define("showProjectileRopeTrail", true);

        PROJECTILE_ROPE_TRAIL_WIDTH = BUILDER
                .comment("Visual width in blocks for the client-side projectile rope trail. Default matches Simulated's 3-pixel rope model width.")
                .defineInRange("projectileRopeTrailWidth", 0.1875D, 0.01D, 0.5D);

        BUILDER.pop();
    }

    public static final ModConfigSpec.DoubleValue MAX_THROW_DISTANCE;
    public static final ModConfigSpec.DoubleValue THROW_VELOCITY;
    public static final ModConfigSpec.DoubleValue LAUNCHER_MAX_DISTANCE;
    public static final ModConfigSpec.BooleanValue CONSUME_ON_SUCCESS_ONLY;
    public static final ModConfigSpec.IntValue COOLDOWN_TICKS;
    public static final ModConfigSpec.BooleanValue PLAY_PARTICLES;
    public static final ModConfigSpec.BooleanValue SHOW_FAILURE_MESSAGES;
    public static final ModConfigSpec.BooleanValue SHOW_PROJECTILE_ROPE_TRAIL;
    public static final ModConfigSpec.DoubleValue PROJECTILE_ROPE_TRAIL_WIDTH;
    public static final ModConfigSpec SPEC = BUILDER.build();

    private ModCommonConfig() {
    }
}
