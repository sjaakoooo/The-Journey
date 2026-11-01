package com.thejourney.client.recap;

import com.mojang.blaze3d.platform.NativeImage;
import com.thejourney.client.shots.EndShots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The pictures shown inside the recap: milestone photos, short moment clips and a
 * handful of End screenshots. Textures are loaded on demand (one per frame at most)
 * and a small LRU cache keeps memory low.
 */
public final class Gallery {
    /** {@code frames} is empty for a normal photo. */
    public record Item(String key, String caption, String tag, Path path, List<Path> frames) {
        public boolean clip() {
            return !frames.isEmpty();
        }
    }

    public record Tex(ResourceLocation id, int width, int height) {
    }

    private static final int MAX_END_SHOTS = 18;
    private static final int CACHE_SIZE = 8;

    private static List<Item> items = null;
    private static final Map<String, Path> PHOTOS = new java.util.HashMap<>();
    private static final Map<Path, Tex> CACHE = new LinkedHashMap<>(16, 0.75F, true);
    private static int counter = 0;
    private static long lastLoadFrame = -1;
    private static long frame = 0;

    private Gallery() {
    }

    public static void newFrame() {
        frame++;
    }

    public static List<Item> items() {
        if (items == null) {
            items = build();
        }
        return items;
    }

    private static List<Item> build() {
        List<Item> result = new ArrayList<>();
        List<EndShots.Entry> entries = EndShots.listForCredits();
        List<EndShots.Clip> clips = EndShots.listClips();

        int endShots = 0;
        for (EndShots.Entry entry : entries) {
            if (entry.caption() != null) {
                result.add(new Item(entry.path().toString(), entry.caption(), "Milestone photo",
                        entry.path(), List.of()));
            }
        }
        for (EndShots.Clip clip : clips) {
            result.add(new Item("clip_" + clip.id(), clip.caption(), "Moment clip",
                    clip.frames().get(0), clip.frames()));
        }
        for (EndShots.Entry entry : entries) {
            if (entry.caption() == null && endShots < MAX_END_SHOTS) {
                endShots++;
                result.add(new Item(entry.path().toString(), "Somewhere in the End", "From the End",
                        entry.path(), List.of()));
            }
        }
        return result;
    }

    /** Forgets the list so it is rebuilt (for example after a replay). */
    public static void refresh() {
        items = null;
        PHOTOS.clear();
    }

    /** Photo of a milestone (cached per recap session), or null. */
    public static Path photoFile(String milestoneId) {
        if (!PHOTOS.containsKey(milestoneId)) {
            PHOTOS.put(milestoneId, EndShots.milestoneFile(milestoneId));
        }
        return PHOTOS.get(milestoneId);
    }

    /** Returns a loaded texture, or null if it is not loaded yet (it is loaded on a later frame). */
    public static Tex texture(Path path) {
        if (path == null) {
            return null;
        }
        if (CACHE.containsKey(path)) {
            return CACHE.get(path); // null = known to be unreadable
        }
        if (lastLoadFrame == frame) {
            return null;
        }
        lastLoadFrame = frame;

        try (InputStream stream = Files.newInputStream(path)) {
            NativeImage image = NativeImage.read(stream);
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("the-journey", "gallery_" + (counter++));
            DynamicTexture texture = new DynamicTexture(image);
            texture.setFilter(true, false);
            Minecraft.getInstance().getTextureManager().register(id, texture);

            Tex tex = new Tex(id, image.getWidth(), image.getHeight());
            CACHE.put(path, tex);
            evict();
            return tex;
        } catch (Exception exception) {
            // Broken or deleted file: remember it as "no picture" so we do not retry every frame.
            CACHE.put(path, null);
            return null;
        }
    }

    private static void evict() {
        while (CACHE.size() > CACHE_SIZE) {
            Path eldest = CACHE.keySet().iterator().next();
            Tex tex = CACHE.remove(eldest);
            if (tex != null) {
                Minecraft.getInstance().getTextureManager().release(tex.id());
            }
        }
    }

    /** Frees every texture. Called when the recap or the credits close. */
    public static void release() {
        for (Tex tex : CACHE.values()) {
            if (tex != null) {
                Minecraft.getInstance().getTextureManager().release(tex.id());
            }
        }
        CACHE.clear();
        PHOTOS.clear();
        items = null;
    }
}
