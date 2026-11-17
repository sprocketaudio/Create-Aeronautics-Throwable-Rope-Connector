package net.sprocketgames.create_aeronautics_throwable_rope_connector;

import dev.simulated_team.simulated.content.items.rope.RopeItem.RopeItem;
import dev.simulated_team.simulated.content.blocks.rope.rope_connector.RopeConnectorBlock;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.block.MountedRopeLauncherBlock;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.block.MountedRopeLauncherBlockEntity;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.integration.CreateSimulatedIds;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.item.ThrowableRopeConnectorPlacement;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModBlocks;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModItems;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@GameTestHolder(CreateAeronauticsThrowableRopeConnector.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DockingGameTests {
    private static final BlockPos LAUNCHER = new BlockPos(2, 1, 5);
    static final BlockPos ANCHOR = new BlockPos(7, 2, 5);

    @GameTest(template = "docking_test", timeoutTicks = 100)
    public static void mountedRedocking(GameTestHelper test) {
        var mounted = placeLauncher(test);
        var anchor = connector();
        for (int y = 1; y <= 4; y++) {
            test.setBlock(8, y, 5, Blocks.STONE);
        }
        mounted.getAutomationInventory().insertItem(0, new ItemStack(ModItems.THROWABLE_ROPE_CONNECTOR.get()), false);
        mounted.fire(null);
        test.runAfterDelay(8, () -> {
            test.assertBlockPresent(anchor, ANCHOR);
            test.assertTrue(mounted.isConnected(), "Connector shot must attach to the new anchor");
            test.assertValueEqual(mounted.getAmmoCount(), 0, "Connector ammo consumed");
            assertSavedState(test, mounted, true, 0);
            mounted.handleRedstoneSignal(false, true);
            test.assertFalse(mounted.isConnected(), "Redstone release disconnects");
            assertSavedState(test, mounted, false, 0);
            test.assertBlockPresent(anchor, ANCHOR);
            test.assertFalse(RopeItem.getRopeHolder(test.getLevel(), test.absolutePos(ANCHOR)).isAttached(), "Anchor is free");
        });
        test.runAfterDelay(18, () -> {
            test.assertTrue(mounted.getAutomationInventory().insertItem(0, new ItemStack(coupling(), 2), false).isEmpty(), "Automation loads couplings");
            mounted.fire(null);
        });
        test.runAfterDelay(26, () -> {
            test.assertTrue(mounted.isConnected(), "Coupling shot redocks");
            test.assertValueEqual(mounted.getAmmoCount(), 1, "One coupling spent");
            assertSavedState(test, mounted, true, 1);
            if (ModList.get().isLoaded("jade")) {
                DockingJadeTests.checkJadeData(test, LAUNCHER);
            }
            ServerPlayer player = testPlayer(test);
            Consumer<BlockEvent.BreakEvent> deny = event -> {
                if (event.getPos().equals(test.absolutePos(ANCHOR))) {
                    event.setCanceled(true);
                }
            };
            NeoForge.EVENT_BUS.addListener(deny);
            try {
                mounted.removeAnchor(player);
                test.assertTrue(mounted.isConnected(), "Canceled break preserves the rope");
                test.assertBlockPresent(anchor, ANCHOR);
            } finally {
                NeoForge.EVENT_BUS.unregister(deny);
            }
            mounted.removeAnchor(player);
            test.assertFalse(mounted.isConnected(), "Anchor removal disconnects");
            test.assertBlockNotPresent(anchor, ANCHOR);
            test.assertValueEqual(mounted.getAmmoCount(), 1, "Removal does not refund ammo");
            test.succeed();
        });
    }

    @GameTest(template = "docking_test", timeoutTicks = 70)
    public static void mountedBlockedCoupling(GameTestHelper test) {
        var mounted = placeLauncher(test);
        test.setBlock(6, 2, 5, Blocks.STONE);
        mounted.getAutomationInventory().insertItem(0, new ItemStack(coupling()), false);
        mounted.fire(null);
        test.runAfterDelay(12, () -> {
            test.assertFalse(mounted.isConnected(), "Stone is not an anchor");
            test.assertValueEqual(mounted.getAmmoCount(), 1, "Blocked shot keeps the coupling");
            test.assertBlockPresent(Blocks.AIR, ANCHOR);
            test.succeed();
        });
    }

    @GameTest(template = "docking_test", timeoutTicks = 80)
    public static void handheldCoupling(GameTestHelper test) {
        ServerPlayer player = handheldShooter(test);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(coupling(), 2));
        placeDock(test);
        BlockPos winchPos = new BlockPos(3, 2, 6);
        test.setBlock(winchPos, BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("simulated:rope_winch")));
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(test.absolutePos(ANCHOR)));
        test.assertTrue(player.getMainHandItem().getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "Handheld launches a coupling");
        test.runAfterDelay(12, () -> {
            var component = BuiltInRegistries.DATA_COMPONENT_TYPE.get(CreateSimulatedIds.ROPE_FIRST_CONNECTION_COMPONENT);
            int total = 0;
            for (int index = 0; index < player.getInventory().getContainerSize(); index++) {
                ItemStack stack = player.getInventory().getItem(index);
                if (stack.is(coupling())) {
                    total += stack.getCount();
                }
            }
            test.assertTrue(test.absolutePos(ANCHOR).equals(player.getOffhandItem().get(component)),
                    "The prepared coupling replaces the blank ammo stack in the off hand");
            test.assertValueEqual(total, 2, "A successful handheld shot converts exactly one coupling");
            var winch = test.absolutePos(winchPos);
            var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(winch), Direction.WEST, winch, false);
            var launcher = player.getMainHandItem();
            player.stopUsingItem();
            test.assertValueEqual(player.gameMode.useItemOn(player, test.getLevel(), launcher, InteractionHand.MAIN_HAND, hit),
                    net.minecraft.world.InteractionResult.PASS, "Winch allows the launcher hand to pass");
            test.assertValueEqual(launcher.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND).getResult(),
                    net.minecraft.world.InteractionResult.PASS, "Launcher passes while a prepared coupling is held");
            test.assertFalse(player.isUsingItem(), "Launcher does not start trying to fire");
            test.assertTrue(player.gameMode.useItemOn(player, test.getLevel(), player.getOffhandItem(),
                    InteractionHand.OFF_HAND, hit).consumesAction(), "Offhand prepared coupling attaches through native use");
            test.assertTrue(RopeItem.getRopeHolder(test.getLevel(), winch).isAttached()
                    && RopeItem.getRopeHolder(test.getLevel(), test.absolutePos(ANCHOR)).isAttached(),
                    "The regular winch and aimed connector are both attached");
            test.assertTrue(player.getOffhandItem().isEmpty(), "The prepared coupling was consumed once");
            test.succeed();
        });
    }

    @GameTest(template = "docking_test", timeoutTicks = 80)
    public static void movingMountedLauncher(GameTestHelper test) {
        placeLauncher(test);
        test.setBlock(LAUNCHER.below(), Blocks.STONE);
        placeDock(test);
        var base = test.absolutePos(LAUNCHER);
        var level = test.getLevel();
        var ship = SubLevelAssemblyHelper.assembleBlocks(level, base, List.of(base, base.above(), base.below()),
                new BoundingBox3i(base.below(), base.above()));
        var plotPos = ship.getPlot().getCenterBlock();
        var launcher = (MountedRopeLauncherBlockEntity) level.getBlockEntity(plotPos);
        test.assertTrue(launcher != null, "Launcher moved onto the ship");
        var physics = SubLevelContainer.getContainer(level).physicsSystem();
        physics.getPipeline().addLinearAndAngularVelocity(ship, new org.joml.Vector3d(0.25, 0, 0), new org.joml.Vector3d());
        test.runAfterDelay(2, () -> {
            var pivot = launcher.projectLocalPositionToWorld(Vec3.atBottomCenterOf(plotPos).add(0, 1, 0));
            var destination = Vec3.atCenterOf(test.absolutePos(ANCHOR));
            launcher.setAimFromWorldDirection(destination.subtract(pivot).normalize());
            launcher.getAutomationInventory().insertItem(0, new ItemStack(coupling()), false);
            launcher.handleRedstoneSignal(true, false);
        });
        test.runAfterDelay(10, () -> {
            test.assertTrue(launcher.isConnected(), "Launcher on moving ship should redock");
            launcher.handleRedstoneSignal(false, true);
            test.assertFalse(launcher.isConnected(), "Moving launcher should disconnect");
            test.assertBlockPresent(connector(), ANCHOR);
            test.succeed();
        });
    }

    @GameTest(template = "docking_test", timeoutTicks = 70)
    public static void handheldNewAnchor(GameTestHelper test) {
        ServerPlayer player = handheldShooter(test);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.THROWABLE_ROPE_CONNECTOR.get()));
        for (int z = 2; z <= 8; z++) {
            test.setBlock(8, 2, z, Blocks.STONE);
        }
        test.assertTrue(player.getMainHandItem().getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "Handheld launches a connector");
        test.runAfterDelay(12, () -> {
            var component = BuiltInRegistries.DATA_COMPONENT_TYPE.get(CreateSimulatedIds.ROPE_FIRST_CONNECTION_COMPONENT);
            ItemStack result = player.getOffhandItem();
            test.assertTrue(result.is(coupling()) && result.has(component), "Connector shot returns prepared coupling");
            test.succeed();
        });
    }

    @GameTest(template = "docking_test", timeoutTicks = 80)
    public static void handheldBlockedCoupling(GameTestHelper test) {
        ServerPlayer player = handheldShooter(test);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(coupling(), 2));
        placeDock(test);
        test.setBlock(5, 2, 5, Blocks.STONE);
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(test.absolutePos(ANCHOR)));
        player.getMainHandItem().getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.runAfterDelay(12, () -> {
            var component = BuiltInRegistries.DATA_COMPONENT_TYPE.get(CreateSimulatedIds.ROPE_FIRST_CONNECTION_COMPONENT);
            ItemStack ammo = player.getOffhandItem();
            test.assertTrue(ammo.is(coupling()) && ammo.getCount() == 2 && !ammo.has(component), "Blocked handheld shot preserves both blank couplings");
            test.succeed();
        });
    }

    @GameTest(template = "docking_test", timeoutTicks = 80)
    public static void riderAimsAtLowDock(GameTestHelper test) {
        var launcher = placeLauncher(test);
        placeFloorDock(test);
        test.setBlock(3, 2, 5, Blocks.STONE);
        fireFromRider(test, launcher, test.absoluteVec(new Vec3(2.5, 1.8, 5.5)));
        test.runAfterDelay(10, () -> {
            test.assertTrue(launcher.isConnected(), "A rider can see the floor dock over the terrain in front of the low barrel");
            test.assertValueEqual(launcher.getAmmoCount(), 0, "Successful rider shot uses one coupling");
            test.succeed();
        });
    }

    @GameTest(template = "docking_test", timeoutTicks = 80)
    public static void riderCannotShootThroughWall(GameTestHelper test) {
        var launcher = placeLauncher(test);
        placeFloorDock(test);
        test.setBlock(5, 2, 5, Blocks.STONE);
        test.setBlock(5, 3, 5, Blocks.STONE);
        fireFromRider(test, launcher, test.absoluteVec(new Vec3(2.5, 1.8, 5.5)));
        test.runAfterDelay(10, () -> {
            test.assertFalse(launcher.isConnected(), "A wall in the rider's sight line blocks the shot");
            test.assertValueEqual(launcher.getAmmoCount(), 1, "Blocked rider shot preserves the coupling");
            test.succeed();
        });
    }

    @GameTest(template = "docking_test", timeoutTicks = 80)
    public static void movingRiderAimsAtLowDock(GameTestHelper test) {
        placeLauncher(test);
        placeFloorDock(test);
        test.setBlock(LAUNCHER.below(), Blocks.STONE);
        var base = test.absolutePos(LAUNCHER);
        var level = test.getLevel();
        var ship = SubLevelAssemblyHelper.assembleBlocks(level, base, List.of(base, base.above(), base.below()),
                new BoundingBox3i(base.below(), base.above()));
        var plotPos = ship.getPlot().getCenterBlock();
        var launcher = (MountedRopeLauncherBlockEntity) level.getBlockEntity(plotPos);
        var physics = SubLevelContainer.getContainer(level).physicsSystem();
        physics.getPipeline().addLinearAndAngularVelocity(ship, new org.joml.Vector3d(0.25, 0, 0), new org.joml.Vector3d());
        test.runAfterDelay(2, () -> fireFromRider(test, launcher,
                launcher.projectLocalPositionToWorld(Vec3.atBottomCenterOf(plotPos).add(0, 0.8, 0))));
        test.runAfterDelay(12, () -> {
            test.assertTrue(launcher.isConnected(), "A rider on a moving ship connects to the targeted dock");
            test.assertValueEqual(launcher.getAmmoCount(), 0, "Moving rider consumes one coupling");
            test.succeed();
        });
    }

    private static void fireFromRider(GameTestHelper test, MountedRopeLauncherBlockEntity launcher, Vec3 feet) {
        var player = testPlayer(test);
        player.moveTo(feet.x, feet.y, feet.z, -90, 0);
        var position = test.absolutePos(ANCHOR);
        var state = test.getLevel().getBlockState(position);
        var center = Vec3.atLowerCornerOf(position).add(state.getCollisionShape(test.getLevel(), position).bounds().getCenter());
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, center);
        launcher.setAimFromWorldDirection(player.getLookAngle());
        launcher.getAutomationInventory().insertItem(0, new ItemStack(coupling()), false);
        launcher.fireFromRider(player);
    }

    private static void placeFloorDock(GameTestHelper test) {
        test.setBlock(ANCHOR.below(), Blocks.STONE);
        test.setBlock(ANCHOR, connector().defaultBlockState().setValue(RopeConnectorBlock.FACING, Direction.UP));
    }

    @GameTest(template = "docking_test", timeoutTicks = 80)
    public static void preparedCouplingAttachesToMountedTop(GameTestHelper test) {
        var launcher = placeLauncher(test);
        placeDock(test);
        var player = handheldShooter(test);
        var prepared = new ItemStack(coupling());
        net.sprocketgames.create_aeronautics_throwable_rope_connector.integration.CreateSimulatedIntegration
                .setFirstConnection(prepared, test.absolutePos(ANCHOR));
        player.setItemInHand(InteractionHand.OFF_HAND, prepared);
        var upper = test.absolutePos(LAUNCHER.above());
        var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(upper), Direction.WEST, upper, false);
        test.assertTrue(player.gameMode.useItemOn(player, test.getLevel(), player.getMainHandItem(),
                InteractionHand.MAIN_HAND, hit).consumesAction(), "The launcher uses the prepared coupling in the other hand");
        test.assertTrue(launcher.isConnected(), "The prepared coupling reaches the base winch from the upper half");
        test.assertTrue(player.getOffhandItem().isEmpty(), "One prepared coupling was consumed");
        test.assertTrue(player.getVehicle() == null, "Using a coupling does not mount the player");
        test.succeed();
    }

    @GameTest(template = "docking_test")
    public static void canceledHandheldPlacementPreservesAmmo(GameTestHelper test) {
        var player = handheldShooter(test);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.THROWABLE_ROPE_CONNECTOR.get()));
        var hit = placementHit(test);
        assertPlacementCanceled(test, player, () -> ThrowableRopeConnectorPlacement.placeConnectorAndGiveRope(
                test.getLevel(), player, hit, Inventory.SLOT_OFFHAND, true));
        test.assertTrue(player.getOffhandItem().is(ModItems.THROWABLE_ROPE_CONNECTOR.get()),
                "Canceled placement leaves the same ammo in the off hand");
        test.succeed();
    }

    @GameTest(template = "docking_test")
    public static void fullInventoryPreservesUnusedConnectors(GameTestHelper test) {
        var player = handheldShooter(test);
        var ammo = ModItems.THROWABLE_ROPE_CONNECTOR.get();
        for (int slot = 1; slot < Inventory.INVENTORY_SIZE; slot++) {
            player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ammo, 4));
        test.assertTrue(ThrowableRopeConnectorPlacement.placeConnectorAndGiveRope(
                test.getLevel(), player, placementHit(test), Inventory.SLOT_OFFHAND, true), "Connector places successfully");
        test.assertTrue(player.getOffhandItem().is(ammo), "Unused connectors stay in the off hand");
        test.assertValueEqual(player.getOffhandItem().getCount(), 3, "Exactly one connector was spent");
        var dropped = test.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(1));
        var component = BuiltInRegistries.DATA_COMPONENT_TYPE.get(CreateSimulatedIds.ROPE_FIRST_CONNECTION_COMPONENT);
        test.assertTrue(dropped.stream().map(ItemEntity::getItem).anyMatch(stack ->
                        stack.is(coupling()) && stack.getCount() == 1 && test.absolutePos(ANCHOR).equals(stack.get(component))),
                "The prepared coupling drops at the player when the inventory is full");
        test.succeed();
    }

    @GameTest(template = "docking_test")
    public static void canceledAutomatedPlacementRestoresBlock(GameTestHelper test) {
        var hit = placementHit(test);
        assertPlacementCanceled(test, null, () -> ThrowableRopeConnectorPlacement.placeConnectorFromMountedLauncher(
                test.getLevel(), hit, null).isPresent());
        test.succeed();
    }

    private static BlockHitResult placementHit(GameTestHelper test) {
        test.setBlock(ANCHOR.east(), Blocks.STONE);
        var support = test.absolutePos(ANCHOR.east());
        return new BlockHitResult(Vec3.atCenterOf(support).add(-0.5, 0, 0), Direction.WEST, support, false);
    }

    private static void assertPlacementCanceled(GameTestHelper test, ServerPlayer player, BooleanSupplier place) {
        var target = test.absolutePos(ANCHOR);
        var eventCount = new AtomicInteger();
        Consumer<BlockEvent.EntityPlaceEvent> deny = event -> {
            if (event.getLevel() == test.getLevel() && event.getPos().equals(target)) {
                test.assertTrue(player == null ? event.getEntity() instanceof net.neoforged.neoforge.common.util.FakePlayer
                        : event.getEntity() == player, "Placement event identifies the firing actor");
                eventCount.incrementAndGet();
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(deny);
        try {
            test.assertFalse(place.getAsBoolean(), "Canceled placement reports failure");
            test.assertValueEqual(eventCount.get(), 1, "Placement posts one NeoForge event");
            test.assertBlockPresent(Blocks.AIR, ANCHOR);
        } finally {
            NeoForge.EVENT_BUS.unregister(deny);
        }
    }

    private static ServerPlayer testPlayer(GameTestHelper test) {
        return new net.neoforged.neoforge.common.util.FakePlayer(test.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "docking-test"));
    }

    private static void assertSavedState(GameTestHelper test, MountedRopeLauncherBlockEntity mounted, boolean connected, int ammo) {
        var registries = test.getLevel().registryAccess();
        var saved = mounted.saveWithFullMetadata(registries);
        var restored = (MountedRopeLauncherBlockEntity) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                mounted.getBlockPos(), mounted.getBlockState(), saved, registries);
        test.assertTrue(restored != null, "Mounted launcher loads from NBT");
        test.assertValueEqual(restored.isConnected(), connected, "Rope connection serializes");
        test.assertValueEqual(restored.getAmmoCount(), ammo, "Ammo count serializes");
    }



    private static void placeDock(GameTestHelper test) {
        test.setBlock(8, 2, 5, Blocks.STONE);
        test.setBlock(ANCHOR, connector().defaultBlockState().setValue(RopeConnectorBlock.FACING, Direction.WEST));
    }

    private static ServerPlayer handheldShooter(GameTestHelper test) {
        ServerPlayer player = testPlayer(test);
        player.getAbilities().instabuild = false;
        var position = test.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 1, 5.5));
        player.moveTo(position.x, position.y, position.z, -90, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ROPE_CONNECTOR_LAUNCHER.get()));
        return player;
    }

    static MountedRopeLauncherBlockEntity placeLauncher(GameTestHelper test) {
        var state = ModBlocks.MOUNTED_ROPE_LAUNCHER.get().defaultBlockState()
                .setValue(MountedRopeLauncherBlock.FACING, Direction.EAST);
        test.setBlock(LAUNCHER, state);
        test.setBlock(LAUNCHER.above(), state.setValue(MountedRopeLauncherBlock.HALF, DoubleBlockHalf.UPPER));
        return test.getBlockEntity(LAUNCHER);
    }

    private static Item coupling() {
        return BuiltInRegistries.ITEM.get(CreateSimulatedIds.ROPE_ITEM);
    }

    private static Block connector() {
        return BuiltInRegistries.BLOCK.get(CreateSimulatedIds.ROPE_CONNECTOR_ITEM);
    }
}
