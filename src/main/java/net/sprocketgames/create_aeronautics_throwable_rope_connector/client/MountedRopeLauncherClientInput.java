package net.sprocketgames.create_aeronautics_throwable_rope_connector.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.block.MountedRopeLauncherBlockEntity;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.entity.MountedRopeLauncherSeatEntity;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.network.MountedRopeLauncherDismountPacket;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.network.MountedRopeLauncherFirePacket;
import net.sprocketgames.create_aeronautics_throwable_rope_connector.network.MountedRopeLauncherReleasePacket;
import org.lwjgl.glfw.GLFW;

public final class MountedRopeLauncherClientInput {
    private static boolean wasSneakDown;
    private static MountedRopeLauncherSeatEntity previousSeat;
    private static CustomPacketPayload pendingAction;

    private MountedRopeLauncherClientInput() {
    }

    public static void onClientTick(ClientTickEvent.Pre event) {
        pendingAction = null;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.getOverlay() != null || minecraft.player == null) {
            wasSneakDown = false;
            previousSeat = null;
            return;
        }

        boolean sneakDown = minecraft.options.keyShift.isDown();
        if (minecraft.player.getVehicle() instanceof MountedRopeLauncherSeatEntity seat) {
            // Read queued clicks before vanilla consumes them, including taps released between ticks.
            boolean usePressed = minecraft.options.keyUse.consumeClick();
            boolean attackPressed = minecraft.options.keyAttack.consumeClick();
            if (previousSeat != seat) {
                showMountedHud(minecraft, seat);
            }
            previousSeat = seat;
            if (sneakDown && !wasSneakDown) {
                PacketDistributor.sendToServer(new MountedRopeLauncherDismountPacket(seat.getId()));
                minecraft.player.stopRiding();
            } else if (usePressed) {
                pendingAction = new MountedRopeLauncherReleasePacket(seat.getId(), isRemovalModifierHeld(minecraft));
            } else if (attackPressed) {
                pendingAction = new MountedRopeLauncherFirePacket(seat.getId());
            }
        } else {
            previousSeat = null;
        }

        wasSneakDown = sneakDown;
    }

    private static boolean isRemovalModifierHeld(Minecraft minecraft) {
        var options = minecraft.options;
        KeyMapping sprint = options.keySprint;
        InputConstants.Key key = sprint.getKey();
        if (sprint.isUnbound() || key.equals(options.keyShift.getKey()) || key.equals(options.keyUse.getKey())
                || key.equals(options.keyAttack.getKey()) || !sprint.getKeyModifier().isActive(sprint.getKeyConflictContext())) {
            return false;
        }

        // Read the physical input so toggle sprint cannot arm anchor removal.
        long window = minecraft.getWindow().getWindow();
        return switch (key.getType()) {
            case KEYSYM -> InputConstants.isKeyDown(window, key.getValue());
            case MOUSE -> GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
            case SCANCODE -> false;
        };
    }

    public static void sendPendingAction() {
        CustomPacketPayload action = pendingAction;
        pendingAction = null;
        if (action != null && Minecraft.getInstance().getConnection() != null) {
            // The player sends the latest aim to the server during the client tick.
            PacketDistributor.sendToServer(action);
        }
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
    }

    private static void showMountedHud(Minecraft minecraft, MountedRopeLauncherSeatEntity seat) {
        int ammoCount = 0;
        if (minecraft.level != null && minecraft.level.getBlockEntity(seat.getLauncherPos()) instanceof MountedRopeLauncherBlockEntity launcher) {
            ammoCount = launcher.getAmmoCount();
        }

        String translationKey = "message.create_aeronautics_throwable_rope_connector.mounted_controls";
        minecraft.player.displayClientMessage(
                Component.translatable(translationKey, Component.keybind("key.attack"), Component.keybind("key.use"),
                        Component.keybind("key.sprint"), Component.keybind("key.sneak"), ammoCount),
                true
        );
    }
}
