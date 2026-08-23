package com.storyengine.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.storyengine.StoryEngineMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Читает .png файлы иконок NPC напрямую из config/story_engine/heads/,
 * минуя систему ресурс-паков, и регистрирует их как DynamicTexture в
 * TextureManager клиента. Только клиентская сторона.
 *
 * Использование: DynamicHeadManager.getOrLoad("old_man") -> ResourceLocation
 * зарегистрированной текстуры (или DEFAULT_ICON, если иконки нет/она "none"/не найдена).
 */
public final class DynamicHeadManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Текстура-заглушка по умолчанию из ресурсов мода. */
    public static final ResourceLocation DEFAULT_ICON =
            new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/default_head.png");

    private static final Map<String, ResourceLocation> LOADED = new HashMap<>();
    private static final Set<String> MISSING = new HashSet<>();

    private DynamicHeadManager() {
    }

    /**
     * Возвращает ResourceLocation зарегистрированной текстуры для указанного
     * iconId, либо DEFAULT_ICON, если иконка "none"/пустая, либо файл не найден/битый.
     * Результат (в т.ч. отрицательный) кэшируется, чтобы не читать диск
     * повторно на каждый кадр отрисовки.
     */
    public static ResourceLocation getOrLoad(String iconId) {
        if (iconId == null || iconId.isBlank() || "none".equalsIgnoreCase(iconId)) {
            return DEFAULT_ICON;
        }

        String key = iconId.toLowerCase(Locale.ROOT);

        ResourceLocation cached = LOADED.get(key);
        if (cached != null) {
            return cached;
        }
        if (MISSING.contains(key)) {
            return DEFAULT_ICON;
        }

        Path file = getHeadsDirectory().resolve(key + ".png");
        if (!Files.isRegularFile(file)) {
            LOGGER.warn("[StoryEngine] Иконка '{}' не найдена: {}", key, file);
            MISSING.add(key);
            return DEFAULT_ICON;
        }

        try (InputStream stream = Files.newInputStream(file)) {
            NativeImage image = NativeImage.read(stream);
            DynamicTexture texture = new DynamicTexture(image);
            ResourceLocation location = new ResourceLocation(StoryEngineMod.MOD_ID, "heads/" + key);
            Minecraft.getInstance().getTextureManager().register(location, texture);
            LOADED.put(key, location);
            return location;
        } catch (IOException | RuntimeException e) {
            LOGGER.error("[StoryEngine] Не удалось загрузить иконку '{}' из {}", key, file, e);
            MISSING.add(key);
            return DEFAULT_ICON;
        }
    }

    private static Path getHeadsDirectory() {
        Path dir = FMLPaths.CONFIGDIR.get().resolve("story_engine").resolve("heads");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Не удалось создать директорию иконок: {}", dir, e);
        }
        return dir;
    }

    /** Сбрасывает кэш - на случай, если иконки заменили на диске без перезапуска клиента. */
    public static void clearCache() {
        LOADED.clear();
        MISSING.clear();
    }
}