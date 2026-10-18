package com.storyengine.trigger.action;

import com.google.gson.JsonObject;
import com.storyengine.trigger.ActionStatus;
import com.storyengine.trigger.TriggerAction;
import com.storyengine.trigger.TriggerContext;
import net.minecraft.server.level.ServerPlayer;

/**
 * Действие {@code start_script}: запустить скрипт HollowEngine (.se.kts)
 * для игрока, вызвавшего триггер, — первоклассная альтернатива
 * {@code run_command} с командой {@code hollowengine start-script}.
 * Параметры:
 * <pre>
 * { "type": "start_script", "script": "act2_chapter.se.kts", "players": "@p" }
 * </pre>
 *
 * @param script  имя файла скрипта (обязательный параметр; путь такой же,
 *                как предлагает автодополнение команды
 *                {@code /hollowengine start-script})
 * @param players селектор или имя получателя; по умолчанию {@code @p} —
 *                игрок, нажавший F/кликнувший цель
 */
public final class StartScriptAction implements TriggerAction {

    private final String script;
    private final String players;

    private StartScriptAction(JsonObject params) {
        this.script = params.has("script") ? params.get("script").getAsString() : "";
        this.players = params.has("players") ? params.get("players").getAsString() : "";
    }

    @Override
    public String type() {
        return "start_script";
    }

    @Override
    public ActionStatus execute(TriggerContext context) {
        ServerPlayer player = context.player();
        if (player == null || script.isBlank()) {
            return ActionStatus.FAILURE;
        }
        // Порядок аргументов команды HollowEngine: start-script <players> <script...>.
        String selector = players.isBlank() ? "@p" : players;
        int result = ActionCommands.runAsPlayer(player,
                "hollowengine start-script " + selector + " " + script);
        return result >= 0 ? ActionStatus.SUCCESS : ActionStatus.FAILURE;
    }

    /** Фабрика для TriggerActionRegistry. */
    public static StartScriptAction of(JsonObject params) {
        return new StartScriptAction(params);
    }
}
