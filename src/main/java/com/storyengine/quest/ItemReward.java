package com.storyengine.quest;

public class ItemReward {

    private String id;
    private int count;
    private String nbt;

    public ItemReward() {
    }

    public ItemReward(String id, int count, String nbt) {
        this.id = id;
        this.count = count;
        this.nbt = nbt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getNbt() {
        return nbt;
    }

    public void setNbt(String nbt) {
        this.nbt = nbt;
    }
}
