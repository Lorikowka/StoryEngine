package com.storyengine.dialogue;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Один узел диалога (файл &lt;nodeId&gt;.json в папке диалога) - один экран
 * реплики NPC с вариантами ответа игрока.
 */
public class DialogueNode {

    private String text = "";
    @SerializedName("speaker")
    private String speakerOverride = "";
    @SerializedName("icon")
    private String iconOverride = "";
    @SerializedName("portrait")
    private String portraitOverride = "";

    private List<DialogueResponse> responses = new ArrayList<>();

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSpeakerOverride() {
        return speakerOverride;
    }

    public void setSpeakerOverride(String speakerOverride) {
        this.speakerOverride = speakerOverride;
    }

    public String getIconOverride() {
        return iconOverride;
    }

    public void setIconOverride(String iconOverride) {
        this.iconOverride = iconOverride;
    }

    public String getPortraitOverride() {
        return portraitOverride;
    }

    public void setPortraitOverride(String portraitOverride) {
        this.portraitOverride = portraitOverride;
    }

    public List<DialogueResponse> getResponses() {
        return responses;
    }

    public void setResponses(List<DialogueResponse> responses) {
        this.responses = responses;
    }

    /** Итоговый спикер узла: переопределение узла, иначе из _meta. */
    public String resolveSpeaker(DialogueMeta meta) {
        if (speakerOverride != null && !speakerOverride.isEmpty()) {
            return speakerOverride;
        }
        return meta != null ? meta.getSpeaker() : "";
    }

    /** Итоговая иконка узла: переопределение узла, иначе из _meta. */
    public String resolveIcon(DialogueMeta meta) {
        if (iconOverride != null && !iconOverride.isEmpty()) {
            return iconOverride;
        }
        return meta != null ? meta.getIcon() : "";
    }

    /** Итоговый портрет узла: переопределение узла, иначе из _meta. */
    public String resolvePortrait(DialogueMeta meta) {
        if (portraitOverride != null && !portraitOverride.isEmpty()) {
            return portraitOverride;
        }
        return meta != null ? meta.getPortrait() : "";
    }
}
