package com.storyengine.trigger.action;

import com.google.gson.JsonObject;
import com.storyengine.dialogue.DialogueActionExecutor;
import com.storyengine.trigger.ActionStatus;
import com.storyengine.trigger.TriggerAction;
import com.storyengine.trigger.TriggerContext;
import net.minecraft.server.level.ServerPlayer;

/**
 * Действие {@code quest_advance}: продвинуть подзадачу/квест (ТЗ §8).
 * Параметры:
 * <pre>
 * { "type": "quest_advance", "quest": "investigation", "task": "archive_clue" }
 * </pre>
 * Делегирует {@link DialogueActionExecutor#completeTask} — единая логика
 * наград/завершения квеста, как в диалогах и командах (анти-дюп сохранён).
 */
public final class QuestAdvanceAction implements TriggerAction {

    private final String quest;
    private final String task;

    private QuestAdvanceAction(JsonObject params) {
        this.quest = params.has("quest") ? params.get("quest").getAsString() : "";
        this.task = params.has("task") ? params.get("task").getAsString() : "";
    }

    @Override
    public String type() {
        return "quest_advance";
    }

    @Override
    public ActionStatus execute(TriggerContext context) {
        ServerPlayer player = context.player();
        if (player == null || quest.isBlank() || task.isBlank()) {
            return ActionStatus.FAILURE;
        }
        DialogueActionExecutor.completeTask(player, quest + " " + task);
        return ActionStatus.SUCCESS;
    }

    /** Фабрика для TriggerActionRegistry. */
    public static QuestAdvanceAction of(JsonObject params) {
        return new QuestAdvanceAction(params);
    }
}