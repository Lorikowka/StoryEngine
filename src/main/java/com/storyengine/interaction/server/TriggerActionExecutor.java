package com.storyengine.interaction.server;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.storyengine.StoryEngineMod;
import com.storyengine.dialogue.DialogueActionExecutor;
import com.storyengine.dialogue.DialogueManager;
import com.storyengine.dialogue.DialogueMeta;
import com.storyengine.dialogue.DialogueNode;
import com.storyengine.interaction.data.InteractionTrigger;
import com.storyengine.interaction.data.TriggerAction;
import com.storyengine.network.NarrativeNetworking;
import com.storyengine.network.dialogue.DialogueNetworking;
import com.storyengine.narrative.NarrativeMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.Collections;

/**
 * Серверное исполнение действия триггера (см. спецификацию §6).
 *
 * Порядок (близок к DialogueActionExecutor §5):
 *   1) command
 *   2) dialogue (открыть диалог) / completeTask
 *   3) sound
 *   4) setFlag
 *   5) storytell
 *
 * command/completeTask/setFlag переиспользуют DialogueActionExecutor, чтобы
 * не дублировать логику квестов/флагов. dialogue и sound — специфичны для
 * Interaction System.
 */
public final class TriggerActionExecutor {

    private static final Logger LOGGER = LogUtils.getLogger();

    private TriggerActionExecutor() {
    }

    public static void execute(ServerPlayer player, InteractionTrigger trigger, TriggerAction action) {
        if (action.getCommand() != null && !action.getCommand().isBlank()) {
            DialogueActionExecutor.runCommand(player, action.getCommand());
        }

        if (action.getDialogue() != null && !action.getDialogue().isBlank()) {
            openDialogue(player, action.getDialogue());
        }

        if (action.getCompleteTask() != null && !action.getCompleteTask().isBlank()) {
            DialogueActionExecutor.completeTask(player, action.getCompleteTask());
        }

        if (action.getSound() != null && !action.getSound().isBlank()) {
            playSound(player, action.getSound());
        }

        if (action.getSetFlag() != null && !action.getSetFlag().isBlank()) {
            DialogueActionExecutor.setFlag(player, action.getSetFlag());
        }

        if (action.getStorytell() != null) {
            storytell(player, action.getStorytell());
        }
    }

    private static void openDialogue(ServerPlayer player, String dialogueId) {
        DialogueManager manager = StoryEngineMod.DIALOGUE_MANAGER;
        if (manager.start(player, dialogueId, null, null) == null) {
            LOGGER.warn("[StoryEngine] Не удалось начать диалог '{}' из триггера.", dialogueId);
            return;
        }
        DialogueMeta meta = manager.loadDialogue(dialogueId).orElse(null);
        DialogueNode node = manager.loadNode(dialogueId, manager.getSession(player).getCurrentNodeId()).orElse(null);
        if (node != null) {
            DialogueNetworking.sendOpen(player, dialogueId, node, meta);
        }
    }

    private static void playSound(ServerPlayer player, String soundId) {
        ResourceLocation loc = new ResourceLocation(soundId);
        SoundEvent event = ForgeRegistries.SOUND_EVENTS.getValue(loc);
        if (event == null) {
            LOGGER.warn("[StoryEngine] Неизвестный звук в триггере: '{}'", soundId);
            return;
        }
        player.playSound(event, 1.0f, 1.0f);
    }

    private static void storytell(ServerPlayer player, TriggerAction.Storytell data) {
        JsonObject message = data.getMessage();
        if (message == null) {
            return;
        }
        Component component = Component.Serializer.fromJson(message);
        if (component == null) {
            return;
        }
        String speaker = data.getSpeaker() != null ? data.getSpeaker() : "";
        NarrativeNetworking.sendToPlayers(Collections.singletonList(player), speaker, "none", component, NarrativeMessage.DEFAULT_NAME_COLOR);
    }
}
