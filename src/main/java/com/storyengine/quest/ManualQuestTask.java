package com.storyengine.quest;

public class ManualQuestTask extends QuestTask {
    public ManualQuestTask() {
        setType("MANUAL");
    }

    public ManualQuestTask(String id, String title, String description) {
        super(id, title, description);
        setType("MANUAL");
    }
}
