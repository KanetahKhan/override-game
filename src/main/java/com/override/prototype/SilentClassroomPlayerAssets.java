package com.override.prototype;

import javafx.scene.image.Image;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads and caches Ayan's directional sprites from
 * {@code /Assets_Characters/Ayan/}. Each pose ships one image per
 * 8-way compass direction (see {@link SilentClassroomGeometry#compassDirection8}).
 */
final class SilentClassroomPlayerAssets {

    private static final Map<String, Image> cache = new HashMap<>();

    private SilentClassroomPlayerAssets() {
    }

    static Image idle(String direction) {
        return load("Full_body_portrait_of_a", direction);
    }

    static Image walking(String direction) {
        return load("walking", direction);
    }

    private static Image load(String pose, String direction) {
        String path = "/Assets_Characters/Ayan/" + pose + "/rotations/" + direction + ".png";
        return cache.computeIfAbsent(path, p -> {
            InputStream stream = SilentClassroomPlayerAssets.class.getResourceAsStream(p);
            if (stream == null) {
                throw new RuntimeException("Missing asset: " + p);
            }
            return new Image(stream);
        });
    }
}
