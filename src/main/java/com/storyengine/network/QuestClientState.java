package com.storyengine.network;

import com.storyengine.quest.QuestData;
import com.storyengine.quest.QuestStatus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class QuestClientState {

    private static final Map<String, QuestStatus> STATUSES = new LinkedHashMap<>();
    private static final Map<String, List<String>> COMPLETED_TASKS = new LinkedHashMap<>();
    private static final Map<String, Map<String, Integer>> TASK_PROGRESS = new LinkedHashMap<>();
    private static final Map<String, QuestData> QUESTS = new LinkedHashMap<>();
    private static String LAST_NOTIFICATION = null;

    private QuestClientState() {
    }

    public static void apply(QuestNetworking.S2CSyncQuestDataPacket packet) {
        STATUSES.clear();
        COMPLETED_TASKS.clear();
        TASK_PROGRESS.clear();
        QUESTS.clear();
        STATUSES.putAll(packet.statuses);
        packet.completedTasks.forEach((questId, tasks) -> COMPLETED_TASKS.put(questId, List.copyOf(tasks)));
        packet.taskProgress.forEach((questId, progress) -> TASK_PROGRESS.put(questId, new LinkedHashMap<>(progress)));
        packet.questData.forEach(quest -> QUESTS.put(quest.getId(), quest));
    }

    public static Map<String, QuestStatus> getStatuses() {
        return Collections.unmodifiableMap(STATUSES);
    }

    public static Map<String, List<String>> getCompletedTasks() {
        return Collections.unmodifiableMap(COMPLETED_TASKS);
    }

    public static Map<String, Map<String, Integer>> getTaskProgress() {
        return Collections.unmodifiableMap(TASK_PROGRESS);
    }

    public static Map<String, QuestData> getQuests() {
        return Collections.unmodifiableMap(QUESTS);
    }

    public static String getLastNotification() {
        return LAST_NOTIFICATION;
    }

    public static void setLastNotification(String text) {
        LAST_NOTIFICATION = text;
    }
}
