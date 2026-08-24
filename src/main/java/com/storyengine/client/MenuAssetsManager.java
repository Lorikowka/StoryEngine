package com.storyengine.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.storyengine.StoryEngineMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Читает .png GUI-текстур мода напрямую из config/story_engine/menu/,
 * минуя систему ресурс-паков, и регистрирует их как DynamicTexture в
 * TextureManager клиента (зеркало DynamicHeadManager для GUI).
 *
 * Покрывает ВСЕ gui-текстуры мода:
 *  - индивидуальные: quest_menu, quest_widgets, default_head (каждый - свой файл);
 *  - атлас gui_atlas.png: status_active, status_completed, status_failed,
 *    quest_icon, narrative_header, narrative_footer (упакованы в одну текстуру
 *    с фиксированной раскладкой, см. ATLAS_REGIONS).
 *
 * Чтобы кастомизировать атласные иконки, достаточно заменить ОДИН файл
 * config/story_engine/menu/gui_atlas.png (шаблон создаётся автоматически).
 * default_head намеренно НЕ входит в атлас (он ближе к heads/ как fallback NPC).
 *
 * Только клиентская сторона для метода get() (регистрация текстуры).
 * Методы копирования шаблонов (copyDefaultsIfMissing/resetDefaults) и clearCache
 * НЕ обращаются к классам клиента на сервере (атласные шаблоны пишутся только
 * в клиентском дисте), поэтому безопасны при вызове с сервера
 * (например, из команды /storymenu), даже на выделенном сервере.
 *
 * Использование для индивидуальных ассетов:
 *   MenuAssetsManager.get("quest_menu") -> ResourceLocation зарегистрированной
 *   кастомной текстуры, либо встроенной, если файла нет/кастомизация выключена.
 * Для атласных ассетов get() возвращает локацию атласа, а регион (uv) берётся
 * через MenuAssetsManager.getRegion(id) + константы ATLAS_W/ATLAS_H.
 */
