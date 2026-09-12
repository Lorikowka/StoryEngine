package com.storyengine.trigger.action;

import com.google.gson.JsonObject;
import com.storyengine.narrative.NarrativeMessage;
import com.storyengine.network.NarrativeNetworking;
import com.storyengine.trigger.ActionStatus;
import com.storyengine.trigger.TriggerAction;
import com.storyengine.trigger.TriggerContext;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;

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

    private StoryTellAction(JsonObject params) {
        this.speaker = params.has("speaker") ? params.get("speaker").getAsString() : "";
        this.icon = params.has("icon") ? params.get("icon").getAsString() : "none";
        this.message = params.has("text")
                ? Component.Serializer.fromJson(params.get("text"))
                : Component.literal(params.has("text") ? "" : "");
        this.nameColor = params.has("name_color") ? params.get("name_color").getAsInt()
                : NarrativeMessage.DEFAULT_NAME_COLOR;
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
        NarrativeNetworking.sendToPlayers(Collections.singletonList(player), speaker, icon, message, nameColor);
        return ActionStatus.SUCCESS;
    }

    /** Фабрика для TriggerActionRegistry. */
    public static StoryTellAction of(JsonObject params) {
        return new StoryTellAction(params);
    }
}