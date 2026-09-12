package com.storyengine.trigger;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Реестр типов действий триггера (ТЗ §12, этап 3).
 *
 * Каждый допустимый тип JSON-действия регистрируется фабрикой
 * {@link Function}{code<JsonObject, TriggerAction>}: params (без поля
 * {@code type}) превращаются в сконфигурированный экземпляр действия.
 *
 * Реестр — единый источник известных типов: {@link TriggerConfigManager}
 * валидирует JSON через {@link #isKnown(String)}, а {@link TriggerExecutor}
 * строит экземпляры через {@link #buildAction(String, JsonObject)}.
 * Регистрация всех действий — в {@code StoryEngineMod} (регистрация
 * статических фабрик уже в aware-структуре теста).
 */
public final class TriggerActionRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** тип действия → фабрика экземпляра из params. */
    private static final Map<String, Function<JsonObject, TriggerAction>> ACTIONS = new LinkedHashMap<>();

    private TriggerActionRegistry() {
    }

    /** Регистрирует фабрику действия по типу. Повторная регистрация перезаписывает. */
    public static void register(String type, Function<JsonObject, TriggerAction> factory) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Тип действия не может быть пустым");
        }
        ACTIONS.put(type, factory);
        LOGGER.debug("[StoryEngine] Действие триггера '{}' зарегистрировано", type);
    }

    /** Известен ли тип действия (валидация конфигурации, ТЗ §8). */
    public static boolean isKnown(@Nullable String type) {
        return type != null && ACTIONS.containsKey(type);
    }

    /** Снимок зарегистрированных типов (для команд/диагностики). */
    public static Set<String> knownTypes() {
        return Collections.unmodifiableSet(ACTIONS.keySet());
    }

    /**
     * Строит экземпляр действия по типу и параметрам.
     * {@code null}, если тип не зарегистрирован (конфигурация таких вызовов
     * уже отсекается валидатором, но защита остаётся).
     */
    @Nullable
    public static TriggerAction buildAction(String type, JsonObject params) {
        Function<JsonObject, TriggerAction> factory = ACTIONS.get(type);
        if (factory == null) {
            LOGGER.warn("[StoryEngine] Попытка выполнить неизвестный тип действия '{}'", type);
            return null;
        }
        TriggerAction action = factory.apply(params);
        if (action == null) {
            LOGGER.error("[StoryEngine] Действие '{}' сконструировано как null (некорректные params)", type);
        }
        return action;
    }
}