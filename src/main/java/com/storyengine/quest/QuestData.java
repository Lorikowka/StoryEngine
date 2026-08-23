package com.storyengine.quest;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Модель квеста, загружаемая из config/story_engine/quests/&lt;id&gt;.json
 *
 * Пример структуры файла:
 * {
 *   "id": "find_ancient_key",
 *   "title": "Потерянный ключ",
 *   "description": "Исследуйте старый дом на краю деревни и найдите ключ в сундуке.",
 *   "prerequisites": ["first_quest"],
 *   "tasks": [ ... ],
 *   "rewards": { "commands": [ "..." ], "experience": 10, "items": [ ... ] }
 * }
 */
public class QuestData {

    private String id;
    private String title;
    private String author;
    private String description;
    private List<String> prerequisites = new ArrayList<>();
    private List<QuestTask> tasks = new ArrayList<>();
    private QuestRewards rewards = new QuestRewards();

    public QuestData() {
        // требуется для десериализации Gson
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /** Автор квеста (для сюжета - например, имя NPC-заказчика). Может быть null. */
    @Nullable
    public String getAuthor() {
        return author;
    }

    public void setAuthor(@Nullable String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getPrerequisites() {
        return prerequisites != null ? prerequisites : new ArrayList<>();
    }

    public void setPrerequisites(List<String> prerequisites) {
        this.prerequisites = prerequisites;
    }

    public List<QuestTask> getTasks() {
        return tasks != null ? tasks : new ArrayList<>();
    }

    public void setTasks(List<QuestTask> tasks) {
        this.tasks = tasks;
    }

    public QuestRewards getRewards() {
        if (rewards == null) {
            rewards = new QuestRewards();
        }
        return rewards;
    }

    public void setRewards(QuestRewards rewards) {
        this.rewards = rewards;
    }

    @Override
    public String toString() {
        return "QuestData{id='" + id + "', title='" + title + "', author='" + author + "', tasks=" + getTasks().size() + "}";
    }
}
