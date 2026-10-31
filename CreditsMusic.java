package com.thejourney.client.shots;

import com.thejourney.ModSounds;
import com.thejourney.client.config.JourneyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Music;

/**
 * Optional custom music for the End Credits.
 * Put an OGG Vorbis file at: src/main/resources/assets/the-journey/sounds/credits.ogg
 * If the file is missing (or the option is off) the normal credits music keeps playing.
 */
public final class CreditsMusic {
    private static final ResourceLocation FILE =
            ResourceLocation.fromNamespaceAndPath("the-journey", "sounds/credits.ogg");

    private CreditsMusic() {
    }

    public static boolean available() {
        return JourneyConfig.get().customCreditsMusic
                && ModSounds.CREDITS_MUSIC != null
                && Minecraft.getInstance().getResourceManager().getResource(FILE).isPresent();
    }

    public static Music music() {
        return new Music(ModSounds.CREDITS_MUSIC, 0, 0, true);
    }
}
