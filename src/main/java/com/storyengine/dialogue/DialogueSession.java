package com.storyengine.dialogue;

import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Активная сессия диалога у конкретного игрока. Хранится эфемерно
 * (в Map в DialogueManager), НЕ сериализуется. При logout удаляется.
 */
public class DialogueSession {

    private final ServerPlayer player;
    private final String dialogueId;
    private String currentNodeId;
    private long lastSelectTime = 0;
    @Nullable
    private UUID npcId;

    public DialogueSession(ServerPlayer player, String dialogueId, String startNodeId, @Nullable UUID npcId) {
        this.player = player;
        this.dialogueId = dialogueId;
        this.currentNodeId = startNodeId;
        this.npcId = npcId;
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

    /** Сессионный UUID NPC (для камеры). null = диалог без привязки к сущности. */
    @Nullable
    public UUID getNpcId() {
        return npcId;
    }

    public void setNpcId(@Nullable UUID npcId) {
        this.npcId = npcId;
    }

    public long getLastSelectTime() {
        return lastSelectTime;
    }

    public void setLastSelectTime(long lastSelectTime) {
        this.lastSelectTime = lastSelectTime;
    }
}
