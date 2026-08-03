package com.storyengine.quest;

public class ItemQuestTask extends QuestTask {

    private String target;
    private int count;
    private boolean consume;

    public ItemQuestTask() {
        setType("ITEM");
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isConsume() {
        return consume;
    }

    public void setConsume(boolean consume) {
        this.consume = consume;
    }
}
