package com.storyengine.narrative;

/**
 * Настройки Narrative HUD на уровне мира/сервера.
 * Сериализуется через Gson в config/story_engine/narrative_config.json.
 */
public class NarrativeConfig {

    /** Цвет имени игрока, когда он отвечает через NarrativeLogScreen, если не задан отдельно. */
    private int defaultPlayerColor = NarrativeMessage.DEFAULT_NAME_COLOR;

    public int getDefaultPlayerColor() {
        return defaultPlayerColor;
    }

    public void setDefaultPlayerColor(int defaultPlayerColor) {
        this.defaultPlayerColor = defaultPlayerColor;
    }
}
