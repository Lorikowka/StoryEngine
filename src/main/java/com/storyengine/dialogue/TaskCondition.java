package com.storyengine.dialogue;

import com.storyengine.player.PlayerQuestDataHelper;
import net.minecraft.world.entity.player.Player;

/** Подзадача выполнена: task:<questId> <taskId>. */
public class TaskCondition extends DialogueCondition {

    private final String questId;
    private final String taskId;

    public TaskCondition(String questId, String taskId) {
        this.questId = questId;
        this.taskId = taskId;
    }

    @Override
    public boolean evaluate(Player player) {
        return PlayerQuestDataHelper.isTaskCompleted(player, questId, taskId);
    }
}
