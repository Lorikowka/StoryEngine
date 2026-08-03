package com.storyengine.quest;

import java.util.ArrayList;
import java.util.List;

/**
 * Награды за выполнение квеста.
 */
public class QuestRewards {

    private List<String> commands = new ArrayList<>();
    private Integer experience;
    private List<ItemReward> items = new ArrayList<>();

    public QuestRewards() {
        // требуется для десериализации Gson
    }

    public List<String> getCommands() {
        return commands != null ? commands : new ArrayList<>();
    }

    public void setCommands(List<String> commands) {
        this.commands = commands;
    }

    public Integer getExperience() {
        return experience;
    }

    public void setExperience(Integer experience) {
        this.experience = experience;
    }

    public List<ItemReward> getItems() {
        return items != null ? items : new ArrayList<>();
    }

    public void setItems(List<ItemReward> items) {
        this.items = items;
    }
}
