package com.storyengine.player;

import com.storyengine.quest.QuestStatus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class PlayerQuestData implements IPlayerQuestData, INBTSerializable<CompoundTag> {

    private final Map<String, QuestStatus> statuses = new HashMap<>();
    private final Map<String, Set<String>> completedTasks = new HashMap<>();
    /** questId -> (taskId -> прогресс). Только для автоотслеживаемых типов задач. */
    private final Map<String, Map<String, Integer>> taskProgress = new HashMap<>();

    @Override
    public QuestStatus getStatus(String questId) {
        return statuses.getOrDefault(questId, QuestStatus.NOT_STARTED);
    }

    @Override
    public void setStatus(String questId, QuestStatus status) {
        if (status == null || status == QuestStatus.NOT_STARTED) {
            statuses.remove(questId);
            completedTasks.remove(questId);
            taskProgress.remove(questId);
            return;
        }
        statuses.put(questId, status);
    }

    @Override
    public Set<String> getCompletedTasks(String questId) {
        Set<String> tasks = completedTasks.get(questId);
        return tasks == null ? Collections.emptySet() : Collections.unmodifiableSet(tasks);
    }

    @Override
    public void setCompletedTasks(String questId, Set<String> completedTasks) {
        if (completedTasks == null || completedTasks.isEmpty()) {
            this.completedTasks.remove(questId);
            return;
        }
        this.completedTasks.put(questId, new LinkedHashSet<>(completedTasks));
    }

    @Override
    public boolean isTaskCompleted(String questId, String taskId) {
        return getCompletedTasks(questId).contains(taskId);
    }

    @Override
    public void completeTask(String questId, String taskId) {
        if (taskId == null || taskId.isBlank()) {
            return;
        }
        Set<String> tasks = new LinkedHashSet<>(getCompletedTasks(questId));
        tasks.add(taskId);
        setCompletedTasks(questId, tasks);
        if (!statuses.containsKey(questId)) {
            statuses.put(questId, QuestStatus.ACTIVE);
        }
    }

    @Override
    public int getTaskProgress(String questId, String taskId) {
        Map<String, Integer> quest = taskProgress.get(questId);
        return quest == null ? 0 : quest.getOrDefault(taskId, 0);
    }

    @Override
    public void setTaskProgress(String questId, String taskId, int value) {
        if (value <= 0) {
            Map<String, Integer> quest = taskProgress.get(questId);
            if (quest != null) {
                quest.remove(taskId);
                if (quest.isEmpty()) {
                    taskProgress.remove(questId);
                }
            }
            return;
        }
        taskProgress.computeIfAbsent(questId, ignored -> new HashMap<>()).put(taskId, value);
    }

    @Override
    public Map<String, Map<String, Integer>> getAllTaskProgress() {
        Map<String, Map<String, Integer>> copy = new HashMap<>();
        taskProgress.forEach((questId, tasks) -> copy.put(questId, new HashMap<>(tasks)));
        return copy;
    }

    @Override
    public void reset(String questId) {
        statuses.remove(questId);
        completedTasks.remove(questId);
        taskProgress.remove(questId);
    }

    @Override
    public void clear() {
        statuses.clear();
        completedTasks.clear();
        taskProgress.clear();
    }

    @Override
    public void copyFrom(IPlayerQuestData other) {
        clear();
        if (other instanceof PlayerQuestData otherData) {
            otherData.statuses.forEach((questId, status) -> statuses.put(questId, status));
            otherData.completedTasks.forEach((questId, tasks) -> completedTasks.put(questId, new LinkedHashSet<>(tasks)));
            otherData.taskProgress.forEach((questId, tasks) -> taskProgress.put(questId, new HashMap<>(tasks)));
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag root = new CompoundTag();
        statuses.forEach((questId, status) -> root.putString("status:" + questId, status.name()));
        completedTasks.forEach((questId, tasks) -> {
            ListTag taskList = new ListTag();
            tasks.stream().sorted().forEach(taskId -> taskList.add(StringTag.valueOf(taskId)));
            root.put("tasks:" + questId, taskList);
        });

        CompoundTag progressRoot = new CompoundTag();
        taskProgress.forEach((questId, tasks) -> {
            CompoundTag questProgress = new CompoundTag();
            tasks.forEach(questProgress::putInt);
            progressRoot.put(questId, questProgress);
        });
        root.put("progress", progressRoot);

        return root;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        clear();
        for (String key : nbt.getAllKeys()) {
            if (key.startsWith("status:")) {
                String questId = key.substring("status:".length());
                String rawStatus = nbt.getString(key);
                try {
                    statuses.put(questId, QuestStatus.valueOf(rawStatus));
                } catch (IllegalArgumentException ignored) {
                    // ignore malformed status values
                }
            } else if (key.startsWith("tasks:")) {
                String questId = key.substring("tasks:".length());
                ListTag taskList = nbt.getList(key, Tag.TAG_STRING);
                Set<String> tasks = new LinkedHashSet<>();
                for (int i = 0; i < taskList.size(); i++) {
                    tasks.add(taskList.getString(i));
                }
                if (!tasks.isEmpty()) {
                    completedTasks.put(questId, tasks);
                }
            }
        }

        if (nbt.contains("progress")) {
            CompoundTag progressRoot = nbt.getCompound("progress");
            for (String questId : progressRoot.getAllKeys()) {
                CompoundTag questProgress = progressRoot.getCompound(questId);
                Map<String, Integer> tasks = new HashMap<>();
                for (String taskId : questProgress.getAllKeys()) {
                    tasks.put(taskId, questProgress.getInt(taskId));
                }
                if (!tasks.isEmpty()) {
                    taskProgress.put(questId, tasks);
                }
            }
        }
    }
}
