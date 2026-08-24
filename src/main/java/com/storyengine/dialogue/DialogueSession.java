package com.storyengine.dialogue;

import net.minecraft.server.level.ServerPlayer;

/**
 * Активная сессия диалога у конкретного игрока. Хранится эфемерно
 * (в Map в DialogueManager), НЕ сериализуется. При logout удаляется.
 */
public class DialogueSession {

    private final ServerPlayer player;
    private final String dialogueId;
    private String currentNodeId;
    private long lastSelectTime = 0;

    public DialogueSession(ServerPlayer player, String dialogueId, String startNodeId) {
        this.player = player;
        this.dialogueId = dialogueId;
        this.currentNodeId = startNodeId;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public String getDialogueId() {
        return dialogueId;
    }

    public String getCurrentNodeId() {
        return currentNodeId;
    }

    public void setCurrentNodeId(String currentNodeId) {
        this.currentNodeId = currentNodeId;
    }

    public long getLastSelectTime() {
        return lastSelectTime;
    }

    public void setLastSelectTime(long lastSelectTime) {
        this.lastSelectTime = lastSelectTime;
    }
}
