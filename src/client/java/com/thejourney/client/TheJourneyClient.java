package com.thejourney.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.WinScreen;
import com.thejourney.client.config.JourneyConfig;
import com.thejourney.client.shots.EndShots;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;

import com.thejourney.JourneyRecapPayload;
import com.thejourney.MilestonePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class TheJourneyClient implements ClientModInitializer {
    private KeyMapping openRecapKey;

    private void openRecap(Minecraft client, Screen parent) {
        JourneyScreen.setRecapText("Loading the latest recap...");
        ClientPlayNetworking.send(new JourneyRecapPayload("request"));
        client.setScreen(new JourneyScreen(parent));
    }

    /**
     * Puts the recap button right next to the "Options" row, no matter how
     * Minecraft lays out the pause menu. If that fails, the old position is used.
     */
    private static int[] findPauseButtonPosition(Screen screen, int width, int height) {
        String optionsLabel = Component.translatable("menu.options").getString();
        AbstractWidget options = null;

        for (AbstractWidget widget : Screens.getButtons(screen)) {
            if (widget.getMessage().getString().equals(optionsLabel)) {
                options = widget;
                break;
            }
        }

        if (options == null) {
            return new int[]{width / 2 + 104, height / 4 + 72};
        }

        int rowY = options.getY();
        int rowRight = options.getX() + options.getWidth();

        for (AbstractWidget widget : Screens.getButtons(screen)) {
            if (widget.getY() == rowY) {
                rowRight = Math.max(rowRight, widget.getX() + widget.getWidth());
            }
        }

        return new int[]{rowRight + 4, rowY};
    }

    @Override
    public void onInitializeClient() {
        JourneyConfig.load();
        EndShots.init();
        com.thejourney.client.recap.SessionLog.init();
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof WinScreen) {
                // Wheel: scrolls the recap menu when the mouse is over it, otherwise the credits as usual.
                ScreenMouseEvents.allowMouseScroll(screen).register(
                        (currentScreen, mouseX, mouseY, horizontalAmount, verticalAmount) ->
                                !(JourneyConfig.get().showRecapInCredits
                                        && CreditsMenu.mouseScrolled(mouseX, mouseY, verticalAmount))
                );

                // Clicks on the arrow tab or inside the recap menu.
                ScreenMouseEvents.allowMouseClick(screen).register(
                        (currentScreen, mouseX, mouseY, button) ->
                                !(JourneyConfig.get().showRecapInCredits
                                        && CreditsMenu.mouseClicked(mouseX, mouseY, button))
                );
            }
            if (screen instanceof PauseScreen) {
                int[] position = findPauseButtonPosition(screen, width, height);

                Button recapButton = Button.builder(
                        Component.literal("R"),
                        button -> openRecap(client, screen)
                )
                        .bounds(position[0], position[1], 20, 20)
                        .tooltip(Tooltip.create(Component.literal("Open The Journey recap")))
                        .build();
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(
                JourneyRecapPayload.TYPE,
                (payload, context) -> JourneyScreen.setRecapText(payload.text())
        );

        // The server tells us when a big moment happens, so we can take a photo of it.
        ClientPlayNetworking.registerGlobalReceiver(
                MilestonePayload.TYPE,
                (payload, context) -> EndShots.requestMilestone(payload.id())
        );

        openRecapKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                        "key.the-journey.open_recap",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_J,
                        "category.the-journey"
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openRecapKey.consumeClick() && client.player != null) {
                JourneyScreen.setRecapText("Loading the latest recap...");
                ClientPlayNetworking.send(new JourneyRecapPayload("request"));
                client.setScreen(new JourneyScreen());
            }
        });
    }
}