package com.storyengine.trigger.event;

import com.mojang.logging.LogUtils;
import com.storyengine.StoryEngineMod;
import com.storyengine.trigger.TriggerContext;
import com.storyengine.trigger.TriggerDefinition;
import com.storyengine.trigger.TriggerEvent;
import com.storyengine.trigger.TriggerExecutor;
import com.storyengine.trigger.TriggerHintService;
import com.storyengine.trigger.TriggerManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.List;

/**
 * Точки входа для state-событий (ТЗ §4.2): QUEST_CHANGE и DIALOGUE_FINISH.
 *
 * В отличие от interaction-событий (§9.3) здесь выполняются ВСЕ подходящие
 * триггеры события подряд — по убыванию {@code priority} (порядок от
 * {@link TriggerManager#byEvent}). Каждый проходит собственный фильтр
 * условий/one_time внутри {@link TriggerExecutor}.
 *
 * Хуки вызываются из интеграционных точек кода (квестовый API, менеджер
 * диалогов), поэтому защищены от отсутствия менеджера триггеров и от
 * рекурсии через {@link TriggerExecutor} (там счётчик глубины).
 */
public final class EventTriggerHooks {

    private static final Logger LOGGER = LogUtils.getLogger();

    private EventTriggerHooks() {
    }

    /**
     * Срабатывает при любом изменении прогресса квеста игрока: переход
     * статуса, завершение подзадачи, прогресс отслеживаемой задачи.
     */
    public static void fireQuestChanged(ServerPlayer player, ServerLevel level) {
        fireStateEvent(player, level, TriggerEvent.QUEST_CHANGE);
    }

    /** Срабатывает при завершении активного диалога игрока. */
    public static void fireDialogueFinished(ServerPlayer player, ServerLevel level) {
        fireStateEvent(player, level, TriggerEvent.DIALOGUE_FINISH);
    }

    private static void fireStateEvent(ServerPlayer player, ServerLevel level, TriggerEvent eventType) {
        TriggerManager manager = StoryEngineMod.TRIGGER_MANAGER;
        if (manager == null) {
            return;
        }
        List<TriggerDefinition> candidates = manager.byEvent(eventType);
        TriggerContext ctx = TriggerContext.builder(player, level, eventType).build();
        if (!candidates.isEmpty()) {
            TriggerExecutor.executeAll(ctx, candidates);
        }
        // Актуализация серверной подсказки: квест/флаги/диалог могли изменить доступность триггера.
        TriggerHintService.get().resync(player, level);
    }
}