package com.storyengine.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuestJsonModelTest {

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(QuestTask.class, new QuestTask.Serializer())
            .setPrettyPrinting()
            .create();

    @Test
    void deserializesPolymorphicTasksAndPrerequisites() {
        String json = """
                {
                  "id": "test_quest",
                  "title": "Test quest",
                  "description": "desc",
                  "prerequisites": ["root_quest"],
                  "tasks": [
                    {
                      "id": "loc_task",
                      "title": "Go there",
                      "description": "Reach the place",
                      "type": "LOCATION",
                      "dimension": "minecraft:overworld",
                      "x": 10.5,
                      "y": 64.0,
                      "z": -20.0,
                      "radius": 5.0
                    }
                  ],
                  "rewards": {
                    "experience": 25,
                    "items": [
                      {"id": "minecraft:diamond", "count": 2, "nbt": "{}"}
                    ]
                  }
                }
                """;

        QuestData data = gson.fromJson(json, QuestData.class);

        assertNotNull(data);
        assertEquals("test_quest", data.getId());
        assertEquals(1, data.getPrerequisites().size());
        assertEquals("root_quest", data.getPrerequisites().get(0));
        assertEquals(1, data.getTasks().size());
        assertInstanceOf(LocationQuestTask.class, data.getTasks().get(0));
        LocationQuestTask task = (LocationQuestTask) data.getTasks().get(0);
        assertEquals("minecraft:overworld", task.getDimension());
        assertEquals(10.5, task.getX());
        assertEquals(25, data.getRewards().getExperience());
        assertEquals(1, data.getRewards().getItems().size());
    }
}
