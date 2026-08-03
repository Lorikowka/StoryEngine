package com.storyengine.quest;

public class KillEntityQuestTask extends QuestTask {

    private String entityType;
    private int count;

    public KillEntityQuestTask() {
        setType("KILL_ENTITY");
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
