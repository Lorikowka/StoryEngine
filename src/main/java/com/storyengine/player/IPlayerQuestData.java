package com.storyengine.player;

import com.storyengine.quest.QuestStatus;

import java.util.Set;

public interface IPlayerQuestData {

    QuestStatus getStatus(String questId);

    void setStatus(String questId, QuestStatus status);

    Set<String> getCompletedTasks(String questId);

    void setCompletedTasks(String questId, Set<String> completedTasks);

    boolean isTaskCompleted(String questId, String taskId);

    void completeTask(String questId, String taskId);

    void reset(String questId);

    void clear();

    void copyFrom(IPlayerQuestData other);
}
