package net.sprocketgames.create_aeronautics_throwable_rope_connector.config;

import dev.simulated_team.simulated.service.SimConfigService;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ModCommonConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final int DEFAULT_COOLDOWN_TICKS = 10;
    public static final double DEFAULT_PROJECTILE_ROPE_TRAIL_WIDTH = 0.1875D;

    static {
        BUILDER.push("throwing");
        MAX_THROW_DISTANCE = BUILDER
                .comment(
                        "Maximum distance in blocks that a thrown connector can travel before it fizzles and returns.",
                        "Default is 10.",
                        "Create Simulated's server config max_rope_range is the absolute maximum this can ever use at runtime."
                )
                .defineInRange("maxThrowDistance", 10.0D, 1.0D, 128.0D);

        THROW_VELOCITY = BUILDER
                .comment("Projectile launch velocity. Snowballs use 1.5.")
                .defineInRange("throwVelocity", 1.5D, 0.1D, 5.0D);
        BUILDER.pop();

        BUILDER.push("launcher");
        LAUNCHER_MAX_DISTANCE = BUILDER
                .comment(
                        "Maximum distance in blocks that the Rope Connector Launcher can fire a connector before it fizzles and returns.",
                        "Default is 20, intended as a step up from throwing by hand.",
                        "Create Simulated's server config max_rope_range is the absolute maximum this can ever use at runtime."
                )
                .defineInRange("launcherMaxDistance", 20.0D, 1.0D, 1000.0D);
        BUILDER.pop();

        BUILDER.push("mounted");
        MOUNTED_LAUNCHER_RANGE = BUILDER
                .comment(
                        "Maximum distance in blocks that a Mounted Rope Launcher can fire a connector.",
                        "Default is 40, matching Create Simulated's default max_rope_range in this pack.",
                        "This can exceed the normal handheld launcher range because the mounted block creates the rope link itself.",
                        "Create Simulated's server config max_rope_range is the absolute maximum this can ever use at runtime."
                )
                .defineInRange("mountedLauncherRange", 40.0D, 1.0D, 1000.0D);

        MOUNTED_REMOVE_PLACED_CONNECTOR_ON_RELEASE = BUILDER
                .comment("Remove the remote rope connector block placed by the Mounted Rope Launcher when releasing its rope.")
                .define("removePlacedConnectorOnRelease", true);
        BUILDER.pop();

        BUILDER.push("general");
        CONSUME_ON_SUCCESS_ONLY = BUILDER
                .comment(
                        "If true, throwable connector ammo is only consumed after a successful attachment.",
                        "This applies to hand throws, the handheld launcher, and the mounted launcher."
                )
                .define("consumeOnSuccessOnly", true);

        PLAY_PARTICLES = BUILDER
                .comment("Spawn particles at the placed connector on success.")
                .define("playParticles", true);

        SHOW_FAILURE_MESSAGES = BUILDER
                .comment("Show action bar feedback when the item cannot be used.")
                .define("showFailureMessages", true);
        BUILDER.pop();

        BUILDER.push("visual");
        SHOW_PROJECTILE_ROPE_TRAIL = BUILDER
                .comment("Render a client-side rope-like line for thrown, handheld-launched, and mounted-launched connector projectiles.")
                .define("showProjectileRopeTrail", true);
        BUILDER.pop();

    }

    public static final ModConfigSpec.DoubleValue MAX_THROW_DISTANCE;
    public static final ModConfigSpec.DoubleValue THROW_VELOCITY;
    public static final ModConfigSpec.DoubleValue LAUNCHER_MAX_DISTANCE;
    public static final ModConfigSpec.BooleanValue CONSUME_ON_SUCCESS_ONLY;
    public static final ModConfigSpec.BooleanValue PLAY_PARTICLES;
    public static final ModConfigSpec.BooleanValue SHOW_FAILURE_MESSAGES;
    public static final ModConfigSpec.BooleanValue SHOW_PROJECTILE_ROPE_TRAIL;
    public static final ModConfigSpec.DoubleValue MOUNTED_LAUNCHER_RANGE;
    public static final ModConfigSpec.BooleanValue MOUNTED_REMOVE_PLACED_CONNECTOR_ON_RELEASE;
    public static final ModConfigSpec SPEC = BUILDER.build();

    private ModCommonConfig() {
    }

    public static double getClampedThrowDistance() {
        return MAX_THROW_DISTANCE.get();
    }

    public static double getClampedLauncherMaxDistance() {
        return LAUNCHER_MAX_DISTANCE.get();
    }

    public static double getClampedMountedLauncherRange() {
        return MOUNTED_LAUNCHER_RANGE.get();
    }

    public static double getSimulatedMaxRopeRange() {
        try {
            return SimConfigService.INSTANCE.server().blocks.maxRopeRange.getF();
        } catch (Exception ignored) {
            return 40.0D;
        }
    }

    private static double clampToSimulatedMax(double configuredValue) {
        return Math.min(configuredValue, getSimulatedMaxRopeRange());
    }

    public static boolean correctRangeValuesToSimulatedMax() {
        boolean changed = false;
        changed |= correctRangeValue(MAX_THROW_DISTANCE);
        changed |= correctRangeValue(LAUNCHER_MAX_DISTANCE);
        changed |= correctRangeValue(MOUNTED_LAUNCHER_RANGE);
        return changed;
    }

    private static boolean correctRangeValue(ModConfigSpec.DoubleValue value) {
        double corrected = clampToSimulatedMax(value.get());
        if (Double.compare(value.get(), corrected) != 0) {
            value.set(corrected);
            return true;
        }
        return false;
    }
}
