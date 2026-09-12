package com.storyengine.trigger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import com.storyengine.dialogue.DialogueConditionParser;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Читает и валидирует JSON-файлы триггеров из
 * {@code config/story_engine/triggers/} (ТЗ §6).
 *
 * Ошибочный файл не роняет загрузку: ошибка пишется в лог, остальные
 * корректные файлы загружаются, а ранее активная версия ошибочного
 * триггера сохраняется (политика hot-reload ТЗ §14).
 */
public final class TriggerConfigManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /** Результат загрузки директории: успешно прочитанные триггеры + список ошибок. */
    public static final class LoadResult {
        private final List<TriggerDefinition> definitions = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();

        /** id триггера → файл, из которого он загружен (для команд create/delete/enable). */
        private final Map<String, Path> filesByTrigger = new LinkedHashMap<>();

        public List<TriggerDefinition> definitions() {
            return definitions;
        }

        public List<String> errors() {
            return errors;
        }

        public Map<String, Path> filesByTrigger() {
            return filesByTrigger;
        }

        public void error(String message) {
            errors.add(message);
        }
    }

    /**
     * Читает все {@code *.json} из директории. Ошибки отдельных файлов не
     * прерывают обход. Вернувшийся {@link LoadResult} содержит только
     * корректные определения и список диагностических сообщений.
     */
    public LoadResult loadFromDirectory(Path dir) {
        LoadResult result = new LoadResult();
        if (!Files.isDirectory(dir)) {
            return result;
        }
        List<Path> files;
        try (Stream<Path> stream = Files.list(dir)) {
            files = stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Не удалось прочитать директорию триггеров {}: {}", dir, e.getMessage());
            return result;
        }
        for (Path file : files) {
            TriggerDefinition def = loadFile(file);
            if (def != null) {
                result.definitions.add(def);
                result.filesByTrigger.put(def.id(), file);
            }
        }
        return result;
    }

    /** Читает один JSON-файл; {@code null}, если файл ошибочен (ошибка залогирована). */
    @Nullable
    private TriggerDefinition loadFile(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement root = GSON.fromJson(reader, JsonElement.class);
            if (root == null || !root.isJsonObject()) {
                LOGGER.error("[StoryEngine] Триггер {}: корень файла должен быть JSON-объектом", file.getFileName());
                return null;
            }
            return parseDefinition(root.getAsJsonObject(), file.getFileName().toString());
        } catch (com.google.gson.JsonSyntaxException e) {
            LOGGER.error("[StoryEngine] Триггер {}: синтаксическая ошибка JSON: {}", file.getFileName(), e.getMessage());
            return null;
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Триггер {}: ошибка чтения: {}", file.getFileName(), e.getMessage());
            return null;
        }
    }

    /** Разбирает JSON-объект триггера; {@code null} при ошибке (валgидация ТЗ §6, §7). */
    @Nullable
    private TriggerDefinition parseDefinition(JsonObject json, String fileName) {
        String id = getString(json, "id");
        if (id == null || id.isBlank()) {
            LOGGER.error("[StoryEngine] Триггер {}: отсутствует обязательное поле 'id'", fileName);
            return null;
        }

        String eventName = getString(json, "event");
        TriggerEvent event = TriggerEvent.byId(eventName).orElse(null);
        if (event == null) {
            LOGGER.error("[StoryEngine] Триггер {}: неизвестное событие '{}'", fileName, eventName);
            return null;
        }

        String targetTag = getString(json, "target_tag");
        boolean interaction = event.isInteraction();
        if (interaction && (targetTag == null || targetTag.isBlank())) {
            LOGGER.error("[StoryEngine] Триггер {}: для события {} обязателен 'target_tag'", fileName, eventName);
            return null;
        }

        TriggerTargetType targetType = null;
        String targetTypeName = getString(json, "target_type");
        if (targetTypeName != null && !targetTypeName.isBlank()) {
            targetType = TriggerTargetType.byId(targetTypeName).orElse(null);
            if (targetType == null) {
                LOGGER.error("[StoryEngine] Триггер {}: неизвестный тип цели '{}'", fileName, targetTypeName);
                return null;
            }
        }

        List<String> conditions = parseConditions(json, fileName);
        if (conditions == null) {
            return null;
        }

        List<TriggerActionSpec> actions = parseActions(json, fileName);
        if (actions == null) {
            return null;
        }

        String hintLabel = getString(json, "hint_label");

        TriggerDefinition def = TriggerDefinition.builder(id, event)
                .targetTag(targetTag)
                .targetType(targetType)
                .cancelVanilla(getBoolean(json, "cancel_vanilla", false))
                .hintLabel(hintLabel)
                .conditions(conditions)
                .actions(actions)
                .oneTime(getBoolean(json, "one_time", false))
                .priority(getInt(json, "priority", 0))
                .enabled(getBoolean(json, "enabled", true))
                .build();
        LOGGER.info("[StoryEngine] Загружен триггер '{}' (событие {}, цель '{}')",
                id, eventName, targetTag != null ? targetTag : "-");
        return def;
    }

    /**
     * Условия в формате Dialogue System (ТЗ §7). Неизвестные/битые условия
     * делают файл невалидным: триггер не активируется (см. §7.3).
     *
     * @return список условий, или {@code null} при ошибке
     */
    @Nullable
    private List<String> parseConditions(JsonObject json, String fileName) {
        JsonElement el = json.get("conditions");
        if (el == null || el.isJsonNull()) {
            return List.of();
        }
        if (!el.isJsonArray()) {
            LOGGER.error("[StoryEngine] Триггер {}: 'conditions' должно быть массивом строк", fileName);
            return null;
        }
        List<String> result = new ArrayList<>();
        for (JsonElement child : el.getAsJsonArray()) {
            if (!child.isJsonPrimitive() || !((JsonPrimitive) child).isString()) {
                LOGGER.error("[StoryEngine] Триггер {}: условие должно быть строкой", fileName);
                return null;
            }
            String raw = child.getAsString();
            if (raw == null || raw.isBlank()) {
                LOGGER.error("[StoryEngine] Триггер {}: пустое условие", fileName);
                return null;
            }
            if (DialogueConditionParser.parse(raw).isEmpty()) {
                LOGGER.error("[StoryEngine] Триггер {}: некорректное условие '{}'", fileName, raw);
                return null;
            }
            result.add(raw);
        }
        return result;
    }

    /**
     * Действия (ТЗ §8). Каждый элемент — объект с обязательным полем
     * {@code type}; тип должен быть известен. Порядок сохраняется.
     *
     * @return список действий, или {@code null} при ошибке
     */
    @Nullable
    private List<TriggerActionSpec> parseActions(JsonObject json, String fileName) {
        JsonElement el = json.get("actions");
        if (el == null || el.isJsonNull()) {
            LOGGER.error("[StoryEngine] Триггер {}: отсутствует обязательное 'actions'", fileName);
            return null;
        }
        if (!el.isJsonArray() || el.getAsJsonArray().isEmpty()) {
            LOGGER.error("[StoryEngine] Триггер {}: 'actions' должно быть непустым массивом", fileName);
            return null;
        }
        List<TriggerActionSpec> result = new ArrayList<>();
        JsonArray array = el.getAsJsonArray();
        for (int i = 0; i < array.size(); i++) {
            JsonElement item = array.get(i);
            if (!item.isJsonObject()) {
                LOGGER.error("[StoryEngine] Триггер {}: действие #{} должно быть объектом", fileName, i);
                return null;
            }
            JsonObject action = item.getAsJsonObject();
            String type = getString(action, "type");
            if (type == null || type.isBlank()) {
                LOGGER.error("[StoryEngine] Триггер {}: действие #{} без поля 'type'", fileName, i);
                return null;
            }
            if (!TriggerActionRegistry.isKnown(type)) {
                LOGGER.error("[StoryEngine] Триггер {}: неизвестный тип действия '{}'", fileName, type);
                return null;
            }
            JsonObject params = new JsonObject();
            action.entrySet().stream()
                    .filter(e -> !"type".equals(e.getKey()))
                    .forEach(e -> params.add(e.getKey(), e.getValue()));
            result.add(new TriggerActionSpec(type, params));
        }
        return result;
    }

    /** Возвращает множество зарегистрированных типов действий (для команд/диагностики). */
    public static Set<String> knownActionTypes() {
        return TriggerActionRegistry.knownTypes();
    }

    @Nullable
    private static String getString(JsonObject json, String key) {
        JsonElement el = json.get(key);
        return el != null && el.isJsonPrimitive() && ((JsonPrimitive) el).isString()
                ? el.getAsString() : null;
    }

    private static boolean getBoolean(JsonObject json, String key, boolean def) {
        JsonElement el = json.get(key);
        return el != null && el.isJsonPrimitive() && ((JsonPrimitive) el).isBoolean()
                ? el.getAsBoolean() : def;
    }

    private static int getInt(JsonObject json, String key, int def) {
        JsonElement el = json.get(key);
        return el != null && el.isJsonPrimitive() && ((JsonPrimitive) el).isNumber()
                ? el.getAsInt() : def;
    }
}