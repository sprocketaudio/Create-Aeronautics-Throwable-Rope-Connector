package net.sprocketgames.create_aeronautics_throwable_rope_connector;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.integration.CreateSimulatedIds;

final class DockingJadeTests {
    static void checkJadeData(GameTestHelper test, net.minecraft.core.BlockPos launcherPos) {
        try {
            var providerClass = Class.forName("net.sprocketgames.create_aeronautics_throwable_rope_connector.integration.MountedLauncherJadePlugin$Provider");
            var provider = (snownee.jade.api.IServerDataProvider<snownee.jade.api.BlockAccessor>) providerClass.getEnumConstants()[0];
            var level = test.getLevel();
            var upper = test.absolutePos(launcherPos.above());
            var accessor = (snownee.jade.api.BlockAccessor) java.lang.reflect.Proxy.newProxyInstance(
                    snownee.jade.api.BlockAccessor.class.getClassLoader(), new Class<?>[]{snownee.jade.api.BlockAccessor.class},
                    (ignored, method, args) -> switch (method.getName()) {
                        case "getPosition" -> upper;
                        case "getBlockState" -> level.getBlockState(upper);
                        case "getLevel" -> level;
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
            var tag = new net.minecraft.nbt.CompoundTag();
            provider.appendServerData(tag, accessor);
            var ammo = ItemStack.parseOptional(level.registryAccess(), tag.getCompound("LauncherAmmo"));
            test.assertTrue(tag.getBoolean("LauncherConnected") && ammo.is(BuiltInRegistries.ITEM.get(CreateSimulatedIds.ROPE_ITEM)) && ammo.getCount() == 1,
                    "Jade reads the server-side rope and ammo from the upper half");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
