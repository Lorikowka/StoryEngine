package com.storyengine.narrative;

public record SpeakerInfo(String name, String iconId, int nameColor) {

    public SpeakerInfo(String name, String iconId) {
        this(name, iconId, NarrativeMessage.DEFAULT_NAME_COLOR);
    }
}