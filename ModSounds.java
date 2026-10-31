package com.thejourney;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
    /** Music that plays during the End Credits (see sounds.json and sounds/credits.ogg). */
    public static Holder<SoundEvent> CREDITS_MUSIC;

    private ModSounds() {
    }

    public static void register() {
        ResourceLocation id = TheJourney.id("credits_music");
        CREDITS_MUSIC = Registry.registerForHolder(
                BuiltInRegistries.SOUND_EVENT,
                id,
                SoundEvent.createVariableRangeEvent(id)
        );
    }
}
