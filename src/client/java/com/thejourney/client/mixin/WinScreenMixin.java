package com.thejourney.client.mixin;

import com.thejourney.JourneyRecapPayload;
import com.thejourney.client.CreditsLayout;
import com.thejourney.client.JourneyScreen;
import com.thejourney.client.shots.CreditsMusic;
import com.thejourney.client.shots.EndShots;
import com.thejourney.client.shots.ShotSlideshow;
import com.thejourney.client.config.JourneyConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Music;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WinScreen.class)
public abstract class WinScreenMixin {
    private static final ResourceLocation THE_JOURNEY_LOGO =
            ResourceLocation.fromNamespaceAndPath(
                    "the-journey",
                    "textures/gui/journey_credits_logo.png"
            );

    /*
     * Replace the default credits background with the screenshots taken in the End.
     * If there are no screenshots, the normal background keeps working.
     */
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void theJourney$renderShotBackground(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo info
    ) {
        Minecraft client = Minecraft.getInstance();

        if (ShotSlideshow.render(
                graphics,
                client.getWindow().getGuiScaledWidth(),
                client.getWindow().getGuiScaledHeight()
        )) {
            info.cancel();
        }
    }

    /*
     * As soon as the credits are really finished (or aborted): delete all screenshots.
     */
    @Inject(method = "onClose", at = @At("HEAD"))
    private void theJourney$deleteShots(CallbackInfo info) {
        ShotSlideshow.release();
        EndShots.deleteEverythingForWorld();
    }

    /*
     * Your own credits music, if sounds/credits.ogg exists and the option is on.
     * Otherwise Minecraft's normal credits music keeps playing.
     */
    @Inject(method = "getBackgroundMusic", at = @At("HEAD"), cancellable = true)
    private void theJourney$creditsMusic(CallbackInfoReturnable<Music> info) {
        if (CreditsMusic.available()) {
            info.setReturnValue(CreditsMusic.music());
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void theJourney$requestRecap(CallbackInfo info) {
        Minecraft client = Minecraft.getInstance();

        if (client.player != null && JourneyConfig.get().showRecapInCredits) {
            JourneyScreen.setRecapText("Loading the latest recap...");
            ClientPlayNetworking.send(new JourneyRecapPayload("request"));
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void theJourney$renderRecap(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo info
    ) {
        if (!JourneyConfig.get().showRecapInCredits) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        CreditsLayout.Layout layout = CreditsLayout.compute(screenWidth, screenHeight);

        // Logo and recap panel share the same center (the left column).
        graphics.blit(
                THE_JOURNEY_LOGO,
                layout.logoX(),
                layout.logoY(),
                layout.logoWidth(),
                layout.logoHeight(),
                0.0F,
                0.0F,
                1024,
                256,
                1024,
                256
        );

        JourneyScreen.renderCreditCards(
                graphics,
                layout.panelX(),
                layout.panelY(),
                layout.panelWidth(),
                layout.panelHeight()
        );

        // Thin divider between recap and credits, fading out at both ends.
        int themeRgb = JourneyConfig.get().theme.palette().border() & 0x00FFFFFF;
        int dividerTop = 10;
        int dividerBottom = screenHeight - 10;
        int dividerMid = (dividerTop + dividerBottom) / 2;
        graphics.fillGradient(layout.dividerX(), dividerTop, layout.dividerX() + 1, dividerMid,
                0x00000000 | themeRgb, 0xAA000000 | themeRgb);
        graphics.fillGradient(layout.dividerX(), dividerMid, layout.dividerX() + 1, dividerBottom,
                0xAA000000 | themeRgb, 0x00000000 | themeRgb);
    }

    /*
     * Hide the vanilla Minecraft logo only while our recap is on screen.
     * Without the recap the normal logo is drawn as usual.
     */
    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/LogoRenderer;renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IFI)V"
            )
    )
    private void theJourney$replaceVanillaLogo(
            LogoRenderer logoRenderer,
            GuiGraphics graphics,
            int originalWidth,
            float alpha,
            int y
    ) {
        if (!JourneyConfig.get().showRecapInCredits) {
            logoRenderer.renderLogo(graphics, originalWidth, alpha, y);
        }
    }

    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V"
            ),
            index = 2
    )
    private int theJourney$moveCenteredCreditsRight(int originalX) {
        if (!JourneyConfig.get().showRecapInCredits) {
            return originalX;
        }

        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        return originalX + CreditsLayout.creditsShift(screenWidth);
    }

    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I"
            ),
            index = 2
    )
    private int theJourney$moveLeftAlignedCreditsRight(int originalX) {
        if (!JourneyConfig.get().showRecapInCredits) {
            return originalX;
        }

        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        return originalX + CreditsLayout.creditsShift(screenWidth);
    }
}