package com.thejourney.client.mixin;

import com.thejourney.JourneyRecapPayload;
import com.thejourney.client.CreditsMenu;
import com.thejourney.client.JourneyScreen;
import com.thejourney.client.recap.Gallery;
import com.thejourney.client.shots.CreditsMusic;
import com.thejourney.client.shots.EndShots;
import com.thejourney.client.shots.ShotSlideshow;
import com.thejourney.client.config.JourneyConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Music;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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

    @Shadow
    private float scroll;

    @Shadow
    private int totalScrollLength;

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
     * As soon as the credits are really finished (or aborted): free the pictures, and delete them unless the player keeps them.
     */
    @Inject(method = "onClose", at = @At("HEAD"))
    private void theJourney$deleteShots(CallbackInfo info) {
        ShotSlideshow.release();
        Gallery.release();
        CreditsMenu.reset();
        if (JourneyConfig.get().keepPhotos) {
            // Milestone photos and clips stay for the recap; only the bulk background shots go.
            EndShots.deleteEndShots();
        } else {
            EndShots.deleteEverythingForWorld();
        }
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
            CreditsMenu.attach(this);
            JourneyScreen.setRecapText("Loading the latest recap...");
            ClientPlayNetworking.send(new JourneyRecapPayload("request"));
        }
    }

    /*
     * The recap menu and its arrow tab are drawn on top of the credits.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void theJourney$renderRecap(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo info
    ) {
        Minecraft client = Minecraft.getInstance();
        if (!CreditsMenu.holdCredits()) {
            ShotSlideshow.renderCaption(graphics,
                    client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
        }

        if (!JourneyConfig.get().showRecapInCredits) {
            return;
        }

        CreditsMenu.render(graphics, mouseX, mouseY);
    }

    /*
     * While the recap menu is open, the credits wait at their very end instead of closing,
     * so nobody is thrown out of the recap halfway through reading it.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void theJourney$holdCreditsAtEnd(CallbackInfo info) {
        if (!JourneyConfig.get().showRecapInCredits || !CreditsMenu.holdCredits()) {
            return;
        }

        float limit = totalScrollLength + ((Screen) (Object) this).height * 2 + 24 - 4;
        if (scroll > limit) {
            scroll = limit;
        }
    }

    /*
     * The Journey logo takes the place of the vanilla Minecraft logo and scrolls with the
     * credits, always centred above them. Without the recap the normal logo is drawn as usual.
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
            return;
        }

        int logoWidth = Math.min(240, Math.max(120, originalWidth - 40));
        int logoHeight = logoWidth / 4;
        int centerX = originalWidth / 2 + CreditsMenu.creditsShift(originalWidth);

        RenderSystem.enableBlend();
        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        graphics.blit(
                THE_JOURNEY_LOGO,
                centerX - logoWidth / 2,
                y + 4,
                logoWidth,
                logoHeight,
                0.0F, 0.0F, 1024, 256, 1024, 256
        );
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
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
        return originalX + CreditsMenu.creditsShift(screenWidth);
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
        return originalX + CreditsMenu.creditsShift(screenWidth);
    }
}
