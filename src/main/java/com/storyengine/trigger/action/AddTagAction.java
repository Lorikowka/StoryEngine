package com.storyengine.trigger.action;

import com.google.gson.JsonObject;
import com.storyengine.trigger.ActionStatus;
import com.storyengine.trigger.TriggerAction;
import com.storyengine.trigger.TriggerContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;

/**
 * Действие {@code add_tag}: добавить scoreboard-тег цели (ТЗ §5.2, §8).
 * Параметры:
 * <pre>
 * { "type": "add_tag", "tag": "archive_clue_found" }
 * </pre>
 * Тег ставится на сущность-цель события (если есть) или на игрока
 * (выбирается ближайшая цель по §5.3).
 */
public final class AddTagAction implements TriggerAction {

    private final String tag;

    private AddTagAction(JsonObject params) {
        this.tag = params.has("tag") ? params.get("tag").getAsString() : "";
    }

    @Override
    public String type() {
        return "add_tag";
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
        if (target.getTags().contains(tag)) {
            return ActionStatus.SKIP;
        }
        target.addTag(tag);
        return ActionStatus.SUCCESS;
    }

    /** Фабрика для TriggerActionRegistry. */
    public static AddTagAction of(JsonObject params) {
        return new AddTagAction(params);
    }
}