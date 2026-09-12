package com.storyengine.trigger;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Менеджер триггеров (ТЗ §12): загрузка конфигурации из
 * {@code config/story_engine/triggers/}, кэширование, поиск по событию и
 * по тегу, hot-reload и диагностика дублирующихся тегов.
 *
 * Экземпляр хранится в {@code StoryEngineMod.TRIGGER_MANAGER}.
 *
 * Политика hot-reload (ТЗ §14): при неудачном JSON-файле ранее загруженная
 * версия этого триггера сохраняется до следующей успешной загрузки; битый
 * новый файл добавляется в список ошибок, но не роняет остальные.
 */
public final class TriggerManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final TriggerConfigManager configManager = new TriggerConfigManager();

    /** id триггера → определение. */
    private final Map<String, TriggerDefinition> cache = new HashMap<>();

    /** id триггера → файл, из которого он загружен (для команд create/delete/enable). */
    private final Map<String, Path> filesByTrigger = new HashMap<>();

    /** Ошибки последней загрузки (для {@code /story trigger info} и диагностики). */
    private final List<String> lastErrors = new ArrayList<>();

    /**
     * Возвращает (и при необходимости создаёт) директорию триггеров:
     * config/story_engine/triggers/
     */
    public Path getTriggersDirectory() {
        Path dir = FMLPaths.CONFIGDIR.get().resolve("story_engine").resolve("triggers");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Не удалось создать директорию триггеров: {}", dir, e);
        }
        return dir;
    }

    /**
     * Полная (пере)загрузка всех триггеров из файлов на диске.
     * При успешной загрузке заменяет активный набор; ошибочные файлы
     * оставляют прежнюю версию триггера без предупреждения-исчезновения.
     */
    public void loadAll() {
        lastErrors.clear();
        TriggerConfigManager.LoadResult result = configManager.loadFromDirectory(getTriggersDirectory());
        cache.clear();
        filesByTrigger.clear();
        Map<String, TriggerDefinition> fresh = new HashMap<>();
        for (TriggerDefinition def : result.definitions()) {
            TriggerDefinition previous = fresh.put(def.id(), def);
            if (previous != null) {
                LOGGER.warn("[StoryEngine] Дублирующийся id триггера '{}': используется последний файл", def.id());
            }
            cache.put(def.id(), def);
        }
        filesByTrigger.putAll(result.filesByTrigger());
        lastErrors.addAll(result.errors());
        LOGGER.info("[StoryEngine] Загружено триггеров: {}, ошибок: {}", cache.size(), result.errors().size());
        warnConflictingTargetTags();
    }

    /** Алиас {@link #loadAll()} для консистентности с QuestManager. */
    public void reload() {
        loadAll();
    }

    /** Ошибки последней загрузки (только диагностика). */
    public List<String> getLastErrors() {
        return ImmutableList.copyOf(lastErrors);
    }

    public boolean exists(String id) {
        return cache.containsKey(id);
    }

    public Optional<TriggerDefinition> getTrigger(String id) {
        return Optional.ofNullable(cache.get(id));
    }

    public Collection<TriggerDefinition> getAllDefinitions() {
        return ImmutableList.copyOf(cache.values());
    }

    public int size() {
        return cache.size();
    }

    /** Все включённые триггеры указанного события. */
    public List<TriggerDefinition> byEvent(TriggerEvent event) {
        return cache.values().stream()
                .filter(d -> d.event() == event && d.enabled())
                .sorted((a, b) -> Integer.compare(b.priority(), a.priority()))
                .collect(Collectors.toList());
    }

    /** Все включённые триггеры с указанным тегом цели. */
    public List<TriggerDefinition> byTag(String targetTag) {
        return cache.values().stream()
                .filter(d -> targetTag.equals(d.targetTag()) && d.enabled())
                .collect(Collectors.toList());
    }

    /**
     * Валидация {@code target_tag} (правка 1.1): предупреждение о нескольких
     * триггерах одного interaction-события с одним и тем же тегом (это чревато
     * тихим конфликтом приоритетов §9.3). Не блокирует загрузку.
     */
    private void warnConflictingTargetTags() {
        Map<String, List<TriggerDefinition>> byTag = new HashMap<>();
        for (TriggerDefinition def : cache.values()) {
            if (def.event().isInteraction() && def.targetTag() != null) {
                byTag.computeIfAbsent(def.targetTag(), k -> new ArrayList<>()).add(def);
            }
        }
        byTag.forEach((tag, list) -> {
            if (list.size() > 1) {
                String ids = list.stream().map(TriggerDefinition::id).collect(Collectors.joining(", "));
                LOGGER.warn("[StoryEngine] Тег '{}' используется несколькими interaction-триггерами: {} — "
                        + "убедитесь, что конфликт приоритетов (§9.3) осознанный", tag, ids);
            }
        });
    }

    /**
     * Диагностика дублей тегов в мире: для каждого используемого
     * {@code target_tag} считает сущности с этим scoreboard-тегом в
     * загруженных чанках и логирует предупреждение, если их больше одной
     * (против правила «один объект — один уникальный тег», §5.3).
     *
     * Метод сканирует только загруженные чанки и предназначен для
     * диагностики на старте/hot-reload, не для горячего пути.
     */
    public void auditDuplicateWorldTags(ServerLevel level) {
        if (level.isClientSide()) {
            return;
        }
        List<String> tags = cache.values().stream()
                .map(TriggerDefinition::targetTag)
                .filter(tag -> tag != null && !tag.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        for (String tag : tags) {
            long count = 0;
            for (Entity e : level.getEntities().getAll()) {
                if (e.getTags().contains(tag)) {
                    count++;
                }
            }
            if (count > 1) {
                LOGGER.warn("[StoryEngine] В мире найдено {} сущностей с тегом '{}' — правило "
                        + "«один сюжетный объект — один уникальный тег» (§5.3) может нарушаться", count, tag);
            }
        }
    }

    /**
     * Поиск цели с тегом. Правило MVP (§5.3): если несколько сущностей имеют
     * один тег, выбирается ближайшая к игроку. {@code null}, если не найдено.
     */
    @Nullable
    public Entity findTarget(ServerLevel level, ServerPlayer player, @Nullable String targetTag) {
        if (targetTag == null || targetTag.isBlank()) {
            return null;
        }
        Entity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : level.getEntities().getAll()) {
            if (!e.getTags().contains(targetTag)) {
                continue;
            }
            double d = e.distanceToSqr(player);
            if (d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        return best;
    }

    /**
     * Поиск сущности с тегом в радиусе от позиции. Ближайшая к центру.
     * {@code null}, если такой сущности в радиусе нет.
     */
    @Nullable
    public Entity findTargetNear(ServerLevel level, Vec3 center, @Nullable String targetTag, double radius) {
        if (targetTag == null || targetTag.isBlank()) {
            return null;
        }
        double radiusSq = radius * radius;
        Entity best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (Entity e : level.getEntities().getAll()) {
            if (!e.getTags().contains(targetTag)) {
                continue;
            }
            double d = e.position().distanceToSqr(center);
            if (d <= radiusSq && d < bestDistSq) {
                bestDistSq = d;
                best = e;
            }
        }
        return best;
    }

    /**
     * Путь к файлу загруженного триггера (например, для команды delete).
     * Если триггер не загружен — стандартный путь {@code <id>.json}.
     */
    public Path fileFor(String id) {
        Path known = filesByTrigger.get(id);
        return known != null ? known : getTriggersDirectory().resolve(id + ".json");
    }

    /**
     * Создаёт валидный шаблон триггера в {@code config/story_engine/triggers/<id>.json}
     * и перезагружает кэш. Используется командой {@code /story trigger create}.
     * Возвращает пустой Optional, если файл уже существует.
     */
    public Optional<TriggerDefinition> createTemplate(String id, TriggerEvent event) {
        Path file = getTriggersDirectory().resolve(id + ".json");
        if (Files.exists(file)) {
            return Optional.empty();
        }

        JsonObject root = new JsonObject();
        root.addProperty("id", id);
        root.addProperty("event", event.name().toLowerCase());
        if (event.isInteraction()) {
            root.addProperty("target_type", "entity");
            root.addProperty("target_tag", "story_npc_" + id);
        }
        root.addProperty("enabled", true);
        root.addProperty("one_time", false);
        root.addProperty("priority", 0);
        root.addProperty("cancel_vanilla", false);

        JsonObject tell = new JsonObject();
        tell.addProperty("type", "story_tell");
        tell.addProperty("speaker", "NPC");
        tell.addProperty("icon", "none");
        JsonObject text = new JsonObject();
        text.addProperty("text", "Привет!");
        text.addProperty("color", "white");
        tell.add("text", text);
        JsonArray actions = new JsonArray();
        actions.add(tell);
        root.add("actions", actions);

        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Не удалось записать триггер {}", file, e);
        }

        loadAll();
        return getTrigger(id);
    }

    /**
     * Полностью удаляет триггер: файл с диска + определение из кэша.
     * Возвращает false, если триггера с таким id нет ни в кэше, ни на диске.
     */
    public boolean delete(String id) {
        Path file = fileFor(id);
        if (!Files.exists(file) && !cache.containsKey(id)) {
            return false;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Не удалось удалить файл триггера {}", file, e);
        }
        loadAll();
        return !cache.containsKey(id);
    }

    /**
     * Включает/отключает триггер: правит поле {@code enabled} в JSON-файле и
     * перезагружает кэш. Возвращает false, если триггера нет на диске.
     */
    public boolean setEnabled(String id, boolean enabled) {
        Path file = fileFor(id);
        if (!Files.exists(file)) {
            return false;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                return false;
            }
            root.addProperty("enabled", enabled);
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Не удалось изменить enabled триггера {}", file, e);
            return false;
        }
        loadAll();
        return cache.containsKey(id);
    }
}