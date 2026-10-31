package com.thejourney.client.mixin;

import com.thejourney.client.JourneyScreen;
import com.thejourney.client.config.JourneyConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(
            method = "mouseScrolled",
            at = @At("HEAD"),
            cancellable = true
    )
    private void theJourney$scrollCredits(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount,
            CallbackInfoReturnable<Boolean> info
    ) {
        if (!((Object) this instanceof WinScreen)
                || !JourneyConfig.get().showRecapInCredits) {
            return;
        }

        JourneyScreen.scrollCreditCards(verticalAmount);

        info.setReturnValue(true);
    }
}