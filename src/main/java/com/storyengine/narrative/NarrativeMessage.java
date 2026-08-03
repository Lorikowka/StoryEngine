package com.storyengine.narrative;

import net.minecraft.network.chat.Component;

/**
 * Одно сообщение "сюжетного чата": кто говорит, какая иконка и сам текст
 * (с сохранением форматирования - как в /tellraw), плюс цвет имени спикера.
 */
public final class NarrativeMessage {

    /** Цвет имени спикера по умолчанию (жёлтый), если не задан явно через /storytell. */
    public static final int DEFAULT_NAME_COLOR = 0xFFFF55;

    private final String speaker;
    private final String iconId;
    private final Component text;
    private final int nameColor;

    /** Конструктор с цветом имени по умолчанию (жёлтый). */
    public NarrativeMessage(String speaker, String iconId, Component text) {
        this(speaker, iconId, text, DEFAULT_NAME_COLOR);
    }

    public NarrativeMessage(String speaker, String iconId, Component text, int nameColor) {
        this.speaker = speaker;
        this.iconId = iconId;
        this.text = text;
        this.nameColor = nameColor;
    }

    public String getSpeaker() {
        return speaker;
    }

    /** Сырое имя иконки без .png, как передано в /storytell. "none" - иконки нет. */
    public String getIconId() {
        return iconId;
    }

    public Component getText() {
        return text;
    }

    /** ARGB-цвет имени спикера (с альфой 0xFF), как задано в /storytell color <hex>. */
    public int getNameColor() {
        return nameColor;
    }
}
