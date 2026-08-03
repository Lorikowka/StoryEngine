package com.storyengine.quest;

public class BlockBreakQuestTask extends QuestTask {

    private String blockId;
    private int count;

    public BlockBreakQuestTask() {
        setType("BLOCK_BREAK");
    }

    public String getBlockId() {
        return blockId;
    }

    public void setBlockId(String blockId) {
        this.blockId = blockId;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
