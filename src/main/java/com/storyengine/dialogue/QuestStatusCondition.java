package com.storyengine.dialogue;

import com.storyengine.player.PlayerDialogueData;
import com.storyengine.player.PlayerQuestDataHelper;
import com.storyengine.quest.QuestStatus;
import net.minecraft.server.level.ServerPlayer;

/** Статус квеста у игрока: quest:<id>:<status>. */
public class QuestStatusCondition extends DialogueCondition {

    private final String questId;
    private final QuestStatus status;

    public QuestStatusCondition(String questId, String statusName) {
        this.questId = questId;
        QuestStatus parsed;
        try {
            parsed = QuestStatus.valueOf(statusName.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            parsed = null;
        }
        this.status = parsed;
    }

    @Override
    public boolean evaluate(ServerPlayer player) {
        if (status == null) {
            return false;
        }
        return PlayerQuestDataHelper.getStatus(player, questId) == status;
    }
}
