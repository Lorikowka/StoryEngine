package com.storyengine.player;

import com.storyengine.quest.QuestStatus;

import java.util.Map;
import java.util.Set;

public interface IPlayerQuestData {

    QuestStatus getStatus(String questId);

    void setStatus(String questId, QuestStatus status);

    Set<String> getCompletedTasks(String questId);

    void setCompletedTasks(String questId, Set<String> completedTasks);

    boolean isTaskCompleted(String questId, String taskId);

    void completeTask(String questId, String taskId);

    /** Прогресс автоотслеживаемой подзадачи (ItemQuestTask/BlockBreakQuestTask/KillEntityQuestTask). */
    int getTaskProgress(String questId, String taskId);

    /** value <= 0 удаляет запись. Сериализуется в NBT - переживает рестарт сервера. */
    void setTaskProgress(String questId, String taskId, int value);

    /** Снимок всего прогресса игрока по всем квестам - questId -> (taskId -> значение). */
    Map<String, Map<String, Integer>> getAllTaskProgress();

    void reset(String questId);

    void clear();

    void copyFrom(IPlayerQuestData other);
}
