package net.sprocketgames.create_aeronautics_throwable_rope_connector.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.block.MountedRopeLauncherBlockEntity;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.config.ModCommonConfig;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.entity.MountedRopeLauncherSeatEntity;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.network.MountedRopeLauncherDismountPacket;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.network.MountedRopeLauncherFirePacket;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.network.MountedRopeLauncherReleasePacket;

public final class MountedRopeLauncherClientInput {
    private static boolean wasUseDown;
    private static boolean wasAttackDown;
    private static boolean wasShiftDown;

    private MountedRopeLauncherClientInput() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.player == null) {
            wasUseDown = false;
            wasAttackDown = false;
            wasShiftDown = false;
            return;
        }

        boolean useDown = minecraft.options.keyUse.isDown();
        boolean attackDown = minecraft.options.keyAttack.isDown();
        boolean shiftDown = minecraft.options.keyShift.isDown();
        if (minecraft.player.getVehicle() instanceof MountedRopeLauncherSeatEntity seat) {
            showMountedHud(minecraft, seat);
            if (shiftDown && !wasShiftDown) {
                PacketDistributor.sendToServer(new MountedRopeLauncherDismountPacket(seat.getId()));
                minecraft.player.stopRiding();
            }
            if (useDown && !wasUseDown) {
                PacketDistributor.sendToServer(new MountedRopeLauncherReleasePacket(seat.getId()));
            }
            if (attackDown && !wasAttackDown) {
                sendFire(seat);
            }
        }

        wasUseDown = useDown;
        wasAttackDown = attackDown;
        wasShiftDown = shiftDown;
    }

    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.player == null) {
            return;
        }

        if (!(minecraft.player.getVehicle() instanceof MountedRopeLauncherSeatEntity seat)) {
            return;
        }

        if (!event.isUseItem() && !event.isAttack()) {
            return;
        }

        event.setCanceled(true);
        event.setSwingHand(false);
        if (event.isUseItem()) {
            PacketDistributor.sendToServer(new MountedRopeLauncherReleasePacket(seat.getId()));
            return;
        }

        sendFire(seat);
    }

    private static void sendFire(MountedRopeLauncherSeatEntity seat) {
        PacketDistributor.sendToServer(new MountedRopeLauncherFirePacket(seat.getId()));
    }

    private static void showMountedHud(Minecraft minecraft, MountedRopeLauncherSeatEntity seat) {
        int ammoCount = 0;
        boolean connected = false;
        if (minecraft.level != null && minecraft.level.getBlockEntity(seat.getLauncherPos()) instanceof MountedRopeLauncherBlockEntity launcher) {
            ammoCount = launcher.getAmmoCount();
            connected = launcher.isConnected();
        }

        String translationKey = "message.create_aeronautics_throwable_rope_connector.mounted_controls";
        if (connected && !ModCommonConfig.canMountedLauncherRemoteRelease()) {
            translationKey = "message.create_aeronautics_throwable_rope_connector.mounted_controls_release_disabled";
        }

        minecraft.player.displayClientMessage(
                Component.translatable(translationKey, ammoCount),
                true
        );
    }
}
