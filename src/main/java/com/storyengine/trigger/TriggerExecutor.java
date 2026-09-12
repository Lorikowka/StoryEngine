package com.storyengine.trigger;

import com.mojang.logging.LogUtils;
import com.storyengine.dialogue.DialogueCondition;
import com.storyengine.dialogue.DialogueConditionParser;
import com.storyengine.player.PlayerDialogueData;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

/**
 * Исполняет цепочку действий триггера на сервере (ТЗ §8.1, §8.2, §9.2).
 *
 * Контекст создаётся источником события (листенером) и прокидывается в каждое
 * действие — действия не пересоздают игрока/цель.
 *
 * Правила:
 * - порядок действий — сверху вниз (как в JSON);
 * - fail-stop (ТЗ §8.2): действие со статусом {@link ActionStatus#FAILURE}
 *   останавливает последующие действия и логирует ошибку;
 * - {@code SKIP} не является ошибкой и не прерывает цепочку;
 * - one_time (ТЗ §9.2): флаг «trigger_done:&lt;id&gt;» проверяется ДО выполнения и
 *   устанавливается ТОЛЬКО после успешного выполнения всей цепочки. Флаг
 *   живёт в {@link com.storyengine.player.PlayerDialogueData} — переживает
 *   смерть и рестарт сервера.
 */
public final class TriggerExecutor {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String DONE_FLAG_PREFIX = "trigger_done:";

    /** Максимальная глубина вложенных выполнений (защита от рекурсии через триггерные цепочки). */
    private static final int MAX_DEPTH = 32;

    /** Текущая глубина вложенных вызовов execute (ThreadLocal, чтобы не мешать параллельным мирам). */
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private TriggerExecutor() {
    }

    /**
     * Вспомогательный: выполняет самый приоритетный подходящий триггер из
     * списка. Используется для interaction-событий (ТЗ §9.3: один триггер
     * на клик). Список должен быть уже отсортирован по {@code priority}
     * (см. {@link TriggerManager#byEvent}).
     *
     * @return {@code true}, если триггер выполнен
     */
    public static boolean executeFirstAvailable(TriggerContext ctx, List<TriggerDefinition> candidates) {
        for (TriggerDefinition trigger : candidates) {
            if (!trigger.enabled()) {
                continue;
            }
            if (trigger.oneTime() && isDone(ctx.player(), trigger.id())) {
                continue;
            }
            if (!conditionsMet(ctx, trigger)) {
                LOGGER.debug("[StoryEngine] Триггер '{}': условия не выполнены, пропуск", trigger.id());
                continue;
            }
            ctx.cancelVanillaIfRequested(trigger);
            return execute(ctx, trigger);
        }
        return false;
    }