public final class MenuAssetsManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Базовые имена файлов (без .png) -> встроенный ResourceLocation из ресурсов мода. */
    private static final Map<String, ResourceLocation> DEFAULTS = new HashMap<>();

    static {
        DEFAULTS.put("quest_menu", new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/quest_menu.png"));
        DEFAULTS.put("quest_widgets", new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/quest_widgets.png"));
        // Иконка по умолчанию для голов/портретов NPC - индивидуальная (вне атласа).
        DEFAULTS.put("default_head", new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/default_head.png"));
        // Панель меню интерактивного взаимодействия (левый нижний угол).
        DEFAULTS.put("interaction_menu", new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/interaction_menu.png"));
    }

    /** Размеры атласа gui_atlas.png (фиксированная раскладка, см. ATLAS_REGIONS). */
    public static final int ATLAS_W = 360;
    public static final int ATLAS_H = 124;

    /**
     * Фиксированная раскладка атласа: id -> {u, v, w, h} в пикселях gui_atlas.png.
     * Верхний ряд - квадратные иконки по 32px, ниже - широкие бары истории чата.
     */
    private static final Map<String, int[]> ATLAS_REGIONS = new HashMap<>();
    static {
        ATLAS_REGIONS.put("status_active", new int[]{0, 0, 32, 32});
        ATLAS_REGIONS.put("status_completed", new int[]{32, 0, 32, 32});
        ATLAS_REGIONS.put("status_failed", new int[]{64, 0, 32, 32});
        ATLAS_REGIONS.put("quest_icon", new int[]{96, 0, 32, 32});
        ATLAS_REGIONS.put("narrative_header", new int[]{0, 32, 360, 30});
        ATLAS_REGIONS.put("narrative_footer", new int[]{0, 62, 360, 26});
        ATLAS_REGIONS.put("quest_toast", new int[]{0, 88, 220, 36});
    }

    /** Встроенные исходники для сборки дефолтного атласа (читаются из ресурсов мода). */
    private static final Map<String, ResourceLocation> ATLAS_SOURCES = new LinkedHashMap<>();
    static {
        ATLAS_SOURCES.put("status_active", new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/status_active.png"));
        ATLAS_SOURCES.put("status_completed", new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/status_completed.png"));
        ATLAS_SOURCES.put("status_failed", new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/status_failed.png"));
        ATLAS_SOURCES.put("quest_icon", new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/quest_icon.png"));
        ATLAS_SOURCES.put("narrative_header", new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/narrative_header.png"));
        ATLAS_SOURCES.put("narrative_footer", new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/narrative_footer.png"));
        ATLAS_SOURCES.put("quest_toast", new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/quest_toast.png"));
    }

    private static final Map<String, ResourceLocation> LOADED = new HashMap<>();
    private static final Set<String> MISSING = new HashSet<>();
    private static ResourceLocation atlasLocation = null;

    private MenuAssetsManager() {
    }

    public static boolean isAtlas(String id) {
        return ATLAS_REGIONS.containsKey(id);
    }

    /** Возвращает регион {u, v, w, h} в пикселях атласа для атласного ассета, либо null. */
    public static int[] getRegion(String id) {
        return ATLAS_REGIONS.get(id);
    }

    /**
     * Возвращает ResourceLocation текстуры для указанного assetId.
     * - атласные ассеты -> локация gui_atlas.png (регион через getRegion);
     * - индивидуальные ассеты:
     *     если кастомизация выключена -> встроенная;
     *     если в config/story_engine/menu/&lt;id&gt;.png есть валидный файл -> DynamicTexture;
     *     иначе -> встроенная (результат кэшируется в MISSING).
     */
    public static ResourceLocation get(String assetId) {
        if (ATLAS_SOURCES.containsKey(assetId)) {
            buildAtlasIfNeeded();
            return atlasLocation;
        }

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

    private static void buildAtlasIfNeeded() {
        if (atlasLocation != null) {
            return;
        }
        // Кастомный атлас из config, если включено и файл подходящего размера.
        if (MenuCustomizationConfig.enabled()) {
            Path file = getMenuDirectory().resolve("gui_atlas.png");
            if (Files.isRegularFile(file)) {
                try (InputStream stream = Files.newInputStream(file)) {
                    NativeImage img = NativeImage.read(stream);
                    if (img.getWidth() == ATLAS_W && img.getHeight() == ATLAS_H) {
                        atlasLocation = registerAtlas(img);
                        return;
                    }
                    LOGGER.warn("[StoryEngine] gui_atlas.png имеет размер {}x{}, ожидается {}x{} — используем встроенный атлас",
                            img.getWidth(), img.getHeight(), ATLAS_W, ATLAS_H);
                } catch (IOException | RuntimeException e) {
                    LOGGER.error("[StoryEngine] gui_atlas.png повреждён, используем встроенный атлас", e);
                }
            }
        }
        // Дефолтный атлас, собранный из встроенных текстур.
        NativeImage atlas = buildDefaultAtlas();
        atlasLocation = registerAtlas(atlas);
        if (MenuCustomizationConfig.enabled() && FMLLoader.getDist() == Dist.CLIENT) {
            Path file = getMenuDirectory().resolve("gui_atlas.png");
            if (!Files.isRegularFile(file)) {
                try {
                    atlas.writeToFile(file);
                } catch (IOException e) {
                    LOGGER.warn("[StoryEngine] Не удалось записать шаблон gui_atlas.png", e);
                }
            }
        }
    }

    private static ResourceLocation registerAtlas(NativeImage image) {
        DynamicTexture texture = new DynamicTexture(image);
        ResourceLocation location = new ResourceLocation(StoryEngineMod.MOD_ID, "menu_atlas/gui_atlas");
        Minecraft.getInstance().getTextureManager().register(location, texture);
        return location;
    }

    /** Собирает дефолтный атлас из встроенных gui-текстур по фиксированной раскладке. */
    private static NativeImage buildDefaultAtlas() {
        NativeImage atlas = new NativeImage(ATLAS_W, ATLAS_H, true);
        for (int y = 0; y < ATLAS_H; y++) {
            for (int x = 0; x < ATLAS_W; x++) {
                atlas.setPixelRGBA(x, y, 0);
            }
        }
        for (String id : ATLAS_SOURCES.keySet()) {
            int[] reg = ATLAS_REGIONS.get(id);
            NativeImage src = loadEmbedded(ATLAS_SOURCES.get(id));
            if (src == null) {
                continue;
            }
            drawInto(atlas, src, reg[0], reg[1], reg[2], reg[3]);
            src.close();
        }
        return atlas;
    }

    private static NativeImage loadEmbedded(ResourceLocation loc) {
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(loc);
            if (res.isEmpty()) {
                LOGGER.warn("[StoryEngine] Встроенная текстура не найдена: {}", loc);
                return null;
            }
            return NativeImage.read(res.get().open());
        } catch (IOException | RuntimeException e) {
            LOGGER.error("[StoryEngine] Не удалось прочитать встроенную текстуру {}", loc, e);
            return null;
        }
    }

    /** Копирует src в регион (dx,dy,dw,dh) атласа, растягивая (nearest-neighbor). */
    private static void drawInto(NativeImage dst, NativeImage src, int dx, int dy, int dw, int dh) {
        int sw = src.getWidth();
        int sh = src.getHeight();
        for (int y = 0; y < dh; y++) {
            int sy = sh > 0 ? (y * sh) / dh : 0;
            for (int x = 0; x < dw; x++) {
                int sx = sw > 0 ? (x * sw) / dw : 0;
                dst.setPixelRGBA(dx + x, dy + y, src.getPixelRGBA(sx, sy));
            }
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
        if (FMLLoader.getDist() == Dist.CLIENT) {
            writeAtlasTemplateIfMissing();
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
        if (FMLLoader.getDist() == Dist.CLIENT) {
            Path target = getMenuDirectory().resolve("gui_atlas.png");
            try {
                NativeImage atlas = buildDefaultAtlas();
                atlas.writeToFile(target);
                atlas.close();
            } catch (IOException e) {
                LOGGER.warn("[StoryEngine] Не удалось перезаписать gui_atlas.png", e);
            }
        }
        LOGGER.info("[StoryEngine] Текстуры меню сброшены к исходным в {}", getMenuDirectory());
    }

    /** Сбрасывает кэш - чтобы изменения на диске/в конфиге подхватились без перезапуска клиента. */
    public static void clearCache() {
        LOADED.clear();
        MISSING.clear();
        atlasLocation = null;
    }

    private static void writeAtlasTemplateIfMissing() {
        Path file = getMenuDirectory().resolve("gui_atlas.png");
        if (Files.exists(file)) {
            return;
        }
        try {
            NativeImage atlas = buildDefaultAtlas();
            atlas.writeToFile(file);
            atlas.close();
        } catch (IOException e) {
            LOGGER.warn("[StoryEngine] Не удалось записать шаблон gui_atlas.png", e);
        }
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
