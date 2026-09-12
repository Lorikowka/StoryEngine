package com.storyengine.trigger.action;

import com.google.gson.JsonObject;
import com.storyengine.trigger.ActionStatus;
import com.storyengine.trigger.TriggerAction;
import com.storyengine.trigger.TriggerContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Действие {@code remove_tag}: удалить scoreboard-тег цели (ТЗ §8).
 * Параметры:
 * <pre>
 * { "type": "remove_tag", "tag": "archive_clue_found" }
 * </pre>
 * Тег снимается с сущности-цели события (если есть) или с игрока.
 */
public final class RemoveTagAction implements TriggerAction {

    private final String tag;

    private RemoveTagAction(JsonObject params) {
        this.tag = params.has("tag") ? params.get("tag").getAsString() : "";
    }

    @Override
    public String type() {
        return "remove_tag";
    }

    @Override
    public ActionStatus execute(TriggerContext context) {
        if (tag.isBlank()) {
            return ActionStatus.FAILURE;
        }
        Entity target = context.targetEntity();
        if (target == null) {
            ServerPlayer player = context.player();
            if (player == null) {
                return ActionStatus.FAILURE;
            }
            target = player;
        }
        if (!target.getTags().contains(tag)) {
            return ActionStatus.SKIP;
        }
        target.removeTag(tag);
        return ActionStatus.SUCCESS;
    }

    /** Фабрика для TriggerActionRegistry. */
    public static RemoveTagAction of(JsonObject params) {
        return new RemoveTagAction(params);
    }
}