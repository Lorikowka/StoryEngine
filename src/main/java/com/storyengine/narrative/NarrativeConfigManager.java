package com.storyengine.narrative;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class NarrativeConfigManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static NarrativeConfig config;

    private NarrativeConfigManager() {
    }

    public static NarrativeConfig get() {
        if (config == null) {
            load();
        }
        return config;
    }

    private static Path getFile() {
        Path dir = FMLPaths.CONFIGDIR.get().resolve("story_engine");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Не удалось создать директорию config/story_engine/", e);
        }
        return dir.resolve("narrative_config.json");
    }

    public static void load() {
        Path file = getFile();
        NarrativeConfig loaded = null;

        if (Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                loaded = GSON.fromJson(reader, NarrativeConfig.class);
            } catch (IOException e) {
                LOGGER.error("[StoryEngine] Не удалось прочитать narrative_config.json", e);
            }
        }

        config = loaded != null ? loaded : new NarrativeConfig();
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(getFile(), StandardCharsets.UTF_8)) {
            GSON.toJson(get(), writer);
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Не удалось сохранить narrative_config.json", e);
        }
    }
}
