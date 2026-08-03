package com.storyengine.quest;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * Базовый класс подзадачи квеста.
 * Полиморфная десериализация выполняется по полю "type".
 */
public class QuestTask {

    private String id;
    private String title;
    private String description;
    private String type = "MANUAL";

    public QuestTask() {
        // требуется для десериализации Gson
    }

    public QuestTask(String id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "QuestTask{id='" + id + "', title='" + title + "', type='" + type + "'}";
    }

    public static class Serializer implements JsonSerializer<QuestTask>, JsonDeserializer<QuestTask> {

        @Override
        public QuestTask deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            String type = jsonObject.has("type") ? jsonObject.get("type").getAsString() : "MANUAL";
            QuestTask task;
            switch (type.toUpperCase()) {
                case "LOCATION" -> task = context.deserialize(jsonObject, LocationQuestTask.class);
                case "ITEM" -> task = context.deserialize(jsonObject, ItemQuestTask.class);
                case "BLOCK_BREAK" -> task = context.deserialize(jsonObject, BlockBreakQuestTask.class);
                case "KILL_ENTITY" -> task = context.deserialize(jsonObject, KillEntityQuestTask.class);
                case "MANUAL" -> task = context.deserialize(jsonObject, ManualQuestTask.class);
                default -> task = context.deserialize(jsonObject, QuestTask.class);
            }
            return task;
        }

        @Override
        public JsonElement serialize(QuestTask src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src, src.getClass());
        }
    }
}
