package com.storyengine.interaction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.storyengine.network.QuestNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Списки блоков для Interaction Hint System — файл
 * {@code config/story_engine/interaction.json}:
 *
 * <pre>{@code
 * {
 *   "no_hints":    ["minecraft:chest", "minecraft:oak_door -20 56 -4"],
 *   "no_interact": ["minecraft:barrel -54 56 6"]
 * }
 * }</pre>
 *
 * <ul>
 *   <li><b>no_hints</b> — у блока не показывается подсказка при наведении
 *       (клиентский сканер пропускает его);</li>
 *   <li><b>no_interact</b> — ПКМ/F на блоке отменяется на сервере.</li>
 * </ul>
 *
 * Запись — либо id блока ({@code minecraft:chest} — все блоки типа, допускается
 * сокращение {@code chest}), либо id + координаты целого блока
 * ({@code minecraft:barrel -54 56 6} — только этот блок).
 *
 * <p>Из скриптов файл управляется через {@code StoryApi}:
 * {@code addNoHintBlock}/{@code removeNoHintBlock},
 * {@code addNoInteractBlock}/{@code removeNoInteractBlock},
 * {@code reloadInteractionConfig} — каждый вызов сохраняет файл и
 * синхронизирует списки клиентам.
 */
public final class InteractionBlockLists {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /** Одна запись списка: нормализованный id блока, опц. позиция, исходная строка. */
    public record Entry(String blockId, @Nullable BlockPos pos, String raw) {
        public boolean matches(String id, BlockPos blockPos) {
            return blockId.equals(id) && (pos == null || pos.equals(blockPos));
        }
    }

    private static List<Entry> noHints = new ArrayList<>();
    private static List<Entry> noInteract = new ArrayList<>();

    private InteractionBlockLists() {
    }

