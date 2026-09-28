package com.storyengine.trigger.action;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.storyengine.StoryEngineMod;
import com.storyengine.checkpoint.CheckpointRegionSpec;
import com.storyengine.checkpoint.SceneSnapshotManager;
import com.storyengine.trigger.ActionStatus;
import com.storyengine.trigger.TriggerAction;
import com.storyengine.trigger.TriggerContext;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Действие {@code save_checkpoint}: снимает снапшот сцены в заданном регионе и
 * сохраняет его в {@code config/story_engine/checkpoints/<id>.json}.
 *
 * <pre>
 * {
 *   "type": "save_checkpoint",
 *   "id": "melan_scene_start",
 *   "checkpoint_region": {
 *     "center_tag": "story_npc_melan",
 *     "radius": 8,
 *     "entity_tags": ["story_npc_melan", "scene_prop_candles"]
 *   }
 * }
 * </pre>
 *
 * Нет региона / центр по тегу не найден / недопустимый id — {@code FAILURE}
 * (останавливает цепочку по правилу fail-stop, как остальные действия).
 */
public final class SaveCheckpointAction implements TriggerAction {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final String id;
    private final Optional<CheckpointRegionSpec> region;

    private SaveCheckpointAction(JsonObject params) {
        this.id = params.has("id") ? params.get("id").getAsString() : "";
        this.region = CheckpointRegionSpec.parse(params);
    }

    @Override
    public String type() {
        return "save_checkpoint";
    }

    @Override
    public ActionStatus execute(TriggerContext context) {
        if (id.isBlank()) {
            LOGGER.error("[StoryEngine] save_checkpoint: отсутствует обязательный параметр 'id'");
            return ActionStatus.FAILURE;
        }
        if (region.isEmpty()) {
            LOGGER.error("[StoryEngine] save_checkpoint '{}': отсутствует 'checkpoint_region'", id);
            return ActionStatus.FAILURE;
        }
        SceneSnapshotManager manager = StoryEngineMod.SNAPSHOT_MANAGER;
        boolean captured = manager.capture(id, context.level(), context.player(), region.get()).isPresent();
        if (!captured) {
            LOGGER.error("[StoryEngine] save_checkpoint: не удалось снять чекпойнт '{}'", id);
            return ActionStatus.FAILURE;
        }
        return ActionStatus.SUCCESS;
    }

    /** Фабрика для TriggerActionRegistry. */
    public static SaveCheckpointAction of(JsonObject params) {
        return new SaveCheckpointAction(params);
    }
}