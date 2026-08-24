package com.storyengine.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.storyengine.StoryEngineMod;
import com.storyengine.client.MenuAssetsManager;
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
 * Загружает PNG-портреты NPC из config/story_engine/portraits/ напрямую с
 * диска (в обход ресурс-паков), регистрирует как DynamicTexture.
 * Fallback: сначала portraits/, затем heads/ (иконка), затем встроенный
 * default_head.png. Только клиентская сторона. Зеркало DynamicHeadManager.
 */
public final class DialoguePortraitManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Портрет по умолчанию. Берётся через MenuAssetsManager, поэтому при
     * включённой кастомизации подхватывается config/story_engine/menu/default_head.png.
     */
    public static ResourceLocation defaultPortrait() {
        return MenuAssetsManager.get("default_head");
    }

    private static final Map<String, ResourceLocation> LOADED = new HashMap<>();
    private static final Set<String> MISSING = new HashSet<>();

    private DialoguePortraitManager() {
    }

    /**
     * Возвращает ResourceLocation портрета. Если portraitId пустой/не найден -
     * пробует iconId (heads/), затем default_head.png. Результат кэшируется.
     */
    public static ResourceLocation getOrLoad(String portraitId, String iconId) {
        ResourceLocation direct = resolve(portraitId);
        if (direct != null) {
            return direct;
        }
        if (iconId != null && !iconId.isBlank() && !"none".equalsIgnoreCase(iconId)) {
            ResourceLocation icon = DynamicHeadManager.getOrLoad(iconId);
            if (icon != null && !icon.equals(DynamicHeadManager.defaultIcon())) {
                return icon;
            }
            if (icon != null) {
                return icon; // heads тоже может вернуть default - ок
            }
        }
        return defaultPortrait();
    }

    private static ResourceLocation resolve(String id) {
        if (id == null || id.isBlank() || "none".equalsIgnoreCase(id)) {
            return null;
        }
        String key = id.toLowerCase(Locale.ROOT);
        ResourceLocation cached = LOADED.get(key);
        if (cached != null) {
            return cached;
        }
        if (MISSING.contains(key)) {
            return null;
        }
        Path file = getPortraitsDirectory().resolve(key + ".png");
        if (!Files.isRegularFile(file)) {
            MISSING.add(key);
            return null;
        }
        try (InputStream stream = Files.newInputStream(file)) {
            NativeImage image = NativeImage.read(stream);
            DynamicTexture texture = new DynamicTexture(image);
            ResourceLocation location = new ResourceLocation(StoryEngineMod.MOD_ID, "dialogue_portraits/" + key);
            Minecraft.getInstance().getTextureManager().register(location, texture);
            LOADED.put(key, location);
            return location;
        } catch (IOException | RuntimeException e) {
            LOGGER.error("[StoryEngine] Не удалось загрузить портрет '{}' из {}", key, file, e);
            MISSING.add(key);
            return null;
        }
    }

    private static Path getPortraitsDirectory() {
        Path dir = FMLPaths.CONFIGDIR.get().resolve("story_engine").resolve("portraits");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Не удалось создать директорию портретов: {}", dir, e);
        }
        return dir;
    }

    /** Сброс кэша (после замены PNG на диске). */
    public static void clearCache() {
        LOADED.clear();
        MISSING.clear();
    }
}
