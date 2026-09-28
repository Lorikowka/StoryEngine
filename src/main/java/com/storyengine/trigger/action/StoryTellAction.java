package com.storyengine.trigger.action;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.storyengine.narrative.NarrativeMessage;
import com.storyengine.narrative.SpeakerInfo;
import com.storyengine.network.NarrativeNetworking;
import com.storyengine.trigger.ActionStatus;
import com.storyengine.trigger.TriggerAction;
import com.storyengine.trigger.TriggerContext;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

/**
 * Действие {@code story_tell}: показать реплику через Narrative HUD
 * (ТЗ §8). Параметры:
 * <pre>
 * { "type": "story_tell",
 *   "speaker": "Мысли",
 *   "icon": "none",
 *   "text": {"text":"..."},          // Component-JSON
 *   "name_color": 0xFFFF55 }        // опционально
 * </pre>
 */
public final class StoryTellAction implements TriggerAction {

    private final String speaker;
    private final String icon;
    private final Component message;
    private final int nameColor;
    private final List<SpeakerInfo> speakers;

    private StoryTellAction(JsonObject params) {
        this.speakers = parseSpeakers(params);
        this.speaker = params.has("speaker") ? params.get("speaker").getAsString() : "";
        this.icon = params.has("icon") ? params.get("icon").getAsString() : "none";
        this.message = params.has("text")
                ? Component.Serializer.fromJson(params.get("text"))
                : Component.literal(params.has("text") ? "" : "");
        this.nameColor = parseColor(params, NarrativeMessage.DEFAULT_NAME_COLOR);
    }

    @Override
    public String type() {
        return "story_tell";
    }

    @Override
    public ActionStatus execute(TriggerContext context) {
        ServerPlayer player = context.player();
        if (player == null || message == null) {
            return ActionStatus.FAILURE;
        }
        if (speakers != null) {
            NarrativeNetworking.sendToPlayers(Collections.singletonList(player), speakers, message);
        } else {
            NarrativeNetworking.sendToPlayers(Collections.singletonList(player), speaker, icon, message, nameColor);
        }
        return ActionStatus.SUCCESS;
    }

    private static List<SpeakerInfo> parseSpeakers(JsonObject params) {
        if (!params.has("speakers") || !params.get("speakers").isJsonArray()) {
            return null;
        }
        JsonArray jsonSpeakers = params.getAsJsonArray("speakers");
        if (jsonSpeakers.isEmpty()) {
            return null;
        }
        List<SpeakerInfo> speakers = new ArrayList<>(jsonSpeakers.size());
        for (JsonElement element : jsonSpeakers) {
            JsonObject speaker = element.getAsJsonObject();
            String name = speaker.has("name") ? speaker.get("name").getAsString() : "";
            String icon = speaker.has("icon") ? speaker.get("icon").getAsString() : "none";
            int color = parseColor(speaker, NarrativeMessage.DEFAULT_NAME_COLOR);
            speakers.add(new SpeakerInfo(name, icon, color));
        }
        return List.copyOf(speakers);
    }

    private static int parseColor(JsonObject params, int defaultColor) {
        if (!params.has("name_color")) {
            return defaultColor;
        }
        JsonElement color = params.get("name_color");
        if (color.getAsJsonPrimitive().isNumber()) {
            return color.getAsInt();
        }
        String value = color.getAsString().trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        return Integer.parseUnsignedInt(value, 16);
    }

    /** Фабрика для TriggerActionRegistry. */
    public static StoryTellAction of(JsonObject params) {
        return new StoryTellAction(params);
    }
}