    /**
     * Выполняет триггер для игрока, если он доступен (включён, не отработан
     * для one_time, действия конструируются без ошибок).
     *
     * @return {@code true}, если цепочка выполнена (все SUCCESS/SKIP),
     *         {@code false} при ошибке или недоступности
     */
    public static boolean execute(TriggerContext ctx, TriggerDefinition trigger) {
        if (ctx.player() == null || trigger == null) {
            return false;
        }
        int depth = DEPTH.get();
        if (depth >= MAX_DEPTH) {
            LOGGER.error("[StoryEngine] Достигнута максимальная глубина выполнения триггеров ({}), "
                    + " цепочка '{}' остановлена — вероятен цикл из действий", MAX_DEPTH, trigger.id());
            return false;
        }
        if (!trigger.enabled()) {
            return false;
        }
        if (trigger.oneTime() && isDone(ctx.player(), trigger.id())) {
            LOGGER.debug("[StoryEngine] Триггер '{}' уже выполнен (one_time), пропуск", trigger.id());
            return false;
        }
        if (!conditionsMet(ctx, trigger)) {
            LOGGER.debug("[StoryEngine] Триггер '{}': условия не выполнены, пропуск", trigger.id());
            return false;
        }

        DEPTH.set(depth + 1);
        try {
            List<TriggerActionSpec> actions = trigger.actions();
            if (actions.isEmpty()) {
                LOGGER.warn("[StoryEngine] Триггер '{}' без действий, пропуск", trigger.id());
                return false;
            }

            boolean ok = true;
            for (int i = 0; i < actions.size(); i++) {
                TriggerActionSpec spec = actions.get(i);
                TriggerAction action = TriggerActionRegistry.buildAction(spec.type(), spec.params());
                if (action == null) {
                    LOGGER.error("[StoryEngine] Триггер '{}': действие #{} (тип '{}') не построено, цепочка остановлена",
                            trigger.id(), i, spec.type());
                    ok = false;
                    break;
                }
                ActionStatus status = action.execute(ctx);
                if (status == ActionStatus.FAILURE) {
                    LOGGER.error("[StoryEngine] Триггер '{}': действие #{} ('{}') вернуло FAILURE, цепочка остановлена",
                            trigger.id(), i, spec.type());
                    ok = false;
                    break;
                }
                LOGGER.debug("[StoryEngine] Триггер '{}': действие '{}' → {}", trigger.id(), spec.type(), status);
            }

            if (ok) {
                markDone(ctx.player(), trigger.id());
            }
            // Состояние игрока могло измениться (флаги, предметы, квесты) —
            // актуализируем серверную подсказку (ТЗ §10.3).
            if (ctx.level() != null) {
                TriggerHintService.get().resync(ctx.player(), ctx.level());
            }
            return ok;
        } finally {
            DEPTH.set(depth);
        }
    }

    /**
     * Выполняет ВСЕ подходящие триггеры списка (state-события, §9.3) —
     * в порядке переданного списка, который должен быть отсортирован по
     * {@code priority} (см. {@link TriggerManager#byEvent}). Каждый триггер
     * независимо проходит фильтры enabled/one_time/conditions; сбой одного
     * не прерывает остальные.
     */
    public static void executeAll(TriggerContext ctx, List<TriggerDefinition> candidates) {
        for (TriggerDefinition trigger : candidates) {
            if (!trigger.enabled()) {
                continue;
            }
            if (trigger.oneTime() && isDone(ctx.player(), trigger.id())) {
                continue;
            }
            try {
                execute(ctx, trigger);
            } catch (RuntimeException e) {
                LOGGER.error("[StoryEngine] Ошибка выполнения триггера '{}'", trigger.id(), e);
            }
        }
    }

    /**
     * Доступен ли триггер для игрока сейчас: включён, не отработан (one_time),
     * условия выполнены. Используется серверным резолвером подсказок
     * (TriggerHintService), чтобы не показывать подсказку на невыполнимом
     * триггере.
     */
    public static boolean isAvailable(TriggerContext ctx, TriggerDefinition trigger) {
        if (trigger == null || !trigger.enabled()) {
            return false;
        }
        if (trigger.oneTime() && isDone(ctx.player(), trigger.id())) {
            return false;
        }
        return conditionsMet(ctx, trigger);
    }

    /** Проверка условий триггера (единый диалект Dialogue System, ТЗ §7). */
    private static boolean conditionsMet(TriggerContext ctx, TriggerDefinition trigger) {
        List<String> conditions = trigger.conditions();
        if (conditions.isEmpty()) {
            return true;
        }
        for (String raw : conditions) {
            Optional<DialogueCondition> parsed = DialogueConditionParser.parse(raw);
            if (parsed.isEmpty()) {
                LOGGER.warn("[StoryEngine] Триггер '{}': некорректное условие '{}'", trigger.id(), raw);
                return false;
            }
            if (!parsed.get().evaluate(ctx.player())) {
                return false;
            }
        }
        return true;
    }

    /** {@code true}, если one_time-триггер уже отработан игроком. */
    public static boolean isDone(ServerPlayer player, String triggerId) {
        return PlayerDialogueData.get(player).getFlag(DONE_FLAG_PREFIX + triggerId);
    }

    private static void markDone(ServerPlayer player, String triggerId) {
        PlayerDialogueData.get(player).setFlag(DONE_FLAG_PREFIX + triggerId, true);
    }
}