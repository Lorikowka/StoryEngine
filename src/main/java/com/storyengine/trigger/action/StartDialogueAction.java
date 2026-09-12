package com.storyengine.trigger.action;

import com.google.gson.JsonObject;
import com.storyengine.StoryEngineMod;
import com.storyengine.dialogue.DialogueManager;
import com.storyengine.dialogue.DialogueMeta;
import com.storyengine.dialogue.DialogueNode;
import com.storyengine.network.dialogue.DialogueNetworking;
import com.storyengine.trigger.ActionStatus;
import com.storyengine.trigger.TriggerAction;
import com.storyengine.trigger.TriggerContext;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Действие {@code start_dialogue}: запустить диалог у игрока (ТЗ §8).
 * Параметры:
 * <pre>
 * { "type": "start_dialogue", "dialogue": "investigation", "node": "start" }
 * </pre>
 * {@code node} опционален (берётся entry диалога); {@code npc} — optional UUID
 * NPC для камеры. Полностью повторяет флоу {@code /story dialogue start}.
 */
public final class StartDialogueAction implements TriggerAction {

    private final String dialogue;
    private final String node;
    private final String npc;

    private StartDialogueAction(JsonObject params) {
        this.dialogue = params.has("dialogue") ? params.get("dialogue").getAsString() : "";
        this.node = params.has("node") ? params.get("node").getAsString() : null;
        this.npc = params.has("npc") ? params.get("npc").getAsString() : null;
    }

    @Override
    public String type() {
        return "start_dialogue";
    }

    @Override
    public ActionStatus execute(TriggerContext context) {
        ServerPlayer player = context.player();
        if (player == null || dialogue.isBlank()) {
            return ActionStatus.FAILURE;
        }
        DialogueManager manager = StoryEngineMod.DIALOGUE_MANAGER;
        if (!manager.dialogueExists(dialogue)) {
            return ActionStatus.FAILURE;
        }
        UUID npcId = null;
        if (npc != null && !npc.isBlank()) {
            try {
                npcId = UUID.fromString(npc);
            } catch (IllegalArgumentException e) {
                return ActionStatus.FAILURE;
            }
        }
        if (manager.start(player, dialogue, node, npcId) == null) {
            return ActionStatus.FAILURE;
        }
        DialogueMeta meta = manager.loadDialogue(dialogue).orElse(null);
        DialogueSessionProxy session = new DialogueSessionProxy(manager, player);
        DialogueNode startNode = session.currentNodeId() == null
                ? null : manager.loadNode(dialogue, session.currentNodeId()).orElse(null);
        if (startNode == null) {
            return ActionStatus.FAILURE;
        }
        DialogueNetworking.sendOpen(player, dialogue, startNode, meta);
        return ActionStatus.SUCCESS;
    }

    /** Лёгкий прокси, чтобы не тащить полный DialogueSession в зависимость пакета. */
    private static final class DialogueSessionProxy {
        private final DialogueManager manager;
        private final ServerPlayer player;

        private DialogueSessionProxy(DialogueManager manager, ServerPlayer player) {
            this.manager = manager;
            this.player = player;
        }

        private String currentNodeId() {
            var session = manager.getSession(player);
            return session == null ? null : session.getCurrentNodeId();
        }
    }

    /** Фабрика для TriggerActionRegistry. */
    public static StartDialogueAction of(JsonObject params) {
        return new StartDialogueAction(params);
    }
}