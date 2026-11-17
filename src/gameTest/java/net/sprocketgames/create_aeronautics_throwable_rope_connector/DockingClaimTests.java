package net.sprocketgames.create_aeronautics_throwable_rope_connector;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.registry.ModItems;

import java.util.UUID;

@GameTestHolder(CreateAeronauticsThrowableRopeConnector.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DockingClaimTests {
    @GameTest(template = "docking_test", batch = "placement_claims", timeoutTicks = 100)
    public static void claimBlocksAutomatedPlacement(GameTestHelper test) throws CommandSyntaxException {
        var launcher = DockingGameTests.placeLauncher(test);
        var anchor = DockingGameTests.ANCHOR;
        for (int y = 1; y <= 4; y++) {
            test.setBlock(anchor.getX() + 1, y, anchor.getZ(), Blocks.STONE);
        }
        var level = test.getLevel();
        var source = level.getServer().createCommandSourceStack();
        var team = FTBTeamsAPI.api().getManager().createServerTeam(
                source, "Dock" + UUID.randomUUID().toString().substring(0, 8), "Test claim", Color4I.WHITE);
        var claim = new ChunkDimPos(level, test.absolutePos(anchor));
        var chunks = FTBChunksAPI.api().getManager().getOrCreateData(team);
        test.assertTrue(chunks.claim(source, claim, false).isSuccess(), "The destination is claimed");
        launcher.getAutomationInventory().insertItem(0, new ItemStack(ModItems.THROWABLE_ROPE_CONNECTOR.get()), false);
        launcher.fire(null);
        test.runAfterDelay(12, () -> {
            try {
                test.assertBlockPresent(Blocks.AIR, anchor);
                test.assertFalse(launcher.isConnected(), "Automation cannot place into another team's claim");
                test.assertValueEqual(launcher.getAmmoCount(), 1, "The denied shot preserves its ammo");
            } finally {
                chunks.unclaim(source, claim, false);
            }
            test.succeed();
        });
    }
}
