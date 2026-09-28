package com.storyengine.trigger.action;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.storyengine.StoryEngineMod;
import com.storyengine.checkpoint.SceneSnapshotManager;
import com.storyengine.trigger.ActionStatus;
import com.storyengine.trigger.TriggerAction;
import com.storyengine.trigger.TriggerContext;
import org.slf4j.Logger;

/**
 * Действие {@code restore_checkpoint}: применяет сохранённый снапшот сцены
 * (блоки → сущности → игрок).
 *
 * <pre>
 * { "type": "restore_checkpoint", "id": "melan_scene_start" }
 * </pre>
 *
 * Попытка восстановить несуществующий id — {@code FAILURE} с {@code LOGGER.error}
 * (это ошибка конфигурации, а не штатный пропуск; останавливает цепочку по
 * fail-stop правилу).
 */
public final class RestoreCheckpointAction implements TriggerAction {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final String id;

    private RestoreCheckpointAction(JsonObject params) {
        this.id = params.has("id") ? params.get("id").getAsString() : "";
    }

    @Override
    public String type() {
        return "restore_checkpoint";
    }

    @Override
    public ActionStatus execute(TriggerContext context) {
        if (id.isBlank()) {
            LOGGER.error("[StoryEngine] restore_checkpoint: отсутствует обязательный параметр 'id'");
            return ActionStatus.FAILURE;
        }
        SceneSnapshotManager manager = StoryEngineMod.SNAPSHOT_MANAGER;
        if (!manager.restore(id, context.player())) {
            LOGGER.error("[StoryEngine] restore_checkpoint: чекпойнт '{}' не найден", id);
            return ActionStatus.FAILURE;
        }
        return ActionStatus.SUCCESS;
    }

    /** Фабрика для TriggerActionRegistry. */
    public static RestoreCheckpointAction of(JsonObject params) {
        return new RestoreCheckpointAction(params);
    }
}