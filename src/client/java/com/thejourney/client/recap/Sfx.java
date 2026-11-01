package com.thejourney.client.recap;

import com.thejourney.ModSounds;
import com.thejourney.client.config.JourneyConfig;
import com.thejourney.client.shots.CreditsMusic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * All recap sounds. Uses Minecraft's own sound events (no extra files), scaled by the
 * volume setting, and throttled so quick sequences never pile up into noise.
 */
public final class Sfx {
    public enum Kind {
        CLICK(120, true), TOGGLE(120, true), TAB(120, true), OPEN(200, true), CLOSE(200, true),
        STAT(60, false), SPARKLE(110, false), ACHIEVE(300, true), FINALE(1500, true),
        EXPORT(300, true), TITLE(500, true), WHOOSH(150, false);

        final long cooldown;
        final boolean essential;

        Kind(long cooldown, boolean essential) {
            this.cooldown = cooldown;
            this.essential = essential;
        }
    }

    private static final long[] LAST = new long[Kind.values().length];
    private static final long[] RECENT = new long[5];
    private static int recentIndex = 0;
    private static long lastAny = 0;
    private static int statStep = 0;

    private record Later(long at, Kind kind) {
    }

    private static final List<Later> QUEUE = new ArrayList<>();
    private static LoopingMusic music;

    private Sfx() {
    }

    public static void play(Kind kind) {
        play(kind, 1.0F);
    }

    public static void play(Kind kind, float pitchMul) {
        JourneyConfig config = JourneyConfig.get();
        float volume = config.soundVolume / 100.0F;
        if (!config.soundEffects || volume <= 0.0F) {
            return;
        }

        long now = Anim.now();
        if (now - lastAny < 35 || now - LAST[kind.ordinal()] < kind.cooldown) {
            return;
        }
        // At most 5 sounds per half second, unless it is an important one.
        if (!kind.essential && now - RECENT[recentIndex] < 500) {
            return;
        }

        LAST[kind.ordinal()] = now;
        lastAny = now;
        RECENT[recentIndex] = now;
        recentIndex = (recentIndex + 1) % RECENT.length;

        Minecraft client = Minecraft.getInstance();
        switch (kind) {
            case CLICK -> one(client, SoundEvents.UI_BUTTON_CLICK.value(), 1.25F * pitchMul, 0.5F * volume);
            case TOGGLE -> one(client, SoundEvents.UI_BUTTON_CLICK.value(), 0.9F * pitchMul, 0.55F * volume);
            case TAB -> one(client, SoundEvents.UI_LOOM_SELECT_PATTERN, 1.3F * pitchMul, 0.45F * volume);
            case OPEN -> one(client, SoundEvents.ENDER_CHEST_OPEN, 1.5F * pitchMul, 0.5F * volume);
            case CLOSE -> one(client, SoundEvents.ENDER_CHEST_CLOSE, 1.5F * pitchMul, 0.5F * volume);
            case STAT -> {
                // A rising pentatonic run, so a row of stats sounds like a little melody.
                float[] scale = {0.8F, 0.9F, 1.0F, 1.2F, 1.35F, 1.5F};
                one(client, SoundEvents.NOTE_BLOCK_CHIME.value(), scale[statStep++ % scale.length] * pitchMul, 0.45F * volume);
            }
            case ACHIEVE -> one(client, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F * pitchMul, 0.55F * volume);
            case SPARKLE -> one(client, SoundEvents.AMETHYST_BLOCK_CHIME, (1.0F + (statStep++ % 4) * 0.15F) * pitchMul, 0.5F * volume);
            case FINALE -> {
                one(client, SoundEvents.BEACON_ACTIVATE, 1.2F, 0.5F * volume);
                QUEUE.add(new Later(now + 350, Kind.ACHIEVE));
                QUEUE.add(new Later(now + 700, Kind.SPARKLE));
            }
            case EXPORT -> one(client, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.2F, 0.6F * volume);
            case TITLE -> {
                one(client, SoundEvents.END_PORTAL_FRAME_FILL, 0.8F, 0.55F * volume);
                QUEUE.add(new Later(now + 300, Kind.SPARKLE));
            }
            case WHOOSH -> one(client, SoundEvents.ENDERMAN_TELEPORT, 1.9F * pitchMul, 0.22F * volume);
        }
    }

    private static void one(Minecraft client, SoundEvent event, float pitch, float volume) {
        client.getSoundManager().play(SimpleSoundInstance.forUI(event, pitch, Math.max(0.0F, volume)));
    }

    /** Plays sounds that were scheduled a moment ahead (call once per frame). */
    public static void tick() {
        if (QUEUE.isEmpty()) {
            return;
        }
        long now = Anim.now();
        for (int i = QUEUE.size() - 1; i >= 0; i--) {
            Later later = QUEUE.get(i);
            if (later.at() <= now) {
                QUEUE.remove(i);
                lastAny = 0;
                LAST[later.kind().ordinal()] = 0;
                play(later.kind());
            }
        }
    }

    // ------------------------------------------------------------------ background music

    /** Loops music for the standalone recap screen. Safe to call repeatedly. */
    public static void startMusic() {
        if (!JourneyConfig.get().backgroundMusic) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (music != null && !music.isStopped() && client.getSoundManager().isActive(music)) {
            return;
        }

        SoundEvent event = CreditsMusic.available() && ModSounds.CREDITS_MUSIC != null
                ? ModSounds.CREDITS_MUSIC.value()
                : SoundEvents.MUSIC_CREDITS.value();
        music = new LoopingMusic(event, 0.5F * JourneyConfig.get().soundVolume / 100.0F);
        client.getSoundManager().play(music);
    }

    public static void stopMusic() {
        if (music != null) {
            music.fadeOut();
            music = null;
        }
    }

    public static void refreshMusicVolume() {
        if (music != null) {
            music.setTarget(0.5F * JourneyConfig.get().soundVolume / 100.0F);
        }
    }

    private static final class LoopingMusic extends AbstractSoundInstance implements TickableSoundInstance {
        private boolean stopped = false;
        private boolean fading = false;
        private float target;

        LoopingMusic(SoundEvent event, float target) {
            super(event, SoundSource.MUSIC, RandomSource.create());
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
            this.volume = 0.0F;
            this.target = target;
        }

        void setTarget(float value) {
            if (!fading) {
                target = value;
            }
        }

        void fadeOut() {
            fading = true;
            target = 0.0F;
        }

        @Override
        public boolean isStopped() {
            return stopped;
        }

        @Override
        public void tick() {
            volume += (target - volume) * 0.06F;
            if (fading && volume < 0.01F) {
                stopped = true;
            }
        }
    }
}
