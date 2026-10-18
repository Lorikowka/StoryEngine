package com.storyengine.narrative;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Одно сообщение "сюжетного чата": кто говорит, какая иконка и сам текст
 * (с сохранением форматирования - как в /tellraw), плюс цвет имени спикера.
 */
public final class NarrativeMessage {

    /** Цвет имени спикера по умолчанию (жёлтый), если не задан явно через /storytell. */
    public static final int DEFAULT_NAME_COLOR = 0xFFFF55;

    private final List<SpeakerInfo> speakers;
    private final Component text;
    private final NarrativeStyle style;

    /** Конструктор с цветом имени по умолчанию (жёлтый). */
    public NarrativeMessage(String speaker, String iconId, Component text) {
        this(speaker, iconId, text, DEFAULT_NAME_COLOR);
    }

    public NarrativeMessage(String speaker, String iconId, Component text, int nameColor) {
        this(List.of(new SpeakerInfo(speaker, iconId, nameColor)), text);
    }

    public NarrativeMessage(List<SpeakerInfo> speakers, Component text) {
        this(speakers, text, NarrativeStyle.NORMAL);
    }

    public NarrativeMessage(List<SpeakerInfo> speakers, Component text, NarrativeStyle style) {
        if (speakers == null || speakers.isEmpty()) {
            throw new IllegalArgumentException("NarrativeMessage requires at least one speaker");
        }
        this.speakers = List.copyOf(speakers);
        this.text = text;
        this.style = style == null ? NarrativeStyle.NORMAL : style;
    }

    public List<SpeakerInfo> getSpeakers() {
        return speakers;
    }

    public String getSpeaker() {
        return speakers.get(0).name();
    }

    /** Сырое имя иконки без .png, как передано в /storytell. "none" - иконки нет. */
    public String getIconId() {
        return speakers.get(0).iconId();
    }

    public Component getText() {
        return text;
    }

    public NarrativeStyle getStyle() {
        return style;
    }

    /** ARGB-цвет имени спикера (с альфой 0xFF), как задано в /storytell color <hex>. */
    public int getNameColor() {
        return speakers.get(0).nameColor();
    }
}
