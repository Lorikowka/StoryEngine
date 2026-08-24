package com.storyengine.dialogue;

import com.google.gson.annotations.SerializedName;

/**
 * Мета-информация диалога (файл _meta.json в папке диалога).
 * Все поля опционально переопределяются в конкретном узле.
 */
public class DialogueMeta {

    private String title = "";
    private String entry = "start";
    private String speaker = "";
    private String icon = "";
    private String portrait = "";

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEntry() {
        return entry;
    }

    public void setEntry(String entry) {
        this.entry = entry;
    }

    public String getSpeaker() {
        return speaker;
    }

    public void setSpeaker(String speaker) {
        this.speaker = speaker;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getPortrait() {
        return portrait;
    }

    public void setPortrait(String portrait) {
        this.portrait = portrait;
    }
}
