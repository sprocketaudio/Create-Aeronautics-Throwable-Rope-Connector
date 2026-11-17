package net.sprocketgames.create_aeronautics_throwable_rope_connector.integration;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.Protection;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.CommonHooks;

public final class AnchorProtection {
    private AnchorProtection() {
    }

    public static boolean canPlace(ServerLevel level, BlockPos pos, ServerPlayer player) {
        BlockPos worldPos = projectToWorld(level, pos);
        return player.level() == level && !player.isSpectator() && level.mayInteract(player, worldPos)
                && (!ModList.get().isLoaded("ftbchunks")
                    || (!isClaimProtected(player, pos) && (worldPos.equals(pos) || !isClaimProtected(player, worldPos))));
    }

    public static boolean canRemove(ServerLevel level, BlockPos pos, ServerPlayer player) {
        if (!level.hasChunkAt(pos) || !CreateSimulatedIntegration.isRopeConnector(level, pos)) {
            return false;
        }
        BlockPos worldPos = projectToWorld(level, pos);
        if (player.isSpectator() || !level.mayInteract(player, worldPos)) {
            return false;
        }
        if (CommonHooks.fireBlockBreak(level, player.gameMode.getGameModeForPlayer(), player, pos, level.getBlockState(pos)).isCanceled()) {
            return false;
        }
        return canEditProjectedClaim(player, pos, worldPos);
    }

    public static BlockPos projectToWorld(ServerLevel level, BlockPos pos) {
        return BlockPos.containing(Sable.HELPER.projectOutOfSubLevel(level, Vec3.atCenterOf(pos)));
    }

    private static boolean canEditProjectedClaim(ServerPlayer player, BlockPos pos, BlockPos worldPos) {
        return worldPos.equals(pos) || !ModList.get().isLoaded("ftbchunks") || !isClaimProtected(player, worldPos);
    }

    private static boolean isClaimProtected(ServerPlayer player, BlockPos worldPos) {
        var api = FTBChunksAPI.api();
        return api.isManagerLoaded() && api.getManager().shouldPreventInteraction(
                player, InteractionHand.MAIN_HAND, worldPos, Protection.EDIT_BLOCK, null);
    }
}
