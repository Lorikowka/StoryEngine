package com.storyengine.trigger.action;

import com.google.gson.JsonObject;
import com.storyengine.trigger.ActionStatus;
import com.storyengine.trigger.TriggerAction;
import com.storyengine.trigger.TriggerContext;

/**
 * Действие {@code run_command}: выполнить произвольную команду от имени
 * сервера в контексте игрока (ТЗ §8). Параметры:
 * <pre>
 * { "type": "run_command", "command": "story dialogue start @p melan" }
 * </pre>
 * Команда выполняется с правами level 4 и селектором {@code @p} = игрок,
 * запустивший триггер. Тот же механизм, что и награды квестов
 * {@link com.storyengine.quest.QuestRewards} (команды наград квестов).
 */
public final class RunCommandAction implements TriggerAction {

    private final String command;

    private RunCommandAction(JsonObject params) {
        this.command = params.has("command") ? params.get("command").getAsString() : "";
    }

    @Override
    public String type() {
        return "run_command";
    }

    @Override
    public ActionStatus execute(TriggerContext context) {
        // Контекст игрока (для селектора @p), права level 4, без вывода в чат.
        int result = ActionCommands.runAsPlayer(context.player(), command);
        return result >= 0 ? ActionStatus.SUCCESS : ActionStatus.FAILURE;
    }

    /** Фабрика для TriggerActionRegistry. */
    public static RunCommandAction of(JsonObject params) {
        return new RunCommandAction(params);
    }
}