    /** Путь к файлу списков: config/story_engine/interaction.json */
    public static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("story_engine").resolve("interaction.json");
    }

    // ----------------------------------------------------------------
    //  Загрузка / сохранение
    // ----------------------------------------------------------------

    /** Загрузить списки с диска; при отсутствии файла создать пустой-шаблон. */
    public static synchronized void load() {
        Path file = file();
        if (!Files.exists(file)) {
            noHints = new ArrayList<>();
            noInteract = new ArrayList<>();
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                return; // битый файл — оставляем прежние списки
            }
            noHints = parseArray(root.get("no_hints"));
            noInteract = parseArray(root.get("no_interact"));
            LOGGER.info("[StoryEngine] Загружены списки interaction.json: no_hints={}, no_interact={}",
                    noHints.size(), noInteract.size());
        } catch (IOException | RuntimeException e) {
            LOGGER.error("[StoryEngine] Не удалось прочитать {}", file, e);
        }
    }

    /** Перечитать файл с диска и разослать списки клиентам (для API/команд). */
    public static synchronized void reload() {
        load();
        sync();
    }

    private static synchronized void save() {
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Не удалось создать директорию для {}", file, e);
            return;
        }
        JsonObject root = new JsonObject();
        root.add("no_hints", toArray(noHints));
        root.add("no_interact", toArray(noInteract));
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Не удалось записать {}", file, e);
        }
    }

    private static JsonArray toArray(List<Entry> entries) {
        JsonArray array = new JsonArray();
        for (Entry entry : entries) {
            array.add(entry.raw());
        }
        return array;
    }

    // ----------------------------------------------------------------
    //  Разбор записей
    // ----------------------------------------------------------------

    private static List<Entry> parseArray(@Nullable JsonElement element) {
        List<Entry> result = new ArrayList<>();
        if (element == null || element.isJsonNull()) {
            return result;
        }
        if (!element.isJsonArray()) {
            LOGGER.warn("[StoryEngine] interaction.json: ожидается массив строк, запись пропущена");
            return result;
        }
        for (JsonElement child : element.getAsJsonArray()) {
            if (!child.isJsonPrimitive() || !child.getAsJsonPrimitive().isString()) {
                LOGGER.warn("[StoryEngine] interaction.json: запись не строка — пропущена");
                continue;
            }
            Entry entry = parseEntry(child.getAsString());
            if (entry != null) {
                result.add(entry);
            }
        }
        return result;
    }

    /** Разобрать сырую запись: {@code <id>} или {@code <id> <x> <y> <z>}. null — невалидна. */
    @Nullable
    public static Entry parseEntry(String raw) {
        if (raw == null || raw.isBlank()) {
            LOGGER.warn("[StoryEngine] interaction.json: пустая запись — пропущена");
            return null;
        }
        String[] parts = raw.trim().split("\\s+");
        String id = normalizeId(parts[0]);
        if (id == null) {
            LOGGER.warn("[StoryEngine] interaction.json: некорректный id '{}' — запись пропущена", parts[0]);
            return null;
        }
        BlockPos pos = null;
        if (parts.length > 1) {
            if (parts.length != 4) {
                LOGGER.warn("[StoryEngine] interaction.json: запись '{}' — формат: <id> или <id> <x> <y> <z>", raw);
                return null;
            }
            try {
                pos = new BlockPos(Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
            } catch (NumberFormatException e) {
                LOGGER.warn("[StoryEngine] interaction.json: координаты в '{}' не числа — запись пропущена", raw);
                return null;
            }
        }
        return new Entry(id, pos, raw.trim());
    }

    /** Нормализовать id: «chest» → «minecraft:chest»; null — невалидный id. */
    @Nullable
    public static String normalizeId(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return null;
        }
        String id = blockId.trim();
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        try {
            return new ResourceLocation(id).toString();
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ----------------------------------------------------------------
    //  Изменение из API (сохранение + рассылка клиентам)
    // ----------------------------------------------------------------

    /** Добавить «все блоки этого id» в no_hints. false — невалидный id или уже есть. */
    public static synchronized boolean addNoHint(String blockId) {
        return add(noHints, blockId, "no_hints");
    }

    /** Убрать из no_hints все записи этого id (типовые и позиционные). */
    public static synchronized boolean removeNoHint(String blockId) {
        return remove(noHints, blockId, "no_hints");
    }

    /** Добавить «все блоки этого id» в no_interact. false — невалидный id или уже есть. */
    public static synchronized boolean addNoInteract(String blockId) {
        return add(noInteract, blockId, "no_interact");
    }

    /** Убрать из no_interact все записи этого id (типовые и позиционные). */
    public static synchronized boolean removeNoInteract(String blockId) {
        return remove(noInteract, blockId, "no_interact");
    }

    private static boolean add(List<Entry> list, String blockId, String listName) {
        String id = normalizeId(blockId);
        if (id == null) {
            LOGGER.error("[StoryEngine] add{}: некорректный id блока '{}'", listName, blockId);
            return false;
        }
        for (Entry entry : list) {
            if (entry.blockId().equals(id) && entry.pos() == null) {
                return false; // уже есть запись «весь тип»
            }
        }
        list.add(new Entry(id, null, id));
        save();
        sync();
        return true;
    }

    private static boolean remove(List<Entry> list, String blockId, String listName) {
        String id = normalizeId(blockId);
        if (id == null) {
            LOGGER.error("[StoryEngine] remove{}: некорректный id блока '{}'", listName, blockId);
            return false;
        }
        boolean removed = list.removeIf(entry -> entry.blockId().equals(id));
        if (removed) {
            save();
            sync();
        }
        return removed;
    }

    /** Рассылка актуальных списков клиентам (вызывается с сервера). */
    public static synchronized void sync() {
        QuestNetworking.sendInteractionBlockLists();
    }

    // ----------------------------------------------------------------
    //  Проверки (сервер: отмена взаимодействия; клиент: сканер подсказок)
    // ----------------------------------------------------------------

    /** Показывать ли подсказку при наведении на блок (false — блок в no_hints). */
    public static synchronized boolean isNoHint(Block block, BlockPos pos) {
        return contains(noHints, block, pos);
    }

    /** Разрешено ли взаимодействие с блоком (false — блок в no_interact). */
    public static synchronized boolean isNoInteract(Block block, BlockPos pos) {
        return contains(noInteract, block, pos);
    }

    private static boolean contains(List<Entry> list, Block block, BlockPos pos) {
        if (list.isEmpty()) {
            return false;
        }
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
        if (key == null) {
            return false;
        }
        String id = key.toString();
        for (Entry entry : list) {
            if (entry.matches(id, pos)) {
                return true;
            }
        }
        return false;
    }

    // ----------------------------------------------------------------
    //  Синхронизация с клиентом
    // ----------------------------------------------------------------

    /** Применить присланные сервером списки (клиент, из пакета). Без save/sync. */
    public static synchronized void applyClient(List<String> rawHints, List<String> rawInteract) {
        noHints = parseRaw(rawHints);
        noInteract = parseRaw(rawInteract);
    }

    private static List<Entry> parseRaw(List<String> raws) {
        List<Entry> result = new ArrayList<>();
        if (raws == null) {
            return result;
        }
        for (String raw : raws) {
            Entry entry = parseEntry(raw);
            if (entry != null) {
                result.add(entry);
            }
        }
        return result;
    }

    /** Сырые записи для пакета: no_hints. */
    public static synchronized List<String> noHintRaw() {
        List<String> result = new ArrayList<>();
        for (Entry entry : noHints) {
            result.add(entry.raw());
        }
        return result;
    }

    /** Сырые записи для пакета: no_interact. */
    public static synchronized List<String> noInteractRaw() {
        List<String> result = new ArrayList<>();
        for (Entry entry : noInteract) {
            result.add(entry.raw());
        }
        return result;
    }
}
