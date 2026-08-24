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
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Читает .png GUI-текстур мода напрямую из config/story_engine/menu/,
 * минуя систему ресурс-паков, и регистрирует их как DynamicTexture в
 * TextureManager клиента (зеркало DynamicHeadManager для GUI).
 *
 * Покрывает ВСЕ gui-текстуры мода (рамка меню, виджеты, иконки статусов,
 * default_head и т.п.), чтобы их можно было кастомизировать, подменив файл
 * в config/story_engine/menu/.
 *
 * Только клиентская сторона для метода get() (регистрация текстуры).
 * Методы копирования шаблонов (copyDefaultsIfMissing/resetDefaults) и clearCache
 * НЕ обращаются к классам клиента, поэтому безопасны при вызове с сервера
 * (например, из команды /storymenu), даже на выделенном сервере.
 *
 * Использование: MenuAssetsManager.get("quest_menu") -> ResourceLocation
 * зарегистрированной кастомной текстуры, либо встроенной, если файла нет/
 * кастомизация выключена/файл битый.
 */
public final class MenuAssetsManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Базовые имена файлов (без .png) -> встроенный ResourceLocation из ресурсов мода. */
    private static final Map<String, ResourceLocation> DEFAULTS = new HashMap<>();

    static {
        DEFAULTS.put("quest_menu", new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/quest_menu.png"));
        DEFAULTS.put("quest_widgets", new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/quest_widgets.png"));
        DEFAULTS.put("quest_icon", new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/quest_icon.png"));
        DEFAULTS.put("status_active", new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/status_active.png"));
        DEFAULTS.put("status_completed", new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/status_completed.png"));
        DEFAULTS.put("status_failed", new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/status_failed.png"));
        // Иконка по умолчанию для голов/портретов NPC - тоже кастомизируемая.
        DEFAULTS.put("default_head", new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/default_head.png"));
        // Текстуры шапки и подвала окна истории сюжетного чата (кастомизируемые бары).
        DEFAULTS.put("narrative_header", new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/narrative_header.png"));
        DEFAULTS.put("narrative_footer", new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/narrative_footer.png"));
    }

    private static final Map<String, ResourceLocation> LOADED = new HashMap<>();
    private static final Set<String> MISSING = new HashSet<>();

    private MenuAssetsManager() {
    }

    /**
     * Возвращает ResourceLocation текстуры для указанного assetId.
     * - если кастомизация выключена (MenuCustomizationConfig.enabled() == false) -> встроенная;
     * - если в config/story_engine/menu/&lt;id&gt;.png есть валидный файл -> зарегистрированная DynamicTexture;
     * - иначе (файла нет/битый) -> встроенная (результат кэшируется в MISSING).
     */
    public static ResourceLocation get(String assetId) {
        ResourceLocation fallback = DEFAULTS.get(assetId);
        if (fallback == null) {
            LOGGER.warn("[StoryEngine] Неизвестный asset меню: {}", assetId);
            return fallback;
        }
        if (!MenuCustomizationConfig.enabled()) {
            return fallback;
        }

        ResourceLocation cached = LOADED.get(assetId);
        if (cached != null) {
            return cached;
        }
        if (MISSING.contains(assetId)) {
            return fallback;
        }

        Path file = getMenuDirectory().resolve(assetId + ".png");
        if (!Files.isRegularFile(file)) {
            MISSING.add(assetId);
            return fallback;
        }

        try (InputStream stream = Files.newInputStream(file)) {
            NativeImage image = NativeImage.read(stream);
            DynamicTexture texture = new DynamicTexture(image);
            ResourceLocation location = new ResourceLocation(StoryEngineMod.MOD_ID, "menu_custom/" + assetId);
            Minecraft.getInstance().getTextureManager().register(location, texture);
            LOADED.put(assetId, location);
            return location;
        } catch (IOException | RuntimeException e) {
            LOGGER.error("[StoryEngine] Не удалось загрузить текстуру меню '{}' из {}", assetId, file, e);
            MISSING.add(assetId);
            return fallback;
        }
    }

    /** Папка config/story_engine/menu/ (создаётся при необходимости). */
    public static Path getMenuDirectory() {
        Path dir = FMLPaths.CONFIGDIR.get().resolve("story_engine").resolve("menu");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Не удалось создать директорию текстур меню: {}", dir, e);
        }
        return dir;
    }

    /** Копирует исходные PNG из jar в папку, только если файла там ещё нет. */
    public static void copyDefaultsIfMissing() {
        for (String id : DEFAULTS.keySet()) {
            Path target = getMenuDirectory().resolve(id + ".png");
            if (!Files.exists(target)) {
                copyFromJar(id + ".png", target);
            }
        }
        LOGGER.info("[StoryEngine] Шаблоны текстур меню готовы в {}", getMenuDirectory());
    }

    /** Перезаписывает все PNG в папке исходниками из jar и сбрасывает кэш загруженных текстур. */
    public static void resetDefaults() {
        clearCache();
        for (String id : DEFAULTS.keySet()) {
            Path target = getMenuDirectory().resolve(id + ".png");
            copyFromJar(id + ".png", target);
        }
        LOGGER.info("[StoryEngine] Текстуры меню сброшены к исходным в {}", getMenuDirectory());
    }

    /** Сбрасывает кэш - чтобы изменения на диске/в конфиге подхватились без перезапуска клиента. */
    public static void clearCache() {
        LOADED.clear();
        MISSING.clear();
    }

    private static void copyFromJar(String name, Path target) {
        String resource = "/assets/" + StoryEngineMod.MOD_ID + "/textures/gui/" + name;
        try (InputStream in = StoryEngineMod.class.getResourceAsStream(resource)) {
            if (in == null) {
                LOGGER.warn("[StoryEngine] Исходная текстура не найдена в jar: {}", resource);
                return;
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Не удалось скопировать исходную текстуру '{}' в {}", resource, target, e);
        }
    }
}
